package com.replaymod.mixin;

import com.replaymod.gui.utils.Consumer;
import com.replaymod.recording.packet.ResourcePackRecorder;
import net.minecraft.client.resources.DownloadingPackFinder;
import net.minecraft.resources.IPackNameDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(DownloadingPackFinder.class)
public abstract class MixinDownloadingPackFinder implements ResourcePackRecorder.IDownloadingPackFinder {
    private Consumer<File> requestCallback;

    @Override
    public void setRequestCallback(Consumer<File> callback) {
        requestCallback = callback;
    }

    @Inject(method = "setServerPack", at = @At("HEAD"))
    private void recordDownloadedPack(
            File file,
            IPackNameDecorator arg,
            CallbackInfoReturnable ci
    ) {
        if (requestCallback != null) {
            requestCallback.consume(file);
            requestCallback = null;
        }
    }
}
