package io.milton.cloud.server.web.templating;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 *
 * @author brad
 */
public class DataBinderTest {
    
    private DataBinder dataBinder;
    private Map map;
    private MyBean bean;

    @Before
    public void setUp() throws Exception {
        dataBinder = new DataBinder();
        map = new HashMap();
        bean = new MyBean();
    }


    @Test
    public void testPopulate_String() throws Exception {
        map.put("s1", "astring");
        dataBinder.populate(bean, map);
        assertEquals("astring", bean.getS1());
    }

    @Test
    public void testPopulate_Int() throws Exception {
        map.put("i1", 111);
        dataBinder.populate(bean, map);
        assertEquals(111, bean.getI1());
    }
    
    @Test
    public void testPopulate_Date() throws Exception {
        Date d = new Date();
        map.put("date1", d);
        dataBinder.populate(bean, map);
        assertEquals(d, bean.getDate1());
    }
    
    //@Test
    public void testPopulate_StringToDate() throws Exception {
        map.put("date1", "16/07/12");
        dataBinder.populate(bean, map);
        assertNotNull(bean.getDate1());
        Date d = bean.getDate1();
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        assertEquals(2012, cal.get(Calendar.YEAR));
        assertEquals(6, cal.get(Calendar.MONTH)); // is zero-based
        assertEquals(16, cal.get(Calendar.DATE));
    }    
        
    @Test
    public void testPopulate_EmptyStringToNullDate() throws Exception {
        map.put("date1", "");
        dataBinder.populate(bean, map);
        assertNull(bean.getDate1());
    }      
    
    @Test
    public void testPopulate_StringToBigDecimal() throws Exception {
        map.put("bd", "1.3");
        dataBinder.populate(bean, map);
        assertEquals(new BigDecimal("1.3"), bean.getBd());
    }        
        
    
    public class MyBean {
        private String s1;
        private int i1;
        private Date date1;
        private BigDecimal bd;

        public BigDecimal getBd() {
            return bd;
        }

        public void setBd(BigDecimal bd) {
            this.bd = bd;
        }

        

        public int getI1() {
            return i1;
        }

        public void setI1(int i1) {
            this.i1 = i1;
        }

        public Date getDate1() {
            return date1;
        }

        public void setDate1(Date date1) {
            this.date1 = date1;
        }

        public String getS1() {
            return s1;
        }

        public void setS1(String s1) {
            this.s1 = s1;
        }
        
        
    }
}
