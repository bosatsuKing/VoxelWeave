package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.GridPoint;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/** Immutable local six-neighbor topology for one occupied voxel. */
public record SurfaceCell(
        GridPoint position,
        BlockStateRef state,
        List<VoxelFace> exposedFaces,
        List<VoxelFace> unknownFaces,
        int occupiedNeighborCount,
        GridPoint componentRoot) implements Comparable<SurfaceCell> {

    public SurfaceCell {
        Objects.requireNonNull(position);
        Objects.requireNonNull(state);
        Objects.requireNonNull(exposedFaces);
        Objects.requireNonNull(unknownFaces);
        Objects.requireNonNull(componentRoot);

        exposedFaces = normalized(exposedFaces);
        unknownFaces = normalized(unknownFaces);

        EnumSet<VoxelFace> overlap = EnumSet.noneOf(VoxelFace.class);
        overlap.addAll(exposedFaces);
        overlap.retainAll(unknownFaces);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException("A voxel face cannot be both exposed and unknown");
        }
        if (occupiedNeighborCount < 0 || occupiedNeighborCount > VoxelFace.values().length) {
            throw new IllegalArgumentException("occupiedNeighborCount must be between 0 and 6");
        }
        if (occupiedNeighborCount + exposedFaces.size() + unknownFaces.size() != VoxelFace.values().length) {
            throw new IllegalArgumentException("Neighbor topology must account for all six voxel faces");
        }
    }

    public boolean isInterior() {
        return occupiedNeighborCount == VoxelFace.values().length;
    }

    /** Known empty neighbor data exposes at least one face. Unknown data never counts as exposure. */
    public boolean isSurface() {
        return !exposedFaces.isEmpty();
    }

    /** Safe isolated classification requires all six neighbors to be known and empty. */
    public boolean isIsolated() {
        return occupiedNeighborCount == 0 && unknownFaces.isEmpty();
    }

    /** Conservative local candidate for spike/end-point cleanup; unknown boundaries are excluded. */
    public boolean isWeaklySupported() {
        return occupiedNeighborCount <= 1 && unknownFaces.isEmpty();
    }

    public boolean hasUnknownBoundary() {
        return !unknownFaces.isEmpty();
    }

    public int knownEmptyNeighborCount() {
        return exposedFaces.size();
    }

    @Override
    public int compareTo(SurfaceCell other) {
        return position.compareTo(other.position);
    }

    private static List<VoxelFace> normalized(List<VoxelFace> faces) {
        EnumSet<VoxelFace> set = EnumSet.noneOf(VoxelFace.class);
        for (VoxelFace face : faces) set.add(Objects.requireNonNull(face));
        return List.copyOf(set);
    }
}
