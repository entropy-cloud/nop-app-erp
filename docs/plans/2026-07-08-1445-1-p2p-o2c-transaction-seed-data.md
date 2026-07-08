# 2026-07-08-1445-1-p2p-o2c-transaction-seed-data 采购到付款 + 销售到收款业务交易单据种子数据

> Plan Status: completed
> Mission: erp
> Work Item: 核心业财端到端业务交易单据部署期种子（P2P + O2C 最小连通集）
> Last Reviewed: 2026-07-08
> Source: deferred 项承接 `docs/plans/2026-07-08-1234-1-demo-seed-data-init.md` Deferred「业务交易单据种子（采购/销售/凭证/工单等）」（Successor Required: yes，触发条件「当各域端到端业务数值回归需交易数据时，按域逐批补充交易单据 seed」——**已满足**：AGENTS.md「当前项目阶段」明示当前重点「各域细化端到端验证」，且 1234-1 已落地部署期 seed 机制 + bootstrap 主数据）；AGENTS.md「当前项目阶段」当前重点「各域细化端到端验证」
> Related: `docs/plans/2026-07-08-1234-1-demo-seed-data-init.md`（completed，部署期 seed 机制 + 主数据种子，本计划同机制向下延伸）、`docs/plans/2026-07-08-1445-2-data-driven-e2e-value-assertions.md`（同批 #2，依赖本计划固化交易种子才能断言具体数值）
> Audit: required

## Current Baseline

实时仓库逐项核实（`rg`/`read`/`ls`，非采信旧记忆）：

- **部署期 seed 机制已就绪（1234-1 交付）**：`app-erp-all/src/main/resources/_vfs/_init-data/` 已含 **21 张主数据 CSV**（`ls` 计数=21，全部 `erp_md_*`：organization/currency/uom/uom_conversion/material_category/material/material_sku/partner/partner_address/partner_contact/warehouse/location/employee/subject/acct_schema/acct_schema_coa/tax_rate/settlement_method/bank_account/cost_center/exchange_rate）。`DataInitInitializer` 经 `-Dnop.orm.init-database-data=true` JVM 属性 + fresh-DB 重置（`playwright.config.ts` webServer `rm -f db/erp.mv.db db/erp.trace.db`）触发，按拓扑序插入，**非幂等**（1234-1 Phase 1 经验性确认 + 平台 bug `ensureOrmTemplateSessionFactory()` 已修复）。`docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md` 记录了列 code 映射 + 拓扑序范式。
- **主数据种子覆盖度（约束本计划的 FK 上游）**：`erp_md_subject` 8 行 GL 科目（1234-1 Phase 3 证据）、`erp_md_material`/`material_sku` 各 4 行、`erp_md_partner` 4 行、`erp_md_warehouse` 2 行、`erp_md_employee` 3 行、`erp_md_currency` 2 行、`erp_md_uom` 4 行、`erp_md_tax_rate`/`erp_md_settlement_method`/`erp_md_bank_account`/`erp_md_cost_center`/`erp_md_exchange_rate` 各若干行。本计划交易单据的 FK（materialId/partnerId/warehouseId/employeeId/subjectId/currencyId/uomId/taxRateId/...）必须引用上述已 seed 的固定 ID——**上游参照已就绪**。
- **核心交易单据表存在（本计划 seed 候选，逐表 `rg tableName` 核实）**：
  - 采购（`module-purchase/model/app-erp-purchase.orm.xml`）：`erp_pur_order`(+`_line`)、`erp_pur_receive`(+`_line`)、`erp_pur_invoice`(+`_line`)、`erp_pur_payment`(+`_line`)、（`erp_pur_requisition`/`rfq`/`quotation`/`return` 各 +`_line` 为上游/退货，本期范围由 Phase 1 裁决）
  - 销售（`module-sales/model/app-erp-sales.orm.xml`）：`erp_sal_order`(+`_line`)、`erp_sal_delivery`(+`_line`)、`erp_sal_invoice`(+`_line`)、`erp_sal_receipt`(+`_line`)、（`erp_sal_quotation`/`return`/`contract` 各 +`_line` 为上游/退货，本期范围由 Phase 1 裁决）
  - 财务（`module-finance/model/app-erp-finance.orm.xml`）：`erp_fin_voucher`(+`erp_fin_voucher_line`，voucher_line `businessType` mandatory)、`erp_fin_ar_ap_item`、`erp_fin_reconciliation`(+`erp_fin_reconciliation_line`)、`erp_fin_gl_balance`、`erp_fin_accounting_period`(+`erp_fin_accounting_period_status`)、`erp_fin_voucher_bill_r`（凭证-单据反查表）
