package Mutator;

import IsoVar.VariableInfo;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.List;

public abstract class Mutator {
    public static final SootClass counterClass;
    public static final SootMethod recordBB;

    static {
        counterClass = Scene.v().loadClassAndSupport("Counter");
        recordBB = counterClass.getMethod("void r(int,int,int)");
    }

    public VariableInfo var;
    int times;

    public Mutator(VariableInfo var, int times) {
        this.var = var;
        this.times = times;
    }


    public abstract List<String> mutate(String path);


}
