package is.generador.core.application.deduction;

import is.generador.core.application.context.PendingResolution;
import is.generador.core.application.context.ResolutionKind;
import is.generador.core.application.registry.ResolutionRegistry;
import is.generador.core.application.resolver.NameScopeResolver;
import is.generador.core.domain.relationship.UmlAssociation;
import is.generador.core.domain.spec.AggregationKind;
import java.util.Optional;

public final class AssociationRelationshipDeductor implements RelationshipDeductor {

    @Override
    public int getSemanticWeight() {
        return 20;
    }

    @Override
    public void deduct(PendingResolution entry, ResolutionRegistry registry,
            NameScopeResolver scopeResolver) {
        if (entry.kind() != ResolutionKind.ASSOCIATION) {
            return;
        }
        registry.findClassifier(entry.sourceClassifierFqn()).ifPresent(source ->
                scopeResolver.resolveFqn(entry.rawTypeName(), source,
                        entry.contextImports(), registry.availableClassifiers())
                        .flatMap(registry::findClassifier)
                        .ifPresent(target -> registry.registerRelationship(
                        new UmlAssociation(source, "1", target, entry.targetMultiplicity(),
                                entry.aggregationKind(),
                                Optional.empty(), Optional.empty()))));
    }
}
