# VoxelWeave Architecture

## Design objective

Keep Minecraft/Litematica-specific APIs at the boundary and keep the editing engine deterministic and testable.

## Layers

### 1. Integration layer

Responsibilities:

- Fabric client lifecycle
- keybind/menu entry points
- Litematica/MaLiLib API access
- conversion between external schematic representations and VoxelWeave domain objects
- read-only capture of the active bounded schematic target
- future write/export adapters behind explicit safety boundaries

Must not contain transformation policy beyond adapter-level mapping.

Current read path:

```text
Litematica selected placement + current selection
        ↓
LitematicaTargetMapping
        ↓
OperationTarget (world-space selection × placement)
        ↓
LitematicaBlockSnapshotAdapter
        ↓
SchematicSnapshotCapture
 ├─ OperationTarget
 └─ SchematicSnapshot
        ↓
EditWorkspace / analysis / transformation layers
```

`LitematicaBlockSnapshotAdapter` reads the schematic state as Litematica presents it in world space. This preserves placement/subregion rotation and mirror in directional block states without leaking Minecraft/Litematica types into pure domain code.

The schematic world can contain multiple placements. A captured coordinate is accepted only when it maps unambiguously to exactly one part of the selected placement and no foreign placement overlaps it. Ambiguous coordinates fail closed instead of silently producing a composite snapshot.

Capture includes air as well as non-air states for every visited coordinate inside the bounded target. Missing coordinates therefore mean "outside the captured data", not "air".

### 2. Workspace layer

Represents the user's current editing session:

- immutable source snapshot
- committed snapshot
- pending preview `ChangeSet`
- undo history
- redo history
- dirty state

The workspace makes source-vs-preview-vs-committed state explicit. Preview reads are overlay lookups over the pending `ChangeSet`; they do not copy the full schematic for every read.

### 3. Analysis layer

Pure structural analysis over bounded immutable schematic data. Analysis does not mutate the snapshot and does not produce a `ChangeSet` by itself.

Current flow:

```text
SchematicSnapshot + OperationTarget
        +
BlockOccupancyPolicy
        ↓
SurfaceAnalyzer
        ↓
SurfaceAnalysis
 ├─ SurfaceCell
 │   ├─ known exposed faces
 │   ├─ unknown faces
 │   ├─ occupied-neighbor count
 │   └─ component root
 └─ SurfaceComponent
     ├─ deterministic root
     ├─ target-local size
     └─ complete / incomplete connectivity
        ↓
SurfaceFeatureAnalyzer
        ↓
SurfaceFeatureAnalysis
 └─ SurfaceFeatureDescriptor
     ├─ feature kind
     ├─ discrete exposure vector
     ├─ exposed-axis count
     ├─ opposite-exposure pairs
     ├─ nearby surface-neighbor count
     └─ component-completeness context
```

Step 6A deliberately distinguishes **known empty** from **unknown/missing** neighbor data. A missing coordinate is never assumed to be air. This prevents selection/capture boundaries from being misclassified as exposed surface or tiny cleanup islands.

`BlockOccupancyPolicy` is injected by the caller so pure analysis code does not hard-code Minecraft air identifiers or parse Minecraft/Litematica block-state representations.

Step 6B adds local feature descriptors for `INTERIOR`, `FACE`, `EDGE`, `CORNER`, `THIN_FEATURE`, `TIP`, `ISOLATED` and `UNKNOWN_BOUNDARY`. Unknown-boundary classification has precedence over confident geometry classes. These descriptors are evidence for future preservation policy; they are not editing commands and do not encode smoothing strength.

The Step 6B exposure vector is discrete/sign-normalized local evidence, not a floating-point fitted normal. Larger-neighborhood curvature or continuous normal fitting can be added later without changing the Step 6A topology contract.

### 4. Transformation layer

Pure or near-pure operations over bounded schematic data.

Initial transformation:

```text
ReplaceBlocks(selection, fromBlock, toBlock)
```

`DisconnectedIslandCleanupPlanner` consumes a snapshot, its `SurfaceAnalysis`, the corresponding
`SurfaceFeatureAnalysis` and an `IslandCleanupRequest`. It reuses `smallIslandCandidates` and feature
evidence without new neighbor traversal. Unknown/incomplete components and any component containing
a protected feature are preserved as a whole. It returns only a `ChangeSet`, usable directly through
`EditWorkspace.preview(...)`; workspace commit/history behavior is unchanged.

