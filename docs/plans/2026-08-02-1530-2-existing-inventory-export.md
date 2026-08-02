# 2026-08-02-1530-2 existing-inventory-export 存量清单导出（方案 B / successor 三源对账）

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: 0.3（M0 审计编排基线 — 存量清单导出）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item 0.3
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 已 done，解除 0.3 阻塞）
> Audit: required

## Current Baseline

- **方法论契约已就绪**：`docs/audits/requirement-compliance-methodology.md` 已落盘（M0.1 done）。§2 分级判据 + §4 "显式人工批准记录"三判据证据标准 + §7 arm-index 衔接 + §去重协议 为 MA2/MA3 定义了复查口径；0.3 为 MA2/MA3 提供**待复查全集清单**。
- **arm-index 是首要源（源 1）**：`docs/audits/arm-index.md` 共 587 行，含 P0/P1/P2 finding 详细清单表，每行 `resolved` 注记携带**关闭方式标签**——`方案 B 裁决（documented simplification）` / `documented simplification` / `Deferred` / `resolved (R*.n done)`（实现修复项）/ `fixed`（实现修复项）。示例：P1-MA2-001 GRNI 冲回 = `方案 B 裁决（documented simplification）`；P1-MA2-018 年初余额 = `documented simplification`；P1-MA2-022 FX 重估 = `documented simplification`；P1-MA2-002 多币种 = `fixed`（方案 A 实现，属实现修复项，0.3 **排除**）。
- **owner doc 内嵌声明（源 2）**：部分 owner doc 含显式 documented simplification / Deferred 段落，且其批准来源需可追溯（方法论 §4 三判据 (ii)）。示例：`docs/design/finance/period-close.md §已知简化`（年初余额/FX 重估/反结账审批 successor）、`docs/design/purchase/returns.md §暂估应付冲减`、`docs/design/manufacturing/mrp.md §实现偏离补注`。这些内嵌声明不一定有独立 arm-index 行，0.3 须**三源对账**避免遗漏。
- **backlog/README 既有追踪（源 3）**：`docs/backlog/README.md` 共 148 行，其中 **81 行**含 successor/deferred 提及。successor 计数（roadmap 称 arm-index 内嵌声明数 41）与 backlog 追踪存在口径差异（含叙述性提及），0.3 须对账消歧，修正计数口径。
- **导出口径（roadmap M0.3 详情，关键）**：按 arm-index 各行 resolved 注记的**关闭方式标签**筛行——**保留** `方案 B 裁决（documented simplification）` / `documented simplification` / `Deferred` 标签行；**排除** `resolved (R*.n done)` / `fixed`（实现修复项）。示例 ID 仅作锚点，精确清单以导出为准。
- **分区约束（roadmap MA2，关键）**：每个 finding ID 恰好归属**一个** A2.x 行（跨域项按主域归属并显式标注），0.3 导出后脚本校验分区不重叠。MA2 表 A2.1-A2.9 按域分区（A2.1 finance 会计保护区域 / A2.2 finance 非保护区域 / A2.3 mfg / A2.4 hr / A2.5 purchase+sales / A2.6 assets+inventory / A2.7 projects+quality / A2.8 扩展域 / A2.9 跨域）。
- **复杂度分级输入**：roadmap（§M0 详情 line 256）要求"复杂度分级复用 arm-scope §1.2"。"arm-scope" 即 `docs/audits/audit-remediation-scope-and-dimension-matrix.md`（注意：`docs/architecture/arm-scope.md` **不存在**，"arm-scope" 是该矩阵文件的昵称）。其 §1.2 复杂度分级按**域**评级为 S/A/B/C（S=finance/mfg/hr；A=assets/purchase/sales/quality/crm/projects/cs/contract/b2b/inventory；B=master-data/maintenance/drp；C=logistics/notify/aps），每条方案 B 项/successor 项按其所属域继承 S/A/B/C 等级。
- **既有脚本参考**：`docs/audits/scripts/` 含 `classify_mutations.py`、`cross-module-dep-extract.py`（解析范式参考）；0.3 可仿其解析思路但不强制复用。
- **保护区域**：本工作项为**只读提取 + 清单产出**，**不修改 arm-index / owner doc / backlog/README 任何内容**（§9 冻结条款 + 只读纪律）。属 roadmap 预授权类目（"文档更新类修复：预授权自动执行"——产出是新清单文件，非改既有真相源）。无 ask-first。
- **剩余差距**：方案 B 全集清单 + successor 三源对账清单 + A2.x 分区映射全部缺失 = MA2 A2.1-A2.9（Deps=0.2+0.3）与 MA3 A3.1-A3.5（Deps=0.3）的复查范围未定，全部阻塞。本计划解除 MA3（及 MA2 的 0.3 依赖）的 DRAFT_PLANS 阻塞。

## Goals

