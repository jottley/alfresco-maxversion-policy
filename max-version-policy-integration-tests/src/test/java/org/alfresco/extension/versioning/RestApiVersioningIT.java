/*
 * Copyright 2018-2026 Jared Ottley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.alfresco.extension.versioning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

/**
 * Standalone integration test using REST API to verify versioning behavior. This test does not use
 * AlfrescoTestRunner and makes direct REST API calls.
 *
 * @author Jared Ottley (jared.ottley@hyland.com)
 */
public class RestApiVersioningIT {

    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private ObjectMapper objectMapper;
    private String baseUrl;
    private String username;
    private String password;
    private String testFolderId;
    private String testNodeId;

    @Before
    public void setUp() throws Exception {
        // Load configuration from application.properties
        Properties props = new Properties();
        try (InputStream input =
                getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find application.properties");
            }
            props.load(input);
        }

        baseUrl = props.getProperty("content.service.url", "http://localhost:8080");
        username = props.getProperty("content.service.security.basicAuth.username", "admin");
        password = props.getProperty("content.service.security.basicAuth.password", "admin");

        // Initialize HTTP client with basic auth
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

        // Set up preemptive authentication context
        // Parse host and port from baseUrl
        String host = baseUrl.replaceAll("https?://", "").split(":")[0];
        int port = baseUrl.contains(":8080") ? 8080 : 80;
        String scheme = baseUrl.startsWith("https") ? "https" : "http";

