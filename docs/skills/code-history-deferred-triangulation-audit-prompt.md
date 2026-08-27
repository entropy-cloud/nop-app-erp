# 代码 × 历史 × Deferred 三路交叉审计提示

> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` auto + dual-agent-approval、会计/财务/数据删除，双独立子 agent 分别批准）、验证命令（`mvn clean install -DskipTests` / `mvn test` / `bash docs/audits/nop-compliance-checker.sh`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。本提示的通用默认值在本仓库不充分。
>
> **保护区域授权（本提示词特有，覆盖项目默认 dual-agent-approval）**：本提示词作为审计方法，**不改任何代码、ORM、配置**——它只产出 finding 列表登记到审计结果目录 + 同步更新索引。修复必须走专门修复 plan（见 §与现有 skill 的边界声明）。这一保护区域守门是为了避免「审计抢跑修复」（ai-check roadmap 已固化「两阶段时序硬约束」）。

## 用途

对一个**已经过多次审计、体量大、容易产生疏漏**的复杂项目（或它的下一个切片）做**三路交叉**审计：

- **第一路 = 当前实现代码**（`module-<domain>/` 手写 Java / orm.xml / 调度配置）
- **第二路 = 历史审计记录**（`docs/audits/` 下所有 `*.md` + `docs/lessons/*.md` + `docs/audits/{arm,rc,check}-index.md`）
- **第三路 = 历史 plan 中的 deferred 项 / Non-Goal / successor 触发条件**（`docs/plans/**/*.md` 的 Deferred 段、Successor Required 段、CR 段 + `docs/backlog/*.md` 的 Deferred 注记）

三路交叉的核心问题不是「当前代码有没有 bug」（那是 `code-quality-audit-prompt` 的工作），而是：

> **历史已经识别过 / 显式 defer 过 / 已文档化为 Non-Goal 的问题，今天的实现里是否仍然存在？是否仍然处于触发条件未满足状态？是否有同类问题在历史未覆盖的代码路径上以新形态出现？**

### 何时使用

- 项目已积累多轮审计（`docs/audits/` 文件 ≥ 20），存在 arm-index / rc-index / ai-check-index 多个索引
- 历史 plan 中存在大量显式 deferred 项（"触发条件 X 满足后实现"，"下次审计时复核"），怀疑有些触发条件已满足但未被识别
- 怀疑历史识别的某类问题在另一域 / 另一模块以同型出现（如「宽 catch 吞咽」从一个站点传染到 12 站点）
- 需要为下一轮 roadmap 提供审计输入（场景：arm mission 已闭合、ai-check 第一轮已收口，需要 ai-check 第二轮 / 第三轮）

### 何时不使用

- 项目刚起步，历史上无任何审计记录 → 不需要三路交叉，直接用 `code-quality-audit-prompt` + `state-machine-business-review-prompt` 即可
- 任务是对单一对象做窄审计（一个字段、一个 method、一个文件） → 用对应专项提示
- 任务是设计文档 vs 实现 drift → 用 `design-doc-audit-prompt` / `requirement-compliance-audit-prompt`
- 想直接执行修复而非审计 → 三路交叉审计只产出 finding，修复走专门 plan

### 与现有 skill 的边界声明

| 既有 skill | 与本 skill 的关系 |
|---|---|
| `audit-remediation-roadmap-authoring-prompt` | 它是 **roadmap 设计师**（产物 = `roadmap.md` + `mission.json` + 审计维度矩阵）。本 skill 是 roadmap 设计师在「步骤 1 来源 B：残留风险维度」与「步骤 2：汇聚未闭包发现」的具体方法论。两者是 **设计师 vs 审计员** 的关系：roadmap 编排调用本 skill 作为某一维度的取证步骤。 |
| `behavioral-failure-mode-scan-prompt` | 它是**单域单切片代码层行为扫描**（B1-B4 四类失败模式 + grep 程式 + 决策树）。本 skill 是**全仓三路交叉审计**。两者抽象层不同：BFMS 是「深度」（单域穷举 4 类行为失败），本 skill 是「广度」（跨域、跨历史、跨 deferred 找遗漏点）。**互补**：BFMS 在每个域切片上穷举，本 skill 跨域找遗漏。 |
| `code-quality-audit-prompt` | 通用代码质量审查。本 skill 在它的基础上叠加了**两条额外证据来源**——「历史审计复用裁决」与「deferred 触发条件已满足扫描」。无历史与 deferred 上下文时退化等同 code-quality。 |
| `multi-dimensional-audit-prompt` | 整件工作多维挑战。本 skill 可作为 multi-dim 内的「事实证据采集步骤」——特别是证据来源 = 三路交叉的具体 finding。 |
| `compliance-baseline-drift-adjudication-prompt` | 单文件级 R*.x 漂移裁决（grep 实仓站点 vs baseline）。本 skill 在站点级（function/slice）的三路交叉——抽象层不同：前者是「合规维度」、本 skill 是「行为维度」。 |
| `closure-pending-detection-prompt` | 检测缺 closure audit 证据的计划。本 skill 检测**缺审计覆盖的功能**——前者审计划过程，后者审功能本身。 |
| `requirement-compliance-audit-prompt` | 五级追踪矩阵审 owner doc ↔ 实现。本 skill 不审需求契约符合性，专注**实现层已识别的同类遗漏**。 |
| `state-machine-business-review-prompt` | 设计层状态机图审查。本 skill 不替代它，但 §2 的「历史 dict 死状态清单」可作为状态机审查的代码层证据输入。 |

**理由**（避免 skill 膨胀，README §反模式 #4）：本 skill 抽象层独特——它不是「单域深度」也不是「整件多维」也不是「roadmap 编排」也不是「单类失败模式扫描」，它是**「历史上下文驱动的全仓交叉审计」**。该抽象层在既有 25 个 skill 中是空缺。新建本 skill 填补此空缺，**不替代**任何既有 skill。

---

## 提示词主体

```text
你是高级 ERP 架构师与项目历史审计专家。对一个复杂项目（或其下一个切片）做"代码 × 历史 × Deferred"三路交叉审计。本 skill 的产物 = finding 列表（存本轮审计结果子目录下的 `ck-<slice>.md`）+ 同步更新本轮索引。

你**不修改任何代码、ORM、配置或文档**——所有 finding 进入修复阶段后由专门修复 plan 处理（参考 ai-check-roadmap §两阶段时序硬约束）。这一保护区域守门是为了避免"审计抢跑修复"。

## 步骤 0 — 强制前置阅读

### 项目上下文（必需）
- `AGENTS.md` §任务路由 + §强制技能加载 + §当前项目阶段
- `docs/context/project-context.md`（当前阶段、验证命令、阻塞条件）
- `docs/context/ai-autonomy-policy.md`（保护区域表）
- `docs/context/codebase-map.md`（19 域结构、入口点）

### 审计目标范围（必需）
- 本轮审计切片（一个域 / 一组域 / 全域）
- 该切片的所有 owner doc（`docs/design/<domain>/`）
- 该切片的 ORM 模型（`module-<domain>/model/app-erp-<domain>.orm.xml`）
- 该切片的实际手写代码（`module-<domain>/erp-*-service/src/main/java` + `module-<domain>/erp-*-web/src/main/resources/_vfs/`）
- 该切片的调度配置（`*.batch.xml` / `nop-job` / cron 配置）

### 历史审计记录（必需——本 skill 的核心证据来源）
- `docs/audits/` 下全部文件（按时间倒序读最近的 20+ 份；优先读 `*-index.md` 与最近的 mission 收口报告）
- `docs/audits/{arm,rc,check}-index.md`（如有）——三路交叉的"第二路索引"
- `docs/lessons/` 下全部经验笔记（特别是 `01-15-*.md`）
- `docs/backlog/*-roadmap.md` 下所有 backlog roadmap（特别是 mission driver 驱动的 mission）——找历史 deferred 与 Non-Goal 注记

### 历史 plan 中的 deferred / successor（必需——本 skill 的核心证据来源之二）
- `docs/plans/` 下全部文件（按时间倒序读最近的 50+ 份）
- 每份 plan 必须读其 `## Closure Gates` 与 `## Deferred` 段
- 找以下三类条款：
  1. **「Deferred - 触发条件 X 满足后实现」** —— 评估今天是否已满足
  2. **「Successor Required - 当 Y 发生时启动」** —— 评估今天是否已发生
  3. **「Non-Goal - 不在本计划范围（仅 X 在范围内）」** —— 评估今天的实现是否突破了 Non-Goal 边界

### 既有 skill 库（必需——本 skill 与之互补而非替代）
- `docs/skills/README.md` §项目定制化层 + §已知失败模式（13 项）
- `docs/skills/behavioral-failure-mode-scan-prompt.md` §1-§4（B1-B4 四类失败模式 + grep 程式 + 决策树）——本 skill 调用其 grep 程式作为"第一路"取证
- `docs/skills/code-quality-audit-prompt.md` ——本 skill 调用其 7 个重点领域作为"第一路"通用框架

读完以上后，你应该能回答：
- 本切片历史上已被识别的同类问题是什么（按 lesson / P编号汇总）？
- 本切片历史上显式 deferred 的触发条件今天哪些已满足？
- 本切片历史上已审计过哪几个维度？哪些维度是已知盲区？

## 步骤 1 — 建立本轮审计结果目录与三路证据表

### 1.1 创建本轮审计结果子目录（**多次执行隔离纪律**——必做）

每次执行本 skill 都要在 `docs/audits/check/` 下创建一个**新的、以日期时间 + mission 名开头**的子目录，**严禁**直接写入 `docs/audits/check/ck-*.md` 的扁平空间——多次执行会冲突（finding ID 重复、索引覆盖、报告混入）。

**目录命名规范**：

```
docs/audits/check/<YYYY-MM-DD-HHmm>-<mission-name>/ck-<slice>.md
docs/audits/check/<YYYY-MM-DD-HHmm>-<mission-name>/<mission-name>-index.md
```

**示例**：
- `docs/audits/check/2026-08-28-0930-ai-check-r2/ck-finance-posting.md`
- `docs/audits/check/2026-08-28-0930-ai-check-r2/ck-mfg-workorder.md`
- `docs/audits/check/2026-08-28-0930-ai-check-r2/ai-check-r2-index.md`
- `docs/audits/check/2026-09-15-1400-ai-check-r3/ck-finance-posting.md`（同一 mission 第三轮，**绝不**覆盖 08-28 的目录）

**纪要要求**：

| 要素 | 规则 | 违反后果 |
|---|---|---|
| 目录名格式 | `YYYY-MM-DD-HHmm-<mission-name>` 严格 ISO 风格 + 小写连字符 mission 名 | 排序混乱、跨 mission 难定位 |
| 时间戳精度 | 至少分钟级（HHmm）。同一 mission 同分钟内尝试启动会被命名冲突挡掉——必须用秒级补齐（HHmmss） | 同 mission 同分钟内并发启动会冲突 |
| 严禁覆盖 | 每次执行都是只读历史目录，写入新目录 | 历史 finding 丢失 |
| 索引并行 | 每轮独立 `<mission-name>-index.md`，**不**统一索引（统一索引由 ai-check-index.md 跨轮聚合） | 跨轮索引污染 |
| 跨 mission 命名 | 多个 mission 可并行（如 audit-remediation + ai-check-r2）→ 用各自 mission 名作前缀 | mission 边界混淆 |

**`docs/audits/check/ai-check-index.md` 的角色变化**：

- 它**不是**某个单轮的执行目录——它是**跨轮 finding 聚合索引**（finding ID 全局连续 + 状态机跨轮继承）
- 本轮目录下的 `<mission-name>-index.md` 是**本轮 finding 索引**（仅含本轮 ck-*.md 的 finding）
- 跨轮 finding 状态延续规则：发现一条新 finding → 在 ai-check-index 中追加一行（如果 ID 冲突则按轮次加 `-r2/-r3` 后缀，如 `P0-CK-mfg-001` → `P0-CK-mfg-001-r2`）

### 1.2 建立三路证据表

产出**三列证据表**：

#### 第一路：当前实现代码的事实证据

按 `behavioral-failure-mode-scan` 的 grep 程式扫本切片：
- B1 业财过账吞异常悬挂（`catch(Exception)` + 只 log.warn）→ 见 BFMS §1
- B2 dict 死状态（`setStatus` writer 缺失）→ 见 BFMS §2
- B3.1 调度链断裂（job 间接力 / 重试对称）→ 见 BFMS §3.1
- B3.2 守卫散点（状态机迁移 / 乐观锁 / 权限注解 / 输入边界）→ 见 BFMS §3.2
- B4 平台反模式（`@Inject private` / `System.currentTimeMillis()` / 非 `NopException` 异常）→ 见 `nop-platform-conformance-audit` 维度
- **扩展维度**（本 skill 特有）：
  - **跨域反写闭环**：源域 `posted=true` 后，过账引擎是否发事件？财务侧 `VoucherPostedEvent` 是否被各域 `IErpFinVoucherPostedListener` 接收？无源域 writer + 无跨域 listener = 闭环断裂
  - **增量/边界行为**：retry / 多次报工 / 部分核销 / 边界日期等增量路径——单测通常只测单发达量，增量路径无覆盖
  - **silent failure 三态**：catch 后既不 rethrow 也不记 FAILED 也不告警——伪装成成功的失败

每个发现记一行：`file#method` 控制点 + 关键代码摘录（≤5 行）+ 信号核查结果。

#### 第二路：历史审计记录的复用裁决

读 `docs/audits/` 与 `docs/lessons/`，对**每个同型问题**做「复用 or 新增」裁决：

| 历史 finding | 控制点（历史） | 状态（已闭包 / deferred / open） | 本切片同型控制点 | 裁决 |
|---|---|---|---|---|
| P1-MA2-032 宽 catch 吞咽 | `ErpFinXxxProcessor.java:108` | 已 fix（R1.16） | `ErpInvXxxProcessor.java:152` | 复用历史 fix 范式 + 在新切片立项 finding |
| P1-RC-024 dict 死状态 | finance `erp-fin/voucher-status` DRAFT | 已 defer（待 BC 计划） | inventory `erp-inv/batch-status` RELEASED | 复核 defer 触发条件是否满足 |
| ... | ... | ... | ... | ... |

**裁决规则**：
- 同型已 fix：复用 fix 范式 + 新切片立项 finding（不重复历史编号）
- 同型 deferred 且**触发条件今天已满足**：立项 finding（这是本 skill 的核心价值）
- 同型 deferred 且**触发条件仍未满足**：登记"残余风险"入 `ck-*.md` §剩余风险声明（不算新 finding）
- 本切片新发现且历史无同型：立项 finding

#### 第三路：历史 plan 的 deferred 触发条件扫描

对每个 `docs/plans/YYYY-MM-DD-HHmm-*.md`：
1. grep 「`Deferred`」/「`Successor Required`」/「`Non-Goal`」段
2. 提取每个 deferred 项的**触发条件**
3. 对照今天的实现（`git log` + `module-<domain>/erp-*-service/src/main/java`）评估触发条件是否已满足
4. 满足者：立项 finding（指向该 plan 编号 + 原触发条件 + 现状证据）
5. 不满足者：登记"残余风险"

**示例**：

```markdown
### 历史 deferred 项（第三路扫描结果）

| 来源 plan | deferred 触发条件 | 当前状态 | 裁决 |
|---|---|---|---|
| `2026-07-04-1452-1.md` Deferred "Maintenance 红冲链路" | 当 mnt 域到达 `approveStatus` 主路径后 | mnt approve 路径已上线 | **立项 P1-CK-mnt-013** |
| `2026-07-06-1606-1.md` Deferred "OEE 精确计算" | 当设备数据采集层到位时 | 未到位 | 残余风险，watch-only |
| `2026-07-09-1249-1.md` Deferred "Payment xwf E2E" | sysUser 兜底找到方案后 | 2330-1 已证实 NOT FEASIBLE | 残余风险（结论反转，非触发） |
```

## 步骤 2 — 三路交叉与 finding 立项

对每个候选 finding，做**三路交叉验证**：

- **第一路**：代码确有 bug（控制点 + 信号核查）
- **第二路**：历史无同型 fix（findings 编号 grep 零命中），或历史同型但本切片控制点不同
- **第三路**：历史 plan 无 deferred 该问题 / 历史 deferred 已触发

满足以上三路才立项。立项后填 finding 表（见 §3 报告格式）。

**避免两类误判**：
- **历史已 fix 但新切片有同型**：不复用历史 finding ID（finding ID 切片内连续），但登记 finding 必填「`arm-index 复用裁决`」列说明"复用 R*.x 范式"
- **历史 deferred 但触发条件今日仍未满足**：不算新 finding（避免未触发项污染 finding 列表）

## 步骤 3 — 报告格式（产出 `<执行目录>/ck-<slice>.md`）

报告骨架（5 段结构，参考 ai-check `docs/audits/check/ck-*.md`）：

### §1 元信息
- 范围（域 / 切片 / 文件清单摘要 + 计数）
- 方法（Skill 使用记录：本 skill + `behavioral-failure-mode-scan` + `code-quality-audit`）
- 检查日期、执行者
- **本轮执行目录路径**（`docs/audits/check/<YYYY-MM-DD-HHmm>-<mission-name>/`，明确标注"本目录为本次执行隔离区"）
- 与既有 ai-check-index 索引的衔接说明（"本报告延续 ai-check roadmap 工作项 C*.x"）

### §2 三路证据汇总
- 第一路：本切片 grep 程式命中摘要（按 B1-B4 + 扩展维度）
- 第二路：历史 finding 复用裁决表（必填）
- 第三路：历史 deferred 触发条件扫描表（必填）

### §3 finding 列表

每条 finding 含：
- **ID**：`P{0-3}-CK-{域短码}-{NNN}` 或 `P{0-3}-CK-{域短码}-{NNN}-r{N}`（同一 mission 多轮时加 -r2/-r3 后缀避免 ID 冲突；详见 §3.1）
- **级别**：P0/P1/P2/P3（沿用 ai-check 严重性定义）
- **维度**：`D{1-10}`（沿用 ai-check 维度分类，详见 ai-check-roadmap §检查方法）
- **控制点**：`file#method` 锚点 + 关键代码摘录（≤5 行）
- **问题描述**：触发条件 / 影响面
- **三路证据指针**：
  - 第一路：grep 信号核查结果
  - 第二路：`arm-index 复用裁决` 列（"新增（grep arm-index 零命中）" / "复用 R*.x 范式" / "复核 P1-RC-024 deferred" 等）
  - 第三路：`plan deferred` 列（指向 `docs/plans/YYYY-MM-DD-HHmm-*.md` 编号 + 原 deferred 条款）
- **建议修复方向**：1-3 行（不在本 plan 实施，仅指引修复 plan）
- **初始状态**：`open`

### §3.1 finding ID 跨轮冲突处理

当同一 mission 在不同 `<执行目录>` 多次执行时，finding ID 可能冲突（如两轮都发现 `P0-CK-mfg-001`）：

| 情况 | 处理 |
|---|---|
| 同 mission 同 ID 在历史轮次中已 `fixed` | 当前轮次**复用**历史 ID，沿用 `fixed` 状态（在 ck-*.md §3 注明"复用已 fixed，无新增 finding"） |
| 同 mission 同 ID 在历史轮次中 `open` | 当前轮次**复用** ID，沿用 `open` 状态（如本轮有新证据可升级到 P1/P0，则**新增**一条 `P*_CK_xxx_NNN-r2` 而**不是**修改原 ID） |
| 同 mission 同 ID 在历史轮次中 `deferred` 且触发条件仍未满足 | 当前轮次**复用** ID，沿用 `deferred`（在 ck-*.md §3 注明"复核 deferred 仍未满足"） |
| 同 mission 同 ID 在历史轮次中 `deferred` 且**触发条件已满足** | 当前轮次**新建** finding（`P*_CK_xxx_NNN-r2`），原 ID 状态置 `reopened`（在 ai-check-index 标注） |
| 新发现且无历史同型 | 新增 `P*_CK_xxx_NNN` 或 `P*_CK_xxx_NNN-r2`（按本轮是否首轮） |

**绝不在跨轮之间直接覆盖 ID**——避免历史 finding 信息丢失。

### §4 统计
- 按级别 / 维度计数
- 按三路来源计数（仅第一路 / 第一路+第二路 / 第一路+第二路+第三路）
- 与 ai-check 同域既有 finding 的对比（本切片是新开还是延续）

### §5 剩余风险声明（必填）
- 列出"未查区域"（哪些文件 / 维度本次未覆盖）
- 列出"残余风险"（触发条件未满足的历史 deferred 项）
- 列出"证伪项"（检查过但确认不是问题的，按第三段三态强制）

## 步骤 4 — 索引更新（与 ai-check-index 衔接）

报告产出即更新**两个索引**：

1. **本轮索引 `<执行目录>/<mission-name>-index.md`**：仅含本轮 ck-*.md 的 finding 列表
2. **`docs/audits/check/ai-check-index.md`**（跨轮聚合索引）：新增本轮 finding 行

**ai-check-index.md 跨轮聚合规则**：

| 情况 | 在 ai-check-index 中的处理 |
|---|---|
| 新 finding（无历史同 ID） | 新增一行 |
| 同 ID 已在历史轮次存在且 `fixed` | 不新增行；在 `ai-check-index.md` §历史轮次 段注释"轮次 X 已 fix，本轮无复发" |
| 同 ID 已在历史轮次存在且 `open` | 沿用原行；在 `## Finding 追踪` 表加备注列"轮次 X 复核：仍 open"（如升级则拆分两行：原行保持 + 新增 `-r2` 行） |
| 同 ID 已 `deferred` 且本轮触发条件满足 | 原行 `状态` 列置 `reopened`；新增 `-r2` 行 |
| 同 ID 已 `deferred` 且本轮仍不满足 | 沿用原行；备注列"轮次 X 复核：触发条件仍未满足" |

**写入 ai-check-index 的纪律**：
- 每次轮次执行收官时，统一批量追加新 finding 行（避免每份 ck-*.md 写一次导致索引膨胀）
- 收官前必须 grep `<执行目录>` 下所有 ck-*.md 汇总 finding 列表 → 一次性追加到 ai-check-index

## 步骤 5 — 与既有 ai-check mission 的衔接

如果项目**正在运行** ai-check mission（`docs/backlog/ai-check-roadmap.md`）：

- 本 skill 产出的 finding 直接进入 ai-check-index，按 ai-check 状态机（`open` → `verifying` → `fixed` / `not-a-problem` / `deferred`）流转
- 不需要新建 mission / mission.json
- 修复走 ai-check roadmap 的 F2.x / F3.x 工作项（不必为新 finding 单独开修复 plan）

如果项目**没有运行** ai-check mission：

- 本 skill 产出的 finding 仍登记到 ai-check-index（这是项目通用索引）
- 但修复需要新建一个 roadmap + mission（如 `docs/backlog/audit-remediation-round-N-roadmap.md` + `missions/audit-remediation-round-N.json`），使用 `audit-remediation-roadmap-authoring-prompt` 设计
- 或纳入下一个 mission 的 backlog（参考项目具体 mission driver 配置）

## 步骤 6 — 自检（产出前强制）

### 三路覆盖自检
- [ ] 第一路：grep 程式全部运行（至少 B1/B2/B3.2 三类；B3.1 仅对含调度的域）
- [ ] 第二路：每条历史 finding 都做「复用 or 新增」裁决（同型已 fix 复用范式；同型 deferred 且触发条件满足立项）
- [ ] 第三路：最近 50 份 plan 的 Deferred / Successor / Non-Goal 段已扫；每个 deferred 项评估触发条件

### Finding 立项自检
- [ ] 每条 finding 有三路证据指针（缺一不可）
- [ ] 没有把"历史已 fix 的同型"登记为新 finding 编号（应复用）
- [ ] 没有把"触发条件未满足的 deferred"登记为新 finding（应入"残余风险"）

### 报告归档自检
- [ ] **报告写入本轮审计结果子目录 `docs/audits/check/<YYYY-MM-DD-HHmm>-<mission-name>/`，不是 `docs/audits/check/ck-*.md` 扁平空间**（多次执行隔离纪律核心）
- [ ] ai-check-index 已同步更新（跨轮聚合）
- [ ] 本轮 `<执行目录>/<mission-name>-index.md` 已建（仅本轮 finding）
- [ ] §5 剩余风险声明已填（含未查区域 + 残余风险 + 证伪项三段）
- [ ] 与既有 ai-check 同域 finding 已对比说明（避免重复计数）

### 反模式自检（沿用 00-roadmap-authoring-guide.md §反模式）
- [ ] 没有把本 skill 当作"重跑 code-quality-audit"——三路证据是关键差异
- [ ] 没有把"修改代码 / 修复"作为本 skill 产物——只产 finding，修复走专门 plan
- [ ] 没有跳过第二路 / 第三路——只跑第一路就是普通 code-quality 审计，三路才是本 skill 的价值
- [ ] **没有把多次执行结果写到同一 ck-*.md 文件**——必须每次新建子目录
- [ ] **没有复用历史执行目录**——已固定的目录是只读的，新执行必须新目录

## 步骤 7 — 返回摘要

返回：
- **本轮执行目录路径**（`docs/audits/check/<YYYY-MM-DD-HHmm>-<mission-name>/`）+ finding 总数（按级别）
- 三路证据统计（仅第一路 / 第一路+第二路 / 第一路+第二路+第三路 各多少）
- 历史 deferred 触发条件已满足项数（这是核心新价值）
- 立项 finding 中"复用历史 fix 范式"的比例（说明三路交叉发现了多少"传染站点"）
- 未覆盖区域（§5 剩余风险声明）
- 与下一轮 roadmap 的衔接建议（哪些 finding 应纳入下个 mission 的修复批次）
- **是否新建了独立执行目录**（自检：避免多次执行覆盖）
```

---

## 产物清单

执行本提示词后，仓库应新增/更新以下文件：

| 产物 | 路径 | 说明 |
|------|------|------|
| 本轮审计结果子目录 | `docs/audits/check/<YYYY-MM-DD-HHmm>-<mission-name>/` | **每次执行隔离**的子目录（多次执行必新建） |
| 审计报告 | `<执行目录>/ck-<slice>.md` | 主产物，含三路证据 + finding 列表 |
| 本轮索引 | `<执行目录>/<mission-name>-index.md` | 仅本轮 finding |
| 跨轮索引更新 | `docs/audits/check/ai-check-index.md` | 跨轮聚合索引（追加新 finding 行） |
| daily log | `docs/logs/{year}/{month}-{day}.md` | 追加本轮审计日志段 |

**不产生的产物**（明确边界）：
- 不产生修复 plan（修复走 `audit-remediation-roadmap-authoring-prompt` 设计的 roadmap + 单独 plan）
- 不产生 roadmap（roadmap 由本 skill 的 §步骤 5 指引，依当前活跃 mission 决定）
- 不修改任何代码、ORM、配置或文档（除 `docs/audits/check/<执行目录>/` 与日志外）

---

## 与既有 skill 的关系速查

| 你要做什么 | 用哪个 skill |
|---|---|
| 拟制"全面审计-修复 roadmap" | `audit-remediation-roadmap-authoring-prompt` |
| 在 roadmap 执行时做单域 / 单切片代码层行为扫描 | `behavioral-failure-mode-scan-prompt` |
| 在 roadmap 执行时做单切片代码质量审查 | `code-quality-audit-prompt` |
| **在 roadmap 执行时做"代码 × 历史 × Deferred"三路交叉审计** | **本 skill（`code-history-deferred-triangulation-audit-prompt`）** |
| 检测缺 closure audit 证据的计划 | `closure-pending-detection-prompt` |
| 单文件级 R*.x compliance 漂移裁决 | `compliance-baseline-drift-adjudication-prompt` |
| 设计文档 vs 实现 drift | `requirement-compliance-audit-prompt` / `design-doc-audit-prompt` |
| 设计层状态机图审查 | `state-machine-business-review-prompt` |

---

## 定制说明

本 skill 已针对 nop-app-erp 项目定制（保护区域授权范围、验证命令、命名约定、已知失败模式、既有 skill 库、报告归档规范、ai-check-index 索引均内嵌）。

若复制到其他项目：
- 替换步骤 0 的前置阅读清单为该项目的 owner docs
- 替换"既有审计记录"路径（按该项目的 `docs/audits/` 与 `docs/lessons/` 命名）
- 替换"历史 plan"路径（按该项目的 plan 目录与命名规范）
- 调整索引路径（如该项目的索引命名为 `*-findings.md` 而非 `ai-check-index.md`）
- 若该项目已有类似 mission（如 audit-remediation），§步骤 5 调整为指向该项目 mission
- 若该项目**没有** `behavioral-failure-mode-scan-prompt` 同型 skill，第一路取证退化为 `code-quality-audit-prompt` 7 个重点领域
- **本 skill 的"每次执行新建日期时间前缀子目录"纪律是通用要求**——不同项目都应遵守，避免多轮执行冲突

若本项目后续发现新的高频"历史已识别但遗漏传染"模式，应将其补充到步骤 1 第一路"扩展维度"，保持三路取证的覆盖率。
