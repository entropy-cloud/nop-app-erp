# 平台最佳实践合规审计报告

> 日期：2026-07-01
> 范围：基于 ERP roadmap 已编写的代码（CRUD 18 域 + P1 核心业务逻辑部分段）vs `nop-entropy/docs-for-ai/` + 本项目 `docs/architecture/` 最佳实践
> 方法：4 维度并行子代理审计（ORM 模型 / 服务层 / 模块结构 / 视图层）+ 主代理关键结论 grep 复核
> 配套执行计划：`docs/plans/2026-07-01-1900-1-platform-compliance-remediation.md`

## 目的

回答一个问题：**已经根据 ERP roadmap 编写的代码，是否满足 `docs-for-ai` 以及本项目架构设计中描述的最佳实践？**

结论先行：**结构与模型层达到平台优秀水准（~8.5/10），主要短板集中在自定义服务层（~5.5/10）——业务逻辑正确，但平台 API 用法系统性偏离规范。** 综合约 **7.3 / 10**。

## 审计基线（当前代码范围）

| 维度 | 已完成范围 | 来源 |
| --- | --- | --- |
| CRUD | 18 域全绿（90 冒烟方法通过） | `docs/backlog/crud-roadmap.md` |
| P1 业务逻辑 | 请购→订单转化、订单三轴审批、入库/出库触发库存、StockMove、过账引擎基座（5 段 done/partial） | `docs/backlog/core-business-roadmap.md` |
| 自定义 Java | 集中在 purchase / sales / inventory / finance 四域的 BizModel + Converter/Builder/Bookkeeper/Checker + posting Provider | 见各 `*-service` 模块 |

## 评分汇总

| 维度 | 评分 | 一句话结论 |
| --- | --- | --- |
| ORM 模型 | 7.0 / 10 | 命名/关系/字典/跨域引用优秀，但 3 核心域金额族大规模 VARCHAR 违反硬规则 |
| **服务层（BizModel）** | **5.5 / 10** | 业务设计扎实，但存在**系统性平台 API 误用**（最严重） |
| 模块结构 / 代码生成纪律 | 8.5 / 10 | 模块链、命名、生成物纯净度高度合规 |
| 视图 / 页面 | 8.0 / 10 | 三层架构零违规，DRP 菜单断链 + 状态机按钮未接线 |

---

## 第一部分：服务层（BizModel）— 主要短板

> 这是本次审计发现的**最集中、最系统**的问题域。业务逻辑本身（状态机、幂等、跨域编排）设计正确，但承载它的平台 API 用法偏离 `ai-defaults.md` 反模式表。

### S1. 🔴 BizModel 方法系统性缺失 `@BizMutation/@BizQuery` 注解（阻塞性）

**证据**：全项目 `grep -rn "@Biz(Mutation|Query|Action)" --include=*.java` 在 main 代码中**命中 0 次**。所有自定义写方法（submit/approve/reject/cancel/reverseApprove/convertToOrder/generateMove/confirm/complete/reverse）一律用 `@SingleSession @Transactional` 代替。

```
IErpPurOrderBiz.java:27-49  —— 接口方法无任何 Biz 注解
ErpPurOrderBizModel.java:52-53  @SingleSession @Transactional public ErpPurOrder submit(...)
IErpSalOrderBiz / IErpSalDeliveryBiz / IErpInvStockMoveBiz（generateMove/confirm/complete/cancel/reverse 全无注解）
```

**后果**：
- 方法无法被 `BizProxyInvocationHandler` 识别路由 → 无法生成 GraphQL schema → 前端无法调用。
- 参数无 `@Name` → GraphQL 入参无法绑定。
- 测试被迫声明"直接调用 Java API，不走 GraphQL 快照"（`TestErpSalOrderApproval.java:28`），回避了真正应验证的路径。

**规则出处**：`ai-defaults.md` 反模式表「`@BizMutation @Transactional` → 只保留 `@BizMutation`」、`service-layer.md`「I*Biz 方法必须有注解」。

**修复**：写操作标 `@BizMutation`（自动包事务），查询标 `@BizQuery`；移除冗余 `@Transactional`；参数加 `@Name`。`@SingleSession` 按需保留。

### S2. 🔴 跨实体/跨域访问绕过 I*Biz 管道

**证据**：BizModel 中跨实体普遍用 `daoFor(X.class)` / `daoProvider().daoFor()`，静默跳过数据权限与 Meta 管道。

需精确区分两类（审计初稿把同聚合访问也计为违规，此处复核对齐平台基线）：

