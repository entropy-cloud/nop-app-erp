# nop-app-erp 开发历程 PPT 大纲 v3（最终版）

> 用途：逐页 PPT 结构和内容指导。基于 v3 修订版 + 3 路独立子代理审查反馈（AGE 概念准确性/叙事与观众体验/技术事实准确性）反复迭代至三方共识。
> 素材来源：`base-material.md` + `docs/logs/` + `docs/plans/` + `~/app/attractor-guided-engineering-template/`（AGE 模板 + 两篇深度 AGE 文章） + `~/app/nop-chaos-flux-wt/.../docs/articles/flux-design-introduction.md`。

> **核心叙事**：AGE 不是一个工具，不是一个模板，是一个 dynamical systems 方法论——`state space → attractor → trajectory → control`。nop-app-erp 的实践将其落地为三个嵌套的控制 Loop：Plan Loop（关闭单次变更）、Mission Driver Loop（编排整个路线图）、Attractor Evolution Cycle（演进吸引子本身）。"智能"不在 AI 的代码生成能力里，在这三层 Loop 的嵌套结构里。
>
> **审查共识摘要**（3 路独立子代理审计后）：
> - ✅ 核心命题（三层嵌套控制 Loop）通过 AGE 概念准确性审查
> - ❌ "Attractor Loop" 用词不准确——attractor 是**结构**不是 Loop，改为 **Attractor Evolution Cycle**
> - ❌ ORM XML 不是吸引子本身，是**承载者**——需要区分结构层/承载层/实现层
> - ✅ Mission Driver 流程描述与 `mission-driver.json` 一致
> - ❌ Plan Loop 流程图把验证作为独立状态步骤不准确——验证是执行阶段的最后子步骤
> - ❌ S06 只说了 1 个拦截问题，实际该批次拦截了 4 个 P0 缺陷——需要修正
> - ✅ S07 fresh session 审计原则符合 AGE "生成与接受分离"核心原则
> - ✅ 三层 Loop 框架是 nop-app-erp 的实践映射，不是 AGE 论文原文——需标注出处
>
> **叙事审查要点**（综合采纳）：
> - ✅ S02 去掉 dynamical systems 理论，用具体痛点（竞争杠杆审计误判故事）开场
> - ✅ 增加高潮页：S18 作为三层汇聚的"反转时刻"
> - ✅ S26 重新设计为"行动挑战"而非"感谢回扣"
> - ✅ Act 3 扩展：4→5 页、7→9 min
> - ✅ Act 4 重构为"接力故事"而非数据陈列
> - ✅ 加入人的故事：用户的关键决定时刻
>
> **总页数**：34 页正文，55~58 min + Q&A。

---

## Act 0：开场——问题的重新定义（3 页 / 5 min）

### S01 标题页（1 min）
- **标题**：Loop 的嵌套——nop-app-erp × Attractor-Guided Engineering 实践
- **副标题**：22 天 · 18+1 业务域 · 154 模块 · 3 层嵌套控制 Loop
- **埋钩子（口播）**："今天我要展示的不是 AI 写了多少代码，而是一个三层嵌套的控制系统——Plan Loop、Mission Driver Loop、Attractor Evolution Cycle。它们嵌套在一起，让代码从路线图自动流出。"
- **视觉**：三个嵌套的圆环（最内 = Plan，中层 = Mission Driver，外层 = Attractor Evolution Cycle）。底部标注 "智能不在代码里，在 Loop 的结构里。"

### S02 一个让你不安的问题（2 min）
- **开场不抛理论，抛一个真实故事**：
  - 07-12，项目进行了竞争杠杆审计——检查"承诺的功能/优势是否已完整兑现"
  - 子代理审计结论："零 Delta 使用"——意味着从未利用过平台机制的可扩展性
  - 但主代理用 `grep` 验证发现：**338 个 view.xml 文件使用了 Delta 机制 `x:extends`**
  - 子代理还报告："finance↔inventory 循环依赖"——但 reactor 构建无环
  - **子代理不是在撒谎**。它在同一个上下文里生成了代码、测试、文档和检查报告——天然有一致的确认偏差。
- **这是 AGE 要解决的核心问题**：
  - 不是"AI 能不能写代码"——AI 写代码已经太容易了
  - 而是**做完 100 个变更后，系统还在正确的结构上吗？**
  - 每一个变更单独看都没问题——测试过了、合并了、review 了
  - 但 100 个后，架构在漂移、测试与实现耦合、owner docs 与代码不一致
  - 传统"状态检查"（state-level QA）做不到轨迹检查——这是 AGE 的切入点
