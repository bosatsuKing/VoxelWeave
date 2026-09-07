# Selection and snapshot domain (Steps 2–5)

## World-space geometry

`domain` uses immutable integer `GridPoint` coordinates (`long`) and non-empty
`Region` bounds: `[minInclusive, maxExclusive)` on all axes. `Region.between`
normalizes half-open boundary corners; equal boundaries and empty intersections
return `Optional.empty()`. It never converts block-inclusive endpoints.

`Selection` contains complete `SelectionBox` snapshots. `PlacementTarget` contains
enabled subregions in world space. `OperationTarget` retains both and computes
their pairwise intersections. Lists are immutable, sorted lexicographically and
exact duplicates are removed. Gaps in either selection or placement remain gaps.
Partial overlaps remain a union and every block traversal must deduplicate visited
coordinates. Empty selection, missing/disabled placement, or disjoint bounds grant
no regions. No placement names, identifiers, paths or live integration objects are
retained in the pure domain model.

The client adapter reads the selected placement and current area selection on
demand. It includes all complete boxes in the current selection, not only the
currently highlighted box. Incomplete boxes are ignored. Litematica 0.27.12
`getSubRegionBoxes(PLACEMENT_ENABLED)` provides inclusive world-space bounds with
placement and subregion rotation/mirror applied. `WorldBoundsMapping` normalizes
endpoints and widens before adding one. It can represent a block at
`Integer.MAX_VALUE` without overflow.

Before upstream integer transforms, a conservative magnitude bound checks that
origin + transformed relative position + transformed extent cannot overflow on
any axis. Zero sizes, missing sizes and unsafe magnitudes fail closed through the
existing readable integration-error boundary. This may reject extreme placements
whose signed arithmetic would actually fit; it never accepts wrapped bounds.

## Bounded block snapshot capture

Step 5 adds a read-only bridge from the validated `OperationTarget` to
`SchematicSnapshot`:

```text
selected placement + current selection
        ↓
OperationTarget.worldRegions()
        ↓
LitematicaBlockSnapshotAdapter
        ↓
SchematicSnapshotCapture
 ├─ target
 └─ snapshot
```

Capture semantics:

- only coordinates inside `OperationTarget.worldRegions()` are read;
- overlapping target regions are deduplicated so each world coordinate is read once;
- air is captured explicitly, just like non-air block states;
- a missing coordinate in `SchematicSnapshot` therefore means it was outside the
  captured data, not that it was air;
- block states are captured as Litematica presents them in world space, so
  placement/subregion rotation and mirror are already reflected in directional
  block-state identity;
- the capture retains no live Minecraft/Litematica objects after mapping.

The current adapter uses Litematica's schematic world for world-oriented block
state reads. Because that world can contain more than one placement, each captured
coordinate is validated first. Capture requires exactly one matching part from the
selected placement and no overlapping foreign placement. Multiple overlapping
subregions of the selected placement are also treated as ambiguous. Ambiguity
fails closed instead of silently mixing schematic-world data.

The returned `SchematicSnapshotCapture` can be passed directly into the existing
pure editing flow:

```text
capture.snapshot()
        ↓
EditWorkspace.start(...)
        ↓
previewReplacement(capture.target(), request)
        ↓
preview → commit → undo / redo
```

This is still a read-only pipeline. `commitPreview()` commits only to VoxelWeave's
immutable workspace state; it does not mutate a Litematica schematic or the
Minecraft world.

## Staleness and threading

Selection geometry and block snapshots are point-in-time values. They become stale
when the Litematica selection, placement, schematic contents, rotation/mirror or
other relevant integration state changes. Future write-back must revalidate its
source/target before mutating external data.

Capture is intended to run explicitly on the Minecraft client thread and must not
be placed on a render-tick full-schematic loop. Large schematics should be handled
through bounded selections and future scheduling/progress mechanisms rather than
unbounded per-frame scans.

## Verification

Pure domain/transformation/history tests do not import Minecraft, Litematica or
Fabric types. Litematica snapshot capture is an integration boundary and requires
a dev-client smoke test with the pinned dependencies.

For code/build changes, run the automated gates:

```text
./gradlew test
./gradlew build
./gradlew build -PwithLitematica=true
```

For changes affecting Litematica capture/placement mapping or Minecraft renderer/UI
behavior, and when required by the active acceptance criteria, also run a dev-client
smoke test with the pinned dependencies:

```text
./gradlew runClient -PwithLitematica=true
```

Pure-only changes do not require client launch unless they affect an integration
contract. Record the observed behavior for each required case; client startup alone
does not establish that an interactive or UI acceptance criterion passed.

Representative capture/mapping cases should include air, directional blocks, rotated or
mirrored placements, multiple selection boxes and deliberate placement overlap to
confirm fail-closed behavior.
