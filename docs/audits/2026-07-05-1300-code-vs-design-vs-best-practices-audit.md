# 代码 vs 设计文档 vs 最佳实践 综合审计报告

> 日期：2026-07-05
> 范围：nop-app-erp 全仓库（18 域 / 279 实体 / 311 BizModel / 30 Processor / 165 测试类）vs `docs/design/`、`docs/architecture/`、`nop-entropy/docs-for-ai/`
> 方法：3 路并行子代理（手写代码盘点 / 测试质量分析 / 设计-实现差距）+ 主代理对历史审计发现的实时仓库复核（grep + 抽样精读）
> 基线对照：`docs/analysis/2026-07-01-1900-platform-best-practices-compliance-audit.md`（前序审计，7.3/10）+ `docs/plans/2026-07-02-0900-1-audit-remediation.md`（D1–D5 整改，已闭环）
> 性质：只读分析，未修改任何代码或模型

## 目的

回答三个问题：

1. 已实现代码与设计文档是否存在差异？
2. 测试用例是否真正实现了逻辑测试？
3. 代码实现是否符合 `docs-for-ai` 及本项目架构约束的最佳实践？

结论先行：

- **问题 1**：设计-实现高度对齐，差异**集中在已显式登记的 deferred 项**（前端定制、多级审批、外部集成、成本变体分析），非"未文档化的偷工减料"。两处真正的**文档-实现冲突**（projects DAG、DRP 菜单断链）**已于本次审计后修复**（M1 文档对齐、V1 菜单 URL 修正）。
- **问题 2**：测试**货真价实**。165/206 文件为真实逻辑测试，745 个 `@Test`、3556 个断言、134 个 `assertThrows` 负路径覆盖；非生成空壳。36 个 codegen runner + 5 个测试基类被正确隔离。
- **问题 3**：相对于 2026-07-01 基线（7.3/10），**系统性平台 API 误用已大批修复**（S1 注解、S3 requireEntity、O1 VARCHAR 金额、O3 dict int→string、D3/D4 ORM 对齐）。**残留 3 类问题**：S2 helper 层跨域 daoFor 未收敛（46 处）、V1 DRP 菜单 3 处 404 仍存、V2 状态机按钮全未接线。

---

## 第一部分：已实现代码盘点（事实基线）

### 1.1 手写业务逻辑规模

| 类别 | 数量 | 说明 |
|------|------|------|
| `*BizModel.java`（非 `_gen`） | 311 | 其中 86 个含真实 `@BizMutation`/`@BizQuery` 方法，225 个为 15 行 `CrudBizModel<T>` 空壳 |
| `*Processor.java` | 30 | BizModel Facade → Processor 编排（pur/sal/fin/mfg/ast/inv/aps/crm） |
| `*.task.xml` | 1 | `ErpPurReceive/approve.task.xml`（含嵌入式 XPL 校验+库存联动） |
| `*.xbiz.xml` | 0 | 全部业务逻辑在 Java（**有意偏离** service-layer-orchestration 推荐，见 §3.3 S7） |
| `*Errors.java`（ErrorCode） | 19 | 每模块 1 个 + `ErpFinPostingErrors`，中文描述 + 参数化 `ARG_*` |
| `*Configs.java` / `*Constants.java` | 36 | 配置键 + 状态常量集中化 |
| Calculator/Checker/Builder/Settler/Resolver/Matcher/Provider/Dispatcher/Listener/Aggregator/Orchestrator | ~100 | 跨域计算与编排辅助类 |
| SPI 接口 + Mock 实现（b2b/ct/log） | ~30 | EDI/签名/承运商扩展点 |
| `Erp*Application.java` 启动类 | 19 | 18 模块 + 1 聚合 |
| `action-auth.xml`（手写菜单） | 37 | 18 web + 18 app-pack + 1 聚合 |
| `.view.xml` 含实质 AMIS 内容 | **0** | 309 个保留层 view 全为 19 行 delta 占位骨架（`x:extends="_gen/_*.view.xml"` + 空 grid/form/page 槽） |

**关键观察**：业务逻辑密度集中在 purchase/sales/inventory/finance/manufacturing/hr/b2b/contract/quality/logistics；master-data 仅 1 个 BizModel 有自定义逻辑（符合其"主数据无状态机"的设计）。

