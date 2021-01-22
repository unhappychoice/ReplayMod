package com.replaymod.recording;

import net.minecraft.client.multiplayer.ServerData;

/** Extension interface for {@link net.minecraft.client.multiplayer.ServerData}. */
public interface ServerInfoExt {

    static ServerInfoExt from(ServerData base) {
        return (ServerInfoExt) base;
    }

    /** Per-server optional overwrite for {@link Setting#AUTO_START_RECORDING}. */
    Boolean getAutoRecording();

    /** Per-server optional overwrite for {@link Setting#AUTO_START_RECORDING}. */
    void setAutoRecording(Boolean autoRecording);
}
