package IsoVar;

public class VariableInfo {
    public int methodHash;
    public boolean isField = false;
    public boolean isArrayRef = false;
    public boolean isParameter = false;
    public boolean isClinit = false;
    public VariableType type;
    public boolean isDef;
    int line;
    public String className, methodName, name, desc, trueName; // true name denotes the recovered name by asm

    public void setDef(boolean def) {
        isDef = def;
    }

    public VariableInfo(String str, int methodHash, String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
        isClinit = methodName.equals("void <clinit>()");
        this.methodHash = methodHash;
        int right = str.indexOf(")");
        this.desc = str.substring(1, right);
        str = str.substring(right + 1);
        if (str.contains(".")) {
            this.isField = true;
        }
        if (str.contains("@")) {
            this.isDef = true;
            String[] lineSplit = str.split("@");
            String name = lineSplit[0];
            if (name.contains("[]")) {
                name = name.substring(0, name.indexOf('['));
                this.isArrayRef = true;
            }
            handleRecoverName(name);
            this.line = Integer.parseInt(lineSplit[1]);
        } else if (str.contains(":")) {
            this.isDef = false;
            String[] lineSplit = str.split(":");
            String name = lineSplit[0];
            if (name.contains("[]")) {
                name = name.substring(0, name.indexOf('['));
                this.isArrayRef = true;
            }
            handleRecoverName(name);
            this.line = Integer.parseInt(lineSplit[1]);
        }

        if (line == -1) {
            this.isParameter = true;
        }
        type = decideType();
    }

    public VariableInfo(int line, String name, String desc, boolean isDef) {
        this.line = line;
        this.name = name;
        this.desc = desc;
        this.isDef = isDef;
    }

    public void handleRecoverName(String name) {
        int pos = name.indexOf('^');
        if (pos == -1)
            this.name = name;
        else {
            this.name = name.substring(0, pos);
            this.trueName = name.substring(pos + 1);
        }
    }

    public VariableInfo(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public VariableInfo(VariableInfo var, int line, boolean isDef) {
        this.name = var.name;
        this.desc = var.desc;
        this.line = line;
        this.isDef = isDef;
    }

    public VariableInfo(String name, int hash) {
        this.name = name;
        this.methodHash = hash;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public boolean isDef() {
        return isDef;
    }

    private VariableType decideType() {
        if (desc.contains(";")) {
            // instance or static field
            String subType = desc.substring(desc.indexOf(";") + 1);
            if (subType.endsWith("[]"))
                return VariableType.object_matrix;
            else if (name.startsWith("."))
                return VariableType.object_static;
            else if (name.contains("."))
                return VariableType.object_instance;
        } else if (desc.endsWith("[][]")) {
            return VariableType.multiArray;
        } else if (desc.endsWith("[]")) {
            String subType = desc.substring(0, desc.indexOf("[]"));
            if (isPrimary(subType))
                return VariableType.primary_matrix;
            else
                return VariableType.object_matrix;
        } else {
            if (isPrimary(desc))
                return VariableType.primary;
            else
                return VariableType.object_single;
        }
        return null;
    }

    public static boolean isPrimary(String str) {
        return str.equals("java.lang.String") ||
                str.equals("boolean") ||
                str.equals("short") ||
                str.equals("byte") ||
                str.equals("char") ||
                str.equals("int") ||
                str.equals("long") ||
                str.equals("float") ||
                str.equals("double");
    }


    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public VariableType getType() {
        return type;
    }

    public void setType(VariableType type) {
        this.type = type;
    }


    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        VariableInfo other = (VariableInfo) obj;
        return name.equals(other.name) && line == other.line;
    }

}
