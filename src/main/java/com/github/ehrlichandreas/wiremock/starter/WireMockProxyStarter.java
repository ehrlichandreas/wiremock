package com.github.ehrlichandreas.wiremock.starter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

import com.github.ehrlichandreas.wiremock.creater.WireMockCreater;
import com.github.ehrlichandreas.wiremock.creater.WireMockProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;

public abstract class WireMockProxyStarter extends WireMockStarter implements WireMockCreaterProxyLoader {

    private static final String PROXY_PROTOCOL = "http";
    private static final String PROXY_HOST = "localhost";
    private static final String LOAD_WIRE_MOCK_CREATER_METHOD_NAME = "loadWireMockCreater";

    public abstract String getWiremockServerPortPropertyName();

    public abstract String getWiremockStubsDirectoryPropertyName();

    public abstract String getWiremockStubsRootContextPropertyName();

    public abstract String getWiremockProxyPropertiesPrefix();

    @Override
    public int startWireMockServer() {
        final WireMockCreater wireMockCreater = loadWireMockCreater();
        final WireMockServer wireMock = wireMockCreater.createWireMock();
        wireMock.start();

        initWireMockServersForProxy(wireMock);

        return wireMock.port();
    }

    @Override
    public Collection<WireMockServer> initWireMockServersForProxy(WireMockServer wireMockProxy) {
        return initWireMockServersForProxy(wireMockProxy, null);
    }

    @Override
    public Collection<WireMockServer> initWireMockServersForProxy(WireMockServer wireMockProxy, Collection<String> classNameSubstrings) {
        Objects.requireNonNull(wireMockProxy);

        final Optional<Collection<String>> classNameSubstringsOptional = Optional.ofNullable(classNameSubstrings);
        final Collection<String> emptyList = Collections.emptyList();
        final Collection<String> classNameSubStringsFixed = classNameSubstringsOptional.orElse(emptyList);

        final Collection<String> classNameCollection = loadWireMockStarters();
        final Optional<Collection<String>> classNameCollectionOptional = Optional.ofNullable(classNameCollection);
        final Collection<String> classNameCollectionFixed = classNameCollectionOptional.orElse(emptyList);

        final Stream<String> classNameStream = classNameCollectionFixed.stream();

        final Stream<String> classNameStreamFiltered = classNameStream
                .filter(className -> classNameContainsSubstring(classNameSubStringsFixed, className));
        final Collection<String> classNameCollectionFiltered = classNameStreamFiltered.collect(Collectors.toList());

        final Collection<WireMockCreater> wireMockCreaterCollection = createWireMockCreaters(
                classNameCollectionFiltered);
        final Stream<WireMockCreater> wireMockCreaterStream = wireMockCreaterCollection.stream();
        final Stream<Map.Entry<WireMockServer, MappingBuilder>> entryStream = wireMockCreaterStream
                .map(this::initCustomerviewWireMockServer);
        final Collection<Map.Entry<WireMockServer, MappingBuilder>> entryCollection = entryStream
                .collect(Collectors.toList());

        final Stream<MappingBuilder> mappingBuilderStream = entryCollection.stream().map(Map.Entry::getValue);
        final Collection<MappingBuilder> mappingBuilderCollection = mappingBuilderStream.collect(Collectors.toList());

        final Stream<WireMockServer> wireMockServerStream = entryCollection.stream().map(Map.Entry::getKey);
        final Collection<WireMockServer> wireMockServerCollection = wireMockServerStream.collect(Collectors.toList());

        mappingBuilderCollection.forEach(wireMockProxy::stubFor);

        return wireMockServerCollection;
    }

    private boolean classNameContainsSubstring(Collection<String> classNameSubStringsFixed, String className) {
        return classNameSubStringsFixed.isEmpty()
                || classNameSubStringsFixed.stream().anyMatch(className::contains);
    }

    private Collection<WireMockCreater> createWireMockCreaters(Collection<String> classNameCollection) {
        final Optional<Collection<String>> classNameCollectionOptional = Optional.ofNullable(classNameCollection);
        final Collection<String> classNameCollectionFixed = classNameCollectionOptional.orElse(Collections.emptyList());
        final Stream<String> classNameStream = classNameCollectionFixed.stream();
        final Stream<WireMockCreater> wireMockCreaterStream = classNameStream.map(this::loadWireMockCreater);
        final Stream<WireMockProperties> wireMockPropertiesStream = wireMockCreaterStream
                .map(WireMockCreater::getWireMockProperties);
        final Stream<WireMockProperties> wireMockPropertiesStreamFixed = wireMockPropertiesStream
                .map(WireMockProperties::withDynamicServerPort);
        final Stream<WireMockCreater> wireMockCreaterStreamFixed = wireMockPropertiesStreamFixed
                .map(WireMockCreater::of);
        return wireMockCreaterStreamFixed.collect(Collectors.toList());
    }

