package com.replaymod.mixin;

import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Screen.class)
public interface GuiScreenAccessor {
    @Accessor
    List<Widget> getButtons();

    @Accessor
    List<IGuiEventListener> getChildren();
}
