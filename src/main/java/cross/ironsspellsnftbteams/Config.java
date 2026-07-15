package cross.ironsspellsnftbteams;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_TEAM_SUMMON_PROTECTION = BUILDER
            .comment("Whether Iron's Spells summons refuse to target players who share an FTB Team with their owner")
            .define("enableTeamSummonProtection", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
