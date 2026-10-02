# Portable Beacons — Design (phase 1)

> Written under the mod's original name, BeaconPack, and renamed in place. The reasoning is
> unchanged; only the identifiers moved.

> Mod NeoForge 1.21.1. Objectif : **petit mod**, une seule idée, exécutée proprement et data-driven.
> Ports prévus : 26.1 puis 26.2. Backport 1.20.1 possible → contrainte d'architecture (voir §9).

## 1. Concept

Un item d'inventaire qui reproduit les effets d'un beacon, en 4 tiers, modulable par des
augments physiques, avec un coût d'entretien en carburant. Tout le contenu (effets autorisés,
tiers, augments, valeurs de carburant) est défini en datapack.

## 2. Positionnement (concurrence)

| Mod | Version | Résumé | Faiblesse |
|---|---|---|---|
| Portable Beacons (Feaseron) | 1.16.5 | Backpack 5 tiers, copie d'un beacon actif, Curios obligatoire | Mort en 2022, mod dispersé (armes, structures, blocs), aucun coût |
| Wither Star Trinkets | 1.20.1 | 3 trinkets, coût en durabilité + cooldown | Mort, ARR, figé, non configurable |
| Portable Beacon | 1.20.1 | 1 item = tous les effets | Cheat item, aucun design |
| Inventory Beacon | 1.21.1/1.21.4 | GUI beacon en inventaire, lingot → effet temporaire | Minuscule, pas de portée, pas de progression, non data-driven |
| Beastly Beacons | — | Beacon posé alimenté à l'XP | Concerne le bloc, pas un item |

**Créneau libre** : rien de moderne, rien de data-driven, rien de modulaire, aucune vraie UI.

## 3. Les 4 tiers

Le beacon vanilla porte à 20/30/40/50 blocs mais il est **fixe**. Un beacon mobile à portée
égale serait très supérieur → portées volontairement courtes.

| Tier | Effets | Amplitude | Portée aura | Slots d'augment |
|---|---|---|---|---|
| T1 | 1 | I | soi seul | 0 |
| T2 | 1 | I | 8 blocs | 1 |
| T3 | 2 | I | 12 blocs | 2 |
| T4 | 2 | dont 1 en II | 16 blocs | 3 |

Pool d'effets par défaut = strictement celui du beacon vanilla (Speed, Haste, Resistance,
Jump Boost, Strength, Regeneration). Élargissable en datapack, mais **pas par défaut** :
ouvrir le pool transforme un mod d'utilité en mod cheat.

**Pas de distinction primaire/secondaire** (contrairement au beacon vanilla). Le modèle
vanilla est un artefact de son UI figée ; il ne survit pas à un pool data-driven. À la place,
un pool plat où chaque entrée déclare son propre coût et son amplitude maximale :

```json
{ "effect": "minecraft:regeneration", "cost": 3.0, "max_amplifier": 1, "min_tier": 3 }
```

Le tier (+ augment Focus) donne un nombre de **slots de sélection** ; chaque effet sélectionné
porte son propre niveau (I/II), cliquable si l'entrée et le tier l'autorisent.

## 4. Garder le beacon vanilla pertinent

Risque n°1 du mod. Trois garde-fous cumulés :

1. **Craft** : chaque tier consomme un bloc Beacon (T3/T4 : + Nether Star).
2. **Portée courte** : le beacon posé reste supérieur pour une base ou une team statique.
3. Option config `require_beacon_to_configure` (défaut `false`) : la GUI n'autorise le
   changement d'effets qu'à proximité d'un beacon actif de niveau suffisant. Pour les modpacks
   qui veulent la progression stricte.

## 5. La GUI

