package com.vetpetmon.realmengine.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntitySection; // adjust if names are mapped

@Mixin(PersistentEntitySectionManager.class)
public class MixinPersistentEntitySectionManager {
    // Redirect the call site where addEntityWithoutEvent (or the method that calls getOrCreateSection)
    // invokes EntitySectionStorage.getOrCreateSection(long)
    @Redirect(
            method = "addEntityWithoutEvent", // change to the actual target name if different; or use descriptor
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/EntitySectionStorage;getOrCreateSection(J)Lnet/minecraft/world/level/entity/EntitySection;"
            )
    )
    private EntitySection synchronizedGetOrCreate(EntitySectionStorage storage, long sectionPos) {
        synchronized (storage) {
            return storage.getOrCreateSection(sectionPos);
        }
    }
}