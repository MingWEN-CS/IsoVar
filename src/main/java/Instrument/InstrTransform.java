package Instrument;

import IsoVar.MethodInfo;
import IsoVar.VariableInfo;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;

import java.util.*;

import static Instrument.Instrument.byteCodeSunSignature;
import static Instrument.Instrument.myHashCode;

public class InstrTransform {
    private static SootClass counterClass;
    private static SootMethod recordBB;

    // register Counter
    static {
        counterClass = Scene.v().loadClassAndSupport("Counter");
        // int hash, int index, int total
        recordBB = counterClass.getMethod("void r(int,int,int)");
    }

    private Map<MethodInfo, Set<VariableInfo>[]> methodMap;


    public InstrTransform(Map<MethodInfo, Set<VariableInfo>[]> methodMap) {
        this.methodMap = methodMap;
    }

    public static boolean isJVMGeneratedMethod(Body b) {
        int index = 0; // for generated interface override method, such as int compareTo(Object obj)
        for (Local local : b.getLocals()) {
            ++index;
            if (index == 2) {
                if (local.getName().equals("x0"))
                    return true;
                else break;
            }
        }

        // <org.joda.time.DateTimeFieldType: org.joda.time.DateTimeFieldType access$1000()>
        SootMethod method = b.getMethod();
        String methodName = method.getName();
        if (methodName.startsWith("access$"))
            return true;

        // for generated <clinit>
        if (methodName.equals("<clinit>")) {
            String clazz = method.getDeclaringClass().getName();
            char lastChar = clazz.charAt(clazz.length() - 1);
            return clazz.contains("$") && lastChar >= '0' && lastChar <= '9';
        }
        return false;
    }

    public static void doInstr(Body b) {
        SootMethod method = b.getMethod();
        if (isJVMGeneratedMethod(b))
            return;
        String subSig = byteCodeSunSignature(method);
        int hash = myHashCode(method.getDeclaringClass().getName()) +
                myHashCode(subSig);

        BlockGraph bg = new BriefBlockGraph(b);
        int uniqueIndex = 0;
        int total = bg.size();

        Set<Block> accessed = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        for (Block head : bg.getHeads()) {
            accessed.add(head);
            queue.offer(head);
        }
        while (!queue.isEmpty()) {
            Block block = queue.poll();
            insertExprBB(block, hash, total, uniqueIndex++);
            for (Block succs : bg.getSuccsOf(block)) {
                if (!accessed.contains(succs)) {
                    accessed.add(succs);
                    queue.offer(succs);
                }
            }
        }
    }


    public static Set<VariableInfo>[] doTransverse(Body b) {
        BlockGraph bg = new BriefBlockGraph(b);
        int uniqueIndex = 0;
        int total = bg.size();
        Set<VariableInfo>[] locals = new Set[total];
        Set<Block> accessed = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        for (Block head : bg.getHeads()) {
            accessed.add(head);
            queue.offer(head);
        }
        Map<String, VariableInfo> fieldLocalMap = new HashMap<>();
        while (!queue.isEmpty()) {
            Block block = queue.poll();
            Set<VariableInfo> set = traverserBlock(block, fieldLocalMap);
            locals[uniqueIndex++] = set;
            for (Block succs : bg.getSuccsOf(block)) {
                if (!accessed.contains(succs)) {
                    accessed.add(succs);
                    queue.offer(succs);
                }
            }
        }
        return locals;
    }

    private static void insertExprBB(Block block, int hash, int total, int index) {
        Unit point = null;
        for (Unit unit : block) {
            if (!(unit instanceof IdentityStmt)) {
                point = unit;
                break;
            }

        }
        InvokeExpr recordExpr = Jimple.v().newStaticInvokeExpr(
                recordBB.makeRef(),
                IntConstant.v(hash),
                IntConstant.v(total),
                IntConstant.v(index));
        Stmt stmt = Jimple.v().newInvokeStmt(recordExpr);
        if (point != null)
            block.insertBefore(stmt, point);
        else
            block.insertAfter(stmt, block.getTail());
    }

