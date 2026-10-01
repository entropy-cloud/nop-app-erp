package io.nop.app.all.it;

import io.nop.api.core.ioc.BeanContainer;
import io.nop.autotest.bundle.FixtureBundleExportConfig;
import io.nop.autotest.bundle.FixtureBundleManifest;
import io.nop.autotest.bundle.FixtureBundleRecordingSession;
import io.nop.orm.IOrmEntity;
import io.nop.orm.IOrmTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

/**
 * fixture-bundle M2.3 consumer demo support (plan 2026-10-02-0100-1): JVM-once export of
 * the shared demo bundle + the full-master-data baseline bundle, then construction rows
 * are DELETED so each per-class fresh seeded DB imports the same on-disk bundle with
 * identical assertions regardless of surefire ordering (Decision J).
 * <p>
 * Sensitive-column demonstration: the demo currency's NAME column is declared masked —
 * export writes the MASKED-BUNDLE-SEED placeholder, import lands the placeholder form.
 */
public class FixtureBundleDemoSupport {
    private static final Object LOCK = new Object();

    private static final File DEMO_DIR = new File("target/fixture-bundle-demo/entry");
    private static final File BASELINE_DIR = new File("target/fixture-bundle-baseline/entry");

    /** md-domain entity set for the roadmap-scale baseline (base = full master data) */
    static final List<String> MD_ENTITIES = List.of(
            "app.erp.md.dao.entity.ErpMdMaterial",
            "app.erp.md.dao.entity.ErpMdMaterialCustoms",
            "app.erp.md.dao.entity.ErpMdMaterialCategory",
            "app.erp.md.dao.entity.ErpMdMaterialSku",
            "app.erp.md.dao.entity.ErpMdPartner",
            "app.erp.md.dao.entity.ErpMdPartnerAddress",
            "app.erp.md.dao.entity.ErpMdPartnerContact",
            "app.erp.md.dao.entity.ErpMdWarehouse",
            "app.erp.md.dao.entity.ErpMdLocation",
            "app.erp.md.dao.entity.ErpMdUoM",
            "app.erp.md.dao.entity.ErpMdUoMConversion",
            "app.erp.md.dao.entity.ErpMdCurrency",
            "app.erp.md.dao.entity.ErpMdExchangeRate",
            "app.erp.md.dao.entity.ErpMdTaxRate",
            "app.erp.md.dao.entity.ErpMdSettlementMethod",
            "app.erp.md.dao.entity.ErpMdBankAccount",
            "app.erp.md.dao.entity.ErpMdEmployee",
            "app.erp.md.dao.entity.ErpMdSubject",
            "app.erp.md.dao.entity.ErpMdSubjectMapping",
            "app.erp.md.dao.entity.ErpMdAcctSchema",
            "app.erp.md.dao.entity.ErpMdAcctSchemaCoa",
            "app.erp.md.dao.entity.ErpMdCostCenter",
            "app.erp.md.dao.entity.ErpMdOrganization",
            "app.erp.md.dao.entity.ErpMdSupplierApproval",
            "app.erp.md.dao.entity.ErpSysConfig");

    private static volatile boolean demoExported;
    private static volatile boolean baselineExported;
    private static volatile long baselineRows = -1;
    private static volatile long baselineElapsedNanos = -1;

    private static IOrmTemplate orm() {
        return (IOrmTemplate) BeanContainer.getBeanByType(IOrmTemplate.class);
    }

