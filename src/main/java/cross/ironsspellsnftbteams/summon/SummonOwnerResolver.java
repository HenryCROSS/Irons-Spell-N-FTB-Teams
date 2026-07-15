package cross.ironsspellsnftbteams.summon;

import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public final class SummonOwnerResolver {

    private static final int MAX_OWNER_CHAIN_DEPTH = 8;

    private SummonOwnerResolver() {
    }

    public static Optional<UUID> resolveResponsiblePlayer(Entity entity) {
        return resolve(entity, 0);
    }

    private static Optional<UUID> resolve(Entity entity, int depth) {
        if (entity == null || depth >= MAX_OWNER_CHAIN_DEPTH) {
            return Optional.empty();
        }
        if (entity instanceof Player player) {
            return Optional.of(player.getUUID());
        }
        if (entity instanceof IMagicSummon summon) {
            return resolve(summon.getSummoner(), depth + 1);
        }
        if (entity instanceof OwnableEntity ownable) {
            return resolve(ownable.getOwner(), depth + 1);
        }
        return Optional.empty();
    }
}
