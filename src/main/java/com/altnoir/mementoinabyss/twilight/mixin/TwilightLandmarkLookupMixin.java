package com.altnoir.mementoinabyss.twilight.mixin;

import com.altnoir.mementoinabyss.twilight.worldgen.TwilightLayerStructures;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "twilightforest.util.landmarks.LandmarkUtil")
public abstract class TwilightLandmarkLookupMixin {
    @Inject(
            method =
                    "locateNearestMatchingLandmark(Lnet/minecraft/world/level/LevelAccessor;Ljava/util/function/Predicate;IIZ)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true)
    private static void mia$findNativeLandmark(
            LevelAccessor level,
            Predicate<Structure> filter,
            int x,
            int z,
            boolean checkReady,
            CallbackInfoReturnable<Optional<StructureStart>> cir) {
        if (TwilightLayerStructures.applies(level))
            cir.setReturnValue(TwilightLayerStructures.findStart(level, filter, x, z, checkReady));
    }

    @Inject(
            method =
                    "locateNearestLandmarkStart(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/levelgen/structure/Structure;II)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true)
    private static void mia$findNativeStructure(
            LevelAccessor level,
            Structure structure,
            int x,
            int z,
            CallbackInfoReturnable<Optional<StructureStart>> cir) {
        if (TwilightLayerStructures.applies(level))
            cir.setReturnValue(
                    TwilightLayerStructures.findStart(
                            level, candidate -> candidate == structure, x, z, false));
    }
}
