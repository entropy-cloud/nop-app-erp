# 2026-09-02-1415-2 M1.5 第二批扩展域 seed 扩面——b2b + contract + drp（39 CSV）

> Plan Status: completed
> Last Reviewed: 2026-09-03
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.5（ready，Deps M0.1 + M0.2 均 done）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁常量随批更新协议）、同批 `2026-09-02-1415-1-m14b-aps-logistics-seed-expansion.md`（N=1，先行，共享门禁常量与对账表触点）
> Audit: required

## Current Baseline

- 实仓 363 个 `app.erp.*` 实体（className 口径；运行时注册 368 = 363 + finance 5 缺 className 补充档，finance 5 已由 M1.2a1 消费），M1.4b 落地后有 seed 329 / 精确缺 39（本批 39 = b2b 13 + contract 15 + drp 11，即 M1 全量覆盖的收尾批）；`_init-data/` 基数 318 CSV（314 app.erp + 4 平台）+ 1 SQL（M1.4b 后 333 CSV）；`TestErpSeedDataIntegrity` 基线常量 `EXPECTED_APP_ERP_CSV_COUNT = 314`（随批更新协议：基数以「314 + M1.4b 15」= 329 起算，本批 +39 → 368）/ `EXPECTED_PLATFORM_CSV_COUNT = 4` / `EXPECTED_APP_ERP_ENTITY_COUNT = 363`（+ finance 5 缺 className 补充集，不触碰）。
- 三域实测已 seed / 缺 seed（与 roadmap M1.5 清单一致，均为零 seed 域）：
  - b2b：已 seed **0**，缺 **13**（asn / asn_line / certification_checklist / code_mapping / edi_doc / edi_format / edi_log / mft_certificate / mft_config / mft_log / partner_credential / partner_profile / test_exchange）；
  - contract：已 seed **0**，缺 **15**（approval_matrix / approval_record / consumption_line / contract / contract_line / contract_version / document / invoice_plan / rebate_accrual / rebate_agreement / rebate_settlement / rebate_tier / signature_request / template / volume_discount）；
  - drp：已 seed **0**，缺 **11**（drp_line / drp_parameter / drp_plan / drp_scenario / drp_scenario_param / drp_scenario_version / inv_drp_cross_dock / inv_drp_dock_appointment / inv_drp_lead_time_record / inv_drp_safety_stock_calc / inv_drp_supplier_score）。
  - 逐实体规格见 `docs/architecture/seed-data.md` 规格表 b2b / contract / drp 三节，本计划逐行引用不复制。ORM tableName 已实仓核实：`erp_b2b_*` × 13 + `erp_ct_*` × 15 + `erp_drp_*` × 6 + `erp_inv_drp_*` × 5（`module-b2b` / `module-contract` / `module-drp` 三域 `model/*.orm.xml`）。
- 当前验证基线：`mvn test -pl app-erp-all` **71/0/0/1** 全绿（M0.2 实测，经七批 M1.x 维持）；compliance 门控锚点 = `docs/audits/compliance-baseline.md` §BASELINE 机器块（R2c: 1537），**R2c=1542 / R2b=242 / R12a=71** 为已登记实测值（+5/+2/+1 pre-existing，沿 plan `2026-09-01-0527-1` 口径协议）。
- 既有 CSV 约定：列头为 DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000；装载拓扑序由 `DataInitInitializer` 自动排序；CSV 装载经 `dao.saveEntity` 直插，不触发 biz mutation / 状态机 / Processor（前七批 M1.x 先例，本计划 Phase 1 复证——b2b 状态机 `ErpB2bAsnStateMachine` 与 retry/match Processor 族为事件驱动，预期不被 CSV 直插触发）。
- 冻结时钟纪律（`docs/bugs/2026-09-01-0017` / `2026-09-01-0058` 家族教训）：本批全部 seed 行日期列使用静态固定值，禁止 `now()`/滚动期间语义（contract / contract_version / signature_request / rebate_agreement / rebate_settlement / edi_doc / edi_log / mft_log / dock_appointment / cross_dock 时效类实体尤其注意）。
- **干扰面**（本计划核心风险，读取面均 2026-09-02 实仓核实）：
  - **b2b 域**：app-erp-all 集成用例 `TestErpC19B2bAsnAutoReceiveLandedCost`（C19）——fixture 直建 `status=RECEIVED` 的 ASN + **真实** relatedBillType（PO_ORDER / PURCHASE_RECEIPT）+ `matchPurchaseOrder` by-id（草案审查 iteration 1 勘误：`E2E_` 标记族属 dashboard value specs 专用，C19 无 E2E 标记；执行期 plan-audit iteration 1 Minor-4/5 勘误：ASN 真终态 = `RECEIVED_TO_STOCK`/`CANCELLED`，`matchPurchaseOrder` 实证无 relatedBillType 过滤、按显式 asnId 事件驱动）；种子 ASN 行须**全终态（RECEIVED_TO_STOCK / CANCELLED 承载）且 relatedBillType/code 避开 `E2E_` 标记族与既有 fixture UK**；`ErpB2bAsnMatchPurchaseOrderProcessor` / `ErpB2bAsnRetryMatchProcessor` / `ErpB2bAsnCreateReceiveFromAsnProcessor` 均为 per-mutation 事件驱动 Processor（草案审查 iteration 1 实证 + 执行期预分析 (a) 复证，不被 CSV 直插触发）；E2E 消费面 `tests/e2e/dashboards/b2b-asn-flow.value.spec.ts` + `b2b-edi-detail.value.spec.ts`——自包含 setup（`E2E-` 前缀 code + get-by-id + 客户端 asnId/ediDocId 过滤 + 清理），种子行仅增大 findPage 全集（无害），但种子 relatedBillType/code 禁用 `E2E_` 标记族；`tests/e2e/business-actions/` b2b 3 个 action spec（asn-match-receive / asn-line-level-receive-fill / edi-doc）+ `b2b.smoke.spec.ts`。
  - **contract 域**：app-erp-all 集成用例 `TestErpC18CtLifecycleRebate`（C18）——fixture 自建 contract/version/rebate agreement，`ErpCtRebateAgreement__runAccrual`（PERIOD_END 聚合**已过账 AR 发票**）——草案审查 iteration 1 实证聚合为 partner+period-scoped（filter supplierId/customerId + 窗口）、accrued 去重 agreement-scoped，种子 agreement 行不被 runAccrual 调用 → 零漂移可达（Phase 1 复证并登记）；`tests/e2e/business-actions/` ct 5 个 action spec（contract-lifecycle / contract-version / invoice-plan-trigger / rebate-accrual / rebate-settlement）+ `contract.smoke.spec.ts`。
  - **drp 域**：app-erp-all 集成用例 `TestErpC20aDrpNetRequirementRelease`（C20a，`ErpDrpParameter__save` fixture ×2 + `ErpDrpPlan__runDrp`）+ `TestErpC20bDrpSimulationPromote`（C20b）——**载重避让判据 = ORG_ID**（草案审查 iteration 1 实仓核出）：`DrpDemandAggregator.loadParametersInScope` 仅按 orgId 过滤（plan.orgId 为 null 时零过滤），`DrpEngine` 为每条 in-scope parameter 行生成 `ErpDrpLine`（无零净额跳过），C20a/C20b fixture 全部 `orgId="2"` 且 C20a 断言 `lines.size()==2`——**种子 `erp_drp_parameter` 行 ORG_ID 禁取 "2"**（取 "1" 等 `erp_md_organization.csv` FK 合法组织），(warehouseId, materialId) 组合避让为 belt-and-braces；C20b `ErpDrpSimulationParamResolver` 为 scenarioId-scoped（缓存测试内失效）预期零交互（Phase 1 复证登记）；预存回归 `TestErpDrpCrossDock#testStagingTimeoutFallbackJob` 位于 `module-drp/erp-drp-service`（模块级测试，classpath 不含 app-erp-all `_init-data` 种子，预期零交互）——Phase 1 复证该 classpath 判定 + `ErpDrpCrossDockStagingTimeoutJob` 行扫描过滤面（status=STAGING + updateTime > 24h 窗口，草案审查 iteration 1 实证；种子 cross_dock / dock_appointment 行取 N-TERM 终态避让）；`tests/e2e/business-actions/` drp 5 个 action spec + `drp.smoke.spec.ts`。
  - **跨域 E2E 视觉消费面**：`tests/e2e/visual/ext-domains-child-table.visual.spec.ts` 覆盖 `ErpB2bAsn-main`（1 sub-grid）/ `ErpCtContract-main`（2 sub-grids）/ `ErpDrpPlan-main`（view drawer lines sub-grid-view）——现依赖 no-row 分支，本批种子落地后激活真实行路径（结构断言预期仍通过，Phase 3 实跑核验）；`ext-domains-list-filter.visual.spec.ts` 覆盖 `ErpB2bEdiDoc-main` asideFilter（formatId + state in）+ `ErpB2bEdiLog-main` readonly。
  - **跨用例消费面补充**（草案审查 iteration 1 实仓核出）：`_cases` grep `ErpB2b|ErpCt|ErpDrp` 实际命中 **6** 个用例——除 C18/C19/C20a/C20b 外还有 `TestErpC02PurReturnRefund`（C02，消费 `erp_ct_contract`：contract save/get + `output/tables/erp_ct_contract.csv` 快照表文件）与 `TestErpC03O2cGoldenPath`（C03，消费 `erp_b2b_edi_doc` / `erp_b2b_edi_log`：完整 EDI inbound flow，位于 b2b EDI 保护区段影响面内）——预分析 (g) 覆盖 + C03 EDI inbound 查找语义（by-code vs 扫描）子核验。
  - **集成快照**：`app-erp-all/_cases` 集成用例快照按 `_chgType` 增量机制评估（M1.2b 实证纯加性插入不入既有快照）；上列 6 个用例无 findPage/totalCount 种子计数断言预期（Phase 1 grep 复证）。
