package dev.o8o1o5.myEnchantments.api;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.*;

public class EnchantStats {

    // 슬롯별 -> 속성별 -> 보너스 수치 합계
    private final Map<EquipmentSlotGroup, Map<Attribute, Double>> attributeMap = new HashMap<>();
    private final List<Component> loreLines = new ArrayList<>();

    /**
     * 특정 슬롯에 속성 보너스를 추가합니다.
     */
    public void addAttribute(Attribute attr, double amount, EquipmentSlotGroup slot) {
        attributeMap.computeIfAbsent(slot, k -> new HashMap<>())
                .merge(attr, amount, Double::sum);
    }

    /**
     * 인챈트 설명 로어를 추가합니다.
     */
    public void addLore(Component lore) {
        this.loreLines.add(lore);
    }

    public List<Component> getLoreLines() {
        return loreLines;
    }

    /**
     * 특정 슬롯에 등록된 모든 속성 키셋을 반환합니다. (Applier의 최적화 루프용)
     */
    public Set<Attribute> getAttributesInSlot(EquipmentSlotGroup slot) {
        return attributeMap.getOrDefault(slot, Collections.emptyMap()).keySet();
    }

    /**
     * 특정 슬롯의 특정 속성 보너스 총합을 반환합니다. (7 (+2) 표현용)
     */
    public double getBonusFor(Attribute attr, EquipmentSlotGroup slot) {
        return attributeMap.getOrDefault(slot, Collections.emptyMap()).getOrDefault(attr, 0.0);
    }

    /**
     * 실제로 아이템 메타에 박을 AttributeModifier 객체 리스트를 생성합니다.
     */
    public List<AttributeModifier> getModifiers() {
        List<AttributeModifier> modifiers = new ArrayList<>();

        attributeMap.forEach((slot, stats) -> {
            stats.forEach((attr, amount) -> {
                // 우리 플러그인임을 식별할 수 있는 고유 키 생성
                NamespacedKey key = new NamespacedKey("myenchantments",
                        "bonus_" + slot.toString().toLowerCase() + "_" + attr.getKey().getKey());

                modifiers.add(new AttributeModifier(key, amount,
                        AttributeModifier.Operation.ADD_NUMBER, slot));
            });
        });

        return modifiers;
    }

    /**
     * 해당 슬롯에 우리 플러그인의 보너스가 하나라도 있는지 확인합니다.
     */
    public boolean hasBonusInSlot(EquipmentSlotGroup slot) {
        return attributeMap.containsKey(slot) && !attributeMap.get(slot).isEmpty();
    }
}