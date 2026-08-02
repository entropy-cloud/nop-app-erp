//__XGEN_FORCE_OVERRIDE__
    package app.erp.aps.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpApsScheduleInputBean extends CrudInputBase {

    
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


        private java.time.LocalDate _scheduleDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getScheduleDate(){
            return _scheduleDate;
        }

        public void setScheduleDate(java.time.LocalDate value){
            this._scheduleDate = value;
        }


        private String _schedulingMode;

    
        @PropMeta(propId=5)
    
        public String getSchedulingMode(){
            return _schedulingMode;
        }

        public void setSchedulingMode(String value){
            this._schedulingMode = value;
        }


        private java.sql.Timestamp _horizonStart;

    
        @PropMeta(propId=6)
    
        public java.sql.Timestamp getHorizonStart(){
            return _horizonStart;
        }

        public void setHorizonStart(java.sql.Timestamp value){
            this._horizonStart = value;
        }


        private java.sql.Timestamp _horizonEnd;

    
        @PropMeta(propId=7)
    
        public java.sql.Timestamp getHorizonEnd(){
            return _horizonEnd;
        }

        public void setHorizonEnd(java.sql.Timestamp value){
            this._horizonEnd = value;
        }


        private String _status;

    
        @PropMeta(propId=8)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=9)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=17)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
