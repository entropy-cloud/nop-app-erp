//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinGlBalanceInputBean extends CrudInputBase {

    
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


        private Long _acctSchemaId;

    
        @PropMeta(propId=3)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private Long _periodId;

    
        @PropMeta(propId=4)
    
        public Long getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(Long value){
            this._periodId = value;
        }


        private Long _subjectId;

    
        @PropMeta(propId=5)
    
        public Long getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(Long value){
            this._subjectId = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=6)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private String _openingDebit;

    
        @PropMeta(propId=7)
    
        public String getOpeningDebit(){
            return _openingDebit;
        }

        public void setOpeningDebit(String value){
            this._openingDebit = value;
        }


        private String _openingCredit;

    
        @PropMeta(propId=8)
    
        public String getOpeningCredit(){
            return _openingCredit;
        }

        public void setOpeningCredit(String value){
            this._openingCredit = value;
        }


        private String _periodDebit;

    
        @PropMeta(propId=9)
    
        public String getPeriodDebit(){
            return _periodDebit;
        }

        public void setPeriodDebit(String value){
            this._periodDebit = value;
        }


        private String _periodCredit;

    
        @PropMeta(propId=10)
    
        public String getPeriodCredit(){
            return _periodCredit;
        }

        public void setPeriodCredit(String value){
            this._periodCredit = value;
        }


        private String _closingDebit;

    
        @PropMeta(propId=11)
    
        public String getClosingDebit(){
            return _closingDebit;
        }

        public void setClosingDebit(String value){
            this._closingDebit = value;
        }


        private String _closingCredit;

    
        @PropMeta(propId=12)
    
        public String getClosingCredit(){
            return _closingCredit;
        }

        public void setClosingCredit(String value){
            this._closingCredit = value;
        }


        private String _yearOpeningDebit;

    
        @PropMeta(propId=13)
    
        public String getYearOpeningDebit(){
            return _yearOpeningDebit;
        }

        public void setYearOpeningDebit(String value){
            this._yearOpeningDebit = value;
        }


        private String _yearOpeningCredit;

    
        @PropMeta(propId=14)
    
        public String getYearOpeningCredit(){
            return _yearOpeningCredit;
        }

        public void setYearOpeningCredit(String value){
            this._yearOpeningCredit = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=15)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private Long _departmentId;

    
        @PropMeta(propId=16)
    
        public Long getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(Long value){
            this._departmentId = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=17)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=18)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=19)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
