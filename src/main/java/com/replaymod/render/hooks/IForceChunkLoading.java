package com.replaymod.render.hooks;

import net.minecraft.client.renderer.WorldRenderer;

public interface IForceChunkLoading {
    void replayModRender_setHook(ForceChunkLoadingHook hook);

    static IForceChunkLoading from(WorldRenderer worldRenderer) {
        return (IForceChunkLoading) worldRenderer;
    }
}
