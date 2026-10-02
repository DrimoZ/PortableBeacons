# Porting to the 26.x line

Written before any code was touched. Its job is to say what the port actually is, so the decision
to start it — or not to — is made with the size known rather than discovered halfway.

Researched 17 August 2026 against the NeoForge primers, the NeoForged release notes and the
dependency listings. Facts fetched from those sources are marked as such; everything else is my
reading of this repository against them, and should be treated as an estimate until the compiler
disagrees.

---

## 1. What "26.1" is

Mojang moved to `year.drop.hotfix` versioning in December 2025. There is no 1.22 — the line
continues as 26.1, 26.2, and so on.

| | |
|---|---|
| **26.1** "Tiny Takeover" | released 24 March 2026 |
| **26.2** "Chaos Cubed" | released 16 June 2026 |
| 26.3 | announced, not out |

NeoForge versions gained a fourth component to match: `26.1.2.95` means Minecraft 26.1 hotfix 2,
NeoForge build 95. Latest as of today: **26.1.2.95** on the 26.1 line, **26.2.0.60** on 26.2.

**This matters for the target.** 26.1 has been superseded for two months. Porting to it means
shipping onto a version that is already one drop behind on the day it releases — and 26.1 → 26.2 is
one further primer on top of the ten below, not a tenth of the work. See §6.

## 2. This is not a version bump

The mod is on **1.21.1**. The path to 26.1 crosses ten primers:

`1.21.2` · `1.21.4` · `1.21.5` · `1.21.6` · `1.21.7` · `1.21.8` · `1.21.9` · `1.21.10` · `1.21.11` · `26.1`

1.21.1 was the long-lived modding anchor, so almost nothing in the ecosystem moved for a year and
then everything moved at once. The changes are not spread evenly across those ten: three of them
(1.21.2, 1.21.4, 26.1) carry most of the damage, and all three land on this mod.

## 3. Toolchain

Verified from the NeoForge 26.1 release notes.

| | now | 26.1 |
|---|---|---|
| Java | 21 | **25** |
| Gradle | 8.8 | **9.1+** |
| ModDevGradle | 2.0.78 | **2.0.141** |
| Parchment | pinned to 1.21.1 | **removed** — Mojang dropped obfuscation, the real parameter names ship |

Local JDK is already 26.0.1, so the toolchain is not a blocker. Parchment going away is a small
gift: one less mapping to keep in sync, and the names in the decompiled source become authoritative.

## 4. What breaks, by file

Ordered by cost, not by directory.

### The screen — the whole cost of this port

`client/PortableBeaconScreen.java`, 1423 lines, 97 draw calls.

Three separate refactors land on it:

- **1.21.4** reorganised the widget hierarchy (`AbstractScrollWidget` split, `AbstractSelectionList`
  scroll methods renamed). The effect picker is a scrollable searchable list — it sits directly on
  the part that moved.
- **1.21.5** replaced the rendering backend wholesale: shader JSONs became in-code `RenderPipeline`
  objects, OpenGL is abstracted behind `GpuDevice` / `CommandEncoder` / `RenderPass`.
- **26.1** split drawing into two phases. `GuiGraphics` became `GuiGraphicsExtractor`, and
  `Screen#render` became `Screen#extractRenderState`. Drawing no longer happens inline during
  render; a screen now *describes* what to draw and the engine renders it afterwards.

That last one is the one that hurts. The drawer animation reads a clock and blits at an interpolated
width inside the render call — that is exactly the shape the new model removes. The screen has to be
restructured around state extraction, not patched. **Assume it is rewritten.** Realistically this is
the majority of the port's hours, and everything else in this document is small next to it.

The good news is that it is 1423 lines of *layout*, and layout survives: the coordinates, the tab
geometry, the animation curve and the decisions that took several rounds of your feedback to get
right are all still correct. It is the plumbing under them that is gone.

### Items, models and datagen

- **`registry/BPItems.java`** — 1.21.2 made `Item.Properties#setId` mandatory:
  `new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ...))`. Mechanical, every item.
- **`datagen/BPItemModelProvider.java`** — 1.21.4 replaced the item model system entirely. Models
  now live in `assets/<ns>/items/` as `ClientItem` definitions, `net.minecraft.data.models.*` moved
  to `net.minecraft.client.data.models.*`, and `ItemModelGenerator` is gone. This provider is
  rewritten, not adjusted. It is only 53 lines and the models are generated rather than hand-drawn,
  so the cost is learning the new system once.
