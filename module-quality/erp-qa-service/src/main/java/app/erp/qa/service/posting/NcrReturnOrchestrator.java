package app.erp.qa.service.posting;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.pur.biz.IErpPurReturnBiz;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.service.ErpQaErrors;
import app.erp.sal.biz.IErpSalReturnBiz;
import app.erp.sal.dao.entity.ErpSalReturn;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * NCR→RETURN 处置退货编排器（plan 2026-07-05-2352-2 Phase 3）。
 *
 * <p>resolve 时 dispositionType=RETURN → 按 NCR 来源判定采购/销售退货域：
 * <ul>
 *   <li>supplierId 非空 → 采购退货（{@link IErpPurReturnBiz#save}）——来料不合格退回供应商。</li>
 *   <li>supplierId 为空 → 销售退货（{@link IErpSalReturnBiz#save}）——完工/出货不合格客户退回。</li>
 * </ul>
 *
 * <p>退货单自带审批 + 反向库存 + 红字过账（PURCHASE_RETURN/SALES_RETURN）+ 负辅助账（既有机制）。
 * NCR 侧仅登记关联退货单 code（单一过账来源原则——退货域独占凭证，NCR 不重复过账）。
 *
 * <p><b>跨域 Bean 延迟查找</b>：{@link IErpPurReturnBiz}/{@link IErpSalReturnBiz} 经
 * {@link IBeanContainer#tryGetBeanByType} 延迟查找，避免 standalone 测试无 purchase/sales-service 时
 * Bean 初始化失败（对齐 reactor-cycle test-mock 范式）。
 */
public class NcrReturnOrchestrator {

    @Inject
    @Nullable
    IErpPurReturnBiz purReturnBiz;

    @Inject
    @Nullable
    IErpSalReturnBiz salReturnBiz;

    @Inject
    IDaoProvider daoProvider;

    public void setPurReturnBiz(IErpPurReturnBiz purReturnBiz) {
        this.purReturnBiz = purReturnBiz;
    }

    public void setSalReturnBiz(IErpSalReturnBiz salReturnBiz) {
        this.salReturnBiz = salReturnBiz;
    }

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 按 NCR 来源编排退货域。成功后 NCR.returnCode 登记关联退货单单号。
     */
    public void orchestrateReturn(ErpQaNonConformance ncr, IServiceContext context) {
        Long warehouseId = resolveWarehouseId(ncr.getMaterialId());
        Long currencyId = resolveCurrencyId(ncr.getMaterialId());
        if (ncr.getSupplierId() != null) {
            String returnCode = createPurchaseReturn(ncr, warehouseId, currencyId, context);
            ncr.setReturnCode(returnCode);
        } else {
            String returnCode = createSalesReturn(ncr, warehouseId, currencyId, context);
            ncr.setReturnCode(returnCode);
        }
    }

    private String createPurchaseReturn(ErpQaNonConformance ncr, Long warehouseId, Long currencyId, IServiceContext context) {
        if (purReturnBiz == null) {
            throw new NopException(ErpQaErrors.ERR_NCR_DISPOSITION_NOT_POSTABLE)
                    .param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode())
                    .param(ErpQaErrors.ARG_DISPOSITION_TYPE, "RETURN(purchase)");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "PR-FROM-NCR-" + ncr.getId());
        data.put("supplierId", ncr.getSupplierId());
        data.put("warehouseId", warehouseId);
        data.put("currencyId", currencyId);
        data.put("businessDate", resolveBusinessDate(ncr));
        data.put("docStatus", "DRAFT");
        data.put("approveStatus", "UNSUBMITTED");
        data.put("remark", "NCR退货:" + ncr.getCode());
        ErpPurReturn purReturn = purReturnBiz.save(data, context);
        return purReturn != null ? purReturn.getCode() : null;
    }

    private String createSalesReturn(ErpQaNonConformance ncr, Long warehouseId, Long currencyId, IServiceContext context) {
        if (salReturnBiz == null) {
            throw new NopException(ErpQaErrors.ERR_NCR_DISPOSITION_NOT_POSTABLE)
                    .param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode())
                    .param(ErpQaErrors.ARG_DISPOSITION_TYPE, "RETURN(sales)");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "SR-FROM-NCR-" + ncr.getId());
        data.put("warehouseId", warehouseId);
        data.put("currencyId", currencyId);
        data.put("businessDate", resolveBusinessDate(ncr));
        data.put("docStatus", "DRAFT");
        data.put("approveStatus", "UNSUBMITTED");
        data.put("remark", "NCR退货:" + ncr.getCode());
        ErpSalReturn salReturn = salReturnBiz.save(data, context);
        return salReturn != null ? salReturn.getCode() : null;
    }

    private Long resolveWarehouseId(Long materialId) {
        ErpInvStockBalance balance = findStockBalance(materialId);
        return balance != null ? balance.getWarehouseId() : null;
    }

    private Long resolveCurrencyId(Long materialId) {
        ErpInvStockBalance balance = findStockBalance(materialId);
        return balance != null ? balance.getCurrencyId() : null;
    }

    private ErpInvStockBalance findStockBalance(Long materialId) {
        if (materialId == null) {
            return null;
        }
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.setLimit(1);
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private LocalDate resolveBusinessDate(ErpQaNonConformance ncr) {
        if (ncr.getNcrDate() != null) {
            return ncr.getNcrDate();
        }
        return CoreMetrics.today();
    }
}
