package com.altnoir.mementoinabyss.twilight.mixin;

import com.altnoir.mementoinabyss.twilight.worldgen.TwilightLayerStructures;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "twilightforest.item.mapdata.TFMazeMapData")
public abstract class TwilightMazeMapMixin {
    @WrapOperation(
            method = "calculateMapCenter",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ltwilightforest/util/landmarks/LegacyLandmarkPlacements;pickLandmarkForChunk(IILnet/minecraft/world/level/LevelReader;)Lnet/minecraft/resources/ResourceKey;"))
    private @Nullable ResourceKey<Structure> mia$recognizeNativeMaze(
            int x,
            int z,
            LevelReader reader,
            Operation<ResourceKey<Structure>> original,
            @Local(argsOnly = true) Level level) {
        if (!TwilightLayerStructures.applies(level)) return original.call(x, z, reader);
        return TwilightLayerStructures.labyrinthChunk(level, x, z).isPresent()
                ? TwilightLayerStructures.LABYRINTH
                : null;
    }

    @WrapOperation(
            method = "calculateMapCenter",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ltwilightforest/util/landmarks/LegacyLandmarkPlacements;getNearestCenterXZ(II)Lnet/minecraft/core/BlockPos;"))
    private BlockPos mia$centerOnNativeMaze(
            int x, int z, Operation<BlockPos> original, @Local(argsOnly = true) Level level) {
        if (!TwilightLayerStructures.applies(level)) return original.call(x, z);
        return TwilightLayerStructures.labyrinthChunk(level, x, z)
                .map(pos -> new BlockPos(pos.getMiddleBlockX(), 0, pos.getMiddleBlockZ()))
                .orElseGet(() -> new BlockPos((x << 4) + 8, 0, (z << 4) + 8));
    }
}
