package com.replaymod.mixin;

import net.minecraft.client.gui.widget.Widget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Widget.class)
public interface AbstractButtonWidgetAccessor {
    @Accessor
    int getHeight();
}
