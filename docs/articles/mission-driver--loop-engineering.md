# Mission Driver：Loop Engineering 的标准实现

> 一个通用的 AI 任务驱动引擎如何通过 loop 嵌套实现局部容错和稳定保障。

# 第一部分：入门

## 一、问题：从 Vibe Coding 到自主运行

当前主流的 AI 辅助开发仍然是 Vibe Coding：人提示，AI 响应，人纠正，AI 再响应。这可以称作是 Human In The Loop（人在环中）模式。每一次产出都需要人确认和修正，人和 AI 交替工作。

这个模式有两个问题。

第一个是**质量失控**。AI的执行本质上是概率采样过程，在没有外部控制结构介入时，AI 很容易走上岔路，比如改到一半跑去改别的代码，改完了忘记跑测试，失败了陷入死循环，进程崩溃后状态丢失只能从头再来。更危险的是自我宣称完成——跳过实际实现直接声称做完了。

第二个是**产能限制**。人在环中时，最终瓶颈仍然是人的工作时间和精力。人每天工作 8 小时，即使 AI 辅助让效率提升 3 倍，天花板也不过 24 人时。要真正释放 AI 的生产力，必须走向 Human On The Loop（人在环上）：AI 全自主 7×24 运行，人脱离 loop，成为按需介入的控制因子。

这不只是运行模式的转变，更是成本结构的变化。AI 可以 7×24 自主运行时，工时不再是制约因素。较弱的模型需要更长时间、消耗更多 token 才能完成工作，但它足够便宜，可以在夜间跑，不占用人的工作时间。多个 mission 可以并行处理不同的 roadmap 工作项。成本核算从"每小时人时产出"变成了"每美元智能产出"。

Mission Driver 就是实现这个转变的一种具体的控制结构。

## 二、什么是 Mission Driver（一分钟版）

Mission Driver 是一个声明式的任务驱动引擎。你给它一个目标（通过 roadmap 描述），它就自动进入一个循环：

```
检查健康状态（初始一次）→ 审查方案 → 执行方案 → 起草新方案 → （回到审查方案）→ 无新方案可起草时 → 深度审计 → （回到审查方案）...
```

循环一直跑，直到目标达成或审计预算耗尽。每一步都是独立的 AI 子进程或者脚本函数，单个步骤失败不会影响整体循环。

它是吸引子引导工程（Attractor Guided Engineering）的一个组成部分，不仅仅可以用于软件开发设计。通过配置自定义 flow、prompt 和 commands，它可以很自然的被推广到数据处理、文档分析等场景，是一种通用的AI全自主运行机制。通过配置自定义 flow、prompt 和 commands，它可以很自然的被推广到数据处理、文档分析等场景，是一种通用的AI全自主运行机制。

它适合需要长时间运行、有明确验收标准、需要多步迭代的复杂任务。

## 三、怎么运作：Loop 嵌套与局部容错

如果把Vibe Coding看作是一种无限长的单一Loop，Mission Driver 的核心就是分解为多层Loop的嵌套——Mission Driver 的主循环（5 步闭环）在外层，内层嵌入了 Plan Loop（EXEC_PLANS 子流中的执行→检查→审计→验证闭环）和可选的 Audit Loop（DEEP_AUDIT 子流中的多维审计闭环）。理解了这个嵌套结构，就理解了它为什么能长时间稳定运行。

![mission-driver-loop](images/mission-driver-loop.png)

假设有 3 个 active plan，plan-002 的执行遇到了阻塞：

```
EXEC_PLANS:
  ├── plan-001: EXECUTE ✓ → CHECK ✓ → BUILD ✓ → completed ✓
  ├── plan-002: EXECUTE ✗ → 重试 3 次仍失败 → subflow failed
  │              → 执行 agent 发现某个前提不成立
  │              → 将阻塞项移至 Deferred But Adjudicated，记录触发条件
  │              → 其余 Phase 继续执行 → completed（含 Follow-up 项）
  └── plan-003: EXECUTE ✓ → CHECK ✓ → BUILD ✓ → completed ✓
```

