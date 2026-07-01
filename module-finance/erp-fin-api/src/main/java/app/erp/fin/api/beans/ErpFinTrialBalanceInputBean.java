//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinTrialBalanceInputBean extends CrudInputBase {

    
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


        private String _subjectCode;

    
        @PropMeta(propId=6)
    
        public String getSubjectCode(){
            return _subjectCode;
        }

        public void setSubjectCode(String value){
            this._subjectCode = value;
        }


        private String _subjectName;

    
        @PropMeta(propId=7)
    
        public String getSubjectName(){
            return _subjectName;
        }

        public void setSubjectName(String value){
            this._subjectName = value;
        }


        private java.math.BigDecimal _openingDebit;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getOpeningDebit(){
            return _openingDebit;
        }

        public void setOpeningDebit(java.math.BigDecimal value){
            this._openingDebit = value;
        }


        private java.math.BigDecimal _openingCredit;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getOpeningCredit(){
            return _openingCredit;
        }

        public void setOpeningCredit(java.math.BigDecimal value){
            this._openingCredit = value;
        }


        private java.math.BigDecimal _periodDebit;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getPeriodDebit(){
            return _periodDebit;
        }

        public void setPeriodDebit(java.math.BigDecimal value){
            this._periodDebit = value;
        }


        private java.math.BigDecimal _periodCredit;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getPeriodCredit(){
            return _periodCredit;
        }

        public void setPeriodCredit(java.math.BigDecimal value){
            this._periodCredit = value;
        }


        private java.math.BigDecimal _closingDebit;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getClosingDebit(){
            return _closingDebit;
        }

        public void setClosingDebit(java.math.BigDecimal value){
            this._closingDebit = value;
        }


        private java.math.BigDecimal _closingCredit;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getClosingCredit(){
            return _closingCredit;
        }

        public void setClosingCredit(java.math.BigDecimal value){
            this._closingCredit = value;
        }


        private java.time.LocalDateTime _generatedAt;

    
        @PropMeta(propId=14)
    
        public java.time.LocalDateTime getGeneratedAt(){
            return _generatedAt;
        }

        public void setGeneratedAt(java.time.LocalDateTime value){
            this._generatedAt = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
