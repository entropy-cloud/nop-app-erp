# ck-b2b — b2b（B2B 集成 / EDI / ASN）实现代码检查报告

> 工作项：C7.3。执行日期：2026-08-26。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-b2b/erp-b2b-service/src/main/java` 全部 40 个手写生产文件（约 3,300 行）——EDI 信封状态机（`entity/ErpB2bEdiDocBizModel` 243 行 + `ErpB2bEdiDocStateMachine` + `ErpB2bEdiDocCreateOutbound/CreateInboundProcessor`）、ASN 入站链（`entity/ErpB2bAsnBizModel` + 4 个 Processor：handleInboundWebhook / matchPurchaseOrder / createReceiveFromAsn / retryMatch）、EDI Provider SPI（`ErpB2bEdiRegistry` + `UblInvoiceEdiProvider` / `UblDespatchAdviceEdiProvider` + `CodeMappingResolver`）、MFT 传输（`TransportManager` + `ErpB2bMftTransportRegistry` + `MockTransportAdapter`）、伙伴上线（`ErpB2bPartnerProfileBizModel` + `ErpB2bPartnerProfileStateMachine` + `ErpB2bOnboardingMonitorJob`）、9 个 CRUD stub BizModel、Configs/Constants/Errors。跨文件核实：`module-b2b/model/app-erp-b2b.orm.xml`（13 实体 versionProp/UK/dict/列集）、`erp-b2b-service/_vfs/erp/b2b/beans/app-service.beans.xml`（17 bean）、`app-erp-all/_vfs/nop/job/conf/erp-b2b-onboarding-monitor.job.yaml`、`erp-b2b-meta` dict（edi-doc-state/asn-status/partner-status/blocking-level）、`erp-b2b-web/auth/erp-b2b.action-auth.xml`（13 FNPT）、测试 9 文件（TestErpB2bAsnInbound/AsnInventoryIntegration/EdiEnvelope/PartnerOnboarding/MftTransport/EdiPosting/AsnCrudSmoke + statemachine 矩阵 2 文件）、`app-erp-all` C19 集成测试（OA-03 断言）、`docs/bugs/2026-08-25-asn-receive-orgid-missing-writer-posting-dangling.md`（OA-03）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。40 文件全量通读（40 个文件数小，无抽样）+ 平台源码实证 4 处（`ReflectionBizModelBuilder` 无 @Auth 时自动派生默认权限 `BizObj:opType|BizObj:funcName` 且 deny-by-default、`GraphQLActionAuthChecker.isAllowAccess` auth=null 放行/非 null 按 sitemap 权限判定、`CrudBizModel.requireEntity` not-found 抛 `UnknownEntityException` 非 null、先例沿用 `FilterBeans.eq(name,null)`→IS NULL 与 `addOrderField(name,desc)` 第二参为 desc）+ orm/beans/job-yaml/dict/action-auth/xbiz 接线核对 + arm-index b2b 9 条 finding（P1-RC-080、P2-RC-062..068、P1-MA2-073/088）逐条裁决。
> 切片边界：`erp-b2b-api` 骨架与 `erp-b2b-web` AMIS 页面契约 drift 不深查（仅抽查 dict 死值消费与 action-auth 注册面）；测试代码仅用于行为语义交叉验证；`_gen/` 产物不查。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-b2b-001（D8，跨 D6）AsnLine.materialId 全链零 writer——webhook 入站把代码映射结果写入 remark 而非 materialId：逐行物料匹配与超量校验对 webhook 路径为死代码 + createReceiveFromAsn 对 webhook 路径必抛守卫错（自动收货链末端断裂），集成测试经直 seed 遮蔽

- **控制点 A**：`app/erp/b2b/service/processor/ErpB2bAsnHandleInboundWebhookProcessor.java#parseToAsn`（L155-162：`String internalMaterial = codeMappingResolver.resolveInbound(profile.getPartnerId(), MAPPING_TYPE_MATERIAL, parsedLine.getSupplierPartNo()); // internalMaterial 为物料 code 字符串，实际 materialId 需查 ErpMdMaterial。本期保留 code 值到 supplierPartNo + 映射结果到 remark 供后续处理。 line.setRemark(internalMaterial);`——**`line.setMaterialId(...)` 全域零调用**，grep 实证 module-b2b 生产代码 materialId 仅有读取（matchPurchaseOrder L72 / createReceiveFromAsn L124 / findMatchingPoLine L165,230）无写入）
- **控制点 B**：`app/erp/b2b/service/processor/ErpB2bAsnMatchPurchaseOrderProcessor.java#matchPurchaseOrder`（L71-84：`findMatchingPoLine(poLines, asnLine.getMaterialId())` → `findMatchingPoLine` L161-163 `if (materialId == null) return null;` → 每行落入 L73-76 `LOG.warn("行物料 {} 未在 PO 中找到") + continue`——数量校验 L77-83 从不执行）
- **控制点 C**：`app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java#fillReceiveLinesFromAsn`（L124-130：`String materialId = asnLine.getMaterialId(); if (materialId == null) { throw new NopException(ERR_B2B_ASN_LINE_MATERIAL_REQUIRED)...}`——webhook 路径 AsnLine materialId 恒 null → **每次必抛**，@BizMutation 事务回滚，Receive 头+行全部回滚）
- **证据**：三个子症状：① **匹配校验死代码**——asn-processing.md §4.1 步骤 2/3（逐行物料匹配 + 数量验证）与 §4.2（映射后 materialId = MAT-001 写入 AsnLine）对 webhook 建立的 ASN 整体失效：所有行 materialId=null → 超量/部分交货判定（L77-83）与「未在 PO 找到」的行级结果全部退化为 warn 日志，ASN 仍无条件 MATCHED（对 PO 不含的物料同样 MATCHED）；② **自动收货链断裂**——config `erp-b2b.asn-auto-create-receive=true` 时，webhook→match→createReceiveFromAsn 全链在最后一步 100% 抛 `ERR_B2B_ASN_LINE_MATERIAL_REQUIRED`（D6 数量容差校验与收货草稿创建双双落空；该 config 的存在意义即此链）；③ **plan/审计双重遮蔽**——plan `2026-07-04-2200-1` L144 勾选 `[x]` 声明「AsnLine（**materialId 经 CodeMappingResolver.resolveInbound**，supplierPartNo 保留原值，shippedQty/quantity）」与实现不符（实现只写 remark）；该 plan 独立结束审计「语义验证六项全部通过」未发现；A1.47 审计 UC-B2B-003 裁决「接受 on parsePayload+Asn/AsnLine 强测」亦未覆盖。测试遮蔽实证：`TestErpB2bAsnInbound` 仅断言 `supplierPartNo`（L112），而 `TestErpB2bAsnInventoryIntegration` L377 注释自认「**直接 seed ASN（绕过 handleInboundWebhook）**」并 L349/401/415 手工 `line.setMaterialId(materialId)`——webhook 路径的 materialId 缺失对全部集成测试不可见。
- **问题**：D8 跨域数据流 writer 缺失（对齐 OA-03「缺 writer」站点形态，但此处是主链字段而非 orgId）+ D6 匹配容差死代码。ASN 入站自动化链（webhook→匹配→收货）的核心闭环对唯一自动入口（webhook）断裂。
- **建议修复方向**：parseToAsn 内完成 `resolveInbound → ErpMdMaterial 按 code 反查 id → line.setMaterialId(id)`（物料不存在时保留 null + remark 标记「待映射」，与设计 §4.2「未找到 → materialId=null + 标记未映射」一致）；或 matchPurchaseOrder 前置补一道物料解析步骤。修复时补一条「webhook 建单 → 行 materialId 断言 + createReceiveFromAsn 成功」的贯通测试（当前零覆盖）。
- **arm-index 裁决**：新增（grep arm-index「materialId remark/待映射/AsnLine 物料」零命中；P2-RC-065 只覆盖出站映射维度；A1.47 八条 finding 均不含）。

