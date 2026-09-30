package is.generador.core.application.registry;

import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.relationship.UmlRelationship;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ResolutionRegistryImpl implements ResolutionRegistry {
    private final Map<String, UmlClassifier> classifiers;
    private final List<UmlRelationship> rawAttempts = new ArrayList<>();

    public ResolutionRegistryImpl(Map<String, UmlClassifier> classifiers,
            Collection<UmlRelationship> initialStructuralRelationships) {
        this.classifiers = Map.copyOf(Objects.requireNonNull(classifiers, "classifiers"));
        rawAttempts.addAll(Objects.requireNonNull(initialStructuralRelationships,
                "initialStructuralRelationships"));
    }

    @Override public Map<String, UmlClassifier> availableClassifiers() { return classifiers; }
    @Override public Optional<UmlClassifier> findClassifier(String qualifiedName) {
        return Optional.ofNullable(classifiers.get(Objects.requireNonNull(qualifiedName, "qualifiedName")));
    }
    @Override public void registerRelationship(UmlRelationship relationship) {
        rawAttempts.add(Objects.requireNonNull(relationship, "relationship"));
    }
    @Override public Collection<UmlRelationship> resolvedRelationships() {
        return List.copyOf(computeWinners().values());
    }

    @Override public List<DeductionEvent> deductionEvents() {
        var winners = computeWinners();
        return rawAttempts.stream()
                .map(relationship -> new DeductionEvent(
                        relationship,
                        relationship.semanticWeight(),
                        winners.get(buildKey(relationship)) == relationship
                                ? DeductionStatus.ACCEPTED : DeductionStatus.OMITTED))
                .toList();
    }

    private Map<String, UmlRelationship> computeWinners() {
        Map<String, UmlRelationship> winners = new LinkedHashMap<>();
        for (var candidate : rawAttempts) {
            winners.merge(buildKey(candidate), candidate, (current, replacement) ->
                    replacement.semanticWeight().isStrongerThan(current.semanticWeight())
                            ? replacement : current);
        }
        return winners;
    }

    private String buildKey(UmlRelationship relationship) {
        return relationship.source().qualifiedName() + "->" + relationship.target().qualifiedName();
    }
}
