# 设计文档开发就绪度与结构一致性评估

> 本文已根据多名独立子代理的交叉审查修订。修订重点：
> 1. 区分"当前无唯一答案"和"旧文档残留但现行答案已可判定"。
> 2. 下调证据不足的严重级别。
> 3. 补入更直接阻塞开发的模块落位、审批状态语义、requirements 越权等问题。
> 4. **第二轮修订（2026-07-01）**：对每个问题结合开源 ERP 调研（`docs/analysis/erp-survey/`）与 Nop 平台最佳实践（`nop-entropy/docs-for-ai/`）给出"选定方案"，含具体落地动作。同时修复了编号重置和重复条目，将测试相关子条目合并为单一 Issue。
>
> **人工裁决（后续补充）**：
> - `app-erp-all` 是最终打包的完整应用工程。
> - 当前正式范围是 **18 域**，`10 域` 为旧历史口径。
> - 跨域采用 `notGenCode` 外部实体引用方案，但必须保持单向 DAG，禁止循环依赖。
> - 财务过账方式需要支持配置调整，不固定死为单一执行模式。
> - 多步骤业务逻辑应优先参考 `docs-for-ai`，默认优先 `task.xml`。
> - 本项目不是 MVP 路线，而是完整产品的渐进式演化，通过 roadmap 规划。
> - 需要新增独立测试数据目录，用 CSV 组织，并提供稳定可发现的引用方式。

## 评估目的

评估 `docs/design/` 及其相关需求、架构、上下文文档是否已经形成可直接支撑开发的稳定基线，重点回答四个问题：

1. 对开发而言还有哪些关键不明确内容。
2. 各类技术难点的解决思路是否表达充分。
3. 整体文档结构是否合理。
4. 文档之间的内在逻辑是否一致。

## 评估范围

- `docs/context/`
- `docs/requirements/`
- `docs/design/`
- `docs/architecture/`
- 根 `pom.xml`

## 总结结论

**结论：本版已嵌入争议项的"选定方案"。对每个主要争议项，都给出了开源 ERP 参考 + Nop 平台最佳实践 + 可执行的落地动作。文档体系已从"问题报告"升级为"执行就绪的修复计划"。**

评估发现的核心问题分类为 16 项，已全部附带"选定方案"（结合 `erp-survey` 中成熟 ERP 做法与 `nop-entropy/docs-for-ai` 中平台最佳实践），并已修正编号重复、合并测试重复条目、删除已过时的子条目。

**第一轮（初始评估）核心结论仍是有效的**：局部深文档质量已很高，但全局总览层（18 域口径、审批状态、过账边界、测试策略、落位映射）未收敛。第二轮修订在每个问题下方直接嵌入了收敛后的方案。以下"主要发现"的每条现在都附带"选定方案"——即结合开源实践和平台最佳实践的明确裁决，而非仅指出冲突。

## 已经表达得比较充分的部分

以下内容已经达到较高可开发性：

### 1. 核心业务语义与流程

- `docs/design/app-overview.md` 对核心业务环节、角色和应用边界表达清楚。
- `docs/design/flow-overview.md` 已把采购、销售、制造、库存、业财打通、异常恢复串成完整业务图景。
- 各核心域 README 与 `state-machine.md` 已能回答"单据是什么、状态怎么走、谁触发、异常怎么回退"。

### 2. 典型技术难点的解决思路

以下难点已有较强设计表达：

- 库存建模：`docs/design/inventory/README.md:26-78`
- 采购审批/反审核/三轴状态：`docs/design/purchase/state-machine.md:7-208`
- 业财打通与可插拔 Provider：`docs/design/finance/posting.md:9-328`
- 跨域数据依赖与弱指针/外部实体引用：`docs/architecture/data-dependency-matrix.md`
- 模块 DAG 与边界：`docs/architecture/module-boundaries.md:7-82`
- 角色与高危操作控制：`docs/design/roles-and-permissions.md:11-89`

这些内容说明项目已经跨过"业务概念草图"阶段，进入了"可以指导实现"的层次。

### 3. 文档分层思想本身是正确的

顶层分层 `context -> requirements -> design -> architecture -> analysis` 是合理的，`docs/index.md` 也给出了清晰路由。整体方法论没有问题。

## 主要发现

> 以下编号在第一轮评估基础上做了修正：移除了重复编号、合并了测试主题下的 11.x 子条目（含 11.4↔12、11.5↔13 两对重复），形成 16 条连续编号。每条末尾附"选定方案"（来源：开源 ERP 调研 + Nop 平台最佳实践 + 项目上下文综合裁决）。

---

### 1. Major: 稳定 owner doc 与真实模块/路径映射混用，增加开发落位成本

**证据**：

