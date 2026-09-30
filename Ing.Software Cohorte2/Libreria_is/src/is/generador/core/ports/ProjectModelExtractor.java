package is.generador.core.ports;

import is.generador.core.application.context.ResolutionContext;
import java.io.IOException;
import java.nio.file.Path;

public interface ProjectModelExtractor {
    ResolutionContext extract(Path sourceDirectory) throws IOException;
}
