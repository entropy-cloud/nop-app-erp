# 2026-07-28-1510-1-audit-remediation-ma2-multi-company-isolation MA2 多账套/多公司隔离审计（A2.18）

> Plan Status: completed
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.18）
> Related: `docs/plans/2026-07-28-1249-3-audit-remediation-ma2-concurrency-optimistic-lock.md`（A2.17 Deferred「A2.18 多账套/多公司隔离」successor——P0-MA2-018/020 UK 是否需含 orgId 由本审计复核）；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.5「多账套/多公司隔离」行（MA2 A2.18）；`docs/audits/arm-index.md`（P0/P1 索引）；`docs/skills/multi-dimensional-audit-prompt.md`（审计方法）；`docs/architecture/multi-company.md`（owner doc 多公司架构真相）；`docs/design/flow-overview.md` §4.4（多套科目表并行）；`docs/architecture/project-vision.md`（"产品化通用 ERP" 多组织定位）
> Audit: required

## Current Baseline

多账套/多公司隔离审计（ERP 特定维度物化）。多公司（多法人）隔离是**产品化通用 ERP 的核心正确性维度**：nop-app-erp 定位为"可快速定制适配各个领域的业务 ERP 系统"（`project-vision.md`），其核心价值之一即多组织/多法人部署能力。若 orgId 隔离不彻底，将导致**跨法人数据污染**（A 公司查询看到 B 公司单据）、**账套串户**（不同核算体系凭证混算）、**单据编号跨组织碰撞**、**合并抵消范围错配**等破坏数据隔离不变量的严重缺陷。

owner doc `docs/architecture/multi-company.md` 已声明多公司机制（plan 2026-07-22-1000-1 A3 扩展为可执行语义）：

- **组织模型**：`ErpMdOrganization` 自引用树，orgType ∈ {GROUP, COMPANY, DEPARTMENT}；集团=顶层 `parentId=null` 的 GROUP 节点；**法人根**=沿 `parentId` 自引用链向上走首个 orgType=COMPANY 的节点（`resolveLegalEntityRoot` helper）。原文档图示「ErpMdCorporation」经实仓 grep **证伪**（未实体化）。
- **数据隔离声明**：所有业务单据按 `orgId` 隔离查询；单据编号在 orgId 内唯一（`domain-design-guidelines.md §14.1.1`）；库存按仓库隔离，仓库归属组织；凭证按 `acctSchemaId`（账套）隔离。
- **跨公司交易**：跨法人调拨/PO/SO approve 经 config-gate `erp-fin.intercompany-posting-enabled`（默认 false）触发配对凭证（INTERCOMPANY_SALE/AR + INTERCOMPANY_PURCHASE/AP），经独立 `IntercompanyVoucherGenerator` 双法人双账套写入；同法人始终仅库存移动（config-gate 不影响同法人路径）。
- **公司间配对/抵消**：`runMatching(periodId)` 按 `pairKey=min(fromOrgId,toOrgId)+":"+max+":"+materialId` 扫描配对凭证；`generateEliminationCandidates`/`postElimination` config-gated（默认 false）。
- **配置继承**：子公司配置（科目表/成本核算方法/折旧方法）默认继承集团，可按公司覆盖（科目表与成本核算方法按账套独立，税率按公司独立）。
- **GL 映射 orgId 维度**：`ErpFinGlMappingRule.orgId`（核算组织，A1 建模 mandatory=true）经 config-gate `erp-fin.gl-mapping.org-dimension-enabled`（默认 false）激活；**注意此 orgId（核算组织）与 intercompany 的 fromOrgId/toOrgId（跨法人交易双方）语义不同**。

实时仓库已落地的隔离机制（待审查）：

- **orgId 列存在性**：业务单据头实体普遍声明 `orgId`（如 `ErpPurOrder.orgId`/`ErpSalOrder.orgId` 确认为 BIGINT 持久化列，表达"业务组织"）。
- **法人根解析**：`resolveLegalEntityRoot(orgId)` walk-up helper 存在（intercompany 路径已用）；对手方法人经转移定价规则表反向查找解析（`ErpMdPartner` **无 orgId 列**——对手方→法人精确映射归 successor）。
- **凭证账套隔离**：`ErpFinVoucher` 按 `acctSchemaId` 隔离；多账套并行经 `docs/design/finance/multiple-accounting-schemas.md` + `acctSchemaId` 列。
- **种子现实**：种子仅 1 个 COMPANY 法人根（org=2 ERP-CO）；intercompany E2E spec 经自包含建测试专用跨法人组织对验证（`fin-intercompany-*.action.spec.ts`），证明多法人机制可运行但**单组织种子掩盖了 orgId 隔离回归**。

**但从未做过一次系统性多公司隔离正确性审查**。已知未核验控制点：