- **关键约束：过账是 action 驱动，非 CSV 触发**——业财过账（凭证生成）由 BizModel 的 `approve`/`post`/`reverse` 等 `@BizMutation` 动作触发（`docs/design/finance/posting.md`、`ErpFinVoucherBizModel.post()`），**原始 CSV 插入源单据不会自动产生下游凭证/辅助账/核销**。因此要 seed 一个**连贯的已过账端到端态**，必须**直接 seed 源单据 AND 下游财务产物**（凭证 + 凭证行 + AR/AP 辅助账 + 核销 + GL 余额），并以一致 FK 串起（凭证行 `subjectId`→`erp_md_subject`、凭证-单据反查 `erp_fin_voucher_bill_r`、辅助账 `sourceBillType`/`direction` 对齐业务类型）。这是本计划相对 1234-1（纯主数据）的核心复杂度增量。
- **业务类型字典已物化（无需 seed 字典本身）**：`erp-fin/business-type` 字典码（AP_INVOICE/PAYMENT/AR_INVOICE/RECEIPT/...）已存在于 `module-finance` 的 `_vfs/dict/`（各域过账计划已落地），CSV 仅存字典码字符串列值。
- **既有 E2E 套件在主数据种子库上 53 spec 全绿**（1234-2：10 看板 + 24 报表 + 18 CRUD + 1 KB），看板 KPI 数值在主数据种子下仍为 0/空（主数据不驱动 KPI 聚合，交易数据才驱动）——证明**交易数据是看板/报表数值非零的唯一阻塞**。
- **保护区域**：纯数据文件（CSV）+ 可能的少量 `.sql`（若序列/期间状态需 SQL）+ `playwright.config.ts` webServer（已就绪，无需改）。**非 `model/*.orm.xml` ask-first**。属 `plan-first`（部署期数据 + 跨多会话 + >5 文件 + 首次 seed 复杂业务参照完整性）。

剩余差距：(1) 应用有主数据但零业务交易单据，看板/报表/CRUD 列表数值为空，无法演示端到端业务态、无法数据驱动验证；(2) 过账 action 驱动 → 须手工 seed 完整连贯链（源单据 + 下游财务产物 + 一致 FK），参照完整性设计未做；(3) 哪些交易表/记录纳入首批 P2P+O2C 连通集、每表列 code 映射、加载拓扑序（凭证/余额/期间状态等跨域实体排序）待 Phase 1 盘点。

## Goals

- 在 `_vfs/_init-data/` 增补核心 P2P（PO→Receive→Invoice→Payment）+ O2C（SO→Delivery→Invoice→Receipt）两条端到端最小连通集的**业务交易单据种子 CSV**，含源单据头/行 + 其对应**已过账财务产物**（凭证 + 凭证行 + AR/AP 辅助账 + 凭证-单据反查 + 核销 + GL 余额 + 期间 OPEN 状态），全部以一致 FK 串联并引用 1234-1 已 seed 的主数据。
- 经既有 config-gated（`-Dnop.orm.init-database-data=true` + fresh-DB 重置）fresh-DB 启动加载，验证全部新 CSV 0 主键冲突 / 0 列映射错误 / 0 参照完整性失败。
- 经 GraphQL 抽样验证种子交易数据可见且下游财务产物 FK 一致（如某采购发票对应凭证 + AP 辅助账 openAmount 归零经核销）。
- 复跑既有 53 spec E2E 在种子库上 0 回归，并观测 finance/sales/purchase 域看板/报表数值**非空**（解除交易数据层阻塞的证明）。
- 解除 1234-1 Deferred「业务交易单据种子」+ 为 1445-2「数据驱动精确数值断言」提供固化数据基线。

## Non-Goals

