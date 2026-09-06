package io.github.bosatsuking.voxelweave.domain;

import java.util.Objects;

/** Pure, external-API-free block-state identity used by transformations. */
public record BlockStateRef(String value) implements Comparable<BlockStateRef> {
    public BlockStateRef {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException("Block state value must not be blank");
    }

    @Override
    public int compareTo(BlockStateRef other) {
        return value.compareTo(other.value);
    }
}
