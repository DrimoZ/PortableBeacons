# Changelog

All notable changes to this project are documented here, newest first.
Versions follow `{mod version}+{minecraft version}`.

## Unreleased

### Added
- **Recharging at a real beacon.** Standing in a lit beacon's range tops up every portable beacon you
  carry, switched on or not - a base with a beacon is now a charging station.
  `beacon_recharge_per_second` sets the rate; 0 turns it off.
- **Forge energy.** Any energy mod's charger fills a beacon through the energy capability, at
  `energy_per_fuel_unit` FE per fuel unit (40 by default); 0 turns it off. Whole units only, and a
  beacon never gives energy back.
- **Effect pools.** An effect can declare `"pools": [...]`, and a tier's `effect_pool` can list
  `"#pool"` beside effect ids. The four standard tiers now list `"#standard"`, so a datapack effect
  joins all four from its own file instead of being added to every tier.
- **Glyphs for datapack augments.** An augment can name its icon's glyph with `"glyph"`: any of the
  shipped augments' glyphs, or one of six generic shapes - star, bolt, heart, gem, shield, leaf.
  Before, an augment from a datapack could only ever show a bare casing.
- **Higher ceilings for datapacks.** Effects can reach level X (`max_amplifier` up to 9, was 3), a
  tier can unlock up to 8 effect slots (was 5) and up to 8 augment slots (was 4). The effect table
  scrolls past five rows and the augment tab grows a second row only when a tier needs it, so the
  shipped beacons look exactly as before. Augments keep three tiers: their casing shows the tier as
  pips, and five do not read at 16 pixels.
- **Two targeted augment operations.** `add_effect_amplifier` raises one effect's level ceiling and
  `mul_effect_cost` multiplies one effect's cost, each naming its effect with `"effect"` - so a
  datapack can build a specialised augment (a sprinter's, a miner's) instead of only all-round ones.
  A targeted operation without its effect is refused when the file is read.

### Changed
- **A new screen, on FactoryIO's layout.** The fuel gauge and fuel slot sit in the window's first
  column instead of a drawer; a status light in the title band says whether the beacon is running,
  idle, low on fuel, out of fuel or switched off; the beacon's figures are a yellow tab on the left
  and the augments a blue tab on the right. Each effect is one row with everything set in place:
  click the icon to change the effect (right-click to remove it), click the level to raise it
  (right-click to lower it), click the figure to choose who it reaches, flip the switch to turn it
  off. No more selecting a socket first and finding its controls elsewhere; what an effect drains
  and how far it reaches are in the row's tooltip. Empty slots show what goes in them.
- **The effect picker is a grid**, covering the whole window, with a search field and arrow-key
  navigation, and a "remove" cell first when the row already holds an effect.
- **The master switch is a slide switch**, beside a status light, instead of a power glyph.
- **A beacon that runs dry waits instead of switching off.** It warns a minute before, stays on
  while dry - the status light turns red - and resumes by itself as soon as there is fuel in its
  slot. It used to switch itself off, so refuelling did nothing until the screen was reopened.
- **Only one beacon runs at a time, and the screen says so.** Switching one on switches your others
  off; before, a second beacon stayed "on", glinting, and did nothing.
- **An augment can replace one of its own type.** Dropping Range III on Range II swaps them; it used
  to be refused as a duplicate.
- **The beacon's tooltip counts the fuel slot** in its runtime, as the screen does, and the info tab
  shows moving and standing runtimes separately when Wayfarer or Sentinel makes them differ.
- **JEI no longer covers the side tabs.**
- **New item art.** Augments are drawn as fitted modules - a casing, a screen in the augment's
  colour, its glyph - and show their tier as one to three lit pips, so Range I and Range III are no
  longer the same icon. Beacons show their tier by the band's material, iron to netherite, and by pips.

### Fixed
- **Upgrading a beacon no longer destroys what it carried.** The tier and themed recipes built their
  result from nothing, so crafting a Beacon II into a III lost its installed augments, its fuel and
  its configured effects. They now carry over; whatever the new beacon cannot use is dropped from
  its configuration exactly as when an augment is removed.
- **Removing a Capacity augment no longer destroys fuel.** The buffer was cut to the new capacity on
  the spot. The surplus now stays and burns down, and no fuel is taken in until it has.
