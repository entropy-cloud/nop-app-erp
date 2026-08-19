//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntEquipmentStatusLogOutputBean {

    
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


        private String _fromStatus_label;

    
        public String getFromStatus_label(){
            return _fromStatus_label;
        }

        public void setFromStatus_label(String value){
            this._fromStatus_label = value;
        }


        private String _toStatus;

    
        @PropMeta(propId=4)
    
        public String getToStatus(){
            return _toStatus;
        }

        public void setToStatus(String value){
            this._toStatus = value;
        }


        private String _toStatus_label;

    
        public String getToStatus_label(){
            return _toStatus_label;
        }

        public void setToStatus_label(String value){
            this._toStatus_label = value;
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


        private String _source_label;

    
        public String getSource_label(){
            return _source_label;
        }

        public void setSource_label(String value){
            this._source_label = value;
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


        private Long _delVersion;

    
        @PropMeta(propId=9)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=10)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=11)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=13)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _equipment;

        public Map<String,Object> getEquipment(){
            return _equipment;
        }

        public void setEquipment(Map<String,Object> value){
            this._equipment = value;
        }


    }
