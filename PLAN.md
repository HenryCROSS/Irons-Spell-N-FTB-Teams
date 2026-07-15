# IronsSpellsNFTBteams 设计规划文档

> 目标：让 [Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) 的**法术召唤物**（骷髅/僵尸/Vex/极地熊等）在判断攻击目标时尊重 **FTB Teams** 的队伍关系——同队玩家之间即使互相攻击，也不会被对方（或对方召唤物）的召唤物当作目标。
>
> 环境：NeoForge `1.21.1`（neo `21.1.235`），Java 21。
> 本模组定位：**桥接 / addon 模组**，不添加任何物品、方块、法术，只做行为修正。

---

## 1. 依赖关系确认

### 1.1 三方模组版本（已在客户端 mods 目录与源码中核实）

| 模组 | modId | 版本 | 来源 |
|---|---|---|---|
| Iron's Spells 'n Spellbooks | `irons_spellbooks` | `1.21.1-3.16.2` | `C:\Users\henry\gitFetch\irons-spells-n-spellbooks`（源码，group `io.redspace`）与客户端 jar 完全一致 |
| FTB Teams | `ftbteams` | `2101.1.10` | 客户端 `mods\[A] [FTB 团队] ftb-teams-neoforge-2101.1.10.jar`（仅有编译好的 jar，无源码，已反编译其 API 包确认签名） |

Iron's Spells 自身的 `neoforge.mods.toml` 要求以下**必须**存在的三方库，客户端 mods 目录已逐一核实存在：`neoforge`、`geckolib`（`geckolib-neoforge-1.21.1-4.9.2.jar`）、`playeranimator`（`player-animation-lib-forge-2.0.4+1.21.1.jar`）、`curios`（`curios-neoforge-9.5.1+1.21.1.jar`）、`irons_lib`（`irons_lib-1.21.1-2.1.0.jar`）。
FTB Teams 要求 `architectury`（`architectury-13.0.8-neoforge.jar`）与 `ftblibrary`（`ftb-library-neoforge-2101.1.32.jar`），同样已确认存在。**因此客户端环境本身没有缺依赖的风险**，本模组只需老老实实声明对 `irons_spellbooks` 与 `ftbteams` 的依赖即可。

### 1.2 本模组的依赖声明（`neoforge.mods.toml`）

在 `[[dependencies.${mod_id}]]` 中新增两条，`type = "required"`（没有这两个模组，本模组的存在没有意义，直接崩溃提示比静默失效更好）：

```toml
[[dependencies.${mod_id}]]
    modId = "irons_spellbooks"
    type = "required"
    versionRange = "[${irons_spellbooks_version},)"
    ordering = "AFTER"
    side = "BOTH"

[[dependencies.${mod_id}]]
    modId = "ftbteams"
    type = "required"
    versionRange = "[${ftbteams_version},)"
    ordering = "AFTER"
    side = "BOTH"
```

`ordering = "AFTER"` 只影响 `@Mod` 构造顺序（对我们意义不大，因为逻辑都在 AI tick 时才触发），但写上是良好实践、也便于以后排查加载顺序问题。

### 1.3 编译期依赖（`build.gradle`）

FTB Teams 没有公开发布的、可确认的 Maven 坐标（官方仓库地址未经核实，不确定就不瞎写）；Iron's Spells 虽然发布到 `https://code.redspace.io/releases`（`io.redspace:irons_spellbooks:1.21.1-3.16.2`，源码 `build.gradle` 里能看到，甚至专门产出了一个只含 `api` 包的 `:api` classifier jar），但为了**离线可靠、版本 100% 对齐客户端**，采用最稳妥的方式：

直接把客户端正在用的两个 jar 复制进项目的 `libs/` 目录，作为 `compileOnly` 文件依赖（`compileOnly` 保证不会被打进最终产物，也不会被下游误传递）：

```
libs/irons_spellbooks-1.21.1-3.16.2.jar   (来自客户端 mods 目录)
libs/ftb-teams-neoforge-2101.1.10.jar     (来自客户端 mods 目录)
```