- **过渡到 S03**："22 天后，我们证明了三层 Loop 可以解决这个问题。先看结果，再讲方法。"
- **视觉**：左右对比——左为"状态检查"（PR pass / CI pass / merge → 100 个后架构漂移 ❌），右为"轨迹检查"（定义方向 → 执行 → 检查偏离 → 纠正 → 再执行 → 收敛 ✅）。右下方标注子代理误判案例的 grep 截图。

### S03 22 天的输出——系统出产物，不是奇迹（2 min）
- 关键数字面板：18+1 域 / 154 模块 / ~2900 测试 / 260+ E2E / 0 regression / 189 计划 / 9 审计
- 强调：这些数字不是"22 天的奇迹"——是三层控制 Loop 的系统输出产物
- 预告完整路线图：从最内层 Plan Loop 开始 → 中层 Mission Driver → 外层 Attractor Evolution Cycle → 证据 → 带走
- **视觉**：仪表盘布局 + 底部路线图小字标注 6 段页数范围。三个嵌套圆环标注在右下角（与 S01 相同，建立视觉锚点）。

---

## Act 1：最内层控制 Loop——Plan（6 页 / 11 min）

### S04 没有 Plan Loop 会怎样（1.5 min）
- 传统 AI 开发模式：用户说→AI 写→用户改→AI 再写→循环直到"差不多了"
- 问题：没有基线、没有范围边界、没有退出标准、没有审计——**一个不可关闭的循环**
- Plan Loop 解决：给每次变更一个正式关闭契约
- **过渡**："来看这个契约长什么样。"
- **视觉**：无限循环箭头（用户说→AI 写→用户改→...），标注"∞ 无退出条件"。右侧打 ❌。

### S05 Plan 作为关闭契约（2 min）
- Plan 不是任务清单，是**关闭契约**：`Current Baseline`（从仓库读取，不依赖记忆）+ `Goals` + `Non-Goals`（明确不做啥）+ `Exit Criteria`（可观察的关闭条件）+ `Closure Gates`（最终检查清单）+ `Draft Review Record`（独立审查迭代）
- **Plan Loop 的闭环**：
  ```
  draft（起草）
    → 独立草案审查（fresh session，方向正确？可 iter 多轮）
      → active（可执行）
        → 执行各 Phase（改代码、加测试、验证）
          → 独立结束审计（fresh session，交付完整？）
            → completed
  ```
  - 注：验证（`mvn test`）是执行阶段的最后子步骤，不是独立的状态步骤
- 这不是多此一举——是把"一次变更"从不可控对话变成有始有终的 Loop
- **视觉**：Plan Loop 流程图（5 步，从 draft 到 completed）。验证标注在执行步骤内部。

### S06 实例：Plan 1100-1 销售定价引擎的完整 Loop（2.5 min）
- **完整实录**（07-10）：
  ```
  起草（draft）→ 独立审查 iter1（"客户组维度 Decision 缺失"，needs revision）
    → 修订 → 独立审查 iter2（accept）
      → active → Phase 1-4（ORM 变更 + SPI 引擎 + 测试 + 前端）
        → mvn clean install + mvn test 全绿
          → 独立结束审计 PASS → completed
  ```
- **补充：该批次 4 个 Plan 的独立审查共拦截 4 个 P0 缺陷**——码值冲突、BUDGET 污染实际财务、GlBalance 前提错误、维度歧义。**全部在编码前拦截。**
- **关键数字**：draft → completed 全程约 2 天。如果没 Plan Loop：这 4 个缺陷会在实施/运行期才暴露，修复成本高出 5-10 倍。
- **视觉**：横向时间线，标注 Plan 的每个状态变化 + 关键事件（审查拦截 4 个 P0 → 红色标注）。

### S07 为什么独立审查必须 fresh session（2 min）
- **AGE 核心原则**：不要让同一个上下文既执行又判断完成
  - AI 写完了代码 → 同一个 AI 说"做完了" → 天然确认偏差
  - 必须由独立子代理（fresh session）在不继承执行上下文的情况下重新审查
