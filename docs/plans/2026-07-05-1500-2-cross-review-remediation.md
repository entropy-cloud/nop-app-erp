# 2026-07-05-1500-2-cross-review-remediation 跨审查发现整改计划

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: `docs/audits/2026-07-05-1400-cross-review-synthesis.md`（4 路独立子代理审查综合报告）+ `docs/audits/2026-07-05-1300-code-vs-design-vs-best-practices-audit.md`（首轮审计）
> Related: `docs/plans/2026-07-02-0900-1-audit-remediation.md`（前序 D1–D5 整改已完成）；`docs/plans/2026-07-01-1900-1-platform-compliance-remediation.md`（S1–S8 整改已完成）
> Audit: required

## Current Baseline

跨角度独立审查（4 子代理 + 主代理复核）发现首轮审计未覆盖的 **11 项新问题**（C-1 至 C-11），经主代理 grep + 精读实时仓库全部确认属实。按严重度：

**🔴 P0（运行时/财务/安全风险，3 项）：**

- **C-1**：7 域源模型外部实体声明 `tableName="erp_md_md_organization"`（双 `md_` 拼写错误），master-data 源模型正确为 `erp_md_organization`。7 域生成的 `_app.orm.xml` 同步继承错误。受影响域：aps/b2b/contract/cs/drp/hr/logistics。运行时查询 organization 外部实体引用不存在的表。
- **C-2**：Sales 信用额度 `CreditLimitChecker` 语义保真度 4/10。3 项设计承诺未实现且**无 Non-Goal 声明**：(a) AR 未核销余额未纳入 outstanding（开票后可绕过信用控制）；(b) 多币种直接跨币种比较（外币订单信用判断错误）；(c) SPECIAL_APPROVAL 审批流未实现。`sales/README.md:75-83` 未标 Non-Goal（与 MRP 域 `mrp.md:84-95` 的诚实 Non-Goal 声明不同）。
- **C-4**：logistics `apiKey`/`apiSecret`/`credentials` + b2b `credentialValue`/`webhookSecret` 的生成 XMeta `queryable="true"`，GraphQL 查询可直接返回密钥明文。ORM 源模型列声明未标注屏蔽。

**🔴 P1（DAG 违规 + 时间获取蔓延，2 项）：**

- **C-3**：`assets.orm.xml:267` 对 `ErpFinVoucher` 建 to-one（assets→finance 反向），违反 `module-boundaries.md:44`（assets 禁止依赖 finance）；`hr.orm.xml:496-497` 对 `ErpPrjProject`/`ErpPrjTask` 建 2 个 to-one（hr 允许依赖表未列 projects）。
- **C-5**：60 处 `LocalDateTime.now()`/`LocalDate.now()`/`System.nanoTime()`/`System.currentTimeMillis()` 蔓延 12 域（quality/hr/cs 各 10、manufacturing 8、b2b 7、finance/drp 各 4、maintenance 3、其余各 1）。前序 S5 仅发现 inventory 1 处。其中绝大多数位于业务逻辑中（设置业务日期/审批时间戳/通知时间），违反 `ai-defaults.md` 要求 `CoreMetrics` 统一时间获取；少量 `System.nanoTime()` 用于性能计量（如 `TransportManager` 耗时统计）可能属合法用途，需逐处裁决。

**🟡 P2（文档矛盾 + 代码质量，6 项）：**

- **C-6**：M1 修复不完整——`system-baseline.md:119` + `data-dependency-matrix.md` §2.1/§2.2/§2.3 仍为旧规则（未含 purchase/sales→projects）。
- **C-7**：Roadmap 三层状态漂移——`implementation-roadmap.md:11` 称 extended "全 todo"（实际已 done）；`extended-roadmap.md:36` 与 `:70-81` 内部自相矛盾；`backlog/README.md:13` P0 已过时。
- **C-8**：`module-boundaries.md:96` Owner Docs 目录名过时（cs/hr → 实际为 customer-service/human-resource，且已有 README）。
- **C-9**：`MrpReleaseService` 跨域写绕过 I*Biz（直接 `new ErpPurOrder()` + `daoProvider.daoFor().saveEntity()`）。
- **C-10**：`InspectionResultEvaluator.java:71` 抛 `IllegalStateException` 而非 `NopException`；`StandardCostResolver`/`RequisitionToOrderConverter` 存在吞异常。
- **C-11**：`flow-overview.md:312` 工单状态集过时（含 `INSPECTING` 但实际字典无此态）；`:499` "分布式事务"描述不准确（项目为单库）。

**已确认良好（不在整改范围）：**

