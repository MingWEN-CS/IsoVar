package Instrument;

import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;

import java.util.Map;

import static Instrument.InstrTransform.isJVMGeneratedMethod;
import static Instrument.Instrument.byteCodeSunSignature;
import static Instrument.Instrument.myHashCode;

public class FailingAccessTransForm extends BodyTransformer {

    private static SootClass counterClass;
    private static SootMethod recordM;

    // register Counter
    static {
        counterClass = Scene.v().loadClassAndSupport("Counter");
        // int hash, int index, int total
        recordM = counterClass.getMethod("void m(int)");
    }


    public FailingAccessTransForm() {
    }

    @Override
    protected void internalTransform(Body b, String s, Map<String, String> map) {
        if (!b.getMethod().hasActiveBody())
            return;
        SootMethod method = b.getMethod();
        if (isJVMGeneratedMethod(b))
            return;
        int hash = myHashCode(method.getDeclaringClass().getName()) +
                myHashCode(byteCodeSunSignature(method));
        BlockGraph bg = new BriefBlockGraph(b);
        for (Block head : bg.getHeads()) {
            insertExprM(head, hash);
        }
    }


    private void insertExprM(Block block, int hash) {
        InvokeExpr recordExpr = Jimple.v().newStaticInvokeExpr(
                recordM.makeRef(),
                IntConstant.v(hash));
        Stmt stmt = Jimple.v().newInvokeStmt(recordExpr);
        block.insertBefore(stmt, block.getHead());
    }
}
