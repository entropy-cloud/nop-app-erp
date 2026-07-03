//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntDowntimeEntryInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _equipmentId;

    
        @PropMeta(propId=2)
    
        public Long getEquipmentId(){
            return _equipmentId;
        }

        public void setEquipmentId(Long value){
            this._equipmentId = value;
        }


        private java.time.LocalDateTime _startTime;

    
        @PropMeta(propId=3)
    
        public java.time.LocalDateTime getStartTime(){
            return _startTime;
        }

        public void setStartTime(java.time.LocalDateTime value){
            this._startTime = value;
        }


        private java.time.LocalDateTime _endTime;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDateTime getEndTime(){
            return _endTime;
        }

        public void setEndTime(java.time.LocalDateTime value){
            this._endTime = value;
        }


        private java.math.BigDecimal _totalMinutes;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getTotalMinutes(){
            return _totalMinutes;
        }

        public void setTotalMinutes(java.math.BigDecimal value){
            this._totalMinutes = value;
        }


        private String _reason;

    
        @PropMeta(propId=6)
    
        public String getReason(){
            return _reason;
        }

        public void setReason(String value){
            this._reason = value;
        }


        private Long _relatedJobOrderId;

    
        @PropMeta(propId=7)
    
        public Long getRelatedJobOrderId(){
            return _relatedJobOrderId;
        }

        public void setRelatedJobOrderId(Long value){
            this._relatedJobOrderId = value;
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