    protected static void internalTransform(Body b, Map<MethodInfo, Set<VariableInfo>[]> methodMap) {
//        if (!b.getMethod().hasActiveBody())
//            return;
        SootMethod method = b.getMethod();
        if (isJVMGeneratedMethod(b))
            return;

//        if (!failingAcc.contains(hash))
//            return;

        String subSig = byteCodeSunSignature(method);
        int hash = myHashCode(method.getDeclaringClass().getName()) +
                myHashCode(subSig);

        BlockGraph bg = new BriefBlockGraph(b);

        int uniqueIndex = 0;
        int total = bg.size();

        Set<VariableInfo>[] locals = new Set[total];
        Set<Block> accessed = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        for (Block head : bg.getHeads()) {
            accessed.add(head);
            queue.offer(head);
        }
        Map<String, VariableInfo> fieldLocalMap = new HashMap<>();
        while (!queue.isEmpty()) {
            Block block = queue.poll();
            Set<VariableInfo> set = traverserBlock(block, fieldLocalMap);
            locals[uniqueIndex] = set;
            insertExprBB(block, hash, total, uniqueIndex++);
            for (Block succs : bg.getSuccsOf(block)) {
                if (!accessed.contains(succs)) {
                    accessed.add(succs);
                    queue.offer(succs);
                }
            }
        }
        methodMap.putIfAbsent(new MethodInfo(method.getDeclaringClass().getName(),
                subSig, hash), locals);
    }

    private static boolean isOriginVar(String name) {
        return !name.startsWith("$");
    }