- **保护区域**：**b2b EDI 凭据/外部集成段 = plan-first**（roadmap 横切关注点 1：EDI 凭据触 deployment/external integrations 区域）——本计划在 Phase 1 内置显式独立 plan-audit 门控项（批准后方可落地 b2b EDI 段 CSV，沿 M1.2c notify 段先例），`ErpB2bPartnerCredential`（P+N-DIS）与 `ErpB2bEdiFormat` / `ErpB2bMftConfig` / `ErpB2bTestExchange` 相关行的凭据/端点字段一律 masked placeholder 值（禁真实密钥/真实端点），N-DIS 行 `IS_ACTIVE=false` 停用承载；**masked placeholder 纪律显式扩展至 `ErpB2bPartnerProfile` 的 `TRANSPORT_ENDPOINT` / `WEBHOOK_SECRET` / `CERT_FINGERPRINT` / `CONTACT_*` 列（执行期独立 plan-audit iteration 1 Major-2 扩展，本批 in-batch FK 锚点实体同属凭据/端点敏感承载）**；contract/drp 段无额外保护区。 ORM / 敏感字段 / 会计过账 / 视觉 mask 四类不触及。
- **Deferred 消费**：`docs/architecture/seed-data.md` §Non-Goals「其他扩展域交易种子（logistics/b2b/contract/drp/aps 后续批次）」Deferred 之 **b2b + contract + drp 子集**——本计划按 roadmap M1 全量覆盖口径消费该 Deferred（沿 M1.2b/M1.4a/M1.4b 消费先例；本批消费完成后该 Deferred 全部子集清零）。
- **owner doc 目录映射**：roadmap M1.5 行记 `docs/design/b2b/` + `contract/` + `drp/`；物理目录三者均实存（2026-09-02 核实），owner doc 落三目录下新建 `seed-data.md`。
- **M1 收尾语义**：本批 39 CSV 落地后精确缺 seed 54→39（M1.4b 后）→ **0**，roadmap M1 里程碑 11 项全部 `done`（有 seed 368 = 运行时口径全覆盖），解锁 M2.x 按域全量推进与 M3.1 前置。

## Goals

- 39 个 seed CSV 落地（b2b 13 + contract 15 + drp 11，逐行消费规格表三节），每实体最小可用数据集（行数 ≤ 规格表建议区间，用例指示编码 P / N-TERM / N-DIS），FK 引用零悬空，b2b asn+line / mft_config+mft_log 主子链 + contract 三组主子表（contract+line / contract+version / rebate_agreement+accrual/tier/settlement）+ drp scenario+param/version 与 plan+line 主子链 + dock_appointment→cross_dock 边完整。
- `TestErpSeedDataIntegrity` CSV 基线常量按随批更新协议推进至 **368**（314 + M1.4b 15 + 本批 39），seed-data.md 对账表同步（有 seed 368 / 精确缺 0 = M1 全量覆盖闭环）。
- `docs/design/b2b/seed-data.md` + `docs/design/contract/seed-data.md` + `docs/design/drp/seed-data.md`「种子数据」owner doc 段落地（3 个新文件）。

## Non-Goals

