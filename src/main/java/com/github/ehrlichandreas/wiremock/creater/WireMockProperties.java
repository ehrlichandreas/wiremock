package com.github.ehrlichandreas.wiremock.creater;

import java.util.Objects;

import com.github.ehrlichandreas.wiremock.core.Options;

public class WireMockProperties {

    private int serverPort;
    private String stubsDirectory;
    private String stubsRootContext;

    public WireMockProperties() {
        this(Options.DEFAULT_PORT, "wiremock-stubs", Options.DEFAULT_ROOT_CONTEXT);
    }

    public WireMockProperties(final int serverPort, final String stubsDirectory, final String stubsRootContext) {
        this.serverPort = serverPort;
        this.stubsDirectory = stubsDirectory;
        this.stubsRootContext = stubsRootContext;
    }

    public static WireMockProperties wireMockProperties() {
        return of();
    }

    public static WireMockProperties of() {
        return new WireMockProperties();
    }

    public static WireMockProperties of(final int serverPort, final String stubsDirectory,
                                        final String stubsRootContext) {
        return new WireMockProperties(serverPort, stubsDirectory, stubsRootContext);
    }

    public WireMockProperties withDynamicServerPort() {
        return this.withServerPort(0);
    }

    public WireMockProperties withServerPort(final int serverPort) {
        return of(serverPort, getStubsDirectory(), getStubsRootContext());
    }

    public WireMockProperties withStubsDirectory(final String stubsDirectory) {
        return of(getServerPort(), stubsDirectory, getStubsRootContext());
    }

    public WireMockProperties withStubsRootContext(final String stubsRootContext) {
        return of(getServerPort(), getStubsDirectory(), stubsRootContext);
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public String getStubsDirectory() {
        return this.stubsDirectory;
    }

    public String getStubsRootContext() {
        return this.stubsRootContext;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof WireMockProperties))
            return false;
        WireMockProperties that = (WireMockProperties) o;
        return getServerPort() == that.getServerPort() && Objects.equals(getStubsDirectory(), that.getStubsDirectory())
                && Objects.equals(getStubsRootContext(), that.getStubsRootContext());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerPort(), getStubsDirectory(), getStubsRootContext());
    }

    @Override
    public String toString() {
        return "WireMockProperties{" + "serverPort=" + serverPort + ", stubsDirectory='" + stubsDirectory + '\''
                + ", stubsRootContext='" + stubsRootContext + '\'' + '}';
    }
}
