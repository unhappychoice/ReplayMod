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
@Mixin(net.minecraft.client.renderer.RenderState.class)
//#else
//$$ @Mixin(net.minecraft.client.render.item.ItemRenderer.class)
//#endif
public class Mixin_RenderItem {
    //#if MC>=11400
    //#if MC>=11500
    @Redirect(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;milliTime()J"))
    //#else
    //#if MC>=11400
    //$$ @Redirect(method = "renderGlint", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"))
    //#else
    //$$ @Redirect(method = "renderEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;milliTime()J"))
    //#endif
    //#endif
    private static long getEnchantmentTime() {
    //#else
    //$$ @Redirect(method = "renderEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //$$ private long getEnchantmentTime() {
    //#endif
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
