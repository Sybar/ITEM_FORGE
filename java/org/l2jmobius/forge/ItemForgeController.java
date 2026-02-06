package org.l2jmobius.forge;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.L2ClientDat;
import org.l2jmobius.actions.OpenDat;
import org.l2jmobius.actions.SaveDat;
import org.l2jmobius.config.ConfigWindow;

public class ItemForgeController {
    private static final Logger LOGGER = Logger.getLogger(ItemForgeController.class.getName());
    private L2ClientDat _mainFrame;

    public void setMainFrame(L2ClientDat frame) {
        _mainFrame = frame;
    }

    // =============================================================================================
    //  XML GENERATION
    // =============================================================================================

    public String generateServerXml(ForgeItem item) {
        StringBuilder sb = new StringBuilder();
        String type = item.getType();

        sb.append(String.format("<item id=\"%d\" name=\"%s\" type=\"%s\">\n",
                item.getId(),
                item.getName().replace("\"", "'"),
                type));

        appendSet(sb, "icon", item.getIcon());
        // icon_panel is excluded from XML
        appendSet(sb, "price", String.valueOf(item.getPrice()));
        appendSet(sb, "weight", String.valueOf(item.getWeight()));

        if (item.getMaterial() != null && !item.getMaterial().isEmpty()) {
            appendSet(sb, "material", item.getMaterial().toUpperCase());
        }
        if (item.getCrystalType() != null && !item.getCrystalType().equalsIgnoreCase("none")) {
            appendSet(sb, "crystal_type", item.getCrystalType().toUpperCase());
        }

        if ("Weapon".equalsIgnoreCase(type)) {
            appendSet(sb, "bodypart", "rhand");
            appendSet(sb, "soulshots", String.valueOf(item.getSoulshotCount()));
            appendSet(sb, "spiritshots", String.valueOf(item.getSpiritshotCount()));
            appendSet(sb, "mp_consume", String.valueOf(item.getMpConsume()));

            sb.append("\t<set name=\"weapon_type\" val=\"SWORD\" />\n");
            sb.append("\t<stats>\n");
            sb.append("\t\t<set stat=\"pAtk\" val=\"10\" />\n");
            sb.append("\t\t<set stat=\"mAtk\" val=\"10\" />\n");
            sb.append("\t\t<set stat=\"rCrit\" val=\"4\" />\n");
            sb.append("\t\t<set stat=\"pAtkSpd\" val=\"300\" />\n");
            sb.append("\t</stats>\n");

        } else if ("Armor".equalsIgnoreCase(type)) {
            appendSet(sb, "bodypart", item.getBodyPart());
            sb.append("\t<stats>\n");
            sb.append("\t\t<set stat=\"pDef\" val=\"10\" />\n");
            sb.append("\t</stats>\n");
        } else {
            appendSet(sb, "is_stackable", "true");
            appendSet(sb, "handler", "ItemSkills");
        }

        sb.append("</item>");
        return sb.toString();
    }

    private void appendSet(StringBuilder sb, String name, String val) {
        if (val != null && !val.isEmpty() && !val.equals("0")) {
            sb.append(String.format("\t<set name=\"%s\" val=\"%s\" />\n", name, val));
        }
    }

    // =============================================================================================
    //  LOAD LOGIC
    // =============================================================================================

    public ForgeItem loadItem(int id) {
        if (_mainFrame == null) return null;
        String currentFilePath = ConfigWindow.LAST_FILE_SELECTED;
        if (currentFilePath == null) return null;
        File systemFolder = new File(currentFilePath).getParentFile();

        ForgeItem item = new ForgeItem();
        item.setId(id);

        if (!loadItemNameData(systemFolder, item)) return null;

        if (!loadGroupData(systemFolder, "weapongrp.dat", item, "Weapon")) {
            if (!loadGroupData(systemFolder, "armorgrp.dat", item, "Armor")) {
                loadGroupData(systemFolder, "etcitemgrp.dat", item, "EtcItem");
            }
        }
        return item;
    }