```groovy
repositories {
    flatDir { dir 'libs' }
}

dependencies {
    compileOnly files('libs/irons_spellbooks-1.21.1-3.16.2.jar')
    compileOnly files('libs/ftb-teams-neoforge-2101.1.10.jar')
}
```

以后升级依赖版本时，只需替换 `libs/` 里的 jar 并同步改文件名即可，不涉及网络仓库是否可达的问题。（如果之后想改成 Maven 方式，Iron's Spells 一侧可以用 `io.redspace:irons_spellbooks:<version>:api`，FTB Teams 一侧需要先自行确认其官方 Maven 地址。）

---

## 2. 问题分析：召唤物的目标选取到底怎么工作

以下结论全部基于**直接阅读源码**（`irons-spells-n-spellbooks` 仓库）以及**反编译本项目自己 Gradle 缓存里的已反混淆 `TargetingConditions.java`**（`build/moddev/artifacts/neoforge-21.1.235-merged.jar` 内置的官方 Mojang 映射源码），不是猜测。

### 2.1 关键类型：`IMagicSummon`

`io.redspace.ironsspellbooks.entity.mobs.IMagicSummon` 是所有法术召唤物（`SummonedZombie`/`SummonedSkeleton`/`SummonedVex`/`SummonedPolarBear`/`SummonedHorse` 等）实现的接口，核心方法：

```java
default Entity getSummoner() { return SummonManager.getOwner((Entity) this); }

default boolean isAlliedHelper(Entity entity) {
    var owner = getSummoner();
    if (owner == null) return false;
    if (entity instanceof IMagicSummon magicSummon) {
        var otherOwner = magicSummon.getSummoner();
        return otherOwner != null && (owner == otherOwner || otherOwner.isAlliedTo(otherOwner));
    } else if (entity instanceof OwnableEntity tamableAnimal) {
        var otherOwner = tamableAnimal.getOwner();
        return otherOwner != null && (owner == otherOwner || otherOwner.isAlliedTo(otherOwner));
    }
    return false; // <- 关键：entity 是「普通玩家」时，永远返回 false！
}
```

每个具体召唤物类都这样覆写 `isAlliedTo`：

```java
@Override
public boolean isAlliedTo(Entity pEntity) {
    return super.isAlliedTo(pEntity) || this.isAlliedHelper(pEntity);
}
```

**这里有一个明确的空白**：`isAlliedHelper` 只处理「目标是另一个召唤物」或「目标是被驯服的宠物」这两种情况，**完全没有处理「目标就是一名普通玩家」的情况**。也就是说，哪怕将来两名玩家在同一个 FTB 队伍里，只要没有额外逻辑，召唤物依然会把对方玩家当成合法目标——这正是需求里要补上的空白，不是我们凭空加需求。

### 2.2 目标选取用到 `isAlliedTo` 的确切位置

所有召唤物的 `targetSelector` 都是同一套五个 goal（`SummonedZombie`/`Skeleton`/`Vex`/`PolarBear` 完全一致）：

```java
targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(this, this::getSummoner));
targetSelector.addGoal(4, new GenericHurtByTargetGoal(this, e -> e == getSummoner()).setAlertOthers());
targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(this, this::getSummoner));
```

- **Goal 1/2/3** 都在 `canUse()` 末尾调用 `this.canAttack(candidate, TargetingConditions.DEFAULT)`。
- **Goal 4** 额外有一个直接的 `livingentity.isAlliedTo(mob)` 前置判断，但那是「攻击者对召唤物」方向（见 2.3 的方向性说明），随后同样会走 `canAttack(...)`。
- **Goal 5**（保护主人）不走 `canAttack`，而是直接扫描「目标正是主人（或主人的召唤物）」的敌对 `Mob`。它不会响应玩家对玩家的近战伤害（`Player` 不是 `Mob`，没有 `getTarget()`），只会响应「别的召唤物/生物在打你主人」——只要 Goal 1/2/3/4 已经正确地不把队友当目标，Goal 5 就永远不会被队友触发，**无需单独处理**。

