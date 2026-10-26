package com.altnoir.mementoinabyss.twilight.mixin;

import com.altnoir.mementoinabyss.infrastructure.worldgen.layer2.Layer2WorldGenLevel;
import com.altnoir.mementoinabyss.twilight.worldgen.TwilightLayerTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "twilightforest.world.components.feature.templates.TemplateFeature")
public abstract class TwilightTemplateGroundMixin {
    @Inject(method = "offsetToAverageGroundLevel", at = @At("HEAD"), cancellable = true)
    private static void mia$fitNegativeGround(
            WorldGenLevel world,
            BlockPos.MutableBlockPos start,
            Vec3i size,
            CallbackInfoReturnable<Boolean> cir) {
        if (world instanceof Layer2WorldGenLevel)
            cir.setReturnValue(TwilightLayerTemplates.fitGround(world, start, size));
    }
}
