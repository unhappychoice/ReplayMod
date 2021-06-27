package com.replaymod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.util.SearchTreeManager;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.STagsListPacket;
import net.minecraft.tags.ITagCollectionSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetHandler.class)
public abstract class MixinClientPlayNetHandler {
    @Shadow
    @Final
    private NetworkManager netManager;

    @Shadow
    private Minecraft client;

    @Shadow
    private ITagCollectionSupplier networkTagManager;

    @Inject(method = "handleTags", at = @At(value = "HEAD"), cancellable = true)
    public void replayMod_ignoreHandshakeConnectionClose(STagsListPacket packetIn, CallbackInfo ci) {
        System.out.println("Injected ClientPlayNetHandler.handleTags");
        // PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
        ITagCollectionSupplier itagcollectionsupplier = packetIn.getTags();
        // boolean vanillaConnection = net.minecraftforge.fml.network.NetworkHooks.isVanillaConnection(netManager);
        boolean vanillaConnection = false;
        net.minecraftforge.common.ForgeTagHandler.resetCachedTagCollections(true, vanillaConnection);
        itagcollectionsupplier = ITagCollectionSupplier.reinjectOptionalTags(itagcollectionsupplier);
        this.networkTagManager = itagcollectionsupplier;
        if (!this.netManager.isLocalChannel()) {
            itagcollectionsupplier.updateTags();
        }

        this.client.getSearchTree(SearchTreeManager.TAGS).recalculate();

        ci.cancel();
    }
}
