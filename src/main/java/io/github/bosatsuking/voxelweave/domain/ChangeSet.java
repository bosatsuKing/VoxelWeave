package io.github.bosatsuking.voxelweave.domain;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/** Immutable, sorted, duplicate-coordinate change set for preview/history. */
public record ChangeSet(List<BlockChange> changes) {
    public ChangeSet {
        Objects.requireNonNull(changes);
        TreeSet<BlockChange> sorted = new TreeSet<>();
        for (BlockChange change : changes) {
            Objects.requireNonNull(change);
            if (!sorted.add(change)) {
                throw new IllegalArgumentException("Duplicate block position in change set: " + change.position());
            }
        }
        changes = List.copyOf(sorted);
    }

    public static ChangeSet empty() {
        return new ChangeSet(List.of());
    }

    public static ChangeSet of(Collection<BlockChange> changes) {
        return new ChangeSet(List.copyOf(changes));
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /** O(log n) lookup for preview overlays without materializing a full schematic copy. */
    public Optional<BlockChange> changeAt(GridPoint position) {
        Objects.requireNonNull(position);
        int low = 0;
        int high = changes.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            BlockChange candidate = changes.get(mid);
            int comparison = candidate.position().compareTo(position);
            if (comparison < 0) {
                low = mid + 1;
            } else if (comparison > 0) {
                high = mid - 1;
            } else {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** Returns a deterministic change set that restores every block to its previous state. */
    public ChangeSet inverse() {
        return ChangeSet.of(changes.stream()
                .map(change -> new BlockChange(change.position(), change.after(), change.before()))
                .toList());
    }
}
