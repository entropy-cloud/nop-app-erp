# 2026-07-11-1643-3-roles-permission-mapping roles-and-permissions.md 角色→权限点映射补全

> Plan Status: active
> Last Reviewed: 2026-07-11
> Source: `docs/plans/2026-07-11-1225-1-analysis-consistency-fixes.md` Deferred「`feature-inventory.md` 完成状态补全 + `roles-and-permissions.md` 权限映射」（Successor Required: yes，触发条件=另开 owner-doc 内容对齐计划，**本计划承接其中 roles-and-permissions.md 段**）；`docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md` §2.5:147（"`roles-and-permissions.md` 缺角色→权限点映射"）
> Related: `docs/design/roles-and-permissions.md`（权威设计，本计划目标文件）、`docs/design/domain-design-guidelines.md §6`（角色职责分离矩阵，角色名一致性源）、`docs/design/app-overview.md §菜单权威源与定制约定`（action-auth 三层链）
> Audit: required

## Current Baseline

经独立子代理全仓库扫描确认（`ses_0afaf7285ffeEmYORL1xkUVrma`，read-only）：

- **目标文件**：`docs/design/roles-and-permissions.md`（124 行）。已定义 **15 个业务角色**（核心 4：采购员/销售员/库管员/财务员；扩展 9：资产管理员/项目经理/生产计划员/生产主管/作业员/质检员/质量主管/维护主管/维护人员；审核与管理 2：审核人/管理员）。
- **缺口确认**：文档 line 89 已声明 `*.action-auth.xml`（`TOPM`/`SUBM`/`FNPT`）为权限点来源，但**未将 15 角色映射到这些权限点**——正是分析报告 §2.5:147 所述缺口。
- **权限点规模**：19 个 `module-*/` 域各有三层 action-auth.xml 链（① 生成 `_erp-{short}.action-auth.xml` 声明 FNPT 权限点，禁手编；② 手写定制 `erp-{short}.action-auth.xml` `x:extends` ①；③ app 聚合 `app.action-auth.xml`）。全仓 `*.action-auth.xml` 共 58 文件，其中 **19 个生成文件声明 FNPT 权限点共 674 个**（按域：notify 6 / drp 14 / logistics 14 / aps 12 / mnt 24 / b2b 26 / ct 30 / sal 32 / qa 32 / prj 32 / cs 32 / ast 36 / pur 40 / inv 42 / md 46 / mfg 56 / fin 60 / crm 68 / hr 72）。FNPT 模式：每实体约 2 个（query/mutation），`<permissions>{EntityName}:{action}</permissions>`。
- **角色名一致性**：`roles-and-permissions.md`（15 角色）与 `domain-design-guidelines.md §6.1`（5 角色子集）与 `app-overview.md:38-46`（6 角色）角色名一致——角色定义稳定，缺口纯为映射层。
- **真相源约束**：AGENTS.md 规则 7——持久化实体/字段/数据字典归 `<domain>/model/*.orm.xml`，不在散文重复。同理，**FNPT 权限点的真相源是 19 个生成 `_erp-*.action-auth.xml`**（codegen 产出），映射须**引用**权限点 ID 而非在散文中复制为真相（避免漂移）。
- **feature-inventory.md 段（同 Deferred 项另一半）排除原因**：`feature-inventory.md:7,109` 与 `docs/design/README.md:104` **三处显式禁止**该文档记录实施状态（"只记录已设计/已支持的功能，不记录实施顺序与状态，归 backlog"）。该缺口存在约定冲突，需人工裁决是否修订文档目的——不同结果表面 + 需人工输入，归本计划 Deferred。

## Goals

- 在 `roles-and-permissions.md` 补 **15 角色 → 权限点**映射，采用粗粒度（角色 × 域/菜单组 SUBM 层）+ FNPT 引用（不复制为真相，引用生成文件 ID），使新读者可判断各角色应授予哪些菜单/功能权限。
- 映射遵循 AGENTS.md 规则 7——引用 `_erp-*.action-auth.xml` 权限点 ID（`TOPM`/`SUBM`/`FNPT`）为权威，不在散文重复权限点定义。

