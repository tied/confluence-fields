package com.mesilat.confield;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.confluence.ConfluenceApplicationType;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExportAsService({DataService.class})
@Named
public class DataServiceImpl implements DataService {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    private final Map<Long,String> confluenceBaseUrl = new HashMap<>();

    @ComponentImport
    private final ApplicationLinkService appLinkService;
    @ComponentImport
    private final ActiveObjects ao;

    @Override
    public ObjectNode getPageDetail(Long id) {
        ArrayNode arr = getPageDetails(new Long[] { id });
        if (arr == null || arr.size() == 0 || arr.get(0).isNull()){
            return null;
        } else {
            return (ObjectNode)arr.get(0);
        }
    }
    @Override
    public ArrayNode getPageDetails(Long[] ids) {
        ApplicationLink confluence = appLinkService.getPrimaryApplicationLink(ConfluenceApplicationType.class);
        ApplicationLinkRequestFactory reqFactory = confluence.createAuthenticatedRequestFactory();
        try {
            return reqFactory
                .createRequest(Request.MethodType.POST, "/rest/nsi/1.0/data/pages")
                .addHeader("Content-Type", "application/json")
                .setEntity(ids)
                .executeAndReturn((com.atlassian.sal.api.net.Response response) -> {
                    if (response.getStatusCode() == S_OK) {
                        return response.getEntity(ArrayNode.class);
                    } else {
                        throw new RuntimeException(String.format("Error reading page details from Confluence server: %s", response.getResponseBodyAsString()));
                    }
                });
        } catch (CredentialsRequiredException|ResponseException ex) {
            throw new RuntimeException("Erro reading page details from Confluence server", ex);
        }
    }


    @Override
    public DataServiceResult test(String confluenceId, String filter, String q, int limit) throws DataServiceException {
        LOGGER.debug(String.format("Test find pages: %s | %s | %s | %d", confluenceId, filter, q, limit));
        try {
            I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
            ApplicationLink link = confluenceId == null
                    ? appLinkService.getPrimaryApplicationLink(ConfluenceApplicationType.class)
                    : appLinkService.getApplicationLink(new ApplicationId(confluenceId));
            
            if (link == null){
                throw new DataServiceException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    i18nHelper.getText("com.mesilat.confluence-field.err.linkNotFound", confluenceId == null? "default": confluenceId)
                );
            }

            ApplicationLinkRequestFactory reqFactory = link.createAuthenticatedRequestFactory();
            StringBuilder cql = new StringBuilder();
            cql.append("type=\"page\"");
            if (q != null && !q.isEmpty()){
                cql.append(" and title~\"")
                    .append(q)
                    .append("\"");
            }
            if (filter != null && !filter.isEmpty()){
                cql.append(" and ").append(filter);
            }
            String url = String.format("/rest/api/content/search?cql=%s&limit=%d", URLEncoder.encode(cql.toString(), "UTF-8"), limit);
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    return execute(request);
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex);
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public String getConfluenceBaseUrl(long fieldId){
        if (!confluenceBaseUrl.containsKey(fieldId)){
            try {
                FieldSettings fs = ao.get(FieldSettings.class, fieldId);
                ApplicationLink link;
                link = fs == null
                        ? appLinkService.getPrimaryApplicationLink(ConfluenceApplicationType.class)
                        : appLinkService.getApplicationLink(new ApplicationId(fs.getConfluenceId()));
                confluenceBaseUrl.put(fieldId, link.getDisplayUrl().toString());
            } catch (TypeNotInstalledException ex) {
                throw new RuntimeException(ex);
            }
        }
        return confluenceBaseUrl.get(fieldId);
    }
    @Override
    public void clearConfluenceBaseUrls(){
        confluenceBaseUrl.clear();
    }
    @Override
    public DataServiceResult getPage(long fieldId, String pageTitle) throws DataServiceException {
        LOGGER.debug(String.format("Get page for field %d", fieldId));
        try {
            FieldSettings fs = ao.get(FieldSettings.class, fieldId);
            ApplicationLinkRequestFactory reqFactory = createFactory(fs);
            StringBuilder cql = new StringBuilder();
            if (pageTitle.contains("\"")){
                cql.append("type=\"page\" and title='").append(pageTitle.replace("\"", "\\\\\"")).append("'");
            } else {
                cql.append("type=\"page\" and title=\"").append(pageTitle).append("\"");
            }
            if (fs != null && fs.getFilter() != null && !fs.getFilter().isEmpty()){
                cql.append(" and ").append(fs.getFilter());
            }
            String url = String.format("/rest/api/content/search?cql=%s&limit=1", URLEncoder.encode(cql.toString(), "UTF-8"));
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    return execute(request);
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex);
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
    }
    @Override
    public DataServiceResult findPages(long fieldId, String q, int limit) throws DataServiceException {
        LOGGER.debug(String.format("Find pages for field %d", fieldId));
        try {
            FieldSettings fs = ao.get(FieldSettings.class, fieldId);
            ApplicationLinkRequestFactory reqFactory = createFactory(fs);
            StringBuilder cql = new StringBuilder();
            cql.append("type=\"page\"");
            if (q != null && !q.isEmpty()){
                cql.append(" and title~\"")
                    .append(q)
                    .append("\"");
            }
            if (fs != null && fs.getFilter() != null && !fs.getFilter().isEmpty()){
                cql.append(" and ").append(fs.getFilter());
            }
            String url = String.format("/rest/api/content/search?cql=%s&limit=%d", URLEncoder.encode(cql.toString(), "UTF-8"), limit);
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    return execute(request);
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex);
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
    }
    @Override
    public DataServiceResult getPagesById(long fieldId, List<String> pageId) throws DataServiceException {
        LOGGER.debug(String.format("Get pages for field %d", fieldId));
        try {
            FieldSettings fs = ao.get(FieldSettings.class, fieldId);
            ApplicationLinkRequestFactory reqFactory = createFactory(fs);
            StringBuilder cql = new StringBuilder();
            cql.append("type=\"page\" and id in (")
                .append(String.join(",", pageId))
                .append(")");
            if (fs != null && fs.getFilter() != null && !fs.getFilter().isEmpty()){
                cql.append(" and ").append(fs.getFilter());
            }
            String url = String.format("/rest/api/content/search?cql=%s", URLEncoder.encode(cql.toString(), "UTF-8"));
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    return execute(request);
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex);
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
    }
    @Override
    public DataServiceResult getPagesByTitle(long fieldId, List<String> pageTitle) throws DataServiceException {
        LOGGER.debug(String.format("Get pages for field %d", fieldId));
        try {
            FieldSettings fs = ao.get(FieldSettings.class, fieldId);
            ApplicationLinkRequestFactory reqFactory = createFactory(fs);
            StringBuilder cql = new StringBuilder();
            cql.append("type=\"page\" and title in (")
               .append(
                    String.join(",", pageTitle
                        .stream()
                        .map(title -> {
                            if (title.contains("\"")){
                                return String.format("'%s'", title.replace("\"", "\\\\\""));
                            } else {
                                return String.format("\"%s\"", title);
                            }
                        })
                        .collect(Collectors.toList())
                    )
               )
               .append(")");

            if (fs.getFilter() != null && !fs.getFilter().isEmpty()){
                cql.append(" and ").append(fs.getFilter());
            }
            String url = String.format("/rest/api/content/search?cql=%s", URLEncoder.encode(cql.toString(), "UTF-8"));
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    return execute(request);
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex);
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
    }