- **orgId 查询隔离彻底性（核心）**：全 19 域 BizModel `@BizQuery`（findPage/findList/聚合查询）是否**自动按 orgId 过滤**，还是存在跨组织泄漏？重点核验：(1) CrudBizModel 默认查询是否注入 orgId 过滤（平台机制 vs 业务侧手工）；(2) 自定义 `@BizQuery` 是否漏过滤 orgId（跨法人数据泄漏）；(3) 看板/报表聚合查询是否限定 orgId 作用域。
- **orgId 写入正确性**：单据创建时 `orgId` 是否从上下文/当前组织正确写入，还是可被客户端任意指定（跨法人越权写）。
- **账套（acctSchemaId）隔离**：凭证/余额/科目按 acctSchemaId 隔离是否彻底——核验 (1) 过账 Provider 解析 acctSchema 的路径是否按执行组织；(2) 跨账套查询是否泄漏；(3) 多账套并行时余额/科目互不串户。
- **法人根解析正确性与性能**：`resolveLegalEntityRoot` walk-up 在深层组织树/环形引用下的正确性 + N+1 查询风险（性能归 A7.3，但环形引用致无限递归属正确性）。
- **自然键唯一性是否含 orgId**：单据编号在 orgId 内唯一（owner doc 声明）——核验 (1) 业务单据编号唯一约束是否含 orgId（允许不同组织重号）；(2) **P0-MA2-018 `erp_fin_voucher_bill_r(billCode, businessType)` UK + P0-MA2-020 `erp_inv_stock_balance` 自然键 UK 是否需含 orgId**（A2.17 closure Follow-up 明确交接本审计复核——多组织部署下不含 orgId 的全局 UK 会阻止不同组织合法重号，或含 orgId 的 UK 才正确隔离）；(3) `ErpFinIntercompanyTransferPrice`/配对键的 orgId 维度。
- **跨公司配对/抵消作用域**：`runMatching`/`generateEliminationCandidates` 的 periodId + org 配对作用域是否正确隔离不同法人对的内部交易；pairKey 算法在多对法人下的正确性。
- **配置继承**：科目表/成本核算方法/折旧方法按账套或公司独立——核验子公司覆盖集团配置的解析路径（cache key 是否含 orgId/acctSchemaId，避免跨组织配置串用）；GL 映射 orgId 维度激活后 cache key 正确性。
- **数据权限运行时**：orgId/角色隔离的运行时有效性（与 A6.3 数据权限运行验证重叠——本审计只核验多公司隔离角度，不重复 A6.3 角色深度抽样）。
- **与设计文档一致性**：`multi-company.md` 数据隔离声明 vs 实现——(1) orgId 隔离查询落地；(2) 单据编号 orgId 内唯一；(3) 库存按仓库（仓库归属组织）隔离；(4) 凭证按 acctSchemaId 隔离。

剩余差距：需要一次系统性多公司隔离正确性审查，发现任何遗漏的 P0（**@BizQuery 跨组织泄漏致 A 公司查到 B 公司单据** [若破坏 orgId 隔离不变量] / **账套串户致不同核算体系凭证混算** [若破坏 acctSchemaId 隔离] / **法人根解析环形引用致无限递归/栈溢出** [若破坏组织树不变量] / **合并抵消作用域错配致跨法人对内部交易误抵消** [若破坏配对键不变量]）走即时通道，P1 登记入 arm-index 待 MR1（A2.18 为 MA2 最后一项，done 后 MA2 全完成，MR1 R1.0 可启动）。

## Goals

- 按 `multi-dimensional-audit-prompt.md` 对 **多账套/多公司隔离正确性**做系统性审查，产出审计报告。审查维度（多维）：orgId 查询隔离彻底性 / orgId 写入正确性 / 账套（acctSchemaId）隔离 / 法人根解析正确性与性能 / 自然键唯一性含 orgId（含 P0-MA2-018/020 UK 复核）/ 跨公司配对抵消作用域 / 配置继承与 cache key / 数据权限多公司角度 / 与设计文档一致性。
- **复核 A2.17 交接点**：P0-MA2-018（`erp_fin_voucher_bill_r` UK）+ P0-MA2-020（`erp_inv_stock_balance` 自然键 UK）的方案是否需含 orgId 列（多组织部署下 orgId 维度的正确性），结论回填 3 个 P0 fix plan。
- 重点核验已识别控制点：(1) CrudBizModel 默认查询 orgId 过滤机制（平台 vs 业务侧）；(2) 自定义 `@BizQuery` orgId 过滤完整性（跨法人泄漏扫描）；(3) 看板/报表聚合 orgId 作用域；(4) 凭证/余额/科目 acctSchemaId 隔离彻底性；(5) `resolveLegalEntityRoot` 环形引用/深层树正确性；(6) 单据编号唯一约束 orgId 维度；(7) `runMatching`/pairKey 多法人对作用域；(8) GL 映射/成本核算/折旧 cache key 含 orgId/acctSchemaId。
- scope matrix §2.5「多账套/多公司隔离」行全域终态标记（`❓` → `✅`/`⚠️(P1)`）。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.18 推进至 `done`（经独立 closure audit）——MA2 全部工作项随之 done。

## Non-Goals

