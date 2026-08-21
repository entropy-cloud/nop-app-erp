//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinVoucherLineInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _voucherId;

    
        @PropMeta(propId=2)
    
        public String getVoucherId(){
            return _voucherId;
        }

        public void setVoucherId(String value){
            this._voucherId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _subjectId;

    
        @PropMeta(propId=4)
    
        public String getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(String value){
            this._subjectId = value;
        }


        private String _subjectCode;

    
        @PropMeta(propId=5)
    
        public String getSubjectCode(){
            return _subjectCode;
        }

        public void setSubjectCode(String value){
            this._subjectCode = value;
        }


        private String _subjectName;

    
        @PropMeta(propId=6)
    
        public String getSubjectName(){
            return _subjectName;
        }

        public void setSubjectName(String value){
            this._subjectName = value;
        }


        private String _dcDirection;

    
        @PropMeta(propId=7)
    
        public String getDcDirection(){
            return _dcDirection;
        }

        public void setDcDirection(String value){
            this._dcDirection = value;
        }


        private java.math.BigDecimal _debitAmount;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getDebitAmount(){
            return _debitAmount;
        }

        public void setDebitAmount(java.math.BigDecimal value){
            this._debitAmount = value;
        }


        private java.math.BigDecimal _creditAmount;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getCreditAmount(){
            return _creditAmount;
        }

        public void setCreditAmount(java.math.BigDecimal value){
            this._creditAmount = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=10)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
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


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private String _acctSchemaId;

    
        @PropMeta(propId=14)
    
        public String getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(String value){
            this._acctSchemaId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=15)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _memo;

    
        @PropMeta(propId=16)
    
        public String getMemo(){
            return _memo;
        }

        public void setMemo(String value){
            this._memo = value;
        }


        private String _partnerId;

    
        @PropMeta(propId=17)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
            this._partnerId = value;
        }


        private String _departmentId;

    
        @PropMeta(propId=18)
    
        public String getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(String value){
            this._departmentId = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=19)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=20)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private String _materialId;

    
        @PropMeta(propId=21)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private String _businessType;

    
        @PropMeta(propId=22)
    
        public String getBusinessType(){
            return _businessType;
        }

        public void setBusinessType(String value){
            this._businessType = value;
        }


        private String _costCenterId;

    
        @PropMeta(propId=23)
    
        public String getCostCenterId(){
            return _costCenterId;
        }

        public void setCostCenterId(String value){
            this._costCenterId = value;
        }


    }
