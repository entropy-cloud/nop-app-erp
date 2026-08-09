# 2026-08-09-1314-3 procurement-confidentiality-q1q4-adjudication

> Plan Status: active
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P1.2
> Related: `docs/discussions/2026-08-05-1800-ai-mfg-rd-bom-and-procurement-confidentiality.md` §未解决问题；mission `permissions-enforcement`
> Audit: required
> Mission: permissions-enforcement
> Work Item: P1.2

## Current Baseline

讨论文档 `docs/discussions/2026-08-05-1800-ai-mfg-rd-bom-and-procurement-confidentiality.md` §未解决问题列出 4 个未裁决项，其中 **Q1/Q4 为本路线图 E3.2/E4.x 硬前置**：

- **Q1（成本聚合可见度的粒度）**：研发/AI 视图看到「按成本要素的成本区间」/「标准成本总额」/「仅相对高低（高/中/低）」？——影响新权限控制点的字段范围。
- **Q4（保密与成本滚算的冲突处理）**：`CostRollupService` 需读 `purchasePrice`/供应商价以算制造件成本——聚合成本**由服务内部跨域取值**，取值的可见性边界与「研发角色不可见」如何共存（服务端计算不经由研发角色查询）。

**已实测定位的相关实现**（不动业务逻辑，仅作裁决输入）：

- `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/costing/CostRollupService.java` —— 标准成本滚算服务，服务端跨域取值。
- `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/StandardCostResolver.java` —— STANDARD 成本方法读 `ErpMfgCostRollupLine.unitCost`（`docs/design/finance/costing-methods.md` §STANDARD）。
- 保密取值源：`ErpMdMaterialSku.purchasePrice`（master-data）+ `ErpPurSupplierPriceList`（purchase）——均为 P1.1 供应商价格面字段。

**关键事实**（讨论文档 §讨论点二/四）：服务端计算是**角色无关**的——`CostRollupService` 用系统/服务上下文取值，不经由研发角色的查询路径，故「研发角色不可见」与「成本可滚算」在服务端本就共存；Q4 的真实矛盾在于**研发角色通过何种视图消费聚合结果**（直读 unitCost 字段 vs 代理视图），而非取值本身。

**Q2/Q3**（候选 BOM 建模位置 / AI provenance 字段集）属 AI 研发轨道，本计划**仅判归属域**（预计 manufacturing 域 successor），不实施。

**缺口**：Q1/Q4 未裁决 → E3.2（取值豁免裁决）与 E4.1（字段级可见性）保持 todo（横切关注点 6）；无裁决记录 → 采购保密字段级 enforcement 无法进入。

## Goals

- **裁决 Q1**：选定研发/AI 视图的成本聚合可见粒度（成本要素区间 / 标准成本总额 / 相对高中低 三选一或组合），并指出其对 E4.1 字段范围与新权限控制点的影响。记录替代方案与残留风险。
- **裁决 Q4**：选定成本滚算跨域取值的可见性边界方案——确认"服务端取值角色无关、本就共存"事实，并裁决研发角色消费聚合结果的路径（直读字段受 E4.1 字段级隐藏 vs 经代理视图/E3.x 聚合值代理）。为 E3.2（`CostRollupService` 取值豁免）提供冻结输入。
- **裁决 Q2/Q3 归属域**：判定候选 BOM 建模 + AI provenance 归 manufacturing 域 successor（仅归属判定，不实施、不设计字段集）。
- **落盘裁决**：将 Q1/Q4 裁决写入讨论文档 §未解决问题（标记 Q1/Q4 resolved）+ `docs/design/finance/costing-methods.md`（取值豁免边界）交叉引用；Q2/Q3 归属写入讨论文档。

## Non-Goals

- **不实施**代理视图或取值豁免代码（归 E3.2）。
- **不实施**字段级可见性（meta published/queryable，归 E4.1）。
- **不设计** AI 候选 BOM 模型 / provenance 字段集（Q2/Q3 仅判归属域，实施归 manufacturing 域独立 successor）。
- **不改** `CostRollupService` / `StandardCostResolver` 业务逻辑（财务/成本代码为保护区域，横切关注点 1，只做取值豁免/脱敏裁决）。
- **不改 ORM**（`ErpMdMaterialSku.purchasePrice` 等不动）。

