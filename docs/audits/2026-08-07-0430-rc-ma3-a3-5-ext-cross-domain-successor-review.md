# rc-ma3-a3-5-ext-cross-domain-successor-review 扩展域+跨域 MA3 successor 追踪完整性与回队复查报告（A3.5）

> Plan Status: completed
> 产出时间：2026-08-07
> 来源 Plan：`docs/plans/2026-08-07-0400-2-rc-ma3-a3-5-ext-cross-domain-successor-review.md`（Work Item A3.5）
> Mission：requirement-compliance（MA3 successor 触发条件复查 — MA3 收官行）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§4 三判据 / §5 Q4 + 保护区域 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议 + §MA2↔MA3 协作）
> 路线图：`docs/backlog/requirement-compliance-roadmap.md`（A3.5 扩展域+跨域 successor 复查 + Work Item Details MA3）
> 复查全集：`docs/audits/rc-existing-inventory.md`（§successor 三源对账清单 扩展域+跨域分组 — **8 项** + §对账差异登记 #5 实现修复项 successor 残留 + §集成排序汇总表[8-vs-9 计数自检注记]）
> Skill：`docs/skills/open-ended-audit-prompt.md`
> 审计性质：**只读审计**——读 arm-index / owner doc / backlog README / 实仓代码 / config / ORM 裁决 successor 触发条件，**不修改任何代码/ORM/api.xml/真相源**

---

## §复查口径与 Q4 修复义务边界

本报告复查对象 = M0.3（`rc-existing-inventory.md` §successor 三源对账清单）导出的扩展域+跨域 design-level successor 去重并集 **8 项**（逐行精确匹配详细表 :172-179）。逐项完成方法论 §MA3 四任务：① 触发条件是否已满足（grep 实仓代码/config/ORM 字段验证）；② 是否该回队（已满足→回队 MR1 R1.0；未满足→维持 backlog successor）；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核。

> **计数自检注记**（继承自 plan baseline）：rc-existing-inventory §集成排序汇总表（:217）标注 A3.5 = 「9 项 successor」，但同文件 §successor 三源对账清单详细表（:172-179）权威列出 **8 行**。本报告以详细表 8 项为复查全集（逐行精确匹配），9-vs-8 差异为源文档汇总表 off-by-one（32 vs 33 总和），不影响完整枚举纪律。

**Q4 修复义务边界（§5）**：successor 触发条件**已满足**者须回队 MR1（R1.0 展开为 RC-R1.n，Q4 强制实现禁方案 B）；触发条件**未满足**者维持 backlog successor 登记（不强制实现，待触发）。本复查 8 项触发条件**全部未满足**（grep 实仓逐项证实，§对账差异 #5 各项经区分「finding 已修复」与「successor 残留」后核心 successor 仍维持 backlog）→ 8 项**全部维持 backlog successor**，无回队 MR1。

**finding 路由 vs successor 触发条件路由（防执行者混淆）**：本 A3.x 只裁决 **successor 触发条件**是否回队，不重审方案 B 关闭裁决本身（属 A2.x），也不重审 finding 是否修复（属 A1.x→MR1）。即：successor 回队与否（A3.x）≠ finding 是否修复（A2.x/A1.x→MR1），两者各自裁决、交叉引用不冲突。本复查的 §对账差异登记 #5 处置（区分「finding 已修复/关闭」与「successor 仍待触发」）正是此原则的体现：

- **#1 contract EXPIRED 自动到期 Job + 续期草稿**：`P1-MA2-071` **finding resolved R1.22 via deferral**（owner doc `state-machine.md §2/§4/§7` Deferred 标注，A2.8 RC 范围）；A1.45+A1.46 RC 从 L1 视角 §4 三判据复核裁决 deferral 不成立（[i]R1.22 AI 子代理裁决≠人工批准 + [ii]owner doc Deferred git log 全 AI commits 无人工批准痕迹 + [iii]product-scope 未裁剪）→ A1.45 reuse `P1-MA2-071` 倾向 MR1 须实现 successor（**属 A1.x→MR1 裁决**）。本 A3.5 裁决的是 **successor 触发条件**（ErpCtContractExpiryJob 自动到期 + 续期草稿 config-gated 自动化）→ 触发条件「合同到期自动化需求」未满足 → 维持 backlog。两者各自裁决不冲突。
- **#2 b2b EDI 出站自动化**：`P1-MA2-073` **finding resolved R1.23 via deferral**（owner doc state-machine.md §L-8/§6/§8/§9 Deferred 标注，A2.8 RC 范围；A1.47 RC reuse 确认 resolved-via-deferral）；其 **successor**（MFT transport 真实对接上线时）触发条件未满足 → 维持 backlog。
- **#3 contract InvoicePlan 跨域写收敛**：`P1-MA1-029` **finding resolved**（写侧豁免补登于 `posting-exemptions.md §ErpCtInvoicePlanBizModel`，A2.8 RC 范围）；其 **successor**（pur/sal 提供 purpose-built Facade 时收敛为 I*Biz）触发条件未满足 → 维持 backlog。
- **#4 logistics 部分签收**：`P1-MA2-079` **finding resolved R1.25 via deferral**（owner doc `state-machine.md §2/§4` Deferred 标注，A2.8 RC 范围）；其 **successor**（承运商支持部分签收回调时）触发条件未满足 → 维持 backlog。
- **#5 跨公司 orgId 隔离**：`P1-MA2-093/094` **finding resolved R1.29 via implementation**（`ErpOrgContext` + `ErpOrgIsolationOrmInterceptor` + 全局 `ErpOrgIsolationQueryTransformer` 落地，config-gated `org-isolation-enabled` 默认 false）；其 **successor**（多组织部署启用时）触发条件未满足 → 维持 backlog。
- **#6 多账套 acctSchemaId 读路径隔离**：`P1-MA2-095` **finding resolved R1.29 via implementation**（读路径 period.orgId + 主账套 scope 过滤，A1.7 RC §4 HEAD 复核确认已修复不双计）；其 **successor**（`multi-schema-enabled=true` 部署下「每账套独立三表」运行时渲染）触发条件未满足 → 维持 backlog。**与 A4.1.23 交叉引用**（不同控制点：MA3 successor 触发条件[部署启用] vs MA4 运行时渲染行为[每账套独立三表渲染]）。
- **#7 全域敏感动作 action-level RBAC**：`P1-MA3-046` **finding done R2.7**（per-action FNPT 声明 `_erp-*.action-auth.xml` + `app.action-auth.xml` + data-auth enforcement beans `ErpRoleDataAuthChecker` 落地，config-gated `nop.auth.enable-action-auth=false` 默认）；其 **successor**（灰度翻转至 enforcement）触发条件未满足 → 维持 backlog。**owner doc 显式声明有意默认**（`roles-and-permissions.md §运行基线`）。
- **#8 OPEN_AUDIT 轮次形式化**：`P1-MA6-005` **finding done R3.5**（Round 3 closure-audit sweep 批次闭合第三波 closure-pending 超集）；其 **successor**（OPEN_AUDIT 轮次形式化 option B — 制度化定期 closure-pending 清理循环）触发条件未满足 → 维持 backlog。

---

## 1. successor 三源对账清单（扩展域+跨域，段 1，§6 MA3 适配）

> 三源：S1 = `docs/audits/arm-index.md` 行内 successor/触发条件声明 / S2 = owner doc 内嵌 successor / Deferred 段落 / S3 = `docs/backlog/README.md` 既有追踪行。

| # | successor 项 | 域 | 三源覆盖 | 触发条件摘要 | 复杂度 | A2.x 关闭裁决交叉（two-faces） |
|---|-------------|----|---------|-------------|--------|------------------------------|
| 1 | contract EXPIRED 自动到期 Job + 续期草稿 | contract | S1 | nop-job 接线时（合同到期自动化需求） | A | `P1-MA2-071`（A2.8：resolved R1.22 via deferral；A1.45+A1.46 RC §4 三判据复核 deferral 不成立倾向 MR1） |
| 2 | b2b EDI 出站自动化（TransportManager 接线 + ACK-timeout + 重试 + 升级） | b2b | S1+S2 | MFT transport 真实对接上线时（AS2/SFTP/FTPS） | A | `P1-MA2-073`（A2.8：resolved R1.23 via deferral；A1.47 RC reuse 确认） |
| 3 | contract InvoicePlan 跨域写收敛为 I*Biz | contract | S1 | pur/sal 提供 purpose-built Facade 时 | A | `P1-MA1-029`（A2.8：resolved 写侧豁免补登于 posting-exemptions.md） |
| 4 | logistics 部分签收 | logistics | S1 | 承运商支持部分签收回调时 | C | `P1-MA2-079`（A2.8：resolved R1.25 via deferral） |
| 5 | 跨公司 orgId 隔离查询/写入（多公司部署） | 跨域（全 19 域） | S1 | 多组织部署启用时（`org-isolation-enabled=true`） | A（跨域） | `P1-MA2-093/094`（A2.9：resolved R1.29 via implementation） |
| 6 | 多账套 acctSchemaId 读路径隔离（报表/看板「每账套独立三表」渲染） | 跨域（finance） | S1 | `multi-schema-enabled=true` 启用时 | S | `P1-MA2-095`（A2.9：resolved R1.29 via implementation；**与 A4.1.23 交叉引用**） |
| 7 | 全域敏感动作 action-level RBAC（@BizAuth/FNPT enforcement 灰度翻转） | 跨域 | S1 | owner doc §运行基线 灰度翻转（须人工批准 + role 种子 + 灰度计划） | A（跨域） | `P1-MA3-046`（A2.x 跨域：done R2.7 部分修复，enforcement beans config-gated OFF） |
| 8 | OPEN_AUDIT 轮次形式化（制度化 closure-pending 清理循环） | 跨域（docs/plans/） | S1 | 形式化 closure-pending 清理循环 | A（跨域） | `P1-MA6-005`（A6.4：done R3.5 option A[一次性 sweep]，option B[制度化] Deferred） |

