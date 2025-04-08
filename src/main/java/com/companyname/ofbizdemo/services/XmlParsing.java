package com.companyname.ofbizdemo.services;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.service.GenericServiceException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.entity.Delegator;
import javax.xml.stream.events.StartElement;
import org.apache.ofbiz.base.util.UtilDateTime;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.sql.Timestamp;

public class XmlParsing {
    public static final String MODULE = XmlParsing.class.getName();

    public static Map<String, Object> parseXml(DispatchContext dctx, Map<String, Object> context) {
        String filePath = (String) context.get("filePath");
        Debug.logInfo("Processing XML file: " + filePath, MODULE);
        List<Map<String, Object>> itemsList = new ArrayList<>();
        boolean insideItems = false, insideItem = false;
        Map<String, Object> currentItem = null;
        int itemCount = 0;

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (userLogin == null) {
            Debug.logError("User login is missing", MODULE);
            return ServiceUtil.returnError("User login is required.");
        }

        try (InputStream inputStream = new FileInputStream(new File(filePath))) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader reader = factory.createXMLEventReader(inputStream);

            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    String tagName = startElement.getName().getLocalPart();
                    Debug.logInfo("Processing tag: " + tagName, MODULE);
                    if ("Items".equals(tagName)) {
                        insideItems = true;
                    } else if ("Item".equals(tagName) && insideItems) {
                        insideItem = true;
                        currentItem = new HashMap<>();
                        currentItem.put("packages", new ArrayList<>());
                        currentItem.put("descriptions", new ArrayList<>());
                        currentItem.put("extendedInformation", new ArrayList<>());
                        currentItem.put("productAttributes", new ArrayList<>());
                        currentItem.put("digitalAssets", new ArrayList<>());
                        currentItem.put("partInterchangeInfo", new ArrayList<>());
                        currentItem.put("prices", new ArrayList<>());
                        Debug.logInfo("New item started", MODULE);
                    } else if (insideItem) {
                        processItemAttributes(reader, tagName, currentItem);
                    }
                } else if (event.isEndElement()) {
                    String elementName = event.asEndElement().getName().getLocalPart();
                    if ("Item".equals(elementName) && insideItem) {
                        insideItem = false;
                        //objects.toString is used to handle nullPointerException
                        String partNumber = Objects.toString(currentItem.get("PartNumber"), "");
                        String itemLevelGTIN = Objects.toString(currentItem.get("ItemLevelGTIN"), "");
                        String itemQuantitySize = Objects.toString(currentItem.get("ItemQuantitySize"), "");
                        String quantityPerApplication = Objects.toString(currentItem.get("QuantityPerApplication"), "");
//                        String minimumOrderQuantity = Objects.toString(currentItem.get("MinimumOrderQuantity"), "");
                        String brandAAIAID = Objects.toString(currentItem.get("BrandAAIAID"), "");
                        String brandLabel = Objects.toString(currentItem.get("BrandLabel"), "");
                        String partTerminologyID = Objects.toString(currentItem.get("PartTerminologyID"),"");

                        //retrive details from the currentItem and then a list of maps is created
                        List<Map<String, String>> descriptions = (List<Map<String, String>>) currentItem.get("descriptions");
                        List<Map<String, String>> extendedInformation = (List<Map<String, String>>) currentItem.get("extendedInformation");
                        List<Map<String, String>> productAttributes = (List<Map<String, String>>) currentItem.get("productAttributes");
                        List<Map<String, String>> packages = (List<Map<String, String>>) currentItem.get("packages");
                        List<Map<String, String>> prices = (List<Map<String, String>>) currentItem.get("prices");
                        List<Map<String, String>> digitalAssets = (List<Map<String, String>>) currentItem.get("digitalAssets");
                        List<Map<String, String>> partInterchangeInfo = (List<Map<String, String>>) currentItem.get("partInterchangeInfo");

                        Debug.logInfo("Final descriptions for item: " + descriptions, MODULE);
                        Debug.logInfo("Final extended information for item: " + extendedInformation, MODULE);
                        Debug.logInfo("Final product attributes for item: " + productAttributes, MODULE);
                        Debug.logInfo("Final package details for item: " + packages, MODULE);
                        Debug.logInfo("Final price details for item: " + prices, MODULE);
                        Debug.logInfo("Final digital asset details for item: " + digitalAssets, MODULE);
                        Debug.logInfo("Final part interchange details for item: " + partInterchangeInfo, MODULE);

                        Map<String, Object> createdItem = createItem(dctx, partNumber, itemQuantitySize, quantityPerApplication, partTerminologyID, itemLevelGTIN, brandAAIAID, brandLabel, descriptions, extendedInformation, productAttributes, packages, prices, digitalAssets, partInterchangeInfo, userLogin);
                        Debug.logInfo("Create Item Response: " + createdItem, MODULE);
                        itemsList.add(currentItem);
                        itemCount++;
                        Debug.logInfo("Item parsed successfully: " + currentItem, MODULE);
                    } else if ("Items".equals(elementName)) {
                        insideItems = false;
                        break;
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            Debug.logError(e, "Error parsing XML file: " + e.getMessage(), MODULE);
            return ServiceUtil.returnError("Error parsing XML file: " + e.getMessage());
        }

        Debug.logInfo("Total items found: " + itemCount, MODULE);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("itemsList", itemsList);
        result.put("itemCount", itemCount);
        return result;
    }

