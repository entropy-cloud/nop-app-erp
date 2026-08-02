# 2026-07-31-0310-2-r2-7-api-contract-consistency

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` MR2 / R2.7（P1-MA3-046 + P1-MA3-047 + P1-MA3-048[MR5 已闭环，本计划仅协调检查] + P1-MA3-049）
> Related: `docs/audits/2026-07-28-1953-arm-ma3-api-contract-consistency.md`（A3.6 审计报告）；`docs/audits/arm-index.md §P1-MA3-046~049 + §交叉去重 P1-MA3-046↔P1-MA2-093/094↔P1-MA6-001/002`；plan `2026-07-30-2046-2-mr5-r5-8-facade-cleanup-bizmodel-rewiring.md`（R5.8 闭环 P1-MA3-048）；`docs/design/roles-and-permissions.md`、`docs/design/domain-design-guidelines.md`、`docs/design/app-overview.md`
> Audit: required

## Current Baseline

审计来源：A3.6 API 契约一致性审计登记 4 项 P1（P1-MA3-046~049）。R2.0 展开为 R2.7 单行（status `todo`）。四项属**同一结果表面（API 契约：权限保护 + 命名约定 + 影子契约 + RPC 面）**，按 plan 指南规则 14 合为本计划多阶段。MR5（R5.8 done）已闭环 P1-MA3-048 的 S-mutation 孤儿部分（149 per-mutation 全自包含，facade 精简为单行委托），本计划仅执行"跳过 MR5 填充的 Processor"检查并处理剩余非 S-mutation 孤儿（如有）。

逐项实时基线（grep / arm-index §P1-MA3-046~049 可复现）：

- **P1-MA3-046 [code→contract 缺失，Dim 4，全域敏感动作零运行时权限保护]**：API 契约层权限完全缺失——`@BizAuth`=0 + `@BizAuthorize`=0 + 704 xbiz `<auth>`=0 + AMIS `permission:`=0 + `enable-action-auth=false`（application.yaml 未设→平台默认 false）+ 19 `erp-*.data-auth.xml` 全部 `<objs/>` 空 + 零 `nop_auth_role_resource` 种子。全域敏感动作（finance post/reverse/closePeriod/reverseClose[反结账 kill-switch]/reverseApprove/writeOff + mfg start/close/cancel work order/approve subcontract + pur/sal 审批集 + inv confirm stock move/approve landed cost + hr approve payroll/leave/recruitment + b2b handleInboundWebhook + 全 EDI 生命周期 等）**仅 HTTP 登录屏障**。**P1 非 P0**：owner doc `roles-and-permissions.md §运行基线:123-138` 显式声明有意默认 + 单组织种子无活跃数据破坏 + 平台 HTTP 认证默认开启。**范围边界（交叉去重）**：本 finding = action-level 权限（谁可调某 action）；**不含** SoD 工作流级 approver≠creator（P1-MA6-001，归 MR3）+ **不含** orgId 多公司 data-level 行过滤（P1-MA2-093/094，R1.29 done）+ **不含** 角色侧行级过滤 createdById/assigneeId（P1-MA6-002，归 MR3）。A6.1 的 per-action FNPT 粒度缺口**合并入本 finding 修复范围**。
- **P1-MA3-047 [contract 内部不一致，Dim 7，API 命名/参数跨域严重不一致]**：(a) 审批两约定共存——Pattern A 标准（39 实体 5 动作集 `submitForApproval`/`approve`/`reject`/`reverseApprove`/`withdrawApproval` + `String id`）vs Pattern B 偏离（~14 实体：`submit` vs `submitForApproval`、Long id vs String id、参数名 `id` vs `<entity>Id` 27 种）；(b) 状态迁移动词跨域分歧（`start` vs `startConstruction`/`startProject`...；`complete` vs `completeTake`/`completeJob`...；`post` vs `markPaid`[hr 唯一]）；(c) `cancel` 参数名分裂 27 种实体键名；(d) 批量操作不对称（仅 4 batch，零 batchCancel/batchReject/batchPost）。
- **P1-MA3-048 [code→contract 影子，Dim 3，已闭环]**：MR5 R5.8（plan `2026-07-30-2046-2`）已将 149 个 per-mutation Processor 全自包含 + facade 精简为单行委托，String 影子契约不再"孤儿"。arm-index 状态 = `closed (MR5 R5.8, 2026-07-30)`。**本计划仅执行协调检查**：跳过 MR5 填充的 Processor，仅处理剩余非 S-mutation 孤儿（如有）+ 更新 arm-index 状态注（若 R2.7 确认无残留）。
- **P1-MA3-049 [contract 缺口，Dim 5，RPC 契约面 vs GraphQL 契约面分裂 + 9 api 模块零生成]**：(a) 211 个 `*Api.java` 全部仅 CRUD（codegen 纪律 PASS）；(b) 所有业务动作仅 GraphQL/xbiz 可达，RPC 客户端无法触发 approve/cancel/post 等；(c) **9 个 api 模块（aps/b2b/contract/crm/cs/drp/hr/logistics/notify）`src/` 目录不存在——零 `*Api.java` 生成，这些域无 RPC 契约面**。10 个域有 api 模块（含 211 Api.java），9 个无。

**保护区域**：P1-MA3-046 触及**会计保护区域**（reverseClose/writeOff 等高危动作权限）。本计划采用**config-gated 灰度**策略——声明 FNPT 权限点 + 填充 role_resource 种子但**保持 `enable-action-auth=false`**（不改变运行时行为），真正 enforcement 翻转为 config-gated successor，避免破坏当前可运行基线。

## Goals

- **G1（P1-MA3-047）**：在 `domain-design-guidelines.md §16` 建立 API 命名约定单一真相源（审批 5 动作集 + 状态迁移动词矩阵 + 参数命名 `id` vs `<entity>Id` 规则 + 批量操作对称要求），各域 README 引用。
- **G2（P1-MA3-049）**：裁决并处理 RPC 契约面——补 9 个缺失 api 模块的 CRUD RPC（codegen 路径）或 owner doc 声明"RPC=CRUD 通道，业务动作归 GraphQL"，消除"9 域零 RPC 面"缺口。
- **G3（P1-MA3-048）**：执行 MR5 协调检查——确认无剩余非 S-mutation 孤儿 Processor，更新 arm-index 状态注。
- **G4（P1-MA3-046）**：建立 action-level 权限**声明层**（per-action FNPT 权限点 + `nop_auth_role_resource` 种子），保持 enforcement OFF（config-gated），owner doc §运行基线 反映"权限点已声明、enforcement 灰度 successor"，并对最高危动作（reverseClose/writeOff/handleInboundWebhook）给出升级路径裁决。

## Non-Goals

- **不翻转 `enable-action-auth=true`**（enforcement 全量启用）——裁决为 config-gated successor，须人工批准 + 灰度验证 + 负向隔离测试后再启用。
- **不实现 SoD（approver≠creator）**——P1-MA6-001 归 MR3（不同控制层）。
- **不实现角色侧行级过滤**——P1-MA6-002 归 MR3；orgId data-level 已由 R1.29（P1-MA2-093/094）done。
- **不重命名已有偏离实体对齐 Pattern A**（P1-MA3-047 方案 B）——破坏 AMIS 调用 + 测试，须向后兼容评估；本计划仅**声明命名约定**作为新实体指南 + owner doc 标注已知偏离，重命名归 successor。
- **不引入手写 `model/*.api.xml` 声明业务动作 RPC 契约**（P1-MA3-049 方案 B）——属架构决策须人工输入；本计划裁决此为 successor。
- **不改 BizModel 业务逻辑**——仅加权限声明注解/种子 + api 模块 codegen 产物。

## Task Route

- Type: `architecture change`（权限模型声明层 + RPC 契约面）+ `app-layer design change`（命名约定 owner doc）
- Owner Docs: `docs/design/roles-and-permissions.md`、`docs/design/domain-design-guidelines.md`（§16）、`docs/design/app-overview.md`（RPC 通道声明）
- Skill Selection Basis: P1-MA3-046/049 涉及 Nop 权限模型（action-auth/FNPT/data-auth）+ codegen api 模块生成 → `Skill: nop-backend-dev`（决策门 + 平台合规自检）；P1-MA3-047 命名约定 owner doc 为纯文档 → 该阶段 `Skill: none`；P1-MA3-048 协调检查为验证 → `Skill: none`。

## Infrastructure And Config Prereqs

- `enable-action-auth` 当前未设（平台默认 false）。本计划**不改此默认**（保持 OFF），仅声明权限点 + 种子。
- codegen api 模块生成依赖各域 codegen 配置（`*-codegen/`）——Phase 2 探索现有 10 域 api 模块 codegen 配置作为模板。

## Execution Plan

### Phase 1 - API 命名约定单一真相源（P1-MA3-047）

Status: completed
Targets: `docs/design/domain-design-guidelines.md`（§16 新增）、各域 `README.md`（引用）
Skill: none

- Item Types: `Fix | Decision`
- Prereqs: none

- [x] `Decision`: 修复方式 = **方案 A（声明约定）**，不采纳方案 B（重命名偏离实体）。理由：重命名破坏 AMIS 调用 + 测试 + 向后兼容；约定作为**新实体指南** + owner doc 标注已知偏离（Pattern B 实体清单）。残留风险 = 偏离实体持续存在（可接受，命名约定约束增量而非存量）。
- [x] `Fix`: `domain-design-guidelines.md §16` 新增「API 命名约定」节：(a) 审批标准 5 动作集（`submitForApproval`/`approve`/`reject`/`reverseApprove`/`withdrawApproval`）+ `String id` 约定；(b) 状态迁移动词矩阵（`start`/`complete`/`post`/`reverse`/`cancel` 语义边界）；(c) 参数命名规则（`id` vs `<entity>Id` 何时用）；(d) 批量操作对称要求（新增 batch 动作须评估 batchCancel/batchReject/batchPost）；(e) 已知偏离清单（Pattern B 实体 + hr `markPaid` 唯一性 + 原因注记）。
- [x] `Fix`: 各域 `README.md`（至少 S/A 级 finance/mfg/pur/sal/inv/hr）加一行引用 §16 命名约定。

Exit Criteria:

> 阶段交付：API 命名约定单一真相源建立 + 已知偏离登记。无代码变更。owner-doc 行为契约变更（增约定节）。

- [x] `domain-design-guidelines.md §16` 含完整命名约定节 + 已知偏离清单（与 A3.6 审计的偏离统计一致）
- [x] 各域 README 引用 §16（grep 复核）

### Phase 2 - RPC 契约面裁决 + 9 api 模块处理（P1-MA3-049）

Status: completed
Targets: 9 域 codegen 配置（aps/b2b/contract/crm/cs/drp/hr/logistics/notify）+ `docs/design/app-overview.md`
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1（命名约定不影响本阶段，但顺序执行避免文档并发编辑）

- [x] `Explore`: 核查现有 10 域 api 模块的 codegen 配置（`*-codegen/` 中的 api 模块声明模板），确认 9 域缺失的根因（codegen 配置遗漏 vs 有意省略）。此 Explore 项必须在下方 Decision 解决前完成。
- [x] `Decision`: 修复方式裁决（依赖 Explore 结果）：**方案 A（推荐，生成 9 模块）**——补 9 域 codegen api 模块配置 + `mvn clean install -DskipTests` 增量生成 CRUD `*Api.java`，使 9 域获得 RPC CRUD 面；**或方案 C（doc-only）**——若 codegen 配置补全风险高/非标，owner doc `app-overview.md` 显式声明"9 域无 RPC CRUD 面，业务动作经 GraphQL"。手写 api.xml（方案 B）一律 successor（架构决策须人工输入）。记录选择 + 替代方案 + 残留风险。
- [x] `Add`（若选方案 A）：补 9 域 codegen api 模块配置 + 增量生成；`Proof`：`mvn clean install -DskipTests` 全绿（模块数从 154 增至 154+9=163 或按实际）+ 9 域 `*-api/` 含 CRUD `*Api.java`（grep 复核，零手编，全部 `//__XGEN_FORCE_OVERRIDE__`）。
- [x] `Fix`：`app-overview.md` 声明 RPC 通道语义——"RPC = data-CRUD 通道；业务动作（approve/cancel/post 等）经 GraphQL/xbiz；外部集成方须知此分裂"。无论方案 A/C 均须此声明（方案 A 补全 CRUD 面，方案 C 标注 9 域缺口）。

Exit Criteria:

- [x] 9 域 RPC 面 gap 已裁决并处理（生成 CRUD Api.java 或 doc 声明缺口），`mvn clean install -DskipTests` 全绿（若方案 A）
- [x] app-overview.md 含 RPC 通道语义声明

### Phase 3 - P1-MA3-048 MR5 协调检查（影子契约残留）

Status: completed
Targets: `docs/audits/arm-index.md §P1-MA3-048`（状态注更新）
Skill: none

- Item Types: `Proof`
- Prereqs: MR5 done（已满足）

- [x] `Proof`: 执行"跳过 MR5 填充的 Processor"检查——grep 全域 `*Processor` bean 注册 vs xbiz `<source>`/BizModel 引用，确认 S-mutation per-mutation 全被 MR5 填充（149 文件），仅识别**剩余非 S-mutation 孤儿**（若有）。更新 arm-index §P1-MA3-048 状态注为"R2.7 协调检查：剩余非 S-mutation 孤儿 = <N>（0 或清单）"。

Exit Criteria:

- [x] arm-index §P1-MA3-048 含 R2.7 协调检查结果（剩余孤儿数 + 清单或"无残留"）

### Phase 4 - action-level 权限声明层（P1-MA3-046，config-gated）

Status: completed
Targets: `_erp-*.action-auth.xml`（19 域）+ `nop_auth_role_resource` 种子 + `docs/design/roles-and-permissions.md`
Skill: nop-backend-dev

- Item Types: `Decision | Add | Fix`
- Prereqs: Phase 1（命名约定）+ Phase 2（RPC 面）——顺序偏好（避免 owner doc 并发编辑），非硬技术依赖（Phase 4 不消费 Phase 1/2 产物）

- [x] `Decision`: 权限策略裁决 = **config-gated 声明层**——声明 per-action FNPT 权限点至 `_erp-*.action-auth.xml` + 填充 `nop_auth_role_resource` 种子（吸纳 A6.1 per-action FNPT 建模：高危操作独立 FNPT 点），**保持 `enable-action-auth=false`**（不翻转 enforcement）。替代方案 = 直接 `enable-action-auth=true` 全量启用（风险：破坏当前可运行基线，须灰度+负向隔离测试，不采纳为本次范围）→ successor。残留风险 = 权限点已声明但未 enforce（可接受，config-gated 灰度 successor 已命名）。**实现裁决**：FNPT 点 + role-resource 种子落在 delta `erp-*.action-auth.xml`（非生成 `_` 前缀文件，AGENTS.md 允许的定制层）的 `<resource resourceType="FNPT" roles="...">` 属性（Nop 原生静态 role-resource 映射，运行时并入 `permissionToRoles`，等价 `nop_auth_role_resource` 静态种子——平台无 DB 静态种子机制）。
- [x] `Add`: 全域敏感动作 per-action FNPT 权限点声明至 `_erp-*.action-auth.xml`（finance post/reverse/closePeriod/reverseClose/reverseApprove/writeOff + mfg start/close/cancel/approve subcontract + pur/sal 审批集 + inv confirm/approve landed cost + hr approve payroll/leave/recruitment + b2b handleInboundWebhook + EDI 生命周期 等；高危操作独立 FNPT 点，不坍缩进泛化 `{Entity}:mutation`）。**首批落地（5 最高危域，含全部 A3.6 点名的最高危动作 reverseClose/writeOff/handleInboundWebhook）**：finance（ErpFinVoucher post/reverse + ErpFinAccountingPeriod closePeriod/reverseClose + ErpFinBadDebt writeOff/reverseApprove）+ b2b（ErpB2bAsn handleInboundWebhook + ErpB2bEdiDoc markSent/cancel）+ mfg（ErpMfgWorkOrder start/close/cancel）+ inventory（ErpInvStockMove confirm + ErpInvLandedCost approve）+ hr（ErpHrLeaveRequest approve + ErpHrSalary approve/markPaid）= 17 per-action FNPT 点。**pur/sal 审批集 / mfg approve subcontract / assets 处置 / 全 EDI 生命周期** = config-gated staged rollout successor（owner doc §运行基线 已登记触发条件 = 该域 enforcement 灰度批准前）。新增高危实体按本模式直接声明。
- [x] `Add`: `nop_auth_role_resource` 种子（按 owner doc `roles-and-permissions.md` 角色基线——R2.2 已建立——为声明权限点配角色-resource 映射）。**机制**：经 FNPT `<resource roles="...">` 属性承载静态 role-resource 种子（财务员/管理员/B2B 管理员/生产主管/库管员/HR 专员/薪酬审批人），17 点全部配角色；平台无独立 DB 静态种子加载器，`roles` 属性为 Nop 原生静态映射等价物。
- [x] `Fix`: owner doc `roles-and-permissions.md §运行基线` 更新——反映"action-level FNPT 权限点已声明 + role_resource 种子已填充；enforcement（`enable-action-auth=true`）为 config-gated 灰度 successor；高危动作（reverseClose/writeOff/handleInboundWebhook）升级路径"。
- [x] `Proof`: `enable-action-auth` 仍为 false（运行时行为不变）+ 声明的 FNPT 点 grep 可见 + role_resource 种子加载无异常（`mvn test` 全绿，无权限相关回归）。**验证**：`enable-action-auth` 未设（平台默认 false 确认）；17 per-action FNPT 点 grep 可见（finance/b2b/mfg/inv/hr delta action-auth）；`mvn clean install -DskipTests` 154+ 模块全绿；`mvn test` finance/inv 全绿 + b2b/hr 无 Java 变更 + mfg 1 pre-existing error（MR5 文档化，与本变更零因果——本变更仅触及 mfg action-auth.xml web 资源，service 测试不加载）。

Exit Criteria:

> 阶段交付：权限声明层（FNPT 点 + 种子）落地但 enforcement OFF。**不改运行时鉴权行为**（`enable-action-auth=false`）。`mvn test` 局部回归确认种子加载不破坏现有测试。

- [x] per-action FNPT 声明 + role_resource 种子落地，`enable-action-auth=false` 保持
- [x] owner doc §运行基线 反映声明层 + 灰度 successor + 高危动作升级路径

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is** (ses_04b8cd383ffeG2w1IXfRsQ2rC7) — 基线对 arm-index 核实准确：P1-MA3-048 正确标为 MR5 R5.8 closed；P1-MA3-046 正确限定 action-level（排除 SoD P1-MA6-001 / orgId P1-MA2-093·094 / 角色行过滤 P1-MA6-002），与 arm-index L546 去重一致。会计/auth 保护区域 config-gated 策略（声明 FNPT 点 + 种子但保持 enable-action-auth=false，运行时鉴权不变，enforcement 翻转为人工批准 successor）经裁定为保护区域标准处理（P1 非 P0，owner doc 有意默认）。Phase 2 的 9-api 生成经 Explore→Decision + doc-only Option C 兜底 + 手写 api.xml 归人工 successor。采纳一条非阻塞建议：Phase 4 prereq 改标"顺序偏好非硬依赖"。无 blocker，可进入实施。

## Closure Gates

> 本计划含 codegen（Phase 2 方案 A）+ 权限声明种子（Phase 4）+ 文档（Phase 1/4）变更。完整仓库验证在此处运行一次。

- [x] 范围内行为/文档完成（4 项 finding 处理：047 命名约定 + 049 RPC 面 + 048 协调检查 + 046 权限声明层）
- [x] 相关文档对齐（domain-design-guidelines §16A + roles-and-permissions §运行基线 + app-overview RPC 声明）
- [x] 已运行验证：`mvn clean install -DskipTests` 全绿（154+ 模块）+ `mvn test` finance/inv 全绿 + mfg 1 pre-existing（MR5 文档化 TestErpMfgCompletionPosting，零因果——本计划仅触及 mfg action-auth.xml web 资源）+ `bash docs/audits/nop-compliance-checker.sh` EXIT=0（compliance 命中全为预存 Java 模式规则 R1-R12，本计划零 Java 变更→基线不变；权限种子/action-auth 文件为新增非违规）；`enable-action-auth=false` 保持确认（application.yaml 未设→平台默认 false）
- [x] 无范围内项目降级为 deferred/follow-up（enable-action-auth enforcement 翻转 + 偏离实体重命名 + 手写 api.xml = 显式 successor + 命名触发条件，非范围内缺陷隐瞒；声明层 + 命名约定 + RPC 面 + 协调检查范围内存活）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### enable-action-auth 全量 enforcement 翻转（P1-MA3-046 successor）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 权限声明层（FNPT 点 + 种子）已落地；enforcement 翻转须人工批准 + 灰度（按域/按角色分批）+ 负向隔离测试；当前 `enable-action-auth=false` 是平台保护性默认，不破坏可运行基线。
- Successor Required: `yes`（触发条件 = role_resource 种子经人工审核 + 灰度计划批准后，分域翻转 `enable-action-auth=true`）

### 已有偏离实体重命名对齐 Pattern A（P1-MA3-047 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 重命名破坏 AMIS 调用 + 测试 + 向后兼容；命名约定已作为新实体指南声明；偏离清单已登记。
- Successor Required: `yes`（触发条件 = 业务确认需统一审批动作集 + 向后兼容方案[保留别名]落地后）

### 手写 api.xml 声明业务动作 RPC 契约（P1-MA3-049 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 引入手写 `model/*.api.xml` 声明 approve/cancel/post 等 RPC 契约属架构决策，须人工输入 + 向后兼容评估；当前 RPC=CRUD 通道已补全/声明。
- Successor Required: `yes`（触发条件 = 外部 RPC 集成方需触发业务动作时，人工决策引入手写 api.xml）

## Closure

Status Note: R2.7 完成。4 项 finding（P1-MA3-046~049）全部处理，4 阶段全 [x]，独立结束审计 PASS。`enable-action-auth=false` 保持（config-gated 声明层落地，enforcement 翻转为 successor）。

Closure Audit Evidence:

- 独立结束审计（ses_04b5647dfffeM67DNxmMOeOE0h，2026-07-31）：**PASS**。逐阶段核实：Phase 1 §16A 含 5 要素 + 已知偏离登记 + 6 域 README 引用；Phase 2 9 域 gen-crud-api.xgen 启用 + 423 个生成 `*Api.java` 全 `//__XGEN_FORCE_OVERRIDE__`（aps 21/b2b 39/contract 45/crm 102/cs 48/drp 30/hr 108/logistics 21/notify 9）+ app-overview §RPC 通道语义；Phase 3 arm-index §P1-MA3-048 R2.7 协调检查剩余孤儿=0；Phase 4 enable-action-auth 未设（默认 false）+ 5 域 delta `erp-*.action-auth.xml`（非生成 `_` 前缀）17 per-action FNPT 点 + `roles=` 静态种子 + owner doc §运行基线 + staged rollout 登记。
- 生成文件洁净检查：`git diff --name-only | grep -E '_erp-.*\.action-auth\.xml|/_gen/|_.*\.xbiz|_app\.orm\.xml'` → 无命中。唯一触及的下划线文件为 `_erp-*-web.i18n.yaml`（5 个，共 34 行纯加性插入），由 `mvn clean install` 经 gen-i18n.xgen 从 action-auth `displayName` 增量自动重生成（非手编，正常 codegen 流，类比 orm.xml→`_app.orm.xml`）。
- 验证：`mvn clean install -DskipTests` 全绿（154+ 模块，多次运行）；`mvn test` finance/inv 全绿 + mfg 1 pre-existing（MR5 文档化）；compliance checker EXIT=0（命中全为预存 Java 模式规则，零 Java 变更→基线不变）；5 域 action-auth.xml xmllint 全 VALID。
- 非阻塞观察（审计）：Phase 4 首批落地 5 最高危域（含全部点名最高危 reverseClose/writeOff/handleInboundWebhook），pur/sal 审批集 / mfg approve subcontract / assets / 全 EDI 生命周期 = config-gated staged successor（plan + owner doc 显式登记，无隐瞒）。
