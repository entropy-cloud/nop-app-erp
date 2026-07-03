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

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _visitId;

    
        @PropMeta(propId=2)
    
        public Long getVisitId(){
            return _visitId;
        }

        public void setVisitId(Long value){
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


        private java.time.LocalDateTime _completedAt;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDateTime getCompletedAt(){
            return _completedAt;
        }

        public void setCompletedAt(java.time.LocalDateTime value){
            this._completedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=8)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=9)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