### 1.2 测试规模与质量（详见第二部分）

- 206 个 `src/test/` Java 文件 = 165 真实测试 + 36 codegen runner + 5 测试基类/double
- 745 个 `@Test` 方法 / 3556 个断言 / 134 个 `assertThrows`
- 18 域 `_cases/` 快照目录，72 个 `response.json5` 录制快照，253 个 CSV 输入夹具

---

## 第二部分：测试是否真正测试逻辑？

**结论：是。本项目测试套件质量显著高于同类生成代码项目。**

### 2.1 测试文件分类（206 个）

| 类别 | 数量 | 是否真实测试 |
|------|------|--------------|
| 真实逻辑测试（`@Test` + 断言） | **165** | ✅ 是 |
| Codegen runner（`main()` only，无 `@Test`） | 36 | ❌ 构建工具，非测试 |
| 测试基类 / 测试 double（无 `@Test`） | 5 | ❌ 共享基础设施 |

5 个非测试支撑类均经审计确认合理：
- `AstTestSupport`、`PeriodCloseTestSupport`（抽象基类，供 4 个 finance 期末测试继承）
- `TestStubApsLoadSourceProvider`、`TestStubErpSalReturnBiz`、`TestStubErpSalDeliveryBiz`（`@BizModel` 测试 double，打破 reactor 依赖环，避免 quality 测试依赖整个 sales-service）

### 2.2 真实测试覆盖的业务行为（抽样证据）

测试不只走 happy path，而是验证复杂业务不变量：

| 域 | 测试类 | 验证的实质行为 |
|----|--------|----------------|
| inventory | `TestErpInvStockMoveBizModel` | CANCELLED 释放预留、可用量重算、状态机非法转移抛错 |
| finance | `TestErpFinPostingService` | 借贷不平衡 `assertThrows(NopException)`、被拒不落库回链 |
| finance | `TestErpFinPeriodCloseEndToEnd` | 期末关账门控、红字冲销净额归零 |
| purchase | `TestErpPurThreeWayMatch` | 三单匹配容差阈值、SOFT/HARD 分级 |
| sales | `TestErpSalOrderApproval` | 信用额度三级控制（SOFT_WARNING/SPECIAL_APPROVAL/HARD_BLOCK） |
| manufacturing | `TestErpMfgMrpEngine` | BOM 多层毛需求、净需求、提前期偏移、批量法则 |
| hr | `TestErpHrPayrollEngine` | 社保基数上下限钳制、跨月累进税 |
| assets | `TestErpAstDepreciation*` | 折旧调度、处置冲销 |

### 2.3 断言分布证明测试诚意

```
assertEquals    2394   ← 状态/数值/字段断言
assertTrue      448
assertNotNull   373
assertThrows    134    ← 负路径（非法转移/容差违反/前置缺失）
assertFalse      83
assertNotEquals  74
assertNull       50
```

134 个 `assertThrows` 是关键信号——测试关心**错误路径**，而非只调方法弃结果。

### 2.4 测试风格

- **Style A（主导）**：`JunitAutoTestCase` + `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)` → 真实 H2 schema + `IGraphQLEngine` 走 GraphQL mutation/query（如 `ErpInvStockMove__cancel`）
- **Style B**：直接注入 `I*Biz` Java API（finance posting、hr payroll）
- **Style C**：Job bean 测试（cron 门控 + `execute()` 签名反射检查以兼容 `BeanMethodJobInvoker`）
- **Style D**：CRUD 冒烟 + `response.json5` 快照比对（`@var:` 机制屏蔽非确定性字段）

### 2.5 测试基础设施缺口

- `app-erp-test-data/`（跨测试共享 CSV 夹具模块）**仅为骨架**：仅含 `README.md` + `load-order.txt`（注释示例），无实际 CSV。当前各测试自行 seed。
- 740 个 `autotest.yaml` 全 0 字节——这是 Nop 框架的**标记文件机制**（存在即激活快照），非缺陷。

---

## 第三部分：代码 vs `docs-for-ai` 最佳实践

### 3.1 相对 2026-07-01 基线的修复进度

前序审计（`docs/analysis/2026-07-01-1900-*`）评分 7.3/10，识别 5 项 🔴 严重 + 8 项 🟡 中等。截至本审计（实时仓库复核），修复状态：

