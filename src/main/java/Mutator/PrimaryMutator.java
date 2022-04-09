package Mutator;

import IsoVar.VariableInfo;
import fj.data.IO;
import soot.*;
import soot.baf.BafASMBackend;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocal;
import soot.options.Options;
import soot.util.Chain;

import java.io.*;
import java.util.*;

import static Instrument.InstrTransform.doInstr;
import static Instrument.Instrument.*;

public class PrimaryMutator extends Mutator {

    public PrimaryMutator(VariableInfo var, int times) {
        super(var, times);
    }

    public static Stmt floatInsert(Local left) {
        float random = new Random().nextFloat() * 256;
        Expr expr = Jimple.v().newAddExpr(left, FloatConstant.v(random));
        return Jimple.v().newAssignStmt(left, expr);
    }


    public static Stmt booleanInsert(Local local) {
        Value rValue = Jimple.v().newXorExpr(local, IntConstant.v(1));
        return Jimple.v().newAssignStmt(local, rValue);
    }

    public static List<Stmt> IntLikeInsert(Local left, Chain<Local> locals, Type type) {
        List<Stmt> stmts = new ArrayList<>();
        boolean isPos = new Random().nextBoolean();
        int random = new Random().nextInt(128);
        Local newLocal = Jimple.v().newLocal("vNew", IntType.v());
        locals.add(newLocal);
        Expr expr1, expr2;
        if (isPos) {
            expr1 = Jimple.v().newAddExpr(left, IntConstant.v(random));
        } else
            expr1 = Jimple.v().newSubExpr(left, IntConstant.v(random));
        Stmt stmt1 = Jimple.v().newAssignStmt(newLocal, expr1);
        stmts.add(stmt1);
        expr2 = Jimple.v().newCastExpr(newLocal, type);
        Stmt stmt2 = Jimple.v().newAssignStmt(left, expr2);
        stmts.add(stmt2);
        return stmts;
    }

    public static Stmt intInsert(Local left) {
        boolean isPos = new Random().nextBoolean();
        int random = new Random().nextInt(128);
        Expr expr;
        if (isPos) {
            expr = Jimple.v().newAddExpr(left, IntConstant.v(random));
        } else
            expr = Jimple.v().newSubExpr(left, IntConstant.v(random));
        return Jimple.v().newAssignStmt(left, expr);
    }

    public static Stmt doubleInsert(Local left) {
        double random = new Random().nextDouble() * 256;
        Expr expr = Jimple.v().newAddExpr(left, DoubleConstant.v(random));
        return Jimple.v().newAssignStmt(left, expr);
    }

    private static List<Stmt> floatMatrixInsert(Local local, Local new4, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        Stmt stmt5, stmt6, stmt7;
        Local new5, new6;
        Expr expr6;
        float random = new Random().nextFloat() * 256;

        new5 = Jimple.v().newLocal("xzf5", FloatType.v());
        Value array1 = Jimple.v().newArrayRef(local, new4);
        stmt5 = Jimple.v().newAssignStmt(new5, array1);

        new6 = Jimple.v().newLocal("xzf6", FloatType.v());
        expr6 = Jimple.v().newAddExpr(new5, FloatConstant.v(random));
        stmt6 = Jimple.v().newAssignStmt(new6, expr6);

        Value array2 = Jimple.v().newArrayRef(local, new4);
        stmt7 = Jimple.v().newAssignStmt(array2, new6);

        locals.add(new5);
        locals.add(new6);

        stmts.add(stmt5);
        stmts.add(stmt6);
        stmts.add(stmt7);
        return stmts;

    }

    public static Stmt longInsert(Local left) {
        boolean isPos = new Random().nextBoolean();
        long random = new Random().nextLong();
        Expr expr;
        if (isPos) {
            expr = Jimple.v().newAddExpr(left, LongConstant.v(random));
        } else
            expr = Jimple.v().newSubExpr(left, LongConstant.v(random));
        return Jimple.v().newAssignStmt(left, expr);
    }

    public static Stmt StringInsert(Local left, Value right) {
        if (right instanceof NullConstant)
            return null;
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String toConcat = String.valueOf(str.charAt(new Random().nextInt(62)));
        SootMethod concat = Scene.v().getMethod("<java.lang.String: java.lang.String concat(java.lang.String)>");
        Expr expr = Jimple.v().newVirtualInvokeExpr(left, concat.makeRef(), StringConstant.v(toConcat));
        return Jimple.v().newAssignStmt(left, expr);
    }

