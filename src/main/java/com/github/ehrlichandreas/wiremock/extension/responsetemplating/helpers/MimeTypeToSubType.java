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
package com.github.ehrlichandreas.wiremock.extension.responsetemplating.helpers;

import java.io.IOException;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.HandlebarsHelper;

public class MimeTypeToSubType extends HandlebarsHelper<Object> {

    @Override
    public Object apply(Object context, Options options) throws IOException {
        if (null == context) {
            return null;
        }

        final String contextAsString = String.valueOf(context);

        MimeType mimeType;

        try {
            mimeType = new MimeType(contextAsString);
        } catch (MimeTypeParseException e) {
            throw new IOException(e);
        }

        return mimeType.getSubType();
    }
}
