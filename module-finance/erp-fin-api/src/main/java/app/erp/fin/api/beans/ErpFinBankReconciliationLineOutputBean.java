//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBankReconciliationLineOutputBean {

    
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


        private String _dcDirection;

    
        @PropMeta(propId=6)
    
        public String getDcDirection(){
            return _dcDirection;
        }

        public void setDcDirection(String value){
            this._dcDirection = value;
        }


        private String _dcDirection_label;

    
        public String getDcDirection_label(){
            return _dcDirection_label;
        }

        public void setDcDirection_label(String value){
            this._dcDirection_label = value;
        }


        private java.math.BigDecimal _amount;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getAmount(){
            return _amount;
        }

        public void setAmount(java.math.BigDecimal value){
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


        private Integer _version;

    
        @PropMeta(propId=11)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=12)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=14)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _reconciliation;

        public Map<String,Object> getReconciliation(){
            return _reconciliation;
        }

        public void setReconciliation(Map<String,Object> value){
            this._reconciliation = value;
        }


    }
