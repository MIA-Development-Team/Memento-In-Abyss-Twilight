package com.altnoir.mementoinabyss.twilight.worldgen;

import com.altnoir.mementoinabyss.infrastructure.worldgen.TheAbyssChunkGenerator;
import com.altnoir.mementoinabyss.infrastructure.worldgen.biome.TheAbyssBiomeSource;
import com.altnoir.mementoinabyss.infrastructure.worldgen.layer2.Layer2Runtime;
import com.altnoir.mementoinabyss.twilight.MementoInAbyssTwilight;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

final class TwilightLayerIntegration {
    static void install(ServerLevel level, ChunkGenerator generator, RandomState random) {
        var terrain = Layer2Runtime.state(random);
        if (!(generator instanceof TheAbyssChunkGenerator abyssGenerator)
                || terrain == null
                || !(generator.getBiomeSource() instanceof TheAbyssBiomeSource nativeBiomes))
            return;
        var profile = new TwilightLayerProfile(terrain);
        var resolver =
                new TwilightLayerBiomeResolver(
                        nativeBiomes.definition(),
                        level.registryAccess().lookupOrThrow(Registries.BIOME));
        var source = nativeBiomes.withLayer2Resolver(resolver);
        Layer2Runtime.setBiomeSource(generator, source);
        abyssGenerator.setExtension(
                new TwilightLayerExtension(level, source, resolver, profile, abyssGenerator));
        AbyssTwilight.installed(random, profile);
        MementoInAbyssTwilight.LOGGER.info(
                "Twilight content bound to native layer 2 terrain regions");
    }

    private TwilightLayerIntegration() {}
}
