package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IEntityDao;
import static io.nop.api.core.beans.FilterBeans.eq;
import app.erp.common.service.AbstractErpCrudBizModel;
import app.erp.inv.biz.IErpInvStockTakeLineBiz;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockTake;
import app.erp.inv.dao.entity.ErpInvStockTakeLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdMaterialSku;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static app.erp.inv.service.ErpInvConstants.CONFIG_BARCODE_ENABLED;
import static app.erp.inv.service.ErpInvConstants.CONFIG_PDA_REQUIRE_LOCATION_SCAN;
import static app.erp.inv.service.ErpInvConstants.DEFAULT_BARCODE_ENABLED;
import static app.erp.inv.service.ErpInvConstants.DEFAULT_PDA_REQUIRE_LOCATION_SCAN;
import static app.erp.inv.service.ErpInvErrors.ERR_BARCODE_DISABLED;
import static app.erp.inv.service.ErpInvErrors.ERR_BARCODE_LOCATION_REQUIRED;
import static app.erp.inv.service.ErpInvErrors.ERR_BARCODE_LOCATION_UNKNOWN;
import static app.erp.inv.service.ErpInvErrors.ERR_BARCODE_LOCATION_WAREHOUSE_MISMATCH;
import static app.erp.inv.service.ErpInvErrors.ERR_BARCODE_SKU_UNKNOWN;
import static app.erp.inv.service.ErpInvErrors.ERR_INV_TAKE_NOT_IN_COUNTING;
import static app.erp.inv.service.ErpInvErrors.ERR_INV_TAKE_QTY_INVALID;
import static app.erp.inv.service.ErpInvErrors.ARG_ACTUAL_QTY;
import static app.erp.inv.service.ErpInvErrors.ARG_BARCODE;
import static app.erp.inv.service.ErpInvErrors.ARG_CURRENT_STATUS;
import static app.erp.inv.service.ErpInvErrors.ARG_EXPECTED_WAREHOUSE_ID;
import static app.erp.inv.service.ErpInvErrors.ARG_ACTUAL_WAREHOUSE_ID;
import static app.erp.inv.service.ErpInvErrors.ARG_LOCATION_CODE;
import static app.erp.inv.service.ErpInvErrors.ARG_TAKE_ID;

/**
 * 盘点行 BizModel。{@code scanSave}（plan 2026-10-05-0400-2，UC-INV-05/US-IV-05 PDA 场景 5）：
 * PDA 扫码落行单往返入口——服务端派生 materialId/uoMId（SKU 行，resolveBarcode 返回不含 uoMId）、
 * bookQuantity（StockBalance 落行时点快照，对齐 completeTake D1「盘点行字段快照对账」口径，
 * 无余额行=账面 0 全量盘点口径）、differenceQuantity=actual−book、nextLineNo（头下 max+1，
 * 新行语义：每次扫码独立成行，差异聚合在 completeTake 移动单生成侧）。守卫：barcode-enabled
 * 总开关（与 resolveBarcode 同开关）、盘点单 docStatus==CONFIRMED 作业窗口（盘点中：
 * state-machine.md §盘点单状态机，COUNTING↔CONFIRMED 标签漂移行为一致）、库位编码按
 * pda-require-location-scan 必填 + 仓库归属校验。
 *
 * <p>DAO 访问走 CrudBizModel 内建 {@code daoFor(...)}（平台解析 provider，无需字段注入；
 * 生成层 _service.beans.xml 已声明本 BizModel bean，plan 2026-10-05-0400-2 执行期的注入失败
 * 疑为 stale target 误诊——内建访问器消解该面）。
 */
@BizModel("ErpInvStockTakeLine")
public class ErpInvStockTakeLineBizModel extends AbstractErpCrudBizModel<ErpInvStockTakeLine> implements IErpInvStockTakeLineBiz {
    public ErpInvStockTakeLineBizModel(){
        setEntityName(ErpInvStockTakeLine.class.getName());
    }

