package com.vetpetmon.realmengine.common.metaworld.example;

import com.vetpetmon.realmengine.common.metaworld.AbstractMetaworldModule;

/**
 * Example metaworld module demonstrating the module system.
 * This is a simple example that ticks every 20 ticks (1 second).
 *
 * This is a LIBRARY EXAMPLE, demonstrating how to use the metaworld system.
 */
@SuppressWarnings("unused")
public class ExampleMetaworldModule extends AbstractMetaworldModule {

    /**
     * Creates the example module with a tick interval of 20 (every second).
     */
    public ExampleMetaworldModule() {
        super("example_metaworld_module", 20);
    }

    @Override
    public void onRegister() {
        System.out.println("[Metaworld] Example module registered!");
    }

    @Override
    public void onUnregister() {
        System.out.println("[Metaworld] Example module unregistered!");
    }
}

