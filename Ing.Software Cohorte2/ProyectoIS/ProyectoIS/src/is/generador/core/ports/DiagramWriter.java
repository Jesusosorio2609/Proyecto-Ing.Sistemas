package is.generador.core.ports;

import java.nio.file.Path;

/** Puerto de salida para almacenar el contenido de un diagrama. */
public interface DiagramWriter {

    void write(String content, Path targetPath);
}
