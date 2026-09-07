package io.github.bosatsuking.voxelweave.transform;

import io.github.bosatsuking.voxelweave.analysis.SurfaceAnalysis;
import io.github.bosatsuking.voxelweave.analysis.SurfaceAnalyzer;
import io.github.bosatsuking.voxelweave.analysis.SurfaceFeatureAnalysis;
import io.github.bosatsuking.voxelweave.analysis.SurfaceFeatureAnalyzer;
import io.github.bosatsuking.voxelweave.analysis.SurfaceFeatureKind;
import io.github.bosatsuking.voxelweave.domain.BlockChange;
import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.ChangeSet;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import io.github.bosatsuking.voxelweave.domain.PlacementTarget;
import io.github.bosatsuking.voxelweave.domain.Region;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;
import io.github.bosatsuking.voxelweave.domain.Selection;
import io.github.bosatsuking.voxelweave.domain.SelectionBox;
import io.github.bosatsuking.voxelweave.workspace.EditWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DisconnectedIslandCleanupPlannerTest {
    private static final BlockStateRef EMPTY = new BlockStateRef("test:empty");
    private static final BlockStateRef SOLID = new BlockStateRef("test:solid");
    private static final Region AREA = new Region(p(0, 0, 0), p(10, 4, 4));

    @Test
    void isolatedBlockProducesReversibleCandidateWithoutMutatingSnapshot() {
        SchematicSnapshot snapshot = snapshot(p(0, 0, 0));
        ChangeSet changes = plan(snapshot, target(AREA), request(1));

        assertEquals(List.of(new BlockChange(p(0, 0, 0), SOLID, EMPTY)), changes.changes());
        assertEquals(SOLID, snapshot.blockAt(p(0, 0, 0)));
        SurfaceAnalysis surface = analyze(snapshot, target(AREA));
        EditWorkspace preview = EditWorkspace.start(snapshot)
                .previewIslandCleanup(surface, SurfaceFeatureAnalyzer.analyze(surface), request(1));
        assertEquals(changes, preview.pendingPreview().orElseThrow());
        assertSame(snapshot, preview.committedSnapshot());
        assertEquals(EMPTY, preview.previewBlockAt(p(0, 0, 0)));
        EditWorkspace committed = preview.commitPreview();
        assertEquals(EMPTY, committed.committedSnapshot().blockAt(p(0, 0, 0)));
        EditWorkspace undone = committed.undo();
        assertEquals(snapshot, undone.committedSnapshot());
        assertEquals(committed.committedSnapshot(), undone.redo().committedSnapshot());
    }

    @Test
    void workspaceRejectsOldCleanupAfterNeighborCommitAndAcceptsFreshAnalysis() {
        SchematicSnapshot original = snapshot(p(0, 0, 0));
        SurfaceAnalysis surface = analyze(original, target(AREA));
        SurfaceFeatureAnalysis features = SurfaceFeatureAnalyzer.analyze(surface);
        ChangeSet oldCleanup = DisconnectedIslandCleanupPlanner.plan(original, surface, features, request(1));
        assertEquals(1, oldCleanup.changes().size());
        EditWorkspace current = EditWorkspace.start(original)
                .preview(ChangeSet.of(List.of(new BlockChange(p(1, 0, 0), EMPTY, SOLID))))
                .commitPreview();

        IllegalArgumentException stale = assertThrows(IllegalArgumentException.class,
                () -> current.previewIslandCleanup(surface, features, request(1)));
        assertTrue(stale.getMessage().contains("current snapshot"));
        assertFalse(current.hasPreview());
        assertEquals(1, current.undoStack().size());
        assertEquals(SOLID, current.committedSnapshot().blockAt(p(0, 0, 0)));
        assertEquals(SOLID, current.committedSnapshot().blockAt(p(1, 0, 0)));
        assertEquals(EMPTY, original.blockAt(p(1, 0, 0)));

        SurfaceAnalysis fresh = analyze(current.committedSnapshot(), target(AREA));
        EditWorkspace preview = current.previewIslandCleanup(fresh, SurfaceFeatureAnalyzer.analyze(fresh), request(1));
        assertTrue(preview.pendingPreview().orElseThrow().isEmpty());
        EditWorkspace committed = preview.commitPreview();
        assertSame(current.committedSnapshot(), committed.committedSnapshot());
        assertEquals(current.undoStack(), committed.undoStack());
    }

    @Test
    void staleCleanupRejectionPreservesPendingPreviewAndRedoHistory() {
        SchematicSnapshot original = snapshot(p(0, 0, 0));
        SurfaceAnalysis oldSurface = analyze(original, target(AREA));
        SurfaceFeatureAnalysis oldFeatures = SurfaceFeatureAnalyzer.analyze(oldSurface);
        EditWorkspace undone = EditWorkspace.start(original)
                .preview(ChangeSet.of(List.of(new BlockChange(p(1, 0, 0), EMPTY, SOLID))))
                .commitPreview().undo();
        assertEquals(original, undone.committedSnapshot());
        assertNotSame(original, undone.committedSnapshot());
        ChangeSet pending = ChangeSet.of(List.of(new BlockChange(p(0, 0, 0), SOLID, EMPTY)));
        EditWorkspace current = undone.preview(pending);

        assertThrows(IllegalArgumentException.class,
                () -> current.previewIslandCleanup(oldSurface, oldFeatures, request(1)));
        SurfaceAnalysis fresh = analyze(current.committedSnapshot(), target(AREA));
        assertThrows(IllegalArgumentException.class,
                () -> current.previewIslandCleanup(fresh, oldFeatures, request(1)));
        assertSame(pending, current.pendingPreview().orElseThrow());
        assertEquals(undone.undoStack(), current.undoStack());
        assertEquals(undone.redoStack(), current.redoStack());

        EditWorkspace preview = current.previewIslandCleanup(fresh, SurfaceFeatureAnalyzer.analyze(fresh), request(1));
        assertSame(current.committedSnapshot(), preview.committedSnapshot());
        assertEquals(current.redoStack(), preview.redoStack());
        assertEquals(pending, preview.pendingPreview().orElseThrow());
        EditWorkspace committed = preview.commitPreview();
        assertFalse(committed.canRedo());
        assertEquals(EMPTY, committed.committedSnapshot().blockAt(p(0, 0, 0)));
        assertEquals(original, committed.undo().committedSnapshot());
    }

    @Test
    void thresholdIncludesExactSizeAndSkipsLargerComponents() {
        SchematicSnapshot snapshot = snapshot(p(0, 0, 0), p(3, 0, 0), p(4, 0, 0),
                p(7, 0, 0), p(8, 0, 0), p(9, 0, 0));
        assertEquals(List.of(p(0, 0, 0), p(3, 0, 0), p(4, 0, 0)),
                positions(plan(snapshot, target(AREA), request(2))));
    }

    @Test
    void unknownBoundaryAndKnownContinuationOutsideTargetArePreserved() {
        SchematicSnapshot unknown = new SchematicSnapshot(Map.of(p(0, 0, 0), SOLID));
        SurfaceAnalysis surface = analyze(unknown, target(AREA));
        SurfaceFeatureAnalysis features = SurfaceFeatureAnalyzer.analyze(surface);
        assertEquals(SurfaceFeatureKind.UNKNOWN_BOUNDARY, features.descriptors().getFirst().kind());
        assertFalse(surface.components().getFirst().complete());
        assertTrue(DisconnectedIslandCleanupPlanner.plan(unknown, surface, features, request(10)).isEmpty());

        SchematicSnapshot continuation = snapshot(p(0, 0, 0), p(1, 0, 0));
        OperationTarget oneBlock = target(new Region(p(0, 0, 0), p(1, 1, 1)));
        SurfaceAnalysis incomplete = analyze(continuation, oneBlock);
        assertFalse(incomplete.components().getFirst().complete());
        assertFalse(incomplete.cells().getFirst().hasUnknownBoundary());
        assertTrue(plan(continuation, oneBlock, request(10)).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = SurfaceFeatureKind.class, names = {"TIP", "THIN_FEATURE", "EDGE", "CORNER"})
    void oneProtectedFeaturePreservesWholeComponent(SurfaceFeatureKind protectedKind) {
        ArrayList<GridPoint> shape = new ArrayList<>();
        if (protectedKind == SurfaceFeatureKind.TIP || protectedKind == SurfaceFeatureKind.THIN_FEATURE) {
            shape.addAll(List.of(p(0, 0, 0), p(1, 0, 0), p(2, 0, 0)));
        } else {
            for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) for (int z = 0; z < 3; z++) {
                shape.add(p(x, y, z));
            }
        }
        // A second unprotected component must still be cleaned up.
        shape.add(p(7, 0, 0));
        SchematicSnapshot snapshot = snapshot(shape.toArray(GridPoint[]::new));
        SurfaceAnalysis surface = analyze(snapshot, target(AREA));
        SurfaceFeatureAnalysis features = SurfaceFeatureAnalyzer.analyze(surface);
        assertFalse(features.ofKind(protectedKind).isEmpty());
        assertTrue(features.descriptors().stream().anyMatch(d -> d.kind() != protectedKind));
        assertEquals(List.of(p(7, 0, 0)), positions(DisconnectedIslandCleanupPlanner.plan(snapshot, surface,
                features, new IslandCleanupRequest(27, EMPTY, Set.of(protectedKind)))));
    }

    @Test
    void selectionPlacementGapsAndOverlapsDoNotExpandOrDuplicateOutput() {
        SchematicSnapshot snapshot = snapshot(p(0, 0, 0), p(2, 0, 0), p(4, 0, 0), p(8, 0, 0));
        OperationTarget target = new OperationTarget(new Selection(List.of(
                new SelectionBox(new Region(p(0, 0, 0), p(1, 1, 1))),
                new SelectionBox(new Region(p(4, 0, 0), p(9, 1, 1))),
                new SelectionBox(new Region(p(4, 0, 0), p(5, 1, 1))))),
                Optional.of(new PlacementTarget(List.of(new Region(p(0, 0, 0), p(5, 1, 1))))));
        assertEquals(List.of(p(0, 0, 0), p(4, 0, 0)), positions(plan(snapshot, target, request(10))));
    }

    @Test
    void changedHaloRejectsStaleSurfaceEvenWhenAnalyzedCellStateIsUnchanged() {
        SchematicSnapshot original = snapshot(p(0, 0, 0));
        SurfaceAnalysis surface = analyze(original, target(AREA));
        SurfaceFeatureAnalysis features = SurfaceFeatureAnalyzer.analyze(surface);
        Map<GridPoint, BlockStateRef> changed = new HashMap<>(original.blocks());
        changed.put(p(-1, 0, 0), SOLID);
        assertThrows(IllegalArgumentException.class, () -> DisconnectedIslandCleanupPlanner.plan(
                new SchematicSnapshot(changed), surface, features, request(1)));
        changed.remove(p(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> DisconnectedIslandCleanupPlanner.plan(
                new SchematicSnapshot(changed), surface, features, request(1)));
    }

    @Test
    void staleFeaturesAndFeaturesFromAnotherAnalysisAreRejected() {
        SchematicSnapshot original = snapshot(p(0, 0, 0));
        SurfaceAnalysis oldSurface = analyze(original, target(AREA));
        SurfaceFeatureAnalysis oldFeatures = SurfaceFeatureAnalyzer.analyze(oldSurface);
        SchematicSnapshot current = snapshot(p(0, 0, 0), p(1, 0, 0));
        SurfaceAnalysis currentSurface = analyze(current, target(AREA));
        assertThrows(IllegalArgumentException.class, () -> DisconnectedIslandCleanupPlanner.plan(
                current, currentSurface, oldFeatures, request(2)));

        SurfaceAnalysis equivalent = analyze(original, target(AREA));
        assertEquals(oldSurface, equivalent);
        assertThrows(IllegalArgumentException.class, () -> DisconnectedIslandCleanupPlanner.plan(
                original, equivalent, oldFeatures, request(2)));
    }

    @Test
    void copiedOrManuallyConstructedEvidenceCannotAcquireSourceBinding() {
        SchematicSnapshot snapshot = snapshot(p(0, 0, 0));
        SurfaceAnalysis surface = analyze(snapshot, target(AREA));
        SurfaceAnalysis unbound = new SurfaceAnalysis(surface.cells(), surface.components());
        assertThrows(IllegalArgumentException.class, () -> DisconnectedIslandCleanupPlanner.plan(
                snapshot, unbound, SurfaceFeatureAnalyzer.analyze(unbound), request(1)));
        SurfaceFeatureAnalysis features = SurfaceFeatureAnalyzer.analyze(surface);
        assertThrows(IllegalArgumentException.class, () -> DisconnectedIslandCleanupPlanner.plan(
                snapshot, surface, new SurfaceFeatureAnalysis(features.descriptors()), request(1)));
        // Even an equal snapshot copy requires fresh analysis; equality is not provenance.
        assertThrows(IllegalArgumentException.class, () -> DisconnectedIslandCleanupPlanner.plan(
                new SchematicSnapshot(snapshot.blocks()), surface, features, request(1)));
    }

    @Test
    void noOpAndEmptyAnalysisProduceNoChangesButStillRequireValidSources() {
        SchematicSnapshot snapshot = snapshot(p(0, 0, 0));
        assertTrue(plan(snapshot, target(AREA), new IslandCleanupRequest(1, SOLID, Set.of())).isEmpty());
        assertTrue(plan(snapshot(), target(AREA), request(1)).isEmpty());
        assertTrue(plan(snapshot, OperationTarget.empty(), request(1)).isEmpty());
        SurfaceAnalysis emptySurface = analyze(snapshot, OperationTarget.empty());
        assertThrows(IllegalArgumentException.class, () -> DisconnectedIslandCleanupPlanner.plan(
                snapshot(), emptySurface, SurfaceFeatureAnalyzer.analyze(emptySurface), request(1)));
        assertThrows(IllegalArgumentException.class, () -> DisconnectedIslandCleanupPlanner.plan(
                snapshot, SurfaceAnalysis.empty(), SurfaceFeatureAnalysis.empty(), request(1)));
    }

    @Test
    void immutableInputsAndInsertionOrderDoNotChangeOutput() {
        SchematicSnapshot source = snapshot(p(0, 0, 0), p(4, 0, 0));
        Map<GridPoint, BlockStateRef> before = Map.copyOf(source.blocks());
        ArrayList<GridPoint> keys = new ArrayList<>(source.blocks().keySet());
        Collections.reverse(keys);
        Map<GridPoint, BlockStateRef> reversed = new LinkedHashMap<>();
        for (GridPoint key : keys) reversed.put(key, source.blockAt(key));
        SchematicSnapshot copy = new SchematicSnapshot(reversed);
        reversed.clear();
        EnumSet<SurfaceFeatureKind> mutableKinds = EnumSet.of(SurfaceFeatureKind.TIP);
        IslandCleanupRequest request = new IslandCleanupRequest(1, EMPTY, mutableKinds);
        mutableKinds.add(SurfaceFeatureKind.ISOLATED);
        SurfaceAnalysis surface = analyze(source, target(AREA));
        SurfaceFeatureAnalysis features = SurfaceFeatureAnalyzer.analyze(surface);
        ChangeSet first = DisconnectedIslandCleanupPlanner.plan(source, surface, features, request);
        assertEquals(List.of(p(0, 0, 0), p(4, 0, 0)), positions(first));
        assertEquals(first, plan(copy, target(AREA), request));
        assertEquals(first, DisconnectedIslandCleanupPlanner.plan(source, surface, features, request));
        assertEquals(before, source.blocks());
        assertEquals(before, copy.blocks());
        assertThrows(UnsupportedOperationException.class, () -> request.protectedFeatureKinds().clear());
        assertThrows(UnsupportedOperationException.class, () -> surface.cells().clear());
        assertThrows(UnsupportedOperationException.class, () -> surface.components().clear());
        assertThrows(UnsupportedOperationException.class, () -> features.descriptors().clear());
        assertThrows(UnsupportedOperationException.class, () -> first.changes().clear());
    }

    @Test
    void invalidRequestsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> request(0));
        assertThrows(IllegalArgumentException.class, () -> request(-1));
        assertThrows(NullPointerException.class, () -> new IslandCleanupRequest(1, null, Set.of()));
        assertThrows(NullPointerException.class, () -> new IslandCleanupRequest(1, EMPTY, null));
    }

    private static ChangeSet plan(SchematicSnapshot snapshot, OperationTarget target, IslandCleanupRequest request) {
        SurfaceAnalysis surface = analyze(snapshot, target);
        return DisconnectedIslandCleanupPlanner.plan(snapshot, surface, SurfaceFeatureAnalyzer.analyze(surface), request);
    }

    private static SurfaceAnalysis analyze(SchematicSnapshot snapshot, OperationTarget target) {
        return SurfaceAnalyzer.analyze(snapshot, target, state -> !state.equals(EMPTY));
    }

    private static IslandCleanupRequest request(int maxSize) {
        return new IslandCleanupRequest(maxSize, EMPTY, Set.of());
    }

    private static List<GridPoint> positions(ChangeSet changes) {
        return changes.changes().stream().map(BlockChange::position).toList();
    }

    private static OperationTarget target(Region region) {
        return new OperationTarget(new Selection(List.of(new SelectionBox(region))),
                Optional.of(new PlacementTarget(List.of(region))));
    }

    private static SchematicSnapshot snapshot(GridPoint... occupied) {
        Map<GridPoint, BlockStateRef> blocks = new HashMap<>();
        for (int x = -1; x <= 10; x++) for (int y = -1; y <= 4; y++) for (int z = -1; z <= 4; z++) {
            blocks.put(p(x, y, z), EMPTY);
        }
        for (GridPoint position : occupied) blocks.put(position, SOLID);
        return new SchematicSnapshot(blocks);
    }

    private static GridPoint p(long x, long y, long z) {
        return new GridPoint(x, y, z);
    }
}