- **Wayfarer and Sentinel now see you move.** The server read movement in a way its own tick order
  always reset to zero, so every carrier counted as standing still: Wayfarer only ever charged its
  surcharge, and Sentinel always gave its discount.
- **Effect icons no longer blink, and Night Vision no longer strobes.** Effects were refreshed with
  too little time left on them, and spent half of every refresh inside vanilla's "about to expire"
  window.
- **A beacon no longer runs dry with fuel in the slot.** It burned one item per refresh at most, so a
  build costing more than one item per refresh switched off with a full stack waiting.
- **A refused write to an augment or fuel slot no longer empties it.**
- **Items put into a beacon's slots after the beacon was moved away are handed back** when the screen
  closes. They went into one store shared by every open screen on the server, and could be lost or
  turn up in another player's.

## 1.1.0

### Added
- **Seven augments, and four new levers to build them from.**

  The mod had seven operations and seven augments — one each — so the vocabulary was saturated and
  anything new was a recombination. These four operations are the first additions to it:

  | | |
  |---|---|
  | `mul_aura_cost` | scales the sharing surcharge **alone** |
  | `free_effect_slot` | runs the dearest N effects for nothing |
  | `mul_cost_moving` | while the carrier is travelling |
  | `mul_cost_still` | while holding a position |

  Four augments spend them, and three combine several gains across different axes for one cost — so
  what they buy back is a **slot**, which is the scarcest thing a beacon has:

  | | Gains | Pays |
  |---|---|---|
  | **Communion** | sharing surcharge ×0.5 / ×0.3 | all fuel ×1.35 / ×1.5 |
  | **Wellspring** | one effect free | everything else ×1.6 |
  | **Wayfarer** | ×0.5 / ×0.35 while moving | ×1.6 / ×1.9 standing still |
  | **Sentinel** | ×0.5 / ×0.35 standing still | ×1.6 / ×1.9 while moving |
  | **Vanguard** | +12 range, +1 aura rank | fuel ×1.9 |
  | **Prism** | +1 effect slot, +1 level ceiling | fuel ×2.2 |
  | **Recluse** | fuel ×0.35, buffer ×3, no particles | −2 aura ranks |

  `mul_aura_cost` is the one that opens the most ground: sharing became the central mechanic in this
  release and nothing modulated its price.

- **Beacon IV: three effect slots and four augment slots.**

### Changed
- **Fuel moved to container slot 0**, with augments after it. It used to sit *after* the augments, so
  its index was derived from the augment count — meaning any future slot added by the mod would shift
  it and turn the fuel in every saved beacon into an augment. Doing this now costs nothing, since the
  id rename already breaks saves, and it means the layout is stable under the change this mod is
  most likely to make again.

  To be clear, since it is easy to confuse the two: a **datapack** moving `augment_slots` was never
  dangerous and still is not. That field says how many slots are *usable*; the container's size is
  fixed by the mod. Lowering it locks augments in place rather than losing them.
- The sharing surcharge is computed separately from an effect's own cost, so an augment can discount
  one without the other. Every existing price is unchanged, and a test pins them there — a refactor
  of that shape moves the whole balance without failing anything.

### Fixed
- `effect_slots` accepted up to 9 while the screen laid out 5, and nothing connected them. A sixth
  effect would have been resolved, charged and kept — paid for and invisible. One ceiling now, in
  the place the number is decided.
- The effect cases were drawn from the background texture, which held exactly three, so a beacon
  with more slots showed its fourth and fifth floating with no frame.
- An effect covered by Wellspring reported "Drain 375%": its share compared a gross cost against a
  total it had been excluded from. It says *free* now.
- The advancement tab drew as missing texture. `ClientAsset.ResourceTexture` builds its own path, so
  passing a full one asked for `textures/textures/….png.png`.

## 1.1.0+26.1.2 — the port

Everything in 1.1.0 above, built for **Minecraft 26.1.2** on NeoForge 26.1.2.95 and Java 25.

Ten primers separate 1.21.1 from 26.1 — 1.21.1 was the modding anchor for a year, so nothing moved
and then everything did at once. `core/`, the layer with no dependency on components, packets or
rendering, came through the whole port with **three errors, all one rename**; the two files that
differ from the 1.21.1 branch differ by six lines and two.

### Changed
- Item handling moved to NeoForge's resource transfer API. `IItemHandler` is deprecated for removal,
  and a bridge existed, but this plan already commits to 26.2 and 26.3 — the wrapper would have come
  due inside work already scheduled. Fuel is now drawn inside a transaction, so a slot that refuses
  to give up its item cannot credit fuel that was never burned.