> **Note (état actuel)** — la disposition décrite ci-dessous a été remplacée. L'écran suit
> désormais la charte de FactoryIO (`GuiMetrics`, `GuiTheme`, `GuiSprites`) : 176 de large, tout
> aligné sur les colonnes de l'inventaire. Le bandeau porte le titre, l'interrupteur général et le
> voyant d'état. La colonne 0 porte la jauge et le slot de carburant. Le reste est **un tableau,
> une ligne par effet**, où tout se règle sur place : icône (changer / retirer), badge de niveau
> (monter / descendre), silhouette (qui reçoit l'effet), interrupteur, et sous le nom une barre de
> la part de consommation. Il n'y a plus de case « sélectionnée » dont les réglages vivaient
> ailleurs : c'était un état que le joueur devait garder en tête. Les chiffres du pack sont un
> onglet jaune à gauche, les augments un onglet bleu à droite. Le sélecteur est une grille modale
> qui recouvre tout l'écran, avec une case « retirer » en tête. Les schémas ci-dessous sont gardés
> pour l'historique du raisonnement.

Ouverture : clic-droit depuis n'importe quel slot d'inventaire, **ou** touche configurable
(cf. §5.2). Écran complet avec un vrai `AbstractContainerMenu` (slots réels).

```
┌────────────────────────────────────────────────────────────┐
│  Beacon Pack IV                                  [⏻ ON ]   │  ← coupe-circuit global
│                                                            │
│   EFFETS                                          2 / 3    │
│   ┌────┐┌────┐┌────┐                                       │
│   │ 🏃 ││ ❤  ││ +  │   clic = sélecteur                    │
│   │ II ││ I  ││    │   clic-droit = vider                  │
│   └────┘└────┘└────┘                                       │
│                                                            │
│   ┌──────────────────────────────────────────────────┐     │
│   │ 🏃 Rapidité II                                   │     │
│   │ Augmente la vitesse de déplacement de 40 %.      │     │
│   │ Coût 0,8 u/s · Portée 24 m · min. tier II        │     │
│   │ [ Niveau ‹ II › ]  [ ⏻ Actif ]  [ 👥 Alliés ]    │     │  ← panneau d'info contextuel
│   └──────────────────────────────────────────────────┘     │
│                                                            │
│   AUGMENTS            CARBURANT                            │
│   ┌──┐┌──┐┌──┐        ┌──┐ ▓▓▓▓▓▓░░░  412 u. · ~55 min     │
│   │◈ ││◈ ││🔒│        │💎│ conso totale : 1,4 u/s          │
│   └──┘└──┘└──┘        └──┘                                 │
│                                                            │
│   ┌────────────────────────────────────────────────┐       │
│   │      inventaire du joueur (3×9) + hotbar       │       │
│   └────────────────────────────────────────────────┘       │
└────────────────────────────────────────────────────────────┘
```

### 5.1 Cases d'effet + sélecteur

L'écran principal n'affiche **que les cases d'effet du pack** (1 à 3 selon tier + augment
Focus), pas le pool. Conséquence directe : le nombre d'effets déclarés en datapack n'a
**aucun** impact sur la mise en page. Un modpack peut en déclarer 40, l'écran est identique.

- **Case d'effet** : dessinée comme un slot mais elle ne contient pas d'`ItemStack` — c'est une
  donnée. Icône de l'effet + niveau en coin + pastille grisée si l'effet est désactivé
  individuellement. Case vide = `+`. Case hors-tier = cadenas + tooltip « nécessite tier III ».
- **Clic gauche** → ouvre le **sélecteur** en overlay. **Clic droit** → vide la case.

Sélecteur (overlay modal, centré sur la case) :

```
      ┌───────────────────────────────┐
      │ 🔍 rege▏                      │  ← champ de recherche, masqué si le
      ├───────────────────────────────┤    pool tient sur une page
      │ ❤  Régénération      3,0 u/s  │ ▲
      │ 🛡  Résistance        2,5 u/s  │ ║  ← liste scrollable
      │ ⛏  Célérité          1,5 u/s  │ ║
      │ 🔒 Force          tier III    │ ▼  ← verrouillé, affiché quand même
      └───────────────────────────────┘
```

- Liste scrollable, une ligne = icône + nom traduit + coût. Recherche par nom (filtrage
  client, `EditBox`), affichée seulement si le pool dépasse une page.
- Les entrées verrouillées sont **affichées et grisées** avec la raison, jamais masquées : le
  joueur voit ce qu'il gagnera en montant de tier.
- Les effets déjà présents dans une autre case sont grisés (pas de doublon).
- **Survoler une ligne met à jour le panneau d'info du dessous** → on compare coût, portée et
  niveau max avant de valider. C'est le point qui rend le choix éclairé plutôt qu'à l'aveugle.

### 5.2 Panneau d'info

Sous les cases, un panneau contextuel décrivant l'effet de la case sélectionnée (ou de la ligne
survolée dans le sélecteur) :

- Nom + niveau, description traduite, coût en u/s réel (déjà multiplié par le niveau, la portée
  et les augments), portée effective, tier minimum.
- **`[ Niveau ‹ I/II › ]`** — borné par `max_amplifier` de l'entrée et par le tier/augment
  Amplification.
