package com.replaymod.core.utils;

import com.replaymod.replaystudio.data.ModInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ModInfoGetter {
    static Collection<ModInfo> getInstalledNetworkMods() {
        return  ModList.get().getMods().stream()
                .map(mod -> new ModInfo(mod.getModId(), mod.getModId(), mod.getVersion().toString()))
                .collect(Collectors.toList());
    }
}
