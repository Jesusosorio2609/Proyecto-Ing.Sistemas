package is.generador.infra.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.WildcardType;
import is.generador.core.application.context.ResolutionContext;
import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.element.UmlNamespace;
import is.generador.core.domain.spec.AggregationKind;
import is.generador.core.ports.DomainPolicyProvider;
import is.generador.core.ports.ProjectModelExtractor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class JavaParserProjectModelExtractor implements ProjectModelExtractor {

    private final DomainPolicyProvider policyProvider;
    private final JavaParser parser;
    private final JavaParserClassifierMapper classifierMapper;
    private final Set<String> excludedPackages;
    private final Set<String> excludedClasses;

    public JavaParserProjectModelExtractor(DomainPolicyProvider policyProvider) {
        this(policyProvider, Set.of(), Set.of());
    }

    public JavaParserProjectModelExtractor(DomainPolicyProvider policyProvider,
            Set<String> excludedPackages, Set<String> excludedClasses) {
        this.policyProvider = Objects.requireNonNull(policyProvider, "policyProvider");
        this.excludedPackages = Set.copyOf(Objects.requireNonNull(
                excludedPackages, "excludedPackages"));
        this.excludedClasses = Set.copyOf(Objects.requireNonNull(
                excludedClasses, "excludedClasses"));
        parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
        classifierMapper = new JavaParserClassifierMapper(
                new JavaParserTypeMapper());
    }

    @Override
    public ResolutionContext extract(Path sourceDirectory) throws IOException {
        Objects.requireNonNull(sourceDirectory, "sourceDirectory");
        if (!Files.isDirectory(sourceDirectory)) {
            throw new IOException("Source directory does not exist: "
                    + sourceDirectory.toAbsolutePath());
        }

        ResolutionContext context = new ResolutionContext(Map.of(), policyProvider);
        try (Stream<Path> paths = Files.walk(sourceDirectory)) {
            for (Path file : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList()) {
                extractFile(file, context);
            }
        }
        List<String> errors = context.diagnosticTracker().getReport().stream()
                .filter(message -> message.startsWith("ERROR ")).toList();
        if (!errors.isEmpty()) throw new IOException(String.join(System.lineSeparator(), errors));
        return context.freeze();
    }

    private void extractFile(Path file, ResolutionContext context) {
        try {
            ParseResult<CompilationUnit> result = parser.parse(file);
            if (result.getResult().isEmpty()) {
                context.reportWarning(file.toString(),
                        "Could not parse source: " + result.getProblems());
                return;
            }
            if (!result.isSuccessful()) {
                context.reportWarning(file.toString(),
                        "Source parsed with problems: " + result.getProblems());
            }

            CompilationUnit unit = result.getResult().orElseThrow();
            UmlNamespace namespace = unit.getPackageDeclaration()
                    .map(declaration -> UmlNamespace.fromQualifiedName(
                    declaration.getNameAsString()))
                    .orElseGet(UmlNamespace::root);
            List<String> imports = importsOf(unit);
            for (TypeDeclaration<?> declaration : unit.getTypes()) {
                mapAndRegister(declaration, namespace, imports,
                        Optional.empty(), context);
            }
        } catch (IOException | RuntimeException exception) {
            context.reportError(file.toString(),
                    "Could not extract source", exception);
        }
    }

    private void mapAndRegister(TypeDeclaration<?> declaration,
            UmlNamespace namespace, List<String> imports,
            Optional<UmlClassifier> nestingClassifier,
            ResolutionContext context) {
        String qualifiedName = namespace.qualifiedNameOf(
                declaration.getNameAsString());
        if (isExcluded(namespace.toString(), qualifiedName,
                declaration.getNameAsString())) {
            return;
        }
        UmlClassifier classifier = classifierMapper.map(
                declaration, namespace, nestingClassifier);
        context.registerClassifier(classifier);

        nestingClassifier.ifPresent(outer -> context.registerUnresolvedNesting(
                outer.qualifiedName(), classifier.qualifiedName(),
                declaration.isStatic()));
        registerHierarchy(declaration, classifier, imports, context);
        registerMemberTypes(declaration, classifier, imports, context);

        for (var member : declaration.getMembers()) {
            if (member.isTypeDeclaration()) {
                mapAndRegister(member.asTypeDeclaration(),
                        namespace.child(classifier.name()), imports,
                        Optional.of(classifier), context);
            }
        }
    }

    private boolean isExcluded(String namespace, String qualifiedName,
            String simpleName) {
        boolean excludedPackage = excludedPackages.stream().anyMatch(excluded ->
                namespace.equals(excluded) || namespace.startsWith(excluded + "."));
        return excludedPackage || excludedClasses.contains(simpleName)
                || excludedClasses.contains(qualifiedName);
    }

    private void registerHierarchy(TypeDeclaration<?> declaration,
            UmlClassifier classifier, List<String> imports,
            ResolutionContext context) {
        if (declaration instanceof ClassOrInterfaceDeclaration type) {
            type.getExtendedTypes().forEach(ancestor ->
                    context.registerUnresolvedHierarchy(
                            classifier.qualifiedName(), ancestor.getNameWithScope(),
                            imports, false));
            type.getImplementedTypes().forEach(ancestor ->
                    context.registerUnresolvedHierarchy(
                            classifier.qualifiedName(), ancestor.getNameWithScope(),
                            imports, true));
        } else if (declaration instanceof EnumDeclaration type) {
            type.getImplementedTypes().forEach(ancestor ->
                    context.registerUnresolvedHierarchy(
                            classifier.qualifiedName(), ancestor.getNameWithScope(),
                            imports, true));
        } else if (declaration instanceof RecordDeclaration type) {
            type.getImplementedTypes().forEach(ancestor ->
                    context.registerUnresolvedHierarchy(
                            classifier.qualifiedName(), ancestor.getNameWithScope(),
                            imports, true));
        }
    }

    private void registerMemberTypes(TypeDeclaration<?> declaration,
            UmlClassifier classifier, List<String> imports,
            ResolutionContext context) {
        declaration.getMembers().forEach(member -> {
            if (member.isFieldDeclaration()) {
                var field = member.asFieldDeclaration();
                AggregationKind kind = JavaParserAggregationMapper.map(field);
                if (field.isStatic() && kind != AggregationKind.NONE)
                    throw new IllegalArgumentException("@Agregacion y @Composicion requieren atributos de instancia.");
                field.getVariables().forEach(variable ->
                        registerType(variable.getType(), classifier, imports,
                                context, true, kind, "1"));
            } else if (member.isMethodDeclaration()) {
                var method = member.asMethodDeclaration();
                registerType(method.getType(), classifier, imports, context, false);
                method.getParameters().forEach(parameter ->
                        registerType(parameter.getType(), classifier, imports,
                                context, false));
                method.getThrownExceptions().forEach(type ->
                        registerType(type, classifier, imports, context, false));
            } else if (member.isConstructorDeclaration()) {
                member.asConstructorDeclaration().getParameters().forEach(parameter ->
                        registerType(parameter.getType(), classifier, imports,
                                context, false));
                member.asConstructorDeclaration().getThrownExceptions().forEach(type ->
                        registerType(type, classifier, imports, context, false));
            } else if (member.isAnnotationMemberDeclaration()) {
                registerType(member.asAnnotationMemberDeclaration().getType(),
                        classifier, imports, context, false);
            }
        });
        if (declaration instanceof RecordDeclaration record) {
            record.getParameters().forEach(parameter ->
                    registerType(parameter.getType(), classifier, imports,
                            context, true, JavaParserAggregationMapper.map(parameter), "1"));
        }
    }

    private void registerType(Type type, UmlClassifier classifier,
            List<String> imports, ResolutionContext context,
            boolean association) {
        registerType(type, classifier, imports, context, association, AggregationKind.NONE, "1");
    }

    private void registerType(Type type, UmlClassifier classifier,
            List<String> imports, ResolutionContext context,
            boolean association, AggregationKind kind, String multiplicity) {
        if (type.isPrimitiveType() || type.isVoidType() || type.isVarType()) {
            return;
        }
        if (type instanceof ArrayType array) {
            registerType(array.getComponentType(), classifier, imports,
                    context, association, kind, "0..*");
        } else if (type instanceof ClassOrInterfaceType reference) {
            if (reference.getScope().isEmpty() && isTypeParameter(reference)) return;
            if (association) {
                context.registerUnresolvedAssociation(classifier.qualifiedName(),
                        reference.getNameWithScope(), imports, kind, multiplicity);
            } else {
                context.registerUnresolvedType(classifier.qualifiedName(),
                        reference.getNameWithScope(), imports);
            }
            boolean many = Set.of("Collection", "List", "Set", "Iterable", "Queue", "Deque", "Map",
                    "ArrayList", "LinkedList", "HashSet", "TreeSet", "HashMap", "TreeMap")
                    .contains(reference.getNameAsString());
            reference.getTypeArguments().ifPresent(arguments ->
                    arguments.forEach(argument ->
                            registerType(argument, classifier, imports,
                                    context, association, kind, many ? "0..*" : multiplicity)));
        } else if (type instanceof WildcardType wildcard) {
            wildcard.getExtendedType().ifPresent(bound ->
                    registerType(bound, classifier, imports, context, association, kind, multiplicity));
            wildcard.getSuperType().ifPresent(bound ->
                    registerType(bound, classifier, imports, context, association, kind, multiplicity));
        } else if (type instanceof UnionType union) {
            union.getElements().forEach(element ->
                    registerType(element, classifier, imports, context, association));
        } else if (type instanceof IntersectionType intersection) {
            intersection.getElements().forEach(element ->
                    registerType(element, classifier, imports, context, association));
        }
    }

    private List<String> importsOf(CompilationUnit unit) {
        return unit.getImports().stream()
                .filter(importDeclaration -> !importDeclaration.isStatic())
                .map(this::importName)
                .toList();
    }

    private boolean isTypeParameter(ClassOrInterfaceType reference) {
        for (Node node = reference; node != null; node = node.getParentNode().orElse(null)) {
            if (node instanceof NodeWithTypeParameters<?> generic
                    && generic.getTypeParameters().stream().anyMatch(parameter ->
                            parameter.getNameAsString().equals(reference.getNameAsString())))
                return true;
        }
        return false;
    }

    private String importName(ImportDeclaration declaration) {
        return declaration.getNameAsString()
                + (declaration.isAsterisk() ? ".*" : "");
    }
}
