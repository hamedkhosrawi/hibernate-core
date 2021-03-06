/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.event.spi;

import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

/**
 * Envers-specific entity (pre) update event listener.
 *
 * @author Chris Cranford
 */
public class EnversPreUpdateEventListenerImpl extends BaseEnversUpdateEventListener implements PreUpdateEventListener {
	public EnversPreUpdateEventListenerImpl(AuditService auditService) {
		super( auditService );
	}

	@Override
	public boolean onPreUpdate(PreUpdateEvent event) {
		final String entityName = event.getDescriptor().getEntityName();
		if ( getAuditService().getEntityBindings().isVersioned( entityName ) ) {
			checkIfTransactionInProgress( event.getSession() );
			if ( isDetachedEntityUpdate( entityName, event.getOldState() ) ) {
				final AuditProcess auditProcess = getAuditService().getAuditProcess( event.getSession() );
				auditProcess.cacheEntityState(
						event.getId(),
						entityName,
						event.getDescriptor().getDatabaseSnapshot( event.getId(), event.getSession() )
				);
			}
		}
		return false;
	}
}
