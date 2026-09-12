package io.github.bosatsuking.voxelweave.analysis;

/** Explicit work budget, counting visited cells including the seed and any attachment. */
public record ProtrusionAnalysisRequest(int maxTraceLength) {
    public ProtrusionAnalysisRequest {
        if (maxTraceLength <= 0) throw new IllegalArgumentException("maxTraceLength must be positive");
    }
}
