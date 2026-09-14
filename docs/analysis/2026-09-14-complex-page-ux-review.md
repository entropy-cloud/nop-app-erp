# 复杂页面 UX 评审报告：布局 / 弹框 / 控件联动 / 操作便捷性

> 日期：2026-09-14
> 评审对象：全部 368 个保留层 `.view.xml`（非 `_gen`），重点深读 12 个典型复杂页 + 关联子表/ref drawer 页
> 评审维度：布局、弹框宽度与内容、控件联动、操作便捷性
> 性质：审查导向（不引入代码变更），发现按 P1（确认缺陷）/ P2（一致性问题）/ P3（体验优化）分级
> 关联文档：`docs/analysis/2026-08-03-1000-frontend-complex-page-deep-analysis.md`（页面资产盘点）、`docs/analysis/optimize/`（M4 UI 评估条件触发）

---

## 1. 结论先行

**有问题，且分为三类：**

| 级别 | 数量 | 代表问题 |
|------|------|----------|
| **P1 确认缺陷** | 5 | 行级业务动作携带硬编码/空参数（含 `assignedToId=0` 真 bug），用户无法输入业务参数 |
| **P2 一致性问题** | 6 | 弹框尺寸体系混乱（`<dialog>`/`<drawer>` 元素混用、68 个编辑 drawer 无显式尺寸）、状态字段在编辑表单中可改、双查询入口并存 |
| **P3 体验优化** | 7 | 列表列数超 AMIS 虚拟化阈值、行按钮过载（最多 16 个）、头部与行级联动缺失、批量操作覆盖不全 |

正面结论：**控件联动是本次评审中做得最好的维度**——行金额自动计算（数量×单价→金额/税额/含税金额）、借贷方向切换、科目快照驱动辅助核算显隐、状态开关二次确认等都已形成可复用范式；问题集中在**弹框体系一致性与行级业务动作的参数收集**上。

---

## 2. 评审范围与量化基线

### 2.1 深读页面（典型复杂页）

| 页面 | 行数 | 复杂度特征 |
|------|------|-----------|
| `ErpCsTicket`（cs） | 480 | 3 表单 tabs + 子表 + 行动作 10 个 + 2 参数 dialog + 自定义控件 |
| `ErpHrEmployee`（hr） | 389 | 员工档案 5-tab drawer（跨 4 实体 crud）+ 调动参数 drawer |
| `ErpMdMaterial`（md） | 370 | 5 段 tabs + 状态开关确认 + 编码唯一性 blur 校验 + 删除引用阻断 |
| `ErpFinVoucherLine`（fin） | 317 | 17 列凭证子表（借贷切换 + 科目快照 + 6 维辅助核算显隐） |
| `ErpPurInvoice` / `ErpSalInvoice` | 303/290 | 开票-核销流程 + 关联 drawer |
| `ErpMdPartner`（md） | 276 | 状态开关 + 删除引用阻断（物料同构） |
| `ErpMfgWorkOrder`（mfg） | 271 | 5 段 tabs + 进度仪表板 + 行动作 12 个 + 3 关联 drawer |
| `ErpFinVoucher`（fin） | 266 | tabs + 17 列子表 + 平衡 badge + 红冲预览 dialog |
| `ErpPurOrder` / `ErpSalOrder` | 256/230 | 4 段 tabs + 11 列子表 + 审批流 + 批量审批 + 关联 drawer ×3 |
| `ErpPrjProject`（prj） | 226 | 3 段 tabs |
| `ErpApsOperationOrder`（aps） | 176 | 28 列列表 + 排程/开始/完成行动作 |

### 2.2 全局量化（368 个非 gen view）

