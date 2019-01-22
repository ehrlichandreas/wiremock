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
package com.github.ehrlichandreas.wiremock;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.github.ehrlichandreas.wiremock.core.WireMockApp;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.HttpServer;
import com.github.tomakehurst.wiremock.http.HttpServerFactory;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;

public class WireMockServer extends com.github.tomakehurst.wiremock.WireMockServer {

    public WireMockServer(Options options) {
        try {
            final WireMockApp wireMockApp = new WireMockApp(options, this);
            final StubRequestHandler stubRequestHandler = wireMockApp.buildStubRequestHandler();
            final HttpServerFactory httpServerFactory = options.httpServerFactory();
            final HttpServer httpServer = httpServerFactory.buildHttpServer(
                    options,
                    wireMockApp.buildAdminRequestHandler(),
                    stubRequestHandler
            );
            final WireMock client = new WireMock(wireMockApp);

            FieldUtils.writeField(this, "options", options, true);
            FieldUtils.writeField(this, "notifier", options.notifier(), true);
            FieldUtils.writeField(this, "wireMockApp", wireMockApp, true);
            FieldUtils.writeField(this, "stubRequestHandler", stubRequestHandler, true);
            FieldUtils.writeField(this, "httpServer", httpServer, true);
            FieldUtils.writeField(this, "client", client, true);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