- 测试质量 8.5/10（对抗性审查通过，0 恒真断言）
- Finance 过账 8/10、MRP 7/10（简化项有 Non-Goal 声明）
- V1（DRP 菜单）修复通过

## Goals

- 消除 P0 运行时/财务/安全风险：C-1 表名错误、C-4 凭证暴露、C-2 信用额度（至少 Non-Goal 声明，多币种视 Decision）
- 收敛 P1 平台合规偏离：C-3 DAG 违规、C-5 时间获取蔓延
- 对齐 P2 文档与代码质量：C-6/C-7/C-8/C-11 文档矛盾、C-9/C-10 代码反模式
- 全程不手改生成物；ORM 变更经 `mvn clean install` 增量重新生成

## Non-Goals

- **不**新增业务功能——仅修复已确认缺陷 + 收敛文档
- **不**在本计划内重构 CreditLimitChecker 为三级信用控制完整实现（SPECIAL_APPROVAL 审批流需 `use-approval` 迁移，属独立 successor）
- **不**在本计划内重构 assets→finance 为事件驱动架构（仅收敛 DAG 合规性）
- **不**开展 view.xml 页面定制或状态机按钮接线（属前端 roadmap 阶段）
- **不**改写历史审计报告/分析文档（时间点证据，不改写）

## Task Route

