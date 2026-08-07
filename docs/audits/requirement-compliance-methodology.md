# 需求-实现符合性审计方法论

> 状态：active（mission `requirement-compliance` Phase 1 契约，仿 audit-remediation MQ 文档先行范式）
> 来源：`docs/backlog/requirement-compliance-roadmap.md` Work Item 0.1（M0 审计编排基线）
> 裁决依据：`docs/discussions/2026-08-02-1700-requirement-implementation-compliance-audit.md`（Q1=(c) / Q4=(a) 已裁决）
> 方法论据先例：`docs/skills/audit-remediation-roadmap-authoring-prompt.md`（MQ 文档先行范式骨架）、`docs/audits/00-audit-execution-guide.md`（审计执行通用规范）

## 用途

本文件是 mission `requirement-compliance` 的**审计契约**（Phase 1 contract），为 MA1-MA4 全部审计工作项定义统一的、可被 mission driver 自主消费的方法学。

**本审计与既有 audit-remediation 审计的根本区别**（讨论文档 §根因分析）：

- **audit-remediation（MA1-MA7）**：审计维度定位 = **doc↔code 文本一致性**，收口偏好 = **方案 B 优先**（documented simplification / Deferred 合法化 doc↔code 分歧）。审计闭环后 doc↔code 自洽，但需求层（product-scope/use-cases）与实现的分歧被写进文档合法化。
- **requirement-compliance（本 mission）**：审计维度定位 = **需求→实现符合性**（从需求真相源出发，逐模块逐功能点核对**运行时行为**是否符合需求）；收口偏好 = **方案 A 优先**（Q4 已裁决=(a)：P0/P1 必须实现，禁止方案 B 关闭，无例外通道）。

本方法论**不复跑 MA1-MA7**，只审"行为是否符合需求契约"，与既有审计按本文件 §去重协议 协作。

### 何时使用本方法论

- MA1 工作项 A1.1-A1.51（功能切片 × UC 清单的逐切片五级追踪审计）
- MA2 工作项 A2.1-A2.9（按域枚举的方案 B 关闭项复查）
- MA3 工作项 A3.1-A3.5（successor 触发条件复查）
- MA4 工作项 A4.1/A4.2 展开器及其展开的 A4.1.n/A4.2.n 实体行（运行时行为验证）

### 何时不使用本方法论

- doc↔code 文本一致性审计 → audit-remediation 已收口（见 §去重协议）
- 单一对象的窄审计 → 用 `docs/skills/` 下的对象级提示（orm-model-audit / code-quality-audit 等）
- 需求本身的修订 → 属 0.2 范围，须经人工批准（见 §真相源冻结条款）

---

## 前置阅读

每个 MA 工作项的执行 agent 在起草 plan 前**必须完整阅读**：

### 强制上下文（裁决与契约）
- 本文件（方法论契约）
- `docs/discussions/2026-08-02-1700-requirement-implementation-compliance-audit.md`（Q1/Q4 裁决原文）
- `docs/backlog/requirement-compliance-roadmap.md`（工作项定义与 Work Item Details）

### 真相源（审计基线，冻结条款适用）
- `docs/requirements/product-scope.md`（顶层范围真相源）
- 目标域 `docs/design/<domain>/use-cases.md`（UC 功能契约真相源）
- 目标域 owner doc（机制说明、状态机定义、跨域协作契约，**设计参考**非真相源）

### 既有审计衔接（去重输入）
- `docs/audits/arm-index.md`（P0/P1/P2 finding 索引；方案 B 关闭项与 successor 声明内嵌于此。具体计数随审计推进增长，0.3 导出时以实测为准）
- `docs/audits/2026-07-2*-arm-ma2-*.md`（既有 MA2 状态机/业财链路行为审计报告——已证实行为作为既有证据输入）

### 通用规范
- `docs/audits/00-audit-execution-guide.md`（审计执行通用规范、草案审查/结束审计检查点）
- `docs/plans/00-plan-authoring-and-execution-guide.md`（plan 格式与关闭契约）
- `docs/context/ai-autonomy-policy.md`（保护区域表、自主级别）
- `docs/audits/compliance-baseline.md` + `docs/audits/nop-compliance-checker.sh`（过程纪律门控工具）

---

## §1 五级追踪矩阵模板

每个 MA1 切片报告的核心载体是**五级追踪矩阵**：从需求真相源逐级追踪到运行时行为，逐 UC 核验每一级是否存在且一致。

### 矩阵结构（5 列）

