package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import io.github.bosatsuking.voxelweave.domain.PlacementTarget;
import io.github.bosatsuking.voxelweave.domain.Region;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;
import io.github.bosatsuking.voxelweave.domain.Selection;
import io.github.bosatsuking.voxelweave.domain.SelectionBox;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static io.github.bosatsuking.voxelweave.analysis.ProtrusionTermination.*;
import static org.junit.jupiter.api.Assertions.*;

class ConnectedProtrusionAnalyzerTest {
    private static final BlockStateRef EMPTY = new BlockStateRef("test:empty");
    private static final BlockStateRef SOLID = new BlockStateRef("test:solid");
    private static final Region AREA = new Region(p(-10, -10, -10), p(200, 200, 200));

    @Test
    void oneBlockBumpRecordsSeparateAttachmentAndSupport() {
        Fixture fixture = fixture(bump(1));
        ProtrusionEvidence evidence = only(fixture.run(2));

        assertEquals(JUNCTION_REACHED, evidence.termination());
        assertEquals(List.of(p(0, 1, 0)), evidence.pathPositions());
        assertEquals(OptionalInt.of(1), evidence.exactPathLength());
        assertEquals(p(0, 0, 0), evidence.attachment().orElseThrow().position());
        assertEquals(Optional.of(VoxelFace.DOWN), evidence.attachmentDirection());
        assertEquals(List.of(VoxelFace.NORTH, VoxelFace.SOUTH, VoxelFace.WEST, VoxelFace.EAST),
                evidence.attachmentSupportFaces());
        assertEquals(SurfaceFeatureKind.TIP, evidence.path().getFirst().kind());
        assertEquals(SurfaceFeatureKind.FACE, evidence.attachment().orElseThrow().kind());
        assertEquals(-1, evidence.attachment().orElseThrow().exposureVectorY());
        assertEquals(5, evidence.attachment().orElseThrow().surfaceNeighborCount());
        assertSame(fixture.surface().components().getFirst(), evidence.component());
        assertSame(fixture.features().descriptorAt(p(0, 1, 0)).orElseThrow(), evidence.path().getFirst());
        assertTrue(evidence.contextIssues().isEmpty());
    }

    @Test
    void longSpireUsesExplicitBudgetAbove64WithoutInterpretingIntent() {
        ProtrusionEvidence spire = only(fixture(bump(70)).run(71));
        assertEquals(OptionalInt.of(70), spire.exactPathLength());
        assertEquals(0, spire.directionChangeCount());
        assertEquals(Collections.nCopies(70, VoxelFace.DOWN), spire.stepDirections());
        assertEquals(JUNCTION_REACHED, spire.termination());
    }

    @Test
    void sameGeometryHasSameEvidenceForDecorationNoiseAndNormalOffsetInterpretations() {
        // These interpretations are fixture rationale only; there is no cause/intent input or label.
        List<GridPoint> geometry = bump(1);
        List<ProtrusionEvidence> decoration = fixture(geometry).run(10).evidence();
        List<ProtrusionEvidence> apparentNoise = fixture(new ArrayList<>(geometry)).run(10).evidence();
        Collections.reverse(geometry);
        List<ProtrusionEvidence> normalOffsetLike = fixture(geometry).run(10).evidence();
        assertEquals(decoration, apparentNoise);
        assertEquals(decoration, normalOffsetLike);
    }

    @Test
    void bentDecorationRetainsUniquePathAndDirectionChanges() {
        List<GridPoint> shape = bump(2);
        shape.add(p(1, 2, 0));
        shape.add(p(2, 2, 0));
        ProtrusionEvidence evidence = only(fixture(shape).run(5));
        assertEquals(List.of(p(2, 2, 0), p(1, 2, 0), p(0, 2, 0), p(0, 1, 0)),
                evidence.pathPositions());
        assertEquals(List.of(VoxelFace.WEST, VoxelFace.WEST, VoxelFace.DOWN, VoxelFace.DOWN),
                evidence.stepDirections());
        assertEquals(1, evidence.directionChangeCount());
        assertEquals(OptionalInt.of(4), evidence.exactPathLength());
        assertEquals(SurfaceFeatureKind.THIN_FEATURE, evidence.path().get(2).kind());
    }

