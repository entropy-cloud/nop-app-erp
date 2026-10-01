---
分析日期: 2026-09-22
类型: 用户故事 × 现平台能力差距分析（是否满足 / 是否需要满足）
方法: 对照 `docs/requirements/2026-09-22-erp-user-stories.md` 逐故事核对 feature-inventory / product-scope / roles-and-permissions / competitive-comparison / 既有 coverage 矩阵与 owner doc 证据
覆盖标准:
  ✅ = 设计 + 实现已接线（有 owner doc + 落地能力/测试证据）
  🔶 = 设计或实现部分具备（骨架/条件启用/依赖 successor）
  🕒 = 有意延迟 / future / 非当前基线
  ❌ = 明显缺口
需要度: Must = 产品定位下应满足；Should = 应规划但可分期；Could = 可选增强；Won't = 明确不需要/出界
---

# 用户故事差距分析：当前平台能否满足、是否需要满足

> 输入故事：`docs/requirements/2026-09-22-erp-user-stories.md`
> 产品范围权威：`docs/requirements/product-scope.md`（18 域 + notify）
> 功能地图：`docs/design/feature-inventory.md`

## 一、结论摘要

| 判定 | 数量（约） | 含义 |
|------|-----------|------|
| ✅ 满足且 Must | 多数核心 P2P/O2C/库存/财务/制造故事 | 与「产品化通用 ERP」定位一致，已有模型+逻辑+（部分）E2E |
| 🔶 部分满足、Must/Should | 权限生产灰度、移动审批体验、多组织行为层、门户协同 | 能力地基在，运行时/体验/行为层未收口 |
| 🕒 不满足但 Won't/Could（当前） | POS、原生 App、电签外部集成、SaaS 多租户启用、AI 分析 | 出界或配套产品/延迟范围（product-scope 延迟段） |
| ❌ 真实缺口（相对「通用 ERP 完整故事」） | 少数横切：用户级配置、审批移动端专用面、门户未立项 | 需产品裁决，不默认进 backlog |

**总判断**：以 `product-scope.md` 的 18 域通用 ERP 为标尺，**核心业务角色的主干故事已覆盖**；联网资料中的「行业标配」大半已映射到既有域。真正要回答「需不需要」的是横切体验与集成边缘，而非再堆业务域。

## 二、逐故事对照表

### Epic A 主数据

| ID | 满足 | 证据 | 需要度 | 裁决建议 |
|----|------|------|--------|---------|
| US-MD-01 | ✅ | feature-inventory 物料/SKU/单位/条码；`master-data/` | Must | 保持；条码作业见 US-IV-05 |
| US-MD-02 | ✅ | 往来单位统一主数据 | Must | 保持 |
| US-MD-03 | ✅ | COA/币种/`ErpMdOrganization`；competitive 杠杆 B/C | Must | 保持；多组织**行为层**见 US-PL 横切 |
| US-MD-04 | 🔶 | 单据审计字段+高危操作审计有；主数据「变更历史 UI」非独立故事级承诺 | Should | 审计日志可查即可，不单独立项除非合规客户点名 |

### Epic B 采购 P2P

| ID | 满足 | 证据 | 需要度 | 裁决建议 |
|----|------|------|--------|---------|
| US-PO-01 | ✅ | RFQ/报价/PO；coverage 矩阵 Odoo/ERPNext 采购行 | Must | 保持 |
| US-PO-02 | ✅ | 采购入库+部分收货；P2P E2E | Must | 保持 |
| US-PO-03 | ✅ | `purchase/three-way-match.md` | Must | 保持 |
| US-PO-04 | ✅ | 过账+`ErpFinArApItem`+核销；M4 | Must | 保持 |
| US-PO-05 | ✅ | 审批轴+SoDGuard（4 核心域） | Must | 扩展域 SoD 为已登记 successor，不升为新故事 |
| US-PO-06 | ✅ | 退货 + `supplier-evaluation.md` | Should | 保持 |

