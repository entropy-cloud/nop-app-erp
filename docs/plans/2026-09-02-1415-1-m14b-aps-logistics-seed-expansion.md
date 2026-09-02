# 2026-09-02-1415-1 M1.4b 第一批扩展域 seed 扩面 B——aps + logistics（15 CSV）

> Plan Status: completed
> Last Reviewed: 2026-09-02
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.4b（ready，Deps M0.1 + M0.2 均 done）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁常量随批更新协议）、同批 `2026-09-02-1415-2-m15-b2b-ct-drp-seed-expansion.md`（N=2，共享门禁常量与对账表触点）
> Audit: required

## Current Baseline

- 实仓 363 个 `app.erp.*` 实体（className 口径；运行时注册 368 = 363 + finance 5 缺 className 补充档，本域不涉及——finance 5 已由 M1.2a1 消费），有 seed 314 / 精确缺 54（M1.4a 后对账表口径）；`_init-data/` 实测 318 CSV（314 app.erp + 4 平台）+ 1 SQL；`TestErpSeedDataIntegrity` 基线常量 `EXPECTED_APP_ERP_CSV_COUNT = 314` / `EXPECTED_PLATFORM_CSV_COUNT = 4` / `EXPECTED_APP_ERP_ENTITY_COUNT = 363`（+ finance 5 缺 className 补充集，非本域不触碰）。
- 两域实测已 seed / 缺 seed（与 roadmap M1.4b 清单一致，均为零 seed 域）：
  - aps：已 seed **0**，缺 **7**（capacity_reservation / constraint / dispatch_log / dispatch_rule / op_routing / operation_order / schedule）；
  - logistics：已 seed **0**，缺 **8**（carrier / carrier_config / delivery_booking / delivery_window / shipment / shipment_line / shipment_log / shipment_parcel）。
  - 逐实体规格见 `docs/architecture/seed-data.md` 规格表 aps / logistics 两节（建议 CSV 文件名 / 行数 / 主子表组 / FK 闭环依赖 / 用例指示），本计划逐行引用不复制。ORM tableName 已实仓核实：`erp_aps_*` × 7 + `erp_log_*` × 8（`module-aps/model/app-erp-aps.orm.xml` + `module-logistics/model/app-erp-logistics.orm.xml`）。
