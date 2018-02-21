/*
 * Copyright 2018 Jared Ottley
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
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.version.VersionServicePolicies.AfterCreateVersionPolicy;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;


/**
 * 
 * @author Jared Ottley (jared.ottley@alfresco.com)
 * @version 0.0.10
 * 
 */

public class MaxVersionPolicy
    implements AfterCreateVersionPolicy
{

    private Logger          logger = Logger.getLogger(MaxVersionPolicy.class);

    private PolicyComponent policyComponent;
    private VersionService  versionService;

    // max number of versions per version node
    private int             maxVersions;

    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }


    public void setVersionService(VersionService versionService)
    {
        this.versionService = versionService;
    }


    public void setMaxVersions(int maxVersions)
    {
        this.maxVersions = maxVersions;
    }


    public void init()
    {
        logger.debug("MaxVersions is set to: " + maxVersions);
        Behaviour afterCreateVersionBehaviour = new JavaBehaviour(this, "afterCreateVersion", NotificationFrequency.TRANSACTION_COMMIT);
        this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "afterCreateVersion"), MaxVersionPolicy.class, afterCreateVersionBehaviour);
    }


    @Override
    public void afterCreateVersion(NodeRef versionableNode, Version version)
    {
        VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);

        // If maxVersions is zero, consider policy to be disabled
        if (maxVersions == 0)
        {
            logger.debug("maxVersions is set to zero, consider policy to be disabled");
            return;
        }

        if (versionHistory != null)
        {
            logger.debug("Current number of versions: " + versionHistory.getAllVersions().size());
            logger.debug("least recent/root version: " + versionHistory.getRootVersion().getVersionLabel());

            // If the current number of versions in the VersionHistory is greater
            // than the maxVersions limit, remove the root/least recent version
            // (Remove all version in while cycle since we can have legacy nodes with long history created before the policy was applied.)
            while (versionHistory.getAllVersions().size() > maxVersions)
            {
                logger.debug("Removing Version: " + versionHistory.getRootVersion().getVersionLabel());
                versionService.deleteVersion(versionableNode, versionHistory.getRootVersion());
                versionHistory = versionService.getVersionHistory(versionableNode);
            }
        }
        else
        {
            logger.debug("versionHistory does not exist");
        }
    }
}