    @BizMutation
    public ErpInvStockTakeLine scanSave(@Name("takeId") @Optional String takeId,
                                        @Name("takeCode") @Optional String takeCode,
                                        @Name("skuBarcode") String skuBarcode,
                                        @Name("locationCode") @Optional String locationCode,
                                        @Name("actualQuantity") BigDecimal actualQuantity,
                                        IServiceContext context) {
        if (!AppConfig.var(CONFIG_BARCODE_ENABLED, DEFAULT_BARCODE_ENABLED)) {
            throw new NopException(ERR_BARCODE_DISABLED);
        }
        if (actualQuantity == null || actualQuantity.signum() < 0) {
            throw new NopException(ERR_INV_TAKE_QTY_INVALID)
                    .param(ARG_ACTUAL_QTY, actualQuantity);
        }

        // 盘点单定位：takeId 直取或 takeCode 解析（PDA 扫单据条码即编码，D5 修订——
        // flux select 动态选项源在本仓手写页无可用承载，扫/输编号更符合作业惯例）
        ErpInvStockTake take = null;
        if (takeId != null && !takeId.isEmpty()) {
            take = daoFor(ErpInvStockTake.class).getEntityById(takeId);
        } else if (takeCode != null && !takeCode.isEmpty()) {
            ErpInvStockTake codeExample = new ErpInvStockTake();
            codeExample.setCode(takeCode);
            take = daoFor(ErpInvStockTake.class).findFirstByExample(codeExample);
        }
        if (take == null || !Objects.equals(take.getDocStatus(), "CONFIRMED")) {
            throw new NopException(ERR_INV_TAKE_NOT_IN_COUNTING)
                    .param(ARG_TAKE_ID, takeId != null ? takeId : takeCode)
                    .param(ARG_CURRENT_STATUS, take == null ? null : take.getDocStatus());
        }

        boolean requireLocationScan = AppConfig.var(CONFIG_PDA_REQUIRE_LOCATION_SCAN, DEFAULT_PDA_REQUIRE_LOCATION_SCAN);
        if (requireLocationScan && (locationCode == null || locationCode.isEmpty())) {
            throw new NopException(ERR_BARCODE_LOCATION_REQUIRED)
                    .param(ARG_LOCATION_CODE, locationCode);
        }

        ErpMdMaterialSku skuExample = new ErpMdMaterialSku();
        skuExample.setBarcode(skuBarcode);
        ErpMdMaterialSku sku = daoFor(ErpMdMaterialSku.class).findFirstByExample(skuExample);
        if (sku == null) {
            throw new NopException(ERR_BARCODE_SKU_UNKNOWN)
                    .param(ARG_BARCODE, skuBarcode);
        }

        ErpMdLocation location = null;
        if (locationCode != null && !locationCode.isEmpty()) {
            ErpMdLocation locExample = new ErpMdLocation();
            locExample.setCode(locationCode);
            location = daoFor(ErpMdLocation.class).findFirstByExample(locExample);
            if (location == null) {
                throw new NopException(ERR_BARCODE_LOCATION_UNKNOWN)
                        .param(ARG_LOCATION_CODE, locationCode);
            }
            if (location.getWarehouseId() != null && take.getWarehouseId() != null
                    && !location.getWarehouseId().equals(take.getWarehouseId())) {
                throw new NopException(ERR_BARCODE_LOCATION_WAREHOUSE_MISMATCH)
                        .param(ARG_LOCATION_CODE, locationCode)
                        .param(ARG_EXPECTED_WAREHOUSE_ID, take.getWarehouseId())
                        .param(ARG_ACTUAL_WAREHOUSE_ID, location.getWarehouseId());
            }
        }

        // bookQuantity：StockBalance 落行时点快照（仓库+SKU[+库位]），无余额行=账面 0（全量盘点口径）
        ErpInvStockBalance balExample = new ErpInvStockBalance();
        balExample.setWarehouseId(take.getWarehouseId());
        balExample.setSkuId(sku.getId());
        if (location != null)
            balExample.setLocationId(location.getId());
        ErpInvStockBalance balance = daoFor(ErpInvStockBalance.class).findFirstByExample(balExample);
        BigDecimal bookQuantity = balance == null || balance.getTotalQuantity() == null
                ? BigDecimal.ZERO : balance.getTotalQuantity();

        ErpInvStockTakeLine line = new ErpInvStockTakeLine();
        line.setTakeId(take.getId());
        line.setLineNo(nextLineNo(take.getId()));
        line.setSkuId(sku.getId());
        line.setMaterialId(sku.getMaterialId());
        line.setUoMId(sku.getUoMId());
        if (location != null)
            line.setLocationId(location.getId());
        line.setBookQuantity(bookQuantity);
        line.setActualQuantity(actualQuantity);
        line.setDifferenceQuantity(actualQuantity.subtract(bookQuantity));
        saveEntity(line, null, context);
        return line;
    }

    private int nextLineNo(String takeId) {
        // 单查询点查（plan 2026-10-06-0355-5）：orderBy lineNo desc + limit 1，
        // 替代全量物化求 max（扫码热路径 O(N)/次 → O(1)）；IDX_INV_STOCK_TAKE_LINE_TAKE_ID 承载
        QueryBean q = new QueryBean();
        q.addFilter(eq("takeId", takeId));
        q.addOrderField("lineNo", false);
        q.setLimit(1);
        IEntityDao<ErpInvStockTakeLine> dao = daoProvider().daoFor(ErpInvStockTakeLine.class);
        List<ErpInvStockTakeLine> lines = dao.findAllByQuery(q);
        int max = 0;
        if (!lines.isEmpty() && lines.get(0).getLineNo() != null) {
            max = lines.get(0).getLineNo();
        }
        return max + 1;
    }
}
