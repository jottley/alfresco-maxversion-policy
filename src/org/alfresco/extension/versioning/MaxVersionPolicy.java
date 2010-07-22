package org.alfresco.extension.versioning;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.PolicyScope;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.version.VersionServicePolicies.OnCreateVersionPolicy;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

public class MaxVersionPolicy implements OnCreateVersionPolicy {

	private Logger logger = Logger.getLogger(MaxVersionPolicy.class);

	private PolicyComponent policyComponent;
	private VersionService versionService;

	// max number of versions per versioned node
	private int maxVersions;

	private Behaviour onCreateVersion;

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

		this.onCreateVersion = new JavaBehaviour(this, "onCreateVersion",
				NotificationFrequency.TRANSACTION_COMMIT);

		this.policyComponent.bindClassBehaviour(QName.createQName(
				NamespaceService.ALFRESCO_URI, "onCreateVersion"),
				MaxVersionPolicy.class, this.onCreateVersion);
	}

	@Override
	public void onCreateVersion(QName classRef, NodeRef nodeRef,
			Map<String, Serializable> versionProperties, PolicyScope nodeDetails) {

		VersionHistory versionHistory = versionService
				.getVersionHistory(nodeRef);

		logger.debug("Current number of versions: "
				+ versionHistory.getAllVersions().size());
		logger.debug("least recent/root version: "
				+ versionHistory.getRootVersion().getVersionLabel());

		// If the current number of versions in the VersionHistory is greater
		// than the maxVersions limit, remove the root/least recent version
		if (versionHistory.getAllVersions().size() > maxVersions) {
			logger.debug("Removing Version: "
					+ versionHistory.getRootVersion().getVersionLabel());
			versionService.deleteVersion(nodeRef, versionHistory
					.getRootVersion());
		}
	}
}