> **§对账差异登记 #5 覆盖**：8 项的「finding 已修复/关闭」与「successor 仍待触发」区分见 §复查口径段（上）。**#1/#5/#6/#7 是 §对账差异 #5 的核心核实项**（plan baseline 命名）——经逐项核实：#1 finding resolved-via-deferral（successor 维持）/ #5/#6 finding resolved-via-implementation（successor 部署侧维持）/ #7 finding done R2.7 partial（successor 灰度翻转维持），均严格区分「finding 已修复」与「successor 仍有效」，**避免误将已修复 finding 重新纳入 MR1**。
>
> **三源覆盖说明**：#2 为 S1+S2 双源覆盖（arm-index `P1-MA2-073` 行 successor 声明 + `managed-file-transfer.md §Non-Goal` transport Deferred）；其余 7 项为 S1 单源覆盖（arm-index 行内 successor 声明）。S3（`docs/backlog/README.md`）经 M0.3 §对账差异登记 #4 核实为 E2E 测试 successor + 叙述性提及，不产生独立 design-level successor，仅作覆盖交叉验证（task④ 复核见 §2）。
>
> **与 A3.3 maintenance 域投影交叉引用**：A3.3 #5（employee-id 行过滤，quality inspectorId / maintenance assignedTo 跨域投影）已由 A3.3 合并裁决一次（合并 quality+maintenance 两域投影，ORM ask-first 保护区域，successor 维持 backlog）。本 A3.5 8 项 successor **不含 maintenance employee-id 投影**，故交叉引用 A3.3 #5 结论，不重复裁决。

---

## 2. 逐项四任务核证（段 2，§6 MA3 适配）

> 四任务：① 触发条件是否已满足（grep 实仓代码/config/ORM）；② 是否该回队；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核。

### 2.1 #1 contract EXPIRED 自动到期 Job + 续期草稿 — contract

- **① 触发条件状态**：**未满足**。实仓 grep `module-contract/erp-ct-service/src/main`：(a) **手工 expire 路径存在**——`ErpCtContractBizModel.expire():123` `@BizMutation` 设 `CONTRACT_STATUS_EXPIRED`（require ACTIVE 守卫），R1.22 落地；(b) **无自动到期 Job**——grep `ErpCtContractExpiryJob|IJob|@CronProvider|scheduler|\.job\.xml` 跨 module-contract 全模块**零业务命中**（owner doc `state-machine.md §2:49` 显式「无 `ErpCtContractExpiryJob`，module-contract 全域零 Job 类、零 scheduler、零 `@CronProvider`」）；(c) **无续期草稿 config + 零业务使用**——`ErpCtConfigs.java` 仅有 volume-discount/rebate/invoiceplan-auto-trigger/settlement-mode/e-signature 五键，**无 `erp-ct.auto-create-renewal-draft`**；`parentContractId` 字段存在但 grep 全 module-contract `renewal|续期|续签` **无匹配**（零业务 Java 代码使用）。owner doc `state-machine.md §2:47/49`（ACTIVE→EXPIRED ~~系统自动~~ Deferred 注记）+ `§4:69/71`（续期草稿 auto-create-renewal-draft Deferred 注记）显式 **Deferred + Successor**「合同到期自动化需求时实现 `ErpCtContractExpiryJob`（cron-gated 扫描 ACTIVE 且 endDate<now 批量 expire）+ config-gated `auto-create-renewal-draft`，对齐 hr 域 `ErpHrContractExpiryJob` 范式」。触发条件 = 「合同到期自动化需求」，该业务**未驱动**（手工 `expire()` 路径存在兜底）。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——到期自动化需求未驱动；修复属 missing-automation[新 Job + config-gated 续期]，预授权可自动执行，但触发条件未满足不强制实现）。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA2-071` 行含 successor 声明 + owner doc `state-machine.md §2:49 + §4:71` 显式 Deferred + Successor 触发条件标注最详）。
- **④ README 覆盖复核**：`docs/backlog/README.md` `:58`（contract + drp DIRECT 业务动作 E2E）+ `:64`（contract + drp 跨域编排 E2E）为 **E2E 测试 successor**（合同生命周期状态机 activate/suspend/resume/terminate + expire/amend 浏览器层 E2E，已 RELEASED done），非本 design successor（nop-job 自动到期 + 续期草稿）。design successor 经 S1（arm-index `P1-MA2-071` 行）覆盖，**无「已登记但从未触发」风险**（触发条件「合同到期自动化需求」明确未触发，未误标 done）。
- **结构性约束标注（§对账差异 #5 + A1.45 reuse 交叉）**：`P1-MA2-071` **finding resolved R1.22 via deferral**（owner doc Deferred 标注，非方案 A 实现）。A1.45+A1.46 RC 从 L1 视角 §4 三判据复核裁决 deferral 不成立 → reuse `P1-MA2-071` 倾向 MR1 须实现 successor（**属 A1.x→MR1 通道裁决**）。本 A3.5 裁决的是 **successor 触发条件**（未满足→维持 backlog），**不重审** finding 关闭裁决（归 A1.45/A2.8 RC→MR1 通道，两者各自独立交叉不冲突）。

### 2.2 #2 b2b EDI 出站自动化（TransportManager 接线 + ACK-timeout + 重试 + 升级）— b2b

- **① 触发条件状态**：**未满足**。实仓 grep `module-b2b/erp-b2b-service/src/main`：(a) **TransportManager wired-but-uncalled**——`TransportManager.java:36` 存在（`maxRetries` + `MFT_RETRY_EXHAUSTED`/`MFT_NON_RETRYABLE` 重试 + 死信 `ErpB2bMftLog` 写日志），但 `ErpB2bEdiDocBizModel.createOutbound:57-60` 委派 `ErpB2bEdiDocCreateOutboundProcessor.createOutbound` **仅留 TO_SEND 信封**（生成 payload + checkDuplicate + 写 TO_SEND + writeLog），**生产代码零调 `transportManager.send`**（A1.47 RC §3 + A2.14 已证实「needsWebService 异步/同步派发分支在 createOutbound 零调用」）；(b) **无 nop-job + 无 ack-timeout config**——grep `IJob|JobExecutor|ErpB2b.*Job|\.job\.xml|@Scheduled` 跨 module-b2b **零业务命中**；`ErpB2bConfigs.java` 仅 `CONFIG_ASN_AUTO_MATCH_RETRY_INTERVAL` + `CONFIG_B2B_ENABLED`（=`erp-b2b.enabled`），**无 `ack-timeout-seconds` config**；(c) **retry 仅手工**——`ErpB2bEdiDocBizModel.retry:112` `@BizMutation` 手工 `retryCount++`，无自动触发；(d) **MFT transport Mock-only**——`MockTransportAdapter` 唯一 impl，真实 AS2/SFTP/FTPS = `managed-file-transfer.md §Non-Goal`（`:8`「本期仅 MockTransportAdapter + SPI 契约；真实 AS2/SFTP/FTPS 协议库集成归 follow-up，触发条件：具体传输伙伴接入 + 证书就绪」）；(e) **整个 b2b 子系统 config-gated OFF 默认**——`erp-b2b.enabled`（`docs/architecture/b2b-integration.md` `n` default false）→ 默认 config 零生产暴露。owner doc `state-machine.md §L-8/§6/§8/§9 场景 C` 自动化控制点（auto-retry 3 次指数退避 + ACK-timeout 24h→ERROR + ERROR>24h 升级 + 系统每 30 分钟自动重试）经 R1.23 标注 **Deferred**（「出站自动化 Deferred——MFT transport 真实对接上线时实现」）。触发条件 = 「MFT transport 真实对接上线时（AS2/SFTP/FTPS）」，该真实对接**未上线**。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——MFT transport 真实对接未上线 + b2b 子系统 config-gated OFF；修复属 missing-automation + 真实 MFT 协议库集成）。**外部集成保护区域**（§5：触及外部集成 AS2/SFTP/FTPS 真实对接，修复实施须 ask-first + 独立 plan-audit）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖，arm-index `P1-MA2-073` 行含 successor 声明 + `managed-file-transfer.md §Non-Goal` + `state-machine.md §L-8/§6/§8/§9` Deferred 触发条件标注）。
- **④ README 覆盖复核**：README `:60`（aps+b2b+logistics DIRECT 业务动作 E2E，`b2b-edi-doc` 出站 TO_SEND→markSent→markAcknowledged + markError/retry + 入站 + cancel 守卫）+ `:65`（b2b+logistics+aps 跨域编排 E2E，`b2b-asn-match-receive`）为 **E2E 测试 successor**（已 RELEASED done），非本 design successor（TransportManager 真实接线 + ACK-timeout + 自动重试 + 升级）。design successor 经 S1+S2 覆盖充分，无悬空。
- **结构性约束标注（§对账差异 #5 + 外部集成保护区域）**：`P1-MA2-073` **finding resolved R1.23 via deferral**（owner doc state-machine.md Deferred 标注，A2.8 RC 范围；A1.47 RC reuse 确认 resolved-via-deferral 非 implementation）。本 A3.5 裁决的是 **successor 触发条件**（未满足→维持 backlog），**不重审** finding 关闭裁决。**外部集成保护区域**：真实 MFT transport（AS2/SFTP/FTPS）对接修复实施须 ask-first + 独立 plan-audit（§5），本裁决仅判 successor 触发条件。

### 2.3 #3 contract InvoicePlan 跨域写收敛为 I*Biz — contract（→ pur/sal 跨域写）

- **① 触发条件状态**：**未满足**。实仓 grep `module-contract/erp-ct-service/src/main`：`ErpCtInvoicePlanTriggerInvoiceProcessor.createApInvoiceDraft:74` + `createArInvoiceDraft:111` **经 IDaoProvider 直接持久化跨域写**（注释自承「发票草稿生成（经 IDaoProvider 直接持久化）」）——`daoProvider.daoFor(ErpPurInvoice.class).newEntity()` + `setCode/setOrgId/setSupplierId/...` + `dao.saveEntity(invoice)` + `daoProvider.daoFor(ErpPurInvoiceLine.class).saveEntity(invLine)`（AR 侧同理经 `daoProvider.daoFor(ErpSalInvoice.class)`）——**绕过 pur/sal I*Biz Facade 直写 daoFor saveEntity**。grep `createFromInvoicePlan|generateInvoicePlan|invoicePlan` 跨 module-purchase + module-sales **零 purpose-built Facade 命中**（pur/sal 域无 InvoicePlan 收敛 Facade）。owner doc `posting-exemptions.md §ErpCtInvoicePlanBizModel`（写侧豁免补登）登记此为 O-4 写侧半治理豁免。触发条件 = 「pur/sal 提供 purpose-built Facade 时收敛为 I*Biz 调用」（同 A3.2 #2 委外收敛 `createFromMrpLine` 范式），pur/sal Facade **未提供**。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——pur/sal purpose-built Facade 未提供；收敛修复属跨域契约须与 pur/sal 协调 ask-first + contract 侧改为 I*Biz Facade 调用，预授权可执行但触发条件未满足不强制实现）。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA1-029` 行 + `posting-exemptions.md §ErpCtInvoicePlanBizModel` 收敛条件 successor 声明）。
- **④ README 覆盖复核**：README `:64`（contract + drp 跨域编排 E2E，`ct-invoice-plan-trigger` triggerInvoice INBOUND→AP/OUTBOUND→AR 发票草稿创建 + 已开票/SUSPENDED 守卫 + triggerDuePlans 批量入口，已 RELEASED done）为 **E2E 测试 successor**（断言发票草稿创建行为本身），非本 design successor（跨域写经 daoFor 直写收敛为 I*Biz Facade 调用的治理收敛）。design successor 经 S1 覆盖充分。**注**：E2E 覆盖的是「草稿创建行为正确」，本 successor 是「创建路径经 I*Biz Facade 还是 daoFor 直写」的治理面——两者维度不同，E2E done 不消解 design successor。
- **结构性约束标注（§对账差异 #5 + 跨域契约协调）**：`P1-MA1-029` **finding resolved**（写侧豁免补登于 `posting-exemptions.md §ErpCtInvoicePlanBizModel`，A2.8 RC 范围）。本 A3.5 裁决的是 **successor 触发条件**（未满足→维持 backlog），**不重审** finding 关闭裁决（归 A2.8 RC 写侧豁免合理性裁决）。**跨域契约**：收敛修复须与 purchase/sales 域协调提供 purpose-built Facade（同 A3.2 #2 `createFromMrpLine` 范式），属跨域契约协调 ask-first。

