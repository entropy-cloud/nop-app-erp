package app.erp.all.meta;

import java.util.ArrayList;
import java.util.List;

/**
 * 依赖完整性校验结果。{@link ModuleMetaReader#checkDependencyIntegrity()} 返回。
 *
 * <p>诊断场景：{@code ok=true} 表示全部 businessDependencies 引用的 moduleId 存在且精确版本匹配；
 * {@code ok=false} 时 missing/mismatches 非空，调用方可据此决定是否阻断升级。
 *
 * <p>首版仅做存在性 + 精确版本匹配，不做 SemVer 范围求解（归 successor）。
 */
public class DependencyIntegrityResult {

    private boolean ok;
    private final List<String> missing = new ArrayList<>();
    private final List<VersionMismatch> mismatches = new ArrayList<>();

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public List<String> getMissing() {
        return missing;
    }

    public List<VersionMismatch> getMismatches() {
        return mismatches;
    }

    public static class VersionMismatch {
        private final String moduleId;
        private final String dependencyId;
        private final String expected;
        private final String actual;

        public VersionMismatch(String moduleId, String dependencyId, String expected, String actual) {
            this.moduleId = moduleId;
            this.dependencyId = dependencyId;
            this.expected = expected;
            this.actual = actual;
        }

        public String getModuleId() {
            return moduleId;
        }

        public String getDependencyId() {
            return dependencyId;
        }

        public String getExpected() {
            return expected;
        }

        public String getActual() {
            return actual;
        }
    }
}
