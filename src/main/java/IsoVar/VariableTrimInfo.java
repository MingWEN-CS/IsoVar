package IsoVar;

import polyglot.ast.Do;

public class VariableTrimInfo implements Comparable<VariableTrimInfo> {
//    public String name;
//    public String trueName;
//    public String desc;

//    public String className;
//    public  String methodSig;

    //    public int methodHash;
//    public boolean isClinit;
    public VariableInfo var;

    public double failingFreq;
    public double passingFreq;
    public double cosSim;
    public double statistical = 0; // failingFreq / (failingFreq + passingFreq)-alpha * cosSim

    public double failingDiff;
    public double passingDiff;
    public double mutate = 0; // failingDiff - beta * passingDiff

    public double suspicious = 0; //statistical+ gama * mutate

    public VariableTrimInfo(VariableInfo var) {
        this.var = var;
    }

    public VariableTrimInfo(VariableInfo var, double failingFreq, double passingFreq, double cosSim, double statistical, double failingDiff, double passingDiff, double mutate, double suspicious) {
        this.var = var;
        this.failingFreq = failingFreq;
        this.passingFreq = passingFreq;
        this.cosSim = cosSim;
        this.statistical = statistical;
        this.failingDiff = failingDiff;
        this.passingDiff = passingDiff;
        this.mutate = mutate;
        this.suspicious = suspicious;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        VariableTrimInfo other = (VariableTrimInfo) obj;
        String name = var.trueName==null?var.name: var.trueName;
        String oname = other.var.trueName==null?other.var.name: other.var.trueName;
        return name.equals(oname) && var.methodHash == other.var.methodHash;
    }

    @Override
    public int hashCode() {
        String name = var.trueName==null?var.name: var.trueName;
        return name.hashCode() + var.methodHash;
    }


    @Override
    public String toString() {
        return var.methodHash+" "+ var.trueName;
    }


    @Override
    public int compareTo(VariableTrimInfo o) {
        return Double.compare(this.suspicious,o.suspicious);
    }
}