- **nop-app-erp 的真实教训**（07-12 竞争杠杆审计）：
  - 子代理在审计上下文中声称"零 Delta 使用"——但 grep 发现 338 个 `x:extends`
  - 子代理声称"循环依赖"——但 `mvn clean install` 无环
  - 如果审查继承了执行上下文，这些误判会直接进入报告
- **Plan Loop 的核心设计**：草案审查 + 结束审计，都是独立子代理 fresh session 冷启动
- **视觉**：两个并列 AI 图标——左边"同一上下文（写代码 + 自己检查 = ❌）"，右边"fresh session 独立检查（= ✅）"。底部展示 grep 截图。

### S08 小 Loop 的效果：189 份 Plan 的可追溯性（1.5 min）
- 189 份 Plan = 189 次从起草到关闭的完整 Loop。每份可追溯：
  - 为什么做 → `Source` 指向路线图工作项
  - 做之前什么状态 → `Current Baseline`
  - 做了什么决策 → `Decision` 类型项目（含备选方案 + 被否原因）
  - 做完了吗 → `Closure Gates` + 独立结束审计记录
- 这不是文档完备性——是**轨迹信息的外部化**。关键决策在文件里，不在临时对话里。下次 AI 会话可以重新加载。
- **一句话过渡 Act 2**："Plan Loop 解决了'一次变更怎么关闭'。但项目有 189 次变更——谁来编排它们的顺序？"
- **视觉**：时间线散点图（189 个点，X=日期）。右下角标注"下一个做什么？Mission Driver 知道。"

---

## Act 2：中间层控制 Loop——Mission Driver（6 页 / 12 min）

### S09 为什么需要更大一层的 Loop（1.5 min）
- Plan Loop 只管理"一次变更"，但项目有几十个工作项：
  - 谁来决定下一个做什么？谁确保路线图上所有工作项都被执行？
  - 谁在空闲时触发审计？谁把 Plan 完成状态同步回路线图？
- 全凭人做→手动、不可重复、依赖注意力
- **Mission Driver 就是这更大一层的 Loop**：在 Plan Loop 之外加一个编排层，自动遍历路线图
- **视觉**：Mission Driver 大圆环包含多个 Plan Loop 小圆环。标注"编排：自动选、自动执行、自动审计"。

### S10 Mission Driver 的真实 Loop——先看一个实例预告（1.5 min）
- **在理解流程图之前，先看它跑完一次的样子**（从 07-10 日志截取）：
  ```
  08:00  CHECK → mvn clean install 154 模块 BUILD SUCCESS ✓
  08:05  REVIEW_PLANS → 4 个 draft Plan 逐个审查
         - 1100-1 销售定价：iter1 needs_revision → iter2 accept
         - 1100-3 到岸成本：iter1 needs_revision（码值 430 冲突）→ iter2 accept
         - 1100-4 预算管理：iter1 6 blocking → 重大重规 → iter2 accept
  09:30  EXEC_PLANS → 4 个 active Plan 逐个执行完毕，全部全绿 ✅
  16:00  DRAFT_PLANS → 路线图已全 done，nothing
  16:05  DEEP_AUDIT → 自动启动深度审计...
  ```
- **先看到效果，再理解机制。** 接下来展示流程图。
- **视觉**：日志风格，时间线 + 每步状态 + 产物的紧凑显示。

### S11 Mission Driver 的真实 Loop——流程图（2.5 min）
- **来自 `flows/mission-driver.json` 的实际流程**（动画分层构建）：
  - **[第一层] 外环（5 步）**
    ```
    CHECK（健康检查）→ REVIEW_PLANS（draft→active 提升）
      → EXEC_PLANS（执行所有 active Plan）
        → DRAFT_PLANS（从路线图起草新 Plan→自审计→标记 active）
          → DEEP_AUDIT（路线图全 done→自动深度审计→起草新 Plan）
            → 回到 REVIEW_PLANS（循环继续）
    ```
  - **[第二层] EXEC_PLANS 展开**：对每个 active Plan 运行 `plan-execution` subflow（执行→BUILD_VERIFY→CLOSURE_AUDIT→completed）
  - **[第三层] DRAFT_PLANS 展开**：读取路线图→选 1-3 个工作项→起草 Plan→在步骤内切换 coordinator 模式→独立子代理审查→标记 active
