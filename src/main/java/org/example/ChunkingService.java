package org.example;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.xcontent.XContentType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * A utility class that performs file ingestion logic:
 * - Reads the contents of a text file
 * - Splits the content into fixed-size chunks
 * - Indexes each chunk as a separate document in Elasticsearch
 * - Attaches the chunking ingest pipeline to each request
 *
 * This class is used by TxtIngestRestHandler to perform file-based ingestion
 * in response to a user-initiated HTTP request.
 *
 * Restrictions:
 * - Only supports plain .txt files
 * - File must exist and be readable by the Elasticsearch node
 */
public class ChunkingService {

    /**
     * Reads the given .txt file, splits it into chunks, and indexes each chunk
     * into the "my-index" index using the "chunking_pipeline".
     *
     * Each indexed chunk includes:
     * - fileName: The name of the source file
     * - filePath: The absolute path to the file
     * - sequenceNumber: A counter identifying the chunk's order
     * - content: The actual text data in this chunk
     * - processedAt: A timestamp of ingestion
     *
     * @param client   Elasticsearch NodeClient (internally provided by plugin)
     * @param filePath Absolute file path on disk (e.g. /mnt/nas/file.txt)
     * @return number of successfully indexed chunks
     * @throws IOException if the file is invalid or unreadable
     */
    public static int ingestSingleFile(NodeClient client, String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found or not valid: " + filePath);
        }

        byte[] data = Files.readAllBytes(file.toPath());
        int chunkSize = 10_000;
        int start = 0;
        int sequence = 1;
        int chunkCount = 0;

        while (start < data.length) {
            int end = Math.min(start + chunkSize, data.length);
            String content = new String(data, start, end - start, StandardCharsets.UTF_8);

            Map<String, Object> document = new HashMap<>();
            document.put("fileName", file.getName());
            document.put("filePath", file.getAbsolutePath());
            document.put("sequenceNumber", sequence++);
            document.put("content", content);
            document.put("processedAt", new Date());

            IndexRequest request = new IndexRequest("my-index")
                    .source(document, XContentType.JSON)
                    .setPipeline("chunking_pipeline");

            client.index(request).actionGet();
            start = end;
            chunkCount++;
        }

        return chunkCount;
    }
}
