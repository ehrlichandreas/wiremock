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
package com.github.ehrlichandreas.wiremock.core;

import java.util.List;
import java.util.Map;

import com.github.ehrlichandreas.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.AsynchronousResponseSettings;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.HttpsSettings;
import com.github.tomakehurst.wiremock.common.JettySettings;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.common.ProxySettings;
import com.github.tomakehurst.wiremock.core.MappingsSaver;
import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.http.CaseInsensitiveKey;
import com.github.tomakehurst.wiremock.http.HttpServerFactory;
import com.github.tomakehurst.wiremock.http.ThreadPoolFactory;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.security.Authenticator;
import com.github.tomakehurst.wiremock.standalone.MappingsLoader;
import com.github.tomakehurst.wiremock.standalone.MappingsSource;
import com.github.tomakehurst.wiremock.verification.notmatched.NotMatchedRenderer;
import com.google.common.base.Function;
import com.google.common.base.Optional;

public final class WireMockConfiguration implements Options {

    private com.github.tomakehurst.wiremock.core.WireMockConfiguration wireMockConfiguration;
    private String rootContext;

    public WireMockConfiguration() {
        this(new com.github.tomakehurst.wiremock.core.WireMockConfiguration());
    }

    public WireMockConfiguration(com.github.tomakehurst.wiremock.core.WireMockConfiguration wireMockConfiguration) {
        this(wireMockConfiguration, DEFAULT_ROOT_CONTEXT);
    }

    public WireMockConfiguration(com.github.tomakehurst.wiremock.core.WireMockConfiguration wireMockConfiguration, String rootContext) {
        this.wireMockConfiguration = wireMockConfiguration;
        this.rootContext = rootContext;

        if (wireMockConfiguration instanceof Options) {
            withRootContext(((Options) wireMockConfiguration).rootContext());
        }
    }

    public static WireMockConfiguration wireMockConfig() {
        return new WireMockConfiguration();
    }

    public static WireMockConfiguration options() {
        return wireMockConfig();
    }

    public static Options from(com.github.tomakehurst.wiremock.core.Options options) {
        final WireMockConfiguration wireMockConfiguration = wireMockConfig();

        if (options instanceof Options) {
            wireMockConfiguration.withRootContext(((Options) options).rootContext());
        }

        wireMockConfiguration.port(options.portNumber());
        wireMockConfiguration.bindAddress(options.bindAddress());
        wireMockConfiguration.containerThreads(options.containerThreads());

        final HttpsSettings httpsSettings = options.httpsSettings();
        wireMockConfiguration.httpsPort(httpsSettings.port());
        wireMockConfiguration.keystorePath(httpsSettings.keyStorePath());
        wireMockConfiguration.keystorePassword(httpsSettings.keyStorePassword());
        wireMockConfiguration.keystoreType(httpsSettings.keyStoreType());
        wireMockConfiguration.trustStorePath(httpsSettings.trustStorePath());
        wireMockConfiguration.trustStorePassword(httpsSettings.trustStorePassword());
        wireMockConfiguration.trustStoreType(httpsSettings.trustStoreType());
        wireMockConfiguration.needClientAuth(httpsSettings.needClientAuth());

        wireMockConfiguration.enableBrowserProxying(options.browserProxyingEnabled());
        wireMockConfiguration.proxyVia(options.proxyVia());
        wireMockConfiguration.fileSource(options.filesRoot());
        wireMockConfiguration.mappingSource((MappingsSource) options.mappingsLoader());
        wireMockConfiguration.notifier(options.notifier());

        if (options.requestJournalDisabled()) {
            wireMockConfiguration.disableRequestJournal();
        }

        wireMockConfiguration.maxRequestJournalEntries(options.maxRequestJournalEntries());
        wireMockConfiguration.matchingHeaders().addAll(options.matchingHeaders());
        wireMockConfiguration.preserveHostHeader(options.shouldPreserveHostHeader());
        wireMockConfiguration.proxyHostHeader(options.proxyHostHeader());
        wireMockConfiguration.httpServerFactory(options.httpServerFactory());
        wireMockConfiguration.threadPoolFactory(options.threadPoolFactory());

        final JettySettings jettySettings = options.jettySettings();
        wireMockConfiguration.jettyAcceptors(jettySettings.getAcceptors().orNull());
        wireMockConfiguration.jettyAcceptQueueSize(jettySettings.getAcceptQueueSize().orNull());
        wireMockConfiguration.jettyHeaderBufferSize(jettySettings.getRequestHeaderSize().orNull());
        wireMockConfiguration.jettyStopTimeout(jettySettings.getStopTimeout().orNull());

        wireMockConfiguration.extensions(options.extensionsOfType(Extension.class).values().toArray(new Extension[]{}));
        wireMockConfiguration.networkTrafficListener(options.networkTrafficListener());
        wireMockConfiguration.adminAuthenticator(options.getAdminAuthenticator());

        if (options.getHttpsRequiredForAdminApi()) {
            wireMockConfiguration.requireHttpsForAdminApi();
        }

        wireMockConfiguration.notMatchedRenderer(options.getNotMatchedRenderer());

        final AsynchronousResponseSettings asynchronousResponseSettings = options.getAsynchronousResponseSettings();
        wireMockConfiguration.asynchronousResponseEnabled(asynchronousResponseSettings.isEnabled());
        wireMockConfiguration.asynchronousResponseThreads(asynchronousResponseSettings.getThreads());

        return wireMockConfiguration;
    }

