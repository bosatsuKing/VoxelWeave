package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.GridPoint;

import java.util.Objects;

/** Deterministic 6-connected occupied component inside the analyzed operation target. */
public record SurfaceComponent(GridPoint root, int size, boolean complete) implements Comparable<SurfaceComponent> {
    public SurfaceComponent {
        Objects.requireNonNull(root);
        if (size <= 0) throw new IllegalArgumentException("Surface component size must be positive");
    }

    /**
     * A component is a safe small-island candidate only when its connectivity is fully known.
     * Incomplete components touch unknown snapshot data or continue outside the operation target.
     */
    public boolean isSmallIslandCandidate(int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be positive");
        return complete && size <= maxSize;
    }

    @Override
    public int compareTo(SurfaceComponent other) {
        return root.compareTo(other.root);
    }
}
