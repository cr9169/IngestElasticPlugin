package org.example;

import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.rest.*;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentType;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A REST endpoint for ingesting text files into Elasticsearch via plugin.
 * Triggered by: POST /_txt_ingest
 *
 * Requires a JSON body:
 * {
 *   "path": "/absolute/path/to/file.txt"
 * }
 */
public class TxtIngestRestHandler extends BaseRestHandler {

    @Override
    public String getName() {
        return "txt_ingest_handler";
    }

    @Override
    public List<Route> routes() {
        return List.of(new Route(RestRequest.Method.POST, "/_txt_ingest"));
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        Map<String, Object> source = request.contentParser().map();
        String filePath = (String) source.get("path");

        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Missing required 'path' in request body");
        }

        return channel -> {
            try {
                if (!new File(filePath).exists()) {
                    throw new IllegalArgumentException("File does not exist: " + filePath);
                }

                int chunkCount = ChunkingService.ingestSingleFile(client, filePath);

                XContentBuilder builder = XContentFactory.jsonBuilder();
                builder.startObject();
                builder.field("success", true);
                builder.field("message", "Ingestion complete");
                builder.field("chunks", chunkCount);
                builder.endObject();

                BytesReference bytes = BytesReference.bytes(builder);

                RestResponse response = new RestResponse(RestStatus.OK, XContentType.JSON.mediaType(), bytes);
                channel.sendResponse(response);

            } catch (Exception e) {
                XContentBuilder builder = XContentFactory.jsonBuilder();
                builder.startObject();
                builder.field("success", false);
                builder.field("error", e.getMessage());
                builder.endObject();

                BytesReference bytes = BytesReference.bytes(builder);

                RestResponse response = new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, XContentType.JSON.mediaType(), bytes);
                channel.sendResponse(response);
            }
        };
    }
}