### Epic C 销售 O2C

| ID | 满足 | 证据 | 需要度 | 裁决建议 |
|----|------|------|--------|---------|
| US-SO-01 | ✅ | SO/CRM 商机转化 | Must | 保持 |
| US-SO-02 | ✅ | 四档价格/价格档；pricing-rule 页面链 | Must | 保持 |
| US-SO-03 | ✅ | 出库可用量校验；O2C E2E | Must | 保持 |
| US-SO-04 | ✅ | AR 发票/收款/核销/账龄报表 | Must | 保持 |
| US-SO-05 | ✅ | 销售退货；回链原单 | Must | 保持 |
| US-SO-06 | ✅ | CRM `sales-forecast`/`territory` 配额 | Should | 保持；与 sales 域边界已文档化 |

### Epic D 库存仓储

| ID | 满足 | 证据 | 需要度 | 裁决建议 |
|----|------|------|--------|---------|
| US-IV-01 | ✅ | 三层模型 Move/Ledger/Balance | Must | 保持 |
| US-IV-02 | ✅ | 调拨状态机（含在途设计） | Must | 保持 |
| US-IV-03 | ✅ | 盘点→移动单；过账 | Must | 保持 |
| US-IV-04 | ✅ | 批次/序列号/效期；召回 | Must | 保持 |
| US-IV-05 | 🔶 | `inventory/barcode-integration.md` 设计在册；现场 PDA 体验/设备流依赖页面与运行验证 | Must（对仓储客户）/ Could（对纯财务轻部署） | **模块组装档位**：制造/商贸档 Must；不默认阻塞核心发布 |
| US-IV-06 | ✅ | DRP 净需求 + MRP；补货建议释放 | Should | 保持 |

### Epic E 财务合规

| ID | 满足 | 证据 | 需要度 | 裁决建议 |
|----|------|------|--------|---------|
| US-FN-01 | ✅ | 凭证模板+posted+兜底；M4/M5 | Must | 保持 |
| US-FN-02 | ✅ | 期间结账/反结账+管理员门控 | Must | 保持 |
| US-FN-03 | ✅ | `bank-reconciliation.md` + E2E | Must | 保持；PSP/模糊匹配为既有分析增强项，非本故事阻塞 |
| US-FN-04 | ✅ | `budget.md` 承付/控制/结转 | Must | 保持 |
| US-FN-05 | ✅ | 多币种+汇兑（核销 fxGainLoss） | Must | 保持 |
| US-FN-06 | ✅ | 红冲回链+高危审计；E4.2 读披露审计（test 灰度） | Must | 保持 |
| US-FN-07 | ✅ | nop-report + 资产负债/利润等种子报表 | Must | 保持 |
| US-FN-08 | ✅ | `expense-claim.md`；M4 相关 | Should | 保持 |

### Epic F 制造质量

| ID | 满足 | 证据 | 需要度 | 裁决建议 |
|----|------|------|--------|---------|
| US-MF-01 | ✅ | BOM+MRP 三件套 | Must（制造档） | 模块组装：非制造客户 Won't |
| US-MF-02 | ✅ | 齐套 STOCK_RESERVED 门控 | Must（制造档） | 同上 |
| US-MF-03 | ✅ | 作业卡+TimeLog | Must（制造档） | 同上 |
| US-MF-04 | ✅ | CostRollup+差异 E2E | Must（制造档） | 同上 |
| US-MF-05 | ✅ | 质检/NCR/CAPA/让步 | Should | 保持 |
| US-MF-06 | ✅ | `quality/recall.md` | Should | 保持 |

### Epic G 资产/维护/项目

| ID | 满足 | 证据 | 需要度 | 裁决建议 |
|----|------|------|--------|---------|
| US-OP-01 | ✅ | assets 全生命周期+折旧 E2E | Should | 保持 |
| US-OP-02 | ✅ | maintenance 计划/请求/访问+备件过账 | Should | 保持 |
| US-OP-03 | ✅ | projects 工时/结算 E2E | Should | 保持 |