    @Test
    void thinBranchJunctionIsOnlyLocalSupportAndPreservesMultipleTips() {
        List<GridPoint> shape = List.of(p(0, 0, 0), p(-1, 0, 0), p(1, 0, 0), p(0, 1, 0));
        List<ProtrusionEvidence> evidence = fixture(shape).run(2).evidence();
        assertEquals(List.of(p(-1, 0, 0), p(0, 1, 0), p(1, 0, 0)),
                evidence.stream().map(ProtrusionEvidence::tipPosition).toList());
        for (ProtrusionEvidence trace : evidence) {
            assertEquals(JUNCTION_REACHED, trace.termination());
            assertEquals(4, trace.component().size());
            assertEquals(SurfaceFeatureKind.THIN_FEATURE, trace.attachment().orElseThrow().kind());
            assertEquals(2, trace.attachmentSupportFaces().size());
            assertEquals(1, trace.observedPathLength());
        }
    }

    @Test
    void cornerAndEdgeAttachmentsKeepTheirDescriptorsAndRoles() {
        List<GridPoint> corner = List.of(p(0, 0, 0), p(1, 0, 0), p(0, 1, 0), p(0, 0, 1));
        ProtrusionEvidence cornerTrace = at(fixture(corner).run(2), p(1, 0, 0));
        assertEquals(SurfaceFeatureKind.CORNER, cornerTrace.attachment().orElseThrow().kind());
        assertEquals(List.of(VoxelFace.UP, VoxelFace.SOUTH), cornerTrace.attachmentSupportFaces());
        List<GridPoint> edge = new ArrayList<>(corner);
        edge.add(p(-1, 0, 0));
        ProtrusionEvidence edgeTrace = at(fixture(edge).run(2), p(1, 0, 0));
        assertEquals(SurfaceFeatureKind.EDGE, edgeTrace.attachment().orElseThrow().kind());
        assertEquals(List.of(VoxelFace.UP, VoxelFace.SOUTH, VoxelFace.WEST), edgeTrace.attachmentSupportFaces());
        assertEquals(List.of(p(1, 0, 0)), edgeTrace.pathPositions());
    }

    @Test
    void featureKindsAreAttachedAsEvidenceRatherThanUsedAsTraversalControl() {
        Fixture fixture = fixture(bump(3));
        // A package-bound descriptor variant isolates traversal from classifier precedence.
        List<SurfaceFeatureDescriptor> variant = fixture.features().descriptors().stream().map(descriptor ->
                descriptor.position().equals(p(0, 2, 0))
                        ? new SurfaceFeatureDescriptor(descriptor.position(), SurfaceFeatureKind.EDGE,
                            descriptor.exposureVectorX(), descriptor.exposureVectorY(), descriptor.exposureVectorZ(),
                            descriptor.exposedAxisCount(), descriptor.oppositeExposurePairCount(),
                            descriptor.surfaceNeighborCount(), descriptor.componentComplete())
                        : descriptor).toList();
        SurfaceFeatureAnalysis alternative = SurfaceFeatureAnalysis.fromSurface(fixture.surface(), variant);
        ProtrusionEvidence actual = only(ConnectedProtrusionAnalyzer.analyze(fixture.snapshot(), fixture.surface(),
                alternative, new ProtrusionAnalysisRequest(4)));
        ProtrusionEvidence original = only(fixture.run(4));
        assertEquals(original.pathPositions(), actual.pathPositions());
        assertEquals(original.attachment(), actual.attachment());
        assertEquals(original.termination(), actual.termination());
        assertEquals(original.exactPathLength(), actual.exactPathLength());
        assertEquals(SurfaceFeatureKind.EDGE, actual.path().get(1).kind());
    }