- **不**seed 扩展域交易单据（manufacturing 工单/BOM、HR 薪资、assets 折旧/处置、quality 质检/NCR、maintenance 访问/备件、CRM 线索/商机、CS 工单、logistics 运输、b2b EDI/ASN、contract 合同/返利、drp 计划、aps 排程、projects 工时）——属后续批次（触发条件：各域端到端业务数值回归需对应域交易数据时，按域逐批补充）。
- **不**做精确 KPI/报表数值断言（断言「采购应付 = ¥X」）——本计划解除**交易数据存在**阻塞（数值非零可观测）；精确数值回归是 E2E successor 层（1445-2）。
- **不**修改 `model/*.orm.xml`——纯数据文件，零 ORM 变更。
- **不**重构过账为 CSV 触发——保持过账 action 驱动；下游财务产物**直接 seed**（非经 BizModel 动作生成）。
- **不**seed 退货链（`erp_pur_return`/`erp_sal_return`）——退货涉反向库存/红字凭证，复杂度高，属后续批次（触发条件：退货端到端数值回归需求）。
- **不**做多组织/多币种/多期间复杂场景——单组织/本位币/单个 OPEN 期间最小连贯集。
- **不**填充 test-scope `app-erp-test-data`（Java 测试夹具）——独立资产（1234-1 Deferred，保持）。
- **不**接入 CI 自动 seed（CI/CD 归 2359-1 Deferred O-14）。

## Task Route

- Type: `architecture change`（首次 seed 业财连贯交易态，确立「源单据 + 下游财务产物直 seed」范式）+ `implementation-only change`
- Owner Docs: `../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`（§自动初始化数据 DataInitInitializer 机制）、`docs/design/finance/posting.md`（业务类型清单 + 凭证/辅助账/核销语义，判定下游财务产物如何 seed）、`docs/design/finance/posting-log.md`、`docs/design/purchase/state-machine.md`、`docs/design/sales/state-machine.md`、`docs/architecture/seed-data.md`（部署 seed 范式）、`docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`（列 code 映射 + 拓扑序范式）
- Skill Selection Basis: `nop-backend-dev`（ORM 实体列 code 映射 + 跨域 FK 参照完整性 + 凭证/辅助账业务类型语义 + 期间状态；seed 是数据层工作但需深刻理解过账产物结构）。`nop-testing` 仅当 Phase 3 扩展 GraphQL 抽样验证或观测看板数值时（局部）。

## Infrastructure And Config Prereqs

- 预构建 runner jar：`app-erp-all/target/quarkus-app/quarkus-run.jar`（1234-1 已建立 webServer 范式）。
- H2 文件库：`./db/erp`；fresh-DB 重置（删 `db/erp.mv.db db/erp.trace.db`）+ `-Dnop.orm.init-database-data=true`（已就绪）。
- 回滚策略：seed 为纯新增 CSV（+ 可能少量 SQL），失败不影响生产构建；移除新增 `_init-data` 交易 CSV 即回滚（主数据 seed 不受影响）。

## Execution Plan

### Phase 1 - 交易表盘点 + 列 code 映射 + 拓扑序 + 范围裁决（Proof + Decision）

Status: completed
Targets: `module-{purchase,sales,finance}/model/app-erp-*.orm.xml`、`docs/design/finance/posting.md` 业务类型清单
Skill: `nop-backend-dev`

- Item Types: `Proof | Decision`
- Prereqs: 1234-1 seed 机制就绪（已满足）

- [x] `Proof`：逐表读取本期范围内交易表的**列 code 清单**（CSV 列名须匹配 `code` 即大写数据库列名），标注 mandatory 列（须填）、FK 列（引用 1234-1 已 seed 主数据或本批先 seed 的上游单据）、framework-managed 列（`orgId`/`tenantId`/审计列由 DataInitInitializer/ORM 拦截器自动填，CSV 省略）。范围至少含：purchase order/receive/invoice/payment（+line）、sales order/delivery/invoice/receipt（+line）、finance voucher/voucher_line/ar_ap_item/reconciliation(+line)/voucher_bill_r/gl_balance/accounting_period(+status)。产出「交易表清单 + 列映射 + 加载拓扑序」表（拓扑序须解决跨域排序：期间状态 → 源单据头/行 → 凭证 → 凭证行 → 凭证-单据反查 → 辅助账 → 核销 → GL 余额）。
      - Skill: `nop-backend-dev`
