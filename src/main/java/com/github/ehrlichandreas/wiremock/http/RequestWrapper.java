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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.text.WordUtils;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.google.common.base.Optional;

public class RequestWrapper implements Request {

    private Request request;

    public RequestWrapper(Request request) {
        this.request = request;
    }

    private String capitalizeHeaderKey(String key) {
        return WordUtils.capitalizeFully(key, '-');
    }

    @Override
    public String getUrl() {
        return request.getUrl();
    }

    @Override
    public String getAbsoluteUrl() {
        return request.getAbsoluteUrl();
    }

    @Override
    public RequestMethod getMethod() {
        return request.getMethod();
    }

    @Override
    public String getScheme() {
        return request.getScheme();
    }

    @Override
    public String getHost() {
        return request.getHost();
    }

    @Override
    public int getPort() {
        return request.getPort();
    }

    @Override
    public String getClientIp() {
        return request.getClientIp();
    }

    @Override
    public String getHeader(String key) {
        final String keyNew = capitalizeHeaderKey(key);
        return request.getHeader(keyNew);
    }

    @Override
    public HttpHeader header(String key) {
        final String keyNew = capitalizeHeaderKey(key);
        return request.header(keyNew);
    }

    @Override
    public ContentTypeHeader contentTypeHeader() {
        return request.contentTypeHeader();
    }

    @Override
    public HttpHeaders getHeaders() {
        final Set<String> httpHeaderKeySet = getAllHeaderKeys();
        final List<HttpHeader> httpHeaderList = new ArrayList<>();

        for(final String key: httpHeaderKeySet) {
            final HttpHeader httpHeader = header(key);

            httpHeaderList.add(httpHeader);
        }

        final HttpHeader[] httpHeaderArray = httpHeaderList.toArray(new HttpHeader[]{});
        return new HttpHeaders(httpHeaderArray);
    }

    @Override
    public boolean containsHeader(String key) {
        final String keyNew = capitalizeHeaderKey(key);
        return request.containsHeader(keyNew);
    }

    @Override
    public Set<String> getAllHeaderKeys() {
        final Set<String> httpHeaderKeySet = request.getAllHeaderKeys();
        final Set<String> httpHeaderKeySetNew = new TreeSet<>();

        for(final String headerKey: httpHeaderKeySet) {
            final String headerKeyNew = capitalizeHeaderKey(headerKey);
            httpHeaderKeySetNew.add(headerKeyNew);
        }

        return httpHeaderKeySetNew;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return request.getCookies();
    }

    @Override
    public QueryParameter queryParameter(String key) {
        return request.queryParameter(key);
    }

    @Override
    public byte[] getBody() {
        return request.getBody();
    }

    @Override
    public String getBodyAsString() {
        return request.getBodyAsString();
    }

    @Override
    public String getBodyAsBase64() {
        return request.getBodyAsBase64();
    }

    @Override
    public boolean isMultipart() {
        return request.isMultipart();
    }

    //TODO
    @Override
    public Collection<Part> getParts() {
        return request.getParts();
    }

    @Override
    public Part getPart(String name) {
        return request.getPart(name);
    }

    @Override
    public boolean isBrowserProxyRequest() {
        return request.isBrowserProxyRequest();
    }

    @Override
    public Optional<Request> getOriginalRequest() {
        return request.getOriginalRequest();
    }
}
