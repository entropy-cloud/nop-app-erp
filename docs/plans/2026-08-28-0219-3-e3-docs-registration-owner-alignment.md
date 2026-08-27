# 2026-08-28-0219-3-e3-docs-registration-owner-alignment E3 批文档注册与 owner-doc 对齐收口（含 @Description 补齐）

> Plan Status: active（独立草案审查共识：iteration 1 acceptable-as-is，task `ses_fbb89f5e3ffeY9WCHTv3yWF12T`，2026-08-28）
> Mission: erp-enhancement
> Work Item: Follow-up Backlog 文档注册批（P2-A/P2-B/P2-C+P2-13/P2-2/P2-10/P2-12）
> Last Reviewed: 2026-08-28
> Source: `docs/backlog/erp-enhancement-roadmap.md §Follow-up Backlog`（来源审计 `docs/audits/2026-08-26-2226-open-audit-erp-enhancement.md` P2-A/P2-B/P2-C（其「修复建议路由」第 1 条显式 grouping 为「文档注册批」）+ `docs/audits/2026-08-26-2226-multi-audit-erp-enhancement.md` P2-2/P2-10/P2-12/P2-13（其路由第 4 条分流 P2-2 入 erp-enhancement follow-up、P2-13 入 architecture doc 维护））
> Related: plan `2026-08-26-0735-2`（E3 整体实现——本批收口其文档注册欠账与证据表述漂移）、plan `2026-08-28-0219-1`（P2-11 限流措辞归其 fin 面，本批不重复）
> Audit: required

## Current Baseline

（2026-08-28 活仓核实）

- **P2-A job 注册漏行**：`docs/architecture/job-scheduling.md:131` 已有 `erp-inv-stock-check` 行（E3.2 WIRED 格式范本：batch xml + job.yaml 三件套 + config-gated 默认关 + 部署键 + owner doc 引用），**无** `erp-fin-ap-doc-processing` 行（生产 nop-job 已运行，含 `nop.job.erp-fin-ap-doc-processing.enabled/.cron-expr` 两部署键未登记）。
- **P2-B 设计路由「暂不编码」标注陈旧 ×6**：`docs/design/README.md:40-41`（ai-native-interface → E3.6 / dashboard-semantic-layer → E3.1+E3.1b）+ `docs/design/finance/README.md:116`（document-driven-ap-automation → E3.5）+ `docs/design/aps/README.md:91`（constraint-based-planning → E3.4）+ `docs/design/inventory/README.md:131`（audit-snapshot-cycle-count → E3.2）+ `docs/design/assets/README.md:80`（audit-trail-and-custom-fieldsets → E3.3+E3.8）——均已于 2026-08-26/27 落地（映射按 roadmap §5 E3.1-E3.8 owner doc 归属）。
- **P2-C+P2-13 数据依赖矩阵漂移**：`docs/architecture/data-dependency-matrix.md:81` finance 行「master-data / purchase / sales / inventory / assets / projects（全部 R，经 I*Biz 只读查源单）」特征化失实——E3.5 `ErpFinApDocumentPipelineProcessor.draft()` 经 `IErpPurInvoiceBiz.save()` 创建采购发票草稿（fin→pur command 写边，架构合法：§「禁止反向 S 写」明示经 I*Biz command 编排为认可形态，期末结账同型），矩阵未更新；§2.4 缺两行：fin→md `ErpMdPartner` 只读边（`ErpFinApDocRuleClassifier` 查询）+ aps→mfg `IErpMfgCapacityProvider` SPI 边。
- **P2-2 G1 缺口清单不完备**：`docs/design/dashboard-semantic-layer.md` §4 缺口登记表 G1 行（:66）仅覆盖 `getDashboardKpi`/trend/topN/alerts 全族；同批姊妹 API `getInventorySnapshot` 直连 `ormTemplate.findListByQuery`（enableFilter=false 路径，E3.1b 实测同型）未收录——缺口登记不完备（修复本身归 permissions-enforcement G1 族，本批只补登记）。
- **P2-10 @Description 缺失 ×5**：`uploadApDocument`（`ErpFinApDocumentBizModel.java:32-38` + `IErpFinApDocumentBiz`）、`getInventorySnapshot`/`checkStockBalanceConsistency`（`ErpInvStockLedgerBizModel`）、`getAssetAuditTrail`（`ErpAstAssetBizModel`）、`scheduleToc`（`ErpApsOperationOrderBizModel`）仅 javadoc 不进 GraphQL schema description，违反 `ai-native-interface.md:72-73` item 2 登记的「新增对外 action 应带 @Description」约定（fin 侧活仓已核零 @Description；其余三域执行时 grep 复核）。
- **P2-12 悬空引用 ×2**：①`TestErpAiIntrospectionEnabled.java:39-41` 注释称 AST 单父约束 workaround「见 ai-native-interface.md 实现注记」——该文档无此注记（:70-76 最小落地集无实现注记节）；②plan `2026-08-26-0735-2` Phase 7 ②「11 个 getDashboardKpi 补 @Description（IBiz 同步，11 域 grep 核对）」——仓内不存在 `IErp*Dashboard*` 接口（活仓 grep 证实），「IBiz 同步」为空指。

