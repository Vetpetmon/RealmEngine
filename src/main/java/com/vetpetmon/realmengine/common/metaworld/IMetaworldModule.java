package com.vetpetmon.realmengine.common.metaworld;

import net.minecraftforge.eventbus.api.Event;

/**
 * Interface that defines the contract for a Metaworld module. This is a LIBRARY, we will use these later.
 * Modules implementing this interface can be registered with the Metaworld system
 * and will receive tick events based on their configured tick intervals.
 */
@SuppressWarnings("unused")
public interface IMetaworldModule {
    /**
     * Gets the unique identifier for this module.
     * This is used as the key in the module registry.
     *
     * @return the unique identifier for this module
     */
    String getModuleId();

    /**
     * Gets the tick interval for this module.
     * The module's event will be posted every N ticks.
     * A value of 0 means the event is only posted on-demand (e.g., when the module is created).
     * A value of 1 means every tick.
     * A value of 20 means every second (assuming 20 ticks per second).
     *
     * @return the tick interval
     */
    int getTickInterval();

    /**
     * Gets the event class that this module uses for ticking.
     * This event will be posted to the Forge event bus at the specified tick interval.
     *
     * @return the event class to post for this module
     */
    Class<? extends Event> getEventClass();

    /**
     * Gets the event instance to be posted to the event bus.
     * Called every N ticks (where N is the tick interval).
     *
     * @return a new or reusable event instance ready to be posted
     */
    Event getEventInstance();

    /**
     * Called when the module is registered with the Metaworld system.
     */
    void onRegister();

    /**
     * Called when the module is unregistered from the Metaworld system.
     */
    void onUnregister();
}
