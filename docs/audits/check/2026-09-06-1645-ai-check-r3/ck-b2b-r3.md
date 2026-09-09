# ck-b2b-r3 — b2b U17 五维符合性审计报告（ai-check-r3 M1.14）

> 工作项：M1.14（U12 + U13 + U18 + U17 + U19 各 × 五维全格，冻结清单 §4 映射表第 14 行；本报告 = U17 b2b 格，其余四域格分别见 `ck-crm-r3.md` / `ck-cs-r3.md` / `ck-contract-r3.md` / `ck-drp-r3.md`）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `5eb2c4dbe9faa68a685b5a90728cc2493b477b76`（2026-09-09；计划基线 `2c1c1ef25` 后唯一推进 = 同批姊妹审计产物提交，生产代码零变化）；脏面 = 2 条 untracked 计划文件（本计划 + 姊妹 `0547-3`），tracked 零修改。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2 + §3.3 U17 行 + §6 勘误 E1。执行期仅在报告与计划勾选注记追加证据，判定标准零改动。
> 范围：b2b 全域（C 级，无切片细分）——EDI 文档（EdiDoc/EdiFormat/EdiLog + CreateOutbound/CreateInbound Processor + 状态机 Bean 12 边）/ASN（Asn/AsnLine + HandleInboundWebhook/MatchPurchaseOrder/CreateReceiveFromAsn/RetryMatch 4 Processor + 状态机 Bean 2 边）/MFT（MftConfig/MftCertificate/MftLog/TransportManager transport SPI + CodeMappingResolver/UblInvoiceEdiProvider SPI 族）/伙伴（PartnerProfile + 状态机 Bean 12 边 + PartnerCredential/CertificationChecklist/TestExchange + OnboardingMonitorJob）（`module-b2b/erp-b2b-{dao,service,web}` src/main）；owner docs `docs/design/b2b/`（asn-processing/edi-formats/managed-file-transfer/partner-onboarding + state-machine/README/use-cases）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎内部归 fin-1（b2b 无过账面，EdiPosting 测试为 EDI 文档生命周期非财务过账）；common 抽象族行为归 U20（13/13 BizModel 基类接入调用点合规）；聚合横切面归 U21（action-auth b2b 注册在位，FNPT 缺口 b2b-011 归并）；notify 消费点归 U11（OnboardingMonitorJob 经 `IErpSysNotificationBiz` 消费合规）。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U17 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（域焦点：EDI 文档状态机/MFT 证书链/伙伴凭证（webhook processor 族）；共性②⑧⑨⑮）+ 维度⑮断言抽样 4 doc × 13 断言 | 反模式族近零（RuntimeException=0/@Inject private=0/@Transactional=0；System.currentTimeMillis=0，唯 `LocalDateTime.now()` ×1 = b2b-010 站点 open 归并）；checker 19 规则 = M0.3 快照行零漂移；`__XGEN_FORCE_OVERRIDE__` 13 处全为 erp-b2b-meta dict.yaml 校验点零手改；聚合器含 `/erp/b2b/auth/erp-b2b.action-auth.xml`。IDaoProvider/IOrmTemplate 10 文件逐文件核验：CodeMappingResolver/UblInvoiceEdiProvider 2 处合规样板在位，8 处无注释（webhook/match/receive/createInbound/createOutbound/TransportManager 族）——其中跨域写面立 P2-CK-b2b-017-r3、注释缺位并入其裁决面。15/15 无跳维：①13 实体 orm 生成 + service Bean/Processor pass；②**finding**——BizModel 内部全 I*Biz（PartnerProfile 经 TestExchange/CertificationChecklist Biz、job 注入 4 I*Biz），但 CreateReceiveFromAsn 跨域写 `ErpPurReceive/Line` 经裸 DAO（:68-80,139-169）且 data-dependency-matrix §2.2:94 b2b 行 S 列「待深化」、§2.4 Java 边表无此边 = **新立 P2-CK-b2b-017-r3**（未登记未裁决的跨域写边）；③全 NopException + ErpB2bErrors 23 码 pass（2 处不可达防御分支 IllegalArgumentException = P3-CK-b2b-020-r3 minor）；④机械全零 + 跨域写同 @BizMutation 事务（javadoc L109-110 强一致声明）pass；⑤CoreMetrics 全覆盖唯 b2b-010 一站点归并；⑥13/13 AbstractErpCrudBizModel pass；⑦notGenCode ×3（md partner/organization/material）声明齐全 pass；⑧**finding**——3 状态机 Bean 声明式（12+11+2 边矩阵）合规，dict 死状态：edi-doc-state TO_CANCEL（D-B2B-1 裁决 ✓ 不重开）、asn-status CANCELLED（D-B2B-2 裁决 ✓）、**mft-status PENDING/RECEIVED/RETRYING 三值零 writer 零裁决**（TransportManager 仅写 SENT/FAILED/DEAD_LETTER :78,98-99；RETRYING 与 managed-file-transfer.md:257-258 重试策略声明相悖）= **新立 P3-CK-b2b-018-r3**；partner-status REGISTERED 无系统 writer = b2b-009 归并补证；blockingLevel defaultValue="10" = b2b-013 归并；⑨无审批流需求（伙伴推进为管理员 mutation）+ job 接线单键 `@cfg:` 模式 + 空值跳过门控 pass；⑩6 Processor Delta 覆盖点声明 pass；⑪无 tenantId 预置 pass；⑫9 测试 + 双 SM 矩阵 + FrozenClockExtension pass；⑬codegen 安全 pass；⑭action-auth 13 FNPT 保留层注册（4 mutation 缺口 = b2b-011 归并）pass 边界内；⑮4 doc × 13 断言漂移 4（2 新立 + 2 归并） | **finding**（4 新立：1 P2 + 3 P3；归并 16，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + EDI/伙伴管理页面 | `npm run validate:flux`：FLUX_PAGE_ERROR_COUNT 0 / 999 页（erp 855）；整体 exit 1 = 325 ERR 全 variant 族既有外部漂移（b2b 命中 10 条同族不立项）；`component="AMIS"` 0、ORM `ext:web-renderer="flux"` 无缺失。页面面 28 page.yaml + 2 flux.yaml 全量清点（M0.4 重放零漂移）：13 实体 main+picker codegen stub 族 26 页全 PASS（保留层继承、`git status module-b2b` 零脏面）；手写页仅 dashboard/asn-flow + edi-detail 孪生（`@query:ErpB2bAsn__*`/`ErpB2bEdiDoc__get`+`ErpB2bEdiLog__findPage` REST /r/，i18nEn 24/30 + 10/15）；`graphql:` 3 处全 labelProp；docStatus 死状态样式分支 0 命中。**事实记录**：伙伴上线无手写 wizard 页（promoteToTesting/Certified 经 BizModel mutation，页面为 PartnerProfile 标准 CRUD）——非违规。E2E：E2E_ENGINE 缺省 flux、b2b 3 business-actions + 2 value + 1 negative spec selector 纪律 0 命中、页面级 GraphQL 断言 0（value spec 经 GraphQLClient 重放数据源取原始值 = runbook 数据驱动数值断言层 sanctioned 范式）。r1 b2b 族无 DIM-F finding；b2b-011 FNPT 缺口原站点归并 open 不重开 | **pass**（归并态注记） |
| **DIM-S seed 数据** | §1.3 全套 + b2b 13 表 seed（M1.5 批含 EDI 段） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；`_init-data` porcelain 空（零 seed 变更）；资产清点 **372 CSV + 1 SQL** = 冻结登记值；b2b deploy `_seed_*.sql` 命中 = 0（登记处一致）；erp_b2b_* **13 CSV** 精确在册；EDI 段自洽抽查：EDI-SEED-2026-001（ARCHIVED/INFO，RELATED_BILL ASN-SEED-2026-001 与 asn.csv CODE 精确互链，SENT/ACK 时间序一致）↔ ASN#1（RECEIVED_TO_STOCK 终态，SOURCE_EDI_DOC_ID 空 = 出站向一致）↔ edi_format#1 引用完整；b2b 非过账域 posted 列零命中 N/A 带理由 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P2 EDI outbound 清单行覆盖 | `mvn test -pl module-b2b/erp-b2b-service` **80/0/0/0 全绿 BUILD SUCCESS**（= 锚点 80 零增量）；覆盖对账：注解动作 20 / BizModel 13 vs `_cases` 资产根 9（EdiPosting/EdiEnvelope/AsnInbound/AsnInventoryIntegration/MftTransport/AsnCrudSmoke/FkNameLoader/SM 矩阵族）；**P2 行在位**：`TestErpB2bEdiPosting`（EDI outbound 链）+ `TestErpB2bEdiEnvelope` ✓；`SnapshotTest.RECORDING` = 0 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0** + `--self-test` **PASS**；b2b 探针族（CAT-1 22）维持清零零回归；WHITELIST b2b 条目 **0**（记录条目数 0，与计划基线一致）；`grep -L @Locale` = 空；meta/i18n 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ai-check-index.md` §Finding 追踪（同域报告 `ck-b2b.md` C7.3 全 16 条逐一比对）+ r2 只读目录（无 b2b 同型新独立登记）+ §Mission 基线快照。**本轮新立 4 条**（1 P2 + 3 P3）；历史 16 ID 零覆写（实仓核对 2026-09-09：r1 族最大号 CK-b2b-016，新立自 017 起）。
> **索引行失配注记（lesson-11 回填缺口同型）**：`P2-CK-b2b-008`（matchPurchaseOrder 数量校验粒度）索引行状态 `fixed`、终态证据「F1.3：统一基类接入自动生效」——与 finding 内容（数量校验粒度，非 pur-003 守卫族）失配，疑似与相邻行 `P2-CK-b2b-009`（CRUD 守卫，恰为 pur-003 族且索引 open）状态/证据互换登记。本裁决按 **finding 内容**处置（008 现症 HEAD 复核在位 → 归并 open + 回填缺口注记；009 维持 open 归并），历史 ID 零覆写，状态回填归索引 owner 流程。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 0 条