    /** Export the shared demo bundle once per JVM; construction rows are deleted so any
     *  per-class fresh seeded DB imports it identically (Decision J). */
    public static void ensureDemoExported() {
        if (demoExported)
            return;
        synchronized (LOCK) {
            if (demoExported)
                return;
            Object[] ids = new Object[2];
            FixtureBundleRecordingSession session = new FixtureBundleRecordingSession();
            session.run(hook -> orm().runInSession(s -> {
                IOrmEntity currency = s.newEntity("app.erp.md.dao.entity.ErpMdCurrency");
                currency.orm_propValueByName("code", "TST-DEMO");
                currency.orm_propValueByName("name", "验收币种-DEMO-RAW");
                currency.orm_propValueByName("symbol", "D");
                currency.orm_propValueByName("decimalPlaces", 2);
                currency.orm_propValueByName("isFunctional", false);
                currency.orm_propValueByName("isActive", true);
                s.save(currency);
                ids[0] = currency.orm_propValueByName("id");

                IOrmEntity rate = s.newEntity("app.erp.md.dao.entity.ErpMdExchangeRate");
                rate.orm_propValueByName("fromCurrencyId", ids[0]);
                rate.orm_propValueByName("toCurrencyId", 1L);
                rate.orm_propValueByName("rate", new BigDecimal("3.14000000"));
                rate.orm_propValueByName("validFrom", Date.valueOf("2026-01-01"));
                s.save(rate);
                ids[1] = rate.orm_propValueByName("id");
                return null;
            }));

            // sensitive-column declaration (roadmap M2.3): NAME masked — placeholder form
            FixtureBundleExportConfig config = new FixtureBundleExportConfig("demo-shared", "snap");
            config.addBaseTable("app.erp.md.dao.entity.ErpMdCurrency", List.of("CODE"), List.of("NAME"));
            config.addPayloadTable("app.erp.md.dao.entity.ErpMdExchangeRate");
            DEMO_DIR.getParentFile().mkdirs();
            new io.nop.autotest.bundle.FixtureBundleExporter().export(DEMO_DIR, config, session, orm());

            // Decision J: remove construction rows — host class returns to clean seeded state
            deleteRow("app.erp.md.dao.entity.ErpMdExchangeRate", ids[1]);
            deleteRow("app.erp.md.dao.entity.ErpMdCurrency", ids[0]);
            demoExported = true;
        }
    }

    /** Export the roadmap-scale baseline bundle once per JVM (base = full master data via
     *  findAll touch + 1 payload), then remove the constructed payload row. */
    public static void ensureBaselineExported() {
        if (baselineExported)
            return;
        synchronized (LOCK) {
            if (baselineExported)
                return;
            Object[] ids = new Object[1];
            FixtureBundleRecordingSession session = new FixtureBundleRecordingSession();
            session.run(hook -> {
                // touch every seeded master-data row (onLoad collection = same semantics as
                // platform _cases input capture); M1.1 full-row reload guarantees columns
                orm().runInSession(s -> {
                    for (String entityName : MD_ENTITIES) {
                        s.findAllByExample(s.newEntity(entityName), null);
                    }
                    return null;
                });
                orm().runInSession(s -> {
                    IOrmEntity rate = s.newEntity("app.erp.md.dao.entity.ErpMdExchangeRate");
                    rate.orm_propValueByName("fromCurrencyId", 2L);
                    rate.orm_propValueByName("toCurrencyId", 1L);
                    rate.orm_propValueByName("rate", new BigDecimal("9.99000000"));
                    rate.orm_propValueByName("validFrom", Date.valueOf("2026-02-01"));
                    s.save(rate);
                    ids[0] = rate.orm_propValueByName("id");
                    return null;
                });
            });

            FixtureBundleExportConfig config = new FixtureBundleExportConfig("baseline-full-md", "snap");
            for (String entityName : MD_ENTITIES) {
                config.addPayloadTable(entityName);
            }
            BASELINE_DIR.getParentFile().mkdirs();
            FixtureBundleManifest manifest = new io.nop.autotest.bundle.FixtureBundleExporter()
                    .export(BASELINE_DIR, config, session, orm());
            baselineRows = manifest.getSnapshots().get(0).getTables().stream()
                    .mapToLong(e -> e.getRowCount()).sum();

            deleteRow("app.erp.md.dao.entity.ErpMdExchangeRate", ids[0]);
            baselineExported = true;
        }
    }

    /** Timed baseline import (roadmap measured baseline): gc-bracketed wall clock; heap
     *  observation is informational only (GC interference — recorded with caveat). */
    public static synchronized long timedBaselineImport() {
        ensureBaselineExported();
        System.gc();
        long t0 = System.nanoTime();
        new FixtureBundleImporterHolder().importer.importBundle(BASELINE_DIR, orm());
        long elapsed = System.nanoTime() - t0;
        System.gc();
        baselineElapsedNanos = elapsed;
        return elapsed;
    }

    public static long getBaselineElapsedNanos() {
        return baselineElapsedNanos;
    }

    public static long getBaselineRows() {
        return baselineRows;
    }

    private static void deleteRow(String entityName, Object id) {
        orm().runInSession(s -> {
            IOrmEntity entity = s.get(entityName, id);
            if (entity != null)
                s.delete(entity);
            return null;
        });
    }

    static final class FixtureBundleImporterHolder {
        final io.nop.autotest.bundle.FixtureBundleImporter importer = new io.nop.autotest.bundle.FixtureBundleImporter();
    }
}
