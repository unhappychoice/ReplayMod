package com.replaymod.mixin;

import net.minecraft.network.NetworkManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.FMLHandshakeHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FMLHandshakeHandler.class)
public abstract class MixinFMLHandshakeHandler {
    @Redirect(method = "handleRegistryLoading", at=@At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;closeChannel(Lnet/minecraft/util/text/ITextComponent;)V"))
    public void replayMod_ignoreHandshakeConnectionClose(NetworkManager networkManager, ITextComponent message) {}
}