### P2-CK-b2b-002（D2）解析失败错误路径的 EdiDoc 防重键塌缩为 (formatId, ASN_INBOUND, NULL)——同格式第二次解析失败被误判 ALREADY_PROCESSED：真实解析错误被掩蔽 + 后续失败报文 rawPayload 审计永久丢失

- **控制点 A**：`app/erp/b2b/service/processor/ErpB2bAsnHandleInboundWebhookProcessor.java#parseToAsn`（L94-102 catch 块：`ErpB2bEdiDoc errorDoc = ediDocBiz.createInbound(RELATED_BILL_TYPE_ASN_INBOUND, **null**, payload, formatCode, context); ediDocBiz.markError(...); writeEdiLog(errorDoc, ..., payload, e.getMessage()); throw e;`——**所有**解析失败的 error Doc 共用 relatedBillType=ASN_INBOUND + relatedBillCode=null）
- **控制点 B**：`app/erp/b2b/service/processor/ErpB2bEdiDocCreateInboundProcessor.java#checkDuplicate`（L54-68：`q.addFilter(and(eq("formatId", formatId), eq("relatedBillType", relatedBillType), eq("relatedBillCode", relatedBillCode)))`——`FilterBeans.eq(name, null)` 生成 `relatedBillCode IS NULL`（C1.2 校准先例），命中**任意**既往同格式 error Doc 即抛 `ERR_B2B_EDI_DOC_ALREADY_PROCESSED`）
- **证据**：触发序列（要求 formatCode 有 DB 格式记录，如 UBL_DESPATCH_ADVICE 种子）：① 第一次畸形报文 → catch → createInbound(ASN_INBOUND, null) 无既有 → 建 error Doc + markError + EdiLog 保留 rawPayload ✓；② 第二次**不同**畸形报文（同格式）→ catch → createInbound → checkDuplicate 以 (formatId, ASN_INBOUND, IS NULL) 命中第一条 error Doc → **抛 ALREADY_PROCESSED**——该异常从 catch 块内向外传播，(a) 掩蔽真实解析错误（调用方收到「已处理」而非「解析失败」），(b) 第二份 rawPayload 从未落任何 EdiLog（asn-processing.md §3.3 逐字「保留 rawPayload 到 EdiLog（responsePayload = 错误详情）」+ README 反模式「⛔ 不记录 EDI 报文原文」违背）。DB 层 `UK_EDI_DOC_FORMAT_BILL`（orm L194-196）因 NULL 不参与唯一比较不会拦截应用层查询先行。注：成功路径 Doc 键为 (formatId, PO_ORDER, orderCode)，与 error 键不相交——仅 error-vs-error 塌缩。
- **问题**：D2 异常闭环——错误路径自身在第二次触发后失真（错误码错报 + 审计载体丢失），排错与重试（§3.3 通知 B2B 管理员的前提）失去输入。
- **建议修复方向**：error 路径绕过 checkDuplicate（createInbound 增 skipDedup 变体或 error Doc 改用唯一 code 承载去重）；或 error Doc 键补充确定性成分（eventId/payload hash）。
- **arm-index 裁决**：新增（grep「error Doc 塌缩/parse 失败 rawPayload」arm-index 零命中；P1-MA2-088 是 eventId 幂等维度，P2-RC-063 是幂等返回形态维度，均不覆盖 error 路径键塌缩）。

### P2-CK-b2b-003（D8，orgId 隔离族写侧形态）EdiDoc/Asn/EdiLog 全链 orgId 零 writer——R1.28 webhook 并发幂等的 UK 兜底因 NULL 不去重而失效 + 24h 上线监控 org 锚点与 Doc orgId 永不相交（有 orgId 的伙伴监控恒盲区）

- **控制点 A**：`ErpB2bAsnHandleInboundWebhookProcessor#parseToAsn`（L117-126 建 ASN：setBusinessDate/setCode/setSourceEdiDocId/setPartnerId/setRelatedBillType/setRelatedBillCode/setStatus/setShipmentDate/setEstimatedArrivalDate——**无 setOrgId**）+ `ErpB2bEdiDocCreateInboundProcessor#createInbound`（L36-45）与 `ErpB2bEdiDocCreateOutboundProcessor#createOutbound`（L57-66）建 Doc 同样无 setOrgId；grep 实证全域 setOrgId 仅 5 处：4 处 EdiLog 透传 `doc.getOrgId()`（本就 null）+ 1 处 OA-03 的 `receive.setOrgId(po.getOrgId())`。平台无 orgId 回填（OA-03 双取向裁决，`docs/bugs/2026-08-25-...md` §根因）。
- **控制点 B**：`parseToAsn` L131-144 注释声称「flush 触发 INSERT，**命中 (code,orgId) UK**（确定性 code + 并发 TOCTOU 越过 isDuplicateEvent 时）→ 翻译为友好错误码」——`UK_B2B_ASN_CODE_ORG`（orm L243-245）含 orgId 列，两并发同 eventId 插入均为 (code='ASN-WEBHOOK-{eventId}', orgId=NULL)，标准 SQL 唯一约束对 NULL 不去重（MySQL/PostgreSQL/H2 一致）→ **UK 兜底不触发**，双 ASN 落库（串行重复仍被 remark 检查 L183-189 拦截，仅并发窗口暴露）。
- **控制点 C**：`app/erp/b2b/service/job/ErpB2bOnboardingMonitorJob#countEdiDocs`（L215-217：`if (profile.getOrgId() != null) q.addFilter(eq("orgId", profile.getOrgId()))`——javadoc 称「org 级 scope 兜底防跨伙伴误计」，但全部 EdiDoc orgId=null：伙伴档案配了 orgId 时 `eq` 永不命中 → total=0 → monitorPartner L168-169 直接返回 → **24h 监控对该伙伴静默零扫描**）。
- **证据**：三条传导链：① R1.28（P1-MA2-088）修复的并发兜底前提（UK 冲突）被 orgId=null 消解；② RC-R1.36 交付的 24h 监控在「profile.orgId 有值」部署形态下功能全盲（orgId=null 的 profile 则跳过 filter 退化为全局计数，靠 formatIds 锚点尚可）；③ 多组织部署下 b2b 全部行无组织归属（查询侧 orgId filter 因此也无从谈起——orgId 隔离族在本域表现为**写侧**缺口而非读侧漏 filter）。
- **问题**：D8 orgId 透传缺失（OA-03 只修了 receive.orgId 单点，同链上游 EdiDoc/Asn/EdiLog 均遗漏）。
- **建议修复方向**：webhook 入口从 PartnerProfile（或伙伴→组织关系）解析 orgId 并在 EdiDoc/Asn/EdiLog 三处写入（含 createInbound/createOutbound 路径）；与 003 联动修复后 UK 兜底与监控锚点自动恢复。
- **arm-index 裁决**：orgId 隔离族（P2-CK-fin2-007/P2-CK-mfg-007/P2-CK-hr-003④）b2b 站点登记——但形态为写侧 writer 缺口（非读侧 filter 缺失），登记为本域独立 finding；OA-03 bug 只覆盖 receive 单点（其「successor：全仓 #11 家族缺 writer 站点扫描」触发条件即本轮此类检查）。