- The augment glyph is chosen by the model, keyed on the augment's **registry key**. The old
  `model_data` integer had to match an array's order in the model generator, and when the two
  drifted an augment simply rendered as the wrong glyph with nothing reporting it. The field is gone
  from `AugmentDef`; existing datapacks still load, because a record codec ignores what it does not
  know.
- Item colours are tint sources declared by the model rather than handlers registered in code, so
  the augment tint travels with the model it tints.
- Tooltips are assembled from registered appenders instead of an override on the item.
- Gametests are registered functions plus `test_instance` data files; the `@GameTest` annotation is
  gone.

### Fixed
- Vanilla unpacked the beacon's own slots into its tooltip, on every hover, shift or not:
  `ItemContainerContents` is a tooltip provider now and the beacon stores its augments and fuel in
  `minecraft:container`. Hidden through `TOOLTIP_DISPLAY` — the beacon already says what it holds,
  in its own words and only when asked.
- Every label on the beacon screen was invisible. Text colours are strict ARGB now, and a bare
  `0xRRGGBB` is alpha 0 — it draws nothing rather than defaulting to opaque. The stats drawer read
  as an empty panel because it is nothing but text.

### Notes
- **EMI is not included.** No 26.x NeoForge build exists to compile against, on either platform. The
  plugin stays in the source tree and returns in one commit when one appears. JEI covers the same
  ground meanwhile.
- Curios 15.0.0+26.1.2 and JEI 29.29.0.76 are the versions this was built against.

## 1.0.1

Finishes the rename. 1.0.0 changed the mod id and left the item ids alone, so the beacons were still
`portablebeacons:beacon_pack_i` and the themed ones were still named after dimensions rather than
after themselves.

| was | is |
|---|---|
| `beacon_pack_i` … `beacon_pack_iv` | `beacon_i` … `beacon_iv` |
| `nether_pack` | `cinder_beacon` |
| `end_pack` | `void_beacon` |
| `tidal_pack` | `tidal_beacon` |

The themed three were the worst of it: their ids said Nether, End and Tidal while the game showed
Cinder Beacon, Void Beacon and Tidal Beacon. Anyone reading a crash report, writing a datapack or
typing `/give` met a name the mod never uses out loud.

**This breaks beacons saved with 1.0.0 or the 0.9.0 beta** — they become unknown items, and a
datapack written against the old ids needs updating. Same reasoning as the rename itself: the ids
are wrong now and will only get more expensive to fix, so it happens at 1.0.1 or never.

The data component and the item tag moved too — `portablebeacons:pack` is now
`portablebeacons:beacon`, and the tag `packs` is now `beacons`. A datapack naming either needs
updating.

### Fixed
- **Attunement did nothing, on any beacon.** It raised the beacon's effective tier for the sharing
  check, and every tier already cleared the threshold it raised — so there was never a mode left for
  it to unlock. Sharing is now gated by an `aura_rank` on each tier, which Attunement adds to.

  | Beacon | Shares unaided | + Attunement I | + Attunement II |
  |---|---|---|---|
  | I, II | self only | Team | Allies |
  | III, IV, themed | Team | Allies | Allies and Pets |

  This is a real balance change: sharing with every nearby player now costs an augment slot on every
  beacon. It also makes Team meaningful, since it now sits *below* Allies on the ladder — narrower
  reach, lower cost, the cheaper first step out of keeping everything to yourself.

- **Amplification did nothing on the beacons that could hold it.** It lifts the beacon's level
  ceiling, but each effect carries a second ceiling and the lower one wins — and every effect stopped
  at level II while Beacon IV and Cinder already granted level II unaided. Speed, Haste, Jump Boost,
  Resistance, Strength and Regeneration now allow level III, so the augment has somewhere to go. The
  utility effects stay at level I, where a second level means nothing in vanilla.

### Changed
- The GUI texture and the item textures are named after their items again.
- Datagen runs in its own game folder, so a mod left in `run/mods` for a different Minecraft version
  can no longer stop the generator before it writes anything.

