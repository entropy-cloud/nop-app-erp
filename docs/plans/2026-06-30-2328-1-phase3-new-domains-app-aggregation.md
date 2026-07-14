# 2026-06-30-2328-1 Phase 3 收尾：8 个新业务域接入聚合 app（app-erp-all）

> Plan Status: completed
> Last Reviewed: 2026-07-01
> Source: `docs/backlog/crud-roadmap.md` Milestone 3（8 个新业务域）
> Related: `docs/plans/2026-06-30-2328-2-phase4-crud-smoke-tests.md`（本计划解除其阻塞）、`docs/plans/01-product-grade-erp-model-overhaul.md`（Deferred「nop-cli 代码生成」的延续）
> Audit: required

## Current Baseline

**项目阶段**（实时仓库核实，2026-06-30）：post-codegen。根 `pom.xml` 聚合 **19 个 reactor 模块**（18 个 `module-<domain>` + `app-erp-all`，子模块链合计 ~145 个 reactor 模块 / 146 个 pom.xml）。`module-*/model/*.orm.xml` 共 **18 域 / 279 实体**（原 10 域 153 + 新 8 域 126）。实体数按各域**自有**实体计（= 非生成 view.xml 页数，与 `crud-roadmap.md` Phase 1-3 表一致）；ORM 中 `<entity name="..."/>` 形式的跨域引用（如 finance.orm.xml 引用 ErpMdSubject）不计入。

**8 个新业务域 codegen 产物已完整落地**（实时核实）：

| 域 | 目录 | app 模块 own artifactId | web action-auth（在 src 树中） | 自有实体 | 页面 page.yaml（已生成，在 src 树中） |
|---|---|---|---|---|---|
| crm | `module-crm` | `app-erp-crm-app` | `erp/crm/auth/erp-crm.action-auth.xml` | 34 | 68 |
| customer-service | `module-cs` | `app-erp-cs-app` | `erp/cs/auth/erp-cs.action-auth.xml` | 16 | 32 |
| human-resource | `module-hr` | `app-erp-hr-app` | `erp/hr/auth/erp-hr.action-auth.xml` | 28 | 56 |
| aps | `module-aps` | `app-erp-aps-app` | `erp/aps/auth/erp-aps.action-auth.xml` | 6 | 12 |
| contract | `module-contract` | `app-erp-contract-app` | `erp/ct/auth/erp-ct.action-auth.xml` | 15 | 30 |
| drp | `module-drp` | `app-erp-drp-app` | `erp/drp/auth/erp-drp.action-auth.xml` | 7 | 14 |
| logistics | `module-logistics` | `app-erp-logistics-app` | `erp/log/auth/erp-log.action-auth.xml` | 7 | 14 |
| b2b | `module-b2b` | `app-erp-b2b-app` | `erp/b2b/auth/erp-b2b.action-auth.xml` | 13 | 26 |

> 注：目录短名与 web 资源短名一致（cs/hr/ct/log），但 own artifactId 用完整域名（`app-erp-contract-app` / `app-erp-logistics-app`）。此差异经逐 pom 核实，wiring 时须精确匹配。原 10 域自有实体（22/15/17/13/17/10/13/23/11/12=153）同样以非生成 view.xml 计数核实。

**剩余差距（实时核实，这是本计划范围）**：

