package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.BlockStateRef;

/** Pure policy that decides whether a captured block state occupies voxel geometry. */
@FunctionalInterface
public interface BlockOccupancyPolicy {
    boolean isOccupied(BlockStateRef state);
}
