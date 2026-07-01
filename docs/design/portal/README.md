# 客户门户与供应商门户（Portal）

> **STATUS: future extension placeholder（非当前产品基线）**
>
> Portal **不在当前产品基线**（`product-scope.md` 18 域正式范围不含 portal）。本文是方向性设计资产，保留供未来启用时参考。
> - 实施前需 **plan-first + 人工批准**（portal 跨入 `ai-autonomy-policy.md` 受保护区域：支付、认证、外部集成）。
> - 下文涉及的支付/SSO/与 `nop-app-mall` 复用论述均为 **`(future)` 占位性描述**，非已落地承诺。
> - `/Users/abc/app/nop-app-mall` 实测不存在，故"复用 nop-app-mall"前提当前落空，相关论述待未来评估。
> - 外部主体身份模型骨架见 `portal/identity-and-access.md`（future，不立即改 orm.xml）。

## 目的

设计客户自助门户与供应商自助门户，实现企业间 B2B 协作的数字化。客户可在线追踪订单/发票/付款，供应商可在线确认 PO/提交 ASN/对账。

## 边界

- **客户门户**（future）：客户登录→查看订单状态/历史、查看/下载发票、在线付款、提交售后工单、更新联系信息/地址簿。
- **供应商门户**（future）：供应商登录→查看/确认/回复采购订单（PO）、提交 ASN（提前发货通知）、提交发票、查看绩效评分。
- **与 nop-app-mall 的关系**（future）：如果部署了电商商城（`nop-app-mall`），客户门户应复用其前端 UI（统一登录/统一地址簿/统一订单查询接口），B2B 客户门户侧重企业级功能（信用额度/批量订单/合同价格/对账），与商城的 B2C 客户门户互补。**当前 `nop-app-mall` 不存在，此复用为占位性描述。**
- 本模块不负责：B2C 商城前端（`nop-app-mall`，future）；EDI 自动对接（`b2b/README.md`）。

## 设计依据

> 参考 **Axelor client-portal**（5 Java 文件）+ **Axelor supplier-portal**（11 Java 文件）+ **ERPNext Customer Portal**。

## 功能清单

### 客户门户

| 功能 | 描述 | 后端接口 |
|------|------|---------|
| 订单查询 | 按日期/状态/单号筛选，查看订单行明细/发货状态 | `IErpSalOrderBiz` read-only |
| 发票查看/下载 | 查看/下载 PDF 发票、查看付款状态 | `IErpFinVoucherBiz` / `IErpSalInvoiceBiz` |
| 在线付款 | 集成第三方支付（银行/支付宝/微信）**(future，受保护区域)** | `ErpFinPayment` 创建 |
| 售后工单 | 提交/查看售后工单进度 | `IErpCsTicketBiz` |
| 地址管理 | 维护收货地址/联系人 | `ErpMdPartner` 地址更新 |
| 信用额度 | 查看当前信用额度/使用情况 | `IErpSalCreditLimitBiz` |
| 报价单确认 | 在线确认/拒绝报价单 | `IErpSalQuotationBiz` |
| 合同查看 | 查看关联合同条款/价格 | `IErpCtContractBiz` |

### 供应商门户

| 功能 | 描述 | 后端接口 |
|------|------|---------|
| PO 查看/确认 | 查看采购订单、确认交期/数量、反馈交期变更 | `IErpPurOrderBiz` |
| PO 回复 | 部分接受/拒绝/建议替代 | `IErpPurOrderBiz` |
| ASN 提交 | 发货前录入运单信息/批次/数量 | `IErpB2bAsnBiz` |
| 发票提交 | 提交采购发票/对账单 | `IErpPurInvoiceBiz` |
| 价格清单查看 | 查看与我方约定的价格 | `IErpPurSupplierPriceListBiz` |
| 绩效评分查看 | 查看质量/交期/价格评分 | `IErpPurSupplierScorecardBiz` |
| 资质更新 | 上传资质文件（ISO/行业认证） | `ErpMdSupplierApproval` |

## 技术架构

```
┌─────────────────────────────────────────────┐
│              Portal Frontend                 │
│  (H5 SPA / Vue / React / 嵌入式 nop页面)    │
│  ┌──────────────┐  ┌────────────────────┐   │
│  │ 客户门户      │  │ 供应商门户          │   │
│  │ 登录→订单/发票│  │ 登录→PO/ASN/对账   │   │
│  │ 售后工单/地址 │  │ 绩效/资质/价格     │   │
│  └──────┬───────┘  └────────┬───────────┘   │
└─────────┼──────────────────┼────────────────┘
          │                  │
          │ SSO (nop-auth OAuth2 / JWT)
          ▼                  ▼
┌─────────────────────────────────────────────┐
│           nop-app-erp Backend                │
│  PortalGatewayController                    │
│  ├─ 认证鉴权（统一 SSO 登录）               │
│  ├─ 客户侧路由（调用 I*Biz read-only 为主） │
│  └─ 供应商侧路由（调用 I*Biz 读写结合）     │
│                                              │
│  各域 I*Biz 接口（复用现有，不新造）         │
└─────────────────────────────────────────────┘
```

## 核心原则

1. **门户是前端聚合层，不是后端业务域**——门户不创建独立实体，不维护独立业务逻辑。门户调用各域的 `I*Biz` 接口完成操作。如需扩展业务逻辑，在各域 BizModel 中增加，门户只负责编排和 UI。
2. **SSO 统一认证**（future，受保护区域）——门户用户通过 `nop-auth` OAuth2/JWT 登录，使用与内部系统一致的认证体系。客户和供应商作为 `ErpMdPartner` 的关联用户，通过 `partner.userId` 关联内部账号。实施前需人工批准。
3. **数据权限隔离**——客户门户只看到本客户的数据（`partnerId` 过滤），供应商门户只看到本供应商的数据。通过 `data-auth.xml` 行级权限实现（项目不预置 tenantId，行级隔离用 data-auth 软过滤而非 tenant 字段）。
4. **复用 nop-app-mall 前端**（future）——若同时部署了商城，客户门户优先复用商城的前端 UI 框架和组件库（统一品牌/导航/登录状态）。**当前 `nop-app-mall` 不存在，此为占位描述。**

## 反模式警示

- ⛔ **门户自建业务逻辑层**——门户是 UI 编排层，业务逻辑在 BizModel，不要在门户代码中写业务规则。
- ⛔ **客户/供应商共用同一个门户**——客户门户和供应商门户的数据域和权限不同，应独立路由/独立部署或同一前端分菜单。
- ⛔ **门户绕过认证直接调用后端 API**——所有 API 必须经过 OAuth2/JWT 鉴权，portal 层不能跳过安全校验。

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-portal.customer-enabled` | true | 是否启用客户门户 |
| `erp-portal.supplier-enabled` | true | 是否启用供应商门户 |
| `erp-portal.sso-type` | OAUTH2 | SSO 类型（OAUTH2/JWT/LDAP） |
| `erp-portal.allow-online-payment` | false | 是否允许客户门户在线付款 |