1. **`app-erp-all/pom.xml` 仅依赖原 10 域 `-app`**（`app-erp-{master-data,purchase,sales,inventory,finance,assets,projects,manufacturing,quality,maintenance}-app`）。8 个新域的 `-app` 模块**不是** `app-erp-all` 的依赖 → 新域的 beans/service/web 资源**未进入可部署 app**。
2. **`app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml` 的 `x:extends` 仅合并原 10 域**（`/erp/{md,pur,sal,inv,fin,ast,prj,mfg,qa,mnt}/auth/erp-*.action-auth.xml`）。8 个新域菜单**未聚合** → 运行时菜单不含新域。
3. **roadmap 漂移**：`crud-roadmap.md` Phase 3 标记 8 域 `页面 ❌`，但页面**实际已生成**（存在于 src 树中，但**未 git 跟踪**——与下述工作树状态同属未提交生成产物；roadmap「页面」列按其规则跟踪的是「生成」而非「提交」，故翻 ❌→✅ 仍正确）。真实缺口是 app 聚合 wiring，而非页面生成。此为 owner-doc 漂移（指南规则 13，不可降级为 follow-up）。
4. **基线索引漂移**：`docs/context/project-context.md`（称「10 域 ORM 模型 145 实体」「82 模块」「11 个模块」）与 `docs/context/codebase-map.md`（「根 pom 列出 11 个模块」「ORM 模型清单 10 域 × 145 实体」）均陈旧，未反映 18 域 / 279 实体 / ~145 reactor 模块（146 pom）。

**工作树状态（风险）**：`git status` 显示 `module-aps`、`module-assets`、`module-crm` 等存在大量**未提交的 regen/生成产物**（`deploy/sql/*`、`_app.orm.xml`、`_service.beans.xml`、`_gen/_*.java`、`_templates/_*.json`，以及 8 新域的 `view.xml`/`page.yaml`——经核实 crm web src 有 0 个 view.xml/page.yaml 被 git 跟踪，而 master-data 有 82 个）。这些是前序 codegen/i18n 改造与 8 新域生成的增量产物，未提交。本计划构建验证前须先确认/处置这些改动，否则脏工作树可能掩盖或引入构建错误。

## Goals

- **`app-erp-all` 聚合全部 18 域**：`pom.xml` 增加 8 个新域 `-app` 依赖；`app.action-auth.xml` 的 `x:extends` 增加 8 个新域 action-auth，使新域菜单进入运行时站点 `main`。
- **构建验证通过**：`mvn clean install -DskipTests` 在 `app-erp-all`（含 18 域）全绿，Quarkus 构建成功产出 runner。
- **菜单可加载无冲突**：18 域 action-auth 合并到同一 `site=main` 时无 resource id 冲突（`x:override` 合并语义正确）。
- **roadmap 与基线索引对齐真实状态**：纠正 Phase 3「页面 ❌ / 菜单 ❌」漂移（页面已生成、菜单本计划聚合），更新 Phase 3 状态至 `done`；更新 `project-context.md` / `codebase-map.md` 的域数/实体数/模块数为 18 域 / 279 实体 / ~145 reactor 模块（146 pom）。

## Non-Goals

- **不修改任何 `model/*.orm.xml`、`.api.xml`**（保护区域，`project-context.md` AI 阻塞条件）。
- **不写/不定制 BizModel、xbiz、view.xml、page.yaml 业务逻辑**——本计划只做 app 聚合 wiring + 验证。CRUD 页面已生成，业务深化属后续 roadmap。
- **不重新生成 codegen**（不跑 `nop-cli gen`；按 `project-context.md` 增量重新生成仅用于模型变更，本计划无模型变更）。
- **不做 CRUD 冒烟测试**（属 `2026-06-30-2328-2-phase4-crud-smoke-tests.md`）。
- **不提交未提交的 regen 产物**（除非人工批准；本计划在处置项中显式标记为需人工确认，不擅自 commit）。
- **不新增种子数据 / app-erp-seed 模块**（属 Phase 4 测试范畴）。

## Task Route

- Type: `architecture change`（`app-erp-all` 是部署入口契约，影响全部 18 域的可用性；`app.action-auth.xml` 是 codebase-map 标记的脆弱文件）。
- Owner Docs: `docs/backlog/crud-roadmap.md`（Phase 3）、`docs/context/project-context.md`、`docs/context/codebase-map.md`、`docs/design/roles-and-permissions.md`（菜单/权限模型）、平台 `../nop-entropy/docs-for-ai/02-core-guides/`（delta/聚合机制）。
- Skill Selection Basis: `Skill: none`。本计划是 app 聚合 wiring + 构建验证，`docs/skills/README.md` 现有技能均为审计/重构方法，无 wiring 技能匹配。独立草案审查与结束审计阶段使用 `plan-audit-prompt.md` / `closure-audit-prompt.md`。

