# Roadmap

**2.0.0 on three Minecraft versions: 1.21.1, 26.1 and 26.2, with the same content.** Released on
CurseForge on 2 October 2026 and played on all three. Only the
plumbing differs where the game's API does. `main` follows the newest version, 26.2.

What follows is ordered by what actually moves the mod forward — which is no longer "what is
missing" but "what do players hit first". The pre-release checklist that used to live here is done;
its history is in the git log and the [changelog](CHANGELOG.md).

---

## Now

**Bump Curios on 26.2 once it stops using `logoFile`.** Curios 16.0.0+26.2 still does, and the 26.2
client opens on a "1 warning" screen for it - ours is fixed, theirs is not. Nothing to do but
change `curios_version` when a fixed 26.2 build appears.

**Decide on 1.20.1.** Feasible, at roughly twice the cost of the 1.21.1 backport - no data
components, an older capability and networking system, no GUI sprites - and most 1.20.1 packs run
Forge rather than NeoForge.

## Then — the first week

**Watch the issue tracker.** This is the whole point of shipping. Until reports come in, every
priority below is a guess, and a guess should lose to a real report every time.

**Tell people it exists.** r/feedthebeast and r/MinecraftMods on a weekend, the NeoForged Discord's
showcase channel. Modpack authors are the real growth channel — a small utility mod with a clean
GUI and datapack hooks is easy to include, and the permissions block already says yes for them.

## The 26.x line

**1.21.1 → 26.1 → 26.2 → 26.3.** One hop at a time, a published file for each, rather than one jump
to whatever is newest.

**26.1 is done.** Ten primers separated it from 1.21.1, and the full analysis of what broke and why
is in [PORTING.md](PORTING.md) — including where that analysis guessed wrong. What is left is play,
not compilation.

**26.2 branches from 26.1 once 26.1 has been played**, not before, or it is an empty copy that
drifts. EMI returns whenever a 26.x NeoForge build appears; the plugin is still in the tree.

`1.21.1` stays maintained throughout. Every extra branch multiplies each bug report by the number of
branches, and reports have only just begun.

## Still open

**One translation reviewed by a native speaker.** Fifteen locales ship and all fifteen are mine. The
first correction from a real player outranks anything I wrote.

**A balance pass on sharing.** 1.0.1 made Attunement matter by taking wide sharing away from every
tier that used to have it for free. That is the right shape, but the numbers have never been played
— only tested. Whether Beacon II spending its single augment slot on Attunement feels like a
decision or a tax is something only playing will say.

## Backlog

Ranked by impact on players and pack authors against what it costs, with the cost counted on
three branches - anything in code lands on 1.21.1, 26.1 and 26.2. Real issue reports outrank this
whole list the day they arrive.

| # | Item | Why | Cost |
|---|---|---|---|
| 1 | ~~**A key to switch the beacon on and off**~~ - done on all three versions | Stopping the drain mid-fight meant opening the screen; with a worn beacon there is no item to click at all | Small: one payload, one bind |
| 2 | **Check team sharing against FTB Teams / Open Parties and Claims** | Team mode asks `isAlliedTo`, i.e. vanilla scoreboard teams. Neither mod documents a bridge to them, so team sharing may silently do nothing in the packs this mod grows through. Next step: two players in one FTB team, in a pack, before writing any binding | Small to check, medium to bind |
| 3 | ~~**An optional built-in "Extended effects" datapack**~~ - done on all three versions | Luck, Hero of the Village, Invisibility - off by default so the default pool stays vanilla. Absorption and Health Boost left out: re-applied every pass, they misbehave | Small: data plus a pack finder |
| 4 | ~~**A fuel and runtime HUD while the beacon runs**~~ - done on all three versions | A fuel-burning item with no gauge on screen is checked by opening it, which is what the dry warning papers over | Medium: a GUI layer, a toggle |
| 5 | **Publish on Modrinth too** - upload task wired on 26.2 (`gradlew modrinth`); waits on the project being created and a token | The direct competitors (Inventory Beacon, Beacon Overhaul Continued) live there; one Gradle task can upload to both, so it is not two pages kept half-current | Small once `curseforge-gradle` is wired |
| 6 | ~~**Specialised example augments** (Sprinter, Miner, Diver)~~ - done on all three versions | Shows off `add_effect_amplifier` / `mul_effect_cost`, which nothing shipped uses | Small: data, glyphs exist |
| 7 | ~~**Effect presets**~~ - done on 26.2 | Two or three saved loadouts, switched from the screen | Medium: state, codec, GUI |
| 8 | ~~**A hostile aura mode**~~ - done on 26.2, as the `mark_hostiles` operation rather than a mode | Glowing or Slowness on monsters in range, behind a costly augment - the one idea no other beacon mod does well | Medium: new `AuraMode`, balance |
| 9 | ~~**New augment operations**: solar recharge, last-stand burst, lingering effects~~ - done on 26.2 | Each opens a family of datapack augments rather than one item | Medium each |
| 10 | ~~**A Nether Star tier V**~~ - done on 26.2 | An endgame target; must stay under the placed beacon's reach | Small in data, needs a balance pass |
| 11 | ~~**More fuels** (amethyst, echo shard)~~ - done on 26.2, numbers unplayed | Renewable early fuel. Waits on the sharing balance pass, since fuel value is the other half of the same numbers | Data only |
| 12 | ~~**`/portablebeacons inspect`**~~ - done on 26.2 | Computed stats of the held beacon, for datapack authors | Small |

Not on it: Fabric and 1.20.1 before reports exist, and a block that holds a portable beacon - it
would compete with the vanilla beacon this mod is careful not to replace.

## Settled

**The rename.** Done fully: display name, mod id, Java package, every item id, the data component,
the item tag, the datapack paths, the repository and the CurseForge page. "BeaconPack" read as a
modpack, which is the one thing this is not.

I argued for changing only the display name and keeping the ids, because an id is baked into every
saved beacon and into any datapack written against it. That reasoning is right in general and was
wrong here twice over: the beta had been public a day, and 1.0.0 had not shipped at all. If the ids
ever have to move again, it belongs to a 2.0 with a migration, not to a patch.

**CurseForge first, Modrinth from the same jar.** One page kept current beats two kept half-current,
which is why Modrinth waited. What changed is that the upload no longer means a second page to keep
by hand: `gradlew modrinth` sends the jar with the newest CHANGELOG section as its notes, so both
stores say the same thing from the same file.

## Publishing mechanics

Manual uploads to CurseForge for now; Modrinth through `gradlew modrinth`, with `MODRINTH_TOKEN`
in the environment and `modrinth_project` set. Once CurseForge has accepted one by hand,
`curseforge-gradle` can publish from a local Gradle task the same way — no CI needed, which is consistent with this project not having any.

Keep the changelog current as things land: the store page renders its release notes straight from it.