结论：**只要修好 `canAttack()` 最终查询的那次 `isAlliedTo`，五个 goal 全部覆盖到。**

### 2.3 反编译确认：`TargetingConditions.test()` 到底查了谁的 `isAlliedTo`

`net/minecraft/world/entity/ai/targeting/TargetingConditions.java`（反混淆源码，直接摘录关键行）：

```java
public boolean test(@Nullable LivingEntity attacker, LivingEntity target) {
    ...
    if (this.isCombat && (!attacker.canAttack(target)
                        || !attacker.canAttackType(target.getType())
                        || attacker.isAlliedTo(target))) {
        return false;
    }
    ...
}
```

而 `TargetGoal.canAttack(target, conditions)` 内部把 `this.mob`（也就是**召唤物自己**）作为 `attacker` 传进去。也就是说 `canAttack()` 最终调用的是 **`召唤物.isAlliedTo(目标)`**——虚方法分派会走到每个 `SummonedX` 类覆写的版本，也就是会经过 `isAlliedHelper`。**这正是我们唯一需要打补丁的地方**，不需要碰 `Player`、不需要碰 `TargetGoal`、更不需要碰 NeoForge/vanilla 任何一行代码。

（附带确认：`Entity#isAlliedTo(Entity)` 官方实现是 `this.isAlliedTo(entity.getTeam())`，即基于计分板 Team 对象，和 FTB Teams 的队伍系统完全是两套东西——这也解释了为什么现状下 FTB 同队玩家不会被 `super.isAlliedTo` 判定为盟友。）

### 2.4 `FTB Teams` 提供的查询 API

反编译 `ftb-teams-neoforge-2101.1.10.jar` 的 `dev.ftb.mods.ftbteams.api` 包，确认公开接口：

```java
public interface TeamManager {
    boolean arePlayersInSameTeam(UUID a, UUID b); // 就是这个！
    Optional<Team> getTeamForPlayerID(UUID playerId);
    ...
}
public final class FTBTeamsAPI {
    public static FTBTeamsAPI.API api();
    public interface API {
        boolean isManagerLoaded();
        TeamManager getManager();
        ...
    }
}
```

`TeamManager#arePlayersInSameTeam(UUID, UUID)` 已经是现成的、语义完全匹配的一次调用，不需要自己遍历队伍成员。`isManagerLoaded()` 用来防御「世界还没加载完 / 只在客户端」等情况。

---

## 3. 设计方案

### 3.1 核心原则

- **纯函数优先**：所有判断逻辑写成无副作用、可单独推理的静态方法，输入输出明确；`Mixin` 只做「翻译胶水」，不写业务逻辑。
- **单一注入点**：只 mixin 一个方法（`IMagicSummon#isAlliedHelper`），覆盖全部五个 targetSelector goal，不逐个 mixin 每个 `SummonedX` 类。
- **防御式默认关闭**：任何一步解析失败（找不到主人、FTB 队伍系统未加载、目标不是玩家等）都直接返回 `false`（不通过，退回原版行为），绝不因为异常导致目标判定出错甚至崩服。
- **可配置**：一个总开关，方便玩家在不修改代码的情况下临时关闭本功能来排查问题。

### 3.2 模块划分与职责

```
cross.ironsspellsnftbteams
├─ IronsSpellsNFTBteams.java          # @Mod 入口，只做注册装配，不写业务逻辑
├─ IronsSpellsNFTBteamsClient.java    # 客户端入口，只挂配置界面
├─ Config.java                        # 唯一配置项：enableTeamSummonProtection
│
├─ team/
│   └─ FtbTeamAllegiance.java         # 纯函数：对 FTB Teams API 的最小只读封装
│
├─ summon/
│   ├─ SummonOwnerResolver.java       # 纯函数：Entity -> 「真正负责任的玩家 UUID」
│   └─ SummonTeamProtection.java      # 纯函数：组合上面两者 + 读配置，给出最终布尔判定
│
└─ mixin/
    └─ IMagicSummonMixin.java         # 唯一的 Mixin，接口默认方法注入，只做转发
```

