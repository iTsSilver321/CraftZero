# CraftZero Release 1.0 Parity Report

Target: Minecraft Java Release 1.0-style behavior across the whole project.

Current status: partial parity. CraftZero now has many Release-era systems in
place, including registries, crafting/smelting/brewing scaffolding, tile
entities, redstone/mechanisms, fluids, mobs, dimensions, structures, and
save/load coverage. It is not yet safe to call the game finished: every
category below still needs source comparison, focused fixes, or visual/runtime
verification before the 1.0 target is credible.

## Verification Policy

- Prefer focused tests for changed behavior.
- Run broad suites only when touching shared engine code such as `World`,
  registries, entity base classes, chunk save/load, rendering primitives, or
  redstone scheduling.
- Preserve vanilla rules even when a fixture is fragile. Tests should set up
  the world state they depend on instead of weakening gameplay logic.

## Updated In This Pass

- Tightened item-5 mob chase/navigation parity. Shared melee and ranged pursuit
  now lets the MobAI navigator drive normal chase movement instead of setting a
  path target and then overriding it with direct steering every tick; direct
  steering remains for short strafes, retreats, and stuck alternate-path
  fallback. The shared navigator now uses a wider search budget, shorter
  target refresh cadence, and stale-target recalc checks, and MobAI now clears
  stale local/remote combat targets when target type changes or a target is
  rejected.
- Tightened item-5 XP-orb pickup parity. Local XP orbs now use the orb's
  collision box against an expanded player body box for pickup instead of
  measuring distance to the player's eye point, so ground-level orbs at the
  player's feet/body collect through the old contact-style path while
  attraction still pulls toward the player's eye/head height.
- Tightened item-5 natural pack spawning on uneven terrain. Runtime ground
  packs now keep the selected natural Y when it is still valid for a jittered
  member, and otherwise fall back to the nearby local ground search already
  used by explicit pack helpers, so same-pack mobs no longer fail merely
  because one drifted block column is a step higher or lower.
- Tightened item-4 long-tail cake/cauldron parity. Hungry left-click on cake no
  longer consumes a slice through the use path; attack-click falls through to
  ordinary block breaking while right-click still uses the hunger-gated slice
  path with eat/burp feedback. Cauldrons also no longer fill from active rain
  ticks, matching Release 1.0 before the later 12w22a/1.3.1 weather-fill
  mechanic; bucket fills and glass-bottle drains remain intact.
- Tightened item-4/item-9 locked-chest creative exposure parity. The legacy
  locked chest remains registered under its Release-era block/item id for
  commands, save/load, block breaking, scheduled decay, piston mobility, and
  old-world compatibility, but the creative inventory catalog now treats it as
  command/editor-only alongside fire, mob spawners, double slabs, and monster
  eggs instead of surfacing it as an ordinary creative-visible gameplay block.
- Tightened item-7/item-9 achievement-toast icon fade parity. The survival HUD
  now has alpha-aware flat sprite and isometric block icon helpers, and the
  achievement-toast icon uses those helpers directly during slide/fade instead
  of rendering a full-strength icon and masking it with a dark cover. Toast
  frames, slot art, title text, special/normal achievement state, ordinary
  hotbar icons, status-effect icons, stack counts, glint, and dynamic
  clock/compass overlays keep their existing rendering paths.
- Tightened item-7/item-9 menu item-icon visual parity. The shared menu
  renderer now draws block item icons, including achievement-tree node icons,
  with the same compact source-style isometric cube proportions and face
  brightness used by inventory/container/cursor/hotbar rendering instead of
  flattening every block item into a square terrain tile. Non-block item icons
  keep the flat sprite path with the same half-pixel UV inset, and shaded
  achievement background tiles remain flat texture tiles so the background
  surface does not turn into repeated item cubes.
- Tightened item-7/item-9 item and block icon visual parity. Inventory,
  container, cursor, and survival-hotbar icon renderers now share the same
  compact source-style isometric block-icon proportions, including matching
  top/side face brightness and a slightly raised centered cube silhouette
  instead of changing shape between GUI and HUD paths. Flat item sprites and
  block icon faces now also use a half-pixel atlas UV inset in both renderers,
  reducing terrain/items atlas edge bleed while preserving potion overlays,
  dynamic clock/compass needles, stack counts, and enchanted glint passes.
- Tightened item-7/item-9 first-person sprite hand-follow polish. Flat held
  item sprites keep the existing lower-right, face-on item pose, but the
  visible first-person hand now derives its softened swing translate/rotation
  from the same sprite swing constants at a controlled follow ratio instead of
  separate hard-coded values. Tools, food, materials, sticks, arrows, and other
  non-block sprites therefore stay attached to the hand more consistently
  during ordinary swings without retuning block/cube holding, maps, bow draw,
  blocking, eating/drinking, or glint rendering.
- Tightened item-7 Snow Golem model proportion parity. The Snow Golem model now
  derives its Y-up head, torso, lower-body, and stick-arm anchors from the old
  source snowman part pivots instead of hand-placed CraftZero values, restoring
  the one-model-unit torso/head overlap and raising the stick arms to the
  Release-era shoulder band while preserving the existing quarter body-yaw arm
  orbit and bundled `textures/mob/snowman.png` texture path.
- Tightened item-7/item-9 first-person held-map parchment visual parity. The
  live 3D held-map texture now draws a subtle source-style inner content frame,
  paper bevel, and shadow/highlight edge pass around the 128x128 map data
  instead of dropping raw map pixels straight into the parchment. The
  first-person player marker is now a smaller black-outlined white pointer with
  a dimmer clamped-edge variant, so same-dimension off-map markers stay visible
  without dominating the held map. Map data, player marker tracking, map save
  data, and the two-hand grip pose are unchanged.
- Tightened item-7/item-9 3D Anaglyph color parity. The anaglyph world render
  still uses the existing Release-style red/cyan stereo eye passes, but scene
  shader output now applies the old color-remap weights while the anaglyph
  world pass is active: red from `30/59/11` luminance, green from `30/70/0`,
  and blue from `30/0/70`. The correction is scoped to the stereo world
  scene, including fogged blocks, entities, particles, held/dropped glint, and
  dropped items; HUD, inventory, menus, and other 2D overlays render normally
  after the pass resets the flag.
- Tightened item-7/item-9 enchanted glint visual parity. The shared enchanted
  item visual helper now uses a 60-tick source-style scroll cadence, source
  purple tint ratios, a softer fallback wash, and a three-repeat texture scale
  for the bundled `misc/glint.png` pass. First-person/third-person held items
  and dropped items now consume the same shared glint phase instead of keeping
  duplicated 64-tick constants, and the scene shader's glint UV scale/default
  tint matches the GUI/hotbar/cursor overlay tuning.
- Tightened item-9 Statistics screen visual layout parity. The live menu and
  factory menu paths now share the same compact Release-style statistics
  geometry: lower tab strip, 12-pixel table rows, a taller visible list area,
  and consistent tab/list bounds. The statistics renderer now uses a matching
  compact text baseline and object-counter column layout so Blocks and Items
  pages keep their five count columns aligned without crowding the object-name
  column differently between menu implementations.
- Tightened item-7/item-9 first-person sprite hand anchor polish. The
  first-person hand now has an explicit sprite-item anchor and smaller
  viewmodel scale instead of reusing the empty/held-block hand placement, so
  flat tools, food, materials, sticks, arrows, and similar items sit in a
  cleaner lower-right grip while block/cube holding remains untouched.
- Tightened item-7 first-person sprite hand walk-bob parity. The visible
  first-person hand now follows the same no-extra-walk-bob rule as flat sprite
  items, so tools, food, materials, sticks, arrows, and similar held sprites no
  longer have the hand drifting around them while the item itself stays settled
  in the old lower-right pose. Empty-hand and block/cube hand motion keep their
  existing bob behavior.
- Tightened item-7 first-person sprite hand swing parity. When a flat sprite
  item is held, the visible first-person hand now uses a softened sprite-item
  swing translate/rotate path instead of the heavier empty-hand punch arc, so
  tools, food, materials, sticks, arrows, and similar items stay visually
  attached to the hand during ordinary swings. Block/cube holding keeps the
  existing hand swing, and active bow/block/eat use keeps its specialized hand
  pose.
- Tightened item-7 first-person sprite active-use hand pose parity. Bow draw,
  sword blocking, and eating/drinking now give the first-person hand a matching
  modest use pose when the held stack is a flat sprite item, instead of leaving
  the arm in the neutral lower-right view while the item uses its specialized
  Release-style transform. The item transforms remain unchanged; this only
  keeps the visible hand attached to them during active use.
- Tightened item-7 first-person sprite hand use-jolt parity. The
  first-person hand now skips the generic block-place jab when the held stack
  is a flat sprite item, matching the sprite item renderer's legacy no-action
  branch. Block/cube placement keeps the existing generic hand use motion, but
  tools, buckets, bottles, food, materials, and other non-block sprites no
  longer make the hand twitch separately from the held item when their item
  transform intentionally stays in the normal lower-right pose.
- Tightened item-7 first-person consumable held-state parity. The first-person
  renderer now treats food, milk, and drinkable potions as "eating/drinking"
  only while the player is actively using the held stack, matching the
  third-person pose gate. Edible or drinkable items that are merely held now
  keep the ordinary lower-right sprite swing path instead of being frozen in
  the consume-use branch.
- Tightened item-7/item-9 first-person sprite hand equip timing. The
  first-person hand now follows the same linear equip/drop cadence as flat
  sprite items while block/cube holding keeps its existing eased switch motion,
  so tools, food, materials, and other non-block sprites no longer slide on a
  different timing curve from the visible hand during hotbar changes.
- Tightened item-7/item-9 first-person non-block hand presentation. Flat held
  sprites now render over the same first-person arm/hand viewmodel used by
  empty-hand swings, so swords, tools, food, sticks, arrows, and material
  sprites read as held in the hand instead of floating alone in the lower-right
  view. Block/cube holding remains untouched, maps keep their dedicated
  two-hand map path, and active bow draw, sword blocking, and eating/drinking
  keep their existing specialized item-use transforms while the hand remains
  present behind the item.
- Tightened item-7/item-9 first-person non-block item pose polish. Flat held
  sprites now use the shallower lower-right pose from the latest visual pass:
  0.37 scale, 0.57/-0.57/-0.88 hand offset, 31-degree inward yaw, and the
  softened sprite-only swing amplitudes. Block/cube holding, maps, bow draw,
  sword blocking, eating/drinking, glint, and atlas selection stay on their
  existing branches.
- Tightened item-2 Release standalone player sidecar fallback parity. Importing
  old `players/*.dat` files now tries all regular candidate sidecars instead of
  stopping at the first match: the loader still prefers the normal `Player.dat`
  file, then falls through to newer alternate `.dat` files if the preferred
  sidecar is corrupt, unreadable, or lacks a complete player/inventory payload.
  Valid alternate player sidecars can now replace embedded `level.dat` player
  data instead of being hidden behind one bad file.
- Tightened item-2 `server.properties` world-catalog parity. The singleplayer
  world list now treats a Release-style `server.properties` file as loadable
  world data, includes it in save freshness, and mirrors the loader's
  `level-name`, `level-seed`, `gamemode`/`game-mode`, `difficulty`, and
  `hardcore` overrides for menu metadata. Server-properties-only save folders
  can now bootstrap with the same visible name, seed, mode, and difficulty that
  `SaveManager.createServerPropertiesBootstrap(...)` will apply on load.
- Tightened item-2 Release `level.dat`/`players/*.dat` NBT import hardening.
  The legacy level/player NBT reader now applies the same bounded array/list
  length guard used by the `.mcr` region bridge, so corrupt old byte arrays,
  int arrays, or list payloads fail as unreadable save data instead of trying
  to allocate arbitrary file-declared sizes during metadata import.
- Tightened item-2 save-presence detection parity. `SaveManager.hasSave()` now
  recognizes supported `level.json.bak` fallback saves and Release-style
  `server.properties` bootstrap worlds, matching the loader/catalog paths that
  can already recover from a JSON backup or create a world from server
  properties. The world catalog now also reads `server.properties` as UTF-8,
  matching the real load-time merge path.
- Tightened item-2 stored-enchantment NBT preservation. Release `level.dat`
  player inventories and `.mcr` region stack payloads still import compatible
  `StoredEnchantments` into CraftZero's enchantment model, but now also retain
  the original typed `StoredEnchantments` list as legacy NBT metadata so
  imported old/later-editor item tags are not flattened into ordinary `ench`
  data on sidecar export.
- Tightened item-2 scalar legacy NBT export parity. Release `level.dat` player
  inventory stacks and `.mcr` region stack payloads still emit older scalar
  `nbt.*` compatibility metadata as real top-level legacy string tags, but no
  longer duplicate those same bridge-only keys inside the CraftZero metadata
  compound during Release sidecar export.
- Tightened item-2 empty CraftZero item-metadata sidecar cleanup. Release
  `level.dat` player inventory stacks and `.mcr` region stack payloads no
  longer write an empty `CraftZero/Metadata` compound when every stack metadata
  key was already emitted as typed legacy NBT or skipped as bridge-only data.
  Real CraftZero-only stack metadata still writes through that compound.
- Tightened item-2 empty item `tag` sidecar cleanup. Release `level.dat`
  player inventory stacks and `.mcr` region stack payloads now only write a
  top-level item `tag` compound when at least one display, enchantment, typed
  legacy NBT, scalar legacy NBT, or CraftZero metadata payload will actually be
  emitted, so corrupt/filtered bridge metadata no longer creates empty item
  tags in Release sidecars.
- Tightened item-2 Release `.mcr` chunk fallback parity. If a custom
  CraftZero chunk sidecar exists but fails to decode and has no `.bak` rescue
  file, chunk loading now falls through to the Release region chunk before
  declaring the chunk missing, matching the intended custom-primary,
  custom-backup, then `.mcr` recovery order.
- Tightened item-2 world-catalog backup metadata parity. The singleplayer world
  list now checks the supported `level.json.bak` payload before falling through
  to Release metadata, matching the real loader's primary-JSON, backup-JSON,
  then `level.dat` ordering for displayed world name, seed, mode, difficulty,
  and last-played data. Backup JSON also counts toward the catalog's
  `hasLevelData` flag and freshness timestamp now that it is a valid load
  source.
- Tightened item-2 world-catalog stale-JSON parity. The singleplayer world list
  now applies the same supported `level.json` format-version gate as the real
  loader before using JSON metadata, so stale/unsupported CraftZero metadata
  falls through to valid Release `level.dat`/`level.dat_old` metadata instead
  of masking an imported vanilla-style save in the menu.
- Tightened item-2 Release metadata error handling. When `level.json` is
  absent, a corrupt or unreadable Release `level.dat`/`level.dat_old` now
  returns its blocking load error after any JSON backup rescue attempt, instead
  of being flattened into a missing-save result that could bootstrap over a
  damaged vanilla-style save.
- Tightened item-2 level metadata fallback parity. A save with a present but
  unsupported/corrupt `level.json` now still tries the JSON backup and then
  valid Release `level.dat`/`level.dat_old` metadata before blocking world
  load, preserving normal JSON-first behavior while making imported
  vanilla-style saves less fragile.
- Tightened item-2 Release `.mcr` dimension-runtime import parity. Region
  import now keeps time-only runtime payloads from chunk `LastUpdate`, so a
  normal region with no entities, tile entities, moving pistons, or scheduled
  ticks can still restore per-dimension world time, day count, and moon phase
  instead of being discarded as empty runtime data.
- Tightened item-2 save/load world-catalog parity. The singleplayer world list
  now treats Release `level.dat` and `level.dat_old` as real level data, reads
  their level name, seed, game mode, difficulty, and last-played timestamp for
  menu display, and includes those files in the save freshness calculation.
  Existing `level.json` worlds still load first, but imported/vanilla-style
  save directories no longer appear as generic metadata-only folders.
- Tightened item-7/item-9 first-person sprite item holding parity. Non-block
  held items now use Release-style first-person placement and swing math:
  0.40 scale, 0.70/-0.65/-0.90 hand offset, 45-degree yaw, the 0.60 equip
  drop, and the stronger source-shaped swing translate/rotate arc. The block
  held-item profile remains unchanged, so the already-good block pose is not
  retuned by this pass.
- Tightened item-7/item-9 underwater viewport overlay parity. The survival HUD
  now loads the bundled `textures/misc/water.png`, repeats it as a slow
  drifting full-screen wash whenever `Player.isHeadInWater()` is true, and
  keeps it behind portal, pumpkin, fire, HUD, chat, menu, and inventory chrome.
  Water physics, air/bubble timing, movement drag, item use, and world fluid
  simulation remain unchanged.
- Tightened item-7/item-9 Nether portal overlay parity. The active gameplay HUD
  now loads the bundled `textures/misc/tunnel.png`, repeats it as an animated
  full-screen portal wash, and drives opacity/UV drift from the same
  `netherPortalTime / requiredPortalTime` progress used by dimension transfer.
  The overlay renders behind pumpkin/fire/HUD/menu chrome, while Nether
  transfer timing, Creative instant transfer behavior, multiplayer gating, and
  portal block checks remain unchanged.
- Tightened item-7/item-9 first-person vignette parity. The survival HUD now
  loads the bundled `textures/misc/vignette.png` and draws a full-screen edge
  falloff before helmet/fire/HUD chrome, with opacity shaped from the player's
  current eye-position sky/block light so caves and dark interiors regain the
  old darker edge treatment. Held maps, pumpkin blur, fire overlay, normal HUD,
  chat, menus, inventory, debug text, and gameplay lighting remain on their
  existing paths.
- Tightened item-7/item-9 pumpkin helmet overlay parity. The survival HUD now
  loads the bundled `textures/misc/pumpkinblur.png` mask and draws it across
  the first-person view whenever the helmet armor slot contains a pumpkin,
  covering held-map/world view before the normal health, hunger, XP, hotbar,
  status, boss, achievement, chat, and menu surfaces render above it. Armor
  equip rules, inventory state, multiplayer armor sync, and fire/map overlay
  behavior are unchanged.
- Tightened item-9 in-game chat HUD visual parity. The active chat overlay now
  uses Release-style compact translucent rows, stronger open-state bevels, a
  lower-profile input bar with blinking cursor timing, command-completion rows
  that stay attached above the input, selected-suggestion tracking, long-line
  fitting, and softer closed-chat fade math. Chat open/close keys, message
  history, command submission, multiplayer chat routing, and suggestion
  provider behavior are unchanged.
- Tightened item-9/item-10 Tab player-list visual parity. The active
  multiplayer player-list overlay now uses a compact `Players` header with a
  right-aligned count, stronger old-style bevel/shadow frame, alternating
  low-alpha rows, a clear local-player marker strip, stable column spacing, and
  cleaner two-tone ping glyphs. Roster collection, local/remote row ordering,
  latency thresholds, Tab visibility rules, and networking behavior are
  unchanged.
- Tightened item-9 terrain-loading overlay visual parity. The active "Building
  terrain" screen now keeps the dirt background but uses a classic black inset
  progress bar with old-style light/dark bevel edges, a muted percent readout,
  and a subtle fill shimmer driven by the existing loading timer. Chunk
  readiness, terrain-load gating, dimension transition behavior, and gameplay
  startup logic are unchanged; the chunk count is pushed into a quieter
  secondary line.
- Tightened item-9 status-effect HUD visual parity. Active potion/status
  effects now render as compact beveled upper-right HUD chips with source-era
  highlight/shadow edges, a tiny duration fill strip, and an overlaid amplifier
  numeral only for levels above I. This removes the always-on two-line
  text-panel treatment from normal gameplay while preserving effect sorting,
  warning blink timing, duration math, item-icon fallback, colors, and status
  effect behavior.
- Tightened item-9 achievement-toast visual parity. The in-game "Achievement
  get!" HUD notification now pulls its dark label panel and normal/special icon
  slot from the bundled `textures/achievement/bg.png` source regions instead
  of relying entirely on procedural rectangles. The HUD textured shader now
  supports per-draw alpha so the source-textured toast still slides/fades with
  the existing notification timing; achievement unlock logic, queueing, title
  truncation, icon selection, and screen achievement tree behavior are
  unchanged.
- Tightened item-9 creative inventory visual parity. The active creative item
  browser now draws the scrollbar thumb from the bundled `allitems.png` source
  region instead of a procedural gray block, and the selected hotbar slot uses
  the same `gui.png` 24x24 selected-slot frame as the in-game HUD. Creative
  catalog browsing, hotbar assignment, cursor stacks, tooltips, item glint, and
  close behavior are unchanged.
- Tightened item-9 inventory player-preview visual parity. The inventory screen
  player model now uses the Release-style local mouse-look curve from the
  preview box instead of screen-edge normalization, keeping body yaw, head yaw,
  and pitch tied to the classic `(51,75)` inventory anchor. Equipped armor now
  renders as preview layers over the player skin and turns with the same model
  pose, while the surrounding inventory slots, cursor item, tooltips, and click
  behavior remain unchanged.
- Tightened item-9 active Controls menu reset parity. The live in-game Controls
  route now exposes a bottom-row `Reset Keys` button alongside `Done`, matching
  the factory-backed route's recovery action. Pressing it cancels any waiting
  key capture, restores all `GameSettings.KeyBinding` entries to their defaults,
  saves `options.txt`, and immediately refreshes the visible key labels and
  conflict colors.
- Tightened item-9 creative inventory interaction parity. The active creative
  inventory screen now supports number-key hotbar assignment: hovering a
  creative catalog item and pressing `1`-`9` copies that stack into the matching
  hotbar slot, hovering a hotbar slot swaps with the pressed hotbar slot, and a
  cursor-held creative stack can be copied directly into a hotbar slot. The
  screen also honors the configured Drop binding to clear the cursor first, then
  the hovered hotbar slot, without spawning survival dropped-item entities.
- Tightened item-9 enchanting-offer glyph row polish. The live Enchanting Table
  menu now fits offer phrases to the row width before drawing so alternate-font
  glyph text no longer clips mid-word against the level-cost area. Offer
  phrases also render with a dark shadow pass, preserving the regular-font
  right-aligned level cost, enabled/disabled colors, hover row texture, offer
  costs, seed logic, and enchantment generation behavior.
- Tightened item-9 classic title splash animation. Menu labels can now opt into
  rotated centered bitmap-font drawing, and the active title screen uses that
  path for a yellow angled splash with a small pulse and short Release-style
  phrase pool. The text renderer applies the same shadow pass to rotated text,
  so the splash reads like part of the old title chrome instead of a flat menu
  label.
- Tightened item-9 title-screen visual polish. The active panorama title menu
  now includes a classic yellow splash line plus responsive lower-corner
  version/footer text, keeping the existing full-width Singleplayer,
  Multiplayer, Texture Packs rows and split Options/Quit row intact. Narrow
  logical widths collapse the footer to one centered line so title chrome does
  not collide with menu controls.
- Tightened item-9 inventory drag-splitting parity. Shared container drag
  distribution now uses one stable left-drag share computed from the original
  cursor stack and selected eligible slots, leaving odd remainders on the
  cursor instead of feeding them into later slots. Right-drag still places one
  item per eligible slot, and every survival container inherits the same rule
  through `ContainerDragDistributor`.
- Tightened item-9 inventory/drop-key settings parity. Survival container
  keyboard drop now honors the configured Drop key through the same
  `GameSettings` binding path as in-world dropping, while older screen
  constructors still fall back to default `Q`. Cursor-first and hovered mutable
  slot drop behavior is unchanged, output/result slots remain protected, and
  the existing world/multiplayer `itemsToThrow` pipeline still handles the
  actual spawned item.
- Tightened item-9 inventory keyboard-drop parity. Survival container screens
  now support the default Release-style `Q` drop flow while open: the cursor
  stack drops one item first, otherwise the hovered mutable slot drops one item
  through the existing `itemsToThrow` world/multiplayer pipeline. Inventory,
  Crafting Table, Chest, Furnace, Dispenser, Brewing Stand, and Enchanting
  Table share the new helper, with tile containers still marking dirty when
  their own slots change and recipe/result output slots remaining protected
  from duplication.
- Tightened item-9 Statistics table visual polish. The live Statistics rows now
  use structured columns instead of compressed comma sentences: General gets
  fixed Statistic/Value columns, Blocks gets Block/Mined/Craft/Used/Pick/Drop,
  and Items gets Item/Pick/Drop/Craft/Used/Break. The shared statistics list
  renderer now draws header bands, column separators, clipped names, and
  right-aligned values on narrow and wide menus without changing the saved
  counters, tab behavior, or gameplay hooks.
- Tightened item-7/item-9 enchanting-table animation polish. The world
  enchanting table renderer now uses the tile entity's existing spread,
  rotation, bob, and page-flip state to draw visible parchment pages over the
  book covers, including two animated turning pages while a nearby player keeps
  the book open. This moves the table away from a static two-half prop without
  changing enchant offer generation, XP costs, item placement, multiplayer
  synchronization, or container input behavior.
- Tightened item-9/item-10 multiplayer menu visual polish. The active saved
  server browser now uses a 320-safe classic-width list, 22-pixel rows, two
  balanced action-button rows, a centered Cancel row, and an empty-list label
  that stays inside the framed list. Direct Connect and Add/Edit Server now use
  labeled responsive text fields and matching bottom buttons, preserving saved
  server selection, ping status text, join, host-world, save, and cancel
  behavior.
- Tightened item-9 Create World and text-field visual polish. The active Create
  New World screen now uses responsive centered field sizing, classic label
  placement for world name and seed, and a stable game-mode row instead of bare
  unlabeled inputs. Shared menu text fields now draw old-style inset frames,
  top/left highlight edges, clipped visible text, and a real cursor at the
  current cursor index instead of appending an underscore at the end; text entry,
  cursor movement, Enter submission, create-world, rename, Direct Connect, and
  server-edit behavior are unchanged.
- Tightened item-9 selection-list menu visual polish. Select World and Texture
  Packs now use narrower classic centered lists, 22-pixel rows, balanced
  old-style action-button rows, and consistent bottom controls instead of the
  previous wide mixed-width strip. The shared non-statistics `MenuList` renderer
  now draws framed dark list panels, alternating row shade, hover/selected row
  bands, clipped centered labels, and a compact scrollbar, improving world,
  texture-pack, language, and other simple selection lists without changing
  activation, double-click, selection, or resource/world callbacks.
- Tightened item-9 active Controls menu pixel polish. The live key-binding
  screen now uses stable classic two-column control rows, fixed 74-pixel key
  buttons, muted clipped action labels, safer row spacing on short/tall windows,
  and a consistent bottom Done placement. Waiting-for-input, mouse-button names,
  conflict coloring, key capture, saving, and return-to-options behavior are
  unchanged.
- Tightened item-9 active Language menu parity. The live in-game Options
  language route no longer opens a one-button English-only placeholder: it now
  shows a classic dirt-backed language list, current-language feedback, the old
  translation accuracy note, selectable Release-style language rows, Select and
  Done controls, double-click activation through the menu-list path, and keeps
  unknown saved language codes visible instead of discarding them. The selected
  language still persists through the existing `GameSettings` save path.
- Tightened item-9 Achievements menu visual polish. The live achievement tree
  now loads the bundled `textures/achievement/bg.png` sheet, frames the tree
  with the old achievement-window art, insets the scrolling content viewport,
  uses stronger old-style shadowed connector lines, gives unlocked/available/
  locked/special nodes beveled source-era frames instead of flat gray boxes,
  darkens hidden locked icons, and draws achievement tooltips on the bundled
  dark tooltip panel art. Achievement unlock rules, parent gates, icons, labels,
  drag/scroll navigation, and detail text are unchanged.
- Tightened item-7/item-9 3D Anaglyph visual/settings parity. The active video
  option already reached the render path; the stereo world pass now uses a
  named 0.035-block half-eye offset, source-style red then cyan color masks
  without alpha writes, a shared eye-pass helper, finite right-vector fallback,
  and guaranteed color-mask/camera restoration. The world is drawn stereo while
  HUD, menus, inventory, chat, and debug surfaces remain a single readable
  full-color overlay afterward.
- Tightened item-7 first-person held item visual polish against the old
  `ItemRenderer.renderItemInFirstPerson` lower-right pose and reference
  screenshots. Block/cube holding remains untouched, while flat item sprites now
  sit a hair lower, farther back, and slightly more face-on: 0.57/-0.57/-0.88
  placement, 31-degree inward yaw, 0.37 scale, and a softer sprite-only swing
  arc. This keeps swords, tools, food, arrows, sticks, and material sprites from
  feeling too close to the camera or too edge-on while preserving the existing
  bow, blocking, eating, glint, and atlas paths.
- Tightened item-9 visual menu polish. Managed menu buttons and sliders now
  draw from the Release-era `textures/gui/gui.png` button strips through
  `ClassicGuiTexture` two-half stretching, preserving disabled, normal, and
  hovered atlas states instead of the previous flat gray rectangles. Sliders
  now use the classic 8-pixel handle built from the button-strip edges. Default
  control labels now use the old `GuiButton`-style colors: muted gray when
  disabled, `0xE0E0E0` normal text, and pale yellow hover text, with vertical
  centering based on the classic 8-pixel font height.
- Tightened item-9 inventory tooltip visual polish. Inventory, crafting,
  chest, furnace, dispenser, brewing, enchanting, and creative item tooltips
  now share a classic dark tooltip frame with the old purple-blue vertical
  border gradient, 1x bitmap text, first-line spacing, and screen-edge clamping
  instead of drawing an oversized flat purple box that could run off-screen.
- Tightened item-9 stack-count visual polish. Inventory containers, creative
  slots, cursor-held stacks, and the survival hotbar now render item counts
  with the Minecraft bitmap font and one-pixel shadow instead of CraftZero's
  procedural seven-segment rectangle digits. The old rectangle digit path
  remains as a fallback when the bitmap font renderer is unavailable, and the
  inventory/HUD color shader is rebound after text drawing so later hover and
  frame quads continue rendering correctly.
- Tightened item-9 survival hotbar visual polish. The textured HUD hotbar now
  derives its background, selected-slot frame, item placement, and item size
  from the source `gui.png` pixel layout: 182x22 hotbar strip, 20-pixel slot
  pitch, 24-pixel selected frame with the old -1 pixel offset, and 16x16 item
  icons at the 3-pixel slot inset. This removes the previous ad hoc selected
  slot shift and undersized 14-pixel item icons while preserving the procedural
  fallback hotbar when `gui.png` is unavailable.
- Tightened item-9 XP HUD label polish. The textured XP bar was already using
  `icons.png`; the level number now uses the old green text treatment with
  four-direction dark-green outlining and a bright lime face instead of a
  single black drop shadow. The HUD color shader is rebound after the bitmap
  text draw so subsequent HUD quads keep rendering with the expected projection.
- Tightened item-9 boss HUD visual polish. The Ender Dragon boss bar now draws
  from the Release-era `icons.png` boss-health strips at rows 74/79, keeps the
  label above the bar, and uses the existing bitmap text shadow helper instead
  of the previous procedural purple rectangles with the label below the bar.
  Tiny-window layouts now skip the bar cleanly instead of asking the rectangle
  path to draw a negative-width boss meter.
- Tightened item-9 selected-item HUD parity. The survival HUD no longer shows
  the newer selected-hotbar-item name pop-up on slot changes; Release-style HUD
  stays limited to health/armor/hunger/air/XP/hotbar/status/boss/achievement
  surfaces. Removed the isolated animation state so hotbar-only rendering no
  longer advances a non-Release overlay.
- Tightened item-9 death-screen button visual polish. The live death overlay now
  draws Respawn, Title Menu, and Hardcore Delete World controls from the
  Release-era `textures/gui/gui.png` button strips with two-half stretching and
  old hover text colors instead of the separate procedural beveled rectangles.
  The old rectangle path remains only as a missing-atlas fallback.
- Tightened item-9 sign-editor visual polish. The live sign editor now presents
  the old `Edit sign message:` title, a centered wooden sign preview with
  plank bands and selected-line cursor, and a real `Done` control drawn from the
  Release-era `gui.png` button strips. The previous flat debug-board look and
  visible `Done: Esc` shortcut hint were removed, while the click path still
  exits through the existing sign-close/broadcast flow.
- Tightened item-9 chat HUD visual polish. The in-game chat overlay now renders
  old translucent chat rows and a short bottom input strip, fades closed-chat
  messages over their visible lifetime, and no longer draws the newer visible
  command-suggestion popup. Tab completion and history still use the existing
  suggestion data internally, so command input behavior remains intact while
  the HUD surface reads closer to the Release-era chat box.
- Tightened item-9/item-10 Tab player-list visual polish. The multiplayer Tab
  overlay now keeps the same roster and latency data but presents it in a
  centered classic panel with a `Connected players:` title, subtle border,
  header divider, row shading, local-player highlight, column separators, and
  existing compact signal bars instead of the earlier bare rectangle.
- Tightened item-9 F3 debug overlay visual polish. The debug overlay no longer
  draws modern boxed black panels around the left/right diagnostics; it now
  renders compact shadowed bitmap text directly at the screen edges, with the
  right diagnostics aligned to the top-right and stacked below the left block
  on narrow screens. The diagnostic data payload is unchanged.
- Tightened item-9 enchanting-table menu visual polish. Offer clue phrases now
  use the old three-to-five-word generated range and render as clipped Standard
  Galactic text inside each option row instead of being word-trimmed early.
  The level cost number is aligned to the right side of the row with a small
  dark shadow and old green/dim-green enabled-state colors; gameplay offer
  costs and enchantment generation are unchanged.
- Tightened item-9 statistics menu visual polish. Statistics list widgets now
  use a dedicated Release-style table renderer instead of the generic menu-list
  surface: dark framed panel, alternating row shade, hover/selected row band,
  split label/value columns, right-aligned values, text truncation inside each
  column, and a compact scrollbar for long General/Blocks/Items pages. The
  statistics taxonomy, order, values, and tab behavior are unchanged.
- Tightened item-7 first-person sprite item holding polish. Block holding keeps
  its existing cube branch, while flat item/tool/material sprites now sit lower,
  farther back, and less edge-on in the lower-right hand pose. The sprite swing
  arc is also damped so swords, sticks, food, and materials no longer lunge into
  the camera or rotate to a harsh edge during ordinary holding/swinging; bow,
  blocking, eating, and glint/item atlas paths remain on the same renderer.
- Tightened item-9 achievement toast visual polish. The HUD notification now
  uses a beveled old-style toast frame with an inset icon slot and renders the
  actual achievement icon from `AchievementType.icon()` instead of placeholder
  yellow rectangles. Regular and special achievements get distinct frame accents,
  the title text is spaced beside the slot, and the existing slide/fade timing is
  preserved.
- Tightened item-9 classic menu background polish. Dirt-background menus now
  tile the real terrain dirt texture with subtle per-tile shade variation and
  Release-style darkening instead of using procedural checkerboard brown blocks.
  Options, Video Settings, Controls, world/resource-pack selection, and other
  dirt-backed menu screens keep their existing controls but read closer to the
  old full-screen menu surface.
- Tightened item-9 settings menu pixel layout polish. The active Options and
  Video Settings screens now use the old 150-pixel paired option columns with
  10-pixel column spacing instead of oversized 200-pixel columns that could
  overrun the classic 320-wide GUI layout. Button and slider labels now clip
  with an ellipsis inside their textured controls, keeping long labels such as
  Advanced OpenGL, Render Distance, and Performance from bleeding outside the
  button art.
- Tightened item-7 first-person sprite item pose parity. Tool, material,
  skinny, large, and terrain-sprite held items now share a Release-style
  lower-right item transform while compensating for CraftZero's extruded sprite
  mesh: 0.56/-0.55/-0.84 placement, 34-degree inward yaw, and 0.38 scale. Block
  items keep their existing cube pose. Sprite-item swings keep the legacy
  pre-base sine shape, but use softened -0.30/0.13/-0.12 translation and
  -14/-13/-52 rotation amplitudes so tools and materials move through a flatter
  old-game arc instead of hanging stiffly or drifting too close to the camera.
  The sprite path also applies the base yaw before swing/use rotations and
  scales afterward, matching the legacy first-person item transform order
  instead of rotating the already-swung item into a more edge-on pose. Sprite
  equip/drop settling now uses the old 0.60 vertical
  offset instead of the block-oriented 0.70 value. First-person sprite items
  also no longer apply CraftZero's extra bow/stick/arrow/bone/feather mirror
  pass, keeping held sprites in the same icon orientation used by the legacy
  item renderer. Sprite items now also skip the extra per-item walk bob that
  was layered on top of the camera/view bob, so tools and materials settle in
  the classic lower-right pose instead of drifting around the screen while
  walking. Sprite equip/drop animation now uses the legacy linear equip
  progress for its 0.60 vertical translate, while blocks and maps keep their
  existing eased switch motion. First-person bow draw now follows the old
  item-renderer action branch directly: the bow applies its base scale before
  draw-use transforms, then uses the source Z/Y/X rotations, -0.9/0.2
  translation, draw wobble, forward pull, and center-pivot Z stretch. The
  shared first-person pose helper now reports the same source-shaped bow pull
  values instead of the earlier approximate CraftZero bow pose. First-person
  sword blocking now also uses the old item-action branch after base item
  scale, with the source -0.5/0.2 translation and 30Y/-80X/60Y guard rotations
  instead of the earlier CraftZero guard approximation. Eating and drinking now
  run the old mouth pull/bite bob branch before the shared lower-right base item
  pose, so food, milk, and drinkable potions move toward the player in view
  space instead of being pulled through the already-yawed/scaled local item
  axis. Ordinary first-person sprite items now ignore CraftZero's generic
  right-click use transform when their legacy item action is none, so buckets,
  bottles, tools, and materials no longer jolt through a non-vanilla use pose;
  block items keep their existing placement animation.
- Tightened item-7 dynamic clock/compass visual parity. The scene shader now
  supports solid-color overlay primitives, and first-person, third-person, and
  dropped item renderers draw live compass/clock needles from player, camera,
  and world state while leaving the base item sprite and enchanted glint paths
  intact.
- Tightened item-7 third-person held-item grip parity. The player renderer now
  applies pose-aware hand and item adjustments for drawn bows, blocking swords,
  and active eating/drinking, so the held sprite/block follows the new arm pose
  with Release-shaped pull, guard, and consume offsets instead of using one
  static third-person grip for every action.
- Tightened item-7 third-person held-use animation parity. Player models now
  receive explicit held-use pose state from the renderer, so blocking swords
  lift into a guard pose, bow drawing raises both arms around the pulled bow,
  and eating/drinking bends the held-item arm through the same consume progress
  that drives the first-person item. The third-person held item now follows the
  posed hand transform instead of only switching item sprites while the arm kept
  ordinary walk/swing animation.
- Tightened item-7 first-person consumable animation parity. Eating and drinking
  viewmodel poses now use a Release-shaped 32-tick consume curve with the old
  high-power pull-in, late rhythmic bite bob, and stronger yaw/pitch/roll
  transform instead of the softer generic lift-to-mouth curve. Food, milk, and
  drinkable potion use therefore read closer to the old first-person item
  renderer while keeping the existing item-use timing and sound/crumb paths.
- Tightened item-5/item-10 projectile direct-hit source parity. Arrow hits now
  build one owner-aware damage source for mobs and the host player, preserving
  player credit plus connected shooter ids instead of mixing remote ids with
  anonymous point damage. Arrow and returned-fireball hits on connected remote
  players now carry an optional `sourcePlayerId` through the validated command
  damage payload, and clients restore that id into the received `DamageSource`.
  Blaze fireball direct hits on the host player now also apply the same burn
  timer already used for living entities and connected remote players.
- Tightened item-5/item-10 End crystal attack parity. Local player and hosted
  remote-player entity attacks now handle End crystals before the ordinary
  living-entity combat branch, so crystals still explode and consume tool
  durability on a valid hit but no longer run mob-style enchantment damage,
  critical/magic-critical hit particles, knockback, fire-aspect, or tamed-wolf
  assist targeting logic. Hosted remote crystal hits use remote-player damage
  metadata for the hit source while keeping the crystal explosion path itself
  on the existing anonymous explosion behavior.
- Tightened item-5/item-10 hosted XP-orb attraction parity. Experience orbs now
  choose the nearest live host or connected remote-player view inside the old
  attraction radius instead of homing only toward the host player while remote
  players could only collect nearly-static orbs. Remote XP pickup commands now
  also compare the previous and new level and play the level-up cue when the
  pickup crosses a level, matching the local pickup feedback path.
- Tightened item-5/item-10 hosted fishing-hook remote-player parity. Fishing
  bobbers now treat connected remote-player view boxes as hookable targets
  while ignoring the remote owner that cast the line, follow a hooked remote
  player through live world views, and apply the old reel-in pull vector through
  a validated server-to-client velocity command. Hosted casts preserve the
  caster id in hook state, hook snapshots include remote hooked-player ids, and
  receiving clients apply the pull as local velocity instead of faking it as
  damage.
- Tightened item-5/item-10 Ender pearl impact parity. Pearl impacts now build
  an owner-aware contact `DamageSource`, preserving local player credit and
  connected remote owner ids for zero-damage projectile contact instead of
  leaving impacted mobs with anonymous metadata. Endermen now treat Ender
  pearls as dodgeable projectiles alongside arrows and thrown snowballs/eggs,
  preserving their projectile-teleport behavior. Hosted remote-player Ender
  pearl impact callbacks now send the same 5-point fall-style owner damage as
  the local `teleportFromEnderPearl` path after teleporting the connected
  client, instead of only moving the client and skipping the old pearl damage
  consequence.
- Tightened item-5/item-10 player-owned explosion combat parity. Primed TNT now
  carries local player ownership and remote owner ids, local flint-and-steel
  ignition marks TNT as player-owned, hosted-client TNT ignition records the
  connected player id, and hosted TNT entity snapshots preserve both fields.
  The shared explosion damage path now accepts an owner-aware `DamageSource`
  and reuses its player-credit/remote-player metadata when damaging living
  entities and the host player. Player-owned TNT blasts and returned Ghast
  fireball explosions therefore feed the same recent-player damage, remote
  retaliation, loot/XP, and knockback source path as direct projectile and
  potion combat, while anonymous explosions such as beds, creepers, crystals,
  redstone-primed TNT, and environment blasts remain anonymous.
- Tightened item-5/item-10 splash-potion combat ownership parity. Splash potion
  projectiles now carry a `playerOwned` flag in addition to remote shooter ids,
  local player throws mark their spawned splash entity as player-owned, and
  hosted entity snapshots preserve that ownership bit. Instant splash potion
  harm now receives an owner-aware magic `DamageSource` instead of anonymous
  magic damage, so player-thrown Harming potions and Healing damage against
  undead mobs can count as recent player damage, carry remote attacker ids,
  drive hostile-mob retaliation through the new damage metadata path, and
  preserve player-credit loot/XP behavior. Non-instant status-effect splash
  behavior remains on the existing effect path.
- Tightened item-5/item-10 hosted hostile-retaliation parity. Living entities
  now retain the full `DamageSource` metadata for their last hit alongside the
  old source-entity pointer, so AI can distinguish direct remote-player damage
  and remote-owned projectiles from generic point damage. Shared hostile mob
  retaliation now resolves connected-player ids from melee, arrows, thrown
  items, and returned fireballs, immediately targets the matching authoritative
  remote player view, and keeps passive mobs out of that hostile-only path.
  The generic hurt-by-target revenge goal now also carries remote attacker ids
  through its revenge timer, refreshes the remote target by id, and computes
  distance from the mob rather than trusting origin-relative cached distances.
  This closes another hosted combat gap where a remote player could hit a mob
  and the mob would only fall back to nearest-player acquisition instead of
  pursuing the actual attacker.
- Tightened item-5/item-10 hosted wolf owner-awareness parity. Remote player
  world targets now carry username metadata alongside held item and pose
  state, letting tamed wolves owned by connected clients resolve their live
  owner by name. The owner-follow goal now follows and teleports toward either
  the host owner or a remote owner view, and wolf begging now looks at the
  nearest host or connected client holding a valid bone/meat item for that
  wolf's wild/tamed owner state instead of only reacting to the host player's
  hand. Hosted client melee replay now also wakes nearby wolves owned by the
  attacking connected player's username, so remote owners can direct tamed-wolf
  assist attacks through ordinary combat instead of only through host-player
  combat. Hosted mob-melee damage sent to a connected player now resolves the
  nearest living attacker at the reported source point and wakes nearby wolves
  owned by the damaged player's username, bringing remote owner-defense closer
  to the local player hurt path.
- Tightened item-5/item-10 hosted natural-spawning anchor parity. Runtime mob
  spawning now builds the Release-style eligible chunk set as a union around
  the host plus connected live players instead of only using the host player.
  Hostile spawning uses targetable local/remote players, passive and water
  creature spawning use live player views, mob caps scale from the unioned
  eligible chunk count, border chunks stay cap-counted but non-spawnable, and
  the 24-block player exclusion now checks every live spawn anchor.
- Tightened item-5/item-10 hosted remote combat identity parity. Player-owned
  combat sources now carry connected-player identity through shared damage
  metadata for remote melee, arrows, thrown items, and returned fireballs.
  Zombie Pigmen resolve the remote provoker id through the shared combat
  resolver before alerting nearby Pigmen, wild wolves keep the provoking remote
  player as their angry target while spreading that same target through pack
  alerts, and tamed wolves can now retaliate against connected clients through
  their assist goal instead of only understanding local living-entity attackers.
- Tightened item-5/item-10 hosted passive-player awareness parity. Remote
  player metadata now separates hostile targetable players from general live
  player views and carries each remote player's held item through the shared
  world target record. Breedable animals using the shared temptation goal now
  choose the nearest host or connected client holding their breeding item and
  continue following updated remote positions, while idle look-at behavior can
  pick nearby connected clients as visual attention targets without weakening
  creative/Peaceful filtering for hostile combat AI.
- Tightened item-5/item-10 hosted Enderman stare-target parity. Authoritative
  remote player targets now carry yaw, pitch, and pumpkin-helmet state, and
  the server exposes the sorted set of nearby remote targets instead of only
  the nearest one. Endermen can now detect connected clients staring at them
  through the same line-of-sight and dot-threshold rule used for the host
  player, preserve the pumpkin exemption, become angry after the old short
  stare window, keep the provoking remote target assigned through the shared
  target goal, teleport near far remote targets, and suppress close melee with
  random teleporting while remote eye contact is maintained.
- Tightened item-5/item-10 hosted cave-spider melee parity. Shared mob melee
  now fires a successful-hit callback for authoritative remote player targets,
  and Cave Spiders apply their Release-era Normal/Hard poison duration to
  connected clients after accepted melee damage instead of only poisoning the
  host/local player. The remote-player world hook now carries explicit bounded
  status-effect payloads through the existing command-effect channel and keeps
  the host's cached remote status list aligned, so later player-state snapshots
  do not erase mob-applied effects. Spider-family mid-range leap pursuit now
  also runs against authoritative remote player targets through the same
  source-shaped random leap queue used for the host player, so hosted Spider
  and Cave Spider fights no longer lose their old lunge behavior against
  connected clients.
- Tightened item-5/item-10 custom hostile remote-combat parity. Creeper fuse
  logic now keeps authoritative remote player targets through chase, line of
  sight, fuse advance, and explosion checks instead of cooling off when the
  shared targeting goal picked a client player. Slimes and Magma Cubes now
  choose the nearest local or remote player for jump-facing and contact damage,
  preserving size-based hit rules and sounds. The Ender Dragon can now pick a
  remote player as a flight target and apply head/body contact damage to the
  nearest authoritative remote player box, so hosted boss and slime-family
  fights no longer depend on the host standing near the mob.
- Tightened item-5/item-10 special hostile remote-target parity. Ghasts now
  choose between the local player and the nearest authoritative remote player
  target before charging/firing, and Blazes now run their custom close-melee
  and three-fireball burst loop against remote player targets as well as local
  players and living-entity retaliation targets. Hosted Nether fights no
  longer leave Ghasts or Blazes idle just because their nearest valid target is
  a connected client rather than the host player.
- Tightened item-5 hosted mob despawn authority. Monster and water-creature
  despawn distance now uses the nearest live local player or authoritative
  remote player target instead of only the host/local player. Hosted mobs no
  longer hard-despawn or start the old 32-block soft despawn timer merely
  because the host is far away while another connected player is still close.
- Tightened item-5 runtime natural-spawn candidate parity. Runtime natural
  ground groups now test the selected chunk Y directly instead of snapping
  up/down through a broad local search, while Ghast candidates use the
  flying-volume path inside the same runtime group loop. Ground mobs now
  require a Release-style normal full-cube spawn floor, excluding leaves,
  cactus, TNT, thin/partial blocks, and other non-normal supports from being
  treated as valid natural spawn floors, while explicit pack helpers keep
  their controlled local search behavior.
- Tightened item-4 fragile ground-cover replacement parity. Normal block
  placement now treats the same ground-cover plant family used by support,
  collision, fluid displacement, and falling-block replacement as replaceable,
  and the player placement path now targets the clicked fragile plant cell
  itself instead of the adjacent face. Placing blocks into flowers, mushrooms,
  saplings, dead bushes, tall grass, snow layers, fire, or fluids now follows
  the source-style displacement path instead of failing or placing beside the
  selected cover block.
- Corrected item-3 redstone torch strong-power classification. Lit torches
  still weak-power every non-support face for adjacent devices, dust inputs,
  and torch inversion, but they now strong-power only upward. Side-adjacent
  opaque blocks therefore no longer relay torch power as if the torch were
  directly strongly powering them, which keeps solid-block relays, dust
  decay, repeaters, pistons, and rail-adjacent mechanisms closer to the
  Release-era weak/strong power split.
- Corrected item-3 redstone torch burnout recovery timing. Torch burnout still
  uses the Release-style eight-toggle history window, but a burned-out torch
  now stays suppressed for the old long recovery delay instead of relighting
  almost immediately after the 60-tick history window expires. Rapid clocks
  therefore keep torches dark through the intended burnout cooldown while
  preserving the existing per-world burnout history isolation.
- Tightened item-2 Release entity relationship save/load parity. Release
  `.mcr` entity import/export now preserves CraftZero save ids plus
  relationship ids for projectile shooters, mob targets, fishing-hook targets,
  minecart passengers, and spider jockey riders, letting the existing native
  restore pass relink those live references after region reconstruction.
  Release `level.dat`/`players/*.dat` also now carries the player's riding
  entity id/type so mounted boat, rideable minecart, and saddled-pig state can
  survive the old player/entity split. Fishing hooks now have a custom
  Release-bridge entity payload for owner credit, wait/catchable/stuck state,
  and hooked-target relinking instead of disappearing from the Release region
  bridge.
- Tightened item-2 Release player cursor-stack save/load parity. Release
  `level.dat`/`players/*.dat` import/export now preserves CraftZero's cursor
  item stack in a custom player compound, including the same stack metadata,
  enchantments, potion data, names, and map id state handled by ordinary
  Release inventory slots. Saves made with a dragged item or active container
  cursor no longer lose that stack when passing through the Release player NBT
  bridge.
- Tightened item-2 Release projectile NBT save/load parity. Release `.mcr`
  arrows now import/export `shake`, `crit`, CraftZero knockback,
  fire-on-hit, and critical-hit tags, so reloaded stuck/enchanted/critical
  arrows keep the same combat behavior instead of falling back to partial old
  NBT state. Release snowballs/eggs and Ender pearls now retain player-owned
  credit, returned fireballs retain player-deflection credit, and Eyes of
  Ender now preserve their steering target coordinates instead of reloading
  with the current entity position as the target.
- Tightened item-2 Release region dropped-item/entity-age save parity.
  Dropped-item Release `.mcr` import/export now preserves `OnGround`, standard
  base entity tags, CraftZero item rotation/bob phase, and fractional pickup
  delay accumulator state instead of only carrying item stack, age, health, and
  whole pickup-delay ticks. The Release region bridge also now separates
  CraftZero generic `ticksExisted` into an explicit custom NBT tag so mob
  breeding `Age` no longer doubles as generic entity lifetime on import.
- Tightened item-2 player runtime save/load parity. Native player saves now
  preserve FoodStats-style regen/starvation timers, peaceful regen timing,
  drowning/air tick accumulators, spawn invincibility, hurt invulnerability,
  last damage amount, death animation time, hurt flash time, and the active
  `foodTickTimer`. Release `level.dat`/`players/*.dat` now import/export
  visible `FallDistance`, `HurtTime`, `DeathTime`, `AttackTime`, and
  `foodTickTimer` tags, plus CraftZero exact runtime timer tags for lossless
  reloads through the Release NBT bridge.
- Tightened item-2 generic entity Release save/load parity. Native entity
  snapshots now preserve each entity's ground/fall-start/falling state, and
  restored entities apply that state through the shared base-entity path after
  reconstruction. Release `.mcr` import/export now round-trips `OnGround` and
  `FallDistance` instead of exporting every entity as airborne, and the Release
  region bridge now imports/exports `EnderCrystal` entities so End spike
  crystals and dragon healing anchors survive the old region-file path.
- Tightened item-2 Release player XP save/load parity. Release
  `level.dat`/`players/*.dat` export now writes `XpLevel` and `XpP` derived
  from CraftZero's total XP instead of always zeroing those tags, and import
  reconstructs total XP from `XpLevel`/`XpP` when `XpTotal` is missing or
  stale. Old Release-compatible tools and worlds now see the same visible XP
  level/progress CraftZero derives internally.
- Tightened item-2 Release player sleep-state save/load parity. Release
  `level.dat`/`players/*.dat` export now writes the vanilla `Sleeping` and
  `SleepTimer` player tags from CraftZero's saved sleeping state and includes
  CraftZero bed foot/head plus sleep-return metadata. Release import restores
  that metadata when present, so saves made while the player is in bed can wake
  at the correct return position instead of losing sleeping context through the
  Release NBT path.
- Tightened item-2 chunk-runtime sidecar save parity. Incremental
  `saveModifiedChunkData` writes now merge fresh chunk-local runtime payloads
  into the cached dimension runtime and rewrite that dimension's `runtime.json`
  sidecar, instead of only updating the native chunk bin and Release `.mcr`
  chunk. Chunk unload saves now keep native sidecars aligned with fresh
  dropped-item, entity, tile-entity, moving-piston, scheduled-tick, clock, and
  weather metadata.
- Tightened item-2 moving-piston Release save/load parity. Release `.mcr`
  chunk import/export now bridges active moving pistons through old `Piston`
  tile-entity NBT, including carried block id/data, facing, progress, and
  extending/retracting direction. Native moving-piston runtime no longer lives
  only in CraftZero sidecars when writing or importing Release-style region
  chunks.
- Tightened item-2 runtime-only chunk save parity. Save snapshots now include
  loaded chunks that contain dropped items, entities, tile entities, moving
  pistons, or scheduled block ticks even when the block array itself was not
  modified. This lets Release `.mcr` export refresh entity, tile-entity, and
  delayed block-tick NBT for otherwise unchanged chunks instead of only
  updating those legacy chunk payloads after a block edit.
- Tightened item-2 Release region runtime save/load parity. Exported `.mcr`
  chunks now stamp `LastUpdate` from the saved runtime world clock instead of
  always writing zero, imported region chunks fold that timestamp back into the
  runtime clock metadata, and scheduled-tick-only region runtime data is no
  longer discarded as empty. Release-region saves now preserve delayed block
  updates and chunk update timing more faithfully when no entities or tile
  entities happen to be present.
- Tightened item-2 dimension runtime save/load parity. Dimension runtime
  sidecars now preserve world clock metadata, derived day/moon phase, weather
  state, and rain/thunder timers alongside entities, tile entities, moving
  pistons, and scheduled block ticks. Dimension transfers now pass the active
  `DayCycleManager` through runtime restore and refresh the active weather
  state after restore, so returning to saved Nether/End/Overworld runtime
  snapshots no longer depends only on the latest global level header.
- Tightened item-5/item-10 hosted mob-projectile shooter parity. Hosted
  snapshots now include an authoritative `shooterEntityId` for synced
  entity-fired arrows, Ghast/Blaze fireballs, thrown items, and splash
  potions, and remote clients restore or clear those shooter references as
  ordinary entity updates arrive. Mob projectiles now keep the same
  save/load-aligned shooter collision grace and combat context across
  multiplayer instead of becoming anonymous client-side projectiles.
- Tightened item-5/item-10 hosted fishing-hook attachment parity. Hosted hook
  snapshots now carry the network id of a hooked synced entity, remote hooks
  restore or clear that attachment as authoritative updates arrive, and stale
  client hook state no longer keeps following an entity after the host reports
  no hooked target. The hosted entity allow-list also now includes
  `EndCrystalEntity`, so the earlier End crystal type/create support is
  actually broadcast through ordinary entity snapshots instead of only being
  understood if such a packet appeared.
- Tightened item-5/item-10 hosted Ender pearl owner parity. Client-authored
  Ender pearls now carry their remote owner id on the authoritative host entity,
  hosted pearl raycasts check remote player hit boxes with the same owner grace
  exclusion as local player pearls, and entity snapshots replay the owner id to
  late clients. Remote pearl impacts can now resolve against other connected
  players while still teleporting the throwing client through the existing
  hosted callback instead of treating the pearl as ownerless host physics.
- Tightened item-5/item-10 hosted projectile owner-credit parity. Host
  snapshots now advertise local player-owned arrows and thrown snowballs/eggs
  as `host` projectiles when no remote client shooter id is present, while
  preserving `player-N` for client-authored shots. Remote arrow and thrown-item
  fireball deflections now transfer their shooter id into the fireball's
  remote deflector metadata, so returned-fireball collision grace and credit
  remain attached to the actual player instead of falling back to an anonymous
  or host-owned deflection in later snapshots.
- Tightened item-5/item-10 hosted projectile runtime snapshot parity. Host
  entity updates now carry bounded `age` for every synced entity plus
  remote-shooter/remote-deflector player ids for arrows, thrown snowballs/eggs,
  splash potions, and returned fireballs. Remote clients now apply those fields
  with protocol-player-id sanitization, so late joins and ordinary entity
  updates preserve projectile lifetime/despawn timing, collision grace, and
  returned-fireball credit metadata instead of recreating those entities as
  fresh anonymous projectiles.
- Tightened item-5/item-10 End crystal entity coverage. Hosted multiplayer now
  advertises End crystals with a stable `end_crystal` entity type, creates
  remote `EndCrystalEntity` instances from host snapshots, and accepts old
  `EndCrystalEntity`/`END_CRYSTAL` aliases from existing packets. Remote
  clients can now materialize save-supported End crystals for End spike/dragon
  healing and destruction flows instead of silently dropping those updates.
- Tightened item-5/item-10 non-living entity snapshot parity. Hosted entity
  updates now carry save-aligned runtime state for arrows, XP orbs, boats, and
  all minecart kinds: arrow knockback/fire-on-hit/critical/stuck-block data,
  orb pickup delay and health, and vehicle hit-wobble amplitude/direction.
  Remote clients now keep projectile pickups, burning arrow hits, stuck arrow
  visuals, XP pickup timing, and boat/minecart damage wobble aligned with the
  authoritative host instead of only preserving that state through save/load.
- Tightened item-5/item-10 special-mob snapshot parity. Hosted entity updates
  now carry save-aligned runtime state for creepers, Endermen, slimes and
  magma cubes, ghasts, blazes, squids, skeletons, snow golems, wolves,
  chickens, zombie pigmen, spiders, villagers, and the Ender Dragon. Remote
  clients now preserve creeper fuse/powered visuals, Enderman carried blocks
  and anger/attention, slime size/jump delay, ghast charge/flight targets,
  blaze burst state, squid swim animation, ranged attack cadence, wolf shake
  state, villager profession, zombie pigman anger, spider provocation, and
  dragon flight/death state through ordinary host broadcasts.
- Tightened item-5/item-10 living-entity runtime network parity. Hosted entity
  snapshots now carry generic living `fireTicks` plus active status-effect
  payloads, hurt/invulnerability damage timers, recent player-credit timers,
  and death-animation phase, and remote clients apply those values through
  shared bounded network parsers. Burning mobs, poisoned/regenerating targets,
  freshly hurt mobs, and dying living entities now keep their visible/runtime
  state across ordinary host broadcasts instead of relying only on local
  save/load persistence or one-off combat replay messages.
- Tightened item-5/item-10 ageable-mob network parity. Hosted entity snapshots
  now include each mob's `growingAge` and `loveTicks`, and clients apply those
  values through the same Release-range clamps used by save validation. Remote
  baby scale, adult breeding cooldowns, and love-mode timers now stay aligned
  across late joins and ordinary host broadcasts instead of existing only in
  local save/load state.
- Tightened item-7/item-9 first-person held-map parity. Holding a map now uses
  a 3D viewmodel path with a live 146x146 parchment texture built from
  persistent `MapItemData` colors, off-map-aware 16-direction marker drawing,
  a cached map mesh, and a two-arm grip pose instead of relying only on the HUD
  overlay or the generic flat item renderer.
- Tightened item-8 cave ambience parity. Dark, loaded Overworld air pockets now
  occasionally queue `ambient.cave.cave` with Release-style volume/pitch and a
  long cooldown, the resource-pack resolver expands the old `cave1`-`cave13`
  pool, and the procedural sound bank covers the cue when no old OGG media is
  installed.
- Tightened item-9 debug-surface parity. `F3` now toggles an in-game bitmap-font
  debug overlay with FPS/display size, dimension/generator/seed, game mode,
  player/world/chunk coordinates, facing/yaw/pitch, biome, sky/block light,
  targeted block, time/weather/moon phase, loaded chunks, entity/item/particle
  counts, and Java heap usage without forcing chunk generation.
- Tightened item-9 statistics taxonomy/order parity. The statistics screen now
  presents general counters in a source-style progression: session/time first,
  then travel distances, action/item totals, fishing, combat/death counters,
  and CraftZero's world/session bookkeeping at the end, while keeping the
  existing block and item tabs backed by the full persisted per-type counters.
- Tightened item-5/item-10 hosted projectile ownership parity. Server-replayed
  client bow shots, snowballs/eggs, splash potions, and returned fireballs now
  stamp the remote actor id onto the spawned projectile path and the hosted
  remote-player raycast skips that actor during the normal launch/deflection
  grace window. Remote clients no longer risk immediately hitting themselves
  with their own authoritative projectile replay while nearby players remain
  valid projectile targets.
- Tightened item-2/item-5 projectile combat save/load parity. Native entity
  snapshots now save and validate mob-owned projectile shooter references for
  arrows, Ghast/Blaze fireballs, snowballs/eggs, and splash potions, then
  restore those links after all entities are reconstructed. Reloaded mid-flight
  projectiles now keep collision grace, mob revenge targeting, skeleton record
  credit, Snow Golem/Blaze projectile ownership, and returned-fireball combat
  paths aligned with live runtime behavior instead of becoming anonymous shots.
- Tightened item-2/item-3 vehicle mechanism runtime persistence. Minecart and
  boat hit-wobble amplitude/direction now round-trip through native entity
  snapshots with validation and restore hooks, and the Release `.mcr` bridge
  imports/exports the old minecart rolling fields plus boat hit-direction
  fields alongside existing damage state. Recently struck carts and boats no
  longer reload with their damage value intact but their visible hit mechanism
  reset.
- Tightened item-2 mob runtime save/load parity. Native entity snapshots now
  preserve living hurt animation time, invulnerability-frame damage comparison,
  recent player-credit timers, and looting credit across save/load and
  dimension-runtime cache restore. Release `.mcr` mob import/export now also
  bridges the old `HurtTime`, `AttackTime`, and dragon `DeathTime` NBT fields,
  so damaged mobs and in-progress dragon deaths no longer reload as if those
  transient combat timers vanished.
- Tightened item-5/item-10 hosted sprint-hit knockback replay parity. Hosted
  remote attacks now track the Release-era W-tap sprint knockback latch per
  remote player, reset it when forwarded movement input is released, add the
  sprint knockback bonus to accepted PvP attacks and surviving living-entity
  hits, and clear the remote sprint flag after consumption so repeated hosted
  attacks do not keep receiving the sprint bonus without a forward-key reset.
- Tightened item-5/item-10 hosted PvP Fire Aspect replay parity. Fire Aspect
  from hosted player-vs-player attacks now ignites the target only after the
  damage hit is actually accepted and the target survives, both when the
  integrated host is hit and when a remote client receives a hosted damage
  command. Environmental fire/lava commands keep their contact-fire behavior.
- Tightened item-5/item-10 hosted player-hit durability replay parity. Accepted
  hosted player-vs-player attacks now spend selected-item durability only when
  the authoritative selected stack is an actual tool, matching local combat's
  melee aftermath and preventing damageable non-tools such as bows from wearing
  down just because they were selected during a hosted melee hit.
- Tightened item-5/item-10 hosted status-effect combat replay parity. Hosted
  living-entity and player-vs-player attack damage now includes the remote
  actor's validated Strength and Weakness effects using the same Release-era
  `+3 << amplifier` and `-2 << amplifier` damage bonus rules as local player
  combat, instead of replaying potion-affected attacks as raw weapon/enchantment
  hits.
- Tightened item-5/item-10 hosted enchanted mob-hit replay parity. Accepted
  hosted living-entity attacks now apply held-item Knockback enchantment levels
  to the same horizontal knockback vector used by local combat, and Fire Aspect
  now ignites surviving targets for the Release-era 80 ticks per level instead
  of dropping those enchantment side effects on the host path.
- Tightened item-5/item-10 hosted creative entity-attack replay parity. Remote
  creative-mode attacks now pass the actor's authoritative game mode into
  minecart, boat, and painting break logic, so creative clients remove those
  entities without survival drops and without hosted durability loss instead of
  replaying every vehicle/decorative hit as a survival attack. Hosted attack
  durability now also matches the local split: only successful living/vehicle
  hits can wear actual tools, while painting breaks and fireball deflections no
  longer spend selected-item durability on the host.
- Tightened item-5/item-10 hosted pig interaction replay parity. Host-side
  entity-use replay now tries valid non-wolf breeding feeds before the saddled
  pig mount branch, matching the local player decision tree, so right-clicking
  a saddled pig with wheat enters love mode and consumes the food instead of
  mounting the pig only on the hosted authority path.
- Tightened item-9/item-10 hosted chest-minecart open replay parity. Remote
  right-clicks on storage minecarts are now accepted as host-authoritative
  container-open intents and immediately rebroadcast the server cart entity
  snapshot, including the 27-slot inventory, so clients start chest-minecart
  screens from authoritative contents before sending container edits.
- Tightened item-9/item-10 hosted armor-equip replay parity. Right-click armor
  equip now emits a dedicated item-use action, hosted replay validates that the
  selected hotbar stack is a real armor piece, swaps one copied armor item into
  the authoritative armor slot, returns the previous armor stack to the selected
  hotbar slot, updates held-stack state, and broadcasts the changed inventory
  slots instead of relying on client-only inventory aftermath.
- Tightened item-9/item-10 hosted filled-map use parity. Local map use now emits
  a dedicated multiplayer item-use action, the protocol accepts only the normal
  compact action payload for that use, and hosted replay updates the
  authoritative selected map stack through `MapItemData.useMap` before
  broadcasting the changed inventory slot. Map id/damage, center, dimension,
  player marker, and newly explored color metadata therefore come from the host
  instead of only the client's local held-item aftermath.
- Tightened item-5/item-10 hosted wolf ownership replay parity. Hosted
  entity-use replay now matches local wolf interaction ownership checks for
  meat feeding and sitting toggles, so a client can no longer heal or command
  another player's tamed wolf by replaying a valid-looking use action. Bone
  taming still consumes bones and assigns the hosted actor name on success.
- Tightened item-9/item-10 hosted consumable replay parity. Local food, milk,
  and drinkable-potion completion now emits dedicated item-use actions; hosted
  replay validates the authoritative selected stack, applies Release-style
  hunger/saturation, food side effects, milk effect clearing, instant and timed
  potion effects, container replacements, eat/drink sounds, and survival
  inventory mutations instead of trusting client-only aftermath state.
- Tightened item-4/item-10 note-block and jukebox replay parity. Jukebox
  right-click ejection is restored through the world toggle path, gated by the
  inserted-record block metadata before the tile contents are ejected. Local
  note-block punch playback, note-block right-click tuning, jukebox record
  insertion, and jukebox ejection now emit dedicated multiplayer item-use
  actions with block coordinates; hosted replay validates reach/protection,
  mutates the authoritative note/jukebox tile, plays the correct world sounds,
  consumes inserted records only after acceptance, and syncs the dirty tile
  state back out instead of relying on client-only metadata edits.
- Tightened item-10 hosted painting placement parity. Local painting placement
  now records a dedicated item-use action with support coordinates and the
  clicked horizontal face, the protocol validates that exact payload and reach,
  and hosted replay runs the world's existing Release-style painting fitting
  selection before consuming the painting stack and animating remote use.
- Tightened item-5/item-10 hosted cow and mooshroom interaction parity. Hosted
  client entity-use replay now mirrors the local specific interaction order for
  cow-family mobs: buckets milk adult cows and mooshrooms into milk buckets,
  bowls fill from adult mooshrooms into mushroom stew, and shears convert adult
  mooshrooms into cows with health/motion transfer, the large conversion
  particle, five red-mushroom drops, and survival shears durability. Baby
  cow-family mobs still reject those adult-only container/tool interactions.
- Tightened item-4/item-10 hosted cake interaction parity. The local cake food
  values are now shared with multiplayer replay, and accepted hosted cake bites
  apply Release-style hunger/saturation clamping to the remote actor state
  while emitting the same eat/burp world sounds as local completion. Final-slice
  hosted cake removal also requires the expected air metadata, so client block
  edits cannot consume cake without matching the source-shaped bite transition.
- Tightened item-4/item-9 brewing stand slot parity. The brewing stand UI now
  treats the three bottle cells as potion-item slots only, so empty glass
  bottles no longer shift-click or place directly into the stand before being
  filled into water bottles. The brewing recipe registry now owns that slot
  predicate, brewing-stand bottle metadata only marks actual potion stacks as
  occupied, and multiplayer host container replay validates submitted tile
  inventories against the same schema before mutating authoritative tiles,
  rejecting non-fuel furnace fuel slots, increased/spoofed furnace output slots,
  non-potion brewing bottle slots, and invalid brewing ingredients while keeping
  chest, dispenser, and chest-minecart bulk storage behavior intact.
- Tightened item-2/item-4 Release furnace recipe parity. The smelting registry
  now limits ore cooking to iron and gold, removing the non-vanilla furnace
  paths that turned diamond, coal, redstone, and lapis ores directly into their
  resource drops. Those resource ores stay mining/drop-table outputs instead of
  becoming alternate furnace inputs.
- Tightened item-10 dimension-transfer inventory authority. ProtocolServer now
  treats inventories as retained player state during dimension metadata changes:
  player/block/entity snapshots reset, per-player slot snapshots are rebased to
  the active dimension, and joined players reseed from their retained selected
  hotbar slot/held stack so initial Nether/Overworld snapshots do not briefly
  lose held-item state or server inventory authority.
- Tightened item-9 Statistics feature coverage. Persistent player statistics now
  track the source-style `Times Played` session counter plus worlds loaded,
  multiplayer joins, and worlds saved in addition to the existing play-time,
  quit, travel, combat, item, and block counters. The new counters restore from
  saves, validate as non-negative player statistics, write through native save
  snapshots, update from local load, multiplayer client world creation, and
  sync/async save hooks, and appear on both Statistics screen implementations
  through the shared General row builder.
- Tightened item-8 audio asset resolution parity. Release logical sound ids now
  search broader old-pack and modern pack layouts, including root record/music
  paths, `streaming/`, `records/`, `music/records/`, legacy `sound/`,
  `newsound/`, `sound3/`, and `assets/minecraft/sounds/` variants before
  falling back. Classic hurt aliases such as `random.classic_hurt`,
  `random.old_hurt`, `damage.hurt`, `damage.hurtflesh`, and `damage.hit` now
  resolve against the same old damage pools and receive procedural fallback
  audio when media is absent.
- Tightened item-7 render-distance visual parity. The active
  Far/Normal/Short/Tiny preset now updates the camera far clip alongside the
  matching distance fog, and shared render culling drives particles, mobs,
  dropped items, projectiles, falling blocks, moving pistons, lightning, chests,
  enchanting books, spawner previews, and in-world sign text from that camera
  range instead of each renderer enforcing a stale fixed cap. Far scenes keep
  their distant visual effects visible, while Short/Tiny scenes trim the same
  systems with the terrain fog boundary.
- Tightened item-6 explosion exposure and finite blast math parity. Explosion
  block-density sampling now uses the Release-era target-size-dependent lattice
  instead of a fixed 3x3x3 probe, so mobs, players, boats, minecarts, XP orbs,
  dropped items, and hosted remote players receive source-shaped blast
  attenuation and knockback from their actual hit-box dimensions. Hosted
  remote-player explosion admission now also rejects non-finite
  coordinates/power and clamps exposure math before damage or knockback is
  emitted.
- Tightened item-5 mob AI, navigation, and combat pursuit parity. Pathfinding now
  evaluates full entity clearance against loaded collision boxes, rejects
  dangerous/open-below/fence nodes as movement targets, avoids double-counting
  terrain penalties, and falls back to the closest reachable partial path instead
  of abandoning movement when the exact target cell is blocked. Shared AI
  movement requests now guard non-finite target coordinates and apply the same
  cliff-safe direction selection to melee pursuit, skeleton/ranged strafing and
  approach/retreat, creeper fuse pursuit, tamed wolf assist attacks, wild wolf
  sheep hunting, and passive threat avoidance.
- Tightened item-4 tile-entity lifecycle parity. Loaded stale tile entities are
  pruned before tile ticking, tile ticks skip entries whose backing block no
  longer matches or whose chunk is unloaded, public tile iteration now uses a
  deterministic position-sorted snapshot for render/save callers, and chunk
  unload removes tile entities after the modified-chunk runtime snapshot is
  queued. Furnace, brewing stand, spawner, sign, note, jukebox, dispenser,
  enchanting table, and chest tiles therefore stay tied to their backing
  block/chunk lifecycle instead of lingering as ghost runtime state.
- Tightened item-3 transient redstone/mechanism runtime cleanup. Sticky-piston
  short-pulse memory is now discarded when a sticky piston block is removed or
  replaced, so a later sticky piston at the same coordinate cannot inherit stale
  pull-suppression state. Per-world redstone runtime caches for torch burnout,
  powered openables, and sticky pistons are also explicitly dropped during world
  cleanup instead of waiting for weak-reference cleanup. Torch burnout queries
  now read existing history without creating empty world maps, and prune expired
  empty histories as their 60-tick source window rolls off.
- Tightened item-2 old-NBT item metadata parity for Release save bridges.
  `level.dat` player inventories and `.mcr` region stack payloads now preserve
  unknown legacy item `tag` entries as typed NBT metadata during import and
  restore them as real named NBT tags during Release sidecar export. Nested
  `display` extras such as lore/color now round-trip beside custom names, and
  older scalar `nbt.*` CraftZero metadata is still emitted as legacy string
  tags when no typed copy exists.
- Tightened item-4 loaded-safe block/fluid interaction parity. Bucket source
  pickup, source placement, lava/water hardening, lily-pad-on-water placement,
  cauldron bucket/bottle edits, and scheduled fluid spreading now use
  loaded-only block and metadata probes instead of the chunk-generating block
  accessor. Unloaded neighboring cells behave as blocked/unavailable for fluid
  flow, while loaded fluid edits still run the normal block-change side effects
  that wake adjacent fluids, support checks, redstone, and lava mixing.
- Tightened item-3 rail/redstone scheduling parity. Rail shape changes now wake
  their reciprocal connected rail graph immediately, and block-change
  redstone scheduling uses that graph in addition to the local rail-neighbor
  sweep. Powered rails, detector rails, ordinary switch rails, and sloped
  rail chains therefore refresh through actual Release-style rail connections
  instead of depending only on a fixed block-radius wakeup pattern. Detector
  rail occupancy logic now also rechecks that support/shape validation did not
  already pop the rail, preventing unsupported detector rails from being
  recreated by a same-tick cart occupancy update.
- Tightened item-2 Release save ownership and `level.dat` metadata parity.
  Release sidecar writes now claim a stable per-session `session.lock`, verify
  it before subsequent full-save and chunk-only `.mcr` writes, and fail instead
  of silently writing after another save session takes ownership. Release
  `level.dat` now writes `generatorVersion` and a real save-directory
  `SizeOnDisk` value after player/map sidecars are refreshed, instead of the
  previous zero placeholder.
- Tightened item-2 Release map-id save/load parity. Release map imports now read
  `data/idcounts.dat` even when the counter is ahead of the actual `map_*.dat`
  files, native `level.json` snapshots preserve the next filled-map id, world
  restore reserves the live allocator from that saved value, and Release sidecar
  writes keep `idcounts.dat` at the greater of saved allocator state and emitted
  map files so new maps do not reuse old vanilla ids after load.
- Tightened item-2 save/load runtime parity for chunk-only flushes. Modified
  chunks saved during unload now capture a chunk-scoped runtime snapshot before
  the async save worker runs, and pass dropped items, tile entities, entities,
  moving pistons, and scheduled block ticks into the mirrored Release `.mcr`
  chunk payload. Full save snapshots now also include live pending/generated
  entities, so just-spawned projectiles, mobs, vehicles, and other queued
  entities are not skipped if an autosave or quit-save lands before the next
  entity-list drain.
- Tightened item-10 multiplayer server outbound packet hygiene. Protocol
  broadcasts now fail closed for unknown message classes, validate server-side
  `client_action` relays against the same known action set and bounded legacy
  data contract clients accept, allow embedded block payloads only for
  `block_update`, validate disconnect payloads, cap player/world snapshot list
  sizes, and require broadcast chat/block source ids to stay inside the
  optional network-id rules before fan-out. Server-originated `client_action`
  broadcasts now use the same concrete command-effect and sleep-complete
  schemas as the receiving client, and empty `block_update` actions without an
  embedded block payload are rejected before fan-out instead of relying on each
  client to drop them.
- Added item-10 multiplayer targeted server sends. Host-originated
  `client_action` replies for commands, private messages, combat/status
  effects, respawn, enchanting, crafting, and ender-pearl reconciliation now go
  through a single-player protocol send path instead of being broadcast to all
  connected clients and filtered locally by non-targets. The targeted path
  still applies current-dimension tagging, server-side packet validation, and
  send-failure cleanup before returning success to gameplay code.
- Tightened item-10 multiplayer disconnect privacy. Kicked or access-evicted
  clients still receive the concrete server reason directly, but the subsequent
  leave fan-out sent to observers now uses a neutral disconnect payload before
  refreshing the player list. Admin kick/ban reasons and internal cleanup
  causes therefore no longer leak through ordinary remote-player removal
  packets.
- Tightened item-10 host-inclusive multiplayer roster parity. Protocol
  player-list snapshots and facade join/leave roster broadcasts now merge
  tracked world player states with joined socket clients, so the integrated
  host appears in remote Tab/player-list rosters instead of only as an in-world
  replicated player. Joined socket entries still override tracked fallback
  rows with their measured latency, and legacy server-list pings now count the
  same roster snapshot for online players so browser status and in-game roster
  agree. Full-server admission now checks that same host-inclusive listed
  player count instead of only remote socket clients, so an integrated host
  consumes a max-player slot just like the roster and status text advertise.
  Duplicate-name admission also checks tracked world player states, preventing
  a remote join from taking the integrated host's displayed player name.
- Tightened item-10 hosted server identity parity. Starting an integrated
  multiplayer host now passes the current world's display name into the
  protocol server, so the handshake server name and legacy `0xFE` server-list
  MOTD advertise the hosted world instead of the hardcoded `CraftZero`
  fallback. Blank or missing world names still fall back to `CraftZero`.
- Added item-10 Release-style server administration sidecars. Save/load now
  reads present `ops.txt`, `banned-players.txt`, `banned-ips.txt`, and
  `white-list.txt` files from the world directory, normalizes them into the
  live operator/ban/whitelist state used by multiplayer admission, and writes
  those same sorted text sidecars on save alongside the structured CraftZero
  metadata. Existing `level.json` administration fields remain supported when
  the sidecar files are absent.
- Added item-10 Release-style `server.properties` integration. Save/load now
  reads a world-local properties subset for `motd`, `max-players`, and
  `white-list`, feeds MOTD/max-player values into the integrated host's
  handshake, status ping, player cap, and world metadata, and writes a
  Release-shaped properties file on save alongside the admin sidecars. The
  structured CraftZero metadata remains the fallback when the properties file
  is absent.
- Extended item-10 hosted server property parity beyond decorative save data.
  World-local `server.properties` now also reads and preserves `server-port`,
  `gamemode`, `difficulty`, `hardcore`, `pvp`, `spawn-animals`,
  `spawn-monsters`, `allow-nether`, `online-mode`, `allow-flight`,
  `spawn-protection`, and `view-distance`. The integrated host binds the
  configured port, PVP-disabled servers reject player-vs-player damage paths,
  animal/monster spawn flags gate natural mob spawning, `allow-nether=false`
  blocks Nether portal transfer, and the PVP/spawn/Nether flags are sent
  through the multiplayer world-state packet so clients mirror the hosted
  session rules.
- Added item-10 Release-style spawn-protection enforcement. The integrated
  host now treats the world spawn square from `server.properties`
  `spawn-protection` as operator-only for remote block authority: raw
  block edits, hosted special block replays, sign edits, protected container
  edits, enchanting/crafting table use, boat placement, and minecart placement
  are rejected for non-ops near spawn while usernames listed in `ops.txt` keep
  edit rights. Rejected protected block/tile edits rebroadcast authoritative
  state so clients snap back to the host world.
- Added item-10 `allow-flight` session enforcement. The hosted world-state
  packet now carries the `allow-flight` server property, clients cache that
  rule with the rest of the world metadata, and the protocol server tracks
  each joined client's pose stream. When `allow-flight=false`, non-creative
  clients that keep reporting a hovering or rising airborne pose past the
  Release-style grace window are disconnected with "Flying is not enabled on
  this server"; grounded, falling, dead, creative, and explicitly allowed
  flight states reset the guard.
- Added item-10 `view-distance` session propagation. The hosted world-state
  packet now carries the `view-distance` server property, clients cache that
  cap with the rest of the world metadata, and connected multiplayer worlds
  clamp chunk loading plus normal-distance fog to the lower of the client's
  local video setting and the hosted server cap while singleplayer and the
  integrated host view still follow local settings.
- Added item-10 `online-mode` admission behavior. The integrated host now feeds
  the world-local `server.properties` `online-mode` flag into the same
  normalized admission gate used for bans, IP bans, and whitelist checks. With
  `online-mode=true`, unauthenticated CraftZero protocol joins are rejected
  with an explicit authentication-required disconnect reason instead of being
  admitted under offline-mode identity rules; `online-mode=false` keeps the
  existing offline/LAN join flow.
- Added item-10 `server-ip` bind parity. Save/load now preserves the
  world-local `server.properties` bind address in both `level.json` metadata
  and the Release-style sidecar. Starting an integrated host passes the
  configured address into the protocol server socket bind, while a blank
  `server-ip` keeps the normal all-interfaces bind and existing loopback
  constructors remain compatible.
- Added item-10 `spawn-npcs` server-property parity. Save/load now preserves
  `spawn-npcs` in world metadata and the Release-style sidecar, hosted
  world-state packets carry the rule to clients, and all newly constructed
  worlds apply it before chunk generation. Village structure generation now
  skips generated villager staging when `spawn-npcs=false`, while existing
  saved villagers still load normally instead of being deleted by the setting.
- Added item-10 `max-build-height` server-property parity. Save/load now
  preserves the Release-style height cap in world metadata and the sidecar,
  hosted world-state packets carry the rule to clients, and protocol validation
  rejects raw or special client block edits at or above the configured cap.
  Hosted replay applies the same cap before spawn-protection checks for block
  placements, signs, containers, enchanting/crafting table use, boat placement,
  and minecart placement; missing or blank settings keep the existing 128-block
  world height.
- Added item-10 `level-seed` and `generate-structures` server-property parity.
  Save/load now preserves the seed text and map-feature switch in `level.json`,
  Release `level.dat` `MapFeatures`, and the world-local sidecar. Worlds with
  only `server.properties` can bootstrap their first save from `level-seed`
  using the same numeric/hash seed parsing as the world creation screen, while
  `generate-structures=false` suppresses villages, mineshafts, strongholds,
  and Nether fortresses plus their lookup/side-effect paths. Hosted
  world-state metadata now carries the flag so multiplayer clients generate
  matching terrain across joins and dimension changes.
- Added item-10 `enable-query` and `query.port` server-property parity.
  Save/load now preserves the UDP query toggle and port in level metadata and
  the world-local sidecar, the integrated host applies those settings before
  startup, and the protocol server owns a Release-era GameSpy-style UDP query
  responder. Enabled hosts now answer challenge handshakes plus basic/full
  status requests with the current MOTD, map/dimension, online/max player
  counts, host port/IP, and player list, while disabled query mode leaves no
  UDP listener behind after host shutdown or reconfiguration.
- Tightened item-10 live multiplayer access-control enforcement. The
  multiplayer server facade now owns the shared connected-player eviction pass
  for current ban, IP-ban, and whitelist state, using the same normalized
  username/address rules as join admission. Host admin commands now delegate
  their post-ban and whitelist kick counts through that facade path instead of
  carrying a second copy of the access-control predicate in gameplay code.
- Tightened item-10 multiplayer client receive/session hygiene. Protocol
  clients now reset stale hello/world/disconnect queues when a new socket
  session starts, validate every inbound server packet before it reaches the
  high-level multiplayer facade, reject raw client-only packet types and
  unknown server action names with a bounded `Bad packet` disconnect, keep
  server-approved action relays inside known action/data limits, and deliver
  remote disconnect messages to gameplay before clearing the client id and
  cached world metadata. Server-originated `client_action` payloads now also
  validate the concrete command-effect and sleep-completion schemas at the
  socket boundary: targeted private messages, give, teleport, kill, clear,
  spawnpoint, gamemode, XP, damage, potion-effect, and all-player sleep-complete
  packets have bounded keys and finite/ranged numeric fields before gameplay
  handlers can consume them.
- Tightened item-10 remaining client action-family schemas. Container updates
  now accept only tile/entity location fields plus validated `tile.inventory.*`
  slot payloads, enchanting only accepts table/offer fields plus `table.item.*`,
  crafting only accepts 2x2/3x3 grid, cursor, quick-move, and table-position
  fields for the matching grid size, inventory sync validates `stack.*` extras
  with the same item-stack schema, and sign updates reject non-sign-line side
  channels before the host replay layer sees them.
- Tightened item-10 client action payload side channels. Entity attack/use and
  player attack requests now accept only their target id fields, and `item_use`
  requests now validate the exact key set for each subtype: projectile/boat/
  fishing/drop intent fields, minecart placement block coordinates, stack drop
  payloads, death-drop payloads, and death-XP amount. Extra action data can no
  longer ride beside a valid target or use subtype and reach the host replay
  layer as an unreviewed side channel.
- Tightened item-10 client-authored state packet closure. The high-level
  multiplayer client facade now rewrites legacy block and inventory sends into
  explicit validated client actions, and drops client-originated entity,
  world-event, player-state, world-state, player-list, and hello state packets.
  The protocol server no longer accepts direct client `BlockUpdate` state
  packets, leaving the `block_update` client action as the only block-edit
  request path. The host facade also ignores raw block/entity/inventory state
  packets if they ever reach its listener, keeping those snapshots host-owned.
- Tightened item-10 client player-state metadata admission. Client movement
  packets now validate their attached metadata as the bounded player-state
  contract the runtime actually consumes instead of accepting any generic
  stack-data-shaped map and filtering later. The protocol now range-checks
  `stats.*`, `progression.*`, `status.*`, `respawn.*`, `bedSpawn.*`,
  `vehicle.*`, and `remote.*` fields at the socket boundary, while host-owned
  player snapshots can still carry derived armor/inventory visuals through the
  normal server state path.
- Tightened item-10 mixed client-action/block-edit authority. Client actions
  with embedded block payloads now have to be the dedicated `block_update`
  action, carry no side-channel action data, and still pass the existing
  block-edit reach gate. The host facade applies the same guard before relaying
  block edits, so a client can no longer attach a reachable block payload to an
  `item_use`, combat, container, crafting, enchanting, or respawn action to skip
  that action's stricter validator while still reaching the host replay path.
- Tightened item-10 client inventory sync authority. Multiplayer clients now
  send inventory deltas as the explicit `inventory_sync` client action instead
  of raw `InventoryUpdate` state packets, the protocol server rejects direct
  client-authored inventory state, validates slot/item/count/damage plus bounded
  `stack.*` metadata, and the host facade stamps the admitted player id before
  rebroadcasting/snapshotting the update. Host-owned inventory broadcasts and
  initial snapshot replay still use the normal state packet path, but clients
  can no longer inject arbitrary inventory packets beside the validated action
  channel.
- Tightened item-10 protocol-server dimension-change snapshot hygiene. The
  lower-level `ProtocolServer.configureWorldMetadata` now detects dimension
  changes itself, clears cached player/block/entity world snapshots at that
  boundary, rebases retained inventory slot snapshots to the active dimension,
  and re-seeds joined clients with new-dimension spawn fallback states that keep
  their retained selected hotbar slot/held stack. Direct protocol metadata
  changes can no longer restamp old block/entity caches into the new dimension
  before the host facade reseeds authoritative world snapshots, while player
  inventory authority survives the transfer.
- Tightened item-10 multiplayer dimension scoping for hosted remote players.
  Host-side player-state lookup now has explicit current-dimension and
  current-dimension-live helpers, and host-handled entity actions, item replays,
  death drops/respawns, enchanting, crafting, container edits, block edits,
  player attacks, fishing-owner snapshots, AI target lookup, and hosted remote
  rendering use those gates instead of raw cached player ids. Old-dimension
  player snapshots are now removed from host render views and can no longer be
  targeted, damaged, used as projectile/fishing owners, or used to replay client
  actions after a dimension transfer.
- Added item-10 multiplayer remote player replication polish. Player-state
  snapshots now carry bounded `remote.*` sprint/use/block/bow animation state in
  addition to the real `input.*` movement bits, remote player views apply that
  metadata before pose interpolation, and remote views tick transient swing/use
  animations locally. Client action and block-update packets now also trigger
  visible remote swing/place/use animations while skipping local echoes, so
  attacks, entity uses, projectile/item releases, and block edits no longer look
  like silent pose-only updates on other peers.
- Added item-10 multiplayer real input-state sync. The local `Player` now
  captures raw forward/back/left/right/jump key state, clears it when gameplay
  controls are paused or screens/chat take over, and sends those bits through
  the high-level multiplayer client instead of defaulting every movement bit to
  false. The protocol server and host facade now mirror sanitized
  `input.forward/backward/left/right/jumping/sneaking` metadata into
  player-state snapshots for host authority, late joins, and downstream
  animation/interaction logic.
- Fixed item-10 client-side application of streamed block snapshots. Multiplayer
  clients now keep a bounded deferred block-update queue for host block packets
  whose target chunks have not loaded yet, retry it during loading and normal
  network drains, and clear it on unload or dimension rebuild. Initial streamed
  saved-world edits and later host block changes therefore no longer disappear
  just because the local client terrain loader has not caught up to that chunk.
- Added an item-10 multiplayer initial-sync completion barrier. After the
  server streams the admitted player's roster, world metadata, and cached
  player/block/entity/inventory packets, it now sends an `initial_sync_complete`
  world event with snapshot counts. The high-level multiplayer client waits for
  that marker after receiving world metadata, so connect completion no longer
  depends on timing while a streamed snapshot burst is still draining.
- Expanded item-10 multiplayer initial snapshot sync. Join acceptance now sends
  an authoritative player-list packet first, then a metadata-only world-state
  packet, then streams cached player, block, entity, and inventory snapshots as
  normal bounded protocol packets. Regular world-state broadcasts now carry
  time/weather/spawn/rules/dimension metadata only instead of rebroadcasting the
  entire cached world. Late joiners still receive pre-existing host state, but
  large saved/edited worlds no longer depend on one oversized JSON line during
  initial sync or weather/time updates.
- Expanded item-10 Release-era multiplayer binary-client compatibility. The
  protocol server now recognizes old binary login and handshake packet IDs at
  connection accept time and answers with a Release-style `0xFF` kick packet
  carrying an explicit CraftZero protocol-required reason instead of letting the
  bytes fall into the JSON reader and close silently. The same kick-packet
  writer now serves status pings, status MOTD text is sanitized before the
  `motd / online / max` response is built, and malformed custom JSON packets
  receive a bounded `Bad packet` disconnect before cleanup. Legacy status text
  now routes through a shared parser/formatter that preserves the plain
  Release-style `motd§online§max` response for `0xFE` pings while also answering
  `0xFE 0x01` probes with the null-delimited extended legacy status fields.
  Status packets now bypass the generic kick-reason sanitizer after their
  individual fields are sanitized, so the extended null separators survive the
  UTF-16BE `0xFF` packet writer.
- Tightened item-10 multiplayer wire/legacy bridge hardening. Typed protocol
  reads now use a bounded line reader, outbound packets are size-checked before
  flush, decode rejects oversized lines and malformed numeric/boolean fields
  instead of silently coercing them, and `data` maps are capped to protocol
  limits. The legacy `NetworkMessage` facade now normalizes message types,
  sanitizes payload keys/values, tolerates malformed JSON by returning no
  message, and the multiplayer bridge drops invalid legacy sends without
  throwing through gameplay code.
- Tightened item-9 source-order quick-move parity. Container slot ordering now
  exposes a reverse player-inventory destination order that matches the old
  container transfer shape, and player inventory, crafting table, chest/chest
  minecart, furnace, dispenser, brewing stand, and enchanting table
  shift-click paths use the shared filtered quick-move engine for
  container-to-player transfers instead of the generic pickup/add path. Partial
  remainders stay in the source slot, tile containers still mark dirty, brewing
  potion take bookkeeping is preserved, and crafting outputs/remainders keep
  their dedicated crafted/add notification path.
- Tightened item-8 audio event/sink/resolver parity. `WorldSoundEvent` now
  normalizes logical ids, clamps malformed positions, volume, and pitch, and
  exposes one `isPlayable` boundary used by world queuing, multiplayer
  rebroadcast, dispatch, OpenAL playback, and ambient music. The dispatcher and
  OpenAL sink now reject null/non-finite cue state, sanitize listener/source
  coordinates and orientation vectors, contain sink/decode failures, and keep
  record/music/transient source handling from poisoning later playback. The
  resolver now accepts direct `.ogg` references, root-level fallbacks, broader
  music/streaming/record layouts, Release-style `note.bassattack`, Ender
  Dragon death aliases, wolf panting variants, singular/plural Enderman portal
  folders, and extra classic sound roots without changing gameplay sound ids.
- Tightened item-7 render/particle state hardening. Camera position, movement,
  look-target, aspect, FOV, and direction-vector updates now reject malformed
  values before projection/view matrices are rebuilt. Renderer fog, color,
  light, tint, brightness, alpha, mesh, and texture entry points now clamp or
  fail closed instead of sending NaN/Infinity into shader uniforms or model
  matrices. World particles now sanitize construction, network particle
  payloads, collision stepping, interpolation, scale, encoded block/item data,
  and render-time partial ticks, and the world particle queue refuses invalid
  visual events before rendering or multiplayer rebroadcast.
- Tightened item-6 damage and explosion finite-math hardening. Shared
  `DamageSource` instances now normalize null types, invalid source
  coordinates, knockback magnitudes, and looting levels, while player/living
  hurt paths reject non-finite damage before armor, resistance, invulnerability,
  or knockback math runs. Living-entity `addMotion` now uses the same sanitized
  motion contract as base entities. Explosion distance, impact, push, particle,
  block-drop, and ray-propagation helpers now fail closed on malformed power or
  coordinates, so invalid blast data cannot turn into NaN damage, push, or
  destroyed-block walks.
- Tightened item-6 shared geometry and raycast math hardening. AABBs now
  normalize inverted endpoints and sanitize malformed constructor, offset,
  expand, and movement inputs before shared collision math sees them. Block
  selection, fluid-source selection, entity picking, slab intersections,
  line-of-sight/cliff checks, and explosion exposure now reject null,
  non-finite, zero-length, or invalid-range inputs at their entry points, so
  bad geometry cannot leak NaN/Infinity into targeting, AI visibility, or blast
  attenuation.
- Tightened item-5 mob retaliation and natural pack spawning parity. Combat
  target ownership now resolves through one shared helper for direct hits,
  arrows, thrown items, and fireballs, so hostile retaliation, legacy
  `HurtByTargetGoal` revenge targeting, tamed-wolf assist retaliation,
  spider provocation, and Zombie Pigman player-aggression checks no longer
  carry separate projectile-owner logic. Revenge targets now reject removed
  attackers, keep Y-aware movement targets, and clear stale movement when they
  stop. Natural ground-pack spawning now uses the same local ground re-check
  path for jittered pack members as explicit pack helpers, so packs can spread
  over uneven valid terrain instead of inheriting one flat Y slice from the
  first candidate.
- Tightened item-9 menu-factory settings parity. The newer
  `MenuScreenFactory` options path now exposes the Release-style `Language...`
  route, builds a real language selection screen with current-language feedback
  plus selectable language rows, and carries the chosen language through
  `SettingsModel` and the existing settings callback. `Content` can now provide
  available languages and the menu system has a dedicated language screen id,
  so the factory path no longer lags behind the older runtime options screen.
- Tightened item-8 sound-pack compatibility. The Release-era sound resolver now
  treats door, chest, fuse, level-up, consume, fire, rain, portal, and wolf
  shake cues as variant-capable pools, deduplicates generated candidate paths,
  and falls back through common old pack naming differences such as
  no-underscore door/chest files, flat piston names, `pigzombie` folders, and
  singular `enderman` folders. Existing exact `sounds/`, `sound/`,
  `newsound/`, and `sound3/` candidates remain first, so packs that already
  match CraftZero's emitted IDs keep priority while older archived layouts have
  a better chance to play.
- Tightened item-7 render-angle interpolation safety. Entity yaw/pitch setters
  now normalize yaw, clamp pitch, and reject non-finite angle input, while
  remote-pose application and `lookAt` route through those setters before render
  interpolation sees the values. Camera rotation, look-target, and direct
  yaw/pitch setters now apply the same finite/normalized rules, preventing
  malformed angles from leaking NaN or unbounded rotations into model and view
  matrices.
- Tightened item-6 shared entity motion math. Generic entity `setMotion`,
  `addMotion`, and remote-pose motion application now sanitize non-finite
  velocity components to zero before they reach physics/collision integration,
  preventing NaN or infinite motion from poisoning projectiles, mobs, dropped
  items, carts, or restored/synced entity state while leaving ordinary finite
  high-speed motion unclamped.
- Tightened item-5 mob AI pathing parity. The shared A* navigator now removes
  and re-adds open-set nodes when a cheaper route is found, so Java's priority
  queue no longer leaves mobs following stale path costs. `MobAI` movement
  targets now track Y as well as X/Z, and the main chase/retaliation paths for
  melee mobs, creepers, wolves, sheep hunting, anger, and nearest-target goals
  pass the real target height into navigation instead of reusing the mob's
  current Y.
- Tightened item-4 tile-entity drop parity. Breaking or explosion-removing
  tile-backed blocks now scatters their contained item stacks in old-style
  randomized 10-30 item fragments with per-fragment offsets and Gaussian toss
  velocity, while preserving stack metadata such as durability, potions, maps,
  names, and enchantments. The same helper is shared by normal block breaking
  and explosion cleanup, so chests, furnaces, dispensers, brewing stands, and
  other inventory-backed tiles no longer eject one centered whole stack per
  slot. Loaded chunk reconciliation now also purges restored/generated tile
  entities whose block no longer exists or whose tile type does not match the
  block, repairs missing/wrong tile instances with the proper Release-era tile
  class, and mirrors tile-owned visual state back onto blocks for jukebox
  records, brewing-stand bottle bits, and lit/unlit furnace state. Ghost
  containers from stale saves can no longer keep ticking, saving, or dropping
  after their backing block is gone.
- Tightened item-3 piston/redstone mobility parity. Piston pushability now uses
  named Release-style mobility buckets instead of loose chained checks: fluids
  and fire are displaced, plant/attachment/pressure/repeater/snow/door/bed
  targets crush through the destroy path, tile entities still block movement,
  locked chests join the immovable legacy blockers, and the 12-block push limit
  is explicit. Supported rails keep the existing move-and-settle behavior, while
  pumpkins, jack-o-lanterns, melons, leaves, and dragon eggs are no longer
  accidental crush-only targets. Connected redstone dust now settles as a
  bounded network when any member updates, so long lines, vertical step runs,
  and branch-heavy circuits converge their power values together instead of
  waiting for one scheduled neighbor tick per segment. Minecart-to-minecart
  collision now follows the old horizontally expanded overlap rule directly,
  so crossing, curved, and low-angle cart contacts exchange momentum instead
  of being rejected by the leftover travel-axis filter.
- Tightened item-2 save/load compatibility. Saves now write a gzip-compressed
  NBT `level.dat` sidecar with a Release-era `Data` compound carrying seed,
  level name, game type, spawn, time, last-played, region save version,
  rain/thunder timers, hardcore/commands, difficulty, and active dimension.
  `SaveManager` still prefers the full CraftZero `level.json` runtime payload,
  but can now import a lone Release-style `level.dat` as metadata plus empty
  runtime scaffolding so world headers are not locked to the custom JSON path,
  and now mirrors modified chunks into legacy `.mcr` region files. The region
  bridge reads/writes Release-style `r.x.z.mcr` files for Overworld, Nether,
  and End folders, converts the old y-fast chunk block/nibble layout into
  CraftZero's runtime layout, preserves chunk block ids, metadata, sky light,
  block light, and height maps, and falls back to `.mcr` import when the native
  `c.x.z.bin` chunk is absent or corrupt. Region import/export now also carries
  common Release chunk runtime NBT: chest/furnace/dispenser/brewing/sign/
  spawner/note/jukebox/enchanting-table tile entities, item-stack inventories,
  dropped items, XP orbs, paintings, boats, minecarts, TNT, falling blocks,
  arrows, fireballs, thrown items/potions, Eyes/pearls, and implemented mobs
  with the Release-era per-type state CraftZero can restore. Release
  `TileTicks` now round-trip as CraftZero scheduled block ticks, so pending
  fluid, fire, redstone, crop, and other tickable-block updates are no longer
  discarded by region conversion. Item stack `tag` NBT now bridges custom
  display names, supported enchantments, old potion damage values, map ids, and
  CraftZero metadata for inventories, dropped items, and thrown splash potions.
  Release `data/map_*.dat` files now import/export shared filled-map color
  payloads plus scale, center, dimension, and `idcounts.dat` allocation state,
  and loaded map stacks are enriched with matching map metadata so old maps keep
  their pixels and viewport identity. Release `level.dat` player compounds now
  preserve player position, motion, rotation, selected hotbar slot, inventory,
  armor/crafting slots, spawn point, health, hunger, saturation, exhaustion,
  air/fire state, score/XP total, and active potion effects instead of falling
  back to a default player on import. Player inventory stacks now use the same
  rich Release stack `tag` bridge as chunk inventories, so custom names,
  supported enchantments, potion/map identity, and scalar CraftZero metadata no
  longer flatten when they live on the player. Standalone Release
  `players/*.dat` files are now imported as a fallback/override for
  server-style worlds and CraftZero writes `players/Player.dat` beside
  `level.dat` for old tooling that expects per-player save files. Supported
  native CraftZero `level.json` saves now normalize/migrate before validation,
  filling missing player, inventory, time, weather, map, admin, and generator
  fields so older format versions are not rejected before defaults are applied.
  Release sidecars now include `session.lock`, copy the previous `level.dat` to
  `level.dat_old`, and can fall back to `level.dat_old` when the primary
  Release level file is missing or corrupt.
  Unknown or unsupported old NBT records are skipped or preserved as scalar
  metadata instead of poisoning the runtime lists, so remaining save
  compatibility work is now truly obscure old-NBT edge cases rather than the
  absence of entity/tile/tick/stack/player/map conversion or native migration.
- Tightened item-10 multiplayer snapshot authority. The typed multiplayer
  protocol still stamps current dimension metadata onto live and cached player,
  block, entity, inventory, client-action, sound, particle, and lightning
  payloads, and now validates remembered/broadcast state before it can enter
  the server snapshot. World, player-list, player, block, entity, and inventory
  messages reject invalid ids, out-of-bounds coordinates, non-finite poses or
  health, oversized metadata, bad slots/counts/damage, and malformed usernames;
  cached snapshot readers also filter invalid leftovers. Clients can no longer
  submit raw `PlayerState`, `WorldState`, `PlayerList`, or `Hello` messages as
  live updates, so movement remains normalized through `ClientInput`. The
  custom protocol JSON parser and legacy `NetworkMessage` bridge now reject
  non-finite decimal payloads instead of letting overflow, `Infinity`, or `NaN`
  become multiplayer state.
- Tightened item-9 UI/settings parity. The alternate `MenuScreenFactory` video
  settings model now exposes the same broad Release-era option surface as the
  runtime menu path: Graphics, Render Distance, Smooth Lighting, Performance,
  3D Anaglyph, View Bobbing, GUI Scale, Advanced OpenGL, Brightness, Clouds,
  and Particles. Those fields now mutate through the screen controls and copy
  through the callback payload instead of preserving only graphics/render
  distance/GUI scale.
- Tightened item-8 audio parity. Player and living-entity fall damage now emits
  the old `damage.fallsmall` or `damage.fallbig` cue before the hurt/damage
  path, matching the source distinction between ordinary and heavy landings.
  The sound asset resolver now also treats common `random.*` gameplay cues as
  variant pools and aliases `random.hurt` to legacy `damage/hurtflesh*`
  resources, so classic resource packs with old damage media layouts resolve
  without changing gameplay sound ids.
- Tightened item-7 rendering/animation/particle emitters. Shared entity visual
  effects now use stable crit-particle counts, entity-sized burst placement,
  body-volume mob death poofs, impact-scaled water-entry bubble/splash counts,
  and speed-gated sprint tile-crack dust. Blaze charge-up flames/smoke now wrap
  the Blaze body volume instead of popping from a single point, and the shared
  particle burst scale/lifetime rules are reused across the old point-burst and
  new entity-burst paths.
- Tightened item-6 entity ray selection. The shared living-entity raycast now
  rejects removed entities like the other entity raycast paths already did, and
  player combat/entity picking now queries active plus queued same-tick
  entities. Melee targeting and right-click entity selection no longer risk
  preferring a removed living entity that is waiting for end-of-tick cleanup or
  missing a valid queued target.
- Tightened item-5 same-tick entity visibility. Natural mob cap accounting now
  counts active, pending, and generated-but-not-yet-merged mobs, so queued
  same-tick spawns reserve Release-era cap space instead of being invisible
  until the next entity merge. Projectile, splash-potion, Ender Pearl, thrown
  item, and fishing-hook collision scans now use that same active-plus-pending
  entity view, preventing same-tick spawned mobs, vehicles, paintings, and
  hookable targets from being skipped by transient entity interactions.
- Tightened item-4 multi-block block-change side effects. Suppressed batch
  edits for Nether portal frame/interior creation and removal, door placement
  and removal, and bed placement/removal now replay the same delayed simulation
  side effects as ordinary block edits after the full shape exists. Those paths
  now clear powered-openable state, wake redstone/mechanisms, run support
  checks, immediately tick adjacent fluids, and trigger lava/water mixing
  without breaking half-built two-block or portal structures mid-edit.
- Tightened item-4 fluid wakeups around block changes. Whenever a block changes,
  neighboring water/lava cells now get an immediate scheduled update in addition
  to the normal delayed fluid cadence, so bucket edits, block placement/removal,
  ice transitions, piston-settled blocks, and fluid displacement clear or
  reshape adjacent stale flow without waiting on a later unrelated tick.
- Corrected another item-3 detector-rail timing edge. Minecart rail physics now
  wakes a detector rail at the start of the cart's rail step before movement
  projection can carry the cart out of the cell, while retaining the existing
  end-of-step detector update for the rail the cart finishes on.
- Corrected another item-3 piston entity-motion edge. Players displaced by
  piston extension or moving-piston travel now inherit the same shove velocity
  applied to other piston-moved entities, so piston pushes no longer teleport
  the local player without carrying momentum into the next movement tick.
- Corrected another item-3 piston/minecart parity edge. Minecarts displaced by
  piston extension or moving-piston travel now immediately resync seated living
  passengers, preventing a one-tick rider/cart separation during piston shoves.
- Added all-dimension save/load runtime preservation. `SaveManager` now primes a
  runtime cache from the root level payload plus every
  `dimensions/<dimension>/runtime.json` sidecar, updates the active dimension
  snapshot before every save, writes every cached dimension runtime sidecar, and
  restores revisited dimensions from the cache. Saving or autosaving after
  leaving the Nether/End/Overworld no longer risks forgetting that dimension's
  dropped items, tile entities, entities, moving pistons, or scheduled block
  ticks.
- Added host-authoritative multiplayer ordinary block break/place handling.
  When a connected client sends a break-to-air edit, the host now derives the
  result from its live world block, cached held tool, harvest rules, block drop
  resolver, and tool durability instead of blindly setting air from the client
  payload. When a connected client places into air, the host now requires a
  cached selected placeable item whose placed block matches the requested block,
  checks the live world placement rule, applies the block in the host world,
  consumes the cached selected stack outside creative mode, and lets the normal
  block-change sync broadcast the host-authored result.
- Added host-authoritative multiplayer special block interaction handling. The
  host now derives connected-client fluid bucket pickup/place, cauldron
  bucket/bottle use, flint-and-steel fire/TNT ignition, door/bed/sign multi-block
  placement, slab merging, redstone ore activation, door/trapdoor/fence-gate and
  redstone control toggles, cake eating, dragon egg teleporting, hoe tilling,
  crop/stem seed planting, end portal frame eye insertion, and crop/stem bone
  meal use from the host world plus cached held item state. These paths consume,
  replace, or damage cached remote inventory on the host and broadcast the
  host-authored block state, so common special edits no longer rely on blindly
  accepting the client's final block metadata.
- Added multiplayer block-container inventory sync for chest, furnace,
  dispenser, and brewing stand screens. Connected clients now send a validated
  `container_update` action when an open block-container tile becomes dirty;
  the host applies the inventory snapshot only to a live matching tile in reach,
  marks it dirty, and rebroadcasts the host tile state through the existing
  block/tile sync path. Double chests sync both backing chest tiles.
- Extended multiplayer container sync to chest minecarts. Chest minecart screen
  edits now track a dirty cart inventory, send the same validated
  `container_update` action with a target entity id, and the host accepts it
  only for a reachable tracked chest-minecart entity before rebroadcasting the
  accepted entity state. Chest minecart inventory is now included in ordinary
  entity sync as `entity.inventory.*`, so accepted cart contents replicate to
  other clients and reconnecting clients.
- Added host-authoritative multiplayer enchanting-table actions. Connected
  clients now send an `enchant_item` intent with the table position, offer slot,
  cost, offer seed, and pre-enchant item snapshot after a local enchant click;
  the host accepts it only for a reachable live enchanting table, recomputes
  bookshelf power, offer cost, enchantment output, and level spending from the
  host world plus cached remote progression, then sends the accepted result and
  corrected XP totals back to the target client table screen.
- Added host-validated multiplayer crafting output replay. Inventory and
  crafting-table screens now record successful output clicks and shift-click
  batches as `craft_item` intents containing the pre-click grid and cursor
  snapshots. The host validates bounded 2x2/3x3 craft intents, checks 3x3 table
  reach against a live crafting table, recomputes the recipe/output with the
  shared Release-era crafting registry, replays cursor or quick-move output
  insertion through the normal inventory rules, consumes ingredients through
  `CraftingGridOps`, and broadcasts authoritative changes across the full
  multiplayer inventory slot range including cursor and 2x2 crafting slots.
  Accepted 3x3 grid state is sent back to the owning crafting-table screen.
- Added host-authoritative multiplayer Q-drop replay. Connected clients now
  send selected-slot drops as a validated `item_use/drop_item` intent instead
  of spawning a client-local dropped item. The host reconstructs the dropped
  stack from the cached selected inventory slot, spawns the thrown dropped-item
  entity with the normal pickup delay and forward velocity, consumes one cached
  selected item outside creative mode, and lets existing inventory and dropped
  entity sync broadcast the authoritative result.
- Added host-authoritative multiplayer screen/cursor drop replay. Connected
  clients now send inventory-screen, crafting-grid, cursor, and container-close
  drops as validated `item_use/drop_stack` intents with serialized stack data
  and clamped throw velocity. The client no longer spawns those UI drops
  locally; the host spawns the dropped-item entity and the existing inventory
  sync clears the source cursor/grid/container state.
- Added host-authoritative multiplayer remote action inventory costs. Hosted
  replays of connected-client combat now compute damage from the cached stack
  metadata, including supported enchantment bonuses and looting, and successful
  entity/PvP attacks damage the server-cached held durable item before
  rebroadcasting changed inventory slots. Host-replayed item uses now also
  mutate authoritative cached inventory for arrows, bow durability, eggs,
  snowballs, ender pearls, Eyes of Ender, splash potions, boats, minecarts, and
  fishing-rod reel durability, while right-click entity mutations consume or
  damage the relevant saddle, dye, shears, breeding item, wolf bone/meat, or
  furnace-minecart fuel only when the host-side interaction actually succeeds.
- Added host-authoritative multiplayer player-vs-player attack parity. Local
  player input now asks the multiplayer layer for remote player hit boxes before
  mining through a rendered player, so host players can hit connected clients
  and clients can hit the host or other connected clients with normal left-click
  reach ordering. Clients send a validated `player_attack` intent containing
  only the target player id; the host checks cached actor/target player state,
  recomputes weapon/enchantment damage and knockback from server-side inventory
  state, applies host damage locally when the target is `host`, or sends the
  existing targeted damage command to the selected remote client.
- Added host-authoritative multiplayer hostile targeting against remote players.
  `World` now exposes a remote-player target/damage provider backed by hosted
  client state, and `MobAI` can retain a connected player target separately from
  ordinary living-entity targets. `TargetNearestGoal` now chooses between the
  host player and remote survival players with the same range and line-of-sight
  checks, while `MeleeAttackGoal` and `RangedAttackGoal` chase, face, melee-hit,
  and shoot arrows at the selected remote player through the targeted damage
  command path. This gives common hostile mobs and skeleton-style ranged mobs a
  real host-authoritative path to attack connected clients instead of only
  reacting to the host player.
- Added host-authoritative multiplayer projectile hits against remote players.
  `World` now exposes a remote projectile-player interaction handler so arrows,
  fireballs, thrown eggs/snowballs, and splash potions can include connected
  clients as first-class closest-hit candidates beside blocks, mobs, vehicles,
  paintings, and the local player. Arrow and fireball direct hits now send the
  same targeted damage command path with projectile knockback/fire metadata,
  thrown item impacts terminate at the remote hit point, and splash potions now
  dispatch targeted potion-effect commands so remote clients apply healing,
  harming, poison, regeneration, and other splash effects through
  `PotionEffectResolver.applyToPlayer(...)`.
- Added host-authoritative multiplayer explosion and lightning damage for remote
  players. `World` now exposes a small damage-event listener beside its existing
  block-change listener, and hosted worlds translate real explosion/lightning
  events into targeted remote damage commands. Remote explosion damage samples
  line-of-sight exposure against the connected player's host-side bounding box,
  applies the same Release-era damage curve and knockback vector as the local
  player path, while lightning uses the same strike box, 5-damage hit, and
  160-tick fire application.
- Added host-authoritative multiplayer remote environmental damage. Hosted
  worlds now scan connected remote player snapshots against loaded lava, fire,
  cactus, and suffocation collisions using the same player box dimensions and
  contact damage values as local play, send a targeted damage command with
  fire-tick metadata to the affected client, and let the client apply the hit
  through `Player.hurt(...)` so difficulty, armor, resistance, fire resistance,
  hurt invulnerability, sounds, and armor durability stay on the normal code
  path.
- Added host-authoritative multiplayer remote pickup resolution. Hosted worlds
  now scan connected remote player-state snapshots for nearby dropped items and
  XP orbs after host physics ticks, merge collectable item stacks into the
  server's cached hotbar/main inventory slots with the existing inventory stack
  rules, rebroadcast only changed slots, remove or split the authoritative drop,
  and emit the usual pickup sound/particle events. XP orbs now use a small
  host-side per-player pickup cooldown, send the existing targeted experience
  action to the collecting client with pickup metadata, remove the host orb, and
  let the client's next player-state payload propagate the new progression
  values.
- Added host-authoritative multiplayer vehicle riding sync. Client player-state
  metadata now carries bounded `vehicle.*` keys for mounted entity id/type,
  forward/strafe/yaw rider input, and one-shot dismounts. The host maps remote
  players to authoritative boat, rideable minecart, and saddled-pig entity ids,
  reserves occupied vehicles, clears stale mounts on disconnect/removal, feeds
  boat steering and minecart start input into the host simulation, and snaps
  mounted player state to the host vehicle position. Entity snapshots now carry
  `riderPlayerId` so clients restore occupied vehicles and local owners replace
  stale speculative mounts with the host-backed vehicle state.
- Added host-authoritative multiplayer fishing rod cast/reel sync. Clients now
  record fishing casts and reels as validated `item_use` actions; the host owns
  the remote bobber entity, resolves bite/fish/pull outcomes from a live remote
  owner snapshot, and syncs fishing-hook wait/catchable/stuck state through the
  normal entity snapshot path. Synced hooks also carry owner id/pose metadata so
  the owning client replaces its speculative local bobber with the host copy and
  remote clients can render the fishing line from the correct player hand.
- Added multiplayer ender-pearl teleport reconciliation. Host-spawned
  ender-pearl projectiles can now carry a one-shot impact callback, and remote
  client pearl throws install that callback with the throwing client's player
  id. When the authoritative host pearl lands, the host sends the existing
  targeted teleport client action back to that client using the impact
  coordinates, so the projectile remains host-authored while the throwing
  client is moved to the same landing point.
- Added host-authoritative multiplayer projectile and entity-spawning item-use
  requests. Client bow shots, egg/snowball throws, ender pearl throws, Eyes of
  Ender, splash potions, boat placement, and rail minecart placement are now
  recorded after successful local use and sent as validated `item_use` client
  actions. The protocol server only accepts known item-use subtypes with finite
  direction/power data, and still validates rail-target minecart placement
  against the client's server-known reach. The host reconstructs the client's
  selected stack from the authoritative inventory snapshot, replays the
  projectile or vehicle spawn in the host world, and lets the existing entity,
  sound, inventory, particle, and block sync paths propagate the result.
- Added host-authoritative multiplayer entity action requests. Client-side
  player attacks and successful right-click entity uses are now recorded by the
  local `Player`, translated back to the host's entity snapshot id, and sent as
  validated `entity_attack`/`entity_use` client actions. The protocol server
  accepts those actions only when the target exists in its authoritative entity
  snapshot and is within reach of the client's last server-known pose. The host
  applies attacks to living entities, vehicles, paintings, and deflectable
  fireballs, applies common right-click mutations for sheep, pigs, wolves,
  breeding feed, boats, rideable minecarts, and furnace minecart fuel/push
  direction, then immediately rebroadcasts the authoritative entity state.
  Entity snapshots now also carry sheep wool/sheared/eating state, pig saddled
  state, and wolf tame/sit/anger/owner state so those multiplayer interactions
  become visible to remote clients.
- Expanded multiplayer player-state survival/progression payloads. Player-state
  metadata now carries hunger, saturation, air, exhaustion, fire/on-fire state,
  total XP, score, level, and current level progress in addition to the existing
  pose, health, held-item, armor, and status-effect fields. Clients send the
  same bounded survival/progression metadata to the host; protocol servers
  preserve only whitelisted `status.`, `stats.`, and `progression.` keys while
  still deriving armor visuals from cached inventory. Remote player views apply
  the richer state on both normal client messages and the host's direct server
  snapshot render path, so burning players, food/air state, and XP/score
  snapshots no longer remain purely local.
- Added multiplayer lightning visual event sync. Host lightning strikes now add
  their generated jagged bolt render object to a drainable event queue, serialize
  the exact segment geometry plus flash windows through the typed
  `world_event`/lightning payload, and clients reconstruct the same visual-only
  bolt while triggering the sky flash locally. Lightning gameplay remains
  host-owned: fire placement, entity damage, sounds, and resulting block/entity
  state continue through the authoritative world, sound, block, and entity sync
  paths.
- Added multiplayer world-particle event sync. Host worlds now record newly
  spawned particles separately from the live render list, drain that event queue
  once per tick, and rebroadcast non-weather particle cues through the typed
  `world_event`/particle payload. Clients reconstruct received particles with
  the source type, position, motion, scale, lifetime, data value, and target
  interpolation fields, so explosion puffs/debris, block crack/dust, redstone
  sparks, note glyphs, spell effects, item pickup arcs, enchantment-table glyphs,
  and other host-authored gameplay particles become visible remotely without
  duplicating old particles every frame.
- Added multiplayer world-sound event sync. Host worlds now drain sound events
  once, rebroadcast gameplay sound cues through a typed `world_event`/sound
  payload, and still play the same drained events locally through the normal
  spatial dispatcher. Clients queue received world sounds into their local
  world and play them through the existing dispatcher path, so block, redstone,
  piston, explosion, jukebox, mob, weather, pickup, XP, and other world-authored
  cues no longer stay host-local in multiplayer.
- Expanded multiplayer player-state visual payloads. Typed `ClientInput` and
  `PlayerState` messages now carry bounded string metadata, while the protocol
  server copies only client status-effect keys and derives armor visuals from
  the server's cached armor inventory slots. Host player-state broadcasts and
  seeded snapshots include armor stack payloads plus active status effects, and
  remote player views apply those armor/effect fields so rendered players can
  show equipped armor and current potion/status state instead of only pose,
  health, sneak, held item, and game mode.
- Expanded multiplayer player-inventory sync to preserve rich stack metadata.
  `InventoryUpdate` packets and world snapshots now carry the same string data
  payload shape used by tile-entity item stacks, including custom names, potion
  data, enchantments, and stack metadata. Host/client sends, server snapshot
  caching, legacy `NetworkMessage` bridging, and local inventory application all
  pass the payload through while still validating the top-level item/count/damage
  fields.
- Normalized multiplayer player-state held-item data against server inventory
  snapshots. Client movement/state packets still provide pose and basic status,
  but their advertised held item fields are now derived from the server's
  cached hotbar slot for that player in both the protocol snapshot and host
  facade mirror, preventing movement packets from independently spoofing held
  item/count/damage state.
- Moved multiplayer inventory rebroadcasts to the host facade. Client inventory
  packets are still validated by the protocol server, but the low-level server
  no longer writes them directly into the snapshot cache or rebroadcasts them;
  the admitted player id is normalized and relayed through `MultiplayerServer`
  before the authoritative protocol broadcast/cache path runs.
- Moved multiplayer block-edit rebroadcasts to the host world. Client block
  packets now act as validated edit requests only: the protocol server forwards
  them to the host runtime without rebroadcasting or caching the requested
  state, and the host rebroadcasts the actual post-apply block metadata/tile
  payload after mutating its world.
- Hardened multiplayer tile payload authority. Client-authored block edits now
  strip all tile payload data before the server caches or rebroadcasts them,
  while sign editing uses a bounded `sign_update` client action that the host
  applies to its real sign tile entity and rebroadcasts through the
  host-authored tile payload path.
- Added multiplayer tile-entity state payloads. Block updates and initial
  world-state block snapshots now carry typed tile data for signs, chests,
  furnaces, brewing stands, dispensers, note blocks, jukeboxes, enchanting
  tables, and monster spawners; host dirty tile entities are rebroadcast on a
  throttled sync path, and receivers rebuild/apply matching tile entities with
  inventories, item stack metadata, sign text, timers, and animation state.
- Expanded multiplayer projectile/vehicle entity snapshots. Host entity updates
  now include Eye of Ender target/drop state, preserve splash-potion payload
  data when remote clients materialize potion entities, and broadcast minecart
  damage, rolling amplitude/direction, boat hit wobble, arrow stuck/knockback
  state, and XP-orb pickup/health state so existing remote appliers receive
  authoritative non-living entity state. Generic living-entity updates now
  also carry fire timers, active status effects, hurt flash timing,
  invulnerability comparison state, recent player-credit timers, and death
  animation phase, keeping damaged, burning, and potion-affected remote mobs
  aligned with the host after the initial combat or splash event.
- Added immediate multiplayer death/respawn inventory sync. Death drops and
  respawns now force all carried inventory slots through the multiplayer
  inventory sync path alongside the immediate player-state update, so remote
  snapshots do not retain stale held items, armor, crafting slots, or cursor
  contents after the ordinary update loop returns early for the death screen.
- Added host-spawned multiplayer player death drops. The local `Player`
  death-drop loop now has a multiplayer-aware drop handler; connected clients
  send source-slot-indexed death inventory drops and death XP as validated
  item-use subactions after forcing their health-zero state to the host, and no
  longer spawn those dropped items or XP orbs in their client-only world. The
  host accepts stack-drop packets only from dead player state, derives the
  dropped stack from the cached authoritative inventory slot, clears and
  rebroadcasts that slot, clamps motion/pickup delay, derives death XP from the
  cached progression level instead of the packet amount, clears the cached XP
  after payout, and spawns the authoritative dropped items and XP orbs for
  normal entity sync.
- Added immediate multiplayer death/respawn player-state sync. The runtime now
  forces a player-state broadcast/send when the local player enters the death
  path and again immediately after respawn, bypassing the ordinary throttled
  0.1s player-state timer so remote players and late snapshots see health-zero
  death state and the restored respawn state promptly.
- Added host-validated multiplayer respawn requests. Connected clients now send
  a `player_respawn` request from the death screen instead of locally accepting
  arbitrary post-death coordinates; the protocol server accepts that request
  only from dead player state, the host chooses a valid bed respawn or its
  cached/server world spawn target, and the client respawns only after the host
  returns an accepted target. Player-state payloads now include bounded
  respawn/bed-spawn metadata, and server-issued `/spawnpoint` commands are
  remembered as host-side respawn overrides for later multiplayer death flows.
- Added multiplayer pre-join socket timeout cleanup. Pending sockets now track
  their connect time and the server keepalive loop removes handshakes that do
  not complete admission within the protocol join timeout, sending a direct
  `Join timeout` disconnect without emitting join/leave announcements.
- Added admission-gated multiplayer initial sync. Protocol clients now receive
  only the version/assigned-id hello before admission; the full world-state
  snapshot is sent only after the server accepts the join. Server gameplay
  broadcasts now skip sockets that have not completed admission, while the
  client connect flow waits for accepted roster membership before requiring the
  world snapshot so bans, full servers, duplicate names, and invalid names keep
  their explicit disconnect reasons.
- Added multiplayer roster/view reconciliation. Client player-list snapshots now
  update existing remote-player display names from the authoritative roster and
  prune stale rendered `player-N` views that no longer appear in that roster,
  while preserving host-authored `host` snapshots. This gives player-list
  updates a cleanup role if a leave/disconnect packet is missed or arrives out
  of order.
- Added graceful multiplayer client leave delivery. The high-level multiplayer
  client close path now sends an explicit disconnect packet when leaving an
  active connection unless the server has already sent one, so hosts can remove
  the player and refresh rosters through the normal disconnect path instead of
  waiting for a raw socket close to become `connection closed`.
- Tightened client-side multiplayer authority APIs. The multiplayer client
  facade no longer exposes a client-authored entity update send path now that
  entity snapshots are host-owned, and client chat sends no longer accept a
  caller-provided sender name. Local system/server messages on multiplayer
  clients now remain local; only the hosting runtime broadcasts `Server` chat
  through the protocol server.
- Constrained client-authored multiplayer actions. The protocol server now only
  accepts client actions for validated block edits or the two bed sleep intents
  the runtime actually expects from clients; arbitrary client action names and
  action data are dropped before the host bridge can mutate sleep state or
  process them. Accepted client actions also have their player id rewritten to
  the admitted connection identity before listener dispatch.
- Hardened multiplayer chat and command identity. Joined client chat packets now
  pass a bounded text-length gate, then the protocol server rewrites the
  packet player id and sender to the admitted connection identity before host
  listeners can process ordinary chat or slash-command dispatch. The host
  bridge also derives command/chat sender fallback from the connected-player
  roster instead of trusting a packet-provided display name.
- Hardened multiplayer player-state authority. Client movement/state packets now
  pass server-side sanity checks for finite bounded coordinates, health range,
  selected hotbar slot, held-stack count/damage, and valid network ids before
  the host listener or player snapshot cache sees them. Newly accepted clients
  also broadcast their seeded spawn player-state immediately after the roster
  update, so idle joined players become visible before their first movement
  tick replaces the spawn snapshot.
- Added multiplayer block-edit reach authority. Accepted joins now seed a
  server-side player snapshot at the host spawn point, movement packets replace
  that state as before, and client-originated block updates are only accepted
  when the target block center is within a bounded interaction radius of the
  server's latest player eye position. Far-away packet edits are dropped before
  the host runtime listener or rebroadcast cache can apply them.
- Tightened multiplayer block/inventory authority. Shared protocol constants now
  define Release-height block update bounds, block metadata nibble range, and
  the full carried inventory slot span including cursor state. The protocol
  server now rejects malformed client block and inventory packets before host
  listeners see them or snapshots rebroadcast them, cached block/inventory
  replay skips invalid state, and local apply/send paths use the same slot,
  stack-count, damage, and metadata limits.
- Added robust multiplayer disconnect delivery and teardown. Hosted shutdown now
  sends explicit `server closed` disconnect packets before sockets close, the
  protocol client synthesizes a `Connection lost` disconnect when a live socket
  drops without a packet and surfaces keep-alive response failures as `Timed
  out`, and local client disconnect packets now unload the multiplayer world
  before showing the Disconnect screen.
- Added multiplayer client join-acceptance gating. Clients no longer treat the
  pre-admission hello/world-state snapshot as a completed join; after initial
  sync they now wait until the server roster includes their assigned player id
  or a disconnect reason arrives. Rejected joins such as bans, full servers, or
  duplicate names therefore fail at the connection flow instead of briefly
  entering a local multiplayer world.
- Tightened multiplayer joined-player counting. Protocol-level connected-player
  counts and id lists now include only clients that completed admission, so
  legacy server-list ping population, max-player admission, roster snapshots,
  and multiplayer sleep eligibility no longer count raw sockets that have not
  successfully joined.
- Added host-authoritative multiplayer session/entity validation. Protocol
  clients must now complete one successful join before gameplay packets are
  accepted, duplicate join attempts are rejected, and client-originated entity
  update packets are dropped before they can reach the host runtime listener or
  be rebroadcast to other clients. Mobs, projectiles, dropped items, and other
  entity snapshots now stay owned by the hosted world state instead of by
  arbitrary client packets.
- Added live multiplayer access-control enforcement parity. Connected-player
  roster entries now expose each client's remote address, `/ban-ip <player>`
  resolves a connected player's address before banning, and player bans, IP
  bans, whitelist enable/remove/reload changes immediately disconnect live
  clients that no longer satisfy the active server policy while refreshing the
  save manager's cached administration state.
- Added multiplayer player-list latency parity. Keep-alive acknowledgements now
  preserve each connected client's round-trip time, the server broadcasts a
  refreshed typed player roster after joins, leaves, and latency updates, and
  the in-game Tab list renders compact signal bars beside each connected name
  while preserving unknown-latency fallback for snapshot-only remote players.
- Added in-game multiplayer player-list parity. The typed protocol now has a
  roster message that the host broadcasts after join/leave changes, clients
  keep a connected-name map separate from movement snapshots, and holding Tab
  during multiplayer gameplay renders a compact player list from local,
  roster, and visible remote-player names without stealing Tab from chat
  command completion.
- Added multiplayer connected-roster parity. The host facade now exposes the
  join/leave lifecycle as a stable connected-player roster instead of relying
  on movement snapshots to discover remote names. `/list`, tab completion,
  kicks, private messages, and targeted command actions now see newly joined
  idle clients immediately, while movement snapshots remain the source for
  coordinates, health, and inventory-facing state.
- Added saved-server status ping parity. The saved Multiplayer server list now
  starts daemon status probes for each saved entry, sends the same legacy
  `0xFE` ping packet the CraftZero host already answers, decodes the
  UTF-16BE status response, and updates each row with `pinging`, `offline`, or
  `motd online/max` status text. The parser accepts both the old
  section-sign-delimited response and null-delimited extended legacy response,
  and status probes now try `0xFE 0x01` first before falling back to plain
  `0xFE`; long status rows are clamped to the list width so the menu keeps its
  Release-style compact layout.
- Added server-side multiplayer command dispatch parity. Multiplayer clients now
  send slash-prefixed chat input to the host instead of executing it locally,
  the protocol server consumes those slash messages instead of rebroadcasting
  them as ordinary chat, and the host dispatches them through the normal command
  dispatcher using the remote player's name and operator permissions. Feedback
  is returned to the issuing client through targeted command messages, common
  self-targeting commands act on the remote sender, host-targeting commands can
  still affect the host player, and remote `/gamemode` plus `/xp` now use
  targeted client actions instead of accidentally changing only the host.
- Added multiplayer host admission parity for server administration commands.
  The typed protocol server now accepts a join-admission hook with the
  connecting remote IP address, and the runtime host wires `/ban`, `/ban-ip`,
  `/pardon`, `/pardon-ip`, and `/whitelist` state into that hook. New joins are
  rejected before being marked joined when the player name is banned, the
  remote IP is banned, or whitelist mode is enabled and the name is absent; live
  command changes refresh the active host admission policy immediately.
- Added saved-server multiplayer flow parity. The Multiplayer screen now loads
  and persists `servers.json`, shows selectable saved servers, supports
  double-click/Enter joining through the typed protocol client, and provides
  Add/Edit/Delete controls around the existing Direct Connect and Host World
  paths. Successful joins update `lastServer`, saved-list joins refresh the
  entry's last-connected timestamp, and Direct Connect now pre-fills from the
  remembered server address while accepting host/port input through one shared
  parser.
- Added live multiplayer dimension metadata switching. Multiplayer clients now
  rebuild their local world when a host `world_state` changes dimension, clear
  stale remote player/entity/item snapshots from the old dimension, keep
  host-authored spawn/game-mode/difficulty/permission metadata, refresh atlases
  and render settings, and restart terrain loading in the new dimension before
  replayed block/entity/inventory snapshots are applied. Hosts now reset
  cached player/block/entity/inventory world-state replay snapshots when the
  hosted dimension changes, reseed the fresh host/world snapshot, and broadcast
  that new world-state immediately so old-dimension deltas are not replayed to
  late joiners or freshly switched clients. Initial multiplayer joins also
  create the client world through the dimension-specific Release generator id
  instead of relying on default Overworld identity.
- Added multiplayer keep-alive/session-timeout parity. The typed protocol now
  has a `keep_alive` message, the server runs a joined-client keep-alive loop,
  clients automatically echo the exact probe id from their protocol reader, and
  unanswered probes disconnect through the existing remove-client path with a
  timed-out reason. Stale sockets therefore remove cached player/inventory
  state and notify remaining clients instead of leaving dead remote players
  visible until some later send/read failure happens.
- Expanded multiplayer initial world-state parity. Typed `WorldState` snapshots
  now carry host-authoritative spawn coordinates, game mode, difficulty,
  hardcore flag, allow-cheats flag, dimension id, and max-player count in
  addition to seed/time/weather and cached runtime deltas. Hosts configure that
  metadata before accepting clients and refresh it before world-state
  broadcasts; clients store it from the initial sync, build their multiplayer
  world in the host dimension, spawn at the host spawn point, and apply host
  game-mode/difficulty/permission metadata on join and later world-state
  updates instead of falling back to local settings/default Overworld state.
- Added Release-era server-list ping compatibility to the custom multiplayer
  server. Incoming TCP connections that start with the old `0xFE` ping byte are
  now answered before the JSON handshake with a legacy `0xFF` UTF-16BE
  kick-packet string in the section-sign-delimited `motd / online / max` shape used by Beta 1.8 through
  Release 1.3-era clients. Normal CraftZero clients still receive the existing
  typed hello/world-state handshake, with a short first-byte peek preserving
  early JSON join data when it arrives before the server hello.
- Tightened multiplayer session validation. Client join messages now carry the
  CraftZero protocol version, and the socket server validates version, username
  shape, duplicate active usernames, and the configured max-player count before
  marking a connection joined or notifying the runtime. Rejected clients receive
  explicit disconnect reasons such as outdated client, invalid username, full
  server, or duplicate username, and only accepted joins can trigger system chat
  or later leave broadcasts.
- Expanded multiplayer player-state parity. The typed protocol now carries
  richer player state alongside pose: on-ground/sneak state, health, selected
  hotbar slot, held item id/count/damage, and game mode. Hosted worlds now
  broadcast the host player's own state as a cached protocol player snapshot,
  clients send the same metadata with their movement updates, initial
  world-state replay materializes cached player snapshots for late joiners, and
  remote player views apply the received health/held-item/sneak/game-mode
  metadata instead of rendering anonymous pose-only players. Server-side join
  and leave lifecycle events now produce normal system chat, and clients no
  longer open a disconnect screen when some other remote player leaves.
- Expanded UI/inventory quick-move parity. Chest/chest-minecart,
  dispenser, furnace, brewing-stand, enchanting-table, crafting-table, and
  player-inventory shift-click handlers now share a slot-filter-aware
  quick-move path for player-to-container and main/hotbar transfers. The shared
  path merges into existing compatible stacks before filling empty slots,
  respects per-slot max sizes such as brewing bottles and the enchanting input,
  keeps partial remainders in the source slot, clears the source only when the
  stack is fully consumed, and lets container dirty/update hooks run when
  tile-backed slots receive quick-moved items.
- Tightened audio resource-pack switching behavior. The active resource manager
  now exposes a revision counter that changes when the active manager or
  selected pack changes, and the OpenAL sink watches that revision before
  playback. When sound resources change, the sink stops old active sources,
  deletes decoded buffer caches, clears stale missing-sound markers, and lets
  later world/UI/music/record cues resolve against the newly selected pack
  instead of staying silent or continuing to use old decoded audio.
- Tightened rendering and particle settings parity. The world particle renderer
  now uses Release-style particle-density ratios for Video Settings (`All`,
  `Decreased`, and `Minimal`), rendering roughly two thirds of particles for
  Decreased and one tenth for Minimal instead of the previous half/third
  filter. Rendered particles also sample local sky/block light and use that
  brightness directly during the particle pass, while obvious self-lit fire,
  lava, drip-lava, and explosion particles remain bright.
- Expanded physics/math ray-hit parity. Entities now expose the Release-era
  `0.1` collision border used for ray picking, and `Raycast` owns the shared
  entity pick-box expansion plus slab-based AABB intersection helper. Player
  hit checks for arrows, fireballs, Ender Pearls, thrown eggs/snowballs, and
  splash potions now use that same math, and the old duplicate
  `ArrowEntityRay` helper was removed.
- Re-aligned natural mob spawning cadence with the Release-era fixed tick pass.
  `MobSpawner` no longer keeps its own 10-tick scan throttle, so hostile,
  passive, and water-creature natural spawn sweeps are considered every fixed
  world tick while the existing category caps, eligible-chunk sweep, pack
  attempts, light checks, biome/dimension tables, and player/world-spawn
  exclusions still gate actual placement.
- Tightened block/fluid interaction parity. Bucket-placed water and lava now
  use the same fluid-displacement gate as flowing fluids instead of the narrow
  generic block-placement replaceable list, so source buckets can clear
  source-era non-solid targets such as torches, plants, wires, rails, and other
  fluid-displaceable blocks while still refusing doors, signs, ladders, solid
  blocks, Nether water placement, and existing water/lava mixing rules through
  the normal fluid path.
- Tightened redstone/mechanism scheduled tick behavior. The shared world tick
  queue now lets earlier wakeups preempt an existing pending tick for the same
  block/type instead of preserving a stale slower due time. Live dust,
  repeater, piston, dispenser, button, pressure-plate, detector-rail, and
  powered-rail updates can now respond to newly earlier neighbor events without
  waiting behind an older delayed schedule; later duplicate schedules are still
  ignored so the queue remains deduplicated.
- Expanded save/load dimension persistence. `SaveManager` now keeps a
  world-global filled-map color cache, refreshes it from `level.json` on load,
  merges active-world map colors into every save snapshot, writes that merged
  map set back to the root level file, and reapplies it when a dimension
  runtime sidecar is loaded. Dimension transfers also construct the next
  `World` with the explicit target dimension generator id, so Nether/End
  reloads no longer depend on the Overworld generator id being interpreted as a
  dimension fallback.
- Tightened multiplayer host-authority behavior after the broad entity sync
  pass. Client multiplayer worlds no longer run local dropped-item physics,
  hazard removal, despawn, or merge logic for host-mirrored item stacks; those
  stacks now move, change count, and disappear only through host entity-update
  packets. World-state sync also carries the host weather state alongside time,
  and direct-connect clients apply that weather on join and every subsequent
  world-state packet instead of rolling their own local rain/clear transitions.
- Expanded multiplayer parity. The typed protocol cache now removes stale
  entity snapshot entries when removal updates are broadcast, so late joiners
  no longer inherit dead entity state. Host-side entity sync now covers the
  main visible runtime entity set beyond mobs: arrows, fireballs, eyes of
  ender, ender pearls, splash potions, thrown eggs/snowballs, XP orbs, falling
     blocks, minecarts, boats, paintings, and primed TNT. Clients can materialize
  those remote entity types from update payloads and apply type-specific state
  such as arrow critical flags, furnace-cart fuel/push vectors, TNT fuse time,
  falling-block contents, and painting art/facing. Dropped item stacks now ride
  the same entity-update stream with stable host IDs, live removal broadcasts,
  late-join snapshot seeding, and non-pickup client mirrors so visible item
  stacks stay host-authoritative instead of being locally granted by clients.
- Expanded UI, inventory, and settings parity. Runtime input now shares one
  binding helper for keyboard and mouse-backed options, so player controls,
  global shortcuts, and control-menu labels use the same negative mouse-button
  encoding as `options.txt`. The configured Inventory binding now closes
  crafting table, chest, furnace, dispenser, brewing stand, and enchanting
  table GUIs instead of those screens hardcoding `E`, while keeping Escape as a
  universal close action. The listed Screenshot control now captures the next
  rendered frame into `screenshots/*.png`, and the Smooth Camera control toggles
  a cinematic mouse-look smoothing mode with chat feedback.
- Expanded audio parity. World sound events now distinguish control events,
  record playback, and music playback, giving the OpenAL sink proper long-lived
  channels instead of treating every cue as a disposable one-shot. Jukeboxes now
  emit a stop control before replay, eject, or block-removal drops so the
  currently playing record at that block stops cleanly. Active record sources
  keep their block position and update gain as the listener moves through the
  same Release-style audible radius used by dispatch culling, while background
  music is listener-relative and replaces any previous music cue instead of
  stacking. Player damage that actually applies now queues the legacy
  `random.hurt` cue at the player, bringing local player hurt feedback into the
  same world sound event path as mobs, blocks, records, and UI clicks.
- Expanded rendering, animation, and particle parity. Status-effect
  `MOB_SPELL` particles now render through the same animated spell sprite path
  as potion and instant-spell particles instead of falling back to generic
  smoke while keeping their packed status color tint. The player now tracks a
  short hurt-flash render timer when damage is actually applied, so third-person
  player skin and armor use the same red damage feedback language as mobs
  without tinting the held item. Third-person player rendering also gets the
  legacy crossed fire texture overlay while burning, sized from the player
  model dimensions and sharing the terrain fire sprite used by mob fire
  overlays.
- Expanded projectile physics and collision math parity. Arrows, thrown
  eggs/snowballs, splash potions, and fireballs now use short shooter collision
  grace windows instead of excluding their entity shooter forever, so returning
  or redirected projectiles can collide with the source after the launch-safe
  period. Player-fired arrows now mirror the same grace-window behavior instead
  of permanently ignoring the player, and Ender pearls stop ignoring their owner
  after the initial launch window. Deflected fireballs also keep only a brief
  deflector grace window, then regain normal player collision while preserving
  player credit for returned Ghast fireball kills. This broadens the shared
  ray/AABB projectile path without changing block collision order.
- Expanded mob AI and combat parity. Living entities now remember the last
  damage source position even when the source is a point hit instead of an
  entity, so passive panic goals flee away from player melee/projectile impact
  positions instead of choosing a random panic vector. Zero-damage thrown-item
  hits keep the same damage-source memory as damaging hits. Bright-light
  spiders now become provoked by player melee and player-owned arrows, closing
  the old gap where player point damage did not count as an entity source.
  Custom ranged hostiles now honor generic living retaliation targets:
  Blazes can melee or run their fireball burst against remembered living
  attackers, and Ghasts can charge and fire at remembered living attackers
  before falling back to normal player targeting. This makes snow-golem,
  tamed-wolf, projectile, and mob-vs-mob retaliation paths work through the
  same combat memory instead of only the generic melee/ranged goals.
- Expanded redstone mechanism wake-up coverage for the mechanisms pass.
  Scheduler fan-out now treats the block space above quasi-connected pistons and
  dispensers as a shared power query, wakes dispensers through the same
  quasi-connectivity path as pistons, and follows nearby opaque solid-block
  relays so live dust/lever changes beside those relays do not leave pistons or
  dispensers stuck until another neighbor update happens. Minecart rail motion
  now also applies the legacy horizontal speed cap to stored velocity before the
  rail movement step and again after friction, slope energy adjustment,
  rail-cell realignment, powered-rail boosts, and furnace-cart force, so the cap
  governs the cart state instead of only clamping a single tick's displacement.
- Expanded block/fluid interaction parity for lily pads. Player use now routes
  lily pads through the source-fluid raycast used by buckets and bottles, and
  `World` exposes a placement helper that only lands pads above source water
  while still enforcing normal placement collision and support checks. This
  lets pads target water directly instead of depending on adjacent-air block
  placement. Bucket-placed source fluids now also enter the lava/water mixing
  path before raw source placement: water buckets harden eligible lava into
  obsidian or cobblestone, lava buckets harden water into cobblestone, mix fizz
  feedback is emitted, and solid mix results respect player collision instead
  of silently overwriting the fluid.
- Expanded the custom multiplayer layer's parity surface. Initial world-state
  sync now carries cached block, entity, and inventory deltas for late joiners;
  clients replay those snapshots through the existing runtime listener path;
  server-side block/entity/inventory broadcasts retain authoritative state; and
  clients can now send entity and inventory updates directly. World block
  changes now flow through a central `World` block-change listener, so local
  host changes, client block edits, suppressed multi-block placements, and
  server-driven block ticks can be propagated without hand-wiring every
  interaction path. Multiplayer clients no longer run local block ticks, keeping
  environmental block simulation server-owned. Starting a host now seeds the
  protocol snapshot cache from loaded saved/modified chunks plus the host
  inventory, so late joiners receive pre-existing world edits rather than only
  future deltas. Multiplayer inventory updates also apply to the targeted local
  client using a stable
  hotbar/main/crafting/armor/cursor slot order, and local inventory mutations
  are diffed once per tick into protocol updates without echoing remote slot
  applies back to the server, while entity compatibility messages preserve
  metadata instead of dropping it during conversion. Remote player poses are
  now materialized into render-only player views, and server-owned mob/entity
  state is broadcast, snapshot-seeded, spawned, updated, and removed on clients
  through the same protocol cache instead of remaining invisible outside the
  host simulation. Player-state packets now also carry on-ground/sneak state,
  health, selected hotbar slot, held item id/count/damage, and game mode. The
  host broadcasts its own player state into the same typed snapshot cache,
  clients replay cached player snapshots from initial world-state sync, and
  remote render-only players apply received held-item/health/sneak metadata.
  Server join/leave lifecycle events now surface as system chat, and clients
  distinguish their own disconnect from another player leaving so ordinary
  remote departures no longer kick them back to a disconnect screen. The server
  also recognizes Release-era `0xFE` server-list pings on the game port and
  responds with the old section-sign-delimited UTF-16BE kick-packet format, so
  legacy multiplayer lists can query the hosted world without attempting the
  CraftZero JSON session handshake. The in-game Multiplayer screen now also
  persists and displays saved servers from `servers.json`, supports
  add/edit/delete plus double-click/Enter join, pings each saved entry through
  that same legacy server-list path for MOTD/player-count status, refreshes
  successful saved-list joins with a last-connected timestamp, and uses the
  existing `lastServer` option to pre-fill Direct Connect. Join/session
  acceptance now also validates
  protocol version, old username shape, duplicate active usernames, and max
  player count before runtime join notifications fire, and hosted access
  control now rejects banned names, banned remote IPs, and non-whitelisted
  names before marking the connection joined. Multiplayer slash commands from
  clients are now consumed as server commands instead of being rebroadcast as
  raw chat; the host executes them with remote sender/operator context and sends
  feedback or targeted player effects back through command-action packets.
  Rejected clients therefore no longer briefly look joined to chat/listeners.
  Protocol reads now use a bounded line reader and outbound flushes reject
  oversized packets before they hit the socket. Typed decode rejects malformed
  numeric/boolean fields rather than silently coercing bad data to defaults,
  and `data` maps are capped and string-only at the protocol boundary. The
  legacy `NetworkMessage` facade now normalizes allowed message types,
  sanitizes payload entries, treats malformed JSON as absent, and drops invalid
  bridge sends cleanly so local gameplay code cannot crash on a malformed
  facade packet.
  Old binary Release-era login and handshake packet IDs are now detected before
  the JSON session path; those clients receive a proper `0xFF` UTF-16BE kick
  packet with an explicit CraftZero protocol-required reason instead of a silent
  parse failure. The shared legacy kick writer also sanitizes the server-list
  MOTD/status response, and malformed custom JSON packets now receive a `Bad
  packet` disconnect before the socket is cleaned up. Initial sync now sends
  the admitted roster first, then a metadata-only world-state packet, then
  streams cached player/block/entity/inventory state as ordinary bounded
  protocol packets. Time/weather world-state broadcasts likewise carry metadata
  only, so large host snapshot caches no longer ride inside one oversized
  `world_state` line. The stream now ends with an `initial_sync_complete` world
  event carrying the streamed snapshot counts, and the high-level multiplayer
  client waits for that barrier before reporting connect completion. Clients
  now also defer host block-update packets whose target chunks are not loaded
  yet, retry those deferred updates during terrain loading and normal network
  drains, and clear the deferred queue on unload or dimension switches, so
  streamed saved-world edits no longer vanish if they beat local chunk loading.
  Client player-state packets now carry the real raw movement buttons instead
  of only pose/on-ground/sneak values, and the host records those sanitized
  `input.*` bits in player-state snapshots for authority and remote consumers.
  Player-state snapshots now also carry bounded `remote.*` sprint/use/block/bow
  state, and action/block packets trigger remote swing/use animation hooks so
  other peers see attacks, entity uses, projectile/item releases, and block
  edits as visible third-person actions instead of pose-only updates.
- Initial multiplayer world-state sync now includes host-authored world
  metadata: spawn coordinates, game mode, difficulty, hardcore, allow-cheats,
  dimension, and max-player count. Hosted clients use that metadata to create
  the correct dimension, set the shared spawn, place the joining player at the
  host spawn point, and keep runtime game mode/difficulty/permission state in
  step with later world-state broadcasts instead of deriving those values from
  local client defaults. Later world-state dimension changes now rebuild the
  client world in the host dimension, clear stale remote snapshots from the old
  dimension, and resume terrain loading before replayed runtime deltas apply.
  Hosts reset cached world-state replay snapshots across dimension changes and
  reseed the new host snapshot before broadcasting, preventing old-dimension
  player/block/entity/inventory deltas from repopulating the fresh dimension.
  Live and cached player, block, entity, inventory, client-action, world-sound,
  world-particle, and lightning-event packets now also carry dimension stamps,
  and clients drop stamped packets that target a different active dimension.
  The host facade mirrors that dimension stamp into cached remote-player state
  so host-authoritative combat, item/XP pickup, hazard, command, and entity
  action checks no longer trust old-dimension remote poses after a transfer.
  The typed protocol now also carries explicit keep-alive probes; clients echo
  them automatically, and timed-out joined clients are removed through the
  normal disconnect path so stale sessions do not linger in player lists,
  remote render views, or inventory snapshots.
- Expanded player attachment persistence in the save/load path. Player saves
  now record the saved entity id and mount type for active rideable minecart,
  boat, or saddled-pig mounts, validate that the reference resolves to the
  correct saved vehicle/mob payload, and restore the mount after entities are
  rebuilt so quitting mid-ride does not silently detach the player or leave a
  stale vehicle relationship behind.
- Expanded player movement persistence in the save/load path. New saves now
  carry player motion, on-ground state, fall-start height, and active falling
  state alongside position/rotation, validate those fields when present, and
  restore them after load so knockback, airborne falls, swimming drift, and
  slippery-block motion do not collapse to a stationary player on reload.
  Older saves without the movement payload remain tolerated.
- Expanded active sleep persistence in the save/load path. Format 10 saves now
  record an in-bed player's foot/head bed coordinates plus the pre-sleep return
  pose, validate that the saved bed parts are adjacent and in world bounds,
  wake the player safely on load, and strip the temporary bed-occupied bit from
  saved chunk snapshots so quitting mid-sleep no longer leaves a permanently
  occupied bed or a player stranded in bed pose after reload.
- Hardened block/tile/fluid state transitions. Metadata-only updates for
  tile-backed blocks now keep their existing tile entity, real tile-type
  replacements install the correct new tile instead of reusing stale inventory
  state, bucket-placed water/lava now uses the same displacement and fluid-mix
  hooks as flowing fluids, adjacent ice wakes on block/light changes, and
  silent vine/leaf metadata fixes mark their chunk dirty for save/render
  refreshes.
- Corrected Release-era suspended-water ambient particle rules. Water display
  ticks now use the old 1-in-10 roll, spawn motes at the source block's
  `y + randomFloat` height, and only emit for source/still water or falling
  water metadata; ordinary side-flowing water levels no longer create extra
  suspended visual noise.
- Corrected Release-era critical-arrow trail motion. Critical arrows still emit
  the four old `crit` particles along the in-flight segment, but their spawn
  motion now uses the source arrow arguments `-motionX`, `-motionY + 0.2`, and
  `-motionZ` instead of an extra-damped reverse drift.
- Added Release-era lava-mixing feedback. Lava hardening against water and lava
  displacing non-air blocks now plays the old `random.fizz` cue and emits eight
  `largesmoke` particles at `y + 1.2`, so obsidian/cobblestone/stone conversion
  is no longer visually silent.
- Corrected powered furnace minecart exhaust identity. Fueled furnace carts now
  emit the old intermittent `largesmoke` puff at `y + 0.8` instead of ordinary
  smoke while preserving the source 1-in-4 display tick roll.
- Corrected Release-era love-mode heart placement. Periodic animal love hearts
  now spawn from the source body-height random box (`y + 0.5 + rand * height`)
  with the same tiny Gaussian emitter arguments used by breeding/birth bursts,
  instead of popping from a fixed point above the head.
- Corrected Release-era wolf tame particle placement. Successful tame hearts
  and failed tame smoke now use the old seven-particle body-height random box
  with tiny Gaussian emitter arguments instead of a generic centered burst.
- Corrected Nether water-bucket vapor particles to the Release-era identity.
  Water buckets used in the Nether already fizzed and stayed unplaced, but the
  vapor cloud now emits eight old `largesmoke` particles instead of ordinary
  smoke.
- Added Release-era eating item-icon crumbs. Food use now emits old
  `iconcrack_<item>`-style `ITEM_CRACK` particles from the hand/mouth path on
  the first eat tick and as the 16-particle finish burst, using the consumed
  item icon. Milk and drinkable potions keep the source-style drink sound path
  without invented crumb particles.
- Added Release-era generic entity water-entry bursts. Non-player entities now
  initialize their water particle state on the first physics pass, then emit the
  old source-shaped `bubble` and `splash` burst only when later crossing from
  dry into water. The shared water-entry helper now emits all bubbles before
  splashes at `floor(entityY) + 1`, and custom projectile paths for arrows,
  thrown items, fireballs, and Eyes of Ender use the same transition guard.
- Added Release-era sprinting terrain chips. Dry sprinting players now emit the
  old `tilecrack`-style `BLOCK_CRACK` particle from the block underfoot every
  sprint tick, with source-style reverse-motion kick, while underwater sprint
  state stays quiet.
- Corrected normal movement particle parity. Ordinary walking now keeps the
  Release material step sound cadence without invented block-dust or footprint
  particles, and steady player swimming no longer emits throttled movement
  bubbles. Dry sprinting terrain chips and dry-to-water bubble/splash entry
  bursts remain intact.
- Corrected Release-era block digging particle construction. Block-destroy
  4x4x4 fragments and block-hit chips now use the old `EntityFX`-style
  randomized normalized motion, upward bias, lifetime roll, and size jitter
  instead of fixed outward pushes. Hit chips also spawn exactly `0.1` blocks
  outside the clicked render bounds, so partial blocks like snow layers emit
  chips from their visible surface instead of the full cube. All digging chips
  now use the old side-0 block texture lookup, mapped to CraftZero's bottom
  face, rather than rendering with the clicked face.
- Corrected Release-era fire large-smoke placement. Ambient fire display ticks
  now emit the old three top puffs when the fire is supported below, or two
  edge puffs per flammable side when it is side-supported, instead of one
  generic center puff.
- Added Release-era redstone-ore activation sparkles. Clicking, activating,
  stepping on, or item-contacting redstone ore now emits the old exposed-face
  `reddust` sparkle burst immediately while lighting or refreshing the glowing
  ore timer.
- Corrected Release-era red-dust color and placement parity. Powered wire
  ambient dust now uses the old center +/-0.1 block jitter, `y + 1/16`
  placement, and source power RGB curve, while zero-argument torch, repeater,
  and redstone-ore `reddust` particles render as clean red instead of max-power
  wire tint. Glowing redstone ore ambient display ticks now emit exposed-face
  sparkle bursts instead of a single wire-style particle.
- Corrected Release-era lava particle smoke handoff. Ambient lava sparks now
  shed ordinary `smoke` particles from their pre-move position with their
  current motion while alive, using the old age-fading chance, instead of
  silently disappearing after the lava sparkle arc.
- Corrected splash-potion spell cloud shape. Potion impacts now use the old
  auxiliary-effect radial roll: spell particles spawn at `impact + radial *
  0.1`, fixed `y + 0.3`, move outward with radial-scaled horizontal motion, and
  receive the source 75%-100% per-particle color brightness instead of a flat,
  tightly capped colored cloud.
- Corrected splash-potion bottle-shard placement. Potion impacts now emit the
  old auxiliary-effect 2002 item-crack chips at the exact impact coordinate
  with Gaussian horizontal motion and a small upward random velocity, instead
  of using the generic random item-break cube around the impact point.
- Corrected mob-spawner successful-spawn particle burst shape. Successful cage
  spawns now use the old auxiliary-effect 2004 pattern: 20 paired `smoke` and
  `flame` samples in the 2x2x2 cube centered on the spawner, with zero motion,
  instead of a generic upward radial 12/12 burst.
- Corrected failed Eye of Ender shatter particles. The failed expiration branch
  now mirrors the old auxiliary-effect 2003 shape: eight eye item-crack chips
  at the rounded shatter center plus 80 portal particles in two radius-5 inward
  rings, instead of the previous tiny generic portal puff.
- Added Release-era dispenser smoke feedback. Successful non-empty dispenser
  activations now emit the old auxiliary-effect 2000 smoke: 10 directional
  `smoke` particles from the dispenser mouth with source-style side/vertical
  jitter and mostly outward/downward motion. Empty activations keep the old
  click-only behavior and stay particle-silent.
- Added Release-era living-entity drowning bubbles. Non-water-breathing living
  entities now emit the old eight `bubble` particles around their body on the
  exact tick they take drowning damage, while underwater breathers and Water
  Breathing entities remain quiet.
- Added Release-era fishing-bobber bite particles. When a hook becomes
  catchable in water it now emits the source-shaped six `bubble` plus six
  `splash` particles at the bobber waterline using the bobber's current motion,
  instead of a generic splash-only burst.
- Added Release-era End portal frame eye-insertion smoke. Placing an Eye of
  Ender into a frame now emits the old 16-particle `smoke` burst in the
  5/16..11/16 inset box at `y + 0.8125` before the portal-completion scan.
- Added Release-era powered repeater display particles. Lit repeaters now emit
  old `reddust` sparkles at either the fixed output torch or the
  delay-dependent rear torch offset, while unpowered repeaters stay quiet.
- Added Release-era Blaze ambient smoke. Living Blazes now emit the old two
  `largesmoke` body particles every tick from source-style width/height samples
  instead of only showing particles during charge and shot events.
- Added the Release-era underwater projectile bubble trails for arrows and
  thrown item projectiles. Arrows, snowballs, and eggs now emit the old
  four-bubble trail from a quarter-step behind the projectile after moving
  through water, then use the source 0.8 underwater drag before gravity.
- Added Release-era fireball flight trails. Fireballs now emit the old
  per-tick `smoke` particle after successful open-air movement and switch on
  the source four-bubble water trail while still emitting smoke when their
  current sample is underwater.
- Added the missing Release-era ambient block display particles for common
  visual blocks. Ordinary torches now emit paired `smoke` and `flame` particles
  at the source metadata-specific flame point, active redstone torches emit the
  old single jittered `reddust` sparkle while inactive redstone torches stay
  quiet, and brewing stands plus End portal blocks now emit their old ambient
  smoke display ticks.
- Added a high-impact Release-era particle slice. Block hits now emit a
  single textured crack chip, successful block breaks emit the old 4x4x4
  fragment burst, and sprinting terrain chips now preserve terrain-atlas block
  payload data. The particle payload preserves block type and metadata, while
  digging-chip texture lookup follows the old side-0/bottom-face `EntityDiggingFX`
  path instead of the clicked face. Player water entry now emits splash and bubble
  bursts, ordinary swimming movement stays particle-quiet like the source, and
  the renderer has a dedicated bubble sprite plus terrain-atlas mesh caching
  for block particles. This pass also adds old `snowballpoof` clouds for
  snowball/egg impacts, item-atlas crack particles for splash-potion impacts
  plus depleted durable items, and common living-entity deaths now emit
  bounded old `explode` poofs instead of simply disappearing.
- Restored Release-era zero-damage thrown item hit reactions. Eggs and
  snowballs that hit ordinary living entities now preserve the visible hurt
  animation, hit source, and projectile knockback without reducing health,
  while snowballs still keep their special Blaze damage path.
- Added the next high-visibility particle event slice. Dropped-item collection
  now emits a short item-atlas `take` style pickup particle only after inventory
  transfer succeeds, slime-family landings emit dedicated `slime` particles at
  the old eight-particles-per-size density while rendering with the slimeball
  item texture, and powered enchanting tables now
  emit old `enchantmenttable` particles from valid bookshelves toward the table
  using the same two-block ring and air-gap rules as enchantment power.
- Added the underwater suspended-particle and physical-particle collision
  slice. Water blocks now emit subtle suspended particles during ambient display
  ticks, rendered with a pale underwater tint from the shared particle atlas.
  Physical particles such as block crack/dust, item shards, drips, splash,
  smoke, flame, weather streaks, old `lava` particles, and `reddust` now resolve
  against loaded block collision boxes instead of freely drifting through
  terrain, while non-physical paths such as item pickup arcs, portal shimmer,
  and enchanting glyphs remain unobstructed.
- Added Release-era enchanted-hit magic crit feedback. Successful player melee
  hits that gain extra damage from Sharpness, Smite, or Bane of Arthropods now
  start the old short blue magic-crit entity emitter around the target after
  damage applies, separate from ordinary falling critical-hit particles. The
  renderer now uses a dedicated crit atlas sprite for both normal and magic
  crits, with a distinct enchanted-hit tint for magic crits.
- Corrected crit particle source-index and motion parity. Ordinary crits and
  magic crits now render from old particle atlas cell 65 instead of the
  incorrect first-row placeholder, with a visible tintable sprite restored in
  `particles.png`. Crit particles now inherit scaled incoming motion, grow
  through the source-style 32x fast scale ramp, use heavy 0.7 drag, fall under
  old 0.02 gravity, and fade their green/blue channels over their short
  lifetime; normal crit children also carry randomized gray base tint values.
- Corrected player critical-hit and enchanted-hit emitter timing. Accepted
  melee crits now spawn the source-shaped immediate child-particle pass plus
  two queued 20 Hz follow-up passes, each using the old 16 random unit-sphere
  attempts around the struck entity instead of a one-shot generic seven-particle
  puff.
- Added Release-era low-depth `depthsuspend` ambience. Overworld-only air
  samples below Y=17 now emit subtle gray suspended particles near the player
  without forcing chunk generation, restoring the old bedrock/void-fog visual
  cue separately from water suspension. The renderer reuses the suspended
  particle atlas cell with a darker void-fog tint.
- Removed the incorrect footstep/footprint particle feedback from the live
  walking path. Material footsteps still play their block-specific step sound,
  but ordinary walking no longer leaves terrain dust or ground-aligned
  `textures/misc/footprint.png` particles because the Release source does not
  emit those during normal movement.
- Added colored splash-potion spell impact particles. Splash potions still
  shatter into potion item-crack shards, but now also emit the old packed-RGB
  spell cloud at the impact point; healing and harming splashes use the
  instant-spell particle type while ordinary potion effects use the normal
  spell particle type. Potion item overlays and splash particles now share the
  same Release-era visual color mapping.
- Added Release-era mycelium display particles and corrected old thrown-item
  impact poofs. Mycelium random display ticks now emit occasional purple
  `townaura` spores above the block instead of staying visually inert, and
  snowballs/eggs now emit the old `snowballpoof` impact cloud instead of the
  later item-atlas crack behavior. Splash potions keep their separate
  item-crack bottle shatter.
- Corrected `snowballpoof` rendering and motion. Thrown snowball and egg
  impacts now display the old snowball item-icon chip instead of falling back
  to generic smoke, and the poof particles use falling item-particle gravity,
  `0.98` drag, block collision, and constant sprite size.
- Corrected item-icon crack particle physics. Potion bottle shards and
  depleted-item crack chips now share the old falling item-particle update:
  per-tick gravity, `0.98` drag, constant sprite scale, and stepped collision
  against loaded block shapes so fast visual shards do not tunnel through the
  block they visibly hit.
- Corrected terrain and slime fragment particle physics. Block-break fragments,
  block-hit chips, sprinting terrain chips, and slime landing shards now use the
  same Release-era fragment update as item chips: full per-tick movement,
  `0.04` gravity, `0.98` drag, constant sprite size, ground horizontal damping,
  and stepped loaded-block collision instead of slow generic particle drift.
- Split the Release-era `largeexplode` particle from true `hugeexplosion`
  blasts. Mooshroom shearing and small power-1.5 fireball explosions now use
  the smaller animated `largeexplode` puff, while TNT, creepers, beds, End
  crystals, and other power-2-or-larger explosions keep the larger
  `hugeexplosion` burst.
- Corrected destructive explosion debris particles. The shared `World.explode`
  path now mirrors the old blast visual pairing for destroyed blocks: one
  outward-moving `explode` flash spawns halfway between the blast center and
  the sampled block point, and one ordinary `smoke` particle spawns at that
  sampled block point using the same source-shaped velocity falloff. This
  applies to TNT, creepers, beds, fireballs, End crystals, and other callers
  routed through the shared explosion path, while the central burst now follows
  the old `hugeexplosion` versus `largeexplode` size gate.
- Corrected old mob/slime particle identities. Common mob deaths now emit the
  small `explode` poof particle instead of furnace-style smoke, and
  slime-family landing feedback now uses the dedicated `slime` particle type
  while retaining the slimeball item texture and eight-particles-per-size
  density.
- Added Release-era fire `largesmoke` display particles. Ambient fire block
  ticks now spawn the larger old smoke particle above the flame while furnace,
  redstone burnout, and explosion debris continue using the ordinary smoke
  particle. Furnace minecart exhaust now uses its own old `largesmoke` path.
- Corrected two more old particle identities. Ambient lava display ticks now
  emit the Release-era `lava` particle instead of the invented `LAVA_POP`
  identity, and snow-golem construction now emits the old 120-particle
  `snowshovel` burst from the consumed pumpkin/snow stack.
- Corrected the powered-redstone particle identity to the Release-era
  `reddust` particle. Powered wire and glowing redstone ore keep their existing
  ambient display behavior and tint scaling, but now emit through the old
  particle name instead of the modernized `REDSTONE_DUST` enum.
- Corrected the powered-enchanting-table particle identity to the Release-era
  `enchantmenttable` particle. The bookshelf-to-table path and alternate-font
  glyph atlas rendering remain intact, but world particles no longer use the
  invented `ENCHANTMENT_GLYPH` identity.
- Corrected item-pickup and enchanting-table particle interpolation. Dropped
  item pickup now uses the old three-tick squared pull toward the collector
  instead of a slower constant-velocity drift, while enchanting-table particles
  now spawn from the table top with source-style 30-39 tick lifetime, 0.2-0.7
  scale, 1-26 alternate-font glyph selection, pale blue-white tint, and the
  old target-to-table curve with quartic vertical sag.
- Polished the high-visibility old particle rendering paths for `reddust` and
  `lava`. Powered redstone dust now uses the old first-row animated particle
  frames and fast scale ramp instead of behaving like a static smoke sprite,
  while ambient lava sparks render from the old lava particle atlas slot and
  arc back down under particle gravity after their initial pop. The bundled
  particle sheet now includes a visible lava spark in that historical slot so
  the renderer does not address an empty cell.
- Corrected splash-potion spell particle rendering and motion. Regular splash
  potion clouds now use the old `EntitySpellParticleFX` 128-135 atlas sequence,
  instant healing/harming clouds use the old 144-151 instant-spell sequence,
  and both gain the source-style slight upward acceleration/0.96 drag instead
  of rendering as colored smoke. The packed potion tint and `mobSpell` aura
  path remain separate, matching the old aura particle's smoke-cell basis.
- Corrected portal particle source behavior across Nether portal ambience,
  Enderman ambient/teleport feedback, Ender Pearl impacts, Eye of Ender trails
  and shatters, dragon-egg teleports, and generic portal bursts. World-spawned
  portal particles now get the old randomized 40-49 tick lifetime and fixed
  random first-row particle cell instead of age-animating through the row, then
  use the original source-position displacement curve, upward offset, and
  quadratic scale ramp from `EntityPortalFX`.
- Restored the Enderman projectile-dodge hook for zero-damage thrown items.
  Snowballs and eggs still give ordinary mobs the old no-damage hurt reaction,
  but Endermen now receive the projectile source first so they teleport away
  without accepting hurt state or player-credit damage.
- Corrected heart and note particle motion/scale parity. Love-mode, breeding,
  birth, and tame-success hearts now ignore emitter-side random drift like the
  old `EntityHeartFX`, use the source 16-tick lifetime, pop upward under the
  0.86 drag curve, and ramp from tiny to full size through the 32x scale
  clamp. Note-block notes now keep their pitch-based color but use the old
  six-tick `EntityNoteFX` upward pop, 0.66 drag, and fast scale ramp instead
  of generic particle drift/fading.
- Corrected ambient micro-particle behavior for suspended water, depth-suspend,
  and mycelium town-aura particles. Suspended water particles now spawn with
  the old slight downward offset, zero motion, source-shaped 16-80 tick
  lifetime, constant tiny scale, and expire immediately when their sample
  leaves water. Depth-suspend and town-aura particles now keep constant scale,
  use the old 20-100 tick aura lifetime shape, and apply no-clip aura damping
  instead of behaving like generic drifting/fading particles.
- Corrected status-effect `mobSpell` aura particle physics. Potion/combat
  effect specks keep their packed effect color, but now use the old aura path:
  randomized 20-100 tick lifetime, source-shaped random scale, input motion
  scaled by `0.02`, tick-based no-clip drift with `0.99` drag, and constant
  sprite size instead of generic smoke-style fading/acceleration.
- Corrected the old smoke-family particle animation and motion. Ordinary
  `smoke`, fire `largesmoke`, common mob-death `explode` poofs, and
  snow-golem `snowshovel` bursts now animate through the first-row particle
  atlas frames instead of staying on one static cell. Smoke and small explode
  particles gain the source-style slight upward acceleration and drag,
  snowshovel particles now fall with old gravity and 0.99 drag, and smoke/snow
  shovel particles use the old fast scale ramp instead of appearing fully grown
  on their first rendered tick.
- Corrected water `splash` particles to stop rendering as smoke. Existing
  player water-entry, boat wake, wolf shake, and fishing splash emitters now
  render through the old rain/splash particle atlas cell, with visible
  rain/splash sprites restored in `particles.png`, and splash particles now
  fall under the old rain-particle gravity instead of drifting like smoke.
- Corrected old `bubble` particle physics. Water-entry, drowning, fishing,
  and underwater projectile bubbles keep using the old particle atlas cell 32,
  but now gain only the source-style
  tiny upward acceleration, apply the heavier 0.85 water drag, and expire as
  soon as their particle position leaves a loaded water block instead of
  surviving in air like a generic transient particle.
- Corrected `flame` particle rendering to match the old particle effect path.
  Furnace, blaze, spawner, and other existing flame emitters now use the
  Release-era particle atlas cell 48 instead of a terrain fire-block quad,
  `particles.png` includes the missing tintable flame cell, and flame particles
  now use the source-style 0.96 drag plus quadratic scale shrink over their
  lifetime.
- Corrected animated particle family motion to use old per-tick source
  movement instead of frame-time-scaled drift. Smoke, large smoke, explosion
  poofs, spell/instant-spell clouds, splash drops, lava sparks, snow-shovel
  bursts, and flame particles now apply their source acceleration/gravity,
  move by full current motion once per tick, then apply the old drag and ground
  damping, so furnace smoke/flame, potion spell clouds, water splashes, lava
  pops, snow-golem bursts, mob-death poofs, and Blaze/spawner flames no longer
  hover at one-twentieth of the old visual speed.
- Corrected water/lava drip particle rendering and behavior. Underside fluid
  display ticks now spawn stationary old `EntityDropParticleFX`-style drips
  with zero initial motion and source-shaped random lifetimes; the particle
  hangs for the old 40-tick bob phase on atlas cell 113, switches to falling
  cell 112, and uses stronger drip gravity with 0.98 drag. Water drips now die
  into a splash particle when they hit loaded block collision, while lava drips
  keep the old warm-to-red color shift and switch to the ground/splat cell 114
  on impact. `particles.png` now includes visible tintable cells 112-114.
- Reworked the Options/Video Settings path toward Release 1.0 behavior.
  Render distance is back to the old Far/Normal/Short/Tiny button cycle and
  saves both the legacy option id and compatibility chunk value, while the
  runtime still clamps to the current safe 4-5 chunk engine window to avoid
  reintroducing the recent minimum-distance FPS/world-load regression.
  Release-era Video Settings now expose Graphics, Render Distance, Smooth
  Lighting, Performance, 3D Anaglyph, View Bobbing, GUI Scale, Advanced
  OpenGL, Brightness, Clouds, and Particles; later Fullscreen/VSync buttons
  are no longer shown in that Release-style menu, though their saved/runtime
  handling remains for F11/window behavior. Brightness uses Moody/Bright end
  labels, GUI scale cycles Auto/Small/Normal/Large, 3D Anaglyph now renders
  the world through a guarded red/cyan two-eye pass, Particles affects rendered
  particle density, and options round-trip coverage verifies the preset
  save/load path.
- Corrected Release-era generated-village size gating. Village origins now have
  to build more than two non-road components before they count for chunk
  placement, structure locating, or lake suppression, so tiny well/road-only
  attempts no longer appear as token villages or remove nearby lakes.
- Corrected Release-era minecart rail-band acquisition. Minecart physics now
  searches the current rail block and the block below, and only accepts a rail
  one block above when an adjacent ascending rail actually leads into it. Carts
  still climb slope-to-raised-rail transitions, but they no longer snap upward
  onto an unconnected raised rail band from below.
- Corrected Release-era mob-spawner entity-volume semantics. Hostile spawner
  mobs no longer need a solid floor directly below the selected spawn point,
  so cage mobs can spawn into clear air and fall as they did in vanilla, while
  non-water mobs now reject liquid inside their bounding box instead of
  appearing with water in their body volume.
- Corrected Overworld taiga tall-grass decorator metadata and RNG coupling.
  `WorldGenGrass`-style detail generation now always uses the Release 1.0
  long-grass metadata value `1` instead of taking a taiga-only random branch
  that could produce fern metadata and shift every later decorator draw in that
  origin stream.
- Corrected Release-era creeper fuse target routing. Creepers now honor the
  active living-entity target assigned by AI/restore paths before falling back
  to the local player, so they hiss, stop, chase, cool down, and explode around
  the entity they are actually pursuing instead of silently using the wrong
  player target.
- Moved the Statistics screens closer to the Release-era taxonomy and object
  pages. Both the direct pause-menu screen and the `MenuScreenFactory` route now
  share the same General rows with source-style names/order for minutes played,
  movement distances, dropped items, damage, deaths, mob/player kills, and fish
  caught. The Blocks tab now exposes crafted/used/mined object columns for
  block items, and the Items tab focuses on non-block crafted/used/depleted
  object stats instead of showing pickup/drop-only rows.
- Added remote-player command targeting for the Release-era multiplayer command
  surface. `/tell`/`/msg`/`/w` now route private messages through the command
  context, and host-side `/give`, `/tp`, `/kill`, `/clear`, and `/spawnpoint`
  can target connected known players through the existing multiplayer
  `clientAction` path. Local command behavior remains unchanged, while remote
  clients now apply addressed give, teleport, kill, clear-inventory, spawnpoint,
  and private-message actions.
- Added Release-era villager zombie avoidance. Villagers now scan for nearby
  live zombies before taking damage, interrupt idle wandering with a flee
  movement away from the closest threat, keep wooden-door opening compatible
  with that movement, and ignore distant zombies outside the avoidance range.
- Corrected shared mob combat target routing. `MeleeAttackGoal` and
  `RangedAttackGoal` now honor explicit living-entity targets before falling
  back to the world player, so restored or AI-assigned mob combat targets can
  actually be chased, looked at, bitten, or shot without requiring a player
  target. Ordinary hostile player targeting remains intact for zombies,
  skeletons, Endermen, Giants, and wolves.
- Added Release-era XP level-up feedback. Collecting an experience orb now
  checks whether the pickup crossed a player level boundary and queues the old
  `random.levelup` cue once after the normal `random.orb` pickup sound, so
  combat/mining rewards produce audible level-crossing feedback without giving
  commands, restores, or non-pickup XP mutations extra side effects.
- Added a Release-style End-exit completion flow. Taking the End exit portal
  now still returns the player to the Overworld spawn, but it also opens a
  paused `The End.` completion screen with a `Continue` action instead of
  silently resuming ordinary gameplay the moment the dimension transfer
  completes.
- Added the Release-era `Games quit` Statistics lifecycle counter. Saving
  unloads from the actual world unload path now increment the counter before
  writing the world, both Statistics screen implementations show the General
  row, save/load validation rejects negative values, and player saves
  round-trip the value.
- Completed the Release-era typed dropped-item Statistics path for real player
  hand drops. Q-dropping from survival or creative hotbar slots now records both
  the total dropped count and the item-specific dropped count, the General
  stats page shows the total dropped-item counter, and typed dropped-item maps
  save/load with the rest of the player statistics payload.
- Corrected Release-era Hardcore death flow. Hardcore deaths now show the
  no-respawn message and a single `Delete World` action in both the death
  overlay and live death menu instead of also exposing the ordinary `Respawn`
  or `Title Menu` exits, and the newer menu-model death screen mirrors that
  Hardcore-only delete path.
- Added Release-era weighted stronghold portal rooms. The generated stronghold
  side-branch table now includes the old depth-gated portal-room weight, so
  recursive stronghold branches can produce source-sized 11x8x16 portal
  chambers instead of the portal room existing only on the guaranteed proxy
  spine. The existing guaranteed proxy library/portal route remains as a
  fallback-style locator guarantee and no longer consumes the recursive
  library/portal weight caps.
- Corrected Release-era stronghold corridor shell material selection. Generated
  stronghold corridor connectors now use the stronghold-stone palette for edge
  blocks, producing plain, mossy, and cracked stone bricks while keeping
  connector corridors free of monster-egg blocks, instead of carving uniformly
  plain stone-brick tubes.
- Corrected Release-era Ender Dragon healing-crystal link timing. Dragons now
  acquire the nearest live End crystal as soon as they tick with no active
  healing crystal, while the actual health restoration still happens on the
  old 10-tick pulse. Destroying a newly linked crystal before the first heal
  pulse now applies the Release-era 10-damage backlash instead of being ignored
  until tick 10.
- Corrected Nether Hell cave vertical carving. The Release-era Nether cave
  carver now mutates the same Y cell used for the ellipsoid source check
  instead of carving one block above it, so generated Nether caves line up with
  the source-shaped radius math while still preserving soul-sand/gravel surface
  patches and lava-contact abort behavior.
- Corrected FireballEntity combat rendering. Ghast and Blaze fireballs now use
  a terrain-atlas fire sprite and bind the terrain texture during projectile
  rendering instead of drawing as a Blaze-powder item icon, so ranged Nether
  combat reads as an actual fireball in-world while preserving the existing
  impact, explosion, and burn logic.
- Corrected Release-era Blaze rod animation. The in-world Blaze model now uses
  the old three-ring rod orbit with separate counter-rotating speeds and
  cosine vertical bobbing instead of fixed-height rings, so Nether combat
  Blazes have a closer Release 1.0 silhouette while keeping the existing head
  aim and combat behavior.
- Upgraded Ender Dragon healing-beam rendering. Active End-crystal healing now
  draws an animated multi-segment tether with paired twisted strands and
  crossbars from the selected crystal center to the dragon body, driven by the
  crystal's existing inner-rotation tick, instead of a single straight debug
  line. Focused renderer-helper coverage verifies the active-crystal anchor,
  strand offsets, and animation change.
- Added the Release-era Player kills Statistics counter. The persistent
  statistics model now tracks player kills separately from mob/monster kills,
  both Statistics screen implementations show the General-page row, save/load
  validation rejects negative values, and world saves round-trip the counter.
- Corrected moving sticky piston head rendering. Moving piston block-event
  meshes now preserve the carried piston-head metadata instead of rebuilding it
  from facing alone, so sticky piston extension/retraction animations keep the
  green sticky face that the settled head already had.
- Corrected Release-era door floor support. Wooden and iron doors now require
  a normal full-block top support for placement and lower-half survival, so
  glass, chests, stairs, and other non-normal supports reject or pop doors
  while normal full blocks such as stone and furnaces still work.
- Added the Release-era dropped-item Statistics hook to live player hand drops.
  Successful drops from survival or creative hands now increment a persistent
  Items dropped counter, both Statistics screen implementations show the new
  General-page row, and the counter round-trips through player saves.
- Added the Release-era fish-caught Statistics hook to the live fishing loop.
  Reeling a catchable bobber now records a persistent fish-caught counter in
  addition to spawning the raw-fish item and damaging the rod, and both
  Statistics screen implementations show the new General-page row.
- Corrected generated abandoned-mineshaft corridor torch metadata. Successful
  corridor torch rolls now emulate Release-era torch auto-orientation against
  generated plank/support neighbors before direct chunk writes, so mineshaft
  torches can render and behave as supported wall torches instead of staying
  raw metadata `0`.
- Corrected Release-era minecart collision query bounds. The world collision
  sweep now expands carts by the old 0.2 block range only on the horizontal X/Z
  axes, preserving the source vertical AABB instead of growing the query
  upward/downward. Vertically separated carts or mobs on nearby rails/platforms
  no longer shove or transfer momentum just because their horizontal boxes are
  close, while same-height cart and mob interactions still use the existing
  Release-style collision paths.
- Added Release-era movement Statistics taxonomy counters. Player travel now
  separates walked, swum, fallen, climbed, flown, dove, minecart, boat, and
  pig distances instead of collapsing all horizontal movement into walked
  distance. Riding updates record vehicle distance without double-counting
  camera sync, the Statistics screen exposes the new rows, and the new counters
  round-trip through player saves.
- Corrected Release-era stranded Squid dry-out behavior. Squid now refill air
  while in water, count down the old air timer when stranded, and take
  2-damage dry-out pulses on the legacy `-20` threshold while preserving their
  fixed land motion and tentacle animation path.
- Corrected Release-era lightning replacement cleanup for ridden pigs. When a
  lightning strike converts a mounted saddled pig into a Zombie Pigman, the
  local player now dismounts through the normal vehicle dismount path before
  the pig is removed, so the player is placed beside the former mount instead
  of relying on a later removed-vehicle cleanup tick.
- Corrected Release-era Fire Resistance ignition behavior for players and
  living entities. Active Fire Resistance now extinguishes existing burn state
  and prevents fire/lava contact from leaving players or mobs visibly burning,
  while preserving ordinary fire contact damage and burn ticks for entities
  without the effect.
- Added Release-era Water Breathing air behavior for players and living
  entities. Active Water Breathing now preserves player air while underwater
  and opts generic living entities out of the shared drowning counter, so the
  HUD/status effect no longer appears without actually preventing underwater
  air loss and drowning.
- Added Release-era Resistance damage reduction for players and living
  entities. Active Resistance now applies the old 20%-per-amplifier incoming
  damage reduction through the real player hurt path and shared living-entity
  damage path, so restored/editor-applied Resistance no longer renders as an
  active status effect without reducing combat or environmental damage.
- Added Release-era Jump Boost jump physics for players and living entities.
  Active Jump Boost now adds the old per-amplifier vertical jump impulse to
  player ground jumps and the shared living-entity jump path, including
  AI-requested and obstacle auto-jumps, so restored/editor-applied effects no
  longer render as active status state without changing jump height.
- Added Release-era Haste/Mining Fatigue mining-speed effects for players.
  Active Haste and Mining Fatigue status effects now scale survival block-break
  progress through the shared player stats path, so editor/restored effect
  payloads affect actual mining speed instead of only rendering as active
  status-effect UI/particles.
- Added Release-era Speed/Slowness movement effects for living mobs. Active
  Speed and Slowness status effects now scale the shared living-entity AI
  motion path, so splashed or otherwise affected mobs visibly chase, flee,
  wander, and breed-move faster or slower instead of only showing HUD/particle
  state while walking at their base speed.
- Corrected cross-dimension portal transfer player state. Nether and End portal
  placement now detaches the player from rideable minecarts, boats, or saddled
  pigs without doing an old-world side dismount, clears carried velocity, and
  resets fall tracking at the destination, so portal travel no longer leaves
  stale vehicle references or old motion bleeding into the new dimension.
- Corrected exact-overlap minecart shove handling for players and living mobs.
  Storage/furnace carts and non-mounting rideable cart contacts now resolve
  center-on-center overlaps from cart motion, with a deterministic fallback when
  both sides are stationary, so clipped mobs or players are separated instead of
  remaining inside the cart hit box.
- Corrected Release-era slime-family contact damage gating. Slimes and Magma
  Cubes now require the old 3D contact distance and clear line of sight before
  applying contact damage and attack sounds, so players separated vertically or
  by a solid wall are no longer hit just because their X/Z positions are close.
- Corrected Release-era taiga grass decoration metadata. Overworld taiga and
  taiga-hills grass scatter now uses the old biome-specific fern roll, placing
  tall-grass metadata `2` most of the time instead of hard-coding every
  generated ground-cover plant to plain grass metadata `1`.
- Added Release-era first-person fire feedback for burning players. The survival
  HUD now draws mirrored lower-screen terrain-atlas flame quads while the local
  player is on fire, behind the normal HUD so hearts, hunger, and hotbar remain
  readable.
- Extended Release-era wetness fire behavior to active rain. Burning players and
  living mobs now extinguish when exposed to rain through the world's biome and
  open-sky precipitation checks, and undead mobs skip daylight ignition while
  raining instead of immediately relighting in wet daytime conditions.
- Corrected Release-era burning skeleton projectile and player burn behavior.
  Skeletons that are on fire now shoot arrows carrying the old 100-tick fire
  payload, fire arrows ignite players after accepted hits, and players now keep
  a timed burn state that ticks down, extinguishes in water, deals periodic
  fire damage, starts from fire/lava contact, and round-trips through saves.
- Added player-kill statistics through the live mob death path. Player-credited
  mob deaths now increment saved Statistics counters for total mob kills and
  hostile monster kills, so melee kills and player-owned arrow kills show on
  the pause-menu Statistics screen and round-trip through saves.
- Corrected Release-era repeater input from branched rear dust. Powered
  redstone wire directly behind a repeater now counts as repeater input even
  when that dust also has a sideways branch, while ordinary dust weak-power to
  neighboring blocks still keeps the old straight-axis bend gate.
- Corrected Release-era abandoned mineshaft crossing support floors. Crossing
  rooms now fill air one block below the intersection footprint with oak planks
  while preserving existing terrain, so generated crossings no longer float
  unsupported over caves even though straight corridors still avoid synthetic
  floors.
- Corrected held-map player markers for off-map same-dimension travel. The map
  item still stores the true player pixel even when it is outside the 128x128
  explored area, but the HUD now clamps the visible marker to the nearest map
  edge instead of hiding it, while different-dimension/invalid marker state
  still suppresses the marker.
- Added shared Release-era explosion visual feedback. The central
  `World.explode` path now emits the animated large explosion particle for
  TNT, creepers, invalid-dimension beds, End crystals, and explosive fireballs,
  and destructive blasts also emit bounded smoke/debris particles from
  destroyed block positions using the display RNG so block drops, fire
  placement, and TNT-chain fuse randomness stay unchanged.
- Completed the remaining Release 1.0 achievement gameplay triggers. Player
  monster kills now unlock Monster Hunter, player-owned arrows that kill a
  skeleton from at least 50 blocks away unlock Sniper Duel, mounted minecart
  rides now track their start point and unlock On A Rail at the 1 km threshold,
  and ridden saddled pigs now unlock When Pigs Fly when fall damage lands on
  the pig. These all flow through live combat/projectile/vehicle/fall-damage
  paths and the existing parent-gated HUD toast queue.
- Completed the Release-era enchanting achievement branch wiring. Crafting an
  enchanting table now unlocks Enchanter after DIAMONDS!, crafting bookshelves
  unlocks Librarian after Enchanter, and a real successful player attack that
  deals at least nine hearts in one hit unlocks Overkill after Enchanter. These
  triggers reuse the existing crafting-output listener, player combat path, HUD
  toast queue, parent gates, and save/load achievement id storage.
- Added movement-history-driven Ender Dragon model articulation. The dragon
  model now has segmented neck and tail chains instead of one rigid head/tail
  pair, and the renderer drives those segment rotations from the existing
  64-sample dragon movement history while keeping wing flaps animated. The
  in-world boss now bends through turns and vertical flight instead of looking
  like a mostly rigid simplified body.
- Wired Release-era menu button click audio. Managed title, pause, options,
  world-select, multiplayer, death, achievement, statistics, and related menu
  buttons now play the old `random.click` cue at 1.0 volume/pitch through the
  current sound-volume setting when a real enabled button activation occurs.
  The sound routes through a direct non-spatial dispatcher path instead of the
  world-position sound queue, so UI feedback works before any world is loaded.
- Enabled the Release-era Achievements pause-menu route now that achievement
  state exists. The in-game pause menu opens a dirt-background Achievements
  screen showing unlocked count plus locked/unlocked Release milestones, and
  the Done/Escape path returns to the game menu.
- Added source-shaped Release-era achievement tree presentation. Achievement
  definitions now carry the old display column/row and special-achievement
  flags, and the pause-menu Achievements screen uses them for parent
  connectors, unlocked/available/locked node states, special-node marking, and
  a next-achievement description instead of a generic wrapped checklist.
- Finished the player-visible Release-era achievement-tree polish. Achievement
  definitions now also carry their source-era icon item, and the Achievements
  screen renders a scrollable, drag-pannable icon tree using terrain/item atlas
  sprites, background tiles, hidden-distance gating, hover tooltips, selected
  details, and viewport scrollbars.
- Enabled the pause-menu Statistics route with real persistent player counters
  instead of placeholder data. Play time, distance walked, jumps, mined blocks,
  picked-up and crafted item counts, successful attacks, damage dealt/taken,
  deaths, mob kills, and hostile monster kills now update from the existing
  gameplay paths, survive save/load, and render on a dirt-background Statistics
  screen. The Statistics screen now also has Release-style General, Blocks, and
  Items pages backed by typed mined-block, picked-up-item, dropped-item,
  crafted-item, used-item, depleted-item, fish-caught, games-quit, and
  travel-mode distance counters that survive save/load. Remaining statistics
  parity work is exact source-era ordering, naming, remaining uncommon counter
  hooks, and
  category-page layout review.
- Closed the newer menu-model progression route gap. `MenuScreenFactory` pause
  screens now expose Release-era Achievements and Statistics buttons, push
  factory-native dirt-background screens backed by the live achievement tracker
  and player statistics, and use scrollable General/Blocks/Items statistics
  pages that fit the 320x240 logical menu model.
- Expanded Release-era item statistics beyond pickup/crafting counters.
  Successful item actions now increment saved total and per-item "used" stats,
  and durability breakage increments saved total and per-item "depleted" stats.
  The Statistics General page shows item-use/depletion totals, and the Items
  page now lists picked, dropped, crafted, used, and break/depletion counts for
  each touched item, using two readable rows so the 320x240 menu model does not
  hide later columns. Representative gameplay hooks include fishing-rod
  cast/reel/breakage,
  bows, throwable items, food/drinks, buckets/bottles, armor equip, block
  placement, animal tools, jukebox records, and similar accepted interactions.
- Unified the direct pause-menu and factory-backed Statistics screens on the
  same scrollable Release-era General/Blocks/Items row builders. General now
  exposes the saved totals for blocks mined, picked-up/crafted/used/depleted
  items, successful attacks, monster kills, and the existing movement/combat
  counters in one path, while the Blocks and Items object pages include
  picked/dropped activity alongside mined/crafted/used/depleted rows instead
  of hiding objects that only have pickup or drop history.
- Added Release-era achievement progression for the first player-facing
  milestone chain and several acquisition/crafting milestones. Opening the
  inventory now unlocks Taking Inventory; obtaining/breaking logs can unlock
  Getting Wood; crafting workbenches, pickaxes, upgraded pickaxes, furnaces,
  hoes, bread, cake, swords, and key acquired items such as iron, diamonds,
  leather, cooked fish, and blaze rods now feed a parent-gated achievement
  tracker. Nether portal travel now unlocks We Need to Go Deeper, End portal
  travel unlocks The End?, and taking the End exit portal unlocks The End.,
  all through the same parent-gated tracker. Unlocks queue a survival-HUD
  "Achievement get!" toast, crafting triggers come from the shared 2x2/3x3
  output-transfer path, and unlocked achievement ids round-trip through world
  saves. Returned player-deflected Ghast fireball kills now unlock Return to
  Sender, and taking a brewed non-water potion from a brewing stand bottle slot
  now unlocks Local Brewery. Crafting an enchanting table now unlocks
  Enchanter, crafting bookshelves unlocks Librarian, and nine-heart single-hit
  player damage now unlocks Overkill through the same parent-gated tracker.
  Monster Hunter, Sniper Duel, On A Rail, and When Pigs Fly now also unlock
  from their live combat, projectile, minecart, and ridden-pig fall paths.
  Remaining progression-menu parity work is the exhaustive statistics taxonomy
  and ordering plus exact source visual review.
- Corrected Release-era slime-family fall handling. Slimes and Magma Cubes now
  ignore generic fall damage while still playing their landing squish/sound
  feedback, so large slime-like mobs no longer die or split just from landing
  after a drop.
- Corrected Release-era wild wolf sheep-hunting cadence. Calm untamed wolves
  still hunt nearby sheep, but now enter the prey scan through the old
  `1/200` target chance instead of checking every second, so sheep predation is
  opportunistic rather than constant while tamed and angry wolf targeting paths
  remain unchanged.
- Added Release-era Giant zombie support for the non-natural legacy entity.
  Giants are now canonical mob definitions backed by the factory, spawners,
  save/load, undead potion/enchantment classification, zombie humanoid
  rendering data, 6x collision/render scale, 100 health, and 50-damage melee
  behavior while remaining absent from ordinary natural spawn tables.
- Extended Release-era sound asset resolution for real pack layouts. Runtime
  audio lookup now keeps exact legacy sound paths first, then resolves
  one-folder-wrapped folder/zip packs, `.jar` archives, `resources/...`
  legacy roots, and `assets/minecraft/...` namespaced layouts, so copied
  Release resource trees or archived sound packs can feed OpenAL playback
  without repacking files into CraftZero's internal resource layout.
- Corrected Release-era animal age-transition movement cleanup. Baby animals
  that become adults, and breeding-cooldown parents that become ordinary
  adults, now stop incompatible parent/child-follow goals and clear their
  movement on the exact transition tick instead of carrying one stale follow
  impulse after their age-gated behavior is no longer valid.
- Corrected Release-era dropped block item rendering. World item entities for
  block items now build their spinning cube meshes from the block item's
  placed metadata as well as its block type, so dropped spruce/birch logs,
  colored wool, slab variants, saplings, furnaces, and other variant blocks
  keep their correct terrain-atlas faces instead of reverting to the canonical
  block metadata.
- Corrected Release-era held block rendering. First- and third-person block
  items now build their cube meshes from the same face-by-face terrain atlas
  UV resolver used by world/entity block meshes, preserve block item metadata
  in the mesh cache, and show distinct front/top/bottom/side and variant
  textures for furnaces, logs, wool colors, slabs, saplings, and similar
  block items instead of collapsing held cubes to one generic side texture.
- Corrected Release-era jukebox removal behavior. Breaking or otherwise
  replacing a jukebox that contains a record now runs the same source-style
  record ejection path before the tile entity disappears: the record pops from
  the randomized top offset with the old upward/random horizontal impulse,
  keeps the 10-tick pickup delay, emits the pop cue, and still lets the jukebox
  block drop when the break should drop blocks.
- Corrected Release-era Eye of Ender flight and expiration. Locator projectiles
  now use the old per-tick steering curve, emit one portal trail particle per
  movement tick, keep the age-80 tick in flight, and only resolve the
  drop-versus-shatter branch after passing that boundary. Failed expiration
  rolls still emit a small portal-particle burst at the rounded final position,
  while successful rolls still drop the recoverable Eye item.
- Corrected Release-era filled map center anchoring. Newly initialized scale-3
  maps now snap their center to the old 128-by-scale world-spawn grid instead
  of using the player's current block, so maps used from the same world tile
  share the expected 1024x1024 coverage.
- Corrected Release-era ascending rail support behavior. Ordinary, powered,
  and detector rails now require the raised-side normal block for sloped
  metadata, so removing that high-end support pops the rail into its item
  instead of leaving a floating slope.
- Corrected Release-era ice lifecycle behavior. Ice now participates in
  scheduled block ticks, melts into still water when block light exceeds the
  old `11 - lightOpacity(ice)` threshold, stays frozen in low block light,
  turns into flowing water when mined over solid/liquid support, and resolves
  to air instead of leaving illegal water behind in the Nether.
- Corrected Release-era sword-blocking damage mitigation. Active right-click
  sword blocking now uses a dedicated source-blockability rule instead of the
  armor-bypass flag, applies the old whole-damage `(damage + 1) / 2` reduction
  before armor/protection/durability, and leaves unblockable fall, drowning,
  suffocation, and magic damage on the normal damage path.
- Added Release-era number-key hotbar swapping across the implemented
  inventory/container screens. Hovered slots in player inventory, crafting
  table, chest/chest minecart, furnace, dispenser, brewing stand, and
  enchanting table screens can now swap with hotbar slots `1`-`9`, while
  preserving each screen's slot rules: crafting/furnace output slots stay
  protected, armor slots require matching armor, furnace fuel rejects non-fuel,
  and one-item brewing/enchanting slots keep their caps.
- Corrected Release-era crafting-result transfer for the player 2x2 grid and
  crafting table. Both screens now use one shared output-taking path for normal
  clicks and shift-click crafting, preserve stack-aware outputs such as repaired
  tools, return container remainders like cake's empty buckets to the crafting
  grid, and queue overflow remainders for world drop instead of deleting them.
- Added Release-era player footstep sound feedback. Player movement now tracks
  post-collision grounded travel, emits old `step.*` material cues for the block
  under the feet, suppresses steps while flying/swimming/in lava, and resolves
  numbered classic resource-pack step pools such as `sounds/step/stone1.ogg`
  through `newsound/step/grass4.ogg`.
- Replaced placeholder procedural cloud clusters with a Release-era textured
  cloud sheet. Overworld clouds now use the bundled
  `/textures/environment/clouds.png`, render as a wrapped 256x256 tiled layer
  at the classic y=108 height, scroll horizontally, alpha-blend against the
  sky, and continue to obey day/night plus dimension/weather brightness
  multipliers.
- Added Release-era moon phase sky rendering. The sky renderer now binds the
  bundled `moon_phases.png` sheet, selects the correct 4x2 atlas cell from the
  world's eight-phase moon cycle, and wraps invalid/debug phase values instead
  of drawing the same static moon every night.
- Added Release-era dimension-aware render environments. The main render loop
  now uses Overworld sky/cloud/weather only in the Overworld, suppresses sun,
  moon, and clouds while underwater/in lava, gives the Nether fixed red dense
  fog without day-night or storm tinting, and keeps The End dark and cloudless
  instead of drawing the ordinary sky layer there.
- Corrected Release-era player-to-living collision feedback. Player/mob
  overlaps now apply the same max-axis `0.05` soft collision impulse to both
  sides: mobs are shoved away and the player receives the opposite horizontal
  nudge. Exact same-position overlaps now use a deterministic fallback push
  instead of silently doing nothing.
- Corrected Release-era abandoned mineshaft corridor support arches. When the
  special support roll triggers, corridors now span oak planks along the left
  and right roof rails one block before and after the support post while
  leaving the center lane open, matching the old side-arch silhouette instead
  of placing only two roof cap blocks.
- Tightened Release-era Enderman teleport placement. Random/projectile dodge
  teleports now skip rainy destinations, cactus-supported landing spots, and
  low-ceiling cells that cannot fit the Enderman's full height before accepting
  a target.
- Added Release-era slime and Magma Cube squish animation state. Slime-like
  mobs now set a jump stretch when they launch, squash on landing, decay the
  squish amount each tick, and feed size-normalized interpolated squish into
  rendering instead of using a passive time-only pulse.
- Corrected Release-era redstone dust bend weak-power semantics. Wire still
  renders and connects around corners, but a powered bend no longer weak-powers
  adjacent blocks through perpendicular branches; horizontal side weak power now
  follows the old straight-axis gate, while isolated dust keeps its all-sides
  weak-power fallback.
- Corrected Release-era flaming explosion aftermath. Explosive Ghast-style
  fireballs now call the flaming explosion path, and explosion-created fire now
  follows the old opaque-full-cube support rule instead of the broader
  attachment-support predicate, so flaming blasts can leave fire on stone-like
  floors but not on glass, chests, fences, or other non-opaque supports.
- Corrected Release-era explosion block item yields. The shared explosion path
  now applies the old inverse blast-power drop chance instead of a flat 30%
  roll: creeper-strength blasts drop roughly one third of destroyed block
  items, TNT roughly one quarter, bed explosions one fifth, and End-crystal
  blasts one sixth, while low-power blasts that destroy blocks keep guaranteed
  drops.
- Corrected Release-era tamed wolf tail feedback. Wolf rendering now feeds
  tamed health into the old tail-pitch formula, so standing owned wolves carry
  their tail high at full health and visibly lower it as they are wounded,
  while sitting wolves and angry wild wolves keep their fixed source-style tail
  poses.
- Added Release-era sheep grazing and wool regrowth. Sheep now run a 40-tick
  grass-eating AI goal, stop moving while eating, consume tall grass at their
  feet before grass blocks below them, turn eaten grass blocks into dirt,
  regrow sheared wool, accelerate baby growth after grazing, and expose the
  lowered-head eating pose to both the body and fleece render layers.
- Added in-world Release-era status-effect particle feedback. Living entities
  and the local player now emit tinted `MOB_SPELL` particles while potion/combat
  effects are active, using a shared vanilla-style potion color table and
  amplifier-weighted color blending so poison, strength, regeneration, hunger,
  Jump Boost, and stacked effects are visible during combat and splash-potion
  play instead of existing only in simulation/HUD state.
- Added player-visible active status-effect HUD feedback. Potion and combat
  effects now stack in Release-era effect-id order at the upper-right of the
  survival HUD, with colored effect panels, brewing/combat item icon cues,
  duration text, amplifier suffixes for stronger effects, and low-time pulsing
  plus a shrinking duration bar so active buffs/debuffs are no longer hidden in
  stats-only state.
- Added an index-aligned generated terrain fallback for the classic 16x16
  Terrain.png atlas. If the terrain texture is missing from the active pack and
  classpath defaults, the texture loader now falls back to an encoded procedural
  terrain atlas instead of failing construction. Common Release-era cells,
  including ores, wood, TNT, plants, furnace, rails, redstone pieces, Nether/End
  blocks, and the crafting-table side, now land on the same Terrain.png indices
  used by `BlockType`; the workbench side also has plank seams, an inset tool
  panel, hammer/saw silhouettes, saw teeth, shadows, and highlights instead of
  placeholder rectangles.
- Extended Release-era double-click collection to the crafting table, furnace,
  brewing stand, dispenser, and enchanting table screens. Specialized
  containers now use their existing slot rules while collecting matching visible
  stacks: crafting/furnace/brewing/dispenser storage can feed the cursor, output
  slots stay protected, fuel and ingredient rules still apply, and one-item
  table/bottle slots keep their caps.
- Added Release-era double-click collection for the player inventory and
  chest/chest-minecart screens. A fast second left click on the same slot while
  carrying a stack now pulls matching visible stacks into the cursor up to the
  item stack cap, while mismatched stacks and non-storage slots stay untouched.
- Corrected Release-era Blaze rain weakness. Blazes now treat exposed rain
  columns like direct water contact, taking one point of environmental damage
  on entity ticks while covered or non-raining columns stay safe.
- Added Release-era container drag distribution for the player inventory,
  crafting table, chest/chest minecart, furnace, brewing stand, dispenser, and
  enchanting table. When a stack is held on the cursor, left-dragging now splits
  it across the dragged eligible slots, while right-dragging places one item
  per eligible slot. Output slots remain non-placeable, armor slots still
  enforce matching armor pieces, furnace fuel slots reject non-fuel, brewing
  bottle slots cap at one item, and the enchanting table slot still caps at one.
- Corrected Release-era player armor rendering. Third-person player armor now
  renders one overlay pass for each equipped armor slot, using the slot's own
  material texture and only the matching helmet, chestplate, leggings, or boots
  model parts. Mixed armor sets no longer let the first layer material paint
  unrelated body parts, and a single helmet no longer draws a full armor suit.
- Corrected Release-era natural spawn cap categories. Runtime spawning now
  counts only monster mobs against the hostile cap, creature mobs against the
  passive animal cap, and water creatures against the squid cap; ambient,
  utility, and boss mobs such as villagers and Snow Golems no longer suppress
  ordinary animal spawns just because they share the `Mob` base class.
- Corrected Release-era idle look-at-player AI handoff. The shared low-priority
  mob look goal now only starts while the mob is genuinely idle and stops as
  soon as movement targets, navigation, or horizontal motion begin, so villagers
  and passive mobs turn toward nearby players without fighting flee, wander,
  breeding-follow, door, or combat movement.
- Corrected Release-era Zombie Pigman anger decay. Player-provoked Pigmen still
  use the old 400-799 tick anger window and delayed angry cue, but the timer
  now actually counts down, clears stale angry-sound state, and drops pursuit
  once it expires instead of keeping Nether packs hostile forever.
- Corrected Release-era rideable minecart mob pickup. Empty rideable carts now
  only capture living mobs through collision after moving above the old
  horizontal speed threshold; stationary or barely moving carts shove nearby
  mobs instead of becoming passive traps, while explicit restored passengers
  still attach during save/load recovery.
- Added Release-era generated stronghold fallback corridors and source library
  gating. Weighted stronghold side branches now try the old
  `ComponentStrongholdCorridor` collision fallback when a branch endpoint runs
  into a same-floor piece, creating short open-ended stone-brick connector
  tubes instead of silently ending. Generated libraries now obey the source
  depth gate, weighted chest corridors use the old max-count cap, and the
  weighted selector now stops once every capped room class is exhausted instead
  of continuing with unlimited filler-only corridors.
- Corrected Release-era water-creature despawning. Squid now share the old
  natural despawn path used by despawnable mobs: immediate removal beyond 128
  blocks from the player and the 32-block/600-tick/1-in-800 soft despawn gate,
  while passive animals, utility mobs, villagers, and bosses remain persistent.
- Corrected Release-era held-map HUD presentation. The initialized map overlay
  now renders in the first-person layer behind the normal HUD, keeps the
  hotbar/XP overlay readable, and uses mirrored angled sleeve and hand-grip
  geometry instead of blocky rectangular grips.
- Added Release-era standing-sign render rotation. Standing signs now draw a
  source-sized board/post mesh through all 16 metadata rotations, and sign text
  is offset onto the board face, so diagonal signs no longer show rotated text
  on an axis-aligned board.
- Added Release-era skeleton bow-aim presentation. Skeleton rendering now
  drives the existing thin-limbed bow pose from the active ranged-attack goal,
  including restored active state, so attacking skeletons visibly raise and aim
  both arms while idle or non-skeleton mobs keep the normal walk/idle arm
  animation.
- Added Release-era bow draw sprite frames. First- and third-person held bow
  rendering now switches from the idle bow icon to the old pull_0, pull_1, and
  pull_2 `items.png` frames at the source-shaped draw thresholds while keeping
  combat charge, release sound, durability, and critical-arrow behavior
  unchanged.
- Added Release-era creeper fuse rendering. Ignited creepers now use the old
  nonuniform swelling pulse and alternating white flash tied to fuse progress,
  while ordinary hurt flashes stay red and charged creepers keep their separate
  `power.png` lightning overlay.
- Added Release-era first-person held-item use poses. The renderer now reads
  active bow drawing, sword blocking, and consumable use as separate states:
  drawn bows pull into a stretched aim pose, blocking swords lift into a guard
  pose even before generic use progress advances, and food/drink items rise
  toward the mouth with bite bobbing instead of sharing the block-place jab.
- Added Release-era saddled-pig rendering. Saddled pigs now draw the classic
  transparent `textures/mob/saddle.png` overlay on an inflated pig render-pass
  model, so applying a saddle is visible in-world instead of only changing
  interaction state.
- Added Release-era sound-pool asset fallbacks. The world-sound resolver now
  keeps canonical `sounds/`, `sound/`, and `newsound/` paths first, then probes
  numbered block material, step, explosion, glass, eat, thunder, flat mob, and
  nested mob sound-pool variants plus the legacy flat `mob/chickenplop.ogg`
  alias, so queued mining, placement, footsteps, explosion, chicken-egg, Blaze,
  Wolf, Pigman, Silverfish, Magma Cube, Enderman, and Ghast cues can actually
  resolve against classic resource-pack layouts instead of silently missing.
- Added Release-era flowing-water boat current behavior. Boats now sample the
  same decay-gradient water current vector as other entities while their hulls
  intersect water, so streams push stationary or drifting boats before the
  existing boat speed clamp, movement, wake, and crash logic runs.
- Added Release-era lava immersion movement. Players and generic entities now
  enter a separate heavy-drag lava branch with the old small downward pull,
  jump-held slow swim-up impulse for the player, and flowing-lava current
  forces, while the existing fire/lava contact damage and item-entity fizz
  cadence stay intact.
- Added Release-era flowing-water current forces. Fluid metadata now exposes
  the old decay-gradient current vector, falling water sheets add the downward
  pull at solid sides, and player, generic entity, and dropped-item movement
  paths apply the same small per-tick push so streams carry mobs, players, and
  item drops instead of behaving like static drag volumes.
- Corrected Release-era enchanting table offer presentation. Open enchanting
  table screens now generate deterministic source-style offer phrases from the
  table offer seed, render those phrases with the bundled alternate font, and
  draw disabled, normal, and hovered offer rows from the old `enchant.png`
  strips instead of placeholder rectangles while keeping level numbers
  right-aligned in the source colors.
- Corrected Release-era piston base/head texture metadata. Piston and sticky
  piston blocks now choose source-style front, side, back, and extended-face
  atlas regions from their facing/extended metadata, and sticky piston
  extension heads keep the sticky-head bit while moving and after settling so
  moving-piston rendering no longer shows sticky heads as normal piston heads.
- Corrected Release-era furnace GUI progress rendering. Open furnace screens
  now draw the flame and cook arrow from the old `furnace.png` overlay strip
  with integer `12`/`24` progress scaling, including the final two-pixel ember
  and one-pixel idle arrow, instead of approximate rounded solid bars.
- Corrected Release-era mineshaft stairwell geometry. Generated abandoned
  mineshaft stairs now carve the five descending slices from local `y=5-i`,
  preserving the block below each slice instead of over-carving one block too
  low for the first four steps.
- Corrected Release-era Ender Dragon flight steering. The dragon now uses a
  source-style turn-limited flight loop with yaw inertia, alignment-based
  acceleration/damping, vertical pitch from motion, far/close waypoint
  retargeting, and a 64-sample movement history buffer instead of snapping its
  facing and velocity directly toward each target point.
- Corrected Release-era Zombie Pigman provocation targeting. Player melee or
  player-owned arrow damage now seeds an immediate chase target for the
  attacked Pigman and nearby alerted Pigmen, instead of leaving them angry
  until the generic nearest-player scan happens to acquire the player.
- Corrected Release-era slime-like mob spawner output. Monster spawner tiles
  now roll the old 1/2/4 size variants for spawned Slimes and Magma Cubes
  before validating the final spawn volume, so spawner-created slime-like mobs
  no longer always appear as size 4.
- Added Release-era End crystal presentation. End crystals now carry the old
  ticking inner-rotation animation state, restore that state from saved entity
  age, and render as a simple 3D crystal/base mesh with the old sinusoidal bob
  and spin instead of a static flat sprite.
- Added Release-era material sound feedback for ordinary player block
  interactions. Successful player placement and mining now emit the old
  `dig.*`/`random.glass` material cues from the affected block center, including
  wood, grass, gravel/dirt, sand, cloth, snow, ladder, glass, stone, and
  metal-pitched stone groups, while automatic world block removals stay quiet.
- Added Release-era world-sound distance attenuation. Spatially dispatched
  piston, chest, mob, explosion, portal, weather, and UI/world cues now fade
  linearly to silence across the old `16 * max(1, volume)` audible radius,
  while loud events such as records and explosions extend range instead of
  becoming louder than full gain. The OpenAL sink now uses the dispatcher-owned
  attenuation curve to avoid driver-dependent double rolloff while retaining
  listener-relative source positioning.
- Added Release-era leave-bed behavior. Sleeping players can now get out of bed
  with Escape or the configured jump/sneak/attack/use controls; leaving bed
  clears the occupied bit, wakes beside the bed when a valid adjacent space
  exists, keeps night/weather unchanged, and tells multiplayer hosts that the
  client no longer counts toward the all-player sleep quorum.
- Added Release-era multiplayer bed sleep gating. Hosted worlds now track
  sleeping clients through the protocol, keep the host in bed until every
  connected player has also entered bed, then broadcast a morning world-state
  and sleep-complete action so clients wake and clear weather together instead
  of letting one host/client skip night alone.
- Added Release-era sheep dyeing. Using any dye on an unsheared sheep now maps
  the old dye metadata to the matching fleece color, consumes one dye in
  Survival/Hardcore, preserves creative stacks, rejects already-matching or
  sheared sheep without falling through to block use, and feeds the existing
  colored fleece render/drop path.
- Corrected Release-era timed potion effect applicability and cadence. Timed
  Poison/Regeneration no longer attach to undead mobs, spider-family mobs now
  reject Poison, instant Healing/Harming still invert against undead, and
  Regeneration ticks use the old `50 >> amplifier` cadence while Poison keeps
  `25 >> amplifier`.
- Corrected Release-era passive and water mob XP variance. Player-killed
  creature and water-creature mobs now drop a random 1-3 experience like old
  `EntityAnimal`/`EntityWaterMob`, while hostile mobs keep their fixed
  Release-era XP values and non-player kills still do not spawn XP.
- Corrected Release-era armor durability wear on player damage. Accepted
  non-armor-bypassing hits now charge equipped armor by the old
  incoming-damage / 4 rule with a one-durability minimum per hit, while each
  durability point still passes through the existing Unbreaking gate and
  broken armor is removed from its slot immediately.
- Corrected Release-era mob-spawner defaults. Player/command-placed spawner
  tiles now default to pigs like old Java spawners, while generated dungeon,
  mineshaft, stronghold, and Nether fortress spawners still set their explicit
  mob type after tile creation.
- Corrected Release-era panic AI fire reactions. Mobs with passive panic goals
  now flee as soon as they are burning, before the first delayed fire damage
  tick, instead of standing idle until damage lands.
- Corrected Release-era animal breeding feedback and age-state AI routing.
  Wheat use now emits the immediate seven-heart burst and can refresh an
  already in-love adult, accepted damage cancels love/courtship, love-mode
  animals stop treating held wheat as a temptation target while they seek
  mates, newborns inherit the first parent's facing, and breeding-cooldown
  parents can trail nearby babies instead of dropping into ordinary wandering.
- Corrected Release-era natural spawning chunk-border semantics. The
  player-centered 17x17 eligible chunk square still drives the old
  `baseCap * eligibleChunks / 256` mob-cap scaling, but runtime spawn attempts
  now skip the outer border chunks before selecting in-chunk candidates, so
  mobs spawn from the inner area instead of appearing on the safety border.
- Corrected Release-era water-creature spawn cell rules. Natural squid spawning
  now accepts a water block with non-normal-cube space above it, including
  surface water, instead of requiring two stacked water blocks. Solid head
  blockers still reject the spawn through the shared water-spawn predicate.
- Hardened Release-era furnace cook-state recovery. A furnace with valid input
  and stale partial cook progress but an empty fuel slot now resets progress
  cleanly instead of dereferencing missing fuel or starting phantom burn,
  preserving input/output state until fuel is added.
- Corrected Release-era piston snow-layer push reaction. Snow layers now use
  the old no-push/destroy mobility path: pistons break front snow layers into
  snowballs instead of moving them, and sticky pistons leave snow layers in
  place rather than pulling them back on retraction.
- Corrected Release-era one-time Overworld creature population to read the
  post-decoration scratch state. Passive packs now validate top
  solid/liquid height, grass/mycelium support, fluids, and collision boxes
  against the decorated shifted `chunk*16+8` spawn area, so off-chunk
  grass/features from population can affect generated animal placement before
  staged mobs are added to the world.
- Corrected Overworld decorator source-order scratch replay. Late terrain
  decorators now see bounded off-chunk dungeon shell, carved air, chest, and
  spawner mutations before ores, trees, huge mushrooms, plants, reeds/cactus,
  and springs run, so neighboring dungeon rooms affect generated terrain
  features instead of being invisible outside the current chunk.
- Added Release-era passive animal damage feedback. Cows, mooshrooms, pigs,
  sheep, and chickens now route their old hurt and death cues through the
  shared mob sound path, including the vanilla reuse cases where cow/chicken
  death repeats the hurt cue and sheep hurt/death repeat the idle bleat.
- Corrected Release-era natural sheep fleece colors. Runtime passive spawns
  and one-time worldgen creature population now initialize sheep with the old
  weighted color roll: common white, black/gray/light-gray/brown variants, and
  the rare pink `1/500` path. Manually constructed/restored sheep still keep
  their explicit stored wool color.
- Added Release-era generated stronghold crossing-hall side branches. The
  weighted stronghold branch queue can now roll the 10x9x11
  `ComponentStrongholdCrossing` hall after the forced start crossing, preserve
  its randomized door and optional lower/upper side openings, and continue from
  its rear and side exits. Generated stronghold branches can now form the old
  multi-level crossing rooms instead of limiting those halls to the fixed entry
  spine.
- Corrected Release-era thrown-item and splash-potion impacts against vehicles.
  Thrown eggs, snowballs, and splash potions now raycast boats and minecarts
  before block impact. Eggs still run their hatch roll on vehicle contact,
  vehicles remain undamaged by zero-damage thrown impacts, and splash potion
  effect radius now starts at the boat/cart hit box instead of a backing wall
  or open-air continuation.
- Corrected Release-era Ender Pearl impacts against collidable non-living
  entities. Ender Pearls now raycast paintings, boats, and minecarts before
  block impact; paintings pop and drop, while boats and minecarts stop the
  pearl at their hit boxes without taking damage. Owner teleport, fall-source
  damage, and the portal-particle burst now use the actual entity impact point
  instead of a backing wall or open-air continuation.
- Corrected Release-era projectile impacts against vehicles. Arrows now raycast
  boats and all minecart variants as collidable entities and feed their
  projectile damage through the existing vehicle attack path, so high-damage
  shots break boats into 3 planks plus 2 sticks and break storage/furnace carts
  into their legacy components and contents. Small fireballs now directly break
  vehicles with their old hit damage, while explosive fireballs detonate at the
  vehicle surface instead of passing through open-air carts or boats.
- Corrected Release-era boat crash speed parity. Horizontal block impacts now
  shatter boats only when their pre-collision horizontal speed is above the old
  `0.15` threshold, sharing the same cutoff used by splash wake behavior.
  Slow dock bumps survive, while faster wall/shore impacts route through the
  legacy component drop path for 3 planks and 2 sticks instead of dropping the
  later boat item.
- Improved Release-era filled-map terrain sampling. Newly painted map pixels
  now sample the full block area represented by that pixel at the map scale,
  choose the dominant terrain color across the area, and feed averaged
  terrain-height/water-depth data into the shaded map palette. A single center
  column no longer makes an entire 8x8 scale-3 map pixel look like water, sand,
  or grass when the surrounding represented area is mostly something else.
- Corrected Release-era filled-map shade selection. Terrain map pixels now use
  the source-shaped previous-pixel height delta plus the odd/even checker term,
  and water pixels use the old depth-plus-checker thresholds instead of a
  simplified depth bucket. Hills, ledges, and medium-depth water now shade like
  old Java maps rather than relying on four-neighbor averaging.
- Improved Release-era held-map presentation. Holding an initialized map now
  renders a large centered parchment-framed map from the full 128x128 color
  grid when the viewport has room, scales down without clipping on short
  windows, keeps the 16-direction player marker aligned to the displayed map
  pixels, and adds simple first-person hand grips instead of leaving maps as a
  small 32-sample HUD preview.
- Corrected Release-era worldgen creature spawn-volume checks. The one-time
  Overworld population pass now tests each generated animal's actual
  block/fluid collision volume instead of requiring literal air in the center
  feet/head cells, so cows, wolves, and mooshrooms can spawn in harmless
  tall-grass/flower ground cover while still rejecting water and real body
  obstructions.
- Corrected Release-era silverfish swarm wakeup volume. Hurt silverfish now scan
  the old source-shaped nearby monster-egg volume from the mob outward
  (`+/-5` horizontally and `+/-10` vertically) and release each awakened egg
  through the shared no-drop monster-egg break path. Stronghold/infested-stone
  fights can now wake hidden silverfish above and below the current floor
  instead of only activating eggs near the same Y level.
- Corrected Release-era tamed wolf follow teleport safety. Standing owned
  wolves now search the owner's nearby landing ring across a one-block vertical
  offset, reject unloaded or colliding spots, and skip liquid, fire, cactus,
  and unsafe support before teleporting. Pets can rejoin across small elevation
  differences without appearing in hazards or inside blocks.
- Corrected cauldron weather behavior back to Release 1.0. Active rain-column
  sampling leaves cauldrons dry; filling remains player-driven through water
  buckets, and glass bottles still drain one water level into water bottles.
  This removes the later 12w22a/1.3.1 rain-fill mechanic from the Release-era
  target.
- Corrected active Nether portal integrity checks. Existing portal blocks now
  require a complete active 2x3 portal interior before staying supported or
  being reused as a destination portal, while fire activation still accepts the
  empty/fire interior space needed to create a new portal. Replacing one portal
  interior block now collapses the whole active portal instead of leaving
  floating portal planes, and destination search skips stale incomplete portals.
- Corrected Release-era sound resource-pack loading and mob sound-pool
  playback. Sound lookup now uses raw active resource paths instead of the
  texture-only resolver, so `sounds/`, `sound/`, `newsound/`, `records/`, and
  `streaming/` assets can be found in selected/default packs. Flat old mob
  cues such as `mob.cow`, `mob.zombiehurt`, and `mob.spiderdeath` also try
  numbered pool files (`cow1.ogg`..`cow4.ogg`, etc.), and the OpenAL sink now
  caches decoded buffers as a per-sound pool so repeated mob events can choose
  among available variants instead of pinning the first resolved asset forever.
- Added Release-era filled-map player facing markers. Map updates now quantize
  the player's yaw into the old 16-direction marker value, clear the marker
  when viewed from the wrong dimension, persist it in map metadata, and the
  held-map HUD preview now draws a small directional marker instead of an
  orientationless cross.
- Added Release-era filled-map item-damage identity. Newly initialized maps now
  mirror their `map_N` runtime id into the stack's item-damage value, copied
  maps preserve that value, initialized legacy maps can restore `map.id` from
  item damage, and save/load accepts and round-trips map damage without
  relaxing durability validation for ordinary non-damageable items.
- Restored the Release-era two-button death screen flow. The live death overlay
  now draws and tracks both `Respawn` and `Title Menu` button regions, and the
  active death menu label uses `Title Menu` consistently with the menu-model
  path instead of the newer `Title Screen` wording.
- Corrected the Hardcore variant of that death flow. Hardcore worlds no longer
  offer normal respawn or a non-deleting title-menu escape after player death;
  the death overlay, live menu, and menu-model death screen now present the old
  no-respawn message with a `Delete World` action.
- Corrected Enderman projectile dodge coverage for thrown items. Endermen now
  treat snowballs and eggs like other physical projectiles: a hit attempts the
  old random teleport escape and cancels the living-entity damage path, so the
  mob does not show hurt feedback, store player-credit damage, or accept the
  zero-damage impact as a real combat hit.
- Corrected thrown egg/snowball impact knockback. Accepted living-entity hits
  now push ordinary targets along the projectile's travel direction with the
  local projectile vertical bump even when damage is zero, so player throws and
  Snow Golem snowballs visibly shove mobs. Endermen still cancel the hit before
  knockback, and End crystals keep their direct destruction path.
- Added Release-style generated stronghold side-branch stairwells. The bounded
  weighted branch queue can now roll the separate 5x11x5
  `ComponentStrongholdStairs` piece, preserve its randomized door variant,
  build the door-aware upper entrance, and continue the branch from the lower
  exit instead of only using straight corridors or straight-stairs connectors.
- Restored Release-era note-block right-click tuning. Ordinary use now cycles
  the note block pitch through the old 0..24 range, immediately attempts
  playback with the new pitch, and still consumes the interaction when a block
  above suppresses sound/particles. Attack-click playback remains the separate
  current-note path, so players can tune and test note blocks the same way they
  could in Java Release 1.0.
- Corrected Release-era chest minecart container lifetime. Right-clicking a
  storage minecart already opened the 27-slot chest UI; that screen now keeps
  the backing minecart entity as its usability target and closes once the cart
  is destroyed or more than 8 blocks from the player, matching the old
  entity-container interaction rule instead of leaving a stale remote inventory
  open.
- Corrected runtime natural mob spawning cadence. The natural spawner now runs
  its Release-style eligible-chunk/category/cap pass on each fixed world tick
  instead of waiting behind a CraftZero-only 20-tick throttle, so valid dark,
  passive, and water spawn opportunities populate promptly while the existing
  Release-era caps, biome lists, pack sizing, light checks, and spawn-distance
  exclusions still bound the result.
- Added Release-style lightning bolt flicker timing. Strike gameplay still
  happens immediately, but the transient render bolt now owns a short flash
  schedule with hidden gaps and re-lit pulses, and the renderer respects fully
  dark gaps instead of forcing every bolt to a constant minimum alpha. Lightning
  now reads as a flickering strike instead of one smooth linear fade.
- Added Release-era precipitation curtain column variation. Rain and snow
  sheets now derive stable per-column phase, fall-speed, width, and height
  variation from the old coordinate-seeded weather hash, so active storms no
  longer scroll as one synchronized flat grid around the camera while keeping
  the existing open-sky biome gates and rain-strength radius ramp.
- Corrected Release-era hostile natural-spawn light gating. Ordinary hostile
  mobs now use the old two-stage random light check: Overworld raw sky light
  must survive the `nextInt(32)` precheck, then the time-adjusted sky/block
  light at the spawn body must be no greater than `nextInt(8)`. Pitch-black
  spaces remain fully eligible, marginal light levels now spawn less often,
  sky-exposed night surfaces are no longer treated as guaranteed valid spawn
  cells, and special Ghast/Slime paths keep their Release-era exceptions.
- Corrected live Release-era piston quasi-connectivity scheduling. Pistons
  already queried the block space above themselves for Java-style power, but
  power changes beside that above-space could leave already-placed pistons
  stale until another neighbor update arrived. Redstone/mechanism scheduling
  now also wakes pistons below each affected power-query cell, including the
  two-block horizontal solid-relay cases used by dust and opaque block
  conduction, so common QC piston setups extend and retract from live lever or
  dust changes.
- Added Release-era projectile fireball deflection. Arrows and thrown
  eggs/snowballs now raycast against fireball entities as well as living
  targets, redirecting the fireball along the projectile path and preserving
  player credit for player-owned projectiles, instead of passing through
  fireballs as if they were not entities.
- Corrected Release-era returned Ghast fireball damage. Player-deflected
  explosive fireballs now use a non-fire projectile damage source with player
  credit on direct entity hits, so returned Ghast fireballs can actually hurt
  fire-immune Ghasts and keep the kill/drop path tied to player combat, while
  ordinary non-deflected fireballs still preserve Ghast fire immunity.
- Corrected Release-era vehicle environmental hazards. Boats and minecarts now
  participate in fire, lava, and cactus contact instead of only living entities
  and dropped items receiving those world hazards. Contact damage routes
  through each vehicle's legacy attack/break path, so boats still drop
  planks/sticks and storage/furnace minecarts keep component/content drops.
- Added first-pass Release-era filled map behavior. Maps now initialize with
  old-style item metadata when first held/used, bind to scale 3 and the current
  dimension, snap their center to the old world-spawn `128 * (1 << scale)` map
  grid, track the player's map pixel, sample loaded terrain into a persistent
  128x128 color array, survive save/load through item metadata and Release-era
  item damage, and show a centered held-map HUD preview with a 16-direction
  player marker. Stack-sensitive map-copy crafting now accepts one
  initialized map plus blank maps and returns copied maps with the source map
  id, item-damage value, center/dimension/scale, player marker, and explored
  color metadata. Runtime map data is now world-backed by map id, so separated
  copied maps share live explored-color updates, while separately initialized
  maps at the same center receive distinct ids instead of accidentally merging.
  That shared filled-map store now persists through save/load, so copied maps
  keep their live shared explored data across sessions. Newly sampled pixels
  now store format-marked Release-style base color plus shade bits, choose the
  dominant terrain color across the represented block area, and use averaged
  terrain-height relief plus source-shaped checker/water-depth thresholds for
  shading, while legacy raw color bytes normalize before rendering. Holding an
  initialized map now displays a large responsive parchment-framed 128-cell map
  with the persisted 16-direction player marker and mirrored angled sleeve/hand
  grips instead of the old tiny preview. Same-dimension off-map marker
  positions now clamp to the nearest visible map edge while preserving the true
  stored off-map pixel for later updates. Remaining map parity work is
  independent source-oracle coverage and matching the exact vanilla
  first-person hand/map texture pose.
- Added Release-era Silverfish block hiding. Idle silverfish now pick a nearby
  compatible stone, cobblestone, or stone-brick block, convert it into the
  matching monster-egg metadata, and remove themselves, while silverfish with an
  active move/combat target keep pursuing instead of disappearing into blocks.
- Corrected Release-era fishing bite timing. Live bobbers now roll the old
  per-water-tick bite chance instead of counting down a predetermined wait:
  clear/open water uses the `1/500` roll, rain-exposed water uses the faster
  `1/300` roll, and successful bites still open the old 10-39 tick catchable
  window with splash feedback.
- Corrected Release-era boat motion feel. Boats now clamp horizontal `motionX`
  and `motionZ` independently to the old `0.4` per-axis cap, then rotate toward
  their actual travel vector by at most 20 degrees per tick instead of snapping
  directly to current motion.
- Corrected generated village blacksmith/HOUSE_2 loot. Release-era
  `ComponentVillageHouse2` now places the blacksmith chest at local `(5,1,5)`
  with the old weighted blacksmith loot table, consumes the structure placement
  random stream for chest rolls, and keeps the forge layout plus smith villager.
- Corrected thrown egg/snowball player attribution. Player hand-thrown
  projectiles now carry explicit player ownership, and thrown projectile
  damage can grant player-credit loot eligibility while preserving the
  projectile itself as the recorded hit source. Player-owned snowball hits now
  open the Blaze rod drop gate; dispenser/null-owned and Snow Golem snowballs
  remain environmental/non-player credit.
- Added Release-era Ender Pearl impact particles. Block and entity impacts now
  emit the old 32 portal particles at the hit point in a two-block vertical
  column before teleporting the owner and applying the existing five-point
  fall-source damage; no extra
  teleport sound was added because the source impact path is particle-only.
- Corrected generated village road recursion. Village side roads now advance
  the Release-style path component depth instead of staying at root depth, so
  generated village layouts branch through the old bounded road chain and stop
  at the source depth cap rather than treating every side road as a fresh root
  path.
- Corrected generated village lamp torches. Village fallback lamp posts now
  place the four torches with rotated Release-style wall metadata attached to
  the wool lamp block instead of invalid metadata `15`, so generated village
  lamps render and survive support checks like ordinary wall torches.
- Corrected Release-era Ghast natural spawning. Ghasts now use flying-mob
  eligibility: their full spawn volume is checked for block/fluid collision,
  but they no longer require a solid floor or hostile darkness, and accepted
  pack attempts roll the old one-in-twenty Ghast spawn gate before placement.
- Added Release-era Ghast vocalizations. Ghasts now emit the old
  `mob.ghast.moan` ambient cry, `mob.ghast.scream` hurt scream, and
  `mob.ghast.death` death cue through the transient world sound queue at
  Ghast-scale audible volume, while preserving the existing charge/fireball
  attack cues and fire immunity.
- Expanded generated stronghold room-crossing exits. The fixed portal/library
  spine now keeps its guaranteed portal and library route while the remaining
  room-crossing side exit either continues from the optional chest corridor or
  creates a source-anchored straight branch root that feeds the bounded weighted
  stronghold branch queue, so generated strongholds expose more explorable
  Release-style rooms instead of ending that exit in the simplified proxy.
- Corrected occupied rideable minecart movement for captured living mobs. Mob
  passengers now trigger the same Release-era 75% movement-step displacement
  multiplier and occupied-cart drag path as player riders, so rideable carts
  that pick up mobs no longer move like empty carts while merely syncing the
  passenger afterward.
- Corrected ordinary rail junction routing to follow the Release-era rail
  logic. Normal rails now use the old reciprocal two-connection neighbor check
  before accepting adjacent tracks, keep straight routes when a neighboring
  track is already fully connected elsewhere, and flip multi-exit junctions
  between the unpowered and powered source curve preferences when powered
  through their support block.
- Added Release-era active Nether portal ambient feedback. Valid portal blocks
  now receive random display ticks near the player, emit the old four
  side-biased portal particles per display tick, rarely queue the
  `portal.portal` sound at the block center with the old 0.8-1.2 pitch band,
  and render all portal particles with a purple shimmer tint instead of plain
  white smoke.
- Corrected cave-spider melee effect gating. Mob-specific melee side effects
  now run only after the shared player damage path accepts the hit, so spawn
  protection, hurt-resistance, Creative immunity, Fire Resistance-style
  rejections, and other failed player damage checks no longer let cave spiders
  apply poison from a swing that dealt no damage; accepted Normal/Hard hits
  still apply the Release-era 7s/15s poison durations.
- Corrected open block-container usability. Crafting table, chest, furnace,
  dispenser, brewing stand, and enchanting table screens now use the old
  same-block/tile and 8-block distance checks while open, and the main gameplay
  loop automatically closes invalid screens and drops carried cursor/table-grid
  stacks if the backing block is removed/replaced or the player moves too far.
- Corrected tamed wolf hurt retaliation. Damaged tamed wolves now stand up and
  assign a valid living attacker as their combat target, including resolving a
  skeleton-style arrow back to its shooter, while still refusing creeper/ghast
  targets and preserving owner-assist sitting rules.
- Corrected Snow Golem visual pose parity. The Snow Golem model now uses the
  Release-era quarter body-yaw turn from head yaw and fixed orbiting stick-arm
  pivots/rotations, replacing the generic idle sway and limb-swing arm motion.
- Corrected caught-fish reel motion. Reeling a catchable fishing bobber now
  spawns the raw fish with the old vector toward the angler, including the
  distance-based upward lift, instead of a fixed vertical hop and weaker
  horizontal pull.
- Corrected mineshaft corridor support bases. Generated abandoned mineshaft
  corridor arches now place oak plank foot blocks with fence posts above them,
  matching the Release-era support shape instead of filling both lower support
  cells with fence.
- Corrected player death XP clearing. The first death-drop pass now always
  clears stored player XP after computing the Release-era capped death-orb
  payout, so level-zero partial XP no longer survives deaths that spawn no
  orbs, while higher-level deaths still drop the capped XP orb total and keep
  score intact for the death screen.
- Corrected player death armor drops. Equipped armor now goes through the same
  thrown-stack death drop path as hotbar, main-inventory, crafting-grid, and
  cursor stacks before the inventory is cleared, so worn armor is no longer
  silently deleted on death.
- Corrected Enderman carried-block pickup cadence. Endermen now roll the old
  1-in-20 pickup chance for carryable blocks instead of the much rarer
  1-in-200 CraftZero cadence, while preserving carried metadata and the
  existing valid-placement gate when they set the block back down.
- Added the Release-era Enderman ambient portal shimmer. Living Endermen now
  emit two portal particles each tick, so idle and angry Endermen visibly
  shimmer even when they are not teleporting; teleport ticks still emit their
  existing 128-particle path feedback on top of the ambient particles.
- Corrected stared-at Enderman melee behavior. Angry Endermen now suppress the
  normal melee swing while the player keeps direct eye contact, reset the old
  stare teleport delay, and random-teleport away at close range; looking away
  still leaves the ordinary Enderman melee path active.
- Corrected minecart-to-player/mob shove math. Non-cart entities hit by a
  moving cart now use the old squared-distance push vector, with the cart taking
  the full collision impulse and the player/mob receiving the quarter-strength
  shove instead of both sides sharing the same simplified max-axis impulse.
- Corrected Nether portal transfer timing for Creative mode. Survival and
  Hardcore still use the Release-era 4-second portal dwell, while Creative
  players now transfer immediately on entering a valid Nether portal instead
  of waiting like survival players.
- Expanded generated stronghold side branches beyond dead-end straight stubs.
  Optional branch roots from the first crossing and first straight connector now
  feed a bounded weighted stronghold branch queue, adding source-shaped turns,
  stair-straights, room crossings, prisons, chest corridors, libraries, and
  additional straights when distance and collision rules allow. The fixed
  portal/library route and portal-room locator remain intact while exploration
  now exposes more Release-style stronghold interiors.
- Added Release-era redstone dust render shaping. Redstone wire chunk meshes now
  draw flat top-only dust segments, extend only toward Release-style connected
  flat/stepped neighbors, fall back to the isolated cross shape when alone, and
  tint from dark unpowered red to bright powered red from metadata strength.
- Added Release-era lever render geometry. In-world levers now draw a separate
  cobblestone base and a narrow handle prism whose floor, wall, ceiling, and
  powered orientations follow the stored metadata, instead of rendering as the
  broad selection box.
- Added Release-era cauldron water-level rendering. Filled cauldrons now draw
  the old inset still-water surface at metadata levels 1..3 on the translucent
  mesh layer, while empty cauldrons stay visually dry.
- Added Release-era minecart variant rendering. Rideable, chest, and furnace
  minecarts now render as a textured open cart tub instead of a flat item
  billboard; chest carts draw a chest payload, and furnace carts draw a
  terrain-atlas furnace payload that switches to the lit face while fueled.
- Corrected Release-era spider combat movement and daylight neutrality.
  Spiders and cave spiders now use the old `0.5` local-brightness threshold
  for neutral targeting/interest loss, and grounded spiders perform the old
  1-in-10 mid-range leap attack impulse instead of only walking into melee.
- Added Release-era spider jockey spawning. Natural spider spawns now roll the
  old 1-in-100 skeleton rider chance, create the skeleton as a normal world
  entity, and keep it pinned to the spider mount until either side is removed.
- Added spider jockey save/load persistence. Saved spider-to-skeleton rider
  references now restore as a mounted pair after reload, and corrupt rider ids
  are rejected instead of silently dropping the jockey relationship.
- Corrected weak-powered opaque-block mechanism conduction. Directly
  weak-powered solid blocks now activate adjacent mechanisms and feed repeater
  inputs, while redstone dust still refuses to pull power through those weak
  blocks, matching the old distinction between mechanism activation and dust
  signal relay.
- Corrected redstone dust propagation timing. When a wire's strength changes,
  adjacent connected dust now gets a same-tick recalculation pass, so long
  dust runs power up and decay in one circuit update instead of crawling one
  block per scheduled tick while still preserving torch/repeater/button delays.
- Corrected Zombie Pigman anger persistence and feedback. Player melee or
  player-owned arrows now set the old finite 400-799 tick anger timer,
  schedule the delayed angry `zpigangry` cue, count anger down each mob tick,
  and route Pigman ambient/hurt/death sounds through the shared mob sound path.
  Provoked Pigmen and nearby alerted Pigmen now immediately receive the
  provoking player's current position as their chase target, so Nether packs
  start pursuing on the damage event instead of waiting for the periodic
  nearest-player scan, then forgive and drop pursuit when the anger timer
  expires.
- Added Release-era Silverfish vocal feedback and locked Squid silence to the
  Release 1.0 behavior. Silverfish now emit their old ambient, hit, and kill
  cues through the shared mob sound path while retaining nearby monster-egg
  wakeup behavior; Squid remain silent for ambient, hurt, and death events.
- Added Release-era Blaze combat feedback. Blazes now emit ambient breathing,
  hurt, and death cues through the shared mob sound path, spawn flame/smoke
  particles when their volley visibly charges, and spawn flame particles at
  each small-fireball launch without changing the existing three-shot cadence.
- Added Release-era slime and magma cube sound feedback. Slimes now emit
  size-scaled squish audio when jumping, landing, taking non-lethal damage,
  dying/splitting, and landing accepted contact damage; magma cubes reuse the
  same sized volume/pitch path while using their distinct jump and big/small
  squish cues.
- Added Release-era wolf vocalizations. Wolves now use state-based ambient
  cues (`bark`, angry `growl`, healthy-tamed `panting`, low-health tamed
  `whine`) and route hurt/death cues through the shared mob sound path while
  preserving the existing wet-shake sound.
- Completed the key Release-era Enderman sound cues. Endermen now emit neutral
  idle and angry scream ambient cues through the shared mob timer, play the
  fixed-pitch stare warning when player gaze aggro starts, and route hit/death
  sounds through the normal mob damage/death path without duplicate lethal hit
  audio.
- Added Release-era timed ambient idle sounds for common mobs. Zombies,
  skeletons, spiders/cave spiders, cows/mooshrooms, pigs, sheep, and chickens
  now use the old increasing idle-sound counter with an 80-tick quiet reset,
  queueing their `mob.*` idle cue through the shared world sound path while
  Creepers remain silent outside fuse/hurt/death events.
- Added Release-era passive animal hurt/death sounds. Cows/mooshrooms, pigs,
  sheep, and chickens now play the old non-lethal hurt cues and single death
  cues without also emitting a lethal hurt sound.
- Added Release-era Enderman carried-block rendering. Endermen that hold a
  block now switch to a raised-arm carrying pose and draw the carried block as
  a metadata-aware terrain-atlas block in front of the body, so block pickup
  state is visible instead of being simulation-only.
- Added Release-era Enderman daylight escape behavior. Exposed daytime
  Endermen now use the old bright-sky random teleport check, clear active
  aggression after a successful escape, and keep covered/night Endermen on
  their normal combat path.
- Added Overworld village and mineshaft support to the shared structure
  locator. Locate-style callers can now resolve the nearest generated
  village/mineshaft start center using the same placement gates and generated
  start graphs that chunk population uses, instead of only finding strongholds
  and Nether fortresses.
- Corrected love-mode animal mate-follow continuation range. Wheat-fed animals
  now keep pursuing a selected compatible mate through the full 1.25x leash
  distance instead of dropping pursuit early from a squared-distance
  multiplier mistake.
- Corrected chest lid rendering to use the Release-era cubic easing curve and
  full right-angle hinge rotation. In-world chests now visually snap open with
  the old accelerating lid motion instead of the previous linear 65-degree
  proxy animation.
- Added Release-era chest latch geometry to the in-world chest renderer. Single
  and double chests now draw a small lid-mounted front latch that follows the
  same facing metadata, joined-chest center, and cubic lid hinge as the animated
  lid instead of rendering as plain body/lid boxes.
- Added Release-era potion bottle inventory visuals. Potion stacks now render a
  metadata-driven colored liquid overlay in inventory, crafting/container GUIs,
  creative inventory, cursor stacks, and legacy slots, with splash bottles
  receiving a separate visible marker instead of every brewed result looking
  identical until hover text is read.
- Added Release-era enchanted item glint visibility. Enchanted stacks now draw
  a metadata-driven animated purple glint wash/band overlay in inventory,
  crafting/container GUIs, creative inventory, cursor stacks, legacy slots, and
  the survival hotbar instead of looking identical to plain gear until hover
  text is read.
- Added Release-era baby animal parent-follow AI. Baby cows, mooshrooms, pigs,
  sheep, and chickens now select nearby compatible adult parents, trail them
  while farther than the old close-follow distance, stop once close/out of
  range, and stop seeking parents as soon as they grow into adults.
- Corrected Release-era passive animal follow speed constants. Love-mode mate
  seeking now uses the old slower breeding chase multiplier, while wheat
  temptation and baby parent-follow use the source parent/tempt movement
  multiplier instead of the previous over-fast shared value.
- Added a dedicated Release-era villager render model. Generated villagers now
  keep their profession texture path while rendering with the tall head,
  protruding nose, robe body, and folded-arm silhouette instead of the generic
  humanoid/zombie stand-in model.
- Added Release-era villager wooden-door interaction. Moving villagers now
  open closed wooden doors in their path, leave iron doors untouched, close
  only the wooden doors they opened after the old short hold window, and route
  the change through normal door sound, mesh, and mechanism-update hooks.
- Added Release-era mob spawner preview rendering. Active spawner tiles now
  advance the old delay-based cage-mob spin state, inactive spawners keep a
  stable preview, and the main render pass draws a scaled cached mob matching
  the configured spawner definition inside the cage.
- Corrected Release-era spawn orientation. Natural ground/water mob packs and
  mob-spawner spawns now initialize mobs with randomized horizontal facing and
  zero pitch at spawn time instead of letting newly spawned mobs all inherit
  the default orientation until AI moves them.
- Added Release-era vine random-tick behavior. Vines now participate in the
  scheduled block tick loop, prune side metadata whose support disappeared,
  fall off when fully detached, obey a local density cap, and can grow new
  hanging columns downward or spread into nearby supported air cells.
- Added Release-era grass/mycelium random-tick terrain mutation. Grass and
  mycelium now participate in scheduled block ticks, decay back to dirt when
  covered by opaque low-light blocks, and spread to nearby dirt under bright
  open light using the old four-attempt local search. Block simulation now
  reads calculated chunk light for these ticks instead of the public
  spawn-safety sky-light fallback.
- Added Release-era rideable minecart living-passenger capture. Empty rideable
  carts now pick up living mobs they collide with, keep the passenger seated
  on the cart during rail/off-rail movement, use occupied-cart drag while a mob
  passenger is aboard, and preserve the cart-passenger reference through
  save/load. Storage and furnace carts still shove living entities instead of
  capturing them.
- Added Release-era minecart-to-player collision in the world sweep. Moving
  carts now shove the local player with the same max-axis impulse family used
  for living-entity cart contacts, while mounted minecart players are skipped so
  riding synchronization does not fight collision pushes.
- Added Release-era boat-to-player collision in the world sweep. Moving boats
  now shove overlapping unmounted local players with the same horizontal impulse
  family used for boat entity contacts, while mounted boat players are skipped
  so riding synchronization remains authoritative.
- Restored the Release-era death-screen score line. The live death overlay and
  menu-model death screen now show the player's tracked score using the old
  `Score: N` label instead of only showing the title and respawn controls.
- Added Release-era boat splash wake feedback. Boats moving faster than the old
  water-wake threshold now spawn transient splash particles along their current
  travel heading while they are in water, while slow or dry boats remain quiet.
- Added Release-era wolf wet/shake visuals. Wolves now become wet while in
  water, start the old grounded idle shake after leaving water, play the wolf
  shake sound, emit splash particles during the shake window, roll the
  head/body/tail model parts through the sine-based shake pose, and preserve
  active wet/shake timers across save/load.
- Added Release-era fully drawn bow critical arrows. Player bows now mark
  max-charge shots as critical, critical arrows emit in-flight crit particles,
  accepted hits add the old randomized critical damage bonus, and active
  critical arrow state round-trips through save/load.
- Corrected Release-era spider wall-climbing physics. Spiders now treat
  horizontal collision as their ladder flag, so pushing into a wall clamps
  falling motion, applies the old upward climb bump through the shared
  climbable path, and clears stale fall-distance tracking at the climb contact.
- Corrected Release-era painting hanging validity to reject broad entity
  overlap, not only painting-on-painting overlap. Paintings now fail placement
  when mobs, players, pending entities, or dropped item entities occupy the
  hanging space, and an existing painting breaks/drops when another entity
  enters its thin wall-aligned bounds.
- Corrected projectile impacts against hanging paintings. Arrows, thrown
  eggs/snowballs, splash potions, and fireballs now raycast paintings as
  collidable hanging entities, pop the painting item at the impact point, and
  consume the projectile before the backing wall absorbs the hit.
- Added Release-era opaque-block suffocation for players and living entities.
  The shared hazard pass now detects intersection with opaque full-cube
  collision boxes, applies a dedicated suffocation damage source that bypasses
  armor and difficulty scaling, and uses the old half hurt-resistance cadence
  for repeated in-wall damage.
- Added safer Release-style Nether destination portal creation. When no
  destination portal is found, portal preparation now searches a nearby loaded
  creation radius for an air cavity with floor support before falling back to
  forced target construction, and generated frames now honor both X and Z portal
  axes.
- Added Release-era generic living-entity drowning. Non-water-breathing living
  mobs now keep the old 300-tick air supply, take 2 drowning damage on the
  source-style -20 air pulse, and repeat that pulse every 20 underwater ticks;
  squid-style underwater breathers opt out of the ordinary air counter and now
  use their dedicated fixed water-mob land motion path instead.
- Added Release-style Nether destination portal reuse. Dimension transfers now
  search the loaded 128-block destination radius for an existing Nether portal
  before creating a new frame, and the player is placed at the reused/built
  portal interior instead of blindly landing at the raw scaled coordinate.
- Added Release-era boat entity collision. The world post-tick collision sweep
  now includes boats, so moving boats shove overlapping mobs/living entities
  and transfer horizontal push to other boats instead of phasing through them.
- Runtime natural mob caps now scale with the same Release-style eligible
  chunk area used for spawning. The single-player 17x17 chunk sweep raises the
  effective caps with the old `baseCap * eligibleChunks / 256` shape, so
  hostile spawning can continue past the flat 70-mob shortcut when the
  player-centered spawn area supports it.
- Runtime natural mob spawning now uses the Release-style 17x17 eligible chunk
  area around the player instead of making one radial candidate attempt per
  category, while skipping that square's outer border for actual placement.
  Hostile, passive, and squid spawning still honor the existing caps, pack
  sizing, loaded-chunk safety, and 24-block player/world-spawn exclusions, but
  valid packs can now appear from generated inner eligible chunks in the old
  player-centered range.
- Runtime natural mob spawning now performs the old-style three local group
  attempts inside each selected eligible chunk. Each group jitters through up
  to four nearby candidate positions, locks a same-entry pack target, and stops
  as soon as that group's rolled count or the remaining cap is filled, so valid
  dark/water/passive surfaces can populate with denser Release-like packs
  instead of only one selected pack attempt per chunk.
- Runtime passive and water-creature spawning no longer use CraftZero-only
  category chance gates on top of the Release-style cap check. During each
  natural-spawn pass, animals and squid now get the same cap-driven eligible
  chunk sweep as hostiles whenever their terrain and dimension rules allow.
- Runtime natural mob caps now use Release-style categories instead of counting
  every non-hostile mob as a creature. Villagers, Snow Golems, and other
  ambient/utility/boss mobs no longer fill the animal cap, while monsters,
  animals, and squid continue to clamp only their own natural-spawn groups.
- Corrected Release-style lit furnace ambience. Burning furnaces now emit smoke
  and flame particles from the metadata-facing front side while staying
  soundless, matching the Release 1.0 display tick rather than a later
  furnace-specific crackle.
- Replaced new-world spawn selection's custom ring scan with a Release-era
  source-shaped search. Overworld world spawns now use the seeded
  generation-layer forest/plains/taiga biome reservoir search, then the old
  paired 64-block random X/Z jitter loop until a grass-surface spawn coordinate
  is accepted; player placement resolves to the generated surface at that
  coordinate.
- Corrected Skeleton ranged combat toward Release 1.0 behavior. Skeletons now
  fire immediately once an eligible player is inside the old close-range bow
  window, keep shooting even when the player is closer than four blocks, use a
  30-tick bow cadence, and spawn slower arcing arrows from the old raised
  skeleton launch point instead of using the newer strafing/backoff profile.
- Added Release-era wild wolf sheep hunting. Calm untamed wolves now acquire
  nearby sheep as prey through the old `1/200` target chance, chase them, and
  bite with the wild-wolf damage value, while tamed owner-assist and
  angry-player retaliation stay on their separate targeting paths.
- Added Release-era wild wolf pack anger. Player melee and player-owned arrow
  hits that leave a wild wolf alive now alert nearby calm wild wolves inside
  the source-shaped local pack range, while tamed wolves and one-hit wolf kills
  stay inert.
- Added Ender Dragon healing-beam feedback. When the dragon is actively
  charging from a nearby End crystal, the renderer now draws an animated
  multi-segment crystal-to-dragon tether from the crystal center to the dragon
  body, matching the gameplay healing state instead of leaving crystal healing
  invisible or showing only a single straight line.
- Added Release-era Ender Dragon terrain carving. During normal flight, the
  dragon now deletes ordinary loaded blocks inside its source-shaped head and
  body part boxes without dropping items, leaves bedrock/obsidian/End stone
  intact, emits visible destruction bursts when blocks are carved, and uses the
  old protected-block slowdown on the next flight step.
- Corrected Ender Dragon contact combat to use its source-shaped head/body part
  boxes instead of a coarse center-radius hit. Players are now damaged and
  knocked away only when their player box intersects the flying dragon's active
  contact parts, so the final fight better matches the Release-era multipart
  dragon.
- Added Release-era Ender Dragon player retargeting. When a live player exists
  in The End, dragon retarget rolls can now choose the player's current
  position instead of always wandering around a random island ring, so the boss
  fight can pressure the player through the existing flight, block-carving, and
  contact-damage paths.
- Added Release-era minecart-to-living-entity shove handling. The world
  post-tick minecart collision sweep now lets moving carts push overlapping
  mobs/entities with the same max-axis impulse used by old living-entity
  collision, while preserving the existing cart-to-cart momentum rules.
- Added Release-era Enderman teleport feedback. Successful Enderman teleports
  now spawn the old 128 portal particles along the previous-to-new path and
  queue the `mob.endermen.portal` sound at both endpoints.
- Corrected Release-era Ender Dragon healing-crystal link timing. The dragon
  now chooses a nearby live End crystal immediately when no current crystal is
  linked, keeps that active crystal relationship between heal pulses, and still
  heals only once per 10 ticks. Destroying the newly linked crystal before the
  first heal pulse now damages the dragon through the existing crystal
  destruction callback.
- Corrected End crystal damage dispatch so crystals no longer depend on
  `LivingEntity` health, damage amount, or invulnerability-frame acceptance.
  Any non-fire hit now immediately removes the crystal, preserving the existing
  fire immunity and non-living potion/healing behavior. Direct non-explosion
  hits create the Release-era power-6 crystal blast, while explosion-sourced
  crystal damage destroys the crystal without recursively creating another
  crystal explosion and still notifies the dragon healing link.
- Hardened End crystal save validation so zero-health crystal payloads are
  treated as corrupt instead of being restored as live crystals. This matches
  the runtime save path, which omits destroyed crystals rather than preserving a
  dead crystal entity for later resurrection.
- Hardened non-dragon mob save validation so zero-health mob payloads are
  treated as corrupt instead of being restored at full health. The Ender Dragon
  remains the explicit exception because its old death sequence is a persisted
  runtime state.
- Hardened experience-orb save validation so zero-health and overfull-health
  orb payloads are treated as corrupt instead of being restored as
  default/impossible-health collectible orbs.
- Hardened experience-orb age validation so expired orb payloads at or beyond
  the old 6,000-tick despawn boundary are rejected instead of being restored as
  live collectible entities.
- Hardened dropped-item age validation so stale item-entity payloads at or
  beyond the old 300-second despawn boundary are rejected instead of being
  restored as live world drops.
- Hardened dropped-item health validation so saved item-entity payloads above
  the old five-health maximum are rejected instead of being restored with
  impossible durability against fire, lava, or explosions.
- Hardened stuck-arrow save validation so in-ground arrows at or beyond the
  old 1,200-tick despawn boundary, plus airborne arrows carrying impossible
  stuck timers, are rejected instead of being restored as collectible
  projectiles.
- Hardened Eye of Ender save validation so locator projectiles older than the
  old 80-tick in-flight boundary are rejected instead of being restored as live
  entities.
- Hardened Ender pearl save validation so ownerless or expired teleport
  projectiles at the old 1,200-tick despawn boundary are rejected instead of
  being restored as live entities.
- Hardened thrown item, splash potion, and fireball save validation so
  projectile payloads past their runtime despawn boundaries are rejected
  instead of being restored as live entities.
- Hardened fishing hook save validation so ownerless bobbers, expired bobbers,
  invalid hooked-target references, and impossible wait-plus-catchable phases
  are rejected instead of being restored as live fishing entities.
- Hardened saved mob age and breeding state. Restored mobs now reject
  impossible `growingAge` values outside the Release-era baby-to-parent
  cooldown range, love-mode counters outside `0..600`, and saved love mode on
  babies or cooling-down adults instead of restoring an impossible animal state
  and letting the next runtime tick silently rewrite it.
- Corrected the main simulation fixed step to the Release-era 20Hz tick rate.
  Entity AI, mob age/breeding timers, projectile lifetimes, minecart ticks, and
  other per-tick systems now run from a 50ms fixed update instead of the
  previous 60 UPS driver, preventing source-tick logic from advancing too fast
  during normal gameplay while rendering still interpolates between fixed ticks.
- Hardened Release dimension generator selection against stale save metadata.
  Dimension-specific generator ids for Nether and The End now win over a
  contradictory dimension field, so restored worlds cannot silently downgrade a
  saved Release Nether/End generator into an Overworld generator. Nonblank
  unknown dimension names now count as corrupt level metadata and recover from
  backup when available instead of falling through the forgiving Overworld
  parser path. Nonblank unknown generator ids now follow the same corruption
  path instead of silently selecting the legacy null-generator terrain.
  Corrupted potion payloads on non-potion item stacks now also fail validation,
  including player/container inventory stacks and dropped items, so ordinary
  items cannot round-trip hidden potion metadata through save/load. Null or
  otherwise invalid saved enchantment entries on item stacks now follow the
  same corruption path instead of being silently filtered during restore.
  Saved item stacks whose count exceeds the item's Release-era max stack size
  now also fail validation and recover from backup when available, so impossible
  overlarge inventory or dropped-item stacks cannot be restored into live state.
  Saved item payload durability now follows the same restore gate: damageable
  items must carry a remaining durability value within their Release-era range,
  while non-damageable items must not carry durability metadata. Saved potion
  payloads now also have to match a potion identity from the Release-era
  brewing/creative catalog, so impossible hybrid combinations such as extended
  and enhanced effects cannot restore into inventory, dropped-item, or active
  splash-potion state. Saved enchantments now also have to match Release-era
  item applicability, level caps, and compatibility rules, so post-1.0 bow
  enchantments and impossible pairs such as Silk Touch plus Fortune cannot
  restore through the save path. Player armor slots now validate their saved
  item type against the slot they restore into, rejecting misplaced helmets,
  chestplates, leggings, or boots before they can bypass ordinary equipment
  rules. Saved thrown-item projectile entities now only accept the Release-era
  egg and snowball projectile item identities; ordinary items must restore as
  dropped stacks instead of silently becoming active projectiles. Saved
  falling-block entities now likewise require a block that participates in the
  Release-style falling-block path, rejecting ordinary block ids before they
  can restore as active falling entities. Saved minecart entities now reject
  unknown cart kinds and kind-inappropriate inventory payloads, preventing
  malformed carts from silently downgrading to rideable carts or carrying
  hidden chest state. Saved painting entities now validate their art motive
  against the Release-era painting catalog and require horizontal facing
  metadata, so unknown motives or impossible faces no longer restore through
  constructor defaults. Saved world metadata now rejects non-finite or
  out-of-range day-cycle times, unknown nonblank weather states, and explicit
  nonpositive weather countdowns before restore can skip, normalize, or replace
  those values. Saved player numeric state now rejects non-finite
  position/rotation/spawn coordinates, impossible health/food/saturation/
  exhaustion/air values, and negative progression counters before restore can
  clamp them into a different player state. Saved player and entity
  status-effect lists now reject null entries, expired effects, negative
  amplifiers, and duplicate effect types before restore, preventing malformed
  effect payloads from being silently filtered into different runtime state.
  Saved dropped item payloads
  now reject non-finite, negative, and expired ages plus impossible health
  above the old five-health item-entity maximum before restore instead of
  letting setters clamp or preserve them into live world drops. Saved
  experience orb payloads now reject nonpositive XP values, expired ages,
  negative pickup delays, and nonpositive or overfull health
  before restore instead of letting entity constructors or setters clamp them
  into a different live orb. Saved transient entity payloads now also reject
  impossible arrow fire/stuck/knockback state, expired stuck-arrow lifetimes,
  ownerless/expired Ender pearl payloads and Eye of Ender lifetimes or
  non-finite Eye targets, expired thrown item/splash potion/fireball/fishing
  hook lifetimes, ownerless fishing hooks, invalid fishing hook target references,
  invalid mob combat target references, duplicate positive entity reference
  ids, out-of-range falling-block metadata, Release-era fishing hook
  wait/catchable timers outside their live windows or both active at once,
  invalid primed TNT fuses, and impossible
  boat/minecart damage or furnace-minecart fuel/push state before those values
  can be clamped into different runtime entities. Saved End crystals and mobs now likewise reject
  impossible health, negative fire/combat/AI timer values, invalid Creeper
  fuses, invalid Slime/Magma Cube sizes, and clamped mob-specific payloads such
  as sheep colors or villager professions before restore can turn corrupt data
  into plausible live entities.
  Tile-entity and mechanism queue payloads now get the same treatment: furnace,
  brewing stand, chest, note block, jukebox, enchanting table, mob spawner,
  scheduled block tick, and moving-piston snapshots reject invalid timer,
  accumulator, inventory-size, block-id, facing, metadata, and animation values
  instead of letting restore clamp them into altered redstone or tile state.
- Restored the Release-era command/editor mob-spawner item identity. Item ID
  `52` now resolves to `MOB_SPAWNER` and maps back to the mob-spawner block for
  old `/give` and editor-style item paths, while the creative catalog keeps it
  hidden from normal browsing alongside `FIRE`; breaking spawners still drops
  no collectible item.
- Restored the Release-era monster-egg command/editor item identities. Item ID
  `97:0..2` now resolves to the infested stone, cobblestone, and stone-brick
  variants used by strongholds and silverfish behavior, while normal creative
  browsing hides those variants and both ordinary breaking and Silk Touch still
  keep monster eggs non-collectible.
- Restored the Release-era double-slab command/editor item identities. Item ID
  `43:0..5` now resolves to the matching double stone/sandstone/wooden/
  cobblestone/brick/stone-brick slab variants, while normal creative browsing
  hides them and survival mining, including Silk Touch, continues to return two
  half-slab drops instead of collectible double-slab blocks.
- Corrected right-click block-conversion visual refreshes. Wheat seeds,
  pumpkin seeds, and melon seeds now rebuild the newly planted crop/stem cell
  immediately after successful farmland placement; hoeing dirt/grass into
  farmland and flint-and-steel fire placement now also rebuild the changed
  block immediately instead of waiting for a later mesh refresh. Water-bucket
  cauldron fills, glass-bottle cauldron drains, and Eye of Ender frame
  insertion now do the same, with final portal activation refreshing the
  clicked frame and newly created End portal cells. Flint-and-steel TNT
  priming now also requires a successful TNT prime before damaging the tool and
  refreshes the removed TNT block immediately. Empty-bucket source-fluid pickup
  and water/lava bucket placement now refresh the removed or placed source
  fluid block immediately while leaving Nether water evaporation as a feedback
  event without a block mesh rebuild. Manual wooden-door toggles now rebuild
  both valid door halves, so clicking the upper half no longer leaves the lower
  metadata change waiting for a later mesh refresh. Bed occupied metadata
  changes now also rebuild both bed halves when sleep starts or completes.
  Jukebox record insertion/ejection metadata sync now rebuilds the jukebox
  block immediately so the record state does not wait for a later chunk mesh
  refresh.
- Corrected brewing modifier delivery preservation. Splash potions can now
  continue through the same base, effect, redstone, glowstone, and fermented
  spider-eye brewing transforms while preserving their splash delivery flag;
  gunpowder on an already-splash potion remains a no-op.
- Corrected brewing stand bottle-bit visual refreshes. Runtime bottle-slot
  occupancy metadata changes now rebuild the brewing stand block immediately
  so the visible bottle model does not wait for a later chunk mesh refresh.
- Corrected brewing stand bottle-bit rendering. The in-world stand mesh now
  draws only the occupied bottle silhouettes from metadata bits `0..2`, so
  empty or partially-filled stands no longer show phantom bottles while the
  source rod/base collision remains unchanged.
- Corrected furnace lit/unlit visual refreshes. Tile-preserving swaps between
  normal and lit furnace blocks now rebuild the furnace block immediately while
  retaining facing metadata and the same furnace tile entity, so restored burn
  state and spent-fuel transitions do not wait for a later chunk mesh refresh.
- Corrected note-block redstone edge timing. Powered neighbor changes now
  update note blocks immediately through the mechanism-neighbor path instead of
  waiting for a delayed scheduled redstone tick, while the block metadata still
  debounces held power so only rising edges play notes.
- Corrected redstone-driven openable visual refreshes. Signal-edge changes for
  doors, trapdoors, and fence gates now rebuild the changed openable block
  immediately; doors rebuild both lower and upper halves so redstone-powered
  panel orientation does not wait for a later chunk mesh refresh.
- Corrected Release-era pig saddle and loot edges. Baby pigs can now accept
  saddles and be mounted through the same player path as adult pigs, matching
  the old pre-1.2.4 saddle interaction, and pig porkchop drops now use the old
  0-2 range instead of the later 1-3 range. Unsaddled, already-saddled,
  removed-pig, and no-saddle rejection paths remain covered.
- Corrected Release-style starvation damage. Hunger-empty damage now waits the
  old 80-tick interval, applies one health point per starvation pulse, stops at
  10 health on Easy and 1 health on Normal, can kill only on Hard, and Peaceful
  movement no longer drains the visible food bar or starves the player.
  Peaceful now also restores one health per second without refilling food,
  preserving the Release-era separation between passive Peaceful healing and
  hunger state.
- Corrected Release 1.0 FoodStats-style action exhaustion. Walking and
  sprinting now add distance-based exhaustion, jumping adds the old hidden
  exhaustion cost instead of immediately shaving hunger/saturation, and the
  food bar only changes after exhaustion crosses the old 4.0 threshold with
  saturation draining before visible food. The Hunger status effect now feeds
  the same hidden exhaustion path each tick instead of directly subtracting
  fractional food, and accepted attacks, accepted player damage, and successful
  survival block breaks now add their old FoodStats exhaustion costs. Hidden
  FoodStats exhaustion now also round-trips through level save/load. Eating and
  player-stat restore now clamp hidden saturation to the current food level
  instead of allowing saturation to exceed visible hunger.
- Corrected Release-style natural food regeneration cadence. Players with
  food level 18 or higher now heal one health point only after the old 80-tick
  timer instead of receiving continuous fractional healing, and that pulse no
  longer directly consumes hunger or saturation through CraftZero's previous
  regen-only cost path. Each natural-regeneration pulse now adds the old
  hidden FoodStats exhaustion cost so later threshold processing drains
  saturation/food through the shared exhaustion path.
- Corrected represented Release 1.0 block-light emission constants. Redstone
  torches now emit the old lower redstone-torch light instead of normal torch
  brightness, and jack-o-lanterns, lit repeaters, brown mushrooms, End portal
  frames, plus the existing portal/furnace/lava/glowstone/special lights are
  pinned by focused registry coverage.
- Corrected glowstone's material-style opacity. Glowstone now remains a solid
  full block with light level 15, but no longer behaves as an opaque
  face-occluding/ambient-occluding normal cube; redstone dust keeps the old
  explicit glowstone support exception through the placement predicate and can
  carry dust power upward onto glowstone without also reading dust power
  downward through it.
- Corrected source-style fire survival and placement support. Fire placement
  now requires an opaque block below or a flammable/TNT neighbor instead of
  accepting unsupported air, and scheduled fire ticks no longer let ordinary
  solid blocks behave like permanent fuel: fire on plain support burns out
  after the old age threshold unless it has flammable neighbors. Netherrack
  remains the infinite-fire support and now resists rain dousing, while End
  bedrock is treated as the End-dimension infinite support for crystal-style
  fire.
- Corrected fire spread to use the Release-era burn-rate tables instead of a
  single generic flammable flag. Planks, fences, stairs, logs, leaves,
  bookshelves, TNT, tall grass, wool, and vines now expose their old
  encouragement/flammability rates; immediate spread uses the old horizontal
  and vertical catch chances, and TNT primes only after passing the source burn
  table. Empty-air propagation now uses the source-shaped 3x3 column scan
  around fire, maxes neighbor encouragement for each candidate air cell, and
  makes higher upward spread progressively harder instead of using the old local
  1-in-6 adjacent-air shortcut.
- Corrected global gameplay-screen closing for container GUIs. Crafting table,
  chest, furnace, dispenser, brewing stand, and enchanting table screens now
  drain their queued cursor/click-out stacks into the world when closed through
  the shared Escape path instead of leaving items stranded inside a closed
  screen instance. Player inventory closing now follows the same Release-era
  close rule by dropping the carried cursor stack and 2x2 crafting-grid
  contents instead of merging the carried stack back into inventory. The focused
  close regressions now run headlessly because cursor-lock state changes
  tolerate tests without an initialized GLFW window.
- Corrected player-inventory 2x2 crafting-grid quick-move routing. Shift-clicks
  from the temporary crafting grid now try the whole player inventory, so an
  item can move into an empty hotbar slot even when the main inventory is full.
- Corrected enchanting table close semantics: the temporary table slot now
  queues its item for world drop on GUI close instead of silently returning it
  to the player inventory, matching the old container-close path.
- Corrected orphaned upper wooden-door activation. Interacting with a top-half
  door block, or processing its powered scheduled tick, now delegates only when
  a valid lower half exists, so malformed or partially restored doors no longer
  synthesize a missing lower block.
- Corrected door render/collision bounds to use the Release-era `BlockDoor`
  facing table plus the upper-half hinge bit. Closed doors now occupy the
  source edge for metadata `0..3`, and open lower/upper halves share the same
  hinge-aware panel box instead of ignoring the top-half metadata.
- Corrected door placement to write the source upper-half hinge metadata from
  neighboring opaque block counts and adjacent door columns instead of always
  storing metadata `8`, so newly placed doors can actually use both hinge
  orientations.
- Corrected door floor support to use the same Release-era normal-top rule as
  other floor mechanisms. Wooden and iron door placement now rejects glass,
  chests, stairs, and other non-normal blocks below the lower half, and
  existing doors pop when that normal support is replaced.
- Corrected upper-door item drops. Breaking the top half of a valid paired
  door still removes both halves and drops one door item through the lower
  half, but orphaned upper halves now vanish without producing a door item,
  matching the source `BlockDoor` upper-half `null` drop. Orphaned upper
  halves also no longer clear unrelated blocks below them, and lower halves now
  require a real upper-half partner for support instead of accepting any
  same-type door block above.
- Corrected trapdoor side-anchor support to use the source trapdoor predicate
  instead of the broader generic attached-block predicate. Trapdoors now accept
  opaque full blocks plus glowstone, slabs, and stairs as anchors, while
  rejecting glass and chest-style non-normal anchors.
- Corrected fence-gate placement support. Gates now require a buildable block
  below when placed, matching `BlockFenceGate.canPlace`, while already placed
  gates still survive later floor removal because the source physics callback
  only handles redstone state changes.
- Corrected closed fence-gate collision to use the source single 1.5-block-tall
  collision strip for each axis instead of reusing the multi-part visual gate
  model and leaving post/bar gaps in the blocking volume. Open gates still have
  no collision, but now keep visible swung gate leaves attached to the posts
  instead of visually collapsing down to posts only.
- Corrected stone-button side-anchor support to use the source normal-block
  predicate instead of the broader attached-block helper. Buttons still keep
  the Release metadata/power-face mapping, but now reject glass and chest-style
  anchors while accepting normal cube anchors such as furnaces.
- Corrected lever wall, floor, and ceiling support to use source-style normal
  block anchors instead of the broader attached-block helper. Levers still keep
  their Release metadata variants and strong-power face mapping, but now reject
  glass and chest-style anchors while accepting normal cube anchors such as
  furnaces.
- Corrected lever selection/render bounds to the Release 1.0 source metadata
  table. Wall levers now use the old 0.2..0.8 height and 3/8-block protrusion
  on the correct side, floor levers use the wider 0.25..0.75 footprint with
  0.6-block height, and ceiling levers now occupy the upper 0.4..1.0 slab
  instead of sharing the floor shape.
- Corrected torch and redstone-torch selection/render bounds to the Release
  1.0 source metadata table. Wall torches now use the old 0.15 inset, 0.2..0.8
  height, and 0.3-block protrusion, while standing torches use the old centered
  0.1 inset and 0.6-block height.
- Corrected ladder and wall-sign selection/render bounds to their Release 1.0
  source metadata tables. Ladders now use the old 1/8-block wall thickness and
  metadata `4/5` side placement, while wall signs now use the source
  0.28125..0.78125 vertical band, full wall width, and 1/8-block thickness.
- Corrected standing-sign ray-selection bounds and chunk rendering toward the
  Release 1.0 `BlockSign` source model. Selection still uses the old centered
  `0.25..0.75` footprint through full block height while collision remains
  empty, and the chunk mesh now renders the source-sized 24x12x2 board plus
  2x14x2 post through all 16 metadata rotations instead of collapsing diagonal
  signs into two axis-aligned board buckets.
- Corrected vine selection/render bounds to the Release 1.0 side-bitmask
  source table. Single-side vines now use the matching 1/16 wall strip,
  multi-side vines expand through the same source bounding-box accumulation,
  and metadata-zero vines hanging from a solid block now use the old upper
  1/16 slab instead of an arbitrary side plate.
- Corrected crop, pumpkin/melon stem, and nether-wart selection/render bounds
  to their separate source tables. Wheat crops and nether wart now use the old
  full-block footprint with quarter-block height, while pumpkin and melon stems
  use the old narrow centered stem box with age-dependent height.
- Corrected Release-era Nether wart growth gating. Nether wart still plants
  and validates support on soul sand outside the Nether, but only Nether
  worlds advance its age toward maturity, preserving the old brewing-farm
  progression instead of allowing free Overworld wart farms.
- Corrected ground-cover plant selection/render bounds to their separate
  Release-era source families. Flowers now use the `BlockPlant` narrow
  `0.3..0.7` footprint with `0.6` height, mushrooms use the same footprint
  with `0.4` height, and saplings, tall grass, and dead bushes use the wider
  `0.1..0.9` footprint with `0.8` height instead of one shared local box.
- Corrected snow-layer collision bounds to the Release 1.0 metadata formula.
  Render/selection height still uses `2 * (metadata + 1) / 16`, but collision
  now uses the old `metadata / 8` height: metadata `0` is non-colliding,
  metadata `2` collides at 1/4 block, and metadata `7` collides at 7/8 block.
- Corrected glass-pane and iron-bar collision/selection bounds to the Release
  1.0 `BlockThin` source rules. Collision now uses the old per-axis collision
  boxes, including the two-strip isolated-pane cross and half-block one-sided
  connections, while selection uses the single source `updateShape` bound
  instead of the decorative render boxes.
- Corrected Nether portal render bounds to the Release 1.0 `BlockPortal`
  source table. Axis metadata still drives the plane orientation, but the
  visual plane now uses the old centered 4/16-thick slab (`0.375..0.625`)
  instead of the too-thin 2/16 local approximation.
- Corrected cactus ray-selection bounds to stay source-default full-block
  while preserving the old 1/16-inset, 15/16-high collision/contact box and
  narrow render proxy. This separates the default `Block` selection path from
  the `BlockCactus` collision override instead of reusing one narrow box for
  every shape role.
- Corrected lily pad collision/selection/render bounds to the Release 1.0
  `BlockWaterLily` source thickness. Lily pads now use the old full-footprint
  `1/64`-block-high plate instead of the too-tall `1/16` local slab, while
  keeping level-0 water support and boat-clearing behavior intact.
- Corrected brewing-stand collision bounds to the Release 1.0
  `BlockBrewingStand` source boxes. Physical collision now uses only the old
  center rod and full-footprint 1/8-block base, while the local decorative
  five-box render proxy remains separate.
- Corrected cake collision bounds to the Release 1.0 `BlockCake` source AABB.
  Cake selection/render still use the old 1/2-block-high `updateShape` box,
  but physical collision now uses the shorter 7/16-block height while keeping
  the same west-shrinking bite metadata footprint.
- Corrected cobweb movement storage to match the source `Entity.as()` path.
  Entities and players still move with the old 0.25 horizontal / 0.05 vertical
  damped movement attempt while inside a web, but their stored velocity is now
  cleared after that move instead of leaking residual web momentum into later
  ticks.
- Corrected player and living-entity ladder/vine movement toward the Release
  1.0 `EntityLiving.h_()` path. When touching ladders or vines, player
  horizontal velocity and living-entity horizontal motion are now
  component-clamped to the old `0.15` blocks/tick scale, idle falling is capped
  at the same downward speed, and horizontal collision while on a climbable now
  applies the source upward climb bump (`0.2` blocks/tick, scaled to player
  velocity units). Sneaking now holds downward climbable motion instead of
  forcing descent, and jump/forward/back keys no longer synthesize vertical
  climb motion without the source horizontal-collision path.
- Corrected openable movement sound pitch. Doors, trapdoors, and fence gates
  still emit the old `random.door_open`/`random.door_close` cues, but now use
  the source `0.9 + rand*0.1` pitch band instead of fixed-pitch playback.
- Corrected piston movement sound pitch. Extension now emits
  `tile.piston.out` with the old `0.6 + rand*0.25` pitch band, and retraction
  emits `tile.piston.in` with the old `0.6 + rand*0.15` pitch band, instead of
  both mechanism sounds playing at a fixed pitch.
- Corrected bed explosions outside the Overworld to use the shared flaming
  explosion path. The bed halves are removed, the normal power-5 explosion ray
  pass runs, the shared animated explosion particle is emitted at the bed
  head, and follow-up fire is now limited to positions reached by that
  affected-block set instead of a separate fixed cube scatter around the head
  block.
- Corrected pressure plate scan cadence and bounds. Stone and wooden plates now
  use the old 1/8-block inset entity search box, keep the pressed state until
  the 20-tick rescan, test dropped-item boxes instead of dropped-item center
  points for wooden-plate activation, and emit their `random.click` cue from
  the low plate surface at `y + 0.1` instead of the block center.
- Corrected floor-mounted redstone device placement support. Redstone dust
  now keeps the old glowstone exception while rejecting glass and chest-style
  top anchors, and rails, powered/detector rails, repeaters, plus stone/wooden
  pressure plates now require normal top support instead of inheriting the
  broader attached-block predicate used by torch/sign-style blocks.
- Corrected redstone-triggered dispenser activation timing. Dispensers now use
  the old 4-tick scheduled activation delay instead of firing on the generic
  one-tick mechanism update.
- Corrected dispenser success versus empty sound effects. Empty activation now
  keeps the old high-pitch `random.click` cue, successful generic item ejection
  emits the lower-pitch click effect, and arrow/egg/snowball/splash-potion
  launches emit the old `random.bow` effect without also playing the empty
  click.
- Corrected dispenser transport items back to Release 1.0 behavior. Boats and
  rideable/chest/furnace minecart stacks now eject as ordinary item entities
  even when water or rails are directly in front; automatic vehicle placement
  was a later Java behavior.
- Corrected minecart ascending-rail acceleration to the old `0.0078125`
  downhill step instead of a rounded approximation, with focused coverage for
  all four ascending rail directions and the post-tick empty-cart drag result.
- Corrected post-move slope speed adjustment. Minecarts now sample rail path
  height before and after the movement step and apply the old
  `(oldY - newY) * 0.05` horizontal speed adjustment across slopes and
  slope-to-flat transitions.
- Corrected minecart vertical placement after rail movement. Carts now resample
  the rail path after moving so slope traversal and slope-to-flat transitions
  update the entity's bottom-center `y` instead of leaving it at the pre-move
  rail height.
- Corrected minecart item placement on ascending rails. World-level minecart
  placement now applies the old half-block slope lift when the clicked rail is
  ascending, while flat, curved, detector, and powered rails keep the existing
  flat placement height.
- Corrected ordinary rail shape recalculation so curve metadata `8/9` is never
  treated as the powered-rail/detector-rail power bit. Normal rails now reshape
  from old north-west/north-east curves back to straight track when neighbors
  change, while detector and powered rails still preserve or recompute their
  actual power bit.
- Corrected furnace minecart push-vector storage to match the old raw
  `cart - player` delta. Engine force still normalizes the vector before
  acceleration, same-position use now clears stale direction, and save/load
  preserves raw `PushX`/`PushZ` state.
- Corrected furnace minecart non-fuel interaction. Empty-hand or non-fuel use
  now still refreshes the raw push direction without adding fuel or consuming
  items, while coal/charcoal use still adds the old 3600-tick fuel duration.
- Corrected removed minecart entity interactions. Player use now rejects
  already-removed storage and furnace carts before opening a chest screen,
  changing push direction, consuming coal, or starting the hand-use animation.
- Corrected furnace minecart engine force timing and constants. Powered furnace
  carts now apply their source-shaped post-move rail step by damping horizontal
  motion to `0.8`, adding `0.05` along the normalized push vector, and using
  the old `0.98` drag when no push force is active instead of ordinary empty
  minecart drag.
- Added powered furnace minecart exhaust feedback. Fueled furnace carts now
  spawn the old intermittent large-smoke puff at the cart after its movement
  update, and the smoke stops immediately once the final fuel tick is consumed.
- Corrected unpowered powered-rail braking. Very slow carts now stop at the
  old `0.03` horizontal-speed threshold, while faster carts are halved by the
  rail brake instead of creeping through a near-zero state.
- Corrected mounted minecart starts on unpowered powered rails. A near-stopped
  occupied cart that receives forward rider input now keeps the old passenger
  nudge instead of immediately losing half of it to the unpowered rail brake.
- Corrected minecart rail speed limiting to clamp each horizontal component to
  the old `0.4` movement cap independently instead of scaling the whole X/Z
  vector or overwriting stored motion, preserving source-style diagonal motion
  after curve projection.
- Corrected curve-rail motion projection. Curved rails now use the Release
  rail matrix and flip the projected direction from incoming motion instead of
  forcing one hardcoded diagonal per curve metadata.
- Corrected pre-move minecart rail-path positioning. Curved rails now project
  the cart's horizontal position onto the Release rail matrix segment before
  movement instead of only snapping straight rails to their centerline.
- Corrected post-move minecart direction realignment. After a cart crosses
  into a neighboring rail cell, stored horizontal motion now points along the
  crossed block direction before any powered-rail boost is applied.
- Corrected occupied minecart rail movement. Passenger carts now use the old
  75% displacement multiplier for the current rail step while still retaining
  their stronger occupied-cart post-step drag.
- Corrected powered-rail acceleration timing. Active powered rails now move the
  cart using its current motion, apply rail friction, then add the `0.06`
  powered boost or stopped-cart launch motion for the next tick.
- Corrected off-rail minecart fallback physics. Carts with no rail under/near
  them now use the old minecart-specific `0.04` gravity, per-axis `0.4`
  horizontal clamp, `0.5` ground damping, and `0.95` airborne drag instead of
  generic entity physics.
- Corrected minecart yaw updates on rails. Rail ticks now preserve the cart's
  yaw unless horizontal displacement exceeds the old `0.001` squared-distance
  threshold, preventing stopped and tiny-nudged carts from rotating to an
  artificial direction that then affects rendering and collision filtering.
- Corrected minecart side-contact filtering. Minecart-to-minecart collisions
  now use the cart yaw axis to ignore contacts whose relative direction is not
  aligned with the cart's travel axis, matching the old `abs(dot) >= 0.8`
  gate.
- Corrected minecart collision impulse scaling. Valid minecart contacts now
  apply the old `min(1, 1 / distance) * 0.05` push vector instead of a fixed
  impulse for every collision.
- Corrected vehicle explosion damage. TNT, End crystal, bed, and other world
  explosions now run boats and minecarts through the same Release-style
  exposure/damage curve used by other entities, so blast-destroyed boats drop
  planks/sticks and blast-destroyed rideable, chest, and furnace carts drop
  their legacy components and chest contents instead of ignoring the explosion.
- Corrected mounted vehicle dismount placement. Rideable minecarts, boats, and
  saddled pigs now try nearby clear player boxes before falling back to the old
  default side, so dismounting beside rails or docks avoids embedding the
  player in loaded solid blocks when another side is open.
- Corrected Release 1.0 furnace fuel container handling. Lava buckets now start
  the old 20,000-tick burn and leave an empty bucket in the fuel slot instead
  of being destroyed outright.
- Corrected runtime ground-cover support to match Release-era plant rules used
  by generation. Flowers, tall grass, and saplings can now stay on farmland,
  and small mushrooms now use the source-shaped mycelium-or-covered-low-light
  opaque-support rule instead of a short hardcoded support list.
- Corrected world-backed Overworld creature population RNG drift. Weighted
  creature-type selection still consumes the world random stream, while group
  size, start position, drift, and yaw stay on the chunk population random; the
  deterministic Release-one proxy now primes the world stream before the first
  source-style creature pick so the locked cow, taiga wolf, and mooshroom
  live-spawn fixtures line up again.
- Separated Release 1.0 explosion resistance from block break hardness. The
  explosion ray path now consumes explicit resistance values for air, fluids,
  obsidian/unbreakable blocks, stone/brick families, wood families, bookshelves,
  dirt/sand/gravel, ores, glass, and chests instead of reusing mining hardness.
- Added Release 1.0 mining hardness values for common survival blocks and
  routed player block-breaking through them. This fixes the remaining break
  speed input mismatch for stone, dirt/grass, sand/gravel, wood, ores,
  sandstone, glass, chests, bookshelves, stone brick, and Nether brick while
  leaving the existing explosion-resistance behavior isolated for a later
  source pass.
- Corrected special tool block-breaking strength inputs: swords and shears now
  cut cobwebs at the old 15x rate, shears cut leaves at 15x, and shears cut
  wool at 5x before the Release 1.0 block-strength formula is applied.
- Corrected survival block-breaking progress to use the Release 1.0
  block-strength formula: harvestable blocks advance by tool strength divided
  by hardness and 30 ticks, non-harvestable blocks use the old 100-tick
  penalty, and underwater/airborne mining each apply the old 5x slowdown.
- Wired Aqua Affinity into the survival block-strength path. Helmet Aqua
  Affinity now removes the underwater 5x mining penalty while preserving the
  separate airborne 5x penalty, matching the old enchantment-specific mining
  rule.
- Wired Respiration into the breath countdown. Helmet Respiration now uses the
  Release-era per-tick chance to skip underwater air consumption, while
  unenchanted helmets keep the normal 300-tick/15-second air drain.
- Confirmed note blocks correctly refuse to play when the block above is not
  air, matching the Release-era interaction rule.
- Corrected note-block instrument mapping against the Release-era material
  identity checks. Enchanting tables and End portal frames now select bass
  drum as `Material.STONE`; piston blocks, lapis blocks, iron bars, and monster
  eggs no longer falsely select bass drum; glowstone now selects the
  shatterable/glass sticks instrument; diamond ore, mossy cobblestone, and
  obsidian now select bass drum; and stone/wood pressure plates map to their
  source materials.
- Fixed the note block and jukebox save/load fixture so it explicitly clears
  the block above the note block before testing playback-state persistence.
- Confirmed `SaveManager` already persists note pitch, last instrument,
  playback ticks, jukebox record, and jukebox playback ticks.
- Expanded the default save/load fixture to cover dispenser tile inventory and
  facing metadata, and corrected the standing-sign portion of that fixture to
  use valid support so neighbor updates cannot turn the save test into an
  unsupported-sign drop case.
- Tightened sign text filtering to the Release-era font-backed character
  surface instead of accepting every printable byte below 256. Sign editor
  input and direct tile storage now reject non-font extended characters such
  as `\u00C0` and `\u20AC` while preserving old extended glyphs such as
  `\u00E9`, then clamp each line to 15 characters.
- Added in-world sign text rendering for placed sign tile entities. Standing
  and wall signs now draw their four stored lines onto the sign face using the
  bundled bitmap font texture, centered line layout, black text tint, and the
  existing scene depth/fog path instead of only showing text inside the editor.
- Corrected bed occupied metadata to the old head-half-only layout. Successful
  bed use now sets the occupied bit on the head block, keeps/normalizes the
  foot block clear, and still clears occupied state after wakeup.
- Corrected malformed bed-pair breaking. Breaking a valid paired head still
  removes both halves and produces the single bed item through the paired
  block path, but orphaned head halves now vanish without dropping a bed and
  no longer clear an unrelated block at the predicted foot position. Malformed
  foot halves also no longer delete unrelated bed blocks at their predicted
  head position; only a matching head half with the same facing is cleared.
- Added the Release-style note particle emitted above a note block on
  successful manual, punched, or redstone-triggered playback. Note particles
  now carry the old `pitch / 24.0` color payload separately from movement and
  render with the source sine-wave tint; blocked note blocks still reject
  playback without spawning particles.
- Corrected live jukebox record state. Player/live insertion now sets the old
  inserted-record block metadata, ejection clears it back to empty, emits the
  existing record-pop cue, and spawns the record item from the source-style
  randomized offset with a small upward/random horizontal impulse and the old
  10-tick item pickup delay. Insertion and ejection now follow the old
  block-metadata gate: records only insert when the jukebox metadata is empty,
  and block use only ejects when metadata says a record is inserted. Breaking
  or replacing a recorded jukebox now also ejects the stored record through
  that same pop/drop path before the tile entity is removed, while still
  dropping the jukebox item on ordinary block breaks.
- Added jukebox metadata reconciliation during tile/chunk restore, so older or
  tile-only saves with a stored record but empty block metadata relight the
  old inserted-record state when the chunk is loaded.
- Reviewed the current End crystal, dispenser, and piston coverage. These are
  not empty placeholders: End crystals have generation/healing/explosion
  coverage, dispensers already fire arrows and throw eggs, snowballs, and
  splash potions while ejecting TNT as an item, and pistons already cover
  moving blocks, sticky short pulses, immovable blocks, fragile blocks, entity
  pushing/damage, and quasi-connectivity. They still need source-audit passes,
  but they are not the first missing systems.
- Corrected piston pushability for signs. Standing and wall signs are source
  `BlockContainer`/`IContainer` tile-entity blocks, so pistons now refuse to
  extend into them instead of treating them as fragile blocks and popping them
  into sign drops.
- Reviewed the crafting/smelting/fuel/brewing registries. The crafting table
  already has a broad Release 1.0 recipe surface, including the old output
  counts for doors, trapdoors, ladders, slabs, stairs, food, dyes, brewing
  ingredients, and End-era recipes. This pass keeps `NIGHT_VISION` as an
  unobtainable legacy status-effect id while excluding it from the
  brewable/creative potion identity enum, matching the Release 1.0 potion
  surface.
- Corrected dispenser crafting to keep the old undamaged-bow requirement.
  Stack-grid matching now rejects the dispenser recipe when the bow slot holds
  a damaged bow, while the ordinary type-grid helper remains available for
  recipe table audits.
- Tightened the furnace smelting audit to enforce the exact Release 1.0 recipe
  surface currently represented in CraftZero: ores, logs-to-charcoal, raw
  foods, sand, cobblestone, clay balls, and cactus are covered, while later or
  invalid inputs such as clay blocks, netherrack, and stone are rejected.
- Corrected the ghast-tear and magma-cream brewing branch: water bottles now
  make the Release 1.0 mundane base potion, while Regeneration and Fire
  Resistance still require Awkward potions. The older mundane/weakness paths
  remain covered.
- Corrected splash potion application edges. Non-instant splash effects now
  follow the old 21-tick minimum duration cutoff instead of applying tiny
  one-tick effects at the edge of the radius, and direct entity hits apply full
  effect strength even when the target center is offset from the impact point.
- Corrected splash potion detonation coordinates. Entity, player, and block
  impacts now move the potion to the raycast hit point before radius strength
  is calculated, so nearby targets are affected from the actual collision point
  instead of the projectile's pre-tick position.
- Corrected thrown egg/snowball impact coordinates. Entity, player, and block
  impacts now move the projectile to the raycast hit point before hatching,
  damaging, destroying End crystals, or removing the projectile, matching the
  hit-point behavior already used by splash potions and fireballs.
- Routed public/world-spawned egg hatch rolls through the owning world's RNG
  instead of projectile-local random state, while preserving injectable random
  sources for focused hatch-count tests.
- Corrected player attack potion modifiers to use the Release-era bit-shift
  values: Strength adds `3 << amplifier` damage and Weakness subtracts
  `2 << amplifier`, instead of leaving Weakness far below its old combat
  penalty.
- Corrected Enderman melee and death-drop behavior. Endermen no longer reuse
  zombie melee damage for their attack goal, and their death-loot path now
  stays limited to Ender Pearls instead of converting the carried block state
  into an item drop.
- Corrected Enderman stare targeting for pumpkin helmets. A player wearing a
  pumpkin in the helmet slot no longer advances the Enderman stare-aggro path,
  while the same direct stare without a pumpkin still makes the Enderman angry.
- Corrected Enderman carried-block placement to use the carried block's own
  placement rule after confirming the target cell is empty, so cactus, flowers,
  mushrooms, and similar carried blocks no longer use a generic "solid block
  below" shortcut.
- Added Enderman teleport feedback. Successful arrow, wet-damage, or combat
  teleports now emit the source-style 128 portal particles along the
  previous-to-new position path and play the old endpoint teleport cue.
- Corrected arrow despawn timing. The old 1200-tick despawn gate now applies
  only to arrows stuck in blocks; airborne arrows no longer vanish merely
  because the stuck-arrow timer length elapsed while they were still flying.
  Saved in-ground arrows now reject `stuckTicks >= 1200` before restore, so
  expired collectible arrows cannot be revived from save data, and airborne
  arrows now reject nonzero stuck timers before restore.
- Wired Looting into player-kill mob drops. Player melee damage now carries
  the held sword's Looting level through the damage source, and shared mob
  drop ranges use that level when the accepted killing hit has player credit.
  Shared mob drop counts now use the Release-style separate base roll plus
  additive Looting roll instead of one flattened enlarged random range.
- Corrected Squid ink-sac drops to keep their own Release-style single
  `1 + random.nextInt(3 + looting)` count roll instead of inheriting the
  shared mob base-plus-Looting helper.
- Corrected stranded Squid motion toward the Release water-mob path. Out of
  water they now stop horizontal motion, ease pitch toward straight down at
  the old rate, animate tentacles from the absolute rotation sine, and no
  longer take invented generic land damage. Stranded Squid now instead keep
  the Release air countdown, taking 2-point dry-out damage at the `-20`
  threshold until they return to water or die.
- Wired Silk Touch and Fortune into player-style block drops. Player mining now
  passes the actual enchanted tool stack into the drop resolver instead of only
  the item id, Silk Touch returns collectable block items before normal drops,
  and Fortune now affects leaf sapling chance, gravel flint chance,
  coal/diamond/lapis ore multipliers, redstone additive dust counts,
  glowstone/melon capped counts, and crop/stem extra seed rolls with focused
  resolver and world-break coverage. Tall grass now also uses the old
  Fortune-expanded seed-attempt count before running its `1/8` seed rolls. Oak
  leaves now explicitly stay on the Java Release 1.0 sapling-only drop path:
  the later Java 1.1 oak-apple leaf drop and the later Java 1.5 Fortune apple
  boost are excluded from this target. Dead bushes now explicitly stay on the
  Release 1.0 no-drop path even when cut with shears; the later shears harvest
  behavior remains excluded from the target.
- Tightened Silk Touch collection to the Release 1.0 block set. Ice and glass
  panes now stay on the no-drop glass-family path even when mined with Silk
  Touch; ice collection had already been removed before Release 1.0, and glass
  pane collection is a later post-1.0 behavior.
- Corrected leaf decay metadata semantics. Leaves now distinguish the old
  player-placed/no-decay bit (`4`) from the check-decay bit (`8`), so generated
  leaves do not decay merely because a scheduled tick reaches them. Log removal
  and neighboring block changes mark non-persistent leaves for decay, connected
  marked leaves clear the check bit, disconnected marked leaves decay, and
  marked leaves at chunk edges wait until their full radius is generated.
- Corrected status-effect merging for players and living entities. Stronger
  active effects now survive weaker longer reapplications, while same-amplifier
  reapplications can still extend duration, matching the old potion combine
  rule.
- Corrected Regeneration and Poison amplifier ticking and mob applicability.
  Higher amplifiers now speed up the old separate tick cadences
  (`50 >> amplifier` for Regeneration, `25 >> amplifier` for Poison), while
  each ready tick still heals or damages one point instead of scaling the
  amount. Timed Poison/Regeneration now reject undead mobs, spider-family mobs
  reject Poison, and instant Healing/Harming keeps the old undead inversion.
- Corrected instant Healing/Harming potion amounts and damage source behavior.
  Beneficial instant effects now use the old `4 << amplifier` amount, harmful
  instant effects use `6 << amplifier` magic damage with splash-strength
  rounding, and undead living entities still invert heal versus harm.
- Re-checked the high-risk registry/recipe exposure surface after broadening
  the parity goal. No obvious post-1.0 items such as emeralds, hoppers,
  comparators, anvils, horses, witches, bats, or spawn eggs are present in the
  item/block registries. Spawn eggs are intentionally treated as post-1.0
  content and locked out by focused registry coverage. The legacy fire-based
  chainmail recipes are intentionally left as old command/editor-path recipes:
  `/give 51` can still produce the fire item, but the creative catalog no longer
  exposes fire as ordinary inventory content.
- Corrected the golden apple item max stack size to the Release-era stack of
  64, so inventory splitting, `/give`, creative stacks, and loot stack handling
  no longer treat it as a single-stack item.
- Added stack-aware dynamic crafting repair. Two count-one matching damageable
  item stacks now craft into a fresh single item with combined remaining
  durability plus the Release 1.0 5% max-durability bonus, capped at max
  durability, while static recipe count remains at 174.
- Corrected huge mushroom cap block drops. Brown and red mushroom block caps no
  longer drop collectible cap blocks through the default path; they now use the
  old sparse 0-2 matching mushroom roll.
- Added Release 1.0 cow/mooshroom entity interactions. Empty buckets now milk
  cows and mooshrooms into milk buckets, bowls fill from mooshrooms into
  mushroom stew, and shears convert mooshrooms into cows while dropping five
  red mushrooms and damaging the shears in survival. Baby cows/mooshrooms now
  reject those adult-only milk, bowl, and shear interactions without consuming
  the held container/tool, and sheared mooshrooms transfer current health to
  the replacement cow instead of healing to full.
- Added Release 1.0 pig saddle interaction state. Saddles can be applied to
  unsaddled adult or baby pigs, saddled pigs can be mounted/dismounted by the
  local player, saddle state now round-trips through save/load, and saddled-pig
  death loot now stays on the Release 1.0 zero-to-two porkchop path without
  returning the saddle item. Accepted one-saddle use now still starts the
  hand-use animation after the stack is consumed, while invalid
  unsaddled/missing-saddle use remains inert.
- Corrected the shared successful item-use consumption path so last-item
  placements/uses still start the hand-use animation after the stack empties.
  This covers one-count block placement, crops, paintings, minecarts, boats,
  End portal eyes, and other paths that route through the common placement-use
  helper; rejected placements remain inert. Successful clicked-block toggles
  such as jukebox ejection, note-block pitch cycling, levers/buttons,
  openables, and fence gates now also start the player hand-use animation on
  the real right-click path.
- Corrected regular spider loot to include the Release 1.0 spider-eye roll in
  addition to string, matching cave spider parity instead of leaving only cave
  spiders with spider eyes.
- Corrected spider provocation source handling. Entity-caused damage now keeps
  spiders hostile in bright light, while environmental/generic damage can hurt
  them without permanently setting the provoked state.
- Corrected bright-light spider chase persistence. Daylight still prevents
  neutral spiders from acquiring new player targets, but spiders that are
  already chasing now only lose interest on the old 1-in-100 bright-light roll
  instead of clearing their target every tick.
- Corrected spider wall-climbing to reuse the Release-style climbable physics
  path when horizontal collision is active. Wall-climbing spiders now get the
  old upward bump and do not carry stale pre-climb fall distance into short
  post-climb landings.
- Corrected slime and magma cube split placement to use the old half-block
  child spawn lift, and locked large slime/magma cube deaths to 2-4 same-family,
  half-size children with focused coverage.
- Routed Eye of Ender break/drop chance through the player's deterministic RNG
  instead of process-global `Math.random()`, preserving the old 80% drop /
  20% shatter rule while making both progression outcomes testable.
- Corrected Eye of Ender locator steering to use a short rising Release 1.0
  waypoint when the stronghold is far away, rather than accelerating directly
  toward the final stronghold coordinates for the full 80-tick lifetime. The
  active flight step now uses the old source-shaped horizontal acceleration,
  vertical pull, near-target damping, one-particle portal trail, and
  `age > 80` drop-versus-shatter boundary. Saved Eyes of Ender now reject age
  `> 80` and non-finite target coordinates before restore, so expired or
  malformed locator projectiles cannot be revived.
- Added the Release-style Eye of Ender portal-particle flight trail through
  the existing transient particle system, with focused coverage that the
  locator emits portal particles while travelling. Eyes of Ender now switch
  that trail to the old four-bubble water branch when the projectile is inside
  water instead of continuing to emit portal particles underwater.
- Corrected the missing failed-roll Eye of Ender expiration feedback. The 20%
  shatter branch now emits the old eye item-crack chips plus the two inward
  portal rings at the rounded end position instead of disappearing or showing a
  tiny generic puff.
- Completed XP orb RNG routing: injectable tests still control launch/fizz,
  public orbs initialize launch from the owning world's RNG when attached, and
  restored orbs keep saved motion while using the world RNG for later lava fizz.
- Routed idle look-at-player, wander target selection, panic flee selection,
  and ranged skeleton strafing randomness through the mob-owned RNG instead of
  process-global or goal-local random state, preserving the existing
  probabilities while making AI decisions replayable and save-auditable.
- Completed dropped-item visual RNG routing: injectable tests still control
  spin/bob state, public drops initialize deferred visuals from the owning
  world's RNG when attached, and restored drops keep saved animation state
  without rerolling.
- Corrected sheep wool drops to preserve the sheep's stored wool color on
  death and shearing instead of always dropping white wool, and added the
  matching Release 1.0 source-style fleece tint path for rendered sheep fur.
  Sheep shearing now uses a non-Looting interaction drop path, so recent
  player-kill Looting credit cannot inflate the old 1-3 wool shearing range.
  Player dye interaction now also changes unsheared sheep to the Release-era
  inverse dye metadata wool color, consumes dye only outside Creative, rejects
  sheared or unchanged-color sheep without applying the dye or falling through
  to block use, and lets later shearing/death drops use the dyed fleece color.
- Corrected mob distance despawning so Release 1.0 passive/utility mobs such
  as pigs and Snow Golems do not vanish under the monster despawn rule, while
  monster-category mobs use the old despawn thresholds: immediate removal past
  128 blocks, random old-age despawn outside 32 blocks after 600 ticks, and age
  reset inside 32 blocks.
- Corrected undead daylight burning to use the old bright-sky random ignition
  gate instead of setting zombies and skeletons on fire every open-daytime tick;
  successful rolls still apply the Release-style eight-second burn duration,
  while active rain now counts as wetness for extinguishing and prevents
  daylight re-ignition in exposed rainy columns.
- Corrected baby mob death/shearing behavior: baby animals no longer drop
  adult loot or player-kill XP, and baby sheep now reject shearing instead of
  producing wool.
- Added Release 1.0 wheat breeding for cows, mooshrooms, pigs, sheep, and
  chickens. Eligible adults enter love mode from wheat, nearby matching mates
  now use the old 60-tick courtship delay before spawning a baby at the
  initiating parent, parents receive the old cooldown age, babies reject
  breeding, and love-mode ticks now persist through save/load.
- Added Release 1.0 animal breeding polish: love-mode animals now emit
  periodic in-world heart particles plus the old seven-heart birth burst, and
  cows, mooshrooms, pigs, sheep, and chickens follow the player while the
  selected hand item is wheat.
- Corrected Release 1.0 love-state interruption edges: repeated wheat use on
  eligible adults now refreshes love mode and emits another immediate heart
  burst, while accepted damage clears the love timer and pending courtship.
- Added love-mode mate seeking for ageable animals: compatible wheat-fed
  animals now steer toward nearby in-love mates while the source-style
  courtship timer runs, and breeding no longer creates XP orbs.
- Corrected an End crystal entity edge from CraftZero's living-entity
  implementation: crystals now ignore potion/status effect state and healing
  APIs, matching vanilla crystals as non-living entities while preserving their
  existing fire maintenance and explosion behavior.
- Corrected generated End crystal placement to use the source
  `WorldGenSpikes` cap coordinate instead of a one-block-high CraftZero offset.
  Spike crystals now spawn at the same Y as the generated cap and keep the old
  crystal yaw sequencing after the height/radius draws.
- Corrected End crystal fire maintenance so crystals recreate fire at their
  own floored block position in The End, replacing any non-fire block there as
  the old entity update did, while still avoiding arbitrary Overworld or Nether
  ignition.
- Corrected Ender Dragon death exit portal construction and timing. The portal
  now appears after the old 200-tick death sequence, centers on the dragon's
  death block X/Z, uses the fixed `y=64` circular bedrock/end-portal footprint,
  clears the tall source-shaped interior, and adds the central bedrock pillar,
  four torches, and dragon egg instead of creating a tiny hardcoded origin
  portal immediately.
- Corrected Ender Dragon death XP payout cadence. The dragon now drifts upward
  and releases 1,000 XP every 5 ticks from death tick 155 through 200, then
  releases the final 2,000 XP on tick 200, preserving the 12,000 total without
  dumping every orb only after the portal appears.
- Added the Release-style Ender Dragon death presentation cues. The death
  sequence now queues the old dragon end sound on death tick 1 and emits one
  huge-explosion particle per tick from death tick 180 through 200 using the
  bundled explosion texture instead of staying visually silent until the portal
  appears.
- Corrected thrown egg and snowball impacts against End crystals. Zero-damage
  thrown-item hits now route through the End crystal damage path, so crystals
  explode on Release-era projectile contact instead of ignoring non-Blaze
  thrown-item collisions.
- Locked direct arrow impact coverage against End crystals. Arrows now have
  focused projectile-contact coverage alongside fireballs, eggs, and snowballs,
  proving they route through the crystal destruction path instead of remaining
  only indirectly covered by generic living-entity damage.
- Corrected glass pane drops so panes follow the Release-era glass-family
  no-drop path instead of dropping collectible pane items through the default
  block-item fallback.
- Corrected Silk Touch glass-family edge cases so ice and glass panes do not
  become collectible through the enchanted tool path for the Release 1.0
  target.
- Corrected snow harvest gating so player snowball drops from snow layers and
  snow blocks require shovels even though those blocks have zero harvest level.
- Corrected special-block drop fallback so bedrock and End portal frames never
  report collectible drops through the generic block-item path.
- Corrected utility-block harvest gating so enchanting tables, brewing stands,
  and cauldrons require a pickaxe before player-style block breaking drops the
  block item.
- Corrected stone-control harvest gating so stone pressure plates and stone
  buttons require a pickaxe before player-style block breaking drops the item,
  while support-removal drops stay on the attachment path.
- Corrected netherrack harvest gating so bare-hand player-style breaking
  destroys it without handing out the block item; any pickaxe can still collect
  it.
- Added save/load support for scheduled block ticks. Runtime updates such as
  redstone, fluids, glowing redstone ore, fire, falling blocks, crops, snow,
  leaves, and other scheduled block behavior now serialize their remaining
  delay and resume after reload instead of silently disappearing.
- Corrected scheduled block tick bookkeeping so stale queued entries are removed
  when a block state changes, and world-level scheduling now passes metadata
  into redstone tick-delay calculation. Tuned repeaters no longer keep an old
  2-tick update after cycling to a longer delay.
- Corrected redstone/mechanism scheduled ticks near chunk edges. Redstone,
  pistons, buttons, plates, rails, dispensers, note blocks, and openables now
  tick against their existing loaded-safe neighbor queries even when adjacent
  horizontal chunks are not generated, preventing ordinary edge layouts from
  freezing while keeping the loaded-neighborhood guard for fluids and fire.
- Corrected deferred fluid/fire scheduled ticks at unloaded chunk edges. When
  the horizontal loaded-neighborhood guard blocks a due tick, the tick now
  reschedules with its metadata-aware delay instead of being discarded.
- Corrected the first redstone tick for restored openable metadata. Open
  unpowered wooden doors, trapdoors, and fence gates no longer close just
  because the runtime powered-state cache is empty after placement/load, and
  restored closed openables beside active redstone now preserve their saved
  metadata on the first scheduled tick until the signal changes.
- Corrected chest viewer-count bookkeeping for UI sounds. Chest open/close
  events now fire only when the tile crosses from zero to one viewer or from
  one viewer back to zero, preventing duplicate open sounds and premature close
  sounds while another viewer still has the chest open.
- Corrected silverfish wake-up behavior for nearby monster eggs. Hurt
  silverfish now clear awakened infested blocks as the new silverfish emerge
  instead of converting the egg back into ordinary stone.
- Added save/load support for moving piston runtime state. Mid-extension and
  mid-retraction piston blocks now preserve their carried block, final block,
  metadata, direction, movement endpoints, and elapsed movement ticks across
  reloads so they can finish settling correctly.
- Corrected restored moving-piston completion. Restored moving piston states now
  reschedule completion from persisted elapsed movement, settle by elapsed tick
  time even when ordinary dynamic ticks are backlogged, and avoid false
  retraction of restored extension heads while the relevant piston-power query
  chunks are still unloaded.
- Added save/load support for active projectile runtime state. Arrows,
  fireballs, ender pearls, thrown eggs/snowballs, and splash potions now
  preserve position, motion, lifetime, projectile type data, and key special
  state such as stuck-arrow pickup data and player-owned ender pearls.
- Corrected stuck-arrow block tracking. Player-owned arrows now remember the
  block type they embedded in and release back into flight when that block is
  removed or replaced, instead of remaining collectible from a stale block
  coordinate or vanishing as a zero-motion projectile.
- Added the old `random.pop` pickup cue for collecting player-owned stuck
  arrows, matching the dropped-item pickup feedback path.
- Expanded dropped item save/load so item entities preserve velocity,
  on-ground state, spin rotation, and bob animation phase instead of reloading
  as freshly spawned static drops.
- Added save/load support for transient physics entities. Falling sand/gravel
  entities and thrown Eyes of Ender now preserve block/target data, motion, and
  lifetime instead of vanishing on reload.
- Corrected falling sand/gravel lifetime outside vertical world bounds. Blocks
  below y=1 or past the world height now wait until the old 100-tick
  out-of-world age gate before dropping as items, while the exact top boundary
  can still fall back in and the hard 600-tick timeout still drops long-lived
  falling blocks.
- Split falling-block fall-through rules from the broader collision pass-through
  helper. Sand, gravel, and dragon eggs now start/continue falling only through
  air, fire, water, and lava like Release 1.0 `BlockSand`, while landed falling
  blocks can still replace fire, fluids, and ground-cover plants.
- Corrected falling-block item conversion to the source-shaped raw block-item
  path. Timed-out or broken falling gravel now drops gravel even when the
  ordinary broken-block resolver would roll flint, matching the old
  `EntityFallingSand` item drop behavior instead of treating falling-block
  conversion as player block breaking.
- Corrected dragon egg teleport search to use the old two-draw triangular
  offset stream (`nextInt(16)-nextInt(16)` horizontally and
  `nextInt(8)-nextInt(8)` vertically) and spawn the source-style 128 portal
  particles on successful teleport.
- Corrected locked chest scheduled ticks to match the legacy block source:
  when its update tick fires, the block now immediately turns into air without
  drops instead of rolling a long random decay chance and rescheduling itself.
- Added save/load support for several mob-specific runtime states: sheep
  sheared/color state, wolf angry/tamed/sitting state, and creeper
  fuse/ignition progress now survive reload.
- Added save/load support for active mob AI timers and animation state:
  chicken egg timers, slime/magma cube jump delays, blaze burst cooldowns,
  ghast fire/wander targets, squid swim/air/animation state, and Enderman
  stare/teleport cooldowns now survive reload.
- Added save/load support for passive panic goal state. Passive mobs and
  villagers that are already fleeing now preserve their panic timer and flee
  destination through reload instead of resuming as calm idle mobs.
- Added save/load support for hostile nearest-target goal timers. Hostile mobs
  now preserve their target scan cooldown, sight-loss memory, and target refresh
  cooldown through reload instead of restarting those private AI timers.
- Added save/load support for mob movement targets and melee attack goal state.
  Hostile mobs now preserve their current chase target coordinate plus melee
  path-recalculation cooldown, stuck counter, and last-progress sample through
  reload instead of resetting their pursuit cadence.
- Restored mob movement targets now also keep the target Y coordinate and seed
  the lazy navigator as soon as the mob is attached to a world, so pathing
  resumes by recomputing from the restored target instead of leaving the
  navigator idle after reload.
- Hardened level save loading against structurally partial `level.json` files.
  Supported-format saves missing critical player or inventory payloads now
  count as corrupt, letting the loader recover from `level.json.bak` instead
  of silently normalizing the save into lost player state.
- Hardened level save loading against structurally damaged runtime lists.
  Present dropped-item, tile-entity, entity, moving-piston, and scheduled-tick
  lists that contain null elements now count as corrupt, letting the loader use
  the previous backup instead of crashing during apply or dropping saved
  runtime state.
- Hardened level save loading against malformed saved object types. Unknown
  tile-entity ids, unknown entity ids, invalid saved mob definitions, and
  invalid falling-block ids now count as corrupt before apply, so a damaged
  primary save can recover from backup instead of silently dropping objects.
- Hardened level save loading against malformed saved item payloads. Non-null
  player inventory, cursor, tile inventory, jukebox record, minecart inventory,
  thrown-item, and dropped-item payloads with unknown item ids or non-positive
  counts now count as corrupt and recover from backup when available instead of
  silently disappearing during restore.
- Added explicit dropped-item pickup-delay state. Default world/entity drops
  now expose the old 10-tick pickup delay, player-thrown inventory drops can
  use the longer 40-tick delay, freshly merged item stacks keep the freshest
  remaining delay instead of becoming instantly collectable, and the delay now
  round-trips through save/load.
- Added dropped-item entity health and explosion damage. Dropped item entities
  now carry the old five-health damage state, explosions can destroy them
  through the shared exposure/damage pass, surviving blast-hit items receive
  velocity push, and item-entity health now round-trips through save/load.
- Corrected dropped-item fire/lava hazards to use the same five-health
  item-entity damage path. Fire now burns item entities down through their
  health, while lava applies the old stronger fire hit plus fizz sound and
  upward/randomized item bounce. Lava damage still applies on contact, but the
  decorative fizz/bounce now follows the old item-entity cadence instead of
  resetting every update: first observed lava contact, block-cell movement, or
  each 25th source tick.
- Added the missing runtime dropped-item merge scan. Nearby matching item
  entities that were restored, moved, or otherwise bypassed the spawn-time
  merge path now coalesce during the same block-cell/25-tick item cadence used
  by the old entity update loop.
- Corrected default dropped-item launch motion. Newly spawned item entities
  now use the world RNG for the old random horizontal pop plus upward launch
  instead of rising straight up, while explicit thrown/saved item velocities
  remain unchanged.
- Corrected dropped-item ground contact motion. Item entities now bounce with
  damped vertical motion and keep sliding under ground friction after landing
  instead of zeroing all velocity on first contact. Ground drag now uses the
  old item-entity slipperiness formula (`block slipperiness * 0.98`), including
  higher retained motion on ice. Airborne vertical motion now uses the old
  item-entity `0.04`-per-tick gravity equivalent plus `0.98` vertical drag
  before the ground bounce.
- Hardened chunk save/load against corrupted chunk files. Chunk writes now keep
  `.bak` copies of the previous binary chunk, and chunk loading falls back to
  that backup when the primary RLE file is malformed instead of regenerating
  terrain and losing saved block edits.
- Added save/load support for skeleton ranged-combat goal state. Active
  skeleton bow cooldown, strafe timer, strafe direction, and strafe speed now
  survive reload and resume on the next goal start instead of resetting to a
  fresh ranged-attack cadence.
- Added save/load support for the shared living-entity attack cooldown. Melee
  and contact attackers now preserve their post-hit delay across reloads
  instead of being able to attack immediately after a save/load boundary.
- Added save/load support for the Snow Golem's private snowball attack
  cooldown, so utility mobs do not reload into an immediate throw while their
  Release-style ranged attack delay was still active.
- Corrected magma cube behavior against the Release-era slime override: tiny
  magma cubes now damage players instead of inheriting harmless small-slime
  contact rules, contact damage is size plus two, jumps use the old
  size-scaled vertical impulse with longer idle delay, and magma cubes no
  longer expose the post-1.0 magma cream drop.
- Added the old creeper skeleton-kill record drop rule. Creepers killed by
  skeleton arrows now drop one of the Release 1.0 survival-obtainable records
  (`13` or `cat`) in addition to normal gunpowder rolls, while non-skeleton
  deaths still do not drop records.
- Corrected primed TNT runtime physics details: newly primed TNT now uses the
  old fixed 0.02 horizontal launch speed in a random circular direction,
  live fuse ticks emit smoke particles, and grounded TNT bounces with the
  legacy half-height vertical reversal instead of losing all vertical motion.
- Corrected explosion-primed TNT sound behavior. TNT blocks ignited by an
  explosion still receive the old short randomized fuse and launch impulse, but
  they no longer emit the manual/redstone `random.fuse` cue on top of the
  explosion sound.
- Corrected primed TNT save/load for the final live fuse tick. Source-style
  TNT can persist with fuse `0` for one smoking tick before exploding; reload
  now preserves that zero instead of inflating the fuse back to 80 ticks, while
  older saves without explicit TNT fuse data still fall back to 80.
- Corrected XP orb ground physics so orbs bounce upward with the old 90%
  vertical reversal after landing instead of losing all vertical motion through
  the generic entity collision path.
- Added the Release-style XP orb lava fizz cue. Orbs that bob in lava now emit
  `random.fizz` at the old low volume and high randomized pitch while retaining
  their lava upward kick and randomized horizontal motion. The lava reset now
  happens after the orb gravity decrement, matching the old tick order instead
  of losing part of the upward kick to same-tick gravity.
- Added save/load support for provoked spider state, so bright-light neutral
  spiders that were attacked stay hostile after reload instead of forgetting
  the player-facing aggression state.
- Expanded save/load entity cross-references to cover mob AI combat targets.
  Entity-backed targets now restore to the corresponding reloaded entity, with
  focused coverage for tamed wolf assist targets continuing to bite after
  reload.
- Added save/load support for Ender Dragon flight target/cooldown and
  in-progress death animation state without rerunning the boss death hook on
  reload.
- Expanded furnace save/load to preserve the fractional tick accumulator used
  by CraftZero's delta-time furnace ticking, so active furnaces resume
  smelting without losing partial tick progress after reload.
- Corrected furnace lit/unlit block reconciliation so restored or partial
  active states relight the furnace block on the next tick, and stale lit
  blocks cool back to normal without replacing the tile entity or losing
  facing metadata.
- Corrected the furnace cook-progress idle edge. When the last burn tick
  expires with smeltable input still present but no fuel left, partial cook
  progress now resets through the same source-style non-burning path as other
  invalid processing states.
- Corrected stale furnace cook progress for invalid input states. Removing the
  input stack, placing an unsmeltable input, or blocking the output slot now
  clears the partial cook timer instead of leaving a stale progress bar behind
  while the furnace is idle.
- Tightened jukebox record lifecycle behavior: inserting a disc now starts
  playback accounting from the beginning, and ejecting a disc clears playback
  progress so the next record cannot inherit stale play state.
- Expanded brewing stand save/load to preserve the fractional tick accumulator
  used by delta-time brewing ticks, so an active stand resumes without losing
  partial tick progress after reload.
- Expanded enchanting table save/load to preserve runtime book animation state,
  including spread, rotation, page flip, velocity, tick count, and fractional
  tick progress, so active table visuals resume after reload.
- Tightened dispenser output facing to the Release 1.0 horizontal-only model:
  north/south/east/west metadata is preserved, while top/bottom or invalid
  metadata falls back to north instead of emitting vertically.
- Added focused dispenser GUI coverage for container-style right-click stack
  splitting and shift-click player-inventory transfer into the 3x3 dispenser
  inventory.
- Hardened sign text storage so callers cannot bypass the Release 1.0
  four-line/15-character line invariant by mutating the array returned from the
  tile entity.
- Tightened sign editing behavior: the editor now accepts the full 0-255
  bitmap-font character range while rejecting control/delete and formatting
  marker characters, and screen open/close no longer leaves the cursor in the
  wrong lock state.
- Corrected furnace quick-transfer routing so smeltable fuel items such as logs
  go to the input slot before the fuel slot, preserving the vanilla charcoal
  workflow while still sending fuel-only items such as coal to fuel.
- Corrected direct furnace GUI slot placement to match the old plain input
  slot: the input slot now accepts any stack, while the fuel slot still accepts
  only fuel and the output slot remains output-only for cursor interactions.
- Corrected furnace GUI quick-transfer fallback for ordinary player items.
  Stacks that are neither smeltable nor fuel now move between the main
  inventory and hotbar instead of doing nothing while the furnace screen is
  open.
- Tightened brewing stand and enchanting table GUI merge paths so invalid
  stacks cannot bypass slot eligibility by merging into an existing special
  slot stack.
- Corrected brewing stand GUI slot semantics for the old container: bottle
  slots now accept potion stacks as one-count stacks, empty glass bottles stay
  out until filled into water bottles, ingredients quick-transfer as whole
  stacks only when the ingredient slot is empty, and occupied ingredient slots
  fall back to normal main-inventory/hotbar transfer.
- Corrected enchanting table GUI slot semantics. The old one-slot enchantment
  inventory accepts any item but caps the table slot at one stack item; ordinary
  non-enchantable items can sit in the slot and simply produce no enchantment
  offers, while quick-transfer moves one player item into an empty table slot.
- Completed the missing crafting-table quick-transfer path: either Shift key
  now handles output crafting, craft-grid item return, and main
  inventory/hotbar transfer while the table is open.
- Corrected mob spawner tile ticking to accumulate delta time into 20Hz
  spawner ticks and persist partial tick progress across reloads, preventing
  spawner delays from advancing too quickly under faster or fractional updates.
- Brought mob spawner spawn attempts closer to Release 1.0 source semantics:
  nearby entity caps now use the expanded 8x4x8 box around the spawner instead
  of a sphere, and spawn positions use independent old-style X/Z random
  offsets instead of polar radius placement.
- Corrected mob spawner block drops so breaking the block does not drop a
  collectible spawner item.
- Corrected natural hostile spawn light checks to include block light as well
  as time-adjusted sky light, so torch-lit spawn spaces are rejected at night.
- Corrected natural passive spawn light checks to use the same effective light
  model, so dark open grass at night no longer passes and torch-lit grass can
  satisfy the Release-style brightness threshold.
- Tightened mob spawner same-tick cap enforcement so mobs queued by earlier
  successful spawn attempts in the same spawner cycle count toward
  `maxNearbyEntities`, matching the loaded-entity-list behavior expected by
  Release-era spawners.
- Added hostile mob-spawner light gating so torch/glowstone-lit spawn positions
  reject hostile spawns instead of allowing dungeon spawners to run in bright
  spaces.
- Replaced mob spawner feet/head-only placement checks with entity
  bounding-box collision checks, so tall or wide mobs cannot spawn into
  ceilings, walls, entities, or other occupied spawn volumes.
- Corrected mob spawner floor/liquid semantics for non-water mobs. Hostile
  spawner mobs now follow the Release-era entity-volume path without requiring
  solid support under the selected point, while liquid blocks inside the
  bounding box reject zombie/skeleton/creeper-style spawns.
- Corrected mob spawner delay resets to use the Release-style exclusive upper
  bound, matching `min + random(max - min)` behavior instead of allowing the
  configured maximum delay value.
- Corrected mob spawner retry timing after failed spawn attempts. Once a
  spawner reaches zero delay, blocked or otherwise invalid spawn positions now
  keep the delay at zero and retry on the next active tick; only successful
  spawns or the nearby-entity cap start a fresh random delay.
- Routed mob spawner active particles, spawn position attempts, and delay resets
  through the owning world's RNG instead of tile-local random state, while
  preserving injectable scripted RNG for focused source-shape tests.
- Added passive creature mob-spawner checks so animal spawners require bright
  grass support instead of spawning pigs, cows, sheep, or wolves on arbitrary
  solid blocks.
- Added Release-era water creature spawner height checks so squid spawners only
  run in the old valid water band instead of spawning in any arbitrary water
  pocket.
- Corrected successful-spawn mob-spawner feedback: when a spawner actually
  creates a mob, it now emits the old auxiliary-effect 2004 smoke/flame pair
  cloud around the cage in addition to the per-tick active particles.
- Corrected dispenser slot choice to use the Release-style reservoir scan over
  filled inventory slots. It remains uniformly random, but now consumes the
  same source-shaped `nextInt(1)`, `nextInt(2)`, ... call sequence before item
  spread/projectile motion draws.
- Corrected bed placement/support rules so beds require normal opaque support
  under both halves. Beds now reject glass/chest-style support during placement
  and break when an existing half loses valid support.
- Added a Release-style bed respawn resolver that chooses a clear adjacent
  two-block-high standing position around the bed instead of saving the
  player's respawn directly on top of the bed foot.
- Completed the accepted bed-sleep transition through world state: successful
  sleep now skips the day cycle to morning, clears rain/thunder weather, and
  clears temporary occupied metadata on both bed halves.
- Added a player-facing bed sleep transition. Accepted bed use now moves the
  player into a bed-facing sleeping pose, pauses ordinary input/interaction
  while asleep, exposes sleeping bed state to render/UI/network hooks, and
  wakes the player at the resolved adjacent standing position when one exists.
- Hardened bed sleep spawn saving so a successful sleep only overwrites the
  player spawn when the bed respawn resolver finds a clear adjacent standing
  position. Fully blocked beds still complete the sleep transition but preserve
  the previous spawn instead of saving the bed foot as an unsafe fallback.
- Added persisted bed-home anchors and death-respawn validation. Bed sleeps now
  remember the source bed foot separately from the standing spawn position, and
  respawning revalidates that bed before use; missing or obstructed saved beds
  clear the bed-home state and fall back to the world spawn instead of reusing a
  stale unsafe coordinate.
- Corrected bed sleep monster checks to use the old head-centered 8x5x8
  hostile-mob volume instead of an oversized box expanded around both bed
  halves.
- Corrected cactus contact hazards so living entities and dropped items use the
  inset cactus collision box instead of the full block cell, while true item
  contacts are still destroyed before item physics can bump them upward.
- Corrected snow-layer mesh culling so metadata-7 snow layers still render
  their top face under a solid block, matching the old always-visible snow top
  face rule instead of disappearing when the layer reaches full visual height.
- Moved Release-style chest double/triple placement rejection into the shared
  block placement gate, so non-player placement callers cannot bypass the
  single/double chest rule.
- Moved chest open/close audio timing into the chest tile entity. Chests now
  emit the open cue as lid animation starts and the close cue when the lid drops
  below halfway; double chests emit one centered cue from the north/west half.
- Tightened enchanting table input eligibility and armor-material detection so
  hoes remain farming tools but no longer receive Release 1.0 enchanting offers
  or generated enchantments, and swords/tools no longer borrow armor
  enchantability just because their item names start with `IRON_`, `DIAMOND_`,
  or `GOLD_`.
- Gated post-1.0 bow enchantment behavior out of the Release 1.0 target. Bows
  no longer receive enchanting-table offers or generated bow enchantments, and
  loaded/command-created Power/Punch/Flame/Infinity metadata no longer changes
  player-fired arrows or prevents arrow consumption.
- Corrected enchanting table offer rerolls. Opening/changing the table slot now
  refreshes the offer seed instead of deriving the same costs forever from
  table position and item identity. Offer rerolls now consume the owning
  world's RNG instead of a screen-local unseeded random, and shift-click removal
  clears stale offers immediately.
- Reworked enchanting bookshelf power around the Release 1.0 source-shaped
  adjacent-air-gap scan. The two-block shelf ring now has focused coverage for
  the old 30-power cap, diagonal wing shelves, and lower/upper gap blockers.
- Corrected generated enchantment selection to use Release-style enchantment
  weights instead of choosing uniformly from the candidate list, so common
  enchants such as Sharpness/Protection/Efficiency correctly outrank rare
  choices such as Silk Touch.
- Corrected Unbreaking durability prevention for armor. Armor pieces now apply
  the Release-era extra 60% armor damage gate before the normal Unbreaking
  level roll, while tools keep the direct `nextInt(level + 1) > 0` prevention
  path.
- Added focused double-chest screen coverage for the Release-style inventory
  order: north/west chest inventory first, south/east chest inventory second.
- Corrected generic dispenser item ejection to use Release-style lower output
  offset and randomized motion/spread instead of a fixed horizontal velocity.
- Corrected legacy brewing transformations so redstone brewed into a water
  bottle produces the extended mundane variant, and fermented spider eye can
  corrupt awkward, thick, and mundane base potions into weakness while
  preserving the extended mundane-to-extended weakness chain.
- Hardened armor damage math so misplaced armor pieces do not contribute armor
  points or protection-enchantment EPF when save data or future UI paths bypass
  the normal Release-style equip slot rules.
- Removed the post-1.0 full-cauldron empty-bucket pickup behavior. Release-era
  cauldrons now accept water buckets to fill to level 3 and glass bottles drain
  one level into water bottles, while empty buckets do not drain cauldrons.
- Added the missing source-fluid-aware player ray for empty buckets and glass
  bottles. Normal block selection still skips fluids, but bucket/bottle use can
  now target source water/lava before solid blocks behind it, while solid
  blockers still prevent reaching liquid behind them. Flowing-water bottle use
  is now explicitly guarded so this path stays source-only for the Release 1.0
  target.
- Corrected Nether water-bucket placement. Water bucket use in the Nether now
  succeeds as an item use, emits the old fizz/smoke feedback, leaves no water
  block behind, and returns the held stack to an empty bucket; lava bucket
  placement remains valid.
- Corrected lava/water hardening metadata thresholds. Water contact now turns
  lava metadata `0` into obsidian and `1..4` into cobblestone, while shallow
  lava levels `5..15` no longer harden through the old `metadata & 7` shortcut.
- Restored the Release-era note-block callbacks: attack-click playback now
  plays the current note without cycling pitch, right-click tuning cycles pitch
  and then attempts playback with the new value, and redstone rising-edge
  playback remains a separate non-tuning path. Blocked note blocks still cycle
  pitch on right-click while suppressing sound and note particles.
- Tightened End portal activation so the 12 eyed frames must also use the
  inward-facing frame metadata emitted by the stronghold generator; fully eyed
  but misoriented frame rings no longer open a portal.
- Restored the pumpkin/melon stem gameplay loop: stems now receive crop ticks,
  accept old `2..5` age-step bone meal, mature into adjacent pumpkin/melon
  fruit on valid support, drop matching seed rolls, and pumpkin/melon seed
  items plant the matching stem block on farmland.
- Added the missing Release-era bone meal interaction for grass. White dye on
  grass now consumes successfully through the existing player path and scatters
  tall grass plus occasional yellow/red flowers with old tall-grass metadata.
- Added the missing live mushroom bone meal interaction. Brown/red mushrooms
  now attempt matching huge-mushroom growth with Release-style trunk/cap
  metadata, convert support to dirt on success, and restore the small mushroom
  when the source-shaped clearance/support check fails.
- Corrected live sapling bone meal tree selection. Oak saplings now use the
  old normal-tree height/corner RNG, spruce saplings use taiga conifer geometry,
  and birch saplings use the taller forest-style metadata-2 tree path instead
  of treating all saplings as one generic tree shape.
- Added Release-style farmland trampling from fall impact for players and
  living entities, including the old fall-distance threshold/chance behavior
  and crop break/drop cleanup when farmland converts back to dirt.
- Added Release-style rain hydration for exposed farmland. Farms in non-frozen
  rainy Overworld biomes now wet back to max moisture during normal farmland
  ticks even without nearby water, while covered/dimension/biome rain checks
  still flow through the shared weather visibility path.
- Removed the post-1.0 `Wither` painting motive from the painting catalog so
  random painting placement and save-load fallback stay within the Java
  Release 1.0 artwork set. Direct painting creation now falls back to the
  owning world's RNG when no test RNG is injected, and explosions now break
  hanging paintings in the blast radius through an explicit non-living entity
  path. Painting placement now also checks pending same-tick painting entities,
  so two placement attempts cannot occupy the same hanging space before the
  next world entity update. Painting hanging validity now uses broad entity
  overlap checks, so mobs, players, and dropped item entities block placement
  and cause existing paintings to break/drop when they enter the hanging box.
- Corrected the sign item max stack size from one to the Release-style stack
  of 16, so inventory splitting and creative stack creation use the old item
  registry limit.
- Backed the Release 1.0 Snow Golem definition with a runtime mob, renderer
  model, snowman texture path, pumpkin/jack-o-lantern construction from two
  snow blocks, snowball targeting against hostile mobs, snowball drops,
  cool-biome snow trails, wet damage, and hot-biome melt damage. Snow Golem
  death snowball drops now keep the old 0-15 roll without Looting inflation.
- Tuned Snow Golem snowball attacks toward the Release-era throw behavior:
  snowballs now spawn from the golem throw height, use a horizontal-distance
  arc boost and faster classic throw speed, and emit the old bow throw cue.
- Corrected Snow Golem model animation toward the Release-era source pose:
  head yaw now drives a quarter body turn, and the stick arms orbit that body
  yaw with fixed +/-1 radian roll instead of swinging like humanoid limbs.
- Corrected powered rail launch behavior for stopped minecarts: powered rails
  now only kick a stationary cart away from a neighboring opaque stopper block
  instead of always launching in a fixed positive direction.
- Corrected minecart ascending-rail acceleration to the source-shaped
  `1/128` downhill step, so one-tick position deltas and retained empty-cart
  motion no longer drift from the old rail physics.
- Corrected minecart rail drag so empty carts slow down at the stronger
  Release-style rate while mounted rideable carts preserve speed with the
  gentler occupied-cart drag.
- Hardened world-level minecart placement so only the three Release 1.0
  minecart item types can spawn carts; invalid item callers no longer silently
  create a rideable minecart.
- Corrected minecart collision bias for furnace carts: powered furnace
  minecarts now shove ordinary carts while keeping most of their own momentum
  instead of using equal cart-to-cart momentum sharing.
- Corrected vehicle participation in world explosions. Nearby boats and
  minecarts now take exposure-scaled explosion damage and, when destroyed,
  still leave their Release-era component/content drops while loose item
  entities that existed before the blast remain vulnerable to the same
  explosion.
- Tightened detector rail sensing to the old inset minecart activation box and
  restored the longer detector rail recheck delay, so carts brushing only the
  outer full-block edge no longer power the rail.
- Corrected vertical redstone dust propagation so lower dust can only pull
  power from one-block-higher dust when the lower wire's overhead space is not
  blocked, matching the Release-era step-up connection rule and preventing
  ceiling-through propagation.
- Added focused player-level coverage for Eye of Ender throwing so the
  progression path keeps consuming one eye in survival and spawning the
  locator entity toward the nearest stronghold.
- Corrected XP orb pickup pacing so normal newly spawned orbs start with no
  per-orb pickup delay, while the player's short Release 1.0 pickup cooldown
  still gates stacked nearby orbs instead of collecting every eligible orb in
  the same entity tick; successful orb payouts now also emit the old
  `random.orb` pickup cue. XP orbs now also expose their old five-health
  damage path to explosions, so nearby blasts can destroy orbs instead of
  leaving them collectable after the explosion. XP orb attraction now aims at
  the player's eye/head height instead of a lower body-height point.
- Corrected player death XP clearing after the Release-era capped death-orb
  calculation. Deaths below level 1 now still empty the XP bar even though they
  spawn no collectible orbs, while higher-level deaths continue to drop the
  capped XP value and preserve the progression score used by the death screen.
- Corrected equipped armor handling in the player death-drop path. Armor slots
  now spawn their item stacks into the world before `clearInventory()` runs, so
  death no longer deletes worn armor while dropping ordinary carried items.
- Added successful-hit critical attack particles through the shared transient
  particle renderer, replacing the combat TODO with in-world feedback for
  Release-era falling critical hits. Player crit and magic-crit hits now use the
  old three-update entity-emitter cadence instead of a single generic burst.
- Added a transient world sound event queue and wired initial Release-style
  cues for note blocks, jukebox record playback/ejection, manual and redstone
  openables, redstone control clicks, piston extension/retraction, chest UI
  open/close, dropped-item pickup, XP orb pickup, bow shots, flint-and-steel
  fire ignition, TNT fuse priming, creeper fuse priming, explosions, and
  fishing splash feedback. This gives the simulation a real audio-event
  contract; mixer/assets playback remains a separate finish-line item.
- Added Release 1.0 hostile mob damage/death sound events for zombies,
  skeletons, creepers, spiders, and cave spiders. Non-lethal accepted damage
  now queues the old hurt cue, while lethal hits suppress duplicate hurt audio
  and emit the death cue on the mob's death tick.
- Added Release-era timed ambient idle mob sounds for common overworld mobs.
  Zombies, skeletons, spiders/cave spiders, cows/mooshrooms, pigs, sheep, and
  chickens now roll the old increasing idle-sound counter and emit their
  `mob.*` living cue through the same transient sound queue; Creepers stay
  silent until fuse/hurt/death cues.
- Completed the key Enderman vocal cues: neutral Endermen can emit
  `mob.endermen.idle`, angry Endermen can emit `mob.endermen.scream`, the
  first stare-triggered aggro transition queues `mob.endermen.stare`, and
  accepted non-lethal/lethal damage now routes `mob.endermen.hit` and
  `mob.endermen.death` through the shared world sound queue.
- Added Enderman ambient portal particles. Every living Enderman now spawns
  the old two portal particles per tick around its body, while teleport events
  keep their separate 128-particle path burst.
- Added Release-era wolf vocal cues. Calm wolves can bark, angry wolves growl,
  healthy tamed wolves pant, wounded tamed wolves whine below the old
  half-health threshold, and accepted non-lethal/lethal wolf damage routes
  `mob.wolf.hurt`/`mob.wolf.death` through the shared world sound queue.
- Added Release-era slime-family feedback. Slimes now queue size-scaled
  `mob.slime` squish cues for jump/landing/non-lethal hurt/death and
  `mob.slimeattack` when contact damage is accepted, while magma cubes use
  distinct `mob.magmacube.jump` plus big/small magma-cube squish cues through
  the same volume/pitch path.
- Completed jukebox sound-id routing for every Java 1.0 music disc, not just
  `13` and `cat`, so inserted records now queue the matching `records.*`
  playback cue through the shared world sound event path.
- Added Release-style dispenser activation feedback: dispenser fires now queue
  the old `random.click` cue at the dispenser position even when the 3x3
  inventory is empty.
- Extended player item-use coverage: food, milk buckets, and drinkable potions
  now use a held 32-tick Release-style duration, reject duplicate begin calls
  while already active, cancel without consuming when released early, emit
  repeated eat/drink tick cues while held, and only commit
  hunger/effects/inventory changes on completion. Food and cake completion queue
  `random.eat` plus `random.burp`; milk buckets and drinkable potions queue
  `random.drink`; splash potions, ender pearls, Eyes of Ender, and fishing-rod
  casts use the old low-volume `random.bow` thrown-item cue.
- Corrected Release 1.0 food constants and side effects for the held-use path:
  cookies and raw fish now use the old low saturation value, spider eyes are
  edible with poison, raw chicken can apply hunger, and golden apples grant the
  old regeneration effect after completion. Mushroom stew completion now
  preserves any remaining stew count and returns one bowl through inventory/drop
  handling instead of replacing the entire held stack with a bowl.
- Added a runtime world-sound dispatcher bridge that drains queued sound
  events through the current sound-volume setting into an injectable playback
  sink, so the main loop no longer leaves sound events as test-only state.
  This established the audio contract; the backend sink now exists below, while
  fuller mixer behavior and bundled asset coverage remain finish-line work.
- Added an OpenAL-backed OGG world-sound sink and wired the main loop to use it
  with graceful fallback when no audio device is available. Sound ids now
  resolve through resource-pack/classpath candidates for old `newsound`,
  `sound(s)`, and record `streaming` layouts; bundled Release 1.0 sound assets,
  fuller attenuation/mixer behavior, and asset coverage still need follow-up.
- Extended the world-sound dispatcher and OpenAL sink to pass listener
  orientation as well as listener position. Runtime playback now uses the
  camera's current position, forward vector, and up vector so block, entity,
  and interaction sounds can pan against the player's view instead of being
  emitted through a fixed-facing listener.
- Corrected direct camera yaw/pitch setters to refresh cached forward/right/up
  vectors immediately. Third-person transitions, direct look-target changes,
  projectile direction consumers, and audio listener orientation no longer
  depend on a later view-matrix rebuild to see the new look direction.
- Corrected open fence-gate render boxes so gates keep their placed post axis
  and render the two swung leaves on the metadata-facing side when opened,
  while the existing Release-style empty collision for open gates remains
  intact.
- Added a first Release-era fishing rod gameplay pass: right-click now casts a
  bobber entity, the bobber waits in water and becomes briefly catchable,
  reeling a catchable bobber drops raw fish and damages the rod once, and the
  lightweight entity renderer shows the bobber using an existing item-sprite
  path. Fishing hooks now also render a line from the player's rod hand anchor
  to the bobber, and catchable bobbers emit a short in-world splash particle
  burst plus a matching splash sound event. Fishing hooks now use the source
  per-water-tick bite roll instead of a predetermined wait countdown: clear/open
  water rolls `1/500`, rain-exposed water rolls `1/300`, and a successful bite
  opens the 10-39 tick catchable window. Missed catch windows return to that
  same bite roll instead of scheduling a fresh fixed wait.
  Public/player-spawned bobbers now route bite, catch-window, and splash-pitch
  rolls through the owning world's RNG instead of bobber-local random state.
  Catchable bite splash audio now uses the old lower splash volume and
  randomized pitch band. World sound dispatch now uses the player's listener
  position to cull inaudible events outside the Release-style 16-block base
  radius, with louder cues such as records and explosions extending that
  radius; OpenAL OGG playback is now available when matching sound assets are
  present, while bundled assets and exact mixing/orientation remain separate
  polish.
- Added save/load support for active fishing bobbers, including motion, age,
  wait/catchable timers, stuck-in-ground state, and player-owner reattachment
  so restored rods can reel their existing bobber instead of losing it.
- Added Release-era fishing hook entity latching: bobbers now attach to
  hookable entities such as mobs, boats, and minecarts, follow the hooked
  target, and reeling pulls that target toward the player while applying the
  higher rod durability cost.
- Corrected caught-fish reel motion so raw fish items fly back toward the
  angler with the same Release-era distance-lift formula used by the hooked
  entity pull path instead of popping up with a fixed vertical velocity.
- Added save/load cross-reference support for hooked fishing targets. Active
  bobbers now preserve the hooked mob, boat, or minecart identity across reload
  and can still reel the restored target afterward.
- Added save/load cross-reference support for spider jockey riders. Spider
  mounts now preserve their skeleton rider identity across reload, and malformed
  rider references are treated as corrupt save data.
- Added Release 1.0-style wolf bone taming: valid wild, non-angry wolves now
  consume one bone per right-click attempt, tame on the old one-in-three
  success roll, clear anger on success, and reject bone taming once tamed or
  angry.
- Expanded tamed wolf interactions: successful taming now uses the larger
  tamed-wolf health pool and starts the wolf sitting, right-click toggles
  sitting for tamed wolves, meat heals damaged tamed wolves while consuming
  one item, accepted bone/meat uses start the hand-use animation while rejected
  full-health feeding stays inert, and sitting state persists across reloads.
- Added single-player owner-follow behavior for standing tamed wolves: they
  now follow the world player when far enough away, stay put while sitting, and
  teleport near the player from long distances when a safe nearby spot exists.
- Added Release-style wild wolf retaliation. Player melee hits and
  player-owned arrows now turn wild wolves angry, retarget them toward the
  world player, and let them bite with the wild-wolf damage value without
  disturbing tamed wolf owner-assist targeting. Nonlethal player-owned hits
  now also alert nearby calm wild wolves into the same angry state, while
  tamed wolves and one-hit wolf kills do not spread pack anger.
- Added Release-style wild wolf sheep predation. Non-tamed, non-angry wolves
  now acquire nearby sheep through the old `1/200` target chance, chase them,
  and bite with the same wild-wolf melee damage value, while tamed wolves
  ignore sheep unless their owner-assist combat path assigns a real target.
- Corrected burning baby mob visuals so the transient fire overlay follows the
  same baby render scale as the mob body instead of using adult-width and
  adult-height overlay bounds.
- Corrected ageable baby mob gameplay dimensions. Baby animals now expose
  half-size width/height and bounding boxes until their growing age reaches
  adulthood, so collision, targeting, overlays, and entity-interaction checks
  no longer see an adult-sized body behind a half-sized render model.
- Routed mob eye/source/contact height calculations through the age-aware size
  accessors for blaze fireballs, ghast fireballs, Enderman stare and wet
  checks, Snow Golem wet/target checks, squid swim bounds, spider light checks,
  and Ender Dragon contact damage.
- Corrected Ghast ranged-combat timing. Ghasts now use a Release-style charge
  window before firing, switch to the `ghast_fire` texture late in the charge,
  emit charge/fireball sound events, launch explosive fireballs from four
  blocks in front of the body instead of the body center, and preserve
  mid-charge state across save/load.
- Corrected Blaze combat cadence. Blazes now use a close-range melee fallback
  when overlapping the player, then otherwise start a charged ranged sequence
  that fires three small fireballs at 20-tick intervals before entering the
  longer post-volley cooldown instead of dumping the volley six ticks apart.
- Added Blaze combat feedback cues. Blazes now breathe on the shared mob
  ambient timer, play hit/death sounds through the normal damage/death path,
  emit flame/smoke particles at the visible charge point, and emit flame
  particles when each small fireball leaves the body.
- Added Release-style fireball deflection. Left-click attacks against ghast and
  blaze fireballs now redirect the projectile along the player's aim, prevent
  immediate owner self-collision, and preserve the deflected-player state across
  save/load.
- Corrected fireball impact coordinates and End crystal contact behavior.
  Fireballs now resolve entity, player, and block impacts at the raycast hit
  point before applying damage/explosions, and direct fireball contact now
  routes through the End crystal destruction path instead of being ignored as
  ordinary fire damage.
- Corrected small-fireball block impact behavior. Non-explosive Blaze
  fireballs now place fire in the adjacent air cell on block impact before
  removing, while explosive Ghast fireballs keep using the explosion path
  without placing adjacent fire.
- Tightened direct fireball burn semantics. Small fireballs now set living
  targets on fire only when their fire damage is accepted, and explosive
  fireballs no longer apply the small-fireball burn side effect on direct mob
  hits.
- Corrected thrown egg/snowball living-target impacts. Ordinary living targets
  now receive the old zero-damage projectile hit so hurt animation/source state
  is recorded without losing health, while snowballs still deal their special
  Blaze damage and End crystals keep routing through the explosion path.
- Corrected fall-source damage scaling and routing. Player `FALL` damage now
  bypasses difficulty scaling, so ender pearls and explicit fall-source hits
  deal their fixed Release-era amount on Peaceful through Hard instead of
  disappearing on Peaceful or doubling on Hard. Normal landing damage now uses
  the same source-aware fall path, so Feather Falling and fall-specific
  protection apply to actual falls instead of being bypassed by raw stat damage.
  Landing fall damage now also rounds `ceil(fallDistance - 3)`, so fractional
  falls above the safe threshold deal the old whole-point damage instead of
  leaking tiny fractional health loss.
- Restored landing fall damage for non-player living entities. Mobs now receive
  the same rounded Release-era fall damage on landing while still trampling
  farmland through the shared landing path; chickens keep their slow-fall
  fall-damage immunity.
- Corrected dropped item entity stacking. Oversized spawned drops now split into
  valid item max-stack entities, and nearby matching item entities partially top
  off existing stacks instead of refusing to merge unless the whole incoming
  count fits.
- Corrected dropped item pickup with nearly-full inventories. Item entities now
  transfer the amount that fits, leave the remainder in the world, and emit the
  pickup cue only after at least one item actually moves.
- Upgraded villagers from generated-structure shells to passive runtime mobs.
  They still preserve Release 1.0's no-drop/no-trade behavior, but now panic
  when hurt and idle-wander through the same movement goals as passive animals.
- Corrected Nether fire-mob immunity. Ghasts, Blazes, Magma Cubes, and Zombie
  Pigmen now reject explicit fire damage and lava-contact fire damage instead
  of only clearing their burning timer.
- Corrected Zombie Pigman anger triggers. Incidental/environment damage can
  still hurt them, but only accepted player melee damage or player-owned arrow
  damage starts anger and alerts nearby Pigmen.
- Corrected Zombie Pigman anger persistence and sound feedback. Their anger
  value now uses the old finite 400-799 tick window, counts down each mob tick,
  attacks schedule the delayed `mob.zombiepig.zpigangry` cue, and
  ambient/hurt/death pigman sounds use the shared world sound queue.
- Added Blaze water-contact vulnerability while preserving their Nether fire
  immunity and existing snowball damage behavior.
- Corrected Blaze rod drops so rods require recent player-credit damage,
  including tamed-wolf melee credit, and use the Blaze-specific single
  `nextInt(2 + looting)` count roll instead of the shared base-plus-Looting
  helper. Environmental Blaze deaths still drop no rods.
- Corrected Enderman wet-damage gating so clear daylight no longer acts like
  rain; Endermen now use water contact plus real open-sky rain checks for wet
  damage.
- Corrected Enderman damage-source handling. Water/rain damage now uses an
  environmental drowning-style source that hurts without making the Enderman
  player-angry, while arrow impacts remain fully ignored even if random
     teleport attempts cannot find a destination.
- Added Release-era Enderman carried-block rendering. Carried block state now
  raises both arms and renders the held block with its block metadata in front
  of the Enderman body instead of remaining invisible until the block is placed.
- Added Release-era Enderman ambient portal shimmer. The same transient
  particle renderer used by teleport feedback now receives the old two
  per-tick portal particles for ordinary living Endermen.
- Corrected stared-at Enderman close-combat behavior. The shared melee pursuit
  goal now lets source-specific mob logic intercept before applying contact
  damage, so an angry Enderman that is still being directly stared at stops the
  normal swing path, clears its teleport delay, and uses the old close-range
  random teleport gate; if the player looks away, the Enderman can still land
  its normal Release-era melee hit.
- Added minimal Release-style weather simulation state. `/weather` and loaded
  saves now normalize clear/rain/thunder into `World`, Overworld rain/thunder
  can be queried per open-sky position, Nether/End worlds stay dry, and
  Endermen plus Snow Golems take rain damage in non-frozen biomes.
- Added Release-era weather countdowns. Rain and thunder now advance through
  independent world tick timers, toggle between long clear delays and shorter
  active durations, and persist their remaining countdowns through save/load.
- Added Release-style rain/thunder strength ramps for rendering. Weather now
  fades in/out by tick, exposes partial-tick interpolated rain and thunder
  intensity, and feeds those values into sky/fog and brightness darkening.
- Added a Release-style lightning strike simulation path for thunderstorms.
  Exposed, rainy Overworld columns can now receive thunder plus explosion
  crack sound events, start supported fire, and apply fire damage/ignition to
  nearby living entities; rain-only and covered columns reject strikes.
- Added Release-era lightning mob side effects. Creepers struck by lightning
  now become powered, persist that state through save/load, use the doubled
  charged explosion strength, and render with the classic `power.png` overlay;
  struck pigs now convert into Zombie Pigmen without dropping pig loot.
- Corrected the ridden-pig edge of the lightning transform path. Struck
  saddled pigs now detach the local rider before being replaced, using the
  same nearby clear-box dismount placement as ordinary pig dismounts.
- Added transient lightning bolt geometry for struck columns. Successful
  strikes now create short-lived jagged main/branch line segments that age
  through the world particle update path and render through the shared line
  shader, so lightning has an observable bolt instead of sound/fire-only
  feedback. Bolt visibility now flickers through repeated flash windows with
  dark gaps instead of fading once from full opacity.
- Added a transient lightning sky-flash signal. Successful strikes now expose
  a short-lived interpolated flash strength that brightens sky, fog, ambient,
  and sun-light rendering before fading through the same visual update path as
  lightning bolt geometry.
- Added player-centered precipitation effects for active weather. Open rainy
  columns near the player now emit transient rain particles plus throttled
  `ambient.weather.rain` cues, frozen precipitation columns can emit snow
  particles, and desert/no-rain biomes no longer satisfy `isRainingAt`.
- Wired active weather particles into the bundled environment precipitation
  assets. Rain particles now render as narrow blue rain streaks from
  `textures/environment/rain.png`, and snow particles render as wider falling
  flake sheets from `textures/environment/snow.png` instead of both falling
  back to the generic smoke particle atlas cell.
- Corrected transient rain particle behavior to match the old `EntityRainFX`
  role. The continuous curtain still owns falling storm sheets, while
  world-spawned `RAIN` particles now spawn just above exposed precipitation
  surfaces with source-shaped 8-40 tick lifetimes, apply `0.06` raindrop
  gravity, move by full tick motion with `0.98` drag, and expire on ground or
  liquid contact instead of falling slowly from several blocks above and
  lingering on collision.
- Added a camera-centered precipitation curtain renderer for active weather.
  Open rainy columns now draw continuous vertical rain sheets around the
  player, frozen precipitation columns draw broader snow sheets, covered
  columns stay clear through the same `isRainingAt`/`isSnowingAt` gates, and
  the curtain radius ramps with weather strength while the bundled
  `rain.png`/`snow.png` environment textures animate downward. The curtain now
  also uses stable coordinate-seeded column phase, fall-speed, width, and
  height variation so neighboring rain/snow strips no longer move in lockstep.
- Added runtime snowfall accumulation for active weather. Exposed frozen
  precipitation columns can now place one-layer `SNOW_LAYER` blocks on valid
  opaque/leaf support, while covered columns, warm rain biomes, existing snow
  layers, ice support, and block light 10 or higher reject accumulation like
  the source-shaped snow placement gate.
- Added runtime weather water freezing. Active snowing columns can now turn
  exposed level-0 water into ice below block light 10, reject flowing/non-source
  water and warm rain biomes, and silently clear unsupported lily pads above
  newly frozen water without dropping free lily pad items.
- Removed runtime weather cauldron filling from the Release 1.0 target.
  Exposed cauldrons in warm/rainy Overworld columns now stay at their existing
  water metadata during active precipitation ticks instead of receiving the
  later rain-fill roll.
- Corrected fire/weather interaction so exposed Overworld fire is extinguished
  by rain on its scheduled tick, while covered fire remains lit.
- Tightened natural mob spawning to validate the selected mob's full bounding
  volume against loaded block collisions and fluids, so wide/tall mobs such as
  Ghasts no longer pass a two-air-block check and spawn intersecting walls or
  liquid.
- Corrected natural hostile spawn light checks outside the Overworld so Nether
  and End spawning use block light only instead of inheriting daylight-scaled
  sky light from the Overworld path.
- Routed public natural mob spawner construction through the owning world's RNG
  instead of a fresh unseeded random source, while preserving injectable random
  sources for focused pack-size and spawn-position tests.
- Corrected cake attack-click behavior. Left-clicking cake now falls through to
  ordinary block mining/breaking; only the use path consumes hunger-gated cake
  slices with hunger/saturation gain and eat/burp cues.
- Tightened player vine placement so top/bottom face clicks are rejected before
  placement metadata fallback. Supported horizontal face placement remains
  valid, but accidental vertical-face placement can no longer create a
  metadata-zero vine beside an unrelated side support.
- Corrected new vine metadata writes to use Release-style side bits
  (`8/2/1/4`) instead of engine face ids, routed the older `TreeFeature`
  swamp-vine path through the same helper, and allowed hanging vine columns to
  stay when the vine above carries the same side bit.
- Corrected Nether portal frame validation to require the full fixed 4x5
  obsidian perimeter, including corners, before bottom-row fire can create the
  portal interior. Portal fixtures now explicitly clear the interior air space
  instead of relying on activation to overwrite generated terrain.
- Corrected active Nether portal support validation to distinguish activation
  from ongoing integrity. Bottom-row fire can still create a portal through
  air/fire interior cells, but existing portal blocks now require all six
  interior cells to remain portal blocks before they stay supported or are
  reused by destination transfer search.
- Corrected torch and redstone-torch wall metadata to the Release-style source
  values (`1..4` for side attachments, `5` for floor placement). New player
  placement now writes source metadata, loaded/source-metadata wall torches keep
  the correct support, and wall redstone torches skip powering their attachment
  block while powering the other faces.
- Corrected stone-button wall metadata to the Release-style source values
  (`1..4`) instead of engine face ids. World/player placement now writes source
  metadata, button support checks use the source attachment face, and powered
  buttons still strongly power only their anchor block.
- Corrected lever ceiling placement. Levers can now attach to walls, floors,
  and ceilings; floor metadata can use source variants `5/6`, ceiling metadata
  can use `0/7`, ceiling support checks honor the block above, and powered
  ceiling levers strongly power only their attachment block.
- Corrected piston world-height bounds. Pistons can now extend into valid
  Release 1.0 block cells at `y=0` and `y=127`, and sticky pistons can pull
  movable blocks from `y=0`, while pushes that would move a block beyond the
  top of the 128-high world still fail.
- Corrected piston rail push reaction. Rails, powered rails, and detector rails
  are no longer treated as always-fragile piston targets: supported rails move
  with the piston, while rails that settle onto unsupported destinations pop
  into drops after the moving-piston phase validates the final block support.
- Corrected active moving-piston entity interaction. Moving piston blocks now
  shove mobs, players, and dropped items during the travel ticks using the
  current moving block face, instead of acting only as static collision after
  the initial whole-block staging push.
- Corrected minecart ascending-rail gravity direction. Carts on ascending east,
  west, north, and south rails now accelerate downhill from rest instead of
  being nudged uphill by the slope force.
- Corrected detector rail shape/power metadata ordering. Detector rails now
  preserve the freshly resolved rail shape when a minecart powers them in the
  same tick, instead of writing the powered bit over stale shape metadata.
- Corrected powered rail propagation across ascending slopes. Powered rails now
  treat the flat rail at the top of a slope as connected to the lower ascending
  rail, so activation can climb Release-style rail ramps instead of stopping at
  the height change.
- Corrected ladder and wall-sign east/west metadata to the Release-style source
  table (`2/3/4/5`), so metadata `4` means west-facing/support-east and
  metadata `5` means east-facing/support-west. Player placement, support
  checks, and selection shapes now use the same mapping.
- Corrected open trapdoor hinge shapes so source metadata `0..3` folds the
  vertical collision/render slab against the supporting side instead of the
  opposite edge, while preserving the lower/top closed slab bits.
- Corrected sneaking placement against interactive blocks. Sneaking with a
  placeable held stack now bypasses clicked-block use/open/toggle behavior so a
  block can be placed against chests, furnaces, dispensers, beds, cake, doors,
  and similar interaction blocks; empty hands and non-placeable items still use
  the clicked block first.
- Corrected base entity soul-sand contact to include the pre-move contact
  state, so entities resting on the lowered soul-sand surface receive the old
  horizontal slowdown on that tick instead of only after they still overlap it
  post-movement.
- Restored the stone button crafting recipe to the Release 1.0 two-smooth-stone
  vertical input. The later one-stone recipe is now rejected by focused
  crafting coverage.
- Corrected the boat crafting recipe to the Release 1.0 five-plank U shape.
  The three-plank V shape now remains the bowl recipe instead of also matching
  boats through a duplicate recipe entry.
- Corrected boat attack-break drops to the Release-era component output.
  Player-broken boats now drop 3 planks and 2 sticks like crash-broken boats
  instead of dropping the later boat item.
- Corrected boat horizontal crash breaking to the Release-era strict `> 0.15`
  speed cutoff. Actual solid-wall collisions now keep slow docking nudges alive
  but shatter faster boats into 3 planks and 2 sticks through the same legacy
  component drop path.
- Corrected boat item placement collision checks. World/player boat placement
  now builds the candidate boat's source-style shrunken spawn box before
  enqueuing it, rejecting solid collisions, live entities, same-tick pending
  entities, dropped items, and the player instead of spawning overlapping
  boats.
- Corrected boat explosion damage. World explosions now damage boats through
  the shared exposure-scaled entity blast path; destroyed boats drop the old
  three planks and two sticks, while pre-existing loose item entities in the
  blast can still be destroyed.
- Added Release-era boat wake particles. Fast boats in water now leave the old
  splash spray, while slow or dry boats avoid noisy particle emission.
- Replaced the flat in-world boat item sprite with a textured five-part
  hull mesh in the entity renderer. Placed and ridden boats now render with a
  real bottom and side panels from `textures/item/boat.png` instead of facing
  the player as a billboarded inventory icon.
- Corrected chicken passive details: egg laying now emits the old
  `mob.chicken.plop` cue with the source randomized pitch formula, and
  airborne chickens damp downward velocity by the old wing-flap factor instead
  of only using a hard fall-speed cap. Chicken death loot now keeps the
  Release-era single raw/cooked chicken meat drop even when recent player
  Looting credit widens the feather count.
- Corrected spider-eye drops to the Release rare-drop path. Spiders and cave
  spiders still drop string normally, but spider eyes now require recent
  player-credit damage and use the old 1-in-3 base chance with Looting widening
  instead of dropping from any environmental death.
- Corrected mooshroom shearing feedback. Shearing an adult mooshroom now emits
  the old body-centered large explosion particle while converting it into a
  cow, dropping five red mushrooms, damaging shears in survival, and preserving
  the existing health-transfer behavior.

## P0 Finish-Line Work

These are the systems that decide whether the project feels like Minecraft 1.0
rather than a partial sandbox.

1. Registries and recipes
   - Compare `BlockType`, `ItemType`, drops, stack sizes, metadata semantics,
     tools, armor, food, records, buckets, and potions against the Java 1.0
     item/block set. Spawn eggs are post-1.0 content and should remain absent.
     Fire is now preserved as an old command/editor item for chainmail recipes,
     and mob spawners, double slabs, plus monster-egg variants are now
     preserved as old command/editor block-id items; the creative catalog keeps
     those hidden from normal browsing.
   - Finish the recipe audit for shaped/shapeless crafting, fuel durations,
     brewing transformations, any obscure repair/combine edge cases, and
     enchantment table inputs/outputs. The furnace smelting table now has an
     exact Release 1.0 surface test for represented inputs, and dynamic
     two-item crafting repair now has focused coverage for formula and
     crafting-table output. Stone buttons now use the old two-stone vertical
     recipe instead of the later one-stone input, and boats now use
     the old five-plank U recipe instead of colliding with the bowl recipe.
     Dispenser crafting now preserves the old full-durability bow requirement
     instead of accepting damaged bows through the stack matcher.
   - Remove, gate, or document any post-1.0 content that should not be enabled
     in the Release 1.0 target. Night vision is now explicitly guarded as an
     unobtainable legacy status effect rather than a brewable/creative potion
     identity.

2. Blocks and tile entities
   - Furnace: verify burn/cook tick timing, remaining edge-case fuel
     remainders, lit/unlit block swaps, inventory slot rules, and save/load of
     active progress. Active
     progress and fractional ticks now persist, furnace ticks now reconcile
     lit/unlit block state against current burn time without replacing the
     tile entity, and quick-transfer routing now prioritizes smeltable input
     before fuel for dual-purpose items. Direct GUI placement now lets any
     stack enter the plain input slot while still rejecting non-fuels from fuel
     and cursor insertion into output, and ordinary shift-clicked player items
     now transfer between main inventory and hotbar while the furnace screen is
     open. Lava-bucket fuel now leaves the empty bucket container item after
     starting the 20,000-tick burn. The represented Release 1.0 smelting recipe
     table now has exact coverage, including rejection of invalid/later-era
     furnace inputs. Cook progress now resets when heat runs out, when the
     furnace is actively processing an invalid fuel/input state, or when the
     input is missing, unsmeltable, or blocked by an incompatible output stack.
     Stale partial progress with valid input but no fuel now also resets without
     consuming input or crashing. Lit furnaces now also emit
     front-face smoke/flame particles without a furnace-specific sound cue,
     using the block's facing metadata. Open furnace GUIs now close when the
     same furnace tile is gone/replaced or the player leaves the old 8-block
     usable range, while preserving lit/unlit furnace tile swaps as usable.
     The open GUI now renders the Release-era flame and cook-arrow overlays
     from `furnace.png` using source-style integer progress dimensions.
   - Chest: verify remaining render/sound polish. Single/double placement,
     triple-chest rejection through the shared placement gate, blocked opening,
     breaking drops, lid animation, and double-chest GUI inventory order now
     have focused coverage. Chest open/close audio now belongs to the tile
     entity lid animation, including the delayed close cue below half-open and a
     single centered cue for double chests; those transient world sound events
     route through the OpenAL-backed dispatcher when assets and an audio device
     are available. Global gameplay-screen close now drains queued cursor items
     from the chest GUI into world drops. In-world lid rendering now uses the
     old cubic easing curve and full right-angle hinge rotation. In-world chest
     models now also draw the old small front latch, centered correctly for
     single and double chests and carried by the same lid hinge. Single and
     large chest body/lid/latch meshes now use the old model-box texture
     unwraps against the 64x64 and 128x64 chest textures instead of stretching
     the whole texture across every animated part. Remaining work is final
     audio-asset polish and visual source review, not the core blocked-opening,
     open-container validity, animation, or UI path.
   - Dispenser: source-audit remaining item-specific edge cases and GUI/save
     behavior. Random filled-slot selection, redstone triggering,
     arrow/egg/snowball/splash-potion behavior, TNT item ejection, generic item
     output position/spread, boat/minecart item ejection even with valid
     water/rail targets, and horizontal-only Release 1.0 output orientation now
     have focused coverage. Redstone activation now waits the old 4-tick scheduled
     dispenser delay before dispensing. Dispenser GUI right-click
     splitting and shift-click transfer into the 3x3 inventory, plus tile inventory
     save/load, now have focused coverage.
     Slot selection now uses the old reservoir RNG call sequence over filled
     slots, and projectile/item spread uses the world-owned RNG instead of
     tile-local or process-global random state, making dispenser behavior
     replayable from the world seed.
     Empty activation now emits the old high-pitch dispenser click cue, generic
     success emits the lower-pitch click effect, and projectile/splash success
     emits the bow effect with no extra empty click. Open dispenser GUIs now
     close when the same dispenser tile is gone/replaced or the player leaves
     the old 8-block usable range.
   - Brewing stand: verify remaining GUI/render polish and source-audit any
     obscure metadata aliases. Bottle slots, ingredient validation, direct
     placement/merge slot rules, brew timing, visual bottle bits, persistence,
     Release 1.0 potion identity set, and the legacy
     mundane/awkward/thick-to-weakness recipe chain now have focused coverage.
     Ghast tears and magma cream now make mundane potions from water bottles,
     while Awkward potions are still required for Regeneration and Fire
     Resistance. The GUI now accepts potion items in bottle slots with the
     old one-item slot cap, keeps empty glass bottles out until they are filled
     into water bottles, and quick-transfer follows the source container
     routing for bottle, ingredient, and main-inventory/hotbar fallback paths.
     The in-world renderer now honors the three bottle-occupancy metadata bits,
     so empty and partially-filled stands render only their actual bottles.
     Brewed potion stacks now show Release-era liquid colors in GUI slots and
     cursor/creative inventory rendering, with splash variants visibly marked,
     so potion identity is no longer tooltip-only.
     Brewing-stand physical collision now uses the source rod plus
     full-footprint base boxes instead of the decorative bottle proxy. Open
     brewing stand GUIs now close when the same brewing tile is gone/replaced
     or the player leaves the old 8-block usable range.
   - Enchanting table: verify any final sound/visual source polish after the
     core math and GUI passes. Book-animation tile creation and Release 1.0
     enchantment eligibility, including hoe rejection and zero offers for
     non-enchantable stacks, now has focused coverage. Offer
     seeds now reroll from the owning world's RNG when the table slot changes,
     and emptying the slot clears stale offers immediately. XP level spending
     now preserves the old
     progress-bar fraction instead of snapping to the new level floor, and the
     level-50 XP curve is locked by focused coverage. Post-1.0 bow enchantment
     offers, generation, and combat effects are now gated out for the Release
     1.0 target. The GUI table slot now follows the source permissive
     one-item slot behavior, so non-enchantable stacks are allowed into the
     slot while still generating zero usable offers. Bookshelf power now follows the source-shaped adjacent-air-gap
     and two-block-ring scan with focused coverage for the 30-power cap.
     Generated enchantment picks now use Release-style enchantment weights
     instead of uniform candidate selection. Generated enchantment levels now
     use the Release-era quarter-enchantability random bonus and
     per-enchantment min/max gates, so high-value effects such as Protection
     IV, Knockback II, Fire Aspect II, Unbreaking III, and Fortune III appear
     at their source-shaped adjusted levels instead of too early or too late.
     Closing the GUI now drops the temporary table-slot stack instead of
     returning it to the player inventory. Runtime book animation state now
     round-trips through save/load. Enchanted
     output/player stacks now show a visible glint in the GUI and hotbar, so
     enchantment state is no longer only exposed by tooltip text. Open
     enchanting table GUIs now close when the table block is gone/replaced or
     the player leaves the old 8-block usable range. Offer rows now also use
     the source `enchant.png` disabled/normal/hovered strips, deterministic
     pseudo-enchantment phrases generated from the offer seed, the bundled
     alternate font for the phrase text, and source-colored right-aligned level
     numbers instead of flat placeholder rectangles.
   - Mob spawner: verify final render/audio polish. Active spawners now emit
     in-world smoke/flame particles through the shared transient particle
     renderer, and successful spawns emit an extra cage-centered smoke/flame
     burst. Active spawners now also advance the source-style delay-based
     preview rotation, and the renderer draws a scaled cached mob preview for
     the configured definition inside visible spawner cages. Mob type, delay
     state, partial 20Hz tick progress, expanded-box
     spawn caps, same-tick cap accounting, hostile light checks, entity-box
     spawn collision, passive bright-grass support checks, water creature
     height band, exclusive reset delay range, old-style X/Z spawn spread,
     random spawn yaw/pitch, slime/magma-cube 1/2/4 size selection,
     world-owned RNG routing, failed-attempt zero-delay retries, and no-drop
     block breaking are now covered.
   - Signs, note blocks, jukeboxes, beds, cake, cauldrons, dragon eggs, locked
     chests, cactus, snow layers, crops, nether wart, fire, portals, and
     attachable blocks need final source-audited behavior passes. Runtime
     ground-cover support now honors farmland for flower-style plants and the
     mycelium-or-covered-low-light opaque-support rule for small mushrooms,
     matching the generation-side Release rules. Ground-cover plant
     selection/render bounds now split into source flower, mushroom,
     sapling, tall-grass, and dead-bush shape families instead of one shared
     local box.
     Sign text
     clamping plus font-backed editor/storage filtering, world-space sign-face
     text rendering, source-sized 16-way standing-sign board/post chunk
     rendering, and standing-sign source selection bounds, punched
     note-block playback without pitch cycling, right-click pitch cycling plus
     new-note playback, blocked-above silent tuning, pitch-colored
     successful-play note particles, source-material instrument mapping for
     stone, shatterable, sand, and wood families, jukebox
     record insert/eject metadata gates, recorded-jukebox removal ejection,
     randomized record ejection and 10-tick ejected-record pickup delay,
     restored record/block metadata reconciliation, and bed paired
     placement/use/support plus malformed break cleanup,
     head-centered sleep monster bounds, and adjacent respawn-position selection
     are now covered. Represented Release-era block-light emission constants are
     now pinned, including redstone torch, lit repeater, jack-o-lantern, brown
     mushroom, End portal frame, portal, lit furnace, lava, and glowstone
     behavior. Cactus support/growth, full-block ray selection,
     narrow collision/contact shape, living contact damage, and dropped-item
     destruction now have focused coverage.
     Lily pads now use the source 1/64-block collision/selection plate while
     preserving level-0 water support and boat path clearing.
     Cauldron water-bucket fill, glass-bottle level draining,
     empty-bucket rejection, source-only player bucket/bottle targeting,
     weather-driven rain filling, and metadata-driven inset water-level
     rendering now have focused coverage.
     Tile-backed metadata transitions now keep their real tile entity when the
     tile type is compatible, stale tile inventories are replaced when the block
     changes to a different tile type, bucket-placed fluids now run the same
     displacement/mixing path as flowing fluids, including non-solid
     fluid-displaceable blocks beyond the generic placement replaceable list;
     neighboring water/lava now receives an immediate scheduled wakeup after
     block changes so stale flow clears promptly after bucket edits,
     placement/removal, ice transitions, piston-settled blocks, and fluid
     displacement; suppressed multi-block portal, door, and bed edits now replay
     their normal support, redstone/mechanism, immediate fluid, and lava/water
     mixing side effects after the whole shape is placed or removed; ice wakes
     from neighboring block/light changes, and silent vine/leaf metadata cleanup
     marks chunks dirty so save/render state follows the gameplay state.
     Tile inventory drops now use the old container scatter path: saved stack
     identity is preserved, but large stacks split into randomized fragments
     with independent in-block offsets and toss velocity for block breaking and
     explosion cleanup alike.
     Wheat crop support/growth/drops, grass bone-meal
     ground-cover scatter, grass/mycelium light-based decay and dirt-spread
     ticks, live huge-mushroom bone-meal growth/restoration,
     metadata-specific sapling bone-meal tree generation, fall-impact farmland
     trampling, and pumpkin/melon
     stem support, bone meal, fruit growth, seed planting, and seed drops now
     have focused coverage. Crop, stem, and nether-wart selection/render bounds
     now follow their separate source shape tables instead of sharing one broad
     narrow crop box, and Nether wart age growth is now gated to the Nether
     while support checks still run in every dimension. Snow layers now split
     source render height from collision
     height, with collision using the old `metadata / 8` table instead of a
     hard jump to half-block collision. Ice now follows its old scheduled melt
     and harvest lifecycle: high block light melts it to still water, mining it
     over solid/liquid support creates flowing water, and Nether ice leaves air
     instead of water. Cake now removes on the sixth eaten
     slice, clamps shape metadata to valid Release metadata 0-5, keeps the
     1/2-block selection/render box separate from the 7/16-block source
     collision box, and supports the source-style right-click and attack-click
     eat-slice callbacks. Release-style
     flint-and-steel fire placement now emits the old `fire.ignite` cue without
     making worldgen, fire spread, or bed-explosion fire placement noisy.
     Exposed Overworld fire now extinguishes from rain while covered fire stays
     lit through the scheduled fire tick, and world weather now advances through
     persisted rain/thunder countdowns instead of staying in a static command-set
     state forever. Rain/thunder strength now ramps per tick and feeds
     partial-tick interpolated sky/fog and brightness darkening for rendering.
     Infested stone now breaks into silverfish without drops both on direct
     breaking and when a hurt silverfish wakes nearby monster eggs. Idle
     silverfish can now also re-enter compatible stone, cobblestone, or
     stone-brick blocks as the corresponding monster-egg metadata instead of
     wandering forever after losing a target.
     Sign editor character filtering and line navigation now have focused
     coverage, and direct tile storage now applies the same Release-style
     character filtering before clamping each line to 15 characters. Full bed
     use now rejects worlds without a day-cycle clock instead of treating
     unknown time as night, and successful bed use marks the head half
     occupied until the short player sleep transition completes, skips to
     morning, clears active rain/thunder weather, and wakes the player beside
     the bed when a safe adjacent space exists. Leaving bed before completion
     now clears the occupied bit, preserves night/weather, wakes beside the bed
     when possible, and withdraws the sleeper from the multiplayer sleep quorum.
     Dragon egg click
      teleport now uses the source-shaped random
      search and emits the old 128 portal particle burst on success. Locked
      chests now vanish immediately on their scheduled update tick without
      drops, matching the old `BlockLockedChest.updateTick`. Player vine
      placement now rejects invalid vertical-face clicks while preserving
      supported horizontal placement, new vines now write source side-bit
      metadata, hanging vine columns can stay from matching vine bits
      above, scheduled vine ticks prune unsupported side bits, detach fully
      unsupported vines, and grow bounded hanging columns, and vine
      selection/render bounds now use the source side-bitmask accumulation
      including metadata-zero top-hanging vines. Nether portal
      creation now requires the full 4x5 obsidian perimeter, including
      corners, before bottom fire fills the interior, and portal render bounds
      now use the source centered 4/16-thick axis plane.
      Leaf decay now follows the old no-decay/check-decay metadata split:
      unmarked generated leaves ignore scheduled decay probes, log removal and
      neighbor changes mark nearby non-persistent leaves, connected marked
      leaves clear the check bit, and disconnected marked leaves decay.
      Torches and redstone torches now write and honor Release-style wall
      metadata for side support and redstone power faces, and their
      selection/render bounds now follow the source wall and standing torch
      tables instead of coarse 1/16 block-grid approximations. Stone buttons now
      write and honor the same source-style side metadata while still strongly
      powering only their anchor block. Levers now support Release-style wall,
      floor, and ceiling anchors, including vertical metadata variants `5/6`
      and `0/7`. Ladders and wall signs now use the Release `2/3/4/5` side
      metadata table for east/west support, and their selection/render bounds
      now follow the source ladder and wall-sign thickness/position tables.
      Glass panes and iron bars now split source collision and selection bounds
      from the decorative render boxes, so isolated and one-sided connections
      use the old `BlockThin` collision behavior.
      Open trapdoors now fold toward their source hinge/support side instead
      of the opposite edge.
      Sneaking with a placeable held stack now places against
      interactive blocks instead of always opening, toggling, or eating them.
      Bed spawn saving now preserves the previous spawn when
      no safe adjacent standing position exists, and death respawn now
      revalidates persisted bed-home anchors before using them. Beds used
      outside the Overworld now remove both halves and route follow-up fire
      through the shared source-shaped explosion affected set instead of a
      fixed cube scatter around the head block, while inheriting the shared
      large explosion visual burst. Accepted bed sleep now renders a
      full-screen dark fade tied to the active one-second sleep transition and
      holds that dark overlay while multiplayer sleep waits for completion.
      Item-4 feature implementation is treated as covered for this pass;
      remaining bed/source-audio checks belong to final verification polish
      after the broader feature list is complete.

3. Redstone, pistons, rails, and mechanisms
   - Redstone dust: verify weak/strong power, wire shape, vertical adjacency,
     dust decay, block powering, scheduled tick order, and neighbor updates.
     Directional weak power, upward/non-downward weak-power behavior, visible
     corner connections, Release-era bend side-power gating, and
     blocked/unblocked vertical step-up propagation now have focused coverage.
     Dust strength changes now immediately reschedule
     connected neighboring dust, so straight wire runs settle their full
     15-to-0 strength gradient and clear stale power in one block update tick
     instead of rippling through the line over many ticks. Weak-powered opaque
     blocks now conduct into neighboring mechanisms without becoming dust
     relays, so TNT, repeaters, and other mechanisms can be triggered from a
     powered solid block while adjacent dust remains unpowered unless it has a
     direct or strongly powered input. Mechanism update scheduling now also
     wakes same-level, step-up, and step-down dust around changed blocks with
     immediate ticks, reducing stale vertical dust state after support or power
     changes. The shared scheduled tick queue now replaces an existing pending
     tick when a newly scheduled wakeup is due earlier, so live mechanism
     updates are not held behind stale delayed entries for the same block/type.
     Redstone dust chunk rendering now uses the same flat, step-up, step-down,
     and blocked-ceiling connection cases to draw top-only cutout dust arms
     instead of a raised full-block plate, with metadata-based red power tint
     and an isolated cross fallback.
   - Levers, buttons, and pressure plates now emit old `random.click` sounds on
     actual powered/unpowered state edges, with the Release-style 0.3 volume and
     on/off pitch split. Pressure plates now use the source 1/8-block inset
     entity scan, dropped-item bounding boxes for wooden-plate activation,
     20-tick rescan/release cadence, and low `y + 0.1` sound origin. Stone
     buttons now write source wall metadata and route
     their strong-power face through that metadata instead of engine face ids;
     side-anchor support now uses the source normal-block predicate instead of
     the broader attachable-block helper.
     Levers now place on ceilings as well as floors/walls and preserve the old
     vertical metadata variants while strongly powering only their anchor block;
     wall, floor, and ceiling support now reject glass and chest-style anchors
     via the source normal-block predicate. Lever selection/render bounds now
     follow the source metadata table for wall, floor, and ceiling variants
     instead of reusing the old undersized floor-like box.
     Lever chunk rendering now keeps those broad selection bounds for picking
     but draws a small cobblestone base plus a powered/unpowered metadata-aware
     handle arm on the cutout mesh layer.
     Powered stone-button metadata now has focused shape coverage for the old
     1/16-block depressed protrusion versus the unpressed 2/16-block
     protrusion across all four wall orientations. Remaining control work is
     deeper source-order auditing. Cycling a repeater's delay now reschedules
     that repeater and surrounding mechanisms immediately, so the new delay
     participates in the next pending redstone edge instead of waiting for an
     unrelated neighbor update; existing delayed repeater ticks are replaced
     rather than coexisting with stale pre-click delay timing.
   - Torches/repeaters: verify burnout, directional power, and deeper block
     update ordering. Repeater four-step delay cycling, configured delay timing,
     and Release 1.0 non-locking side-power behavior now have focused coverage.
     Repeaters now also accept weak-powered opaque blocks on their input side,
     preserving the old solid-block input path without introducing post-1.0
     side locking. Powered rear redstone dust now feeds repeater input directly
     even when the dust has a side branch, matching the old repeater-specific
     input path without weakening the separate dust bend side-power rules.
     Redstone torch burnout now emits the old fizz cue and smoke burst when the
     8-toggle history trips, then schedules recovery so an unpowered burned-out
     torch can turn back on after the history window expires without requiring
     another external block update. Wall redstone torches now use source
     metadata for their attachment face, so metadata-1 torches invert from the
     west support block and still power the non-support faces.
   - Doors/trapdoors/fence gates: verify manual interaction versus redstone
     state, paired-block metadata, breaking, drops, and sounds/animation hooks.
     Manual toggles and redstone signal edges now emit Release-style door
     open/close world sound events with the old randomized pitch band.
     Door render/collision bounds now use the source facing table and
     upper-half hinge bit for both lower and upper halves, and placement now
     writes that upper hinge bit from neighboring opaque block/door columns
     instead of always creating left-hinged metadata `8` doors.
     Door placement/lower-half survival now requires a Release-style normal
     full-block floor instead of the broader generic attached-block predicate.
     Orphaned upper door halves no longer drop a door item or clear unrelated
     lower blocks when broken, and lower-half survival now requires a real
     upper-half partner instead of any same-type door block above.
     Player-opened fence gates now apply the Release-style back-side facing
     correction while redstone preserves the placed axis. Open trapdoor shape
     now follows the source hinge/support side for metadata `0..3`, and
     trapdoor placement/support now uses the source-specific anchor predicate
     instead of the generic attached-block helper. Fence-gate placement now
     requires a buildable block below without adding unsupported-gate popping
     when the floor later changes, and closed gates now use the source
     1.5-block-tall collision strip instead of the visual model's post/bar
     gaps. Restored
     open, unpowered openables now preserve their metadata on the first
     scheduled redstone tick instead of being forced shut by an empty runtime
     power cache. Orphaned upper wooden-door halves now ignore manual
     activation and powered scheduled ticks without recreating a missing lower
     half. Villager-opened wooden doors now use the same sound, paired-mesh
     refresh, and surrounding mechanism-update path as other door state
     changes.
   - Pistons: verify pushability table, 12-block limit, sticky pull/short-pulse
     behavior, tile-entity immobility, entity pushing/damage, moving block
     state, rendering, and save/load cleanup. Successful extension/retraction
     now emits the old `tile.piston.out`/`tile.piston.in` world sound events
     with the source randomized pitch bands.
     Piston movement bounds now use the Release 1.0 valid block range
     `0..127`: extension into top/bottom edge cells and sticky pull from `y=0`
     are covered, while pushing past the top of the world remains blocked.
     Rail push reaction now distinguishes supported movement from unsupported
     popping for normal, powered, and detector rails, and moving-piston
     completion validates the final block's own support. Standing and wall
     signs now follow the source tile-entity pushability rule and block piston
     extension instead of being crushed as fragile blocks.
     Snow layers now follow the source no-push/destroy mobility path: extension
     breaks front snow layers into snowballs and sticky retraction does not
     pull snow layers as ordinary movable blocks.
     Piston and sticky piston textures now use the source metadata rules for
     front, side, back, and extended faces, and sticky moving heads retain the
     sticky metadata bit through the moving-piston renderer and settled head.
     Moving piston blocks now actively displace mobs, players, and dropped
     items during their travel ticks, including entities that enter the swept
     path after the piston has already started moving. Player displacement now
     also applies the same shove velocity as other piston-moved entities for
     both instant extension pushes and active moving-piston travel. Live
     redstone changes now also schedule pistons that depend on the block space
     above them for Release-style quasi-connectivity, including powered
     solid-block relays beside that above-space. Piston push and sticky-pull
     handling now flow through an explicit Release-style mobility decision
     table, keeping replaceable cells, destroy-on-push blocks, movable blocks,
     and immovable blocks on separate paths for both extension and retraction.
     The mobility table is now named and centralized: locked chests block
     piston movement with the other legacy immovables, fire/fluids are treated
     as displaced cells, fragile plant/attachment/control blocks crush through
     the destroy path, and movable ordinary blocks such as pumpkins, melons,
     leaves, and dragon eggs no longer fall into the fragile bucket.
   - Rails/minecarts: verify rail shape resolution, powered/detector rail
     behavior, cart physics, chest/furnace minecart inventory/fuel, collisions,
     drops, and persistence. Detector rail activation, eight-rail powered rail
     propagation, delayed detector rail depowering after carts leave, inset
     detector-rail minecart sensing, core minecart drops, powered-rail
     stopped-cart launch direction, and furnace-cart collision bias now have
     focused coverage. Ascending rail gravity now nudges stopped carts downhill
     for all four slope directions using the old `1/128` acceleration step,
     then applies the old rail path height-delta speed adjustment after the
     movement/friction step. Minecart `y` now also follows the post-move rail
     path so slope-to-flat transitions place carts on the upper rail height.
     Furnace minecart push vectors now store and persist the raw old
     `cart - player` delta while normalizing only for engine force. Non-fuel
     and empty-hand use now refresh push direction without adding fuel. Furnace
     cart rail force now uses the old post-move `0.8` damping, `0.05` engine
     push, and `0.98` no-push drag. Unpowered powered rails now use the old
     `0.03` stop threshold before halving faster carts, and mounted rider
     input now bypasses that brake for the source-style stopped-cart nudge.
     Rail speed limiting now clamps X and Z independently at the old `0.4`
     movement cap without overwriting stored motion, and carts occupied by
     either a player or captured living mob apply the old 75% passenger
     displacement multiplier before that cap. Curve rails
     now project horizontal position onto the Release rail matrix before moving
     and project motion through the same matrix while preserving approach
     direction via the source dot-product flip. Carts that cross into a
     neighboring rail cell now realign stored motion to that crossed block
     direction after rail friction and before powered-rail acceleration. Active
     powered rails now add their `0.06` boost or stopped-cart launch after the
     movement and rail-friction step instead of before the current
     displacement. Off-rail carts now use the old minecart fallback clamp,
     gravity, ground damping, and airborne drag path instead of generic entity
     physics. Rail ticks now preserve yaw until the cart has moved past the
     old `0.001` squared-distance rotation threshold. Minecart side contacts
     now pass through the old yaw-axis alignment gate before applying
     collision impulses, and accepted collision impulses now use the source
     distance-scaled push vector. Detector rail
     occupancy updates now preserve same-tick shape recalculation. Moving
     storage/furnace minecarts now also shove overlapping living entities
     through the world collision pass instead of only resolving cart-to-cart
     overlaps, while empty rideable carts capture living mobs as seated
     passengers only after exceeding the old collision speed threshold, and
     save/load that passenger reference. Empty rideable carts now refuse to
     steal a living passenger that is already seated in another
     minecart, and non-owning carts ignore that seated passenger during the
     collision sweep instead of treating it as a loose mob to mount or shove.
     Minecart collision now also includes the local player object, so
     unmounted players are pushed by overlapping carts instead of being absent
     from the entity-only sweep, while mounted players are excluded from their
     own cart's shove. Non-cart player/mob shove now uses the old
     squared-distance impulse split: carts receive the full collision push,
     while bumped players and living entities receive the quarter-strength
     velocity nudge, and exact center overlaps now separate from cart motion or
     a deterministic fallback instead of returning a no-op. Powered rail
     propagation now walks the lower-ascending-to-upper-flat slope connection.
     Exact-overlap minecart
     collisions now resolve from relative motion instead of becoming a
     zero-distance no-op. World-level placement now rejects non-minecart items
     while preserving rideable, chest, and furnace cart placement, and carts
     placed on ascending rails now receive the source-style half-block spawn
     lift. Ordinary rail curve metadata `8/9` now stays separate from the
     detector/powered rail power bit when shape updates recalculate old curves
     into straight rails. Ordinary rail junctions now use the source
     two-connection neighbor acceptance rules and redstone-powered curve
     preference, so four-way and T-style rail junctions can route and flip like
     Release-era minecart switches instead of defaulting to the simplified
     east/west straight-line heuristic. Ascending ordinary, powered, and
     detector rails now validate their raised-side normal support as well as
     the base support, so high-end support removal pops the slope with its
     correct rail item drop. Player minecart dismount now searches
     for a clear
     adjacent player box instead of always placing the rider east of the cart,
     preventing blocked-side dismounts from leaving the player inside loaded
     solid collision. Boats and minecarts now take exposure-scaled explosion
     damage and destroyed vehicle variants drop their legacy components/contents
     without their newly spawned drops being consumed by the same blast. They
     also take fire, lava, and cactus contact damage through the old vehicle
     attack threshold, preserving boat component drops and storage/furnace
     minecart contents/components when hazards destroy them. Arrows and
     fireballs now also collide with boat/minecart hit boxes, so shots and
     fireball impacts no longer pass through vehicles before the backing block
     or open air handles the projectile.
     Rail update scheduling now also wakes direct vertical, horizontal,
     two-block, and diagonal slope-neighbor rail cells immediately around block
     changes, so powered rail chains, raised rail supports, and adjacent slope
     candidates refresh without waiting for a later delayed mechanism tick.
     Minecart placement and rail physics now share `RailShapeResolver`
     metadata decoding, keeping normal rail curve metadata distinct from the
     powered/detector rail power bit at every shape sample. Powered-rail
     propagation now uses that shared shape decoder as well, and the internal
     rail connection helper handles all ten Release-era rail shapes instead of
     masking curve metadata down to straight rails. Minecart physics now wakes
     detector rails at both the start and end of the rail step, so a cart that
     begins on a detector rail still triggers it before projection/movement can
     leave the cell.
     Minecart entities now also render as an open tub mesh
     instead of a flat cart item sprite, with chest/furnace payloads visible
     in-world and fueled furnace carts switching to a lit furnace face. Chest
     minecart containers now also track the live cart entity while open, so
     moving out of the old 8-block use range or destroying the cart closes the
     storage UI instead of leaving a stale inventory screen usable from afar.
     Piston-displaced minecarts now also resync living passengers immediately
     after the cart position changes, keeping seated mobs attached during
     piston shoves and moving-piston travel.

4. Entities, mobs, combat, and spawning
   - Audit every Release 1.0 mob: passive animals, wolves, mooshrooms,
     villagers, hostile overworld mobs, Nether mobs, silverfish, slimes,
     magma cubes, Endermen, and the Ender Dragon.
     Snow Golems now have a factory-backed runtime entity, construction block
     pattern, renderer model/texture path, hostile snowball targeting, and
     snowball drops that ignore Looting. Cool-biome snow trails, water contact
     damage, and hot-biome damage now have focused coverage. Snowball throw source, arc,
     speed, and bow cue now have focused coverage. Snow Golem model animation
     now uses the source quarter body-yaw turn and fixed orbiting stick-arm
     pose; remaining visual polish is any exact texture/scale review against
     captured Release-era renders.
     Giant zombies now exist as factory/spawner/save-backed legacy mobs with
     zombie humanoid visuals, 6x body scale, 100 health, 50-damage melee, and
     undead potion/enchantment behavior. They remain intentionally excluded
     from natural spawn tables because Release-era Giants were unsupported
     non-natural entities.
     Chicken egg timers now emit the source `mob.chicken.plop` sound when an
     adult lays an egg, and airborne chicken descent now applies the old 60%
     downward-motion damping after physics updates.
     Villagers now retain generated professions while behaving as passive mobs:
     they flee when hurt, idle-wander, turn toward nearby players only while
     idle, render with a dedicated villager model instead of the generic
     humanoid model, open closed wooden doors while moving through village
     buildings, ignore iron doors, proactively flee nearby zombies before
     contact, and trading remains intentionally absent for the Release 1.0
     target.
     Ghasts, Blazes, Magma Cubes, and Zombie Pigmen now ignore fire/lava damage
     sources as Nether fire-immune mobs rather than merely refusing a burning
     timer. Ghasts now wind up before firing, expose the old charged texture
     state, play charge/fireball cues, and spawn explosive fireballs from the
     forward mouth/body offset rather than from their center.
     Mooshroom adult interactions now cover bucket milking, bowl-to-stew,
     shearing into a cow with five red-mushroom drops, health transfer, shears
     damage, and the old large conversion particle; baby cows/mooshrooms reject
     those adult-only interactions.
     Zombie Pigmen now start and spread anger from player melee or player-owned
     arrow damage only, instead of becoming hostile from every incidental damage
     source. Their anger value now uses the old finite 400-799 tick window,
     counts down each mob tick, clears pursuit when it expires, and angry
     Pigmen schedule the old delayed angry cue while ordinary idle/hurt/death
     vocalizations route through the shared sound queue.
     Blazes now also take water-contact damage while retaining fire/lava
     immunity and the existing snowball vulnerability. Their combat loop now
     supports close-range melee plus a charged three-fireball volley spaced at
     20-tick intervals before the longer post-volley cooldown, with
     flame/smoke charge particles, shot flame particles, and ambient/hurt/death
     sound cues. Their renderer now uses the Release-style three-ring,
     vertically bobbing rod orbit rather than fixed-height rods. Blaze rods now
     require recent player-credit damage, including tamed wolf melee credit and
     player-owned snowball projectile hits, and use the old single widened
     Looting count roll instead of the shared base-plus-Looting helper.
     Magma Cubes now keep their Release-specific overrides instead of inheriting
     every slime rule: size-one magma cubes still damage players, contact
     damage is size plus two, jumps use size-scaled lift/longer idle delay, and
     the post-1.0 magma cream drop is excluded for the Release 1.0 target.
     Large slimes and magma cubes now split into 2-4 same-family, half-size
     children at the old offset and half-block lift. Slime-family mobs now also
     keep their landing squash/audio feedback without taking generic fall
     damage, preventing accidental drop-death splits from long falls. Slime-family
     contact attacks now also require source-style 3D distance and line of sight,
     so close X/Z positions alone cannot damage players through vertical
     separation or intervening solid blocks.
     Creepers killed by skeleton-fired arrows now add the old `13`/`cat`
     record drop instead of only dropping gunpowder; generic and non-skeleton
     deaths still do not drop records.
     Spider and cave-spider eyes now follow the source rare-drop gate: no
     environmental spider-eye drops, player-credit required, and Looting can
     widen the chance. Spiders now also treat horizontal collision as
     climbable contact, giving wall-climbing the same motion clamp, upward bump,
     and fall-distance reset behavior as the shared ladder/vine path. Spider
     daylight neutrality now uses the old `0.5` local-brightness cutoff instead
     of requiring very bright light, and spider-family melee AI now applies the
     source-style grounded mid-range leap roll and impulse. Cave-spider poison
     now belongs to accepted melee damage only, preserving the old Normal/Hard
     poison durations while rejected player damage no longer applies the
     status effect.
     Natural spider spawns now also roll the old 1-in-100 spider jockey path:
     accepted jockey rolls spawn a skeleton rider as a separate skeleton entity
     mounted on the spider, keep the rider synced to the spider during ticks,
     and dismount it when the spider dies or is removed. Spider jockey rider
     identity now also survives save/load and rejects missing or non-skeleton
     rider references as corrupt saved entity data.
   - Verify health, attack damage, armor handling, drops, XP, burning rules,
     drowning, fall damage, despawn rules, pathfinding, target selection,
     breeding/taming, and special attacks.
     Armor damage reduction now ignores armor items placed in the wrong slot,
     matching the valid equipment states enforced by Release-style inventory
     rules. Accepted non-armor-bypassing player damage now also wears equipped
     armor by the old quarter-damage durability rule, with one durability
     minimum per hit and immediate slot removal when a piece breaks. Active
     sword blocking now reduces blockable incoming player damage before armor
     and armor durability are calculated, while unblockable fall, drowning,
     suffocation, and magic damage bypass the guard. Active Resistance status
     effects now reduce accepted player and living-entity damage by the old
     20%-per-amplifier formula after the existing player difficulty, blocking,
     armor, and protection calculations. Passive
     animal and water-mob player-kill XP now uses the old random 1-3 value
     instead of a fixed one-point orb, while hostile mobs keep their fixed
     XP values. Player fall-source damage, including ender pearl teleport
     damage and normal landing damage, now bypasses difficulty scaling and stays on
      the source-aware fall-protection path. Players and living entities now
      suffocate while intersecting opaque full-cube blocks through a dedicated
      armor-bypassing, difficulty-independent damage source with the old
      half-window repeat cadence. Generic non-water-breathing living
     entities now use the old 300-tick underwater air counter and 2-damage
     drowning pulse at the -20 air threshold, while squid-style breathers opt
     out and keep their fixed water-mob land motion path; stranded Squid now
     reuse the old air timer as dry-out state, refilling in water and taking
     2-damage pulses at the `-20` threshold while saved mid-dry-out states are
     accepted only inside the source `-20..300` air range. Helmet Respiration
     now feeds the player underwater air
     countdown's old per-tick skip roll. Water Breathing status effects now
     keep player air full while underwater and opt living entities out of the
     shared drowning counter. Fire Resistance now prevents player/living-entity
     ignition from fire and lava contact and clears existing burn state instead
     of only cancelling fire damage while leaving the entity visibly burning.
     Player melee Looting
     now widens shared mob drop-count ranges on accepted player-kill hits via
     the old separate base and enchantment rolls, while Squid ink sacks keep
     their old single widened Looting roll.
      Wolves now support bone taming with one bone
      consumed per valid
     attempt and a one-in-three success chance, while tamed or angry wolves
     reject taming attempts. Tamed wolves now gain the old larger health pool,
     can sit/stand from right-click, can heal from meat, and persist sitting
     state. Standing tamed wolves now persist explicit owner names, follow and
     long-distance teleport only to their named owner, search nearby vertical
     landing offsets, avoid unloaded/colliding/hazardous teleport destinations,
     assist their owner's combat by targeting entities the owner attacks or is
     damaged by, and bite assigned targets while avoiding creeper/ghast
     targets. Tamed wolves that
     are hurt now stand up and target valid living attackers, resolving
     arrow damage back to its shooter instead of targeting the projectile.
     Wild wolves now
     become angry from player melee or player-owned arrow damage, alert nearby
     calm wild wolves when the hit is nonlethal, and retaliate against the
     world player instead of fleeing like passive animals. Calm
     wild wolves now hunt nearby sheep without using the tamed owner-assist
     target path. Wolf
     visual state now includes owner-only meat begging, wild bone begging,
     sitting/begging model poses, angry tail pose, wet-to-dry shake rolls,
     wolf shake sound, splash particles, and in-world heart/smoke tame
     particle bursts through the shared transient particle renderer. Player critical
     hits now emit in-world crit particles only after damage is accepted.
     Strength and Weakness now feed player attack damage with the old
     bit-shift modifier values, and status-effect merging now preserves
     stronger active amplifiers for players and living entities. Speed and
     Slowness now also feed the living-entity AI motion path, matching the
     existing player movement multiplier so splashed mobs do not keep walking
     at base speed while the effect is active. Mob navigation now also refreshes
     on vertical target changes and reorders improved A* open-set nodes, so
     chase paths toward players, living targets, wolves' assist prey, and
     creeper/wolf anger targets do not keep stale same-X/Z paths across stairs,
     drops, and ledges. Regeneration and Poison now use
     their old separate amplifier-dependent tick cadences for both player stats
     and living entities: Regeneration uses
     `50 >> amplifier`, while Poison uses `25 >> amplifier`. Timed
     Poison/Regeneration no longer attach to undead mobs, spider-family mobs
     reject Poison, and instant Healing/Harming still split the old beneficial
     `4 << amplifier` amount from harmful `6 << amplifier` magic damage with
     undead inversion.
     Wheat-fed cow/mooshroom/pig/sheep/chicken breeding now covers love mode,
     60-tick courtship before baby spawning, parent cooldown age, no breeding
     XP orb, player wheat consumption, baby rejection, save/load of active love
     ticks, neutral-adult wheat-follow AI, immediate wheat-use heart bursts,
     repeated wheat refresh on already in-love adults, accepted-damage love
     cancellation, visible love heart particles, the seven-heart birth burst,
     newborn facing inheritance, and nearby mate seeking. Love-mode animals now
     prioritize mate seeking instead of continuing to chase held wheat, and
     breeding cooldown parents can trail nearby babies. Saved animal
     age/love-mode payloads now reject impossible values and love-mode
     combinations before restore.
     Baby animals now also follow nearby same-species adults until close or
     grown. Love-mode mate seeking now preserves the full 1.25x continuation
     leash instead of cutting off early from a squared-distance multiplier.
     Mate seeking, wheat temptation, baby parent-follow, and cooldown
     parent-child following now use their source follow-speed multipliers.
     Panic AI now also starts from active fire state, so burning passive
     animals and villagers flee immediately before the first delayed fire
     damage pulse.
     Baby/adult and post-breeding-cooldown age transitions now stop active
     age-gated follow goals and clear stale follow movement immediately on the
     boundary tick, so grown animals do not keep a parent/child-follow impulse
     after the behavior has become invalid. Hosted multiplayer entity
     snapshots now also carry age and love-mode counters for every mob, so
     remote baby scale, parent cooldowns, and love timers stay aligned with
     the authoritative world. Remaining animal polish is broader
     source-auditing of less visible behavior constants and visual edge cases.
      Burning baby mob overlays now follow the same
      half-size render scale as the body instead of rendering adult-sized fire,
      and baby animal width/height plus bounding boxes now use that half-size
      gameplay scale until adulthood. Species-specific eye/source/contact
      calculations now also use the scaled size for blaze, ghast, Enderman,
      Snow Golem, squid, spider, and Ender Dragon logic.
      Creepers now emit the old `random.fuse` cue once on the first fuse tick,
      while already-ignited restored creepers continue fusing without replaying
      the priming sound every tick. Hurt silverfish now wake nearby monster eggs
      into air plus spawned silverfish instead of leaving replacement stone
      behind, using the old tall `+/-10` vertical source search rather than a
      shallow same-floor band, and idle silverfish can hide back into compatible
      stone-family blocks as monster eggs when they are no longer pursuing a target. Existing
      monster-category mobs now remove themselves on Peaceful difficulty ticks
      instead of merely losing targeting/spawning behavior.
      Monster distance despawning now uses the old 128-block hard cutoff plus
      the 32-block/600-tick/1-in-800 soft despawn gate instead of the earlier
      256-block hard cutoff and deterministic far timer. Water creatures now
      use the same distance/age despawn path, so old or far squid release the
      water-creature cap instead of persisting forever.
      Exposed daytime Endermen now use the same old bright-sky random escape
      roll as daylight-sensitive hostile behavior, teleport away with portal
      feedback when the roll succeeds, and drop active anger/move targeting
      instead of continuing the same attack path after the escape.
      Endermen now dodge thrown snowballs and eggs through the same teleporting
      projectile-rejection path as arrows, preventing zero-damage projectile
      impacts from setting hurt feedback, player-credit damage, or last-damage
      source state.
      Stared-at angry Endermen now suppress the normal melee hit while eye
      contact is maintained and random-teleport from close range, while
      looking away still permits the ordinary Enderman melee hit.
      Enderman carried-block pickup now uses the Java-era 1-in-20 tick roll
      instead of the previous 1-in-200 delay, and focused coverage verifies
      metadata-preserving pickup plus valid supported placement.
      Skeleton ranged combat now uses the old close-range bow profile:
      eligible skeletons fire immediately, keep shooting at players closer than
      four blocks instead of backing away, wait 30 ticks between shots, and
      launch slower arcing arrows from the raised skeleton bow position.
      Burning skeletons now pass the old 100-tick fire payload to those arrows,
      and fire arrows can ignite the player through the same accepted-hit path
      already used for living mobs.
      Skeleton rendering now also applies the bow-aim arm pose while the
      ranged attack goal is active, including restored active goal state, so
      skeletons visibly aim during ranged combat instead of only spawning
      arrows from the logic layer.
      Shared melee and ranged attack goals now honor explicit living-entity
      combat targets before falling back to the world player. Assigned targets
      are looked at, pursued, line-of-sight checked, and damaged/projectile
      attacked through the same goal cadence, so saved or AI-assigned
      mob-vs-mob combat no longer stalls when no player target exists.
      Shared hostile hurt handling now also resolves living attackers from
      direct mob melee plus arrow, thrown-item, and fireball projectiles and
      assigns them as combat targets immediately. The mob AI target cache now
      rejects self, dead, and removed targets centrally, while direct fireball
      hits preserve projectile ownership so hostile retaliation follows the
      actual shooter instead of the transient projectile. Zero-damage egg and
      snowball hits now also notify the living hurt hook after recording their
      projectile source, so hostile mobs can retaliate to Snow Golem or other
      living-entity throws instead of only flashing hit feedback.
      Direct/projectile attacker resolution is now centralized for direct
      hits, arrows, thrown items, and fireballs, and the legacy revenge goal
      uses that resolver with removed/dead target rejection plus Y-aware chase
      updates. Tamed wolf retaliation, spider provocation, and Zombie Pigman
      player-aggression checks share the same helper instead of maintaining
      separate arrow-owner branches.
      Zombies, Skeletons, Creepers, Spiders, and Cave Spiders now emit their
      old hostile hurt/death sound events through the shared world sound queue,
      with lethal hits waiting for the death tick instead of also playing a
      duplicate hurt cue.
      Silverfish now also emit their Release-era ambient, hit, and kill sounds,
      while Squid are explicitly covered as silent for ambient, hurt, and death
      events in the Release 1.0 target.
      Shared melee and ranged pursuit now use MobAI navigation for ordinary
      chase movement instead of bypassing the navigator with direct steering on
      every tick. This keeps stale target recalc, partial paths, ledges, stairs,
      and obstacle detours in the same movement path for zombies, giants,
      silverfish, wolves, pigmen, Endermen, spiders, creepers, and skeletons,
      while preserving short direct steering for strafing, backing away, and
      stuck alternate-path escape. Remaining item-5 AI risk is exact
      source-matching of every old goal constant and mob-specific priority edge,
      not a missing shared chase/navigation implementation.
   - Verify spawn tables by biome/dimension, light level, pack size, block
     constraints, world cap, and player-distance rules.
     Natural hostile spawn light checks now use the Release-era random gate:
     Overworld raw sky light is checked against `nextInt(32)`, then
     time-adjusted sky/block light is checked against `nextInt(8)`, while
     Nether and End hostile checks keep using block light only. Passive spawn
     light checks account for block light plus time-adjusted sky light instead
     of raw sky light alone. Natural ground and water-creature spawns now create same-species packs from
     Release 1.0 definition min/max pack sizes, clamp those packs to the
     remaining Release-style eligible-chunk-scaled category cap, keep the
     24-block player exclusion for each pack member, reject living-entity
     collisions, and validate the selected mob's full block/fluid collision
     volume. Natural cap counts now ignore ambient/utility/boss mobs, so
     villagers and Snow Golems do not block ordinary animal spawns, and now
     include pending plus generated-but-not-yet-merged mobs so same-tick queued
     spawns reserve cap space before the next entity merge.
     Ghast natural spawning now follows its Release-era flying-mob path: the
     large Ghast volume must be collision/fluid clear, but the selected
     position does not need a solid support block, ignores ordinary hostile
     light rejection, and rolls the old one-in-twenty Ghast spawn gate before
     placement. Ground Nether mobs such as Zombie Pigmen keep the supported
     dark-position path.
     Slime natural spawning now uses the Release-era slime-chunk gate and
     below-layer-40 height cap through both random hostile rule selection and
     explicit pack spawning, while ignoring ordinary hostile light rejection in
     valid slime chunks.
     Ground spawn packs now re-check block/fluid collision against the actual
     mob instance after size-specific construction, so large rolled slimes and
     magma cubes cannot pass the small definition preflight and appear clipped
     into nearby blocks.
     Water creature spawn rules now use the Release 1.0 water-cell predicate:
     water at the candidate spawn cell and non-normal-cube space above it.
     Explicit squid pack calls and runtime water-creature sweeps now share that
     predicate, so surface water can spawn squid while solid head blockers still
     reject the candidate.
     Natural spawn attempts now also enforce the old 24-block exclusion around
     the stored world spawn, and loaded/new/command-set world-spawn state is
     synchronized into `World` so the spawner uses the same anchor as respawn
     and save metadata.
     Runtime natural spawning now uses the old 17x17 eligible chunk square
     around the player for cap scaling, skips that square's outer border for
     actual placement, and picks in-chunk candidates from the inner area rather
     than relying on a single radial candidate that could miss all loaded valid
     spawn surfaces.
     Runtime natural spawning now also runs three local group attempts per
     selected eligible chunk, with up to four short-drift candidate checks per
     group and same-entry pack targeting, so one valid selected chunk can fill
     multiple remaining cap slots when its nearby blocks pass the normal
     light/support/distance/collision rules.
     Jittered natural ground-pack members now re-check nearby local ground Y
     before placement, matching explicit pack helper behavior and allowing
     same-pack mobs to land on uneven but valid terrain rather than all sharing
     the first candidate's Y level.
     Runtime natural ground spawning now chooses the first Y candidate from the
     selected chunk column and tests that Y directly for natural attempts,
     instead of searching up/down around the player's height. Explicit pack
     helpers still keep their controlled local search behavior.
     Public natural spawners now consume the owning world's RNG instead of
     creating an unseeded per-spawner stream. Passive animals and squid now
     run whenever their category is below the Release-style cap during the
     natural-spawn pass, instead of being skipped by extra CraftZero-only
     random category gates. Natural ground and water spawns now also apply the
     old randomized spawn yaw with zero pitch across explicit pack helpers and
     runtime spawning, so spawned mobs no longer all start facing the same
     default direction. Runtime natural spawning now runs every fixed world
     tick instead of waiting for a local one-second throttle, preserving the
     existing Release-style caps and pack gates while making valid spawn
     opportunities appear at the old cadence.
     Natural passive selection now follows the Release 1.0 biome
     creature lists: forests/taiga include wolves with source weights, deserts,
     beaches, rivers, oceans, Hell, and Sky have no passive creature list, and
     mushroom islands use mooshrooms only while suppressing natural monsters.
     Naturally spawned sheep now also use the old weighted fleece-color roll,
     including black, gray, light-gray, brown, and the rare pink branch, instead
     of every spawned sheep defaulting to white.
     The End hostile natural list now exposes Endermen instead of leaking the
     Overworld monster table through weighted selection.
     Wolf and mooshroom definition pack sizes now match the source worldgen
     groups, and mooshrooms require mycelium support for natural passive packs.
     World-backed one-time creature population now uses the same collision-box
     and fluid-volume style gate as Release-era spawning, allowing generated
     passive mobs in collision-free tall grass while preventing water or
     colliding block intersections, and reads the post-decoration scratch state
     so the shifted spawn area sees late generated terrain/features.
   - Finish projectile/entity behavior for arrows, fireballs, splash potions,
      thrown items, ender pearls, boats, dropped items, TNT, XP orbs,
      Eyes of Ender, falling blocks, and remaining End crystal edge cases not
      covered by the current generation, cap-top placement/fire maintenance,
      healing, immediate dragon link timing, explosion, TNT-priming, non-living
      status/healing immunity, direct arrow/fireball/thrown-projectile crystal
      contact, End progression edge cases, and remaining transient entity
      save/load tests.
      Thrown eggs/snowballs and splash potions now collide with boats and
      minecarts before backing blocks; thrown items consume on the vehicle hit
      point without damaging vehicles, and splash potions use the vehicle
      surface as the radius origin.
     Falling blocks now preserve the old out-of-world
     100-tick drop gate, hard 600-tick timeout, and source-shaped
     air/fire/water/lava falling predicate. Boats now break collided
     lily pads through the normal block-break
     path so water lilies drop as items instead of being deleted, and
     player-broken boats now drop 3 planks plus 2 sticks instead of the later
     boat item. Boats now also run a post-tick entity collision pass, shoving
      overlapping living entities, unmounted local players, and other boats with
      horizontal Release-style impulse instead of phasing through them, while
      mounted boat players stay under riding synchronization. Fast boats moving
      in water now emit the old splash wake particles, while slow or dry boats do
      not. Flowing water now pushes boats along source decay gradients before
      their existing per-axis speed cap, so channels and generated streams carry
      unpowered boats instead of acting like still water. Boat motion now clamps
      the two horizontal axes to the old `0.4` cap independently and turns the
      hull toward travel by at most 20 degrees per tick instead of snapping yaw
      directly to motion. Placed and ridden boats
      now render as a textured hull mesh rather than a flat item sprite. Boat item placement now rejects occupied spawn boxes, including
      same-tick pending boats, before consuming/spawning. Arrows and fireballs
     now collide with boats and minecarts instead of phasing through them,
     routing direct hits through vehicle attack/explosion behavior and preserving
     old component/content drops. Thrown eggs/snowballs, splash potions, and
     Ender Pearls now also stop on boat/minecart hit boxes before backing block
      impacts. Arrows, fireballs, thrown eggs/snowballs, splash potions, Ender
      Pearls, splash-potion radius effects, and fishing-hook sweeps now include
      queued same-tick entities in their collision/effect scans instead of only
      the committed entity list. End crystals now bypass
      living-entity damage gates entirely: their damage gate accepts non-fire
      attack sources even when the damage amount is zero, so snowballs, eggs,
      fireballs, and other projectile contacts immediately remove and explode
      the crystal even if a living invulnerability-frame state would have
      rejected ordinary mob damage.
      Explosion-sourced End crystal damage now keeps the old non-recursive
      behavior: the hit crystal is removed and active dragon healing is punished,
      but no additional crystal blast, sound, or huge-explosion particle is
      spawned from that secondary crystal destruction.
      End crystals now also have direct transient entity save/load
     coverage proving fixed position/orientation/age round-trip, non-living
     potion/health immunity, restored End-dimension fire maintenance, and
     rejection of zero-health saved crystal payloads that would otherwise
     resurrect destroyed crystals.
      Ender pearl throwing no longer has a post-1.0 item cooldown, so
      back-to-back throws consume and spawn projectiles immediately. Saved Ender
      pearls now reject missing player ownership and age `>= 1200` before
      restore, so ownerless or expired teleport projectiles cannot be revived.
      Ender Pearl block and entity impacts now emit the Release-era 32 portal
      particles at the hit point, using the old vertical-column spread, before
      the normal owner teleport and five-point fall-source damage path. Ender
      Pearls now also collide with paintings, boats, and minecarts before
      backing blocks, popping paintings and teleporting to vehicle hit boxes
      rather than passing through them.
     Fully drawn player bow shots now create critical arrows, emit visible
     crit-particle trails while flying, apply the old randomized critical
     damage bonus on accepted living/player hits, and preserve the critical
     flag through active projectile save/load.
     Player hand-thrown eggs and
     snowballs now consume one stack item, spawn the existing thrown-item
     player-owned projectile from the player view, queue the old low-volume
     `random.bow` throw cue, allow back-to-back Release 1.0 throws, and apply
     the old zero-damage living-target hit plus projectile-shaped knockback for
     ordinary egg/snowball impacts while preserving the projectile as the hit
     source. Player-owned thrown
     items now grant player-credit damage for loot-gated outcomes such as
     snowball-hit Blaze rod drops, while preserving snowball Blaze damage and
     End crystal destruction. Saved thrown
     items and splash potions now reject age `>= 1200`, and saved fireballs
     reject age `> 600`, matching their runtime removal gates. Thrown Eyes
     of Ender now have focused player-level consume/spawn coverage, source-style
     short waypoint steering for far stronghold targets, source-shaped in-flight
     acceleration/vertical pull/near-target damping, and focused
     `age > 80` drop-versus-shatter lifetime coverage. Eyes also emit the old
     one-particle portal trail while travelling, and failed shatter rolls now
     show the old eye item-crack chips plus inward portal rings instead of
     vanishing silently. Player-owned stuck
     arrows now emit the old pickup pop when collected. XP orbs now start
     without a normal per-orb pickup delay and respect the short player pickup
     cooldown before stacked orbs pay out, with `random.orb`
     emitted only on successful payouts. XP orbs now also preserve the old
      ground bounce instead of flattening on first contact, use their
      five-health damage path for explosions, reject expired-age and
      zero-health saved payloads before restore, and lava-contact
      orbs emit the old low-volume, high-pitch `random.fizz` cue after the
      source tick's gravity decrement, preserving the full old upward lava
      reset before drag. Their attraction vector now targets the player's
      eye/head height rather than `y + 1.0`, matching the old `getHeadHeight`
      pull point. Dropped item
      collection now emits the
      old `random.pop` pickup cue only after pickup delay, range, and inventory
      capacity checks pass, and dropped item entities now split oversized spawn
      counts plus partially merge into nearby matching stacks without exceeding
      item max-stack limits. Dropped item entities also carry their old
      five-health damage state, reject expired-age and overfull-health saved
      payloads before restore, and explosions can destroy them through the same
      exposure/damage pass used by other entities. Fire and lava contact now
      damage the same item-entity health state, with lava applying the old
      stronger fire hit.
      Lava fizz and upward/randomized bounce now run on the old first-contact,
      block-cell movement, or 25-tick item cadence instead of being reset every
      update. Nearby matching dropped items now also run a runtime merge scan
      on that same item cadence instead of only merging at spawn/add time.
      Default
      item-entity creation now uses the source-shaped random horizontal pop
      instead of a straight vertical launch, and landing now preserves the old
      bounce/ground-friction motion instead of freezing item entities on first
      contact. Grounded item drag now uses block slipperiness times `0.98`, so
      ice preserves more item motion than normal blocks, and airborne item
      vertical motion now applies the old gravity decrement plus `0.98`
      vertical drag before landing bounce. Item pickup now also
      accepts partial stack
     transfers into nearly-full inventories and leaves the leftover entity
     count in-world. TNT block
     priming now emits the old `random.fuse` sound through the world sound
     queue for redstone, fire, player ignition, and explosion-chain priming
     paths. Primed TNT now also uses the old circular 0.02 launch impulse,
     emits smoke while the fuse is live, and bounces with half vertical
     reversal on ground impact. The shared explosion path now emits the old
     `random.explode` cue with Release-style loud volume and randomized pitch,
     the animated large explosion particle at the blast center, and bounded
     source-shaped `explode`/`smoke` debris pairs at destroyed block positions
     for TNT, creepers, beds, End crystals, and fireball explosions, with
     small fireballs using `largeexplode` instead of the TNT-sized
     `hugeexplosion` center sprite. Player bow release and
     skeleton ranged attacks now emit the old `random.bow` cue with the
     Release-style randomized pitch band without making generic arrow spawns or
     dispenser arrows noisy. Player attacks, arrows, snowballs, and eggs now
     deflect ghast and blaze
     fireballs along the aim direction, deflected-player fireball state now
     survives save/load, and returned explosive fireballs now hurt fire-immune
     Ghasts through a player-credited non-fire direct hit before the normal
     explosion path. Non-explosive fireballs now place adjacent fire on
     block impacts, burn living targets only after accepted damage, and keep
     that burn side effect out of explosive fireball direct hits while explosive
     fireballs still route to the explosion path.
     Splash potion direct hits and edge-radius duration
     cutoffs now have focused coverage. Fishing rods now have a functional
     cast/reel/catch loop with raw-fish drops and rod durability, active bobber
     save/load, hooked-entity pull behavior, and hooked-target persistence
     across reloads, plus in-world fishing line rendering from the player hand
     to the bobber, a Release-style cast sound cue, and catchable splash
     particles. Bite/catch RNG now uses the source-shaped per-water-tick bite
     roll: `1/500` in clear/open water, `1/300` when rain can strike above, and
     a 10-39 tick catchable window once a bite lands. Public bobbers route those
     rolls through the world-owned RNG, and bite splash audio now uses the old
     lower volume plus randomized pitch band. Saved bobbers now reject missing player ownership, missing or
     unhookable hooked-target references, age `>= 1200`, and impossible states
     where wait and catchable timers are both active. Paintings now cover
     placement support, drops,
     same-tick pending-entity overlap rejection, mob/dropped-item hanging-space
     collision, projectile impacts including Ender Pearl impacts,
     explosion/support breaking, save/load, and Release 1.0 artwork catalog
     gating.

5. Physics and math
   - Source-audit player/entity movement constants, acceleration, friction,
     step height, jumping, swimming, lava/water drag, ladder/vine climbing,
     sneaking, sprinting, flying/no-clip debug behavior if supported, and
     collision resolution. Player survival mining progress now uses the
     Release 1.0 block-strength denominators, common block mining hardness
     values, and underwater/airborne penalties, with special
     cobweb/shears/leaves/wool cutting strengths feeding that formula. Aqua
     Affinity now removes only the underwater penalty while keeping airborne
     mining slowdown intact. Base entity/player cobweb movement now clears
     stored velocity after the damped source movement attempt, player ladder
     and vine contact now uses source-scale horizontal/downward movement
     clamps and the post-horizontal-collision upward climb bump, living
     entities opt into the same per-tick climbable branch, player
     jump/forward/back keys no longer bypass the source collision climb path,
     and ice plus soul-sand movement interactions have focused coverage
     against the shared physics path. Hunger-empty starvation and high-food
     natural regeneration now follow the Release-era 80-tick pulse cadence
     instead of difficulty-agnostic or continuous drains, starvation honors the
     old difficulty health floors, and walking, sprinting, and jumping now feed
     the old hidden 4.0-threshold exhaustion accumulator instead of directly
     applying per-second or per-jump hunger loss. Hunger status effects also
     feed that accumulator rather than directly lowering visible food, and
     accepted player attacks, applied player damage, and successful survival
     block breaks plus natural food regeneration now add their Release-style
     action exhaustion. Feeding and loaded player stats now keep saturation capped to the current food level.
     Peaceful passive healing now runs on its own one-health-per-second timer
     without refilling or draining the food bar.
     Player movement constants no longer use the older softened CraftZero
     values: gravity, jump velocity, walking speed, and sprinting speed now
     match the Release-style values already assumed by the movement code and
     have focused constant coverage. Sneaking now derives its speed from the
     old 30% walk-speed factor and the horizontal movement cap honors sneaking
     before sprint/walk speed, preventing retained sprint/walk velocity while
     crouched. Player-to-living and generic living-entity collision pushes now
     use the old max-axis collision impulse: scale by
     `sqrt(max(abs(dx), abs(dz)))`, cap the reciprocal multiplier at `1.0`,
     and apply the `0.05` horizontal shove instead of the older CraftZero
     fixed normalized push, with close-overlap and wide-entity overlap
     coverage. Player-to-living collisions now also apply the opposite
     horizontal shove to the player and use a deterministic same-position
     fallback so exact overlaps separate. Player and living-entity collision
     resolution now runs the old half-block step-height retry when grounded
     horizontal clipping would otherwise stop movement, choosing the better
     horizontal result after stepping up and settling back down. Player
     movement collision now also includes active moving-piston boxes through
     the same search used by generic entities, so pistons are solid to direct
     player movement as well as their explicit push pass. Player water movement no longer uses the previous faster
     bobbing/cannonball tuning: water drag is back to the old `0.8` per 20Hz
     tick, vertical water gravity is the converted `-0.02` motion-per-tick
     step, jump-held swimming adds the converted `+0.04` per-tick impulse, and
     sneak no longer applies a custom fast downward swim. The old hidden
     player surface-bobbing cooldown gate is also gone, so jump-held swimming
     applies immediately from the same Release-style water branch. Generic
     entity water physics now also drops the artificial sine bobbing, surface
     auto-lift, and hard water terminal velocity; ordinary entities apply water
     drag then the old `-0.02` per-tick water gravity step, while squid and
     other special entities keep their explicit drag/gravity overrides.
     Lava immersion now has its own heavy `0.5` per-tick drag branch for
     players and generic entities instead of falling through lava with air
      gravity; player jump-held movement uses the old slow lava swim-up impulse
      while existing lava damage remains on the hazard path. Fluid decay metadata
      now feeds a shared current vector: water carries players, generic
     entities, dropped-item entities, and boats along horizontal flow gradients,
      falling water sheets add the old downward side-pull, and lava carries
      players/generic entities along its own flow gradients. Shared entity
      motion setters now reject non-finite components before collision math sees
      them, including motion received from remote-pose application, so malformed
      projectile, mob, item, cart, or restored entity velocity cannot propagate
      NaN/Infinity through AABB movement. Entity and player pose entry points
      now also reject non-finite position coordinates before bounding boxes,
      camera placement, remote-pose interpolation, Ender Pearl teleports,
      dimension transfers, bed sleep/wake placement, or respawn state can feed
      invalid coordinates into the collision/raycast/explosion stack.
      Shared AABB geometry now normalizes inverted min/max endpoints at
      construction and use, sanitizes malformed offset/expand/move deltas, and
      fails closed when collision clips receive bad boxes or non-finite
      velocities. Shared block, fluid-source, and entity raycasts now normalize
      direction vectors, reject non-finite origins, zero-length rays, and
      invalid ranges before DDA/slab math runs, and use the normalized vector
      for hit distances/points. Line-of-sight, cliff, and explosion exposure
      helpers now reject malformed coordinates or bounds before sampling, so
      corrupted geometry cannot poison AI visibility or blast attenuation.
      Shared damage-source records now sanitize invalid coordinates, null
      damage types, knockback values, and looting levels, and player/living
      hurt paths reject non-finite damage before armor, resistance,
      invulnerability, or knockback calculations run. Living-entity `addMotion`
      now routes through the base finite-motion guard, closing the last
      subclass bypass for mob/player/explosion push. Explosion distance,
      impact, damage, push, particle, block-drop, and ray-propagation helpers
      now fail closed on invalid coordinates or power, preventing malformed
      blast inputs from generating NaN damage, motion, or destroyed-block walks.
     The main
     simulation loop now advances gameplay at the Release-era 20Hz fixed tick,
     so entity AI, mob age/breeding counters, projectile lifetimes, and other
     source-tick systems are no longer driven by the previous 60 UPS loop.
   - Verify raycast math, block selection boxes, entity hit boxes, explosion
     propagation, knockback, block break speed, tool harvest tiers, and fluid
     flow timing. Explosion rays now use explicit Release 1.0 resistance inputs
     instead of the mining hardness table.
     Entity ray picking now also uses a single Release-style collision-border
     path: entities expose the old `0.1` pick expansion, `Raycast` owns the
     shared AABB slab intersection helper, player hit boxes use the same border
     as entity hit boxes, and arrows, fireballs, Ender Pearls, thrown
     eggs/snowballs, and splash potions no longer carry separate ray/AABB
     player-hit implementations. The shared living-entity raycast now skips
     removed entities, and player combat/entity picking uses the active plus
     queued same-tick entity view so stale removed bodies cannot win targeting
     while newly queued valid entities are still visible.
     Haste and Mining Fatigue status effects now scale the player's survival
     block-break progress with the old per-amplifier mining-speed modifiers,
     matching the existing Efficiency/Aqua Affinity/underwater/airborne mining
     path instead of leaving those active effects as visual-only state.
     Jump Boost now adds the old per-amplifier vertical impulse for player
     ground jumps and shared living-entity jumps, including AI jump requests
     and one-block obstacle auto-jumps, instead of remaining a visual-only
     active status effect.
     Explosion block item drops now use the source-style inverse blast-power
     chance instead of the previous flat 30% roll, so TNT, creepers, bed blasts,
     fireballs, and End crystals no longer produce the same terrain yield.
     Flaming explosions now also use the Release-era fire aftermath rules:
     explosive fireballs request the flaming blast path, and post-blast fire is
     only placed above opaque full cubes instead of glass/chest/fence-style
     attachment supports.
     Empty-bucket and glass-bottle use now
     has a source-fluid-aware ray that does not change ordinary block selection.
     Block-change fluid mixing now uses loaded-only neighbor probes, so ordinary
     updates at chunk edges no longer synchronously generate adjacent chunks
     while lava/water hardening still works across already-loaded borders.

6. World, biomes, dimensions, and structures
   - Continue `WORLDGEN_PARITY_REPORT.md` for detailed terrain and structure
     parity status.
   - Remaining broad work: exact Overworld noise, biome decorators, caves,
     ravines, ores, lakes, dungeons, villages, strongholds, Nether terrain,
     Nether caves, fortresses if in scope, End island/spikes/dragon arena,
     broader visual precipitation validation against source captures, exact
     source lightning bolt geometry/source-capture validation, broader
     sky/light behavior, and final cross-dimension portal placement tuning.
     Natural thunderstorm lightning now uses a Release-style active-chunk
     scheduler: every thunder tick scans generated chunks around the player,
     applies the old 1-in-100000 roll per generated chunk, then uses the
     source-shaped chunk LCG to choose the struck column before reusing the
     existing bolt, fire, entity-damage, pig-transform, creeper-charge, sky
     flash, and thunder/explosion sound path.
     Nether portal transfers now reuse an existing loaded destination portal
     within the old 128-block search radius before creating a new portal frame,
     and the player landing position follows the reused/built portal interior
     instead of the raw scaled coordinate. If no portal is found, destination
     creation now searches nearby loaded terrain for a clear supported cavity
     before using the forced target build, and the generated frame builder
     supports both X-axis and Z-axis portal interiors. Survival and Hardcore
     players keep the old 4-second portal dwell, while Creative players now
     transfer immediately on entering the portal. Cross-dimension placement now
     also uses a transfer-specific player reset that detaches rideable
     minecarts, boats, and saddled pigs, clears incoming velocity, and resets
     fall tracking at the destination instead of carrying old-world riding or
     physics state through the portal.
     Overworld village and mineshaft locate calls now return the nearest
     generated start center through the shared structure locator, reusing the
     same generated start builders as chunk population.
     The one-time Overworld creature population pass now validates spawned
     animals against their full collision/fluid volume instead of requiring
     literal air in the center column, preserving Release-style spawning in
     collision-free ground cover while rejecting water and solid body
     obstructions. It now reads the post-decoration scratch state for the
     shifted spawn area, so off-chunk decorated support and obstructions affect
     generated passive packs before they are staged.
     Overworld population replay now also builds lake/dungeon scratch chunks
     from carved terrain plus generated structures, and overlays neighboring
     structure block differences into the mutable decorator scratch. Late
     population features can now see nearby stronghold/village/mineshaft blocks
     and carved air before deciding tree, plant, spring, ore, dungeon, and
     creature-spawn outcomes.
     Non-portal stronghold pieces now honor the source liquid-envelope abort
     used by old structure components, so water or lava touching a room's
     expanded placement box can skip that target-chunk room/corridor carving;
     portal rooms remain exempt and still place their chamber, frames, lava,
     and silverfish spawner.
     Taiga grass decoration now rolls the biome-specific fern variant, so
     generated taiga chunks contain metadata-2 ferns alongside ordinary
     metadata-1 grass instead of using only one ground-cover variant.
     Village path pieces now choose gravel height from the live generated
     column with source-style top solid/liquid semantics, ignoring leaves and
     accepting fluids instead of only using density-field terrain height.
     The End dragon exit portal now waits for the old 200-tick death sequence
     and uses the Release-style death-location-centered shape, and dragon XP is
     released through the old tick-155..200 payout cadence instead of one late
     lump; huge-explosion particles and the death sound are now queued during
     the old death sequence. End crystals now maintain their old ticking
     inner-rotation animation state and render as a bobbing/spinning 3D crystal
     mesh instead of a flat static sprite. The active End-crystal healing state
     now also renders as an animated twisted crystal-to-dragon tether. Flying
     dragons now carve ordinary loaded blocks out of their source-shaped
     head/body part boxes without item drops while preserving bedrock,
     obsidian, and End stone, and
     protected block contacts slow the next movement step. Dragon contact
     damage now also uses those source-shaped head/body part boxes instead of
     the previous coarse center-radius player hit, with part-centered knockback
     through the shared damage source path. Dragon retargeting can now choose
     the active live player's position, so the boss can actively pressure the
     fight through the existing flight/contact paths instead of only roaming
     around the island. Dragon flight now also uses the old turn-limited
     steering shape with yaw inertia, alignment-based acceleration and damping,
     motion-derived pitch, far/close stale-target retargeting, and a 64-sample
     movement history buffer for renderer/collision follow-up instead of
     snapping directly toward each waypoint. Dragon rendering now consumes that
     history for segmented neck/tail articulation, so flight visually bends
     through turns instead of staying rigid. Remaining dragon arena work is
     exact source body-part path-node constants, final model proportion/source
     capture validation, and texture-accurate beam material validation.

7. UI, rendering, sound, and animation
   - Verify GUI layouts and slot positions for inventory, crafting table,
     furnace, chest, dispenser, brewing stand, enchanting table, signs, death,
     pause, options, and creative/debug surfaces if present. Crafting-table
     shift-click behavior now has focused coverage for either Shift key,
     output crafting, craft-grid cleanup, and main inventory/hotbar transfer.
     Player-inventory and crafting-table recipe output handling now share the
     same take/quick-move path, including stack-aware repaired tools, repeated
     shift-click crafting, cake bucket remainders, and overflow remainder drops.
     Player inventory, crafting-table, chest/chest minecart, furnace,
     dispenser, brewing-stand, and enchanting-table screens now support
     number-key hotbar swaps on hovered slots and cursor-stack dragging with
     left-drag split distribution and right-drag one-item placement across
     eligible slots while preserving each container's slot filters. The same
     screen set also has double-click collection coverage, and double-click
     collection now uses the old context-sensitive scan order: player-side
     double-clicks drain player slots before the opened container, while
     container-side double-clicks drain the opened container/crafting slots
     before player inventory, with furnace output excluded and slot filters
     still enforced. Shift-click transfer now uses the same slot-filter-aware
     quick-move path across player inventory, crafting table, chest/chest
     minecart, dispenser, furnace, brewing stand, and enchanting table
     main/hotbar and player-to-container routes. It merges compatible stacks
     before filling empty destinations, respects slot caps such as single
     brewing bottles and enchanting-table input, leaves partial remainders in
     the clicked source slot, clears fully moved sources, and marks tile-backed
     containers dirty when quick-moved stacks land there. Container-to-player
     shift-clicking now also uses the old reverse player-inventory destination
     order through that same filtered quick-move engine instead of the generic
     add-to-inventory pickup path, so chest/chest minecart, furnace, dispenser,
     brewing stand, enchanting table, crafting-table grid, player 2x2 grid, and
     armor-slot transfers preserve partial source remainders, slot caps,
     tile dirtying, and brewing-potion take bookkeeping while leaving crafting
     output/remainder notifications on the dedicated crafted path. Remaining
     inventory parity work is mostly final source-order audit for any future
     specialized containers and runtime pixel/click review.
     Global Escape close now drains queued cursor/click-out stacks for the
     crafting table, chest, furnace, dispenser, brewing stand, and enchanting
     table instead of leaving dropped items trapped inside closed screen state.
     Open block-container screens now also self-close when their backing
     crafting table, chest, furnace, dispenser, brewing stand, or enchanting
     table fails the old same-block/tile and 8-block usable-distance check.
     Player inventory close now drops the carried stack plus 2x2 crafting-grid
     contents, and the inventory key no longer opens the player inventory under
     dispenser, brewing stand, or enchanting table screens. Shift-clicking from
     the player inventory's 2x2 crafting grid now routes through the full
     player inventory instead of only the main inventory. Death screens now
     include the old centered score line sourced from the player's progression
     score plus the Release-era `Respawn`/`Title Menu` button flow for ordinary
     deaths, while Hardcore deaths show the old no-respawn message and require
     `Delete World` instead of exposing normal respawn/title exits in either
     the overlay or menu path. Pause-menu Statistics is
     now backed by real saved counters for core general gameplay stats instead
     of a disabled placeholder, and now exposes General, Blocks, and Items
     pages backed by typed mined-block, crafted-item, used-item,
     depleted-item, fish-caught, games-quit, mob-kill,
     hostile-monster-kill, player-kill, and aggregate dropped-item counters that save/load. Mob kill counters flow
     through the shared mob death path, covering direct player melee kills and
     player-owned arrow kills. Fish-caught counters flow through successful
     catchable bobber reels, and dropped-item counters flow through successful
     hand drops into the source-style General `Items Dropped` row. Item-use counters now flow
     through accepted gameplay interactions such as fishing rods, bows,
     consumables, thrown items, buckets, bottles, block placement, jukeboxes,
     animal tools, and armor equip; depleted counters update when durability
     breakage removes a tool/item. Taking the End exit portal now opens a
     paused `The End.` completion screen after the Overworld spawn transfer,
     giving the Release 1.0 progression loop an explicit completion/continue
     flow instead of returning silently to gameplay. Pause-menu Achievements now uses
     source-era display coordinates, source-era icon items, parent connectors,
     scroll/drag tree navigation, hover tooltips, hidden-distance gating, and
     explicit unlocked, available, locked, and special-node states. The newer
     `MenuScreenFactory` path now mirrors those player-facing progression
     routes too: the pause menu has Achievements/Statistics buttons, the Done
     path returns to the game menu, and the factory Statistics screen reuses
     live saved counters with scrollable General, Blocks, and Items tabs.
     The Statistics screens now share source-style General naming/order, Blocks
     object rows for crafted/used/mined counts, and Items object rows for
     crafted/used/depleted counts. Remaining statistics polish is final
     pixel-exact category layout and visual row-spacing review against the old
     client.
     A Release-style `F3` debug surface is now wired through the normal gameplay
     input/render path. The overlay is built from a loaded-only world snapshot
     and shows FPS, display size, dimension/generator/seed, game mode,
     coordinates, chunk-local position, facing/yaw/pitch, biome, sky/block
     light, target block, time/weather/moon phase, loaded chunks,
     entity/item/particle counts, and Java heap usage in the existing bitmap
     HUD font.
     Options/Video Settings now use the Release-era control set and labels,
     including Far/Normal/Short/Tiny render-distance presets, Moody/Bright
     brightness labels, Auto/Small/Normal/Large GUI scale labels, and a real
     rendered-particle-density setting. Runtime render distance now honors
     those old presets at 12/8/4/2 chunks instead of silently clamping every
     choice to the previous 4-5 chunk range. 3D Anaglyph now uses a red/cyan
     two-eye world pass with source-style anaglyph color remapping applied to
     world-scene shader output while HUD/menu overlays stay readable. The alternate menu factory's video settings model now
     mirrors the runtime option surface for smooth lighting, performance,
     anaglyph, view bobbing, advanced OpenGL, brightness, clouds, and particles
     as well as graphics/render distance/GUI scale. The factory options path
     now also exposes the old `Language...` route, opens a dedicated language
     screen with selectable language entries and current-language feedback, and
     sends the selected language through the shared settings callback. Item-9
     UI/settings feature implementation is treated as covered for this pass;
     remaining work is final pixel-review polish, not missing option plumbing.
   - Verify block models, item icons, entity models, animations, particle
     effects, lighting, translucent/cutout sorting, liquid surfaces, chest lid
     animation, potion bottles, and moving pistons.
     The Video Settings particle-density option now uses Release-style render
     ratios across the shared particle renderer: All keeps every particle,
     Decreased keeps roughly two thirds, and Minimal keeps roughly one tenth
     instead of the previous half/third filter. Particle quads now also sample
     local sky/block light and render with that brightness during the particle
     pass, while fire/lava/drip-lava/explosion effects remain self-lit.
     Block-hit, block-destroy, sprinting tile-crack terrain chips,
     player/generic entity water-entry splash/bubble,
     drowning bubbles, Eye of Ender water bubbles,
     ambient lava sparks with source-style smoke handoff, lava-mixing fizz large-smoke, furnace-minecart
     `largesmoke` exhaust, torch smoke/flame, active redstone-torch zero-arg
     `reddust`, redstone-ore activation/ambient exposed-face sparkles,
     powered repeater zero-arg `reddust`,
     brewing-stand smoke, End-portal smoke,
     End-portal-frame eye-insertion smoke,
     fishing-bobber bite bubble/splash bursts, arrow/snowball/egg underwater projectile bubbles,
     source-phased water/lava underside drip, source-colored powered wire
     `reddust`, snowball/egg/splash-
     potion shatter, eating item-icon crumbs, splash-potion source-shaped
     colored spell/instant-spell clouds,
     snowball/egg snowball-poof impacts, depleted durable-item shatter, item
     pickup, `enchantmenttable` bookshelf particles, dedicated slime landing, snow-golem
     creation snowshovel bursts, mycelium town-aura spores, Nether water-bucket vapor large-smoke,
     source-shaped fire large-smoke,
     Blaze ambient large-smoke, fireball smoke trails and underwater fireball bubbles,
     mooshroom large-explosion conversion puffs, metadata-gated suspended underwater,
     animal love/breeding heart feedback, wolf tame heart/smoke feedback,
     low-depth void-fog/depth-suspend, ordinary crit entity emitters,
     source-motion critical-arrow trails,
     enchanted magic-crit entity emitters, and common mob
     death `explode` poofs now flow through the world particle queue and render
     with terrain, item, alternate-font, or particle atlas sprites.
     Physical particles now perform simple collision against loaded block
     collision boxes. Destructive explosions now also emit the old paired
     midpoint `explode` flash plus sampled-block `smoke` debris for destroyed
     block positions instead of smoke-only debris, and the center explosion
     sprite now uses `largeexplode` for small blasts and `hugeexplosion` for
     power-2-or-larger blasts. Shared entity emitters now use stable crit burst
     counts, entity-sized body-volume bursts, impact-scaled water-entry
     bubble/splash density, outward death-poof drift, and speed-gated sprint
     tile-crack dust; Blaze charge-up particles now fill the Blaze body volume.
     Item-7 particle emitter implementation is treated as covered for this
     pass; any remaining differences belong to final visual capture review once
     the broader feature list is complete.
     Player-model walking limbs now use the classic humanoid `0.6662` limb
     phase instead of the faster CraftZero `1.5` walk multiplier, with focused
     model-pose coverage for mirrored legs and arms. Player hand-swing
     animation now uses a single old six-tick cadence for empty hands and held
     items, and swing restart is gated at the old half-swing point instead of a
     fixed 200ms item-feel cooldown, with before-half and after-half restart
     coverage. Accepted bed sleep now exposes a sleeping player state, moves
     the camera to the bed-height sleep pose, hides the first-person hand, and
     renders the third-person player and armor flat along the bed facing during
     the transition. Villagers now use a dedicated Release-era model type with
     the tall head, protruding nose, robe body, folded arms, and profession
     texture path instead of the generic humanoid renderer. Mob spawners now
     render their configured mob preview in the cage using the shared mob
     renderer, with large mobs scaled down by source-style maximum dimension.
     Endermen now render carried blocks with a raised-arm carrying pose and a
     terrain-atlas block mesh using the carried block metadata.
     Entity and camera render angles now sanitize the shared interpolation path:
     yaw is finite-normalized, pitch is finite-clamped, and remote entity poses
     plus camera look targets route through the same guards before model/view
     matrices are built.
     End crystals now render as a 3D crystal/base mesh driven by their
     Release-style inner rotation and sinusoidal bob animation instead of a
     flat billboard.
     Ghast and Blaze fireballs now render from the terrain-atlas fire tile
     instead of borrowing the Blaze-powder item sprite, so hostile fireballs
     visually read as fire in-world.
     Boats now render as a five-part hull mesh using the bundled boat texture
     instead of the old flat item billboard in the projectile/entity renderer.
     Saddled pigs now render the old separate saddle texture pass on an
     inflated pig overlay model, so pig saddle state is visible while riding or
     inspecting the animal.
     Ignited creepers now swell with the old horizontal/vertical pulse and
     blink white on alternating fuse bands, so the pre-explosion warning is
     visible in-world instead of being only a simple uniform scale change.
     Minecarts now render through the same entity renderer as an open tub mesh
     rather than a flat item billboard; chest minecarts show a chest payload,
     and furnace minecarts show a terrain-atlas furnace block that becomes lit
     while fueled.
     Redstone dust now renders as flat connected cutout arms with source-style
     power tint instead of a raised full-block slab, so line, corner, vertical
     step, and isolated dust layouts are visually readable.
     In-world levers now render as a separate base plus a narrow powered-state
     handle arm, so the redstone control no longer appears as the fat ray
     selection cuboid.
     Filled cauldrons now render the Release-style inset still-water surface
     for metadata levels 1..3 on the translucent mesh layer instead of keeping
     filled/drained cauldron state invisible.
     Piston bases and heads now use Release-style metadata-driven texture
     selection, including sticky moving piston heads rendering with the sticky
     face instead of the normal piston head face.
     First- and third-person held block items now use the canonical per-face
     terrain atlas UVs and block item metadata, so furnaces, logs, wool colors,
     slab variants, and similar held cubes show the same visible faces and
     variants as their placed block states instead of a repeated generic side
     tile. Dropped block item entities now use the same metadata-sensitive
     terrain UV source for their spinning world cubes. GUI/hotbar block icons
     were audited during this pass and already route through
     `ItemType.getTextureCoords(...)`, so they preserve block item metadata;
     remaining block-item visual parity work is exact vanilla block-item icon
     pose/lighting and any future specialized item model exceptions.
     Single and large in-world chests now render body, lid, and latch meshes
     with Release-style texture-box UV regions, so the bundled 64x64/128x64
     chest textures are no longer stretched wholesale over each part.
     Furnace GUIs now render the flame and cook-arrow progress by copying the
     same legacy `furnace.png` overlay regions as the old container screen,
     including the ember and idle-arrow edge pixels.
     Potion item stacks now draw metadata-based liquid colors and a splash
     marker across inventory, crafting/container, creative, cursor, and legacy
     slot rendering paths instead of using one indistinguishable bottle sprite.
     Held initialized maps now draw a compact parchment preview from persistent
     128x128 map colors with a 16-direction player marker, and map-copy
     crafting preserves initialized map metadata and item-damage identity on
     copied outputs. Copied maps now also share live world-backed explored
     colors by map id, while independently initialized maps allocate distinct
     ids. The shared map color store and stack item damage now persist through
     save/load. Newly sampled map pixels now decode format-marked Release-style
     color/shade bytes, sample the full represented block area for dominant
     terrain color, and apply source-shaped previous-pixel height relief plus
     water-depth/checker shading, while legacy raw bytes normalize before
     rendering. Held maps now use a large responsive parchment-framed full-map
     presentation with player marker and mirrored angled sleeve/hand grips
     behind the normal HUD instead of the older 32-sample preview. Held-map
     markers for same-dimension off-map positions now clamp to the visible map
     edge, while invalid or different-dimension marker state stays hidden.
     First-person held maps now render through a live 3D parchment texture,
     cached map mesh, 16-direction marker, slot/swing/bob motion, and two-arm
     grip pose. Remaining map rendering work is final source-capture tuning
     rather than missing map data, marker, or first-person presentation plumbing.
     Burning local players now get a first-person terrain-atlas fire overlay
     using mirrored lower-screen flame quads behind the normal HUD, so active
     player fire is visible instead of only changing health/timers.
     Enchanted item stacks now draw a metadata-based animated purple glint
     overlay across inventory/container/creative/cursor/legacy slot paths and
     the survival hotbar. That overlay now uses the bundled `misc/glint.png`
     asset with repeat wrapping, scissored icon bounds, source-style additive
     blend, and two angled scrolling passes instead of the earlier procedural
     color bands; remaining visual polish is source-capture tuning of exact
     scroll speed and blend strength.
     Enchanted stacks now also carry that visual parity into 3D item rendering:
     dropped item entities and first-/third-person held block or sprite items
     draw two additive animated `misc/glint.png` passes through the normal scene
     shader while sampling the base atlas as an alpha mask, so transparent item
     pixels do not glow as solid quads and enchanted dropped/held gear no
     longer loses its visual state outside inventory slots.
     Placed signs now render their stored tile-entity text onto standing and
     wall sign faces through the scene renderer, so sign writing is visible
     after the editor closes instead of existing only in saved tile data.
     Standing signs also render the source-sized board/post mesh through all
     16 metadata rotations, with text offset onto the board face to match the
     visible sign instead of the block center plane.
     Active rain/snow now also renders camera-centered precipitation curtains
     from the bundled environment textures, with biome/open-sky filtering
     shared with the weather simulation.
     Active Nether portal blocks now feed the transient particle renderer from
     nearby-player random display ticks, and portal particles render with the
     old purple shimmer tint instead of the generic white smoke tint.
     Shared visual-state inputs now also reject malformed values before they
     reach render matrices or particle quads: camera movement/look/FOV/aspect
     setters sanitize inputs before view/projection rebuilds, renderer fog,
     color, light, tint, brightness, and alpha uniforms clamp finite values,
     particle construction/network payloads normalize positions, motion, scale,
     lifetime, target coordinates, and encoded block/item data, and the world
     particle queue drops invalid events before they can render or be
     rebroadcast. The particle renderer clamps partial ticks and skips malformed
     positions/scales, preventing bad visual payloads from poisoning the
     Release-style particle and animation passes.
   - Add or wire Release-style sounds for block interaction, entity events,
      pistons, portals, damage, ambience, and UI clicks. Audio routing now
      covers ordinary player block placement/break material cues, note blocks,
      jukebox records/ejection, openables,
      redstone control clicks, pistons, bow shots, flint-and-steel ignition,
     chest UI open/close, managed menu button clicks, dropped-item pickup, XP
     orb pickup, XP level-up feedback, and fishing splash timing, plus
     TNT/creeper fuse priming, explosions, food/cake eat+burp,
     milk/potion drink, all Java 1.0 record sound ids, dispenser activation,
     and thrown-use cues for splash potions, ender pearls, Eyes of Ender, and
     fishing casts, Enderman teleport endpoint cues, Ghast charge/fireball
     cues, hostile zombie/skeleton/creeper/spider hurt/death cues, common
     zombie/skeleton/spider/cow/mooshroom/pig/sheep/chicken idle living cues,
     passive cow/mooshroom/pig/sheep/chicken hurt/death cues,
     Enderman idle/scream/stare/hit/death cues, wolf bark/growl/pant/whine/hurt/death cues,
     slime/magma-cube jump/squish/contact-hit cues,
     Blaze ambient/hit/death cues,
     Zombie Pigman ambient/hit/death/delayed-angry cues,
     Ghast ambient/hurt/death plus charge/fireball cues,
     Silverfish ambient/hit/kill cues, and explicit Release-era Squid silence,
     plus thunderstorm lightning, rain ambience cues, dark-cave
     `ambient.cave.cave` ambience cues, and active Nether portal
     `portal.portal` ambience cues, and
     the main loop now drains those events
     through a volume-scaled, listener-distance and orientation-aware
     dispatcher into an OpenAL OGG sink when matching raw resource-pack or
     classpath sound assets exist. The sound asset path now covers exact and
     one-folder-wrapped folder/zip/jar packs, `resources/...` legacy roots,
     `assets/minecraft/...` namespaced layouts, `sounds/`, `sound/`,
     `newsound/`, `sound3/`, record `records`/`streaming` layouts, numbered
     flat pools plus old nested mob folder aliases such as `mob/zombie/hurt1`,
     `mob/cow/say1`, `mob/skeleton/death1`, `mob/slime/big1`, and
     `mob/chickenplop.ogg`, numbered block
     material/explosion/glass/eat/thunder pools, with decoded OpenAL buffers cached as
     per-sound pools for variant playback. Audio cache lifetime now follows
     active resource-pack changes too: switching the active manager or selected
     pack bumps a resource revision, and the OpenAL sink stops old active
     sources, deletes decoded buffer caches, and clears missing-sound markers
     before resolving later cues against the new pack. Spatial playback
     now applies the Release-style linear distance fade and clamps event gain
     above full volume while still using loud-event volume to extend audible
     range. Player and living-entity fall damage now queues the old
     `damage.fallsmall`/`damage.fallbig` landing cues, while legacy
     `damage/hurtflesh*` player-hurt media and common numbered `random.*`
     gameplay cue pools resolve through the same resource-pack/classpath
     lookup. Sound events now normalize ids and clamp non-finite coordinates,
     volume, and pitch before entering the world queue; multiplayer sound
     rebroadcast, dispatcher playback, ambient music, and OpenAL playback all
     reuse that playable-event boundary. The dispatcher and sink now sanitize
     listener/source state, contain sink/decode failures, reject bad OpenAL
     buffers/sources, and avoid letting malformed packets or broken OGG files
     poison later sound playback. The resolver now also expands
     door/chest/fuse/level-up/consume, fire, rain, cave, portal, wolf-shake, flat
     piston, old `pigzombie`, singular/plural `enderman`, `note.bassattack`,
     Ender Dragon death, direct `.ogg`, root-level, and broader
     music/streaming/record naming variants without changing the gameplay sound ids.
     Installs without old OGG media now fall back to a deterministic procedural
     PCM bank after resource-pack and classpath lookup fail. The generated bank
     covers current Release-style gameplay ids, material dig/step pools,
     note-block instruments, hostile/passive mob cues, weather, cave ambience,
     portal,
     records, and scheduled background music with per-id/per-variant timbres,
     while real packs still take precedence and exact Release audio media
     remains an external resource-pack/copyright issue rather than a bundled
     repository asset.
     First-person bow draw, sword block, and food/drink use poses now
     have distinct held-item transforms. Held bow rendering now also advances
     through the old idle, pull_0, pull_1, and pull_2 sprite frames as draw
     progress crosses the Release-era thresholds. Skeleton ranged rendering now
     raises both thin arms into the bow-aim pose while the attack goal is
     active. Item-8 audio implementation is treated as covered for this pass;
     exact Release 1.0 audio files remain external media/resource-pack
     availability polish rather than a code feature gap.

8. Save/load and runtime state
   - Ensure chunks, tile entities, entities, inventories, player state,
     health/food/XP, weather/time, dimensions, scheduled block ticks, pending
     fluid/redstone updates, portals, moving pistons, minecarts, boats,
     paintings, projectiles, end crystals, and mob AI state persist correctly.
     Scheduled block ticks, moving piston runtime state, active projectile
     runtime state, dropped item motion/animation/pickup-delay/health state, falling
     blocks, Eyes of Ender, XP orbs, active fishing bobbers including hooked-target identity,
     spider jockey rider identity,
     player burn timers,
     furnace fractional tick progress, brewing stand fractional tick progress,
     enchanting table book animation state, and mob spawner fractional tick
     progress now round-trip with
     remaining/elapsed delay or equivalent motion/lifetime data. Scheduled tick
     queues now also discard stale queued entries on block state changes so new
     metadata-dependent delays, such as repeater delay settings, are not stolen
     by old queue entries. Primed TNT now preserves an explicit zero fuse value
     across reloads for the final live smoking tick, instead of treating zero as
     missing data and restoring a full fuse. Sheep, wolf, creeper, chicken,
     slime/magma cube, blaze, ghast attack charge/flight state, squid, Enderman,
     skeleton ranged-combat goal state, shared living-entity attack cooldown,
     Snow Golem snowball attack cooldown, provoked spider, entity-backed mob AI
     combat target state, passive panic goal state, hostile nearest-target goal
     timers, generic mob move targets, melee attack path/stuck counters, Ender
     Dragon flight/death state, player FoodStats exhaustion, and weather
     rain/thunder countdowns are also covered. Saved positive mob combat
     target references now have to resolve to another saved living entity
     instead of silently restoring as no target, and duplicate positive entity
     save ids are rejected before references can become ambiguous. Spider jockey
     rider references must resolve from a saved spider to a saved skeleton before
     the world is applied. Moving piston restore/settle is covered by the save/load
     regression, including unloaded power-query chunk preservation for restored
     extension heads. Remaining combat-goal persistence risk is now deciding
     whether any active path-node progress should be persisted beyond the
     restored target-and-recompute path; the old unused revenge-goal class is
     not part of a registered Release 1.0 behavior path after wolf and
     Zombie Pigman retaliation are handled by mob-specific logic.
     Save format v9 now persists total world time, day count, and moon phase
     state instead of only the wrapped day-clock value, so reloads restore the
     visible lunar cycle. Modified chunk files now use chunk codec v3 when
     possible, storing block ids, metadata, cached sky light, cached block
     light, and the column height map; older v1/v2 chunk payloads remain
     readable and fall back to runtime light rebuilds. Active dimension runtime
     now also writes `dimensions/<dimension>/runtime.json` sidecars for dropped
     items, tile entities, entities, moving pistons, and scheduled block ticks,
     and portal dimension switches reload those sidecars when revisiting a
     dimension instead of relying only on the active root `level.json` payload.
     The root save now also preserves a world-global filled-map color cache
     across dimension switches, so saving after entering the Nether or End no
     longer wipes map colors owned by the previous `World` instance. Portal
     dimension switches now create the next runtime world with
     `WorldGenerators.generatorIdFor(target.dimension())`, making the target
     dimension generator explicit instead of relying on the default Release
     generator path to infer it. Dimension runtime data is now also kept in a
     save-manager cache for all known dimensions, merged with the active
     `level.json` runtime on each save, written back for every cached
     dimension, and used when restoring revisited dimensions instead of only
     depending on the currently active sidecar.
   - Supported-format `level.json` files that are syntactically valid but
     missing critical player or inventory payloads now load the valid backup
     when available, and otherwise report corruption instead of silently
     creating default player/inventory state. Present runtime-state lists with
     null entries are also treated as corrupt and recover from backup when
     possible. Unknown saved tile/entity identifiers are rejected before apply
     instead of silently dropping runtime objects. Malformed non-null saved
     item payloads are also rejected before restore. Corrupt primary chunk
     files now recover from previous binary chunk backups when available.
     Restored jukebox tile records now reconcile their loaded block metadata to
     the old inserted-record state when needed. Saved mob age/love-mode
     combinations now also reject out-of-range or impossible breeding states
     before restore, and zero-health End crystal payloads are rejected instead
     of being upgraded into live crystals. Non-dragon mobs now follow the same
     rule: zero-health saved payloads are corrupt rather than resurrected at
     full health, while the persisted Ender Dragon death sequence remains
     allowed. World metadata saves now reject invalid time/weather state before
     restore can silently skip bad time or replace bad countdowns with defaults.
     Player saves now reject non-finite numeric state, out-of-range
     health/food/saturation/exhaustion/air values, and negative progression
     counters before restore can clamp them into plausible runtime state.
     Dropped items now reject expired-age and overfull-health saved
     payloads rather than restoring already-despawned or impossible-health
     world drops, and experience orbs likewise reject expired-age and
     zero/overfull-health saved payloads rather than being restored as
     live/default-or-impossible-health orbs. Initialized map item metadata now
     round-trips through the ordinary inventory stack payload, including
     persistent center/dimension/scale/player marker/color data.
     Save format compatibility now also writes a Release-style gzip NBT
     `level.dat` sidecar beside the full CraftZero `level.json` payload. The
     sidecar mirrors world header metadata through the old `Data` compound, and
     a lone `level.dat` can seed a new CraftZero save with matching world
     metadata and empty runtime/chunk state. Modified chunk block storage now
     also mirrors to Release-style `.mcr` region files and can be imported from
     those region files when the native CraftZero chunk file is missing or bad.
     Release chunk runtime NBT now imports/exports the common tile entities,
     inventories, dropped items, vehicles/projectiles, paintings, XP orbs, and
     implemented mob records that CraftZero can restore. Region `TileTicks`
     now map to the existing scheduled block tick save model, and Release item
     stack `tag` NBT preserves supported display names, enchantments, potion
     identities, map ids, and scalar CraftZero metadata during `.mcr`
     import/export. Release `data/map_*.dat` files now bridge shared filled-map
     colors and map viewport metadata, with `idcounts.dat` written for numeric
     map id allocation. Release `level.dat` player compounds now import/export
     the core player state and inventory arrays instead of seeding every old
     world with a default player, and those player stacks preserve the same
     rich item `tag` metadata as region inventory stacks. Standalone Release
     `players/*.dat` files now import/export through the same player payload.
     Supported native CraftZero `level.json` versions now run normalization and
     migration defaults before validation, including missing player/inventory
     structures and newly added world metadata. Release `session.lock` and
     `level.dat_old` are now written, and `level.dat_old` can recover a missing
     or corrupt primary Release level file.
      Remaining save compatibility work is obscure old-NBT edge metadata polish.

## Tests Run

- Compile-only for the item-4 cake/cauldron Release 1.0 interaction pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-4/item-9 locked-chest creative exposure parity
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-7/item-9 achievement-toast icon fade polish pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 menu item-icon visual polish pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 item/block icon visual polish pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 first-person sprite hand-follow polish
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-7 Snow Golem model proportion pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 first-person held-map parchment visual
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-7/item-9 3D Anaglyph color-correction pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 enchanted glint visual parity pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 Statistics screen layout polish pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 first-person sprite hand anchor pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 first-person sprite hand walk-bob pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 first-person sprite hand swing pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 first-person sprite active-use hand pose pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 first-person sprite hand use-jolt pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 first-person consumable held-state pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 first-person sprite hand equip-timing
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 first-person non-block hand presentation
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 first-person non-block item pose polish
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-2 Release standalone player sidecar fallback pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-2 empty item `tag` sidecar cleanup pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-2 empty CraftZero item-metadata sidecar cleanup
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-2 scalar legacy NBT export cleanup pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-2 stored-enchantment NBT preservation pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-2 save-presence/bootstrap consistency pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-2 Release `level.dat`/`players/*.dat` NBT import
  hardening pass, per request: `.\gradlew.bat compileJava --console=plain`
  (no tests run).
- Compile-only for the item-2 `server.properties` world-catalog parity pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-2 corrupt custom chunk to Release `.mcr` fallback
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-2 world-catalog backup metadata
  presence/freshness pass, per request: `.\gradlew.bat compileJava --console=plain`
  (no tests run).
- Compile-only for the item-2 world-catalog stale-JSON fallback pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-2 Release `level.dat` missing-json error-preserve
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-2 `level.json` to Release `level.dat` load fallback
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-2 Release `.mcr` time-only dimension-runtime
  import pass, per request: `.\gradlew.bat compileJava --console=plain` (no
  tests run).
- Compile-only for the item-2 Release `level.dat` world-catalog parity pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 first-person sprite item holding parity
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-7/item-9 underwater viewport overlay parity pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 Nether portal overlay parity pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 first-person vignette parity pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7/item-9 pumpkin helmet overlay parity pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 in-game chat HUD visual parity pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9/item-10 Tab player-list visual parity pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 terrain-loading overlay visual parity pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 status-effect HUD visual parity pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 achievement-toast visual parity pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 creative inventory visual parity pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 inventory player-preview visual parity pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 statistics menu visual polish pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 enchanting-table menu visual polish pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 first-person consume item pose-order pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 F3 debug overlay visual polish pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 visual menu control polish pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 inventory tooltip visual polish pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 stack-count visual polish pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 survival hotbar visual polish pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 XP HUD label polish pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 boss HUD visual polish pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 selected-item HUD overlay parity pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 death-screen button texture polish pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 sign-editor visual polish pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9 chat HUD visual polish pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-9/item-10 Tab player-list visual polish pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 first-person sprite item pose polish pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 first-person sprite swing-translate polish pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 dynamic clock/compass overlay pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 third-person held-item grip animation pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 third-person held-use player animation pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-7 first-person consumable animation pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 projectile direct-hit source/burn pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 End crystal attack-specialization pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 hosted XP-orb attraction/level-up feedback
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-5/item-10 hosted fishing-hook remote-player pull
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-5/item-10 Ender pearl impact/remote-owner damage
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-5/item-10 player-owned explosion combat pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 splash-potion combat ownership pass, per
  request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 hosted hostile-retaliation metadata pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 hosted wolf owner-follow/begging pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 hosted natural-spawning anchor/cap pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 hosted remote combat identity and
  wolf/Pigman anger-target pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 hosted passive animal temptation and
  idle look-at remote-player awareness pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 hosted Enderman remote-stare and
  target-retention pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 hosted Cave Spider remote-poison melee
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-5/item-10 Spider-family remote leap pursuit pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 Creeper, Slime/Magma Cube, and Ender
  Dragon remote combat pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5/item-10 Ghast and Blaze remote-target combat
  pass, per request: `.\gradlew.bat compileJava --console=plain` (no tests
  run).
- Compile-only for the item-5 hosted mob despawn authority pass, per request:
  `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-5 runtime natural-spawn candidate/support pass,
  per request: `.\gradlew.bat compileJava --console=plain` (no tests run).
- Compile-only for the item-4 fragile ground-cover replacement placement pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-3 redstone torch strong-power classification pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-3 redstone torch burnout recovery timing pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release entity relationship/fishing-hook bridge,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release player cursor-stack bridge, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release projectile NBT bridge, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release dropped-item/entity-age region bridge,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 player runtime timer/state Release bridge, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 generic entity ground/fall-state and End crystal
  Release region bridge, per request: `.\gradlew.bat compileJava` (no tests
  run).
- Compile-only for the item-2 Release player XP level/progress bridge, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release player sleep-state bridge, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 chunk-runtime sidecar merge pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 moving-piston Release region bridge, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 runtime-only chunk save snapshot pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release region runtime metadata pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 dimension runtime metadata save/load pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5/item-10 hosted mob-projectile shooter parity
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5/item-10 hosted fishing-hook attachment pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5/item-10 hosted Ender pearl owner parity pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5/item-10 hosted projectile owner-credit pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5/item-10 hosted projectile runtime snapshot pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5/item-10 End crystal hosted entity coverage pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5/item-10 non-living entity hosted snapshot pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5/item-10 special-mob hosted snapshot pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5/item-10 living-entity runtime network snapshot
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5/item-10 ageable-mob network snapshot pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 F3 debug overlay pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-8 cave ambience parity pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-7/item-9 first-person held-map pose/render pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 Statistics one-shot counter pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-4/item-9 brewing stand potion-slot parity pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2/item-4 Release furnace recipe-table parity pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 dimension-transfer inventory authority pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-8 audio asset-resolution parity pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-7 render-distance visual culling pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-6 explosion exposure/math parity pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5 mob AI/navigation/combat pursuit pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5 navigator-led chase/target hygiene pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5 XP-orb body-contact pickup pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-4 tile-entity lifecycle parity pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-3 transient redstone/mechanism runtime cleanup
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 protocol-server dimension snapshot hygiene pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 hosted remote-player dimension scoping pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 remote player replication/animation pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 real multiplayer input-state sync pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 deferred client block snapshot application pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 initial-sync completion barrier pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 streamed initial snapshot sync pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 Release-era binary-client kick/status response
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 extended legacy server-list status bridge pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 extended legacy status delimiter/fallback pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 inbound server client-action schema hardening
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 outbound server client-action schema hardening
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 multiplayer wire/legacy bridge hardening pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 source-order quick-move parity pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 statistics screen row-unification pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-8 audio event/sink/resolver hardening pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release `session.lock`/`level.dat_old` sidecar
  bridge, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-4 tile/block reconciliation pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-3 redstone dust network and minecart overlap
  collision pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 native `level.json` migration-before-validation
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release standalone `players/*.dat` bridge, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release `level.dat` rich player stack metadata
  bridge, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release `level.dat` player compound bridge, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release filled-map data bridge, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release `.mcr` item stack metadata/potion bridge,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release `.mcr` scheduled block tick bridge, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release `.mcr` runtime entity/tile NBT bridge,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-2 Release `.mcr` terrain region bridge, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-10 multiplayer snapshot validation/non-finite
  packet rejection pass, per request: `.\gradlew.bat compileJava` (no tests
  run).
- Compile-only for the item-7 render-angle sanitation pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-6 damage/explosion finite-math hardening pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-6 geometry/raycast finite-math hardening pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-6 shared entity motion sanitation pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5 shared combat-target resolver/natural pack
  local-ground pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5 mob AI navigation retargeting/open-set pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-4 tile-entity split-drop pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 factory language settings route pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-3 piston mobility table pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the save/load Release-style `level.dat` metadata bridge,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer dimension-stamped sync/authority pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 menu-factory video settings parity pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-8 expanded legacy sound alias resolver pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-8 fall/damage sound resolver pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-8 procedural missing-media fallback audio pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-7 render/particle finite-state hardening pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-7 entity particle emitter placement/density pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-7 3D enchanted held/dropped item glint pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-6 entity-ray selection pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-5 pending-entity spawn/collision visibility pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-4 deferred multi-block side-effect pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-4 immediate neighboring-fluid wakeup pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the detector-rail start-of-step wakeup pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the piston/player entity-motion parity pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the piston/minecart mechanism parity pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the save/load all-dimension runtime cache pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer ordinary block break/place authority pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer remote action inventory-cost authority pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer player-vs-player attack authority pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer remote hostile AI target/combat authority
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer remote projectile collision/effects authority
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer remote explosion/lightning damage authority
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer remote environmental damage authority pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer remote item/XP pickup authority pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer vehicle riding authority/sync pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer fishing rod authority/sync pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer ender-pearl teleport reconciliation pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer projectile/item-use authority pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer client entity-action authority pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer survival/progression player-state payload
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer lightning visual event sync pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer world-particle event sync pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer world-sound event sync pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer player-state armor/status payload pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer player-state held-item normalization pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer host-facade inventory rebroadcast pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer host-authoritative block rebroadcast pass,
  per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer tile-payload authority/sign update pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer tile-entity state payload pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer projectile/vehicle entity snapshot pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer death/respawn inventory sync pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer death/respawn state sync pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer pre-join timeout pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer admission-gated initial sync pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer roster/view reconciliation pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer graceful client-leave pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer client-authority API cleanup pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer client-action authority pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer chat identity pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer player-state validation pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer block-edit reach authority pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer block/inventory authority pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer disconnect delivery/teardown pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer client join-acceptance gate pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer joined-player count pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer host-authoritative session/entity pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer live access-control enforcement pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer player-list latency pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer in-game player-list pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer connected-roster pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer saved-server status ping pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer remote-command dispatch pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer admin admission pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer saved-server screen pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer live-dimension/snapshot-reset world-state
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer keep-alive/session-timeout pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer host world-metadata pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer session-validation pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer legacy server-list ping pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer player-state/lifecycle pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the save/load player movement-state pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the save/load active sleep-state pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the save/load player attachment pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer remote player/entity pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer screen/cursor drop authority pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer player death-drop authority pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer cached-slot death-drop authority pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer host-derived death-XP authority pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the multiplayer host-validated respawn pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-7/item-9 3D Anaglyph stereo pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 Achievements menu visual polish pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 active Language menu parity pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 active Controls menu polish pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 selection-list menu visual polish pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 Create World/text-field visual polish pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9/item-10 multiplayer menu visual polish pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-7/item-9 enchanting-table book animation polish
  pass, per request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 Statistics table visual polish pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 survival container keyboard-drop parity pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 configured container drop-key parity pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 inventory drag-splitting parity pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 title-screen visual polish pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 animated title-splash polish pass, per request:
  `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 enchanting-offer glyph row polish pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 creative inventory hotbar/drop parity pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- Compile-only for the item-9 active Controls reset-key parity pass, per
  request: `.\gradlew.bat compileJava` (no tests run).
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.world.WorldParticleTest.blockDestroyParticlesUseTexturedFragments" --tests "com.craftzero.world.WorldParticleTest.blockHitParticlesPreserveTextureData" --tests "com.craftzero.world.WorldParticleTest.blockHitParticlesUseNonFullRenderBounds" --tests "com.craftzero.main.PlayerMovementConstantsTest.sprintingPlayersEmitTileCrackParticles" --tests "com.craftzero.main.PlayerPlacementMetadataTest.successfulPlayerBlockBreakingEmitsMaterialSoundFeedback" --tests "com.craftzero.main.PlayerPlacementMetadataTest.survivalBlockHitEmitsTexturedCrackParticle"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.world.WorldParticleTest.blockDestroyParticlesUseTexturedFragments" --tests "com.craftzero.world.WorldParticleTest.blockHitParticlesPreserveTextureData" --tests "com.craftzero.world.WorldParticleTest.blockHitParticlesUseNonFullRenderBounds" --tests "com.craftzero.world.WorldParticleTest.fragmentParticlesUseOldFallingShardMotion"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.main.PlayerMovementConstantsTest.walkingPlayersPlayMaterialFootstepsWithoutInventedParticles" --tests "com.craftzero.main.PlayerMovementConstantsTest.sprintingPlayersEmitTileCrackParticles" --tests "com.craftzero.main.PlayerMovementConstantsTest.sprintingDoesNotEmitTileCrackParticlesUnderwater" --tests "com.craftzero.main.PlayerMovementConstantsTest.playerEnteringWaterEmitsSplashAndBubbleParticles" --tests "com.craftzero.main.PlayerMovementConstantsTest.steadySwimmingDoesNotEmitInventedMovementBubbles" --tests "com.craftzero.world.WorldParticleTest.blockHitParticlesPreserveTextureData"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest.splashPotionSpellParticlesUseOldRadialCloudShape" --tests "com.craftzero.entity.SplashPotionEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.MechanismSprintTest.dispenserSuccessfulActivationEmitsDirectionalSmokePuff" --tests "com.craftzero.world.MechanismSprintTest.emptyDispenserActivationDoesNotEmitSmokePuff" --tests "com.craftzero.world.WorldParticleTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.EyeOfEnderEntityTest" --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.world.tile.MonsterSpawnerTileEntityTest.successfulSpawnerSpawnEmitsBurstParticles"`
- `.\gradlew.bat test --tests "com.craftzero.world.tile.MonsterSpawnerTileEntityTest.successfulSpawnerSpawnEmitsBurstParticles" --tests "com.craftzero.world.tile.MonsterSpawnerTileEntityTest.activeSpawnerEmitsSmokeAndFlameParticles" --tests "com.craftzero.world.WorldParticleTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.entity.SplashPotionEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.world.MechanismSprintTest.redstoneOreActivationEmitsExposedFaceSparkles"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ArrowEntityTest.criticalArrowsEmitTrailParticles" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.FluidSimulationTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.FurnaceMinecartEntityTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.MobBreedingTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.WolfVisualStateTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.BucketInteractionTest" --tests "com.craftzero.main.PlayerBucketInteractionTest" --tests "com.craftzero.world.WorldParticleTest.ambientFireEmitsLargeSmokeParticles"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerConsumableUseSoundTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.EntityWaterPhysicsTest" --tests "com.craftzero.world.WorldParticleTest.waterEntryParticlesEmitSplashAndBubbleBursts" --tests "com.craftzero.entity.ArrowEntityTest.underwaterArrowsEmitBubbleTrailsAndUseWaterDrag" --tests "com.craftzero.entity.ArrowEntityTest.arrowsEmitWaterEntryBurstAfterDryToWetTransition"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ThrownItemEntityTest" --tests "com.craftzero.entity.FireballEntityTest" --tests "com.craftzero.entity.EyeOfEnderEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerMovementConstantsTest.sprintingPlayersEmitTileCrackParticles" --tests "com.craftzero.main.PlayerMovementConstantsTest.sprintingDoesNotEmitTileCrackParticlesUnderwater" --tests "com.craftzero.main.PlayerMovementConstantsTest.walkingPlayersEmitMaterialFootsteps"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest.ambientFireEmitsLargeSmokeParticles" --tests "com.craftzero.world.WorldParticleTest.sideSupportedFireEmitsEdgeLargeSmokeParticles"`
- `.\gradlew.bat test --tests "com.craftzero.world.MechanismSprintTest.redstoneOreActivationEmitsExposedFaceSparkles" --tests "com.craftzero.world.MechanismSprintTest.redstoneOreSparklesRespectCoveredFaces" --tests "com.craftzero.world.MechanismSprintTest.redstoneOreGlowsWhenTouchedAndFades"`
- `.\gradlew.bat test --tests "com.craftzero.entity.EntityWaterPhysicsTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.FishingHookEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.EndProgressionTest.eyeInsertionEmitsSmokeBurstAboveFrame" --tests "com.craftzero.world.EndProgressionTest.completeEndPortalActivates"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest.poweredRepeatersEmitOldRedDustDisplayParticles" --tests "com.craftzero.world.WorldParticleTest.poweredRepeaterDisplayParticlesFollowDelayTorchOffset" --tests "com.craftzero.world.EndProgressionTest.eyeInsertionEmitsSmokeBurstAboveFrame"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.BlazeTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.FireballEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ArrowEntityTest" --tests "com.craftzero.entity.ThrownItemEntityTest" --tests "com.craftzero.entity.FireballEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.FireballEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldSoundEventTest.explosionsEmitSoundAndLargeParticle" --tests "com.craftzero.world.WorldSoundEventTest.smallExplosionsUseLargeExplosionCenterParticle" --tests "com.craftzero.world.BedInteractionTest.bedsExplodeOutsideOverworld" --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.entity.FireballEntityTest.explosiveFireballBlockImpactUsesFlamingExplosion"`
- `.\gradlew.bat test --tests "com.craftzero.entity.FireballEntityTest.explosiveFireballBlockImpactUsesFlamingExplosion" --tests "com.craftzero.entity.FireballEntityTest.directFireballContactDestroysEndCrystal" --tests "com.craftzero.world.MechanismSprintTest.endCrystalExplodesImmediatelyWithReleaseOneExplosionPower" --tests "com.craftzero.world.MechanismSprintTest.endCrystalExplosionDestroysNearbyCrystalsWithoutRecursiveBlast" --tests "com.craftzero.world.EndProgressionTest.explosionDestroyedHealingCrystalDamagesDragonWithoutRecursiveBlast"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerCombatTest.criticalPlayerHitsEmitCritParticles" --tests "com.craftzero.main.PlayerCombatTest.damageEnchantedPlayerHitsEmitMagicCritParticles" --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.EyeOfEnderEntityTest" --tests "com.craftzero.main.PlayerCombatTest.criticalPlayerHitsEmitCritParticles" --tests "com.craftzero.main.PlayerCombatTest.damageEnchantedPlayerHitsEmitMagicCritParticles" --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.world.WorldWeatherTest.rainEmitsPrecipitationParticlesAndAmbience" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.graphics.PrecipitationRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.world.tile.FurnaceTileEntityTest.litFurnaceEmitsFacingSmokeAndFlame" --tests "com.craftzero.world.tile.MonsterSpawnerTileEntityTest.activeSpawnerEmitsSmokeAndFlameParticles" --tests "com.craftzero.world.tile.MonsterSpawnerTileEntityTest.successfulSpawnEmitsSmokeAndFlameBurst" --tests "com.craftzero.entity.SplashPotionEntityTest" --tests "com.craftzero.entity.mob.BlazeTest"`
- Attempted `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.world.tile.FurnaceTileEntityTest.litFurnaceEmitsFacingSmokeAndFlame" --tests "com.craftzero.world.tile.MonsterSpawnerTileEntityTest" --tests "com.craftzero.entity.SplashPotionEntityTest" --tests "com.craftzero.entity.mob.BlazeTest"`; the broad `MonsterSpawnerTileEntityTest` class still fails at `hostileSpawnerRejectsLitSpawnPositions` on an existing hostile-spawn light assertion, while the particle-focused spawner methods pass in the focused run above.
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.entity.mob.SlimeTest" --tests "com.craftzero.entity.mob.MagmaCubeTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.entity.ThrownItemEntityTest.thrownItemsEmitSnowballPoofParticlesOnImpact" --tests "com.craftzero.entity.SplashPotionEntityTest" --tests "com.craftzero.main.PlayerInventoryStackTest.depletedDurableItemsEmitItemCrackParticles"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.entity.ThrownItemEntityTest.thrownItemsEmitSnowballPoofParticlesOnImpact"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.progression.ProgressionSystemsTest.livingEntitiesEmitStatusEffectParticles"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.world.TileEntityTest.noteBlockUsesVanillaPitchAndInstrumentRules" --tests "com.craftzero.world.TileEntityTest.punchedNoteBlockPlaysWithoutCyclingPitch" --tests "com.craftzero.entity.mob.MobBreedingTest" --tests "com.craftzero.entity.mob.WolfVisualStateTest.tameAttemptsSpawnWorldParticles"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.world.NetherPortalInteractionTest.activePortalsEmitAmbientSoundAndParticles" --tests "com.craftzero.world.DragonEggInteractionTest" --tests "com.craftzero.entity.EnderPearlEntityTest" --tests "com.craftzero.entity.EyeOfEnderEntityTest" --tests "com.craftzero.entity.mob.EndermanTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.world.TileEntityTest.enchantingTablesEmitBookshelfEnchantmentTableParticlesWhenPowered" --tests "com.craftzero.world.DroppedItemMergeTest.droppedItemPickupTransfersPartialStacksAndLeavesRemainder"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.main.PlayerCombatTest.criticalPlayerHitsEmitCritParticles" --tests "com.craftzero.main.PlayerCombatTest.damageEnchantedPlayerHitsEmitMagicCritParticles" --tests "com.craftzero.main.PlayerCombatTest.fullyDrawnReleaseOneBowsShouldCreateCriticalArrows" --tests "com.craftzero.entity.ArrowEntityTest.criticalArrowsEmitTrailParticles"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.world.tile.FurnaceTileEntityTest.litFurnaceEmitsFacingSmokeAndFlame"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.main.PlayerMovementConstantsTest.playerEnteringWaterEmitsSplashAndBubbleParticles"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.entity.mob.MobSoundTest.commonMobsEmitDeathPoofParticles" --tests "com.craftzero.world.SnowGolemInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.world.NetherPortalInteractionTest.activePortalsEmitAmbientSoundAndParticles"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.entity.SplashPotionEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.world.TileEntityTest.enchantingTablesEmitBookshelfEnchantmentTableParticlesWhenPowered"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.world.SnowGolemInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.entity.mob.SlimeTest" --tests "com.craftzero.entity.mob.MagmaCubeTest" --tests "com.craftzero.entity.mob.MobSoundTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerMooshroomInteractionTest" --tests "com.craftzero.world.WorldSoundEventTest.explosionsEmitSoundAndLargeParticle" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ThrownItemEntityTest" --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.entity.SplashPotionEntityTest" --tests "com.craftzero.graphics.PotionItemVisualsTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.SplashPotionEntityTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.graphics.PotionItemVisualsTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest" --tests "com.craftzero.main.PlayerMovementConstantsTest.walkingPlayersEmitMaterialFootsteps"`
- Reran after depth-suspend work: `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerCombatTest.damageEnchantedPlayerHitsEmitMagicCritParticles" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.world.DroppedItemMergeTest.droppedItemPickupTransfersPartialStacksAndLeavesRemainder" --tests "com.craftzero.entity.mob.SlimeTest" --tests "com.craftzero.entity.mob.MagmaCubeTest" --tests "com.craftzero.world.TileEntityTest.enchantingTableCreatesBookAnimationTileEntity" --tests "com.craftzero.world.TileEntityTest.enchantingTablesEmitBookshelfGlyphParticlesWhenPowered" --tests "com.craftzero.graphics.ParticleRendererTest"`
- Attempted `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.world.DroppedItemMergeTest" --tests "com.craftzero.entity.mob.SlimeTest" --tests "com.craftzero.entity.mob.MagmaCubeTest" --tests "com.craftzero.world.TileEntityTest" --tests "com.craftzero.graphics.ParticleRendererTest"`; the broad `TileEntityTest` class still fails at `lightEmittersProduceBlockLight` on the existing torch/lit-furnace block-light assertion, while the new particle/enchanting tile tests pass in the focused run above.
- `.\gradlew.bat test --tests "com.craftzero.graphics.ParticleRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldParticleTest" --tests "com.craftzero.entity.ThrownItemEntityTest" --tests "com.craftzero.entity.SplashPotionEntityTest" --tests "com.craftzero.entity.mob.MobSoundTest" --tests "com.craftzero.main.PlayerInventoryStackTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.villagePlacementUsesReleaseOneGridAndBiomeGate" --tests "com.craftzero.world.StructureGeneratorTest.villageBiomeGateUsesGenerationLayer" --tests "com.craftzero.world.StructureGeneratorTest.locateVillageMatchesGeneratedStart" --tests "com.craftzero.world.StructureGeneratorTest.villagePathQueueAttachesImplementedPiecesWithoutTorchSpam" --tests "com.craftzero.world.StructureGeneratorTest.nonSizeableVillageStartsDoNotGenerateOrSuppressLakes"`
- `.\gradlew.bat test --tests "com.craftzero.ui.menu.MenuScreensTest" --tests "com.craftzero.ui.menu.MenuScreenFactoryTest.achievementAndStatisticsFactoryScreensUseLiveProgress" --tests "com.craftzero.main.PlayerStatisticsTest"`
- `.\gradlew.bat test --tests "com.craftzero.command.CommandDispatcherTest" --tests "com.craftzero.multiplayer.MultiplayerLoopbackTest.legacyFacadeBroadcastsAddressedCommandActions"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.VillagerTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ai.MobGoalRandomnessTest" --tests "com.craftzero.entity.ai.MobAttackTargetGoalTest" --tests "com.craftzero.entity.mob.VillagerTest" --tests "com.craftzero.entity.mob.MobDespawnTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ai.MobAttackTargetGoalTest" --tests "com.craftzero.entity.mob.EndermanTest" --tests "com.craftzero.entity.mob.GiantTest" --tests "com.craftzero.entity.mob.SkeletonTest" --tests "com.craftzero.entity.mob.WolfCombatBehaviorTest" --tests "com.craftzero.save.SaveManagerTest.mobCombatTargetIdentityRoundTrips"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ai.MobAttackTargetGoalTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ai.MobAttackTargetGoalTest" --tests "com.craftzero.entity.mob.EndermanTest" --tests "com.craftzero.entity.mob.GiantTest" --tests "com.craftzero.entity.mob.SkeletonTest" --tests "com.craftzero.entity.mob.WolfCombatBehaviorTest"`
- `.\gradlew.bat test --tests "com.craftzero.save.SaveManagerTest.mobCombatTargetIdentityRoundTrips"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerStatisticsTest" --tests "com.craftzero.main.PlayerInventoryStackTest" --tests "com.craftzero.ui.menu.MenuScreensTest.statisticsScreenSwitchesBetweenTypedPages" --tests "com.craftzero.ui.menu.MenuScreenFactoryTest.achievementAndStatisticsFactoryScreensUseLiveProgress" --tests "com.craftzero.save.SaveManagerTest.saveManagerRoundTripsWorldState"`
- `.\gradlew.bat test --tests "com.craftzero.ui.menu.MenuScreensTest.statisticsScreenListsPlayerCounters"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerStatisticsTest" --tests "com.craftzero.ui.menu.MenuScreensTest.statisticsScreenListsPlayerCounters" --tests "com.craftzero.ui.menu.MenuScreenFactoryTest.achievementAndStatisticsFactoryScreensUseLiveProgress" --tests "com.craftzero.save.SaveManagerTest.saveManagerRoundTripsWorldState" --tests "com.craftzero.main.MainGameplayScreenCloseTest.savingUnloadRecordsGamesQuitStatistic"`
- `.\gradlew.bat test --tests "com.craftzero.ui.menu.MenuScreensTest.endCreditsScreenPresentsCompletionFlow" --tests "com.craftzero.world.DimensionTransferServiceTest.endPortalUsesReleaseOneEntryTarget" --tests "com.craftzero.main.MainNetherPortalTransferTest"`
- `.\gradlew.bat test --tests "com.craftzero.ui.menu.MenuScreensTest"`
- `.\gradlew.bat test --tests "com.craftzero.ui.menu.MenuScreenFactoryTest" --tests "com.craftzero.graphics.DeathScreenTest" --tests "com.craftzero.main.MainNetherPortalTransferTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdPortalRoomsObeySourceDepthGate" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollPortalRooms" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdWeightedExpansionStopsAfterLimitedRoomsExhausted"`
- `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.locateStrongholdMatchesGeneratedPortalRoom" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdMainPathUsesSourceChildAccessesForEveryRootOrientation" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesExpandIntoWeightedRooms" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollCrossingHalls" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollStairwellPieces"`
- `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.strongholdCorridorUsesSourceOpenEndedTubeLayout"`
- `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.strongholdStonesUseReleaseOneVariantMetadata" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdCorridorsUseSourceTubeDimensions"`
- `.\gradlew.bat test --tests "com.craftzero.world.EndProgressionTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.PlacementSupportTest.doorsRequireReleaseOneNormalTopSupport" --tests "com.craftzero.world.PlacementSupportTest.doorsBreakWhenNormalTopSupportBecomesInvalid" --tests "com.craftzero.world.PlacementSupportTest.trapdoorsUseReleaseOneSideAnchors" --tests "com.craftzero.world.BlockStateShapeTest.doorMetadataControlsSourceShape"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerStatisticsTest" --tests "com.craftzero.main.PlayerInventoryStackTest" --tests "com.craftzero.ui.menu.MenuScreensTest.statisticsScreenListsPlayerCounters" --tests "com.craftzero.ui.menu.MenuScreenFactoryTest.achievementAndStatisticsFactoryScreensUseLiveProgress" --tests "com.craftzero.save.SaveManagerTest.saveManagerRoundTripsWorldState"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneTaigaDecorationGeneratesFernMetadata"`
- Attempted `.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneOverworldDecoratesSurfaceDetails" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneTaigaDecorationGeneratesFernMetadata"`; the combined decorator scan exceeded the 120-second command timeout, and Gradle was stopped cleanly afterward. The new taiga metadata test passed separately.
- `.\gradlew.bat test --tests "com.craftzero.graphics.SurvivalHudRendererTest" --tests "com.craftzero.world.FireInteractionTest.rainExtinguishesExposedBurningPlayers"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.MobDaylightBurnTest" --tests "com.craftzero.world.FireInteractionTest.rainExtinguishesExposedBurningPlayers" --tests "com.craftzero.world.FireInteractionTest.playerBurningTicksDealPeriodicDamage"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.SkeletonTest" --tests "com.craftzero.entity.ArrowEntityTest.fireArrowsIgnitePlayers" --tests "com.craftzero.world.FireInteractionTest.playerTouchingFireTakesDamage" --tests "com.craftzero.world.FireInteractionTest.playerBurningTicksDealPeriodicDamage" --tests "com.craftzero.save.SaveManagerTest.saveManagerRoundTripsWorldState"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ArrowEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.FireInteractionTest.playerTouchingFireTakesDamage" --tests "com.craftzero.world.FireInteractionTest.playerBurningTicksDealPeriodicDamage"`
- Attempted `.\gradlew.bat test --tests "com.craftzero.world.FireInteractionTest"`; it exceeded the 180-second command timeout and the leftover Gradle test worker was stopped. The two fire tests touched by this pass passed separately.
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerStatisticsTest" --tests "com.craftzero.main.PlayerCombatTest.playerMobKillsUpdateStatisticsFromMobDeath" --tests "com.craftzero.entity.ArrowEntityTest.playerOwnedArrowUnlocksSniperDuelAfterLongSkeletonKill" --tests "com.craftzero.ui.menu.MenuScreensTest.statisticsScreenListsPlayerCounters" --tests "com.craftzero.save.SaveManagerTest.*round*"`
- `.\gradlew.bat test --tests "com.craftzero.world.MechanismSprintTest.repeaterAcceptsPoweredRearDustWithSideBranch" --tests "com.craftzero.world.MechanismSprintTest.repeaterAcceptsWeakPoweredOpaqueInputBlock" --tests "com.craftzero.world.MechanismSprintTest.connectedRedstoneWireUsesDirectionalWeakPower" --tests "com.craftzero.world.MechanismSprintTest.cornerRedstoneWireDoesNotWeakPowerPerpendicularBranches" --tests "com.craftzero.world.MechanismSprintTest.repeaterCyclesAndUsesConfiguredDelay"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.SurvivalHudRendererTest" --tests "com.craftzero.inventory.MapItemDataTest.differentDimensionDoesNotRepaintMap" --tests "com.craftzero.main.PlayerMapInteractionTest"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.world.WorldSoundEventTest.explosionsEmitSoundAndLargeParticle" --tests "com.craftzero.world.BedInteractionTest.bedsExplodeOutsideOverworld"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldSoundEventTest" --tests "com.craftzero.world.BedInteractionTest" --tests "com.craftzero.entity.FireballEntityTest.explosiveFireballsDetonateOnDirectMinecartImpact" --tests "com.craftzero.entity.FireballEntityTest.explosiveFireballBlockImpactUsesFlamingExplosion" --tests "com.craftzero.world.MechanismSprintTest.endCrystalExplodesImmediatelyWithReleaseOneExplosionPower" --tests "com.craftzero.world.MechanismSprintTest.explosionBlockDropsUseInverseBlastPowerChance"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.world.MechanismSprintTest.endCrystalExplosionDestroysNearbyCrystalsWithoutRecursiveBlast" --tests "com.craftzero.world.EndProgressionTest.explosionDestroyedHealingCrystalDamagesDragonWithoutRecursiveBlast" --tests "com.craftzero.world.EndProgressionTest.destroyingActiveHealingCrystalDamagesDragon" --tests "com.craftzero.entity.ArrowEntityTest.arrowsDestroyEndCrystalsOnDirectContact" --tests "com.craftzero.entity.ThrownItemEntityTest.thrownItemsDestroyEndCrystalsWithZeroDamageImpact"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.progression.ProgressionSystemsTest" --tests "com.craftzero.main.PlayerCombatTest" --tests "com.craftzero.entity.ArrowEntityTest" --tests "com.craftzero.main.PlayerMinecartInteractionTest" --tests "com.craftzero.main.PlayerPigInteractionTest"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.progression.ProgressionSystemsTest.releaseOneCombatAndBrewingAchievementsKeepParentGates" --tests "com.craftzero.progression.ProgressionSystemsTest.releaseOneRailAndPigAchievementsKeepParentGates" --tests "com.craftzero.main.PlayerCombatTest.monsterHunterUnlocksFromPlayerMonsterKill" --tests "com.craftzero.entity.ArrowEntityTest.playerOwnedArrowUnlocksSniperDuelAfterLongSkeletonKill" --tests "com.craftzero.main.PlayerMinecartInteractionTest.onARailUnlocksFromLongMountedMinecartTrip" --tests "com.craftzero.main.PlayerPigInteractionTest.whenPigsFlyUnlocksWhenRiddenPigTakesFallDamage"`
- `.\gradlew.bat test --tests "com.craftzero.progression.ProgressionSystemsTest" --tests "com.craftzero.main.MainNetherPortalTransferTest"`
- `.\gradlew.bat test --tests "com.craftzero.progression.ProgressionSystemsTest" --tests "com.craftzero.ui.InventoryScreenTest" --tests "com.craftzero.ui.CraftingTableScreenTest" --tests "com.craftzero.save.SaveManagerTest.saveManagerRoundTripsWorldState" --tests "com.craftzero.graphics.SurvivalHudRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.SlimeTest" --tests "com.craftzero.entity.mob.MagmaCubeTest"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.DroppedItemRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.PlayerRendererFirstPersonPoseTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.IceInteractionTest" --tests "com.craftzero.world.SnowLayerInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.BlockDropResolverTest"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerCombatTest"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerCombatTest.swordBlockingReducesBlockableDamageBeforeArmor" --tests "com.craftzero.main.PlayerCombatTest.swordBlockingDoesNotReduceUnblockableFallDamage"`
- `.\gradlew.bat test --tests com.craftzero.graphics.CloudRendererTest --tests com.craftzero.graphics.DimensionRenderEnvironmentTest`
- `.\gradlew.bat test --tests com.craftzero.graphics.SkyRendererTest --tests com.craftzero.graphics.DimensionRenderEnvironmentTest`
- `.\gradlew.bat test --tests com.craftzero.graphics.DimensionRenderEnvironmentTest`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerMovementConstantsTest" --tests "com.craftzero.entity.EntityCollisionTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.MechanismSprintTest.connectedRedstoneWireUsesDirectionalWeakPower" --tests "com.craftzero.world.MechanismSprintTest.cornerRedstoneWireDoesNotWeakPowerPerpendicularBranches" --tests "com.craftzero.world.MechanismSprintTest.verticalRedstoneWirePropagationRespectsCeiling" --tests "com.craftzero.world.MechanismSprintTest.glowstoneRedstoneSupportCarriesPowerUpwardOnly" --tests "com.craftzero.world.MechanismSprintTest.repeaterCyclesAndUsesConfiguredDelay" --tests "com.craftzero.world.MechanismSprintTest.pistonUsesQuasiConnectivity" --tests "com.craftzero.world.ChunkMeshBuilderTest.redstoneWireRenderConnectionsFollowReleaseStepRules"`
- `.\gradlew.bat test --tests "com.craftzero.world.MechanismSprintTest.flamingExplosionFiresRequireOpaqueSupport" --tests "com.craftzero.world.MechanismSprintTest.explosionBlockDropsUseInverseBlastPowerChance" --tests "com.craftzero.entity.FireballEntityTest.explosiveFireballBlockImpactUsesFlamingExplosion" --tests "com.craftzero.entity.FireballEntityTest.explosiveFireballBlockImpactDoesNotPlaceAdjacentFire" --tests "com.craftzero.entity.FireballEntityTest.explosiveFireballsDetonateOnDirectMinecartImpact"`
- `.\gradlew.bat test --tests com.craftzero.entity.mob.SheepTest --tests com.craftzero.main.PlayerSheepInteractionTest --tests com.craftzero.graphics.MobRendererTest`
- `.\gradlew.bat test --tests com.craftzero.progression.ProgressionSystemsTest --tests com.craftzero.graphics.ParticleRendererTest --tests com.craftzero.graphics.SurvivalHudRendererTest`
- `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdBranchFailuresCreateSourceFallbackCorridors" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdLibrariesObeySourceDepthGate" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesExpandIntoWeightedRooms" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollCrossingHalls" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollStairwellPieces"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.SurvivalHudRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.graphics.SignTextRendererTest" --tests "com.craftzero.world.BlockStateShapeTest.standingSignUsesSourceSelectionBounds" --tests "com.craftzero.world.TileEntityTest.signTextPersistsAndFiltersReleaseCharacters"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.ItemTextureResolverTest" --tests "com.craftzero.graphics.PlayerRendererFirstPersonPoseTest" --tests "com.craftzero.main.PlayerCombatTest.playerBowReleaseEmitsSound" --tests "com.craftzero.main.PlayerCombatTest.fullyDrawnBowsCreateCriticalArrows"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.MobRendererTest" --tests "com.craftzero.graphics.model.MobModelParityTest" --tests "com.craftzero.entity.mob.SkeletonTest"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.MobRendererTest" --tests "com.craftzero.entity.mob.CreeperTest" --tests "com.craftzero.world.WorldWeatherTest.lightningTransformsPigsAndChargesCreepers"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerPigInteractionTest.lightningTransformingRiddenPigDismountsPlayer" --tests "com.craftzero.world.WorldWeatherTest.lightningTransformsPigsAndChargesCreepers"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.PlayerRendererFirstPersonPoseTest" --tests "com.craftzero.graphics.ItemTextureResolverTest"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerAnimationTest" --tests "com.craftzero.main.PlayerCombatTest.playerBowReleaseEmitsSound" --tests "com.craftzero.main.PlayerCombatTest.fullyDrawnBowsCreateCriticalArrows" --tests "com.craftzero.main.PlayerConsumableUseSoundTest"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.MobRendererTest" --tests "com.craftzero.graphics.model.MobModelParityTest" --tests "com.craftzero.main.PlayerPigInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.BoatEntityTest" --tests "com.craftzero.entity.EntityWaterPhysicsTest" --tests "com.craftzero.world.DroppedItemMergeTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.EntityWaterPhysicsTest" --tests "com.craftzero.main.PlayerMovementConstantsTest" --tests "com.craftzero.world.FireInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.ui.EnchantingTableScreenTest" --tests "com.craftzero.progression.ProgressionSystemsTest.enchanting*" --tests "com.craftzero.progression.ProgressionSystemsTest.releaseOneExperienceCurveKeepsLevelFiftyCost"`
- `.\gradlew.bat test --tests "com.craftzero.world.BlockStateShapeTest.pistonMetadataControlsBaseAndHeadTextures" --tests "com.craftzero.world.BlockStateShapeTest.pistonMetadataControlsBaseAndHeadShapes" --tests "com.craftzero.world.MechanismSprintTest.stickyPistonHeadCarriesStickyMetadata" --tests "com.craftzero.world.MechanismSprintTest.pistonExtensionUsesMovingPistonBeforeHeadSettles"`
- `.\gradlew.bat test --tests "com.craftzero.ui.FurnaceScreenTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.mineshaftStairsUseSourceDescendingCarveSlices" --tests "com.craftzero.world.StructureGeneratorTest.mineshaftStairsAbortsWhenLiquidTouchesSourceEnvelope"`
- `.\gradlew.bat test --tests "com.craftzero.world.EndProgressionTest"`
- `.\gradlew.bat test --tests "com.craftzero.save.SaveManagerTest.enderDragonRuntimeStateRoundTrips"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.ArrowRendererFishingLineTest" --tests "com.craftzero.world.EndProgressionTest.endSpikeGenerationSpillsAcrossChunkBorders" --tests "com.craftzero.world.EndProgressionTest.endCrystalMaintainsFireOnlyInEndDimension"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldSoundEventTest" --tests "com.craftzero.main.PlayerPlacementMetadataTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.BlockStateShapeTest.openFenceGatePreservesRenderAxis" --tests "com.craftzero.world.BlockStateShapeTest.closedFenceGateUsesSourceCollisionStrip"`
- `.\gradlew.bat test --tests "com.craftzero.audio.WorldSoundDispatcherTest" --tests "com.craftzero.audio.SoundAssetResolverTest"`
- `.\gradlew.bat test --tests "com.craftzero.resources.ResourcePackManagerTest" --tests "com.craftzero.audio.SoundAssetResolverTest" --tests "com.craftzero.audio.WorldSoundDispatcherTest" --tests "com.craftzero.audio.AmbientMusicSchedulerTest"`
- `.\gradlew.bat test --tests "com.craftzero.main.MainBedSleepTest" --tests "com.craftzero.world.BedInteractionTest" --tests "com.craftzero.multiplayer.MultiplayerLoopbackTest"`
- `.\gradlew.bat test --tests "com.craftzero.multiplayer.MultiplayerLoopbackTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.BedInteractionTest" --tests "com.craftzero.main.MainBedSleepTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.SheepTest" --tests "com.craftzero.main.PlayerSheepInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.progression.ProgressionSystemsTest"`
- `.\gradlew.bat test --tests "com.craftzero.save.SaveManagerTest.expiredMobStatusEffectWithoutBackupIsCorrupt"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.MobPanicTest" --tests "com.craftzero.entity.mob.MobDaylightBurnTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.MobDespawnTest" --tests "com.craftzero.entity.mob.SquidWaterMovementTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.SquidWaterMovementTest" --tests "com.craftzero.save.SaveManagerTest.mobAiRuntimeStateRoundTrips"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.MobBreedingTest" --tests "com.craftzero.entity.mob.MobAgeDropTest" --tests "com.craftzero.main.PlayerAnimalBreedingInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.MobSpawnerTest.runtimeNaturalSpawningSkipsOuterBorderChunks" --tests "com.craftzero.world.MobSpawnerTest.runtimeNaturalSpawningSweepsEligibleChunks" --tests "com.craftzero.world.MobSpawnerTest.runtimeNaturalGroundSpawningUsesSelectedChunkRandomY" --tests "com.craftzero.world.MobSpawnerTest.runtimeNaturalSpawningRunsThreeGroupsPerEligibleChunk" --tests "com.craftzero.world.MobSpawnerTest.runtimePassiveSpawningDoesNotUseCategoryChanceGate" --tests "com.craftzero.world.MobSpawnerTest.runtimeWaterCreatureSpawningDoesNotUseCategoryChanceGate" --tests "com.craftzero.world.MobSpawnerTest.runtimeHostileCapScalesWithEligibleChunks" --tests "com.craftzero.world.MobSpawnerTest.runtimeWaterCreatureSpawningUsesSurfaceWaterCells" --tests "com.craftzero.world.MobSpawnerTest.naturalSpawnsRespectWorldSpawnExclusion"`
- `.\gradlew.bat test --tests "com.craftzero.world.MobSpawnerTest.waterCreatureSpawnsAcceptSurfaceWaterWithNonSolidHeadSpace" --tests "com.craftzero.world.MobSpawnerTest.runtimeWaterCreatureSpawningUsesSurfaceWaterCells" --tests "com.craftzero.world.MobSpawnerTest.naturalWaterCreatureSpawnsUseReleaseOnePackSizes" --tests "com.craftzero.world.MobSpawnerTest.runtimeWaterCreatureSpawningDoesNotUseCategoryChanceGate" --tests "com.craftzero.world.MobSpawnerTest.publicMobSpawnerUsesWorldRandom"`
- `.\gradlew.bat test --tests "com.craftzero.world.tile.FurnaceTileEntityTest.furnaceClearsStaleCookProgressWhenFuelSlotIsEmpty" --tests "com.craftzero.world.tile.FurnaceTileEntityTest.furnaceResetsCookProgressWhenFuelRunsOut" --tests "com.craftzero.world.tile.FurnaceTileEntityTest.furnaceResetsCookProgressWithInvalidFuelPresent" --tests "com.craftzero.world.tile.FurnaceTileEntityTest.furnaceClearsStaleCookProgressForInvalidInputStates" --tests "com.craftzero.world.tile.FurnaceTileEntityTest.lavaBucketFuelLeavesEmptyBucketInReleaseOne" --tests "com.craftzero.world.tile.FurnaceTileEntityTest.furnaceSmeltsAndTogglesLitBlock"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.world.tile.FurnaceTileEntityTest.litFurnaceEmitsFacingSmokeAndFlame" --tests "com.craftzero.world.tile.FurnaceTileEntityTest.litFurnaceAmbienceIsVisualOnly"`
- `.\gradlew.bat test --tests "com.craftzero.world.MechanismSprintTest.pistonBreaksSnowLayerIntoSnowball" --tests "com.craftzero.world.MechanismSprintTest.stickyPistonDoesNotPullSnowLayerOnRetraction" --tests "com.craftzero.world.MechanismSprintTest.pistonCrushesFragileBlockWhilePushing" --tests "com.craftzero.world.MechanismSprintTest.pistonBreaksPumpkinBlocksIntoDrops" --tests "com.craftzero.world.MechanismSprintTest.pistonBreaksMelonsIntoSlices" --tests "com.craftzero.world.MechanismSprintTest.stickyPistonPullsMovableBlockOnRetraction" --tests "com.craftzero.world.MechanismSprintTest.shortPulsedStickyPistonPullsSkippedBlockWhenFrontWasAir" --tests "com.craftzero.world.MechanismSprintTest.pistonMovesSupportedRails" --tests "com.craftzero.world.MechanismSprintTest.pistonPopsUnsupportedMovedRails"`
- `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.worldGenCreatureSpawningReadsDecoratorScratchState" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldGenCreatureSpawningUsesCollisionVolume" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationRunsCreatureSpawning" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationSpawnsWolvesInTaiga" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationSpawnsMooshroomsOnMushroomIslands" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkDungeonsBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkOresBeforeLateFeatures"`
- `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkDungeonsBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkOresBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.nullWorldOverworldGenerationPlacesDungeonBlocks" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDungeonRandomResumesAfterLakeBranches" --tests "com.craftzero.world.WorldGenerationParityTest.overworldOreRandomResumesAfterLakeAndDungeonPhases" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDecoratorRandomResumesAfterOreHelper" --tests "com.craftzero.world.DungeonFeatureTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkStructuresBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.sourceTreeScratchOverlaysOffChunkLakesBeforeTrees" --tests "com.craftzero.world.OverworldGenerationSprintTest.dungeonValidationReadsOffChunkLakeScratchState" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkDungeonsBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkOresBeforeLateFeatures" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDungeonRandomResumesAfterLakeBranches" --tests "com.craftzero.world.WorldGenerationParityTest.overworldOreRandomResumesAfterLakeAndDungeonPhases" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDecoratorRandomResumesAfterOreHelper"`
- `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.nullWorldOverworldGenerationPlacesDungeonBlocks" --tests "com.craftzero.world.OverworldGenerationSprintTest.overworldPopulationCarvesLakes" --tests "com.craftzero.world.StructureGeneratorTest.strongholdPlacementUsesReleaseOneBiomeReservoirSearch" --tests "com.craftzero.world.StructureGeneratorTest.strongholdStonesUseReleaseOneVariantMetadata"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.world.StructureGeneratorTest.strongholdPiecesAbortWhenLiquidTouchesSourceEnvelope" --tests "com.craftzero.world.StructureGeneratorTest.strongholdPortalRoomIgnoresLiquidEnvelopeAbort" --tests "com.craftzero.world.StructureGeneratorTest.strongholdPortalRoomUsesSourceChamberBlocks"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.entity.mob.MobSoundTest.commonPassiveAnimalsEmitHurtSounds" --tests "com.craftzero.entity.mob.MobSoundTest.commonPassiveAnimalsEmitDeathSounds" --tests "com.craftzero.entity.mob.MobSoundTest.commonMobsEmitAmbientIdleSounds" --tests "com.craftzero.entity.mob.MobSoundTest.squidRemainSilentForReleaseOne"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.world.MobSpawnerTest.naturalSheepSpawnsUseReleaseOneWeightedFleeceColors" --tests "com.craftzero.world.MobSpawnerTest.naturalSheepSpawnsUseReleaseOneRarePinkFleeceRoll" --tests "com.craftzero.entity.mob.SheepTest" --tests "com.craftzero.world.MobSpawnerTest.naturalPassiveSpawnsUseReleaseOnePackSizes"`
- `.\gradlew.bat test --tests "com.craftzero.world.WorldLightningBoltTest" --tests "com.craftzero.world.WorldWeatherTest"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.PrecipitationRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.MobSpawnerTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.EnderPearlEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerEnderPearlInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ThrownItemEntityTest" --tests "com.craftzero.main.PlayerEnderPearlInteractionTest" --tests "com.craftzero.entity.mob.BlazeTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.BoatEntityTest" --tests "com.craftzero.main.PlayerBoatInteractionTest" --tests "com.craftzero.world.BoatInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.BoatEntityTest" --tests "com.craftzero.world.BoatInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.FishingHookEntityTest" --tests "com.craftzero.main.PlayerFishingRodInteractionTest" --tests "com.craftzero.save.SaveManagerTest.fishingBobberStateRoundTrips" --tests "com.craftzero.save.SaveManagerTest.fishingHookedTargetRoundTrips"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.SilverfishTest" --tests "com.craftzero.world.StoneBrickMetadataInteractionTest" --tests "com.craftzero.entity.mob.MobSoundTest.silverfishEmitReleaseSounds"`
- `.\gradlew.bat test --tests "com.craftzero.inventory.MapItemDataTest" --tests "com.craftzero.main.PlayerMapInteractionTest" --tests "com.craftzero.save.MapItemSaveTest"`
- `.\gradlew.bat test --tests "com.craftzero.inventory.MapItemDataTest" --tests "com.craftzero.crafting.CraftingRegistryTest"`
- `.\gradlew.bat test --tests "com.craftzero.inventory.MapItemDataTest"`
- `.\gradlew.bat test --tests "com.craftzero.inventory.MapItemDataTest" --tests "com.craftzero.crafting.CraftingRegistryTest" --tests "com.craftzero.main.PlayerMapInteractionTest" --tests "com.craftzero.save.MapItemSaveTest"`
- `.\gradlew.bat test --tests "com.craftzero.save.MapItemSaveTest" --tests "com.craftzero.inventory.MapItemDataTest" --tests "com.craftzero.crafting.CraftingRegistryTest" --tests "com.craftzero.main.PlayerMapInteractionTest" --tests "com.craftzero.save.SaveManagerTest"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.SurvivalHudRendererTest"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.SurvivalHudRendererTest" --tests "com.craftzero.inventory.MapItemDataTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.BoatEntityTest" --tests "com.craftzero.entity.MinecartEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.FireballEntityTest" --tests "com.craftzero.entity.mob.GhastTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ArrowEntityTest" --tests "com.craftzero.entity.ThrownItemEntityTest" --tests "com.craftzero.entity.FireballEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.SilverfishTest" --tests "com.craftzero.world.StoneBrickMetadataInteractionTest" --tests "com.craftzero.entity.mob.MobSoundTest.silverfishEmitReleaseSounds"`
- `.\gradlew.bat test --tests "com.craftzero.entity.MinecartEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerMinecartInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.save.SaveManagerTest.minecartLivingPassengerRoundTrips"`
- `.\gradlew.bat test --tests "com.craftzero.world.MechanismSprintTest.dispenserEjectsMinecartItemsEvenWhenRailIsInFront" --tests "com.craftzero.world.MechanismSprintTest.dispenserEjectsBoatEvenWhenWaterIsInFront" --tests "com.craftzero.world.MechanismSprintTest.dispenserGenericItemEjectionUsesReleaseStyleOffsetAndSpread"`
- `.\gradlew.bat test --tests "com.craftzero.world.BoatInteractionTest" --tests "com.craftzero.entity.MinecartEntityTest.worldPlacementOnlyAcceptsMinecartItems" --tests "com.craftzero.entity.MinecartEntityTest.minecartPlacementLiftsCartsOnAscendingRails"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ThrownItemEntityTest" --tests "com.craftzero.entity.mob.EndermanTest.endermanDodgesThrownSnowballWithoutPlayerCreditHit" --tests "com.craftzero.world.SnowGolemInteractionTest.snowGolemThrowsSnowballsAtHostiles"`
- `.\gradlew.bat test --tests "com.craftzero.entity.PaintingEntityTest" --tests "com.craftzero.entity.ArrowEntityTest" --tests "com.craftzero.entity.ThrownItemEntityTest" --tests "com.craftzero.entity.FireballEntityTest" --tests "com.craftzero.entity.SplashPotionEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ArrowEntityTest" --tests "com.craftzero.entity.FireballEntityTest" --tests "com.craftzero.entity.BoatEntityTest" --tests "com.craftzero.entity.MinecartEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.EnderPearlEntityTest" --tests "com.craftzero.entity.PaintingEntityTest" --tests "com.craftzero.entity.BoatEntityTest" --tests "com.craftzero.entity.MinecartEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ThrownItemEntityTest" --tests "com.craftzero.entity.SplashPotionEntityTest" --tests "com.craftzero.entity.BoatEntityTest" --tests "com.craftzero.entity.MinecartEntityTest" --tests "com.craftzero.entity.PaintingEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.tile.MonsterSpawnerTileEntityTest.slimeSpawnerCreatesRandomSizedSlimes" --tests "com.craftzero.world.tile.MonsterSpawnerTileEntityTest.magmaCubeSpawnerCreatesRandomSizedMagmaCubes" --tests "com.craftzero.world.MobSpawnerTest.groundSpawnPacksValidateActualCreatedMobVolume" --tests "com.craftzero.entity.mob.SlimeTest" --tests "com.craftzero.entity.mob.MagmaCubeTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.tile.MonsterSpawnerTileEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.ZombiePigmanTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.WolfCombatBehaviorTest" --tests "com.craftzero.entity.mob.MobSoundTest.zombiePigmenEmitReleaseSounds" --tests "com.craftzero.save.SaveManagerTest.mobCombatTargetIdentityRoundTrip"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.WolfCombatBehaviorTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.ai.MobGoalRandomnessTest" --tests "com.craftzero.entity.mob.VillagerTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.MobSpawnerTest"`
- `.\gradlew.bat test --tests com.craftzero.ui.ChestScreenTest --tests com.craftzero.ui.DispenserScreenTest --tests com.craftzero.ui.FurnaceScreenTest --tests com.craftzero.ui.BrewingStandScreenTest --tests com.craftzero.ui.EnchantingTableScreenTest --tests com.craftzero.ui.InventoryScreenTest --tests com.craftzero.ui.CraftingTableScreenTest`
- `.\gradlew.bat test --tests "com.craftzero.entity.EyeOfEnderEntityTest" --tests "com.craftzero.main.PlayerEnderPearlInteractionTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.EyeOfEnderEntityTest" --tests "com.craftzero.main.PlayerEnderPearlInteractionTest" --tests "com.craftzero.save.SaveManagerTest.expiredEyeOfEnderAgeWithoutBackupIsCorrupt" --tests "com.craftzero.save.SaveManagerTest.transientPhysicsEntitiesRoundTripRuntimeState"`
- `.\gradlew.bat test --tests "com.craftzero.world.TileEntityTest.breakingRecordedJukeboxEjectsRecordBeforeBlockDrop" --tests "com.craftzero.world.TileEntityTest.jukeboxInteractionFollowsBlockMetadataGate" --tests "com.craftzero.world.WorldSoundEventTest.jukeboxEjectionEmitsPopSound"`
- `.\gradlew.bat test --tests "com.craftzero.world.TileEntityTest" --tests "com.craftzero.world.WorldSoundEventTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.mob.GiantTest" --tests "com.craftzero.entity.mob.MobFactoryTest" --tests "com.craftzero.world.tile.MonsterSpawnerTileEntityTest.giantSpawnersRequireFullGiantVolume" --tests "com.craftzero.save.SaveManagerTest.giantEntityRoundTrips"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.main.PlayerStatisticsTest" --tests "com.craftzero.ui.menu.MenuScreensTest" --tests "com.craftzero.save.SaveManagerTest.saveManagerRoundTripsWorldState"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerStatisticsTest" --tests "com.craftzero.main.PlayerFishingRodInteractionTest" --tests "com.craftzero.ui.menu.MenuScreensTest" --tests "com.craftzero.save.SaveManagerTest.saveManagerRoundTripsWorldState"`
- `.\gradlew.bat test --tests "com.craftzero.ui.menu.MenuScreensTest"`
- `.\gradlew.bat test --tests "com.craftzero.progression.ProgressionSystemsTest" --tests "com.craftzero.ui.menu.MenuRenderingTest"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerMovementConstantsTest.jumpBoostAddsReleaseOnePlayerJumpVelocity" --tests "com.craftzero.progression.ProgressionSystemsTest.livingEntityJumpBoostAddsReleaseOneJumpMotion"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerMovementConstantsTest" --tests "com.craftzero.progression.ProgressionSystemsTest.livingMobMovementPotionModifiersAffectAiMotion" --tests "com.craftzero.progression.ProgressionSystemsTest.livingEntityJumpBoostAddsReleaseOneJumpMotion"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerCombatTest.resistanceReducesAcceptedPlayerDamage" --tests "com.craftzero.progression.ProgressionSystemsTest.livingEntityResistanceReducesIncomingDamage"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerCombatTest.resistanceReducesAcceptedPlayerDamage" --tests "com.craftzero.main.PlayerCombatTest.swordBlockingReducesBlockableDamageBeforeArmor" --tests "com.craftzero.main.PlayerCombatTest.swordBlockingDoesNotReduceUnblockableFallDamage" --tests "com.craftzero.main.PlayerCombatTest.acceptedPlayerDamageWearsArmorByQuarterDamageRule" --tests "com.craftzero.progression.ProgressionSystemsTest.livingEntityResistanceReducesIncomingDamage" --tests "com.craftzero.progression.ProgressionSystemsTest.playerAttackPotionModifiersMatchReleaseOne" --tests "com.craftzero.progression.ProgressionSystemsTest.livingMobMovementPotionModifiersAffectAiMotion"`
- `.\gradlew.bat test --tests "com.craftzero.progression.ProgressionSystemsTest.waterBreathingPreservesPlayerAir" --tests "com.craftzero.entity.EntityWaterPhysicsTest.waterBreathingEffectPreventsGenericLivingEntityDrowning"`
- `.\gradlew.bat test --tests "com.craftzero.progression.ProgressionSystemsTest.respirationUsesReleaseOneAirConsumptionRolls" --tests "com.craftzero.progression.ProgressionSystemsTest.waterBreathingPreservesPlayerAir" --tests "com.craftzero.entity.EntityWaterPhysicsTest.livingEntitiesUseReleaseAirCounterForDrowning" --tests "com.craftzero.entity.EntityWaterPhysicsTest.underwaterBreathersDoNotUseGenericDrowning" --tests "com.craftzero.entity.EntityWaterPhysicsTest.waterBreathingEffectPreventsGenericLivingEntityDrowning"`
- `.\gradlew.bat test --tests "com.craftzero.world.FireInteractionTest.fireResistancePreventsMobLavaIgnitionAndDamage" --tests "com.craftzero.world.FireInteractionTest.fireResistancePreventsPlayerFireContactIgnitionAndDamage"`
- `.\gradlew.bat test --tests "com.craftzero.world.FireInteractionTest.mobTouchingFireIgnites" --tests "com.craftzero.world.FireInteractionTest.mobTouchingLavaBurnsAndTakesDamage" --tests "com.craftzero.world.FireInteractionTest.fireResistancePreventsMobLavaIgnitionAndDamage" --tests "com.craftzero.world.FireInteractionTest.playerTouchingFireTakesDamage" --tests "com.craftzero.world.FireInteractionTest.fireResistancePreventsPlayerFireContactIgnitionAndDamage" --tests "com.craftzero.world.FireInteractionTest.playerBurningTicksDealPeriodicDamage"`
- `.\gradlew.bat test --rerun-tasks --tests "com.craftzero.main.PlayerStatisticsTest" --tests "com.craftzero.ui.menu.MenuScreensTest.statisticsScreenListsPlayerCounters" --tests "com.craftzero.main.PlayerBoatInteractionTest.mountedBoatTravelUpdatesStatistics" --tests "com.craftzero.save.SaveManagerTest.saveManagerRoundTripsWorldState"`
- `.\gradlew.bat test --tests "com.craftzero.ui.menu.MenuScreenFactoryTest"`
- `.\gradlew.bat test --tests "com.craftzero.ui.menu.MenuScreensTest" --tests "com.craftzero.ui.menu.MenuScreenFactoryTest"`
- `.\gradlew.bat test --tests "com.craftzero.entity.MinecartEntityTest.worldMinecartCollisionSweepDoesNotExpandVertically" --tests "com.craftzero.entity.MinecartEntityTest.minecartsTransferMomentumOnCollision" --tests "com.craftzero.entity.MinecartEntityTest.worldStorageMinecartCollisionPassShovesLivingEntities"`
- `.\gradlew.bat test --tests "com.craftzero.entity.MinecartEntityTest"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerFishingRodInteractionTest" --tests "com.craftzero.main.PlayerStatisticsTest" --tests "com.craftzero.ui.menu.MenuScreensTest.statisticsScreenListsPlayerCounters" --tests "com.craftzero.save.SaveManagerTest.saveManagerRoundTripsWorldState"`
- `.\gradlew.bat test --tests "com.craftzero.ui.menu.MenuScreenFactoryTest.achievementAndStatisticsFactoryScreensUseLiveProgress"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.MovingPistonRendererTest" --tests "com.craftzero.world.MechanismSprintTest.stickyPistonHeadCarriesStickyMetadata"`
- `.\gradlew.bat test --tests "com.craftzero.main.PlayerStatisticsTest" --tests "com.craftzero.ui.menu.MenuScreensTest.statisticsScreenListsPlayerCounters" --tests "com.craftzero.ui.menu.MenuScreenFactoryTest.achievementAndStatisticsFactoryScreensUseLiveProgress" --tests "com.craftzero.save.SaveManagerTest.saveManagerRoundTripsWorldState"`
- `.\gradlew.bat test --tests "com.craftzero.graphics.ArrowRendererFishingLineTest"`
- `.\gradlew.bat test --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest.generatedNetherBaseChunkAppliesHellCavesAfterSurfaceReplacement" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneNetherDecoratesTerrain"`
- Compile-only for the item-5 End crystal zero-damage projectile-contact pass,
  per the current no-new-tests instruction.
- Compile-only for the item-6 finite entity/player pose hardening pass, per the
  current no-new-tests instruction.

## Suggested Execution Order

1. Finish the remaining registry and recipe source audit.
2. Complete block and tile-entity interactions, starting with furnace, chest,
   dispenser, brewing stand, enchanting table, spawner, signs, note blocks, and
   jukeboxes.
3. Do a redstone/piston/rail semantic pass, because many block interactions
   depend on update ordering. Minecarts now transfer horizontal momentum when
   their hit boxes overlap on rails instead of phasing through each other, and
   furnace carts now use powered-cart collision bias against ordinary carts.
   Moving storage/furnace minecarts now shove overlapping living entities
   during the world collision pass, while empty rideable minecarts capture
   living mobs as seated passengers only above the old moving-cart speed
   threshold and without stealing passengers already seated in another
   minecart. Stationary rideable carts shove nearby mobs instead of passively
   mounting them. The same sweep now includes the local player for
   unmounted cart shove behavior, while mounted players remain locked to their
   vehicle. The same sweep now uses the Release-era horizontal-only 0.2 block
   query expansion, so carts do not collide with vertically separated entities
   above or below the rail band. Minecart rail lookup now keeps slope-to-raised
   rail handoff behavior while rejecting unconnected upper rails below the cart's
   actual rail band. Cart-to-cart overlap now uses the old direct collision
   path without the leftover travel-axis veto, while non-cart player/mob shove
   uses the old squared-distance, quarter-strength entity impulse. Redstone
   dust networks now settle connected horizontal and stepped vertical wires as
   a bounded group, so branch-heavy lines converge power in the mechanism pass
   instead of waiting for each segment to wake one at a time. Furnace
   minecart player interaction now only accepts coal/charcoal use for fuel, but
   every successful use updates the push direction. Accepted one-coal use still
   starts the hand-use animation after the stack is consumed, while non-fuel
   and empty-hand use refresh direction without item consumption.
   Furnace minecart fuel depletion and same-position accepted use now clear
   stale push vectors instead of letting an unfueled or zero-direction engine
   retain direction, while saved `PushX`/`PushZ` values preserve the old raw
   source delta before physics ticks update the live engine vector. Powered
   furnace carts now apply the source-shaped post-move engine step instead of
   pre-moving with ordinary empty-cart drag. Minecart item placement now also
   applies the old half-block spawn lift on ascending rails. Minecart, boat,
   and saddled-pig dismounts now choose a nearby clear player box when the
   default side is blocked by loaded collision.
4. Do the entity and mob parity pass, including spawning, combat, drops, and
   save/load.
5. Continue worldgen/dimension parity from the dedicated worldgen report.
6. Finish render, animation, sound, and UI polish after behavior stabilizes.

## Open Risk

The project has many broad, dirty worktree changes in progress. Until each
category above has been source-audited and touched behavior has a focused
verification case, the game should be described as "Release 1.0 parity in
progress", not complete.
