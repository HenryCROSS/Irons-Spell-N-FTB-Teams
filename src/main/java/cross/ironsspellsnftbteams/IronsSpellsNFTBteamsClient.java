package cross.ironsspellsnftbteams;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Client-only: exposes the config toggle (enableTeamSummonProtection) via the Mods screen.
@Mod(value = IronsSpellsNFTBteams.MODID, dist = Dist.CLIENT)
public class IronsSpellsNFTBteamsClient {
    public IronsSpellsNFTBteamsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
