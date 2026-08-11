package com.vetpetmon.realmengine.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.vetpetmon.realmengine.common.effect.IPausableEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MobEffectInstance.class)
public class MobEffectInstancePause implements IPausableEffect {
    @Shadow private int duration;

    /**
     * Wraps the tickDownDuration method to check if the duration is paused. If it is, the duration will not decrease.
     * @param original the original tickDownDuration method
     * @return the original duration if not paused, otherwise the current duration
     */
    @WrapMethod(method = "tickDownDuration" )
    int realmengine$wrapTickDownDuration(Operation<Integer> original) {
        return (!this.realmengine$getDurationPaused()) ? original.call() : this.duration;
    }

    @Unique boolean realmengine$durationPaused = false;

    @Override
    public void realmengine$setDurationPaused(boolean paused) {this.realmengine$durationPaused = paused;}

    @Override
    public boolean realmengine$getDurationPaused() {return this.realmengine$durationPaused;}
}

