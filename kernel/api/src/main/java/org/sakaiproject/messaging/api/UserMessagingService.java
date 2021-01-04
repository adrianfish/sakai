package org.sakaiproject.messaging.api;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sakaiproject.user.api.User;

public interface UserMessagingService {

    void message(Set<User> users, Message message, List<MessageMedium> media, Map<String, Object> replacements, int priority);
    boolean importTemplateFromXmlFile(InputStream templateResourceStream, String templateRegistrationKey);
}
