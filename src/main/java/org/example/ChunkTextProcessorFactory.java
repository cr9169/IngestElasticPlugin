package org.example;

import org.elasticsearch.ingest.Processor;
import org.elasticsearch.ingest.Processor.Factory;

import java.util.Map;

/**
 * Factory class for the custom "chunk_text" ingest processor.
 * This class is responsible for creating instances of ChunkTextProcessor
 * based on pipeline configuration parameters.
 *
 * Accepted parameters:
 * - "field" (String, required): the name of the field to truncate
 * - "chunk_size" (int, optional): max characters to keep (default = 10000)
 */
public class ChunkTextProcessorFactory implements Processor.Factory {

    @Override
    public Processor create(Map<String, Processor.Factory> registry,
                            String processorTag,
                            String description,
                            Map<String, Object> config) throws Exception {

        String field = (String) config.get("field");
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("[chunk_text] missing required parameter 'field'");
        }

        int chunkSize = ((Number) config.getOrDefault("chunk_size", 10000)).intValue();

        return new ChunkTextProcessor(processorTag, description, field, chunkSize);
    }
}
