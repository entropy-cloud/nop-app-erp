# 2026-07-01-1900-1-platform-compliance-remediation 平台最佳实践合规整改计划

> Plan Status: active
> Last Reviewed: 2026-07-01
> Source: `docs/analysis/2026-07-01-1900-platform-best-practices-compliance-audit.md`（4 维度合规审计，综合 7.3/10）
> Related: P1 各业务计划（`docs/plans/2026-07-01-0811-*`、`1132-*`、`1426-*`）—— 本计划修复其遗留的平台 API 误用
> Audit: required

## Current Baseline

- CRUD 18 域全绿（90 冒烟方法通过）；P1 核心业务逻辑 5 段 done/partial（请购转化、订单审批、入库/出库触发库存、StockMove、过账引擎）。
- **服务层系统性偏离 `ai-defaults.md`**：全项目 main 代码 `@BizMutation/@BizQuery/@BizAction` 命中 0 次，自定义写方法一律 `@SingleSession @Transactional`；跨域/跨聚合访问用 `daoFor()` 绕过 I*Biz 管道（18 处行级引用 = 跨聚合写 8 + 跨域读 10，详见 Phase 1 S2 表，含审计初稿遗漏的 `ErpSalDeliveryBizModel:364` 跨域读库存）；`requireEntity()` 退化为 `dao().getEntityById()`；`new ErpXxx()` 直接构造实体。
- **ORM**：purchase/inventory/finance 3 域金额族 122+ 字段 VARCHAR 存储（sales 域对照已用 DECIMAL）；全工程 0 索引；dict valueType 不一致。VARCHAR 偏离**未在 owner docs 登记为决策**。
- **模块边界**：purchase/sales → projects 跨业务域 ORM 硬引用，与 `data-dependency-matrix.md`/`module-boundaries.md` 冲突（文档与实现不一致）。
- **视图**：DRP 域菜单断链 3 处（`erp-drp.action-auth.xml:15,19,46` 的 `ErpInvDrp{Plan,Line,Parameter}`，实际目录为 `ErpDrp*`；生成基线亦混用前缀）；单据状态机按钮全未接线。两者已拆出本计划。
- 因 S1（Biz 注解缺失）阻塞，现有测试自述"直调 Java API，不走 GraphQL 快照"，掩盖了 S1/S2。

## Goals

- 自定义服务层（purchase/sales/inventory/finance 四域 BizModel + 辅助类）全面对齐 `ai-defaults.md` 反模式表：Biz 注解、I*Biz 跨实体访问、`requireEntity`、`newEntity`、`CoreMetrics`。
- 消除 2 处阻塞服务层整改的文档-实现冲突（金额族 VARCHAR、projects DAG），通过**明确 Decision** 收敛；VARCHAR/orm.xml 结论属保护区域，须人工批准后方落地。
- 修复后，关键业务方法可经 GraphQL 暴露并由快照测试覆盖。

## Non-Goals（范围收敛说明）

> 本计划的结果表面 = **自定义服务层平台 API 合规**。Phase 0 的两项 Decision 是该表面的前置门控（projects 若选弱指针会改服务层跨域访问写法；VARCHAR 结论决定金额字段 Java 类型），故纳入。DRP 视图缺陷、工具/索引/dict 统一项与结果表面不同、验证路径独立，**已拆出为独立 successor**（见 Deferred），不纳入本计划。

- **不**接线单据状态机页面按钮（V2）—— 视图层结果表面，依赖 S1 落地；列为显式 successor。
- **不**修复 DRP 菜单断链（V1）—— 视图层缺陷，独立 successor 计划 `2026-07-01-1900-2-drp-menu-broken-links.md`（待创建）。
- **不**处理工具/索引/dict 统一项（M2 codegen.sh / M3 validate-page-model / O2 索引 / O3 dict valueType）—— bootstrap 杂项，独立 successor 或 backlog。
- **不**实施 O1 金额 VARCHAR→DECIMAL 的全量数据迁移与回归（属独立计划；本计划仅做 Decision + 保护区域登记）。
- **不**重构为 task.xml/xbiz 编排（S7）—— 业务逻辑当前正确，编排方式优化属后续 enhancement。
- **不**新增业务功能 —— 仅收敛现有代码到平台规范。
- **不**改动 codegen 生成物（`_gen/`、`_` 前缀文件）。

## Task Route

