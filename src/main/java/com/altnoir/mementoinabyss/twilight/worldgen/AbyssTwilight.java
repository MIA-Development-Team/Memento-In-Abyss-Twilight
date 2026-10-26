package com.altnoir.mementoinabyss.twilight.worldgen;

import com.altnoir.mementoinabyss.infrastructure.worldgen.dimension.MiaDimensions;
import com.altnoir.mementoinabyss.twilight.MementoInAbyssTwilight;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.jspecify.annotations.Nullable;

public final class AbyssTwilight {
    public static final TagKey<Biome> BIOMES =
            TagKey.create(Registries.BIOME, MementoInAbyssTwilight.asResource("layer2"));
    private static final Map<RandomState, TwilightLayerProfile> PROFILES =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static void install(ServerLevel level, ChunkGenerator generator, RandomState state) {
        if (!level.dimension().equals(MiaDimensions.THE_ABYSS_LEVEL) || PROFILES.containsKey(state))
            return;
        TwilightLayerIntegration.install(level, generator, state);
    }

    public static void installed(RandomState state, TwilightLayerProfile profile) {
        PROFILES.put(state, profile);
    }

    public static @Nullable TwilightLayerProfile profile(RandomState state) {
        return PROFILES.get(state);
    }

    private AbyssTwilight() {}
}
