package app.erp.all.meta;

import io.nop.core.lang.json.JsonTool;
import io.nop.core.module.ModuleManager;
import io.nop.core.module.ModuleModel;
import io.nop.core.resource.IResource;
import io.nop.core.resource.VirtualFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务模块元数据运行时读取器。扫描 classpath 全部 {@code _module-meta.json}，反序列化为 {@link ModuleMetaBean}，
 * 提供查询 API 与依赖完整性校验。
 *
 * <p>归属模块：{@code app-erp-all}（聚合 app，已知全部 19 模块，见
 * {@code docs/architecture/business-module-metadata.md} §Decision D）。
 *
 * <p>扫描机制：经 {@link ModuleManager#getEnabledModules()} 枚举各域，再取 {@code /{moduleId}/model/_module-meta.json}。
 * 未声明 BT5 扩展字段（version/businessDependencies/optionalFeatures）的模块反序列化后对应字段为 null（向后兼容）。
 *
 * @see docs/architecture/business-module-metadata.md §4
 */
public class ModuleMetaReader {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleMetaReader.class);

    static final String META_FILE_PATH = "model/_module-meta.json";

    public ModuleMetaReader() {
    }

    public List<ModuleMetaBean> listModules() {
        return new ArrayList<>(loadAll().values());
    }

    public ModuleMetaBean getModule(String moduleId) {
        if (moduleId == null)
            return null;
        return loadAll().get(moduleId);
    }

    public DependencyIntegrityResult checkDependencyIntegrity() {
        Map<String, ModuleMetaBean> modules = loadAll();
        DependencyIntegrityResult result = new DependencyIntegrityResult();
        result.setOk(true);

        for (ModuleMetaBean module : modules.values()) {
            if (module.getBusinessDependencies() == null || module.getBusinessDependencies().isEmpty())
                continue;
            for (ModuleMetaBean.ModuleDependency dep : module.getBusinessDependencies()) {
                String depId = dep.getModuleId();
                ModuleMetaBean depModule = modules.get(depId);
                if (depModule == null) {
                    result.getMissing().add(module.getModuleId() + " -> " + depId);
                    result.setOk(false);
                    continue;
                }
                if (dep.getVersion() != null && depModule.getVersion() != null
                        && !dep.getVersion().equals(depModule.getVersion())) {
                    result.getMismatches().add(new DependencyIntegrityResult.VersionMismatch(
                            module.getModuleId(), depId, dep.getVersion(), depModule.getVersion()));
                    result.setOk(false);
                }
            }
        }
        return result;
    }

    public List<ModuleMetaBean.ModuleFeature> listOptionalFeatures() {
        List<ModuleMetaBean.ModuleFeature> all = new ArrayList<>();
        for (ModuleMetaBean module : loadAll().values()) {
            if (module.getOptionalFeatures() != null) {
                for (ModuleMetaBean.ModuleFeature f : module.getOptionalFeatures()) {
                    all.add(f);
                }
            }
        }
        return all;
    }

    protected Map<String, ModuleMetaBean> loadAll() {
        Collection<ModuleModel> enabled = ModuleManager.instance().getEnabledModules(true);
        Map<String, ModuleMetaBean> result = new LinkedHashMap<>();
        for (ModuleModel module : enabled) {
            String moduleId = module.getModuleId();
            if (moduleId == null)
                continue;
            IResource metaRes = VirtualFileSystem.instance().getResource("/" + moduleId + "/" + META_FILE_PATH);
            if (!metaRes.exists()) {
                LOG.debug("nop.module-meta.not-found:moduleId={}", moduleId);
                continue;
            }
            try {
                ModuleMetaBean bean = JsonTool.parseBeanFromResource(metaRes, ModuleMetaBean.class);
                if (bean.getModuleId() == null)
                    bean.setModuleId(moduleId);
                result.put(bean.getModuleId(), bean);
            } catch (Exception e) {
                LOG.warn("nop.module-meta.parse-fail:moduleId={}", moduleId, e);
            }
        }
        return result;
    }
}
