package org.l2jmobius.forge;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.L2ClientDat;
import org.l2jmobius.actions.OpenDat;
import org.l2jmobius.actions.SaveDat;
import org.l2jmobius.config.ConfigWindow;
import org.l2jmobius.forms.JPopupTextArea;

public class ItemForgeController {
    private static final Logger LOGGER = Logger.getLogger(ItemForgeController.class.getName());
    private L2ClientDat _mainFrame;

    public ItemForgeController() {
    }
    
    public void setMainFrame(L2ClientDat frame) {
        _mainFrame = frame;
    }

    public void createItem(ForgeItem item) {
        LOGGER.info("Starting item creation for ID: " + item.getId() + " Name: " + item.getName());
        
        String serverXml = generateServerXml(item);
        System.out.println("Generated Server XML:\n" + serverXml);
        
        updateClientFiles(item);
    }

    /**
     * Generates the Server-Side XML snippet compatible with L2J_Mobius High Five.
     * Based on items.xsd structure.
     */
    public String generateServerXml(ForgeItem item) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(String.format("\t<item id=\"%d\" type=\"%s\" name=\"%s\">\n", item.getId(), item.getType(), item.getName()));

        // Core Sets
        appendSet(sb, "icon", item.getIcon());
        appendSet(sb, "bodypart", item.getBodyPart());
        appendSet(sb, "material", item.getMaterial());
        appendSet(sb, "crystal_type", item.getCrystalType());
        appendSet(sb, "weight", String.valueOf(item.getWeight()));
        appendSet(sb, "price", String.valueOf(item.getPrice()));

        // Weapon Specifics (Shots)
        if (item.getType() != null && item.getType().equalsIgnoreCase("Weapon")) {
            appendSet(sb, "soulshots", String.valueOf(item.getSoulshotCount()));
            appendSet(sb, "spiritshots", String.valueOf(item.getSpiritshotCount()));
            if (item.isMagicWeapon()) {
                appendSet(sb, "is_magic_weapon", "true");
            }
            if (item.getMpConsume() > 0) {
                appendSet(sb, "mp_consume", String.valueOf(item.getMpConsume()));
            }
        } else if (item.getType() != null && item.getType().equalsIgnoreCase("EtcItem")) {
            if (item.getEtcItemType() != null) {
                appendSet(sb, "etcitem_type", item.getEtcItemType());
            }
            if (item.isStackable()) {
                appendSet(sb, "is_stackable", "true");
            }
            if (item.isQuestItem()) {
                appendSet(sb, "is_questitem", "true");
            }
        }

        // Stats Block
        if (!item.getStats().isEmpty() && !item.getType().equalsIgnoreCase("EtcItem")) {
            sb.append("\t\t<stats>\n");
            for (Map.Entry<String, Double> entry : item.getStats().entrySet()) {
                sb.append(String.format("\t\t\t<set stat=\"%s\" val=\"%s\" />\n", entry.getKey(), String.valueOf(entry.getValue())));
            }
            sb.append("\t\t</stats>\n");
        }

