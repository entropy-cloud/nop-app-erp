# 合同全生命周期管理域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 合同域状态对象：**合同（Contract）**（主轴，适用对象一）+ **合同版本（ContractVersion）**（辅助轴，与合同状态联动变更，适用对象二）+ **返利协议（RebateAgreement）**（退化分类轴，适用对象三）+ **返利结算（RebateSettlement）**（独立结算过账轴，适用对象四）。

## 适用对象一：合同（Contract）

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 版本管理影响 |
|------|----------------------|--------------|
| 草稿（DRAFT） | 合同正起草中，等待内部审批 | 可编辑，版本草稿 |
| 谈判中（NEGOTIATION） | 已提交谈判，等待双方确认条款 | 冻结版本内容，变更需 Amendment |
| 执行中（ACTIVE） | 合同已签署生效，正在执行 | 当前版本生效，执行回写 |
| 已中止（SUSPENDED） | 执行中临时中止（纠纷/暂停合作） | 版本冻结，不可开票/消耗 |
| 已到期（EXPIRED） | 终态：endDate 到达，正常到期 | 归档版本 |
| 已终止（TERMINATED） | 终态：提前终止（违约/协商解约） | 归档版本，注明终止原因 |
| 已作废（CANCELLED） | 终态：草稿废弃（仅 DRAFT 可作废） | 删除草稿版本（未生效，无业务影响） |

> L-5 补：CANCELLED 态在 §2 迁移图中早已使用（`DRAFT → CANCELLED` 草稿废弃路径），但 §1 定义表遗漏。CANCELLED 与 TERMINATED 的区别：CANCELLED 仅限 DRAFT 阶段的草稿废弃（未生效，无版本归档，无业务回写）；TERMINATED 是已生效合同（ACTIVE/NEGOTIATION）的提前终止（已生效，需归档版本与关联终止协议）。

> **实现漂移注记（plan 2026-08-12-1118-1 层 2 四方对照裁定）**：CANCELLED 为**目标态未落地**——dict `erp-ct/contract-status`（`module-contract/model/app-erp-contract.orm.xml`）仅含 6 值（DRAFT/NEGOTIATION/ACTIVE/SUSPENDED/EXPIRED/TERMINATED），**缺 CANCELLED 值**；全域零 `setStatus(CANCELLED)` writer。即 `DRAFT → CANCELLED` 草稿废弃迁移在 dict 与 writer 双侧均不存在，DRAFT 废弃当前经 CRUD 删除。`ErpCtContractStateMachine` Bean 据实不将 CANCELLED 纳入终态集。**Successor**：PM 要求草稿废弃命名动作时，开独立 plan 新增 dict 值 + cancel mutation（触及 `model/*.orm.xml` 保护区 ask-first）。

### 2. 迁移完整性

```
DRAFT（草稿）
  ├─ 提交审批 → NEGOTIATION（谈判中）
  │               ├─ 签署生效 → ACTIVE（执行中）
  │               │               ├─ endDate 到达 → EXPIRED（已到期，终态）
  │               │               ├─ 提前终止 → TERMINATED（已终止，终态）
  │               │               ├─ 中止 → SUSPENDED（已中止）
  │               │               │         └─ 恢复 → ACTIVE
  │               │               └─ 变更 → DRAFT（变更单走 DRAFT→NEGOTIATION→ACTIVE）
  │               └─ → TERMINATED（谈判破裂，终态）
  └─ → CANCELLED（取消，终态，未在 10 维度？但用于草稿废弃场景）
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| DRAFT→NEGOTIATION | 合同经办人 | 合同内容填完整，金额/条款/日期必填 | 创建 v1 版本（DRAFT→FINALIZED） |

> **DRAFT→NEGOTIATION 实现注记（RC-R1.32，plan 2026-08-15-0456-2）**：该迁移已由 `ErpCtContractBizModel#submit` @BizMutation 落地（§2 漂移注记已更新为已实现）——守卫仅 `status==DRAFT`（Bean `assertCanSubmitForNegotiation`，**不因已有版本拒绝**——amend 生命周期下变更 DRAFT 已有版本，submit 是其唯一前向出口）；校验经 `validateContractFields`（D1 语义：totalAmount==Σ行金额 DAO 口径 + startDate<endDate，`ERR_CT_AMOUNT_MISMATCH`/`ERR_CT_DATE_RANGE_INVALID`）；版本创建语义 = 零版本时自动创建 v1（versionNo=1、isCurrent=true、status=DRAFT——**初始态写入**，非经版本 Bean 迁移；「创建 v1 版本（DRAFT→FINALIZED）」读作「经 DRAFT→FINALIZED 演进」，activate 前须显式 `finalizeVersion`），已有版本时保留既有 DRAFT 当前版本不动（仅合同头 → NEGOTIATION）。
| DRAFT→CANCELLED | 合同经办人 | 草稿状态、无关联生效业务（无开票/消耗） | 删除草稿版本，记录作废原因 |
| NEGOTIATION→ACTIVE | 双方签署 | 合同文件签署完成（signDate 设置） | 版本状态→SIGNED，isCurrent=true |