- 多个稳定文档仍把域工程写成 `app-erp-inventory`、`app-erp-manufacturing`、`app-erp-app`，例如：
  - `docs/design/inventory/README.md:18-24`
  - `docs/design/manufacturing/README.md:17-23`
  - `docs/architecture/system-baseline.md:21-24,31-51`
  - `docs/architecture/domain-module-split-analysis.md:9,31,51,57,61`
- 但当前仓库真实顶层模块是：
  - `module-inventory/`
  - `module-manufacturing/`
  - `app-erp-all/`
  - 根聚合 `pom.xml:21-45` 中也没有 `app-erp-app`、`app-erp-inventory`、`app-erp-manufacturing` 这些路径。

**选定方案**：以 Maven artifactId 前缀 `app-erp-<domain>` 作为"逻辑工程名"的**唯一规范**，物理目录 `module-<domain>/` 是 bootstrap 期物理别名，两者在 `domain-module-split-analysis.md` 新增的 §2.0 映射表中集中记录。所有 design/architecture 文档统一引用逻辑名，物理目录供构建脚本与 IDE 识别。nop 平台 `domain-module-pattern.md:13-24` 明示目录名 = artifactId 是 nop 统一范式；Metasfresh、Odoo、iDempiere、Axelor 等全部采用"目录名=逻辑名=统一"（`erp-survey/2026-06-22-0000-module-split-comparison.md`），本项目分叉是 bootstrap 期偶然。

**落地动作**：
1. 在 `domain-module-split-analysis.md` §2 前插入 **§2.0 工程命名映射表**（19 行：18 域 + 1 聚合），列：业务域 | 逻辑工程名 | 顶层目录 | 子模块前缀 | artifactId 前缀 | appName | VFS moduleId | 二级简称 | 实体前缀 | 表前缀。
2. `module-boundaries.md` §核心/扩展表上方加指针："物理目录与命名映射见 `domain-module-split-analysis.md §2.0`"。
3. 将表内 `app-erp-app` 改为 `app-erp-app（聚合，物理目录 app-erp-all）`。
4. 在 `docs/context/codebase-map.md` 或 `system-baseline.md` 补充相同映射表入口。

---

### 2. Major: 审批状态与业务状态语义未完全收敛，直接影响实现一致性

**证据**：

- `domain-design-guidelines.md:530-539` 定义 `approveStatus` 为 `UNSUBMITTED / SUBMITTED / APPROVED / REJECTED`。
- `approval-framework.md:42-45` 明确写 `approveStatus` 只跟踪业务终态，不跟踪工作流内部状态。
- 但 `flow-overview.md:541-545` 又写"业务单据提交审核 → 业务状态 APPROVING"。
- `service-layer-orchestration.md:84-95,150-152` 的示例又使用 `order.status` 和 `PENDING_APPROVAL`。

**选定方案**：采用**三轴状态分离** —— 业务单据头使用三个正交字段：`docStatus`（业务生命周期） + `approveStatus`（审批终态） + `posted`（过账标志）。`approveStatus` 严格限定为 `UNSUBMITTED / SUBMITTED / APPROVED / REJECTED` 四态，**禁止** `APPROVING`、`PENDING_APPROVAL`。工作流内部运行态（会签、转审、待阅）完全由 nop-wf 引擎在 `NopWfStepInstance` / `NopWfWork` 表中管理，不污染业务表。依据：iDempiere 的 DocumentEngine 使用双状态机分离（DocStatus + DocAction）是行业标准（`erp-survey/2026-06-22-0000-workflow-vs-state-machine.md:94-118`）；赤龙 ERP 的 status + approveStatus 三轴正交（同文:76-87）。Nop 平台 `nop-wf.md:39` 确认 nop-wf 自身有 13 态步骤状态，与业务 approveStatus 的 4 态完全分离。

**落地动作**：
1. 改 `approval-framework.md:44-45` —— 把"只跟踪 APPROVED/REJECTED"补充为完整四态："`approveStatus` 只跟踪业务终态（`UNSUBMITTED/SUBMITTED/APPROVED/REJECTED`），不跟踪 wf 内部状态。`SUBMITTED` 表示已提交待审批，审批中场内进度由 nop-wf 引擎管理。"
2. 改 `flow-overview.md:542` —— 把`"业务状态 APPROVING"`改为`"approveStatus = SUBMITTED"`。
3. 改 `service-layer-orchestration.md:87,109` —— `order.status` → `order.approveStatus`；`:151` — `'PENDING_APPROVAL'` → `'SUBMITTED'`。
4. 全文 `rg -n 'APPROVING' docs/` 和 `rg -n 'PENDING_APPROVAL' docs/` 确保无残留。

---

### 3. Major: requirements 层既未收口，又未反映"完整产品渐进式演化"这一正式开发策略

