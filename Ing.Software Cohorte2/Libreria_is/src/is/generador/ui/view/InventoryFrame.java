package is.generador.ui.view;

import is.generador.core.domain.classifier.UmlClassifier;
import is.generador.ui.model.DiagnosticEntry;
import is.generador.ui.model.InventoryViewModel;
import is.generador.ui.service.InventoryUiService;
import is.generador.ui.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public final class InventoryFrame extends JFrame {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm:ss");

    private final InventoryUiService service;
    private final SummaryPanel summaryPanel = new SummaryPanel();
    private final DetailPanel detailPanel = new DetailPanel();
    private final RelationshipsPanel relationshipsPanel = new RelationshipsPanel();
    private final ExclusionsPanel exclusionsPanel = new ExclusionsPanel();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextField pathField = new JTextField();
    private final JTextField searchField = new JTextField();
    private final JLabel stateLabel = new JLabel("Listo para analizar");
    private final JLabel footerLabel = new JLabel("Sin análisis");
    private final JButton copyButton = new JButton("Copiar reporte");
    private final JButton exportButton = new JButton("Exportar");
    private final DefaultMutableTreeNode treeRoot =
            new DefaultMutableTreeNode("Proyecto");
    private final JTree projectTree = new JTree(treeRoot);
    private final DefaultTableModel structureTableModel = new DefaultTableModel(
            new Object[]{"Tipo", "Nombre", "Paquete", "Props", "Ops", "Vis."}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable structureTable = new JTable(structureTableModel);
    private final JPanel diagnosticsContent = new JPanel();
    private final Map<Integer, UmlClassifier> tableClassifiers = new LinkedHashMap<>();
    private InventoryViewModel currentModel;
    private Path currentSource;

    public InventoryFrame(InventoryUiService service, Path initialSource) {
        super("Libreria_is · Inventario del proyecto");
        this.service = java.util.Objects.requireNonNull(service, "service");
        currentSource = initialSource.toAbsolutePath().normalize();
        configureWindow();
        buildLayout();
        bindActions();
        exclusionsPanel.setChangeListener(this::saveExclusions);
        analyze(currentSource);
    }

    private void configureWindow() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(980, 640));
        setPreferredSize(new Dimension(1280, 790));
        getContentPane().setBackground(UiTheme.BACKGROUND);
    }

    private void buildLayout() {
        setLayout(new BorderLayout());
        add(createTopBar(), BorderLayout.NORTH);

        tabs.addTab("Resumen", summaryPanel);
        tabs.addTab("Estructura", createStructurePanel());
        tabs.addTab("Relaciones", relationshipsPanel);
        tabs.addTab("Detalle", detailPanel);
        tabs.addTab("Exclusiones", exclusionsPanel);
        tabs.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createSidebar(), tabs);
        splitPane.setDividerLocation(255);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);
        splitPane.setResizeWeight(0);
        add(splitPane, BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBackground(UiTheme.SURFACE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(9, 14, 9, 14)));
        JLabel brand = new JLabel("▣  Libreria_is");
        brand.setFont(new Font("SansSerif", Font.BOLD, 14));
        brand.setForeground(UiTheme.PRIMARY);
        topBar.add(brand, BorderLayout.WEST);

        pathField.setEditable(false);
        pathField.setBackground(new Color(247, 248, 250));
        pathField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        topBar.add(pathField, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        stateLabel.setForeground(UiTheme.SUCCESS);
        actions.add(stateLabel);
        topBar.add(actions, BorderLayout.EAST);
        return topBar;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 8));
        sidebar.setBackground(UiTheme.SURFACE);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(9, 9, 9, 9)));
        searchField.putClientProperty("JTextField.placeholderText", "Buscar...");
        sidebar.add(searchField, BorderLayout.NORTH);
        projectTree.setRootVisible(false);
        projectTree.setShowsRootHandles(true);
        projectTree.setCellRenderer(new ClassifierTreeRenderer());
        projectTree.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        sidebar.add(new JScrollPane(projectTree), BorderLayout.CENTER);
        JLabel countLabel = new JLabel("Paquetes y clasificadores");
        countLabel.setForeground(UiTheme.MUTED);
        sidebar.add(countLabel, BorderLayout.SOUTH);
        sidebar.setMinimumSize(new Dimension(210, 300));
        return sidebar;
    }

    private JPanel createStructurePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UiTheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        structureTable.setRowHeight(25);
        structureTable.setShowVerticalLines(false);
        structureTable.setGridColor(UiTheme.BORDER);
        structureTable.setAutoCreateRowSorter(true);
        structureTable.getTableHeader().setBackground(new Color(232, 235, 240));
        structureTable.getTableHeader().setFont(UiTheme.HEADING);
        panel.add(new JScrollPane(structureTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRelationshipsPlaceholder() {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.setBackground(UiTheme.BACKGROUND);
        JPanel card = UiTheme.card();
        card.setLayout(new GridLayout(3, 1, 0, 8));
        JLabel icon = new JLabel("◇", JLabel.CENTER);
        icon.setFont(new Font("SansSerif", Font.BOLD, 38));
        icon.setForeground(UiTheme.PRIMARY);
        JLabel title = new JLabel("Visualización de relaciones", JLabel.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        JLabel message = new JLabel(
                "Módulo reservado para una fase posterior · Los datos ya se detectan en el resumen y detalle",
                JLabel.CENTER);
        message.setForeground(UiTheme.MUTED);
        card.add(icon);
        card.add(title);
        card.add(message);
        panel.setBorder(BorderFactory.createEmptyBorder(55, 70, 55, 70));
        panel.add(card);
        return panel;
    }

    private JScrollPane createDiagnosticsPanel() {
        diagnosticsContent.setLayout(new javax.swing.BoxLayout(
                diagnosticsContent, javax.swing.BoxLayout.Y_AXIS));
        diagnosticsContent.setBackground(UiTheme.BACKGROUND);
        diagnosticsContent.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return new JScrollPane(diagnosticsContent);
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UiTheme.SURFACE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        footerLabel.setForeground(UiTheme.MUTED);
        footer.add(footerLabel, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        actions.setOpaque(false);
        JButton diagramButton = new JButton("Guardar PUML");
        diagramButton.addActionListener(event -> exportPuml());
        actions.add(copyButton);
        actions.add(exportButton);
        actions.add(diagramButton);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private void bindActions() {
        copyButton.addActionListener(event -> copyReport());
        exportButton.addActionListener(event -> exportReport());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { rebuildTree(); }
            @Override public void removeUpdate(DocumentEvent event) { rebuildTree(); }
            @Override public void changedUpdate(DocumentEvent event) { rebuildTree(); }
        });
        projectTree.addTreeSelectionListener(event -> {
            Object selectedNode = projectTree.getLastSelectedPathComponent();
            if (!(selectedNode instanceof DefaultMutableTreeNode treeNode)) return;
            Object selected = treeNode.getUserObject();
            if (selected instanceof UmlClassifier classifier) {
                showDetail(classifier);
            }
        });
        structureTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || structureTable.getSelectedRow() < 0) return;
            int modelRow = structureTable.convertRowIndexToModel(
                    structureTable.getSelectedRow());
            showDetail(tableClassifiers.get(modelRow));
        });
    }

    private void analyze(Path sourceDirectory) {
        setBusy(true, "Analizando...");
        new SwingWorker<InventoryViewModel, Void>() {
            @Override protected InventoryViewModel doInBackground() throws Exception {
                return service.analyze(sourceDirectory);
            }
            @Override protected void done() {
                try {
                    currentModel = get();
                    currentSource = currentModel.sourceDirectory();
                    refreshDashboard();
                    setBusy(false, "● Análisis completado");
                } catch (Exception exception) {
                    setBusy(false, "● Error de análisis");
                    stateLabel.setForeground(UiTheme.ERROR);
                    JOptionPane.showMessageDialog(InventoryFrame.this,
                            rootMessage(exception), "No se pudo analizar",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshDashboard() {
        pathField.setText(currentSource.toString());
        summaryPanel.update(currentModel);
        relationshipsPanel.update(currentModel.umlModel());
        detailPanel.setModel(currentModel);
        exclusionsPanel.update(currentModel);
        rebuildTree();
        rebuildTable();
        rebuildDiagnostics();
        footerLabel.setText(currentModel.umlModel().classifiers().size()
                + " clasificadores · Último análisis: "
                + currentModel.analyzedAt().format(TIME_FORMAT));
    }

    private void rebuildTree() {
        treeRoot.removeAllChildren();
        if (currentModel == null) {
            ((DefaultTreeModel) projectTree.getModel()).reload();
            return;
        }
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        Map<String, DefaultMutableTreeNode> packages = new LinkedHashMap<>();
        currentModel.umlModel().classifiers().values().stream()
                .sorted(Comparator.comparing(UmlClassifier::qualifiedName))
                .filter(value -> query.isEmpty()
                || value.qualifiedName().toLowerCase(Locale.ROOT).contains(query))
                .forEach(classifier -> {
                    String packageName = classifier.namespace().toString();
                    DefaultMutableTreeNode packageNode = packages.computeIfAbsent(
                            packageName, name -> {
                                DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
                                treeRoot.add(node);
                                return node;
                            });
                    packageNode.add(new DefaultMutableTreeNode(classifier));
                });
        ((DefaultTreeModel) projectTree.getModel()).reload();
        for (int row = 0; row < Math.min(projectTree.getRowCount(), 8); row++) {
            projectTree.expandRow(row);
        }
    }

    private void rebuildTable() {
        structureTableModel.setRowCount(0);
        tableClassifiers.clear();
        currentModel.umlModel().classifiers().values().stream()
                .sorted(Comparator.comparing(UmlClassifier::qualifiedName))
                .forEach(classifier -> {
                    int row = structureTableModel.getRowCount();
                    structureTableModel.addRow(new Object[]{
                        classifier.classification(), classifier.name(),
                        classifier.namespace(), classifier.properties().size(),
                        classifier.operations().size(), classifier.visibility()
                    });
                    tableClassifiers.put(row, classifier);
                });
    }

    private void rebuildDiagnostics() {
        diagnosticsContent.removeAll();
        List<DiagnosticEntry> entries = new ArrayList<>(currentModel.diagnostics());
        if (entries.isEmpty()) {
            entries.add(new DiagnosticEntry(DiagnosticEntry.Severity.INFO,
                    "Análisis", "No se reportaron errores ni advertencias."));
        }
        for (DiagnosticEntry entry : entries) {
            JPanel card = UiTheme.card();
            card.setLayout(new BorderLayout(8, 4));
            Color color = switch (entry.severity()) {
                case ERROR -> UiTheme.ERROR;
                case WARNING -> UiTheme.WARNING;
                case INFO -> new Color(2, 132, 199);
            };
            JLabel level = new JLabel(entry.severity().name());
            level.setForeground(color);
            level.setFont(UiTheme.HEADING);
            card.add(level, BorderLayout.WEST);
            card.add(new JLabel("<html><b>" + entry.source() + "</b><br>"
                    + entry.message() + "</html>"), BorderLayout.CENTER);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
            diagnosticsContent.add(card);
            diagnosticsContent.add(javax.swing.Box.createVerticalStrut(8));
        }
        diagnosticsContent.revalidate();
        diagnosticsContent.repaint();
    }

    private void showDetail(UmlClassifier classifier) {
        if (classifier == null) return;
        detailPanel.showClassifier(classifier);
        tabs.setSelectedComponent(detailPanel);
    }

    private void selectProject() {
        JFileChooser chooser = new JFileChooser(currentSource.toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Seleccionar proyecto o carpeta src");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            analyze(chooser.getSelectedFile().toPath());
        }
    }

    private void saveExclusions(java.util.Set<String> excludedPackages,
            java.util.Set<String> excludedClasses, java.util.Set<String> whitelist,
            java.util.Set<String> blacklist) {
        try {
            service.saveConfiguration(currentSource, excludedPackages, excludedClasses,
                    whitelist, blacklist);
            footerLabel.setText("Exclusiones y políticas guardadas. Actualizando análisis...");
            analyze(currentSource);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "No se pudieron guardar las exclusiones",
                    JOptionPane.ERROR_MESSAGE);
            exclusionsPanel.update(currentModel);
        }
    }

    private void exportPuml() {
        if (currentModel == null) {
            JOptionPane.showMessageDialog(this, "Espera a que termine el análisis.",
                    "Guardar PUML", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        java.awt.FileDialog chooser = new java.awt.FileDialog(this,
                "Guardar diagrama PlantUML", java.awt.FileDialog.SAVE);
        chooser.setDirectory(javax.swing.filechooser.FileSystemView.getFileSystemView()
                .getHomeDirectory().getAbsolutePath());
        chooser.setFile("diagrama-proyecto.puml");
        String selectedFile;
        String selectedDirectory;
        try {
            chooser.setVisible(true);
            selectedFile = chooser.getFile();
            selectedDirectory = chooser.getDirectory();
        } finally {
            chooser.dispose();
        }
        if (selectedFile == null || selectedDirectory == null) return;
        Path target = Path.of(selectedDirectory).resolve(selectedFile).toAbsolutePath().normalize();
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".puml"))
            target = target.resolveSibling(target.getFileName() + ".puml");
        if (Files.exists(target) && JOptionPane.showConfirmDialog(this,
                "El archivo ya existe. ¿Deseas reemplazarlo?", "Guardar PUML",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            new is.generador.SoyLaPuertaJava().guardarPuml(currentModel.umlModel(), target);
            if (!Files.isRegularFile(target) || Files.size(target) == 0)
                throw new IOException("No se creó el archivo PUML: " + target);
            footerLabel.setText("PUML guardado: " + target);
            JOptionPane.showMessageDialog(this, "Archivo guardado en:\n" + target,
                    "PUML guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | RuntimeException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "No se pudo guardar el PUML", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void copyReport() {
        if (currentModel == null) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(currentModel.report()), null);
        footerLabel.setText("Reporte copiado al portapapeles");
    }

    private void exportReport() {
        if (currentModel == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("inventario-proyecto.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Files.writeString(chooser.getSelectedFile().toPath(), currentModel.report());
            footerLabel.setText("Reporte exportado correctamente");
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "No se pudo exportar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setBusy(boolean busy, String state) {
        stateLabel.setText(state);
        stateLabel.setForeground(busy ? UiTheme.WARNING : UiTheme.SUCCESS);
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(UiTheme.PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    private JButton findButton(Component root, String actionCommand) {
        if (root instanceof JButton button
                && actionCommand.equals(button.getActionCommand())) return button;
        if (root instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                JButton result = findButton(child, actionCommand);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static final class ClassifierTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded,
                    leaf, row, hasFocus);
            Object item = ((DefaultMutableTreeNode) value).getUserObject();
            if (item instanceof UmlClassifier classifier) {
                setText(classifier.name());
                setToolTipText(classifier.qualifiedName());
                setForeground(selected ? Color.WHITE : colorFor(classifier));
            } else {
                setText("▸ " + item);
            }
            return this;
        }

        private Color colorFor(UmlClassifier classifier) {
            return switch (classifier.classification()) {
                case INTERFACE -> new Color(8, 145, 178);
                case ENUMERATION -> UiTheme.WARNING;
                case RECORD -> UiTheme.SUCCESS;
                case ANNOTATION -> new Color(219, 39, 119);
                case CLASS -> UiTheme.PRIMARY;
            };
        }
    }
}
