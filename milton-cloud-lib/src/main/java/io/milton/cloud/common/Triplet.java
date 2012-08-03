package io.milton.cloud.common;

import java.util.List;

/**
 * Represents an entry in the filesystem, with a name, a hash and a 
 * unique meta ID
 *
 * @author brad
 */
public class Triplet implements ITriplet {
    
    public static boolean isDirectory(ITriplet t) {
        String type = t.getType();
        return type != null && type.equals("d"); // d for directory
    }
    
    private String name;
    private String hash;
    private String type;
    
    private List<Triplet> children;

    @Override
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }


    @Override
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

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
        
    public boolean isDirectory() {
        return Triplet.isDirectory(this);
    }
    
    
}
