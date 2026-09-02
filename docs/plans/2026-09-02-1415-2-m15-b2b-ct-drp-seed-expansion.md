# 2026-09-02-1415-2 M1.5 第二批扩展域 seed 扩面——b2b + contract + drp（39 CSV）

> Plan Status: active
> Last Reviewed: 2026-09-02
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
  - **b2b 域**：app-erp-all 集成用例 `TestErpC19B2bAsnAutoReceiveLandedCost`（C19）——fixture 直建 `status=RECEIVED` 的 ASN + **真实** relatedBillType（PO_ORDER / PURCHASE_RECEIPT）+ `matchPurchaseOrder` by-id（草案审查 iteration 1 勘误：`E2E_` 标记族属 dashboard value specs 专用，C19 无 E2E 标记）；种子 ASN 行须**全终态（RECEIVED 等 N-TERM 承载）且 relatedBillType 避开 PO 匹配查询过滤面与 `E2E_` 标记族**；`ErpB2bAsnMatchPurchaseOrderProcessor` / `ErpB2bAsnRetryMatchProcessor` / `ErpB2bAsnCreateReceiveFromAsnProcessor` 均为 per-mutation 事件驱动 Processor（草案审查 iteration 1 实证，Phase 1 复证不被 CSV 直插触发）；E2E 消费面 `tests/e2e/dashboards/b2b-asn-flow.value.spec.ts` + `b2b-edi-detail.value.spec.ts`——自包含 setup（`E2E-` 前缀 code + get-by-id + 客户端 asnId/ediDocId 过滤 + 清理），种子行仅增大 findPage 全集（无害），但种子 relatedBillType/code 禁用 `E2E_` 标记族；`tests/e2e/business-actions/` b2b 3 个 action spec（asn-match-receive / asn-line-level-receive-fill / edi-doc）+ `b2b.smoke.spec.ts`。
  - **contract 域**：app-erp-all 集成用例 `TestErpC18CtLifecycleRebate`（C18）——fixture 自建 contract/version/rebate agreement，`ErpCtRebateAgreement__runAccrual`（PERIOD_END 聚合**已过账 AR 发票**）——草案审查 iteration 1 实证聚合为 partner+period-scoped（filter supplierId/customerId + 窗口）、accrued 去重 agreement-scoped，种子 agreement 行不被 runAccrual 调用 → 零漂移可达（Phase 1 复证并登记）；`tests/e2e/business-actions/` ct 5 个 action spec（contract-lifecycle / contract-version / invoice-plan-trigger / rebate-accrual / rebate-settlement）+ `contract.smoke.spec.ts`。
  - **drp 域**：app-erp-all 集成用例 `TestErpC20aDrpNetRequirementRelease`（C20a，`ErpDrpParameter__save` fixture ×2 + `ErpDrpPlan__runDrp`）+ `TestErpC20bDrpSimulationPromote`（C20b）——**载重避让判据 = ORG_ID**（草案审查 iteration 1 实仓核出）：`DrpDemandAggregator.loadParametersInScope` 仅按 orgId 过滤（plan.orgId 为 null 时零过滤），`DrpEngine` 为每条 in-scope parameter 行生成 `ErpDrpLine`（无零净额跳过），C20a/C20b fixture 全部 `orgId="2"` 且 C20a 断言 `lines.size()==2`——**种子 `erp_drp_parameter` 行 ORG_ID 禁取 "2"**（取 "1" 等 `erp_md_organization.csv` FK 合法组织），(warehouseId, materialId) 组合避让为 belt-and-braces；C20b `ErpDrpSimulationParamResolver` 为 scenarioId-scoped（缓存测试内失效）预期零交互（Phase 1 复证登记）；预存回归 `TestErpDrpCrossDock#testStagingTimeoutFallbackJob` 位于 `module-drp/erp-drp-service`（模块级测试，classpath 不含 app-erp-all `_init-data` 种子，预期零交互）——Phase 1 复证该 classpath 判定 + `ErpDrpCrossDockStagingTimeoutJob` 行扫描过滤面（status=STAGING + updateTime > 24h 窗口，草案审查 iteration 1 实证；种子 cross_dock / dock_appointment 行取 N-TERM 终态避让）；`tests/e2e/business-actions/` drp 5 个 action spec + `drp.smoke.spec.ts`。
  - **跨域 E2E 视觉消费面**：`tests/e2e/visual/ext-domains-child-table.visual.spec.ts` 覆盖 `ErpB2bAsn-main`（1 sub-grid）/ `ErpCtContract-main`（2 sub-grids）/ `ErpDrpPlan-main`（view drawer lines sub-grid-view）——现依赖 no-row 分支，本批种子落地后激活真实行路径（结构断言预期仍通过，Phase 3 实跑核验）；`ext-domains-list-filter.visual.spec.ts` 覆盖 `ErpB2bEdiDoc-main` asideFilter（formatId + state in）+ `ErpB2bEdiLog-main` readonly。
  - **跨用例消费面补充**（草案审查 iteration 1 实仓核出）：`_cases` grep `ErpB2b|ErpCt|ErpDrp` 实际命中 **6** 个用例——除 C18/C19/C20a/C20b 外还有 `TestErpC02PurReturnRefund`（C02，消费 `erp_ct_contract`：contract save/get + `output/tables/erp_ct_contract.csv` 快照表文件）与 `TestErpC03O2cGoldenPath`（C03，消费 `erp_b2b_edi_doc` / `erp_b2b_edi_log`：完整 EDI inbound flow，位于 b2b EDI 保护区段影响面内）——预分析 (g) 覆盖 + C03 EDI inbound 查找语义（by-code vs 扫描）子核验。
  - **集成快照**：`app-erp-all/_cases` 集成用例快照按 `_chgType` 增量机制评估（M1.2b 实证纯加性插入不入既有快照）；上列 6 个用例无 findPage/totalCount 种子计数断言预期（Phase 1 grep 复证）。
