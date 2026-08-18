# 客服工单域 — 服务目录（Service Catalog）

## 目的

定义标准化服务请求类型，为每个服务项绑定工单类型、必填字段、SLA 策略、审批流程和履行工作流。实现服务请求的标准化、自动化和可度量。

---

## 一、模型设计

### 1.1 ErpCsServiceCatalogItem（服务目录项）

| 字段 | 含义 | 备注 |
|------|------|------|
| id/code/name/orgId | 标准 | |
| categoryId | 目录分类（→ErpCsCatalogCategory） | |
| parentId | 父项（自引用，支持子项） | |
| shortDescription | 简短描述（列表展示） | |
| fullDescription | 详细说明 | large=true |
| ticketTypeId | 关联工单类型（→ErpCsTicketType） | 创建工单时自动填充 |
| slaPolicyId | 默认 SLA 策略（→ErpCsSlaPolicy） | 覆盖工单类型的默认 SLA |
| fulfillmentProcessId | 履行流程标识 | see §3 |
| requestFormConfig | 请求表单配置（JSON） | see §1.4 |
| isActive | 是否上架 | |
| isPublic | 是否客户可见（false=仅客服可见） | |
| sequence | 排序 | |
| estimatedResolution | 预计解决时间描述 | 如"2 个工作日内" |
| 标准审计字段 | | |

### 1.2 ErpCsCatalogCategory（目录分类）

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| parentId | 父分类（树形） |
| icon | 分类图标 |
| sequence | 排序 |
| isActive | 是否启用 |

**预设分类**：

```
服务目录
├── 技术支持
│   ├── 账户与登录
│   ├── 系统故障
│   ├── 功能咨询
│   └── API 集成
├── 业务支持
│   ├── 订单问题
│   ├── 发票与付款
│   ├── 退货与退款
│   └── 合同变更
├── 现场服务
│   ├── 设备安装
│   ├── 巡检维护
│   └── 紧急维修
└── 内部服务
    ├── 权限申请
    ├── 数据导出
    └── 报表定制
```

### 1.3 ErpCsCatalogFulfillment（目录项履行映射）

| 字段 | 含义 | 备注 |
|------|------|------|
| id/code/orgId | 标准 | |
| catalogItemId | 目录项（→ErpCsServiceCatalogItem） | |
| sequence | 执行顺序 | |
| actionType | 动作类型（dict） | see §3.1 |
| actionConfig | 动作配置（JSON） | see §3.2 |
| assignToRole | 执行角色 | 如 TECHNICIAN/APPROVER |
| estimatedDuration | 预估时长（分钟） | |
| isMandatory | 是否必须执行 | |

### 1.4 requestFormConfig 配置格式

```json
{
  "fields": [
    {
      "key": "subject",
      "label": "请求主题",
      "type": "text",
      "required": true
    },
    {
      "key": "description",
      "label": "问题描述",
      "type": "textarea",
      "required": true
    },
    {
      "key": "productId",
      "label": "关联产品",
      "type": "ref",
      "refEntity": "ErpMdMaterial",
      "required": false
    },
    {
      "key": "orderNumber",
      "label": "订单号",
      "type": "text",
      "required": false
    },
    {
      "key": "urgency",
      "label": "紧急程度",
      "type": "select",
      "options": ["low", "normal", "high", "urgent"],
      "default": "normal",
      "required": true
    },
    {
      "key": "attachment",
      "label": "附件",
      "type": "file",
      "multiple": true,
      "maxSize": 10485760,
      "required": false
    }
  ],
  "sections": [
    {"key": "basic", "label": "基本信息", "fields": ["subject", "description"]},
    {"key": "details", "label": "详细信息", "fields": ["productId", "orderNumber"]},
    {"key": "urgency", "label": "优先级", "fields": ["urgency"]},
    {"key": "files", "label": "上传附件", "fields": ["attachment"]}
  ]
}
```

---

## 二、服务目录使用流程

### 2.1 门户自助提交

```
客户登录自助门户
        │
        ├─► 浏览服务目录（按分类树）
        │       ├─► 仅显示 isPublic=true 的项
        │       └─► 未登录可浏览但不提交
        │
        ├─► 选择目录项 → 加载 requestFormConfig
        │
        ├─► 填写表单 → 提交
        │
        ├─► 系统创建 ErpCsTicket
        │       ├─ ticketTypeId = catalogItem.ticketTypeId
        │       ├─ slaPolicyId = catalogItem.slaPolicyId
        │       ├─ priority = 表单填写的 urgency
        │       └─ catalogItemId = selected item
        │
        └─► 按 fulfillment 流程执行
```