- **`datagen/BPRecipeProvider.java` + `ComponentShapedRecipe.java`** — 26.1 turned `RecipeSerializer`
  into a record of `MapCodec` + `StreamCodec`, killing the separate serializer class, and
  `Recipe#assemble` lost its `HolderLookup$Provider`. `ComponentShapedRecipe` is a custom recipe
  type, so it takes this change head-on. 51 lines, and the codec is already written — mostly
  reshaping.
- **`datagen/BPAdvancementProvider.java`** — advancement display now takes `ItemStackTemplate`
  instead of `ItemStack`. Small.

### Data components and state

`registry/BPComponents.java`, `core/PackState.java`, `core/AugmentInstance.java`.

26.1 changed *when* components initialise: they now come up at resource reload rather than at
construction, via `Item.Properties#delayedComponent` / `delayedHolderComponent`. And `ItemStack` is
being split — an immutable `ItemStackTemplate` for data files, with both it and `ItemStack`
implementing a new `ItemInstance` interface.

`PackState` and its codec are the single place serialization lives, which was the point of writing
them that way. That bet gets tested here. Expect real work but confined to two or three files.

### Datapack registries

`registry/BPDatapackRegistries.java` and the `core/*Def` codecs.

26.1 moved registries to holding `MapCodec` directly (`getType()` → `codec()`) and introduced a
`Validatable` interface — `validate(ValidationContext)` — replacing ad-hoc validation. `FuelDef`
currently validates through `Codec#validate` for its exactly-one-of-item-or-tag rule; that becomes
the new interface. Moderate, and the JSON format the datapacks use should be unaffected — **the
registries stay compatible for datapack authors**, which is worth saying on the store page.

### The parts that should be nearly free

- **`core/`** — 13 files of pure model and arithmetic, touching only `Holder`, `TagKey`,
  `BuiltInRegistries` and `Codec`. This is the layer written to survive exactly this, and it should.
  If it doesn't, the architecture was wrong and that is worth knowing.
- **`net/`** — the payload API has been stable since 1.20.5. Low risk.
- **`menu/`** — `AbstractContainerMenu` and `Slot` are not called out in any primer I read. Assume
  low, verify early, because the curio sentinel path is subtle enough that a silent behaviour change
  would be expensive to find twice.
- **lang, tags, advancement JSON** — carry over.

### Gametests

`gametest/BPGameTests.java`. The framework moved during the 1.21.x run; I have not pinned down where
or how much. Unknown, and it is the thing that proves the rest of the port, so it should be checked
before the port is planned in detail rather than after.

## 5. Dependencies

| | 1.21.1 today | 26.1 | |
|---|---|---|---|
| **Curios** | 9.5.1 | **15.0.0+26.1.2**, stable, 19 July 2026 | six major versions — the compat layer is small but it is being rewritten against an API I have not read |
| **JEI** | 19.44 | **29.5.0.28** for 26.1.2 | ten major versions. `IRecipeCategory` / `ISubtypeInterpreter` will not have survived unchanged |
| **EMI** | 1.1.24 | **none** | no 26.x NeoForge build on either CurseForge or Modrinth; the newest is still 1.1.24+1.21.1, from May 2026 |

EMI is the one that changes scope rather than cost: with no 26.x NeoForge build to compile against,
the plugin cannot come along. It is dropped from the 26.x files and re-added if and when one
appears — the source stays in the repository, since deleting it would only mean writing it again.
That is not worth delaying the port over, JEI covers the same need, but the 26.x files must stop
listing EMI as an optional dependency or players will read it as broken rather than absent.

## 6. The path: 26.1, then 26.2, then 26.3

Settled: **26.1 first and shipped, 26.2 next, 26.3 when it lands.** Not a single jump to whatever is
current.

The reasoning is that the ten primers between 1.21.1 and 26.1 are the wall, and the wall does not
get shorter by aiming further past it — but it does get harder to debug. Landing on 26.1 first means
the screen rewrite is verified against one target before 26.2's delta is stacked on top of it. When
something renders wrong, the question is *which of the ten changes did that*, and adding an eleventh
to the same failure makes it strictly worse.

It also means each drop gets a file rather than only the newest one, which is what actually serves
players and modpack authors — a pack pinned to 26.1 is not helped by a 26.2-only build.