plan-002 的执行阻塞完全没有影响 plan-001 和 plan-003。阻塞被限制在子流内部，不传播到兄弟子流或父循环。这就是 loop 嵌套带来的局部容错。

> Plan执行时，AI 根据实际情况决定：局部阻塞的项移入 `Deferred But Adjudicated` 并记录 successor 触发条件，其余范围正常完成；如果整个 plan 方向被证伪，则标 `superseded` 或 `cancelled`。plan 的 `.md` 文件状态始终由 AI agent 管理。

每一步的执行状态都持久化到磁盘（plan 文件中的 checkbox）。进程崩溃后重启，引擎扫描磁盘上的 checkbox 标记，从断点恢复，不回放历史。

---

# 第二部分：起步

## 四、快速上手

> Mission Driver 的启动脚本路径因项目而异。在 AGE Template 原始项目中位于 tools/mission-driver.sh，在衍生项目中可能位于 ai-dev/tools/mission-driver.sh。使用前确认脚本位置和 MISSION_DRIVER_HOME 环境变量。

**起步流程**：

1. 下载 AGE 模板，让 AI 阅读并适配你的项目（调整文档结构、配置 commands、初始化 roadmap）
2. 确保测试命令可执行（npm test / mvn test / Playwright）
3. 日常与 AI 对话时按 plan guide 拟制计划到 plans 目录，积累 plan 和 log
4. 启动 mission

```bash
# 生成 mission 配置 + roadmap
./tools/mission-driver.sh draft "你的目标描述" --target-file <需求文档>

# 验证配置
node $MISSION_DRIVER_HOME/src/mission-check.mjs missions/<name>.json .

# Dry-run 验证流程编排
./tools/mission-driver.sh run <name> --dry-run --no-monitor

# 正式运行
./tools/mission-driver.sh run <name>
```

**监控**：

```bash
open http://localhost:9300              # 浏览器看板
cat _tmp/<runDir>/run-state.json        # 读状态
tail -f _tmp/<runDir>/<mission>.log     # 追日志
```

**中断和恢复**：

```bash
# Ctrl-C 安全中断（引擎捕获信号，清理子进程）
# 重新运行即自动恢复
./tools/mission-driver.sh run <name>

# 从特定步骤恢复
./tools/mission-driver.sh run <name> --from-step EXEC_PLANS

# 快速模式（跳过 DEEP_AUDIT）
./tools/mission-driver.sh run <name> --fast
```

**Postmortem**：

```bash
./tools/mission-driver.sh analyze           # 最近一次运行复盘
./tools/mission-driver.sh analyze <runId>   # 特定运行复盘
```

Postmortem 扫描所有事件和日志，运行复盘 agent，将结构化报告写入 memory 目录。后续同模块的 mission 会自动加载这些经验记忆。

**注意事项**：

- 不要在 mission 执行期间手动编辑正在被执行的 plan 文件（写竞争）
- 不要在同一个 opencode session 中嵌套启动 mission
- 启动前先手动运行一次 commands.test 确认基线通过
- 修改 roadmap 前，先停掉 mission（Ctrl-C），改完再重启

## 五、四层定义体系

Mission Driver 由四层定义组成，通过配置而非编程使用。

### 5.1 Mission Config（missions/\<name\>.json）

纯静态配置，声明做什么、在哪做、怎么验证：

```json
{
  "name": "medical-qa",
  "description": "从医疗论文生成 QA 训练数据集",
  "roadmapPath": "docs/backlog/medical-qa-roadmap.md",
  "plansDir": "docs/plans/medical-qa",
  "commands": {
    "test": "python scripts/check_quality.py --min-records 500",
    "typecheck": "python -c \"import dataflow\" && echo OK"
  },
  "promptsDir": "missions/prompts/data-processing"
}
```

commands.test 是必需字段，CHECK 和 BUILD_VERIFY 都会运行它。build/lint/typecheck 可选，缺失则跳过。运行时状态在 _tmp/\<runId\>/run-state.json，不写入 mission.json。