- Type: `architecture change`（平台 API 契约对齐 + 阻塞它的模型/边界 Decision）
- Owner Docs: `docs/architecture/data-dependency-matrix.md`、`docs/architecture/module-boundaries.md`、`docs/architecture/system-baseline.md`（VARCHAR/projects 决策落地）；`nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`（服务层规则源）
- Skill Selection Basis: `nop-platform-conformance-audit-prompt`（整改后自检反模式）；`code-quality-audit-prompt`（服务层改造验收）
- Verification: `mvn test -pl module-{purchase,sales,inventory,finance} -am` + GraphQL 快照测试（S1 修复后新增）
- **契约面风险**：Phase 1 将修改 `IErp*Biz` 接口（purchase/sales/inventory/finance/master-data 的 `-dao` 模块）。I*Biz 是跨域后端服务调用契约（`service-layer.md`），新增方法/注解影响所有注入方（`-web`/`-api`/跨域 BizModel）。实施时须确认无注入方被破坏；`IErpMdPartnerBiz` 当前仅 `extends ICrudBiz`，新增只读方法是实质性接口扩张，需回归 master-data 冒烟测试。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 纯代码与模型整改，无外部服务/端口/密钥依赖。

## Execution Plan

### Phase 0 — 决策收敛（金额 VARCHAR + projects DAG）

Status: completed
Targets: `docs/architecture/data-dependency-matrix.md`, `docs/architecture/module-boundaries.md`, 10 域 `model/*.orm.xml`
Skill: none

- Item Types: `Decision | Fix`
- Prereqs: none（门控 Phase 1）

> **保护区域声明**：两项 Decision 的结论若涉及 `model/*.orm.xml` 改动（O1 必然；M1 选弱指针亦然），按 `docs/context/ai-autonomy-policy.md` 保护区域表 `model/*.orm.xml` = **ask-first**。AI 起草的结论**必须经人工批准**后方可落地到 owner doc 与模型；AI 自行写入 owner doc 不构成降级保护区域的证据。本阶段产出"建议结论 + 理由"，落地动作以人工批准为前置。

- [x] **Decision | Explore** 金额族类型策略（O1，已确认缺陷）：**人工已批准 Option A（统一 BigDecimal）**——用户明确指示"数字不要做成 String"。核实发现影响域**不止 audit 的 3 域**，实际 **10 域 248 列**（purchase 55/manufacturing 53/finance 46/crm 31/inventory 30/projects 13/contract 8/quality 6/logistics 3/maintenance 3；sales 等其余域已正确）。修复方式（用户指导）：金额/数量族 column 移除 `stdDataType="string"`（不该设置，它指定 Java 类型），`stdSqlType="VARCHAR"`→`DECIMAL` + 按域补 precision/scale（对齐 sales 写法），让 domain 推导 BigDecimal。**已落地 10 域 248 列**。
  - Skill: none
- [x] **Fix** O1 落地：Python 脚本批量修复 10 域 248 列 → `mvn clean install` 触发 codegen 重生成 `_app.orm.xml`+`_gen` 实体（String→BigDecimal）→ 修复自定义 Java（`ErpFinPostingService`/`StockMoveBookkeeper`/`InvPostingDispatcher`/`ErpInvStockMoveBizModel`/`RequisitionToOrderConverter`/`ReceiveStockMoveBuilder` 去 `parseAmount`/`toPlainString` 桥接）→ 修复测试（去 `new BigDecimal(getX)` 包装 + amount setter 字面量包 `new BigDecimal`）。**附带修复 contract 域预存 bug**：`ErpCtApprovalMatrix`/`ErpCtSignatureRequest` 错误的本模块实体重复外部声明（用错误短包名 `app.erp.ct`，真实为 `app.erp.contract`），被 codegen 重生成暴露，移除 2 个错误 stub。`S5 LocalDate.now()` 顺带修复为 `CoreMetrics.today()`。
  - Skill: none
- [ ] **Decision | Explore** projects 跨域引用策略（M1）：确认 purchase/sales 的"项目采购/项目销售"是否为产品基线稳定需求，据此形成**建议结论**：
  - 选项 A：改弱指针（`projectId` 字符串列 + `IErpPrjProjectBiz` 查询），符合现 `data-dependency-matrix.md`/`module-boundaries.md`（触 `*.orm.xml`，ask-first）。
  - 选项 B：修订 `data-dependency-matrix.md §5.6` 与 `module-boundaries.md` 将 purchase/sales→projects 纳入白名单并补 DAG 说明（纯文档，非保护区域）。
  - 记录理由；收敛"文档与实现不一致"到一方。
  - Skill: none
- [ ] **Fix** 将**已获人工批准的** projects Decision 结论落地到对应 owner doc。
  - Skill: none

Exit Criteria:

- [x] 两项 Decision 均有"建议结论 + 替代方案 + 残留风险"记录，保护区域项已标注 ask-first（O1 经人工批准 Option A；M1 选 Option B 纯文档）
- [x] 已获批准的结论已落地 owner doc 且无内部矛盾（O1 落地 10 域 ORM + codegen 重生成；M1 落地 data-dependency-matrix.md §5.6.1 + 域矩阵 projects 行 + module-boundaries.md projects 行与允许/禁止规则）

### Phase 1 — 服务层平台 API 合规整改（核心切片）

> 顺序说明：本计划唯一执行切片。S1/S2/S3/S4 集中在同一批文件，合并为一切片避免反复触碰。Phase 0 的 projects Decision 若选弱指针会影响 `projectId` 跨域访问写法——Phase 1 实施时按 Phase 0 已落地的结论执行；若 Phase 0 projects 结论未获批，Phase 1 先整改不依赖 projects 的部分（master-data 跨域读、Biz 注解、requireEntity、newEntity），projects 相关访问随后补。

Status: planned
Targets: purchase/sales/inventory/finance 四域的 `IErp*Biz` 接口 + `Erp*BizModel` + Converter/Builder/Checker/Bookkeeper + `ErpFinPostingService`；master-data `-dao` 的 `IErpMdPartnerBiz`（新增只读方法）
Skill: `nop-platform-conformance-audit-prompt`


- Item Types: `Fix`（本阶段 ~90% 项目为缺陷修复，统一声明 Fix-heavy）
- Prereqs: Phase 0 的 Decision 已记录（projects 弱指针选项会影响跨域访问改法）

- [ ] **Fix** Biz 注解对齐（S1）：为全部自定义写方法加 `@BizMutation`（移除冗余 `@Transactional`，`@SingleSession` 按需保留）、查询加 `@BizQuery`，参数加 `@Name`。同步到 `IErp*Biz` 接口声明。覆盖：`IErpPur{Requisition,Order,Receive}Biz`、`IErpSal{Order,Delivery,Quotation}Biz`、`IErpInvStockMoveBiz`、对应 BizModel 实现。
  - Skill: `nop-platform-conformance-audit-prompt`
- [ ] **Fix** 跨实体访问改 I*Biz（S2）：按下表**逐处**改造（同聚合子实体 `daoFor(ErpXxxLine.class)` 标准用法保留；同域内部辅助类的只读 DAO 访问见下表分类）。master-data 跨域读在 `IErpMdPartnerBiz`/`IErpMdAcctSchemaBiz`/`IErpMdSubjectBiz` 补声明所需只读方法（**契约面扩张**，回归 master-data 冒烟测试）。
  - Skill: `nop-platform-conformance-audit-prompt`

  **A. 跨聚合写（必须改 I*Biz，改另一单据状态）**：
  - `ErpPurRequisitionBizModel:193,229` `daoFor(ErpPurOrder.class)` 创建订单 → 注入 `IErpPurOrderBiz`（在 OrderBiz 增 `createFromRequisition` 或等价方法）
  - `ErpPurReceiveBizModel:249,288` `daoFor(ErpPurOrder.class)` 回写订单收货状态 → `IErpPurOrderBiz`
  - `ErpSalDeliveryBizModel:248,287` `daoFor(ErpSalOrder.class)` 回写订单发货状态 → `IErpSalOrderBiz`
  - `ErpSalQuotationBizModel:222,236` `daoFor(ErpSalOrder.class)` 创建订单 → `IErpSalOrderBiz`

  **B. 跨域读（必须改 I*Biz，绕过数据权限管道）**：
  - `ErpPurOrderBizModel:191` `daoFor(ErpMdPartner.class)` → `IErpMdPartnerBiz`
  - `ErpPurReceiveBizModel:339` `daoFor(ErpMdPartner.class)` → `IErpMdPartnerBiz`
  - `ErpPurReceiveBizModel:365` `daoFor(ErpInvStockMove.class)` 跨域读库存 → `IErpInvStockMoveBiz` 只读查询
  - `ErpSalOrderBizModel:191` `daoFor(ErpMdPartner.class)` → `IErpMdPartnerBiz`
  - `ErpSalDeliveryBizModel:338` `daoFor(ErpMdPartner.class)` → `IErpMdPartnerBiz`
  - `ErpSalDeliveryBizModel:364` `daoFor(ErpInvStockMove.class)` 跨域读库存（**审计初稿遗漏处**）→ `IErpInvStockMoveBiz`
  - `CreditLimitChecker:58` `daoFor(ErpMdPartner.class)` → `IErpMdPartnerBiz`
  - `ReceiveStockMoveBuilder:68` / `DeliveryStockMoveBuilder:66` `daoFor(ErpMdAcctSchema.class)` → `IErpMdAcctSchemaBiz`
  - `ErpFinPostingService:322` `daoFor(ErpMdSubject.class)` → `IErpMdSubjectBiz`

  **C. 保留（同聚合/同域内部，不改）**：所有 `daoFor(ErpXxxLine.class)`（头-行同聚合）；`ErpInvStockMoveBizModel`/`StockMoveBookkeeper`/`InvPostingDispatcher` 内的 `daoFor(ErpInvStock{Balance,Ledger,Move,MoveLine}.class)`（inventory 域内部，Bookkeeper 是 inv 内部组件）；`ErpFinPostingService` 内的 `daoFor(ErpFin{Voucher,VoucherLine,VoucherBillR,AccountingPeriod}.class)`（finance 域内部过账引擎写自身凭证）；`CreditLimitChecker:90` `daoFor(ErpSalOrder.class)`（sales 域内部读，同域辅助类——保留，但记录为后续可统一）。
