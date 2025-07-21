package com.santaba.auto.lib.v4.exchange.wrapper; //Author rebeccaweaver

import static com.santaba.auto.lib.v4.exchange.Exchangev4Constants.COMMON_MODEL;
import static com.santaba.auto.lib.v4.exchange.Exchangev4Constants.DENORMALIZED_MODULE_PATH;
import static com.santaba.auto.lib.v4.exchange.Exchangev4Constants.EXCHANGE_DATA_SOURCES;
import static com.santaba.auto.lib.v4.exchange.Exchangev4Constants.EXCHANGE_STORE_LINEAGES;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.santaba.auto.lib.app.AppBase;
import com.santaba.auto.lib.app.TargetEnvConfig;
import com.santaba.auto.lib.common.CommonLibConstants;
import com.santaba.auto.lib.common.CommonLibs;
import com.santaba.auto.lib.common.CustomAssert;
import com.santaba.auto.lib.eventSource.EventSourceConstants;
import com.santaba.auto.lib.http.HttpClient;
import com.santaba.auto.lib.http.HttpResponse;
import com.santaba.auto.lib.utils.TimeUtils;
import com.santaba.auto.lib.v4.exchange.Exchangev4Constants;
import com.santaba.auto.lib.v4.exchange.Exchangev4Constants.ExchangeModuleTypes;
import com.santaba.auto.lib.v4.exchange.dto.ExchangeDevices.ExchangeDeviceAssociated;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeAccessGroupsId;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeDenormalizationV4AllIds;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeDenormalizationV4Data;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeDenormalizationV4Request;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeDenormalizationV4Response;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeItemV4;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeV4FilterDataResponse;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeV4FilterDynamicExpressionsRequest;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeV4FilterDynamicRequest;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeV4FilterFiltersRequest;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeV4FilterMetaRequest;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeV4FilterPagingRequest;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeV4FilterRequest;
import com.santaba.auto.lib.v4.exchange.dto.common.ExchangeV4FilterResponse;
import com.santaba.auto.lib.v4.exchange.dto.common.ImportExportFile;
import com.santaba.auto.lib.v4.exchange.dto.common.LogicModulesV4AllIds;
import com.santaba.auto.lib.v4.exchange.dto.store.*;
import com.santaba.auto.lib.v4.exchange.dto.store.tags.response.AllTagsResponse;
import com.santaba.auto.lib.v4.exchange.dto.toolbox.DeleteModulesRequest;
import com.santaba.auto.lib.v4.exchange.dto.toolbox.DeleteModulesRequestData;
import com.santaba.auto.lib.v4.exchange.dto.toolbox.DeleteModulesResponse;
import com.santaba.auto.lib.v4.exchange.dto.toolbox.repodiff.request.RepoDiffRequest;
import com.santaba.auto.lib.v4.exchange.dto.toolbox.repodiff.response.Item;
import com.santaba.auto.lib.v4.exchange.dto.toolbox.repodiff.response.RepoDiffResponse;
import com.santaba.auto.lib.v4.logicmodules.snmpSysOIDMaps.dto.ExchangeSNMPSysOIDMapsV4;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.testng.Assert;

public class ExchangeV4Lib extends AppBase {

    private static Logger logger = LogManager.getLogger(ExchangeV4Lib.class);
    HttpClient httpPortalClientV4;
    HttpClient httpPortalClientV4Denormalized;
    String responseFields = "id,model,appliesToScript,installationStatuses,status,isPrivate,name,description,type,authorPortalName,tags,version,updatedAtMS";


    public ExchangeV4Lib() {
        httpPortalClientV4 = AppBase.httpPortalClientV4;
        httpPortalClientV4Denormalized = AppBase.httpPortalClientV4;
    }

    public ExchangeV4Lib(HttpClient httpClient) {
        httpPortalClientV4 = httpClient;
        httpPortalClientV4Denormalized = httpClient;
    }

    public void setHttpPortalClientV4(HttpClient httpClient) {
        httpPortalClientV4Denormalized = httpClient;
        httpPortalClientV4 = httpClient;
    }

    public void setHttpPortalClientV4Denormalized(HttpClient httpClient) {
        httpPortalClientV4Denormalized = httpClient;
    }

    /**
     * Sends a request to the Exchange toolbox with the provided filter, returns entire
     * HTTPResponse
     *
     * @param filter ExchangeV4FilterRequest
     * @return httpResponse the HttpResponse pojo object
     */
    public HttpResponse getFilteredExchangeToolboxHttpResponse(
        ExchangeV4FilterRequest filter) {
        HttpResponse httpResponse = httpPortalClientV4
            .doPost(Exchangev4Constants.EXCHANGE_TOOLBOX_FILTER, filter,
                ExchangeV4FilterResponse.class);
        CommonLibs.assert200HttpStatus(httpResponse);
        return httpResponse;
    }

    /**
     * Sends a request to the Exchange toolbox with the provided filter, returns entire responseBody
     * after verifying 200 http status
     *
     * @param filter ExchangeV4FilterRequest
     * @return responseBody the ExchangeV4FilterResponse pojo object
     */
    public ExchangeV4FilterResponse getFilteredExchangeToolbox(
        ExchangeV4FilterRequest filter) {
        HttpResponse httpResponse = getFilteredExchangeToolboxHttpResponse(filter);
        ExchangeV4FilterResponse responseBody = (ExchangeV4FilterResponse) httpResponse.getObject();
        return responseBody;
    }


    /**
     * Helps format the filter request for either the Exchange Toolbox or Exchange Module Store.
     * This applies to the filter type "FILTER_ALL".  Please see
     * https://confluence.logicmonitor.com/pages/viewpage.action?spaceKey=ENG&title=FILTER_ALL for
     * format and other context details to this filter type.  Please visit exchange and toolbox
     * pages for their specific endpoint details
     *
     * Set either quick or dynamic as null to use one or the other, current implementation does not
     * allow including either field as empty if the other is being used.
     *
     * @param perPageCount - the amount of modules returned per paginated page
     * @param pageOffsetCount - the page where the list of items should start
     * @param sort - a comma delimited "list" of fields to sort by (accepted as String)
     * @param quick - value to search all fields by
     * @param dynamicRequest - List of formatted dynamic blocks with specific fields and filter
     * params
     * @return filterRequest the ExchangeV4FilterResponse pojo object
     */
    public ExchangeV4FilterRequest createExchangeV4FilterRequest(int perPageCount,
        int pageOffsetCount, String sort, String quick,
        List<ExchangeV4FilterDynamicRequest> dynamicRequest) {
        List<String> quickPattern = getQuickPattern(quick);
        ExchangeV4FilterMetaRequest metaRequest = getExchangeV4FilterMetaRequest(
            perPageCount, pageOffsetCount, sort, dynamicRequest, quickPattern);

        ExchangeV4FilterRequest filterRequest = ExchangeV4FilterRequest.builder()
            .meta(metaRequest)
            .build();
        List<Map<String, String>> columnRequests = getColumnRequests();
        filterRequest.getMeta().setColumns(columnRequests);

        return filterRequest;
    }

    /**
     * This method create FilterRequest for the packages
     *
     * @param perPageCount per page count
     * @param pageOffsetCount page offset to start count from
     * @param sort by which field we want to sort
     * @param dynamicRequest dynamic request
     * @return ExchangeV4FilterRequest
     */
    public ExchangeV4FilterRequest createExchangeV4FilterRequestForPackage(int perPageCount,
        int pageOffsetCount, String sort, String quick,
        List<ExchangeV4FilterDynamicRequest> dynamicRequest) {
        List<String> quickPattern = getQuickPattern(quick);
        ExchangeV4FilterMetaRequest metaRequest = getExchangeV4FilterMetaRequest(
            perPageCount, pageOffsetCount, sort, dynamicRequest, quickPattern);

        ExchangeV4FilterRequest filterRequest = ExchangeV4FilterRequest.builder()
            .meta(metaRequest)
            .build();
        List<Map<String, String>> columnRequests = getColumnRequestsForPackage();
        filterRequest.getMeta().setColumns(columnRequests);

        return filterRequest;
    }

    /**
     * This method is used to get colounm list for all logicmodule in filters
     *
     * @return List<Map < String, String>>
     */
    private List<Map<String, String>> getColumnRequests() {
        List<Map<String, String>> columnRequests = new ArrayList<>();
        Map<String, String> column = null;
        for (ExchangeModuleTypes moduleType : ExchangeModuleTypes.values()) {
            column = new HashMap<>();
            if (moduleType.compareTo(ExchangeModuleTypes.exchangeSNMPSysOIDMaps) != 0) {
                column.put(moduleType.toString(), responseFields);
                columnRequests.add(column);
            }
        }
        return columnRequests;
    }

    /**
     * This method is used to get colounm list for all logicmodule in filters
     *
     * @return List<Map < String, String>>
     */
    private List<Map<String, String>> getColumnRequestsForPackage() {
        List<Map<String, String>> columnRequests = new ArrayList<>();
        Map<String, String> column = null;
        for (ExchangeModuleTypes moduleType : ExchangeModuleTypes.values()) {
            column = new HashMap<>();
            if (moduleType.compareTo(ExchangeModuleTypes.exchangeSNMPSysOIDMaps) != 0) {
                column.put(moduleType.toString(), responseFields);
                columnRequests.add(column);
            }
        }
        column.clear();
        column.put("exchangeLogicModulePackages", responseFields);
        columnRequests.add(column);
        return columnRequests;
    }

    /**
     * This method create the meta data request of ExchangeV4FilterRequest
     *
     * @param perPageCount per page count
     * @param pageOffsetCount page offset to start count from
     * @param sort by which field we want to sort
     * @param dynamicRequest dynamic request
     * @param quickPattern patter to search
     * @return ExchangeV4FilterMetaRequest
     */
    private ExchangeV4FilterMetaRequest getExchangeV4FilterMetaRequest(int perPageCount,
        int pageOffsetCount, String sort, List<ExchangeV4FilterDynamicRequest> dynamicRequest,
        List<String> quickPattern) {
        ExchangeV4FilterFiltersRequest filtersRequest = ExchangeV4FilterFiltersRequest.builder()
            .dynamic(dynamicRequest)
            .quick(quickPattern)
            .filterType("FILTER_ALL")
            .build();

        ExchangeV4FilterPagingRequest pagingRequest = ExchangeV4FilterPagingRequest.builder()
            .pageOffsetCount(pageOffsetCount)
            .perPageCount(perPageCount)
            .build();

        return ExchangeV4FilterMetaRequest.builder()
            .paging(pagingRequest)
            .filters(filtersRequest)
            .sort(sort)
            .build();
    }

    private List<String> getQuickPattern(String quick) {
        List<String> quickPattern;
        if (quick != null && quick != "") {
            quickPattern = List.of(quick);
        } else {
            quickPattern = List.of();
        }
        return quickPattern;
    }


