package io.github.bosatsuking.voxelweave.domain;

import java.util.Objects;
import java.util.Optional;

/** Non-empty world-space bounds, always [minInclusive, maxExclusive) on every axis. */
public record Region(GridPoint minInclusive, GridPoint maxExclusive) implements Comparable<Region> {
    public Region {
        Objects.requireNonNull(minInclusive);
        Objects.requireNonNull(maxExclusive);
        if (minInclusive.x() >= maxExclusive.x() || minInclusive.y() >= maxExclusive.y()
                || minInclusive.z() >= maxExclusive.z()) {
            throw new IllegalArgumentException("Region must have positive extent on every axis");
        }
    }

    /** Normalizes two half-open boundary corners; equal boundaries represent no region. */
    public static Optional<Region> between(GridPoint first, GridPoint second) {
        GridPoint min = new GridPoint(Math.min(first.x(), second.x()), Math.min(first.y(), second.y()),
                Math.min(first.z(), second.z()));
        GridPoint max = new GridPoint(Math.max(first.x(), second.x()), Math.max(first.y(), second.y()),
                Math.max(first.z(), second.z()));
        return nonEmpty(min, max);
    }

    public Optional<Region> intersection(Region other) {
        return nonEmpty(new GridPoint(Math.max(minInclusive.x(), other.minInclusive.x()),
                        Math.max(minInclusive.y(), other.minInclusive.y()),
                        Math.max(minInclusive.z(), other.minInclusive.z())),
                new GridPoint(Math.min(maxExclusive.x(), other.maxExclusive.x()),
                        Math.min(maxExclusive.y(), other.maxExclusive.y()),
                        Math.min(maxExclusive.z(), other.maxExclusive.z())));
    }

    public boolean contains(GridPoint point) {
        return point.x() >= minInclusive.x() && point.x() < maxExclusive.x()
                && point.y() >= minInclusive.y() && point.y() < maxExclusive.y()
                && point.z() >= minInclusive.z() && point.z() < maxExclusive.z();
    }

    private static Optional<Region> nonEmpty(GridPoint min, GridPoint max) {
        if (min.x() >= max.x() || min.y() >= max.y() || min.z() >= max.z()) return Optional.empty();
        return Optional.of(new Region(min, max));
    }

    @Override
    public int compareTo(Region other) {
        int result = minInclusive.compareTo(other.minInclusive);
        return result == 0 ? maxExclusive.compareTo(other.maxExclusive) : result;
    }
}
