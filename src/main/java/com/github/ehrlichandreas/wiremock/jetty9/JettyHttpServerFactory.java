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

import com.github.ehrlichandreas.wiremock.core.Options;
import com.github.ehrlichandreas.wiremock.core.WireMockConfiguration;
import com.github.ehrlichandreas.wiremock.http.HttpServerFactory;
import com.github.tomakehurst.wiremock.http.AdminRequestHandler;
import com.github.tomakehurst.wiremock.http.HttpServer;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;

public class JettyHttpServerFactory implements HttpServerFactory {

    @Override
    public HttpServer buildHttpServerWithRootContext(
            Options options,
            AdminRequestHandler adminRequestHandler,
            StubRequestHandler stubRequestHandler
    ) {
        return new JettyHttpServer(
                options,
                adminRequestHandler,
                stubRequestHandler
        );
    }

    @Override
    public HttpServer buildHttpServer(com.github.tomakehurst.wiremock.core.Options options, AdminRequestHandler adminRequestHandler, StubRequestHandler stubRequestHandler) {
        return buildHttpServerWithRootContext(WireMockConfiguration.from(options), adminRequestHandler, stubRequestHandler);
    }
}
