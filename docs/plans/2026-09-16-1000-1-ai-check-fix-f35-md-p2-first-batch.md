# 2026-09-16-1000-1 ai-check F3.5-F3.x md 域 P2 首批（md-001..005 五条）

> Plan Status: draft
> Last Reviewed: 2026-09-16
> Source: `docs/backlog/ai-check-roadmap.md` F3.5-F3.x（各域 P2 簇）；findings `ck-master-data.md`
> Related: plan `2026-09-12-1000-1`（F3.1 playbook 先例）
> Audit: required

## Current Baseline

（HEAD `199831333` 实仓核验，5 条 open P2 全部在位）

- **md-001**（D3/D8）：`ErpMdSupplierApprovalBizModel.findEffectiveByPartner` 无 validFrom/validTo 过滤、无 orderBy → 可能返回过期记录，下游 SupplierEligibilityChecker 误判
- **md-002**（D6）：`resolvePriceWithSource` 缺 supplierPriceResolver 层（resolvePrice 四层 vs WithSource 仅 customer→default）
- **md-003**（D6）：`pickDefaultTierPrice` nullSafe→ZERO：未定价 SKU 返回 0 而非 null（0 元可流入开单路径）
- **md-004**（D6/D2）：`ErpMdCurrencyRefreshRatesFromApiProcessor` validFrom=today, validTo=today+1 闭区间覆盖 2 天 + 绕过互斥校验
- **md-005**（D6）：`resolveConversionRate` 仅查正向（from→to），不尝试反向取倒数（strict 模式直接抛错）

## Goals

- 五条 P2 全部 fixed 终态；每条失败测试先行
- owner docs 对齐

## Non-Goals

- md-002 supplierPriceResolver 补全以外的定价引擎重构
- md-003 严格模式配置键（strict 抛错 vs null 返回的配置化归 Deferred）
- md-003 报表侧 `ErpMdReportBizModel#nz` 0 价展示联动裁决（报表 null 展示为合理语义，不在本批修改范围）
- md-005 双向维护约定归 owner doc 裁决（本批先实现反向换算兜底）

## Task Route

- Type: `implementation-only change`
- Skill: none（复用 F2.13-F3.1 已建立的领域知识与代码模式）

## Infrastructure And Config Prereqs

- md-004 配置门控沿用 `erp-md.exchange-rate-overlap-check-enabled`（如存在）；无其他新增基础设施。

## Execution Plan

### Phase 1 — 五条修复实施

Status: planned
Targets: `ErpMdSupplierApprovalBizModel.java`、`ErpMdMaterialSkuBizModel.java`、`ErpMdCurrencyRefreshRatesFromApiProcessor.java`、`ErpMdUoMConversionBizModel.java`
Skill: `nop-backend-dev`

- [ ] Fix: md-001 findEffectiveByPartner 补日期过滤（null 端 = 开放区间：`(isNull("validFrom") OR le("validFrom", today)) AND (isNull("validTo") OR ge("validTo", today))`）+ `orderBy(validFrom, DESC)` 确定性排序
- [ ] Fix: md-002 resolvePriceWithSource 补 supplierPriceResolver 分支（对齐 resolvePrice 四层优先级）
- [ ] Fix: md-003 pickDefaultTierPrice 无价格档返回 null（不移除 nullSafe 方法，改返回 null 语义 + 调用方 null 检查）
- [ ] Fix: md-004 validTo 改 `today`（单日窗口）+ 刷新路径补 `ErpDateRanges.contains` 互斥校验（自排除本次 upsert 行）+ 存量 validTo=D+1 行由今日刷新覆盖修正
- [ ] Fix: md-005 resolveConversionRate 正向未命中时尝试反向查（取倒数 `ONE.divide(rate, 6, HALF_UP)`，rate>0 守卫；层级优先序：物料级正向→物料级反向→通用级正向→通用级反向）
- [ ] Proof: 先行失败测试 ×5（每 finding 一个测试方法）→ 修复后全绿 + md 既有测试零回归

Exit Criteria:

- [ ] 新测试先红后绿；`mvn test -pl module-master-data/erp-md-service` 全绿

### Phase 2 — 验证 + 收官

Status: planned
- [ ] Proof: 全 reactor BUILD SUCCESS + checker R2c 零漂移 + CJK/i18n PASS
- [ ] Fix: 索引 5 行 fixed + roadmap F3.5 md 子集 done + 日志
- [ ] Fix: git 提交

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（agent `agent_635ed6fb-8f3d-4d11-a93d-c8ffa918aa19`，2026-09-16）——B1（Blocker）md-001 日期列可空需 isNull OR 复合过滤；P1 技能名不实；P2 md-004 存量处置+自排除+配置门控；P3 md-005 rate=0 守卫+层级序+md-003 报表联动。全部修订。

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐
- [ ] 已运行验证
- [ ] 无范围内项目降级
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Closure

Status Note: （待结束审计后填写）

Closure Audit Evidence:

- Auditor / Agent: （待独立子代理）
