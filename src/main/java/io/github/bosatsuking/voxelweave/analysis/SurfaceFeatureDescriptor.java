package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.GridPoint;

import java.util.Objects;

/** Immutable higher-order local shape descriptor for one analyzed occupied voxel. */
public record SurfaceFeatureDescriptor(
        GridPoint position,
        SurfaceFeatureKind kind,
        int exposureVectorX,
        int exposureVectorY,
        int exposureVectorZ,
        int exposedAxisCount,
        int oppositeExposurePairCount,
        int surfaceNeighborCount,
        boolean componentComplete) implements Comparable<SurfaceFeatureDescriptor> {

    public SurfaceFeatureDescriptor {
        Objects.requireNonNull(position);
        Objects.requireNonNull(kind);
        if (exposureVectorX < -1 || exposureVectorX > 1
                || exposureVectorY < -1 || exposureVectorY > 1
                || exposureVectorZ < -1 || exposureVectorZ > 1) {
            throw new IllegalArgumentException("Exposure-vector components must be between -1 and 1");
        }
        if (exposedAxisCount < 0 || exposedAxisCount > 3) {
            throw new IllegalArgumentException("exposedAxisCount must be between 0 and 3");
        }
        if (oppositeExposurePairCount < 0 || oppositeExposurePairCount > 3) {
            throw new IllegalArgumentException("oppositeExposurePairCount must be between 0 and 3");
        }
        if (surfaceNeighborCount < 0 || surfaceNeighborCount > 6) {
            throw new IllegalArgumentException("surfaceNeighborCount must be between 0 and 6");
        }
    }

    public boolean hasDirectionalExposure() {
        return exposureVectorX != 0 || exposureVectorY != 0 || exposureVectorZ != 0;
    }

    public boolean hasCompleteContext() {
        return kind != SurfaceFeatureKind.UNKNOWN_BOUNDARY && componentComplete;
    }

    @Override
    public int compareTo(SurfaceFeatureDescriptor other) {
        return position.compareTo(other.position);
    }
}