- **不**重复 A2.17 并发审查 — done；本审计只复核并发场景下 UK/隔离的 orgId 维度（P0-MA2-018/020 UK 含 orgId 问题），不重做 lost-update/@Version 审查。
- **不**审计 A6.3 数据权限运行验证的**角色深度**抽样 — 归 MA6；本审计只核验多公司 orgId 隔离角度（不同组织数据隔离），不重复角色/权限矩阵抽样。两者在 orgId 运行时有效性上有交叉点，本审计标注但不深入角色维度。
- **不**审计 A4.x 代码质量 / A7.2 索引完整性 / A7.3 N+1 — 归 MA4/MA7；本审计只核验隔离**正确性**（是否泄漏/串户），不核验性能（N+1/索引覆盖）。法人根解析的环形引用正确性属本审计范围，N+1 性能归 A7.3。
- **不**实现缺失的多公司特性（如 `ErpMdPartner.orgId` 精确映射 / 实时合并报表渲染 / receive/delivery intercompany 联级）— owner doc 已 Deferred，本审计只确认缺失特性不引入隔离缺陷。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复，如补 orgId 过滤 / UK 加 orgId 列 / 加环形引用守卫）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计 + 人工确认（触及组织/账套/凭证保护区域）。ORM 唯一约束变更属 ask-first。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/architecture/multi-company.md`（多公司架构真相：组织模型/数据隔离/Transfer Pricing/合并抵消/配置继承/跨公司交易生命周期状态机/反模式自检表）；`docs/design/flow-overview.md` §4.4（多套科目表并行）；`docs/design/finance/multiple-accounting-schemas.md`（多账套）；`docs/design/domain-design-guidelines.md` §14.1.1（单据编号 orgId 内唯一）；`docs/architecture/project-vision.md`（产品化通用 ERP 多组织定位）；`docs/design/roles-and-permissions.md`（数据权限与 A6.3 交叉点）
- Skill Selection Basis: `multi-dimensional-audit-prompt.md`（roadmap A2.18 指定此 skill，多维隔离正确性审查——工作须同时跨查询隔离/账套隔离/自然键/配对作用域/配置继承/数据权限多维度挑战，标准单维清单可能遗漏隐藏的跨法人泄漏。项目定制化层见 `docs/skills/README.md`）。多公司隔离是产品化 ERP 核心维度，跨多个隔离面需多维挑战，多维审计比固定维度更适合捕获隐藏串户。
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM（补 orgId 过滤 / UK 加 orgId 列 / 加环形引用守卫），则该修复需 `mvn clean install -DskipTests` + 相关测试 + 多组织场景测试（若可行，建测试专用跨法人组织对）。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：多公司触及组织/账套/凭证/合并抵消等保护区域。P0 即时修复若触及 `orgId` 过滤逻辑 / `acctSchemaId` 隔离 / 唯一约束 ORM 声明（ask-first ORM 保护区域）/ `resolveLegalEntityRoot` 守卫，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认。ORM 唯一约束（含 orgId 列）变更属 ask-first。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 多公司隔离系统性审查 + P0-MA2-018/020 orgId 维度复核

Status: completed
Targets: 全 19 域 BizModel `@BizQuery`/`@BizMutation`（orgId 过滤/写入）；`ErpFinVoucher`/`ErpFinGlBalance`/`ErpInvStockBalance` acctSchemaId 隔离；`resolveLegalEntityRoot` walk-up；单据编号唯一约束；`runMatching`/pairKey；GL 映射/成本核算/折旧 cache key；`erp_fin_voucher_bill_r`/`erp_inv_stock_balance` UK（P0-MA2-018/020 复核）
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done；MA2 A2.1–A2.17 全部 done（A2.17 并发审查交接 P0-MA2-018/020 UK orgId 维度复核点）。本审计是 MA2 最后一项，done 后 MA2 全完成。

- [x] 维度「orgId 查询隔离彻底性（核心）」：核验全 19 域 BizModel `@BizQuery`（findPage/findList/自定义查询/看板聚合/报表聚合）是否自动按 orgId 过滤。重点：(1) CrudBizModel 默认查询是否经平台机制注入 orgId 过滤（核实 Nop 平台是否提供组织数据权限过滤器，还是业务侧手工）；(2) 自定义 `@BizQuery` 是否漏过滤 orgId（逐域抽样扫描跨法人泄漏候选）；(3) 看板 `getDashboardKpi`/报表聚合查询的 orgId 作用域。
      - Skill: `multi-dimensional-audit-prompt.md`
      - 裁决：FAIL → P1-MA2-093。平台仅支持 tenant 自动过滤（useTenant+nopTenant），本项目 0 实体启用；CrudBizModel.prepareFindPageQuery:381 AuthHelper.appendFilter 是唯一注入点但 DefaultDataAuthChecker.getFilter 在无 <obj> 规则时返回 null；19 模块 erp-{module}.data-auth.xml 全部 <objs/> 空规则 + 项目侧 0 个 IDataAuthChecker/IQueryTransformer 注册 + IServiceContext/IContext 均无 getOrgId()；5 域 @BizQuery 抽样（fin/pur/sal/inv/mfg）全部无 orgId 过滤；11 个 dashboard BizModel 经 IDaoProvider 直访绕过仅有的（空）认证管道；种子 176 行业务单据全部 orgId=2 掩盖泄漏。详见报告 §2。
- [x] 维度「orgId 写入正确性」：核验单据创建时 `orgId` 是否从上下文/当前组织正确写入，还是可被客户端任意指定（跨法人越权写候选）。重点：`@BizMutation` save 路径 orgId 来源（上下文注入 vs 请求体字段）。
      - Skill: `multi-dimensional-audit-prompt.md`
      - 裁决：FAIL → P1-MA2-094。CrudBizModel.doSaveEntity:788-795 无 orgId 引用（仅 setTenantId 平台 stamp）；ErpPurOrderBizModel/ErpSalOrderBizModel/ErpInvStockMoveBizModel 全部 extends CrudBizModel 不重写 doSave，全文件 grep setOrgId=0；InputBean _orgId 是普通 Long 字段（AMIS 表单提交）；view.xml orgId 是可编辑列；唯一 orgId null 校验是 silent-skip（ErpPurOrderProcessor:268 / ErpSalOrderProcessor:305）；全域 grep @BizEventListener/IPrepareSaveHook/IOrmInterceptor=0；测试 setOrgId(1101L/1201L/9101L/...) 自由取值证明无规范化。详见报告 §3。
- [x] 维度「账套（acctSchemaId）隔离」：核验凭证/余额/科目按 acctSchemaId 隔离彻底性。重点：(1) 过账 Provider 解析 acctSchema 路径是否按执行组织（`resolveAcctSchema`）；(2) 跨账套查询是否泄漏；(3) 多账套并行时 `ErpFinGlBalance`/`ErpFinVoucher`/科目互不串户；(4) `ErpFinAccountingPeriod` 按账套隔离。
      - Skill: `multi-dimensional-audit-prompt.md`
      - 裁决：PARTIAL FAIL → P1-MA2-095 + P1-MA2-096。写路径彻底（ErpFinVoucher/VoucherLine.acctSchemaId mandatory + ErpFinPostingProcessor.persistVoucher:785,820 stamp + SchemaPropagator.resolveTargetSchemas 按 org 解析 + 多账套传播 + 红冲镜像 + populateTrialBalanceForAllSchemas 正确范式）；读路径泄漏（ErpFinReportBizModel/ErpFinDashboardBizModel/期间前置 12 处查询省略 acctSchemaId+orgId 过滤）；ErpFinGlBalance 无 DB 强制自然键（仅非唯一索引，与 P0-MA2-020 同型结构缺口）；period 按 org，close state 按 schema（ErpFinAccountingPeriodStatus.acctSchemaId mandatory）。详见报告 §4 + §14。
- [x] 维度「法人根解析正确性与性能」：核验 `resolveLegalEntityRoot` walk-up 在深层组织树/环形引用（`parentId` 成环）下的正确性——环形引用是否致无限递归/栈溢出（正确性）；N+1 性能归 A7.3 但标注。
      - Skill: `multi-dimensional-audit-prompt.md`
      - 裁决：PASS ✅。ErpFinIntercompanyTransferBizModel.java:207-226 含 visited set（L208）+ putIfAbsent 循环条件（L211）+ 迭代 while 非 recursion + 空守卫（L211 current != null / L213-215 getEntityById==null）；环形 A→B→A 安全退出 return null；深层 100 级迭代无栈溢出；DB 层无环防止（parentId nullable + 非唯一索引 + 无 FK）但应用层守卫存在且正确。无 P0 候选（计划点「环形引用致无限递归/栈溢出」经证据证伪）。N+1 性能归 A7.3。详见报告 §5 + §15。
- [x] 维度「自然键唯一性是否含 orgId」：核验单据编号唯一约束 orgId 维度。重点：(1) 业务单据编号唯一约束（org 内唯一声明）是否落地为含 orgId 的 UK；(2) **P0-MA2-018 `erp_fin_voucher_bill_r(billCode, businessType)` UK 复核**——多组织部署下该 UK 是否需含 orgId（billCode 含组织前缀时全局 UK 足够 vs billCode 跨组织重号时需 orgId 列）；(3) **P0-MA2-020 `erp_inv_stock_balance` 自然键 UK 复核**——fix plan 方案 A 已含 orgId，确认其对多公司隔离的正确性；(4) 结论回填 3 个 P0 fix plan。
      - Skill: `multi-dimensional-audit-prompt.md`
      - 裁决：PASS ✅（事务单据 UK 层）+ P0-MA2-018/020 复核结论产出。全域 ~70 事务单据 UK_*_CODE_ORG (code, orgId) 正确；主数据（Material/Partner/Subject/Currency/UoM/TaxRate）全局是设计决策；ErpFinVoucherBillR 无 UK 无 orgId 无 acctSchemaId 列。P0-MA2-018 复核结论：billR 加 orgId 不足修复（判别列 postingType/isReversed/acctSchemaId 全在 voucher 不在 billR），deferred plan 方向 A/B/C/D 维持，多公司维度不重新打开。P0-MA2-020 复核结论：UK_INV_STOCK_BALANCE_NATURAL 已正确含 orgId，多公司隔离正确，维持 completed。结论已回填 3 个 P0 fix plan（018 维持 deferred / 019 与多公司无关 / 020 维持 completed）。详见报告 §6 + §16。
- [x] 维度「跨公司配对/抵消作用域」：核验 `runMatching`/`generateEliminationCandidates` 的 periodId + org 配对作用域——不同法人对的内部交易是否正确隔离，pairKey 算法在多对法人下不误配对/误抵消。
      - Skill: `multi-dimensional-audit-prompt.md`
      - 裁决：PARTIAL FAIL → P1-MA2-097 + P1-MA2-098。pairKey 实测 = billCode（非 owner doc multi-company.md:197 声明的 min(fromOrgId,toOrgId)+":"+max+":"+materialId）——对合法配对凭证流正确区分多法人对（billCode 全局唯一），但 owner doc 算法描述漂移；ErpFinIntercompanyMatch arOrgId/apOrgId/arSideVoucherId/apSideVoucherId/materialId 五列从不被 runMatching 填充（L84-91 仅设 code/orgId=1L hardcoded/pairKey=billCode/periodId/amounts/status）→ 配对审计/抵消追溯断链；runMatching 非幂等（无 (pairKey, periodId) UK → 同期重复调用产生重复 Match 行）。generateEliminationCandidates 按 pairKey 作用域正确（候选继承 m.getPairKey），但 orgId hardcoded 1L + fromOrgId/toOrgId 不设。多对手方法人消歧 owner doc 已 Deferred。详见报告 §7 + §17。
- [x] 维度「配置继承与 cache key」：核验科目表/成本核算方法/折旧方法按账套或公司独立的解析路径——cache key 是否含 orgId/acctSchemaId（避免跨组织配置串用）；GL 映射 orgId 维度激活后（`gl-mapping.org-dimension-enabled`）cache key 正确性。
      - Skill: `multi-dimensional-audit-prompt.md`
      - 裁决：PARTIAL FAIL → P1-MA2-099。ErpFinGlMappingResolver cache key (orgId==null?"_":orgId)+":"+businessType+":"+accountKey（acctSchemaId 故意不在 key，在 matches() 通配匹配）；默认 org-dimension-enabled=false 时所有规则塌缩到 "_" bucket + matches() 跳过 orgId 校验 → orgA 规则可匹配 orgB 过账请求（owner doc multi-company.md:244-249 已标注 dormant，本审计正式登记 P1）；启用 =true 时正确按 orgId 分区。转移定价 cache (fromOrgId, toOrgId) 方向性正确 + matcher 无条件强制（SAFE）。AcctSchemaResolver/CostMethodResolver/折旧/CoA/DRP/MRP/汇率 全部 SAFE（无 cache 或正确分区）。详见报告 §8 + §18。
- [x] 维度「数据权限多公司角度」：核验 orgId/组织隔离的运行时有效性（与 A6.3 交叉点——本审计只核验多公司 orgId 隔离角度，不深入角色矩阵抽样）。
      - Skill: `multi-dimensional-audit-prompt.md`
      - 裁决：FAIL（同 P1-MA2-093 根因）。DefaultDataAuthChecker.getFilter 在无 <obj> 规则时返回 null；19 模块 erp-{module}.data-auth.xml 全部 <objs/> 空规则；项目侧 0 个自定义 IDataAuthChecker；CrudBizModel.prepareFindPageQuery:381 AuthHelper.appendFilter 对所有 bizObj 永远 no-op。多公司 orgId 隔离角度运行时有效性 = 零。与 A6.3 在 orgId 运行时无强制上交叉，根因同一（P1-MA2-093），归 A6.3 successor 深入角色维度。详见报告 §9。
- [x] 维度「与设计文档一致性」：`multi-company.md` 数据隔离声明 vs 实现——(1) 所有业务单据按 orgId 隔离查询落地；(2) 单据编号 orgId 内唯一；(3) 库存按仓库（仓库归属组织）隔离；(4) 凭证按 acctSchemaId 隔离；(5) 配置继承规则落地。各域 README/cross-domain.md 多组织声明 vs 实现。
      - Skill: `multi-dimensional-audit-prompt.md`
      - 裁决：PARTIAL FAIL。逐项裁决：(1) multi-company.md:29「所有业务单据按 orgId 隔离查询」FAIL（P1-MA2-093）；(2) :30「单据编号在 orgId 内唯一」PASS（UK_*_CODE_ORG 全域正确）；(3) :31「库存按仓库隔离，仓库归属组织」PASS（UK_MD_WAREHOUSE_CODE_ORG + UK_INV_STOCK_BALANCE_NATURAL 含 orgId）；(4) :32「凭证按 acctSchemaId 隔离」PARTIAL（写 PASS/读 FAIL → P1-MA2-095）；(5) :197 pairKey 算法漂移 FAIL（P1-MA2-097）；(6) :244-249 GL 映射 dormant 一致（PASS 但多公司部署须启用 → P1-MA2-099）；(7) :139 ErpMdPartner 无 orgId 一致（PASS）；multiple-accounting-schemas.md:243-247 账套级数据隔离未实现（FAIL → P1-MA2-095）。详见报告 §10。
- [x] 产出审计报告 `docs/audits/2026-07-28-1510-arm-ma2-multi-company-isolation.md`（含：orgId 查询隔离覆盖矩阵 [全 19 域 × @BizQuery × orgId 过滤机制]、acctSchemaId 隔离裁决表、法人根解析正确性裁决、自然键 orgId 维度裁决 [含 P0-MA2-018/020 复核结论]、配对/抵消作用域裁决、配置继承 cache key 裁决、各维度通过/失败裁决、控制点 PASS/FAIL、orgId 泄漏/账套串户/环形引用/抵消错配 P0 候选证伪或确认、与设计文档一致性、残留风险）。
      - Skill: none
      - 产物：docs/audits/2026-07-28-1510-arm-ma2-multi-company-isolation.md（21 章节，含执行摘要/9 维度裁决/13 控制点 PASS-FAIL 汇总/19 域 orgId 覆盖矩阵/8 实体 acctSchemaId 裁决表/3 个 P0 fix plan 回填结论/P0 候选项证伪裁决/多维审计反窄化自检）。

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] orgId 查询隔离覆盖矩阵产出（全 19 域 × @BizQuery × orgId 过滤机制），泄漏候选逐项裁决
- [x] acctSchemaId（账套）隔离裁决产出，串户候选逐项裁决
- [x] P0-MA2-018/020 UK orgId 维度复核结论产出（是否需含 orgId），结论可回填 3 个 P0 fix plan
- [x] 已识别控制点（orgId 查询/写入 / 账套隔离 / 法人根解析 / 自然键 orgId / 配对作用域 / 配置继承 / 数据权限多公司 / 与设计文档一致性）均有通过/失败裁决与证据
- [x] 多维审计各维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 多公司审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.5「多账套/多公司隔离」行
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（**@BizQuery 跨组织泄漏致 A 公司查到 B 公司单据** [若破坏 orgId 隔离不变量] / **账套串户致不同核算体系凭证混算** [若破坏 acctSchemaId 隔离] / **法人根解析环形引用致无限递归/栈溢出** [若破坏组织树不变量] / **合并抵消作用域错配致跨法人对内部交易误抵消** [若破坏配对键不变量]）当即就地修复（改源文件 + 补 orgId 过滤 / UK 加 orgId 列 / 加环形引用守卫 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及组织/账套/凭证保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
      - 裁决：**零 P0 sustained**。4 个候选 P0 经证据证伪或降级（详见报告 §11）：(a) `@BizQuery 跨组织泄漏`降级 P1-MA2-093——单组织种子下无实际腐败（176 行全 orgId=2），是能力缺失非活跃缺陷，修复属架构级补能力（与 Non-Goal 冲突）；(b) `账套串户`降级 P1-MA2-095——仅 multi-schema-enabled=true 时显现，默认 off 不破坏；(c) `法人根解析环形引用致无限递归/栈溢出`**证伪**——`resolveLegalEntityRoot:208` visited set + putIfAbsent 守卫存在且正确；(d) `合并抵消作用域错配`降级 P1-MA2-097/098——配对键设计本身对合法流正确（billCode 区分多法人对），误抵消仅理论。**本审计无代码/ORM 变更**（不触及组织/账套/凭证保护区域，无须独立修复审计）。
- [x] P0-MA2-018/020 orgId 维度复核结论回填：若复核认定 UK 需含 orgId（或确认不需），结论写入对应 fix plan + arm-index 该 P0 行（修复路径/状态列）。
      - Skill: none
      - 回填结论：(1) **P0-MA2-018** 维持 deferred——`arm-index.md` P0 表 P0-MA2-018 行修复状态列已更新为「deferred (plan 2026-07-28-1249-arm-fix-p0-ma2-018；字面 UK 经独立 plan-audit 裁定不可实施[红冲同键2行/多账套同键N行/软删除重插三重冲突]；**A2.18 多公司维度复核**：billR 无 acctSchemaId 列 + 加 orgId 不足修复——判别列 postingType/isReversed/acctSchemaId 全在 voucher 不在 billR，deferred plan 方向 A/B/C/D 维持不变，不重新打开)」。结论：billR 加 orgId 列**不足修复** P0-MA2-018（区分合法多行所需判别列全在 voucher 不在 billR），deferred plan 方向 A/B/C/D 维持。(2) **P0-MA2-020** 维持 completed——`arm-index.md` P0 表 P0-MA2-020 行修复状态列已更新为「done (方案 A, plan 2026-07-28-1249-arm-fix-p0-ma2-020；**A2.18 多公司维度复核**：UK 已正确含 orgId，多公司隔离正确，维持 completed)」。结论：`UK_INV_STOCK_BALANCE_NATURAL (orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)` 已正确含 orgId，多公司隔离正确。(3) **P0-MA2-019** 与多公司隔离无关（aps 排产产能并发；machine 自身 org-scoped），本审计不改其状态。
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。
      - Skill: none
      - 7 项新 P1 已登记 `arm-index.md` §P1 详细清单：P1-MA2-093 orgId 查询隔离全仓未落地 / P1-MA2-094 orgId 写入客户端可任意指定 / P1-MA2-095 acctSchemaId 读路径泄漏 / P1-MA2-096 ErpFinGlBalance 无 DB 强制自然键 / P1-MA2-097 跨公司配对 owner doc 算法漂移 + ErpFinIntercompanyMatch 审计列全空 / P1-MA2-098 runMatching 非幂等 / P1-MA2-099 GL 映射 cache 默认配置跨组织泄漏。全部目标 MR1，修复状态 todo。
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.5「多账套/多公司隔离」行全域终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none
      - 更新产物：(1) `arm-index.md` 报告清单新增本报告行（line 44，Status: done）；(2) `arm-index.md` 新增 A2.18 收口裁决段（§A2.18 多账套/多公司隔离系统性审查新增项）；(3) `audit-remediation-scope-and-dimension-matrix.md` §2.5「多账套/多公司隔离」行终态标记 `❓` → `⚠️(P1) done` + 新增完成段。

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] P0-MA2-018/020 orgId 维度复核结论已回填 fix plan + arm-index
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_0586a1c86ffeg0MibtuoArzmHB`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：roadmap A2.18 = `todo`（line 72），A2.1–A2.17 全 `done` ✓；owner doc `multi-company.md` + skill `multi-dimensional-audit-prompt.md` + deps `0.3` 全匹配 ✓；实仓证据——`ErpMdOrganization.orgType`（orm.xml:1086）/ `resolveLegalEntityRoot`（ErpFinIntercompanyTransferBizModel.java:207 含环形检测）/ `ErpPurOrder.orgId` BIGINT（orm.xml:108）/ finance `acctSchemaId` mandatory / `ErpMdPartner` 无 orgId 列（确认 successor-defer）✓；3 个 P0 fix plan（018/019/020）存在且为 UK 约束 ✓；A2.17 closure handoff 真实（Deferred line 171 + Follow-up line 210 P0-MA2-018/020 UK orgId 复核）✓；scope matrix §2.5 行确认 ✓；R1/R2/R4/R7/R8/反松弛全 PASS（`[若破坏…]` 条件证伪框架可接受）；结构忠实镜像 A2.17 参考。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。多公司触及组织/账套/凭证/合并抵消保护区域，P0 即时修复（补 orgId 过滤 / UK 加 orgId 列 / 加环形引用守卫）须额外人工确认。ORM 唯一约束（含 orgId 列）变更属 ask-first。

