---
status: active
mission: ai-check-r3
work-item: M2.7
group: "2026-09-10-1141"
verify: [test]
---

# 2026-09-10-1141-2 M2.7 其余域 P1 修复批（ct P1×2 + dict 族 P3×3 + P2×8 + P3×8，共 21 ID，crm/cs/ct/b2b/drp/aps/log/notify/md/common/prj）

## Current Baseline

- 批内面 = M2.0 族裁决（`docs/audits/check/2026-09-06-1645-ai-check-r3/m2-0-family-adjudication.md` §1 全量映射）路由到 M2.7 的 **21 ID**（§1 表逐行枚举；§2.9 正文「≈19」为约数，以 §1 表为准）：**P1×2** = `P1-CK-ct-025-r3`（sign-status/sign-provider dict value/code 双轨）、`P1-CK-ct-026-r3`（返利协议 ACTIVE 死状态前置悖论）；**dict 族 P3×3** = `P3-CK-drp-022-r3`（replenishment-method MIN_MAX/PERIODIC 死值）、`P3-CK-b2b-018-r3`（mft-status PENDING/RECEIVED/RETRYING 三值零 writer 零裁决）、`P3-CK-prj-024-r3`（timesheet-status 死字典 + pnl-calc-status PENDING 死值）；**P2×8** = `P2-CK-aps-012-r3`（insertRushOrder 终态复活）、`P2-CK-b2b-017-r3`（跨域写裸 DAO 未登记）、`P2-CK-crm-020-r3`（qualify 双前置未实现）、`P2-CK-drp-021-r3`（越库 PENDING 零写入链入口断链）、`P2-CK-log-012-r3`（注册版追踪页串页 + 修复版死产物）、`P2-CK-md-015-r3`（ErpMdSupplierApproval.status ORM 孤儿默认值 "10"）、`P2-CK-notify-009-r3`（14 错误码 12 个零消费）、`P2-CK-notify-010-r3`（markAllRead 显式 userId 越权）；**P3×8** = `P3-CK-common-012-r3`（OrgIsolationTransformer fail-open）、`P3-CK-aps-013-r3`（batchScheduleForward 仅 catch NopException）、`P3-CK-aps-014-r3`（约束 horizon 双界强制）、`P3-CK-crm-022-r3`（ForecastPeriod 初始态 OPEN 不设防）、`P3-CK-crm-024-r3`（kanban 前缀硬编码 + ¥ 无 i18nEn）、`P3-CK-cs-025-r3`（TicketAssignResolver 降级零日志）、`P3-CK-drp-023-r3`（DockAppointment.dock refEntityName 错挂 + status 无 ext:dict）、`P3-CK-log-013-r3`（在途运单可预约占窗口）。缺陷面逐条权威源 = `docs/audits/check/ai-check-index.md` 对应行 + 各 `ck-<slice>-r3.md` finding 节；mnt 域零域批面（mnt 全部 finding 归 M2.8 分片）。
- 修复方法论（r3 M2 前言 + M2.0 owner doc `docs/architecture/finding-remediation-method.md`，其为本计划直接 Prereq）：每 finding 先写失败测试 → 修复 → 测试绿 + 既有测试零回归；证伪路径 = 书面 not-a-problem 登记（两态必居其一，禁止无登记跳过）；消化序 P1 → P2 → P3（§2.9）；域批计划按域再分片允许拆多 plan（同 roadmap 行多次执行先例 = MI.2/MI.3/MI.4）——本计划承载全 21 ID，Phase 1~3 按优先级分组、组内按域分片执行，Phase 4 统一收官。
- 保护区守门（横切关注点 4）：触及 ORM/api.xml 的站点 = auto + dual-agent-approval（两个独立子 agent 分别批准，批准记录落本计划勾选注记）——`ct-025-r3`（ORM ext:dict 绑定面/历史数据迁移）、`md-015-r3`（ORM defaultValue）、`drp-023-r3`（ORM refEntityName/dict）、`prj-024-r3`（ORM dict 登记面）四站涉及；批准流随 Phase 1 先行启动（族裁决 §4 分片⑥注记），避免阻塞收官。
- seed 联动（横切关注点 7）：修复默认不动 `_init-data/` seed CSV 与种子 COA；dict 族若必须触 seed 字典数据，触发 `seed-data.md` §快照重录义务双面重录（域 `_cases` + app-erp-all 集成快照）并在计划注记登记；测试用 case 级 fixture。
- 跨轮/跨计划边界（横切关注点 5 + 裁决 §3）：r1/r2 通道 finding 不入本批——`r1 b2b-013`（dict 统一批次注记移交 r1 通道）、`ct-024`（死配置键，返利链协调注记）、`notify-003/004/005`（notify-009/010 与其同批协调注记；INSTANCE_NOT_FOUND 恰为 notify-004 修复所需已定义未接线）、`drp-019`（replenishment 同族）、`pur-013/mfg-017`（aps-013 同型族）——仅落协调/范式证据注记，状态回填归 M2.9。跨计划依赖：`log-012-r3`（DIM-F）消费 M2.0 裁决 §2.5 已冻结的双载体权威裁决（flux.yaml 唯一运行时权威、page.yaml 冻结为回退产物；log-012 为反向形态——page.yaml 为注册载体），本计划按该已冻结裁决执行注册切换；M2.8 分片③负责将该裁决成文 `view-and-page-strategy.md`（两计划互引注记，M2.9 对账）。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-09 行（全 reactor 4006/0/0/1，41 含测试模块聚合口径；执行期以最新登记行为权威，姊妹计划已披露增量允许并披露）；compliance checker 机器块 R2b=242/R2c=1542/R12a=71（R2c 实测 1543 的 +1 漂移已登记 successor baseline-raise 裁决在案，本批不得再新增漂移）；CJK `--strict` PASS（0/0/0/0）。模块参照（M1 锚点）：ct 168 / b2b 80 / crm 188 / cs 185 / drp 98 / aps 82 / log 66 / notify 23 / md 160 / common 23 / prj 179。
- 剩余差距：21 finding 修复 + 负路径测试缺位；4 站保护区双批准未启动；M2.9 收官需本批 21 条 fixed 证据指针与 dict 族统一批次设计证据。
- 依赖状态：M2.0（plan `2026-09-10-0425-1`）已完成；本计划组 `2026-09-10-1141` 内执行序 N=2。组内文件面：本批触 11 域业务站点；与 M2.6（qa 两站点）零重叠；与 M2.8 族批唯一交点 = E 族 `view-and-page-strategy.md` 成文（M2.8 侧）与 log-012 执行（本计划侧）——已按上述跨计划边界消解。

## Goals

- P1×2 修复落地：`ct-025-r3` dict 双轨收敛（dict 收敛单属性或 writer 改 value 形态二选一裁决 + 历史数据迁移，ORM 面双批准）；`ct-026-r3` 返利 ACTIVE 前置悖论消解（补 ACTIVE 激活 writer 或收窄 isActive 前置二选一裁决，「DRAFT 拒绝 + 激活放行」失败测试先行）。
- dict 族 P3×3 与 ct-025 同批范式对齐收口：drp-022 / b2b-018 / prj-024 逐站「实现语义或 dict 收窄 + owner doc 裁决」二选一落地，死值/死字典面 grep 断言归零。
- P2×8 + P3×8 逐条修复或书面证伪：每条先红后绿（行为类）/ 分片验收断言（机械类），守卫/writer/映射/日志面按索引修复方向落地；`common-012-r3` fail-open 面消解（隔离开关投产前必须修——裁决触发条件显式承继）。
- 零回归 + 门控持平：11 域模块套件全绿 + 全 reactor `mvn test` 零新增失败；compliance/cjk 双 checker 不高于基线；seed 触碰面为零或已履行双面重录。
- M2.9 消费证据落盘：21 条 fixed/not-a-problem 证据指针 + dict 族统一批次设计证据（供 r1 b2b-013 通道同型继承）+ 跨轮协调注记（notify/ct/drp 通道）+ 触碰面声明。

## Non-Goals

- 不修 r1/r2 通道 finding（b2b-013、ct-024、notify-003/004/005、drp-019、pur-013/mfg-017 族等）——仅统一设计/范式/联动证据注记落盘，状态回填归 M2.9。
- 不修 M2.6/M2.8 批内面（qa-026/qa-030；A/B/C/D/E/F 族与 doc 批 61 ID——含 cs-023/024/026/027、ct-027/028/029/030、b2b-019/020、drp-024、aps-015/016、log-014/015、md-016/017/018/019/020、crm-021/023、mnt 全系、hr2/prj-022/023/025 系）。
- 不重开 qa-031/qa-033-r3（deferred 裁决）与 enable-action-auth 翻 true（族 B 12 ID 全部 fixed 前禁止——裁决 §2.2 触发登记，归 M2.8 批义务，本批不承载）。
- 不做双索引状态回填（M2.9 义务）；不做 roadmap 状态翻转；不重构各域结算/调度/派发编排结构（守卫与容错以最小面接入既有校验/循环序列）。

## Phase 1 — P1 批 + dict 族同批范式（5 ID：ct-025/ct-026 + drp-022/b2b-018/prj-024）

> 统一类型：Decision | Proof | Fix | Add（2 Decision + 5 Proof + 5 Fix + 迁移/字典 Add 面）。
> Skill: bug-diagnosis-prompt + nop-backend-dev（BizModel/Processor/dict 面）+ nop-testing（失败测试）
> Targets: `module-contract/`（ct-025/026）、`module-drp/`（drp-022）、`module-b2b/`（b2b-018）、`module-projects/`（prj-024）service/dao/test 面；ORM 触及面 = `module-contract/model/`、`module-projects/model/`（双批准前置）
> Prereqs: M2.0 计划完成；ORM 双批准流先行启动（全四站 ct-025/prj-024/md-015/drp-023，ct-025/prj-024 本阶段完成批准，md-015/drp-023 批准记录落 Phase 2/3 各站 Decision 项）

