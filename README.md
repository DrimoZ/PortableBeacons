# Portable Beacons

Beacon effects in an inventory item. Four tiers, augments you slot in, and a fuel cost — all
defined in datapacks rather than in code.

**NeoForge 26.1.2** · Java 25 · MIT

**[Download on CurseForge](https://www.curseforge.com/minecraft/mc-mods/portables-beacons)**

📖 **[The wiki](https://github.com/DrimoZ/PortableBeacons/wiki) is the reference** — player guide,
config, datapack docs and FAQ. This README is the short version.

---

## What it does

A Portable Beacon sitting in your inventory projects beacon effects. Which effects, how far, how
strong, and what it costs are all configured through its own screen.

| Tier | Effects | Level | Shares with | Aura range | Augment slots |
|---|---|---|---|---|---|
| I | 1 | I | you | — | 0 |
| II | 1 | I | you | 8 blocks | 1 |
| III | 2 | I | your team | 12 blocks | 2 |
| IV | 3 | II | your team | 16 blocks | 4 |

Ranges are far below the vanilla beacon's 20–50 blocks on purpose: a beacon that follows you is
worth much more than a fixed one at equal reach.

### Augments

One augment of each type per beacon, each with its own tiers. They are a single item whose identity
comes from a datapack registry entry, so a datapack can add new ones without any code.

| Augment | Effect |
|---|---|
| Range | +4 / +8 / +12 blocks |
| Focus | +1 effect slot |
| Amplification | +1 to the effect level you may *reach* — you still choose it, and still pay for it |
| Efficiency | −25 / −40 / −55 % fuel |
| Capacity | fuel buffer ×2 / ×3 / ×4 |
| Attunement | +1 / +2 sharing ranks, which is how most beacons share at all |
| Discretion | hides effect particles, and the status icon at tier II |

Those seven are pure gains — the only cost is the slot. Seven more give something up, and those are
where the decisions are:

| Augment | Gains | Pays |
|---|---|---|
| Communion | sharing surcharge ×0.5 / ×0.3 | all fuel ×1.35 / ×1.5 |
| Wellspring | your dearest effect runs free | everything else ×1.6 |
| Wayfarer | ×0.5 / ×0.35 while moving | ×1.6 / ×1.9 standing still |
| Sentinel | ×0.5 / ×0.35 standing still | ×1.6 / ×1.9 while moving |
| Vanguard | +12 blocks, +1 sharing rank | fuel ×1.9 |
| Prism | +1 effect slot, +1 level ceiling | fuel ×2.2 |
| Recluse | fuel ×0.35, buffer ×3, no particles | −2 sharing ranks |

Fourteen augments, at most four slots. The question is never which you want but which four.

Sharing is earned rather than given: Beacons I and II keep everything to the carrier, III and IV
reach your team, and anything wider needs Attunement or Vanguard. Each tier's starting point is a
datapack field, so a pack can hand it all out from the start or lock it all behind an augment.

Crafting a beacon into the next tier, or into a themed beacon, keeps what it carried: installed
augments, stored fuel and configured effects all move to the new one.

### Themed beacons

Cinder, Void and Tidal beacons sit alongside tier III with narrower pools drawn from effects the
beacon never offered. Carrying one instead of a tier IV is a trade, not a downgrade.

| Beacon | Pool | Edge |
|---|---|---|
| Cinder | Fire Resistance, Strength, Haste, Resistance | level II allowed |
| Void | Slow Falling, Speed, Jump Boost, Night Vision | longest reach, 14 blocks |
| Tidal | Water Breathing, Conduit Power, Dolphin's Grace, Night Vision | the only aquatic pool |

They needed no new mechanics: a tier entry declares which effects it accepts, so a themed beacon is
a data file plus an item — and a datapack can add more the same way.

### Fuel

Each effect costs fuel per second, scaled by its level and by how widely it is shared. Copper,
iron, gold, emerald, diamond and netherite are worth increasing amounts; the beacon draws from its own
fuel slot. Sharing an effect with allies costs more than keeping it to yourself, which is the
main decision the mod asks you to make.

A master switch stops all consumption instantly, and each effect can be turned off individually
without losing its settings. An effect a real beacon is already providing is free, and the beacon
refuses fuel its buffer cannot hold whole rather than burning most of a netherite ingot for
nothing — which is what gives Capacity and the higher tiers a purpose. Pulling a Capacity augment
out of a full beacon loses nothing either: the surplus stays and burns down, and the beacon takes no
new fuel until it has.

Fuel is not the only way in. Standing in a lit beacon's range recharges every beacon you carry, on
or off (`beacon_recharge_per_second`, 20 by default), so a base with a beacon doubles as a charging
station. And any energy mod's charger fills a beacon through the forge energy capability, at
`energy_per_fuel_unit` FE per unit (40 by default: an iron ingot's worth is 12,000 FE).

A minute before it runs dry the beacon says so. When it does run dry it stays switched on and
waits, and resumes by itself the moment there is fuel in its slot. Only one beacon runs at a time:
switching one on switches your others off.

Turning `require_fuel` off removes fuel from the game rather than leaving it inert: no fuel slot,
no gauge, no runtime figures.

### The screen

One row per effect, with everything set in place: click the icon to change the effect, the level to
raise it, the figure to choose who it reaches, the switch to turn it off - right-click steps back.
A bar under each name shows its share of the drain. The fuel gauge and slot sit beside the list,
the master switch and a status light in the title band; the beacon's figures and its augments are
side tabs. Effects are picked from a searchable grid filtered to what the beacon accepts.

---

## Configuration

Four server-side options: whether fuel exists, whether the aura reaches players off your team,
whether a real beacon makes an effect free, and whether reconfiguring needs a beacon nearby.

**[Config reference →](https://github.com/DrimoZ/PortableBeacons/wiki/Configuration)**

---

## Keeping the vanilla beacon relevant

The obvious failure mode for a mod like this is making the beacon block pointless. Three
safeguards:

1. Every tier consumes a Beacon block in its recipe.
2. Aura ranges stay well below the block's, so a placed beacon is still better for a base.
3. `require_beacon_to_configure` (off by default) restricts changing effects to within 16 blocks
   of a lit beacon.

---

## Data-driven

Four datapack registries under `data/<namespace>/portablebeacons/`:

| Registry | Controls |
|---|---|
| `effect` | which effects a beacon may project, their cost, level cap and minimum tier |
| `augment` | augment types, their per-tier operations, colour and icon |
| `tier` | base stats of each tier |
| `fuel` | what an item is worth in fuel units |

```json
// data/mypack/portablebeacons/effect/fire_resistance.json
{
  "effect": "minecraft:fire_resistance",
  "cost": 2.0,
  "max_amplifier": 0,
  "min_tier": 2,
  "pools": [ "standard" ]
}
```

`"pools"` is what makes that one file: the four standard tiers list `"#standard"` in their
`effect_pool`, so any effect in the `standard` pool is offered by all four with no tier file
touched. A pack can invent its own pools the same way. An augment added by a datapack can borrow
any shipped glyph for its icon with `"glyph"` - one of the augments' own (`"range"`, `"focus"`,
…) or a generic shape: `star`, `bolt`, `heart`, `gem`, `shield`, `leaf`.

Ceilings a datapack can reach: effect levels up to X, 8 effect slots and 8 augment slots per tier,
three tiers per augment.

Two operations act on a single effect, named by `"effect"`, which is how a pack builds a
specialised augment rather than another all-round one:

```json
// data/mypack/portablebeacons/augment/sprinter.json - Speed one level higher, and cheaper
{
  "max_tier": 1, "color": 5636095, "glyph": "bolt",
  "operations": [
    { "type": "add_effect_amplifier", "effect": "portablebeacons:speed", "values": [1] },
    { "type": "mul_effect_cost", "effect": "portablebeacons:speed", "values": [0.6] }
  ]
}
```

**[Full datapack guide →](https://github.com/DrimoZ/PortableBeacons/wiki/Datapack-Guide)** — every field
of all four registries, with worked examples for adding an effect, an augment and a themed tier.

The screen adapts on its own: effects live in a scrolling, searchable picker rather than on the
main panel, so declaring forty of them changes nothing about the layout. Effect icons come from
the vanilla effect atlas, so anything registered — vanilla, another mod's, or datapack-added —
displays correctly with no texture needed.

The default pool is deliberately limited to the vanilla beacon's effects. Widening it is a
datapack away, but it is not the default: an unrestricted pool turns a utility mod into a cheat
item.

---

## Compatibility

**Curios** is optional. With it installed, a beacon worn in the `charm` slot works exactly like one
carried in the inventory — the binding ships with the mod, so nothing needs configuring. A worn
beacon wins if you somehow carry two: one you deliberately equipped should beat one that merely
happens to be in your bag. Without Curios, none of that code is ever touched.

---

## Permissions

**Modpacks: yes.** No permission needed, no message required, public or private,
monetised or not, on any platform or launcher. If you are reading this to find out whether you may
include Portable Beacons, the answer is yes and you can stop reading.

**Credit** is appreciated and never required.

**Forks and addons: yes**, under the MIT terms. Please do not publish a fork under the name
*Portable Beacons* — the name is not covered by the licence, and two mods sharing one name only confuses
players trying to work out which one broke their world.

**Assets** — icons, GUI artwork, logo — are the one exception: redistribute them with the mod
freely, but do not lift them into another project. See [LICENSE-ASSETS](LICENSE-ASSETS).

**Contributions** are accepted under the same terms as the rest of the repository.

---

## Building

Requires JDK 25.

```bash
./gradlew build
```

```bash
./gradlew runClient
```

Textures are generated rather than hand-drawn, so the GUI background stays in sync with the slot
coordinates in `PortableBeaconMenu`:

```bash
java tools/GenerateTextures.java
```

### Layout

- `core/` — the data model and all of the arithmetic, with no dependency on data components,
  packets or rendering. Unit-testable without launching Minecraft, and the only layer a port to
  another Minecraft version leaves largely untouched.
- `registry/`, `item/`, `menu/`, `net/`, `client/` — the platform layer.

`PackState` and its codec are the single place serialization lives, which is what keeps a
backport to an older version down to rewriting one file.
