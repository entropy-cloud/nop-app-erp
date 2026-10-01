package io.nop.app.all.it;

import io.nop.autotest.bundle.FixtureBundleExportConfig;
import io.nop.autotest.bundle.FixtureBundleImporter;
import io.nop.autotest.bundle.FixtureBundleImportResult;
import io.nop.autotest.bundle.FixtureBundleManifest;
import io.nop.autotest.bundle.FixtureBundleRecordingSession;
import io.nop.autotest.bundle.FixtureBundleTableEntry;
import io.nop.api.core.ioc.BeanContainer;
import io.nop.orm.IOrmEntity;
import io.nop.orm.IOrmSession;
import io.nop.orm.IOrmTemplate;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * fixture-bundle roadmap 验收②（M1.3，plan 2026-10-01-2255-1）：脏环境 base 不覆盖。
 *
 * <p>载体 = 文件 H2 + 全量部署 seed（{@link ErpIntegrationTestCase} fresh-DB 天然脏环境）。
 * base = {@code ErpMdCurrency}（业务键 CODE，seed 含 CNY/USD）；payload =
 * {@code ErpMdExchangeRate}（FROM→构造币种走映射重写分支，TO→seed CNY 走「映射外目标库
 * 已存在」分支——M0.1 Decision F 双分支）。录制会话构造后<b>不删除</b>（脏环境判据形态），
 * 导入断言：base 对账 skip、seed 行业务键值不变、payload 按 manifest 行数落地。
 *
 * <p>bundle 仅落 {@code target/}（运行时产物不入库——repo 级敏感门控移交义务不触发，
 * successor = 首个真实 bundle 入库的 plan）。脏环境 payload 落地为新增行（与构造行并存）
 * 是 roadmap ② 判据形态，与 M1.2 Decision G「一次性导入 + fresh 库」消费纪律不悖。
 */
public class TestErpFixtureBundleDirtyImport extends ErpIntegrationTestCase {

    private static final File BUNDLE_DIR = new File("target/fixture-bundle-it/dirty-import");

    private IOrmEntity probeCurrency(IOrmSession orm, String code) {
        IOrmEntity probe = orm.newEntity("app.erp.md.dao.entity.ErpMdCurrency");
        probe.orm_propValueByName("code", code);
        return probe;
    }

    private IOrmEntity newCurrency(IOrmSession orm, String code, String name) {
        IOrmEntity currency = orm.newEntity("app.erp.md.dao.entity.ErpMdCurrency");
        currency.orm_propValueByName("code", code);
        currency.orm_propValueByName("name", name);
        currency.orm_propValueByName("symbol", "T");
        currency.orm_propValueByName("decimalPlaces", 2);
        currency.orm_propValueByName("isFunctional", false);
        currency.orm_propValueByName("isActive", true);
        return currency;
    }

    @Test
    public void testDirtyImportBaseNotOverwrittenAndPayloadLands() {
        IOrmTemplate orm = (IOrmTemplate) BeanContainer.getBeanByType(IOrmTemplate.class);

        int rateCountBefore = orm.runInSession(sess ->
                sess.findAllByExample(sess.newEntity("app.erp.md.dao.entity.ErpMdExchangeRate"), null).size());

        // record + construct: TST currency (base) + exchange rate (payload, FROM→TST, TO→seed CNY id=1)
        Object[] ids = new Object[2];
        FixtureBundleRecordingSession session = new FixtureBundleRecordingSession();
        session.run(hook -> orm.runInSession(s -> {
            IOrmEntity currency = newCurrency(s, "TST", "验收币种");
            s.save(currency);
            ids[0] = currency.orm_propValueByName("id");

            IOrmEntity rate = s.newEntity("app.erp.md.dao.entity.ErpMdExchangeRate");
            rate.orm_propValueByName("fromCurrencyId", ids[0]);
            rate.orm_propValueByName("toCurrencyId", 1L);
            rate.orm_propValueByName("rate", new BigDecimal("1.23450000"));
            rate.orm_propValueByName("validFrom", Date.valueOf("2026-01-01"));
            s.save(rate);
            ids[1] = rate.orm_propValueByName("id");
            return null;
        }));

        FixtureBundleExportConfig config = new FixtureBundleExportConfig("dirty-it", "snap");
        config.addBaseTable("app.erp.md.dao.entity.ErpMdCurrency", List.of("CODE"));
        config.addPayloadTable("app.erp.md.dao.entity.ErpMdExchangeRate");

        BUNDLE_DIR.getParentFile().mkdirs();
        FixtureBundleManifest manifest = new io.nop.autotest.bundle.FixtureBundleExporter()
                .export(BUNDLE_DIR, config, session, orm);
        assertEquals(1, manifest.getBaseTables().get(0).getRowCount());
        assertEquals(1, manifest.getSnapshots().get(0).getTables().get(0).getRowCount());

        // DIRTY import: the constructed rows are still present (never deleted) —
        // base must reconcile-and-skip, payload lands as a new row per the manifest
        FixtureBundleImportResult result = new FixtureBundleImporter().importBundle(BUNDLE_DIR, orm);

        assertEquals(0, result.getBaseImported(), "dirty target: base must reconcile, not insert");
        assertEquals(1, result.getBaseSkipped(), "the constructed TST currency must be reconciled by business key");
        assertEquals(1, result.getPayloadImported(), "payload must land per the manifest row count");

        // seed base rows' business-key values unchanged (reconciliation never overwrites)
        orm.runInSession(s -> {
            IOrmEntity cny = s.findFirstByExample(probeCurrency(s, "CNY"));
            assertNotNull(cny);
            assertEquals("人民币", cny.orm_propValueByName("name"));
            return null;
        });

        // payload landed: rate count = before + manifest payload rowCount,
        // and the NEW row kept TO → seed CNY (mapping-external, target-exists branch)
        int rateCountAfter = orm.runInSession(sess ->
                sess.findAllByExample(sess.newEntity("app.erp.md.dao.entity.ErpMdExchangeRate"), null).size());
        // dirty-env bookkeeping: +1 the constructed row (measured after `before`) and
        // +1 the imported row — the manifest payload rowCount lands on top of the
        // untouched constructed row (roadmap ② judgment form; Decision G discipline)
        assertEquals(rateCountBefore + 1 + manifest.getSnapshots().get(0).getTables().get(0).getRowCount(),
                rateCountAfter);

        Object landedRateId = result.getMaxNewIds().get("app.erp.md.dao.entity.ErpMdExchangeRate");
        assertNotNull(landedRateId);
        orm.runInSession(s -> {
            IOrmEntity landed = s.get("app.erp.md.dao.entity.ErpMdExchangeRate", landedRateId);
            assertNotNull(landed);
            // TO_CURRENCY_ID is declared stdDataType=string (FK-column convention) — the
            // entity prop contract is the string form of the target id; compare against
            // the seed CNY row's id rather than a literal Long
            IOrmEntity cny = s.findFirstByExample(probeCurrency(s, "CNY"));
            assertEquals(String.valueOf(cny.orm_propValueByName("id")),
                    String.valueOf(landed.orm_propValueByName("toCurrencyId")),
                    "landed payload row must keep TO → seed CNY (mapping-external target-exists branch)");
            return null;
        });
    }

}
