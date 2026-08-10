package app.erp.md.service;

import app.erp.common.service.MaskHelper;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.service.entity.ErpMdMaterialSkuBizModel;
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
 * E3.1 后端响应层脱敏 md 供应商价格面单元测试（plan 2026-08-10-2059-2 Phase 3 Proof）。
 *
 * <p>覆盖 ErpMdMaterialSku purchasePrice/salePrice/wholesalePrice：授权角色（采购员/管理员）见明文，
 * 非授权见 null，无上下文 fail-closed。
 *
 * <p>E3.2 取值豁免不变量（load-bearing）：@BizLoader 仅作用于 GraphQL/BizModel 边界，
 * CostRollupService/StandardCostResolver 经 DAO 直读不经此 loader（守卫测试复跑见 mfg-service Phase 4）。
 */
public class TestErpMdResponseMasking extends BaseTestCase {

    private static final BigDecimal PRICE = new BigDecimal("456.7890");

    private final ErpMdMaterialSkuBizModel skuBiz = new ErpMdMaterialSkuBizModel();

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
    public void skuPriceAuthorizedRolesSeePlaintext() {
        loginAs(MaskHelper.ROLE_PURCHASER);
        assertPlaintext("采购员");
        loginAs(MaskHelper.ROLE_BIZ_ADMIN);
        assertPlaintext("管理员");
    }

    @Test
    public void skuPriceUnauthorizedSeesNull() {
        loginAs("STAFF");
        assertNull(skuBiz.purchasePriceMask(newSku()), "非授权 purchasePrice = null");
        assertNull(skuBiz.salePriceMask(newSku()), "非授权 salePrice = null");
        assertNull(skuBiz.wholesalePriceMask(newSku()), "非授权 wholesalePrice = null");
    }

    @Test
    public void noContextFailClosed() {
        IUserContext.set(null);
        assertNull(skuBiz.purchasePriceMask(newSku()), "无上下文 purchasePrice = null（fail-closed）");
    }

    private void assertPlaintext(String label) {
        assertEquals(0, PRICE.compareTo(skuBiz.purchasePriceMask(newSku())), label + "见 purchasePrice 明文");
        assertEquals(0, PRICE.compareTo(skuBiz.salePriceMask(newSku())), label + "见 salePrice 明文");
        assertEquals(0, PRICE.compareTo(skuBiz.wholesalePriceMask(newSku())), label + "见 wholesalePrice 明文");
    }

    private void loginAs(String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId("md-mask-test");
        ctx.setUserName("md-mask-test");
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private ErpMdMaterialSku newSku() {
        ErpMdMaterialSku s = new ErpMdMaterialSku();
        s.setPurchasePrice(PRICE);
        s.setSalePrice(PRICE);
        s.setWholesalePrice(PRICE);
        return s;
    }
}
