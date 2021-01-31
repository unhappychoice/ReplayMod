package com.replaymod.mixin;

import net.minecraft.client.MainWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MainWindow.class)
public interface MainWindowAccessor {
    @Accessor
    int getFramebufferWidth();

    @Accessor
    void setFramebufferWidth(int value);

    @Accessor
    int getFramebufferHeight();

    @Accessor
    void setFramebufferHeight(int value);
}
