package com.replaymod.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.recording.handler.RecordingEventHandler;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.packets.PacketPlayerListEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.network.play.server.SRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Mixin(ClientPlayNetHandler.class)
public abstract class MixinNetHandlerPlayClient {

    // The stupid name is required as otherwise Mixin treats it as a shadow, seemingly ignoring the lack of @Shadow
    private static Minecraft mcStatic = MCVer.getMinecraft();

    @Shadow
    private Map<UUID, NetworkPlayerInfo> playerInfoMap;

    public RecordingEventHandler getRecordingEventHandler() {
        return ((RecordingEventHandler.RecordingEventSender) mcStatic.worldRenderer).getRecordingEventHandler();
    }

    /**
     * Record the own player entity joining the world.
     * We cannot use the {@link net.minecraftforge.event.entity.EntityJoinWorldEvent} because the entity id
     * of the player is set afterwards and the tablist entry might not yet be sent.
     *
     * @param packet The packet
     * @param ci     Callback info
     */
    @Inject(method = "handlePlayerListItem", at = @At("HEAD"))
    public void recordOwnJoin(SPlayerListItemPacket packet, CallbackInfo ci) {
        if (!mcStatic.isOnExecutionThread()) return;
        if (mcStatic.player == null) return;

        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null && packet.getAction() == SPlayerListItemPacket.Action.ADD_PLAYER) {
            // We cannot reference SPacketPlayerListItem.AddPlayerData directly for complicated (and yet to be
            // resolved) reasons (see https://github.com/MinecraftForge/ForgeGradle/issues/472), so we use ReplayStudio
            // to parse it instead.
            ByteBuf byteBuf = Unpooled.buffer();
            try {
                packet.writePacketData(new PacketBuffer(byteBuf));

                byteBuf.readerIndex(0);
                byte[] array = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(array);

                for (PacketPlayerListEntry data : PacketPlayerListEntry.read(new Packet(
                        MCVer.getPacketTypeRegistry(false), 0, PacketType.PlayerListEntry,
                        com.github.steveice10.netty.buffer.Unpooled.wrappedBuffer(array)
                ))) {
                    if (data.getUuid() == null) continue;
                    // Only add spawn packet for our own player and only if he isn't known yet
                    if (data.getUuid().equals(mcStatic.player.getGameProfile().getId())
                            && !this.playerInfoMap.containsKey(data.getUuid())) {
                        handler.spawnRecordingPlayer();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e); // we just parsed this?
            } finally {
                byteBuf.release();
            }
        }
    }

    /**
     * Record the own player entity respawning.
     * We cannot use the {@link net.minecraftforge.event.entity.EntityJoinWorldEvent} because that would also include
     * the first spawn which is already handled by the above method.
     *
     * @param packet The packet
     * @param ci     Callback info
     */
    @Inject(method = "handleRespawn", at = @At("RETURN"))
    public void recordOwnRespawn(SRespawnPacket packet, CallbackInfo ci) {
        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null) {
            handler.spawnRecordingPlayer();
        }
    }
}
