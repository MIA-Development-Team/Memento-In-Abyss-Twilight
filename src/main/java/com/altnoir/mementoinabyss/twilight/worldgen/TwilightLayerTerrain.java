package com.altnoir.mementoinabyss.twilight.worldgen;

import java.util.ArrayList;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import twilightforest.world.components.chunkgenerators.CustomTerrainBeardifier;
import twilightforest.world.components.structures.CustomDensitySource;
import twilightforest.world.components.structures.type.NagaCourtyardStructure;

final class TwilightLayerTerrain {
    static DensityFunctions.BeardifierOrMarker forChunk(
            StructureManager structureManager, ChunkAccess chunk) {
        var vanilla = Beardifier.forStructuresInChunk(structureManager, chunk.getPos());
        var starts =
                structureManager.startsForStructure(
                        chunk.getPos(), structure -> structure instanceof CustomDensitySource);
        if (starts.isEmpty()) return vanilla;
        var adjustments = new ArrayList<DensityFunction>(starts.size());
        for (var start : starts) {
            if (!(start.getStructure() instanceof CustomDensitySource source)) continue;
            var terrain = source.getStructureTerraformer(chunk.getPos(), start);
            if (source instanceof NagaCourtyardStructure) {
                int top =
                        start.getPieces().stream()
                                .mapToInt(piece -> piece.getBoundingBox().maxY())
                                .max()
                                .orElseThrow();
                // Twilight clears an outdoor courtyard up to the sky; a cave has a separate roof.
                terrain =
                        DensityFunctions.mul(
                                terrain,
                                DensityFunctions.yClampedGradient(top + 8, top + 24, 1, 0));
            }
            adjustments.add(terrain);
        }
        return adjustments.isEmpty() ? vanilla : new CustomTerrainBeardifier(vanilla, adjustments);
    }

    private TwilightLayerTerrain() {}
}
