# 2026-07-11-1643-2-settlement-allocation-dto-extraction SettlementAllocation DTO 跨域去重提取至 md-dao

> Plan Status: completed
> Last Reviewed: 2026-07-11
> Source: `docs/plans/2026-07-11-1225-1-analysis-consistency-fixes.md` Deferred「`SettlementAllocation` DTO 跨模块重复」（Successor Required: yes，触发条件=另开代码重构 owner plan，**本计划即该 successor**）；`docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md` §1.6:86 + §4.2:255（P2，"两域重复提取共用"）
> Related: `docs/plans/2026-07-02-0300-2-sales-invoice-receipt-bizmodel.md`（ReceiptSettler 落地，sales 侧 DTO 源头）、`docs/plans/2026-07-02-0300-1-purchase-invoice-payment-three-way-match.md`（PaymentSettler 落地，purchase 侧 DTO 源头）
> Audit: required

## Current Baseline

经独立子代理全仓库扫描确认（`ses_0afafa43fffeJPW8Nle8w34saL`，read-only）：

- **两份重复 DTO 已定位**：
  - `module-sales/erp-sal-dao/src/main/java/app/erp/sal/biz/SettlementAllocation.java`（30 行，`app.erp.sal.biz`）
  - `module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/SettlementAllocation.java`（30 行，`app.erp.pur.biz`）
- **类体 100% 相同**——字段（`Long invoiceId` + `BigDecimal amount`）与方法（getter/setter）完全一致；`diff` 仅 4 处差异：包声明（必需）+ 3 行 Javadoc 文本（`收款→发票`/`{IErpSalReceiptBiz#settle}` vs `付款→发票`/`{IErpPurPaymentBiz#settle}` + 客户/供应商措辞）。纯 POJO，无注解/无构造/无 `serialVersionUID`。
- **使用点（8 处生产源码）**——仅作 `@BizMutation` 方法入参 + 循环局部变量，**从不**作返回类型/字段/持久化：
  - sales：`IErpSalReceiptBiz.java:36`（`settle(... List<SettlementAllocation> ...)` `@BizMutation` `@Name("allocations")`）+ `ErpSalReceiptBizModel.java:5,41` + `ReceiptSettler.java:3,55,72` + `ErpSalReceiptProcessor.java:5,113`
  - purchase：`IErpPurPaymentBiz.java:26` + `ErpPurPaymentBizModel.java:5,41` + `PaymentSettler.java:3,55,72` + `ErpPurPaymentProcessor.java:9,134`
- **无共享/通用模块**——仓库无 `module-common/`/`*shared*` 模块或包。`erp-md-dao` 的 `biz/` 包当前含 23 文件**全为 BizModel 接口**（22 个 `IErpMd*Biz` + 1 个 `IErpSysConfigBiz`），**零 DTO 类**——本计划将建立 md-dao 共享 DTO 子约定。
- **模块依赖图（DAO 层）**：`erp-md-dao`（仅依赖 nop 框架，真基线层）← `erp-prj-dao` ← `{erp-sal-dao, erp-pur-dao, erp-fin-dao}`（三者**同级**，依赖集相同：md-dao + prj-dao + nop 框架，互不依赖——`erp-fin-dao/pom.xml` 实测仅 md-dao + prj-dao + 框架，**不依赖** sal-dao/pur-dao）。**sal-dao 与 pur-dao 均已声明对 `app-erp-master-data-dao` 的编译依赖**——无需新 pom 接线，仅 md-dao 增一源文件。finance-dao **不适用**：因 sal-dao/pur-dao 不依赖 finance-dao（同级），若 DTO 宿于 finance-dao 需令 sal-dao/pur-dao 新增对同级域的横向依赖（反模式）；md-dao 是三者**共同祖先**（均已依赖），为唯一无需新接线的宿主。
- **公共契约暴露（关键风险）**：
  - `*.api.xml`：**全仓库零 `*.api.xml`**，无外部 RPC 契约。
  - `*.xbiz.xml`/`*.xmeta`：无引用（逻辑在 Java BizModel）。
  - **GraphQL（运行时）**：`settle` 方法为 `@BizMutation` `@Name("allocations") List<SettlementAllocation>`，Nop 从两 Java 包自动生成**两个独立** GraphQL input type：`i_app_erp_pur_biz_SettlementAllocation`（schema dump L39638，`ErpPurPayment__settle` L60911）与 `i_app_erp_sal_biz_SettlementAllocation`（L39658，`ErpSalReceipt__settle` L62867）。**移类改包将重命名 GraphQL input type**（→ `i_app_erp_md_biz_SettlementAllocation`）——这是公共面名称变更。
  - schema dump 文件在 `_dump/`（`.gitignore:12` 已忽略），GraalVM `reflect-config.json` 自动重生。
