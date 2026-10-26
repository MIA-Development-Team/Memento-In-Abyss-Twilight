package com.altnoir.mementoinabyss.twilight.worldgen;

import com.altnoir.mementoinabyss.infrastructure.worldgen.TheAbyssChunkGenerator;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class TwilightLayerGeneration {
    public static final int MIN_SURFACE_STRUCTURE_Y = -112;
    private static final Set<String> UNDERGROUND =
            Set.of(
                    "labyrinth",
                    "knight_stronghold",
                    "troll_cave",
                    "small_hollow_hill",
                    "medium_hollow_hill",
                    "large_hollow_hill");
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    public static @Nullable Scope current() {
        return CURRENT.get();
    }

    public static Scope enter(TwilightLayerProfile profile) {
        return new Scope(profile);
    }

    public static boolean isSurfaceStructure(String path) {
        return !UNDERGROUND.contains(path);
    }

    public static final class Scope implements TheAbyssChunkGenerator.Scope {
        private final @Nullable Scope previous;
        private final TwilightLayerProfile profile;

        private Scope(TwilightLayerProfile profile) {
            this.profile = profile;
            previous = CURRENT.get();
            CURRENT.set(this);
        }

        public TwilightLayerProfile profile() {
            return profile;
        }

        @Override
        public void close() {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    private TwilightLayerGeneration() {}
}
