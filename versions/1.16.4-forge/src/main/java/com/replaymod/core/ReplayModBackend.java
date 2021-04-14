package com.replaymod.core;

import com.replaymod.core.mixin.MinecraftAccessor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;

import java.util.List;

import static com.replaymod.core.ReplayMod.MOD_ID;
import static com.replaymod.core.ReplayMod.jGuiResourcePack;
import static com.replaymod.core.versions.MCVer.getMinecraft;

/*
        useMetadata = true,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        acceptableRemoteVersions = "*",
        //#if MC>=10800
        clientSideOnly = true,
        updateJSON = "https://raw.githubusercontent.com/ReplayMod/ReplayMod/master/versions.json",
        //#endif
        guiFactory = "com.replaymod.core.gui.GuiFactory")
 */
@Mod(ReplayMod.MOD_ID)
public class ReplayModBackend {
    private final ReplayMod mod = new ReplayMod(this);
    // private final EventsAdapter eventsAdapter = new EventsAdapter();

    // @Deprecated
    // public static Configuration config;

    public ReplayModBackend() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
    }

    public void init(FMLCommonSetupEvent event) {
        mod.initModules();
        // config = new Configuration(event.getSuggestedConfigurationFile());
        // config.load();
        SettingsRegistry settingsRegistry = mod.getSettingsRegistry();
        // settingsRegistry.backend.setConfiguration(config);
        settingsRegistry.save(); // Save default values to disk
    }

    public String getVersion() {
        return "2.5.1";
    }

    public String getMinecraftVersion() {
        return "1.16.4";
    }

    public boolean isModLoaded(String id) {
        return true;
    }

    static { // Note: even preInit is too late and we'd have to issue another resource reload
        // TODO:
        // List<IResourcePack> defaultResourcePacks = ((MinecraftAccessor) getMinecraft()).getDefaultResourcePacks();

        // if (jGuiResourcePack != null) {
        //     defaultResourcePacks.add(jGuiResourcePack);
        // }

        //#if MC<=10710
        //$$ FolderResourcePack mainResourcePack = new FolderResourcePack(new File("../src/main/resources")) {
        //$$     @Override
        //$$     protected InputStream getInputStreamByName(String resourceName) throws IOException {
        //$$         try {
        //$$             return super.getInputStreamByName(resourceName);
        //$$         } catch (IOException e) {
        //$$             if ("pack.mcmeta".equals(resourceName)) {
        //$$                 return new ByteArrayInputStream(("{\"pack\": {\"description\": \"dummy pack for mod resources in dev-env\", \"pack_format\": 1}}").getBytes(StandardCharsets.UTF_8));
        //$$             }
        //$$             throw e;
        //$$         }
        //$$     }
        //$$ };
        //$$ defaultResourcePacks.add(mainResourcePack);
        //#endif
    }
}