- 当前验证基线：`mvn test -pl app-erp-all` **71/0/0/1** 全绿（M0.2 实测，经七批 M1.x 维持，M1.4a 2026-09-02 复证）；compliance 门控锚点 = `docs/audits/compliance-baseline.md` §BASELINE 机器块（R2c: 1537），**R2c=1542 / R2b=242 / R12a=71** 为已登记实测值（+5/+2/+1 pre-existing，沿 plan `2026-09-01-0527-1` 口径协议）。
- 既有 CSV 约定：列头为 DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000；装载拓扑序由 `DataInitInitializer` 自动排序；CSV 装载经 `dao.saveEntity` 直插，不触发 biz mutation / 状态机 / Processor（前七批 M1.x 先例，本计划 Phase 1 复证）。
- 冻结时钟纪律（`docs/bugs/2026-09-01-0017` / `2026-09-01-0058` 家族教训）：本批全部 seed 行日期列使用静态固定值，禁止 `now()`/滚动期间语义（schedule / operation_order / dispatch_log / delivery_booking / delivery_window / shipment 时效类实体尤其注意）。
- **干扰面**（本计划核心风险，读取面均 2026-09-02 实仓核实）：
  - **aps 域**：app-erp-all 集成用例 `TestErpC08MrpApsRelease`（C08）+ `TestErpC21ApsCapacityLoad`（C21）——均经 `@var:` fixture 自建 `ErpApsOperationOrder` / `ErpApsSchedule` 并按 id reload，自建自清；**排程引擎读取面**（草案审查 iteration 1 实仓核出）：`ErpApsSchedule__publish` 为纯状态翻转（`ErpApsScheduleBizModel`，processor 豁免注册表在案）零读取；`scheduleForward` / `scheduleToc` / `insertRushOrder` → `ErpApsSchedulingProcessor` 全集装载——`loadPendingOrders` = **全部** status ∈ {DRAFT, UNSCHEDULABLE} 且 earliestStartDateT 落排程窗口的 OperationOrder、`loadEnabledRoutings` = 全部 isEnabled=true 的 OpRouting、`loadMaintenanceConstraints` = 全部 constraintType=MAINTENANCE 且窗口相交的 Constraint、`hasOverlappingReservation` = CapacityReservation 按 machineId + 区间相交。**具体破坏向量**：C08 fixture 排程窗口 `2026-07-10 ~ 2026-07-20`、fixture op machineId=`1`；C21 fixture op machineId=`WC-001`——种子 operation_order P 行若取 DRAFT/UNSCHEDULABLE 态且 earliestStartDateT 落 C08 窗口内将被 C08 装载重排（漂移或 `ERR_APS_CAPACITY_CONFLICT`），种子 capacity_reservation / constraint 行 machineId 取 `1`/`WC-001` 且静态窗口相交同理；`ErpApsDispatchRule` 全表读存在（`ErpApsAutoDispatchProcessor`）但 config 门控默认关（`erp-aps.auto-dispatch-enabled` 缺省 false）对本批惰性——Phase 1 逐点复证上述读取面并按零漂移设计裁决处置。
  - **aps 域 E2E 消费面**：`tests/e2e/dashboards/aps-schedule-gantt.value.spec.ts`——自包含 setup（`E2E-GANTT-${Date.now()}` 唯一 code 自建 op order）+ `MACHINE_ID='910'` 客户端过滤 + `findPage(limit:500, orderBy plannedStartDateT)` 全量行断言 `> 0`；种子行仅增大 `allItems`（无害），但**种子 machineId 禁用 `910`（gantt spec 过滤值）、`100`（aps-action 套件值）、`1`（C08 fixture 值）、`WC-001`（C21 fixture 值）**，避免过滤串扰与排程引擎相交；`tests/e2e/business-actions/` aps 4 个 action spec（operation-order / rush-order / schedule-toc / schedule）+ `tests/e2e/crud/aps.smoke.spec.ts` 自包含/渲染型，种子零破坏预期（Phase 1 消费表 grep 复证）。
  - **logistics 域**：app-erp-all 集成用例 `TestErpC05InvLandedCost`（C05，`ErpLogCarrier__save` + `ErpLogShipment__save` fixture + `ErpLogShipment__get` by-id freight 核对）+ `TestErpC19B2bAsnAutoReceiveLandedCost`（C19，log shipment webhook 段，`erp-log.webhook-signature-required` 测试内关闭）——均 fixture 自建 by-id 消费；`tests/e2e/business-actions/` log 3 个 action spec（shipment / delivered-freight-posting / path2-landed-cost-auto-create）+ `logistics.smoke.spec.ts`；`tests/e2e/visual/ext-domains-child-table.visual.spec.ts` 覆盖 `ErpLogShipment-main`（3 sub-grids: lines/parcels/logs，`row-update-button` 开编辑抽屉）——**该 spec 现依赖「ext domain 可能无 seed data」的 no-row 分支，本批种子落地后激活真实行路径**（spec 注释自证 no-row fallback 存在），DOM-className 结构断言预期仍通过，Phase 3 实跑核验；`ext-domains-list-filter.visual.spec.ts` 覆盖 `ErpLogShipment-main` asideFilter（date range + status in）+ `ErpLogShipmentLog-main` readonly（row-view dialog soft-probe 分支，种子后同样激活真实行路径）。
  - **集成快照**：`app-erp-all/_cases` 集成用例快照按 `_chgType` 增量机制评估（M1.2b 实证纯加性插入不入既有快照）；`_cases` grep `ErpAps|ErpLog` 命中 C05/C08/C19/C21 四个用例（上列），无 findPage/totalCount 种子计数断言预期（Phase 1 grep 复证）。
- **保护区域**：无——aps/logistics 两域规格表行不触 ORM、敏感字段、会计过账、外部集成、视觉 mask 五类保护区；标准草案审查即可，无独立 plan-audit 义务。
- **Deferred 消费**：`docs/architecture/seed-data.md` §Non-Goals（归后续批次）「其他扩展域交易种子（logistics/b2b/contract/drp/aps 后续批次）——无看板无报表」Deferred 之 **aps + logistics 子集**——本计划按 roadmap M1 全量覆盖口径消费该 Deferred（触发条件由全覆盖目标取代，沿 M1.2b 消费 L297 / M1.4a 消费 L352 先例）。
- **owner doc 目录映射**：roadmap M1.4b 行记 `docs/design/aps/` + `logistics/`；物理目录 `docs/design/aps/` 与 `docs/design/logistics/` 均实存（2026-09-02 核实），owner doc 落两目录下新建 `seed-data.md`。

