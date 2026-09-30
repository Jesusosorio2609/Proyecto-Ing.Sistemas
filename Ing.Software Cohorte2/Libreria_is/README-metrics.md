# Project summary during compilation

Build or clean and build `Proyecto_Poo` in NetBeans. The summary is printed in
the Output window after compilation. The analysis implementation lives in
`Libreria_is/src/is/generador/ProjectAnalyzer.java`.

## Builder API

```java
ProjectAnalyzer analyzer = ProjectAnalyzer.builder()
        .excludePackage("com.universidad.vista")
        .excludeClass("com.universidad.modelo.Profesor")
        .excludeClass("com.universidad.App")
        .build();

ProjectSummary summary = analyzer.analyze(Path.of("src"));
System.out.println(summary);
```

`excludePackage` excludes the specified package and its subpackages.
`excludeClass` accepts a simple or qualified type name. A qualified name is
recommended when only one specific type should be excluded. Excluding a type
also excludes its members and nested types from the summary.

Every call to `build()` creates an immutable analyzer. A later change to the
builder does not affect analyzers that were already built.

## NetBeans configuration

Edit `metrics.properties` in the consumer project root:

```properties
metrics.exclude.packages=com.universidad.vista
metrics.exclude.classes=com.universidad.modelo.Profesor,com.universidad.App
```

Separate multiple values with commas. Leave a property empty to disable that
kind of exclusion. These exclusions only affect the summary; all project types
continue to compile normally.

## Counting rules

- Java source files are discovered recursively.
- Classes, interfaces, enums, records, and annotation types are separate categories.
- Fields count each declared variable; `int a, b;` counts as two fields.
- Constructors only include declarations explicitly present in the source.
- Getter and setter detection uses JavaBeans-style public, non-static signatures.
- Record components and enum constants are reported separately from fields.
- A parse error fails the analysis instead of returning partial totals.

## Tests

After building the library, run these commands from the `Libreria_is` directory:

```text
java -cp "dist/Libreria_is.jar;librerias/javaparser-core-3.28.2.jar" tests/ProjectAnalyzerTest.java
java -cp "dist/Libreria_is.jar;librerias/javaparser-core-3.28.2.jar" tests/ExclusionBuilderTest.java
```