### 2.4 #4 logistics 部分签收 — logistics

- **① 触发条件状态**：**未满足**。实仓 grep `module-logistics/erp-log-service/src/main`：`GatewayDispatcher.advanceTracking:162-188` **仅处理完整 `TRACKING_EVENT_DELIVERED`**（`:164` if DELIVERED → onDelivered；`:176-177` if IN_TRANSIT||PICKED_UP → 仅推进）；`ErpLogConstants.java:37-39` 仅定义 `TRACKING_EVENT_PICKED_UP/IN_TRANSIT/DELIVERED` 三常量，**无 `TRACKING_EVENT_PARTIAL` 常量**。grep `PARTIAL|partial|部分签收|receivedQuantity|partialSignedQty` 跨 module-logistics/erp-log-service/src/main **零业务命中**——**无部分签收字段 + 无部分签收记录路径**。`MockCarrierGatewayClientFactory:118-119` 仅产 IN_TRANSIT/DELIVERED 事件。owner doc `state-machine.md §2:40`（ASCII 图「部分签收 → 记录部分签收，状态保持 IN_TRANSIT[等待剩余]」）+ `§4:66`（异常路径表「部分签收 Deferred（P1-MA2-079，plan `2026-07-30-0720-2`）——当前 advanceTracking 仅处理完整 DELIVERED，承运商支持部分签收回调时实现 TRACKING_EVENT_PARTIAL 常量 + receivedQuantity/partialSignedQty 字段[须 ORM ask-first 加列] + 累计签收判定」）显式 **Deferred + Successor**。触发条件 = 「承运商支持部分签收回调时」，该回调**未提供**（承运商暂只发完整 DELIVERED 事件）。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——承运商部分签收回调未提供；主路径完整签收 DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED 完整覆盖，部分签收属 owner doc Deferred 业务场景）。**ORM ask-first 保护区域**（§5：须 ORM 加 receivedQuantity/partialSignedQty 列 + TRACKING_EVENT_PARTIAL 常量，修复实施须 ask-first + 独立 plan-audit）。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA2-079` 行 + owner doc `state-machine.md §2:40 + §4:66` Deferred + Successor 触发条件标注）。
- **④ README 覆盖复核**：README `:60`（log-shipment 发运单状态机 advise→completeShipment→cancelShipment + MockCarrierGatewayClientFactory 完整可达 DISPATCHED）+ `:65`（log-delivered-freight-posting DELIVERED→onDelivered→FREIGHT 过账）为 **E2E 测试 successor**（完整签收主路径，已 RELEASED done），非本 design successor（部分签收分支）。design successor 经 S1 覆盖充分。
- **结构性约束标注（§对账差异 #5 + ORM ask-first 保护区域）**：`P1-MA2-079` **finding resolved R1.25 via deferral**（owner doc state-machine.md §2/§4 Deferred 标注，A2.8 RC 范围）。本 A3.5 裁决的是 **successor 触发条件**（未满足→维持 backlog），**不重审** finding 关闭裁决。**ORM ask-first 保护区域**：部分签收字段 ORM 变更须 ask-first + 独立 plan-audit（§5），本裁决仅判 successor 触发条件。

### 2.5 #5 跨公司 orgId 隔离查询/写入（多公司部署）— 跨域（全 19 域）

- **① 触发条件状态**：**未满足（successor 部署侧维持）**。本项是 §对账差异 #5 的核心核实项，须严格区分「finding 已修复」与「successor 仍有效」：
  - **finding（orgId 隔离机制）= 已实现修复（R1.29 方案 A）**：实仓 `module-common-service/src/main/java/app/erp/common/org/ErpOrgContext.java`（`CONFIG_ORG_ISOLATION_ENABLED`=`ErpnConstants.CONFIG_ORG_ISOLATION_ENABLED` 默认 **false**）+ `ErpOrgIsolationOrmInterceptor.java`（`IOrmInterceptor`，`entity.orm_propValueByName(PROP_ORG_ID, orgId)` 写入侧 auto-stamp）+ 全局 `ErpOrgIsolationQueryTransformer`（`IQueryTransformer` 注入查询管道层强制 orgId scope，A1.7/A1.27/A1.41/A1.44 RC reuse 确认覆盖 dashboard 直访路径）已落地。grep `useTenant` 跨全部 `*.orm.xml` **零命中**（平台 tenant 未启用，项目侧用 orgId 机制）。
  - **successor（多公司部署验证）= 未满足**：`ErpOrgContext` 隔离总开关 `CONFIG_ORG_ISOLATION_ENABLED` 默认 **false**（单组织基线零回归，单组织种子 176 行全 orgId=2 掩盖跨组织泄漏）——多组织部署须显式 `org-isolation-enabled=true` 启用 + 多组织种子 + 用户归属 orgId 上下文。触发条件 = 「多组织部署启用时」，该部署**未启用**。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——多组织部署未启用；finding 机制已 R1.29 落地，successor 是部署侧验证）。须区分「finding 已修复（orgId 隔离机制已实现）」与「successor 仍有效（多公司部署未验证）」——**不误将已修复 finding 重新纳入 MR1**。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA2-093/094` 行 successor 声明「多组织部署启用时」）。
- **④ README 覆盖复核**：README `:121`（跨公司 Intercompany PO/SO 配对凭证浏览器层 E2E，自包含 setup 建 2 COMPANY 法人根 + 2 DEPARTMENT 子组织使 `resolveLegalEntityRoot` walk-up 产出不同法人 + 转移定价规则 + AR/AP 配对凭证，已 RELEASED done）为 **E2E 测试 successor**（跨公司配对凭证行为，自包含 setup 而非真实多组织部署隔离）。本 design successor（多组织部署 orgId 隔离查询/写入 enforcement）经 S1 覆盖。**注**：Intercompany E2E 经自包含 setup 验证配对凭证正确性，**不验证** orgId 隔离 enforcement（config-gated OFF 默认）——两者维度不同。
- **结构性约束标注（§对账差异 #5 核心核实项）**：`P1-MA2-093/094` **finding resolved R1.29 via implementation**（`ErpOrgContext` + `ErpOrgIsolationOrmInterceptor` + 全局 `ErpOrgIsolationQueryTransformer` 落地，config-gated 默认 OFF）。本 A3.5 严格区分「finding 已修复」（orgId 隔离机制已实现）与「successor 仍有效」（多公司部署未启用验证）——**不误将已修复 finding 重新纳入 MR1**（finding 关闭/重开归 A2.9 RC + A1.x reuse 通道，successor 触发条件归 A3.x，各自独立）。这是 §对账差异登记 #5「实现修复项 successor 残留」纪律的体现。

### 2.6 #6 多账套 acctSchemaId 读路径隔离（报表/看板「每账套独立三表」渲染）— 跨域（finance）

