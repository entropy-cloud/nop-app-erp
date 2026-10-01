package io.nop.app.all.it;

import io.nop.core.context.IServiceContext;
import io.nop.api.core.ioc.BeanContainer;
import io.nop.autotest.bundle.FixtureBundleImportResult;
import io.nop.autotest.bundle.FixtureBundleImporter;
import io.nop.orm.IOrmTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * fixture-bundle M2.1 batch entry processor (plan 2026-10-02-0026-1, route (a)).
 * <p>
 * Registered as bean {@code fixtureBundleImportProcessor} in an explicitly-loaded test
 * beans file ({@code @NopTestConfig(testBeansFile=...)}) — the framework classes are
 * deliberately NOT IoC beans (M1.1 approval freeze: no auto-assembly entries in
 * io.nop.autotest.bundle), so the consuming side owns the explicit bean definition and
 * instantiates the framework classes directly. Never touches nop-batch-biz
 * {@code IBizEntityImporter} (roadmap cross-cutting 2: placeholder implementation).
 * <p>
 * Loader-drain contract: {@link #isDone()} flips after the first import so the batch
 * loader's sentinel row pattern terminates on the next chunk (the ap-document batch
 * lesson: a constantly-non-empty loader loops forever).
 */
public class FixtureBundleImportBatchProcessor {
    private volatile boolean done;
    private FixtureBundleImportResult last;

    public boolean isDone() {
        return done;
    }

    public FixtureBundleImportResult getLast() {
        return last;
    }

    public Map<String, Object> runOnce(IServiceContext context) {
        if (!done) {
            File bundleDir = new File("target/fixture-bundle-batch/entry");
            IOrmTemplate orm = BeanContainer.getBeanByType(IOrmTemplate.class);
            last = new FixtureBundleImporter().importBundle(bundleDir, orm);
            done = true;
        }
        Map<String, Object> out = new HashMap<>();
        out.put("baseImported", last == null ? 0 : last.getBaseImported());
        out.put("payloadImported", last == null ? 0 : last.getPayloadImported());
        return out;
    }
}