### 2.2 客服代提交

```
客服创建工单
        │
        ├─► 点击"选择服务目录项"
        │
        ├─► 加载全部目录项（isPublic=true + isPublic=false）
        │
        ├─► 选择项 → 自动填充 ticketType/slaPolicy/表单字段
        │
        ├─► 填写剩余信息 → 提交
        │
        └─► 正常走工单流程
```

---

## 三、履行流程映射

### 3.1 actionType 字典

| 动作类型 | 说明 | 示例 |
|----------|------|------|
| CREATE_TICKET | 创建工单（入口） | 所有目录项的起点 |
| ASSIGN_TEAM | 分配处理团队 | 按 serviceType 匹配技术组 |
| ASSIGN_AGENT | 分配处理人 | 按技能/忙闲匹配 |
| REQUEST_APPROVAL | 审批流程 | 权限申请 → 主管审批 |
| NOTIFY_CUSTOMER | 通知客户 | 提交确认/完成通知 |
| UPDATE_STATUS | 更新工单状态 | 自动标记 IN_PROGRESS |
| CREATE_CHILD_TICKET | 创建子工单 | 跨团队协作场景 |
| INVOKE_WORKFLOW | 触发外部工作流 | 调用 maintenance/manufacturing |
| CLOSE_TICKET | 自动关闭 | 简单请求完成即关闭 |

### 3.2 actionConfig 配置示例

```json
// ASSIGN_TEAM
{
  "teamId": "team_tech_support",
  "mode": "ROUND_ROBIN"
}

// REQUEST_APPROVAL
{
  "approvalChain": ["supervisor", "manager"],
  "timeout": 24,
  "onTimeout": "AUTO_APPROVE"
}

// CREATE_CHILD_TICKET
{
  "childTicketTypeId": "ticket_field_service",
  "assignToTeamId": "team_field_support"
}
```

### 3.3 履行流程示例

```
目录项："设备现场维修"
        │
Step 1: CREATE_TICKET ──────────────────────────→ 创建主工单，状态 NEW
Step 2: ASSIGN_TEAM(team=field_tech) ───────────→ 分配到现场技术组
Step 3: ASSIGN_AGENT(mode=SKILL_MATCH) ─────────→ 按技能匹配处理人
Step 4: CREATE_CHILD_TICKET(ticketType=dispatch) → 创建派工子工单，走现场服务流程
Step 5: NOTIFY_CUSTOMER ────────────────────────→ 通知客户预计到达时间
Step 6: UPDATE_STATUS(IN_PROGRESS) ──────────────→ 主工单进入 IN_PROGRESS
...
Step N: UPDATE_STATUS(RESOLVED) ────────────────→ 现场完成，工单解决
Step N: CLOSE_TICKET ────────────────────────────→ 客户确认后关闭
```

---

## 四、目录管理

### 4.1 管理功能

| 功能 | 说明 |
|------|------|
| 目录项 CRUD | 上架/下架、编辑表单配置 |
| 分类管理 | 树形分类维护（最大深度 3 级） |
| 履行流程编排 | 拖拽编排 actionType 序列 |
| 表单设计 | requestFormConfig JSON 编辑器（支持预览） |
| 权限配置 | 按角色控制目录项可见性 |

### 4.2 目录版本

服务目录项支持版本管理（重大表单变更时）：

```
v1（当前上架版本）→ 新工单使用 v1 表单
v2（草稿/待审批） → 审批通过后替换 v1
已创建工单使用创建时的版本（不回滚）
```

---

## 五、报表

| 报表 | 内容 | 用途 |
|------|------|------|
| 目录使用排行 | 各目录项的工单数 Top N | 识别热门服务/低频服务 |
| 目录项 SLA 达标率 | 按目录项分组 SLA 达标率 | 评估各服务项交付质量 |
| 履行流程耗时 | 各 actionType 平均耗时 | 流程瓶颈分析 |
| 自助服务率 | 门户提交 / 总工单数 | 度量自助服务覆盖面 |

---

## 六、配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-cs.service-catalog-enabled` | true | 是否启用服务目录 |
| `erp-cs.service-catalog-self-service` | true | 是否允许客户自助提交 |
| `erp-cs.catalog-category-max-depth` | 3 | 分类最大深度 |
| `erp-cs.fulfillment-retry-cron` | （空） | 履行链自动重试 job cron（空=不调度，见 §9.1） |
| `erp-cs.fulfillment-retry-max` | 3 | 履行步骤重试次数上限（超出后通知管理员人工介入） |
| `erp-cs.fulfillment-approval-timeout-hours` | 24 | REQUEST_APPROVAL 超时自动审批兜底小时数（actionConfig.timeoutHours 优先） |

