# 2026-07-28-1953-2-audit-remediation-ma3-api-contract-consistency MA3 API 契约一致性（A3.6）

> Plan Status: completed
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA3（工作项 A3.6）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.3「API 契约一致性」行（MA3，当前 `新维度`）；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/multi-dimensional-audit-prompt.md`（审计方法）；`docs/plans/2026-07-28-1953-1-audit-remediation-ma3-owner-doc-vs-code-drift.md`（A3.3-A3.5 同批——后端 design vs 后端代码 drift，本审计聚焦 API 契约层 vs 实现一致性，互补不重叠）；`module-*/model/*.api.xml`（roadmap 声明 owner doc——实时仓库核实**不存在**，见 Current Baseline）；`module-*/erp-*-service/src/main/resources/_vfs/erp/<short>/model/*/*.xbiz`（实际 API 契约源——BizModel 动作声明）
> Audit: required

## Current Baseline

API 契约一致性审计（文档-实现一致性层 MA3 第六项）。roadmap 工作项 A3.6 声明审查"API 契约（api.xml）vs 实现一致性（全域）"，owner doc 标注 `module-*/model/*.api.xml`。

**关键基线事实（实时仓库核实）**：**本项目不存在任何手写 `*.api.xml` 源模型文件**（`find . -name "*.api.xml"` 全仓 0 命中，排除 target/）。在 Nop Platform 中，`*.api.xml` 是**可选的手写 API 契约定义文件**，用于显式声明 GraphQL/RPC 接口契约。本项目未手写 api.xml，API 契约面由以下机制**自动派生**：

- **xbiz 文件**（`module-*/erp-*-service/src/main/resources/_vfs/erp/<short>/model/<Entity>/*.xbiz`）：声明 BizModel 的 `@BizQuery`/`@BizMutation` 动作（动作名/参数/返回类型/权限注解）。部分为代码生成产物（`_` 前缀的 `_*.xbiz`），部分为手写覆盖（无 `_` 前缀）。这是 **API 契约的主要声明源**。
- **xmeta 文件**（`module-*/erp-*-meta/`）：声明实体元数据（字段/类型/校验/显示），驱动 GraphQL schema 的实体类型定义。
- **BizModel Java**（`module-*/erp-*-service/`）：`@BizQuery`/`@BizMutation` 注解方法——GraphQL schema 的**实际实现源**。xbiz 与 Java 双向同步（codegen 增量生成）。
- **生成 `*Api.java` 接口**（`module-*/erp-*-api/`）：codegen 生成的 Java RPC 接口契约（如 `ErpFinVoucherApi`），从 orm.xml + xbiz 派生。

因此 A3.6 的实际审查对象不是"手写 api.xml vs 实现"，而是**"自动派生的 API 契约面（xbiz 声明 + xmeta + 生成 *Api.java）vs 实际实现（BizModel Java + ORM）的一致性"**，以及**"手写 xbiz 覆盖（delta）与生成 xbiz 的契约漂移"**。

**已知未核验控制点**（multi-dimensional-audit 7 维度适配 API 契约主题）：

- **xbiz 动作 vs Java 实现 drift**：每个 xbiz 声明的动作是否有对应 Java 实现？Java 中的 `@BizQuery`/`@BizMutation` 方法是否全部反映在 xbiz 中？手写 xbiz 覆盖（无 `_` 前缀）与生成 xbiz（`_` 前缀）是否冲突？
- **参数/返回类型契约 drift**：xbiz 声明的参数名/类型/是否非空 vs Java 方法签名实际；返回类型声明 vs 实际返回。
- **权限注解一致性**：xbiz/Java 中 `@BizQuery`/`@BizMutation` 的权限注解（`@BizAuth` 等）是否一致；公共/敏感动作权限声明是否完整。
- **生成 *Api.java vs xbiz/orm 契约漂移**：生成的 RPC 接口是否与 xbiz 声明一致（codegen 增量生成后是否有手编生成物）。
- **未文档化/未声明 API（code→contract 反向 drift）**：BizModel 中存在但未在任何契约面声明的显著公共方法；或 xbiz 声明但 Java 未实现的悬挂动作。
- **跨实体 API 一致性**：相似操作（如全域 CRUD `__save`/`__get`/`__findPage`/`__delete` + 各域自定义动作）的命名/参数/返回模式是否全域一致。
- **api.xml 缺失的影响裁决**：roadmap owner doc 声明 `module-*/model/*.api.xml` 但全仓不存在——这是"声明了契约源但未物化"的 drift，还是"项目设计选择不手写 api.xml 依赖自动派生"（需裁决）。

剩余差距：需要一次系统性 API 契约面一致性审计。发现的 drift 分类为：(a) **xbiz 声明与 Java 实现不一致**（major/blocker——公共 API 契约漂移）；(b) **手写 xbiz delta 与生成 xbiz 冲突**（major）；(c) **权限注解缺失/不一致**（major——安全面）；(d) **未声明的公共 API**（major）；(e) **api.xml 缺失裁决**（note/major 视影响——若 roadmap 声明的契约源从未物化，登记为文档类 P1 目标 MR2）。blocker/major 登记为 P1（文档+代码类 P1）。本审计属 MA3 维度，全部 finding 目标 MR2（MR2 deps = MA3+MA4 done，由 R2.0 展开机制读取 MA3+MA4 批次 P1）；若发现公共 API 契约致运行时错误，升级标注走 P0 即时通道（不等 MR 批量）。本审计原则上不产生 P0。

## Goals

- 按 `multi-dimensional-audit-prompt.md` 对全域 API 契约面（xbiz 声明 + xmeta + 生成 *Api.java）vs 实际实现（BizModel Java + ORM）做系统性一致性审计，产出审计报告。
- 审查覆盖 7 维度（xbiz 动作 vs Java 实现 / 参数返回类型契约 / 权限注解一致性 / 生成 *Api.java vs xbiz 漂移 / 未声明 API / 跨实体 API 一致性 / api.xml 缺失影响裁决）。
- **裁决 api.xml 缺失性质**：明确本项目 API 契约面的实际定义方式（xbiz/xmeta 自动派生 vs 手写 api.xml），判定 roadmap owner doc `module-*/model/*.api.xml` 声明是 drift 还是设计选择。
- scope matrix §2.3「API 契约一致性」行终态标记（`新维度` → `✅`/`⚠️(P1)`）。
- 发现的 blocker/major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总。roadmap A3.6 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做后端 design owner doc vs 后端代码 drift — 归 A3.3-A3.5（同批）。本审计聚焦 API **契约层**（xbiz/xmeta/*Api.java 声明 vs 实现），不逐条比对设计文档业务语义与代码。
- **不**做 view.xml vs 后端契约 drift — 归 A4.6-A4.8（MA4）。
- **不**做索引路由有效性 — 归 A3.7（同批）。
- **不**做可定制性验证 — 归 A3.8。
- **不**逐个审计全域 ~2890 单元测试的 API 调用正确性 — 归 MA5（测试层）。本审计审契约**声明**与实现的**一致性**，不审测试覆盖深度。
- **不**在本计划内批量修复契约 drift — P1 经 R2.0 展开机制进入 MR2（文档+xbiz）/MR1（Java）。本审计只识别 drift + 分类。
- **不**手写 api.xml 文件（即使 roadmap owner doc 声明）— 是否引入手写 api.xml 属架构决策，不在本审计范围；本审计只裁决当前状态。
- **不**手改生成物或 ORM。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `module-*/erp-*-service/src/main/resources/_vfs/erp/<short>/model/*/*.xbiz`（API 契约主要声明源）；`module-*/erp-*-meta/`（xmeta 实体元数据）；`module-*/erp-*-api/`（生成 *Api.java RPC 接口）；`module-*/erp-*-service/` BizModel Java（实际实现源）；`module-*/model/*.orm.xml`（ORM 模型驱动 codegen）；roadmap A3.6 owner doc `module-*/model/*.api.xml`（声明但不存在——裁决对象）
- Skill Selection Basis: `multi-dimensional-audit-prompt.md`（roadmap A3.6 指定此 skill——多维挑战 7 维度，适配 API 契约主题时以维度为契约一致性检查框架。项目定制化层见 `docs/skills/README.md`）。与 A3.3-A3.5 不同结果表面（API 契约层 vs 业务语义层），独立计划。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。契约 drift 修复在 MR2（xbiz）/MR1（Java）批量进行。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为契约-代码比对审查，不构建/不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。契约比对无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - 全域 API 契约面一致性系统性审计（7 维度）

Status: completed
Targets: 全域 19 域 `module-*/erp-*-service/` xbiz 文件 + BizModel Java；`module-*/erp-*-meta/` xmeta；`module-*/erp-*-api/` 生成 *Api.java；`module-*/model/*.orm.xml`（S+A 级域重点抽样 finance/mfg/pur/sal/inv/hr/assets，B+C 级域合并抽样）
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 平台合规审计 done（xbiz/codegen 纪律已部分覆盖）。A3.1 done。

- [x] 维度「api.xml 缺失性质裁决」：确认全域无手写 `*.api.xml`；裁决本项目 API 契约面定义方式（xbiz/xmeta 自动派生）vs roadmap owner doc `module-*/model/*.api.xml` 声明——drift（声明未物化）还是设计选择（依赖自动派生）。若为 drift 登记文档类 P1。
      - Skill: `multi-dimensional-audit-prompt.md`
      - **裁决：设计选择**（非 drift）。依据：Nop `api-model-and-codegen.md:218-227` 决策表明确 CRUD-centric 模块不应手写 api.xml + 本项目无跨进程 RPC 需求 + 211 生成 `*Api.java` 全部 `ICrudApi` + 352 `_*.xbiz` 全部 `biz-gen:DefaultBizGenExtends` ORM 驱动 + 自动派生合规。文档样板 drift 登记为 P2-MA3-039。
- [x] 维度「xbiz 动作 vs Java 实现 drift」：抽样 S+A 级域，逐实体核对 xbiz 声明的动作（`@BizQuery`/`@BizMutation`）与 BizModel Java 方法的一一对应。标记：xbiz 声明但 Java 未实现的悬挂动作 / Java 实现但 xbiz 未声明的未暴露方法。检查手写 xbiz 覆盖（无 `_` 前缀）与生成 xbiz（`_` 前缀）冲突。
      - Skill: `multi-dimensional-audit-prompt.md`
      - **裁决：CLEAN**（零悬挂/零 CRUD 名冲突/零名字碰撞，0/90 inject 引用悬挂，184 I*Biz 引用全解析；三种合法共存模式 Pattern A xbiz 委托 + Pattern B 内联脚本 + Pattern C BizModel 注解生成）。
- [x] 维度「参数/返回类型契约 drift」：抽样自定义动作（非标准 CRUD），核对 xbiz 声明的参数名/类型/非空 vs Java 方法签名实际；返回类型声明 vs 实际返回。标记类型不匹配/参数缺失/返回类型漂移。
      - Skill: `multi-dimensional-audit-prompt.md`
      - **裁决：零 blocker/major TYPE drift**。Pattern-A 动作参数/返回类型统一（`String id`+`svcCtx`+`THIS_OBJ`）与 Processor 完全匹配；Pattern-C 由 Java 生成不可能 drift。1 项 major（F1 孤儿 Processor 影子契约 → P1-MA3-048）+ 3 项 minor（P2-MA3-036/037/038）。
- [x] 维度「权限注解一致性」：全域 grep `@BizQuery`/`@BizMutation` + 权限注解（`@BizAuth`/`@BizAuthorize` 等），核查公共/敏感动作（过账/红冲/结账/删除/审批）权限声明完整性。标记：敏感动作无权限注解 / xbiz 与 Java 权限注解不一致。
      - Skill: `multi-dimensional-audit-prompt.md`
      - **裁决：1 项 major（本审计最严重发现 → P1-MA3-046）**。全域 `@BizAuth`=0 + xbiz `<auth>`=0 + AMIS `permission:`=0 + `enable-action-auth=false` + 19 `data-auth.xml` 全空 `<objs/>` + 零 `nop_auth_role_resource` 种子。全域敏感动作（post/reverse/close/reverseClose/approve/cancel/writeOff/handleInboundWebhook/全 EDI 生命周期）仅 HTTP 登录屏障。xbiz/Java 无不一致（两层均统一为空）。与 A6.1/A6.2 todo 协同；不与 P1-MA2-093/094 重复（不同维度）。
- [x] 维度「生成 *Api.java vs xbiz/orm 契约漂移」：抽样 erp-*-api 生成接口，核对与 xbiz 声明 + orm.xml 实体一致；检查生成物是否有手编痕迹（违反 codegen 纪律）。
      - Skill: `multi-dimensional-audit-prompt.md`
      - **裁决：codegen 纪律 PASS + 1 项 major（→ P1-MA3-049）**。211 `*Api.java` 全部首行 `//__XGEN_FORCE_OVERRIDE__` + 全部 13 行 + 全部 `extends ICrudApi<I,O>` 空体——零手编。但 RPC 仅 CRUD，所有业务动作 GraphQL-only；9 api 模块（aps/b2b/contract/crm/cs/drp/hr/logistics/notify）零 `*Api.java` 生成。
- [x] 维度「未声明/未文档化 API（code→contract 反向 drift）」：BizModel 中存在但未在 xbiz 声明的显著公共方法；或 xbiz 声明但 Java 未实现。标记影响公共 API 面的未声明行为。
      - Skill: `multi-dimensional-audit-prompt.md`
      - **裁决：note（Nop-normal 模式，非缺陷）**。Pattern C 方法（xbiz delta 空 `<actions/>`）由运行时 biz-gen 从 BizModel Java 反射注册——非未声明，仅 xbiz delta 不重复声明（避免双真相源）。全部 `I*Biz` 接口 `@Override` 编译期契约化。finance 域内等价语义双模式选择（EmployeeAdvance/ExpenseClaim Pattern A vs BadDebt/BudgetScenario/Notes* Pattern C）归 P1-MA3-048 同型根因。
- [x] 维度「跨实体 API 一致性」：全域相似操作（CRUD `__save`/`__get`/`__findPage`/`__delete` + 各域自定义动作如 approve/submit/cancel/reverse）的命名/参数/返回模式一致性。标记偏离全域惯例的异常 API。
      - Skill: `multi-dimensional-audit-prompt.md`
      - **裁决：CRUD 基模式 100% 一致 + 跨域 Facade EXCELLENT + 1 项 major（→ P1-MA3-047）**。`IErpFinVoucherBiz` Facade 9 域 11 调用方签名一致。但审批两约定共存 14 实体偏离标准 + 状态迁移动词跨域严重分歧（`post` vs `markPaid` 等）+ `cancel` 参数名分裂（27 种实体键名）+ 批量操作覆盖不对称。
- [x] 产出审计报告 `docs/audits/2026-07-28-1953-arm-ma3-api-contract-consistency.md`（含：api.xml 缺失裁决 / 7 维度逐项审查结果 / xbiz-Java drift 清单 / 权限注解审计摘要 / 生成物纪律核查 / blocker/major/minor/note finding 清单 / 裁决通过/失败 / 剩余风险）。
      - Skill: none
      - **产出：已生成**（`docs/audits/2026-07-28-1953-arm-ma3-api-contract-consistency.md`，14 节 + 9 节 finding 清单 + 12 节剩余风险 + 14 节审计方法）。

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] api.xml 缺失性质裁决产出（drift vs 设计选择，含依据）
- [x] 7 维度逐项审查结果产出（每维度至少一句裁决，含"本维度无 drift"）
- [x] blocker/major/minor/note finding 清单产出，每个含 drift 方向 / 严重性 / 受影响文件[xbiz+Java] / drift 描述 / 影响

### Phase 2 - finding 汇总交接 MR2/MR1 + 索引/矩阵更新

Status: completed
Targets: API 契约 drift finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.3「API 契约一致性」行
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] finding 汇总：全部 drift blocker/major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA3-NNN`、报告、drift 方向、描述、目标 MR2[xbiz/文档]/MR1[Java]、修复状态 todo）。与 A3.1/A3.2/A3.3-A3.5 已登记 P1-MA3-* 去重无冲突。
      - Skill: none
      - **登记 4 项 P1**：P1-MA3-046（全域敏感动作零运行时权限保护 dim4）/ P1-MA3-047（API 命名/参数跨域严重不一致 dim7）/ P1-MA3-048（孤儿 Processor bean 携带 String 影子契约 dim3）/ P1-MA3-049（RPC vs GraphQL 契约面分裂 dim5）。与 A3.1~A3.5 已登记 P1-MA3-001~045 + MA2 P1-MA2-054（P1-MA3-048 子例，MR2 协同）经交叉去重无重复登记。
- [x] 分类裁决：全部 A3.6 finding 目标 MR2（MR2 deps = MA3+MA4 done）；公共 API 契约致运行时错误走 P0 即时通道，在报告中明确标注。
      - Skill: none
      - **裁决：4 项 P1 全部目标 MR2**（与 plan §Goals 一致）。本审计原则上无 P0——P1-MA3-046 权限缺失虽严重但 owner doc 显式声明有意默认 + 单组织种子无活跃数据破坏 + 平台 HTTP 认证默认开启，按 P1-MA2-093/094 同型裁决范式维持 P1。报告 §5.6 + §13 明确标注。
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.3「API 契约一致性」行终态标记（`新维度` → `✅`/`⚠️(P1)`）。
      - Skill: none
      - **更新完成**：arm-index.md 新增 A3.6 完成公告段（front-matter）+ P1-MA3-046~049 详细行 + 跨维度发现 2 项（P1-MA3-046↔A6.1/A6.2 协同 + P1-MA3-048↔P1-MA2-054 子例）+ 去重说明；scope matrix §2.3 行 `新维度` → `⚠️(P1)` + 新增 A3.6 完成公告段。

Exit Criteria:

- [x] 所有 drift blocker/major 已登记 arm-index §P1 汇总（全部目标 MR2），待展开
- [x] 与 A3.1/A3.2/A3.3-A3.5 已登记 P1 经交叉去重无重复登记
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05769e927ffehray74t4QW1KLE`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：**关键基线事实经直接验证为真**——`find . -name "*.api.xml" -not -path "*/target/*"` 返回 0 结果 ✓；xbiz/xmeta/生成 *Api.java 三契约面均确认存在 ✓；roadmap A3.6 `todo` + Owner Doc/Skill/Deps 匹配 ✓；与 A3.3-A3.5 边界清晰（API 契约层 vs 业务语义层）✓；anti-slack 零禁词 ✓；finding ID 不冲突（next P1-MA3-024+）✓；api.xml 为 Nop 可选手写文件的 reframing 合理 ✓。**采纳 1 项非阻塞修订**：MR 路由对齐——全部 A3.6 finding 目标 MR2（MR2 deps=MA3+MA4 done），删除误导性"MR1[Java]"建议语言（R*.0 展开机制以 roadmap 依赖边为准）。Plan Status 转 active。

## Closure Gates

> 本计划主体是契约-代码比对审查（不改应用代码；产出为审计报告 + arm-index/scope-matrix 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。契约 drift 修复在 MR2 批量进行（全部 A3.6 finding 目标 MR2）；公共 API 契约致运行时错误走 P0 即时通道。本审计只识别 drift + 分类。

- [x] 范围内行为完成（A3.6 API 契约面一致性审计报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix 结论已反映）
- [x] 已运行验证：契约比对无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2/MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 后端 design owner doc vs 后端代码 drift（A3.3-A3.5）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计聚焦 API 契约层（xbiz/xmeta 声明 vs 实现）；设计文档业务语义 vs 代码逐条 drift 归 A3.3-A3.5。若契约审计中发现设计语义层问题，标注交接 A3.3-A3.5。
- Successor Required: `yes`——A3.3-A3.5 执行时复核（同批起草）。

### view.xml vs 后端契约 drift（A4.6-A4.8）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 前端 view.xml 调用的 API/字段 vs 后端契约 drift 归 MA4（A4.6-A4.8）。本审计审后端契约声明本身，不审前端消费。
- Successor Required: `yes`——A4.6-A4.8 执行时复核。

### 测试覆盖深度（MA5）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计审契约声明与实现的一致性；测试是否充分覆盖 API 面归 MA5。
- Successor Required: `no`——MA5 独立维度。

## Closure

Status Note: A3.6 API 契约一致性审计完成（2026-07-28）。Verdict FAIL（有 drift），零 BLOCKER，4 项 major → P1（P1-MA3-046~049，全部目标 MR2）+ 4 项 minor → P2（P2-MA3-036~039）。关键裁决：api.xml 缺失 = 设计选择（非 drift，Nop 明确可选 + 自动派生合规）；Dim 2 xbiz vs Java CLEAN；codegen 纪律 PASS；跨域 Facade EXCELLENT。Dim 4 权限注解（全域敏感动作零运行时保护）为本审计最严重发现，与 A6.1/A6.2 todo 协同裁决，按 P1-MA2-093/094 同型裁决范式维持 P1 非 P0。审计报告 `docs/audits/2026-07-28-1953-arm-ma3-api-contract-consistency.md` 已产出；arm-index §P1 汇总已登记 P1-MA3-046~049 + 跨维度发现 2 项；scope matrix §2.3「API 契约一致性」行 `新维度` → `⚠️(P1)`；roadmap A3.6 `todo` → `done`。MA3 累计 P1=41 / P2=26。

Closure Audit Evidence:

- Auditor / Agent: 独立 close-auditor 子代理（fresh-context 新会话，对照实时仓库逐项复核）
- Evidence:
  - 独立 closure audit 完成（fresh-context 新会话）：核对全部 deliverable 落地——审计报告 `docs/audits/2026-07-28-1953-arm-ma3-api-contract-consistency.md` 存在（35792 字节，14 节，finding 清单含具体代码引用与行号如 `ErpFinBadDebtProcessor.approve(Long,...):93` / `ErpFinBadDebtApproveProcessor.approve(String,...):22-23`）；`arm-index.md` §P1 详细清单 P1-MA3-046~049 行存在 + 跨维度发现表 2 行 + 去重说明段；`audit-remediation-scope-and-dimension-matrix.md` §2.3 行 `新维度` → `⚠️(P1)` + A3.6 完成声明段；`audit-remediation-roadmap.md` A3.6 `done`；`docs/logs/2026/07-28.md` 已记录本次审计工作。
  - 五点一致性已验证：Plan Status `completed` / Phase 1+2 `completed` / 两 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]`（本审计勾选 gate 7）/ Closure evidence 非 placeholder。
  - Anti-Hollow 验证：4 项 P1 finding 均含具体文件路径 + grep 计数（211 `*Api.java`、44 WithdrawApprovalProcessor、704 xbiz、19 `data-auth.xml` 147 字节空 `<objs/>`）+ 行号引用，非空泛声明。
  - Deferred honesty 验证：A3.3-A3.5 / A4.6-A4.8 / MA5 均为合法 non-goal 显式路由至各自 owner plan，无范围内的 live defect 隐藏为 deferred。
  - 验证：审计不改应用代码（audit-only），build/test 门控为同型审计 plan 标准 Closure 实践（见 §Infrastructure And Config Prereqs）。
  - 详见审计报告：`docs/audits/2026-07-28-1953-arm-ma3-api-contract-consistency.md`（14 节 + 4 项 P1 + 4 项 P2 清单 + 交叉去重 + 剩余风险）
  - arm-index 更新：`docs/audits/arm-index.md`（A3.6 完成公告 front-matter 段 + P1-MA3-046~049 详细行 + 跨维度发现表 2 项 + 去重说明段）
  - scope matrix 更新：`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.3（A3.6 完成公告段 + 「API 契约一致性」行 `新维度` → `⚠️(P1)`）
  - roadmap 更新：`docs/backlog/audit-remediation-roadmap.md` A3.6 `todo` → `done`
  - 多维证据采集：4 个并行 explore 子代理（ses_0573267e5ffe3GIuSWlmCqdir0 dim1+5+6 / ses_057323e61ffe6pAUb4MdqC24JF dim2+6 / ses_057321a6dffe2054jYIXwQ966n dim3 / ses_05731e84cffefjOA7cdIK3hN9V dim4 / ses_05731bcf2ffe8V4mztD5XpxDJn dim5+7）

Follow-up:

- 独立 close audit 已由独立子代理（fresh-context 新会话）执行通过——Closure Gates 第 7 项已勾选，Closure 段 Auditor/Agent 字段已填充。
- MR2 批次修复 P1-MA3-046~049 时：(a) P1-MA3-046 与 A6.1/A6.2 todo 协同；(b) P1-MA3-048 吸纳 P1-MA2-054 一并裁决；(c) P1-MA3-049 MR2 裁决可能触发"是否引入手写 api.xml"架构决策（plan Non-Goal 已声明此类决策不在审计范围，须人工决策）；(d) P1-MA3-047 跨域命名重构须评估向后兼容（保留别名 vs 一次性破坏）