- **关键设计决策**：
  - REVIEW 在 EXEC 之前（2026-06-21 重新排序）：启动时先提升所有 draft→active，避免重复起草
  - EXEC 用 `forEach: activePlans()`：主循环不自己循环，一次返回后重新扫描
  - DRAFT 包含自审计：引擎层不单独设 PLAN_AUDIT 步骤
- **每步都是 Loop 的一个节点**：Mission Driver Loop 嵌套 Plan Loop（EXEC 时），Plan Loop 嵌套审查微循环（draft review 时）。三层的嵌套不是概念，是代码。
- **注**：Mission Driver（`tools/mission-driver/` engine.js + flow JSON）是一个具体的工具实现。AGE 工作流不需要这个工具也能运作——它是 AGE 控制原理的一种高效实现，但不是 AGE 本身。nop-app-erp 选择了自动化。
- **视觉**：流程图分三层动画构建。EXEC_PLANS 展开为 subflow 嵌套的详细图。

### S12 DEEP_AUDIT——系统不自满（2 min）
- 路线图全 done → 系统不停止，进入 `deep-audit-loop` subflow（MULTI_AUDIT → OPEN_AUDIT → DRAFT_FROM_AUDITS）
- **07-07 综合审计实例**：4 路并行子代理独立审计，发现 4 严重 + 6 高问题——表名双前缀违规（7 域）、docStatus 不一致、7 域冗余字典、ErrorCode 不统一。1-3 天内全部闭合。
- **这不是"额外安全措施"**。传统模式"全 done"就是交付信号。AGE 说：全 done 不是停止的信号，**是自动升级审计的信号**。
- **视觉**：审计放大镜 → 发现 bug → 起草新 Plan。标注 "System self-audits when idle"。

### S13 Mission Driver 与 Plan Loop 的协作——竞争杠杆审计实例（2 min）
- **07-12 用户要求**："核实 8 个超越点是否完整兑现"
- 这不是一个普通 Plan，是一个**审计 Plan**：
  - 执行后落 DEEP_AUDIT → DRAFT_PLANS 产生 4 个新 Plan（委外引擎、成本要素拆分、测试缺口闭合、文档修正）
  - 然后 EXEC_PLANS 执行这些新 Plan
  - 两层 Loop 协同完成一个从"发现夸大"到"修正夸大"的完整闭环
- **三类裁决**：4 完整兑现 / 2 有缺口（制造委外空壳、Delta 文档数据错误）/ 2 夸大（多公司仅地基、域独立部署言过其实）
- **2 处子代理误判被主代理纠正**——这正是 S07 说的 fresh session 仍然不够，但至少误判被记录并可复核
- **视觉**：两层嵌套 Loop 上标注审计路径。8 个杠杆的三色进度条。

### S14 过渡页——"但还不够"（0.5 min）
- **Plan Loop 和 Mission Driver Loop 解决了"按路线图执行"的问题**
- 但一个更深层的问题是：**路线图本身正确吗？吸引子方向合理吗？**
- "如果路线图方向错了，执行得越好，偏离得越快。"
- **过渡 Act 3**："所以我们需要第三层——不是 Loop，是吸引子本身的演进过程。"
- **视觉**：两环嵌套（Plan + Mission Driver）+ 一个空的外环虚线框 + "？"。

---

## Act 3：最外层控制——Attractor Evolution Cycle（5 页 / 9 min）

### S15 什么是 Attractor——dynamical systems 视角（2 min）
- **AGE 的四层本体**（来自 `attractor-before-harness.md`）：
  ```
  state space（所有可能状态）
    → attractor（系统应长期回归的稳定结构）
      → trajectory（实际演化路径）
        → control（将 trajectory 拉回 attractor 的机制）
  ```
- **关键区分**（文章最常被误解的三点）：
  1. **吸引子 ≠ 边界**——边界禁止行为，吸引子定义汇聚目标
  2. **吸引子 ≠ 更严格的护栏**——护栏在字节码层面检查，吸引子在方向层面运作
  3. **吸引子 ≠ 控制目标**——它先于控制存在，为控制提供最终原因。Plan Loop 和 Mission Driver 是控制层的具体机制
- **三层结构（结构层/承载层/实现层）**：
  ```text
  吸引子本身（结构不变量）—由—→ 承载者（owner docs）—投影为—→ 代码（瞬时实现）
  例："ORM 模型优先"  ———→  app-erp-finance.orm.xml  ———→  生成的 Java Entity + DDL
  ```
  - 吸引子不是文档。文档是承载者。吸引子是"所有实现必须从 ORM XML 单一真相源生成"这个结构不变量。
  - 演讲中为了方便，会说"ORM XML 作为吸引子的承载者"——但台下要理解这个区分。