无（r1 b2b 族无经本切片复核确认的 fixed 终态；b2b-008 索引 fixed 态经内容复核证伪，见上注记）。

### 2.2 归并（同型 open 追加证据至原 ID）— 16 条

> 16 条 r1 ID 于 T0 全量逐条现症复核在位（域走查逐条 file:line 证据），原 ID 状态不动（008 按内容归并 open + 索引失配注记）。

| 原 ID | r3 复核证据（T0） |
| --- | --- |
| P1-CK-b2b-001 | webhook:156-160 仍 `line.setRemark(internalMaterial)`，setMaterialId 全域零 writer；MatchPurchaseOrder:72-75 匹配恒 null 跳过；CreateReceiveFromAsn:124-130 webhook 路径必抛（维度⑮ asn-processing.md:256-259 抽样命中同点）原样 |
| P2-CK-b2b-002 | webhook catch:96-98 `createInbound(ASN_INBOUND, null, ...)` + checkDuplicate（CreateInbound:54-68）精确三元组错误键塌缩原样 |
| P2-CK-b2b-003 | setOrgId 全域仅 5 处透传；EdiDoc/Asn 建单零 setOrgId；job countEdiDocs orgId filter 在位原样 |
| P2-CK-b2b-004 | CONFIG_B2B_ENABLED 仅 ErpB2bConfigs:9 定义零消费（维度⑮ state-machine.md §L7「config-gated OFF 默认」抽样命中同点）原样 |
| P2-CK-b2b-005 | BizModel:127-128 注释自承 + 无条件 retryOutboundTargetStatus()；retryInboundTargetStatus() 零生产调用（维度⑮ state-machine.md §3 ERROR 重试断言命中同点）原样 |
| P2-CK-b2b-006 | getDecimalDirectChild:171-181 静默 null + signum 全域零命中 + :146-147 qty 直写原样 |
| P2-CK-b2b-007 | orm 零 receivedQty 列 + :86-87 首库即终态（维度⑮ README:99「1:N 部分收货」抽样命中同点）原样 |
| P2-CK-b2b-008 | L70-84 逐行独立比较 + L88-90 超额仅 remark 文本（remark 已英文化 `"Some lines exceed PO quantity (blockingLevel=WARN)"` = i18n 治理改动、行为面不变）；**索引行 fixed/F1.3 与内容失配注记**（见 §2 前言） |
| P2-CK-b2b-009 | defaultPrepareSave 仅 businessDate 兜底、defaultPrepareUpdate 未 override、9 stub 未变；F1.3 基类 13/13 接入在位但 status 列惰性 = 注册边界；**补证**：REGISTERED 初始态亦无系统 writer（维度⑮ partner-onboarding.md §Stage1 抽样命中）——CRUD 可直建 PRODUCTION 跳过阶段（违 partner-onboarding 业务规则 1） |
| P3-CK-b2b-010 | LocalDateTime.now() 仍在 OnboardingMonitorJob:220 原样 |
| P3-CK-b2b-011 | action-auth 仍 13 FNPT，createOutbound/createInbound/promoteToTesting/promoteToCertified 4 mutation 零注册（DIM-F 复核同证）原样 |
| P3-CK-b2b-012 | requireDoc:179 / requireAsn ×3 仍抛 ILLEGAL_TRANSITION；NOT_FOUND/PAYLOAD_TOO_LARGE/QUANTITY_MISMATCH 生产零使用；markError:116 方向启发式原样 |
| P3-CK-b2b-013 | orm:174 blockingLevel defaultValue="10" 非 dict 值原样 |
| P3-CK-b2b-014 | findPartnerProfileByCode 仅 eq(code)（webhook:175-181 无 status 过滤）原样 |
| P3-CK-b2b-015 | matchPurchaseOrder 无日期比较 + notify 仅 job 注入 + receive.setSupplierId(po.getSupplierId()):72 与 §6.1 漂移原样 |
| P3-CK-b2b-016 | providers.get(0) 仍在 CreateOutbound:46（维度⑮ edi-formats.md §96 抽样命中同点）+ resolveInbound 逐行 N+1（webhook:149-165）原样 |

