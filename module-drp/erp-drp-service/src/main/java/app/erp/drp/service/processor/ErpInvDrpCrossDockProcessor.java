package app.erp.drp.service.processor;

import app.erp.drp.biz.IErpInvDrpCrossDockBiz;
import app.erp.drp.dao.entity.ErpInvDrpCrossDock;
import app.erp.drp.service.ErpDrpConfigs;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.biz.IErpQaInspectionTemplateBiz;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionTemplate;
import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.biz.IErpSalOrderLineBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 越库执行状态机编排 Processor（RC-R1.81 / P1-RC-081，UC-DRP-07）。
 *
 * <p>owner doc {@code docs/design/drp/cross-dock.md §越库状态机} 全部合法边：
 * PENDING→STAGING（receiveMark）/ PENDING→MATCHED（收货即匹配直连边，match 放行 PENDING）/
 * STAGING→MATCHED / MATCHED→LOADED（load 生成出站移动）/ LOADED→COMPLETED /
 * PENDING|STAGING|MATCHED→CANCELLED；COMPLETED/CANCELLED 为终态。
 *
 * <p>三匹配策略（§Inbound→Outbound 匹配）：PRE_ALLOCATED 读记录预分配目标 / ON_RECEIPT 扫描待出库
 * 销售订单（承诺发货日期 deliveryDate ASC + 创建时间 ASC）/ MANUAL 显式指定。
 *
 * <p>D2 裁决（选项 A）：质检守卫载体 = 物料存在有效检验模板（ErpQaInspectionTemplate.materialId 匹配 +
 * isActive=1，quality 只读）；config {@code erp-inv.drp-xdock-quality-gate-enabled} 默认 false 门控；
 * 快检通过凭证 = 关联本越库记录（billType DRP_XDOCK）结果 ACCEPTED/CONDITIONAL 的质检单。
 *
 * <p>跨域 Facade：inv（{@link IErpInvStockMoveBiz#generateMove} 出站移动，business-linked 自动推进 DONE）；
 * sal/qa 经 @Nullable 注入容错（单域/裁剪部署降级：ON_RECEIPT 无候选、质检守卫视为无需质检）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类或逐个覆盖 protected step。
 */
public class ErpInvDrpCrossDockProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Inject
    @Nullable
    IErpSalOrderBiz salOrderBiz;

    @Inject
    @Nullable
    IErpSalOrderLineBiz salOrderLineBiz;

    @Inject
    @Nullable
    IErpQaInspectionTemplateBiz inspectionTemplateBiz;

    @Inject
    @Nullable
    IErpQaInspectionBiz inspectionBiz;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setStockMoveBiz(IErpInvStockMoveBiz stockMoveBiz) {
        this.stockMoveBiz = stockMoveBiz;
    }

    public void setSalOrderBiz(IErpSalOrderBiz salOrderBiz) {
        this.salOrderBiz = salOrderBiz;
    }

    public void setSalOrderLineBiz(IErpSalOrderLineBiz salOrderLineBiz) {
        this.salOrderLineBiz = salOrderLineBiz;
    }

    public void setInspectionTemplateBiz(IErpQaInspectionTemplateBiz inspectionTemplateBiz) {
        this.inspectionTemplateBiz = inspectionTemplateBiz;
    }

    public void setInspectionBiz(IErpQaInspectionBiz inspectionBiz) {
        this.inspectionBiz = inspectionBiz;
    }

    // ---------- mutation 入口 ----------

    public ErpInvDrpCrossDock receiveMark(Long id, Long inboundMoveId, IServiceContext context) {
        ErpInvDrpCrossDock dock = requireDock(id);
        assertXdockEnabled();
        validateTransition(dock, ErpDrpConstants.XDOCK_STATUS_PENDING, "PENDING");
        doReceiveMark(dock, inboundMoveId, context);
        return dock;
    }

    public ErpInvDrpCrossDock match(Long id, String targetBillType, String targetBillCode, IServiceContext context) {
        ErpInvDrpCrossDock dock = requireDock(id);
        assertXdockEnabled();
        validateTransitionIn(dock, ErpDrpConstants.XDOCK_STATUS_PENDING, ErpDrpConstants.XDOCK_STATUS_STAGING);
        enforceQualityGate(dock, context);
        String[] target = resolveTarget(dock, targetBillType, targetBillCode, context);
        // 嵌套 biz 只读调用（sal/qa Facade）可能重绑环境 ORM session，写前重载挂接当前 session
        // （对齐 ErpPurReceiveApproveProcessor triggerIncomingMove 后 reload 先例）
        dock = requireDock(id);
        doMatch(dock, target[0], target[1], context);
        return dock;
    }

    public ErpInvDrpCrossDock load(Long id, IServiceContext context) {
        ErpInvDrpCrossDock dock = requireDock(id);
        assertXdockEnabled();
        validateTransition(dock, ErpDrpConstants.XDOCK_STATUS_MATCHED, "MATCHED");
        ErpInvStockMove outboundMove = generateOutboundMove(dock, context);
        // 嵌套 biz 调用（inv generateMove）可能重绑环境 ORM session，写前重载挂接当前 session
        dock = requireDock(id);
        doLoad(dock, outboundMove, context);
        return dock;
    }

    public ErpInvDrpCrossDock complete(Long id, IServiceContext context) {
        ErpInvDrpCrossDock dock = requireDock(id);
        assertXdockEnabled();
        validateTransition(dock, ErpDrpConstants.XDOCK_STATUS_LOADED, "LOADED");
        doComplete(dock, context);
        return dock;
    }

    public ErpInvDrpCrossDock cancel(Long id, IServiceContext context) {
        ErpInvDrpCrossDock dock = requireDock(id);
        assertXdockEnabled();
        if (Objects.equals(dock.getStatus(), ErpDrpConstants.XDOCK_STATUS_COMPLETED)
                || Objects.equals(dock.getStatus(), ErpDrpConstants.XDOCK_STATUS_CANCELLED)) {
            throw illegalTransition(dock, "非终态（PENDING/STAGING/MATCHED）");
        }
        doCancel(dock, context);
        return dock;
    }

    /**
     * purchase 收货审批后置 Facade（D1 裁决选项 A）：按采购单号 + 收货行物料标记 PENDING 记录 → STAGING。
     * 仅 PENDING 可迁移 → 重复调用幂等（并发组语义：双收货同记录第二次为无操作）。
     */
    public int markReceivedFromPurchase(String purchaseOrderCode, Long inboundMoveId, List<Long> materialIds,
                                        IServiceContext context) {
        if (!isXdockEnabled()) {
            return 0;
        }
        if (purchaseOrderCode == null || purchaseOrderCode.isEmpty() || materialIds == null || materialIds.isEmpty()) {
            return 0;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillType", ErpDrpConstants.XDOCK_SOURCE_BILL_TYPE_PUR_ORDER));
        q.addFilter(eq("sourceBillCode", purchaseOrderCode));
        q.addFilter(eq("status", ErpDrpConstants.XDOCK_STATUS_PENDING));
        if (materialIds.size() == 1) {
            q.addFilter(eq("materialId", materialIds.get(0)));
        } else {
            q.addFilter(in("materialId", materialIds));
        }
        List<ErpInvDrpCrossDock> docks = dockDao().findAllByQuery(q);
        int marked = 0;
        for (ErpInvDrpCrossDock dock : docks) {
            try {
                doReceiveMark(dock, inboundMoveId, context);
                marked++;
            } catch (Exception e) {
                throw new NopException(ErpDrpErrors.ERR_DRP_XDOCK_ILLEGAL_TRANSITION, e)
                        .param(ErpDrpErrors.ARG_XDOCK_CODE, dock.getCode());
            }
        }
        return marked;
    }

    // ---------- step：执行 ----------

    protected void doReceiveMark(ErpInvDrpCrossDock dock, Long inboundMoveId, IServiceContext context) {
        if (inboundMoveId != null) {
            dock.setInboundMoveId(inboundMoveId);
        }
        dock.setStatus(ErpDrpConstants.XDOCK_STATUS_STAGING);
        dockDao().updateEntity(dock);
    }

    protected void doMatch(ErpInvDrpCrossDock dock, String targetBillType, String targetBillCode,
                           IServiceContext context) {
        dock.setTargetBillType(targetBillType);
        dock.setTargetBillCode(targetBillCode);
        dock.setStatus(ErpDrpConstants.XDOCK_STATUS_MATCHED);
        dock.setMatchedAt(CoreMetrics.currentTimestamp());
        dockDao().updateEntity(dock);
    }

    protected void doLoad(ErpInvDrpCrossDock dock, ErpInvStockMove outboundMove, IServiceContext context) {
        dock.setOutboundMoveId(outboundMove != null ? outboundMove.getId() : null);
        dock.setStatus(ErpDrpConstants.XDOCK_STATUS_LOADED);
        dock.setLoadedAt(CoreMetrics.currentTimestamp());
        dockDao().updateEntity(dock);
    }

    protected void doComplete(ErpInvDrpCrossDock dock, IServiceContext context) {
        dock.setStatus(ErpDrpConstants.XDOCK_STATUS_COMPLETED);
        dockDao().updateEntity(dock);
    }

    protected void doCancel(ErpInvDrpCrossDock dock, IServiceContext context) {
        dock.setStatus(ErpDrpConstants.XDOCK_STATUS_CANCELLED);
        dockDao().updateEntity(dock);
    }

    // ---------- step：匹配目标解析（三策略） ----------

    protected String[] resolveTarget(ErpInvDrpCrossDock dock, String targetBillType, String targetBillCode,
                                     IServiceContext context) {
        String strategy = resolveStrategy(dock);
        if (ErpDrpConstants.XDOCK_STRATEGY_PRE_ALLOCATED.equals(strategy)) {
            if (dock.getTargetBillCode() == null || dock.getTargetBillCode().isEmpty()) {
                throw new NopException(ErpDrpErrors.ERR_DRP_XDOCK_NO_PRE_ALLOCATED_TARGET)
                        .param(ErpDrpErrors.ARG_XDOCK_CODE, dock.getCode());
            }
            return new String[]{dock.getTargetBillType(), dock.getTargetBillCode()};
        }
        if (ErpDrpConstants.XDOCK_STRATEGY_ON_RECEIPT.equals(strategy)) {
            ErpSalOrder candidate = scanAwaitingDeliveryOrder(dock, context);
            if (candidate == null) {
                throw new NopException(ErpDrpErrors.ERR_DRP_XDOCK_NO_MATCH)
                        .param(ErpDrpErrors.ARG_XDOCK_CODE, dock.getCode());
            }
            return new String[]{"SAL_ORDER", candidate.getCode()};
        }
        if (ErpDrpConstants.XDOCK_STRATEGY_MANUAL.equals(strategy)) {
            if (targetBillCode == null || targetBillCode.isEmpty()) {
                throw new NopException(ErpDrpErrors.ERR_DRP_XDOCK_TARGET_REQUIRED)
                        .param(ErpDrpErrors.ARG_XDOCK_CODE, dock.getCode());
            }
            return new String[]{targetBillType, targetBillCode};
        }
        throw new NopException(ErpDrpErrors.ERR_DRP_XDOCK_STRATEGY_UNSUPPORTED)
                .param(ErpDrpErrors.ARG_XDOCK_CODE, dock.getCode())
                .param(ErpDrpErrors.ARG_STRATEGY, strategy);
    }

    /**
     * 策略解析：记录 matchingStrategy（RC-R1.81 A 类列）优先，null 回落 config
     * {@code erp-inv.drp-xdock-default-strategy}（默认 ON_RECEIPT）。
     */
    protected String resolveStrategy(ErpInvDrpCrossDock dock) {
        if (dock.getMatchingStrategy() != null && !dock.getMatchingStrategy().isEmpty()) {
            return dock.getMatchingStrategy();
        }
        return AppConfig.var(ErpDrpConfigs.CONFIG_DRP_XDOCK_DEFAULT_STRATEGY,
                ErpDrpConfigs.DEFAULT_DRP_XDOCK_DEFAULT_STRATEGY);
    }

    /**
     * ON_RECEIPT 候选扫描（drp→sal 只读 Java 边，matrix §2.4 登记）：该物料存在未出库完行的
     * APPROVED 未作废销售订单，按承诺发货日期（deliveryDate，null 视为最晚）ASC + 创建时间 ASC 取首个。
     */
    protected ErpSalOrder scanAwaitingDeliveryOrder(ErpInvDrpCrossDock dock, IServiceContext context) {
        if (salOrderLineBiz == null || salOrderBiz == null) {
            return null;
        }
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("materialId", dock.getMaterialId()));
        List<ErpSalOrderLine> lines = salOrderLineBiz.findList(lq, null, context);
        Set<Long> orderIds = new LinkedHashSet<>();
        for (ErpSalOrderLine line : lines) {
            java.math.BigDecimal remaining = nz(line.getQuantity()).subtract(nz(line.getDeliveredQuantity()));
            if (remaining.signum() > 0 && line.getOrderId() != null) {
                orderIds.add(line.getOrderId());
            }
        }
        if (orderIds.isEmpty()) {
            return null;
        }
        QueryBean oq = new QueryBean();
        oq.addFilter(in("id", orderIds));
        oq.addFilter(eq("approveStatus", ErpDrpConstants.SAL_ORDER_APPROVE_STATUS_APPROVED));
        List<ErpSalOrder> orders = salOrderBiz.findList(oq, null, context);
        if (orders.isEmpty()) {
            return null;
        }
        // 作废单在 Java 侧过滤（biz 查询管道过滤操作集不支持 ne）
        List<ErpSalOrder> sorted = new ArrayList<>();
        for (ErpSalOrder o : orders) {
            if (!Objects.equals(o.getDocStatus(), ErpDrpConstants.SAL_DOC_STATUS_CANCELLED)) {
                sorted.add(o);
            }
        }
        if (sorted.isEmpty()) {
            return null;
        }
        sorted.sort(Comparator
                .comparing((ErpSalOrder o) -> o.getDeliveryDate() == null ? LocalDate.MAX : o.getDeliveryDate())
                .thenComparing(o -> o.getCreateTime() == null ? java.sql.Timestamp.valueOf(java.time.LocalDateTime.MAX)
                        : o.getCreateTime()));
        return sorted.get(0);
    }

    // ---------- step：质检守卫（D2 裁决选项 A） ----------

    /**
     * match 前置守卫：config 开启且物料存在有效检验模板（D2 载体）时，要求暂存区快检
     * （关联本越库记录、结果 ACCEPTED/CONDITIONAL）已完成，否则拒绝匹配。
     */
    protected void enforceQualityGate(ErpInvDrpCrossDock dock, IServiceContext context) {
        if (!isQualityGateEnabled()) {
            return;
        }
        if (!requiresQualityInspection(dock.getMaterialId(), context)) {
            return;
        }
        if (quickCheckPassed(dock, context)) {
            return;
        }
        throw new NopException(ErpDrpErrors.ERR_DRP_XDOCK_QUALITY_GATE_BLOCKED)
                .param(ErpDrpErrors.ARG_XDOCK_CODE, dock.getCode())
                .param(ErpDrpErrors.ARG_MATERIAL_ID, dock.getMaterialId());
    }

    protected boolean requiresQualityInspection(Long materialId, IServiceContext context) {
        if (materialId == null || inspectionTemplateBiz == null) {
            return false;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("isActive", 1));
        q.setLimit(1);
        List<ErpQaInspectionTemplate> templates = inspectionTemplateBiz.findList(q, null, context);
        return !templates.isEmpty();
    }

    protected boolean quickCheckPassed(ErpInvDrpCrossDock dock, IServiceContext context) {
        if (inspectionBiz == null) {
            return false;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpDrpConstants.RELATED_BILL_TYPE_DRP_XDOCK));
        q.addFilter(eq("relatedBillCode", dock.getCode()));
        q.addFilter(in("result", List.of(ErpDrpConstants.QA_INSPECTION_RESULT_ACCEPTED,
                ErpDrpConstants.QA_INSPECTION_RESULT_CONDITIONAL)));
        q.setLimit(1);
        List<ErpQaInspection> inspections = inspectionBiz.findList(q, null, context);
        return !inspections.isEmpty();
    }

    // ---------- step：出站移动生成 ----------

    /**
     * 生成越库出站移动（OUTGOING，source=越库暂存库位，business-linked 自动 DRAFT→CONFIRMED→DONE，
     * §反模式警示「越库不走入出库移动」）。目标端为空（发出）。
     */
    protected ErpInvStockMove generateOutboundMove(ErpInvDrpCrossDock dock, IServiceContext context) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpDrpConstants.MOVE_TYPE_OUTGOING);
        request.setOrgId(dock.getOrgId());
        request.setBusinessDate(CoreMetrics.today());
        request.setSourceWarehouseId(resolveStagingWarehouseId(dock));
        request.setSourceLocationId(dock.getStagingLocationId());
        request.setRelatedBillType(ErpDrpConstants.RELATED_BILL_TYPE_DRP_XDOCK);
        request.setRelatedBillCode(dock.getCode());
        request.setRemark("cross-dock outbound: " + dock.getCode());
        StockMoveLineRequest line = new StockMoveLineRequest();
        line.setMaterialId(dock.getMaterialId());
        line.setUoMId(resolveMaterialUomId(dock.getMaterialId()));
        line.setQuantity(dock.getQuantity());
        line.setSourceLocationId(dock.getStagingLocationId());
        request.setLines(List.of(line));
        return stockMoveBiz.generateMove(request, context);
    }

    /**
     * 暂存仓库解析：stagingLocationId → ErpMdLocation.warehouseId；无库位时回落入站移动单 destWarehouseId。
     */
    protected Long resolveStagingWarehouseId(ErpInvDrpCrossDock dock) {
        if (dock.getStagingLocationId() != null) {
            ErpMdLocation location = daoProvider.daoFor(ErpMdLocation.class)
                    .getEntityById(dock.getStagingLocationId());
            if (location != null && location.getWarehouseId() != null) {
                return location.getWarehouseId();
            }
        }
        if (dock.getInboundMoveId() != null) {
            ErpInvStockMove inbound = daoProvider.daoFor(ErpInvStockMove.class)
                    .getEntityById(dock.getInboundMoveId());
            if (inbound != null) {
                return inbound.getDestWarehouseId();
            }
        }
        throw new NopException(ErpDrpErrors.ERR_DRP_XDOCK_ILLEGAL_TRANSITION)
                .param(ErpDrpErrors.ARG_XDOCK_CODE, dock.getCode())
                .param(ErpDrpErrors.ARG_EXPECTED_STATUS, "暂存库位或入站移动单缺失，无法解析出站源仓库");
    }

    protected Long resolveMaterialUomId(Long materialId) {
        if (materialId == null) {
            return null;
        }
        ErpMdMaterial material = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(materialId);
        return material != null ? material.getUoMId() : null;
    }

    // ---------- step：守卫 ----------

    protected void assertXdockEnabled() {
        if (!isXdockEnabled()) {
            throw new NopException(ErpDrpErrors.ERR_DRP_XDOCK_DISABLED);
        }
    }

    protected boolean isXdockEnabled() {
        return AppConfig.var(ErpDrpConfigs.CONFIG_DRP_XDOCK_ENABLED, ErpDrpConfigs.DEFAULT_DRP_XDOCK_ENABLED);
    }

    protected boolean isQualityGateEnabled() {
        return AppConfig.var(ErpDrpConfigs.CONFIG_DRP_XDOCK_QUALITY_GATE_ENABLED,
                ErpDrpConfigs.DEFAULT_DRP_XDOCK_QUALITY_GATE_ENABLED);
    }

    protected void validateTransition(ErpInvDrpCrossDock dock, String expected, String expectedLabel) {
        if (!Objects.equals(dock.getStatus(), expected)) {
            throw illegalTransition(dock, expectedLabel);
        }
    }

    protected void validateTransitionIn(ErpInvDrpCrossDock dock, String... expected) {
        for (String s : expected) {
            if (Objects.equals(dock.getStatus(), s)) {
                return;
            }
        }
        throw illegalTransition(dock, String.join(" / ", expected));
    }

    protected NopException illegalTransition(ErpInvDrpCrossDock dock, String expected) {
        return new NopException(ErpDrpErrors.ERR_DRP_XDOCK_ILLEGAL_TRANSITION)
                .param(ErpDrpErrors.ARG_XDOCK_CODE, dock.getCode())
                .param(ErpDrpErrors.ARG_CURRENT_STATUS, dock.getStatus())
                .param(ErpDrpErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected ErpInvDrpCrossDock requireDock(Long id) {
        if (id == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_XDOCK_ILLEGAL_TRANSITION)
                    .param(ErpDrpErrors.ARG_XDOCK_ID, id)
                    .param(ErpDrpErrors.ARG_EXPECTED_STATUS, "id 非空");
        }
        ErpInvDrpCrossDock dock = dockDao().getEntityById(id);
        if (dock == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_XDOCK_ILLEGAL_TRANSITION)
                    .param(ErpDrpErrors.ARG_XDOCK_ID, id)
                    .param(ErpDrpErrors.ARG_EXPECTED_STATUS, "越库记录不存在");
        }
        return dock;
    }

    protected IEntityDao<ErpInvDrpCrossDock> dockDao() {
        return daoProvider.daoFor(ErpInvDrpCrossDock.class);
    }

    protected static java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v != null ? v : java.math.BigDecimal.ZERO;
    }
}
