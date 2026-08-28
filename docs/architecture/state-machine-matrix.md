# State Machine 矩阵架构文档（entity-state-machine 维护入口）

> **来源**：entity-state-machine-migration roadmap §M5.1（审计产物）+ §M5.2（维护入口需求）
> **配套工具**：`docs/audits/scripts/state-machine-coverage-check.py`
> **配套报告**：`docs/audits/state-machine-matrix-audit.md`
> **架构上下文**：`docs/architecture/processor-extension-pattern.md`（StateMachine Bean 与 Processor 关系）

## 1 审计方法学

本项目采用 4 维度对账审计每个 Erp*StateMachine Bean 的合规性：

| 维度 | 检查内容 | 通过判据 |
|---|---|---|
| **状态可达性** | 从 `initial(...)` 出发，所有 `transition(...)` 边可达所有声明状态 | 可达闭包 ⊇ 声明状态集合 |
| **终态出边** | `terminal(...)` 状态无出边 | terminal 集合 ∩ transition-from 集合 = ∅ |
| **重复冲突边** | 同一 `(from, to)` 对最多 1 条 transition | key 唯一；多条时 action 必须区分 |
| **dict-writer 对照** | 状态值必须有 writer 命中（`setStatus("XXX")` 或 `setStatus(*Constants.STATE_XXX)`） | writer_index[state] ≥ 1 |

