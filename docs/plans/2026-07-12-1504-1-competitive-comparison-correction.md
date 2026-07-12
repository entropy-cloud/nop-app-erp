# 2026-07-12-1504-1-competitive-comparison-correction 竞品对标文档勘误 + 能力缺口裁决

> Plan Status: completed
> Last Reviewed: 2026-07-12
> Source: `docs/audits/2026-07-12-1504-competitive-levers-implementation-audit.md`
> Related: `docs/architecture/competitive-comparison.md`
> Audit: required

## Current Baseline

实现层审计（`docs/audits/2026-07-12-1504-competitive-levers-implementation-audit.md`，HEAD=6d34e665）核实了 competitive-comparison.md §四 的 8 个杠杆声明。核实结论与当前文档的差异（均为已确认的 owner-doc 漂移，属**不可降级项**，见计划指南规则 13）：

- **H-A 实体/文件计数错误（3 处互相冲突）**：
  - `competitive-comparison.md:84` 写"**10 份** orm.xml，共 **145 实体**"
  - `competitive-comparison.md:165`（§五 MixERP 行）写"全部 18 域 **447 实体**"
  - 实测（Python 解析全部 `module-*/model/app-erp-*.orm.xml`）：**19 份** orm.xml，**352 个自有实体**（生成）+ **110 个跨域 `notGenCode` 引用桩** = 462 个 `<entity>` 元素
- **H-G "每个域可独立 Maven 工程、独立部署"夸大**：`competitive-comparison.md:142`（杠杆 G）。实测 `-service` 层存在大量编译期跨域依赖（`inventory-service → finance-service` 等），域**不可独立构建部署**。（注：文档另一句"无循环依赖"经实测**正确**——`-service→-dao` 单向、Maven reactor 无环、`app-erp-all` 构建成功，无需改。）
- **H-C "多公司"行为层缺失未在文档披露**：`competitive-comparison.md:100-102`（杠杆 C）+ §二总览表"多公司=是"。实测 `orgId` 仅为物理列 + 字典 + 树（结构地基），**无自动 org 数据过滤/权限范围/公司间交易逻辑**（`orgId.*filter|scopeByOrg` 零命中，零行为测试）。§六诚实声明未列此项。
- **M-4 未区分平台继承 vs 本项目落地**：杠杆 A 的 Delta 能力主要**继承自 Nop 平台**（本项目通过 338 个 `x:extends="_gen/` view 演示使用），文档表述易被读作本项目原创。

已确认为**代码缺口**（非文档问题）的三项，需裁决是否本计划范围内修复还是移交 successor：

- **M-1 制造委外空壳**：`ErpMfgSubcontractOrderBizModel.java` 仅 15 行 CRUD，杠杆 F 声明"委外"未兑现（`MrpReleaseService:49` 明示本期不支持）。
- **M-2 成本滚算 overhead/subcontract 恒 0**：`CostRollupService` 材料+人工已实、制造费用+委外未拆（已有文档化 Follow-up）。
- **M-3 核销时点 fxGainLoss 硬编码 ZERO**：`ErpFinReconciliationBizModel`，期末重估由 `ExchangeRevaluationService` 承载。

## Goals

- 修正 competitive-comparison.md 中 3 处已确认的错误/夸大陈述（H-A / H-G / H-C），使文档与实时代码一致。
- 明确区分"Nop 平台继承能力"与"本项目已落地演示"（M-4）。
- 对 3 项代码缺口（M-1/M-2/M-3）做出显式裁决：范围内修复 or 移交命名 successor（不得静默遗留）。

## Non-Goals

- **不修复任何代码缺口**（M-1 委外引擎 / M-2 成本要素拆分 / M-3 动态汇兑损益）——本计划仅做文档勘误 + 裁决。代码修复若裁决为范围内，由独立 successor 计划承接。
- 不改 competitive-comparison.md 中经实测**正确**的声明（多套账/7 成本方法/业财一体/open-item 核销/无循环依赖）。
- 不改 orm.xml 模型、不改 Java、不改测试（纯文档变更）。

## Task Route