## Infrastructure And Config Prereqs

- 构建依赖 `nop-entropy` 2.0.0-SNAPSHOT 父 POM 已在本地 Maven 仓库（`project-context.md` 已确认可构建）。
- **脏工作树处置**：`module-aps`/`module-assets` 等的未提交 regen 产物须先由人工确认是否提交/暂存（执行时第一项处理）。回滚策略：wiring 仅改 2 个文件（`app-erp-all/pom.xml`、`app-erp-all/.../app.action-auth.xml`），git 可单文件 revert；不触碰模型与生成器。

## Execution Plan

### Phase 1 - app-erp-all 依赖与菜单聚合 wiring

Status: completed
Targets: `app-erp-all/pom.xml`、`app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`
Skill: none

- Item Types: `Add`
- Prereqs: 无

- [x] `Add`：`app-erp-all/pom.xml` `<dependencies>` 增加 8 个新域 `-app` 依赖（groupId `io.nop.app`，version `1.0-SNAPSHOT`，own artifactId 精确匹配：`app-erp-crm-app` / `app-erp-cs-app` / `app-erp-hr-app` / `app-erp-aps-app` / `app-erp-contract-app` / `app-erp-drp-app` / `app-erp-logistics-app` / `app-erp-b2b-app`）。置于原 10 域 `-app` 依赖块之后、系统级模块依赖之前，加注释「新增 8 业务域 app(2026-06-30)」。
  - Skill: none
- [x] `Add`：`app.action-auth.xml` 的 `x:extends` 属性追加 8 个新域 action-auth 路径（精确短名）：`/erp/crm/auth/erp-crm.action-auth.xml`、`/erp/cs/auth/erp-cs.action-auth.xml`、`/erp/hr/auth/erp-hr.action-auth.xml`、`/erp/aps/auth/erp-aps.action-auth.xml`、`/erp/ct/auth/erp-ct.action-auth.xml`、`/erp/drp/auth/erp-drp.action-auth.xml`、`/erp/log/auth/erp-log.action-auth.xml`、`/erp/b2b/auth/erp-b2b.action-auth.xml`。文件顶部注释「合并全部 10 业务域」更新为「18 业务域」。
  - Skill: none

Exit Criteria:

> 本阶段交付 wiring 改动（2 文件），使 18 域成为 app-erp-all 的聚合范围。

- [x] `app-erp-all/pom.xml` 含 8 个新域 `-app` 依赖，artifactId 与各域 pom own artifactId 逐一对齐（无 `-app` 缺失/多写）
- [x] `app.action-auth.xml` `x:extends` 含 8 个新域路径，短名（cs/hr/ct/log）与各域 web 实际资源路径逐一核对无误
- [x] 两文件 XML well-formed（`xmllint --noout` 通过）

### Phase 2 - 构建验证 + 菜单合并无冲突

Status: completed
Targets: `app-erp-all/`（构建产物）、运行时 action-auth 合并结果
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] `Fix`（前置处置）：确认 `module-aps`/`module-assets` 等未提交 regen 产物状态——若为合法 codegen 增量，请求人工确认提交；若为实验残留，stash/清理。处置前不进入构建。记录处置结论（提交 hash 或 stash 名）于本阶段。
  - Skill: none
  - 处置结论：合法 codegen 增量产物（`_` 前缀 / `_gen/` 目录 / sql / beans），属 18 域 codegen 正常输出。未获人工提交批准（自主执行），保留在工作树供构建验证。提交决策 deferred 至「未提交 regen 产物的人工提交」后续处理。
