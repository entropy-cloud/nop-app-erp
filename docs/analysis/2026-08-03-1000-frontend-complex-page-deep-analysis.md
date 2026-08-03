# 前端复杂页面深度分析报告（设计层 × 实现层全景）

> 日期：2026-08-03
> 性质：深度分析（研究级，非 owner doc）
> 范围：nop-app-erp 18+1 域前端（AMIS view.xml + page.yaml）的复杂/特殊页面全景
> 方法：设计文档层（范式 owner docs + 18 域 ui-patterns + 分析文档）× 实现层（352 手写 view.xml + 383 main.page.yaml）交叉核查
> 相关：`docs/analysis/2026-07-19-frontend-ui-design-completeness-and-quality-analysis.md`（完备性审计）、`docs/analysis/2026-07-31-1000-complex-page-pattern-catalog.md`（模式目录）

---

## 1. 背景与目的

nop-app-erp 前端实现遵循「codegen 骨架 + 手写定制」双层结构：每个实体生成 `_gen/*.view.xml` 与手写层 `*.view.xml`、`main.page.yaml`、`picker.page.yaml`。绝大多数页面是标准 CRUD（单表列表 + 弹窗编辑），但存在一批**布局或数据交互显著超出标准模板**的复杂页面——它们通常是：

- 设计文档明确标注的「核心复杂页面」（如 finance 凭证录入、aps 排产甘特图）
- 需要 AMIS 非原生能力或降级策略的页面（看板拖拽、日历、wizard）
- 跨实体/跨域聚合的页面（三单匹配、版本对比、收件箱）
- 与后端聚合 BizModel 深度耦合的页面（11 个看板、24 个报表页）

本报告回答三个问题：

1. 项目有哪些复杂页面，分别属于什么模式？
2. 设计文档对它们说了什么（范式、契约、反模式）？
3. 实现与设计的关系：已落地 / 降级实现 / 占位未实现？哪些页面需要特别关注？

**与前序分析的关系**：`2026-07-19-frontend-ui-design-completeness-and-quality-analysis.md` 在 F1–F16 全部未实现时评估「设计完备性 vs 实现缺口」；`2026-07-31-1000-complex-page-pattern-catalog.md` 从模式目录角度提出 12 布局 × 8 数据交互框架。本报告在两者之后，基于**当前实现层事实**（F12/F13/F16 已大量落地）重新盘点，聚焦「设计文档 → 实现」的对应关系与遗留风险。

---

## 2. 事实基线：前端资产规模

| 资产 | 数量 | 说明 |
|---|---|---|
| 手写 view.xml | 352 | `module-*/erp-*-web/.../pages/**/*.view.xml`（排除 `_gen/`） |
| main.page.yaml | 383 | 每实体标准列表页 |
| 非标准 page.yaml | 44 | 非 main/picker/ref 命名（含 24 报表页、看板、向导、联查等）；加 38 ref = 82（见 §3.2） |
| ref-*.page.yaml | 38 | 关联子表 drawer 薄页（`x:gen-extends` + fixedProps）；**35 个被 drawer 接线，3 个死文件**（见 §6.1） |
| 占位页面 | 16 | 7 行 alert「待实现」（见 §6.2） |
| 报表页 | 24 | 11 域 `pages/report/`，全部接线 `renderHtml` + XLSX/PDF 下载 |
| xpt.xml 报表模板 | 25 | `_vfs/nop/main/report/{domain}/` |
| Dashboard BizModel | 11 | 10 域 `Erp*DashboardBizModel` + cs 绩效看板 |

**复杂度标记统计**（手写 view.xml 层，经 grep 全量核实）：

| 标记 | 文件数 | 含义 |
|---|---|---|
| `layoutControl="tabs"` | 15 | form 内 tabs 分组（主单据/档案） |
| `sub-grid-edit` | 60 | 子表行内编辑（头行单据） |
| `sub-grid-view` | 73 | 子表只读视图 |
| `actionType="drawer"` | 69 | 关联 drawer（35 个 ref 页被 19 个 view.xml 接线 + voucher-by-bill×9 + bills-by-voucher） |
| `gen-control` | 133 | 自定义列渲染（手写层 `<gen-control>` 元素块 337 个；status/docStatus/approveStatus 字典色块为主流用法。注意 `gen-control-classification-audit.md` 中的「337」为 966 总块的 C 类口径，数值相同、含义不同） |
| `onEvent` | 24 | 行内自动推算/联动交互（ErpFinVoucherLine 9 处、ErpMdMaterial 6 处、ErpFinReconciliation 等） |
| tree-list / `@TreeChildren` | 4 | 树形主数据（ErpMdMaterialCategory/ErpMdSubject/ErpHrDepartment/ErpCsServiceCatalogItem） |
| `<pages><tabs>`（view.xml 层） | 3 | 机制 B 生产先例：ErpAstAsset:170 / ErpMntEquipment:235 / ErpHrEmployee:395（`mountOnEnter` + 多 simple/crud 子页）；整页 `type: tabs` 仅 notify inbox 一例 |

> 注：`controlLib="/erp/xlib/control.xlib"` 351 处为全库统一标准引用，非差异化复杂度。

---

## 3. 复杂页面全景地图

### 3.1 设计层识别：18 域 ui-patterns 标注的核心复杂页面

每域 `docs/design/<domain>/ui-patterns.md` 均识别出 1–4 个深度复杂页面（18 个文件合计 5,991 行设计）。按域罗列（★ = 公认最深复杂页，跨文档反复引用）：

