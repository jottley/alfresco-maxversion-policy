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

import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.version.VersionServicePolicies.AfterCreateVersionPolicy;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Alfresco policy that automatically limits the number of versions retained for versioned
 * documents.
 *
 * <p>This policy implements {@link AfterCreateVersionPolicy} to execute after a new version is
 * created. When a document's version count exceeds the configured {@code maxVersions} limit, the
 * oldest versions are automatically deleted until the count is within the limit.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatically removes oldest versions when the configured limit is exceeded
 *   <li>Handles legacy nodes with long version histories by removing multiple versions in a single
 *       operation
 *   <li>Can be disabled by setting {@code maxVersions} to zero
 *   <li>Executes at transaction commit frequency to ensure consistency
 * </ul>
 *
 * <p>Configuration is done via Spring bean injection in {@code module-context.xml}. The default
 * limit is set in {@code alfresco-global.properties} using the {@code maxVersions} property.
 *
 * @author Jared Ottley (jared.ottley@hyland.com)
 * @version 1.0-SNAPSHOT
 * @see AfterCreateVersionPolicy
 */
public class MaxVersionPolicy implements AfterCreateVersionPolicy {

    private static final Log logger = LogFactory.getLog(MaxVersionPolicy.class);

    private PolicyComponent policyComponent;
    private VersionService versionService;

    // max number of versions per version node
    private int maxVersions;

    /**
     * Sets the policy component for binding this policy to the Alfresco repository.
     *
     * <p>This method is called by Spring during bean initialization.
     *
     * @param policyComponent the policy component to bind behaviours to
     */
    public void setPolicyComponent(PolicyComponent policyComponent) {
        this.policyComponent = policyComponent;
    }

    /**
     * Sets the version service for managing document versions.
     *
     * <p>This method is called by Spring during bean initialization.
     *
     * @param versionService the version service for accessing and deleting versions
     */
    public void setVersionService(VersionService versionService) {
        this.versionService = versionService;
    }

    /**
     * Sets the maximum number of versions to retain for each versioned document.
     *
     * <p>When set to 0, the policy is disabled and no versions will be deleted. This method is
     * called by Spring during bean initialization, with the value configured in {@code
     * alfresco-global.properties}.
     *
     * @param maxVersions the maximum number of versions to retain (0 to disable)
     */
    public void setMaxVersions(int maxVersions) {
        this.maxVersions = maxVersions;
    }

    /**
     * Gets the currently configured maximum number of versions.
     *
     * @return the maximum number of versions to retain
     */
    public int getMaxVersions() {
        return this.maxVersions;
    }

    /**
     * Initializes the policy by registering it with the Alfresco policy component.
     *
     * <p>This method is called by Spring after all dependencies have been injected. It binds the
     * {@code afterCreateVersion} behaviour to execute at transaction commit frequency, ensuring
     * that version cleanup happens after the transaction is successfully committed.
     */
    public void init() {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("MaxVersions is set to: %s", maxVersions));
        }
        Behaviour afterCreateVersionBehaviour =
                new JavaBehaviour(
                        this, "afterCreateVersion", NotificationFrequency.TRANSACTION_COMMIT);
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "afterCreateVersion"),
                MaxVersionPolicy.class,
                afterCreateVersionBehaviour);
    }

    /**
     * Policy callback executed after a new version is created for a document.
     *
     * <p>This method is invoked automatically by Alfresco after a document version is created and
     * the transaction is committed. It checks if the total number of versions exceeds the
     * configured {@code maxVersions} limit, and if so, deletes the oldest versions until the count
     * is within the limit.
     *
     * <p>The policy handles legacy nodes with long version histories by repeatedly deleting the
     * root (oldest) version in a loop until the version count meets the limit.
     *
     * <p>If {@code maxVersions} is set to 0, the policy is disabled and no action is taken.
     *
     * @param versionableNode the node reference of the document that was versioned
     * @param version the newly created version (not currently used in this implementation)
     */
    @Override
    public void afterCreateVersion(NodeRef versionableNode, Version version) {
        VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);

        // If maxVersions is zero, consider policy to be disabled
        if (maxVersions == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("maxVersions is set to zero, consider policy to be disabled");
            }
            return;
        }

        if (versionHistory != null) {

            if (logger.isInfoEnabled()) {
                logger.info(
                        String.format(
                                "Current number of versions: %s",
                                versionHistory.getAllVersions().size()));
                logger.info(
                        String.format(
                                "least recent/root version: %s",
                                versionHistory.getRootVersion().getVersionLabel()));
            }

            // If the current number of versions in the VersionHistory is greater
            // than the maxVersions limit, remove the root/least recent version
            // (Remove all version in while cycle since we can have legacy nodes with long history
            // created before the policy was applied.)
            while (versionHistory.getAllVersions().size() > maxVersions) {
                if (logger.isInfoEnabled()) {
                    logger.info(
                            String.format(
                                    "Max Version Policy - Removing Version: %s",
                                    versionHistory.getRootVersion().getVersionLabel()));
                }
                versionService.deleteVersion(versionableNode, versionHistory.getRootVersion());
                versionHistory = versionService.getVersionHistory(versionableNode);
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("versionHistory does not exist");
            }
        }
    }
}
