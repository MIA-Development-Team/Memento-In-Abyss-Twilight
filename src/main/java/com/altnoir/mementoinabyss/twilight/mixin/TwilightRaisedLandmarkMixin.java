package com.altnoir.mementoinabyss.twilight.mixin;

import com.altnoir.mementoinabyss.twilight.worldgen.TwilightLayerGeneration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(
        targets = {
            "twilightforest.world.components.structures.type.AuroraPalaceStructure",
            "twilightforest.world.components.structures.type.GiantHouseStructure"
        })
public abstract class TwilightRaisedLandmarkMixin {
    // Ice towers no longer need a forty-block glacier; giant clouds keep a smaller local lift.
    @ModifyVariable(method = "getFirstPiece", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int mia$adaptVerticalLift(int y) {
        if (TwilightLayerGeneration.current() == null) return y;
        var type = BuiltInRegistries.STRUCTURE_TYPE.getKey(((Structure) (Object) this).type());
        return y - (type != null && type.getPath().equals("giant_house") ? 120 : 40);
    }
}