- 产出 **方案 B 关闭项全集清单**：从 arm-index 按 resolved 注记关闭方式标签导出（保留 documented simplification / 方案 B 裁决 / Deferred 标签行，排除 `resolved (R*.n done)` / `fixed` 实现修复项），每行含 finding ID / 域 / 关闭方式标签 / owner doc 锚点 / 复杂度分级 / 主分区（A2.x）。
- 产出 **successor 三源对账清单**：arm-index 行内 successor 声明 + owner doc 内嵌 successor/Deferred 声明 + backlog/README 81 行追踪三源对账，消歧计数（修正"41 = arm-index 内嵌数"口径），每行含触发条件摘要 / 是否已满足 / 当前归属（MA1/R1.0/backlog）。
- **分区映射校验**：每个方案 B finding ID 恰好归属一个 A2.x 行（跨域项显式标注主域归属），导出后校验分区不重叠、无遗漏。
- **复杂度分级**：复用 `docs/audits/audit-remediation-scope-and-dimension-matrix.md §1.2` 域复杂度分级（S/A/B/C），每条方案 B 项与 successor 项按所属域继承等级（跨域项按主域）。
- 按域与影响面排序，产出 MA2 A2.1-A2.9 与 MA3 A3.1-A3.5 可直接消费的**待复查全集**。
- 产出文件的"§导出口径自检"段声明筛行规则、排除项计数、三源对账差异，确保 MA2/MA3 执行者可复核导出完整性。

## Non-Goals

- **不复查方案 B 项是否正确**（"有意设计 vs 静默降级"裁决属 MA2 A2.1-A2.9 范围；0.3 只导出全集 + 分区，不评判）。
- **不复查 successor 触发条件是否该回队**（属 MA3 A3.1-A3.5 范围；0.3 只三源对账 + 记录现状）。
- **不修改 arm-index / owner doc / backlog/README**（只读提取；§9 冻结条款；发现的对账差异记入产出文件交 MA2/MA3，不回写源文件）。
- **不修改 product-scope / use-cases**（真相源冻结）。
- **不修改 ORM/api.xml/BizModel/Processor/view.xml**（纯清单产出）。
- **不执行 MA1-MA4 审计**（Deps 含 0.2/0.3，本轮不触及）。

## Task Route

