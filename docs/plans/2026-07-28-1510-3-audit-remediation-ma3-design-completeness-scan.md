# 2026-07-28-1510-3-audit-remediation-ma3-design-completeness-scan MA3 设计完整性扫描（A3.2）

> Plan Status: completed
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA3（工作项 A3.2）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.3「设计完整性扫描」行（MA3）；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/design-completeness-scan-prompt.md`（审计方法——前瞻性缺口扫描 7 维度含开源对标维度 0）；`docs/requirements/product-scope.md`（目标产品范围基准）；`docs/analysis/erp-survey/` + `docs/analysis/2026-06-30-1200-feature-coverage-matrix.md`（开源对标基准）；`docs/plans/2026-07-28-1510-2-audit-remediation-ma3-design-doc-baseline.md`（A3.1 已有文档质量审查——本扫描找"缺失的内容"，与 A3.1 互补）
> Audit: required

## Current Baseline

设计完整性扫描（文档-实现一致性层 MA3 第二项，A3.1 的前瞻性对应）。`design-completeness-scan-prompt.md` 明确分工：**A3.1 审计存在的内容；A3.2 查找缺失的内容**。即使已有文档质量完美，也无法掩盖从未被设计的整个功能/文档/功能点。此前多轮审计聚焦已有文档间的内部一致性，**未系统性地对照开源 ERP 功能清单做缺失检测**——这是"多次审计仍有大量遗漏"的根本原因（维度 0 强制补上这一环）。

目标产品范围（`docs/requirements/product-scope.md`）声明：产品化通用 ERP，内置 18 业务域（核心 5 进销存+财务 / 第一批扩展 5 资产-项目-制造-质量-维护 / 第二批扩展 8 外围协作域 crm-cs-hr-aps-contract-drp-logistics-b2b），覆盖中等规模 ERP 的进销存+财务一体化+制造全链及外围协作域，可按需裁剪组装。

实时仓库 `docs/design/` 已有覆盖（待扫描）：

- **18 域目录全部存在**：master-data / inventory / purchase / sales / finance / assets / projects / manufacturing / quality / maintenance / crm / customer-service / human-resource / aps / contract / drp / logistics / b2b（README.md 为基线；状态机重的域有 state-machine.md；跨域复杂的域有 cross-domain.md；purchase 有 three-way-match/returns；finance 有 posting/period-close/ar-ap-reconciliation/multiple-accounting-schemas/costing-methods/budget/gl-mapping-rules）。
- **portal 为 future extension placeholder**（`portal/README.md` STATUS 横幅标注非当前基线）。
- **`feature-coverage-matrix.md`** 已有此前识别的覆盖率差距清单（部分标记 ✅ 已覆盖 / ❌ 未覆盖 / 🕒 待设计）。
- **`erp-survey/`** 已有 16 开源 ERP + 7 补充项目源码实测调研（survey-index 速查导航+对比矩阵+选型建议 / business-design-takeaways 9 大主题业务设计参考）。

**但从未做过一次对照开源 ERP 功能清单的系统性设计完整性扫描**。已知未核验控制点（design-completeness-scan 7 维度，含维度 0 开源对标）：

- **维度 0 开源功能对标（核心，此前遗漏的根本原因）**：遍历 `erp-survey/survey-index.md` 提取每个开源项目（Odoo/ERPNext/Metasfresh/iDempiere/Axelor 等）的关键差异化功能，逐项检查 `docs/design/` 是否有对应 owner doc。**特别注意**：不只查"域存在"（如"CRM 域存在"），要查**功能深度**（如 CRM lead scoring / ERPNext 销售预测 / Axelor 替代工艺路线 / Odoo subscription billing 等开源标配逐项核对）。矩阵已标 ❌/🕒 的标记 blocker/major；矩阵已标 ✅ 的快速交叉验证。
- **维度 1 域覆盖**：范围隐含的每业务域是否有 `docs/design/<domain>/` 目录；范围要求但无设计所有者的缺失域；作为代码/待办存在但无设计文档的域。
- **维度 2 每域内文档覆盖**：每域至少 README.md；工作流密集域是否有 state-machine.md 且遵循 state-machine-business-review-prompt 10 审查维度；跨域耦合密集域是否有 cross-domain.md；README 唯一但业务复杂需更多文档的域。
- **维度 3 每文档内功能点覆盖**：域 README 核心业务对象业务含义；状态机是否涵盖 10 维度（状态定义/转换完整性/终端恢复/异常路径/可达性/角色权限/外部依赖/TODO 策略/场景演练/设计文档一致性）；跨域流程是否描述或路由；保护区域行为是否定义或路由。
- **维度 4 跨域流程覆盖**：flow-overview.md L1 宏流程是否涵盖范围要求的端到端路径；L2 状态机映射是否引用所有存在的域状态机；L3 跨域规则是否涵盖需要它们的每个域对（过账触发/库存可用性/快照语义/多货币/对账）。
- **维度 5 术语表与角色覆盖**：domain-glossary.md 是否涵盖新域文档引入的术语；roles-and-permissions.md 是否涵盖每域状态机转换隐含的角色（资产经理/项目经理/质量检查员/维护技术员等）。
- **维度 6 与范围一致性**：设计的域和功能集 vs product-scope.md + 路线图；无设计所有者且无显式推迟的范围隐含功能；描述当前范围之外行为的设计文档（设计蔓延）。

剩余差距：需要一次系统性设计完整性扫描，发现任何 blocker/major（**范围要求的域无设计所有者** / **工作流密集域缺状态机** / **保护区域行为未定义** / **开源标配核心功能（功能深度）在设计中完全缺失** / **跨域流程不完整** / **术语表/角色覆盖滞后新域**）登记为 P1（文档类 P1 目标 MR2）走 MR2 批量修复。扫描为文档层，原则上不产生 P0；若发现范围要求的域已有代码但零设计（实现正在无设计真相下进行），升级标注交接 A4 代码质量审计。

## Goals

- 按 `design-completeness-scan-prompt.md` 主动扫描 `docs/design/` 查找缺失的域/文档/功能点，对照目标产品范围 + 开源 ERP 基准生成优先级差距列表，产出审计报告。审查维度：维度 0 开源功能对标（功能深度）+ 维度 1 域覆盖 + 维度 2 每域文档覆盖 + 维度 3 每文档功能点覆盖 + 维度 4 跨域流程覆盖 + 维度 5 术语表角色覆盖 + 维度 6 与范围一致性。
- **功能深度而非仅域存在**：逐项核对开源 ERP 普遍存在的功能点（lead scoring / 销售预测 / 替代工艺路线 / subscription billing 等），将"域存在但功能深度不足"标记为 major。
- **驱动下一轮设计文档添加**：产出建议的下一轮文档添加（带建议路径的优先级列表）。
- scope matrix §2.3「设计完整性扫描」行终态标记（`❓` → `✅`/`⚠️(P1)`）。
- 发现的 blocker/major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总（目标 MR2）。roadmap A3.2 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做已有文档**质量**审查 — 归 A3.1（`design-doc-audit-prompt.md`）。本扫描（`design-completeness-scan-prompt.md`）找"缺失的内容"，A3.1 审"存在的内容"质量。两者互补不重叠（各自 skill 已明确分工）。对已有文档的质量（措辞/自洽/owner 边界）只标注明显问题，不深入。
- **不**做 owner doc vs 代码 drift 逐行核对 — 归 A3.3-A3.5。本扫描核验设计**覆盖广度**（哪些功能从未设计），不做设计 vs Java/ORM 逐字段比对。
- **不**做 API 契约 vs 实现一致性 — 归 A3.6；不做索引路由有效性 — 归 A3.7；不做可定制性验证 — 归 A3.8。
- **不**做状态机正确性裁决 — 归 MA2（A2.5-A2.15 已 done）。维度 3 只核验状态机文档是否**存在**且**涵盖 10 审查维度结构**，不重做迁移完整性/守卫/并发正确性裁决。
- **不**在本计划内批量补建缺失设计文档 — finding 经 R2.0 展开机制进入 MR2 批量补建。本扫描只识别缺口 + 建议路径，不实现文档。
- **不**手改生成物或 ORM。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/requirements/product-scope.md`（目标产品范围基准）；`docs/design/README.md`（设计文档结构约定）；`docs/design/app-overview.md` + `flow-overview.md` + `domain-glossary.md` + `feature-inventory.md`；`docs/design/` 下全部现有文件（含子目录）；`docs/backlog/README.md` + 路线图；`docs/analysis/erp-survey/`（survey-index + business-design-takeaways）；`docs/analysis/2026-06-30-1200-feature-coverage-matrix.md`（此前覆盖率差距清单）
- Skill Selection Basis: `design-completeness-scan-prompt.md`（roadmap A3.2 指定此 skill，前瞻性设计缺口扫描专用方法——7 维度含维度 0 开源对标（此前多轮审计遗漏的根本原因）+ 反模式表 + 严重性指南。项目定制化层见 `docs/skills/README.md`）。本扫描找"缺失的内容"，A3.1 审"存在的内容"，互补。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。缺失文档补建在 MR2 批量进行（本扫描只识别 P1 + 建议路径）。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为纯文档扫描，不构建/不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。文档扫描无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - 设计完整性系统性扫描（7 维度含开源对标维度 0）

