package com.altnoir.mementoinabyss.twilight.worldgen;

import com.altnoir.mementoinabyss.api.worldgen.Layer2Biomes;
import com.altnoir.mementoinabyss.infrastructure.worldgen.TheAbyssChunkGenerator;
import com.altnoir.mementoinabyss.infrastructure.worldgen.TheAbyssChunkGenerator.GenerationContext;
import com.altnoir.mementoinabyss.infrastructure.worldgen.biome.TheAbyssBiomeSource;
import com.altnoir.mementoinabyss.infrastructure.worldgen.layer2.Layer2GeneratorView;
import com.altnoir.mementoinabyss.infrastructure.worldgen.layer2.Layer2WorldGenLevel;
import com.altnoir.mementoinabyss.twilight.MementoInAbyssTwilight;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;
import twilightforest.world.components.structures.placements.LandmarkGridPlacement;
import twilightforest.world.components.structures.type.HollowHillStructure;
import twilightforest.world.components.structures.util.LandmarkStructure;

final class TwilightLayerExtension implements TheAbyssChunkGenerator.Extension {
    private final ServerLevel level;
    private final TheAbyssBiomeSource source;
    private final TwilightLayerProfile profile;
    private final SurfaceRules.RuleSource surface;

    TwilightLayerExtension(
            ServerLevel level,
            TheAbyssBiomeSource source,
            TwilightLayerProfile profile,
            TheAbyssChunkGenerator generator) {
        this.level = level;
        this.source = source;
        this.profile = profile;
        surface =
                TwilightLayerSurfaces.withOriginal(
                        generator.generatorSettings().value().surfaceRule());
    }

    private static boolean twilight(Holder<Structure> structure) {
        return structure
                .unwrapKey()
                .orElseThrow()
                .identifier()
                .getNamespace()
                .equals("twilightforest");
    }

    @Override
    public boolean includeStructureSet(Holder<StructureSet> set) {
        return !(set.unwrapKey().orElseThrow().identifier().getNamespace().equals("twilightforest")
                && set.value().placement() instanceof LandmarkGridPlacement);
    }

    @Override
    public SurfaceRules.RuleSource surfaceRule(SurfaceRules.RuleSource original) {
        return surface;
    }

    @Override
    public DensityFunctions.BeardifierOrMarker structureTerrain(
            StructureManager structureManager, ChunkAccess chunk) {
        return TwilightLayerTerrain.forChunk(structureManager, chunk);
    }

    @Override
    public TheAbyssChunkGenerator.Scope structureScope(
            Holder<Structure> structure, boolean placing) {
        return twilight(structure)
                ? TwilightLayerGeneration.enter(profile)
                : TheAbyssChunkGenerator.Scope.NOOP;
    }

    @Override
    public Predicate<Holder<Biome>> validBiome(
            Holder<Structure> structure, Predicate<Holder<Biome>> original, ChunkPos candidate) {
        if (!twilight(structure)) return original;
        boolean allowed =
                original.test(
                        source.layer2SurfaceBiome(
                                candidate.getMiddleBlockX(), candidate.getMiddleBlockZ()));
        return biome -> allowed;
    }

    @Override
    public @Nullable GenerationContext generationContext(
            Holder<Structure> structure,
            ChunkAccess chunk,
            RandomState random,
            TheAbyssChunkGenerator generator) {
        if (!twilight(structure))
            return new TheAbyssChunkGenerator.GenerationContext(generator, chunk);
        var pos = chunk.getPos();
        int x = pos.getMiddleBlockX(), z = pos.getMiddleBlockZ();
        var selection = Layer2Biomes.query(level, x, z);
        var group = selection.group().flatMap(TwilightBiomeDefinitions::find).orElse(null);
        if (group == null
                || !(selection.zone().equals(Layer2Biomes.PRIMARY)
                        || selection.zone().equals(Layer2Biomes.COMPANIONS))) return null;
        String id = structure.unwrapKey().orElseThrow().identifier().getPath();
        if (structure.value() instanceof LandmarkStructure
                && !(structure.value() instanceof HollowHillStructure)) {
            String expected =
                    selection.zone().equals(Layer2Biomes.PRIMARY)
                            ? group.structure()
                            : group.satellite();
            if (!id.equals(expected)
                    && !(id.equals("giant_house") && "troll_cave".equals(expected))) return null;
        }
        var column = profile.column(x, z);
        if (!column.supported()) return null;
        if (TwilightLayerGeneration.isSurfaceStructure(id)
                && (column.height() < -114 || column.height() > -60)) return null;
        var view = new Layer2GeneratorView(generator, profile.terrain(), null, true, -112, -64);
        if (TwilightLayerGeneration.isSurfaceStructure(id)
                && view.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, chunk, random) < -112)
            return null;
        return new TheAbyssChunkGenerator.GenerationContext(view, chunk);
    }

    @Override
    public StructureStart adaptStart(
            Holder<Structure> structure,
            StructureStart start,
            ChunkAccess chunk,
            RandomState random) {
        if (!twilight(structure) || !start.isValid()) return start;
        var box = start.getBoundingBox();
        double x = Math.clamp(0, box.minX(), box.maxX()), z = Math.clamp(0, box.minZ(), box.maxZ());
        if (Math.hypot(x, z) < profile.openingRadius() + 48) return StructureStart.INVALID_START;
        return start;
    }

    @Override
    public TheAbyssChunkGenerator.PlacementContext structurePlacement(
            Holder<Structure> structure,
            StructureStart start,
            WorldGenLevel world,
            TheAbyssChunkGenerator generator) {
        if (!twilight(structure))
            return new TheAbyssChunkGenerator.PlacementContext(world, generator);
        var view = new Layer2WorldGenLevel(world, profile.terrain(), true, -112, -64);
        return new TheAbyssChunkGenerator.PlacementContext(
                view, new Layer2GeneratorView(generator, profile.terrain(), view, true, -112, -64));
    }

    @Override
    public TheAbyssChunkGenerator.PlacementContext featurePlacement(
            PlacedFeature feature, WorldGenLevel world, TheAbyssChunkGenerator generator) {
        var key = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getKey(feature);
        if (key == null || !key.getNamespace().equals(MementoInAbyssTwilight.ID))
            return new TheAbyssChunkGenerator.PlacementContext(world, generator);
        var view = new Layer2WorldGenLevel(world, profile.terrain(), false, -128, 0);
        return new TheAbyssChunkGenerator.PlacementContext(
                view, new Layer2GeneratorView(generator, profile.terrain(), view, false, -128, 0));
    }
}
