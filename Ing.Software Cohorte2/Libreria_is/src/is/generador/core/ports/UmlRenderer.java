package is.generador.core.ports;

import is.generador.core.domain.model.UmlModel;

/** Puerto de salida para transformar un modelo UML en texto. */
public interface UmlRenderer {

    String render(UmlModel model);
}
