/*
 * Copyright 2009-2014 the CodeLibs Project and the Others.
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

package jp.sf.fess.ds.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jp.sf.fess.Constants;
import jp.sf.fess.FessSystemException;
import jp.sf.fess.ds.IndexUpdateCallback;
import jp.sf.fess.helper.CrawlingSessionHelper;
import jp.sf.fess.helper.SearchLogHelper;
import jp.sf.fess.helper.SystemHelper;
import jp.sf.fess.util.ComponentUtil;

import org.apache.solr.common.SolrInputDocument;
import org.codelibs.solr.lib.SolrGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexUpdateCallbackImpl implements IndexUpdateCallback {
    private static final Logger logger = LoggerFactory
            .getLogger(IndexUpdateCallbackImpl.class);

    protected SolrGroup solrGroup;

    public int maxDocumentCacheSize = 10;

    public boolean clickCountEnabled = true;

    public boolean favoriteCountEnabled = true;

    protected volatile AtomicLong documentSize = new AtomicLong(0);

    protected volatile long commitPerCount = 0;

    protected volatile long executeTime = 0;

    final List<SolrInputDocument> docList = new ArrayList<SolrInputDocument>();

    /* (non-Javadoc)
     * @see jp.sf.fess.ds.impl.IndexUpdateCallback#store(java.util.Map)
     */
    @Override
    public synchronized boolean store(final Map<String, Object> dataMap) {
        final long startTime = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            logger.debug("Adding " + dataMap);
        }

        //   required check
        final Object urlObj = dataMap.get("url");
        if (urlObj == null) {
            throw new FessSystemException("url is null. dataMap=" + dataMap);
        }

        final CrawlingSessionHelper crawlingSessionHelper = ComponentUtil
                .getCrawlingSessionHelper();
        dataMap.put("id", crawlingSessionHelper.generateId(dataMap));

        final SolrInputDocument doc = createSolrDocument(dataMap);

        docList.add(doc);
        if (logger.isDebugEnabled()) {
            logger.debug("Added the document. "
                    + "The number of a document cache is " + docList.size()
                    + ".");
        }

        if (docList.size() >= maxDocumentCacheSize) {
            sendDocuments();
        }
        documentSize.getAndIncrement();
        // commit
        if (commitPerCount > 0 && documentSize.get() % commitPerCount == 0) {
            if (!docList.isEmpty()) {
                sendDocuments();
            }
            commitDocuments();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("The number of an added document is "
                    + documentSize.get() + ".");
        }

        executeTime += System.currentTimeMillis() - startTime;
        return true;
    }

    protected SolrInputDocument createSolrDocument(
            final Map<String, Object> dataMap) {
        final String url = dataMap.get("url").toString();

        final SolrInputDocument doc = new SolrInputDocument();
        for (final Map.Entry<String, Object> entry : dataMap.entrySet()) {
            if ("boost".equals(entry.getKey())) {
                // boost
                final float documentBoost = Float.valueOf(entry.getValue()
                        .toString());
                doc.setDocumentBoost(documentBoost);
                if (logger.isDebugEnabled()) {
                    logger.debug("Set a document boost (" + documentBoost
                            + ").");
                }
            }
            doc.addField(entry.getKey(), entry.getValue());
        }

        if (clickCountEnabled) {
            addClickCountField(doc, url);
        }

        if (favoriteCountEnabled) {
            addFavoriteCountField(doc, url);
        }

        if (!dataMap.containsKey(Constants.DOC_ID)) {
            final SystemHelper systemHelper = ComponentUtil.getSystemHelper();
            doc.addField(Constants.DOC_ID, systemHelper.generateDocId(dataMap));
        }

        return doc;
    }

    @Override
    public void commit() {
        if (!docList.isEmpty()) {
            sendDocuments();
        }
        commitDocuments();
    }

    protected void commitDocuments() {
        final long execTime = System.currentTimeMillis();
        if (logger.isInfoEnabled()) {
            logger.info("Committing documents. ");
        }
        synchronized (solrGroup) {
            solrGroup.commit(true, true, false, true);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Committed documents. The execution time is "
                    + (System.currentTimeMillis() - execTime) + "ms.");
        }
    }

    protected void sendDocuments() {
        final long execTime = System.currentTimeMillis();
        if (logger.isInfoEnabled()) {
            logger.info("Sending " + docList.size() + " document to a server.");
        }
        synchronized (solrGroup) {
            solrGroup.add(docList);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Sent " + docList.size()
                    + " documents. The execution time is "
                    + (System.currentTimeMillis() - execTime) + "ms.");
        }
        docList.clear();
    }

    protected void addClickCountField(final SolrInputDocument doc,
            final String url) {
        final SearchLogHelper searchLogHelper = ComponentUtil
                .getSearchLogHelper();
        final SystemHelper systemHelper = ComponentUtil.getSystemHelper();
        final int count = searchLogHelper.getClickCount(url);
        doc.addField(systemHelper.clickCountField, count);
        if (logger.isDebugEnabled()) {
            logger.debug("Click Count: " + count + ", url: " + url);
        }
    }

    protected void addFavoriteCountField(final SolrInputDocument doc,
            final String url) {
        final SearchLogHelper searchLogHelper = ComponentUtil
                .getSearchLogHelper();
        final SystemHelper systemHelper = ComponentUtil.getSystemHelper();
        final long count = searchLogHelper.getFavoriteCount(url);
        doc.addField(systemHelper.favoriteCountField, count);
        if (logger.isDebugEnabled()) {
            logger.debug("Favorite Count: " + count + ", url: " + url);
        }
    }

    @Override
    public long getDocumentSize() {
        return documentSize.get();
    }

    @Override
    public long getExecuteTime() {
        return executeTime;
    }

    @Override
    public SolrGroup getSolrGroup() {
        return solrGroup;
    }

    @Override
    public void setSolrGroup(final SolrGroup solrGroup) {
        this.solrGroup = solrGroup;
    }

    @Override
    public void setCommitPerCount(final long commitPerCount) {
        this.commitPerCount = commitPerCount;
    }

}
