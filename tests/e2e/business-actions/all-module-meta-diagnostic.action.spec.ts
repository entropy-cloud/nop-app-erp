import { test, expect, loginAndNavigate, GraphQLClient } from './_helper';

/**
 * app-erp-all `ErpModuleMetaBizModel` 4 `@BizQuery` 浏览器层 E2E
 * （plan 2026-07-27-0930-1，D2 §8.8 浏览器层验证补全）。
 *
 * 验证业务模块元数据诊断 4 个 `@BizQuery` 方法经 GraphQL `/graphql` 全栈可达 + seed
 * 派生确定性断言（镜像 `md-party-query.action.spec.ts` 同型非实体 BizModel `@BizQuery`
 * 读路径范式，对齐 JUnit `TestModuleMetaReader` 7 场景）：
 *   (1) ErpModuleMeta__listModules：返回 List 经 `erp/*` 前缀过滤后长度 == 19 + 全部
 *       version="1.0.0" + 含 erp/md（master-data DAG 根，businessDependencies=null
 *       向后兼容）+ 含 erp/pur（businessDependencies 非空 6 项 + optionalFeatures 非空）
 *   (2) ErpModuleMeta__getModule(moduleId:"erp/md")：非 null + moduleId/version 断言 +
 *       businessDependencies=null（DAG 根向后兼容）
 *   (3) ErpModuleMeta__getModule(moduleId:"erp/pur")：非 null + businessDependencies
 *       长度 = 6 + 含 {moduleId:"erp/md",version:"1.0.0"} + optionalFeatures 含
 *       supplier-scorecard-red-gate
 *   (4) ErpModuleMeta__getModule(moduleId:"erp/nonexistent")：返回 null（不存在 moduleId
 *       容忍，对齐 ModuleMetaReader.getModule:42-45 null 返回）
 *   (5) ErpModuleMeta__checkDependencyIntegrity：返回 ok=true + missing=[] +
 *       mismatches=[]（fresh-DB 19 域版本一致 + DAG 闭合）
 *   (6) ErpModuleMeta__listOptionalFeatures：返回 List 非空 + 含
 *       {feature:"supplier-scorecard-red-gate",configKey:"erp-pur.scorecard-prevent-on-red",
 *        defaultValue:true}（defaultValue Object scalar 序列化为 JSON 布尔）
 *
 * 权威设计（docs/architecture/business-module-metadata.md §4.3 运行时读取器契约 + 4
 * `@BizQuery` 诊断端点）：
 *   - listModules/getModule/checkDependencyIntegrity/listOptionalFeatures 4 `@BizQuery`
 *     经 `app-service.beans.xml` 显式注册（非实体 BizModel）
 *   - 19 域 `_module-meta.json` 全声明 version=1.0.0；master-data(DAG 根)/notify(跨域
 *     基础设施)省 businessDependencies；aps/logistics/b2b 省 optionalFeatures
 *
 * Phase 1 Explore Decision（落盘 plan Execution Decisions 段）：
 *   - **无 setup/cleanup**：模块元数据为运行时启动期 codegen 产物（`_module-meta.json`
 *     经 `ModuleManager` 扫描 classpath），非数据库行；非实体 BizModel 无
 *     `__save`/`__delete` 入口，不污染共享 DB 基线（区别于 1500-2 partner/employee/org
 *     自包含 setup）。
 *   - **无 config-gate**：4 `@BizQuery` 读路径无 config 门控（区别于 simulation/intercompany/
 *     exchange-rate-api 等 config-gated `@BizMutation`），fresh-DB 启动后即可经 GraphQL 可达。
 *   - **GraphQL selection set 构造**：4 `@BizQuery` 全部返回复杂类型（List<ModuleMetaBean>/
 *     ModuleMetaBean/DependencyIntegrityResult/List<ModuleFeature>），`GraphQLClient.callQuery`
 *     不带 selection set（仅适标量返回）→ 经 `new GraphQLClient(page).raw()` 内联完整 query +
 *     selection set（镜像 1500-2 md-party-query + cs-canned-response/fin-reconciliation 范式）。
 *   - **getModule 三路径裁决**：(a) erp/md + (b) erp/pur + (c) erp/nonexistent（返回 null
 *     容忍）三路径覆盖，对齐 1500-2 `getParty` 三类型覆盖范式。
 *   - **erp/* 前缀过滤 + 精确 == 19**：`listModules()` 返回 classpath 全部启用模块（含平台
 *     nop/* 模块），spec 经 `erp/` 前缀过滤后精确 == 19（收紧 JUnit
 *     `testListModulesScansAllDomains:36` 的 `>= 19`，对齐 seed 派生确定性断言范式）。
 */

const MODULE_META_SELECTION =
  'moduleId moduleName appName version businessDependencies{ moduleId version } optionalFeatures{ feature configKey defaultValue }';

interface ModuleDependencyRow {
  moduleId: string;
  version: string;
}

interface ModuleFeatureRow {
  feature: string;
  configKey: string;
  defaultValue: unknown;
}

