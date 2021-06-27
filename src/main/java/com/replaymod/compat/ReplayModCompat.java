package com.replaymod.compat;

import com.replaymod.compat.optifine.DisableFastRender;
import com.replaymod.compat.shaders.ShaderBeginRender;
import com.replaymod.core.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReplayModCompat implements Module {
    public static Logger LOGGER = LogManager.getLogger();

    @Override
    public void initClient() {
        new ShaderBeginRender().register();
        new DisableFastRender().register();
    }

}