### 2.3 新立 `-r3` — 4 条（1 P2 + 3 P3）

**P2-CK-b2b-017-r3**（DIM-B ② + 依赖登记）
- **控制点**：`ErpB2bAsnCreateReceiveFromAsnProcessor.java:68-80,120,139-169`——跨域写 `ErpPurReceive/ErpPurReceiveLine` 经 `daoProvider.daoFor(...).newEntity()/saveEntity` 裸 DAO（未走 `IErpPurReceiveBiz`）；旁证读侧 `ErpB2bAsnMatchPurchaseOrderProcessor.java:127-167`（ErpPurOrder/Line 读，无豁免注释）。
- **问题**：data-dependency-matrix §关键规则 2 限定跨域 S 写仅业财闭环；唯一明示例外（E3.5 finance 行）采 `IErpPurInvoiceBiz.save()` I*Biz command 形态；§2.2:94 b2b 行 S 列「待深化」、§2.4 Java 边表无 b2b→pur-dao 边、§4.2 S 写清单无此边——**跨域裸 DAO 写边未登记未裁决**；javadoc「核心零污染」（L32,67）裁决的是 schema 不加 asnId 列，未裁决写机制。
- **三态裁决**：新立（r1 16 条无跨域写机制形态）。P2（架构级旁路 + 依赖登记缺失；行为本身经 r1 行为审计验证正确，非即时数据风险）。
- **修复方向**：三选一——(a) 注册矩阵 §2.2/§2.4 边 + 写点补豁免注释（对齐 CodeMappingResolver 样板）；(b) 改经 `IErpPurReceiveBiz` command 方法（E3.5 形态）；(c) owner doc 显式登记 RAW-DAO 例外并说明不可用 I*Biz 原因。

