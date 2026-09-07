# Surface analysis and island cleanup (Steps 6A–7A)

VoxelWeave surface analysis is a pure, read-only layer over captured schematic data.
It does not smooth, delete, fill, recolor or otherwise mutate schematic data. Its job is to describe
voxel topology and local feature evidence once so later refinement tools can share the same structural model.

## Why this layer exists

Converted 3D models often contain several different kinds of geometry at the same time:

- broad intentional surfaces;
- ridges and corners;
- one-block-thin structural details;
- deliberate tips/spires;
- isolated conversion artifacts;
- disconnected islands;
- stair-step noise;
- selection/capture boundaries where neighboring schematic state is not known.

Applying one generic smoothing rule to all of those structures creates a recognizable procedural style
and can erase creator intent. VoxelWeave therefore separates **analysis evidence** from **shape mutation**.

## Step 6A: occupancy and six-neighbor topology

`SurfaceAnalyzer` does not hard-code Minecraft air names or parse Minecraft block-state strings.
Callers provide a pure `BlockOccupancyPolicy`:

```text
BlockStateRef -> occupied / empty
```

This keeps Minecraft/Litematica representation details at the integration boundary and keeps the
analysis layer independently testable.

Each occupied analyzed voxel becomes a `SurfaceCell`. The six cardinal faces are evaluated in stable order:

```text
DOWN, UP, NORTH, SOUTH, WEST, EAST
```

For each face the neighbor is one of three states:

- **occupied** — captured neighbor state exists and the occupancy policy says it contains geometry;
- **exposed** — captured neighbor state exists and the occupancy policy says it is empty;
- **unknown** — the snapshot has no data for that neighbor, or coordinate arithmetic cannot be represented.

Unknown is intentionally different from exposed. Missing snapshot data must never be silently treated as
air, because a block at the edge of a selection may continue into geometry that was not captured.

`SurfaceCell` exposes:

- deterministic exposed-face list;
- deterministic unknown-face list;
- occupied-neighbor count;
- component root;
- conservative predicates for interior, known surface, isolated and weakly-supported cells.

An isolated or weakly-supported classification is only returned when all six neighboring states are known.
Unknown-boundary cells are not automatically cleanup candidates.

## Connected components

Occupied cells inside the operation target are grouped using 6-connectivity. Each `SurfaceComponent` has:

- a deterministic root;
- size inside the operation target;
- a `complete` flag.

A component is incomplete when its connectivity touches unknown snapshot data or known occupied data
outside the operation target. Only complete components are eligible for `smallIslandCandidates(...)`.
This prevents a selection boundary from making a large structure look like a tiny disconnected island.

## Step 6B: feature-preservation descriptors

`SurfaceFeatureAnalyzer` consumes `SurfaceAnalysis` and produces one immutable
`SurfaceFeatureDescriptor` per analyzed occupied cell.

The local feature kinds are deliberately conservative:

```text
UNKNOWN_BOUNDARY
INTERIOR
ISOLATED
TIP
THIN_FEATURE
CORNER
EDGE
FACE
```

Classification precedence matters. `UNKNOWN_BOUNDARY` always wins: a cell with missing local context is
never upgraded to a confident face/edge/corner/tip class. `TIP` is evaluated before thin-feature logic so
a one-neighbor endpoint remains distinguishable from a one-block-thick sheet or ridge.

### Feature evidence

Each descriptor records:

- `kind` — conservative local feature class;
- `exposureVectorX/Y/Z` — sign-normalized sum of known exposed face directions;
- `exposedAxisCount` — how many independent X/Y/Z axes contain known exposure;
- `oppositeExposurePairCount` — axes where both opposing faces are exposed;
- `surfaceNeighborCount` — cardinal neighbors that are also known surface cells in the analyzed result;
- `componentComplete` — whether Step 6A proved the occupied component connectivity complete.

The exposure vector is intentionally **not** a floating-point surface normal. It is discrete evidence only.
For example:

```text
UP                     -> ( 0, 1, 0)
UP + EAST              -> ( 1, 1, 0)
UP + EAST + SOUTH      -> ( 1, 1, 1)
WEST + EAST            -> ( 0, 0, 0)  // opposing exposure cancels; thin-feature evidence remains
```

### Local feature interpretation

- **INTERIOR** — all six known neighbors occupied.
- **FACE** — known exposure exists on one axis without opposite exposure.
- **EDGE** — known exposure spans two independent axes.
- **CORNER** — known exposure spans all three axes.
- **THIN_FEATURE** — at least one axis exposes both opposite faces, indicating one-voxel-thick local geometry.
- **TIP** — exactly one occupied cardinal neighbor with complete local neighbor knowledge.
- **ISOLATED** — no occupied cardinal neighbors and all six neighbors known empty.
- **UNKNOWN_BOUNDARY** — any six-neighbor direction lacks captured context.

