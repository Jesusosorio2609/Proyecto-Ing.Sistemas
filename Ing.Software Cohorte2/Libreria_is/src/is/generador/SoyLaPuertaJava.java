package is.generador;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import is.generador.core.application.ProjectModelService;
import is.generador.core.application.deduction.AssociationRelationshipDeductor;
import is.generador.core.application.deduction.HierarchyRelationshipDeductor;
import is.generador.core.application.deduction.NestingRelationshipDeductor;
import is.generador.core.application.deduction.TypeDependencyDeductor;
import is.generador.core.application.resolver.DefaultNameScopeResolver;
import is.generador.core.domain.model.UmlModel;
import is.generador.infra.config.PropertiesDomainPolicyProvider;
import is.generador.infra.javaparser.JavaParserProjectModelExtractor;
import is.generador.ui.app.InventoryApplication;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

public class SoyLaPuertaJava {

    /** Fase 3: genera PUML a partir del modelo ya analizado. */
    public String generarPuml(UmlModel model) {
        return diagramService().generate(model);
    }

    /** Guarda el PUML del modelo actual sin repetir el análisis. */
    public void guardarPuml(UmlModel model, Path destino) {
        diagramService().write(model, destino);
    }

    /** Fases 1, 2 y 3, respetando analisis.properties del proyecto. */
    public String generarPuml(Path directorio) throws IOException {
        Path fuentes = directorio.toAbsolutePath().normalize();
        if (Files.isDirectory(fuentes.resolve("src"))) fuentes = fuentes.resolve("src");
        Path configuracion = fuentes.getParent().resolve("analisis.properties");
        Properties properties = new Properties();
        if (Files.isRegularFile(configuracion)) {
            try (var reader = Files.newBufferedReader(configuracion)) { properties.load(reader); }
        }
        Set<String> paquetes = configurationValues(properties.getProperty("exclude.packages", ""));
        Set<String> clases = configurationValues(properties.getProperty("exclude.classes", ""));
        clases.add("com.universidad.AppGenerator");
        return generarPuml(analyzeModel(fuentes, configuracion, paquetes, clases));
    }

    /** Genera y guarda el PUML desde la carpeta de fuentes o raíz del proyecto. */
    public void guardarPuml(Path directorio, Path destino) throws IOException {
        new is.generador.infra.puml.FileDiagramWriter().write(generarPuml(directorio), destino);
    }

    private is.generador.core.application.ProjectDiagramService diagramService() {
        return new is.generador.core.application.ProjectDiagramService(
                new is.generador.infra.puml.PlantUmlRenderer(),
                new is.generador.infra.puml.FileDiagramWriter());
    }