**P3-CK-b2b-018-r3**（DIM-B ⑧/B2 dict 死状态族 mft-status 轴）
- **控制点**：`TransportManager.java:73-101`（仅终态 SENT/FAILED/DEAD_LETTER 各一笔 log，:78,98-99；sleepSilently(1) 固定 :88,174-178）+ mft-status.dict.yaml 6 值 + `managed-file-transfer.md:257-258`（maxRetries>0 → 状态→RETRYING 按 retryIntervalMin 重试声明）。
- **问题**：PENDING/RECEIVED/RETRYING 三值零 writer 零 owner-doc 裁决（TO_CANCEL/asn CANCELLED 均有 D-B2B-1/2 裁决而此轴无）——RETRYING 与 doc 重试策略声明直接相悖；中间重试尝试零日志（retryCount 恒 0 或 maxRetries 二值）。lesson 10 族 mft-status 轴，r1 与 MR1 波次均未扫此轴。
- **三态裁决**：新立（维度⑮ managed-file-transfer.md §重试策略抽样漂移；RECEIVED 依赖 SFTP 入站可 Non-Goal 豁免，PENDING/RETRYING 无豁免登记）。P3。
- **修复方向**：重试循环内写 RETRYING 中间 log（或 owner doc 显式登记「仅终态日志」+ dict 收窄）；与 transport Deferred successor 一并处理，需 owner doc 补注。

