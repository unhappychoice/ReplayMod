package com.replaymod.mixin;

import net.minecraft.client.gui.screen.AddServerScreen;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AddServerScreen.class)
public interface AddServerScreenAccessor {
    @Accessor("serverData")
    ServerData getServer();
}
