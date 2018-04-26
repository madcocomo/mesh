package com.gentics.mesh.core.verticle.webdav;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.verticle.node.XmlFieldHandler;
import com.gentics.mesh.parameter.UploadParameters;
import com.gentics.mesh.parameter.impl.UploadParametersImpl;
import com.gentics.mesh.rest.EndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class WebdavEndpoint extends AbstractProjectEndpoint {

    private static Logger log = LoggerFactory.getLogger(WebdavEndpoint.class);
    
    private static final String CONTENT_FIELD = "content";
    private static final String MOCK_LANGUAGE = "en";
    private static final String MOCK_VERSION = "99";

    private XmlFieldHandler xmlFieldHandler;

    public WebdavEndpoint() {
        super("webdav", null);
    }

    @Inject
    public WebdavEndpoint(BootstrapInitializer boot, XmlFieldHandler xmlFieldHandler) {
        super("webdav", boot);
        this.xmlFieldHandler = xmlFieldHandler;
    }

    @Override
    public String getDescription() {
        return "Endpoint which provides the webdav function";
    }

    @Override
    public void registerEndPoints() {
        getMethodHandler();
        optionsMethodHandler();
        putMethodHandler();
        otherMethodHandler();
    }

    private void getMethodHandler() {
        EndpointRoute route = createEndpoint();
        route.path("/:nodeUuid");
        route.addUriParameter("nodeUuid", "Node UUID", UUIDUtil.randomUUID());
        route.method(GET);
        route.description("Download the content of the XML field with the given name.");
        route.handler(rc -> {
            String uuid = rc.request().getParam("nodeUuid");
            // Note:Studio will send get xxx.xsd request after getting xml file
            if (!uuid.endsWith("xsd")) {
                xmlFieldHandler.handleGet(rc, uuid, CONTENT_FIELD);
            } else {
                // TODO So far, put a schema folder in server for studio to fetch.
                rc.response().sendFile("./schema/" + uuid);
            }
        });
    }

    private void optionsMethodHandler() {
        EndpointRoute route = createEndpoint();
        route.path("/");
        route.method(HttpMethod.OPTIONS);
        route.description("OPTIONS method for webdav.");
        route.handler(rc -> {
            rc.response().putHeader("DAV", "1,2");
            rc.response().putHeader("Allow", "OPTIONS, MKCOL, PUT, LOCK");
            rc.response().putHeader("MS-Author-Via", "DAV");
            rc.response().end();
        });
    }

    private void putMethodHandler() {
        EndpointRoute route = createEndpoint();
        route.path("/:nodeUuid");
        route.addUriParameter("nodeUuid", "Node UUID", UUIDUtil.randomUUID());
        route.addQueryParameters(UploadParametersImpl.class);
        route.method(HttpMethod.PUT);
        route.produces(APPLICATION_JSON);
        route.exampleRequest("(arbitrary XML binary stream)");
        route.exampleResponse(OK, nodeExamples.getNodeResponseWithAllFields(), "The response contains the updated node.");
        route.description("Update the specified XML field of the given name");
        route.blockingHandler(rc -> {
            String uuid = rc.request().getParam("nodeUuid");
            InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
            UploadParameters uploadParameters = ac.getUploadParameters();
            String language = uploadParameters.getLanguage();
            String version = uploadParameters.getVersion();
            // TODO So far, studio will not send lang and ver, using mock data.
            xmlFieldHandler.handlePost(rc, uuid, CONTENT_FIELD, MOCK_LANGUAGE, MOCK_VERSION);
        });
    }

    private void otherMethodHandler() {
        EndpointRoute route = createEndpoint();
        route.path("/:nodeUuid");
        route.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
        route.method(HttpMethod.OTHER);
        route.description("Other method for webdav.");
        route.handler(rc -> {
            String rawMethod = rc.request().rawMethod();
            if ("PROPFIND".equals(rawMethod)) {
                rc.response().setStatusCode(207);
                String mockPropfindResult = getMockPropfindResult();
                rc.response().end(mockPropfindResult);
            } else {
                rc.next();
            }
        });
    }

    private String getMockPropfindResult() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                "<multistatus xmlns=\"DAV:\" xmlns:DAV=\"DAV:\" xmlns:D=\"DAV:\">" +
                "<response>" +
                "<href>/</href>" +
                "<propstat>" +
                "<prop>" +
                "<getcontentlength/>" +
                "<getlastmodified/>" +
                "<getcontenttype/>" +
                "<resourcetype/>" +
                "<lockdiscovery/>" +
                "</prop>" +
                "<status>HTTP/1.1 200 OK</status>" +
                "</propstat>" +
                "</response>" +
                "</multistatus>";
    }

}
