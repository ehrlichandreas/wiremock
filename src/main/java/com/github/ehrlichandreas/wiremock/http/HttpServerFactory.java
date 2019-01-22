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
package com.github.ehrlichandreas.wiremock.http;

import com.github.ehrlichandreas.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.AdminRequestHandler;
import com.github.tomakehurst.wiremock.http.HttpServer;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;

public interface HttpServerFactory extends com.github.tomakehurst.wiremock.http.HttpServerFactory {

    HttpServer buildHttpServerWithRootContext(
            Options options,
            AdminRequestHandler adminRequestHandler,
            StubRequestHandler stubRequestHandler
    );
}
