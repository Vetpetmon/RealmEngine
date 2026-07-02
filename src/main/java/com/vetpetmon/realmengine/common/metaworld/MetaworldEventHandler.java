package com.vetpetmon.realmengine.common.metaworld;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Server event handler for the Metaworld system.
 * This handler subscribes to server tick events and calls the Metaworld tick method.
 *
 * This class should be registered to the Forge event bus during mod initialization.
 */
@EventBusSubscriber(modid = "realmengine")
public class MetaworldEventHandler {

    /**
     * Called every server tick. Posts tick events for registered metaworld modules
     * that are due for their next tick based on their tick intervals.
     *
     * @param event the tick event
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Only tick on the end phase to ensure all other game logic runs first
        {
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

