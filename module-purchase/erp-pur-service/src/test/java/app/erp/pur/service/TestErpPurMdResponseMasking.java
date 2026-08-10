package app.erp.pur.service;

import app.erp.common.service.MaskHelper;
import app.erp.pur.dao.entity.ErpPurSupplierPriceList;
import app.erp.pur.service.entity.ErpPurSupplierPriceListBizModel;
import io.nop.api.core.auth.IUserContext;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * E3.1 后端响应层脱敏 pur 供应商价格面单元测试（plan 2026-08-10-2059-2 Phase 3 Proof）。
 *
 * <p>覆盖 ErpPurSupplierPriceList.unitPrice：授权角色（采购员/管理员）见明文，非授权见 null，无上下文 fail-closed。
 * ErpMdMaterialSku 价格字段测试见 md-service {@code TestErpMdResponseMasking}（pur-service 不依赖 md-service）。
 */
public class TestErpPurMdResponseMasking extends BaseTestCase {

    private static final BigDecimal PRICE = new BigDecimal("123.4567");

    private final ErpPurSupplierPriceListBizModel priceListBiz = new ErpPurSupplierPriceListBizModel();

    private IUserContext prevCtx;

    @BeforeEach
    void saveContext() {
        prevCtx = IUserContext.get();
    }

    @AfterEach
    void restoreContext() {
        IUserContext.set(prevCtx);
    }

    @Test
    public void unitPriceAuthorizedRolesSeePlaintext() {
        loginAs(MaskHelper.ROLE_PURCHASER);
        assertEquals(0, PRICE.compareTo(priceListBiz.unitPriceMask(newPriceList())), "采购员见 unitPrice 明文");
        loginAs(MaskHelper.ROLE_BIZ_ADMIN);
        assertEquals(0, PRICE.compareTo(priceListBiz.unitPriceMask(newPriceList())), "管理员见 unitPrice 明文");
    }

    @Test
    public void unitPriceUnauthorizedSeesNull() {
        loginAs("STAFF");
        assertNull(priceListBiz.unitPriceMask(newPriceList()), "非授权 unitPrice = null");
    }

    @Test
    public void noContextFailClosed() {
        IUserContext.set(null);
        assertNull(priceListBiz.unitPriceMask(newPriceList()), "无上下文 unitPrice = null（fail-closed）");
    }

    private void loginAs(String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId("pur-mask-test");
        ctx.setUserName("pur-mask-test");
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private ErpPurSupplierPriceList newPriceList() {
        ErpPurSupplierPriceList p = new ErpPurSupplierPriceList();
        p.setUnitPrice(PRICE);
        return p;
    }
}