| 指标 | 数值 | 说明 |
|------|------|------|
| 列表 grid 列数 > 13（AMIS 虚拟化阈值） | **67 页** | 峰值 40 列（ErpLogShipment） |
| `layoutControl="tabs"` 表单 | 15 页 | 全部为业务复杂页 |
| `row-update-button actionType="drawer"` | 68 页 | 均未显式指定 drawer 尺寸 |
| `<drawer size="xl">` | 36 处 | 关联列表抽屉的主流尺寸 |
| `<drawer size="lg">` | 10 处 | 关联凭证等轻量抽屉 |
| `<dialog size="md">` | 7 处 | 参数收集 |
| `<dialog size="sm">` | 2 处 | 单参数/预览 |
| `<form size="lg">` | 426 处 | 表单主流尺寸（lg 是表单/弹框的默认语义） |
| 行动作 ≥ 10 个的页面 | 15 页 | 峰值 16 个（ErpCsTicket） |
| `visibleOn` 使用页 | 58 页 | 状态驱动显隐已普及 |
| `onEvent` 使用页 | 22 页 | 联动实现面较窄 |

---

## 3. 布局问题

### 3.1 列表 grid 列数远超 AMIS 列虚拟化阈值（P3，覆盖面最大）

AMIS 列虚拟化使 ~13 列之外的列不在 DOM 中（本项目已踩坑记录于 `nop-frontend-dev` skill）。67 个页面超过该阈值，最严重：

- `ErpLogShipment`：**40 列**（`module-logistics/erp-log-web/.../ErpLogShipment/ErpLogShipment.view.xml:6`）
- `ErpB2bMftConfig`：29 列；`ErpMdMaterial`：28 列；`ErpApsOperationOrder`：28 列；`ErpDrpLine`：25 列；`ErpCtContract`：23 列；`ErpMdSubject`：23 列
- 典型复杂页也超限：`ErpCsTicket` 15 列、`ErpPrjProject` 14 列、`ErpSalInvoice`/`ErpPurInvoice` 14 列

**影响**：首屏可视列不足一半，用户需要横向滚动才发现关键列（如状态/金额），且状态 tag 等视觉断言在 E2E 中需 scroll 配合（现有测试已被迫处理）。40 列是"字段全部上列表"的堆砌式设计，无列优先级/列分组意识。

### 3.2 编辑态表单暴露状态机管理字段（P2，跨 15 页）

`ErpPurOrder` edit 表单直接可编辑 `paidStatus[付款进度]` `receiveStatus[收货状态]`（`ErpPurOrder.view.xml:123-124`）；`ErpCsTicket` add/edit 表单可编辑 `status[工单状态]` `docStatus[单据状态]` `approveStatus[审核状态]`（`ErpCsTicket.view.xml:114-115,206-207`）；`ErpFinVoucher` edit 可编辑 `docStatus[凭证状态]`（`ErpFinVoucher.view.xml:75`）。

这些字段由状态机/过账引擎派生，手工修改与行级业务动作（提交/审批/过账）语义冲突。应统一为 `readOnly` 或仅展示。

### 3.3 tabs 表单 × drawer 容器：双重"折叠"（P2）

15 个 tabs 表单全部经 `row-update-button actionType="drawer"` 打开（68 页 drawer 编辑的其中一大部分）。编辑一个订单要经历：点"编辑"→ 抽屉弹出 → 切到"明细行"tab → 才看到子表。**弹框 + tab 双重折叠**增加了高频录入路径的点击层级。

### 3.4 子表列数在窄容器中挤压（P2）

凭证行子表 17 列（`ErpFinVoucherLine.view.xml:63` 注释自述"突破 P0 8-12 列基线"）、订单行子表 11 列（`ErpPurOrderLine.view.xml:24`）。这些子表渲染在未指定宽度的编辑 drawer 中，叠加 3.3 的 tab 折叠，行内编辑空间极度有限。

---

## 4. 弹框宽度与内容问题

### 4.1 编辑 drawer 全部走平台默认宽度（P2，68 页）

`<action id="row-update-button" actionType="drawer"/>` 全库 68 处无一显式 `<drawer size="...">`，而内部表单多为 `size="lg"` + tabs + 宽子表。**表单语义 lg 与容器默认宽度不匹配**——表单声明了内容宽度意图，容器没有兑现。反例可对照：关联列表抽屉全部显式 `size="xl"`（36 处），说明项目对"内容宽"的容器是有尺寸意识的，只是编辑抽屉被遗漏。

### 4.2 `<dialog>` / `<drawer>` 元素混用（P2）