- **① 触发条件状态**：**未满足（successor 报表侧维持）**。本项是 §对账差异 #5 的核心核实项 + **与 A4.1.23 交叉引用**，须严格区分「finding 已修复」与「successor 仍有效」：
  - **finding（acctSchemaId 读路径双计）= 已实现修复（R1.29 方案 A）**：`SchemaPropagator.java`（多账套并行传播，config `ErpFinConstants.n_ENABLED`=`erp-fin.multi-schema-enabled` 默认 **false**）+ 读路径查询补 `period.orgId + 主账套 scope` 过滤（A1.7 RC `2026-08-02-2115-rc-ma1-a1-7-...md:142` HEAD 复核确认「P1-MA2-095 读路径双计**已修复**（period.orgId + 主账套 scope；多账套部署取主账套 FINANCIAL 不双计）；scope 解析失败时[period.orgId 为空]跳过 filter 保护单组织基线零回归」）已落地。
  - **successor（多账套部署「每账套独立三表」运行时渲染）= 未满足**：多账套部署须 `multi-schema-enabled=true` 启用 + 每账套独立三表渲染（当前读路径取主账套 FINANCIAL 非**按账套切换**渲染，A1.7 RC §7 SP-2 + `:287` 登记为静态存疑点）。触发条件 = 「`multi-schema-enabled=true` 启用时 + 每账套独立三表渲染需求」，该部署**未启用**。
  - **与 A4.1.23 交叉引用（不同控制点）**：A4.1.23（roadmap :149）= 「多账套部署『每账套独立三表』运行时渲染（当前读路径取主账套非按账套切换）」属 **MA4 运行时行为验证维度**（运行时渲染行为）；本 A3.5 #6 successor = 「`multi-schema-enabled=true` 启用触发条件」属 **MA3 successor 触发条件维度**（部署启用）。两者不同控制点，交叉引用不重复。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——multi-schema-enabled 默认 false + 每账套独立三表渲染需求未驱动；finding 读路径过滤已 R1.29 落地，successor 是多账套部署运行时渲染验证）。须区分「finding 已修复」与「successor 仍有效」——**不误将已修复 finding 重新纳入 MR1**。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA2-095` 行 successor 声明「`multi-schema-enabled=true` 启用时」）。
- **④ README 覆盖复核**：无独立 design successor 行（README finance 域行为 E2E 测试 successor + 已 done 的引擎实现，非本 design successor）。S1 覆盖充分。
- **结构性约束标注（§对账差异 #5 核心核实项 + A4.1.23 交叉引用）**：`P1-MA2-095` **finding resolved R1.29 via implementation**（读路径 period.orgId + 主账套 scope 过滤，A1.7 RC §4 HEAD 复核确认已修复不双计）。本 A3.5 严格区分「finding 已修复」（读路径隔离已实现）与「successor 仍有效」（多账套部署每账套独立三表渲染未验证）——**不误将已修复 finding 重新纳入 MR1**。**与 A4.1.23 交叉引用**（不同控制点：MA3 successor 触发条件[部署启用] vs MA4 运行时渲染行为[每账套独立三表渲染]），交叉引用不重复。

### 2.7 #7 全域敏感动作 action-level RBAC（@BizAuth/FNPT enforcement 灰度翻转）— 跨域

- **① 触发条件状态**：**未满足（successor 灰度翻转维持）**。本项是 §对账差异 #5 的核心核实项，须严格区分「finding 已部分修复」与「successor 仍有效」：
  - **finding（action-level FNPT + enforcement 机制）= 已部分实现修复（R2.7）**：实仓 grep `@BizAuth` 跨全部 `module-*/erp-*-service/src/main` **0 命中**（动作注解层仍裸注解），但 R2.7 落地**声明层 + enforcement beans**——`module-finance/erp-fin-web/_vfs/erp/fin/auth/_erp-fin.action-auth.xml` + `erp-fin.action-auth.xml`（per-action FNPT 声明，`x:extends` 模式）+ `module-finance/erp-fin-app/src/main/resources/_vfs/erp/fin/auth/app.action-auth.xml`（域 action-auth）+ data-auth enforcement `ErpRoleDataAuthChecker`（bean `nopDataAuthChecker`，R3.4 角色侧行级过滤，config-gated 默认 OFF）。owner doc `roles-and-permissions.md §运行基线 :130/159` 显式「当前运行基线 `nop.auth.enable-action-auth=false`（默认）→ 操作级拦截关闭」+ `§行级过滤落地状态 :75`「翻转至 enforcement 为 successor（须人工批准 + role 种子 + 灰度计划）」+ `§灰度推进路线`「finance/b2b/mfg/inventory/hr 为首批落地，其余 per-action FNPT 声明随 enforcement 灰度分批补齐（触发条件 = 该域 `enable-action-auth=true` 灰度批准前）」。
  - **successor（灰度翻转至 enforcement）= 未满足**：翻转须 `nop.auth.enable-action-auth: true`（`app-erp-all/application.yaml`）+ role-resource 种子补全 + 灰度计划 + 人工批准。触发条件 = 「owner doc §运行基线 灰度翻转（须人工批准 + role 种子 + 灰度计划）」，该翻转**未批准**（`permissions-enforcement-roadmap.md` 全部 `todo`，README :124「权限 enforcement 开启（测试环境）」E1-E4 全 todo，人工批准 enforcement 测试环境 2026-08-05 但生产翻转另批）。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——灰度翻转未批准 + role 种子未补全；finding FNPT 声明 + enforcement beans 已 R2.7/R3.4 落地 config-gated OFF，successor 是翻转至 enforcement）。须区分「finding 已部分修复」与「successor 仍有效」——**不误将已修复 finding 重新纳入 MR1**。**owner doc 显式声明有意默认**（`roles-and-permissions.md §运行基线` deliberate design 非 silent gap）。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA3-046` 行 successor 声明「灰度翻转」+ owner doc `roles-and-permissions.md §运行基线/§灰度推进路线` 触发条件标注）。
- **④ README 覆盖复核**：README `:124`（权限 enforcement 开启[E2E 测试环境]：E1 action 级强制分域翻转 + E2 data 级强制 role-row-filter + E3 后端响应层脱敏 + E4 采购保密字段级，**全部 `todo`**，触发条件=RBAC 精细化/合规审计需求）为 **design successor roadmap 行**（permissions-enforcement-roadmap.md），与本 design successor（action-level RBAC enforcement 灰度翻转）**同根因同控制点**——README `:124` 正是本 successor 的追踪行，状态 `todo` 与「触发条件未满足→维持 backlog」一致，**无「已登记但从未触发」风险**（触发条件「灰度翻转批准」明确未触发）。
- **结构性约束标注（§对账差异 #5 核心核实项 + owner doc 显式声明有意默认）**：`P1-MA3-046` **finding done R2.7**（per-action FNPT 声明 + action-auth enforcement beans 落地，config-gated `enable-action-auth=false` 默认；A6.1/A6.2/A6.3 RC 确认 + 合并入 P1-MA3-046 修复范围；与 `P1-MA2-093/094`[orgId 维度] + `P1-MA6-001`[SoD 维度] + `P1-MA6-002`[角色侧行过滤维度] 四维度经 R4.1 adjudicated 协同闭合）。本 A3.5 严格区分「finding 已部分修复」（FNPT 声明 + enforcement beans 已落地 config-gated OFF）与「successor 仍有效」（灰度翻转未批准）——**不误将已修复 finding 重新纳入 MR1**。**owner doc 显式声明有意默认**（`roles-and-permissions.md §运行基线` deliberate design，非 silent gap）——翻转须人工批准 + role 种子 + 灰度计划。

### 2.8 #8 OPEN_AUDIT 轮次形式化（制度化 closure-pending 清理循环）— 跨域（docs/plans/）

- **① 触发条件状态**：**未满足（successor 制度化维持）**。本项是 §对账差异 #5 的核实项，须严格区分「finding 已修复」与「successor 仍有效」：
  - **finding（系统性第三波 closure-pending「completed」计划超集）= 已实现修复（R3.5 option A 一次性 sweep）**：`docs/plans/2026-07-31-1439-1-r3-5-closure-audit-round3-protected-area.md` Round 3 closure-audit 批次闭合 14 份确定 closure-pending 清单（P1-MA6-003 ORM ask-first 5 份 + P1-MA6-004 deployment/auth 2 份 + P1-MA6-005 非保护区域 7 份），方案 A（独立子代理 fresh session closure-audit 回填证据）= **14 份全 PASS**，方案 B（审计不可追溯）= 0 份。arm-index `:613` R3.5 闭合回填确认 `P1-MA6-003/004/005` 三项 finding `done (R3.5)`。
  - **successor（OPEN_AUDIT 轮次形式化 option B — 制度化定期 closure-pending 清理循环）= 未满足**：R3.5 plan `§Deferred But Adjudicated` + `:48` + `2026-07-17-0900-1:48` + `2026-07-14-1449-1` 均显式声明「OPEN_AUDIT 形式化仍 Deferred」「option B[制度化定期 closure-audit 轮次] successor，本 plan 不建制度」。触发条件 = 「形式化 closure-pending 清理循环」（在 `docs/process/` 或 `docs/audits/` 下建立正式 OPEN_AUDIT 轮次队列文件追踪未来 completed-but-unaudited 计划），该制度化**未建立**（当前依赖 ad-hoc Round 1/2/3 sweep 而非定期循环）。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——OPEN_AUDIT 轮次形式化[制度化定期循环]未建立；finding 第三波 closure-pending 已 R3.5 sweep 闭合，successor 是制度化机制）。须区分「finding 已修复」与「successor 仍有效」——**不误将已修复 finding 重新纳入 MR1**。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA6-005` 行 successor 声明「OPEN_AUDIT 轮次形式化 option B」+ R3.5 plan `§Deferred But Adjudicated` 触发条件标注）。