| 域 | ui-patterns 识别出的复杂页面 | 主要设计文档 |
|---|---|---|
| finance | ★**会计凭证录入**（分录子表借贷单侧输入/科目树弹窗/合计实时平衡/自动平衡按钮/快速模板）、凭证查询三级下钻（总账→明细账→凭证）、凭证模板配置、期末结账向导入口 | `finance/ui-patterns.md`、`child-table-editor-patterns.md` §16、`visible-on-patterns.md` §8、`page-structure-patterns.md` §8.1/8.6/§5、`voucher-back-link-patterns.md` |
| manufacturing | ★**BOM 编辑**（多级展开/phantom/完整性校验）、**工单进度仪表板**（4 阶段进度条）、BOM 树形浏览 | `manufacturing/ui-patterns.md`、`manufacturing/bom-and-routing.md`、`page-structure-patterns.md` §8.4/8.8 |
| aps | ★**排产甘特图**（拖拽调整/右键菜单/统计栏） | `aps/ui-patterns.md`、`aps/scheduling.md` §8（JSON 数据契约）、`page-structure-patterns.md` §8.7 |
| quality | 质检单录入与结果判定（规格带出/判定自动计算/触发 NCR）、NCR 详情（CAPA 子表 + 效果验证）、SPC 控制图 | `quality/ui-patterns.md`、`quality/spc.md`、`page-structure-patterns.md` §8.5 |
| maintenance | **设备详情档案**（状态大色块/维护时间轴/待执行计划倒计时）、**维护访问执行 4 步向导** | `maintenance/ui-patterns.md`、`maintenance/equipment-integration.md`、`page-structure-patterns.md` §5.1 |
| purchase | 采购订单（M2M 物料弹窗/部分入库进度条）、采购发票（**三单匹配校验**/红冲链条）、付款单（**核销明细子表**/自动核销） | `purchase/ui-patterns.md`、`purchase/three-way-match.md`、`page-structure-patterns.md` §8.3 |
| sales | 销售订单（价格清单联动/改价标记/可用量预检）、收款单核销子表 | `sales/ui-patterns.md`、`sales/returns.md` |
| inventory | 库存移动单（**opType 驱动动态表单**/批次序列号行内录入）、库存盘点（3 阶段流程）、流水余额只读钻取 | `inventory/ui-patterns.md`、`inventory/trace-chain.md` |
| master-data | 物料/SKU 编辑（SKU 子表/多档价格）、物料分类/科目树形管理、往来单位三区布局 | `master-data/ui-patterns.md`、`tree-entity-patterns.md` |
| assets | **资产卡片仪表板**（双列 + 折旧时间线）、**处置 3 步向导**（清理损益预览） | `assets/ui-patterns.md`、`assets/depreciation-and-posting.md` |
| projects | **项目详情仪表板**（预算进度条/任务进度/关联单据聚合）、任务看板、周工时网格 | `projects/ui-patterns.md`、`page-structure-patterns.md` §3 |
| hr | ★**员工详情 6 tab 档案**、**组织架构图**、团队休假日历矩阵、薪酬核算审批汇总页、招聘管道看板 | `human-resource/ui-patterns.md`、`page-structure-patterns.md` §8.9 |
| crm | ★**商机 Kanban**（动态列）、活动日历、转化 3 步向导 | `crm/ui-patterns.md`、`crm/cpq.md` §2、`non-standard-views-patterns.md` |
| customer-service | ★**工单看板**（SLA 红灯）、客服仪表板、知识库管理 | `customer-service/ui-patterns.md`、`non-standard-views-patterns.md` |
| logistics | ★**发运单详情**（追踪时间线地图/包裹明细/网关日志）、比价面板 | `logistics/ui-patterns.md` |
| contract | ★**合同详情 6+ tab**、**版本对比**（双栏 diff） | `contract/ui-patterns.md`、`page-structure-patterns.md` §8.10 |
| drp | ★**净需求计算报表**（物料分组折叠 + Σ 公式可视化）、补货建议审批（可编辑表格 + 批量批准） | `drp/ui-patterns.md`、`page-structure-patterns.md` §8.11 |
| b2b | ★**EDI 事务详情**（状态时间线/报文查看/重试）、ASN 五阶段流程步骤条 | `b2b/ui-patterns.md`、`b2b/edi-formats.md` §7-8、`page-structure-patterns.md` §8.12 |
| notify | 通知收件箱（三 tab） | `notify/inbox-patterns.md`（无 ui-patterns.md，见 §6.3） |

### 3.2 实现层清单：真实落地的复杂页面（30 个核心页 + 23 个附属形态）

> **计数口径**：核心复杂页面 30 = A(11) + B(10) + C(7) + D(2)。E（独有形态 4）与 F（view.xml 层 19）为附属形态，单列计数。

**A. 看板 Dashboard（10 + 1）**——全部真实实现，标准三段式（KPI 卡 + 趋势图 + 预警 crud），后端 11 个 `Erp*DashboardBizModel`：

- 域级看板 ×10：`{fin,qa,inv,mfg,prj,pur,sal,ast,mnt,md}/pages/dashboard/main.page.yaml`（113–273 行）
- cs 绩效看板：`ErpCsQualityDashboard/main.page.yaml`（149 行，独立实体目录）

**B. 特殊 dashboard 子页（10）**：

| 页面 | 文件 | 模式 |
|---|---|---|
| 排产甘特图 | `aps/dashboard/schedule-gantt.page.yaml`（181 行） | echarts time 轴甘特条（只读） |
| 三单匹配 | `pur/dashboard/three-way-match.page.yaml`（183 行） | 差异预警 crud + 三表并列 |
| BOM 树 | `mfg/dashboard/bom-tree.page.yaml`（121 行） | explode + 栈算法重建 + AMIS tree |
| 组织架构图 | `hr/dashboard/org-chart.page.yaml`（145 行） | 树降级为 each+tpl 缩进列表 |
| 薪酬核算审批 | `hr/dashboard/payroll-approval.page.yaml`（208 行） | service 聚合 + 组织分组汇总 |
| 发运追踪 | `log/dashboard/shipment-tracking.page.yaml`（123 行） | shipment + logs 双查询时间线 |
| 净需求报表 | `drp/dashboard/net-requirement.page.yaml`（170 行） | 按仓库分组聚合 |
| 合同版本对比 | `ct/dashboard/version-diff.page.yaml`（203 行） | 双版本选择 + 差异表 |
| ASN 流程跟踪 | `b2b/dashboard/asn-flow.page.yaml`（184 行） | 状态色块流程条 |
| EDI 事务详情 | `b2b/dashboard/edi-detail.page.yaml`（141 行） | EdiLog + EdiDoc 双查询 + payload 开关 |