## Goals

- `job-scheduling.md` 补 `erp-fin-ap-doc-processing` 注册行（对齐 :131 inv 行格式）。
- 6 处「暂不编码」陈旧标注更新为已实现状态（含 E3.x 与日期）。
- `data-dependency-matrix.md` finance 行特征修正（R-only → 含 fin→pur command 写边）+ §2.4 补 fin→md R 边与 aps→mfg SPI 边两行（P2-13 与 P2-C 合并一次收口）。
- `dashboard-semantic-layer.md` §4 G1 族补录 `getInventorySnapshot`（及同型直查 API 核查后一并补录）。
- 5 个对外 action 补 `@Description`（含 IBiz 接口同步，如接口存在）。
- 2 处悬空引用修正（测试注释指向真实位置或落实现注记；E3 计划 ② 处补归因注记）。
- 消费 roadmap §Follow-up Backlog 对应行并勾销登记。

## Non-Goals

- 不实现 G1 缺口修复（数据权限 enforcement 属 `permissions-enforcement-roadmap.md` 栈，本批只补登记行）。
- 不改任何业务行为/配置/ORM（唯一代码触点 = `@Description` 注解元数据；schema description 变化属可接受元数据面对齐，非行为变更）。
- P2-11 限流措辞（归 plan 1 fin 面）、P2-E 证据归因修正（归 plan 1）、P2-D（mission VERIFY 批）、P2-16/P2-F、E3.7。
- 不批量补齐全仓历史 action 的 @Description（只收口 E3 批新增 5 个——约定-实践漂移的最小闭合面）。

## Task Route

- Type: `documentation + metadata-alignment change`（文档注册批 + 微量元数据代码）
- Owner Docs: `docs/architecture/job-scheduling.md`、`docs/architecture/data-dependency-matrix.md`、`docs/design/dashboard-semantic-layer.md`、`docs/design/ai-native-interface.md`（本批的**修改对象**即 owner docs 本身）
- Skill Selection Basis: `nop-backend-dev`（仅 @Description 项——action 元数据/IBiz 同步约定在其范围内）；其余文档项 `Skill: none`（登记与措辞修正，无平台模式依赖）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 文档注册批（job 注册/路由标注/矩阵/G1 补录/悬空引用）

Status: planned
Targets: `docs/architecture/job-scheduling.md`、`docs/architecture/data-dependency-matrix.md`、`docs/design/README.md`、`docs/design/{finance,aps,inventory,assets}/README.md`、`docs/design/dashboard-semantic-layer.md`、`docs/design/ai-native-interface.md`（如落实现注记）、`app-erp-all/src/test/java/io/nop/app/all/it/TestErpAiIntrospectionEnabled.java`、`docs/plans/2026-08-26-0735-2-e3-integrated-implementation.md`（注记）
Skill: `none`

- Item Types: `Fix`
- Prereqs: none

- [ ] `Fix` P2-A：`job-scheduling.md` 补 `erp-fin-ap-doc-processing` 行（对齐 :131 inv 行八列格式：batch xml + job.yaml 三件套、config-gated 默认关、`nop.job.erp-fin-ap-doc-processing.enabled/.cron-expr` 部署键、owner doc `document-driven-ap-automation.md` 引用、WIRED(E3.5, 2026-08-27)）；§3.x 与批量归档表如涉及同步补。Skill: `none`
- [ ] `Fix` P2-B：6 处「暂不编码」→「已实现（E3.x，2026-08-26/27）」式更新，映射按 Current Baseline 所列 E3 归属逐处标注（design README 两行 → E3.6 / E3.1+E3.1b；finance → E3.5；aps → E3.4；inventory → E3.2；assets → E3.3+E3.8）。Skill: `none`
- [ ] `Fix` P2-C+P2-13：`data-dependency-matrix.md` finance 行（:81）R-only 特征修正（补「E3.5 经 `IErpPurInvoiceBiz.save()` command 写采购发票草稿」表述，对齐期末结账认可形态注记）；§2.4 补 fin→md `ErpMdPartner` R 边行 + aps→mfg `IErpMfgCapacityProvider` SPI 边行（格式对齐既有矩阵行）。Skill: `none`
- [ ] `Fix` P2-2：`dashboard-semantic-layer.md` §4 G1 行补录 `getInventorySnapshot`（直连 ormTemplate、enableFilter=false 同型）；同批直查 API（`checkStockBalanceConsistency` 等）逐一核查，同型绕过者一并补录并注明「修复随 G1 族同机制收口」。Skill: `none`
- [ ] `Fix` P2-12①：`TestErpAiIntrospectionEnabled.java:39-41` 注释与文档对齐——在 `ai-native-interface.md` 落「实现注记」（AST 单父约束 workaround 说明）或改注释为自包含表述（择一，倾向前者——平台约束值得登记）。Skill: `none`
- [ ] `Fix` P2-12②：plan `2026-08-26-0735-2` Phase 7 ② 处补注记（「IBiz 同步」为空指——11 域无 `IErp*Dashboard*` 接口，实际 = BizModel 方法注解；归因修正不改变原验收结论；E3 批新增 action 的 @Description 欠账由本计划 Phase 2 收口）。注记须显式标记 post-hoc 并引用本计划 plan-id。Skill: `none`