- 不修改任何 ORM 模型（保护区 auto + dual-agent-approval）——本计划纯 CSV-only 路径。
- 不修改任何生产 Java 代码（唯一 Java 触点 = `TestErpSeedDataIntegrity.java` 的 CSV 基线常量按内置协议更新）；不触碰 EDI 交换 / MFT / ASN 匹配 / 合同审批 / 返利累计 / DRP 引擎逻辑（seed 为被动数据，且全部走停用/终态/placeholder 语义）。
- 不覆盖 aps / logistics 及其余域的缺 seed 实体（aps/logistics 归同批 N=1 M1.4b 计划）。
- 不修改既有任何 CSV 文件的任何行（b2b/contract/drp 为零 seed 域，本批纯新增）。
- 不新增三域业财一体 GL 凭证串联 seed（contract rebate accrual 的 AR 发票聚合消费面由既有 C18 fixture 覆盖，seed 不做 GL 回链）。
- 不处置 hr/drp 2 处预存回归（`TestErpDrpCrossDock#testStagingTimeoutFallbackJob` 为模块级测试、classpath 不含本批种子，零交互预期在 Phase 1 复证后登记；修复本身归既有 successor，roadmap Non-Goal）。
- 不新增 M2.x 像素断言、不做负向视觉断言、不做跨浏览器矩阵（roadmap Non-Goal）。
- 不引入 production-grade 真实凭据/真实外部端点/真实个人数据；不做「seed 字段值与 owner doc 业务规则一致性」的逐字段回放测试（roadmap Non-Goal）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/seed-data.md`（规格表 b2b/contract/drp 三节 + 对账表 + 扩展域交易种子 Deferred）、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（M1.5 行 + 横切关注点 1 保护区域）、`docs/testing/e2e-runbook.md`（视觉方法论 + 快照重录协议）、`docs/design/b2b/`（asn-processing / edi-formats / managed-file-transfer / partner-onboarding / state-machine）+ `docs/design/contract/`（approval-workflow / contract-repository / e-signature / volume-discount / state-machine）+ `docs/design/drp/`（cross-dock / lead-time-tracking / safety-stock-optimization / state-machine）
- Skill Selection Basis: roadmap M1.5 行指定 `nop-backend-dev`（数据资产须对齐实体/字典/列命名约定）；Proof 阶段运行测试套件与视觉 spec 属测试域，加载 `nop-testing`；b2b EDI 段 plan-first 独立 plan-audit 按保护区政策执行。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（fresh-DB 由 `./scripts/start-app.sh` 承载；`init-database-data: true` 已默认开启）。
- 本批不引入任何外部服务依赖/端点/密钥：b2b 种子的凭据与端点字段一律 masked placeholder（如 `MASKED-CREDENTIAL-SEED`），test_exchange 为演示测试语义实体。
- 回滚策略：seed CSV 为纯新增文件，回滚 = 删除本批新增文件 + 常量回拨 + git revert 文档变更；不涉及数据迁移。

## Execution Plan

> 执行顺序约束：本计划为同批两计划（N=1/N=2）之二，N=1（M1.4b）先行落地；`EXPECTED_APP_ERP_CSV_COUNT` 与 seed-data.md 对账表为共享触点，本批常量基数以「314 + M1.4b 15」= 329 起算 +39，避免覆盖写。

### Phase 1 - Seed CSV authoring（39 CSV = 13 + 15 + 11）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof | Decision`
- Prereqs: M1.4b（N=1）已落地并更新常量/对账表；**独立 plan-audit（b2b EDI 段）通过并落盘批准记录**（下方首项，b2b EDI 保护区段 CSV 的前置门控）；规格表 b2b/contract/drp 三节必填 FK 锚点全部落在〔本批〕或〔跨域:md·已seed〕ErpMdPartner / ErpMdMaterial / ErpMdWarehouse / ErpMdOrganization

- [x] **独立 plan-audit 门控（b2b EDI 凭据/外部集成段）**：独立子代理（fresh session）按 `docs/context/ai-autonomy-policy.md` 外部集成保护政策核验本计划 b2b 段 seed 设计（masked placeholder 纪律 / N-DIS 停用承载 / 零真实凭据零真实端点 / 零外部调用触发面 / C03 EDI inbound 消费面评估），**批准记录落盘本计划后方可落地 `erp_b2b_partner_credential` / `erp_b2b_edi_format` / `erp_b2b_mft_config` / `erp_b2b_test_exchange` 等 b2b EDI 段 CSV**；非触及行（asn 族 / contract / drp）不受此门控阻塞
      - Skill: none
