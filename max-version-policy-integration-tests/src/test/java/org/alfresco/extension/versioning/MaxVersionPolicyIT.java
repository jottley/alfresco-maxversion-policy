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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.alfresco.model.ContentModel;
import org.alfresco.rad.test.AbstractAlfrescoIT;
import org.alfresco.rad.test.AlfrescoTestRunner;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests for MaxVersionPolicy Tests the version policy behavior against a running
 * Alfresco instance
 *
 * @author Jared Ottley (jared.ottley@hyland.com)
 */
@RunWith(value = AlfrescoTestRunner.class)
public class MaxVersionPolicyIT extends AbstractAlfrescoIT {

    private ServiceRegistry serviceRegistry;
    private org.alfresco.service.cmr.repository.NodeService nodeService;
    private NodeRef testFolder;

    @Before
    public void setUp() {
        serviceRegistry = getServiceRegistry();
        nodeService = serviceRegistry.getNodeService();

        // Create a test folder to hold test nodes
        testFolder =
                serviceRegistry
                        .getRetryingTransactionHelper()
                        .doInTransaction(
                                () -> {
                                    NodeRef storeRoot =
                                            nodeService.getRootNode(
                                                    new StoreRef(
                                                            StoreRef.PROTOCOL_WORKSPACE,
                                                            "SpacesStore"));

                                    Map<QName, Serializable> folderProps = new HashMap<>();
                                    folderProps.put(
                                            ContentModel.PROP_NAME,
                                            "MaxVersionPolicyTest-" + System.currentTimeMillis());

                                    return nodeService
                                            .createNode(
                                                    storeRoot,
                                                    ContentModel.ASSOC_CHILDREN,
                                                    QName.createQName(
                                                            NamespaceService.CONTENT_MODEL_1_0_URI,
                                                            "test-folder"),
                                                    ContentModel.TYPE_FOLDER,
                                                    folderProps)
                                            .getChildRef();
                                },
                                false,
                                true);
    }

    @After
    public void tearDown() {
        // Clean up test folder (which will cascade delete all test nodes)
        serviceRegistry
                .getRetryingTransactionHelper()
                .doInTransaction(
                        () -> {
                            if (testFolder != null && nodeService.exists(testFolder)) {
                                nodeService.deleteNode(testFolder);
                            }
                            return null;
                        });
    }

    /**
     * Test that the MaxVersionPolicy bean is installed and properly configured Verifies that
     * maxVersions is set to a non-zero, non-negative number
     */
    @Test
    public void testMaxVersionPolicyIsInstalledAndConfigured() {
        // Given - Get the Spring ApplicationContext
        ApplicationContext context = getApplicationContext();

        // When - Get the maxVersion bean
        Object bean = context.getBean("maxVersion");

        // Then - Verify the bean exists and is properly configured
        assertNotNull("maxVersion bean should exist", bean);
        assertTrue("Bean should be instance of MaxVersionPolicy", bean instanceof MaxVersionPolicy);

        MaxVersionPolicy policy = (MaxVersionPolicy) bean;
        assertNotNull("Policy should not be null", policy);

        // Verify maxVersions is set to a valid value (non-zero, non-negative)
        int maxVersions = policy.getMaxVersions();
        assertTrue("maxVersions should be non-negative, but was: " + maxVersions, maxVersions >= 0);
        assertTrue(
                "maxVersions should be non-zero for active policy, but was: " + maxVersions,
                maxVersions > 0);
    }
}