### 5.2 Flow 定义

引擎核心是**通用状态机 DSL 执行器**（`FlowEngine` 类，零项目特定逻辑）。Flow 定义声明步骤怎么编排、如何转换、遇到错误怎么办。引擎按 flow 描述推进状态机，不绑定任何固定步骤。

一个简化的 flow 结构：

```json
{
  "name": "my-flow",
  "entry": "CHECK",
  "steps": {
    "CHECK": {
      "type": "agent",
      "promptPath": "prompts/health-check.md",
      "transitions": {
        "pass": { "goto": "NEXT_STEP" },
        "fail": { "done": "failed" }
      }
    },
    "NEXT_STEP": {
      "type": "subflow",
      "flow": "my-subflow",
      "forEach": "activePlans()",
      "transitions": {
        "all_complete": { "done": "completed" }
      }
    }
  }
}
```

- **步骤类型**：`agent`（AI 子进程）、`script`（JS 函数）、`subflow`（嵌套子流）、`group`（分组步骤）
- **控制流**：`transitions` 定义步骤间的跳转（`goto`/`done`/`retry`）；`forEach` 遍历集合为每个元素启动子流；`when`/`otherwise` 条件跳过
- **错误处理**：每步可配 `maxRetries`、`onMaxRetries`、`onError`、`onUnknown`；引擎还提供全局死循环检测（ping_pong + max_cycles + max_total_steps）

**随引擎内置的默认 flow**（`flows/mission-driver.json`）就是文章一直在讲的那个循环。它的实际结构是：CHECK **仅入口运行一次**，然后进入 REVIEW_PLANS → EXEC_PLANS → DRAFT_PLANS 的循环体；当 DRAFT_PLANS 无新方案可起草时进入 DEEP_AUDIT，之后回到循环体。它不是硬编码，只是一个**缺省配置**。EXEC_PLANS 和 DEEP_AUDIT 本身也是子流（`plan-execution.json` / `deep-audit-loop.json`），可单独替换。

这意味着**可以设计全新的流程**——例如一个代码审查工作流，不需要 Plan 编排，可以直接写一个四步流：FETCH_PR → RUN_LINT → AI_REVIEW → POST_COMMENT。在 `missions/flows/` 下放自定义 flow.json，同时在 mission.json 中设置 `"flowName": "<custom>"` 即可，引擎不变。审查步骤是 `type: "agent"` 步骤配一个审查 prompt，不是特殊的步骤类型——任何 flow 都可以包含。关键约束：审查 agent 应在全新会话中执行，不与执行步骤共享上下文，以确保独立验证（见 §十三）。

### 5.3 Plan 文件

Plan 是关闭契约，不是任务清单。Markdown + checkbox，核心元素：

```markdown
# 01 采购订单审批流接入

> Plan Status: draft
> Last Reviewed: 2026-07-25
> Source: roadmap item "核心业务逻辑 M1"

## Current Baseline
- ErpPurOrder 实体已建模，CRUD 已生成
- 尚无审批逻辑

## Goals
- 采购订单支持提交→审批→拒绝/通过状态流转
- 审批通过后触发库存过账

## Non-Goals
- 不做多级审批
- 不做审批委托

### Phase 1 - 审批状态机
Status: planned

- [ ] 实现 submitForApproval / approve / reject 方法
- [ ] 状态校验：仅 DRAFT 可提交

Exit Criteria:
- [ ] submit→approve 后 posted=true
- [ ] 单元测试覆盖所有状态转换

## Closure Gates
- [ ] 端到端：提交→审批→过账→冲销 完整链路跑通
- [ ] 独立子代理 closure audit 通过
- [ ] mvn test 全绿
```

顶部三行状态标记、Goals + Non-Goals（防止 scope drift）、每个 Phase 有 Status + checkbox + Exit Criteria、Closure Gates 是 plan 级关门检查。Checkbox 是机器可读的持久化状态，引擎通过扫描 checkbox 从断点恢复。标记 completed 前所有 checkbox 必须勾选。