- **其他跨域重复**：`ReturnStockMoveBuilder.java` 与 `ReturnQtyValidator.java`（各 sales/purchase service 层一份）为**结构性平行非重复**——绑定不同实体类型、相反库存方向、不同错误参数，属不同行为，**不在本计划范围**（归独立泛型模板重构 successor）。**`SettlementAllocation` 是仓库唯一真正跨域 DTO 重复**。

## Goals

- 将 `SettlementAllocation` 单一真相源提取至 `erp-md-dao`（架构上最低应用层，sal-dao/pur-dao 均已依赖），消除两域重复。
- 迁移 sales + purchase 共 8 处使用点至共享类型，删除两旧文件。
- 显式裁决 GraphQL input type 重命名的公共面影响（接受或缓解）。

## Non-Goals

- `ReturnStockMoveBuilder`/`ReturnQtyValidator` 泛型化（service 层结构性平行，不同实体/行为，独立 successor）。
- 建立 `module-common/` 通用模块（架构变更，超出单一 DTO 去重范围；md-dao 已满足共享宿主需求）。
- GraphQL schema 向后兼容垫片（保留旧 input type 别名）——内部 ERP 应用 schema 随后端重部署，且当前两 input type 已分裂为两名。
- DTO 字段扩展（如增 settlementMethod/分配日期）——纯去重，保持字段集不变。

## Task Route

