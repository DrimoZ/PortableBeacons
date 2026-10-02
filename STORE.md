# Store listing copy

Live at <https://www.curseforge.com/minecraft/mc-mods/portables-beacons>.

Paste-ready text for the CurseForge project page. Not documentation — the
[wiki](https://github.com/DrimoZ/PortableBeacons/wiki) is that.

---

## Short description (one line)

> Beacon effects in an inventory item. Four tiers, augments you slot in, and a fuel cost — all
> defined in datapacks.

## Categories

*Adventure and RPG*, *Equipment*, *Utility & QoL*

## Licence field

Custom. Point it at `LICENSE` in the repository: MIT for the code, all rights reserved for the
artwork with redistribution granted. Paste the Permissions block below into the description too —
people read the page, not the repo.

---

## Description

### Portable Beacons

A Portable Beacon in your inventory projects beacon effects on you, and — once it can share — on the
people around you. Which effects, how far, how strong, how widely and what it costs are all
configured in the beacon's own screen.

It is deliberately **not** a beacon replacement. Every tier consumes a Beacon block in its recipe,
the aura tops out at 16 blocks against the block's 20–50, and an effect your own beacon already
provides costs nothing. The block stays the better choice for a base; this is what you take with you.

### Four tiers, and three that specialise

| Beacon | Effects | Level | Shares with | Aura | Augments |
|---|---|---|---|---|---|
| Portable Beacon I | 1 | I | you | — | — |
| Portable Beacon II | 1 | I | you | 8 blocks | 1 |
| Portable Beacon III | 2 | I | your team | 12 blocks | 2 |
| Portable Beacon IV | 3 | II | your team | 16 blocks | 4 |

Upgrading a beacon keeps its augments, its fuel and its effects — nothing you fitted is lost.

**Cinder**, **Void** and **Tidal** beacons sit alongside tier III with narrow pools drawn from effects
the vanilla beacon never offered — Fire Resistance, Slow Falling, Water Breathing, Conduit Power,
Dolphin's Grace. Carrying one instead of a Beacon IV is a trade, not a downgrade.

A **Creative Beacon**, in the creative tab only, has every slot and level at its maximum and needs no fuel.

Both ceilings above can be raised by augments, and both are datapack fields.

### Fourteen augments, four slots

Seven are pure gains: more reach, more effects at once, higher levels, cheaper fuel, a bigger
buffer, wider sharing, or no particles at all. One of each type per beacon, most with three tiers.

The other seven give something up, and those are the interesting ones. **Wellspring** runs your most
expensive effect for nothing and charges more for the rest. **Communion** makes sharing cheap and
everything else dearer. **Wayfarer** and **Sentinel** are opposites — one is cheap while you travel,
the other while you hold a position. **Recluse** cuts your fuel to roughly a
third, triples your buffer and hides your particles, in exchange for sharing with nobody at all.

With at most four slots, the question is never which augment you want. It is which four.

**Sharing is earned, not given.** Reaching beyond your own team costs an Attunement augment, and
therefore an augment slot — which is the decision, since the slot could have held more range or
cheaper fuel instead.

### Fuel you can read

Copper through netherite, priced by a datapack — per item, or by a convention tag, so a modded
metal is accepted without a file of its own. The screen shows **remaining runtime**, not a unit
count: "4 h" answers the question you actually have. Each effect chooses its own level and its own
audience, and both cost more, so a beacon is a set of small trades rather than one switch.

Or skip the ingots: a lit beacon recharges every portable one you carry while you stand in its
range, and any energy mod's charger fills one with FE.

You get a minute's warning before it runs dry, and a dry beacon waits instead of switching off: put
fuel in and it picks up where it left off.

Fuel can be switched off entirely in the config, and when it is, every trace of it disappears from
the screen rather than sitting there inert.

### Data-driven, properly

Four datapack registries: which effects a beacon may project and what they cost, what augments do,
what each tier is worth, and what burns as fuel. The mod's own content ships through exactly that
mechanism — there is no private path. Adding another mod's effect, or a whole themed beacon, is a
JSON file. See the [datapack guide](https://github.com/DrimoZ/PortableBeacons/wiki/Datapack-Guide).

### Compatibility

- **Curios** (optional): wear a beacon in the charm slot, and it works exactly like a carried one.
  Ships configured; nothing to set up.
- **JEI** (optional): all fifteen augments listed separately instead of collapsing into one, plus a
  fuel category showing what each item is worth as runtime. **EMI** the same, on 1.21.1.
- Server-side config, synced to clients. Required on both sides.

---

## Permissions

**Modpacks: yes.** No permission needed, no message required, public or private, monetised or not,
on any platform or launcher. If you are reading this to find out whether you may include
Portable Beacons, the answer is yes and you can stop reading.

**Credit** is appreciated and never required.

**Forks and addons: yes**, under the MIT terms. Please do not publish a fork under the name
*Portable Beacons* — the name is not covered by the licence, and two mods sharing one name only confuses
players trying to work out which one broke their world.

**Assets** — icons, GUI artwork, logo — are the one exception: redistribute them with the mod
freely, but do not lift them into another project.

---

## Upload checklist

- [x] CurseForge project created and renamed to Portable Beacons.
- [ ] Release type: **Release**. The aura is confirmed on a dedicated server with two players.
- [ ] Game version: **1.21.1** for the 1.0.1 file; **26.1.2** for the 26.1 build when it ships.
      Loader NeoForge.
- [ ] Mark Curios and JEI as **optional** dependencies, not required. EMI too on the 1.21.1 file;
      the 26.1 build has no EMI plugin, because no 26.x NeoForge build of EMI exists yet.
- [ ] Changelog: paste the newest section from `CHANGELOG.md`.
- [ ] Gallery: screenshots are still missing, and this is the one thing that decides whether anyone
      clicks. At minimum: the beacon screen with both tabs open, the effect picker, and the JEI fuel
      category.
- [ ] Link the wiki and the issue tracker.
