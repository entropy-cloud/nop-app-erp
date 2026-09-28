# 2026-09-28-1755-1 DOC-01 dashboards.md 采购看板两行事实漂移校正

> Plan Status: completed
> Last Reviewed: 2026-09-28
> Source: `docs/backlog/perf-ux-debt-consolidation-roadmap.md` M2 DOC-01（perf-ux mission 收官 doc-only 清理批；批次 2 日志 m-3 登记的 owner-doc 散记漂移）
> Related: `docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md`（批次 2 结束审计 m-3 漂移登记来源）；`docs/design/dashboards.md`（被校正 owner doc）
> Audit: required

## Current Baseline

- `docs/design/dashboards.md` :76（§2 采购看板汇总表「到货及时率」行）与 :266（§采购域明细表 onTimeRate 行）将比较基准写作 **`orderLine.deliveryDate`**；实仓核验（2026-09-28）：`module-purchase/model/app-erp-purchase.orm.xml` 中 `deliveryDate`（交货日期，propId=9）是**订单头实体 `ErpPurOrder`** 的列，`ErpPurOrderLine` 无此字段；实现 `ErpPurDashboardBizModel.java`（:62/:414/:437 注释与代码）口径为 `receive.businessDate ≤ 关联 order.deliveryDate`（订单头级）。→ **roadmap 指名校正 ①**（两处）。
- `docs/design/dashboards.md` :263（purchaseAmount 行）状态轴仅写 `docStatus='ACTIVE'`；实现主证据 `ErpPurDashboardBizModel.java:345`（`sumActiveInvoiceAmounts`，KPI 本体）过滤为 `approveStatus=APPROVED AND docStatus≠CANCELLED`（:116 趋势查询/:155 TopN/:372 发票头投影为同族）。状态轴等价性系 roadmap 行继承表述、本批未按状态机验证，校正照抄实现谓词原文。→ **roadmap 指名校正 ②**。
- **同行相邻漂移（本计划验证期实核新发现，同一两行内、同性质事实失准，一并校正——见 Goals 枚举）**：
  - :76 该行公式写作 `receiveDate ≤ …` 且分母为「订单数」——`ErpPurReceive` 无 `receiveDate` 字段（实核：仅 `businessDate`）；实现分母为「有 `orderId` 的收货单数」（:266 明细行自身已正确记载分母口径）。
  - :266 数据源列 `ErpPurReceive(docStatus='ACTIVE')` 缺 `approveStatus=APPROVED` 轴——实现 :418 收货单过滤同族（`approveStatus=APPROVED AND docStatus≠CANCELLED`）。
- **orderCount 轴漂移（草案审查 iteration 1 实核确认，本批不修、显式后继登记）**：:264（pur）/ :251（sal）/ :55、:74（两汇总表）四处 orderCount 行均写 `docStatus='ACTIVE'`，实现两域均为 `approveStatus=APPROVED AND docStatus≠CANCELLED` 同轴（`ErpPurDashboardBizModel.countActiveOrders` :398-403、`ErpSalDashboardBizModel.countActiveOrders` :300-303；类 javadoc :60/:54 双重佐证）。四处属跨域同族清剿面，超出「指名点轻量批」边界——登记入 roadmap §3 D-登记表（D-21）。
- 剩余差距：owner doc 与实现口径文本不一致，下游按伪事实推理（lessons/13 基线陈旧族）。

## Goals

- `docs/design/dashboards.md` 两行共 **5 处事实性校正**（全部对齐实仓实现，零业务语义变更）：
  - **:76**（到货及时率行）：①`receiveDate ≤ orderLine.deliveryDate` → `businessDate ≤ 订单头 ErpPurOrder.deliveryDate`；②分母「订单数」→「有 orderId 的收货单数」（与 :266 及实现对齐）。
  - **:263**（purchaseAmount 行）：状态轴补全为 `approveStatus=APPROVED AND docStatus≠CANCELLED`（口径说明列的「docStatus 口径」注记同步措辞）。
  - **:266**（onTimeRate 行）：③`orderLine.deliveryDate` → 订单头 `ErpPurOrder.deliveryDate`；④数据源列 `ErpPurReceive(docStatus='ACTIVE')` → `ErpPurReceive(approveStatus=APPROVED AND docStatus≠CANCELLED)`。
- 每处校正附实仓证据指针（orm.xml 行/BizModel 行），写入计划的 Proof 节。

## Non-Goals

- 不做 dashboards.md 全文复核（本批仅 roadmap 指名两行 + 同行内相邻漂移；全文审计归 document-audit 流程）。
- 不改任何实现代码、ORM、API（本批纯 doc；若实施中发现实现与 owner doc 设计语义冲突——非文本失准——登记 finding 停止并升级，不擅自改语义）。
- 不动 orderCount 四站点（:55/:74/:251/:264）：漂移已实核确认（见基线），但跨域同族清剿超出本批「指名点 + 同行相邻」边界，按反松弛规则显式移入后继所有权（roadmap §3 D-21 登记行，非无主遗留）。