## Task Route

- Type: `requirement clarification`（采购保密裁决 + owner doc 回写，产决策非代码）
- Owner Docs: `docs/discussions/2026-08-05-1800-ai-mfg-rd-bom-and-procurement-confidentiality.md` §未解决问题；`docs/design/finance/costing-methods.md`（取值豁免边界）；`docs/design/manufacturing/bom-and-routing.md`（Q2/Q3 归属参照）
- Skill Selection Basis: `none` —— 纯业务/安全裁决与文档回写，不动成本服务代码/模型；与 roadmap 表格 P1.2 Skill 列一致。触及成本代码区域时仅读作裁决输入（保护区域 ask-first 在 E3.2，本计划不触代码）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（只读成本服务/讨论文档作裁决输入，不改运行时）。

## Execution Plan

### Phase 1 - Q4 成本滚算取值豁免边界裁决

Status: planned
Targets: `docs/discussions/2026-08-05-1800-...md` §未解决问题 Q4、`docs/design/finance/costing-methods.md`
Skill: none

- Item Types: `Decision` / `Proof`
- Prereqs: 无（Q4 独立于 Q1）

- [ ] **Proof**：核实并记录"服务端取值角色无关"事实——`CostRollupService` / `StandardCostResolver` 是 **non-BizModel 直接 DAO 消费者**（用 `IDaoProvider`/`IOrmTemplate`，无 `IContext`/user-context 注入，引用 java 文件 Javadoc 行号），而 Nop 的字段级可见性（meta `published`/`queryable`，E4.1）与行级 data-auth（`nopDataAuthChecker`）在 **BizModel/GraphQL 边界**强制——故服务内部跨域取值（如 `defaultSkuPurchasePrice` → `ErpMdMaterialSku.purchasePrice`）**架构性豁免**这些检查。**须区分触发与读取**：`rollupCost` 由用户经 BizModel *触发*，但*取值*不遍历该用户的查询路径——这是 Q4 裁决的事实基础（引用 costing-methods.md §STANDARD）。
  - Skill: none
- [ ] **Decision**：裁决 Q4 可见性边界方案。考虑的替代方案：(a) 研发角色直读 `unitCost`/成本字段，由 E4.1 字段级隐藏阻断（评估：与服务端取值不冲突，但研发若需看聚合成本则需代理）；(b) 研发角色经代理视图/E3.x 聚合值代理消费（评估：明细供应商数据不离开采购/财务域，符合讨论点二"聚合值代理"原则）；(c) 混合（服务端取值豁免 + 研发侧代理视图）。选定方案 + 残留风险记录（如选 (c)，E3.2 取值豁免须 plan-first 证据）。
  - Skill: none

Exit Criteria:

- [ ] Q4 裁决落地，含"服务端取值角色无关"事实证据 + 选定方案 + 替代方案 + 残留风险，可作为 E3.2 冻结输入。

### Phase 2 - Q1 成本聚合可见粒度裁决

Status: planned
Targets: `docs/discussions/2026-08-05-1800-...md` §未解决问题 Q1
Skill: none

- Item Types: `Decision`
- Prereqs: Phase 1（Q4 方案影响 Q1 粒度选择——若 Q4 选代理视图，Q1 粒度即代理视图输出）

- [ ] **Decision**：裁决 Q1 研发/AI 视图成本聚合可见粒度。考虑的替代方案：(a) 按成本要素的成本区间（材料/人工/制造费用/委外各区间）；(b) 标准成本总额（仅 totalCost/unitCost）；(c) 仅相对高低（高/中/低离散档位）；(d) 组合。选定方案 + 对 E4.1 字段范围与新权限控制点字段集的影响 + 残留风险记录。
  - Skill: none

Exit Criteria:

- [ ] Q1 裁决落地，含选定粒度 + 对字段范围影响 + 替代方案 + 残留风险，可作为 E4.1 冻结输入。

### Phase 3 - Q2/Q3 归属判定与裁决落盘

Status: planned
Targets: `docs/discussions/2026-08-05-1800-...md` §未解决问题 Q2/Q3、§讨论点一
Skill: none

- Item Types: `Decision` / `Add`
- Prereqs: Phase 1 + Phase 2

