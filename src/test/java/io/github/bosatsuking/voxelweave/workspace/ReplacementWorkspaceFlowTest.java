package io.github.bosatsuking.voxelweave.workspace;

import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import io.github.bosatsuking.voxelweave.domain.PlacementTarget;
import io.github.bosatsuking.voxelweave.domain.Region;
import io.github.bosatsuking.voxelweave.domain.ReplacementRequest;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;
import io.github.bosatsuking.voxelweave.domain.Selection;
import io.github.bosatsuking.voxelweave.domain.SelectionBox;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplacementWorkspaceFlowTest {
    private static final BlockStateRef A = new BlockStateRef("minecraft:stone");
    private static final BlockStateRef B = new BlockStateRef("minecraft:quartz_block");

    @Test
    void boundedReplacementPreviewCommitUndoRedoStaysInsideIntersection() {
        GridPoint p0 = p(0, 0, 0);
        GridPoint p1 = p(1, 0, 0);
        GridPoint p2 = p(2, 0, 0);
        SchematicSnapshot source = new SchematicSnapshot(Map.of(p0, A, p1, A, p2, A));
        OperationTarget target = new OperationTarget(
                new Selection(List.of(new SelectionBox(region(0, 0, 0, 2, 1, 1)))),
                Optional.of(new PlacementTarget(List.of(region(1, 0, 0, 3, 1, 1)))));

        EditWorkspace previewed = EditWorkspace.start(source)
                .previewReplacement(target, new ReplacementRequest(A, B));

        assertEquals(A, previewed.committedSnapshot().blockAt(p0));
        assertEquals(A, previewed.committedSnapshot().blockAt(p1));
        assertEquals(A, previewed.committedSnapshot().blockAt(p2));
        assertEquals(A, previewed.previewSnapshot().blockAt(p0));
        assertEquals(B, previewed.previewSnapshot().blockAt(p1));
        assertEquals(A, previewed.previewSnapshot().blockAt(p2));

        EditWorkspace committed = previewed.commitPreview();
        assertEquals(A, committed.committedSnapshot().blockAt(p0));
        assertEquals(B, committed.committedSnapshot().blockAt(p1));
        assertEquals(A, committed.committedSnapshot().blockAt(p2));

        EditWorkspace undone = committed.undo();
        assertEquals(A, undone.committedSnapshot().blockAt(p0));
        assertEquals(A, undone.committedSnapshot().blockAt(p1));
        assertEquals(A, undone.committedSnapshot().blockAt(p2));

        EditWorkspace redone = undone.redo();
        assertEquals(A, redone.committedSnapshot().blockAt(p0));
        assertEquals(B, redone.committedSnapshot().blockAt(p1));
        assertEquals(A, redone.committedSnapshot().blockAt(p2));
    }

    private static Region region(long minX, long minY, long minZ, long maxX, long maxY, long maxZ) {
        return new Region(p(minX, minY, minZ), p(maxX, maxY, maxZ));
    }

    private static GridPoint p(long x, long y, long z) {
        return new GridPoint(x, y, z);
    }
}