- [ ] **Fix** `requireEntity` 对齐（S3）：自定义 `requireXxx()` 改用基类 `requireEntity(id, action, context)`，保留业务校验（状态前置等）。
  - Skill: none
- [ ] **Fix** `newEntity` 对齐（S4）：`new ErpXxx()` 改 `daoProvider().daoFor(X.class).newEntity()`，覆盖 Converter/Bookkeeper/PostingService。
  - Skill: none
- [ ] **Fix** 时间获取（S5）：`ErpInvStockMoveBizModel.java:251 LocalDate.now()` → `CoreMetrics.currentDate()`。
  - Skill: none
- [ ] **Proof** GraphQL 快照测试：为下述每个方法补 GraphQL mutation 快照（替代现"直调 Java API"），验证经 GraphQL 可达，**作为 S1 退出标准的实际承担者**（grep 仅辅助）。方法覆盖清单：`ErpPurRequisition.{submit,approve,convertToOrder}`、`ErpPurOrder.{submit,approve}`、`ErpPurReceive.{submit,approve}`、`ErpSalOrder.{submit,approve}`、`ErpSalDelivery.{submit,approve}`、`ErpSalQuotation.convertToOrder`、`ErpInvStockMove.{generateMove,confirm,reverse}`。沿用 `JunitAutoTestCase` + CHECKING 模式。
  - Skill: none

Exit Criteria:

- [ ] **S2 上表 A/B 两段列出的 18 处行级引用（跨聚合写 8 + 跨域读 10）逐处归零**（按"文件:行 → 改为 IXxxBiz"逐条勾选核验，不依赖正则）；C 段保留项与代码现状一致
- [ ] GraphQL 快照测试覆盖上方"方法覆盖清单"中每个方法且全绿（证明 S1 已暴露 GraphQL）
- [ ] master-data 冒烟测试无回归（`IErpMdPartnerBiz` 等接口扩张后）
- [ ] 四域 `mvn test -pl module-{purchase,sales,inventory,finance} -am` 通过

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（task `ses_0e2a46dedffetgfQ46401dk9Qf`）。发现 2 blocker + 3 major + 3 minor：
  - BLOCKER1：Phase 1 Exit Criteria 的 grep `daoFor(ErpMd|new Erp[A-Z]` 漏掉全部跨聚合同域写（receive→order、delivery→order 等）及 `ErpSalDeliveryBizModel:364` 跨域读库存（审计"正面"结论不完整）→ 已改为 17 处显式清单逐条核验。
  - BLOCKER2：O1 VARCHAR 涉及 `model/*.orm.xml` 保护区域（ask-first），且 Option B 设"无 successor"逃生口违反规则 13（已确认缺陷不得降级）→ 已加保护区域声明、删除逃生口。
  - MAJOR3：DRP 行号/数量/"基线正确"前提均错（实际 15/19/46，5 个 ErpInvDrp 资源，基线亦混用）→ DRP 已拆出为独立 successor，并在本计划证据订正。
  - MAJOR4：范围过大（4 结果表面）→ 已收敛为单一结果表面（服务层合规 + 阻塞 Decision），DRP/工具项拆出。
  - MAJOR5：I*Biz 接口契约为跨域依赖未标记 + delivery→inv 矛盾未察 → 已加契约面风险条目并修正 S2 表。
  - MINOR6/7/8：S1 退出标准改由 GraphQL 快照清单承担；启动校验命令具化；O1 编辑量已标注。