    private static Set<VariableInfo> traverserBlock(Block current, Map<String, VariableInfo> fieldLocalMap) {
        Set<VariableInfo> set = new HashSet<>();
        String toRmvObjName;
        for (Unit unit : current) {
            if (unit instanceof AssignStmt) {
                AssignStmt assign = (AssignStmt) unit;
                Value left = assign.getLeftOp();
                Value right = assign.getRightOp();
                if (left instanceof JimpleLocal) {
                    String leftV = ((JimpleLocal) left).getName();
                    if (right instanceof InstanceFieldRef) {
                        Value rightBase = ((InstanceFieldRef) right).getBase();
                        if (!isOriginVar(leftV) && rightBase instanceof JimpleLocal) {
                            JimpleLocal base = (JimpleLocal) rightBase;
                            if (isOriginVar(base.getName())) {
                                SootField field = ((FieldRef) right).getField();
                                VariableInfo var = new VariableInfo(base.getName() + "." + field.getName(),
                                        field.getDeclaringClass().toString() + ";" + field.getType());
                                fieldLocalMap.putIfAbsent(leftV, var);
                                continue;
                            }
                        }
                    } else if (right instanceof StaticFieldRef) {
                        if (!isOriginVar(leftV)) {
                            SootField field = ((FieldRef) right).getField();
                            VariableInfo var = new VariableInfo("." + field.getName(),
                                    field.getDeclaringClass().toString() + ";" + field.getType());
                            fieldLocalMap.putIfAbsent(leftV, var);
                            continue;
                        }
                    }
                }
            }

            if (unit instanceof Stmt) {
                toRmvObjName = null;
                Stmt stmt = (Stmt) unit;
                for (ValueBox def : stmt.getDefBoxes()) {
                    Value dv = def.getValue();
                    if (dv instanceof ArrayRef) {
                        Value base = ((ArrayRef) dv).getBase();
                        if (base instanceof JimpleLocal) {
                            JimpleLocal baseLocal = (JimpleLocal) base;
                            String baseName = baseLocal.getName();
                            if (isOriginVar(baseName)) {
                                VariableInfo var = new VariableInfo(stmt.getJavaSourceStartLineNumber(),
                                        baseName + "[]", base.getType().toString(), true);
                                set.add(var);
                                toRmvObjName = baseLocal.getName();
                            } else if (fieldLocalMap.get(baseName) != null) {
                                VariableInfo varGet = fieldLocalMap.get(baseName);
                                VariableInfo var = new VariableInfo(stmt.getJavaSourceStartLineNumber(),
                                        varGet.name + "[]", varGet.desc, true);
                                set.add(var);
                            }
                        }
                        Value index = ((ArrayRef) dv).getIndex();
                        if (index instanceof JimpleLocal) {
                            JimpleLocal indexLocal = (JimpleLocal) index;
                            if (isOriginVar(indexLocal.getName())) {
                                VariableInfo var = new VariableInfo(stmt.getJavaSourceStartLineNumber(),
                                        indexLocal.getName(), indexLocal.getType().toString(), false);
                                set.add(var);
                                toRmvObjName = indexLocal.getName();
                            }
                        }
                    } else if (dv instanceof JimpleLocal) {
                        JimpleLocal local = (JimpleLocal) dv;
                        if (!isOriginVar(local.getName()))
                            continue;
                        if (toRmvObjName != null && local.getName().equals(toRmvObjName))
                            continue;
                        set.add(new VariableInfo(stmt.getJavaSourceStartLineNumber(),
                                local.getName(), local.getType().toString(), true));

                    } else if (dv instanceof InstanceFieldRef) {
                        // only record field that base type is also an origin local variable
                        Value bv = ((InstanceFieldRef) dv).getBase();
                        if (bv instanceof JimpleLocal) {
                            JimpleLocal local = (JimpleLocal) bv;
                            if (local.getName().startsWith("$"))
                                continue;
                            SootField field = ((FieldRef) dv).getField();
                            VariableInfo var = new VariableInfo(stmt.getJavaSourceStartLineNumber()
                                    , local.getName() + "." + field.getName(),
                                    field.getDeclaringClass().toString() + ";" + field.getType(), true);
                            set.add(var);
                            toRmvObjName = local.getName();
                        }
                    } else if (dv instanceof StaticFieldRef) {
                        SootField field = ((FieldRef) dv).getField();
                        VariableInfo var = new VariableInfo(stmt.getJavaSourceStartLineNumber(),
                                "." + field.getName(), field.getDeclaringClass().toString() + ";" + field.getType(),
                                true);
                        set.add(var);
                    }
                }
                for (ValueBox def : stmt.getUseBoxes()) {
                    Value dv = def.getValue();
                    if (dv instanceof ArrayRef) {
                        Value base = ((ArrayRef) dv).getBase();
                        if (base instanceof JimpleLocal) {
                            JimpleLocal baseLocal = (JimpleLocal) base;
                            String baseName = baseLocal.getName();
                            if (isOriginVar(baseName)) {
                                VariableInfo var = new VariableInfo(stmt.getJavaSourceStartLineNumber(),
                                        baseName + "[]", base.getType().toString(), false);
                                set.add(var);
                                toRmvObjName = baseLocal.getName();
                            } else if (fieldLocalMap.get(baseName) != null) {
                                VariableInfo varGet = fieldLocalMap.get(baseName);
                                VariableInfo var = new VariableInfo(stmt.getJavaSourceStartLineNumber(),
                                        varGet.name + "[]", varGet.desc, true);
                                set.add(var);
                            }
                        }
                        Value index = ((ArrayRef) dv).getIndex();
                        if (index instanceof JimpleLocal) {
                            JimpleLocal indexLocal = (JimpleLocal) index;
                            if (isOriginVar(indexLocal.getName())) {
                                VariableInfo var = new VariableInfo(stmt.getJavaSourceStartLineNumber(),
                                        indexLocal.getName(), indexLocal.getType().toString(), false);
                                set.add(var);
                                toRmvObjName = indexLocal.getName();
                            }
                        }
                    } else if (dv instanceof JimpleLocal) {
                        JimpleLocal local = (JimpleLocal) dv;
                        String localName = local.getName();
                        if (isOriginVar(local.getName())) {
                            VariableInfo var = new VariableInfo(stmt.getJavaSourceStartLineNumber()
                                    , localName, local.getType().toString(), false);
                            if (localName.equals("this") && stmt.containsInvokeExpr()) {
//                                if (stmt.getInvokeExpr().getMethod().isConstructor())
                                continue;
                            }
                            if (toRmvObjName == null)
                                set.add(var);
                            else if (!localName.equals(toRmvObjName))
                                set.add(var);
                        } else if (fieldLocalMap.get(localName) != null) {
                            VariableInfo var = new VariableInfo(fieldLocalMap.get(localName),
                                    stmt.getJavaSourceStartLineNumber(), false);
                            set.add(var);
                        }

                    } else if (dv instanceof InstanceFieldRef) {
                        // only record field that base type is also a origin local variable
                        Value bv = ((InstanceFieldRef) dv).getBase();
                        if (bv instanceof JimpleLocal) {
                            JimpleLocal local = (JimpleLocal) bv;
                            if (local.getName().startsWith("$"))
                                continue;
                            SootField field = ((FieldRef) dv).getField();
                            VariableInfo var = new VariableInfo(stmt.getJavaSourceStartLineNumber(),
                                    local.getName() + "." + field.getName(),
                                    field.getDeclaringClass().toString() + ";" + field.getType(), false);
                            set.add(var);
                            toRmvObjName = local.getName();
                        }
                    } else if (dv instanceof StaticFieldRef) {
                        SootField field = ((FieldRef) dv).getField();
                        VariableInfo var = new VariableInfo(stmt.getJavaSourceStartLineNumber(),
                                "." + field.getName(), field.getDeclaringClass().toString() + ";" + field.getType().toString(),
                                false);
                        set.add(var);
                    } else if (dv instanceof InstanceInvokeExpr) {
                        InstanceInvokeExpr expr = (InstanceInvokeExpr) dv;
                        Value v = expr.getBase();
                        if (v instanceof JimpleLocal) {
                            JimpleLocal l = (JimpleLocal) v;
                            if (l.getName().equals("this"))
                                toRmvObjName = "this";
                        }
                    }
                }
            }
        }
        return set;
    }
}