- **保护区域**：**b2b EDI 凭据/外部集成段 = plan-first**（roadmap 横切关注点 1：EDI 凭据触 deployment/external integrations 区域）——本计划在 Phase 1 内置显式独立 plan-audit 门控项（批准后方可落地 b2b EDI 段 CSV，沿 M1.2c notify 段先例），`ErpB2bPartnerCredential`（P+N-DIS）与 `ErpB2bEdiFormat` / `ErpB2bMftConfig` / `ErpB2bTestExchange` 相关行的凭据/端点字段一律 masked placeholder 值（禁真实密钥/真实端点），N-DIS 行 `IS_ACTIVE=false` 停用承载；contract/drp 段无额外保护区。 ORM / 敏感字段 / 会计过账 / 视觉 mask 四类不触及。
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

Status: planned
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof | Decision`
- Prereqs: M1.4b（N=1）已落地并更新常量/对账表；**独立 plan-audit（b2b EDI 段）通过并落盘批准记录**（下方首项，b2b EDI 保护区段 CSV 的前置门控）；规格表 b2b/contract/drp 三节必填 FK 锚点全部落在〔本批〕或〔跨域:md·已seed〕ErpMdPartner / ErpMdMaterial / ErpMdWarehouse / ErpMdOrganization

- [ ] **独立 plan-audit 门控（b2b EDI 凭据/外部集成段）**：独立子代理（fresh session）按 `docs/context/ai-autonomy-policy.md` 外部集成保护政策核验本计划 b2b 段 seed 设计（masked placeholder 纪律 / N-DIS 停用承载 / 零真实凭据零真实端点 / 零外部调用触发面 / C03 EDI inbound 消费面评估），**批准记录落盘本计划后方可落地 `erp_b2b_partner_credential` / `erp_b2b_edi_format` / `erp_b2b_mft_config` / `erp_b2b_test_exchange` 等 b2b EDI 段 CSV**；非触及行（asn 族 / contract / drp）不受此门控阻塞
      - Skill: none
- [ ] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——(a) `ErpB2bAsnMatchPurchaseOrderProcessor` / `ErpB2bAsnRetryMatchProcessor` / `ErpB2bAsnCreateReceiveFromAsnProcessor` per-mutation 事件驱动触发面复证 + 种子 ASN 行终态/relatedBillType 避让设计验证；(b) `ErpCtRebateAgreement__runAccrual` 聚合 partner+period-scoped / accrued 去重 agreement-scoped 复证登记；(c) `DrpDemandAggregator.loadParametersInScope` 仅按 orgId 过滤复证 + C20b `ErpDrpSimulationParamResolver` scenarioId-scoped 复证；(d) `TestErpDrpCrossDock` 模块级 classpath 不含 app-erp-all 种子复证 + `ErpDrpCrossDockStagingTimeoutJob` status=STAGING + updateTime>24h 扫描面复证（种子 cross_dock/dock_appointment 行 N-TERM 终态避让验证）；(e) b2b 两个 dashboard value spec 过滤/标记避让核验（`E2E_` relatedBillType/code 标记族）；(f) `ext-domains-child-table` / `ext-domains-list-filter` no-row→row 路径激活影响评估；(g) b2b/ct/drp 相关 `_cases` 集成用例消费表 grep（**6 用例：C02/C03/C18/C19/C20a/C20b**，C03 含 EDI inbound 查找语义 by-code vs 扫描子核验）+ 快照面评估；(h) 判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`
- [ ] 逐实体核对 ORM 表名与列（三域 `model/*.orm.xml` tableName / `code=`）后，按规格表 b2b / contract / drp 三节（39 行，逐行引用不复制）创建 39 个 CSV；b2b 主子链 asn+line、mft_config+mft_log；contract 主子表 3 组（contract+line / contract+version / rebate_agreement+accrual+tier+settlement）；drp 主子表 2 组（scenario+param / scenario+version）+ plan+line 关联链；行数与用例指示编码（P / N-TERM / N-DIS）按规格表逐行执行；N-DIS 行以 `IS_ACTIVE=false` 停用语义词承载，N-TERM 行以终态词承载
      - Skill: `nop-backend-dev`
