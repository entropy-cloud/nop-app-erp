//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntEquipmentStatusLogInputBean extends CrudInputBase {

    
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


        private String _fromStatus;

    
        @PropMeta(propId=3)
    
        public String getFromStatus(){
            return _fromStatus;
        }

        public void setFromStatus(String value){
            this._fromStatus = value;
        }


        private String _toStatus;

    
        @PropMeta(propId=4)
    
        public String getToStatus(){
            return _toStatus;
        }

        public void setToStatus(String value){
            this._toStatus = value;
        }


        private java.sql.Timestamp _changeAt;

    
        @PropMeta(propId=5)
    
        public java.sql.Timestamp getChangeAt(){
            return _changeAt;
        }

        public void setChangeAt(java.sql.Timestamp value){
            this._changeAt = value;
        }


        private String _source;

    
        @PropMeta(propId=6)
    
        public String getSource(){
            return _source;
        }

        public void setSource(String value){
            this._source = value;
        }


        private String _sourceBillCode;

    
        @PropMeta(propId=7)
    
        public String getSourceBillCode(){
            return _sourceBillCode;
        }

        public void setSourceBillCode(String value){
            this._sourceBillCode = value;
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