---

## 七、证据与参考

| 证据 | 强度 | 说明 |
|------|------|------|
| ServiceNow service catalog | 🟢 | Catalog item → 表单 → 履行流程 → 审批绑定 |
| Odoo helpdesk service levels | 🟡 | ticket type + SLA 策略绑定 |
| Jira Service Management | 🟡 | Request type → form → workflow mapping |

## 八、跨域协作

| 对端 | 协作方式 |
|------|---------|
| 工单（ErpCsTicket） | 目录项驱动工单创建与属性填充 |
| maintenance（Request） | 现场维修目录项跨域创建维护请求 |
| master-data（ErpMdOrganization） | 服务目录按组织隔离 |

## 九、实现注记

> 本节为表单字段映射与履行编排引擎的稳定实现注记，非迁移历史。

### 9.1 履行编排引擎（RC-R1.71 已实现）

> **当前基线范围**：目录履行多步编排已实化（RC-R1.71；原「多步履行产品基线外」声明作废——arm-index P1-RC-061 Q4 裁决三判据不成立，2026-08-12 A 类批量裁决授权 ORM）。执行载体 = `ErpCsTicketFulfillmentStep`（per-ticket per-step 执行行，UK(ticketId, fulfillmentId) 幂等物化；status/retryCount/lastError 承载 UC-CS-12 后置「履行流程状态可跟踪，异常可重试」）。

**执行模型**（`ErpCsCatalogFulfillment__executeFulfillmentSteps`）：按目录项加载模板 → 物化执行行（存在即复用，首建写模板快照）→ sequence 升序推进：

| actionType | actionConfig 契约（§3.2 示例的落地收窄） | 语义 |
|---|---|---|
| CREATE_TICKET | —（不消费） | DONE 审计（主单已由 createFromCatalog 创建） |
| ASSIGN_TEAM / ASSIGN_AGENT | `{mode: ROUND_ROBIN\|LEAST_OPEN}`（缺省 `erp-cs.assign-method`） | R1.65 `TicketAssignResolver` 真实分配（挂载策略 team → 类型默认策略 team → 同码 crm 团队成员池 + 纯函数挑人）；ticket 为 NEW 时 NEW→ASSIGNED 迁移 + ASSIGN 审计，非 NEW 幂等仅更新 assignedToId；候选池空 → FAILED |
| REQUEST_APPROVAL | `{approverRole?, timeoutHours?}` | cs-local 轻量审批（nop-workflow 集成 successor）：step IN_PROGRESS + notify 审批人（ROLE approverRole 缺省客服主管）；`approveFulfillmentStep(stepId, approved, comment)` 审批——驳回 → step FAILED + retryCount 置 max（人工终局，阻断自动重试）；超时自动审批（actionConfig.timeoutHours 覆盖 > config 兜底）→ DONE + 审计 |
| CREATE_CHILD_TICKET | —（弱指针承载） | 经 `IErpCsTicketBiz.save` 真实子单：subject=`[子工单] `前缀、同 customerId/ticketTypeId、remark=`parentTicketCode={code}`、code 走 TK codeRule；父单写 TicketAction（content 含子单编号）——双向弱指针，无 ORM 亲子列 |
| NOTIFY_CUSTOMER | — | notify 派发 `cs.fulfillment-notify-customer`（模板种子 7207，ROLE 客服员转达 IN_APP 占位，镜像 7205 范式） |
| UPDATE_STATUS | `{status}`（必填） | 状态机迁移矩阵守卫真实 setStatus（非法迁移/缺配置 → step FAILED + lastError）；target == 当前状态幂等 DONE no-op |
| INVOKE_WORKFLOW | — | SKIPPED（L1 UC-CS-12 ② 未枚举值；cs 域工作流引擎集成 successor——arm-index P1-RC-061 done 注记登记残留防重开） |
| CLOSE_TICKET | — | 审计 DONE 占位（L1 未枚举值，同上 done 注记边界） |

**失败暂停（UC-CS-12 ③）**：步骤失败 → step FAILED + lastError + 中断链（后续保持 PENDING）+ 管理员通知（`cs.fulfillment-step-failed` 模板种子 7206，ROLE 客服主管）。

**终态推进（UC-CS-12 ④）**：链推进执行**最后一个步骤之前** `ensureInProgress` 铺底（NEW 无处理人 → 自动指派当前操作员 → assign 边 → ASSIGNED → start 边 → IN_PROGRESS；ASSIGNED → start；≥IN_PROGRESS/终态幂等跳过）——全部完成后工单 IN_PROGRESS；「或按配置 RESOLVED」经尾部 `UPDATE_STATUS(status=RESOLVED)` 步骤组合达成（末步前铺底 IN_PROGRESS → resolve 边 RESOLVED，零目录级终态配置列）。

