package dev.o8o1o5.myEnchantments.core;

import dev.o8o1o5.myEnchantments.api.EnchantProvider;
import dev.o8o1o5.myEnchantments.api.EnchantStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class EnchantApplier {

    public static void updateItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 1. 기존 데이터를 백업합니다.
        List<Component> currentLore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        Map<Attribute, Collection<AttributeModifier>> existingModifiers =
                meta.hasAttributeModifiers() ? new HashMap<>(meta.getAttributeModifiers().asMap()) : new HashMap<>();

        EnchantStats newStats = new EnchantStats();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 2. 인챈트들의 displayName을 기반으로 lore 필터링을 진행합니다.
        // I. 현재 등록된 모든 인챈트들의 displayName 목록을 가져옵니다.
        List<String> enchantNames = ModuleRegistry.getAll().stream()
                .map(EnchantProvider::getDisplayName)
                .toList();

        currentLore.removeIf(line -> {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            // II. 로어 한 줄에 등록된 인챈트 이름 중 하나라도 포함되어 있으면 삭제합니다.
            return enchantNames.stream().anyMatch(plain::contains);
        });

        // 3. 강화 모듈의 데이터를 수집합니다. (Enchantments, Stats, AttributeModifier ...)
        for (EnchantProvider provider : ModuleRegistry.getAll()) {
            NamespacedKey key = new NamespacedKey("myenchantments", "lvl_" + provider.getId());
            if (pdc.has(key, PersistentDataType.INTEGER)) {
                int level = pdc.get(key, PersistentDataType.INTEGER);
                newStats.addLore(provider.getFullDisplayName(level));
                provider.provideStats(level, newStats);
            }
        }

        // 4. Merge 로직을 통해 Attribute를 통합합니다.
        meta.getAttributeModifiers().forEach((attr, mod) -> meta.removeAttributeModifier(attr, mod));

        // I. 기존에 있던 순수 아이템의 능력치를 복구합니다.
        existingModifiers.forEach((attr, mods) -> {
            for (AttributeModifier mod : mods) {
                // 우리 플러그인이 붙인 게 아닌 (NamespacedKey가 다른) 것만 복구합니다.
                if (!mod.getKey().getNamespace().equals("myenchantments")) {
                    meta.addAttributeModifier(attr, mod);
                }
            }
        });

        // II. 우리 강화 모듈이 요청한 능력치를 add 합니다.
        newStats.getModifiers().forEach(meta::addAttributeModifier);

        // 5. Lore를 재구성 및 적용합니다.
        List<Component> finalLore = new ArrayList<>(newStats.getLoreLines());
        if (!finalLore.isEmpty()) finalLore.add(Component.empty());
        finalLore.addAll(currentLore);

        meta.lore(finalLore);
        item.setItemMeta(meta);
    }
}