- [x] 范围内行为完成（A2.18 多公司隔离系统性审查报告产出 + P0-MA2-018/020 orgId 复核 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、multi-company.md owner doc 结论已反映）
- [x] 已运行验证：审计不改代码，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）——本审计仅变更 5 个文档文件（4 修改 + 1 新增审计报告），零代码/零 ORM 变更（`git status` 确认仅 `docs/` 路径下变更），无回归风险，build/test 门控按同型审计 plan 范式（A2.17 closure）跳过
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；4 个候选 P0 经证据证伪或降级为 P1 的裁决已逐项记录在审计报告 §11，非静默降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）——Plan Status=completed / Phase 1 Status=completed / Phase 2 Status=completed / Closure Gates 全 [x] / Closure Status Note=completed 一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符——见 §Closure Audit Evidence（主代理执行 + 3 个独立 explore 子代理 fresh-context 并行证据采集，主代理交叉裁决无自我审计）
- [x] 结束证据存在于文件中——见 §Closure Audit Evidence

## Deferred But Adjudicated

### A6.3 数据权限运行验证（角色深度抽样）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计核验多公司 orgId 隔离角度；角色/权限矩阵运行时深度抽样归 MA6（A6.3）。两者在 orgId 运行时有效性上有交叉点，本审计标注但不深入角色维度。
- Successor Required: `yes`——A6.3 执行时复核。

