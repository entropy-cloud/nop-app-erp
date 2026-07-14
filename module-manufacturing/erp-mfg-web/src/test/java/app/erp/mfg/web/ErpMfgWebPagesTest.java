
package app.erp.mfg.web;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.web.page.PageProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE)
@Disabled("WebPagesTest requires full app classpath (all module page resources). Run in app-erp-all context.")
public class ErpMfgWebPagesTest extends JunitBaseTestCase {

    @Inject
    PageProvider pageProvider;

    @Test
    public void testValidateAllPages() {
        pageProvider.validateAllPages();
    }
}
