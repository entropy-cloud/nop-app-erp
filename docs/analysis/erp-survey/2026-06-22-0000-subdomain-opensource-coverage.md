---
调研日期: 2026-06-22
来源: ~/sources/erp（13 个项目）+ GitHub/Gitee 补充
状态: 已完成（基于源码实测）
---

# 新增子域开源参考覆盖分析

> 本文档分析 nop-app-erp 新增的 5 个子域（assets/manufacturing/projects/maintenance/quality）在现有开源项目中的模块覆盖情况，识别缺口，并给出补充建议。

## 覆盖总览

| 子域 | 已覆盖项目数 | 满足 5+ | 主要参考项目 |
|------|-------------|---------|-------------|
| **assets（固定资产）** | 3 | ❌ | ERPNext, iDempiere, Dolibarr |
| **manufacturing（制造）** | 5 | ✅ | Odoo, ERPNext, Metasfresh, Dolibarr, WMES |
| **projects（项目）** | 4 | ❌ | Odoo, ERPNext, iDempiere, Dolibarr |
| **maintenance（设备维护）** | 3 | ❌ | Odoo, ERPNext, WMES |
| **quality（质量）** | 3 | ❌ | ERPNext, Metasfresh, WMES |

---

## 1. Assets（固定资产）— 覆盖详情

### 已有模块的项目（3 个）

| 项目 | 模块路径 | 核心实体 | 参考价值 |
|------|----------|----------|----------|
| **ERPNext** | `erpnext/assets/` | Asset, AssetCategory, AssetDepreciationSchedule, AssetFinanceBook, AssetMovement, AssetRepair, AssetValueAdjustment, AssetCapitalization | ⭐⭐⭐ 最完整：资产全生命周期 + 折旧 + 维护 + 资本化 |
| **iDempiere** | `org.adempiere.base/.../model/I_A_Asset.java` | A_Asset, A_Asset_Acct, A_Asset_Addition, A_Asset_Change, A_Asset_Class, A_Asset_Disposed, A_Asset_Group | ⭐⭐ 完整：资产+科目映射+处置+变动 |
| **Dolibarr** | `dolibarr/htdocs/asset/` | card, depreciation, depreciation_options, disposal | ⭐⭐ 完整：卡片+折旧+处置 |

### 缺口分析

缺少的参考维度：
- **折旧方法实现细节**（直线法/双倍余额递减/工作量法的具体计算逻辑）
- **资产与财务凭证的集成模式**（折旧凭证生成触发机制）
- **资产类别科目映射配置**

### 补充建议

需从 GitHub 下载：
1. **Yu-FAMS-SpringBoot**（github.com/watch-rain7/Yu-FAMS-SpringBoot）— 固定资产管理系统，SpringBoot+Vue
2. **assetsmgr**（github.com/algernonking/assetsmgr）— 固定资产设备管理系统

---

## 2. Manufacturing（制造）— 覆盖详情

### 已有模块的项目（5 个）

| 项目 | 模块路径 | 核心实体 | 参考价值 |
|------|----------|----------|----------|
| **Odoo** | `odoo/addons/mrp/` + 11 个关联 addon | mrp.bom, mrp.production, mrp.workorder, mrp.workcenter, mrp.bom.line, mrp.bom.byproduct | ⭐⭐⭐ 行业标杆：BOM/工单/工序/排产/成本最完整 |
| **ERPNext** | `erpnext/manufacturing/` | BOM, WorkOrder, JobCard, Operation, Workstation, ProductionPlan, Routing, DowntimeEntry | ⭐⭐⭐ 完整：BOM+工单+作业卡+工艺+停机 |
| **Metasfresh** | `backend/de.metas.manufacturing/` | 完整制造套件（Java 源码+SQL迁移+Web UI） | ⭐⭐ 现代化 Java 实现 |
| **Dolibarr** | `dolibarr/htdocs/mrp/` | mo_card, mo_list, mo_production, mo_movements | ⭐ 轻量 MRP |
| **WMES** | `MESManage/` | PlanEntity, PsplanEntity | ⭐ MES 执行层 |

### 缺口分析

当前已满足 5 个项目，无需补充。但可考虑增加：
- **qcadoo MES**（开源 MES，AGPL 许可）— 更专业的制造执行
- **Carbon ERP**（开源 ERP+MES+QMS）— 制造+质量一体化

---

## 3. Projects（项目）— 覆盖详情

### 已有模块的项目（4 个）

