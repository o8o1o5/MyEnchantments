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
    private final List<Component> loreLines = new ArrayList<>();
    private final Map<Attribute, AttributeModifier> modifiers = new HashMap<>();

    public void addLore(Component line) { this.loreLines.add(line); }

    public void addAttribute(Attribute attr, double amount, AttributeModifier.Operation op, EquipmentSlotGroup slot) {
        NamespacedKey modifierKey = new NamespacedKey("myenchantments", attr.getKey().getKey());
        AttributeModifier modifier = new AttributeModifier(modifierKey, amount, op, slot);
        this.modifiers.put(attr, modifier);
    }

    public List<Component> getLoreLines() { return loreLines; }
    public Map<Attribute, AttributeModifier> getModifiers() { return modifiers; }
}
