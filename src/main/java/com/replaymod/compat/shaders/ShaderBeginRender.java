//#if MC>=10800
package com.replaymod.compat.shaders;

import com.replaymod.core.events.PreRenderCallback;
import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.gui.utils.EventRegistrations;
import net.minecraft.client.Minecraft;

import java.lang.reflect.InvocationTargetException;

public class ShaderBeginRender extends EventRegistrations {

    private final Minecraft mc = Minecraft.getInstance();

    /**
     *  Invokes Shaders#beginRender when rendering a video,
     *  as this would usually get called by EntityRenderer#renderWorld,
     *  which we're not calling during rendering.
     */
    { on(PreRenderCallback.EVENT, this::onRenderTickStart); }
    private void onRenderTickStart() {
        if (ShaderReflection.shaders_beginRender == null) return;
        if (ShaderReflection.config_isShaders == null) return;

        try {
            // check if video is being rendered
            if (((EntityRendererHandler.IEntityRenderer) mc.gameRenderer).replayModRender_getHandler() == null)
                return;

            // check if Shaders are enabled
            if (!(boolean) (ShaderReflection.config_isShaders.invoke(null))) return;

            ShaderReflection.shaders_beginRender.invoke(null, mc,
                    //#if MC>=11400
                    mc.gameRenderer.getActiveRenderInfo(),
                    //#endif
                    mc.getRenderPartialTicks(), 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
//#endif
