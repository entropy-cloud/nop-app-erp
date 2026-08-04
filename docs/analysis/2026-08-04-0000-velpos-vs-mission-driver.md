# Velpos vs Mission-Driver：外部 Agent 驱动器的两种正交形态

> 研究判断与权衡分析。结论不被视为活动架构契约，是未来选型的候选依据。
> 相关方法论长文：`docs/articles/mission-driver--loop-engineering.md`（Mission Driver 原理）、`docs/articles/beyond-mission-driver-from-loop-control-to-attractor-guided-engineering.md`（AGE 理论）。

## 0. 快照与证据来源

- 调研日期：2026-08-04。
- **Velpos**：`C:/can/ai/velpos`（Python FastAPI + Vue3 + MySQL + Docker），后端约 455 py 文件，硬绑定 Anthropic Claude Code Agent SDK（`backend/infr/client/claude_agent_gateway.py`，`ClaudeSDKClient`）。
- **Mission-Driver**：`C:/can/nop/attractor-guided-engineering-template/tools/mission-driver/`（Node.js），9 个源码文件 + 12 个 prompts + 设计文档，零 npm 依赖（commander 被 vendor），驱动 opencode CLI。
- 两者均为**外部编排器**——驱动黑盒 CLI Agent（Claude Code / opencode），**均不实现 Agent 引擎本身**（无 ReAct 循环、无上下文压缩、无 LLM 管理）。Agent 智能全在外部进程。

## 1. 定位：同一个"驱动外部 Agent"赛道，两种形态

```
┌────────────────────────────────────────────────────┐
│  Velpos          ← 交互式 Web 产品（人在环）        │
│                  驱动 Claude Code CLI               │
├────────────────────────────────────────────────────┤
│  mission-driver  ← 无人值守 Headless 编排引擎      │
│                  驱动 opencode CLI                  │
├────────────────────────────────────────────────────┤
│  Claude Code / opencode  ← 被驱动的黑盒 Agent 进程  │
└────────────────────────────────────────────────────┘
```

区别全部体现在"怎么驱动"上，而非 Agent 能力上。

## 2. 关键对比表

| 维度 | Velpos | mission-driver |
|------|--------|----------------|
| **形态** | Web 控制台（Vue3 + FastAPI + WebSocket） | Node.js 状态机（headless CLI） |
| **交互模式** | 交互式、流式 chat、人在环 | 无人值守、批量循环 |
| **目标用户** | 非技术用户/产品/运营（图形界面） | AI 代理本身（自动化工作流） |
| **被驱动 Agent** | Claude Code（Anthropic 闭源） | opencode CLI（开源） |
| **执行单元** | 长驻会话（可持续交互、权限请求回传） | 一次性 `opencode run` 子进程（跑完即退） |
| **通信** | 实时 WebSocket 双向流 | 磁盘文件 + `<AI_STEP_RESULT>` marker 事后抠 |
| **状态机** | 无（状态在 DB，靠 UI 人工驱动） | **有 FlowEngine**（CHECK→REVIEW→EXEC→DRAFT→AUDIT） |
| **任务编排** | 手动：建会话、发消息、分支、调度 | 自动：扫描 roadmap/plan 文件自动循环 |
| **多 Agent** | 有团队域模型（Team/AgentSlot/Handoff/FlowPlan/WishCard） | 无（单进程串行 spawn，subflow 同进程递归） |
| **成果判定** | 人在聊天里判断"完成没" | 引擎审计 plan 复选框 + closure audit 判定 |
| **记忆** | 会话记录 + CLAUDE.md 版本管理 | file-based Reflexion（`memory/_index.md` 注入下次运行） |
| **分支/对比** | Git worktree 并行分支 + 差异对比 + 收敛 | 无 |
| **调度** | Project Clock 定时任务 + IM 推送 | 无定时（重跑 = 断点恢复） |
| **IM 集成** | 飞书/微信/QQ/OpenIM 双向同步 | 无 |
| **失败恢复** | 会话状态持久化 + 连接超时管理 | 扫磁盘 plan 断点恢复 + ping-pong/cycle 检测 |
| **规模** | 455 py + Vue 前端 + MySQL | 9 JS 源文件（零依赖） |
| **可移植性** | 硬绑定 Claude Code SDK | stack-agnostic（任何有 shell 命令的项目） |

## 3. 五个本质差异

### 3.1 人在环 vs 无人值守（最根本）

**Velpos** 解决"让人能给 Claude Code 一个好的操作界面"：流式 streaming、权限请求弹窗（`PermissionRequestBlock.vue`）、TodoWrite 内联进度、内嵌终端、会话分支对比、IM 消息同步。**人在环是它的核心场景**。

