package com.vetpetmon.realmengine.common.effect;

/**
 * Interface for Mob Effect Instances to pause their durations via the {@code MobEffectInstancePause} Mixin by wrapping {@code tickDownDuration} so that it returns a non-decremented {@code duration} variable.
 * This is used to allow certain effect instances to pause their duration when a specific condition is met.
 * <br>
 * Example usage:
 * <pre>
 * {@code
 *     public void applyEffectTick(LivingEntity entity, int tick) {
 *         ((IPausableEffect)entity.getEffect(this))
 *         .realmengine$setDurationPaused(entity.hasEffect(ModEffects.GENE_SHUFFLE_PAUSE.get()));
 *         super.applyEffectTick(entity, tick);
 *     }
 * }
 * </pre>
 * <br>
 * This checks if the entity has another effect defined as {@code GENE_SHUFFLE_PAUSE} and sets the duration of the current effect to be paused or unpaused accordingly.
 * <br><br>
 * This doesn't interfere with any other effect instance, as they are unpaused by default, and stay that way.
 */
@SuppressWarnings("unused")
public interface IPausableEffect {
    /**
     * Set the duration of this effect to be paused. If true, the duration will not decrease.
     * Implementation is found in the MobEffectInstancePause mixin, which checks if the effect is paused based on the entity's effects.
     * @param paused true to pause the duration, false to unpause
     */
    void realmengine$setDurationPaused(boolean paused);
    /**
     * Get the duration of this effect to be paused. If true, the duration will not decrease.
     * Implementation is found in the MobEffectInstancePause mixin, which checks if the effect is paused based on the entity's effects.
     * @return true if the duration is paused, false otherwise
     */
    boolean realmengine$getDurationPaused();
}
