package io.milton.cloud.common;

import java.util.List;

/**
 * Represents an entry in the filesystem, with a name, a hash and a 
 * unique meta ID
 *
 * @author brad
 */
public class Triplet {
    private String name;
    private long hash;
    private String type;
    
    private List<Triplet> children;

    public long getHash() {
        return hash;
    }

    public void setHash(long hash) {
        this.hash = hash;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Triplet> getChildren() {
        return children;
    }

    public void setChildren(List<Triplet> children) {
        this.children = children;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
        
    public boolean isDirectory() {
        return type != null && type.equals("d"); // d for directory
    }
    
    
}
