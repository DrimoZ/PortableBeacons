package dev.drimoz.portablebeacons.gametest;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.BeaconTicker;
import dev.drimoz.portablebeacons.core.AuraMode;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.BeaconEffectDef;
import dev.drimoz.portablebeacons.core.EffectSlotConfig;
import dev.drimoz.portablebeacons.core.BeaconState;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import dev.drimoz.portablebeacons.menu.PortableBeaconMenu;
import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.registry.BPComponents;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPLookups;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.TestFunctionLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-game tests for the seam the unit tests cannot reach: real players, in a real level, with the
 * aura actually looking around for them.
 *
 * <p>The aura is the one part of the mod whose behaviour depends on other entities existing, so it
 * could never be checked in single player - there, the carrier is the only player and every mode
 * looks identical.
 *
 * <p>These drive {@link BeaconTicker#tickPlayer} directly rather than waiting for the 40-tick
 * interval to come round, which also keeps them independent of server timing they do not control.
 *
 * <p><b>What this does not cover:</b> the {@code PlayerTickEvent.Post} subscription that calls
 * {@code tickPlayer} in the real game. The players here are synthetic - they are in the level and
 * tick as entities, but their connection never reaches the state where the server drives a player
 * tick, so the event never fires for them. A test that waited for it failed for that reason alone
 * and was removed rather than left red or weakened into passing. That one annotation is covered by
 * the mod being played.
 *
 * <p>Run headlessly with {@code ./gradlew runGameTestServer}, or {@code /test run portablebeacons} in a
 * client.
 */
public final class BPGameTests {

    /** Matches the structure named by every test_instance entry. */
    public static final String PLATFORM = "platform";
    public static final int TIMEOUT = 200;

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);

    private static final ResourceKey<BeaconEffectDef> SPEED =
            ResourceKey.create(BPRegistryKeys.EFFECT, BPRegistryKeys.id("speed"));

    /** Distinct names per player - see {@link #spawnPlayer(GameTestHelper, Cleanup, String)}. */
    private static final AtomicInteger NEXT_PLAYER = new AtomicInteger();

    private static final ResourceKey<AugmentDef> ATTUNEMENT =
            ResourceKey.create(BPRegistryKeys.AUGMENT, BPRegistryKeys.id("attunement"));
    private static final ResourceKey<AugmentDef> WAYFARER =
            ResourceKey.create(BPRegistryKeys.AUGMENT, BPRegistryKeys.id("wayfarer"));

    /** Mirrors tier_4.json. A test that silently disagreed with the data would prove nothing. */
    private static final int TIER_IV_AURA_RANK = 1;

    public static void beaconAppliesItsEffectToTheCarrier(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            giveBeacon(carrier, AuraMode.SELF);

            BeaconTicker.tickPlayer(carrier);

            helper.assertTrue(carrier.getEffect(MobEffects.SPEED) != null,
                    "the carrier did not receive the effect their own beacon projects");
        });
    }

    public static void anInactiveBeaconAppliesNothing(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ItemStack beacon = giveBeacon(carrier, AuraMode.SELF);
            PortableBeaconItem.setState(beacon, PortableBeaconItem.stateOf(beacon).withActive(false));

            BeaconTicker.tickPlayer(carrier);

            helper.assertTrue(carrier.getEffect(MobEffects.SPEED) == null,
                    "a beacon switched off still applied its effect");
        });
    }

    /** The half of the aura that would otherwise need a second client to test by hand. */
    public static void auraReachesASecondPlayer(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ServerPlayer bystander = spawnPlayer(helper, cleanup);
            giveBeacon(carrier, AuraMode.ALLIES);

            BeaconTicker.tickPlayer(carrier);

            helper.assertTrue(bystander.getEffect(MobEffects.SPEED) != null,
                    "a player standing next to the carrier was not reached by the aura");
        });
    }

    public static void selfModeReachesNobodyElse(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ServerPlayer bystander = spawnPlayer(helper, cleanup);
            giveBeacon(carrier, AuraMode.SELF);

            BeaconTicker.tickPlayer(carrier);

            helper.assertTrue(carrier.getEffect(MobEffects.SPEED) != null,
                    "the carrier lost their own effect");
            helper.assertTrue(bystander.getEffect(MobEffects.SPEED) == null,
                    "a beacon set to self only leaked its effect to a bystander");
        });
    }

    /**
     * Team mode is the one that excludes, and the exclusion is what cannot be seen in single
     * player: there, the carrier is trivially allied with everyone who exists.
     */
    public static void teamModeExcludesPlayersOffTheTeam(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ServerPlayer outsider = spawnPlayer(helper, cleanup);

            Scoreboard scoreboard = helper.getLevel().getScoreboard();
            PlayerTeam team = scoreboard.addPlayerTeam("portablebeacons_test_team");
            cleanup.add(() -> scoreboard.removePlayerTeam(team));
            scoreboard.addPlayerToTeam(carrier.getScoreboardName(), team);

            giveBeacon(carrier, AuraMode.TEAM);
            BeaconTicker.tickPlayer(carrier);

            helper.assertTrue(carrier.getEffect(MobEffects.SPEED) != null,
                    "the carrier lost their own effect in team mode");
            helper.assertTrue(outsider.getEffect(MobEffects.SPEED) == null,
                    "team mode reached a player who is not on the team");
        });
    }

    public static void auraReachesATamedPetButNotAStrayOne(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            giveBeacon(carrier, AuraMode.ALLIES_AND_PETS);

            Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, CENTRE);
            pet.tame(carrier);
            Wolf stray = helper.spawnWithNoFreeWill(EntityType.WOLF, CENTRE);

            BeaconTicker.tickPlayer(carrier);

            helper.assertTrue(pet.getEffect(MobEffects.SPEED) != null,
                    "a tamed pet was not reached by allies_and_pets");
            helper.assertTrue(stray.getEffect(MobEffects.SPEED) == null,
                    "an untamed animal was reached by allies_and_pets");
        });
    }

    /**
     * Regression test for negative indices.
     *
     * <p>slot and value are var-ints on the wire, so a modified client can send -1. Every guard in
     * applyAction was an upper bound, so a negative index used to reach List.set / List.remove and
     * throw on the server thread.
     */
    public static void hostileActionIndicesAreRejected(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ItemStack beacon = giveBeacon(carrier, AuraMode.SELF);

            // Built directly rather than through BeaconMenuOpener: openMenu writes a screen payload,
            // and a mock player's connection refuses one outright ("Payload
            // neoforge:advanced_open_screen may not be sent to the client"). Nothing in the action
            // path needs a real connection - broadcastChanges is a no-op without a synchronizer.
            PortableBeaconMenu menu = new PortableBeaconMenu(1, carrier.getInventory(), 0);
            BeaconState before = PortableBeaconItem.stateOf(beacon);

            int[] actions = {
                    PortableBeaconMenu.ACTION_SET_EFFECT,
                    PortableBeaconMenu.ACTION_CLEAR_EFFECT,
                    PortableBeaconMenu.ACTION_CYCLE_AMPLIFIER,
                    PortableBeaconMenu.ACTION_TOGGLE_EFFECT,
                    PortableBeaconMenu.ACTION_CYCLE_AURA,
            };
            for (int action : actions) {
                // Each of these threw before the guard was added. The real assertion is that
                // nothing escapes, but the return value is checked too, so a silent no-op that
                // reported success would still fail.
                helper.assertFalse(menu.applyAction(action, -1, 0),
                        "a negative slot index was accepted by action " + action);
                helper.assertFalse(menu.applyAction(action, 0, -1),
                        "a negative value was accepted by action " + action);
                helper.assertFalse(menu.applyAction(action, Integer.MIN_VALUE, Integer.MIN_VALUE),
                        "Integer.MIN_VALUE was accepted by action " + action);
            }

            helper.assertTrue(PortableBeaconItem.stateOf(beacon).equals(before),
                    "a rejected action still changed the beacon");
        });
    }

    /**
     * Regression: movement used to be read as {@code getX() - xOld}, which the server's tick order
     * makes zero by the time the player tick fires. Every carrier counted as standing still, so
     * Wayfarer only ever charged its surcharge.
     */
    public static void wayfarerChargesLessWhileMoving(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ItemStack beacon = giveBeacon(carrier, AuraMode.SELF);
            install(beacon, WAYFARER, 2);

            int still = fuelSpentByOnePass(carrier, beacon);
            carrier.setKnownMovement(new Vec3(0.25, 0.0, 0.0));
            int moving = fuelSpentByOnePass(carrier, beacon);

            helper.assertTrue(moving < still,
                    "Wayfarer charged " + moving + " moving against " + still + " standing still");
        });
    }

    /**
     * Vanilla blinks a status icon and strobes Night Vision under 200 ticks remaining. The effect
     * has to stay above that for the whole interval until the next refresh, or it blinks every pass.
     */
    public static void effectsOutlastTheBlinkUntilTheNextPass(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            giveBeacon(carrier, AuraMode.SELF);

            BeaconTicker.tickPlayer(carrier);

            MobEffectInstance speed = carrier.getEffect(MobEffects.SPEED);
            helper.assertTrue(speed != null, "the carrier did not receive the effect");
            helper.assertFalse(
                    speed.endsWithin(BeaconTicker.VANILLA_BLINK_TICKS + BeaconTicker.INTERVAL),
                    "the effect drops into vanilla's blink window before the next pass refreshes it");
        });
    }

    /**
     * Regression: a slot write was "extract what is there, insert what was asked", committed even
     * when the insert came up short - keeping the extract and losing the slot's contents.
     */
    public static void aRefusedSlotWriteKeepsWhatWasThere(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ItemStack beacon = giveBeacon(carrier, AuraMode.SELF);
            install(beacon, ATTUNEMENT, 1);
            PortableBeaconMenu menu = new PortableBeaconMenu(1, carrier.getInventory(), 0);

            // Augments stack to one, so an insert of two is cut short.
            menu.getSlot(0).set(augment(WAYFARER, 1).copyWithCount(2));

            List<AugmentInstance> installed = BPLookups.installedAugments(beacon);
            helper.assertTrue(installed.size() == 1 && installed.get(0).type().equals(ATTUNEMENT),
                    "a refused write replaced the installed augment with " + installed);
        });
    }

    /**
     * Regression: the tier recipes were plain shaped recipes, so crafting a Beacon II into a III
     * built the result from nothing - installed augments, fuel and configured effects all gone.
     */
    public static void upgradingABeaconKeepsWhatItCarried(GameTestHelper helper) {
        run(helper, cleanup -> {
            ItemStack old = new ItemStack(BPItems.BEACON_II.get());
            BeaconState state = new BeaconState(
                    List.of(new EffectSlotConfig(SPEED, 0, true, AuraMode.SELF)), 1234, true, 3600);
            PortableBeaconItem.setState(old, state);
            install(old, WAYFARER, 1);

            ItemStack star = new ItemStack(net.minecraft.world.item.Items.NETHER_STAR);
            ItemStack diamond = new ItemStack(net.minecraft.world.item.Items.DIAMOND);
            ItemStack block = new ItemStack(net.minecraft.world.item.Items.BEACON);
            CraftingInput grid = CraftingInput.of(3, 3, List.of(
                    ItemStack.EMPTY, star, ItemStack.EMPTY,
                    diamond, old, diamond,
                    ItemStack.EMPTY, block, ItemStack.EMPTY));

            ServerLevel level = helper.getLevel();
            ItemStack result = level.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, grid, level)
                    .map(holder -> holder.value().assemble(grid))
                    .orElse(ItemStack.EMPTY);

            helper.assertTrue(result.is(BPItems.BEACON_III.get()), "the grid did not craft a Beacon III");
            helper.assertTrue(PortableBeaconItem.stateOf(result).equals(state),
                    "the upgrade lost the beacon's effects or fuel");
            List<AugmentInstance> augments = BPLookups.installedAugments(result);
            helper.assertTrue(augments.size() == 1 && augments.get(0).type().equals(WAYFARER),
                    "the upgrade lost the installed augment, found " + augments);
        });
    }

    private static int fuelSpentByOnePass(ServerPlayer carrier, ItemStack beacon) {
        int before = PortableBeaconItem.stateOf(beacon).fuel();
        BeaconTicker.tickPlayer(carrier);
        return before - PortableBeaconItem.stateOf(beacon).fuel();
    }

    /**
     * Places a real {@link ServerPlayer} in the level - not a mock object: the aura queries the
     * level for nearby players, so a player that is not in it would pass every test vacuously.
     */
    private static ServerPlayer spawnPlayer(GameTestHelper helper, Cleanup cleanup) {
        return spawnPlayer(helper, cleanup, "bp_test_" + NEXT_PLAYER.getAndIncrement());
    }

    /**
     * Built here rather than through {@code GameTestHelper#makeMockServerPlayerInLevel} for one
     * reason: that helper names every player it makes {@code test-mock-player}, and a scoreboard
     * team is keyed by name. Two players sharing a name are the same team member, which made the
     * team test assert something that could never be false.
     */
    private static ServerPlayer spawnPlayer(GameTestHelper helper, Cleanup cleanup, String name) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(
                level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);

        BlockPos where = helper.absolutePos(CENTRE);
        player.teleportTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5);
        cleanup.add(() -> level.getServer().getPlayerList().remove(player));
        return player;
    }

    /** A tier IV beacon, switched on, projecting Speed, with more fuel than a test can burn. */
    /**
     * A tier IV beacon set to one sharing mode, with whatever Attunement that mode requires.
     *
     * <p>A tier grants sharing only up to its own {@code aura_rank}, and anything beyond it is
     * fitted, not given. Without the augment the resolver quietly falls the mode back to
     * {@link AuraMode#SELF} — which is correct, and would make an aura test fail for a reason that
     * has nothing to do with auras.
     */
    private static ItemStack giveBeacon(ServerPlayer player, AuraMode aura) {
        ItemStack beacon = new ItemStack(BPItems.BEACON_IV.get());
        BeaconState state = new BeaconState(
                List.of(new EffectSlotConfig(SPEED, 0, true, aura)),
                Integer.MAX_VALUE / 2, true, 0);
        PortableBeaconItem.setState(beacon, state);

        int needed = aura.rank() - TIER_IV_AURA_RANK;
        if (needed > 0) {
            install(beacon, ATTUNEMENT, needed);
        }
        player.getInventory().setItem(0, beacon);
        return beacon;
    }

    /** Fits an augment into the first free augment slot. */
    private static void install(ItemStack beacon, ResourceKey<AugmentDef> type, int tier) {
        BeaconState state = PortableBeaconItem.stateOf(beacon);
        ResourceHandler<ItemResource> slots = BPLookups.handlerOf(beacon);
        int slot = PortableBeaconItem.augmentSlot(0);
        while (slots.getAmountAsInt(slot) > 0) {
            slot++;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            slots.insert(slot, ItemResource.of(augment(type, tier)), 1, transaction);
            transaction.commit();
        }
        // Re-set: the handler wrote the container component onto the same stack, and the state
        // read above was captured before it did.
        PortableBeaconItem.setState(beacon, state);
    }

    private static ItemStack augment(ResourceKey<AugmentDef> type, int tier) {
        ItemStack augment = new ItemStack(BPItems.AUGMENT.get());
        augment.set(BPComponents.AUGMENT.get(), new AugmentInstance(type, tier));
        return augment;
    }

    private static void run(GameTestHelper helper, java.util.function.Consumer<Cleanup> body) {
        Cleanup cleanup = new Cleanup();
        cleanup.around(() -> {
            body.accept(cleanup);
            helper.succeed();
        });
    }

    /**
     * Undoes what a test added to shared server state.
     *
     * <p>Mock players join the real player list and a scoreboard team is global, so a test that
     * failed halfway would otherwise poison every test after it - which surfaces as a cascade of
     * unrelated failures. Hence running the body inside a finally.
     */
    private static final class Cleanup {
        private final List<Runnable> actions = new ArrayList<>();

        void add(Runnable action) {
            actions.add(action);
        }

        void around(Runnable body) {
            try {
                body.run();
            } finally {
                for (int i = actions.size() - 1; i >= 0; i--) {
                    actions.get(i).run();
                }
            }
        }
    }

    private BPGameTests() {}
}