**C. F13 非标准视图族（7）**：

- 看板 3：`prj/ErpPrjTask/kanban.page.yaml`（252 行，4 列）、`cs/ErpCsTicket/kanban.page.yaml`（317 行，6 列 + SLA 🔴 + YAML anchor）、`crm/ErpCrmLead/opportunity-kanban.page.yaml`（动态列 stage 循环）
- 时间线 2：`crm/ErpCrmActivity/timeline.page.yaml`（83 行，**原生 timeline prop 绑定失败 → each+tpl 降级**）、`cs/ErpCsTicketAction/timeline.page.yaml`
- 日历 2：`crm/ErpCrmActivity/calendar.page.yaml`（103 行，**原生 calendar React 报错 #130 → 按日分组卡片网格降级**）、`hr/ErpHrLeaveRequest/team-vacation-calendar.page.yaml`（矩阵 table）

**D. 向导（2）**：

- ★`fin/pages/period-close-wizard/main.page.yaml`（**416 行，全库最复杂页面**）：4+1 步（preCheck→closePeriod→年度结转→finalizePeriod + 反结账），HTML 步骤指示器，dialog 嵌套 service，per-module 关账状态卡
- `mnt/pages/visit-wizard/main.page.yaml`（306 行）：维护访问执行 4 步向导

**E. 其他独有形态**：

- ★`notify/ErpSysNotification/inbox.page.yaml`（329 行）：全库唯一 `type: tabs` 页（未读/已读/全部）+ 客户端 JS 过滤 + countUnread/markAllRead
- ★`md/pages/party-search/main.picker.page.yaml`（126 行）：全库首例手写 picker（非实体 BizModel `ErpPartyBizModel`，文件名 `main.picker.page.yaml` 独有）
- `fin/ErpFinVoucherBillR/voucher-by-bill.page.yaml` + `bills-by-voucher.page.yaml`：凭证↔单据双向联查，voucher-by-bill 被 **9 个视图 drawer 跨域引用**（跨域最广）

**F. view.xml 层复杂视图（15 tabs + 60 子表对 + 4 树形）**：

- tabs 视图 15：ErpPurOrder/ErpSalOrder/ErpInvStockMove/ErpMfgWorkOrder/ErpFinVoucher/ErpFinVoucherTemplate/ErpHrEmployee/ErpAstAsset/ErpMntEquipment/ErpPrjProject/ErpQaInspection/ErpQaNonConformance/ErpCrmLead/ErpCsTicket/ErpCtContract（60 个 sub-grid-edit 文件实为 67 个子表块）
- 树形 4：ErpMdMaterialCategory、ErpMdSubject、ErpHrDepartment、ErpCsServiceCatalogItem

### 3.3 设计-实现对应矩阵（摘要）

| 设计文档识别的复杂页 | 实现状态 | 实现文件 |
|---|---|---|
| 凭证录入（F16 §8.1/8.6） | ✅ 落地（autoBalance + balanceBadge + 快速模板 dialog） | `ErpFinVoucher.view.xml` |
| 凭证模板预览（§8.2） | ✅ 落地（renderTemplate mutation + 预览 input-table） | `ErpFinVoucherTemplate.view.xml` |
| 三单匹配（§8.3） | ✅ 落地 | `pur/dashboard/three-way-match.page.yaml` |
| 工单进度仪表板（§8.4） | ✅ 落地（progress tab tpl） | `ErpMfgWorkOrder.view.xml` |
| NCR 详情（§8.5） | ✅ 落地（capa tab sub-grid-view） | `ErpQaNonConformance.view.xml` |
| 甘特图（§8.7） | ✅ 落地（只读，拖拽=Non-Goal） | `aps/dashboard/schedule-gantt.page.yaml` |
| BOM 树（§8.8） | ✅ 落地 | `mfg/dashboard/bom-tree.page.yaml` |
| 薪酬审批汇总页（§8.9） | ✅ 落地 | `hr/dashboard/payroll-approval.page.yaml` |
| sales 价格清单/价格规则 | ⚠️ 薄页包装（实体目录外同名薄页），无深度交互 | `sal/pages/{pricing-rule,sales-price-list}/main.page.yaml` |
| 合同版本 diff（§8.10） | ✅ 落地（元数据对比 + content 并排，字段级 diff 数据缺失降级） | `ct/dashboard/version-diff.page.yaml` |
| drp 净需求报表（§8.11） | ✅ 落地 | `drp/dashboard/net-requirement.page.yaml` |
| ASN 流程条（§8.12） | ✅ 落地（4 值字典为准） | `b2b/dashboard/asn-flow.page.yaml` |
| 期末结账向导（§5） | ✅ 落地 | `fin/pages/period-close-wizard/main.page.yaml` |
| 维护执行向导（§5.1） | ✅ 落地 | `mnt/pages/visit-wizard/main.page.yaml` |
| 看板×3 / 时间线×2 / 日历×2（F13） | ✅ 落地（3 个组件级降级，见 §4） | 各 page.yaml |
| 员工档案 / 资产卡片 / 设备仪表板（§3 机制 B） | ✅ 落地（3 个 view.xml 内嵌 pages 级 tabs：ErpHrEmployee/ErpAstAsset/ErpMntEquipment） | `ErpHrEmployee.view.xml:395` 等 |
| **注**：配套 ref-employee / ref-asset / ErpMntEquipment 本目录 ref-equipment 三个 page.yaml 存在但**未被任何 drawer 接线（死文件）** | ⚠️ 死文件 | — |
| SPC 三件套（spc.md） | ⚠️ 独立页占位；能力内嵌质量看板 echarts | `qa/dashboard/main.page.yaml` |
| 客服仪表板 / 知识库 | ⚠️ 仪表板✅；知识库未实现（占位缺失） | `ErpCsQualityDashboard/main.page.yaml` |
| 资产处置 3 步向导（assets/ui-patterns） | ❌ 未实现（无对应页面） | — |
| 库存盘点 3 阶段 UI（inventory/ui-patterns） | ❌ 未实现（标准 CRUD 三状态，无流程页） | — |
| 转化向导 3 步（crm/ui-patterns） | ❌ 未实现（cpq.md 有 wizardLayout 模型） | — |
| 发运追踪时间线地图（logistics/ui-patterns） | ⚠️ 时间线✅，地图降级未做 | `log/dashboard/shipment-tracking.page.yaml` |
| 知识库富文本/分类树（cs/ui-patterns） | ❌ 未实现 | — |

