//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdMaterialCustomsOutputBean {

    
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


        private Long _materialId;

    
        @PropMeta(propId=3)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private String _declarationNo;

    
        @PropMeta(propId=4)
    
        public String getDeclarationNo(){
            return _declarationNo;
        }

        public void setDeclarationNo(String value){
            this._declarationNo = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=5)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private java.time.LocalDate _declarationDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getDeclarationDate(){
            return _declarationDate;
        }

        public void setDeclarationDate(java.time.LocalDate value){
            this._declarationDate = value;
        }


        private java.math.BigDecimal _qtyDeclared;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getQtyDeclared(){
            return _qtyDeclared;
        }

        public void setQtyDeclared(java.math.BigDecimal value){
            this._qtyDeclared = value;
        }


        private String _uomDeclared;

    
        @PropMeta(propId=8)
    
        public String getUomDeclared(){
            return _uomDeclared;
        }

        public void setUomDeclared(String value){
            this._uomDeclared = value;
        }


        private java.math.BigDecimal _amountDeclared;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getAmountDeclared(){
            return _amountDeclared;
        }

        public void setAmountDeclared(java.math.BigDecimal value){
            this._amountDeclared = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=10)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private java.math.BigDecimal _dutyAmount;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getDutyAmount(){
            return _dutyAmount;
        }

        public void setDutyAmount(java.math.BigDecimal value){
            this._dutyAmount = value;
        }


        private java.math.BigDecimal _vatAmount;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getVatAmount(){
            return _vatAmount;
        }

        public void setVatAmount(java.math.BigDecimal value){
            this._vatAmount = value;
        }


        private String _drawbackReceiptNo;

    
        @PropMeta(propId=15)
    
        public String getDrawbackReceiptNo(){
            return _drawbackReceiptNo;
        }

        public void setDrawbackReceiptNo(String value){
            this._drawbackReceiptNo = value;
        }


        private String _sourceBillType;

    
        @PropMeta(propId=16)
    
        public String getSourceBillType(){
            return _sourceBillType;
        }

        public void setSourceBillType(String value){
            this._sourceBillType = value;
        }


        private String _sourceBillCode;

    
        @PropMeta(propId=17)
    
        public String getSourceBillCode(){
            return _sourceBillCode;
        }

        public void setSourceBillCode(String value){
            this._sourceBillCode = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=18)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=19)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=20)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=22)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=24)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


    }
