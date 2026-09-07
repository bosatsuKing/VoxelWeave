package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.GridPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable deterministic collection of higher-order local feature descriptors. */
public record SurfaceFeatureAnalysis(List<SurfaceFeatureDescriptor> descriptors) {
    public SurfaceFeatureAnalysis {
        Objects.requireNonNull(descriptors);
        descriptors = descriptors.stream().sorted().toList();

        Set<GridPoint> positions = new HashSet<>();
        for (SurfaceFeatureDescriptor descriptor : descriptors) {
            if (!positions.add(descriptor.position())) {
                throw new IllegalArgumentException("Duplicate feature descriptor position: " + descriptor.position());
            }
        }
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
