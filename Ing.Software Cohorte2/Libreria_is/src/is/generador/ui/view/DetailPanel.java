package is.generador.ui.view;

import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.core.domain.relationship.UmlGeneralization;
import is.generador.core.domain.relationship.UmlRelationship;
import is.generador.ui.model.ClassifierFileDetails;
import is.generador.ui.model.InventoryViewModel;
import is.generador.ui.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class DetailPanel extends JPanel {
    private final JLabel title = UiTheme.heading("Selecciona un clasificador");
    private final JLabel subtitle = new JLabel("Usa el árbol o la tabla de estructura");
    private final JLabel sourceFile = new JLabel("Archivo: —");
    private final JPanel content = new JPanel();
    private InventoryViewModel currentModel;

    DetailPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel header = UiTheme.card();
        header.setLayout(new BorderLayout(0, 3));
        JPanel identity = new JPanel(new GridLayout(2, 1));
        identity.setOpaque(false);
        identity.add(title);
        identity.add(subtitle);
        header.add(identity, BorderLayout.CENTER);
        sourceFile.setForeground(UiTheme.MUTED);
        header.add(sourceFile, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UiTheme.BACKGROUND);
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    void setModel(InventoryViewModel model) {
        currentModel = model;
    }

    void showClassifier(UmlClassifier classifier) {
        if (classifier == null) return;
        title.setText(classifier.name());
        subtitle.setText(classifier.qualifiedName() + "  ·  "
                + classifier.classification() + "  ·  " + classifier.visibility());
        ClassifierFileDetails details = currentModel == null ? null
                : currentModel.classifierDetails().get(classifier.qualifiedName());
        sourceFile.setText("Archivo: " + (details == null ? "no identificado"
                : details.sourceFile().toAbsolutePath().normalize()));
        rebuildContent(classifier, details);
    }

    private void rebuildContent(UmlClassifier classifier, ClassifierFileDetails details) {
        content.removeAll();
        List<DetailSection> availableSections = new ArrayList<>();
        if (details == null) {
            addSection(availableSections, "Propiedades", classifier.properties().stream()
                    .map(value -> value.visibility() + " " + value.name() + ": "
                    + value.type().name()).toList());
            addSection(availableSections, "Métodos y constructores",
                    classifier.operations().stream()
                    .map(value -> value.visibility() + " " + value.name() + "("
                    + value.parameters().size() + "): "
                    + value.returnType().name()).toList());
        } else {
            addSection(availableSections, "Imports internos", details.internalImports());
            addSection(availableSections, "Imports nativos de Java", details.javaImports());
            addSection(availableSections, "Imports externos", details.externalImports());
            addSection(availableSections, "Propiedades", details.properties());
            addSection(availableSections, "Constructores", details.constructors());
            addSection(availableSections, "Constructores compactos",
                    details.compactConstructors());
            addSection(availableSections, "Métodos", details.methods());
            addSection(availableSections, "Getters", details.getters());
            addSection(availableSections, "Setters", details.setters());
            addSection(availableSections, "Componentes de record",
                    details.recordComponents());
            addSection(availableSections, "Constantes de enum", details.enumConstants());
            addSection(availableSections, "Miembros de anotación",
                    details.annotationMembers());
            addSection(availableSections, "Tipos internos", details.nestedTypes());
        }
        addSection(availableSections, "Relaciones", relationshipLines(classifier));
        addSectionRows(availableSections);
        content.revalidate();
        content.repaint();
    }

    private void addSection(List<DetailSection> destination, String name,
            List<String> values) {
        if (!values.isEmpty()) destination.add(new DetailSection(name, values));
    }

    private void addSectionRows(List<DetailSection> sections) {
        for (int start = 0; start < sections.size(); start += 3) {
            int end = Math.min(start + 3, sections.size());
            JPanel[] panels = sections.subList(start, end).stream()
                    .map(value -> section(value.name(), value.values()))
                    .toArray(JPanel[]::new);
            addRow(panels);
        }
        if (sections.isEmpty()) {
            JLabel empty = new JLabel("Este elemento no contiene detalles adicionales.",
                    JLabel.CENTER);
            empty.setForeground(UiTheme.MUTED);
            content.add(empty);
        }
    }

    private void addRow(JPanel... sections) {
        JPanel row = new JPanel(new GridLayout(1, sections.length, 10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));
        row.setPreferredSize(new Dimension(900, 165));
        for (JPanel section : sections) row.add(section);
        content.add(row);
    }

    private List<String> relationshipLines(UmlClassifier classifier) {
        if (currentModel == null) return List.of();
        return currentModel.umlModel().relationships().stream()
                .filter(value -> value.source() == classifier || value.target() == classifier)
                .sorted(Comparator.comparing(value -> value.getClass().getSimpleName()))
                .map(value -> describe(value, classifier)).toList();
    }

    private String describe(UmlRelationship relationship, UmlClassifier classifier) {
        String direction = relationship.source() == classifier ? "→ " : "← ";
        UmlClassifier other = relationship.source() == classifier
                ? relationship.target() : relationship.source();
        String kind = relationship instanceof UmlGeneralization generalization
                ? (generalization.isInterfaceImplementation() ? "IMPLEMENTA" : "HEREDA")
                : relationship.getClass().getSimpleName().replace("Uml", "").toUpperCase();
        return direction + kind + "  " + other.qualifiedName();
    }

    private JPanel section(String name, List<String> values) {
        JTextArea area = createArea();
        area.setText(values.stream().sorted().map(value -> "- " + value)
                .reduce((a, b) -> a + "\n" + b).orElse(""));
        area.setCaretPosition(0);
        JPanel panel = UiTheme.card();
        panel.setLayout(new BorderLayout(0, 7));
        panel.add(UiTheme.heading(name + " (" + values.size() + ")"), BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private static JTextArea createArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(false);
        area.setFont(UiTheme.CODE);
        area.setBackground(new java.awt.Color(247, 248, 250));
        area.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
        return area;
    }

    private record DetailSection(String name, List<String> values) {}
}