### P2-CK-b2b-004（D4）`erp-b2b.enabled` 主开关定义后全域零消费——owner doc 以「config-gated OFF 默认」作为 EDI 出站自动化整体 Deferred 的首要论据，代码层不存在该门

- **控制点**：`app/erp/b2b/service/ErpB2bConfigs.java` L9（`CONFIG_B2B_ENABLED = "erp-b2b.enabled"`）——grep 实证 `CONFIG_B2B_ENABLED` 与字面量 `erp-b2b.enabled` 在 module-b2b 生产代码（service+web main）与 app-erp-all 配置**零消费**（仅测试 setup 赋值，如 TestErpB2bEdiEnvelope L59）；对照 `state-machine.md` 文件顶部 Deferred 注记逐字「(1) 整个 b2b 子系统 **config-gated OFF 默认**（`erp-b2b.enabled` default false，`ErpB2bConfigs.java:9,27`）→ 默认 config 零生产暴露」+ README §配置点同声明。
- **证据**：b2b 全部 mutation（含 webhook 入站、自动收货之外的匹配/取消/重试）与 query 在模块部署后**恒活**；`app-erp-all/application.yaml` `%dev`/`%prod` 均 `enable-action-auth: false` → 登录用户即可调用。owner doc 的 Deferred 风险论证第 (1) 条与实现不符——文档承诺的安全姿态（默认关闭）不存在。同文件另有 5 个零消费键：`asn.auto-match-retry-interval`/`asn.match-timeout-hours`（owner doc 已声明 Non-Goal，一致）、`asn.partial-receipt-enabled`（见 007，plan 勾选声称已实现）、`transport-mode`（TransportManager 未接线，owner doc 已 Deferred）、`error-blocks-flow`（owner doc 已声明 Non-Goal）——后三个中 partial-receipt 与 transport-mode 的「已定义未消费」削弱对应 plan 勾选可信度。
- **问题**：D4 声明 vs 实现——安全/暴露面承诺落空 + Deferred 论据失真（审计追溯时按文档会得出「默认零暴露」的错误结论）。
- **建议修复方向**：二选一并同步 owner doc：(a) 在 webhook/mutation 入口实现 `erp-b2b.enabled` 门（false 抛 `ERR_*` 或静默跳过）；(b) 从 README/state-machine.md Deferred 论据中移除该条，改述为「无总开关，靠 action-auth + HMAC 验签」。
- **arm-index 裁决**：新增（grep「erp-b2b.enabled 消费/总开关」arm-index 零命中；P1-MA2-073 resolved via deferral 的裁决文本引用了该 config-gated 论据，但未验证其存在性）。

### P2-CK-b2b-005（D3）retry 无方向守卫——入站 ERROR Doc 经 retry 进入 TO_SEND（出站生命周期），state machine 提供的 retryInboundTargetStatus() 零调用

- **控制点**：`app/erp/b2b/service/entity/ErpB2bEdiDocBizModel#retry`（L122-135：`assertCan("retry", doc, from, EDI_DOC_STATE_ERROR)` 仅校验来源态为 ERROR，**不区分方向**；L127 `doc.setState(stateMachine.retryOutboundTargetStatus());` 无条件 TO_SEND——注释自承「当前实现出站 retry（ERROR→TO_SEND）。入站 ERROR→RECEIVED 路径为 owner doc §2 Deferred successor（D-B2B-6）」）对照 `ErpB2bEdiDocStateMachine` L100-107（`retryOutboundTargetStatus()`/`retryInboundTargetStatus()` 双目标设计，javadoc「目标按方向，唯一的多目标动作」）——grep 实证 `retryInboundTargetStatus` 生产代码零调用（仅 Bean 自身定义 + 测试断言）。
- **证据**：入站 Doc 生命周期 RECEIVED→ERROR（webhook 解析失败路径，002 的 error Doc 即此形态）→ 管理员按 UC-B2B-006 点击「重试」→ state=**TO_SEND**：入站报文进入出站状态机（可继续 markSent→SENT→ACKNOWLEDGED，EdiLog 语义全程错位为「SEND: 报文已发送」）。D-B2B-6 Deferred 的是「入站重试的重新解析编排」，而「入站 Doc 被迁入出站态」不是缺失的 Deferred 功能而是**未设防的错误迁移**——状态机矩阵（transitions() L153-154 声明双目标边）与 BizModel 行为不一致。测试面：TestErpB2bEdiEnvelope 仅测出站 retry（L86-105），入站 retry 零覆盖。
- **问题**：D3 非法迁移无守卫（方向轴缺失）——入站信封可被推入出站生命周期，状态与日志语义双重错位。
- **建议修复方向**：retry 按方向路由（入站 Doc——可经 relatedBillType∈{ASN_INBOUND,PURCHASE_RECEIPT} 或新增 direction 判别——迁移至 RECEIVED 并触发重新解析，或入站 Doc 的 retry 直接拒绝并提示走重新推送）；至少先加「入站 Doc retry 抛非法迁移」守卫。
- **arm-index 裁决**：新增（grep「retry 方向/入站 retry TO_SEND」arm-index 零命中；P2-RC-067 是 retry 清 error 字段维度）。

### P2-CK-b2b-006（D5）入站数量零边界校验——负数/零/非数值数量全程穿透：匹配不设防 → 收货草稿可落负数量行（任务点名「负数量 ASN 行」核查确认缺失）

- **控制点 A**：`app/erp/b2b/service/spi/ubl/UblDespatchAdviceEdiProvider.java#getDecimalDirectChild`（L171-181：`try { return new BigDecimal(text); } catch (NumberFormatException e) { return null; }`——接受任意符号 BigDecimal，畸形值**静默置 null** 无 WARN）
- **控制点 B**：`ErpB2bAsnMatchPurchaseOrderProcessor#matchPurchaseOrder`（L77-83：`if (asnLine.getShippedQty() != null && matchedPoLine.getQuantity() != null) { ... if (asnLine.getShippedQty().compareTo(remaining) > 0) overQuantity = true; }`——负 shippedQty 恒不触发超量；无 `signum() < 0` 拒绝）+ `ErpB2bAsnCreateReceiveFromAsnProcessor#fillReceiveLinesFromAsn`（L146 `BigDecimal qty = shippedQty != null ? shippedQty : quantity; receiveLine.setQuantity(qty);`——qty 可为负/可为 null，无校验直写 ReceiveLine）
- **证据**：grep 实证全域无 `signum`/负数校验（唯一 `BigDecimal.ZERO` 是 remaining 兜底）。恶意/畸形报文 `<cbc:DeliveredQuantity>-50</cbc:DeliveredQuantity>` → AsnLine.shippedQty=-50 → 匹配通过（-50 ≤ remaining）→ MATCHED → 收货草稿行 quantity=-50 进入 purchase 域（下游 approve 时数量守恒校验是否拦截不在本域，草稿已带脏数据 + ASN 已终态 RECEIVED_TO_STOCK）；非数值数量 → 静默 null → 收货行 quantity=null。asn-processing.md §4.1 数量验证表仅定义超量场景，但 §3.3/§八 的解析失败语义应覆盖非法数量（至少 WARN/ERROR 级标记）。
- **问题**：D5 入参边界——外部输入（webhook 报文）未做符号/合法性校验即穿透两条链。
- **建议修复方向**：getDecimalDirectChild 拒绝负值（抛 ERR_B2B_EDI_PARSE_FAILED 或置 null+WARN）；matchPurchaseOrder/fillReceiveLinesFromAsn 对 `qty == null || qty.signum() <= 0` 抛 `ERR_B2B_ASN_LINE_QUANTITY_MISMATCH`（该错误码已定义未用）。
- **arm-index 裁决**：新增（grep「负数量/quantity signum」arm-index 零命中）。