- Type: `app-layer design change`（owner doc 勘误，属 `docs/architecture/`）
- Owner Docs: `docs/architecture/competitive-comparison.md`（主）；证据源 `docs/audits/2026-07-12-1504-competitive-levers-implementation-audit.md`
- Skill Selection Basis: 无匹配技能——本任务是架构文档勘误 + 裁决记录，非 ORM 建模 / BizModel 编写 / view.xml 页面 / 测试类编写。`docs/skills/README.md` 下技能均为代码/模型方法选择器，与纯文档勘误不匹配。`Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯文档变更，无端口/环境变量/迁移）。

## Execution Plan

### Phase 1 — competitive-comparison.md 事实勘误

Status: completed
Targets: `docs/architecture/competitive-comparison.md`
Skill: `none`

- Item Types: `Fix`（3/3 为已确认 owner-doc 漂移修复，不可降级）

- [x] **Fix H-A（计数）**：将 §四杠杆 A（L84）"10 份 orm.xml，共 145 实体"与 §五 MixERP 行（L165）"全部 18 域 447 实体"统一勘误为实测值："**19 份** `app-erp-<domain>.orm.xml`，**352 个自有实体**（另 110 个跨域 `notGenCode` 引用桩，合计 462 个 `<entity>` 元素）"。§二总览与其他引用同数字处一并核查对齐。
- [x] **Fix H-G（独立部署夸大）**：§四杠杆 G（L142）"每个域可独立 Maven 工程、独立组装裁剪"弱化为如实表述——"域间无 ORM 强引用（走 `I*Biz` 接口）、无循环依赖（`-service→-dao` 单向，Maven reactor 无环），为未来微服务化奠定结构基础；当前 `-service` 层仍有编译期跨域依赖，尚不可单域独立部署"。保留"无循环依赖"（实测正确）。
- [x] **Fix H-C（多公司行为层）**：§四杠杆 C（L100-102）如实补注"当前 `orgId` 为物理列 + 组织树结构基础，自动 org 数据过滤 / 权限范围 / 公司间交易行为层尚未实现"；§二总览表"多公司"行由无条件"是"改为带限定说明；§六"诚实声明"新增一条"多组织行为层（数据隔离/权限范围/公司间）待建"。
- [x] **Fix M-4（继承 vs 落地）**：§四杠杆 A 补注"模型驱动 + Delta 分层能力继承自 Nop 平台；本项目通过 338 个手写 `.view.xml` 以 `x:extends=\"_gen/_*.view.xml\"` + `x:override` 演示 Delta 定制"。

Exit Criteria:

- [x] competitive-comparison.md 中 H-A/H-G/H-C/M-4 四处均已按实测值修正，且文档内部无残留冲突数字（145/447 不再共存）。
- [x] 审计报告 §五问题清单与勘误后文档一致（数字、措辞可交叉核对）。

### Phase 2 — 代码缺口裁决（M-1 / M-2 / M-3）

Status: completed
Targets: 本计划 `## Deferred But Adjudicated` + `docs/backlog/README.md`（如裁决为 successor）
Skill: `none`

- Item Types: `Decision`

- [x] **Decision M-1（委外引擎）**：裁决制造委外是"移交命名 successor（补委外单生命周期 + 出入库 + 委外费用过账）"还是"降级为 watch-only（当前无委外业务需求）"。记录选择、替代方案、残留风险；若移交 successor 则在 `docs/backlog/README.md` 立项。
  - Skill: `none`（裁决=**successor**：模型实体就位 + 文档杠杆 F 声明已交付 + Odoo/ERPNext/iDempiere 制造标配；已在 backlog README P8 立项「制造委外引擎 + 成本要素拆分（M-1+M-2 successor）」）
- [x] **Decision M-2（成本要素拆分）**：裁决 overhead/subcontract 成本要素拆分（当前恒 0）为 successor 或 residual-risk（已有文档化 Follow-up，与 M-1 委外强相关，建议合并）。记录理由。
  - Skill: `none`（裁决=**successor，与 M-1 合并**：subcontract 要素强依赖 M-1 委外费归集源，采纳审计建议合并；已在 backlog README 同一 P8 行）