> **电子签章接入点**：`IErpCtContractVersionBiz.signVersion`（FINALIZED→SIGNED + isCurrent 翻转）
> 既是线下签署的确认入口，也是电子签章 FULLY_SIGNED 完成时由 `ErpCtSignatureRequestBizModel.completeFullySigned`
> 自动调用的接入点（retrieveCertificate 后）。config-gated `erp-ct.e-signature-enabled`（默认关，未启用走线下签署附件上传 + 手动确认 SIGNED）。
| ACTIVE→EXPIRED | ~~系统自动~~（**Deferred**，见下方注） | endDate < now() | 归档版本，不可再修改 |

> **ACTIVE→EXPIRED 自动化 Deferred 注记**：原设计为「系统自动 endDate<now」，当前**无 `ErpCtContractExpiryJob`**（module-contract 全域零 Job 类、零 scheduler、零 `@CronProvider`），实际经运营手工调 `ErpCtContractBizModel.expire()` @BizMutation（contractId 单点）触发。**Deferred**：到期自动化属 missing-automation（新 Job 类 + scheduler + job.yaml 注册），与危害（状态悬挂非业财破坏——`expire()` 手工路径存在 + InvoicePlan 生成 unposted DRAFT 经人工审批兜底 + 不破坏业财一致）不成比例，对齐 missing-automation Deferred 范式。**Successor**：合同到期自动化需求时实现 `ErpCtContractExpiryJob`（cron-gated 扫描 ACTIVE 且 endDate<now 合同批量 expire），对齐 hr 域 `ErpHrContractExpiryJob` 范式。
| ACTIVE→TERMINATED | 合同管理员 | 终止协议签署，填写终止原因 | 版本归档，关联终止协议 |
| ACTIVE→SUSPENDED | 合同管理员 | 双方确认中止，填写中止原因 | 版本冻结 |
| SUSPENDED→ACTIVE | 合同管理员 | 中止状态解除，双方确认恢复 | 版本恢复生效 |
| NEGOTIATION→TERMINATED | 合同管理员 | 谈判破裂，双方确认终止 | 版本归档 |

> **迁移图实现漂移注记（plan 2026-08-12-1118-1 层 2 四方对照裁定 + RC-R1.32 更新）**：上图声明 9 条迁移边，其中**命名动作 writer 原未落地**两条，`ErpCtContractStateMachine` Bean 据实编码：
> - **DRAFT→NEGOTIATION（提交谈判）**：**RC-R1.32 已实现**（plan `2026-08-15-0456-2`）——`ErpCtContractBizModel#submit` @BizMutation + Bean `assertCanSubmitForNegotiation`/`submitTargetStatus()`（DRAFT→NEGOTIATION 边已编码，Bean 7 边 → 9 边）；NEGOTIATION 不再仅作为 activate/terminate 的源态被消费（M0.1 §9.4 残留风险收敛：合同仍可经 CRUD save 直设 NEGOTIATION，但命名动作路径已可达）。同时落地**驳回恢复边** `rejectAmend`（DRAFT→ACTIVE，UC-CT-02 异常路径）——恢复目标 = 优先 SIGNED 最大 versionNo（无 SIGNED 回落 FINALIZED 最大者，D5 选项 B：跨请求可行 + 对重复 amend/reject 周期遗留 DRAFT 行免疫 + 防 finalize-then-reject 恢复未签署 FINALIZED 为 current），遗留 DRAFT 版本行保留为孤儿历史。
> - **DRAFT→CANCELLED（草稿废弃）**：dict 缺 CANCELLED 值 + 零 writer（详见 §1 实现漂移注记），**仍为漂移**（Successor：PM 要求草稿废弃命名动作时开独立 plan，触及 `model/*.orm.xml` 保护区 ask-first）。
>
> Bean 编码**已实现**的 9 条边（submit/rejectAmend/activate/suspend/resume/terminate[多源]/expire/amend）。已实现边与本图一致，无 §迁移表 vs §实现约定 的内部语义漂移。

