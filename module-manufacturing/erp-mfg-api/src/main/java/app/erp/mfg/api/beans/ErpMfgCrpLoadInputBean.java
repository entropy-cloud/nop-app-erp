//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgCrpLoadInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _workcenterId;

    
        @PropMeta(propId=2)
    
        public Long getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(Long value){
            this._workcenterId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _workOrderId;

    
        @PropMeta(propId=4)
    
        public Long getWorkOrderId(){
            return _workOrderId;
        }

        public void setWorkOrderId(Long value){
            this._workOrderId = value;
        }


        private java.time.LocalDate _loadDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getLoadDate(){
            return _loadDate;
        }

        public void setLoadDate(java.time.LocalDate value){
            this._loadDate = value;
        }


        private java.math.BigDecimal _loadHours;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getLoadHours(){
            return _loadHours;
        }

        public void setLoadHours(java.math.BigDecimal value){
            this._loadHours = value;
        }


        private java.math.BigDecimal _setupHours;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getSetupHours(){
            return _setupHours;
        }

        public void setSetupHours(java.math.BigDecimal value){
            this._setupHours = value;
        }


        private String _remark;

    
        @PropMeta(propId=8)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