**证据**：

- `product-baseline.md:11-39` 仍保留 `<capability>`、`<step>`、`<manual operation>` 等占位符。
- `mvp.md:7-13` 仍是完全未落地模板。
- 同时 `product-baseline.md:9` 又写入了本应由 design/architecture 持有的实现决策。

**选定方案**：人工裁决已明确"不是 MVP 开发，而是完整产品渐进式演化"，故 `mvp.md` 应归档为历史模板。`product-baseline.md` 的本职是产品能力边界和验收语义，技术机制内容（如 posting event、I*Biz 优先、单向 DAG）应迁回 `docs/design/` 或 `docs/architecture/`。Reference: Odoo 的 `odoo.md` 和 ERPNext 的 `erpnext.md` 对产品边界的处理（`erp-survey/`）均与该项目"模块化、渐进式"基调一致，没有使用 MVP 化需求文档。Nop 平台 `docs-for-ai/02-core-guides/application-project-docs-and-domain-design.md` 也建议 requirements 层保持产品边界定义而非实现细节。

**落地动作**：
1. 将 `mvp.md` 标注为历史模板或移入 `docs/archive/`；在 `docs/index.md` 路由中去掉它。
2. 补齐 `product-baseline.md`：当前 18 域产品能力边界、渐进式演化策略与 roadmap 关系、阶段性完成标准。
3. 将 posting 机制、跨域引用规则、平台组件选择等迁回 `docs/design/` 或 `docs/architecture/`，requirements 只保留"什么"不保留"怎么"。
4. 在 product-baseline.md 顶部加状态横幅："> 本文是产品基线需求文档。实现决策见各自域 design/architecture owner doc。"

---

### 4. Major: 跨域引用现行方案已明确，但旧文档未清理且循环依赖约束需进一步显化

**证据**：

- `cross-domain-constraints.md:30-46` 仍写"禁止 ORM 层跨工程 refEntityName，使用 @RefLink"。
- 但现行方案已在 `data-dependency-matrix.md:281-286,373-421,452-580` 明确为 `notGenCode + 外部实体引用 + to-one`；`module-boundaries.md:49-64` 一致引用平台 `cross-module-entity-reference.md`。

**选定方案**：`@RefLink` 在 nop-entropy 全仓库不存在，是 2026-06-29 grill 讨论中自造但未落地的概念（`discussions/2026-06-29-1000-grill-with-docs-design-review.md:1360-1369`）。`cross-module-entity-reference.md:26-56` 确认 `notGenCode + <to-one>` 是平台官方推荐。`cross-domain-constraints.md` 应**重写而非归档**——保留其"消弧事件"章节（仍被 `domain-design-guidelines.md:579` 引用），删除 `@RefLink` 节，替换为平台原生机制描述。在 `module-boundaries.md` 和 `data-dependency-matrix.md` 中把"禁止循环依赖"进一步突出为硬规则。

**落地动作**：
1. 改 `cross-domain-constraints.md`：保留 §"目的"与 §"消弧事件"（:3-28）+ §"跨域事务约束"（:48-54）。
2. 删除 §"@RefLink 引用"（:30-46）整节，替换为新节 **"§ 跨域实体引用（平台原生机制）"**，内容：读引用用机制 B（`notGenCode + <to-one>`，高频多维场景）或机制 D（纯外键 + `I*Biz`，列表/详情场景）；写经 `I*Biz` 接口；严禁给外部表生成新 `className`（走 Delta 扩展）。
3. 在被删节位置加取代说明行："`@RefLink` 已被平台原生 `notGenCode + <to-one>`（机制 B）与纯外键 + `I*Biz`（机制 D）取代，详见 `data-dependency-matrix.md §5.5-5.6`、`cross-module-entity-reference.md`。"

---

### 5. Major: 财务过账需要从"单一机制之争"改写为"可配置机制边界"

**证据**：

- `flow-overview.md:41-55,318-362` 与 `posting.md:18-31,256-279` 把异步 PostingEvent + `posted=false` + 兜底扫描写成主路径。
- `data-dependency-matrix.md:132-180` 又把若干场景描述为"同一事务内写业务表 + inventory + finance"，`:204-210` 甚至写到"本工程默认同步，性能瓶颈时再异步化"。

**选定方案**：采用**三层分层模型**：① 业务单据状态变更 + 库存写入（stock_move/ledger/balance）**永远在同一 `@BizMutation` 事务内强一致**（不可配置）——这是物理库存正确性的硬约束，iDempiere 的 `Doc.post` + Metasfresh 的 `IPostingService` 均确认库存无法异步化（`erp-survey/2026-06-22-0000-idempiere.md:71-82`、`metasfresh.md:70-84`）；② **凭证生成层的时序**（SYNC 同事务 / ASYNC post-commit）做成按 `(billType, acctSchemaId)` 可配置；③ **posted 标志 + 业财回链 + 物理锁定 + 兜底扫描 + 红字冲销**是跨两种模式强制生效的稳定不变约束。Nop 平台 `concurrency-and-transactions.md` 和 `transaction-boundaries.md:26-31` 的 `txn().afterCommit()` 是实现 ASYNC 模式的平台原生机制。