### 3. 终态与恢复

- 终态：`已作废（CANCELLED）`、`已到期（EXPIRED）`、`已终止（TERMINATED）`。
- 终态不可恢复。若需续签，从 EXPIRED 创建续期合同（parentContractId 关联原合同）。
- SUSPENDED 不是终态，可恢复为 ACTIVE。
- CANCELLED 仅限 DRAFT 阶段草稿废弃；已进入 NEGOTIATION 或后续态的合同不可作废，只能 TERMINATED。

> **实现约定**：`NEGOTIATION→TERMINATED` 迁移——`ErpCtContractBizModel.terminate` 守卫接受 `status∈{ACTIVE, NEGOTIATION}` 两种源态。NEGOTIATION 路径与 ACTIVE 路径行为一致（仅 `setStatus(TERMINATED) + updateEntity`），**无 signDate/version 归档差异**（NEGOTIATION 未生效，无需签署归档；版本归档经 `useLogicalDelete` 既有语义）。NEGOTIATION→TERMINATED 无独立法务审批门控（与 ACTIVE→TERMINATED 一致，均经 @BizMutation 入口权限 + e-signature config 覆盖）。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| ACTIVE 期间发现条款缺陷 | 创建变更单（Amendment），新建 DRAFT 子合同 → NEGOTIATION → 生效后替换原版本 |
| endDate 到达但合同仍在执行中 | 先标记 EXPIRED（**当前手工 `expire()`；自动批量见 §2 ACTIVE→EXPIRED Deferred 注**），同时自动创建续期草稿（**Deferred**——见下方注） |

> **续期草稿自动创建 Deferred 注记**：原设计为「`auto-create-renewal-draft` 配置驱动自动创建续期草稿」，当前**无 config 键**（`ErpCtConfigs` 仅有 volume-discount/rebate/invoiceplan-auto-trigger/settlement-mode/e-signature 五键，无 `erp-ct.auto-create-renewal-draft`），`parentContractId` 字段存在但**零业务 Java 代码使用**（grep 全 module-contract `renewal|续期|续签` 无匹配）。**Deferred**：与 EXPIRED Job 同根因（新 Job + config-gated 自动化），危害为状态悬挂非业财破坏。**Successor**：合同到期自动化需求时实现 config-gated `erp-ct.auto-create-renewal-draft`（经 `parentContractId` 关联续期草稿），随 `ErpCtContractExpiryJob` successor 一并接入；`parentContractId` 字段保留为预留语义入口。

> **残留风险注记（InvoicePlan triggerInvoice EXPIRED 守卫，watch-only residual）**：`ErpCtInvoicePlanBizModel.triggerInvoice` 仅守卫合同 `status==ACTIVE`——过期但未手工 expire 的 ACTIVE 合同（endDate 已过）仍可生成 **unposted DRAFT** 发票草稿（非 silent posted，经人工审批管道可拦截兜底）。**收敛路径**：`ErpCtContractExpiryJob` successor 接入后，到期合同自动 expire 使 `status=EXPIRED`，triggerInvoice 的 ACTIVE 守卫自然拒绝 EXPIRED 合同（无需独立修改 triggerInvoice 守卫）。
| SUSPENDED 期间有开票计划到期 | 拦截：SUSPENDED 状态下不可生成新发票 |
| 合同金额超预算 | 谈判阶段预算检查拦截；超预算需重新审批 |
| 合同文件丢失/附件损坏 | attachmentId 为空时不允许 ACTIVE |
| 并发修改合同版本 | 乐观锁（version 字段） |

### 5. 可达性

- 所有正式状态均从 DRAFT 出发。
- ACTIVE 为枢纽态，出边最多（→EXPIRED、→TERMINATED、→SUSPENDED）。
- 从 EXPIRED/TERMINATED 无出边。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| DRAFT→NEGOTIATION | 合同经办人 |
| NEGOTIATION→ACTIVE | 合同管理员（确认签署完成） |
| ACTIVE→SUSPENDED | 合同管理员（需法务审批） |
| SUSPENDED→ACTIVE | 合同管理员 |
| ACTIVE→TERMINATED | 合同管理员 + 法务审批 |
| 版本审批 | 法务/合规部门 |