    /**
     * Creates a package with the provided package request Pojo, returns entire HTTPResponse
     *
     * @param packageRequest PackageV4Request pojo
     * @return httpResponse the HttpResponse pojo object
     */
    public HttpResponse createPackageByRequestHttpResponse(
        PackageV4Request packageRequest) {
        HttpResponse httpResponse = httpPortalClientV4
            .doPost(Exchangev4Constants.EXCHANGE_STORE_PACKAGE, packageRequest,
                ExchangeV4FilterResponse.class);
        return httpResponse;
    }

    /**
     * Creates a package with the provided package request Pojo, returns entire responseBody after
     * verifying 200 http status
     *
     * @param packageRequest PackageV4Request pojo
     * @return responseBody the PackageV4Response pojo object
     */
    public ExchangeV4FilterResponse createPackageByRequest(
        PackageV4Request packageRequest) {
        HttpResponse httpResponse = createPackageByRequestHttpResponse(packageRequest);
        CommonLibs.assert200HttpStatus(httpResponse);
        ExchangeV4FilterResponse response = (ExchangeV4FilterResponse) httpResponse.getObject();
        return response;
    }

    /**
     * Creates a package with the provided package details, returns entire responseBody after
     * verifying 200 http status
     *
     * @param name package name
     * @param summary package summary
     * @param description package description
     * @param logo package logo
     * @param lineageIds list of lineage Ids to include
     * @return responseBody the ExchangeV4FilterResponse pojo object
     */
    public ExchangeV4FilterResponse createPackageWithLogo(String name, String summary,
        String description,
        String logo, List<String> lineageIds) {
        PackageItemsV4Request packageItem = PackageItemsV4Request.builder()
            .model("exchangeLogicModulePackages")
            .name(name)
            .summary(summary)
            .description(description)
            .logo(logo)
            .lineageIds(lineageIds)
            .build();
        List<PackageItemsV4Request> packageItems = new ArrayList<>();
        packageItems.add(packageItem);
        PackageDataV4Request packageDataV4Request = PackageDataV4Request.builder()
            .items(packageItems)
            .build();
        PackageV4Request packageV4Request = PackageV4Request.builder()
            .data(packageDataV4Request)
            .build();

        ExchangeV4FilterResponse response = createPackageByRequest(packageV4Request);
        return response;
    }

    /**
     * Creates a package with the provided package details with default no logo, returns entire
     * responseBody after verifying 200 http status
     *
     * @param name package name
     * @param summary package summary
     * @param description package description
     * @param lineageIds list of lineage Ids to include
     * @return responseBody the ExchangeV4FilterResponse pojo object
     */
    public ExchangeV4FilterResponse createPackage(String name, String summary, String description,
        List<String> lineageIds) {
        ExchangeV4FilterResponse response = createPackageWithLogo(name, summary, description, "",
            lineageIds);
        return response;
    }

    /**
     * Sends a request to the Exchange Store with the provided filter, returns entire HTTPResponse
     *
     * @param filter ExchangeV4FilterRequest
     * @return httpResponse the HttpResponse pojo object
     */
    public HttpResponse getFilteredExchangeStoreHttpResponse(
        ExchangeV4FilterRequest filter) {
        HttpResponse httpResponse = httpPortalClientV4
            .doPost(Exchangev4Constants.EXCHANGE_STORE_FILTER, filter,
                ExchangeV4FilterResponse.class);
        CommonLibs.assert200HttpStatus(httpResponse);
        return httpResponse;
    }

    /**
     * Sends a request to the Exchange store with the provided filter, returns entire responseBody
     * after verifying 200 http status
     *
     * @param filter ExchangeV4FilterRequest
     * @return responseBody the ExchangeV4FilterResponse pojo object
     */
    public ExchangeV4FilterResponse getFilteredExchangeStore(
        ExchangeV4FilterRequest filter) {
        HttpResponse httpResponse = getFilteredExchangeStoreHttpResponse((filter));
        CommonLibs.assert200HttpStatus(httpResponse);
        ExchangeV4FilterResponse responseBody = (ExchangeV4FilterResponse) httpResponse.getObject();
        return responseBody;
    }

    /**
     * Sends a request to the Exchange store with the provided filter, returns entire responseBody
     * after verifying 200 http status
     *
     * @param filter ExchangeV4FilterRequest
     * @return responseBody the ExchangeV4FilterResponse pojo object
     */
    public ExchangeV4FilterResponse getFilteredExchangeStoreWithoutAssert(
        ExchangeV4FilterRequest filter) {
        try {
            HttpResponse httpResponse = getFilteredExchangeStoreHttpResponse((filter));
            ExchangeV4FilterResponse responseBody = (ExchangeV4FilterResponse) httpResponse.getObject();
            return responseBody;
        } catch (Exception e) {
            logger.error("Failed to get DataSource from exchange", e);
            return null;
        }

    }

    /**
     * This method is to build the Dynamic filter List with type logicModule and name of Module
     *
     * @param firstField First field for filtering
     * @param secondField Second field for filtering
     * @param moduleType module type
     * @param logicModuleName logic Module Name
     * @return List<ExchangeV4FilterDynamicRequest> List have Dynamic filter
     */
    public List<ExchangeV4FilterDynamicRequest> buildV4DynamicFilterList(String firstField,
        String moduleType, String secondField,
        String logicModuleName) {
        List<ExchangeV4FilterDynamicRequest> dynamicRequestList = new ArrayList<>();

        //building module type expression block
        ExchangeV4FilterDynamicRequest moduleTypeDynamicBlock = getExchangeV4FilterDynamicRequest(
            firstField, moduleType, "EQ");

        //building module name block
        ExchangeV4FilterDynamicRequest nameDynamicBlock = getExchangeV4FilterDynamicRequest(
            secondField, logicModuleName, "EQ");

        dynamicRequestList.add(moduleTypeDynamicBlock);
        dynamicRequestList.add(nameDynamicBlock);

        return dynamicRequestList;

    }

    public ExchangeV4FilterDynamicRequest getExchangeV4FilterDynamicRequest(
        String field, String fieldValue, String operator) {
        List<ExchangeV4FilterDynamicExpressionsRequest> nameExpressionList = new ArrayList<>();
        ExchangeV4FilterDynamicExpressionsRequest nameDynamicExpression = getExchangeV4FilterDynamicExpressionsRequest(
            operator, fieldValue);
        nameExpressionList.add(nameDynamicExpression);
        return ExchangeV4FilterDynamicRequest
            .builder()
            .field(field)
            .expressions(nameExpressionList)
            .build();
    }

    private static ExchangeV4FilterDynamicExpressionsRequest getExchangeV4FilterDynamicExpressionsRequest(
        String operator, String fieldValue) {
        return ExchangeV4FilterDynamicExpressionsRequest
            .builder()
            .operator(operator)
            .value(fieldValue)
            .build();
    }

    /**
     * This method is to build the Dynamic filter List with type logicModule
     *
     * @param field fields for filtering
     * @param moduleType module type
     * @return List<ExchangeV4FilterDynamicRequest> List have Dynamic filter
     */
    public List<ExchangeV4FilterDynamicRequest> buildV4DynamicFilterList(String field,
        String moduleType) {
        List<ExchangeV4FilterDynamicRequest> dynamicRequestList = new ArrayList<>();

        //building module type expression block
        ExchangeV4FilterDynamicRequest moduleTypeDynamicBlock = getExchangeV4FilterDynamicRequest(
            field, String.valueOf(moduleType), "EQ");
        dynamicRequestList.add(moduleTypeDynamicBlock);
        return dynamicRequestList;
    }

    /**
     * This method is to build the Dynamic filter List with type logicModule
     *
     * @param field fields for filtering
     * @param moduleTypes module types
     * @return List<ExchangeV4FilterDynamicRequest> List have Dynamic filter
     */
    public List<ExchangeV4FilterDynamicRequest> buildV4DynamicFilterList(String field,
        List<String> moduleTypes) {
        List<ExchangeV4FilterDynamicRequest> dynamicRequestList = new ArrayList<>();

        //building module type expression block
        List<ExchangeV4FilterDynamicExpressionsRequest> moduleTypeExpressionList = new ArrayList<>();
        for (String moduleType : moduleTypes) {
            ExchangeV4FilterDynamicExpressionsRequest moduleTypeDynamicExpression = getExchangeV4FilterDynamicExpressionsRequest(
                "EQ", String.valueOf(moduleType));
            moduleTypeExpressionList.add(moduleTypeDynamicExpression);
        }
        ExchangeV4FilterDynamicRequest moduleTypeDynamicBlock = ExchangeV4FilterDynamicRequest
            .builder()
            .field(field)
            .expressions(moduleTypeExpressionList)
            .build();
        dynamicRequestList.add(moduleTypeDynamicBlock);
        return dynamicRequestList;
    }

    /**
     * Method to verify basic checks on filtered LogicModules.
     *
     * @param filterResponse - ExchangeV4FilterResponse response of the filtered logicmodules
     */
    public void verifyCommonLogicModulesFilterChecks(ExchangeV4FilterResponse filterResponse,
        int perPageCount, int pageOffset) {
        /*
        Verify:
            at least one logicmodule is returned
            per page count is set to what you entered in your filter
            page offset set to what was set in the filter
            the filteredCount does not exceed the totalCount
         */

        CustomAssert.assertNotEqual(filterResponse.getMeta().getFilteredCount(), 0,
            "at least one module was returned from the filter");
        CustomAssert.assertEqual(filterResponse.getMeta().getPerPageCount(), perPageCount,
            "The page size count is what was set in the filter");
        CustomAssert.assertEqual(filterResponse.getMeta().getPageOffsetCount(), pageOffset,
            "The page offset count is what was set in the filter");
        logger.info(
            "Total count of logicModule in response :" + filterResponse.getMeta().getTotalCount());
        logger.info(
            "Filter count of logicModule in response :" + filterResponse.getMeta().getTotalCount());
        boolean compareFilterToTotal =
            filterResponse.getMeta().getTotalCount() >= filterResponse.getMeta().getFilteredCount();
        CustomAssert.assertTrue(compareFilterToTotal,
            "The total count is greater or equal to the filtered count");
    }

    /**
     * verifyFilteredLogicModulesDetailChecks() - Method to verify filtered response attributes are
     * not empty and contain values expected before verifying data
     *
     * @param filterResponse - ExchangeV4FilterResponse
     */
    public void verifyFilteredLogicModulesDetailChecks(
        ExchangeV4FilterResponse filterResponse, String model) {

        CustomAssert.assertNotNull(filterResponse.getData(),
            "Filter response - Data attribute is not null");
        CustomAssert.assertNotNull(filterResponse.getData().getById(),
            "Filter response - ById attribute is not null");
        CustomAssert
            .assertTrue(filterResponse.getData().getById().containsKey(model),
                "Model " + model + " found in filter response");
    }

