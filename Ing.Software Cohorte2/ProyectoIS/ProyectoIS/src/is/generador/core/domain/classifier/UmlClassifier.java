package is.generador.core.domain.classifier;

import is.generador.core.domain.element.UmlElement;
import is.generador.core.domain.element.UmlNamespace;
import is.generador.core.domain.feature.UmlOperation;
import is.generador.core.domain.feature.UmlProperty;
import is.generador.core.domain.spec.UmlClassification;
import is.generador.core.domain.spec.UmlModifier;
import is.generador.core.domain.spec.UmlVisibility;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public abstract class UmlClassifier extends UmlElement {
    private final UmlVisibility visibility;
    private final UmlClassification classification;
    private final List<String> templateParameters;
    private final Set<UmlModifier> modifiers;
    private final Optional<UmlClassifier> nestingClassifier;
    private final List<UmlProperty> properties;
    private final List<UmlOperation> operations;

    protected UmlClassifier(String name, UmlNamespace namespace, UmlVisibility visibility,
            UmlClassification classification, List<String> templateParameters,
            Set<UmlModifier> modifiers, Optional<UmlClassifier> nestingClassifier,
            List<UmlProperty> properties, List<UmlOperation> operations,
            Optional<String> stereotype, Optional<String> note) {
        super(name, namespace, stereotype, note);
        this.visibility = Objects.requireNonNull(visibility, "visibility");
        this.classification = Objects.requireNonNull(classification, "classification");
        this.templateParameters = List.copyOf(Objects.requireNonNull(templateParameters, "templateParameters"));
        this.modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
        this.nestingClassifier = Objects.requireNonNull(nestingClassifier, "nestingClassifier");
        this.properties = List.copyOf(Objects.requireNonNull(properties, "properties"));
        this.operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
    }

    public UmlVisibility visibility() { return visibility; }
    public UmlClassification classification() { return classification; }
    public List<String> templateParameters() { return templateParameters; }
    public Set<UmlModifier> modifiers() { return modifiers; }
    public Optional<UmlClassifier> nestingClassifier() { return nestingClassifier; }
    public List<UmlProperty> properties() { return properties; }
    public List<UmlOperation> operations() { return operations; }
    public boolean isAbstract() { return modifiers.contains(UmlModifier.ABSTRACT); }
    public boolean isLeaf() { return modifiers.contains(UmlModifier.LEAF); }
    public boolean isStatic() { return modifiers.contains(UmlModifier.STATIC); }
}
