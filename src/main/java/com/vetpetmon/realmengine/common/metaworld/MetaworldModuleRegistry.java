package com.vetpetmon.realmengine.common.metaworld;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.*;

/**
 * Registry for managing metaworld modules. This is a LIBRARY, we will use these later.
 * This registry stores and manages all registered modules, tracking their tick intervals and events.
 */
@SuppressWarnings("unused")
public class MetaworldModuleRegistry {
    private static final MetaworldModuleRegistry INSTANCE = new MetaworldModuleRegistry();

    private final Map<String, IMetaworldModule> modules = new LinkedHashMap<>();
    private final Map<Integer, List<IMetaworldModule>> modulesByTickInterval = new HashMap<>();
    private IEventBus eventBus;

    private MetaworldModuleRegistry() {
    }

    /**
     * Gets the singleton instance of the registry.
     *
     * @return the singleton instance
     */
    public static MetaworldModuleRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Sets the event bus to use for posting module events.
     * This should be called once at mod initialization with the Forge event bus.
     *
     * @param eventBus the event bus to use
     */
    public void setEventBus(IEventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Registers a metaworld module.
     * The module will be added to the registry and grouped by its tick interval.
     *
     * @param module the module to register
     * @throws IllegalArgumentException if a module with the same ID is already registered
     * @throws IllegalStateException if no event bus has been set
     */
    public void register(IMetaworldModule module) {
        if (eventBus == null) {
            throw new IllegalStateException("Event bus must be set before registering modules. Call setEventBus() first.");
        }

        String moduleId = module.getModuleId();
        if (modules.containsKey(moduleId)) {
            throw new IllegalArgumentException("Module with ID '" + moduleId + "' is already registered");
        }

        modules.put(moduleId, module);
        int tickInterval = module.getTickInterval();
        modulesByTickInterval.computeIfAbsent(tickInterval, k -> new ArrayList<>()).add(module);

        module.onRegister();
    }

    /**
     * Unregisters all modules from the registry.
     * This calls onUnregister() for each module and clears all internal data structures.
     */
    public void unregisterAll() {
        for (IMetaworldModule module : modules.values()) {
            module.onUnregister();
        }
        modules.clear();
        modulesByTickInterval.clear();
    }

    /**
     * Unregisters a metaworld module.
     *
     * @param moduleId the ID of the module to unregister
     * @return true if the module was removed, false if it wasn't registered
     */
    public boolean unregister(String moduleId) {
        IMetaworldModule module = modules.remove(moduleId);
        if (module != null) {
            int tickInterval = module.getTickInterval();
            List<IMetaworldModule> intervalModules = modulesByTickInterval.get(tickInterval);
            if (intervalModules != null) {
                intervalModules.remove(module);
                if (intervalModules.isEmpty()) {
                    modulesByTickInterval.remove(tickInterval);
                }
            }
            module.onUnregister();
            return true;
        }
        return false;
    }

    /**
     * Gets a registered module by its ID.
     *
     * @param moduleId the ID of the module
     * @return the module, or null if not found
     */
    public IMetaworldModule getModule(String moduleId) {
        return modules.get(moduleId);
    }

    /**
     * Gets all registered modules.
     *
     * @return an unmodifiable collection of all registered modules
     */
    public Collection<IMetaworldModule> getAllModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    /**
     * Gets all modules with a specific tick interval.
     *
     * @param tickInterval the tick interval to query
     * @return a list of modules with the specified tick interval, or an empty list if none
     */
    public List<IMetaworldModule> getModulesByTickInterval(int tickInterval) {
        return modulesByTickInterval.getOrDefault(tickInterval, Collections.emptyList());
    }

    /**
     * Gets all unique tick intervals currently in use.
     *
     * @return a set of all tick intervals with registered modules
     */
    public Set<Integer> getAllTickIntervals() {
        return Collections.unmodifiableSet(modulesByTickInterval.keySet());
    }

    /**
     * Posts a tick event for a module.
     *
     * @param module the module to post the event for
     */
    public void postTickEvent(IMetaworldModule module) {
        Event event = module.getEventInstance();
        if (event != null) {
            eventBus.post(event);
        }
    }

    /**
     * Ticks the metaworld, posting events for modules whose intervals have elapsed.
     *
     * @param currentTick the current server tick
     */
    public void tick(long currentTick) {
        for (Map.Entry<Integer, List<IMetaworldModule>> entry : modulesByTickInterval.entrySet()) {
            int tickInterval = entry.getKey();

            // Skip interval 0 (on-demand only)
            if (tickInterval == 0) {
                continue;
            }

            // Check if it's time to tick these modules
            if (currentTick % tickInterval == 0) {
                for (IMetaworldModule module : entry.getValue()) {
                    postTickEvent(module);
                }
            }
        }
    }

    /**
     * Clears all registered modules.
     * This calls onUnregister() for each module.
     */
    public void clear() {
        for (IMetaworldModule module : modules.values()) {
            module.onUnregister();
        }
        modules.clear();
        modulesByTickInterval.clear();
    }

    /**
     * Checks if a module is registered.
     *
     * @param moduleId the ID of the module to check
     * @return true if the module is registered, false otherwise
     */
    public boolean isRegistered(String moduleId) {
        return modules.containsKey(moduleId);
    }

    /**
     * Gets the count of registered modules.
     *
     * @return the number of registered modules
     */
    public int getModuleCount() {
        return modules.size();
    }
}
