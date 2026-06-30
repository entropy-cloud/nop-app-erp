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
