package com.altnoir.mementoinabyss.twilight.worldgen;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TwilightBiomeDefinitions {
    public static final String STREAM = "stream";
    public static final Group WETLAND = new Group("fire_swamp", "swamp", "hydra_lair", "labyrinth");
    public static final Group HIGHLAND =
            new Group("glacier", "snowy_forest", "aurora_palace", "yeti_cave");
    public static final Group PLATEAU =
            new Group("final_plateau", "highlands", "final_castle", "troll_cave");
    public static final List<Group> WOODLAND =
            List.of(
                    new Group(
                            "dark_forest_center", "dark_forest", "dark_tower", "knight_stronghold"),
                    new Group(
                            "enchanted_forest", "firefly_forest", "quest_grove", "naga_courtyard"),
                    new Group(
                            "dense_mushroom_forest",
                            "mushroom_forest",
                            "mushroom_tower",
                            "naga_courtyard"),
                    new Group("forest", "dense_forest", "lich_tower", "naga_courtyard"));

    public static Stream<Group> groups() {
        return Stream.concat(Stream.of(HIGHLAND, WETLAND, PLATEAU), WOODLAND.stream());
    }

    public static Set<String> families() {
        return Stream.concat(
                        Stream.of(STREAM),
                        groups().flatMap(group -> Stream.of(group.primary(), group.companion())))
                .collect(Collectors.toUnmodifiableSet());
    }

    public record Group(String primary, String companion, String structure, String satellite) {}

    private TwilightBiomeDefinitions() {}
}
