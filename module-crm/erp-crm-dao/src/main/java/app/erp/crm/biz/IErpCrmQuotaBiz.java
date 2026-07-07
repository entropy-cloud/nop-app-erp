
package app.erp.crm.biz;

import app.erp.crm.dao.entity.ErpCrmQuota;
import app.erp.crm.dao.entity.ErpCrmTerritoryPipeline;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

/**
 * 销售配额业务接口。除标准 CRUD 外，定义配额层级聚合查询 + 定稿锁定/解冻 + 年度均分 + 区域管道对比入口。
 *
 * <p>对齐 {@code docs/design/crm/territory.md §配额层级汇总 / §业务规则 4-5}。
 */
public interface IErpCrmQuotaBiz extends ICrudBiz<ErpCrmQuota> {

    /**
     * 配额层级聚合：territoryId=null 公司级、territoryId≠null 区域级（聚合子树）。
     * 显式值优先（该层级已直接配置 quotaAmount 则直接返回，否则向下聚合子节点求和）。
     */
    @BizQuery
    ErpCrmQuota getQuotaRollup(@Optional @Name("territoryId") Long territoryId,
                                @Name("periodType") String periodType,
                                @Name("fiscalYear") int fiscalYear,
                                @Optional @Name("periodLabel") String periodLabel,
                                IServiceContext context);

    /**
     * 定稿锁定：isFinalized=true，已定稿抛 {@code ERR_QUOTA_FINALIZED}。
     */
    @BizMutation
    ErpCrmQuota finalizeQuota(@Name("quotaId") Long quotaId, IServiceContext context);

    /**
     * 解冻：isFinalized=false。已定稿配额唯一允许的修改入口。
     */
    @BizMutation
    ErpCrmQuota unfinalizeQuota(@Name("quotaId") Long quotaId, IServiceContext context);

    /**
     * 年度配额按季（4 行）或月（12 行）均分生成子期间配额行。
     * 配置 {@code erp-crm.quota.distribute-monthly}（默认 false）控制月度/季度。
     * 仅 periodType=ANNUAL 配额可均分；目标配额须未定稿。
     */
    @BizMutation
    List<ErpCrmQuota> distributeAnnualQuota(@Name("quotaId") Long quotaId,
                                             @Optional @Name("periodType") String periodType,
                                             IServiceContext context);

    /**
     * 区域管道对比入口：同屏返回该区域的目标（QuotaSummary）/ 预测（ForecastSummary）/ 实际收入聚合。
     *
     * <p>对齐 {@code docs/design/crm/territory.md §业务规则 3}（实际/预测/目标同屏对比）。
     */
    @BizQuery
    ErpCrmTerritoryPipeline getTerritoryPipeline(@Optional @Name("territoryId") Long territoryId,
                                                  @Name("periodLabel") String periodLabel,
                                                  IServiceContext context);
}
