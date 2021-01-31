package com.replaymod.mixin;

import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.storage.ISpawnWorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;


@Mixin(ClientWorld.class)
public abstract class MixinWorldClient extends World implements RecordingEventHandler.RecordingEventSender {
    @Shadow
    private Minecraft mc;

    protected MixinWorldClient(ISpawnWorldInfo mutableWorldProperties, RegistryKey<World> registryKey,
                               DimensionType dimensionType, Supplier<IProfiler> profiler, boolean bl, boolean bl2, long l) {
        super(mutableWorldProperties, registryKey,
                dimensionType, profiler, bl, bl2, l);
    }

    private RecordingEventHandler replayModRecording_getRecordingEventHandler() {
        return ((RecordingEventHandler.RecordingEventSender) this.mc.worldRenderer).getRecordingEventHandler();
    }

    // Sounds that are emitted by thePlayer no longer take the long way over the server
    // but are instead played directly by the client. The server only sends these sounds to
    // other clients so we have to record them manually.
    // E.g. Block place sounds
    @Inject(method = "playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V",
            at = @At("HEAD"))
    public void replayModRecording_recordClientSound(PlayerEntity player, double x, double y, double z, SoundEvent sound, SoundCategory category,
                                                     float volume, float pitch, CallbackInfo ci) {
        if (player == this.mc.player) {
            RecordingEventHandler handler = replayModRecording_getRecordingEventHandler();
            if (handler != null) {
                handler.onClientSound(sound, category, x, y, z, volume, pitch);
            }
        }
    }

    // Same goes for level events (also called effects). E.g. door open, block break, etc.
    @Inject(method = "syncWorldEvent", at = @At("HEAD"))
    private void playLevelEvent(PlayerEntity player, int type, BlockPos pos, int data, CallbackInfo ci) {
        if (player == this.mc.player) {
            // We caused this event, the server won't send it to us
            RecordingEventHandler handler = replayModRecording_getRecordingEventHandler();
            if (handler != null) {
                handler.onClientEffect(type, pos, data);
            }
        }
    }
}
