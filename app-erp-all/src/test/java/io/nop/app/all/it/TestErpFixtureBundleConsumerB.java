package io.nop.app.all.it;

import io.nop.api.core.ioc.BeanContainer;
import io.nop.autotest.bundle.FixtureBundleImportResult;
import io.nop.orm.IOrmEntity;
import io.nop.orm.IOrmTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * fixture-bundle M2.3 跨类共享消费 #2（plan 2026-10-02-0100-1）：与 ConsumerA 相同的共享
 * bundle、独立 Importer 实例、相同断言集合（顺序无关）+ roadmap 实测基线（全量主数据 base
 * + 1 payload 的导入耗时；堆数字信息性记录于 owner doc，精度受 GC 干扰）。
 */
public class TestErpFixtureBundleConsumerB extends ErpIntegrationTestCase {

    @Test
    public void testSharedBundleImportConsumerB() {
        FixtureBundleDemoSupport.ensureDemoExported();
        IOrmTemplate orm = (IOrmTemplate) BeanContainer.getBeanByType(IOrmTemplate.class);

        FixtureBundleImportResult result = new io.nop.autotest.bundle.FixtureBundleImporter()
                .importBundle(new java.io.File("target/fixture-bundle-demo/entry"), orm);

        assertEquals(1, result.getBaseImported());
        assertEquals(0, result.getBaseSkipped());
        assertEquals(1, result.getPayloadImported());

        // roadmap measured baseline: full-master-data base + 1 payload import
        long elapsedNanos = FixtureBundleDemoSupport.timedBaselineImport();
        assertTrue(elapsedNanos > 0, "baseline import must take measurable time");
        assertTrue(FixtureBundleDemoSupport.getBaselineRows() > 0, "baseline bundle must carry rows");
        // informational record for the owner-doc measured baseline (GC-interference caveat)
        System.out.println("M23BASELINE elapsedMs=" + elapsedNanos / 1_000_000
                + " rows=" + FixtureBundleDemoSupport.getBaselineRows());
    }
}
