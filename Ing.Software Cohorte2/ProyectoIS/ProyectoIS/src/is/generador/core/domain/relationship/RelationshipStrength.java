package is.generador.core.domain.relationship;

public enum RelationshipStrength {
    WEAK(1, "weak"), MEDIUM(2, "medium"), STRONG(3, "strong");

    private final int weight;
    private final String code;

    RelationshipStrength(int weight, String code) { this.weight = weight; this.code = code; }
    public int getWeight() { return weight; }
    public boolean isStrongerThan(RelationshipStrength other) { return other != null && weight > other.weight; }
    public String toCode() { return code; }
}
