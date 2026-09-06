package io.github.bosatsuking.voxelweave.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/** Immutable geometry snapshot. No selection or no placement grants no target region. */
public record OperationTarget(Selection selection, Optional<PlacementTarget> placement) {
    public OperationTarget {
        Objects.requireNonNull(selection);
        Objects.requireNonNull(placement);
    }

    public static OperationTarget empty() {
        return new OperationTarget(new Selection(List.of()), Optional.empty());
    }

    /** Sorted, duplicate-free pairwise intersections, interpreted as a union.
     * Partial overlaps can remain: future block iteration must visit each coordinate once.
     */
    public List<Region> worldRegions() {
        TreeSet<Region> intersections = new TreeSet<>();
        placement.ifPresent(target -> {
            for (SelectionBox box : selection.boxes()) {
                for (Region region : target.worldRegions()) {
                    box.worldBounds().intersection(region).ifPresent(intersections::add);
                }
            }
        });
        return List.copyOf(intersections);
    }
}
