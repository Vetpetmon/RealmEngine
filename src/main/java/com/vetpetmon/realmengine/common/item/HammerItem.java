package com.vetpetmon.realmengine.common.item;

import com.vetpetmon.realmengine.RealmEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

public class HammerItem extends PickaxeItem {

    protected final TagKey<Block> blocks;
    private final int breakRange;

    /**
     * Creates a Hammer object that extends from the Pickaxe Object. Is able to mine in a 3x3 square.
     * @param tier The Tier of the tool
     * @param attackBonus Extra damage applied to item properties
     * @param attackSpeedModifier Speed modifier to apply to the item (Only affects attack speed)
     * @param properties Item Properties
     * @param breakRange The extra range, in blocks, to break, if the player is not sneaking. Can be omitted.
     */
    public HammerItem(Tier tier, int attackBonus, float attackSpeedModifier, Properties properties, int breakRange) {
        super(tier, attackBonus, attackSpeedModifier, properties);
        this.breakRange = breakRange;
        this.blocks = BlockTags.MINEABLE_WITH_PICKAXE;
    }
    public HammerItem(Tier tier, int attackBonus, float attackSpeedModifier, Properties properties) {
        this(tier, attackBonus, attackSpeedModifier, properties,0);
    }


    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity living, LivingEntity entity) {
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,3));
        return super.hurtEnemy(stack, living, entity);
    }


    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity living) {
        if (state.getDestroySpeed(level, pos) != 0.0F)
            if (canMassBreak(living)){
                for (BlockPos blockPos : getBlocksToBeDestroyed(1+this.breakRange,pos,living))
                    if (level.getBlockState(blockPos).is(blocks))
                        level.destroyBlock(blockPos,true,living);
            }
        return super.mineBlock(stack, level, state, pos, living);
    }

    public float getDestroySpeed(ItemStack stack, BlockState state) {return state.is(this.blocks) ? this.speed : 1.0F;}

    public boolean canMassBreak(LivingEntity living) {return !living.isCrouching();}

    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {return state.is(this.blocks);}

    public static List<BlockPos> getBlocksToBeDestroyed(int range, BlockPos initalBlockPos, LivingEntity player)
    {
        List<BlockPos> positions = new ArrayList<>();
        BlockHitResult traceResult = player.level().clip(new ClipContext(player.getEyePosition(1f),
                (player.getEyePosition(1f).add(player.getViewVector(1f).scale(6f))),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if(traceResult.getType() == HitResult.Type.MISS) return positions;

        for(int x = -range; x <= range; x++) for(int y = -range; y <= range; y++){
            switch (traceResult.getDirection().ordinal()){
                case 0,1-> positions.add(new BlockPos(initalBlockPos.getX() + x,    initalBlockPos.getY(),      initalBlockPos.getZ() + y));
                case 2,3-> positions.add(new BlockPos(initalBlockPos.getX() + x,    initalBlockPos.getY() + y,  initalBlockPos.getZ()));
                case 4,5-> positions.add(new BlockPos(initalBlockPos.getX(),        initalBlockPos.getY() + y,  initalBlockPos.getZ() + x));
            }
        }

        return positions;
    }
}
