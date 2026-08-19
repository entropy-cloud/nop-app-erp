package app.erp.drp.service.job;

import app.erp.drp.biz.IErpInvDrpCrossDockBiz;
import app.erp.drp.dao.entity.ErpInvDrpCrossDock;
import app.erp.drp.service.ErpDrpConfigs;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.dateTimeBetween;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 越库暂存超时回退 Job Bean（RC-R1.81 / P1-RC-081，UC-DRP-07 异常路径：
 * 「超时未匹配（默认 24h）自动转为正常入库」；cross-dock.md §业务规则 3）。
 *
 * <p>R1.38 简单 job bean 范式：nop-job-local scheduler 经 BeanMethodJobInvoker 反射调用 {@link #execute()}；
 * 双层门控 = job.yaml 调度级 {@code nop.job.erp-drp-xdock-staging-timeout.enabled} + bean 内
 * {@code erp-inv.drp-xdock-staging-timeout-cron} 空值跳过（「不调度」语义）+
 * {@code erp-inv.drp-xdock-enabled} 功能总开关。
 *
 * <p>扫描 STAGING 且 updateTime 超 {@code erp-inv.drp-xdock-staging-timeout}（默认 24）小时的记录，
 * 逐条：生成 staging→正常存储位移动单（INTERNAL，billType DRP_XDOCK_PUTAWAY，business-linked 自动推进 DONE）
 * + 记录 → CANCELLED；单条失败隔离（try/catch per record）。SCAN_LIMIT=200 分页保护。
 */
public class ErpDrpCrossDockStagingTimeoutJob {

    static final Logger LOG = LoggerFactory.getLogger(ErpDrpCrossDockStagingTimeoutJob.class);

    static final int SCAN_LIMIT = 200;

    @Inject
    IErpInvDrpCrossDockBiz crossDockBiz;

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    public void setCrossDockBiz(IErpInvDrpCrossDockBiz crossDockBiz) {
        this.crossDockBiz = crossDockBiz;
    }

    public void setStockMoveBiz(IErpInvStockMoveBiz stockMoveBiz) {
        this.stockMoveBiz = stockMoveBiz;
    }

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值 / 越库功能关闭时跳过；非空时扫描超阈值 STAGING 记录转正常入库并取消。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-drp-xdock-staging-timeout-skipped: cron config empty (erp-inv.drp-xdock-staging-timeout-cron)");
            return;
        }
        if (!AppConfig.var(ErpDrpConfigs.CONFIG_DRP_XDOCK_ENABLED, ErpDrpConfigs.DEFAULT_DRP_XDOCK_ENABLED)) {
            LOG.info("erp-drp-xdock-staging-timeout-skipped: cross dock disabled (erp-inv.drp-xdock-enabled)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int fallbacked = runStagingTimeoutFallback(ctx);
            LOG.info("erp-drp-xdock-staging-timeout-done: fallbacked={}", fallbacked);
        } catch (Exception e) {
            LOG.error("erp-drp-xdock-staging-timeout-failed", e);
        }
    }

    /**
     * 扫描超阈值 STAGING 越库记录并逐条转正常入库；返回成功回退条数。
     * 扫描与回退在同一 ORM session 内完成（findList 返回的实体保持 MANAGED）。
     */
    protected int runStagingTimeoutFallback(IServiceContext ctx) {
        long timeoutHours = resolveTimeoutHours();
        LocalDateTime cutoff = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(CoreMetrics.currentTimeMillis() - timeoutHours * 3600_000L),
                java.time.ZoneId.systemDefault());
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("status", ErpDrpConstants.XDOCK_STATUS_STAGING));
            // updateTime < now - timeoutHours 语义：以 dateTimeBetween(epoch, cutoff) 表达
            // （对齐 ErpLogDraftEscalationJob 先例，XMeta 过滤操作集不支持 lt）
            q.addFilter(dateTimeBetween("updateTime",
                    LocalDateTime.of(1970, 1, 1, 0, 0), cutoff));
            q.setLimit(SCAN_LIMIT);
            List<ErpInvDrpCrossDock> docks = crossDockBiz.findList(q, null, ctx);
            if (docks == null || docks.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (ErpInvDrpCrossDock dock : docks) {
                try {
                    if (fallbackToNormalStorage(dock, ctx)) {
                        count++;
                    }
                } catch (Exception e) {
                    LOG.warn("erp-drp-xdock-staging-timeout: 单条回退失败（隔离继续）：crossDockId={}, reason={}",
                            dock.getId(), e.getMessage());
                }
            }
            return count;
        });
    }

    /**
     * 单条超时回退：生成 staging→正常存储位移动单（INTERNAL，business-linked 自动 DONE，
     * 幂等键 (DRP_XDOCK_PUTAWAY, crossDock.code)）+ 记录 → CANCELLED（备注超时回退）。
     * 暂存仓库不可解析（无库位且无入站移动）时跳过并返回 false（留待人工处理，不盲取消）。
     * 移动生成后经独立 session 重载记录再更新（嵌套 biz 调用可能重绑环境 session，
     * 对齐 ErpPurReceiveApproveProcessor triggerIncomingMove 后 reload 先例）。
     */
    protected boolean fallbackToNormalStorage(ErpInvDrpCrossDock dock, IServiceContext ctx) {
        Long stagingWarehouseId = resolveStagingWarehouseId(dock);
        if (stagingWarehouseId == null) {
            LOG.warn("erp-drp-xdock-staging-timeout: 无法解析暂存仓库，跳过：crossDockId={}", dock.getId());
            return false;
        }
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpDrpConstants.MOVE_TYPE_INTERNAL_TRANSFER);
        request.setOrgId(dock.getOrgId());
        request.setBusinessDate(CoreMetrics.today());
        request.setSourceWarehouseId(stagingWarehouseId);
        request.setSourceLocationId(dock.getStagingLocationId());
        request.setDestWarehouseId(stagingWarehouseId);
        request.setRelatedBillType(ErpDrpConstants.RELATED_BILL_TYPE_DRP_XDOCK_PUTAWAY);
        request.setRelatedBillCode(dock.getCode());
        request.setRemark("cross-dock staging timeout fallback: " + dock.getCode());
        StockMoveLineRequest line = new StockMoveLineRequest();
        line.setMaterialId(dock.getMaterialId());
        line.setUoMId(resolveMaterialUomId(dock.getMaterialId()));
        line.setQuantity(dock.getQuantity());
        line.setSourceLocationId(dock.getStagingLocationId());
        request.setLines(List.of(line));
        stockMoveBiz.generateMove(request, ctx);

        ormTemplate.runInSession(session -> {
            ErpInvDrpCrossDock fresh = daoProvider.daoFor(ErpInvDrpCrossDock.class).getEntityById(dock.getId());
            fresh.setStatus(ErpDrpConstants.XDOCK_STATUS_CANCELLED);
            fresh.setRemark("staging timeout " + timeoutHoursLabel() + "h, fallback to normal storage");
            daoProvider.daoFor(ErpInvDrpCrossDock.class).updateEntity(fresh);
            return null;
        });
        return true;
    }

    protected Long resolveStagingWarehouseId(ErpInvDrpCrossDock dock) {
        if (dock.getStagingLocationId() != null) {
            ErpMdLocation location = daoProvider.daoFor(ErpMdLocation.class).getEntityById(dock.getStagingLocationId());
            if (location != null && location.getWarehouseId() != null) {
                return location.getWarehouseId();
            }
        }
        if (dock.getInboundMoveId() != null) {
            ErpInvStockMove inbound = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(dock.getInboundMoveId());
            if (inbound != null) {
                return inbound.getDestWarehouseId();
            }
        }
        return null;
    }

    protected Long resolveMaterialUomId(Long materialId) {
        if (materialId == null) {
            return null;
        }
        ErpMdMaterial material = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(materialId);
        return material != null ? material.getUoMId() : null;
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpDrpConfigs.CONFIG_DRP_XDOCK_STAGING_TIMEOUT_CRON,
                ErpDrpConfigs.DEFAULT_DRP_XDOCK_STAGING_TIMEOUT_CRON);
    }

    protected long resolveTimeoutHours() {
        return AppConfig.var(ErpDrpConfigs.CONFIG_DRP_XDOCK_STAGING_TIMEOUT,
                ErpDrpConfigs.DEFAULT_DRP_XDOCK_STAGING_TIMEOUT);
    }

    protected String timeoutHoursLabel() {
        return String.valueOf(resolveTimeoutHours());
    }
}
