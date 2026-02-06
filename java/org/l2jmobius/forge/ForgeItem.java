package org.l2jmobius.forge;

import java.util.HashMap;
import java.util.Map;

public class ForgeItem {
    private int id;
    private String name;
    private String additionalName;
    private String description = "";
    private String type;
    private String icon;
    private String iconPanel; // Added
    private String visualTemplate;

    // Server Specs
    private String bodyPart;
    private String material;
    private String crystalType;
    private int weight;
    private long price;

    // Weapon Specs
    private int soulshotCount;
    private int spiritshotCount;
    private int randomDamage;
    private boolean isMagicWeapon;
    private int mpConsume;

    // Armor Specs
    private int damageReduction;

    // EtcItem Specs
    private boolean isStackable;
    private boolean isQuestItem;
    private boolean isImmediateEffect;
    private String etcItemType;

    private Map<String, Double> stats = new HashMap<>();

    public ForgeItem() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAdditionalName() { return additionalName; }
    public void setAdditionalName(String additionalName) { this.additionalName = additionalName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getIconPanel() { return iconPanel; }
    public void setIconPanel(String iconPanel) { this.iconPanel = iconPanel; }

    public String getVisualTemplate() { return visualTemplate; }
    public void setVisualTemplate(String visualTemplate) { this.visualTemplate = visualTemplate; }

    public String getBodyPart() { return bodyPart; }
    public void setBodyPart(String bodyPart) { this.bodyPart = bodyPart; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getCrystalType() { return crystalType; }
    public void setCrystalType(String crystalType) { this.crystalType = crystalType; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    public int getSoulshotCount() { return soulshotCount; }
    public void setSoulshotCount(int soulshotCount) { this.soulshotCount = soulshotCount; }

    public int getSpiritshotCount() { return spiritshotCount; }
    public void setSpiritshotCount(int spiritshotCount) { this.spiritshotCount = spiritshotCount; }

    public int getRandomDamage() { return randomDamage; }
    public void setRandomDamage(int randomDamage) { this.randomDamage = randomDamage; }

    public boolean isMagicWeapon() { return isMagicWeapon; }
    public void setMagicWeapon(boolean magicWeapon) { isMagicWeapon = magicWeapon; }

    public int getMpConsume() { return mpConsume; }
    public void setMpConsume(int mpConsume) { this.mpConsume = mpConsume; }

    public int getDamageReduction() { return damageReduction; }
    public void setDamageReduction(int damageReduction) { this.damageReduction = damageReduction; }

    public boolean isStackable() { return isStackable; }
    public void setStackable(boolean stackable) { isStackable = stackable; }

    public boolean isQuestItem() { return isQuestItem; }
    public void setQuestItem(boolean questItem) { isQuestItem = questItem; }

    public boolean isImmediateEffect() { return isImmediateEffect; }
    public void setImmediateEffect(boolean immediateEffect) { isImmediateEffect = immediateEffect; }

    public String getEtcItemType() { return etcItemType; }
    public void setEtcItemType(String etcItemType) { this.etcItemType = etcItemType; }

    public Map<String, Double> getStats() { return stats; }
    public void setStats(Map<String, Double> stats) { this.stats = stats; }
    public void addStat(String stat, Double value) { this.stats.put(stat, value); }
}