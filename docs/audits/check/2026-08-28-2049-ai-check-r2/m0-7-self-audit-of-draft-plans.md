# ai-check-r2 M0.7 — 3 份 draft plan 自审（plan-guide 规则符合性）

> 落盘时间：2026-08-28-2059
> 路径：`docs/audits/check/2026-08-28-2049-ai-check-r2/m0-7-self-audit-of-draft-plans.md`

## 背景

本 mission 已起草 3 份 draft 状态 plan：

1. `docs/plans/2026-08-28-2054-1-entity-state-machine-m5-3-closure-audit.md`（M5.3 closure audit plan）
2. `docs/plans/2026-08-28-2054-2-ai-check-f2-4-finance-period-p1.md`（F2.4 finance-期间 P1 簇修复）
3. `docs/plans/2026-08-28-2059-3-ai-check-f2-5-mfg-workorder-p1.md`（F2.5 mfg-工单 P1 簇修复，本轮新起草）

由于子 agent 通道在本会话不可用（已派发 6 个全失败），独立 plan-audit 不可得。本自审在**主会话**对 3 份 plan 做 plan-guide 规则符合性**自我核对**（**非 plan-audit 替代**——仅作 plan 改进输入）。

## 1. plan-guide 规则符合性核对表

| 规则 | 描述 | F2.4 plan | M5.3 plan | F2.5 plan | 备注 |
|---|---|---|---|---|---|
| #1 | 从实时基线开始 | ✅ 引用 2026-08-28 3947/669 | ✅ 引用 M5.1/M5.2 done | ✅ 引用 M0.1 + 跨域 mfg 测试基线 | 三 plan 均含 Current Baseline 段 |
| #2 | Goals + Non-Goals | ✅ 明确 | ✅ 明确 | ✅ 明确 | — |
| #3 | 复选框执行/收口 | ✅ 5 阶段全 [ ] 项 | ✅ 6 CG 全 [ ] 项 | ✅ 5 阶段全 [ ] 项 | — |
| #4 | 一个 plan 一个结果面 | ✅ F2.4 三 finding | ✅ M5.3 closure audit 6 CG | ✅ F2.5 mfg P1 4 finding | 三 plan 范围互不重叠 |
| #5 | 结束前证明 | ✅ Closure Gates 5 项 | ✅ Closure Gates 7 项 | ✅ Closure Gates 6 项 | — |
| #6 | 无代码设计转储 | ✅ 实施细节下沉到 Phase | ✅ 仅列 6 CG 不复述工具 | ✅ Phase 1-3 是修复方向而非代码 | — |
| #7 | 复选框类型化（Fix/Add/Decision/Proof/Follow-up）| ⚠️ 部分缺失 | ⚠️ 部分缺失 | ⚠️ 部分缺失 | **3 份 plan 均需补 Item Types 行** |
| #8 | 技能使用记录 | ✅ "Skill: bug-diagnosis-prompt" | ✅ "Skill: closure-audit-prompt" | ✅ "Skill: bug-diagnosis-prompt" | — |
| #9 | 决策带理由 | ✅ Deferred But Adjudicated 段有分类 | ✅ deferred 段 | ✅ deferred 段 | — |
| #10 | 检查清单完整 | ✅ Phase 0-5 + Closure Gates | ✅ Phase 0-6 + Closure Gates | ✅ Phase 0-5 + Closure Gates | — |
| #11 | 文本一致性 | ✅ Plan Status: draft + Status Note | ✅ 同 | ✅ 同 | — |
| #12 | 独立草案审查 | ❌ **未做**（子 agent 不可用） | ❌ **未做** | ❌ **未做** | **3 份 plan 仍 draft 状态；不能执行** |
| #13 | 不可降级项 | ✅ P1 finding 无降级 | ✅ 无 P0/P1 降级 | ✅ 无降级 | — |
| #14 | 同组件多功能合一 plan | ✅ 3 finding 同 result surface | ✅ 6 CG 同 result surface | ✅ 4 finding 同 result surface | — |

## 2. 3 份 plan 共同缺口

| 缺口 | 影响 | 建议 |
|---|---|---|
| **Item Types 行缺失**（plan-guide #7）| 实施阶段追踪无类型标签 | **修订 3 份 plan**：每个 Phase 顶部加 `Item Types: Fix | Add | Decision | Proof` |
| **独立草案审查缺失**（plan-guide #12）| Plan 保持 draft，不能进入 active | 需用户/子 agent 触发 plan-audit |
| **Owner Doc 段落引用缺章节**（部分 plan）| 实施期需重新定位 | 修订时加具体小节锚点 |