- **视觉**：四层本体图 + 三层结构调整。右上角标注"最常见的误解（来自 AGE 文章）"。

### S16 nop-app-erp 的三个核心吸引子实例（2 min）
- **吸引子 1：模型驱动（ORM XML 为单一真相源）**
  - 承载者：`module-*/model/app-erp-*.orm.xml`
  - 结构不变量：所有实现必须从 ORM XML 生成，不允许手写绕过的 Entity/DAO
  - 验证方式：`mvn clean install` 触发 codegen，检查有无非生成代码
- **吸引子 2：状态一致性（状态机驱动 BizModel）**
  - 承载者：`docs/design/*/state-machine.md`
  - 结构不变量：每域的审批/过账/状态迁移必须与状态机一致
  - 验证方式：审计对照状态机 vs 代码实现
- **吸引子 3：过账契约（统一 SPI + 红字约定）**
  - 承载者：`docs/design/finance/posting.md`
  - 结构不变量：所有过账走同一个 SPI 契约，红字用同向取负约定
  - 验证方式：新增业务类型必须注册 + 测试验证
- **每个都不只是"一篇文档"**——文档是承载者，真正的吸引子是文档所定义的结构不变量。它们在 22 天内被修订但始终保持一致。
- **视觉**：三个卡片，每张分两行——上行 = 吸引子（结构不变量），下行 = 承载者（owner docs）。

### S17 用户其实在做什么——定义和演进吸引子（2 min）
- AGE 文章 §9 说得很清楚：**"humans define new attractors; AI expands rapidly around the established attractor."**
- **用户的三种介入，对应吸引子的不同层面**：
  - **A 类（明确指明平台机制—9 次，全在 06-22~07-03）**：修正吸引子的技术基底。例："全局租户开关是 `nop.orm.enable-tenant-by-default`" → 修正 ORM 设计吸引子。"所有字典改 string" → 修正数据模型吸引子。
  - **B 类（指明工程原则方向—9 次）**：修正吸引子的结构原则。例："过账方法应挂聚合根 BizModel"（07-01）→ 修正过账引擎吸引子。"架构文档只写通用原则"（07-01）→ 修正文档体系吸引子。
  - **C 类（只让 AI 自查对比—绝大多数）**：在吸引子内让 AI 自行扩张。例："对照 erp-survey 检查功能覆盖"、"承诺的功能是否已完整兑现"。
- **关键洞察**：用户后期介入归零不是因为 AI 学会了写代码，而是因为**吸引子已经定义好了**。A 类+B 类在前期密集出现——每一次都是在修正吸引子的方向。方向对了，AI 可以在吸引子内自动扩张。
- **视觉**：时间线 + 三层模型标注。底部引用 AGE 原文。突出一个具体案例（比如 07-01 过账引擎聚合根质疑——用户用一句话改变了整个过账引擎架构）。

### S18 【全场高潮】三层汇聚——从割裂到嵌套（1.5 min）
- **到目前为止讲了三层，但它们不是独立的——是嵌套的**：
  - Mission Driver 的 `EXEC_PLANS` 步骤，对每个 active Plan 运行 `plan-execution` subflow —→ **包含 Plan Loop**
  - Attractor 的一次修订（如 07-01 过账引擎聚合根纠正）→ 产生新的路线图工作项 → **Mission Driver 自动编排** → 分解为多个 Plan → **每个 Plan 走 Plan Loop**
  - Plan Loop 的完成状态 → 同步回路线图 → Mission Driver 自动选下一个
- **这就是"嵌套"的含义：**
  ```
  最外层：吸引子演进（周级）—— 定义方向
    ↓ 方向变化产生新工作项
  中间层：Mission Driver（天级）—— 自动编排
    ↓ 每个工作项走 Plan Loop
  最内层：Plan（小时级）—— 单次变更关闭
    ↑ 完成后状态同步回路线图
  ```