### P2-CK-b2b-007（D3，跨 D6）ASN 部分收货（1:N 入库）未实现——首次建库即终态 + 行级已收计数器零载体 + partial-receipt-enabled config 零消费（plan 勾选与实现不符）

- **控制点 A**：`ErpB2bAsnCreateReceiveFromAsnProcessor#createReceiveFromAsn`（L86-87：首次建库即 `asn.setStatus(stateMachine.createReceiveFromAsnTargetStatus())`→RECEIVED_TO_STOCK 终态——第二次建库被 `assertCanCreateReceiveFromAsn`（仅 MATCHED）拒绝）对照 README §业务规则 7「ASN 与采购入库单是 **1:N** 弱关联（可部分收货、质检、拒收）」+ asn-processing.md §6.2 部分收货流程（600/400 两次 ErpPurReceive）+ §6.4「ASN 维护已收/未收计数器」
- **控制点 B**：ORM `ErpB2bAsnLine`（L267-299 列集：quantity/shippedQty/remark——**无 receivedQty/已收计数列**）+ grep 实证 `CONFIG_ASN_PARTIAL_RECEIPT_ENABLED` 生产零消费；对照 plan `2026-07-04-2200-1` L147 勾选 `[x]`「部分收货维护已收计数（`erp-b2b.asn.partial-receipt-enabled`）」
- **证据**：当前实现是 1:1 单发建库（全量 shippedQty 一次入草稿即封版）。设计语义（仓管员可 600/400 分批、拒收/质检后剩余量等后续 ASN）无载体：状态机一次终态 + 行级无计数 + config 无门。plan 勾选声称已交付但三项全缺（第二次勾选与实现不符，同 001 的遮蔽模式）。
- **问题**：D3 状态机流程完备性——部分收货主路径（设计明确要求）缺位；超收/短收处理表（§6.5）中「以入库单为准，ASN 标记超额接收」亦无实现。
- **建议修复方向**：短期最小修复：建库后不直接终态（保持 MATCHED 或引入 PARTIAL_RECEIVED），行级补 receivedQty 计数（ORM 变更走 dual-agent-approval）；或 owner doc 显式登记 1:1 为本期取舍并修正 plan 勾选失真。
- **arm-index 裁决**：新增（grep「部分收货/receivedQty/1:N」arm-index 零命中；A1.47 UC-B2B-003 接受声明未覆盖部分收货维度）。

### P2-CK-b2b-008（D6，跨 D3）matchPurchaseOrder 数量校验粒度缺陷——同物料多 ASN 行不聚合剩余量 + 超额仅写 remark 文本「blockingLevel=WARN」而无一实体落 WARN

- **控制点 A**：`ErpB2bAsnMatchPurchaseOrderProcessor#matchPurchaseOrder`（L70-84：`for (asnLine : asnLines) { matchedPoLine = findMatchingPoLine(poLines, asnLine.getMaterialId()); ... remaining = quantity - receivedQuantity; if (asnLine.getShippedQty().compareTo(remaining) > 0) overQuantity = true; }`——**每行独立**与同一 PO 行的 remaining 比较，同物料多 ASN 行（如 60+60 对 remaining 100）互不知晓，合计 120>100 不触发）
- **控制点 B**：L88-90 `if (overQuantity) { asn.setRemark("部分行超 PO 数量（blockingLevel=WARN）"); }`——`ErpB2bAsn` 无 blockingLevel 列（orm L215-246 列集实证），`ErpB2bEdiDoc.blockingLevel` 亦未更新：**WARN 仅存在于 remark 字符串**，asn-processing.md §4.1 步骤 3「ASN 数量 > 订单数量 → 标记超额，blocking_level=WARN」无系统化载体（消费方无法按 blockingLevel 过滤/通知）
- **证据**：两项叠加使「数量验证」环节（§4.1 核心）只剩日志级效力：聚合缺口 + 无字段载体。另注：校验只用 shippedQty，AsnLine.quantity（订单数量）零消费（与 001 的行匹配死代码独立——001 修复后本条才实际可触发）。
- **问题**：D6 数量容差计算正确性（任务点名「ASN 行与 PO 行数量匹配容差」核查：BigDecimal 比较本身用 compareTo 无精度错误，但聚合口径缺失）+ D3 虚拟 blockingLevel。
- **建议修复方向**：按 materialId 聚合 ASN 行合计后与 remaining 比较；超额时写 EdiDoc（来源报文）blockingLevel=WARN 并同步 remark，或 ORM 为 ASN 增列（dual-agent）。
- **arm-index 裁决**：新增（grep「超量聚合/blockingLevel remark」arm-index 零命中）。

### P2-CK-b2b-009（D5，同型 P1-CK-pur-003 族）CRUD update 无已审守卫——EdiDoc state/retryCount/error、Asn status、PartnerProfile status 可经 update_ 直改，9 个 stub BizModel 无任何守卫

- **控制点**：`ErpB2bEdiDocBizModel`/`ErpB2bAsnBizModel`（defaultPrepareSave 仅 businessDate 兜底，defaultPrepareUpdate 未 override——state/status/error/retryCount/blockingLevel 可 update_ 直写，终态复活/错误粉饰/计数篡改全旁路状态机 Bean）+ `ErpB2bPartnerProfileBizModel`（status/goLiveDate/archivedAt 同面，RC-R1.36 守卫仅护 5 个命名 mutation）+ 9 个 17 行 CRUD stub（EdiFormat/EdiLog/CodeMapping/AsnLine/MftConfig/MftLog/TestExchange/CertificationChecklist/PartnerCredential—— EdiFormat.isActive 直改可联动 P2-RC-062、Credential.secretKey 直改绕过 HMAC 语义）
- **证据**：与全域命名 mutation 守卫体系形成旁路面（ACKNOWLEDGED→TO_SEND 等非法迁移经 update_ 可达）。
- **问题**：同型 P1-CK-pur-003 / P2-CK-cs-012 族——b2b 站点登记（不复用展开）。
- **建议修复方向**：defaultPrepareUpdate/Save 守卫：state/status 变更仅允许经命名 mutation；error/retryCount/blockingLevel/isActive/secretKey 拒绝 CRUD 写入（或白名单）。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，b2b 站点新增计数）。

### P3-CK-b2b-010（D1）`LocalDateTime.now()` 直读系统时钟——CoreMetrics 时间可控约定违例（checker R7 盲区变体，冻结钟测试失效点）

- **控制点**：`app/erp/b2b/service/job/ErpB2bOnboardingMonitorJob.java#countEdiDocs`（L219-220：`q.addFilter(dateTimeBetween("createTime", profile.getGoLiveDate().atStartOfDay(), **LocalDateTime.now()**));`——同文件 L128 用 `CoreMetrics.currentDate()`、L142 用 `CoreMetrics.currentTimestamp()`，唯此一处漏网；同目录测试基建 `B2bFrozenClockExtension` 冻结钟对该上界不生效）
- **证据**：skills README 已知失败模式 #4；先例 P3-CK-ast2-016（`YearMonth.now()` 同级定级 P3）。影响面：监控窗口上界不可测 + 时钟注入不一致。
- **问题**：D1 平台反模式（行为影响=测试可控性/窗口上界）。
- **建议修复方向**：改 `CoreMetrics.currentDateTime()`。
- **arm-index 裁决**：新增（b2b 域内首处；家族先例 ast2-016）。

