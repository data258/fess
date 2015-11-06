/*
 * Copyright 2012-2015 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.es.config.exentity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codelibs.fess.es.config.bsentity.BsPathMapping;

/**
 * @author FreeGen
 */
public class PathMapping extends BsPathMapping {

    private static final long serialVersionUID = 1L;

    private Pattern regexPattern;

    public String getId() {
        return asDocMeta().id();
    }

    public void setId(final String id) {
        asDocMeta().id(id);
    }

    public Long getVersionNo() {
        return asDocMeta().version();
    }

    public void setVersionNo(final Long version) {
        asDocMeta().version(version);
    }

    public Matcher getMatcher(final CharSequence input) {
        if (regexPattern == null) {
            regexPattern = Pattern.compile(getRegex());
        }
        return regexPattern.matcher(input);
    }
}