Status: completed
Targets: `docs/design/` 全树（域目录 + 全局文档 + 跨域模式）；`docs/requirements/product-scope.md`；`docs/analysis/erp-survey/` 下的 survey-index 与 business-design-takeaways（实际文件名带 `2026-06-22-0000-` 日期前缀）；`docs/analysis/2026-06-30-1200-feature-coverage-matrix.md`
Skill: `design-completeness-scan-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）。本审计为 MA3 第二项，仅依赖 0.3，可与 A3.1 同批推进（A3.1 审存在的内容，A3.2 找缺失的内容，无相互依赖）。

- [x] 维度「0 开源功能对标（核心）」：遍历 `erp-survey/survey-index.md` 提取每开源项目（Odoo/ERPNext/Metasfresh/iDempiere/Axelor 等）关键差异化功能，逐项检查 `docs/design/` 是否有对应 owner doc。矩阵已标 ✅ 快速交叉验证；矩阵已标 ❌/🕒 标记 blocker/major。**功能深度**核对（CRM lead scoring / ERPNext 销售预测 / Axelor 替代工艺路线 / subscription billing 等开源标配逐项）。
      - Skill: `design-completeness-scan-prompt.md`
- [x] 维度「1 域覆盖」：列出 product-scope 隐含的每业务域，标记是否存在 `docs/design/<domain>/` 目录；标记范围要求但无设计所有者的缺失域；标记作为代码/待办存在但无设计文档的域（实现正在无设计真相下进行）。
      - Skill: `design-completeness-scan-prompt.md`
- [x] 维度「2 每域内文档覆盖」：每现有域目录检查至少 README.md；工作流密集域是否有 state-machine.md 且引用 state-machine-business-review-prompt；跨域耦合密集域是否有 cross-domain.md；标记 README 唯一但业务复杂需更多文档的域。
      - Skill: `design-completeness-scan-prompt.md`
- [x] 维度「3 每文档内功能点覆盖」：每域 README 核心业务对象业务含义；状态机是否涵盖 10 审查维度结构（状态定义/转换完整性/终端恢复/异常路径超时并发幂等/可达性/角色权限/外部依赖/TODO 策略/场景演练/设计文档一致性）；跨域流程描述或路由；保护区域行为（支付/退款/数据删除/会计过账/权限更改）定义或路由。**仅核验结构存在，不重做 MA2 正确性裁决**。
      - Skill: `design-completeness-scan-prompt.md`
- [x] 维度「4 跨域流程覆盖」：flow-overview.md L1 宏流程是否涵盖范围要求的端到端路径；L2 状态机映射引用所有存在的域状态机；L3 跨域规则涵盖需要它们的每个域对（过账触发/库存可用性/快照语义/多货币/对账）。
      - Skill: `design-completeness-scan-prompt.md`
- [x] 维度「5 术语表与角色覆盖」：domain-glossary.md 是否涵盖新域文档引入的术语；roles-and-permissions.md 是否涵盖每域状态机转换隐含的角色（资产经理/项目经理/质量检查员/维护技术员等）。
      - Skill: `design-completeness-scan-prompt.md`
- [x] 维度「6 与范围一致性」：设计的域和功能集 vs product-scope.md + 路线图；标记无设计所有者且无显式推迟的范围隐含功能；标记描述当前范围之外行为的设计文档（设计蔓延）。
      - Skill: `design-completeness-scan-prompt.md`
- [x] 产出审计报告 `docs/audits/2026-07-28-1510-arm-ma3-design-completeness-scan.md`（含：7 维度逐项扫描结果、域覆盖摘要[设计的 vs 范围要求的 vs 推迟的]、每域文档覆盖摘要、状态机覆盖摘要[哪些域有遵循 10 维度的状态机]、跨域流程覆盖摘要、术语表角色覆盖摘要、blocker/major/minor/note finding 清单[每个含严重性/维度/受影响区域/差距描述/建议操作/建议文档路径]、**建议的下一轮文档添加优先级列表**、裁决完整/有差距/有阻塞、剩余风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] 7 维度逐项扫描结果产出（每维度至少一句结论，含"本维度无缺口"）
- [x] 域覆盖摘要 + 状态机覆盖摘要 + 跨域流程覆盖摘要 + 术语表角色覆盖摘要产出
- [x] blocker/major/minor/note finding 清单产出，每个含严重性/维度/受影响区域/差距描述/建议操作/建议文档路径
- [x] 建议的下一轮文档添加优先级列表产出（驱动 MR2 补建）

### Phase 2 - finding 汇总交接 MR2 + 索引/矩阵更新

Status: completed
Targets: 设计完整性扫描 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.3「设计完整性扫描」行
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] finding 汇总：全部 blocker/major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA3-NNN`、报告、描述、目标 MR2、修复状态 todo）。文档类 P1 目标 MR2（依赖 MA3+MA4 done，由 R2.0 展开机制转化为具体补建工作项行）。
      - Skill: none