## Task Route

- Type: `implementation-only change`（doc-only 事实性校正；roadmap 硬约束「仅事实性校正，不改业务语义」）
- Owner Docs: `docs/design/dashboards.md`（被校正对象）；证据源 `module-purchase/model/app-erp-purchase.orm.xml`、`module-purchase/erp-pur-service/.../ErpPurDashboardBizModel.java`
- Skill Selection Basis: 已扫描 `docs/skills/README.md` 全表——`document-audit-prompt` 面向全文审计，本批为指名点校正，无匹配技能。Skill: none

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（doc-only，无验证命令义务；Proof 为文本-代码一致性比对）

## Execution Plan

### Phase 1 - 五处校正 + 证据落盘

Status: completed
Targets: `docs/design/dashboards.md`（:76/:263/:266）、`docs/backlog/perf-ux-debt-consolidation-roadmap.md`（§3 登记表追加 D-21 一行）
Skill: none

- Item Types: `Fix`×5 + `Add`×1（同一 owner doc 两行内的文本-代码漂移 + 后继登记行）
- Prereqs: none

- [x] Fix :76 行 ①比较基准字段与实体级 ②分母口径——附 orm.xml/BizModel 证据
      - Skill: none
- [x] Fix :263 purchaseAmount 状态轴补全 + 口径注记措辞——附 BizModel:116 证据
      - Skill: none
- [x] Fix :266 行 ③比较基准 ④收货单状态轴——附 BizModel:414/:418 证据
      - Skill: none
- [x] Add roadmap §3 登记表追加 D-21 行（orderCount approveStatus 轴漂移校正，4 站点，来源=本计划实核，触发条件=下一 doc-only 批/document-audit；登记行附两处实现证据指针）——机制注记：登记表为无状态监控表面、不参与 §2 状态计数（后继载体取 §3 登记行而非 §2 新工作项行，避免破坏 roadmap「状态块归零 todo=0」完成口径；草案审查 iteration 1 建议的「§2 新增 DOC-03 todo 行」据此改道，理由记录于 Draft Review Record）
      - Skill: none
- [x] Proof 全文 grep 复核：校正后 dashboards.md 内不再出现 `orderLine.deliveryDate` / `receiveDate`（采购看板上下文）/ purchaseAmount 行缺 approveStatus 的三种失准形态；orderCount 四站点（:55/:74/:251/:264）保持原样未被触碰；git diff = dashboards.md 三处行邻域 + roadmap 登记表一行
      - Skill: none

Exit Criteria:

- [x] 五处校正 + D-21 登记行全部落盘且每处有实仓证据指针（记入本节下方）
- [x] `git diff` 面积 = dashboards.md 三处行邻域 + roadmap §3 一行，无其他改动；orderCount 四站点零触碰

Phase 1 执行证据（2026-09-28 实测）：

