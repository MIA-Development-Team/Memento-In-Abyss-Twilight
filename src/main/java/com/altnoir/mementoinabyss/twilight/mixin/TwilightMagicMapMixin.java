package com.altnoir.mementoinabyss.twilight.mixin;

import com.altnoir.mementoinabyss.twilight.MementoInAbyssTwilight;
import com.altnoir.mementoinabyss.twilight.worldgen.TwilightLayerStructures;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "twilightforest.item.MagicMapItem")
public abstract class TwilightMagicMapMixin {
    @Unique private static final ResourceKey<Structure> mia$noLandmark =
            ResourceKey.create(
                    Registries.STRUCTURE,
                    MementoInAbyssTwilight.asResource("no_layer2_map_landmark"));

    @WrapOperation(
            method = "update",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ltwilightforest/util/landmarks/LegacyLandmarkPlacements;blockIsInLandmarkCenter(II)Z"))
    private boolean mia$useActualLandmarkChunk(
            int x, int z, Operation<Boolean> original, @Local(argsOnly = true) Level level) {
        return TwilightLayerStructures.applies(level)
                ? TwilightLayerStructures.loadedMapLandmark(level, x, z).isPresent()
                : original.call(x, z);
    }

    @WrapOperation(
            method = "update",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ltwilightforest/util/landmarks/LegacyLandmarkPlacements;pickLandmarkAtBlock(IILnet/minecraft/world/level/LevelReader;)Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<Structure> mia$useActualLandmarkId(
            int x,
            int z,
            LevelReader reader,
            Operation<ResourceKey<Structure>> original,
            @Local(argsOnly = true) Level level) {
        return TwilightLayerStructures.applies(level)
                ? TwilightLayerStructures.loadedMapLandmark(level, x, z).orElse(mia$noLandmark)
                : original.call(x, z, reader);
    }
}