- **`[ ⏻ Actif ]`** — activation/désactivation **par effet**. Permet de garder une
  configuration en place sans la payer : on laisse Force configurée et éteinte, on l'allume
  avant un combat. Beaucoup plus agréable que de reconfigurer à chaque fois.
- **`[ 👤 Moi | 👥 Alliés ]`** — mode d'aura **par effet**. T1 : verrouillé sur « Moi ».
  T2+ : « Alliés » déverrouillé. L'augment Attunement ajoute les modes « Team uniquement » et
  « + créatures apprivoisées ». Diffuser un effet coûte plus cher que le garder pour soi → on
  partage la Régénération au groupe et on garde la Célérité pour soi. C'est un vrai arbitrage,
  pas un réglage cosmétique.
- Le coupe-circuit global en haut à droite reste au-dessus de tout : il coupe la consommation
  d'un clic sans toucher à la configuration.

### 5.3 Ouverture

- Clic-droit sur le pack **depuis n'importe quel slot** de l'inventaire (pas seulement la main).
- **Touche configurable** (`KeyMapping`, défaut `B`) : ouvre le premier pack trouvé — priorité
  à la main principale, puis ordre des slots. Envoie un packet C2S ; le serveur localise le
  pack et ouvre le menu lui-même (jamais le client qui décide de l'index).
- Dans les deux cas, l'index du slot est mémorisé par le menu pour le verrouillage (cf. §9).
- **Slots d'augment** : 3 slots physiques, ceux au-delà du tier sont rendus verrouillés
  (`mayPlace()` → false, overlay cadenas). Filtre : `portablebeacons:augment` uniquement, et
  **refus d'un second augment du même type** (règle « 1 max par type » appliquée dans le slot,
  pas seulement en logique).
- **Slot carburant** + jauge : quantité restante, consommation/s, autonomie estimée en clair.
  L'autonomie estimée est ce qui rend le coût acceptable plutôt qu'anxiogène.
- **Toggle ON/OFF** + (si augment Attunement présent) un bouton de mode d'aura
  (soi / team / tout le monde).
- Panneau de droite recalculé en direct quand on change un augment → le joueur *voit* l'effet
  de sa modification avant de fermer.

## 6. Carburant

Activé par défaut, désactivable (`require_fuel`).