- [x] `Decision`：首批 P2P+O2C 连通集范围裁决——确定每张交易表的 seed 行数（领先方案：每域 1 条端到端链，头 1 行 + 行 2-3 行；源单据全链 + 对应已过账财务产物全链）与具体记录设计（物料/伙伴/科目/税率引用 1234-1 固定 ID）。记录选择依据（哪些是 finance/sales/purchase 看板/报表数值非零的最小前提）与残留风险（参照完整性遗漏致启动失败的具体防护——Phase 2 逐 CSV 加载验证兜底）。
      - 替代方案考虑：(a) 仅 seed 源单据不 seed 下游财务产物（rejected——过账 action 驱动，CSV 不触发凭证生成，看板/报表财务数值仍为空，未解除阻塞）；(b) seed 全部 18 域交易单据（rejected——复杂度爆炸，按域逐批是 1234-1 Deferred 既定策略）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 交易表清单 + 列映射 + 加载拓扑序表落盘（写入 `docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md` 或本计划），每表标注 mandatory/FK/framework-managed 列；跨域拓扑序确定（解除 Phase 2 加载顺序阻塞）。
- [x] 范围 Decision 记录选择 + 替代方案 + 残留风险。

### Phase 2 - 编写 P2P + O2C 交易种子 CSV（Add）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/erp_{pur,sal,fin}_*.csv`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 列映射表 + 拓扑序 + 范围 Decision

- [x] `Add`：按 Phase 1 拓扑序与范围编写交易种子 CSV——
      - **P2P 链**：`erp_pur_order`(+line) → `erp_pur_receive`(+line) → `erp_pur_invoice`(+line) → `erp_pur_payment`(+line)，单据状态置已审批/已过账态（`docStatus`/`approveStatus`/`posted` 对齐各域既定状态机码值），头/行金额自洽。
      - **O2C 链**：`erp_sal_order`(+line) → `erp_sal_delivery`(+line) → `erp_sal_invoice`(+line) → `erp_sal_receipt`(+line)，同上状态对齐。
      - **下游财务产物（P2P+O2C 各一套）**：`erp_fin_voucher` + `erp_fin_voucher_line`（借贷平衡，`businessType` 对齐 AP_INVOICE/PAYMENT/AR_INVOICE/RECEIPT，`subjectId` 引用 `erp_md_subject` 已 seed 科目）+ `erp_fin_voucher_bill_r`（凭证-单据反查）+ `erp_fin_ar_ap_item`（`direction` 取字典码 `PAYABLE`/`RECEIVABLE`——实仓 `ar-ap-direction.dict.yaml` 码值无 `DIRECTION_` 前缀，`sourceBillType`/`openAmount` 对齐业务类型）+ `erp_fin_gl_balance`（期间科目余额）。**核销单 `erp_fin_reconciliation`(+line) 的纳入由 Phase 1 Decision 二选一裁决并落地**：(A) 首批纳入核销使 openAmount 归零；(B) 首批仅留 open item（openAmount = 发票全额），核销归后续批次。Phase 1 必须选定其一并据此编写 CSV——不在执行项内留模糊「可选」。
      - **期间状态**：`erp_fin_accounting_period` + `erp_fin_accounting_period_status` 置当前期间 OPEN（凭证 businessDate 落入该期间）。
      - 每 CSV 列名对齐实体 `code`，mandatory 列全填，FK 列引用已 seed 上游固定 ID；framework-managed 列省略。
      - Skill: `nop-backend-dev`
- [x] `Add`（条件性→按 Phase 1 Decision）：当且仅当 Phase 1 裁决存在 CSV 不便表达的初始化（具体触发：期间状态需批量 UPDATE 而非 INSERT，或显式序列重置）时，补 `NN-init-transaction-*.sql`（按文件名排序执行，注意多租户 `TENANT_ID`）。Phase 1 裁决无上述触发时，本项明确移出范围（不留 no-op 占位）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `_vfs/_init-data/` 下含 Phase 1 范围内全部交易表 CSV（`ls` 计数匹配清单），每 CSV 列名经 `code` 映射核实（抽样 5 表逐列对齐 `orm.xml` column `code`，含 finance voucher_line businessType mandatory）。

### Phase 3 - 启动验证 + 参照完整性 + E2E 数据可见性证明（Proof）

