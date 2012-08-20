package com.ettrema.tutorial.hr.web;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;
import com.ettrema.tutorial.hr.domain.Department;

public class DepartmentResource implements PropFindableResource, CollectionResource{

	private final HrResourceFactory resourceFactory;
	private final Department department;
	
	public DepartmentResource(HrResourceFactory resourceFactory, Department department) {
		this.resourceFactory = resourceFactory;
		this.department = department;
	}	
	
	@Override
	public Date getCreateDate() {
		// Unknown
		return null;
	}

	@Override
	public Object authenticate(String user, String pwd) {
		// always allow
		return user;
	}

	@Override
	public boolean authorise(Request arg0, Method arg1, Auth arg2) {
		// Always allow
		return true;
	}

	@Override
	public String checkRedirect(Request arg0) {
		// No redirects
		return null;
	}

	@Override
	public Date getModifiedDate() {
		// Unknown
		return null;
	}

	@Override
	public String getName() {
		return department.getName();
	}

	@Override
	public String getRealm() {
		return HrResourceFactory.REALM;
	}

	@Override
	public String getUniqueId() {
		return department.getId().toString();
	}

	@Override
	public Resource child(String name) {
		return null;
	}

	@Override
	public List<? extends Resource> getChildren() {
		// TODO
		return Collections.EMPTY_LIST;
	}

}
