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

> L-5（plan 2026-07-20-2200-1）补：CANCELLED 态在 §2 迁移图中早已使用（`DRAFT → CANCELLED` 草稿废弃路径），但 §1 定义表遗漏。CANCELLED 与 TERMINATED 的区别：CANCELLED 仅限 DRAFT 阶段的草稿废弃（未生效，无版本归档，无业务回写）；TERMINATED 是已生效合同（ACTIVE/NEGOTIATION）的提前终止（已生效，需归档版本与关联终止协议）。

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

> **电子签章接入点**（plan 2026-07-04-2200-2）：`IErpCtContractVersionBiz.signVersion`（FINALIZED→SIGNED + isCurrent 翻转）
> 既是线下签署的确认入口，也是电子签章 FULLY_SIGNED 完成时由 `ErpCtSignatureRequestBizModel.completeFullySigned`
> 自动调用的接入点（retrieveCertificate 后）。config-gated `erp-ct.e-signature-enabled`（默认关，未启用走线下签署附件上传 + 手动确认 SIGNED）。
| ACTIVE→EXPIRED | 系统自动 | endDate < now() | 归档版本，不可再修改 |
| ACTIVE→TERMINATED | 合同管理员 | 终止协议签署，填写终止原因 | 版本归档，关联终止协议 |
| ACTIVE→SUSPENDED | 合同管理员 | 双方确认中止，填写中止原因 | 版本冻结 |
| SUSPENDED→ACTIVE | 合同管理员 | 中止状态解除，双方确认恢复 | 版本恢复生效 |
| NEGOTIATION→TERMINATED | 合同管理员 | 谈判破裂，双方确认终止 | 版本归档 |

### 3. 终态与恢复

- 终态：`已作废（CANCELLED）`、`已到期（EXPIRED）`、`已终止（TERMINATED）`。
- 终态不可恢复。若需续签，从 EXPIRED 创建续期合同（parentContractId 关联原合同）。
- SUSPENDED 不是终态，可恢复为 ACTIVE。
- CANCELLED 仅限 DRAFT 阶段草稿废弃；已进入 NEGOTIATION 或后续态的合同不可作废，只能 TERMINATED。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| ACTIVE 期间发现条款缺陷 | 创建变更单（Amendment），新建 DRAFT 子合同 → NEGOTIATION → 生效后替换原版本 |
| endDate 到达但合同仍在执行中 | 先标记 EXPIRED，同时自动创建续期草稿（auto-create-renewal-draft 配置） |
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
| 合同到期提醒 | nop-job 定时扫描 endDate，到期前 30/15/7 天通知经办人 |

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
7. endDate 到达 → 系统自动标记 EXPIRED。
8. 系统按配置 auto-create-renewal-draft=true → 自动创建续期草稿（parentContractId 关联原合同）。

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
