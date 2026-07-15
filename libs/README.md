# libs/

This directory is where the two compile-time dependency jars must be placed before building. They are **not** committed to this repository — both mods are "All Rights Reserved" and redistributing their jars is not permitted (see the main [README](../README.md#requirements)).

Copy the following files from your Minecraft client's `mods/` folder (or download them from CurseForge) into this directory:

```
libs/irons_spellbooks-1.21.1-3.16.2.jar
libs/ftb-teams-neoforge-2101.1.10.jar
```

Versions must match `irons_spellbooks_version` / `ftbteams_version` in [gradle.properties](../gradle.properties). See [PLAN.md](../PLAN.md#13-编译期依赖buildgradle) for details on why they're vendored this way instead of pulled from Maven.
