package com.replaymod.core.utils;

import com.replaymod.replaystudio.data.ModInfo;
import net.minecraftforge.fml.ModList;

import java.util.Collection;
import java.util.stream.Collectors;

class ModInfoGetter {
    static Collection<ModInfo> getInstalledNetworkMods() {
        return ModList.get().getMods().stream()
                .map(mod -> new ModInfo(mod.getModId(), mod.getModId(), mod.getVersion().toString()))
                .collect(Collectors.toList());
    }
}
