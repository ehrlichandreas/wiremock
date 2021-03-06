/*
 * Copyright (C) 2019 Andreas Ehrlich
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.ehrlichandreas.wiremock.jetty9;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.core.WireMockApp.ADMIN_CONTEXT_ROOT;
import static java.util.concurrent.Executors.newScheduledThreadPool;

import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import javax.servlet.DispatcherType;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.NetworkTrafficListener;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkTrafficServerConnector;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import com.github.tomakehurst.wiremock.common.AsynchronousResponseSettings;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.HttpsSettings;
import com.github.tomakehurst.wiremock.common.JettySettings;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockApp;
import com.github.tomakehurst.wiremock.http.AdminRequestHandler;
import com.github.tomakehurst.wiremock.http.HttpServer;
import com.github.tomakehurst.wiremock.http.RequestHandler;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.jetty9.CustomizedSslContextFactory;
import com.github.tomakehurst.wiremock.jetty9.JettyFaultInjectorFactory;
import com.github.tomakehurst.wiremock.jetty9.NotFoundHandler;
import com.github.tomakehurst.wiremock.servlet.ContentTypeSettingFilter;
import com.github.tomakehurst.wiremock.servlet.FaultInjectorFactory;
import com.github.tomakehurst.wiremock.servlet.TrailingSlashFilter;
import com.github.tomakehurst.wiremock.servlet.WireMockHandlerDispatchingServlet;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

public class JettyHttpServer implements HttpServer {
    private static final String DEFAULT_MOCK_SERVICE_CONTEXT_PATH = "/";
    private static final String DEFAULT_ADMIN_SERVICE_CONTEXT_PATH = ADMIN_CONTEXT_ROOT;
    private String rootContext = com.github.ehrlichandreas.wiremock.core.Options.DEFAULT_ROOT_CONTEXT;
    private static final String FILES_URL_MATCH = String.format("/%s/*", WireMockApp.FILES_ROOT);
    private static final String[] GZIPPABLE_METHODS = new String[] { "POST", "PUT", "PATCH", "DELETE" };

    static {
        System.setProperty("org.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT", "300000");
    }

    private final Server jettyServer;
    private final ServerConnector httpConnector;
    private final ServerConnector httpsConnector;

    public JettyHttpServer(
            Options options,
            AdminRequestHandler adminRequestHandler,
            StubRequestHandler stubRequestHandler
    ) {
        if (options instanceof com.github.ehrlichandreas.wiremock.core.Options) {
            rootContext = ((com.github.ehrlichandreas.wiremock.core.Options)options).rootContext();
        }

        jettyServer = createServer(options);

        NetworkTrafficListenerAdapter networkTrafficListenerAdapter = new NetworkTrafficListenerAdapter(options.networkTrafficListener());
        httpConnector = createHttpConnector(
                options.bindAddress(),
                options.portNumber(),
                options.jettySettings(),
                networkTrafficListenerAdapter
        );
        jettyServer.addConnector(httpConnector);

        if (options.httpsSettings().enabled()) {
            httpsConnector = createHttpsConnector(
                    options.bindAddress(),
                    options.httpsSettings(),
                    options.jettySettings(),
                    networkTrafficListenerAdapter);
            jettyServer.addConnector(httpsConnector);
        } else {
            httpsConnector = null;
        }

        jettyServer.setHandler(createHandler(options, adminRequestHandler, stubRequestHandler));

        finalizeSetup(options);
    }

    protected HandlerCollection createHandler(Options options, AdminRequestHandler adminRequestHandler, StubRequestHandler stubRequestHandler) {
        Notifier notifier = options.notifier();
        ServletContextHandler adminContext = addAdminContext(
                adminRequestHandler,
                notifier
        );
        ServletContextHandler mockServiceContext = addMockServiceContext(
                stubRequestHandler,
                options.filesRoot(),
                options.getAsynchronousResponseSettings(),
                notifier
        );

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(ArrayUtils.addAll(extensionHandlers(), adminContext));

        addGZipHandler(mockServiceContext, handlers);

        return handlers;
    }

    private void addGZipHandler(ServletContextHandler mockServiceContext, HandlerCollection handlers) {
        Class<?> gzipHandlerClass = null;

        try {
            gzipHandlerClass = Class.forName("org.eclipse.jetty.servlets.gzip.GzipHandler");
        } catch (ClassNotFoundException e) {
            try {
                gzipHandlerClass = Class.forName("org.eclipse.jetty.server.handler.gzip.GzipHandler");
            } catch (ClassNotFoundException e1) {
                throwUnchecked(e1);
            }
        }

        try {
            HandlerWrapper gzipWrapper = (HandlerWrapper) gzipHandlerClass.newInstance();
            setGZippableMethods(gzipWrapper, gzipHandlerClass);
            gzipWrapper.setHandler(mockServiceContext);
            handlers.addHandler(gzipWrapper);
        } catch (Exception e) {
            throwUnchecked(e);
        }
    }

    private static void setGZippableMethods(HandlerWrapper gzipHandler, Class<?> gzipHandlerClass) {
        try {
            Method addIncludedMethods = gzipHandlerClass.getDeclaredMethod("addIncludedMethods", String[].class);
            addIncludedMethods.invoke(gzipHandler, new Object[] { GZIPPABLE_METHODS });
        } catch (Exception ignored) {}
    }

    protected void finalizeSetup(Options options) {
        if(!options.jettySettings().getStopTimeout().isPresent()) {
            jettyServer.setStopTimeout(0);
        }
    }

    protected Server createServer(Options options) {
        final Server server = new Server(options.threadPoolFactory().buildThreadPool(options));
        final JettySettings jettySettings = options.jettySettings();
        final Optional<Long> stopTimeout = jettySettings.getStopTimeout();
        if(stopTimeout.isPresent()) {
            server.setStopTimeout(stopTimeout.get());
        }
        return server;
    }

    /**
     * Extend only this method if you want to add additional handlers to Jetty.
     */
    protected Handler[] extensionHandlers() {
        return new Handler[]{};
    }

    @Override
    public void start() {
        try {
            jettyServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long timeout = System.currentTimeMillis() + 30000;
        while (!jettyServer.isStarted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // no-op
            }
            if (System.currentTimeMillis() > timeout) {
                throw new RuntimeException("Server took too long to start up.");
            }
        }
    }

    @Override
    public void stop() {
        try {
            jettyServer.stop();
            jettyServer.join();
        } catch (Exception e) {
            throwUnchecked(e);
        }
    }

    @Override
    public boolean isRunning() {
        return jettyServer.isRunning();
    }

    @Override
    public int port() {
        return httpConnector.getLocalPort();
    }

    @Override
    public int httpsPort() {
        return httpsConnector.getLocalPort();
    }

    protected long stopTimeout() {
        return jettyServer.getStopTimeout();
    }

    protected ServerConnector createHttpConnector(
            String bindAddress,
            int port,
            JettySettings jettySettings,
            NetworkTrafficListener listener) {

        HttpConfiguration httpConfig = createHttpConfig(jettySettings);

        ServerConnector connector = createServerConnector(
                bindAddress,
                jettySettings,
                port,
                listener,
                new HttpConnectionFactory(httpConfig)
        );

        return connector;
    }

    protected ServerConnector createHttpsConnector(
            String bindAddress,
            HttpsSettings httpsSettings,
            JettySettings jettySettings,
            NetworkTrafficListener listener) {

        //Added to support Android https communication.
        CustomizedSslContextFactory sslContextFactory = new CustomizedSslContextFactory();

        sslContextFactory.setKeyStorePath(httpsSettings.keyStorePath());
        sslContextFactory.setKeyManagerPassword(httpsSettings.keyStorePassword());
        sslContextFactory.setKeyStoreType(httpsSettings.keyStoreType());
        if (httpsSettings.hasTrustStore()) {
            sslContextFactory.setTrustStorePath(httpsSettings.trustStorePath());
            sslContextFactory.setTrustStorePassword(httpsSettings.trustStorePassword());
            sslContextFactory.setTrustStoreType(httpsSettings.trustStoreType());
        }
        sslContextFactory.setNeedClientAuth(httpsSettings.needClientAuth());

        HttpConfiguration httpConfig = createHttpConfig(jettySettings);
        httpConfig.addCustomizer(new SecureRequestCustomizer());

        final int port = httpsSettings.port();

        return createServerConnector(
                bindAddress,
                jettySettings,
                port,
                listener,
                new SslConnectionFactory(
                        sslContextFactory,
                        "http/1.1"
                ),
                new HttpConnectionFactory(httpConfig)
        );
    }

    protected HttpConfiguration createHttpConfig(JettySettings jettySettings) {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setRequestHeaderSize(
                jettySettings.getRequestHeaderSize().or(8192)
        );
        httpConfig.setSendDateHeader(false);
        return httpConfig;
    }

    protected ServerConnector createServerConnector(String bindAddress,
                                                  JettySettings jettySettings,
                                                  int port, NetworkTrafficListener listener,
                                                  ConnectionFactory... connectionFactories) {
        int acceptors = jettySettings.getAcceptors().or(2);
        NetworkTrafficServerConnector connector = new NetworkTrafficServerConnector(
                jettyServer,
                null,
                null,
                null,
                acceptors,
                2,
                connectionFactories
        );
        connector.setPort(port);

        connector.setStopTimeout(0);
        connector.getSelectorManager().setStopTimeout(0);

        connector.addNetworkTrafficListener(listener);

        setJettySettings(jettySettings, connector);

        connector.setHost(bindAddress);

        return connector;
    }

    private void setJettySettings(JettySettings jettySettings, ServerConnector connector) {
        if (jettySettings.getAcceptQueueSize().isPresent()) {
            connector.setAcceptQueueSize(jettySettings.getAcceptQueueSize().get());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ServletContextHandler addMockServiceContext(
            StubRequestHandler stubRequestHandler,
            FileSource fileSource,
            AsynchronousResponseSettings asynchronousResponseSettings,
            Notifier notifier
    ) {
        String rootContext = Optional.fromNullable(this.rootContext)
                .transform(new Function<Object, String>() {
                    @Override
                    public String apply(Object input) {
                        return String.valueOf(input);
                    }
                })
                .or(com.github.ehrlichandreas.wiremock.core.Options.DEFAULT_ROOT_CONTEXT).trim();
        String contextPath = DEFAULT_MOCK_SERVICE_CONTEXT_PATH;

        if (!rootContext.isEmpty()) {
            contextPath = rootContext;
        }

        ServletContextHandler mockServiceContext = new ServletContextHandler(jettyServer, contextPath);

        mockServiceContext.setInitParameter("org.eclipse.jetty.servlet.Default.maxCacheSize", "0");
        mockServiceContext.setInitParameter("org.eclipse.jetty.servlet.Default.resourceBase", fileSource.getPath());
        mockServiceContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

        mockServiceContext.addServlet(DefaultServlet.class, FILES_URL_MATCH);

        mockServiceContext.setAttribute(JettyFaultInjectorFactory.class.getName(), new JettyFaultInjectorFactory());
        mockServiceContext.setAttribute(StubRequestHandler.class.getName(), stubRequestHandler);
        mockServiceContext.setAttribute(Notifier.KEY, notifier);
        ServletHolder servletHolder = mockServiceContext.addServlet(WireMockHandlerDispatchingServlet.class, "/");
        servletHolder.setInitParameter(RequestHandler.HANDLER_CLASS_KEY, StubRequestHandler.class.getName());
        servletHolder.setInitParameter(FaultInjectorFactory.INJECTOR_CLASS_KEY, JettyFaultInjectorFactory.class.getName());
        servletHolder.setInitParameter(WireMockHandlerDispatchingServlet.SHOULD_FORWARD_TO_FILES_CONTEXT, "true");

        if (asynchronousResponseSettings.isEnabled()) {
            ScheduledExecutorService scheduledExecutorService = newScheduledThreadPool(asynchronousResponseSettings.getThreads());
            mockServiceContext.setAttribute(WireMockHandlerDispatchingServlet.ASYNCHRONOUS_RESPONSE_EXECUTOR, scheduledExecutorService);
        }

        MimeTypes mimeTypes = new MimeTypes();
        mimeTypes.addMimeMapping("json", "application/json");
        mimeTypes.addMimeMapping("html", "text/html");
        mimeTypes.addMimeMapping("xml", "application/xml");
        mimeTypes.addMimeMapping("txt", "text/plain");
        mockServiceContext.setMimeTypes(mimeTypes);
        mockServiceContext.setWelcomeFiles(new String[]{"index.json", "index.html", "index.xml", "index.txt"});

        mockServiceContext.setErrorHandler(new NotFoundHandler());

        mockServiceContext.addFilter(ContentTypeSettingFilter.class, FILES_URL_MATCH, EnumSet.of(DispatcherType.FORWARD));
        mockServiceContext.addFilter(TrailingSlashFilter.class, FILES_URL_MATCH, EnumSet.allOf(DispatcherType.class));

        return mockServiceContext;
    }

    private ServletContextHandler addAdminContext(
            AdminRequestHandler adminRequestHandler,
            Notifier notifier
    ) {
        String rootContext = Optional.fromNullable(this.rootContext)
                .transform(new Function<Object, String>() {
                    @Override
                    public String apply(Object input) {
                        return String.valueOf(input);
                    }
                })
                .or(com.github.ehrlichandreas.wiremock.core.Options.DEFAULT_ROOT_CONTEXT).trim();
        String contextPath = DEFAULT_ADMIN_SERVICE_CONTEXT_PATH;

        if (!rootContext.isEmpty()) {
            contextPath = rootContext + DEFAULT_ADMIN_SERVICE_CONTEXT_PATH;
        }

        ServletContextHandler adminContext = new ServletContextHandler(jettyServer, contextPath);

        adminContext.setInitParameter("org.eclipse.jetty.servlet.Default.maxCacheSize", "0");

        String javaVendor = System.getProperty("java.vendor");
        if (javaVendor != null && javaVendor.toLowerCase().contains("android")) {
            //Special case for Android, fixes IllegalArgumentException("resource assets not found."):
            //  The Android ClassLoader apparently does not resolve directories.
            //  Furthermore, lib assets will be merged into a single asset directory when a jar file is assimilated into an apk.
            //  As resources can be addressed like "assets/swagger-ui/index.html", a static path element will suffice.
            adminContext.setInitParameter("org.eclipse.jetty.servlet.Default.resourceBase", "assets");
        } else {
            adminContext.setInitParameter("org.eclipse.jetty.servlet.Default.resourceBase", Resources.getResource("assets").toString());
        }

        Resources.getResource("assets/swagger-ui/index.html");

        adminContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        adminContext.addServlet(DefaultServlet.class, "/swagger-ui/*");
        adminContext.addServlet(DefaultServlet.class, "/recorder/*");

        ServletHolder servletHolder = adminContext.addServlet(WireMockHandlerDispatchingServlet.class, "/");
        servletHolder.setInitParameter(RequestHandler.HANDLER_CLASS_KEY, AdminRequestHandler.class.getName());
        adminContext.setAttribute(AdminRequestHandler.class.getName(), adminRequestHandler);
        adminContext.setAttribute(Notifier.KEY, notifier);

        FilterHolder filterHolder = new FilterHolder(CrossOriginFilter.class);
        filterHolder.setInitParameters(ImmutableMap.of(
            "chainPreflight", "false",
            "allowedOrigins", "*",
            "allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin,Authorization",
            "allowedMethods", "OPTIONS,GET,POST,PUT,PATCH,DELETE"));

        adminContext.addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));

        return adminContext;
    }

    private static class NetworkTrafficListenerAdapter implements NetworkTrafficListener {
        private final WiremockNetworkTrafficListener wiremockNetworkTrafficListener;

        NetworkTrafficListenerAdapter(WiremockNetworkTrafficListener wiremockNetworkTrafficListener) {
            this.wiremockNetworkTrafficListener = wiremockNetworkTrafficListener;
        }

        @Override
        public void opened(Socket socket) {
            wiremockNetworkTrafficListener.opened(socket);
        }

        @Override
        public void incoming(Socket socket, ByteBuffer bytes) {
            wiremockNetworkTrafficListener.incoming(socket, bytes);
        }

        @Override
        public void outgoing(Socket socket, ByteBuffer bytes) {
            wiremockNetworkTrafficListener.outgoing(socket, bytes);
        }

        @Override
        public void closed(Socket socket) {
            wiremockNetworkTrafficListener.closed(socket);
        }
    }
}
