/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.play.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import play.Invoker;
import play.Invoker.InvocationContext;
import play.Logger;
import play.Play;
import play.data.validation.Validation;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.mvc.Scope;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.server.ServletWrapper;
import play.templates.TemplateLoader;
import play.utils.Utils;
import play.vfs.VirtualFile;

/**
 * Basic fork of the ServerWrapper which only handlers the request. Intitialization / configuration
 * of the application is handled as part of the deployment process.
 */
public class PlayServlet extends HttpServlet {

    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String IF_NONE_MATCH = "If-None-Match";

    /**
     * Constant for accessing the underlying HttpServletRequest from Play's Request
     * in a Servlet based deployment.
     * <p>Sample usage:</p>
     * <p> {@code HttpServletRequest req = Request.current().args.get(ServletWrapper.SERVLET_REQ);}</p>
     */
    public static final String SERVLET_REQ = "__SERVLET_REQ";
    /**
     * Constant for accessing the underlying HttpServletResponse from Play's Request
     * in a Servlet based deployment.
     * <p>Sample usage:</p>
     * <p> {@code HttpServletResponse res = Request.current().args.get(ServletWrapper.SERVLET_RES);}</p>
     */
    public static final String SERVLET_RES = "__SERVLET_RES";

    @Override
    protected void service(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws ServletException, IOException {
        Http.Request request = null;
        try {
            Http.Response response = new Http.Response();
            response.out = new ByteArrayOutputStream();
            Http.Response.current.set(response);
            request = parseRequest(httpServletRequest);

            boolean raw = Play.pluginCollection.rawInvocation(request, response);
            if (raw) {
                copyResponse(Http.Request.current(), Http.Response.current(), httpServletRequest, httpServletResponse);
            } else {
                Invoker.invokeInThread(new ServletInvocation(request, response, httpServletRequest, httpServletResponse));
            }
        } catch (NotFound e) {
            if (Logger.isTraceEnabled()) {
                Logger.trace("ServletWrapper>service, NotFound: " + e);
            }
            serve404(httpServletRequest, httpServletResponse, e);
            return;
        } catch (RenderStatic e) {
            if (Logger.isTraceEnabled()) {
                Logger.trace("ServletWrapper>service, RenderStatic: " + e);
            }
            serveStatic(httpServletResponse, httpServletRequest, e);
            return;
        } catch (Throwable e) {
            throw new ServletException(e);
        } finally {
            Request.current.remove();
            Response.current.remove();
            Scope.Session.current.remove();
            Scope.Params.current.remove();
            Scope.Flash.current.remove();
            Scope.RenderArgs.current.remove();
            Scope.RouteArgs.current.remove();
        }
    }

    public void serveStatic(HttpServletResponse servletResponse, HttpServletRequest servletRequest, RenderStatic renderStatic) throws IOException {

        VirtualFile file = Play.getVirtualFile(renderStatic.file);
        if (file == null || file.isDirectory() || !file.exists()) {
            serve404(servletRequest, servletResponse, new NotFound("The file " + renderStatic.file + " does not exist"));
        } else {
            servletResponse.setContentType(MimeTypes.getContentType(file.getName()));
            boolean raw = Play.pluginCollection.serveStatic(file, Http.Request.current(), Http.Response.current());
            if (raw) {
                copyResponse(Http.Request.current(), Http.Response.current(), servletRequest, servletResponse);
            } else {
                if (Play.mode == Play.Mode.DEV) {
                    servletResponse.setHeader("Cache-Control", "no-cache");
                    servletResponse.setHeader("Content-Length", String.valueOf(file.length()));
                    if (!servletRequest.getMethod().equals("HEAD")) {
                        copyStream(servletResponse, file.inputstream());
                    } else {
                        copyStream(servletResponse, new ByteArrayInputStream(new byte[0]));
                    }
                } else {
                    long last = file.lastModified();
                    String etag = "\"" + last + "-" + file.hashCode() + "\"";
                    if (!isModified(etag, last, servletRequest)) {
                        servletResponse.setHeader("Etag", etag);
                        servletResponse.setStatus(304);
                    } else {
                        servletResponse.setHeader("Last-Modified", Utils.getHttpDateFormatter().format(new Date(last)));
                        servletResponse.setHeader("Cache-Control", "max-age=" + Play.configuration.getProperty("http.cacheControl", "3600"));
                        servletResponse.setHeader("Etag", etag);
                        copyStream(servletResponse, file.inputstream());
                    }
                }
            }
        }
    }

    public static boolean isModified(String etag, long last,
            HttpServletRequest request) {
        // See section 14.26 in rfc 2616 http://www.faqs.org/rfcs/rfc2616.html
        String browserEtag = request.getHeader(IF_NONE_MATCH);
        String dateString = request.getHeader(IF_MODIFIED_SINCE);
        if (browserEtag != null) {
            boolean etagMatches = browserEtag.equals(etag);
            if (!etagMatches) {
                return true;
            }
            if (dateString != null) {
                return !isValidTimeStamp(last, dateString);
            }
            return false;
        } else {
            if (dateString != null) {
                return !isValidTimeStamp(last, dateString);
            } else {
                return true;
            }
        }
    }

    private static boolean isValidTimeStamp(long last, String dateString) {
        try {
            long browserDate = Utils.getHttpDateFormatter().parse(dateString).getTime();
            return browserDate >= last;
        } catch (ParseException e) {
            Logger.error("Can't parse date", e);
            return false;
        }
    }

    public static Request parseRequest(HttpServletRequest httpServletRequest) throws Exception {

        URI uri = new URI(httpServletRequest.getRequestURI());
        String method = httpServletRequest.getMethod().intern();
        String path = uri.getRawPath();
        if (path != null ) {
            path = URLDecoder.decode(path, "utf-8");
        }
        String querystring = httpServletRequest.getQueryString() == null ? "" : httpServletRequest.getQueryString();

        if (Logger.isTraceEnabled()) {
            Logger.trace("httpServletRequest.getContextPath(): " + httpServletRequest.getContextPath());
            Logger.trace("request.path: " + path + ", request.querystring: " + querystring);
        }

        String contentType = null;
        if (httpServletRequest.getHeader("Content-Type") != null) {
            contentType = httpServletRequest.getHeader("Content-Type").split(";")[0].trim().toLowerCase().intern();
        } else {
            contentType = "text/html".intern();
        }

        if (httpServletRequest.getHeader("X-HTTP-Method-Override") != null) {
            method = httpServletRequest.getHeader("X-HTTP-Method-Override").intern();
        }

        InputStream body = httpServletRequest.getInputStream();
        boolean secure = httpServletRequest.isSecure();

        String url = uri.toString() + (httpServletRequest.getQueryString() == null ? "" : "?" + httpServletRequest.getQueryString());
        String host = httpServletRequest.getHeader("host");
        int port = 0;
        String domain = null;
        if (host.contains(":")) {
            port = Integer.parseInt(host.split(":")[1]);
            domain = host.split(":")[0];
        } else {
            port = 80;
            domain = host;
        }

        String remoteAddress = httpServletRequest.getRemoteAddr();

        boolean isLoopback = host.matches("^127\\.0\\.0\\.1:?[0-9]*$");


        final Request request = Request.createRequest(
                remoteAddress,
                method,
                path,
                querystring,
                contentType,
                body,
                url,
                host,
                isLoopback,
                port,
                domain,
                secure,
                getHeaders(httpServletRequest),
                getCookies(httpServletRequest));


        Request.current.set(request);
        Router.routeOnlyStatic(request);

        return request;
    }

    protected static Map<String, Http.Header> getHeaders(HttpServletRequest httpServletRequest) {
        Map<String, Http.Header> headers = new HashMap<String, Http.Header>(16);

        Enumeration headersNames = httpServletRequest.getHeaderNames();
        while (headersNames.hasMoreElements()) {
            Http.Header hd = new Http.Header();
            hd.name = (String) headersNames.nextElement();
            hd.values = new ArrayList<String>();
            Enumeration enumValues = httpServletRequest.getHeaders(hd.name);
            while (enumValues.hasMoreElements()) {
                String value = (String) enumValues.nextElement();
                hd.values.add(value);
            }
            headers.put(hd.name.toLowerCase(), hd);
        }

        return headers;
    }

    protected static Map<String, Http.Cookie> getCookies(HttpServletRequest httpServletRequest) {
        Map<String, Http.Cookie> cookies = new HashMap<String, Http.Cookie>(16);
        javax.servlet.http.Cookie[] cookiesViaServlet = httpServletRequest.getCookies();
        if (cookiesViaServlet != null) {
            for (javax.servlet.http.Cookie cookie : cookiesViaServlet) {
                Http.Cookie playCookie = new Http.Cookie();
                playCookie.name = cookie.getName();
                playCookie.path = cookie.getPath();
                playCookie.domain = cookie.getDomain();
                playCookie.secure = cookie.getSecure();
                playCookie.value = cookie.getValue();
                playCookie.maxAge = cookie.getMaxAge();
                cookies.put(playCookie.name, playCookie);
            }
        }

        return cookies;
    }

    public void serve404(HttpServletRequest servletRequest, HttpServletResponse servletResponse, NotFound e) {
        Logger.warn("404 -> %s %s (%s)", servletRequest.getMethod(), servletRequest.getRequestURI(), e.getMessage());
        servletResponse.setStatus(404);
        servletResponse.setContentType("text/html");
        Map<String, Object> binding = new HashMap<String, Object>();
        binding.put("result", e);
        binding.put("session", Scope.Session.current());
        binding.put("request", Http.Request.current());
        binding.put("flash", Scope.Flash.current());
        binding.put("params", Scope.Params.current());
        binding.put("play", new Play());
        try {
            binding.put("errors", Validation.errors());
        } catch (Exception ex) {
            //
        }
        String format = Request.current().format;
        servletResponse.setStatus(404);
        // Do we have an ajax request? If we have then we want to display some text even if it is html that is requested
        if ("XMLHttpRequest".equals(servletRequest.getHeader("X-Requested-With")) && (format == null || format.equals("html"))) {
            format = "txt";
        }
        if (format == null) {
            format = "txt";
        }
        servletResponse.setContentType(MimeTypes.getContentType("404." + format, "text/plain"));
        String errorHtml = TemplateLoader.load("errors/404." + format).render(binding);
        try {
            servletResponse.getOutputStream().write(errorHtml.getBytes(Response.current().encoding));
        } catch (Exception fex) {
            Logger.error(fex, "(encoding ?)");
        }
    }

    public void serve500(Exception e, HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> binding = new HashMap<String, Object>();
            if (!(e instanceof PlayException)) {
                e = new play.exceptions.UnexpectedException(e);
            }
            // Flush some cookies
            try {
                Map<String, Http.Cookie> cookies = Response.current().cookies;
                for (Http.Cookie cookie : cookies.values()) {
                    if (cookie.sendOnError) {
                        Cookie c = new Cookie(cookie.name, cookie.value);
                        c.setSecure(cookie.secure);
                        c.setPath(cookie.path);
                        if (cookie.domain != null) {
                            c.setDomain(cookie.domain);
                        }
                        response.addCookie(c);
                    }
                }
            } catch (Exception exx) {
                // humm ?
            }
            binding.put("exception", e);
            binding.put("session", Scope.Session.current());
            binding.put("request", Http.Request.current());
            binding.put("flash", Scope.Flash.current());
            binding.put("params", Scope.Params.current());
            binding.put("play", new Play());
            try {
                binding.put("errors", Validation.errors());
            } catch (Exception ex) {
                //
            }
            response.setStatus(500);
            String format = "html";
            if (Request.current() != null) {
                format = Request.current().format;
            }
            // Do we have an ajax request? If we have then we want to display some text even if it is html that is requested
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With")) && (format == null || format.equals("html"))) {
                format = "txt";
            }
            if (format == null) {
                format = "txt";
            }
            response.setContentType(MimeTypes.getContentType("500." + format, "text/plain"));
            try {
                String errorHtml = TemplateLoader.load("errors/500." + format).render(binding);
                response.getOutputStream().write(errorHtml.getBytes(Response.current().encoding));
                Logger.error(e, "Internal Server Error (500)");
            } catch (Throwable ex) {
                Logger.error(e, "Internal Server Error (500)");
                Logger.error(ex, "Error during the 500 response generation");
                throw ex;
            }
        } catch (Throwable exxx) {
            if (exxx instanceof RuntimeException) {
                throw (RuntimeException) exxx;
            }
            throw new RuntimeException(exxx);
        }
    }

    public void copyResponse(Request request, Response response, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String encoding = Response.current().encoding;
        if (response.contentType != null) {
            servletResponse.setHeader("Content-Type", response.contentType + (response.contentType.startsWith("text/") ? "; charset="+encoding : ""));
        } else {
            servletResponse.setHeader("Content-Type", "text/plain;charset="+encoding);
        }

        servletResponse.setStatus(response.status);
        if (!response.headers.containsKey("cache-control")) {
            servletResponse.setHeader("Cache-Control", "no-cache");
        }
        Map<String, Http.Header> headers = response.headers;
        for (Map.Entry<String, Http.Header> entry : headers.entrySet()) {
            Http.Header hd = entry.getValue();
            String key = entry.getKey();
            for (String value : hd.values) {
                servletResponse.setHeader(key, value);
            }
        }

        Map<String, Http.Cookie> cookies = response.cookies;
        for (Http.Cookie cookie : cookies.values()) {
            Cookie c = new Cookie(cookie.name, cookie.value);
            c.setSecure(cookie.secure);
            c.setPath(cookie.path);
            if (cookie.domain != null) {
                c.setDomain(cookie.domain);
            }
            if (cookie.maxAge != null) {
                c.setMaxAge(cookie.maxAge);
            }
            servletResponse.addCookie(c);
        }

        // Content

        response.out.flush();
        if (response.direct != null && response.direct instanceof File) {
            File file = (File) response.direct;
            servletResponse.setHeader("Content-Length", String.valueOf(file.length()));
            if (!request.method.equals("HEAD")) {
                copyStream(servletResponse, VirtualFile.open(file).inputstream());
            } else {
                copyStream(servletResponse, new ByteArrayInputStream(new byte[0]));
            }
        } else if (response.direct != null && response.direct instanceof InputStream) {
            copyStream(servletResponse, (InputStream) response.direct);
        } else {
            byte[] content = response.out.toByteArray();
            servletResponse.setHeader("Content-Length", String.valueOf(content.length));
            if (!request.method.equals("HEAD")) {
                servletResponse.getOutputStream().write(content);
            } else {
                copyStream(servletResponse, new ByteArrayInputStream(new byte[0]));
            }
        }

    }

    private void copyStream(HttpServletResponse servletResponse, InputStream is) throws IOException {
        OutputStream os = servletResponse.getOutputStream();
        byte[] buffer = new byte[8096];
        int read = 0;
        while ((read = is.read(buffer)) > 0) {
            os.write(buffer, 0, read);
        }
        os.flush();
        is.close();
    }

    public class ServletInvocation extends Invoker.DirectInvocation {

        private Request request;
        private Response response;
        private HttpServletRequest httpServletRequest;
        private HttpServletResponse httpServletResponse;

        public ServletInvocation(Request request, Response response, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
            this.httpServletRequest = httpServletRequest;
            this.httpServletResponse = httpServletResponse;
            this.request = request;
            this.response = response;
            request.args.put(ServletWrapper.SERVLET_REQ, httpServletRequest);
            request.args.put(ServletWrapper.SERVLET_RES, httpServletResponse);
        }

        @Override
        public boolean init() {
            try {
                return super.init();
            } catch (NotFound e) {
                serve404(httpServletRequest, httpServletResponse, e);
                return false;
            } catch (RenderStatic r) {
                try {
                    serveStatic(httpServletResponse, httpServletRequest, r);
                } catch (IOException e) {
                    throw new UnexpectedException(e);
                }
                return false;
            }
        }

        @Override
        public void run() {
            try {
                super.run();
            } catch (Exception e) {
                serve500(e, httpServletRequest, httpServletResponse);
                return;
            }
        }

        @Override
        public void execute() throws Exception {
            ActionInvoker.invoke(request, response);
            copyResponse(request, response, httpServletRequest, httpServletResponse);
        }

        @Override
        public InvocationContext getInvocationContext() {
            ActionInvoker.resolve(request, response);
            return new InvocationContext(Http.invocationType,
                    request.invokedMethod.getAnnotations(),
                    request.invokedMethod.getDeclaringClass().getAnnotations());
        }
    }
}
