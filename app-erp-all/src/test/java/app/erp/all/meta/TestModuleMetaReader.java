package app.erp.all.meta;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitBaseTestCase;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务模块元数据读取器测试（plan 2026-07-21-2225-3 Phase 2）。
 *
 * <p>覆盖：(1) 多模块 classpath 扫描；(2) 依赖完整性正路径；(3) 依赖完整性负路径（missing + version mismatch）；
 * (4) 特性清单查询；(5) 缺失 BT5 字段向后兼容（null 字段不报错）。
 */
@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE)
public class TestModuleMetaReader extends JunitBaseTestCase {

    @Inject
    ModuleMetaReader moduleMetaReader;

    // (1) 多模块 classpath 扫描：聚合 app 启动后应发现全部 19 个 erp/* 模块
    @Test
    public void testListModulesScansAllDomains() {
        List<ModuleMetaBean> modules = moduleMetaReader.listModules();
        long erpModules = modules.stream().filter(m -> m.getModuleId() != null && m.getModuleId().startsWith("erp/")).count();
        assertTrue(erpModules >= 19,
                "应扫描到全部 19 个业务域，实际 erp/* 模块: " + erpModules);
        assertTrue(modules.stream().anyMatch(m -> "erp/pur".equals(m.getModuleId())),
                "应包含 purchase 模块");
        assertTrue(modules.stream().anyMatch(m -> "erp/md".equals(m.getModuleId())),
                "应包含 master-data 模块");
        // Phase 3 落地后全部 19 模块均声明 version
        assertTrue(modules.stream().filter(m -> m.getModuleId() != null && m.getModuleId().startsWith("erp/"))
                .allMatch(m -> m.getVersion() != null),
                "全部 19 业务域均应声明 version");
    }

    // (2) 依赖完整性正路径：真实 classpath 下全部声明依赖应满足（purchase → md/inv 均存在且版本匹配）
    @Test
    public void testDependencyIntegrityPositivePath() {
        DependencyIntegrityResult result = moduleMetaReader.checkDependencyIntegrity();
        assertNotNull(result);
        assertTrue(result.isOk(),
                "依赖完整性校验应通过，missing=" + result.getMissing() + ", mismatches=" + result.getMismatches());
    }

    // (3a) 依赖完整性负路径：缺失依赖
    @Test
    public void testDependencyIntegrityMissingDependency() {
        ModuleMetaReader synthetic = new ModuleMetaReader() {
            @Override
            protected Map<String, ModuleMetaBean> loadAll() {
                Map<String, ModuleMetaBean> map = new LinkedHashMap<>();
                ModuleMetaBean bean = new ModuleMetaBean();
                bean.setModuleId("erp/test");
                bean.setVersion("1.0.0");
                ModuleMetaBean.ModuleDependency dep = new ModuleMetaBean.ModuleDependency();
                dep.setModuleId("erp/MISSING");
                dep.setVersion("1.0.0");
                bean.setBusinessDependencies(List.of(dep));
                map.put(bean.getModuleId(), bean);
                return map;
            }
        };
        DependencyIntegrityResult result = synthetic.checkDependencyIntegrity();
        assertFalse(result.isOk());
        assertEquals(1, result.getMissing().size());
        assertTrue(result.getMissing().get(0).contains("erp/MISSING"));
    }

    // (3b) 依赖完整性负路径：版本不匹配
    @Test
    public void testDependencyIntegrityVersionMismatch() {
        ModuleMetaReader synthetic = new ModuleMetaReader() {
            @Override
            protected Map<String, ModuleMetaBean> loadAll() {
                Map<String, ModuleMetaBean> map = new LinkedHashMap<>();
                ModuleMetaBean dep = new ModuleMetaBean();
                dep.setModuleId("erp/md");
                dep.setVersion("1.0.0");
                map.put(dep.getModuleId(), dep);

                ModuleMetaBean bean = new ModuleMetaBean();
                bean.setModuleId("erp/test");
                bean.setVersion("1.0.0");
                ModuleMetaBean.ModuleDependency d = new ModuleMetaBean.ModuleDependency();
                d.setModuleId("erp/md");
                d.setVersion("2.0.0");
                bean.setBusinessDependencies(List.of(d));
                map.put(bean.getModuleId(), bean);
                return map;
            }
        };
        DependencyIntegrityResult result = synthetic.checkDependencyIntegrity();
        assertFalse(result.isOk());
        assertEquals(1, result.getMismatches().size());
        DependencyIntegrityResult.VersionMismatch vm = result.getMismatches().get(0);
        assertEquals("erp/md", vm.getDependencyId());
        assertEquals("2.0.0", vm.getExpected());
        assertEquals("1.0.0", vm.getActual());
    }

