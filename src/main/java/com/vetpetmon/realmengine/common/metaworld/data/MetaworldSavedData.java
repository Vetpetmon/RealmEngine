package com.vetpetmon.realmengine.common.metaworld.data;

import com.vetpetmon.realmengine.common.metaworld.AbstractMetaworldModule;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Base class for metaworld modules that need to persist data to disk. This is a LIBRARY, we will use these later.
 * Combines the SavedData pattern with the metaworld module system.
 *
 * Subclasses should:
 * 1. Call the super constructor with an appropriate module ID
 * 2. Override load() to create an instance from NBT data
 * 3. Implement save() to serialize the module's state to NBT
 * 4. Register the module with Metaworld during mod initialization
 */
@SuppressWarnings("unused")
public abstract class MetaworldSavedData extends SavedData implements AutoCloseable {
    private final AbstractMetaworldModule moduleAdapter;

    /**
     * Creates a new metaworld saved data module.
     *
     * @param moduleId the unique identifier for this module
     * @param tickInterval the tick interval (0 for on-demand, >0 for periodic ticking)
     */
    public MetaworldSavedData(String moduleId, int tickInterval) {
        this.moduleAdapter = new ModuleAdapter(moduleId, tickInterval);
    }

    /**
     * Gets the module adapter for registering with the Metaworld system.
     *
     * @return the IMetaworldModule adapter
     */
    public AbstractMetaworldModule getModuleAdapter() {
        return moduleAdapter;
    }

    @Override
    public void close() {
        // Default: do nothing. Subclasses can override if needed.
    }

    /**
     * Internal adapter class to bridge MetaworldSavedData with IMetaworldModule.
     */
    private class ModuleAdapter extends AbstractMetaworldModule {
        public ModuleAdapter(String moduleId, int tickInterval) {
            super(moduleId, tickInterval);
        }

        @Override
        public void onRegister() {
            MetaworldSavedData.this.onRegister();
        }

        @Override
        public void onUnregister() {
            MetaworldSavedData.this.onUnregister();
            MetaworldSavedData.this.setDirty();
        }
    }

    /**
     * Called when this module is registered with the Metaworld system.
     * Override to perform initialization logic.
     */
    protected void onRegister() {
        // Default: do nothing
    }

    /**
     * Called when this module is unregistered from the Metaworld system.
     * Override to perform cleanup logic.
     */
    protected void onUnregister() {
        // Default: do nothing
    }

    /**
     * Marks this module's data as dirty, triggering a save on the next available opportunity.
     */
    @Override
    public void setDirty() {
        super.setDirty();
    }
}
