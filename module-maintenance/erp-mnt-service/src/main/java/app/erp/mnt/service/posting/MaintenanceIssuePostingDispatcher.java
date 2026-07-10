package app.erp.mnt.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.service.ErpMntConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 维修备件消耗 GL 过账派发器（maintenance 域侧独立 dispatcher，plan 2026-07-10-1100-6）。
 *
 * <p>语义：在 {@code ErpMntSparePartUsageBizModel.confirm} 完成后（出库移动单 DONE 后），按出库流水汇总
 * 备件成本，组装 {@link PostingEvent} 经 {@link MntPostingExecutor} 调用财务过账引擎，生成
 * MAINTENANCE_ISSUE 凭证（Dr: 维修费用 / Cr: 存货）。
 *
 * <p>承接 {@code ManufacturingIssuePostingDispatcher} 范式（域侧独立 dispatcher，不经 InvPostingDispatcher——
 * InvPostingDispatcher.resolveBusinessType 对 ERP_MNT_SPARE_PART 显式跳过，交由本域独占）。
 *
 * <p>config {@code erp-mnt.spare-part-posting-enabled}（默认 false）门控：关闭时仅库存出库，不生成凭证（向后兼容）。
 * 过账失败以 try/catch 吞异常告警，不阻断备件消耗终态（库存已出库，posted 保持库存出库语义）。
 *
 * <p>与 assets 域 {@code MAINTENANCE_EXPENSE(470)} 防双重扣减：assets 侧 linkedVisit=true 时贷中转清算（备件
 * 已由 maintenance 实物出库），本域贷存货（实物出库 GL 对应）——不同业务类型、不同触发源、不冲突。
 *
 * <p>幂等性：备件消耗单的 {@code posted} 字段语义为「库存已出库」（确认即置 true），不可作为 GL 过账标记，
 * 故本派发器以 billHeadCode（{@code usage.code + "-MI"}）+ MAINTENANCE_ISSUE 查 {@link ErpFinVoucherBillR}
 * 判重，已存在凭证则跳过。
 */
