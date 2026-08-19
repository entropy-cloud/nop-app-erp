package app.erp.md.service.spi;

import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.spi.IErpMdSkuReferenceChecker;

import java.util.Collections;
import java.util.List;

/**
 * SKU 业务单据引用检查聚合注册中心（RC-R1.72，plan 2026-08-19-0445-1 Phase 2 D4）。
 *
 * <p><b>注入范式</b>：镜像 b2b {@code ErpB2bEdiRegistry.setProviders} 的 List 收集范式——
 * {@code checkers} 由容器经 {@code ioc:collect-beans by-type} 收集后 setter 注入（注册于
 * erp-md-service {@code app-service.beans.xml}），替代 fin employee checker 时代的单实例
 * {@code @Nullable @Inject}（其 javadoc 显式声明「多域聚合需引入 List 收集器，归 Deferred」——本类即该 Deferred 的落地）。
 *
 * <p>聚合语义：任一实现 {@code isReferencedByBill=true} → 被引用（OR）。四域生产实现
 * （purchase/sales/inventory/manufacturing）在 app-erp-all 聚合运行时经各自 beans.xml 注册后被本
 * Registry 自动收集；md 单域测试容器仅有测试桩（{@code test-sku-reference-checker.beans.xml}）或
 * 空集合——空集合放行（零回归），桩语义不变。
 */
public class ErpMdSkuReferenceCheckerRegistry {

    private List<IErpMdSkuReferenceChecker> checkers = Collections.emptyList();

    public void setCheckers(List<IErpMdSkuReferenceChecker> checkers) {
        this.checkers = checkers == null ? Collections.emptyList() : checkers;
    }

    /** 聚合判定：任一 checker 命中即被引用。 */
    public boolean isReferencedByBill(ErpMdMaterialSku sku) {
        for (IErpMdSkuReferenceChecker checker : checkers) {
            if (checker.isReferencedByBill(sku)) {
                return true;
            }
        }
        return false;
    }
}