Analyzer-produced results retain private references to their immutable sources. Cleanup requires the
exact snapshot instance and the exact surface instance from which the features were computed.
This constant-time provenance check includes capture halo changes and rejects unbound/manual evidence.
It avoids a second full-snapshot scan or workspace revision redesign; equal snapshot copies require
fresh analysis. See [surface analysis domain](SURFACE_ANALYSIS.md) for lifetime and target semantics.

Future transformations may include smoothing, dithering, connected-surface cleanup and contour correction,
but shape transforms should consume shared analysis results and follow the same command/change-set model.

### 5. History layer

Every committed edit produces a `ChangeSet` containing enough information to reverse the change.

Conceptually:

```text
TransformCommand
      ↓ evaluate
PreviewChangeSet
      ↓ commit
CommittedChangeSet
      ↕
 Undo / Redo
```

Avoid relying on re-running a non-deterministic transform to perform undo.

### 6. Export / write boundary

Writing back to Litematica and exporting `.litematic` files are reliability boundaries and are not implemented yet.

Preferred export flow:

```text
serialize candidate
      ↓
write temporary file
      ↓
validate readable structure / expected metadata
      ↓
finalize destination
      ↓
mark workspace saved
```

If any pre-finalization step fails, retain the original file and report the failure.

Do not silently overwrite the only known-good schematic.

### 7. UI layer

UI coordinates intent and presents state. It should not implement transformation algorithms.

Minimum UI concepts:

- selected schematic / region
- operation parameters
- preview status
- apply / cancel
- undo / redo
- export status and recovery messages

## Performance constraints

Large schematics are a primary use case.

- No full-schematic scan every render tick.
- Snapshot capture is explicit/on-demand and bounded to `OperationTarget.worldRegions()`.
- Surface and feature analysis are explicit/on-demand and should be cached by snapshot/workspace revision before any render-loop use.
- Overlapping target regions must not duplicate coordinate reads or analyzed cells.
- Cache integration lookups by chunk when practical.
- Cache derived palette/statistics/analysis data with clear invalidation.
- Prefer compact change sets over copying the full schematic for every history entry.
- If a transformation becomes expensive, separate computation from render/update scheduling while respecting Minecraft client thread safety.

## Error model

Distinguish at least:

- unsupported integration/version
- invalid or unavailable schematic
- invalid selection
- empty operation target
- ambiguous/overlapping schematic capture
- incomplete/unknown analysis boundary where an operation requires complete context
- invalid transform parameters
- transformation failure
- write/export/IO failure
- validation failure

Recoverable errors should return to a usable workspace rather than crash the client.

## Dependency rule

Direction should remain approximately:

```text
ui/client → workspace → analysis/transform/history/export-domain
integration → workspace/domain adapters
```

Pure domain/analysis/transform/history code must not depend directly on Minecraft rendering classes or Litematica APIs.

## Testing strategy

### Unit tests

- replacement behavior
- selection bounds
- empty/no-op transformations
- change-set inversion
- undo/redo ordering
- preview isolation
- surface exposed/interior/unknown semantics
- surface connected-component sizing/completeness
- local feature-kind classification and precedence
- discrete exposure vectors / axis and opposite-face evidence
- overlap deduplication and deterministic analysis ordering
- export policy and failure recovery logic when export is added

### Integration tests / smoke tests

- Litematica selection/placement mapping
- bounded snapshot capture from a representative `.litematic`
- air and directional block-state capture
- rotated/mirrored placement capture
- overlapping-placement rejection
- missing optional integration behavior
- future load/edit/save/reopen path

### Manual/dev-client verification

- supported Litematica/MaLiLib pair loads
- selected placement + area selection reports READY
- capture does not mutate schematic/world state
- large bounded capture remains responsive enough for editing workflows
- future preview rendering
- future actual export/reopen with representative converted models

Pull requests run JDK 25 GitHub Actions checks for pure tests, the default build and the Litematica-enabled build. Interactive Minecraft/Litematica smoke checks remain manual.

See [surface analysis domain](SURFACE_ANALYSIS.md) for topology, feature-descriptor and boundary semantics.