**重试（后置 + 异常）**：双入口——手动 `retryFulfillment(ticketId)`（仅 FAILED 步骤；retryCount+1 后**刷新读取模板 actionConfig**（修正配置即生效）再重执行；`retryCount >= erp-cs.fulfillment-retry-max` 拒绝 + 管理员通知人工介入，超限通知经 REQUIRES_NEW 独立事务提交以避免随拒绝异常回滚）+ 自动 `erp-cs-fulfillment-retry` job（cron 空值跳过；REQUEST_APPROVAL 超时自动审批 + FAILED 未超限工单逐张重试，单张失败隔离；审批驳回终局行 retryCount=max 天然排除）。状态跟踪查询 `findFulfillmentProgress(ticketId)`（工单列表「履行进度」drawer 最小展示 + 「重试履行」行动作）。

**证据**：`TestErpCsCatalogFulfillmentEngine` 16 @Test（①-⑩ 引擎路径 + ⑪-⑯ 重试/超时/查询/RPC 冒烟）；`TestErpCsServiceCatalog` 履行链弱断言已改造为步骤执行行强断言。

### 9.2 表单字段映射（requestFormConfig → ErpCsTicket）

`createFromCatalog(catalogItemId, formData, ctx)` 中 formData 字段映射口径：

| formData 字段 | 工单字段 | 说明 |
|---------------|---------|------|
| subject | subject | 缺省回退目录项名 |
| description | description | — |
| customerId | customerId | 必填（权益匹配依据） |
| contactId / productId / orderNumber | 同名 | — |
| urgency | priority | urgency 表单字段映射到工单 priority；缺省回退 NORMAL |
| source | source | — |

目录项默认值（ticketTypeId/slaPolicyId/catalogItemId）先填，formData 覆盖之；权益级 slaPolicyId 覆盖优先（见 `entitlement.md §8.1`）。

### 9.3 门户自助 / isPublic 鉴权范围收窄

`isPublic=false` 时本期不引入角色鉴权（归前端 successor），默认允许提交；门户自助前端建立后再加 isPublic + 客户角色校验。

### 9.4 服务端必填校验（RC-R1.28 / P1-RC-060 实现注记）

`createFromCatalog` 按 §1.4 `requestFormConfig.fields[].required==true` 做服务端必填校验（L1 UC-CS-10 异常①「表单必填项缺失 → 禁止提交」）：

- **校验时机**：`ErpCsServiceCatalogItemCreateFromCatalogProcessor.validateRequiredFormFields`（protected step）在 `requireCatalogItem`/`validateCatalogItemUsable` 之后、`buildTicketData` 之前接线——校验失败时工单不落库、权益不扣减（副作用之前拒绝）。
- **校验基准**：formData **原始键值（fallback 前）**——schema 标必填而客户未提交 → 拒绝，即使服务端有 subject/priority 缺省回退（fallback 是服务端兜底非客户提供）；`urgency→priority` 映射按 formData 原始键 `urgency` 校验。
- **错误码**：`ERR_CATALOG_FORM_REQUIRED_MISSING`（`erp.err.cs.catalog-item.form-required-missing`），参数含 catalogItemId + fieldKey（label 若可得）。
- **容错语义**：`requestFormConfig` 为 null/空白/非法 JSON/fields 非数组 → 跳过校验（LOG.warn，配置数据质量问题不阻断建单）；单个字段 `required` 非布尔 true → 该字段不视为必填；空串/空白值视为缺失。
- **数据治理声明**：校验仅在 requestFormConfig 声明 required 时生效——生产/种子 catalogItem 须维护含 `required` 的 schema 才能实际拦截（当前种子数据未维护，见 A4.2.142 §(f) 条件；随门户自助前端上线/目录数据治理批次执行）。
- **schema key 覆盖约束**：校验的 schema keys 须落在 §9.2 映射集（subject/description/customerId/contactId/productId/orderNumber/urgency/source）内——映射集之外的 key 即使 formData 有值也会在落库时被静默丢弃，数据治理责任在 schema 维护方。
- **测试证据**：`TestErpCsServiceCatalog` 必填校验组 6 用例全绿（缺失拒绝+工单零落库+权益零扣减 / 必填满足放行含 urgency→priority / null 配置跳过 / 非法 JSON 跳过 / 空白视为缺失 / 非必填缺失放行）。