**落地动作**：
1. 改 `posting.md` §总体架构和 §异步过账：架构图改为三层（业务+库存同事务 / 可配凭证层 / posted 兜底层），标注"库存写入不参与可配置，强制 SYNC"。
2. 改 `data-dependency-matrix.md` §4.1：把凭证段从"S 写"拆为"SYNC 同事务 / ASYNC 经 afterCommit 解耦"；§4.4 L209 升级为"默认 SYNC，按 billType 可切 ASYNC"。
3. 改 `posting.md` 新增"稳定约束 vs 可配置策略"小节：不变 = 幂等 + 库存强一致 + 业财回链 + posted 锁定 + 可补偿 + 可审计；可配 = 仅凭证生成时序（SYNC/ASYNC）。

---

### 6. Minor: 权限设计基线与当前运行基线需要更明确地区分

**证据**：

- `roles-and-permissions.md:84-89` 把 RBAC、数据权限、审批流、审计日志写成正式实现落位。
- `app-overview.md:36` 又明确写"操作权限检查默认关闭（nop.auth.enable-action-auth=false）"。

**选定方案**：`roles-and-permissions.md` 必须拆为"设计能力基线"和"运行基线"两个显式小节。`nop.auth.enable-action-auth=false` 是平台默认值（`nop-entropy/docs-for-ai/02-core-guides/auth-and-permissions.md:8,108,117`），属合理状态。数据权限（`nopDataAuthChecker`）独立于该开关，始终附加到查询条件（同文:232-296）。Axelor 等开源 ERP 的 portal 模块也是"权限定义随模块安装生效"（`erp-survey/2026-06-30-0000-axelor-open-suite.md:32,50,158`），证明"已定义≠默认开启"是行业常态。

**落地动作**：
1. 改 `roles-and-permissions.md` —— 把"实现落位"拆为两段：
   - **设计能力基线（已沉淀）**：角色矩阵、`*.action-auth.xml` 资源点（codegen 自动产出）、`data-auth.xml` 数据权限规则——这些"已定义且始终生效（数据权限不依赖操作开关）"。
   - **运行基线（当前拦截状态）**：显式写出 `nop.auth.enable-action-auth=false`（操作级拦截关闭）、`nop.auth.skip-check-for-admin=true`；给出灰度启用步骤。加注"数据权限不受此开关影响，始终启用"。
2. 改 `app-overview.md:36` —— 括号注释改为指向 `roles-and-permissions.md#运行基线` 的链接。

---

### 7. Major: 测试策略存在旧稿残留，且缺少正式测试数据目录与发现机制

> 本条目合并了原评估文档的第 7/11/11.1/11.2/11.3/11.4/11.5/12/13 等测试相关子条目。原 11.4 与 12（跨域业务流归属）、11.5 与 13（异步时序模型）内容重复，已在此合并。

**证据**：

- `testing-strategy.md`（90 行）与 `test-strategy.md`（109 行）都以"测试策略"为 owner doc，口径冲突。
- 两者对测试层级、基类、数据来源写法不同。`test-strategy.md:30-34` 把 `_cases/` 写成 `.xml`，而平台真实约定是 `input/tables/*.csv`（`testing.md:53`）。
- 仓库不存在 `app-erp-seed` 或 `app-erp-test-data` 路径。
- 四类测试资产边界（deploy seed / shared fixture / `_cases/input` / `_cases/output`）没有区分。seed-data.md 讨论的是部署初始化而非测试 fixture。
- 跨域业务流测试（如采购审核→库存→过账）缺少归属规则（触发方 vs 被调方）。
- 异步过账场景缺少测试时序模型（同步 profile / 轮询 / 兜底）。