    private boolean loadItemNameData(File systemFolder, ForgeItem item) {
        List<String> lines = readDatFile(systemFolder, "itemname-e.dat");
        if (lines == null) return false;
        String searchId = "id=" + item.getId() + "\t";
        for (String line : lines) {
            if (line.contains(searchId)) {
                item.setName(extractTagValue(line, "name"));
                item.setAdditionalName(extractTagValue(line, "additionalname"));
                item.setDescription(extractTagValue(line, "description"));
                return true;
            }
        }
        return false;
    }

    private boolean loadGroupData(File systemFolder, String fileName, ForgeItem item, String type) {
        List<String> lines = readDatFile(systemFolder, fileName);
        if (lines == null) return false;
        String searchId = "object_id=" + item.getId();
        for (String line : lines) {
            String[] tokens = line.split("\t");
            if (tokens.length > 2 && getValue(tokens[2]).trim().equals(String.valueOf(item.getId()))) {
                item.setType(type);
                item.setVisualTemplate(String.valueOf(item.getId()));

                // Load Icon Panel
                for(String t : tokens) {
                    if (t.contains("icon_panel")) {
                        String val = cleanStr(t);
                        if (!val.equals("icon_panel")) item.setIconPanel(val);
                    }
                }

                if (type.equals("Weapon")) parseWeaponStats(tokens, item);
                else if (type.equals("Armor")) parseArmorStats(tokens, item);
                else parseEtcItemStats(tokens, item);
                return true;
            }
        }
        return false;
    }

    private void parseWeaponStats(String[] tokens, ForgeItem item) {
        if (tokens.length > 18) item.setIcon(cleanStr(tokens[18]));
        if (tokens.length > 20) item.setWeight(safeParseInt(tokens[20]));
        if (tokens.length > 21) item.setMaterial(getMaterialName(safeParseInt(tokens[21])));
        if (tokens.length > 23) item.setCrystalType(getCrystalName(safeParseInt(tokens[23])));
        if (tokens.length > 32) item.setSoulshotCount(safeParseInt(tokens[32]));
        if (tokens.length > 33) item.setSpiritshotCount(safeParseInt(tokens[33]));
        if (tokens.length > 44) item.setMpConsume(safeParseInt(tokens[44]));
        if (tokens.length > 4) item.setBodyPart("rhand");
    }

    private void parseArmorStats(String[] tokens, ForgeItem item) {
        if (tokens.length > 4) item.setBodyPart(getBodyPartName(safeParseInt(tokens[4])));
        if (tokens.length > 16) item.setWeight(safeParseInt(tokens[16]));
        if (tokens.length > 17) item.setMaterial(getMaterialName(safeParseInt(tokens[17])));
        if (tokens.length > 18) item.setCrystalType(getCrystalName(safeParseInt(tokens[18])));
        for(String t : tokens) {
            if (t.contains("icon")) {
                String clean = cleanStr(t);
                if(!clean.isEmpty() && !clean.equals("icon") && !t.contains("icon_panel")) { item.setIcon(clean); break; }
            }
        }
    }

    private void parseEtcItemStats(String[] tokens, ForgeItem item) {
        if (tokens.length > 20) item.setWeight(safeParseInt(tokens[20]));
        if (tokens.length > 21) item.setMaterial(getMaterialName(safeParseInt(tokens[21])));
        if (tokens.length > 33) item.setCrystalType(getCrystalName(safeParseInt(tokens[33])));

        for(String t : tokens) {
            if (t.contains("icon")) {
                String clean = cleanStr(t);
                if(!clean.isEmpty() && !clean.equals("icon") && !t.contains("icon_panel")) { item.setIcon(clean); break; }
            }
        }
    }

    private String cleanStr(String val) {
        if (val == null) return "";
        val = val.replace("icon=", "").replace("icon_panel=", "");
        val = val.replaceAll("[\\[\\]\\{\\}]", "");
        if (val.contains(";")) {
            val = val.split(";")[0];
        }
        return val.trim();
    }

