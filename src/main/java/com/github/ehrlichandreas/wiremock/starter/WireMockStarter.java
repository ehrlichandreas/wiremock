package com.github.ehrlichandreas.wiremock.starter;

import java.util.Optional;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import com.github.ehrlichandreas.wiremock.core.Options;
import com.github.ehrlichandreas.wiremock.creater.WireMockCreater;
import com.github.ehrlichandreas.wiremock.creater.WireMockProperties;
import com.github.tomakehurst.wiremock.WireMockServer;

public abstract class WireMockStarter implements WireMockCreaterLoader {

    public abstract String getWiremockServerPortPropertyName();

    public abstract String getWiremockStubsDirectoryPropertyName();

    public abstract String getWiremockStubsRootContextPropertyName();

    public int startWireMockServer() {
        final WireMockCreater wireMockCreater = loadWireMockCreater();
        final WireMockServer wireMock = wireMockCreater.createWireMock();
        wireMock.start();

        return wireMock.port();
    }

    @Override
    public WireMockCreater loadWireMockCreater() {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.scan(this.getClass().getPackage().getName());
        context.refresh();

        final ConfigurableEnvironment configurableEnvironment = context.getEnvironment();
        final String wiremockServerPortAsString = configurableEnvironment.getProperty(getWiremockServerPortPropertyName());
        final Optional<String> wiremockServerPortAsStringOptional = Optional.ofNullable(wiremockServerPortAsString);
        final String wiremockServerPortDefault = String.valueOf(Options.DEFAULT_PORT);
        final String wiremockServerPortFixed = wiremockServerPortAsStringOptional.orElse(wiremockServerPortDefault);
        final int wiremockServerPort = Integer.parseInt(wiremockServerPortFixed);
        final String wiremockStubsDirectory = configurableEnvironment.getProperty(getWiremockStubsDirectoryPropertyName());
        final String wiremockStubsRootContext = configurableEnvironment.getProperty(getWiremockStubsRootContextPropertyName());

        final WireMockProperties wireMockProperties = WireMockProperties.of(wiremockServerPort, wiremockStubsDirectory,
                wiremockStubsRootContext);

        return WireMockCreater.of(wireMockProperties);
    }
}
