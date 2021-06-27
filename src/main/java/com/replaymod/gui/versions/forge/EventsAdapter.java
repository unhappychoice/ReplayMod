package com.replaymod.gui.versions.forge;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.replaymod.gui.utils.EventRegistrations;
import com.replaymod.gui.versions.callbacks.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class EventsAdapter extends EventRegistrations {
    public static Screen getScreen(GuiScreenEvent event) {
        return event.getGui();
    }

    public static List<Widget> getButtonList(GuiScreenEvent.InitGuiEvent event) {
        return event.getWidgetList();
    }

    @SubscribeEvent
    public void preGuiInit(GuiScreenEvent.InitGuiEvent.Pre event) {
        InitScreenCallback.Pre.EVENT.invoker().preInitScreen(getScreen(event));
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        InitScreenCallback.EVENT.invoker().initScreen(getScreen(event), getButtonList(event));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGuiClosed(GuiOpenEvent event) {
        OpenGuiScreenCallback.EVENT.invoker().openGuiScreen(
                event.getGui()
        );
    }

    public static float getPartialTicks(RenderGameOverlayEvent event) {
        return event.getPartialTicks();
    }

    public static float getPartialTicks(GuiScreenEvent.DrawScreenEvent.Post event) {
        return event.getRenderPartialTicks();
    }

    @SubscribeEvent
    public void onGuiRender(GuiScreenEvent.DrawScreenEvent.Post event) {
        PostRenderScreenCallback.EVENT.invoker().postRenderScreen(new MatrixStack(), getPartialTicks(event));
    }

    // Even when event was cancelled cause Lunatrius' InGame-Info-XML mod cancels it and we don't actually care about
    // the event (i.e. the overlay text), just about when it's called.
    @SubscribeEvent(receiveCanceled = true)
    public void renderOverlay(RenderGameOverlayEvent.Text event) {
        RenderHudCallback.EVENT.invoker().renderHud(new MatrixStack(), getPartialTicks(event));
    }

    @SubscribeEvent
    public void tickOverlay(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            PreTickCallback.EVENT.invoker().preTick();
        }
    }
}