### 5.4 Roadmap

工作项索引，人类可阅读、可控制的宏观规划：

```markdown
| Status | Item | Target | Autonomy |
|--------|------|--------|----------|
| done   | ORM 建模 + codegen | 10 域 145 实体 | implement |
| active | 采购审批 → 库存过账 | 端到端 P2P 打通 | plan-first |
| todo   | 供应商发票 → 三单匹配 | 三单匹配 + 核销 | plan-first |
| planned| 多公司多账套 | 多公司行为 | ask-first |

## Milestones
- [x] M1: 核心域 CRUD 全绿
- [ ] M2: P2P 端到端
```

DRAFT_PLANS 启动 AI agent 读取 roadmap，选择工作项起草 plan，起草完成后将 roadmap 项标记为 active（待 plan 执行审计通过后才转为 done）。Autonomy 列控制自主级别。

## 六、配置与定制

引擎通用，定制通过配置和 prompt 完成，不改代码。

| 定制层 | 机制 | 示例 |
|--------|------|------|
| Prompt 覆盖 | `missions/prompts/\<name\>.md` 覆盖内置 | 数据处理的 EXECUTE prompt 说"调脚本"而非"改代码" |
| Flow 微调 | 在 `missions/flows/mission-driver.json` 覆盖同名 flow，增删改步骤 | 在 CHECK 后插入一个合规审计步骤 |
| 全新流程 | 写 `missions/flows/\<custom\>.json`，mission.json 设 `flowName: "\<custom\>"` | 代码审查流 FETCH_PR → RUN_LINT → AI_REVIEW → POST_COMMENT，不含 Plan 编排 |
| 子流覆盖 | 在 `missions/flows/` 放同名子流 JSON（如 `plan-execution.json`）替换内置子流 | 替换 EXEC_PLANS 子流，让每个 active plan 执行前后调外部脚本 |
| Commands | `mission.json` 的 commands 字段 | test = 质量检查脚本 |

**flow 加载优先级**：项目 `missions/flows/\<flowName\>.json` → 引擎内置 `tools/mission-driver/flows/\<flowName\>.json`。同名文件项目优先。子流同样遵循此链。

`promptsDir` 配置允许不同 mission 指向不同的 prompt 子目录，实现同一引擎、不同 prompt 集：

```json
{
  "promptsDir": "missions/prompts/analysis",
  "flowName": "mission-driver"
}
```

引擎 prompt 加载链：`promptsDir`（任务类型级）→ `missionsDir/prompts`（项目级）→ 内置默认。不设 `promptsDir` 时行为不变。

内置的 CHECK→REVIEW→EXEC→DRAFT→AUDIT 适用于软件开发类的大部分场景。但对于数据处理、文档分析等任务，完全可以用不同的 flow 来适配——引擎不变，换一个 flow 文件即可。

---

# 第三部分：实践方法

## 七、标准工作流

一个完整的任务从问题到交付，经过五步：

```
1. 分析
   写分析报告，用独立子 agent 反复审查改进直到达成共识

2. 计划拟制
   根据分析报告，按照 plan guide 拟制 plan 到 plans 目录

3. 自动审查
   REVIEW_PLANS 步骤启动独立子 agent 审查
   通过则提升为 active，未通过则退回修改

4. 执行
   EXECUTE 执行 plan，之后 CLOSURE_SCRIPT_CHECK 检查完整性
   BUILD_VERIFY 运行测试，失败时 AI 自动修复

5. 审计关闭
   独立子 agent 审计关闭（CLOSURE_AUDIT）
   通过后 plan 标记 completed
```

这五步不是可选项。跳过分析直接编码，跳过审查直接执行，跳过审计直接关闭，都是最常见的质量事故来源。

## 八、智能就是循环反馈改进

Mission Driver 的有效性不来自单次 AI 调用的质量，而来自循环反馈改进。每一轮执行产生的反馈（测试结果、审计发现、人工纠正）被记录下来，用于改进下一轮。

两条改进轨道并行运行：