### P3-CK-b2b-011（D5）action-auth 注册面不一致——createOutbound/createInbound/promoteToTesting/promoteToCertified 4 个 mutation 无 FNPT 注册（同 BizModel 兄弟 mutation 均注册）

- **控制点**：`module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/auth/erp-b2b.action-auth.xml`（13 个 FNPT 实证：EdiDoc 6/8——缺 createOutbound、createInbound；Asn 4/4 全；PartnerProfile 3/5——缺 promoteToTesting、promoteToCertified（RC-R1.36 新增时未同步））+ 平台实证 `nop-entropy/.../ReflectionBizModelBuilder.java` L361-366：无 @Auth 注解的 biz 方法自动派生默认权限 `bizObjName:opType|bizObjName:name`（deny-by-default，需 sitemap 授权命中任一）。
- **证据**：`%test` profile（enable-action-auth=true + skip-check-for-admin=true）下非管理员角色调用这 4 个 mutation 将被默认权限拒绝（无任何角色持有 `ErpB2bEdiDoc:createOutbound` 等授权），而兄弟 mutation（markSent/retry/activate/...）有角色授权可调——同一页面按钮权限行为不一致。`%dev`/`%prod` profile enable-action-auth=false 时无运行时差异（仅 UI 菜单/按钮可见性授权链缺失）。project 内对照：cs 域 0 FNPT、purchase 30 FNPT——注册密度本不统一，但 b2b 域**内部**兄弟 mutation 有/无并存构成明确缺口。
- **问题**：D5 权限注解/注册完整性（test profile 下功能不可达 + UI 授权链缺口）。
- **建议修复方向**：action-auth.xml 补 4 条 FNPT（对齐兄弟条目角色：createOutbound/createInbound→B2B 管理员；promoteToTesting/promoteToCertified→B2B 管理员）。
- **arm-index 裁决**：新增（grep「promoteToTesting action-auth/FNPT 缺注册」arm-index 零命中；P2-RC-066 记录的是 REST 路径维度且确认过 handleInboundWebhook 的 FNPT 存在）。

### P3-CK-b2b-012（D10）not-found 误用非法迁移错误码 + markError 日志方向启发式错标入站为出站

- **控制点 A**：`ErpB2bEdiDocBizModel#requireDoc`（L175-182：doc==null 抛 `ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION` + 仅 ARG_EDI_DOC_ID——「不存在」报成「非法迁移」）与 `ErpB2bAsnMatchPurchaseOrderProcessor#requireAsn`/`ErpB2bAsnCreateReceiveFromAsnProcessor#requireAsn`/`ErpB2bAsnRetryMatchProcessor#requireAsn`（同构抛 `ERR_B2B_ASN_ILLEGAL_TRANSITION`）——`ERR_B2B_EDI_DOC_NOT_FOUND`/`ERR_B2B_ASN_NOT_FOUND`（Errors L72-77, L87-91）已定义零使用（O-11 扩展 12 个错误码中 8 个零使用）
- **控制点 B**：`ErpB2bEdiDocBizModel#markError`（L115：`doc.getRelatedBillType() != null ? DIRECTION_OUTBOUND : DIRECTION_INBOUND`——入站 Doc 的 relatedBillType 恒非空（webhook 成功路径=PO_ORDER、失败路径=ASN_INBOUND），出站 Doc 同样非空（AR_INVOICE）→ 该启发式**恒判 OUTBOUND**：入站解析失败的 EdiLog direction 误记出站（对照 createInbound/writeEdiLog 自身均正确记 INBOUND）。
- **证据**：排错面：按 direction 过滤 EdiLog 时入站错误全部错位；错误码语义面：客户端收到「状态机非法迁移」而非「单据不存在」。
- **问题**：D10 错误归类/日志数据质量（无数据损坏）。
- **建议修复方向**：requireDoc/requireAsn 改用 NOT_FOUND 错误码；markError 方向改经 EdiDoc 方向判别（relatedBillType∈入站集合或经 format.direction）。
- **arm-index 裁决**：新增（grep「NOT_FOUND 误码/direction 启发式」arm-index 零命中）。

### P3-CK-b2b-013（D3）blockingLevel ORM defaultValue="10" 与 dict INFO/WARN/ERROR 不符——CRUD 直存路径落无效字典值

- **控制点**：`module-b2b/model/app-erp-b2b.orm.xml` L173（`<column name="blockingLevel" ... ext:dict="erp-b2b/blocking-level" mandatory="true" defaultValue="10" ...>`）对照 dict `erp-b2b/blocking-level`（选项 INFO/WARN/ERROR，无 10/20/30 数值编码）与 `ErpB2bConstants` L37-39（BLOCKING_LEVEL_INFO="INFO" 等）。
- **证据**：命名路径（createInbound/createOutbound/markError）显式写 INFO/ERROR 不受影响；`ErpB2bEdiDoc__save` 直存未带 blockingLevel 时 DB 默认落 "10"——非 dict 值：前端渲染失配 + 任何按 blockingLevel 过滤的消费方（如未来 error-blocks-flow）三态之外。疑为早期数值编码方案残留。
- **问题**：D3 dict 一致性（ORM 保护区域——修复走 dual-agent-approval）。
- **建议修复方向**：defaultValue 改 "INFO"（或删除依赖 mandatory+应用层兜底）。
- **arm-index 裁决**：新增（grep「defaultValue 10/blockingLevel dict」arm-index 零命中；A1.7/A1.8 ORM 规范审计未覆盖此列）。

### P3-CK-b2b-014（D5）webhook 不校验伙伴档案状态与生效性——SUSPENDED/TERMINATED 伙伴仍可推送建 ASN；payload 无大小上限（错误码已定义未用）

- **控制点**：`ErpB2bAsnHandleInboundWebhookProcessor#handleInboundWebhook`（L59-63 `findPartnerProfileByCode(partnerCode)` 仅按 code 查——**无 status 过滤**（PRODUCTION/TERMINATED/SUSPENDED 均通过）；L66-71 验签后无 payload 大小检查）对照 `Errors` L79-82（`ERR_B2B_EDI_PAYLOAD_TOO_LARGE` + ARG_PAYLOAD_SIZE/ARG_MAX_SIZE 已定义零使用）。
- **证据**：partner-onboarding.md 语义：SUSPENDED/TERMINATED 伙伴不应继续交换 EDI（UC-B2B-007 终止合作后下线）；当前终止伙伴的 webhookSecret 仍有效即可继续注入 ASN（进入匹配/收货链）。payload 无上限 →超大报文直入 CLOB（EdiLog.requestPayload）与 XML 解析（DoS 面；XXE 已防但实体膨胀未防）。
- **问题**：D5 入参边界 + 伙伴生命周期联动缺失（低危——需持有有效 secret）。
- **建议修复方向**：findPartnerProfileByCode 补 `status=PRODUCTION`（或非 TERMINATED/SUSPENDED）过滤；payload 超 config 上限抛 PAYLOAD_TOO_LARGE。
- **arm-index 裁决**：新增（grep「伙伴状态 webhook/SUSPENDED 推送」arm-index 零命中；P1-RC-080 覆盖的是状态机推进维度）。