- **高潮句**："Plan 关闭每一次变更。Mission 编排每一个计划。吸引子定义方向。三层都在，系统才能自运转。缺一层，要么没方向，要么没执行，要么没闭环。"
- **视觉**：三环嵌套动画逐层构建——先展示最外层（吸引子演进）→ 点击展开中间层（Mission Driver 在其中遍历）→ 点击展开最内层（Plan Loop 在其中多次运行）。形成一个完整的系统视图。这就是 S01 的锚图，现在观众理解了它的含义。

---

## Act 4：系统输出——22 天接力赛（4 页 / 7 min）

### S19 八阶段——三层 Loop 的接力故事（2.5 min）
- **不是数据陈列，是一个接力故事**：
  - **S1 设计期（4d）**：吸引子建立——竞品调研 24 项目、10 域 ORM 模型、文档体系。**主导层：吸引子定义**
  - **S2 深化期（2d）**：吸引子基线确定——grill-with-docs 83 题决议、设计审查。**主导层：吸引子校准**
  - **S3 核心逻辑（2d）**：Plan Loop 首次密集运行——9 个 Plan、过账引擎 + 库存 + 采购/销售。**主导层：Plan Loop**
  - **S4 扩展域（2d）**：Mission Driver Loop 首次批量运行——13 域逻辑铺开。**主导层：Mission Driver**
  - **S5 业财+运营（3d）**：三层协同峰值——P2P/O2C/报表/看板/通知。**主导层：三层协同**
  - **S6 审计（1d）**：吸引子修正触发——综合审计 4 严重+6 高。**主导层：吸引子演进**
  - **S7 E2E（2d）**：Control Loop 观测性就绪——Playwright + 种子数据。**主导层：Plan Loop（增强）**
  - **S8 收尾（4d）**：竞争兑现核实 + 产品化打磨。**主导层：三层收敛**
- **每阶段 15-20s，聚焦"哪层 Loop 当主导"的变化**——让观众看到三层交替主导而非并行运行
- **视觉**：特殊 Gantt——每阶段上方标注"主导层"标签（色块对应三环颜色），底部标注关键事件（如"07-01：用户纠正过账引擎吸引子"）。

### S20 验证基线 + 计划产出密度（1.5 min）
- 验证基线演进（模块数 81→154 / 测试数 0→~2900 / E2E 0→260）
- 计划产出分布（07-01 爆发 9、07-02 爆发 14、07-10 爆发 13）
- **关键**：产出的爆发不是均匀的——是 Mission Driver Loop 批处理模式的体现。一批 draft → 一批 review → 一批 exec → 一批 close。
- **视觉**：双 Y 轴图 + 标注批处理模式。强调"全绿验证贯穿始终"。

### S21 审计方法论 8 阶段演进（1.5 min）
- **这是整个系统中最聪明的 Meta 洞察**——控制 Loop 自身也在演进：
  1. 07-02：单代理 + rg 扫描（首次合规审计）
  2. 07-04：单代理 + 6 维度（结束审计范式定型）
  3. 07-05-1300：3 路并行子代理（首次多代理并行）
  4. 07-05-1400：**4 路对抗性子代理——方法论飞跃：从'方法存在'到'语义保真'**
  5. 07-05-1500：工具化合规检查器（15 条 R 规则 → 脚本）
  6. 07-06：UC 断言对照（粒度切换到业务实现率）
  7. 07-07：4 路 + 自省（审计质量回顾）
  8. 07-12：3 路 + 主代理复核（纠正 2 处子代理误判）
- **5 个演进特征**：单→多路 / 方法存在→语义保真 / 人工→工具化 / 代码合规→业务实现率 / 子代理断言须主代理复核
- **视觉**：螺旋上升阶梯图，第 4 阶大红星标注"方法论飞跃"，第 5 阶标注工具图标。

### S22 路线图终态（1.5 min）
- 三个子路线图全 done（crud / core-business / extended）
- Mission Driver 遍历了每个工作项——没有漏项、没有跳过的
- "这不是'22 天刚好做完了'——如果没做完，Mission Driver 不会停。"
- **视觉**：三份路线图截图（全部 ✅ done）。底部 "189 Plans / 9 Audits / 0 Unplanned"。

---

## Act 5：带走这三层控制结构（4 页 / 8 min）

### S23 可复制的是控制结构，不是代码（2 min）
- **有人看到 22 天 154 模块，说 AI 真强——这是错误归因**
- 真正可复制的是三层控制 Loop 的设计模式：
  1. **Plan Loop**（docs/plans/ + fresh session 审计契约）——任何项目都能用
  2. **Mission Driver Loop**（tools/mission-driver/ engine.js + flow JSON）——在 AGE 模板中已包含
  3. **Attractor Evolution Cycle**（owner docs 体系 + 设计审查流程）——在 AGE 模板的 docs 骨架中
