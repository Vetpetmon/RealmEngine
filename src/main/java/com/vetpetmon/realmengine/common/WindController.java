package com.vetpetmon.realmengine.common;

import com.vetpetmon.realmengine.RealmEngine;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = RealmEngine.MODID)
public class WindController {
    private static final Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static int currentStrength = 1;
    private static Direction currentDirection = Direction.NORTH;

    public static Direction getCurrentDirection() {
        return currentDirection;
    }

    public static void setCurrentDirection(Direction currentDirection) {
        WindController.currentDirection = currentDirection;
    }

    public static void setCurrentStrength(int currentStrength) {
        WindController.currentStrength = currentStrength;
    }
    public static int getCurrentStrength() {
        return currentStrength;
    }

    // randomly change wind direction and strength
    public static void randomizeWind(RandomSource random) {
        // Randomly pick a direction
        setCurrentDirection(directions[random.nextInt(directions.length)]);
        // Randomly pick a strength between 0 and 5
        alternateWindStrength(random);
    }

    // Alternate wind strength by 1 unit randomly
    public static void alternateWindStrength(RandomSource random) {
        if (random.nextBoolean()) // Increase strength by 1, max 3
            setCurrentStrength(Math.min(currentStrength + 1, 3));
        else // Decrease strength by 1, min 1
            setCurrentStrength(Math.max(currentStrength - 1, 1));
    }

    // Subscribe to server ticks and randomly change wind every 6000 ticks (5 minutes)
    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide())
            if (event.getLevel().getGameTime() % 6000 == 0) randomizeWind(event.getLevel().getRandom());
    }
}
