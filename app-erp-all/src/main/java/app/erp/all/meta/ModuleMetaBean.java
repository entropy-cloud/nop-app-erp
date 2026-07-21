package app.erp.all.meta;

import java.util.List;

/**
 * 业务模块元数据 Bean，对应 {@code _module-meta.json} 的反序列化结构。
 *
 * <p>前 4 字段（moduleId/moduleName/appName/icon）为平台既有字段，由 ORM 根 {@code ext:} 属性经
 * {@code /nop/templates/meta} 生成。后 3 字段（version/businessDependencies/optionalFeatures）为
 * BT5 风格扩展，由 meta 模块手写源 {@code precompile/module-meta.yaml} 经 {@code gen-meta.xgen} overlay 叠加，
 * 全部 optional，缺失字段为 null（向后兼容）。
 *
 * @see docs/architecture/business-module-metadata.md
 */
public class ModuleMetaBean {

    private String moduleId;
    private String moduleName;
    private String appName;
    private String icon;

    private String version;
    private List<ModuleDependency> businessDependencies;
    private List<ModuleFeature> optionalFeatures;

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ModuleDependency> getBusinessDependencies() {
        return businessDependencies;
    }

    public void setBusinessDependencies(List<ModuleDependency> businessDependencies) {
        this.businessDependencies = businessDependencies;
    }

    public List<ModuleFeature> getOptionalFeatures() {
        return optionalFeatures;
    }

    public void setOptionalFeatures(List<ModuleFeature> optionalFeatures) {
        this.optionalFeatures = optionalFeatures;
    }

    public static class ModuleDependency {
        private String moduleId;
        private String version;

        public String getModuleId() {
            return moduleId;
        }

        public void setModuleId(String moduleId) {
            this.moduleId = moduleId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class ModuleFeature {
        private String feature;
        private String configKey;
        private Object defaultValue;

        public String getFeature() {
            return feature;
        }

        public void setFeature(String feature) {
            this.feature = feature;
        }

        public String getConfigKey() {
            return configKey;
        }

        public void setConfigKey(String configKey) {
            this.configKey = configKey;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }
    }
}
