package com.vetpetmon.realmengine.common.metaworld;

import net.minecraft.nbt.CompoundTag;

@SuppressWarnings("unused")
public class WorldBoss {
    // Spheres of influence determine how many layers of effect the boss has based on distance
    private int spheresOfInfluence;
    // Each sphere radius step determines the distance between each sphere of influence
    private int sphereRadiusSteps;
    // Current position, in X and Z as metaworld is 2D
    private int x, z;
    // Name of the world boss
    private final String name;
    private final int effect;

    public WorldBoss(String name, int spheresOfInfluence, int sphereRadiusSteps, int x, int z, int effect) {
        this.name = name;
        this.spheresOfInfluence = spheresOfInfluence;
        this.sphereRadiusSteps = sphereRadiusSteps;
        this.x = x;
        this.z = z;
        this.effect = effect;
    }

    public int getEffect() {
        return this.effect;
    }

    public int getSphereRadiusSteps() {
        return sphereRadiusSteps;
    }

    public int getSpheresOfInfluence() {
        return spheresOfInfluence;
    }

    public void setSphereRadiusSteps(int sphereRadiusSteps) {
        this.sphereRadiusSteps = sphereRadiusSteps;
    }

    public void setSpheresOfInfluence(int spheresOfInfluence) {
        this.spheresOfInfluence = spheresOfInfluence;
    }

    // Get the radius of a specific sphere of influence
    // sphereIndex is 0-based, 0 would mean the first sphere
    public int getSphereRadius(int sphereIndex) {
        if (sphereIndex < 0 || sphereIndex >= spheresOfInfluence) {
            throw new IllegalArgumentException("Invalid sphere index");
        }
        // Avoid multiplying by 0
        return sphereRadiusSteps * (sphereIndex + 1);
    }

    // Get the position
    public int getX() {
        return x;
    }
    public int getZ() {
        return z;
    }

    // Set the position
    public void setPosition(int x, int z) {
        this.x = x;
        this.z = z;
    }

    // Move the position
    public void movePosition(int deltaX, int deltaZ) {
        this.x += deltaX;
        this.z += deltaZ;
    }

    public void setX(int x) {
        this.x = x;
    }
    public void setZ(int z) {
        this.z = z;
    }

    public String getName() {
        return name;
    }

    // Get distance from boss to a point
    public double distanceToPoint(int px, int pz) {
        int dx = px - x;
        int dz = pz - z;
        return Math.sqrt(dx * dx + dz * dz);
    }


    // Check distance from boss to a point, return true if within any sphere of influence
    public boolean influencesPoint(int px, int pz) {
        // use distanceToPoint to get distance
        double distance = distanceToPoint(px, pz);
        // check against the outermost sphere radius
        int outerRadius = getSphereRadius(spheresOfInfluence - 1);
        return distance <= outerRadius;
    }
    // get the highest sphere index that influences the point, or -1 if none
    public int getInfluencingSphereIndex(int px, int pz) {
        double distance = distanceToPoint(px, pz);
        // check from inner to outer spheres
        for (int i = 0; i < spheresOfInfluence; i++) {
            int radius = getSphereRadius(i);
            if (distance <= radius)
                return i;
        }
        return -1;
    }

    // get the influence level at a given point (1-based), or 0 if none
    public int getInfluenceLevelAtPoint(int px, int pz) {
        int sphereIndex = getInfluencingSphereIndex(px, pz);
        return sphereIndex == -1 ? 0 : sphereIndex + 1;
    }

    // Save to NBT
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putInt("x", x);
        tag.putInt("z", z);
        tag.putInt("steps", sphereRadiusSteps);
        tag.putInt("spheres", spheresOfInfluence);
        tag.putInt("effect", effect);
        return tag;
    }
    // Load from NBT
    public WorldBoss fromNBT(CompoundTag tag) {
        return new WorldBoss(
                tag.getString("name"),
                tag.getInt("spheres"),
                tag.getInt("steps"),
                tag.getInt("x"),
                tag.getInt("z"),
                tag.getInt("effect")
        );
    }


}
