package is.generador.core.application.deduction;

import is.generador.core.application.context.PendingResolution;
import is.generador.core.application.registry.ResolutionRegistry;
import is.generador.core.application.resolver.NameScopeResolver;

public interface RelationshipDeductor {
    default int getSemanticWeight() { return 0; }
    void deduct(PendingResolution entry, ResolutionRegistry registryView,
            NameScopeResolver scopeResolver);
}