### P3-CK-b2b-015（D6）matchPurchaseOrder 缺日期校验与通知链 + createReceiveFromAsn vendorId 来源与 owner doc 漂移

- **控制点 A**：`ErpB2bAsnMatchPurchaseOrderProcessor#matchPurchaseOrder`（全文无 estimatedArrivalDate vs PO 要求日期比较——asn-processing.md §4.1 步骤 4「estimatedArrivalDate > PO 要求日期 → blocking_level=WARN，通知采购员」零实现；PO 关闭路径 L59-64 仅 remark+markEdiDocError，§4.3「通知管理员」与 plan 2200-1 L146「PO 已关闭→blockingLevel=ERROR + **通知**」的通知链零实现——`IErpSysNotificationBiz` 在 Processor 零注入）
- **控制点 B**：`ErpB2bAsnCreateReceiveFromAsnProcessor#createReceiveFromAsn`（L72 `receive.setSupplierId(po.getSupplierId())`——asn-processing.md §6.1 逐字「vendorId = **ASN.partnerId**」；实现取 PO 侧。匹配场景二者应相等（ASN 按 PO 匹配），仅数据异常时取值不同且 PO 侧更稳——登记为 doc-code 漂移供 owner doc 裁决）
- **证据**：日期校验与通知是 §4.1 流程图明列步骤；通知链在 RC-R1.36 为 job 侧补齐（onboarding-monitor-alert），match 侧仍空白。
- **问题**：D6 匹配验收项部分缺失（低危——超期到货无预警）+ 文档口径漂移。
- **建议修复方向**：match 补日期比较（超期 → EdiDoc blockingLevel=WARN + notify 采购员）；owner doc §6.1 修正 vendorId 来源表述或实现改取 ASN.partnerId。
- **arm-index 裁决**：新增（grep「estimatedArrivalDate 校验/通知采购员」arm-index 零命中；§4.4 超时升级 Non-Goal 已声明但步骤 4 日期校验不在 Non-Goal 清单）。

### P3-CK-b2b-016（D9）createOutbound 仅取首个适用 Provider + parseToAsn 逐行查映射表 N+1

