# Plan: BizModel @SingleSession 注解清理

## 问题

49 个 BizModel 的 166 个 `@BizMutation` 方法上叠加了 `@SingleSession` 注解。根据 docs-for-ai：

- `service-layer.md:231`：BizModel 管道已自动提供 ORM Session，不需要 `@SingleSession`
- `implement-complex-business-flow.md:117`：`@SingleSession` 应钉在 Processor 编排方法上（而非 BizModel）

项目中所有 Processor/编排类也**没有** `@SingleSession`（grep 确认零 code-level 出现，仅 javadoc 提及）。这意味着当前 `@SingleSession` 实际锚点在 BizModel 方法上，但从未按 doc 模式迁移到 Processor。

验证工具 `tools/check-bizmodel-annotations.mjs` 已就绪，可精确识别每处违规。

## 范围

| 项目 | 值 |
|------|-----|
| 影响模块 | 9 个：aps, assets, b2b, contract, finance, hr, inventory, logistics, projects |
| 涉及文件 | 49 个 BizModel Java 文件，约 166 处 `@SingleSession` 移除 |
| 不涉及 | 已知例外（`ErpFinVoucherBizModel` 的 `@Transactional(REQUIRES_NEW)` 已加 `nop-check: allow` 标记） |
| 增量 | 约 -166 行（`@SingleSession` 注解行 + import 行），零行为变化 |

## 分析

### 为什么可以安全移除

1. **`@BizMutation` 管道已提供 ORM Session**。GraphQL 引擎在请求入口打开 ORM Session，事务在 `@BizMutation` 切面包裹。`@SingleSession` 在这里的作用域与现有请求 Session 重叠，是重复声明。

2. **Processor 没有 `@SingleSession`**。grep 确认 0 个 Processor 类有 code-level `@SingleSession`。Processor javadoc 写"跟随 Facade @BizMutation+@SingleSession"，但实际运行中只要 `@BizMutation` 提供 Session，Processor 不需要额外注解。

3. `@SingleSession` 的原始意图是"控制 Session 刷新时机"——在编排方法返回时 flush。但 `@BizMutation` 事务提交时也会自动 flush。对于已在事务中的简单状态迁移，移除 `@SingleSession` 语义等价。

4. **测试验证门控**：全量 `mvn test` + E2E 作为回归屏障。如有任何测试因缺 Session 而失败（理论上不可能，因为 `@BizMutation` 提供 Session），可逐案回溯。

### 不需要移到 Processor

Processor 始终在 `@BizMutation` 上下文中被调用，不存在"Processor 自行编排时需要独立 Session"的场景。唯一的 `@SingleSession` 合法场景是**非 BizModel 入口**（定时任务、独立 service bean）——此时需在非 BizModel 方法上加 `@SingleSession`。本项目所有业务入口都经过 `@BizMutation`，故不需要。

## 执行步骤

### Step 1：自动移除 annotation + import

对每个违规文件：
1. 删除方法注解块中的 `@SingleSession` 行
2. 如果文件不再引用 `@SingleSession`，删除 `import io.nop.api.core.annotations.orm.SingleSession;`

可以使用批量脚本（find + rg + sed），但每个方法周围的空白和 import 最后一行需要小心。

可以用以下模式批处理（对每个文件）：

```bash
# 对每个文件移除 @SingleSession 注解行（在方法注解块中）
sed -i '' '/@SingleSession/d' "$file"

# 检查是否仍有引用，否则移除 import
if ! grep -q 'SingleSession' "$file"; then
  sed -i '' '/import io.nop.api.core.annotations.orm.SingleSession;/d' "$file"
fi
```

注意：
- `sed` 会移除所有 `@SingleSession` 行，包括 javadoc 中出现的——但 grep 确认 Processor javadoc 中出现的 `@SingleSession` 在**Processor 文件**中，不在 49 个 BizModel 文件中；BizModel 文件中的 `@SingleSession` 都是注解行
- 执行前用 `node tools/check-bizmodel-annotations.mjs` 建立基线
- 执行后用同一工具确认 166→0

也可以手写 .mjs 脚本更精确地只删除注解行（不碰注释）。

### Step 2：构建验证

```bash
mvn clean install -DskipTests
```

确认 154 模块 BUILD SUCCESS。

### Step 3：JUnit 回归

```bash
mvn test
```

关注关联模块：
- `module-assets/erp-ast-service`
- `module-b2b/erp-b2b-service`
- `module-contract/erp-ct-service`
- `module-finance/erp-fin-service`
- `module-hr/erp-hr-service`
- `module-inventory/erp-inv-service`
- `module-logistics/erp-log-service`
- `module-projects/erp-prj-service`
- `module-aps/erp-aps-service`

如任何测试失败 → 确认是否因移除 `@SingleSession` 导致。理论上不应发生（`@BizMutation` 已提供 Session），但如有，需检查该 BizModel 方法是否被**非 BizModel 上下文**（如定时任务）直接调用。

### Step 4：E2E 回归

```bash
npx playwright test tests/e2e/business-actions/ --workers=1
npx playwright test tests/e2e/orchestration/ --workers=1
```

### Step 5：清理 Processor javadoc

Processor javadoc 中提及"跟随 Facade @BizMutation+@SingleSession"的行应更新为"跟随 Facade @BizMutation"：

- `module-assets/.../ErpAstDepreciationScheduleProcessor.java:39`
- `module-finance/.../ErpFinAccountingPeriodProcessor.java:63`
- `module-finance/.../ErpFinNotesReceivableProcessor.java:32`
- `module-finance/.../ErpFinNotesPayableProcessor.java:30`
- `module-finance/.../ErpFinBadDebtProcessor.java:46`

## 退出标准

- [ ] `node tools/check-bizmodel-annotations.mjs` 输出 0 violations
- [ ] `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS
- [ ] `mvn test` 全 reactor 0 failures
- [ ] E2E business-actions + orchestration 全绿
- [ ] 所有 Processor javadoc 中"@BizMutation+@SingleSession"更新为"@BizMutation"
- [ ] check 脚本已纳入预提交流程（可选，推荐）

## 预提交集成

在 `.opencode/` 或 `scripts/` 中添加 git pre-commit hook：

```bash
#!/bin/bash
# .git/hooks/pre-commit
cd "$(git rev-parse --show-toplevel)/tools"
node check-bizmodel-annotations.mjs || exit 1
```

或用 `pnpm --prefix tools check` 作为 CI 步骤。

## 风险

| 风险 | 概率 | 应对 |
|------|------|------|
| 某方法被非 BizModel 上下文直接调用，缺 `@SingleSession` 报错 | 低 | `mvn test` 可捕获；回溯后给该方法独立保留 `@SingleSession` 或给调用方加 |
| `sed` 批量处理误删 javadoc 中的 `{@code @SingleSession}` | 中 | 但 49 个 BizModel 文件中 javadoc 引用 `{@code @SingleSession}` 的只有 finance 几个文件——这些应改为 `{@code @BizMutation}`；建议用 .mjs 脚本精确定位注解行而非整行删除 |
| Processor javadoc 5 处提到"@BizMutation+@SingleSession"但 `@SingleSession` 实际在 BizModel 上——移除后 javadoc 过时 | 中 | Step 5 单独处理，不依赖批量脚本 |
