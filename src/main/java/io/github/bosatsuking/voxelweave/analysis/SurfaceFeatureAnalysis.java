package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.GridPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable deterministic collection of higher-order local feature descriptors. */
public final class SurfaceFeatureAnalysis {
    private final List<SurfaceFeatureDescriptor> descriptors;
    private final SurfaceAnalysis sourceAnalysis;

    /** Unbound evidence for inspection; not eligible for cleanup planning. */
    public SurfaceFeatureAnalysis(List<SurfaceFeatureDescriptor> descriptors) {
        this(descriptors, null);
    }

    private SurfaceFeatureAnalysis(List<SurfaceFeatureDescriptor> descriptors, SurfaceAnalysis sourceAnalysis) {
        Objects.requireNonNull(descriptors);
        descriptors = descriptors.stream().sorted().toList();

        Set<GridPoint> positions = new HashSet<>();
        for (SurfaceFeatureDescriptor descriptor : descriptors) {
            if (!positions.add(descriptor.position())) {
                throw new IllegalArgumentException("Duplicate feature descriptor position: " + descriptor.position());
            }
        }
        this.descriptors = descriptors;
        this.sourceAnalysis = sourceAnalysis;
    }

    static SurfaceFeatureAnalysis fromSurface(SurfaceAnalysis source, List<SurfaceFeatureDescriptor> descriptors) {
        return new SurfaceFeatureAnalysis(descriptors, Objects.requireNonNull(source));
    }

    public boolean isFrom(SurfaceAnalysis analysis) {
        return sourceAnalysis != null && sourceAnalysis == analysis;
    }

    public List<SurfaceFeatureDescriptor> descriptors() {
        return descriptors;
    }

    /** Evidence equality does not grant source compatibility; use isFrom for that. */
    @Override
    public boolean equals(Object other) {
        return other instanceof SurfaceFeatureAnalysis analysis && descriptors.equals(analysis.descriptors);
    }

    @Override
    public int hashCode() {
        return descriptors.hashCode();
    }

    public static SurfaceFeatureAnalysis empty() {
        return new SurfaceFeatureAnalysis(List.of());
    }

    public Optional<SurfaceFeatureDescriptor> descriptorAt(GridPoint position) {
        Objects.requireNonNull(position);
        int low = 0;
        int high = descriptors.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            SurfaceFeatureDescriptor descriptor = descriptors.get(mid);
            int comparison = descriptor.position().compareTo(position);
            if (comparison < 0) low = mid + 1;
            else if (comparison > 0) high = mid - 1;
            else return Optional.of(descriptor);
        }
        return Optional.empty();
    }

    public List<SurfaceFeatureDescriptor> ofKind(SurfaceFeatureKind kind) {
        Objects.requireNonNull(kind);
        return descriptors.stream().filter(descriptor -> descriptor.kind() == kind).toList();
    }
}
