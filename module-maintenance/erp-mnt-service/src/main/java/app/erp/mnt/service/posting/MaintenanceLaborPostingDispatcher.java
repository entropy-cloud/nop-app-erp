package app.erp.mnt.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntConstants;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 维修工时费用化 GL 过账派发器（maintenance 域，plan 2026-07-18-0949-1）。
 *
 * <p>语义：在 {@code ErpMntVisitBizModel.complete} 完成 totalMinutes 计算后，按
 * {@code totalMinutes × hourlyRate / 60} 派生工时成本，组装 {@link PostingEvent} 经
 * {@link MntPostingExecutor} 调用财务过账引擎，生成 MAINTENANCE_LABOR 凭证
 *（Dr: 折旧费用 6602 / Cr: 应付职工薪酬 2211）。
 *
 * <p>承接 {@link MaintenanceIssuePostingDispatcher} 范式（域侧独立 dispatcher，bean id = 全限定类名）。
 * Phase 1 Decision (e) 裁决 boolean 返回值以便 BizModel 显式 {@code LOG.warn} 消费失败路径（语义等价
 * 1100-6 void 吞异常，运维可见性更优）。
 *
 * <p>config {@code erp-mnt.labor-posting-enabled}（默认 false）门控：关闭时不生成凭证（向后兼容，
 * 既有 visit complete 行为零回归）。{@code erp-mnt.default-labor-hourly-rate}（默认 0）派生工时成本；
 * rate ≤ 0 时跳过（不抛错，归 config 未配置）。
 *
 * <p>幂等性：以 billHeadCode（{@code visit.code + "-ML"}）+ MAINTENANCE_LABOR 查
 * {@link ErpFinVoucherBillR} 判重，已存在凭证则返回 false（与 {@link IErpFinVoucherBiz#post} 内置幂等
 * 双重保护）。
 *
 * <p>设备级 / 模板级 / 员工级费率物化归各自 successor（Non-Goals）：当前 maintenance schema 无
 * {@code ErpMntEquipment.laborHourlyRate} 列也无 {@code ErpMntTemplate} 实体也无
 * {@code standardLaborMinutes} 列；本计划仅落地 config 全局费率。
 */
public class MaintenanceLaborPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceLaborPostingDispatcher.class);

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
     * 派发指定维护访问的工时费用化 GL 过账（config 门控 + rate 守卫 + 幂等判重）。
     *
     * @return true 表示已生成或已存在 MAINTENANCE_LABOR 凭证；false 表示因 config 关闭 / totalMinutes≤0 /
     *         rate≤0 / 幂等命中 / 过账失败（吞异常范式）而跳过或失败。
     */
    public boolean postLabor(ErpMntVisit visit, IServiceContext context) {
        if (!isPostingEnabled()) {
            return false;
        }
        if (visit == null) {
            return false;
        }
        BigDecimal totalMinutes = visit.getTotalMinutes();
        if (totalMinutes == null || totalMinutes.signum() <= 0) {
            return false;
        }
        BigDecimal rate = getDefaultLaborHourlyRate();
        if (rate == null || rate.signum() <= 0) {
            return false;
        }
        String billHeadCode = visit.getCode() + "-ML";
        if (voucherAlreadyExists(billHeadCode)) {
            return false;
        }
        BigDecimal laborCost = totalMinutes.multiply(rate)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        PostingEvent event = buildEvent(visit, rate, laborCost, billHeadCode);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("维修工时费用化过账失败，访问 {} 保持 complete 终态（posted 语义不变）：{}",
                        visit.getCode(), e.getMessage());
            } else {
                LOG.error("维修工时费用化过账异常，访问 {} 保持 complete 终态（posted 语义不变）",
                        visit.getCode(), e);
            }
            return false;
        }
    }

    /**
     * config 门控读取（对齐 1100-6 {@code MaintenanceIssuePostingDispatcher:221-225} 内联范式）。
     * 暴露为 public 以便 BizModel 在 doComplete 内消费同一读取结果（避免重复读 config）。
     */
    public boolean isPostingEnabled() {
        Boolean flag = AppConfig.var(ErpMntConstants.CONFIG_LABOR_POSTING_ENABLED,
                ErpMntConstants.DEFAULT_LABOR_POSTING_ENABLED);
        return flag != null && flag;
    }

    /**
     * 红冲指定维护访问的工时费用化凭证（{@code visit.cancel} 触发，config-gated 同正向）。
     *
     * <p>billHeadCode = {@code visit.code + "-ML"} 与正向 {@link #postLabor} 对称（{@code MaintenanceLaborPostingDispatcher.java:102}
     * 经独立草案审查核实无 millis/uuid 后缀）；委派 {@link MntPostingExecutor#reverse} →
     * {@link IErpFinVoucherBiz#reverse} 生成红字凭证 + 标记原凭证 isReversed=true（platform 内置幂等守护，
     * 无凭证时安全 no-op）。
     *
     * <p>语义对齐 {@code MfgPostingExecutor.reverse}：异常由调用方（{@code ErpMntVisitBizModel.doCancel}）
     * 以 try/catch 吞异常告警保持 cancel 终态不阻断（对齐 {@link #postLabor} 失败语义）。
     */
    public void reverseLabor(ErpMntVisit visit) {
        if (visit == null || visit.getCode() == null) {
            return;
        }
        String billHeadCode = visit.getCode() + "-ML";
        executor.reverse(billHeadCode, ErpFinBusinessType.MAINTENANCE_LABOR);
    }

    private BigDecimal getDefaultLaborHourlyRate() {
        String raw = AppConfig.var(ErpMntConstants.CONFIG_DEFAULT_LABOR_HOURLY_RATE,
                ErpMntConstants.DEFAULT_LABOR_HOURLY_RATE_VALUE);
        if (raw == null || raw.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private PostingEvent buildEvent(ErpMntVisit visit, BigDecimal rate, BigDecimal laborCost, String billHeadCode) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.MAINTENANCE_LABOR);
        event.setBillHeadCode(billHeadCode);
        event.setOrgId(visit.getOrgId());

        Long acctSchemaId = resolveAcctSchemaId(visit.getOrgId());
        Long currencyId = acctSchemaId != null ? resolveFunctionalCurrencyId(acctSchemaId) : null;
        event.setAcctSchemaId(acctSchemaId);
        event.setCurrencyId(currencyId);
        event.setExchangeRate(BigDecimal.ONE);

        LocalDate voucherDate = visit.getBusinessDate() != null
                ? visit.getBusinessDate()
                : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        String equipmentCode = null;
        Long equipmentId = visit.getEquipmentId();
        if (equipmentId != null) {
            ErpMntEquipment equipment = daoProvider.daoFor(ErpMntEquipment.class).getEntityById(equipmentId);
            if (equipment != null) {
                equipmentCode = equipment.getCode();
            }
        }

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(MaintenanceLaborAcctDocProvider.KEY_TOTAL, laborCost);
        billData.put(MaintenanceLaborAcctDocProvider.KEY_EQUIPMENT_CODE, equipmentCode);
        billData.put(MaintenanceLaborAcctDocProvider.KEY_TOTAL_MINUTES, visit.getTotalMinutes());
        billData.put(MaintenanceLaborAcctDocProvider.KEY_HOURLY_RATE, rate);
        billData.put(MaintenanceLaborAcctDocProvider.KEY_VISIT_CODE, visit.getCode());
        event.setBillData(billData);
        return event;
    }

    private boolean voucherAlreadyExists(String billHeadCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", ErpFinBusinessType.MAINTENANCE_LABOR.name())));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    private Long resolveFunctionalCurrencyId(Long acctSchemaId) {
        ErpMdAcctSchema schema = daoProvider.daoFor(ErpMdAcctSchema.class).getEntityById(acctSchemaId);
        return schema != null ? schema.getFunctionalCurrencyId() : null;
    }
}
