package com.universidad.persistencia;

import com.cleandev.tpa.api.TpaRepository;
import com.universidad.modelo.Facultad;
import com.universidad.repositorio.FacultadRepositorio;

public class FacultadRepositorioImpl extends RepositorioBaseAbstracto<Facultad, Long> implements FacultadRepositorio{ 
    
    public FacultadRepositorioImpl(TpaRepository<Facultad, Long> tpaRepository) {
        super(tpaRepository);
    }
    
}
