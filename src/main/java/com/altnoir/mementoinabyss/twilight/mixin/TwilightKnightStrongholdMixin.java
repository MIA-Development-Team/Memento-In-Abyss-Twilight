package com.altnoir.mementoinabyss.twilight.mixin;

import com.altnoir.mementoinabyss.twilight.worldgen.TwilightLayerGeneration;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "twilightforest.world.components.structures.type.KnightStrongholdStructure")
public abstract class TwilightKnightStrongholdMixin {
    @WrapOperation(
            method = "getFirstPiece",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int mia$rememberEntranceRoll(
            RandomSource random,
            int bound,
            Operation<Integer> original,
            @Share("entranceRoll") LocalIntRef roll) {
        int value = original.call(random, bound);
        roll.set(value);
        return value;
    }

    @ModifyArg(
            method = "getFirstPiece",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ltwilightforest/world/components/structures/stronghold/StrongholdEntranceComponent;<init>(IIII)V"),
            index = 2)
    private int mia$keepNativeEntranceHeight(
            int original,
            @Local(argsOnly = true, ordinal = 1) int y,
            @Share("entranceRoll") LocalIntRef roll) {
        // TF's unparenthesized ternary discards negative Y; preserve its one existing random draw.
        return TwilightLayerGeneration.current() == null ? original : y + (roll.get() == 0 ? 5 : 1);
    }
}
