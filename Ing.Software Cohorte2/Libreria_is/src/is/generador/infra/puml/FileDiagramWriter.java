package is.generador.infra.puml;

import is.generador.core.ports.DiagramWriter;
import is.generador.core.ports.UmlGenStorageException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;

public final class FileDiagramWriter implements DiagramWriter {
    @Override public void write(String content, Path targetPath) {
        Objects.requireNonNull(content, "content");
        Path target = Objects.requireNonNull(targetPath, "targetPath").toAbsolutePath().normalize();
        Path temporary = null;
        try {
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), "diagram-", ".tmp");
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new UmlGenStorageException("No se pudo guardar el PUML en " + target, exception);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }
}