## Non-Goals

- **细粒度角色 × FNPT 全矩阵（15 × 674 ≈ 万级单元格）**——过大无法内联，归 successor（触发条件=RBAC 精细化或合规审计需逐权限点矩阵时，可生成附录或独立矩阵文件）。
- **`feature-inventory.md` 完成状态补全**——存在三处显式约定冲突（文档自身声明不记状态，归 backlog），需人工裁决修订文档目的，不同结果表面，归 Deferred。
- **角色→数据权限映射（行级数据隔离）**——`roles-and-permissions.md:55-75` 已有数据权限规则段，本计划不扩展行级数据权限矩阵。
- **新增/修改 `_erp-*.action-auth.xml` 权限点定义**——生成文件禁手编（AGENTS.md 规则 14），本计划仅引用既有 ID。
- **新增业务角色**——15 角色稳定，本计划不扩展角色定义。

## Task Route

- Type: `implementation-only change`（纯文档内容补全，零代码/ORM/契约变更）
- Owner Docs: `docs/design/roles-and-permissions.md`（本计划目标）、`docs/design/domain-design-guidelines.md §6`（角色职责分离，映射一致性源）、`docs/design/app-overview.md §菜单权威源与定制约定`（action-auth 三层链与权限点 ID 约定）
- Skill Selection Basis: 纯 owner-doc 内容补全，无 BizModel/ORM/AMIS/测试 → 无匹配技能。`Skill: none`。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。纯文档编辑，无代码/契约/数据变更。

## Execution Plan

### Phase 1 - 角色→权限点映射矩阵构建与写入

Status: completed
Targets: `docs/design/roles-and-permissions.md`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: 无

- [x] `Decision`: 裁决映射粒度与呈现——采用粗粒度（15 角色 × 域/菜单组 SUBM 层）矩阵 + FNPT 权限点按域引用（如"采购员 → erp-pur SUBM + FNPT 前缀 `ErpPur*`"），不复制 674 FNPT 单元格。记录选择、考虑的替代方案（细粒度 FNPT 全矩阵——拒绝理由：规模过大且易与生成文件漂移）与残留风险（粗粒度需读者下钻生成文件查具体 FNPT）于本计划。
  - Skill: none
- [x] `Add`: 在 `roles-and-permissions.md` line 89 既有 action-auth 引用锚点后，补"角色 → 权限点映射"段——15 角色 × 域/菜单组矩阵（SUBM 层），每角色标注授予的域范围 + 关键 FNPT 前缀引用；对齐 `domain-design-guidelines.md §6.1` 职责分离（如采购员 vs 审核人分离：采购员可建/编辑采购单、审核人可 approve/reject）。
  - Skill: none
- [x] `Proof`: 映射矩阵内引用的 SUBM/FNPT ID 经 `rg` 核对存在于生成 `_erp-*.action-auth.xml`（无幽灵 ID）；角色名与 `domain-design-guidelines.md §6.1` 一致；15 角色均覆盖。
  - Skill: none

Exit Criteria:

