/**********************************************************************************
 * $URL: $
 * $Id: $
 ***********************************************************************************
 *
 * Author: Charles Hedrick, hedrick@rutgers.edu
 *
 * Copyright (c) 2010 Rutgers, the State University of New Jersey
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");                                                                
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.lessonbuildertool.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.sakaiproject.conversations.api.ConversationsPermissionsException;
import org.sakaiproject.conversations.api.ConversationsReferenceReckoner;
import org.sakaiproject.conversations.api.ConversationsService;
import org.sakaiproject.conversations.api.beans.TopicTransferBean;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.id.cover.IdManager;
import org.sakaiproject.lessonbuildertool.SimplePageItem;
import org.sakaiproject.lessonbuildertool.model.SimplePageToolDao;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean.UrlItem;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.cover.ToolManager;

import lombok.extern.slf4j.Slf4j;
import uk.org.ponder.messageutil.MessageLocator;

@Slf4j
public class ConversationsTopicEntity implements LessonEntity {

    protected static final int DEFAULT_EXPIRATION = 10 * 60;

    static ConversationsService conversationsService
        = (ConversationsService) ComponentManager.get("org.sakaiproject.conversations.api.ConversationsService");

    private SimplePageBean simplePageBean;

    static MessageLocator messageLocator = null;
    public void setMessageLocator(MessageLocator m) {
        messageLocator = m;
    }

    static AuthzGroupService authzGroupService = null;
    public void setAuthzGroupService(AuthzGroupService service) {
        authzGroupService = service;
    }

    private String id;
    private TopicTransferBean topic = null;

    // to create bean. the bean is used only to call the pseudo-static
    // methods such as getEntitiesInSite. So type, id, etc are left uninitialized

    public ConversationsTopicEntity() {
    }

    public ConversationsTopicEntity(TopicTransferBean topicBean) {

        this.id = topicBean.id;
        this.topic = topicBean;
    }

    public String getToolId() {
        return "sakai.conversations";
    }

    // type of the underlying object
    public int getType() {
        return TYPE_CONVERSATIONS_TOPIC;
    }

    public int getTypeOfGrade() {
        return 1;
    }

    public String getReference() {
        return topic.reference;
    }

    public List<LessonEntity> getEntitiesInSite() {    
        return getEntitiesInSite(null);
    }

    public List<LessonEntity> getEntitiesInSite(SimplePageBean bean) {    
        
        String siteId = ToolManager.getCurrentPlacement().getContext();
        Site site = null;
        try {
            site = SiteService.getSite(siteId);
        } catch (Exception impossible) {
            return Collections.EMPTY_LIST;
        }

        List<LessonEntity> ret = new ArrayList<>();
            
        ToolConfiguration tool = site.getToolForCommonId("sakai.conversations");
        
        if (tool == null) {
            return ret;
        }

        try {
            ret = conversationsService.getTopicsForSite(siteId).stream()
                .filter(t -> !t.draft).map(ConversationsTopicEntity::new).collect(Collectors.toList());
        } catch (ConversationsPermissionsException cpe) {
            log.warn("No permissions to get topics for site {}", siteId);
        }

        return ret;
    }

    public LessonEntity getEntity(String ref, SimplePageBean o) {
        return getEntity(ref);
    }

    public LessonEntity getEntity(String ref) {

        if (!ref.startsWith(ConversationsService.REFERENCE_ROOT)) return null;

        String topicId = ConversationsReferenceReckoner.reckoner().reference(ref).reckon().getId();

        return getTopic(topicId).map(ConversationsTopicEntity::new).orElse(null);
    }

    public TopicTransferBean getTopicById(String id) {
        return getTopic(id).orElse(null);
    }

    public String getTitle() {
        return getTopic(id).map(t-> t.title).orElse(null);
    }

    public String getUrl() {

        return getTopic(id).map(t-> t.url).orElse(null);
    }

    // I don't think they have this
    public Date getDueDate() {
        return getTopic(id).map(t-> Date.from(t.dueDate)).orElse(null);
    }

    // URL to create a new item. Normally called from the generic entity, not a specific one                                                 
    // can't be null                                                                                                                         
    public List<UrlItem> createNewUrls(SimplePageBean bean) {

        List<UrlItem> list = new ArrayList<>();
        /*
        String tool = bean.getCurrentTool("sakai.conversations");
        if (tool != null) {
            tool = ServerConfigurationService.getToolUrl() + "/" + tool + "/discussionConversationsTopic/forumsOnly/dfConversationsTopics";
            list.add(new UrlItem(tool, messageLocator.getMessage("simplepage.create_forums")));
        }
        if (nextEntity != null) {
            list.addAll(nextEntity.createNewUrls(bean));
        }
        */
        return list;
    }

    public String editItemUrl(SimplePageBean bean) {
        return getUrl();
    }

    // for most entities editItem is enough, however tests allow separate editing of                                                         
    // contents and settings. This will be null except in that situation                                                                     
    public String editItemSettingsUrl(SimplePageBean bean) {
        return null;
    }

    public boolean objectExists() {
        return getTopic(id).isPresent();
    }

    public boolean notPublished(String ref) {

        String id = ConversationsReferenceReckoner.reckoner().reference(ref).reckon().getId();
        return getTopic(id).map(t -> t.draft || t.hidden).orElse(true);
    }

    public boolean notPublished() {
        return getTopic(id).map(t -> t.draft || t.hidden).orElse(true);
    }

    public String getObjectId() {

        String title = getTitle();

        if (title == null) return null;

        return "conversations_topic/" + id + "/" + title;
    }

    public String findObject(String objectid, Map<String,String>objectMap, String siteid) {
        return null;
    }

    public String getSiteId() {
        return getTopic(id).map(t -> t.siteId).orElse(null);
    }

    @Override
    public void setSimplePageBean(SimplePageBean simplePageBean) {
        this.simplePageBean = simplePageBean;
    }

    private Optional<TopicTransferBean> getTopic(String id) {

        try {
            return conversationsService.getTopic(id);
        } catch (ConversationsPermissionsException cpe) {
            log.warn("No permission to get topic {}" , id);
        }

        return Optional.empty();
    }
}