- **控制点 A**：`ErpB2bEdiDocCreateOutboundProcessor#createOutbound`（L40-46：`List<IErpB2bEdiProvider> providers = ediRegistry.findOutboundProviders(relatedBillType); ... IErpB2bEdiProvider provider = providers.get(0);`——edi-formats.md §2.1 派发流程「遍历所有 active 格式……outbound=true → 创建出站 TO_SEND EdiDoc」为**每格式一 Doc**，实现只建首个；多格式并存（UBL+X12）时第二格式静默丢失。当前仅 1 个出站 Provider 实存，无实际触发面）
- **控制点 B**：`ErpB2bAsnHandleInboundWebhookProcessor#parseToAsn`（L149-165：`for (ParsedLine ...) { codeMappingResolver.resolveInbound(...) }`——每行一次 `ErpB2bCodeMapping` 查询；estRows=30 典型规模可接受，超大报文行数不受控（与 014 的无上限叠加））+ `ErpB2bOnboardingMonitorJob#resolveFormatIds`（L198-207 逐 code findList 同构小 N+1）
- **证据**：两项均边界性能/完整性问题，无正确性损坏。
- **问题**：D9 性能 + D4 派发完整性（低危）。
- **建议修复方向**：createOutbound 循环建 Doc（每 provider 一个，UNIQUE 键天然分流）；resolveInbound 批量化（一次 `in(externalCode, ...)` 查询建 Map）。
- **arm-index 裁决**：新增（grep「providers.get(0)/resolveInbound N+1」arm-index 零命中）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / arm 条目 | b2b 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-pur-003` 族（CRUD update 无守卫全域族） | EdiDoc/Asn/PartnerProfile 状态字段 + 9 stub BizModel 裸 update 面 | 同型登记为 P2-CK-b2b-009 |
| orgId 隔离族（`P2-CK-fin2-007`/`P2-CK-mfg-007`/`P2-CK-hr-003`④） | b2b 表现为**写侧** writer 缺口（数据本身 orgId=null，读侧 filter 无独立意义） | 并入 P2-CK-b2b-003 登记（非读侧同型计数） |
| `P0-CK-mfg-001`（固定幂等键吞增量——ASN→收货若同型核查） | `createReceiveFromAsn` **无** findByRelatedBill 式幂等查找：防重= MATCHED 状态守卫 + versionProp 乐观锁 + `UK_PUR_RECEIVE_CODE_ORG`（code=RCV-FROM-ASN-{asn.code}，per-ASN 唯一）；ASN 为 1:1 建库语义非增量累计流程，无「固定键吞增量」形态 | **核查不适用**（无同型；1:N 增量语义缺失另登 007） |
| `P1-CK-fin-003`（post 幂等 null 传导面） | b2b 无过账/凭证代码（过账发生在 purchase 域 approve 链，OA-03 已修 orgId 透传） | **不适用** |
| cron 键漂移家族（P3-CK-crm-014 / mfg-013 / inv-021 / sal-023 / qa-019 / hr-012 / cs-014） | `erp-b2b-onboarding-monitor.job.yaml` cronExpr 消费 `erp-b2b.onboarding-monitor-cron`——与 bean 内层空值跳过门控**同一键**（对齐 R1.35/lead-scoring-recalc 修复形态）；enabled 为独立调度开关（javadoc 明示双层设计） | **核查不成立**（单键模式，非漂移家族） |
| `P2-RC-062`（watch-only todo — isActive 派发门槛） | `findFormatByCode` 仍仅 eq(code) 判 null（两 Processor L91-97/L70-76 现状一致） | 复用不展开 |
| `P2-RC-063`（watch-only todo — 非终态幂等返回已有记录 vs 抛异常） | checkDuplicate 现状仍抛 ALREADY_PROCESSED（含 002 的 error 路径形态） | 复用（002 为不同控制点新增） |
| `P2-RC-064`（watch-only todo — parseToAsn 后不自动 archive） | archive 调用点仍仅 matchPurchaseOrder L95 | 复用不展开 |
| `P2-RC-065`（watch-only todo — 出站代码映射未应用） | `UblInvoiceEdiProvider.generatePayload` 仍零调 resolveOutbound；载荷为样例骨架（PayableAmount 硬编码 0.00、invoice 实体加载后未读取） | 复用不展开（样例定位 javadoc 自认） |
| `P2-RC-066`（watch-only todo — REST 路径未实体化） | 仍仅 GraphQL mutation | 复用不展开 |
| `P2-RC-067`（watch-only todo — retry 清 error 字段） | retry L129 `doc.setError(null)` 现状一致 | 复用不展开 |
| `P2-RC-068`（watch-only todo — MFT 边界：retryIntervalMin 未用于退避 sleepSilently(1) 固定 + 证书过期自动停用缺失） | TransportManager L88/L172-178 与 MftCertificateBizModel 现状一致 | 复用不展开 |
| `P1-MA2-073`（resolved via deferral — needsWebService 异步派发） | 两 Provider needsWebService()=false，无异步队列 | 复用 deferral 注记 |
| `P1-MA2-088`（resolved R1.28 — webhook eventId 幂等） | 确定性 code + remark 检查在位；其 UK 并发兜底被 orgId=null 削弱 | 修复在位验证 + 残余面归 003 |
| E2E watch-only（plan 2026-07-14-0508-1 L108 — createOutbound 跨域查 ErpSalInvoice 缺失抛 ERR_B2B_EDI_PARSE_FAILED） | `UblInvoiceEdiProvider.generatePayload` L59-63 现状一致 | 复用 watch-only 裁决 |
| TransportManager wired-but-uncalled | owner doc state-machine.md「实现约定」已注记（Deferred transport 集成范畴） | 不另登 |
| `P1-RC-080`（done RC-R1.36） | 5 mutation 状态守卫 + 门槛 + goLiveDate/archivedAt + 24h 监控 job | 修复在位验证（见下节） |
| OA-03（fixed 2026-08-25 — receive.orgId 透传） | `ErpB2bAsnCreateReceiveFromAsnProcessor` L71 | 修复在位验证（见下节） |

## 验证为正确（显式排除，防误报）

- **OA-03 修复在位 + 透传链完整**（任务点名核查）：`ErpB2bAsnCreateReceiveFromAsnProcessor#createReceiveFromAsn` L71-74 四连透传 `receive.setOrgId(po.getOrgId())` / `setSupplierId(po.getSupplierId())` / `setWarehouseId(po.getWarehouseId())` / `setCurrencyId(po.getCurrencyId())`——**orgId/warehouseId/supplierId（+currencyId）全部在位**（OA-03 只需修 orgId 是因为其余三个本来就有）；行级 L161 `receiveLine.setWarehouseId(receive.getWarehouseId())` 复用头仓库（Decision (d)②）。C19 集成测试去遮蔽断言在位：`TestErpC19B2bAsnAutoReceiveLandedCost` L166-167 `assertEquals("2", draft.getOrgId())`。
- **RC-R1.36（P1-RC-080）修复在位**：`ErpB2bPartnerProfileBizModel` 5 mutation 全部经 `ErpB2bPartnerProfileStateMachine` 守卫（promoteToTesting 仅 REGISTERED + `assertProfileComplete` 五字段；promoteToCertified 仅 TESTING + 通过率 ≥0.9（零行=0 拒绝 + 1e-9 浮点容差）+ TC-001/TC-004 关键用例必过 + 认证清单必检项全过（空清单拒绝）；activate 仅 CERTIFIED + goLiveDate 写入；suspend 四源合法；deactivate 非终态 + archivedAt 写入）；Bean 矩阵 12 边/transitions() 与 owner doc 图一致；TERMINATED 终态无出边。
- **EDI 信封状态机守卫与测试对齐**（任务点名「ACKNOWLEDGED→markSent 非法守卫」）：markSent 仅 TO_SEND、markAcknowledged 仅 SENT、markError 已按 D-B2B-3 收紧为 {TO_SEND,SENT,RECEIVED}（终态/ERROR 本身不可 markError）、cancel {TO_SEND,SENT,ERROR}、archive 仅 RECEIVED、retry 仅 ERROR；终态三值 Bean 不编码出边 + BizModel assertCan 双保险；`TestErpB2bEdiEnvelope` 六态流 + 非法迁移 + `TestErpB2bEdiDocStateMachineMatrix`/`TestErpB2bAsnStateMachineMatrix` 全矩阵断言在位。Bean 抛 common 码 → BizModel/Processor 映射领域码 + cause 保留（契约 §7 范式）。
- **dict 死状态均已有 owner doc 裁决**（D3/B2 核查）：`edi-doc-state` 的 TO_CANCEL（D-B2B-1 预留，两步取消 Deferred）与 `asn-status` 的 CANCELLED（D-B2B-2 预留，零 writer）在 state-machine.md §2 注 + ASN 段注显式登记，Bean 不编码其边；web 消费面无功能性死值消费（`ErpB2bEdiDoc.view.xml` L113 `visibleOn ${state != 'CANCELLED'}` 为可达值；`ErpB2bAsn.view.xml` L24 dangerVals 为通用调色板数组；dashboard edi-detail 页 `${doc.state}` 透传渲染无硬编码分支）。
- **HMAC 验签实现正确**（任务点名「webhook 签名验签」）：HmacSHA256 + hex 编码 + `MessageDigest.isEqual` 常量时间比较（L218-219，防时序侧信道）+ secret null/空签名拒绝 + 签名必填 config 默认 true（`DEFAULT_WEBHOOK_SIGNATURE_REQUIRED=true`，与设计 §2.2 X-Signature 必填一致；测试 testSignatureNotRequired 验证显式关闸路径）；UBL XML 解析 XXE 三防护在位（disallow-doctype-decl + 双 external-entities false）。
- **webhook 幂等主路径正确**（R1.28 修复在位）：eventId 非空 → `ASN-WEBHOOK-{eventId}` 确定性 code + remark 串查前置拦截（testWebhookIdempotentDuplicate 串行重复抛 DUPLICATE_EVENT ✓）；eventId==null → 时间戳回退避免塌缩（testWebhookNullEventIdFallbackDistinct ✓）；并发 UK 兜底的 orgId=null 削弱归 003（b2b 站点残余面，非修复回归）。
- **`addOrderField` 布尔参数 6 处全部正确**（C1.2 校准先例：第二参=desc）：findPurchaseOrder×2 `("id", true)`=id 降序确定性 ✓、findFormatByCode×2 `("code", false)`=升序 ✓、isDuplicateEvent `("id", true)` ✓、TransportManager.findActiveConfig `("id", false)` ✓（注释写 mftId 实排序 id——注释漂移无行为影响）、UblInvoiceEdiProvider `("invoiceDate", true)`=取最新发票 ✓。
- **`FilterBeans.eq(name, null)` 生成 IS NULL**（先例沿用，本报告用于 002 定性）。
- **D1 机械扫描近零**：`@Inject private`=0（全包级可见）、`System.currentTimeMillis/new Date`=0、`extends RuntimeException/Exception`=0（业务异常全部 NopException + ErpB2bErrors 中文描述）、字典字符串 `==` 比较=0、`printStackTrace`=0；唯一 `LocalDateTime.now()` 见 010。
- **beans.xml 接线完整（D4）**：Registry×2 + Provider×2 + CodeMappingResolver + TransportManager + MockAdapter + Processor×6 + StateMachine×3 + job bean 共 17 bean 全注册（ioc:collect-beans by-type 聚合 SPI），无孤立声明/漏调；唯一 job.yaml invoker `bean: erpB2bOnboardingMonitorJob, method: execute` 与 bean id 对应——单键 cron 模式（非漂移家族，见注记表）。
- **job 监控逻辑主干正确**：cron 空值跳过 + runMonitor 会话内完成（MANAGED 实体）+ 逐伙伴 try/catch 失败隔离 + SCAN_LIMIT 200 + 窗口 dateBetween/dateTimeBetween 规避 XMeta 白名单 + allowedFormats 解析容错（非法 JSON 跳过）+ 重复告警无去重已 javadoc 登记 successor（对齐 R1.4 范式）；org 锚点与 Doc orgId 不相交的盲区归 003。
- **createReceiveFromAsn 并发/幂等防护在位**：MATCHED 单向守卫（串行重入拒绝）+ `version` 乐观锁（并发双开一胜一败回滚）+ `UK_PUR_RECEIVE_CODE_ORG`（code=RCV-FROM-ASN-{asn.code} 每 ASN 唯一）；行级 materialId/物料存在守卫抛领域码强回滚（除 webhook 路径 materialId 恒 null 的 001 外，手工补录路径行为正确）；amount=unitPrice×qty HALF_UP scale=4 与 edi-formats.md 附录 Decision (b)① 一致。
- **平台 `CrudBizModel.requireEntity` 语义实证**（nop-entropy `CrudBizModel.java` L898-945）：not-found 抛 `UnknownEntityException`（ignoreUnknown=false 路径），PartnerProfileBizModel 5 mutation 的 `requireEntity(profileId, null, ctx)` 无 NPE 风险。
- **乐观锁全实体在位（D5）**：orm 13 实体全部 `versionProp="version"`（L136/165/215/267/301/331/364/414/445/480/512/571/612 逐一核对）。
- **retryMatch 幂等短路正确**：MATCHED/RECEIVED_TO_STOCK 经 `isIdempotentRetryStatus` 短路无副作用；「回到 RECEIVED 再委托」分支在 CANCELLED 不可达现状下为死代码但零行为影响（CRUD 旁路面归 009）。
- **CodeMappingResolver 未找到语义正确**：返回原值 + WARN（edi-formats.md §6.2「未找到 → 保留原值」）；IDaoProvider 只读跨实体有 javadoc 理由（系统级查表无用户上下文过滤需求，对齐 logistics 先例）。
- **markError 后 blockingLevel=ERROR 落 EdiDoc**（PO 关闭路径）符合 §4.3「blocking_level=ERROR」（通知缺失维度归 015）。