| 类别 | 判定 | 证据示例 |
| --- | --- | --- |
| 同聚合子实体访问（如订单→订单行） | **可接受**（CrudBizModel 标准 `daoFor()` 用法） | `ErpPurOrderBizModel.java:202 daoFor(ErpPurOrderLine.class)` |
| 跨聚合同域写（如入库单改采购订单） | **违规** | `ErpPurReceiveBizModel.java:249,288 daoFor(ErpPurOrder.class)` 直接改订单、`ErpSalDeliveryBizModel.java:248,287` 同 |
| **跨域读**（master-data 实体） | **违规** | `ErpPurOrderBizModel.java:191 daoFor(ErpMdPartner.class)`、`ErpPurReceiveBizModel.java:339`、`ErpSalOrderBizModel.java:191`、`ErpSalDeliveryBizModel.java:338`、`CreditLimitChecker.java:58 daoProvider.daoFor(ErpMdPartner.class)` |
| 跨域读 AcctSchema（master-data） | **违规** | `ReceiveStockMoveBuilder.java:68`、`DeliveryStockMoveBuilder.java:66`、`ErpFinPostingService.java:322 daoFor(ErpMdSubject.class)` |

**正面**：库存联动**生成/冲销**正确走了 I*Biz —— `ErpPurReceiveBizModel`/`ErpSalDeliveryBizModel` 注入 `IErpInvStockMoveBiz` 调 `generateMove/reverse`，未直连 inv 表生成移动单。这是正确范例。**注**：`ErpSalDeliveryBizModel:364` 与 `ErpPurReceiveBizModel:365` 另有跨域 `daoFor(ErpInvStockMove.class)` 的**只读查询**（非生成路径）仍属 S2 违规，需一并改 I*Biz。

**规则出处**：`ai-defaults.md:74`、`safe-api-reference.md` 跨实体访问章节「业务代码：注入 `I*Biz` 接口」。

**修复**：跨聚合/跨域访问改为注入对应 `I*Biz`（`IErpMdPartnerBiz`/`IErpPurOrderBiz`/`IErpSalOrderBiz`），master-data 跨域读取尤其必须走 I*Biz。

### S3. 🟡 实体获取退化为 `dao().getEntityById()` + 手工抛错

**证据**：`ErpPurOrderBizModel.java:164`、`ErpPurReceiveBizModel.java:312`、`ErpInvStockMoveBizModel.java:299` 等全部自定义 `requireXxx()` 私有方法，本质是 `dao().getEntityById(id)` + 判空抛 `NopException`，未用基类 `requireEntity(id, action, context)`，绕过统一管道（`afterEntityChange` 等）。

**规则出处**：`ai-defaults.md:71`「`dao().getEntityById(id)` 作为 BizModel 模板 → `requireEntity(id, action, context)`」。

### S4. 🟡 直接 `new ErpXxx()` 构造实体（Delta 派生类增强丢失）

**证据**：`RequisitionToOrderConverter.java:42 new ErpPurOrder()`、`ErpInvStockMoveBizModel.java:247,272`、`StockMoveBookkeeper.java:78,155`、`ErpFinPostingService.java:195,223,254`。

**规则**：应用 `daoProvider().daoFor(X.class).newEntity()` 以兼容 Delta 派生实体类。

### S5. 🟢 `LocalDate.now()` 遗漏（同文件却用了 `CoreMetrics.today()`）

**证据**：`ErpInvStockMoveBizModel.java:251 move.setBusinessDate(... LocalDate.now())`，而同文件 `:143` 用了 `CoreMetrics.today()`。这是唯一的时间获取违规（其余 99% 正确使用 `CoreMetrics`）。

### S6. 🟢 配置注入用 `AppConfig.var(...)` 而非 `@InjectValue`

**证据**：`CreditLimitChecker.java:84`、`ErpInvStockMoveBizModel.java:373`。平台推荐稳定配置用 `@InjectValue` 字段注入。

### S7. 🟡 复杂流程全 Java，无 task.xml/xbiz 编排

**证据**：全项目 0 个 `.xbiz.xml`（grep 确认）。审批/转化/过账触发全 Java 实现，未采用 `service-layer-orchestration.md` 推荐的"task.xml 编排 + I*Biz 逻辑"理念，丧失 Delta 定制能力。Processor/Converter/Builder 拆分本身合理。

### S8. 🟢 包结构与重复代码

- `CreditLimitChecker`/`ReceiveStockMoveBuilder`/`DeliveryStockMoveBuilder` 放在 `service.entity` 包，语义不符，宜 `service.processor`。
- pur/sales 的 Order/Delivery BizModel 状态机逻辑几乎逐行镜像；`resolveAcctSchemaId` 在两个 Builder 完全相同；`currentUserId()` 每个 BizModel 重复定义。可抽公共基类/工具。