| 列 | 名称 | 内容 | 证据格式（强制） | 判读标准 |
|----|------|------|----------------|---------|
| L1 | use-case 需求契约 | UC 编号 + 标题 + 验收标准原文 | 引用格式：`<use-cases.md>` UC-XXX-NN 锚点 + 原文摘录（逐字，不转述；`:line` 仅写时实测导航） | 必须可定位（UC 编号 + 逐字原文）；验收标准逐条进入 L5 判读 |
| L2 | owner doc 契约 | 该 UC 对应的机制/状态机/字段契约 | 引用格式：`<owner-doc>:<section>` | 设计参考；与 L1 冲突时**一律以 L1 为准**（见 §4） |
| L3 | 代码路径 | 实现该 UC 的 BizModel/Processor/方法 | 引用格式：`<file>#<method>`（方法锚点）+ 关键行为断言（行号仅写时实测导航） | 必须存在且可达（grep 方法名可定位）；行为断言与代码语义一致；行号漂移不构成引用失效；跨域调用链须列全 |
| L4 | 测试断言 | 覆盖该 UC 的单测/E2E 测试方法 | 引用格式：`<TestFile>.java#<method>` 或 `tests/e2e/<spec>.spec.ts#<describe>` | 须断言验收标准而非仅冒烟通过；MA5 已评级断言强度可引用 |
| L5 | 运行时行为 | 实际运行的行为证据 | 来源：`tests/e2e/` 复用 + 临时探针（MA4 展开）；既有 MA2 报告已证实行为可直接引用（见 §去重协议） | 须与 L1 验收标准逐条对照；存疑点入"静态存疑点清单"交 MA4 |

### 逐级证据格式规范

- **L1 需求契约原文引用格式**：`<域>/use-cases.md` UC-XXX-NN 标题 + 验收标准原文块引用。**禁止转述**——验收标准必须逐字引用，避免"代理转述已向实现妥协"（Q1 裁决根因）。锚点 = 文件路径 + UC 编号 + 逐字原文（原文本身可全文 grep 定位）；`:line` 仅写时实测导航提示，不跨会话要求稳定。
- **L2 owner doc 契约引用格式**：`<owner-doc>:<section anchor>`。owner doc 仅作设计参考；若 L2 与 L1 冲突，在矩阵行的"冲突裁决"列注明"以 L1 为准，L2 推定已向实现妥协"。
- **L3 代码路径引用格式**：`module-<domain>/erp-<short>-service/.../Erp<Domain><X>BizModel.java#<method>` 或 `.../<X>Processor.java#<method>`（方法锚点，`rg "<method>" <file>` 可定位）。锚点 = 文件路径 + 方法名 + 关键行为断言（如"自环优先抛 ERR"），复核以"grep 方法名 → 读方法体 → 对照行为断言"为准。行号仅写时实测导航提示（`#method :123-146`，标注"写时实测"），**漂移不构成引用失效**。跨域调用链须列全路径（Facade → Processor → 跨域 I*Biz）。
- **L4 测试断言引用格式**：单测 `module-<domain>/erp-<short>-service/src/test/.../Test<Name>.java#<method>`；E2E `tests/e2e/<domain>/<spec>.spec.ts#<describe> it。<case>`。须注明断言强度（强断言/弱断言/仅冒烟，引用 MA5 评级）。
- **L5 运行时行为证据来源**：① 复用既有 MA2 报告已证实行为（引用 `docs/audits/2026-07-2*-arm-ma2-*`）；② 复用 `tests/e2e/` + `tests/e2e/orchestration/_helper.ts` 原语；③ 临时探针（仅 MA4 展开项使用，须落盘探针证据）。

### 矩阵填充纪律

- 每个 UC 一行，不可合并多个 UC 到一行（违反完整枚举纪律，见 §3）。
- L1-L4 在 MA1 切片报告完成时填齐；L5 的"存疑点"在该报告的"静态存疑点清单"段登记，交 MA4 展开为运行时验证。
- 已被既有 MA2 报告证实的 L5 行为，在矩阵行注明"行为已证实（引用 MA2 报告）"，不重复验证（见 §去重协议）。

### 引用锚点纪律

**锚点 = 文件路径 + 锚点名（UC 编号 / 方法名 / describe 名）+ 关键断言（验收标准原文 / 行为断言）**。行号（`:line`）仅作**写时实测的导航提示**，标注"写时实测"即可，**不跨会话要求稳定**。

- **禁止将"行号漂移/陈旧/偏移"作为 finding 或 review 阻塞项**。代码重构（如 per-mutation Processor 拆分）导致行号变化时，按锚点名（方法名）重新定位复核，不要求文档行号同步更新。
- **审查与结束审计的核验对象** = 锚点名存在（grep `#<method>` / UC 编号可命中）+ 关键断言与代码/原文语义一致；不复核行号精确性。
- **行为断言是 anti-hollow 的核心载体**：仅"方法名存在"不构成证据，须断言该方法的关键行为（守卫/抛错/状态迁移/计算语义），防止占位实现空转。
- **写时实测行号仍是好的导航辅助**：报告产出时顺手标注当前行号可加速人工复核，但不得在后续审计中因行号偏移要求修订文档。

---

## §2 P0/P1/P2/接受 四级分级判据

每条 finding 必须落入且仅落入一个级别。**分级与修复通道绑定**（Q4 已裁决=(a)，P0/P1 强制实现禁方案 B 无例外）。

### 分级判据表（4 行，含 Q4 修复义务绑定列）

