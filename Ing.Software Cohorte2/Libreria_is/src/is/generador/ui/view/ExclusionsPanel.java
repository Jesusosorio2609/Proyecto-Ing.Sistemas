package is.generador.ui.view;

import is.generador.ui.model.InventoryViewModel;
import is.generador.ui.service.InventoryUiService;
import is.generador.ui.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/** Displays and edits exclusions applied during project analysis. */
final class ExclusionsPanel extends JPanel {
    @FunctionalInterface
    interface ChangeListener {
        void exclusionsChanged(Set<String> packages, Set<String> classes);
    }

    private final JLabel summary = new JLabel("Sin análisis");
    private final JLabel source = new JLabel("Origen: analisis.properties");
    private final JLabel packagesTitle = UiTheme.heading("Paquetes excluidos (0)");
    private final JLabel classesTitle = UiTheme.heading("Clases excluidas (0)");
    private final DefaultListModel<String> packageModel = new DefaultListModel<>();
    private final DefaultListModel<String> classModel = new DefaultListModel<>();
    private final JList<String> packageList = createList(packageModel);
    private final JList<String> classList = createList(classModel);
    private ChangeListener changeListener;

    ExclusionsPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(createHeader(), BorderLayout.NORTH);

        JPanel lists = new JPanel(new GridLayout(1, 2, 10, 0));
        lists.setOpaque(false);
        lists.add(createListCard(packagesTitle, packageList, true,
                "Se excluye el paquete y todos sus subpaquetes."));
        lists.add(createListCard(classesTitle, classList, false,
                "Acepta el nombre simple o completamente calificado."));
        add(lists, BorderLayout.CENTER);
        add(createHelpCard(), BorderLayout.SOUTH);
    }

    void setChangeListener(ChangeListener listener) {
        changeListener = listener;
    }

    void update(InventoryViewModel model) {
        replaceValues(packageModel, model.excludedPackages());
        replaceValues(classModel, model.excludedClasses());
        refreshLabels();
        source.setText("Origen: " + model.sourceDirectory().getParent()
                .resolve("analisis.properties"));
    }

    private JPanel createHeader() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 5));
        JLabel title = UiTheme.heading("Exclusiones del análisis");
        title.setFont(title.getFont().deriveFont(18f));
        card.add(title, BorderLayout.NORTH);
        card.add(summary, BorderLayout.CENTER);
        source.setForeground(UiTheme.MUTED);
        card.add(source, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createListCard(JLabel title, JList<String> list,
            boolean packages, String description) {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 8));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 3));
        heading.setOpaque(false);
        heading.add(title);
        JLabel hint = new JLabel(description);
        hint.setForeground(UiTheme.MUTED);
        heading.add(hint);
        card.add(heading, BorderLayout.NORTH);
        card.add(new JScrollPane(list), BorderLayout.CENTER);
        card.add(createActions(list, packages), BorderLayout.SOUTH);
        card.setMinimumSize(new Dimension(300, 300));
        return card;
    }

    private JPanel createActions(JList<String> list, boolean packages) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        actions.setOpaque(false);
        JButton remove = new JButton("Quitar selección");
        JButton add = new JButton(packages ? "Agregar paquete" : "Agregar clase");
        add.addActionListener(event -> addValue(packages));
        remove.addActionListener(event -> removeSelected(list, packages));
        actions.add(remove);
        actions.add(add);
        return actions;
    }

    private void addValue(boolean packages) {
        String label = packages ? "paquete" : "clase";
        String value = JOptionPane.showInputDialog(this,
                "Nombre del " + label + " que deseas excluir:",
                "Agregar exclusión", JOptionPane.PLAIN_MESSAGE);
        if (value == null) return;
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.contains(" ")) {
            JOptionPane.showMessageDialog(this,
                    "Ingresa un nombre Java válido sin espacios.",
                    "Exclusión inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        DefaultListModel<String> model = packages ? packageModel : classModel;
        if (!model.contains(normalized)) model.addElement(normalized);
        notifyChange();
    }

    private void removeSelected(JList<String> list, boolean packages) {
        String selected = list.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una exclusión primero.",
                    "Sin selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (InventoryUiService.AUTOMATIC_EXCLUSION.equals(selected)) {
            JOptionPane.showMessageDialog(this,
                    "AppGenerator se excluye automáticamente para no inventariar"
                    + " el punto de entrada del propio análisis.",
                    "Exclusión automática", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        (packages ? packageModel : classModel).removeElement(selected);
        notifyChange();
    }

    private void notifyChange() {
        refreshLabels();
        if (changeListener != null) {
            changeListener.exclusionsChanged(valuesOf(packageModel),
                    valuesOf(classModel));
        }
    }

    private void refreshLabels() {
        packagesTitle.setText("Paquetes excluidos (" + packageModel.size() + ")");
        classesTitle.setText("Clases excluidas (" + classModel.size() + ")");
        summary.setText((packageModel.size() + classModel.size())
                + " reglas de exclusión aplicadas al análisis actual");
    }

    private void replaceValues(DefaultListModel<String> model, Set<String> values) {
        model.clear();
        values.stream().sorted().forEach(model::addElement);
    }

    private Set<String> valuesOf(DefaultListModel<String> model) {
        Set<String> values = new LinkedHashSet<>();
        for (int index = 0; index < model.size(); index++) {
            values.add(model.get(index));
        }
        return values;
    }

    private JPanel createHelpCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new GridLayout(3, 1, 0, 3));
        card.add(UiTheme.heading("Persistencia de las exclusiones"));
        card.add(new JLabel("Los cambios se guardan en analisis.properties."));
        card.add(new JLabel("Después de cada cambio el proyecto se analiza nuevamente."));
        return card;
    }

    private static JList<String> createList(DefaultListModel<String> model) {
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(UiTheme.CODE);
        list.setBackground(new java.awt.Color(247, 248, 250));
        list.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
        return list;
    }
}
