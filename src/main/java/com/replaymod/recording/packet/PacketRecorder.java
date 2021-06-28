package com.replaymod.recording.packet;

import com.github.steveice10.netty.buffer.PooledByteBufAllocator;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.login.server.SCustomPayloadLoginPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.replaymod.replaystudio.util.Utils.writeInt;

public class PacketRecorder {
    private static final Logger logger = LogManager.getLogger();

    private final ExecutorService saveService = Executors.newSingleThreadExecutor();
    private final ReplayOutputStream packetOutputStream;

    private final long startTime;
    private long lastSentPacket;
    private long timePassedWhilePaused;
    private volatile boolean serverWasPaused;

    public PacketRecorder(ReplayFile replayFile, ReplayMetaData metaData) throws IOException {
        this.packetOutputStream = replayFile.writePacketData();
        this.startTime = metaData.getDate();
    }

    public long getLastSentPacket() {
        return lastSentPacket;
    }

    public void setServerWasPaused(boolean serverWasPaused) {
        this.serverWasPaused = serverWasPaused;
    }

    public void close() throws IOException {
        packetOutputStream.close();
    }

    public void saveIntoReplayFile(IPacket packet) {
        try {
            long now = System.currentTimeMillis();
            if (serverWasPaused) {
                timePassedWhilePaused = now - startTime - lastSentPacket;
                serverWasPaused = false;
            }
            int timestamp = (int) (now - startTime - timePassedWhilePaused);
            lastSentPacket = timestamp;
            PacketData packetData = getPacketData(timestamp, packet);
            saveService.submit(() -> {
                try {
                    if (ReplayMod.isMinimalMode()) {
                        // Minimal mode, ReplayStudio might not know our packet ids, so we cannot use it
                        com.github.steveice10.netty.buffer.ByteBuf packetIdBuf = PooledByteBufAllocator.DEFAULT.buffer();
                        com.github.steveice10.netty.buffer.ByteBuf packetBuf = packetData.getPacket().getBuf();
                        try {
                            new ByteBufNetOutput(packetIdBuf).writeVarInt(packetData.getPacket().getId());

                            int packetIdLen = packetIdBuf.readableBytes();
                            int packetBufLen = packetBuf.readableBytes();
                            writeInt(packetOutputStream, (int) packetData.getTime());
                            writeInt(packetOutputStream, packetIdLen + packetBufLen);
                            packetIdBuf.readBytes(packetOutputStream, packetIdLen);
                            packetBuf.getBytes(packetBuf.readerIndex(), packetOutputStream, packetBufLen);
                        } finally {
                            packetIdBuf.release();
                            packetBuf.release();
                        }
                    } else {
                        packetOutputStream.write(packetData);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            logger.error("Writing packet:", e);
        }
    }

    private PacketData getPacketData(int timestamp, IPacket packet) throws Exception {
        Integer packetId = ProtocolType.PLAY.getPacketId(PacketDirection.CLIENTBOUND, packet);
        boolean loginPhase = false;

        if (packetId == null) {
            packetId = ProtocolType.LOGIN.getPacketId(PacketDirection.CLIENTBOUND, packet);
            loginPhase = true;

            if (packetId == null) {
                throw new IOException("Unknown packet type:" + packet.getClass());
            }
        }

        ByteBuf byteBuf = Unpooled.buffer(256, 1048576);
        PacketBuffer buf = new PacketBuffer(byteBuf);

        try {
            if (packet instanceof SCustomPayloadLoginPacket) {
                ((SCustomPayloadLoginPacket) packet).getInternalData().resetReaderIndex();
            }

            packet.writePacketData(buf);
            return new PacketData(timestamp, new com.replaymod.replaystudio.protocol.Packet(
                    MCVer.getPacketTypeRegistry(loginPhase),
                    packetId,
                    com.github.steveice10.netty.buffer.Unpooled.wrappedBuffer(
                            buf.array(),
                            buf.arrayOffset(),
                            buf.readableBytes()
                    )
            ));
        } finally {
            byteBuf.release();

            if (packet instanceof SCustomPayloadPlayPacket) {
                ((SCustomPayloadPlayPacket) packet).getBufferData().release();
            }
        }
    }
}
