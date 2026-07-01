# Portal 身份与访问模型（future extension）

> **STATUS: future extension placeholder（非当前基线，ask-first）**
>
> 本文是 portal 外部主体身份模型的方向性骨架，**不立即改 `orm.xml`**。`ErpMdPartner.userId` 字段的新增、`data-auth.xml` 规则的编写均属 `model/*.orm.xml` / 受保护区域变更，**实施前必须 plan-first + 人工批准**（见 `ai-autonomy-policy.md`）。本文仅定义设计意图，供未来 portal 启用时作为起点。

## 目的

定义 portal 外部主体（客户/供应商）的身份、认证、授权模型，与内部 ERP 角色体系（见 `roles-and-permissions.md`）分离。

## 外部角色

| 角色 | 主体类型 | 绑定 | 说明 |
|------|----------|------|------|
| `portal-customer` | 客户（`ErpMdPartner` 中 customer 角色） | `NopAuthUser`（portal 角色）+ `ErpMdPartner.userId` 绑定 | 外部客户账号，只读为主 |
| `portal-supplier` | 供应商（`ErpMdPartner` 中 supplier 角色） | `NopAuthUser`（portal 角色）+ `ErpMdPartner.userId` 绑定 | 外部供应商账号，读写结合 |

> **模型选择**：采用 Odoo 式"业务主体绑定内部账号"——外部账号 = `NopAuthUser`（带 portal 角色），业务主体 = `ErpMdPartner`（新增 `to-one userId → NopAuthUser`，ask-first）。不引入独立 portal 用户表，复用 nop-auth 账号体系。

## partner.userId 绑定模式（ask-first，未落地）

```
ErpMdPartner（业务主体）
   └─ userId  →  NopAuthUser.id  （to-one，新增字段，需 plan-first）
                     └─ roles: [portal-customer | portal-supplier]
```

- 一个 `ErpMdPartner` 可绑定一个 `NopAuthUser`（1:1）。
- 同一 partner 既是客户又是供应商时，绑定同一账号，角色叠加。
- **未落地**：`ErpMdPartner.userId` 字段当前不在 `module-master-data/model/app-erp-master-data.orm.xml`，新增需走保护区域流程。

## 行级数据隔离骨架（data-auth，软过滤）

项目**不预置 tenantId**（见 `system-baseline.md` 多租户策略），portal 数据隔离用 `data-auth.xml` 行级软过滤：

| 角色 | data-auth 规则（示意） | 效果 |
|------|------------------------|------|
| `portal-customer` | 过滤 `partnerId = ${currentUser.partnerId}` 且 partner 角色为 customer | 只看自己的订单/发票/工单 |
| `portal-supplier` | 过滤 `partnerId = ${currentUser.partnerId}` 且 partner 角色为 supplier | 只看自己的 PO/ASN/对账 |

> data-auth 独立于 `nop.auth.enable-action-auth` 开关，始终附加到查询条件（见 `roles-and-permissions.md §设计能力基线`）。

## 最小动作集（future）

> 以下动作集是 portal 启用时的最小可行集，对应 `*.action-auth.xml` 的 FNPT 资源点。当前不落地。

### 客户门户（portal-customer）

| 动作 | I*Biz | 权限 |
|------|-------|------|
| 查询自己的订单 | `IErpSalOrderBiz`（read-only，data-auth 过滤） | 默认允许 |
| 查询/下载自己的发票 | `IErpSalInvoiceBiz` / `IErpFinVoucherBiz` | 默认允许 |
| 提交售后工单 | `IErpCsTicketBiz` | 默认允许 |
| 确认/拒绝报价单 | `IErpSalQuotationBiz` | 默认允许 |
| 在线付款 | `ErpFinPayment`（**future，受保护区域，需人工批准**） | 默认拒绝 |

### 供应商门户（portal-supplier）

| 动作 | I*Biz | 权限 |
|------|-------|------|
| 查询/确认 PO | `IErpPurOrderBiz` | 默认允许 |
| 提交 ASN | `IErpB2bAsnBiz` | 默认允许 |
| 提交发票 | `IErpPurInvoiceBiz` | 默认允许 |
| 查看绩效评分 | `IErpPurSupplierScorecardBiz`（read-only） | 默认允许 |

## 与内部角色体系的关系

- 内部 ERP 角色见 `roles-and-permissions.md`（本文不重复）。
- portal 角色是**外部主体**，与内部角色（采购员/销售员/财务员等）正交，不混用。
- portal 用户不进入内部 ERP 菜单（通过 `action-auth.xml` 的角色绑定隔离菜单可见性）。

## 落地前置条件（ask-first 清单）

- [ ] `ErpMdPartner.userId` 字段新增（`module-master-data/model/*.orm.xml` 保护区域，需 plan-first + 人工批准）
- [ ] `portal-customer` / `portal-supplier` 角色与 `data-auth.xml` 规则编写
- [ ] SSO/OAuth2 配置（受保护区域，需人工批准）
- [ ] 在线付款集成（受保护区域，需人工批准）
- [ ] portal 前端工程立项（当前无 `module-portal`）