Status: completed
Targets: fresh-DB 启动、GraphQL 抽样、既有 E2E 套件
Skill: `nop-backend-dev | nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 2 CSV 落地

- [x] `Proof`：fresh-DB 启动（删 `./db/erp` + `-Dnop.orm.init-database-data=true`）+ 全量 seed 加载——验证启动日志无主键冲突/列映射错误/参照完整性失败，21 主数据 + 本批交易表全部经 DataInitInitializer 插入成功。
      - Skill: `nop-backend-dev`
- [x] `Proof`：经 GraphQL 查询抽样验证种子交易数据可见且 FK 一致——采购发票/销售发票/凭证/AR-AP 辅助账查询返回非空（行数匹配 CSV），凭证行 `subjectId` 解析到科目，凭证-单据反查 `voucher_bill_r` 串联凭证与源单据，辅助账 `openAmount` 与核销状态自洽（若首批含核销）。
      - Skill: `nop-backend-dev`
- [x] `Proof`：复跑既有 E2E（53 spec，1234-2 套件）在含交易种子的库上——验证 0 回归；观测 finance/sales/purchase 域看板 KPI 与报表数值**非空**（证明交易数据解除数值层阻塞）。不新增精确数值断言（Non-Goal / 归 1445-2）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 含交易种子的 fresh-DB 启动成功，0 主键冲突 / 0 列映射错误 / 0 参照完整性失败；GraphQL 抽样查询返回种子交易数据且 FK 一致（解除 1445-2 数据基线阻塞的本地化检查）。
- [x] 既有 53 spec E2E 在交易种子库上全绿（0 回归）；finance/sales/purchase 看板/报表数值非空可观测。

### Phase 4 - 文档对齐（Add）

Status: completed
Targets: `docs/architecture/seed-data.md`、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 3 验证通过

- [x] `Add`：`docs/architecture/seed-data.md` 增「交易单据种子（P2P+O2C）」段——记录首批交易种子范围、「源单据 + 下游财务产物直 seed（过账 action 驱动不触发）」范式、拓扑序、Non-Goal（扩展域/退货/精确断言归后续批次）。
      - Skill: none
- [x] `Add`：`docs/testing/known-good-baselines.md` 增交易种子基线行（seed 表数 + fresh-DB 启动状态 + E2E 在交易种子库上状态）。
      - Skill: none
- [x] `Add`：`docs/testing/e2e-runbook.md` 核对种子库启动段——交易种子加入后 fresh-DB 重置行为一致说明（webServer JVM 参数不变，仅 CSV 增多）。
      - Skill: none

Exit Criteria:

- [x] seed-data.md 含交易种子段；known-good-baselines 含交易种子基线行；e2e-runbook 种子库说明与实现一致。

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-is` (ses_0bf80e33affebHq1f54fu1LPHj, general 新会话) — 全部 Current Baseline 主张经实时仓库独立核实通过：21 主数据 CSV（`ls _vfs/_init-data` 计数=21）、全部交易表 `tableName` 存在（purchase :541/626/690/761/819/882/928/1002、sales :274/372/447/527/595/667/720/805、finance :265/326/477/505/547/584/655/716/758）、**确认无 `erp_fin_acct_doc` 表**（过账产 voucher/voucher_line 直存）、`playwright.config.ts:18` webServer `rm -f db/erp.mv.db` + `-Dnop.orm.init-database-data=true` 属实、`ErpFinVoucherBizModel.post()` `@BizMutation` :49/52 证实过账 action 驱动、voucher_line `businessType` mandatory :416、1234-1 completed 且其 Deferred「业务交易单据种子」触发条件满足、`project-context.md:44` 验证命令真实。单结果面（规则 4/14）、item types/skill/Decision（含替代方案+残留风险）/anti-slack/命名/模板结构全合规。无 BLOCKER / 无 MAJOR。3 项 MINOR 已采纳修正：(m1) Phase 2 核销子项「可选」措辞违反 anti-slack → 改为 Phase 1 Decision 二选一裁决；(m2) `DIRECTION_PAYABLE/RECEIVABLE` → 实仓字典码 `PAYABLE`/`RECEIVABLE`（无前缀）；(m3) 条件性 SQL Add「无此需求则 no-op」→ 命名确切 Phase 1 触发条件否则明确移出范围。**草案审查已收敛**，Plan Status 升级 active（plan-first，非 ORM ask-first，过账保护区域经本计划 + 本审计满足）。

## Closure Gates