危险操作：
- **提前终止**：需法务审批 + 终止协议签署确认。
- **SUSPENDED→ACTIVE 恢复**：需确认中止原因已解除。

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 采购订单引用合同 | PO 关联合同 code（弱指针），回写已执行金额 |
| 销售订单引用合同 | SO 关联合同 code（弱指针），回写已执行金额 |
| 开票计划触发生成发票 | InvoicePlan → 调用 purchase/sales 域 API 生成 AP/AR Invoice |
| 合同到期提醒 | ~~nop-job 定时扫描 endDate~~（**Deferred**——见 §2 ACTIVE→EXPIRED Deferred 注；到期前 30/15/7 天通知经 `IErpSysNotificationBiz` 随 `ErpCtContractExpiryJob` successor 一并接入，当前经运营手工跟踪 endDate） |

外部触发渠道：
- 合同管理员手工创建（主要渠道）。
- 续期自动创建（nop-job 驱动）。
- 订单引用关联（间接）。

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| DRAFT | 是 | assigned（经办人）—— 完善合同内容 |
| NEGOTIATION | 是 | assigned（合同管理员）—— 推进签署 |
| ACTIVE | 否（按 InvoicePlan 或 ConsumptionLine 生成 TODO） | — |
| SUSPENDED | 是 | assigned（合同管理员）—— 跟踪中止状态，待恢复 |
| EXPIRED | 否 | — |
| TERMINATED | 是 | assigned（合同管理员）—— 善后处理（结算/归档） |

避免"合同到期未处理"：
- endDate 前 30/15/7 天 → TODO assigned（经办人）确认续期/终止。
- endDate 到达后 7 天未处理 → 升级通知合同管理员上级。

### 9. 场景演练

#### 场景 A：采购合同起草 → 签署 → 执行 → 到期

1. 采购员创建新合同 → DRAFT（contractType=PURCHASE，contractDirection=INBOUND）。
2. 填写合同行（物料、数量、单价、金额）。
3. 从模板 ErpCtTemplate 加载条款 → 调整 → 提交审批 → NEGOTIATION。
4. 供应商确认条款并签署合同文件上传。
5. 合同管理员确认签署完成 → ACTIVE（signDate=now，isCurrent version）。
6. 合同执行期间，采购订单引用合同，回写已执行金额。
7. endDate 到达 → ~~系统自动标记 EXPIRED~~（**Deferred**：当前经运营手工 `expire()`；自动批量见 §2 ACTIVE→EXPIRED Deferred 注）。
8. ~~系统按配置 auto-create-renewal-draft=true → 自动创建续期草稿~~（**Deferred**：见 §4 续期草稿 Deferred 注；`parentContractId` 关联原合同）。

#### 场景 B：合同变更（Amendment）+ 开票计划

1. 执行中的合同需要变更条款（价格调整）。
2. 合同管理员创建变更单（新的 DRAFT 子合同，parentContractId 指向 ACTIVE 合同）。
3. 变更单走 DRAFT → NEGOTIATION → ACTIVE。
4. 变更单生效后，系统创建新版本（versionNo=2，isCurrent=true），旧版本 isCurrent=false。
5. 按合同条款生成 InvoicePlan（预付30% + 里程碑50% + 完工20%）。
6. 里程碑1完成 → InvoicePlan 按计划日期生成 AP Invoice 草稿。
7. 财务确认后发票过账。

#### 场景 C：合同提前终止 + 善后结算

1. 供应商违约，采购方决定提前终止合同（ACTIVE）。
2. 合同管理员填写终止原因、上传终止协议 → 提交法务审批。
3. 法务审批通过 → TERMINATED。
4. 系统截停所有未执行的 InvoicePlan（未开票的标记为作废）。
5. 已消耗/已收货的部分生成最终结算发票。
6. 合同归档（所有版本 + 终止协议）。
7. 善后 TODO 分配给经办人：确认尾款结算完成。

### 10. 与设计文档一致性

- 合同状态定义见 `contract/README.md` §ErpCtContract。
- 版本管理规则见 `contract/README.md` §业务规则。
- 状态码归 `module-contract/model/app-erp-contract.orm.xml`（dict: erp-ct/contract-status）。
- 版本实体 ErpCtContractVersion 记录完整版本历史。

## 适用对象二：合同版本（ContractVersion）

> 本节由 plan `2026-08-13-1430-3`（M3.18）补章节落地——owner doc 原仅覆盖 Contract 主轴，版本（ContractVersion）作为辅助状态对象仅以「版本管理影响」列散见于 §1/§2（上方）。本节集中建立 `ErpCtContractVersion.status` 轴（dict `erp-ct/version-status`）的权威迁移语义。
>
> 实体级状态机 Bean：`ErpCtContractVersionStateMachine`（`module-contract/erp-ct-service/.../statemachine/`，无状态，2 边 + 终态 SIGNED + 只读 `transitions()` 元数据）。

