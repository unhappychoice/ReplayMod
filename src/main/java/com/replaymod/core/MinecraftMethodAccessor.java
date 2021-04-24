package com.replaymod.core;

public interface MinecraftMethodAccessor {
    //#if MC>=11400
    void replayModProcessKeyBinds();
    //#else
    //#if MC>=10904
    //$$ void replayModRunTickMouse();
    //$$ void replayModRunTickKeyboard();
    //#else
    //$$ void replayModSetEarlyReturnFromRunTick(boolean earlyReturn);
    //#endif
    //#endif
    //#if MC>=11400
    void replayModExecuteTaskQueue();
    //#endif
}