- **④ README 覆盖复核**：无独立 design successor 行（OPEN_AUDIT 轮次形式化属过程纪律 governance successor，非 README 业务/E2E 追踪范围）。S1 覆盖充分。
- **结构性约束标注（§对账差异 #5 核实项）**：`P1-MA6-005` **finding done R3.5**（Round 3 closure-audit sweep 批次闭合第三波 closure-pending 超集，option A 一次性 sweep）。本 A3.5 严格区分「finding 已修复」（第三波 closure-pending 已 R3.5 sweep 闭合）与「successor 仍有效」（OPEN_AUDIT 轮次形式化 option B 制度化未建立）——**不误将已修复 finding 重新纳入 MR1**。

---

## 3. 既有行为证据（段 3，复用既有 arm 审计，§去重协议）

> 本复查为 successor 触发条件复查（需求契约视角），不重做 doc↔code 文本一致性 / 状态机行为 / 代码质量 / 安全权限运行时。实现证据复用既有 arm MA2/MA3/MA4/MA6 报告 + A1.x RC 复查报告已证实的代码路径，仅列锚点供四任务核证溯源。

| # | successor 项 | 代码锚点（复用 arm MA2/MA3/MA6 + A1.x RC 已证实） | 既有证实报告 |
|---|-------------|----------------------------------------------|-------------|
| 1 | contract EXPIRED Job + 续期草稿 | `ErpCtContractBizModel.expire():123`（手工 @BizMutation，R1.22）+ 零 `ErpCtContractExpiryJob` + `ErpCtConfigs.java`（5 键无 `auto-create-renewal-draft`）+ `parentContractId` 字段零业务使用 | `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（P1-MA2-071 EXPIRED Job 缺失已证实）；A1.45+A1.46 RC §3（UC-CT-05 reuse P1-MA2-071 resolved R1.22 via deferral §4 三判据复核） |
| 2 | b2b EDI 出站自动化 | `TransportManager.java:36`（wired + retry）+ `ErpB2bEdiDocBizModel.createOutbound:57-60`（委派 createOutboundProcessor 留 TO_SEND，零 transportManager.send）+ `ErpB2bEdiDocCreateOutboundProcessor`（留 TO_SEND）+ 零 nop-job/ack-timeout config + `MockTransportAdapter` 唯一 impl + `erp-b2b.enabled` default false | `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（P1-MA2-073 出站自动化全部缺失已证实）；A1.47 RC §3（UC-B2B-002/006 reuse P1-MA2-073 resolved R1.23 via deferral） |
| 3 | contract InvoicePlan 跨域写收敛 | `ErpCtInvoicePlanTriggerInvoiceProcessor.createApInvoiceDraft:74` + `createArInvoiceDraft:111`（daoProvider.daoFor 直写 saveEntity，绕 I*Biz Facade）+ pur/sal 零 `createFromInvoicePlan` Facade | `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（P1-MA1-029 InvoicePlan 跨域写半治理已证实）；A1.45+A1.46 RC §3（UC-CT-03 triggerInvoice→AP/AR 草稿 经 daoFor 直写） |
| 4 | logistics 部分签收 | `GatewayDispatcher.advanceTracking:162-188`（仅 DELIVERED）+ `ErpLogConstants.java:37-39`（仅 PICKED_UP/IN_TRANSIT/DELIVERED）+ 零 TRACKING_EVENT_PARTIAL + 零 partial 字段 + `MockCarrierGatewayClientFactory:118-119`（仅 IN_TRANSIT/DELIVERED 事件） | `2026-07-28-1249-arm-ma2-aps-logistics-state-machine.md`（P1-MA2-079 部分签收完全未实现已证实）；A1.49 RC §3（UC-LOG-06 reuse P1-MA2-079 resolved R1.25 via deferral） |
| 5 | 跨公司 orgId 隔离 | `ErpOrgContext.java`（CONFIG_ORG_ISOLATION_ENABLED 默认 false）+ `ErpOrgIsolationOrmInterceptor.java`（写入侧 auto-stamp）+ 全局 `ErpOrgIsolationQueryTransformer`（查询管道 orgId scope）+ 零 useTenant | `2026-07-28-1510-arm-ma2-multi-company-isolation.md`（P1-MA2-093/094 orgId 隔离未落地[历史] → resolved R1.29）；A1.7/A1.27/A1.41/A1.44 RC reuse（dashboard 直访路径覆盖） |
| 6 | 多账套 acctSchemaId 读路径隔离 | `SchemaPropagator.java`（multi-schema-enabled 默认 false）+ 读路径 period.orgId + 主账套 scope 过滤（R1.29 修复）+ 报表取主账套 FINANCIAL 非按账套切换渲染 | `2026-07-28-1510-arm-ma2-multi-company-isolation.md`（P1-MA2-095 读路径泄漏[历史] → resolved R1.29）；A1.7 RC §4（:142 HEAD 复核已修复不双计 + §7 SP-2/A4.1.23 每账套独立三表渲染 successor） |
| 7 | 全域 action-level RBAC | 零 `@BizAuth`（全域裸注解）+ `_erp-*.action-auth.xml` per-action FNPT 声明（R2.7）+ `ErpRoleDataAuthChecker`（R3.4 config-gated OFF）+ `nop.auth.enable-action-auth=false` 默认 | `2026-07-28-1953-arm-ma3-api-contract-consistency.md`（P1-MA3-046 全域敏感动作零权限保护）+ `2026-07-29-1410-arm-ma6-{permission-annotation-completeness,permission-depth-sampling,data-permission-runtime}.md`（R2.7/R3.4 done + 合并入 P1-MA3-046） |
| 8 | OPEN_AUDIT 轮次形式化 | `2026-07-31-1439-1-r3-5-...md`（Round 3 sweep 14 份 PASS）+ `2026-07-17-0900-1:48` + `2026-07-14-1449-1`（OPEN_AUDIT 形式化 Deferred）+ arm-index `:613`（R3.5 闭合回填） | `2026-07-29-1410-arm-ma6-protected-area-discipline.md`（P1-MA6-005 第三波 closure-pending 超集）；R3.5 plan closure audit（option A 14 份 PASS + option B 制度化 successor） |

---

## 4. 运行时行为证据（段 4，复用既有 arm MA2/MA3/MA6，§去重协议）

> 本 mission MA3 = successor 触发条件复查（需求契约视角），与 audit-remediation MA2（状态机/链路行为视角）/ MA3（文档/契约/API 一致性视角）/ MA4（代码质量视角）/ MA6（安全权限运行时视角）维度不重叠（methodology §去重协议）。既有 arm 报告 + A1.x RC 报告已证实的运行时行为直接引用：

- **#1 contract EXPIRED Job + 续期草稿**：合同 ACTIVE→EXPIRED 手工 expire 主路径完整（`expire()` require ACTIVE 守卫 + setStatus EXPIRED + updateEntity），InvoicePlan 生成经合同头 ACTIVE 守卫隐式失效（triggerInvoice 仅 ACTIVE 可触发，EXPIRED 合同不可再生成 unposted DRAFT 草稿）——经 `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md` + A1.45+A1.46 RC §4 证实。自动到期 Job + 续期草稿缺失属 missing-automation（手工路径存在兜底），无运行时数据破坏。
- **#2 b2b EDI 出站自动化**：EdiDoc 信封状态机 TO_SEND→SENT→ACKNOWLEDGED 手工迁移完整（markSent/markAcknowledged/retry/cancel/archive），config-gated `erp-b2b.enabled` 默认 OFF + MFT Mock-only Deferred → 默认 config 零生产暴露，缺失出站自动化不破坏状态机（仅未自动化）——经 `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md` + A1.47 RC §4 证实。
- **#3 contract InvoicePlan 跨域写收敛**：InvoicePlan triggerInvoice→AP/AR 草稿创建行为正确（SUSPENDED+ACTIVE 守卫 + isInvoiced 回写 + 经 daoFor 直写创建 unposted DRAFT 经人工审批管道兜底）——经 `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md` + A1.45+A1.46 RC §4 证实。daoFor 直写 vs I*Biz Facade 是治理面差异（O-4 写侧豁免 documented），不破坏业务正确性。
- **#4 logistics 部分签收**：完整签收 DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED 主路径完整（网关 SPI 重试 maxRetries=3 指数退避 + deadLetter 死信 + scanForPolling 轮询兜底 + DELIVERED→onDelivered FREIGHT 过账），部分签收是 owner doc Deferred 业务场景（承运商回调暂只发完整 DELIVERED 事件），缺失部分签收不破坏主路径——经 `2026-07-28-1249-arm-ma2-aps-logistics-state-machine.md` + A1.49 RC §4 证实。
- **#5 跨公司 orgId 隔离**：orgId 隔离机制（ErpOrgContext + IOrmInterceptor + ErpOrgIsolationQueryTransformer）已 R1.29 落地 config-gated OFF（单组织基线零回归），单组织种子 176 行全 orgId=2 掩盖跨组织泄漏——多组织部署须显式 `org-isolation-enabled=true` 启用。机制存在但部署未启用 → 无运行时数据破坏（单组织基线）——经 `2026-07-28-1510-arm-ma2-multi-company-isolation.md` + A1.7/A1.27/A1.41/A1.44 RC §4 证实。
- **#6 多账套 acctSchemaId 读路径隔离**：读路径双计已 R1.29 修复（period.orgId + 主账套 scope 过滤，A1.7 RC §4 HEAD 复核确认不双计），多账套传播写路径 stamp 彻底；多账套部署须 `multi-schema-enabled=true` 启用（默认 false 单账套向后兼容）。读路径取主账套 FINANCIAL 是合理简化（GlBalance 物理按 acctSchemaId 隔离已落地），「每账套独立三表」按账套切换渲染 successor 未驱动——经 `2026-07-28-1510-arm-ma2-multi-company-isolation.md` + A1.7 RC §4 证实。
- **#7 全域 action-level RBAC**：action-level FNPT 声明（`_erp-*.action-auth.xml`）+ enforcement beans（`ErpRoleDataAuthChecker`）已 R2.7/R3.4 落地 config-gated OFF（`enable-action-auth=false` + `enable-data-auth=false` + `role-row-filter-enabled=false` 三层默认 OFF），单组织基线 + admin skip-check 下零回归；翻转至 enforcement 须人工批准 + role 种子 + 灰度计划。平台 HTTP 认证默认开启兜底——经 `2026-07-28-1953-arm-ma3-api-contract-consistency.md` + `2026-07-29-1410-arm-ma6-*` + A1.x RC §4 证实。
- **#8 OPEN_AUDIT 轮次形式化**：第三波 closure-pending「completed」计划超集已 R3.5 Round 3 sweep 闭合（14 份独立子代理 fresh session closure-audit PASS），代码已落地 + `mvn test` 全绿 + 无活跃数据破坏；OPEN_AUDIT 轮次形式化（option B 制度化定期循环）属过程纪律 governance successor，缺失不破坏代码正确性——经 `2026-07-29-1410-arm-ma6-protected-area-discipline.md` + R3.5 plan closure audit 证实。

---

## 5. 复查结论（段 5，§6 MA3 适配：触发条件状态 + 回队决策）

> 复查结论三分：`回队 MR1`（触发条件已满足 / Q4 强制）/ `维持 backlog successor`（触发条件未满足）/ `补登记`（owner doc 内嵌但 arm-index 无行）。

### 5.1 逐项复查结论

| # | successor 项 | 触发条件状态 | 证据 | 回队决策 | 与 A2.x 关闭裁决交叉 |
|---|-------------|-------------|------|---------|---------------------|
| 1 | contract EXPIRED 自动到期 Job + 续期草稿 | ❌ 未满足（合同到期自动化需求未驱动） | 零 `ErpCtContractExpiryJob` + 零 `auto-create-renewal-draft` config + `parentContractId` 零业务使用 + 手工 `expire():123` 兜底 | **维持 backlog successor** | #1 ↔ `P1-MA2-071`（A2.8：resolved R1.22 via deferral；A1.45+A1.46 RC §4 三判据复核 deferral 不成立倾向 MR1）一致；successor 维持 ≠ finding 重开（finding §4 三判据复核归 A1.45/A2.8→MR1 通道，两者各自裁决交叉不冲突） |
| 2 | b2b EDI 出站自动化 | ❌ 未满足（MFT transport 真实对接未上线 + b2b config-gated OFF） | `TransportManager` wired-but-uncalled + createOutbound 留 TO_SEND + 零 nop-job/ack-timeout config + Mock transport + `erp-b2b.enabled` default false | **维持 backlog successor** | #2 ↔ `P1-MA2-073`（A2.8：resolved R1.23 via deferral；A1.47 RC reuse 确认）一致；**外部集成保护区域**：修复实施须 ask-first + 独立 plan-audit（§5） |
| 3 | contract InvoicePlan 跨域写收敛为 I*Biz | ❌ 未满足（pur/sal purpose-built Facade 未提供） | `createApInvoiceDraft:74`/`createArInvoiceDraft:111` daoFor 直写 saveEntity + pur/sal 零 `createFromInvoicePlan` Facade | **维持 backlog successor** | #3 ↔ `P1-MA1-029`（A2.8：resolved 写侧豁免补登）一致；successor 维持 ≠ finding 重开；**跨域契约协调**：收敛须与 pur/sal 协调 ask-first（同 A3.2 #2 范式） |
| 4 | logistics 部分签收 | ❌ 未满足（承运商部分签收回调未提供） | 零 TRACKING_EVENT_PARTIAL + 零 partial 字段 + advanceTracking 仅 DELIVERED | **维持 backlog successor** | #4 ↔ `P1-MA2-079`（A2.8：resolved R1.25 via deferral）一致；**ORM ask-first 保护区域**：修复实施须 ask-first + 独立 plan-audit（§5） |
| 5 | 跨公司 orgId 隔离查询/写入 | ❌ 未满足（多组织部署未启用，finding 机制已 R1.29 落地） | `ErpOrgContext` + `ErpOrgIsolationOrmInterceptor` + `ErpOrgIsolationQueryTransformer` 落地 + `org-isolation-enabled` 默认 false + 零 useTenant | **维持 backlog successor** | #5 ↔ `P1-MA2-093/094`（A2.9：resolved R1.29 via implementation）一致；**§对账差异 #5 核心核实项**：finding 已修复（机制已实现）≠ successor 全部关闭，successor 部署侧未启用 → 不回队 MR1（避免误将已修复 finding 重新纳入） |
| 6 | 多账套 acctSchemaId 读路径隔离 | ❌ 未满足（multi-schema-enabled 默认 false + 每账套独立三表渲染未驱动，finding 读路径已 R1.29 修复） | `SchemaPropagator` + 读路径 period.orgId + 主账套 scope 过滤（R1.29）+ 报表取主账套 FINANCIAL 非按账套切换 | **维持 backlog successor** | #6 ↔ `P1-MA2-095`（A2.9：resolved R1.29 via implementation）一致；**§对账差异 #5 核心核实项**：finding 已修复（读路径隔离已实现）≠ successor 全部关闭，successor 多账套部署渲染未验证 → 不回队 MR1；**与 A4.1.23 交叉引用**（不同控制点：MA3 successor 触发条件[部署启用] vs MA4 运行时渲染行为[每账套独立三表渲染]） |
| 7 | 全域敏感动作 action-level RBAC | ❌ 未满足（灰度翻转未批准 + role 种子未补全，finding FNPT 声明 + enforcement beans 已 R2.7/R3.4 落地 config-gated OFF） | 零 `@BizAuth` + `_erp-*.action-auth.xml` FNPT 声明（R2.7）+ `ErpRoleDataAuthChecker`（R3.4）+ `enable-action-auth=false` 默认 + owner doc §运行基线 deliberate | **维持 backlog successor** | #7 ↔ `P1-MA3-046`（A2.x 跨域 + A6.1/A6.2/A6.3：done R2.7 部分修复，enforcement beans config-gated OFF；R4.1 四维度 adjudicated 协同闭合）一致；**§对账差异 #5 核心核实项**：finding 已部分修复 ≠ successor 全部关闭，successor 灰度翻转未批准 → 不回队 MR1；**owner doc 显式声明有意默认**（deliberate design 非 silent gap） |
| 8 | OPEN_AUDIT 轮次形式化 | ❌ 未满足（制度化定期循环未建立，finding 第三波 closure-pending 已 R3.5 sweep 闭合） | R3.5 Round 3 sweep 14 份 PASS（option A）+ OPEN_AUDIT 形式化 option B Deferred | **维持 backlog successor** | #8 ↔ `P1-MA6-005`（A6.4：done R3.5 option A 一次性 sweep，option B 制度化 Deferred）一致；**§对账差异 #5 核实项**：finding 已修复（第三波 sweep 闭合）≠ successor 全部关闭，successor 制度化未建立 → 不回队 MR1 |

### 5.2 统计

- **回队 MR1**：0 项（8 项触发条件全部未满足[#1/#2/#3/#4/#5/#6/#7/#8 均未满足 + §对账差异 #5 各项经区分 finding 已修复 vs successor 残留后核心 successor 仍维持 backlog]，无 Q4 强制回队）
- **维持 backlog successor**：8 项（#1-#8 全部维持 backlog，待各自触发条件满足）
- **补登记**：0 项（8 项均有 S1 arm-index 覆盖，#2 另有 S2 owner doc 双源覆盖，无 owner doc 内嵌但 arm-index 无行的遗漏项）
- **本审计新发现 P0**：0 项（无 MR0 即时通道触发）

### 5.3 结构性约束（§对账差异 #5 + 保护区域 + A4.1.23 交叉引用 + A3.3 maintenance 交叉引用 + reuse 交叉引用）

- **§对账差异 #5（实现修复项 successor 残留）**：8 项的 finding 均已有 resolution（#1 resolved-via-deferral R1.22 / #2 resolved-via-deferral R1.23 / #3 resolved 写侧豁免 / #4 resolved-via-deferral R1.25 / #5/#6 resolved-via-implementation R1.29 / #7 done R2.7 部分修复 / #8 done R3.5 option A sweep）。本 A3.5 严格区分「finding 已修复/关闭」与「successor 仍待触发」——**不误将已修复 finding 重新纳入 MR1**（finding 重开/关闭归 A1.x/A2.x→MR1 通道，successor 维持归 A3.x，两者各自裁决）。**#1/#5/#6/#7 是此项纪律的核心体现**（plan baseline 命名核心核实项）：#5/#6 finding resolved-via-implementation（机制已落地）+ #7 finding done R2.7 partial（声明+beans 已落地 config-gated OFF）+ #1 finding resolved-via-deferral（A1.45 §4 三判据复核倾向 MR1）——successor 均维持 backlog。
- **保护区域（§5）**：**#2 外部集成保护区域**（真实 MFT transport AS2/SFTP/FTPS 对接修复实施须 ask-first + 独立 plan-audit）；**#4 ORM ask-first 保护区域**（部分签收字段 receivedQuantity/partialSignedQty ORM 加列须 ask-first + 独立 plan-audit）。本 A3.5 仅裁决 successor 触发条件（均未满足→维持 backlog），保护区域变更属 MR1 修复期门控，非本裁决期。
- **#6 与 A4.1.23 交叉引用（不同控制点）**：A4.1.23（MA4 运行时行为验证维度：多账套部署「每账套独立三表」运行时渲染）vs 本 A3.5 #6 successor（MA3 successor 触发条件维度：`multi-schema-enabled=true` 部署启用）。两者不同控制点，交叉引用不重复（A1.7 RC §7 SP-2 → A4.1.23 运行时展开 + 本 A3.5 #6 successor 触发条件维持）。
- **与 A3.3 maintenance 域投影交叉引用（不重复裁决）**：A3.3 #5（employee-id 行过滤，quality inspectorId / maintenance assignedTo 跨域投影）已由 A3.3 合并裁决一次（ORM ask-first 保护区域，successor 维持 backlog）。本 A3.5 8 项 successor **不含 maintenance employee-id 投影**，故交叉引用 A3.3 #5 结论，不重复裁决。
- **reuse 交叉引用**：#5 orgId 隔离（P1-MA2-093 经 A1.7/A1.27/A1.41/A1.44 RC reuse dashboard 行级权限）/ #7 RBAC（P1-MA3-046 经 A1.6/A1.47/A1.x RC reuse 高危动作权限）从需求契约视角 reuse 交叉引用（同根因同控制点，按 §7 复用既有 finding ID 不新建）。本 A3.5 successor 裁决与 A1.x reuse 注记互补不重复。

---

## 6. 与 arm-index 衔接（段 6，§7「复用 or 新增」裁决）

> §7 规则：successor 项均源自既有 arm finding，本复查原则上**复用既有 finding ID**追加 RC MA3 注记；仅当发现 owner doc 内嵌但 arm-index 无独立行的 successor（如 A3.1 #7/#8 补登记）才补登记。本 A3.5 8 项**全部有 S1 arm-index 覆盖**（8 项均有既有 arm finding 行），故**全部复用**，无补登记。

### 6.1 逐项「复用 or 补登记」裁决

| # | successor 项 | arm-index grep 结果 | 裁决 | 操作 |
|---|-------------|---------------------|------|------|
| 1 | contract EXPIRED 自动到期 Job + 续期草稿 | 既有 `P1-MA2-071` 行（A2.14 :674 narrative + finding 行）含 successor 声明（state-machine.md §2/§4/§7 Deferred） | **复用** | 既有行追加「RC MA3 复查（A3.5）：触发条件未满足[合同到期自动化需求未驱动]→维持 backlog successor；§对账差异 #5 核实项——finding resolved R1.22 via deferral[A1.45+A1.46 RC §4 三判据复核倾向 MR1 属 A1.x→MR1 通道] + successor 维持 ≠ finding 重开」注记 |
| 2 | b2b EDI 出站自动化 | 既有 `P1-MA2-073` 行（arm-index :516）含 successor 声明（MFT transport 真实对接） | **复用** | 既有行追加「RC MA3 复查（A3.5）：触发条件未满足[MFT transport 真实对接未上线 + b2b config-gated OFF]→维持 backlog successor；**外部集成保护区域**[修复实施须 ask-first + 独立 plan-audit]；successor 维持 ≠ finding 重开[finding resolved R1.23 via deferral 归 A2.8]」注记 |
| 3 | contract InvoicePlan 跨域写收敛为 I*Biz | 既有 `P1-MA1-029` 行（A2.8 :111 分区）含 successor 声明（pur/sal 提供 purpose-built Facade 时收敛） | **复用** | 既有行追加「RC MA3 复查（A3.5）：触发条件未满足[pur/sal purpose-built Facade 未提供]→维持 backlog successor；跨域写经 daoFor 直写[createApInvoiceDraft:74/createArInvoiceDraft:111]半治理豁免 documented；successor 维持 ≠ finding 重开[finding 写侧豁免归 A2.8]」注记 |
| 4 | logistics 部分签收 | 既有 `P1-MA2-079` 行（arm-index :522）含 successor 声明（承运商支持部分签收回调时） | **复用** | 既有行追加「RC MA3 复查（A3.5）：触发条件未满足[承运商部分签收回调未提供]→维持 backlog successor；**ORM ask-first 保护区域**[修复实施须 ask-first + 独立 plan-audit]；successor 维持 ≠ finding 重开[finding resolved R1.25 via deferral 归 A2.8]」注记 |
| 5 | 跨公司 orgId 隔离查询/写入 | 既有 `P1-MA2-093/094` 行（A2.18 :789 narrative + :537 P1-MA2-094 finding 行 + P1-MA2-093 finding 行）含 successor 声明（多组织部署启用时） | **复用** | 既有行追加「RC MA3 复查（A3.5）：§对账差异 #5 核心核实项——finding resolved R1.29 via implementation[ErpOrgContext+IOrmInterceptor+ErpOrgIsolationQueryTransformer 落地 config-gated OFF]，successor[多组织部署启用]触发条件未满足→维持 backlog；不回队 MR1[避免误将已修复 finding 重新纳入]」注记 |
| 6 | 多账套 acctSchemaId 读路径隔离 | 既有 `P1-MA2-095` 行（arm-index :538）含 successor 声明（multi-schema-enabled=true 启用时） | **复用** | 既有行追加「RC MA3 复查（A3.5）：§对账差异 #5 核心核实项——finding resolved R1.29 via implementation[读路径 period.orgId+主账套 scope 过滤]，successor[multi-schema-enabled=true 部署下每账套独立三表渲染]触发条件未满足→维持 backlog；不回队 MR1；**与 A4.1.23 交叉引用**[不同控制点：MA3 successor 触发条件 vs MA4 运行时渲染行为]」注记 |
| 7 | 全域敏感动作 action-level RBAC | 既有 `P1-MA3-046` 行（A3.6 :415 narrative + :875 四维度交叉 adjudicated R4.1）含 successor 声明（灰度翻转） | **复用** | 既有行追加「RC MA3 复查（A3.5）：§对账差异 #5 核心核实项——finding done R2.7 部分修复[per-action FNPT 声明+ErpRoleDataAuthChecker enforcement beans 落地 config-gated OFF]，successor[灰度翻转至 enforcement]触发条件未满足[须人工批准+role 种子+灰度计划]→维持 backlog；不回队 MR1；owner doc 显式声明有意默认[deliberate design 非 silent gap]」注记 |
| 8 | OPEN_AUDIT 轮次形式化 | 既有 `P1-MA6-005` 行（arm-index :601 + :613 R3.5 闭合回填）含 successor 声明（OPEN_AUDIT 轮次形式化 option B） | **复用** | 既有行追加「RC MA3 复查（A3.5）：§对账差异 #5 核实项——finding done R3.5[Round 3 sweep 14 份 PASS option A]，successor[OPEN_AUDIT 轮次形式化 option B 制度化定期循环]触发条件未满足→维持 backlog；不回队 MR1」注记 |

**裁决依据**：8 项均为既有 arm finding 的同一根因/同一控制点 successor，复用既有 ID 追加 RC MA3 注记。**不新建 `P*-RC-xxx`**（禁止未经比对直接新建）——8 项全部有既有 arm-index 行覆盖，无 owner doc 内嵌但 arm-index 无独立行的遗漏项（与 A3.1 #7/#8 补登记情形不同）。

### 6.2 双向可追溯

- **回队项 ↔ MR1 R1.0 预留展开行**：**0 项**（8 项触发条件全部未满足，无回队 MR1）。
- **维持 backlog 项 ↔ A3.x successor 登记**：#1-#8 全部维持 backlog，交叉引用本 A3.5 报告 + arm-index successor 注记。
- **finding 重开/关闭项（非本 A3.5 裁决，交叉引用）**：#1（`P1-MA2-071` 经 A1.45+A1.46 RC §4 三判据复核 deferral 不成立倾向 MR1[**属 A1.x→MR1 通道**] / #2（`P1-MA2-073` 归 A2.8 RC resolved R1.23 via deferral）/ #3（`P1-MA1-029` 归 A2.8 RC 写侧豁免）/ #4（`P1-MA2-079` 归 A2.8 RC resolved R1.25 via deferral）/ #5（`P1-MA2-093/094` resolved R1.29 via implementation，**不重开**）/ #6（`P1-MA2-095` resolved R1.29 via implementation，**不重开**）/ #7（`P1-MA3-046` done R2.7 部分修复，**不重开**）/ #8（`P1-MA6-005` done R3.5，**不重开**）——这些 finding 裁决属 **A1.x/A2.x→MR1 通道**，与本 A3.5 successor 裁决各自独立（§MA2↔MA3 协作：关闭裁决/finding 修复归 A1.x/A2.x，successor 触发条件归 A3.x，交叉引用不重复）。
- **arm-index 回填**：§6.1 注记已写入 `arm-index.md`（8 既有行追加 RC MA3 注记）。

---

## 7. 静态存疑点清单（段 7，供 MA4 A4.2 展开）

> L5 无法静态定论、需运行时确认的点。本复查为 successor 触发条件复查（读 arm-index/owner doc/实仓代码/config/ORM），以下为复查中静态无法定论、建议 MA4/A4.2 运行时确认的点：

1. **#5 多组织部署 orgId 隔离 enforcement 运行时有效性**（`org-isolation-enabled=true` 启用后跨组织查询/写入是否实际被 `ErpOrgIsolationQueryTransformer` + `ErpOrgIsolationOrmInterceptor` 强制隔离）：本复查静态确认 orgId 隔离机制（ErpOrgContext + IOrmInterceptor + QueryTransformer）已 R1.29 落地 config-gated OFF，但多组织部署未启用（单组织种子 orgId=2 掩盖）。未来多组织部署 `org-isolation-enabled=true` 启用时，跨组织查询/写入 enforcement 的运行时有效性需 MA4 运行时探针确认（与 A1.7 SP-4 / A1.27 SP-4 / A1.41 SP-1 / A1.44 SP-5 dashboard 直访路径运行时覆盖同根因，复用 P1-MA2-093）。**低优先级**（successor 触发条件未满足，本存疑点仅为前瞻性排除多组织部署隔离风险）。

> 其余 7 项（#1/#2/#3/#4/#6/#7/#8）的运行时行为已由既有 arm MA2/MA3/MA6 报告 + A1.x RC §4 充分证实（§4），无新增静态存疑点。特别地：
> - #6 多账套 acctSchemaId 读路径经 A1.7 RC §4 + `2026-07-28-1510-arm-ma2-multi-company-isolation.md` HEAD 复核确认读路径不双计（period.orgId + 主账套 scope 过滤已落地），「每账套独立三表」按账套切换渲染 successor = A4.1.23 已登记运行时存疑点（不同控制点交叉引用）。
> - #7 action-level RBAC 经 `2026-07-29-1410-arm-ma6-{permission-annotation-completeness,permission-depth-sampling,data-permission-runtime}.md` 运行时证实 enforcement beans config-gated OFF 单组织基线零回归，灰度翻转 successor 触发条件未满足。
> - #2 b2b EDI 经 `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md` + A1.47 RC §4 运行时证实 EdiDoc 手工状态机完整 + config-gated OFF 默认零生产暴露。

---

## 8. 过程纪律自检（段 8，§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（actual 见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（EXIT_CODE=0 恒定，不反映 actual vs baseline），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以 checker 脚本退出码作为门控通过依据**。**本审计无生产代码变更（纯审计报告 + arm-index 文档注记），checker 无回归风险**——actual 计数与本审计行为正交（未触及任何生产代码），任何 actual vs baseline 差异均非本审计引入。

  | 规则 | 基线（compliance-baseline.md §BASELINE machine-readable） | actual（本次实测） | 漂移 | 归因 |
  |------|-------------------------------|-------------------|------|------|
  | R1a | 0 | 0 | 0 | — |
  | R1b | 0 | 0 | 0 | — |
  | R1c | 0 | 0 | 0 | — |
  | R1d | 14 | 14 | 0 | — |
  | R2a | 34 | 34 | 0 | — |
  | R2b | 229 | 229 | 0 | — |
  | R2c | 1382 | 1382（生产代码总计） | 0 | — |
  | R2d | 34 | 34 | 0 | — |

  > 本审计仅产出本报告 + `arm-index.md` 注记（纯文档），未触及 `module-*/` 任何生产代码。actual 全规则 = baseline，零漂移，无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计（见来源 plan Closure Gates）。
- [x] **与 arm-index 交叉去重声明**：本报告全部 8 项 successor 已按 §7 规则 grep arm-index 同域同控制点后给出「复用」裁决（§6.1），无未经比对直接新建的 `P*-RC-xxx` finding（8 项全部复用既有 arm finding ID 追加 RC MA3 注记）。

---

## 9. 与既有审计差异增量声明（段 9，§去重协议）

本报告与既有 arm 审计（`docs/audits/2026-07-2*-arm-ma2-*` + `2026-07-28-1953-arm-ma3-*` + `2026-07-29-1410-arm-ma6-*`）+ A1.x RC 复查报告的差异增量：

- **复用既有证据**（不重复验证）：
  - `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（#1 P1-MA2-071 contract EXPIRED Job 缺失 + #2 P1-MA2-073 b2b EDI 出站自动化缺失 + #3 P1-MA1-029 InvoicePlan 跨域写半治理 已证实）；
  - `2026-07-28-1249-arm-ma2-aps-logistics-state-machine.md`（#4 P1-MA2-079 logistics 部分签收完全未实现已证实）；
  - `2026-07-28-1510-arm-ma2-multi-company-isolation.md`（#5 P1-MA2-093/094 orgId 隔离 + #6 P1-MA2-095 acctSchemaId 读路径泄漏 已证实 → resolved R1.29 via implementation）；
  - `2026-07-28-1953-arm-ma3-api-contract-consistency.md`（#7 P1-MA3-046 全域敏感动作零权限保护）；
  - `2026-07-29-1410-arm-ma6-{permission-annotation-completeness,permission-depth-sampling,data-permission-runtime,protected-area-discipline}.md`（#7 P1-MA3-046 done R2.7 + R3.4 角色侧行过滤 + #8 P1-MA6-005 第三波 closure-pending done R3.5）；
  - A1.x RC 报告（A1.7 finance-F7 §4 HEAD 复核 P1-MA2-095 已修复 + §7 SP-2/A4.1.23 每账套独立三表渲染 / A1.27 inventory-F3 reuse P1-MA2-093 dashboard 行级权限 / A1.41 master-data reuse P1-MA2-093 / A1.44 maintenance-F3 reuse P1-MA2-093 / A1.45+A1.46 contract reuse P1-MA2-071 §4 三判据复核 + P1-MA1-029 / A1.47 b2b reuse P1-MA2-073 + P1-MA3-046 / A1.49 logistics reuse P1-MA2-079）已证实的代码路径 + §4 三判据复核。

