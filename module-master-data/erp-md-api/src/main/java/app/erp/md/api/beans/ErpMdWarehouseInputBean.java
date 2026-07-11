//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdWarehouseInputBean extends CrudInputBase {

    
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


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private String _warehouseType;

    
        @PropMeta(propId=4)
    
        public String getWarehouseType(){
            return _warehouseType;
        }

        public void setWarehouseType(String value){
            this._warehouseType = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=5)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _address;

    
        @PropMeta(propId=6)
    
        public String getAddress(){
            return _address;
        }

        public void setAddress(String value){
            this._address = value;
        }


        private Long _managerId;

    
        @PropMeta(propId=7)
    
        public Long getManagerId(){
            return _managerId;
        }

        public void setManagerId(Long value){
            this._managerId = value;
        }


        private String _status;

    
        @PropMeta(propId=8)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _batchSelectionStrategy;

    
        @PropMeta(propId=16)
    
        public String getBatchSelectionStrategy(){
            return _batchSelectionStrategy;
        }

        public void setBatchSelectionStrategy(String value){
            this._batchSelectionStrategy = value;
        }


        private List<ErpMdLocationInputBean> _locations;

        public List<ErpMdLocationInputBean> getLocations(){
            return _locations;
        }

        public void setLocations(List<ErpMdLocationInputBean> value){
            this._locations = value;
        }


    }