- [ ] Decision: b2b 种子语义安全裁决——ASN 行全终态 + relatedBillType 避开 PO 匹配过滤面与 `E2E_` 标记族；EdiDoc/MftLog 行取终态或惰性状态避开重试/发送扫描面（若存在）；`ErpB2bPartnerCredential` 凭据字段 masked placeholder + N-DIS 停用；记录选择值、考虑的替代方案（如「终态承载 vs IS_ACTIVE=false 双保险」）与残留风险于本计划
      - Skill: `nop-backend-dev`
- [ ] Decision: drp 种子组合避让裁决——**载重判据 = `erp_drp_parameter` 行 ORG_ID ≠ "2"**（C20a/C20b fixture orgId，`loadParametersInScope` 仅按 orgId 过滤 + DrpEngine 逐行生成 ErpDrpLine + C20a 断言 lines.size()==2），取 "1" 等 FK 合法组织；(warehouseId, materialId) 组合与 C20a fixture 组合不相交为 belt-and-braces；cross_dock / dock_appointment 行取 N-TERM 终态避开 staging 超时扫描面；记录选择值、考虑的替代方案（如「组合避让为载重 vs ORG_ID 避让为载重」——iteration 1 已裁决后者）与残留风险于本计划
      - Skill: `nop-backend-dev`
- [ ] FK 闭环按规格表逐行落实：必填 FK 仅指向〔本批〕新增行或〔跨域:md·已seed〕（ErpMdPartner / ErpMdMaterial / ErpMdWarehouse / ErpMdOrganization）；禁止悬空引用；字典码 ∈ ORM `<dicts>`
      - Skill: `nop-backend-dev`
- [ ] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期；列集以 ORM/XMeta 生成的实体列为准（先抽样同族既有 CSV，如 `erp_md_partner.csv` / `erp_inv_stock_move.csv`）；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c/M1.2b/M1.4a 先例）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 独立 plan-audit（b2b EDI 段）批准记录落盘本计划，b2b EDI 段 CSV 在批准后落地
- [ ] 干扰面预分析结论在案（零漂移预期清单或预判漂移用例清单；b2b 语义安全与 drp ORG_ID 载重避让两项 Decision 裁决记录在案）
- [ ] 39 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [ ] 无悬空 FK：全部必填 FK 落在〔本批〕∪〔跨域:md·已seed〕集合内

### Phase 2 - 门禁常量 + owner doc 同步

Status: planned
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/b2b/seed-data.md`、`docs/design/contract/seed-data.md`、`docs/design/drp/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [ ] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：基数以「314 + M1.4b 15」= 329 起算本批 +39 → **368**，`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`
      - Skill: `nop-backend-dev`
