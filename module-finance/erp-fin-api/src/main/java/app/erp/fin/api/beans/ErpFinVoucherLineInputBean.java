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

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _voucherId;

    
        @PropMeta(propId=2)
    
        public Long getVoucherId(){
            return _voucherId;
        }

        public void setVoucherId(Long value){
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


        private Long _subjectId;

    
        @PropMeta(propId=4)
    
        public Long getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(Long value){
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


        private Integer _dcDirection;

    
        @PropMeta(propId=7)
    
        public Integer getDcDirection(){
            return _dcDirection;
        }

        public void setDcDirection(Integer value){
            this._dcDirection = value;
        }


        private String _debitAmount;

    
        @PropMeta(propId=8)
    
        public String getDebitAmount(){
            return _debitAmount;
        }

        public void setDebitAmount(String value){
            this._debitAmount = value;
        }


        private String _creditAmount;

    
        @PropMeta(propId=9)
    
        public String getCreditAmount(){
            return _creditAmount;
        }

        public void setCreditAmount(String value){
            this._creditAmount = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=10)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private String _exchangeRate;

    
        @PropMeta(propId=11)
    
        public String getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(String value){
            this._exchangeRate = value;
        }


        private String _amountSource;

    
        @PropMeta(propId=12)
    
        public String getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(String value){
            this._amountSource = value;
        }


        private String _amountFunctional;

    
        @PropMeta(propId=13)
    
        public String getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(String value){
            this._amountFunctional = value;
        }


        private Long _acctSchemaId;

    
        @PropMeta(propId=14)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=15)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
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


        private Long _partnerId;

    
        @PropMeta(propId=17)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private Long _departmentId;

    
        @PropMeta(propId=18)
    
        public Long getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(Long value){
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


        private Long _warehouseId;

    
        @PropMeta(propId=20)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=21)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Integer _businessType;

    
        @PropMeta(propId=22)
    
        public Integer getBusinessType(){
            return _businessType;
        }

        public void setBusinessType(Integer value){
            this._businessType = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=23)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