| 级别 | 定义 | 判据（满足任一即归此级，取最高） | Q4 修复义务绑定（无例外通道） | 示例 |
|------|------|-------------------------------|------------------------------|------|
| **P0** | 阻断性需求分歧 | ① 需求契约要求的活跃数据破坏防护未实现（如并发无锁致库存负数/凭证重复过账）；② 需求契约要求的安全/数据隔离未实现；③ 需求契约要求的核心业务循环断裂（如销售到收款链路某环节完全缺失）；④ 需求契约要求的会计过账正确性破坏 | **必须实现**。MR0 即时通道止血（发现即独立 fix plan，不等里程碑完成）。**禁止方案 B 关闭**。技术不可行项须更深设计变更（如重构 billR 加判别列 + 对应 UK），**非退缩到方案 B 降级** | 需求要求凭证幂等但实际重复过账；需求要求期间 CLOSED 后禁止过账但实际可过 |
| **P1** | 严重需求分歧 | ① 需求契约要求的功能完全缺失或行为实质偏离验收标准；② 需求契约要求的异常路径未实现；③ 需求契约要求的状态迁移不可达或非法迁移未守卫；④ 需求契约要求的跨域契约行为不一致；⑤ 测试断言完全缺失或仅冒烟（验收标准无断言） | **必须实现**。MR1 批量修复通道（R1.0 展开为 RC-R1.n）。**禁止方案 B 关闭**。唯一出口 = 需求本身不合理经人工批准改 product-scope（需求变更非降级，见 §4） | 需求要求订单维度核销但实际按客户维度；需求要求 NCR-CAPA 闭环但 CAPA 无关闭校验 |
| **P2** | 改进型需求分歧 | ① 需求契约的次要验收标准未完全满足（主路径 OK，边界场景弱）；② 需求契约的可用性/可观测性要求未充分实现；③ 文档化简化但**有显式人工批准记录**（§4 三判据满足其一） | **登记不强制**。记入 finding 报告 + arm-index，作为 successor 由后续 roadmap 处理。**允许 owner doc 显式 documented simplification 标注**（须经 §4 三判据之一） | 某报表的次要筛选维度未实现但主维度可用；某批量操作的进度反馈缺失 |
| **接受** | 符合需求契约 | 需求契约的全部验收标准在 L3-L5 各级均有证据且一致 | **无需修复**。记入矩阵"符合"列 | 五级追踪全部对齐，运行时行为符合验收标准 |

### 分级操作纪律

- **取最高原则**：一条 finding 同时命中多级时，归最高级（P0 > P1 > P2 > 接受）。
- **Q4 例外通道禁令**：P0/P1 分歧**禁止**以下任何收口方式——方案 B（documented simplification / Deferred 标注）、"技术不可行"降级、"representative enough"抽样放过。唯一合法出口见 §4。
- **会计/数据安全类无例外**：对齐 `docs/context/ai-autonomy-policy.md` 保护区域，会计过账/数据删除/数据安全类的 P0/P1 强制实现无任何例外。
- **既有 arm-index P0 deferred 边界**：既有 arm-index P0 deferred 项（如 P0-MA2-018 字面 UK 三重契约冲突）**不属本审计自动重开范围**；仅当 MA2 复查将其重新分级时进入 MR1（roadmap §当前基线）。

---

## §3 完整枚举纪律（禁止抽样）

**本纪律是 MA1 里程碑的核心，也是讨论文档 §根因分析 第 4 条教训的直接修复**："审计粒度是'域×维度 + 抽样'而非'功能点全量'"导致执行退化为抽样。

### 工作项 = 功能切片 × 显式 UC 清单

每个 MA1 工作项（A1.1-A1.51）= **一个功能切片 × 显式 UC 清单**（见 roadmap MA1 表）。一份 plan 覆盖该行全部 UC 的五级追踪。**全表（51 行）即完整工作量**，无任何"代表性抽样"空间。

### 反例（均视为未完成）

| 反模式 | 症状 | 判定 |
|--------|------|------|
| 合并切片 | "finance 全域审计"一个工作项覆盖 F1-F7 | **未完成**。必须拆为 A1.1-A1.7 七个切片 |
| 代表性抽样 | "finance 选 F1 过账做代表，其余推论" | **未完成**。每个切片必须独立五级追踪 |
| UC 跳号 | A1.1 覆盖 UC-FIN-01/02/03/04/12/15，但报告只核 01/02/03 跳过 12/15 | **未完成**。行内 UC 编号须逐 UC 核对，不可跳号 |
| 验收标准抽样 | 某 UC 有 5 条验收标准，只核前 2 条 | **未完成**。每条验收标准须进入 L5 判读 |
| 跨 UC 合并行 | 矩阵一行合并 UC-FIN-01 与 UC-FIN-02 | **未完成**。每个 UC 一行 |

### 完成判据

- MA1 里程碑完成 = 本表全部 51 行 `done`（含 notify 切片 A1.51，待 0.2 补写 use-cases 后纳入）。
- 每行 `done` = 该行全部 UC 的五级追踪矩阵填齐 + 符合性结论给出 + 静态存疑点清单登记（供 MA4 展开）。
- **任何"代表性抽样"即视为未完成**——这是 Q2 已裁决方向（功能切片全量，禁止抽样）的强制纪律。

