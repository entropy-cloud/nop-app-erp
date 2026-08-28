# State Machine 矩阵覆盖审计报告（M5.1 终态）

> **来源**：`entity-state-machine-migration-roadmap.md` §M5.1（todo 已 done）
> **执行 plan**：`docs/plans/2026-08-28-1607-1-entity-state-machine-m5-1-matrix-audit.md`（status: draft，**M5.1 D1/D2/D3 落地后**将由独立子代理 closure-audit 复核）
> **工具**：`docs/audits/scripts/state-machine-coverage-check.py`（一文件运行 4 维度对账）
> **工具输出**：`/tmp/m5-1-final.json` + `/tmp/m5-1-final.md`（commit 前可重跑：`python3 docs/audits/scripts/state-machine-coverage-check.py --md-output <path>`）
> **审计日期**：2026-08-28
> **审计范围**：`module-*/erp-*-service/src/main/java/app/erp/*/service/statemachine/Erp*StateMachine.java`

## §1 摘要

| 指标 | 数值 |
|---|---|
| **Beans 扫描总数** | **105** |
| Findings 总数 | **0** |
| 待处理 Findings（去白名单后） | **0** |
| 白名单命中 | **0**（无已知误报需要裁决） |
| 工具退出码 | **0**（全部通过） |
| writer 索引构建耗时 | 1.9 秒（一次扫全 3796 个 main/java 文件） |
| 工具执行总耗时 | < 5 秒 |

**核心结论**：entity-state-machine-migration mission M2/M3/M4 三个里程碑迁移的 **105 个 Erp*StateMachine Bean 在 4 维度对账（状态可达性 / 终态出边 / 重复冲突边 / dict-writer 一致性）下全部通过**。无 P0/P1/P2/P3 异常，无须新增白名单，无须后续 Fix plan 闭环。

## §2 4 维度对账结论

### 2.1 状态可达性 ✓ PASS

- **方法**：从每个 Bean 声明的 `initial(...)` 状态出发，BFS 遍历所有 `transition(...)` 边，检查可达闭包是否覆盖所有声明状态。
- **结果**：105 个 Bean 全部声明的 from/to state 集合均处于初始状态的可达闭包内。
- **意义**：无「孤立状态」「死路径」—— 每个声明的状态在运行时都至少有一种合法到达方式。
- **样本检查**（spot-check）：`ErpInvOwnershipTransferStateMachine`、`ErpAstDisposalApprovalStateMachine`、`ErpCtRebateSettlementStateMachine`（M4.65 最后一批）均通过。

### 2.2 终态出边 ✓ PASS

- **方法**：检查 `terminal(...)` 声明的状态是否有出边。
- **结果**：105 个 Bean 全部 `terminal(...)` 状态无任何出边（terminal 不可达反向转移）。
- **意义**：无「终态可逆」—— 终态语义保持 M5.1 闭环纪律。
- **已知协商例外（不构成 finding）**：
  - `ErpCtRebateSettlementStateMachine` 的 CANCELLED dict 值是 **intentional reserved 死状态**（详见 plan `2026-08-14-2000-1`），无需写入 Bean。本审计工具检测到的"CANNOT 进入 CANCELLED"未列入 finding（无 transition 提及 = 不在 Bean 控制范围）。
  - `ErpAstSplitApprovalStateMachine` / `ErpAstMergeApprovalStateMachine` 的 reverseApprove 是**名义边**（运行时 per-mutation 无条件抛错 `ERR_AST_{SPLIT,MERGE}_REVERSE_NOT_SUPPORTED`，不可逆契约 owner doc `split-merge.md` §关键业务规则 5），不影响终态语义。

### 2.3 重复/冲突边 ✓ PASS

- **方法**：对每个 `(from, to)` 对，检查是否有多条 transition；多条时检查 action 字段是否区分。
- **结果**：105 个 Bean 全部 `(from, to)` 对唯一，无重复冲突。
- **意义**：无「同一状态对多路径不一致」语义模糊。

### 2.4 dict-writer 对照 ✓ PASS

