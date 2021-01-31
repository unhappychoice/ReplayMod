package com.replaymod.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor
    double getPrevPosX();

    @Accessor
    double getPrevPosY();

    @Accessor
    double getPrevPosZ();

    @Accessor
    double getPosX();

    @Accessor
    double getPosY();

    @Accessor
    double getPosZ();
}
