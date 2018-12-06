package dlang;
enum TypeIndicator {
    INT("int"), REAL("real"), BOOL("bool"), STRING("string"),
    EMPTY("empty"), ARRAY("array"), TUPLE("tuple"), FUNC("func");

    String name;

    TypeIndicator(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return this.name;
    }
}