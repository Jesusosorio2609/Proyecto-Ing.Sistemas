package is.generador.core.domain.classifier;

import is.generador.core.domain.element.UmlNamespace;
import is.generador.core.domain.feature.UmlOperation;
import is.generador.core.domain.feature.UmlProperty;
import is.generador.core.domain.spec.UmlClassification;
import is.generador.core.domain.spec.UmlModifier;
import is.generador.core.domain.spec.UmlVisibility;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class UmlConcreteClassifier extends UmlClassifier {
    public UmlConcreteClassifier(String name, UmlNamespace namespace, UmlVisibility visibility,
            UmlClassification classification, List<String> templateParameters,
            Set<UmlModifier> modifiers, Optional<UmlClassifier> nestingClassifier,
            List<UmlProperty> properties, List<UmlOperation> operations,
            Optional<String> stereotype, Optional<String> note) {
        super(name, namespace, visibility, classification, templateParameters, modifiers,
                nestingClassifier, properties, operations, stereotype, note);
    }
}