    public WireMockConfiguration withRootContext(String rootContext) {
        this.rootContext = rootContext;
        return this;
    }

    @Override
    public String rootContext() {
        return Optional.fromNullable(rootContext).transform(new Function<String, String>() {
            @Override
            public String apply(String input) {
                if (null == input) {
                    return null;
                }

                return input.trim();
            }
        }).or(DEFAULT_ROOT_CONTEXT);
    }

    public WireMockConfiguration port(int portNumber) {
        wireMockConfiguration.port(portNumber);
        return this;
    }

    public WireMockConfiguration dynamicPort() {
        wireMockConfiguration.dynamicPort();
        return this;
    }

    public WireMockConfiguration httpsPort(Integer httpsPort) {
        wireMockConfiguration.httpsPort(httpsPort);
        return this;
    }

    public WireMockConfiguration dynamicHttpsPort() {
        wireMockConfiguration.dynamicHttpsPort();
        return this;
    }

    public WireMockConfiguration containerThreads(Integer containerThreads) {
        wireMockConfiguration.containerThreads(containerThreads);
        return this;
    }

    public WireMockConfiguration jettyAcceptors(Integer jettyAcceptors) {
        wireMockConfiguration.jettyAcceptors(jettyAcceptors);
        return this;
    }

    public WireMockConfiguration jettyAcceptQueueSize(Integer jettyAcceptQueueSize) {
        wireMockConfiguration.jettyAcceptQueueSize(jettyAcceptQueueSize);
        return this;
    }

    public WireMockConfiguration jettyHeaderBufferSize(Integer jettyHeaderBufferSize) {
        wireMockConfiguration.jettyHeaderBufferSize(jettyHeaderBufferSize);
        return this;
    }

    public WireMockConfiguration jettyStopTimeout(Long jettyStopTimeout) {
        wireMockConfiguration.jettyStopTimeout(jettyStopTimeout);
        return this;
    }

    public WireMockConfiguration keystorePath(String path) {
        wireMockConfiguration.keystorePath(path);
        return this;
    }

    public WireMockConfiguration keystorePassword(String keyStorePassword) {
        wireMockConfiguration.keystorePassword(keyStorePassword);
        return this;
    }

    public WireMockConfiguration keystoreType(String keyStoreType) {
        wireMockConfiguration.keystoreType(keyStoreType);
        return this;
    }

    public WireMockConfiguration trustStorePath(String truststorePath) {
        wireMockConfiguration.trustStorePath(truststorePath);
        return this;
    }