`actionType="drawer"` 的按钮下，有的用 `<drawer page=... size=...>`（主流，46 处），有的用 `<dialog page=... size=...>`（如 `ErpHrEmployee.view.xml:401` 完整档案 xl、`ErpFinBudgetScenario.view.xml:198-203` rollForward/carryForward md）。两种写法平台都能渲染，但作为代码契约是分裂的，且 `ErpHrEmployee` 的"完整档案"是 tabs 组合页却用 `<dialog>` 元素描述抽屉语义。

### 4.3 内容-尺寸匹配问题（P3）

- **`ErpCsTicket` escalateQuality（md，5 字段）**：尺寸尚可，但 `materialId` 控件是 `input-number` + placeholder"必填：缺陷物料 ID"（`ErpCsTicket.view.xml:326-330`）——**让用户手输 ID**，违背全库"picker 选物料"的通行范式（对照 `ErpPurOrderLine` 子表 materialId picker）。
- **`ErpFinVoucher` reversePreview（sm，7 行文本）**：预览内容为纯文本行，无表格、无金额对齐（`ErpFinVoucher.view.xml:169-225`），sm 弹框纵向滚动风险；且数据加载用 `data-source` 而非 initApi，弹开瞬间先显示"加载预览中…"闪烁。
- 反面对照良好：`ErpFinBudgetScenario` rollForward/carryForward（md）参数表单是"参数收集"的正确范式（`ErpFinBudgetScenario.view.xml:198-203`），`ErpCsTicket` adoptKnowledge（sm，单 checkbox）尺寸恰当。

---

## 5. 控件联动问题

### 5.1 已建立的优良联动（正面对照，不必改）

| 联动 | 位置 | 机制 |
|------|------|------|
| 数量×单价→金额/税额/含税金额 | `ErpPurOrderLine.view.xml:29-116` | 行内 onEvent.change setValue + ROUND 表达式，三字段相互触发 |
| 借贷方向切换→借方/贷方列显隐+清空 | `ErpFinVoucherLine.view.xml:157+` | button-group-select + visibleOn + clearValueOnHidden |
| 科目选择→8 字段快照→6 维辅助核算显隐 | `ErpFinVoucherLine.view.xml:83-108` | picker onEvent 快照 + 宽松降级表达式 |
| 状态开关→停用二次确认 | `ErpMdMaterial.view.xml:107-145` | switch + dialog 确认 + mutation |
| 编码唯一性 blur 校验 | `ErpMdMaterial.view.xml:176-197` | blur ajax + adapt 转 status |
| 删除引用阻断 | `ErpMdMaterial.view.xml:333-365` | 删除前 countReferences + 条件 dialog |
| 工单主题→KB 建议列表 | `ErpCsTicket.view.xml:133-197` | data-source sendOn（subject≥2 字符） |

### 5.2 缺失的联动（P3，操作摩擦点）

1. **凭证头合计非实时**：`ErpFinVoucher` 头 totalDebit/totalCredit 需点击"刷新合计"按钮才聚合（`ErpFinVoucher.view.xml:125-132` 注释自述实时聚合 DEFERRED），balanceBadge 的"平衡"判断建立在陈旧数据上——录入 3 行后发现不平衡，只能靠 autoBalance 兜底。
2. **币种→汇率不联动**：`ErpPurOrder`/`ErpSalOrder`/`ErpMfgWorkOrder` edit 表单均有 `currencyId + exchangeRate`，但全页无一个 onEvent——换币种不会自动带出汇率，金额也不会按汇率换算提示（`ErpPurOrder.view.xml:88`）。
3. **物料→计量单位不联动**：`ErpPurOrderLine` 子表 materialId 选择后 uoMId 不自动带出（`ErpPurOrderLine.view.xml:27-28`），每行需重复选单位。
4. **行级仓库与头级仓库不联动**：`ErpPurOrder` 头有收货仓库、子表行还有 warehouseId 可改（`ErpPurOrderLine.view.xml:118`），两者无默认继承关系，录入摩擦。
5. **编辑态状态字段可改**（见 §3.2）：状态联动缺失的另一面——可手动改与状态机冲突。