    // --- CREATE / UPDATE FILES ---

    public void createItem(ForgeItem item) {
        if (_mainFrame == null) return;
        updateClientFiles(item);
    }

    public void updateClientFiles(ForgeItem item) {
        String currentFilePath = ConfigWindow.LAST_FILE_SELECTED;
        if (currentFilePath == null) return;
        File systemFolder = new File(currentFilePath).getParentFile();

        boolean successName = processDatFile(systemFolder, "itemname-e.dat", item, true);
        if (!successName) return;

        String groupFileName = getGroupFileName(item.getType());
        boolean successGroup;

        if ("Weapon".equalsIgnoreCase(item.getType()) || "Armor".equalsIgnoreCase(item.getType())) {
            successGroup = exportTxtOnly(systemFolder, groupFileName, item);
            if (successGroup) showMessage("Saved itemname-e.dat (Auto)\nExported " + groupFileName.replace(".dat", ".txt") + " (Manual Compile)");
        } else {
            successGroup = processDatFile(systemFolder, groupFileName, item, false);
            if (successGroup) showMessage("Saved itemname-e.dat and " + groupFileName + " successfully.");
        }
    }

    // --- SMART READ LOGIC ---
    private List<String> readDatFile(File systemFolder, String fileName) {
        // 1. Initialize DAT Context (Critical for SaveDat)
        File datFile = new File(systemFolder, fileName);
        List<String> datLines = new ArrayList<>();
        try {
            org.l2jmobius.actions.ActionTask dummyTask = new org.l2jmobius.actions.ActionTask(null) {
                @Override public Void doInBackground() { return null; }
                @Override protected void action() {}
                @Override public void done() {}
                @Override public void propertyChange(java.beans.PropertyChangeEvent evt) {}
            };
            String text = OpenDat.start(dummyTask, 100.0, ConfigWindow.CURRENT_CHRONICLE, datFile, true);
            if (text != null) {
                for(String s : text.split("\r\n|\n")) datLines.add(s);
            }
        } catch (Exception e) {
            LOGGER.warning("Error initializing DAT context: " + e.getMessage());
        }

        // 2. Use TXT if available
        File txtFile = new File(systemFolder, fileName.replace(".dat", ".txt"));
        if (txtFile.exists()) {
            try {
                LOGGER.info("Reading from working file: " + txtFile.getName());
                return Files.readAllLines(txtFile.toPath(), StandardCharsets.UTF_8);
            } catch (Exception e) {}
        }

        return datLines.isEmpty() ? null : datLines;
    }

    private boolean processDatFile(File systemFolder, String fileName, ForgeItem item, boolean isItemName) {
        List<String> lines = readDatFile(systemFolder, fileName);
        if (lines == null) return false;
        List<String> newLines = generateUpdatedContent(lines, item, isItemName);

        try {
            File txtFile = new File(systemFolder, fileName.replace(".dat", ".txt"));
            Files.write(txtFile.toPath(), newLines, StandardCharsets.UTF_8);
        } catch (Exception e) {}

        File datFile = new File(systemFolder, fileName);
        ConfigWindow.LAST_FILE_SELECTED = datFile.getAbsolutePath();
        _mainFrame.setEditorText(String.join("\r\n", newLines));
        new SaveDat(_mainFrame, datFile, ConfigWindow.CURRENT_CHRONICLE).doInBackground();
        return true;
    }

    private boolean exportTxtOnly(File systemFolder, String fileName, ForgeItem item) {
        List<String> lines = readDatFile(systemFolder, fileName);
        if (lines == null) return false;
        List<String> newLines = generateUpdatedContent(lines, item, false);
        File txtFile = new File(systemFolder, fileName.replace(".dat", ".txt"));
        try {
            Files.write(txtFile.toPath(), newLines, StandardCharsets.UTF_8);
            LOGGER.info("Exported " + txtFile.getName());
            return true;
        } catch (Exception e) { return false; }
    }