### 服务层亮点（做得好的地方）

1. **错误码治理优秀**：`ErpPurErrors`/`ErpSalErrors`/`ErpFinPostingErrors` 集中定义，中文描述 + 参数化（`ARG_*`），按单据作用域细分，无魔法字符串。全部 `new NopException(ERR_...)`。
2. **状态机幂等设计扎实**：approve 重复返回原单、reverse 按 `(REVERSAL, 原单code)` 防双冲销、convertToOrder 防重复转化。
3. **`InvPostingExecutor` 用 `REQUIRES_NEW`** 独立事务隔离过账失败 —— 属 `ai-defaults.md` 明确允许的例外场景。
4. **`InvAcctDocProvider` 纯计算无副作用**，Provider 注册中心冲突裁决 fail-fast。
5. **CoreMetrics 使用一致**（除 1 处遗漏）；无 `@Inject private`；无第三方 JSON/StringUtils；无手改生成文件。

---

## 第二部分：ORM 模型

### O1. 🔴 金额/数量族字段 VARCHAR 存储（3 核心域，122+ 字段）

**证据**：
- **purchase**（46 处）：`purchase.orm.xml:332-339 amountSource/amountFunctional/totalAmount/totalTaxAmount/totalAmountWithTax/discountRate/discountAmount/paidAmount` 全 `stdSqlType="VARCHAR" stdDataType="string"`（`quantity` 例外用 DECIMAL）。
- **inventory**（30 处）：`inventory.orm.xml:161-163 quantity/unitCost/totalCost`、`:202-206,245-251,312-313,345-348,480-484,544-545,575-576`。
- **finance**（46 处）：`finance.orm.xml:189-190 debitAmount/creditAmount`（凭证分录金额）、`:494-501` 总账余额。

**对照**：**sales 域 70 处金额/数量字段全用 `DECIMAL(20,4)`**（`sales.orm.xml:112-115,305-313`）—— 是正确参考实现，证明可正确实现。

**多层问题**：
1. `orm-model-design.md:392-403` 明确"金额/数量必须 BigDecimal/DECIMAL"。
2. Java 层得到 `String` 而非 `BigDecimal`，运算须手动 `new BigDecimal(str)` + 空值/格式防护（plans 已记录此"陷阱"）。
3. SQL 层无法数值聚合/比较/索引；凭证借贷平衡校验依赖字符串解析。
4. **`domain="amount"`(DECIMAL) 与 column `stdSqlType="VARCHAR"` 冲突**：依 `orm-model-design.md:107-108`"column 与 domain 都设置时以 domain 为准"，则 VARCHAR 为死配置（实际可能仍 DECIMAL）；若平台行为与文档不符则实际 VARCHAR。配置自相矛盾、状态不确定。

**治理缺口**：仅在 `docs/plans/*`（作为"陷阱"）与 `docs/architecture/testing-strategy.md:88`（测试屏蔽项）提及；**未在 `docs/architecture/` 或 `docs/design/` 正式登记为架构决策/已知妥协**（违反 AGENTS.md 规则 9/10）。

### O2. 🟡 全工程 18 域 0 个 `<index>` 定义

`code`（业务单号，应唯一）、`supplierId`/`materialId`/`warehouseId`（高频过滤）、`docStatus+approveStatus`（列表筛）、`tenant+delFlag`（逻辑删除过滤）等高频查询路径无索引。`code` 无唯一索引可能导致单号重复风险。

### O3. 🟡 dict `valueType` 全工程不一致

crm 全 string（23）、master-data 混用（int 15 + string 1，`batch-strategy` 用 string 其余 int）、其余 16 域全 int。`code-style.md:62-75` 推荐"新模块优先 string"。

### O4. 🟢 个别审计字段类型细节

`approvedBy`/`postedBy` 用 `VARCHAR(36)`（purchase.orm.xml:115,347），暗示存 UUID，但缺 `stdDomain="userId"` 标注，语义不明。

### ORM 亮点

1. **命名 100% 合规**：18 域 appName/moduleId/表前缀/类前缀全对齐 `domain-module-split-analysis.md §2.0`，B2B `ErpB2b*` 大小写规则正确。
2. **跨模块引用（机制 B）规范**：外部实体引用均带 `notGenCode="true" biz:moduleId="erp/md"`。
3. **三轴状态分离设计**：docStatus/approveStatus/posted 清晰，状态字段全字典化。
4. **元信息丰富**：全域双语 displayName + i18n + Lucide icon。
5. **tenantId 正确未预置**（遵循 system-baseline.md 硬规则）；逻辑删除规范（`delVersion` + `useLogicalDelete`）。

