package org.l2jmobius.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.l2jmobius.forge.ForgeItem;
import org.l2jmobius.forge.ItemForgeController;

public class ItemCreatorPanel extends JPanel {
    // UI Components
    private JTextField txtId;
    private JTextField txtName;
    private JTextField txtAddName;
    private JTextArea txtDesc;
    private JComboBox<String> cbType;
    private JTextField txtVisualTemplate;
    private JTextField txtIcon;

    // Server Specs
    private JComboBox<String> cbBodyPart;
    private JComboBox<String> cbMaterial;
    private JComboBox<String> cbCrystal;
    private JTextField txtWeight;
    private JTextField txtPrice;

    // Weapon Specifics
    private JTextField txtSoulshot;
    private JTextField txtSpiritshot;
    private JTextField txtMpConsume;

    // Labels
    private JLabel lblBodyPart, lblShots, lblMp;

    // XML Output
    private JTextArea txtServerXml;

    // Persistence
    private static File lastIconDir = new File(".");
    private static final String PREF_WIDTH = "forge_width";
    private static final String PREF_HEIGHT = "forge_height";

    public ItemCreatorPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---------------------------------------------------------
        // 0. WINDOW SIZE PERSISTENCE
        // ---------------------------------------------------------
        Preferences prefs = Preferences.userNodeForPackage(ItemCreatorPanel.class);
        int savedWidth = prefs.getInt(PREF_WIDTH, 950);
        int savedHeight = prefs.getInt(PREF_HEIGHT, 700);
        setPreferredSize(new Dimension(savedWidth, savedHeight));