### 1. 状态定义

| status | 业务含义（等待什么） | 版本管理影响 |
|--------|----------------------|--------------|
| 草稿（DRAFT） | 版本正起草/可编辑，等待定稿 | 可编辑；amend 新建版本 seed 此态（初始态写入，§9.2 选项 c） |
| 定稿（FINALIZED） | 内容已冻结，等待签署 | 不可编辑；可被合同激活级联签署 |
| 已签署（SIGNED） | 终态：版本签署生效，归档不可回退 | isCurrent=true（同合同其他版本 isCurrent=false） |

dict `erp-ct/version-status` 3 值全部可达（无死状态）。状态码归 `module-contract/model/app-erp-contract.orm.xml:51-55`。

### 2. 迁移完整性

```
DRAFT（草稿）─ finalizeVersion ─→ FINALIZED（定稿）─ signVersion ─→ SIGNED（已签署，终态）
```

| 迁移 | 触发入口 | 前置条件（守卫） | 结果 |
|------|----------|------------------|------|
| DRAFT→FINALIZED | `ErpCtContractVersionBizModel.finalizeVersion`（@BizMutation） | `status==DRAFT`（Bean `assertCanFinalize`） | `status=FINALIZED` |
| FINALIZED→SIGNED | `ErpCtContractVersionSignVersionProcessor.signVersion` | `isCurrent==true`（动态业务守卫 `ERR_CT_VERSION_NOT_CURRENT`）AND `status==FINALIZED`（Bean `assertCanSign`） | `status=SIGNED` + isCurrent 原子翻转（同合同其他版本 isCurrent=false）+ approvedAt=now |

> **amend = 新建版本（非迁移）**：`ErpCtContractAmendProcessor` 在合同修订时对**新建版本行** seed `setStatus(VERSION_STATUS_DRAFT)`（初始态写入，非既有版本迁移），不调 Bean 的 `assertCanFinalize`/`assertCanSign`（契约 §9.2 初始态路径）。

> **RC-R1.32 版本族裁定注记（plan 2026-08-15-0456-2）**：
> - **submit 建 v1 = 初始态写入（D2 裁决）**：`ErpCtContractBizModel#submit` 零版本时自动创建 v1（versionNo=1、isCurrent=true、status=DRAFT）——与 amend 新建版本同型（初始态写入，不调 Bean `assertCan*`）；已有版本时零操作（amend 场景 v2 已 DRAFT current，submit 仅合同头 → NEGOTIATION，v2 保持 DRAFT 直至显式 `finalizeVersion`）。
> - **amend 行复制语义（D3 裁决）**：同合同 amend 模型（变更单 = 同合同 DRAFT 态，行留 contractId 下）下，L1「复制原合同所有行到变更单」的复制义务语义化 = **行保留**（版本切换不删行，变更 DRAFT 可编辑既有行）——零复制代码；逐行 newEntity 复制会在同 contractId 下产生重复行，与同合同模型冲突（否决）。
> - **rejectAmend 恢复语义（D5 裁决，选项 B）**：`ErpCtContractBizModel#rejectAmend`（DRAFT→ACTIVE）恢复前任 current 版本 = **优先 status==SIGNED 中 versionNo 最大者，无 SIGNED 回落 FINALIZED 最大者**（恢复时推导，跨请求可行；SIGNED 优先防 finalize-then-reject 恢复未签署 FINALIZED 为 current；status 过滤天然排除重复周期遗留 DRAFT 行）；遗留 DRAFT 版本行保留为孤儿历史（逻辑删除语义不破坏）；无候选边界（零版本 ACTIVE 合同 amend 后驳回）清空遗留 DRAFT 的 isCurrent 恢复「无 current 版本」前置不变量。
> - **activate 对非 FINALIZED current 版本静默跳过（D6 watch-only 登记）**：amend→submit 路径下 v2 保持 DRAFT current 时 activate 级联 signVersion 仅对 FINALIZED 生效（`ErpCtContractActivateProcessor:52-55`），静默跳过未补齐守卫（行为变更超出 RC-R1.32 范围）——**activate 前须显式 `finalizeVersion(v2)`**（版本 Bean `assertCanFinalize` 仅收 DRAFT，可达性有保证；测试 `TestErpCtContractCreateValidate#testFullChainAmendPath` 覆盖该契约）。

### 3. 与 Contract 主轴联动（跨聚合级联）