    private Map.Entry<WireMockServer, MappingBuilder> initCustomerviewWireMockServer(
            WireMockCreater wireMockCreater) {
        Objects.requireNonNull(wireMockCreater);

        final WireMockServer wireMockServer = wireMockCreater.createWireMock();
        wireMockServer.start();

        final int port = wireMockServer.port();
        final WireMockProperties wireMockProperties = wireMockCreater.getWireMockProperties();
        final WireMockProperties wireMockPropertiesFixed = wireMockProperties.withServerPort(port);
        final MappingBuilder proxyMappingBuilder = createProxyMappingBuilder(wireMockPropertiesFixed);

        return new AbstractMap.SimpleImmutableEntry<>(wireMockServer, proxyMappingBuilder);
    }

    private MappingBuilder createProxyMappingBuilder(WireMockProperties wireMockProperties) {
        Objects.requireNonNull(wireMockProperties);

        final String stubsRootContext = wireMockProperties.getStubsRootContext();
        final String proxyRootContext = stubsRootContext + "/.*";
        final String endPoint = PROXY_PROTOCOL + "://" + PROXY_HOST + ":" + wireMockProperties.getServerPort();
        final ResponseDefinitionBuilder responseDefinitionBuilder = WireMock.aResponse();
        final ResponseDefinitionBuilder.ProxyResponseDefinitionBuilder proxyResponseDefinitionBuilder = responseDefinitionBuilder
                .proxiedFrom(endPoint);
        final UrlPattern urlPattern = WireMock.urlMatching(proxyRootContext);

        return WireMock.any(urlPattern).willReturn(proxyResponseDefinitionBuilder);
    }

    private Collection<String> loadWireMockStarters() {
        final ConfigurableEnvironment configurableEnvironment = loadConfigurableEnvironment();
        final String wiremockProxyPropertiesPrefix = configurableEnvironment
                .getProperty(getWiremockProxyPropertiesPrefix());
        final Optional<String> wiremockProxyPropertiesPrefixOptional = Optional.ofNullable(wiremockProxyPropertiesPrefix);
        final String wiremockProxyPropertiesPrefixFixed = wiremockProxyPropertiesPrefixOptional.orElse("");

        final PropertySources propertySources = configurableEnvironment.getPropertySources();
        final Stream<PropertySource<?>> propertySourceStream = StreamSupport
                .stream(propertySources.spliterator(), false).filter(Objects::nonNull);
        final Stream<PropertySource<?>> propertySourceStreamFiltered = propertySourceStream
                .filter(ps -> ps instanceof EnumerablePropertySource);
        final Stream<String[]> propertyNamesStream = propertySourceStreamFiltered
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames()).filter(Objects::nonNull);
        final Stream<String> propertyNameStream = propertyNamesStream.flatMap(Arrays::stream).filter(Objects::nonNull);
        final Stream<String> propertyNameStreamFiltered = propertyNameStream
                .filter(propertyName -> wiremockProxyPropertiesPrefixFixed.isEmpty() || propertyName.startsWith(wiremockProxyPropertiesPrefixFixed));
        final Stream<String> propertyValueStream = propertyNameStreamFiltered.map(configurableEnvironment::getProperty)
                .filter(Objects::nonNull);
        final Stream<String> propertyValueStreamDistinct = propertyValueStream.distinct();

        return propertyValueStreamDistinct.collect(Collectors.toList());
    }

    private ConfigurableEnvironment loadConfigurableEnvironment() {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        final String packageName = this.getClass().getPackage().getName();

        context.scan(packageName);
        context.refresh();

        return context.getEnvironment();
    }

    private WireMockCreater loadWireMockCreater(final String className) {
        final Optional<String> classNameOptional = Optional.ofNullable(className);
        final Optional<? extends Class<?>> classOptional = classNameOptional.map(s -> {
            try {
                return Class.forName(s);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        final Optional<Method> loadWireMockCreater = classOptional.map(classN -> {
            try {
                return classN.getMethod(LOAD_WIRE_MOCK_CREATER_METHOD_NAME);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
        final Optional<Object> optionalObject = loadWireMockCreater.map(method -> {
            method.setAccessible(true);
            final Class<?> aClass = classOptional.get();
            try {
                final Constructor<?> declaredConstructor = aClass.getDeclaredConstructor();
                final Object object = declaredConstructor.newInstance();
                return method.invoke(object);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
        final Optional<WireMockCreater> wireMockCreaterOptional = optionalObject
                .map(object -> (WireMockCreater) object);

        return wireMockCreaterOptional.orElse(null);
    }
}
