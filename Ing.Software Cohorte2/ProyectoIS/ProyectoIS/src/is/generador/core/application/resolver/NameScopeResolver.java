package is.generador.core.application.resolver;

import is.generador.core.domain.classifier.UmlClassifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface NameScopeResolver {
    Optional<String> resolveFqn(String rawTypeName, UmlClassifier sourceClassifier,
            List<String> contextImports,
            Map<String, UmlClassifier> availableClassifiers);
}
