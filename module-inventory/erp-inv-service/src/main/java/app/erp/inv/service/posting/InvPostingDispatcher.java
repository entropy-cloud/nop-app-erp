package app.erp.inv.service.posting;

import app.erp.fin.service.posting.ErpFinBusinessType;
import app.erp.fin.service.posting.PostingEvent;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 存货过账派发器。移动单 DONE 后（流水/余额同事务确立终态之后）按移动类型派生业务类型，
 * 构造 {@link PostingEvent} 经 {@link InvPostingExecutor}（独立新事务）调用财务过账引擎；
 * 同法人内部调拨不发事件。
 *
 * <p>过账失败不阻塞移动单终态（cross-domain §与财务域协作 + state-machine §7）：以 try/catch 包裹，
 * 成功置 {@code posted=true}，失败吞异常记日志、保持 {@code posted=false}（由 Deferred 兜底扫描重试）。
 */
public class InvPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(InvPostingDispatcher.class);

    @Inject
    InvPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    /**
     * DONE 后调用。入库→PURCHASE_INPUT、出库→SALES_OUTPUT；同法人内部调拨跳过。
     * 成功置移动单 {@code posted=true}；失败吞异常保持 {@code posted=false}。
     */
    public void dispatchIfApplicable(ErpInvStockMove move, List<ErpInvStockMoveLine> lines) {
        ErpFinBusinessType businessType = resolveBusinessType(move);
        if (businessType == null) {
            return;
        }

        PostingEvent event = buildEvent(move, lines, businessType);
        try {
            Long voucherId = executor.postEvent(event);
            if (voucherId != null) {
                move.setPosted(true);
                move.setPostedAt(CoreMetrics.currentDateTime());
                daoProvider.daoFor(ErpInvStockMove.class).saveOrUpdateEntity(move);
            }
        } catch (Exception e) {
            // 过账失败不阻塞移动单终态：保持 DONE + posted=false，由兜底扫描重试。
            if (e instanceof NopException) {
                LOG.warn("存货过账失败，移动单 {} 保持 DONE、posted=false：{}", move.getCode(), e.getMessage());
            } else {
                LOG.error("存货过账异常，移动单 {} 保持 DONE、posted=false", move.getCode(), e);
            }
        }
    }

    private ErpFinBusinessType resolveBusinessType(ErpInvStockMove move) {
        Integer moveType = move.getMoveType();
        if (moveType == null) {
            return null;
        }
        if (moveType == ErpInvConstants.MOVE_TYPE_INCOMING) {
            return ErpFinBusinessType.PURCHASE_INPUT;
        }
        if (moveType == ErpInvConstants.MOVE_TYPE_OUTGOING) {
            return ErpFinBusinessType.SALES_OUTPUT;
        }
        return null;
    }

    private PostingEvent buildEvent(ErpInvStockMove move, List<ErpInvStockMoveLine> lines,
                                    ErpFinBusinessType businessType) {
        List<ErpInvStockLedger> ledgers = loadLedgers(move.getId());
        BigDecimal totalCost = BigDecimal.ZERO;
        Long acctSchemaId = null;
        Long currencyId = null;
        for (ErpInvStockLedger ledger : ledgers) {
            BigDecimal lineCost = ledger.getTotalCost() != null ? ledger.getTotalCost() : BigDecimal.ZERO;
            totalCost = totalCost.add(lineCost.abs());
            if (acctSchemaId == null) {
                acctSchemaId = ledger.getAcctSchemaId();
            }
            if (currencyId == null) {
                currencyId = ledger.getCurrencyId();
            }
        }
        if (currencyId == null && !lines.isEmpty()) {
            currencyId = lines.get(0).getCurrencyId();
        }

        PostingEvent event = new PostingEvent();
        event.setBusinessType(businessType);
        event.setBillHeadCode(move.getCode());
        event.setOrgId(move.getOrgId());
        event.setAcctSchemaId(acctSchemaId);
        event.setCurrencyId(currencyId);
        event.setExchangeRate(BigDecimal.ONE);
        LocalDate voucherDate = move.getBusinessDate() != null ? move.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put("TOTAL_COST", totalCost);
        billData.put("MOVE_CODE", move.getCode());
        if (!lines.isEmpty()) {
            billData.put("MATERIAL_ID", lines.get(0).getMaterialId());
            billData.put("WAREHOUSE_ID", businessType == ErpFinBusinessType.PURCHASE_INPUT
                    ? move.getDestWarehouseId() : move.getSourceWarehouseId());
        }
        event.setBillData(billData);
        return event;
    }

    private List<ErpInvStockLedger> loadLedgers(Long moveId) {
        ormTemplate.flushSession();
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return dao.findAllByQuery(q);
    }
}
