package io.github.bosatsuking.voxelweave.domain;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
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

    /** Returns a deterministic change set that restores every block to its previous state. */
    public ChangeSet inverse() {
        return ChangeSet.of(changes.stream()
                .map(change -> new BlockChange(change.position(), change.after(), change.before()))
                .toList());
    }
}