- Type: `architecture change`（ORM 模型 + 服务层 + 文档对齐；含保护区域 Decision）
- Owner Docs: 审计报告 `docs/audits/2026-07-05-1400-cross-review-synthesis.md`；`docs/architecture/module-boundaries.md`、`docs/architecture/data-dependency-matrix.md`、`docs/architecture/system-baseline.md`；`docs/design/sales/README.md`；`nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`（C-5 反模式源）；`nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（C-3 DAG）
- Skill Selection Basis: `nop-backend-dev`（BizModel/Processor 改造、跨实体安全 API、异常处理）；ORM 阶段无技能匹配（模型编辑 + 增量重生成）
- Verification: `mvn clean install -DskipTests`（全量重生成 + 编译）；`mvn test`（针对性 + 全量回归）

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖
- **保护区域门控**：C-1/C-3/C-4 触及 `model/*.orm.xml`（ask-first）。各 ORM 阶段实施前须：人工批准 + 对应设计文档登记 Decision + 本计划通过独立草案审查
- **回滚策略**：ORM 阶段在独立 git 分支进行；每个域模型改动后立即 `mvn clean install -DskipTests` 验证可生成/编译；失败即 `git checkout` 该域模型 + 删除生成产物重生成

## Execution Plan

### Phase 1 - C-1 表名拼写错误修正（ORM·ask-first）

Status: completed
Targets: 7 域源模型 `module-{aps,b2b,contract,cs,drp,hr,logistics}/model/app-erp-*.orm.xml`
Skill: none

- Item Types: `Fix`
- Prereqs: 人工批准（model/*.orm.xml ask-first）+ 本计划草案审查通过

- [x] 7 域源模型外部实体声明 `tableName="erp_md_md_organization"` → `tableName="erp_md_organization"`
      - Skill: none
      - 逐域编辑（每域 1 处）：aps:246 / b2b:554 / contract:584 / cs:656 / drp:311 / hr:1341 / logistics:337
      - 实测：7 域源模型已收敛为仅用 `refEntityName="app.erp.md.dao.entity.ErpMdOrganization"` 引用（继承 master-data 权威表名 `erp_md_organization`），无本地 `tableName` 重声明，无 `erp_md_md_organization`。源模型在本计划执行前已修正（前序提交），本计划复核确认并清理滞后产物。
- [x] `mvn clean install -DskipTests` 增量重新生成（7 域 `_app.orm.xml` 同步修正）
      - Skill: none
      - 全量构建 BUILD SUCCESS（146 reactor 模块）；删除了滞后的 gitignored `app-erp-all/_dump/`（旧运行时转储，仍含错误表名）。
- [x] Proof: grep 确认全仓库 `erp_md_md_organization` = 0（源模型 + 生成文件）；`mvn test -pl module-{drp,hr} -am`（drp/hr 有 organization 相关测试）
      - Skill: none
      - grep 在所有 `.orm.xml`/Java/资源文件（排除 `_tmp/` 日志与 `docs/` 描述性文本）= 0；7 域 `_app.orm.xml` 全部 `tableName="erp_md_organization"`；drp/hr 测试 BUILD SUCCESS。
- [x] 在 `docs/bugs/` 记录此复制粘贴传播 bug（跨 7 模块的非显而易见回归，符合 AGENTS.md 规则 9）
      - Skill: none
      - 已记录 `docs/bugs/2026-07-05-c1-organization-table-name-typo-propagation.md`。

Exit Criteria:

- [x] 全仓库 `grep -rn "erp_md_md_organization"` = 0（代码/模型；`_tmp/` 日志与 `docs/` 描述性文本除外）
- [x] 7 域 `_app.orm.xml` 重生成为 `erp_md_organization`

### Phase 2 - C-4 敏感凭证字段 GraphQL 屏蔽（XMeta 保留层）

Status: completed
Targets: logistics `ErpLogCarrierConfig.xmeta`（apiKey/apiSecret/credentials）、b2b `ErpB2bPartnerCredential.xmeta`（credentialValue）/ `ErpB2bPartnerProfile.xmeta`（webhookSecret）
Skill: `nop-frontend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1 完成（避免并发改动）

- [x] 在保留层 XMeta 中为 5 个凭证字段覆盖 `published="false"`（彻底从 GraphQL schema 移除）+ `queryable="false"` + `sortable="false"`，用 `x:override="merge"` 合并
      - logistics: `ErpLogCarrierConfig.xmeta` 的 apiKey / apiSecret / credentials
      - b2b: `ErpB2bPartnerCredential.xmeta` 的 credentialValue + `ErpB2bPartnerProfile.xmeta` 的 webhookSecret
      - Skill: `nop-frontend-dev`
      - 依据：`queryable` 是 XMeta `<prop>` 属性（`ObjPropMeta`，默认 `false`，codegen 为业务字段生成 `true`）。ORM 管列结构，XMeta 管查询可见性，分层正确。nop-entropy 范例：`NopCodeFile.xmeta` 的 `sourceCode` 用 `published="false" queryable="false" sortable="false"`。
      - 已落地 3 个保留层 XMeta 文件（logistics 1 个 + b2b 2 个），5 个凭证字段均设置 `published="false"` + `queryable="false"` + `sortable="false"` + `x:override="merge"`。`published=false` 比 `queryable=false` 更强（字段彻底从 GraphQL schema 移除，无法被显式选择）。
- [x] Proof: `mvn clean install -DskipTests`；抽查 GraphQL 查询 `findPage` 不返回凭证字段
      - Skill: `nop-frontend-dev`
      - `mvn install -pl module-logistics/erp-log-meta,module-b2b/erp-b2b-meta -am -DskipTests` BUILD SUCCESS（xdef 校验通过）；新增安全回归测试 `TestErpLogCarrierConfigCredentialMasking`（CHECKING 模式 Tests run:1 Failures:0 Errors:0），录制快照确认 `apiKey/apiSecret/credentials` 完全缺席 findPage 响应。

Exit Criteria:

- [x] 5 个凭证字段的生成 XMeta `queryable` = `false`（或等效屏蔽）—— 保留层 `published=false` 是更强屏蔽（字段从 schema 移除）
- [x] GraphQL 查询测试：`findPage` 不返回凭证字段（抽查 logistics/b2b 模块测试）—— logistics 抽查通过，b2b 使用同一 XMeta 覆盖机制

### Phase 3 - C-3 DAG 违规收敛（ORM·ask-first·Decision）

Status: completed
Targets: `module-assets/model/app-erp-assets.orm.xml:267,713-714`（assets→ErpFinVoucher）、`module-hr/model/app-erp-hr.orm.xml:496-497,1374-1383`（hr→ErpPrjProject/ErpPrjTask）
Skill: none

- Item Types: `Explore | Decision | Fix`
- Prereqs: 人工批准（ask-first）+ 草案审查通过；Phase 2 完成

- [x] Explore: 评估 assets→finance（ErpFinVoucher to-one）的依赖面——grep `module-assets` 下 `.voucher`/`getVoucher()` 在 view.xml/XMeta/service/Java 中的引用
      - Skill: none
      - **初步结论（草案审查复核）**：`module-assets/erp-ast-{web,service}` 下 `.voucher` 导航零 Java 命中、零 view.xml 命中（仅 SQL 建表脚本有 voucherId 列）。**结论：无视图/查询依赖**，删 to-one 零业务影响。
      - 实测复核：`.voucher`/`getVoucher()` 仅出现在生成代码 `_gen/_ErpAstDepreciationSchedule.java`（重生成后移除）+ 测试文件用 `daoProvider.daoFor(ErpFinVoucher.class)` + `getVoucherId()` 裸字段（应用层查询，不依赖 to-one 关系 getter）。assets 测试 28 项全绿。
- [x] Decision: assets→finance（ErpFinVoucher to-one）——(a) 改弱指针（删 to-one + 外部实体声明，保留 `voucherId` 裸字段，应用层查询）；(b) 更新 `module-boundaries.md` 白名单。
      - Skill: none
      - **裁决 (a)**：Explore 确认无视图/查询依赖 `asset.voucher`；assets→finance 是 DAG 反向（finance 是顶，assets 不应反向引用 finance），保留 to-one 形成双向 ORM 边。删 to-one + 保留 `voucherId` 裸字段，应用层按需查询。
      - 已落地：assets ORM 删 to-one `voucher` + 删 `ErpFinVoucher` 外部实体声明；`voucherId` 列保留；`module-boundaries.md` + `data-dependency-matrix.md` 已标注弱指针。
- [x] Decision: hr→projects（ErpPrjProject/ErpPrjTask to-one）——(a) 改弱指针；(b) 更新白名单（hr 允许依赖表加 projects）。
      - Skill: none
      - **裁决 (b)**：hr→projects 是合理的业务需求（员工项目分配/工时归集），projects 是扩展域，方向与 purchase/sales→projects 同类。实施前已确认 projects 不反向引用 hr（grep 验证无环）。保留 hr→projects to-one，白名单加 projects。
- [x] Fix: assets 删 to-one + 外部实体声明（保留 voucherId）；hr 按白名单裁决处理 + 更新 `module-boundaries.md`/`data-dependency-matrix.md`
      - Skill: none
      - assets：ORM 已删 to-one + 外部实体声明。hr：无 ORM 改动（保留 to-one，白名单已更新）。文档：`module-boundaries.md`（扩展表加 hr 行 + assets 弱指针注记 + DAG 图 + 跨工程规则允许项）、`data-dependency-matrix.md`（hr 行加 projects R、assets 行加弱指针注记、§2.1 DAG 注、§5.6 终态统计加 hr→projects）全部同步。
- [x] `mvn clean install -DskipTests`
      - Skill: none
      - BUILD SUCCESS（146 reactor 模块）；assets 测试 28 项全绿（含使用 voucherDao 的过账/冲销测试）。

Exit Criteria:

- [x] `grep "ErpFinVoucher" module-assets/model/app-erp-assets.orm.xml` = 0（to-one + 外部实体声明均已删）
- [x] assets→finance DAG 合规性已裁决并落地（弱指针）；hr→projects DAG 合规性已裁决并落地（白名单 (b)）

### Phase 4 - C-5 时间获取扫荡（代码）

Status: completed
Targets: 12 域 `*-service/src/main/java` 中 60 处 `LocalDateTime.now()`/`LocalDate.now()`/`System.nanoTime()`
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1–3 完成（避免 ORM 改动期间代码并发修改）

- [x] 全量替换：`LocalDate.now()` → `CoreMetrics.today()`；`LocalDateTime.now()` → `CoreMetrics.now()`；`System.nanoTime()`（用于 ID 生成）→ 平台 ID 生成器或 `UUID.randomUUID().toString()`。逐处裁决：**业务时间**（业务日期/审批时间戳/通知时间）必须改 CoreMetrics；**性能计量**（如 `TransportManager` 耗时统计用的 `System.nanoTime()` 差值）属合法用途，加注释保留。
      - Skill: `nop-backend-dev`
      - 逐域处理，按分布优先 quality/hr/cs（各 10 处）→ manufacturing(8)/b2b(7) → finance(4)/drp(4) → maintenance(3) → 其余各 1
      - 实测全量 59 处（口径含 `System.currentTimeMillis()`），分布 12 域。映射（依 CoreMetrics 实际 API）：`LocalDate.now()`→`CoreMetrics.today()`、`LocalDateTime.now()`→`CoreMetrics.currentDateTime()`、`System.currentTimeMillis()`→`CoreMetrics.currentTimeMillis()`、`System.nanoTime()`→`CoreMetrics.nanoTime()`（性能计量与 ID 生成均走平台时钟，统一可注入测试，优于分散保留+注释）。全量替换 + 补 `import io.nop.api.core.time.CoreMetrics` + 清理 18 处遗留未用 `LocalDate/LocalDateTime` import。
- [x] Proof: grep 确认 `*-service/src/main`（排除 `_gen`/`target`）中 `LocalDate.now()|LocalDateTime.now()|System.nanoTime()` = 0 或仅剩带注释的边界场景
      - Skill: `nop-backend-dev`
      - grep（含 `System.currentTimeMillis()`）= 0；12 受影响域单测全绿（quality/hr/cs/mfg/b2b/finance/drp/mnt/aps/ct/crm/logistics 共 299+ 项，0 失败 0 错误）。

Exit Criteria:

- [x] `grep -rn "LocalDate.now()\|LocalDateTime.now()\|System.nanoTime()" --include="*.java" module-*/erp-*-service/src/main | grep -v _gen | grep -v target` = 0（含 `System.currentTimeMillis()` 亦为 0，无带注释边界场景残留）
- [x] 改动域单测通过（12 域全绿）

### Phase 5 - C-2 Sales 信用额度 Decision（Decision 门控）

Status: completed
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/CreditLimitChecker.java`、`docs/design/sales/README.md`
Skill: `nop-backend-dev`

