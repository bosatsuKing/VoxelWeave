package io.github.bosatsuking.voxelweave.domain;

import java.util.List;

/** Geometry snapshot of enabled placement subregions after all world-space transforms.
 * This is not a handle for writing to a schematic or a persistent placement identity.
 */
public record PlacementTarget(List<Region> worldRegions) {
    public PlacementTarget {
        worldRegions = List.copyOf(worldRegions).stream().distinct().sorted().toList();
    }
}
