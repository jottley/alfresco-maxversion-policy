/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 * As a special exception to the terms and conditions of version 2.0 of the GPL, you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described in Alfresco's FLOSS exception. You should have
 * recieved a copy of the text describing the FLOSS exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.extension.versioning;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.version.VersionServicePolicies.AfterCreateVersionPolicy;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;


/**
 * 
 * @author Jared Ottley (jared.ottley@alfresco.com)
 * @version 0.5
 * 
 */

public class MaxVersionPolicy
    implements AfterCreateVersionPolicy
{

	private Logger logger = Logger.getLogger(MaxVersionPolicy.class);

	private PolicyComponent policyComponent;
	private VersionService versionService;

	// max number of versions per version node
	private int maxMinorVersions;
	private int keepIntermediateMinorVersions;
	private int maxMajorVersions;
	private int keepIntermediateMajorVersions;



    public void setPolicyComponent(PolicyComponent policyComponent)
    {
		this.policyComponent = policyComponent;
	}


    public void setVersionService(VersionService versionService)
    {
		this.versionService = versionService;
	}

	public void setMaxMajorVersions(int maxMajorVersions) {
		this.maxMajorVersions = maxMajorVersions;
	}

	public void setKeepIntermediateMajorVersions(
			int keepIntermediateMajorVersions) {
		this.keepIntermediateMajorVersions = keepIntermediateMajorVersions;
	}

	public void setMaxMinorVersions(int maxMinorVersions) {
		this.maxMinorVersions = maxMinorVersions;
	}

	public void setKeepIntermediateMinorVersions(
			int keepIntermediateMinorVersions) {
		this.keepIntermediateMinorVersions = keepIntermediateMinorVersions;
	}

	public void init() {
		logger.debug("maxMajorVersions is set to: " + maxMajorVersions);
		logger.debug("keepIntermediateMajorVersions is set to: "
				+ keepIntermediateMajorVersions);
		logger.debug("maxMinorVersions is set to: " + maxMinorVersions);
		logger.debug("keepIntermediateMinorVersions is set to: "
				+ keepIntermediateMinorVersions);
		Behaviour afterCreateVersion = new JavaBehaviour(this,
				"afterCreateVersion", NotificationFrequency.TRANSACTION_COMMIT);
		this.policyComponent.bindClassBehaviour(QName.createQName(
				NamespaceService.ALFRESCO_URI, "afterCreateVersion"),
				MaxVersionPolicy.class, afterCreateVersion);

		if (keepIntermediateMajorVersions > maxMajorVersions) {
			logger.warn("keepIntermediateMajorVersions shoud me lesser or equal of maxMajorVersions: maxMajorVersions will be used ("
					+ maxMajorVersions + ")");
			keepIntermediateMajorVersions = maxMajorVersions;
		}
		if (keepIntermediateMinorVersions > maxMinorVersions) {
			logger.warn("keepIntermediateMinorVersions shoud me lesser or equal of maxMinorVersions: maxMinorVersions will be used ("
					+ maxMinorVersions + ")");
			keepIntermediateMinorVersions = maxMinorVersions;
		}
	}

	@Override
	public void afterCreateVersion(NodeRef versionableNode, Version version) {
		// If maxVersions is zero, consider policy to be disabled
		if (maxMajorVersions == 0 && maxMinorVersions == 0) {
			logger.debug("maxMajorVersions and maxMinorVersions is set to zero, consider policy to be disabled");
			return;
		}

		VersionHistory versionHistory = versionService
				.getVersionHistory(versionableNode);

		if (versionHistory != null) {

			Collection<Version> versions = versionHistory.getAllVersions();
			if (logger.isDebugEnabled()) {
				if (versions.size() > 0) {
					if (!versions.iterator().next()
							.equals(versionHistory.getHeadVersion())) {
						throw new IllegalStateException(
								"The first version returned by versionHistory.getAllVersions() should be the latest.");
					}
				}
				StringBuilder sb = new StringBuilder();
				Iterator<Version> it = versions.iterator();
				while (it.hasNext()) {
					sb.append(it.next().getVersionLabel());
					if (it.hasNext()) {
						sb.append(", ");
					}
				}
				logger.debug("Current versions: ("
						+ versionHistory.getAllVersions().size() + ") "
						+ sb.toString());
			}
			
			List<Version> majorVersions = new LinkedList<>();
			List<Version> minorVersions = new LinkedList<>();

			for (Version v : versions) {
				switch (v.getVersionType()) {
				case MAJOR:
					majorVersions.add(v);
					break;
				case MINOR:
					minorVersions.add(v);
					break;
				default:
					throw new IllegalStateException(
							"a version shoul be Major or Minor");
				}
			}

			enforceMaxVersions(versionableNode, VersionType.MAJOR,
					majorVersions, maxMajorVersions,
					keepIntermediateMajorVersions);
			enforceMaxVersions(versionableNode, VersionType.MINOR,
					minorVersions, maxMinorVersions,
					keepIntermediateMinorVersions);
		} else {
			logger.debug("versionHistory does not exist");
		}
	}

	private void enforceMaxVersions(NodeRef versionableNode,
			VersionType versionType, List<Version> versions, int maxVersions,
			int keepIntermediateVersions) {
		if (maxVersions > 0 && versions.size() > maxVersions) {
			Version toBeRemoved = null;

			while (versions.size() > maxVersions) {
				if (keepIntermediateVersions <= 0) {
					// if we do not need to keep intermediate vesions we simply
					// remove the last one (the older)
					toBeRemoved = versions.get(versions.size() - 1);
				} else {
					// skip the first version (the most recent one) that sould
					// not be removed
					// start from the end of the list (the oldest version)
					ListIterator<Version> it = versions.subList(1,
							versions.size()).listIterator(versions.size() - 1);
					// until we found the first version we can remove (scanning
					// from the oldest to the newest ones)
					while (it.hasPrevious()) {
						Version version = it.previous();
						if (toBeRemoved == null) {
							// if we dont find another version we can safely
							// rmove, we remove the last one
							toBeRemoved = version;
						}
						int versionNumber;
						switch (versionType) {
						case MAJOR:
							// the number before the dot (2.3 -> 2)
							versionNumber = Integer.parseInt(version
									.getVersionLabel().split("\\.")[0]);
							break;
						case MINOR:
						default:
							// the number after the dot (2.3 -> 3)
							versionNumber = Integer.parseInt(version
									.getVersionLabel().split("\\.")[1]);
							break;
						}
						// if the module is 0 we must try to keep this version
						if (versionNumber % keepIntermediateVersions == 0) {
							// this version should be kept, continue to search
							continue;
						} else {
							// this is the first version we can remove safely
							toBeRemoved = version;
							break;
						}
					}
				}

				if (toBeRemoved != null) {
					logger.debug("Removing Version: "
							+ toBeRemoved.getVersionLabel());
					versionService.deleteVersion(versionableNode, toBeRemoved);
					versions.remove(toBeRemoved);
					toBeRemoved = null;
				} else {
					throw new IllegalStateException(
							"Unable to find a version to be removed");
				}
			}
		}
	}
}
