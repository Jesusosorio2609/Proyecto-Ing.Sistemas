package is.generador.core.application.resolver;

import is.generador.core.application.context.PendingResolution;
import is.generador.core.application.deduction.RelationshipDeductor;
import is.generador.core.application.registry.ResolutionRegistry;
import is.generador.core.domain.model.UmlModel;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RelationshipResolver {
    private final NameScopeResolver scopeResolver;
    private final List<RelationshipDeductor> deductors;
    private static final Logger logger = Logger.getLogger(RelationshipResolver.class.getName());

    public RelationshipResolver(NameScopeResolver scopeResolver,
            List<RelationshipDeductor> deductors) {
        this.scopeResolver = Objects.requireNonNull(scopeResolver, "scopeResolver");
        this.deductors = Objects.requireNonNull(deductors, "deductors").stream()
                .sorted(Comparator.comparingInt(RelationshipDeductor::getSemanticWeight).reversed())
                .toList();
    }

    public UmlModel resolveRelationships(ResolutionRegistry resolutionView,
            List<PendingResolution> unresolvedLog) {
        Objects.requireNonNull(resolutionView, "resolutionView");
        Objects.requireNonNull(unresolvedLog, "unresolvedLog");
        for (var entry : unresolvedLog) {
            for (var deductor : deductors) {
                try {
                    deductor.deduct(entry, resolutionView, scopeResolver);
                } catch (RuntimeException exception) {
                    logger.log(Level.WARNING, "Relationship deduction failed for " + entry, exception);
                }
            }
        }
        return new UmlModel(resolutionView.availableClassifiers(),
                Set.copyOf(resolutionView.resolvedRelationships()));
    }
}
