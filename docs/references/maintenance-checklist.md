# 文档维护检查清单

## 目的

在更改落地后使用此文件检查仓库记忆是否保持同步。

## 非平凡代码更改后始终审查

1. `docs/architecture/` 中的相关 owner doc
2. `docs/logs/` 中的每日日志
3. 任何受影响的需求、计划、bug 笔记或测试笔记
4. 建立有意义的完整验证基线时的 `docs/testing/known-good-baselines.md`

## 更改触发条件

### 架构或边界更改

审查：

- `docs/architecture/system-baseline.md`
- `docs/architecture/module-boundaries.md`
- 如果路由更改，`docs/index.md`

### 产品意图或范围更改

审查：

- 如果源材料本身更改，`docs/input/` 中的相关文件
- 如果需求解释更改，`docs/discussions/` 中的相关文件
- `docs/requirements/` 中的相关文件
- 如果更改影响长期方向，`docs/architecture/project-vision.md`

### 应用层功能或流程更改

审查：

- `docs/design/` 中最相关的文件
- 如果面向用户的范围更改，`docs/requirements/`
- 如果需要手动/探索性证明，`docs/testing/`

### 非平凡实施切片

审查：

- `docs/plans/` 下的活动计划
- 如果审计是切片的一部分，`docs/audits/` 下的相关文件
- `docs/logs/YYYY/MM-DD.md`
- 如果需要探索性/手动证明，`docs/testing/`

创建的计划需要在实施前进行独立草案审查，并在完成前进行结束审计。

### 微妙回归或根本原因发现

审查：

- `docs/bugs/`
- 如果问题暴露了流程或需求差距，`docs/retrospectives/`
- 任何受影响的 owner doc

## 验证基线

使用 `docs/context/project-context.md` 中的真实项目命令。

如果该文件仍包含占位符，请在声称验证成功前填写。