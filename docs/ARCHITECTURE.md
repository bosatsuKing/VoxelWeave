# VoxelWeave Architecture

## Design objective

Keep Minecraft/Litematica-specific APIs at the boundary and keep the editing engine deterministic and testable.

## Proposed layers

### 1. Integration layer

Responsibilities:

- Fabric client lifecycle
- keybind/menu entry points
- Litematica/MaLiLib API access
- conversion between external schematic representations and VoxelWeave domain objects

Must not contain transformation policy beyond adapter-level mapping.

### 2. Workspace layer

Represents the user's current editing session:

- source schematic identity
- selected region
- palette snapshot
- pending preview
- committed VoxelWeave operations
- dirty/saved state

The workspace should make source-vs-preview-vs-committed state explicit.

### 3. Transformation layer

Pure or near-pure operations over bounded schematic data.

Initial transformation:

```text
ReplaceBlocks(selection, fromBlock, toBlock)
```

Future transformations may include smoothing, dithering, noise cleanup and contour correction, but they should follow the same command/change-set model.

### 4. History layer

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

### 5. Export layer

Export is a reliability boundary.

Preferred flow:

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

### 6. UI layer

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
- Bound operations to explicit regions.
- Cache derived palette/statistics data with clear invalidation.
- Prefer compact change sets over copying the full schematic for every history entry.
- If a transformation becomes expensive, separate computation from render/update scheduling while respecting Minecraft client thread safety.

## Error model

Distinguish at least:

- unsupported integration/version
- invalid or unavailable schematic
- invalid selection
- invalid transform parameters
- transformation failure
- export/IO failure
- validation failure

Recoverable errors should return to a usable workspace rather than crash the client.

## Dependency rule

Direction should remain approximately:

```text
ui/client → workspace → transform/history/export-domain
integration → workspace/domain adapters
```

Pure domain/transform/history code must not depend directly on Minecraft rendering classes.

## Testing strategy

### Unit tests

- replacement behavior
- selection bounds
- empty/no-op transformations
- change-set inversion
- undo/redo ordering
- export policy and failure recovery logic

### Integration tests

- Litematica adapter mapping
- load/edit/save/reopen path
- missing optional integration behavior

### Manual/dev-client verification

- UI behavior
- preview rendering
- large schematic responsiveness
- actual export/reopen with representative converted models
