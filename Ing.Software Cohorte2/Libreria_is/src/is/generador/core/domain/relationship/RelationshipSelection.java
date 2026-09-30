package is.generador.core.domain.relationship;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps the full inventory separate from the directed diagram projection. */
public final class RelationshipSelection {
    private RelationshipSelection() {}

    public static int priority(UmlRelationship relationship) {
        if (relationship instanceof UmlGeneralization) return 60;
        if (relationship instanceof UmlNesting) return 50;
        if (relationship instanceof UmlAssociation association) {
            return switch (association.aggregationKind()) {
                case COMPOSITE -> 40;
                case AGGREGATE -> 30;
                case NONE -> 20;
            };
        }
        return 10;
    }

    public static String kind(UmlRelationship relationship) {
        if (relationship instanceof UmlGeneralization generalization)
            return generalization.isInterfaceImplementation() ? "IMPLEMENTA" : "HEREDA";
        if (relationship instanceof UmlNesting) return "ANIDA";
        if (relationship instanceof UmlAssociation association) {
            return switch (association.aggregationKind()) {
                case COMPOSITE -> "COMPOSICION";
                case AGGREGATE -> "AGREGACION";
                case NONE -> "ASOCIACION";
            };
        }
        return "DEPENDENCIA";
    }

    public static String key(UmlRelationship relationship) {
        String key = relationship.source().qualifiedName() + "->"
                + relationship.target().qualifiedName() + "|" + kind(relationship);
        if (relationship instanceof UmlAssociation association)
            key += "|" + association.sourceMultiplicity() + "|" + association.targetMultiplicity();
        return key;
    }

    public static List<UmlRelationship> distinct(Collection<UmlRelationship> relationships) {
        Map<String, UmlRelationship> unique = new LinkedHashMap<>();
        relationships.stream().sorted(Comparator.comparing(RelationshipSelection::key))
                .forEach(relationship -> unique.putIfAbsent(key(relationship), relationship));
        return List.copyOf(unique.values());
    }

    public static List<UmlRelationship> strongest(Collection<UmlRelationship> relationships) {
        Map<String, UmlRelationship> winners = new LinkedHashMap<>();
        for (var candidate : distinct(relationships)) {
            String pair = candidate.source().qualifiedName() + "->" + candidate.target().qualifiedName();
            winners.merge(pair, candidate, (current, replacement) ->
                    priority(replacement) > priority(current) ? replacement : current);
        }
        return List.copyOf(winners.values());
    }
}
