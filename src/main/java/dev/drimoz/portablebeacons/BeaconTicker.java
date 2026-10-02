package dev.drimoz.portablebeacons;

import dev.drimoz.portablebeacons.compat.CuriosCompat;
import dev.drimoz.portablebeacons.core.AuraMode;
import dev.drimoz.portablebeacons.core.Durations;
import dev.drimoz.portablebeacons.core.BeaconEffectDef;
import dev.drimoz.portablebeacons.core.EffectSlotConfig;
import dev.drimoz.portablebeacons.core.FuelBudget;
import dev.drimoz.portablebeacons.core.BeaconResolver;
import dev.drimoz.portablebeacons.core.BeaconState;
import dev.drimoz.portablebeacons.core.BeaconStats;
import dev.drimoz.portablebeacons.core.BeaconTierDef;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import dev.drimoz.portablebeacons.registry.BPLookups;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Applies beacon effects and charges for them.
 *
 * <p>Runs every {@link #INTERVAL} ticks rather than every tick: a beacon that follows the player
 * does not need 20 Hz precision, and the aura scan is an AABB query per projecting effect.
 */
@EventBusSubscriber(modid = PortableBeacons.MOD_ID)
public final class BeaconTicker {

    public static final int INTERVAL = 40;
    private static final double SECONDS_PER_INTERVAL = INTERVAL / 20.0;

    /** Vanilla blinks an effect's HUD icon, and flickers Night Vision, once this few ticks remain. */
    public static final int VANILLA_BLINK_TICKS = 200;

    /**
     * Must clear {@link #VANILLA_BLINK_TICKS} by a full interval plus a margin. At 220 every effect
     * spent half of each interval below that line - icons blinking, Night Vision strobing - so this
     * is the conduit's figure rather than one picked to merely outlast the interval.
     */
    public static final int EFFECT_DURATION = 260;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % INTERVAL != 0) {
            return;
        }
        rechargeFromBeacons(player);
        tickPlayer(player);
    }

    /**
     * Tops up every beacon the player carries, switched on or not, while they stand in a lit
     * beacon's range - so a base with a beacon is where portable ones are charged.
     *
     * <p>"In range" is read from the effects a beacon has put on the player: vanilla beacons apply
     * ambient instances, out to their real pyramid range, which no block-entity scan here could
     * know. Conduit Power is ambient too but comes from a conduit, so it does not count. Runs before
     * the beacon's own pass, so a beacon left on and starved resumes on the same pass.
     */
    public static void rechargeFromBeacons(Player player) {
        int perSecond = BPConfig.INSTANCE.beaconRechargePerSecond.get();
        if (!BPConfig.fuelEnabled() || perSecond <= 0 || !insideBeaconRange(player)) {
            return;
        }
        int amount = (int) Math.min(Integer.MAX_VALUE, (long) perSecond * INTERVAL / 20);
        RegistryAccess access = player.level().registryAccess();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            recharge(player.getInventory().getItem(slot), amount, access);
        }
        recharge(CuriosCompat.findBeacon(player), amount, access);
    }

    private static boolean insideBeaconRange(Player player) {
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.isAmbient() && !effect.is(MobEffects.CONDUIT_POWER)) {
                return true;
            }
        }
        return false;
    }

    private static void recharge(ItemStack stack, int amount, RegistryAccess access) {
        if (!(stack.getItem() instanceof PortableBeaconItem item)) {
            return;
        }
        BeaconTierDef tier = BPLookups.tier(access, item);
        if (tier == null) {
            return;
        }
        int capacity = BeaconResolver.resolve(tier, BPLookups.installedAugments(stack), BPLookups.augments(access))
                .fuelCapacity();
        BeaconState state = PortableBeaconItem.stateOf(stack);
        int fuel = FuelBudget.recharge(state.fuel(), capacity, amount);
        if (fuel != state.fuel()) {
            PortableBeaconItem.setState(stack, state.withFuel(fuel).withCapacity(capacity));
        }
    }

    /**
     * One pass of the beacon loop for one player.
     *
     * <p>Split out of the event handler so the game tests can drive a pass directly. Waiting for
     * {@code tickCount % INTERVAL} to line up made every test depend on server timing it does not
     * control, and a test that fails because the tick counter landed badly teaches nothing.
     */
    public static void tickPlayer(Player player) {
        // A dimension the server has switched beacons off in: nothing applied and nothing spent,
        // rather than charging for effects that are then withheld.
        if (BPConfig.disabledIn(player.level().dimension())) {
            return;
        }
        ItemStack beacon = findActiveBeacon(player);
        if (beacon.isEmpty()) {
            return;
        }
        tickBeacon(player, beacon);
    }

    /**
     * Only the first active beacon does anything. Letting several stack would make the tier ladder
     * pointless — four tier-I beacons would beat one tier-IV.
     */
    private static ItemStack findActiveBeacon(Player player) {
        // Curios first: a beacon the player has deliberately equipped should beat one that happens to
        // be loose in the bag. The reverse order made a worn beacon look broken - the inventory one
        // quietly won and there was nothing on screen to say why.
        ItemStack worn = CuriosCompat.findActiveBeacon(player);
        if (!worn.isEmpty()) {
            return worn;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof PortableBeaconItem && PortableBeaconItem.stateOf(stack).active()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void tickBeacon(Player player, ItemStack beacon) {
        RegistryAccess access = player.level().registryAccess();
        PortableBeaconItem item = (PortableBeaconItem) beacon.getItem();
        BeaconTierDef tier = BPLookups.tier(access, item);
        if (tier == null) {
            return;
        }

        BeaconResolver.Lookup<BeaconEffectDef> effectLookup = BPLookups.effects(access);
        BeaconStats stats = BPLookups.stats(beacon, access);

        BeaconState state = BeaconResolver.sanitize(
                PortableBeaconItem.stateOf(beacon), stats, effectLookup, tier);

        List<EffectSlotConfig> toApply = new ArrayList<>(state.effects().size());
        for (EffectSlotConfig slot : state.effects()) {
            if (!slot.enabled()) {
                continue;
            }
            if (isCoveredByRealBeacon(player, slot, effectLookup)) {
                // Skipped entirely, not just made free. Re-applying over the beacon's instance
                // would replace an ambient effect with a non-ambient one, the coverage check would
                // fail on the next tick, and the beacon would start charging again - flipping between
                // free and paid every two seconds.
                continue;
            }
            toApply.add(slot);
        }
        // Billed through the resolver rather than summed here, so free slots and the movement
        // multipliers apply to what is charged exactly as they do to what the screen displays.
        double owed = BeaconResolver.fuelPerSecond(toApply, stats, effectLookup, isMoving(player));

        int cost = FuelBudget.costFor(owed, SECONDS_PER_INTERVAL);
        if (BPConfig.INSTANCE.requireFuel.get() && cost > 0) {
            state = refuel(beacon, state, stats, cost, player.level().registryAccess());
            if (state.fuel() < cost) {
                runDry(player, beacon, state);
                return;
            }
            warnIfRunningLow(player, beacon, state.fuel(), cost, owed);
            state = state.withFuel(state.fuel() - cost);
        }

        if (state.starved()) {
            // Fuel arrived for a beacon left on: it resumes by itself, and says so the way a real
            // beacon does when its pyramid is completed.
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4F, 1.0F);
        }
        PortableBeaconItem.setState(beacon, state.withStarved(false));
        for (EffectSlotConfig slot : toApply) {
            apply(player, slot, stats, effectLookup);
        }
    }

    /**
     * Whether the carrier counts as travelling this tick.
     *
     * <p>Deliberately coarse — horizontal movement only, and a threshold well above the drift a
     * standing player produces. An augment that pays out differently for moving and standing still
     * must not flicker between the two because someone shifted their feet.
     *
     * <p>Read from the movement the client last reported, not from {@code getX() - xOld}. The level
     * tick resets {@code xOld} to the current position just before the connection tick fires
     * {@code PlayerTickEvent}, so on the server that difference was always zero: every player
     * counted as standing still, and Wayfarer only ever charged its surcharge.
     */
    private static boolean isMoving(Player player) {
        Vec3 movement = player instanceof ServerPlayer server
                ? server.getKnownMovement()
                : player.getDeltaMovement();
        return movement.horizontalDistanceSqr() > 0.0025;
    }

    /**
     * Switches the beacon off and says so.
     *
     * <p>Effects stopping with no explanation reads as a bug, so it says so - once, on the pass
     * that runs dry, which the starved flag is what remembers.
     *
     * <p>It stays switched on. It used to switch itself off, so a player who refuelled still had
     * to open the screen and turn it back on, and usually found out by noticing the effects were
     * gone. Starved, it resumes on the first pass that finds fuel.
     */
    private static void runDry(Player player, ItemStack beacon, BeaconState state) {
        if (!state.starved()) {
            ActionBar.send(player,
                    Component.translatable("portablebeacons.msg.out_of_fuel").withStyle(ChatFormatting.RED));
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.0F);
        }
        PortableBeaconItem.setState(beacon, state.withStarved(true));
    }

    /** A minute's warning: enough to reach the fuel, too little to forget about it. */
    public static final int LOW_FUEL_SECONDS = 60;

    /**
     * Warns once, on the pass that takes the beacon under a minute of runtime - buffer and slot
     * together, since the slot is burned before the beacon runs dry. Running out with no warning was
     * the other half of the old silence.
     */
    private static void warnIfRunningLow(Player player, ItemStack beacon, int fuel, int cost, double owed) {
        if (owed <= 0.0) {
            return;
        }
        int reserve = BPLookups.reserveUnits(beacon, player.level().registryAccess());
        double before = (fuel + reserve) / owed;
        double after = (fuel - cost + reserve) / owed;
        if (before >= LOW_FUEL_SECONDS && after < LOW_FUEL_SECONDS) {
            ActionBar.send(player, Component.translatable("portablebeacons.gui.low_fuel",
                    Durations.format((int) after)).withStyle(ChatFormatting.GOLD));
        }
    }

    /**
     * Tops the buffer up from the fuel slot, only while it is short, and only by as many items as
     * the shortfall needs - burning a netherite ingot to cover a 3-unit gap would be an unpleasant
     * surprise.
     *
     * <p>This used to burn one item per pass at most, so a build costing more per pass than one
     * item is worth ran dry with a full stack sitting in the slot.
     */
    private static BeaconState refuel(ItemStack beacon, BeaconState state, BeaconStats stats,
                                    int cost, RegistryAccess access) {
        if (state.fuel() >= cost) {
            return state;
        }
        ResourceHandler<ItemResource> handler = BPLookups.handlerOf(beacon);
        if (handler == null || handler.size() <= PortableBeaconItem.FUEL_SLOT) {
            return state;
        }
        ItemResource fuel = handler.getResource(PortableBeaconItem.FUEL_SLOT);
        if (fuel.isEmpty()) {
            return state;
        }
        int units = BPLookups.fuelValue(access, fuel.getItem());
        int burn = FuelBudget.itemsToBurn(state.fuel(), cost, units, stats.fuelCapacity(),
                handler.getAmountAsInt(PortableBeaconItem.FUEL_SLOT));
        if (burn <= 0) {
            return state;
        }
        // Closing without committing aborts, so a slot that will not give up its items leaves the
        // buffer untouched rather than crediting fuel that was never burned.
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.extract(PortableBeaconItem.FUEL_SLOT, fuel, burn, transaction) != burn) {
                return state;
            }
            transaction.commit();
        }
        return state.withFuel(state.fuel() + burn * units);
    }

    /**
     * A real beacon already covering this effect makes the beacon free for it.
     *
     * <p>Detection leans on the fact that beacons apply <em>ambient</em> instances while the beacon
     * deliberately does not — see {@link #apply}.
     */
    private static boolean isCoveredByRealBeacon(Player player, EffectSlotConfig slot,
                                                 BeaconResolver.Lookup<BeaconEffectDef> lookup) {
        if (!BPConfig.INSTANCE.freeWhileNearBeacon.get()) {
            return false;
        }
        Optional<BeaconEffectDef> def = lookup.get(slot.effect());
        if (def.isEmpty()) {
            return false;
        }
        MobEffectInstance existing = player.getEffect(def.get().effect());
        return existing != null && existing.isAmbient() && existing.getAmplifier() >= slot.amplifier();
    }

    private static void apply(Player carrier, EffectSlotConfig slot, BeaconStats stats,
                              BeaconResolver.Lookup<BeaconEffectDef> lookup) {
        Optional<BeaconEffectDef> maybeDef = lookup.get(slot.effect());
        if (maybeDef.isEmpty()) {
            return;
        }
        BeaconEffectDef def = maybeDef.get();

        for (LivingEntity target : targets(carrier, slot.aura(), stats.range())) {
            // Not ambient on purpose: that flag is what lets isCoveredByRealBeacon tell a genuine
            // beacon apart from our own effect a tick later.
            target.addEffect(new MobEffectInstance(def.effect(), EFFECT_DURATION, slot.amplifier(),
                    false, !stats.hideParticles(), !stats.hideIcon()));
        }
    }

    private static List<LivingEntity> targets(Player carrier, AuraMode mode, double range) {
        List<LivingEntity> found = new ArrayList<>();
        found.add(carrier);
        if (!mode.isAura() || range <= 0.0) {
            return found;
        }

        AABB box = carrier.getBoundingBox().inflate(range);
        for (Player other : carrier.level().getEntitiesOfClass(Player.class, box)) {
            if (other != carrier && reaches(carrier, other, mode)) {
                found.add(other);
            }
        }
        if (mode == AuraMode.ALLIES_AND_PETS) {
            for (TamableAnimal pet : carrier.level().getEntitiesOfClass(TamableAnimal.class, box)) {
                if (pet.isTame() && pet.getOwner() == carrier) {
                    found.add(pet);
                }
            }
        }
        return found;
    }

    private static boolean reaches(Player carrier, Player other, AuraMode mode) {
        if (mode == AuraMode.TEAM) {
            return other.isAlliedTo(carrier);
        }
        return BPConfig.INSTANCE.auraAffectsNonTeamPlayers.get() || other.isAlliedTo(carrier);
    }

    private BeaconTicker() {}
}