- [x] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——(a) `ErpB2bAsnMatchPurchaseOrderProcessor` / `ErpB2bAsnRetryMatchProcessor` / `ErpB2bAsnCreateReceiveFromAsnProcessor` per-mutation 事件驱动触发面复证 + 种子 ASN 行终态/relatedBillType 避让设计验证；(b) `ErpCtRebateAgreement__runAccrual` 聚合 partner+period-scoped / accrued 去重 agreement-scoped 复证登记；(c) `DrpDemandAggregator.loadParametersInScope` 仅按 orgId 过滤复证 + C20b `ErpDrpSimulationParamResolver` scenarioId-scoped 复证；(d) `TestErpDrpCrossDock` 模块级 classpath 不含 app-erp-all 种子复证 + `ErpDrpCrossDockStagingTimeoutJob` status=STAGING + updateTime>24h 扫描面复证（种子 cross_dock/dock_appointment 行 N-TERM 终态避让验证）；(e) b2b 两个 dashboard value spec 过滤/标记避让核验（`E2E_` relatedBillType/code 标记族）；(f) `ext-domains-child-table` / `ext-domains-list-filter` no-row→row 路径激活影响评估；(g) b2b/ct/drp 相关 `_cases` 集成用例消费表 grep（**6 用例：C02/C03/C18/C19/C20a/C20b**，C03 含 EDI inbound 查找语义 by-code vs 扫描子核验）+ 快照面评估；(h) 判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`
- [x] 逐实体核对 ORM 表名与列（三域 `model/*.orm.xml` tableName / `code=`）后，按规格表 b2b / contract / drp 三节（39 行，逐行引用不复制）创建 39 个 CSV；b2b 主子链 asn+line、mft_config+mft_log；contract 主子表 3 组（contract+line / contract+version / rebate_agreement+accrual+tier+settlement）；drp 主子表 2 组（scenario+param / scenario+version）+ plan+line 关联链；行数与用例指示编码（P / N-TERM / N-DIS）按规格表逐行执行；N-DIS 行以 `IS_ACTIVE=false` 停用语义词承载，N-TERM 行以终态词承载
      - Skill: `nop-backend-dev`
- [x] Decision: b2b 种子语义安全裁决（经独立 plan-audit iteration 1 勘误修订后执行）——ASN 行全终态（**真终态 = RECEIVED_TO_STOCK / CANCELLED**，audit Minor-4：RECEIVED 为 pre-match 态）+ relatedBillType/code 避开 `E2E_` 标记族与既有 fixture UK（`IT-C19-ASN-001` / `ASN-WEBHOOK-*` / `IT-C03-SO-001` / partnerCode `probe`）；**`erp_b2b_edi_format` 种子 code 禁用 `UBL_ORDER` 及整个 `UBL_*` 族**（audit Major-1：C03 `createInbound` 以 `formatCode="UBL_ORDER"` by-code 解析 format，若命中将翻转 C03 `erp_b2b_edi_doc` 输出快照 `FORMAT_ID` 列）；**`erp_b2b_edi_doc` 种子行禁用 `relatedBillType=SALES_ORDER` + `relatedBillCode=IT-C03-SO-001` 组合**（checkDuplicate UK 面）；EdiDoc/MftLog 行取终态或惰性状态（重试/发送均为 per-id mutation、零批扫描实证）；`ErpB2bPartnerCredential` 与 `ErpB2bPartnerProfile`（audit Major-2 扩展：TRANSPORT_ENDPOINT / WEBHOOK_SECRET / CERT_FINGERPRINT / CONTACT_*）凭据/端点字段一律 masked placeholder + N-DIS 停用；partner_profile 行 status 避开 PRODUCTION + goLiveDate 近期窗口（onboarding-monitor job 唯一读取面，双门控缺省关双保险）；记录选择值、考虑的替代方案与残留风险于本计划执行证据节
      - Skill: `nop-backend-dev`
- [x] Decision: drp 种子组合避让裁决——**载重判据 = `erp_drp_parameter` 行 ORG_ID ≠ "2"**（C20a/C20b fixture orgId，`loadParametersInScope` 仅按 orgId 过滤 + DrpEngine 逐行生成 ErpDrpLine + C20a 断言 lines.size()==2），取 "1" 等 FK 合法组织；(warehouseId, materialId) 组合与 C20a fixture 组合不相交为 belt-and-braces；cross_dock / dock_appointment 行取 N-TERM 终态避开 staging 超时扫描面；记录选择值、考虑的替代方案（如「组合避让为载重 vs ORG_ID 避让为载重」——iteration 1 已裁决后者）与残留风险于本计划
      - Skill: `nop-backend-dev`
- [x] FK 闭环按规格表逐行落实：必填 FK 仅指向〔本批〕新增行或〔跨域:md·已seed〕（ErpMdPartner / ErpMdMaterial / ErpMdWarehouse / ErpMdOrganization）；禁止悬空引用；字典码 ∈ ORM `<dicts>`
      - Skill: `nop-backend-dev`
- [x] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期；列集以 ORM/XMeta 生成的实体列为准（先抽样同族既有 CSV，如 `erp_md_partner.csv` / `erp_inv_stock_move.csv`）；**停用/布尔列型分叉注意（audit Minor-3）**：`ErpB2bEdiFormat.isActive` 为 INTEGER 强制默认 1 → N-DIS 行取 `0`（非 boolean false），`ErpB2bMftConfig` 停用列名为 `ACTIVE` 非 `IS_ACTIVE`，逐列以 ORM domain/stdDataType 为准；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c/M1.2b/M1.4a 先例）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 独立 plan-audit（b2b EDI 段）批准记录落盘本计划，b2b EDI 段 CSV 在批准后落地
- [x] 干扰面预分析结论在案（零漂移预期清单或预判漂移用例清单；b2b 语义安全与 drp ORG_ID 载重避让两项 Decision 裁决记录在案）
- [x] 39 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [x] 无悬空 FK：全部必填 FK 落在〔本批〕∪〔跨域:md·已seed〕集合内

#### Phase 1 执行证据（2026-09-02 执行会话）

**独立 plan-audit（b2b EDI 凭据/外部集成段）记录：**

- **Iteration 1: needs revision → 修订已并入**（独立子代理 fresh session，task id `ses_f9cc1a902ffe2QjEa2DrLj7YU3`）——2 Major + 3 Minor：
  - **Major-1（G5/C03 碰撞向量）**：C03 `TestErpC03O2cGoldenPath` 经 `ErpB2bEdiDoc__createInbound` 传 `formatCode="UBL_ORDER"`（`_cases/.../C03/input/1_edi_inbound.json5:6`），`ErpB2bEdiDocCreateInboundProcessor.findFormatByCode`（:31,70-76）by-code 解析**无 isActive 过滤**，命中则 `doc.setFormatId(...)` 翻转 C03 输出快照 `erp_b2b_edi_doc.csv` 的 `FORMAT_ID` 列（当前录制值为空）→ 修订：`erp_b2b_edi_format` 种子 code 禁用 `UBL_ORDER` 及整个 `UBL_*` 族（规范码 `UBL_DESPATCH_ADVICE`/`UBL_INVOICE` 一并避开，`ErpB2bConstants.java:71-72`）+ `erp_b2b_edi_doc` 种子行禁用 `relatedBillType=SALES_ORDER` + `relatedBillCode=IT-C03-SO-001` 组合（checkDuplicate `ERR_B2B_EDI_DOC_ALREADY_PROCESSED` 面）。已并入上方 Decision 项与保护区域基线注记。
  - **Major-2（masked 纪律范围）**：`ErpB2bPartnerProfile` 携带 `transportEndpoint`（ORM:373）/ `webhookSecret`（:375）/ `certFingerprint`（:377）+ contact PII 列（:380-382），为 partner_credential/test_exchange 的批内 FK 锚点——修订：masked placeholder 纪律显式扩展至该实体四类列（已并入保护区域基线注记 + Decision 项）。
  - Minor-3（列型分叉）：`ErpB2bEdiFormat.isActive` INTEGER 强制默认 1 → N-DIS 行取 `0`；`ErpB2bMftConfig` 停用列名 `ACTIVE`——已并入列对齐项。
  - Minor-4（真终态勘误）：ASN `RECEIVED` 为 pre-match 态，真终态 = `RECEIVED_TO_STOCK` / `CANCELLED`——已并入 Decision 项。
  - Minor-5（措辞勘误）：`matchPurchaseOrder` 无 relatedBillType 过滤（仅 `ErpPurOrder.code` eq + 状态机 RECEIVED 前置，per-mutation by `asnId`）——结论（安全）a fortiori 成立，预分析 (a) 已按实仓语义登记。
- **Iteration 2（修订复核）: APPROVE**（独立子代理 fresh session，task id `ses_f9ca92a2cffewOsIFyxst6UPqg`，与 iteration 1/执行者互不共享上下文）——Iteration 1 全部 5 项修订逐点核验 INCORPORATED（UBL_*/C03 碰撞向量 / partner_profile masked 扩展 / 列型分叉 / ASN 真终态 / 无 relatedBillType 过滤均引实仓代码逐位吻合）；G1–G5 门控保证评估全 PASS（执行设计如文档所述可保证零真实凭据/端点、N-DIS 停用承载、零外部调用触发、零 C03 干扰）；2 Minor 非阻塞行政项（本次追加记录即落实项 1；项 2 = 基线 b2b 干扰面措辞刷新，已随本记录同步修正）。**b2b EDI 段 CSV 落地门控就此解锁。**

**干扰面预分析 (a)–(h) 逐点实仓复证结论（三路独立子代理并行核验，task id `ses_f9cc12f4fffeTjGjXzuWU5KfGT` / `ses_f9cc0c2e8ffelDg7SOH5rSVzwH` / `ses_f9cc05e33ffeTVtrr0sCbHYNnb`）：**