| 项目 | 模块路径 | 核心实体 | 参考价值 |
|------|----------|----------|----------|
| **Odoo** | `odoo/addons/project/` + 11 个关联 addon | project, project.task, project.timesheet, project.update | ⭐⭐⭐ 最完整：项目+任务+工时+甘特图+费用 |
| **ERPNext** | `erpnext/projects/` | Project, Task, Timesheet, TimesheetDetail, ActivityCost, ActivityType, DependentTask | ⭐⭐⭐ 完整：项目+任务+工时+成本+依赖 |
| **iDempiere** | `org.adempiere.base/.../model/` | PA_Project（通过代码引用） | ⭐ 部分：项目作为辅助核算维度 |
| **Dolibarr** | `dolibarr/htdocs/projet/` | card, tasks, activity, ganttview, stats | ⭐⭐ 完整：项目+任务+甘特图+统计 |

### 缺口分析

缺少的参考维度：
- **工时计入成本的触发机制**（工时提交→财务凭证）
- **项目作为辅助核算维度**的凭证集成模式

### 补充建议

需从 GitHub 下载：
1. **redragon-erp** 已有项目主数据（MdProject），但不是完整项目管理模块
2. 可考虑下载 **kanass**（github.com/tiklab-project/tiklab-kanass）— 开源项目管理工具

---

## 4. Maintenance（设备维护）— 覆盖详情

### 已有模块的项目（3 个）

| 项目 | 模块路径 | 核心实体 | 参考价值 |
|------|----------|----------|----------|
| **Odoo** | `odoo/addons/maintenance/` | maintenance.request, maintenance.team, maintenance.equipment, maintenance.stage | ⭐⭐⭐ 完整：设备+工单+团队+预防性维护 |
| **ERPNext** | `erpnext/maintenance/` + `erpnext/assets/doctype/asset_maintenance/` | MaintenanceSchedule, MaintenanceVisit, AssetMaintenance, AssetMaintenanceTask | ⭐⭐⭐ 完整：维护计划+访问+资产维护 |
| **WMES** | `FMmanager/` | Equipmentmaintenancerecord, Equipmentrepairrecord, Repairorder | ⭐⭐ MES 设备维护 |

### 缺口分析

缺少的参考维度：
- **预防性维护计划**（周期性维护调度）
- **维护工单与资产的关联**
- **维护成本归集**

### 补充建议

需从 GitHub 下载：
1. **Grashjs/cmms**（github.com/grashjs/cmms）— Atlas CMMS，自托管 CMMS
2. **SuperCMMS**（github.com/SuperCMMS/Open-Source-CMMS）— 开源 CMMS

---

## 5. Quality（质量）— 覆盖详情

### 已有模块的项目（3 个）

| 项目 | 模块路径 | 核心实体 | 参考价值 |
|------|----------|----------|----------|
| **ERPNext** | `erpnext/quality_management/` + `erpnext/stock/doctype/quality_inspection/` | QualityInspection, QualityInspectionParameter, QualityGoal, QualityProcedure, QualityReview, NonConformance | ⭐⭐⭐ 最完整：质检+质量管理+不合格品+目标+评审 |
| **Metasfresh** | `backend/de.metas.qualitymgmt/` | 质量管理模块（Java 源码） | ⭐⭐ 现代化 Java 实现 |
| **WMES** | `MESManage/` | QualityorderEntity, QualityorderController | ⭐⭐ MES 质检 |

### 缺口分析

缺少的参考维度：
- **来料质检/过程质检/完工质检**的触发时机
- **质检不合格品处理**（退货/让步/报废）
- **质检标准/检验项目配置**

### 补补建议

需从 GitHub 下载：
1. **qcadoo MES**（github.com/qcadoo/mes）— 开源 MES，含质量管理模块
2. **MES-SpringBoot**（github.com/wangziyang6/MES-SpringBoot）— 含质检项维护

---

## 综合补充下载清单

| 项目 | GitHub 地址 | 相关子域 | 优先级 |
|------|------------|----------|--------|
| **Yu-FAMS-SpringBoot** | github.com/watch-rain7/Yu-FAMS-SpringBoot | assets | 高 |
| **assetsmgr** | github.com/algernonking/assetsmgr | assets | 高 |
| **Grashjs/cmms** | github.com/grashjs/cmms | maintenance | 高 |
| **SuperCMMS** | github.com/SuperCMMS/Open-Source-CMMS | maintenance | 高 |
| **qcadoo MES** | github.com/qcadoo/mes | manufacturing, quality | 中 |
| **MES-SpringBoot** | github.com/wangziyang6/MES-SpringBoot | quality, manufacturing | 中 |

## 对各子域设计文档的建议

每个子域的 README.md 应增加"开源参考"节，列出参考的项目及具体借鉴点：

```markdown
## 开源参考

| 项目 | 参考维度 | 具体借鉴 |
|------|----------|----------|
| ERPNext | 资产全生命周期 | AssetCategory 科目映射、DepreciationSchedule 生成逻辑 |
| iDempiere | 资产科目映射 | A_Asset_Acct 多套科目表支持 |
| Dolibarr | 折旧计算 | depreciation.php 的折旧方法实现 |
```