- :76 → `| 到货及时率 | ErpPurReceive/Order | 按期收货单数 / 有 orderId 的收货单数(businessDate ≤ 订单头 ErpPurOrder.deliveryDate) | KPI |`（①比较基准字段+实体级 ②分母口径——证据 orm.xml:546 deliveryDate 属 ErpPurOrder 头实体；ErpPurReceive 无 receiveDate 仅 businessDate:697；BizModel:423-426/:434 分母 orderId != null 计数）
- :263 → `approveStatus=APPROVED AND docStatus≠CANCELLED` + 口径注记「approveStatus+docStatus 双轴口径」（证据 BizModel:345 sumActiveInvoiceAmounts KPI 本体；:116/:155/:372 同族）
- :266 → 比较基准改订单头 deliveryDate + 数据源状态轴补全（证据 BizModel:414/:418/:437；orm.xml ErpPurOrderLine 无 deliveryDate 实证）
- grep 断言：`orderLine.deliveryDate` 0 命中、`receiveDate` 0 命中；approveStatus 新措辞 :263/:266 各命中
- orderCount 四站点（:55/:74/:251/:264）原样零触碰（grep docStatus='ACTIVE' 仍见于 :251/:264 等行）
- roadmap §3 D-21 登记行追加（含 O-2 的 :414 javadoc 顺带项与两处实现证据指针）；diff 面积 = dashboards.md 三行邻域 + roadmap 一行

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 agent_cedf1870，2026-09-28）——**Major-1**：Non-Goals「:264 是否漂移未实核」前提被证伪（pur :398-403/sal :300-303 双域 countActiveOrders 均有 approveStatus 轴，javadoc :60/:54 佐证；漂移实为 4 站点 :55/:74/:251/:264），且「纳入本批」亦不成立（跨域同族清剿超指名点边界、Proof 面重设计、先例是登记后继）→ 改判「显式后继登记」：基线增 orderCount 漂移段、Non-Goals 重写、Deferred 增条目、执行项增 roadmap 登记行；**Minor-1**：校正② 主证据指针改 :345（KPI 本体），:401 移出 purchaseAmount 同族（实为 orderCount 过滤，改挂后继登记证据）；**Minor-2**：「外延等价」降格为 roadmap 继承表述、本批未按状态机验证；**Minor-3**（不阻塞观察）：BizModel:414 javadoc「/ 总 receive 数」代码注释侧同族失准，归后继 doc 批顺带项登记于 D-21 行。修订时机制裁决一处偏离审查者原建议：后继载体取 roadmap §3 D-登记行而非 §2 新增 DOC-03 todo 行——§2 todo 行参与「状态块归零 todo=0」完成口径，新增常驻 todo 将使 roadmap 永不可完成；§3 登记表即 roadmap 为触发驱动后续设计的指定表面（反松弛规则的 explicit successor ownership 同样满足）。
- Independent draft review iteration 2: RESOLVED（同一审查代理定点复核，agent_88bb716f，2026-09-28）——iteration 1 的 1 Major+3 Minor 全部忠实落地（orderCount 四处修订相互一致且与实仓精确吻合；:345/:401 指针逐行属实；降格落地；Proof/Exit 口径互洽）；**机制改道裁决：接受**（roadmap :80 完成口径「todo=0」逐字验证——§2 todo 行参与计数、orderCount 触发驱动项落 §2 即 todo 永不可归零；:13/:47/:49 明文 §3 登记表为触发驱动后续的指定表面；反松弛四要素齐备，偏离已留痕）。非阻塞观察 3 条：O-1 标题「两行」系 roadmap「两处漂移」跨 3 行的既有措辞松弛（操作性门已全用「三处行邻域」，Closure 注记）；O-2 D-21 行文本须含 :414 javadoc 顺带项（已纳入执行项）；O-3 roadmap :44 残留「外延等价」措辞在 diff 面外，DOC-01 翻 done 后成历史注记。**计划可置 active 实施**。

## Closure Gates

- [x] 范围内行为完成（Phase 1 全部退出标准达成）
- [x] 相关文档对齐（被校正 owner doc 即本批对象；日志已更新）
- [x] 已运行验证（Proof 文本-代码一致性比对；doc-only 不适用构建/测试门——理由：零代码变更面）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### orderCount approveStatus 轴漂移（4 站点：dashboards.md :55/:74/:251/:264）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 跨域同族清剿面（pur+sal 两域），超出 roadmap DOC-01 指名两行 + 同行相邻漂移的边界；已按反松弛规则移入显式后继所有权（roadmap §3 D-21 登记行，本计划 Phase 1 追加），非无主遗留
- Successor Required: `yes`——后继载体：roadmap §3 D-21 登记行（触发条件：下一次 doc-only 清理批或 document-audit 复核 dashboards.md 时升格 plan-first）

### dashboards.md 全文事实性复核

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本批仅 roadmap 指名漂移 + 同行相邻漂移；全文级 owner-doc 复核属 document-audit 职责面
- Successor Required: `no`——document-audit-prompt 既有方法覆盖，无需专门 successor 计划

## Closure

Status Note: 五处校正 + D-21 登记行全部落盘且与实仓实现逐字/逐行吻合（审计方独立复跑 git diff/grep/实码抽查/ORM 归属全部吻合；Owner-doc→代码一致性抽样 3 断言 0 漂移）；orderCount 四站点显式后继登记（反松弛合规）；零代码变更约束满足。结束审计一轮通过（前置检查无预勾——lessons/25 三案未再发生）；Minor-1（:62 应为 :59、:423-426 覆盖 :425-426 的指针粗糙度）与 Minor-2（登记性观察）不阻塞。审计方授权置位门控与 completed。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（agent_01a494c5，fresh session，全程只读+独立复跑，2026-09-28）
- Evidence: 审计报告裁决 passes closure audit——开场前置检查 PASS（门控未预勾/证据未预填）；①git diff 恰 2 hunk 3 行逐字吻合 5 处校正、orderCount 四站点零触碰复跑；②grep 断言复跑（orderLine.deliveryDate=0/receiveDate=0）；③实码抽查（BizModel :345/:398-403/:414-437、orm.xml 三实体归属逐行属实、sal 侧 :300-303）；④roadmap D-21 四要素齐备；⑤文本一致性全过；⑥触及面无越界；⑦零代码变更（无 .java/.xml/.mjs 触碰）

Follow-up:

- （无）