### Epic H HR/合同/B2B/物流

| ID | 满足 | 证据 | 需要度 | 裁决建议 |
|----|------|------|--------|---------|
| US-HR-01 | ✅ | hr 员工/合同/考勤/请假 | Should | 保持 |
| US-HR-02 | ✅ | 薪酬引擎+审批（xwf 路径 E2E 有已知限制裁决） | Should | 保持；银行代发文件接口=外部集成延迟段 |
| US-HR-03 | ✅ | 招聘漏斗→hire 联动 | Could | 保持 |
| US-CT-01 | ✅ | contract 版本/开票计划/到期 | Should | 保持 |
| US-B2-01 | ✅ | b2b EDI/ASN+匹配收货 E2E | Should | 保持；真实 EDI 网关=集成延迟 |
| US-LG-01 | ✅ | logistics 发运+轨迹 webhook+运费过账 | Should | 保持；真实承运商=集成延迟 |

### Epic I 横切平台

| ID | 满足 | 证据 | 需要度 | 裁决建议 |
|----|------|------|--------|---------|
| US-PL-01 | 🔶 | action-auth 资源+种子+**%test ON**；%dev/%prod 默认 OFF；菜单塌缩缺口见 roles doc P1.5a 注记 | **Must**（生产可用 ERP） | **需满足**：prod 灰度翻转已是 R2.7/MA6 方向；非新功能，是收口 |
| US-PL-02 | 🔶 | data-auth 规则+**%test 双开关 ON**；prod OFF；orgId 隔离开关独立默认 false | **Must**（多团队） | 需满足（分阶段）：角色行过滤 prod 化优先；org 隔离随多公司需求 |
| US-PL-03 | 🔶 | 通知派发子系统+审批待办语义在；**移动专用审批 UI/推送**无独立承诺 | Must（流程效率）/ 原生 App Could | 需满足「移动可达的 Web 审批面」；独立 App 不默认做 |
| US-PL-04 | ✅ | 看板+报表子系统+E2E 视觉回归推进中 | Must | 保持；当前重点本就是看板运行时回归 |
| US-PL-05 | ✅ | Nop Delta；competitive 杠杆 A | Must（产品差异化） | 保持 |
| US-PL-06 | 🕒 | `portal/README.md` 明确 **future，非 18 域基线**；身份骨架 only | Could（B2B/供应链客户 Should） | **当前不需满足**；触发=客户协同需求立项（plan-first+人工批准） |
| US-PL-07 | 🔶 | 平台 SSO/OAuth2 有；多租户策略「平台标准、未启用」；外部集成=product-scope 延迟 | Should（企业部署包） | 分界：SSO 走平台能力记 Could-Should；SaaS 多租户与税控/银行/物流/电商=**维持延迟，不需当前满足** |
| US-PL-08 | 🔶 | 条码设计有；flux 响应式/PDA 专用作业面未作为独立 UX 基线验收 | Should（仓储档） | 与 US-IV-05 同档位裁决 |

## 三、「需不需要满足」的裁决框架

对齐 `product-scope.md` 与 `competitive-comparison.md §六`：

### 3.1 必须满足（当前产品叙事内）

1. **核心闭环故事**（US-PO-*、US-SO-*、US-IV-01..04、US-FN-01..07）— 已基本满足，缺口主要是**端到端验证深度与看板视觉回归**（已是当前阶段重点），不是新故事。
2. **权限与审计生产化**（US-PL-01/02、US-FN-06）— 机制在、%test 已翻，**prod 默认关**；作为可交付 ERP **需要满足**，路径是既有 R2.7/MA6/enforcement 路线，不新开产品方向。
3. **业财一体与内控**（三单匹配、SoD、期间关账、预算）— 已是 M4/M5 已完成能力，**需要保持回归绿**。