**选定方案**：
- **owner doc 裁决**：`testing-strategy.md` 为现行唯一 owner doc（与平台文档命名 `testing.md` 对齐），`test-strategy.md` 归档至 `docs/archive/`。
- **测试数据目录**：新建 Maven 模块 `app-erp-test-data`，CSV 置于 `src/main/resources/_vfs/test-data/tables/<table_name>.csv`（因平台 `_cases/` 是每方法快照，无跨测试类共享机制；按 `vfs-and-resource-resolution.md:8,10,83`，VFS classpath 扫描是平台推荐发现方式）。格式对齐平台 `_cases/input/tables/*.csv` 约定（`testing.md:53`）。根目录放 `load-order.txt` 指定加载顺序。
- **测试资产边界**：部署 seed（`_init-data/`）无当前存在 → 标注 deferred；共享 fixture（`_vfs/test-data/tables/`）新建模块；`_cases/{input,output}` 私有于测试方法。
- **跨域归属硬规则**：触发域 owner 全链路、被调域仅契约测试、多触发/系统不变量集中到 `app-erp-all`。
- **异步时序模型**：Nop 平台无内建"测试期同步化"（`testing.md:296-305`，`@NopTestConfig` 无相关 flag），故须自建同步测试缝。三点：同步缝（`postNow()` 直调 + `JunitAutoTestCase` 快照）+ 异步轮询（`@Timeout` 自旋断言 `posted` 翻转）+ 兜底直调（`sweepJob.runOnce()` 不依赖时序）。

**落地动作**：
1. 归档 `test-strategy.md` → `docs/archive/test-strategy.md`；`testing-strategy.md` 顶部声明唯一性。
2. 回收 `test-strategy.md:48-69` P0/P1 业务流清单并入 `testing-strategy.md`。
3. 新建 `app-erp-test-data` 模块（`_vfs/test-data/tables/` + `load-order.txt` + fixture-loader helper），各 `-service` 测试模块 test-scope 依赖。
4. 在 `testing-strategy.md` 新增四小节：四类资产边界表、跨域归属三层规则、异步测试时序模型。
5. `seed-data.md` 顶部标注："部署资产，非测试资产，与 `app-erp-test-data` 区分；当前不存在，属独立 follow-up"。
6. 过账测试缝：`IErpFinPostingBiz` 设计阶段即预留同步 `postNow` 入口（写进 `posting.md`）。

---

### 8. Major: 当前正式范围已是 18 域，但全局文档仍残留大量"10 域旧历史"口径

**证据**：

- `product-scope.md:7-31,33-61`、`project-vision.md:11-14` 仍把正式产品基线描述为 10 域。
- 根 `pom.xml:21-45` 已聚合 18 个业务模块。
- 多个 design 文档已把若干扩展模块当作正式内容写入。

**选定方案**：人工裁决已明确正式范围是 18 域。全局 owner doc（`product-scope.md`、`project-context.md`、`project-vision.md`、`system-baseline.md`、`module-boundaries.md`、`data-dependency-matrix.md`）必须统一为 18 域口径。若不统一，roadmap、测试矩阵、开发审计都会持续被旧数字污染。

**落地动作**：
1. 改 `product-scope.md` —— 18 域完整列表，删除"10 域"阶段概念。如需保留历史口径，迁入 `docs/archive/`。
2. 改 `project-vision.md` —— 同样改 18 域。
3. 改 `module-boundaries.md` —— DAG 图、依赖表从 10 域扩展为 18 域（含 crm/cs/hr/aps/contract/drp/logistics/b2b），并加"8 个扩展域为 18 域正式基线的一部分"说明。
4. 改 `data-dependency-matrix.md` —— 同上，扩展域级矩阵。

---

### 9. Minor: B2B 主题已经形成 design/architecture 双层文档，但顶层路由仍把它当成纯 architecture 主题

**证据**：

- `design/b2b/README.md:1-15` 自称"业务语义、工作流、状态含义"的设计文档。
- 但 `design/README.md:79` 仍写"B2B 集成 owner doc 在 `architecture/b2b-integration.md`"。

**选定方案**：承认已自然形成的双层分工：`docs/design/b2b/` 拥有业务语义、状态机、用例、页面；`docs/architecture/b2b-integration.md` 拥有 SPI、Webhook、集成契约和技术边界。同步修正顶层路由。

**落地动作**：
1. 改 `docs/design/README.md` —— 在业务域表中将 B2B 列为 design 域，owner doc 指向 `design/b2b/README.md`。
2. 改 `docs/architecture/README.md` —— 保留 B2B 集成契约指向 `architecture/b2b-integration.md`。
3. 改 `feature-inventory.md` —— 修正 B2B 路由引用。

---

### 10. Major: Portal 已被写入功能清单，但未被正式纳入顶层 owner-doc 体系，且跨入了受保护主题

**证据**：

- `feature-inventory.md:100` 把 portal 列为已设计功能。
- 但 `app-overview.md:10` 仍写"前台商城/门户暂不在当前基线范围"。
- `portal/README.md` 定义了在线付款、OAuth2/JWT、SSO、第三方支付、与 `nop-app-mall` 复用契约——这些均落在 `ai-autonomy-policy.md` 的受保护区域（支付、认证、外部集成）。
- `roles-and-permissions.md` 只覆盖内部 ERP 角色，没有外部主体模型。