**做事的提示词**（DRAFT/EXECUTE）：当 AI 生成的 plan 或代码被人工纠正时，纠正记录被子 agent 分析，提取通用规则写入 skill。下次 AI 自动加载。

**检查的提示词**（CLOSURE_AUDIT/DEEP_AUDIT）：当审计遗漏问题时，补充检查维度到审计 prompt。

共同原则：AI 生成第一版，人工修改的历史记录下来，修改意见用于完善提示词。

```
AI 生成第一版
  → 人工修改（修改过程被 log 记录）
  → 独立子 agent 对比原始和修改后版本
  → 提取"为什么改"的通用规则
  → 写入 skill 或编码规范
  → 下次 AI 自动加载
```

nop-app-erp 项目积累了 19 个可复用 skill，全部来自这个循环。

## 九、E2E 测试的 AI 自动生成

E2E 测试手写成本高，可以让 AI 自动生成。关键是降低难度：封装 PageObject 模式，提供 getFieldValue(containerLocator, fieldName) 等简化操作，让 AI 不需要处理复杂 DOM 选择器。

然后就是 §八的循环：AI 生成 → 人工纠正 → 提取规则 → 写入 skill → 下次改进。nop-app-erp 从 0 到 260+ spec，初期人工纠正频繁，后期基本自动生成。

## 十、通用应用场景

四个场景共享同一默认 flow，区别在 prompt 集和 commands；flow 引擎、plan 格式、恢复机制全部相同。

| 场景 | DRAFT | EXEC | AUDIT |
|------|-------|------|-------|
| 数据处理 | 读 scripts/index 拟制 pipeline | 调预置脚本 | 数据质量断言 |
| 文档对比分析 | 拟制分析维度 | 读源码生成对比表 | 分析覆盖度检查 |
| 技术调研 | 拟制调研提纲 | 搜索+阅读+总结 | 结论可靠性检查 |
| 代码开发 | 读设计文档拟制 plan | 改代码 | 测试+lint+审查 |

---

# 第四部分：案例研究

## 十一、nop-app-erp：22 天 154 模块

nop-app-erp 提供了一个可公开审计的案例：多层 Loop 嵌套如何驱动 AI 从空骨架产出产品级 ERP。

### 规模指标（经独立审计校准）

| 维度 | 数值 |
|------|------|
| 开发周期 | 22 天 |
| 业务域 | 18 + 1 |
| Maven 模块 | 154 |
| 自有实体 | 352 + 110 引用桩 |
| Java 测试 | ~2890（0 failures） |
| Playwright E2E | 260+ spec（0 回归） |
| Plan 文件 | 187 份（全部双审计） |

### 一次真实的循环（07-10）

```
08:00 CHECK     — mvn 全绿
08:05 REVIEW    — 4 个 draft → 独立审查 → all active
09:30 EXEC      — 4 个 plan 全部执行完毕，全绿通过
16:00 DRAFT     — 路线图工作项全部 done
16:05 DEEP_AUDIT — 自动启动深度审计
```

一次循环约 8 小时。整个路线图横跨 22 天，由数十次循环完成。

### 编码前缺陷拦截

07-10 的一个 plan 批次，独立草案审查拦截了 4 个 P0 缺陷：码值冲突、BUDGET 污染实际财务、GlBalance 架构前提错误、维度歧义。全部在编码前拦截。没有 Plan Loop，这些缺陷会在实施甚至运行阶段才暴露。

### 知识转移曲线

用户介入分三类：A 类（明确指明平台机制，集中在早期）、B 类（指明工程原则方向）、C 类（只让 AI 自查对比，后期为主）。

```
06-22  AAAAAAAAAAA  A 类密集
06-29        CCCCCCCCCC  C 类为主（grill 83 问，仅 4 题需纠正）
07-04+         CCCCCCCCCCCC  几乎全 C 类
```

两条曲线在 06-29 ~ 07-01 交叉，此后 AI 自主成为主要工作模式。

"用户后期介入归零不是因为 AI 学会了写代码，而是因为吸引子已经定义好了（吸引子的形式化定义见 §十四）。方向对了，AI 能自动扩张。"

