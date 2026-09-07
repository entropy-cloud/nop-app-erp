# Lesson 17: 代码 × 历史 × Deferred 三路交叉审计——ai-check-r2 mission 方法学沉淀

> **来源**：2026-08-28 ai-check-r2 mission 起草（roadmap `docs/backlog/ai-check-r2-roadmap.md`）+ M0 阶段产物（`docs/audits/check/2026-08-28-2049-ai-check-r2/` 7 个产物）+ M0.7 自审产物（`m0-7-self-audit-of-draft-plans.md`）。
> **本课性质**：**方法学沉淀课**——指向已存在但**未广泛复用**的 skill。**不要重新发明轮子**。
> **核心复用资源**：
> - `docs/skills/code-history-deferred-triangulation-audit-prompt.md`（核心 skill，已存在，详尽描述三路方法学）
> - `docs/skills/executions/audit-roadmap-authoring-workflow.md`（配套执行流 workflow）
> - `docs/backlog/ai-check-r2-roadmap.md`（首个应用本方法的 roadmap 范本）
> **适用场景**：任何"项目已积累 ≥ 20 份审计报告 + 历史 plan 累积 ≥ 100 份 + 多轮 mission 触达"的项目，**新一輪审计 mission 启动前**。
> **失败模式**：仅按"代码扫描"重新扫一遍（与前 mission 重复） / 仅按"历史 finding 列表"机械修复（重复派工） / 忽略历史 plan 的 Deferred / Successor 触发条件（隐性 finding 漏识别） / **重新发明 skill 范式**（导致方法学散落多处、不可复用）。

## 关键陷阱（**已在本会话验证**）

**1. 本课不是新方法学——是"已存在 skill 的应用推广"**——M0.7 沉淀过程中"方法学创造"的工作实质是把 `code-history-deferred-triangulation-audit-prompt.md` 描述的方法学**在 ai-check-r2 实际执行**，产物（roadmap + M0-M2 工作项 + 多次执行隔离目录 + 跨 mission 复用映射）是**应用产物**而非**方法学新发明**。

**2. M5.3 closure audit 的"6 项 CG"也是"已存在方法学的应用"**——`docs/architecture/state-machine-matrix.md §9` 已固化 6 CG + `tools/check-state-machine-coverage.sh` + `state-machine-coverage-check.py` 已实现所有验证逻辑。**M5.3 不是新设计，是 checklist 执行 + 产物落盘**。

**3. 警惕"方法学创造"的诱惑**——当 mission 起草或 audit 落盘时容易把"应用已有 skill 的产物"误标为"新方法学"。**测试方法**：检查产物是否真的新增了 skill/workflow 文档，还是仅消费了已有 skill？

## 核心论点

**新一輪审计 mission 的"非平凡增量" = 三路交叉**：

| 路 | 视角 | 工具 | 产物 |
|---|---|---|---|
| **第一路：代码** | 本轮切片按 B1/B2/B3/B4 维度（与上轮 D1-D10 正交）扫实际代码 | grep 程式 + owner doc 阅读 + 平台源码实证 | 新 finding |
| **第二路：历史** | 读 `docs/audits/` + `docs/lessons/` + `arm-index.md` + `ai-check-index.md`，按"复用 / 新增 / 残余风险"三态裁决 | 复用既有 finding 索引 | 已修复 / 同型待修 / 新增 3 列表 |
| **第三路：Deferred**（核心） | 读最近 50 份 plan 的 `## Deferred But Adjudicated` / `## Follow-up` / `## Non-Goals` 段，列每条 deferred 项的**触发条件 + 今天是否已满足 + 是否立项 finding** | grep + 人工裁决 | 已满足 / 部分满足 / 未满足 3 分类表 |

**第三路 = 隐藏的 finding 金矿**。本轮 M0.2 扫描揭示：

- **8 项已满足** deferred 触发条件（如 AMIS Runtime 包移除 82.5% 满足 → 100% 由本轮 non-standard-pages 计划收口触发）
- **11 项部分满足**（如 mfg 完工门控阻断点 = reportCompletion 非 close，2026-08-24 B4 已有断言但未跨域验证）
- **21 项未满足**（按 owner doc 与后继 mission 跟踪，本 mission 不直接处理）

8 项已满足中**至少 2 项是 ai-check-r1 未识别的隐性 finding 机会**（mfg 完工门控 + 全仓 #11 缺 writer 家族）——**这是 M1 切片 M2.x 修复批的"非扫描增量"**。

## 三路交叉审计的产物结构

按 ai-check-r2 M0 阶段验证有效的子目录结构（多次执行隔离纪律）：

