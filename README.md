# Irons Spells N FTB Teams

[English](#english) | [中文](#中文)

---

## English

A bridge / addon mod: makes **[Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks)** spell summons (skeletons, zombies, vexes, polar bears, etc.) respect **FTB Teams** allegiance when picking attack targets — within the same FTB team, players (and their summons) will never be targeted by each other's summons, even if they can still deal damage to one another directly.

This mod adds no new items, blocks, or spells. It only patches the summon targeting logic with a minimal Mixin injection into `IMagicSummon#isAlliedHelper`. See [PLAN.md](PLAN.md) for the full design rationale.

### Requirements

| Mod | Required version | Notes |
|---|---|---|
| Minecraft | 1.21.1 | |
| NeoForge | 21.1.235 | |
| [Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) | 1.21.1-3.16.2+ | Required dependency; missing it will fail to load |
| [FTB Teams](https://www.curseforge.com/minecraft/mc-mods/ftb-teams) | 2101.1.10+ | Required dependency; missing it will fail to load |

Iron's Spells and FTB Teams each have their own transitive dependencies (`geckolib`, `curios`, `playeranimator`, `irons_lib`, `architectury`, `ftblibrary`) — install per their own requirements; this mod does not redeclare them.

### Configuration

Single toggle, `enableTeamSummonProtection` (default `true`). Disabling it fully reverts to vanilla Iron's Spells behavior, no need to uninstall the mod. Editable via the config file or in-game config screen.

### About this project

This mod's code was generated and written by AI (Claude Code) under user direction.

### License

GPL-3.0-or-later

---

## 中文

一个桥接 / addon 模组：让 **[Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks)** 的法术召唤物（骷髅、僵尸、Vex、极地熊等）在选取攻击目标时，尊重 **FTB Teams** 的队伍关系——同一个 FTB 队伍内的玩家之间，即使互相能造成伤害，对方（或对方的召唤物）也不会被自己的召唤物当作攻击目标。

本模组不添加任何新物品、方块或法术，只对召唤物的目标选取逻辑做最小侵入式修正（通过 Mixin 注入 `IMagicSummon#isAlliedHelper`），详细设计原理见 [PLAN.md](PLAN.md)。

### 前置模组 / 版本要求

| 模组 | 要求版本 | 说明 |
|---|---|---|
| Minecraft | 1.21.1 | |
| NeoForge | 21.1.235 | |
| [Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) | 1.21.1-3.16.2 及以上 | 必需，`required` 依赖，缺失会导致加载失败 |
| [FTB Teams](https://www.curseforge.com/minecraft/mc-mods/ftb-teams) | 2101.1.10 及以上 | 必需，`required` 依赖，缺失会导致加载失败 |

Iron's Spells 与 FTB Teams 各自的间接依赖（`geckolib`、`curios`、`playeranimator`、`irons_lib`、`architectury`、`ftblibrary`）需按它们各自的要求安装，本模组不重复声明。

### 配置

唯一开关 `enableTeamSummonProtection`（默认 `true`），关闭后行为完全回退到原版 Iron's Spells 逻辑，可在配置文件或配置界面中修改，无需卸载模组。

### 关于本项目

本模组的代码由 AI（Claude Code）在用户指导下生成和编写。

### License

GPL-3.0-or-later