人的注意力应该花在定义吸引子上，而不是监督执行。Mission Driver 做的是后者。整个 22 天只有 28 次人类干预，且集中在项目早期。后期随着吸引子定型，人类介入归零，AI 完全自主推进。

---

# 第五部分：深度分析

## 十二、Loop Engineering 原理

传统的 pipeline 假设每一步是确定性的。但 AI 步骤是概率性的——同一个 prompt 在不同 session 可能产生不同结果。

Loop 嵌套的优势：失败是预期的（loop 天然支持重试和跳过）、质量是迭代的（audit 发现后可以改进）、恢复是自然的（checkpoint 就是磁盘上的 checkbox）、隔离是结构性的（子流边界 = 容错边界）。

三个原则：轨迹可恢复（持久化到磁盘，崩溃后磁盘扫描恢复）、局部容错（子任务失败不传播到父循环）、独立验证（完成与否由独立子代理审计，不由执行者自验）。

### 稳定保障

| 防线 | 机制 | 作用 |
|------|------|------|
| 磁盘持久化 | 所有状态写磁盘 | 崩溃 → 重启 → 磁盘扫描 → 断点恢复 |
| 子流隔离 | 每个 plan 独立子流 | 局部错误不扩散 |
| 重试预算 | 每步最多 3 次全新 session | 避免同一上下文重复失败 |
| 死循环检测 | ping_pong + max_cycles + max_total_steps | 防止无限循环 |
| 看门狗 | 60 分钟子 agent 超时 | 防止挂死 |
| 独立审计 | CLOSURE_AUDIT 用不同子代理 | 防止执行者自欺 |

### 恢复机制

恢复不是 replay（不重新执行历史步骤），而是 disk scan（扫描磁盘标记）。引擎启动后扫描 plansDir 的 .md 文件，找到 Status: active 的 plan，读 checkbox（[x] 跳过，[ ] 恢复）。run-state.json 的 steps 历史仅用于审计查看，不驱动恢复。

## 十三、Plan Loop 与验证体系

每次 plan 的执行本身有一个内部控制循环——Plan Loop。生命周期：draft → 独立草案审查（fresh session）→ active → 执行 → 独立结束审计（fresh session）→ completed。

核心原则：生成与验收必须分离。草案审查和结束审计都由独立子代理在全新会话中执行，审计者不继承执行者的上下文，从零开始读仓库。

### 验证体系：脚本检测 + AI 自动改进

验证不是单纯的机械检查或 AI 审计，而是两者的配对协作。脚本自动检测，发现问题后 AI 自动诊断并修复，然后重新检测。

```
EXECUTE (agent: AI 执行 plan)
  ↓
CLOSURE_SCRIPT_CHECK (script: 检查 checkbox + evidence)
  ├── pass → BUILD_VERIFY
  └── fail → CLOSURE_AUDIT (独立子代理审查)
               ├── 可修 → 回 EXECUTE
               └── 不可修 → 阻塞项移至 Deferred/Follow-up
                              plan 其余范围继续 → completed
BUILD_VERIFY (agent: AI 运行 test/build/lint)
  ├── 失败 → AI 自动诊断 → 修复 → 重新运行
  └── 通过 → plan completed ✓
```

| 检查点 | 检测方式 | 发现问题后 |
|--------|---------|-----------|
| CHECK | 确认可编译、测试通过的基线 | AI 自动修复（最多 3 次全新 session） |
| CLOSURE_SCRIPT_CHECK | script 检查 checkbox + evidence | 进入 CLOSURE_AUDIT → 回 EXECUTE；不可修项移至 Deferred/Follow-up |
| BUILD_VERIFY | AI 运行 test/build/lint | AI 诊断 → 修复 → 重新运行 |
| DEEP_AUDIT | 多维度审计 + 对抗审查 | 生成 remediation plan |

机器负责裁判，AI 负责改进。

## 十四、AGE 理论：吸引子与动力系统

