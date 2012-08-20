package com.ettrema.tutorial.hr.web;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import com.ettrema.tutorial.hr.domain.Department;


public class HrResourceFactory implements ResourceFactory {
	
	private Logger log = LoggerFactory.getLogger(HrResourceFactory.class);
	
	public static final String REALM = "MyCompany";
	
	private final SessionFactory sessionFactory;
	
	public HrResourceFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}	
	
	@Override
	public Resource getResource(String host, String p) {		
		Path path = Path.path(p).getStripFirst();
		log.debug("getResource: " + path);
		Session session = sessionFactory.openSession();
		if( path.isRoot() ) {
			return new AllDepartmentsResource(this, session); 
		} else if( path.getLength() == 1 ) {
			return findDepartment(path.getName(), session);
		} else if( path.getLength() == 2) {
			// TODO
			return null;
		} else {
			return null;
		}
	}

	public List<Resource> findAllDepartments(Session session) {
		Criteria crit = session.createCriteria(Department.class);
		List list = crit.list();
		if( list == null || list.size() == 0) {
			return Collections.EMPTY_LIST;
		} else {
			List<Resource> departments = new ArrayList<Resource>();
			for( Object o : list ) {
				departments.add( new DepartmentResource(this, (Department)o) );
			}
			return departments;
		}
		
	}
	
	public Resource findDepartment(String name, Session session) {
		log.debug("findDepartment: " + name);
		Criteria crit = session.createCriteria(Department.class);
		crit.add(Expression.eq("name", name));
		List list = crit.list();
		if( list == null || list.size() == 0 ) {
			log.debug("not found");
			return null;
		} else {
			Department d = (Department) list.get(0);
			log.debug("found: " + d.getName());
			return new DepartmentResource(this, d);
		}
	}

}
