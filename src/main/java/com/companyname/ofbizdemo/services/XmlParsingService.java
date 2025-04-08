package com.companyname.ofbizdemo.services;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;
import java.io.*;
import java.util.*;

import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.Debug;

public class XmlParsingService {
    private static final String MODULE = XmlParsingService.class.getName();

    public static Map<String, Object> parseXmlFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        String filePath = (String) context.get("filePath");
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<Map<String, Object>> itemsList = new ArrayList<>();
        boolean insideItems = false, insideItem = false;
        Map<String, Object> currentItem = null;
        int itemCount = 0;

        Debug.logInfo("Starting XML parsing for file: " + filePath, MODULE);

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (userLogin == null) {
            Debug.logError("User login is missing", MODULE);
            return ServiceUtil.returnError("User login is required.");
        }

        try (InputStream inputStream = new FileInputStream(new File(filePath))) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = factory.createXMLEventReader(inputStream);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    String elementName = event.asStartElement().getName().getLocalPart();

                    if ("Items".equals(elementName)) {
                        insideItems = true;
                        Debug.logInfo("Entered <Items> section.", MODULE);
                    } else if ("Item".equals(elementName) && insideItems) {
                        insideItem = true;
                        currentItem = new HashMap<>();
                        currentItem.put("Descriptions", new HashMap<String, String>());
                        currentItem.put("Attributes", new HashMap<String, String>());
                        currentItem.put("Packages", new ArrayList<String>());
                        currentItem.put("ExtendedInformation", new HashMap<String, String>());
                        currentItem.put("Pricing", new ArrayList<String>());
                        currentItem.put("ProductAttributes", new ArrayList<String>());
//                        currentItem.put("PartInterchangeInfo", new ArrayList<String>());
                        Debug.logInfo("Started processing <Item>", MODULE);
                    } else if (insideItem) {
                        switch (elementName) {
                            case "Description":
                                String descriptionCode = event.asStartElement().getAttributeByName(new QName("DescriptionCode")).getValue();
//                                String languageCode = event.asStartElement().getAttributeByName(new QName("LanguageCode")).getValue();
                                event = eventReader.nextEvent();
                                if (event.isCharacters() && !event.asCharacters().isWhiteSpace()) {
                                    String descText = event.asCharacters().getData().trim();
                                    Map<String, String> descriptions = (Map<String, String>) currentItem.get("Descriptions");
//                                    List<String> descriptions = (List<String>) currentItem.get("Descriptions");
                                    descriptions.put(descriptionCode, descText);
                                    Debug.logInfo("Captured Description: " + descText + ",Captured Description Code:" + descriptionCode, MODULE);
                                }
                                break;
                            case "ProductAttribute":
                                String attributeId = event.asStartElement().getAttributeByName(new QName("AttributeID")).getValue();
                                event = eventReader.nextEvent();
                                if (event.isCharacters() && !event.asCharacters().isWhiteSpace()) {
                                    String attrValue = event.asCharacters().getData().trim();
                                    Map<String, String> attributes = (Map<String, String>) currentItem.get("Attributes");
                                    attributes.put(attributeId, attrValue);
                                    Debug.logInfo("Captured Attribute: " + attributeId + " -> " + attrValue, MODULE);
                                }
                                break;
                            case "ExtendedProductInformation":
                                String expiCode = event.asStartElement().getAttributeByName(new QName("EXPICode")).getValue();
                                event = eventReader.nextEvent();
                                if (event.isCharacters() && !event.asCharacters().isWhiteSpace()) {
                                    String extText = event.asCharacters().getData().trim();
                                    Map<String, String> extendedInformation = (Map<String, String>) currentItem.get("ExtendedInformation");
//                                    List<String> descriptions = (List<String>) currentItem.get("ExtendedInformation");
                                    extendedInformation.put(expiCode, extText);
                                    Debug.logInfo("Captured Extended Product Information: " + extText + ",expiCode: " + expiCode, MODULE);
                                }
                                break;
                            case "PackageLevelGTIN":
                                event = eventReader.nextEvent();
                                if (event.isCharacters() && !event.asCharacters().isWhiteSpace()) {
                                    String packageGtin = event.asCharacters().getData().trim();
                                    List<String> packages = (List<String>) currentItem.get("Packages");
                                    packages.add(packageGtin);
                                    Debug.logInfo("Captured Package GTIN: " + packageGtin, MODULE);
                                }
                                break;
                            case "PackageBarCodeCharacters":
                                event = eventReader.nextEvent();
                                if (event.isCharacters() && !event.asCharacters().isWhiteSpace()) {
                                    String packageBarCodeCharacters = event.asCharacters().getData().trim();
                                    List<String> packages = (List<String>) currentItem.get("Packages");
                                    packages.add(packageBarCodeCharacters);
                                    Debug.logInfo("Captured Package Bar Code Characters: " + packageBarCodeCharacters, MODULE);
                                }
                                break;
                            case "Pricing":
                                String priceType = event.asStartElement().getAttributeByName(new QName("PriceType")).getValue();
                                event = eventReader.nextEvent();
                                if (event.isCharacters() && !event.asCharacters().isWhiteSpace()) {
                                    List<String> pricing = (List<String>) currentItem.get("Pricing");
                                    pricing.add(priceType);
                                    Debug.logInfo("Captured PriceType: " + priceType, MODULE);
                                }
                                break;
                            default:
                                event = eventReader.nextEvent();
                                if (event.isCharacters() && !event.asCharacters().isWhiteSpace()) {
                                    String value = event.asCharacters().getData().trim();
                                    currentItem.put(elementName, value);
                                    Debug.logInfo("Captured: " + elementName + " -> " + value, MODULE);
                                }
                                break;
                        }
                    }
                } else if (event.isEndElement()) {
                    String elementName = event.asEndElement().getName().getLocalPart();

                    if ("Item".equals(elementName) && insideItem) {
                        insideItem = false;
                        itemsList.add(currentItem);
                        itemCount++;
                        Debug.logInfo("Completed processing <Item>: " + currentItem, MODULE);

                        String partNumber = (String) currentItem.get("PartNumber");
                        String itemQuantitySize = (String) currentItem.get("ItemQuantitySize");
                        String quantityPerApplication = (String) currentItem.get("QuantityPerApplication");
                        String minimumOrderQuantity = (String) currentItem.get("MinimumOrderQuantity");

                        Debug.logInfo("Extracted values - PartNumber: " + partNumber +
                                ", ItemQuantitySize: " + itemQuantitySize +
                                ", QuantityPerApplication: " + quantityPerApplication +
                                ", MinimumOrderQuantity: " + minimumOrderQuantity, MODULE);


                    } else if ("Items".equals(elementName)) {
                        insideItems = false;
                        Debug.logInfo("Exited <Items> section. Stopping processing.", MODULE);
                        break;
                    }
                }
            }
            eventReader.close();
        } catch (Exception e) {
            Debug.logError(e, "Error parsing XML file: " + e.getMessage(), MODULE);
            return ServiceUtil.returnError("Error parsing XML file: " + e.getMessage());
        }
        Debug.logInfo("Total items found: " + itemCount, MODULE);
        Debug.logInfo("Final itemList: " + itemsList, MODULE);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("itemsList", itemsList);
        result.put("itemCount", itemCount);
        return result;
    }

//    private static void createItem(DispatchContext dctx, String partNumber, String itemQuantitySize, String quantityPerApplication, String minimumOrderQuantity, GenericValue userLogin) {
//        try {
//            GenericValue existingProduct = EntityQuery.use(dctx.getDelegator())
//                    .from("Product")
//                    .where("productId", partNumber)
//                    .queryOne();
//
//            if (existingProduct != null) {
//                Debug.logWarning("Product already exists: " + partNumber, MODULE);
//            } else {
//                Map<String, Object> productParams = UtilMisc.toMap(
//                        "productId", partNumber,
//                        "productTypeId", "FINISHED_GOOD",
//                        "internalName", partNumber,
//                        "piecesIncluded", itemQuantitySize,
//                        "quantityIncluded", quantityPerApplication,
//                        "orderDecimalQuantity", minimumOrderQuantity,
//                        "userLogin", userLogin
//                );
//
//                Map<String, Object> result = dctx.getDispatcher().runSync("createProduct", productParams);
//                Debug.logInfo("Product created successfully with productId: " + partNumber, MODULE);
//            }
//        } catch (Exception e) {
//            Debug.logError("Error creating product: " + e.getMessage(), MODULE);
//        }
//    }
}
