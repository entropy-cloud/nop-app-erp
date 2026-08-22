//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntVisitTaskInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _visitId;

    
        @PropMeta(propId=2)
    
        public String getVisitId(){
            return _visitId;
        }

        public void setVisitId(String value){
            this._visitId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _taskDescription;

    
        @PropMeta(propId=4)
    
        public String getTaskDescription(){
            return _taskDescription;
        }

        public void setTaskDescription(String value){
            this._taskDescription = value;
        }


        private String _status;

    
        @PropMeta(propId=5)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Long _completedBy;

    
        @PropMeta(propId=6)
    
        public Long getCompletedBy(){
            return _completedBy;
        }

        public void setCompletedBy(Long value){
            this._completedBy = value;
        }


        private java.sql.Timestamp _completedAt;

    
        @PropMeta(propId=7)
    
        public java.sql.Timestamp getCompletedAt(){
            return _completedAt;
        }

        public void setCompletedAt(java.sql.Timestamp value){
            this._completedAt = value;
        }


        private java.math.BigDecimal _standardMinutes;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getStandardMinutes(){
            return _standardMinutes;
        }

        public void setStandardMinutes(java.math.BigDecimal value){
            this._standardMinutes = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