    /**
     * This method is to verify the Quick filter response of both tool box and store
     *
     * @param filterResponse ExchangeV4FilterResponse pojo object
     * @param model Model of logic Module
     * @param quickSearch text which we are provided in quick search
     */
    public void verifyExchangeQuickFilterResponseFields(ExchangeV4FilterResponse filterResponse,
        String model,
        String quickSearch) {
        Map<String, ExchangeItemV4> allExchangeItemMap = filterResponse
            .getData()
            .getById().get(model);
        Set<String> logicModuleIdSet = allExchangeItemMap.keySet();
        for (String logicModuleId : logicModuleIdSet) {
            ExchangeItemV4 exchangeItemV4 = allExchangeItemMap
                .get(logicModuleId);
            logger.info("LogicModule  name : " + exchangeItemV4.getName());
            logger.info("LogicModule description : " + exchangeItemV4.getDescription());
            if (exchangeItemV4.getDescription() != null) {
                CustomAssert.assertTrue(
                    (exchangeItemV4.getName().toLowerCase().contains(quickSearch) || (exchangeItemV4
                        .getDescription().toLowerCase()
                        .contains(quickSearch))),
                    "Verifying the name or description contains searched text :");
            } else {
                CustomAssert.assertTrue(
                    (exchangeItemV4.getName().toLowerCase().contains(quickSearch)),
                    "Verifying the name contains searched text :");
            }
        }
    }

    /**
     * This method is to verify the SNMP OIds Quick filter response of both tool box and store
     *
     * @param filterResponse ExchangeV4FilterResponse pojo object * @param model Model of logic
     * Module
     * @param quickSearch text which we are provided in quick search
     */
    public void verifyExchangeSNMPOIdsQuickFilter(ExchangeV4FilterResponse filterResponse,
        String model,
        String quickSearch) {
        Map<String, ExchangeItemV4> allOIDMap = filterResponse
            .getData()
            .getById().get(model);
        boolean oidStatus = false;
        boolean categoryStatus = false;
        Set<String> logicModuleIdSet = allOIDMap.keySet();
        for (String logicModuleId : logicModuleIdSet) {
            ExchangeSNMPSysOIDMapsV4 snmpSysOIDMapsV4 = (ExchangeSNMPSysOIDMapsV4) allOIDMap
                .get(logicModuleId);
            logger.info("SNMP OIds  OId : " + snmpSysOIDMapsV4.getOid());
            logger.info("SNMP OIds categories : " + snmpSysOIDMapsV4.getCategories());
            if (snmpSysOIDMapsV4.getCategories() != null && snmpSysOIDMapsV4.getOid() != null) {
                if (snmpSysOIDMapsV4.getOid().toLowerCase().contains(quickSearch)) {
                    oidStatus = true;
                    break;
                }

                for (String category : snmpSysOIDMapsV4.getCategories()) {
                    if (category.toLowerCase().contains(quickSearch)) {
                        categoryStatus = true;
                        break;
                    }
                }
            }
            CustomAssert.assertTrue(oidStatus || categoryStatus,
                "Verifying the Response Contains " + quickSearch + " in response :");
        }
    }


    /**
     * Sends a request to the store skip update endpoint, returns entire HTTPResponse
     *
     * @param request SkipUpdateRequest
     * @return httpResponse the HttpResponse pojo object
     */
    public HttpResponse skipModulesHttpResponse(
        SkipUpdateRequest request) {
        HttpResponse httpResponse = httpPortalClientV4
            .doPost(Exchangev4Constants.EXCHANGE_STORE_SKIP_UPDATE, request,
                SkipUpdateResponse.class);
        return httpResponse;
    }

    /**
     * Sends a request to the store skip update endpoint, returns entire responseBody after
     * verifying 200 http status
     *
     * @param request SkipUpdateRequest
     * @return responseBody the SkipUpdateResponse pojo object
     */
    public SkipUpdateResponse skipModulesResponse(
        SkipUpdateRequest request) {
        HttpResponse httpResponse = skipModulesHttpResponse(request);
        CommonLibs.assert200HttpStatus(httpResponse);
        SkipUpdateResponse responseBody = (SkipUpdateResponse) httpResponse.getObject();
        return responseBody;
    }

    /**
     * Given a list of Exchange Ids and a string skip message, sends a request to the store skip
     * update endpoint, returns entire responseBody after verifying 200 http status
     *
     * @param idList String list of Ids to skip
     * @param message String skip message
     * @return responseBody the SkipUpdateResponse pojo object
     */
    public SkipUpdateResponse skipModulesByIds(
        List<String> idList, String message) {

        // add skip reason to meta block
        SkipUpdateRequestMeta skipUpdateRequestMeta = SkipUpdateRequestMeta.builder()
            .skipReason(message).build();

        //Create AllIds List
        List<ModuleStoreBulkActionV4RequestAllId> skipUpdateRequestAllIds = new ArrayList<>();
        for (String id : idList) {
            ModuleStoreBulkActionV4RequestAllId skipUpdateRequestAllId = ModuleStoreBulkActionV4RequestAllId.builder()
                .model("exchangeLogicModules").id(id).build();
            skipUpdateRequestAllIds.add(skipUpdateRequestAllId);
        }

        //Create Data block
        ModuleStoreBulkActionV4RequestData skipUpdateRequestData = ModuleStoreBulkActionV4RequestData.builder()
            .allIds(skipUpdateRequestAllIds).build();
        //Create request
        SkipUpdateRequest skipUpdateRequest = SkipUpdateRequest.builder()
            .data(skipUpdateRequestData).meta(skipUpdateRequestMeta).build();

        HttpResponse httpResponse = skipModulesHttpResponse(skipUpdateRequest);
        CommonLibs.assert200HttpStatus(httpResponse);
        SkipUpdateResponse responseBody = (SkipUpdateResponse) httpResponse.getObject();
        return responseBody;
    }

    /**
     * Sends a request to the store import endpoint, returns entire HTTPResponse
     *
     * @param request Import Modules Request
     * @return httpResponse the HttpResponse pojo object
     */
    public HttpResponse importModulesHttpResponse(
        ImportModulesV4Request request) {
        HttpResponse httpResponse = httpPortalClientV4
            .doPost(Exchangev4Constants.EXCHANGE_STORE_IMPORT, request,
                ImportModulesV4Response.class);
        return httpResponse;
    }

    /**
     * Sends a request to the store import endpoint with a list of registry ids and fields to
     * preserve. Verifies successful response and returns Import Response
     *
     * @param idList list of ids to import
     * @param handleConflict Options ERROR or FORCE_OVERWRITE
     * @param fieldsToPreserve list of fields to preserve during import
     * @return response the ImportModulesV4Response pojo object
     */
    public ImportModulesV4Response importModulesByIdsWithPreserveFields(
        List<String> idList, String handleConflict, List<String> fieldsToPreserve) {

        //Create AllIds List
        List<LogicModulesV4AllIds> allIds = new ArrayList<>();
        for (String id : idList) {
            LogicModulesV4AllIds idBlock = LogicModulesV4AllIds.builder()
                .model("exchangeLogicModules").id(id).build();
            allIds.add(idBlock);
        }

        //Create Data Block
        ImportModulesV4RequestData dataRequest = ImportModulesV4RequestData.builder().allIds(allIds)
            .build();
        //Create Meta Block
        ImportModulesV4RequestMeta metaRequest = ImportModulesV4RequestMeta.builder()
            .fieldsToPreserve(fieldsToPreserve).handleConflict(handleConflict).build();

        //Create Request
        ImportModulesV4Request request = ImportModulesV4Request.builder().data(dataRequest)
            .meta(metaRequest).build();

        HttpResponse httpResponse = importModulesHttpResponse(request);
        CommonLibs.assert200HttpStatus(httpResponse);
        ImportModulesV4Response response = (ImportModulesV4Response) httpResponse.getObject();
        return response;
    }

    /**
     * Imports a list of modules by registry id, handle conflict = ERROR (default for basic module
     * import) and preserves no fields
     *
     * @param idList list of modules to import
     * @return response the ImportModulesV4Response pojo object
     */
    public ImportModulesV4Response importModulesByIds(
        List<String> idList) {
        List<String> preserveFieldsList = new ArrayList<>();
        ImportModulesV4Response response = importModulesByIdsWithPreserveFields(idList, "ERROR",
            preserveFieldsList);
        return response;
    }

    /**
     * Imports a single module by registry id, handle conflict = ERROR (default for basic module
     * import) and preserves no fields.  Verifies the module was imported and returns the Data >
     * ById > LocalId Data Block.  Will fail if error occurs
     *
     * @param registryId moduleId to import
     * @return response the ImportModulesV4Response pojo object
     */
    public ExchangeItemV4 importModuleById(String
        registryId) {
        List<String> idList = new ArrayList<>();
        idList.add(registryId);
        ImportModulesV4Response response = importModulesByIds(idList);
        Assert.assertNotNull(response.getData(), "The data block is empty, something went wrong");
        Assert.assertEquals(response.getData().getAllIds().size(), 1,
            "Expected one module to be returned");
        String model = response.getData().getAllIds().get(0).getModel();
        String id = response.getData().getAllIds().get(0).getId();
        ExchangeItemV4 responseItem = response.getData().getById().get(model).get(id);
        return responseItem;
    }

    /**
     * Sends a DELETE request to the logicModule endpoint and returns the response as an
     * HttpResponse
     *
     * @param request DeleteModulesRequest
     * @return The response from the API
     */
    public HttpResponse deleteModulesHttpResponse(DeleteModulesRequest request) {
        return httpPortalClientV4
            .doDelete(Exchangev4Constants.EXCHANGE_TOOLBOX_LOGICMODULES, request,
                DeleteModulesResponse.class);
    }

    /**
     * Sends a DELETE request to the logicModule endpoint, returns entire responseBody after
     * verifying 200 http status
     *
     * @param request DeleteModulesRequest
     * @return responseBody the DeleteModulesResponse pojo object
     */
    public DeleteModulesResponse deleteModulesResponse(
        DeleteModulesRequest request) {
        HttpResponse httpResponse = deleteModulesHttpResponse(request);
        CommonLibs.assert200HttpStatus(httpResponse);
        DeleteModulesResponse responseBody = (DeleteModulesResponse) httpResponse.getObject();
        return responseBody;
    }

    /**
     * This Method deletes the logicModules*
     *
     * @param id id of the LogicModule
     * @param exchangeModuleTypes type of LogicModule
     * @return DeleteModulesResponse
     */
    public DeleteModulesResponse deleteLogicModule(String id,
        ExchangeModuleTypes exchangeModuleTypes) {
        return deleteModulesResponse(createLogicModuleV4DeleteRequest(id, exchangeModuleTypes));
    }

