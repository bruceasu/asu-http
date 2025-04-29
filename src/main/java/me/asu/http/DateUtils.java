package me.asu.http;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateUtils {
    static final String[] DATE_PATTERNS = {
            "EEE, dd MMM yyyy HH:mm:ss z", // [RFC9110#5.6.7] IMF-fixdate format
            "EEEE, dd-MMM-yy HH:mm:ss z",  // obsolete RFC 850 format
            "EEE MMM d HH:mm:ss yyyy",      // ANSI C's asctime() format
            "yyyy-MM-dd'T'HH:mm:sssX",
            "yyyy-MM-dd HH:mm:ss z",
            "yyyy/MM/dd HH:mm:ss z",
            "yyyy.MM.dd HH:mm:ss z",
    };

    /**
     * A GMT (UTC) timezone instance.
     */
    static final TimeZone GMT = TimeZone.getTimeZone("GMT");
//        static final TimeZone JST = TimeZone.getTimeZone("Asia/Tokyo");
//        static final TimeZone CST = TimeZone.getTimeZone("Asia/Shanghai");
//        static final TimeZone EST = TimeZone.getTimeZone("America/New_York");

    /**
     * Date format strings.
     */
    static final char[]
            DAYS   = "Sun Mon Tue Wed Thu Fri Sat".toCharArray(),
            MONTHS = "Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec".toCharArray();

    /**
     * Parses a date string in one of the supported {@link #DATE_PATTERNS}.
     * <p>
     * Received date header values must be in one of the following formats:
     * Sun, 06 Nov 1994 08:49:37 GMT  ; IMF-fixdate
     * Sunday, 06-Nov-94 08:49:37 GMT ; obsolete RFC 850 format
     * Sun Nov  6 08:49:37 1994       ; ANSI C's asctime() format
     *
     * @param time a string representation of a time value
     * @return the parsed date value
     * @throws IllegalArgumentException if the given string does not contain
     *                                  a valid date format in any of the supported formats
     */
    public static Date parseDate(String time) {
        // [RFC9110#5.6.7] interpret 2-digit years >50 years in future as past,
        // SDF defaults to >20 years which covers it (see set2DigitYearStart)
        for (String pattern : DATE_PATTERNS) {
            try {
                SimpleDateFormat df = new SimpleDateFormat(pattern, Locale.US);
                df.setLenient(false);
                df.setTimeZone(GMT);
                return df.parse(time);
            } catch (ParseException ignore) {}
        }
        throw new IllegalArgumentException("invalid date format: " + time);
    }

    /**
     * Formats the given time value as a string in IMF-fixdate format.
     *
     * @param time the time in milliseconds since January 1, 1970, 00:00:00 GMT
     * @return the given time value as a string in IMF-fixdate format
     */
    public static String formatDate(long time) {
        // this implementation performs far better than SimpleDateFormat instances, and even
        // quite better than ThreadLocal SDFs - the server's CPU-bound benchmark gains over 20%!
        if (time < -62167392000000L || time > 253402300799999L)
            throw new IllegalArgumentException("year out of range (0001-9999): " + time);
        char[]   s   = "DAY, 00 MON 0000 00:00:00 GMT".toCharArray(); // copy the format template
        Calendar cal = new GregorianCalendar(GMT, Locale.US);
        cal.setTimeInMillis(time);
        System.arraycopy(DAYS, 4 * (cal.get(Calendar.DAY_OF_WEEK) - 1), s, 0, 3);
        System.arraycopy(MONTHS, 4 * cal.get(Calendar.MONTH), s, 8, 3);
        int n = cal.get(Calendar.DATE);
        s[5] += n / 10;
        s[6] += n % 10;
        n = cal.get(Calendar.YEAR);
        s[12] += n / 1000;
        s[13] += n / 100 % 10;
        s[14] += n / 10 % 10;
        s[15] += n % 10;
        n = cal.get(Calendar.HOUR_OF_DAY);
        s[17] += n / 10;
        s[18] += n % 10;
        n = cal.get(Calendar.MINUTE);
        s[20] += n / 10;
        s[21] += n % 10;
        n = cal.get(Calendar.SECOND);
        s[23] += n / 10;
        s[24] += n % 10;
        return new String(s);
    }
}