---

## 4. 需要特别关注的复杂页面（逐页深度剖析）

### 4.1 finance：期末结账向导（全库最复杂页面）

- **设计文档**：`docs/design/page-structure-patterns.md` §5（wizard 范式：select 置于 page body 共享 scope、预渲染 HTML 步骤指示器、`actionType:ajax` + reload、adaptor 探测 errors）；`docs/design/finance/period-close.md`（8 步概念）；`docs/design/finance/ui-patterns.md`（结账入口）
- **实现**：`module-finance/erp-fin-web/.../pages/period-close-wizard/main.page.yaml`（416 行）
- **复杂度特征**：零后端 delta 编排 4 个 @BizMutation；反结账 dialog 内二次确认（红冲影响预览）；年度结转仅 12 月可见（visibleOn 分支）；per-module 关账状态卡（arStatus/apStatus/invStatus/glStatus/assetStatus）
- **关注点**：财务保护区域；`@BizQuery` 复杂返回必须显式 field selection；`hasIssues()` 等 has* 方法不被暴露为 GraphQL 字段（§5 反模式表）；12 月分支与年度结转联动是回归高发区；**反结账/反审核属高危操作，按钮可见性与确认流受 `docs/design/roles-and-permissions.md` 高危操作权限表约束**（需管理员或额外审批），前端 visibleOn 仅预拦截

### 4.2 finance：凭证录入 / 凭证模板 / 凭证↔单据联查

- **设计文档**：`child-table-editor-patterns.md` §16（17 列子表、dcDirection 行内切换、科目 picker 8 字段快照、subject 驱动 6 维 visibleOn、多币种推算、autoBalance）；`visible-on-patterns.md` §8（dcDirection 切换 + 反审核冲销预览）；`page-structure-patterns.md` §8.1（头聚合刷新 graceful fallback：行 onEvent 不能写头字段 → autoBalance 按钮 + balanceBadge）；§8.2/8.6（renderTemplate 预览 + 快速模板 toolbar）；`voucher-back-link-patterns.md`（业财回链）
- **实现**：`ErpFinVoucher.view.xml`、`ErpFinVoucherTemplate.view.xml`、`ErpFinVoucherBillR/{voucher-by-bill,bills-by-voucher}.page.yaml`
- **关注点**：`${Number(totalDebit)==Number(totalCredit)}` 数值比较防 NaN；模板算术求值器白名单安全；voucher-by-bill 被 9 个视图跨域引用——任何 URL/字段契约变更影响面最大

### 4.3 quality：质量看板（内嵌 SPC 控制图）与 SPC 占位缺口

- **设计文档**：`docs/design/quality/spc.md`（控制图数据模型：UCL/LCL/CL、计数值/计量值分支、失控规则）；`docs/design/dashboards.md` §9（SPC 失控预警）
- **实现**：`qa/pages/dashboard/main.page.yaml`（273 行，全库最复杂看板）——echarts line + markLine（UCL/LCL/CL 虚线）+ 失控点红色 + tooltip 违规规则明细；`ErpQaDashboard__getSpcControlChartData`；后端 SPC 计算器（ControlLimit/RuleEngine/Capability/Statistics/AttributesFormulas）+ ErpQaSpcChartBizModel + **4 个 processor**（采集样本/重算控制限/规则评估/能力计算）完整
- **关注点**：`qa/pages/spc-{chart,capability,sample}/main.page.yaml` 三个独立页均为 7 行占位——**SPC 后端能力完整但独立页面缺失**，当前仅能经质量看板间接使用。这是 F16 落地后最显著的「后端就绪、前端未交付」缺口（也印证 `2026-07-20-complex-ui-controls-inventory-for-flux.md` 将 SPC 控制图列为剩余控件缺口）

### 4.4 manufacturing：BOM 树 / 工单进度 / 工单-作业卡-领料 drawer 群

- **设计文档**：`page-structure-patterns.md` §8.8（explode 扁平结果 + level 栈算法重建、phantom 已合并无需前端处理、adaptor 必须吞 GraphQL error）；§8.4（progress tpl 防 NaN）；`manufacturing/bom-and-routing.md`（BomExpander pre-order DFS）；`manufacturing/ui-patterns.md`
- **实现**：`mfg/dashboard/bom-tree.page.yaml`（121 行）、`ErpMfgWorkOrder.view.xml`（6 tab + progress）、`ErpMfgJobCard/ref-work-order.page.yaml`、`ErpMfgMaterialIssue/ref-work-order.page.yaml`
- **关注点**：栈算法层级正确性（pre-order 假设）、未填 bomId 时 explode 报错需优雅降级；工单→作业卡→领料单的 drawer 链是跨单据追溯的代表路径

