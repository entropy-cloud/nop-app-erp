package app.erp.mfg.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.inv.biz.IErpInvStockLedgerBiz;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdMaterial;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 生产领料出库过账派发器（manufacturing 域侧独立 dispatcher，plan 2026-07-10-1100-5 §Phase 3）。
 *
 * <p>语义：在 {@code ErpMfgMaterialIssueBizModel.confirm} 完成后（出库移动单 DONE 后），按领料行汇总材料成本，
 * 组装 {@link PostingEvent} 经 {@link MfgPostingExecutor} 调用财务过账引擎，成功后回写 {@code ErpMfgMaterialIssue.posted=true}。
 *
 * <p>承接 {@link ProductionVarianceDispatcher} 范式：领料单 → 加载关联出库移动单 → 读流水成本 →
 * 装配 PostingEvent → 调 Facade post() → 成功置 posted=true。过账失败以 try/catch 吞异常告警，
 * 保持 posted=false（不阻断领料出库终态）。
 *
 * <p>借方科目（WIP）需 WorkOrder 上下文，属 manufacturing 域职责，故不经 InvPostingDispatcher。
 */
public class ManufacturingIssuePostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ManufacturingIssuePostingDispatcher.class);

    @Inject
    MfgPostingExecutor executor;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpInvStockLedgerBiz stockLedgerBiz;

    public void setExecutor(MfgPostingExecutor executor) {
        this.executor = executor;
    }

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    public void setStockLedgerBiz(IErpInvStockLedgerBiz stockLedgerBiz) {
        this.stockLedgerBiz = stockLedgerBiz;
    }

    /**
     * 派发指定领料单的 GL 过账：加载关联出库移动单 → 读流水成本 → 装配 PostingEvent → 过账 → 成功回写 posted=true。
     *
     * <p>过账失败不阻塞领料终态：以 try/catch 吞异常告警，保持 posted=false。
     */
    public void dispatchIfApplicable(Long materialIssueId) {
        ErpMfgMaterialIssue issue = daoProvider.daoFor(ErpMfgMaterialIssue.class).getEntityById(materialIssueId);
        if (issue == null) {
            return;
        }
        if (Boolean.TRUE.equals(issue.getPosted())) {
            return;
        }

        ErpMfgWorkOrder wo = issue.getWorkOrderId() != null
                ? daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(issue.getWorkOrderId())
                : null;

        ErpInvStockMove move = findIssueMove(issue.getCode());
        if (move == null) {
            return;
        }

        List<ErpInvStockLedger> ledgers = loadLedgers(move.getId());
        if (ledgers.isEmpty()) {
            return;
        }

        List<ErpMfgMaterialIssueLine> issueLines = loadIssueLines(materialIssueId);
        PostingEvent event = buildEvent(issue, wo, move, ledgers, issueLines);
        try {
            Long voucherId = executor.postEvent(event);
            if (voucherId != null) {
                markIssuePosted(materialIssueId);
            }
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("生产领料过账失败，领料单 {} 保持 posted=false：{}", issue.getCode(), e.getMessage());
            } else {
                LOG.error("生产领料过账异常，领料单 {} 保持 posted=false", issue.getCode(), e);
            }
        }
    }

    private PostingEvent buildEvent(ErpMfgMaterialIssue issue, ErpMfgWorkOrder wo,
                                    ErpInvStockMove move, List<ErpInvStockLedger> ledgers,
                                    List<ErpMfgMaterialIssueLine> issueLines) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.MANUFACTURING_ISSUE);
        event.setBillHeadCode(issue.getCode() + "-MI");
        event.setOrgId(issue.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(issue.getOrgId()));
        event.setCurrencyId(issue.getCurrencyId());
        event.setExchangeRate(BigDecimal.ONE);
        LocalDate voucherDate = issue.getBusinessDate() != null ? issue.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ManufacturingIssueAcctDocProvider.KEY_WORKORDER_CODE,
                wo != null ? wo.getCode() : null);

        List<Map<String, Object>> lines = new ArrayList<>();
        for (ErpInvStockLedger ledger : ledgers) {
            BigDecimal lineCost = ledger.getTotalCost() != null
                    ? ledger.getTotalCost().abs() : BigDecimal.ZERO;
            if (lineCost.signum() == 0) {
                continue;
            }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put(ManufacturingIssueAcctDocProvider.KEY_MATERIAL_COST, lineCost);
            line.put(ManufacturingIssueAcctDocProvider.KEY_INVENTORY_SUBJECT, "1401");

            ErpMdMaterial material = ledger.getMaterialId() != null
                    ? daoProvider.daoFor(ErpMdMaterial.class).getEntityById(ledger.getMaterialId())
                    : null;
            line.put(ManufacturingIssueAcctDocProvider.KEY_MATERIAL_CODE,
                    material != null ? material.getCode() : "");
            lines.add(line);
        }
        billData.put(ManufacturingIssueAcctDocProvider.KEY_LINES, lines);
        event.setBillData(billData);
        return event;
    }

    private void markIssuePosted(Long issueId) {
        ErpMfgMaterialIssue managed = daoProvider.daoFor(ErpMfgMaterialIssue.class).getEntityById(issueId);
        if (managed != null) {
            managed.setPosted(true);
            managed.setPostedAt(CoreMetrics.currentTimestamp());
        }
    }

    private ErpInvStockMove findIssueMove(String issueCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpMfgConstants.RELATED_BILL_TYPE_MFG_ISSUE));
        q.addFilter(eq("relatedBillCode", issueCode));
        q.setLimit(1);
        List<ErpInvStockMove> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpInvStockLedger> loadLedgers(Long moveId) {
        ormTemplate.flushSession();
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return dao.findAllByQuery(q);
    }

    private List<ErpMfgMaterialIssueLine> loadIssueLines(Long issueId) {
        IEntityDao<ErpMfgMaterialIssueLine> dao = daoProvider.daoFor(ErpMfgMaterialIssueLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("issueId", issueId));
        return dao.findAllByQuery(q);
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    /**
     * 红冲指定领料单的 GL 凭证（{@code ErpMfgMaterialIssue.reverseConfirm} 触发）。
     *
     * <p>billHeadCode = {@code issue.code + "-MI"} 与正向 {@link #dispatchIfApplicable} 对称
     *（{@code ManufacturingIssuePostingDispatcher.java:123} 经独立草案审查核实无后缀）；委派
     * {@link MfgPostingExecutor#reverse} → {@link IErpFinVoucherBiz#reverse} 生成红字凭证 + 标记原凭证
     * isReversed=true（platform 内置幂等守护，无凭证时安全 no-op）。
     *
     * <p>红冲失败由调用方（{@code ErpMfgMaterialIssueBizModel.reverseConfirm}）以 try/catch 吞异常告警
     * 保持幂等（对齐 {@link #dispatchIfApplicable} 正向过账范式）。
     */
    public void reverse(ErpMfgMaterialIssue issue) {
        if (issue == null || issue.getCode() == null) {
            return;
        }
        String billHeadCode = issue.getCode() + "-MI";
        executor.reverse(billHeadCode, ErpFinBusinessType.MANUFACTURING_ISSUE);
    }
}
