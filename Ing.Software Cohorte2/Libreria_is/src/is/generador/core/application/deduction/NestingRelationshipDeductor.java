package is.generador.core.application.deduction;

import is.generador.core.application.context.PendingResolution;
import is.generador.core.application.registry.ResolutionRegistry;
import is.generador.core.application.resolver.NameScopeResolver;
import is.generador.core.domain.relationship.UmlNesting;
import java.util.Optional;

public final class NestingRelationshipDeductor implements RelationshipDeductor {

    @Override
    public int getSemanticWeight() {
        return 30;
    }

    @Override
    public void deduct(PendingResolution entry, ResolutionRegistry registry,
            NameScopeResolver scopeResolver) {
        if (!entry.isNestingClaim()) {
            return;
        }
        registry.findClassifier(entry.sourceClassifierFqn()).ifPresent(outer ->
                registry.findClassifier(entry.rawTypeName()).ifPresent(inner ->
                        registry.registerRelationship(new UmlNesting(
                                outer, inner, entry.isStaticNesting(),
                                Optional.empty(), Optional.empty()))));
    }
}
