package com.replaymod.extras.playeroverview;

import com.google.common.base.Optional;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.PreRenderHandCallback;
import com.replaymod.core.utils.Utils;
import com.replaymod.extras.Extra;
import com.replaymod.gui.utils.EventRegistrations;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.events.ReplayClosedCallback;
import com.replaymod.replay.events.ReplayOpenedCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.replaymod.core.versions.MCVer.Keyboard;

public class PlayerOverview extends EventRegistrations implements Extra {
    private ReplayModReplay module;

    private final Set<UUID> hiddenPlayers = new HashSet<>();
    private boolean savingEnabled;

    @Override
    public void register(final ReplayMod mod) throws Exception {
        this.module = ReplayModReplay.instance;

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.playeroverview", Keyboard.KEY_B, new Runnable() {
            @Override
            public void run() {
                if (module.getReplayHandler() != null) {
                    List<PlayerEntity> players = mod.getMinecraft().world.getPlayers()
                            .stream()
                            .map(it -> (PlayerEntity) it)
                            .filter(it -> !(it instanceof CameraEntity))
                            .collect(Collectors.toList());
                    if (!Utils.isCtrlDown()) {
                        // Hide all players that have an UUID v2 (commonly used for NPCs)
                        Iterator<PlayerEntity> iter = players.iterator();
                        while (iter.hasNext()) {
                            UUID uuid = iter.next().getGameProfile().getId();
                            if (uuid != null && uuid.version() == 2) {
                                iter.remove();
                            }
                        }
                    }
                    new PlayerOverviewGui(PlayerOverview.this, players).display();
                }
            }
        }, true);

        register();
    }

    public boolean isHidden(UUID uuid) {
        return hiddenPlayers.contains(uuid);
    }

    public void setHidden(UUID uuid, boolean hidden) {
        if (hidden) {
            hiddenPlayers.add(uuid);
        } else {
            hiddenPlayers.remove(uuid);
        }
    }

    {
        on(ReplayOpenedCallback.EVENT, this::onReplayOpen);
    }

    private void onReplayOpen(ReplayHandler replayHandler) throws IOException {
        Optional<Set<UUID>> savedData = replayHandler.getReplayFile().getInvisiblePlayers();
        if (savedData.isPresent()) {
            hiddenPlayers.addAll(savedData.get());
            savingEnabled = true;
        } else {
            savingEnabled = false;
        }
    }

    {
        on(ReplayClosedCallback.EVENT, this::onReplayClose);
    }

    private void onReplayClose(ReplayHandler replayHandler) {
        hiddenPlayers.clear();
    }

    {
        on(PreRenderHandCallback.EVENT, this::shouldHideHand);
    }

    private boolean shouldHideHand() {
        Entity view = module.getCore().getMinecraft().getRenderViewEntity();
        return view != null && isHidden(view.getUniqueID());
    }

    // See MixinRender for why this is 1.7.10 only

    public boolean isSavingEnabled() {
        return savingEnabled;
    }

    public void setSavingEnabled(boolean savingEnabled) {
        this.savingEnabled = savingEnabled;
    }

    public void saveHiddenPlayers() {
        if (savingEnabled) {
            try {
                module.getReplayHandler().getReplayFile().writeInvisiblePlayers(hiddenPlayers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