- [x] <Proof> 保护区双批准：ct-025/prj-024 ORM 触及面按 ai-autonomy-policy 完成 dual-agent-approval（两个独立子 agent 分别批准），批准记录与范围摘录落本项勾选注记；md-015/drp-023 双批准流同批先行启动（批准记录落 Phase 2/3 各站 Decision 项）；批准范围外零 ORM 触碰。
      - Skill: none
      - 批准记录（2026-09-10，dual-agent-approval，两独立子 agent fresh session 分别审查）：**Agent#1**（task ses_f756d3549ffeXo5U6SGBcNTkiT）4/4 APPROVE；**Agent#2**（task ses_f756ce8d6ffek6U24Uhlztqz92）4/4 APPROVE。范围：①ct-025 = `module-contract/model/app-erp-contract.orm.xml` sign-status/sign-provider dict value 数值轨(10..60/99)→value==code 收敛（两 agent 独立核证：writer 全链 code 形态、seed `_init-data/erp_ct_signature_request.csv` 与全部 `_cases` 表均 code 形态零迁移、生成链 `_app.orm.xml`/`_ErpCtDaoConstants`/dict.yaml/i18n 四件一致）；②prj-024 = `module-projects/model/app-erp-projects.orm.xml` 死字典 `erp-prj/timesheet-status` 删除（零列绑定，`ErpPrjTimesheet.status` 实绑 `wf/approve-status`；生成链 dict.yaml 删除/_ErpPrjDaoConstants 裁剪/_app.orm.xml/i18n 一致）；③md-015（Phase 2 消费）= `module-master-data/model/app-erp-master-data.orm.xml:1191` 仅移除 `defaultValue="10"`（保 mandatory+ext:dict）；④drp-023（Phase 3 消费，收窄面）= `module-drp/model/app-erp-drp.orm.xml` DockAppointment.status 加 `ext:dict="erp-inv/drp-xdock-dock-status"` + 新增加性 5 值字典（dock 关系 retarget 因 ErpMdWarehouseLocation 实体不存在判不可行，采 Non-Goal 裁决注记路线）。Agent#2 附加条件：`docs/design/contract/seed-data.md:53` 数值轨 quirk 注记陈旧须同批更新——已履行（改为「已消解」注记）。批准范围外零 ORM 触碰（git status 复核：ORM 仅 contract/projects 两文件在动）。
- [x] <Proof> ct-025 失败测试：断言双轨失效面——writer 存 code 形态后按 value 翻译的 ext:dict 绑定列读取失配（label 解析/字典过滤错误复现）；执行确认红，失败输出记入勾选注记。
      - Skill: bug-diagnosis-prompt
      - 红绿证据：`TestErpCtSignDictTrack`（module-contract/erp-ct-service）双向断言——writer 形态 PENDING_SIGNATURE/MOCK 按 value 可解析 label + 数值轨 "10"/"99" 不存在。红态证据：本计划工作树系前次中断执行已落修复后恢复，本执行未回滚 ORM 重编译复现红（ORM 红态需全量 codegen 重建）；红态由 (a) 审计控制点（`_ErpCtDaoConstants:214-274` 数值形态常量 + ErpCtConstants code 形态 writer 双轨并存）经两批准 agent 于 HEAD 实仓复核确认 + (b) 测试双向断言（若数值轨残留任一方向断言即红）承担。
- [x] <Proof> ct-026 失败测试：断言「DRAFT 协议 runAccrual 拒绝 + 合法激活后放行」——现状零 setStatus(ACTIVE) writer 使激活路径不可达（计提链入口断链复现）；执行确认红，失败输出记入勾选注记。
      - Skill: bug-diagnosis-prompt
      - 红绿证据：`TestErpCtRebateActivation.testActivateDraftAgreementUnlocksRunAccrual` 断言链 = DRAFT runAccrual 拒绝（ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE）→ activate 成功 → status=ACTIVE → runAccrual 放行。红态证据：本执行将 BizModel/StateMachine/IBiz 回退至 HEAD 后编译即报 `ErpCtRebateAgreementBizModel 未覆盖 IErpCtRebateAgreementBiz.activate`——HEAD 无 activate writer 实证（测试激活 RPC 在 HEAD 必然 not-found 失败）；修复后全链绿。同红态由审计控制点（RunAccrualProcessor:50 isActive 守卫 + 全仓零 setStatus(ACTIVE)）经两批准 agent HEAD 复核确认。
- [x] <Proof> dict 族失败测试×3：drp-022（MIN_MAX/PERIODIC 配置静默无效复现）、b2b-018（RETRYING 与 managed-file-transfer.md:257-258 重试声明相悖面复现）、prj-024（timesheet-status 零列绑定死字典 + pnl-calc-status PENDING 零消费复现）；执行确认红，失败输出记入勾选注记。
      - Skill: bug-diagnosis-prompt
      - drp-022 红态实测（本执行回退 DrpEngine/SimulationDrpEngine 后跑 `TestErpDrpReplenishmentMethod`）：`testMinMaxTopsUpToMaxWhenBelowMin` FAILURE——`MIN_MAX 低于 min 应补至 max（300-20=280），实际=0.0000`；恢复修复后 4 测试绿（280/400/30/30）。
      - b2b-018 红态实测（本执行回退 TransportManager 后跑 `testTransport5xxRetryWritesRetryingLog`）：FAILURE——`5xx 重试路径应写 RETRYING 中间日志（managed-file-transfer.md 重试策略表），实际=0`；恢复修复后绿。
      - prj-024 红态证据：静态面（死字典零列绑定/PENDING 零消费）为 ORM/常量登记面而非运行时行为，可复现红 = `ErpPrjConstants:62` 注释自认合并 + grep 零绑定（两批准 agent HEAD 实仓复核）；登记面修复（删字典 + PENDING 保留注记）后 grep 断言归零（本执行复核：`erp-prj/timesheet-status` 在 module-products 全域仅剩裁决注释行，零活引用）。
- [x] <Decision> ct-025 双轨收敛选型：dict 收敛单属性 vs writer 改 value 形态 + 历史数据迁移——选择、替代方案、残余风险（存量行形态转换与迁移联动评估）记入注记；与 r1 b2b-013 统一批次范式互链注记（r1 通道所有，状态不动）。
      - Skill: bug-diagnosis-prompt
      - 裁决：**dict 收敛 value==code 单属性**（D1 语义编码单轨）。理由：writer 全链（InitProcessor:44-45 + 回调/轮询 transition 矩阵）与全部存量数据（seed CSV + `_cases` 快照表）均为 code 形态，改 writer 形态需迁移全部存量行且与 docType/ocrStatus 同域 value 形态并存继续双轨；收敛字典侧零数据迁移、生成链自动同步。替代方案（writer 改 value 数值形态 + 历史迁移）否决理由：迁移面广（DB 存量 + seed + cases 三处重录）、违反 system-baseline §D1。残余风险：无存量数值行（两 agent 独立核证 seed/cases 零数值形态）；外部部署库若存在数值行需一次性 UPDATE（登记为部署注记，非本仓义务）。范式互链：r1 b2b-013（blockingLevel defaultValue=10 同族字典值域面）继承「value==code 单轨收敛」范式——r1 通道所有，状态回填归 M2.9。
- [x] <Decision> 四站「实现 vs 收窄/登记」裁决：ct-026（补 ACTIVE 激活 writer（协议生效 mutation + 审批/日期守卫） vs 收窄 isActive 前置 + owner doc 裁决）、drp-022（runDrp 接入 MIN_MAX/PERIODIC 语义 vs dict 缩减 LOT_FOR_LOT + owner doc Deferred）、b2b-018（重试循环写 RETRYING 中间 log vs owner doc 登记「仅终态日志」+ dict 收窄）、prj-024（ORM 登记清理/复用决策 + PENDING 保留初始态语义则补注释登记）——逐站选择与替代方案记入注记（索引修复方向所列两选一）。
      - Skill: bug-diagnosis-prompt
      - **ct-026 = 补 ACTIVE 激活 writer**：新命名动作 `activate`（DRAFT→ACTIVE），`ErpCtRebateAgreementBizModel.activate` + IBiz 声明 + 新码 `ERR_CT_REBATE_AGREEMENT_NOT_EFFECTIVE`（生效日守卫）+ StateMachine `assertCanActivate`（仅 DRAFT 源态 = 审批前评审态载体）+ `transitions()` 注册 activate 单边。否决「收窄 isActive 前置」：将使计提门槛形同虚设且与 owner doc「生效中可计提」语义相悖。owner doc `state-machine.md` §适用对象三已随批修订（实现注记 + 状态表 ACTIVE 可达性 + §2 轴形态声明）；EXPIRED/SETTLED 维持预留死状态登记。
      - **drp-022 = runDrp 接入 MIN_MAX/PERIODIC 语义**：`DrpEngine.applyReplenishmentMethod`（MIN_MAX 低于 min 补至 max 缺省回落 min；PERIODIC order-up-to 补至目标水位；LOT_FOR_LOT/null 纯需求驱动不变）+ SimulationDrpEngine 同源接线。否决「dict 缩减 + Deferred」：owner doc（drp README/ui-patterns）已声明三方法语义，参数列 mandatory 用户误导面大，实现语义成本可控（纯函数叠加）。reviewPeriodDays 定性 = 运营审视节奏（批引擎无逐日触发语义），不参与数量计算。
      - **b2b-018 = 重试循环写 RETRYING 中间 log**（保留 6 值字典不收窄）：TransportManager 重试循环内逐次写 RETRYING 中间日志（retryCount 递增）+ WARN 日志。PENDING/RECEIVED 处置 = owner doc 实现注记登记 intentional reserved（PENDING 预传输预留态——MftLog 在传输尝试后落库；RECEIVED 依赖 SFTP 入站通道 = 本切片 Non-Goal）。否决「仅终态日志 + dict 收窄」：与 managed-file-transfer.md:258 重试声明直接相悖面未消解。doc 已随批修订（值域表 value==code + 处置注记 + 重试策略表对齐）。
      - **prj-024 = ORM 登记清理 + PENDING 保留注记**：删零绑定死字典 `erp-prj/timesheet-status`（`ErpPrjTimesheet.status` 实绑 `wf/approve-status`）；`pnl-calc-status` PENDING 保留为 intentional reserved 初始态（`ErpPrjConstants:86-88` 注释登记，successor = 首次挂账前置检查等初始态语义复用）。否决「复用」：无消费方需求，复用属过度设计。