```
docs/audits/check/<YYYY-MM-DD-HHmm>-ai-check-r2/
├── m0-1-baseline-snapshot.md          # 第一路：基线锚定
├── m0-2-deferred-trigger-index.md      # 第三路核心：40 项 deferred 三态分类
├── m0-3-open-findings-bucketing.md     # 第一路+第二路：488 finding 域分布 + 13 批修复边界
├── m0-4-closure.md                     # 收官
├── m0-5-dimension-and-cross-pattern-analysis.md  # 第一路+第二路：D1-D10 维度分布 + 同型 finding 合并
├── m0-6-cross-mission-reuse-map.md     # 第二路：跨 mission 触达状态
├── ai-check-r2-index.md                # 本轮统一索引
└── ck-<slice>.md + 触发条件扫描          # M1 切片产物（按域独立文件）
```

## 多次执行隔离纪律（**M0 阶段已验证的强制规则**）

**每次执行必须新建 `<YYYY-MM-DD-HHmm>-ai-check-r2/` 子目录**，不复用历史目录，历史子目录只读保留。**理由**：
- mission driver 重跑可能产生不同产物（如 mvn test 结果变化）
- 跨 mission 复用必须显式从历史子目录引用（不留 single-of-truth 风险）
- 符号链接 LATEST（如 `LATEST-m5-2`）指向最新一份可执行

## 关键设计抉择

**1. 状态机延用，不新建 ID 空间**——`P{0-3}-CK-{域短码}-{NNN}` 连续编号，状态机 `open`/`verifying`/`fixed`/`not-a-problem`/`deferred` 与第一轮 ai-check 完全一致。**避免 mission 闭包成本**。

**2. 同型 finding 合并基类**——按 7 大同型 finding 合并基类（CRUD 无状态守卫 / REQUIRES_NEW 凭证悬挂 / 跨域反写闭环 / currentUserId 宽 catch / notify 模板种子 / cron 键漂移 / orgId 隔离），**避免每个 finding 单 plan 单测试的颗粒碎度**。本轮 M2.x 13 批修复边界即按此基类聚合。

**3. 保护区域路由不绕**——保护区域（会计过账 / auth / 数据删除 / 跨域接口）的修复即使在 M0/M1 阶段也必须显式登记。**plan-first** + **dual-agent-approval** 是硬约束，**plan-guide #12 独立草案审查是硬约束**——**不因 mission 效率而降低保护**。

**4. 多次执行隔离纪律**——M0.5/M0.6 已在 m0-2/M0-3 子目录内，本轮不写到 `docs/audits/check/` 扁平空间。**多次执行隔离** 是 M0 阶段的硬纪律，违反将导致 mission driver 重跑覆盖历史审计证据。

## 与既有 lesson 的关系

| Lesson | 关联点 |
|---|---|
| Lesson 02（cross-ref renumber scan）| 第二路索引依赖 finding ID 体系稳定 |
| Lesson 08（plan closure without independent audit）| 独立草案审查 + 独立 closure audit 是 mission 收官硬约束 |
| Lesson 11（index status not backfilled after fix）| finding 状态回写是 mission 收口必要动作 |
| Lesson 12（documented simplification abuse）| 第三路 deferred 扫描揭示的"已满足 deferred"可能正是 documented simplification 的根因 |
| Lesson 15（xbiz XScript 编排下沉 Java Bean）| plan-level 实施受 plan-guide 约束 |
| Lesson 16（跨仓库 schema 契约验证）| 第二路索引中需消费端源码验证 |

## 关键陷阱（执行期已验证）

1. **子 agent 通道不稳**——本会话 6 次子 agent 派发 5/6 失败（统计：6 派发 5 失败 + 1 计划起草可能成功）。**plan-audit 不能依赖子代理**时，**M5.3 / V.2 等需独立审查的收口工作会受阻塞**。**应对**：登记 successor 触发条件 = 子代理通道恢复 + 人工裁决。
2. **plan-guide #12 vs 主会话执行能力**——本规则不可降级（#13）。**主会话能做的 ≠ 计划能做的**。仅文档、状态、索引、计划起草可主会话完成；业务代码修改、mission closure 均受 plan-audit 阻塞。
3. **多次执行隔离 vs "ai-check-r2 是新一轮"**——isolation 纪律**绝对不能**因"mission 还没收口"放松。每次重新执行必须建新子目录，不复用历史。

## 防御机制（强制清单）

新 mission 启动前，逐项执行：

1. **开三路证据索引（M0.2）**——读最近 50 份 plan 的 Deferred/Successor/Non-Goal 段，列已满足 / 部分满足 / 未满足 3 分类。**这一步是 mission 的"非扫描增量"**。
2. **设多次执行隔离目录**——M0 阶段第一动作必建 `<YYYY-MM-DD-HHmm>-<mission>/` 子目录，路径在 mission.json `executionIsolationDiscipline` 字段声明。
3. **保护区域显式登记**——M2.x 修复批若触及保护区域，每个 plan 必含 dual-agent-approval 步骤。**不因 mission 效率优化而绕过**。
4. **跨 mission 触达率评估**（M0.6 复用映射）——读前 mission arm-index.md 与本 mission finding 索引对照，标注"已 by 跨 mission 触达"的 finding 避免重复派工。