---

## §4 Q1 真相源层级与冲突裁决规则

> **Q1 裁决原文**（讨论文档 2026-08-02-1700）：采用 **(c) 两者逐项对照，分歧处逐项裁决**。

### 真相源层级（权威性从高到低）

| 层级 | 真相源 | 角色 | 权威性 |
|------|--------|------|--------|
| 1（顶层范围） | `docs/requirements/product-scope.md` | 产品范围基线 | **权威** |
| 2（功能契约） | 各域 `docs/design/<domain>/use-cases.md`（192 UC） | 功能验收契约 | **权威** |
| 3（设计参考） | 各域 owner doc（机制说明、状态机、跨域协作） | 设计参考 | **非真相源**——机制/状态机/跨域契约的说明，但需求权威性低于 1/2 |

### 冲突裁决规则

**当 owner doc（层级 3）与 product-scope/use-cases（层级 1/2）冲突时，一律以需求真相源（1/2）为准**，推定 owner doc 已向实现妥协（讨论文档 §根因分析 第 1 条：审计收口方式系统性允许"文档对齐代替需求实现"）。

**冲突不得通过修改需求真相源来消除**（除非满足下方"显式人工批准记录"三判据之一，且属需求变更非降级，见 §真相源冻结条款）。

### "显式人工批准记录"三判据证据标准

> 本标准用于 MA2 工作项判定 arm-index 方案 B 关闭项是否"经人工批准"（避免字面误重开，roadmap MA2 详情）。三判据满足其一即成立；AI 自写标注**不算**人工批准。

| 判据 | 证据要求 | 适用场景 |
|------|---------|---------|
| **(i) plan 含独立 plan-audit 通过记录** | plan 文件的 `Draft Review Record` / `## Closure` 含独立子代理或审查者的通过证据（task id / 会话记录） | 该简化经独立审计裁决（区别于"静默降级"） |
| **(ii) owner doc 显式 documented simplification 标注且经人工批准** | owner doc 含显式 `documented simplification` / `Deferred` 段落 + 该标注的批准来源可追溯（git log / commit message / 讨论文档） | 该简化在 owner doc 中显式声明（AI 自写标注不算，须人工批准痕迹） |
| **(iii) product-scope 范围裁剪登记** | product-scope 明确将该功能列入"不在范围"或"后续阶段" + 裁剪理由 + 影响面 + 批准人 | 该功能本身不在产品范围内（需求变更非降级） |

**判据应用顺序**：(i) → (ii) → (iii)。判据三仅当 (i)/(ii) 均不成立时兜底触发。

**代理独立审计通过 = "审计裁决质量证据"**（可区分"静默降级" vs "经审计裁决的简化"），**不算**人工批准（参照 `docs/context/ai-autonomy-policy.md`）。

### notify 特例

notify 是已实现的跨域子系统但无 use-cases 文件。**Q1 裁决：0.2 必须补写完整 use-cases**（不标注 N/A——已实现子系统必须有需求契约）。补写后纳入 MA1 审计范围（A1.51 切片），可能新增切片行。

---

## §5 Q4 修复义务与保护区域暂停协议

> **Q4 裁决原文**（讨论文档 2026-08-02-1700）：采用 **(a) P0/P1 必须实现，禁止再用方案 B 关闭**；**无"技术不可行"例外通道**；唯一出口 = 需求本身不合理时经人工批准修改 product-scope（需求变更非降级）。

### 修复义务（强制，无例外）

| 级别 | 义务 | 关闭方式 |
|------|------|---------|
| P0 | **必须实现** | MR0 即时通道（§MR0） |
| P1 | **必须实现** | MR1 批量修复通道（§MR1） |
| P2 | 登记不强制 | finding 报告 + arm-index successor |
| 接受 | 无需修复 | — |

**禁止的关闭方式**（P0/P1，无任何例外）：
- ❌ 方案 B（documented simplification / Deferred 标注）
- ❌ "技术不可行"降级
- ❌ "representative enough"抽样放过
- ❌ "已在 owner doc 说明"自圆（owner doc 不是真相源，§4）

**唯一合法出口**：审计发现**需求本身不合理**（非实现不达标）时，经人工批准修改 product-scope（这是**需求变更**，非方案 B 降级；须登记变更理由 + 影响面 + 批准人）。

**技术不可行项的处理**：须找到技术可行路径（更深设计变更），而非退缩到方案 B。例：P0-MA2-018 字面 UK 三重契约冲突 → 重构 billR 加 acctSchemaId/postingType/isReversed 判别列 + 对应 UK（而非降级）。

### 保护区域三类 ask-first 门控

涉及以下三类区域的修复（无论 P0/P1/P2），**必须 ask-first + 独立 plan-audit**（对齐 `docs/context/ai-autonomy-policy.md` 保护区域表）：

| 保护区域 | 范围 | 门控要求 |
|---------|------|---------|
| **ORM 结构变更** | `module-<domain>/model/*.orm.xml` 字段/索引/UK/实体 | ask-first + 独立 plan-audit |
| **会计过账逻辑** | VoucherFact / PostingProcessor 核心路径 | ask-first + 独立 plan-audit |
| **数据删除 / 数据迁移** | 任何删除/迁移数据的逻辑 | ask-first + 独立 plan-audit |

