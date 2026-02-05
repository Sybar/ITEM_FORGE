package org.l2jmobius.ui;

import org.l2jmobius.forge.ForgeItem;
import org.l2jmobius.forge.ItemForgeController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ItemCreatorPanel extends JPanel {

    private static final String VISUAL_TEMPLATE_HINT = "Enter Item ID to copy visuals from...";
    private static File _lastIconDir;

    // Core inputs
    private JTextField txtId;
    private JTextField txtName;
    private JTextField txtAddName; // The yellow text
    private JTextArea txtDescription; // Description field
    private JComboBox<String> cbType; // Weapon, Armor, EtcItem

    // Visual inputs
    private JTextField txtIcon;
    private JButton btnSelectIcon;
    private JTextField txtVisualTemplate; // e.g., "Use Draconic Bow mesh"

    // Server Specs inputs (Common)
    private JComboBox<String> cbMaterial;
    private JComboBox<String> cbCrystalType;
    private JTextField txtWeight;
    private JTextField txtPrice;

    // Dynamic Panels
    private JPanel pnlWeaponSpecs;
    private JPanel pnlArmorSpecs;
    private JPanel pnlEtcSpecs;
    private JPanel pnlDynamicContainer;

    // Weapon Specs
    private JComboBox<String> cbWeaponBodyPart;
    private JTextField txtSoulshotCount;
    private JTextField txtSpiritshotCount;
    private JTextField txtRandomDamage;
    private JCheckBox chkIsMagicWeapon;
    private JTextField txtMpConsume;

    // Armor Specs
    private JComboBox<String> cbArmorBodyPart;
    private JTextField txtDamageReduction;

    // EtcItem Specs
    private JCheckBox chkStackable;
    private JCheckBox chkQuestItem;
    private JCheckBox chkImmediateEffect;
    private JComboBox<String> cbEtcItemType;

    // Stats Table
    private DefaultTableModel statsModel;
    private JTable statsTable;

    public ItemCreatorPanel() {
        setLayout(new BorderLayout());

        // --- Header Actions ---
        JToolBar toolbar = new JToolBar();
        JButton btnLoadTemplate = new JButton("Load Existing Item");
        btnLoadTemplate.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "This feature is not yet implemented.", "Info", JOptionPane.INFORMATION_MESSAGE);
        });
        JButton btnClear = new JButton("Reset");
        toolbar.add(btnLoadTemplate);
        toolbar.add(btnClear);
        add(toolbar, BorderLayout.NORTH);

        // --- Main Tabs ---
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1. Identity & Visuals", createIdentityPanel());
        tabs.addTab("2. Server Specs", createServerSpecsPanel());
        tabs.addTab("3. Combat Stats", createStatsPanel());

        add(tabs, BorderLayout.CENTER);

        // --- Footer Actions ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGenerate = new JButton("FORGE ITEM (Generate Files)");
        btnGenerate.setFont(new Font("Arial", Font.BOLD, 14));
        btnGenerate.setBackground(new Color(100, 200, 100)); // Light Green
        btnGenerate.addActionListener(e -> {
            try {
                // 1. Get Data from Form
                ForgeItem item = getFormData();
                ItemForgeController controller = new ItemForgeController();
                
                // 2. CRITICAL FIX: Traverse up to find the Main Window (L2ClientDat)
                java.awt.Window parent = javax.swing.SwingUtilities.getWindowAncestor(this);
                org.l2jmobius.L2ClientDat mainFrame = null;

                if (parent instanceof org.l2jmobius.L2ClientDat) {
                    mainFrame = (org.l2jmobius.L2ClientDat) parent;
                } else if (parent instanceof javax.swing.JDialog) {
                    // If we are in a popup dialog, get its owner
                    java.awt.Window owner = ((javax.swing.JDialog) parent).getOwner();
                    if (owner instanceof org.l2jmobius.L2ClientDat) {
                        mainFrame = (org.l2jmobius.L2ClientDat) owner;
                    }
                }

                // 3. Pass the Main Frame to the Controller
                if (mainFrame != null) {
                    controller.setMainFrame(mainFrame);
                    controller.createItem(item);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Internal Error: Could not find Main Window context.\nPlease restart the tool.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
                    
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "Error creating item: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        footer.add(btnGenerate);
        add(footer, BorderLayout.SOUTH);
        
        // Initialize dynamic panel state
        updateDynamicPanels();
    }

    private JPanel createIdentityPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1: ID and Type
        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Item ID:"), gbc);
        gbc.gridx = 1; txtId = new JTextField(10); panel.add(txtId, gbc);

        gbc.gridx = 2; panel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 3;
        String[] types = {"Weapon", "Armor", "EtcItem"}; // Derived from your XML types
        cbType = new JComboBox<>(types);
        cbType.addActionListener(e -> updateDynamicPanels());
        panel.add(cbType, gbc);

        // Row 2: Names
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        txtName = new JTextField(20); panel.add(txtName, gbc);
        gbc.gridwidth = 1;
        
        // Row 3: Additional Name
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Add. Name:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        txtAddName = new JTextField(20); panel.add(txtAddName, gbc);
        gbc.gridwidth = 1;
        
        // Row 4: Description
        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        txtDescription = new JTextArea(3, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane scrollDescription = new JScrollPane(txtDescription);
        panel.add(scrollDescription, gbc);
        gbc.gridwidth = 1;

        // Row 5: Visual Template (The "Copy From" feature)
        gbc.gridx = 0; gbc.gridy = 4; panel.add(new JLabel("Visual Template:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        txtVisualTemplate = new JTextField(VISUAL_TEMPLATE_HINT);
        txtVisualTemplate.setForeground(Color.GRAY);
        txtVisualTemplate.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtVisualTemplate.getText().equals(VISUAL_TEMPLATE_HINT)) {
                    txtVisualTemplate.setText("");
                    txtVisualTemplate.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtVisualTemplate.getText().isEmpty()) {
                    txtVisualTemplate.setText(VISUAL_TEMPLATE_HINT);
                    txtVisualTemplate.setForeground(Color.GRAY);
                }
            }
        });
        panel.add(txtVisualTemplate, gbc);

        // Row 6: Icon
        gbc.gridx = 0; gbc.gridy = 5; panel.add(new JLabel("Icon String:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        txtIcon = new JTextField();
        panel.add(txtIcon, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 3;
        btnSelectIcon = new JButton("Browse...");
        btnSelectIcon.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(_lastIconDir);
            fileChooser.setDialogTitle("Select Icon");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                _lastIconDir = fileChooser.getCurrentDirectory();
                
                // Construct icon string: ParentDir.FileNameWithoutExtension
                String parentName = selectedFile.getParentFile().getName();
                String fileName = selectedFile.getName();
                if (fileName.lastIndexOf('.') > 0) {
                    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                }
                String iconString = parentName + "." + fileName;
                
                txtIcon.setText(iconString);
            }
        });
        panel.add(btnSelectIcon, gbc);

        return panel;
    }

    private JPanel createServerSpecsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Common Specs Panel
        JPanel commonPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        commonPanel.setBorder(BorderFactory.createTitledBorder("Common Specifications"));

        commonPanel.add(new JLabel("Material:"));
        String[] materials = {"STEEL", "FINE_STEEL", "WOOD", "CLOTH", "LEATHER", "BONE", "BRONZE", "GOLD", "LIQUID"};
        cbMaterial = new JComboBox<>(materials);
        commonPanel.add(cbMaterial);

        commonPanel.add(new JLabel("Crystal Type:"));
        String[] crystals = {"NONE", "D", "C", "B", "A", "S", "S80", "S84"};
        cbCrystalType = new JComboBox<>(crystals);
        commonPanel.add(cbCrystalType);

        commonPanel.add(new JLabel("Weight:"));
        txtWeight = new JTextField("0");
        commonPanel.add(txtWeight);

        commonPanel.add(new JLabel("Price (Adena):"));
        txtPrice = new JTextField("0");
        commonPanel.add(txtPrice);
        
        mainPanel.add(commonPanel, BorderLayout.NORTH);

        // Dynamic Container
        pnlDynamicContainer = new JPanel(new CardLayout());
        
        // Weapon Specs Panel
        pnlWeaponSpecs = new JPanel(new GridLayout(0, 2, 10, 10));
        pnlWeaponSpecs.setBorder(BorderFactory.createTitledBorder("Weapon Specifications"));
        pnlWeaponSpecs.add(new JLabel("Body Part:"));
        String[] weaponBodyParts = {"rhand", "lrhand", "bigsword", "dual"};
        cbWeaponBodyPart = new JComboBox<>(weaponBodyParts);
        pnlWeaponSpecs.add(cbWeaponBodyPart);
        pnlWeaponSpecs.add(new JLabel("Soulshot Usage:"));
        txtSoulshotCount = new JTextField("1");
        pnlWeaponSpecs.add(txtSoulshotCount);
        pnlWeaponSpecs.add(new JLabel("Spiritshot Usage:"));
        txtSpiritshotCount = new JTextField("1");
        pnlWeaponSpecs.add(txtSpiritshotCount);
        pnlWeaponSpecs.add(new JLabel("Random Damage:"));
        txtRandomDamage = new JTextField("10");
        pnlWeaponSpecs.add(txtRandomDamage);
        pnlWeaponSpecs.add(new JLabel("MP Consume:"));
        txtMpConsume = new JTextField("0");
        pnlWeaponSpecs.add(txtMpConsume);
        pnlWeaponSpecs.add(new JLabel("Is Magic Weapon:"));
        chkIsMagicWeapon = new JCheckBox();
        pnlWeaponSpecs.add(chkIsMagicWeapon);
        
        // Armor Specs Panel
        pnlArmorSpecs = new JPanel(new GridLayout(0, 2, 10, 10));
        pnlArmorSpecs.setBorder(BorderFactory.createTitledBorder("Armor Specifications"));
        pnlArmorSpecs.add(new JLabel("Body Part:"));
        String[] armorBodyParts = {"chest", "legs", "gloves", "feet", "head", "fullarmor", "hair", "face"};
        cbArmorBodyPart = new JComboBox<>(armorBodyParts);
        pnlArmorSpecs.add(cbArmorBodyPart);
        pnlArmorSpecs.add(new JLabel("Damage Reduction:"));
        txtDamageReduction = new JTextField("0");
        pnlArmorSpecs.add(txtDamageReduction);
        
        // EtcItem Specs Panel
        pnlEtcSpecs = new JPanel(new GridLayout(0, 2, 10, 10));
        pnlEtcSpecs.setBorder(BorderFactory.createTitledBorder("EtcItem Specifications"));
        pnlEtcSpecs.add(new JLabel("EtcItem Type:"));
        String[] etcTypes = {"POTION", "SCROLL", "RECIPE", "MATERIAL", "OTHER"};
        cbEtcItemType = new JComboBox<>(etcTypes);
        pnlEtcSpecs.add(cbEtcItemType);
        pnlEtcSpecs.add(new JLabel("Stackable:"));
        chkStackable = new JCheckBox();
        pnlEtcSpecs.add(chkStackable);
        pnlEtcSpecs.add(new JLabel("Quest Item:"));
        chkQuestItem = new JCheckBox();
        pnlEtcSpecs.add(chkQuestItem);
        pnlEtcSpecs.add(new JLabel("Immediate Effect:"));
        chkImmediateEffect = new JCheckBox();
        pnlEtcSpecs.add(chkImmediateEffect);

        pnlDynamicContainer.add(pnlWeaponSpecs, "Weapon");
        pnlDynamicContainer.add(pnlArmorSpecs, "Armor");
        pnlDynamicContainer.add(pnlEtcSpecs, "EtcItem");
        
        mainPanel.add(pnlDynamicContainer, BorderLayout.CENTER);

        return mainPanel;
    }
    
    private void updateDynamicPanels() {
        CardLayout cl = (CardLayout) (pnlDynamicContainer.getLayout());
        String selectedType = (String) cbType.getSelectedItem();
        cl.show(pnlDynamicContainer, selectedType);
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Stats Table Column
        String[] columns = {"Stat Name (e.g., pAtk)", "Value", "Operation (Set/Add)"};
        statsModel = new DefaultTableModel(columns, 0);
        statsTable = new JTable(statsModel);

        // Pre-fill some common stats from your XML
        statsModel.addRow(new Object[]{"pAtk", "0", "set"});
        statsModel.addRow(new Object[]{"mAtk", "0", "set"});
        statsModel.addRow(new Object[]{"critRate", "0", "set"});
        statsModel.addRow(new Object[]{"pAtkSpd", "0", "set"});

        JScrollPane scroll = new JScrollPane(statsTable);
        panel.add(scroll, BorderLayout.CENTER);

        // Add/Remove buttons
        JPanel buttons = new JPanel();
        JButton btnAdd = new JButton("+ Add Stat");
        JButton btnRem = new JButton("- Remove Selected");

        btnAdd.addActionListener(e -> statsModel.addRow(new Object[]{"", "", "set"}));
        btnRem.addActionListener(e -> {
            if(statsTable.getSelectedRow() != -1)
                statsModel.removeRow(statsTable.getSelectedRow());
        });

        buttons.add(btnAdd);
        buttons.add(btnRem);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private ForgeItem getFormData() {
        ForgeItem item = new ForgeItem();
        try {
            item.setId(Integer.parseInt(txtId.getText()));
        } catch (NumberFormatException e) {
            item.setId(0);
        }
        item.setName(txtName.getText());
        item.setAdditionalName(txtAddName.getText());
        
        // Capture Description and sanitize newlines
        String rawDesc = txtDescription.getText();
        if (rawDesc != null) {
            item.setDescription(rawDesc.replace("\n", "\\n"));
        } else {
            item.setDescription("");
        }

        String type = (String) cbType.getSelectedItem();
        item.setType(type);
        item.setIcon(txtIcon.getText());
        
        // Handle Visual Template hint
        String visualTemplate = txtVisualTemplate.getText();
        if (VISUAL_TEMPLATE_HINT.equals(visualTemplate)) {
            item.setVisualTemplate("");
        } else {
            item.setVisualTemplate(visualTemplate);
        }

        item.setMaterial((String) cbMaterial.getSelectedItem());
        item.setCrystalType((String) cbCrystalType.getSelectedItem());
        
        try {
            item.setWeight(Integer.parseInt(txtWeight.getText()));
        } catch (NumberFormatException e) {
            item.setWeight(0);
        }
        
        try {
            item.setPrice(Long.parseLong(txtPrice.getText()));
        } catch (NumberFormatException e) {
            item.setPrice(0);
        }
        
        // Dynamic Data Gathering
        if ("Weapon".equals(type)) {
            item.setBodyPart((String) cbWeaponBodyPart.getSelectedItem());
            try {
                item.setSoulshotCount(Integer.parseInt(txtSoulshotCount.getText()));
            } catch (NumberFormatException e) { item.setSoulshotCount(0); }
            try {
                item.setSpiritshotCount(Integer.parseInt(txtSpiritshotCount.getText()));
            } catch (NumberFormatException e) { item.setSpiritshotCount(0); }
            try {
                item.setRandomDamage(Integer.parseInt(txtRandomDamage.getText()));
            } catch (NumberFormatException e) { item.setRandomDamage(0); }
            try {
                item.setMpConsume(Integer.parseInt(txtMpConsume.getText()));
            } catch (NumberFormatException e) { item.setMpConsume(0); }
            item.setMagicWeapon(chkIsMagicWeapon.isSelected());
        } else if ("Armor".equals(type)) {
            item.setBodyPart((String) cbArmorBodyPart.getSelectedItem());
            try {
                item.setDamageReduction(Integer.parseInt(txtDamageReduction.getText()));
            } catch (NumberFormatException e) { item.setDamageReduction(0); }
        } else if ("EtcItem".equals(type)) {
            item.setBodyPart("none");
            item.setEtcItemType((String) cbEtcItemType.getSelectedItem());
            item.setStackable(chkStackable.isSelected());
            item.setQuestItem(chkQuestItem.isSelected());
            item.setImmediateEffect(chkImmediateEffect.isSelected());
        }

        Map<String, Double> stats = new HashMap<>();
        for (int i = 0; i < statsModel.getRowCount(); i++) {
            String statName = (String) statsModel.getValueAt(i, 0);
            String statValueStr = (String) statsModel.getValueAt(i, 1);
            if (statName != null && !statName.isEmpty() && statValueStr != null && !statValueStr.isEmpty()) {
                try {
                    stats.put(statName, Double.parseDouble(statValueStr));
                } catch (NumberFormatException e) {
                    // Ignore invalid numbers
                }
            }
        }
        item.setStats(stats);

        return item;
    }
}
