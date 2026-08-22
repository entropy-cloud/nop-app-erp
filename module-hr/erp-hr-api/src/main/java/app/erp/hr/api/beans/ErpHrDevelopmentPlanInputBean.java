//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrDevelopmentPlanInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _employeeId;

    
        @PropMeta(propId=2)
    
        public String getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(String value){
            this._employeeId = value;
        }


        private String _planName;

    
        @PropMeta(propId=3)
    
        public String getPlanName(){
            return _planName;
        }

        public void setPlanName(String value){
            this._planName = value;
        }


        private java.time.LocalDate _targetDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getTargetDate(){
            return _targetDate;
        }

        public void setTargetDate(java.time.LocalDate value){
            this._targetDate = value;
        }


        private String _status;

    
        @PropMeta(propId=5)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _orgId;

    
        @PropMeta(propId=6)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=7)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=14)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private List<ErpHrDevelopmentPlanItemInputBean> _items;

        public List<ErpHrDevelopmentPlanItemInputBean> getItems(){
            return _items;
        }

        public void setItems(List<ErpHrDevelopmentPlanItemInputBean> value){
            this._items = value;
        }


    }
