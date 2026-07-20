# Lesson 06: 代码生成产物编辑——`_` 前缀和 `__XGEN_FORCE_OVERRIDE__` 文件被 `mvn clean install` 覆盖

> **来源**：notify inbox（3 轮审计）+ business-type.dict.yaml（3 轮审计）真实案例。两次都需要至少 3 轮独立审计才捕获根因：「在生成产物上直接编辑」。
> **适用场景**：任何 Nop 平台项目中需要修改一个由 ORM/api 模型驱动生成的资源（`.dict.yaml`、`_app.orm.xml`、`_service.beans.xml`、`_*.xbiz`、生成的 Java `_gen/` 文件等）。
> **失败模式**：手工修改生成产物；下一次 `mvn clean install`（或 `nop-cli gen`）覆盖修改，回归"看似已修复"的 bug。

## 核心论点

**Nop 的代码生成是单向变换：模型 → 产物。** 任何在产物上的手工编辑都是临时状态，下一次 codegen 周期必然丢失。修改的**唯一正确位置是模型源**或**保留层 Delta**，二选一。

## 失败模式（典型路径）

```
1. 审计/调试发现 X 文件（如 app-erp-x.dict.yaml）的某字典值/字段错误
2. 直接编辑 X 文件 → 跑 mvn test → 通过 → 提交
3. 下一次 ORM/api 模型变更触发 codegen，覆盖 X → bug 回归
4. 下一轮审计再发现 → 再编辑 → 再被覆盖 → 反复 3 轮
```

## 真实案例

### Case A: notify inbox saga（3 轮审计）

notify 跨域通知派发子系统的 inbox 视图/路由配置，最初被手工编辑在生成的 `_app.orm.xml` 或 `_service.beans.xml` 衍生文件上。每轮审计修改后短期通过测试，但下一次模型增量重生成时被 `__XGEN_FORCE_OVERRIDE__` 标记强制覆盖。3 轮后才定位到应编辑**保留层**（无 `_` 前缀的同名文件，由 codegen 通过 `x:extends` 增量合并）。

### Case B: business-type.dict.yaml（3 轮审计）

业务类型字典 `business-type.dict.yaml` 由 ORM 模型 `<dict name="biz-type">` 驱动生成。审计发现某字典值的 `desc` 描述错误，直接编辑 yaml 文件通过测试，但下一次 ORM 模型变更后 codegen 强制覆盖。3 轮后才将描述改回 ORM `<dict>` 元素中。

## 决策树：发现要改某文件时，先问"这是谁生成的？"

```
1. 文件名带 `_` 前缀（如 `_app.orm.xml`、`_service.beans.xml`、`_{Entity}.xbiz`）？
   → 生成产物。禁止编辑。改模型源或保留层。

2. 文件首行/末行含 `__XGEN_FORCE_OVERRIDE__` 或 `//GENERATED CODE - DO NOT EDIT`？
   → 生成产物。禁止编辑。改模型源或保留层。

3. 文件在 `module-<domain>/model/*.orm.xml` 或 `*.api.xml`？
   → 模型源。**唯一真相**。可以编辑（注意 ask-first 保护区域）。

4. 文件在 `_gen/` 子目录下？
   → 生成产物。禁止编辑。改模型源。

5. 文件在保留层（同名但无 `_` 前缀，且通过 `x:extends="_xxx"` 引用生成层）？
   → 保留层。**这是 Delta 定制的正确位置**。可以编辑。

6. 不确定？
   → grep 同名带 `_` 前缀的文件；查 `x:extends` 引用关系；查 codegen 入口（`*-codegen` 模块）。
   → 仍不确定？ask-first，不要假设。
```

## 自检清单（每次编辑前）

- [ ] 我确认了文件名**不带** `_` 前缀？
- [ ] 我确认了文件**不在** `_gen/` 目录下？
- [ ] 我确认了文件首行**不含** `__XGEN_FORCE_OVERRIDE__` / `//GENERATED` 标记？
- [ ] 如果是字典/yaml/config：我确认了它**不是**从 ORM `<dict>` 或 `<options>` 元素生成的？
- [ ] 如果编辑了生成产物：我**同时**修改了模型源（或保留层 Delta）以确保 codegen 周期不丢失？

## 平台参考

- `../nop-entropy/docs-for-ai/02-core-guides/delta-customization.md` — Delta 机制原理
- `../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md` — 模型优先开发
- `../nop-entropy/docs-for-ai/01-repo-map/domain-module-pattern.md` — 模块链 model → codegen → dao → ... → api
- 平台规则：决策顺序 Model → Delta → Java（见 `../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`）

## 防御性习惯

1. **永远 grep 文件名**：编辑前 `rg "__XGEN_FORCE_OVERRIDE__|_app\.orm\.xml|_gen/" <file>`。
2. **看 git blame**：若历史中"同一文件多次被改回同一问题"，强烈怀疑生成产物被覆盖。
3. **codegen 后 diff**：跑 `mvn clean install -DskipTests` 后 `git diff` 看哪些"已修复"的内容被还原了。
4. **审计优先怀疑**：审计发现"看似已修但实际未修"的项，优先查文件是否在生成产物上。

## 反模式

| 不要这样 | 应该这样 |
| --- | --- |
| `vim module-x/erp-x-meta/src/main/resources/_vfs/dict/y.dict.yaml`（生成产物） | 编辑 `module-x/model/app-erp-x.orm.xml` 的 `<dict name="y">` 元素 |
| `vim module-x/erp-x-dao/_vfs/_app.orm.xml`（`_` 前缀） | 编辑 `module-x/model/app-erp-x.orm.xml`（模型源） |
| `vim module-x/erp-x-service/.../_{Entity}.xbiz.xml`（`_` 前缀） | 编辑保留层 `{Entity}.xbiz.xml`（通过 `x:extends` 增量覆盖） |
| "我先改产物验证假设，再回头改模型" | 直接改模型——验证假设的成本和回头补的成本几乎相同，但少了"忘记回头"的风险 |