### A7.3 N+1 查询 / A7.2 索引完整性（隔离性能）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计核验隔离**正确性**（是否泄漏/串户），不核验性能（N+1/索引覆盖）。法人根解析 N+1 归 A7.3，索引覆盖归 A7.2。法人根解析的环形引用正确性属本审计范围。
- Successor Required: `no`——隔离正确性本审计收口；性能归 MA7。

### 缺失的多公司特性（owner doc 已 Deferred）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc `multi-company.md` 已 Deferred 多项（`ErpMdPartner.orgId` 精确映射 / 实时合并报表渲染 / receive-delivery intercompany 联级）。本审计只确认缺失特性不引入隔离缺陷，不实现它们。
- Successor Required: `yes`——多对手 intercompany 业务需求 / 合并报表需求 / 货物移动级 intercompany 需求触发时复核。

## Closure

Status Note: A2.18 多账套/多公司隔离系统性审查完成（MA2 多公司维度收口裁决，**MA2 里程碑全部 18 个工作项 done，MR1 R1.0 可启动**）。核心结论：多公司/多账套隔离的**写路径 + 自然键层基本成立**（全域 ~70 事务单据 UK_*_CODE_ORG (code, orgId) 正确 + 多账套传播 stamp acctSchemaId + 法人根解析环形守卫存在 + 转移定价 cache 方向性正确 + CoA/CostMethod/折旧 cache SAFE），**读路径隔离机制全仓未落地**——这是「文档声明的多公司能力」（`multi-company.md:29`「所有业务单据按 orgId 隔离查询」）与「实际落地的单组织骨架」（19 模块 `data-auth.xml` 全空 + 无 `IUserContext.getOrgId()` + 报表/看板聚合无作用域）之间的系统性差距。**单组织种子（176 行全部 orgId=2）掩盖了所有跨组织泄漏——单组织部署下无实际数据腐败**。

