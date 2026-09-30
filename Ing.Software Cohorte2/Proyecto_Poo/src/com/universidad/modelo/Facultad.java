package com.universidad.modelo;

import com.cleandev.tpa.api.annotations.TpaConvert;
import com.cleandev.tpa.api.annotations.TpaId;
import com.universidad.modelo.convertidor.EstadoEntidadConverter;
import com.universidad.modelo.enumeracion.EstadoEntidad;

public class Facultad {

    @TpaId
    private Long idFacultad;

    private String codigoFacultad;
    private String nombreFacultad;

    @TpaConvert(converter = EstadoEntidadConverter.class)
    private EstadoEntidad estadoFacultad;

    protected Facultad() {
    }

    /*
     * Constructor para crear una nueva facultad.
     * No recibe ID porque este será generado posteriormente.
     */
    public Facultad(
            String codigoFacultad,
            String nombreFacultad
    ) {
        this.codigoFacultad = codigoFacultad;
        this.nombreFacultad = nombreFacultad;
        this.estadoFacultad = EstadoEntidad.ACTIVO;
    }

    /*
     * Constructor de hidratación.
     * Se utiliza cuando la Facultad ya existe y viene,
     * por ejemplo, desde la base de datos.
     */
    public Facultad(
            Long idFacultad,
            String codigoFacultad,
            String nombreFacultad,
            EstadoEntidad estadoFacultad
    ) {
        this.codigoFacultad = codigoFacultad;
        this.nombreFacultad = nombreFacultad;
        this.estadoFacultad =
                (estadoFacultad != null)
                        ? estadoFacultad
                        : EstadoEntidad.ACTIVO;

        if (idFacultad == null) {
            throw new IllegalArgumentException(
                    "El ID de la facultad es obligatorio en hidratacion"
            );
        }

        this.idFacultad = idFacultad;
    }

    public void actualizarCodigo(String nuevoCodigo) {
        if (nuevoCodigo.equalsIgnoreCase(this.codigoFacultad)) {
            throw new IllegalArgumentException(
                    "El nuevo codigo es igual al actual"
            );
        }

        this.codigoFacultad = nuevoCodigo;
    }

    public void actualizarNombre(String nuevoNombre) {
        if (nuevoNombre.equalsIgnoreCase(this.nombreFacultad)) {
            throw new IllegalArgumentException(
                    "El nuevo nombre es igual al actual"
            );
        }

        this.nombreFacultad = nuevoNombre;
    }

    public void cambiarEstado(EstadoEntidad nuevoEstado) {
        this.estadoFacultad = estadoFacultad.cambiarEstadoA(nuevoEstado);
    }

    public boolean estaActivo() {
        return this.estadoFacultad == EstadoEntidad.ACTIVO;
    }

    public Long getIdFacultad() {
        return idFacultad;
    }

    public String getCodigoFacultad() {
        return codigoFacultad;
    }

    public String getNombreFacultad() {
        return nombreFacultad;
    }

    public EstadoEntidad getEstadoFacultad() {
        return estadoFacultad;
    }
}
