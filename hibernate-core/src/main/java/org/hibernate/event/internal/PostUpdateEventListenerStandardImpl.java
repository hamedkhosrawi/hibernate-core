/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class PostUpdateEventListenerStandardImpl implements PostUpdateEventListener, CallbackRegistryConsumer {
	private CallbackRegistry callbackRegistry;

	@Override
	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		Object entity = event.getEntity();
		EventSource eventSource = event.getSession();
		handlePostUpdate(entity, eventSource);
	}

	private void handlePostUpdate(Object entity, EventSource source) {
		EntityEntry entry = source.getPersistenceContext().getEntry( entity );
		// mimic the preUpdate filter
		if ( Status.DELETED != entry.getStatus()) {
			callbackRegistry.postUpdate(entity);
		}
	}

	@Override
	public boolean requiresPostCommitHandling(EntityTypeDescriptor descriptor) {
		return callbackRegistry.hasRegisteredCallbacks( descriptor.getMappedClass(), CallbackType.POST_UPDATE );
	}
}
