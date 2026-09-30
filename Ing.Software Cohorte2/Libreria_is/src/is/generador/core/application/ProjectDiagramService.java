package is.generador.core.application;

import is.generador.core.domain.model.UmlModel;
import is.generador.core.ports.DiagramWriter;
import is.generador.core.ports.UmlRenderer;
import java.nio.file.Path;
import java.util.Objects;

public final class ProjectDiagramService {
    private final UmlRenderer renderer;
    private final DiagramWriter writer;
    public ProjectDiagramService(UmlRenderer renderer, DiagramWriter writer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.writer = Objects.requireNonNull(writer, "writer");
    }
    public String generate(UmlModel model) { return renderer.render(model); }
    public void write(UmlModel model, Path target) { writer.write(generate(model), target); }
}
