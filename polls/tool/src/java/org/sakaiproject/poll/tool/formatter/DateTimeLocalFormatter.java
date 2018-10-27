package org.sakaiproject.poll.tool.formatter;

import org.springframework.format.Formatter;
import org.springframework.lang.UsesJava8;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Resource;

import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.user.api.UserDirectoryService;

@UsesJava8
public class DateTimeLocalFormatter implements Formatter<Date> {

    @Resource(name = "org.sakaiproject.user.api.PreferencesService")
    private PreferencesService preferencesService;

    @Resource(name = "org.sakaiproject.user.api.UserDirectoryService")
    private UserDirectoryService userDirectoryService;

    @Override
    public Date parse(String text, Locale locale) throws ParseException {
        Objects.requireNonNull(text);
        if (text.length() > 0 && text.contains("T")) {
            // assuming ISO_LOCAL_DATE_TIME a la "2007-12-03T10:15:30"
            return Date.from(LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(ZoneId.systemDefault()).toInstant());
        }
        return new Date();
    }

    @Override
    public String print(Date object, Locale locale) {

        Preferences preferences = preferencesService.getPreferences(userDirectoryService.getCurrentUser().getId());
        String timezone = preferences.getProperties("sakai:time").getProperty(TimeService.TIMEZONE_KEY);
        return object.toInstant().atZone(ZoneId.of(timezone)).format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

}