    // --- UPDATED GENERATOR (WITH IN-PLACE REPLACEMENT) ---
    private List<String> generateUpdatedContent(List<String> lines, ForgeItem item, boolean isItemName) {
        List<String> newLines = new ArrayList<>();

        if (isItemName) {
            String format = "item_name_begin\tid=%d\tname=[%s]\tadditionalname=[%s]\tdescription=[%s]\tpopup=-1\tsupercnt0=0\tsetid_1={}\tset_bonus_desc=[]\tsupercnt1=0\tset_extra_id={}\tset_extra_desc=[]\tunknown={0;0;0;0;0;0;0;0;0}\tspecial_enchant_amount=0\tspecial_enchant_desc=[]\tcolor=1\titem_name_end";
            String newItemLine = String.format(format, item.getId(), item.getName(), item.getAdditionalName() != null ? item.getAdditionalName() : "", item.getDescription() != null ? item.getDescription() : "");

            boolean replaced = false;
            String searchId = "id=" + item.getId() + "\t";
            for (String line : lines) {
                if (line.contains(searchId)) {
                    newLines.add(newItemLine); replaced = true;
                } else newLines.add(line);
            }
            if (!replaced) newLines.add(newItemLine);

        } else {
            String templateId = item.getVisualTemplate();
            String templateLine = null;
            String searchId = String.valueOf(templateId).trim();

            for (String line : lines) {
                String[] t = line.split("\t");
                if (t.length > 2 && getValue(t[2]).trim().equals(searchId)) {
                    templateLine = line; break;
                }
            }

            if (templateLine != null) {
                String[] tokens = templateLine.split("\t", -1);
                if (tokens.length > 0 && tokens[tokens.length - 1].isEmpty()) {
                    String[] t = new String[tokens.length-1]; System.arraycopy(tokens,0,t,0,tokens.length-1); tokens=t;
                }

                if (tokens.length > 18) {
                    tokens[2] = updateToken(tokens[2], String.valueOf(item.getId()));

                    if (item.getIcon() != null && !item.getIcon().isEmpty()) {
                        tokens[18] = tokens[18].replaceFirst("icon\\.[\\w_]+", item.getIcon());
                    }

                    if (item.getIconPanel() != null && !item.getIconPanel().isEmpty()) {
                        for(int i=0; i<tokens.length; i++) {
                            if (tokens[i].contains("icon_panel")) {
                                tokens[i] = "icon_panel=[" + item.getIconPanel() + "]";
                                break;
                            }
                        }
                    }

                    String type = item.getType();
                    if ("Weapon".equalsIgnoreCase(type)) {
                        if (tokens.length > 20) tokens[20] = updateToken(tokens[20], String.valueOf(item.getWeight()));
                        if (tokens.length > 21) tokens[21] = updateToken(tokens[21], String.valueOf(getMaterialId(item.getMaterial())));
                        if (tokens.length > 23) tokens[23] = updateToken(tokens[23], String.valueOf(getCrystalId(item.getCrystalType())));
                        if (tokens.length > 32) tokens[32] = updateToken(tokens[32], String.valueOf(item.getSoulshotCount()));
                        if (tokens.length > 33) tokens[33] = updateToken(tokens[33], String.valueOf(item.getSpiritshotCount()));
                        if (tokens.length > 44) tokens[44] = updateToken(tokens[44], String.valueOf(item.getMpConsume()));
                    } else if ("Armor".equalsIgnoreCase(type)) {
                        if (tokens.length > 4) tokens[4] = updateToken(tokens[4], String.valueOf(getBodyPartId(item.getBodyPart())));
                        if (tokens.length > 16) tokens[16] = updateToken(tokens[16], String.valueOf(item.getWeight()));
                        if (tokens.length > 17) tokens[17] = updateToken(tokens[17], String.valueOf(getMaterialId(item.getMaterial())));
                        if (tokens.length > 18) tokens[18] = updateToken(tokens[18], String.valueOf(getCrystalId(item.getCrystalType())));
                    } else {
                        if (tokens.length > 20) tokens[20] = updateToken(tokens[20], String.valueOf(item.getWeight()));
                        if (tokens.length > 21) tokens[21] = updateToken(tokens[21], String.valueOf(getMaterialId(item.getMaterial())));
                        if (tokens.length > 33) tokens[33] = updateToken(tokens[33], String.valueOf(getCrystalId(item.getCrystalType())));
                    }
                    String newLine = String.join("\t", tokens);

                    // --- REPLACEMENT LOGIC (RESTORED) ---
                    boolean replaced = false;
                    String targetId = String.valueOf(item.getId());

                    for (String line : lines) {
                        String[] t = line.split("\t");
                        if (t.length > 2 && getValue(t[2]).trim().equals(targetId)) {
                            newLines.add(newLine); replaced = true;
                        } else newLines.add(line);
                    }
                    if (!replaced) newLines.add(newLine);

                } else newLines.addAll(lines);
            } else newLines.addAll(lines);
        }
        return newLines;
    }

