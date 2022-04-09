package Mutator;

import IsoVar.VariableInfo;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;

import static Instrument.InstrTransform.doInstr;
import static Instrument.Instrument.*;
import static IsoVar.VariableInfo.isPrimary;
import static Mutator.PrimaryMutator.*;

enum SearchState {
    can_not_mutate,
    further_search,
    final_state
}

public class ObjectMutator extends Mutator {

    private final int maxLevel = 3;

    public ObjectMutator(VariableInfo var, int times) {
        super(var, times);
    }

    @Override
    public List<String> mutate(String mutantTemplate) {
        List<String> mutants = new ArrayList<>();
        Set<SootClass> currentClass = new HashSet<>();
        for (int i = 1; i <= times; i++) {
            boolean existPhantom = false;
            // first add one class
            SootClass clazz = Scene.v().loadClassAndSupport(var.className);
            if (clazz.isJavaLibraryClass()) // would not mutate JDK field
                return mutants;
            currentClass.add(clazz);
            for (SootMethod method : clazz.getMethods()) {
                if (method.isAbstract())
                    continue;
                if (method.isPhantom()) {
                    existPhantom = true;
                    currentClass.remove(clazz);
                    break;
                }
                Body body = method.retrieveActiveBody();
                doInstr(body);
                if (byteCodeSunSignature(method).equals(var.methodName)) {
                    if (!mutateObjectVar(body, currentClass)) {
                        // do not exist path
                        Scene.v().removeClass(clazz);
                        return mutants;
                    }
                }
                method.setActiveBody(body);
            }
            if (existPhantom) {
                Scene.v().removeClass(clazz);
                clazz = Scene.v().loadClassAndSupport(var.className);
                currentClass.add(clazz);
                for (SootMethod method : clazz.getMethods()) {
                    if (method.isAbstract())
                        continue;
                    Body body = method.retrieveActiveBody();
                    doInstr(body);
                    if (byteCodeSunSignature(method).equals(var.methodName)) {
                        if (!mutateObjectVar(body, currentClass)) {
                            // do not exist path
                            Scene.v().removeClass(clazz);
                            return mutants;
                        }
                    }
                    method.setActiveBody(body);
                }
            }
            String mutantPath = mutantTemplate + i + "/";
            mutants.add(mutantPath);

            for (SootClass aClass : currentClass) {
                String clazzPath = mutantPath + aClass.getName().replace(".", "/") + ".class";
                mkdirs(clazzPath);
                if (aClass != clazz) {
                    for (SootMethod method : aClass.getMethods()) {
                        if (method.isAbstract())
                            continue;
                        try {
                            Body body = method.retrieveActiveBody();
                            doInstr(body);
                            method.setActiveBody(body);
                        } catch (RuntimeException ignored) {
                        }
                    }
                }
                writeMutants(clazzPath, aClass);
                try {
                    Scene.v().removeClass(aClass);
                } catch (RuntimeException ignore) {
                }

            }
            currentClass.clear();
        }
        return mutants;
    }


    private boolean mutateObjectVar(Body body, Set<SootClass> sootClasses) {
        Chain<Local> locals = body.getLocals();
        List<Stmt> toInsert = new ArrayList<>();
        Unit point = null;
        Unit afterIdentifyPoint = null;
        for (Unit unit : body.getUnits()) {
            if (point != null && afterIdentifyPoint != null)
                break;
            int line = unit.getJavaSourceStartLineNumber();
            if (line != -1 && afterIdentifyPoint == null)
                afterIdentifyPoint = unit;
            if (line == var.getLine() && (unit instanceof AssignStmt || unit instanceof IdentityStmt)) {
                DefinitionStmt stmt = (DefinitionStmt) unit;
                Value left = stmt.getLeftOp();
                Value right = stmt.getRightOp();
                switch (var.type) {
                    case object_single: // means var is a local
                        if (left instanceof Local) {
                            Local local = (Local) left;
                            if (local.getName().equals(var.name)) {
                                point = unit;
                                // e.g. this.case3.case4.a
                                List<FieldTreeNode> paths = traversePath_single();
                                if (paths.isEmpty())
                                    return false;
                                int random = new Random().nextInt(paths.size());
                                toInsert.addAll(mutateSingleForOnePath(paths.get(random), local, sootClasses, locals));
                            }
                        }
                        break;
                    case object_instance:
                        int dotIndex = var.name.indexOf(".");
                        String varBase = var.name.substring(0, dotIndex);
                        String varName = var.name.substring(dotIndex + 1);
                        Value target;
                        if (var.isDef()) // for some instance field
                            target = left;
                        else
                            target = right;
                        if (target instanceof InstanceFieldRef) {
                            InstanceFieldRef instanceFieldRef = (InstanceFieldRef) target;
                            Value baseV = instanceFieldRef.getBase();
                            if (baseV instanceof Local) {
                                String baseName = ((Local) baseV).getName();
                                SootField field = instanceFieldRef.getField();
                                String fieldName = field.getName();
                                if (baseName.equals(varBase) && fieldName.equals(varName)) {
                                    point = unit;
                                    List<FieldTreeNode> paths = traversePath_instance(field);
                                    if (paths.isEmpty())
                                        return false;
                                    int random = new Random().nextInt(paths.size());
                                    toInsert.addAll(mutateObjectInstanceForOnePath(paths.get(random),
                                            instanceFieldRef, sootClasses, locals));
                                }
                            }
                        }
                        break;
                    case object_static:
                        if (var.isDef()) // for some instance field
                            target = left;
                        else
                            target = right;
                        varName = var.name.substring(1);
                        if (target instanceof StaticFieldRef) {
                            StaticFieldRef ref = (StaticFieldRef) target;
                            SootField field = ref.getField();
                            String declaringClass = field.getDeclaringClass().toString();
                            String varDeclaringClass = var.desc.substring(0, var.desc.indexOf(";"));
                            if (varName.equals(field.getName()) && declaringClass.equals(varDeclaringClass)) {
                                if (SearchState.final_state == isFinalNode(field, 0)) {
                                    point = unit;
                                    toInsert.addAll(mutateStaticField(field, ref, sootClasses, locals));
                                } else
                                    return false;
                            }
                        }

                        break;
//                    default:  // should not code here, it means a bug
//                        System.err.println("debug " + var.name + " " + var.methodName + " " + var.getLine());
                }
            }
        }
        if (point == null)
            return false;
        if (var.isParameter) {
            body.getUnits().insertBefore(toInsert, afterIdentifyPoint);
        } else {
            body.getUnits().insertAfter(toInsert, point);
        }
        return true;
    }