## arm-index 复用 or 新增裁决（汇总）

- **新增** 16 条（arm-index 相关符号 grep 零命中）：`AsnLine materialId writer`/`error Doc 键塌缩`/`orgId 写侧缺口`/`erp-b2b.enabled 零消费`/`retry 方向`/`负数量穿透`/`部分收货 1:N`/`超量聚合`（+pur-003 族同型 009）/`LocalDateTime.now`（ast2-016 家族新站点）/`FNPT 缺注册`/`NOT_FOUND 误码`/`defaultValue 10`/`伙伴状态 webhook`/`日期校验缺失`/`providers.get(0)`。
- **复用（不重复登记，注记）**：P2-RC-062/063/064/065/066/067/068（watch-only todo 修复方向已立案）、P1-MA2-073（deferral）、E2E watch-only（plan 0508-1）。
- **同型登记**：P2-CK-b2b-009（pur-003 族）；P2-CK-b2b-003（orgId 族写侧形态，独立 finding）。
- **同型核查不成立/不适用**：P0-CK-mfg-001（固定幂等键吞增量——无 findByRelatedBill 幂等形态）、P1-CK-fin-003（无过账面）、cron 键漂移家族（单键模式）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 1 | P1-CK-b2b-001 |
| P2 | 8 | P2-CK-b2b-002..009 |
| P3 | 7 | P3-CK-b2b-010..016 |

按主维度：D8×2（001、003）、D2×1（002）、D4×1（004）、D3×2（005、007，013 亦 D3 但归 P3 桶见下）、D5×3（006、009、011，014 主 D5）、D6×2（008、015）、D1×1（010）、D10×1（012）、D9×1（016）。（精确主维度归属：001 D8、002 D2、003 D8、004 D4、005 D3、006 D5、007 D3、008 D6、009 D5、010 D1、011 D5、012 D10、013 D3、014 D5、015 D6、016 D9。）

同型/复用裁决：同型登记 2（009 pur-003 族、003 orgId 族写侧形态）+ arm-index 复用不登记 9 项（RC-062..068、MA2-073、E2E watch-only）+ 修复在位验证 3 项（P1-RC-080/RC-R1.36、P1-MA2-088/R1.28、OA-03）+ 同型核查不适用 3 项（mfg-001、fin-003、cron 键漂移家族）。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-b2b-service 40 文件全量通读（无抽样）；平台源码实证 4 处（ReflectionBizModelBuilder 默认权限推导 deny-by-default、GraphQLActionAuthChecker.isAllowAccess、CrudBizModel.requireEntity、FilterBeans.eq null 先例沿用）；orm 核对（13 实体 versionProp / UK 清单含 UK_EDI_DOC_FORMAT_BILL + UK_B2B_ASN_CODE_ORG / Asn 无 blockingLevel 列 / blockingLevel defaultValue="10"）；接线核对（beans.xml 18 bean / job.yaml 单键模式 / action-auth 13 FNPT vs 17 mutation / xbiz 空 extends）；dict 4 个逐一比对常量；测试 9 文件交叉验证（TestErpB2bAsnInbound 的 supplierPartNo-only 断言、AsnInventoryIntegration 绕过 webhook 直 seed materialId、EdiEnvelope 六态流、C19 OA-03 断言）；arm-index b2b 9 条 + OA-03 bug doc + plan 2200-1/0508-1 勾选与裁决文本逐条对照。
- **未深查**：`erp-b2b-web` AMIS/flux 页面契约 drift（归 C8.2，仅抽查 dict 死值消费与 action-auth）；`erp-b2b-api` 骨架；`erp-b2b-dao` 生成物（问题应回溯模型）；测试代码自身正确性（仅用于行为交叉验证）；TransportManager/MockTransportAdapter 全链（owner doc 已 Deferred wired-but-uncalled，仅读代码未深审重试时序）；UBL 报文规范符合度（样例定位已由 javadoc/owner doc 声明）；`ErpPurOrderLine.receivedQuantity` 的 purchase 域维护正确性（跨域，purchase 报告范畴）；H2/MySQL 对 NULL 唯一约束的行为差异（按标准 SQL 论证，未实测）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-b2b-001**——「webhook 路径 materialId 恒 null」的定性依赖「无其他自动补录路径」（grep 实证零 writer）。若产品语义是「webhook 只建骨架、人工经 CRUD 补录 materialId 后再 match/receive」，则自动链断裂降级为设计取舍（但 plan 2200-1 L144 勾选与 A1.47 接受声明均承诺 webhook 路径直接产出 materialId，且 §4.2 设计如此）。建议集成测试实证：webhook 建单 → 断言行匹配/超额标志从未触发 + `createReceiveFromAsn` 抛 `ERR_B2B_ASN_LINE_MATERIAL_REQUIRED`。
  2. **P2-CK-b2b-003**——UK(code,orgId) 对 orgId=NULL 不去重依赖标准 SQL 语义（MySQL/PostgreSQL/H2 一致，未在本项目 DB 实测并发复现）；「profile.orgId 有值 → 监控盲区」取决于部署是否为伙伴档案配置 orgId（CRUD 可编辑列）。
  3. **P2-CK-b2b-004**——`erp-b2b.enabled` 零消费的 grep 范围为 module-b2b（service+web main）+ app-erp-all resources；若存在本报告未覆盖的装配层 gate（如 quarkus 模块按 profile 装配排除），则降级 not-a-problem 并仅需修正 owner doc 论据。
  4. **P2-CK-b2b-005**——入站 ERROR Doc retry→TO_SEND 的用户触达面取决于 EDI 事务页面是否对入站 Doc 暴露重试按钮（web 未深查）；语义错误本身确定，触达面影响定级。
  5. **P2-CK-b2b-007**——部分收货 1:N 的产品取舍需 owner doc 裁决：若「本期 1:1 单发建库」被接受为显式取舍，则降级为 plan 勾选失真修正（P3）+ doc 补注；行级 receivedQty 计数列属 ORM 保护区域（dual-agent-approval）。