- **(a) b2b ASN 三 Processor 事件驱动复证**：`ErpB2bAsnBizModel` 三入口（matchPurchaseOrder L76-80 / createReceiveFromAsn L82-86 / retryMatch L88-92）全部 `@BizMutation` by 显式 `asnId`（`ErpB2bAsnMatchPurchaseOrderProcessor.java:121-128` getEntityById 起步），种子行永不被枚举；`matchPurchaseOrder` 查询面 = `ErpPurOrder.code eq asn.relatedBillCode`（:134-138）**无 relatedBillType 过滤**（草案措辞勘误见 audit Minor-5），状态门控为状态机前置（RECEIVED→MATCHED）。CSV 直插不触发 mutation → 零触发面。域内唯一 b2b job `erp-b2b-onboarding-monitor.job.yaml` 双门控缺省关（yaml `enabled|false` + bean cron 空 skip，`ErpB2bOnboardingMonitorJob.java:106-111`）。
- **(b) `ErpCtRebateAgreement__runAccrual` 聚合 scope 复证**：`ErpCtRebateAgreementRunAccrualProcessor.runAccrual(agreementId, ...)`（:48-58）按**显式单一 agreementId** 装载，聚合过滤 = posted=true + businessDate ∈ [agreement.startDate, asOfDate] + customerId/supplierId = agreement.partnerId（:106-118），accrued 去重 per-agreement（`loadAccruedBillCodes` eq rebateAgreementId，:92-103）；**无任何全表迭代 runAccrual 的调用方** → 种子 agreement 行（status=DRAFT/SETTLED）与 C18 零交互（C18 全部断言 by runtime id / per-agreementId 过滤）。C18 fixture：CUSTOMER_ID="1"、agreement partnerId="1" ACTIVE、窗口 [2026-07-07, 2026-12-31]、冻结时钟 2026-07-17——种子避让 belt-and-braces：partnerId 取 2/4、status 避开 ACTIVE。
- **(c) drp 载重判据复证**：`DrpDemandAggregator.loadParametersInScope`（:167-173）**仅按 plan.orgId eq 过滤**（plan.orgId 为 null 时零过滤——种子 ORG_ID 禁 null），`DrpEngine`（:82-116）对每条 in-scope parameter **无条件**生成 `ErpDrpLine`（net=0 clamp 不跳过）挂到**本 planId**；C20a fixture plan/param orgId 全 "2" + 断言 `lines.size()==2`（TestErpC20a:131）与 `totalReplenishmentQty==140`（:150）→ **种子 `erp_drp_parameter` 行 ORG_ID 禁取 "2" 且禁 null，取 "1"**（GROUP-HQ，org CSV 仅 1/2 两行，"1" 为唯一 FK 合法替代）；(warehouseId, materialId) 组合避让 C20a fixture 组合 (2,1)/(1,2) 及 C20b/E2E 常用 (1,4)/(2,4) 为 belt-and-braces。C20b `ErpDrpSimulationParamResolver.loadParams` scenarioId-scoped（:75-95，scenarioId 键缓存 + `invalidateCache()` 测试内失效）→ 种子 scenario/param 行零泄漏。
- **(d) `TestErpDrpCrossDock` classpath + job 扫描面复证**：该测试位于 `module-drp/erp-drp-service/src/test`（:246），`erp-drp-service/pom.xml` 不依赖 app-erp-all（仅域内模块 + nop 库）、测试资源无 `_init-data` → 种子对其不可见，**零交互判定成立**。`ErpDrpCrossDockStagingTimeoutJob`（:105-118）扫描面 = `status=STAGING` AND `updateTime > 24h 前`（limit 200）→ 种子 cross_dock 行取终态（CANCELLED/COMPLETED）+ dock_appointment 静态过期日期双保险不可见；dock_appointment 无任何扫描器（唯一 drp job 即上者）。job yaml `erp-drp-xdock-staging-timeout.job.yaml` enabled 缺省 false。
- **(e) b2b 两个 dashboard value spec 过滤/标记避让核验**：`b2b-asn-flow.value.spec.ts`（:22-23 `E2E-ASN-FLOW-${Date.now()}` + relatedBillType `'E2E_FLOW'`）与 `b2b-edi-detail.value.spec.ts`（:22 `E2E-EDI-DETAIL-${Date.now()}` + `'E2E_RBT_DETAIL'`）均自包含 setup + get-by-id + 客户端 asnId/ediDocId 过滤，`total` 选中不断言；无 relatedBillType IN 服务器过滤（草案前提勘误：隔离靠唯一 code + 客户端 id find）——种子行相关BillType/code 避开 `E2E_` 族即零串扰；唯一体量约束 = 种子 asn_line / edi_log 行数须远小于 findPage(limit:5000) 首页（≤20/表天然满足）。
- **(f) `ext-domains-child-table` / `ext-domains-list-filter` no-row→row 激活评估**：child-table spec（:72-78）对 `/ErpB2bAsn-main`（1 sub-grid）/ `/ErpCtContract-main`（2 sub-grids，row-update-button）/ `/ErpDrpPlan-main`（view drawer lines sub-grid-view）现为 no-row skip 分支，本批种子后三视图激活真实行路径——断言为 DOM 结构性（drawer 内 `.cxd-InputTable` ≥ 1），无数值行断言，预期通过（Phase 3 实跑核验）；`f12-page-structure.visual.spec.ts` 的 `/ErpCtContract-main` Tier D 段（7 tabs + InputTable）同样 skip→激活（结构断言）。list-filter spec 对 `/ErpB2bEdiDoc-main` asideFilter（标签组断言 + `toBeGreaterThan(0)` 文本断言——加行只增文本不减）与 `/ErpB2bEdiLog-main` readonly（精确匹配写按钮缺失——生成 readonly 页与行数无关）行数免疫。
- **(g) `_cases` 消费表 grep 复证（6 用例全集确认 + C03 子核验）**：`app-erp-all/_cases` grep `ErpB2b|ErpCt|ErpDrp` 实际命中 **C02 / C03 / C18 / C19 / C20a / C20b** 六用例（b2b 面 = C03 + C19；ct 面 = C02 + C18；drp 面 = C20a + C20b；六用例均无 findPage/totalCount 种子计数断言）。C03 **不按 code 查找** edi_doc（消费 createInbound 响应 id/code，:103-106），output 快照为 `_chgType` 变更行增量（恰 1 条 A 行，ID=100000），种子行属 before-image 不入变更集 → 唯一漂移向量 = Major-1 的 by-code format 解析（已裁决避让）；C19 全 by-id（own asnId），唯一硬约束 = code 不撞 `IT-C19-ASN-001`；C02 自包含 `ErpCtContract__save`（javadoc :40-41 自证无 contract seed 前提），output `erp_ct_contract.csv` 恰 1 条 A 行 ID=100000——**ID < 100000 纪律 load-bearing**（`zz-sequence-advance.sql` 钉 default 序列 NEXT_VALUE=100000）；C18 全 by-id + per-agreementId。快照面：六用例 input/tables 为插入源非比对面（`setTableInit(false)`），纯加性种子不入既有快照（M1.2b 先例复证）。
- **(h) 判定**：**零漂移预期**（无预判漂移用例）——前提 = 执行本节 (a)-(g) 全部避让裁决：b2b（UBL_* 族 / E2E_ 族 / fixture UK / 真终态 / masked placeholder / partner_profile 非 PRODUCTION）+ ct（ID<100000 / agreement 避 ACTIVE+partner 1/3 / signature 字符串态码 / invoice_plan planDate 静态 2027 不落 due 窗口）+ drp（parameter ORG_ID="1" + 组合避让 / cross_dock 终态 / 全批 ID<100000 / UK 不撞）。视觉激活面 (f) 为 skip→激活过渡（结构断言），Phase 3 实跑核验。

**CSV 落地记录（2026-09-02，门控批准后 authoring）**：39 CSV 全部经 schema 驱动脚本生成（列头直接取自 ORM `code=`，零转录），共 **81 行**。程序化验证三连全绿：**FK 闭环 166 边全解析**（〔本批〕∪〔跨域:md·已seed〕，含 dock_appointment→cross_dock 边、signature_request→contract_version 边、drp_line→plan/material/warehouse 13 边、approval_record→contract+matrix 6 边）、**列头 39/39 与 ORM `code=` 全等**、ID < 100000 / 行数 > 0 / DATE 与 TIMESTAMP / boolean 与 INTEGER 列型逐值校验通过。

**Decision 裁决记录（b2b 语义安全）**：

