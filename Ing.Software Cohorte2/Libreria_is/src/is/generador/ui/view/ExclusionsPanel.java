package is.generador.ui.view;

import is.generador.ui.model.InventoryViewModel;
import is.generador.ui.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Displays exclusions that are actively applied during project analysis. */
final class ExclusionsPanel extends JPanel {
    private final JLabel summary = new JLabel("Sin análisis");
    private final JLabel source = new JLabel("Origen: analisis.properties");
    private final JTextArea packages = createArea();
    private final JTextArea classes = createArea();
    private final JLabel packagesTitle = UiTheme.heading("Paquetes excluidos (0)");
    private final JLabel classesTitle = UiTheme.heading("Clases excluidas (0)");

    ExclusionsPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(createHeader(), BorderLayout.NORTH);

        JPanel lists = new JPanel(new GridLayout(1, 2, 10, 0));
        lists.setOpaque(false);
        lists.add(createListCard(packagesTitle, packages,
                "Se excluye el paquete indicado y todos sus subpaquetes."));
        lists.add(createListCard(classesTitle, classes,
                "Puede utilizarse el nombre simple o el nombre completamente calificado."));
        add(lists, BorderLayout.CENTER);
        add(createHelpCard(), BorderLayout.SOUTH);
    }

    void update(InventoryViewModel model) {
        Set<String> excludedPackages = model.excludedPackages();
        Set<String> excludedClasses = model.excludedClasses();
        packagesTitle.setText("Paquetes excluidos (" + excludedPackages.size() + ")");
        classesTitle.setText("Clases excluidas (" + excludedClasses.size() + ")");
        packages.setText(listText(excludedPackages));
        classes.setText(listText(excludedClasses));
        packages.setCaretPosition(0);
        classes.setCaretPosition(0);
        int total = excludedPackages.size() + excludedClasses.size();
        summary.setText(total + " reglas de exclusión aplicadas al análisis actual");
        source.setText("Origen: " + model.sourceDirectory().getParent()
                .resolve("analisis.properties"));
    }

    private JPanel createHeader() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 5));
        JLabel title = UiTheme.heading("Exclusiones del análisis");
        title.setFont(title.getFont().deriveFont(18f));
        card.add(title, BorderLayout.NORTH);
        summary.setForeground(UiTheme.TEXT);
        card.add(summary, BorderLayout.CENTER);
        source.setForeground(UiTheme.MUTED);
        card.add(source, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createListCard(JLabel title, JTextArea area, String description) {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 8));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 3));
        heading.setOpaque(false);
        heading.add(title);
        JLabel hint = new JLabel(description);
        hint.setForeground(UiTheme.MUTED);
        heading.add(hint);
        card.add(heading, BorderLayout.NORTH);
        card.add(new JScrollPane(area), BorderLayout.CENTER);
        card.setMinimumSize(new Dimension(300, 300));
        return card;
    }

    private JPanel createHelpCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new GridLayout(3, 1, 0, 3));
        card.add(UiTheme.heading("Cómo configurar las exclusiones"));
        card.add(new JLabel("exclude.packages=com.ejemplo.generado,com.ejemplo.legacy"));
        card.add(new JLabel("exclude.classes=ClaseTemporal,com.ejemplo.AppGenerator"));
        return card;
    }

    private String listText(Set<String> values) {
        return values.isEmpty() ? "No hay exclusiones configuradas."
                : values.stream().sorted().map(value -> "• " + value)
                        .reduce((a, b) -> a + "\n" + b).orElse("");
    }

    private static JTextArea createArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(UiTheme.CODE);
        area.setBackground(new Color(247, 248, 250));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return area;
    }
}
