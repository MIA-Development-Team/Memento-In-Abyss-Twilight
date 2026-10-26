package com.altnoir.mementoinabyss.twilight.mixin;

import com.altnoir.mementoinabyss.twilight.worldgen.TwilightLayerGeneration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(
        targets =
                "twilightforest.world.components.structures.finalcastle.FinalCastleDungeonExitComponent")
public abstract class TwilightCastleDungeonMixin {
    @ModifyArg(
            method = "addChildren",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ltwilightforest/world/components/structures/finalcastle/FinalCastleDungeonStepsComponent;<init>(IIIILnet/minecraft/core/Direction;)V"),
            index = 1)
    private int mia$keepDungeonBelowCastle(int original) {
        // TF passes minZ as X here, displacing the entire lower dungeon by thousands of blocks.
        return TwilightLayerGeneration.current() == null
                ? original
                : ((StructurePiece) (Object) this).getBoundingBox().minX() + 15;
    }
}
