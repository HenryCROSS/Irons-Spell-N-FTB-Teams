package cross.ironsspellsnftbteams.summon;

import cross.ironsspellsnftbteams.Config;
import cross.ironsspellsnftbteams.team.FtbTeamAllegiance;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

public final class SummonTeamProtection {

    private SummonTeamProtection() {
    }

    public static boolean shouldTreatAsAlly(Entity summon, Entity potentialTarget) {
        if (!Config.ENABLE_TEAM_SUMMON_PROTECTION.get()) {
            return false;
        }
        if (summon == null || potentialTarget == null || summon.level().isClientSide()) {
            return false;
        }

        Optional<UUID> summonOwner = SummonOwnerResolver.resolveResponsiblePlayer(summon);
        Optional<UUID> targetOwner = SummonOwnerResolver.resolveResponsiblePlayer(potentialTarget);
        if (summonOwner.isEmpty() || targetOwner.isEmpty()) {
            return false;
        }

        return FtbTeamAllegiance.arePlayersAllied(summonOwner.get(), targetOwner.get());
    }
}
