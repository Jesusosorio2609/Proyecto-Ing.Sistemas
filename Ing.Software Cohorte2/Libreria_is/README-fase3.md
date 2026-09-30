# Fase 3: generación de PlantUML

El flujo es extracción -> inferencia -> generación de PUML. Se conservan todas
las relaciones del modelo; el diagrama usa `diagramRelationships()` para elegir
la de mayor prioridad por pareja dirigida, según las reglas de la fase 2.

Se generan paquetes, clases concretas y abstractas, interfaces, enums,
anotaciones y records (clase con estereotipo `record`), atributos, operaciones,
constructores, parámetros, visibilidad y modificadores estáticos/abstractos.
Los identificadores internos son alias estables para evitar conflictos entre
clases con el mismo nombre. Los tipos genéricos y arrays conservan su forma Java.

## API

```java
var puerta = new SoyLaPuertaJava();
// Acepta la raíz del proyecto o su carpeta src. Lee analisis.properties.
String puml = puerta.generarPuml(java.nio.file.Path.of("src"));
puerta.guardarPuml(java.nio.file.Path.of("src"), java.nio.file.Path.of("diagrama.puml"));

// Reutiliza el modelo existente, sin repetir las fases 1 y 2.
String desdeModelo = puerta.generarPuml(modelo);
puerta.guardarPuml(modelo, java.nio.file.Path.of("diagrama.puml"));
```

Los métodos que analizan fuentes lanzan `IOException`. Los fallos de escritura
lanzan `UmlGenStorageException`. El guardado es UTF-8, crea carpetas padre y
reemplaza el archivo de destino mediante un archivo temporal.

En la interfaz, **Guardar PUML** exporta el modelo del análisis actual y pregunta
antes de reemplazar un archivo existente. No genera una imagen ni requiere
instalar PlantUML. El archivo `.puml` se puede abrir en un visor PlantUML.

## Organización

- `core/application/ProjectDiagramService`: coordina renderizado y guardado.
- `infra/puml/PlantUmlRenderer`: traduce el modelo a sintaxis PlantUML.
- `infra/puml/FileDiagramWriter`: escribe el archivo.
- `SoyLaPuertaJava`: expone la generación a consumidores de la biblioteca.

Prueba: `tests/Phase3PumlTest.java`. Cubre sintaxis de relaciones, tipos de
clasificadores, miembros, selección por prioridad, alias estables, exclusiones,
guardado y errores. La sintaxis también se verificó con PlantUML 1.2025.0.

Referencia de sintaxis: https://plantuml.com/es/class-diagram