- [x] `Proof`：执行 `mvn clean install -DskipTests`（在 `app-erp-all`，`-am` 传递构建 18 域依赖链）至 BUILD SUCCESS。记录耗时与模块数（预期 ~146 模块 / 18 域 + app-erp-all）。
  - Skill: none
  - 证据：根 reactor `mvn clean install -DskipTests` BUILD SUCCESS，146 reactor 模块（含 18 域 × 8 子模块 + app-erp-all + 根），耗时 01:16 min。Quarkus 构建产出 `app-erp-all/target/quarkus-app/quarkus-run.jar`。（注：因各域 `maven.install.skip=true`，必须从根 reactor 构建，`-pl app-erp-all -am` 无法解析依赖。）
- [x] `Proof`：验证菜单合并无 resource id 冲突——通过启动应用加载 `app.action-auth.xml`，或用平台工具/单测加载 merged action-auth，确认 18 域 TOPM/SUBM resource id 在 `site=main` 内无重复（`x:extends` 合并 + 现有 `x:override=remove` 规则未误删新域菜单）。若发现冲突，记录冲突 id 并在域级 action-auth 调整 orderNo/id 前缀（不删菜单）。
  - Skill: none
  - 证据：新增单测 `app-erp-all/src/test/java/io/nop/app/all/auth/TestAppActionAuthMerge.java`，使用 `DslNodeLoader`（平台运行时合并菜单的同一机制）执行 18 域 + 系统模块 `x:extends` 节点级合并。测试 PASS：全部 18 域 TOPM id（erp-md/pur/sal/inv/fin/ast/prj/mfg/qa/mnt/crm/cs/hr/aps/ct/drp/log/b2b）均出现在合并结果 `site=main`，零 resource id 重复。静态校验独立确认 415 个 resource id 跨 18 域 + app.action-auth.xml 无重复。

Exit Criteria:

> 本阶段证明 wiring 后 18 域 app 可构建且菜单可合并。完整仓库验证归 Closure Gates，此处只验证 wiring 直接交付的可观察结果。

- [x] `app-erp-all` `mvn clean install -DskipTests` BUILD SUCCESS（含 18 域）
- [x] merged `app.action-auth.xml` 加载无 resource id 冲突；新域菜单出现在 `site=main`（grep 验证新域 TOPM id 存在于合并结果，或启动日志无冲突报错）
- [x] 脏工作树 regen 产物处置结论已记录

### Phase 3 - 基线与索引对齐（owner-doc 漂移纠正）

Status: completed
Targets: `docs/backlog/crud-roadmap.md`、`docs/context/project-context.md`、`docs/context/codebase-map.md`
Skill: none

- Item Types: `Fix`
- Prereqs: Phase 2（构建绿后更新状态才为真）

- [x] `Fix`：`crud-roadmap.md` Phase 3 表 8 域 `页面 ❌`→`✅`、`菜单 ❌`→`✅`（页面已生成提交、菜单本计划聚合到 app-erp-all，Phase 1-2 已证），Phase 3 备注「action-auth 已创建但页面路径尚不存在」更新为「页面已生成；app-erp-all 聚合 wiring 已完成」，Phase 3 整体状态 `planned`→`done`。
  - Skill: none
- [x] `Fix`：`docs/context/project-context.md` 将「10 域 ORM 模型 145 实体」「82 模块」「1096 个 Java 文件」更新为 18 域 / 279 实体 / ~145 reactor 模块（146 pom）（Java 文件数待 Phase 2 构建后由实际值填入），「当前项目阶段」段对齐 post-codegen 18 域。
  - Skill: none
  - 实际值：1721 个 Java 文件（2026-07-01 核实）；146 reactor 模块 / 146 pom.xml。
- [x] `Fix`：`docs/context/codebase-map.md` 「根 pom 列出 11 个模块」→「19 个 reactor 模块（18 module-* + app-erp-all）」；「ORM 模型清单 10 域 × 145 实体」表**追加 8 新域行**（域/路径/实体数/字典命名空间/最后验证 2026-06-30），**并纠正陈旧的原 10 域行**（master-data 20→22、finance 13→17、manufacturing 21→23、及其它与 279 总数不符的行），「入口点」表实体数与置信度同步，标题改为「18 域 × 279 实体」。
  - Skill: none