        // Footer
        sb.append("\t</item>");
        return sb.toString();
    }

    private void appendSet(StringBuilder sb, String name, String val) {
        if (val != null && !val.isEmpty() && !val.equals("NONE")) {
            sb.append(String.format("\t\t<set name=\"%s\" val=\"%s\" />\n", name, val));
        }
    }

    /**
     * Updates the client DAT files with the new item.
     * Handles background loading if the file is not currently open.
     */
    public void updateClientFiles(ForgeItem item) {
        // 1. Get Context (System Folder)
        String currentFilePath = ConfigWindow.LAST_FILE_SELECTED;
        if (currentFilePath == null || currentFilePath.isEmpty()) {
            LOGGER.severe("No file selected context. Please open a DAT file first to establish the system folder.");
            return;
        }
        File systemFolder = new File(currentFilePath).getParentFile();
        if (systemFolder == null || !systemFolder.exists()) {
            LOGGER.severe("Invalid system folder derived from: " + currentFilePath);
            return;
        }

        // 2. Identify Targets
        String itemNameFileName = "itemname-e.dat";
        String groupFileName = getGroupFileName(item.getType());

        // 3. Process ItemName
        processDatFile(systemFolder, itemNameFileName, item, true);

        // 4. Process Group File (Visuals)
        processDatFile(systemFolder, groupFileName, item, false);
    }

    private void processDatFile(File systemFolder, String fileName, ForgeItem item, boolean isItemName) {
        // Check if this file is currently open in the main editor
        boolean isOpen = false;
        if (ConfigWindow.LAST_FILE_SELECTED != null) {
            File openFile = new File(ConfigWindow.LAST_FILE_SELECTED);
            if (openFile.getName().equalsIgnoreCase(fileName)) {
                isOpen = true;
            }
        }

        if (isOpen) {
            LOGGER.info("File " + fileName + " is currently open. Editing in memory.");
            backgroundLoadAndEdit(systemFolder, fileName, item, isItemName);
        } else {
            LOGGER.info("File " + fileName + " is not open. Performing background load.");
            backgroundLoadAndEdit(systemFolder, fileName, item, isItemName);
        }
    }

    private void backgroundLoadAndEdit(File systemFolder, String fileName, ForgeItem item, boolean isItemName) {
        File txtFile = new File(systemFolder, fileName.replace(".dat", ".txt"));
        
        // Auto-unpack logic
        if (!txtFile.exists()) {
            File datFile = new File(systemFolder, fileName);
            if (datFile.exists()) {
                LOGGER.info("TXT file missing. Attempting to unpack " + fileName + "...");
                try {
                    // Create a dummy ActionTask that overrides methods to avoid NPEs
                    org.l2jmobius.actions.ActionTask dummyTask = new org.l2jmobius.actions.ActionTask(null) {
                        @Override
                        public Void doInBackground() { return null; }
                        @Override
                        protected void action() {}
                        @Override
                        public void done() {} // Override to avoid calling _l2clientdat.onStopTask()
                        @Override
                        public void propertyChange(java.beans.PropertyChangeEvent evt) {} // Override to avoid calling _l2clientdat.onProgressTask()
                    };
                    
                    String unpackedText = OpenDat.start(dummyTask, 100.0, ConfigWindow.CURRENT_CHRONICLE, datFile, true); // mass=true to avoid UI logs
                    if (unpackedText != null && !unpackedText.isEmpty()) {
                        Files.write(txtFile.toPath(), unpackedText.getBytes(StandardCharsets.UTF_8));
                        LOGGER.info("Successfully unpacked " + fileName);
                    } else {
                        LOGGER.severe("Failed to unpack " + fileName);
                        return;
                    }
                    
                } catch (Exception e) {
                    LOGGER.severe("Error auto-unpacking " + fileName + ": " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            } else {
                LOGGER.warning("Neither TXT nor DAT file found for: " + fileName);
                return;
            }
        }
        
        try {
            List<String> lines = Files.readAllLines(txtFile.toPath(), StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();
            
            if (isItemName) {
                // Update itemname-e.txt with strict formatting
                String format = "item_name_begin\tid=%d\tname=[%s]\tadditionalname=[%s]\tdescription=[%s]\tpopup=-1\tsupercnt0=0\tsetid_1={}\tset_bonus_desc=[]\tsupercnt1=0\tset_extra_id={}\tset_extra_desc=[]\tunknown={0;0;0;0;0;0;0;0;0}\tspecial_enchant_amount=0\tspecial_enchant_desc=[]\tcolor=1\titem_name_end";
                String newItemLine = String.format(format, 
                        item.getId(), 
                        item.getName(), 
                        item.getAdditionalName() != null ? item.getAdditionalName() : "", 
                        item.getDescription() != null ? item.getDescription() : "");
                
                for (String line : lines) {
                    // Check Condition: if (line.contains("id=" + item.getId() + "\t"))
                    if (line.contains("id=" + item.getId() + "\t")) {
                        continue; // Skip existing line to replace it
                    }
                    newLines.add(line);
                }
                newLines.add(newItemLine);
                
            } else {
                // Update group file
                String templateId = item.getVisualTemplate();
                String templateLine = null;
                
                // 1. Convert the target ID to a clean String
                String searchId = "object_id=" + String.valueOf(templateId).trim();
                System.out.println("DEBUG: Looking for strict match: '" + searchId + "' at Index 2");
                int rowIndex = 0;

                for (String line : lines) {
                    if (line == null || line.trim().isEmpty()) {
                        rowIndex++;
                        continue;
                    }

                    String[] tokens = line.split("\t");
                    
                    // 2. Check Index 2
                    if (tokens.length > 2) {
                        String cellString = tokens[2].trim();
                        
                        // 3. Debug the first row
                        if (rowIndex == 0) {
                            System.out.println("FULL ROW DUMP: " + line);
                            for (int i = 0; i < tokens.length; i++) {
                                System.out.println("Index " + i + ": " + tokens[i]);
                            }
                        }

                        // 4. Compare Strings
                        if (cellString.equals(searchId)) {
                            templateLine = line;
                            System.out.println("DEBUG: MATCH FOUND at row " + rowIndex);
                            break;
                        }
                    }
                    rowIndex++;
                }
                
                if (templateLine != null) {
                    String[] tokens = templateLine.split("\t");
                    if (tokens.length > 18) {
                        // 2. Cloning Logic (Setting the new ID)
                        tokens[2] = "object_id=" + item.getId();
                        
                        // 3. Icon Logic (Updating Index 18)
                        if (item.getIcon() != null && !item.getIcon().isEmpty()) {
                            String oldIconVal = tokens[18];
                            String newIconVal = oldIconVal.replaceFirst("icon\\.[\\w_]+", item.getIcon());
                            tokens[18] = newIconVal;
                        }
                        
                        String newLine = String.join("\t", tokens);
                        
                        // Append
                        for (String line : lines) {
                             // Check if ID exists to avoid duplicates (using the new ID format)
                             String[] lineTokens = line.split("\t");
                             if (lineTokens.length > 2 && lineTokens[2].trim().equals("object_id=" + item.getId())) {
                                 continue;
                             }
                             newLines.add(line);
                        }
                        newLines.add(newLine);
                    } else {
                        LOGGER.severe("Template line found but has insufficient columns (" + tokens.length + "). Expected > 18.");
                        newLines.addAll(lines);
                    }
                } else {
                    LOGGER.severe("Template ID " + templateId + " not found, aborting save.");
                    return;
                }
            }
            
            Files.write(txtFile.toPath(), newLines, StandardCharsets.UTF_8);
            LOGGER.info("Updated " + txtFile.getName());
            
            // Auto-pack (Encrypt)
            File datFile = new File(systemFolder, fileName);
            if (_mainFrame != null) {
                SaveDat task = new SaveDat(_mainFrame, datFile, ConfigWindow.CURRENT_CHRONICLE);
                task.doInBackground(); // Execute synchronously
                LOGGER.info("Compiled " + fileName);
            }
            
        } catch (Exception e) {
            LOGGER.severe("Error processing file " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getGroupFileName(String type) {
        if (type == null) return "etcitemgrp.dat";
        switch (type.toLowerCase()) {
            case "weapon": return "weapongrp.dat";
            case "armor": return "armorgrp.dat";
            default: return "etcitemgrp.dat";
        }
    }
}