        this.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0 && isDisplayable()) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    window.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent we) {
                            prefs.putInt(PREF_WIDTH, window.getWidth());
                            prefs.putInt(PREF_HEIGHT, window.getHeight());
                        }
                    });
                }
            }
        });

        // ---------------------------------------------------------
        // 1. HEADER & LOAD SECTION
        // ---------------------------------------------------------
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel lblTitle = new JLabel("Item Forge", JLabel.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(lblTitle, BorderLayout.NORTH);

        JPanel loadPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loadPanel.setBorder(BorderFactory.createTitledBorder("Load Existing"));

        JTextField txtLoadId = new JTextField(6);
        JButton btnLoad = new JButton("Load ID");

        btnLoad.addActionListener(e -> {
            try {
                int id = Integer.parseInt(txtLoadId.getText());
                ItemForgeController controller = getController();
                if (controller != null) {
                    ForgeItem loadedItem = controller.loadItem(id);
                    if (loadedItem != null) {
                        setFormData(loadedItem);
                        txtServerXml.setText(controller.generateServerXml(loadedItem));
                        JOptionPane.showMessageDialog(this, "Item Loaded Successfully!");
                    } else {
                        JOptionPane.showMessageDialog(this, "Item ID " + id + " not found.", "Not Found", JOptionPane.WARNING_MESSAGE);
                    }
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid ID format.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // --- RESET BUTTON ---
        JButton btnReset = new JButton("Reset View");
        btnReset.addActionListener(e -> {
            txtLoadId.setText("");
            resetForm();
        });

        loadPanel.add(new JLabel("ID:"));
        loadPanel.add(txtLoadId);
        loadPanel.add(btnLoad);
        loadPanel.add(btnReset); // Added

        headerPanel.add(loadPanel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // ---------------------------------------------------------
        // 2. MAIN SPLIT PANE
        // ---------------------------------------------------------
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        // LEFT: Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Column 1 ---
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Item ID:"), gbc);
        txtId = new JTextField(10);
        gbc.gridx = 1; formPanel.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Type:"), gbc);
        cbType = new JComboBox<>(new String[]{"Weapon", "Armor", "EtcItem"});
        cbType.addActionListener(e -> updateFieldVisibility());
        gbc.gridx = 1; formPanel.add(cbType, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Name:"), gbc);
        txtName = new JTextField(20);
        gbc.gridx = 1; formPanel.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Add. Name:"), gbc);
        txtAddName = new JTextField(20);
        gbc.gridx = 1; formPanel.add(txtAddName, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Description:"), gbc);
        txtDesc = new JTextArea(3, 20);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        gbc.gridx = 1; formPanel.add(new JScrollPane(txtDesc), gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Visual Template ID:"), gbc);
        JPanel pnlTemplate = new JPanel(new BorderLayout(5, 0));
        txtVisualTemplate = new JTextField(10);
        JLabel lblHint = new JLabel("(Source ID for Model)");
        lblHint.setFont(new Font("Arial", Font.ITALIC, 11));
        lblHint.setForeground(Color.GRAY);
        pnlTemplate.add(txtVisualTemplate, BorderLayout.CENTER);
        pnlTemplate.add(lblHint, BorderLayout.EAST);
        gbc.gridx = 1; formPanel.add(pnlTemplate, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Icon Name:"), gbc);
        JPanel pnlIcon = new JPanel(new BorderLayout(5, 0));
        txtIcon = new JTextField(15);
        JButton btnBrowseIcon = new JButton("...");
        btnBrowseIcon.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(lastIconDir);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                lastIconDir = fc.getCurrentDirectory();
                File selected = fc.getSelectedFile();
                String fileName = selected.getName();
                String folderName = selected.getParentFile().getName();
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) fileName = fileName.substring(0, dotIndex);
                txtIcon.setText(folderName + "." + fileName);
            }
        });
        pnlIcon.add(txtIcon, BorderLayout.CENTER);
        pnlIcon.add(btnBrowseIcon, BorderLayout.EAST);
        gbc.gridx = 1; formPanel.add(pnlIcon, gbc);

        // --- Column 2 ---
        int col2X = 2;
        int rowStart = 0;

        gbc.gridx = col2X; gbc.gridy = rowStart;
        lblBodyPart = new JLabel("Body Part:");
        formPanel.add(lblBodyPart, gbc);
        cbBodyPart = new JComboBox<>(new String[]{"rhand", "lhand", "lrhand", "chest", "legs", "feet", "head", "gloves", "none"});
        gbc.gridx = col2X + 1; formPanel.add(cbBodyPart, gbc);

        gbc.gridx = col2X; gbc.gridy++;
        formPanel.add(new JLabel("Material:"), gbc);
        cbMaterial = new JComboBox<>(new String[]{"steel", "fine_steel", "wood", "bone", "bronze", "leather", "cloth", "gold", "mithril", "liquid", "paper"});
        gbc.gridx = col2X + 1; formPanel.add(cbMaterial, gbc);

        gbc.gridx = col2X; gbc.gridy++;
        formPanel.add(new JLabel("Crystal Type:"), gbc);
        cbCrystal = new JComboBox<>(new String[]{"none", "d", "c", "b", "a", "s", "s80", "s84"});
        gbc.gridx = col2X + 1; formPanel.add(cbCrystal, gbc);

        gbc.gridx = col2X; gbc.gridy++;
        formPanel.add(new JLabel("Weight:"), gbc);
        txtWeight = new JTextField("1000", 6);
        gbc.gridx = col2X + 1; formPanel.add(txtWeight, gbc);

        gbc.gridx = col2X; gbc.gridy++;
        formPanel.add(new JLabel("Price:"), gbc);
        txtPrice = new JTextField("0", 6);
        gbc.gridx = col2X + 1; formPanel.add(txtPrice, gbc);

        gbc.gridx = col2X; gbc.gridy++;
        lblShots = new JLabel("Soul/Spirit Shot:");
        formPanel.add(lblShots, gbc);
        JPanel shotPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        txtSoulshot = new JTextField("1", 3);
        txtSpiritshot = new JTextField("1", 3);
        shotPanel.add(txtSoulshot);
        shotPanel.add(new JLabel(" / "));
        shotPanel.add(txtSpiritshot);
        gbc.gridx = col2X + 1; formPanel.add(shotPanel, gbc);

        gbc.gridx = col2X; gbc.gridy++;
        lblMp = new JLabel("MP Consume:");
        formPanel.add(lblMp, gbc);
        txtMpConsume = new JTextField("0", 5);
        gbc.gridx = col2X + 1; formPanel.add(txtMpConsume, gbc);

        // RIGHT: XML
        JPanel xmlPanel = new JPanel(new BorderLayout());
        xmlPanel.setBorder(BorderFactory.createTitledBorder("Server XML"));
        txtServerXml = new JTextArea();
        txtServerXml.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtServerXml.setEditable(false);
        xmlPanel.add(new JScrollPane(txtServerXml), BorderLayout.CENTER);

        splitPane.setLeftComponent(new JScrollPane(formPanel));
        splitPane.setRightComponent(xmlPanel);

        add(splitPane, BorderLayout.CENTER);

        // 3. FOOTER
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGenerate = new JButton("FORGE ITEM");
        btnGenerate.setFont(new Font("Arial", Font.BOLD, 14));
        btnGenerate.setBackground(new Color(100, 200, 100));

        btnGenerate.addActionListener(e -> {
            try {
                ForgeItem item = getFormData();
                ItemForgeController controller = getController();
                if (controller != null) {
                    txtServerXml.setText(controller.generateServerXml(item));
                    controller.createItem(item);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        footer.add(btnGenerate);
        add(footer, BorderLayout.SOUTH);

        updateFieldVisibility();
    }

    // --- Helpers ---
    private void updateFieldVisibility() {
        String type = (String) cbType.getSelectedItem();
        boolean isWeapon = "Weapon".equalsIgnoreCase(type);
        boolean isArmor = "Armor".equalsIgnoreCase(type);
        cbBodyPart.setEnabled(isWeapon || isArmor);
        lblBodyPart.setEnabled(isWeapon || isArmor);
        txtSoulshot.setEnabled(isWeapon);
        txtSpiritshot.setEnabled(isWeapon);
        lblShots.setEnabled(isWeapon);
        txtMpConsume.setEnabled(isWeapon);
        lblMp.setEnabled(isWeapon);
    }

    // --- RESET VIEW FUNCTION ---
    private void resetForm() {
        txtId.setText("");
        txtName.setText("");
        txtAddName.setText("");
        txtDesc.setText("");

        // Reset Combo Boxes
        if (cbType.getItemCount() > 0) cbType.setSelectedIndex(0);
        if (cbBodyPart.getItemCount() > 0) cbBodyPart.setSelectedIndex(0);
        if (cbMaterial.getItemCount() > 0) cbMaterial.setSelectedIndex(0);
        if (cbCrystal.getItemCount() > 0) cbCrystal.setSelectedIndex(0);

        txtVisualTemplate.setText("");
        txtIcon.setText("");

        // Reset defaults
        txtWeight.setText("1000");
        txtPrice.setText("0");
        txtSoulshot.setText("1");
        txtSpiritshot.setText("1");
        txtMpConsume.setText("0");

        txtServerXml.setText("");

        updateFieldVisibility();
    }

    private ItemForgeController getController() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        org.l2jmobius.L2ClientDat mainFrame = null;
        if (parent instanceof org.l2jmobius.L2ClientDat) {
            mainFrame = (org.l2jmobius.L2ClientDat) parent;
        } else if (parent instanceof javax.swing.JDialog) {
            Window owner = ((javax.swing.JDialog) parent).getOwner();
            if (owner instanceof org.l2jmobius.L2ClientDat) {
                mainFrame = (org.l2jmobius.L2ClientDat) owner;
            }
        }
        if (mainFrame != null) {
            ItemForgeController controller = new ItemForgeController();
            controller.setMainFrame(mainFrame);
            return controller;
        }
        return null;
    }

    private ForgeItem getFormData() {
        ForgeItem item = new ForgeItem();
        try { item.setId(Integer.parseInt(txtId.getText())); } catch (Exception e) { item.setId(0); }
        item.setName(txtName.getText());
        item.setAdditionalName(txtAddName.getText());
        item.setDescription(txtDesc.getText());
        item.setType((String) cbType.getSelectedItem());
        item.setVisualTemplate(txtVisualTemplate.getText());
        item.setIcon(txtIcon.getText());
        item.setBodyPart((String) cbBodyPart.getSelectedItem());
        item.setMaterial((String) cbMaterial.getSelectedItem());
        item.setCrystalType((String) cbCrystal.getSelectedItem());
        try { item.setWeight(Integer.parseInt(txtWeight.getText())); } catch (Exception e) {}
        try { item.setPrice(Long.parseLong(txtPrice.getText())); } catch (Exception e) {}
        try { item.setSoulshotCount(Integer.parseInt(txtSoulshot.getText())); } catch (Exception e) {}
        try { item.setSpiritshotCount(Integer.parseInt(txtSpiritshot.getText())); } catch (Exception e) {}
        try { item.setMpConsume(Integer.parseInt(txtMpConsume.getText())); } catch (Exception e) {}
        return item;
    }

    private void setFormData(ForgeItem item) {
        txtId.setText(String.valueOf(item.getId()));
        txtName.setText(item.getName());
        txtAddName.setText(item.getAdditionalName());
        txtDesc.setText(item.getDescription());
        txtIcon.setText(item.getIcon());
        if (item.getVisualTemplate() == null || item.getVisualTemplate().isEmpty()) {
            txtVisualTemplate.setText(String.valueOf(item.getId()));
        } else {
            txtVisualTemplate.setText(item.getVisualTemplate());
        }
        if (item.getType() != null) cbType.setSelectedItem(item.getType());
        updateFieldVisibility();
        if (item.getBodyPart() != null) cbBodyPart.setSelectedItem(item.getBodyPart());
        if (item.getMaterial() != null) cbMaterial.setSelectedItem(item.getMaterial());
        if (item.getCrystalType() != null) cbCrystal.setSelectedItem(item.getCrystalType());
        txtWeight.setText(String.valueOf(item.getWeight()));
        txtPrice.setText(String.valueOf(item.getPrice()));
        txtSoulshot.setText(String.valueOf(item.getSoulshotCount()));
        txtSpiritshot.setText(String.valueOf(item.getSpiritshotCount()));
        txtMpConsume.setText(String.valueOf(item.getMpConsume()));
    }
}