- [x] 若发现范围要求的域已有代码但零设计（实现正在无设计真相下进行），升级标注并交接 A4 代码质量审计（该域代码可能在无设计 owner 下实现）。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.3「设计完整性扫描」行终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 blocker/major 已登记 arm-index §P1 汇总（目标 MR2），待 R2.0 展开
- [x] 范围要求域零设计的情况已标注交接 A4
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05869a7c1ffeOufF73E81fBeWE`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：roadmap A3.2 = `todo`（line 79），Owner Doc/Skill/Deps 匹配 ✓；skill 维度 0 开源对标声明真实（skill lines 32-38 "此前多轮审计…未系统性地对照开源 ERP 功能清单"）✓；product-scope 声明 18 域 ✓；18 域目录实仓存在 ✓；feature-coverage-matrix.md + erp-survey/（42 文件）存在 ✓；survey-index/business-design-takeaways 实际文件名带 `2026-06-22-0000-` 日期前缀已采纳入 Targets 注记 ✓；与 A3.1 互补分工双向一致 ✓；Non-Goals 明确排除 A3.1/A3.3-A3.5/A3.6-A3.8/MA2 ✓；R1/R2/R4/R7/R8/反松弛全 PASS；结构匹配参考 A2.17（诚实适配：文档层审计原则上无 P0 即时通道，与 A3.1 sibling + roadmap 设计一致）。Plan Status 转 active。

## Closure Gates

> 本计划主体是文档扫描（不改代码/文档）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。缺失文档补建在 MR2 批量进行，本扫描只识别 finding + 建议路径。

- [x] 范围内行为完成（A3.2 设计完整性扫描报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix 结论已反映）
- [x] 已运行验证：文档扫描无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）——`mvn clean install -DskipTests` BUILD SUCCESS（154 模块，01:30 min）+ `mvn test` BUILD SUCCESS（1768 tests，0 failures / 0 errors / 1 skipped，08:14 min）
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（详见 Closure 段审计证据；本审计为 fresh-context 独立子代理会话执行）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A3.1 已有文档质量审查（存在的内容质量）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本扫描找"缺失的内容"；A3.1 审"存在的内容"质量。对已有文档质量（措辞/自洽/owner 边界）只标注明显问题，不深入。
- Successor Required: `yes`——A3.1 执行时复核（同批起草）。

### A3.3-A3.5 owner doc vs 代码 drift（逐行核对）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本扫描核验设计**覆盖广度**（哪些功能从未设计），不做设计 vs Java/ORM 逐字段比对。逐行 drift 归 A3.3-A3.5。
- Successor Required: `yes`——A3.3-A3.5 执行时复核。

### MA2 状态机正确性（迁移/守卫/并发）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 状态机正确性裁决归 MA2（A2.5-A2.15 已 done）。维度 3 只核验状态机文档是否存在且涵盖 10 审查维度结构，不重做正确性裁决。
- Successor Required: `no`——状态机正确性 MA2 已收口。

## Closure

Status Note: 执行侧已完成（Phase 1 + Phase 2 全部 `[x]`；Closure Gates 全部 `[x]`）。独立结束审计由 fresh-context 子代理会话执行并通过（见下方 Closure Audit Evidence）。Plan Status = completed；arm-index/roadmap 已记 done。

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor subagent（fresh-context 新会话，非执行者上下文，对照实时仓库逐项复核）。本审计会话使用 Read/Grep/Glob/Bash 工具直接核验实时仓库状态，不依赖执行者声称。
- Audit Method: 对照 plan Phase 1+2 + Closure Gates + Goals/Non-Goals + 与 A3.1 去重正确性 + finding ID 无冲突，逐项核验实时仓库工件存在性与内容一致性。
- Evidence:
  - 审计报告：`docs/audits/2026-07-28-1510-arm-ma3-design-completeness-scan.md` 存在（480 行/40007 字节），13 节齐全（§0 裁决 + §1 维度 0 + §2 维度 1 + §3 维度 2 + §4 维度 3 + §5 维度 4 + §6 维度 5 + §7 维度 6 + §8 finding 清单 + §9 下一轮文档添加优先级列表 + §10 覆盖摘要 5 节 + §11 剩余风险/跳过区域 + §12 与 plan Exit Criteria 对照 + §13 范围内行为完成声明）。7 维度逐项扫描结果真实落地。
  - arm-index 更新：报告清单新增 1 行（line 46，`done`）；§P1 汇总新增 A3.2 完成声明段（line 67）；§P1 详细清单新增 P1-MA3-022/023 两行（lines 191-192，目标 MR2，`todo`，含完整字段：严重性/维度/受影响区域/差距描述/建议操作/建议文档路径）。
  - scope matrix §2.3 「设计完整性扫描」行终态：`❓` → `⚠️(P1)`（line 124，含完整完成段）。
  - finding 计数交叉核验：实时仓库 grep 确认 MA3 累计 P1=15（A3.1 13 = P1-MA3-001~013 + A3.2 2 = P1-MA3-022/023）/ P2=9（A3.1 8 = P2-MA3-014~021 + A3.2 1 = P2-MA3-022）/ P0=0。与 plan line 170 声明一致。
  - finding ID 无冲突：P1-MA3-014~021 为 A3.1 占用段；P1-MA3-022/023 为本批次新增；P2-MA3-022 为本批次新增；编号无碰撞、无重复登记。
  - 与 A3.1 去重裁定正确性：审计报告 §8.4 协同矩阵 + line 191-192 finding 描述均显式标注与 A3.1 P1-MA3-009（dim 6）/P1-MA3-010（dim 9）同根因不同维度投影（dim 4 + dim 5），正交不重复。
  - 验证基线：与 plan Non-Goals 一致——未手改任何 ORM/generated 代码；纯文档审计（仅 docs/audits + docs/backlog + docs/logs + 计划本身变更）。
  - 与 plan Goals 一致：7 维度逐项扫描完成 + 域覆盖/状态机覆盖/跨域流程覆盖/术语表角色覆盖四摘要产出（审计报告 §10.1-§10.5）+ finding 清单（§8）+ 下一轮文档添加优先级列表（§9，驱动 MR2）+ scope matrix 终态标记（§2.3 `⚠️(P1)`）+ finding 登记 arm-index §P1 汇总。
  - 与 plan Non-Goals 一致：未做 A3.1 质量审查（仅 §8.4 + finding 描述交叉引用）/ 未做 A3.3-A3.5 drift（§11.2 跳过区域声明）/ 未做 A3.6-A3.8 / 未手改生成物或 ORM（audit-only 变更集已核验）。
  - 五点一致性核验：Plan Status（completed）/ Phase 1 Status（completed）/ Phase 2 Status（completed）/ Phase 1 Exit Criteria（4/4 `[x]`）/ Phase 2 Exit Criteria（3/3 `[x]`）/ Closure Gates（8/8 `[x]`）/ docs/logs/2026/07-28.md（全条目）/ arm-index（done）/ scope matrix（`⚠️(P1)`）/ roadmap A3.2（done）—— 全部一致。
  - Deferred honesty 核验：3 个 Deferred But Adjudicated 项均为明确 out-of-scope 且 Successor Required 显式声明（A3.1 yes / A3.3-A3.5 yes / MA2 no），无范围内 P0/P1 隐藏为 deferred/follow-up。
  - Anti-Hollow 核验：本计划为 audit-only（无可执行代码生成），可观察产物（审计报告 + arm-index + scope matrix 三工件）均有真实内容非占位。
- Audit Verdict: **APPROVED**——本计划 Phase 1+2 全部交付物经实时仓库逐项核验落地且与 plan Goals/Non-Goals/Exit Criteria 一致，finding 计数与去重裁定正确，无 P0/P1 隐藏为 deferred，可标记 `Plan Status: completed` 关闭。本审计为独立结束审计（fresh-context 子代理新会话），非执行者自审；本 `[x]` 由独立审计员勾选，非占位符。

Follow-up:

- ~~独立子代理 closure audit（新会话，fresh-context，对照实时仓库逐项复核本计划 Phase 1+2 + Closure Gates + 与 plan Goals/Non-Goals 一致性 + 与 A3.1 去重正确性 + finding ID 无冲突）~~ — **已完成**（见 Closure Audit Evidence，verdict APPROVED）
- MR2 协同修复：P1-MA3-022（flow-overview §3）+ P1-MA3-023（domain-glossary）应与 A3.1 P1-MA3-009/010 协同一并扩展（8 第二批扩展域"全局视图"系统性缺位在 dim 4+5+6+9 四联投影），避免分散修复漂移
- P2-MA3-022 subscription billing 深度拆解：subscription 客户触发时按需补建
- NOTE 现场服务/POS：随需求触发或显式声明产品基线外
