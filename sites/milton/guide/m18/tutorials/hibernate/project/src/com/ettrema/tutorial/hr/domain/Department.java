package com.ettrema.tutorial.hr.domain;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Department implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private static long nextId;
    
    private Long id;
    
    private String name;
     
    public static Department create(String name) {
    	Department d = new Department();
    	d.setId(nextId++);
    	d.setName(name);
    	return d;
    }


    @Column(length = 15)
    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    @Id
    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

}