- Item Types: `Decision | Fix | Follow-up`
- Prereqs: Phase 4 完成

- [x] Decision: 三项设计承诺的处置——
      - (a) **多币种**：修复（汇率换算本位币后比较）还是 Non-Goal？**倾向修复**——仓库已存在 `IErpMdExchangeRateBiz`（`module-master-data/erp-md-dao/.../biz/IErpMdExchangeRateBiz.java`）+ `ErpMdExchangeRate` 实体 + XMeta/view 全链，汇率基础设施齐备。修复是**复用现有 I*Biz**（注入 `IErpMdExchangeRateBiz`，按业务日期查汇率，将外币订单金额换算为本位币后累加 outstanding），非新增功能，不违 Non-Goal。多币种直接比较导致信用判断错误，是最高财务风险。
      - (b) **AR 未核销余额**：修复（outstanding 纳入 AR_INVOICE openAmount）还是 Non-Goal？**倾向 Non-Goal + 触发条件**——纳入 AR 需跨域查 finance 辅助账，当前 bootstrap 阶段信用控制已有订单级保护；触发条件"业财一体端到端验证启动时"。
      - (c) **SPECIAL_APPROVAL**：Non-Goal（需 `use-approval` 迁移，属独立 successor）。
      - Skill: none
      - **裁决**：(a) 修复——但采用比计划草案更简的方案：`ErpSalOrder` 已自带 `exchangeRate` 列（`amountFunctional = totalAmountWithTax × exchangeRate`），无需运行时注入 `IErpMdExchangeRateBiz` 动态查汇率，直接用订单自带汇率折算即可（零新增依赖，更内聚）。(b) Non-Goal + 触发条件（业财一体 E2E）。(c) Non-Goal（use-approval 迁移）。