Exit Criteria:

> 本阶段使 roadmap 与上下文索引反映 18 域真实状态。owner-doc 对齐是计划级义务（指南执行时规则 6）。

- [x] `crud-roadmap.md` Phase 3 状态为 `done`，页面列与菜单列全部 `✅`，备注对齐
- [x] `project-context.md` 与 `codebase-map.md` 域数/实体数/模块数与实时仓库一致（18 域 / 279 实体 / ~145 reactor 模块；grep 验证无「10 域」「82 模块」「11 个模块」「145 实体」陈旧残留误述）

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（ses_0e6d6fbc2ffefJrIdJ4yXqeOun，独立 general 子代理，研究型）— 核心 staleness 主张（页面已生成、真实缺口=app-erp-all 聚合）经实时核实为 TRUE；其余基线主张全部核实属实。1 项阻塞 B1：实体计数错误（原 10 域 145→应为 153、总数 271→应为 279），系误抄 codebase-map 陈旧值而非重计；执行 Phase 3 会写入「仍错误」的 271，违反 Rule 1。3 项非阻塞：N1 evidence 表 view.xml 列为真实值一半（page.yaml 列正确，已据此重构表）；N2 roadmap `菜单` 列未翻 ✅（本计划交付物即菜单聚合，已补翻）；N3「82 模块→146 pom」指标口径（已改为「~145 reactor 模块 / 146 pom」）。迭代 1 已修订：实体计数全部改为 153/126/279，Phase 3 codebase-map 项扩展为「追加 8 行 + 纠正原 10 域陈旧行」。
- Independent draft review iteration 2: **acceptable / consensus**（ses_0e6ca7126ffeM8lIkLCrKfnREc，独立 general 子代理）— 迭代 1 的 B1 实体计数已修订且独立重核正确（153/126/279，非生成 view.xml 计数核实；codebase-map 项含纠正原 10 域陈旧行）。审查发现 2 处遗漏的 `271`（基线 gap#4 与 Closure 模板），已改 279；并采纳非阻塞建议：新域页面「已生成但未 git 跟踪」（crm web src 0 跟踪 vs master-data 82），将「已提交到 src」措辞改为「已生成，在 src 树中」、把 view.xml/page.yaml 纳入工作树未提交产物清单（roadmap「页面」列按规则跟踪生成故翻 ❌→✅ 仍正确）。至此 14 条规则全部 PASS，无阻塞，可翻 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划含 wiring + 构建，在结束时运行一次完整仓库验证。