依赖方向单向、无环：`mixin` 依赖 `summon`，`summon` 依赖 `team` 和 `Config`；`team`/`summon` 两个包互相之间、以及它们与 `mixin` 之间都不反向依赖，改动任何一层不影响其余层的可测试性。

### 3.3 关键函数签名

```java
// team/FtbTeamAllegiance.java
public final class FtbTeamAllegiance {
    private FtbTeamAllegiance() {}

    /** 两个玩家是否属于同一个 FTB 队伍；FTB Teams 未就绪时保守返回 false。 */
    public static boolean arePlayersAllied(UUID playerA, UUID playerB) {
        if (playerA.equals(playerB)) return true;
        if (!FTBTeamsAPI.api().isManagerLoaded()) return false;
        return FTBTeamsAPI.api().getManager().arePlayersInSameTeam(playerA, playerB);
    }
}
```

```java
// summon/SummonOwnerResolver.java
public final class SummonOwnerResolver {
    private static final int MAX_OWNER_CHAIN_DEPTH = 8; // 防止异常数据形成环

    private SummonOwnerResolver() {}

    /** 递归解析「谁该为这个实体的行为负责」：玩家本人 / 法术召唤物的施法者 / 驯服生物的主人。 */
    public static Optional<UUID> resolveResponsiblePlayer(Entity entity) {
        return resolve(entity, 0);
    }

    private static Optional<UUID> resolve(Entity entity, int depth) {
        if (entity == null || depth >= MAX_OWNER_CHAIN_DEPTH) return Optional.empty();
        if (entity instanceof Player player) return Optional.of(player.getUUID());
        if (entity instanceof IMagicSummon summon) return resolve(summon.getSummoner(), depth + 1);
        if (entity instanceof OwnableEntity ownable) return resolve(ownable.getOwner(), depth + 1);
        return Optional.empty();
    }
}
```

```java
// summon/SummonTeamProtection.java
public final class SummonTeamProtection {
    private SummonTeamProtection() {}

    /** summon 是否应当把 potentialTarget 视为「同队盟友，不可选为攻击目标」。 */
    public static boolean shouldTreatAsAlly(Entity summon, Entity potentialTarget) {
        if (!Config.ENABLE_TEAM_SUMMON_PROTECTION.get()) return false;
        if (summon == null || potentialTarget == null) return false;
        if (summon.level().isClientSide()) return false; // AI 只在服务端 tick，双重防御

        var summonOwner = SummonOwnerResolver.resolveResponsiblePlayer(summon);
        var targetOwner = SummonOwnerResolver.resolveResponsiblePlayer(potentialTarget);
        if (summonOwner.isEmpty() || targetOwner.isEmpty()) return false;

        return FtbTeamAllegiance.arePlayersAllied(summonOwner.get(), targetOwner.get());
    }
}
```

```java
// mixin/IMagicSummonMixin.java
@Mixin(value = IMagicSummon.class, remap = false)
public interface IMagicSummonMixin extends IMagicSummon {
    @Inject(method = "isAlliedHelper", at = @At("HEAD"), cancellable = true)
    private default void ironsspellsnftbteams$checkFtbTeam(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (SummonTeamProtection.shouldTreatAsAlly((Entity) this, entity)) {
            cir.setReturnValue(true);
        }
    }
}
```

三个纯函数每一个都可以脱离 Minecraft 运行时单独用假数据验证逻辑（`resolveResponsiblePlayer` 尤其如此），`Mixin` 里完全没有 if/else 业务分支，出问题时改动面很小。

### 3.4 配置项

```java
public static final ModConfigSpec.BooleanValue ENABLE_TEAM_SUMMON_PROTECTION = BUILDER
        .comment("同 FTB 队伍的玩家之间，Iron's Spells 的法术召唤物是否禁止将对方选为攻击目标")
        .define("enableTeamSummonProtection", true);
```

默认开启（这是本模组存在的唯一目的），出问题时可以直接在配置文件或配置界面关掉，不需要卸载模组排查。

---

## 4. 明确的范围边界（这次不做的事）

