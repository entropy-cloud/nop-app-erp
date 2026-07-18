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
    static final String APPROVE_STATUS_REJECTED = "REJECTED";

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ErpInvLandedCostProcessor.class);

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

    // ---------- 红冲（plan 2026-07-18-1745-2） ----------

    /**
     * 红冲已审核到岸成本单：红冲 LANDED_COST 凭证 + 反向应用 {@code ErpInvCostAdjust(LANDED_COST_SUPPLEMENT)}
     * 成本层 + 翻 {@code posted=false}/{@code approveStatus=REJECTED}/{@code docStatus=CANCELLED}。
     *
     * <p>守卫：仅 {@code posted=true} 且 {@code approveStatus=APPROVED}（已审核已过账）的到岸成本单可红冲。
     * 未过账抛 {@link ErpInvErrors#ERR_LANDED_COST_NOT_POSTED}。
     *
     * <p>编排（protected step 方法，下游可逐个覆盖）：
     * <ol>
     *   <li>红冲 LANDED_COST 凭证（{@link LandedCostPostingDispatcher#reverse} 委派
     *       {@code IErpFinVoucherBiz.reverse} 生成红字凭证 + 原凭证 isReversed=true）</li>
     *   <li>按 Phase 1 Decision 调 {@link CostAdjustmentService#reverseCostAdjust} 反向应用成本层
     *       （FIFO 按 {@code -line.id} 哨兵删调整层，MOVING_AVERAGE 反向 adjustAmount 回滚 avgCost/totalCost）</li>
     *   <li>翻 {@code posted=false}/{@code approveStatus=REJECTED}/{@code docStatus=CANCELLED}
     *       + 同步 CostAdjust 单 {@code posted=false}</li>
     * </ol>
     *
     * <p>红冲失败语义对齐 {@link LandedCostPostingDispatcher#tryPost}：异常由调用方以 try/catch 吞掉告警，
     * 但本方法作为顶层 BizModel 入口语义更严格——异常向上抛由 GraphQL 表达，便于前端感知失败。
     */
    public ErpInvLandedCost reverseApprove(Long id, IServiceContext context) {
        ErpInvLandedCost landedCost = requireLandedCost(id, context);
        validateCanReverse(landedCost, context);

        ErpInvCostAdjust costAdjust = findCostAdjustForLandedCost(landedCost.getCode());
        List<ErpInvCostAdjustLine> adjustLines = costAdjust != null ? loadAdjustLines(costAdjust.getId()) : java.util.Collections.emptyList();

        doReverseApprove(landedCost, costAdjust, adjustLines, context);

        return reload(id);
    }

    protected void validateCanReverse(ErpInvLandedCost landedCost, IServiceContext context) {
        if (!Boolean.TRUE.equals(landedCost.getPosted())
                || !Objects.equals(landedCost.getApproveStatus(), APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpInvErrors.ERR_LANDED_COST_NOT_POSTED)
                    .param(ErpInvErrors.ARG_LANDED_COST_CODE, landedCost.getCode());
        }
    }

    /**
     * step 1-3：红冲凭证 → 反向应用成本层 → 状态翻转。
     *
     * <p>红冲凭证失败吞异常告警保持幂等（对齐 {@code LandedCostPostingDispatcher.tryPost} 正向范式：
     * platform {@code IErpFinVoucherBiz.reverse} 内置幂等守护，无凭证时安全 no-op）。
     * 成本层反向应用失败向上抛（不可恢复的余额不一致）。
     */
    protected void doReverseApprove(ErpInvLandedCost landedCost, ErpInvCostAdjust costAdjust,
                                      List<ErpInvCostAdjustLine> adjustLines, IServiceContext context) {
        // 1. 红冲 LANDED_COST 凭证（billHeadCode=landedCost.code 与正向对称）
        try {
            postingDispatcher.reverse(landedCost);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("到岸成本红冲 GL 凭证失败（吞异常保持幂等），单 {} billHeadCode={}: {}",
                        landedCost.getCode(), landedCost.getCode(), e.getMessage());
            } else {
                LOG.error("到岸成本红冲 GL 凭证异常（吞异常保持幂等），单 {} billHeadCode={}",
                        landedCost.getCode(), landedCost.getCode(), e);
            }
        }

        // 2. 反向应用 ErpInvCostAdjust(LANDED_COST_SUPPLEMENT) 成本层（按 Phase 1 Decision 复用既有 reverseCostAdjust）
        if (costAdjust != null && !adjustLines.isEmpty()) {
            costAdjustmentService.reverseCostAdjust(costAdjust, adjustLines);
            ormTemplate.flushSession();
        }

        // 3. 翻状态：posted=false / approveStatus=REJECTED / docStatus=CANCELLED + CostAdjust 同步 posted=false
        ErpInvLandedCost managed = landedCostDao().getEntityById(landedCost.getId());
        Timestamp now = CoreMetrics.currentTimestamp();
        managed.setPosted(false);
        managed.setPostedAt(now);
        managed.setApproveStatus(APPROVE_STATUS_REJECTED);
        managed.setDocStatus(ErpInvConstants.DOC_STATUS_CANCELLED);
        landedCostDao().updateEntity(managed);
        ormTemplate.flushSession();

        if (costAdjust != null) {
            IEntityDao<ErpInvCostAdjust> adjustDao = daoProvider.daoFor(ErpInvCostAdjust.class);
            ErpInvCostAdjust managedAdjust = adjustDao.getEntityById(costAdjust.getId());
            if (managedAdjust != null) {
                managedAdjust.setPosted(false);
                managedAdjust.setPostedAt(now);
                managedAdjust.setDocStatus(ErpInvConstants.DOC_STATUS_CANCELLED);
                adjustDao.updateEntity(managedAdjust);
                ormTemplate.flushSession();
            }
        }
    }

    protected ErpInvCostAdjust findCostAdjustForLandedCost(String landedCostCode) {
        IEntityDao<ErpInvCostAdjust> dao = daoProvider.daoFor(ErpInvCostAdjust.class);
        QueryBean q = new QueryBean();
        // 命名约定：createAndApplyCostAdjust 中 adjust.code = "LC-" + landedCost.code
        q.addFilter(eq("code", "LC-" + landedCostCode));
        q.setLimit(1);
        List<ErpInvCostAdjust> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected List<ErpInvCostAdjustLine> loadAdjustLines(Long adjustId) {
        IEntityDao<ErpInvCostAdjustLine> dao = daoProvider.daoFor(ErpInvCostAdjustLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("adjustId", adjustId));
        q.addOrderField("lineNo", false);
        return dao.findAllByQuery(q);
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
