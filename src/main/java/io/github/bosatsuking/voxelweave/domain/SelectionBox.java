package io.github.bosatsuking.voxelweave.domain;

import java.util.Objects;

public record SelectionBox(Region worldBounds) {
    public SelectionBox {
        Objects.requireNonNull(worldBounds);
    }
}
