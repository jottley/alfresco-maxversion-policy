/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
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
 * @version 0.4
 * 
 */

public class MaxVersionPolicy implements AfterCreateVersionPolicy {

	private Logger logger = Logger.getLogger(MaxVersionPolicy.class);

	private PolicyComponent policyComponent;
	private VersionService versionService;

	// max number of versions per versioned node
	private int maxVersions;

	private Behaviour afterCreateVersion;

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}

	public void setMaxVersions(int maxVersions) {
		this.maxVersions = maxVersions;
	}

	public void init() {

		logger.debug("MaxVersions is set to: " + maxVersions);

		this.afterCreateVersion = new JavaBehaviour(this, "afterCreateVersion",
				NotificationFrequency.TRANSACTION_COMMIT);

		this.policyComponent.bindClassBehaviour(QName.createQName(
				NamespaceService.ALFRESCO_URI, "afterCreateVersion"),
				MaxVersionPolicy.class, this.afterCreateVersion);
	}

	@Override
	public void afterCreateVersion(NodeRef versionableNode, Version version) {
		VersionHistory versionHistory = versionService
				.getVersionHistory(versionableNode);

		if (versionHistory != null) {
			logger.debug("Current number of versions: "
					+ versionHistory.getAllVersions().size());
			logger.debug("least recent/root version: "
					+ versionHistory.getRootVersion().getVersionLabel());

			// If the current number of versions in the VersionHistory is
			// greater
			// than the maxVersions limit, remove the root/least recent version
			if (versionHistory.getAllVersions().size() > maxVersions) {
				logger.debug("Removing Version: "
						+ versionHistory.getRootVersion().getVersionLabel());
				versionService.deleteVersion(versionableNode, versionHistory
						.getRootVersion());
			}
		} else {
			logger.debug("versionHistory does not exist");
		}

	}
}