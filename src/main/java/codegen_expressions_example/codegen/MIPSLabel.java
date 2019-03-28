package codegen_expressions_example.codegen;

public class MIPSLabel implements MIPSEntry {
    public final String baseName;
    public final int id;

    public MIPSLabel(final String baseName,
                     final int id) {
        this.baseName = baseName;
        this.id = id;
    }

    public String toString() {
        return baseName + id;
    }

    public boolean equals(final Object other) {
        if (other instanceof MIPSLabel) {
            final MIPSLabel label = (MIPSLabel)other;
            return (baseName.equals(label.baseName) &&
                    id == label.id);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return baseName.hashCode() + id;
    }
}