- [ ] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed +39，精确缺 seed → **0**）+ 新增 M1.5 批次增量行 + 「其他扩展域交易种子」Deferred 之 b2b/contract/drp 子集消费注记（该 Deferred 全部子集清零登记）+ 快照重录义务节资产计数注记（以对账表批次链为准）+ M1 里程碑全量覆盖闭环状态注记
      - Skill: `nop-backend-dev`
- [ ] 新建 `docs/design/b2b/seed-data.md` + `docs/design/contract/seed-data.md` + `docs/design/drp/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔本批〕/〔跨域:md〕标注）、用例指示编码、negative 行语义（N-TERM 终态 / N-DIS 禁用）、干扰面零漂移设计节（含 b2b 语义安全与 drp ORG_ID 载重避让两项 Decision 裁决 + masked placeholder 纪律）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 常量（368）与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [ ] 3 个域 seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: planned
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2 + 独立 plan-audit（b2b EDI 段）通过

- [ ] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [ ] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移，按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏（C02/C03/C18/C19/C20a/C20b 优先排查）→ 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [ ] 视觉快照双面义务核查：运行 `ext-domains-child-table` + `ext-domains-list-filter`（本批激活真实行路径的两 spec）+ `dashboards.visual` + `dashboards.snapshot` + `reports.visual` + `reports.snapshot`；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段；禁止单面重录
      - Skill: `nop-testing`
- [ ] E2E 数值断言联动评估：`b2b-asn-flow.value` + `b2b-edi-detail.value`（过滤非串扰实证）+ b2b 3 个 action spec + ct 5 个 action spec + drp 5 个 action spec + `b2b.smoke` + `contract.smoke` + `drp.smoke` + Phase 1 预分析标记的消费面——有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [ ] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 8 张本批表（b2b ≥ 3 含 asn 主子表头 + contract ≥ 3 含 contract/rebate 主子表头 + drp ≥ 2 含 plan/scenario 头）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [ ] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [ ] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目
      - Skill: none
- [ ] 独立结束审计通过后回写 roadmap 工作项 M1.5 `ready` → `done`（含批次证据摘要 + M1 里程碑闭环注记，格式沿 M1.1/M1.2/M1.4a 批先例）
      - Skill: none

Exit Criteria:

- [ ] TestErpSeedDataIntegrity 全绿且常量断言通过
- [ ] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [ ] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [ ] compliance checker 零漂移；日志条目在位；roadmap 状态回写完成

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 fresh session `ses_f9ee75cfbffedlH7FKiAjsyRuF`）——1 Blocker + 3 Major + 2 Minor。Blocker：drp 种子避让判据轴错误——`loadParametersInScope` 仅按 orgId 过滤 + DrpEngine 逐行生成 ErpDrpLine + C20a/C20b fixture orgId="2" + C20a 断言 lines.size()==2，组合避让不构成保护，ORG_ID ≠ "2" 才是载重判据——已重写基线 + Decision (d) + 预分析 (c) 扩展 C20b ParamResolver；Major m2：C19 `E2E_` 标记归属勘误（C19 实为直建 RECEIVED + 真实 billType + by-id，标记族属 dashboard value specs）；Major m3：`_cases` 枚举漏 C02/C03（6 用例全集 + C03 EDI inbound by-code 子核验）；Major m4：plan-audit 门控结构性挂接 Phase 1（Prereqs + 首项门控 b2b EDI 段 CSV + Exit Criteria）；Minor m5：ct action spec 计数 6→5；Minor m6：C18 runAccrual scope 预记录（partner+period-scoped + agreement-scoped 去重）。全部已并入本稿。
- Independent draft review iteration 2: acceptable as-is（独立子代理 fresh session `ses_f9ed8d304ffeai2c7xNG7lHcBH`）——iteration 1 全部 6 项修订核验通过（ORG_ID 判据/C19 归属/6 用例/门控挂接/计数/C18 scope 逐点实仓吻合），无新引入缺陷；1 Minor 行政项 = Draft Review Record 待回填（本条即回填）。共识达成，转 `active`。

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [ ] 范围内行为完成（39 CSV + 常量 + 对账表 + 3 个 owner doc 段）
- [ ] 相关文档对齐（seed-data.md 对账表/快照重录注记/扩展域交易种子 Deferred 清零注记 + 3 个域 seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [ ] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] **b2b EDI 凭据/外部集成段独立 plan-audit 已完成并记录（批准记录落盘）**
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

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

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
