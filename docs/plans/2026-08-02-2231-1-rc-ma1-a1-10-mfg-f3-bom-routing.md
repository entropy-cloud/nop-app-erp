# 2026-08-02-2231-1 rc-ma1-a1-10-mfg-f3-bom-routing mfg-F3 BOM 与工艺路线需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.10（MA1 需求追踪矩阵审计 — mfg-F3 BOM 与工艺路线：UC-MFG-02 多级 BOM 展开(phantom 虚拟件) + UC-MFG-10 BOM 变更不影响已开工工单(快照原则)）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.10
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.10 的 0.2 依赖）、`2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md`（A1.8，同域同范式；UC-MFG-05/08 预留写路径归 A1.8，本切片仅交叉引用齐套/BOM 展开读侧）、`2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md`（A1.9，同域同范式；UC-MFG-03 齐套/UC-MFG-07 成本结转归 A1.9，本切片仅交叉引用 BOM 展开读侧）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.10 给出 UC 清单 = `UC-MFG-02/10`（2 UC），锚点 `use-cases.md:43` / `:176`（inventory :344 确认一致 ✅）。

- **L1 需求契约（权威真相源）**：`docs/design/manufacturing/use-cases.md`：
  - UC-MFG-02 多级 BOM 展开(phantom 虚拟件)（`:43`）：BOM.is_phantom==true 的组件 → 不生成该组件的生产订单 + 其子件直接展开到当前工单的物料需求；齐套校验基于展开后的全部子件(含虚拟件子件)。
  - UC-MFG-10 BOM 变更不影响已开工工单(快照原则)（`:176`）：工单审核时快照 BOM(工单行记录当时 BOM 内容)；BOM 后续修改 → 不影响已审核工单的物料需求/成本；新建工单才用新 BOM。

- **L2 owner doc 设计参考**：`docs/design/manufacturing/bom-and-routing.md`（§多级 BOM 展开 `:62-72`：phantom 展开规则 + 多级展开 + 缓存；§BOM 版本快照规则 `:128-136`：快照时机 DRAFT→SUBMITTED + snapshotBomVersion 字段 + erp-mfg.bom-snapshot-strategy LOCK_AT_CREATION/AUTO_UPGRADE；§实现注记 `:138-147`：**本期 Non-Goal 显式列"BOM 版本快照"** + AUTO_ON_BOM_CHANGE 自动重算 + 展开结果缓存表）。**注意**：L2 为设计参考，与 L1 冲突时按 §4 Q1 以 L1 为准；§实现注记为 AI 落地补注（Non-Goal 标注是否构成 §4 判据(ii)"显式人工批准 documented simplification"须执行时核验批准来源——AI 自写不算）。

- **L3 代码实现现状（执行时实测核验）**：
  - **BOM 展开器（UC-MFG-02 核心）**：`module-manufacturing/erp-mfg-service/.../bom/BomExpander.java`（`expand:77-83` 入口 + `expandLines:85-119` DFS：环检测 `path.contains(product)`→抛 `ERR_BOM_CYCLE`、深度上限 `level > maxDepth`(config `erp-mfg.bom-max-depth` 默认 15)→抛 `ERR_BOM_MAX_DEPTH_EXCEEDED`、`finally { path.remove(product); }` 路径回溯、**phantom(bomType=PHANTOM) 展开其子件并入当前层级不产生独立节点 `:106-108`**、有效用量 `line.quantity × scale`(scale=requestedQty/bom.qty，divide 守护 b.signum()==0)）。config 常量 `ErpMfgConstants.CONFIG_BOM_MAX_DEPTH`（`ErpMfgConstants.java:25-26`）+ error key `ErpMfgErrors.java:64`。
  - **BOM 快照（UC-MFG-10 核心）**：执行时 grep `snapshotBomVersion` / `bomSnapshotStrategy` / `BOM_SNAPSHOT` / `LOCK_AT_CREATION` / `AUTO_UPGRADE` / `bom-snapshot` 全 `module-manufacturing/` **零命中**（起草时已实测：仅 BomExpander/ErpMfgConstants/ErpMfgErrors 的 max-depth 命中，无任何快照字段/策略/config key/写路径）。**推定快照子系统未实现**（owner doc §实现注记已标 Non-Goal）——执行时对 HEAD 复核确认仍零命中，并核验 `ErpMfgWorkOrder`/`ErpMfgWorkOrderLine` ORM 是否含 `snapshotBomVersion` 列。

