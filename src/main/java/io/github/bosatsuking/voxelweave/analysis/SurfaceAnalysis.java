package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.GridPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable deterministic result of one bounded surface-analysis pass. */
public record SurfaceAnalysis(List<SurfaceCell> cells, List<SurfaceComponent> components) {
    public SurfaceAnalysis {
        Objects.requireNonNull(cells);
        Objects.requireNonNull(components);

        cells = cells.stream().sorted().toList();
        components = components.stream().sorted().toList();

        Set<GridPoint> positions = new HashSet<>();
        for (SurfaceCell cell : cells) {
            if (!positions.add(cell.position())) {
                throw new IllegalArgumentException("Duplicate surface cell position: " + cell.position());
            }
        }

        Set<GridPoint> componentRoots = new HashSet<>();
        for (SurfaceComponent component : components) {
            if (!componentRoots.add(component.root())) {
                throw new IllegalArgumentException("Duplicate surface component root: " + component.root());
            }
        }
        for (SurfaceCell cell : cells) {
            if (!componentRoots.contains(cell.componentRoot())) {
                throw new IllegalArgumentException("Surface cell references missing component: " + cell.componentRoot());
            }
        }
    }

    public static SurfaceAnalysis empty() {
        return new SurfaceAnalysis(List.of(), List.of());
    }

    public Optional<SurfaceCell> cellAt(GridPoint position) {
        Objects.requireNonNull(position);
        int low = 0;
        int high = cells.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            SurfaceCell cell = cells.get(mid);
            int comparison = cell.position().compareTo(position);
            if (comparison < 0) low = mid + 1;
            else if (comparison > 0) high = mid - 1;
            else return Optional.of(cell);
        }
        return Optional.empty();
    }

    public Optional<SurfaceComponent> componentAt(GridPoint root) {
        Objects.requireNonNull(root);
        int low = 0;
        int high = components.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            SurfaceComponent component = components.get(mid);
            int comparison = component.root().compareTo(root);
            if (comparison < 0) low = mid + 1;
            else if (comparison > 0) high = mid - 1;
            else return Optional.of(component);
        }
        return Optional.empty();
    }

    public List<SurfaceComponent> smallIslandCandidates(int maxSize) {
        return components.stream().filter(component -> component.isSmallIslandCandidate(maxSize)).toList();
    }
}