- [x] **Decision M-3（动态汇兑损益）**：裁决核销时点 `fxGainLoss` 计算为 successor 或 residual-risk（期末 `ExchangeRevaluationService` 已部分覆盖，多币种核销场景才触发）。记录理由 + 重开触发条件。
  - Skill: `none`（裁决=**watch-only residual**：期末重估已部分覆盖，仅多币种核销触发，无现行需求；重开触发=出现多币种核销业务需求，已命名于 backlog watch-only residual 行 + 本计划 Deferred But Adjudicated）

Exit Criteria:

- [x] M-1/M-2/M-3 各自恰好处于一种状态（successor-owned / residual-risk-only），无模糊遗留；successor 项均在 `docs/backlog/README.md` 或本计划 `Deferred But Adjudicated` 命名触发条件。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (draft-review pass, 2026-07-12) because all required sections/fields present; cited line numbers (L84/L142/L100-102/L165) verified against live `competitive-comparison.md`; load-bearing Source (`docs/audits/2026-07-12-1504-competitive-levers-implementation-audit.md`) and Targets exist; scope doc-only + adjudication is coherent single result surface; exit criteria testable; closure gates correctly customized for doc-only plan with stated reason. No Blocker/Major found. Minor (non-blocking, left for executor): Phase 1 items omit per-item `Skill:` (covered by phase-level `Skill: none`); `Deferred But Adjudicated` entries pre-labeled "待 Phase 2 裁决" are reconciled by Phase 2 exit criterion enforcing exactly-one-state.

## Closure Gates

> 本计划为**纯文档变更 + 裁决**，无代码，故删除 build/test 验证门控，改为文档一致性 + 链接核查。

- [x] 范围内行为完成（H-A/H-G/H-C/M-4 勘误落地 + M-1/M-2/M-3 裁决记录）
- [x] 相关文档对齐（competitive-comparison.md ↔ 审计报告 ↔ backlog successor 一致）
- [x] 无范围内项目降级为 deferred/follow-up（勘误项为不可降级 Fix，已全部落地）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status / Phase Status / Exit Criteria / Closure Gates / `docs/logs/2026/07-12.md` 条目一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于本文件 `## Closure`

## Deferred But Adjudicated

### M-1 制造委外引擎

- Classification: `successor`（裁决：移交命名 successor 补全委外生命周期）
- 裁决理由：模型实体已就位（`ErpMfgSubcontractOrder/Line`），`competitive-comparison.md` 杠杆 F 声明"委外"为已交付能力（实测 `ErpMfgSubcontractOrderBizModel.java` 仅 15 行 CRUD 空壳，`MrpReleaseService:49` 明示本期不支持）。委外是 Odoo/ERPNext/iDempiere 制造标配，结构地基完备，属"承诺 > 实现"缺口而非无业务需求。降级 watch-only 需同时撤回文档杠杆 F 委外声明 + 丢失一项标准能力，代价大于补全，故选 successor。
- 替代方案（已否决）：(a) 降级为 watch-only——否决，原因如上；(b) 本计划范围内修复——否决，本计划 Non-Goal 明确不修代码。
- 残留风险：补全前文档杠杆 F "委外"仍为结构地基（模型存在）+ 空壳（无业务逻辑），已由本次 §四杠杆 F 未在勘误范围（杠杆 F 经审计为"大部分兑现"，委外为唯一空壳子项）+ successor 立项覆盖。
- Successor Required: `docs/backlog/README.md` P8 「制造委外引擎 + 成本要素拆分（M-1+M-2 successor）」，路线图引用 `2026-07-12-1504-1`

### M-2 成本要素拆分（overhead/subcontract）

- Classification: `successor`（裁决：与 M-1 合并 successor）
- 裁决理由：`CostRollupService` 材料+人工已实，制造费用（overhead）分配率应用 + 委外费（subcontract）归集恒 0（已有文档化 Follow-up）。subcontract 要素强依赖 M-1 委外引擎（需委外费归集源），与 M-1 合并可一次贯通"委外单→委外费→成本滚算 subcontract 列"；overhead 要素（工作中心费率 × 工时）可独立但同属成本滚算完整性，合并不增复杂度。故采纳审计建议合并 M-1 successor。
- 替代方案（已否决）：独立 overhead successor——否决，拆分会割裂成本滚算完整性且 subcontract 仍需等 M-1。
- 残留风险：补全前 `ErpMfgCostRollup/Line` 的 overhead/subcontract 金额列恒 0（材料+人工列有效），成本滚算部分有效，由 successor 覆盖。
- Successor Required: 同 M-1（合并 successor），`docs/backlog/README.md` P8 「制造委外引擎 + 成本要素拆分（M-1+M-2 successor）」

