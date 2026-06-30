# 客服工单域 — 客户服务权益（Entitlement）

## 目的

管理客户的服务权益与保修期限：绑定支持合同 → 确定 SLA 策略 → 跟踪保修有效期 → 超期预警。确保服务交付与客户购买的服务级别一致。

---

## 一、模型设计

### 1.1 ErpCsEntitlement（服务权益）

| 字段 | 含义 | 备注 |
|------|------|------|
| id/code/orgId | 标准 | |
| partnerId | 客户（→ErpMdPartner） | 权益归属客户 |
| contractId | 支持合同（→ErpContContract） | 可选：关联正式合同 |
| slaPolicyId | SLA 策略（→ErpCsSlaPolicy） | 该权益适用的 SLA |
| serviceType | 服务类型（dict） | WARRANTY/SUPPORT_CONTRACT/PAY_PER_TICKET |
| startDate | 生效日期 | |
| endDate | 失效日期 | |
| maxTickets | 最大工单数（可选） | 按次计费场景上限 |
| usedTickets | 已用工单数 | 每次创建工单 +1 |
| maxResponseTime | 承诺响应时限（分钟）可选 | 覆盖 SLA 策略的响应时间 |
| maxResolutionTime | 承诺解决时限（分钟）可选 | 覆盖 SLA 策略的解决时间 |
| isActive | 是否启用 | |
| notes | 备注 | large=true |
| 标准审计字段 | | |

### 1.2 serviceType 字典

| 值 | 说明 | 典型场景 |
|----|------|----------|
| WARRANTY | 保修期权益 | 设备/产品保修期内免费支持 |
| SUPPORT_CONTRACT | 付费支持合同 | 年度维保合同，含 N 次服务 |
| PAY_PER_TICKET | 按次计费 | 超出保修期的单次付费服务 |

### 1.3 ErpCsContract（支持合同，轻量级）

不依赖 contract 域时使用本实体独立管理；已有正式合同（contract 域）时通过 `contractId` 弱指针关联。

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| partnerId | 签约客户 |
| contractType | dict：ANNUAL/QUARTERLY/ONE_TIME |
| startDate | 合同生效日期 |
| endDate | 合同到期日期 |
| totalAmount | 合同总金额 |
| billingCycle | 计费周期（MONTHLY/QUARTERLY/YEARLY） |
| status | dict：DRAFT/ACTIVE/EXPIRED/CANCELLED |
| attachmentId | 合同文件附件 |

---

## 二、核心流程

### 2.1 工单创建时的权益匹配

```
创建工单（customerId, ticketTypeId, priority）
        │
        ├─► 查询客户的有效权益
        │       SELECT * FROM ErpCsEntitlement
        │       WHERE partnerId = customerId
        │         AND isActive = true
        │         AND startDate <= now()
        │         AND endDate >= now()
        │         AND (maxTickets IS NULL OR usedTickets < maxTickets)
        │
        ├─► 取有效期最近的权益
        │
        ├─► 写入工单.slaPolicyId = entitlement.slaPolicyId
        │
        ├─► 更新 entitlement.usedTickets = usedTickets + 1
        │
        └─► 无有效权益：
                ├─► 仍允许创建工单（可选配置）
                └─► 标记为"无服务权益"，客服手动处理
```

### 2.2 权益到期与续约

```
到期前 30 天
        │
        ├─► nop-job 每日扫描：
        │       SELECT * FROM ErpCsEntitlement
        │       WHERE endDate BETWEEN now() AND now() + 30d
        │         AND isActive = true
        │
        ├─► 创建提醒通知（通知销售/客服主管）
        │
        └─► 客服联系客户续约 → 创建新权益或延长 endDate

到期日
        │
        ├─► isActive = false（自动停用）
        │
        └─► 关联的工单仍可处理但不再享受 SLA 保障
```

### 2.3 按次计费权益扣减