- Type: `verification or audit work`（存量清单提取 + 三源对账，为下游审计提供待复查全集；非实现变更、非需求澄清）
- Owner Docs: `docs/backlog/requirement-compliance-roadmap.md`（M0.3 工作项 + Work Item Details + MA2/MA3 分区表）+ `docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §4 三判据证据标准 / §7 衔接 / §去重协议）+ `docs/audits/arm-index.md`（源 1）+ `docs/backlog/README.md`（源 3）+ `docs/audits/audit-remediation-scope-and-dimension-matrix.md §1.2`（域复杂度分级 S/A/B/C；注："arm-scope" 是该文件昵称，`docs/architecture/arm-scope.md` 不存在）
- Skill Selection Basis: `Skill: none`（无 opencode 技能匹配"从 markdown 提取清单 + 三源对账"。roadmap 为 0.3 指定"参考 `docs/audits/scripts/`"——这是解析范式参考，非工作方法选择器；0.3 的方法是"按 resolved 标签筛行 + 三源对账 + 分区映射"，由 roadmap M0.3 详情明确定义。）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 纯 markdown 解析与清单产出，无端口/环境变量/密钥/外部服务依赖。可选辅助脚本（仿 `docs/audits/scripts/` 解析思路）为纯文本处理，无运行时依赖。

## Execution Plan

### Phase 1 - arm-index 方案 B 关闭项导出 + 分区映射（源 1）

Status: completed
Targets: 产出文件 `docs/audits/rc-existing-inventory.md`（新建，方案 B 全集 + 分区 + 复杂度）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: M0.1 done（方法论 §4 三判据 / §7 衔接可用）

- [x] `Add` 按导出口径解析 `docs/audits/arm-index.md` 全部 finding 行：按 resolved 注记关闭方式标签筛行——**保留** `方案 B 裁决（documented simplification）` / `documented simplification` / `Deferred` 标签行；**排除** `resolved (R*.n done)` / `fixed`（实现修复项）；每保留行提取 finding ID / 域 / 关闭方式标签 / owner doc 锚点 / 复杂度
      - Skill: none
- [x] `Add` 按 roadmap MA2 表 A2.1-A2.9 分区为每条方案 B finding ID 映射主分区（A2.1 finance 会计保护区域 / A2.2 finance 非保护 / A2.3 mfg / A2.4 hr / A2.5 purchase+sales / A2.6 assets+inventory / A2.7 projects+quality / A2.8 扩展域 / A2.9 跨域）；跨域项按主域归属并显式标注跨域涉及域
      - Skill: none
- [x] `Proof` 校验分区完整性：每个方案 B finding ID 恰好归属一个 A2.x 行（无重叠、无遗漏），校验失败则在产出文件记录未分区项
      - Skill: none
- [x] `Add` 复用 `docs/audits/audit-remediation-scope-and-dimension-matrix.md §1.2` 域复杂度分级（S/A/B/C）对每条方案 B 项按所属域继承等级（跨域项按主域），落盘产出文件"§方案 B 全集清单"段（按 A2.x 分区 + 域 S/A/B/C + 影响面排序）
      - Skill: none
- [x] `Add` 产出文件"§导出口径自检"段：声明筛行规则（保留/排除标签）+ 保留项计数 + 排除项计数 + 未分区项计数（须为 0）
      - Skill: none

Exit Criteria:

- [x] 产出文件 `docs/audits/rc-existing-inventory.md` 存在，含"§方案 B 全集清单"段（每行 finding ID/域/关闭方式标签/owner doc 锚点/复杂度/A2.x 分区）
- [x] 分区校验通过：每个方案 B finding ID 恰好一个 A2.x 分区（产出文件"§导出口径自检"段记录未分区项 = 0）

### Phase 2 - successor 三源对账（源 1+2+3）

Status: completed
Targets: `docs/audits/rc-existing-inventory.md`（补 §successor 三源对账清单）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成（方案 B 全集已建立，successor 声明常与方案 B 项关联）

- [x] `Add` 提取 successor **源 1**：arm-index 行内 successor 声明（grep successor/触发条件关键词）
      - Skill: none
- [x] `Add` 提取 successor **源 2**：owner doc 内嵌 successor/Deferred 声明（如 `costing-methods.md:66` FIFO 红冲、`period-close.md §已知简化` successor 触发条件、`mrp.md §实现偏离补注` Deferred），跨 owner doc 扫描
      - Skill: none
- [x] `Add` 提取 successor **源 3**：`docs/backlog/README.md` 既有追踪行（81 行 successor/deferred 提及）
      - Skill: none
- [x] `Proof` 三源对账：逐 successor 项核对三源覆盖（仅源 1 / 仅源 2 / 仅源 3 / 多源一致）、触发条件是否已满足、当前归属（已 done / 待 MA1 / 待 R1.0 / backlog README 登记但从未触发）；消歧计数（修正 roadmap "41 = arm-index 内嵌数"口径为实测三源并集）
      - Skill: none
- [x] `Add` 落盘产出文件"§successor 三源对账清单"段：每行含 successor 项 / 三源覆盖标记 / 触发条件摘要 / 是否已满足 / 当前归属 / 复杂度，按域与影响面排序
      - Skill: none

Exit Criteria:

- [x] 产出文件含"§successor 三源对账清单"段（每行 successor 项/三源覆盖/触发条件/是否满足/当前归属/复杂度）
- [x] 三源对账差异已记录（计数口径修正、单源项、backlog 登记从未触发项）

### Phase 3 - 集成排序 + 对账差异登记 + MA2/MA3 消费说明

Status: completed
Targets: `docs/audits/rc-existing-inventory.md`（补 §集成排序 + §对账差异登记 + §MA2/MA3 消费说明）
Skill: none

- Item Types: `Add | Decision`

- Prereqs: Phase 2 完成（两份清单已建立）

- [x] `Add` 按域 + 影响面（复杂度 + 会计保护区域优先）对全集排序，产出 MA2 A2.1-A2.9 与 MA3 A3.1-A3.5 可直接消费的待复查全集视图
      - Skill: none
- [x] `Add` "§对账差异登记"段：记录三源对账中发现的所有差异（计数口径偏差 / 单源遗漏 / backlog 登记从未触发 / owner doc 内嵌但 arm-index 无行），交 MA2/MA3 复查时关注，**不回写源文件**
      - Skill: none
- [x] `Decision` "§MA2/MA3 消费说明"段：明确 MA2 逐 A2.x 行复查范围 = §方案 B 全集清单对应分区行；MA3 逐 A3.x 行复查范围 = §successor 三源对账清单对应域分组；声明 0.3 仅导出不评判（复查裁决属 MA2/MA3）
      - Skill: none

Exit Criteria:

- [x] 产出文件含集成排序视图（按域 + 影响面）、"§对账差异登记"段、"§MA2/MA3 消费说明"段
- [x] MA2 A2.x 分区与方案 B 全集行一一对应；MA3 A3.x 域分组与 successor 清单域归属一致

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 ses_03ea400d1ffekncnnmCNdmFF6v，fresh session）。10/11 项检查 PASS，1 项 BLOCKING（[C] Baseline 准确性）：`docs/architecture/arm-scope.md` 不存在（实测），真实文件为 `docs/audits/audit-remediation-scope-and-dimension-matrix.md`（昵称 arm-scope），其 §1.2 复杂度分级按**域**评级 S/A/B/C，非 plan 臆造的 "major/moderate/minor" per-finding 分级。该错误出现在 4 处（Current Baseline / Goals / Task Route / Execution item）。
- Independent draft review iteration 2: `acceptable`（独立子代理 ses_03ea0c00cffekLxb3aDevVqBcz，fresh session）。修订后复查：阻塞项已完全解决——`rg "architecture/arm-scope"` 仅命中 3 处显式负面警告（"不存在"），无活引用；"major/moderate/minor" 仅余 Draft Review Record 历史引述；4 处活引用（Current Baseline/Goals/Task Route/Phase 1 item）全部改为 `docs/audits/audit-remediation-scope-and-dimension-matrix.md §1.2` 域分级 S/A/B/C（findings 继承所属域等级，跨域项按主域），与实测 §1.2 一致（S=finance/mfg/hr；A=assets/purchase/sales/quality/crm/projects/cs/contract/b2b/inventory；B=master-data/maintenance/drp；C=logistics/notify/aps）。S/A/B/C 继承逻辑连贯。无新增阻塞项；单结果表面、只读纪律、导出口径、A2.x 分区校验、Closure Gates doc-only 删除 build/test、Deps 框架均完整。达到共识。

## Closure Gates

> 本计划为**只读提取 + 清单产出**工作项（无代码/ORM/api.xml/view.xml 变更，不修改 arm-index/owner doc/backlog），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——清单产出不触发编译或测试。验证 = 产出文件完整性 + 分区校验 + 三源对账可追溯 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：方案 B 全集清单（按 A2.x 分区）+ successor 三源对账清单 + 集成排序 + 对账差异登记全部落地
- [x] 相关文档对齐：产出文件与方法论 §2/§4（三判据）/§7（衔接）一致；导出口径与 roadmap M0.3 详情 + MA2 分区表一致
- [x] 已运行验证：方案 B 筛行口径自检（保留/排除标签 + 计数）+ 分区完整性校验（未分区项=0）+ 三源对账覆盖可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 方案 B 项与 successor 项的复查裁决

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0.3 是**全集导出 + 分区 + 对账**，不评判方案 B 项是否"有意设计 vs 静默降级"（MA2 A2.1-A2.9 范围）、不裁决 successor 是否该回队（MA3 A3.1-A3.5 范围）。0.3 产出的全集与差异登记是 MA2/MA3 的复查输入，非复查结论。
- Successor Required: yes（MA2 A2.1-A2.9 逐行复查方案 B 项 + MA3 A3.1-A3.5 逐行复查 successor 项，二者 Deps 含 0.3，本计划 done 后进入 DRAFT_PLANS 可起草范围）

## Closure

Status Note: 全部三个 Phase 已完成。产出文件 `docs/audits/rc-existing-inventory.md`（300 行）含方案 B 全集清单（10 项，A2.x 分区无重叠无遗漏，未分区项=0）+ successor 三源对账清单 + 集成排序 + 对账差异登记（6 项）+ MA2/MA3 消费说明。只读纪律遵守——git status 仅本计划文件 + 新产出文件变更，无真相源/ORM/Java 修改。本计划为纯清单产出无代码变更，故 Closure Gates 删除 build/test 门控（按 plan 既定声明）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_03e8c7f4dffeQye65SYIIIpyIB（fresh session，cold-context，未执行本计划）
- Evidence: 9 门控全 PASS——(1) 7 必需段落齐全；(2) 方案 B 全集恰 10 项 + 自检保留项计数=10/未分区项=0；(3) 分区无重叠（A2.1=6/A2.2=1/A2.3=1/A2.8=1/A2.9=1/A2.4-A2.7=0）；(4) 导出口径可追溯（P1-MA2-001=方案 B 裁决 / P1-MA2-018=documented simplification / P0-MA2-018=deferred / P1-MA2-002 正确排除）经 arm-index 实测确认；(5) 复杂度分级来源 §1.2 正确（finance/mfg=S，contract=A）经矩阵文件确认；(6) 三源对账含三源 + 计数差异登记；(7) plan 文件一致性——Plan Status=completed + 三 Phase completed + 全 [x] 无残留 [ ]；(8) anti-hollow——300 行实质内容 + 对账差异登记 6 条；(9) read-only 纪律——git status 仅 plan 文件 + 产出文件，无真相源修改。2 项 minor NOTES（Closure 占位符待回填[本步骤即回填] + 「41」计数来源间接）非阻塞。

Follow-up:

- 0.2（需求基线提取）与本计划无 Deps 互赖（均仅依赖 0.1），可并行；MA2 A2.1-A2.9 须等 0.2+0.3 均 done；MA3 A3.1-A3.5（Deps=0.3）在本计划 done 后进入 DRAFT_PLANS 可起草范围。
