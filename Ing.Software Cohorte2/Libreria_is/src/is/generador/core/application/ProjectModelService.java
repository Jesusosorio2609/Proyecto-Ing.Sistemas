package is.generador.core.application;

import is.generador.core.application.deduction.RelationshipDeductor;
import is.generador.core.application.resolver.NameScopeResolver;
import is.generador.core.application.resolver.RelationshipResolver;
import is.generador.core.domain.model.UmlModel;
import is.generador.core.ports.ProjectModelExtractor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ProjectModelService {
    private final ProjectModelExtractor extractor;
    private final RelationshipResolver relationshipResolver;

    public ProjectModelService(ProjectModelExtractor extractor,
            NameScopeResolver scopeResolver,
            List<RelationshipDeductor> deductors) {
        this.extractor = Objects.requireNonNull(extractor, "extractor");
        relationshipResolver = new RelationshipResolver(
                Objects.requireNonNull(scopeResolver, "scopeResolver"),
                List.copyOf(Objects.requireNonNull(deductors, "deductors")));
    }

    public UmlModel analyze(Path sourceDirectory) throws IOException {
        var context = extractor.extract(sourceDirectory);
        return relationshipResolver.resolveRelationships(
                context.resolutionRegistry(), context.getUnresolvedTypesLog());
    }
}
