package com.universidad;

import is.generador.SoyLaPuertaJava;

/** Ejecuta el inventario detallado generado por Libreria_is. */
public final class AppGenerator {

    private AppGenerator() {
    }

    public static void main(String[] args) {

        try {
            var puerta = new SoyLaPuertaJava();
            var fuentes = java.nio.file.Path.of("src").toAbsolutePath().normalize();
            var configuracion = fuentes.getParent().resolve("analisis.properties");
            var servicio = new is.generador.ui.service.InventoryUiService(puerta);

            // Excluir paquetes y clases
            servicio.saveExclusions(fuentes,
                    java.util.Set.of("com.universidad.vista"),
                    java.util.Set.of("com.universidad.modelo.Profesor"));

            // Agregar a blacklist
            puerta.agregarABlacklist(configuracion, "java.sql");


//            // Agregar a whitelist
//            puerta.agregarAWhitelist(configuracion, "java.time");


        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }

        // Imprimir en Consola
        //new SoyLaPuertaJava().imprimirReporteDetallado(args);

        //Mostrar Interfaz
        new SoyLaPuertaJava().mostrarReporteDetallado(args);
    }
}
