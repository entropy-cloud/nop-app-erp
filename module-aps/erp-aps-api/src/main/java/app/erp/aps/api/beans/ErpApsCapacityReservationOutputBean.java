//__XGEN_FORCE_OVERRIDE__
    package app.erp.aps.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpApsCapacityReservationOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _machineId;

    
        @PropMeta(propId=2)
    
        public Long getMachineId(){
            return _machineId;
        }

        public void setMachineId(Long value){
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


        private Long _operationOrderId;

    
        @PropMeta(propId=5)
    
        public Long getOperationOrderId(){
            return _operationOrderId;
        }

        public void setOperationOrderId(Long value){
            this._operationOrderId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=6)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
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


        private Integer _version;

    
        @PropMeta(propId=8)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=9)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=11)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


    }
