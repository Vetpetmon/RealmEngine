package com.vetpetmon.realmengine.common.hitbox;

import com.vetpetmon.realmengine.RealmEngine;
import com.vetpetmon.realmengine.common.networking.EntityHitboxPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class EntityHitbox extends PartEntity<Mob> {

    // Enum for direction in radians.
    public enum Direction {
        WEST(0.0f),
        NORTH_WEST(0.8f),
        NORTH(1.6f),
        NORTH_EAST(2.4f),
        EAST(3.2f),
        SOUTH_EAST(-2.4f),
        SOUTH(-1.6f),
        SOUTH_WEST(-0.8f)
        ;

        private final float angle;

        Direction(float angle) {
            this.angle = angle;
        }

        public float getAngle() {
            return angle;
        }
    }


    private EntityDimensions size;
    public float scale = 1.0F;

    private float radius; // The distance from the parent entity's center to the hitbox's center
    private float angle; // -1.6 = behind, 0 = center, 1.6 = in front, 0.8 = right, -0.8 = left
    private final float dmgVuln, yPos; // dmgVuln is the multiplier for damage taken by the hitbox, yPos is the vertical offset from the parent entity's position
    private final Predicate<DamageSource> damageSourceImmunityPredicate; // A predicate to determine if the hitbox is immune to a specific damage source
    // Function to be executed when the hitbox is damaged, taking the source as input and returning the entity that was damaged
    private Function<DamageDataObject, Entity> onDamaged;
    // If this hitbox should be activated. If false, it will not update its position or allow collisions, and will functionally not be existent.
    private boolean activated = true;

    public EntityHitbox(Mob parent, float radius, float angle, float yOffset, float sizeXZ, float sizeY, float damageVulnerability, Predicate<DamageSource> damageSourceImmunityPredicate) {
        super(parent);
        this.blocksBuilding = true;
        this.size = EntityDimensions.scalable(sizeXZ, sizeY);
        this.refreshDimensions();
        this.radius = radius;
        this.angle = angle;
        this.yPos = yOffset;
        this.dmgVuln = damageVulnerability;
        this.damageSourceImmunityPredicate = damageSourceImmunityPredicate;
    }
    public EntityHitbox(Mob parent, float radius, float angle, float yOffset, float sizeXZ, float sizeY, float damageVulnerability) {
        this(parent, radius, angle, yOffset, sizeXZ, sizeY, damageVulnerability, null);
    }

    public record DamageDataObject(DamageSource source, float amount) { }

    // Add a setter for the onDamaged function
    public void setOnDamaged(Function<DamageDataObject, Entity> onDamaged) {
        this.onDamaged = onDamaged;
    }

    // Allow the angle to be changed dynamically
    public void setAngle(float angle) {this.angle = angle;}
    // Allow the radius to be changed dynamically
    public void setRadius(float radius) {this.radius = radius;}

    public boolean save(@NotNull CompoundTag tag) {return false;}

    public boolean canBeCollidedWith() {
        Mob parent = this.getParent();
        return parent != null && (parent.canBeCollidedWith() && parent.isAlive()); // Ensure the parent is alive before allowing collision
    }

    public boolean isInvulnerableTo(@NotNull DamageSource damageSource) {
        if (damageSourceImmunityPredicate != null && damageSourceImmunityPredicate.test(damageSource))
            return true;
        return super.isInvulnerableTo(damageSource) || damageSource.getEntity() != null && (this.getParent()).isPassengerOfSameVehicle(damageSource.getEntity());
    }

    public boolean is(@NotNull Entity entityIn) {return this == entityIn || this.getParent() == entityIn;}

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag p_20052_) {}

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag p_20139_) {}


    public boolean isPickable() {
        Mob parent = this.getParent();
        return parent != null && parent.isPickable();
    }

    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        Mob parent = this.getParent();
        if (parent == null) {
            return InteractionResult.PASS;
        } else {
            this.playSound(SoundEvents.ITEM_BREAK);
            if (player.level().isClientSide) {
                // Send EntityHitboxMessage to the server with the parent entity ID, player entity ID, and interaction type
                RealmEngine.sendPacketToServer(new EntityHitboxPacket(parent.getId(), player.getId(), 0, 0.0F));



            }

            return parent.interact(player, hand);
        }
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean isActivated() {
        return activated;
    }

    public boolean hurt(@NotNull DamageSource source, float amount) {
        Mob parent = this.getParent();

        if (!this.isInvulnerableTo(source) && parent != null) {
            Entity player = source.getEntity();
            if (player != null && !parent.isAlliedTo(player) && player.level().isClientSide)
                RealmEngine.sendPacketToServer(new EntityHitboxPacket(parent.getId(), player.getId(), 1, amount * this.dmgVuln));
            parent.hurt(parent.damageSources().generic(),amount * this.dmgVuln);
            // Run the onDamaged function if it's set
            if (onDamaged != null && !parent.level().isClientSide)
                onDamaged.apply(new DamageDataObject(source, amount * this.dmgVuln));
            return true;
        }

        return false;
    }


    public void setPosCenteredY(Vec3 pos) {this.setPos(pos.x, pos.y - (double)(this.getBbHeight()), pos.z);}

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {throw new UnsupportedOperationException();}

    public boolean fireImmune() {return true;}

    public @NotNull AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(2.0F, 0.5F, 2.0F);
    }

    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        Mob parent = this.getParent();
        return parent == null ? this.size : this.size.scale(parent.getScale());
    }

    @Override
    public boolean shouldBeSaved() {return false;}

    //Updates positions of the hitbox
    public void updatePosition() {
        Mob parent = this.getParent();
        if (parent != null) {
            if (parent.isAlive() && this.isActivated())
                this.setPos(
                        parent.getX() + this.radius
                                * Math.cos(this.getParent().yBodyRot * (Math.PI / 180.0F) + this.angle),
                        parent.getY() + this.yPos,
                        parent.getZ() + this.radius
                                * Math.sin(this.getParent().yBodyRot * (Math.PI / 180.0F) + this.angle)
                );
            else {
                // resize to 0 to prevent collisions if the parent is dead
                this.size = EntityDimensions.scalable(0.0F, 0.0F);
                this.refreshDimensions();
            }
        }
    }

}