### M-3 核销时点动态汇兑损益

- Classification: `watch-only residual`（裁决：残留风险，不立项；命名重开触发条件）
- 裁决理由：核销时 `fxGainLoss` 硬编码 ZERO（`ErpFinReconciliationBizModel`），但期末汇兑重估由 `ExchangeRevaluationService` 承载已部分覆盖（期末按即期汇率重估 AR/AP 余额）。核销时点动态汇兑损益仅在**多币种核销**场景（核销日即期汇率与发票/收付款入账历史汇率不同）才触发；当前单币种核销场景不受影响。无现行多币种核销业务需求，故 watch-only 而非立项。
- 重开触发条件：出现多币种核销业务需求（核销时点需动态计算汇兑损益）。触发后在 `docs/backlog/README.md` 升级为 successor。
- Successor Required: 无（watch-only residual）；重开触发条件已命名于上一行 + `docs/backlog/README.md` watch-only residual 行

## Closure

Status Note: 全部工作完成。Phase 1 四处文档勘误（H-A/H-G/H-C/M-4）已落地 `docs/architecture/competitive-comparison.md`，文档内部无残留冲突数字或夸大措辞；Phase 2 三项代码缺口裁决（M-1/M-2 successor 合并立项 / M-3 watch-only residual）已记录于本计划 `## Deferred But Adjudicated` + `docs/backlog/README.md`。独立结束审计 PASS（CLOSURE WARRANTED，0 Blocker / 0 Major）。纯文档 + 裁决，零代码/ORM/测试变更，Non-Goals 守约。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 `ses_0a7f4fc0affeqO6sQnF2t7qSoN`，general，无执行者上下文）
- Verdict: PASS / CLOSURE WARRANTED（0 Blocker / 0 Major / 2 非阻塞 Minor）
- Evidence:
  - H-A：`rg "145|447|10 份"` competitive-comparison.md = 0 命中；L84/L167 数字经独立 Python 重测（DOTALL-aware regex 全 19 orm.xml）= 19/352/110/462 精确匹配。
  - H-G：`rg "可独立部署|独立 Maven|可独立组装|每个域可独立"` = 0 命中；L144/L146 如实表述「尚不可单域独立构建部署」；「无循环依赖」保留（L144）。
  - H-C：三处确认——L104 杠杆 C 诚实补注 + L40 §二总览多公司行限定 + L180 §六诚实声明新第 6 条。
  - M-4：L85「平台继承 vs 本项目落地」区分 Nop 平台继承 vs 338 view.xml 演示。
  - Phase 2：M-1=successor（plan L106-112 + backlog L65 P8 ready）/ M-2=successor 合并 M-1（plan L114-120 + backlog L65）/ M-3=watch-only residual 含命名重开触发（plan L122-127 + backlog L66）；各恰好一种状态。
  - 无禁改：`git status --short` + `git diff --stat` = 恰好 4 文件（competitive-comparison.md / backlog README.md / 本计划 / logs/07-12.md），零 .java/.xml(orm/api/xbiz/view)/test 变更，Non-Goals 守约。
  - 跨文档对齐：审计报告 §五问题清单与勘误后文档一致；logs/2026/07-12.md 含本计划执行条目。
  - Minor（非阻塞）：backlog L66 `watch-only residual` 状态值不在标准枚举内（L79-82），系刻意裁决标记、行文自解释；competitive-comparison.md §七结论 L188「独立演进、独立替换」为结构性声明（接口解耦支持替换域实现），区别于 H-G 标记的「独立部署」夸大，可接受。

Follow-up:

- M-1+M-2 successor（`docs/backlog/README.md` P8「制造委外引擎 + 成本要素拆分」）：补全前 competitive-comparison.md 杠杆 F「委外」仍为结构地基 + 空壳，由 successor 覆盖（非本计划范围内缺陷）。
- M-3 重开触发：出现多币种核销业务需求时在 backlog 升级为 successor（当前 watch-only residual，非阻塞）。
