package app.erp.fin.service.processor;

import app.erp.fin.dao.api.IErpFinGlMappingResolver;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinGlMappingRule refreshCache per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含缓存手动刷新编排（list grid 工具栏按钮触发，多节点仅刷新本节点缓存）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinGlMappingRuleRefreshCacheProcessor {

    @Inject
    IErpFinGlMappingResolver glMappingResolver;

    public void refreshCache(IServiceContext context) {
        glMappingResolver.invalidateCache();
    }
}
