package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvCostAdjustLine;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.dao.entity.ErpInvLandedCostLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.costing.CostAdjustmentService;
import app.erp.inv.service.costing.LandedCostAllocationEngine;
import app.erp.inv.service.posting.LandedCostPostingDispatcher;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 到岸成本单审核编排 Processor（plan 2026-07-10-1100-3；costing-methods.md §到岸成本分摊）。
 *
 * <p>审核编排步骤（step 方法 protected，下游可逐个覆盖）：
 * <ol>
 *   <li>加载到岸成本单 + 费用行 + 关联采购入库单 + 入库行</li>
 *   <li>校验（入库单已审核 + 防重复分摊）</li>
 *   <li>调用 {@link LandedCostAllocationEngine} 计算分摊结果</li>
 *   <li>创建 {@link ErpInvCostAdjust}(type=LANDED_COST_SUPPLEMENT) + 行</li>
 *   <li>调用 {@link CostAdjustmentService#applyCostAdjust} 直接更新成本层（不走 ErpInvCostAdjustProcessor.applyCostAdjust
 *       的完整审核链，避免 COST_ADJUSTMENT(420) 过账与到岸成本 LANDED_COST 过账双重入账）</li>
 *   <li>状态迁移 + posted 标志</li>
 * </ol>
 *
 * <p>跨域只读：采购入库单头/行经 purchase-dao 直接 DAO 访问（receive 行无 IBiz 查询方法，
 * 与 StandardCostResolver 读 mfg-dao 同范式）。
 */
public class ErpInvLandedCostProcessor {

    static final String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    static final String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    static final String APPROVE_STATUS_APPROVED = "APPROVED";

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    CostAdjustmentService costAdjustmentService;

    @Inject
    LandedCostAllocationEngine allocationEngine;

    @Inject
    LandedCostPostingDispatcher postingDispatcher;

    // ---------- 审核编排 ----------

    public ErpInvLandedCost approve(Long id, IServiceContext context) {
        ErpInvLandedCost landedCost = requireLandedCost(id, context);

        if (Objects.equals(landedCost.getApproveStatus(), APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpInvErrors.ERR_LANDED_COST_ALREADY_APPROVED)
                    .param(ErpInvErrors.ARG_LANDED_COST_CODE, landedCost.getCode());
        }

        List<ErpInvLandedCostLine> costLines = loadCostLines(landedCost.getId());
        if (costLines.isEmpty()) {
            throw new NopException(ErpInvErrors.ERR_LANDED_COST_NO_LINES)
                    .param(ErpInvErrors.ARG_LANDED_COST_CODE, landedCost.getCode());
        }

        ErpPurReceive receive = loadReceive(landedCost.getReceiveId());
        validateReceiveApproved(receive);
        validateNotAlreadyAllocated(landedCost.getReceiveId(), landedCost.getId());

        List<ErpPurReceiveLine> receiveLines = loadReceiveLines(landedCost.getReceiveId());

        List<LandedCostAllocationEngine.AllocationResult> allocations = doAllocate(landedCost, costLines, receiveLines);

        ErpInvCostAdjust costAdjust = createAndApplyCostAdjust(landedCost, receive, allocations);

        doPostApprove(landedCost, costAdjust, costLines, allocations, context);

        return reload(id);
    }

    // ---------- 分摊预览（只读，不落库） ----------

    public List<Map<String, Object>> allocatePreview(Long id, IServiceContext context) {
        ErpInvLandedCost landedCost = requireLandedCost(id, context);
        List<ErpInvLandedCostLine> costLines = loadCostLines(landedCost.getId());
        List<ErpPurReceiveLine> receiveLines = loadReceiveLines(landedCost.getReceiveId());

        BigDecimal totalCost = sumCostLines(costLines);
        List<LandedCostAllocationEngine.ReceiveLineInput> inputs = toInputs(receiveLines);
        List<LandedCostAllocationEngine.AllocationResult> results = allocationEngine.allocate(inputs, totalCost, landedCost.getAllocationMethod());

        List<Map<String, Object>> preview = new ArrayList<>(results.size());
        for (LandedCostAllocationEngine.AllocationResult r : results) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("receiveLineId", r.getReceiveLineId());
            row.put("materialId", r.getMaterialId());
            row.put("warehouseId", r.getWarehouseId());
            row.put("allocatedAmount", r.getAllocatedAmount());
            row.put("newUnitCost", r.getNewUnitCost());
            preview.add(row);
        }
        return preview;
    }

    // ---------- 自动创建（path-2 运费→到岸成本，plan 2026-07-11-2329-1） ----------

    /**
     * 按采购入库单 code + 运费数据自动创建 DRAFT 到岸成本单（FREIGHT 费用行）。
     *
     * <p>内部解析 receiveCode→receiveId（inventory-service 已 compile-scope 依赖 purchase-dao）。
     * 幂等：同 receiveId 已有非 CANCELLED 到岸成本单时抛 {@link ErpInvErrors#ERR_LANDED_COST_DRAFT_EXISTS}。
     */
    public ErpInvLandedCost generateFreightLandedCost(String receiveCode, BigDecimal freightAmount,
                                                       Long freightCurrencyId, BigDecimal freightExchangeRate,
                                                       IServiceContext context) {
        ErpPurReceive receive = loadReceiveByCode(receiveCode);
        if (receive == null) {
            throw new NopException(ErpInvErrors.ERR_LANDED_COST_RECEIVE_NOT_FOUND)
                    .param("receiveCode", receiveCode);
        }

        validateNoDraftExists(receive.getId());

        Long currencyId = freightCurrencyId != null ? freightCurrencyId : receive.getCurrencyId();
        BigDecimal exchangeRate = resolveExchangeRate(freightExchangeRate, freightCurrencyId, receive);

        ErpInvLandedCost landedCost = createLandedCostHead(receive, freightAmount, currencyId, exchangeRate);
        createFreightLine(landedCost, freightAmount, receive.getSupplierId());

        return landedCost;
    }

    protected ErpPurReceive loadReceiveByCode(String receiveCode) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", receiveCode));
        q.addOrderField("code", false);
        return dao.findFirstByQuery(q);
    }

    protected void validateNoDraftExists(Long receiveId) {
        IEntityDao<ErpInvLandedCost> dao = landedCostDao();
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("receiveId", receiveId),
                ne("docStatus", "CANCELLED")
        ));
        List<ErpInvLandedCost> existing = dao.findAllByQuery(q);
        if (!existing.isEmpty()) {
            throw new NopException(ErpInvErrors.ERR_LANDED_COST_DRAFT_EXISTS)
                    .param(ErpInvErrors.ARG_RECEIVE_ID, receiveId)
                    .param(ErpInvErrors.ARG_LANDED_COST_CODE, existing.get(0).getCode());
        }
    }

    protected BigDecimal resolveExchangeRate(BigDecimal freightExchangeRate, Long freightCurrencyId,
                                              ErpPurReceive receive) {
        if (freightExchangeRate != null) {
            return freightExchangeRate;
        }
        return receive.getExchangeRate() != null ? receive.getExchangeRate() : BigDecimal.ONE;
    }

    protected ErpInvLandedCost createLandedCostHead(ErpPurReceive receive, BigDecimal freightAmount,
                                                      Long currencyId, BigDecimal exchangeRate) {
        IEntityDao<ErpInvLandedCost> dao = landedCostDao();
        ErpInvLandedCost head = dao.newEntity();
        head.setCode("LC-FRT-" + receive.getCode() + "-" + CoreMetrics.currentTimeMillis());
        head.setOrgId(receive.getOrgId());
        head.setReceiveId(receive.getId());
        head.setSupplierId(receive.getSupplierId());
        head.setCurrencyId(currencyId);
        head.setExchangeRate(exchangeRate);
        head.setTotalCostAmount(freightAmount);
        head.setAllocationMethod(ErpInvConstants.ALLOC_METHOD_BY_AMOUNT);
        head.setDocStatus(ErpInvConstants.DOC_STATUS_DRAFT);
        head.setApproveStatus(APPROVE_STATUS_UNSUBMITTED);
        head.setPosted(false);
        head.setBusinessDate(CoreMetrics.today());
        dao.saveEntity(head);
        return head;
    }

    protected void createFreightLine(ErpInvLandedCost head, BigDecimal freightAmount, Long apPartnerId) {
        IEntityDao<ErpInvLandedCostLine> dao = daoProvider.daoFor(ErpInvLandedCostLine.class);
        ErpInvLandedCostLine line = dao.newEntity();
        line.setLandedCostId(head.getId());
        line.setLineNo(1);
        line.setCostElement(ErpInvConstants.COST_ELEMENT_FREIGHT);
        line.setAmount(freightAmount);
        line.setApPartnerId(apPartnerId);
        dao.saveEntity(line);
    }

    // ---------- step 方法（protected，下游可覆盖） ----------

    protected List<LandedCostAllocationEngine.AllocationResult> doAllocate(
            ErpInvLandedCost landedCost, List<ErpInvLandedCostLine> costLines,
            List<ErpPurReceiveLine> receiveLines) {
        BigDecimal totalCost = sumCostLines(costLines);
        List<LandedCostAllocationEngine.ReceiveLineInput> inputs = toInputs(receiveLines);
        return allocationEngine.allocate(inputs, totalCost, landedCost.getAllocationMethod());
    }

    @SuppressWarnings("unchecked")
    protected ErpInvCostAdjust createAndApplyCostAdjust(ErpInvLandedCost landedCost, ErpPurReceive receive,
                                                          List<LandedCostAllocationEngine.AllocationResult> allocations) {
        IEntityDao<ErpInvCostAdjust> adjustDao = daoProvider.daoFor(ErpInvCostAdjust.class);
        ErpInvCostAdjust adjust = adjustDao.newEntity();
        adjust.setCode("LC-" + landedCost.getCode());
        adjust.setOrgId(landedCost.getOrgId());
        adjust.setBusinessDate(landedCost.getBusinessDate());
        adjust.setAdjustType(ErpInvConstants.ADJUST_TYPE_LANDED_COST_SUPPLEMENT);
        adjust.setReason("到岸成本分摊：" + landedCost.getCode());
        adjust.setDocStatus(ErpInvConstants.DOC_STATUS_DRAFT);
        adjust.setApproveStatus(APPROVE_STATUS_APPROVED);
        adjust.setPosted(false);
        adjust.setCurrencyId(landedCost.getCurrencyId());
        adjustDao.saveEntity(adjust);

        IEntityDao<ErpInvCostAdjustLine> lineDao = daoProvider.daoFor(ErpInvCostAdjustLine.class);
        List<ErpInvCostAdjustLine> adjustLines = new ArrayList<>();
        int lineNo = 1;
        for (LandedCostAllocationEngine.AllocationResult r : allocations) {
            ErpInvCostAdjustLine line = lineDao.newEntity();
            line.setAdjustId(adjust.getId());
            line.setLineNo(lineNo++);
            line.setMaterialId(r.getMaterialId());
            line.setWarehouseId(r.getWarehouseId() != null ? r.getWarehouseId() : receive.getWarehouseId());
            BigDecimal newUnitCost = r.getNewUnitCost();
            line.setNewUnitCost(newUnitCost);
            line.setAdjustAmount(r.getAllocatedAmount());
            line.setCurrencyId(landedCost.getCurrencyId());
            line.setRemark("到岸成本行 " + r.getReceiveLineId());
            lineDao.saveEntity(line);
            adjustLines.add(line);
        }

        // 直接调用 CostAdjustmentService 更新成本层（不走 ErpInvCostAdjustProcessor.applyCostAdjust，
        // 后者会独立派发 COST_ADJUSTMENT(420) 过账，与 LANDED_COST 过账双重入账）
        costAdjustmentService.applyCostAdjust(adjust, adjustLines);
        ormTemplate.flushSession();

        adjust = adjustDao.getEntityById(adjust.getId());
        adjust.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
        adjustDao.updateEntity(adjust);
        ormTemplate.flushSession();
        return adjust;
    }

    protected void doPostApprove(ErpInvLandedCost landedCost, ErpInvCostAdjust costAdjust,
                                   List<ErpInvLandedCostLine> costLines,
                                   List<LandedCostAllocationEngine.AllocationResult> allocations,
                                   IServiceContext context) {
        // LANDED_COST 过账（借存货/贷应付）
        Long voucherId = postingDispatcher.tryPost(landedCost, costLines, allocations);

        landedCost = reload(landedCost.getId());
        Timestamp now = CoreMetrics.currentTimestamp();
        landedCost.setApproveStatus(APPROVE_STATUS_APPROVED);
        landedCost.setApprovedBy(currentUserId());
        landedCost.setApprovedAt(now);
        landedCost.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
        if (voucherId != null) {
            landedCost.setPosted(true);
            landedCost.setPostedAt(now);
            landedCost.setPostedBy(currentUserId());

            // 同步成本调整单 posted 标志（成本层已在 createAndApplyCostAdjust 更新）
            IEntityDao<ErpInvCostAdjust> adjustDao = daoProvider.daoFor(ErpInvCostAdjust.class);
            ErpInvCostAdjust adjust = adjustDao.getEntityById(costAdjust.getId());
            if (adjust != null) {
                adjust.setPosted(true);
                adjust.setPostedAt(now);
                adjust.setPostedBy(currentUserId());
                adjustDao.updateEntity(adjust);
            }
        }
        landedCostDao().updateEntity(landedCost);
    }

    // ---------- 校验 ----------

    protected void validateReceiveApproved(ErpPurReceive receive) {
        if (receive == null) {
            throw new NopException(ErpInvErrors.ERR_LANDED_COST_RECEIVE_NOT_APPROVED)
                    .param(ErpInvErrors.ARG_RECEIVE_ID, "null");
        }
        String status = receive.getApproveStatus();
        if (!APPROVE_STATUS_APPROVED.equals(status)) {
            throw new NopException(ErpInvErrors.ERR_LANDED_COST_RECEIVE_NOT_APPROVED)
                    .param(ErpInvErrors.ARG_RECEIVE_ID, receive.getId());
        }
    }

    protected void validateNotAlreadyAllocated(Long receiveId, Long currentLandedCostId) {
        IEntityDao<ErpInvLandedCost> dao = landedCostDao();
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("receiveId", receiveId),
                eq("approveStatus", APPROVE_STATUS_APPROVED)
        ));
        List<ErpInvLandedCost> existing = dao.findAllByQuery(q);
        for (ErpInvLandedCost lc : existing) {
            if (!Objects.equals(lc.getId(), currentLandedCostId)) {
                throw new NopException(ErpInvErrors.ERR_LANDED_COST_ALREADY_ALLOCATED)
                        .param(ErpInvErrors.ARG_RECEIVE_ID, receiveId)
                        .param(ErpInvErrors.ARG_LANDED_COST_CODE, lc.getCode());
            }
        }
    }

    // ---------- 加载/查询辅助 ----------

    protected ErpInvLandedCost requireLandedCost(Long id, IServiceContext context) {
        ErpInvLandedCost landedCost = landedCostDao().getEntityById(id);
        if (landedCost == null) {
            throw new NopException(ErpInvErrors.ERR_LANDED_COST_NOT_FOUND)
                    .param(ErpInvErrors.ARG_LANDED_COST_ID, id);
        }
        return landedCost;
    }

    protected ErpInvLandedCost reload(Long id) {
        return landedCostDao().getEntityById(id);
    }

    protected List<ErpInvLandedCostLine> loadCostLines(Long landedCostId) {
        IEntityDao<ErpInvLandedCostLine> dao = daoProvider.daoFor(ErpInvLandedCostLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("landedCostId", landedCostId));
        q.addOrderField("lineNo", false);
        return dao.findAllByQuery(q);
    }

    protected ErpPurReceive loadReceive(Long receiveId) {
        if (receiveId == null) {
            return null;
        }
        return daoProvider.daoFor(ErpPurReceive.class).getEntityById(receiveId);
    }

    protected List<ErpPurReceiveLine> loadReceiveLines(Long receiveId) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("receiveId", receiveId));
        q.addOrderField("lineNo", false);
        return dao.findAllByQuery(q);
    }

    protected IEntityDao<ErpInvLandedCost> landedCostDao() {
        return daoProvider.daoFor(ErpInvLandedCost.class);
    }

    // ---------- 转换/计算辅助 ----------

    protected BigDecimal sumCostLines(List<ErpInvLandedCostLine> costLines) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpInvLandedCostLine line : costLines) {
            total = total.add(nz(line.getAmount()));
        }
        return total;
    }

    protected List<LandedCostAllocationEngine.ReceiveLineInput> toInputs(List<ErpPurReceiveLine> receiveLines) {
        List<LandedCostAllocationEngine.ReceiveLineInput> inputs = new ArrayList<>(receiveLines.size());
        for (ErpPurReceiveLine rl : receiveLines) {
            inputs.add(new LandedCostAllocationEngine.ReceiveLineInput(
                    rl.getId(), rl.getMaterialId(), rl.getWarehouseId(),
                    rl.getQuantity(), rl.getAmount(), rl.getUnitPrice(),
                    null
            ));
        }
        return inputs;
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