版本签署（FINALIZED→SIGNED）有两条触发路径，**守卫统一在 signVersion Processor 内**（注入 Version Bean）：

1. **版本自身命名动作**：`IErpCtContractVersionBiz.signVersion`（前端/直接调用）。
2. **合同激活级联（父驱子）**：`ErpCtContractActivateProcessor.activate` 在合同 NEGOTIATION→ACTIVE 时，若当前版本 `status==FINALIZED` 则调 `contractVersionBiz.signVersion(...)`（级联 FINALIZED→SIGNED）。**signVersion 不写父 Contract.status**（仅版本行）。ActivateProcessor 不重复注入 Version Bean——级联经 IBiz 调用，守卫在 signVersion 内统一。

### 4. 电子签章接入点（独立流）

`signVersion` 既是线下签署确认入口，也是电子签章 FULLY_SIGNED 完成时由 `ErpCtSignatureRequestBizModel` 回调的接入点。电子签章经**独立** `ErpCtSignatureRequest` 实体 + `IErpCtSignatureProvider` SPI，config-gated `erp-ct.e-signature-enabled`（默认 OFF，未启用走线下签署附件上传 + 手动确认 SIGNED）。`signVersion` 自身不调 SPI、不过账。

### 5. 终态与可达性

- 终态 = {SIGNED}（版本签署后归档，不可回退）。线性无分支。
- 初始 = {DRAFT}。从 DRAFT 出发 FINALIZED/SIGNED 均可达。
- 并发版本翻转：乐观锁（version 字段）+ isCurrent 原子翻转。

### 6. 与设计文档一致性

- 版本管理规则交叉引用：本 doc §适用对象：合同 §1「版本管理影响」列 + §2「NEGOTIATION→ACTIVE 版本状态→SIGNED」迁移表行。
- 返利计提明细见 `docs/design/contract/volume-discount.md`。
- 状态码归 `module-contract/model/app-erp-contract.orm.xml`（dict: `erp-ct/version-status`）。

## 适用对象三：返利协议（RebateAgreement）

> 本节由 plan `2026-08-13-1430-3`（M3.19）补章节落地——owner doc 原无 §RebateAgreement 章节（返利语义在 `docs/design/contract/volume-discount.md`）。本节集中建立 `ErpCtRebateAgreement.status` 轴（dict `erp-ct/rebate-agreement-status`）的权威迁移语义与退化轴裁定登记。
>
> 实体级状态机 Bean：`ErpCtRebateAgreementStateMachine`（退化分类 Bean——`transitions()` 空 + 集中化 ACTIVE accrual 只读守卫 `isActive(status)`）。

### 1. 状态定义

| status | 业务含义 | 可达性 |
|--------|----------|--------|
| 草稿（DRAFT） | 协议正起草，等待激活 | 经 CRUD 创建可达（新建 seed，初始态写入 §9.2 选项 c） |
| 生效中（ACTIVE） | 协议生效，可计提返利 | **预留死状态**（零命名动作 writer 可达） |
| 已到期（EXPIRED） | 终态预留：到期 | **预留死状态**（零 writer） |
| 已结算（SETTLED） | 终态预留：结算完成 | **预留死状态**（零 writer） |

dict `erp-ct/rebate-agreement-status` 4 值（`module-contract/model/app-erp-contract.orm.xml:67-72`）。

### 2. 退化轴声明（layer-2 四方对照裁定）

本轴为**退化分类轴**：

- **零命名动作迁移 writer**：全仓无 `setStatus(REBATE_AGREEMENT_STATUS_ACTIVE|EXPIRED|SETTLED)` 生产 writer，无 activate/suspend/expire/terminate/cancel mutation。仅 DRAFT 经 CRUD 创建可达（新建 seed）。
- **ACTIVE/EXPIRED/SETTLED = 预留死状态（intentional reserved）**：dict 含值但命名动作路径下零 writer 可达。Bean `transitions()` 返回**空列表**（零迁移边），`terminalStatuses()` 亦为空（三死状态非真正终态，仅预留语义入口），`isTerminal(status)` 对所有状态返回 false。ACTIVE/EXPIRED/SETTLED 不在 `initialStatuses`/`terminalStatuses`/`transitions` 任一集合。
- **裁定（Decision）**：分类 = `intentional reserved`。dict 值保留（**不删除**——对齐 Contract CANCELLED/NEGOTIATION + hr SUSPENDED 先例：保留优于删除）；owner doc 本节登记。
- **Successor**：返利协议 activate/expire/settle 业务流落地时，开独立 plan 实现命名动作 mutation + 填充 Bean `transitions()` 边；届时三值转为可达并据实纳入对应集合。

