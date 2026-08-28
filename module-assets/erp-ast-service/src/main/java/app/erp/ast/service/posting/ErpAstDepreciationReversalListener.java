package app.erp.ast.service.posting;

import app.erp.ast.biz.IErpAstAssetBiz;
import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.service.posting.IErpFinVoucherReversedListener;
import app.erp.fin.service.posting.VoucherReversedEvent;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 资产折旧凭证红冲监听者（P1-CK-ast2-005，业财闭环方向二：财务侧红冲 → 业务回退）。
 *
 * <p>财务员直接红冲已过账折旧凭证时，finance 引擎派发 {@link VoucherReversedEvent}，本监听者据此回退
 * 折旧计划行（posted=false/voucherId=null/status=REVERSED）并回退资产累计折旧/净值（按计划行 actualAmount）。
 * 修复前 assets 域零 ReversalListener 实现——异步 sweep 重试红冲成功后 GL 已冲而资产侧 posted/累计折旧
 * 不回退，事后手动红冲报 ERR_REVERSE_SOURCE_NOT_FOUND 死锁。
 *
 * <p>幂等安全：{@code schedule.posted==false} 时 no-op（与域级 {@code executeDepreciation} 重执行前置
 * 红冲路径无双重处理）。回退时置 schedule.status=REVERSED（标识「监听者已回退」——域内重执行/域级
 * reverseDepreciation 路径据此跳过自身回退，避免双重应用；悬挂自愈路径无红冲、监听者不触发，域内回退
 * 以 EXECUTED 前置正常工作）。
 *
 * <p>镜像 {@code MfgSubcontractReversalListener} 范式：switch businessType → 反查 → posted 前置 → 回退。
 * billHeadCode = 资产编码#期间（{@code DepreciationPostingDispatcher.billHeadCode}）。
 * CATCHUP 汇总凭证（billHeadCode 含 #CATCHUP 后缀）为多期汇总，无单一计划行可回退——静默跳过。
 * 跨实体读经 I*Biz（对齐跨实体访问纪律，不新增 daoFor 站点）。
 *
 * <p>监听者失败经 {@code ErpFinReversalListenerRegistry.dispatch} 的 try/catch 隔离，不阻断其他域监听者、
 * 不回滚已过账红字凭证。
 */
public class ErpAstDepreciationReversalListener implements IErpFinVoucherReversedListener {

    static final String BILL_CODE_SEPARATOR = "#";
    static final String CATCHUP_SUFFIX = "#CATCHUP";

    @Inject
    IErpAstAssetBiz assetBiz;
    @Inject
    IErpAstDepreciationScheduleBiz scheduleBiz;

    @Override
    public void onVoucherReversed(VoucherReversedEvent event, IServiceContext context) {
        String businessType = event.getBusinessType();
        if (businessType == null) {
            return;
        }
        if (Objects.equals(businessType, ErpFinBusinessType.DEPRECIATION.name())) {
            rollbackDepreciationSchedule(event, context);
        }
    }

    protected void rollbackDepreciationSchedule(VoucherReversedEvent event, IServiceContext context) {
        String billHeadCode = event.getBillHeadCode();
        if (billHeadCode == null || billHeadCode.endsWith(CATCHUP_SUFFIX)) {
            // CATCHUP 汇总凭证无单一计划行（多期汇总），静默跳过
            return;
        }
        int idx = billHeadCode.lastIndexOf(BILL_CODE_SEPARATOR);
        if (idx <= 0 || idx == billHeadCode.length() - 1) {
            return;
        }
        String assetCode = billHeadCode.substring(0, idx);
        String period = billHeadCode.substring(idx + 1);

        ErpAstAsset asset = findAssetByCode(assetCode, context);
        if (asset == null) {
            return;
        }
        ErpAstDepreciationSchedule schedule = findSchedule(asset.getId(), period, context);
        if (schedule == null || !Boolean.TRUE.equals(schedule.getPosted())) {
            return; // 幂等：无计划行或已回退
        }
        BigDecimal oldAmount = schedule.getActualAmount() != null ? schedule.getActualAmount() : BigDecimal.ZERO;

        // 置 REVERSED（终态标记，标识「监听者已回退」——域内重执行/域级 reverse 路径据此跳过自身回退）
        schedule.setStatus(ErpAstConstants.SCHEDULE_STATUS_REVERSED);
        schedule.setPosted(false);
        schedule.setVoucherId(null);
        scheduleBiz.updateEntity(schedule, null, context);

        if (oldAmount.signum() != 0) {
            asset.setAccumulatedDepreciation(nz(asset.getAccumulatedDepreciation()).subtract(oldAmount));
            asset.setNetBookValue(nz(asset.getNetBookValue()).add(oldAmount));
            assetBiz.updateEntity(asset, null, context);
        }
    }

    protected ErpAstAsset findAssetByCode(String assetCode, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", assetCode));
        q.setLimit(1);
        List<ErpAstAsset> list = assetBiz.findList(q, null, context);
        return list.isEmpty() ? null : list.get(0);
    }

    protected ErpAstDepreciationSchedule findSchedule(String assetId, String period, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", assetId));
        q.addFilter(eq("period", period));
        q.setLimit(1);
        List<ErpAstDepreciationSchedule> list = scheduleBiz.findList(q, null, context);
        return list.isEmpty() ? null : list.get(0);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