    /**
     * This method creates DeleteRequest for delete LogicModule*
     *
     * @param id Id of LogicModule
     * @param moduleTypes module type of request
     * @return DeleteModulesRequest
     */
    public DeleteModulesRequest createLogicModuleV4DeleteRequest(String id,
        ExchangeModuleTypes moduleTypes) {
        DeleteModulesRequest dataSourceV4DeleteRequest = new DeleteModulesRequest();
        DeleteModulesRequestData dataSourceV4DeleteData = new DeleteModulesRequestData();
        LogicModulesV4AllIds dataSourceV4DeleteAllId = new LogicModulesV4AllIds();
        dataSourceV4DeleteAllId.setId(id);
        dataSourceV4DeleteAllId.setModel(moduleTypes.name());
        dataSourceV4DeleteData.setAllIds(List.of(dataSourceV4DeleteAllId));
        dataSourceV4DeleteRequest.setData(dataSourceV4DeleteData);
        return dataSourceV4DeleteRequest;
    }

    /**
     * Given a list of Module Ids and a string Exchange module type, sends a request to the toolbox
     * Delete Module endpoint, returns entire responseBody after verifying 200 http status
     *
     * @param idList String list of Ids to delete
     * @param modelList String list of module types to delete
     * @return responseBody the SkipUpdateResponse pojo object
     */
    public DeleteModulesResponse deleteModulesByIds(
        List<String> idList, List<String> modelList) {

        //Create AllIds List
        List<LogicModulesV4AllIds> logicModulesV4AllIdsList = new ArrayList<>();
        for (int i = 0; i < idList.size(); i++) {
            LogicModulesV4AllIds logicModulesV4AllIds = LogicModulesV4AllIds.builder()
                .model(modelList.get(i)).id(idList.get(i)).build();
            logicModulesV4AllIdsList.add(logicModulesV4AllIds);
        }

        //Create Data block
        DeleteModulesRequestData deleteModulesRequestData = DeleteModulesRequestData.builder()
            .allIds(logicModulesV4AllIdsList).build();
        //Create request
        DeleteModulesRequest deleteModulesRequest = DeleteModulesRequest.builder()
            .data(deleteModulesRequestData).build();

        HttpResponse httpResponse = deleteModulesHttpResponse(deleteModulesRequest);
        CommonLibs.assert200HttpStatus(httpResponse);
        DeleteModulesResponse responseBody = (DeleteModulesResponse) httpResponse.getObject();
        return responseBody;
    }


    /**
     * Sends a request to the Exchange store with the provided registry id, returns entire
     * HTTPResponse
     *
     * @return HttpResponse pojo object
     */
    public HttpResponse getModuleByRegistryIdHttpResponse(
        String registryId) {
        String url = String.format(Exchangev4Constants.EXCHANGE_STORE_BY_ID, registryId);
        return httpPortalClientV4
            .doPost(url, new JSONObject(),
                ExchangeV4FilterResponse.class);
    }

    /**
     * Sends a request to the Exchange store with the provided registry id, returns entire
     * responseBody after verifying 200 http status
     *
     * @return responseBody the ExchangeV4FilterResponse pojo object
     */
    public ExchangeV4FilterResponse getModuleByRegistryId(
        String registryId) {
        HttpResponse httpResponse = getModuleByRegistryIdHttpResponse(registryId);
        CommonLibs.assert200HttpStatus(httpResponse);
        ExchangeV4FilterResponse responseBody = (ExchangeV4FilterResponse) httpResponse.getObject();
        return responseBody;
    }

    /**
     * The methods get LogicModules details by its id
     *
     * @param localId local id of the module
     * @param moduleType type of module
     * @return responseBody -> HttpResponse object
     */
    public HttpResponse getModuleByLocalIdHttpResponse(
        int localId, String moduleType) {
        String url = String.format(Exchangev4Constants.MODULE_TOOLBOX_BY_ID, moduleType, localId);
        return httpPortalClientV4
            .doPost(url, new JSONObject(),
                ExchangeV4FilterResponse.class);
    }

    /**
     * The methods get LogicModules details by its id
     *
     * @param localId local id of the module
     * @param moduleType type of module
     * @return responseBody -> ExchangeV4FilterResponse pojo object
     */
    public ExchangeV4FilterResponse getModuleByLocalId(
        String localId, String moduleType) {
        HttpResponse httpResponse = getModuleByLocalIdHttpResponse(Integer.parseInt(localId),
            moduleType);
        CommonLibs.assert200HttpStatus(httpResponse);
        ExchangeV4FilterResponse responseBody = (ExchangeV4FilterResponse) httpResponse.getObject();
        return responseBody;
    }

    /**
     * Sends a request to the store privacy endpoint, returns entire HTTPResponse
     *
     * @param request PrivacyV4Request
     * @return HttpResponse pojo object
     */
    public HttpResponse changePrivacyHttpResponse(
        PrivacyV4Request request) {
        HttpResponse httpResponse = httpPortalClientV4
            .doPost(Exchangev4Constants.EXCHANGE_STORE_CHANGE_PRIVACY, request,
                ImportModulesV4Response.class);
        return httpResponse;
    }

    /**
     * Sends a request to the store privacy endpoint, returns entire responseBody after verifying
     * 200 http status
     *
     * @param request PrivacyV4Request
     * @return responseBody the ImportModulesV4Response pojo object
     */
    public ImportModulesV4Response changePrivacyResponse(
        PrivacyV4Request request) {
        HttpResponse httpResponse = changePrivacyHttpResponse(request);
        CommonLibs.assert200HttpStatus(httpResponse);
        ImportModulesV4Response responseBody = (ImportModulesV4Response) httpResponse.getObject();
        return responseBody;
    }

    /**
     * Given a list of Exchange Ids and the privacy boolean, sends a request to the store privacy
     * endpoint, returns entire responseBody after verifying 200 http status
     *
     * @param idList String list of Ids of modules whose privacy statuses should be changed
     * @param privacy privacy boolean of modules
     * @return responseBody the ImportModulesV4Response pojo object
     */
    public ImportModulesV4Response changePrivacyByIds(
        List<String> idList, boolean privacy) {

        // add is private to meta block
        PrivacyRequestMeta privacyRequestMeta = PrivacyRequestMeta.builder().isPrivate(privacy)
            .build();

        //Create AllIds List
        List<ModuleStoreBulkActionV4RequestAllId> privacyRequestAllIds = new ArrayList<>();
        for (String id : idList) {
            ModuleStoreBulkActionV4RequestAllId privacyRequestAllId = ModuleStoreBulkActionV4RequestAllId.builder()
                .model("exchangeLogicModules").id(id).build();
            privacyRequestAllIds.add(privacyRequestAllId);
        }

        //Create Data block
        ModuleStoreBulkActionV4RequestData privacyRequestData = ModuleStoreBulkActionV4RequestData.builder()
            .allIds(privacyRequestAllIds).build();
        //Create request
        PrivacyV4Request changePrivacyRequest = PrivacyV4Request.builder()
            .meta(privacyRequestMeta)
            .data(privacyRequestData)
            .build();

        ImportModulesV4Response responseBody = changePrivacyResponse(changePrivacyRequest);
        return responseBody;
    }

    /**
     * This method to create default payload to delete the single logicModule using APIv4
     *
     * @param moduleType Model type in LM Exchange
     * @param moduleId id of Module
     * @return DeleteModulesRequest pojo object
     */
    public DeleteModulesRequest defaultPayloadToDeleteModule(String moduleType, String moduleId) {
        DeleteModulesRequest deleteModulesRequest = new DeleteModulesRequest();
        DeleteModulesRequestData deleteModulesRequestData = new DeleteModulesRequestData();
        LogicModulesV4AllIds logicModulesV4AllIds = new LogicModulesV4AllIds();
        logicModulesV4AllIds.setModel(moduleType);
        logicModulesV4AllIds.setId(moduleId);
        List<LogicModulesV4AllIds> deleteModulesRequestAllIdList = new ArrayList<>();
        deleteModulesRequestAllIdList.add(logicModulesV4AllIds);
        deleteModulesRequestData.setAllIds(deleteModulesRequestAllIdList);
        deleteModulesRequest.setData(deleteModulesRequestData);
        return deleteModulesRequest;
    }

    /**
     * This method is to verify delete operation of module
     *
     * @param deleteModulesResponse DeleteModulesResponse pojo object
     * @param id id of the logicModule
     * @param moduleType Module type
     */
    public void verifyDeleteModuleOperation(DeleteModulesResponse deleteModulesResponse, String id,
        String moduleType) {
        CustomAssert.assertEqual(deleteModulesResponse.getData().getAllIds().get(0).getId(),
            id, "Verifying the Module Id");
        CustomAssert.assertEqual(deleteModulesResponse.getData().getAllIds().get(0).getModel(),
            moduleType, "Verifying the Module Type");
        CustomAssert.assertNull(deleteModulesResponse.getErrors(),
            "Verifying Error filed in Null");
    }

    /**
     * This method to get Denormalised details of Module
     *
     * @param exchangeDenormalizationV4Request ExchangeDenormalizationV4Request pojo object
     * @return ExchangeDenormalizationV4Response pojo object
     */
    public ExchangeDenormalizationV4Response denormalizationOfModule(
        ExchangeDenormalizationV4Request exchangeDenormalizationV4Request) {
        HttpResponse httpResponse = denormalizationOfModuleHttpResponse(
            exchangeDenormalizationV4Request);
        CommonLibs.assert200HttpStatus(httpResponse);
        return (ExchangeDenormalizationV4Response) httpResponse.getObject();
    }