| 裁决点 | 选择值 | 考虑的替代方案 | 残留风险 |
|---|---|---|---|
| ASN 状态承载 | 全终态 `RECEIVED_TO_STOCK`（P）+ `CANCELLED`（N-TERM） | RECEIVED（pre-match 态）——放弃：audit Minor-4 勘误后非真终态 | 无（零扫描面 + 零枚举面双实证） |
| ASN relatedBillType/Code | `PO_ORDER`（语义正确值）+ `ASN-REF-SEED-00x`（不命中任何 PO code） | 改用非 PO 语义值——放弃：实证 matchPurchaseOrder 无 relatedBillType 过滤，PO_ORDER 保演示语义且 relatedBillCode 不命中已双保险 | 若未来出现按 relatedBillCode 扫描 ASN 的新批处理器需复评（当前零命中） |
| edi_format code | `EDIFACT_ORDERS` / `X12_850`（避开 `UBL_*` 族） | `UBL_DESPATCH_ADVICE` 等规范码——放弃：audit Major-1（C03 by-code 碰撞向量） | 无（C03 `UBL_ORDER` 恒 miss） |
| edi_doc relatedBill 组合 | `ASN`/`SHIPMENT` 类型 + 本批/既有种子 code；**禁 `SALES_ORDER`+`IT-C03-SO-001`** | 允许 SALES_ORDER 类型——放弃：checkDuplicate UK 面无必要暴露 | 无 |
| EdiDoc/MftLog 状态 | edi_doc `ARCHIVED`/`RECEIVED`；mft_log `SENT`/`DEAD_LETTER`（真终态） | `TO_SEND`/`FAILED`——放弃：终态/死信为最惰性承载（重试/发送均 per-id mutation，零批扫描） | 无 |
| 凭据/端点字段 | `MASKED-CREDENTIAL-SEED` + `masked-credential-seed.example` 伪域（partner_credential / partner_profile TRANSPORT_ENDPOINT/WEBHOOK_SECRET/CERT_FINGERPRINT/CONTACT_* / mft_config TRANSPORT_ENDPOINT / mft_certificate 证书四列） | 留空——放弃：masked placeholder 保演示形态且审计可 grep 辨识 | 无（零真实凭据零真实端点，audit G1/G3 PASS） |
| partner_profile status | `TESTING`（P）+ `TERMINATED`（N-TERM），goLiveDate 静态 2026-05/06 | `PRODUCTION`——放弃：onboarding-monitor job 唯一读取面（双门控缺省关）的过滤键，非 PRODUCTION 双保险 | 无 |
| N-DIS 停用承载 | partner_credential / mft_certificate `IS_ACTIVE=false`（BOOLEAN）；edi_format `IS_ACTIVE=0`（INTEGER 强制默认 1，audit Minor-3 列型分叉）；edi_format 第二行 N-DIS / X12_850 恰为 partner_profile TERMINATED 行 ALLOWED_FORMATS，语义互洽 | 全批回避 N-DIS——放弃：规格表 P+N-DIS 指示需承载 | 无 |

**Decision 裁决记录（drp ORG_ID 载重避让）**：

| 裁决点 | 选择值 | 考虑的替代方案 | 残留风险 |
|---|---|---|---|
| `erp_drp_parameter.ORG_ID` | **两行均取 "1"**（GROUP-HQ；org CSV 仅 1/2 两行，"1" 为唯一 FK 合法替代） | (warehouseId, materialId) 组合避让为载重——放弃：iteration 1 草案审查已裁决 ORG_ID 为载重判据（`loadParametersInScope` 仅按 orgId 过滤 + DrpEngine 逐行生线 + C20a 断言 lines==2/total==140），组合避让不构成保护 | 无（org="1" 对 C20a/C20b 及 5 个 drp e2e spec（全 orgId="2"）恒不可见；ORG_ID 禁 null 同样满足——null 时零过滤） |
| parameter (warehouseId, materialId) 组合 | (wh1,mat1) / (wh2,mat3) | 与 C20a fixture (2,1)/(1,2) 及 C20b/E2E 常用 (1,4)/(2,4) 全不相交——belt-and-braces | 无 |
| cross_dock / dock_appointment 状态 | cross_dock `COMPLETED`/`CANCELLED`（避 `STAGING`）；dock_appointment 自由串 `COMPLETED`/`CANCELLED` + 静态过期日期 | 留 STAGING 态——放弃：`ErpDrpCrossDockStagingTimeoutJob` 扫描面 = status=STAGING + updateTime>24h，终态+旧日期双保险不可见 | 无（job enabled 缺省 false + 模块级测试 classpath 不含本批种子） |
| plan/line/scenario 组织 | plan/line/scenario 等输出侧表 orgId="2"（常规演示口径） | 全域 org="1"——放弃：载重判据仅在 parameter 表（引擎唯一输入面），输出侧按 planId/scenarioId 维度隔离已充分 | 无（C20a/C20b 断言全部 planId/场景 id 过滤） |

**预存 ORM quirk 登记（非本批引入，CSV-only 规避完成，模型修正归 successor）**：① `erp-ct/sign-status` / `erp-ct/sign-provider` 字典 option 值为数字串（'10'~'60'/'10'~'99'），而运行时常量（`ErpCtConstants:49-57`）写入字符串码 `PENDING_SIGNATURE`/`REJECTED`/`MOCK`——种子按运行时词汇取值（状态机/查询/UI 过滤的实际消费面），字典与运行时失配为预存 ORM 字典陈旧，登记待 successor 修正；② `erp-drp/simulation-status` / `erp-drp/simulation-param-type` 字典被 ORM 列引用但未在任何 ORM `<dicts>` 定义——种子取 `ErpDrpConstants`（:84-92）运行时词汇 `DRAFT`/`ARCHIVED`/`SAFETY_STOCK`/`LEAD_TIME`，零校验影响；③ `erp_b2b_edi_doc.BLOCKING_LEVEL` 强制列 default="10" 而字典 `erp-b2b/blocking-level` 值为 INFO/WARN/ERROR——种子显式取 `INFO` 归位字典域。

### Phase 2 - 门禁常量 + owner doc 同步

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/b2b/seed-data.md`、`docs/design/contract/seed-data.md`、`docs/design/drp/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：基数以「314 + M1.4b 15」= 329 起算本批 +39 → **368**，`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`
      - Skill: `nop-backend-dev`
- [x] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed +39，精确缺 seed → **0**）+ 新增 M1.5 批次增量行 + 「其他扩展域交易种子」Deferred 之 b2b/contract/drp 子集消费注记（该 Deferred 全部子集清零登记）+ 快照重录义务节资产计数注记（以对账表批次链为准）+ M1 里程碑全量覆盖闭环状态注记
      - Skill: `nop-backend-dev`
