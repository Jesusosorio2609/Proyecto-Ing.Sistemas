package com.universidad.mapeador;

import com.universidad.dto.facultad.FacultadDto;
import com.universidad.modelo.Facultad;
import java.util.ArrayList;
import java.util.List;

public class FacultadMapeador implements Mapeador<Facultad, FacultadDto> {

    @Override
    public FacultadDto toDto(Facultad entidad) {
        if (entidad == null) {
            throw new IllegalArgumentException("Facultad Requerida");
        }

        return new FacultadDto(
                entidad.getIdFacultad(),
                entidad.getCodigoFacultad(),
                entidad.getNombreFacultad(),
                entidad.getEstadoFacultad(),
                entidad.estaActivo()
        );
    }

    @Override
    public List<FacultadDto> toDtoList(List<Facultad> entidades) {
        if (entidades == null || entidades.isEmpty()) {
            return List.of();
        }
        List<FacultadDto> resultados = new ArrayList<>(entidades.size());
        for (Facultad facultad : entidades) {
            resultados.add(toDto(facultad));
        }
        return resultados;
    }

}
