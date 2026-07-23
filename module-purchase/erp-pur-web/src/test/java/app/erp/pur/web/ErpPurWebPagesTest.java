
package app.erp.pur.web;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.web.page.PageProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE)
@Tag("full-app")
public class ErpPurWebPagesTest extends JunitBaseTestCase {

    @Inject
    PageProvider pageProvider;

    @Test
    public void testValidateAllPages() {
        pageProvider.validateAllPages();
    }
}
