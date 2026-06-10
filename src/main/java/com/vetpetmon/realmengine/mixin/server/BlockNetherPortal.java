package com.vetpetmon.realmengine.mixin.server;

import com.vetpetmon.realmengine.common.CommonConfig;import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mixin to prevent entities (players or otherwise) from using nether portals by
// cancelling the original entityInside behavior.
@Mixin(value = NetherPortalBlock.class)
public class BlockNetherPortal {

    @Inject(method = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), cancellable = true)
    private void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        // Cancel the original method so entities won't handle portal logic.
        if (CommonConfig.disableNetherPortal.get()){
            ci.cancel();
        } // Else, if the config option is disabled, we allow portals to work as normal.

    }

}
