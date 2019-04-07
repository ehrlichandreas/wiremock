package com.github.ehrlichandreas.wiremock.starter;

import java.util.Collection;

import com.github.tomakehurst.wiremock.WireMockServer;

public interface WireMockCreaterProxyLoader extends WireMockCreaterLoader {
    Collection<WireMockServer> initWireMockServersForProxy(final WireMockServer wireMockProxy);

    Collection<WireMockServer> initWireMockServersForProxy(final WireMockServer wireMockProxy,
                                                           final Collection<String> subStringFilters);
}