    public WireMockConfiguration trustStorePassword(String trustStorePassword) {
        wireMockConfiguration.trustStorePassword(trustStorePassword);
        return this;
    }

    public WireMockConfiguration trustStoreType(String trustStoreType) {
        wireMockConfiguration.trustStoreType(trustStoreType);
        return this;
    }

    public WireMockConfiguration needClientAuth(boolean needClientAuth) {
        wireMockConfiguration.needClientAuth(needClientAuth);
        return this;
    }

    public WireMockConfiguration enableBrowserProxying(boolean enabled) {
        wireMockConfiguration.enableBrowserProxying(enabled);
        return this;
    }

    public WireMockConfiguration proxyVia(String host, int port) {
        wireMockConfiguration.proxyVia(host, port);
        return this;
    }

    public WireMockConfiguration proxyVia(ProxySettings proxySettings) {
        wireMockConfiguration.proxyVia(proxySettings);
        return this;
    }

    public WireMockConfiguration withRootDirectory(String path) {
        wireMockConfiguration.withRootDirectory(path);
        return this;
    }

    public WireMockConfiguration usingFilesUnderDirectory(String path) {
        wireMockConfiguration.usingFilesUnderDirectory(path);
        return this;
    }

    public WireMockConfiguration usingFilesUnderClasspath(String path) {
        fileSource(new ClasspathFileSource(path));
        return this;
    }

    public WireMockConfiguration fileSource(FileSource fileSource) {
        wireMockConfiguration.fileSource(fileSource);
        return this;
    }

    public WireMockConfiguration mappingSource(MappingsSource mappingsSource) {
        wireMockConfiguration.mappingSource(mappingsSource);
        return this;
    }

    public WireMockConfiguration notifier(Notifier notifier) {
        wireMockConfiguration.notifier(notifier);
        return this;
    }

    public WireMockConfiguration bindAddress(String bindAddress) {
        wireMockConfiguration.bindAddress(bindAddress);
        return this;
    }

    public WireMockConfiguration disableRequestJournal() {
        wireMockConfiguration.disableRequestJournal();
        return this;
    }

    public WireMockConfiguration maxRequestJournalEntries(Optional<Integer> maxRequestJournalEntries) {
        wireMockConfiguration.maxRequestJournalEntries(maxRequestJournalEntries);
        return this;
    }

    public WireMockConfiguration maxRequestJournalEntries(int maxRequestJournalEntries) {
        wireMockConfiguration.maxRequestJournalEntries(maxRequestJournalEntries);
        return this;
    }

    public WireMockConfiguration recordRequestHeadersForMatching(List<String> headers) {
        wireMockConfiguration.recordRequestHeadersForMatching(headers);
        return this;
    }

    public WireMockConfiguration preserveHostHeader(boolean preserveHostHeader) {
        wireMockConfiguration.preserveHostHeader(preserveHostHeader);
        return this;
    }

    public WireMockConfiguration proxyHostHeader(String hostHeaderValue) {
        wireMockConfiguration.proxyHostHeader(hostHeaderValue);
        return this;
    }

    public WireMockConfiguration extensions(String... classNames) {
        wireMockConfiguration.extensions(classNames);
        return this;
    }

    public WireMockConfiguration extensions(Extension... extensionInstances) {
        wireMockConfiguration.extensions(extensionInstances);
        return this;
    }

    public WireMockConfiguration extensions(Class<? extends Extension>... classes) {
        wireMockConfiguration.extensions(classes);
        return this;
    }

    public WireMockConfiguration httpServerFactory(HttpServerFactory serverFactory) {
        wireMockConfiguration.httpServerFactory(serverFactory);
        return this;
    }

    public WireMockConfiguration threadPoolFactory(ThreadPoolFactory threadPoolFactory) {
        wireMockConfiguration.threadPoolFactory(threadPoolFactory);
        return this;
    }

