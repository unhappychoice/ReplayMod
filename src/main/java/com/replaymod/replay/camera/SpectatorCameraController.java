package com.replaymod.replay.camera;

import com.replaymod.mixin.EntityPlayerAccessor;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;

import java.util.Arrays;

import static com.replaymod.core.versions.MCVer.getMinecraft;

public class SpectatorCameraController implements CameraController {
    private final CameraEntity camera;

    public SpectatorCameraController(CameraEntity camera) {
        this.camera = camera;
    }

    @Override
    public void update(float partialTicksPassed) {
        Minecraft mc = getMinecraft();
        if (mc.gameSettings.keyBindSneak.isPressed()) {
            ReplayModReplay.instance.getReplayHandler().spectateCamera();
        }

        // Soak up all remaining key presses
        for (KeyBinding binding : Arrays.asList(mc.gameSettings.keyBindAttack, mc.gameSettings.keyBindUseItem,
                mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSneak, mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight)) {
            //noinspection StatementWithEmptyBody
            while (binding.isPressed()) ;
        }

        // Prevent mouse movement
        // No longer needed

        // Always make sure the camera is in the exact same spot as the spectated entity
        // This is necessary as some rendering code for the hand doesn't respect the view entity
        // and always uses mc.thePlayer
        Entity view = mc.getRenderViewEntity();
        if (view != null && view != camera) {
            camera.setCameraPosRot(mc.getRenderViewEntity());
            // If it's a player, also 'steal' its inventory so the rendering code knows what item to render
            if (view instanceof PlayerEntity) {
                PlayerEntity viewPlayer = (PlayerEntity) view;
                camera.setItemStackToSlot(EquipmentSlotType.HEAD, viewPlayer.getItemStackFromSlot(EquipmentSlotType.HEAD));
                camera.setItemStackToSlot(EquipmentSlotType.MAINHAND, viewPlayer.getItemStackFromSlot(EquipmentSlotType.MAINHAND));
                camera.setItemStackToSlot(EquipmentSlotType.OFFHAND, viewPlayer.getItemStackFromSlot(EquipmentSlotType.OFFHAND));
                EntityPlayerAccessor cameraA = (EntityPlayerAccessor) camera;
                EntityPlayerAccessor viewPlayerA = (EntityPlayerAccessor) viewPlayer;
                cameraA.setItemStackMainHand(viewPlayerA.getItemStackMainHand());
                camera.swingingHand = viewPlayer.swingingHand;
                cameraA.setActiveItemStackUseCount(viewPlayerA.getActiveItemStackUseCount());
            }
        }
    }

    @Override
    public void increaseSpeed() {

    }

    @Override
    public void decreaseSpeed() {

    }
}
