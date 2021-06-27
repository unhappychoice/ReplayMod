package com.replaymod.recording.handler;

import com.mojang.datafixers.util.Pair;
import com.replaymod.core.events.PreRenderCallback;
import com.replaymod.gui.utils.EventRegistrations;
import com.replaymod.gui.versions.callbacks.PreTickCallback;
import com.replaymod.mixin.EntityLivingBaseAccessor;
import com.replaymod.mixin.IntegratedServerAccessor;
import com.replaymod.recording.packet.PacketListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.*;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;
import java.util.Objects;

import static com.replaymod.core.versions.MCVer.getMinecraft;

public class RecordingEventHandler extends EventRegistrations {

    private final Minecraft mc = getMinecraft();
    private final PacketListener packetListener;

    private Double lastX, lastY, lastZ;
    private ItemStack[] playerItems = new ItemStack[6];
    private int ticksSinceLastCorrection;
    private boolean wasSleeping;
    private int lastRiding = -1;
    private Integer rotationYawHeadBefore;
    private boolean wasHandActive;
    private Hand lastActiveHand;

    public RecordingEventHandler(PacketListener packetListener) {
        this.packetListener = packetListener;
    }

    @Override
    public void register() {
        super.register();
        ((RecordingEventSender) mc.worldRenderer).setRecordingEventHandler(this);
    }

    @Override
    public void unregister() {
        super.unregister();
        RecordingEventSender recordingEventSender = ((RecordingEventSender) mc.worldRenderer);
        if (recordingEventSender.getRecordingEventHandler() == this) {
            recordingEventSender.setRecordingEventHandler(null);
        }
    }

    public void onPacket(IPacket<?> packet) {
        packetListener.save(packet);
    }