Exit Criteria:

- [ ] `grep erp-fin-ap-doc-processing job-scheduling.md` 命中注册行；两部署键在案
- [ ] `grep -rn 暂不编码 docs/design/README.md docs/design/{finance,aps,inventory,assets}/README.md` 对应 6 行更新（历史批次的合理保留行不误改——逐行人工判定）
- [ ] 矩阵 finance 行含 command 写边表述；§2.4 新增两行存在
- [ ] G1 行含 `getInventorySnapshot`；同型 API 核查结论记录（补录或排除理由）
- [ ] P2-12① 可 grep：`ai-native-interface.md` 含「实现注记」节 或 测试注释不再指向不存在内容
- [ ] P2-12② 可 grep：plan 0735-2 Phase 7 ② 处存在引用本计划的 post-hoc 注记

### Phase 2 - @Description 补齐 + 收口登记

Status: planned
Targets: fin/inv/ast/aps 四域 BizModel（+ 存在的 IBiz 接口）、`docs/backlog/erp-enhancement-roadmap.md`、`docs/logs/2026/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof | Fix`
- Prereqs: Phase 1

- [ ] `Add` P2-10：5 个 action 补 `@Description`（中文描述，进 GraphQL schema description；`uploadApDocument` 同步 `IErpFinApDocumentBiz` 接口方法——其余三域按活仓 grep 结果同步存在的 IBiz）。Skill: `nop-backend-dev`
- [ ] `Proof` scoped 验证：四域模块 `mvn compile`/`mvn test -pl`（轻量：受影响模块编译 + 既有测试回归；@Description 为元数据面，introspection 行为由既有 `TestErpAiIntrospectionEnabled` 家族覆盖）；如变更测试注释则复跑该测试类。Skill: `nop-testing`
- [ ] `Fix` roadmap §Follow-up Backlog 勾销：P2-A/P2-B/P2-C/P2-13/P2-2/P2-10/P2-12 行，附本计划引用与日期；compliance checker 复跑（actual ≤ baseline，@Description 非生产逻辑站点，预期零漂移）。Skill: `none`
- [ ] `Proof` 日志条目（`docs/logs/2026/` 执行当日，含验证状态）。Skill: `none`

Exit Criteria:

- [ ] 5 action 的 GraphQL schema description 可观测（introspection 输出或编译产物注解存在性证明）
- [ ] roadmap 对应行勾销、checker actual ≤ baseline、日志落盘

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is（5 MINOR 采纳修订）**（task `ses_fbb89f5e3ffeY9WCHTv3yWF12T`，fresh session）——11 组基线主张活仓复核全吻合（含：生产 job 三件套存在于 `app-erp-all/_vfs/nop/job/conf/erp-fin-ap-doc-processing.job.yaml`；6 处「暂不编码」逐行核实；矩阵 finance 行原文核实；5 action 的 IBiz 接口**全部存在**（IErpFinApDocumentBiz:21 等）——原「如接口存在」措辞实际全域可满足；`interface IErp*Dashboard` 全仓 grep 零命中证实空指；修改已完成计划 0735-2 属可接受先例）。5 MINOR 已修：①Source 归因措辞修正（「文档注册批」grouping 仅 open-audit 路由第 1 条，multi-audit 路由为分流条目）；②inventory README 映射更正为 E3.2（原 E3.2+E3.3/E3.4 两处均错，按 roadmap §5 owner doc 归属重标）；③Phase 1 退出标准补 P2-12①/② 两条可 grep 检查；④Phase 2 Item Types 补 `Fix`；⑤post-hoc 注记显式标记引用本计划 plan-id。

## Closure Gates

- [ ] 范围内行为完成（Phase 1/2 全部项目落地）
- [ ] 相关文档对齐（本批修改对象即 owner docs——Phase 1 Exit Criteria 覆盖）
- [ ] 已运行验证（受影响模块 scoped 编译/测试 + compliance checker；文档项以 grep 证明）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

（无——G1 修复归属在 Non-Goals 显式声明为 permissions-enforcement 栈，非本批范围裁剪）

## Closure

Status Note: （待执行后填写）

Closure Audit Evidence:

- Auditor / Agent: （待独立结束审计）
- Evidence: （待填写）

Follow-up:

- （无预留）
