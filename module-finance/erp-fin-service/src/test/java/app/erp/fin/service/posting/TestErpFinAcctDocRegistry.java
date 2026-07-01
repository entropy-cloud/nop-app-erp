package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 过账 Provider 注册中心的契约测试（Phase 1）——在接入真实 Provider 前（Phase 2）证明
 * fallback / 域优先 / fail-fast 语义已稳定。
 *
 * <p>纯单元测试（不启动 IoC 容器），直接构造 Registry + 桩 Provider。
 */
public class TestErpFinAcctDocRegistry {

    private static class DomainProvider implements IErpFinAcctDocProvider {
        private final Set<ErpFinBusinessType> types;

        DomainProvider(ErpFinBusinessType... types) {
            this.types = EnumSet.noneOf(ErpFinBusinessType.class);
            Collections.addAll(this.types, types);
        }

        @Override
        public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
            return types;
        }

        @Override
        public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
            return Collections.emptyList();
        }
    }

    private static class FallbackProvider extends DomainProvider {
        FallbackProvider(ErpFinBusinessType... types) {
            super(types);
        }

        @Override
        public boolean isFallback() {
            return true;
        }
    }

    @Test
    public void testRegistryDomainProviderWinsOverDefault() {
        DomainProvider domain = new DomainProvider(ErpFinBusinessType.PURCHASE_INPUT);
        FallbackProvider fallback = new FallbackProvider(ErpFinBusinessType.PURCHASE_INPUT,
                ErpFinBusinessType.AP_INVOICE);

        ErpFinAcctDocRegistry registry = new ErpFinAcctDocRegistry();
        registry.setProviders(Arrays.asList(fallback, domain));
        registry.init();

        assertSame(domain, registry.getProvider(ErpFinBusinessType.PURCHASE_INPUT),
                "域 Provider 应优先于默认 Provider");
        assertSame(fallback, registry.getProvider(ErpFinBusinessType.AP_INVOICE),
                "未被域 Provider 接管的类型由默认 Provider 兜底");
    }

    @Test
    public void testRegistryDuplicateNonDefaultFailsFast() {
        DomainProvider a = new DomainProvider(ErpFinBusinessType.SALES_OUTPUT);
        DomainProvider b = new DomainProvider(ErpFinBusinessType.SALES_OUTPUT);

        ErpFinAcctDocRegistry registry = new ErpFinAcctDocRegistry();
        registry.setProviders(Arrays.asList(a, b));

        assertThrows(NopException.class, registry::init,
                "两个非默认 Provider 声明同一 businessType 应启动期 fail-fast");
    }
}
