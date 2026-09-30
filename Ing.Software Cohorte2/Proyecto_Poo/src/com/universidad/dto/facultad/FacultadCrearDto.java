package com.universidad.dto.facultad;

import com.universidad.dto.validacion.ReglasValidacion;

public record FacultadCrearDto(
        String codigo,
        String nombre
        ) {

    public FacultadCrearDto {
        codigo = ReglasValidacion.limpiarRequerido(
                codigo,
                "El codigo es obligatorio"
        );

        nombre = ReglasValidacion.limpiarRequerido(
                nombre,
                "El nombre es obligatorio"
        );
    }
}