The cost is real and worth stating: three ports instead of one, and the two follow-on hops are only
cheap if the first one leaves the code in good shape. If 26.1 → 26.2 turns out to be another wall
rather than a step, that is the moment to reconsider, not now.

## 7. Feasibility

**Feasible, and worth doing — but it is a port, not a bump.** The honest shape of it:

- roughly half the work is one file, and that file is being rewritten rather than migrated
- roughly a quarter is datagen and registration, which is mechanical once the new APIs are read
- roughly a quarter is the three compat layers, one of which may simply be dropped
- `core/`, which is the actual mod, should come through nearly untouched

The one caution worth keeping in view: 1.0.0 shipped days ago and has no player reports yet, and a
bug found on 1.21.1 is much cheaper to fix on one branch than on three. That is an argument for
keeping `1.21.1` genuinely maintained, not an argument against starting — the 1.21.1 ecosystem is
where it will stay, and every month there is a month of players who cannot install the mod at all.

## 8. What the compiler actually said

The build config landed and `createMinecraftArtifacts` succeeded — 26.1.2.95 downloads, decompiles
and patches cleanly. `compileJava` then produced **169 errors across 20 files**. Every replacement
below was read out of `minecraft-patched-26.1.2.95-sources.jar` and the NeoForge 26.1.2.95 sources,
not inferred from the primers.

| file | errors |
|---|---|
| `client/PortableBeaconScreen` | 44 |
| `gametest/BPGameTests` | 24 |
| `datagen/BPAdvancementProvider` | 16 |
| `datagen/BPItemModelProvider` | 13 |
| `datagen/ComponentShapedRecipe` | 9 |
| `compat/jei/PortableBeaconsJeiPlugin` | 8 |
| `item/PortableBeaconItem` | 8 |
| `datagen/BPDataGen` | 8 |
| `registry/BPLookups` | 7 |
| `client/BPClientEvents` | 7 |
| the other 10 | 1–6 each |

**`core/` came through almost untouched** — 3 errors in `BPRegistryKeys`, all of them the same
rename, and nothing at all in `PackState`, the codecs or the arithmetic. That was the bet the layer
was written to win, and it won.

### The mechanical majority

131 of the 169 are `cannot find symbol`, and most are one rename cascading:

| 1.21.1 | 26.1 |
|---|---|
| `net.minecraft.resources.ResourceLocation` | **`net.minecraft.resources.Identifier`** — same factory methods (`fromNamespaceAndPath`, `parse`) |
| `ResourceKey#location()` | `ResourceKey#identifier()` |
| `HolderLookup.Provider#registryOrThrow(key)` | `#lookupOrThrow(key)` |
| `ItemStack#getDescriptionId()` | `#getHoverName()`, which already returns a `Component` |
| `Player#displayClientMessage(Component, boolean)` | gone; `sendSystemMessage(Component)` for chat, and the action bar goes through `ClientboundSetActionBarTextPacket` |
| `mouseClicked(double, double, int)` | `mouseClicked(MouseButtonEvent, boolean doubleClick)` |
| `keyPressed(int, int, int)` | `keyPressed(KeyEvent)` |
| `imageWidth` / `imageHeight` | now final — set through the constructor |

Input became event objects rather than loose primitives. Incidentally the new `mouseClicked` carries
its own `doubleClick` flag, so whatever the screen does to detect double clicks can be deleted
rather than migrated.

### The one real decision: `IItemHandler`

NeoForge replaced the item capability with a resource-based one. `Capabilities.ItemHandler.ITEM`
is now `Capabilities.Item.ITEM`, typed `ResourceHandler<ItemResource>` instead of `IItemHandler`.

`IItemHandler` still exists — but marked `@Deprecated(since = "1.21.9", forRemoval = true)`, with an
explicit migration bridge, `IItemHandler.of(ResourceHandler<ItemResource>)`.

So there are two ways through, and they differ in more than effort:

- **Bridge.** Wrap at the capability boundary, leave the rest of the item code alone. Cheap now.
  Deprecated *for removal*, so it is a debt with a due date somewhere in 26.2 or 26.3 — the exact
  versions this plan already commits to porting to. It also carries a real constraint: the adapter's
  javadoc warns that `insertItem` / `extractItem` open new root transactions and cannot be called
  from inside a transactional context, and fuel is consumed every tick.
- **Migrate.** Rewrite the item handling against `ResourceHandler<ItemResource>`. More work now,
  and it is the shape the API is actually going to keep.

