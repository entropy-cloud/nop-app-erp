//__XGEN_FORCE_OVERRIDE__
    package app.erp.aps.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpApsCapacityReservationInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _machineId;

    
        @PropMeta(propId=2)
    
        public String getMachineId(){
            return _machineId;
        }

        public void setMachineId(String value){
            this._machineId = value;
        }


        private java.sql.Timestamp _plannedStartT;

    
        @PropMeta(propId=3)
    
        public java.sql.Timestamp getPlannedStartT(){
            return _plannedStartT;
        }

        public void setPlannedStartT(java.sql.Timestamp value){
            this._plannedStartT = value;
        }


        private java.sql.Timestamp _plannedEndT;

    
        @PropMeta(propId=4)
    
        public java.sql.Timestamp getPlannedEndT(){
            return _plannedEndT;
        }

        public void setPlannedEndT(java.sql.Timestamp value){
            this._plannedEndT = value;
        }


        private String _operationOrderId;

    
        @PropMeta(propId=5)
    
        public String getOperationOrderId(){
            return _operationOrderId;
        }

        public void setOperationOrderId(String value){
            this._operationOrderId = value;
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


    }
