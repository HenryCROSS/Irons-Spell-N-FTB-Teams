package cross.ironsspellsnftbteams;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

// Bridges Iron's Spells 'n Spellbooks summon targeting with FTB Teams; see PLAN.md.
// All behavior lives in cross.ironsspellsnftbteams.mixin/summon/team; this class only wires config.
@Mod(IronsSpellsNFTBteams.MODID)
public class IronsSpellsNFTBteams {
    public static final String MODID = "ironsspellsnftbteams";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IronsSpellsNFTBteams(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
