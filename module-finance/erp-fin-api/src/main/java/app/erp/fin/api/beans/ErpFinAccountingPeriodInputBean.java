//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinAccountingPeriodInputBean extends CrudInputBase {

    
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


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Integer _year;

    
        @PropMeta(propId=5)
    
        public Integer getYear(){
            return _year;
        }

        public void setYear(Integer value){
            this._year = value;
        }


        private Integer _month;

    
        @PropMeta(propId=6)
    
        public Integer getMonth(){
            return _month;
        }

        public void setMonth(Integer value){
            this._month = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private Integer _quarter;

    
        @PropMeta(propId=9)
    
        public Integer getQuarter(){
            return _quarter;
        }

        public void setQuarter(Integer value){
            this._quarter = value;
        }


        private Boolean _isAdjustment;

    
        @PropMeta(propId=10)
    
        public Boolean getIsAdjustment(){
            return _isAdjustment;
        }

        public void setIsAdjustment(Boolean value){
            this._isAdjustment = value;
        }


        private String _status;

    
        @PropMeta(propId=11)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _closedBy;

    
        @PropMeta(propId=12)
    
        public String getClosedBy(){
            return _closedBy;
        }

        public void setClosedBy(String value){
            this._closedBy = value;
        }


        private java.sql.Timestamp _closedAt;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getClosedAt(){
            return _closedAt;
        }

        public void setClosedAt(java.sql.Timestamp value){
            this._closedAt = value;
        }


        private List<ErpFinAccountingPeriodStatusInputBean> _statusRecords;

        public List<ErpFinAccountingPeriodStatusInputBean> getStatusRecords(){
            return _statusRecords;
        }

        public void setStatusRecords(List<ErpFinAccountingPeriodStatusInputBean> value){
            this._statusRecords = value;
        }


    }