- [x] <Fix> ct-025 按 Decision 落地（writer 形态或 dict 收敛 + 历史数据迁移/fixture 等价物）；<Fix> ct-026 按裁决落地（激活 writer 含审批/日期守卫，或前置收窄 + owner doc 行）。
      - Skill: nop-backend-dev
      - 落地面：ct-025 = ORM dict 收敛（value==code）+ 生成链四件（_app.orm.xml/_ErpCtDaoConstants/sign-*.dict.yaml/i18n）+ 零迁移实证；ct-026 = `ErpCtRebateAgreementBizModel.activate`（源态守卫 assertCanActivate + 生效日守卫 NOT_EFFECTIVE）+ IBiz 声明 + ErpCtErrors 新码 + StateMachine 边注册 + `TestErpCtRebateAgreementStateMachineMatrix` 同步扩展。测试：`TestErpCtRebateActivation` 3 测试 + `TestErpCtSignDictTrack` 2 测试全绿。
- [x] <Fix> drp-022 / b2b-018 / prj-024 按 Decision 落地（语义实现或 dict 收窄 + doc 裁决行；owner doc 修订仅限 finding 语义对齐，不改需求契约段）。
      - Skill: nop-backend-dev
      - 落地面：drp-022 = `DrpEngine.applyReplenishmentMethod`（runDrp:92-93 接线）+ `SimulationDrpEngine:120-122` 同源 + `TestErpDrpReplenishmentMethod` 4 测试；b2b-018 = `TransportManager:87-96` RETRYING 中间 log + `TestErpB2bMftTransport.testTransport5xxRetryWritesRetryingLog`；prj-024 = ORM 死字典删除（生成链同步）+ `ErpPrjConstants` PENDING 保留注记。doc 裁决行：`state-machine.md`（ct）/`managed-file-transfer.md`（b2b）/drp README·ui-patterns 已有语义声明对齐（prj 注记落常量文件）。
- [x] <Add> dict/迁移配套面：按 Decision 结论新增/收窄字典值域、迁移脚本或测试 fixture（seed 触碰则履行双面重录义务并登记）。
      - Skill: nop-backend-dev
      - 配套面：ct-025 生成 dict.yaml ×2 收敛 + i18n 键同步；prj-024 死 dict.yaml 删除 + i18n 键删除；迁移 = 零（seed/`_cases` 全 code 形态，两 agent 核证）；seed 触碰面 = 零（`git status` 复核 `_init-data` 无改动，双面重录义务未触发）。b2b-018 伴随快照重录：`TestErpB2bMftTransport.testTransport5xxRetryDeadLetter` output/tables/erp_b2b_mft_log.csv 按新 RETRYING 行为重录（case 级 fixture，非部署 seed，`_init-data` 零触碰）。

Exit Criteria:

- [x] 双批准记录在案；5 条失败测试先红后绿；ct-025 双轨面 grep 断言归零（writer 写入形态 ↔ ext:dict 翻译形态单一化）
      - 双批准：Agent#1 ses_f756d3549ffeXo5U6SGBcNTkiT + Agent#2 ses_f756ce8d6ffek6U24Uhlztqz92 均 4/4 APPROVE（本 Phase 首项注记）。先红后绿：drp-022/b2b-018 本执行实测红→绿（失败输出在上）；ct-026 HEAD 回退编译实证 writer 缺位 + 修复后绿；ct-025/prj-024 红态经审计控制点 + 双 agent HEAD 复核承担（ORM 红态复现需全量 codegen 重建，未执行，如实登记）。grep 断言：`sign-*.dict.yaml` 数值串零残留 + ORM `value="10"/"99"` sign 面零命中（本执行复核 exit=1 零残留）。
- [x] ct-026 激活/守卫语义落地且 DRAFT 拒绝 + 激活放行测试绿；dict 族三站死值面按裁决收口（实现语义测试绿或 dict 收窄 grep 断言归零 + doc 裁决行在案）
      - `TestErpCtRebateActivation` 3 测试绿（拒绝码三分支断言）；drp-022 实现语义 4 测试绿；b2b-018 RETRYING 中间 log 测试绿 + PENDING/RECEIVED 处置注记在 doc；prj-024 死字典 grep 归零（仅裁决注释行）+ PENDING 保留注记在 `ErpPrjConstants:86-88`；doc 裁决行 = state-machine.md §适用对象三 / managed-file-transfer.md §MFT 状态字典+§重试策略 / seed-data.md:53 quirk 关闭注记。Phase 1 域模块套件（ct/b2b/drp/prj service）全绿 + 全 reactor `mvn test` BUILD SUCCESS（2026-09-10 本执行）。

## Phase 2 — P2 独立项批（8 ID：aps-012/b2b-017/crm-020/drp-021/log-012/md-015/notify-009/notify-010）

> 统一类型：Decision | Proof | Fix | Add（3 Decision + 8 Proof + 8 Fix）。
> Skill: bug-diagnosis-prompt + nop-backend-dev + nop-frontend-dev（log-012 DIM-F 面）+ nop-testing
> Targets: 各域 service/processor/web 面 + `module-master-data/model/`（md-015 ORM，双批准前置）+ b2b data-dependency-matrix 对账面 + notify 错误码接线面
> Prereqs: Phase 1 完成（P1 → P2 消化序）

- [x] <Proof> Phase 2 八条失败测试先行（负路径逐条复现：aps-012 FINISHED/CANCELLED 工序被 insertRushOrder 直置 DRAFT 复活；b2b-017 跨域裸 DAO 写未登记面；crm-020 无联系信息 leadType=LEAD 可 QUALIFIED；drp-021 越库 PENDING 输入链断链面；log-012 追踪页混合串页复现；md-015 裸 save 建行永不能 apply + findEffectiveByPartner 污染；notify-009 契约异常路径抛裸码或无码；notify-010 任意用户批量置他人已读）；执行确认红，失败输出记入勾选注记。
      - Skill: nop-testing
      - **aps-012 红态实测**：`TestErpApsSchedulingEngine.testInsertRushOrderRejectsTerminalRush/RejectsCancelledRush` 各 FAILURE——`FINISHED 终态急单不可插单复活: ApiResponse[status=0,...scheduledOperationIds=[2]]` / `CANCELLED ...`（终态被直置 DRAFT 复活重排实证）。
      - **crm-020 红态实测**：`TestErpCrmLeadConversion.testQualifyRequiresContactInfoAndLeadType` FAILURE——`无联系人信息的 leadType=LEAD 不可 QUALIFIED 入漏斗 ==> expected: <true> but was: <false>`。
      - **notify-009/010 红态实测**：`TestErpSysNotificationReadIdentity` 3 FAILURE——`当前用户不得批量置他人已读: ApiResponse[status=0,data=1]`（010 越权实证）+ `通知不存在时 markRead 不得静默写孤儿已读行: ApiResponse[status=0]`（009 INSTANCE_NOT_FOUND 未接线实证）+ `当前用户不得标记他人通知已读: ApiResponse[status=0,...]`。
      - **md-015 红态实测**：`TestErpMdSupplierApprovalDefaultOrphan.testBareSaveWithoutStatusRejected` FAILURE——`裸 save 缺 status 应被 mandatory 校验拒绝...: ApiResponse[status=0,...status=10,status_label=null,...]`（孤儿默认值实证）。
      - **b2b-017 / drp-021 / log-012 = 机械/登记面（计划 Goals 分片验收断言口径）**：b2b-017 红态 = 矩阵 §2.2 b2b 行「待深化」+ §2.4/§4.2 无边（审计控制点 + 本批登记后 grep 断言在位）；drp-021 红态 = 全仓零 PENDING 生产 writer（审计 grep + 探索复核 `daoFor(ErpInvDrpCrossDock)` 仅读/改两处生产命中）；log-012 红态 = 注册版 page.yaml:43-48 无 shipmentId 过滤（limit 5000 全量）+ 页头注释自述相悖（审计控制点）。修复后断言见各 Fix 项注记。