    /**
     * This method to get Denormalised details of Module Which return HttpResponse
     *
     * @param exchangeDenormalizationV4Request ExchangeDenormalizationV4Request pojo object
     * @return HttpResponse pojo object
     */
    public HttpResponse denormalizationOfModuleHttpResponse(
        ExchangeDenormalizationV4Request exchangeDenormalizationV4Request) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/version4Denormalized+json");
        String requestUrl = DENORMALIZED_MODULE_PATH;
        return httpPortalClientV4Denormalized.doPost(requestUrl, exchangeDenormalizationV4Request,
            new HashMap(), headers, false, ExchangeDenormalizationV4Response.class);
    }


    /**
     * This Method is to create the Denormalization Request Data
     *
     * @param model Module name
     * @param id id of Module
     * @return ExchangeDenormalizationV4AllIds Pojo object
     */
    public ExchangeDenormalizationV4AllIds createDenormalizationRequestData(String model,
        String id) {
        ExchangeDenormalizationV4AllIds exchangeDenormalizationV4AllIds = new ExchangeDenormalizationV4AllIds();
        exchangeDenormalizationV4AllIds.setId(id);
        exchangeDenormalizationV4AllIds.setModel(model);
        return exchangeDenormalizationV4AllIds;
    }

    /**
     * This method is to create Default Payload For Denormalization
     *
     * @param exchangeDenormalizationV4AllIdsList list of ids for Denormalization
     * @return ExchangeDenormalizationV4Request pojo object.
     */
    public ExchangeDenormalizationV4Request createDefaultPayloadForDenormalization(
        List<ExchangeDenormalizationV4AllIds> exchangeDenormalizationV4AllIdsList) {
        ExchangeDenormalizationV4Request denormalizationV4Request = new ExchangeDenormalizationV4Request();
        ExchangeDenormalizationV4Data exchangeDenormalizationV4Data = new ExchangeDenormalizationV4Data();
        exchangeDenormalizationV4Data.setAllIds(exchangeDenormalizationV4AllIdsList);
        denormalizationV4Request.setData(exchangeDenormalizationV4Data);
        return denormalizationV4Request;
    }

    /**
     * Sends a DELETE request to the Store ExchangeLogicModulePackage endpoint, returns entire
     * responseBody after verifying 200 http status
     *
     * @param request DeleteModulesRequest
     * @return DeleteModulesResponse pojo object
     */
    public DeleteModulesResponse deleteExchangeLogicModulePackage(
        DeleteModulesRequest request) {
        HttpResponse httpResponse = httpPortalClientV4
            .doDelete(Exchangev4Constants.EXCHANGE_STORE_PACKAGE, request,
                DeleteModulesResponse.class);
        CommonLibs.assert200HttpStatus(httpResponse);
        return (DeleteModulesResponse) httpResponse.getObject();
    }

    /**
     * Method sends get request to get AllTagsResponse from store
     *
     * @return AllTagsResponse
     */
    public AllTagsResponse getAllTags() {
        HttpResponse httpResponse = httpPortalClientV4.doGet(Exchangev4Constants.EXCHANGE_ALL_TAGS,
            new HashMap<>(),
            AllTagsResponse.class);
        CommonLibs.assert200HttpStatus(httpResponse);
        return (AllTagsResponse) httpResponse.getObject();
    }

    /**
     * Method get LogicModules details in form of ExchangeItemV4 from exchange
     *
     * @param model type of modules
     * @param registryId registryId
     * @return ExchangeItemV4
     */
    public ExchangeItemV4 getLogicModuleDetailsFromStore(String model, String registryId) {
        logger.info("fetching the LogicModule Details from store ");
        ExchangeItemV4 exchangeItemV4;
        ExchangeV4FilterResponse exchangeV4FilterResponse = getModuleByRegistryId(registryId);
        if (!exchangeV4FilterResponse.getData().getAllIds().isEmpty()) {
            Map<String, ExchangeItemV4> allExchangeItemMap = exchangeV4FilterResponse
                .getData()
                .getById().get(model);
            exchangeItemV4 = allExchangeItemMap
                .get(registryId);
        } else {
            exchangeItemV4 = new ExchangeItemV4();
            logger.info("No Module found hence returning blank response ");
        }
        return exchangeItemV4;

    }

    /**
     * This method is to export LogicModule using APIv4 in Json format
     *
     * @param logicModuleId Id of LogicModule
     * @param moduleTypes LogicModule type
     * @return String Pojo object
     */
    public String exportLogicModuleV4(ExchangeModuleTypes moduleTypes, String logicModuleId) {
        HttpResponse httpResponse = exportLogicModuleV4HttpResponse(moduleTypes, logicModuleId);
        CommonLibs.assert200HttpStatus(httpResponse);
        return (String) httpResponse.getObject();
    }

    /**
     * This method is to export LogicModule using APIv3 in XML format
     *
     * @param logicModuleType type of logic module
     * @param logicModuleId logic module id
     * @return String response Pojo object
     */
    public String exportLogicModuleXML(String logicModuleType, int logicModuleId) {
        String requestUrl = String.format("setting/%s/%d", logicModuleType, logicModuleId);
        Map filter = new HashMap();
        filter.put("format", "xml");
        filter.put("v", "3");
        HttpResponse httpResponse = this.httpPortalClient.doGet(requestUrl, filter, String.class);
        CommonLibs.assert200HttpStatus(httpResponse);
        return (String) httpResponse.getObject();
    }

    /**
     * This method is to export LogicModule using APIv4 in Json format
     *
     * @param logicModuleId Id of LogicModule
     * @param moduleTypes LogicModule type
     * @return ImportExportFile Pojo object
     */
    public ImportExportFile exportLogicModuleV4WithResponseJSON(ExchangeModuleTypes moduleTypes,
        String logicModuleId) {
        String requestUrl = String
            .format(Exchangev4Constants.EXPORT_V4_PATH, moduleTypes.name(), logicModuleId);
        HttpResponse httpResponse = httpPortalClientV4
            .doPost(requestUrl, new HashMap<>(), ImportExportFile.class);
        CommonLibs.assert200HttpStatus(httpResponse);
        return (ImportExportFile) httpResponse.getObject();
    }

    /**
     * This method is to import LogicModule using APIv4 in Json format
     *
     * @param importExportFile request Pojo object
     * @return String response Pojo object
     */
    public String importLogicModuleV4FromJSONFile(String jsonFilePath,
        ImportExportFile importExportFile,
        String requestUrl) {

        String jsonString = null;

        try {
            jsonString = new ObjectMapper().writeValueAsString(importExportFile);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        Assert.assertNotNull(jsonString, "failed to write in the file..");
        new CommonLibs().writeContentInFile(jsonString, jsonFilePath);
        return importFromFile(jsonFilePath, requestUrl);
    }

    /**
     * This method is to import LogicModule using APIv4 in XML format
     *
     * @param importRequest String import request
     * @return String response Pojo object
     */
    public String importLogicModuleV4FromXMLFile(String xmlFilePath, String importRequest,
        String requestUrl) {
        new CommonLibs().writeContentInFile(importRequest, xmlFilePath);
        return importFromFile(xmlFilePath, requestUrl);
    }

    /**
     * This method is to import LogicModule using APIv4
     *
     * @param filePath path of the file to be imported
     * @param requestUrl target URL for the API
     * @return String response Pojo object
     */
    public String importFromFile(String filePath, String requestUrl) {
        Map<String, String> headers = new HashMap();
        headers.put("Accept", "application/version4Denormalized+json");

        FileSystemResource file = new FileSystemResource(filePath);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("file", file);

        HttpResponse httpResponse = httpPortalClientV4Denormalized
            .doPost(requestUrl, requestBody, String.class, headers, "import from file", true);
        return (String) httpResponse.getObject();
    }

    /**
     * Get Exchange DataSource ID from Exchange Store
     *
     * @param dsName - DataSource Name
     * @return String - Exchange DataSource ID
     */
    public String getDataSourceFromExchangeStore(String dsName) {
        ExchangeV4FilterResponse exchangeV4FilterResponse = getFilteredExchangeStoreWithoutAssert(
            createExchangeV4FilterRequest(dsName));
        // Verify the Requested DataSource is available in Exchange Store
        if (exchangeV4FilterResponse != null && !exchangeV4FilterResponse.getData().getById()
            .isEmpty() && exchangeV4FilterResponse.getData().getById().get(
            "exchangeDataSources").entrySet().stream().findFirst().isPresent()) {
            ExchangeItemV4 exchangeDataSource = exchangeV4FilterResponse.getData().getById().get(
                    "exchangeDataSources").entrySet().stream().findFirst().get()
                .getValue();
            if (exchangeDataSource.getName().equalsIgnoreCase(dsName)) {
                return exchangeDataSource.getId();
            }
        }
        return null;
    }


    /**
     * Create Exchange Store Filter request
     *
     * @param dsName - DataSource Name
     * @return ExchangeV4FilterRequest - Exchange Filter Request
     */
    private ExchangeV4FilterRequest createExchangeV4FilterRequest(String dsName) {

        Map<String, Map<String, String>> dynamicFilterFields = new HashMap<>();
        dynamicFilterFields.put("model", Map.of("exchangeDataSources", "EQ"));
        dynamicFilterFields.put("status", Map.of("CORE", "EQ", "COMMUNITY", "EQ"));

        return getExchangeV4FilterRequestWithoutRegex(dsName, dynamicFilterFields, 1);
    }


    /**
     * Create Exchange Store Filter request for cloud DS
     *
     * @param dsName - DataSource Name
     * @return ExchangeV4FilterRequest - Exchange Filter Request
     */
    private ExchangeV4FilterRequest createExchangeV4FilterRequestForCloud(String dsName) {

        Map<String, Map<String, String>> dynamicFilterFields = new HashMap<>();
        dynamicFilterFields.put("model", Map.of("exchangeDataSources", "EQ"));
        dynamicFilterFields.put("status",
            Map.of("CORE", "EQ", "COMMUNITY", "EQ", "SECURITY_REVIEW", "EQ"));

        return getExchangeV4FilterRequestWithoutRegex(dsName, dynamicFilterFields, 1);
    }

    /**
     * Get Exchange DataSource ID from Exchange Store for cloud DS
     *
     * @param dsName - DataSource Name
     * @return String - Exchange DataSource ID
     */
    public String getDataSourceFromExchangeStoreForCloud(String dsName) {
        ExchangeV4FilterResponse exchangeV4FilterResponse = getFilteredExchangeStoreWithoutAssert(
            createExchangeV4FilterRequestForCloud(dsName));
        // Verify the Requested DataSource is available in Exchange Store
        if (exchangeV4FilterResponse != null && !exchangeV4FilterResponse.getData().getById()
            .isEmpty() && exchangeV4FilterResponse.getData().getById().get(
            EXCHANGE_DATA_SOURCES).entrySet().stream().findFirst().isPresent()) {
            ExchangeItemV4 exchangeDataSource = exchangeV4FilterResponse.getData().getById().get(
                    EXCHANGE_DATA_SOURCES).entrySet().stream().findFirst().get()
                .getValue();
            if (exchangeDataSource.getName().equalsIgnoreCase(dsName)) {
                return exchangeDataSource.getId();
            }
        }
        return null;
    }


    /**
     * This method creates ExchangeV4Filter Request
     *
     * @param name value to search name
     * @param dynamicFilterFields Map<String, Map<String, String>>  to add dynamic
     * @param perPageCount per page count
     * @return ExchangeV4FilterRequest
     */
    public ExchangeV4FilterRequest getExchangeV4FilterRequest(String name,
        Map<String, Map<String, String>> dynamicFilterFields, int perPageCount) {
        List<ExchangeV4FilterDynamicRequest> dynamicRequestList = getExchangeV4FilterDynamicRequestList(
            dynamicFilterFields);
        return createExchangeV4FilterRequest(
            perPageCount, 0, "-updatedAtMS"
            , ".*" + name.replace(" ", ".*") + ".*", dynamicRequestList);
    }

    public ExchangeV4FilterRequest getExchangeV4FilterRequestWithoutRegex(String name,
        Map<String, Map<String, String>> dynamicFilterFields, int perPageCount) {
        List<ExchangeV4FilterDynamicRequest> dynamicRequestList = getExchangeV4FilterDynamicRequestList(
            dynamicFilterFields);
        return createExchangeV4FilterRequest(
            1, 0, "-updatedAtMS", name, dynamicRequestList);
    }

    public List<ExchangeV4FilterDynamicRequest> getExchangeV4FilterDynamicRequestList(
        Map<String, Map<String, String>> dynamicFilterFields) {
        List<ExchangeV4FilterDynamicRequest> dynamicRequestList = new ArrayList<>();
        for (Entry<String, Map<String, String>> field : dynamicFilterFields.entrySet()) {
            List<ExchangeV4FilterDynamicExpressionsRequest> moduleTypeExpressionList = new ArrayList<>();
            for (Entry<String, String> expression : field.getValue().entrySet()) {
                ExchangeV4FilterDynamicExpressionsRequest moduleTypeDynamicExpression = getExchangeV4FilterDynamicExpressionsRequest(
                    expression.getValue(), expression.getKey());
                moduleTypeExpressionList.add(moduleTypeDynamicExpression);
            }
            ExchangeV4FilterDynamicRequest moduleTypeDynamicBlock = ExchangeV4FilterDynamicRequest
                .builder()
                .field(field.getKey())
                .expressions(moduleTypeExpressionList)
                .build();
            dynamicRequestList.add(moduleTypeDynamicBlock);
        }
        return dynamicRequestList;
    }

    /**
     * Process the DataSource Import response to get imported/existing DataSource ID
     *
     * @param importModulesV4Response - Exchange import response
     * @param dsName - DataSource Name
     * @return Integer - DataSource ID
     */
    public Integer processDataSourceImportResponse(ImportModulesV4Response importModulesV4Response,
        String dsName) {
        int dataSourceId = -1;
        if (importModulesV4Response.getErrors() != null) {
            if (importModulesV4Response.getErrors().get(0).getType()
                .equalsIgnoreCase("CONFLICT")) {
                String errorDetail = importModulesV4Response.getErrors().get(0)
                    .getDetail();
                Pattern pattern = Pattern.compile("`[0-9]*`");
                Matcher matcher = pattern.matcher(errorDetail);
                dataSourceId = (matcher.find() ?
                    Integer.parseInt(matcher.group(0).replaceAll("`", "")) : -1);
                logger.info(String.format("%s already exists with ID: %d", dsName,
                    dataSourceId));
            } else {
                logger.error(String.format(
                    "Received following error while importing DataSource %s: %s",
                    dsName,
                    importModulesV4Response.getErrors().get(0).getDetail()));
            }
        } else {
            dataSourceId =
                Integer.parseInt(
                    importModulesV4Response.getData().getAllIds().get(0).getId());
            logger.info(String.format("Imported %s DataSource with ID: %d", dsName,
                dataSourceId));
        }

        return dataSourceId;
    }

    /**
     * Import DataSource from Exchange Store
     *
     * @param exchangeDataSourceId - Exchange DataSource ID
     * @return ImportModulesV4Response - Exchange Import Module Response
     */
    public ImportModulesV4Response importDataSourceFromExchangeStore(
        String exchangeDataSourceId) {
        try {
            ExchangeV4Lib exchangeV4Lib = new ExchangeV4Lib();
            LogicModulesV4AllIds idBlock = LogicModulesV4AllIds.builder()
                .model("exchangeLogicModules").id(exchangeDataSourceId).build();

            //Create Data Block
            ImportModulesV4RequestData dataRequest = ImportModulesV4RequestData.builder()
                .allIds(List.of(idBlock))
                .build();

            //Create Meta Block
            ImportModulesV4RequestMeta metaRequest = ImportModulesV4RequestMeta.builder()
                .fieldsToPreserve(new ArrayList<>()).handleConflict("ERROR").build();

            //Create Request
            ImportModulesV4Request importModulesV4Request = ImportModulesV4Request.builder()
                .data(dataRequest)
                .meta(metaRequest).build();

            HttpResponse httpResponse = exchangeV4Lib.importModulesHttpResponse(
                importModulesV4Request);

            if (httpResponse.getHttpStatus() != 200) {
                logger.error("Received error while making import request in Exchange Store: "
                    + httpResponse.getFailureResponse());
                return null;
            } else {
                return (ImportModulesV4Response) httpResponse.getObject();
            }
        } catch (Exception e) {
            logger.error("Failed to import DataSource from Exchange", e);
            return null;
        }
    }

    /**
     * This method creates the Exchange Access Group request
     *
     * @param id of access group
     * @return exchangeAccessGroupsIds request
     */
    public ExchangeAccessGroupsId createExchangeAccessGroupIdRequest(String id) {
        ExchangeAccessGroupsId exchangeAccessGroupsId =
            new ExchangeAccessGroupsId();
        exchangeAccessGroupsId.setId(id);
        exchangeAccessGroupsId.setModel("exchangeAccessGroups");
        return exchangeAccessGroupsId;
    }

    /**
     * This method remove the accessGroupIds from given list of access Groups Ids
     *
     * @param exchangeAccessGroupsIds List of ExchangeAccessGroupsId
     * @param accessGroupId access group id to remove
     * @return List<ExchangeAccessGroupsId> updated
     */
    public List<ExchangeAccessGroupsId> removeAccessGroups(
        List<ExchangeAccessGroupsId> exchangeAccessGroupsIds,
        String accessGroupId) {
        logger.info("Removing access group id: " + accessGroupId);
        for (ExchangeAccessGroupsId exchangeAccessGroupsId : exchangeAccessGroupsIds) {
            if (Objects.equals(exchangeAccessGroupsId.getId(), accessGroupId)) {
                exchangeAccessGroupsIds.remove(exchangeAccessGroupsId);
                break;
            }
        }
        return exchangeAccessGroupsIds;
    }

    /**
     * This method remove the accessGroupIds from given list of access Groups Ids
     *
     * @param accessGroupId integer value
     * @return List<ExchangeAccessGroupsId>
     */
    public List<ExchangeAccessGroupsId> removeAccessGroups(
        List<ExchangeAccessGroupsId> exchangeAccessGroupsIds,
        int accessGroupId) {
        return removeAccessGroups(exchangeAccessGroupsIds, Integer.toString(accessGroupId));
    }

    /**
     * This method is to force match any logic module type with resources
     *
     * @param moduleType kind of logicModule
     * @param logicModuleId logicModule Id
     */
    public void forceResourceMatchLogicModuleV4(String moduleType,
        String logicModuleId) {
        String requestUrl = String
            .format(Exchangev4Constants.FORCE_RESOURCE_MATCH_LOGIC_MODULE, moduleType,
                logicModuleId);
        HttpResponse httpResponse = httpPortalClient
            .doPost(requestUrl, new HashMap<>(), String.class);
        CommonLibs.assert200HttpStatus(httpResponse);
    }

    /**
     * This method is to export LogicModule using APIv4 in Json format
     *
     * @param logicModuleId Id of LogicModule
     * @param moduleTypes LogicModule type
     * @return HttpResponse
     */
    public HttpResponse exportLogicModuleV4HttpResponse(ExchangeModuleTypes moduleTypes,
        String logicModuleId) {
        String requestUrl = String
            .format(Exchangev4Constants.EXPORT_V4_PATH, moduleTypes.name(), logicModuleId);
        HttpResponse httpResponse = httpPortalClientV4
            .doPost(requestUrl, new HashMap<>(), String.class);
        return httpResponse;
    }

    /**
     * Upgrade DataSource from Exchange Store
     *
     * @param exchangeDataSourceId - Exchange DataSource ID
     * @param datasourceName - DataSource Name
     * @return ImportModulesV4Response - Exchange Import Module Response
     */
    public ImportModulesV4Response upgradeDataSourceFromExchangeStore(
        String exchangeDataSourceId, String datasourceName) {
        try {
            ExchangeV4Lib exchangeV4Lib = new ExchangeV4Lib();
            ExchangeItemV4 exchangeItemV4 = ExchangeItemV4.builder()
                .model(COMMON_MODEL)
                .name(datasourceName)
                .upgradeableRegistryId(exchangeDataSourceId)
                .build();

            //Create Data Block
            ImportModulesV4RequestData dataRequest = ImportModulesV4RequestData.builder()
                .items(List.of(exchangeItemV4))
                .build();

            //Create Meta Block
            ImportModulesV4RequestMeta metaRequest = ImportModulesV4RequestMeta.builder()
                .fieldsToPreserve(new ArrayList<>()).handleConflict("ERROR").build();

            //Create Request
            ImportModulesV4Request importModulesV4Request = ImportModulesV4Request.builder()
                .data(dataRequest)
                .meta(metaRequest).build();

            HttpResponse httpResponse = exchangeV4Lib.importModulesHttpResponse(
                importModulesV4Request);

            if (httpResponse.getHttpStatus() != 200) {
                logger.error("Received error while making upgrading request in Exchange Store: {}",
                    httpResponse.getFailureResponse());
                return null;
            } else {
                return (ImportModulesV4Response) httpResponse.getObject();
            }
        } catch (Exception e) {
            logger.error("Failed to upgrade DataSource from Exchange", e);
            return null;
        }
    }

    /**
     * Method to get device details of a module
     *
     * @param moduleType - type of module
     * @param moduleId - module Id
     * @return ExchangeDeviceAssociated - device details pojo response
     */
    public ExchangeDeviceAssociated getDeviceAssociatedDetails(String moduleType, String moduleId) {

        ExchangeDeviceAssociated deviceAssociated = null;

        if (moduleType.equals(ExchangeModuleTypes.exchangePropertySources.toString())) {
            moduleType = Exchangev4Constants.PROPERTYSOURCE_V3_DEVICE_CONSTANT;
        } else if (moduleType.equals(ExchangeModuleTypes.exchangeJobMonitors.toString())) {
            moduleType = Exchangev4Constants.JOBMONITOR_V3_DEVICE_CONSTANT;
        } else {
            moduleType = moduleType.substring(8).toLowerCase();
        }

        String requestUrl = String.format("setting/%s/%s/devices", moduleType, moduleId);
        for (int i = 0; i < 5; i++) {
            HttpResponse httpResponse = this.httpPortalClient.doGet(requestUrl,
                ExchangeDeviceAssociated.class);
            CommonLibs.assert200HttpStatus(httpResponse);
            deviceAssociated = (ExchangeDeviceAssociated) httpResponse.getObject();

            if (deviceAssociated.getItems().isEmpty()) {
                TimeUtils.sleep(10);
            } else {
                break;
            }
        }

        Assert.assertNotNull(deviceAssociated,
            "Failed to fetch the device details for " + moduleType);
        return deviceAssociated;
    }

    /**
     * Method to get module details when searched by locator on Exchange
     *
     * @param locator - locator id of module
     * @return ExchangeV4FilterResponse response pojo object
     */
    public ExchangeV4FilterResponse getModuleDetailsByLocator(String locator) {
        return getModuleByRegistryId(locator);
    }

    /**
     * Method to add an access group id to the module
     *
     * @param moduleType - kind of module
     * @param moduleId = module id
     * @param accessGroupId - access group id to be added
     * @return ExchangeV4FilterResponse response pojo object
     */
    public ExchangeV4FilterResponse addAccessGroupToModule(ExchangeModuleTypes moduleType,
        String moduleId, String accessGroupId) {

        ExchangeV4FilterResponse exchangeV4FilterResponse = getModuleByLocalId(moduleId,
            moduleType.toString());

        List<ExchangeAccessGroupsId> existingModuleAccessGroupList = exchangeV4FilterResponse.getData()
            .getById()
            .get(moduleType.toString()).get(moduleId).getExchangeAccessGroupsIds();

        ExchangeItemV4 exchangeItemV4 = new ExchangeItemV4();
        existingModuleAccessGroupList.add(createExchangeAccessGroupIdRequest(accessGroupId));
        exchangeItemV4.setExchangeAccessGroupsIds(existingModuleAccessGroupList);

        exchangeV4FilterResponse.getData().getById()
            .get(moduleType.toString()).put(moduleId, exchangeItemV4);

        String requestUrl = String.format(Exchangev4Constants.UPDATE_LOGIC_MODULE_V4, moduleType);
        HttpResponse httpResponse = this.httpPortalClientV4.doPatch(requestUrl,
            exchangeV4FilterResponse, ExchangeV4FilterResponse.class);
        CommonLibs.assert200HttpStatus(httpResponse);

        return (ExchangeV4FilterResponse) httpResponse.getObject();

    }

    /**
     * This method is to upload the script  for any logicModule
     *
     * @param filePath path of the script file to be uploaded
     * @return true of false based on upload result
     */
    public boolean uploadScript(String filePath) {
        FileSystemResource file = new FileSystemResource(filePath);

        HttpResponse httpResponse = uploadScriptHttpResponse(file);
        CommonLibs.assert200HttpStatus(httpResponse);

        if (httpResponse.getHttpStatus() == 200) {
            logger.info("File uploaded successfully");
            return true;
        }

        logger.info("Error uploading file");
        return false;
    }

    /**
     * This method is to return http response of upload script operation
     *
     * @param file file to be uploaded
     * @return HttpResponse pojo response
     */
    public HttpResponse uploadScriptHttpResponse(FileSystemResource file) {
        String requestUrl = EventSourceConstants.UPLOAD_SCRIPT_PATH;
        HttpResponse httpResponse = httpPortalClient
            .doPost(requestUrl, file, String.class);

        return httpResponse;
    }

    /**
     * This method is to import modules from repository
     *
     * @param repoDiffRequest import from repository request
     * @return ExchangeV4FilterResponse pojo response
     */
    public ExchangeV4FilterResponse importModuleFromRepository(RepoDiffRequest repoDiffRequest) {
        String requestUrl = Exchangev4Constants.IMPORT_FROM_REPO_PATH;
        HttpResponse httpResponse = this.httpPortalClientV4.doPost(requestUrl, repoDiffRequest,
            ExchangeV4FilterResponse.class);
        CommonLibs.assert200HttpStatus(httpResponse);
        return (ExchangeV4FilterResponse) httpResponse.getObject();
    }

    /**
     * This method is to delete package from exchange
     *
     * @param packageId id of the package to be deleted
     * @return DeleteModulesResponse pojo response
     */
    public DeleteModulesResponse deleteExchangeLogicModulePackage(String packageId) {
        DeleteModulesRequest deleteModulesRequest = defaultPayloadToDeleteModule(
            Exchangev4Constants.PACKAGE_MODEL, packageId);
        DeleteModulesResponse deleteModulesResponse = deleteExchangeLogicModulePackage(
            deleteModulesRequest);
        CustomAssert.assertTrue(!deleteModulesResponse.getData().getAllIds().isEmpty(),
            "verifying if package was actually deleted...");
        return deleteModulesResponse;
    }

    /**
     * This method is to skip install module/package pojo request
     *
     * @param registryIdList list of registryIds
     * @param skipReason reason to skip install
     * @return SkipUpdateRequest pojo request
     */
    public SkipUpdateRequest createSkipInstallRequestV4(List<String> registryIdList,
        String skipReason) {
        List<ModuleStoreBulkActionV4RequestAllId> moduleStoreBulkActionV4RequestAllIdList = new ArrayList<>();
        for (String registryId : registryIdList) {
            moduleStoreBulkActionV4RequestAllIdList.add(
                ModuleStoreBulkActionV4RequestAllId.builder()
                    .upgradeableRegistryId(registryId).model(COMMON_MODEL)
                    .build());
        }
        ModuleStoreBulkActionV4RequestData moduleStoreBulkActionV4RequestData = ModuleStoreBulkActionV4RequestData.builder()
            .items(moduleStoreBulkActionV4RequestAllIdList).build();
        SkipUpdateRequestMeta skipUpdateRequestMeta = SkipUpdateRequestMeta.builder()
            .skipReason(skipReason).build();
        SkipUpdateRequest skipInstallRequest = SkipUpdateRequest.builder()
            .data(moduleStoreBulkActionV4RequestData).meta(skipUpdateRequestMeta).build();
        return skipInstallRequest;
    }

    /**
     * Method to set the core filter on Exchange and Toolbox for any type of module
     *
     * @param moduleName - name of the module
     * @param moduleType = kind of module
     * @param coreFilter - core filter name
     * @return ExchangeV4FilterRequest pojo
     */
    public ExchangeV4FilterRequest getExchangeFilterRequest(String moduleName, String moduleType,
        String coreFilter) {
        ExchangeV4FilterDynamicRequest exchangeV4FilterDynamicRequest1 =
            getExchangeV4FilterDynamicRequest("model", moduleType, "EQ");
        ExchangeV4FilterDynamicRequest exchangeV4FilterDynamicRequest2 =
            getExchangeV4FilterDynamicRequest(coreFilter, "CORE", "EQ");
        List<ExchangeV4FilterDynamicRequest> exchangeV4FilterDynamicRequestList = Arrays.asList(
            exchangeV4FilterDynamicRequest1, exchangeV4FilterDynamicRequest2);
        ExchangeV4FilterRequest exchangeV4FilterRequest =
            createExchangeV4FilterRequest(10, 0, "-updatedAtMS",
                moduleName, exchangeV4FilterDynamicRequestList);
        return exchangeV4FilterRequest;
    }

    /**
     * Method to get the local id of the module when importing it from Exchange
     *
     * @param moduleName - name of the module
     * @param moduleType = kind of module
     * @return String - local id of the module after it has been imported
     */
    public String getIdByImportingLogicModuleFromExchange(String moduleName, String moduleType) {
        String localId = null;
        ExchangeV4FilterRequest exchangeV4FilterRequest = getExchangeFilterRequest(moduleName,
            moduleType, "status");
        ExchangeV4FilterResponse exchangeV4FilterResponse = getFilteredExchangeStore(
            exchangeV4FilterRequest);
        CustomAssert.assertTrue(!exchangeV4FilterResponse.getData().getAllIds().isEmpty(),
            "verifying if filter search results were found in Exchange...");

        for (LogicModulesV4AllIds id : exchangeV4FilterResponse.getData().getAllIds()) {
            String registryId = id.getId();
            ExchangeItemV4 item = exchangeV4FilterResponse.getData().getById().get(moduleType)
                .get(registryId);
            if (item.getName().equals(moduleName)) {
                ExchangeItemV4 exchangeItemV4 = importModuleById(registryId);
                localId = exchangeItemV4.getId();
            }
        }
        return localId;
    }

    /**
     * Method to check whether module is present in toolbox or not
     *
     * @param exchangeV4FilterResponse - Filter search response
     * @param moduleName - name of the module
     * @param moduleType - kind of module
     * @return boolean value whether search was found or not
     */
    public boolean isModulePresentInExchangeV4FilterResponse(
        ExchangeV4FilterResponse exchangeV4FilterResponse,
        String moduleName, String moduleType) {
        logger.info("verifying if core module is present in exchangeV4FilterResponse...");
        for (LogicModulesV4AllIds id : exchangeV4FilterResponse.getData().getAllIds()) {
            String localModuleId = id.getId();
            ExchangeItemV4 item = exchangeV4FilterResponse.getData().getById().get(moduleType)
                .get(localModuleId);
            if (item.getName().equals(moduleName)) {
                logger.info("search found in exchangeV4FilterResponse for " + moduleName);
                return true;
            }
        }
        logger.info("search for " + moduleName + " was not found in exchangeV4FilterResponse!!");
        return false;
    }


    /**
     * Method to return the module Id if present in toolbox
     *
     * @param exchangeV4FilterResponse - Filter search response
     * @param moduleName - name of the module
     * @param moduleType - kind of module
     * @return String value representing module Id
     */
    public String getModuleIdFromToolbox(ExchangeV4FilterResponse exchangeV4FilterResponse,
        String moduleName, String moduleType) {
        for (LogicModulesV4AllIds id : exchangeV4FilterResponse.getData().getAllIds()) {
            String localModuleId = id.getId();
            ExchangeItemV4 item = exchangeV4FilterResponse.getData().getById().get(moduleType)
                .get(localModuleId);
            if (item.getName().equals(moduleName)) {
                logger.info("Module found in toolbox with id " + localModuleId);
                return localModuleId;
            }
        }
        logger.info("Module not found in toolbox hence returning null value!!");
        return null;
    }

    /**
     * Method to check whether module present is updatable or not
     *
     * @param localId - local Id of the module
     * @param moduleName - name of the module
     * @param moduleType - kind of module
     * @return boolean value whether search was found or not
     */
    public boolean IsModuleUpdatable(String localId, String moduleName,
        String moduleType) {
        logger.info("verifying is logic module is updatable...");
        ExchangeV4FilterResponse exchangeV4FilterResponse = getModuleByLocalId(
            localId, moduleType);
        CustomAssert.assertTrue(!exchangeV4FilterResponse.getData().getAllIds().isEmpty(),
            "verifying if"
                + "module details were correctly fetched...");
        if (exchangeV4FilterResponse.getData().getById().get(moduleType).get(localId)
            .getInstallationStatuses().contains("CAN_UPGRADE")) {
            return true;
        } else {
            logger.info(moduleName + " is already up to date!!");
        }
        return false;
    }

    /**
     * Method to update module if it is updatable
     *
     * @param moduleId - local id of the module
     * @param moduleName - name of the module
     * @param moduleType - kind of module
     * @return String - logic Module local Id
     */
    public String updateLogicModuleToLatestVersion(String moduleId, String moduleName,
        String moduleType) {
        if (IsModuleUpdatable(moduleId, moduleName, moduleType)) {
            logger.info("Updating " + moduleName + " to latest version...");
            logger.info("deleting the logicModule locally...");
            DeleteModulesRequest deleteModulesRequest = defaultPayloadToDeleteModule(moduleType,
                moduleId);
            deleteModulesResponse(deleteModulesRequest);
            logger.info("installing latest version from Exchange...");
            moduleId = getIdByImportingLogicModuleFromExchange(moduleName,
                moduleType);
        }
        return moduleId;
    }

    /**
     * Method to convert any object to ExchangeV4FilterResponse pojo object
     *
     * @param response Object class
     * @return ExchangeV4FilterResponse pojo
     */
    public ExchangeV4FilterResponse convertToFilterResponsePojo(Object response) {
        ExchangeV4FilterResponse exchangeV4FilterResponse = new ObjectMapper().convertValue(
            response, ExchangeV4FilterResponse.class);
        return exchangeV4FilterResponse;
    }

    /**
     * Method to import any module from core repository
     *
     * @param moduleType - type of module
     * @param moduleName - name of module
     * @return ExchangeV4FilterResponse pojo response
     */
    public ExchangeV4FilterResponse importModuleFromCoreRepository(ExchangeModuleTypes moduleType,
        String moduleName) {
        HttpClient httpClientCustomJSON = HttpClient
            .getHttpClientWithPortalBasedAuth(TargetEnvConfig.getPortalUser(),
                TargetEnvConfig.getPortalPassword(),
                TargetEnvConfig.getCompany(), TargetEnvConfig.getBaseUrl(),
                CommonLibConstants.ApiVersion.V4);

        ExchangeToolbox exchangeToolboxDenormalised = new ExchangeToolbox(httpClientCustomJSON);
        ExchangeToolbox exchangeToolbox = new ExchangeToolbox();

        boolean matchFound = false;
        //check modules to be imported from repository
        logger.info("checking the module to be imported from core repository...");
        RepoDiffResponse repoDiffResponse = exchangeToolboxDenormalised.getRepoDiff(
            TargetEnvConfig.getCoreServerUrl(), TargetEnvConfig.repositoryUserName,
            TargetEnvConfig.repositoryPassword, moduleType);
        CustomAssert.assertTrue(!repoDiffResponse.getData().getItems().isEmpty(),
            "verifying if list of modules were found in the repo...");

        for (Item item : repoDiffResponse.getData().getItems()) {
            if (item.getName().equals(moduleName)) {
                matchFound = true;
                break;
            }
        }

        if (matchFound) {
            logger.info(moduleType + " found in core repository !!");

            //import module from repository
            logger.info("importing " + moduleType + " from core repository...");
            List<String> modulesToImport = new ArrayList<>();
            modulesToImport.add(moduleName);
            RepoDiffRequest repoDiffRequest = exchangeToolbox.getRepoDiffRequest(
                TargetEnvConfig.getCoreServerUrl(), TargetEnvConfig.repositoryUserName,
                TargetEnvConfig.repositoryPassword, moduleType);
            repoDiffRequest.getMeta().setImportModuleIdentifiers(modulesToImport);

            return importModuleFromRepository(repoDiffRequest);
        } else {
            logger.error(
                "search for " + moduleType + " not found in the core repo hence returning null");
            return null;
        }
    }

    /**
     * Method to set the deprecated filter on Exchange and Toolbox for any type of module
     *
     * @param moduleName - name of the module
     * @param moduleType = kind of module
     * @param deprecatedFilter - deprecated filter name
     * @return ExchangeV4FilterRequest pojo
     */
    public ExchangeV4FilterRequest getExchangeFilterRequestForDeprecatedModule(String moduleName,
        String moduleType,
        String deprecatedFilter) {
        ExchangeV4FilterDynamicRequest exchangeV4FilterDynamicRequest1 =
            getExchangeV4FilterDynamicRequest("model", moduleType, "EQ");
        ExchangeV4FilterDynamicRequest exchangeV4FilterDynamicRequest2 =
            getExchangeV4FilterDynamicRequest(deprecatedFilter, "DEPRECATED", "EQ");
        List<ExchangeV4FilterDynamicRequest> exchangeV4FilterDynamicRequestList = Arrays.asList(
            exchangeV4FilterDynamicRequest1, exchangeV4FilterDynamicRequest2);
        ExchangeV4FilterRequest exchangeV4FilterRequest =
            createExchangeV4FilterRequest(10, 0, "-updatedAtMS",
                moduleName, exchangeV4FilterDynamicRequestList);
        return exchangeV4FilterRequest;
    }

    /**
     * Method create filter request to search all logicModules type
     *
     * @param name name of logicModule or partial name
     * @return ExchangeV4FilterRequest
     */
    public ExchangeV4FilterRequest getExchangeV4FilterRequestForAllModuleType(String name,
        int perPageCount, int pageOffset) {
        List<ExchangeV4FilterDynamicRequest> dynamicRequestList =
            buildV4DynamicFilterList("model", Stream.of(ExchangeModuleTypes.values())
                .map(ExchangeModuleTypes::name)
                .collect(Collectors.toList()));
        return createExchangeV4FilterRequest(perPageCount, pageOffset, "name", ".*" + name + ".*",
            dynamicRequestList);
    }

    /**
     * Method to reassign a module's origin using locator from Exchange
     *
     * @param moduleType -> type of module
     * @param moduleId -> local id of module
     * @param locator -> exchange locator of module
     * @return ExchangeV4FilterResponse pojo response
     */
    public ExchangeV4FilterResponse reassignModuleOrigin(ExchangeModuleTypes moduleType,
        String moduleId, String locator) {
        String requestUrl = String.format(Exchangev4Constants.REASSIGN_MODULE_ORIGIN_PATH,
            moduleType.toString(), moduleId, locator);
        HttpResponse httpResponse = httpPortalClientV4
            .doPost(requestUrl, new HashMap<>(), ExchangeV4FilterResponse.class);
        CommonLibs.assert200HttpStatus(httpResponse);
        return convertToFilterResponsePojo(httpResponse.getObject());
    }

    /**
     * Method to get the exchange metadata of all versions available in the lineage
     *
     * @param lineageId -> LineageId of the module whose lineage data needs to be fetched
     * @return ExchangeV4FilterResponse pojo response
     */
    public ExchangeV4FilterResponse getAllExchangeModuleLineageIdsAvailable(String lineageId) {
        String requestUrl = String.format(EXCHANGE_STORE_LINEAGES, lineageId);
        HttpResponse httpResponse = httpPortalClientV4
            .doPost(requestUrl, new HashMap<>(), ExchangeV4FilterResponse.class);
        Object responseObject = httpResponse.getObject();
        if (responseObject == null) {
            throw new IllegalStateException("Received null response from Exchange Lineage Service");
        } else {
            CommonLibs.assert200HttpStatus(httpResponse);
        }
        return convertToFilterResponsePojo(responseObject);
    }

    /**
     * Method to create version diff request between local and Exchange module version data
     *
     * @param localId -> LocalId of the logic module
     * @param registryId -> RegistryId of the logic module
     * @param moduleType -> type of logic module
     * @return ExchangeV4FilterResponse pojo response
     */
    public ExchangeV4FilterResponse createFilterRequestForExchangeVersionDifferences(String localId,
        String registryId, String moduleType) {
        ExchangeV4FilterResponse exchangeV4FilterResponse = new ExchangeV4FilterResponse();
        ExchangeV4FilterDataResponse dataResponse = new ExchangeV4FilterDataResponse();
        List<LogicModulesV4AllIds> modulesV4AllIds = new ArrayList<>();

        LogicModulesV4AllIds localReference = new LogicModulesV4AllIds();
        localReference.setId(localId);
        localReference.setModel(moduleType);
        modulesV4AllIds.add(localReference);

        LogicModulesV4AllIds exchangeReference = new LogicModulesV4AllIds();
        exchangeReference.setId(registryId);
        exchangeReference.setModel(COMMON_MODEL);
        modulesV4AllIds.add(exchangeReference);

        dataResponse.setAllIds(modulesV4AllIds);
        exchangeV4FilterResponse.setData(dataResponse);
        return exchangeV4FilterResponse;
    }

    /**
     * Method to get version differences metadata between local and Exchange module versions
     *
     * @param localId -> LocalId of the logic module
     * @param registryId -> RegistryId of the logic module
     * @param moduleType -> type of logic module
     * @return ExchangeDenormalizationV4Response pojo response
     */
    public ExchangeDenormalizationV4Response getExchangeAndLocalLogicModuleVersionDifferences(
        String localId,
        String registryId, String moduleType) {
        ExchangeV4FilterResponse versionDiffRequest = createFilterRequestForExchangeVersionDifferences(
            localId, registryId, moduleType);
        String requestUrl = DENORMALIZED_MODULE_PATH;

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/version4Denormalized+json");

        HttpResponse httpResponse = httpPortalClientV4
            .doPost(requestUrl, versionDiffRequest, new HashMap<>(), headers, false,
                ExchangeDenormalizationV4Response.class);
        CommonLibs.assert200HttpStatus(httpResponse);
        return (ExchangeDenormalizationV4Response) httpResponse.getObject();
    }

    /**
     * Method to create request for reverting module to previous version available in lineage
     *
     * @param moduleName name of the module
     * @param registryId registry id of the module to which it needs to be reverted
     * @return ImportModulesV4Request pojo request
     */
    public ImportModulesV4Request createRevertModuleRequest(String moduleName, String registryId) {
        ExchangeItemV4 exchangeItemV4 = ExchangeItemV4.builder()
            .model(COMMON_MODEL)
            .name(moduleName)
            .upgradeableRegistryId(registryId)
            .build();

        //Create Data Block
        ImportModulesV4RequestData dataRequest = ImportModulesV4RequestData.builder()
            .items(List.of(exchangeItemV4))
            .build();

        //Create Meta Block
        ImportModulesV4RequestMeta metaRequest = ImportModulesV4RequestMeta.builder()
            .fieldsToPreserve(new ArrayList<>()).handleConflict("ERROR").build();

        //Create Revert Module Request
        ImportModulesV4Request importModulesV4Request = ImportModulesV4Request.builder()
            .data(dataRequest)
            .meta(metaRequest).build();

        return importModulesV4Request;
    }

    /**
     * Method to revert module to previous Exchange version
     *
     * @param importModulesV4Request Revert module pojo request
     * @return ImportModulesV4Response pojo response
     */
    public ImportModulesV4Response revertModuleToAnotherVersion(
        ImportModulesV4Request importModulesV4Request) {
        HttpResponse httpResponse = importModulesHttpResponse(importModulesV4Request);
        CommonLibs.assert200HttpStatus(httpResponse);
        return (ImportModulesV4Response) httpResponse.getObject();
    }

}
