package com.replaymod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.ITagCollectionSupplier;
import net.minecraft.tags.TagRegistryManager;
import net.minecraftforge.common.ForgeTagHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(FMLPlayMessages.class)
public abstract class MixinFMLPlayMessages {
    @Inject(method = "handle", at=@At(value = "HEAD"), cancellable = true)
    public void replayMod_ignoreHandshakeConnectionClose(FMLPlayMessages.SyncCustomTagTypes msg, Supplier<NetworkEvent.Context> ctx, CallbackInfo ci) {
        System.out.println("Injected FMLPlayMessage.handle");

        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().world != null) {
                ITagCollectionSupplier tagCollectionSupplier = Minecraft.getInstance().world.getTags();
                    ForgeTagHandler.updateCustomTagTypes(msg);
                    if (!ctx.get().getNetworkManager().isLocalChannel()) {
                        TagRegistryManager.fetchCustomTagTypes(tagCollectionSupplier);
                        MinecraftForge.EVENT_BUS.post(new TagsUpdatedEvent.CustomTagTypes(tagCollectionSupplier));
                    }
            }
        });
        ctx.get().setPacketHandled(true);
        ci.cancel();
    }
}
