package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;

import java.util.List;
import java.util.Objects;

/** Immutable source-bound evidence. An empty result does not establish artifact-free geometry. */
public final class ConnectedProtrusionAnalysis {
    private final SchematicSnapshot sourceSnapshot;
    private final SurfaceAnalysis sourceSurface;
    private final SurfaceFeatureAnalysis sourceFeatures;
    private final ProtrusionAnalysisRequest request;
    private final List<ProtrusionEvidence> evidence;

    ConnectedProtrusionAnalysis(SchematicSnapshot snapshot, SurfaceAnalysis surface,
                               SurfaceFeatureAnalysis features, ProtrusionAnalysisRequest request,
                               List<ProtrusionEvidence> evidence) {
        this.sourceSnapshot = Objects.requireNonNull(snapshot);
        this.sourceSurface = Objects.requireNonNull(surface);
        this.sourceFeatures = Objects.requireNonNull(features);
        this.request = Objects.requireNonNull(request);
        this.evidence = List.copyOf(evidence);
    }

    public boolean isFrom(SchematicSnapshot snapshot, SurfaceAnalysis surface, SurfaceFeatureAnalysis features) {
        return sourceSnapshot == snapshot && sourceSurface == surface && sourceFeatures == features;
    }

    public ProtrusionAnalysisRequest request() {
        return request;
    }

    /** Seed-coordinate order. Only complete TIP-to-TIP traces are deduplicated. */
    public List<ProtrusionEvidence> evidence() {
        return evidence;
    }
}
