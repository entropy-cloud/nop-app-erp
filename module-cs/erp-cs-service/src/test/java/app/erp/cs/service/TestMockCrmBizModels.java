package app.erp.cs.service;

import app.erp.crm.biz.IErpCrmTeamBiz;
import app.erp.crm.biz.IErpCrmTeamMemberBiz;
import app.erp.crm.dao.entity.ErpCrmTeam;
import app.erp.crm.dao.entity.ErpCrmTeamMember;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import java.util.List;

/**
 * cs 测试容器专用 crm 团队 BizModel mock（RC-R1.65 自动分配候选池跨域查询）。
 *
 * <p>不引入 crm-service（其全量 beans 需 sal/inventory/quality/finance 服务闭包），
 * 以 {@link CrudBizModel} 实现空接口 {@link IErpCrmTeamBiz}/{@link IErpCrmTeamMemberBiz}。
 * 候选池解析仅消费 findList 只读查询——mock 覆写为 dao 直查（绕过 BizObject action 派发：
 * 测试容器无 crm 生成的 xbiz/服务 bean，defaultPrepareQuery action 不可达；生产路径
 * app-erp-all 经 crm-service 走标准管道，不受本测试覆写影响）。
 * 经 test resources 的 app-test-mock-crm.beans.xml 自动合并（app- 前缀约定），仅测试容器生效。
 */
public final class TestMockCrmBizModels {

    private TestMockCrmBizModels() {
    }

    public abstract static class ReadOnlyCrmBizMock<T extends io.nop.orm.IOrmEntity> extends CrudBizModel<T> {
        @Override
        public List<T> findList(QueryBean query, io.nop.api.core.beans.FieldSelectionBean selection,
                                IServiceContext context) {
            return dao().findAllByQuery(query);
        }
    }

    @BizModel("ErpCrmTeam")
    public static class MockErpCrmTeamBiz extends ReadOnlyCrmBizMock<ErpCrmTeam> implements IErpCrmTeamBiz {
        public MockErpCrmTeamBiz() {
            setEntityName(ErpCrmTeam.class.getName());
        }
    }

    @BizModel("ErpCrmTeamMember")
    public static class MockErpCrmTeamMemberBiz extends ReadOnlyCrmBizMock<ErpCrmTeamMember>
            implements IErpCrmTeamMemberBiz {
        public MockErpCrmTeamMemberBiz() {
            setEntityName(ErpCrmTeamMember.class.getName());
        }
    }
}
