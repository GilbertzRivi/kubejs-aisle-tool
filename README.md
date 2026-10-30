# Structure To KubeJs Aisles (1.21.1, NeoForge, GTM 8)

Build a multiblock in the world, select it with the Copy Tool, and paste the result into a KubeJS script.
Written in Hot Chocolate (`src/main/hotc`) with a little Java glue; originally by Oktawia.

## Using it

| Action | What it does |
| --- | --- |
| Sneak + right click a block | Corner 1 |
| Right click a block | Corner 2 |
| `[` | Switch right click between marking corners and marking the **controller** |
| Right click a block (controller mode) | Mark the controller. Sneak + right click unmarks it |
| `]` | Clear the corners and the controller |
| Right click air | Copy the selection to the clipboard |

The info above the hotbar shows the mode, the marks, the size and how many different block types the selection has (the limit is 91).
The selection is outlined in green (the controller in orange) and the outline also shows, fainter, through walls.

## The output

GTM 8 renamed `aisle` to `slice`, so the tool writes `.slice(...)` lines and one `.where(...)` per block:

```js
.slice("CCC", "CKC")
.slice("CCC", "CCC")

.where("K", Predicates.controller(Predicates.blocks(definition.get())))
.where("C", Predicates.blocks("gtceu:solid_machine_casing"))
```

* **Controller marked:** the controller becomes `K`, and the whole pattern is turned so it reads relative to the controller's
  facing (slices walk away from its front, rows go up, characters go right), which is how the pattern builder reads it. With no
  controller marked the tool assumes a controller facing south, which is the layout the 1.20.1 tool always wrote.
* **GTM hatches and buses** (`gtceu:..._input_bus`, `..._energy_input_hatch`, ...) become `Predicates.abilities(PartAbility.IMPORT_ITEMS)` and
  friends, and blocks that provide the same ability share one letter. The ability is guessed from the block id.

## Settings (client config, also in the mod's config screen)

* `letterCase` — `LOWERCASE_FIRST` (a to z, then A to Z) or `UPPERCASE_FIRST` (A to Z, then a to z).
* `air` — `SPACE` (GTM's "anything"), `AIR_PREDICATE` (`Predicates.air()`) or `BLOCK` (an ordinary block, as the 1.20.1 tool did).
* `gtmPartsAsAbilities` — write hatches and buses as abilities.
* `outputStyle` — `FRAGMENT` (slice and where lines) or `FULL_PATTERN` (the whole `.pattern(definition => ...)` call).

A result too long for the clipboard is written to `packet.txt` in the game folder and its path is copied instead.
