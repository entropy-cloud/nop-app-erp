# Human In The Loop vs Human On The Loop——Plan 作为 AI Loop 的基本单位

> 讨论背景：审阅 PPT S08-S16（Plan Loop / Mission Driver 章节）时发现"vibe coding"的类比不准确，需要重新定位。
> 日期：2026-07-19
> 后续更新：2026-07-19（追加日志实证分析和 PPT 修正记录）

## 核心区分

### Vibe Coding = Human In The Loop

传统 AI 编程模式（包括 vibe coding、ChatGPT 对话式编程、Cursor 的 chat 模式）：

```
Human: prompt → AI: generate → Human: read → Human: next prompt → ...
```

人**在循环内部**：每一轮迭代都需要人的输入才能推进。没有人的指令，AI 不会进入下一轮。人是循环的**节拍器**——循环的节奏由人的注意力决定。

这种模式下不可能有真正的"全自动开发"——人的注意力是瓶颈，且人必须为每一轮代码生成负责。

### AI 全自动开发 = Human On The Loop

AGE / Loop Engineering 模式：

```
AI: plan → review → execute → verify → loop ...
                          ↑
                    Human: 观察、干预（可选）
```

人**在循环之上**：AI 自主执行完整的 Plan 循环，人在关键决策点（草案审计、方向纠偏、异常处理）介入。人不是每一轮都必须参与，但可以在任何时刻打断、调整、重新定向。

这要求循环的基本单位是一个**可独立执行的闭包**——即 Plan。

## Plan 作为 AI Loop 的基本单位

为什么 Plan 必须是基本单位，而不是"一段代码"或"一个文件"？

| 需求 | 说明 |
|------|------|
| **有边界** | Plan 定义 Goals / Non-Goals，AI 知道做什么和不做什么 |
| **有基线** | Current Baseline 从仓库读取，不依赖对话记忆 |
| **有退出标准** | Exit Criteria / Closure Gates，AI 知道"什么时候做完" |
| **可审计** | 独立子代理可以在不继承执行上下文的情况下验证完成度 |
| **可中断/可恢复** | Plan 状态（draft/active/completed）+ 复选框粒度，中断后扫盘即可恢复 |
| **可编排** | Mission Driver 可以遍历路线图，自动选择下一个 Plan |

没有 Plan 作为基本单位，AI 循环就是一个**不可关闭的对话**——人必须一直在线，因为只有人知道"什么时候算做完"。

## Mission Driver：更大程度的自主循环

Mission Driver 在 Plan Loop 之上又加了一层编排循环，实现了更大程度的自主性：

```
Mission Driver Loop（天/周级）：
  CHECK → REVIEW_PLANS → EXEC_PLANS → DRAFT_PLANS → DEEP_AUDIT → ...
                    ↑         ↑              ↑
                    Plan      Plan           Plan
                  draft→active  执行         起草新 Plan
```

### Mission Driver 的设计特征

1. **持续接受新 Plan**：DRAFT_PLANS 步骤从路线图持续起草新 Plan，不设固定批次上限。只要路线图还有 todo 项，Mission Driver 就会继续。

2. **随时可中断**：每个顶级步骤是隔离的（不传递参数），中断后重跑时引擎扫盘恢复——`draftPlans()` 扫出 draft 状态的 Plan，`activePlans()` 扫出 active 状态的 Plan，恢复不依赖内存状态。

3. **随时接受人工调整**：
   - 人可以手动修改 Plan 的状态（draft→active→completed），Mission Driver 的 REVIEW_PLANS / EXEC_PLANS 步骤自动识别
   - 人可以手动修改路线图（添加/删除/重排工作项），DRAFT_PLANS 自动适配
   - 人可以在草案审查中投否决票（独立子代理审查），拦截方向性错误
   - 人可以在任何时刻中断 Mission Driver 进程，手动调整后重新启动

4. **空闲时自审计**：DEEP_AUDIT 步骤在路线图无可起草时自动启动——系统不自满。

### Human On The Loop 的介入点

```
                     人工介入点
                         │
                   草案审查拦截方向错误
                         │
                    ┌────┴────┐
                    ▼         ▼
              draft Plan   active Plan  → 执行 → 审计 → completed
                    │                        ↑
                    │                    观察运行状态
                    │                    随时中断调整
                    ▼
                人工调整路线图
                手动修改 Plan 状态
```

## 对 PPT 的修正方向

