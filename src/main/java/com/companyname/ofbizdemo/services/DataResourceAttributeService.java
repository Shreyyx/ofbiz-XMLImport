package com.companyname.ofbizdemo.services;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.service.*;

import java.util.HashMap;
import java.util.Map;

public class DataResourceAttributeService {
    public static final String MODULE = DataResourceAttributeService.class.getName();

    public static Map<String, Object> createDataResourceAttributes(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // Prepare attributes
        String assetType = (String) context.get("assetType");
        String representation = (String) context.get("representation");
        String background = (String) context.get("background");
        String orientationView = (String) context.get("orientationView");
        String assetHeight = (String) context.get("assetHeight");
        String assetWidth = (String) context.get("assetWidth");
        String uri = (String) context.get("uri");

        try {
            // Step 1: Create a new DataResource
            String dataResourceId = delegator.getNextSeqId("DataResource");
            GenericValue newDataResource = delegator.makeValue("DataResource");
            newDataResource.set("dataResourceId", dataResourceId);
            newDataResource.set("dataResourceTypeId", "IMAGE_OBJECT"); // Set your desired type
            newDataResource.set("statusId", "CTNT_PUBLISHED"); // Optional
            newDataResource.set("dataResourceName", "AutoCreatedDataResource");
            newDataResource.set("createdDate", UtilDateTime.nowTimestamp());
            newDataResource.set("createdByUserLogin", userLogin.getString("userLoginId"));
            delegator.create(newDataResource);

            Debug.logInfo("Created new DataResource with ID: " + dataResourceId, MODULE);

            // Step 2: Create DataResourceAttribute entries
            Map<String, String> attributes = new HashMap<>();
            attributes.put("AssetType", assetType);
            attributes.put("Representation", representation);
            attributes.put("Background", background);
            attributes.put("OrientationView", orientationView);
            attributes.put("AssetHeight", assetHeight);
            attributes.put("AssetWidth", assetWidth);
            attributes.put("uri", uri);

            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                String tagName = entry.getKey();
                String tagValue = entry.getValue();

                if (UtilValidate.isEmpty(tagValue)) continue;

                GenericValue existingAttr = EntityQuery.use(delegator)
                        .from("DataResourceAttribute")
                        .where("dataResourceId", dataResourceId, "attrName", tagName)
                        .queryOne();

                if (UtilValidate.isEmpty(existingAttr)) {
                    Map<String, Object> attrParams = UtilMisc.toMap(
                            "dataResourceId", dataResourceId,
                            "attrName", tagName,
                            "attrValue", tagValue,
                            "userLogin", userLogin
                    );

                    Map<String, Object> result = dispatcher.runSync("createDataResourceAttribute", attrParams);

                    if (ServiceUtil.isSuccess(result)) {
                        Debug.logInfo("Created attribute: " + tagName + " = " + tagValue, MODULE);
                    } else {
                        Debug.logError("Error creating attribute: " + tagName + ": " + result.get("errorMessage"), MODULE);
                    }
                }
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("dataResourceId", dataResourceId);
            return result;

        } catch (Exception e) {
            Debug.logError(e, "Error during DataResource and Attribute creation", MODULE);
            return ServiceUtil.returnError("Service failed: " + e.getMessage());
        }
    }
}