### 保护区域暂停协议（无人值守 driver 下可操作）

> 吸取 P2-MA6-001 教训：不能只依赖"草案审查预授权"——触及保护区域的修复行须显式 ask-first 人工确认 checkbox。

**执行机制**（mission driver 自主驱动时）：

1. **R1.0/MR0 展开时标注**：R1.0 展开为 RC-R1.n 时，每行标注"触及保护区域"（是/否 + 类别）。MR0 动态创建 R0.n 时同理。
2. **触及行的 plan 须含显式 checkbox**：触及保护区域的修复 plan，其 Phase 须含显式 `- [ ] ask-first 人工确认（保护区域：<类别>）` checkbox。
3. **driver 执行到触及行时暂停该行**：mission driver 执行到触及行时，**暂停该行**等待人工批准记录（批准登记于 plan 文件 checkbox）。**非触及行继续执行**（不阻塞整个里程碑）。
4. **mission 启动前可预授权**：人工可在 mission 启动前按修复类目预授权（须列明授权类目清单，见 roadmap §预授权声明）。

### 预授权类目清单（roadmap 已声明）

| 类目 | 预授权状态 |
|------|-----------|
| 文档更新类修复（owner doc / use-cases / arm-index） | 预授权自动执行 |
| 代码逻辑修复（BizModel / Processor / xbiz / view.xml） | 预授权自动执行 |
| ORM 结构变更（orm.xml 字段/索引/UK/实体） | **须 ask-first** + 独立 plan-audit |
| 会计过账逻辑变更（VoucherFact / PostingProcessor 核心路径） | **须 ask-first** + 独立 plan-audit |
| 数据删除 / 数据迁移 | **须 ask-first** + 独立 plan-audit |
| 未列明的修复类目 | **默认须 ask-first** |

### 人工扩展授权登记（2026-08-07 批准 / 2026-08-08 生效）

> 依据 `docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md` §5（批准人：用户，逐项人工裁决）。以下为**人工扩展授权**（human-in-the-loop 扩展，非自动放宽；approver 边界逐项选定，报告 §5/§6 为唯一权威登记）：

| 授权 | 范围（判据，符合才免 ask-first） | 回落条件 |
|------|-----------------------------------|---------|
| **ORM-Q3 纯加性 ORM 批量授权**（Q3=选项 B） | 表 A 中「**加列 / 加 UK / 新增实体**，不改既有语义、无 NOT NULL 无默认值列、无涉及既有数据的 UK 增设（须先数据变更评估）、无删除/迁移/索引结构改造」 | 超出即回落 ORM ask-first + 独立 fix plan + 独立 plan-audit |
| **ACCOUNT-Q4 收敛性会计批量授权**（Q4=选项 B） | 表 B 中「**使实现向 owner doc 契约收敛**」的修复；**不得反向修改 owner doc 契约段**（§9 真相源冻结不因本授权解除）；不涉数据删除/迁移 | VoucherFact / PostingProcessor 核心路径**改动行为**仍须独立 plan-audit |
| **TRUTH-Q10 use-cases 命名对齐**（Q10=选项 A） | P2-RC-005/011/016/012 的真相源命名修订，经批准按 §9 流程登记（变更理由 + 影响面 + 批准人） | 其他真相源契约段修订仍须 §9 冻结人工批准 |

> **Q1 裁决覆盖自动展开时机**：R1.0 展开器启动时机由同一报告 §5 Q1 约束——「**MA1-MA4 完成后不自动启动，保持 todo，待另行人工裁决**」。cold-start driver 读到本段应据此跳过自动展开。
>
> **§7 追加裁决（2026-08-08，同一讨论文档 §7）覆盖本段**：
> - **R1.0-2026-08-08（A1）**：经人工裁决 **R1.0 分批启动**——第一批纯预授权类修复（A2 代码逻辑类），第二批越界项逐项暂停 ask-first。cold-start driver 据此展开 R1.0，不视为"自动展开"违规（Q1 已由 §7 A1 撤销挂起状态）。
> - **P1-RC-091（A3）**：试算平衡 BUDGET/COMMITMENT 过滤**属会计核心路径**，须独立 plan-audit + ask-first，不自动执行。
> - **P2-RC-061（A4）**：修复形态=**纯逻辑修复**（`EquipmentStatusLinker.restoreToRunning` 补 IDLE 分支），不改 ORM，按 A2 预授权。
> - **P2-RC-057（A5）**：修复=ORM `ErpMdMaterialCategory.priceValidationLevel` defaultValue 改 `"WARN"`，纯收敛修复（不改表结构/既有数据/行为），按 ORM-Q3 纯加性类自动执行。

---

## §6 报告输出格式（MA 报告必含 9 段落骨架）

每份 MA1-MA4 审计报告必须包含以下 9 个段落（顺序固定，便于 mission driver 与 closure audit 自动核验完整性）：