- Valeurs data-driven : fer 1, or 2, émeraude 4, diamant 8, netherite 32 « unités ».
- Consommé depuis le **slot carburant** de la GUI, converti en unités dans un buffer interne
  (capacité définie par le tier, ×N via l'augment Capacity).
- Consommation = somme **par effet actif** de
  `cost × mult_niveau × mult_aura(portée) × mult_efficiency`.
  Un effet éteint individuellement ne coûte rien ; un effet en mode « Moi » ignore totalement la
  portée. La formule est donc lisible ligne par ligne dans le panneau d'info, au lieu d'être un
  chiffre global opaque.
- **Coupe-circuit global** : rien consommé à l'arrêt. Non négociable.
- Rien consommé si un vrai beacon fournit déjà l'effet au joueur.
- Barre de durabilité détournée sur l'item → carburant visible sans ouvrir la GUI.
- Extension prévue : `fuel_source: item | xp` (pas par défaut, conflit avec l'enchantement).

## 7. Augments (= items)

Puisqu'ils vont dans des slots, ce sont de vrais items. Pour rester data-driven **sans**
exploser le nombre d'items enregistrés :

> **Un seul item** `portablebeacons:augment`, dont l'identité vient d'un composant
> `portablebeacons:augment_type` = `{ type: <id de registry datapack>, tier: 1..3 }`.

Un modpack peut donc **ajouter** de nouveaux augments en datapack (entrée de registry +
recette), sans code. L'onglet créatif énumère toutes les entrées enregistrées.
Rendu : teinte définie dans le JSON appliquée à une texture de base (zéro fichier modèle) ;
option `model_data` dans le JSON pour ceux qui veulent un modèle dédié via overrides.

| Augment | Effet |
|---|---|
| Range | +4 / +8 / +12 blocs |
| Focus | +1 slot d'effet |
| Amplification | +1 niveau (coût de carburant fortement majoré) |
| Efficiency | −25 / −40 / −55 % de consommation |
| Capacity | buffer ×2 / ×3 / ×4 |
| Attunement | aura limitée à la team / inclut les mobs apprivoisés |
| Discretion | masque les particules d'effet, et l'icône d'état au tier II |

Chaque entrée déclare une liste d'opérations : `add_range`, `add_effect_slot`,
`add_amplifier`, `mul_fuel`, `mul_capacity`, `set_aura_filter`.

## 8. Confort joueur

- Actif depuis **n'importe quel slot d'inventaire**, pas seulement la main.
- Support Curios/Trinkets en **soft dependency** uniquement (l'erreur de Portable Beacons était
  de l'imposer).
- Effets appliqués en mode *ambient* (réappliqués ~toutes les 11 s, particules discrètes),
  comme le beacon : pas de spam d'icônes.
- Tooltip complet hors GUI : effets, portée, augments, carburant restant.
- Config serveur : aura désactivable sur les joueurs hors-team (PvP).
- Perf : scan inventaire + AABB des joueurs proches toutes les **40 ticks**, pas chaque tick.

## 9. Architecture technique

- 4 items distincts (`beacon_pack_t1..t4`) plutôt qu'un item + composant tier → recettes, tags
  et intégration JEI nettement plus simples.
- **Tout l'état du pack dans un composant unique** `portablebeacons:pack` :
  effets sélectionnés, carburant restant, actif o/n, mode d'aura, et le contenu des slots
  (augments + carburant) via `ItemContainerContents`.
- Inventaire item-backed : `ComponentItemHandler` (NeoForge) plutôt qu'un `SimpleContainer`
  copié/recopié — l'écriture retourne directement dans le composant du stack.
- **4** registries datapack : `portablebeacons/effect` (effets autorisés + coût), `portablebeacons/augment`,
  `portablebeacons/tier`, `portablebeacons/fuel`.
  Le carburant est passé de « tag + valeurs codées » à un registry à part entière : un tag sait
  seulement dire *« ceci est du carburant »*, il ne peut pas porter la valeur par item — et coder
  les valeurs en dur annulait l'intérêt d'avoir rendu tout le reste data-driven.
- Les quatre registries déclarent un **network codec** : la GUI doit afficher coûts, plafonds et
  tiers requis sans aller-retour serveur.

### Pièges du menu item-backed (à traiter dès le départ)

1. **Le joueur ne doit pas pouvoir déplacer/jeter le pack pendant que sa GUI est ouverte.**
   Pattern shulker : mémoriser l'index du slot d'ouverture, y substituer un `Slot` avec
   `mayPickup() → false`, et `stillValid()` qui vérifie que le stack de cet index est toujours
   le même objet. Sinon : duplication d'items.
2. **Ouverture depuis un slot quelconque** : le slot d'origine peut être dans l'inventaire
   principal, la hotbar ou l'offhand. Le menu stocke cet index et le neutralise ; la touche
   configurable passe par un packet C2S où **le serveur** choisit l'index (ne jamais faire
   confiance à un index envoyé par le client — c'est la porte ouverte à l'ouverture d'un
   inventaire arbitraire).
3. **Boutons** : `clickMenuButton(id)` avec un id encodant (action, index) — pas de packet
   custom à écrire, et validé serveur. Suffit pour toggle, sélection d'effet et niveau.
4. **Valeurs affichées** (carburant, conso, portée) : `ContainerData` / `DataSlot`, recalculées
   serveur. Ne jamais calculer la portée côté client seul, sinon désync à chaque changement
   d'augment.

### Contrainte de portabilité (ports 26.x / backport 1.20.1)

Les data components n'existent pas en 1.20.1, l'API réseau a changé en 1.20.5, et
`ComponentItemHandler` est spécifique NeoForge. Discipline dès le départ, sans outillage
multiloader :

- package `core/` : logique pure (résolution tiers/augments, calcul de conso, application des
  effets, validation d'une config de pack) — **aucune** référence aux components, aux packets
  ou au rendu. C'est aussi ce qui rend cette logique testable en unitaire.
- package `platform/` : sérialisation de l'état (component en 1.21+, `CompoundTag` en 1.20.1),
  inventaire item-backed, menu, écran, registries.
- Toute la sérialisation passe par un seul `PackState` avec un `Codec` unique. C'est le
  principal point à réécrire pour un backport.

## 10. Périmètre — hors scope explicite

4 items de pack + 1 item d'augment + 1 GUI + carburant + registries data-driven. **Point.**
Pas de structures, pas d'armes, pas de blocs décoratifs, pas de dimension, pas de mobs.
C'est exactement là que Portable Beacons s'est noyé.
