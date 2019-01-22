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
package com.github.ehrlichandreas.wiremock.servlet;

import static com.github.tomakehurst.wiremock.extension.ExtensionLoader.valueAssignableFrom;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;

import com.github.tomakehurst.wiremock.common.AsynchronousResponseSettings;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.HttpsSettings;
import com.github.tomakehurst.wiremock.common.JettySettings;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.common.ProxySettings;
import com.github.tomakehurst.wiremock.core.MappingsSaver;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.extension.ExtensionLoader;
import com.github.tomakehurst.wiremock.http.CaseInsensitiveKey;
import com.github.tomakehurst.wiremock.http.HttpServerFactory;
import com.github.tomakehurst.wiremock.http.ThreadPoolFactory;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.security.Authenticator;
import com.github.tomakehurst.wiremock.standalone.MappingsLoader;
import com.github.tomakehurst.wiremock.verification.notmatched.NotMatchedRenderer;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

public class WarConfiguration implements Options {

    private final MyWarConfiguration warConfiguration;
    private Map<String, Extension> extensions = newLinkedHashMap();

    public WarConfiguration(ServletContext servletContext) {
        warConfiguration = new MyWarConfiguration(servletContext);
    }

    public WarConfiguration extensions(String... classNames) {
        extensions.putAll(ExtensionLoader.load(classNames));
        return WarConfiguration.this;
    }

    public WarConfiguration extensions(Extension... extensionInstances) {
        extensions.putAll(ExtensionLoader.asMap(asList(extensionInstances)));
        return WarConfiguration.this;
    }

    public WarConfiguration extensions(Class<? extends Extension>... classes) {
        extensions.putAll(ExtensionLoader.load(classes));
        return WarConfiguration.this;
    }

    public int portNumber() {
        return warConfiguration.portNumber();
    }

    public HttpsSettings httpsSettings() {
        return warConfiguration.httpsSettings();
    }

    public JettySettings jettySettings() {
        return warConfiguration.jettySettings();
    }

    public int containerThreads() {
        return warConfiguration.containerThreads();
    }

    public boolean browserProxyingEnabled() {
        return warConfiguration.browserProxyingEnabled();
    }

    public ProxySettings proxyVia() {
        return warConfiguration.proxyVia();
    }

    public FileSource filesRoot() {
        return warConfiguration.filesRoot();
    }

    public MappingsLoader mappingsLoader() {
        return warConfiguration.mappingsLoader();
    }

    public MappingsSaver mappingsSaver() {
        return warConfiguration.mappingsSaver();
    }

    public Notifier notifier() {
        return warConfiguration.notifier();
    }

    public boolean requestJournalDisabled() {
        return warConfiguration.requestJournalDisabled();
    }

    public Optional<Integer> maxRequestJournalEntries() {
        return warConfiguration.maxRequestJournalEntries();
    }

    public String bindAddress() {
        return warConfiguration.bindAddress();
    }

    public List<CaseInsensitiveKey> matchingHeaders() {
        return warConfiguration.matchingHeaders();
    }

    public boolean shouldPreserveHostHeader() {
        return warConfiguration.shouldPreserveHostHeader();
    }

    public String proxyHostHeader() {
        return warConfiguration.proxyHostHeader();
    }

    public HttpServerFactory httpServerFactory() {
        return warConfiguration.httpServerFactory();
    }

    public ThreadPoolFactory threadPoolFactory() {
        return warConfiguration.threadPoolFactory();
    }

    public <T extends Extension> Map<String, T> extensionsOfType(Class<T> extensionType) {
        return warConfiguration.extensionsOfType(extensionType);
    }

    public WiremockNetworkTrafficListener networkTrafficListener() {
        return warConfiguration.networkTrafficListener();
    }

    public Authenticator getAdminAuthenticator() {
        return warConfiguration.getAdminAuthenticator();
    }

    public boolean getHttpsRequiredForAdminApi() {
        return warConfiguration.getHttpsRequiredForAdminApi();
    }

    public NotMatchedRenderer getNotMatchedRenderer() {
        return warConfiguration.getNotMatchedRenderer();
    }

    public AsynchronousResponseSettings getAsynchronousResponseSettings() {
        return warConfiguration.getAsynchronousResponseSettings();
    }

    private class MyWarConfiguration extends com.github.tomakehurst.wiremock.servlet.WarConfiguration {
        public MyWarConfiguration(ServletContext servletContext) {
            super(servletContext);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Extension> Map<String, T> extensionsOfType(final Class<T> extensionType) {
            return (Map<String, T>) Maps.filterEntries(extensions, valueAssignableFrom(extensionType));
        }
    }
}