- **L4 测试证据现状**：`TestErpMfgBomExplosion`（BOM 多级展开 + phantom）。**UC-MFG-10 快照隔离无测试**（功能推定未实现）。执行时逐项核验断言强度：UC-MFG-02 phantom 展开（虚拟件不产生独立节点 + 子件并入当前层 + 齐套基于展开后全集）、环检测/深度上限异常路径、有效用量算术。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`（A2.6b）= MRP 计划规划 + BOM 状态机审计**：Verdict **pass**（零 P0、3 P1：P1-MA2-036 CANCELLED/CONSUMED 死状态 / P1-MA2-037 建议单 RELEASED 漂移 / P1-MA2-038 委外 O-4 豁免登记）。**BOM 无独立状态机**（is_active/is_default 治理，A2.6b 已声明）；BomExpander 经 `TestErpMfgBomExplosion` 覆盖。事务边界覆盖 MRP 运算 + 释放原子性确认。
  - **`docs/audits/2026-07-29-0024-arm-ma4-mfg-work-order-bom-code-quality.md`（A4.2a）= 工单/BOM 代码质量审计**：Verdict **FAIL**（零 P0、3 P1：P1-MA4-007 完工编排吞异常 / P1-MA4-008 跨域 daoFor / P1-MA4-009 测试有效性 + 1 P2）。**BomExpander DFS 环检测 + 深度上限 + path 回溯 + phantom 展开 + 算术正确性判定为扎实 PASS**（§5 维度 2）；P1-MA4-008 含 BomExpander 跨域 daoFor(ErpMd*/ErpInv*) 绕 I*Biz 投影。
  - **注意**：A2.6b/A4.2a 覆盖**BOM 展开算法正确性/代码质量/状态机治理**，但本切片从**需求契约↔实现符合性**视角补差异（UC-MFG-02 phantom 展开的需求验收 + UC-MFG-10 快照原则的 L1 字面要求 vs 推定未实现的符合性裁决）。

- **arm-index 既有 finding 衔接**：BOM 相关——`P1-MA4-008`（BomExpander 跨域 daoFor 绕 I*Biz，同 P1-MA1-022 根因投影）。UC-MFG-10 快照缺失为新功能点维度（既有审计未从需求契约视角裁决快照义务），执行时 grep `arm-index.md` mfg BOM/快照同域同控制点后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；触及 ORM 结构变更（若快照修复需加 `snapshotBomVersion` 列/快照表）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.10 切片的五级追踪审计报告缺失 = MA4（A4.2 扩展域展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.10 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.10 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-10-mfg-f3-bom-routing.md`，含方法论 §6 **9 段全部内容**：①UC-MFG-02/10 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含 BomExpander DFS 环检测/深度上限/phantom 展开 + max-depth config + UC-MFG-10 快照写路径 grep 零命中证据）③测试证据（注明断言强度）④运行时行为证据（复用 A2.6b/A4.2a，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2/MA4 报告差异增量声明。
- 对 2 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-MFG-02（phantom 虚拟件不生成生产订单 + 子件直接展开到当前工单物料需求 + 齐套校验基于展开后全部子件含虚拟件子件）+ UC-MFG-10（审核时快照 BOM + BOM 修改不影响已审核工单 + 新建工单才用新 BOM），各一矩阵行。
- 对候选缺口/偏离给出分级结论：**UC-MFG-10 BOM 快照原则（L1 字面"审核时快照"vs 推定完全未实现，owner doc §实现注记标 Non-Goal）——按 §4 Q1 L1 为准 + §5 Q4（P0/P1 必须实现、禁止方案 B 无例外）+ §4 三判据核验 Non-Goal 标注是否构成"显式人工批准 documented simplification"（AI 自写不算，须人工批准痕迹）→ 候选 P1（会计/成本正确性类：BOM 变更后已开工工单按新 BOM 算物料需求/成本将破坏成本结转正确性）**；UC-MFG-02 phantom 展开（A4.2a 已证算法扎实 → 倾向接受，dedup P1-MA4-008 跨域 daoFor）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / mfg use-cases / bom-and-routing.md 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.8/A1.9 mfg done；A1.11 mfg-F4 差异/批次/看板 = 独立切片独立 plan；A1.10 只覆盖 UC-MFG-02/10）。**UC-MFG-03 齐套/UC-MFG-07 成本结转归 A1.9**，本切片 UC-MFG-02 BOM 展开仅交叉引用齐套读侧（齐套=展开结果 vs 可用量，展开归本切片）。
- **不重跑既有状态机/代码质量行为审计**（§去重协议：A2.6b/A4.2a 已证实 BOM 展开/状态机治理/代码质量，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.10 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.10 UC 锚点）+ `docs/design/manufacturing/use-cases.md`（L1 真相源）+ `docs/design/manufacturing/bom-and-routing.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ A2.6b/A4.2a 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用 A2.6b/A4.2a 审计（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-manufacturing/erp-mfg-service -Dtest=TestErpMfgBomExplosion`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + resolved finding HEAD 复核