- AGE 模板包含这三层的完整实现：`cp -r ~/app/attractor-guided-engineering-template/ ./my-project/`
  - `docs/plans/00-plan-authoring-and-execution-guide.md` → Plan Loop
  - `tools/mission-driver/`（engine.js + flows + prompts）→ Mission Driver Loop
  - `docs/design/` + `docs/architecture/` 骨架 → 承载吸引子
  - 需要填：`missions/*.json` + `docs/context/` 适配到项目
- **视觉**：终端命令截图 `cp -r .../attractor-guided-engineering-template/ ./my-project/` + 目录树标注三层对应。

### S24 落在自己项目里需要几步（1.5 min）
- 最简路径（从 AGE 模板的 Day 0 checklist 开始）：
  1. 复制模板 `cp -r .../attractor-guided-engineering-template/ ./my-project/`
  2. 填 `docs/context/project-context.md`（验证命令）——约 15 min
  3. 填 `docs/context/ai-autonomy-policy.md`（保护区域）——约 10 min
  4. **定义第一个吸引子**（写核心架构文档 + 第一个设计文档）——这步不能外包给 AI
  5. 创建第一个路线图工作项（`docs/backlog/README.md`）
  6. 创建 `missions/<name>.json` + `tools/mission-driver.sh`
  7. 运行 `./tools/mission-driver.sh run <name>` → Mission Driver 自动循环开始
- **注意**：吸引子需要人来定义。"The responsibility for defining a new attractor cannot be outsourced to AI by default."（AGE 文章原话）
- **视觉**：7 步检查清单 + 每步估计时间。步骤 4 加红色星号标注"需人工"。

### S25 三个 Takeaway（1.5 min）
- **Takeaway 1 · 把"变更"从对话变成契约**
  - 对话不可追溯 → Plan Loop 给每次变更一个关闭契约（draft → review → audit → close）
- **Takeaway 2 · 把"编排"从记忆变成自动**
  - 不需要人记住下一个做什么 → Mission Driver Loop 自动选工作项、起草、验证
- **Takeaway 3 · 把"方向"从个人变成文件**
  - 架构判断是稀缺资源，不应只在一个人脑子里 → 吸引子承载于 owner docs，可版本、可审计、可继承
- **视觉**：三张卡片，每张一个 icon + 标题 + 一句话核心。

### S26 结尾——不是回扣，是挑战（1.5 min）
- **一句话总结**："Plan 关闭每一次变更。Mission 编排每一个计划。Attractor 定义方向。三者都到位，系统才会自运转。"
- **停顿**
- **挑战**（看向观众）：
  - "但 AGE 文章说得很清楚：吸引子不能外包给 AI。定义方向是你的工作。AI 可以在吸引子内高速扩张，但吸引子需要人来定义。"
  - "所以今天讲的不是一个'AI 写代码 4000 行/天'的故事。"
  - "这是一个'人定义方向，AI 执行扩张，系统检查偏离'的故事。"
  - "三层控制 Loop。"
- **回扣 S02 的问题**："100 个变更后，系统还在正确的结构上吗？答案是——是的——如果三层 Loop 都在。"
- **项目地址 + "谢谢，Q&A"**
- **视觉**：三环嵌套锚图（同 S01/S18，但全屏）+ 底部"人定义方向 · AI 执行扩张 · 系统检查偏离"。左下方项目信息。

---

## 附录 A：3 路独立子代理审查共识记录