- [x] 范围内行为完成：app-erp-all 聚合 18 域（pom + 菜单）且构建绿
- [x] 相关文档对齐：crud-roadmap Phase 3 done、project-context/codebase-map 反映 18 域
- [x] 已运行验证：`mvn clean install -DskipTests`（app-erp-all，含 18 域）BUILD SUCCESS；`xmllint --noout` 两 wiring 文件通过
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、`docs/logs/` 一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为人工门控占位符
  - 独立结束审计 ses_0e6a241ceffeux855pshQjLQsx（general 子代理）2026-07-01：**PASS**。全部 Phase 1/2/3 主张经实时仓库核实为 TRUE：pom 8 依赖（artifactId 逐一核对）、app.action-auth.xml 8 路径（文件存在性核实）、xmllint 通过、TestAppActionAuthMerge 用 DslNodeLoader 验证 18 域 TOPM、415 resource id 无重复、crud-roadmap Phase 3 done、project-context/codebase-map 对齐 18 域/279 实体。非阻塞建议：deferred regen 产物含 7 个 model/*.orm.xml 的 notGenCode 微调（属前序 codegen 改造，非本计划 wiring 范围），建议后续显式裁决。
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 8 新域 BizModel 业务逻辑深化

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划只做 CRUD app 聚合 wiring；BizModel 仍是空壳 `CrudBizModel<T>`，业务规则深化属 `implementation-roadmap.md`（product-scope post-codegen 阶段重点）。
- Successor Required: yes（触发条件：按 implementation-roadmap 顺序深化各域）

### CRUD 冒烟测试

- Classification: `moved to explicit successor ownership`
- Why Not Blocking Closure: 属 `2026-06-30-2328-2-phase4-crud-smoke-tests.md`，本计划为其解除「8 域未进 app」阻塞。
- Successor Required: yes（紧邻后续计划）

### 未提交 regen 产物的人工提交

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划在 Phase 2 处置项中确认其状态并记录；是否提交属人工决策（保护区域）。若处置为 stash，工作树恢复干净即可构建验证。
- Successor Required: yes（触发条件：人工审查 regen diff 后决定提交/丢弃）

## Closure

Status Note: 已执行（2026-07-01）。18 域聚合 app 构建绿（`mvn clean install -DskipTests` BUILD SUCCESS，146 reactor 模块，Quarkus `quarkus-run.jar` 产出）；菜单合并无冲突（单测 `TestAppActionAuthMerge` PASS：18 域 TOPM id 全部出现于 `site=main`，零 resource id 重复）；roadmap Phase 3 done；上下文索引对齐 18 域 / 279 实体 / 146 reactor 模块（146 pom）/ 1721 Java 文件。

Closure Audit Evidence:

- Executor: opencode 主代理（executor，非独立审计者）
- Wiring 改动（2 文件）:
  - `app-erp-all/pom.xml`：新增 8 个新域 `-app` 依赖（`app-erp-{crm,cs,hr,aps,contract,drp,logistics,b2b}-app`），artifactId 经各域 pom 逐一核实对齐
  - `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`：`x:extends` 追加 8 个新域 action-auth 路径（cs/hr/ct/log 短名核实），注释 10→18 业务域
  - 两文件 `xmllint --noout` 通过
- 测试改动（1 文件）:
  - `app-erp-all/src/test/java/io/nop/app/all/auth/TestAppActionAuthMerge.java`：使用 `DslNodeLoader`（运行时合并同一机制）验证 18 域 `x:extends` 合并；手动初始化核心链（Reflection→XLang→VFS→RegisterModel），跳过 IoC/ORM 以隔离已知 `ErpProProject` 类加载问题（不在本计划范围）
- 构建证据: 根 reactor `mvn clean install -DskipTests` BUILD SUCCESS（146 模块，01:16 min）
- 菜单合并证据: `mvn test -Dtest=TestAppActionAuthMerge` PASS（18 域 TOPM 全到、零 id 重复）
- 脏工作树处置: ~885 个未提交 regen 产物确认为合法 codegen 增量，保留工作树供构建；提交决策 deferred
- owner-doc 对齐: `crud-roadmap.md` Phase 3 done、`project-context.md` 18 域/279 实体、`codebase-map.md` 18 域表+纠正陈旧行


- **Independent Closure Audit (2026-07-14-1449-1 batch)** — Auditor: independent closure audit subagent (fresh session, cold-replay, 2026-07-14). Verdict: **PASS**. All Phase 1/2/3 exit criteria verified against live repo: pom.xml 8 new domain -app deps present with matching artifactIds; app.action-auth.xml x:extends 8 paths with correct short names; TestAppActionAuthMerge non-hollow (DslNodeLoader + 18 TOPM id assertions + duplicate detection); crud-roadmap Phase 3 done; no stale context refs. Deferred items honestly classified. (Audit dispatch ref: docs/plans/2026-07-14-1449-1-closure-audit-consistency-remediation-batch.md Phase 2; this evidence block appended by Phase 3 backfill.)
Follow-up:

- CRUD 冒烟测试（见紧邻后续计划 `2026-06-30-2328-2-phase4-crud-smoke-tests.md`）
- 8 新域 BizModel 业务深化（见 implementation-roadmap）
- 未提交 regen 产物的人工提交（触发条件：人工审查 regen diff 后决定提交/丢弃）
- `ErpProProject` 类加载问题（projects 域，阻碍完整 IoC/ORM 启动；不在本计划范围，需后续单独排查）