### 4.5 aps：排产甘特图（F16 高风险落地）

- **设计文档**：`page-structure-patterns.md` §8.7（echarts custom series renderItem + encode、dateRange 客户端过滤、`.replace(' ','T')` 解析、`_ErpApsOperationOrder` 无 scheduleId 字段的核实裁决）；`aps/scheduling.md` §8.2-8.3（颜色编码契约 + 未来拖拽 spec `IEtpApsGanttService`）；`aps/ui-patterns.md`
- **实现**：`aps/dashboard/schedule-gantt.page.yaml`（181 行，只读甘特，dataZoom + tooltip 读 `_op` meta）
- **关注点**：拖拽 = 明确 Non-Goal（需 AMIS 原生拖拽组件或第三方扩展，触发条件在 `non-standard-views-patterns.md` §7）；只读甘特与设计文档「拖拽调整 + 右键菜单」的差距是当前最大单项 UX 缺口

### 4.6 hr：组织架构图 / 薪酬审批 / 休假矩阵 / 员工档案

- **设计文档**：`page-structure-patterns.md` §3（机制 B 员工档案 5 tab drawer + `mountOnEnter=true`/`unmountOnExit=false` 懒加载）、§8.9（薪酬审批：实体无 departmentId 用 orgId 分组、`filter_<field>` 简写不支持的 QueryBean 裁决）；`non-standard-views-patterns.md` §4.3（矩阵日历 custom table）；`human-resource/ui-patterns.md`
- **实现**：`ErpHrEmployee.view.xml`（6 tab 含敏感字段隐藏，机制 B pages 级 tabs :395）、`hr/dashboard/org-chart.page.yaml`（145 行，**AMIS tree 渲染器降级为 each+tpl 缩进**）、`payroll-approval.page.yaml`（208 行）、`team-vacation-calendar.page.yaml`（注：`ref-employee.page.yaml` 为死文件，见 §6.1）
- **关注点**：org-chart 是「设计用 AMIS tree、实现降级缩进列表」的代表——视觉差距最大；薪酬审批 `query:{limit:2000}` + 客户端过滤对大数据量有性能上限（successor 需后端 @BizQuery 聚合）

### 4.7 crm：商机看板 / 活动日历 / 时间线（F13 组件降级集中区）

- **设计文档**：`non-standard-views-patterns.md` §2.2（动态列看板 service+each+每阶段 crud）、§3（原生 timeline 裁决）、§4.2（原生 calendar 裁决）；§5（mutation 契约：`ErpCrmLeadBizModel:105` moveStage + LeadProcessor 守卫）
- **实现**：`opportunity-kanban.page.yaml`、`ErpCrmActivity/timeline.page.yaml`（83 行）、`ErpCrmActivity/calendar.page.yaml`（103 行）
- **关注点**：**三处实现期组件降级未在范式文档中回填**——① crm 活动时间线与 ② cs 工单操作时间线：原生 `type: timeline` prop 契约经 service scope 失败，降级 each+tpl（cs 文件头注释明确记载「同 crm activity-timeline」）；③ crm 活动日历：原生 `type: calendar` React 渲染报错（Minified React error #130），降级按日分组卡片网格。`non-standard-views-patterns.md` §3.1/§4.1 仍宣称原生组件直接可用，与实现证据冲突，属**文档漂移**（见 §6.4）

### 4.8 cs：工单看板（6 列 + SLA）与客服绩效看板

- **设计文档**：`non-standard-views-patterns.md` §2.2（6 列固定、SLA 超时 🔴 tpl、NEW 列 bg-warning-subtle）；§5.1（`ErpCsTicketBizModel` assign/start/resolve/close/reopen/cancel 六态 mutation，行号以范式文档为准）；`customer-service/ui-patterns.md`
- **实现**：`ErpCsTicket/kanban.page.yaml`（317 行，YAML anchor `&ticketApiTpl` 复用 6 列 API 定义）
- **关注点**：SLA 违约 close 需 remark 的后端守卫与前端 dialog 的契约一致性；看板列数 = 字典值，字典变更会静默影响列结构

### 4.9 notify：通知收件箱（页级 tabs 唯一实现）

- **设计文档**：`docs/design/notify/inbox-patterns.md`（三 tab 数据源策略、userId 后端 fallback 裁决、未读计数实时 service、6 条反模式）
- **实现**：`ErpSysNotification/inbox.page.yaml`（329 行，`type: tabs` + 客户端 JS 多维过滤 + markAllRead）
- **关注点**：`page-structure-patterns.md` §2 机制 B 在 view.xml 层已有 3 个生产先例（AstAsset/MntEquipment/ErpHrEmployee 内嵌 pages 级 tabs），但**整页 `type: tabs`** 形式仅 inbox 一例——inbox 是机制 B 的 page.yaml 变体（无 view.xml 包装、纯 AMIS tabs 直写），其「客户端 JS 过滤 + 全量拉取」模式在数据量增长后需后端分页支持

### 4.10 contract / b2b / drp / logistics：扩展域特殊页