Status: completed
Targets: `docs/audits/2026-08-02-2231-1-rc-ma1-a1-10-mfg-f3-bom-routing.md`（新建，先填 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [x] `Proof` 对 UC-MFG-02/10 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:43/:176` 验收标准原文（禁止转述）；L2 引用 `bom-and-routing.md`（§多级 BOM 展开 `:62-72` + §BOM 版本快照规则 `:128-136` + §实现注记 Non-Goal `:138-147`）对应 section（标注"设计参考，冲突以 L1 为准"）；L3 引用 `module-manufacturing/erp-mfg-service/.../bom/BomExpander.java:line`（expand/expandLines DFS + phantom 展开 + 环检测/深度上限）+ `ErpMfgConstants.java:25-26`（CONFIG_BOM_MAX_DEPTH）+ `ErpMfgErrors.java:64`；**UC-MFG-10 快照写路径**附 grep 零命中证据（snapshotBomVersion/bomSnapshotStrategy/LOCK_AT_CREATION/AUTO_UPGRADE/bom-snapshot 全仓零命中）+ 核验 `ErpMfgWorkOrder`/`ErpMfgWorkOrderLine` ORM 是否含 snapshotBomVersion 列；L4 引用 `TestErpMfgBomExplosion#method`（注明断言强度；快照无测试）；L5 复用 A2.6b/A4.2a + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口/偏离**（逐条验收标准对照）：UC-MFG-02——①phantom(bomType=PHANTOM) 子件不生成生产订单；②其子件直接展开到当前工单物料需求（并入当前层级）；③齐套校验基于展开后全部子件（含虚拟件子件）；④环检测 `ERR_BOM_CYCLE` + 深度上限 `ERR_BOM_MAX_DEPTH_EXCEEDED` 异常路径；⑤有效用量算术 `line.quantity × scale`。UC-MFG-10——⑥审核(DRAFT→SUBMITTED)时快照 BOM（工单行记录当时 BOM 内容）；⑦BOM 后续修改不影响已审核工单物料需求/成本；⑧新建工单才用新 BOM。**⑥⑦⑧ 推定完全未实现**（grep 零命中 + owner doc Non-Goal）——须 HEAD 复核确认。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` **resolved finding HEAD 复核**：对 BOM 相关 finding（P1-MA4-008 BomExpander 跨域 daoFor 绕 I*Biz——**resolved 状态执行时经 arm-index grep 确认，未确认者按"未定"处理**）在当前 HEAD 代码实际落地（按逻辑非行号核验），记录复核结论（已落地/回退/部分落地/documented simplification 仍 open successor）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：**UC-MFG-10 BOM 快照缺失（核心裁决点）**——按 §4 Q1 L1 为准（需求契约字面要求"审核时快照"+ §4 推定 owner doc 已向实现妥协）+ §5 Q4（P0/P1 必须实现禁方案 B 无例外）+ §4 三判据核验 Non-Goal 标注是否构成"显式人工批准 documented simplification"（判据(i) plan 含独立 plan-audit / (ii) owner doc 显式标注经人工批准痕迹 / (iii) product-scope 范围裁剪登记；AI 自写 Non-Goal 不算）；会计/成本正确性类（BOM 变更后已开工工单按新 BOM 算物料需求/成本破坏成本结转）Q4 无例外 → **候选 P1**（执行时据三判据证据 + 是否破坏会计正确性精确定级，若同时致凭证错误可升 P0）。UC-MFG-02 phantom 展开（A4.2a 已证算法扎实）→ 倾向接受，dedup P1-MA4-008。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-MFG-02/10 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号 + UC-MFG-10 快照 grep 零命中证据、L4 注明断言强度、L5 标注复用 A2.6b/A4.2a 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；候选缺口 ①-⑧ 有明确分级（非悬空"待查"）；UC-MFG-10 快照缺失结论含 §4 三判据核验证据；resolved finding HEAD 复核结论已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-02-2231-1-rc-ma1-a1-10-mfg-f3-bom-routing.md`（补 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` mfg BOM/展开/快照同域同控制点（如 P1-MA4-008）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点（如 UC-MFG-10 快照缺失 = 需求契约视角新维度） → 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。**特别注意**：UC-MFG-02 phantom 展开若仅复用 A4.2a 已证算法结论则不新建 finding（交叉引用 P1-MA4-008 daoFor 投影）。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（如 BOM 变更后已开工工单运行时是否实际按新 BOM 重算物料需求/成本、快照缺失运行时是否致成本结转凭证错误；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2/MA4 报告差异增量声明：声明复用 A2.6b（BOM 无独立状态机 + is_active/is_default 治理 + MRP 运算/释放事务原子性）+ A4.2a（BomExpander DFS/环检测/phantom 展开/算术正确性 PASS + P1-MA4-008 daoFor）已证实结论，列明本切片只补的需求视角差异（UC-MFG-02 phantom 展开需求验收 + UC-MFG-10 快照原则符合性裁决）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [x] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.2 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 `ses_03d1a2debffen81hLkNNsZ1fQ1`，fresh session，未起草本计划）。逐项实测核验全 PASS：roadmap 对齐（A1.10 / UC-MFG-02/10 / Deps=0.2 done / Skill）、2 UC 锚点 :43/:176 全匹配（完整枚举无跳无合并，逐 UC 一矩阵行）、L2 owner doc 引用存在（bom-and-routing.md §多级展开 :62-72 + §快照规则 :128-136 + §实现注记 Non-Goal :147）、L3 路径存在（BomExpander explode:77-83/expandLines:85-119 DFS + 环检测 ERR_BOM_CYCLE + 深度上限 ERR_BOM_MAX_DEPTH_EXCEEDED via erp-mfg.bom-max-depth + phantom 展开 + path 回溯 + 算术 + ErpMfgConstants:25-26 + ErpMfgErrors:64）、**UC-MFG-10 快照子系统缺失独立复核确认**（grep snapshotBomVersion/bomSnapshotStrategy/LOCK_AT_CREATION/AUTO_UPGRADE/bom-snapshot 全 module-manufacturing 零命中；ORM 唯一 snapshot* 命中 = ErpMfgMrpScenarioVersion.snapshotSummary 非工单快照——"推定快照未实现"主张事实正确）、L4 TestErpMfgBomExplosion 存在（无快照测试）、L5 dedup 报告存在（A2.6b BOM 无独立状态机 + A4.2a BomExpander DFS/phantom PASS + P1-MA4-008 daoFor）、跨切片边界正确（UC-MFG-03/07 归 A1.9、UC-MFG-05/08 归 A1.8）、item typing/skill/anti-slack/只读 Closure-Gate 删门控有据/保护区域 ask-first 全合规。无阻塞 issue。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + resolved finding HEAD 复核 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.10 报告 9 段齐全 + UC-MFG-02/10 逐矩阵行 + resolved finding HEAD 复核 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.10 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；触及 ORM 结构变更（若 UC-MFG-10 快照修复需加 `snapshotBomVersion` 列/快照表）或会计过账逻辑（成本结转凭证）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: 独立结束审计（fresh session，不重用执行者上下文）通过。A1.10 切片只读审计交付物已实测落地：报告 9 段齐全 + 2 UC 五级矩阵 + 6 resolved finding HEAD 复核 + 1 新 P1 finding（P1-RC-009）登记入 arm-index。本计划为只读审计（无代码/ORM/api.xml/view.xml/真相源变更），不存在 anti-hollow 风险（无新代码）；§8 checker 19 规则全 actual==baseline 零漂移佐证零回归。finding 的修复属 MR1 后续切片（已 adjudicated 为 out-of-scope improvement，见 Deferred 段）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（CLOSURE_VERIFY 模式 fresh session，不重用执行者上下文；执行者日志 line 10 显式声明 "Closure Gates 留待独立结束审计... gate 134 禁执行者自勾"，未自我审计）
- Evidence:
  - 报告产物：`docs/audits/2026-08-02-2231-1-rc-ma1-a1-10-mfg-f3-bom-routing.md`（44KB，303 行，9 段齐全：§1 L1 逐字 6 断言 / §2 L3 file:line + 快照 grep 零命中表 / §3 L4 断言强度 / §4 L5 复用 A2.6b/A4.2a + 差异 / §5 矩阵+结论+resolved HEAD 复核 / §6 arm-index 衔接 / §7 静态存疑点 / §8 过程纪律自检+checker 表 / §9 差异增量声明）
  - HEAD 锚点核验（HEAD `15bf103d2`）：实测 `module-manufacturing/erp-mfg-service/.../bom/BomExpander.java:77-83`（explode 入口）+ `:85-119`（expandLines DFS）+ `:88-92`（环检测 ERR_BOM_CYCLE）+ `:93-97`（深度上限 ERR_BOM_MAX_DEPTH_EXCEEDED）+ `:104-108`（phantom 展开 `bomType==PHANTOM` 子件并入当前层级不产生独立节点）+ `:116-118`（finally path 回溯）+ `:101-103`（有效用量算术）—— 全部逐字匹配报告 §2.1
  - UC-MFG-10 快照零命中实测复核：`grep -rn "snapshotBomVersion|bomSnapshotStrategy|LOCK_AT_CREATION|AUTO_UPGRADE|bom-snapshot|BOM_SNAPSHOT" module-manufacturing/` → 零输出（EXIT=0），佐证报告 §2.2 "推定快照子系统未实现"主张事实正确
  - arm-index 登记：`docs/audits/arm-index.md:108` 含 `P1-RC-009` finding 行（控制点/根因/§2 P1① 判据/MR1 目标/修复方向 完整）+ `:130` A1.10 summary 注记（含 §4 三判据核验结论 + 6 resolved finding HEAD 复核 + BomExpander 同域 daoFor 不涉 P1-MA4-008 澄清 + 3 静态存疑点交接 MA4）
  - 日志同步：`docs/logs/2026/08-02.md:5-11` 含本切片 EXECUTE 条目（2 Phase 全完成 + 产出文件清单 + bookkeeping + successor 说明）
  - 真相源锚点一致：`docs/audits/rc-requirement-baseline-inventory.md:51,59` UC-MFG-02 `:43` / UC-MFG-10 `:176` + `:344` A1.10 行 "✅ 一致" 交叉核验通过
  - draft review 证据：plan §Draft Review Record iter-1 `acceptable as-is`（独立子代理 `ses_03d1a2debffen81hLkNNsZ1fQ1`，fresh session）—— 逐项实测全 PASS 记录在案
  - 只读零回归证明：本计划无生产代码变更（无 .java/.orm.xml/.api.xml/.view.xml/真相源 diff），§8 checker 19 规则 actual 全 == baseline（零漂移），无回归风险
  - 文本一致性：Plan Status `completed` / Phase 1-2 Status `completed` / 各 Exit Criteria 全 `[x]` / Closure Gates 全 `[x]`（本审计 tick）/ 日志条目一致
  - Deferred honesty：唯一 deferred 项 = finding 修复实施（Classification `out-of-scope improvement`，Successor Required=yes → MR1 RC-R1.n），无已确认缺陷隐藏在 Follow-up

Follow-up:

- P1-RC-009（UC-MFG-10 BOM 快照原则完全缺失）由 MR1 R1.0 展开为 RC-R1.n 修复行；修复触及 ORM 结构变更（snapshotBomVersion 字段或 ErpMfgWorkOrderBomSnapshot 实体）+ 成本计算读侧（KitAvailabilityChecker/BomExpander 读侧改读快照），须 ask-first + 独立 plan-audit（§5 ORM 结构变更 + 成本正确性类）
- SP-1/SP-2/SP-3（3 项静态存疑点）交 MA4 A4.2 运行时展开（确认 P1-RC-009 是否致成本结转凭证错误，决定是否升 P0）
