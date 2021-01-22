package com.replaymod.gui.utils;

//#if FABRIC<1
import com.replaymod.gui.versions.forge.EventsAdapter;
import net.minecraftforge.common.MinecraftForge;
//#endif

//#if MC<10809
//$$ import net.minecraftforge.fml.common.FMLCommonHandler;
//#endif

import java.util.ArrayList;
import java.util.List;

public class EventRegistrations {
    //#if FABRIC<1
    static { new EventsAdapter().register(); }
    //#endif

    private List<EventRegistration<?>> registrations = new ArrayList<>();

    public <T> EventRegistrations on(EventRegistration<T> registration) {
        registrations.add(registration);
        return this;
    }

    public <T> EventRegistrations on(Event<T> event, T listener) {
        return on(EventRegistration.create(event, listener));
    }

    public void register() {
        //#if FABRIC<1
        MinecraftForge.EVENT_BUS.register(this);
        //#endif
        //#if MC<10809
        //$$ FMLCommonHandler.instance().bus().register(this);
        //#endif
        for (EventRegistration<?> registration : registrations) {
            registration.register();
        }
    }

    public void unregister() {
        //#if FABRIC<1
        MinecraftForge.EVENT_BUS.unregister(this);
        //#endif
        //#if MC<10809
        //$$ FMLCommonHandler.instance().bus().unregister(this);
        //#endif
        for (EventRegistration<?> registration : registrations) {
            registration.unregister();
        }
    }
}
