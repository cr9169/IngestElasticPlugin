package org.example;

import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;

/**
 * A custom Elasticsearch ingest processor that truncates the value of a given string field
 * if it exceeds a configured maximum length (chunkSize).
 *
 * This processor is registered under the type name "chunk_text" and can be used in
 * ingest pipelines to limit the size of a document's content field prior to indexing.
 *
 * Example pipeline configuration:
 * {
 *   "processors": [
 *     {
 *       "chunk_text": {
 *         "field": "content",
 *         "chunk_size": 10000
 *       }
 *     }
 *   ]
 * }
 *
 * Expected behavior:
 * - If the field does not exist or is already under the size limit, it is left unchanged.
 * - If the field is longer than chunkSize characters, it is truncated at chunkSize.
 */
public class ChunkTextProcessor extends AbstractProcessor {
    private final String field;
    private final int chunkSize;

    public ChunkTextProcessor(String tag, String description, String field, int chunkSize) {
        super(tag, description);
        this.field = field;
        this.chunkSize = chunkSize;
    }

    @Override
    public IngestDocument execute(IngestDocument doc) {

        if (!doc.hasField(field)) return doc;

        Object value = doc.getFieldValue(field, Object.class);
        if (value instanceof String text && text.length() > chunkSize) {
            doc.setFieldValue(field, text.substring(0, chunkSize));
        }

        return doc;
    }

    @Override
    public String getType() {
        return "chunk_text";
    }
}
