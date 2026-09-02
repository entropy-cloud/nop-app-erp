# Logistics 域种子数据（Seed Data）

> Owner: 本文件（logistics 域「种子数据」owner doc；plan `2026-09-02-1415-1-m14b-aps-logistics-seed-expansion.md` Phase 2 落地）
> 上游规格: `docs/architecture/seed-data.md` 规格表 logistics 节（8 实体逐行规格权威源）+ 对账表（计数权威源）
> 业务语义 owner docs: `docs/design/logistics/`（carrier-integration / delivery-window / state-machine 等）；本文件只登记种子数据面，不重复业务语义

## 1. 范围与文件清单

M1.4b 批次（2026-09-02）为 logistics 域补齐 8 个 seed CSV（此前 logistics 为零 seed 域）。文件位于 `app-erp-all/src/main/resources/_vfs/_init-data/`，命名 = `<tableName>.csv`（`DataInitInitializer.loadCsvData` 按表名查找契约）。至此 logistics 域 8 规格实体全量 seed 覆盖（8 / 8）。

| CSV | 行数 | 主子表 | 用例指示 | 说明 |
|---|---|---|---|---|
| erp_log_carrier.csv | 2 | 头（carrier 组头） | P+N-DIS | 演示顺丰（EXPRESS，IS_ACTIVE=1）+ 停用演示货运（FREIGHT，IS_ACTIVE=0） |
| erp_log_carrier_config.csv | 2 | 子表（carrier） | P+N-DIS | carrier1 STANDARD 配置（active）+ NEXT_DAY 旧配置（N-DIS inactive） |
| erp_log_shipment.csv | 2 | 头（shipment 组头） | P+N-TERM | SHP-2026-001 IN_TRANSIT（含运单号/面单/运费）+ SHP-2026-002 CANCELLED（终态） |
| erp_log_shipment_line.csv | 2 | 子表（shipment） | P | shipment1 两行（MAT-001 ×20 + MAT-002 ×5〔跨域:md·已seed〕） |
| erp_log_shipment_log.csv | 2 | 子表（shipment） | P | shipment1 CREATE_SHIPMENT + TRACK 网关日志（成功态，静态 EXECUTED_AT） |
| erp_log_shipment_parcel.csv | 2 | 子表（shipment） | P+N-DIS | shipment1 两包裹（PKG-SHP1-001 active + PKG-SHP1-002 作废 IS_ACTIVE=0） |
| erp_log_delivery_window.csv | 2 | 头/独立 | P+N-DIS | partner1 周一上午窗（active，CURRENT_BOOKED=1）+ 周三下午窗（N-DIS inactive） |
| erp_log_delivery_booking.csv | 2 | 头/独立（FK→shipment+window） | P+N-TERM | shipment1 CONFIRMED 预约 + shipment2 CANCELLED 预约（终态；UK(shipmentId,delVersion) 每单至多一预约，故挂两不同 shipment） |

## 2. FK 闭环图

- **〔已 seed〕锚点**：`erp_md_organization`（ORG_ID=2）、`erp_md_partner`（PARTNER_ID=1 = CUST-001 华东科技 / 3 = SUP-001 北方钢铁，carrier.partnerId 与 delivery_window.partnerId 必填/可选锚点）、`erp_md_material`（MATERIAL_ID=1/2）、`erp_md_currency`（FREIGHT_CURRENCY_ID=1 CNY）、`erp_md_employee`（SHIPPER_ID=1 张三）。
- **〔本批〕链路**（logistics 4 组主子表 + 2 条跨实体链）：
  - carrier → carrier_config（CARRIER_ID 必填 to-one→carrier 1）；
  - shipment（CARRIER_ID 必填 to-one→carrier 1 + CARRIER_CONFIG_ID 可选 to-one→config 1）→ shipment_line（SHIPMENT_ID 必填 to-one→shipment 1，MATERIAL_ID 可选→md_material〔跨域:md·已seed〕）+ shipment_parcel（→shipment 1）+ shipment_log（→shipment 1）；
  - delivery_window（PARTNER_ID 必填 to-one→md_partner 1〔跨域:md·已seed〕，规格表唯一跨域必填边）；
  - delivery_booking（SHIPMENT_ID 必填 to-one→shipment 1/2 + WINDOW_ID 必填 to-one→window 1）。
- 全部必填 FK 落在〔已 seed〕∪〔本批〕集合内（`TestErpSeedDataIntegrity` 引用完整性断言门禁，白名单零增量）。

## 3. 用例指示编码与 negative 行语义