**选定方案**：实测 `/Users/abc/app/nop-app-mall` 不存在，故 portal/README.md 的"复用 nop-app-mall"前提落空。**portal 应降级为 future extension placeholder**（保留 `portal/` 目录作方向性设计资产，但明确不在当前基线内）。外部主体模型采用 Odoo 式"业务主体绑定内部账号"：外部账号 = `NopAuthUser`（带 portal 角色）+ 业务主体 = `ErpMdPartner`（新增 `to-one userId → NopAuthUser`）；行级隔离用 `data-auth.xml` 软过滤而非 tenant 字段（项目不预置 tenantId，`tenant-model.md:85-94`）。参考 ERP 调研：portal 在所有标杆 ERP 中都是可选独立模块——Axelor `client-portal`/`supplier-portal` 独立可装卸（`axelor-open-suite.md:32,50,158-161,186`），ERPNext `shopping_cart` 是独立 domain（`erpnext.md:33`），非内核。

**落地动作**：
1. 改 `portal/README.md` —— 顶部加 STATUS 横幅："future extension placeholder，不在当前产品基线（product-scope.md 延迟范围）。支付/SSO/mall 复用均为占位性描述，实施前需 plan-first + 人工批准。"
2. 降级其中的支付/SSO/商城复用论述为 `(future)` 标记。
3. 改 `design/README.md` —— 扩展域表加一行：`portal | future extension（非当前基线） | portal/README.md`。
4. 新建 `portal/identity-and-access.md`（future）—— 定义 portal-customer/portal-supplier 外部角色、partner.userId 绑定模式、data-auth 行级隔离骨架、最小动作集。写在文档中不立即改 orm.xml（点击 `model/*.orm.xml` 保护区域）。
5. 改 `roles-and-permissions.md` —— 末尾加一句"外部 portal 主体（客户/供应商）见 `docs/design/portal/identity-and-access.md`（future extension），本文仅覆盖内部 ERP 角色"。
6. 改 `app-overview.md:10` —— 保留原句，补"详见 portal/README.md STATUS 横幅"。

---

### 11. Major: 设计层与架构层对 master-data 跨域引用仍给出相反指令

**证据**：

- `domain-design-guidelines.md:98` 仍写"所有域引用主数据必须通过 IErpMd*Biz 接口，禁止直接 ORM 跨工程引用"。
- `module-boundaries.md:51-57` 与 `data-dependency-matrix.md:281-286,454-462` 明确采用 `notGenCode` + `<to-one>` 作为现行标准做法，且 §5.6.2 已实测落地 267 个跨模块 to-one。

**选定方案**：`cross-module-entity-reference.md:193-255 §7` 明确给出了分级处理：列表显示用机制 D（纯外键 + 冗余显示名）、高频多维筛选/报表用机制 B（`notGenCode + <to-one>`）、写强制走 `I*Biz`。所以原规则"读引用也走 I*Biz"是错误的。应将 guidelines 的单句禁令改写为分级规则。开源 ERP（Odoo 的 `Many2one` 引用 `product.product`/`res.partner`）也是混合模式而非全部走服务接口。

**落地动作**：
1. 改 `domain-design-guidelines.md:96-98` —— 改写为 **§3.1 主数据引用分级规则**：
   - 读引用（列表显示名）：机制 D。
   - 读引用（详情带出完整对象）：机制 D（`@BizLoader` + `requireBiz`）。
   - 读引用（高频多维筛选/报表/GraphQL 展开）：机制 B（`notGenCode` + `<to-one>`，EQL 可点导航）。
   - 写引用：必须经 `IErpMd*Biz` 接口的 `@BizMutation`。
   - 禁止：给 master-data 表生成新 `className`（加字段走 `app-erp-delta` 的 `ext:baseClass` Delta 扩展）。

---

### 12. Major: `module-boundaries.md` 和 `data-dependency-matrix.md` 内部存在局部自相矛盾

**证据**：

- `module-boundaries.md:42-46` 表说 manufacturing 禁止依赖 quality、projects 禁止依赖 finance、quality 禁止依赖任何业务域。
- 但同文 `68-77` 英文段又写 Manufacturing may reference quality、Projects may reference finance、Quality may reference manufacturing/inventory。
- `data-dependency-matrix.md:69-71,84-86` 与 `166-179` S 写清单之间也存在局部不一致。