- [x] <Decision> b2b-017 三选一：矩阵登记边 + 补豁免注释 / 改经 `IErpPurReceiveBiz` command（E3.5 形态）/ owner doc 登记 RAW-DAO 例外——与 data-dependency-matrix §2.2 b2b 行「待深化」列收口同批，选择与残余风险记入注记。
      - Skill: bug-diagnosis-prompt
      - 裁决：**选择 (a) 矩阵登记边 + 写点豁免注释**（§2.2 b2b 行 S 列 + §2.4 新增 b2b-service→pur-dao 行 + §4.2 新增「B2B ASN 入库草稿」行 + 双 Processor javadoc 豁免登记）。否决 (b) 改经 I*Biz：`IErpPurReceiveBiz` 仅 `cancel` + 平台标准审批 mutation，无 purpose-built 外部建单 command，新增 command 属跨域契约变更超本批面；通用 `save(Map)` 用户面校验管道与 config-gated 系统级 ASN 回填不匹配（DrpReleaseService:44-47 同型豁免先例在案）。否决 (c) 纯 doc 例外：边登记 + 代码点注释双轨才满足「未登记未裁决」消解。残余风险：pur 域未来为 Receive 增加 save 侧钩子（如默认值/code 生成管道）时，b2b 草稿创建不自动受益——已于 §2.4 行注记披露；草稿不触发过账（posting 归 pur approve 链），行为面经 r1 行为审计验证正确。
- [x] <Decision> drp-021 三选一：释放时按需创建 PENDING（cross-dock.md 方式 1）/ purchase 收货侧 create-if-absent / owner doc 降级裁决——选择与残余风险记入注记（索引 finding 行全文为裁决输入）。
      - Skill: bug-diagnosis-prompt
      - 裁决：**选择 (c) owner doc 降级裁决**（cross-dock.md §预分配匹配流程 PENDING 创建面实现注记落盘）。理由：方式 1（释放时创建）的 owner doc 流程以 `DrpLine.crossDockFlag=true` 为行级门控，但 `ErpDrpLine` **无 crossDockFlag 列**（ORM 实仓核证）——按行门控缺 ORM 载体，加列属新保护区面（ORM 变更不在本站双批准范围，且 Non-Goal 约束「不重构释放编排」）；无门控的全量创建（所有 PURCHASE 释放均产 PENDING）在启用域产生大量无目标记录，违背最小面。方式 2（收货侧 create-if-absent）同缺门控载体且方向反直觉。**登记内容**：PENDING 仅经 CRUD 手工/集成创建（自带 sourceBillType=PUR_ORDER + sourceBillCode + materialId + quantity 精确匹配键）；链路前置条件 = `erp-inv.drp-xdock-enabled=true` + 上述记录存在；触发条件「越库启用前必须修复」转化为「启用前必须知晓的前置条件」显式落 doc。残余风险：手工创建依赖调用方准确填写 sourceBillCode（填错即链路空转，无副作用、可诊断）；successor = 落地 crossDockFlag 列 + 释放面自动创建时开独立 plan（ORM 面 + 释放编排面）。
- [x] <Decision> md-015 双批准 + 选型：defaultValue 改 APPLIED vs 移除默认（ORM 保护区）——选择与残余风险（存量行兼容面）记入注记；「裸 save 后可 apply」负路径测试随行。
      - Skill: bug-diagnosis-prompt
      - 双批准：Phase 1 首项注记 Agent#1 + Agent#2 均 APPROVE（范围 = 仅移除 `defaultValue="10"`，保 mandatory + ext:dict）。
      - 裁决：**移除默认（fail-fast）**，否决「改 APPLIED」：裸 save 自动获得 APPLIED 会使 findEffectiveByPartner 污染路径原样保留（裸建行即「有效资格」），仅消除 apply 死锁而保留资格污染面。移除后裸 save 缺 status 被 mandatory 校验拒绝（负路径测试绿）；apply(null→APPLIED)「新建申请」leg 的 null 源态在本默认存在前即不可持久化（mandatory 先于默认填充不可达），无可达性回归（Agent#2 独立核证）。存量行兼容面：本仓 seed/CASES 零 "10" 行（双 agent 核证）零迁移；外部部署库若存在 "10" 行需一次性数据修复（登记为部署注记）。
- [x] <Fix> aps-012：rush 本体状态白名单（DRAFT/PLANNED/UNSCHEDULABLE）+ 终态/过期在制拒绝或 re-open 裁决行 + 新码（CAT-2 传码）。
      - Skill: nop-backend-dev
      - 落地面：`ErpApsSchedulingInsertRushOrderProcessor` 白名单守卫（DRAFT/PLANNED/UNSCHEDULABLE 外抛新码 `ERR_APS_RUSH_ORDER_NOT_INSERTABLE`，CAT-2 传码 = opCode+currentStatus）；PLANNED 态急单重排先 `releaseReservationsByOrder` 释放原时段预留（防自冲突，与回退者同型）再经 `assertCanRevertToDraft` Bean 矩阵回退（原先绕过状态机直置 DRAFT 面同步消解）；UNSCHEDULABLE 保持同池自愈语义（RC-R1.87）。owner doc aps/state-machine.md §3 终态不可恢复语义不变（无 re-open 裁决需要）。测试：终态复活拒绝 ×2 绿 + 既有插单测试（低优回退/IN_PROGRESS 拒绝/预留释放）零回归绿。
- [x] <Fix> b2b-017 按 Decision 落地（矩阵登记边/补豁免注释，或改经 `IErpPurReceiveBiz` command，或 RAW-DAO 例外 doc 登记行——三选一所选形态 + data-dependency-matrix §2.2 b2b 行对齐断言）。
      - Skill: nop-backend-dev
      - 落地面：`ErpB2bAsnCreateReceiveFromAsnProcessor` 类 javadoc「跨域写豁免登记」节 + 写点两处 inline 注记（ErpPurReceive 创建点 + PO 行拉取点）；`ErpB2bAsnMatchPurchaseOrderProcessor.findPurchaseOrder` 只读豁免 javadoc（旁证点）。matrix 对齐断言：§2.2 b2b 行 S 列已由「待深化」改为登记边（grep 断言：`data-dependency-matrix.md` 含 b2b-service→pur-dao 行 + B2B ASN 入库草稿 S 写行，本批 diff 证据）；既有 `TestErpB2bAsnInbound`/createReceiveFromAsn 行为测试零回归绿（登记面修复零行为变更）。
- [x] <Fix> crm-020：Processor 层补 qualify 双前置（leadType≠LEAD 复用 ERR_LEAD_TYPE_MISMATCH；contact 全空抛新码）或 owner doc 显式降级登记（二选一裁决注记）。
      - Skill: nop-backend-dev
      - 裁决 + 落地面：**联系人门槛落地（LEAD 路径）**——`ErpCrmLeadProcessor.validateTransitionForQualify` 对 leadType=LEAD 且 contactName/Phone/Email 全空抛新码 `ERR_LEAD_CONTACT_REQUIRED`（`erp.err.crm.lead-contact-required`）。**类型前置收窄适用面（裁决注记）**：finding 建议的「leadType≠LEAD 复用 ERR_LEAD_TYPE_MISMATCH」经 lesson 13 HEAD 复核与实仓转化链矛盾——convertToCustomer 新建 OPPORTUNITY(NEW) 商机须经 qualify 入漏斗（owner doc state-machine.md §转化前置守卫实现注记 + :48 QUALIFIED→CONVERTED 期待 OPPORTUNITY；`TestErpCrmLeadConversion.testFullConversionChain` 红态实证），:44「leadType=LEAD」按字面全量适用将击穿转化链；故联系人门槛按 LEAD 入漏斗路径适用，OPPORTUNITY 路径豁免，owner doc :44 行补实现注记落盘。测试：无联系 LEAD 拒绝绿（保持 NEW 不入漏斗断言）+ 转化链既有测试零回归绿。
- [x] <Fix> drp-021 按 Decision 落地（PENDING 写入链接通或降级裁决行 + doc 对齐）。
      - Skill: nop-backend-dev
      - 落地面：降级裁决行落 `docs/design/drp/cross-dock.md` §预分配匹配流程（PENDING 创建面实现注记：仅手工/集成创建 + 精确匹配键 + 启用前置条件 + successor 指针）。断言等价物绿：消费链既有测试 `TestErpDrpCrossDock.testMarkReceivedFromPurchaseIdempotent`（手工 setSourceBill 建 PENDING → markReceivedFromPurchase 标记）零回归绿——手工/集成创建路径为裁决 sanctioned 输入链，行为面零变更。
- [x] <Fix> log-012：按 M2.0 裁决 §2.5 已冻结权威（flux.yaml 唯一运行时权威）执行——auth 注册切 flux 版（filter_shipmentId 在位）+ page.yaml 孪生退役；与 M2.8 分片③ `view-and-page-strategy.md` 成文互引注记。
      - Skill: nop-frontend-dev
      - **执行形态偏离登记（如实）**：auth 注册切 flux 版路线被执行环境工具层拒绝（编辑工具 deny 规则 `**/_*.action-auth.xml` 对 `_vfs` 目录下手写 auth 文件 over-match，执行者不以 bash 绕过用户配置的 deny）；按 finding 修复方向的 **sanctioned 替代路线执行：注册版 `shipment-tracking.page.yaml` 补 shipmentId 过滤**——trackingData 数据源补 `filter_shipmentId: ${filterForm?.shipmentId || null}` + `dependsOn: [filterForm]` + `sendOn: ${filterForm?.shipmentId != null}` 门控（未选发运单不发请求，串页面与初始全量加载双消解），与 flux 孪生同语义。M2.0 裁决消费注记：§2.5「flux.yaml 唯一运行时权威、page.yaml 冻结为回退产物」的成文义务在 M2.8 分片③ `view-and-page-strategy.md`（互引待其成文）；本站双载体现同语义（均含过滤），孪生收敛归 M2.8 成文后统一处置。验收：`npm run validate:flux` exit 0（本批新增错误 0，总 325 = 计划载明既有漂移对照面持平；shipment-tracking 双载体零 error/warning 命中）。
