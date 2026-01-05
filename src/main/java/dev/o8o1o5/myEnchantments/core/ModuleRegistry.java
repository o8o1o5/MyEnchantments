package dev.o8o1o5.myEnchantments.core;

import dev.o8o1o5.myEnchantments.api.EnchantProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ModuleRegistry {
    private static final Map<String, EnchantProvider> providers = new HashMap<>();

    public static void register(EnchantProvider provider) {
        providers.put(provider.getId(), provider);
    }

    public static EnchantProvider getProvider(String id) { return providers.get(id); }
    public static Collection<EnchantProvider> getAll() { return providers.values(); }
}