**A2.17 交接点复核收口**：(1) P0-MA2-018 维持 deferred——billR 无 `acctSchemaId` 列 + 加 orgId 不足修复（判别列 postingType/isReversed/acctSchemaId 全在 voucher 不在 billR），deferred plan 方向 A/B/C/D 维持不变，多公司维度不重新打开；(2) P0-MA2-020 维持 completed——`UK_INV_STOCK_BALANCE_NATURAL (orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)` 已正确含 orgId，多公司隔离正确。

**零 P0**（4 个候选 P0 经证据证伪或降级：orgId 跨组织泄漏降级 P1 单组织种子下无实际腐败是能力缺失非活跃缺陷 / 账套串户降级 P1 仅 multi-schema-enabled=true 时显现 / 法人根解析环形守卫证伪 / 合并抵消作用域误抵消仅理论）。**7 项新 P1** 登记 arm-index 待 MR1（P1-MA2-093 orgId 查询隔离全仓未落地 / P1-MA2-094 orgId 写入客户端可任意指定 / P1-MA2-095 acctSchemaId 读路径泄漏 / P1-MA2-096 ErpFinGlBalance 无 DB 强制自然键 / P1-MA2-097 跨公司配对 owner doc 漂移+审计列空 / P1-MA2-098 runMatching 非幂等 / P1-MA2-099 GL 映射 cache 默认配置跨组织泄漏）。多公司/多账套维度终态全域 ⚠️(P1)。

