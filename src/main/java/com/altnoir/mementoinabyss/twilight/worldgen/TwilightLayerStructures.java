package com.altnoir.mementoinabyss.twilight.worldgen;

import com.altnoir.mementoinabyss.infrastructure.worldgen.TheAbyssChunkGenerator;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public final class TwilightLayerStructures {
    public static final ResourceKey<Structure> LABYRINTH =
            ResourceKey.create(
                    Registries.STRUCTURE,
                    Identifier.fromNamespaceAndPath("twilightforest", "labyrinth"));
    private static final TagKey<Structure> MAP_LANDMARKS =
            TagKey.create(
                    Registries.STRUCTURE,
                    Identifier.fromNamespaceAndPath("twilightforest", "landmark"));

    public static boolean applies(LevelAccessor level) {
        return level instanceof ServerLevel server
                && server.getChunkSource().getGenerator() instanceof TheAbyssChunkGenerator
                && AbyssTwilight.profile(server.getChunkSource().randomState()) != null;
    }

    public static Optional<ChunkPos> labyrinthChunk(LevelAccessor level, int x, int z) {
        var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(LABYRINTH);
        return structure
                .flatMap(
                        holder ->
                                findStart(
                                        level,
                                        candidate -> candidate == holder.value(),
                                        x,
                                        z,
                                        true))
                .map(StructureStart::getChunkPos);
    }

    public static Optional<StructureStart> findStart(
            LevelAccessor level, Predicate<Structure> filter, int x, int z, boolean checkReady) {
        if (checkReady && !level.hasChunk(x, z)) return Optional.empty();
        var chunk = level.getChunk(x, z, ChunkStatus.STRUCTURE_REFERENCES);
        for (var entry : chunk.getAllStarts().entrySet()) {
            if (filter.test(entry.getKey()) && entry.getValue().isValid())
                return Optional.of(entry.getValue());
        }
        // Boss home points belong to the actual structure footprint, not Twilight's old grid.
        for (var entry : chunk.getAllReferences().entrySet()) {
            if (!filter.test(entry.getKey())) continue;
            for (long reference : entry.getValue()) {
                int startX = ChunkPos.getX(reference), startZ = ChunkPos.getZ(reference);
                if (!level.hasChunk(startX, startZ)) continue;
                var start =
                        level.getChunk(startX, startZ, ChunkStatus.STRUCTURE_STARTS)
                                .getStartForStructure(entry.getKey());
                if (start != null && start.isValid()) return Optional.of(start);
            }
        }
        return Optional.empty();
    }

    public static Optional<ResourceKey<Structure>> loadedMapLandmark(
            LevelAccessor level, int blockX, int blockZ) {
        if (!(level instanceof ServerLevel server) || !applies(server)) return Optional.empty();
        int chunkX = blockX >> 4, chunkZ = blockZ >> 4;
        var chunk = server.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) return Optional.empty();
        var structures = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        ResourceKey<Structure> selected = null;
        // Map pixels cover whole chunks. Referenced footprint chunks must not add extra icons,
        // and map updates must never request generation or load an absent chunk from disk.
        for (var entry : chunk.getAllStarts().entrySet()) {
            var start = entry.getValue();
            if (!start.isValid()
                    || start.getChunkPos().x() != chunkX
                    || start.getChunkPos().z() != chunkZ
                    || !structures.wrapAsHolder(entry.getKey()).is(MAP_LANDMARKS)) continue;
            var key = structures.getResourceKey(entry.getKey()).orElse(null);
            if (key != null
                    && (selected == null
                            || key.identifier()
                                            .toString()
                                            .compareTo(selected.identifier().toString())
                                    < 0)) selected = key;
        }
        return Optional.ofNullable(selected);
    }

    private TwilightLayerStructures() {}
}