Mission Driver 是 AGE（Attractor-Guided Engineering）理论在工具层的核心实现：

```
状态空间 = 仓库/项目的所有可能状态
吸引子   = docs 文档体系（design/architecture 等规范化文档）
           定义"系统应长期收敛到什么稳定结构"
轨迹     = plans + logs + audits 记录的"怎么走到现在的"
控制     = 健康检查 + 执行验证 + 独立审计 校正偏离
```

关键区分：roadmap 不是吸引子。Roadmap 是人类可控制的宏观规划，是吸引子的任务化投影。真正的吸引子是 docs 中的文档体系，变化速度远慢于 roadmap（季度级 vs 周级）。

### 吸引子的形成生命周期

吸引子不是一开始就完整的。它从模糊到精确：

```
一句话需求 → Grill Me 澄清 → 吸引子雏形（docs 初稿 + roadmap）
  → roadmap 初期：调研竞品，细化吸引子
  → roadmap 中期：按项执行，审计收敛
  → 定期插入：审计/重构/再思考，修正吸引子
  → 收敛完成
```

整个 roadmap 全自主执行。人类通过两个通道施加影响：执行前（Grill Me 澄清 + roadmap 设定）和执行中（异步注入 plan 到 plans 目录）。

### 异步人机交互

Mission Driver 执行中不需要人类实时交互。但人类可以随时通过 plans 目录异步注入新任务：

```
用户（或另一个 AI agent）生成新 plan → 放入 plans/ 目录
  → 下一轮 REVIEW_PLANS 自动拾取
  → draft 状态：独立审查 → 提升 active
  → active 状态：直接进入执行队列
```

默认不打断原则：即使当前执行可能有误，也优先不立刻打断。打断会丢失上下文；错误会被审计捕获；修正可以排队（插入修正 plan）。例外：破坏性操作、token 预算告警、明显死循环。

plans 目录是文件系统上的共享队列，多个贡献者可以独立写入：人类开发者、Code Review agent、DEEP_AUDIT 生成的 remediation plan，都进入同一队列。

## 十五、与 Codex goal 的对比

基于 Codex codex-rs/ 源码分析。Codex 有比较完善的机制（SQLite 状态库、goal 6 态状态机、pause/resume、token 预算），但以下维度仍有结构性差距。

| 维度 | Codex goal | Mission Driver |
|------|------------|----------------|
| 状态持久化 | SQLite + rollout JSONL。但运行时计量状态仅在内存 Mutex，崩溃即丢失；rollout 可能未物化 | checkbox 磁盘持久化 + run-state.json，所有状态在磁盘 |
| 独立验证 | continuation prompt 要求自审，但 update_goal(complete) 仅写 DB 不检查客观状态 | CLOSURE_AUDIT 强制由独立子代理执行 |
| 任务粒度 | 每 thread 至多 1 个 goal（单一目标 + token 预算） | 多 plan 共存管理：plans/ 可同时有多个 active plan，引擎顺序执行 |
| 异步交互 | 可 pause/resume 同一线程，但无法异步注入新任务 | 随时往 plans/ 塞 plan，下轮自动拾取 |
| 失败隔离 | 单 goal 内失败污染 context window | 子流隔离：一个 plan 失败不影响其他 |
| 终止保障 | token 预算 + blocked 检测（模型自报需 3 轮，或系统强制） | 多层防线（重试预算、死循环检测、看门狗等），全部外部强制 |
| 信息可见性 | SQLite + JSONL 需工具解析；运行时状态内存不可见 | 所有状态是文件，cat 即可读 |

METR 的研究表明，大量通过自动化测试的自主 agent PR 仍需显著人工修正才能合并（来源：METR developer productivity study, Becker et al., arXiv:2507.09089, July 2025）。原因在于缺乏独立审计层——agent 既是执行者又是验证者。

---

# 第六部分：愿景

## 十六、相关概念框架

**Harness Engineering**：构建控制 AI 行为的线束——plan 审计、验证命令、质量门控。Mission Driver 的 plan-execution 子流就是一个 harness。