interface ModuleMetaRow {
  moduleId: string;
  moduleName: string | null;
  appName: string | null;
  version: string | null;
  businessDependencies: ModuleDependencyRow[] | null;
  optionalFeatures: ModuleFeatureRow[] | null;
}

interface DependencyIntegrityRow {
  ok: boolean;
  missing: string[];
  mismatches: Array<{
    moduleId: string;
    dependencyId: string;
    expected: string;
    actual: string;
  }>;
}

test.describe('app-erp-all ErpModuleMeta diagnostic (@BizQuery) browser-layer E2E', () => {
  test('(1)-(6) listModules + getModule three-path + checkDependencyIntegrity + listOptionalFeatures', async ({ page }) => {
    await loginAndNavigate(page, '/md-dashboard-main');

    const gql = new GraphQLClient(page);

    // ---- (1) listModules: erp/* 前缀 == 19 + 全部 version=1.0.0 + erp/md(DAG根) + erp/pur(6 deps+1 feature) ----
    const listJson: any = await gql.raw(
      `query{ ErpModuleMeta__listModules{ ${MODULE_META_SELECTION} } }`,
    );
    expect(
      listJson?.errors,
      `listModules should not return GraphQL errors: ${JSON.stringify(listJson?.errors)}`,
    ).toBeFalsy();
    const allModules: ModuleMetaRow[] = listJson?.data?.ErpModuleMeta__listModules ?? [];
    expect(Array.isArray(allModules), 'listModules must return an array').toBe(true);

    // listModules 返回 classpath 全部启用模块（含平台 nop/* 模块），按 erp/ 前缀过滤到 19 业务域
    const erpModules = allModules.filter(m => m.moduleId?.startsWith('erp/'));
    expect(
      erpModules.length,
      'listModules erp/* prefix count must be exactly 19 (11 core + 5 first-batch + 3 second-batch)',
    ).toBe(19);
    // 全部 19 业务域均声明 version=1.0.0
    for (const m of erpModules) {
      expect(
        m.version,
        `erp module ${m.moduleId} must declare version="1.0.0"`,
      ).toBe('1.0.0');
    }

    // master-data（DAG 根，businessDependencies=null 向后兼容验证点）
    const md = erpModules.find(m => m.moduleId === 'erp/md');
    expect(md, 'listModules should contain erp/md (master-data DAG root)').toBeTruthy();
    expect(
      md!.businessDependencies,
      'erp/md (DAG root) must omit businessDependencies (null for backward compat)',
    ).toBeNull();

    // purchase（businessDependencies 非空 6 项 + optionalFeatures 非空样本）
    const pur = erpModules.find(m => m.moduleId === 'erp/pur');
    expect(pur, 'listModules should contain erp/pur (purchase)').toBeTruthy();
    expect(
      pur!.businessDependencies,
      'erp/pur businessDependencies must be non-null (6 deps sample)',
    ).not.toBeNull();
    expect(
      pur!.businessDependencies!.length,
      'erp/pur businessDependencies length must be 6',
    ).toBe(6);
    expect(
      pur!.optionalFeatures,
      'erp/pur optionalFeatures must be non-null (1 feature sample)',
    ).not.toBeNull();

    // ---- (2) getModule(moduleId:"erp/md"): 非 null + businessDependencies=null（DAG 根向后兼容）----
    const mdGetJson: any = await gql.raw(
      `query{ ErpModuleMeta__getModule(moduleId:"erp/md"){ ${MODULE_META_SELECTION} } }`,
    );
    expect(
      mdGetJson?.errors,
      `getModule(erp/md) should not return GraphQL errors: ${JSON.stringify(mdGetJson?.errors)}`,
    ).toBeFalsy();
    const mdGet: ModuleMetaRow | null = mdGetJson?.data?.ErpModuleMeta__getModule ?? null;
    expect(mdGet, 'getModule(erp/md) should return non-null').toBeTruthy();
    expect(mdGet!.moduleId, 'getModule(erp/md) moduleId').toBe('erp/md');
    expect(mdGet!.version, 'getModule(erp/md) version').toBe('1.0.0');
    expect(
      mdGet!.businessDependencies,
      'getModule(erp/md) businessDependencies must be null (DAG root backward compat)',
    ).toBeNull();

    // ---- (3) getModule(moduleId:"erp/pur"): businessDependencies=6 + 含 erp/md 1.0.0 + supplier-scorecard-red-gate ----
    const purGetJson: any = await gql.raw(
      `query{ ErpModuleMeta__getModule(moduleId:"erp/pur"){ ${MODULE_META_SELECTION} } }`,
    );
    expect(
      purGetJson?.errors,
      `getModule(erp/pur) should not return GraphQL errors: ${JSON.stringify(purGetJson?.errors)}`,
    ).toBeFalsy();
    const purGet: ModuleMetaRow | null = purGetJson?.data?.ErpModuleMeta__getModule ?? null;
    expect(purGet, 'getModule(erp/pur) should return non-null').toBeTruthy();
    expect(purGet!.moduleId, 'getModule(erp/pur) moduleId').toBe('erp/pur');
    expect(
      purGet!.businessDependencies,
      'getModule(erp/pur) businessDependencies must be non-null',
    ).not.toBeNull();
    expect(
      purGet!.businessDependencies!.length,
      'getModule(erp/pur) businessDependencies length must be 6',
    ).toBe(6);
    // 含 erp/md version=1.0.0（DAG 根被引用样本）
    const depMd = purGet!.businessDependencies!.find(d => d.moduleId === 'erp/md');
    expect(depMd, 'getModule(erp/pur) businessDependencies should include erp/md').toBeTruthy();
    expect(depMd!.version, 'getModule(erp/pur) erp/md dep version').toBe('1.0.0');
    // optionalFeatures 含 supplier-scorecard-red-gate
    expect(
      purGet!.optionalFeatures,
      'getModule(erp/pur) optionalFeatures must be non-null',
    ).not.toBeNull();
    const scorecardFeature = purGet!.optionalFeatures!.find(
      f => f.feature === 'supplier-scorecard-red-gate',
    );
    expect(
      scorecardFeature,
      'getModule(erp/pur) optionalFeatures should include supplier-scorecard-red-gate',
    ).toBeTruthy();
    expect(
      scorecardFeature!.configKey,
      'supplier-scorecard-red-gate configKey',
    ).toBe('erp-pur.scorecard-prevent-on-red');

    // ---- (4) getModule(moduleId:"erp/nonexistent"): 返回 null（不存在 moduleId 容忍）----
    const noneGetJson: any = await gql.raw(
      `query{ ErpModuleMeta__getModule(moduleId:"erp/nonexistent"){ ${MODULE_META_SELECTION} } }`,
    );
    expect(
      noneGetJson?.errors,
      `getModule(erp/nonexistent) should not return GraphQL errors: ${JSON.stringify(noneGetJson?.errors)}`,
    ).toBeFalsy();
    const noneGet: ModuleMetaRow | null = noneGetJson?.data?.ErpModuleMeta__getModule ?? null;
    expect(
      noneGet,
      'getModule(erp/nonexistent) must return null (tolerate unknown moduleId, ModuleMetaReader.getModule:42-45)',
    ).toBeNull();

    // ---- (5) checkDependencyIntegrity: ok=true + missing=[] + mismatches=[] ----
    const integrityJson: any = await gql.raw(
      `query{ ErpModuleMeta__checkDependencyIntegrity{ ok missing mismatches{ moduleId dependencyId expected actual } } }`,
    );
    expect(
      integrityJson?.errors,
      `checkDependencyIntegrity should not return GraphQL errors: ${JSON.stringify(integrityJson?.errors)}`,
    ).toBeFalsy();
    const integrity: DependencyIntegrityRow | null =
      integrityJson?.data?.ErpModuleMeta__checkDependencyIntegrity ?? null;
    expect(integrity, 'checkDependencyIntegrity should return non-null result').toBeTruthy();
    expect(
      integrity!.ok,
      `checkDependencyIntegrity.ok must be true (fresh-DB 19 domains version 1.0.0 + DAG closed). missing=${JSON.stringify(integrity!.missing)} mismatches=${JSON.stringify(integrity!.mismatches)}`,
    ).toBe(true);
    expect(
      integrity!.missing,
      'checkDependencyIntegrity.missing must be empty (no missing deps)',
    ).toEqual([]);
    expect(
      integrity!.mismatches,
      'checkDependencyIntegrity.mismatches must be empty (no version mismatches)',
    ).toEqual([]);

    // ---- (6) listOptionalFeatures: 非空 + 含 purchase supplier-scorecard-red-gate defaultValue=true ----
    const featuresJson: any = await gql.raw(
      `query{ ErpModuleMeta__listOptionalFeatures{ feature configKey defaultValue } }`,
    );
    expect(
      featuresJson?.errors,
      `listOptionalFeatures should not return GraphQL errors: ${JSON.stringify(featuresJson?.errors)}`,
    ).toBeFalsy();
    const features: ModuleFeatureRow[] = featuresJson?.data?.ErpModuleMeta__listOptionalFeatures ?? [];
    expect(Array.isArray(features), 'listOptionalFeatures must return an array').toBe(true);
    expect(
      features.length,
      'listOptionalFeatures must return non-empty list (purchase + master-data + notify + ...)',
    ).toBeGreaterThan(0);
    // 含 purchase supplier-scorecard-red-gate（defaultValue Object scalar 序列化为 JSON boolean true）
    const purFeature = features.find(f => f.feature === 'supplier-scorecard-red-gate');
    expect(
      purFeature,
      'listOptionalFeatures should include purchase supplier-scorecard-red-gate',
    ).toBeTruthy();
    expect(
      purFeature!.configKey,
      'supplier-scorecard-red-gate configKey',
    ).toBe('erp-pur.scorecard-prevent-on-red');
    expect(
      purFeature!.defaultValue,
      'supplier-scorecard-red-gate defaultValue must serialize as JSON boolean true',
    ).toBe(true);
  });
});