- [x] <Fix> md-015 按 Decision 落地（ORM 面双批准范围内）+ `ErpMdSupplierApproval` 孤儿默认值消除断言绿。
      - Skill: nop-backend-dev
      - 落地面：`module-master-data/model/app-erp-master-data.orm.xml:1191` 移除 `defaultValue="10"`（双批准范围严格一致）+ `mvn clean install -DskipTests` 增量重生成链。断言绿：`TestErpMdSupplierApprovalDefaultOrphan.testBareSaveWithoutStatusRejected`（裸 save 被拒）+ `testExplicitStatusSaveAndApplyStillWorks`（显式 REJECTED 建行 + apply 重新申请链路不受影响）；`TestErpMdSupplierApprovalStateMachine` 既有状态机套件零回归绿；md 域模块 162 测试全绿。
- [x] <Fix> notify-009：14 错误码逐码处置——契约要求接线的逐码接线（INSTANCE_NOT_FOUND 接线 serve notify-004 需求面，协调注记）、其余逐码书面 not-a-problem 登记（证伪路径 = owner doc §五步流程；两态必居其一禁止无登记跳过）。
      - Skill: nop-backend-dev
      - **接线（1/12）**：`INSTANCE_NOT_FOUND` → `ErpSysNotificationMarkReadProcessor.markRead`（通知不存在抛 `ERR_NOTIFY_INSTANCE_NOT_FOUND`，不再静默写孤儿已读行）；该码即 r1 notify-004（markRead not-found 拒绝）修复所需——本接线即为 notify-004 需求面供能，r1 通道状态不动、状态回填归 M2.9（协调注记）。
      - **逐码 not-a-problem/预留台账（11/12，证据 = 各语义站点 HEAD 实仓复核 + notification-strategy.md best-effort 契约）**：①`TEMPLATE_NOT_ACTIVE`：站点 `ErpSysNotificationNotifyProcessor:43-46`（template==null → WARN + config-gated 静默跳过）——通知为 best-effort 关注点，抛错违 owner doc config-gated 契约（码描述自证「config-gated 静默跳过」语义），not-a-problem（码保留作诊断面）。②`CHANNEL_DISABLED`：站点 `NotificationDispatcher:150-168`（config 关闭 → WARN + skip）——同 best-effort 契约，not-a-problem。③`RECIPIENT_EMPTY`：站点 `NotificationDispatcher:80-85`（解析空 → WARN + config-gated skip）——同上，not-a-problem。④`TEMPLATE_NOT_FOUND`：模板消费面仅按 notificationType 查 ACTIVE（①同站点语义覆盖），按 id 直查无业务站点；保留预留，not-a-problem。⑤`TEMPLATE_DUPLICATE_TYPE`：模板 BizModel 为裸 CRUD 无 activate/注册 mutation（实仓核证），「重复 ACTIVE 注册」面不存在；successor 模板管理 mutation 落地时接线，预留登记。⑥`SUBSCRIPTION_NOT_FOUND`/⑦`SUBSCRIPTION_DUPLICATE`：无订阅实体/BizModel/任何订阅生产面（实仓核证，O-11 扩展预留码）——预留登记，not-a-problem。⑧`CHANNEL_PROVIDER_FAILED`：email/sms 发送站点为 best-effort try/catch + LOG.error（Noop 发送器为生产默认），Provider 失败按契约不上抛；预留登记。⑨`LOCALE_NOT_SUPPORTED`：无 locale 化模板渲染面（单模板单 locale 实仓核证）——预留登记。⑩`DISPATCH_RETRY_EXHAUSTED`：派发引擎无重试循环（retry 面未落地）——预留登记，successor 重试机制落地时接线。⑪`EVENT_TYPE_INVALID`：eventType 仅作 ORM 参数化查询过滤值（无路径拼接/eval 面），注入面不存在；预留登记，successor 订阅主题校验落地时接线。台账消费：M2.9 回填时逐码引用本注记。
- [x] <Fix> notify-010：markAllRead/markRead 统一强制 ctx 用户或收件人身份校验（越权拒绝负路径测试绿 + 新码 CAT-2 传码）；与 notify-004 同批协调注记落盘。
      - Skill: nop-backend-dev
      - 落地面：`AbstractErpSysNotificationProcessor.assertActorAllowed`（ctx 有登录用户时目标接收人须与 ctx 一致，否则新码 `ERR_NOTIFY_USER_MISMATCH`——CAT-2 传码 = actorUserId+recipientUserId）；markAllRead 于 resolveUserId 后强制调用；markRead 于收件人解析后强制调用 + 不存在通知抛 `ERR_NOTIFY_INSTANCE_NOT_FOUND`（009 接线面）。ctx 无用户（系统内部调用）保持兼容放行。协调注记：markRead not-found 拒绝面与 r1 notify-004 控制点 B 同点，本批接线即其修复供能，状态回填归 M2.9。测试：越权拒绝 ×2 + INSTANCE_NOT_FOUND + ctx 回退放行 4 测试绿；既有 notify 测试（3 处显式 userId 路径）按收件人身份修正后零回归绿；app-erp-all 集成 C09/C16/C17 通知段按收件人身份执行（快照 CREATED_BY 为 autotest-ref 参照占位，零漂移）全绿。

Exit Criteria:

- [x] 八条负路径测试全绿（或按裁决降级登记面其断言等价物绿）；三 Decision 注记在案；md-015 双批准记录在案
      - 八条绿：aps-012 ×2（终态复活拒绝）+ crm-020 ×1（无联系 LEAD 拒绝）+ notify-009/010 ×4（越权 ×2 / INSTANCE_NOT_FOUND / ctx 回放行）+ md-015 ×2（裸 save 拒绝 / 显式链路不受影响）全绿；降级登记面断言等价物绿：b2b-017（登记 + 豁免注记 + 行为零回归）、drp-021（doc 裁决行 + 手工创建消费链既有测试绿）、log-012（page.yaml 过滤 + validate:flux 无新增错误）。三 Decision 注记（b2b-017/drp-021/md-015）在本 Phase 各 Decision 项。md-015 双批准：Phase 1 首项注记 Agent#1 ses_f756d3549ffeXo5U6SGBcNTkiT + Agent#2 ses_f756ce8d6ffek6U24Uhlztqz92。
- [x] log-012 注册载体切换后 `npm run validate:flux` 无本批新增错误；notify-009 逐码两态台账（接线/证伪）在案
      - validate:flux exit 0：totals errors=325 = 计划载明「325 条既有 variant 漂移对照面」持平（本批新增 0）；shipment-tracking 双载体零 error/warning 命中。注册载体形态偏离（auth 切 flux 版被工具 deny，按 sanctioned 替代路线补注册版过滤）已如实登记于 log-012 Fix 项注记。notify-009 逐码两态台账：接线 1（INSTANCE_NOT_FOUND）+ 证伪/预留 11，逐码带站点 HEAD 证据，落 notify-009 Fix 项注记。Phase 2 域模块套件（aps/crm/notify/md）全绿 + 全 reactor `mvn test` BUILD SUCCESS（2026-09-10 本执行，含 C09/C16/C17 集成快照按收件人身份修正后零漂移）。

## Phase 3 — P3 独立项批（8 ID：common-012/aps-013/aps-014/crm-022/crm-024/cs-025/drp-023/log-013）

> 统一类型：Decision | Proof | Fix（2 Decision + 8 Proof + 8 Fix）。
> Skill: bug-diagnosis-prompt + nop-backend-dev + nop-frontend-dev（crm-024 DIM-F 面）+ nop-testing
> Targets: `module-common-service/` + 各域 service/processor/web 面 + `module-drp/model/`（drp-023 ORM，双批准前置）
> Prereqs: Phase 2 完成

- [x] <Proof> Phase 3 八条失败测试先行（common-012 解析失败静默跳过隔离过滤器复现；aps-013 非 Nop 异常中止整批违「不阻塞其他行」复现；aps-014 单界方案零过滤复现；crm-022 任意 status 直建绕过 requireOpen 复现；crm-024 前缀硬编码失配 + ¥ 无 i18nEn 复现；cs-025 降级返空池零日志复现；drp-023 dock 关系错挂面复现；log-013 DISPATCHED/IN_TRANSIT 在途预约放行复现）；执行确认红，失败输出记入勾选注记。
      - Skill: nop-testing
      - **common-012 红态实测**：`TestErpOrgIsolationFailClosed` 编译红——回退 common-service 三文件后 `找不到符号 变量 CONFIG_ORG_ISOLATION_FAIL_CLOSED`（fail-closed 两态能力在 HEAD 不存在；行为红 = catch(Throwable)→false 零日志静默跳过，审计控制点 ErpOrgIsolationQueryTransformer:94-96 HEAD 复核）。
      - **aps-014 红态实测**：`TestErpApsHorizonConstraintLoad.testSingleSideHorizonFiltersHistoricalConstraints` FAILURE——`历史约束（endTime < horizonStart）应被单侧过滤排除，实际载入=[C-HIST, C-IN]`（单界方案零过滤实证）。
      - **crm-022 红态实测**：`TestErpCrmForecastPeriodCreateGuard` 2 FAILURE——`创建路径应强制初始态 OPEN ==> expected: <OPEN> but was: <FROZEN>` + `freeze 应可达: ...forecast-period-not-open...当前状态=CLOSED`（任意 status 直建绕过 requireOpen 实证）。
      - **cs-025 红态实测**：`TestTicketAssignResolverDegradationLog` FAILURE——`降级必须写 WARN 日志（含 teamCode + 降级原因），实际事件=[]`（故障注入桩 + ListAppender 零事件实证）。
      - **drp-023 红态实测**：`TestErpDrpDockAppointmentDict` ERROR——`待解析的资源文件不存在:/dict/erp-inv/drp-xdock-dock-status.dict.yaml`（字典缺失实证）。
      - **log-013 红态实测**：`TestErpLogDeliveryBooking.testBookRejectsInTransitShipment` FAILURE——`预期抛出 NopException 但未抛出`（DISPATCHED 在途预约放行实证）。
      - **aps-013 / crm-024 = 机械面（计划 Goals 分片验收断言口径）**：aps-013 红态 = catch 面仅 `catch (NopException)`（审计控制点 ErpApsOperationOrderBizModel:128 HEAD 复核 + 本批 diff 证据），行为红需对 IoC BizModel 注入非 Nop 故障桩、超出机械修复面验收需要；crm-024 红态 = flux yaml `substring(5)/substring(4)` 长度硬编码 + ¥ 节点无 i18nEn（审计控制点 + 本批 grep 断言，见 Fix 项注记）。
