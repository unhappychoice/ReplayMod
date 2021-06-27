package com.replaymod.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(ParticleManager.class)
public abstract class Mixin_ParticleManager {
    @Final
    @Shadow
    private Queue<Particle> queue;

    /**
     * This method additionally clears the queue of particles to be added when the world is changed.
     * Otherwise particles from the previous world might show up in this one if they were spawned after
     * the last tick in the previous world.
     *
     * @param world The new world
     * @param ci    Callback info
     */
    @Inject(method = "clearEffects", at = @At("HEAD"))
    public void replayModReplay_clearParticleQueue(
            ClientWorld world,
            CallbackInfo ci) {
        this.queue.clear();
    }
}
