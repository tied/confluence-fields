package com.mesilat.confield;

import java.util.List;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

public interface DataService {
    public static final int S_OK = 200;

    ObjectNode getPageDetail(Long id);
    ArrayNode getPageDetails(Long[] ids);


    DataServiceResult test(String confluenceId, String filter, String q, int limit) throws DataServiceException;

    String getConfluenceBaseUrl(long fieldId);
    void clearConfluenceBaseUrls();
    DataServiceResult getPage(long fieldId, String pageTitle) throws DataServiceException;
    DataServiceResult findPages(long fieldId, String q, int limit) throws DataServiceException;
    DataServiceResult getPagesById(long fieldId, List<String> pageId) throws DataServiceException;
    DataServiceResult getPagesByTitle(long fieldId, List<String> pageTitle) throws DataServiceException;
}