| # | 段落 | 内容要求 |
|---|------|---------|
| 1 | **需求契约原文** | 该切片/工作项覆盖的 UC 编号 + 标题 + 验收标准原文（逐字引用，§1 L1 格式） |
| 2 | **实现证据** | 代码路径引用（§1 L3 格式，`file#method` 方法锚点 + 关键行为断言），含跨域调用链 |
| 3 | **测试证据** | 测试断言引用（§1 L4 格式），注明断言强度 |
| 4 | **运行时行为证据** | 运行时行为证据（§1 L5 格式）；既有 MA2 报告已证实行为直接引用 |
| 5 | **符合性结论** | 五级追踪矩阵 + 每 UC 的符合性结论（P0/P1/P2/接受，§2 判据） |
| 6 | **与 arm-index 衔接** | 本报告 finding 与 arm-index 既有 finding 的"复用 or 新增"裁决（§7 规则） |
| 7 | **静态存疑点清单** | 该切片的 L5 存疑点（供 MA4 展开），每存疑点一行；无存疑点注明"无" |
| 8 | **过程纪律自检段** | checker 退出码门控核查 + closure-audit 独立性声明 + 与 arm-index 交叉去重声明（§9 模板） |
| 9 | **与 MA2 报告差异增量声明** | MA1 切片报告开头声明"与既有 MA2 报告（`docs/audits/2026-07-2*-arm-ma2-*`）的差异增量"——复用其已证实行为，只补需求视角差异（§去重协议） |

### 段落完整性自检（报告产出前强制）

报告产出 agent 在落盘前必须自查 9 段落全部存在。closure audit（MV V.3）将核验此完整性——缺失任一段落即 `needs revision`。

---

## §7 与 arm-index 命名衔接

### Finding 命名规则

本审计新建 finding 使用 **`P1-RC-xxx` 系列**（RC = Requirement Compliance），与 audit-remediation 的 `P1-MA2-xxx` / `P1-MA3-xxx` 系列区分：

- 格式：`P<级别>-RC-<序号>`，如 `P0-RC-001`、`P1-RC-012`。
- 序号在本审计内连续，ID 在报告产出时分配、写入 arm-index 后不可变。
- **不与既有 audit-remediation finding ID 冲突**（前缀 RC 区分）。

### "复用 or 新增"裁决规则

每份报告产出 finding 前，**必须 grep arm-index 同域同控制点后给出裁决**（禁止未经比对直接新建）：

| 情形 | 裁决 | 操作 |
|------|------|------|
| **同根因同控制点** | **复用**既有 finding ID | 在既有 arm-index 行追加 RC 交叉引用注记（如 `P1-MA2-001` 行追加"RC 视角复核：见 P1-RC-xxx 报告"），**不新建编号** |
| **新根因 / 新功能点 / 新维度** | **新建** `P1-RC-xxx` | 在报告中列明与既有 finding 的差异依据（根因不同点 / 控制点不同点 / 维度不同点） |

### 双向可追溯要求

- **新 finding 入 arm-index**：每条 `P1-RC-xxx` 产出即写入 arm-index 对应分区（MA1 finding 入 MA1 报告清单，MR0/MR1 修复入修复追踪区）。
- **修复行引用 finding**：MR0 的 R0.n / MR1 的 RC-R1.n 修复行须含 finding ID 交叉引用。
- **finding 引用修复**：arm-index 的 finding 行在修复完成后回填修复状态（对齐 audit-remediation 归档纪律）。
- **MV V.3 校验**：closure audit 核验全部 P0/P1 finding 的修复状态均为 `done` 或显式 successor。

---

## §8 审计过程纪律自检段（每份 MA 报告必含）

> 本段对应 roadmap 横切关注点 #8（审计过程自身纪律）。吸取 R6.9 closure 误判教训（`project-context.md §已知失败模式`）。

### 自检段模板（每份 MA 报告段落 8 必含）

```markdown
## 8. 过程纪律自检

- [ ] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [ ] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [ ] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决，无未经比对直接新建的 finding。
```

### checker 退出码门控核查要点（吸取 R6.9 教训）

R6.9 closure 误把 checker 脚本退出码（纯 reporter，恒 0）当作门控通过，致 compliance 漂移累积。本方法论强制：

- **checker 脚本 = 纯 reporter**：`nop-compliance-checker.sh` 退出码恒为 0，不反映 actual vs baseline（`compliance-baseline.md §回归门控规则` option b）。
- **真正门控 = CI workflow**：`.github/workflows/compliance.yml` 的 `Enforce baseline gate` step（python 解析 actual > baseline => `sys.exit(1)`）。
- **报告自检纪律**：MA 报告段落 8 须**实际运行 checker 并附 actual vs baseline 汇总表**，**不**以脚本退出码 0 作为门控通过依据；若本审计无生产代码变更（纯审计报告），注明"本报告无生产代码变更，checker 无回归风险"。

---

## §9 真相源冻结条款

> 防重蹈讨论文档 §根因分析 第 2 条教训（"真相源在审计过程中被自身修订"：P1-MA3-062 MR5 用伪概念豁免 D-mutation）。