- **本复查只补的差异增量**：**successor 触发条件是否已满足 + 是否该回队**——从 methodology §MA3 四任务（① 触发条件状态 grep 实仓验证 / ② 回队决策 / ③ 补登记 / ④ README 覆盖复核）出发，逐项核证 8 项扩展域+跨域 successor 的触发条件现状。这是既有 arm 审计（状态机/链路行为维度 + 文档/契约/API 一致性维度 + 代码质量维度 + 安全权限运行时维度）+ A1.x RC（L1 验收标准视角 + §4 三判据 finding 复核维度）未覆盖的「successor 触发条件完整性 + 回队决策」维度（methodology §去重协议 §MA2↔MA3 协作——关闭裁决/finding 修复归 A1.x/A2.x，successor 触发条件归 A3.x，交叉引用不重复）。特别地，**§对账差异 #5 核实**（区分 #1/#5/#6/#7/#8 finding 已修复/关闭 vs successor 残留）+ **#6 与 A4.1.23 交叉引用**（不同控制点）+ **与 A3.3 maintenance 域投影交叉引用**（不重复裁决）是本 A3.5 独有的裁决维度。

- **不重复**：不重做 doc↔code 文本一致性（audit-remediation MA3 已收口）、不重做状态机/链路行为（arm MA2 已收口）、不重做代码质量（arm MA4 已收口）、不重做安全权限运行时（arm MA6 已收口）、不重审方案 B 关闭裁决本身（A2.x RC 已收口）、不重审 finding §4 三判据复核（A1.x RC 已收口，本 A3.5 只复查 successor 触发条件，交叉引用）。