- Independent draft review iteration 2: `passes draft review`（task `ses_0e299b15cffef6SvYRpFXtQGq7`）。逐项核实 5 项修订 + 文本一致性 + 规则 13 + 反松弛规则均通过；抽查 S2 清单 10+ 处行号全部属实。已顺手订正计数口径（17→18，A=8+B=10）。剩余非阻塞风险：S5 `LocalDate.now()` 行号本次未独立复核（低风险）；`@SingleSession 按需保留`措辞可在实施时收紧为具体方法清单。计划转 `active`。

## Closure Gates

- [ ] 范围内行为完成（服务层 API 合规：S1/S2/S3/S4/S5 + GraphQL 快照）
- [ ] Phase 0 两项 Decision 有"建议结论"记录，保护区域项（O1/M1-弱指针）经人工批准后落地 owner doc
- [ ] 相关 owner docs 对齐（system-baseline / data-dependency-matrix / module-boundaries / domain-design-guidelines，仅限已获批结论）
- [ ] 已运行验证：`mvn test -pl module-{purchase,sales,inventory,finance} -am` 全绿；master-data 冒烟无回归；GraphQL 快照覆盖方法清单且全绿
- [ ] 无范围内项目降级为 deferred/follow-up（V2/O1 全量迁移/S7/DRP/工具项均显式拆出为独立 successor，非范围内缺陷降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status / 阶段 Status / Exit Criteria / Closure Gates / logs 一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### V1 DRP 菜单断链（拆出独立计划）

- Classification: `moved to explicit successor ownership`
- Why Not Blocking Closure: 视图层结果表面，与本计划（服务层合规）验证路径独立。实测断链 3 处：`erp-drp.action-auth.xml:15,19,46` 的 `ErpInvDrp{Plan,Line,Parameter}` 指向不存在的 page 目录（实际为 `ErpDrp*`）；另 2 个 `ErpInvDrp*` 资源（SafetyStockCalc/LeadTimeRecord）目录存在不断链；生成基线 `_erp-drp.action-auth.xml` 自身亦混用 `ErpInvDrp*`/`ErpDrp*`，extends 路径需先理基线命名。
- Successor Required: yes —— `docs/plans/2026-07-01-1900-2-drp-menu-broken-links.md`（待创建）

### 工具/bootstrap 统一项 M2/M3/O2/O3（拆出独立计划/backlog）

- Classification: `moved to explicit successor ownership`
- Why Not Blocking Closure: `codegen.sh` 陈旧路径、`validate-page-model=false`、零 `<index>`、dict valueType 不一致，均与服务平台 API 合规不同表面、不阻塞当前业务。
- Successor Required: yes —— 触发条件：bootstrap 杂项整理计划，或首次性能测试前（索引项）。

### V2 单据状态机按钮页面接线

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 视图层结果表面，依赖本计划 S1 GraphQL 暴露先落地；当前页面纯 CRUD 可运行（属未接线功能，非已确认缺陷）。
- Successor Required: yes —— 触发条件：本计划 Phase 1 完成后，独立视图层计划首批工作项。

### O1 金额族 VARCHAR→DECIMAL 全量数据迁移

- Classification: `watch-only residual`（Phase 0 Decision 选 A 时升级为独立迁移计划）
- Why Not Blocking Closure: 本计划 Phase 0 仅做 Decision + 保护区域登记；全量迁移脚本与回归范围独立。
- Successor Required: yes —— 无论 Phase 0 选 A/B 均保留 successor：选 A → 创建迁移计划；选 B → 触发条件为"首次涉及金额聚合查询性能/凭证平衡精度问题"时重开。**O1 为已确认缺陷，不设可永久关闭路径。**

### S7 复杂流程改 task.xml/xbiz 编排

- Classification: `optimization candidate`
- Why Not Blocking Closure: 现有 Java 实现业务逻辑正确；编排方式优化为 enhancement（ai-defaults 决策顺序允许 Java 兜底）。
- Successor Required: no（按域深化时按需采用）

## Closure

Status Note: pending（待执行与独立结束审计）

Closure Audit Evidence:

- Auditor / Agent: <待独立子代理填充>
- Evidence: <task id / walkthrough record>

Follow-up:

- V1 DRP 菜单断链 → 独立 successor `2026-07-01-1900-2-drp-menu-broken-links.md`（实测断链 3 处：`erp-drp.action-auth.xml:15,19,46`）
- 工具/bootstrap 统一项 M2/M3/O2/O3 → 独立 successor 或 backlog
- V2 状态机按钮接线（见 Deferred，独立视图层计划）
- O1 全量迁移（Phase 0 选 A 时创建迁移计划）
- 8 域 action-auth 统一为 `x:extends` 生成基线（V3，bootstrap 期非阻塞）
- dashboard 占位页补齐（V4，按域深化）
- FK 字段 picker 控件（bootstrap 期可接受）
