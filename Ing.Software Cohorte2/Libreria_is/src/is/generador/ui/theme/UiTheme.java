package is.generador.ui.theme;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

public final class UiTheme {
    public static final Color BACKGROUND = new Color(244, 246, 249);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(220, 224, 230);
    public static final Color PRIMARY = new Color(79, 70, 229);
    public static final Color TEXT = new Color(31, 41, 55);
    public static final Color MUTED = new Color(107, 114, 128);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color WARNING = new Color(217, 119, 6);
    public static final Color ERROR = new Color(220, 38, 38);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font HEADING = new Font("SansSerif", Font.BOLD, 14);
    public static final Font CODE = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private UiTheme() {
    }

    public static void install() {
        UIManager.put("Label.font", BODY);
        UIManager.put("Button.font", BODY);
        UIManager.put("Table.font", BODY);
        UIManager.put("TabbedPane.font", BODY);
        UIManager.put("TextField.font", BODY);
        UIManager.put("Panel.background", BACKGROUND);
    }

    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        return panel;
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(HEADING);
        label.setForeground(TEXT);
        return label;
    }

    public static <T extends JComponent> T padded(T component, int size) {
        component.setBorder(BorderFactory.createEmptyBorder(size, size, size, size));
        return component;
    }
}