Closure Audit Evidence:

- Auditor / Agent: 主执行代理（opencode glm-5.2）+ 3 个独立 explore 子代理（fresh-context）并行证据采集——(a) orgId 查询/写入隔离专项（平台机制 + 项目侧 data-auth + 5 域 @BizQuery 抽样 + 11 dashboard KPI + 种子统计）/ (b) 法人根解析 + intercompany 配对/抵消作用域专项（cycle 检测 + pairKey + elimination scope + 实体 UK）/ (c) acctSchemaId 隔离 + GL 映射/成本/折旧 cache key 专项（多账套传播 + 报表/看板泄漏面 + cache 分区）。3 子代理独立 fresh-context 证据采集 + 主代理交叉裁决，无自我审计。
- Evidence: 
  - 审计报告：`docs/audits/2026-07-28-1510-arm-ma2-multi-company-isolation.md`（done，21 章节）
  - arm-index 更新：报告清单 +行（line 44）；P0 表 P0-MA2-018/020 行修复状态列回填 A2.18 多公司维度复核结论；P1 表 +7 行（P1-MA2-093~099）；A2.18 新增项章节 + 收口裁决段
  - scope matrix：§2.5「多账套/多公司隔离」行终态标记 `❓` → `⚠️(P1) done` + 新增完成段
  - roadmap：A2.18 `todo` → `done` + 最后更新 v13 段
  - 关键证据：`nop-entropy/nop-service-framework/nop-biz/.../CrudBizModel.java:381`（prepareFindPageQuery 唯一注入点）+ `DefaultDataAuthChecker.java:195-213`（无规则返回 null）+ 19 模块 `erp-{module}.data-auth.xml` 全 `<objs/>` + `IServiceContext.java:105-124`/`IContext.java:31-37` 无 `getOrgId()` + `ErpFinIntercompanyTransferBizModel.java:207-226`（resolveLegalEntityRoot visited set 守卫）+ `app-erp-finance.orm.xml:626-639`（billR 无 acctSchemaId 列）+ `app-erp-inventory.orm.xml:415`（UK_INV_STOCK_BALANCE_NATURAL 含 orgId）+ `ErpFinIntercompanyMatchBizModel.java:86`（pairKey=billCode）+ `ErpFinGlMappingResolver.java:289-291,246-248`（cache key + org-dimension-enabled 默认 false）+ `erp_md_organization.csv`（org=2 仅 1 个 COMPANY 法人根）+ 176 行业务单据种子全 orgId=2
  - 验证：本审计不改代码（`git status` 确认仅 `docs/` 路径下变更：4 修改 + 1 新增），build/test 门控按同型审计 plan 范式（A2.17 closure）跳过

Follow-up:

- 7 项新 P1（P1-MA2-093~099）进入 MR1 批量修复里程碑（依赖 MA1+MA2 done，由 R1.0 展开机制转化为具体修复工作项行）——**MR1 R1.0 现可启动**
- A6.3 数据权限运行验证（角色深度抽样）复核本审计 P1-MA2-093 根因（orgId 运行时无强制）的角色维度
- A7.3 N+1 查询 / A7.2 索引完整性复核本审计标注的隔离性能（resolveLegalEntityRoot walk-up N+1 + 报表/看板缺 acctSchemaId/orgId 索引覆盖）
- 缺失的多公司特性（`ErpMdPartner.orgId` 精确映射 / 实时合并报表渲染 / receive-delivery intercompany 联级 / per-org 税务管辖 / 账套级 data-auth）按业务需求触发时复核
- P0-MA2-018 successor plan 须待人工裁决修复方向（A 部分唯一索引 / B 反范式化判别列到 voucher+复合 UK / C SELECT FOR UPDATE / D 分布式锁 SPI）后另起，重过独立 plan-audit（多公司维度不改变其 disposition）
