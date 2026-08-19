//__XGEN_FORCE_OVERRIDE__
    package app.erp.log.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpLogDeliveryBookingInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _shipmentId;

    
        @PropMeta(propId=2)
    
        public Long getShipmentId(){
            return _shipmentId;
        }

        public void setShipmentId(Long value){
            this._shipmentId = value;
        }


        private Long _windowId;

    
        @PropMeta(propId=3)
    
        public Long getWindowId(){
            return _windowId;
        }

        public void setWindowId(Long value){
            this._windowId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private java.time.LocalDate _bookedDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getBookedDate(){
            return _bookedDate;
        }

        public void setBookedDate(java.time.LocalDate value){
            this._bookedDate = value;
        }


        private String _bookedTime;

    
        @PropMeta(propId=6)
    
        public String getBookedTime(){
            return _bookedTime;
        }

        public void setBookedTime(String value){
            this._bookedTime = value;
        }


        private String _status;

    
        @PropMeta(propId=7)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private java.math.BigDecimal _missedFee;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getMissedFee(){
            return _missedFee;
        }

        public void setMissedFee(java.math.BigDecimal value){
            this._missedFee = value;
        }


        private Integer _priorityScore;

    
        @PropMeta(propId=9)
    
        public Integer getPriorityScore(){
            return _priorityScore;
        }

        public void setPriorityScore(Integer value){
            this._priorityScore = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
