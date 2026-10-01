package io.nop.app.all.it;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.ioc.BeanContainer;
import io.nop.api.core.ioc.IBeanContainer;
import io.nop.autotest.bundle.FixtureBundleExportConfig;
import io.nop.autotest.bundle.FixtureBundleManifest;
import io.nop.autotest.bundle.FixtureBundleRecordingSession;
import io.nop.batch.dsl.runner.IBatchTaskRunner;
import io.nop.orm.IOrmEntity;
import io.nop.orm.IOrmSession;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * fixture-bundle roadmap M2.1（plan 2026-10-02-0026-1）：批量入口可调度调用证明（route a，
 * test-scope batch-dsl）。经真实调度面 {@code IBatchTaskRunner.execute}（= 生产 job.yaml 的
 * invoker 同面，非旁路 shortcut，沿 {@code TestErpFinApDocBatchWiring} 范式）触发 processor
 * 导入入口；round-trip 断言对具名字段与回读实体（批准附注 N2）。bundle 仅落 {@code target/}
 * （repo 级入库门控 successor 义务不变）。
 */
@NopTestConfig(localDb = false, initDatabaseSchema = OptionalBoolean.TRUE,
        testBeansFile = "/nop/test/beans/fixture-bundle-batch.beans.xml")
public class TestErpFixtureBundleBatchEntry extends ErpIntegrationTestCase {

    private static final File BUNDLE_DIR = new File("target/fixture-bundle-batch/entry");
    private static final String TASK_PATH = "/nop/test/batch-task/fixture-bundle.batch.xml";

    @Inject
    IBatchTaskRunner batchTaskRunner;

    private IOrmEntity newCurrency(IOrmSession orm, String code, String name) {
        IOrmEntity currency = orm.newEntity("app.erp.md.dao.entity.ErpMdCurrency");
        currency.orm_propValueByName("code", code);
        currency.orm_propValueByName("name", name);
        currency.orm_propValueByName("symbol", "B");
        currency.orm_propValueByName("decimalPlaces", 2);
        currency.orm_propValueByName("isFunctional", false);
        currency.orm_propValueByName("isActive", true);
        return currency;
    }

    private IOrmEntity probeCurrency(IOrmSession orm, String code) {
        IOrmEntity probe = orm.newEntity("app.erp.md.dao.entity.ErpMdCurrency");
        probe.orm_propValueByName("code", code);
        return probe;
    }

    @Test
    public void testBatchEntryRoundTripViaSchedulerFace() {
        IOrmTemplate orm = (IOrmTemplate) BeanContainer.getBeanByType(IOrmTemplate.class);

        // construct + export the bundle the batch entry will import
        FixtureBundleRecordingSession session = new FixtureBundleRecordingSession();
        session.run(hook -> orm.runInSession(s -> {
            IOrmEntity currency = newCurrency(s, "TST-BATCH", "批量入口币种");
            s.save(currency);
            Object currencyId = currency.orm_propValueByName("id");

            IOrmEntity rate = s.newEntity("app.erp.md.dao.entity.ErpMdExchangeRate");
            rate.orm_propValueByName("fromCurrencyId", currencyId);
            rate.orm_propValueByName("toCurrencyId", 1L);
            rate.orm_propValueByName("rate", new BigDecimal("2.50000000"));
            rate.orm_propValueByName("validFrom", Date.valueOf("2026-01-01"));
            s.save(rate);
            return null;
        }));

        FixtureBundleExportConfig config = new FixtureBundleExportConfig("batch-entry", "snap");
        config.addBaseTable("app.erp.md.dao.entity.ErpMdCurrency", List.of("CODE"));
        config.addPayloadTable("app.erp.md.dao.entity.ErpMdExchangeRate");
        BUNDLE_DIR.getParentFile().mkdirs();
        FixtureBundleManifest manifest = new io.nop.autotest.bundle.FixtureBundleExporter()
                .export(BUNDLE_DIR, config, session, orm);
        assertEquals(1, manifest.getSnapshots().get(0).getTables().get(0).getRowCount());

        // positive discrimination (approval C1): the processor bean resolves at the
        // expected type via the explicitly-loaded testBeansFile — "tests stay green"
        // proves nothing about the wiring
        IBeanContainer container = BeanContainer.instance();
        Object processor = container.getBean("fixtureBundleImportProcessor");
        assertTrue(processor instanceof FixtureBundleImportBatchProcessor,
                "testBeansFile 显式装载未生效：fixtureBundleImportProcessor 应为批量入口实现");

        // schedule face: the real invoker used by production job.yaml entries
        assertNotNull(batchTaskRunner);
        batchTaskRunner.execute(TASK_PATH);

        // named result-field assertions (approval note N2)
        FixtureBundleImportBatchProcessor proc = (FixtureBundleImportBatchProcessor) processor;
        assertNotNull(proc.getLast(), "batch entry must have run the import");
        // dirty-env semantics (constructed rows are never deleted): base reconciles
        // and skips, payload lands per the manifest
        assertEquals(0, proc.getLast().getBaseImported());
        assertEquals(1, proc.getLast().getBaseSkipped());
        assertEquals(1, proc.getLast().getPayloadImported());

        // read-back: payload landed and FROM was remapped to the reconciled TST currency
        orm.runInSession(s -> {
            IOrmEntity example = s.newEntity("app.erp.md.dao.entity.ErpMdExchangeRate");
            example.orm_propValueByName("rate", new BigDecimal("2.50000000"));
            IOrmEntity landed = s.findFirstByExample(example);
            assertNotNull(landed, "imported rate row must be present");
            Object tst = s.findFirstByExample(probeCurrency(s, "TST-BATCH")).orm_propValueByName("id");
            assertEquals(String.valueOf(tst),
                    String.valueOf(landed.orm_propValueByName("fromCurrencyId")),
                    "FROM must be remapped to the reconciled TST currency id");
            return null;
        });
    }
}
