# 2026-07-12-1504-1-competitive-comparison-correction 竞品对标文档勘误 + 能力缺口裁决

> Plan Status: active
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

Status: planned
Targets: `docs/architecture/competitive-comparison.md`
Skill: `none`

- Item Types: `Fix`（3/3 为已确认 owner-doc 漂移修复，不可降级）

- [ ] **Fix H-A（计数）**：将 §四杠杆 A（L84）"10 份 orm.xml，共 145 实体"与 §五 MixERP 行（L165）"全部 18 域 447 实体"统一勘误为实测值："**19 份** `app-erp-<domain>.orm.xml`，**352 个自有实体**（另 110 个跨域 `notGenCode` 引用桩，合计 462 个 `<entity>` 元素）"。§二总览与其他引用同数字处一并核查对齐。
- [ ] **Fix H-G（独立部署夸大）**：§四杠杆 G（L142）"每个域可独立 Maven 工程、独立组装裁剪"弱化为如实表述——"域间无 ORM 强引用（走 `I*Biz` 接口）、无循环依赖（`-service→-dao` 单向，Maven reactor 无环），为未来微服务化奠定结构基础；当前 `-service` 层仍有编译期跨域依赖，尚不可单域独立部署"。保留"无循环依赖"（实测正确）。
- [ ] **Fix H-C（多公司行为层）**：§四杠杆 C（L100-102）如实补注"当前 `orgId` 为物理列 + 组织树结构基础，自动 org 数据过滤 / 权限范围 / 公司间交易行为层尚未实现"；§二总览表"多公司"行由无条件"是"改为带限定说明；§六"诚实声明"新增一条"多组织行为层（数据隔离/权限范围/公司间）待建"。
- [ ] **Fix M-4（继承 vs 落地）**：§四杠杆 A 补注"模型驱动 + Delta 分层能力继承自 Nop 平台；本项目通过 338 个手写 `.view.xml` 以 `x:extends=\"_gen/_*.view.xml\"` + `x:override` 演示 Delta 定制"。

Exit Criteria:

- [ ] competitive-comparison.md 中 H-A/H-G/H-C/M-4 四处均已按实测值修正，且文档内部无残留冲突数字（145/447 不再共存）。
- [ ] 审计报告 §五问题清单与勘误后文档一致（数字、措辞可交叉核对）。

### Phase 2 — 代码缺口裁决（M-1 / M-2 / M-3）

Status: planned
Targets: 本计划 `## Deferred But Adjudicated` + `docs/backlog/README.md`（如裁决为 successor）
Skill: `none`

- Item Types: `Decision`

- [ ] **Decision M-1（委外引擎）**：裁决制造委外是"移交命名 successor（补委外单生命周期 + 出入库 + 委外费用过账）"还是"降级为 watch-only（当前无委外业务需求）"。记录选择、替代方案、残留风险；若移交 successor 则在 `docs/backlog/README.md` 立项。
  - Skill: `none`
- [ ] **Decision M-2（成本要素拆分）**：裁决 overhead/subcontract 成本要素拆分（当前恒 0）为 successor 或 residual-risk（已有文档化 Follow-up，与 M-1 委外强相关，建议合并）。记录理由。
  - Skill: `none`
- [ ] **Decision M-3（动态汇兑损益）**：裁决核销时点 `fxGainLoss` 计算为 successor 或 residual-risk（期末 `ExchangeRevaluationService` 已部分覆盖，多币种核销场景才触发）。记录理由 + 重开触发条件。
  - Skill: `none`

Exit Criteria:

- [ ] M-1/M-2/M-3 各自恰好处于一种状态（successor-owned / residual-risk-only），无模糊遗留；successor 项均在 `docs/backlog/README.md` 或本计划 `Deferred But Adjudicated` 命名触发条件。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (draft-review pass, 2026-07-12) because all required sections/fields present; cited line numbers (L84/L142/L100-102/L165) verified against live `competitive-comparison.md`; load-bearing Source (`docs/audits/2026-07-12-1504-competitive-levers-implementation-audit.md`) and Targets exist; scope doc-only + adjudication is coherent single result surface; exit criteria testable; closure gates correctly customized for doc-only plan with stated reason. No Blocker/Major found. Minor (non-blocking, left for executor): Phase 1 items omit per-item `Skill:` (covered by phase-level `Skill: none`); `Deferred But Adjudicated` entries pre-labeled "待 Phase 2 裁决" are reconciled by Phase 2 exit criterion enforcing exactly-one-state.

## Closure Gates

> 本计划为**纯文档变更 + 裁决**，无代码，故删除 build/test 验证门控，改为文档一致性 + 链接核查。

- [ ] 范围内行为完成（H-A/H-G/H-C/M-4 勘误落地 + M-1/M-2/M-3 裁决记录）
- [ ] 相关文档对齐（competitive-comparison.md ↔ 审计报告 ↔ backlog successor 一致）
- [ ] 无范围内项目降级为 deferred/follow-up（勘误项为不可降级 Fix，已全部落地）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status / Phase Status / Exit Criteria / Closure Gates / `docs/logs/2026/07-12.md` 条目一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于本文件 `## Closure`

## Deferred But Adjudicated

### M-1 制造委外引擎

- Classification: `out-of-scope improvement`（待 Phase 2 Decision 最终裁定 successor or watch-only）
- Why Not Blocking Closure: 本计划仅勘误文档 + 裁决；委外引擎实现是独立代码工作，不阻塞文档一致性结束。
- Successor Required: `待 Phase 2 裁决`

### M-2 成本要素拆分（overhead/subcontract）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 同上；与 M-1 强相关，建议合并 successor。
- Successor Required: `待 Phase 2 裁决`

### M-3 核销时点动态汇兑损益

- Classification: `watch-only residual`
- Why Not Blocking Closure: 期末 `ExchangeRevaluationService` 已部分覆盖；仅多币种核销场景触发。
- Successor Required: `待 Phase 2 裁决（重开触发：出现多币种核销业务需求）`

## Closure

Status Note: <待结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立子代理>
- Evidence: <task id / walkthrough>

Follow-up:

- <仅非阻塞跟进；已确认缺陷不得列此>
