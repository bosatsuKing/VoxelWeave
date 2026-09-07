package io.github.bosatsuking.voxelweave.transform;

import io.github.bosatsuking.voxelweave.analysis.SurfaceFeatureKind;
import io.github.bosatsuking.voxelweave.domain.BlockStateRef;

import java.util.Objects;
import java.util.Set;

/** Explicit cleanup policy. The caller supplies the replacement state and preservation policy. */
public record IslandCleanupRequest(int maxComponentSize, BlockStateRef replacementState,
                                   Set<SurfaceFeatureKind> protectedFeatureKinds) {
    public IslandCleanupRequest {
        if (maxComponentSize <= 0) throw new IllegalArgumentException("maxComponentSize must be positive");
        Objects.requireNonNull(replacementState);
        protectedFeatureKinds = Set.copyOf(protectedFeatureKinds);
    }
}
