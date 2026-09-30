package is.generador.ui.app;

import is.generador.SoyLaPuertaJava;
import is.generador.ui.service.InventoryUiService;
import is.generador.ui.theme.UiTheme;
import is.generador.ui.view.InventoryFrame;
import java.nio.file.Path;
import javax.swing.SwingUtilities;

public final class InventoryApplication {
    private InventoryApplication() {
    }

    /** Starts the centralized project inventory application. */
    public static void main(String[] arguments) {
        show(new SoyLaPuertaJava(), arguments);
    }

    public static void show(SoyLaPuertaJava gateway, String[] arguments) {
        Path initialSource = InventoryUiService.sourceFrom(arguments);
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            new InventoryFrame(new InventoryUiService(gateway), initialSource)
                    .setVisible(true);
        });
    }
}