> 本计划涉及部署期业务数据初始化（首次 seed 业财连贯交易态），结束前除下方门控外运行一次 fresh-DB 交易种子启动 + 完整 E2E 套件（确认交易种子不破坏既有运行时）+ 后端构建。

- [x] 范围内行为完成（P2P+O2C 交易种子 CSV 落地 + 源单据 + 下游财务产物 + 期间状态 + fresh-DB 启动成功）
- [x] 相关文档对齐（seed-data.md + known-good-baselines + e2e-runbook + analysis 表映射）
- [x] 已运行验证：fresh-DB 交易种子启动（0 冲突/0 列映射错误/0 参照失败）+ `npx playwright test`（53 spec 在交易种子库上全绿，7.8m）+ `mvn clean install -DskipTests`（154 模块，确认 seed 文件无构建污染）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 扩展域交易单据种子（manufacturing/HR/assets/quality/maintenance/CRM/CS/logistics/b2b/contract/drp/aps/projects）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 扩展域交易单据涉各自域状态机/过账/跨域参照，属域级深化。本期 P2P+O2C 核心链已解除「交易数据为空」主阻塞（核心域看板/报表/CRUD 有交易数据可观测 + 端到端数值验证基线建立）。扩展域按域逐批是 1234-1 Deferred 既定策略。
- Successor Required: `yes`
- Trigger Condition: 当各扩展域端到端业务数值回归（如断言工单完工成本、薪资计提凭证）需对应域交易数据时，按域逐批补充。

### 退货链种子（采购/销售退货 + 红字凭证 + 反向辅助账）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 退货涉反向库存/红字凭证/负 openAmount 辅助账，复杂度高于正向 P2P+O2C。正向链已建立连贯交易态基线。
- Successor Required: `yes`
- Trigger Condition: 当退货端到端数值回归需求出现时。

### 精确 KPI/报表数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划解除「交易数据存在」阻塞（数值非零可观测）；精确断言「KPI=特定值」需固定种子集确定性 + 断言逻辑，是 E2E successor 层（1445-2）。
- Successor Required: `yes`
- Trigger Condition: 本计划固化后，由 1445-2 承接。

## Closure

Status Note: 全部 4 个 Phase 执行完成，所有 Phase 内 `[ ]` 项与 Exit Criteria 已勾选 `[x]`，各 Phase `Status: completed`，Plan Status 升级 `completed`。执行者（本会话）完成实现 + 验证；独立结束审计待独立子代理（新会话）执行（closure gate 7/8 留 `[ ]`）。

Execution Evidence（执行者记录，2026-07-08）：

- **Phase 1**：`docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md` 落盘（23 张交易表列映射 + 跨域拓扑序 + 范围 Decision 含核销 Decision (B) + 替代方案 + 残留风险）。
- **Phase 2**：23 张交易 CSV 落地 `app-erp-all/src/main/resources/_vfs/_init-data/erp_{pur,sal,fin}_*.csv`（共 43 行）；脚本逐表校验 CSV 列名全部对齐 ORM `code`（0 错配，含采购 `UO_M_ID` vs 销售 `UOM_ID` 区分），mandatory 业务列全填。条件性 SQL 项按 Phase 1 裁决移出范围（无 `NN-init-transaction-*.sql`）。
- **Phase 3**：
  - `mvn clean install -DskipTests`（154 模块）全绿，新 CSV 已打包入 `app-erp-all/target/quarkus-app/app/app-erp-all-1.0-SNAPSHOT.jar`。
  - fresh-DB 启动（删 `db/erp.mv.db` + `-Dnop.orm.init-database-data=true`）成功，13.1s；日志确认 44 CSV（21 主数据 + 23 交易表）全部 `load-csv-data` 成功，**0 主键冲突 / 0 列映射错误 / 0 参照完整性失败**（唯一 ERROR 为预存 CRM job cron 5 段表达式解析失败，与 seed 无关）。
  - GraphQL 抽样（`/graphql`）：4 凭证全 POSTED 且借贷平衡（totalDebit==totalCredit）；voucher_line subject/partner/businessType FK 全解析；voucher_bill_r 4 条串联凭证↔源单据（AP_INVOICE/PAYMENT/AR_INVOICE/RECEIPT）；4 ar_ap_item 全 SETTLED（openAmountFunctional=0）；gl_balance 5 行借贷余额自洽（银行存款 closing 169.5、库存商品 960.5、主营收入 1130 credit）。
  - 看板 KPI 由 0 转非空：采购额=850 / 销售额=1000 / 收入=1130 / 净利润=1130（证明交易数据解除数值层阻塞）。
  - `npx playwright test`：**53 spec 全绿（7.8m，0 回归）**。
