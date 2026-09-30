package is.generador.core.application.deduction;

import is.generador.core.application.context.PendingResolution;
import is.generador.core.application.registry.ResolutionRegistry;
import is.generador.core.application.resolver.NameScopeResolver;
import is.generador.core.domain.relationship.UmlGeneralization;
import java.util.Optional;

public final class HierarchyRelationshipDeductor implements RelationshipDeductor {

    @Override
    public int getSemanticWeight() {
        return 40;
    }

    @Override
    public void deduct(PendingResolution entry, ResolutionRegistry registry,
            NameScopeResolver scopeResolver) {
        if (!entry.isHierarchyClaim()) {
            return;
        }
        registry.findClassifier(entry.sourceClassifierFqn()).ifPresent(source ->
                scopeResolver.resolveFqn(entry.rawTypeName(), source,
                        entry.contextImports(), registry.availableClassifiers())
                        .flatMap(registry::findClassifier)
                        .ifPresent(target -> registry.registerRelationship(
                        new UmlGeneralization(source, target, Optional.empty(),
                                Optional.empty(),
                                entry.isInterfaceImplementation()))));
    }
}