- **不改动伤害判定**：`DamageSources.isFriendlyFireBetween(...)`（`irons_spellbooks` 里的另一套独立机制，控制「能不能对自己/盟友召唤物造成伤害」）与本次需求（「会不会被选为攻击目标」）是两回事，现状里玩家之间的友伤已经由原版 `Player#canHarmPlayer`（计分板队伍的 friendlyFire 设置）在管，不属于「FTB 队伍」范畴。如果以后想让「同 FTB 队但计分板允许友伤」时召唤物也不能顺手误伤，可以在 `DamageSources` 上加一个对称的 Mixin 挂载点，复用同一套 `SummonOwnerResolver` / `FtbTeamAllegiance`，是独立的、可选的后续任务。
- **不处理非玩家的召唤者**：例如 `NecromancerEntity`、`FireBossEntity` 等 Boss/野怪自己也会召唤仆从，它们的 `getSummoner()` 解析不到玩家 UUID，`resolveResponsiblePlayer` 会返回空，直接短路为「不是盟友」，行为与现状完全一致，不影响 Boss 战。
- **不处理 `SummonedHorse`**：确认过它没有 `targetSelector`（骑乘用途，没有攻击行为），无需关心。

---

## 5. 已知风险与验证计划

1. **接口默认方法 Mixin 的兼容性**：往接口的 `default` 方法里注入（`@Mixin(value = IMagicSummon.class) interface IMagicSummonMixin extends IMagicSummon`）是 SpongePowered Mixin 支持但相对少见的用法。**如果实测中 Mixin 加载报错**（比如提示接口未正确合并），退化方案是把同一段 `@Inject` 逻辑分别搬到 `SummonedZombie`/`SummonedSkeleton`/`SummonedVex`/`SummonedPolarBear` 四个类的 `isAlliedTo` 方法上（每个类 3 行代码，`SummonTeamProtection` 等纯函数完全不用动），改动量很小。计划里两种方案都记录在案，先尝试方案一。
2. **`isAlliedHelper` 的可见性/是否被内联**：如果编译器/运行时优化导致 `isAlliedHelper` 没有被当作独立虚方法调用（正常 Java 不会），需要用同样的降级方案。
3. **`arePlayersInSameTeam` 的调用频率**：`targetSelector` 的 `canUse()` 每 tick 可能被调用，但 FTB Teams 自己的 `TeamManager` 实现是基于 `Map<UUID, Team>` 的查找（从接口上 `getKnownPlayerTeams()` 的存在可以判断），单次调用是 O(1)～O(小常数)，召唤物数量在正常游玩场景下不会构成性能问题，暂不引入缓存（YAGNI，真出现性能问题再加）。
4. **验证步骤（需要实机测试，本次规划阶段无法在没有完整 Minecraft 客户端的环境里跑起来）**：
   - 两个测试玩家 A、B 建立同一个 FTB 队伍；A 用 Raise Dead 召唤骷髅/僵尸；B 攻击 A（或 A 攻击 B）——召唤物应保持不将 B（或 A）选为目标。
   - A、B 退出同一队伍（或 B 不在任何队伍/在别的队伍）——召唤物应恢复原版行为，正常把 B 当目标。
   - 把 `enableTeamSummonProtection` 设为 `false`——行为应完全回到「打补丁之前」。
   - 打一场 Boss 战（如 Dead King），确认 Boss 自己的召唤物行为不受影响。

---

## 6. 实施顺序

1. `build.gradle` + `neoforge.mods.toml` + `libs/` 依赖接线。
2. `team/FtbTeamAllegiance.java`、`summon/SummonOwnerResolver.java`、`summon/SummonTeamProtection.java`（纯函数，最容易验证的部分先写）。
3. `mixin/IMagicSummonMixin.java` + 注册进 `ironsspellsnftbteams.mixins.json`。
4. `Config.java` 精简为唯一开关；清理模板自带的示例方块/物品/日志代码。
5. `gradlew compileJava` 做编译期验证（Mixin 语法、依赖解析是否正确）；实机测试留给你后续在真实客户端里进行。
