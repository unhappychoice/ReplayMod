package com.replaymod.core;

import com.replaymod.core.versions.forge.EventsAdapter;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ReplayMod.MOD_ID)
public class ReplayModBackend {
    private final ReplayMod mod = new ReplayMod(this);
    private final EventsAdapter eventsAdapter = new EventsAdapter();

    // @Deprecated
    // public static Configuration config;

    public ReplayModBackend() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
    }

    public void init(FMLCommonSetupEvent event) {
        mod.initModules();
        eventsAdapter.register();
        // config = new Configuration(event.getSuggestedConfigurationFile());
        // config.load();
        // SettingsRegistry settingsRegistry = mod.getSettingsRegistry();
        // settingsRegistry.backend.setConfiguration(config);
        // settingsRegistry.save(); // Save default values to disk
    }

    public String getVersion() {
        return "2.5.1";
    }

    public String getMinecraftVersion() {
        return "1.16.4";
    }

    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }
}
