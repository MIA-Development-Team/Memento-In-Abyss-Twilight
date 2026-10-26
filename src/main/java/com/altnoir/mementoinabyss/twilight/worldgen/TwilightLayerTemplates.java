package com.altnoir.mementoinabyss.twilight.worldgen;

import com.google.common.math.StatsAccumulator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import twilightforest.util.features.FeatureLogic;

public final class TwilightLayerTemplates {
    public static boolean fitGround(
            WorldGenLevel world, BlockPos.MutableBlockPos start, Vec3i size) {
        var heights = new StatsAccumulator();
        var cursor = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                int x = start.getX() + dx, z = start.getZ() + dz;
                int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (; y >= world.getMinY(); y--) {
                    var block = world.getBlockState(cursor.set(x, y, z));
                    if (FeatureLogic.isBlockNotOk(block)) return false;
                    if (FeatureLogic.isBlockOk(block)) break;
                }
                if (y < world.getMinY()) return false;
                heights.add(y);
            }
        }
        if (heights.populationStandardDeviation() > 2) return false;
        int baseY = Mth.floor(heights.mean() + .5);
        int maxY = (int) heights.max();
        start.setY(baseY);
        for (var point :
                BlockPos.betweenClosed(start.above(maxY - baseY + 1), start.offset(size))) {
            if (!world.getBlockState(point).canBeReplaced()) return false;
        }
        return true;
    }

    private TwilightLayerTemplates() {}
}
