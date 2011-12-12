package org.dcache.webdav;

import com.bradmcevoy.http.ServletRequest;
import com.bradmcevoy.http.ServletResponse;
import com.bradmcevoy.http.HttpManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;

import org.dcache.cells.CellMessageSender;
import org.dcache.util.Transfer;

import dmg.cells.nucleus.CDC;
import dmg.cells.nucleus.CellInfo;
import dmg.cells.nucleus.CellEndpoint;

/**
 * A Jetty handler that wraps a Milton HttpManager. Makes it possible
 * to embed Milton in Jetty without using the Milton servlet.
 */
public class MiltonHandler
    extends AbstractHandler
    implements CellMessageSender
{
    private HttpManager _httpManager;
    private String _cellName;
    private String _domainName;

    public void setHttpManager(HttpManager httpManager)
    {
        _httpManager = httpManager;
    }

    public void setCellEndpoint(CellEndpoint endpoint)
    {
        CellInfo info = endpoint.getCellInfo();
        _cellName = info.getCellName();
        _domainName = info.getDomainName();
    }

    public void handle(String target, Request baseRequest,
                       HttpServletRequest request,HttpServletResponse response)
        throws IOException, ServletException
    {
        CDC cdc = CDC.reset(_cellName, _domainName);
        Transfer.initSession();
        try {
            ServletRequest req = new ServletRequest(request) {
                    @Override
                    public String getExpectHeader() {
                        /* Jetty deals with expect headers, so no
                         * reason for Milton to worry about them.
                         */
                        return "";
                    }
                };
            ServletResponse resp = new ServletResponse(response) {
                    @Override
                    public void setContentLengthHeader(Long length) {
                        /* If the length is unknown, Milton insists on
                         * setting an empty string value if the
                         * Content-Length header.
                         *
                         * Instead we want the Content-Length header
                         * to be skipped and rely on Jetty using
                         * chunked encoding.
                         */
                        if (length != null) {
                            super.setContentLengthHeader(length);
                        }
                    }
                };
            baseRequest.setHandled(true);
            _httpManager.process(req, resp);
            response.getOutputStream().flush();
            response.flushBuffer();
        } finally {
            cdc.restore();
        }
    }
}