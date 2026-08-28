# scripts/ 与 tools/ 脚本索引

本目录存放**一次性 + 可重复运行**的辅助脚本。供 AI 与开发者在**一次性维护任务 / 数据迁移 / CI 接线**时使用。

## 分类索引

### 一致性 / 兼容性检查

- `check-orm-auth-i18n.js` — 全仓 ORM / 权限 / i18n 一致性检查
- `tools/check-state-machine-coverage.sh` — **entity-state-machine 守卫层（M5.2）** — 4 维度对账（状态可达性 / 终态出边 / 重复冲突边 / dict-writer 一致性），包装 `docs/audits/scripts/state-machine-coverage-check.py` 工具（M5.1）
  - 默认 manual 模式（exit 0 always）
  - `--strict` 模式（CI 用，exit 1 if finding）
  - 报告落 `docs/audits/check/<YYYY-MM-DD-HHmm>-entity-state-machine-m5-2/`（多次执行隔离）
  - 配套文档：`docs/audits/state-machine-matrix-audit.md` + `docs/architecture/state-machine-matrix.md`

### ORM / 代码生成

- `flip-orm-to-flux.sh` — ORM `web-renderer=amis` 翻转为 `flux`
- `add-orm-i18n.js` — ORM 列加 i18n 字段
- `add-orm-indexes.js` — ORM 加索引
- `add-dict-i18n.js` — dict 加 i18n
- `flip-menu-to-flux.sh` — 菜单翻转为 flux
- `rebuild-flux-chain.sh` — 重建 flux 渲染链

### 重生成辅助

- `scripts/` 下的脚本多数为一次性数据迁移 / 字段补充辅助

## 使用约定

- **零破坏原则**：脚本默认 dry-run，加 `--apply` 才真正写回
- **白名单机制**：扫到已知误报时工具应支持白名单（`KNOWN_FALSE_POSITIVES` 集合 + 文档同步）
- **多次执行隔离**：审计类工具的产出落 `docs/audits/check/<YYYY-MM-DD-HHmm>-<mission>/` 而非平铺

## 添加新脚本

1. 在 `scripts/` 或 `tools/`（CI/守卫层）下创建
2. 加 `chmod +x` + shebang `#!/usr/bin/env bash` 或 `#!/usr/bin/env python3`
3. 在本 README 添加分类索引
4. 重大变更走独立 plan + closure audit