### 3. 唯一 live 用途：ACTIVE accrual 只读守卫

返利计提（accrual）在计提前断言 `status==ACTIVE`（否则抛 `ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE`）。该只读分类集中为 Bean 可测元数据 `ErpCtRebateAgreementStateMachine.isActive(status)`，委托点：

- `ErpCtRebateAgreementRunAccrualProcessor.runAccrual`（@BizMutation 入口经 Processor）
- `RebateEngine.accrue`（计提引擎，`validateActive`）

违例仍由调用方抛领域码 `ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE`（错误码对外不变）。`runAccrual` 副作用生成 `ErpCtRebateAccrual` 行（内部累计明细，**非凭证**），保留原位。

### 4. 返利结算过账边界声明（独立轴）

返利结算过账操作**独立** `ErpCtRebateSettlement` 实体的 `settlement-status`（dict `erp-ct/settlement-status` DRAFT→POSTED，M4.65 plan-first 业财过账）：

- `ErpCtRebateSettlementPostSettlementProcessor` 操作 **Settlement 实体**的 `settlement-status`（DRAFT→POSTED），生成 credit-memo 发票保存 `posted=false`（结算凭证独立流）。
- **与 `ErpCtRebateAgreement.status` 轴无关**——RebateAgreement 状态轴零过账副作用（§8 posted 不入轴 + §3 零过载）。

### 5. 与设计文档一致性

- 返利语义/计提明细交叉引用：`docs/design/contract/volume-discount.md` §返利计提明细 / §追溯调整。
- 结算过账独立轴交叉引用：本 doc 未含（归 M4.65 `ErpCtRebateSettlement.status` 轴）。
- 状态码归 `module-contract/model/app-erp-contract.orm.xml`（dict: `erp-ct/rebate-agreement-status`）。

## 适用对象四：返利结算（RebateSettlement）

> 本节由 plan `2026-08-14-2000-1`（M4.65）补章节落地——owner doc 原仅以边界声明散见于 §适用对象三 §4（返利结算过账独立轴声明）。本节集中建立 `ErpCtRebateSettlement.status` 轴（dict `erp-ct/settlement-status`）的权威迁移语义与裁定登记。
>
> 实体级状态机 Bean：`ErpCtRebateSettlementStateMachine`（`module-contract/erp-ct-service/.../statemachine/`，无状态，1 实现边 postSettlement DRAFT→POSTED + 终态 POSTED + 只读 `transitions()` 元数据）。

### 1. 状态定义

| status | 业务含义（等待什么） | 可达性 |
|--------|----------------------|--------|
| 草稿（DRAFT） | 结算单正起草，等待过账 | 经 CRUD 创建可达（新建 seed，初始态写入 §9.2 选项 c；`ErpCtRebateSettlementBizModel.defaultPrepareSave` 仅 seed businessDate，status 由调用方请求体提供） |
| 已过账（POSTED） | 终态：结算已过账，credit-memo 已生成、计提已回写 | 经 `postSettlement` 命名动作可达（DRAFT→POSTED 唯一实现边） |
| 已取消（CANCELLED） | **预留死状态**（intentional reserved） | **零 writer 可达**（见 §2 裁定） |

dict `erp-ct/settlement-status` 3 值（`module-contract/model/app-erp-contract.orm.xml:73-77`）。

### 2. 迁移完整性 + CANCELLED 死状态裁定（layer-2 四方对照 Decision）

```
DRAFT（草稿）─ postSettlement ─→ POSTED（已过账，终态）
```

| 迁移 | 触发入口 | 前置条件（守卫） | 结果 |
|------|----------|------------------|------|
| DRAFT→POSTED | `ErpCtRebateSettlementPostSettlementProcessor.postSettlement`（BizModel `postSettlement` @BizMutation 单行委托） | `status==DRAFT`（Bean `assertCanPostSettlement`） | `status=POSTED` + totalRebateAmount + postedAt/postedBy + creditMemoBillType/BillCode |

**CANCELLED = intentional reserved 死状态（Decision）**：dict 含 CANCELLED 值但全仓**零 `setStatus(CANCELLED)` 生产 writer**（全域零 reverse/unpost/cancel 命名 mutation）。裁定分类 = `intentional reserved`（对齐 Contract CANCELLED / RebateAgreement EXPIRED+SETTLED 先例：保留优于删除）。Bean `transitions()` **不含** CANCELLED 边、`terminalStatuses()` **不含** CANCELLED（非真正终态，仅预留语义入口）、`initialStatuses()` 不含 CANCELLED；dict 值保留不删。**Successor**：PM 要求 settlement cancel 工作流时，开独立 plan 实现 cancel mutation + dict 值激活。

