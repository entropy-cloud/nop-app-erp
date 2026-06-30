//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdCostCenterInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _managerId;

    
        @PropMeta(propId=5)
    
        public Long getManagerId(){
            return _managerId;
        }

        public void setManagerId(Long value){
            this._managerId = value;
        }


        private Long _parentId;

    
        @PropMeta(propId=6)
    
        public Long getParentId(){
            return _parentId;
        }

        public void setParentId(Long value){
            this._parentId = value;
        }


        private Boolean _isBudgetable;

    
        @PropMeta(propId=7)
    
        public Boolean getIsBudgetable(){
            return _isBudgetable;
        }

        public void setIsBudgetable(Boolean value){
            this._isBudgetable = value;
        }


        private Integer _status;

    
        @PropMeta(propId=8)
    
        public Integer getStatus(){
            return _status;
        }

        public void setStatus(Integer value){
            this._status = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=9)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpMdCostCenterInputBean> _children;

        public List<ErpMdCostCenterInputBean> getChildren(){
            return _children;
        }

        public void setChildren(List<ErpMdCostCenterInputBean> value){
            this._children = value;
        }


    }
