# 2026-07-05-1500 补充审计报告 — 与启发式检测工具

> 日期：2026-07-05 15:00
> 前置审计：`2026-07-05-1300-code-vs-design-vs-best-practices-audit.md`
> 性质：对前序审计的独立验证 + 反模式检测工具 + 补充发现
> 方法：独立 grep 验证 + 子代理并行扫描 + 启发式工具运行

---

## 1. 前序审计声明的验证

### 1.1 S3"已修复" — ❌ 不准确

前序审计声称 `dao().getEntityById()` 已修复。启发式工具检测结果：

| 模式 | BizModel中残留 | 说明 |
|------|---------------|------|
| `dao().getEntityById()` | **2处** | `ErpMdSupplierApprovalBizModel:196`、`ErpPurSupplierScorecardBizModel:61` |
| `dao().findAllByQuery()` | **3处** | `ErpCsTicketBizModel:242`、`ErpCsSurveyBizModel:148,156` |

D2 Phase 2 整改计划声称修复 15 个 BizModel 的 48 处，但 **cs 域的 3 处遗漏了**。

### 1.2 S2"46处" — ❌ 严重低估

| 维度 | 前序审计声称 | 工具检测实际 |
|------|-------------|-------------|
| `daoFor(ErpMd*)` in helper层 | 46处 | 28处（Processor/Dispatcher/Engine） |
| BizModel 中 `daoFor(Erp*)` | 未单独统计 | **116处** |
| 全生产代码 `daoFor()` 总量 | 未统计 | **600处** |

前序审计的"46处"只捕获了 master-data 跨域子集。**600处 `daoFor()`** 才是完整暴露面。

### 1.3 `dao().updateEntity` — ❌ 完全遗漏

前序审计 **完全没有** 将 `dao().updateEntity()` 列为独立问题。启发式工具检测到 **48处**，遍布 11 个模块的 BizModel 中。这是比 S2（46处）数量更多、影响更直接的反模式——直接绕过 CrudBizModel 的 `updateEntity` 生命周期管道。

### 1.4 `new Erp*()` 构造实体 — ⚠️ 部分遗漏

前序审计声称"基本修复"。工具检测到 **38处** 生产代码中直接 `new Erp*()`（排除测试和 _gen），未使用 `newEntity()`。

### 1.5 xbiz 编排缺失 — ⚠️ 未被识别为风险

前序审计将"0个 xbiz.xml"记录为"有意偏离（S7）"，但未指出：**32个 Processor 全部缺少 xbiz 接线**。Processor 的 Javadoc 引用了 xbiz 委托模式，但实际无对应文件。这意味着标准审批流的 BizModel→Processor 委托链不完整。

### 1.6 准确确认项

| 声明 | 验证结果 |
|------|---------|
| R4 extends RuntimeException = 0 | ✅ 确认 |
| R5 @Inject private = 0 | ✅ 确认 |
| R7 System.currentTimeMillis() = 0 | ✅ 确认 |
| 跨域核心调用正确实现 | ✅ 确认 |
| 测试质量 9.0/10 | ✅ 确认 |

---

## 2. 启发式检测工具

### 2.1 工具位置

```
docs/audits/nop-compliance-checker.sh
```

### 2.2 使用方法

```bash
# 全量扫描
bash docs/audits/nop-compliance-checker.sh

# 按模块过滤
bash docs/audits/nop-compliance-checker.sh --module sales
bash docs/audits/nop-compliance-checker.sh --module finance
```

### 2.3 规则清单

