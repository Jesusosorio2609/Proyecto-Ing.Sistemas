package com.universidad;

import is.generador.SoyLaPuertaJava;

/** Ejecuta el inventario detallado generado por Libreria_is. */
public final class AppGenerator {

    private AppGenerator() {
    }

    public static void main(String[] args) {
        new SoyLaPuertaJava().mostrarReporteDetallado(args);
    }
}
