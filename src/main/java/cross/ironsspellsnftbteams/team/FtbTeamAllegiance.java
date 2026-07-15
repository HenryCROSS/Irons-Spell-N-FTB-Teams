package cross.ironsspellsnftbteams.team;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

import java.util.UUID;

public final class FtbTeamAllegiance {

    private FtbTeamAllegiance() {
    }

    public static boolean arePlayersAllied(UUID playerA, UUID playerB) {
        if (playerA.equals(playerB)) {
            return true;
        }
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return false;
        }
        return FTBTeamsAPI.api().getManager().arePlayersInSameTeam(playerA, playerB);
    }
}
