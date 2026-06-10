package com.vetpetmon.realmengine.common.metaworld;

import net.minecraftforge.eventbus.api.Event;

/**
 * Subscribed event to run on every tick of the metaworld.
 * Will tick metaworld elements that are subscribed to it, such as civilizations and world bosses.
 *
 * This is a generic base tick event that modules can extend or use directly.
 * Modules can override getEventInstance() in their IMetaworldModule implementation
 * to create custom event subclasses with additional data if needed.
 */
@SuppressWarnings("unused")
public class MetaworldTick extends Event {
    // Run this event after every nth tick
    private int tickInterval; // If set to 0, it means the event happens when called externally (on demand, like creation of a Civilization), not on a regular interval.
    private long currentTick; // The current server tick when this event is being posted

    public MetaworldTick() {
        this(0, 0);
    }

    public MetaworldTick(int tickInterval, long currentTick) {
        this.tickInterval = tickInterval;
        this.currentTick = currentTick;
    }

    public int getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(int tickInterval) {
        this.tickInterval = tickInterval;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public void setCurrentTick(long currentTick) {
        this.currentTick = currentTick;
    }
}
