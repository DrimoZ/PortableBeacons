# Store copy

Paste-ready text for the CurseForge project page,
<https://www.curseforge.com/minecraft/mc-mods/portables-beacons>. Not documentation: this file sells
the mod, the [wiki](https://github.com/DrimoZ/PortableBeacons/wiki) explains it. Every claim here is
something the mod does today; check it against the data files and `CHANGELOG.md` before changing a
number.

Images are generated, not committed: `java tools/Banners.java run/screenshots/preview run/store-art`
after a GUI preview run (see the tool's header). Upload each one through the description editor's
image button, which refuses anything wider than 850 px, then replace its `UPLOAD:` placeholder below
with the URL CurseForge gives it.


## Summary

> One line, 256 characters at most, shown under the name in every search result.

A beacon you carry. Pick its effects, choose who they reach, and pay for them in fuel. Four tiers, three themed beacons, fourteen augments, and everything defined in datapacks.

## Categories

Main: **Equipment**. Additional: Utility & QoL, Adventure and RPG.

## Licence field

Custom. Point it at `LICENSE` in the repository: MIT for the code, all rights reserved for the
artwork with redistribution granted.

## Description

<!-- Everything below this line is pasted into CurseForge's Markdown editor as-is. -->

![Portable Beacons](https://media.forgecdn.net/attachments/description/1650659/description_3f4d10a1-e6cc-40bb-90c9-a88bf1b6baa4.png)

### A beacon you carry.

Put a Portable Beacon in your inventory and it projects beacon effects: on you, and once it can
share, on the people around you. Which effects, how strong, who they reach and what they cost are
all set in its own screen. Craft it into the next tier and it keeps everything it carried.

It does not replace the Beacon block. Every Portable Beacon is crafted from one, its reach stays
below the block's, an effect a real beacon already gives you costs nothing, and a real beacon
recharges the ones you carry. The block is still what you build for a base; this is what you take
with you.

![The beacons](https://media.forgecdn.net/attachments/description/1650659/description_2df3ae8f-8f01-4846-8d9d-f74f106c69b0.png)

![Eight beacons](https://media.forgecdn.net/attachments/description/1650659/description_c868bd6b-73ca-46b6-ae57-4f9546402e13.png)

| | Effects | Max level | Reaches | Range | Augment slots |
|---|---|---|---|---|---|
| **Beacon I** | 1 | I | you | — | 0 |
| **Beacon II** | 1 | I | you | 8 blocks | 1 |
| **Beacon III** | 2 | I | your team | 12 blocks | 2 |
| **Beacon IV** | 3 | II | your team | 16 blocks | 4 |

- **Cinder, Void and Tidal beacons** trade the standard list for effects the tiers never offer: fire
  resistance and strength, slow falling and night vision, water breathing and conduit power.
- **A Creative Beacon** for builders and pack makers: every slot, every level, and no fuel.
- **One runs at a time.** Switching one on switches your others off. Wear it in a
  [Curios](https://www.curseforge.com/minecraft/mc-mods/curios) charm slot if you like.

![One screen](https://media.forgecdn.net/attachments/description/1650659/description_6ec9dc00-ec3b-47fe-b32a-fbd529913d79.png)

![The beacon screen](https://media.forgecdn.net/attachments/description/1650659/description_c13ef2b8-455b-4351-8956-fdc68469c9c4.png)

One row per effect, and everything set right there: click the icon to choose the effect, the level
to raise it, the figure to choose who it reaches, the switch to turn it off. Right-click steps back.

- **A bar under each effect** shows its share of what the beacon burns.
- **A status light** says whether it is running, idle, low on fuel or dry.
- **The beacon's figures and its augments** live in side tabs.
- **Effects are picked from a searchable grid**, keyboard included.

![Choosing an effect](https://media.forgecdn.net/attachments/description/1650659/description_0f87fb8c-cf5e-4b1e-ad39-1ef4dd0f3f1e.png)

![Augments](https://media.forgecdn.net/attachments/description/1650659/description_fd5f1db3-cb61-4f53-953c-fb8c101ea8f0.png)

![Fourteen augments](https://media.forgecdn.net/attachments/description/1650659/description_616748f0-9e37-45aa-8932-29ae1117e7fd.png)

Seven are pure gains: **Range**, **Focus** (+1 effect), **Amplification** (+1 level), **Efficiency**
(up to −55% fuel), **Capacity** (up to ×4 buffer), **Attunement** (share wider) and **Discretion**
(hide the particles).

Seven give something up, and those are where the decisions are:

| Augment | Gains | Pays |
|---|---|---|
| **Communion** | sharing costs much less | everything costs more |
| **Wellspring** | your most expensive effect is free | the rest cost ×1.6 |
| **Wayfarer** | cheap while you move | dear while you stand still |
| **Sentinel** | cheap while you stand still | dear while you move |
| **Vanguard** | +12 blocks and wider sharing | fuel ×1.9 |
| **Prism** | +1 effect and +1 level | fuel ×2.2 |
| **Recluse** | fuel ×0.35, triple buffer | keeps everything to you |

Fourteen augments, at most four slots: the question is never which ones you want, but which four.

![Fuel](https://media.forgecdn.net/attachments/description/1650659/description_2a955d4f-a1b4-4c80-8979-e7e8b39d57e2.png)

Every effect burns fuel each second, more for higher levels and more again for sharing it. Copper,
iron, gold, emerald, diamond and netherite each last longer than the last.

- **The screen shows time, not units**: how long the beacon lasts at what it is drawing now.
- **A real beacon recharges it.** Stand in a lit beacon's range and every beacon you carry tops up.
- **Forge Energy.** Any mod's charger fills it.
- **A minute's warning** before it runs dry, and when it does, it waits and starts again by itself
  as soon as there is fuel.
- **Servers can turn fuel off entirely**: the slot and gauge disappear with it.

![Data-driven](https://media.forgecdn.net/attachments/description/1650659/description_cdd0e9e0-9035-4b96-bc91-e45484b09ce5.png)

Effects, tiers, augments and fuels are datapack files. A pack can retune every number, add any
effect from the game or another mod, or make a new augment with its own colour and icon, all
without code. The shipped list sticks to the vanilla beacon's kind of effects on purpose.

Nine server options cover the rest: fuel on or off, PvP-safe sharing, a global cost multiplier, a
reach cap, dimensions where beacons switch off, and more.

### Requirements

| | 1.21.1 | 26.1 | 26.2 |
|---|---|---|---|
| Loader | NeoForge 21.1 | NeoForge 26.1.2 | NeoForge 26.2 |
| [Curios](https://www.curseforge.com/minecraft/mc-mods/curios) *(optional)* | 9.5+ | 15.0+ | 16.0+ |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) *(optional)* | 19.x | 29.x | 30.x |
| [EMI](https://www.curseforge.com/minecraft/mc-mods/emi) *(optional)* | 1.1+ | — | — |

Needed on both the client and the server. All three versions have the same content.

### FAQ

**Does it make the Beacon block pointless?** No. You need one to craft any Portable Beacon, the
block reaches further, and standing near one makes your portable beacon's matching effects free.

**Can I add my own effects?** Yes, with one datapack file each. See the
[datapack guide](https://github.com/DrimoZ/PortableBeacons/wiki/Datapack-Guide).

**Fabric? 1.20.1?** No Fabric. 1.20.1 is not available yet.

**Can I put it in my modpack?** Yes. No need to ask.

### Permissions

**Modpacks: yes.** No permission needed, no message required, public or private, monetised or not,
on any platform or launcher.

**Credit** is appreciated and never required.

**Forks and addons: yes**, under the MIT terms. Please do not publish a fork under the name
*Portable Beacons*: the name is not covered by the licence.

**Assets** (icons, GUI artwork, logo) are the one exception: redistribute them with the mod freely,
but do not lift them into another project.

### Links

[Wiki](https://github.com/DrimoZ/PortableBeacons/wiki) ·
[Source](https://github.com/DrimoZ/PortableBeacons) ·
[Report a bug](https://github.com/DrimoZ/PortableBeacons/issues)

<!-- End of the pasted description. -->


## Release checklist: 2.0.0

- [x] Upload the images from `run/store-art/` and replace every `UPLOAD:` above.
- [x] Paste the description.
- [x] Three files, release type **Release**, each with the `## 2.0.0` section of `CHANGELOG.md`:

| File | Game version | Loader | Java |
|---|---|---|---|
| the jar built on branch `1.21.1` | 1.21.1 | NeoForge | 21 |
| the jar built on branch `26.1` | 26.1.2 | NeoForge | 25 |
| the jar built on branch `26.2` | 26.2 | NeoForge | 25 |

- [x] Optional dependencies on every file: Curios and JEI; EMI on the 1.21.1 file only.
- [x] Gallery: `screen.png`, `picker.png`, `items_augments.png`, `items_beacons.png`.