S08 当前标题暗示"vibe coding"是反面案例，这不够准确。vibe coding 不是"坏"的——它是 Human In The Loop 的极端形式。我们的观点不是"vibe coding 不好"，而是：

1. **Human In The Loop（vibe coding）** 和 **Human On The Loop（Plan Loop + Mission Driver）** 是两种不同的控制模式，适用于不同的场景
2. 对于产品级 ERP（352 实体、154 模块、~2900 测试），Human In The Loop 不可能——人的注意力无法支撑每轮迭代
3. Plan 作为基本单位 + Mission Driver 作为编排层，使得 Human On The Loop 成为可能

S14 关于"为什么需要 Mission Driver"的论述也应强化：Mission Driver 不是 Plan Loop 的锦上添花，而是实现"Human On The Loop"的必然要求——没有编排层的自主循环，Plan Loop 仍然需要人逐个触发，本质上还是 Human In The Loop。

---

## 追加：日志实证分析（2026-07-19）

### 分析请求

用户要求验证一个关键主张的真实性：**人随时发起额外审计/计划 → Mission Driver 自动拾取、审查、执行**。为此读取了 25 个日志文件和 258 份计划文件。

### 最完整的实证链条：竞争杠杆审计（2026-07-12）

来源：`docs/logs/2026/07-12.md:45-53`

```
用户要求 →
  系统审计：docs/audits/2026-07-12-1504-competitive-levers-implementation-audit.md
    → 系统起草：docs/plans/2026-07-12-1504-1-competitive-comparison-correction.md（draft）
      → Mission Driver REVIEW_PLANS 自动拾取 → 独立审查 → 自动提升 active
        → EXEC_PLANS 自动执行（07-13 日志记录 Phase 1-5 全部完成）
          → 自动关闭
```

**用户唯一的手动步骤**：第一次提出"核实 8 个超越点"要求。从审计报告生成到 Plan 最终关闭，全部自动完成。

### 其他案例

| 日期 | 触发方式 | 自动拾取证据 |
|------|---------|-------------|
| 06-22 | 用户要求核查 ORM 设计 | 系统自动运行 Python 扫描脚本 + 5 维度核查 + 产出修复 Plan |
| 06-23 | 用户要求审计设计文档 | 系统自动审查 36 文件 + 发现 1 blocker/5 major + 自动修复 |
| 06-23 | 用户问"流程覆盖充分吗" | 系统自动审计全流程文档 + 发现 4 缺口 + 自动补齐 |
| 07-10 | Mission Driver DRAFT_PLANS | 自动检测 4 个 draft Plan → 4 路并行子代理审查 → 自动提升 active |
| 07-12 | Mission Driver EXEC_PLANS | 自动执行 7 个 Plan 中的 item 1/7，两次失败自动重试 |
| 07-13 | Mission Driver DRAFT_PLANS | 自动检测 FK 名称解析剩余 9 域 → 逐批次起草执行 |
| 07-14 | 闭包审计自动扫描 | 自动扫描 206 份 completed Plan → 发现 24 份 deficient → 自动 spawn 24 子代理重审计 |

### 关键发现

1. **Human On The Loop 在实际运行中成立**：07-12 竞争审计链条中，用户只在"发起请求"这一步介入，其余所有步骤（审计→Plan 起草→审查→执行→验证→关闭）由系统自动完成。

2. **Mission Driver 的"拾取"是无差别的**：它不区分一个 Plan 是由路线图自动起草的还是由人工审计触发的——`draftPlans()` 函数只扫 Status=draft 的文件。这保证了人的额外介入可以无缝融入自动循环。

3. **Plan 作为基本单位的有效性得到验证**：07-10 的 4 个 Plan 审查批次中，每个 Plan 的 draft→active 转换独立执行（互不影响），有的 iter1 就 accept，有的需要 iter2。Plan 的边界属性（Goals/Non-Goals）使独立审查可以在不依赖上下文的情况下逐 Plan 进行。

### 对 PPT 框架的启示

- S08 不应说"四种缺失"，而应建立 **In the Loop vs On the Loop** 的二元框架
- S09 应强调 Plan 作为"基本单位"而非仅仅"关闭契约"——这是 On the Loop 的前提
- S14 应使用竞争审计的实证链条作为核心案例：人发起→系统自动完成剩余步骤
- Mission Driver 不是"Plan Loop 的锦上添花"，而是"从 In 到 On 的跨越器"

