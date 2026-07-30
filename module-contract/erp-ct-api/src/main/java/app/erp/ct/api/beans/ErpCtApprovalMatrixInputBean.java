//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtApprovalMatrixInputBean extends CrudInputBase {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private java.math.BigDecimal _minAmount;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getMinAmount(){
            return _minAmount;
        }

        public void setMinAmount(java.math.BigDecimal value){
            this._minAmount = value;
        }


        private java.math.BigDecimal _maxAmount;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getMaxAmount(){
            return _maxAmount;
        }

        public void setMaxAmount(java.math.BigDecimal value){
            this._maxAmount = value;
        }


        private String _approverRole;

    
        @PropMeta(propId=6)
    
        public String getApproverRole(){
            return _approverRole;
        }

        public void setApproverRole(String value){
            this._approverRole = value;
        }


        private Integer _approvalOrder;

    
        @PropMeta(propId=7)
    
        public Integer getApprovalOrder(){
            return _approvalOrder;
        }

        public void setApprovalOrder(Integer value){
            this._approvalOrder = value;
        }


        private String _contractType;

    
        @PropMeta(propId=8)
    
        public String getContractType(){
            return _contractType;
        }

        public void setContractType(String value){
            this._contractType = value;
        }


        private Boolean _allowSkip;

    
        @PropMeta(propId=9)
    
        public Boolean getAllowSkip(){
            return _allowSkip;
        }

        public void setAllowSkip(Boolean value){
            this._allowSkip = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=10)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
