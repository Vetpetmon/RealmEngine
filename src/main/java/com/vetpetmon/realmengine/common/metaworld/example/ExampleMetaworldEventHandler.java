package com.vetpetmon.realmengine.common.metaworld.example;

import com.vetpetmon.realmengine.common.metaworld.MetaworldTick;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Example event handler for the example metaworld module.
 * Demonstrates how to subscribe to metaworld tick events.
 *
 * This is a LIBRARY EXAMPLE, showing how modules receive and process tick events.
 */
@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = "realmengine", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExampleMetaworldEventHandler {

    /**
     * Handles metaworld tick events for the example module.
     * This will be called every 20 ticks (1 second) based on the module configuration.
     *
     * @param event the metaworld tick event
     */
    @SubscribeEvent
    public static void onMetaworldTick(MetaworldTick event) {
        // You can use getCurrentTick() to get the current server tick when this event was posted
        long tick = event.getCurrentTick();
        // Do something interesting here based on the tick interval
        // Example: Update civilizations, move world bosses, apply global effects, etc.
    }
}

