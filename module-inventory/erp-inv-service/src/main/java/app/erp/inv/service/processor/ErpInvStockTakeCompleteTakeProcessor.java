package app.erp.inv.service.processor;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.dao.entity.ErpInvStockTake;
import app.erp.inv.dao.entity.ErpInvStockTakeLine;
import app.erp.inv.service.ErpInvConfigs;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.statemachine.ErpInvStockTakeStateMachine;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpInvStockTake completeTake per-mutation Processor（RC-R1.56 / P1-MA2-062，UC-INV-07）。
 *
 * <p>盘点完成闭环编排（protected step 方法，下游可经 Delta beans.xml 同名 bean id 逐个覆盖）：
 * 源态守卫（委托 {@link ErpInvStockTakeStateMachine}）→ 行加载 → D1 公式逐行计算差异并回填
 * {@code differenceQuantity}/{@code differenceAmount}（盘点单行零生产 writer 首次业务写入）→ 逐行经
 * {@link IErpInvStockMoveBiz#generateMove} Facade 生成盘盈 INCOMING / 盘亏 OUTGOING 差异移动单（D2 选项 A：
 * {@code relatedBillType=ERP_INV_STOCK_TAKE} + {@code relatedBillCode=null} → {@code isBusinessLinked()==false}
 * → 停 CONFIRMED 待库管员二次确认；D4-a remark 关联）→ 置 DONE。
 *
 * <p>失败语义（D4-b 选项 A）：逐行生成失败**不阻断整单**——同事务补偿删除该行已建孤立 DRAFT 移动单
 * （强制修正裁决 (a)）→ LOG.warn + config {@code erp-inv.stocktake-diff-alert-enabled}（默认 false）门控
 * 派发 {@code inv.stocktake-diff-generation-failed} 告警（对齐 A4.2.4 dispatchVarianceFailureAlert 范式，
 * 无 ACTIVE 模板静默跳过）；{@code differenceQuantity}/{@code differenceAmount} 回填不依赖生成成败
 * （强制修正裁决 (b)，盘点单 DONE 后差异数据完整，运维可经手工 generateMove 补录）。
 *
 * <p>余额影响：盘点单本身不改余额（断言④）——差异经移动单状态机（库管员二次确认 DONE 后
 * {@code bookkeeper.bookCompletion}，断言⑤）；D3 选项 A 下差异移动单过账跳过
 * （{@code InvPostingDispatcher} 跳过集，盘盈/盘亏会计化 = successor）。
 *
 * <p>权威：{@code docs/design/inventory/use-cases.md}（L1 UC-INV-07 五断言）、
 * {@code docs/design/inventory/state-machine.md} §盘点单状态机。
 */
public class ErpInvStockTakeCompleteTakeProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpInvStockTakeCompleteTakeProcessor.class);

    /** 盘点差异移动单生成失败告警事件（D4-b，对齐 A4.2.4 命名族；模板不预置，无 ACTIVE 模板时 notify 静默跳过）。 */
    static final String NOTIFY_EVENT_STOCKTAKE_DIFF_GENERATION_FAILED = "inv.stocktake-diff-generation-failed";

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Inject
    ErpInvStockTakeStateMachine stateMachine;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    public ErpInvStockTake completeTake(ErpInvStockTake take, IServiceContext context) {
        validateTransition(take, context);
        List<ErpInvStockTakeLine> lines = loadLines(take.getId());
        for (ErpInvStockTakeLine line : lines) {
            backfillDifference(line);
        }
        for (ErpInvStockTakeLine line : lines) {
            generateDiffMoveIfNeeded(take, line, context);
        }
        return finalizeComplete(take.getId());
    }

    // ---------- step：源态守卫 ----------

    protected void validateTransition(ErpInvStockTake take, IServiceContext context) {
        String status = take.getDocStatus();
        // 固定来源态守卫委托 StateMachine Bean（非法边 Bean 抛 common 层码，映射为领域码 + common 作 cause）
        try {
            stateMachine.assertCanCompleteTake(status);
        } catch (NopException e) {
            throw new NopException(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION, e)
                    .param(ErpInvErrors.ARG_TAKE_ID, take.getId())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status);
        }
    }

    // ---------- step：行加载 ----------

    protected List<ErpInvStockTakeLine> loadLines(Long takeId) {
        // 同聚合子表加载，父实体已由 BizModel requireEntity 授权，子行无独立权限规则。
        IEntityDao<ErpInvStockTakeLine> dao = daoProvider.daoFor(ErpInvStockTakeLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("takeId", takeId));
        q.addOrderField("lineNo", false);
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- step：D1 差异计算回填 ----------

    /**
     * D1 公式（use-cases.md:129 逐字）：{@code 差异 = 实盘数量 - 账面数量}——盘点行字段快照对账，
     * {@code bookQuantity} 为盘点单行账面数量快照（盘点时点），非实时余额比对（选项 B 否决）。
     * {@code differenceQuantity} 带符号（盘盈正/盘亏负）；{@code differenceAmount = 差异 × 单位成本}（unitCost 为空则不回填金额）。
     */
    protected void backfillDifference(ErpInvStockTakeLine line) {
        BigDecimal diff = nz(line.getActualQuantity()).subtract(nz(line.getBookQuantity()));
        line.setDifferenceQuantity(diff);
        if (line.getUnitCost() != null) {
            line.setDifferenceAmount(diff.multiply(line.getUnitCost()));
        } else {
            line.setDifferenceAmount(null);
        }
        lineDao().updateEntity(line);
    }

    // ---------- step：逐行差异移动单生成（失败隔离） ----------

    /**
     * 零差异行跳过；差异 >0 生成盘盈 INCOMING / 差异 <0 生成盘亏 OUTGOING 移动单（行量 = |差异|）。
     * 逐行失败隔离（D4-b）：catch → 同事务补偿删除孤立 DRAFT 移动单（强制修正裁决 (a)）→ LOG.warn + 告警 → 继续下一行。
     */
    protected void generateDiffMoveIfNeeded(ErpInvStockTake take, ErpInvStockTakeLine line, IServiceContext context) {
        BigDecimal diff = nz(line.getDifferenceQuantity());
        if (diff.signum() == 0) {
            return;
        }
        boolean gain = diff.signum() > 0;
        BigDecimal qty = diff.abs();
        StockMoveRequest request = buildDiffMoveRequest(take, line, qty, gain);
        try {
            stockMoveBiz.generateMove(request, context);
        } catch (Exception e) {
            deleteOrphanDiffMove(take, line, qty, gain);
            if (e instanceof NopException) {
                LOG.warn("盘点差异移动单生成失败（行 {} 物料 {} 已隔离，盘点单 {} 继续）：{}",
                        line.getLineNo(), line.getMaterialId(), take.getCode(), e.getMessage());
            } else {
                LOG.error("盘点差异移动单生成异常（行 {} 物料 {} 已隔离，盘点单 {} 继续）",
                        line.getLineNo(), line.getMaterialId(), take.getCode(), e);
            }
            dispatchDiffGenerationFailureAlert(take, line, e);
        }
    }

    /**
     * D2 选项 A：独立移动单——{@code relatedBillType=ERP_INV_STOCK_TAKE} + {@code relatedBillCode=null}
     * （{@code isBusinessLinked()==false} → generateMove 停 CONFIRMED 待库管员二次确认，state-machine.md:129）；
     * 类型键为 D3 过账跳过的判别载体，code 置空保持独立语义。D4-a：移动单 remark 承载「盘点差异 {code} 盘盈/盘亏」。
     * 行级映射：盘盈 dest=盘点仓库+行库位（INCOMING），盘亏 source=盘点仓库+行库位（OUTGOING），
     * 行量 = |差异|，单位成本沿用盘点行 {@code unitCost}（入库侧成本输入；出库侧由成本策略在 DONE 时解析）。
     */
    protected StockMoveRequest buildDiffMoveRequest(ErpInvStockTake take, ErpInvStockTakeLine line,
                                                     BigDecimal qty, boolean gain) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(gain ? ErpInvConstants.MOVE_TYPE_INCOMING : ErpInvConstants.MOVE_TYPE_OUTGOING);
        request.setOrgId(take.getOrgId());
        request.setBusinessDate(take.getBusinessDate() != null ? take.getBusinessDate() : CoreMetrics.today());
        if (gain) {
            request.setDestWarehouseId(take.getWarehouseId());
            request.setDestLocationId(line.getLocationId());
        } else {
            request.setSourceWarehouseId(take.getWarehouseId());
            request.setSourceLocationId(line.getLocationId());
        }
        request.setRelatedBillType(ErpInvConstants.RELATED_BILL_TYPE_STOCK_TAKE);
        request.setRemark(buildDiffMoveRemark(take.getCode(), gain));

        StockMoveLineRequest reqLine = new StockMoveLineRequest();
        reqLine.setMaterialId(line.getMaterialId());
        reqLine.setSkuId(line.getSkuId());
        reqLine.setUoMId(line.getUoMId());
        reqLine.setQuantity(qty);
        reqLine.setUnitCost(nz(line.getUnitCost()));
        reqLine.setBatchNo(line.getBatchNo());
        if (gain) {
            reqLine.setDestLocationId(line.getLocationId());
        } else {
            reqLine.setSourceLocationId(line.getLocationId());
        }
        List<StockMoveLineRequest> reqLines = new ArrayList<>(1);
        reqLines.add(reqLine);
        request.setLines(reqLines);
        return request;
    }

    protected String buildDiffMoveRemark(String takeCode, boolean gain) {
        return "盘点差异 " + takeCode + (gain ? " 盘盈" : " 盘亏");
    }

    /**
     * 同事务补偿删除孤立 DRAFT 移动单（强制修正裁决 (a)）：生成失败时移动单头+行已在会话中保存
     * （doConfirm 前 saveEntity），失败面集中于 {@code validateAvailable/validateBatchExpiry}（预留/余额变更
     * 之前抛错），DRAFT 删除零余额/流水/预留副作用。按 remark + DRAFT 态 + 行 material/量匹配定位该行移动单，
     * 清理失败降级 LOG.warn 不阻断（极端 DB 故障边缘登记 residual risk，见计划 Deferred But Adjudicated）。
     */
    protected void deleteOrphanDiffMove(ErpInvStockTake take, ErpInvStockTakeLine line, BigDecimal qty, boolean gain) {
        try {
            String remark = buildDiffMoveRemark(take.getCode(), gain);
            QueryBean q = new QueryBean();
            q.addFilter(eq("relatedBillType", ErpInvConstants.RELATED_BILL_TYPE_STOCK_TAKE));
            q.addFilter(eq("remark", remark));
            List<ErpInvStockMove> candidates = moveDao().findAllByQuery(q);
            for (ErpInvStockMove candidate : candidates) {
                if (!ErpInvConstants.DOC_STATUS_DRAFT.equals(candidate.getDocStatus())) {
                    continue;
                }
                List<ErpInvStockMoveLine> candLines = loadMoveLines(candidate.getId());
                boolean matches = candLines.stream().anyMatch(cl ->
                        Objects.equals(cl.getMaterialId(), line.getMaterialId())
                                && nz(cl.getQuantity()).compareTo(qty) == 0);
                if (!matches) {
                    continue;
                }
                for (ErpInvStockMoveLine cl : candLines) {
                    moveLineDao().deleteEntity(cl);
                }
                moveDao().deleteEntity(candidate);
                LOG.warn("盘点差异移动单生成失败，已同事务删除孤立 DRAFT 移动单 {}（盘点单 {} 行 {}）",
                        candidate.getCode(), take.getCode(), line.getLineNo());
                break;
            }
        } catch (Exception cleanupErr) {
            LOG.warn("盘点差异移动单生成失败后孤立 DRAFT 清理异常（盘点单 {} 行 {}）：{}",
                    take.getCode(), line.getLineNo(), cleanupErr.getMessage());
        }
    }

    // ---------- step：终态回写 ----------

    protected ErpInvStockTake finalizeComplete(Long takeId) {
        ErpInvStockTake take = takeDao().getEntityById(takeId);
        take.setDocStatus(stateMachine.completeTakeTargetStatus());
        takeDao().updateEntity(take);
        return take;
    }

    // ---------- step：失败告警派发（D4-b） ----------

    /**
     * 盘点差异移动单生成失败告警（D4-b 选项 A，对齐 A4.2.4 {@code dispatchVarianceFailureAlert} 范式）：
     * config {@code erp-inv.stocktake-diff-alert-enabled}（默认 false）门控 + {@code notificationBiz} null 守卫
     * + notify 失败降级 LOG.warn 不阻断主流程 + 无 ACTIVE 模板静默跳过。
     */
    protected void dispatchDiffGenerationFailureAlert(ErpInvStockTake take, ErpInvStockTakeLine line, Exception cause) {
        if (notificationBiz == null) {
            return;
        }
        if (!ErpInvConfigs.isStocktakeDiffAlertEnabled()) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("takeId", take.getId());
        ctx.put("takeCode", take.getCode());
        ctx.put("lineNo", line.getLineNo());
        ctx.put("materialId", line.getMaterialId());
        ctx.put("errorCode", cause instanceof NopException ? ((NopException) cause).getErrorCode()
                : cause.getClass().getName());
        ctx.put("errorMessage", cause.getMessage());
        IServiceContext serviceCtx = new ServiceContextImpl();
        try {
            notificationBiz.notify(NOTIFY_EVENT_STOCKTAKE_DIFF_GENERATION_FAILED, ctx, serviceCtx);
        } catch (Exception notifyErr) {
            LOG.warn("盘点差异移动单生成失败告警派发失败（降级）：takeCode={}, reason={}",
                    take.getCode(), notifyErr.getMessage());
        }
    }

    // ---------- helpers ----------

    protected IEntityDao<ErpInvStockTake> takeDao() {
        return daoProvider.daoFor(ErpInvStockTake.class);
    }

    protected IEntityDao<ErpInvStockTakeLine> lineDao() {
        return daoProvider.daoFor(ErpInvStockTakeLine.class);
    }

    protected IEntityDao<ErpInvStockMove> moveDao() {
        return daoProvider.daoFor(ErpInvStockMove.class);
    }

    protected IEntityDao<ErpInvStockMoveLine> moveLineDao() {
        return daoProvider.daoFor(ErpInvStockMoveLine.class);
    }

    protected List<ErpInvStockMoveLine> loadMoveLines(Long moveId) {
        IEntityDao<ErpInvStockMoveLine> dao = moveLineDao();
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