- **合同版本对比**：`ct/dashboard/version-diff.page.yaml`（203 行）。设计文档 `page-structure-patterns.md` §8.10 的关键裁决——**ErpCtContractVersion 仅 content blob + 元数据，字段级 diff 数据模型不支持**，降级元数据对比表 + content `<pre>` 并排。这是「数据模型决定 UI 上限」的教科书案例
- **ASN 流程**：`b2b/dashboard/asn-flow.page.yaml`（184 行）。§8.12 裁决：roadmap「五阶段」为笔误，字典实为 4 值（RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED）；ErpB2bAsnLine 无 matched-status 字段，匹配状态经 quantity vs shippedQty 推断
- **EDI 详情**：`b2b/dashboard/edi-detail.page.yaml`（141 行，双查询 + payload 开关）。设计文档 `b2b/edi-formats.md` §7-8
- **净需求报表**：`drp/dashboard/net-requirement.page.yaml`（170 行）。§8.11：AMIS crud 无原生行分组 → service 分组 + each section + 嵌套 table
- **发运追踪**：`log/dashboard/shipment-tracking.page.yaml`（123 行）。设计文档 `logistics/ui-patterns.md` 的「时间线地图」视觉未实现（地图 = Non-Goal）

### 4.11 master-data：party-search 手写 picker（全库首例）

- **设计文档**：`picker-patterns.md`（7 个高频 picker 范式 + bounded-merge）；`master-data/ui-patterns.md`（统一往来单位身份）
- **实现**：`md/pages/party-search/main.picker.page.yaml`（126 行，`ErpParty__findParties` 联合检索 + onSelect 回填 + mapping 渲染；文件名 `main.picker.page.yaml` 非标准命名）
- **关注点**：非实体 BizModel 的 picker 无法走 codegen，此文件是后续同类需求（如跨域搜索）的模板；命名偏离标准（picker.page.yaml）可能导致工具链扫描遗漏

### 4.12 报表与看板基建：24 报表 + 11 看板

- **设计文档**：`docs/design/dashboards.md`（9 域指标定义 + 实现约定 §6）；`docs/architecture/view-and-page-strategy.md` §5（`$var` 转义、`dataType: raw`、service 内 html 拍平范式）
- **实现**：24 个 `report/*.page.yaml`（统一模式：参数 form + `ErpXxxReport__renderHtml` + `/p/ErpXxxReport__download` blob 下载）+ 25 个 xpt.xml；11 个 DashboardBizModel + 15 个测试类
- **关注点**：看板指标口径与 `dashboards.md` 的可追溯性（每指标标注实体/计算口径）；`2026-07-31-1000` 目录指出 A8 看板 + A12 报表是「参数表单 + 渲染容器」同构模式，可模板化

### 4.13 finance：对账执行页（onEvent 交互类复杂页代表）

- **设计文档**：`finance/ar-ap-reconciliation.md`（对账与冲销语义）；`visible-on-patterns.md` §8.5（反审核冲销 preview-then-confirm）
- **实现**：`ErpFinReconciliation.view.xml`——`runAutoReconciliation` 自动对账 mutation + onEvent 联动 + 冲销预览 dialog（onEvent 高级交互的 24 个手写 view.xml 中财务域代表）
- **关注点**：onEvent 是「行内自动推算/跨控件联动」的交互类复杂度标记（§2 表 24 文件），与结构类标记（tabs/子表/drawer）正交；对账-冲销涉及 AR/AP 保护区域，按钮可见性受 `roles-and-permissions.md` 高危操作权限表约束

---

## 5. 跨域模式级观察（12 布局 × 8 数据交互在本项目的落地）

`docs/analysis/2026-07-31-1000-complex-page-pattern-catalog.md` 提出 12 布局 × 8 数据交互框架。本项目实现层实证：

| 布局模式 | 项目实例 | 数据交互 | 备注 |
|---|---|---|---|
| A1 单表 CRUD | ~85% 页面（352 view.xml 的大多数） | B1 单实体 | 完全模板化 |
| A2 头行单据 | 15 tabs + 60 sub-grid-edit 对 | B2 随头提交（ORM cascade） | 「标准 ERP 单据页」= 最高价值抽象 |
| A3 主从分屏 | 无（`crud.layoutMode="bottom-detail"` 平台休眠钩子未接线） | B6 | **布局缺口**，详见 §6.2 |
| A4 tabs 工作台 | 15 form 级 tabs + 3 view.xml pages 级 tabs（AstAsset/MntEquipment/ErpHrEmployee）+ inbox 整页 tabs | B2/B5 | 机制 B 两形态均有先例 |
| A5 树形 | 4 实体 | B1 | tree-list 三件套标准 |
| A6 看板 | 3 kanban（prj/cs/crm） | B8 状态机写 | 拖拽降级为列式 crud + row-action |
| A7 时间线/日历/甘特 | 2 timeline + 2 calendar + 1 gantt | B8/只读 | **3/5 组件级实现期降级**（timeline×2、calendar×1） |
| A8 经营看板 | 10 + 1 | 聚合查询 | 后端 11 BizModel 支撑 |
| A9 向导 | 2 | B8 状态机链 | 全手写 page.yaml（layoutControl="wizard" 未实现） |
| A10 关联 drawer | 69 drawer + 38 ref 页 | B3/B4/B7 | 最主流钻取手法 |
| A11 多表联查 | 三单匹配、版本 diff、凭证联查 | B3 | 3 实例 |
| A12 报表 | 24 页 | 只读渲染 | nop-report 接线 |

**关键洞察**：

1. **12 布局中 11 种已有实现实例**，唯一零落地的是 A3 主从分屏（`layoutMode="bottom-detail"` 平台钩子未接线，见 `2026-07-31-1000` §8.3 方案 4）。
2. **复杂度收敛**：30 个核心复杂页面（A+B+C+D）中，A8 看板（11）+ B 特殊页（10）+ A6 看板（3）+ A9 向导（2）占绝大多数，与模式目录「标准 ERP 单据页模板覆盖半数页面」的判断一致（A2 头行/A10 drawer 作为 view.xml 层附属形态另有 60 子表对 + 69 drawer）。
3. **组件级降级是最大实现风险面**：F13 的 7 个页面中 3 个为**实现期**组件降级（crm timeline / cs timeline / crm calendar），2 个为**设计裁决**降级（kanban 列式、矩阵 table）；加上 hr org-chart 共 4 处实现期降级，其中 3 处（crm/cs timeline、crm calendar）未回填范式文档。
4. **交互类复杂度未被既有统计捕获**：24 个 onEvent 视图（行内推算/联动）与 133 个 gen-control 是页面「行为复杂度」的另一半，模式目录（A/B 维度）未完全覆盖。