## Goals

- 15 个 seed CSV 落地（aps 7 + logistics 8，逐行消费规格表两节），每实体最小可用数据集（行数 ≤ 规格表建议区间，用例指示编码 P / N-TERM / N-DIS），FK 引用零悬空，logistics 4 组主子表（carrier+config / shipment+line / shipment+log / shipment+parcel）+ delivery_booking→shipment+window 链路 + aps dispatch_log→operation_order 边完整。
- `TestErpSeedDataIntegrity` CSV 基线常量按随批更新协议推进（基数以「314 + 已落地批次 CSV 数」为准，本批 +15），seed-data.md 对账表同步（有 seed 314→329 / 精确缺 54→39）。
- `docs/design/aps/seed-data.md` + `docs/design/logistics/seed-data.md`「种子数据」owner doc 段落地（2 个新文件）。

## Non-Goals

- 不修改任何 ORM 模型（保护区 auto + dual-agent-approval）——本计划纯 CSV-only 路径。
- 不修改任何生产 Java 代码（唯一 Java 触点 = `TestErpSeedDataIntegrity.java` 的 CSV 基线常量按内置协议更新）；不触碰 APS 排程引擎 / dispatch / capacity 计算逻辑与物流运力/履约逻辑（seed 为被动数据）。
- 不覆盖 b2b / contract / drp 及其余 M1.x 工作项的缺 seed 实体（归同批 N=2 M1.5 计划）；不触碰 finance 5 个缺 className 运行时实体。
- 不修改既有任何 CSV 文件的任何行（aps/logistics 为零 seed 域，本批纯新增）。
- 不新增 aps/logistics 域业财一体 GL 凭证串联 seed（delivered-freight-posting 等过账语义归既有 C05/C19 fixture 测试覆盖，seed 不做 GL 回链）。
- 不新增 M2.x 像素断言、不做负向视觉断言、不做跨浏览器矩阵（roadmap Non-Goal）。
- 不处置 hr/drp 2 处预存回归；不引入 production-grade 真实个人数据。
- 不做「seed 字段值与 owner doc 业务规则一致性」的逐字段回放测试（roadmap Non-Goal）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/seed-data.md`（规格表 aps/logistics 两节 + 对账表 + 扩展域交易种子 Deferred）、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（M1.4b 行 + 横切关注点）、`docs/testing/e2e-runbook.md`（视觉方法论 + 快照重录协议）、`docs/design/aps/`（scheduling / auto-dispatch / constraint-based-planning / state-machine）+ `docs/design/logistics/`（carrier-integration / delivery-window / state-machine）
- Skill Selection Basis: roadmap M1.4b 行指定 `nop-backend-dev`（数据资产须对齐实体/字典/列命名约定）；Proof 阶段运行测试套件与视觉 spec 属测试域，加载 `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（fresh-DB 由 `./scripts/start-app.sh` 承载；`init-database-data: true` 已默认开启）。
- 回滚策略：seed CSV 为纯新增文件，回滚 = 删除本批新增文件 + 常量回拨 + git revert 文档变更；不涉及数据迁移。

## Execution Plan

> 执行顺序约束：本计划为同批两计划（N=1/N=2）之一，N=1 先行；`EXPECTED_APP_ERP_CSV_COUNT` 与 seed-data.md 对账表为共享触点，N=2 的常量基数以「314 + 已落地批次 CSV 数」为准（含本批 +15），避免覆盖写。

### Phase 1 - Seed CSV authoring（15 CSV = 7 + 8）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof | Decision`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪；规格表 aps/logistics 两节必填 FK 锚点全部落在〔本批〕或〔跨域:md·已seed〕——两节实际跨域必填边仅 delivery_window→ErpMdPartner 一处，沿草案审查 iteration 1 勘误收敛）

