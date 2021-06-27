package com.replaymod.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.network.login.ClientLoginNetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.server.SCustomPayloadLoginPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLoginNetHandler.class)
public abstract class MixinNetHandlerLoginClient {

    @Final
    @Shadow
    private NetworkManager networkManager;

    @Inject(method = "handleLoginSuccess", at = @At("HEAD"))
    public void replayModRecording_initiateRecording(CallbackInfo cb) {
        initiateRecording(null);
    }

    /**
     * Starts the recording right before switching into PLAY state.
     * We cannot use the {@link FMLNetworkEvent.ClientConnectedToServerEvent}
     * as it only fires after the forge handshake.
     */
    @Inject(method = "handleCustomPayloadLogin", at = @At("HEAD"))
    public void replayModRecording_initiateRecording(SCustomPayloadLoginPacket packetIn, CallbackInfo cb) {
        initiateRecording(packetIn);
    }

    private void initiateRecording(SCustomPayloadLoginPacket packet) {
        RecordingEventHandler.RecordingEventSender eventSender = (RecordingEventHandler.RecordingEventSender) MCVer.getMinecraft().worldRenderer;
        if (eventSender.getRecordingEventHandler() != null) {
            return; // already recording
        }
        ReplayModRecording.instance.initiateRecording(this.networkManager);
        if (eventSender.getRecordingEventHandler() != null && packet != null) {
            eventSender.getRecordingEventHandler().onPacket(packet);
        }
    }
}
