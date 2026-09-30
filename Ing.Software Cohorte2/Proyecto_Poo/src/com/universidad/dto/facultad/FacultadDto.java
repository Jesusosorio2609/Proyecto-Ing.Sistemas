package com.universidad.dto.facultad;

import com.universidad.modelo.enumeracion.EstadoEntidad;

public record FacultadDto(
        Long id,
        String codigo,
        String nombre,
        EstadoEntidad estado,
        boolean activo
        ) {

}