### 冻结规则

**审计期间（MA1-MA4 + MR0/MR1 执行期），以下真相源的"需求契约"修订须经人工批准并登记：**

- `docs/requirements/product-scope.md`
- 各域 `docs/design/<domain>/use-cases.md`
- 各域 owner doc 的需求契约段落（机制/状态机/跨域契约的定义段落）

### 修订流程

1. **审计发现的 doc 分歧记入报告，不直接改真相源**：MA 报告段落 5/6 记录分歧，段落 8 声明"未修改真相源"。
2. **真相源修订须人工批准**：任何审计 agent 不得直接修改上述真相源文件的需求契约段落。修订须经人工批准 + 登记（变更理由 + 影响面 + 批准人）。
3. **需求变更非降级**：经人工批准的 product-scope 修订是**需求变更**（如范围裁剪、优先级调整），**非方案 B 降级**（不用于关闭 P0/P1 finding）。
4. **owner doc 的设计参考段落**：owner doc 的机制说明段落（非需求契约）可与代码协同修订，但状态机/跨域契约定义段落的修订须登记（避免"伪概念豁免"重演）。

### 与 §4 的关系

§4 定义真相源层级与冲突裁决（冲突时以需求真相源为准）；本条款定义审计期间真相源的修订门控（冻结 + 人工批准）。两者共同防止"真相源被自身修订"根因。

---

## §10 MR0 即时通道 + MR1 展开器机制声明

### MR0 即时通道（P0 发现即止血）

**触发**：MA1-MA4 任何工作项执行中发现 P0（活跃数据破坏 / 会计错误 / 安全漏洞）。

**机制**（对齐 audit-remediation P0-MA2-016/017/019/020 即时通道先例）：

1. 发现 P0 的审计 agent **不等整个里程碑完成**，立即创建独立 fix plan（`docs/plans/YYYY-MM-DD-HHmm-rc-fix-P0-RC-xxx.md`）。
2. 向 MR0 里程碑表追加实体行（编号 R0.1, R0.2...，含 finding ID / 域 / 修复范围 / Skill / 触及保护区域标注）。
3. mission driver 按追加行正常执行（DRAFT_PLANS → 独立草案审查 → EXECUTE → 独立结束审计 → done）。
4. **触及保护区域的 P0 行仍须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议，P0 即时通道不豁免）。
5. 修复后回写 MA 报告状态 + arm-index finding 修复状态。

**P0 不得留到 MR1 批量修复**——即时通道是 P0 的唯一合法修复路径。

### MR1 展开器机制（R1.0 → RC-R1.n，对齐 audit-remediation R*.0 范式）

**触发**：MA1-MA4 全部完成后，R1.0 展开器读取全部 P0/P1 需求分歧。

> **⚠ 人工裁决覆盖自动展开时机（2026-08-07 批准 / 2026-08-08 生效）**：依据 `docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md` §5/§6 Q1 裁决——**MA1-MA4 完成后 R1.0 不自动启动，维持 `todo`，待另行人工裁决**。mission driver 在 MA4 done 后到达本触发点时，按 Q1 裁决**跳过自动展开 R1.0**，不创建 RC-R1.n 行，直到人工明确启动（当前状态属「已批准的不自动启动」，非 bug）。

**机制**：

1. **R1.0 = 展开器工作项**（非容器行 / 非预注册固定范围）：读取 MA1-MA4 报告的全部 P0/P1 finding → 汇总、排序 → 向 MR1 表追加实体行。
2. **实体行命名**：`RC-R1.1, RC-R1.2...`（前缀 **RC** 避免与 audit-remediation 的 R1.x 系列混淆）。每行含：finding ID 交叉引用 / 域 / 修复范围 / 触及保护区域标注 / Skill。
3. **展开完成判据**：全部实体行已追加后 R1.0 标记 `done`（同 A4.1/A4.2 的 done 判据 = 展开完成，而非"全部修复完成"）。
4. **后续 RC-R1.n 各自独立 plan + 独立 done**：mission driver 逐项自动执行（DRAFT_PLANS → 独立草案审查 → EXECUTE → 独立结束审计 → done），**无需人工逐个介入**（触及保护区域者除外，按 §5 暂停协议）。
5. **禁止方案 B 关闭**（Q4 已裁决=(a)，无例外）：RC-R1.n 修复行**必须实现**，不得用 documented simplification / Deferred 关闭。

**不预注册占位行**：MR1 表**不预注册 RC-R1.n 占位行**（避免"R*.x 占位符卡死"反模式，对齐 audit-remediation 修法）。R1.0 展开时才追加实体行。

---

## §去重协议（与既有 audit-remediation 审计协作）

> 对应 roadmap MA1 §与既有审计逐维度去重。本审计不复跑 MA1-MA7，按本协议与既有审计协作。

### MA1 ↔ 既有 MA2 行为审计去重

新 MA1 的"运行时行为证据"维度（L5）与既有 MA2 状态机/业财链路行为审查**对象重叠**。去重协议：

