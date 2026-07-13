
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjProjectUserBiz;
import app.erp.prj.dao.entity.ErpPrjProjectUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPrjProjectUser")
public class ErpPrjProjectUserBizModel extends CrudBizModel<ErpPrjProjectUser> implements IErpPrjProjectUserBiz{
    public ErpPrjProjectUserBizModel(){
        setEntityName(ErpPrjProjectUser.class.getName());
    }

    @BizLoader(forType = ErpPrjProjectUser.class)
    public List<String> projectName(@ContextSource List<ErpPrjProjectUser> members) {
        orm().batchLoadProps(members, Collections.singleton("project"));
        List<String> result = new ArrayList<>(members.size());
        for (ErpPrjProjectUser member : members) {
            result.add(member.getProject() != null ? member.getProject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjProjectUser.class)
    public List<String> userName(@ContextSource List<ErpPrjProjectUser> members) {
        orm().batchLoadProps(members, Collections.singleton("user"));
        List<String> result = new ArrayList<>(members.size());
        for (ErpPrjProjectUser member : members) {
            result.add(member.getUser() != null ? member.getUser().getName() : null);
        }
        return result;
    }
}