Given that the plan is three ports rather than one, paying this once looks right — but it is a
genuine fork and not mine to take silently.

`ComponentItemHandler`, `SlotItemHandler` and `ItemContainerContents` all still exist, so the way the
beacon stores its contents does not have to change; this is about the capability boundary only.

### The parts that are rewrites, not renames

- **The screen**, as predicted — 44 errors, and the count understates it. Renames aside, 26.1's
  extract-then-render split is not something the compiler can point at.
- **Datagen**, more than expected. `net.minecraft.advancements.critereon` no longer exists,
  `net.neoforged.neoforge.client.model.generators` is gone with the old item model system,
  `RecipeProvider` now has an abstract no-arg `buildRecipes()`, and recipes want `Recipe.CommonInfo`.
  All four providers plus `BPDataGen` are affected.
- **Gametests**, 24 errors, the area §4 flagged as unknown. Now measured, still unexamined.

## 9. What actually happened

The port is done: 169 errors to zero, 33 unit tests, 8 gametests. Kept as written above rather than
tidied, because the estimate is only useful next time if its mistakes are still visible.

### Where the estimate was wrong

**The screen was not a rewrite.** §4 called it "the whole cost of this port" and said to assume it
was rewritten, because 26.1 "split drawing into two phases". The split is real, but
`GuiGraphicsExtractor` kept the same verbs — `fill`, `blit`, text — so all 97 draw calls migrated by
renaming. The drawer animation, which §4 named as "exactly the shape the new model removes", needed
no change at all: extraction runs every frame, so reading a clock and interpolating a width works
as it always did. I read more into one line of a primer summary than the line said.

**Datagen was worse.** §4 listed two providers; all four broke, plus the entry point, plus a trap
the analysis never suspected — see below.

**Gametests were a rearchitecture, not a migration.** §4 marked them "unknown", which was honest,
and unknown turned out to mean the `@GameTest` annotation no longer exists.

### What the analysis could not have predicted

Three of the hardest bugs were invisible to a compiler and to every test:

- **The two datagen runs deleted each other's output.** The generator purges anything under its
  output folder that the providers it just ran did not write. Pointed at one folder, whichever run
  went second wiped the other half — the mod had models and no recipes, then recipes and no models.
  Separate output folders, separate caches.
- **Every label on the screen was invisible.** Text colours are strict ARGB now, and the two text
  constants had no alpha channel — `0x404040` is alpha 0. The accent colours all carried `0xFF`
  already, so the tab glyphs and buttons looked right and hid how wide the problem was.
- **Vanilla unpacked the beacon's slots into its tooltip**, unprompted, because
  `ItemContainerContents` is a tooltip provider now.

None of these fail a build. All three needed the game running and someone looking at it.

### What held

`core/` came through with three errors, all one rename. The two files that differ from the 1.21.1
branch differ by six lines and two — the same arithmetic, character for character, across ten
primers. That is the whole argument for the layer, and it is the one prediction in this document
that was not just right but underclaimed.

## 10. Branches

One branch per game version, since each one keeps getting files rather than being replaced.

| branch | what it is |
|---|---|
| `main` | documentation and planning, and 1.21.1 until the port lands. |
| `1.21.1` | maintenance. Where a hotfix for players already on 1.0.0 goes, so it has somewhere to live once `main` moves on. |
| `26.1` | the port. Branches from `main`. |
| `26.2` | branches from `26.1` once it builds and runs — not before, or it is just an empty copy that drifts. |

`26.3` the same way, when there is a 26.3.

## 11. 26.1 → 26.2: what it cost

An afternoon, against the ten primers of 26.1. Twenty-nine compile errors, none in `core/`, none in
the screen beyond one line:

- **ModDevGradle 2.0.144 could not recompile 26.2** - it failed inside Minecraft's own decompiled
  sources, before any of ours. 2.0.148 does.
- **Advancement classes split into `predicates` and `triggers`** packages; `Criterion` moved with them.
- **The current screen moved from `Minecraft` to `Gui`** (`mc.gui.screen()`, `mc.gui.setScreen`),
  and the HUD became `Hud`, which is where `getMobEffectSprite` now lives.
- **Tag providers take `ResourceKey`s**, not items: `add(holder.getKey())`.
- **Entity types live in `EntityTypes`.**
- Recipes are written without a default `category`; the files change, the game does not.

All nineteen gametests passed on the first run that compiled.