    private List<Stmt> mutateStaticField(SootField staticField, StaticFieldRef ref, Set<SootClass> sootClasses, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        if (!staticField.isPublic())
            staticField.setModifiers(Modifier.PUBLIC | staticField.getModifiers()); // make the field public
        SootClass clazz = staticField.getDeclaringClass();
        Local fieldLocal = Jimple.v().newLocal("xzf_", ref.getType());
        Stmt fieldStmt = Jimple.v().newAssignStmt(fieldLocal, ref);
        stmts.add(fieldStmt);
        locals.add(fieldLocal);
        sootClasses.add(clazz);
        stmts.addAll(mutatePrimaryVar(fieldLocal, locals));
        Stmt lastAssignStmt = Jimple.v().newAssignStmt(ref, fieldLocal);
        stmts.add(lastAssignStmt);

        return stmts;
    }

    private List<FieldTreeNode> traversePath_instance(SootField field) {
        List<FieldTreeNode> paths = new ArrayList<>();
        switch (isFinalNode(field, 1)) {
            case final_state:
                FieldTreeNode node = new FieldTreeNode(null, field, null, 0);
                paths.add(node);
                break;
            case further_search:
                SootClass sootClass = Scene.v().loadClassAndSupport(field.getType().toString());
                node = new FieldTreeNode(null, field, sootClass, 1);
                return BFS(node);
            default:
                return paths;
        }
        return paths;
    }

    private List<FieldTreeNode> traversePath_single() {

        SootClass clazz = Scene.v().getSootClass(var.desc);
        FieldTreeNode first = new FieldTreeNode(null, null, clazz, 0);
        return BFS(first);
    }

    private List<Stmt> mutateObjectInstanceForOnePath(FieldTreeNode path, InstanceFieldRef fieldRef,
                                                      Set<SootClass> sootClasses, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        SootField instanceField = fieldRef.getField();
        if (!instanceField.isPublic())
            instanceField.setModifiers(Modifier.PUBLIC | instanceField.getModifiers()); // make the field public
        SootClass clazz = fieldRef.getField().getDeclaringClass();
        Local fieldLocal = Jimple.v().newLocal("xzf_", fieldRef.getType());
        Stmt fieldStmt = Jimple.v().newAssignStmt(fieldLocal, fieldRef);
        stmts.add(fieldStmt);
        locals.add(fieldLocal);
        sootClasses.add(clazz);
        if (path.pre != null)
            stmts.addAll(iterPathForStmts(path, sootClasses, locals, fieldLocal));
        else {
            stmts.addAll(mutatePrimaryVar(fieldLocal, locals));
            Stmt stmt = Jimple.v().newAssignStmt(fieldRef, fieldLocal);
            stmts.add(stmt);
        }
        return stmts;
    }

