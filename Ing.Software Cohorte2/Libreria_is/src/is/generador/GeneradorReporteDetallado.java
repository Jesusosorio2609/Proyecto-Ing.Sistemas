package is.generador;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import is.generador.infra.report.TextInventoryRenderer;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Ejecuta el análisis detallado de Libreria_is sin iniciar la aplicación principal. */
final class GeneradorReporteDetallado {

    private GeneradorReporteDetallado() {
    }

    static void ejecutar(String[] args) {
        System.out.print(generar(args));
    }

    static String generar(String[] args) {
        synchronized (GeneradorReporteDetallado.class) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            PrintStream originalOutput = System.out;
            try (PrintStream capturedOutput = new PrintStream(
                    buffer, true, StandardCharsets.UTF_8)) {
                System.setOut(capturedOutput);
                ejecutarInternamente(args);
            } finally {
                System.setOut(originalOutput);
            }
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    private static void ejecutarInternamente(String[] args) {
        try {
            ConfiguracionAnalisis configuracion = ConfiguracionAnalisis.desde(args);
            ProjectAnalyzer.Builder builder = ProjectAnalyzer.builder();
            configuracion.paquetesExcluidos.forEach(builder::excludePackage);
            configuracion.clasesExcluidas.forEach(builder::excludeClass);
            ProjectSummary resumen = builder.build().analyze(configuracion.fuentes);
            imprimirResumen(resumen, configuracion.fuentes);
            imprimirDetalle(configuracion);
            imprimirModeloRelaciones(configuracion);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("No se pudo analizar el proyecto: " + e.getMessage());
            throw new IllegalArgumentException("No se pudo analizar el proyecto: " + e.getMessage(), e);
        }
    }

    private static void imprimirModeloRelaciones(
            ConfiguracionAnalisis configuracion) throws IOException {
        var model = new SoyLaPuertaJava().analyzeModel(
                configuracion.fuentes,
                configuracion.archivoConfiguracion,
                configuracion.paquetesExcluidos,
                configuracion.clasesExcluidas);
        System.out.print(new TextInventoryRenderer().render(model));
    }

    private static void imprimirResumen(ProjectSummary resumen, Path fuentes) {
        System.out.println("========== RESUMEN GENERAL DE LIBRERIA_IS ==========");
        System.out.println("Fuentes: " + fuentes.toAbsolutePath().normalize());
        resumen.getTotals().forEach((nombre, cantidad) ->
                System.out.printf("%-26s : %d%n", nombre, cantidad));
        System.out.println("=============================================");
    }

    private static void imprimirDetalle(ConfiguracionAnalisis configuracion) throws IOException {
        Path fuentes = configuracion.fuentes;
        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
        Map<String, List<Path>> porPaquete = new TreeMap<>();
        Set<String> tiposInternos = new LinkedHashSet<>();
        Set<String> paquetesInternos = new LinkedHashSet<>();

        try (Stream<Path> rutas = Files.walk(fuentes)) {
            for (Path archivo : rutas.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.toString().endsWith(".java"))
                    .sorted().toList()) {
                CompilationUnit unidad = parsear(parser, archivo);
                String paquete = unidad.getPackageDeclaration()
                        .map(declaracion -> declaracion.getNameAsString()).orElse("(sin paquete)");
                if (configuracion.paqueteExcluido(paquete)) {
                    continue;
                }
                agregarTiposInternos(unidad, paquete, tiposInternos);
                paquetesInternos.add(paquete);
                porPaquete.computeIfAbsent(paquete, clave -> new java.util.ArrayList<>()).add(archivo);
            }
        }

        System.out.println("\n===============================================================================");
        System.out.println("                     INVENTARIO DETALLADO DEL PROYECTO");
        System.out.println("===============================================================================\n");
        for (Map.Entry<String, List<Path>> entrada : porPaquete.entrySet()) {
            String paquete = entrada.getKey();
            List<Path> archivos = entrada.getValue();
            int tipos = archivos.stream().mapToInt(archivo -> {
                try {
                    return parsear(parser, archivo).getTypes().size();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }).sum();
            System.out.printf("%n-------------------------------------------------------------------------------%n"
                    + "PAQUETE : %s%nRUTA    : %s%nARCHIVOS: %d    TIPOS DECLARADOS: %d%n"
                    + "-------------------------------------------------------------------------------%n", paquete,
                    fuentes.resolve(paquete.equals("(sin paquete)") ? "" : paquete.replace('.', java.io.File.separatorChar))
                            .toAbsolutePath().normalize(), archivos.size(), tipos);
            for (Path archivo : archivos) {
                CompilationUnit unidad = parsear(parser, archivo);
                System.out.println("  ARCHIVO: " + archivo.getFileName());
                imprimirImportaciones(unidad, tiposInternos, paquetesInternos, configuracion, "    ");
                for (TypeDeclaration<?> tipo : unidad.getTypes()) {
                    String nombreCompleto = paquete.equals("(sin paquete)") ? tipo.getNameAsString()
                            : paquete + "." + tipo.getNameAsString();
                    imprimirTipo(tipo, "  ", paquete, nombreCompleto, configuracion);
                }
            }
        }
        System.out.println("\n===============================================================================");
        System.out.println("                           FIN DEL INVENTARIO");
        System.out.println("===============================================================================");
    }

    private static void agregarTiposInternos(CompilationUnit unidad, String paquete, Set<String> tiposInternos) {
        for (TypeDeclaration<?> tipo : unidad.getTypes()) {
            tiposInternos.add(paquete.equals("(sin paquete)") ? tipo.getNameAsString()
                    : paquete + "." + tipo.getNameAsString());
        }
    }

    private static void imprimirImportaciones(CompilationUnit unidad, Set<String> tiposInternos,
            Set<String> paquetesInternos, ConfiguracionAnalisis configuracion, String sangria) {
        List<String> internas = new ArrayList<>();
        List<String> nativasJava = new ArrayList<>();
        List<String> externas = new ArrayList<>();
        for (com.github.javaparser.ast.ImportDeclaration importacion : unidad.getImports()) {
            if (importacion.isAsterisk() || !importacionSeUsa(unidad, importacion)) {
                continue;
            }
            String nombre = importacion.getNameAsString();
            if (configuracion.libreriaBloqueada(nombre)) {
                continue;
            }
            if (esImportacionInterna(nombre, tiposInternos, paquetesInternos)) {
                internas.add(nombre);
            } else if (esImportacionNativaJava(nombre)) {
                nativasJava.add(nombre);
            } else {
                externas.add(nombre);
            }
        }
        if (!internas.isEmpty()) {
            imprimirLista(sangria + "IMPORTACIONES INTERNAS", internas);
        }
        if (!nativasJava.isEmpty()) {
            imprimirLista(sangria + "IMPORTACIONES NATIVAS DE JAVA", nativasJava);
        }
        if (!externas.isEmpty()) {
            imprimirLista(sangria + "IMPORTACIONES EXTERNAS", externas);
        }
    }

    private static boolean importacionSeUsa(CompilationUnit unidad,
            com.github.javaparser.ast.ImportDeclaration importacion) {
        String nombreSimple = importacion.getName().getIdentifier();
        return unidad.getTypes().stream().flatMap(tipo -> tipo.findAll(SimpleName.class).stream())
                .anyMatch(nombre -> nombre.getIdentifier().equals(nombreSimple));
    }

    private static boolean esImportacionInterna(String importacion, Set<String> tiposInternos,
            Set<String> paquetesInternos) {
        return tiposInternos.contains(importacion)
                || tiposInternos.stream().anyMatch(tipo -> tipo.startsWith(importacion + "."))
                || paquetesInternos.stream().anyMatch(paquete -> importacion.startsWith(paquete + "."));
    }

    private static boolean esImportacionNativaJava(String importacion) {
        return importacion.startsWith("java.") || importacion.startsWith("javax.");
    }

    private static CompilationUnit parsear(JavaParser parser, Path archivo) throws IOException {
        ParseResult<CompilationUnit> resultado = parser.parse(archivo);
        if (!resultado.isSuccessful() || resultado.getResult().isEmpty()) {
            throw new IOException("No se pudo analizar " + archivo + ": " + resultado.getProblems());
        }
        return resultado.getResult().get();
    }

    private static void imprimirTipo(TypeDeclaration<?> tipo, String sangria, String paquete,
            String nombreCompleto, ConfiguracionAnalisis configuracion) {
        if (configuracion.claseExcluida(nombreCompleto)) {
            return;
        }
        System.out.printf("%s[%s] %s%n", sangria, claseDe(tipo), tipo.getNameAsString());
        if (tipo instanceof RecordDeclaration registro) {
            imprimirLista(sangria + "  COMPONENTES", registro.getParameters().stream()
                    .map(parametro -> parametro.getType() + " " + parametro.getName()).toList());
        }
        if (tipo instanceof EnumDeclaration enumeracion) {
            imprimirLista(sangria + "  CONSTANTES", enumeracion.getEntries().stream()
                    .map(entrada -> entrada.getNameAsString()).toList());
        }
        if (tipo instanceof AnnotationDeclaration anotacion) {
            imprimirLista(sangria + "  MIEMBROS DE ANOTACION", anotacion.getMembers().stream()
                    .map(miembro -> firma((AnnotationMemberDeclaration) miembro)).toList());
        }
        imprimirLista(sangria + "  PROPIEDADES", tipo.getFields().stream()
                .flatMap(campo -> campo.getVariables().stream()
                        .map(variable -> campo.getElementType() + " " + variable.getName())).toList());
        imprimirLista(sangria + "  CONSTRUCTORES", tipo.getConstructors().stream()
                .map(GeneradorReporteDetallado::firma).toList());
        imprimirLista(sangria + "  CONSTRUCTORES COMPACTOS", tipo.getMembers().stream()
                .filter(CompactConstructorDeclaration.class::isInstance)
                .map(miembro -> firma((CompactConstructorDeclaration) miembro)).toList());
        imprimirLista(sangria + "  METODOS", tipo.getMethods().stream().map(GeneradorReporteDetallado::firma).toList());
        imprimirLista(sangria + "  GETTERS", tipo.getMethods().stream()
                .filter(GeneradorReporteDetallado::esGetter).map(GeneradorReporteDetallado::firma).toList());
        imprimirLista(sangria + "  SETTERS", tipo.getMethods().stream()
                .filter(GeneradorReporteDetallado::esSetter).map(GeneradorReporteDetallado::firma).toList());
        for (BodyDeclaration<?> miembro : tipo.getMembers()) {
            if (miembro instanceof TypeDeclaration<?> interno) {
                imprimirTipo(interno, sangria + "  ", paquete,
                        nombreCompleto + "." + interno.getNameAsString(), configuracion);
            }
        }
    }

    private static String claseDe(TypeDeclaration<?> tipo) {
        if (tipo instanceof ClassOrInterfaceDeclaration clase) {
            return clase.isInterface() ? "INTERFAZ" : clase.isAbstract() ? "CLASE ABSTRACTA" : "CLASE";
        }
        if (tipo instanceof EnumDeclaration) return "ENUM";
        if (tipo instanceof RecordDeclaration) return "RECORD";
        if (tipo instanceof AnnotationDeclaration) return "ANOTACIÓN";
        return "TIPO";
    }

    private static void imprimirLista(String titulo, List<String> valores) {
        System.out.printf("%s (%d)%n", titulo, valores.size());
        String sangria = titulo.substring(0, titulo.length() - titulo.stripLeading().length()) + "  ";
        if (valores.isEmpty()) {
            System.out.println(sangria + "- ninguno");
            return;
        }
        for (String valor : valores) {
            System.out.println(sangria + "- " + valor);
        }
    }

    private static String firma(MethodDeclaration metodo) {
        return metodo.getDeclarationAsString(false, false, false);
    }

    private static String firma(ConstructorDeclaration constructor) {
        return constructor.getDeclarationAsString(false, false, false);
    }

    private static String firma(CompactConstructorDeclaration constructor) {
        return constructor.getNameAsString() + "(...)";
    }

    private static String firma(AnnotationMemberDeclaration miembro) {
        return miembro.getType() + " " + miembro.getName();
    }

    private static boolean esGetter(MethodDeclaration metodo) {
        String nombre = metodo.getNameAsString();
        return !metodo.isStatic() && metodo.isPublic() && metodo.getParameters().isEmpty()
                && !metodo.getType().isVoidType()
                && ((nombre.startsWith("get") && nombre.length() > 3)
                || (nombre.startsWith("is") && nombre.length() > 2
                && metodo.getType().asString().equals("boolean")));
    }

    private static boolean esSetter(MethodDeclaration metodo) {
        String nombre = metodo.getNameAsString();
        return !metodo.isStatic() && metodo.isPublic() && nombre.startsWith("set") && nombre.length() > 3
                && metodo.getParameters().size() == 1 && metodo.getType().isVoidType();
    }

    private static final class ConfiguracionAnalisis {
        private final Path fuentes;
        private final Set<String> paquetesExcluidos = new LinkedHashSet<>();
        private final Set<String> clasesExcluidas = new LinkedHashSet<>();
        private final Set<String> libreriasPermitidas = new LinkedHashSet<>();
        private final Set<String> libreriasBloqueadas = new LinkedHashSet<>();
        private final Path archivoConfiguracion;

        private ConfiguracionAnalisis(Path fuentes) {
            this.fuentes = fuentes;
            Path proyecto = fuentes.toAbsolutePath().normalize().getParent();
            this.archivoConfiguracion = proyecto.resolve("analisis.properties");
            clasesExcluidas.add("com.universidad.AppGenerator");
        }

        private static ConfiguracionAnalisis desde(String[] argumentos) {
            Path fuentes = Path.of("src");
            ConfiguracionAnalisis configuracion = new ConfiguracionAnalisis(fuentes);
            for (String argumento : argumentos) {
                if (argumento.startsWith("--excluir-paquete=")) {
                    configuracion.paquetesExcluidos.add(argumento.substring("--excluir-paquete=".length()));
                } else if (argumento.startsWith("--excluir-clase=")) {
                    configuracion.clasesExcluidas.add(argumento.substring("--excluir-clase=".length()));
                } else if (!argumento.startsWith("--")) {
                    configuracion = copiarConFuentes(configuracion, Path.of(argumento));
                } else {
                    throw new IllegalArgumentException("Parametro no reconocido: " + argumento
                            + "\nUso: AppGenerator [fuentes] [--excluir-paquete=paquete] [--excluir-clase=clase]");
                }
            }
            configuracion.cargarPoliticasLibrerias();
            return configuracion;
        }

        private static ConfiguracionAnalisis copiarConFuentes(ConfiguracionAnalisis anterior, Path fuentes) {
            ConfiguracionAnalisis copia = new ConfiguracionAnalisis(fuentes);
            copia.paquetesExcluidos.addAll(anterior.paquetesExcluidos);
            copia.clasesExcluidas.addAll(anterior.clasesExcluidas);
            return copia;
        }

        private void cargarPoliticasLibrerias() {
            if (!Files.isRegularFile(archivoConfiguracion)) {
                return;
            }
            Properties propiedades = new Properties();
            try (var lector = Files.newBufferedReader(archivoConfiguracion)) {
                propiedades.load(lector);
            } catch (IOException e) {
                throw new IllegalArgumentException("No se pudo leer " + archivoConfiguracion, e);
            }
            cargarLista(propiedades.getProperty("libraries.whitelist", ""), libreriasPermitidas);
            cargarLista(propiedades.getProperty("libraries.blacklist", ""), libreriasBloqueadas);
            cargarLista(propiedades.getProperty("exclude.packages", ""), paquetesExcluidos);
            cargarLista(propiedades.getProperty("exclude.classes", ""), clasesExcluidas);
        }

        private static void cargarLista(String texto, Set<String> destino) {
            for (String valor : texto.split(",")) {
                if (!valor.isBlank()) {
                    destino.add(valor.trim());
                }
            }
        }

        private boolean libreriaPermitida(String importacion) {
            return !libreriaBloqueada(importacion) && coincide(importacion, libreriasPermitidas);
        }

        private boolean libreriaBloqueada(String importacion) {
            return coincide(importacion, libreriasBloqueadas);
        }

        private static boolean coincide(String importacion, Set<String> lista) {
            return lista.stream().anyMatch(valor -> importacion.equals(valor)
                    || importacion.endsWith("." + valor));
        }

        private boolean paqueteExcluido(String paquete) {
            return paquetesExcluidos.stream().anyMatch(excluido -> paquete.equals(excluido)
                    || paquete.startsWith(excluido + "."));
        }

        private boolean claseExcluida(String nombreCompleto) {
            String simple = nombreCompleto.substring(nombreCompleto.lastIndexOf('.') + 1);
            return clasesExcluidas.contains(simple) || clasesExcluidas.contains(nombreCompleto);
        }
    }
}
