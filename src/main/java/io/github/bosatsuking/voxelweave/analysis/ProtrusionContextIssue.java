package io.github.bosatsuking.voxelweave.analysis;

/** Reasons why a trace cannot establish a complete measurement. */
public enum ProtrusionContextIssue {
    INCOMPLETE_COMPONENT,
    UNKNOWN_NEIGHBOR,
    OCCUPIED_NEIGHBOR_OUTSIDE_TARGET,
    COORDINATE_OVERFLOW
}
