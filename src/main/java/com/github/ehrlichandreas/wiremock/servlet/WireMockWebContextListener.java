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

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.github.ehrlichandreas.wiremock.core.WireMockApp;
import com.github.ehrlichandreas.wiremock.extension.responsetemplating.helpers.MimeTypeToSubType;
import com.github.jknack.handlebars.Helper;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.AdminRequestHandler;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;
import com.github.tomakehurst.wiremock.servlet.NotImplementedContainer;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class WireMockWebContextListener implements ServletContextListener {

    private static final String APP_CONTEXT_KEY = "WireMockApp";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        boolean verboseLoggingEnabled = Boolean.parseBoolean(
                firstNonNull(context.getInitParameter("verboseLoggingEnabled"), "true"));

        final ImmutableMap.Builder<String, Helper> stringHelperBuilder = new ImmutableMap.Builder<>();
        stringHelperBuilder.put("mimetype-subtype", new MimeTypeToSubType());
        final Map<String, Helper> helpers = stringHelperBuilder.build();

        final WarConfiguration options = new WarConfiguration(context);
        options.extensions(new ResponseTemplateTransformer(false, helpers));
        WireMockApp wireMockApp = new WireMockApp(options, new NotImplementedContainer());

        context.setAttribute(APP_CONTEXT_KEY, wireMockApp);
        context.setAttribute(StubRequestHandler.class.getName(), wireMockApp.buildStubRequestHandler());
        context.setAttribute(AdminRequestHandler.class.getName(), wireMockApp.buildAdminRequestHandler());
        context.setAttribute(Notifier.KEY, new Slf4jNotifier(verboseLoggingEnabled));
    }

    /**
     * @param context Servlet context for parameter reading
     * @return Maximum number of entries or absent
     */
    private Optional<Integer> readMaxRequestJournalEntries(ServletContext context) {
        String str = context.getInitParameter("maxRequestJournalEntries");
        if (str == null) {
            return Optional.absent();
        }
        return Optional.of(Integer.parseInt(str));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

}