- Type: `architecture change`（跨模块 DTO 提取 + 公共 GraphQL 面名称变更裁决）
- Owner Docs: `docs/architecture/domain-module-split-analysis.md`（域模块拆分与依赖方向）、`docs/architecture/data-dependency-matrix.md`（模块依赖 R/S/P 类型）、`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（跨模块引用与共享类型约定）
- Skill Selection Basis: 涉 Java BizModel/IFace 接口 + GraphQL input type 公共面 → 匹配 `nop-backend-dev`（xbiz 动作声明 / 跨实体 I*Biz / 公共契约面 / 决策门）。纯 DTO POJO 迁移无需 ORM/codegen → `Skill: none`（DAO 层无 BizModel 逻辑）。Java 测试 → `nop-testing`。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。无数据迁移（DTO 非持久化）、无新外部服务。GraphQL schema 随后端重部署自动重生（`_dump/` 已 gitignore）。

## Execution Plan

### Phase 1 - 共享类型落地 + 公共面裁决

Status: completed
Targets: `module-master-data/erp-md-dao/src/main/java/app/erp/md/biz/`（新 DTO 宿主）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] `Decision`: 裁决两项——(a) 包路径：`app.erp.md.biz`（与既有 IBiz 接口同包，单一简单 DTO 无需子包）；(b) GraphQL input type 重命名（`i_app_erp_{sal|pur}_biz_SettlementAllocation` → `i_app_erp_md_biz_SettlementAllocation`）——接受，内部 ERP schema 随后端重部署、AMIS 前端发原始 JSON 对象非显式 input type 名。记录选择、考虑的替代方案（保留两 thin 子类型——拒绝理由：额外维护负担且无业务价值；向后兼容垫片——拒绝理由：input type 已分裂为两名无已有消费者依赖旧名）与残留风险（schema dump 需重生）于本计划。
  - Skill: `nop-backend-dev`
- [x] `Add`: 在 `app.erp.md.biz` 包创建 `SettlementAllocation.java`（字段 `Long invoiceId` + `BigDecimal amount` + getter/setter，泛化 Javadoc 去客户/供应商措辞）。
  - Skill: none

Exit Criteria:

- [x] 裁决记录于计划（包路径选择 + GraphQL 重命名接受理由 + 替代方案分析）
- [x] 新 `SettlementAllocation.java` 存在于 md-dao 且字段集与原两份一致

### Phase 2 - 迁移使用点 + 删除重复 + 重建

Status: completed
Targets: sales/purchase dao+service 共 8 文件
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 共享类型已落地

- [x] `Fix`: 8 处使用点改 import 至 `app.erp.md.biz.SettlementAllocation`——sales（`IErpSalReceiptBiz.java:36`、`ErpSalReceiptBizModel.java:5,41`、`ReceiptSettler.java:3,55,72`、`ErpSalReceiptProcessor.java:5,113`）+ purchase（`IErpPurPaymentBiz.java:26`、`ErpPurPaymentBizModel.java:5,41`、`PaymentSettler.java:3,55,72`、`ErpPurPaymentProcessor.java:9,134`）；删除两旧文件。
  - Skill: `nop-backend-dev`
- [x] `Proof`: sales/purchase service 测试全绿（`mvn test -pl module-sales/erp-sal-service,module-purchase/erp-pur-service -am` 3m3s BUILD SUCCESS，0 failures/0 errors）。验证命令：`mvn test -pl module-sales/erp-sal-service,module-purchase/erp-pur-service -am`。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 8 处使用点 import 指向 md-dao 共享类型，两旧文件已删
- [x] sales/purchase service `mvn test` 全绿（核销行为不变）
- [x] GraphQL input type 重生命名一致（schema dump 随后端重部署自动重生，本重构未改行为语义）

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0afa2d457ffe0x1Tc6YDmofTM`，general agent 新会话，无执行者上下文) — 1 BLOCKING：Current Baseline 模块依赖图称"finance-dao 依赖 sal-dao/pur-dao，方向相反"为事实错误（`erp-fin-dao/pom.xml` 实测 finance-dao 与 sal-dao/pur-dao 同级，仅 md-dao + prj-dao + 框架），与同段"三者同级依赖集相同"自相矛盾。审查者明确：结论（md-dao 为正确宿主）不变，仅 finance-dao 排除理由需订正。2 Minor（md-dao biz/ 命名 IErpMd* 涵盖 IErpSysConfigBiz 略不精确 / Phase 2 Proof 可补既有 settle 测试证据）。12+ 基线声明独立核对通过。
- Independent draft review iteration 2: accept — 订正 1 BLOCKING（finance-dao 改为同级 + 排除理由改为"sal-dao/pur-dao 不依赖 finance-dao，宿于 finance-dao 需新增同级横向依赖反模式，md-dao 为共同祖先"——`erp-fin-dao/pom.xml` 已复核）+ 2 Minor（IErpMd* 命名精确化 22+1 / Phase 2 Proof 补既有 5 个 settle/端到端测试覆盖行为不变性）。审查者预设该修复"trivial 且不改结论与任何执行步骤"，已逐字应用。草案审查收敛，状态 draft→active。

## Closure Gates

> 仅在所有项目和每个阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处：`mvn clean install -DskipTests`（154 模块，重生 GraalVM reflect-config + GraphQL schema dump）一次。