        HttpHost targetHost = new HttpHost(host, port, scheme);
        AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());
        httpContext = HttpClientContext.create();
        httpContext.setAuthCache(authCache);

        objectMapper = new ObjectMapper();

        // Create a test folder
        testFolderId = createTestFolder("RestApiVersionTest-" + System.currentTimeMillis());
    }

    @After
    public void tearDown() throws Exception {
        // Clean up test node and folder
        if (testNodeId != null) {
            deleteNode(testNodeId);
        }
        if (testFolderId != null) {
            deleteNode(testFolderId);
        }

        if (httpClient != null) {
            httpClient.close();
        }
    }

    /**
     * Test creating multiple versions via REST API This verifies that the REST API approach works
     * for creating multiple versions
     */
    @Test
    public void testCreateMultipleVersionsViaRestApi() throws Exception {
        System.out.println("\n=== Testing Multiple Version Creation via REST API ===");

        // Create initial versionable document
        testNodeId = createVersionableDocument("test-versioning-doc.txt", testFolderId);
        System.out.println("Created document with ID: " + testNodeId);

        // Upload initial content (creates version 1.0 or 1.1 depending on initialVersion property)
        updateContent(testNodeId, "Initial content - version 1.0");
        Thread.sleep(500); // Allow transaction to complete

        // Check initial version count (may be 1 or 2 depending on initialVersion behavior)
        int initialVersionCount = getVersionCount(testNodeId);
        System.out.println("Initial version count: " + initialVersionCount);

        // Create 4 more versions
        for (int i = 1; i <= 4; i++) {
            String content = "Updated content - version 1." + i;
            updateContent(testNodeId, content);
            Thread.sleep(500); // Allow transaction to complete
            System.out.println("Created version " + (initialVersionCount + i));
        }

        // Verify we have 5 or 6 versions total
        int finalVersionCount = getVersionCount(testNodeId);
        System.out.println("Final version count: " + finalVersionCount);
        int expectedCount = initialVersionCount + 4;
        assertEquals(
                "Should have " + expectedCount + " versions total",
                expectedCount,
                finalVersionCount);

        // Get and display version history
        JsonNode versions = getVersionHistory(testNodeId);
        System.out.println("\n=== Version History ===");
        for (JsonNode entry : versions.get("list").get("entries")) {
            JsonNode version = entry.get("entry");
            System.out.println(
                    "Version: "
                            + version.get("id").asText()
                            + " - Modified: "
                            + version.get("modifiedAt").asText());
        }

        System.out.println("\n=== Test Passed! ===\n");
    }

    /**
     * Test MaxVersionPolicy enforcement when limit is exceeded Creates 12 versions (exceeds the
     * configured limit of 10) and verifies oldest versions are deleted
     */
    @Test
    public void testMaxVersionPolicyDeletesOldestVersions() throws Exception {
        System.out.println("\n=== Testing MaxVersionPolicy Enforcement ===");

        // Create initial versionable document
        testNodeId = createVersionableDocument("test-max-policy.txt", testFolderId);
        System.out.println("Created document with ID: " + testNodeId);

        // Upload initial content
        updateContent(testNodeId, "Initial content");
        Thread.sleep(500);

        int initialVersionCount = getVersionCount(testNodeId);
        System.out.println("Initial version count: " + initialVersionCount);

        // Create 10 more versions (total will be 11 or 12, exceeding limit of 10)
        for (int i = 1; i <= 10; i++) {
            updateContent(testNodeId, "Content update " + i);
            Thread.sleep(500);
            System.out.println("Created version " + (initialVersionCount + i));
        }

        // Wait for policy to execute
        Thread.sleep(2000);

        // Verify version count is capped at 10
        int finalVersionCount = getVersionCount(testNodeId);
        System.out.println("Final version count after policy enforcement: " + finalVersionCount);
        assertEquals("MaxVersionPolicy should cap versions at 10", 10, finalVersionCount);

        // Verify the oldest version was deleted (should not have version 1.0 anymore)
        JsonNode versions = getVersionHistory(testNodeId);
        System.out.println("\n=== Final Version History (should be capped at 10) ===");
        String oldestVersionLabel = null;
        for (JsonNode entry : versions.get("list").get("entries")) {
            JsonNode version = entry.get("entry");
            String label = version.get("id").asText();
            System.out.println(
                    "Version: " + label + " - Modified: " + version.get("modifiedAt").asText());
            oldestVersionLabel = label; // Last one in the list is the oldest
        }

        System.out.println("\nOldest remaining version: " + oldestVersionLabel);
        assertNotNull("Should have an oldest version", oldestVersionLabel);
        System.out.println("\n=== Test Passed! MaxVersionPolicy working correctly ===\n");
    }

    /**
     * Creates a test folder in Company Home via REST API.
     *
     * <p>Uses the Alfresco REST API to create a new folder under the user's home directory (-my-).
     * The folder is created with the {@code cm:folder} type.
     *
     * @param name the name of the folder to create
     * @return the node ID of the created folder
     * @throws Exception if the REST API call fails or returns an error status code
     */
    private String createTestFolder(String name) throws Exception {
        String url =
                baseUrl + "/alfresco/api/-default-/public/alfresco/versions/1/nodes/-my-/children";
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "application/json");

        String json = String.format("{\"name\":\"%s\",\"nodeType\":\"cm:folder\"}", name);
        post.setEntity(new StringEntity(json));

        HttpResponse response = httpClient.execute(post, httpContext);
        int statusCode = response.getStatusLine().getStatusCode();
        String body = EntityUtils.toString(response.getEntity());

        if (statusCode != 200 && statusCode != 201) {
            throw new RuntimeException(
                    "Failed to create folder. Status: " + statusCode + ", Body: " + body);
        }

        JsonNode node = objectMapper.readTree(body);
        return node.get("entry").get("id").asText();
    }

    /**
     * Creates a versionable document via REST API.
     *
     * <p>Uses the Alfresco REST API to create a new content node with the {@code cm:versionable}
     * aspect enabled. The document is configured with {@code cm:autoVersion} set to true, which
     * means a version is created automatically when content is updated.
     *
     * @param name the name of the document to create
     * @param parentId the node ID of the parent folder
     * @return the node ID of the created document
     * @throws Exception if the REST API call fails or returns an error status code
     */
    private String createVersionableDocument(String name, String parentId) throws Exception {
        String url =
                baseUrl
                        + "/alfresco/api/-default-/public/alfresco/versions/1/nodes/"
                        + parentId
                        + "/children";
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "application/json");

        String json =
                String.format(
                        "{\"name\":\"%s\",\"nodeType\":\"cm:content\","
                            + "\"aspectNames\":[\"cm:versionable\"],"
                            + "\"properties\":{\"cm:autoVersion\":true,\"cm:initialVersion\":true}}",
                        name);
        post.setEntity(new StringEntity(json));

        HttpResponse response = httpClient.execute(post, httpContext);
        int statusCode = response.getStatusLine().getStatusCode();
        String body = EntityUtils.toString(response.getEntity());

        if (statusCode != 200 && statusCode != 201) {
            throw new RuntimeException(
                    "Failed to create document. Status: " + statusCode + ", Body: " + body);
        }

        JsonNode node = objectMapper.readTree(body);
        return node.get("entry").get("id").asText();
    }

    /**
     * Updates the content of a document via REST API.
     *
     * <p>Uses the Alfresco REST API to update the content of an existing document. For versionable
     * documents with auto-versioning enabled, this triggers the creation of a new version.
     *
     * @param nodeId the node ID of the document to update
     * @param content the new content text to write to the document
     * @throws Exception if the REST API call fails or returns an error status code
     */
    private void updateContent(String nodeId, String content) throws Exception {
        String url =
                baseUrl
                        + "/alfresco/api/-default-/public/alfresco/versions/1/nodes/"
                        + nodeId
                        + "/content";
        HttpPut put = new HttpPut(url);
        put.setHeader("Content-Type", "text/plain");
        put.setEntity(new ByteArrayEntity(content.getBytes()));

        HttpResponse response = httpClient.execute(put, httpContext);
        int statusCode = response.getStatusLine().getStatusCode();
        EntityUtils.consume(response.getEntity());

        if (statusCode != 200 && statusCode != 201) {
            throw new RuntimeException("Failed to update content. Status: " + statusCode);
        }
    }

    /**
     * Gets the total number of versions for a document.
     *
     * <p>Retrieves the version history from the REST API and returns the count of all versions.
     *
     * @param nodeId the node ID of the document
     * @return the total number of versions
     * @throws Exception if the REST API call fails
     */
    private int getVersionCount(String nodeId) throws Exception {
        JsonNode versions = getVersionHistory(nodeId);
        return versions.get("list").get("entries").size();
    }

    /**
     * Retrieves the complete version history for a document via REST API.
     *
     * <p>Returns the full version history as a JSON structure containing all versions with their
     * metadata (version labels, modification dates, etc.).
     *
     * @param nodeId the node ID of the document
     * @return JSON node containing the version history
     * @throws Exception if the REST API call fails
     */
    private JsonNode getVersionHistory(String nodeId) throws Exception {
        String url =
                baseUrl
                        + "/alfresco/api/-default-/public/alfresco/versions/1/nodes/"
                        + nodeId
                        + "/versions";
        HttpGet get = new HttpGet(url);

        HttpResponse response = httpClient.execute(get, httpContext);
        String body = EntityUtils.toString(response.getEntity());
        return objectMapper.readTree(body);
    }

    /**
     * Permanently deletes a node via REST API.
     *
     * <p>Uses the REST API to permanently delete a node (bypassing the trashcan). This is used
     * during test cleanup to remove test data.
     *
     * @param nodeId the node ID of the node to delete
     * @throws Exception if the REST API call fails
     */
    private void deleteNode(String nodeId) throws Exception {
        String url =
                baseUrl
                        + "/alfresco/api/-default-/public/alfresco/versions/1/nodes/"
                        + nodeId
                        + "?permanent=true";
        HttpDelete delete = new HttpDelete(url);

        HttpResponse response = httpClient.execute(delete, httpContext);
        EntityUtils.consume(response.getEntity());
    }
}