- [ ] **Decision**：判定 Q2（候选 BOM 建模位置）/Q3（AI provenance 字段集）归属 manufacturing 域 successor（仅归属判定，不设计字段集、不实施）。考虑的替代归属：crm / logistics / manufacturing —— 选 manufacturing，因其拥有 `ErpMfgBom` 权威源，provenance 不宜混入生产实体（讨论点一已倾向独立 candidate 头依附正式 BOM）。残留风险：若 AI 研发管道最终跨域（如 crm 选配），归属可能 revisited。
  - Skill: none
- [ ] **Add**：将 Q1/Q4 裁决写入讨论文档 §未解决问题（标记 Q1/Q4 resolved + 交叉引用 costing-methods.md），Q2/Q3 归属写入 §讨论点一/未解决问题。
  - Skill: none
- [ ] **Add**：在 `docs/design/finance/costing-methods.md` 增加 Q4 取值豁免边界交叉引用（指向讨论文档裁决），为 E3.2 plan-first 提供锚点。
  - Skill: none

Exit Criteria:

- [ ] 讨论文档 Q1/Q4 标记 resolved 且裁决可被 E3.2/E4.1 直接引用；Q2/Q3 归属判定落盘；costing-methods.md 交叉引用落地。

## Draft Review Record

- Independent draft review iteration 1: accept（0 blocker / 0 major / 3 minor）（ses_01b0b6ff7ffeDJTP7YxZJJUfd）。minor：Phase 1 Proof 须阐明"角色无关"的架构根因（non-BizModel 直接 DAO 消费 + BizModel 边界强制 + 触发 vs 读取区分）；E3.2 Deferred 分类标签 `optimization candidate` 对"必需 successor"略误；Q2/Q3 归属 Decision 缺显式替代方案。三 minor 均非阻塞，可顺带修；其中 minor 1（Proof 根因）建议执行前收紧以保 Q4 airtight。
- 合并修订（iteration 1 → v2，吸收 minor）：Phase 1 Proof 扩写为架构性豁免证据（non-BizModel DAO 消费 + BizModel 边界 + 触发/读取区分）；E3.2 分类标签补"必需 successor, trigger-locked"澄清；Q2/Q3 Decision 补替代归属（crm/logistics/manufacturing）+ 残留风险。裁决内容与基线无变化。
- Independent draft review iteration 2: 不需要（iteration 1 即 accept，本轮仅吸收非阻塞 minor，无结构性变更）。

## Closure Gates

> 本计划为纯裁决/文档工作（无生产代码、无 ORM/成本服务改动——仅读 java/讨论文档作裁决输入）。移除 build/test 验证命令门控，理由：无代码变更；验证对象为裁决内部一致性、事实证据可追溯、与 costing-methods.md 不矛盾。Q1/Q4 为安全/业务裁决，触及成本代码区域的实施（E3.2）为独立 ask-first successor，本计划不触代码。

- [ ] 范围内行为完成（Q1 + Q4 + Q2/Q3 归属 + 三处落盘）
- [ ] 相关文档对齐（讨论文档 §未解决问题/§讨论点一、`costing-methods.md`）
- [ ] 无代码变更 → 跳过 build/test 门控（已说明理由）；裁决事实证据（CostRollupService/StandardCostResolver 行号引用）可追溯
- [ ] 无范围内项目降级为 deferred/follow-up（Q2/Q3 实施显式移出范围为 successor，非降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Q2/Q3 AI 候选 BOM 建模与 provenance 字段集（实施）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 AI 研发轨道 successor，本计划仅判归属域（manufacturing）；采购保密 enforcement 不依赖 AI 能力落地。
- Successor Required: yes（触发条件 = AI 驱动研发候选 BOM 管道启动）

### E3.2 CostRollupService 取值豁免实施

- Classification: `optimization candidate`（实为**必需 successor**，trigger-locked，非可选优化——三个分类标签无一完美贴合"必需 successor"）
- Why Not Blocking Closure: 本计划仅裁决边界方案；实施触及财务/成本保护区域，须独立 plan-first + ask-first。
- Successor Required: yes（触发条件 = E3.2 工作项进入，依赖本计划 Q4 裁决）

## Closure

Status Note: <待完成后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立审计者>
- Evidence: <task id / 链接>

Follow-up:

- <非阻塞跟进项；已确认缺陷不得出现于此>
