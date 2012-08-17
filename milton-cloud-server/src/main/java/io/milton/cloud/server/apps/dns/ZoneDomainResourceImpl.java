package io.milton.cloud.server.apps.dns;

import io.milton.dns.record.Record;
import io.milton.dns.resource.DomainResource;
import io.milton.dns.resource.DomainResourceRecord;
import io.milton.dns.resource.ZoneDomainResource;


import java.util.List;
import java.util.Set;




public class ZoneDomainResourceImpl extends DomainResourceImpl implements ZoneDomainResource{

	public ZoneDomainResourceImpl(String name) {
		super(name);
	}
	public ZoneDomainResourceImpl(String name, List<DomainResourceRecord> resourceRecords) {
		super(name, resourceRecords);
	}
	
	@Override
	public Set<DomainResource> getDomainResoures() {
		return null;
	}
}