**P3-CK-b2b-019-r3**（DIM-B ⑮）
- **控制点**：`partner-onboarding.md:40`「webhookSecret | Webhook 签名密钥（**加密存储**）」vs `ErpB2bAsnHandleInboundWebhookProcessor.java:68,206-219`（HMAC 直读 `profile.getWebhookSecret()` 原文）+ 全域零 EncryptionHelper/encrypt 引用（grep 实证）+ orm webhookSecret 列无加密语义标记。
- **三态裁决**：新立。P3（密钥明文存 DB；managed-file-transfer.md 对 MFT 私钥有 AES-256 要求，webhookSecret 同级敏感无对应实现与 Deferred 登记——doc 断言 vs 实现漂移）。
- **修复方向**：partner-onboarding.md 修正为明文 + Deferred 登记，或引入 EncryptionHelper（对齐 MFT §私钥存储安全要求）。

**P3-CK-b2b-020-r3**（DIM-B ③，minor）
- **控制点**：`ErpB2bEdiDocBizModel.java:212`、`ErpB2bPartnerProfileBizModel.java:249`——assertCan 防御分支抛裸 `IllegalArgumentException`（私有 switch 全覆盖不可达护栏）。
- **三态裁决**：新立（维度③字面命中，r1 扫 extends 面未扫 throw 点）。P3。
- **修复方向**：改抛 NopException 内部码或登记防御编程约定豁免；不入修复队列优先级。

### 2.4 归属标注（§3.2 共享代码边界 + 跨域横切）

- posting 引擎：b2b 无过账消费点，不适用。
- common 抽象族调用点合规（13/13 基类接入）；基类行为（status 列惰性）归 U20。
- 聚合横切面归 U21：action-auth b2b 注册在位；b2b-011 FNPT 缺口维持原 ID（跨域家族 ct 站点 = P3-CK-ct-030-r3 本轮新立）。
- notify 消费点（OnboardingMonitorJob）合规，发送内部归 U11。
- 跨切片注记：017-r3 跨域写边登记缺口牵动 data-dependency-matrix owner（matrix §2.2 b2b 行「待深化」列收口归矩阵 owner 流程）；b2b-013 字典值域面与 ct 025-r3 同族。

### 2.5 维度⑮断言抽样记录（4 doc 主抽 + 2 doc 扩样 × 13 断言，漂移 4）

