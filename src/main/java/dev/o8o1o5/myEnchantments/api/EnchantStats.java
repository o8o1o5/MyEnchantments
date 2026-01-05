package dev.o8o1o5.myEnchantments.api;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnchantStats {
    private final Map<Attribute, Double> attributeSums = new HashMap<>();
    private final List<Component> loreLines = new ArrayList<>();
    private final Map<Attribute, AttributeModifier> modifiers = new HashMap<>();

    public void addLore(Component line) { this.loreLines.add(line); }

    public void addAttribute(Attribute attr, double amount) {
        attributeSums.put(attr, attributeSums.getOrDefault(attr, 0.0) + amount);
    }

    public List<Component> getLoreLines() { return loreLines; }
    public Map<Attribute, Double> getAttributeSums() { return attributeSums; }
}