---

## 6. 设计-实现差距与风险清单

### 6.1 四象限状态总览

| 象限 | 页面 | 说明 |
|---|---|---|
| ✅ 完整落地 | 15 tabs 视图、60 子表对、3 kanban、2 wizard、11 看板、24 报表、35 接线 ref drawer（+3 死文件）、三单匹配/BOM 树/甘特/diff 等 10 特殊页 | F12/F13/F16 主体已交付 |
| ⚠️ 降级实现 | crm/cs timeline（each+tpl）、crm calendar（卡片网格）、hr org-chart（缩进列表）、logistics 追踪（无地图）、合同 diff（无字段级）、aps 甘特（无拖拽） | 多数有文档裁决，3 处文档漂移（见 §6.4） |
| 🅿️ 占位页面 | 16 个：SPC 三件套、expense-claim/gl-distribution/bank-reconciliation 等财务 7、project-pnl/settlement、cost-center、ncr-disposal、asset-stocktake/repair | 7 行 alert，菜单已挂或未挂 |
| ❌ 未实现 | 资产处置 3 步向导、库存盘点 3 阶段流程 UI、crm 转化向导、cs 知识库、portal 客户/供应商门户 | 设计存在（ui-patterns/cpq.md/portal/），实现缺失 |

### 6.2 需要特别关注的页面与风险

1. **period-close-wizard（416 行）**：财务保护区域 + 年度分支 + 反结账预览——任何修改需 `finance/period-close.md` owner doc 支撑，变更走 plan-first
2. **SPC 三件套占位**：后端完整前端缺页，`spc.md` 设计待落地；独立页面落地时可复用质量看板已验证的 echarts markLine 范式
3. **aps 甘特拖拽**：`scheduling.md` §8.3 有未来 spec（`IEtpApsGanttService.dragUpdateOperation`），前端只读；拖拽需独立 PoC（`page-structure-patterns.md` §8.7 明确 Non-Goal）
4. **voucher-by-bill 跨域引用面最大**（9 drawer）：URL/字段契约变更影响面审计优先级最高
5. **notify inbox 客户端 JS 过滤**：`findPage` 全量拉取 + 客户端过滤模式在数据量增长后需后端分页支持（inbox-patterns.md 已列反模式）
6. **A3 主从分屏布局缺口**：`layoutMode="bottom-detail"` 平台钩子未接线（`2026-07-31-1000` §8.3），若未来需要（如银行流水核对）需平台补丁
7. **非标准命名文件**：`party-search/main.picker.page.yaml`（与标准 `picker.page.yaml` 不同）与 `sales/pricing-rule`、`sales/sales-price-list` 等实体目录外同名薄页包装——工具链/审计脚本可能遗漏
8. **死文件清理**：`ref-employee.page.yaml`、`ref-asset.page.yaml`、`ErpMntEquipment/ref-equipment.page.yaml` 三个 ref 页存在但零引用（机制 B 已改走 view.xml 内嵌 tabs），建议清理或接线

### 6.3 文档缺口

| 缺口 | 证据 | 建议 |
|---|---|---|
| notify 域无 ui-patterns.md | `2026-07-19` §2.3；`notify/inbox-patterns.md` 仅覆盖收件箱 | 通知模板管理、偏好设置的页面设计仍无 owner 文档 |
| SPC 独立页交互设计浅 | `quality/spc.md` 偏数据模型，控件层仅分析文档提及 | 落地 SPC 页面前补交互设计（图表下钻/规则配置 UI） |
| F13 组件降级文档漂移 | `non-standard-views-patterns.md` §3.1/§4.1 宣称原生 timeline/calendar 可用，实现证据相反 | 回填三处实现期裁决（crm/cs timeline prop 失败 + crm calendar React #130） |
| 看板实现细节无文档 | `dashboards.md` 只定义指标，`view-and-page-strategy.md` §5 只定义取数范式 | 看板三段式 page.yaml 结构可考虑沉淀为范式（复用度 10+ 页） |
| **page.yaml 层 i18n 空白** | `frontend-ui-roadmap.md` F15：view.xml 层 i18n 已完成（351 文件），但 **page.yaml 无 i18n 机制**（Deferred l10n）——本报告 30 个核心复杂页（向导/看板/收件箱/甘特）恰是 i18n 空白区 | 复杂页硬编码中文文案（title/remark/按钮）未来需 l10n 方案，属横切风险 |
| portal 仅 placeholder | `portal/README.md`（future extension） | 明确基线外，避免误判为缺口 |

### 6.4 文档漂移清单（实现与文档冲突，需人工确认）

1. **crm/cs timeline + crm calendar**：范式文档「原生组件可用」vs 实现「each+tpl / 卡片网格」——最明确的漂移（3 处）
2. **hr org-chart**：`human-resource/ui-patterns.md` 设计 AMIS tree，实现降级缩进列表
3. **aps 甘特**：`aps/ui-patterns.md` 设计含拖拽/右键菜单，实现只读（§8.7 已裁决 Non-Goal，但 ui-patterns 未同步标注）
4. **b2b ASN 五阶段**：`b2b/ui-patterns.md` 若仍写五阶段则为笔误传播（§8.12 已裁决 4 值字典）

---

## 7. 结论与建议