state-machine.md 5 断言（ERROR 重试 → **漂移** = b2b-005 归并 / 8 状态集 ✓ / TO_CANCEL 裁决 ✓ / asn CANCELLED 裁决 ✓ / config-gated → **漂移** = b2b-004 归并）；managed-file-transfer.md 2 断言（RETRYING 重试 → **漂移**（RETRYING 死值部分新立 018-r3 + retryIntervalMin 固定 sleep 部分 = RC-068 复用 watch-only）/ 证书过期告警+自动停用 → **漂移** = RC-068 归并复用（findExpiringCertificates 零 job 消费者 HEAD 现症在位））；partner-onboarding.md 4 断言（12 边矩阵 ✓ / 三门槛错误码 ✓ / REGISTERED 初始态 → **漂移** = b2b-009 归并补证 / webhookSecret 加密 → **漂移** = **019-r3 新立**）；扩样 edi-formats.md 2 断言（遍历 active 格式 → **漂移** = b2b-016 归并 / ERROR→TO_SEND 出站向 ✓）+ asn-processing.md 1 断言（materialId 写入 → **漂移** = b2b-001 归并）+ README 2 断言（blocking_level ✓ / 1:N → **漂移** = b2b-007 归并）。漂移 ≥2 扩样条款已履行（4/7 doc 主抽 + asn-processing/README 扩样，未发现第三处独立新漂移面）。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 0 | 1（b2b-001） |
| P2 | 1（b2b-017-r3 跨域写边未登记） | 0 | 8（b2b-002..009，含 008 索引失配注记） |
| P3 | 3（b2b-018/019/020-r3） | 0 | 7（b2b-010..016） |
| **合计** | **4** | **0** | **16** |

五格 verdict：DIM-B **finding**（4 新立）/ DIM-F **pass**（归并态注记）/ DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 16 条 r1 ID 状态零覆写（16 open 追加证据；008 索引行失配注记在案不覆写）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-b2b-{dao,service} 全部 processor（6 族）+ 13 BizModel + 3 状态机 Bean（12+11+2 边逐一）+ TransportManager/CodeMappingResolver/UblInvoiceEdiProvider SPI 族 + webhook 全链 + OnboardingMonitorJob + ErpB2bErrors/Constants/Configs + 机械程式全套实跑（checker/反模式/codegen/聚合 E1/validate:flux/seed 门禁/b2b 回归 80/strict+self-test）；owner docs 4 doc 主抽 + 2 doc 扩样 × 13 断言；r1 16 条全量逐条复核；r2 目录核对；b2b seed 13 CSV EDI 段抽查；执行者对 017-r3 跨域写站点与依赖矩阵三处核对。
- **未深查（边界归属）**：`AbstractErpCrudBizModel` 基类内部（归 U20）；`erp-b2b-web` 渲染时行为（静态 + 门禁）；ErpPurReceive 侧被写实体的守卫语义（pur 格 M1.13 已闭合，本格只审写机制）；MFT 真实传输通道（stub/SPI 面，真实 SFTP 接入 Deferred）。
- **残留风险（登记不裁决）**：① 16 条归并 open 修复归 M2.x，P1-CK-b2b-001（webhook 物料映射断裂使自动收货链末端必抛）建议最优先——ASN 自动化主链路断点；② P2-CK-b2b-017-r3 修复裁决（矩阵登记 vs I*Biz 化 vs RAW-DAO 例外）牵动依赖矩阵 owner 流程，建议与矩阵 §2.2「待深化」列收口同批；③ 018-r3 mft-status 三值死与 transport Deferred（真实通道未接入）联动——真实通道落地前 dict 维持死值面，两事同批收口；④ webhookSecret 明文（019-r3）与生产凭据面（PartnerCredential 表已建零消费）建议安全专项批次。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 open 项；b2b.enabled 主开关（b2b-004）若裁决接入则 EDI 出站自动化 Deferred 面整体重估。