工具扫描全仓 105 个 Erp*StateMachine.java + 全 main/java/*.java writer 命中，**约 2 秒完成**。

## 2 检查工具使用说明

### 2.1 命令

```bash
# 默认（手动运行，exit 0 always）
python3 docs/audits/scripts/state-machine-coverage-check.py

# 产出 JSON + Markdown 报告
python3 docs/audits/scripts/state-machine-coverage-check.py \
  --json-output /tmp/audit.json \
  --md-output /tmp/audit.md

# CI 模式（unwhitelisted findings → exit 1）
python3 docs/audits/scripts/state-machine-coverage-check.py --strict

# 自定义根目录（用于 monorepo 或子目录扫描）
python3 docs/audits/scripts/state-machine-coverage-check.py --root /path/to/repo
```

### 2.2 输出格式

- **stdout**：JSON（默认）或 Markdown（带 `--md-output`）
- **stderr**：日志（INFO/ERROR/WARN）
- **退出码**：
  - 0 = 全部通过 / 白名单内 / 非 strict 模式
  - 1 = `--strict` 模式下存在未白名单 finding
  - 2 = 工具内部错误

### 2.3 字段说明

| 字段 | 含义 |
|---|---|
| `summary.beans_scanned` | 扫描的 Erp*StateMachine.java 总数 |
| `summary.findings_total` | 4 维度发现的异常总数 |
| `summary.findings_after_whitelist` | 去掉白名单后的待处理数 |
| `summary.whitelisted` | 白名单命中数 |
| `by_domain` | 按域（pur/sal/inv/...）分布 |
| `by_finding_type` | 按 finding 类型（UNREACHABLE_STATE / REVERSIBLE_TERMINAL / DUPLICATE_TRANSITION / ORPHAN_DICT_VALUE）分布 |
| `findings[]` | 待处理 finding 列表（每条含 domain/bean_class/finding_type/severity/detail） |
| `whitelisted_findings[]` | 白名单命中列表（应定期审计） |

### 2.4 CI 接线建议

```bash
# .github/workflows/state-machine-check.yml 伪代码
- name: State Machine Matrix Check
  run: python3 docs/audits/scripts/state-machine-coverage-check.py --strict
- name: Upload report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: state-machine-audit
    path: state-machine-audit-*.md
```

**CI 触发条件**：PR 改动 `module-*/erp-*-service/src/main/java/**/*StateMachine*.java` 或 `**/*.orm.xml` 或 `**/*Constants.java` 时触发。

**警告**：CI 默认不 gate（防止误报阻断开发流）。仅 `--strict` 模式 + reviewer 同意后才升级到必过。

## 3 新增状态/动作维护义务清单

**这是文档的核心**。当开发者需要给现有 Bean 加状态/动作时，**必须同步以下 6 项**：

### 3.1 加新状态（status value）

| 同步项 | 路径 | 说明 |
|---|---|---|
| Bean `initial()` / `terminal()` / `transition()` | `module-*/erp-*-service/src/main/java/app/erp/<domain>/service/statemachine/<Entity>StateMachine.java` | 更新状态集合声明 |
| ORM dict | `module-*/erp-*-meta/src/main/resources/_dict/<entity>-status.dict.yaml` 或 `<domain>.orm.xml` <dict> 段 | 加 dict 值（含 i18n 字段） |
| Java Constants | `module-*/erp-*-service/src/main/java/app/erp/<domain>/service/<Entity>Constants.java` | 加常量声明 |
| Writer 调用方 | 找 setStatus 调用（grep `\.setStatus\s*\(\s*<新状态>`） | 确保至少 1 个生产代码 writer |
| 矩阵测试 | `module-*/erp-*-service/src/test/java/.../statemachine/Test<Entity>StateMachineMatrix.java` | 加状态可达性断言 |
| 文档 | `docs/design/<domain>/state-machine.md` | 加状态说明 + 迁移图更新 |

### 3.2 加新动作（action / transition 边）

| 同步项 | 路径 | 说明 |
|---|---|---|
| Bean `transition()` | 同上 | 加 from-to-action 元组 |
| facade Processor | `module-*/erp-*-service/src/main/java/app/erp/<domain>/service/processor/<Entity>*Processor.java` | 加 per-mutation 方法（命名 `<action><Entity>`） |
| facade helper | 如有 `Erp<Entity>StateMachineHelper` | 委托给 Bean assertCanXxx + *TargetStatus |
| 矩阵测试 | 同上 | 加 transition 边断言 |
| 文档 | `docs/design/<domain>/state-machine.md` | 加动作说明 |

### 3.3 加新 Bean（新实体）

| 同步项 | 路径 | 说明 |
|---|---|---|
| Bean 类 | `module-*/erp-*-service/src/main/java/app/erp/<domain>/service/statemachine/<Entity>StateMachine.java` | 新建文件，命名 `Erp<Entity>StateMachine` |
| Bean 注册 | `module-*/erp-*-service/src/main/resources/META-INF/services/` 或 nop-ioc beans.xml | 注册为 `@Bean`（可被 @Inject） |
| 矩阵测试 | `module-*/erp-*-service/src/test/java/.../statemachine/Test<Entity>StateMachineMatrix.java` | 层 1 矩阵测试（4 维对账） |
| 四方对照 | `docs/design/<domain>/state-machine.md` | 加章节：dict ↔ owner doc ↔ Bean ↔ writer |
| 文档 | `docs/design/<domain>/state-machine.md` | 加新实体章节 |
| 进程接线 | `module-*/erp-*-service/src/main/java/app/erp/<domain>/service/processor/<Entity>*Processor.java` | 委托给 Bean assertCanXxx + *TargetStatus |

### 3.4 加新 ORM 字段（status 类型）

| 同步项 | 路径 | 说明 |
|---|---|---|
| ORM 列 | `module-*/model/app-erp-<domain>.orm.xml` | 加 status 列（如 `postedStatus`），用 `<dict>` 关联 dict |
| 触发 M5.1 重跑 | `python3 docs/audits/scripts/state-machine-coverage-check.py` | 验证新字段对应的 dict 值在 Bean 中已声明 |
| 触发 M5.2 重跑 | 守卫脚本 | 同上 |

## 4 已知误报白名单

**当前白名单为空**。如果发现工具误报，按以下步骤添加：

### 4.1 添加流程

1. 在 `docs/audits/scripts/state-machine-coverage-check.py` 的 `KNOWN_FALSE_POSITIVES` 集合添加：
   ```python
   ("ErpXxxStateMachine", "FINDING_TYPE", "detail[:50] 前缀")
   ```
2. 在本节登记：
   | Bean | Finding Type | 理由 | 处置计划 |
   |---|---|---|---|
   | _例_ | _ORPHAN_DICT_VALUE_ | _CANCELLED 是 intentional reserved per plan `2026-08-14-2000-1`_ | _PM 要求时再实现_ |
3. 在 `docs/audits/state-machine-matrix-audit.md` §4 同步登记
4. 重跑工具验证：白名单命中数 +1，待处理数 -1

### 4.2 白名单登记（当前空）

| Bean | Finding Type | 理由 | 处置计划 |
|---|---|---|---|
| _（空）_ | | | |

## 5 工具开发约定

### 5.1 工具维护原则

- **零依赖**：仅 stdlib（re / json / argparse / pathlib），可离线运行
- **确定性输出**：同一仓库两次运行结果字节级一致（exit code + JSON 内容）
- **退出码语义明确**：0/1/2 三态（通过/失败/内部错误），CI 可直接判断
- **日志走 stderr**：stdout 仅承载数据输出（JSON / Markdown），方便 pipe 与重定向

### 5.2 工具扩展点

| 需求 | 扩展位置 |
|---|---|
| 加新 finding 类型 | `Finding` dataclass + `check_*()` 函数 + `KNOWN_FALSE_POSITIVES` key 格式 |
| 加新扫描维度 | `check_*()` 函数 + `all_findings.extend(...)` 注册 |
| 改变扫描路径 | `scan_module()` 内 rglob pattern（注意 src/main 过滤） |
| 改 writer 索引正则 | `build_writer_index()` 内 `re.finditer` |
| 输出格式扩展 | `--md-output` / `--json-output` 路径下追加新格式输出 |

### 5.3 已知工具局限

1. **writer 索引仅匹配 setStatus**：不追踪动态状态赋值（如 `setStatus(enum.value)`、`setStatus(stateMap.get(k))`）。如果发现漏检场景，扩展 `build_writer_index` 的正则。
2. **未识别 action 与 owner doc 的语义匹配**：工具只检查状态轴完整性，不验证 action 命名是否对齐 owner doc。
3. **跨 Bean 一致性未检查**：如果两个 Bean 共享 dict 值但 transitions 不一致，工具无法识别（需要人工或下游工具）。

## 6 关联文档

- **Roadmap**：`docs/backlog/entity-state-machine-migration-roadmap.md`（M5.1/M5.2/M5.3 闭环纪律）
- **工具**：`docs/audits/scripts/state-machine-coverage-check.py`（代码）
- **审计报告**：`docs/audits/state-machine-matrix-audit.md`（M5.1 终态）
- **进程模式**：`docs/architecture/processor-extension-pattern.md`（Bean + Processor 关系）
- **领域 owner doc**：各域 `docs/design/<domain>/state-machine.md`（迁移图真相源）
- **历史 plan**：
  - `2026-08-14-2000-1-erpct-rebate-settlement-state-machine-bean.md`（M4.65 最后一批）
  - `2026-08-14-1931-*`（assets 域）
  - `2026-08-14-0930-*`（manufacturing/quality/maintenance）
  - `2026-08-14-0456-*`（finance/hr/logistics）
  - `2026-08-13-0810-*`（purchase/sales/inventory）
  - 等等（M2/M3/M4 共 65+ 计划）