### Notes
- `aura_rank` is a new tier field, `0`–`3`, defaulting to `0`. Sharing was the one mechanic in the
  mod that a datapack could not touch; it now is one. See the
  [wiki](https://github.com/DrimoZ/PortableBeacons/wiki/Augments#attunement).

## 1.0.0

Renamed from **BeaconPack** to **Portable Beacons**, and renamed all the way down: the mod id, the
item, recipe and advancement ids, the datapack paths, the item tag and the Java package are all
`portablebeacons` now. The old name read as a modpack, which is the one thing this is not.

**This breaks worlds saved with the 0.9.0 beta.** A beacon stored under the old id becomes an
unknown item, and any datapack written against `beaconpack:` needs its ids updated. The beta was up
for a day, so renaming now costs a handful of saves instead of every save the mod will ever touch —
which is exactly why it had to happen before 1.0 and cannot happen after.

The aura is now verified on a dedicated server with two players: an effect set to *Allies* reaches
the second player, and *Team* excludes one who is not on the team. That exclusion cannot be observed
in single player — there the carrier is trivially allied with everyone who exists — and it was the
last thing standing between the beta and a release.

### Added
- EMI plugin, matching the JEI one: the fifteen augments are listed separately instead of
  collapsing into a single entry, and a fuel category shows what each item is worth as runtime.
- Fuel entries can be priced by an item **tag** as well as by a single item, so one line covers
  every modded metal that follows the convention. Copper is now fuel, at 150 units.
- A chime when an augment is fitted or pulled. Every button in the screen made a sound; the slot
  that changes the most did not.
- Curios, JEI and EMI are declared as optional dependencies, so the loader and the store pages say
  so rather than leaving players to guess.

### Fixed
- The rename left every id in the Java sources pointing at the old namespace, so 44 translation
  keys and the GUI background resolved to nothing: the screen, every tooltip, the creative tab, the
  keybind category and the out-of-fuel message all rendered as raw ids over a missing texture.
  Caught before the 1.0.0 file went up, by a new test that checks every key named in the code is one
  the language files actually define — the existing test only compared the locales to each other,
  and all fifteen were consistently wrong together.
- EMI showed an on-screen error for every fuel row: rows built from a registry rather than from a
  JSON recipe need EMI's synthetic-id form.

## 0.9.0 — first public build

Released as a **beta**: everything worked and was tested, but nobody outside the author had played
it yet. Report anything odd on the [issue tracker](https://github.com/DrimoZ/PortableBeacons/issues).

### Added
- Four beacon tiers projecting beacon effects from the inventory, configured through their own
  screen.
- Seven augments: Range, Focus, Amplification, Efficiency, Capacity, Attunement and Discretion,
  one of each type per beacon.
- Three themed beacons — Cinder, Void and Tidal — with effect pools drawn from effects the vanilla
  beacon never offered.
- Fuel: items are consumed per second of projection, priced by a datapack registry and reported as
  remaining runtime rather than as points.
- Four datapack registries (`effect`, `augment`, `tier`, `fuel`), so effects, augments, tiers and
  fuel values can all be retuned or extended without code.
- Server config for fuel, aura reach, free coverage near a real beacon, and an optional requirement
  to stand near a lit beacon to reconfigure.
- Optional Curios support: one worn in the `charm` slot works like one carried in the inventory.
- Advancements: get one, fit an augment, then specialise or finish the ladder.
- Fifteen languages: English, French, Spanish, German, Portuguese (BR), Russian, Simplified and
  Traditional Chinese, Japanese, Korean, Polish, Italian, Dutch, Turkish, Ukrainian.
- JEI plugin, and a key bind (**B** by default) that opens the one you are carrying or wearing.

### Changed
- The screen's side tabs are split across both edges — power and stats on the left, augments and
  fuel on the right — and open in place with a short animation instead of appearing as detached
  panels. One drawer per side can be open at a time.
- One worn in a Curios slot takes priority over one loose in the inventory.
- Built against NeoForge 21.1.248.

### Fixed
- One worn as a Curio could not be opened at all: the key bind's request was discarded before it
  reached the code that opens the menu.
- A modified client could send a negative slot or value in a configuration action. Every existing
  check was an upper bound, so it reached `List.set` / `List.remove` and threw on the server thread.
- Two labels in the effect panel could run past the frame, and one was truncated to nonsense.

### Notes
- Documentation lives in the [wiki](https://github.com/DrimoZ/PortableBeacons/wiki).
- Code is MIT; the artwork is reserved, with redistribution as part of the mod — modpacks included —
  granted explicitly. See the README's Permissions section.
