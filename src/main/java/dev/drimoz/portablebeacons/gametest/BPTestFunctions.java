package dev.drimoz.portablebeacons.gametest;

import dev.drimoz.portablebeacons.PortableBeacons;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.Consumer;

/**
 * Puts the seven test bodies into the {@code test_function} registry.
 *
 * <p>26.1 dropped the {@code @GameTest} annotation: a test is now a registered function plus a
 * {@code test_instance} data file naming it, its structure and its timeout. The bodies did not
 * change - only how the game finds them.
 *
 * <p>Registered through {@link RegisterEvent} rather than {@code TestFunctionLoader}, which looks
 * like the intended hook but cannot work for a mod: {@code BuiltInRegistries} runs the loaders from
 * its own static bootstrap, which is over long before any mod is constructed. A loader registered
 * from a mod is simply never run, and every test fails with "missing test function" while the data
 * files load perfectly - which is exactly what happened.
 */
@EventBusSubscriber(modid = PortableBeacons.MOD_ID)
public final class BPTestFunctions {

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> {
            helper.register(id("beacon_applies_its_effect_to_the_carrier"),
                    (Consumer<GameTestHelper>) BPGameTests::beaconAppliesItsEffectToTheCarrier);
            helper.register(id("an_inactive_beacon_applies_nothing"),
                    (Consumer<GameTestHelper>) BPGameTests::anInactiveBeaconAppliesNothing);
            helper.register(id("aura_reaches_a_second_player"),
                    (Consumer<GameTestHelper>) BPGameTests::auraReachesASecondPlayer);
            helper.register(id("self_mode_reaches_nobody_else"),
                    (Consumer<GameTestHelper>) BPGameTests::selfModeReachesNobodyElse);
            helper.register(id("team_mode_excludes_players_off_the_team"),
                    (Consumer<GameTestHelper>) BPGameTests::teamModeExcludesPlayersOffTheTeam);
            helper.register(id("aura_reaches_a_tamed_pet_but_not_a_stray_one"),
                    (Consumer<GameTestHelper>) BPGameTests::auraReachesATamedPetButNotAStrayOne);
            helper.register(id("hostile_action_indices_are_rejected"),
                    (Consumer<GameTestHelper>) BPGameTests::hostileActionIndicesAreRejected);
            helper.register(id("wayfarer_charges_less_while_moving"),
                    (Consumer<GameTestHelper>) BPGameTests::wayfarerChargesLessWhileMoving);
            helper.register(id("effects_outlast_the_blink_until_the_next_pass"),
                    (Consumer<GameTestHelper>) BPGameTests::effectsOutlastTheBlinkUntilTheNextPass);
            helper.register(id("a_refused_slot_write_keeps_what_was_there"),
                    (Consumer<GameTestHelper>) BPGameTests::aRefusedSlotWriteKeepsWhatWasThere);
        });
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(PortableBeacons.MOD_ID, name);
    }

    private BPTestFunctions() {}
}