```
PAY_PER_TICKET 类型
        │
        ├─► 每次创建工单 → usedTickets + 1
        │
        ├─► usedTickets >= maxTickets → 拒绝创建新工单
        │
        ├─► 客服可手动创建"超限工单"（不计权益，按次付费后结算）
        │
        └─► 工单 CLOSED 后 → 如需退款 → usedTickets - 1（管理员操作）
```

### 2.4 保修权益

```
WARRANTY 类型（与产品/批次相关）
        │
        ├─► 关联产品（productId→ErpMdMaterial）或批次（batchNo）
        │
        ├─► 保修期 = 产品出库日期 + warrantyPeriod（天）
        │
        ├─► ErpCsEntitlement 可在销售出库时自动创建
        │
        └─► 工单创建时根据 productId/batchNo 自动匹配保修权益
```

---

## 三、SLA 策略覆盖规则

工单 SLA 优先级（从高到低）：

| 优先级 | 来源 | 说明 |
|--------|------|------|
| 1 | entitlement.maxResolutionTime（不为空时） | 权益级覆盖 |
| 2 | entitlement.slaPolicyId → resolveHours | 权益关联的 SLA 策略 |
| 3 | ticketType.defaultSlaPolicyId | 工单类型默认 SLA |
| 4 | 无 SLA | 不启用 SLA 计时 |

---

## 四、权益报表

| 报表 | 内容 | 用途 |
|------|------|------|
| 权益到期清单 | 未来 30/60/90 天到期的权益列表 | 续约管理 |
| 权益使用率 | usedTickets / maxTickets 按客户 | 评估合同使用情况 |
| 无权益工单 | 创建时无有效权益的工单列表 | 识别流失服务机会 |
| 保修到期预警 | 即将到期的保修权益，按产品分组 | 主动通知客户延保 |

### 查询示例

```
-- 权益到期预警（30 天内）
SELECT
    e.code,
    e.endDate,
    p.name AS partnerName,
    e.usedTickets,
    e.maxTickets
FROM ErpCsEntitlement e
JOIN ErpMdPartner p ON p.id = e.partnerId
WHERE e.endDate BETWEEN now() AND now() + 30
  AND e.isActive = true
ORDER BY e.endDate ASC

-- 权益使用率
SELECT
    p.name,
    count(e.id) AS totalEntitlements,
    sum(e.usedTickets) AS totalUsed,
    sum(e.maxTickets) AS totalMax,
    round(
        sum(e.usedTickets)::decimal / nullif(sum(e.maxTickets), 0) * 100, 2
    ) AS usageRate
FROM ErpCsEntitlement e
JOIN ErpMdPartner p ON p.id = e.partnerId
WHERE e.serviceType = 'PAY_PER_TICKET'
GROUP BY p.id, p.name
ORDER BY usageRate DESC
```

---

## 五、配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-cs.entitlement-check-enabled` | true | 创建工单时是否校验权益 |
| `erp-cs.entitlement-allow-no-entitlement` | true | 无权益时是否允许创建工单 |
| `erp-cs.entitlement-expiry-warning-days` | 30 | 到期预警提前天数 |
| `erp-cs.entitlement-auto-warranty` | false | 销售出库是否自动创建保修权益 |

---

## 六、证据与参考

| 证据 | 强度 | 说明 |
|------|------|------|
| Odoo helpdesk entitlement | 🟢 | `helpdesk.ticket` → `partner_id` + `sla_id` 关联合同 |
| ServiceNow contract management | 🟢 | Contract 关联 SLA + 权益有效期 + 到期预警 |
| ServiceNow warranty management | 🟡 | Warranty 字段 + 到期自动停用 |

## 七、跨域协作

| 对端 | 协作方式 |
|------|---------|
| master-data（ErpMdPartner） | 客户/联系人主数据 |
| contract（ErpContContract） | 合同弱指针关联 |
| sales（出库单） | 出库审核时触发保修权益自动创建（可选） |
| inventory（批次管理） | 保修期按出库日期 + 产品保修周期自动计算 |