    public WireMockConfiguration networkTrafficListener(WiremockNetworkTrafficListener networkTrafficListener) {
        wireMockConfiguration.networkTrafficListener(networkTrafficListener);
        return this;
    }

    public WireMockConfiguration adminAuthenticator(Authenticator authenticator) {
        wireMockConfiguration.adminAuthenticator(authenticator);
        return this;
    }

    public WireMockConfiguration basicAdminAuthenticator(String username, String password) {
        wireMockConfiguration.basicAdminAuthenticator(username, password);
        return this;
    }

    public WireMockConfiguration requireHttpsForAdminApi() {
        wireMockConfiguration.requireHttpsForAdminApi();
        return this;
    }

    public WireMockConfiguration notMatchedRenderer(NotMatchedRenderer notMatchedRenderer) {
        wireMockConfiguration.notMatchedRenderer(notMatchedRenderer);
        return this;
    }

    public WireMockConfiguration asynchronousResponseEnabled(boolean asynchronousResponseEnabled) {
        wireMockConfiguration.asynchronousResponseEnabled(asynchronousResponseEnabled);
        return this;
    }

    public WireMockConfiguration asynchronousResponseThreads(int asynchronousResponseThreads) {
        wireMockConfiguration.asynchronousResponseThreads(asynchronousResponseThreads);
        return this;
    }

    public int portNumber() {
        return wireMockConfiguration.portNumber();
    }

    public int containerThreads() {
        return wireMockConfiguration.containerThreads();
    }

    public HttpsSettings httpsSettings() {
        return wireMockConfiguration.httpsSettings();
    }

    public JettySettings jettySettings() {
        return wireMockConfiguration.jettySettings();
    }

    public boolean browserProxyingEnabled() {
        return wireMockConfiguration.browserProxyingEnabled();
    }

    public ProxySettings proxyVia() {
        return wireMockConfiguration.proxyVia();
    }

    public FileSource filesRoot() {
        return wireMockConfiguration.filesRoot();
    }

    public MappingsLoader mappingsLoader() {
        return wireMockConfiguration.mappingsLoader();
    }

    public MappingsSaver mappingsSaver() {
        return wireMockConfiguration.mappingsSaver();
    }

    public Notifier notifier() {
        return wireMockConfiguration.notifier();
    }

    public boolean requestJournalDisabled() {
        return wireMockConfiguration.requestJournalDisabled();
    }

    public Optional<Integer> maxRequestJournalEntries() {
        return wireMockConfiguration.maxRequestJournalEntries();
    }

    public String bindAddress() {
        return wireMockConfiguration.bindAddress();
    }

    public List<CaseInsensitiveKey> matchingHeaders() {
        return wireMockConfiguration.matchingHeaders();
    }

    public HttpServerFactory httpServerFactory() {
        return wireMockConfiguration.httpServerFactory();
    }

    public ThreadPoolFactory threadPoolFactory() {
        return wireMockConfiguration.threadPoolFactory();
    }

    public boolean shouldPreserveHostHeader() {
        return wireMockConfiguration.shouldPreserveHostHeader();
    }

    public String proxyHostHeader() {
        return wireMockConfiguration.proxyHostHeader();
    }

    public <T extends Extension> Map<String, T> extensionsOfType(Class<T> extensionType) {
        return wireMockConfiguration.extensionsOfType(extensionType);
    }

    public WiremockNetworkTrafficListener networkTrafficListener() {
        return wireMockConfiguration.networkTrafficListener();
    }

    public Authenticator getAdminAuthenticator() {
        return wireMockConfiguration.getAdminAuthenticator();
    }

    public boolean getHttpsRequiredForAdminApi() {
        return wireMockConfiguration.getHttpsRequiredForAdminApi();
    }

    public NotMatchedRenderer getNotMatchedRenderer() {
        return wireMockConfiguration.getNotMatchedRenderer();
    }

    public AsynchronousResponseSettings getAsynchronousResponseSettings() {
        return wireMockConfiguration.getAsynchronousResponseSettings();
    }
}
