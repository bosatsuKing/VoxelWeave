package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.GridPoint;

import java.util.Objects;
import java.util.Optional;

/** Six cardinal voxel faces in deterministic enum order. */
public enum VoxelFace {
    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    private final long dx;
    private final long dy;
    private final long dz;

    VoxelFace(long dx, long dy, long dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    /** Returns empty only when long-coordinate arithmetic would overflow. */
    public Optional<GridPoint> neighborOf(GridPoint origin) {
        Objects.requireNonNull(origin);
        try {
            return Optional.of(new GridPoint(
                    Math.addExact(origin.x(), dx),
                    Math.addExact(origin.y(), dy),
                    Math.addExact(origin.z(), dz)));
        } catch (ArithmeticException overflow) {
            return Optional.empty();
        }
    }
}