    private DataServiceResult execute(ApplicationLinkRequest request) throws ResponseException {
        return request.execute(new ApplicationLinkResponseHandler<DataServiceResult>() { 
            @Override 
            public DataServiceResult credentialsRequired(com.atlassian.sal.api.net.Response response) throws ResponseException {
                LOGGER.debug(String.format("Response from remote server: code=%d", response.getStatusCode()));
                return new DataServiceResult(response.getStatusCode(), response.getResponseBodyAsString());
            } 
            @Override 
            public DataServiceResult handle(com.atlassian.sal.api.net.Response response) throws ResponseException {
                LOGGER.debug(String.format("Response from remote server: code=%d", response.getStatusCode()));
                return new DataServiceResult(response.getStatusCode(), response.getResponseBodyAsString());
            } 
        });
    }
    private ApplicationLinkRequestFactory createFactory(FieldSettings fs) throws DataServiceException {
        try {
            I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
            ApplicationLink link = fs == null
                    ? appLinkService.getPrimaryApplicationLink(ConfluenceApplicationType.class)
                    : appLinkService.getApplicationLink(new ApplicationId(fs.getConfluenceId()));
            
            if (link == null){
                throw new DataServiceException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    i18nHelper.getText("com.mesilat.confluence-field.err.linkNotFound", fs == null? "default": fs.getConfluenceId())
                );
            }
            
            return link.createAuthenticatedRequestFactory();
        } catch (TypeNotInstalledException ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
    private void throwIfNotNonceUsed(Throwable ex) throws Throwable {
        try {
            Method m = ex.getClass().getMethod("getOAuthProblem");
            String oauthProblem = (String)m.invoke(ex);
            if (!"nonce_used".equals(oauthProblem)){
                throw ex;
            }
        } catch(Throwable ignore){
            throw ex;
        }
    }

    @Inject
    public DataServiceImpl(ApplicationLinkService appLinkService, ActiveObjects ao){
        this.appLinkService = appLinkService;
        this.ao = ao;
    }
}