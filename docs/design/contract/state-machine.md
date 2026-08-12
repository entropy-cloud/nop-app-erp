# 合同全生命周期管理域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 合同域主状态对象：**合同（Contract）**。版本管理（ContractVersion）作为辅助状态对象，与合同状态联动变更。

## 适用对象：合同（Contract）

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

> **迁移图实现漂移注记（plan 2026-08-12-1118-1 层 2 四方对照裁定）**：上图声明 9 条迁移边，其中两条**命名动作 writer 未落地**，`ErpCtContractStateMachine` Bean 据实不编码：
> - **DRAFT→NEGOTIATION（提交谈判）**：全域零 `setStatus(NEGOTIATION)` writer（无 `submitForNegotiation` 命名动作），NEGOTIATION 仅作为 activate/terminate 的源态被消费，命名动作路径下从 DRAFT 不可达（仅经 CRUD `save` 可写，M0.1 §9.4 残留风险）。**Successor**：合同提交谈判业务流落地时开独立 plan。
> - **DRAFT→CANCELLED（草稿废弃）**：dict 缺 CANCELLED 值 + 零 writer（详见 §1 实现漂移注记）。
>
> Bean 仅编码**已实现**的 7 条边（activate/suspend/resume/terminate[多源]/expire/amend）。已实现边与本图一致，无 §迁移表 vs §实现约定 的内部语义漂移。

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

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 版本变更时新旧版本 isCurrent 切换是否原子。
- SUSPENDED 状态下开票/消耗拦截是否落实。
- 提前终止的法务审批权限。
- 到期提醒的 nop-job 定时任务配置。
- contractType 与 contractDirection 的组合校验（采购合同→INBOUND，销售合同→OUTBOUND）。