---

## 第三部分：模块结构与代码生成纪律

### M1. 🔴 purchase/sales → projects 跨业务域 ORM 硬引用违反 DAG

**证据**：
- `purchase.orm.xml:160,407,837` 对 `ErpPrjProject` 建立 to-one + `erp-pur-dao/pom.xml:36-40` compile 依赖 `app-erp-projects-dao`。
- `module-sales/erp-sal-dao/pom.xml` 同样 compile 依赖 `app-erp-projects-dao`；sales ORM 含 2 处 `ErpPrj` 引用。

**冲突**：`data-dependency-matrix.md:530` 明确"所有业务域 ORM 层均只引用 master-data（跨业务域仅 finance→projects 单向合法）"；`module-boundaries.md:36` 将 projects（扩展域）列入 purchase 禁止依赖。

**定性**：文档与实现不一致 —— 必须收敛到一方（改弱指针，或修订 owner docs 白名单）。

### M2. 🟡 `codegen.sh` 指向不存在的根级单 orm 文件

`codegen.sh:5` 执行 `nop-cli gen ... model/app-erp.orm.xlsx`，项目根无 `model/` 目录（实际为 18 域独立 `module-<domain>/model/app-erp-<domain>.orm.xml`）。脚本陈旧，照抄会失败。

### M3. 🟡 聚合 app 关闭页面模型校验

`app-erp-all/application.yaml:17 nop.web.validate-page-model: false`（注释自述"聚合 app 暂跳过页面校验避免跨域页面引用阻塞启动"）。掩盖潜在 parse-missing-resource。

### M4. 🟢 其它轻微

- `application.yaml:3 nop.debug: true` + `:6 enc-key` 硬编码（bootstrap 期可接受，生产前外置化）。
- `missions/`（crud.json/erp.json）工作流临时文件被 git 跟踪。
- `erp-md-api/pom.xml:15 java.version=11` 与项目 Java 17+ 基线不一致。

### 模块结构亮点

1. **代码生成纪律零违规**：558 个 `_gen` 文件 + 全部 `_` 前缀产物工作树零脏改，git log 全为 codegen 触发提交。
2. **18 域命名映射零偏差**：物理目录/artifactId/appName/moduleId/表前缀/类前缀六维全对齐。
3. **action-auth 聚合设计规范**：`x:extends` 合并 18 业务域 + 4 系统模块，site-map 配置正确，无重复无遗漏。
4. **DAG 核心链路无环**：master-data←inventory←purchase/sales←finance 单向合法（除 M1 的 projects 边）。
5. **保留层/生成层分离清晰**：279 保留层实体 + 279 BizModel + 279 I*Biz 接口各归其位。

---

## 第四部分：视图与页面

### V1. 🔴 DRP 域菜单断链（3 处 404）

`erp-drp.action-auth.xml:15,19,46` url 指向 `ErpInvDrp{Plan,Line,Parameter}`，实际 page 目录为 `ErpDrp{Plan,Line,Parameter}`（`ErpInv` 前缀错误，疑复制自 inventory）。该文件共含 5 个 `ErpInvDrp*` 资源，其中 SafetyStockCalc/LeadTimeRecord 的目录恰好存在（不断链），仅 Plan/Line/Parameter 3 处断链。**注意**：生成基线 `_erp-drp.action-auth.xml` 自身也混用 `ErpInvDrp*`（CrossDock/DockAppointment 等）与 `ErpDrp*`，并非"基线正确"——extends 路径需先理基线命名。已拆出为独立 successor 计划。

### V2. 🟡 单据状态机按钮全未接线

全工程 0 个业务动作按钮（approve/confirm/post/void/reject）。`document-engine.md` 定义三轴状态机 + 业务层已实现，ORM 有 `docStatus/approveStatus/posted` 字段，但页面纯 CRUD，用户无法从 UI 触发审批/过账/作废。

### V3. 🟢 8 域 action-auth standalone 不 extends 生成基线

aps/b2b/ct/crm/cs/drp/hr/log 的 `erp-xx.action-auth.xml` 无 `x:extends`，与另 10 域风格不一致；ORM 新增实体时生成基线菜单不会自动并入。

### V4. 🟢 其它

- `app-erp-all/app.action-auth.xml` l10n-cn 段 3 处占位页不存在（已知设计阶段）。
- FK 字段无 picker 选择控件（`supplierId`/`materialId` 渲染为纯 ID），bootstrap 阶段可接受。
- dashboard 占位页仅 9/18 域有。