- [x] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——(a) aps 排程引擎读取面逐点复证（`ErpApsSchedule__publish` 纯状态翻转零读取 / `ErpApsSchedulingProcessor` 四类全集装载的过滤谓词与 fixture 机器/窗口值 `1`、`WC-001`、`2026-07-10~20` / `ErpApsAutoDispatchProcessor` config 门控缺省关），判定种子行零漂移或列出预判漂移用例；(b) aps-schedule-gantt value spec 过滤值核验（种子 machineId 避开 100/910/1/WC-001）；(c) C05/C19 对 carrier/shipment 的消费是否 by-id（fixture 自建）复证；(d) `ext-domains-child-table` / `ext-domains-list-filter` no-row→row 路径激活影响评估；(e) 是否有处理器按批扫描 logistics 表（delivered-freight / path2 landed-cost 触发为事件驱动非批扫描的复证）；(f) aps/log 相关 `_cases` 集成用例消费表 grep（C05/C08/C19/C21）+ 快照面评估；(g) 判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`
- [x] 逐实体核对 ORM 表名与列（`module-aps` / `module-logistics` 两域 `model/*.orm.xml` tableName / `code=`）后，按规格表 aps / logistics 两节（15 行，逐行引用不复制）创建 15 个 CSV；logistics 主子表组 4 组（carrier+config / shipment+line / shipment+log / shipment+parcel）；行数与用例指示编码（P / N-TERM / N-DIS）按规格表逐行执行；N-DIS 行以 `IS_ACTIVE=false` 停用语义词承载，N-TERM 行以终态词承载
      - Skill: `nop-backend-dev`
- [x] Decision: aps 种子值域零漂移裁决——(i) operation_order P 行 status 取排程引擎 pending 集 {DRAFT, UNSCHEDULABLE} 之外（如 PLANNED），earliestStartDateT 取静态值避开 C08 fixture 窗口 2026-07-10~20；(ii) capacity_reservation / constraint 行 machineId 避开 `100` / `910` / `1` / `WC-001`（或静态窗口与 fixture 窗口不相交）；(iii) constraint 行 constraintType 避开 MAINTENANCE 或窗口避让；(iv) op_routing 行 isEnabled 取值经 (a) 复证裁决；(v) dispatch_rule 行不依赖 auto-dispatch（config 缺省关）；记录选择值、考虑的替代方案（如「全窗口移出 vs 状态避让」）与残留风险于本计划
      - Skill: `nop-backend-dev`
- [x] FK 闭环按规格表逐行落实：必填 FK 仅指向〔本批〕新增行或〔跨域:md·已seed〕（delivery_window→ErpMdPartner）；禁止悬空引用；字典码 ∈ ORM `<dicts>`
      - Skill: `nop-backend-dev`
- [x] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期；列集以 ORM/XMeta 生成的实体列为准（先抽样同族既有 CSV，如 `erp_inv_stock_move.csv` / `erp_md_partner.csv`）；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c/M1.2b/M1.4a 先例）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 干扰面预分析结论在案（零漂移预期清单或预判漂移用例清单；aps 种子值域零漂移裁决记录在案）
- [x] 15 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [x] 无悬空 FK：全部必填 FK 落在〔本批〕∪〔跨域:md·已seed〕集合内

#### Phase 1 执行证据（2026-09-02 执行会话复证）

**干扰面预分析 (a)–(g) 逐点实仓复证结论（零漂移预期，无预判漂移用例）：**

- (a) **aps 排程引擎读取面**：`ErpApsScheduleBizModel.publish`（`erp-aps-service/.../entity/ErpApsScheduleBizModel.java:44`）= require + 守卫 + 状态翻转，零排程读取（processor 豁免注册表在案）。`ErpApsSchedulingProcessor` 四类全集装载逐点核对：`loadPendingOrders`（status ∈ {DRAFT, UNSCHEDULABLE} 且 earliestStartDateT 落方案窗口）——C08 fixture 实证（`_cases/.../C08/input/13_op_order_save.json5`：machineId="1"、status=DRAFT、earliest 2026-07-10T08:00；`14_schedule_save.json5` 窗口 2026-07-10T00:00~2026-07-20T00:00；`15_schedule_forward_response.json5` 快照 `scheduledOperationIds` 仅含 `@var:opOrderId`）；种子 op 行 PLANNED×2 + CANCELLED×1 均 pending 集外 + earliest 2026-08 静态避窗 → 双保险零装载。`loadMaintenanceConstraints`——种子 constraint 窗口 2026-08-10~12 与 C08 窗口不相交 → 零装载。`loadEnabledRoutings` + `ErpApsSchedulingEngine.resolveCandidates`（javadoc 实证关联键 `isDefault=true && machineId=op.machineId`）——种子 routing machineId 7001/7002 ≠ fixture op 机器 "1"/"WC-001"（C21 实证 machineId=WC-001）→ 恒零候选。`hasOverlappingReservation`——种子预留 machineId 7001 机器维度隔离 → `ERR_APS_CAPACITY_CONFLICT` 零风险。C21 fixture 同构自建（machineId=WC-001、DRAFT）。
- (b) **gantt value spec**：`tests/e2e/dashboards/aps-schedule-gantt.value.spec.ts:19` `MACHINE_ID = '910'` 客户端过滤 + 自包含 setup（`E2E-GANTT-${Date.now()}`）——种子 machineId 7001/7002 避开 '910'/'100'/'1'/'WC-001' 全部串扰值。
- (c) **C05/C19 by-id 复证**：`TestErpC05InvLandedCost.java:134-145`（`ErpLogCarrier__save` → `ErpLogShipment__save` → `ErpLogShipment__get` by-id freight 核对，fixture 无 trackingNo）；`TestErpC19B2bAsnAutoReceiveLandedCost` fixture trackingNo=`IT-C19-TRK-001` + `@NopTestProperty` 关 webhook 签名——均 fixture 自建 by-id 消费，与种子 code/trackingNo 零 UK 冲突。
- (d) **no-row→row 激活评估**：`ext-domains-child-table.visual.spec.ts:42` `/ErpLogShipment-main` row-update-button（3 sub-grids）、`ext-domains-list-filter.visual.spec.ts:67,97` asideFilter + `/ErpLogShipmentLog-main` readonly soft-probe——DOM-className 结构断言不依赖行数，种子后激活真实行路径，Phase 3 实跑核验通过（见 Phase 3 证据）。
- (e) **logistics 批扫描休眠实证**：`_vfs/nop/job/conf/erp-log-tracking-poll.job.yaml` + `erp-log-draft-escalation.job.yaml` 均 `enabled: "@cfg:...|false"` 缺省关（实仓核验）；delivered-freight / path2 landed-cost 为 webhook/动作事件驱动非批扫描。aps 侧 `erp-aps-auto-dispatch.job.yaml` / `erp-aps-workorder-scan.job.yaml` 同为 `|false` 缺省关 + `erp-aps.auto-dispatch-enabled` 全局开关缺省 false（`ErpApsConfigs.java:38`）双门控 → 种子 dispatch_rule 2 行零消费。
- (f) **`_cases` grep 实证**：`ErpAps|ErpLog` 命中恰为 C05/C08/C19/C21 四用例 output 目录，无 findPage/totalCount 种子计数断言；纯加性插入不入既有快照（M1.2b 先例）。
- (g) **判定：零漂移预期**（无预判漂移用例）。`mvn test -pl app-erp-all` 实测与基线一致（见 Phase 3 证据），预分析成立。

**零漂移裁决记录（Decision (i)–(v)）**：

| 裁决点 | 选择值 | 考虑的替代方案 | 残留风险 |
|---|---|---|---|
| (i) operation_order 状态/窗口 | PLANNED×2 + CANCELLED×1；earliest 静态 2026-08-01/02 | 全窗口移出 vs 状态避让——**两者叠加（双保险）**：状态避让为充分条件，窗口避让兜底防未来排程窗口变更 | 若未来 C08 窗口扩至 2026-08 且 pending 集扩展含 PLANNED，需复评（当前引擎/fixture 均不含） |
| (ii) capacity_reservation / constraint machineId | 7001 | 窗口避让单独使用——叠加机器维度隔离更稳 | 无（fixture 机器 1/WC-001 恒异） |
| (iii) constraint 类型与窗口 | constraintType=MAINTENANCE（保留业务演示价值）+ 窗口 2026-08-10~12 与 C08 窗口不相交 | constraintType 改非 MAINTENANCE——放弃：MAINTENANCE 是引擎唯一读取类型，留类型改窗口更能演示真实读取面被窗口谓词过滤 | 无 |
| (iv) op_routing isEnabled | true（两行均 enabled） | 全部 false 规避装载——放弃：引擎 `resolveCandidates` 关联键 `isDefault=true && machineId=op.machineId` 使 enabled 行恒零候选（实仓核验），false 反而失真演示替代路由 | 无（关联键 machineId 维度隔离） |
| (v) dispatch_rule | 2 行（enableAuto true/false 各一），依赖双门控缺省关 | 只建 enableAuto=false 行——放弃：双门控（job enabled 缺省 false + 全局开关缺省 false）实证惰性，保留 true 行演示完整规则语义 | 生产开启 auto-dispatch 时种子规则参与派工（预期行为，非缺陷） |

**列集/格式/FK 复证**：脚本化逐表比对 CSV 列头 ↔ ORM `code=`（15 表零错列、零缺必填列）；字典码 ∈ `<dicts>` 零越界；FK 闭环 0 悬空（含 dispatch_log→op 1、booking→shipment 1/2 + window 1、window→md_partner 1、config→carrier 1、line/parcel/log→shipment 1、capacity_reservation→op 1）；行数与用例指示编码符合规格表（REMARK 后缀 (P)/(N-TERM)/(N-DIS)）。`TestErpSeedDataIntegrity` 4/4 全绿（scope-pinning + 零孤儿 CSV + 行数 > 0 + 引用完整性零悬空白名单零增量）。

### Phase 2 - 门禁常量 + owner doc 同步

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/aps/seed-data.md`、`docs/design/logistics/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：基数以「314 + 已落地批次 CSV 数」为准本批 +15，`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`
      - Skill: `nop-backend-dev`
- [x] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed +15，精确缺 seed −15）+ 新增 M1.4b 批次增量行 + 「其他扩展域交易种子」Deferred 之 aps/logistics 子集消费注记 + 快照重录义务节资产计数注记（以对账表批次链为准）
      - Skill: `nop-backend-dev`
- [x] 新建 `docs/design/aps/seed-data.md` + `docs/design/logistics/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔本批〕/〔跨域:md〕标注）、用例指示编码、negative 行语义（N-TERM 终态 / N-DIS 禁用）、干扰面零漂移设计节（含 aps 种子值域零漂移裁决）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 常量与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [x] 2 个域 seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: completed
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2

- [x] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [x] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移，按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏（C05/C08/C19/C21 优先排查）→ 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [x] 视觉快照双面义务核查：运行 `ext-domains-child-table` + `ext-domains-list-filter`（本批激活真实行路径的两 spec）+ `dashboards.visual` + `dashboards.snapshot` + `reports.visual` + `reports.snapshot`；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段；禁止单面重录
      - Skill: `nop-testing`
- [x] E2E 数值断言联动评估：`aps-schedule-gantt.value`（machineId 过滤非串扰实证）+ aps 4 个 action spec + log 3 个 action spec + `aps.smoke` + `logistics.smoke` + Phase 1 预分析标记的消费面——有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [x] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 6 张本批表（aps ≥ 2 + logistics ≥ 4 含 shipment 主子表头）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [x] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [x] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目
      - Skill: none
- [x] 独立结束审计通过后回写 roadmap 工作项 M1.4b `ready` → `done`（含批次证据摘要，格式沿 M1.1/M1.2/M1.4a 批先例）
      - Skill: none

Exit Criteria:

- [x] TestErpSeedDataIntegrity 全绿且常量断言通过
- [x] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [x] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [x] compliance checker 零漂移；日志条目在位；roadmap 状态回写完成

#### Phase 3 执行证据（2026-09-02）

1. **门禁**：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` **4/4 全绿**（scope-pinning 363 + CSV 常量 329 = 314 + M1.4b 15 + 零孤儿 CSV + 引用完整性零悬空白名单零增量）。
2. **模块回归**：`mvn test -pl app-erp-all` **70/0/0/1**——较基线 71/0/0/1 的 −1 delta 经实仓归因 = 工作树中兄弟计划 `2026-09-02-2028-1-flux-picker-schema-override` 删除的 `ErpAllFluxPagesExportTest`（HEAD 实证恰 1 个 `@Test`），非本批变更；**C05/C08/C19/C21 四用例全绿零集成快照漂移**，快照重录双面义务未触发，与 Phase 1 预分析零漂移结论对账一致。
3. **视觉快照双面义务核查**：canonical test-mode webServer（playwright config 内嵌全 `-D` flag，flux 渲染）下 **82/82 全绿**——`dashboards.visual` 10 + `dashboards.snapshot` 10 + `reports.visual` 24 + `reports.snapshot` 6 零 DOM/像素漂移（快照重录义务未触发）；`ext-domains-list-filter`（本批激活真实行路径）通过——种子后 Shipment/ShipmentLog 真实行路径激活且结构断言语义不变。
   **`ext-domains-child-table` 5 失败 = 预存红灯，非本批引入**——四重归因证据：①移除本批 15 CSV → 重建 jar → fresh-DB 重启（`ErpLogShipment__findPage total=0` 证伪种子在库）→ 同 spec 同样 5/5 失败（IDENTICAL FAILURE SETS，M1.3/M1.4a 对照实验先例）；②5 用例中 b2b/contract/drp 三域零 seed（M1.5 未执行），空列表同样失败于 `.cxd-Crud` 等待步；③DOM 探针实证 `/ErpHrTimesheet-main` 完整渲染后 `.cxd-Crud` 计数 = 0（flux 输出 `nop-crud`/`nop-table` 类）——spec 为 AMIS 遗留选择器，runbook「渲染模式与 flux 调试三路径」L117 在案迁移义务；④错误快照中页面行/分页渲染齐全（非渲染破坏）。已登记 `docs/bugs/2026-09-02-ext-domains-child-table-amis-legacy-selector-flux-preexisting-red.md`，归 flux 迁移/e2e 基建 owner 域 Follow-up。
4. **E2E 数值断言联动**：canonical 环境 15 spec **全绿**——`aps-schedule-gantt.value`（machineId='910' 过滤非串扰，种子 7001/7002 零污染）+ aps 4 action（operation-order/rush-order/schedule-toc/schedule）+ `log-shipment` + `log-delivered-freight-posting` + `log-path2-landed-cost-auto-create` + `aps.smoke` + `logistics.smoke`，零期望值基线调整。首测以 `start-app.sh` 演示语义服务器出现 4 失败（webhook 签名族）= 启动模式 artifact（该 4 spec 依赖 canonical `-Derp-log.webhook-signature-required=false` 测试 flag，runbook §启动方式 A），换 canonical webServer 后全绿。
5. **运行时装载证明**：`./scripts/start-app.sh restart`（fresh-DB）13~15s ready、装载零冲突零列映射错误；GraphQL `/r/{Entity}__findPage` 抽样 **15/15 表 total==CSV 行数**（aps 7 全量：operation_order 3 / schedule 2 / op_routing 2 / constraint 1 / capacity_reservation 1 / dispatch_log 1 / dispatch_rule 2；logistics 8 全量：shipment 2 / shipment_line 2 / shipment_parcel 2 / shipment_log 2 / carrier 2 / carrier_config 2 / delivery_booking 2 / delivery_window 2——4 组主子表头全在样）。
6. **compliance checker**：**exit 0 零新增漂移**（R2c=1542 = 机器块 1537 + 已登记 pre-existing +5 / R2b=242 = 240+2 / R12a=71 = 70+1，纯资源 + 测试常量变更预期成立）。
7. **日志条目**：`docs/logs/2026/09-02.md` 已追加（M1.4b 执行条目）。
8. roadmap 回写待独立结束审计通过后执行（见下项）。

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 fresh session `ses_f9ee7c6b9ffeNklGSQ1sIZG74D`）——0 Blocker + 1 Major + 2 Minor。Major：aps 排程引擎读取面枚举错误/不足——`ErpApsSchedule__publish` 实为纯状态翻转零读取，真实读取面 = `ErpApsSchedulingProcessor` 四类全集装载（pending OperationOrder {DRAFT,UNSCHEDULABLE}+窗口 / enabled OpRouting / MAINTENANCE Constraint+窗口 / CapacityReservation machineId+区间），fixture machineId `1`（C08）/`WC-001`（C21）+ 窗口 2026-07-10~20 未点名，DispatchRule 全表读系 config 门控缺省关——已重写基线干扰面 + Phase 1 item (a) + 扩展值域 Decision（含替代方案与残留风险）；Minor m1：prereqs 跨域锚点超集收敛为 delivery_window→ErpMdPartner 单边；Minor m2：Decision 补记录替代方案与残留风险（guide 规则 9）。全部已并入本稿。
- Independent draft review iteration 2: acceptable as-is（独立子代理 fresh session `ses_f9ed90602ffekp6D4vfvUlQjzZ`）——iteration 1 全部 3 项修订核验通过（publish 状态翻转 / Processor 四类读取面 / fixture 值 / config 门控 / Decision 扩展逐点实仓吻合），无新引入缺陷；1 Minor 行政项 = Draft Review Record 待回填（本条即回填）。共识达成，转 `active`。

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [x] 范围内行为完成（15 CSV + 常量 + 对账表 + 2 个 owner doc 段）
- [x] 相关文档对齐（seed-data.md 对账表/快照重录注记/扩展域交易种子 Deferred 消费注记 + 2 个域 seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [x] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- **seed-data.md §Non-Goals「其他扩展域交易种子（logistics/b2b/contract/drp/aps 后续批次）」Deferred 之 aps + logistics 子集**：本计划 Phase 1 全量覆盖口径消费（触发条件由 roadmap M1 全量覆盖目标取代，沿 M1.2b/M1.4a 消费先例）；消费注记由 Phase 2 回写 seed-data.md。b2b/contract/drp 子集归同批 N=2 M1.5 计划消费。
  - Classification: `consumed by this plan`（仅 aps + logistics 交易种子子集）
  - Why Not Blocking Closure: 非阻塞项——为消费登记而非遗留债务。
  - Successor Required: `no`（b2b/contract/drp 子集由 M1.5 计划消费，不归本计划）

（其余待执行期裁定：若规格表某行证伪 CSV-only 可满足性，按反松弛规则移入本节分类登记）

## Closure

Status Note: 本计划可闭包——15 个 seed CSV（aps 7 + logistics 8）按规格表逐行落地并经脚本化列头/字典/FK 门禁 + `TestErpSeedDataIntegrity` 4/4 全绿 + fresh-DB 运行时装载 15/15 表行数实证；门禁常量 314→329 与 seed-data.md 对账表（329/39）及 `_init-data/` 实数（333 CSV + 1 SQL）三方一致；2 个域 owner doc 落地且与实仓 CSV 逐行吻合；`mvn test -pl app-erp-all` 70/0/0/1（−1 delta 经实仓归因为兄弟 flux-picker 计划工作树删除的 `ErpAllFluxPagesExportTest`，非本批变更，C05/C08/C19/C21 全绿零快照漂移）；canonical E2E 82/82 全绿零 DOM/像素漂移（快照重录义务未触发）；compliance checker exit 0 零新增漂移；唯一异常发现 `ext-domains-child-table` 5 失败经对照实验（IDENTICAL FAILURE SETS）+ DOM 探针 + 零种子域同败三重归因裁决为 AMIS 遗留选择器预存红灯（先于本批存在，与种子零因果），已登记 `docs/bugs/2026-09-02-ext-domains-child-table-amis-legacy-selector-flux-preexisting-red.md` 归 flux 迁移 owner 域 Follow-up。独立结束审计 APPROVE（0 Blocker / 0 Major / 4 Minor 非阻塞，Minor 全部已落实或登记）。roadmap M1.4b 已回写 `done`。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，task id `ses_f9d9447ddffeIoqZIf6DY4hCbj`）
- Evidence: 审计报告 A..J 十项实仓核验全 PASS（15 CSV 存在性/规格区间/用例编码/ID 上限、4 表 ORM `code=` 列头脚本化全等、FK 闭环 to-one 逐边、门禁常量 329/4/363 与 `_init-data/` 334 文件构成、门禁测试独立复跑 4/4、compliance checker 独立复跑 exit 0 R2c=1542/R2b=242/R12a=71、双 owner doc 抽核逐行吻合、对账表+日志+bugs 归因、roadmap 前置状态 `ready` 正确）；4 Minor：Minor-1/2（闭包序列义务：回写 roadmap→勾门控→回填 Closure→置 completed——本 Closure 节即落实）+ Minor-3（TestErpSeedDataIntegrity 类 javadoc 过期数值 314→329，已随闭包修正，修正后门禁复跑 4/4 全绿）+ Minor-4（aps owner doc dispatch_log 编码承载列 REMARK→NOTE 措辞勘误，已落实）。

Follow-up:

- `docs/bugs/2026-09-02-ext-domains-child-table-amis-legacy-selector-flux-preexisting-red.md`——AMIS 遗留 spec 选择器 flux 化迁移（`ext-domains-child-table` 等 `.cxd-*` 存量 spec 经 EngineAdapter 翻译），触发条件 = flux 迁移/e2e 基建 owner 域下一批次；非本批引入（对照实验归因在案），不阻塞本闭包。