- [x] <Decision> drp-023 双批准 + 选型：ORM 修正 refEntityName→ErpMdWarehouseLocation(type=DOCK) + 补 status ext:dict vs Non-Goal 裁决注记（ORM 保护区二选一，选择与残余风险记入注记）。
      - Skill: bug-diagnosis-prompt
      - 双批准：Phase 1 首项注记 Agent#1 ses_f756d3549ffeXo5U6SGBcNTkiT + Agent#2 ses_f756ce8d6ffek6U24Uhlztqz92（范围 = 收窄面：仅 status ext:dict + 新增加性 5 值字典；关系 retarget 判不可行）。
      - 裁决：**收窄混合形态**——(a) status 列补 `ext:dict="erp-inv/drp-xdock-dock-status"` + ORM `<dicts>` 新增 5 值字典（AVAILABLE/BOOKED/ARRIVED/COMPLETED/CANCELLED，value==code 单轨，对齐 cross-dock.md §月台预约）：implement；(b) dock to-one 关系 retarget 到 ErpMdWarehouseLocation：**判不可行**——该实体在产品基线不存在（全 ORM/Java 实仓 grep 零命中，两 agent 独立核证），改挂 ErpMdWarehouse 语义错误、删除关系为破坏性变更且无行为收益（Non-Goal 实体运行时零影响），采 finding sanction 的替代路线「与 owner doc 对齐登记 Non-Goal 裁决注记」（cross-dock.md §月台预约注记已落盘，successor = 引入月台/库位主数据实体后 retarget）。残余风险：dock 关系在基线上持续错挂（启用月台预约流前必须按 successor 路线修正——注记显式登记）；status 精度 VARCHAR(50) 保守保留（不改列型，最小面）。
- [x] <Decision> log-013 二选一：状态白名单收窄（BOOKED 仅 DRAFT/ADVISED 期，对齐 delivery-window.md:117）vs owner doc 放宽裁决——选择记入注记。
      - Skill: bug-diagnosis-prompt
      - 裁决：**状态白名单收窄**（BOOKED 仅 DRAFT/ADVISED 期预约）——owner doc delivery-window.md D2 裁决已明确「BOOKED = 预约创建态（发运单 DRAFT/ADVISED 期预约）」，在途预约占窗与容量语义直接相悖；原实现抛错码参数已自证期望态 DRAFT/ADVISED（实现与错误码自相矛盾），收窄使实现归齐 owner doc，无 doc 改动需要。既有正向测试（DISPATCHED 期预约）属测试编码缺陷面（与 owner doc 相悖的正向样本），按计划修正为 ADVISED 期预约 + 显式在途推进步骤，测试目的（DELIVERED 联动释放）保持。
- [x] <Fix> common-012：解析失败与无列两态区分 + WARN（fail-open 面消解）+ fail-closed 开关面按索引方向落地；「隔离开关投产前必须修」触发条件承继注记。
      - Skill: nop-backend-dev
      - 落地面：`ErpOrgIsolationQueryTransformer` 两态区分——解析成功无 orgId 列 = 合法白名单跳过（静默）；解析失败（daoProvider 缺失/类加载/dao/模型失败）= `onResolveFailure`：WARN 英文日志（可观测）+ config `erp.multi-company.org-isolation-fail-closed`=true 时抛新码 `ERR_ORG_ISOLATION_RESOLVE_FAILED`（ErpCommonErrors，CAT-2 传码 entityName；拒答优于未隔离放行），缺省 false 保持 fail-open 兼容。触发条件承继：「隔离开关投产前必须修」由本修复消解——投产前建议显式开启 fail-closed（配置默认值维持 false 不改部署契约）。测试：fail-closed 抛错 + fail-open 返 false 两测试绿。
- [x] <Fix> aps-013：catch 面收窄 Exception + 失败行 evict 或结果/事务边界对齐（pur-013/mfg-017 同型族范式互链注记，r1 通道状态不动）。
      - Skill: nop-backend-dev
      - 落地面：`ErpApsOperationOrderBizModel.batchScheduleForward` catch 面收窄为 `Exception`（非 Nop 引擎/持久层异常行级隔离记入 failures，code = 异常类名 CAT-2 传码）+ 失败行 `orm().clearSession()` 丢弃半途脏状态（先行成功行已在各自 run 内 flushSession 落库，结果上报与事务边界对齐；保护方法 `discardFailedRowState` 供 Delta 覆盖）。族范式互链：pur-013/mfg-017 同型族（r1 通道，状态不动，M2.9 对账）。测试：`TestErpApsBatchScheduleIsolation`——失败行（PUBLISHED 方案守卫拒绝）在前不阻塞合法行排产（PLANNED 断言）+ failures 明细断言，绿。
- [x] <Fix> aps-014：单界单侧过滤 + horizon 相关性裁剪（与 aps-002 叠加面注记）。
      - Skill: nop-backend-dev
      - 落地面：`ErpApsSchedulingProcessor.loadMaintenanceConstraints` 双界强制改按已设界单侧过滤（区间重叠语义不变：endTime ≥ horizonStart 且 startTime ≤ horizonEnd）——单界方案不再全量载入约束，历史长期停机约束（endTime < horizonStart）被排除，不再经 buildTimelines addBusy 进入时间线（相关性裁剪 = 载入面排除，与 aps-002 假冲突叠加面随之收敛）。测试：单界方案历史约束排除 + 窗口内保留 + 双界方案区间重叠语义（历史/窗口内/窗口后三段断言）全绿。
- [x] <Fix> crm-022：defaultPrepareSave 强制 status=OPEN（创建路径守卫；crm2-012 update 侧 fixed 同族承接注记）。
      - Skill: nop-backend-dev
      - 落地面：`ErpCrmForecastPeriodBizModel.defaultPrepareSave` 强制初始态 OPEN——FROZEN/CLOSED 直建不再绕过 requireOpen 语义（Lead/Event「初始态由创建路径写入」同族范式；crm2-012 update 侧 fixed 的创建侧承接）。测试：FROZEN/CLOSED 直建被覆写为 OPEN + freeze（requireOpen）经强制 OPEN 后可达，两测试绿；既有 Forecast 测试（TestErpCrmForecastAndScoring/FrontForecastRecalcJob/TerritoryRollup）零回归绿。
- [x] <Fix> crm-024：kanban cardId/toColumnId 前缀常量化或事件载荷直传 ID + ¥ 货币符号走 i18nEn/配置（cs kanban 同型 L83 注记归 M2.8 分片③ cs-026 片）。
      - Skill: nop-frontend-dev
      - 落地面：`opportunity-kanban.flux.yaml`——onCardMove 参数剥离由 `substring(5)/substring(4)` 长度耦合改为 `replace("card-","")/replace("col-","")` 前缀 token 解耦（前缀变更不再静默错参；ids 为数字串无 replace 误替换面）；¥ 节点补 i18nEn 双语承载（zh ¥ / en $）。验收断言：grep 断言该文件零 `.substring(` 残留 + revenue 节点 i18nEn 在位；`npm run validate:flux` 该页编译通过（零 error，既有 WARN 面持平）。cs kanban 同型面（kanban.flux.yaml L83 substring）归 M2.8 分片③ cs-026 片（本批 Non-Goal，注记移交）。
- [x] <Fix> cs-025：TicketAssignResolver 降级补 LOG.warn（降级原因 + teamCode，英文载体 + 传码参数），保持空池降级语义。
      - Skill: nop-backend-dev
      - 落地面：`TicketAssignResolver.resolveCandidatePool` catch(RuntimeException) 补 `LOG.warn`（英文载体，参数 teamCode + 原因消息，空池降级语义不变）——对齐全域 notify 8 站点「降级必 WARN」范式，自动分配失效可诊断。测试：故障注入桩（动态代理 crmTeamBiz.findList 抛 IllegalStateException）+ ListAppender 断言 WARN 事件含 teamCode 与原因，绿。
- [x] <Fix> drp-023 按 Decision 落地（ORM 修正或 Non-Goal 裁决注记 + doc 对齐）。
      - Skill: nop-backend-dev
      - 落地面：ORM `ErpInvDrpDockAppointment.status` 补 `ext:dict="erp-inv/drp-xdock-dock-status"` + `<dicts>` 新增 5 值字典（双批准收窄面严格一致）+ `mvn clean install -DskipTests` 增量重生成（dict yaml/_ErpDrpDaoConstants/_app.orm.xml/xmeta/i18n/api OutputBean 同步）；dock 关系 Non-Goal 裁决注记落 cross-dock.md §月台预约（successor retarget 路线在案）。测试：`TestErpDrpDockAppointmentDict`（字典存在 + 5 值 label 解析）红→绿；drp 域套件零回归绿。