---

## 6. 操作便捷性问题

### 6.1 行级按钮过载（P3，15 页 ≥ 10 个）

峰值：`ErpCsTicket` 16 个、`ErpMfgWorkOrder` 15 个、`ErpHrRecruitment` 14 个、`ErpPrjProject` 13 个、`ErpSalOrder`/`ErpPurOrder`/`ErpMfgSubcontractOrder` 12 个。虽多数有 `visibleOn` 按状态裁剪（实际同时可见 5-8 个），但行内图标按钮排布仍拥挤，且**没有"更多"下拉分组**——高频动作（提交/审批）与低频动作（操作历史/履行进度/关联凭证）同级展示。

### 6.2 行级业务动作硬编码/空参数（**P1，5 处确认缺陷**）

```text
erp-cs-web/.../ErpCsTicket.view.xml:382   assign?ticketId=$id&assignedToId=0
erp-cs-web/.../ErpCsTicket.view.xml:396   resolve?ticketId=$id&resolution=
erp-mfg-web/.../ErpMfgWorkOrder.view.xml:216  reportCompletion?workOrderId=$id&completedQty=0
erp-qa-web/.../ErpQaNonConformance.view.xml:150  resolve?ncrId=$id&resolution=
erp-aps-web/.../ErpApsOperationOrder.view.xml:144  scheduleForward?scheduleId=0
```

逐项核实：

1. **`assign?assignedToId=0`（真 bug）**：后端回落逻辑是 `StringHelper.isEmpty(assignedToId) ? context.getUserId() : assignedToId`（`ErpCsTicketBizModel.java:241`）——"0" 非空，**会把工单分派给用户 ID "0"**（不存在的用户），且前端"分派"按钮没有选人 dialog，服务端注释里"杜绝 ASSIGNED 无主单"的兜底被前端硬编码值击穿。
2. **`resolve?resolution=`（空串）**：解决动作本应收集解决说明（后端 `@Optional resolution`，`ErpCsTicketBizModel.java:268`），前端直接空提交，**操作历史里解决记录无说明**；`ErpQaNonConformance` 同构（`ErpQaNonConformance.view.xml:150`）。
3. **`reportCompletion?completedQty=0`**：完工数量被硬编码为 0（后端把负值/空归零，`ErpMfgWorkOrderReportCompletionProcessor.java:33-35`），**用户无法在列表行直接报实际完工数量**，按钮语义退化为"标记完工"；未达计划量时点了无任何效果。
4. **`scheduleForward?scheduleId=0`**：confirmText 说"排程操作请在排程方案详情页执行"（`ErpApsOperationOrder.view.xml:145`），但确认后仍会带 `scheduleId=0` 调 mutation——**死按钮**，要么去掉，要么引导跳转（参照 PurOrder 的"创建入库单" link 模式）。

**模式根因**：这些动作本应走 §4.3 已建立的"参数 dialog + simple 提交页"范式（对照 `ErpFinBudgetScenario` rollForward、`ErpCsTicket` escalateQuality），但行内直接 `@mutation` 时参数无处收集，于是硬编码占位。

### 6.3 批量操作覆盖不全（P3）

批量审批（pur/sal）与批量启停（md）已建立，但**缺批量驳回、批量撤回、批量作废**——审批流中高频的"反悔/拒绝"场景只能逐行操作。

### 6.4 跨页跳转创建（P3，设计权衡）

"创建入库单/创建出库单"用 `link` 跳到目标列表带 filter（`ErpPurOrder.view.xml:228-232`）——上下文保留是优点，但跳转后用户仍要手动"新增"并等待预填，路径比"一步生成"长；若后端有 `createFromOrder` 类 mutation，用 drawer 参数化生成更顺。属可选优化，非缺陷。

---

## 7. 改进建议（分级）

### P1（确认缺陷，优先修复）