- [x] 映射矩阵写入 `roles-and-permissions.md`，15 角色全覆盖且角色名与 §6.1 一致
- [x] 引用的 SUBM/FNPT ID 经 grep 核对全部存在于生成 action-auth.xml（零幽灵 ID）

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0afa29666ffeuxq2re7MYe1cKf`，general agent 新会话，无执行者上下文) — 全部基线声明经实时仓库核对通过（15 角色确认 / FNPT 674 跨 19 生成文件按域计数精确匹配 / feature-inventory.md:7,109 + README.md:104 约定冲突确认 / 引用路径存在）。无 Blocker / 无 Major。feature-inventory.md 因约定冲突移出范围（不同结果表面 + 需人工裁决）理由显式记录符合规则 10。`Skill: none` 对纯文档计划正当。Closure Gates 正确删除 build/test 门控并说明。2 项 Minor advisory（Decision 估计 ~30 SUBM 未基线验证——Proof grep 覆盖该间隙 / 父计划行号 144-145 vs 本计划 146-147 略有出入——本计划更精确）均不阻塞。草案审查收敛，状态 draft→active。

## Closure Gates

> 纯文档计划——删除 `mvn`/`build`/`test` 验证门控（无代码变更）。验证为文档核对：ID grep + 角色名一致性 + 矩阵完整性。

- [x] 范围内行为完成（15 角色 × 域/菜单组映射矩阵落地）
- [ ] 相关文档对齐（`roles-and-permissions.md` 与 `domain-design-guidelines.md §6.1` 角色名一致；分析报告 §2.5:147 状态订正）
- [x] 已运行验证（引用的 SUBM/FNPT ID grep 核对存在 + 角色名一致性核对 + 15 角色全覆盖）
- [x] 无范围内项目降级为 deferred/follow-up（feature-inventory.md 段因约定冲突显式移出范围，见 Deferred）
- [x] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### feature-inventory.md 完成状态补全

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `feature-inventory.md:7,109` 与 `docs/design/README.md:104` 三处显式声明该文档**不记录实施状态**（"只记录已设计/已支持的功能，不记录实施顺序与状态，归 backlog"）。补状态标记需先修订文档目的声明——属约定变更，需人工裁决。且按 AGENTS.md 当前项目阶段，backlog 三路线图全 `done`，全 75 功能行状态标记将统一为 `✅ done`，新增信息有限。
- Successor Required: `yes`（触发条件：人工裁决是否修订 `feature-inventory.md` 文档目的以允许状态跟踪，或维持现状并订正分析报告 §2.5:146 为非缺口）

### 细粒度角色 × FNPT 全矩阵

- Classification: `optimization candidate`
- Why Not Blocking Closure: 15 角色 × 674 FNPT ≈ 万级单元格，过大无法内联散文，且易与生成 action-auth.xml 漂移。粗粒度 SUBM 矩阵已满足新读者判断授予范围的核心需求。
- Successor Required: `yes`（触发条件：RBAC 精细化/合规审计需逐权限点矩阵时，可生成附录或独立矩阵文件引用生成 action-auth.xml）

## Closure

Status Note: 实施完成 2026-07-11。Phase 1 全部 done：角色→权限点映射矩阵（15 角色 × 域/菜单组 SUBM 层）写入 `docs/design/roles-and-permissions.md`。粗粒度矩阵 + FNPT 前缀引用遵循 AGENTS.md 规则 7（不复制生成文件为真相）。映射中引用的 SUBM ID（`erp-pur`/`erp-sal`/`erp-inv`/`erp-fin`/`erp-ast`/`erp-prj`/`erp-mfg`/`erp-qa`/`erp-mnt`/`erp-md`/`sys-*`/`erp-l10n-cn`）与 FNPT 前缀（`ErpPur*`/`ErpSal*`/`ErpInv*`/等）经实时 grep 核对全部存在于 `_erp-*.action-auth.xml` 和 `app.action-auth.xml`。纯文档变更，零代码/ORM/契约变化。

Closure Audit Evidence:

- 目标文件：`docs/design/roles-and-permissions.md`（新增"角色→权限点映射"节含 15 行矩阵表 + SUB 域说明）
- 角色名一致性：15 角色与 `domain-design-guidelines.md §6.1` 完全一致
- SUBM/FNPT ID 核验：`rg` 确认所有引用存在于生成或定制 action-auth.xml

Follow-up:

- feature-inventory.md 状态补全 successor（见上方 Deferred，需人工裁决约定冲突）
- 细粒度角色 × FNPT 全矩阵 successor（见上方 Deferred）
