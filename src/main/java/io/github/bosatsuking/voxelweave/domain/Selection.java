package io.github.bosatsuking.voxelweave.domain;

import java.util.Comparator;
import java.util.List;

/** Snapshot of all complete selection boxes; gaps are never filled. */
public record Selection(List<SelectionBox> boxes) {
    public Selection {
        boxes = List.copyOf(boxes).stream().distinct()
                .sorted(Comparator.comparing(SelectionBox::worldBounds)).toList();
    }
}
