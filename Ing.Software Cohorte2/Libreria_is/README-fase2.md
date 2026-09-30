# Fase 2: inferencia de relaciones

El flujo actual es extracción de declaraciones -> resolución de nombres -> inferencia
de relaciones -> inventario completo. La generación de PUML corresponde a la fase 3.

## Reglas

- Un atributo sin anotaciones produce una asociación.
- `@Agregacion` declara una relación de todo/parte compartida.
- `@Composicion` declara que el todo es propietario de la parte.
- Parámetros de métodos y constructores, retornos y excepciones declaradas en
  `throws` producen dependencias. No se inspeccionan cuerpos de métodos ni
  constructores para deducir relaciones.
- También se detectan herencia, implementación y anidamiento.
- Las dos anotaciones se aplican a atributos de instancia o componentes de records.
  No pueden combinarse en un mismo atributo. Las inicializaciones con `new` no
  se interpretan como evidencia de composición.
- Se resuelven relaciones entre los clasificadores extraídos del proyecto.
  No se cargan ni ejecutan las clases ni se inspeccionan JAR de dependencias.
- Arrays y argumentos de las colecciones reconocidas usan multiplicidad `0..*`.
  Una referencia simple usa `1` como valor convencional, no como garantía de
  ausencia de `null`. Los parámetros genéricos no se tratan como clases concretas.

```java
import is.generador.annotations.Agregacion;
import is.generador.annotations.Composicion;

class Equipo {
    @Agregacion java.util.List<Persona> integrantes;
    @Composicion Motor motor;
    Proveedor proveedor; // asociación

    Proveedor consultar(Persona persona) { // dependencias por firma
        return proveedor;
    }
}
```

Las anotaciones son afirmaciones explícitas de diseño. El analizador no demuestra
el ciclo de vida de los objetos. Se admiten nombres simples y nombres completamente
calificados `is.generador.annotations.Agregacion` y `.Composicion`.

## Inventario y selección para diagramas

`UmlModel.relationships()` conserva todos los tipos de relación por pareja dirigida.
Las repeticiones del mismo tipo y multiplicidades se unifican; el inventario no es
una lista de cada aparición en cada atributo o parámetro. El reporte y la interfaz
muestran este inventario completo.

`UmlModel.diagramRelationships()` ofrece una proyección de una relación por pareja
`origen -> destino` para el futuro PUML. No elimina relaciones del inventario.
La prioridad es:

1. Herencia o implementación (60).
2. Anidamiento (50).
3. Composición (40).
4. Agregación (30).
5. Asociación (20).
6. Dependencia (10).

La dirección inversa se conserva como otra pareja. Los empates se resuelven con
orden estable por tipo y multiplicidades. La prioridad se define en
`RelationshipSelection`; los pesos de los deductores solo ordenan su ejecución.

## Verificación

Compilar y ejecutar `tests/Phase2RelationshipsTest.java` con las clases de la
biblioteca y JavaParser en el classpath. Comprueba inventario completo, prioridad,
anotaciones, multiplicidades, firmas, exclusiones, referencias recursivas,
parámetros genéricos y ausencia de inferencia a partir de cuerpos de métodos.
