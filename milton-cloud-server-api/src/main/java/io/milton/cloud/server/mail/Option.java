 package io.milton.cloud.server.mail;

import java.util.List;

/**
 *
 * @author brad
 */


public class Option {
    
    public static void add(List<Option> list, String code, String text) {
        Option o = new Option(code, text);
        list.add(o);
    }
    
    public static void add(List<Option> list, long id, String text) {
        Option o = new Option(id+"", text);
        list.add(o);
    }    

    public static void add(List<Option> list, String code) {
        Option o = new Option(code, code);
        list.add(o);
    }    
    
    
    private String code;
    private String text;

    public Option(String code, String text) {
        this.code = code;
        this.text = text;
    }
        
    public String getCode() {
        return code;
    }

    public String getText() {
        return text;
    }
    
    
}
