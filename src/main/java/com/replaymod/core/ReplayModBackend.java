package com.replaymod.core;

import com.mojang.bridge.launcher.Launcher;
import com.replaymod.mixin.MinecraftAccessor;
import com.replaymod.core.versions.forge.EventsAdapter;
import com.replaymod.extras.modcore.ModCoreInstaller;
import net.minecraft.resources.IResourcePack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.util.List;

import static com.replaymod.core.ReplayMod.MOD_ID;
import static com.replaymod.core.ReplayMod.jGuiResourcePack;
import static com.replaymod.core.versions.MCVer.getMinecraft;

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
