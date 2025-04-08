//package com.companyname.ofbizdemo.services;
//
//import org.apache.ofbiz.entity.GenericEntityException;
//import org.apache.ofbiz.base.util.Debug;
//import org.apache.ofbiz.base.util.UtilMisc;
//import org.apache.ofbiz.base.util.UtilValidate;
//import org.apache.ofbiz.entity.Delegator;
//import org.apache.ofbiz.entity.GenericValue;
//import org.apache.ofbiz.entity.util.EntityQuery;
//import org.apache.ofbiz.service.DispatchContext;
//import org.apache.ofbiz.service.GenericServiceException;
//import org.apache.ofbiz.service.ServiceUtil;
//import org.apache.ofbiz.base.util.UtilDateTime;
//
//import java.util.ArrayList;
//import javax.xml.namespace.QName;
//import javax.xml.stream.XMLStreamException;
//import javax.xml.stream.XMLEventReader;
//import javax.xml.stream.XMLInputFactory;
//import javax.xml.stream.events.*;
//import java.io.BufferedReader;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.List;
//import java.sql.Timestamp;
//
//public class ImportPiesProduct {
//    public static final String MODULE = ImportPiesProduct.class.getName();
//
//    public static Map<String, Object> importPiesProduct(DispatchContext dctx, Map<String, Object> context) {
//        String filePath = (String) context.get("filePath");
//        Debug.logInfo("Processing XML file: " + filePath, MODULE);
//
//        GenericValue userLogin = (GenericValue) context.get("userLogin");
//        if (userLogin == null) {
//            Debug.logError("User login is missing", MODULE);
//            return ServiceUtil.returnError("User login is required.");
//        }
//
//        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
//            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance(); //created an instance of XMLInputFactory which introduces the XML Parser
//            XMLEventReader reader = xmlInputFactory.createXMLEventReader(br); //XMLEventRader allows for pull based parsing
//
//            while (reader.hasNext()) { //checks if there are more XML events to process
//                XMLEvent nextEvent = reader.nextEvent(); //reader.nextEvent reads the next event
//
//                if (nextEvent.isStartElement()) { //checks if next event is a start tag
//                    StartElement startElement = nextEvent.asStartElement(); //etxracts the tag names and attribute names as well
//                    String tagName = startElement.getName().getLocalPart(); //we get the QName from this(Qualified Name (gets both tagname and namespace)), .getLocalPart()--> extracts only tag name ignoring namespace
//                    Debug.logInfo("Processing tag: " + tagName, MODULE);
//
//                    if ("Items".equals(tagName)) {
//                        List<Map<String, String>> items = new ArrayList<>();
//                        items = processItems(reader, dctx, userLogin);
//                        Debug.logInfo("Total items processed: " + items.size(), MODULE);
//                    }
//                }
//            }
//        } catch (FileNotFoundException e) {
//            Debug.logError("File not found: " + e.getMessage(), MODULE);
//            return ServiceUtil.returnError("File not found: " + e.getMessage());
//        } catch (Exception e) {
//            Debug.logError("Unexpected error: " + e.getMessage(), MODULE);
//            return ServiceUtil.returnError("Unexpected error: " + e.getMessage());
//        }
//
//        return ServiceUtil.returnSuccess("Products imported successfully");
//    }
//
//    private static List<Map<String, String>> processItems(XMLEventReader reader, DispatchContext dctx, GenericValue userLogin) throws XMLStreamException {
//        List<Map<String, String>> itemList = new ArrayList<>();
//
//        while (reader.hasNext()) {
//            XMLEvent event = reader.nextEvent();
//
//            if (event.isCharacters() && event.asCharacters().isWhiteSpace()) {
//                continue;
//            }
//
//            if (event.isStartElement() && "Item".equals(event.asStartElement().getName().getLocalPart())) {
//                StartElement startElement = event.asStartElement();
//
//                Map<String, String> itemData = processItem(reader, startElement, dctx, userLogin);
//                itemList.add(itemData);
//                Debug.logInfo("Processed Item: " + itemData, MODULE);
////                Debug.logInfo(itemList);
//            }
//
//            if (event.isEndElement() && "Items".equals(event.asEndElement().getName().getLocalPart())) {
//                break;
//            }
//        }
//        return itemList;
//    }
//
//    private static Map<String, String> processItem(XMLEventReader reader, StartElement startElement, DispatchContext dctx, GenericValue userLogin) {
//        String partNumber = null;
//        String itemQuantitySize = null;
//        String quantityPerApplication = null;
//        String minimumOrderQuantity = null;
//        String itemLevelGTIN = null;
//        String description = null;
//        String descriptionCode = null;
//        String languageCode = null;
//        String brandAAIAID = null;
//        String brandLabel = null;
//        String extendedProductInformation = null;
//        String expiCode = null;
//        String attributeId = null;
//        String productAttribute = null;
//
//
//        //A list of map is created here to store multiple values in a key-value pair
//        Map<String, String> itemData = new HashMap<>();
//        List<Map<String, String>> descriptions = new ArrayList<>();
//        List<Map<String, String>> extendedProductInformations = new ArrayList<>();
//        List<Map<String, String>> productAttributes = new ArrayList<>();
//
//        try {
//            while (reader.hasNext()) {
//                XMLEvent nextEvent = reader.nextEvent();
//                if (nextEvent.isStartElement()) {
//                    StartElement nestedElement = nextEvent.asStartElement();
//                    String nestedTagName = nestedElement.getName().getLocalPart();
//
//                    switch (nestedTagName) {
//                        case "PartNumber":
//                            nextEvent = reader.nextEvent();
//                            if (nextEvent.isCharacters()) { //gets for you the characters inside the tags
//                                partNumber = nextEvent.asCharacters().getData().trim();
//                                itemData.put("partNumber", partNumber);
//                                Debug.logInfo("Processing Part Number: " + partNumber, MODULE);
//                            }
//                            break;
//                        case "Packages":
//                            processPackages(reader, nestedElement, dctx, userLogin);
//                            break;
//                        case "DigitalAssets":
//                            processDigitalAssets(reader, nestedElement, dctx, userLogin);
//                            break;
//                        case "PartInterchangeInfo":
//                            processPartInterchangeInfo(reader, nestedElement, dctx, userLogin);
//                            break;
////                        case "Prices":
////                                processPrices(reader, nestedElement, dctx, userLogin, partNumber);
////                            break;
//                        case "ItemQuantitySize":
//                            nextEvent = reader.nextEvent();
//                            if (nextEvent.isCharacters()) {
//                                itemQuantitySize = nextEvent.asCharacters().getData().trim();
//                                itemData.put("itemQuantitySize", itemQuantitySize);
//                            }
//                            break;
//                        case "QuantityPerApplication":
//                            nextEvent = reader.nextEvent();
//                            if (nextEvent.isCharacters()) {
//                                quantityPerApplication = nextEvent.asCharacters().getData().trim();
//                                itemData.put("quantityPerApplication", quantityPerApplication);
//                            }
//                            break;
//                        case "MinimumOrderQuantity":
//                            nextEvent = reader.nextEvent();
//                            if (nextEvent.isCharacters()) {
//                                minimumOrderQuantity = nextEvent.asCharacters().getData().trim();
//                                itemData.put("minimumOrderQuantity", minimumOrderQuantity);
//                            }
//                            break;
//                        case "ItemLevelGTIN":
//                            nextEvent = reader.nextEvent();
//                            if (nextEvent.isCharacters()) {
//                                itemLevelGTIN = nextEvent.asCharacters().getData().trim();
//                                itemData.put("itemLevelGTIN", itemLevelGTIN);
//                            }
//                            break;
//                        case "BrandAAIAID":
//                            nextEvent = reader.nextEvent();
//                            if (nextEvent.isCharacters()) {
//                                brandAAIAID = nextEvent.asCharacters().getData().trim();
//                                itemData.put("brandAAIAID", brandAAIAID);
//                            }
//                            break;
//                        case "BrandLabel":
//                            nextEvent = reader.nextEvent();
//                            if (nextEvent.isCharacters()) {
//                                brandLabel = nextEvent.asCharacters().getData().trim();
//                                itemData.put("brandLabel", brandLabel);
//                            }
//                            break;
//                        case "Description":
//                            Attribute descriptionCodeAttr = nestedElement.getAttributeByName(new QName("DescriptionCode"));
//                            Attribute languageCodeAttr = nestedElement.getAttributeByName(new QName("LanguageCode"));
//
//                            descriptionCode = descriptionCodeAttr != null ? descriptionCodeAttr.getValue() : null; //checks if the value is null(avoids nullPointerException)
//                            languageCode = languageCodeAttr != null ? languageCodeAttr.getValue() : null;
//                            nextEvent = reader.nextEvent();
//                            description = nextEvent.asCharacters().getData().trim();
//
//                            if (UtilValidate.isNotEmpty(descriptionCode) && UtilValidate.isNotEmpty(languageCode) && UtilValidate.isNotEmpty(description)) {
//                                Map<String, String> descMap = new HashMap<>();
//                                descMap.put("descriptionCode", descriptionCode);
//                                descMap.put("languageCode", languageCode);
//                                descMap.put("description", description);
//                                descriptions.add(descMap);
//                            }
//                            break;
//                        case "ExtendedProductInformation":
//                            Attribute expiCodeAttr = nestedElement.getAttributeByName(new QName("EXPICode"));
//                            expiCode = expiCodeAttr != null ? expiCodeAttr.getValue() : null;
//                            nextEvent = reader.nextEvent();
//                            extendedProductInformation = nextEvent.asCharacters().getData().trim();
//
//                            if (UtilValidate.isNotEmpty(expiCode) && UtilValidate.isNotEmpty(extendedProductInformation)) {
//                                Map<String, String> extMap = new HashMap<>();
//                                extMap.put("expiCode", expiCode);
//                                extMap.put("extendedProductInformation", extendedProductInformation);
//                                extendedProductInformations.add(extMap);
//                            }
//                            break;
//                        case "ProductAttribute":
//                            Attribute attributeIdAttr = nestedElement.getAttributeByName(new QName("AttributeID"));
//                            attributeId = attributeIdAttr != null ? attributeIdAttr.getValue() : null;
//                            nextEvent = reader.nextEvent();
//                            productAttribute = nextEvent.asCharacters().getData().trim();
//
//                            if (UtilValidate.isNotEmpty(attributeId) && UtilValidate.isNotEmpty(productAttribute)) {
//                                Map<String, String> attrMap = new HashMap<>();
//                                attrMap.put("attributeId", attributeId);
//                                attrMap.put("productAttribute", productAttribute);
//                                productAttributes.add(attrMap);
//                            }
//                            break;
//                        default:
//                            break;
//                    }
//                } else if (nextEvent.isEndElement()) {
//                        EndElement endElement = nextEvent.asEndElement();
//                        String tagName = endElement.getName().getLocalPart();
//                        Debug.logInfo("Closing tag encountered: " + tagName, MODULE);
//                        if ("Items".equals(tagName)) {
//                            break;
//                        }
//                    }
//                }
//            if (UtilValidate.isNotEmpty(partNumber)) {
//                createItem(dctx, partNumber, itemQuantitySize, quantityPerApplication, minimumOrderQuantity, userLogin);
//            }
//
//            if (UtilValidate.isNotEmpty(itemLevelGTIN)) {
//                createItemLevelGTIN(dctx, partNumber, itemLevelGTIN, userLogin);
//            }
//
//            if (UtilValidate.isNotEmpty(brandAAIAID)) {
//                createProductCategory(dctx, brandAAIAID, brandLabel, userLogin);
//            }
//
//            if (UtilValidate.isNotEmpty(descriptions)) {
//                for (Map<String, String> desc : descriptions) { //extracts each key-value pair and processes it
//                    String descriptionCode1 = desc.get("descriptionCode");
//                    String languageCode1 = desc.get("languageCode");
//                    String description1 = desc.get("description");
//
//                    if (UtilValidate.isNotEmpty(description1)) {
//                        createDescription(dctx, descriptionCode1, languageCode1, description1, userLogin);
//                    }
//                }
//            }
//
//            if (UtilValidate.isNotEmpty(extendedProductInformations)) {
//                for (Map<String, String> ext : extendedProductInformations) {
//                    String expiCode1 = ext.get("expiCode");
//                    String extendedProductInformation1 = ext.get("extendedProductInformation");
//
//                    if (UtilValidate.isNotEmpty(extendedProductInformation1)) {
//                        storeExtendedProductInformation(dctx, expiCode1, extendedProductInformation1, userLogin);
//                    }
//                }
//            }
//
//            if (UtilValidate.isNotEmpty(productAttributes)) {
//                for (Map<String, String> attr : productAttributes) {
//                    String attributeId1 = attr.get("attributeId");
//                    String productAttribute1 = attr.get("productAttribute");
//
//                    if (UtilValidate.isNotEmpty(productAttribute1)) {
//                        storeProductAttribute(dctx, partNumber, attributeId1, productAttribute1, userLogin);
//                    }
//                }
//            }
//
//            if (!descriptions.isEmpty()) {
//                itemData.put("descriptions", descriptions.toString());
//            }
//            if (!extendedProductInformations.isEmpty()) {
//                itemData.put("extendedProductInformations", extendedProductInformations.toString());
//            }
//            if (!productAttributes.isEmpty()) {
//                itemData.put("productAttributes", productAttributes.toString());
//            }
//
//        } catch (Exception e) {
//            Debug.logError("Error processing item: " + e.getMessage(), MODULE);
//        }
//        return itemData;
//    }
//
//    private static void processPackages(XMLEventReader reader, StartElement startElement, DispatchContext dctx, GenericValue userLogin) {
//        List<Map<String, String>> packagesList = new ArrayList<>();
//
//        String productFeatureId = null;
//
//        try {
//            while (reader.hasNext()) {
//                XMLEvent nextEvent = reader.nextEvent();
//
//                if (nextEvent.isStartElement()) {
//                    StartElement nestedElement = nextEvent.asStartElement();
//                    String nestedTagName = nestedElement.getName().getLocalPart();
//
//                    if ("Package".equals(nestedTagName)) {
//                        String packageLevelGTIN = null;
//                        String packageBarCodeCharacters = null;
//
//                        while (reader.hasNext()) {
//                            nextEvent = reader.nextEvent();
//
//                            if (nextEvent.isStartElement() && "PackageLevelGTIN".equals(nextEvent.asStartElement().getName().getLocalPart())) {
//                                nextEvent = reader.nextEvent();
//                                if (nextEvent.isCharacters()) {
//                                    packageLevelGTIN = nextEvent.asCharacters().getData().trim();
//                                }
//                            } else if (nextEvent.isStartElement() && "PackageBarCodeCharacters".equals(nextEvent.asStartElement().getName().getLocalPart())) {
//                                nextEvent = reader.nextEvent();
//                                if (nextEvent.isCharacters()) {
//                                    packageBarCodeCharacters = nextEvent.asCharacters().getData().trim();
//                                }
//                            } else if (nextEvent.isEndElement() && "Package".equals(nextEvent.asEndElement().getName().getLocalPart())) {
//                                break;
//                            }
//                        }
//
//                        if (UtilValidate.isNotEmpty(packageLevelGTIN)) {
//                            Map<String, String> packagesMap = new HashMap<>();
//                            packagesMap.put("packageLevelGTIN", packageLevelGTIN);
//                            packagesMap.put("packageBarCodeCharacters", packageBarCodeCharacters);
//                            packagesList.add(packagesMap);
//                        }
//                    } else if (nextEvent.isEndElement() && "Packages".equals(nextEvent.asEndElement().getName().getLocalPart())) {
//                        break;
//                    }
//                }
//            }
//
//            if (UtilValidate.isNotEmpty(packagesList)) {
//                for (Map<String, String> packages : packagesList) {
//                    createPackages(dctx, packages.get("packageLevelGTIN"), packages.get("packageBarCodeCharacters"), productFeatureId, userLogin);
//                }
//            }
//
//        } catch (Exception e) {
//            Debug.logError("Error processing package information: " + e.getMessage(), MODULE);
//        }
//    }
//
//    private static void processDigitalAssets(XMLEventReader reader, StartElement startElement, DispatchContext dctx, GenericValue userLogin) {
//        List<Map<String, String>> digitalAssetList = new ArrayList<>();
//
//        String dataResourceId = null;
//
//        try {
//            while (reader.hasNext()) {
//                XMLEvent nextEvent = reader.nextEvent();
//
//                if (nextEvent.isStartElement()) {
//                    StartElement nestedElement = nextEvent.asStartElement();
//                    String nestedTagName = nestedElement.getName().getLocalPart();
//
//                    if ("DigitalFileInformation".equals(nestedTagName)) {
//                        Attribute languageCodeAttr = nestedElement.getAttributeByName(new QName("LanguageCode"));
//                        String languageCode = (languageCodeAttr != null) ? languageCodeAttr.getValue().trim() : null;
//
//                        String fileName = null;
//                        String fileType = null;
//
//                        while (reader.hasNext()) {
//                            nextEvent = reader.nextEvent();
//
//                            if (nextEvent.isStartElement() && "FileName".equals(nextEvent.asStartElement().getName().getLocalPart())) {
//                                nextEvent = reader.nextEvent();
//                                if (nextEvent.isCharacters()) {
//                                    fileName = nextEvent.asCharacters().getData().trim();
//                                    Debug.logInfo("Extracted fileName: " + fileName, MODULE);
//                                }
//                            } else if (nextEvent.isStartElement() && "FileType".equals(nextEvent.asStartElement().getName().getLocalPart())) {
//                                nextEvent = reader.nextEvent();
//                                if (nextEvent.isCharacters()) {
//                                    fileType = nextEvent.asCharacters().getData().trim();
//                                    Debug.logInfo("Extracted fileType: " + fileType, MODULE);
//                                }
//                            } else if (nextEvent.isEndElement() && "DigitalFileInformation".equals(nextEvent.asEndElement().getName().getLocalPart())) {
//                                break;
//                            }
//                        }
//
//                        if (UtilValidate.isNotEmpty(languageCode) && UtilValidate.isNotEmpty(fileName) && UtilValidate.isNotEmpty(fileType)) {
//                            Map<String, String> digitalAssetMap = new HashMap<>();
//                            digitalAssetMap.put("languageCode", languageCode);
//                            digitalAssetMap.put("fileName", fileName);
//                            digitalAssetMap.put("fileType", fileType);
//                            digitalAssetList.add(digitalAssetMap);
//
//                            Debug.logInfo("Added digital asset: LanguageCode=" + languageCode + ", FileName=" + fileName + ", FileType=" + fileType, MODULE);
//                        }
//                    }
//
//                    if (nextEvent.isEndElement() && "DigitalAssets".equals(nextEvent.asEndElement().getName().getLocalPart())) {
//                        break;
//                    }
//                }
//            }
//
//            if (UtilValidate.isNotEmpty(digitalAssetList)) {
//                for (Map<String, String> digitalAsset : digitalAssetList) {
//                    createDigitalAssets(dctx, digitalAsset.get("languageCode"), digitalAsset.get("fileName"), digitalAsset.get("fileType"), dataResourceId, userLogin);
//                }
//            }
//
//        } catch (Exception e) {
//            Debug.logError("Error processing digital asset information: " + e.getMessage(), MODULE);
//        }
//    }
//
//
//    private static void processPartInterchangeInfo(XMLEventReader reader, StartElement startElement, DispatchContext dctx, GenericValue userLogin) {
//        List<Map<String, String>> partInterchangeInfos = new ArrayList<>();
//
//        try {
//            while (reader.hasNext()) {
//                XMLEvent nextEvent = reader.nextEvent();
//
//                if (nextEvent.isStartElement()) {
//                    StartElement nestedElement = nextEvent.asStartElement();
//                    String nestedTagName = nestedElement.getName().getLocalPart();
//
//                    if ("PartInterchange".equals(nestedTagName)) {
//                        Attribute brandAAIAIDAttr = nestedElement.getAttributeByName(new QName("BrandAAIAID"));
//                        Attribute brandLabelAttr = nestedElement.getAttributeByName(new QName("BrandLabel"));
//
//                        String brandAAIAID = brandAAIAIDAttr != null ? brandAAIAIDAttr.getValue().trim() : null;
//                        String brandLabel = brandLabelAttr != null ? brandLabelAttr.getValue().trim() : null;
//                        String partNumber = null;
//
//                        while (reader.hasNext()) {
//                            Attribute interchangeQuantityAttr = nestedElement.getAttributeByName(new QName("InterchangeQuantity"));
//                            String interchangeQuantity = interchangeQuantityAttr != null ? interchangeQuantityAttr.getValue().trim() : null;
//                            nextEvent = reader.nextEvent();
//                            if (nextEvent.isStartElement() && "PartNumber".equals(nextEvent.asStartElement().getName().getLocalPart())) {
//                                nextEvent = reader.nextEvent();
//                                if (nextEvent.isCharacters()) {
//                                    partNumber = nextEvent.asCharacters().getData().trim();
////                                    Debug.logInfo("part number under part interchange info: " + partNumber, MODULE);
//                                }
//                            } else if (nextEvent.isEndElement() && "PartInterchange".equals(nextEvent.asEndElement().getName().getLocalPart())) {
//                                break;
//                            }
//
//                            if (UtilValidate.isNotEmpty(brandAAIAID) && UtilValidate.isNotEmpty(brandLabel) && UtilValidate.isNotEmpty(partNumber)) {
//                                Map<String, String> interchangeMap = new HashMap<>();
//                                interchangeMap.put("brandAAIAID", brandAAIAID);
//                                interchangeMap.put("brandLabel", brandLabel);
//                                interchangeMap.put("partNumber", partNumber);
//                                interchangeMap.put("interchangeQuantity", interchangeQuantity);
//                                partInterchangeInfos.add(interchangeMap);
//                            }
//                        }
//                    } else if (nextEvent.isEndElement() && "PartInterchangeInfo".equals(nextEvent.asEndElement().getName().getLocalPart())) {
//                        break;
//                    }
//                }
//                if (UtilValidate.isNotEmpty(partInterchangeInfos)) {
//                    for (Map<String, String> interchange : partInterchangeInfos) {
//                        createPartInterchangeInfo(dctx, interchange.get("brandAAIAID"), interchange.get("brandLabel"), interchange.get("partNumber"), interchange.get("interchangeQuantity"), userLogin);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            Debug.logError("Error processing part interchange info: " + e.getMessage(), MODULE);
//        }
//    }
//
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
//
//    private static void createItemLevelGTIN(DispatchContext dctx, String partNumber, String itemLevelGTIN, GenericValue userLogin) {
//        try {
//            Map<String, Object> fields = UtilMisc.toMap(
//                    "goodIdentificationTypeId", "GTIN",
//                    "description", "Global Trade Item Number (GTIN)",
//                    "userLogin", userLogin
//            );
//
//            Map<String, Object> result = dctx.getDispatcher().runSync("createGoodIdentificationType", fields);
//
//            Map<String, Object> goodIdentificationParams = UtilMisc.toMap(
//                    "goodIdentificationTypeId", "GTIN",
//                    "productId", partNumber,
//                    "idValue", itemLevelGTIN,
//                    "userLogin", userLogin
//            );
//
//            Map<String, Object> resultGoodIdentification = dctx.getDispatcher().runSync("createGoodIdentification", goodIdentificationParams);
//            Debug.logInfo("Good Identification created successfully for GTIN: " + itemLevelGTIN, MODULE);
//
//        } catch (GenericServiceException e) {
//            Debug.logError("Error creating Good Identification: " + e.getMessage(), MODULE);
//        }
//    }
//
//    public static void createProductCategory(DispatchContext dctx, String brandAAIAID, String brandLabel, GenericValue userLogin) {
//
//        Delegator delegator = dctx.getDelegator();
//        String productCategoryId = delegator.getNextSeqId("ProductCategory");
//
//        try {
//            GenericValue productCategoryType = EntityQuery.use(delegator)
//                    .from("ProductCategoryType")
//                    .where("productCategoryTypeId", "BRAND_CATEGORY")
//                    .queryOne();
//
//            if (UtilValidate.isEmpty(productCategoryType)) {
//                Map<String, Object> productCategoryTypeParams = UtilMisc.toMap(
//                        "productCategoryTypeId", "BRAND_CATEGORY",
//                        "userLogin", userLogin
//                );
//
//                Map<String, Object> categoryTypeResult = dctx.getDispatcher().runSync("createProductCategoryType", productCategoryTypeParams);
//
//                if (!ServiceUtil.isSuccess(categoryTypeResult)) {
//                    Debug.logError("Error creating ProductCategoryType: " + categoryTypeResult.get("errorMessage"), MODULE);
//                    return;
//                }
//            }
//
//            GenericValue existingProductCategory = EntityQuery.use(delegator)
//                    .from("ProductCategory")
//                    .where("productCategoryId", productCategoryId)
//                    .queryOne();
//
//            if (existingProductCategory != null) {
//                Debug.logWarning("Product with productCategoryId: " + productCategoryId + " already exists.", MODULE);
//                return;
//            }
//
//            Map<String, Object> productCategoryParams = UtilMisc.toMap(
//                    "productCategoryId", productCategoryId,
//                    "productCategoryTypeId", "BRAND_CATEGORY",
//                    "categoryName", brandAAIAID,
//                    "description", brandLabel,
//                    "userLogin", userLogin
//            );
//
//            Map<String, Object> categoryResult = dctx.getDispatcher().runSync("createProductCategory", productCategoryParams);
//
//            if (ServiceUtil.isSuccess(categoryResult)) {
//                Debug.logInfo("Product Category created successfully with productCategoryId: " + productCategoryId, MODULE);
//            } else {
//                Debug.logError("Error creating ProductCategory: " + categoryResult.get("errorMessage"), MODULE);
//            }
//
//        } catch (GenericServiceException e) {
//            Debug.logError("ServiceException while creating product category: " + e.getMessage(), MODULE);
//        } catch (Exception e) {
//            Debug.logError("Unexpected error while creating product category: " + e.getMessage(), MODULE);
//        }
//    }
//
//    private static void createDescription(DispatchContext dctx, String descriptionCode, String languageCode, String description, GenericValue userLogin) {
//
//        Delegator delegator = dctx.getDelegator();
//        String contentId = delegator.getNextSeqId("Content");
//
//        try {
//            if (description.length() > 255) {
//                Map<String, Object> electronicTextParams = UtilMisc.toMap(
//                        "textData", description,
//                        "userLogin", userLogin
//                );
//
//                Map<String, Object> electronicTextResult = dctx.getDispatcher().runSync("createElectronicText", electronicTextParams);
//
//                if (ServiceUtil.isSuccess(electronicTextResult)) {
//                    String dataResourceId = (String) electronicTextResult.get("dataResourceId");
//
//                    Map<String, Object> contentParams = UtilMisc.toMap(
//                            "contentId", contentId,
//                            "contentName", descriptionCode,
//                            "contentTypeId", "DOCUMENT",
//                            "localeString", languageCode,
//                            "description", null,
//                            "dataResourceId", dataResourceId,
//                            "userLogin", userLogin
//                    );
//                } else {
//                    Debug.logError("Error creating ElectronicText: " + electronicTextResult.get("errorMessage"), MODULE);
//                }
//            } else {
//                Map<String, Object> contentParams = UtilMisc.toMap(
//                        "contentId", contentId,
//                        "contentName", descriptionCode,
//                        "contentTypeId", "DOCUMENT",
//                        "localeString", languageCode,
//                        "description", description,
//                        "userLogin", userLogin
//                );
//                Map<String, Object> contentResult = dctx.getDispatcher().runSync("createContent", contentParams);
//                if (ServiceUtil.isSuccess(contentResult)) {
//                    Debug.logInfo("Content created successfully with contentId: " + contentId, MODULE);
//                }
//            }
//        } catch (GenericServiceException e) {
//            Debug.logError("Error creating description: " + e.getMessage(), MODULE);
//        }
//    }
//
//    public static void storeExtendedProductInformation(DispatchContext dctx, String expiCode, String extendedProductInformation, GenericValue userLogin) {
//
//        Delegator delegator = dctx.getDelegator();
//        String productFeatureId = delegator.getNextSeqId("ProductFeature");
//
//        try {
//            GenericValue productFeatureType = EntityQuery.use(delegator)
//                    .from("ProductFeatureType")
//                    .where("productFeatureTypeId", "EXPI")
//                    .queryOne();
//
//            if (UtilValidate.isEmpty(productFeatureType)) {
//                Map<String, Object> productFeatureTypeParams = UtilMisc.toMap(
//                        "productFeatureTypeId", "EXPI",
//                        "userLogin", userLogin
//                );
//
//                Map<String, Object> featureTypeResult = dctx.getDispatcher().runSync("createProductFeatureType", productFeatureTypeParams);
//
//                if (!ServiceUtil.isSuccess(featureTypeResult)) {
//                    Debug.logError("Error creating ProductFeatureType: " + featureTypeResult.get("errorMessage"), MODULE);
//                    return;
//                }
//            }
//
//            GenericValue existingProductFeature = EntityQuery.use(delegator)
//                    .from("ProductFeature")
//                    .where("productFeatureId", productFeatureId)
//                    .queryOne();
//
//            if (existingProductFeature != null) {
//                Debug.logWarning("Product with productFeatureId: " + productFeatureId + " already exists.", MODULE);
//                return;
//            }
//
//            Map<String, Object> productFeatureParams = UtilMisc.toMap(
//                    "productFeatureId", productFeatureId,
//                    "productFeatureTypeId", "EXPI",
//                    "idCode", expiCode,
//                    "description", extendedProductInformation,
//                    "userLogin", userLogin
//            );
//
//            Map<String, Object> featureResult = dctx.getDispatcher().runSync("createProductFeature", productFeatureParams);
//
//            if (ServiceUtil.isSuccess(featureResult)) {
//                Debug.logInfo("Product Feature created successfully with productFeatureId: " + productFeatureId, MODULE);
//            } else {
//                Debug.logError("Error creating ProductFeature: " + featureResult.get("errorMessage"), MODULE);
//            }
//
//
//        } catch (GenericServiceException e) {
//            Debug.logError("ServiceException while creating product feature: " + e.getMessage(), MODULE);
//        } catch (Exception e) {
//            Debug.logError("Unexpected error while creating product feature: " + e.getMessage(), MODULE);
//        }
//    }
//
//    private static void storeProductAttribute(DispatchContext dctx, String partNumber, String attributeId, String productAttribute, GenericValue userLogin) {
//
//        try {
//            GenericValue existingProductAttribute = EntityQuery.use(dctx.getDelegator())
//                    .from("ProductAttribute")
//                    .where("productId", partNumber, "attrName", attributeId)
//                    .queryOne();
//
//            if (existingProductAttribute != null) {
//                Debug.logWarning("Product Attribute already exists", MODULE);
//            } else {
//                Map<String, Object> productAttributeParams = UtilMisc.toMap(
//                        "productId", partNumber,
//                        "attrName", attributeId,
//                        "attrValue", productAttribute,
//                        "attrType", "PADB_ATTRIBUTE",
//                        "userLogin", userLogin
//                );
//
//                Map<String, Object> productAttributeresult = dctx.getDispatcher().runSync("createProductAttribute", productAttributeParams);
//                Debug.logInfo("Product Attribute created successfully", MODULE);
//            }
//        } catch (Exception e) {
//            Debug.logError("Error creating product attribute: " + e.getMessage(), MODULE);
//        }
//    }
//
//    private static void createPartInterchangeInfo(DispatchContext dctx, String brandAAIAID, String brandLabel, String partNumber, String interchangeQuantity, GenericValue userLogin) {
//        Delegator delegator = dctx.getDelegator();
//
//        try {
//
//            GenericValue productCategoryType = EntityQuery.use(delegator)
//                    .from("ProductCategoryType")
//                    .where("productCategoryTypeId", "BRAND_CATEGORY")
//                    .queryOne();
//
//            if (UtilValidate.isEmpty(productCategoryType)) {
//                Map<String, Object> productCategoryTypeParams = UtilMisc.toMap(
//                        "productCategoryTypeId", "BRAND_CATEGORY",
//                        "userLogin", userLogin
//                );
//
//                Map<String, Object> categoryTypeResult = dctx.getDispatcher().runSync("createProductCategoryType", productCategoryTypeParams);
//
//                if (!ServiceUtil.isSuccess(categoryTypeResult)) {
//                    Debug.logError("Error creating ProductCategoryType: " + categoryTypeResult.get("errorMessage"), MODULE);
//                    return;
//                }
//            }
//
//            GenericValue existingProductCategory = EntityQuery.use(delegator)
//                    .from("ProductCategory")
//                    .where("categoryName", brandAAIAID)
//                    .queryOne();
//
//            String productCategoryId;
//            if (existingProductCategory != null) {
//                productCategoryId = existingProductCategory.getString("productCategoryId");
//                Debug.logInfo("Product Category already exists with ID: " + productCategoryId, MODULE);
//            } else {
//                productCategoryId = delegator.getNextSeqId("ProductCategory");
//
//                Map<String, Object> productCategoryParams = UtilMisc.toMap(
//                        "productCategoryId", productCategoryId,
//                        "productCategoryTypeId", "BRAND_CATEGORY",
//                        "categoryName", brandAAIAID,
//                        "description", brandLabel,
//                        "userLogin", userLogin
//                );
//
//                Map<String, Object> categoryResult = dctx.getDispatcher().runSync("createProductCategory", productCategoryParams);
//
//                if (!ServiceUtil.isSuccess(categoryResult)) {
//                    Debug.logError("Error creating ProductCategory: " + categoryResult.get("errorMessage"), MODULE);
//                    return;
//                }
//                Debug.logInfo("Product Category created successfully with productCategoryId: " + productCategoryId, MODULE);
//            }
//
//            GenericValue existingProduct = EntityQuery.use(delegator)
//                    .from("Product")
//                    .where("productId", partNumber)
//                    .queryOne();
//
//            if (existingProduct == null) {
//                Map<String, Object> productParams = UtilMisc.toMap(
//                        "productId", partNumber,
//                        "productTypeId", "FINISHED_GOOD",
//                        "internalName", partNumber,
//                        "userLogin", userLogin
//                );
//
//                Map<String, Object> productResult = dctx.getDispatcher().runSync("createProduct", productParams);
//
//                if (!ServiceUtil.isSuccess(productResult)) {
//                    Debug.logError("Error creating Product: " + productResult.get("errorMessage"), MODULE);
//                    return;
//                }
//                Debug.logInfo("Product created successfully with productId: " + partNumber, MODULE);
//            } else {
//                Debug.logWarning("Product already exists: " + partNumber, MODULE);
//            }
//
//            Map<String, Object> productAssocParams = UtilMisc.toMap(
//                    "productId", "899774",
//                    "productIdTo", partNumber,
//                    "productAssocTypeId", "PRODUCT_SUBSTITUTE",
//                    "fromDate", UtilDateTime.nowTimestamp(),
//                    "quantity", interchangeQuantity,
//                    "userLogin", userLogin
//            );
//
//            Map<String, Object> productAssocResult = dctx.getDispatcher().runSync("createProductAssoc", productAssocParams);
//
//            if (!ServiceUtil.isSuccess(productAssocResult)) {
//                Debug.logError("Error creating Product Association: " + productAssocResult.get("errorMessage"), MODULE);
//                return;
//            }
//            Debug.logInfo("Product Association created successfully between 877994 and " + partNumber, MODULE);
//
//        } catch (Exception e) {
//            Debug.logError(e, "Error creating PartInterchangeInfo", MODULE);
//        }
//    }
//
//
////    private static void createPrices(DispatchContext dctx, String partNumber, String priceType, String price, GenericValue userLogin) {
////
////        Delegator delegator = dctx.getDelegator();
////
////        try {
////            GenericValue productPriceType = EntityQuery.use(delegator)
////                    .from("ProductPriceType")
////                    .where("productPriceTypeId", priceType)
////                    .queryOne();
////
////            if (UtilValidate.isEmpty(productPriceType)) {
////                Map<String, Object> productPriceTypeParams = UtilMisc.toMap(
////                        "productPriceTypeId", priceType,
////                        "userLogin", userLogin
////                );
////
////                Map<String, Object> priceTypeResult = dctx.getDispatcher().runSync("createProductPriceType", productPriceTypeParams);
////
////                if (!ServiceUtil.isSuccess(priceTypeResult)) {
////                    Debug.logError("Error creating ProductPriceType: " + priceTypeResult.get("errorMessage"), MODULE);
////                    return;
////                }
////            }
////
////            Map<String, Object> productPriceParams = UtilMisc.toMap(
////                    "productId", partNumber,
////                    "productPriceTypeId", priceType,
////                    "productPricePurposeId", "PURCHASE",
////                    "currencyUomId", "USD",
////                    "productStoreGroupId", "_NA_",
////                    "fromDate", "2025-03-29 12:00:00",
////                    "price", price,
////                    "userLogin", userLogin
////            );
////
////            Map<String, Object> priceResult = dctx.getDispatcher().runSync("createProductPrice", productPriceParams);
////
////            if (!ServiceUtil.isSuccess(priceResult)) {
////                Debug.logError("Error creating ProductPrice: " + priceResult.get("errorMessage"), MODULE);
////                return;
////            }
////
////            Debug.logInfo("Product Price created successfully for PartNumber: " + partNumber, MODULE);
////        } catch (Exception e) {
////            Debug.logError(e, "Error creating ProductPrice", MODULE);
////        }
////    }
//
//    private static void createDigitalAssets(DispatchContext dctx, String languageCode, String fileName, String fileType, String dataResourceId, GenericValue userLogin) {
//
//        Delegator delegator = dctx.getDelegator();
//
//        try {
//            Map<String, Object> dataResourceParams = UtilMisc.toMap(
//                    "dataResourceId", dataResourceId,
//                    "dataResourceTypeId", "IMAGE_OBJECT",
//                    "dataResourceName", fileName,
//                    "localeString", languageCode,
//                    "mimeTypeId", fileType,
//                    "userLogin", userLogin
//            );
//
//            Map<String, Object> dataResourceResult = dctx.getDispatcher().runSync("createDataResource", dataResourceParams);
//
//            if (!ServiceUtil.isSuccess(dataResourceResult)) {
//                Debug.logError("Error creating data resource: " + dataResourceResult.get("errorMessage"), MODULE);
//                return;
//            }
//        } catch (GenericServiceException e) {
//            Debug.logError(e, "Error creating Data Resource", MODULE);
//        }
//    }
//
////    public static void createPackages(DispatchContext dctx, String packageLevelGTIN, String productFeatureId, GenericValue userLogin) {
////
////        Delegator delegator = dctx.getDelegator();
////
////        try {
////            GenericValue productFeatureCategory = EntityQuery.use(delegator)
////                    .from("ProductFeatureCategory")
////                    .where("productFeatureCategoryId", "PACKAGE")
////                    .queryOne();
////
////            if (UtilValidate.isEmpty(productFeatureCategory)) {
////                Map<String, Object> productFeatureCategoryTypeParams = UtilMisc.toMap(
////                        "productFeatureCategoryId", "PACKAGE",
////                        "userLogin", userLogin
////                );
////
////                Map<String, Object> FeaturecategoryResult = dctx.getDispatcher().runSync("createProductFeatureCategory", productFeatureCategoryTypeParams);
////
////                if (!ServiceUtil.isSuccess(FeaturecategoryResult)) {
////                    Debug.logError("Error creating Product Feature Category: " + FeaturecategoryResult.get("errorMessage"), MODULE);
////                    return;
////                }
////            }
////
////            GenericValue existingProductFeature = EntityQuery.use(delegator)
////                    .from("ProductFeature")
////                    .where("productFeatureId", productFeatureId)
////                    .queryOne();
////
////            if (existingProductFeature != null) {
////                Debug.logWarning("Product with productFeatureId: " + productFeatureId + " already exists.", MODULE);
////                return;
////            }
////
////            Map<String, Object> productFeatureParams = UtilMisc.toMap(
////                    "productFeatureId", productFeatureId,
////                    "productFeatureTypeId", "OTHER_FEATURE",
////                    "productFeatureCategoryId", "PACKAGE",
////                    "numberSpecified", packageLevelGTIN,
////                    "description", "Package Info",
////                    "userLogin", userLogin
////            );
////
////            Map<String, Object> featureResult = dctx.getDispatcher().runSync("createProductFeature", productFeatureParams);
////
////            if (ServiceUtil.isSuccess(featureResult)) {
////                Debug.logInfo("Product feature created successfully with productFeatureId: " + productFeatureId, MODULE);
////            } else {
////                Debug.logError("Error creating ProductFeature: " + featureResult.get("errorMessage"), MODULE);
////            }
////
////        } catch (GenericServiceException e) {
////            Debug.logError("ServiceException while creating product feature: " + e.getMessage(), MODULE);
////        } catch (Exception e) {
////            Debug.logError("Unexpected error while creating product feature: " + e.getMessage(), MODULE);
////        }
////    }
//
//    public static void createPackages(DispatchContext dctx, String packageLevelGTIN, String packageBarCodeCharacters, String productFeatureId, GenericValue userLogin) {
//        Delegator delegator = dctx.getDelegator();
//
//        try {
//            // Check and create ProductFeatureCategory if not exists
//            GenericValue productFeatureCategory = EntityQuery.use(delegator)
//                    .from("ProductFeatureCategory")
//                    .where("productFeatureCategoryId", "PACKAGE")
//                    .queryOne();
//
//            if (UtilValidate.isEmpty(productFeatureCategory)) {
//                Map<String, Object> productFeatureCategoryTypeParams = UtilMisc.toMap(
//                        "productFeatureCategoryId", "PACKAGE",
//                        "userLogin", userLogin
//                );
//
//                Map<String, Object> FeaturecategoryResult = dctx.getDispatcher().runSync("createProductFeatureCategory", productFeatureCategoryTypeParams);
//
//                if (!ServiceUtil.isSuccess(FeaturecategoryResult)) {
//                    Debug.logError("Error creating Product Feature Category: " + FeaturecategoryResult.get("errorMessage"), MODULE);
//                    return;
//                }
//            }
//
//            // Store GTIN if available
//            if (UtilValidate.isNotEmpty(packageLevelGTIN)) {
//                createProductFeatureEntry(dctx, productFeatureId, packageLevelGTIN, userLogin);
//            }
//
//            // Store Barcode Characters as a separate entry
//            if (UtilValidate.isNotEmpty(packageBarCodeCharacters)) {
//                String barcodeFeatureId = productFeatureId + "_BC"; // Unique ID for barcode feature
//                createProductFeatureEntry(dctx, barcodeFeatureId, packageBarCodeCharacters, userLogin);
//            }
//        } catch (Exception e) {
//            Debug.logError("Unexpected error while creating product feature: " + e.getMessage(), MODULE);
//        }
//    }
//
//    // Helper method to create product feature entry
//    private static void createProductFeatureEntry(DispatchContext dctx, String productFeatureId, String numberSpecified, GenericValue userLogin) {
//        Delegator delegator = dctx.getDelegator();
//
//        try {
//            GenericValue existingProductFeature = EntityQuery.use(delegator)
//                    .from("ProductFeature")
//                    .where("productFeatureId", productFeatureId)
//                    .queryOne();
//
//            if (existingProductFeature != null) {
//                Debug.logWarning("Product with productFeatureId: " + productFeatureId + " already exists.", MODULE);
//                return;
//            }
//
//            Map<String, Object> productFeatureParams = UtilMisc.toMap(
//                    "productFeatureId", productFeatureId,
//                    "productFeatureTypeId", "OTHER_FEATURE",
//                    "productFeatureCategoryId", "PACKAGE",
//                    "numberSpecified", numberSpecified,
//                    "description", "Package Info",
//                    "userLogin", userLogin
//            );
//
//            Map<String, Object> featureResult = dctx.getDispatcher().runSync("createProductFeature", productFeatureParams);
//
//            if (ServiceUtil.isSuccess(featureResult)) {
//                Debug.logInfo("Product feature created successfully with productFeatureId: " + productFeatureId, MODULE);
//            } else {
//                Debug.logError("Error creating ProductFeature: " + featureResult.get("errorMessage"), MODULE);
//            }
//        } catch (Exception e) {
//            Debug.logError("ServiceException while creating product feature: " + e.getMessage(), MODULE);
//        }
//    }
//
//    private static List<Map<String, String>> processDigitalAssets(XMLEventReader reader) throws XMLStreamException {
//        List<Map<String, String>> digitalAssets = new ArrayList<>();
//        Map<String, String> currentDigitalAsset = null;
//        boolean insideAssetDimensions = false;
//
//        while (reader.hasNext()) {
//            XMLEvent event = reader.nextEvent();
//
//            if (event.isStartElement()) {
//                String tagName = event.asStartElement().getName().getLocalPart();
//
//                switch (tagName) {
//                    case "DigitalFileInformation":
//                        currentDigitalAsset = new HashMap<>();
//                        currentDigitalAsset.put("LanguageCode", getAttributeValue(event.asStartElement(), "LanguageCode"));
//                        currentDigitalAsset.put("MaintenanceType", getAttributeValue(event.asStartElement(), "MaintenanceType"));
//                        break;
//
//                    case "FileName":
//                    case "AssetType":
//                    case "FileType":
//                    case "Representation":
//                    case "Background":
//                    case "OrientationView":
//                        currentDigitalAsset.put(tagName, getCharacterData(reader));
//                        break;
//
//                    case "AssetDimensions":
//                        insideAssetDimensions = true;
//                        currentDigitalAsset.put("AssetUOM", getAttributeValue(event.asStartElement(), "UOM"));
//                        break;
//
//                    case "AssetHeight":
//                        if (insideAssetDimensions) {
//                            currentDigitalAsset.put("AssetHeight", getCharacterData(reader));
//                        }
//                        break;
//
//                    case "AssetWidth":
//                        if (insideAssetDimensions) {
//                            currentDigitalAsset.put("AssetWidth", getCharacterData(reader));
//                        }
//                        break;
//                }
//
//            } else if (event.isEndElement()) {
//                String tagName = event.asEndElement().getName().getLocalPart();
//
//                if ("AssetDimensions".equals(tagName)) {
//                    insideAssetDimensions = false;
//                } else if ("DigitalFileInformation".equals(tagName) && currentDigitalAsset != null) {
//                    digitalAssets.add(currentDigitalAsset);
//                    currentDigitalAsset = null;
//                } else if ("DigitalAssets".equals(tagName)) {
//                    break;
//                }
//            }
//        }
//
//        return digitalAssets;
//    }
//
//}
