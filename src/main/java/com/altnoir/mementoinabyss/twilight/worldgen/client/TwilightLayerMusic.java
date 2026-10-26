package com.altnoir.mementoinabyss.twilight.worldgen.client;

import com.altnoir.mementoinabyss.infrastructure.worldgen.dimension.MiaDimensions;
import com.altnoir.mementoinabyss.twilight.MementoInAbyssTwilight;
import com.altnoir.mementoinabyss.twilight.worldgen.AbyssTwilight;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = MementoInAbyssTwilight.ID)
public final class TwilightLayerMusic {
    private static final ResourceKey<DimensionType> TWILIGHT_TYPE =
            ResourceKey.create(
                    Registries.DIMENSION_TYPE,
                    Identifier.fromNamespaceAndPath("twilightforest", "twilight_forest_type"));

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void select(SelectMusicEvent event) {
        if (event.getMusic() != null) return;
        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        var camera = minecraft.gameRenderer.getMainCamera();
        if (level == null
                || player == null
                || !level.dimension().equals(MiaDimensions.THE_ABYSS_LEVEL)) return;
        var biome = level.getBiome(camera.blockPosition());
        if (!biome.is(AbyssTwilight.BIOMES)) return;
        level.registryAccess()
                .lookupOrThrow(Registries.DIMENSION_TYPE)
                .get(TWILIGHT_TYPE)
                .ifPresent(
                        type -> {
                            var music =
                                    type.value()
                                            .attributes()
                                            .applyModifier(
                                                    EnvironmentAttributes.BACKGROUND_MUSIC,
                                                    BackgroundMusic.EMPTY);
                            event.setMusic(
                                    music.select(
                                                    player.getAbilities().instabuild
                                                            && player.getAbilities().mayfly,
                                                    player.isUnderWater())
                                            .orElse(null));
                        });
    }

    private TwilightLayerMusic() {}
}
