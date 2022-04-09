package IsoVar;

public class MethodInfo {
    public String className;
    public String subSignature;
    public int hash;

    public MethodInfo(String className, String subSignature, int hash) {
        this.className = className;
        this.subSignature = subSignature;
        this.hash = hash;
    }

    @Override
    public String toString() {
        return className + "\t" + subSignature + "\t" + hash;
    }
}
