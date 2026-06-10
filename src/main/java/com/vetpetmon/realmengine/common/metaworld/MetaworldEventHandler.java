package com.vetpetmon.realmengine.common.metaworld;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server event handler for the Metaworld system.
 * This handler subscribes to server tick events and calls the Metaworld tick method.
 *
 * This class should be registered to the Forge event bus during mod initialization.
 */
@Mod.EventBusSubscriber(modid = "realmengine", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MetaworldEventHandler {

    /**
     * Called every server tick. Posts tick events for registered metaworld modules
     * that are due for their next tick based on their tick intervals.
     *
     * @param event the tick event
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // Only tick on the end phase to ensure all other game logic runs first
        if (event.phase == TickEvent.Phase.END) {
            Metaworld.tick();
        }
    }

    /**
     * On server close, unregister all modules to clean up resources and prevent crashes.
     * Most relevant for integrated servers.
     */
    @SubscribeEvent
    public static void onServerClose(ServerStoppedEvent event) {
        Metaworld.getRegistry().unregisterAll();
    }
}

