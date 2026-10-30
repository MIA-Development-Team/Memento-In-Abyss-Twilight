package com.altnoir.mementoinabyss.twilight.mixin;

import com.altnoir.mementoinabyss.twilight.worldgen.TwilightLayerGeneration;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.world.components.structures.finalcastle.FinalCastleMainComponent;

@Pseudo
@Mixin(targets = "twilightforest.world.components.structures.type.FinalCastleStructure")
public abstract class TwilightFinalCastleMixin {
    @Redirect(
            method = "getFirstPiece",
            at =
                    @At(
                            value = "NEW",
                            target =
                                    "(IIII)Ltwilightforest/world/components/structures/finalcastle/FinalCastleMainComponent;"))
    private FinalCastleMainComponent mia$placeCastleOnLayer2Surface(
            int index,
            int x,
            int y,
            int z,
            Structure.GenerationContext context,
            RandomSource random,
            ChunkPos chunk,
            int originalX,
            int originalY,
            int originalZ) {
        if (TwilightLayerGeneration.current() != null) {
            int surface =
                    context.chunkGenerator()
                            .getFirstOccupiedHeight(
                                    x,
                                    z,
                                    Heightmap.Types.WORLD_SURFACE_WG,
                                    context.heightAccessor(),
                                    context.randomState());
            // FinalCastleMainComponent offsets its floor 120 blocks above this argument.
            y = surface - 120;
        }
        return new FinalCastleMainComponent(index, x, y, z);
    }
}