---

## 结论

扩展域+跨域 MA3 successor 复查（A3.5 — **MA3 里程碑收官行**）完成：8 项 design-level successor 逐项经 §MA3 四任务核证。

- **回队 MR1**：0 项（8 项触发条件全部未满足，无 Q4 强制回队）。
- **维持 backlog successor**：8 项（#1-#8 全部维持 backlog，待各自触发条件满足）：
  - #1 contract EXPIRED 自动到期 Job + 续期草稿（触发=合同到期自动化需求；A1.45 §4 三判据复核 finding 倾向 MR1 属 A1.x 通道）；
  - #2 b2b EDI 出站自动化（触发=MFT transport 真实对接上线；**外部集成保护区域 ask-first**）；
  - #3 contract InvoicePlan 跨域写收敛为 I*Biz（触发=pur/sal 提供 purpose-built Facade；跨域契约协调 ask-first）；
  - #4 logistics 部分签收（触发=承运商支持部分签收回调；**ORM ask-first 保护区域**）；
  - #5 跨公司 orgId 隔离查询/写入（触发=多组织部署启用；finding 机制已 R1.29 落地 config-gated OFF，successor 部署侧维持）；
  - #6 多账套 acctSchemaId 读路径隔离（触发=`multi-schema-enabled=true` + 每账套独立三表渲染；finding 读路径已 R1.29 修复，successor 报表侧维持；**与 A4.1.23 交叉引用**）；
  - #7 全域敏感动作 action-level RBAC（触发=灰度翻转至 enforcement 须人工批准+role 种子+灰度计划；finding FNPT 声明+enforcement beans 已 R2.7/R3.4 落地 config-gated OFF，owner doc 显式声明有意默认）；
  - #8 OPEN_AUDIT 轮次形式化（触发=制度化定期 closure-pending 清理循环；finding 第三波 closure-pending 已 R3.5 sweep 闭合，successor 制度化维持）。
