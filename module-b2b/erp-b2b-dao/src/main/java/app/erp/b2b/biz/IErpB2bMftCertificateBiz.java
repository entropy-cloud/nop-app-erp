
package app.erp.b2b.biz;

import app.erp.b2b.dao.entity.ErpB2bMftCertificate;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

/**
 * MFT 证书 Biz 接口。除标准 CRUD 外，提供证书过期检查查询入口。
 *
 * <p>注意：{@code ErpB2bMftCertificate} ORM 实体仅存储证书元数据（指纹/算法/过期日期等），
 * 不含 {@code certificatePem}/{@code privateKeyRef} 列（design {@code managed-file-transfer.md §证书管理} 列出但 ORM 未落地）。
 * 真实私钥加密存储（EncryptionHelper）归 follow-up（触发条件：真实证书集成）。
 */
public interface IErpB2bMftCertificateBiz extends ICrudBiz<ErpB2bMftCertificate> {

    /**
     * 查询即将过期的证书（默认 30 天内）。
     *
     * @param withinDays 多少天内过期
     * @return 即将过期的活跃证书列表
     */
    @BizQuery
    List<ErpB2bMftCertificate> findExpiringCertificates(@Name("withinDays") Integer withinDays,
                                                         IServiceContext context);
}