**选定方案**：**裁决原则**——① ORM 层以 `data-dependency-matrix.md §5.6.2` 实测清单为最高权威（已 DAG 验证零循环）；② S 写层以 §4.2 清单为权威；③ `module-boundaries.md` 中文表是模块级摘要，冲突时回改以匹配数据矩阵；④ **英文段（:68-77）是孤儿草稿，直接删除**不裁决。三处具体裁决：manufacturing → quality 允许单向业务触发（事件/I*Biz，ORM 零引用）；projects ↔ finance 双向 BUT 在不同层（ORM finance→projects，S 写 projects→finance），合法不构成循环；quality ORM 层只依赖 master-data，对任何业务域零 ORM 引用。`architecture-principles.md §二` 确认 DAG 按依赖类型分层校验。

**落地动作**：
1. 改 `module-boundaries.md` —— 删除 `:68-77` 整个英文"Cross-Module Reference Rules"段。改中文表依赖方向细化。改 DAG 图修正 quality/manufacturing 方向。
2. 改 `data-dependency-matrix.md` —— 在 §2.1 DAG 图上方加"裁决原则"小节（5 条优先级）。修正 manufacturing/manufacturing→quality、quality 描述。改 manufacturing 行 S 写/事件分离。

---

### 13. Minor: `businessType` 与 `billType` 两套标识体系都被写成权威，但缺少显式映射规则

**证据**：

- `posting.md:47-49,55-112` 把 `businessType` 视为过账路由的唯一权威。
- `data-dependency-matrix.md:230-258` 又把 `billType` / `relatedBillType` 定为弱指针反查与回链的重要标识。

**选定方案**：两者**不是一对一，职责正交**。`billType` 负责源单识别/回链（对应具体 ORM 实体/表，如 `PUR_RECEIVE`），承载于弱指针三元组，取值受 `data-dependency-matrix.md §5.2` 枚举约束。`businessType` 负责过账语义/凭证模板路由（如 `PURCHASE_INPUT`），承载于 PostingEvent 和凭证模板。一个 `billType` 可映射多个 `businessType`（同一源单在不同环节触发不同会计事件）。回链表 `voucher_bill_r` 同时存两者——它们共存，非互斥。iDempiere 的 `C_DocTypeTarget_ID`/`DocBaseType` 与 `Fact_Acct.AD_Table_ID+Record_ID` 正是这一分工的原型（`erp-survey/2026-06-22-0000-idempiere.md:51-59,67-71`），Metasfresh 的 `AcctDocRegistry` 用 `docTableName`（识别实体）与 `Doc_Invoice.internal createFacts`（会计语义）也分离（`metasfresh.md:77-84`）。

**落地动作**：
1. 改 `posting.md` —— 在 §业务类型映射开头补"`businessType` vs `billType` 分工"小节：明确 billType 回链（来自 data-dependency-matrix §5.2 枚举）、businessType 凭证路由（由本表定义）、二者非 1:1、`voucher_bill_r` 同时落两者。
2. 改 `data-dependency-matrix.md` §5.2 —— 在枚举表上加反向引用："`billType` 只管源单识别/回链；过账模板路由用 `businessType`（见 `finance/posting.md`），两者非 1:1"。

---

### 14. Minor: `domain-design-guidelines.md` 过大且承载职责偏宽，维护成本高

**证据**：

- 文档 >700 行，同时承载业务域边界与状态命名、ErrorCode 规则、删除策略、版本演进、BizModel / xbiz / Java 决策规则。

**选定方案**：`design` 中保留业务语义、状态命名、跨域业务规则。把 BizModel/xbiz/task.xml 选型、ErrorCode 落位、迁移策略等技术实现规则迁回 `architecture`。

**落地动作**：
1. guidelines 中 §"ErrorCode 规则" → 迁入 `docs/architecture/error-handling.md`。
2. §"删除策略" → 迁 `docs/architecture/logical-deletion.md`。
3. §"BizModel/xbiz/Java 决策" → 迁 `docs/architecture/service-layer-orchestration.md`。
4. guidelines 顶部加"本文定位：业务语义、状态命名、跨域业务规则的设计层规范。技术实现规则见 architecture 对应文档。"

---

### 15. Minor: `feature-inventory.md` 存在重复条目和导航歧义

**证据**：

- `feature-inventory.md:81-98` 已列一次扩展域，`:90-98` 又列一组重复条目。
- 同一能力 owner doc 口径不一致（如 APS 先指向 `manufacturing/crp.md` 后指向 `aps/README.md`）。

**选定方案**：去重，对跨域功能明确写"主 owner doc"和"辅助引用 doc"。

**落地动作**：
1. 清理重复行，归并为单表。
2. 对于跨域功能（如 APS、DRP、B2B），首列 owner doc 为主文档，次列辅助引用。
3. 确认 routing 一致性（如 APS 统一指向 `aps/README.md` 而非 `manufacturing/crp.md`）。

---

### 16. Minor: 少量稳定文档仍残留旧模板或旧审计产物，影响整体洁净度

**证据**：