    @Test
    void completeTipToTipRodIsCanonicalAndEmittedOnce() {
        ProtrusionEvidence rod = only(fixture(line(5)).run(5));
        assertEquals(OTHER_TIP_REACHED, rod.termination());
        assertEquals(line(5), rod.pathPositions());
        assertEquals(OptionalInt.of(5), rod.exactPathLength());
        assertTrue(rod.attachment().isEmpty());
        assertTrue(rod.attachmentDirection().isEmpty());
        assertTrue(rod.attachmentSupportFaces().isEmpty());
        assertEquals(Collections.nCopies(4, VoxelFace.EAST), rod.stepDirections());
    }

    @Test
    void twoAdjacentTipsAlsoCanonicalize() {
        ProtrusionEvidence rod = only(fixture(line(2)).run(2));
        assertEquals(OTHER_TIP_REACHED, rod.termination());
        assertEquals(line(2), rod.pathPositions());
    }

    @Test
    void overlappingPartialRodTracesAreNotMerged() {
        List<ProtrusionEvidence> traces = fixture(line(5)).run(4).evidence();
        assertEquals(2, traces.size());
        assertEquals(line(4), traces.getFirst().pathPositions());
        assertEquals(List.of(p(4, 0, 0), p(3, 0, 0), p(2, 0, 0), p(1, 0, 0)),
                traces.getLast().pathPositions());
        for (ProtrusionEvidence trace : traces) {
            assertEquals(TRACE_LIMIT_REACHED, trace.termination());
            assertTrue(trace.exactPathLength().isEmpty());
        }
    }

    @Test
    void junctionBudgetBeforeAtAndAfterArrivalCountsAttachmentVisit() {
        Fixture fixture = fixture(bump(3));
        ProtrusionEvidence before = only(fixture.run(3));
        assertEquals(TRACE_LIMIT_REACHED, before.termination());
        assertEquals(3, before.observedPathLength());
        assertTrue(before.exactPathLength().isEmpty());
        assertTrue(before.attachment().isEmpty());
        assertEquals(OptionalInt.of(3), only(fixture.run(4)).exactPathLength());
        assertEquals(only(fixture.run(4)), only(fixture.run(5)));
        assertEquals(TRACE_LIMIT_REACHED, only(fixture(bump(1)).run(1)).termination());
    }

    @Test
    void otherTipBudgetBeforeAtAndAfterArrival() {
        Fixture fixture = fixture(line(3));
        assertEquals(2, fixture.run(2).evidence().size());
        assertEquals(OTHER_TIP_REACHED, only(fixture.run(3)).termination());
        assertEquals(only(fixture.run(3)), only(fixture.run(4)));
    }

    @Test
    void unknownHaloStopsWithoutConfidentLengthOrAttachment() {
        SchematicSnapshot complete = snapshot(line(4));
        Map<GridPoint, BlockStateRef> blocks = new HashMap<>(complete.blocks());
        blocks.remove(p(1, 1, 0));
        ProtrusionEvidence trace = at(fixture(new SchematicSnapshot(blocks), target(AREA)).run(10), p(0, 0, 0));
        assertEquals(INCOMPLETE_CONTEXT, trace.termination());
        assertEquals(List.of(p(0, 0, 0), p(1, 0, 0)), trace.pathPositions());
        assertEquals(SurfaceFeatureKind.UNKNOWN_BOUNDARY, trace.path().getLast().kind());
        assertEquals(List.of(ProtrusionContextIssue.INCOMPLETE_COMPONENT, ProtrusionContextIssue.UNKNOWN_NEIGHBOR),
                trace.contextIssues());
        assertTrue(trace.exactPathLength().isEmpty());
        assertTrue(trace.attachment().isEmpty());
    }

    @Test
    void unknownSeedIsNotPromotedToTip() {
        Map<GridPoint, BlockStateRef> blocks = new HashMap<>(snapshot(line(2)).blocks());
        blocks.remove(p(0, 1, 0));
        blocks.remove(p(1, 1, 0));
        assertTrue(fixture(new SchematicSnapshot(blocks), target(AREA)).run(10).evidence().isEmpty());
    }