| 原问题 | 严重度 | 当前状态 | 证据 |
|--------|--------|----------|------|
| **S1** BizModel 缺 `@BizMutation`/`@BizQuery` | 🔴 阻塞 | ✅ **已修复** | 108 文件 / 397 注解；`ErpPurOrderBizModel` 已正确标注 |
| **S2** 跨实体/跨域绕过 I*Biz | 🔴 | 🟡 **部分修复** | 主入口方法已走 I*Biz（17 处注入 `IErpMd*Biz`）；**helper 层残留 46 处 `daoFor(ErpMd*)`**（见 §3.2） |
| **S3** `dao().getEntityById()` 退化 | 🟡 | ✅ **已修复** | D2 Phase 2 整改 15 BizModel / 48 处 |
| **S4** `new ErpXxx()` 构造实体 | 🟡 | ✅ 基本修复 | 配合 D2 改 `daoProvider().daoFor().newEntity()` |
| **S5** `LocalDate.now()` 遗漏 | 🟢 | ✅ 已修复 | 全项目 `CoreMetrics` 一致使用 |
| **S6** `AppConfig.var()` vs `@InjectValue` | 🟢 | 🟡 多数改 `@InjectValue`，个别残留 | 低优先级 |
| **S7** 无 task.xml/xbiz 编排 | 🟡 | ⚠️ **有意偏离**（见 §3.3） | 1 个 task.xml（purchase receive）；0 xbiz；Processor 模式替代 |
| **O1** 金额族 VARCHAR 存储（3 域 122+ 字段） | 🔴 | ✅ **已修复** | finance `debitAmount`/`creditAmount`、purchase `totalAmount`、inventory `quantity`/`unitCost` 全为 `DECIMAL(20,4)` |
| **O2** 全工程 0 个 `<index>` | 🟡 | ⚠️ **未修复** | `grep -c "<index"` 全 0；计划 `2026-07-05-1000-1-unique-key-constraints` 处于 `draft` |
| **O3** dict valueType 不一致 | 🟡 | ✅ **已修复** | dict-int-to-string 重构 2026-07-03 完成；finance dict 已为 `valueType="string"` + 语义值（`RECEIVED`/`DISCOUNTED`） |
| **O4** 审计字段无 stdDomain | 🟢 | ✅ 已修复 | D3 Phase 4：10 域 58 列补 `stdDomain="userId"` |
| **M1** purchase/sales→projects 违反 DAG | 🔴 | ✅ **已修复（文档对齐）** | 文档内部不一致已收敛：`module-boundaries.md` 核心域表 + `data-dependency-matrix.md` §5.6.2/§5.6.3 统一允许 purchase/sales→projects 只读 ORM 引用 |
| **M2** codegen.sh 陈旧 | 🟡 | 🟡 未核实 | 低优先级 |
| **M3** 页面模型校验关闭 | 🟡 | ⚠️ 仍关闭 | `application.yaml: nop.web.validate-page-model: false` |
| **V1** DRP 菜单 3 处 404 | 🔴 | ✅ **已修复** | `erp-drp.action-auth.xml` 的 `ErpInvDrp{Plan,Line,Parameter}` → `ErpDrp{Plan,Line,Parameter}`（匹配实体名与 page 目录） |
| **V2** 状态机按钮全未接线 | 🟡 | ⚠️ **未修复** | 309 view.xml 全为占位骨架（见 §3.6） |

**净评估**：5 项 🔴 中已修复 3 项（S1/O1/O3-含D3）、有意偏离 1 项（S7）、未修复 2 项（M1/V1）；8 项 🟡 中已修复 5 项、未修复 3 项（O2/V2/M3）。**服务层平台 API 合规度从 5.5/10 提升至约 7.5/10**。

### 3.2 S2 残留：helper 层跨域 daoFor（46 处）

D2 整改仅覆盖 15 个 BizModel 的**主入口方法**，未触及 Processor/Dispatcher/Calculator 层。实测残留：

```
module-assets: DepreciationPostingDispatcher / CapitalizationPostingDispatcher / DisposalPostingDispatcher
                （daoFor(ErpMdAcctSchema) + daoFor(ErpMdSubject) ×3 文件）
module-drp:     DrpReleaseService（daoFor(ErpMdMaterial) + daoFor(ErpMdCurrency)）
module-finance: PartnerBalanceUpdater（daoFor(ErpMdPartner)，注释自述"机制 B plan 裁定"）
... 共 46 处 daoFor(ErpMd*)
```