### 视图层亮点

1. **三层架构零违规**：279 个保留层 view 全部 `x:extends="_gen/_*.view.xml"`，无一处手改 `_gen`。
2. **i18n 全程 ORM 驱动**：从 `<dict i18n-en:label>` 到自动生成 `_erp-xx.i18n.yaml`，零手写重复 key。
3. **菜单业务语义化**：`erp-pur.action-auth.xml` 按业务阶段分组 + `app:useCases` 用例追溯。
4. **薄 page.yaml wrapper 一致**：279/306 main.page.yaml 为 3 行 `web:GenPage` 标准 wrapper。

---

## 关键问题总览（按严重程度）

### 🔴 严重（5 项）
| ID | 问题 | 维度 | 阻塞性 |
| --- | --- | --- | --- |
| S1 | BizModel 方法系统性缺失 Biz 注解 | 服务层 | 阻塞 GraphQL 暴露 + 测试 |
| S2 | 跨实体/跨域访问绕过 I*Biz 管道 | 服务层 | 静默跳过数据权限 |
| O1 | 金额族 VARCHAR 存储（3 域 122+ 字段） | ORM | 类型/运算/治理 |
| M1 | purchase/sales → projects 违反 DAG | 模块边界 | 文档与实现冲突 |
| V1 | DRP 菜单断链 3 处 404 | 视图 | 用户可见功能缺陷 |

### 🟡 中等（8 项）
S3（requireEntity 退化）、S4（new 实体）、S7（无 task.xml）、O2（零索引）、O3（dict valueType 不一致）、M2（codegen.sh 陈旧）、M3（页面校验关闭）、V2（状态机按钮未接线）

### 🟢 轻微（多项，见各部分）
S5/S6/S8、O4、M4、V3/V4

---

## 根因分析

1. **业务设计先行、平台 API 规范滞后**：roadmap 的计划文档（`docs/plans/2026-07-01-*`）高度聚焦业务状态机正确性，对 `ai-defaults.md` 反模式表的强制自检执行不足。P1 各段计划虽在"参考示例"提到 `task.xml`，但实际实现全走 Java。
2. **VARCHAR 金额陷阱是历史遗留**：sales 域后写，已用 DECIMAL；purchase/inventory/finance 先写，留下 VARCHAR，且未提升为 owner doc 决策。这是典型的"重复出现的执行经验未提升到 owner docs"（AGENTS.md 规则 9/10）。
3. **测试驱动不足**：因 S1 阻塞，测试绕过 GraphQL 直调 Java API，使 S1/S2 长期未暴露。
4. **projects 引用是业务驱动的合理需求**（项目采购/项目销售），但 owner docs 未同步更新，造成文档落后于实现。

## 优先修复顺序建议

1. **S1**（Biz 注解）—— 阻塞性，修完后 V2/M10/S5 才能依次验证接线。
2. **S2 + S3 + S4**（跨实体 I*Biz + requireEntity + newEntity）—— 服务层平台 API 系统性整改，建议作为一个集中改造切片。
3. **O1 + M1**（VARCHAR 策略 + projects DAG）—— 需先做 **Decision**（迁移 vs 登记；弱指针 vs 文档白名单），收敛文档与实现。
4. **V1**（DRP 断链）—— 快速修复。
5. **O2/O3/M2/M3**（索引/dict/脚本/校验）—— bootstrap 期一次性统一。
6. **V2**（状态机按钮接线）—— S1 修完后按域深化。

> 详细执行方案见配套计划 `docs/plans/2026-07-01-1900-1-platform-compliance-remediation.md`。

## 抽样覆盖说明

| 维度 | 覆盖 |
| --- | --- |
| ORM | 全文精读 5 核心域（master-data/purchase/inventory/finance/sales）；结构+外部实体抽样扩展域（crm 等）；全工程量化 dict valueType、index 计数、金额 VARCHAR 计数 |
| 服务层 | 逐行阅读 purchase/sales/inventory/finance 全部自定义 BizModel + Converter/Builder/Checker/Bookkeeper/Posting；全项目 grep 反模式扫描 |
| 模块结构 | 全部 18 域子模块结构批量核对；深度 pom 审查（master-data 全 7 个 + purchase/inventory/finance/app-erp-all）；git 核查生成物纯净度 |
| 视图 | 全量统计 558 view.xml + 588 page.yaml + 54 action-auth.xml 的断链/一致性；深度抽查 5 核心域 + DRP |

> 本报告为只读分析，未修改任何代码。所有"违规"判定均附 `文件:行号` 证据并对照 `docs-for-ai` 具体规则出处，可逐条复核。