    // --- HELPERS ---
    private String getValue(String token) {
        if (token.contains("=")) return token.split("=")[1];
        return token;
    }

    private String updateToken(String oldToken, String newValue) {
        if (oldToken.contains("=")) {
            String key = oldToken.split("=")[0];
            return key + "=" + newValue;
        }
        return newValue;
    }

    private void showMessage(String msg) { javax.swing.JOptionPane.showMessageDialog(_mainFrame, msg, "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE); }
    private int safeParseInt(String val) { try { return Integer.parseInt(getValue(val).trim()); } catch (Exception e) { return 0; } }
    private String extractTagValue(String line, String tag) { Pattern p = Pattern.compile(tag + "=\\[(.*?)\\]"); Matcher m = p.matcher(line); return m.find() ? m.group(1) : ""; }

    private int getMaterialId(String name) { String[] m = {"steel","fine_steel","blood_steel","bronze","silver","gold","mithril","ori_harukon","paper","wood","cloth","leather","bone","horn","damascus","adamanta","chrysolite","crystal","liquid","scale_of_dragon","dyestuff","cobweb","seed"}; for(int i=0;i<m.length;i++) if(m[i].equalsIgnoreCase(name)) return i; return 0; }
    private int getCrystalId(String name) { String[] c = {"none","d","c","b","a","s","s80","s84"}; for(int i=0;i<c.length;i++) if(c[i].equalsIgnoreCase(name)) return i; return 0; }
    private int getBodyPartId(String name) { String[] b = {"underwear","rear;lear","head","hair","neck","gloves","chest","legs","feet","back","lrhand","onepiece","rhand"}; for(int i=0;i<b.length;i++) if(b[i].equalsIgnoreCase(name)) return i; return 0; }

    private String getMaterialName(int id) { String[] m = {"steel","fine_steel","blood_steel","bronze","silver","gold","mithril","ori_harukon","paper","wood","cloth","leather","bone","horn","damascus","adamanta","chrysolite","crystal","liquid","scale_of_dragon","dyestuff","cobweb","seed"}; return (id>=0 && id<m.length) ? m[id] : "steel"; }
    private String getCrystalName(int id) { String[] c = {"none","d","c","b","a","s","s80","s84"}; return (id>=0 && id<c.length) ? c[id] : "none"; }
    private String getBodyPartName(int id) { String[] b = {"underwear","rear;lear","head","hair","neck","gloves","chest","legs","feet","back","lrhand","onepiece","rhand"}; return (id>=0 && id<b.length) ? b[id] : "none"; }

    private String getGroupFileName(String type) { if (type == null) return "etcitemgrp.dat"; switch (type.toLowerCase()) { case "weapon": return "weapongrp.dat"; case "armor": return "armorgrp.dat"; default: return "etcitemgrp.dat"; } }
}