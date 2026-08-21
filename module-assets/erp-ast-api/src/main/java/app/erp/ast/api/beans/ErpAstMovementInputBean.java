//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstMovementInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _assetId;

    
        @PropMeta(propId=4)
    
        public String getAssetId(){
            return _assetId;
        }

        public void setAssetId(String value){
            this._assetId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private java.time.LocalDate _fromDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getFromDate(){
            return _fromDate;
        }

        public void setFromDate(java.time.LocalDate value){
            this._fromDate = value;
        }


        private java.time.LocalDate _thruDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getThruDate(){
            return _thruDate;
        }

        public void setThruDate(java.time.LocalDate value){
            this._thruDate = value;
        }


        private String _fromDepartmentId;

    
        @PropMeta(propId=8)
    
        public String getFromDepartmentId(){
            return _fromDepartmentId;
        }

        public void setFromDepartmentId(String value){
            this._fromDepartmentId = value;
        }


        private String _toDepartmentId;

    
        @PropMeta(propId=9)
    
        public String getToDepartmentId(){
            return _toDepartmentId;
        }

        public void setToDepartmentId(String value){
            this._toDepartmentId = value;
        }


        private String _fromStaffId;

    
        @PropMeta(propId=10)
    
        public String getFromStaffId(){
            return _fromStaffId;
        }

        public void setFromStaffId(String value){
            this._fromStaffId = value;
        }


        private String _toStaffId;

    
        @PropMeta(propId=11)
    
        public String getToStaffId(){
            return _toStaffId;
        }

        public void setToStaffId(String value){
            this._toStaffId = value;
        }


        private String _fromLocationId;

    
        @PropMeta(propId=12)
    
        public String getFromLocationId(){
            return _fromLocationId;
        }

        public void setFromLocationId(String value){
            this._fromLocationId = value;
        }


        private String _toLocationId;

    
        @PropMeta(propId=13)
    
        public String getToLocationId(){
            return _toLocationId;
        }

        public void setToLocationId(String value){
            this._toLocationId = value;
        }


        private String _handlerId;

    
        @PropMeta(propId=14)
    
        public String getHandlerId(){
            return _handlerId;
        }

        public void setHandlerId(String value){
            this._handlerId = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=15)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=16)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=22)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=23)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=24)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=25)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private String _docVersion;

    
        @PropMeta(propId=26)
    
        public String getDocVersion(){
            return _docVersion;
        }

        public void setDocVersion(String value){
            this._docVersion = value;
        }


        private String _remark;

    
        @PropMeta(propId=33)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