    @Test
    void contextLimitedTracesFromBothTipsAreNotCanonicalizedTogether() {
        Map<GridPoint, BlockStateRef> blocks = new HashMap<>(snapshot(line(5)).blocks());
        blocks.remove(p(2, 1, 0));
        List<ProtrusionEvidence> traces = fixture(new SchematicSnapshot(blocks), target(AREA)).run(10).evidence();
        assertEquals(2, traces.size());
        assertEquals(List.of(p(0, 0, 0), p(1, 0, 0), p(2, 0, 0)), traces.getFirst().pathPositions());
        assertEquals(List.of(p(4, 0, 0), p(3, 0, 0), p(2, 0, 0)), traces.getLast().pathPositions());
        for (ProtrusionEvidence trace : traces) {
            assertEquals(INCOMPLETE_CONTEXT, trace.termination());
            assertTrue(trace.exactPathLength().isEmpty());
        }
    }

    @Test
    void occupiedContinuationOutsideTargetIsNotAirAndIsNeverEmitted() {
        Region selection = new Region(p(0, 0, 0), p(2, 1, 1));
        ProtrusionEvidence trace = only(fixture(snapshot(line(4)), target(selection)).run(10));
        assertEquals(INCOMPLETE_CONTEXT, trace.termination());
        assertEquals(List.of(p(0, 0, 0), p(1, 0, 0)), trace.pathPositions());
        assertTrue(trace.contextIssues().contains(ProtrusionContextIssue.OCCUPIED_NEIGHBOR_OUTSIDE_TARGET));
        assertTrue(trace.pathPositions().stream().allMatch(selection::contains));
        assertTrue(trace.exactPathLength().isEmpty());
    }

    @Test
    void incompleteComponentElsewherePreventsConfidentJunctionEvenWithKnownLocalPath() {
        Map<GridPoint, BlockStateRef> blocks = new HashMap<>(snapshot(bump(2)).blocks());
        blocks.remove(p(2, -1, 2));
        Fixture fixture = fixture(new SchematicSnapshot(blocks), target(AREA));
        ProtrusionEvidence trace = only(fixture.run(3));
        assertEquals(INCOMPLETE_CONTEXT, trace.termination());
        assertEquals(List.of(ProtrusionContextIssue.INCOMPLETE_COMPONENT), trace.contextIssues());
        assertTrue(trace.attachment().isPresent());
        assertTrue(trace.exactPathLength().isEmpty());
        assertFalse(trace.component().complete());
        // Component incompleteness also takes precedence over a budget stop.
        assertEquals(INCOMPLETE_CONTEXT, only(fixture.run(1)).termination());
    }

    @Test
    void outsideTargetSupportAtJunctionIsRetainedOnlyAsIncompleteEvidence() {
        Region selected = new Region(p(0, 0, 0), p(1, 2, 1));
        ProtrusionEvidence trace = only(fixture(snapshot(bump(1)), target(selected)).run(2));
        assertEquals(INCOMPLETE_CONTEXT, trace.termination());
        assertEquals(p(0, 0, 0), trace.attachment().orElseThrow().position());
        assertTrue(trace.contextIssues().contains(ProtrusionContextIssue.OCCUPIED_NEIGHBOR_OUTSIDE_TARGET));
        assertTrue(trace.exactPathLength().isEmpty());
    }

    @Test
    void coordinateOverflowRemainsDistinctFromIncompleteComponentAndUnknownHalo() {
        long min = Long.MIN_VALUE;
        List<GridPoint> shape = List.of(p(min, 0, 0), p(min + 1, 0, 0));
        Region region = new Region(p(min, 0, 0), p(min + 2, 1, 1));
        ProtrusionEvidence trace = only(fixture(snapshot(shape), target(region)).run(2));
        assertEquals(COORDINATE_OVERFLOW, trace.termination());
        assertEquals(List.of(p(min + 1, 0, 0), p(min, 0, 0)), trace.pathPositions());
        assertEquals(List.of(ProtrusionContextIssue.INCOMPLETE_COMPONENT, ProtrusionContextIssue.COORDINATE_OVERFLOW),
                trace.contextIssues());
        assertTrue(trace.exactPathLength().isEmpty());
    }

