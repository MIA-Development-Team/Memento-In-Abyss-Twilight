package com.altnoir.mementoinabyss.twilight.worldgen;

import com.altnoir.mementoinabyss.api.worldgen.Layer2BiomeResolver;
import com.altnoir.mementoinabyss.infrastructure.worldgen.biome.TheAbyssBiomeSource;
import com.altnoir.mementoinabyss.infrastructure.worldgen.density.Layer2Layout;
import com.altnoir.mementoinabyss.twilight.MementoInAbyssTwilight;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

public final class TwilightLayerBiomeResolver implements Layer2BiomeResolver {
    private final TheAbyssBiomeSource.Definition definition;
    private final List<Group> woodland;
    private final List<List<Group>> nativeGroups;
    private final Group wetland, highland, plateau;
    private final Holder<Biome> stream;

    public TwilightLayerBiomeResolver(
            TheAbyssBiomeSource.Definition definition, HolderGetter<Biome> biomes) {
        this.definition = definition;
        nativeGroups =
                definition.layer2Groups().stream()
                        .map(
                                g ->
                                        g.companions().stream()
                                                .map(c -> new Group(g.primary(), c, null, null))
                                                .toList())
                        .toList();
        wetland = bind(biomes, TwilightBiomeDefinitions.WETLAND);
        highland = bind(biomes, TwilightBiomeDefinitions.HIGHLAND);
        plateau = bind(biomes, TwilightBiomeDefinitions.PLATEAU);
        woodland =
                TwilightBiomeDefinitions.WOODLAND.stream()
                        .map(group -> bind(biomes, group))
                        .toList();
        stream = biomes.getOrThrow(key(TwilightBiomeDefinitions.STREAM));
    }

    private static Group bind(HolderGetter<Biome> biomes, TwilightBiomeDefinitions.Group group) {
        return new Group(
                biomes.getOrThrow(key(group.primary())),
                biomes.getOrThrow(key(group.companion())),
                group.structure(),
                group.satellite());
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, MementoInAbyssTwilight.asResource(path));
    }

    public Group group(Layer2Layout.Region region) {
        return switch (Layer2Layout.terrainKind(region)) {
            case WETLAND -> wetland;
            case HIGHLAND -> highland;
            case PLATEAU -> plateau;
            case WOODLAND -> {
                int index =
                        Math.floorMod(
                                region.id() >>> 7,
                                woodland.size() + definition.layer2Groups().size());
                if (index < definition.layer2Groups().size()) {
                    yield nativeGroups.get(index).get(region.sector());
                }
                yield woodland.get(index - definition.layer2Groups().size());
            }
        };
    }

    @Override
    public Holder<Biome> resolve(Layer2Layout.Region region, boolean river) {
        var group = group(region);
        if (river) return group.structure() == null ? definition.layer2River() : stream;
        return region.primary() ? group.primary() : group.companion();
    }

    @Override
    public Stream<Holder<Biome>> possibleBiomes() {
        return Stream.concat(
                        Stream.concat(
                                Stream.of(definition.layer2River()),
                                definition.layer2Groups().stream()
                                        .flatMap(
                                                g ->
                                                        Stream.concat(
                                                                Stream.of(g.primary()),
                                                                g.companions().stream()))),
                        Stream.concat(
                                Stream.of(stream),
                                Stream.concat(
                                                woodland.stream(),
                                                Stream.of(wetland, highland, plateau))
                                        .flatMap(g -> Stream.of(g.primary(), g.companion()))))
                .distinct();
    }

    public record Group(
            Holder<Biome> primary,
            Holder<Biome> companion,
            @Nullable String structure,
            @Nullable String satellite) {}
}
