package com.mxmind.scraper.internal.supported;

import com.mxmind.scraper.api.Indexer;
import com.mxmind.scraper.api.PageContent;
import org.apache.lucene.document.*;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;

import java.io.IOException;

/**
 * The WebScraper solution.
 *
 * @author mxmind
 * @version 1.0-SNAPSHOT
 * @since 1.0-SNAPSHOT
 */
public final class IndexerImpl implements Indexer {

    private final IndexWriter writer;

    public IndexerImpl(IndexWriter indexWriter) {
        this.writer = indexWriter;
    }

    @Override
    public void index(PageContent pageContent) {
        try {
            writer.addDocument(toDocument(pageContent));
        } catch (CorruptIndexException ex) {
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Document toDocument(PageContent content) {
        final Document doc = new Document();
        doc.add(new StringField("id", content.getPath(), Field.Store.YES));
        doc.add(new StringField("title", content.getTitle(), Field.Store.YES));
        doc.add(new StringField("content", content.getContent(), Field.Store.NO));

        return doc;
    }

    @Override
    public void commit() {
        try {
            writer.commit();
        } catch (CorruptIndexException ex) {
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void close() {
        try {
            writer.close(true);
        } catch (CorruptIndexException ex) {
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

    }

}