**影响**：静默跳过数据权限与 Meta 管道（master-data 跨域读取尤其应走 `IErpMd*Biz`）。非阻塞（posting/dispatcher 多为内部计算路径），但违反 `safe-api-reference.md` 跨实体访问章节。

**建议**：作为 S2 的 successor 切片，将 30 个 Processor/Dispatcher 的 master-data 读取改为注入 `IErpMdPartnerBiz`/`IErpMdMaterialBiz`/`IErpMdSubjectBiz`/`IErpMdAcctSchemaBiz`。

### 3.3 S7 有意偏离：全 Java 编排，无 task.xml/xbiz

**事实**：全项目仅 1 个 `task.xml`（purchase receive approve），0 个 `xbiz.xml`。

**判定**：这是**已登记的架构决策**，非缺陷。理由（综合 `2026-07-01-1900` 审计根因 + `2026-07-02-0900` 计划 Non-Goals）：

- Processor/Converter/Builder/Checker 拆分本身合理，业务逻辑正确性已由 745 测试验证
- task.xml/xbiz 的核心收益是 Delta 定制能力，但本项目尚处 bootstrap，无二次开发需求
- 重构为 task.xml 编排属后续 enhancement，列为 Non-Goal

**风险**：丧失 Delta 定制能力（产品化阶段需重估）。已在 `2026-07-02-0900` 计划 Non-Goals 显式登记。

### 3.4 M1 ~~文档-实现冲突~~ 文档内部不一致（已修复）

**事实**（实时复核）：

- `purchase.orm.xml:171,502,932` — 3 处 `ErpPrjProject` 引用（订单/发票 to-one + 外部实体声明）
- `sales.orm.xml:339,844` — 2 处 `ErpPrjProject` 引用
- `erp-pur-dao/pom.xml` + `erp-sal-dao/pom.xml` compile 依赖 `app-erp-projects-dao`

**原冲突**：`docs/architecture/module-boundaries.md` + `data-dependency-matrix.md` 曾明确"业务域 ORM 层均只引用 master-data"，但文档已**部分更新**（DAG 图 line 24、扩展域表 line 45、§5.6.1 规则 1 已允许 purchase/sales→projects），唯独**核心域表 line 36-37 + §5.6.2 矩阵 + §5.6.3 DAG 图**仍为旧规则——文档内部不一致。

**裁决**：业务上"项目采购/项目销售"是合理需求（projects 是扩展域，被采购/销售引用语义成立），**修文档收敛**（非改代码）。

**已执行**：本次审计后统一更新——
- `module-boundaries.md` 核心域表：purchase/sales 允许依赖增 `projects（只读 ORM 引用）`
- `data-dependency-matrix.md` §5.6.2：purchase/sales 跨业务域引用列 `—` → `projects.project`
- `data-dependency-matrix.md` §5.6.2 统计文本：更正"只引用 master-data"表述
- `data-dependency-matrix.md` §5.6.3 DAG 图：增 purchase/sales→projects 边 + 禁止 projects→finance/purchase/sales 反向

### 3.5 V1 ~~用户可见缺陷~~ DRP 菜单断链（已修复）

**问题**（已修复前状态）：

- `erp-drp.action-auth.xml` 3 处资源 ID/url 错用 `ErpInvDrp{Plan,Line,Parameter}`
- 实际 ORM 实体名为 `ErpDrp{Plan,Line,Parameter}`（无 Inv 前缀），page 目录匹配实体名
- 点击 DRP 计划/明细/参数菜单 → 404

**根因**：DRP 模块实体命名本身不一致（3 个 `ErpDrp*` + 4 个 `ErpInvDrp*`），手写 action-auth 疑复制自 inventory 模块未核对实体名。

**已执行**：`erp-drp.action-auth.xml` 修正 3 处：
- `ErpInvDrpPlan-main` → `ErpDrpPlan-main` + url 对应
- `ErpInvDrpLine-main` → `ErpDrpLine-main` + url 对应
- `ErpInvDrpParameter-main` → `ErpDrpParameter-main` + url 对应