1. **项目复杂页面已高度体系化**：12 布局模式中 11 种有实现实例；30 个真实复杂页面均有设计文档支撑（范式文档 + 域 ui-patterns + 分析文档三层覆盖），「先设计后实现」的工作流在 F12/F13/F16 批次得到完整执行。
2. **复杂度分布符合「少数模式反复组合」判断**：A2 头行单据（60 对 sub-grid-edit）+ A10 drawer（69）+ A8 看板（11）+ A12 报表（24）占复杂页面绝大多数，与 `2026-07-31-1000` 的「标准 ERP 单据页模板」高价值抽象结论互相印证。
3. **风险集中在三类**：① 组件级降级（4 处实现期，其中 3 处文档漂移）；② 占位页面（16 个，SPC 三件套为后端就绪的显著缺口）；③ 高耦合页面（period-close-wizard 保护区域、voucher-by-bill 9 处跨域引用）。
4. **建议后续动作**（按优先级）：
   - 回填 3 处 F13 文档漂移（crm/cs timeline、crm calendar 实现期裁决）
   - 落地 SPC 独立页（后端完整，可复用质量看板 echarts 范式）
   - 沉淀「看板三段式 page.yaml」与「标准单据页」为模板/范式（复用度最高）
   - 审计 voucher-by-bill 跨域引用契约；清理 3 个死 ref 页
   - 明确 16 个占位页面与 4 个未实现设计项的去留（成功/取消/延后）
   - 关注 page.yaml 层 i18n 空白对复杂页的影响（F15 延后项）

---

## 8. 参考文档索引

### 范式 owner docs（docs/design/）
- `page-structure-patterns.md` — F12/F16：tabs 机制 A/B、仪表板、wizard、12 类复杂页范式
- `non-standard-views-patterns.md` — F13：看板/时间线/日历（含拖拽降级裁决与 mutation 契约）
- `child-table-editor-patterns.md` — F4：子表行内编辑，§16 凭证子表变体
- `tree-entity-patterns.md` — F10：树形 CRUD 三件套
- `cross-doc-navigation-patterns.md` — F9：关联单据 drawer
- `voucher-back-link-patterns.md` — 凭证回链双向导航
- `picker-patterns.md` / `batch-operation-patterns.md` / `visible-on-patterns.md` / `query-filter-patterns.md` / `field-formatting-patterns.md` / `status-color-map.md` / `date-ranged-validity-pattern.md`
- `dashboards.md` — 9 域看板指标定义
- `roles-and-permissions.md` — 高危操作权限表（反审核/反结账守卫）
- `app-overview.md` — 主要界面/导航模型/菜单权威源
- `notify/inbox-patterns.md` — 收件箱范式
- `portal/README.md` — 门户（future placeholder）

### 域设计文档（docs/design/<domain>/）
- 18 域 `ui-patterns.md`（复杂页面识别见 §3.1 表）
- 深度页面主题：`finance/period-close.md`、`finance/ui-patterns.md`、`quality/spc.md`、`manufacturing/bom-and-routing.md`、`aps/scheduling.md` §8、`maintenance/equipment-integration.md`、`purchase/three-way-match.md`、`crm/cpq.md` §2、`b2b/edi-formats.md` §7-8

### 架构与分析（docs/architecture/、docs/analysis/）
- `architecture/view-and-page-strategy.md` — 页面策略 + 看板/报表取数范式
- `analysis/2026-07-19-frontend-ui-design-completeness-and-quality-analysis.md` — 设计完备性 vs 实现缺口审计
- `analysis/2026-07-31-1000-complex-page-pattern-catalog.md` — 12 布局 × 8 数据交互模式目录 + view.xml 扩展机制源码实证
- `analysis/2026-07-31-1100-form-layout-dsl-evolution.md` / `1200-page-config-infer-assemble-architecture.md` / `1300-xview-schema-assessment.md` — 页面配置简化探索链
- `analysis/2026-07-20-complex-ui-controls-inventory-for-flux.md` — 复杂控件缺口清单（SPC/Formula 剩余）
- `analysis/gen-control-classification-audit.md`、`analysis/view-button-coverage/`、`analysis/view-form-layout-gap-analysis.md`
- `backlog/frontend-ui-roadmap.md` — F1–F16 路线图（F12/F13/F16 定义）

### 实现文件索引（代表性）
- 向导：`module-finance/erp-fin-web/.../period-close-wizard/main.page.yaml`、`module-maintenance/erp-mnt-web/.../visit-wizard/main.page.yaml`
- 看板：`module-{qa,fin,inv,mfg,prj,pur,sal,ast,mnt,md}-*/.../dashboard/main.page.yaml`、`module-cs/erp-cs-web/.../ErpCsQualityDashboard/main.page.yaml`
- 非标准视图：`module-{prj,cs,crm}-*/.../kanban|timeline|calendar.page.yaml`、`module-hr/.../team-vacation-calendar.page.yaml`
- 特殊页：`module-aps/.../dashboard/schedule-gantt.page.yaml`、`module-mfg/.../dashboard/bom-tree.page.yaml`、`module-pur/.../dashboard/three-way-match.page.yaml`、`module-hr/.../dashboard/{org-chart,payroll-approval}.page.yaml`、`module-ct/.../dashboard/version-diff.page.yaml`、`module-b2b/.../dashboard/{asn-flow,edi-detail}.page.yaml`、`module-drp/.../dashboard/net-requirement.page.yaml`、`module-log/.../dashboard/shipment-tracking.page.yaml`
- 收件箱/联查/picker：`module-notify/.../ErpSysNotification/inbox.page.yaml`、`module-finance/.../ErpFinVoucherBillR/*.page.yaml`、`module-master-data/.../party-search/main.picker.page.yaml`
- 报表：各域 `pages/report/*.page.yaml`（24 页）+ `_vfs/nop/main/report/{domain}/*.xpt.xml`（25 模板）
- ref drawer：各域 `ref-*.page.yaml`（38 页）
