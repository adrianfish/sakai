package org.sakaiproject.messaging.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;

import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.emailtemplateservice.api.RenderedTemplate;
import org.sakaiproject.emailtemplateservice.api.EmailTemplateService;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.messaging.api.Message;
import org.sakaiproject.messaging.api.MessageMedium;
import org.sakaiproject.messaging.api.UserMessagingService;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.user.api.User;

public class UserMessagingServiceImpl implements UserMessagingService {

    @Resource private EmailService emailService;
    @Resource private EmailTemplateService emailTemplateService;
    @Resource private PreferencesService preferencesService;

    private ExecutorService executor;

    public void init() {
        executor = Executors.newFixedThreadPool(20);
    }

    public void destroy() {
        executor.shutdownNow();
    }

    /*
    public void message(String userId, Message message, List<MessageMedium> media, Map<String, String> replacements, int priority) {
        message(new HashSet<String>(Arrays.asList(new String[] { userId })), message, media, replacements, priority);
    }
    */

    public void message(Set<User> users, Message message, List<MessageMedium> media, Map<String, Object> replacements, int priority) {

        executor.execute(() -> {

            String tool = message.getTool();
            String type = message.getType();

            // Check with the preferences service if userId wants to get messages from this tool
            // ...

            users.forEach(user -> {

                Locale locale = preferencesService.getLocale(user.getId());

                media.forEach(m -> {

                    switch (m) {
                        case EMAIL:
                            if (NotificationService.NOTI_REQUIRED == priority) {
                                RenderedTemplate renderedTemplate = emailTemplateService.getRenderedTemplate(tool + "." + type, locale, replacements); 
                                emailService.sendToUser(user, renderedTemplate.getHeaders(), renderedTemplate.getRenderedMessage());
                            }
                            // Check with the prefs service if userId wants email from this tool
                            // ...
                            // Lookup the email template using message.getTool() and message.getType() 
                            // ...
                            // Send email via email service
                            // ...
                            break;
                        case BROWSER:
                            // Check with the prefs service if userId wants browser alerts from this tool
                            // ...
                            
                            // Send message to bullhorns (browser push service)
                            // ...
                            break;
                        case SMS:
                            // Check with the prefs service if userId wants sms alerts from this tool
                            // ...
                            
                            // Send message to sms gateway
                            // ...
                            break;
                        default:
                    }
                });
            });
        });
    }

    /**
     * Registers a new template with the service, defined by the given XML file
     *
     * @param templateResourceStream the resource stream for the XML file
     * @param templateRegistrationKey the key (name) to register the template under
     * @return true if the template was registered
     */
    @Override
    public boolean importTemplateFromXmlFile(InputStream templateResourceStream, String templateRegistrationKey) {
        return emailTemplateService.importTemplateFromXmlFile(templateResourceStream, templateRegistrationKey);
    }
}
