package io.github.bosatsuking.voxelweave.analysis;

/** Measurement outcomes, never artifact or removal classifications. */
public enum ProtrusionTermination {
    /** Reached a cell with at least three occupied neighbors; does not prove a broad support surface. */
    JUNCTION_REACHED,
    OTHER_TIP_REACHED,
    TRACE_LIMIT_REACHED,
    INCOMPLETE_CONTEXT,
    COORDINATE_OVERFLOW
}