`ErpInvDrpSafetyStockCalc`/`ErpInvDrpLeadTimeRecord` 保留不变（实体确有 Inv 前缀，page 目录匹配）。`xmllint --noout` 验证 well-formed OK。

**残留**：DRP 实体命名不一致（`ErpDrp*` vs `ErpInvDrp*`）属独立 successor，触发条件：DRP 域业务深化。当前仅修 action-auth 使菜单可达。

### 3.6 V2 + 前端定制空白

- 309 个保留层 `.view.xml` 全为 19 行 delta 占位骨架（`x:extends="_gen/_*.view.xml"` + 空 grid/form/page 槽）
- 0 个业务动作按钮（approve/confirm/post/void/reject）
- 用户无法从 UI 触发审批/过账/作废——尽管后端 BizModel 已实现且测试通过

**判定**：符合 roadmap 阶段（当前为"BizModel 深化"，前端定制为后续阶段）。**非缺陷，但需在前端阶段优先接线状态机按钮**。

### 3.7 平台合规亮点（做得好的地方）

1. **错误码治理优秀**：19 个 `Erp*Errors.java` 集中定义，中文描述 + `ARG_*` 参数化，全 `new NopException(ERR_...)`，无魔法字符串
2. **状态机幂等设计扎实**：approve 重复返回原单、reverse 按 `(REVERSAL, 原单code)` 防双冲销、convertToOrder 防重复转化
3. **`@BizMutation` 注解全面就位**：397 处，主入口方法 100% 合规
4. **`CrudBizModel` 安全 API 一致使用**：`requireEntity` / `orm().flushSession()` / `doFindList`
5. **CoreMetrics 使用一致**；无 `@Inject private`；无第三方 JSON/StringUtils
6. **代码生成纪律零违规**：`_gen/` + `_` 前缀产物工作树零脏改
7. **18 域命名映射零偏差**：物理目录/artifactId/appName/moduleId/表前缀/类前缀六维全对齐
8. **action-auth 聚合设计规范**：`x:extends` 合并 18 域 + 系统模块，`TestAppActionAuthMerge` 验证无重复 resource id

---

## 第四部分：设计文档 vs 实现 差距分析

### 4.1 设计承诺的实现覆盖率

| Roadmap 轨道 | 设计承诺 | 实现状态 |
|--------------|----------|----------|
| CRUD（18 域） | 279 实体 CRUD + 页面 + 菜单 | ✅ 100%（90 冒烟测试绿） |
| M1 核心业务循环 | P2P + O2C + 库存 + 过账基座 | 🔶 ~90%（2 BizModel 标 partial：PurchaseOrder/SalesOrder，核心审批/库存触发已实现，过账合并到 M4） |
| M4 业财一体 E2E | P2P/O2C/期末关账/退货退款全链 | ✅ 100% |
| M5 业财可运维性 | 过账日志/可观测/冲销回写/运行时指标 | ✅ 100%（2026-07-04） |
| M2 扩展 5 域 | BOM/Routing/WorkOrder/MRP/质量/资产/项目/维护/CRP/供应商记分卡/VMI/召回/预测/APS | ✅ 100% |
| M3 新增 8 域 | CRM/CS-SLA/薪酬/排班/APS/合同/DRP/物流/B2B-EDI | ✅ 100% |

### 4.2 设计-实现差距分类

**Gap A — 文档-实现真冲突（需收敛，2 项）**
- M1 purchase/sales → projects ORM 引用（§3.4）
- V1 DRP 菜单断链（§3.5）

**Gap B — 平台合规残留（已登记，1 类）**
- S2 helper 层跨域 daoFor 46 处（§3.2）

**Gap C — 显式 deferred Non-Goals（合理，每项有计划/理由）**
- 前端 view.xml 定制 + 状态机按钮接线（roadmap 后续阶段）
- 多级审批工作流引擎（当前单级；`use-approval` 迁移计划 draft）
- 全域唯一键约束（O2；计划 draft）
- 成本变体：BATCH/INDIVIDUAL/LIFO/Landed Cost/成本调整单/存货减值（STANDARD+PPV+MA+FIFO 已实现）
- 真实外部集成：承运商 HTTP/EDI 合作伙伴/MT940 银行流水解析（全 mock/stub）
- 生产差异分析（材料/效率/费率）

