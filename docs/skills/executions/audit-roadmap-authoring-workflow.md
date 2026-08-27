# 审计类 Roadmap 拟制与执行工作流

> **用途**：使用 `code-history-deferred-triangulation-audit-prompt` + `audit-remediation-roadmap-authoring-prompt` 这两个 skill 拟制审计类 roadmap（"代码 × 历史 × Deferred"三路交叉审计 + 修复闭环）的**标准执行流**。
>
> **适用场景**：
> - 项目已积累多轮审计（≥ 20 份 `docs/audits/*.md`），需要开新一轮审计-修复 roadmap
> - 现有 mission（如 `audit-remediation`、`ai-check`）已收口，需要启动第二轮（ai-check-r2、audit-remediation-r2 等）
> - 怀疑历史识别的某类问题在另一域 / 另一模块以同型出现
> - 怀疑历史 deferred 的某条触发条件今天已满足但未被识别
>
> **不适用场景**：单对象窄审计（直接用 `code-quality-audit-prompt` / `state-machine-business-review-prompt` 等专项 skill）；项目刚起步、无历史审计记录（直接用通用 code-quality 扫描）。

---

## 一、工作流全景

```
┌─────────────────────────────────────────────────────────────────────┐
│  阶段 1 — 需求识别与决策（人工 / 用户主导，AI 协助）                  │
│  ├── 1.1 识别触发条件：哪些信号提示需要新一轮审计？                   │
│  ├── 1.2 决定 mission 模式：复用现有 mission 还是新建？              │
│  ├── 1.3 决定产物路径：roadmap / mission.json 命名空间                │
│  └── 1.4 决定本轮审计结果目录（多次执行隔离纪律——必做）                │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│  阶段 2 — Roadmap 拟制（用 audit-remediation-roadmap-authoring）    │
│  ├── 2.1 前置阅读（AGENTS.md / context / 现有 roadmap / 历史审计）  │
│  ├── 2.2 步骤 1：建立审计维度矩阵（含三路交叉来源 B/C）              │
│  ├── 2.3 步骤 2：汇聚已有审计的未闭包发现                            │
│  ├── 2.4 步骤 3-5：里程碑结构 + 工作项粒度 + 优先级                  │
│  ├── 2.5 步骤 6：roadmap 文件 + 报告归档规范                          │
│  ├── 2.6 步骤 7：mission.json                                       │
│  └── 2.7 步骤 8：自检清单 + 步骤 9：返回摘要                        │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│  阶段 3 — 三路交叉审计执行（用 code-history-deferred-triangulation）│
│  ├── 3.1 按里程碑切片，每切片一份 plan                                │
│  ├── 3.2 计划 EXECUTE：跑第一路（grep 程式）+ 第二路（复用裁决）     │
│  │             + 第三路（deferred 触发条件扫描）                     │
│  ├── 3.3 产出 `<执行目录>/ck-<slice>.md` + 本轮索引同步              │
│  └── 3.4 提交独立子代理 closure audit（验收三路证据完整性）          │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│  阶段 4 — 修复闭环（用 audit-remediation-roadmap-authoring §MR）    │
│  ├── 4.1 按 finding 级别分流：P0 即时通道 / P1 批量 / P2-P3 deferred │
│  ├── 4.2 P0 立即修复或异步注入 plan（不进入批量修复里程碑）           │
│  ├── 4.3 P1 修复 plan：先写失败测试 → 修复 → 复跑全量测试             │
│  ├── 4.4 修复完成回填本轮 ck-* 与 ai-check-index 状态                │
│  └── 4.5 修复触及保护区域：走 auto + dual-agent-approval 守门       │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│  阶段 5 — 全量验证与收口                                              │
│  ├── 5.1 V.1：mvn clean install -DskipTests + 全量 mvn test 全绿   │
│  ├── 5.2 V.2：compliance checker 不得高于 M0 快照（合法新增走       │
│  │         baseline-raise）                                          │
│  ├── 5.3 索引终态校验：所有 finding 到达 `fixed` / `not-a-problem`  │
│  │             / `deferred`（deferred 计数与理由显式报告）          │
│  └── 5.4 知识沉淀：新失败模式入 docs/lessons/，新 skill 入          │
│                docs/skills/，基线行入 known-good-baselines.md         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 二、阶段 1 详细：需求识别与决策

### 1.1 触发新一轮审计的信号

以下任一信号出现时，应启动新一轮审计-修复 roadmap：

| 信号 | 检测方法 | 典型阈值 |
|---|---|---|
| 现有 mission 已收口 | 检查 `docs/backlog/*-roadmap.md` 末尾状态块 | 上一轮 V.2 done ≥ 1 周 |
| 历史审计遗留 finding 数过多 | 检查 `docs/audits/*-index.md` | open finding 数 ≥ 200 |
| 业务代码显著扩张 | 检查 `find module-* -name "*.java" -not -path "*/_gen/*"` 差量 | 较上一轮增长 ≥ 20% |
| 新增域 / 新增重大特性 | 检查 git log 大提交 | 任意新增 module-* 域 |
| 历史 plan 中 deferred 项触发条件疑似满足 | 抽样 20 份最近 plan 的 Deferred 段 | 任一抽样项的触发条件已可见成立 |
| 集成测试 / E2E / 单元测试中发现 product defect | 检查 `docs/bings/` + 最近 plan | 任一 P0 product defect |

### 1.2 mission 模式选择

| 模式 | 何时选 | 命名空间 |
|---|---|---|
| **复用现有 mission**（推荐） | 现有 mission（如 `ai-check`）已建立索引与状态机，finding 流转成熟 | 直接追加工作项到现有 roadmap |
| **新建 mission** | 现有 mission 闭合 / 命名不符 / 工作流有质变 | 新建 `docs/backlog/<name>-roadmap.md` + `missions/<name>.json` |

**决策规则**：
- 同一审计方法（行为维度）的迭代 → 复用 mission
- 跨维度（如从代码行为切到 ORM 设计） → 新建 mission
- mission 闭合后第二轮开新 → 一般新建（使命边界清晰）

### 1.3 产物路径命名

| 产物 | 命名规范 | 示例 |
|---|---|---|
| Roadmap | `docs/backlog/<mission>-r<N>-roadmap.md` 或 `docs/backlog/<mission>-roadmap.md`（首轮） | `docs/backlog/ai-check-r2-roadmap.md` / `docs/backlog/audit-remediation-roadmap.md` |
| Mission 配置 | `missions/<mission>.json` 或 `missions/<mission>-r<N>.json` | `missions/ai-check-r2.json` |
| 审计报告 | **本轮独立子目录** + `ck-<slice>.md`（见 §1.4） | `docs/audits/check/2026-08-28-0930-ai-check-r2/ck-finance-posting.md` |
| 本轮索引 | **本轮独立子目录** + `<mission-name>-index.md` | `docs/audits/check/2026-08-28-0930-ai-check-r2/ai-check-r2-index.md` |
| 跨轮聚合索引 | `docs/audits/check/ai-check-index.md` | （**不**放本轮 ck-* 报告，避免多次执行冲突） |
| 修复 plan | `docs/plans/YYYY-MM-DD-HHmm-<mission>-fix-*.md` | `docs/plans/2026-08-28-0950-ai-check-r2-fix-P1-CK-mfg-007.md` |

### 1.4 本轮审计结果目录决策（**多次执行隔离纪律**——必做）

**为什么必须每次新建子目录？**

- 多次执行同一 mission（ai-check-r2 重新启动、ai-check-r3 启动）→ 不同时间的 finding 不可写到同一 `docs/audits/check/ck-*.md` 扁平空间（finding ID 冲突、索引覆盖、报告混入）
- 不同 mission 可并行（audit-remediation-r3 + ai-check-r2 同月运行）→ 用各自 mission 名作子目录前缀
- 历史目录是只读证据——已 fixed 的目录不应被后续执行覆盖

**目录命名规范**：

```
docs/audits/check/<YYYY-MM-DD-HHmm>-<mission-name>/ck-<slice>.md
docs/audits/check/<YYYY-MM-DD-HHmm>-<mission-name>/<mission-name>-index.md
```

**示例**：

| 场景 | 子目录 |
|---|---|
| 2026-08-28 09:30 启动 ai-check-r2 | `docs/audits/check/2026-08-28-0930-ai-check-r2/` |
| 2026-09-15 14:00 启动 ai-check-r3 | `docs/audits/check/2026-09-15-1400-ai-check-r3/`（**不**覆盖 08-28） |
| 2026-09-20 10:00 启动 audit-remediation-r3 | `docs/audits/check/2026-09-20-1000-audit-remediation-r3/`（与 ai-check 并行） |
| 同 mission 同分钟内并发启动 | 时间戳补齐秒级：`0930` → `093001` / `093045`（避免命名冲突） |

**纪要要求**：

| 要素 | 规则 | 违反后果 |
|---|---|---|
| 目录名格式 | `YYYY-MM-DD-HHmm-<mission-name>` 严格 ISO + 小写连字符 | 排序混乱 |
| 时间戳精度 | 至少分钟级（HHmm）。同 mission 同分钟内并发启动用秒级补齐 | 命名冲突 |
| 严禁覆盖 | 历史目录只读；新执行必新目录 | 历史 finding 丢失 |
| 索引并行 | 每轮独立 `<mission-name>-index.md`（**不**与历史轮次共用） | 跨轮索引污染 |
| 跨 mission 命名 | 不同 mission 用各自前缀（如 `ai-check-r2/` vs `audit-remediation-r3/`） | mission 边界混淆 |

**跨轮聚合索引 `docs/audits/check/ai-check-index.md` 的角色**：

- 它**不是**某个单轮的执行目录——它是**跨轮 finding 聚合索引**（finding ID 全局连续 + 状态机跨轮继承）
- 每轮收官时**批量**追加新 finding 行（避免每份 ck-*.md 写一次导致索引膨胀）
- finding ID 跨轮冲突处理规则见 `code-history-deferred-triangulation-audit-prompt.md §3.1`

---

## 三、阶段 2 详细：Roadmap 拟制

**主用 skill**：`docs/skills/audit-remediation-roadmap-authoring-prompt.md`

**输入**：
- 项目上下文（AGENTS.md / project-context.md / ai-autonomy-policy.md / codebase-map.md）
- 现有所有 mission 的 `*-roadmap.md`（避免重复架构）
- `docs/audits/` 与 `docs/lessons/` 全部历史
- `docs/skills/` 全部 skill（避免新建重复）

**关键步骤摘要**（详见 skill 主体提示词）：

1. **步骤 0 强制前置阅读**：按 skill 步骤 0 完整读完 6 类文档，未读完不进入步骤 1
2. **步骤 1 建立审计维度矩阵**：这是 roadmap 设计的核心
   - 来源 A = 已有 skill 库覆盖的维度（**复用 skill 本身，不要重复新建提示**）
   - 来源 B = 历史残留风险与已知盲区（**特别包括「历史 deferred 触发条件扫描」维度——这是 `code-history-deferred-triangulation-audit-prompt` 的覆盖范围**）
   - 来源 C = ERP 特定风险维度（保护区域 / 多账套 / 期间状态机 / 冲销反写闭环）
3. **步骤 2 汇聚未闭包发现**：遍历 `docs/audits/` + `docs/audits/*-index.md` + `docs/audits/check/ai-check-index.md`（如有），对每条 finding 提取状态 + 触发条件
4. **步骤 3-5 里程碑结构 + 粒度 + 优先级**：遵循 `00-roadmap-authoring-guide.md` 八段结构
5. **步骤 6 roadmap 文件 + 报告归档规范**：
   - 审计报告路径改为 `<执行目录>/ck-<slice>.md`（**不再**写扁平 `docs/audits/check/ck-*.md`）
   - mission.json 必须包含 `executionDirPrefix` / `reportDirTemplate` 字段
6. **步骤 7 mission.json**：commands 包含 `test` / `build` / `compliance`；新增 `reportDir` / `indexPath` / `reportNaming` 字段
7. **步骤 8 自检 + 步骤 9 返回摘要**

**输出**：
- `docs/backlog/<mission>-r<N>-roadmap.md`
- `missions/<mission>-r<N>.json`

---

## 四、阶段 3 详细：三路交叉审计执行

**主用 skill**：`docs/skills/code-history-deferred-triangulation-audit-prompt.md`

**输入**：
- 阶段 2 产出的 roadmap 的审计工作项（MA / B / C 工作项）
- 上一轮的 finding 索引（`ai-check-index.md` 或类似）
- 阶段 2 §1.4 决定的本轮审计结果子目录路径（**必新建**）

**执行步骤**（详见 skill 主体提示词）：

1. **按切片建 plan**：每个 audit 工作项 = 一份 plan
2. **plan EXECUTE 第一步**（**必做且写进 plan**）：创建本轮审计结果子目录
   ```bash
   mkdir -p docs/audits/check/$(date +%Y-%m-%d-%H%M)-<mission-name>
   # 或按 mission.json 的 executionDirPrefix 生成
   ```
3. **plan EXECUTE 三阶段**：
   - **第一路（当前代码）**：调用 `behavioral-failure-mode-scan-prompt` 的 grep 程式 + `code-quality-audit-prompt` 的 7 重点领域
   - **第二路（历史审计复用裁决）**：读 `docs/audits/` 与 `docs/lessons/`，对每个候选 finding 做「复用 or 新增」裁决
   - **第三路（deferred 触发条件扫描）**：读最近 50 份 plan 的 Deferred / Successor / Non-Goal 段，评估触发条件
4. **产出报告**：
   - `<执行目录>/ck-<slice>.md`（5 段结构：元信息 / 三路证据汇总 / finding 列表 / 统计 / 剩余风险声明）
   - `<执行目录>/<mission-name>-index.md`（本轮 finding 聚合）
   - 同步追加到 `docs/audits/check/ai-check-index.md`（跨轮聚合）
5. **提交独立 closure audit**：由独立子代理（fresh session）跑 `closure-audit-prompt`，验收：
   - 三路证据是否齐全
   - finding 编号是否与既有索引连续（跨轮冲突按 §3.1 处理）
   - 剩余风险声明是否完整
   - 与既有 ai-check 同域 finding 的对比是否清晰
   - **本轮子目录路径**是否正确（隔离区验证）

**修复不动**：本 skill 只产 finding，**严禁在 plan 内顺手修改代码**。修复走阶段 4。

---

## 五、阶段 4 详细：修复闭环

**主用 skill**：`audit-remediation-roadmap-authoring-prompt §步骤 5 P0 即时通道 + §MR 修复阶段`

**输入**：
- 阶段 3 产出的 finding 列表（已分类 P0/P1/P2/P3）
- 阶段 2 roadmap 的 MR 修复里程碑

**修复级别分流**：

| 级别 | 修复通道 | plan 类型 |
|---|---|---|
| **P0** | 即时通道：plan 内就地修复 或 异步注入 plan（`docs/plans/YYYY-MM-DD-HHmm-<mission>-fix-*.md`） | 单 finding 单 plan |
| **P1** | MR 批量：按域 / 按文件簇批量修复 | 一批 finding 一份 plan |
| **P2** | 登记为 deferred successor，写明触发条件 | 不开 plan |
| **P3** | 登记为 note，不开 plan | 不开 plan |

**修复方法论**（每条 finding 强制流程）：

1. 读 finding 控制点（file#method + 摘录 + 三路证据）
2. **先写失败测试**（复现缺陷；JUnit 或集成测试）
3. 复现 → 修复 → 测试绿 + 既有测试零回归
4. 证伪路径：书面 not-a-problem 说明（理由 + 证据）
5. 回填索引状态：`fixed`（附测试 + 提交指针）/ `not-a-problem`（附说明）/ `deferred`（附触发条件）

**保护区域守门**：

| 触发情形 | 守门 |
|---|---|
| 修改 `module-<domain>/model/*.orm.xml` | auto + dual-agent-approval（两个独立子 agent 分别检查批准） |
| 修改 `module-<domain>/model/*.api.xml` | 同上 |
| 修改会计过账 / 数据删除 / auth | plan-first + owner doc + tests + 独立 plan audit |
| 修改生成产物（`_gen/` / `_` 前缀） | 禁止，改模型源而非生成代码 |

**修复完成回填纪律**：

- 修复完成后必须更新**两个索引**：
  - 本轮 `<执行目录>/<mission-name>-index.md`（修改本轮 finding 状态）
  - `docs/audits/check/ai-check-index.md`（跨轮聚合索引同步状态）
- 同型 finding 状态继承：修复一处，全族回填（如 F1.3 AbstractErpCrudBizModel 修复后，pur/sal/inv/mfg/mfg3 五个同型 finding 全部转 fixed）

---

## 六、阶段 5 详细：全量验证与收口

**主用 skill**：`closure-audit-prompt`（独立子代理）+ `compliance-baseline-drift-adjudication-prompt`（如有 baseline 漂移）

**输入**：阶段 4 全部修复完成的 finding + 已验证的测试基线

**执行清单**：

1. **V.1 全量验证**：
   - `mvn clean install -DskipTests` BUILD SUCCESS
   - `mvn test` 全 reactor 零新增失败
   - `bash docs/audits/nop-compliance-checker.sh` 不得高于 M0 快照
   - 抽样 E2E 回归（如有活跃 E2E mission）
2. **V.2 索引终态校验**：
   - 全部 finding 到达终态（`fixed` / `not-a-problem` / `deferred`）
   - `deferred` 计数与逐条理由在收官报告中显式列出（防例外通道稀释「P0-P3 都要修复」指令）
   - 跨轮索引 `ai-check-index.md` 状态机连续性校验
3. **MG 收尾**：
   - 新失败模式入 `docs/lessons/`（如 `16-code-history-deferred-miss.md`）
   - 新 skill 入 `docs/skills/`（如发现新的高频失败模式可提升为 skill）
   - 基线行入 `docs/testing/known-good-baselines.md`
   - 更新 `docs/context/project-context.md` 当前阶段
   - 更新 `docs/skills/README.md` 已知失败模式清单
4. **本轮子目录归档纪律**：
   - 历史轮次的 `<执行目录>/` 永久只读保留（作为审计证据）
   - 不删除历史目录（即使 finding 全 fixed 也不删——避免审计证据丢失）
   - 后续 mission 启动新轮次时**新建**子目录，**不**复用

---

## 七、与 Mission Driver 的衔接

如果项目使用 mission driver（参考 `missions/*.json` + `tools/mission-driver.sh`）：

```bash
# 1. 校验 mission 配置
node $MISSION_DRIVER_HOME/src/mission-check.mjs missions/<mission>-r<N>.json .

# 2. Dry-run 验证流程编排（含子目录创建）
./tools/mission-driver.sh run <mission>-r<N> --dry-run --no-monitor

# 3. 正式运行（Mission Driver 会按 M0 → MA1-... → MV → MG 顺序推进，
#    每工作项创建 plan 时自动使用 executionDirPrefix）
./tools/mission-driver.sh run <mission>-r<N>

# 4. 监控
open http://localhost:9300
```

Mission Driver 会按 M0 → MA1-... → MV → MG 顺序推进；每个工作项生成 plan → 独立草案审查 → 执行 → 结束审计。

**mission.json 必填字段（多次执行隔离）**：

```json
{
  "executionDirPrefix": "docs/audits/check/",
  "reportDirTemplate": "<executionDirPrefix><YYYY-MM-DD-HHmm>-<name>/",
  "reportNaming": "ck-<slice>.md",
  "indexPath": "docs/audits/check/ai-check-index.md",
  "indexTemplate": "<reportDirTemplate><mission-name>-index.md"
}
```

如果项目**不使用** mission driver（如 ai-check 第一轮的 user-driven 模式）：roadmap 仅作为状态块 + 工作项拆分参考，每个工作项由人工或 AI 单独起草 plan，独立 closure audit 由独立子代理人工发起。**但子目录创建 + 报告路径规则仍然适用**。

---

## 八、典型工作流示例

### 示例 A：ai-check 第一轮已收口，启动第二轮

1. **阶段 1 决策**：
   - 触发信号：上一轮 V.2 done + 488 open finding 待修
   - mission 模式：复用 `ai-check`（索引与状态机已成熟）
   - 产物路径：`docs/backlog/ai-check-r2-roadmap.md` + `missions/ai-check-r2.json`
   - **本轮审计结果子目录**：`docs/audits/check/2026-08-28-0930-ai-check-r2/`
2. **阶段 2**：
   - 读 `audit-remediation-roadmap-authoring-prompt`（架构师视角）
   - 维度矩阵覆盖：本轮重点 = 「代码 × 历史 × Deferred」三路交叉（特别是第三路 deferred 扫描）
3. **阶段 3**：
   - 工作项按域切片：finance-r2 / mfg-r2 / assets-r2 / ...
   - 每个 plan EXECUTE 第一步：创建 `2026-08-28-0930-ai-check-r2/` 子目录
   - 产出 `ck-finance-r2.md` / `ck-mfg-r2.md` / ... + 本轮 `ai-check-r2-index.md` + 同步 `ai-check-index.md`
4. **阶段 4**：
   - P0 即时通道（如有）/ P1 批量修复（按域）
   - 状态回填两个索引
5. **阶段 5**：
   - V.1 / V.2 / MG
6. **收口**：新发现的可复用模式入 `docs/lessons/16-*.md`

### 示例 B：arm mission 已收口，启动 audit-remediation 第二轮

1. **阶段 1**：
   - 触发信号：arm V.2 done + 未闭包 finding ≥ 100
   - mission 模式：新建 `audit-remediation-r2`（架构质变）
   - 产物路径：`docs/backlog/audit-remediation-r2-roadmap.md` + `missions/audit-remediation-r2.json`
   - **本轮审计结果子目录**：`docs/audits/check/2026-09-20-1000-audit-remediation-r3/`（同 mission 第二轮用 `-r2` 后缀）
2. **阶段 2-5**：按 A 同

### 示例 C：单域小切片补强（不新增 mission）

1. **阶段 1**：触发信号 = 单一 product defect 报告 → 跳到阶段 3
2. **阶段 3**：
   - **仍然必须**创建子目录：`docs/audits/check/2026-10-05-1100-ai-check-r3/`
   - 仅一个 plan = `ck-finance-posting-r3.md`（按本 skill 三路取证）
3. **阶段 4**：仅 1-3 条 finding，直接走 P0 即时通道或单 P1 plan
4. **阶段 5**：跳过 V.1 全量（仅 ck-* 自身 closure audit）

### 示例 D：多次执行同一 mission（关键纪律示例）

| 时间 | 触发 | 子目录 | 与上次关系 |
|---|---|---|---|
| 2026-08-28 09:30 | ai-check-r2 第一轮 | `2026-08-28-0930-ai-check-r2/` | 首轮 |
| 2026-09-15 14:00 | ai-check 第二轮新增 finding | `2026-09-15-1400-ai-check-r3/` | **不**复用 08-28 目录；08-28 目录保持只读 |
| 2026-10-05 11:00 | ai-check 第三轮补充 | `2026-10-05-1100-ai-check-r4/` | **不**复用 09-15；累积三个目录只读保留 |

**ai-check-index.md 跨轮聚合规则**：
- 第一轮 finding 全部登记（533 项）
- 第二轮新增 finding 追加（按 §3.1 ID 冲突处理）
- 第三轮新增 finding 追加
- `ai-check-index.md` 永远是**单一跨轮索引**，不按轮次拆分

---

## 九、常见反模式（必须避免）

| 反模式 | 后果 | 正确做法 |
|---|---|---|
| 跳过第二路 / 第三路，只跑第一路 | 等同 code-quality 审计，丧失三路交叉价值 | 严格执行本 skill §步骤 1 |
| 在审计 plan 内顺手修改代码 | 违反"两阶段时序硬约束"——审计抢跑修复 | 审计 plan 仅产 finding；修复走专门 plan |
| 把"历史已 fix 的同型"登记为新 finding 编号 | 索引膨胀 + finding ID 浪费 | 复用历史编号 + 在新 finding 标注「复用 R*.x 范式」 |
| 把"触发条件未满足的 deferred"登记为新 finding | 未触发项污染 finding 列表 | 入 §5 剩余风险声明，不算 finding |
| 阶段 2 不读历史就设计维度矩阵 | 矩阵漏覆盖历史已知盲区 | 步骤 0 强制前置阅读全部 `docs/audits/` + `docs/lessons/` |
| 阶段 4 修复不写失败测试 | 修复可能修了症状而非根因 | 每 finding 强制「先写失败测试 → 修复 → 既有测试零回归」 |
| 阶段 5 跳过 compliance baseline 检查 | baseline 漂移逃逸 CI | V.1 必跑 `nop-compliance-checker.sh` |
| 新建 skill 不挂 README 路由 | skill 退化为氛围编码 | §阶段 2 后同步 `docs/skills/README.md` 技能注册表 |
| **多次执行写到同一 ck-*.md 文件** | finding ID 冲突 / 历史覆盖 / 索引膨胀 | | **每次必创建新子目录** |
| **复用历史执行目录** | 历史审计证据丢失 | 已固定目录只读；新执行必新目录 |
| **mission.json 缺少 executionDirPrefix 字段** | mission driver 不知道每次写到新目录 | §七 必填字段落地 |
| 拟制新 roadmap 不读旧 roadmap | 重复造轮子 | §阶段 2 步骤 0 必读所有现有 `*-roadmap.md` |

---

## 十、关联文档

- **skill 主入口**：`docs/skills/audit-remediation-roadmap-authoring-prompt.md`（roadmap 设计师）
- **skill 主入口**：`docs/skills/code-history-deferred-triangulation-audit-prompt.md`（三路交叉审计员）
- **skill 关联**：`docs/skills/behavioral-failure-mode-scan-prompt.md`（第一路取证）
- **skill 关联**：`docs/skills/code-quality-audit-prompt.md`（第一路通用框架）
- **方法论**：`docs/audits/00-audit-execution-guide.md`（三个默认审计 + 持久审查证据规则）
- **roadmap 规范**：`docs/backlog/00-roadmap-authoring-guide.md`（里程碑 + 工作项状态表面）
- **plan 规范**：`docs/plans/00-plan-authoring-and-execution-guide.md`（plan 格式 + 关闭契约）
- **项目定制层**：`docs/skills/README.md §项目定制化层（nop-app-erp）`（保护区域 / 验证命令 / 命名约定 / 已知失败模式）
- **真实案例**：`docs/audits/check/ai-check-index.md`（533 finding / 19 域 / 三路交叉产物范例）
- **真实案例**：`docs/backlog/ai-check-roadmap.md`（roadmap + mission + 阶段拆分范例）

---

## 十一、定制说明

本工作流已针对 nop-app-erp 项目定制（mission driver + 保护区域守门 + ai-check-index 索引 + 三 skill 组合 + 多次执行隔离纪律）。

若复制到其他项目：
- 替换 mission driver 配置（`missions/*.json` + `tools/mission-driver.sh` 路径）
- 替换索引路径（如该项目的 finding 索引命名为 `*-findings.md`）
- 替换保护区域守门（按 `docs/context/ai-autonomy-policy.md`）
- 若该项目**没有** `behavioral-failure-mode-scan-prompt` 同型 skill，第一路退化为 `code-quality-audit-prompt`
- 若该项目无 mission driver，roadmap 仅作状态块，工作项由人工 / AI 单独起草 plan
- **多次执行隔离纪律（§1.4 + §四 + §六 + §八示例 D）是通用要求**——不同项目都应遵守

若本项目后续发现新的高频三路交叉失败模式（如「历史 deferred 触发条件评估方法有遗漏」），应将其补充到 `code-history-deferred-triangulation-audit-prompt §步骤 1 第三路扫描判定规则`，保持 skill 的覆盖率。