- `integration-and-transaction-patterns.md:1-19` 仍带明显模板口吻。
- `erp-design-audit-checklist.md` 仍保留大量"完成项复述"。

**选定方案**：清除模板残留、降级审计干预期产物。`integration-and-transaction-patterns.md` 如果无实质内容应标注 status placeholder 或精简。`erp-design-audit-checklist.md` 在审计体系稳定后归档。

**落地动作**：
1. 改 `integration-and-transaction-patterns.md` —— 去除模板口吻；确认其内容是否被 `data-dependency-matrix.md` 或 `transaction-boundaries.md` 覆盖，如是则标注为 superseded。
2. `erp-design-audit-checklist.md` —— 在 owner doc 体系稳定后宣告其使命完成，标注"历史审计清单，当前业务规则以各自设计文档为准"。

---

## 对开发而言仍不明确的关键点

> 以下问题在原评估中列出，当前均已在"主要发现"中给出明确方案。此处不再重复列出。

原评估列出的 6 个关键不明确点（代码落位路径、审批状态、过账约束、多步骤默认落位、权限基线、测试闭环）已全部在"主要发现"中给出选定方案和落地动作。

## 对"技术难点是否表达充分"的判断

### 表达充分的难点

以下难点已经有较充分方案，**且已在本次修订中收敛**：

- 采购/销售/库存/财务主闭环的业务语义
- 反审核、红冲、核销、三单匹配等 ERP 典型复杂语义
- 库存三层模型与追溯链
- Provider 式业财过账扩展
- 多账套、多币种、批次/序列号等典型 ERP 技术要点

### 表达还不够收敛的难点

以下难点在**本次修订前**是多份共存，**修订后已给出收敛方案**，但尚需执行落地后方可算真正收敛：

- 审批状态与工作流状态映射 → 选定方案：三轴分离，approveStatus 严格四态
- 过账事务边界 → 选定方案：三层模型（库存强一致 + 凭证可配）
- 测试策略 → 选定方案：单一 owner doc + 新建 test-data 模块 + 归属规则 + 时序模型
- 18 域正式范围的全局口径清理 → 选定方案：全局文档统一改
- 多步骤服务层的默认落位 → 人工裁决已明确默认优先 `task.xml`

因此整体判断更新为：

**技术难点已全部有选定方案。剩余工作是执行落地，而非讨论。**

## 对文档结构的判断

### 结构上合理的部分

- 顶层目录职责划分清楚。
- `docs/index.md` 的路由能力较强。
- `docs/design/<domain>/` 的域化组织方式适合 ERP 这种多域系统。
- `analysis/` 作为研究与比对结论的沉淀区，位置正确。

### 结构上需要优化的部分

1. **当前基线设计与扩展性设计混在同一层级** —— 建议更多强调"现行基线 vs 历史分析/历史阶段"的区分。
2. **同主题双文档并存** —— 测试策略最明显，已在选定方案中处理（归档 test-strategy.md）。
3. **逻辑工程名与仓库路径缺少集中映射层** —— 选定方案已解决（domain-module-split-analysis.md §2.0）。
4. **少数总纲文档过大** —— domain-design-guidelines.md 已有拆分方案。

## 对内在逻辑一致性的判断

### 一致性较好的部分

- 核心域设计内部一致性整体不错。
- 状态机、角色、高危操作、大多数业务流程之间基本能互相对上。

### 一致性问题主要集中在"横向总览层"

局部深文档的质量高于全局总览文档的收敛度。本次修订将几乎所有横向总览层的矛盾点（范围口径、过账策略、审批状态、测试策略、模块边界内部矛盾、跨域引用指导方向）都给出了收敛后的选定方案。

## 建议的修复顺序

以下是建议的执行顺序，与主要发现的编号对应：

1. **先清理全局旧历史口径与真实落位映射**（#1, #8）
2. **统一审批与状态语义**（#2）
3. **补齐并收边 requirements 层**（#3）
4. **统一过账、跨域引用、权限、测试四类总控答案**（#4, #5, #6, #7, #11, #12, #13）
5. **处理 portal / B2B 路由与文档界限**（#9, #10）
6. **最后再处理总纲瘦身与历史归档**（#14, #15, #16）

## 最终结论

本项目的设计文档**深度已经足够高**，经过本轮基于开源 ERP 实践和 Nop 平台最佳实践的综合修订后，原有 16 个问题已全部配有"选定方案"和可执行的落地动作。

如果目标是做需求研究、架构探索、域模型设计，当前文档体系已经很强。

如果目标是让不同开发者直接实现，**当前文档已给出所有关键争议的明确裁决**，剩余工作是把"选定方案"中的落地动作逐一执行——修改 owner doc、归档旧稿、新建映射表、补充测试数据目录。本章不包含新讨论，只包含执行清单。