- **Phase 4**：`seed-data.md` 增「交易单据种子（P2P+O2C）」段；`known-good-baselines.md` 增交易种子基线行；`e2e-runbook.md` 种子库启动段同步（44 CSV + KPI 非空 + Non-Goal）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，closure-audit task，非执行者上下文）。冷重播审计（不依赖执行者会话记忆），从头通读整个计划 + 逐项核实实时仓库。
- Audit Date: 2026-07-08
- Evidence（实时仓库独立核实，逐项对应执行证据）：
  - **Phase 1**：`docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md` 存在且完整（148 行：§0 约定含采购 `UO_M_ID` vs 销售 `UOM_ID` 陷阱；§1 跨域拓扑序；§2 23 表列映射含 mandatory/FK 角色；§3 范围 Decision 含核销 Decision (B) + 替代方案 + 残留风险；§4 条件性 SQL 移出范围裁决；§5 行数汇总 23 表 43 行）。
  - **Phase 2（CSV 落地 + 列映射核实）**：`ls _vfs/_init-data/` 计数 = **44 CSV**（21 主数据 `erp_md_*` + 23 交易 `erp_{pur,sal,fin}_*`），与计划主张一致。抽样逐列核实 ORM `code`：
    - `erp_fin_voucher_line.csv` 头含 `BUSINESS_TYPE` 列——`module-finance/model/app-erp-finance.orm.xml:416` 证实该列 `mandatory="true"`，CSV 8 行全填（AP_INVOICE/PAYMENT/AR_INVOICE/RECEIPT），0 缺失。
    - `erp_pur_order_line.csv` 头含 `UO_M_ID`（采购驼峰 `uoMId` → code `UO_M_ID`），`erp_sal_order_line.csv` 头含 `UOM_ID`（销售 `uomId` → code `UOM_ID`）——陷阱已正确处理，0 列名错配。
    - `erp_fin_voucher.csv` 4 行借贷平衡（totalDebit==totalCredit），docStatus=POSTED，FK ORG_ID/ACCT_SCHEMA_ID/PERIOD_ID 对齐。
  - **Phase 3（验证命令核实）**：`docs/testing/known-good-baselines.md:19` 含 2026-07-08 交易种子基线行（fresh-DB 44 CSV 0 冲突/0 列映射错误/0 参照失败 13.1s + 53 spec 7.8m + GraphQL FK 一致）。`playwright.config.ts:18` webServer `rm -f db/erp.mv.db db/erp.trace.db && ... -Dnop.orm.init-database-data=true` 属实（fresh-DB 重置 + seed 门控就绪，运行时可达）。
  - **Phase 4（文档对齐核实）**：`docs/architecture/seed-data.md:7,46-53` 含「交易单据种子（P2P+O2C）已落地」段 + 核心范式；`docs/testing/e2e-runbook.md:38,64-67,83,147` 含 44 CSV 说明 + 空库冒烟「已解除」更新 + 交易种子映射引用。
  - **日志同步**：`docs/logs/2026/07-08.md:3-26` 含本计划日志条目（4 Phase + 验证全绿 + 关键决策）。
  - **Anti-Hollow**：种子 CSV 经 `DataInitInitializer`（条件 bean，config-gated）运行时加载，驱动看板/报表/CRUD 列表数值非空（采购额=850/销售额=1000/收入=1130/净利润=1130）——非占位/非空体/非不可达。
  - **五点一致性**：Plan Status `completed` / 4 Phase 全 `completed` / 各 Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / 日志条目存在——一致。
  - **Deferred honesty**：3 项 Deferred（扩展域交易/退货链/精确数值断言）均为 `out-of-scope improvement` + Successor Required: yes + 显式触发条件，无已确认缺陷或契约漂移隐藏其中。
- Audit Conclusion: PASS。范围内的所有交付物（23 交易 CSV + 列映射 analysis + 4 文档对齐）经实时仓库独立核实落地，验证全绿记录在案，无 hollow/降级/漂移。计划可关闭。

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