    private static void processItemAttributes(XMLEventReader reader, String tagName, Map<String, Object> currentItem) throws Exception {
        Debug.logInfo("Processing attribute: " + tagName, MODULE);
        //used to store values of these tags if these tags are encountered
        //list.of()--> creates an immutable list from the given elements , we can also use or symbol "||"
        if (List.of("PartNumber", "ItemLevelGTIN", "BrandAAIAID", "BrandLabel", "PartTerminologyID", "ItemQuantitySize", "QuantityPerApplication").contains(tagName)) {
            XMLEvent nextEvent = reader.nextEvent();
            if (nextEvent.isCharacters()) { //isCharacter checks if the next event is a text
                String value = nextEvent.asCharacters().getData().trim(); //extracts the actual text from XML tag
                if ("PartNumber".equals(tagName) && !currentItem.containsKey("PartNumber")) { //used if partNumber is not stored it stores or else it does not
                    currentItem.put("PartNumber", value);
                    Debug.logInfo("Stored PartNumber: " + value, MODULE);
                } else if (!"PartNumber".equals(tagName)) {
                    currentItem.put(tagName, value);
                    Debug.logInfo("Stored " + tagName + ": " + value, MODULE);
                }
            }
        } else if ("ProductAttributes".equals(tagName)) {
            //we pass XML stream reader here, gets product attribute list from the currentItem
            processProductAttribute(reader, (List<Map<String, String>>) currentItem.get("productAttributes"), currentItem);
        } else if ("Descriptions".equals(tagName)) {
            processDescription(reader, (List<Map<String, String>>) currentItem.get("descriptions"), currentItem);
        } else if ("ExtendedInformation".equals(tagName)) {
            processExtendedProductInformation(reader, (List<Map<String, String>>) currentItem.get("extendedInformation"), currentItem);
        } else if ("Packages".equals(tagName)) {
//            List<Map<String, String>> packages = new ArrayList<>();
//            processPackages(reader, packages);
            List<Map<String, String>> packages = processPackages(reader);
            if (currentItem == null) {
                currentItem = new HashMap<>();
            }
            currentItem.put("packages", packages);
        } else if ("Prices".equals(tagName)) {
            List<Map<String, String>> prices = processPrices(reader);
            if (currentItem == null) {
                currentItem = new HashMap<>();
            }
            currentItem.put("prices", prices);
    } else if ("DigitalAssets".equals(tagName)) {
            List<Map<String, String>> digitalAssets = processDigitalAssets(reader);
            if (currentItem == null) {
                currentItem = new HashMap<>();
            }
            currentItem.put("digitalAssets", digitalAssets);
        } else if ("PartInterchangeInfo".equals(tagName)) {
            List<Map<String, String>> partInterchangeList = processPartInterchangeInfo(reader, currentItem);
            if (currentItem == null) {
                currentItem = new HashMap<>();
            }
            currentItem.put("partInterchangeInfo", partInterchangeList);
        }
    }

    public static void processProductAttribute(XMLEventReader reader, List<Map<String, String>> productAttributeList, Map<String, Object> currentItem) throws Exception {
        Debug.logInfo("Processing Product Attribute", MODULE);

        if (!currentItem.containsKey("productAttributes")) {
            //if nothing is present in the list it will initialize a new list
            currentItem.put("productAttributes", new ArrayList<>());
        }

        //retrieves the existing list to ensure that all attributes are added to the same list
        List<Map<String, String>> existingProductAttributes = (List<Map<String, String>>) currentItem.get("productAttributes");

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement() && "ProductAttribute".equals(event.asStartElement().getName().getLocalPart())) {
                StartElement startElement = event.asStartElement();
                Attribute attributeIDAttr = startElement.getAttributeByName(new QName("AttributeID"));
                String attributeID = (attributeIDAttr != null) ? attributeIDAttr.getValue() : "";
                //String builder improves efficiency, since strings are immutable in java each modification will create a new string, String Builder avoids this overhead
                StringBuilder productAttributeText = new StringBuilder();

                while (reader.hasNext()) {
                    event = reader.nextEvent();
                    if (event.isCharacters()) {
                        //will add the extracted text to the end of product attribute
                        productAttributeText.append(event.asCharacters().getData().trim()).append(" ");
                    } else if (event.isEndElement() && "ProductAttribute".equals(event.asEndElement().getName().getLocalPart())) {
                        break;
                    }
                }
                String finalProductAttributeText = productAttributeText.toString().trim();
                Map<String, String> productAttributeData = new HashMap<>();
                productAttributeData.put("AttributeID", attributeID);
                productAttributeData.put("ProductAttribute", finalProductAttributeText);
                existingProductAttributes.add(productAttributeData);
                Debug.logInfo("Added product attributes: " + productAttributeData, MODULE);
            } else if (event.isEndElement() && "ProductAttributes".equals(event.asEndElement().getName().getLocalPart())) {
                break;
            }
        }
    }

    public static void processExtendedProductInformation(XMLEventReader reader, List<Map<String, String>> extendedProductInformationList, Map<String, Object> currentItem) throws Exception {
        Debug.logInfo("Processing Extended Product Information", MODULE);

        if (!currentItem.containsKey("extendedInformation")) {
            currentItem.put("extendedInformation", new ArrayList<>());
        }

        List<Map<String, String>> existingextendedInformation = (List<Map<String, String>>) currentItem.get("extendedInformation");

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement() && "ExtendedProductInformation".equals(event.asStartElement().getName().getLocalPart())) {
                StartElement startElement = event.asStartElement();
                Attribute expiCodeAttr = startElement.getAttributeByName(new QName("EXPICode"));
                String expiCode = (expiCodeAttr != null) ? expiCodeAttr.getValue() : "";
                StringBuilder extendedProductInformationText = new StringBuilder();

                while (reader.hasNext()) {
                    event = reader.nextEvent();
                    if (event.isCharacters()) {
                        extendedProductInformationText.append(event.asCharacters().getData().trim()).append(" ");
                    } else if (event.isEndElement() && "ExtendedProductInformation".equals(event.asEndElement().getName().getLocalPart())) {
                        break;
                    }
                }
                String finalExtendedProductInformationText = extendedProductInformationText.toString().trim();
                Map<String, String> extendedProductInformationData = new HashMap<>();
                extendedProductInformationData.put("EXPICode", expiCode);
                extendedProductInformationData.put("ExtendedProductInformation", finalExtendedProductInformationText);
                existingextendedInformation.add(extendedProductInformationData);
                Debug.logInfo("Added extended product information: " + extendedProductInformationData, MODULE);
            } else if (event.isEndElement() && "ExtendedInformation".equals(event.asEndElement().getName().getLocalPart())) {
                break;
            }
        }
    }

    public static void processDescription(XMLEventReader reader, List<Map<String, String>> descriptionList, Map<String, Object> currentItem) throws Exception {
        Debug.logInfo("Processing Descriptions", MODULE);

        List<Map<String, String>> existingDescriptions = (List<Map<String, String>>) currentItem.get("descriptions");

        if (existingDescriptions == null) {
            existingDescriptions = new ArrayList<>();
            currentItem.put("descriptions", existingDescriptions);
        }

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement() && "Description".equals(event.asStartElement().getName().getLocalPart())) {
                StartElement startElement = event.asStartElement();
                Attribute descriptionCodeAttr = startElement.getAttributeByName(new QName("DescriptionCode"));
                Attribute languageCodeAttr = startElement.getAttributeByName(new QName("LanguageCode"));
                String descriptionCode = (descriptionCodeAttr != null) ? descriptionCodeAttr.getValue() : "";
                String languageCode = (languageCodeAttr != null) ? languageCodeAttr.getValue() : "";
                //string builder is used to build the final description string by appending text chunks
                StringBuilder descriptionText = new StringBuilder();

                while (reader.hasNext()) {
                    event = reader.nextEvent();
                    if (event.isCharacters()) {
                        descriptionText.append(event.asCharacters().getData().trim()).append(" ");
                    } else if (event.isEndElement() && "Description".equals(event.asEndElement().getName().getLocalPart())) {
                        break;
                    }
                }
                String finalText = descriptionText.toString().trim();
                Map<String, String> descriptionData = new HashMap<>();
                descriptionData.put("DescriptionCode", descriptionCode);
                descriptionData.put("LanguageCode", languageCode);
                descriptionData.put("Description", finalText);
                existingDescriptions.add(descriptionData);
                Debug.logInfo("Added description: " + descriptionData, MODULE);
            } else if (event.isEndElement() && "Descriptions".equals(event.asEndElement().getName().getLocalPart())) {
                break;
            }
        }
        currentItem.put("descriptions", existingDescriptions);
    }