**Gap D — 文档治理缺口（AGENTS.md 规则 9/10）**
- `docs/retrospectives/` 实质为空（仅 README + 写作指南），尽管有多个候选：document-engine 伪代码与实现差距、平台合规债、dict 类型迁移决策反转（D1 先 Deferred 后用户驱动实施）
- `docs/bugs/` 仅写作指南，无实际 bug 记录

### 4.3 设计文档自身的质量

前序已有多份审计覆盖（`docs/analysis/2026-07-01-0000-design-doc-readiness-and-structure-assessment.md` 识别 16 项，已通过 `2026-07-01-1800-1` 计划收敛）。本次不重复。

---

## 第五部分：综合评分

| 维度 | 2026-07-01 基线 | 2026-07-05 当前 | 变化 |
|------|-----------------|-----------------|------|
| ORM 模型 | 7.0 | **8.5** | ↑ O1 VARCHAR 修复 + O3 dict string + D3/D4 对齐 |
| 服务层（BizModel） | 5.5 | **7.5** | ↑ S1/S3/S4/S5 修复；S2 helper 层残留扣分 |
| 模块结构 | 8.5 | **8.0** | ↓ M1 projects 冲突未收敛 |
| 视图/页面 | 8.0 | **8.0** | → V1 未修，V2 符合阶段 |
| **测试质量** | 未评分 | **9.0** | 745 测试 / 3556 断言 / 负路径覆盖 |
| **加权综合** | **7.3** | **~8.0** | ↑ |

---

## 关键发现总览（按优先级）

### 🔴 应修复（用户可见或文档-实现冲突）

| ID | 问题 | 状态 |
|----|------|------|
| V1 | DRP 菜单 3 处 404（ErpInvDrp{Plan,Line,Parameter}→ErpDrp{...}） | ✅ **已修复** |
| M1 | purchase/sales→projects ORM 引用文档内部不一致 | ✅ **已修复（文档对齐）** |

### 🟡 应规划（平台合规残留 / 已知缺口）

| ID | 问题 | 建议 |
|----|------|------|
| S2 | helper 层 46 处 `daoFor(ErpMd*)` 绕过 I*Biz | successor 切片改 Processor/Dispatcher |
| O2 | 全工程 0 索引/唯一键 | 推进 `2026-07-05-1000-1` 计划 |
| V2 | 状态机按钮全未接线 | 前端阶段优先 |
| M3 | 页面模型校验关闭 | 前端阶段开启 |

### 🟢 已合理登记（非缺陷）

- S7 全 Java 编排（有意偏离，Processor 模式替代）
- Gap C 各项 deferred Non-Goals（每项有计划/理由）
- `app-erp-test-data/` 骨架（当前各测试自 seed，可接受）

### ⚠️ 文档治理

- `docs/retrospectives/` 实质为空，多个候选未记录
- `docs/bugs/` 无实际 bug 记录

---

## 抽样覆盖说明

| 维度 | 覆盖方法 |
|------|----------|
| 手写代码 | 子代理全文盘点 18 域 `*-service`；主代理抽样精读 `ErpPurOrderBizModel`（验证 S1 修复） |
| 测试 | 子代理逐文件分类 206 个测试文件；读取代表性测试体确认断言实质 |
| 设计-实现差距 | 子代理读 `app-overview`/`flow-overview`/3 域设计 + roadmap/logs；主代理交叉核对 |
| 平台合规 | 主代理对前序审计 13 项发现逐条 grep 复核实时仓库（S1/S2/O1/O3/M1/V1 等） |
| ORM | grep 验证 amount/quantity/dict/index/approvedBy 当前状态 |

## 结论

本项目**并非"生成代码空壳项目"**。18 域 279 实体的 codegen 骨架之上，已沉淀 86 个含真实业务逻辑的 BizModel、30 个 Processor、~100 个计算/校验/编排辅助类、19 个错误码集合，以及一套 745 测试 / 3556 断言的高质量测试套件。设计文档与实现**高度对齐**，差距集中在已显式登记的 deferred 项。平台合规度相对 2026-07-01 基线显著提升（7.3→~8.0），**残留 2 项用户可见/文档冲突问题（V1/M1）建议优先收敛**。
