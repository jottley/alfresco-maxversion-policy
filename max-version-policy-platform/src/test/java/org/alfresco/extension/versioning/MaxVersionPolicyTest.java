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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Unit tests for MaxVersionPolicy
 *
 * @author Jared Ottley (jared.ottley@hyland.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class MaxVersionPolicyTest {
    @Mock private PolicyComponent policyComponent;

    @Mock private VersionService versionService;

    @Mock private VersionHistory versionHistory;

    @Mock private Version rootVersion;

    @Mock private Version latestVersion;

    private NodeRef nodeRef;

    private MaxVersionPolicy policy;

    @Before
    public void setUp() {
        policy = new MaxVersionPolicy();
        policy.setPolicyComponent(policyComponent);
        policy.setVersionService(versionService);

        // Create a real NodeRef instance (cannot be mocked as it's a final class)
        nodeRef = new NodeRef("workspace://SpacesStore/test-node-id");
    }

    /** Test that init() correctly registers the policy with PolicyComponent */
    @Test
    public void testInitRegistersPolicy() {
        // Given
        policy.setMaxVersions(10);

        // When
        policy.init();

        // Then
        ArgumentCaptor<QName> policyQNameCaptor = ArgumentCaptor.forClass(QName.class);
        ArgumentCaptor<Class<?>> classCaptor = ArgumentCaptor.forClass(Class.class);
        ArgumentCaptor<Behaviour> behaviourCaptor = ArgumentCaptor.forClass(Behaviour.class);

        verify(policyComponent)
                .bindClassBehaviour(
                        policyQNameCaptor.capture(),
                        classCaptor.capture(),
                        behaviourCaptor.capture());

        // Verify QName is "afterCreateVersion" with Alfresco namespace
        assertEquals("afterCreateVersion", policyQNameCaptor.getValue().getLocalName());
        // The namespace URI comes from NamespaceService.ALFRESCO_URI
        assertTrue(
                "Namespace should be Alfresco URI",
                policyQNameCaptor.getValue().getNamespaceURI().contains("alfresco.org"));

        // Verify class is MaxVersionPolicy
        assertEquals(MaxVersionPolicy.class, classCaptor.getValue());

        // Verify behaviour is not null
        assertNotNull(behaviourCaptor.getValue());
    }

    /** Test that policy is disabled when maxVersions is zero */
    @Test
    public void testPolicyDisabledWhenMaxVersionsIsZero() {
        // Given
        policy.setMaxVersions(0);
        when(versionService.getVersionHistory(nodeRef)).thenReturn(versionHistory);

        // When
        policy.afterCreateVersion(nodeRef, latestVersion);

        // Then
        verify(versionService, never()).deleteVersion(any(NodeRef.class), any(Version.class));
        verify(versionHistory, never()).getAllVersions();
    }

    /** Test that no versions are deleted when below the limit */
    @Test
    public void testNoVersionsDeletedWhenBelowLimit() {
        // Given
        policy.setMaxVersions(10);
        when(versionService.getVersionHistory(nodeRef)).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(createVersionCollection(5));
        // No need to stub getRootVersion() as the while loop won't execute

        // When
        policy.afterCreateVersion(nodeRef, latestVersion);

        // Then
        verify(versionService, never()).deleteVersion(any(NodeRef.class), any(Version.class));
    }

    /** Test that exactly one version is deleted when at limit + 1 */
    @Test
    public void testSingleVersionDeletedWhenAtLimit() {
        // Given
        policy.setMaxVersions(10);
        when(versionService.getVersionHistory(nodeRef)).thenReturn(versionHistory);

        // First call returns 11 versions, second call returns 10 (after deletion)
        when(versionHistory.getAllVersions())
                .thenReturn(createVersionCollection(11))
                .thenReturn(createVersionCollection(10));
        when(versionHistory.getRootVersion()).thenReturn(rootVersion);

        // When
        policy.afterCreateVersion(nodeRef, latestVersion);

        // Then
        verify(versionService, times(1)).deleteVersion(nodeRef, rootVersion);
    }

    /** Test that multiple versions are deleted for legacy nodes with many versions */
    @Test
    public void testMultipleVersionsDeletedForLegacyNode() {
        // Given
        policy.setMaxVersions(10);
        when(versionService.getVersionHistory(nodeRef)).thenReturn(versionHistory);

        // Simulate 100 versions being reduced to 10
        when(versionHistory.getAllVersions())
                .thenReturn(createVersionCollection(100))
                .thenReturn(createVersionCollection(99))
                .thenReturn(createVersionCollection(98))
                .thenReturn(createVersionCollection(97))
                .thenReturn(createVersionCollection(96))
                .thenReturn(createVersionCollection(95))
                .thenReturn(createVersionCollection(94))
                .thenReturn(createVersionCollection(93))
                .thenReturn(createVersionCollection(92))
                .thenReturn(createVersionCollection(91))
                .thenReturn(createVersionCollection(90))
                .thenReturn(createVersionCollection(89))
                .thenReturn(createVersionCollection(88))
                .thenReturn(createVersionCollection(87))
                .thenReturn(createVersionCollection(86))
                .thenReturn(createVersionCollection(85))
                .thenReturn(createVersionCollection(84))
                .thenReturn(createVersionCollection(83))
                .thenReturn(createVersionCollection(82))
                .thenReturn(createVersionCollection(81))
                .thenReturn(createVersionCollection(80))
                .thenReturn(createVersionCollection(79))
                .thenReturn(createVersionCollection(78))
                .thenReturn(createVersionCollection(77))
                .thenReturn(createVersionCollection(76))
                .thenReturn(createVersionCollection(75))
                .thenReturn(createVersionCollection(74))
                .thenReturn(createVersionCollection(73))
                .thenReturn(createVersionCollection(72))
                .thenReturn(createVersionCollection(71))
                .thenReturn(createVersionCollection(70))
                .thenReturn(createVersionCollection(69))
                .thenReturn(createVersionCollection(68))
                .thenReturn(createVersionCollection(67))
                .thenReturn(createVersionCollection(66))
                .thenReturn(createVersionCollection(65))
                .thenReturn(createVersionCollection(64))
                .thenReturn(createVersionCollection(63))
                .thenReturn(createVersionCollection(62))
                .thenReturn(createVersionCollection(61))
                .thenReturn(createVersionCollection(60))
                .thenReturn(createVersionCollection(59))
                .thenReturn(createVersionCollection(58))
                .thenReturn(createVersionCollection(57))
                .thenReturn(createVersionCollection(56))
                .thenReturn(createVersionCollection(55))
                .thenReturn(createVersionCollection(54))
                .thenReturn(createVersionCollection(53))
                .thenReturn(createVersionCollection(52))
                .thenReturn(createVersionCollection(51))
                .thenReturn(createVersionCollection(50))
                .thenReturn(createVersionCollection(49))
                .thenReturn(createVersionCollection(48))
                .thenReturn(createVersionCollection(47))
                .thenReturn(createVersionCollection(46))
                .thenReturn(createVersionCollection(45))
                .thenReturn(createVersionCollection(44))
                .thenReturn(createVersionCollection(43))
                .thenReturn(createVersionCollection(42))
                .thenReturn(createVersionCollection(41))
                .thenReturn(createVersionCollection(40))
                .thenReturn(createVersionCollection(39))
                .thenReturn(createVersionCollection(38))
                .thenReturn(createVersionCollection(37))
                .thenReturn(createVersionCollection(36))
                .thenReturn(createVersionCollection(35))
                .thenReturn(createVersionCollection(34))
                .thenReturn(createVersionCollection(33))
                .thenReturn(createVersionCollection(32))
                .thenReturn(createVersionCollection(31))
                .thenReturn(createVersionCollection(30))
                .thenReturn(createVersionCollection(29))
                .thenReturn(createVersionCollection(28))
                .thenReturn(createVersionCollection(27))
                .thenReturn(createVersionCollection(26))
                .thenReturn(createVersionCollection(25))
                .thenReturn(createVersionCollection(24))
                .thenReturn(createVersionCollection(23))
                .thenReturn(createVersionCollection(22))
                .thenReturn(createVersionCollection(21))
                .thenReturn(createVersionCollection(20))
                .thenReturn(createVersionCollection(19))
                .thenReturn(createVersionCollection(18))
                .thenReturn(createVersionCollection(17))
                .thenReturn(createVersionCollection(16))
                .thenReturn(createVersionCollection(15))
                .thenReturn(createVersionCollection(14))
                .thenReturn(createVersionCollection(13))
                .thenReturn(createVersionCollection(12))
                .thenReturn(createVersionCollection(11))
                .thenReturn(createVersionCollection(10));

        when(versionHistory.getRootVersion()).thenReturn(rootVersion);

        // When
        policy.afterCreateVersion(nodeRef, latestVersion);

        // Then - should have deleted 90 versions (100 - 10 = 90)
        verify(versionService, times(90)).deleteVersion(nodeRef, rootVersion);

        // Verify version history was refreshed 91 times (initial + 90 refreshes)
        verify(versionService, times(91)).getVersionHistory(nodeRef);
    }

    /** Test that null version history is handled gracefully */
    @Test
    public void testNullVersionHistoryHandledGracefully() {
        // Given
        policy.setMaxVersions(10);
        when(versionService.getVersionHistory(nodeRef)).thenReturn(null);

        // When
        policy.afterCreateVersion(nodeRef, latestVersion);

        // Then - should not throw exception
        verify(versionService, never()).deleteVersion(any(NodeRef.class), any(Version.class));
    }

    /** Test that maxVersions = 1 keeps only one version */
    @Test
    public void testMaxVersionsEqualsOne() {
        // Given
        policy.setMaxVersions(1);
        when(versionService.getVersionHistory(nodeRef)).thenReturn(versionHistory);

        // Simulate 5 versions being reduced to 1
        when(versionHistory.getAllVersions())
                .thenReturn(createVersionCollection(5))
                .thenReturn(createVersionCollection(4))
                .thenReturn(createVersionCollection(3))
                .thenReturn(createVersionCollection(2))
                .thenReturn(createVersionCollection(1));
        when(versionHistory.getRootVersion()).thenReturn(rootVersion);

        // When
        policy.afterCreateVersion(nodeRef, latestVersion);

        // Then - should have deleted 4 versions
        verify(versionService, times(4)).deleteVersion(nodeRef, rootVersion);
    }

    /** Test first version creation with maxVersions > 0 doesn't delete anything */
    @Test
    public void testFirstVersionCreation() {
        // Given
        policy.setMaxVersions(10);
        when(versionService.getVersionHistory(nodeRef)).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(createVersionCollection(1));
        // No need to stub getRootVersion() as the while loop won't execute

        // When
        policy.afterCreateVersion(nodeRef, latestVersion);

        // Then
        verify(versionService, never()).deleteVersion(any(NodeRef.class), any(Version.class));
    }

    /** Test that root version is always the one being deleted */
    @Test
    public void testOldestVersionDeletedFirst() {
        // Given
        policy.setMaxVersions(10);
        when(versionService.getVersionHistory(nodeRef)).thenReturn(versionHistory);
        when(versionHistory.getAllVersions())
                .thenReturn(createVersionCollection(12))
                .thenReturn(createVersionCollection(11))
                .thenReturn(createVersionCollection(10));
        when(versionHistory.getRootVersion()).thenReturn(rootVersion);

        // When
        policy.afterCreateVersion(nodeRef, latestVersion);

        // Then - verify rootVersion is used for deletion
        verify(versionService, times(2)).deleteVersion(nodeRef, rootVersion);
        verify(versionHistory, times(2)).getRootVersion();
    }

    /** Test that versions exactly at the limit don't trigger deletion */
    @Test
    public void testVersionsExactlyAtLimit() {
        // Given
        policy.setMaxVersions(10);
        when(versionService.getVersionHistory(nodeRef)).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(createVersionCollection(10));
        // No need to stub getRootVersion() as the while loop won't execute

        // When
        policy.afterCreateVersion(nodeRef, latestVersion);

        // Then
        verify(versionService, never()).deleteVersion(any(NodeRef.class), any(Version.class));
    }

    /**
     * Helper method to create a collection with the specified number of versions We use simple mock
     * objects without stubbing final methods
     */
    private Collection<Version> createVersionCollection(int size) {
        Collection<Version> versions = new ArrayList<Version>();
        for (int i = 0; i < size; i++) {
            versions.add(mock(Version.class));
        }
        return versions;
    }
}
