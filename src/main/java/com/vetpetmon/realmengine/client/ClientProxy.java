package com.vetpetmon.realmengine.client;

import com.vetpetmon.realmengine.common.CommonProxy;

public class ClientProxy extends CommonProxy {
    @Override
    public boolean isClient() {
        return true;
    }
}
