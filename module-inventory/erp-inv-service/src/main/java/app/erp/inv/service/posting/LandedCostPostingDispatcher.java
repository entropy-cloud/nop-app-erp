package app.erp.inv.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.dao.entity.ErpInvLandedCostLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.costing.LandedCostAllocationEngine;
import app.erp.md.dao.AcctSchemaResolver;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 到岸成本过账派发器（plan 2026-07-10-1100-3）。
 *
 * <p>审核末尾组装 {@link PostingEvent}(LANDED_COST) 经 {@link IErpFinVoucherBiz} 调用财务过账引擎。
 * billHeadCode = 到岸成本单 code（幂等/红冲键）。
 *
 * <p>失败语义对齐 {@link CostAdjustmentPostingDispatcher}：过账失败吞异常返回 null
 * （保持 posted=false），不阻塞审核终态。
 *
 * <p>{@link #reverse(ErpInvLandedCost)}（plan 2026-07-18-1745-2）：到岸成本红冲入口，
 * billHeadCode 与正向 {@link #tryPost} 对称（{@code landedCost.code} 无后缀），委派
 * {@link InvPostingExecutor#reverse} → {@link IErpFinVoucherBiz#reverse} 生成红字凭证 +
 * 原凭证 isReversed=true（platform 内置幂等守护，无凭证时安全 no-op）。
 */
public class LandedCostPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(LandedCostPostingDispatcher.class);

    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    InvPostingExecutor postingExecutor;

    public void setPostingExecutor(InvPostingExecutor postingExecutor) {
        this.postingExecutor = postingExecutor;
    }

    public Long tryPost(ErpInvLandedCost landedCost, List<ErpInvLandedCostLine> costLines,
                          List<LandedCostAllocationEngine.AllocationResult> allocations) {
        PostingEvent event = buildEvent(landedCost, costLines, allocations);
        try {
            return postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("到岸成本过账失败，单 {} 保持 posted=false：{}", landedCost.getCode(), e.getMessage());
            } else {
                LOG.error("到岸成本过账异常，单 {} 保持 posted=false", landedCost.getCode(), e);
            }
            return null;
        }
    }

    /**
     * 红冲指定到岸成本单的 GL 凭证（{@code ErpInvLandedCost.reverseApprove} 触发）。
     *
     * <p>billHeadCode = {@code landedCost.code} 与正向 {@link #tryPost} 对称（无后缀，经独立草案审查核实）；
     * 委派 {@link InvPostingExecutor#reverse} → {@link IErpFinVoucherBiz#reverse} 生成红字凭证 +
     * 标记原凭证 isReversed=true（platform 内置幂等守护，无凭证时安全 no-op）。
     *
     * <p>红冲失败由调用方（{@code ErpInvLandedCostBizModel.reverseApprove}）以 try/catch 吞异常告警
     * 保持幂等（对齐 {@link #tryPost} 正向过账范式）。
     */
    public void reverse(ErpInvLandedCost landedCost) {
        if (landedCost == null || landedCost.getCode() == null) {
            return;
        }
        String billHeadCode = landedCost.getCode();
        postingExecutor.reverse(billHeadCode, ErpFinBusinessType.LANDED_COST);
    }

    private Long postEvent(PostingEvent event) {
        return voucherBiz.post(event, ctx());
    }

    private PostingEvent buildEvent(ErpInvLandedCost landedCost, List<ErpInvLandedCostLine> costLines,
                                      List<LandedCostAllocationEngine.AllocationResult> allocations) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.LANDED_COST);
        event.setBillHeadCode(landedCost.getCode());
        event.setOrgId(landedCost.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(landedCost.getOrgId()));
        event.setCurrencyId(landedCost.getCurrencyId());
        event.setExchangeRate(landedCost.getExchangeRate() != null ? landedCost.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = landedCost.getBusinessDate() != null ? landedCost.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();

        // 借方：每入库行分摊结果
        List<Map<String, Object>> allocList = new ArrayList<>(allocations.size());
        for (LandedCostAllocationEngine.AllocationResult r : allocations) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("receiveLineId", r.getReceiveLineId());
            row.put("materialId", r.getMaterialId());
            row.put("warehouseId", r.getWarehouseId());
            row.put("allocatedAmount", r.getAllocatedAmount());
            allocList.add(row);
        }
        billData.put(ErpInvConstants.BILL_DATA_LANDED_COST_ALLOCATIONS, allocList);

        // 贷方：每费用要素
        List<Map<String, Object>> elemList = new ArrayList<>(costLines.size());
        for (ErpInvLandedCostLine cl : costLines) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("costElement", cl.getCostElement());
            row.put("amount", cl.getAmount());
            row.put("apPartnerId", cl.getApPartnerId() != null ? cl.getApPartnerId() : landedCost.getSupplierId());
            elemList.add(row);
        }
        billData.put(ErpInvConstants.BILL_DATA_LANDED_COST_COST_ELEMENTS, elemList);

        event.setBillData(billData);
        return event;
    }

    private IServiceContext ctx() {
        IServiceContext context = IServiceContext.getCtx();
        return context != null ? context : new ServiceContextImpl();
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }
}
