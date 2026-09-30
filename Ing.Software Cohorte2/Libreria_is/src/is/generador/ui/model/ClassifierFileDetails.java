package is.generador.ui.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Source-level information shown without extending the UML domain model. */
public record ClassifierFileDetails(Path sourceFile, List<String> internalImports,
        List<String> javaImports, List<String> externalImports,
        List<String> recordComponents, List<String> enumConstants,
        List<String> annotationMembers, List<String> properties,
        List<String> constructors, List<String> compactConstructors,
        List<String> methods, List<String> getters, List<String> setters,
        List<String> nestedTypes) {

    public ClassifierFileDetails {
        Objects.requireNonNull(sourceFile, "sourceFile");
        internalImports = List.copyOf(internalImports);
        javaImports = List.copyOf(javaImports);
        externalImports = List.copyOf(externalImports);
        recordComponents = List.copyOf(recordComponents);
        enumConstants = List.copyOf(enumConstants);
        annotationMembers = List.copyOf(annotationMembers);
        properties = List.copyOf(properties);
        constructors = List.copyOf(constructors);
        compactConstructors = List.copyOf(compactConstructors);
        methods = List.copyOf(methods);
        getters = List.copyOf(getters);
        setters = List.copyOf(setters);
        nestedTypes = List.copyOf(nestedTypes);
    }
}