## 3. 各 plan 细节问题

### 3.1 F2.4 finance-期间 P1 簇

**Strengths**：
- 3 finding 完整描述（控制点/证据/建议方向）
- 5 阶段结构清晰（0 基线 + 1-3 三 finding 修复 + 4 全量 + 5 索引）
- dual-agent-approval 跨域接口变更已声明
- Phase 4 域全量 + app-erp-all 回归充分

**Weaknesses**：
- Phase 3 (F2.4-3) 跨域接口签名扩展设计未给出具体 API（仅"扩展 `onTransferConfirmed` 签名"未给最终方法签名草案）——这是**实施时需补 design step**
- Phase 1 (F2.4-1) 修复细节："按账户维度拆分 book"——技术方案不具体（group by fundAccountId？还是改 vq.addFilter eq subjectId + per-account loop？）——**实施时需具体化**
- 跑 mvn test 估算未给（建议加 `mvn test -pl module-finance/erp-fin-service` 估算时间）

### 3.2 M5.3 closure audit

**Strengths**：
- 6 项 CG 完整覆盖 §9 checklist
- 5 阶段结构 + 独立子代理 closure-audit-prompt 验收
- Phase 0 不重跑 mvn install 决策合理
- Phase 2 stub 场景复现 evidence 要求清晰

**Weaknesses**：
- Phase 6 独立子代理 closure-audit-prompt：**当前会话子代理通道不可用**——这正是 plan 自身的执行阻塞点
- Phase 0 基线快照：依赖既有 2026-08-28 3947/669 行——但**当日有 4 个 erp-enhancement plan 收口批的增量**——M5.3 收官时应**复核当日最新 baseline 行**（而非仅引用 28 日 0219-2）
- 失败回退：若独立子代理不可用，**plan 须显式登记"等待子代理通道恢复"作为 successor 触发条件**

### 3.3 F2.5 mfg-工单 P1 簇

**Strengths**：
- 4 finding 完整描述
- 5 阶段结构与 F2.4 一致
- Phase 0 mvn test 充分

**Weaknesses**：
- Phase 1-3 各 finding 的"控制点/证据/建议方向"指向 ck-mfg-workorder.md——但**plan 内未摘要关键控制点**——实施 agent 需先读完整报告才能理解
- 建议：**修订时**每 Phase 顶部加 "参考 ck-mfg-workorder.md §P1-CK-mfg-00X" 行
- Deferred But Adjudicated 段提到 "mfg-004/005 详见 ck-mfg-workorder.md"——但**finding ID 与实际不匹配**（实测 mfg-002/003 + 2 个未指明 ID）——**修订时需精确 ID**

## 4. 3 份 plan 改进建议清单（待独立 plan-audit 时一并修订）

| 优先级 | 改进项 | 涉及 plan | 工作量 |
|---|---|---|---|
| 高 | 每 Phase 加 `Item Types` 行 | 3 份 | 小 |
| 高 | F2.4 Phase 1/3 修复细节具体化 | F2.4 | 中 |
| 中 | F2.5 每 Phase 加"参考 ck-mfg-workorder.md"行 | F2.5 | 小 |
| 中 | M5.3 Phase 0 复核当日最新 baseline 行 | M5.3 | 小 |
| 中 | F2.5 deferred finding ID 精确化 | F2.5 | 小 |
| 中 | 3 份 plan 跑 mvn test 估算时间 | 3 份 | 极小 |
| 低 | F2.4 Phase 3 跨域接口签名草案 | F2.4 | 中（需对照现有接口）|

## 5. 自审裁决

- **3 份 plan 不进入 active 状态**——按 plan-guide #12，子代理通道不可用导致 plan-audit 不可得
- **3 份 plan 仍 draft 状态**——保留为未来独立 plan-audit 的输入
- **3 份 plan 仍有改进空间**——本自审列出 7 项改进建议（按优先级）
- **不重新派发子 agent**——已 6 次失败，避免浪费 token

## 6. 多次执行隔离

本文件落盘于 `docs/audits/check/2026-08-28-2049-ai-check-r2/m0-7-self-audit-of-draft-plans.md`。