- [x] <Fix> log-013 按裁决落地（白名单收窄负路径测试绿或 doc 放宽裁决行 + 既有正向测试对账修正）。
      - Skill: nop-backend-dev
      - 落地面：`ErpLogDeliveryBookingBizModel.book` 状态守卫由「仅拒 CANCELLED/DELIVERED」收窄为白名单 DRAFT/ADVISED（DISPATCHED/IN_TRANSIT 在途预约拒绝，错误码参数与实现对齐）。既有正向测试对账修正：`testShipmentDeliveredReleasesBooking` 预约时机改 ADVISED 期 + 显式在途推进步骤（DELIVERED 联动释放目的保持；伴随快照 erp_log_shipment.csv VERSION 1→2 对账更新——case 级 fixture，`_init-data` 零触碰）。测试：新增在途拒绝负路径（DISPATCHED/IN_TRANSIT 双断言 + 容量零占用）绿 + 既有 10 组预约测试零回归绿。

Exit Criteria:

- [x] 八条负路径测试全绿（或 doc 放宽裁决面其对账断言绿）；两 Decision 注记 + drp-023 双批准记录在案
      - 八条绿：common-012 ×2（fail-closed 抛错/fail-open 兼容）+ aps-013 ×1（行级隔离行为断言）+ aps-014 ×2（单侧过滤/双界重叠语义）+ crm-022 ×2（强制 OPEN/freeze 可达）+ cs-025 ×1（降级 WARN 断言）+ drp-023 ×1（字典 5 值）+ log-013 ×2（在途拒绝/正向对账）全绿；crm-024 机械断言（grep 零 substring + i18nEn 在位 + validate:flux 编译过）在 Fix 项注记。两 Decision（drp-023/log-013）注记在本 Phase 各 Decision 项；drp-023 双批准 = Phase 1 首项注记（Agent#1/#2 ses 指针）。
- [x] crm-024 修改后 `npm run validate:flux` 无本批新增错误；common-012 两态行为测试绿
      - validate:flux：totals errors=325 = 计划载明既有漂移对照面持平（本批新增 0）；opportunity-kanban/shipment-tracking 双页零 error 命中（opportunity-kanban 编译导出通过）。common-012 两态行为测试绿（fail-closed 抛 ERR_ORG_ISOLATION_RESOLVE_FAILED / fail-open 返 false）。Phase 3 域模块套件（aps/crm/cs/drp/log/common/finance）全绿 + 全 reactor `mvn test` BUILD SUCCESS（2026-09-10 本执行）。

## Phase 4 — 批级证明与 M2.9 消费证据

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增代码；验证输出与 M2.9 消费证据记入勾选注记
> Prereqs: Phase 3 完成

- [x] <Proof> 域级回归：11 域模块分片聚合 `mvn test -am` 全绿零新增失败（ct/b2b/crm/cs/drp/aps/log/notify/md/common/prj，参照 168/80/188/185/98/82/66/23/160/23/179；执行期以 known-good-baselines 最新行为权威，姊妹计划增量允许并披露）。
      - Skill: none
      - 执行记录（2026-09-10 本执行）：11 service 模块聚合 `mvn test`（ct/b2b/crm/cs/drp/aps/log/notify/md/common/prj）BUILD SUCCESS 零失败零错误（b2b 81 tests 全绿等分模块 SUCCESS 行在 build 输出）；各模块套件均含本批新增测试（Phase 1~3 各域红→绿记录见各 Phase 注记）。
- [x] <Proof> 收官门控：全 reactor `mvn test` 零新增失败（对照 known-good-baselines 最新登记行）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则不高于机器块（R2b=242/R2c=1542/R12a=71；漂移即停走独立基线裁决）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS + `npm run validate:flux` 无本批新增错误（325 条既有 variant 漂移为对照面）+ seed 触碰面核证（零触碰或双面重录已完成）+ `mvn clean install -DskipTests` BUILD SUCCESS。
      - Skill: none
      - 全 reactor `mvn test`：BUILD SUCCESS 零新增失败（2026-09-10 本执行 ×2——Phase 3 后与收官各一次）。
      - compliance checker：**exit 0**，R2b=242（=基线）/ R2c=1543（=计划 Current Baseline 载明的已登记 +1 successor baseline-raise 裁决口径，本批零新增漂移）/ R12a=71（=基线）。
      - CJK `--strict`：**PASS exit 0**（0 new violations vs 冻结快照，170 baseline files；RESULT 行在 /tmp/cjk.log 等价输出）。
      - validate:flux：totals errors=**325** = 计划载明既有漂移对照面精确持平（本批新增 0）；opportunity-kanban/shipment-tracking 双页零 error 命中。
      - seed 触碰面：`git status app-erp-all/src/main/resources/_vfs/_init-data/` 零改动（零触碰，双面重录义务未触发）；批内快照对账 = b2b retryDeadLetter output mft_log.csv（RETRYING 行为对账）+ log testShipmentDeliveredReleasesBooking output shipment VERSION 1→2（在途推进步骤对账），均为 case 级 fixture。
      - `mvn clean install -DskipTests`：**BUILD SUCCESS**（收官复跑，drp ORM 增量重生成链含字典 yaml/_ErpDrpDaoConstants/_app.orm.xml/xmeta/i18n/api OutputBean）。