//        private static void processPackages(XMLEventReader reader, List<Map<String, String>> packagesList) throws Exception {
//        Debug.logInfo("Processing Packages", MODULE);
//        while (reader.hasNext()) {
//            XMLEvent event = reader.nextEvent();
//            if (event.isStartElement() && "Package".equals(event.asStartElement().getName().getLocalPart())) {
//                Map<String, String> packageData = new HashMap<>();
//                while (reader.hasNext()) {
//                    event = reader.nextEvent();
//                    if (event.isStartElement()) {
//                        String name = event.asStartElement().getName().getLocalPart();
//                        StringBuilder content = new StringBuilder();
//                        while (reader.hasNext()) {
//                            event = reader.nextEvent();
//                            if (event.isCharacters()) {
//                                content.append(event.asCharacters().getData().trim());
//                            } else {
//                                break;
//                            }
//                        }
//                        packageData.put(name, content.toString());
//                    } else if (event.isEndElement() && "Package".equals(event.asEndElement().getName().getLocalPart())) {
//                        break;
//                    }
//                }
//                packagesList.add(packageData);
//                Debug.logInfo("Added package: " + packageData, MODULE);
//            } else if (event.isEndElement() && "Packages".equals(event.asEndElement().getName().getLocalPart())) {
//                break;
//            }
//        }
//    }

    private static List<Map<String, String>> processPackages(XMLEventReader reader) throws XMLStreamException {
        List<Map<String, String>> packages = new ArrayList<>();
        Map<String, String> currentPackage = null;
        boolean insideDimensions = false;
        boolean insideWeights = false;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                String tagName = event.asStartElement().getName().getLocalPart();
                switch (tagName) {
                    case "Package":
                        currentPackage = new HashMap<>();
                        break;
                    case "PackageLevelGTIN":
                    case "PackageBarCodeCharacters":
                    case "PackageUOM":
                    case "QuantityofEaches":
                    case "MerchandisingHeight":
                    case "MerchandisingWidth":
                    case "MerchandisingLength":
                    case "ShippingHeight":
                    case "ShippingWidth":
                    case "ShippingLength":
                    case "Weight":
                            currentPackage.put(tagName, getCharacterData(reader));
                        break;
                    case "InnerQuantity":
                            currentPackage.put("InnerQuantity", getCharacterData(reader));
                            currentPackage.put("InnerQuantityUOM", getAttributeValue(event.asStartElement(), "InnerQuantityUOM"));
                        break;
                    case "Dimensions":
                        insideDimensions = true;
                        currentPackage.put("DimensionsUOM", getAttributeValue(event.asStartElement(), "UOM"));
                        break;
                    case "Weights":
                        insideWeights = true;
                        currentPackage.put("WeightUOM", getAttributeValue(event.asStartElement(), "UOM"));
                        break;
                }
            } else if (event.isEndElement()) {
                String tagName = event.asEndElement().getName().getLocalPart();
                if ("Dimensions".equals(tagName)) {
                    insideDimensions = false;
                } else if ("Weights".equals(tagName)) {
                    insideWeights = false;
                } else if ("Package".equals(tagName) && currentPackage != null) {
                    packages.add(currentPackage);
                    currentPackage = null;
                } else if ("Packages".equals(tagName)) {
                    break;
                }
            }
        }
        return packages;
    }

    private static List<Map<String, String>> processDigitalAssets(XMLEventReader reader) throws XMLStreamException {
        List<Map<String, String>> digitalAssets = new ArrayList<>();
        Map<String, String> currentDigitalAsset = null;
        boolean insideAssetDimensions = false;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                String tagName = event.asStartElement().getName().getLocalPart();
                switch (tagName) {
                    case "DigitalFileInformation":
                        currentDigitalAsset = new HashMap<>();
                        currentDigitalAsset.put("LanguageCode", getAttributeValue(event.asStartElement(), "LanguageCode"));
                        currentDigitalAsset.put("MaintenanceType", getAttributeValue(event.asStartElement(), "MaintenanceType"));
                        break;

                    case "FileName":
                    case "AssetType":
                    case "FileType":
                    case "Representation":
                    case "Background":
                    case "OrientationView":
                        currentDigitalAsset.put(tagName, getCharacterData(reader));
                        break;

                    case "AssetDimensions":
                        insideAssetDimensions = true;
                        currentDigitalAsset.put("AssetUOM", getAttributeValue(event.asStartElement(), "UOM"));
                        break;

                    case "AssetHeight":
                        if (insideAssetDimensions) {
                            currentDigitalAsset.put("AssetHeight", getCharacterData(reader));
                        }
                        break;

                    case "AssetWidth":
                        if (insideAssetDimensions) {
                            currentDigitalAsset.put("AssetWidth", getCharacterData(reader));
                        }
                        break;
                }

            } else if (event.isEndElement()) {
                String tagName = event.asEndElement().getName().getLocalPart();

                if ("AssetDimensions".equals(tagName)) {
                    insideAssetDimensions = false;
                } else if ("DigitalFileInformation".equals(tagName) && currentDigitalAsset != null) {
                    digitalAssets.add(currentDigitalAsset);
                    currentDigitalAsset = null;
                } else if ("DigitalAssets".equals(tagName)) {
                    break;
                }
            }
        }

        return digitalAssets;
    }

    private static List<Map<String, String>> processPrices(XMLEventReader reader) throws XMLStreamException {
        List<Map<String, String>> prices = new ArrayList<>();
        Map<String, String> currentPrice = null;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                String tagName = event.asStartElement().getName().getLocalPart();
                switch (tagName) {
                    case "Pricing":
                        currentPrice = new HashMap<>();
                        String priceType = getAttributeValue(event.asStartElement(), "PriceType");
                        currentPrice.put("PriceType", priceType);
                        break;
                    case "CurrencyCode":
                        currentPrice.put("CurrencyCode", getCharacterData(reader));
                        break;
                    case "Price":
                        currentPrice.put("Price", getCharacterData(reader));
                        break;
                }
            } else if (event.isEndElement()) {
                String tagName = event.asEndElement().getName().getLocalPart();
                if ("Pricing".equals(tagName) && currentPrice != null) {
                    prices.add(currentPrice);
                } else if ("Prices".equals(tagName)) {
                    break;
                }
            }
        }
        return prices;
    }

    private static List<Map<String, String>> processPartInterchangeInfo(XMLEventReader reader, Map<String, Object> currentItem) throws XMLStreamException {
        List<Map<String, String>> partInterchangeList = new ArrayList<>();

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                String tagName = event.asStartElement().getName().getLocalPart();

                if ("PartInterchange".equals(tagName)) {
                    Map<String, String> interchangeMap = new HashMap<>();
                    StartElement partInterchange = event.asStartElement();
                    interchangeMap.put("partBrandAAIAID", getAttributeValue(partInterchange, "BrandAAIAID"));
                    interchangeMap.put("partBrandLabel", getAttributeValue(partInterchange, "BrandLabel"));
                    interchangeMap.put("ItemEquivalentUOM", getAttributeValue(partInterchange, "ItemEquivalentUOM"));
                    interchangeMap.put("productId", (String) currentItem.get("PartNumber"));

                    while (reader.hasNext()) {
                        event = reader.nextEvent();

                        if (event.isStartElement() && "PartNumber".equals(event.asStartElement().getName().getLocalPart())) {
                            StartElement partNumberElement = event.asStartElement();
                            interchangeMap.put("InterchangeQuantity", getAttributeValue(partNumberElement, "InterchangeQuantity"));

                            String partNumberTo = getCharacterData(reader);
                            interchangeMap.put("PartNumberTo", partNumberTo);
                        } else if (event.isEndElement() && "PartInterchange".equals(event.asEndElement().getName().getLocalPart())) {
                            break;
                        }
                    }

                    partInterchangeList.add(interchangeMap);
                }

            } else if (event.isEndElement() && "PartInterchangeInfo".equals(event.asEndElement().getName().getLocalPart())) {
                break;
            }
        }
        return partInterchangeList;
    }

    private static Map<String, Object> createItem(DispatchContext dctx, String partNumber, String itemQuantitySize, String quantityPerApplication, String itemLevelGTIN, String brandAAIAID, String brandLabel, String partTerminologyID, List<Map<String, String>> descriptions, List<Map<String, String>> extendedInformation, List<Map<String, String>> productAttributes, List<Map<String, String>> packageDetails, List<Map<String, String>> priceDetail, List<Map<String, String>> digitalAssetDetail, List<Map<String, String>> partInterchangeDetail,
            GenericValue userLogin) {

        Map<String, Object> response = new HashMap<>();
        try {
            GenericValue existingProduct = EntityQuery.use(dctx.getDelegator())
                    .from("Product")
                    .where("productId", partNumber)
                    .queryOne();
            if (existingProduct != null) {
                Debug.logWarning("Product already exists: " + partNumber, MODULE);
            } else {
                Map<String, Object> productParams = UtilMisc.toMap(
                        "productId", partNumber,
                        "productTypeId", "FINISHED_GOOD",
                        "internalName", partNumber,
                        "piecesIncluded", itemQuantitySize,
                        "quantityIncluded", quantityPerApplication,
                        "orderDecimalQuantity", "1",
                        "userLogin", userLogin
                );
                Debug.logInfo("Creating new product: " + productParams, MODULE);
                Map<String, Object> result = dctx.getDispatcher().runSync("createProduct", productParams);
                if (ServiceUtil.isSuccess(result)) {
                    Debug.logInfo("Product created successfully: " + partNumber, MODULE);
                    createItemLevelGTIN(dctx, partNumber, itemLevelGTIN, userLogin);
                    createProductCategory(dctx, brandAAIAID, brandLabel, partTerminologyID, userLogin);
                    if(descriptions!=null) {
                        for (Map<String, String> desc : descriptions) {
                            String languageCode = desc.getOrDefault("LanguageCode", "");
                            String descriptionCode = desc.getOrDefault("DescriptionCode", "");
                            String finalText = desc.getOrDefault("Description", "");
                            createDescription(dctx, descriptionCode, languageCode, finalText, userLogin);
                        }
                    }
                    if(extendedInformation!=null) {
                        for (Map<String, String> extInfo : extendedInformation) {
                            String expiCode = extInfo.getOrDefault("EXPICode", "");
                            String finalExtendedProductInformationText = extInfo.getOrDefault("ExtendedProductInformation", "");
                            storeExtendedProductInformation(dctx, expiCode, finalExtendedProductInformationText, userLogin);
                        }
                    }
                    if(priceDetail!=null) {
                        for (Map<String, String> priceInfo : priceDetail) {
                            String priceType = priceInfo.getOrDefault("PriceType", "");
                            String currencyCode = priceInfo.getOrDefault("CurrencyCode", "");
                            String price = priceInfo.getOrDefault("Price", "");
                            createPrices(dctx, partNumber, priceType, price, userLogin);
                        }
                    }
                    if(digitalAssetDetail!=null) {
                        for (Map<String, String> digitalAssetInfo : digitalAssetDetail) {
                            String languageCode = digitalAssetInfo.getOrDefault("LanguageCode", "");
                            String FileName = digitalAssetInfo.getOrDefault("FileName", "");
                            String AssetType = digitalAssetInfo.getOrDefault("AssetType", "");
                            String representation = digitalAssetInfo.getOrDefault("Representation", "");
                            String background = digitalAssetInfo.getOrDefault("Background", "");
                            String orientationView = digitalAssetInfo.getOrDefault("OrientationView", "");
                            String assetHeight = digitalAssetInfo.getOrDefault("AssetHeight", "");
                            String assetWidth = digitalAssetInfo.getOrDefault("AssetWidth", "");
                            String uri = digitalAssetInfo.getOrDefault("uri", "");
                            String FileType = digitalAssetInfo.getOrDefault("FileType", "");
                            createDataResourceAttribute(dctx, AssetType, representation, background, orientationView, assetHeight, assetWidth, uri, userLogin);
                            createDigitalAssets(dctx, languageCode, FileName, FileType, userLogin);
                        }
                    }
                    if (partInterchangeDetail != null) {
                        for (Map<String, String> interchangeInfo : partInterchangeDetail) {
                            String partBrandAAIAID = interchangeInfo.getOrDefault("BrandAAIAID", "");
                            String partBrandLabel = interchangeInfo.getOrDefault("BrandLabel", "");
                            String itemEquivalentUOM = interchangeInfo.getOrDefault("ItemEquivalentUOM", "");
                            String partNumberTo = interchangeInfo.getOrDefault("PartNumberTo", "");
                            String interchangeQuantity = interchangeInfo.getOrDefault("InterchangeQuantity", "");
                            String productId = interchangeInfo.getOrDefault("productId", "");
                            createPartInterchangeInfo(dctx, partBrandAAIAID, partBrandLabel, interchangeQuantity, partNumberTo, itemEquivalentUOM,  productId, userLogin);
                            }
                    }
                    if(productAttributes!=null) {
                        for (Map<String, String> attrInfo : productAttributes) {
                            String attributeId = attrInfo.getOrDefault("AttributeID", "");
                            String finalProductAttributeText = attrInfo.getOrDefault("ProductAttribute", "");
                            storeProductAttribute(dctx, partNumber, attributeId, finalProductAttributeText, userLogin);
                        }
                    }
                    if (packageDetails!=null) {
                        for (Map<String, String> packageInfo : packageDetails) {
                            String packageLevelGTIN = packageInfo.getOrDefault("PackageLevelGTIN", "");
                            String packageBarCodeCharacters = packageInfo.getOrDefault("PackageBarCode", "");
                            String productFeatureId = packageInfo.getOrDefault("ProductFeatureId", partNumber + "_PKG");
                            Debug.logInfo("Creating package with ProductFeatureId: " + productFeatureId, MODULE);
                            createPackages(dctx, packageLevelGTIN, packageBarCodeCharacters, productFeatureId, userLogin);
                        }
                    }
                } else {
                    Debug.logError("Failed to create product: " + result.get("errorMessage"), MODULE);
                    response.put("status", "error");
                    response.put("message", "Product creation failed: " + result.get("errorMessage"));
                }
            }
        } catch (Exception e) {
            Debug.logError("Error creating product: " + e.getMessage(), MODULE);
            response.put("status", "error");
            response.put("message", "Exception: " + e.getMessage());
        }
        return response;
    }

    private static void createItemLevelGTIN(DispatchContext dctx, String partNumber, String itemLevelGTIN, GenericValue userLogin) {
        try {
            GenericValue existingGTIN = null;
            try {
                existingGTIN = EntityQuery.use(dctx.getDelegator())
                        .from("GoodIdentification")
                        .where("productId", partNumber, "goodIdentificationTypeId", "GTIN")
                        .queryFirst();
            } catch (GenericEntityException e) {
                Debug.logError("Error querying existing GTIN: " + e.getMessage(), MODULE);
                return;
            }

            if (existingGTIN != null) {
                Debug.logWarning("GTIN already exists for product: " + partNumber, MODULE);
                return;
            }
            GenericValue goodIdType = null;
            try {
                goodIdType = EntityQuery.use(dctx.getDelegator())
                        .from("GoodIdentificationType")
                        .where("goodIdentificationTypeId", "GTIN")
                        .queryOne();
            } catch (GenericEntityException e) {
                Debug.logError("Error querying GoodIdentificationType GTIN: " + e.getMessage(), MODULE);
                return;
            }
            if (goodIdType == null) {
                Map<String, Object> fields = UtilMisc.toMap(
                        "goodIdentificationTypeId", "GTIN",
                        "description", "Global Trade Item Number (GTIN)",
                        "userLogin", userLogin
                );
                Map<String, Object> result = dctx.getDispatcher().runSync("createGoodIdentificationType", fields);

                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError("Failed to create GoodIdentificationType GTIN: " + result.get("errorMessage"), MODULE);
                    return;
                }
            }
            Map<String, Object> goodIdentificationParams = UtilMisc.toMap(
                    "goodIdentificationTypeId", "GTIN",
                    "productId", partNumber,
                    "idValue", itemLevelGTIN,
                    "userLogin", userLogin
            );

            Map<String, Object> resultGoodIdentification = dctx.getDispatcher().runSync("createGoodIdentification", goodIdentificationParams);

            if (ServiceUtil.isSuccess(resultGoodIdentification)) {
                Debug.logInfo("GoodIdentification (GTIN) created for product: " + partNumber, MODULE);
            } else {
                Debug.logError("Error in createGoodIdentification: " + resultGoodIdentification.get("errorMessage"), MODULE);
            }

        } catch (GenericServiceException e) {
            Debug.logError("Exception in createItemLevelGTIN: " + e.getMessage(), MODULE);
        }
    }

    public static void createProductCategory(DispatchContext dctx, String brandAAIAID, String brandLabel, String partTerminologyID, GenericValue userLogin) {
        Delegator delegator = dctx.getDelegator();

        try {
            GenericValue brandCategoryType = EntityQuery.use(delegator)
                    .from("ProductCategoryType")
                    .where("productCategoryTypeId", "BRAND_CATEGORY")
                    .queryOne();

            if (UtilValidate.isEmpty(brandCategoryType)) {
                Map<String, Object> brandCategoryTypeParams = UtilMisc.toMap(
                        "productCategoryTypeId", "BRAND_CATEGORY",
                        "description", "Category for Brands",
                        "userLogin", userLogin
                );
                Map<String, Object> brandTypeResult = dctx.getDispatcher().runSync("createProductCategoryType", brandCategoryTypeParams);
                if (!ServiceUtil.isSuccess(brandTypeResult)) {
                    Debug.logError("Error creating BRAND_CATEGORY type: " + brandTypeResult.get("errorMessage"), MODULE);
                    return;
                }
            }
            GenericValue existingBrandCategory = EntityQuery.use(delegator)
                    .from("ProductCategory")
                    .where("categoryName", brandAAIAID,
                            "productCategoryTypeId", "BRAND_CATEGORY")
                    .queryFirst();

            if (existingBrandCategory == null) {
                String brandCategoryId = delegator.getNextSeqId("ProductCategory");

                Map<String, Object> brandCategoryParams = UtilMisc.toMap(
                        "productCategoryId", brandCategoryId,
                        "productCategoryTypeId", "BRAND_CATEGORY",
                        "categoryName", brandAAIAID,
                        "description", brandLabel,
                        "userLogin", userLogin
                );

                Map<String, Object> categoryResult = dctx.getDispatcher().runSync("createProductCategory", brandCategoryParams);
                if (ServiceUtil.isSuccess(categoryResult)) {
                    Debug.logInfo("BRAND_CATEGORY created with ID: " + brandCategoryId, MODULE);
                } else {
                    Debug.logError("Error creating BRAND_CATEGORY: " + categoryResult.get("errorMessage"), MODULE);
                }
            } else {
                Debug.logInfo("BRAND_CATEGORY already exists for: " + brandAAIAID, MODULE);
            }

            if (UtilValidate.isNotEmpty(partTerminologyID)) {
                GenericValue terminologyCategoryType = EntityQuery.use(delegator)
                        .from("ProductCategoryType")
                        .where("productCategoryTypeId", "PART_TERMINOLOGY")
                        .queryOne();

                if (UtilValidate.isEmpty(terminologyCategoryType)) {
                    Map<String, Object> terminologyTypeParams = UtilMisc.toMap(
                            "productCategoryTypeId", "PART_TERMINOLOGY",
                            "description", "Category for Part Terminology",
                            "userLogin", userLogin
                    );
                    Map<String, Object> terminologyResult = dctx.getDispatcher().runSync("createProductCategoryType", terminologyTypeParams);
                    if (!ServiceUtil.isSuccess(terminologyResult)) {
                        Debug.logError("Error creating PART_TERMINOLOGY type: " + terminologyResult.get("errorMessage"), MODULE);
                        return;
                    }
                }

                GenericValue existingTerminologyCategory = EntityQuery.use(delegator)
                        .from("ProductCategory")
                        .where("categoryName", partTerminologyID,
                                "productCategoryTypeId", "PART_TERMINOLOGY")
                        .queryFirst();

                if (existingTerminologyCategory == null) {
                    String partTerminologyCategoryId = delegator.getNextSeqId("ProductCategory");

                    Map<String, Object> partCategoryParams = UtilMisc.toMap(
                            "productCategoryId", partTerminologyCategoryId,
                            "productCategoryTypeId", "PART_TERMINOLOGY",
                            "categoryName", partTerminologyID,
                            "description", partTerminologyID,
                            "userLogin", userLogin
                    );

                    Map<String, Object> result = dctx.getDispatcher().runSync("createProductCategory", partCategoryParams);
                    if (ServiceUtil.isSuccess(result)) {
                        Debug.logInfo("PART_TERMINOLOGY Product Category created: " + partTerminologyCategoryId, MODULE);
                    } else {
                        Debug.logError("Error creating PART_TERMINOLOGY ProductCategory: " + result.get("errorMessage"), MODULE);
                    }
                } else {
                    Debug.logInfo("PART_TERMINOLOGY already exists for: " + partTerminologyID, MODULE);
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError("ServiceException while creating product category: " + e.getMessage(), MODULE);
        } catch (Exception e) {
            Debug.logError("Unexpected error while creating product category: " + e.getMessage(), MODULE);
        }
    }

    private static void createDescription(DispatchContext dctx, String descriptionCode, String languageCode, String finalText, GenericValue userLogin) {
        Delegator delegator = dctx.getDelegator();
        String contentId = delegator.getNextSeqId("Content");

        try {
            if (finalText.length() > 255) {
                Map<String, Object> electronicTextParams = UtilMisc.toMap(
                        "textData", finalText,
                        "userLogin", userLogin
                );

                Map<String, Object> electronicTextResult = dctx.getDispatcher().runSync("createElectronicText", electronicTextParams);

                if (ServiceUtil.isSuccess(electronicTextResult)) {
                    String dataResourceId = (String) electronicTextResult.get("dataResourceId");

                    Map<String, Object> contentParams = UtilMisc.toMap(
                            "contentId", contentId,
                            "contentName", descriptionCode,
                            "contentTypeId", "DOCUMENT",
                            "localeString", languageCode,
                            "description", null,
                            "dataResourceId", dataResourceId,
                            "userLogin", userLogin
                    );

                    Map<String, Object> contentResult = dctx.getDispatcher().runSync("createContent", contentParams);
                    if (ServiceUtil.isSuccess(contentResult)) {
                        Debug.logInfo("Content created successfully with contentId: " + contentId, MODULE);
                    }
                } else {
                    Debug.logError("Error creating ElectronicText: " + electronicTextResult.get("errorMessage"), MODULE);
                }
            } else {
                Map<String, Object> contentParams = UtilMisc.toMap(
                        "contentId", contentId,
                        "contentName", descriptionCode,
                        "contentTypeId", "DOCUMENT",
                        "localeString", languageCode,
                        "description", finalText,
                        "userLogin", userLogin
                );
                Map<String, Object> contentResult = dctx.getDispatcher().runSync("createContent", contentParams);
                if (ServiceUtil.isSuccess(contentResult)) {
                    Debug.logInfo("Content created successfully with contentId: " + contentId, MODULE);
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError("Error creating description: " + e.getMessage(), MODULE);
        }
    }

    public static void storeExtendedProductInformation(DispatchContext dctx, String expiCode, String finalExtendedProductInformationText, GenericValue userLogin) {

        Delegator delegator = dctx.getDelegator();
        String productFeatureId = delegator.getNextSeqId("ProductFeature");

        try {
            GenericValue productFeatureType = EntityQuery.use(delegator)
                    .from("ProductFeatureType")
                    .where("productFeatureTypeId", "EXPI")
                    .queryOne();

            if (UtilValidate.isEmpty(productFeatureType)) {
                Map<String, Object> productFeatureTypeParams = UtilMisc.toMap(
                        "productFeatureTypeId", "EXPI",
                        "userLogin", userLogin
                );

                Map<String, Object> featureTypeResult = dctx.getDispatcher().runSync("createProductFeatureType", productFeatureTypeParams);

                if (!ServiceUtil.isSuccess(featureTypeResult)) {
                    Debug.logError("Error creating ProductFeatureType: " + featureTypeResult.get("errorMessage"), MODULE);
                    return;
                }
            }

            GenericValue existingProductFeature = EntityQuery.use(delegator)
                    .from("ProductFeature")
                    .where("productFeatureId", productFeatureId)
                    .queryOne();

            if (existingProductFeature != null) {
                Debug.logWarning("Product with productFeatureId: " + productFeatureId + " already exists.", MODULE);
                return;
            }

            Map<String, Object> productFeatureParams = UtilMisc.toMap(
                    "productFeatureId", productFeatureId,
                    "productFeatureTypeId", "EXPI",
                    "idCode", expiCode,
                    "description", finalExtendedProductInformationText,
                    "userLogin", userLogin
            );

            Map<String, Object> featureResult = dctx.getDispatcher().runSync("createProductFeature", productFeatureParams);

            if (ServiceUtil.isSuccess(featureResult)) {
                Debug.logInfo("Product Feature created successfully with productFeatureId: " + productFeatureId, MODULE);
            } else {
                Debug.logError("Error creating ProductFeature: " + featureResult.get("errorMessage"), MODULE);
            }

        } catch (GenericServiceException e) {
            Debug.logError("ServiceException while creating product feature: " + e.getMessage(), MODULE);
        } catch (Exception e) {
            Debug.logError("Unexpected error while creating product feature: " + e.getMessage(), MODULE);
        }
    }

    private static void storeProductAttribute(DispatchContext dctx, String partNumber, String attributeId, String finalProductAttributeText, GenericValue userLogin) {

        try {
            GenericValue existingProductAttribute = EntityQuery.use(dctx.getDelegator())
                    .from("ProductAttribute")
                    .where("productId", partNumber, "attrName", attributeId)
                    .queryOne();

            if (existingProductAttribute != null) {
                Debug.logWarning("Product Attribute already exists", MODULE);
            } else {
                Map<String, Object> productAttributeParams = UtilMisc.toMap(
                        "productId", partNumber,
                        "attrName", attributeId,
                        "attrValue", finalProductAttributeText,
                        "attrType", "PADB_ATTRIBUTE",
                        "userLogin", userLogin
                );

                Map<String, Object> productAttributeresult = dctx.getDispatcher().runSync("createProductAttribute", productAttributeParams);
                Debug.logInfo("Product Attribute created successfully", MODULE);
            }
        } catch (Exception e) {
            Debug.logError("Error creating product attribute: " + e.getMessage(), MODULE);
        }
    }

    public static void createPackages(DispatchContext dctx, String packageLevelGTIN, String packageBarCodeCharacters, String productFeatureId, GenericValue userLogin) {
        Delegator delegator = dctx.getDelegator();

        try {
            GenericValue productFeatureCategory = EntityQuery.use(delegator)
                    .from("ProductFeatureCategory")
                    .where("productFeatureCategoryId", "PACKAGE")
                    .queryOne();

            if (productFeatureCategory == null) {
                GenericValue newCategory = delegator.makeValue("ProductFeatureCategory");
                newCategory.set("productFeatureCategoryId", "PACKAGE");
                newCategory.set("description", "This will contain information about package");
                delegator.create(newCategory);
                Debug.logInfo("ProductFeatureCategory 'PACKAGE' created successfully!", MODULE);
            }
//                Map<String, Object> params = UtilMisc.toMap(
//                        "productFeatureCategoryId", "PACKAGE",
//                        "description", "This will contain information about package",
//                        "userLogin", userLogin
//                );
//                Map<String, Object> result = dctx.getDispatcher().runSync("createProductFeatureCategory", params);
//
//                if (ServiceUtil.isSuccess(result)) {
//                    Debug.logInfo("ProductFeatureCategory 'PACKAGE' created successfully!", MODULE);
//                } else {
//                    Debug.logError("Error creating Product Feature Category: " + result.get("errorMessage"), MODULE);
//                    return;
//                }
//            }

            if (UtilValidate.isNotEmpty(packageLevelGTIN) && UtilValidate.isNotEmpty(packageBarCodeCharacters)) {
                Debug.logInfo("Creating Package Feature for GTIN: " + packageLevelGTIN, MODULE);
                createProductFeatureEntry(dctx, productFeatureId, packageLevelGTIN, userLogin);
                Debug.logInfo("Creating Package Barcode Feature: " + packageBarCodeCharacters, MODULE);
                createProductFeatureEntry(dctx, productFeatureId + "_BC", packageBarCodeCharacters, userLogin);
            }

        } catch (Exception e) {
            Debug.logError("Unexpected error while creating product feature: " + e.getMessage(), MODULE);
        }
    }

    private static void createProductFeatureEntry(DispatchContext dctx, String productFeatureId, String numberSpecified, GenericValue userLogin) {
        Delegator delegator = dctx.getDelegator();

        try {
            if (UtilValidate.isEmpty(numberSpecified)) {
                Debug.logWarning("numberSpecified is empty for ProductFeatureId: " + productFeatureId, MODULE);
                return;
            }

            GenericValue existingFeature = EntityQuery.use(delegator)
                    .from("ProductFeature")
                    .where("productFeatureId", productFeatureId)
                    .queryOne();

            if (existingFeature != null) {
                Debug.logWarning("ProductFeature already exists: " + productFeatureId + " -> Skipping creation.", MODULE);
                return;
            }

            Map<String, Object> params = UtilMisc.toMap(
                    "productFeatureId", productFeatureId,
                    "productFeatureTypeId", "OTHER_FEATURE",
                    "productFeatureCategoryId", "PACKAGE",
                    "numberSpecified", numberSpecified,
                    "description", "packageInfo",
                    "userLogin", userLogin
            );
            Map<String, Object> result = dctx.getDispatcher().runSync("createProductFeature", params);

            if (ServiceUtil.isSuccess(result)) {
                Debug.logInfo("Created ProductFeature successfully: " + productFeatureId, MODULE);
            } else {
                Debug.logError("Failed to create ProductFeature: " + result.get("errorMessage"), MODULE);
            }
        } catch (Exception e) {
            Debug.logError("Exception in createProductFeatureEntry: " + e.getMessage(), MODULE);
        }
    }

    private static void createPrices(DispatchContext dctx, String partNumber, String priceType, String price, GenericValue userLogin) {
        Delegator delegator = dctx.getDelegator();
        try {
            GenericValue productPriceType = EntityQuery.use(delegator)
                    .from("ProductPriceType")
                    .where("productPriceTypeId", priceType)
                    .queryOne();

            if (UtilValidate.isEmpty(productPriceType)) {
                Map<String, Object> productPriceTypeParams = UtilMisc.toMap(
                        "productPriceTypeId", priceType,
                        "userLogin", userLogin
                );

                Map<String, Object> priceTypeResult = dctx.getDispatcher().runSync("createProductPriceType", productPriceTypeParams);

                if (!ServiceUtil.isSuccess(priceTypeResult)) {
                    Debug.logError("Error creating ProductPriceType: " + priceTypeResult.get("errorMessage"), MODULE);
                    return;
                }
            }

            Map<String, Object> productPriceParams = UtilMisc.toMap(
                    "productId", partNumber,
                    "productPriceTypeId", priceType,
                    "productPricePurposeId", "PURCHASE",
                    "currencyUomId", "USD",
                    "productStoreGroupId", "_NA_",
                    "termUomID", "PE",
                    "fromDate", UtilDateTime.nowTimestamp(),
                    "price", price,
                    "userLogin", userLogin
            );

            Map<String, Object> priceResult = dctx.getDispatcher().runSync("createProductPrice", productPriceParams);

            if (!ServiceUtil.isSuccess(priceResult)) {
                Debug.logError("Error creating ProductPrice: " + priceResult.get("errorMessage"), MODULE);
                return;
            }

            Debug.logInfo("Product Price created successfully for PartNumber: " + partNumber, MODULE);
        } catch (Exception e) {
            Debug.logError(e, "Error creating ProductPrice", MODULE);
        }
    }

    private static void createDigitalAssets(DispatchContext dctx, String languageCode, String fileName, String fileType, GenericValue userLogin) {

        Delegator delegator = dctx.getDelegator();
        String dataResourceId = delegator.getNextSeqId("DataResource");

        try {
            Map<String, Object> dataResourceParams = UtilMisc.toMap(
                    "dataResourceId", dataResourceId,
                    "dataResourceTypeId", "IMAGE_OBJECT",
                    "dataResourceName", fileName,
                    "localeString", languageCode,
                    "mimeTypeId", fileType,
                    "userLogin", userLogin
            );

            Map<String, Object> dataResourceResult = dctx.getDispatcher().runSync("createDataResource", dataResourceParams);

            if (!ServiceUtil.isSuccess(dataResourceResult)) {
                Debug.logError("Error creating data resource: " + dataResourceResult.get("errorMessage"), MODULE);
                return;
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error creating Data Resource", MODULE);
        }
    }

    private static void createDataResourceAttribute(DispatchContext dctx, String assetType, String representation, String background, String orientationView, String assetHeight, String assetWidth, String uri, GenericValue userLogin) {

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String dataResourceId = delegator.getNextSeqId("DataResource");

        try {
            Map<String, Object> createDataResourceCtx = UtilMisc.toMap(
                    "dataResourceId", dataResourceId,
                    "dataResourceTypeId", "IMAGE_OBJECT",
                    "statusId", "CTNT_PUBLISHED",
                    "dataResourceName", "AutoCreatedDataResource",
                    "userLogin", userLogin
            );
            Map<String, Object> dataResourceResult = dispatcher.runSync("createDataResource", createDataResourceCtx);
            if (ServiceUtil.isError(dataResourceResult)) {
                Debug.logError("Error creating DataResource: " + dataResourceResult.get("errorMessage"), MODULE);
                return;
            }
            Debug.logInfo("Successfully created DataResource with ID: " + dataResourceId, MODULE);
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
                    Map<String, Object> params = UtilMisc.toMap(
                            "dataResourceId", dataResourceId,
                            "attrName", tagName,
                            "attrValue", tagValue,
                            "userLogin", userLogin
                    );
                    Map<String, Object> result = dispatcher.runSync("createDataResourceAttribute", params);
                    if (ServiceUtil.isSuccess(result)) {
                        Debug.logInfo("Created DataResourceAttribute: " + tagName + " = " + tagValue, MODULE);
                    } else {
                        Debug.logError("Error creating attribute for " + tagName + ": " + result.get("errorMessage"), MODULE);
                    }
                } else {
                    Debug.logInfo("Attribute already exists for: " + tagName + ", skipping creation.", MODULE);
                }
            }
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, "Error while creating DataResourceAttributes", MODULE);
        } catch (Exception e) {
            Debug.logError(e, "Unexpected error while creating DataResourceAttributes", MODULE);
        }
    }

    private static void createPartInterchangeInfo(DispatchContext dctx, String partBrandAAIAID, String partBrandLabel, String interchangeQuantity, String partNumberTo, String itemEquivalentUOM,  String productId, GenericValue userLogin) {
        Delegator delegator = dctx.getDelegator();

        try {

            GenericValue productCategoryType = EntityQuery.use(delegator)
                    .from("ProductCategoryType")
                    .where("productCategoryTypeId", "BRAND_CATEGORY")
                    .queryOne();

            if (UtilValidate.isEmpty(productCategoryType)) {
                Map<String, Object> productCategoryTypeParams = UtilMisc.toMap(
                        "productCategoryTypeId", "BRAND_CATEGORY",
                        "userLogin", userLogin
                );

                Map<String, Object> categoryTypeResult = dctx.getDispatcher().runSync("createProductCategoryType", productCategoryTypeParams);

                if (!ServiceUtil.isSuccess(categoryTypeResult)) {
                    Debug.logError("Error creating ProductCategoryType: " + categoryTypeResult.get("errorMessage"), MODULE);
                    return;
                }
            }

            GenericValue existingProductCategory = EntityQuery.use(delegator)
                    .from("ProductCategory")
                    .where("categoryName", partBrandAAIAID)
                    .queryFirst();

            String productCategoryId;
            if (existingProductCategory != null) {
                productCategoryId = existingProductCategory.getString("productCategoryId");
                Debug.logInfo("Product Category already exists with ID: " + productCategoryId, MODULE);
            } else {
                productCategoryId = delegator.getNextSeqId("ProductCategory");

                Map<String, Object> productCategoryParams = UtilMisc.toMap(
                        "productCategoryId", productCategoryId,
                        "productCategoryTypeId", "BRAND_CATEGORY",
                        "categoryName", partBrandAAIAID,
                        "description", partBrandLabel,
                        "userLogin", userLogin
                );

                Map<String, Object> categoryResult = dctx.getDispatcher().runSync("createProductCategory", productCategoryParams);

                if (!ServiceUtil.isSuccess(categoryResult)) {
                    Debug.logError("Error creating ProductCategory: " + categoryResult.get("errorMessage"), MODULE);
                    return;
                }
                Debug.logInfo("Product Category created successfully with productCategoryId: " + productCategoryId, MODULE);
            }

            GenericValue existingProduct = EntityQuery.use(delegator)
                    .from("Product")
                    .where("productId", partNumberTo)
                    .queryOne();

            if (existingProduct == null) {
                Map<String, Object> productParams = UtilMisc.toMap(
                        "productId", partNumberTo,
                        "productTypeId", "FINISHED_GOOD",
                        "internalName", partNumberTo,
                        "quantityUomID", itemEquivalentUOM,
                        "userLogin", userLogin
                );

                Map<String, Object> productResult = dctx.getDispatcher().runSync("createProduct", productParams);

                if (!ServiceUtil.isSuccess(productResult)) {
                    Debug.logError("Error creating Product: " + productResult.get("errorMessage"), MODULE);
                    return;
                }
                Debug.logInfo("Product created successfully with productId: " + partNumberTo, MODULE);
            } else {
                Debug.logWarning("Product already exists: " + partNumberTo, MODULE);
            }

            Map<String, Object> productAssocParams = UtilMisc.toMap(
                    "productId", productId,
                    "productIdTo", partNumberTo,
                    "productAssocTypeId", "PRODUCT_SUBSTITUTE",
                    "fromDate", UtilDateTime.nowTimestamp(),
                    "quantity", interchangeQuantity,
                    "userLogin", userLogin
            );

            Map<String, Object> productAssocResult = dctx.getDispatcher().runSync("createProductAssoc", productAssocParams);

            if (!ServiceUtil.isSuccess(productAssocResult)) {
                Debug.logError("Error creating Product Association: " + productAssocResult.get("errorMessage"), MODULE);
                return;
            }
            Debug.logInfo("Product Association created successfully " + productId + "and" + partNumberTo, MODULE);

        } catch (Exception e) {
            Debug.logError(e, "Error creating PartInterchangeInfo", MODULE);
        }
    }

    private static String getAttributeValue(StartElement element, String attributeName) {
        Attribute attr = element.getAttributeByName(new QName(attributeName));
        return attr != null ? attr.getValue() : "";
    }

    private static String getCharacterData(XMLEventReader reader) throws XMLStreamException {
        String result = "";
        XMLEvent event = reader.nextEvent();
        if (event instanceof Characters) {
            result = event.asCharacters().getData();
        }
        return result;
    }


}