1. **既有 MA2 报告已证实行为作为既有证据输入**：`docs/audits/2026-07-2*-arm-ma2-*` 报告已证实的状态机迁移/链路行为，MA1 直接引用（矩阵 L5 注明"行为已证实，引用 MA2 报告"），**不重新核实行为本身**。
2. **MA1 只补"需求契约↔实际行为"差异**：MA1 视角 = use-case 验收标准视角；既有 MA2 视角 = 状态机/链路行为视角。MA1 只补既有 MA2 未覆盖的"需求契约↔行为"差异。
3. **每切片报告开头声明差异增量**：报告段落 9（§6）声明"与 MA2 报告的差异增量"。
4. **新 finding 按衔接规则入 arm-index**：§7 "复用 or 新增"裁决。

### MA4 ↔ A5.6 边界（已定方向，非阻塞）

- **MA4**：审"行为是否符合需求"（需求契约视角）。
- **A5.6**（audit-remediation）：审"E2E 断言强度"（测试质量视角）。
- 判据不同，按此边界执行，Q5 非阻塞，MA4 不设门控。

### MA2（本 mission）↔ MA3（audit-remediation）边界

- **本 mission MA2**：方案 B 关闭项复查（需求契约视角，与 product-scope 对照）。
- **audit-remediation MA3**：owner doc vs code drift（文本一致性视角）。
- 不重复：本 mission MA2 不重做文本一致性，只做需求契约对照。

---

## §归档规范

### 报告命名

本审计报告统一使用 **`rc` 前缀**（Requirement Compliance），与 audit-remediation 的 `arm` 前缀区分：

```
docs/audits/YYYY-MM-DD-HHmm-rc-<milestone>-<slice>.md
```

示例：
- `docs/audits/2026-08-05-0900-rc-ma1-a1-1-finance-f1-posting.md`（MA1 切片 A1.1）
- `docs/audits/2026-08-06-1400-rc-ma2-a2-1-finance-protected.md`（MA2 A2.1）
- `docs/audits/2026-08-07-0800-rc-fix-p0-rc-001.md`（MR0 P0 即时修复）

### 归档纪律

1. **报告产出即更新 arm-index**：每份报告产出后，执行 agent 同步更新 `docs/audits/arm-index.md`（新增 RC finding 入对应分区）。
2. **修复完成即回填 arm-index**：MR0/MR1 修复 plan 完成后，在 arm-index 对应行的"修复状态"列回填 `done`。
3. **MV V.3 校验索引完整性**：closure audit 核验全部 P0/P1 RC finding 的修复状态均为 `done` 或显式 successor。
4. **既有 audit-remediation 文件不动**：`docs/audits/` 下非 `rc-` 前缀的 arm 文件是历史审计，本审计不修改。

---

## §自检清单（方法论文档产出前强制）

对照以下清单，任何一项不满足回到对应段落修订：

### 内容完整性自检
- [x] §1 五级追踪矩阵模板（5 列 + 每级证据格式 + 判读标准）
- [x] §2 P0/P1/P2/接受 分级判据表（4 行 + Q4 修复义务绑定列）
- [x] §3 完整枚举纪律（反例表 + 完成判据）
- [x] §4 Q1 真相源层级与冲突裁决规则（层级表 + 冲突规则 + "显式人工批准记录"三判据）
- [x] §5 Q4 修复义务 + 保护区域暂停协议（修复义务表 + 三类 ask-first 门控 + 暂停协议 + 预授权清单）
- [x] §6 报告输出格式（9 段落骨架 + 完整性自检）
- [x] §7 与 arm-index 命名衔接（P1-RC-xxx 命名 + "复用 or 新增"裁决规则 + 双向可追溯）
- [x] §8 审计过程纪律自检段（checker 退出码门控核查 + closure-audit 独立性 + 交叉去重声明）
- [x] §9 真相源冻结条款（冻结规则 + 修订流程 + 与 §4 关系）
- [x] §10 MR0 即时通道 + MR1 展开器机制声明（双机制 + 不预注册占位行）

### 裁决对齐自检
- [x] Q1=(c) 已体现（§4 逐项对照，需求真相源优先）
- [x] Q4=(a) 已体现（§2/§5 强制实现禁方案 B，无例外通道，唯一出口=需求变更）
- [x] Q2 完整枚举已体现（§3 禁止抽样）
- [x] 保护区域暂停协议可无人值守操作（§5 driver 暂停触及行，非触及行继续）

### 去重协议自检
- [x] MA1 ↔ 既有 MA2 去重协议已定义（§去重协议）
- [x] MA4 ↔ A5.6 边界已定义（§去重协议）
- [x] MA2(本) ↔ MA3(audit-remediation) 边界已定义（§去重协议）

### 机制对齐自检
- [x] MR0 即时通道对齐 audit-remediation P0 先例（§10）
- [x] MR1 R1.0 展开器对齐 audit-remediation R*.0 范式（§10）
- [x] checker 退出码门控核查区分门控 vs reporter（§8，吸取 R6.9 教训）
- [x] Finding 命名 P1-RC-xxx 与 audit-remediation 区分（§7）