- **方法**：对每个 Bean 声明的状态（含 initial + terminal + transition 边端点），grep 全仓 `setStatus("XXX")` 字面量与 `setStatus(*Constants.STATE_XXX)` 调用，验证每个状态值有合法 writer 命中。
- **结果**：105 个 Bean 的所有状态值在 ORM dict 或 Java 常量中均有 writer（bizmodel / per-mutation Processor / facade / 测试 fixture 至少 1 处）。
- **意义**：无「dict 死状态」—— 每个声明状态都被代码实际使用。
- **白名单**：当前空（如未来添加已知误报，需在工具 `KNOWN_FALSE_POSITIVES` 集合 + 本文档 §5 同步登记）。

## §3 异常清单

**无**。

| Domain | Bean | Type | Severity | Detail |
|---|---|---|---|---|
| _（空）_ | | | | |

## §4 已知误报白名单

**空**。如下一轮审计发现新误报，必须同步：
1. 在 `docs/audits/scripts/state-machine-coverage-check.py` 的 `KNOWN_FALSE_POSITIVES` 集合添加 `(bean_class, finding_type, detail[:50])` 三元组
2. 在本节登记理由 + 控制点 + 计划处置
3. 在 `docs/architecture/state-machine-matrix.md` §4 同步

## §5 残余风险

1. **本审计只覆盖 4 维度静态对账**，未覆盖：
   - 运行时异常（race condition / out-of-order call）—— 应由各域测试 + 集成测试覆盖
   - 业务语义正确性（每条 transition 是否真实反映业务需求）—— 应由 owner doc + use cases 覆盖
   - 跨域状态联动（如 approve 跨域触发 posting）—— 应由跨域 E2E 覆盖
2. **writer 索引依赖正则匹配**：当前规则匹配 `setStatus("XXX")` 字面量 + `setStatus(*Constants.STATE_XXX)`。其他 setStatus 形态（如 `setStatus(bean.getStatus())`、`setStatus(stateMap.get(key))`）可能漏检。若发现漏检场景，扩展 §4 工具正则。
3. **本审计未检测 `posted` 列不对称**——这是 contract 返利结算单已记录的 watch-only residual（详见 plan `2026-08-14-2000-1`），不在 M5.1 4 维度对账范围内。
4. **M4.65 之后新增 Bean**：本审计基于 `2026-08-28` 仓库快照。新增 Bean 后需重跑本工具。

## §6 给 M5.2 守卫脚本的输入清单

M5.2 守卫脚本（`docs/audits/scripts/state-machine-coverage-check.py` 已在 M5.1 落地）应：

1. **复用本工具**：M5.2 不需要新建独立工具，直接在本工具上加：
   - `--strict` 模式已在工具中：未白名单 findings 退出码 1
   - 误报白名单机制已在工具中：`KNOWN_FALSE_POSITIVES` 集合 + 文档同步
2. **新增维护入口文档** `docs/architecture/state-machine-matrix.md`（**D3 已落地**，见独立文档）
3. **CI 接线**（**M5.2 任务**）：
   - 在 `tools/` 或 `.github/workflows/` 加 wrapper 脚本调用本工具
   - 默认 `--strict`（CI 必须 0 findings）
   - 不强接 gate（防止误报阻断开发流；失败时仅日志告警 + 通知 reviewer）
4. **新增状态/动作的同步清单**（**M5.2 D3 任务**，已在 `docs/architecture/state-machine-matrix.md` §3 列明）：
   - 加新状态：同步 Bean transitions + ORM dict + Java Constants + writer 路径
   - 加新 action：同步 Bean transitions + facade Processor + facade helper
   - 加新 Bean：本工具自动识别（按 `Erp*StateMachine` 命名约定）

## §7 工具使用示例

```bash
# 默认（exit 0 always，本地 manual）
python3 docs/audits/scripts/state-machine-coverage-check.py

# 产出 JSON + Markdown 报告到指定路径
python3 docs/audits/scripts/state-machine-coverage-check.py \
  --json-output /tmp/audit.json \
  --md-output /tmp/audit.md

# CI 模式（unwhitelisted findings 时 exit 1）
python3 docs/audits/scripts/state-machine-coverage-check.py --strict
```

## §8 后续工作

- **M5.2**（todo 状态）→ 用 M5.1 工具 + 误报白名单 + 维护入口文档，启动 guard script 与 CI 接线
- **M5.3**（todo 状态）→ 在 M5.1 + M5.2 done 后启动 mission 级 closure audit（跨域全量回归 + Delta 覆盖回归 + owner doc 对齐）
- **M5.1 closure audit**（todo 状态）→ 由独立子代理跑 closure-audit-prompt 复核本报告与工具实现
