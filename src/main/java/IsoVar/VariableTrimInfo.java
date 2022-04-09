package IsoVar;

public class VariableTrimInfo {
    public String name;
    public String desc;
    public int methodHash;
    public boolean isClinit;

    public double failingFreq;
    public double passingFreq;
    public double cosSim;
    public double statistical = 0; // failingFreq / (failingFreq + passingFreq)-alpha * cosSim

    public double failingDiff;
    public double passingDiff;
    public double mutate = 0; // failingDiff - beta * passingDiff

    public double suspicious = 0; //statistical+ gama * mutate

    public VariableTrimInfo(String name, String desc, int methodHash, boolean isClinit) {
        this.desc = desc;
        this.name = name;
        this.methodHash = methodHash;
        this.isClinit = isClinit;
    }

    public VariableTrimInfo(String name, int methodHash) {
        this.name = name;
        this.methodHash = methodHash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        VariableTrimInfo other = (VariableTrimInfo) obj;
        return name.equals(other.name) && methodHash == other.methodHash;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + methodHash;
    }
}
