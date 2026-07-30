//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSalarySimulationItemAdjustmentInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _simulationId;

    
        @PropMeta(propId=2)
    
        public Long getSimulationId(){
            return _simulationId;
        }

        public void setSimulationId(Long value){
            this._simulationId = value;
        }


        private Long _employeeId;

    
        @PropMeta(propId=3)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private String _salaryItemCode;

    
        @PropMeta(propId=4)
    
        public String getSalaryItemCode(){
            return _salaryItemCode;
        }

        public void setSalaryItemCode(String value){
            this._salaryItemCode = value;
        }


        private java.math.BigDecimal _originalAmount;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getOriginalAmount(){
            return _originalAmount;
        }

        public void setOriginalAmount(java.math.BigDecimal value){
            this._originalAmount = value;
        }


        private java.math.BigDecimal _adjustedAmount;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getAdjustedAmount(){
            return _adjustedAmount;
        }

        public void setAdjustedAmount(java.math.BigDecimal value){
            this._adjustedAmount = value;
        }


        private String _adjustmentReason;

    
        @PropMeta(propId=7)
    
        public String getAdjustmentReason(){
            return _adjustmentReason;
        }

        public void setAdjustmentReason(String value){
            this._adjustmentReason = value;
        }


        private String _adjustedBy;

    
        @PropMeta(propId=8)
    
        public String getAdjustedBy(){
            return _adjustedBy;
        }

        public void setAdjustedBy(String value){
            this._adjustedBy = value;
        }


        private java.sql.Timestamp _adjustedAt;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getAdjustedAt(){
            return _adjustedAt;
        }

        public void setAdjustedAt(java.sql.Timestamp value){
            this._adjustedAt = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=10)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