- **补登记**：0 项（8 项均有 S1 arm-index 覆盖，#2 另有 S2 owner doc 双源覆盖，无 owner doc 内嵌但 arm-index 无行的遗漏项）。
- **结构性约束**：§对账差异 #5（8 项 finding 已修复/关闭 vs successor 仍待触发 区分；**#1/#5/#6/#7 为核心核实项**——finding 修复 ≠ successor 全部关闭，但 successor 触发条件未满足时不回队，避免误将已修复 finding 重新纳入 MR1）；保护区域（#2 外部集成 + #4 ORM ask-first）；#6 与 A4.1.23 交叉引用（不同控制点）；与 A3.3 maintenance 域投影交叉引用（不重复裁决）；reuse 交叉引用（#5 P1-MA2-093 + #7 P1-MA3-046 经 A1.x reuse，两维度互补不重复）。
- **arm-index 衔接**：8 项全部复用既有 ID 追加 RC MA3 注记（无新 `P*-RC-xxx`，无补登记）。
- **MA3 里程碑收官**：A3.1-A3.5 五个域分组 successor 复查全部 done（finance 8 + mfg/inv/pur 7 + sal/ast/prj/qa 5 + hr/crm/cs 4 + 扩展域/跨域 8 = **32 项 successor** 全部经 §MA3 四任务核证），MA3 里程碑（successor 追踪完整性与回队复查）完成，解除 MR1（R1.0 展开器）链路的 successor 回队决策证据缺口。
- **本审计无生产代码变更**（纯报告 + arm-index 文档注记），§9 真相源冻结条款遵守（未修改 product-scope / owner doc 需求契约段落 / arm-index 已关闭 finding 的关闭事实 / backlog README）。
