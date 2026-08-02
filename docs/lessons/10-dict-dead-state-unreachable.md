# Lesson 10: dict 死状态——状态机字典声明了不可达 / 无迁移的状态值

> **来源**：2026-07 审计-修复任务。MR1 跨多域（finance R1.13 / mfg R1.14 / hr R1.15 / inventory R1.19 / quality R1.20 / projects R1.21 / contract R1.22 / aps-logistics R1.25 等）。MA2 状态机审查在 finance/mfg/hr 等域**系统性发现** dict 含声明但无任何 `setStatus(...)` writer 的状态值，owner doc 迁移图却声明了进/出迁移。
> **适用场景**：任何含状态机字典（`erp-<short>/<xxx>-status` dict）+ owner doc 状态迁移图的域。状态机审查、ORM 模型审查、owner doc vs 代码 drift 审查时必查。
> **失败模式**：dict（`*.dict.yaml`，由 ORM `<dict>` 驱动生成）声明了 N 个状态值，但生产代码中**仅有部分**被 `setStatus(...)` 写入。未被任何代码路径写入的状态值是「死状态」——运行时**永不出现**，UI 下拉/筛选永远空命中，owner doc 迁移图声明的「进/出迁移」是空头承诺。审查者 / 开发者按 dict 全集或 owner doc 期望实现逻辑时产生悬挂数据或假阳性查询。

## 核心论点

状态机字典的每个值必须满足**可达性**：存在至少一个 `setStatus(该值)` 写入路径。dict 声明但无 writer 的值是死状态，它有三重危害：

1. **UI 误导**：下拉框列出永不可选项；筛选查询按死状态过滤永远空命中。
2. **owner doc 漂移**：迁移图画了进/出迁移但代码无实现，审查者按图期望落空。
3. **查询假阳性/假阴性**：按死状态做条件查询（`findAllByQuery(status=X)`）逻辑错误但编译通过——silent bug。

死状态的两种合法处置（任选其一，但必须显式裁决）：(A) 删除 dict 项 + 删常量 + owner doc 标注「走 useLogicalDelete / 经 XX 表达」；(B) 实现迁移 BizMutation + owner doc 同步。**禁止**：留着 dict 项不删也不实现（沉默悬挂）。

## 失败模式（典型路径）

```
1. 建模时按「完整生命周期」声明 dict 全集（含预留状态）
2. 实现时只写了主路径状态迁移（DRAFT→SUBMITTED→APPROVED→COMPLETED）
3. 预留状态（CANCELLED / SUSPENDED / OVERDUE / ...）常量定义在 *Constants.java 但零 setStatus writer
4. owner doc 迁移图照搬 dict 全集画了进/出迁移（承诺但无实现）
5. dict 项留着不删也不实现 → 死状态悬挂
6. 审查者按 owner doc 期望实现「CANCELLED 守卫」→ 守卫永不触发 → silent
```

## 真实案例（跨域同型，节选）

| Finding | 域 | dict | 死状态 | 处置 |
|---|---|---|---|---|
| `P1-MA2-031` | finance | `erp-fin/voucher-status` | CANCELLED（DRAFT→CANCELLED 无 action，`useLogicalDelete` 致 DB 永不出现） | R1.13：删 dict + owner doc 注 |
| `P1-MA2-035` | mfg | `erp-mfg/job-card-status` | PARTIALLY_TRANSFERRED / MATERIAL_TRANSFERRED（常量零使用） | R1.14：删 dict + 常量 |
| `P1-MA2-036` | mfg | `erp-mfg/mrp-status` / `forecast-status` | MRP CANCELLED / 预测 CONSUMED | R1.14：删 dict + 常量 |
| `P1-MA2-039` | hr | `erp-hr/employment-status` | RESIGNED/TERMINATED/RETIRED（无 resign/terminate/retire BizMutation） | R1.15：Deferred 标注 |
| `P1-MA2-040` | hr | `erp-hr/contract-status` | SUSPENDED（无 suspend/resume） | R1.15：Deferred |
| `P1-MA2-042` | hr | dev-plan / plan-item | DRAFT / CANCELLED / OVERDUE | R1.15：Deferred |
| `P1-MA2-043` | hr | `erp-hr/timesheet-status` | APPROVED/REJECTED（BizModel 仅 submit） | R1.15：删 dict + Deferred |
| `P1-MA2-045` | hr | `erp-hr/bank-file-status` | UPLOADED/CONFIRMED（CRUD 桩） | R1.15：删 dict + Deferred |

> 共同裁决范式（finance A2.5a P1-MA2-031 首立）：dict 项不可达 + owner doc 声明但代码无实现 = 同型，按 (A) 删 / (B) 实现 二选一显式裁决。

## 决策树：审查状态机 dict 时，对每个状态值问「谁写入它？」

```
对 dict 的每个状态值 X：
1. grep `setStatus(*_X)` / `setStatus("X")` 全 src/main 是否有 writer？
   → 有 writer：可达，正常。
   → 无 writer：死状态，进入步骤 2。

2. owner doc 迁移图是否声明了进入 X 的迁移？
   → 声明了但代码无实现：承诺漂移，进入步骤 3。
   → 未声明：纯预留死状态，进入步骤 3。

3. 显式裁决（二选一，禁止沉默保留）：
   a. 删 dict 项 + 删 *Constants 常量 + owner doc 标注「走 useLogicalDelete / 经 YY 表达 / Deferred 触发条件」。
   b. 实现迁移 BizMutation + owner doc 同步实际实现。
   → 裁决理由记录在 plan / owner doc。
```

## 自检清单（状态机审查 / 新增 dict 时）

- [ ] 对 dict 每个值 grep 了 `setStatus` writer？无 writer 的值已显式裁决（删 / 实现 / Deferred 标注）？
- [ ] owner doc 迁移图声明的每个「进/出迁移」在代码中有对应 action / setStatus？
- [ ] `*Constants.java` 中的状态常量是否都有使用方（零使用 = 死常量）？
- [ ] 删除 dict 项时是否同步删了常量 + 更新 owner doc（三处同步）？
- [ ] Deferred 处置是否在 owner doc 标注了**触发条件**（何时补实现）而非仅标 Deferred？

## 何时复发

- 新建模时按「完整生命周期」声明 dict 全集但只实现主路径。
- owner doc 先于代码编写，迁移图画了未实现状态（doc-driven 时易发）。
- 复制相邻域 dict 模板时带入不适用的预留状态。

## 关联

- 真相源：`docs/audits/arm-index.md`（P1-MA2-031/035/036/039-045 + 各域状态机审查报告）
- 修复证据：roadmap R1.13（finance）/ R1.14（mfg）/ R1.15（hr）/ R1.19（inv）/ R1.20（qa）/ R1.21（prj）/ R1.22（contract）/ R1.25（aps+logistics）
- 审查方法：`docs/skills/state-machine-business-review-prompt.md`
- 平台规则：dict 由 ORM `<dict>` 驱动生成（见 lesson 06——改 dict 必须改模型源）
