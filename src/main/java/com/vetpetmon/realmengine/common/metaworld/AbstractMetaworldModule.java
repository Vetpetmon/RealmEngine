package com.vetpetmon.realmengine.common.metaworld;

import net.minecraftforge.eventbus.api.Event;

/**
 * Abstract base class for implementing IMetaworldModule. This is a LIBRARY, we will use these later.
 * This provides sensible defaults for common module implementations.
 * Subclasses should override getEventClass() and getEventInstance() to provide custom event handling.
 */
@SuppressWarnings("unused")
public abstract class AbstractMetaworldModule implements IMetaworldModule {
    private final String moduleId;
    private final int tickInterval;

    /**
     * Creates a new abstract metaworld module.
     *
     * @param moduleId the unique identifier for this module
     * @param tickInterval the tick interval (0 for on-demand only, >0 for periodic ticking)
     */
    public AbstractMetaworldModule(String moduleId, int tickInterval) {
        this.moduleId = moduleId;
        this.tickInterval = tickInterval;
    }

    @Override
    public String getModuleId() {
        return moduleId;
    }

    @Override
    public int getTickInterval() {
        return tickInterval;
    }

    @Override
    public Class<? extends Event> getEventClass() {
        return MetaworldTick.class;
    }

    @Override
    public Event getEventInstance() {
        return new MetaworldTick(tickInterval, Metaworld.getTickCount());
    }

    @Override
    public void onRegister() {
        // Default: do nothing
    }

    @Override
    public void onUnregister() {
        // Default: do nothing
    }
}