public class MaintenanceIssuePostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceIssuePostingDispatcher.class);

    @Inject
    MntPostingExecutor executor;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    public void setExecutor(MntPostingExecutor executor) {
        this.executor = executor;
    }

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 派发指定备件消耗单的 GL 过账（config 门控）：加载关联出库移动单 → 读流水成本 →
     * 装配 PostingEvent → 过账。过账失败不阻塞备件消耗终态：以 try/catch 吞异常告警。
     */
    public void dispatchIfApplicable(Long sparePartUsageId) {
        if (!isPostingEnabled()) {
            return;
        }

        ErpMntSparePartUsage usage = daoProvider.daoFor(ErpMntSparePartUsage.class).getEntityById(sparePartUsageId);
        if (usage == null) {
            return;
        }

        String billHeadCode = usage.getCode() + "-MI";
        if (voucherAlreadyExists(billHeadCode)) {
            return;
        }

        ErpMntEquipment equipment = usage.getEquipmentId() != null
                ? daoProvider.daoFor(ErpMntEquipment.class).getEntityById(usage.getEquipmentId())
                : null;

        ErpInvStockMove move = findIssueMove(usage.getCode());
        if (move == null) {
            return;
        }

        List<ErpInvStockLedger> ledgers = loadLedgers(move.getId());
        if (ledgers.isEmpty()) {
            return;
        }

        PostingEvent event = buildEvent(usage, equipment, move, ledgers);
        try {
            executor.postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("维修备件消耗过账失败，消耗单 {} 保持库存已出库（posted 语义不变）：{}",
                        usage.getCode(), e.getMessage());
            } else {
                LOG.error("维修备件消耗过账异常，消耗单 {} 保持库存已出库（posted 语义不变）", usage.getCode(), e);
            }
        }
    }

    private PostingEvent buildEvent(ErpMntSparePartUsage usage, ErpMntEquipment equipment,
                                    ErpInvStockMove move, List<ErpInvStockLedger> ledgers) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.MAINTENANCE_ISSUE);
        event.setBillHeadCode(usage.getCode() + "-MI");
        event.setOrgId(usage.getOrgId());

        Long acctSchemaId = null;
        Long currencyId = null;
        for (ErpInvStockLedger ledger : ledgers) {
            if (acctSchemaId == null) {
                acctSchemaId = ledger.getAcctSchemaId();
            }
            if (currencyId == null) {
                currencyId = ledger.getCurrencyId();
            }
        }
        if (acctSchemaId == null) {
            acctSchemaId = resolveAcctSchemaId(usage.getOrgId());
        }
        // 备件消耗单无 currencyId 字段（ErpMntSparePartUsage 无此列），出库移动单行亦可能未携带币种，
        // 故从账套的本位币（functionalCurrencyId）兜底——GL 凭证以本位币记账。
        if (currencyId == null && acctSchemaId != null) {
            currencyId = resolveFunctionalCurrencyId(acctSchemaId);
        }
        event.setAcctSchemaId(acctSchemaId);
        event.setCurrencyId(currencyId);
        event.setExchangeRate(BigDecimal.ONE);
        LocalDate voucherDate = usage.getBusinessDate() != null ? usage.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        String inventorySubject = resolveInventorySubjectCode();

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(MaintenanceIssueAcctDocProvider.KEY_EQUIPMENT_CODE,
                equipment != null ? equipment.getCode() : null);

        List<Map<String, Object>> lines = new ArrayList<>();
        for (ErpInvStockLedger ledger : ledgers) {
            BigDecimal lineCost = ledger.getTotalCost() != null
                    ? ledger.getTotalCost().abs() : BigDecimal.ZERO;
            if (lineCost.signum() == 0) {
                continue;
            }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put(MaintenanceIssueAcctDocProvider.KEY_MATERIAL_AMOUNT, lineCost);
            line.put(MaintenanceIssueAcctDocProvider.KEY_INVENTORY_SUBJECT, inventorySubject);

            ErpMdMaterial material = ledger.getMaterialId() != null
                    ? daoProvider.daoFor(ErpMdMaterial.class).getEntityById(ledger.getMaterialId())
                    : null;
            line.put(MaintenanceIssueAcctDocProvider.KEY_MATERIAL_CODE,
                    material != null ? material.getCode() : "");
            lines.add(line);
        }
        billData.put(MaintenanceIssueAcctDocProvider.KEY_LINES, lines);
        event.setBillData(billData);
        return event;
    }

    private boolean voucherAlreadyExists(String billHeadCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", ErpFinBusinessType.MAINTENANCE_ISSUE.name())));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private ErpInvStockMove findIssueMove(String usageCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpMntConstants.RELATED_BILL_TYPE_MNT_SPARE_PART));
        q.addFilter(eq("relatedBillCode", usageCode));
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

    private Long resolveAcctSchemaId(Long orgId) {
        if (orgId == null) {
            return null;
        }
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        q.addFilter(eq("status", "ACTIVE"));
        q.setLimit(1);
        List<ErpMdAcctSchema> schemas = dao.findAllByQuery(q);
        return schemas.isEmpty() ? null : schemas.get(0).getId();
    }

    private Long resolveFunctionalCurrencyId(Long acctSchemaId) {
        ErpMdAcctSchema schema = daoProvider.daoFor(ErpMdAcctSchema.class).getEntityById(acctSchemaId);
        return schema != null ? schema.getFunctionalCurrencyId() : null;
    }

    private boolean isPostingEnabled() {
        Boolean flag = AppConfig.var(ErpMntConstants.CONFIG_SPARE_PART_POSTING_ENABLED,
                ErpMntConstants.DEFAULT_SPARE_PART_POSTING_ENABLED);
        return flag != null && flag;
    }

    private String resolveInventorySubjectCode() {
        String code = AppConfig.var(ErpMntConstants.CONFIG_INVENTORY_SUBJECT_CODE,
                ErpMntConstants.DEFAULT_INVENTORY_SUBJECT_CODE);
        return code != null && !code.trim().isEmpty() ? code.trim() : ErpMntConstants.DEFAULT_INVENTORY_SUBJECT_CODE;
    }
}