    public void spawnRecordingPlayer() {
        try {
            ClientPlayerEntity player = mc.player;
            assert player != null;
            packetListener.save(new SSpawnPlayerPacket(player));
            packetListener.save(new SEntityMetadataPacket(player.getEntityId(), player.getDataManager(), true));
            lastX = lastY = lastZ = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClientSound(SoundEvent sound, SoundCategory category,
                              double x, double y, double z, float volume, float pitch) {
        try {
            // Send to all other players in ServerWorldEventHandler#playSoundToAllNearExcept
            packetListener.save(new SPlaySoundEffectPacket(sound, category, x, y, z, volume, pitch));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClientEffect(int type, BlockPos pos, int data) {
        try {
            // Send to all other players in ServerWorldEventHandler#playEvent
            packetListener.save(new SPlaySoundEventPacket(type, pos, data, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    {
        on(PreTickCallback.EVENT, this::onPlayerTick);
    }

    private void onPlayerTick() {
        if (mc.player == null) return;
        ClientPlayerEntity player = mc.player;
        try {

            boolean force = false;
            if (lastX == null || lastY == null || lastZ == null) {
                force = true;
                lastX = player.getPosX();
                lastY = player.getPosY();
                lastZ = player.getPosZ();
            }

            ticksSinceLastCorrection++;
            if (ticksSinceLastCorrection >= 100) {
                ticksSinceLastCorrection = 0;
                force = true;
            }

            double dx = player.getPosX() - lastX;
            double dy = player.getPosY() - lastY;
            double dz = player.getPosZ() - lastZ;

            lastX = player.getPosX();
            lastY = player.getPosY();
            lastZ = player.getPosZ();

            IPacket packet;
            if (force || Math.abs(dx) > 8.0 || Math.abs(dy) > 8.0 || Math.abs(dz) > 8.0) {
                packet = new SEntityTeleportPacket(player);
            } else {
                byte newYaw = (byte) ((int) (player.rotationYaw * 256.0F / 360.0F));
                byte newPitch = (byte) ((int) (player.rotationPitch * 256.0F / 360.0F));

                packet = new SEntityPacket.MovePacket(
                        player.getEntityId(),
                        (short) Math.round(dx * 4096), (short) Math.round(dy * 4096), (short) Math.round(dz * 4096),
                        newYaw, newPitch
                        , player.isOnGround()
                );
            }

            packetListener.save(packet);

            //HEAD POS
            int rotationYawHead = ((int) (player.rotationYawHead * 256.0F / 360.0F));

            if (!Objects.equals(rotationYawHead, rotationYawHeadBefore)) {
                packetListener.save(new SEntityHeadLookPacket(player, (byte) rotationYawHead));
                rotationYawHeadBefore = rotationYawHead;
            }

            packetListener.save(new SEntityVelocityPacket(player.getEntityId(),
                    player.getMotion()
            ));

            //Animation Packets
            //Swing Animation
            if (player.isSwingInProgress && player.swingProgressInt == 0) {
                packetListener.save(new SAnimateHandPacket(
                        player,
                        player.swingingHand == Hand.MAIN_HAND ? 0 : 3
                ));
            }

			/*
        //Potion Effect Handling
		List<Integer> found = new ArrayList<Integer>();
		for(PotionEffect pe : (Collection<PotionEffect>)player.getActivePotionEffects()) {
			found.add(pe.getPotionID());
			if(lastEffects.contains(found)) continue;
			S1DPacketEntityEffect pee = new S1DPacketEntityEffect(entityID, pe);
			packetListener.save(pee);
		}

		for(int id : lastEffects) {
			if(!found.contains(id)) {
				S1EPacketRemoveEntityEffect pre = new S1EPacketRemoveEntityEffect(entityID, new PotionEffect(id, 0));
				packetListener.save(pre);
			}
		}

		lastEffects = found;
			 */

            //Inventory Handling
            for (EquipmentSlotType slot : EquipmentSlotType.values()) {
                ItemStack stack = player.getItemStackFromSlot(slot);
                if (playerItems[slot.ordinal()] != stack) {
                    playerItems[slot.ordinal()] = stack;
                    packetListener.save(new SEntityEquipmentPacket(player.getEntityId(), Collections.singletonList(Pair.of(slot, stack))));
                }
            }

            //Leaving Ride

            Entity vehicle = player.getRidingEntity();
            int vehicleId = vehicle == null ? -1 : vehicle.getEntityId();
            if (lastRiding != vehicleId) {
                lastRiding = vehicleId;
                packetListener.save(new SMountEntityPacket(
                        player,
                        vehicle
                ));
            }

            //Sleeping
            if (!player.isSleeping() && wasSleeping) {
                packetListener.save(new SAnimateHandPacket(player, 2));
                wasSleeping = false;
            }

            // Active hand (e.g. eating, drinking, blocking)
            if (player.isHandActive() ^ wasHandActive || player.getActiveHand() != lastActiveHand) {
                wasHandActive = player.isHandActive();
                lastActiveHand = player.getActiveHand();
                EntityDataManager dataManager = new EntityDataManager(null);
                int state = (wasHandActive ? 1 : 0) | (lastActiveHand == Hand.OFF_HAND ? 2 : 0);
                dataManager.register(EntityLivingBaseAccessor.getLivingFlags(), (byte) state);
                packetListener.save(new SEntityMetadataPacket(player.getEntityId(), dataManager, true));
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onPickupItem(ItemPickupEvent event) {
        try {
            ItemStack stack = event.getStack();
            packetListener.save(new SCollectItemPacket(
                    event.getOriginalEntity().getEntityId(),
                    event.getPlayer().getEntityId(),
                    event.getStack().getCount()
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // FIXME fabric

    /* FIXME event not (yet?) on 1.13
    @SubscribeEvent
    public void enterMinecart(MinecartInteractEvent event) {
        try {
                        if(event.getEntity() != mc.player) {
                return;
            }

            packetListener.save(new SPacketEntityAttach(event.getPlayer(), event.getMinecart()));

            lastRiding = event.getMinecart().getEntityId();
                                                                                                                    } catch(Exception e) {
            e.printStackTrace();
        }
    }
    */

    public void onBlockBreakAnim(int breakerId, BlockPos pos, int progress) {
        PlayerEntity thePlayer = mc.player;
        if (thePlayer != null && breakerId == thePlayer.getEntityId()) {
            packetListener.save(new SAnimateBlockBreakPacket(breakerId,
                    pos,
                    progress));
        }
    }

    {
        on(PreRenderCallback.EVENT, this::checkForGamePaused);
    }

    private void checkForGamePaused() {
        if (mc.isSingleplayer()) {
            IntegratedServer server = mc.getIntegratedServer();
            if (server != null && ((IntegratedServerAccessor) server).isGamePaused()) {
                packetListener.setServerWasPaused();
            }
        }
    }

    public interface RecordingEventSender {
        void setRecordingEventHandler(RecordingEventHandler recordingEventHandler);

        RecordingEventHandler getRecordingEventHandler();
    }
}
