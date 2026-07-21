package app.erp.all.meta;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 业务模块元数据诊断 BizModel，供运维/升级前检查经 GraphQL 查询。
 *
 * <p>查询示例（GraphQL）：
 * <pre>
 *   query { ErpModuleMeta__listModules { moduleId version } }
 *   query { ErpModuleMeta__checkDependencyIntegrity { ok missing } }
 * </pre>
 *
 * @see docs/architecture/business-module-metadata.md §4.3
 */
@BizModel("ErpModuleMeta")
public class ErpModuleMetaBizModel {

    @Inject
    ModuleMetaReader moduleMetaReader;

    public void setModuleMetaReader(ModuleMetaReader moduleMetaReader) {
        this.moduleMetaReader = moduleMetaReader;
    }

    @BizQuery
    public List<ModuleMetaBean> listModules() {
        return moduleMetaReader.listModules();
    }

    @BizQuery
    public ModuleMetaBean getModule(@Name("moduleId") String moduleId) {
        return moduleMetaReader.getModule(moduleId);
    }

    @BizQuery
    public DependencyIntegrityResult checkDependencyIntegrity() {
        return moduleMetaReader.checkDependencyIntegrity();
    }

    @BizQuery
    public List<ModuleMetaBean.ModuleFeature> listOptionalFeatures() {
        return moduleMetaReader.listOptionalFeatures();
    }
}
