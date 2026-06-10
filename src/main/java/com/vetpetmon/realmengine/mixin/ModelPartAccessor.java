package com.vetpetmon.realmengine.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.model.geom.ModelPart.class)
public interface ModelPartAccessor {
    @Accessor
    List<ModelPart.Cube> getCubes();
}