- [x] 新建 `docs/design/b2b/seed-data.md` + `docs/design/contract/seed-data.md` + `docs/design/drp/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔本批〕/〔跨域:md〕标注）、用例指示编码、negative 行语义（N-TERM 终态 / N-DIS 禁用）、干扰面零漂移设计节（含 b2b 语义安全与 drp ORG_ID 载重避让两项 Decision 裁决 + masked placeholder 纪律）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 常量（368）与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [x] 3 个域 seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: completed
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2 + 独立 plan-audit（b2b EDI 段）通过

- [x] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [x] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移，按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏（C02/C03/C18/C19/C20a/C20b 优先排查）→ 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [x] 视觉快照双面义务核查：运行 `ext-domains-child-table` + `ext-domains-list-filter`（本批激活真实行路径的两 spec）+ `dashboards.visual` + `dashboards.snapshot` + `reports.visual` + `reports.snapshot`；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段；禁止单面重录
      - Skill: `nop-testing`
- [x] E2E 数值断言联动评估：`b2b-asn-flow.value` + `b2b-edi-detail.value`（过滤非串扰实证）+ b2b 3 个 action spec + ct 5 个 action spec + drp 5 个 action spec + `b2b.smoke` + `contract.smoke` + `drp.smoke` + Phase 1 预分析标记的消费面——有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [x] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 8 张本批表（b2b ≥ 3 含 asn 主子表头 + contract ≥ 3 含 contract/rebate 主子表头 + drp ≥ 2 含 plan/scenario 头）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [x] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [x] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目
      - Skill: none
- [x] 独立结束审计通过后回写 roadmap 工作项 M1.5 `ready` → `done`（含批次证据摘要 + M1 里程碑闭环注记，格式沿 M1.1/M1.2/M1.4a 批先例）——已完成（结束审计 APPROVE `ses_f9c6bb3ecffe6Z4R2ScokLKShB` 后回写）
      - Skill: none

Exit Criteria:

- [x] TestErpSeedDataIntegrity 全绿且常量断言通过
- [x] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [x] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [x] compliance checker 零漂移；日志条目在位；roadmap 状态回写完成

#### Phase 3 执行证据（2026-09-03）

1. **门禁**：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` **4/4 全绿**（scope-pinning 363 + CSV 常量 **368** = 314 + M1.4b 15 + M1.5 39 + 零孤儿 CSV + 引用完整性零悬空白名单零增量）。
2. **模块回归**：`mvn test -pl app-erp-all` **70/0/0/1 = M1.4b 基线精确一致**（−1 vs M0.2 口径 = 兄弟 flux-picker 计划删除的 `ErpAllFluxPagesExportTest`，沿 M1.4b 已归因；**C02/C03/C18/C19/C20a/C20b 六消费用例全绿，零集成快照漂移**，快照重录双面义务未触发，与 Phase 1 预分析 (h) 零漂移判定对账一致）。
3. **视觉快照双面义务核查**：canonical test-mode webServer（playwright config 内嵌全 `-D` flag，flux 渲染，fresh-DB）下 **50/50 全绿**——`dashboards.visual` 10 + `dashboards.snapshot` 10 + `reports.visual` 24 + `reports.snapshot` 6 零 DOM/像素漂移（**快照重录义务未触发**，无双面重录，无「快照重录合规声明」触发面）；`ext-domains-list-filter` 全绿——`/ErpB2bEdiDoc-main` asideFilter + `/ErpB2bEdiLog-main` readonly 真实行路径激活且断言语义不变（行数免疫预分析 (f) 实证成立）。`ext-domains-child-table` **5 失败 = 与 M1.4b 基线 IDENTICAL FAILURE SETS**（logistics/b2b/contract/hr/drp 五入口，全部 `page.waitForSelector('.cxd-Crud')` 20s 超时同签名）——M1.4b 已四重归因为 AMIS 遗留选择器预存红灯（错误快照中页面行/分页渲染齐全；`.cxd-*` 为 AMIS 类名而 flux 输出 `nop-crud`/`nop-table`），已登记 `docs/bugs/2026-09-02-ext-domains-child-table-amis-legacy-selector-flux-preexisting-red.md` 归 flux 迁移/e2e 基建 owner 域 Follow-up，**非本批引入、非 seed 漂移**（本批三域行激活不改变该 spec 失败集——等待步先于 no-row skip 分支，零种子时同败）。
4. **E2E 数值断言联动评估：36 spec = 33 绿 + 3 预存失败，零期望值基线调整**：
   - b2b **13/13 全绿**：`b2b-asn-flow.value` + `b2b-edi-detail.value`（种子行非串扰实证——spec 隔离靠唯一 `E2E-*` code + 客户端 id find，种子行零污染）+ `b2b-asn-match-receive` / `b2b-asn-line-level-receive-fill` / `b2b-edi-doc` 3 action（edi-doc inbound 用 `NONEXISTENT-${Date.now()}` formatCode 对种子 edi_format 恒 miss）+ `b2b.smoke`。
   - ct **13 绿 + 2 预存失败**：`ct-contract-version` / `ct-invoice-plan-trigger` / `ct-rebate-accrual`（runAccrual 引擎对自有 agreement）/ `ct-rebate-settlement` / `contract.smoke` / `ct-contract-lifecycle` expire+amend 路径全绿；`ct-contract-lifecycle` happy path + illegal guards 2 失败 = `docs/bugs/2026-08-23-ct-terminate-approval-fnpt-deadlock.md` **已登记确定性预存红灯**（enforcement 栈下 `role-ct-approver` 被 action-auth 拒 `nop.err.auth.no-permission`「没有访问权限」，实测错误逐位吻合；身份守卫 × FNPT 声明缺口死锁，产品缺陷归 successor）。
   - drp **10 绿 + 1 预存失败**：`drp-plan-engine`（planId 过滤容忍种子行，ORG_ID="1" 载重避让实证）/ `drp-release-line` / `drp-simulation` / `drp-safety-stock`（种子 (wh,mat) 组合避让实证）/ `drp.smoke` 全绿；`drp-release-approved` happy path 1 失败 = `docs/bugs/2026-08-23-drp-release-approved-double-advance.md` **已登记确定性预存红灯**（`releaseApproved` 循环内 `releaseLine` 已推进 plan→EXECUTED，循环后冗余二次 `advancePlanToExecutedIfComplete` 断言 APPROVED 抛 `非法状态转换：当前=EXECUTED，期望=APPROVED`，实测错误逐位吻合；产品代码缺陷归 successor）。
   - 三处失败与 Phase 1 预分析「零漂移」判定不冲突——预分析面为 seed 值引发的行为面；三处均为与种子零因果的产品态/spec 态缺陷（bug 记录早于本批，复现命令确定性红）。
5. **运行时装载证明**：`./scripts/start-app.sh restart`（fresh-DB）14s ready、装载零冲突零列映射错误；GraphQL `/r/{Entity}__findPage` 抽样 **14/14 表 total==CSV 行数**（b2b 6：asn 2 / asn_line 3 / partner_profile 2 / edi_doc 2 / edi_format 2 / partner_credential 2；contract 4：contract 2 / contract_line 3 / rebate_agreement 2 / template 2；drp 4：plan 2 / drp_line 3 / parameter 2 / scenario 2——asn 主子表头 + contract/rebate 主子表头 + plan/scenario 头全在样，超 ≥8 表门控要求）。
6. **compliance checker**：**exit 0 零新增漂移**（R2c=1542 = 机器块 1537 + 已登记 pre-existing +5 / R2b=242 = 240+2 / R12a=71 = 70+1 / R12b=66 / R12c=42，纯资源 + 测试常量 + docs 变更预期成立）。
7. **日志条目**：`docs/logs/2026/09-03.md` 已追加（M1.5 执行条目，实际执行日）。
8. roadmap 回写待独立结束审计通过后执行（见下项）。

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 fresh session `ses_f9ee75cfbffedlH7FKiAjsyRuF`）——1 Blocker + 3 Major + 2 Minor。Blocker：drp 种子避让判据轴错误——`loadParametersInScope` 仅按 orgId 过滤 + DrpEngine 逐行生成 ErpDrpLine + C20a/C20b fixture orgId="2" + C20a 断言 lines.size()==2，组合避让不构成保护，ORG_ID ≠ "2" 才是载重判据——已重写基线 + Decision (d) + 预分析 (c) 扩展 C20b ParamResolver；Major m2：C19 `E2E_` 标记归属勘误（C19 实为直建 RECEIVED + 真实 billType + by-id，标记族属 dashboard value specs）；Major m3：`_cases` 枚举漏 C02/C03（6 用例全集 + C03 EDI inbound by-code 子核验）；Major m4：plan-audit 门控结构性挂接 Phase 1（Prereqs + 首项门控 b2b EDI 段 CSV + Exit Criteria）；Minor m5：ct action spec 计数 6→5；Minor m6：C18 runAccrual scope 预记录（partner+period-scoped + agreement-scoped 去重）。全部已并入本稿。
- Independent draft review iteration 2: acceptable as-is（独立子代理 fresh session `ses_f9ed8d304ffeai2c7xNG7lHcBH`）——iteration 1 全部 6 项修订核验通过（ORG_ID 判据/C19 归属/6 用例/门控挂接/计数/C18 scope 逐点实仓吻合），无新引入缺陷；1 Minor 行政项 = Draft Review Record 待回填（本条即回填）。共识达成，转 `active`。

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [x] 范围内行为完成（39 CSV + 常量 + 对账表 + 3 个 owner doc 段）
- [x] 相关文档对齐（seed-data.md 对账表/快照重录注记/扩展域交易种子 Deferred 清零注记 + 3 个域 seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [x] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] **b2b EDI 凭据/外部集成段独立 plan-audit 已完成并记录（批准记录落盘）**
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- **seed-data.md §Non-Goals「其他扩展域交易种子（logistics/b2b/contract/drp/aps 后续批次）」Deferred 之 b2b + contract + drp 子集**：本计划 Phase 1 全量覆盖口径消费；消费后该 Deferred 全部子集（aps/logistics 归 M1.4b + b2b/contract/drp 归本计划）清零，消费注记由 Phase 2 回写 seed-data.md。
  - Classification: `consumed by this plan`（b2b + contract + drp 交易种子子集）
  - Why Not Blocking Closure: 非阻塞项——为消费登记而非遗留债务；该 Deferred 消费完成后无残留。
  - Successor Required: `no`
