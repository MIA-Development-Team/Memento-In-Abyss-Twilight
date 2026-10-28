package com.altnoir.mementoinabyss.twilight.worldgen;

import com.altnoir.mementoinabyss.api.worldgen.Layer2BiomeGroup;
import com.altnoir.mementoinabyss.api.worldgen.Layer2Biomes;
import com.altnoir.mementoinabyss.api.worldgen.Layer2TerrainKind;
import com.altnoir.mementoinabyss.twilight.MementoInAbyssTwilight;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;

public final class TwilightBiomeDefinitions {
    public static final String STREAM = "stream";
    public static final Group WETLAND =
            new Group("fire_swamp", "swamp", "hydra_lair", "labyrinth", Layer2TerrainKind.WETLAND);
    public static final Group HIGHLAND =
            new Group(
                    "glacier",
                    "snowy_forest",
                    "aurora_palace",
                    "yeti_cave",
                    Layer2TerrainKind.HIGHLAND);
    public static final Group PLATEAU =
            new Group(
                    "final_plateau",
                    "highlands",
                    "final_castle",
                    "troll_cave",
                    Layer2TerrainKind.PLATEAU,
                    Optional.of("thornlands"),
                    64);
    public static final List<Group> WOODLAND =
            List.of(
                    new Group(
                            "dark_forest_center",
                            "dark_forest",
                            "dark_tower",
                            "knight_stronghold",
                            Layer2TerrainKind.WOODLAND),
                    new Group(
                            "enchanted_forest",
                            "firefly_forest",
                            "quest_grove",
                            "naga_courtyard",
                            Layer2TerrainKind.WOODLAND),
                    new Group(
                            "dense_mushroom_forest",
                            "mushroom_forest",
                            "mushroom_tower",
                            "naga_courtyard",
                            Layer2TerrainKind.WOODLAND),
                    new Group(
                            "forest",
                            "dense_forest",
                            "lich_tower",
                            "naga_courtyard",
                            Layer2TerrainKind.WOODLAND));

    private static final Map<ResourceKey<Layer2BiomeGroup>, Group> BY_KEY =
            groups().collect(Collectors.toUnmodifiableMap(Group::key, group -> group));

    public static Optional<Group> find(ResourceKey<Layer2BiomeGroup> key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }

    public static Stream<Group> groups() {
        return Stream.concat(Stream.of(HIGHLAND, WETLAND, PLATEAU), WOODLAND.stream());
    }

    public static Set<String> families() {
        return Stream.concat(
                        Stream.of(STREAM),
                        groups().flatMap(
                                        group ->
                                                Stream.concat(
                                                        Stream.of(
                                                                group.primary(), group.companion()),
                                                        group.transition().stream())))
                .collect(Collectors.toUnmodifiableSet());
    }

    public record Group(
            String primary,
            String companion,
            String structure,
            String satellite,
            Layer2TerrainKind terrainKind,
            Optional<String> transition,
            int transitionWidth) {
        public Group(
                String primary,
                String companion,
                String structure,
                String satellite,
                Layer2TerrainKind kind) {
            this(primary, companion, structure, satellite, kind, Optional.empty(), 64);
        }

        public ResourceKey<Layer2BiomeGroup> key() {
            return ResourceKey.create(
                    Layer2Biomes.REGISTRY_KEY, MementoInAbyssTwilight.asResource(primary));
        }
    }

    private TwilightBiomeDefinitions() {}
}