**mission-driver** 解决"让 AI 在没有人的监督下自我完成整个开发循环"：spawn 子进程 → 等退出 → 抠 marker → 决定下一步。`flows/mission-driver.json` 的 `markerAliases`（`ok→pass`、`none→created`）专门为容忍 AI 输出不确定性而设计。**无人值守是它的核心场景**。

### 3.2 状态机的有无（最大的技术鸿沟）

- mission-driver **有一切**：FlowEngine、subflow 递归、forEach 数据源（`activePlans()`）、marker 转换表、ping-pong 检测、maxCycleVisits、transient error 独立重试预算、`_wfClose()` 原子写盘。`EXECUTION-PRINCIPLE.md` 597 行完整阐述进程模型、中断清理、marker 引擎、断点恢复。
- Velpos **无状态机**：没有"下一步该干嘛"的概念，全部由用户在 UI 手动发起。

**后果**：mission-driver 跑 2 小时能自动完成"检查→执行计划→起草新计划→深度审计→回到执行"的完整闭环；Velpos 跑 2 小时只是维持了一个供人操作的会话面板。

### 3.3 "完成"如何判定

- mission-driver 靠**契约**：plan 文件复选框必须勾完 + closure audit 独立跑过 + `BUILD_VERIFY` 命令通过才 `completed`。有防伪机制（`CLOSURE_SCRIPT_CHECK` 脚本检查）。
- Velpos 靠**人**：人觉得好了就好，无强制审计后归档的硬门。

### 3.4 通信模型

- Velpos：实时双向（WebSocket 流式 token、权限请求回传、后台任务事件）。
- mission-driver：**单向、事后、文本**。父进程等子进程退出后 `readFileSync` 全文读日志，正则抠 `<AI_STEP_RESULT>`。

**关键后果**：mission-driver **无法处理运行时需人确认的场景**（如 Agent 中途问"要不要执行危险命令"）；Velpos 专门处理此场景。

### 3.5 复现/可移植性

- mission-driver：9 源文件、零依赖、纯磁盘文件状态——可随 AGE 模板 `install-age.sh` 一键装进任何项目。你的质量流程（plan/audit 规范）就是它的"业务逻辑"。
- Velpos：一个需部署的**完整产品**，硬绑定 Anthropic 闭源 Claude Code。

## 4. 复杂度来源对比（反直觉点）

| | Velpos 的复杂度 | mission-driver 的复杂度 |
|--|----------------|------------------------|
| 复杂在哪 | **交互与产品**：会话生命周期、连接池、流式传输、权限协议、IM 适配、分支合并 | **流程确定性**：状态机收敛、防死循环、防伪闭环、断点恢复、marker 解析容错 |
| 换来什么 | 人的操作体验、协作便利 | 自动化的**可证明**完成（审计门控） |
| 实现语言 | Python async + Vue | 纯 Node.js（无依赖） |

## 5. 使用场景判断

| 你要什么 | 选谁 |
|----------|------|
| 给**人**用的 AI 工作台（看进度、回权限、开分支、IM 收通知） | Velpos |
| 给**AI**用的自动化开发循环（无人值守跑完 roadmap，强制审计闭环） | mission-driver |
| 非技术团队"打包"自己的 Agent 角色 + 插件复用 | Velpos（核心卖点） |
| 固化"计划→实现→审计→沉淀"SOP 为可重复流程 | mission-driver（状态机天然表达） |
| 需要人在 Agent 干活中途参与决策 | Velpos |
| 需要完全自动化并且能证明"真的做完了" | mission-driver |

## 6. 结论

**Velpos 和 mission-driver 是"同一个问题（驱动外部 Agent）的两个正交解"：**

- Velpos：把 Agent 变成**人**能优雅操作的**产品**——交互、协作、运营。
- mission-driver：把 Agent 变成**流程**能自主推进的**引擎**——契约、状态机、审计门控。

它们甚至可以互补：理想系统 = mission-driver 提供无人值守自动开发循环 + Velpos 提供人工介入的观察/决策界面。但实际两者各自封闭（Velpos 绑 Claude Code、mission-driver 绑 opencode），互不打通。

**一句话**：mission-driver 做"自治"，Velpos 做"交互"；一个为无人值守的确定性而生，一个为人机协作的体验而生。与 nop-ai-agent（自研白盒 Agent 引擎，Nop 生态内部）相比，二者均仅编排黑盒 CLI，不与之同层竞争。