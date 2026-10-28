package com.altnoir.mementoinabyss.twilight.worldgen;

import com.altnoir.mementoinabyss.infrastructure.worldgen.placement.Layer2SurfacePlacement;
import com.altnoir.mementoinabyss.twilight.MementoInAbyssTwilight;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Layer2ChunkBlanketingPlacement extends PlacementModifier {
    public static final MapCodec<Layer2ChunkBlanketingPlacement> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.floatRange(0, 1)
                                                    .fieldOf("integrity")
                                                    .forGetter(p -> p.integrity),
                                            Heightmap.Types.CODEC
                                                    .fieldOf("heightmap")
                                                    .forGetter(p -> p.heightmap),
                                            RegistryCodecs.homogeneousList(Registries.BIOME)
                                                    .optionalFieldOf("biome_lock")
                                                    .forGetter(p -> p.biomes))
                                    .apply(i, Layer2ChunkBlanketingPlacement::new));
    private static final DeferredRegister<PlacementModifierType<?>> TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, MementoInAbyssTwilight.ID);
    private static final DeferredHolder<
                    PlacementModifierType<?>, PlacementModifierType<Layer2ChunkBlanketingPlacement>>
            TYPE = TYPES.register("layer2_chunk_blanketing", () -> () -> CODEC);

    private final float integrity;
    private final Heightmap.Types heightmap;
    private final Optional<HolderSet<Biome>> biomes;

    public Layer2ChunkBlanketingPlacement(
            float integrity, Heightmap.Types heightmap, Optional<HolderSet<Biome>> biomes) {
        this.integrity = integrity;
        this.heightmap = heightmap;
        this.biomes = biomes;
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    @Override
    public Stream<BlockPos> getPositions(
            PlacementContext context, RandomSource random, BlockPos origin) {
        var positions = Stream.<BlockPos>builder();
        int minX = origin.getX() & ~15, minZ = origin.getZ() & ~15;
        var scan = Layer2SurfacePlacement.of(heightmap);
        for (int dz = 0; dz < 16; dz++)
            for (int dx = 0; dx < 16; dx++) {
                if (random.nextFloat() > integrity) continue;
                scan.getPositions(
                                context, random, new BlockPos(minX + dx, origin.getY(), minZ + dz))
                        .filter(
                                p ->
                                        biomes.isEmpty()
                                                || biomes.orElseThrow()
                                                        .contains(context.getLevel().getBiome(p)))
                        .forEach(positions::add);
            }
        return positions.build();
    }

    @Override
    public PlacementModifierType<?> type() {
        return TYPE.get();
    }
}
