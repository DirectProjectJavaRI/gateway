package org.nhindirect.stagent;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.nhindirect.common.audit.AuditContext;
import org.nhindirect.common.audit.AuditEvent;
import org.nhindirect.common.audit.impl.NoOpAuditor;

public class MockAuditor extends NoOpAuditor
{
	private final Map<AuditEvent, Collection<? extends AuditContext>> events = new HashMap<AuditEvent, Collection<? extends AuditContext>>();
	
	@Override
	public void writeEvent(UUID eventId, Calendar eventTimeStamp, String principal, AuditEvent event, Collection<? extends AuditContext> contexts)
	{
		events.put(event, contexts);
	}
	
	public Map<AuditEvent, Collection<? extends AuditContext>> getEvents()
	{
		return events;
	}
}