- [x] 范围内行为完成（单一真相源 DTO + 8 处迁移 + 两旧文件删除）
- [x] 相关文档对齐（`domain-module-split-analysis.md`/`data-dependency-matrix.md` 未提及共享 DTO 约定，无需补注；分析报告 §1.6:86 + §4.2:255 状态订正为已修；前驱计划 `2026-07-11-1225-1` Deferred + Follow-up successor 标记完成）
- [x] 已运行验证（`mvn clean install -DskipTests` 相关模块 BUILD SUCCESS + sales/purchase service `mvn test` 0 failures/0 errors）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话 `ses_0ae63cf00ffenU5Fm5eVZEPw3I`）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ReturnStockMoveBuilder / ReturnQtyValidator 泛型化

- Classification: `optimization candidate`
- Why Not Blocking Closure: service 层结构性平行非真正重复——绑定不同实体类型（ErpSalReturn/Line vs ErpPurReturn/Line）、相反库存方向（INCOMING vs OUTGOING）、不同错误参数。泛型化需引入跨域泛型模板，不同结果表面。
- Successor Required: `yes`（触发条件：退货域泛型抽象需求或第三域退货落地时）

### module-common 通用模块建立

- Classification: `optimization candidate`
- Why Not Blocking Closure: 单一 DTO 去重无需新模块；md-dao 已满足共享宿主。建立 module-common 属架构变更，需独立评估全仓共享类型需求规模。
- Successor Required: `no`（触发条件：跨域共享类型累积至需独立模块时）

## Closure

Status Note: 实施完成 2026-07-11，关闭 2026-07-11。Phase 1-2 全部 done：共享 SettlementAllocation 在 `app.erp.md.biz` 包创建（泛化 Javadoc 去客户/供应商措辞）；sales/purchase 共 8 处 import 迁移至 md-dao 共享类型；两旧文件删除。验证：`mvn test -pl module-sales/erp-sal-service,module-purchase/erp-pur-service -am` 全绿（BUILD SUCCESS，121 tests 0 failures/0 errors）；`mvn clean install -DskipTests` 相关模块 BUILD SUCCESS。GraphQL schema 随后端重部署自动重生（`_dump/` gitignore）。纯 Java 重构，零 ORM/契约/AMIS 变更。文档对齐：分析报告 §1.6:86 + §4.2:255 订正为已修；前驱计划 `2026-07-11-1225-1` Deferred + Follow-up successor 标记完成。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 `ses_0ae63cf00ffenU5Fm5eVZEPw3I`，冷重播无执行者上下文，read-only 核对）
- Verdict: PASS（无 Blocker / 无 Major）
- Evidence:
  - DTO 单一真相源：`module-master-data/erp-md-dao/src/main/java/app/erp/md/biz/SettlementAllocation.java`（`glob **/SettlementAllocation.java` count=1，两旧文件实测已删）
  - 8 处迁移文件：4 sales 文件（`IErpSalReceiptBiz.java:10`、`ErpSalReceiptBizModel.java:5`、`ReceiptSettler.java:3`、`ErpSalReceiptProcessor.java:5`）+ 4 purchase 文件（`IErpPurPaymentBiz.java:10`、`ErpPurPaymentBizModel.java:5`、`PaymentSettler.java:3`、`ErpPurPaymentProcessor.java:9`），import 全部 `app.erp.md.biz`
  - `rg "app\.erp\.(sal|pur)\.biz\.SettlementAllocation"` 零命中（无残留旧包引用）
  - 模块依赖已接线：`erp-sal-dao/pom.xml:18` + `erp-pur-dao/pom.xml:18` 均声明 `app-erp-master-data-dao`，无需新 pom 接线
  - 字段集/行为不变：纯 POJO（无注解/无构造/无 serialVersionUID），`Settler` 签名与循环体仅 import 行变更
  - 测试验证：sales+purchase service 121 tests 0 failures/0 errors（BUILD SUCCESS）
  - 无范围蔓延：`ReturnStockMoveBuilder`/`ReturnQtyValidator` 仍存于两 service 层未动（正确 deferred）；`module-common` 未建
- Anti-hollow: 旧文件实测删除（glob count=1），非 no-op

Follow-up:

- ReturnStockMoveBuilder/ReturnQtyValidator 泛型化 successor（见上方 Deferred）