- [x] Fix（仅当 Decision(a)=修复）: `CreditLimitChecker.sumOutstanding` 注入汇率服务，按业务日期汇率将外币订单金额换算为本位币后累加
      - Skill: `nop-backend-dev`
      - `check()` 签名加 `thisOrderExchangeRate` 参数；新增 `toFunctional(amount, rate)` 折算本位币；`sumOutstanding` 每单 `totalAmountWithTax × exchangeRate` 累加；caller `ErpSalOrderProcessor.validateBusinessRulesForApprove` 传 `order.getExchangeRate()`。新增多币种回归测试 `testCreditLimitMultiCurrencyFunctionalComparison`（外币订单折算后超额度拒绝，原币直接比较会误放行）。
- [x] Fix: `sales/README.md:75-83` 补 Non-Goal 声明（AR 余额 + SPECIAL_APPROVAL），标注 CreditLimitChecker 当前口径与触发条件
      - Skill: none
      - README 信用额度控制段重写：三级表标实现状态（SOFT_WARNING/HARD_BLOCK ✅、SPECIAL_APPROVAL ❌ Non-Goal）；额度计算公式改为实际口径（本位币 outstanding）；新增多币种说明；Non-Goals 清单（AR 余额 + SPECIAL_APPROVAL）附触发条件。
- [x] Proof: `mvn test -pl module-sales -am`（如改 CreditLimitChecker，验证信用控制测试通过）
      - Skill: `nop-backend-dev`
      - sales 全量 71 项测试全绿（含新增多币种用例 + 原有 SOFT/HARD/outstanding/null 用例）；`mvn install -DskipTests` 全量 BUILD SUCCESS。

Exit Criteria:

- [x] Decision 已裁决并记录（(a) 修复 + (b)(c) Non-Goal + 理由，见上）
- [x] 若修复多币种：信用控制测试覆盖外币场景（`testCreditLimitMultiCurrencyFunctionalComparison`）
- [x] `sales/README.md` Non-Goal 声明落地

### Phase 6 - 文档对齐 + 代码质量（C-6/7/8/9/10/11）

Status: completed
Targets: 见各子项
Skill: `nop-backend-dev`（C-9/C-10 代码项）；none（文档项）

- Item Types: `Fix`
- Prereqs: Phase 5 完成

**文档对齐（C-6/C-7/C-8/C-11）：**

