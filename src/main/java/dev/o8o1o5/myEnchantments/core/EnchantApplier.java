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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EnchantApplier {

    public static void updateItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 1. 기초 데이터 준비
        List<Component> currentLore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        EnchantStats newStats = new EnchantStats();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 2. 기존 인챈트 로어 제거 (작성하신 로직 유지)
        List<String> enchantNames = ModuleRegistry.getAll().stream()
                .map(EnchantProvider::getDisplayName).toList();
        currentLore.removeIf(line -> {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            return enchantNames.stream().anyMatch(plain::contains);
        });

        // 3. 모듈 데이터 수집
        for (EnchantProvider provider : ModuleRegistry.getAll()) {
            NamespacedKey key = new NamespacedKey("myenchantments", "lvl_" + provider.getId());
            if (pdc.has(key, PersistentDataType.INTEGER)) {
                int level = pdc.get(key, PersistentDataType.INTEGER);
                newStats.addLore(provider.getFullDisplayName(level));
                provider.provideStats(level, newStats);
            }
        }

        // 4. Attribute 관리 (Merge & Hide)
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // 바닐라 툴팁 숨김

        // 기존 우리 플러그인 modifier만 제거 (작성하신 복구 로직의 고도화)
        if (meta.hasAttributeModifiers()) {
            meta.getAttributeModifiers().entries().forEach(entry -> {
                if (entry.getValue().getKey().getNamespace().equals("myenchantments")) {
                    meta.removeAttributeModifier(entry.getKey(), entry.getValue());
                }
            });
        }

        // 우리 보너스 스탯 실제 적용
        newStats.getAttributeSums().forEach((attr, amount) -> {
            NamespacedKey key = new NamespacedKey("myenchantments", "bonus_" + attr.getKey().getKey());
            meta.addAttributeModifier(attr, new AttributeModifier(key, amount,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        });

        // 5. 로어 재구성 (확장형 렌더링)
        List<Component> finalLore = new ArrayList<>(newStats.getLoreLines());
        if (!finalLore.isEmpty()) finalLore.add(Component.empty());

        // [핵심] 바닐라 모방 섹션 생성
        finalLore.add(Component.text("주로 사용하는 손에 들었을 때:").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

        // 모든 속성(바닐라 + 커스텀)을 통합 렌더링
        newStats.getAttributeSums().forEach((attr, bonus) -> {
            double base = getDefaultValue(item.getType(), attr);

            // 7 (+2) 형식 생성
            String statValue = (base > 0 ? base : "") + (bonus > 0 ? " (+" + bonus + ")" : "");
            if (!statValue.isEmpty()) {
                finalLore.add(Component.text(" " + statValue + " " + translateAttribute(attr))
                        .color(NamedTextColor.DARK_GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            }
        });

        finalLore.addAll(currentLore);
        meta.lore(finalLore);
        item.setItemMeta(meta);
    }

    // 아이템 타입별 기본 속성 수치를 가져오는 유틸리티
    private static double getDefaultValue(Material type, Attribute attr) {
        return type.getDefaultAttributeModifiers(EquipmentSlot.HAND).get(attr).stream()
                .mapToDouble(AttributeModifier::getAmount).sum();
    }

    // 속성 키를 한글로 변환 (확장 가능)
    private static String translateAttribute(Attribute attr) {
        String key = attr.getKey().getKey();
        return switch (key) {
            case "attack_damage" -> "공격 피해";
            case "attack_speed" -> "공격 속도";
            case "max_health" -> "최대 체력";
            case "movement_speed" -> "이동 속도";
            default -> key; // 등록되지 않은 커스텀 속성은 키 이름 그대로 출력
        };
    }
}