package dev.o8o1o5.myEnchantments;

import dev.o8o1o5.myEnchantments.api.EnchantProvider;
import dev.o8o1o5.myEnchantments.core.ModuleRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

public final class MyEnchantments extends JavaPlugin {

    @Override
    public void onEnable() {
        // 다른 플러그인들이 EnchantProvider 서비스를 등록할 시간을 주기 위해
        // 한 틱(tick) 후에 프로바이더를 로드합니다.
        Bukkit.getScheduler().runTask(this, () -> {
            loadProvidersFromServices();
            getLogger().info("Loaded " + ModuleRegistry.getAll().size() + " enchantment modules.");
        });

        getLogger().info("MyEnchantments enabled! Waiting for other plugins to register enchantments...");
    }

    private void loadProvidersFromServices() {
        // 서버에 등록된 모든 EnchantProvider 서비스를 가져옵니다.
        Collection<EnchantProvider> providers = Bukkit.getServicesManager().getRegistrations(EnchantProvider.class).stream()
                .map(org.bukkit.plugin.RegisteredServiceProvider::getProvider)
                .toList();

        if (providers.isEmpty()) {
            getLogger().info("No external enchantment providers found.");
            return;
        }

        // 가져온 모든 프로바이더를 모듈 레지스트리에 등록합니다.
        for (EnchantProvider provider : providers) {
            ModuleRegistry.register(provider);
            getLogger().info("Registered enchantment: " + provider.getId());
        }
    }


    @Override
    public void onDisable() {
        getLogger().info("MyEnchantments disabled!");
    }
}