- **`TestErpDrpCrossDock#testStagingTimeoutFallbackJob` 预存回归**：模块级测试（`module-drp/erp-drp-service`），classpath 不含 app-erp-all `_init-data` 种子，本批种子零交互预期（Phase 1 复证后登记）；修复归既有 successor（roadmap Non-Goal 明示），不因本批种子落地而重新归属。
  - Classification: `watch-only residual`
  - Why Not Blocking Closure: 既有 successor 义务 + 零交互复证在案；本计划不变更其修复归属。
  - Successor Required: `yes`（归既有 hr/drp 预存回归 successor，非本计划新增）

（其余待执行期裁定：若规格表某行证伪 CSV-only 可满足性，按反松弛规则移入本节分类登记）

## Closure

Status Note: 本计划可闭包——39 个 seed CSV（b2b 13 + contract 15 + drp 11，81 行）按规格表逐行落地，b2b EDI 凭据/外部集成段先经独立 plan-audit 双迭代（iteration 1 NEEDS REVISION 2 Major + 3 Minor 全部修订并入 → iteration 2 APPROVE，批准记录落盘 Phase 1 执行证据节）后落地；脚本化门禁（列头 39/39 与 ORM `code=` 全等 + FK 闭环 166 边零悬空 + ID<100000/列型逐值校验）+ `TestErpSeedDataIntegrity` 4/4 全绿（常量 329→368）+ fresh-DB 运行时装载 GraphQL findPage 14/14 表行数实证（0 冲突/0 列映射错误）；`mvn test -pl app-erp-all` 70/0/0/1 = M1.4b 基线精确一致（六消费用例 C02/C03/C18/C19/C20a/C20b 全绿零快照漂移，重录义务未触发）；canonical 视觉双面 50/50 零 DOM/像素漂移 + ext-domains-list-filter 真实行路径通过（child-table 5 失败 = 与 M1.4b 基线 IDENTICAL FAILURE SETS 的已登记 AMIS 遗留选择器预存红灯，非本批引入）；E2E 联动 36 spec = 33 绿零期望值调整（3 失败均为早于本批的已登记确定性预存红灯：`2026-08-23-ct-terminate-approval-fnpt-deadlock` ×2 + `2026-08-23-drp-release-approved-double-advance` ×1，错误签名逐位吻合归 successor）；compliance checker exit 0 零新增漂移（R2c=1542/R2b=242/R12a=71 = 机器块 + 已登记增量）；门禁常量与对账表（有 seed 368 / 精确缺 0 = **M1 全量覆盖闭环，扩展域交易种子 Deferred 全子集清零**）与 `_init-data/` 实数（372 CSV + 1 SQL）三方一致；3 个域 owner doc 落地且与实仓 CSV 逐行吻合（结束审计抽核通过）。独立结束审计 APPROVE（0 Blocker / 0 Major / 2 Minor 文档精度项，均已随闭包落实）。roadmap M1.5 已回写 `done`（含 M1 闭环注记）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，task id `ses_f9c6bb3ecffe6Z4R2ScokLKShB`）
- Evidence: A..K 十一项实仓核验全 PASS（plan 文本一致性 + 门控批准记录先于 gated CSV 落地；39 CSV 存在性/规格区间/用例编码/ID 上限/masked placeholder 全批扫描；39 表列头 ↔ 19 个 ORM 文件 368 实体脚本化全等；FK 闭环 166 边零悬空脚本化复算（ORG_ID=["1","1"]、cross_dock 无 STAGING）；门禁常量 368/4/363 与 `_init-data/` 372 CSV + 1 SQL 构成零孤儿；门禁测试独立复跑 4/4 全绿；compliance checker 独立复跑 exit 0 R2c=1542/R2b=242/R12a=71；3 owner doc 逐域 ≥4 表抽核吻合；对账表/日志/Deferred 清零注记在位；3 个 E2E 失败归因 bug 记录逐一验证存在且早于执行日 + C20a spot-run 1/1 绿（ORG_ID 避让运行时实证）；roadmap 前置状态 `ready` 正确）；2 Minor：Minor-1（无 REMARK/NOTES 列实体用例编码承载措辞——已落实 3 处文档软化）+ Minor-2（ct/drp owner doc FK 边数勘误 58→54 / 62→66——已落实）。

Follow-up:

- `docs/bugs/2026-08-23-ct-terminate-approval-fnpt-deadlock.md`（open，successor = 产品 FNPT 声明补齐）与 `docs/bugs/2026-08-23-drp-release-approved-double-advance.md`（open，successor = 产品代码幂等短路/删冗余调用）——两处预存红灯在 E2E 联动评估中再次确证（错误签名逐位吻合），修复归属不变（plan 2026-08-23-0434-2 Non-Goal），非本批新增债务。
- `docs/bugs/2026-09-02-ext-domains-child-table-amis-legacy-selector-flux-preexisting-red.md`——AMIS 遗留 spec 选择器 flux 化迁移，触发条件 = flux 迁移/e2e 基建 owner 域下一批次；本批 b2b/ct/drp 种子激活其三入口真实行路径但失败集与 M1.4b 基线 IDENTICAL（等待步先于 skip 分支），归 flux 迁移 owner 域。
- 3 处预存 ORM 字典 quirk（sign-status/sign-provider 字典数字码 vs 运行时字符串码、erp-drp simulation 双字典未定义、erp_b2b_edi_doc.BLOCKING_LEVEL default=10 越字典域）——本批 CSV-only 规避并登记于 plan Phase 1 证据节 + 3 个 owner doc；模型/字典修正归 ORM owner successor（保护区，非本计划范围）。