1. **`ErpCsTicket` 分派**：改为 dialog 收集 `assignedToId`（picker 选人），去掉 `=0` 硬编码；同时保留后端空值回落当前用户。
2. **`ErpCsTicket`/`ErpQaNonConformance` 解决**：加 `resolution` 必填 dialog（对照 escalateQuality 范式），空串提交应被表单校验拦截。
3. **`ErpMfgWorkOrder` 报告完工**：加数量输入 dialog（默认带出计划剩余量），移除 `completedQty=0` 硬编码。
4. **`ErpApsOperationOrder` 排程**：改为 link 跳转排程方案页（带筛选），或移除该按钮。

### P2（一致性，随页面重构消化）

5. 68 个编辑 drawer 显式标注尺寸（业务单据类 ≥ `xl`，纯表单类 `lg`），与内部表单语义对齐。
6. 统一 `actionType="drawer"` 下的子元素为 `<drawer>`（清理 `ErpHrEmployee`/`ErpFinBudgetScenario` 的 `<dialog>` 混用）。
7. 编辑态表单中状态机/过账派生字段统一 `readOnly`（status/docStatus/approveStatus/paidStatus/receiveStatus）。
8. 凭证头合计实时聚合（将 deferred 的 onEvent 头聚合落地，或至少让 balanceBadge 跟随 autoBalance 后刷新）。

### P3（体验优化）

9. 超 13 列列表做列裁剪/折叠列/列分组（40 列的 ErpLogShipment 优先）；可考虑"关键列前置 + 次要列进详情"。
10. 行动作按频率分组（高频动作平铺、低频进"更多"下拉）。
11. 补币种→汇率、物料→单位、行仓库→头仓库的联动带出。
12. 批量操作补齐驳回/撤回/作废。
13. `escalateQuality` 的 materialId 改 picker。
14. 编辑器 drawer 内 tabs 表单考虑改"头部字段 + 子表同屏"布局（长表单才用 tabs）。

---

## 8. 附录：量化数据明细

### 8.1 列数 > 13 的列表页（67 页，取前 20）

40 列：ErpLogShipment；29 列：ErpB2bMftConfig；28 列：ErpMdMaterial / ErpB2bMftLog / ErpApsOperationOrder；25 列：ErpDrpLine；23 列：ErpMdSubject / ErpInvDrpCrossDock / ErpCtContract / ErpB2bPartnerProfile；22 列：ErpApsDispatchRule；20 列：ErpInvDrpSafetyStockCalc / ErpCtRebateAgreement / ErpB2bMftCertificate / ErpB2bEdiDoc；19 列：ErpDrpParameter / ErpCtSignatureRequest / ErpApsOpRouting / ErpApsDispatchLog；18 列：ErpMdMaterialSku / ErpLogShipmentLog / ErpLogDeliveryWindow / ErpFinVoucherLine / ErpInvDrpLeadTimeRecord / ErpCtRebateSettlement / ErpB2bTestExchange / ErpB2bAsn …

### 8.2 行动作数 ≥ 10 的页面（15 页）

16：ErpCsTicket；15：ErpMfgWorkOrder；14：ErpHrRecruitment；13：ErpPrjProject；12：ErpSalOrder / ErpPurOrder / ErpMfgSubcontractOrder；11：ErpFinBudgetScenario；10：ErpSalInvoice / ErpSalDelivery / ErpPurReceive / ErpPurInvoice / ErpMntRequest / ErpCrmLead / ErpSalReturn。

### 8.3 tabs 表单页面（15 页）

ErpSalOrder / ErpQaNonConformance / ErpQaInspection / ErpPurOrder / ErpPrjProject / ErpMntEquipment / ErpMfgWorkOrder / ErpInvStockMove / ErpHrEmployee / ErpFinVoucherTemplate / ErpFinVoucher / ErpCtContract / ErpCsTicket / ErpCrmLead / ErpAstAsset。

---

## 9. 参考

- 典型页源码：本报告各节内 file:line 引用
- 页面资产全景：`docs/analysis/2026-08-03-1000-frontend-complex-page-deep-analysis.md`
- 既有 UI 评估路线（条件触发，未启动）：`docs/analysis/optimize/README.md` §M4、`docs/backlog/optimization-audit-roadmap.md`
- AMIS 运行时约束（列虚拟化/FormDialog）：`.opencode/skills/nop-frontend-dev/SKILL.md`