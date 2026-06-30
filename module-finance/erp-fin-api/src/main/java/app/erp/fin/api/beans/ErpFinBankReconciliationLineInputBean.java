//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBankReconciliationLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _reconciliationId;

    
        @PropMeta(propId=2)
    
        public Long getReconciliationId(){
            return _reconciliationId;
        }

        public void setReconciliationId(Long value){
            this._reconciliationId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _adjustmentType;

    
        @PropMeta(propId=4)
    
        public String getAdjustmentType(){
            return _adjustmentType;
        }

        public void setAdjustmentType(String value){
            this._adjustmentType = value;
        }


        private String _description;

    
        @PropMeta(propId=5)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private Integer _dcDirection;

    
        @PropMeta(propId=6)
    
        public Integer getDcDirection(){
            return _dcDirection;
        }

        public void setDcDirection(Integer value){
            this._dcDirection = value;
        }


        private String _amount;

    
        @PropMeta(propId=7)
    
        public String getAmount(){
            return _amount;
        }

        public void setAmount(String value){
            this._amount = value;
        }


        private String _side;

    
        @PropMeta(propId=8)
    
        public String getSide(){
            return _side;
        }

        public void setSide(String value){
            this._side = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=10)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
