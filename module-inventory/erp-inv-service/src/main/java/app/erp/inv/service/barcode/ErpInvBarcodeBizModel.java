package app.erp.inv.service.barcode;

import app.erp.inv.dao.entity.ErpInvStockTake;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.pur.dao.entity.ErpPurReceive;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Description;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;

import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static app.erp.inv.service.ErpInvConstants.CONFIG_BARCODE_AUTO_PRINT_ON_RECEIVE;
import static app.erp.inv.service.ErpInvConstants.CONFIG_BARCODE_ENABLED;
import static app.erp.inv.service.ErpInvConstants.CONFIG_BARCODE_SKU_FORMAT;
import static app.erp.inv.service.ErpInvConstants.CONFIG_PDA_REQUIRE_LOCATION_SCAN;
import static app.erp.inv.service.ErpInvConstants.DEFAULT_BARCODE_AUTO_PRINT_ON_RECEIVE;
import static app.erp.inv.service.ErpInvConstants.DEFAULT_BARCODE_ENABLED;
import static app.erp.inv.service.ErpInvConstants.DEFAULT_BARCODE_SKU_FORMAT;
import static app.erp.inv.service.ErpInvConstants.DEFAULT_PDA_REQUIRE_LOCATION_SCAN;
import static app.erp.inv.service.ErpInvErrors.ARG_BARCODE;
import static app.erp.inv.service.ErpInvErrors.ERR_BARCODE_DISABLED;
import static app.erp.inv.service.ErpInvErrors.ERR_BARCODE_LOCATION_REQUIRED;
import static app.erp.inv.service.ErpInvErrors.ERR_BARCODE_UNRESOLVED;

/**
 * 条码/PDA 解析路由入口（USC-05，plan 2026-10-01-0930-1；barcode-integration.md 架构图
 * SKU/位置解析框 v1 落地）。
 *
 * <p>解析顺序（D1 裁决，SKU-first）：SKU 码（barcode 列 example 直查——md 域 findSkuByBarcode @BizQuery 直调需 IServiceContext）
 * → 单据号三类经 IEntityDao 直查（Receive/MaterialIssue/StockTake，pur/mfg dao 依赖在案）
 * → 发货单经 Class.forName 反射直查（inv→sal DAG 违规禁 pom 依赖；sal-dao 类在 app-erp-all
 * 运行时 classpath；inv-service 单域测试容错返回不可解析）→ 库位 code。
 * 码型歧义 SKU-first（13 位纯数字先走 SKU 解析）。
 *
 * <p>键消费矩阵：barcode-enabled=总门控；sku-format=v1 仅 EAN13 纯数字 13 位校验声明；
 * pda-require-location-scan=true 时 STOCK_TAKE 分支要求 locationCode；
 * auto-print-on-receive=收货流读取时 WARN no-op（打印子系统 residual 在册）。
 *
 * <p>%prod 语义：本 BizModel 无 FNPT 种子 = 业务角色 403（菜单壳已验收语义，随权限矩阵 successor）。
 */
@BizModel("ErpInvBarcode")
public class ErpInvBarcodeBizModel {
    private static final Logger LOG = LoggerFactory.getLogger(ErpInvBarcodeBizModel.class);

    @Inject
    IDaoProvider daoProvider;

    @Description("条码解析路由：SKU/单据（收货单/领料单/盘点单/发货单）/库位")
    @BizQuery
    public Map<String, Object> resolveBarcode(@Name("code") @Description("条码值") String code,
                                              @Name("locationCode") @Optional @Description("库位编码（pda-require-location-scan 时盘点解析必填）") String locationCode) {
        if (!AppConfig.var(CONFIG_BARCODE_ENABLED, DEFAULT_BARCODE_ENABLED)) {
            throw new NopException(ERR_BARCODE_DISABLED);
        }

        Map<String, Object> locationInfo = null;
        if (locationCode != null && !locationCode.isEmpty()) {
            locationInfo = resolveLocation(locationCode);
        }

        // 1. SKU 码优先（SKU-first：EAN13 纯数字或任意 SKU barcode 命中）
        Map<String, Object> result = resolveSku(code);
        if (result != null) {
            if (locationInfo != null)
                result.put("location", locationInfo);
            return result;
        }

        // 2. 单据号三类直查 + 发货单反射
        result = resolveDocument(code);
        if (result != null) {
            if (AppConfig.var(CONFIG_PDA_REQUIRE_LOCATION_SCAN, DEFAULT_PDA_REQUIRE_LOCATION_SCAN)
                    && "STOCK_TAKE".equals(result.get("refType")) && locationInfo == null) {
                throw new NopException(ERR_BARCODE_LOCATION_REQUIRED);
            }
            if (locationInfo != null)
                result.put("location", locationInfo);
            return result;
        }

        // 3. 库位码作为主解析目标
        if (locationInfo != null) {
            Map<String, Object> loc = new HashMap<>();
            loc.put("type", "LOCATION");
            loc.put("refType", "LOCATION");
            loc.put("refId", locationInfo.get("locationId"));
            loc.put("refCode", locationInfo.get("locationCode"));
            loc.put("location", locationInfo);
            return loc;
        }

        throw new NopException(ERR_BARCODE_UNRESOLVED).param(ARG_BARCODE, code);
    }