- **P（最小正例行）**：每表主行，字段值取规整演示值（运费 `.0000` 四位小数、重量 `.000` 三位小数、体积 `.000000` 六位小数、静态日期 2026-07 ~ 2026-08）。
- **N-TERM（终态行）**：shipment 2（CANCELLED）、delivery_booking 2（CANCELLED）——供业务动作非法迁移守卫负路径（发运单状态机 DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED 主链外终态）。
- **N-DIS（禁用/停用行）**：carrier 2 / carrier_config 2 / shipment_parcel 2 / delivery_window 2（`IS_ACTIVE=0` 或 `IS_ACTIVE=false` + REMARK 后缀（N-DIS）——carrier/config/parcel 的 IS_ACTIVE 为 INTEGER 列取 0/1，delivery_window 为 BOOLEAN 列取 false，逐表对齐 ORM 列型）——供启用前置守卫负路径。
- 行级用途以 REMARK 后缀（P）/（N-TERM）/（N-DIS）显式标注（沿 M1.4a 批先例），与 `docs/architecture/seed-data.md`「seed 数据分层裁决」节 negative seed 语义一致。

## 4. 干扰面零漂移设计（本批核验结论，Phase 1 预分析逐点复证）

- **C05/C19 零交集**：`TestErpC05InvLandedCost`（carrier+shipment fixture save → `ErpLogShipment__get` by-id freight 核对）与 `TestErpC19B2bAsnAutoReceiveLandedCost`（log shipment webhook 段，`erp-log.webhook-signature-required` 测试内关闭）均 `@var:` 自建实体 by-id 消费，不 findPage 种子行；fixture shipment 无 trackingNo（UK(TrackingNo,carrierId,delVersion) 唯一约束零冲突——种子两行 trackingNo 自身唯一），fixture carrier/shipment code（IT-C05-*/IT-C19-*）与种子 code 无 UK(code,orgId) 冲突。
- **批扫描面休眠实证**：`ErpLogTrackingPollJob`（扫 DISPATCHED/IN_TRANSIT 调网关 trackShipment）与 `ErpLogDraftEscalationJob`（扫 DRAFT 超 24h 派升级通知）的 `erp-log-tracking-poll.job.yaml` / `erp-log-draft-escalation.job.yaml` 均为 `enabled: "@cfg:nop.job.erp-log-*.enabled|false"` 缺省 false（实仓核验）→ 种子 IN_TRANSIT 行零被推进/零通知派发。delivered-freight / path2 landed-cost 触发为事件驱动（webhook / 动作），非批扫描。
- **E2E 消费面**：
  - `ext-domains-child-table.visual.spec.ts` 对 `/ErpLogShipment-main` 的 row-update-button 编辑抽屉（3 sub-grids: lines/parcels/logs）——种子落地后激活真实行路径（此前依赖 no-row fallback），DOM-className 结构断言不依赖行数，Phase 3 实跑核验；
  - `ext-domains-list-filter.visual.spec.ts` 对 `/ErpLogShipment-main` asideFilter（date range + status in）label 断言 + `/ErpLogShipmentLog-main` readonly row-view dialog soft-probe——同样激活真实行路径，结构断言语义不变；
  - log 3 个 action spec + `logistics.smoke.spec.ts` 自包含/渲染型，零消费种子行。
- **集成快照零漂移**：`app-erp-all/_cases` logistics 相关用例仅 C05/C19（grep 实证），均 `@var:` 自建 by-id 断言、无种子计数断言；纯加性插入不入既有快照（M1.2b 实证先例）。
- **UK 自洽**：shipment UK(code,orgId) 两行 code 独立 + UK(trackingNo,carrierId,delVersion) 两行 trackingNo 唯一；delivery_booking UK(shipmentId,delVersion) 两行分挂 shipment 1/2（同单不可重复预约契约的种子面表达）。

## 5. 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准）；省略审计列；日期 ISO（`2026-08-03`）、时间戳空格分隔（`2026-08-03 09:00:00`）、时间列（START_TIME/END_TIME/BOOKED_TIME）为 VARCHAR(8) 取 `HH:mm`；小写布尔（BOOLEAN 列）/1-0（INTEGER 列 isActive）逐列对齐；ID < 100000（各表独立自 1 起段）；JSON 文本列（REQUEST_BODY/RESPONSE_BODY/SUPPORTED_SERVICE_TYPES）取无逗号演示 JSON（带引号字段按 CSV 双写引号约定，`erp_sys_notification_template.csv` 先例）；字典码 ∈ `module-logistics/model/app-erp-logistics.orm.xml` `<dicts>`（erp-log/ 命名空间 6 字典：carrier-type / freight-terms / shipment-status / settlement-status / gateway-action / booking-status）。
- 拓扑序由 `DataInitInitializer` 按 ORM `getEntityModelsInTopoOrder()` 自动排序，同批主子表（config/line/parcel/log 晚于 carrier/shipment；booking 晚于 shipment+window）无需手工排序文件名。
