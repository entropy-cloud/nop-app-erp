//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmSequenceAssignmentInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=2)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _sequenceId;

    
        @PropMeta(propId=3)
    
        public Long getSequenceId(){
            return _sequenceId;
        }

        public void setSequenceId(Long value){
            this._sequenceId = value;
        }


        private Integer _priority;

    
        @PropMeta(propId=4)
    
        public Integer getPriority(){
            return _priority;
        }

        public void setPriority(Integer value){
            this._priority = value;
        }


        private String _conditionType;

    
        @PropMeta(propId=5)
    
        public String getConditionType(){
            return _conditionType;
        }

        public void setConditionType(String value){
            this._conditionType = value;
        }


        private String _conditionValue;

    
        @PropMeta(propId=6)
    
        public String getConditionValue(){
            return _conditionValue;
        }

        public void setConditionValue(String value){
            this._conditionValue = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=7)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private Boolean _isDefault;

    
        @PropMeta(propId=8)
    
        public Boolean getIsDefault(){
            return _isDefault;
        }

        public void setIsDefault(Boolean value){
            this._isDefault = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
