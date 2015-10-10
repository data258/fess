/*
 * Copyright 2009-2015 the CodeLibs Project and the Others.
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

package org.codelibs.fess.crawler.transformer;

import org.codelibs.fess.exception.FessSystemException;
import org.codelibs.robot.entity.ResponseData;
import org.codelibs.robot.extractor.Extractor;
import org.lastaflute.di.core.SingletonLaContainer;

public class FessTikaTransformer extends AbstractFessFileTransformer {
    @Override
    protected Extractor getExtractor(final ResponseData responseData) {
        final Extractor extractor = SingletonLaContainer.getComponent("tikaExtractor");
        if (extractor == null) {
            throw new FessSystemException("Could not find tikaExtractor.");
        }
        return extractor;
    }
}