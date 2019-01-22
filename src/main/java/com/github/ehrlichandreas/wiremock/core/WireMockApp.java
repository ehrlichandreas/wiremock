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

import java.util.Map;

import com.github.ehrlichandreas.wiremock.http.RequestWrapper;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.core.Container;
import com.github.tomakehurst.wiremock.core.MappingsSaver;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.github.tomakehurst.wiremock.standalone.MappingsLoader;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.base.Optional;

public class WireMockApp extends com.github.tomakehurst.wiremock.core.WireMockApp {
    public WireMockApp(Options options, Container container) {
        super(options, container);
    }

    public WireMockApp(boolean browserProxyingEnabled, MappingsLoader defaultMappingsLoader, MappingsSaver mappingsSaver, boolean requestJournalDisabled, Optional<Integer> maxRequestJournalEntries, Map<String, ResponseDefinitionTransformer> transformers, Map<String, RequestMatcherExtension> requestMatchers, FileSource rootFileSource, Container container) {
        super(browserProxyingEnabled, defaultMappingsLoader, mappingsSaver, requestJournalDisabled, maxRequestJournalEntries, transformers, requestMatchers, rootFileSource, container);
    }

    @Override
    public ServeEvent serveStubFor(Request request) {
        final RequestWrapper requestWrapper = new RequestWrapper(request);
        return super.serveStubFor(requestWrapper);
    }
}