**Loop Engineering**：用循环结构替代线性 pipeline。Mission Driver 的主循环与内嵌的 Plan Loop / Audit Loop 的多层嵌套是 Loop Engineering 的一种实现。

**SDD（Spec-Driven Development）/ OpenSpec**：先写规格再写代码。Mission Driver 的 plan（Goals / Non-Goals / Exit Criteria / Closure Gates）本质上是一份执行规格。

三者的交集正是 Mission Driver 的定位：用 Loop Engineering 的循环结构，在 Harness 的控制下，按照 SDD 的规格驱动 AI 工作。

## 十七、Goal-Driven 愿景与经济范式

Mission Driver 的演进方向是 goal-driven 的 AI 系统——用户说目标，系统自动澄清、规划、执行、验证。

```
用户: "帮我分析 DataFlow 和 Flink 的区别"
  → Grill Me（需求澄清）→ clarified spec
  → Auto-Detect（任务类型 → promptsDir）
  → Generate（roadmap + mission.json）
  → Run（自动循环执行直到完成）
```

其中 Generate（draft 命令）和 Run（run 命令）已有实现基础。Grill Me（deep-interview skill 增强）和 Wrapper（入口层）需要补建。

### 经济范式的转变

§一 已经指出人在环中模式的人时天花板。这里展开这个转变在设计上的具体含义。

Human On The Loop 的核心不是把人完全排除出去，而是改变人在系统中的位置：人从 loop 内部的执行节点变成 loop 外部的控制因子。AI 在 loop 内 7×24 自主推进，人脱离 loop，只在需要时按需介入——介入的频率和时机由吸引子定义的清晰度决定，而不是由 AI 的工作节奏决定。

这个结构性变化最深刻的影响是**模型选择策略的反转**：

- Human In The Loop 模式下，每一次产出都阻塞在人的确认上。人不能等，所以必须用最快最强的模型，哪怕它贵。响应延迟是首要约束，模型成本是次要的。
- 全自主运行模式下，没有人在等。弱模型虽然慢、token 消耗多、需要更多轮迭代才能完成工作，但它单价低、可以晚上跑、可以多个 mission 并行处理不同的 roadmap 工作项。响应延迟从约束变成可调度变量。

成本核算因此从"每小时人时产出"变成"每美元智能产出"——前者优化的是单次人机交互的效率，后者优化的是单位算力的累积产出。这两个优化目标导致完全不同的工程选择：前者追求单次对话质量，后者追求循环结构的稳定性和审计严密度。

## 十八、总结

Mission Driver 的价值在于三个方面：

第一，它用持久化的、可局部重试的 loop 嵌套替代线性 pipeline。AI 步骤是概率性的，需要重试、迭代、局部容错。子流隔离让一个 plan 的失败不影响其他 plan。

第二，它通过配置而非编程实现定制。引擎是通用 FSM DSL 执行器，5 步循环只是内置默认 flow——可以微调步骤、替换子流，也可以设计全新的流程。不同任务类型的区别在 flow + prompt + commands 三层。

第三，它把所有状态放在磁盘上。checkbox 持久化 + 磁盘扫描恢复，进程崩溃后无损恢复。

它不局限于代码开发。任何需要多步迭代、质量审计、容错恢复的复杂任务，都可以用它驱动。这正是 Loop Engineering 作为一种工程实践的落地实现。

---

## 进一步阅读

- Mission Driver 完整文档：`.opencode/skills/mission-driver/SKILL.md`
- Mission Config Schema：`.opencode/skills/mission-driver/references/mission-config-schema.md`
- Plan 编写指南：`ai-dev/plans/00-plan-authoring-and-execution-guide.md`
- AGE 理论深度分析：`ai-dev/analysis/2026-06-07-trellis-vs-age-comparison.md`
- nop-app-erp 案例完整材料：`~/app/nop-app-erp/docs/ppts/`
- DataFlow Harness 完整推演：`ai-dev/analysis/2026-07-25-opendcai-vs-age-vs-mission-driver.md`
