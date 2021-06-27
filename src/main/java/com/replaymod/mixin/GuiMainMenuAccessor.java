package com.replaymod.mixin;

import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MainMenuScreen.class)
public interface GuiMainMenuAccessor {
    @Accessor
    Screen getRealmsNotification();

    @Accessor
    void setRealmsNotification(Screen value);
}
