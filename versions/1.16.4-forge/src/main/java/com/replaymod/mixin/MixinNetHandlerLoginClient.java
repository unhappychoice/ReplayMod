package com.replaymod.mixin;

import com.replaymod.recording.ReplayModRecording;
import net.minecraft.client.network.login.ClientLoginNetHandler;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLoginNetHandler.class)
public abstract class MixinNetHandlerLoginClient {

    @Final @Shadow
    private NetworkManager networkManager;

    /**
     * Starts the recording right before switching into PLAY state.
     * We cannot use the {@link FMLNetworkEvent.ClientConnectedToServerEvent}
     * as it only fires after the forge handshake.
     */
    @Inject(method = "handleLoginSuccess", at=@At("HEAD"))
    public void replayModRecording_initiateRecording(CallbackInfo cb) {
        ReplayModRecording.instance.initiateRecording(this.networkManager);
    }
}
