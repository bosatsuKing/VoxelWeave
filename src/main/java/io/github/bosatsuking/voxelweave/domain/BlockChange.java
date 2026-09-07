package io.github.bosatsuking.voxelweave.domain;

import java.util.Objects;

public record BlockChange(GridPoint position, BlockStateRef before, BlockStateRef after)
        implements Comparable<BlockChange> {
    public BlockChange {
        Objects.requireNonNull(position);
        Objects.requireNonNull(before);
        Objects.requireNonNull(after);
        if (before.equals(after)) throw new IllegalArgumentException("Block change must modify state");
    }

    @Override
    public int compareTo(BlockChange other) {
        return position.compareTo(other.position);
    }
}