    // (4) 特性清单查询
    @Test
    public void testListOptionalFeatures() {
        List<ModuleMetaBean.ModuleFeature> features = moduleMetaReader.listOptionalFeatures();
        // purchase 模块声明了 supplier-scorecard-red-gate 特性（pilot）
        assertTrue(features.stream().anyMatch(f -> "supplier-scorecard-red-gate".equals(f.getFeature())),
                "应包含 purchase 的 supplier-scorecard-red-gate 特性: " + features);
    }

    // (5) 缺失 BT5 字段向后兼容：未声明 businessDependencies 的模块（master-data DAG 根）字段为 null，不报错
    @Test
    public void testBackwardCompatMissingFieldsAreNull() {
        ModuleMetaBean pur = moduleMetaReader.getModule("erp/pur");
        assertNotNull(pur);
        assertNotNull(pur.getVersion(), "purchase 应有 version");
        assertNotNull(pur.getBusinessDependencies(), "purchase 应有 businessDependencies");

        // master-data 是 DAG 根，无跨域调用，省略 businessDependencies（声明 vs 省略规则的负样本）
        ModuleMetaBean md = moduleMetaReader.getModule("erp/md");
        assertNotNull(md);
        assertNull(md.getBusinessDependencies(), "master-data 作为 DAG 根应省略 businessDependencies");
    }

    // (6) 跨域依赖矩阵抽样（Phase 3）：核对生成 businessDependencies 与架构文档 DAG 一致
    @Test
    public void testCrossDomainDependencyMatrixSampling() {
        // finance → master-data + inventory + purchase + sales + assets + projects
        ModuleMetaBean fin = moduleMetaReader.getModule("erp/fin");
        assertNotNull(fin.getBusinessDependencies());
        List<String> finDeps = fin.getBusinessDependencies().stream()
                .map(ModuleMetaBean.ModuleDependency::getModuleId).collect(java.util.stream.Collectors.toList());
        assertTrue(finDeps.contains("erp/md"), "finance 应依赖 master-data");
        assertTrue(finDeps.contains("erp/inv"), "finance 应依赖 inventory");
        assertTrue(finDeps.contains("erp/pur"), "finance 应依赖 purchase");
        assertTrue(finDeps.contains("erp/sal"), "finance 应依赖 sales");

        // manufacturing → master-data + inventory（与 domain-module-split-analysis.md §4.1 DAG 一致）
        ModuleMetaBean mfg = moduleMetaReader.getModule("erp/mfg");
        List<String> mfgDeps = mfg.getBusinessDependencies().stream()
                .map(ModuleMetaBean.ModuleDependency::getModuleId).collect(java.util.stream.Collectors.toList());
        assertTrue(mfgDeps.contains("erp/md"), "manufacturing 应依赖 master-data");
        assertTrue(mfgDeps.contains("erp/inv"), "manufacturing 应依赖 inventory");

        // maintenance → master-data + inventory（与 §4.1 DAG 一致）
        ModuleMetaBean mnt = moduleMetaReader.getModule("erp/mnt");
        List<String> mntDeps = mnt.getBusinessDependencies().stream()
                .map(ModuleMetaBean.ModuleDependency::getModuleId).collect(java.util.stream.Collectors.toList());
        assertTrue(mntDeps.contains("erp/md"), "maintenance 应依赖 master-data");
        assertTrue(mntDeps.contains("erp/inv"), "maintenance 应依赖 inventory");
    }
}
