package io.github.bosatsuking.voxelweave.workspace;

import io.github.bosatsuking.voxelweave.domain.BlockChange;
import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.ChangeSet;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditWorkspaceTest {
    private static final BlockStateRef A = new BlockStateRef("minecraft:stone");
    private static final BlockStateRef B = new BlockStateRef("minecraft:quartz_block");
    private static final BlockStateRef C = new BlockStateRef("minecraft:dirt");
    private static final GridPoint P0 = new GridPoint(0, 0, 0);
    private static final GridPoint P1 = new GridPoint(1, 0, 0);

    @Test
    void previewDoesNotMutateCommittedOrSourceState() {
        SchematicSnapshot source = snapshot(A, A);
        EditWorkspace workspace = EditWorkspace.start(source).preview(change(P0, A, B));

        assertEquals(A, workspace.sourceSnapshot().blockAt(P0));
        assertEquals(A, workspace.committedSnapshot().blockAt(P0));
        assertEquals(B, workspace.previewSnapshot().blockAt(P0));
        assertTrue(workspace.hasPreview());
        assertFalse(workspace.isDirty());
    }

    @Test
    void commitStoresHistoryAndKeepsOriginalSource() {
        EditWorkspace workspace = EditWorkspace.start(snapshot(A, A))
                .preview(change(P0, A, B))
                .commitPreview();

        assertEquals(A, workspace.sourceSnapshot().blockAt(P0));
        assertEquals(B, workspace.committedSnapshot().blockAt(P0));
        assertFalse(workspace.hasPreview());
        assertTrue(workspace.canUndo());
        assertFalse(workspace.canRedo());
        assertTrue(workspace.isDirty());
    }

    @Test
    void cancelPreviewLeavesCommittedStateUntouched() {
        EditWorkspace workspace = EditWorkspace.start(snapshot(A, A))
                .preview(change(P0, A, B))
                .cancelPreview();

        assertEquals(A, workspace.committedSnapshot().blockAt(P0));
        assertFalse(workspace.hasPreview());
        assertFalse(workspace.canUndo());
    }

    @Test
    void undoAndRedoRestoreCommittedState() {
        EditWorkspace committed = EditWorkspace.start(snapshot(A, A))
                .preview(change(P0, A, B))
                .commitPreview();

        EditWorkspace undone = committed.undo();
        assertEquals(A, undone.committedSnapshot().blockAt(P0));
        assertFalse(undone.canUndo());
        assertTrue(undone.canRedo());
        assertFalse(undone.isDirty());

        EditWorkspace redone = undone.redo();
        assertEquals(B, redone.committedSnapshot().blockAt(P0));
        assertTrue(redone.canUndo());
        assertFalse(redone.canRedo());
        assertTrue(redone.isDirty());
    }

    @Test
    void historyUsesLifoOrderingAcrossMultipleCommits() {
        EditWorkspace workspace = EditWorkspace.start(snapshot(A, A))
                .preview(change(P0, A, B)).commitPreview()
                .preview(change(P1, A, C)).commitPreview();

        EditWorkspace firstUndo = workspace.undo();
        assertEquals(B, firstUndo.committedSnapshot().blockAt(P0));
        assertEquals(A, firstUndo.committedSnapshot().blockAt(P1));

        EditWorkspace secondUndo = firstUndo.undo();
        assertEquals(A, secondUndo.committedSnapshot().blockAt(P0));
        assertEquals(A, secondUndo.committedSnapshot().blockAt(P1));

        EditWorkspace firstRedo = secondUndo.redo();
        assertEquals(B, firstRedo.committedSnapshot().blockAt(P0));
        assertEquals(A, firstRedo.committedSnapshot().blockAt(P1));

        EditWorkspace secondRedo = firstRedo.redo();
        assertEquals(B, secondRedo.committedSnapshot().blockAt(P0));
        assertEquals(C, secondRedo.committedSnapshot().blockAt(P1));
    }

    @Test
    void newCommitAfterUndoClearsRedoHistory() {
        EditWorkspace workspace = EditWorkspace.start(snapshot(A, A))
                .preview(change(P0, A, B)).commitPreview()
                .undo()
                .preview(change(P1, A, C)).commitPreview();

        assertFalse(workspace.canRedo());
        assertEquals(A, workspace.committedSnapshot().blockAt(P0));
        assertEquals(C, workspace.committedSnapshot().blockAt(P1));
    }

    @Test
    void commitRejectsStalePreviewInsteadOfOverwritingUnexpectedState() {
        EditWorkspace workspace = EditWorkspace.start(snapshot(A, A))
                .preview(change(P0, C, B));

        assertThrows(IllegalStateException.class, workspace::commitPreview);
        assertEquals(A, workspace.committedSnapshot().blockAt(P0));
    }

    @Test
    void undoAndRedoRequirePendingPreviewToBeResolvedFirst() {
        EditWorkspace workspace = EditWorkspace.start(snapshot(A, A))
                .preview(change(P0, A, B));

        assertThrows(IllegalStateException.class, workspace::undo);
        assertThrows(IllegalStateException.class, workspace::redo);
    }

    @Test
    void emptyPreviewCommitDoesNotCreateHistory() {
        EditWorkspace workspace = EditWorkspace.start(snapshot(A, A))
                .preview(ChangeSet.empty())
                .commitPreview();

        assertFalse(workspace.hasPreview());
        assertFalse(workspace.canUndo());
        assertFalse(workspace.canRedo());
        assertFalse(workspace.isDirty());
    }

    @Test
    void inverseChangeSetIsDeterministicAndReversible() {
        ChangeSet original = ChangeSet.of(List.of(
                new BlockChange(P1, A, C),
                new BlockChange(P0, A, B)));

        ChangeSet inverse = original.inverse();

        assertEquals(List.of(P0, P1), inverse.changes().stream().map(BlockChange::position).toList());
        assertEquals(B, inverse.changes().get(0).before());
        assertEquals(A, inverse.changes().get(0).after());
        assertEquals(C, inverse.changes().get(1).before());
        assertEquals(A, inverse.changes().get(1).after());
    }

    private static SchematicSnapshot snapshot(BlockStateRef first, BlockStateRef second) {
        return new SchematicSnapshot(Map.of(P0, first, P1, second));
    }

    private static ChangeSet change(GridPoint position, BlockStateRef before, BlockStateRef after) {
        return ChangeSet.of(List.of(new BlockChange(position, before, after)));
    }
}