| 规则 | 严重度 | 检测内容 | 来源文档 |
|------|--------|---------|---------|
| R1a | 🔴 高 | `dao().saveEntity()` 绕过 CrudBizModel | safe-api-reference.md |
| R1b | 🔴 高 | `dao().updateEntity()` 绕过 CrudBizModel | safe-api-reference.md |
| R1c | 🔴 高 | `dao().getEntityById()` 绕过 requireEntity | safe-api-reference.md |
| R1d | 🔴 高 | `dao().findAllByQuery()` 绕过 findList 管道 | safe-api-reference.md |
| R2a | 🔴 高 | BizModel `daoFor(ErpMd*)` 跨 master-data | service-layer.md |
| R2b | 🔴 高 | BizModel `daoFor(Erp*)` 跨域引用 | service-layer.md |
| R2c | 🔴 高 | 全生产代码 `daoFor()` 总量 | service-layer.md |
| R2d | 🔴 高 | Processor/Dispatcher `daoFor(ErpMd*)` | service-layer.md |
| R3 | 🟡 中 | `new Erp*()` 直接构造实体 | safe-api-reference.md |
| R4 | 🟢 低 | `extends RuntimeException` | error-handling.md |
| R5 | 🟡 中 | `@Inject` + `private` | service-layer.md |
| R6 | 🟢 低 | `@Transactional` 在 BizModel 上 | service-layer.md |
| R7 | 🟢 低 | `System.currentTimeMillis()` | common-java-helpers.md |
| R8 | 🔴 高 | Processor 无 xbiz 接线 | service-layer-orchestration.md |
| R9 | 🟡 中 | 反审核行为不一致 | 自定义启发式 |
| R10 | 🟡 中 | `REQUIRES_NEW` 事务 | 自定义启发式 |

### 2.4 本次运行结果

```
规则    描述                                 严重度  命中
------  -----------------------------------  ------  ------
R1a     dao().saveEntity (BizModel)           🔴 高   2
R1b     dao().updateEntity (BizModel)         🔴 高   48
R1c     dao().getEntityById (BizModel)        🔴 高   2
R1d     dao().findAllByQuery (BizModel)       🔴 高   3
R2b     BizModel daoFor(Erp*) 跨域            🔴 高   116
R2c     全生产代码 daoFor() 总量              🔴 高   600
R2d     Processor daoFor(ErpMd*)              🔴 高   28
R3      new Erp*() 构造实体                   🟡 中   38
R8      Processor 无 xbiz 接线               🔴 高   32
R9      反审核 approvedBy 处理不一致          🟡 中   2清除/14未清除
R10     REQUIRES_NEW 事务                    🟡 中   29
```

### 2.5 工具局限性

- **假阳性**：`R10` 的 29 处 `REQUIRES_NEW` 大部分是文档注释（非代码），实际代码仅 `ErpFinVoucherBizModel:42` 一处
- **假阳性**：`R2b` 的 116 处包含同域 `daoFor`（如 purchase 访问 purchase 的行实体），不全是跨域
- **无法检测**：设计-实现语义偏差（如 posting 同步vs异步）、跨 Processor 行为不一致的深层语义
- **依赖文件名约定**： Processor 必须以 `Processor.java` 结尾才能被 R8/R9 命中

---

## 3. 补充发现

### 3.1 R9: doReverseApprove 行为不一致

| 行为 | Processor |
|------|-----------|
| 反审核时**清除** approvedBy/approvedAt | `ErpSalDeliveryProcessor`、`ErpSalReturnProcessor` |
| 反审核时**未清除**（仅改 approveStatus） | `ErpPurReceiveProcessor`、`ErpPurOrderProcessor`、`ErpSalOrderProcessor` 等 14 个 |

设计文档未明确规定此行为，但同类 Processor 之间不一致是一个需要收敛的设计决策。

### 3.2 Posting 同步vs异步偏差

`flow-overview.md` 设计为"审核成功后异步生成凭证"。实际实现为 `REQUIRES_NEW` 同步调用。虽然事务隔离已达成，但未实现时间解耦。

---

## 4. 修正后评分

| 维度 | 前序审计评分 | 本次评估 | 差异原因 |
|------|-------------|---------|---------|
| 服务层合规 | 7.5 | **6.0-6.5** | 48处dao().updateEntity未识别 + S3残留 + 600处daoFor被低估 |
| 测试质量 | 9.0 | **9.0** | 独立验证准确 |
| 综合 | 8.0 | **7.0-7.5** | 结构性问题（32 Processor无xbiz）+ 服务层低估 |

---

## 5. 建议

1. **立即**：将 `nop-compliance-checker.sh` 集成到 CI 或定期审计流程
2. **短期**：修复 R1（55处 BizModel `dao()` 直接调用）+ R1c/R1d 残留（5处）
3. **中期**：收敛 R9（反审核行为一致性决策）+ 评估 R8（xbiz 接线策略）
4. **长期**：逐步将 600 处 `daoFor()` 收敛到 I*Biz 注入模式
