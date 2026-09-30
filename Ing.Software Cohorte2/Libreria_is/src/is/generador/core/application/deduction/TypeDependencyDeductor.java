package is.generador.core.application.deduction;

import is.generador.core.application.context.PendingResolution;
import is.generador.core.application.context.ResolutionKind;
import is.generador.core.application.registry.ResolutionRegistry;
import is.generador.core.application.resolver.NameScopeResolver;
import is.generador.core.domain.relationship.UmlDependency;
import java.util.Optional;

public final class TypeDependencyDeductor implements RelationshipDeductor {

    @Override
    public int getSemanticWeight() {
        return 10;
    }

    @Override
    public void deduct(PendingResolution entry, ResolutionRegistry registry,
            NameScopeResolver scopeResolver) {
        if (entry.kind() != ResolutionKind.TYPE_DEPENDENCY) {
            return;
        }
        registry.findClassifier(entry.sourceClassifierFqn()).ifPresent(source ->
                scopeResolver.resolveFqn(entry.rawTypeName(), source,
                        entry.contextImports(), registry.availableClassifiers())
                        .flatMap(registry::findClassifier)
                        .filter(target -> target != source)
                        .ifPresent(target -> registry.registerRelationship(
                        new UmlDependency(source, target,
                                Optional.empty(), Optional.empty()))));
    }
}
