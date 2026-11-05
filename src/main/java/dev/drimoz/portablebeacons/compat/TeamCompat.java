package dev.drimoz.portablebeacons.compat;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * Who counts as the carrier's team: a shared scoreboard team, or a shared FTB Teams party.
 *
 * <p>Team sharing used to ask the scoreboard alone. FTB Teams keeps its own parties and never fills
 * scoreboard teams, so in the packs that use it - most of the packs this mod is put in - "your team"
 * reached nobody, with nothing on screen to say why. Optional the way Curios is: the API is only
 * resolved inside {@link Inner}, behind the load check.
 */
public final class TeamCompat {

    private static final boolean FTB_TEAMS = ModList.get().isLoaded("ftbteams");

    public static boolean sameTeam(Player carrier, Player other) {
        return other.isAlliedTo(carrier) || (FTB_TEAMS && Inner.sameParty(carrier, other));
    }

    private static final class Inner {
        static boolean sameParty(Player carrier, Player other) {
            var api = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api();
            // Every player has a personal team of one, so "same team" is only ever a real party.
            return api.isManagerLoaded()
                    && api.getManager().arePlayersInSameTeam(carrier.getUUID(), other.getUUID());
        }

        private Inner() {}
    }

    private TeamCompat() {}
}
