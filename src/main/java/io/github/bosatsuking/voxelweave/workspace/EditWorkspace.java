package io.github.bosatsuking.voxelweave.workspace;

import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.ChangeSet;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import io.github.bosatsuking.voxelweave.domain.ReplacementRequest;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;
import io.github.bosatsuking.voxelweave.history.ChangeSetApplier;
import io.github.bosatsuking.voxelweave.transform.BlockReplacementEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable editing session with explicit source, committed, preview, undo and redo state. */
public record EditWorkspace(
        SchematicSnapshot sourceSnapshot,
        SchematicSnapshot committedSnapshot,
        Optional<ChangeSet> pendingPreview,
        List<ChangeSet> undoStack,
        List<ChangeSet> redoStack) {

    public EditWorkspace {
        Objects.requireNonNull(sourceSnapshot);
        Objects.requireNonNull(committedSnapshot);
        Objects.requireNonNull(pendingPreview);
        Objects.requireNonNull(undoStack);
        Objects.requireNonNull(redoStack);
        undoStack = List.copyOf(undoStack);
        redoStack = List.copyOf(redoStack);
    }

    public static EditWorkspace start(SchematicSnapshot sourceSnapshot) {
        Objects.requireNonNull(sourceSnapshot);
        return new EditWorkspace(sourceSnapshot, sourceSnapshot, Optional.empty(), List.of(), List.of());
    }

    /** Replaces the pending preview without mutating source or committed state. */
    public EditWorkspace preview(ChangeSet changeSet) {
        Objects.requireNonNull(changeSet);
        return new EditWorkspace(sourceSnapshot, committedSnapshot, Optional.of(changeSet), undoStack, redoStack);
    }

    /** Evaluates bounded block replacement against committed state and stores only the resulting preview. */
    public EditWorkspace previewReplacement(OperationTarget target, ReplacementRequest request) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(request);
        return preview(BlockReplacementEngine.replace(committedSnapshot, target, request));
    }

    /** Reads the preview overlay without copying the full schematic snapshot. */
    public BlockStateRef previewBlockAt(GridPoint position) {
        Objects.requireNonNull(position);
        return pendingPreview
                .flatMap(changeSet -> changeSet.changeAt(position))
                .map(change -> change.after())
                .orElseGet(() -> committedSnapshot.blockAt(position));
    }

    public EditWorkspace cancelPreview() {
        if (pendingPreview.isEmpty()) return this;
        return new EditWorkspace(sourceSnapshot, committedSnapshot, Optional.empty(), undoStack, redoStack);
    }

    /** Commits the current preview to workspace state and records one reversible history entry. */
    public EditWorkspace commitPreview() {
        ChangeSet changeSet = pendingPreview.orElseThrow(() -> new IllegalStateException("No pending preview to commit"));
        if (changeSet.isEmpty()) {
            return new EditWorkspace(sourceSnapshot, committedSnapshot, Optional.empty(), undoStack, redoStack);
        }

        SchematicSnapshot nextCommitted = ChangeSetApplier.apply(committedSnapshot, changeSet);
        return new EditWorkspace(sourceSnapshot, nextCommitted, Optional.empty(), appended(undoStack, changeSet), List.of());
    }

    public EditWorkspace undo() {
        requireNoPendingPreview("undo");
        if (undoStack.isEmpty()) return this;

        ChangeSet changeSet = undoStack.getLast();
        SchematicSnapshot restored = ChangeSetApplier.apply(committedSnapshot, changeSet.inverse());
        return new EditWorkspace(sourceSnapshot, restored, Optional.empty(), withoutLast(undoStack),
                appended(redoStack, changeSet));
    }

    public EditWorkspace redo() {
        requireNoPendingPreview("redo");
        if (redoStack.isEmpty()) return this;

        ChangeSet changeSet = redoStack.getLast();
        SchematicSnapshot restored = ChangeSetApplier.apply(committedSnapshot, changeSet);
        return new EditWorkspace(sourceSnapshot, restored, Optional.empty(), appended(undoStack, changeSet),
                withoutLast(redoStack));
    }

    public boolean hasPreview() {
        return pendingPreview.isPresent();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public boolean isDirty() {
        return !sourceSnapshot.equals(committedSnapshot);
    }

    private void requireNoPendingPreview(String operation) {
        if (pendingPreview.isPresent()) {
            throw new IllegalStateException("Cannot " + operation + " while a preview is pending");
        }
    }

    private static List<ChangeSet> appended(List<ChangeSet> stack, ChangeSet changeSet) {
        ArrayList<ChangeSet> copy = new ArrayList<>(stack);
        copy.add(changeSet);
        return List.copyOf(copy);
    }

    private static List<ChangeSet> withoutLast(List<ChangeSet> stack) {
        return List.copyOf(stack.subList(0, stack.size() - 1));
    }
}