| 维度 | 代理 | 关键发现 | 采纳 |
|------|------|---------|------|
| AGE 概念准确性 | AGE 专家 | "Attractor Loop"用词错误——attractor 是结构不是 Loop | ✅ 改为 Attractor Evolution Cycle |
| AGE 概念准确性 | AGE 专家 | ORM XML 不是吸引子本身是承载者 → 引入三层结构 | ✅ S15 新增三层结构区分 |
| AGE 概念准确性 | AGE 专家 | Mission Driver 是工具实现不是 AGE 本体 | ✅ S11 添加免责说明 |
| AGE 概念准确性 | AGE 专家 | 需要完整展示 AGE 本体论四层 | ✅ S15 新增 |
| 叙事/观众体验 | 故事专家 | 无高潮 → 重构 S18 为汇聚高潮 | ✅ S18 重写 |
| 叙事/观众体验 | 故事专家 | S02 理论太早 → 用具体痛点开场 | ✅ S02 用竞争杠杆审计故事 |
| 叙事/观众体验 | 故事专家 | S26 结尾弱 → 改为挑战+行动指令 | ✅ S26 重写 |
| 叙事/观众体验 | 故事专家 | 缺人的故事 → 增强用户关键决策 | ✅ S17 增加具体案例 |
| 技术事实 | 技术审查 | Plan 1100-1 拦截 1 个→实际 4 个 P0 | ✅ S06 修正 |
| 技术事实 | 技术审查 | 验证步骤位置不准确 | ✅ S05 修正 |
| 技术事实 | 技术审查 | DEEP_AUDIT 描述遗漏 2 个步骤 | ✅ S12 注明了简化 |
| 技术事实 | 技术审查 | 三层 Loop 是实践映射非 AGE 原文 | ✅ S18 标注出处 |

## 附录 B：AGE 本体论 ↔ v3 叙事映射

```
AGE 本体论（dynamical systems）        v3 叙事映射
─────────────────────────────────────────────────────────────
state space（所有可能状态）            →  不直接展示，隐含在"AI 可能写出任何东西"的背景中
attractor（稳定结构）                  →  Act 3：吸引子（结构层不变量）
  └─ 承载者（owner docs）             →  Act 3：ORM XML、状态机文档、过账设计
  └─ 瞬时投影（代码）                  →  Act 4 证据
trajectory（实际演化路径）             →  Act 1 Plan Loop 记录 + Act 4 验证基线
control（将轨迹拉回吸引子的机制）       →  Act 1 + Act 2（两层控制 Loop）
  └─ Plan 作为关闭契约                →  Act 1
  └─ Mission Driver 编排              →  Act 2
  └─ 审计（独立 fresh session）        →  Act 1 S07 + Act 2 S13
  └─ 工具化合规检查                    →  Act 2 S13
```

## 附录 C：视觉素材清单

| 页码 | 视觉类型 | 描述 |
|------|----------|------|
| **S01** | **三嵌套圆环** | **全场的锚图**——内=Plan、中=Mission Driver、外=Attractor Evolution Cycle |
| S02 | 左右对比 + grep 截图 | 左=状态检查(→架构漂移)，右=轨迹检查(→收敛) + 子代理误判证据 |
| **S05** | **Plan Loop 流程图** | draft→独立审查→active→执行(含验证)→独立结束审计→completed |
| S06 | 横向时间线 | Plan 1100-1 从起草到完成的完整过程，拦截点红色标注 |
| **S07** | **双 AI 头像对比** | 同一上下文 vs fresh session，+ grep 截图 |
| S10 | 实例预览 | 日志风格的 07-10 运行记录 |
| **S11** | **Mission Driver 流程图（三层动画）** | 第一层 5 步外环 → 第二层 EXEC 展开 → 第三层 DRAFT 展开 |
| S12 | 审计→Bug→Plan | DEEP_AUDIT 流程图 + 07-07 实例 |
| **S15** | **四层本体图 + 三层结构** | state space→attractor→trajectory→control + 结构层/承载层/实现层 |
| S16 | 三卡片 | 每卡：吸引子（不变量）+ 承载者（owner doc） |
| S17 | 时间线 + A/B/C 介入 | 标注用户在何时修正了哪个吸引子 |
| **S18** | **三环汇聚动画（高潮）** | 逐层构建→三个环完全嵌套→底部文字 |
| S19 | 特殊 Gantt | 8 阶段 + 每阶段标注"主导层"标签 |
| S21 | 螺旋上升阶梯 | 审计方法论 8 阶段演进，第 4 阶大红星 |
| S23 | 终端+目录树 | cp -r 命令 + 三层 Loop 对应的 AGE 模板组件 |
| S24 | 7 步清单 | 从复制模板到 Mission Driver 自动运行 |
| **S26** | **三环锚图全屏** | 同 S01/S18 + 底部"人定义方向 · AI 执行扩张 · 系统检查偏离" |

**加粗页码** = 最关键的视觉页（S01/S05/S07/S11/S15/S18/S26）。这 7 张图做出来，全场叙事就立住了。
