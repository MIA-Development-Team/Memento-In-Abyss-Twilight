package com.altnoir.mementoinabyss.twilight.mixin;

import com.altnoir.mementoinabyss.twilight.worldgen.TwilightLayerGeneration;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "twilightforest.world.components.structures.darktower.DarkTowerMainComponent")
public abstract class TwilightDarkTowerMainMixin {
    // Twilight's rotation tables use Java remainder, which becomes a negative index below Y=0.
    @ModifyExpressionValue(
            method = {
                "addThreeQuarterFloors",
                "addThreeQuarterFloorsDecorateBoss",
                "makeTimberBeams",
                "addTopBuilderPlatform"
            },
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;minY()I"))
    private int mia$nonnegativeRotationPhase(int height) {
        return height < 0 && TwilightLayerGeneration.current() != null
                ? Math.floorMod(height, 4)
                : height;
    }
}
