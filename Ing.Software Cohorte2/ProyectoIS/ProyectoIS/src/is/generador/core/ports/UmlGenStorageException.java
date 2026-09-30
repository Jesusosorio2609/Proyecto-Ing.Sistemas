package is.generador.core.ports;

/** Excepción producida al almacenar el resultado de la generación UML. */
@SuppressWarnings("serial")
public class UmlGenStorageException extends RuntimeException {

    public UmlGenStorageException(String message) {
        super(message);
    }

    public UmlGenStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
