package dev.o8o1o5.myEnchantments.api;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public interface EnchantProvider {
    String getId();
    String getDisplayName();
    int getMaxLevel();

    void provideStats(int level, EnchantStats stats);

    default Component getFullDisplayName(int level) {
        return Component.text(getDisplayName() + " " + toRoman(level))
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }

    private String toRoman(int level) {
        return switch (level) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III";
            case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(level);
        };
    }
}
