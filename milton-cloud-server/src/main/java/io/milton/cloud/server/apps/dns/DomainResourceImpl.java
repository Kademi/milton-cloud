package io.milton.cloud.server.apps.dns;
import io.milton.dns.resource.DomainResource;
import io.milton.dns.resource.DomainResourceRecord;

import java.util.ArrayList;
import java.util.List;



public class DomainResourceImpl implements DomainResource{

	private String name;
	private List<DomainResourceRecord> resourceRecords = new ArrayList<>();
	
	public DomainResourceImpl(String name) {
		this.name  = name;
	}
	public DomainResourceImpl(String name, List<DomainResourceRecord> rrs) {
		this.name  = name;
		this.resourceRecords.addAll(rrs);
	}
	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<DomainResourceRecord> getRecords() {
		return resourceRecords;
	}
	
	public void addRecord(DomainResourceRecord rr) {
		resourceRecords.add(rr);
	}

}
