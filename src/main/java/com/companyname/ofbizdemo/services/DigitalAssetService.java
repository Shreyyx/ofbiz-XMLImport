package com.companyname.ofbizdemo.services;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import java.util.Map;

public class DigitalAssetService {
    public static final String MODULE = DigitalAssetService.class.getName();

    public static Map<String, Object> updateDigitalAsset(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String dataResourceId = (String) context.get("dataResourceId");
        String fileName = (String) context.get("fileName");
        String mimeTypeId = (String) context.get("mimeTypeId");
        String objectInfo = (String) context.get("objectInfo");
        String localeString = (String) context.get("localeString");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            GenericValue dataResource = EntityQuery.use(delegator)
                    .from("DataResource")
                    .where("dataResourceId", dataResourceId, "dataResourceName", fileName)
                    .queryOne();

            if (dataResource == null) {
                return ServiceUtil.returnError("DataResource not found with ID: " + dataResourceId + " and name: " + fileName);
            }

            boolean isUpdated = false;

            if (UtilValidate.isNotEmpty(mimeTypeId) && !mimeTypeId.equals(dataResource.getString("mimeTypeId"))) {
                dataResource.set("mimeTypeId", mimeTypeId);
                isUpdated = true;
            }

            if (UtilValidate.isNotEmpty(objectInfo) && !objectInfo.equals(dataResource.getString("objectInfo"))) {
                dataResource.set("objectInfo", objectInfo);
                isUpdated = true;
            }

            if (UtilValidate.isNotEmpty(localeString) && !localeString.equals(dataResource.getString("localeString"))) {
                dataResource.set("localeString", localeString);
                isUpdated = true;
            }

            if (isUpdated) {
                dataResource.store();
                Debug.logInfo("Updated DataResource: " + dataResourceId, MODULE);
            } else {
                Debug.logInfo("No changes detected in DataResource: " + dataResourceId, MODULE);
            }

            return ServiceUtil.returnSuccess("Digital asset updated successfully.");
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError("Error updating DigitalAsset: " + e.getMessage());
        }
    }
}