### 3. postSettlement 副作用边界声明

`postSettlement`（DRAFT→POSTED）副作用（**保留在 Processor，Bean 不持有**，§8 分工边界）：

- 汇总关联未结算计提 `findUnsettledAccruals` → 按 `agreement.rebateType` 分支生成 credit-memo **负额发票**（PURCHASE → AP 发票 `posted=false` / SALES → AR 发票 `posted=false`，O-4 架构豁免 `docs/architecture/posting-exemptions.md §ErpCtRebateSettlementBizModel`，非 GL 凭证）；
- `settlement.setCreditMemoBillType/BillCode`；
- `ErpCtRebateAccrual.isSettled=true` + `settledDate=today` 回写；
- `postedAt`/`postedBy` 审计字段写入。

### 4. `posted` 布尔列不对称（watch-only residual，Decision）

ORM `posted` 列（`app-erp-contract.orm.xml:605`，BOOLEAN default false）存在，但 Processor **从不 `setPosted(true)`**——仅写 `postedAt`/`postedBy`；credit-memo 发票自身 `posted=false`（经 pur/sal 管道后续翻转）。裁定分类 = **watch-only residual（登记非修正）**：Bean **不入轴 `posted`**（契约 §3：`posted` 不作 StateMachine 迁移轴，过账/红冲契约归过账侧）。Successor：仅当 PM 要求 settlement 独立 posted 翻转语义时重评。

### 5. 终态与可达性

- 终态 = {POSTED}（结算过账后归档，不可回退；非法 re-post 经 Bean `assertCanPostSettlement` 拦截 + 领域码 `ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION` 仅传 settlementId + currentStatus）。
- 初始 = {DRAFT}。从 DRAFT 出发仅 POSTED 可达；CANCELLED 命名动作路径下零 writer 可达（死状态，§2 裁定）。
- 幂等：POSTED 重复 postSettlement 经 `assertCanPostSettlement` 拒绝（层 3 `TestErpCtContractRebate.testSettlementIllegalTransition` 回归）。

### 6. 与设计文档一致性

- 返利结算流程交叉引用：`docs/design/contract/volume-discount.md` §结算流程。
- 返利协议轴边界交叉引用：本 doc §适用对象三 §4（返利结算过账独立轴声明，M4.65 与本对象互为权威）。
- 状态码归 `module-contract/model/app-erp-contract.orm.xml`（dict: `erp-ct/settlement-status`）；错误码 `ErpCtErrors.ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION`（`erp.err.ct.settlement-illegal-transition`，参数 settlementId + currentStatus，action/expectedStatus 仅存于 common 码 cause）。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 版本变更时新旧版本 isCurrent 切换是否原子。
- SUSPENDED 状态下开票/消耗拦截是否落实。
- 提前终止的法务审批权限。
- 到期提醒的 nop-job 定时任务配置。
- contractType 与 contractDirection 的组合校验（采购合同→INBOUND，销售合同→OUTBOUND）。
- 合同版本（适用对象二）：signVersion 的 isCurrent 守卫 + FINALIZED 来源态；Contract 激活→版本 SIGNED 级联（父驱子，守卫统一在 signVersion）；amend 新建 DRAFT 不调 assertCan*。
- 合同头（适用对象一）：submit（DRAFT→NEGOTIATION，零版本建 v1 / 已有版本保留 current）+ rejectAmend（DRAFT→ACTIVE，SIGNED 优先恢复）两 RC-R1.32 边；activate 前置 finalizeVersion 契约（D6 watch-only）。
- 返利协议（适用对象三）：ACTIVE/EXPIRED/SETTLED 预留死状态裁定（intentional reserved，不删除 dict 值）；ACTIVE accrual 只读守卫委托 Bean；返利结算过账在独立 `ErpCtRebateSettlement.status` 轴（M4.65），非 RebateAgreement.status。
- 返利结算（适用对象四）：postSettlement DRAFT→POSTED 唯一实现边（Bean `assertCanPostSettlement`）；CANCELLED 预留死状态裁定（intentional reserved，零 writer，dict 值保留）；`posted` 布尔列不对称 watch-only residual（不入轴）；credit-memo 生成 + accrual 回写副作用保留在 Processor。
