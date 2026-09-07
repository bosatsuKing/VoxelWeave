package io.github.bosatsuking.voxelweave.analysis;

/** Conservative local shape class derived only from known six-neighbor topology. */
public enum SurfaceFeatureKind {
    UNKNOWN_BOUNDARY,
    INTERIOR,
    ISOLATED,
    TIP,
    THIN_FEATURE,
    CORNER,
    EDGE,
    FACE
}