    @Test
    void diagonalVisualContactDoesNotConnectRods() {
        List<GridPoint> shape = List.of(p(0, 0, 0), p(1, 0, 0), p(2, 1, 0), p(3, 1, 0));
        List<ProtrusionEvidence> traces = fixture(shape).run(10).evidence();
        assertEquals(2, traces.size());
        assertEquals(List.of(p(0, 0, 0), p(1, 0, 0)), traces.getFirst().pathPositions());
        assertEquals(List.of(p(2, 1, 0), p(3, 1, 0)), traces.getLast().pathPositions());
        for (ProtrusionEvidence trace : traces) {
            assertEquals(OTHER_TIP_REACHED, trace.termination());
            assertEquals(2, trace.component().size());
        }
    }

    @Test
    void thinShellWithoutTipsIsOutOfModel() {
        List<GridPoint> shell = new ArrayList<>();
        for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) for (int z = 0; z < 3; z++) {
            if (x != 1 || y != 1 || z != 1) shell.add(p(x, y, z));
        }
        assertTrue(fixture(shell).run(64).evidence().isEmpty());
    }

    @Test
    void thicknessTransitionRecordsFirstJunctionWithoutInferringCause() {
        List<GridPoint> shape = new ArrayList<>(line(4));
        shape.addAll(List.of(p(3, 1, 0), p(4, 0, 0), p(4, 1, 0)));
        ProtrusionEvidence trace = only(fixture(shape).run(4));
        assertEquals(JUNCTION_REACHED, trace.termination());
        assertEquals(line(3), trace.pathPositions());
        assertEquals(p(3, 0, 0), trace.attachment().orElseThrow().position());
        assertEquals(List.of(VoxelFace.UP, VoxelFace.EAST), trace.attachmentSupportFaces());
    }

    @Test
    void stairSteppedSlopePreservesFaceConnectedTurns() {
        List<GridPoint> stairs = List.of(p(0, 0, 0), p(1, 0, 0), p(1, 1, 0), p(2, 1, 0), p(2, 2, 0));
        ProtrusionEvidence trace = only(fixture(stairs).run(5));
        assertEquals(OTHER_TIP_REACHED, trace.termination());
        assertEquals(stairs, trace.pathPositions());
        assertEquals(3, trace.directionChangeCount());
        assertEquals(OptionalInt.of(5), trace.exactPathLength());
    }

    @Test
    void wideProtrusionRidgeAndPlaneWithoutTipsDoNotProduceArtifactFreeLabels() {
        List<GridPoint> plane = bump(0);
        assertTrue(fixture(plane).run(64).evidence().isEmpty());
        List<GridPoint> ridge = new ArrayList<>(plane);
        for (int x = -2; x <= 2; x++) ridge.add(p(x, 1, 0));
        assertTrue(fixture(ridge).run(64).evidence().isEmpty());
        List<GridPoint> wide = new ArrayList<>(plane);
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) for (int y = 1; y < 5; y++) {
            wide.add(p(x, y, z));
        }
        assertTrue(fixture(wide).run(64).evidence().isEmpty());
    }

    @Test
    void orderingOverlapAndRepeatedAnalysisAreDeterministic() {
        List<GridPoint> shape = List.of(p(0, 0, 0), p(-1, 0, 0), p(1, 0, 0), p(0, 1, 0));
        SchematicSnapshot snapshot = snapshot(shape);
        ArrayList<GridPoint> keys = new ArrayList<>(snapshot.blocks().keySet());
        Collections.reverse(keys);
        Map<GridPoint, BlockStateRef> reverse = new LinkedHashMap<>();
        for (GridPoint key : keys) reverse.put(key, snapshot.blockAt(key));
        OperationTarget overlap = new OperationTarget(
                new Selection(List.of(new SelectionBox(AREA), new SelectionBox(AREA))),
                Optional.of(new PlacementTarget(List.of(AREA, AREA))));
        Fixture first = fixture(snapshot, target(AREA));
        Fixture second = fixture(new SchematicSnapshot(reverse), overlap);
        assertEquals(first.run(10).evidence(), second.run(10).evidence());
        assertEquals(first.run(10).evidence(), first.run(10).evidence());
        assertEquals(3, second.run(10).evidence().size());
    }

    @Test
    void staleEqualSnapshotOtherSurfaceAndUnboundInputsAreRejectedEvenWhenEmpty() {
        for (List<GridPoint> geometry : List.of(line(3), List.<GridPoint>of())) {
            Fixture fixture = fixture(geometry);
            SchematicSnapshot equalCopy = new SchematicSnapshot(fixture.snapshot().blocks());
            SurfaceAnalysis other = surface(fixture.snapshot(), target(AREA));
            ProtrusionAnalysisRequest request = new ProtrusionAnalysisRequest(10);
            assertEquals(fixture.snapshot(), equalCopy);
            assertEquals(fixture.surface(), other);
            assertThrows(IllegalArgumentException.class, () -> ConnectedProtrusionAnalyzer.analyze(
                    equalCopy, fixture.surface(), fixture.features(), request));
            assertThrows(IllegalArgumentException.class, () -> ConnectedProtrusionAnalyzer.analyze(
                    fixture.snapshot(), fixture.surface(), SurfaceFeatureAnalyzer.analyze(other), request));
            SurfaceAnalysis unbound = new SurfaceAnalysis(fixture.surface().cells(), fixture.surface().components());
            assertThrows(IllegalArgumentException.class, () -> ConnectedProtrusionAnalyzer.analyze(
                    fixture.snapshot(), unbound, SurfaceFeatureAnalyzer.analyze(unbound), request));
            assertThrows(IllegalArgumentException.class, () -> ConnectedProtrusionAnalyzer.analyze(
                    fixture.snapshot(), fixture.surface(), new SurfaceFeatureAnalysis(fixture.features().descriptors()), request));
            ConnectedProtrusionAnalysis result = fixture.run(10);
            assertTrue(result.isFrom(fixture.snapshot(), fixture.surface(), fixture.features()));
            assertFalse(result.isFrom(equalCopy, fixture.surface(), fixture.features()));
            assertFalse(result.isFrom(fixture.snapshot(), other, fixture.features()));
            assertFalse(result.isFrom(fixture.snapshot(), fixture.surface(), SurfaceFeatureAnalyzer.analyze(fixture.surface())));
            assertFalse(result.isFrom(null, null, null));
        }
    }

    @Test
    void changedHaloSnapshotInvalidatesBinding() {
        Fixture original = fixture(line(3));
        Map<GridPoint, BlockStateRef> blocks = new HashMap<>(original.snapshot().blocks());
        blocks.remove(p(0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> ConnectedProtrusionAnalyzer.analyze(
                new SchematicSnapshot(blocks), original.surface(), original.features(), new ProtrusionAnalysisRequest(3)));
    }

    @Test
    void inputsOutputsAndDerivedCollectionsAreImmutable() {
        Fixture fixture = fixture(bump(2));
        Map<GridPoint, BlockStateRef> before = Map.copyOf(fixture.snapshot().blocks());
        List<SurfaceCell> cells = List.copyOf(fixture.surface().cells());
        List<SurfaceFeatureDescriptor> features = List.copyOf(fixture.features().descriptors());
        ConnectedProtrusionAnalysis result = fixture.run(3);
        ProtrusionEvidence trace = only(result);
        assertEquals(before, fixture.snapshot().blocks());
        assertEquals(cells, fixture.surface().cells());
        assertEquals(features, fixture.features().descriptors());
        assertEquals(new ProtrusionAnalysisRequest(3), result.request());
        assertThrows(UnsupportedOperationException.class, () -> result.evidence().clear());
        assertThrows(UnsupportedOperationException.class, () -> trace.path().clear());
        assertThrows(UnsupportedOperationException.class, () -> trace.pathPositions().clear());
        assertThrows(UnsupportedOperationException.class, () -> trace.stepDirections().clear());
        assertThrows(UnsupportedOperationException.class, () -> trace.attachmentSupportFaces().clear());
        assertThrows(UnsupportedOperationException.class, () -> trace.contextIssues().clear());
        ArrayList<SurfaceFeatureDescriptor> path = new ArrayList<>(trace.path());
        ArrayList<VoxelFace> support = new ArrayList<>(trace.attachmentSupportFaces());
        ProtrusionEvidence copy = new ProtrusionEvidence(trace.component(), path, trace.attachment(), support,
                trace.termination(), trace.contextIssues());
        path.clear();
        support.clear();
        assertEquals(trace, copy);
    }

    @Test
    void positiveBudgetsHaveNo64MaximumOrEagerBudgetSizedAllocation() {
        assertThrows(IllegalArgumentException.class, () -> new ProtrusionAnalysisRequest(0));
        assertThrows(IllegalArgumentException.class, () -> new ProtrusionAnalysisRequest(-1));
        assertEquals(65, new ProtrusionAnalysisRequest(65).maxTraceLength());
        assertEquals(OptionalInt.of(2), only(fixture(line(2)).run(Integer.MAX_VALUE)).exactPathLength());
    }

    private record Fixture(SchematicSnapshot snapshot, SurfaceAnalysis surface, SurfaceFeatureAnalysis features) {
        ConnectedProtrusionAnalysis run(int limit) {
            return ConnectedProtrusionAnalyzer.analyze(snapshot, surface, features, new ProtrusionAnalysisRequest(limit));
        }
    }

    private static Fixture fixture(List<GridPoint> occupied) {
        return fixture(snapshot(occupied), target(AREA));
    }

    private static Fixture fixture(SchematicSnapshot snapshot, OperationTarget target) {
        SurfaceAnalysis surface = surface(snapshot, target);
        return new Fixture(snapshot, surface, SurfaceFeatureAnalyzer.analyze(surface));
    }

    private static SurfaceAnalysis surface(SchematicSnapshot snapshot, OperationTarget target) {
        return SurfaceAnalyzer.analyze(snapshot, target, state -> !state.equals(EMPTY));
    }

    private static ProtrusionEvidence only(ConnectedProtrusionAnalysis result) {
        assertEquals(1, result.evidence().size());
        return result.evidence().getFirst();
    }

    private static ProtrusionEvidence at(ConnectedProtrusionAnalysis result, GridPoint seed) {
        return result.evidence().stream().filter(trace -> trace.tipPosition().equals(seed)).findFirst().orElseThrow();
    }

    private static List<GridPoint> bump(int height) {
        List<GridPoint> shape = new ArrayList<>();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) shape.add(p(x, 0, z));
        for (int y = 1; y <= height; y++) shape.add(p(0, y, 0));
        return shape;
    }

    private static List<GridPoint> line(int length) {
        List<GridPoint> shape = new ArrayList<>();
        for (int x = 0; x < length; x++) shape.add(p(x, 0, 0));
        return shape;
    }

    private static SchematicSnapshot snapshot(List<GridPoint> occupied) {
        Map<GridPoint, BlockStateRef> blocks = new HashMap<>();
        for (GridPoint point : occupied) {
            for (VoxelFace face : VoxelFace.values()) face.neighborOf(point).ifPresent(neighbor -> blocks.put(neighbor, EMPTY));
        }
        for (GridPoint point : occupied) blocks.put(point, SOLID);
        return new SchematicSnapshot(blocks);
    }

    private static OperationTarget target(Region region) {
        return new OperationTarget(new Selection(List.of(new SelectionBox(region))),
                Optional.of(new PlacementTarget(List.of(region))));
    }

    private static GridPoint p(long x, long y, long z) {
        return new GridPoint(x, y, z);
    }
}
