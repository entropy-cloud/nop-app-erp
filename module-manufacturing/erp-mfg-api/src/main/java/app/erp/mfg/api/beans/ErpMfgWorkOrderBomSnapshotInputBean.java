//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgWorkOrderBomSnapshotInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _workOrderId;

    
        @PropMeta(propId=2)
    
        public Long getWorkOrderId(){
            return _workOrderId;
        }

        public void setWorkOrderId(Long value){
            this._workOrderId = value;
        }


        private Long _bomId;

    
        @PropMeta(propId=3)
    
        public Long getBomId(){
            return _bomId;
        }

        public void setBomId(Long value){
            this._bomId = value;
        }


        private Long _productId;

    
        @PropMeta(propId=4)
    
        public Long getProductId(){
            return _productId;
        }

        public void setProductId(Long value){
            this._productId = value;
        }


        private String _versionLabel;

    
        @PropMeta(propId=5)
    
        public String getVersionLabel(){
            return _versionLabel;
        }

        public void setVersionLabel(String value){
            this._versionLabel = value;
        }


        private java.math.BigDecimal _qty;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getQty(){
            return _qty;
        }

        public void setQty(java.math.BigDecimal value){
            this._qty = value;
        }


        private List<ErpMfgWorkOrderBomLineSnapshotInputBean> _lines;

        public List<ErpMfgWorkOrderBomLineSnapshotInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpMfgWorkOrderBomLineSnapshotInputBean> value){
            this._lines = value;
        }


        private List<ErpMfgWorkOrderBomOperationSnapshotInputBean> _operations;

        public List<ErpMfgWorkOrderBomOperationSnapshotInputBean> getOperations(){
            return _operations;
        }

        public void setOperations(List<ErpMfgWorkOrderBomOperationSnapshotInputBean> value){
            this._operations = value;
        }


    }