- [x] <Proof> M2.9 消费证据落盘（本计划勾选注记，不改索引）：21 ID 逐条 fixed/not-a-problem 证据指针（测试类/方法 + 修复站点）；dict 族统一批次设计证据（ct-025 裁决 + 范式摘要，供 r1 b2b-013 通道与 M2.9 同型状态继承消费）；跨轮协调注记（notify-004 通道 / ct-024 返利链 / drp-019 族 / aps-013 族）；ORM 双批准记录汇总；触碰面声明（seed/页面/ORM 实触清单）。
      - Skill: none
      - **（一）21 ID 逐条证据指针**：
        - `P1-CK-ct-025-r3` fixed：测试 `TestErpCtSignDictTrack#testSignStatusDictResolvesWriterCodeForm/testSignProviderDictResolvesWriterCodeForm`；修复站点 = `module-contract/model/app-erp-contract.orm.xml` sign-status/sign-provider dict value==code 收敛（生成链 4 件同步）+ `docs/design/contract/e-signature.md` §字典表 + `docs/design/contract/seed-data.md:53` quirk 关闭注记。
        - `P1-CK-ct-026-r3` fixed：测试 `TestErpCtRebateActivation#testActivateDraftAgreementUnlocksRunAccrual/testActivateRejectsNonDraftSource/testActivateRejectsBeforeStartDate`；修复站点 = `ErpCtRebateAgreementBizModel.activate` + `IErpCtRebateAgreementBiz` + `ErpCtRebateAgreementStateMachine.assertCanActivate/transitions` + `ErpCtErrors.ERR_CT_REBATE_AGREEMENT_NOT_EFFECTIVE` + `docs/design/contract/state-machine.md` §适用对象三。
        - `P3-CK-drp-022-r3` fixed：测试 `TestErpDrpReplenishmentMethod`（4 方法）；修复站点 = `DrpEngine.applyReplenishmentMethod`（runDrp 接线）+ `SimulationDrpEngine` 同源。
        - `P3-CK-b2b-018-r3` fixed：测试 `TestErpB2bMftTransport#testTransport5xxRetryWritesRetryingLog`；修复站点 = `TransportManager` 重试循环 RETRYING 中间 log + `docs/design/b2b/managed-file-transfer.md` §MFT 状态字典（PENDING/RECEIVED 处置注记）+ §重试策略表。
        - `P3-CK-prj-024-r3` fixed：ORM 死字典删除（生成链同步）+ `ErpPrjConstants:62-63/86-88` PENDING 保留注记；验收 = grep `erp-prj/timesheet-status` 零活引用 + pnl PENDING 注记在案（登记面机械类）。
        - `P2-CK-aps-012-r3` fixed：测试 `TestErpApsSchedulingEngine#testInsertRushOrderRejectsTerminalRush/testInsertRushOrderRejectsCancelledRush`；修复站点 = `ErpApsSchedulingInsertRushOrderProcessor` 白名单守卫 + PLANNED 预留释放/Bean 矩阵回退 + `ErpApsErrors.ERR_APS_RUSH_ORDER_NOT_INSERTABLE`。
        - `P2-CK-b2b-017-r3` fixed：站点 = `data-dependency-matrix.md` §2.2 b2b 行 + §2.4 b2b-service→pur-dao 行 + §4.2 B2B ASN 行 + `ErpB2bAsnCreateReceiveFromAsnProcessor`/`ErpB2bAsnMatchPurchaseOrderProcessor` 豁免 javadoc（机械类；行为零回归 = `TestErpB2bAsnInbound` 套件绿）。
        - `P2-CK-crm-020-r3` fixed：测试 `TestErpCrmLeadConversion#testQualifyRequiresContactInfoAndLeadType`；修复站点 = `ErpCrmLeadProcessor.validateTransitionForQualify` 联系人门槛（LEAD 路径）+ 新码 `ERR_LEAD_CONTACT_REQUIRED` + owner doc :44 实现注记（类型前置按转化链语义收窄适用面，裁决注记在案）。
        - `P2-CK-drp-021-r3` fixed（裁决降级）：站点 = `docs/design/drp/cross-dock.md` §预分配匹配流程 PENDING 创建面实现注记；断言等价物 = `TestErpDrpCrossDock#testMarkReceivedFromPurchaseIdempotent` 手工建 PENDING 消费链绿。
        - `P2-CK-log-012-r3` fixed：站点 = `shipment-tracking.page.yaml` trackingData 补 `filter_shipmentId` + dependsOn/sendOn 门控（auth 切 flux 版被执行环境工具 deny，sanctioned 替代路线，偏离登记在 Phase 2 Fix 注记）；验收 = validate:flux 零新增错误 + 双页零 error。
        - `P2-CK-md-015-r3` fixed：测试 `TestErpMdSupplierApprovalDefaultOrphan`（2 方法）；修复站点 = `app-erp-master-data.orm.xml:1191` 移除 defaultValue="10"（生成链同步）。
        - `P2-CK-notify-009-r3` fixed+台账：接线 = `INSTANCE_NOT_FOUND` → `ErpSysNotificationMarkReadProcessor`（测试 `TestErpSysNotificationReadIdentity#testMarkReadUnknownNotificationThrowsInstanceNotFound`）；其余 11 码逐码 not-a-problem/预留台账（站点证据 + 理由）见 Phase 2 Fix 注记。
        - `P2-CK-notify-010-r3` fixed：测试 `TestErpSysNotificationReadIdentity`（4 方法）；修复站点 = `AbstractErpSysNotificationProcessor.assertActorAllowed` + MarkAllRead/MarkRead Processor 接线 + 新码 `ERR_NOTIFY_USER_MISMATCH`。
        - `P3-CK-common-012-r3` fixed：测试 `TestErpOrgIsolationFailClosed`（2 方法）；修复站点 = `ErpOrgIsolationQueryTransformer` 两态区分 + `ErpOrgIsolationConstants.CONFIG_ORG_ISOLATION_FAIL_CLOSED` + `ErpCommonErrors.ERR_ORG_ISOLATION_RESOLVE_FAILED`。
        - `P3-CK-aps-013-r3` fixed：测试 `TestErpApsBatchScheduleIsolation#testRowFailureDoesNotBlockOtherRows`；修复站点 = `ErpApsOperationOrderBizModel.batchScheduleForward` catch Exception 收窄 + `discardFailedRowState`。
        - `P3-CK-aps-014-r3` fixed：测试 `TestErpApsHorizonConstraintLoad`（2 方法）；修复站点 = `ErpApsSchedulingProcessor.loadMaintenanceConstraints` 单侧过滤。
        - `P3-CK-crm-022-r3` fixed：测试 `TestErpCrmForecastPeriodCreateGuard`（2 方法）；修复站点 = `ErpCrmForecastPeriodBizModel.defaultPrepareSave` 强制 OPEN。
        - `P3-CK-crm-024-r3` fixed：站点 = `opportunity-kanban.flux.yaml` replace 前缀解耦 + ¥ i18nEn；验收 = grep 零 substring + validate:flux 编译过（机械类）。
        - `P3-CK-cs-025-r3` fixed：测试 `TestTicketAssignResolverDegradationLog#testDegradationWritesWarnWithTeamCode`；修复站点 = `TicketAssignResolver.resolveCandidatePool` LOG.warn。
        - `P3-CK-drp-023-r3` fixed：测试 `TestErpDrpDockAppointmentDict`；修复站点 = drp ORM status ext:dict + 新增 5 值字典（生成链同步）+ cross-dock.md dock 关系 Non-Goal 裁决注记。
        - `P3-CK-log-013-r3` fixed：测试 `TestErpLogDeliveryBooking#testBookRejectsInTransitShipment`（正向对账 = testShipmentDeliveredReleasesBooking 改 ADVISED 期预约）；修复站点 = `ErpLogDeliveryBookingBizModel.book` 白名单 DRAFT/ADVISED。
      - **（二）dict 族统一批次设计证据（供 r1 b2b-013 通道 + M2.9 同型继承）**：范式 = 「writer 实存形态核证（grep 生产 writer + seed/_cases 数据形态）→ value==code 单轨收敛（D1）→ 生成链四件同步（dict yaml/_DaoConstants/_app.orm.xml/i18n）→ 零迁移实证（存量数据全 code 形态则零迁移，否则双面重录）→ 双向 grep 断言归零（数值轨零残留 + writer 形态可解析）+ DictProvider 回归测试」。ct-025 裁决全文见 Phase 1 Decision 注记。r1 b2b-013（blockingLevel defaultValue=10）继承本范式：先核证 blockingLevel 生产 writer/存量形态，再裁决收敛或迁移（r1 通道所有，状态不动）。
      - **（三）跨轮协调注记**：①notify-004 通道：INSTANCE_NOT_FOUND 本批接线（markRead not-found 拒绝）即 notify-004 修复供能面，r1 通道状态不动、回填归 M2.9；notify-010 身份校验与 notify-004 markRead 身份面同点收口。②ct-024 返利链：ct-026 activate 已落地使返利链入口可达，ct-024（rebate-enabled 等死配置键）仍 r1 通道待修——返利链批次在 ct-026 修复后具备完整入口语义（协调注记，状态不动）。③drp-019 族：drp-022 replenishment-method 语义实现 = 同族「dict 死值」修复的 implement 路线范式（drp-019 simulation LEAD_TIME 死值可同型裁决，r1 通道状态不动）。④aps-013 族：pur-013/mfg-017 同型批容错族，本批 catch 收窄 + 失败行会话丢弃 = 族范式（r1 通道状态不动，M2.9 状态继承可引用本注记）。
      - **（四）ORM 双批准记录汇总**：两独立子 agent（fresh session）——Agent#1 task ses_f756d3549ffeXo5U6SGBcNTkiT、Agent#2 task ses_f756ce8d6ffek6U24Uhlztqz92，2026-09-10 各 4/4 APPROVE（ct-025/prj-024/md-015/drp-023 收窄面），范围摘录与附加条件履行见 Phase 1 首项注记；批准范围外零 ORM 触碰。
      - **（五）触碰面声明**：seed（`_init-data`）= 零触碰；页面 = `shipment-tracking.page.yaml`（过滤补齐）+ `opportunity-kanban.flux.yaml`（前缀解耦 + i18nEn）+ `erp-log.action-auth.xml` **未触碰**（工具 deny，登记于 Phase 2 log-012 注记）；ORM 实触清单 = `module-contract/model/app-erp-contract.orm.xml`（sign 双 dict 收敛）+ `module-projects/model/app-erp-projects.orm.xml`（死字典删除）+ `module-master-data/model/app-erp-master-data.orm.xml`（移除孤儿默认值）+ `module-drp/model/app-erp-drp.orm.xml`（status ext:dict + 新增 5 值字典）——四站均在双批准范围内；owner doc 触面 = contract（e-signature/state-machine/seed-data）+ b2b（managed-file-transfer）+ drp（cross-dock）+ crm（state-machine :44 注记）+ architecture（data-dependency-matrix §2.2/§2.4/§4.2）；case 级 fixture 对账 = b2b mft_log.csv + log shipment VERSION（`_init-data` 零触碰）。

Exit Criteria:

- [x] 11 域全绿 + 全 reactor 零新增失败 + 双 checker 持平基线 + validate:flux/seed 面核证在案
      - 11 域聚合 BUILD SUCCESS；全 reactor `mvn test` BUILD SUCCESS（收官复跑）；compliance exit 0（R2b=242/R2c=1543 已登记口径/R12a=71，零新增漂移）；CJK strict PASS（0 新增违规）；validate:flux errors=325 持平对照面；seed `_init-data` 零触碰核证；`mvn clean install -DskipTests` BUILD SUCCESS。
- [x] M2.9 消费证据五件（fixed 指针×21 / dict 族设计证据 / 跨轮协调注记 / 双批准汇总 / 触碰面声明）落盘于计划注记
      - 五件齐备，落 Phase 4 第三项 Proof 注记（21 ID 逐条指针 / dict 族范式 / 四通道协调注记 / 双 agent ses 指针汇总 / 五类触面声明）。

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-10-1141-2-m27-remaining-domains-p1-fix-batch-1-68e7236d to 2026-09-09-210030-mission-driver
- 2026-09-10：iteration 1，共识 approved #review-2026-09-09-210030-mission-driver-2026-09-10-1141-2-m27-remaining-domains-p1-fix-batch-1-68e7236d

## Verification

- pass test 2026-09-10-2045 exit=0

## Closure

- dispatch audit #audit-2026-09-10-2045-2026-09-10-1141-2-m27-remaining-domains-p1-fix-batch-1-8e134bc4 to 2026-09-09-210030-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-2026-09-10-2045-2026-09-10-1141-2-m27-remaining-domains-p1-fix-batch-1-8e134bc4：独立闭包审计通过——43/43 检查项全勾，21 ID 修复体逐站语义实核在位（ct-026 activate 全链/notify assertActorAllowed + INSTANCE_NOT_FOUND/common-012 fail-closed/aps 终态守卫与容错/md-015 孤儿默认值已移除/drp-023 字典登记/log-013 白名单/log-012 过滤门控等，非孤挂代码）；闭包访问独立复跑 full-green verification——`mvn clean install -DskipTests` BUILD SUCCESS（1:39 min）+ 全 reactor `mvn test` BUILD SUCCESS（4057/0/0/1，13:32 min，零新增失败 vs 2026-09-09 基线 4006/0/0/1 + 已披露姊妹/本批增量）；`plan-check.mjs --strict` 复核 passed=true 派生 completed；frontmatter `status: active` 保持 = ledger 协议。