    private List<Stmt> iterPathForStmts(FieldTreeNode path, Set<SootClass> sootClasses, Chain<Local> locals, Local fieldLocal) {
        Stack<Stmt> stmts = new Stack<>();
        List<SootField> nodes = new ArrayList<>();
        nodes.add(path.field);
        FieldTreeNode node = path.pre;
        if (path.field.isStatic()) {
            SootField staticField = path.field;
            StaticFieldRef ref = Jimple.v().newStaticFieldRef(staticField.makeRef());
            stmts.addAll(mutateStaticField(staticField, ref, sootClasses, locals));
            return stmts;
        }
        while (node.pre != null) {
            nodes.add(node.field);
            node = node.pre;
        }

        Local lastLocal = fieldLocal;
        SootField field;
        InstanceFieldRef lastAssignRef = null;
        for (int i = nodes.size() - 1; i >= 0; i--) {
            field = nodes.get(i);
            if (!field.isPublic())
                field.setModifiers(Modifier.PUBLIC | field.getModifiers()); // make the field public
            sootClasses.add(field.getDeclaringClass());
            Local newLocal = Jimple.v().newLocal("xzf" + i, field.getType());
            InstanceFieldRef ref = Jimple.v().newInstanceFieldRef(lastLocal, field.makeRef());
            Stmt stmt = Jimple.v().newAssignStmt(newLocal, ref);
            stmts.push(stmt);
            locals.add(newLocal);
            lastLocal = newLocal;
            if (i == 0) {
                lastAssignRef = ref;
                stmts.addAll(mutatePrimaryVar(lastLocal, locals));
            }
        }
        Stmt fieldAssign = Jimple.v().newAssignStmt(lastAssignRef, lastLocal);
        stmts.add(fieldAssign);
        return stmts;
    }

    private List<Stmt> mutateSingleForOnePath(FieldTreeNode path, Local local,
                                              Set<SootClass> sootClasses, Chain<Local> locals) {
        return iterPathForStmts(path, sootClasses, locals, local);
    }

    private List<Stmt> mutatePrimaryVar(Local local, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        switch (local.getType().toString()) {
            case "boolean":
                stmts.add(booleanInsert(local));
                break;
            case "byte":
                stmts.addAll(IntLikeInsert(local, locals, ByteType.v()));
                break;
            case "char":
                stmts.addAll(IntLikeInsert(local, locals, CharType.v()));
                break;
            case "short":
                stmts.addAll(IntLikeInsert(local, locals, ShortType.v()));
                break;
            case "int":
                stmts.add(intInsert(local));
                break;
            case "float":
                stmts.add(floatInsert(local));
                break;
            case "double":
                stmts.add(doubleInsert(local));
                break;
            case "long":
                stmts.add(longInsert(local));
                break;
            case "java.lang.String":
                Stmt strStmt = StringInsert(local);
                if (strStmt != null)
                    stmts.add(strStmt);
                break;
            default:// for array type without index
                stmts.addAll(commonCurrentTimeMillis(local, locals, local.getType().toString()));
                break;
        }
        return stmts;
    }

    private List<FieldTreeNode> BFS(FieldTreeNode node) {
        List<FieldTreeNode> paths = new ArrayList<>();
        Queue<FieldTreeNode> queue = new LinkedList<>();
        queue.add(node);
        FieldTreeNode temp;
        while (!queue.isEmpty()) {
            temp = queue.poll();
            if (temp.level >= maxLevel)
                break;
            for (SootField field : temp.declaringClass.getFields()) {
                switch (isFinalNode(field, temp.level + 1)) {
                    case can_not_mutate: // do nothing
                        break;
                    case final_state:
                        if (!(paths.size() >= times)) {
                            FieldTreeNode newNode = new FieldTreeNode(temp, field, null, temp.level + 1);
                            paths.add(newNode);
                        }
                        break;
                    case further_search:
                        SootClass newClass = Scene.v().loadClassAndSupport(field.getType().toString());
                        FieldTreeNode newNode = new FieldTreeNode(temp, field, newClass, temp.level + 1);
                        queue.add(newNode);
                        break;
                }
            }
        }
        return paths;
    }

    private SearchState isFinalNode(SootField field, int level) {
        // only for the first level, we accept primary matrix field
        // return -1 if it can not mutated
        // return 0 if it should further search
        // return 1 if it is final
        if (field.isFinal())
            return SearchState.can_not_mutate;
        if (field.getDeclaringClass().isJavaLibraryClass())
            return SearchState.can_not_mutate;
        String fieldType = field.getType().toString();
        if (fieldType.contains("[]")) {
            String subType = fieldType.substring(0, fieldType.indexOf("["));
            if (level == 1 && isPrimary(subType)) // static primary field, can be a matric
                return SearchState.final_state;
            else
                return SearchState.can_not_mutate;
        }

        if (isPrimary(fieldType))
            return SearchState.final_state;
        else
            return SearchState.further_search;
    }

}

class FieldTreeNode {

    FieldTreeNode pre;
    SootField field;
    int level;
    SootClass declaringClass;

    public FieldTreeNode(FieldTreeNode pre, SootField field, SootClass declaringClass, int level) {
        this.pre = pre;
        this.field = field;
        this.level = level;
        this.declaringClass = declaringClass;
    }
}
