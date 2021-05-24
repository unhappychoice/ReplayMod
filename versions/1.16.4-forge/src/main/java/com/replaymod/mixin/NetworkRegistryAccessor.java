package com.replaymod.mixin;

import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(NetworkRegistry.class)
public interface NetworkRegistryAccessor {
    @Invoker("gatherLoginPayloads")
    static List<NetworkRegistry.LoginPayload> invokeGatherLoginPayloads(NetworkDirection direction, boolean isLocal) {
        throw new AssertionError();
    }
}
