package com.vetpetmon.realmengine.common.metaworld;

import net.minecraftforge.eventbus.api.IEventBus;

/**
 * The metaworld is a system that manages background events in the level. No chunkloading is required, as the metaworld is not tied to any specific location.
 * Ticks every tick. This is a LIBRARY, we will use these later.
 *
 * This class provides the main interface for managing the metaworld system.
 * It initializes the module registry and coordinates the ticking of registered modules.
 */
@SuppressWarnings("unused")
public class Metaworld {
    private static final MetaworldModuleRegistry registry = MetaworldModuleRegistry.getInstance();
    private static long tickCount = 0;

    /**
     * Initializes the Metaworld system with the provided event bus.
     * This should be called once during mod initialization.
     *
     * @param eventBus the Forge event bus to use for posting module events
     */
    public static void initialize(IEventBus eventBus) {
        registry.setEventBus(eventBus);
    }

    /**
     * Registers a metaworld module.
     * The module will receive tick events based on its configured tick interval.
     *
     * @param module the module to register
     * @throws IllegalArgumentException if a module with the same ID is already registered
     * @throws IllegalStateException if the Metaworld has not been initialized
     */
    public static void registerModule(IMetaworldModule module) {
        registry.register(module);
    }

    /**
     * Unregisters a metaworld module.
     *
     * @param moduleId the ID of the module to unregister
     * @return true if the module was removed, false if it wasn't registered
     */
    public static boolean unregisterModule(String moduleId) {
        return registry.unregister(moduleId);
    }

    /**
     * Gets the module registry.
     * Advanced users can use this for direct registry access.
     *
     * @return the metaworld module registry
     */
    public static MetaworldModuleRegistry getRegistry() {
        return registry;
    }

    /**
     * Gets the current tick count of the metaworld.
     *
     * @return the current tick number
     */
    public static long getTickCount() {
        return tickCount;
    }

    /**
     * Ticks the metaworld. This should be called every server tick.
     * This method posts tick events to all registered modules whose tick intervals have elapsed.
     * This method is called by the MetaworldEventHandler automatically.
     */
    static void tick() {
        registry.tick(tickCount);
        tickCount++;
    }

    /**
     * Posts an on-demand tick event for a specific module.
     * This is useful for modules with a tick interval of 0 (on-demand only).
     *
     * @param moduleId the ID of the module to tick
     */
    public static void tickModuleOnDemand(String moduleId) {
        IMetaworldModule module = registry.getModule(moduleId);
        if (module != null) {
            registry.postTickEvent(module);
        }
    }
}
