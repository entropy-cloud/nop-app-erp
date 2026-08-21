//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtConsumptionLineOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _contractLineId;

    
        @PropMeta(propId=2)
    
        public String getContractLineId(){
            return _contractLineId;
        }

        public void setContractLineId(String value){
            this._contractLineId = value;
        }


        private java.time.LocalDate _consumptionDate;

    
        @PropMeta(propId=3)
    
        public java.time.LocalDate getConsumptionDate(){
            return _consumptionDate;
        }

        public void setConsumptionDate(java.time.LocalDate value){
            this._consumptionDate = value;
        }


        private java.math.BigDecimal _quantity;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getQuantity(){
            return _quantity;
        }

        public void setQuantity(java.math.BigDecimal value){
            this._quantity = value;
        }


        private java.math.BigDecimal _unitPrice;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getUnitPrice(){
            return _unitPrice;
        }

        public void setUnitPrice(java.math.BigDecimal value){
            this._unitPrice = value;
        }


        private java.math.BigDecimal _amount;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getAmount(){
            return _amount;
        }

        public void setAmount(java.math.BigDecimal value){
            this._amount = value;
        }


        private String _sourceBillType;

    
        @PropMeta(propId=7)
    
        public String getSourceBillType(){
            return _sourceBillType;
        }

        public void setSourceBillType(String value){
            this._sourceBillType = value;
        }


        private String _sourceBillCode;

    
        @PropMeta(propId=8)
    
        public String getSourceBillCode(){
            return _sourceBillCode;
        }

        public void setSourceBillCode(String value){
            this._sourceBillCode = value;
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


        private Map<String,Object> _contractLine;

        public Map<String,Object> getContractLine(){
            return _contractLine;
        }

        public void setContractLine(Map<String,Object> value){
            this._contractLine = value;
        }


    }
