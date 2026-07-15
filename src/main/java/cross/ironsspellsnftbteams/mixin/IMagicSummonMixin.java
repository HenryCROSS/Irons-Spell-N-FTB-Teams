package cross.ironsspellsnftbteams.mixin;

import cross.ironsspellsnftbteams.summon.SummonTeamProtection;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Targets a default interface method: the mixin class must itself implement IMagicSummon.
// See PLAN.md section 5.1 for the per-class fallback if this pattern proves unsupported.
@Mixin(value = IMagicSummon.class, remap = false)
public interface IMagicSummonMixin extends IMagicSummon {

    @Inject(method = "isAlliedHelper", at = @At("HEAD"), cancellable = true)
    private void ironsspellsnftbteams$checkFtbTeam(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (SummonTeamProtection.shouldTreatAsAlly((Entity) this, entity)) {
            cir.setReturnValue(true);
        }
    }
}