These are not final artistic labels and they do not decide how strongly a future tool edits a cell. They
provide stable evidence for later preservation policy, such as protecting a ridge or spire while allowing
conversion noise on a broad surface to be relaxed.

## Bounding and overlap semantics

Analysis only emits cells whose coordinates are inside `OperationTarget.worldRegions()`.
The input snapshot is keyed by world-space coordinate, so partially overlapping target regions do not
duplicate analyzed cells. Output topology cells, components and feature descriptors are sorted deterministically.

The analyzer may use captured neighbor data outside the target when that data is available, but it never
emits or mutates those outside-target coordinates. A future capture halo can therefore improve confidence
without changing the analysis API.

## Performance

Surface and feature analysis are explicit operations, not render-tick tasks. Step 6A scans captured
snapshot entries once and performs bounded six-neighbor passes. Step 6B consumes the already-produced
surface result and performs only local descriptor calculations.

Derived analysis should be cached by snapshot/workspace revision before any future render-loop use.

## Current scope boundary

Implemented through Step 6B:

- local six-face topology;
- exposed/interior/unknown distinction;
- conservative isolated/weak-support candidates;
- deterministic 6-connected component sizes and completeness;
- local face/edge/corner/thin-feature/tip/isolated/unknown-boundary descriptors;
- discrete exposure vector and local feature evidence.

Step 7A additionally implements conservative disconnected-island cleanup planning as described below.

Not implemented yet:

- larger-neighborhood curvature fitting;
- continuous or normalized surface-normal estimation;
- configurable feature-preservation strength;
- spike shortening and connected-surface cleanup;
- smoothing / relaxation;
- gap filling;
- contour rewriting;
- palette, gradient, pattern or dithering operations.

Later operations should consume this analysis instead of re-implementing neighbor semantics or making
destructive assumptions at incomplete boundaries.

## Step 7A: disconnected-island cleanup planning

`DisconnectedIslandCleanupPlanner.plan(snapshot, surface, features, request)` returns an immutable
`ChangeSet`. `IslandCleanupRequest` requires a positive `maxComponentSize`, a caller-supplied
`replacementState` and explicit `protectedFeatureKinds`. No Minecraft air state is assumed in the
pure layer. To plan removal, the caller supplies the state its occupancy policy considers empty.

The planner reuses `SurfaceAnalysis.smallIslandCandidates(...)`; it does not recompute connectivity.
A component is eligible only if it is complete and its size is at most the threshold. Any unknown
context or protected feature vetoes the whole component before changes are generated. Setting an
empty protection set does not permit removing unknown/incomplete components. Replacement no-ops
are omitted. Output is sorted, duplicate-free and restricted to the cells in the analyzed target;
outside-target snapshot/halo entries are never emitted.

### Source binding and stale analysis

Analyzer-generated `SurfaceAnalysis` retains a private reference to its exact immutable source
`SchematicSnapshot`; `SurfaceFeatureAnalysis` retains its exact source `SurfaceAnalysis`. Cleanup
checks both identities before producing any result, including an empty result. Changes to occupied
cells, formerly empty cells or outside-target halo data require a new snapshot and invalidate reuse.
Even an equal snapshot copy or another equivalent analysis requires recomputation. This is deliberately
conservative and costs constant time without hashing/scanning the full schematic again.

The analysis collections are now final immutable classes with the existing constructors/accessors
and structural equality. Public constructors and `empty()` create unbound evidence useful for
inspection/classification; the planner rejects it. Only the analyzers attach source bindings. Equality
compares evidence, not provenance, and must not be used as a stale-analysis check.

Keep snapshot, target and analysis together for a planning operation. A changed operation target
requires new surface and feature analysis; this planner always uses the scope recorded by its
surface cells. Analysis retains its snapshot while cached, so discard obsolete analysis together
with old workspace context. No global cache or persisted identifier is introduced.

### Existing workspace flow

```text
current workspace + surface + features + explicit cleanup request
    → EditWorkspace.previewIslandCleanup(...)
    → plan against committedSnapshot → store ChangeSet preview
    → existing workspace commit / undo / redo
```

Use the dedicated workspace operation for cleanup previews. It validates analysis against the
current `committedSnapshot` before replacing a pending preview, including when planning would
produce no changes. An intervening commit or undo/redo requires fresh analysis of the resulting
snapshot. For example, adding a neighbor invalidates an earlier isolated-component analysis even
when the component's own block states have not changed. Rejection leaves the workspace unchanged.

The low-level planner still returns an ordinary `ChangeSet` for inspection and pure tests; that
value does not retain analysis provenance. Do not cache it and replay it through generic
`preview(...)` on another workspace state. The dedicated operation keeps planning and preview
on the same immutable workspace; commit consumes that preview and undo/redo reuse existing history.

Planning does not apply changes. Workspace commit affects immutable workspace state only, not
Litematica or Minecraft. UI, write-back, export, smoothing, relaxation, connected-surface cleanup,
spike cleanup, contour movement and color tools remain outside Step 7A.
