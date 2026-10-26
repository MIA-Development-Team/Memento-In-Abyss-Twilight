package com.altnoir.mementoinabyss.twilight.worldgen;

import com.altnoir.mementoinabyss.twilight.MementoInAbyssTwilight;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

public final class TwilightLayerSurfaces {
    private static final List<ResourceKey<Biome>> BIOMES =
            List.of(
                    key("forest"),
                    key("dense_forest"),
                    key("firefly_forest"),
                    key("clearing"),
                    key("oak_savannah"),
                    key("enchanted_forest"),
                    key("spooky_forest"),
                    key("mushroom_forest"),
                    key("dense_mushroom_forest"),
                    key("dark_forest"),
                    key("dark_forest_center"),
                    key("swamp"),
                    key("fire_swamp"),
                    key("highlands"),
                    key("thornlands"),
                    key("final_plateau"),
                    key("snowy_forest"),
                    key("glacier"),
                    key("lake"),
                    key("stream"),
                    key("underground"));

    @SuppressWarnings("unchecked")
    public static SurfaceRules.RuleSource withOriginal(SurfaceRules.RuleSource original) {
        var grass = SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState());
        var dirt = SurfaceRules.state(Blocks.DIRT.defaultBlockState());
        var sand = SurfaceRules.state(Blocks.SAND.defaultBlockState());
        var floor =
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                SurfaceRules.isBiome(key("lake"), key("stream")),
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.waterBlockCheck(-1, 0), grass),
                                        sand)),
                        SurfaceRules.ifTrue(
                                SurfaceRules.isBiome(key("glacier")),
                                SurfaceRules.state(Blocks.ICE.defaultBlockState())),
                        SurfaceRules.ifTrue(
                                SurfaceRules.isBiome(key("snowy_forest")),
                                SurfaceRules.state(Blocks.SNOW_BLOCK.defaultBlockState())),
                        SurfaceRules.ifTrue(
                                SurfaceRules.isBiome(key("highlands")),
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.noiseCondition(
                                                        Noises.SURFACE,
                                                        2.25 / 8.25,
                                                        Double.MAX_VALUE),
                                                SurfaceRules.state(
                                                        Blocks.COARSE_DIRT.defaultBlockState())),
                                        SurfaceRules.state(Blocks.PODZOL.defaultBlockState()))),
                        SurfaceRules.ifTrue(
                                SurfaceRules.waterBlockCheck(-1, 0),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.yBlockCheck(
                                                VerticalAnchor.absolute(
                                                        TwilightLayerProfile.SEA_LEVEL - 4),
                                                0),
                                        grass)),
                        dirt);
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(
                        SurfaceRules.isBiome(key("thornlands"), key("final_plateau")),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.ON_FLOOR, twilightBlock("weathered_deadrock")),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.UNDER_FLOOR,
                                        twilightBlock("cracked_deadrock")),
                                twilightBlock("deadrock"))),
                SurfaceRules.ifTrue(
                        SurfaceRules.isBiome(BIOMES.toArray(ResourceKey[]::new)),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, floor),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.UNDER_FLOOR,
                                        SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(
                                                        SurfaceRules.isBiome(key("glacier")),
                                                        SurfaceRules.state(
                                                                Blocks.PACKED_ICE
                                                                        .defaultBlockState())),
                                                dirt)))),
                original);
    }

    private static SurfaceRules.RuleSource twilightBlock(String name) {
        return SurfaceRules.state(
                BuiltInRegistries.BLOCK
                        .get(Identifier.fromNamespaceAndPath("twilightforest", name))
                        .orElseThrow()
                        .value()
                        .defaultBlockState());
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, MementoInAbyssTwilight.asResource(path));
    }

    private TwilightLayerSurfaces() {}
}
