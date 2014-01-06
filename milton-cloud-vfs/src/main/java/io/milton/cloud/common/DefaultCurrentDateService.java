package io.milton.cloud.common;

import io.milton.http.DateUtils;
import static io.milton.http.DateUtils.parseDate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/**
 * Just returns a new Date to give the actual system time
 *
 * @author brad
 */
public class DefaultCurrentDateService implements CurrentDateService {

    private static final Date DEFAULT_TWO_DIGIT_YEAR_START;
    
    private static final Collection<String> DEFAULT_PATTERNS = Arrays.asList("dd/MM/yyyy HH:mm", "dd/MM/yyyy");
    
    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, Calendar.JANUARY, 1, 0, 0);
        DEFAULT_TWO_DIGIT_YEAR_START = calendar.getTime();
    }    
    
    public DefaultCurrentDateService() {
    }

    @Override
    public Date getNow() {
        return new Date();
    }

    @Override
    public Date parseDate(String s) throws DateUtils.DateParseException {
        // TODO: link to locale on Profile
        Collection<String> dateFormats = Arrays.asList("dd/MM/yyyy HH:mm", "dd/MM/yyyy");
        return parseDate(s, dateFormats);
    }

    // These functions were pinched from milton's DateUtls, but with timezone stuff removed
    
    public Date parseDate(String dateValue, Collection<String> dateFormats) throws DateUtils.DateParseException {
        return parseDate(dateValue, dateFormats, null);
    }

    public Date parseDate( String dateValue,Collection<String> dateFormats, Date startDate) throws DateUtils.DateParseException {

        if (dateValue == null) {
            throw new IllegalArgumentException("dateValue is null");
        }
        if (dateFormats == null) {
            dateFormats = DEFAULT_PATTERNS;
        }
        if (startDate == null) {
            startDate = DEFAULT_TWO_DIGIT_YEAR_START;
        }
        // trim single quotes around date if present
        // see issue #5279
        if (dateValue.length() > 1
                && dateValue.startsWith("'")
                && dateValue.endsWith("'")) {
            dateValue = dateValue.substring(1, dateValue.length() - 1);
        }

        SimpleDateFormat dateParser = null;
        Iterator<String> formatIter = dateFormats.iterator();

        while (formatIter.hasNext()) {
            String format = formatIter.next();
            if (dateParser == null) {
                dateParser = new SimpleDateFormat(format, Locale.US);
                //dateParser.setTimeZone(TimeZone.getTimeZone("GMT"));
                dateParser.set2DigitYearStart(startDate);
            } else {
                dateParser.applyPattern(format);
            }
            try {
                Date dt = dateParser.parse(dateValue);
                return dt;
            } catch (ParseException pe) {
                // ignore this exception, we will try the next format
            }
        }

        // we were unable to parse the date
        throw new DateUtils.DateParseException("Unable to parse the date: " + dateValue);
    }

}
