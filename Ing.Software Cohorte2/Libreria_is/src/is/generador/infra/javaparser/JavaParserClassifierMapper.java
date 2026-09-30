package is.generador.infra.javaparser;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.classifier.UmlConcreteClassifier;
import is.generador.core.domain.element.UmlNamespace;
import is.generador.core.domain.feature.UmlOperation;
import is.generador.core.domain.feature.UmlParameter;
import is.generador.core.domain.feature.UmlProperty;
import is.generador.core.domain.spec.AggregationKind;
import is.generador.core.domain.spec.UmlClassification;
import is.generador.core.domain.spec.UmlModifier;
import is.generador.core.domain.spec.UmlVisibility;
import is.generador.core.domain.type.UmlType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class JavaParserClassifierMapper {

    private final JavaParserTypeMapper typeMapper;

    JavaParserClassifierMapper(JavaParserTypeMapper typeMapper) {
        this.typeMapper = Objects.requireNonNull(typeMapper, "typeMapper");
    }

    UmlConcreteClassifier map(TypeDeclaration<?> declaration,
            UmlNamespace namespace, Optional<UmlClassifier> nestingClassifier) {
        Objects.requireNonNull(declaration, "declaration");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(nestingClassifier, "nestingClassifier");

        List<String> templateParameters = templateParametersOf(declaration);
        Set<String> templateParameterNames = new LinkedHashSet<>(templateParameters);

        return new UmlConcreteClassifier(
                declaration.getNameAsString(),
                namespace,
                JavaParserSpecMapper.toVisibility(
                        (NodeWithAccessModifiers<?>) declaration),
                classificationOf(declaration),
                templateParameters,
                JavaParserSpecMapper.toClassifierModifiers(
                        (NodeWithModifiers<?>) declaration),
                nestingClassifier,
                propertiesOf(declaration, templateParameterNames),
                operationsOf(declaration, templateParameterNames),
                Optional.empty(),
                Optional.empty());
    }

    private List<String> templateParametersOf(TypeDeclaration<?> declaration) {
        if (declaration instanceof NodeWithTypeParameters<?> parameterized) {
            return parameterized.getTypeParameters().stream()
                    .map(parameter -> parameter.getNameAsString())
                    .toList();
        }
        return List.of();
    }

    private UmlClassification classificationOf(TypeDeclaration<?> declaration) {
        if (declaration instanceof ClassOrInterfaceDeclaration type) {
            return type.isInterface()
                    ? UmlClassification.INTERFACE : UmlClassification.CLASS;
        }
        if (declaration instanceof EnumDeclaration) {
            return UmlClassification.ENUMERATION;
        }
        if (declaration instanceof RecordDeclaration) {
            return UmlClassification.RECORD;
        }
        if (declaration instanceof AnnotationDeclaration) {
            return UmlClassification.ANNOTATION;
        }
        throw new IllegalArgumentException(
                "Unsupported declaration: " + declaration.getClass().getName());
    }

    private List<UmlProperty> propertiesOf(TypeDeclaration<?> declaration,
            Set<String> templateParameters) {
        List<UmlProperty> properties = new ArrayList<>();
        for (var member : declaration.getMembers()) {
            if (member instanceof FieldDeclaration field) {
                field.getVariables().forEach(variable -> properties.add(
                        new UmlProperty(
                                variable.getNameAsString(),
                                JavaParserSpecMapper.toVisibility(field),
                                typeMapper.map(variable.getType(), templateParameters),
                                JavaParserSpecMapper.toPropertyModifiers(field),
                                variable.getInitializer().map(Object::toString),
                                Optional.<AggregationKind>empty())));
            }
        }
        if (declaration instanceof RecordDeclaration record) {
            record.getParameters().forEach(parameter -> properties.add(
                    new UmlProperty(
                            parameter.getNameAsString(),
                            UmlVisibility.PRIVATE,
                            typeMapper.map(parameter.getType(), templateParameters),
                            Set.of(UmlModifier.READ_ONLY),
                            Optional.empty(),
                            Optional.empty())));
        }
        if (declaration instanceof EnumDeclaration enumeration) {
            enumeration.getEntries().forEach(entry -> properties.add(
                    new UmlProperty(
                            entry.getNameAsString(),
                            UmlVisibility.PUBLIC,
                            new UmlType.Unknown(enumeration.getNameAsString()),
                            Set.of(UmlModifier.STATIC, UmlModifier.READ_ONLY),
                            Optional.empty(),
                            Optional.empty())));
        }
        return List.copyOf(properties);
    }

    private List<UmlOperation> operationsOf(TypeDeclaration<?> declaration,
            Set<String> classifierTemplateParameters) {
        List<UmlOperation> operations = new ArrayList<>();
        for (var member : declaration.getMembers()) {
            if (member instanceof MethodDeclaration method) {
                Set<String> parameters = new LinkedHashSet<>(classifierTemplateParameters);
                method.getTypeParameters().forEach(
                        parameter -> parameters.add(parameter.getNameAsString()));
                operations.add(new UmlOperation(
                        method.getNameAsString(),
                        JavaParserSpecMapper.toVisibility(method),
                        typeMapper.map(method.getType(), parameters),
                        parametersOf(method.getParameters(), parameters),
                        JavaParserSpecMapper.toOperationModifiers(method),
                        Optional.empty()));
            } else if (member instanceof ConstructorDeclaration constructor) {
                operations.add(new UmlOperation(
                        constructor.getNameAsString(),
                        JavaParserSpecMapper.toVisibility(constructor),
                        new UmlType.Unknown(declaration.getNameAsString()),
                        parametersOf(constructor.getParameters(),
                                classifierTemplateParameters),
                        JavaParserSpecMapper.toOperationModifiers(constructor),
                        Optional.of("constructor")));
            } else if (member instanceof CompactConstructorDeclaration constructor) {
                operations.add(new UmlOperation(
                        constructor.getNameAsString(),
                        JavaParserSpecMapper.toVisibility(constructor),
                        new UmlType.Unknown(declaration.getNameAsString()),
                        List.of(),
                        JavaParserSpecMapper.toOperationModifiers(constructor),
                        Optional.of("constructor")));
            } else if (member instanceof AnnotationMemberDeclaration annotationMember) {
                operations.add(new UmlOperation(
                        annotationMember.getNameAsString(),
                        UmlVisibility.PUBLIC,
                        typeMapper.map(annotationMember.getType(),
                                classifierTemplateParameters),
                        List.of(),
                        Set.of(UmlModifier.ABSTRACT),
                        Optional.of("annotationMember")));
            }
        }
        return List.copyOf(operations);
    }

    private List<UmlParameter> parametersOf(
            List<com.github.javaparser.ast.body.Parameter> parameters,
            Set<String> templateParameters) {
        return parameters.stream()
                .map(parameter -> new UmlParameter(
                parameter.getNameAsString(),
                parameter.isVarArgs()
                        ? new UmlType.Array(
                                typeMapper.map(parameter.getType(), templateParameters), 1)
                        : typeMapper.map(parameter.getType(), templateParameters)))
                .toList();
    }
}