    public static Stmt StringInsert(Local left) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String toConcat = String.valueOf(str.charAt(new Random().nextInt(62)));
        SootMethod concat = Scene.v().getMethod("<java.lang.String: java.lang.String concat(java.lang.String)>");
        Expr expr = Jimple.v().newVirtualInvokeExpr(left, concat.makeRef(), StringConstant.v(toConcat));
        return Jimple.v().newAssignStmt(left, expr);
    }

    public static List<Stmt> commonCurrentTimeMillis(Local local, Chain<Local> locals, String desc) {
        List<Stmt> stmts = new ArrayList<>();
        Local new1, new2, new3, new4, new5, new6, new7, new8;
        Expr expr1, expr2, expr3, expr4, expr5, expr6, expr7, expr8;
        Stmt stmt1, stmt2, stmt3, stmt4, stmt5, stmt6, stmt7, stmt8;
        SootMethod currentTimeMillis = Scene.v().getMethod("<java.lang.System: long currentTimeMillis()>");
        new1 = Jimple.v().newLocal("xzf1", LongType.v());
        expr1 = Jimple.v().newStaticInvokeExpr(currentTimeMillis.makeRef());
        stmt1 = Jimple.v().newAssignStmt(new1, expr1);

        new2 = Jimple.v().newLocal("xzf2", IntType.v());
        expr2 = Jimple.v().newCastExpr(new1, IntType.v());
        stmt2 = Jimple.v().newAssignStmt(new2, expr2);

        new3 = Jimple.v().newLocal("xzf3", IntType.v());
        expr3 = Jimple.v().newLengthExpr(local);
        stmt3 = Jimple.v().newAssignStmt(new3, expr3);

        new4 = Jimple.v().newLocal("xzf4", IntType.v());
        expr4 = Jimple.v().newRemExpr(new2, new3);
        stmt4 = Jimple.v().newAssignStmt(new4, expr4);

        locals.add(new1);
        locals.add(new2);
        locals.add(new3);
        locals.add(new4);

        stmts.add(stmt1);
        stmts.add(stmt2);
        stmts.add(stmt3);
        stmts.add(stmt4);

        switch (desc) {
            case "boolean[]":
                stmts.addAll(booleanMatrixInsert(local, new4, locals));
                break;
            case "byte[]":
                stmts.addAll(intLikeMatrixInsert(local, new4, locals, ByteType.v()));
                break;
            case "char[]":
                stmts.addAll(intLikeMatrixInsert(local, new4, locals, CharType.v()));
                break;
            case "short[]":
                stmts.addAll(intLikeMatrixInsert(local, new4, locals, ShortType.v()));
                break;
            case "int[]":
                stmts.addAll(intMatrixInsert(local, new4, locals));
                break;
            case "float[]":
                stmts.addAll(floatMatrixInsert(local, new4, locals));
                break;
            case "double[]":
                stmts.addAll(doubleMatrixInsert(local, new4, locals));
                break;
            case "long[]":
                stmts.addAll(longMatrixInsert(local, new4, locals));
                break;
            case "java.lang.String[]":
                stmts.addAll(StringMatrixInsert(local, new4, locals));
                break;
        }

        return stmts;
    }

    private static List<Stmt> booleanMatrixInsert(Local local, Local new4, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        Stmt stmt5, stmt6, stmt7;
        Local new5, new6;
        Expr expr6;
        new5 = Jimple.v().newLocal("xzf5", BooleanType.v());
        Value array1 = Jimple.v().newArrayRef(local, new4);
        stmt5 = Jimple.v().newAssignStmt(new5, array1);

        new6 = Jimple.v().newLocal("xzf6", BooleanType.v());
        expr6 = Jimple.v().newXorExpr(new5, IntConstant.v(1));
        stmt6 = Jimple.v().newAssignStmt(new6, expr6);

        Value array2 = Jimple.v().newArrayRef(local, new4);
        stmt7 = Jimple.v().newAssignStmt(array2, new6);

        locals.add(new5);
        locals.add(new6);

        stmts.add(stmt5);
        stmts.add(stmt6);
        stmts.add(stmt7);
        return stmts;
    }

    private static List<Stmt> intLikeMatrixInsert(Local local, Local new4, Chain<Local> locals, Type type) {
        List<Stmt> stmts = new ArrayList<>();
        Stmt stmt5, stmt6, stmt7, stmt8;
        Local new5, new6, new7;
        Expr expr6, expr7;
        boolean isPos = new Random().nextBoolean();
        int random = new Random().nextInt(128);

        new5 = Jimple.v().newLocal("xzf5", type);
        Value array1 = Jimple.v().newArrayRef(local, new4);
        stmt5 = Jimple.v().newAssignStmt(new5, array1);

        new6 = Jimple.v().newLocal("xzf6", IntType.v());
        if (isPos) {
            expr6 = Jimple.v().newAddExpr(new5, IntConstant.v(random));
        } else
            expr6 = Jimple.v().newSubExpr(new5, IntConstant.v(random));
        stmt6 = Jimple.v().newAssignStmt(new6, expr6);

        new7 = Jimple.v().newLocal("xzf7", type);
        expr7 = Jimple.v().newCastExpr(new6, type);
        stmt7 = Jimple.v().newAssignStmt(new7, expr7);

        Value array2 = Jimple.v().newArrayRef(local, new4);
        stmt8 = Jimple.v().newAssignStmt(array2, new7);

        locals.add(new5);
        locals.add(new6);
        locals.add(new7);

        stmts.add(stmt5);
        stmts.add(stmt6);
        stmts.add(stmt7);
        stmts.add(stmt8);
        return stmts;
    }

    private static List<Stmt> intMatrixInsert(Local local, Local new4, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        Stmt stmt5, stmt6, stmt7;
        Local new5, new6;
        Expr expr6;
        boolean isPos = new Random().nextBoolean();
        int random = new Random().nextInt(128);

        new5 = Jimple.v().newLocal("xzf5", IntType.v());
        Value array1 = Jimple.v().newArrayRef(local, new4);
        stmt5 = Jimple.v().newAssignStmt(new5, array1);

        new6 = Jimple.v().newLocal("xzf6", IntType.v());
        if (isPos) {
            expr6 = Jimple.v().newAddExpr(new5, IntConstant.v(random));
        } else
            expr6 = Jimple.v().newSubExpr(new5, IntConstant.v(random));
        stmt6 = Jimple.v().newAssignStmt(new6, expr6);

        Value array2 = Jimple.v().newArrayRef(local, new4);
        stmt7 = Jimple.v().newAssignStmt(array2, new6);

        locals.add(new5);
        locals.add(new6);

        stmts.add(stmt5);
        stmts.add(stmt6);
        stmts.add(stmt7);
        return stmts;
    }

    private static List<Stmt> doubleMatrixInsert(Local local, Local new4, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        Stmt stmt5, stmt6, stmt7;
        Local new5, new6;
        Expr expr6;
        double random = new Random().nextDouble() * 256;

        new5 = Jimple.v().newLocal("xzf5", DoubleType.v());
        Value array1 = Jimple.v().newArrayRef(local, new4);
        stmt5 = Jimple.v().newAssignStmt(new5, array1);

        new6 = Jimple.v().newLocal("xzf6", DoubleType.v());
        expr6 = Jimple.v().newAddExpr(new5, DoubleConstant.v(random));
        stmt6 = Jimple.v().newAssignStmt(new6, expr6);

        Value array2 = Jimple.v().newArrayRef(local, new4);
        stmt7 = Jimple.v().newAssignStmt(array2, new6);

        locals.add(new5);
        locals.add(new6);

        stmts.add(stmt5);
        stmts.add(stmt6);
        stmts.add(stmt7);
        return stmts;
    }

    private static List<Stmt> longMatrixInsert(Local local, Local new4, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        Stmt stmt5, stmt6, stmt7;
        Local new5, new6;
        Expr expr6;
        boolean isPos = new Random().nextBoolean();
        long random = new Random().nextLong();

        new5 = Jimple.v().newLocal("xzf5", LongType.v());
        Value array1 = Jimple.v().newArrayRef(local, new4);
        stmt5 = Jimple.v().newAssignStmt(new5, array1);

        new6 = Jimple.v().newLocal("xzf6", LongType.v());
        if (isPos) {
            expr6 = Jimple.v().newAddExpr(new5, LongConstant.v(random));
        } else
            expr6 = Jimple.v().newSubExpr(new5, LongConstant.v(random));
        stmt6 = Jimple.v().newAssignStmt(new6, expr6);

        Value array2 = Jimple.v().newArrayRef(local, new4);
        stmt7 = Jimple.v().newAssignStmt(array2, new6);

        locals.add(new5);
        locals.add(new6);

        stmts.add(stmt5);
        stmts.add(stmt6);
        stmts.add(stmt7);
        return stmts;
    }

    private static List<Stmt> StringMatrixInsert(Local local, Local new4, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        Stmt stmt5, stmt6, stmt7;
        Local new5, new6;
        Expr expr6;

        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String toConcat = String.valueOf(str.charAt(new Random().nextInt(62)));
        SootMethod concat = Scene.v().getMethod("<java.lang.String: java.lang.String concat(java.lang.String)>");

        SootClass clazz = Scene.v().getSootClass("java.lang.String");
        new5 = Jimple.v().newLocal("xzf5", clazz.getType());
        Value array1 = Jimple.v().newArrayRef(local, new4);
        stmt5 = Jimple.v().newAssignStmt(new5, array1);

        new6 = Jimple.v().newLocal("xzf6", clazz.getType());
        expr6 = Jimple.v().newVirtualInvokeExpr(new5, concat.makeRef(), StringConstant.v(toConcat));
        stmt6 = Jimple.v().newAssignStmt(new6, expr6);

        Value array2 = Jimple.v().newArrayRef(local, new4);
        stmt7 = Jimple.v().newAssignStmt(array2, new6);

        locals.add(new5);
        locals.add(new6);

        stmts.add(stmt5);
        stmts.add(stmt6);
        stmts.add(stmt7);
        return stmts;
    }

    private void mutatePrimaryVar(Body body) {
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
                if (left instanceof JimpleLocal) {
                    JimpleLocal local = (JimpleLocal) left;
                    if (local.getName().equals(var.name)) {
                        point = unit;
                        switch (var.desc) {
                            case "boolean":
                                toInsert.add(booleanInsert(local));
                                break;
                            case "byte":
                                toInsert.addAll(IntLikeInsert(local, locals, ByteType.v()));
                                break;
                            case "char":
                                toInsert.addAll(IntLikeInsert(local, locals, CharType.v()));
                                break;
                            case "short":
                                toInsert.addAll(IntLikeInsert(local, locals, ShortType.v()));
                                break;
                            case "int":
                                toInsert.add(intInsert(local));
                                break;
                            case "float":
                                toInsert.add(floatInsert(local));
                                break;
                            case "double":
                                toInsert.add(doubleInsert(local));
                                break;
                            case "long":
                                toInsert.add(longInsert(local));
                                break;
                            case "java.lang.String":
                                Stmt strStmt = StringInsert(local, right);
                                if (strStmt != null)
                                    toInsert.add(strStmt);
                                break;
                            default:// for array type without index
                                toInsert.addAll(commonCurrentTimeMillis(local, locals, var.desc));
                                break;
                        }
                    }
                } else if (left instanceof ArrayRef && stmt instanceof AssignStmt) {
                    AssignStmt assignStmt = (AssignStmt) stmt;
                    Value base = ((ArrayRef) left).getBase();
                    if (base instanceof JimpleLocal) {
                        JimpleLocal baseLocal = (JimpleLocal) base;
                        String baseName = baseLocal.getName();
                        if (baseName.equals(var.name)) {
                            point = unit;
                            // for array type with index
                            switch (var.desc) {
                                case "boolean[]":
                                    toInsert.add(booleanMatrixWithIndexInsert(left, assignStmt, locals));
                                    break;
                                case "byte[]":
                                    toInsert.addAll(intLikeMatrixWithIndexInsert(left, assignStmt, locals, ByteType.v()));
                                    break;
                                case "char[]":
                                    toInsert.addAll(intLikeMatrixWithIndexInsert(left, assignStmt, locals, CharType.v()));
                                    break;
                                case "short[]":
                                    toInsert.addAll(intLikeMatrixWithIndexInsert(left, assignStmt, locals, ShortType.v()));
                                    break;
                                case "int[]":
                                    toInsert.addAll(intMatrixWithIndexInsert(left, assignStmt, locals));
                                    break;
                                case "float[]":
                                    toInsert.addAll(floatMatrixWithIndexInsert(left, assignStmt, locals));
                                    break;
                                case "double[]":
                                    toInsert.addAll(doubleMatrixWithIndexInsert(left, assignStmt, locals));
                                    break;
                                case "long[]":
                                    toInsert.addAll(longMatrixWithIndexInsert(left, assignStmt, locals));
                                    break;
                                case "java.lang.String[]":
                                    toInsert.addAll(StringMatrixWithIndexInsert(left, assignStmt, locals));
                                    break;
                            }
                        }
                    }

                }
            }
        }
        if (var.isParameter) {
            body.getUnits().insertBefore(toInsert, afterIdentifyPoint);
        } else {
            body.getUnits().insertAfter(toInsert, point);
        }
    }

    @Override
    public List<String> mutate(String mutantTemplate) {
        List<String> mutants = new ArrayList<>();
        for (int i = 1; i <= times; i++) {
            boolean existPhantom = false;
            SootClass clazz = Scene.v().loadClassAndSupport(var.className);
            for (SootMethod method : clazz.getMethods()) {
                if (method.isAbstract())
                    continue;
                if (method.isPhantom()) {
                    existPhantom = true;
                    break;
                }
                Body body = method.retrieveActiveBody();
                doInstr(body);

                if (byteCodeSunSignature(method).equals(var.methodName)) {
                    mutatePrimaryVar(body);
                }
                method.setActiveBody(body);
            }
            if (existPhantom) {
                Scene.v().removeClass(clazz);
                clazz = Scene.v().loadClassAndSupport(var.className);
                for (SootMethod method : clazz.getMethods()) {
                    if (method.isAbstract())
                        continue;
                    Body body = method.retrieveActiveBody();
                    doInstr(body);

                    if (byteCodeSunSignature(method).equals(var.methodName)) {
                        mutatePrimaryVar(body);
                    }
                    method.setActiveBody(body);
                }
            }
            String mutantPath = mutantTemplate + i + "/";
            String clazzPath = mutantPath + clazz.getName().replace(".", "/") + ".class";
            mutants.add(mutantPath);
            mkdirs(clazzPath);
            writeMutants(clazzPath, clazz);
            Scene.v().removeClass(clazz);
            if (var.desc.equals("boolean"))
                return mutants;
        }
        return mutants;
    }

    private Stmt booleanMatrixWithIndexInsert(Value left, AssignStmt stmt, Chain<Local> locals) {
        Local new1 = Jimple.v().newLocal("xzf1", BooleanType.v());
        stmt.setLeftOp(new1);

        Local new2 = Jimple.v().newLocal("xzf2", BooleanType.v());
        Expr expr = Jimple.v().newXorExpr(new1, IntConstant.v(1));
        locals.add(new1);
        locals.add(new2);
        return Jimple.v().newAssignStmt(left, expr);
    }

    private List<Stmt> intLikeMatrixWithIndexInsert(Value left, AssignStmt assignStmt, Chain<Local> locals, Type type) {
        List<Stmt> stmts = new ArrayList<>();
        Local new1 = Jimple.v().newLocal("xzf1", type);
        assignStmt.setLeftOp(new1);

        boolean isPos = new Random().nextBoolean();
        int random = new Random().nextInt(128);

        Local new2 = Jimple.v().newLocal("xzf2", IntType.v());
        Expr expr1;
        if (isPos)
            expr1 = Jimple.v().newAddExpr(new1, IntConstant.v(random));
        else
            expr1 = Jimple.v().newSubExpr(new1, IntConstant.v(random));
        Stmt stmt1 = Jimple.v().newAssignStmt(new2, expr1);

        Local new3 = Jimple.v().newLocal("xzf3", type);
        Expr expr2 = Jimple.v().newCastExpr(new2, type);
        Stmt stmt2 = Jimple.v().newAssignStmt(new3, expr2);
        Stmt stmt3 = Jimple.v().newAssignStmt(left, new3);

        locals.add(new1);
        locals.add(new2);
        locals.add(new3);

        stmts.add(stmt1);
        stmts.add(stmt2);
        stmts.add(stmt3);
        return stmts;
    }

    private List<Stmt> intMatrixWithIndexInsert(Value left, AssignStmt assignStmt, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        Local new1 = Jimple.v().newLocal("xzf1", IntType.v());
        assignStmt.setLeftOp(new1);

        boolean isPos = new Random().nextBoolean();
        int random = new Random().nextInt(128);

        Expr expr1;
        if (isPos)
            expr1 = Jimple.v().newAddExpr(new1, IntConstant.v(random));
        else
            expr1 = Jimple.v().newSubExpr(new1, IntConstant.v(random));
        Local new2 = Jimple.v().newLocal("xzf2", IntType.v());
        Stmt stmt1 = Jimple.v().newAssignStmt(new2, expr1);
        Stmt stmt2 = Jimple.v().newAssignStmt(left, new2);
        stmts.add(stmt1);
        stmts.add(stmt2);

        locals.add(new1);
        locals.add(new2);
        return stmts;
    }

    private List<Stmt> floatMatrixWithIndexInsert(Value left, AssignStmt assignStmt, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        Local new1 = Jimple.v().newLocal("xzf1", FloatType.v());
        assignStmt.setLeftOp(new1);

        float random = new Random().nextFloat() * 256;

        Expr expr1;
        expr1 = Jimple.v().newAddExpr(new1, FloatConstant.v(random));
        Local new2 = Jimple.v().newLocal("xzf2", FloatType.v());
        Stmt stmt1 = Jimple.v().newAssignStmt(new2, expr1);
        Stmt stmt2 = Jimple.v().newAssignStmt(left, new2);
        stmts.add(stmt1);
        stmts.add(stmt2);

        locals.add(new1);
        locals.add(new2);
        return stmts;
    }

    private List<Stmt> doubleMatrixWithIndexInsert(Value left, AssignStmt assignStmt, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        Local new1 = Jimple.v().newLocal("xzf1", DoubleType.v());
        assignStmt.setLeftOp(new1);

        double random = new Random().nextDouble() * 256;

        Expr expr1;
        expr1 = Jimple.v().newAddExpr(new1, DoubleConstant.v(random));
        Local new2 = Jimple.v().newLocal("xzf2", DoubleType.v());
        Stmt stmt1 = Jimple.v().newAssignStmt(new2, expr1);
        Stmt stmt2 = Jimple.v().newAssignStmt(left, new2);
        stmts.add(stmt1);
        stmts.add(stmt2);

        locals.add(new1);
        locals.add(new2);
        return stmts;
    }

    private List<Stmt> longMatrixWithIndexInsert(Value left, AssignStmt assignStmt, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        Local new1 = Jimple.v().newLocal("xzf1", LongType.v());
        assignStmt.setLeftOp(new1);

        boolean isPos = new Random().nextBoolean();
        long random = new Random().nextLong();

        Expr expr1;
        if (isPos)
            expr1 = Jimple.v().newAddExpr(new1, LongConstant.v(random));
        else
            expr1 = Jimple.v().newSubExpr(new1, LongConstant.v(random));
        Local new2 = Jimple.v().newLocal("xzf2", LongType.v());
        Stmt stmt1 = Jimple.v().newAssignStmt(new2, expr1);
        Stmt stmt2 = Jimple.v().newAssignStmt(left, new2);
        stmts.add(stmt1);
        stmts.add(stmt2);

        locals.add(new1);
        locals.add(new2);
        return stmts;
    }

    private List<Stmt> StringMatrixWithIndexInsert(Value left, AssignStmt assignStmt, Chain<Local> locals) {
        List<Stmt> stmts = new ArrayList<>();
        SootClass clazz = Scene.v().getSootClass("java.lang.String");
        Local new1 = Jimple.v().newLocal("xzf1", clazz.getType());
        assignStmt.setLeftOp(new1);

        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String toConcat = String.valueOf(str.charAt(new Random().nextInt(62)));
        SootMethod concat = Scene.v().getMethod("<java.lang.String: java.lang.String concat(java.lang.String)>");

        Expr expr = Jimple.v().newVirtualInvokeExpr(new1, concat.makeRef(), StringConstant.v(toConcat));
        Local new2 = Jimple.v().newLocal("xzf2", clazz.getType());
        Stmt stmt1 = Jimple.v().newAssignStmt(new2, expr);
        Stmt stmt2 = Jimple.v().newAssignStmt(left, new2);
        stmts.add(stmt1);
        stmts.add(stmt2);

        locals.add(new1);
        locals.add(new2);
        return stmts;
    }

}
