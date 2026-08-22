//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSalarySimulationInputBean extends CrudInputBase {

    
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


        private String _sourceSalaryId;

    
        @PropMeta(propId=4)
    
        public String getSourceSalaryId(){
            return _sourceSalaryId;
        }

        public void setSourceSalaryId(String value){
            this._sourceSalaryId = value;
        }


        private Integer _simulationPeriodYear;

    
        @PropMeta(propId=5)
    
        public Integer getSimulationPeriodYear(){
            return _simulationPeriodYear;
        }

        public void setSimulationPeriodYear(Integer value){
            this._simulationPeriodYear = value;
        }


        private Integer _simulationPeriodMonth;

    
        @PropMeta(propId=6)
    
        public Integer getSimulationPeriodMonth(){
            return _simulationPeriodMonth;
        }

        public void setSimulationPeriodMonth(Integer value){
            this._simulationPeriodMonth = value;
        }


        private String _simulationName;

    
        @PropMeta(propId=7)
    
        public String getSimulationName(){
            return _simulationName;
        }

        public void setSimulationName(String value){
            this._simulationName = value;
        }


        private String _status;

    
        @PropMeta(propId=8)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _reviewerId;

    
        @PropMeta(propId=9)
    
        public String getReviewerId(){
            return _reviewerId;
        }

        public void setReviewerId(String value){
            this._reviewerId = value;
        }


        private java.sql.Timestamp _reviewedAt;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getReviewedAt(){
            return _reviewedAt;
        }

        public void setReviewedAt(java.sql.Timestamp value){
            this._reviewedAt = value;
        }


        private java.sql.Timestamp _convertedAt;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getConvertedAt(){
            return _convertedAt;
        }

        public void setConvertedAt(java.sql.Timestamp value){
            this._convertedAt = value;
        }


        private String _convertedSalaryId;

    
        @PropMeta(propId=12)
    
        public String getConvertedSalaryId(){
            return _convertedSalaryId;
        }

        public void setConvertedSalaryId(String value){
            this._convertedSalaryId = value;
        }


        private String _notes;

    
        @PropMeta(propId=13)
    
        public String getNotes(){
            return _notes;
        }

        public void setNotes(String value){
            this._notes = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=20)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
