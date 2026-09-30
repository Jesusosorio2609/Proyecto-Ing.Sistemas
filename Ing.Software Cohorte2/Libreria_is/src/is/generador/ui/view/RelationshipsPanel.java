package is.generador.ui.view;

import is.generador.core.domain.model.UmlModel;
import is.generador.core.domain.relationship.RelationshipSelection;
import is.generador.core.domain.relationship.UmlAssociation;
import is.generador.ui.theme.UiTheme;
import java.awt.BorderLayout;
import java.util.Comparator;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

final class RelationshipsPanel extends JPanel {
    private final JLabel count = new JLabel("Sin análisis");
    private final DefaultTableModel rows = new DefaultTableModel(
            new Object[]{"Origen", "Relación", "Destino", "Mult. origen", "Mult. destino"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    RelationshipsPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel header = UiTheme.card();
        header.setLayout(new BorderLayout(0, 5));
        header.add(UiTheme.heading("Todas las relaciones detectadas"), BorderLayout.NORTH);
        header.add(count, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JTable table = new JTable(rows);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setBackground(UiTheme.SURFACE);
        table.setForeground(UiTheme.TEXT);
        table.setSelectionBackground(UiTheme.PRIMARY);
        table.setSelectionForeground(java.awt.Color.WHITE);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(
                    JTable owner, Object value, boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(owner, value, selected, focused, row, column);
                setForeground(selected ? java.awt.Color.WHITE : UiTheme.TEXT);
                setBackground(selected ? UiTheme.PRIMARY : UiTheme.SURFACE);
                setToolTipText(value == null ? null : value.toString());
                return this;
            }
        });
        table.setRowHeight(28);
        table.setFont(UiTheme.CODE);
        table.getTableHeader().setFont(UiTheme.HEADING);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {300, 150, 300, 100, 100};
        for (int index = 0; index < widths.length; index++)
            table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    void update(UmlModel model) {
        rows.setRowCount(0);
        model.relationships().stream()
                .sorted(Comparator.comparing(RelationshipSelection::key))
                .forEach(relationship -> {
                    String sourceMultiplicity = "—";
                    String targetMultiplicity = "—";
                    if (relationship instanceof UmlAssociation association) {
                        sourceMultiplicity = association.sourceMultiplicity();
                        targetMultiplicity = association.targetMultiplicity();
                    }
                    rows.addRow(new Object[]{relationship.source().qualifiedName(),
                        RelationshipSelection.kind(relationship),
                        relationship.target().qualifiedName(),
                        sourceMultiplicity, targetMultiplicity});
                });
        count.setText(rows.getRowCount() == 0 ? "No se detectaron relaciones"
                : rows.getRowCount() + " relaciones detectadas");
    }
}
