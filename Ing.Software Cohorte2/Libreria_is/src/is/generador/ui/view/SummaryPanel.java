package is.generador.ui.view;

import is.generador.core.domain.relationship.UmlAssociation;
import is.generador.core.domain.relationship.UmlDependency;
import is.generador.core.domain.relationship.UmlGeneralization;
import is.generador.core.domain.relationship.UmlNesting;
import is.generador.ui.model.InventoryViewModel;
import is.generador.ui.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class SummaryPanel extends JPanel {
    private final JPanel cards = new JPanel(new GridLayout(2, 4, 10, 10));
    private final JPanel distributions = new JPanel(new GridLayout(1, 3, 10, 10));

    SummaryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiTheme.BACKGROUND);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        cards.setOpaque(false);
        distributions.setOpaque(false);
        add(cards, BorderLayout.NORTH);
        add(distributions, BorderLayout.CENTER);
    }

    void update(InventoryViewModel model) {
        cards.removeAll();
        distributions.removeAll();
        var summary = model.summary();
        addCard("Archivos Java", summary.getJavaFiles(), new Color(124, 58, 237));
        addCard("Paquetes", summary.getTotals().get("Packages"), new Color(180, 83, 9));
        addCard("Clasificadores", model.umlModel().classifiers().size(), UiTheme.PRIMARY);
        addCard("Métodos", summary.getMethods(), new Color(5, 150, 105));
        addCard("Clases", summary.getClasses(), new Color(79, 70, 229));
        addCard("Interfaces", summary.getInterfaces(), new Color(8, 145, 178));
        addCard("Enums", summary.getEnums(), new Color(217, 119, 6));
        addCard("Records", summary.getRecords(), new Color(16, 185, 129));
        distributions.add(createTypeDistribution(model));
        distributions.add(createRelationshipSummary(model));
        distributions.add(createLibraryPolicies(model));
        revalidate();
        repaint();
    }

    private void addCard(String title, int value, Color accent) {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout());
        JLabel valueLabel = new JLabel(String.valueOf(value));
        valueLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 24));
        valueLabel.setForeground(accent);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(UiTheme.MUTED);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(titleLabel, BorderLayout.SOUTH);
        cards.add(card);
    }

    private JPanel createTypeDistribution(InventoryViewModel model) {
        JPanel panel = UiTheme.card();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(UiTheme.heading("Distribución de tipos"));
        int maximum = Math.max(1, model.umlModel().classifiers().size());
        addProgress(panel, "Clases", model.summary().getClasses(), maximum, UiTheme.PRIMARY);
        addProgress(panel, "Records", model.summary().getRecords(), maximum, UiTheme.SUCCESS);
        addProgress(panel, "Interfaces", model.summary().getInterfaces(), maximum,
                new Color(8, 145, 178));
        addProgress(panel, "Enums", model.summary().getEnums(), maximum, UiTheme.WARNING);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createRelationshipSummary(InventoryViewModel model) {
        JPanel panel = UiTheme.card();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(UiTheme.heading("Relaciones detectadas (vista resumida)"));
        long inheritance = model.umlModel().relationships().stream()
                .filter(UmlGeneralization.class::isInstance)
                .filter(value -> !((UmlGeneralization) value).isInterfaceImplementation())
                .count();
        long implementations = model.umlModel().relationships().stream()
                .filter(UmlGeneralization.class::isInstance)
                .filter(value -> ((UmlGeneralization) value).isInterfaceImplementation())
                .count();
        addMetric(panel, "Herencias", inheritance, UiTheme.PRIMARY);
        addMetric(panel, "Implementaciones", implementations, new Color(8, 145, 178));
        addMetric(panel, "Asociaciones", count(model, UmlAssociation.class), UiTheme.WARNING);
        addMetric(panel, "Dependencias", count(model, UmlDependency.class), new Color(234, 88, 12));
        addMetric(panel, "Anidamientos", count(model, UmlNesting.class), UiTheme.SUCCESS);
        return panel;
    }

    private JPanel createLibraryPolicies(InventoryViewModel model) {
        JPanel panel = UiTheme.card();
        panel.setLayout(new BorderLayout(0, 8));
        panel.add(UiTheme.heading("Políticas de bibliotecas"), BorderLayout.NORTH);

        JTextArea policies = new JTextArea();
        policies.setEditable(false);
        policies.setFont(UiTheme.CODE);
        policies.setBackground(new Color(247, 248, 250));
        policies.setForeground(UiTheme.TEXT);
        policies.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        policies.setText(policyText("WHITELIST", model.whitelistedLibraries())
                + "\n\n"
                + policyText("BLACKLIST", model.blacklistedLibraries()));
        policies.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(policies);
        scrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(UiTheme.BORDER));
        panel.add(scrollPane, BorderLayout.CENTER);

        JLabel source = new JLabel("Origen: analisis.properties");
        source.setForeground(UiTheme.MUTED);
        panel.add(source, BorderLayout.SOUTH);
        return panel;
    }

    private String policyText(String title, java.util.Set<String> values) {
        if (values.isEmpty()) {
            return title + " (0)\n  - sin entradas";
        }
        return title + " (" + values.size() + ")\n"
                + values.stream().sorted()
                        .map(value -> "  • " + value)
                        .reduce((first, second) -> first + "\n" + second)
                        .orElse("");
    }

    private long count(InventoryViewModel model, Class<?> type) {
        return model.umlModel().relationships().stream().filter(type::isInstance).count();
    }

    private void addProgress(JPanel parent, String name, int value,
            int maximum, Color color) {
        JPanel row = new JPanel(new BorderLayout(8, 2));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(420, 28));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JLabel label = new JLabel(name);
        label.setPreferredSize(new Dimension(78, 20));
        label.setForeground(UiTheme.TEXT);

        JLabel valueLabel = new JLabel(String.valueOf(value), JLabel.RIGHT);
        valueLabel.setPreferredSize(new Dimension(28, 20));
        valueLabel.setForeground(UiTheme.MUTED);

        JProgressBar progress = new JProgressBar(0, maximum);
        progress.setValue(value);
        progress.setForeground(color);
        progress.setBackground(new Color(229, 231, 235));
        progress.setBorderPainted(false);
        progress.setStringPainted(false);
        progress.setPreferredSize(new Dimension(190, 8));
        progress.setMinimumSize(new Dimension(80, 8));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));

        row.add(label, BorderLayout.WEST);
        row.add(progress, BorderLayout.CENTER);
        row.add(valueLabel, BorderLayout.EAST);
        parent.add(row);
    }

    private void addMetric(JPanel parent, String name, long value, Color color) {
        JLabel label = new JLabel("●  " + name + ": " + value);
        label.setForeground(color);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 0, 2, 0));
        parent.add(label);
    }
}