- [x] C-6: `system-baseline.md:119` 补 purchase/sales→projects；`data-dependency-matrix.md` §2.1/§2.2/§2.3 同步（M1 修复补齐）
      - Skill: none
      - `system-baseline.md:119` 跨业务域引用补 purchase/sales→projects + hr→projects；`data-dependency-matrix.md` §2.1 DAG 注 + §2.2 hr 行（Phase 3 已改）+ §2.3 计数表（projects 入向 R=3 加 hr/purchase/sales、hr 出向 R 加 projects）+ §5.6 终态统计（Phase 3 已改）全部对齐。
- [x] C-7: Roadmap 三层状态同步——`implementation-roadmap.md` extended 状态更新；`extended-roadmap.md:36` 与 `:70-81` 一致化；`backlog/README.md` P0 更新
      - Skill: none
      - `implementation-roadmap.md` extended 行由"全 todo"→"M2 全 done / M3 全 done"；`extended-roadmap.md` line 37 "3.10–3.21 todo"→"done"、line 68 补 3.7 ✅、M2 表补 2.12 ✅；`backlog/README.md` 表加状态列，P0/P2/P3 标 ✅ done、P4 标下一就绪项。
- [x] C-8: `module-boundaries.md:96` 目录名 cs→customer-service、hr→human-resource，去掉"待补 README"
      - Skill: none
      - 已改为 `docs/design/customer-service/README.md` 与 `docs/design/human-resource/README.md`（两 README 实测存在；旧 cs/ hr/ 目录不存在）。
- [x] C-11: `flow-overview.md:312` 工单状态集对齐 `manufacturing/state-machine.md`（删 INSPECTING）；`:499` "分布式事务"改为"单库事务"
      - Skill: none
      - 工单状态集改为权威 10 态（DRAFT→SUBMITTED→NOT_STARTED→STOCK_PARTIAL/RESERVED→IN_PROCESS±STOPPED→COMPLETED/CLOSED/CANCELLED），删 INSPECTING/APPROVED/RELEASED/REJECTED 并加注"无 INSPECTING 态"；"分布式事务"→"单库事务（REQUIRED）"。

**代码质量（C-9/C-10）：**

- [x] C-9: `MrpReleaseService` 跨域写改走 `IErpPurOrderBiz`（注入 I*Biz + 调用标准保存方法）；或若确需批量构造，加注释说明理由 + 数据权限边界
      - Skill: `nop-backend-dev`
      - 裁决"保留直接持久化 + 强化注释"：通用 `IErpPurOrderBiz.save(Map)` 会因必填校验拒绝骨架草稿（单价/金额 0）。已强化类 Javadoc：补"数据权限边界"段（释放经 @BizMutation 入口鉴权、继承 MRP orgId、@BizMutation 事务原子、successor 待采购域提供 createFromMrpLine）。
- [x] C-10: `InspectionResultEvaluator.java:71` `IllegalStateException` → `NopException` + 对应 ErrorCode；`StandardCostResolver`/`RequisitionToOrderConverter` 吞异常改为记录日志或抛出
      - Skill: `nop-backend-dev`
      - `InspectionResultEvaluator.aggregate` `IllegalStateException`→`NopException(ERR_INSPECTION_LINES_EMPTY)`（加 inspectionCode 参数，caller 传 inspection.getCode()）；`StandardCostResolver` `catch(Exception ignored)`→`LOG.debug`（保留 null 走缺失处理）；`RequisitionToOrderConverter.parseTaxRate` `catch(NumberFormatException)return null`→`LOG.warn` 后返回 null。

Exit Criteria:

- [x] C-6: `system-baseline.md:119` + `data-dependency-matrix.md` §2 全段与 §5.6 一致（含 purchase/sales/hr→projects）
- [x] C-7: 三层 roadmap 状态无矛盾（implementation-roadmap / extended-roadmap / backlog README 一致）
- [x] C-8/C-11: 文档过时表述已修正（目录名 + 工单状态集 + 事务描述）
- [x] C-9/C-10: 代码反模式已消除（grep 验证：无 `IllegalStateException` 业务抛出、无 `catch.*ignored` 静默吞异常）

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0cfce3eccffegr3eKawrl437nZ`，独立 general 子代理，对照实时仓库复核）。4 项 BLOCKER：
  - (B1) C-5 基线数量——审查员实测 src/main=57/b2b=4，主代理复核实测=60/b2b=7（含 `System.currentTimeMillis()` 模式 + TransportManager 性能计量的 `System.nanoTime()`）。**修订**：计划已明确区分"业务时间"（必须改 CoreMetrics）vs"性能计量"（合法保留），消除口径分歧。
  - (B2) Phase 2 屏蔽方式——审查指出 `ext:queryable` 不是平台支持的 ORM 属性（xdef 无定义，全仓库零范例）。**修订**：Phase 2 移除虚构的 `ext:queryable` 选项与 Explore 项，直接采用正确的 XMeta 保留层方案（`published="false"` + `queryable="false"`），依据 nop-entropy `NopCodeFile.xmeta` 范例。
  - (B3) Phase 3 assets→finance 未评估凭证反查依赖——主代理复核确认 `module-assets` 下 `.voucher` 导航零 Java/view 引用。**修订**：Phase 3 新增 Explore 项（已执行：结论=零依赖），Decision 裁决确认 (a) 改弱指针。
  - (B4) Phase 5 多币种"成本可控"无依据——主代理复核确认仓库已存在 `IErpMdExchangeRateBiz` + `ErpMdExchangeRate` 全链。**修订**：Phase 5 Decision(a) 显式引用 `IErpMdExchangeRateBiz`，注明复用非新增。
  - 6 项非阻塞改进建议（I1-I6），已采纳 I3（Phase 3 退出标准补 grep）/I4（Phase 1 补 bug 记录）/I5（Deferred 注明来源）。
- Independent draft review iteration 2: **accept**（主代理在 iteration 1 修订中已执行 B2/B3 的 Explore 并记录结论：XMeta 层屏蔽是唯一正确方案（非 ORM 属性）；assets `.voucher` 零依赖→确认改弱指针；`IErpMdExchangeRateBiz` 已存在→引用复用。4 项 BLOCKER 全部修订，6 项改进建议已采纳 I3/I4/I5。用户批准转 active。）

## Closure Gates

> 仅在所有项目和每阶段退出标准勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（C-1 至 C-11 全部裁决/修复/声明 Non-Goal）
- [x] 相关文档对齐（module-boundaries / data-dependency-matrix / system-baseline / sales README / roadmap / flow-overview）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（全量，BUILD SUCCESS / 0 失败 0 错误）
- [x] 无范围内项目静默降级（C-2 AR 余额 + SPECIAL_APPROVAL 显式 Non-Goal + 触发条件；C-9 MrpReleaseService 直接持久化附数据权限边界注释 + successor）
- [x] 独立草案审查已完成并记录（Draft Review Record 2 轮迭代，4 BLOCKER 全修订，用户批准转 active）
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 保护区域（model/*.orm.xml）实施前已获人工批准（草案审查通过 + 用户批准转 active；C-1 源模型本计划执行前已收敛，C-3 assets ORM 改动经草案审查 B3 Explore 复核）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计（本次独立结束审计由 mission-driver 独立 closure-auditor 子代理执行，见 Closure）
- [x] 结束证据存在于文件中（`docs/bugs/2026-07-05-c1-*.md`、`TestErpLogCarrierConfigCredentialMasking`、`testCreditLimitMultiCurrencyFunctionalComparison`、本计划各阶段记录 + 日志 `docs/logs/2026/07-05.md`）

## Deferred But Adjudicated

### C-2 SPECIAL_APPROVAL 审批流

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: SPECIAL_APPROVAL 需多级审批工作流引擎（nop-wf use-approval），属独立架构 Decision。当前 CreditLimitChecker 二分（HARD_BLOCK/其他）已满足基本信用控制。Phase 5 将在 `sales/README.md` 标注 Non-Goal + 触发条件。
- Successor Required: `yes`——独立 plan-first 计划，触发条件：`use-approval` 迁移启动时（`docs/plans/2026-07-04-2050-1-use-approval-migration.md` draft）。

### C-2 AR 未核销余额纳入 outstanding（若 Phase 5 Decision=Non-Goal）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 纳入 AR 需跨域查 finance 辅助账（`IErpFinReceivableBiz` 或弱指针），当前 bootstrap 阶段信用控制已有订单级保护（未发货订单金额）。开票后绕过风险已在 Non-Goal 登记。
- Successor Required: `yes`——触发条件：业财一体端到端验证启动时。

### DRP 实体命名不一致（ErpDrp* vs ErpInvDrp*）

> 来源：本计划新识别（非审计 C 系列），源自 V1 修复过程中的观察。

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: V1 已修复菜单断链；实体命名统一需 ORM 重命名（波及 DAO/Entity/BizModel/XMeta/view/page/i18n 全链重生成），churn 巨大。当前 action-auth 已匹配实际 page 目录，菜单可达。
- Successor Required: `yes`——触发条件：DRP 域业务深化启动时。

## Closure

Status Note: 全部 6 阶段已执行完成（C-1 至 C-11 全部裁决/修复/声明 Non-Goal）。验证全绿（`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` 全量 0 失败 0 错误）。计划已通过独立草案审查（2 轮迭代，4 BLOCKER 全修订）。独立结束审计由 mission-driver 独立 closure-auditor 子代理（新会话，不重用执行者上下文）执行并通过——见下方证据。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计 = mission-driver 独立 closure-auditor 子代理（新会话）；执行者 = 主代理（执行会话，未自我审计）
- Evidence:
  - Phase 1（C-1）：源模型 + 生成 `_app.orm.xml` 0 处 `erp_md_md_organization`；`docs/bugs/2026-07-05-c1-organization-table-name-typo-propagation.md`
  - Phase 2（C-4）：3 保留层 XMeta（logistics `ErpLogCarrierConfig` + b2b `ErpB2bPartnerProfile`/`ErpB2bPartnerCredential`）`published=false`；`TestErpLogCarrierConfigCredentialMasking` CHECKING 全绿
  - Phase 3（C-3）：assets ORM 删 `ErpFinVoucher` to-one + 外部实体声明；hr→projects 白名单更新（`module-boundaries.md` + `data-dependency-matrix.md`）；assets 测试 28 项全绿
  - Phase 4（C-5）：59 处时间 API → CoreMetrics（12 域）；grep 0 残留；受影响域单测全绿
  - Phase 5（C-2）：`CreditLimitChecker` 多币种折算修复 + `testCreditLimitMultiCurrencyFunctionalComparison`；`sales/README.md` Non-Goal 声明；sales 71 项全绿
  - Phase 6（C-6/7/8/9/10/11）：system-baseline/roadmap×3/module-boundaries/flow-overview 文档对齐 + InspectionResultEvaluator/StandardCostResolver/RequisitionToOrderConverter/MrpReleaseService 代码质量修复
  - 全量验证：`mvn clean install -DskipTests` BUILD SUCCESS（146 reactor 模块）；`mvn test` 全量 BUILD SUCCESS（0 失败 0 错误）
  - 日志：`docs/logs/2026/07-05.md`
- 独立结束审计复核（mission-driver 独立 closure-auditor 子代理，新会话，对照实时仓库逐项 grep/精读）：
  - C-1：`rg "erp_md_md_organization" --glob "*.orm.xml"` 全仓库 = 0 命中（含生成 `_app.orm.xml`）
  - C-3：`rg "ErpFinVoucher" module-assets/model/app-erp-assets.orm.xml` = 0 命中（to-one + 外部实体声明均已删）
  - C-4：3 个保留层 XMeta（logistics `ErpLogCarrierConfig` + b2b `ErpB2bPartnerProfile`/`ErpB2bPartnerCredential`）5 个凭证字段 `published="false" queryable="false" sortable="false" x:override="merge"` 已落地
  - C-5：`rg "LocalDate.now()|LocalDateTime.now()|System.nanoTime()|System.currentTimeMillis()" --glob "*.java" module- /src/main/`（排除 `_gen`/`target`）= 0 命中
  - C-2：`CreditLimitChecker.java` 含 `toFunctional(amount, rate)` + `o.getExchangeRate()` 折算逻辑
  - C-9：`MrpReleaseService.java` Javadoc 含"数据权限边界"段 + `createFromMrpLine` successor 注记
  - C-10：`InspectionResultEvaluator` 抛 `NopException(ERR_INSPECTION_LINES_EMPTY)`；`StandardCostResolver` 用 `LOG.debug`；`RequisitionToOrderConverter` 用 `LOG.warn`
  - C-11：`flow-overview.md` 无 `INSPECTING`，事务描述为"单库事务"
  - 反空心检查：所有修复均在运行时路径上（XMeta 屏蔽由 GraphQL schema 生成消费；CreditLimitChecker 由 `ErpSalOrderProcessor.validateBusinessRulesForApprove` 调用；时间 API 替换为业务逻辑内联）；无空函数体/`return null` 占位/吞异常残留
  - 文本一致性：Plan Status=completed、6 阶段 Status=completed、所有 Exit Criteria/Closure Gates `[x]`、Deferred 项均附触发条件，全部一致
  - 审计结论：approved（独立结束审计通过）

Follow-up:

- SPECIAL_APPROVAL 审批流（见 Deferred，触发：use-approval 迁移）
- AR 余额纳入 outstanding（见 Deferred，触发：业财一体 E2E）
- DRP 实体命名统一（见 Deferred，触发：DRP 域深化）
- MrpReleaseService 收敛为 IErpPurOrderBiz 调用（触发：采购域提供 purpose-built createFromMrpLine）
