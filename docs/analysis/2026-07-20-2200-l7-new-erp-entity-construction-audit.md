# L-7 审计报告：`new Erp*()` 直接构造实体审计

> Plan: `docs/plans/2026-07-20-2200-1-audit-findings-remediation.md`（L-7 Fix）
> Source Audit: `docs/audits/2026-07-20-independent-multi-dimensional-audit.md`（L-7 代码质量）
> Methodology: 全仓库 grep + Python 分类（实体 vs 非实体）+ Revert Pattern 评估
> Scope: 审计完成；改造决策为保留 + 已有注释确认（不改造）

## 总览

合规检查器 R3 报 18 处 `new Erp*()` 在生产代码（非测试）。本审计将 18 处分类：

| 类型 | 数量 | 合法性 | 处置 |
|------|------|--------|------|
| 非实体（POJO/DTO/Engine） | 14 | 合法（非 OrmEntity） | 保留，无需改造 |
| OrmEntity + Revert Pattern（纯函数引擎） | 4 | 合法（`nop-backend-dev` skill §ORM 实体构造反转模式） | 保留，已有注释确认 |
| OrmEntity + 应改造为 newEntity() | 0 | — | 无 |

**结论**：18/18 处合法，**不需要任何改造**。

## 14 处非实体 `new` 调用（合法）

| 文件 | 类 | 类型 |
|------|-----|------|
| `ErpApsAtpCtpServiceImpl.java:81,164` | `ErpApsSchedulingEngine` | Engine（无 IoC） |
| `ErpApsSchedulingProcessor.java:235` | `ErpApsSchedulingEngine` | Engine |
| `ErpCrmQuotaBizModel.java:90` | `ErpCrmTerritoryPipeline` | POJO 计算器 |
| `ErpCrmQuotaBizModel.java:94,100,108` | `ErpCrmTerritoryPipeline.QuotaSummary` 等 | 内部 DTO |
| `QuotaRollupCalculator.java:164` | `ErpCrmPipelineAccumulator` | POJO 累加器 |
| `ErpFinPostingExceptionBizModel.java:183,190,197,203` | `ErpFinPostingMetricsSnapshot.MetricValue` | 内部 DTO |
| `NcrLifecycleService.java:122` | `ErpQaActionImpl` | DTO 实现类 |

**理由**：这些类不扩展 `_Erp*`（不是 OrmEntity），不存在 `newEntity()` 工厂方法。直接 `new` 是 Java 标准做法。

## 4 处 OrmEntity + Revert Pattern（合法 + 已注释）

| 文件 | 类 | 用途 | 注释位置 |
|------|-----|------|----------|
| `QuotaRollupCalculator.java:105` | `ErpCrmQuota` | 虚拟聚合行（不持久化） | line 101 `// 构造虚拟聚合行（不持久化）反映聚合结果` |
| `FunnelAggregationEngine.java:272` | `ErpCrmFunnelStageMetrics` | 计算快照值对象（调用方持久化） | line 269-271 多行注释解释 |
| `GapAnalysisCalculator.java:90` | `ErpHrGapAnalysis` | 间隙分析结果（条件性持久化） | line 85-91 `newGapEntity()` helper（daoProvider 可用时走 newEntity，否则 fallback new） |
| `ErpSalPricingRuleEngine.java:206` | `ErpSalOrderLine` | 赠品行评估快照（调用方持久化） | line 204-205 `// 纯函数式引擎：赠品行作为评估快照返回` |

**理由**：这 4 处符合 `nop-backend-dev` skill 的 §ORM 实体构造反转模式（Revert Pattern Awareness）：

> 如果你重构一个**纯函数组件**（无 IoC/DB 依赖），将其改为依赖注入的 ORM 实体构造方式（`new ErpXxx()` → `daoProvider().daoFor(...).newEntity()`），需要特别谨慎：
> - 组件创建实体后直接返回，不持久化 → **保持 `new XxxEntity()`**
> - 组件内部完成全部计算，无外部依赖 → **保持纯函数**
> - 组件被多个 BizModel/Service 通过 `new` 直接调用 → **保持构造函数兼容**

**强制改为 `newEntity()` 的负面影响**：

1. 所有调用方需改为容器注入 → 改动范围大
2. 测试不能再 `new Engine()` → 必须上容器
3. 无持久化行为的引擎内 `newEntity()` 无意义（newEntity 主要价值是触发 OrmEntity 的内部状态注册）

## 复现命令

```bash
grep -rn "new Erp[A-Z]" --include="*.java" module-*/erp-*-service/src/main 2>/dev/null \
  | grep -v target | grep -v "/test/" | grep -v "_gen"
```

## 后续行动

- **不需要任何代码改造**
- **不需要补注释**（4 处 OrmEntity 全部已有注释解释）
- 复查合规检查器 R3 的命中数：当前 18 处全部合法，可作为基线快照。若未来 R3 命中数上升且新增项不是 Revert Pattern，则需重新审计
