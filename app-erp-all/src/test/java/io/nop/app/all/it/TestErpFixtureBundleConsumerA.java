package io.nop.app.all.it;

import io.nop.api.core.ioc.BeanContainer;
import io.nop.autotest.bundle.FixtureBundleImportResult;
import io.nop.orm.IOrmEntity;
import io.nop.orm.IOrmTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * fixture-bundle M2.3 跨类共享消费 #1（plan 2026-10-02-0100-1）：同一共享 bundle 经 M1.2
 * 导入路径装载（零 `_cases` CSV 副本）；断言集合与 ConsumerB 相同（顺序无关，Decision J）。
 */
public class TestErpFixtureBundleConsumerA extends ErpIntegrationTestCase {

    @Test
    public void testSharedBundleImportConsumerA() {
        FixtureBundleDemoSupport.ensureDemoExported();
        IOrmTemplate orm = (IOrmTemplate) BeanContainer.getBeanByType(IOrmTemplate.class);

        FixtureBundleImportResult result = new io.nop.autotest.bundle.FixtureBundleImporter()
                .importBundle(new java.io.File("target/fixture-bundle-demo/entry"), orm);

        // fresh seeded DB (construction rows deleted after export) — base inserts, not skips
        assertEquals(1, result.getBaseImported());
        assertEquals(0, result.getBaseSkipped());
        assertEquals(1, result.getPayloadImported());

        // masked column lands in placeholder form; the raw value never appears
        orm.runInSession(s -> {
            IOrmEntity probe = s.newEntity("app.erp.md.dao.entity.ErpMdCurrency");
            probe.orm_propValueByName("code", "TST-DEMO");
            IOrmEntity landed = s.findFirstByExample(probe);
            assertNotNull(landed);
            assertEquals("MASKED-BUNDLE-SEED", landed.orm_propValueByName("name"),
                    "masked NAME column must land as the placeholder");
            return null;
        });
        orm.runInSession(s -> {
            IOrmEntity rawProbe = s.newEntity("app.erp.md.dao.entity.ErpMdCurrency");
            rawProbe.orm_propValueByName("name", "验收币种-DEMO-RAW");
            assertEquals(null, s.findFirstByExample(rawProbe), "raw masked value must not exist");
            return null;
        });
    }
}
