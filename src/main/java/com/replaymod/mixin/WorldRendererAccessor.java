package com.replaymod.mixin;

import net.minecraft.client.renderer.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
}
