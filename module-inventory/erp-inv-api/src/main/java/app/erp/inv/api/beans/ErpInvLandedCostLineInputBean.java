//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvLandedCostLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _landedCostId;

    
        @PropMeta(propId=2)
    
        public Long getLandedCostId(){
            return _landedCostId;
        }

        public void setLandedCostId(Long value){
            this._landedCostId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _costElement;

    
        @PropMeta(propId=4)
    
        public String getCostElement(){
            return _costElement;
        }

        public void setCostElement(String value){
            this._costElement = value;
        }


        private java.math.BigDecimal _amount;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getAmount(){
            return _amount;
        }

        public void setAmount(java.math.BigDecimal value){
            this._amount = value;
        }


        private Long _apPartnerId;

    
        @PropMeta(propId=6)
    
        public Long getApPartnerId(){
            return _apPartnerId;
        }

        public void setApPartnerId(Long value){
            this._apPartnerId = value;
        }


        private String _remark;

    
        @PropMeta(propId=7)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=8)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
