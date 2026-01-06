package dev.o8o1o5.myEnchantments.core;

import dev.o8o1o5.myEnchantments.api.EnchantProvider;
import dev.o8o1o5.myEnchantments.api.EnchantStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class EnchantApplier {

    public static void updateItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> userLore = cleanLore(meta);
        EnchantStats stats = new EnchantStats();

        collectModuleData(meta, stats);
        applyInternalModifiers(meta, stats);
        renderFinalLore(item, meta, stats, userLore);

        item.setItemMeta(meta);
    }

    private static List<Component> cleanLore(ItemMeta meta) {
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        List<String> enchantNames = ModuleRegistry.getAll().stream()
                .map(EnchantProvider::getDisplayName).toList();

        lore.removeIf(line -> {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            return enchantNames.stream().anyMatch(plain::contains);
        });
        return lore;
    }

    private static void collectModuleData(ItemMeta meta, EnchantStats stats) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (EnchantProvider provider : ModuleRegistry.getAll()) {
            NamespacedKey key = new NamespacedKey("myenchantments", "lvl_" + provider.getId());
            if (pdc.has(key, PersistentDataType.INTEGER)) {
                int level = pdc.get(key, PersistentDataType.INTEGER);
                stats.addLore(provider.getFullDisplayName(level));
                provider.provideStats(level, stats);
            }
        }
    }

    private static void applyInternalModifiers(ItemMeta meta, EnchantStats stats) {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        if (meta.hasAttributeModifiers()) {
            meta.getAttributeModifiers().entries().forEach(entry -> {
                if (entry.getValue().getKey().getNamespace().equals("myenchantments")) {
                    meta.removeAttributeModifier(entry.getKey(), entry.getValue());
                }
            });
        }

        stats.getModifiers().forEach(meta::addAttributeModifier);
    }

    private static void renderFinalLore(ItemStack item, ItemMeta meta, EnchantStats stats, List<Component> userLore) {
        List<Component> finalLore = new ArrayList<>(stats.getLoreLines());
        if (!finalLore.isEmpty()) finalLore.add(Component.empty());

        // 바닐라와 동일한 출력 순서 정의
        List<EquipmentSlotGroup> slotsToRender = List.of(
                EquipmentSlotGroup.MAINHAND,
                EquipmentSlotGroup.OFFHAND,
                EquipmentSlotGroup.HEAD,
                EquipmentSlotGroup.CHEST,
                EquipmentSlotGroup.LEGS,
                EquipmentSlotGroup.FEET,
                EquipmentSlotGroup.ARMOR,
                EquipmentSlotGroup.ANY
        );

        for (EquipmentSlotGroup slot : slotsToRender) {
            // 해당 슬롯에 표시할 '기본 속성' 키셋 가져오기
            Set<Attribute> attributesInSlot = new HashSet<>(item.getType().getDefaultAttributeModifiers(slot).keySet());

            // 우리 플러그인이 추가한 '보너스 속성' 키셋 합치기
            attributesInSlot.addAll(stats.getAttributesInSlot(slot));

            if (!attributesInSlot.isEmpty()) {
                renderSlotSection(finalLore, item, slot, stats, attributesInSlot);
            }
        }

        finalLore.addAll(userLore);
        meta.lore(finalLore);
    }

    private static void renderSlotSection(List<Component> lore, ItemStack item, EquipmentSlotGroup slot, EnchantStats stats, Set<Attribute> attributes) {
        lore.add(Component.empty());
        lore.add(translateSlot(slot).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

        for (Attribute attr : attributes) {
            double base = getDefaultValue(item.getType(), attr, slot);
            double bonus = stats.getBonusFor(attr, slot);

            if (base != 0 || bonus != 0) {
                String baseStr = base == 0 ? "" : format(base);
                String bonusStr = bonus == 0 ? "" : " (+" + format(bonus) + ")";

                lore.add(Component.text(" " + baseStr + bonusStr + " " + translateAttribute(attr))
                        .color(NamedTextColor.DARK_GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
    }

    private static String format(double d) {
        // 정수면 소수점을 떼고, 소수면 1자리까지 표시
        return d == (long) d ? String.format("%d", (long) d) : String.format("%.1f", d);
    }

    private static double getDefaultValue(Material type, Attribute attr, EquipmentSlotGroup slot) {
        // 슬롯 그룹에 맞는 기본 속성값 합산
        return type.getDefaultAttributeModifiers(slot).get(attr).stream()
                .mapToDouble(AttributeModifier::getAmount).sum();
    }

    private static Component translateSlot(EquipmentSlotGroup slot) {
        String name = switch (slot.toString().toLowerCase()) {
            case "mainhand" -> "주 손에 들었을 때:";
            case "offhand" -> "보조 손에 들었을 때:";
            case "head" -> "머리에 착용 시:";
            case "chest" -> "몸에 착용 시:";
            case "legs" -> "다리에 착용 시:";
            case "feet" -> "발에 착용 시:";
            case "armor" -> "방어구 착용 시:";
            case "any" -> "장착 시:";
            default -> slot.toString() + " 상태일 때:";
        };
        return Component.text(name);
    }

    private static String translateAttribute(Attribute attr) {
        String key = attr.getKey().getKey();
        return switch (key) {
            case "attack_damage" -> "공격 피해";
            case "attack_speed" -> "공격 속도";
            case "armor" -> "방어력";
            case "armor_toughness" -> "방어 강도";
            case "max_health" -> "최대 체력";
            case "movement_speed" -> "이동 속도";
            case "knockback_resistance" -> "밀치기 저항";
            case "luck" -> "행운";
            default -> key;
        };
    }
}