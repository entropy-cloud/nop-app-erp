# APS 约束排产深化设计（Constraint-Based Planning）

## 定位

本文基于 frePPLe（开源 APS 标杆）调研，设计 nop-app-erp **aps 域排产引擎的深化方向**：从「贪心启发式填充」向「约束理论（TOC）瓶颈驱动 + 多约束求解」演进的设计蓝图。**当前阶段只产出设计与分析，不进入编码状态**（2026-08-12 用户指示）；既有贪心引擎保持不变。

## 来源与背景

- 参考报告：`docs/analysis/erp-survey/2026-08-12-0000-frepple.md`（C++ 约束求解器 + Django 业务层 + TOC 拉动式 + ML 预测 + erpconnection 对接层）。
- 现状基线：`docs/design/aps/scheduling.md` —— 前向/后向贪心启发式（`ErpApsSchedulingEngine`），MAINTENANCE 单约束 + capacity=1，工作中心班次日历未展开；ILP/CP 优化求解为既有 Non-Goal。
- 前置：MRP/DRP 仿真引擎已落地（deepening B1，`simulation-engine.md`）——预测/场景化输入链路已存在。

## 现状 vs frePPLe 对照

| 维度 | nop 现状 | frePPLe | 差距 |
|------|---------|---------|------|
| 求解方法 | 贪心启发式（前/后向填充） | C++ 约束求解器 + TOC 瓶颈驱动 | 无瓶颈识别/拉动式视角 |
| 约束类型 | MAINTENANCE 单约束、capacity=1 | 多约束（产能/物料/时序） | PERSONNEL/TOOL/capacity>1 未启用 |
| 日历 | 未展开班次/节假日 | 完整日历 | 工作中心班次日历展开 follow-up |
| 预测衔接 | MRP/DRP 仿真（场景化输入） | ML 预测（mlforecast）→ solver 输入 | 预测到排产的接口设计 |
| 结果指标 | 排程时间 + 可行性 | metrics + reportmanager（交付率/利用率/缺料） | 排产 KPI 可视化 |
| ERP 对接 | 域间 I*Biz 调用 | erpconnection/odoo 双同步适配 | 跨系统计划同步 SPI |

## 设计要点

### 1. 求解引擎与业务层分离（参考 frePPLe 双层架构）

- **目标**：`ErpApsSchedulingEngine` 保持纯算法 POJO（无 ORM 依赖，现状已如此），把约束建模（`ErpApsConstraint` 扩展）与求解策略分离：
  - `IApsSchedulingSolver` 接口（贪心实现保留为默认）+ 可插拔求解器（约束传播/瓶颈启发式），经 bean 注册与 config 切换（对齐 D3 子计算器注入范式）。
- **不引入** C++/ILP 求解器依赖；约束传播在 Java 内实现（容量约束 + 时序约束），避免外部求解器运维成本。

### 2. 瓶颈识别与拉动式排产（TOC，frePPLe 核心借鉴）

- **设计**：新增瓶颈识别步骤——扫描 horizon 内各工作中心负荷率（`CrpLoadCalculator` 已有负荷率派生链，见 `2026-07-17-2010-1`），识别负荷率超过阈值的瓶颈中心；排产时**先排瓶颈中心**（拉动式：瓶颈计划作为后工序的起点约束），再排非瓶颈中心（前向/后向兜底）。
- **与现有排产的关系**：作为 `scheduleForward/scheduleBackward` 之外的新模式（`scheduleToc`），不改变既有模式行为（向后兼容）。
- **产出**：`SchedulingResult` 扩展瓶颈清单（bottleneckMachineIds + 各中心负荷率），供看板/甘特图消费（对接 `dashboards.md`）。

### 3. 多约束扩展（对照 frePPLe 约束求解）

- `ErpApsConstraint` 约束类型字典已预留 PERSONNEL/TOOL 值（scheduling.md 实现约定）；**capacity>1 并联产能字段在 `ErpMfgWorkcenter.capacity` 与 `ErpMfgWorkcenterCapacity`**（workcenter 配置链，既有）。设计补齐：
  - PERSONNEL：约束=人员数量×班次时长；TOOL：约束=工具数量。
  - capacity>1 并联排产：同工序多机并行的时间槽分配（同开工/完工时间约束），产能读取复用 `ErpMfgWorkcenter`/`ErpMfgWorkcenterCapacity`。
- **触发条件**（roadmap E3.4 门控）：真实多约束排产业务需求出现（非当前默认路径）。

### 4. 预测→排产衔接（参考 frePPLe forecast→solver 输入 + InvenTree ML 集成模式）

- **现状**：MRP/DRP 仿真引擎已支持场景化输入（`simulation-engine.md`）；aps 排产目前由 WorkOrder/OperationOrder 驱动。
- **设计**：定义 `IApsDemandSourceProvider` SPI（与既有 `IErpApsLoadSourceProvider` 同构）——排产输入可来自 (a) 已审批工单（现状）/ (b) 仿真场景的预测需求（`ErpMfgMrpScenarioVersion` 转正的正式计划行）。衔接点=`promoteToFormalPlan` 产物作为排产候选。
- **ML 需求预测（InvenTree machine 集成模式 / frePPLe mlforecast 对照）**：ML 预测作为 `IApsDemandSourceProvider` 的可选实现（SPI 注入，对齐 frePPLe forecast→solver 输入链），不内建训练/推理框架；预测模型接入触发条件=业务方明确需求。
- **此设计保持抽象层，不实现**（跨域耦合 + 触发条件驱动）。

### 5. 排产 KPI 可视化（参考 frePPLe metrics/reportmanager）

- 排产结果指标（计划交付率/中心利用率/缺料清单）经 `SchedulingResult` 暴露，接入看板子系统（`dashboards.md` echarts 范式）；与既有 CRP 负荷图（`crpLoadChart`）共享负荷率派生链。
- **设计先行，实现归前端/看板计划**（F16 类 follow-up）。

## 落地策略（分阶段）

| 阶段 | 内容 | 状态 |
|------|------|------|
| 设计 | 本文档（求解器分离 + TOC 瓶颈 + 多约束 + 预测衔接 + KPI） | ✅ 已完成（本批次） |
| 试点实现 | `IApsSchedulingSolver` 分离 + 瓶颈识别（`scheduleToc`） | todo（roadmap E3.4，plan-first；默认贪心保持） |
| 深化实现 | 多约束扩展 / 预测→排产衔接 / 排产 KPI 看板 | todo（触发条件驱动） |

## 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|----------|
| AP-1 | 重写既有贪心引擎（破坏已验证排产链） | 新增模式/求解器接口，默认行为不变 |
| AP-2 | 引入外部求解器（C++/ILP）增加运维成本 | Java 内约束传播实现 |
| AP-3 | 瓶颈识别与 CRP 负荷链重复计算 | 复用 `CrpLoadCalculator` 派生链 |
| AP-4 | 排产引擎直接依赖仿真/预测实体 | 经 `IApsDemandSourceProvider` SPI 解耦 |
| AP-5 | 未触发需求先实现多约束 | 触发条件门控（默认 MAINTENANCE 单约束保持） |

## 相关文档

- `docs/analysis/erp-survey/2026-08-12-0000-frepple.md` — 参考报告
- `docs/design/aps/scheduling.md` — 既有排产算法（实现基线）
- `docs/design/manufacturing/simulation-engine.md` — MRP/DRP 仿真（预测衔接输入）
- `docs/design/dashboards.md` — 看板子系统（KPI 消费）
- `docs/backlog/erp-enhancement-roadmap.md` — 本主题 roadmap