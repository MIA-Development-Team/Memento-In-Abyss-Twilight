package com.altnoir.mementoinabyss.twilight.worldgen;

import com.altnoir.mementoinabyss.infrastructure.worldgen.layer2.Layer2Runtime;

/** Metadata view only; Twilight never re-samples or replaces the native terrain graph. */
public final class TwilightLayerProfile {
    public static final int SEA_LEVEL = -112;
    public static final int MIN_Y = -128;
    public static final int FLOOR_Y = -128;
    public static final int ROOF_Y = 1;
    private final Layer2Runtime.State terrain;

    public TwilightLayerProfile(Layer2Runtime.State terrain) {
        this.terrain = terrain;
    }

    public Column column(int x, int z) {
        double height = terrain.surfaceHeight(x, z), roof = terrain.roofHeight(x, z);
        double weight = Math.hypot(x, z) >= openingRadius() + 48 ? 1 : 0;
        boolean supported =
                weight == 1
                        && !terrain.pillarCore(x, z)
                        && terrain.layout().sample(x, z).edgeDistance()
                                > terrain.layout().riverBankDistance() + 16
                        && roof - height >= 24;
        return new Column(height, roof, weight, supported);
    }

    public boolean contains(int x, int z) {
        return Math.hypot(x, z) >= openingRadius() + 48;
    }

    public Layer2Runtime.State terrain() {
        return terrain;
    }

    public double openingRadius() {
        return terrain.layout().openingRadius();
    }

    public record Column(double height, double roof, double weight, boolean supported) {}
}