### 3.2 应规划但可分期（Should）

- 条码/PDA 与移动可达审批（US-IV-05、US-PL-03、US-PL-08）— 按客户档位；设计资产在，补运行时与页面体验验收。
- 多组织行为层（org 数据隔离/公司间交易）— competitive 已诚实登记为「结构地基有、行为层待建」；**有跨公司客户需求时升 Must**。
- 真实外部集成（银企、税控、承运商、EDI 网关、电签）— 保持 product-scope「延迟范围」；本地 mock 已允许。

### 3.3 当前不需要满足（Won't / 配套 / 延迟）

| 故事/能力 | 理由 |
|-----------|------|
| US-PL-06 门户 | 设计层已标非基线；B2B 协同故事由内部 EDI 模块部分覆盖 |
| POS 零售 | coverage 矩阵 🕒；非 18 域 |
| 电商商城 | 配套 `nop-app-mall`（实存于 `nop-app-mall-wt` 工作树），不进本仓 |
| SaaS 多租户启用 | product-scope 延迟（待业务确认） |
| 原生移动 App | 无产品承诺；Web/PDA 优先 |
| AI 预测性分析/GenAI 单据 | 调研有、基线无；不阻塞通用 ERP 定义 |
| 预测性维护 IoT | maintenance 已标数据未就绪→IoT 触发 |

### 3.4 相对调研「可能显缺、但裁为非必须」的点

- **用户级个性化配置**（analysis 0004 已指出用户级空白）— Could；企业客户再议。
- **审批金额阈值多级矩阵**（部分行业文模板）— 现有 nop-wf 可配，故事由 US-PO-05 代表；细则归 wf 配置非新域。
- **银行自动对账 API 拉取** — 手工/导入对账已有；API=集成延迟。

## 四、建议动作（不自动执行）

1. **不新增业务域**：用户故事主干已由 18 域吸收；避免把调研词直接变 backlog。
2. **把「需满足」的横切项对齐既有路线**：权限 prod 灰度、看板/端到端验证 — 对接 `docs/backlog/` 现有 P 项与 R2.7/MA6，而非平行新计划。
3. **档位化验收**：纯商贸 / 制造 / 完整 三档组装（product-scope）下，对 US-IV-05、US-MF-*、US-B2-* 标注档位 Must，避免全量冒充 Must 导致范围膨胀。
4. **门户与移动 App**：维持 🕒，触发条件写入 product-scope 延迟段已有语义即可（本分析不改 product-scope）。
5. 若需把本用户故事升为实现就绪需求，按 `00-requirement-synthesis-guide.md` 对**单个**故事拆 Feature 需求，不在本文件上直接开工。

## 五、诚实性声明

- 满足度判断以 **owner doc + feature-inventory + 既有 roadmap/E2E 证据** 为准，不以「文档提到」冒充「运行时全绿」（lessons 文档化简化滥用）。
- 「已设计未 prod 默认开启」记为 🔶 而非 ❌ 或 ✅（config-gate 裁决惯例）。
- 调研来源为公开营销/文档交叉印证，非逐产品实测；与既有 `docs/analysis/2026-06-30-1200-feature-coverage-matrix.md` 冲突时以该矩阵+实时仓库为准。

## 参考（调研 URL 摘录）

- Oracle ERP modules：https://www.oracle.com/erp/erp-modules/
- NetSuite modules/features/use cases：https://www.netsuite.com/portal/resource/articles/erp/erp-modules.shtml 、`erp-features.shtml` 、`erp-use-cases.shtml`
- Odoo purchase/barcode：https://www.odoo.com/app/purchase-features 、https://www.odoo.com/documentation/18.0/applications/inventory_and_mrp/barcode.html
- Odoo UAT 角色场景（第三方）：https://erpeek.ai/blog/odoo-uat-playbook
- ERPNext roles：https://prilk.com/docs/erpnext/users-roles
