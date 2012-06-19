package io.milton.cloud.server.web.templating;

import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang.StringUtils;

public class EncodeUtils {

    public static boolean isEmpty(Object val) {
        if (val == null) {
            return true;
        } else if( val instanceof List) {
            List list = (List) val;
            return list.isEmpty();
        } else if (val instanceof String) {
            String s = (String) val;
            return StringUtils.isBlank(s);
        } else {
            return false;
        }
    }

    /**
     * TODO: replace with JTidy or AntiSamy
     * http://jtidy.sourceforge.net/multiproject/jtidyservlet/clover/org/w3c/tidy/servlet/util/HTMLEncode.html
     * 
     *
     * @param s
     * @return
     */
    public static String encodeHTML(String s) {
//        s = s.replace("& ", "&amp; ");
//        StringBuilder out = new StringBuilder();
//        for (int i = 0; i < s.length(); i++) {
//            char c = s.charAt(i);
//            if (c > 127 || c == '"' || c == '<' || c == '>') {
//                out.append("&#" + (int) c + ";");
//            } else {
//                out.append(c);
//            }
//        }
//        return out.toString();
        return encode(s, "\n");
    }
    private static HashMap<String, String> entityTableEncode = null;
    private static final String[] ENTITIES = {
        ">",
        "&gt;",
        "<",
        "&lt;",
        "&",
        "&amp;",
        "\"",
        "&quot;",
        "'",
        "&#039;",
        "\\",
        "&#092;",
        "\u00a9",
        "&copy;",
        "\u00ae",
        "&reg;"};

    protected static synchronized void buildEntityTables() {
        entityTableEncode = new HashMap<String, String>(ENTITIES.length);

        for (int i = 0; i < ENTITIES.length; i += 2) {
            if (!entityTableEncode.containsKey(ENTITIES[i])) {
                entityTableEncode.put(ENTITIES[i], ENTITIES[i + 1]);
            }
        }
    }

    public static String encode(String s, String cr) {
        if (entityTableEncode == null) {
            buildEntityTables();
        }
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() * 2);
        char ch;
        for (int i = 0; i < s.length(); ++i) {
            ch = s.charAt(i);
            if ((ch >= 48 && ch <= 59) || (ch >= 63 && ch <= 90) || (ch >= 97 && ch <= 122) || (ch == ' ')) {
                sb.append(ch);
            } else if (ch == '\n') {
                sb.append(cr);
            } else {
                // cherry pick some safe non-seq chars
                if (ch == '(' || ch == ')' || ch == '+' || ch == '-' || ch == '*' || ch == '_') {
                    sb.append(cr);
                } else {
                    String chEnc = encodeSingleChar(String.valueOf(ch));
                    if (chEnc != null) {
                        sb.append(chEnc);
                    } else {
                        // Not 7 Bit use the unicode system
                        sb.append("&#");
                        sb.append(new Integer(ch).toString());
                        sb.append(';');
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String encodeSingleChar(String ch) {
        return (String) entityTableEncode.get(ch);
    }
}
