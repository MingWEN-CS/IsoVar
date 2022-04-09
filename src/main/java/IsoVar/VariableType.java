package IsoVar;

public enum VariableType {
    primary,
    primary_matrix,
    object_single, // such as "this"
    object_instance, // such as "this.x"
    object_static, // such as ".MIDNIGHT"
    object_matrix,
    multiArray;

    public boolean isMutableObject(VariableType type) {
        return type == object_single || type == object_instance || type == object_static;
    }
}
