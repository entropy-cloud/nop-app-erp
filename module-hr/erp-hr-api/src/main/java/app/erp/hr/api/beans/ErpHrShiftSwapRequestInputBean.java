//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrShiftSwapRequestInputBean extends CrudInputBase {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _requesterId;

    
        @PropMeta(propId=4)
    
        public Long getRequesterId(){
            return _requesterId;
        }

        public void setRequesterId(Long value){
            this._requesterId = value;
        }


        private Long _targetEmployeeId;

    
        @PropMeta(propId=5)
    
        public Long getTargetEmployeeId(){
            return _targetEmployeeId;
        }

        public void setTargetEmployeeId(Long value){
            this._targetEmployeeId = value;
        }


        private Long _sourceAssignmentId;

    
        @PropMeta(propId=6)
    
        public Long getSourceAssignmentId(){
            return _sourceAssignmentId;
        }

        public void setSourceAssignmentId(Long value){
            this._sourceAssignmentId = value;
        }


        private Long _targetAssignmentId;

    
        @PropMeta(propId=7)
    
        public Long getTargetAssignmentId(){
            return _targetAssignmentId;
        }

        public void setTargetAssignmentId(Long value){
            this._targetAssignmentId = value;
        }


        private java.time.LocalDate _swapDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getSwapDate(){
            return _swapDate;
        }

        public void setSwapDate(java.time.LocalDate value){
            this._swapDate = value;
        }


        private String _reason;

    
        @PropMeta(propId=9)
    
        public String getReason(){
            return _reason;
        }

        public void setReason(String value){
            this._reason = value;
        }


        private String _status;

    
        @PropMeta(propId=10)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _approvedById;

    
        @PropMeta(propId=11)
    
        public String getApprovedById(){
            return _approvedById;
        }

        public void setApprovedById(String value){
            this._approvedById = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=18)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
