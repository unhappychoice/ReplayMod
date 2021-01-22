package com.replaymod.mixin;

import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC>=11400
import net.minecraft.util.Util;
//#else
//$$ import net.minecraft.client.Minecraft;
//#endif

//#if MC>=11500
@Mixin(net.minecraft.client.renderer.RenderState.PortalTexturingState.class)
//#else
//$$ @Mixin(net.minecraft.client.render.block.entity.EndPortalBlockEntityRenderer.class)
//#endif
public class MixinTileEntityEndPortalRenderer {
    //#if MC>=11500
    @Redirect(method = "func_228597_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;milliTime()J"))
    static
    //#else
    //#if MC>=11400
    //$$ @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"))
    //#else
    //#if MC>=11200
    //$$ @Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityEndPortal;DDDFIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#else
    //#if MC>=10809
    //$$ @Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityEndPortal;DDDFI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#else
    //#if MC>=10800
    //$$ @Redirect(method = "func_180544_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#else
    //$$ @Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityEndPortal;DDDF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#endif
    //#endif
    //#endif
    //#endif
    //#endif
    private long replayModReplay_getEnchantmentTime() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        //#if MC>=11400
        return Util.milliTime();
        //#else
        //$$ return Minecraft.getSystemTime();
        //#endif
    }
}