    private Set<String> configurationValues(String text) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : text.split(",")) if (!value.isBlank()) result.add(value.trim());
        return result;
    }
    /** Returns the source-code metrics for a project. */
    public ProjectSummary analyzeProject(Path sourceDirectory) throws IOException {
        return new ProjectAnalyzer().analyze(sourceDirectory);
    }

    /** Calcula métricas respetando exclusiones de paquetes y clases. */
    public ProjectSummary analyzeProject(Path sourceDirectory,
            Set<String> excludedPackages, Set<String> excludedClasses)
            throws IOException {
        ProjectAnalyzer.Builder builder = ProjectAnalyzer.builder();
        excludedPackages.forEach(builder::excludePackage);
        excludedClasses.forEach(builder::excludeClass);
        return builder.build().analyze(sourceDirectory);
    }

    /** Extrae clasificadores y resuelve las relaciones internas del proyecto. */
    public UmlModel analyzeModel(Path sourceDirectory) throws IOException {
        Path normalizedSource = sourceDirectory.toAbsolutePath().normalize();
        Path projectDirectory = normalizedSource.getParent();
        Path configurationFile = projectDirectory == null
                ? Path.of("analisis.properties")
                : projectDirectory.resolve("analisis.properties");
        return analyzeModel(sourceDirectory, configurationFile, Set.of(), Set.of());
    }

    /** Analiza el modelo respetando configuración y exclusiones explícitas. */
    public UmlModel analyzeModel(Path sourceDirectory, Path configurationFile,
            Set<String> excludedPackages, Set<String> excludedClasses)
            throws IOException {
        var policy = PropertiesDomainPolicyProvider.from(configurationFile);
        var extractor = new JavaParserProjectModelExtractor(
                policy, excludedPackages, excludedClasses);
        var service = new ProjectModelService(
                extractor,
                new DefaultNameScopeResolver(),
                List.of(
                        new HierarchyRelationshipDeductor(),
                        new NestingRelationshipDeductor(),
                        new AssociationRelationshipDeductor(),
                        new TypeDependencyDeductor()));
        return service.analyze(sourceDirectory);
    }

    /** Muestra en consola el inventario detallado del proyecto y sus exclusiones. */
    public void mostrarReporteDetallado(String[] argumentos) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.print(generarReporteDetallado(argumentos));
        } else {
            InventoryApplication.show(this, argumentos);
        }
    }

    /** Devuelve el inventario completo para integraciones gráficas o archivos. */
    public String generarReporteDetallado(String[] argumentos) {
        return GeneradorReporteDetallado.generar(argumentos);
    }

    /** Imprime explícitamente el inventario en consola. */
    public void imprimirReporteDetallado(String[] argumentos) {
        GeneradorReporteDetallado.ejecutar(argumentos);
    }

    /** Agrega una biblioteca a libraries.whitelist del archivo indicado. */
    public void agregarAWhitelist(Path archivoConfiguracion, String biblioteca) throws IOException {
        agregarBiblioteca(archivoConfiguracion, "libraries.whitelist", biblioteca);
    }

    /** Agrega una biblioteca a la whitelist del proyecto actual. */
    public void agregarAWhitelist(String biblioteca) throws IOException {
        agregarAWhitelist(Path.of("analisis.properties"), biblioteca);
    }

    /** Agrega una biblioteca a libraries.blacklist del archivo indicado. */
    public void agregarABlacklist(Path archivoConfiguracion, String biblioteca) throws IOException {
        agregarBiblioteca(archivoConfiguracion, "libraries.blacklist", biblioteca);
    }

    /** Agrega una biblioteca a la blacklist del proyecto actual. */
    public void agregarABlacklist(String biblioteca) throws IOException {
        agregarABlacklist(Path.of("analisis.properties"), biblioteca);
    }

    private void agregarBiblioteca(Path archivoConfiguracion, String propiedad, String biblioteca)
            throws IOException {
        if (biblioteca == null || biblioteca.isBlank()) {
            throw new IllegalArgumentException("El nombre de la biblioteca no puede estar vacio.");
        }
        Properties propiedades = new Properties();
        if (Files.isRegularFile(archivoConfiguracion)) {
            try (var lector = Files.newBufferedReader(archivoConfiguracion)) {
                propiedades.load(lector);
            }
        }
        Set<String> bibliotecas = new LinkedHashSet<>();
        for (String valor : propiedades.getProperty(propiedad, "").split(",")) {
            if (!valor.isBlank()) {
                bibliotecas.add(valor.trim());
            }
        }
        bibliotecas.add(biblioteca.trim());
        propiedades.setProperty(propiedad, String.join(",", bibliotecas));
        try (var escritor = Files.newBufferedWriter(archivoConfiguracion)) {
            propiedades.store(escritor, "Configuracion del inventario de Libreria_is");
        }
    }

    /**
     * Cuenta las declaraciones class de los archivos .java de una carpeta.
     * Recorre subcarpetas e incluye clases internas; no cuenta interfaces,
     * enumeraciones, records ni clases anonimas.
     */
    public int contarClases(Path carpetaFuentes) throws IOException {
        return obtenerNombresClases(carpetaFuentes).size();
    }

    /**
     * Devuelve los nombres simples de las clases en orden alfabetico.
     * Incluye clases internas y conserva nombres repetidos de clases distintas.
     * No incluye interfaces, enumeraciones, records ni clases anonimas.
     */
    public List<String> obtenerNombresClases(Path carpetaFuentes) throws IOException {
        if (!Files.isDirectory(carpetaFuentes)) {
            throw new IOException("No existe la carpeta de fuentes: "
                    + carpetaFuentes.toAbsolutePath());
        }

        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
        List<String> nombres = new ArrayList<>();

        try (Stream<Path> archivos = Files.walk(carpetaFuentes)) {
            Iterator<Path> javaFiles = archivos
                    .filter(Files::isRegularFile)
                    .filter(ruta -> ruta.toString().endsWith(".java"))
                    .iterator();

            while (javaFiles.hasNext()) {
                Path archivo = javaFiles.next();
                ParseResult<CompilationUnit> resultado = parser.parse(archivo);
                if (!resultado.isSuccessful() || !resultado.getResult().isPresent()) {
                    throw new IOException("No se pudo analizar " + archivo
                            + ": " + resultado.getProblems());
                }

                CompilationUnit unidad = resultado.getResult().get();
                for (ClassOrInterfaceDeclaration declaracion
                        : unidad.findAll(ClassOrInterfaceDeclaration.class)) {
                    if (!declaracion.isInterface()) {
                        nombres.add(declaracion.getNameAsString());
                    }
                }
            }
        }
        Collections.sort(nombres);
        return nombres;
    }
}

