package com.universidad.servicio;

import com.universidad.dto.facultad.FacultadCrearDto;
import com.universidad.dto.facultad.FacultadDto;
import com.universidad.mapeador.FacultadMapeador;
import com.universidad.modelo.Facultad;
import com.universidad.repositorio.FacultadRepositorio;
import java.util.List;

public class FacultadServicio {
    private final FacultadMapeador mapeador;
    private final FacultadRepositorio repositorio;

    public FacultadServicio(FacultadMapeador mapeador, FacultadRepositorio repositorio) {
        if (mapeador == null) {
            throw new IllegalArgumentException("El mapeador es requerido");
        }
        
        if (repositorio == null) {
            throw new IllegalArgumentException("El repositorio es obligatorio");
        }
        
        this.mapeador = mapeador;
        this.repositorio = repositorio;
    }
    
    public FacultadDto registrarFacultad(FacultadCrearDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la Facultad son obligatorios");
        }
        
        Facultad nuevo = new Facultad(
                dto.codigo(),
                dto.nombre()
        );
        
        Facultad guardado = repositorio.guardar(nuevo);
        return mapeador.toDto(guardado);
    }
    
    public List<FacultadDto> obtenerFacultad() {
        return mapeador.toDtoList(repositorio.listarTodos());
    }
    
    public int contarFacultades() {
        return (int) repositorio.contar();
    }
    
}
