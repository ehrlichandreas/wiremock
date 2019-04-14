package com.github.ehrlichandreas.wiremock.creater;

import java.util.Map;
import java.util.Objects;

import com.github.ehrlichandreas.wiremock.WireMockServer;
import com.github.ehrlichandreas.wiremock.core.WireMockConfiguration;
import com.github.ehrlichandreas.wiremock.extension.responsetemplating.helpers.MimeTypeToSubType;
import com.github.ehrlichandreas.wiremock.jetty9.JettyHttpServerFactory;
import com.github.jknack.handlebars.Helper;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.trafficlistener.ConsoleNotifyingWiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.google.common.collect.ImmutableMap;

public class WireMockCreater {

    private final WireMockProperties wireMockProperties;

    public WireMockCreater() {
        this(new WireMockProperties());
    }

    public WireMockCreater(WireMockProperties wireMockProperties) {
        this.wireMockProperties = wireMockProperties;
    }

    public static WireMockCreater wireMockCreater() {
        return of();
    }

    public static WireMockCreater of() {
        return new WireMockCreater();
    }

    public static WireMockCreater of(WireMockProperties wireMockProperties) {
        return new WireMockCreater(wireMockProperties);
    }

    public WireMockProperties getWireMockProperties() {
        return this.wireMockProperties;
    }

    public WireMockCreater withWireMockProperties(WireMockProperties wireMockProperties) {
        return of(wireMockProperties);
    }

    public WireMockServer createWireMock() {
        final WireMockConfiguration wireMockConfiguration = createWireMockConfiguration();
        return new WireMockServer(wireMockConfiguration);
    }

    public WireMockConfiguration createWireMockConfiguration() {
        final WireMockConfiguration wireMockConfiguration = WireMockConfiguration.wireMockConfig();

        final WireMockProperties wireMockProperties = getWireMockProperties();
        final int serverPort = wireMockProperties.getServerPort();
        wireMockConfiguration.port(serverPort);

        final String stubsDirectory = wireMockProperties.getStubsDirectory();
        wireMockConfiguration.usingFilesUnderClasspath(stubsDirectory);

        final String stubsRootContext = wireMockProperties.getStubsRootContext();
        wireMockConfiguration.withRootContext(stubsRootContext);

        final Map<String, Helper> helpers = createHandlebarsHelpers();
        final ResponseTemplateTransformer responseTemplateTransformer = new ResponseTemplateTransformer(false, helpers);
        wireMockConfiguration.extensions(responseTemplateTransformer);

        final JettyHttpServerFactory serverFactory = new JettyHttpServerFactory();
        wireMockConfiguration.httpServerFactory(serverFactory);

        final WiremockNetworkTrafficListener networkTrafficListener = new ConsoleNotifyingWiremockNetworkTrafficListener();
        wireMockConfiguration.networkTrafficListener(networkTrafficListener);

        return wireMockConfiguration;
    }

    public Map<String, Helper> createHandlebarsHelpers() {
        final ImmutableMap.Builder<String, Helper> stringHelperBuilder = new ImmutableMap.Builder<>();
        stringHelperBuilder.put("mimetype-subtype", new MimeTypeToSubType());
        return stringHelperBuilder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof WireMockCreater))
            return false;
        WireMockCreater that = (WireMockCreater) o;
        return Objects.equals(getWireMockProperties(), that.getWireMockProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWireMockProperties());
    }

    @Override
    public String toString() {
        return "WireMockStarter{" + "wireMockProperties=" + wireMockProperties + '}';
    }
}