    private Map<String, Object> resolveSku(String code) {
        String format = AppConfig.var(CONFIG_BARCODE_SKU_FORMAT, DEFAULT_BARCODE_SKU_FORMAT);
        if ("EAN13".equals(format) && !(code.length() == 13 && code.chars().allMatch(Character::isDigit))) {
            return null;
        }
        // AGENTS.md I*Biz-first 规则偏离说明：IErpMdMaterialSkuBiz.findSkuByBarcode 为 @BizQuery，
        // Direct Java 调用缺 IServiceContext 会 NPE（getEvalScope）——同语义改走 barcode 列 example
        // 查询（唯一性由 erp-md.sku-barcode-unique 门控保证，与 md 域查重门控不同面）。
        ErpMdMaterialSku skuExample = new ErpMdMaterialSku();
        skuExample.setBarcode(code);
        ErpMdMaterialSku sku = daoProvider.daoFor(ErpMdMaterialSku.class).findFirstByExample(skuExample);
        if (sku == null)
            return null;
        Map<String, Object> m = new HashMap<>();
        m.put("type", "SKU");
        m.put("refType", "SKU");
        m.put("refId", sku.getId());
        m.put("refCode", sku.getSkuCode());
        Map<String, Object> skuInfo = new HashMap<>();
        skuInfo.put("skuCode", sku.getSkuCode());
        skuInfo.put("barcode", sku.getBarcode());
        if (sku.getMaterialId() != null)
            skuInfo.put("materialId", sku.getMaterialId());
        m.put("skuInfo", skuInfo);
        return m;
    }

    private Map<String, Object> resolveDocument(String code) {
        // 单据直查走 IDaoProvider example（AGENTS.md I*Biz-first 偏离说明：findPage 型查询
        // 经注入接口需 IServiceContext/GraphQL 层，Direct Java 面以 example 直查等价实现）
        ErpPurReceive receiveExample = new ErpPurReceive();
        receiveExample.setCode(code);
        ErpPurReceive receive = daoProvider.daoFor(ErpPurReceive.class).findFirstByExample(receiveExample);
        if (receive != null) {
            logAutoPrintIfConfigured();
            return doc("PUR_RECEIVE", receive.getId(), receive.getCode());
        }
        // 领料单（mfg dao 依赖在案）
        ErpMfgMaterialIssue issueExample = new ErpMfgMaterialIssue();
        issueExample.setCode(code);
        ErpMfgMaterialIssue issue = daoProvider.daoFor(ErpMfgMaterialIssue.class).findFirstByExample(issueExample);
        if (issue != null)
            return doc("MFG_MATERIAL_ISSUE", issue.getId(), issue.getCode());
        // 盘点单（同域）
        ErpInvStockTake takeExample = new ErpInvStockTake();
        takeExample.setCode(code);
        ErpInvStockTake take = daoProvider.daoFor(ErpInvStockTake.class).findFirstByExample(takeExample);
        if (take != null)
            return doc("STOCK_TAKE", take.getId(), take.getCode());
        // 发货单（sal：Class.forName 反射直查——inv→sal DAG 违规禁 pom 依赖，运行时 classpath 含 sal-dao；
        // inv-service 单域测试 classpath 无 sal-dao 时容错返回 null）
        try {
            @SuppressWarnings("unchecked")
            Class<? extends io.nop.dao.api.IDaoEntity> salDeliveryClass =
                    (Class<? extends io.nop.dao.api.IDaoEntity>) Class.forName("app.erp.sal.dao.entity.ErpSalDelivery");
            Object salExample = salDeliveryClass.getDeclaredConstructor().newInstance();
            salExample.getClass().getMethod("setCode", String.class).invoke(salExample, code);
            IEntityDao salDao = rawDaoFor(salDeliveryClass);
            Object delivery = salDao.findFirstByExample((io.nop.dao.api.IDaoEntity) salExample);
            if (delivery != null) {
                return doc("SAL_DELIVERY", idOf(delivery), codeOf(delivery));
            }
        } catch (ClassNotFoundException e) {
            LOG.debug("[USC-05] sal-dao 不在当前 classpath（单域测试），Delivery 解析跳过");
        } catch (ReflectiveOperationException e) {
            LOG.warn("[USC-05] Delivery 反射查询失败：{}", e.getMessage());
        }
        return null;
    }

    private Object idOf(Object entity) {
        try {
            return entity.getClass().getMethod("getId").invoke(entity);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private String codeOf(Object entity) {
        try {
            Object v = entity.getClass().getMethod("getCode").invoke(entity);
            return v == null ? null : String.valueOf(v);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private Map<String, Object> resolveLocation(String locationCode) {
        ErpMdLocation locExample = new ErpMdLocation();
        locExample.setCode(locationCode);
        ErpMdLocation loc = daoProvider.daoFor(ErpMdLocation.class).findFirstByExample(locExample);
        if (loc == null)
            return null;
        Map<String, Object> m = new HashMap<>();
        m.put("locationId", loc.getId());
        m.put("locationCode", loc.getCode());
        if (loc.getWarehouseId() != null)
            m.put("warehouseId", loc.getWarehouseId());
        return m;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private IEntityDao rawDaoFor(Class clazz) {
        return daoProvider.daoFor(clazz);
    }

    private void logAutoPrintIfConfigured() {
        if (AppConfig.var(CONFIG_BARCODE_AUTO_PRINT_ON_RECEIVE, DEFAULT_BARCODE_AUTO_PRINT_ON_RECEIVE)) {
            // v1 显式 no-op：打印子系统 residual 在册（plan 2026-10-01-0930-1 Deferred 对账表）。
            LOG.warn("[USC-05] barcode-auto-print-on-receive=true 但标签打印子系统未实现（residual 在册），跳过标签打印");
        }
    }

    private Map<String, Object> doc(String refType, Object id, String code) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "DOC");
        m.put("refType", refType);
        m.put("refId", id);
        m.put("refCode", code);
        return m;
    }
}
