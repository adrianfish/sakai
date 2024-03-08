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

package org.sakaiproject.lessonbuildertool.tool.producers;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import uk.org.ponder.localeutil.LocaleGetter;
import uk.org.ponder.messageutil.MessageLocator;
import uk.org.ponder.rsf.components.UIBranchContainer;
import uk.org.ponder.rsf.components.UICommand;
import uk.org.ponder.rsf.components.UIContainer;
import uk.org.ponder.rsf.components.UIForm;
import uk.org.ponder.rsf.components.UILink;
import uk.org.ponder.rsf.components.UIOutput;
import uk.org.ponder.rsf.components.UIInput;
import uk.org.ponder.rsf.components.UISelect;
import uk.org.ponder.rsf.components.UISelectChoice;
import uk.org.ponder.rsf.components.UIInternalLink;
import uk.org.ponder.rsf.components.decorators.UIFreeAttributeDecorator;
import uk.org.ponder.rsf.components.decorators.UIStyleDecorator;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCase;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCaseReporter;
import uk.org.ponder.rsf.view.ComponentChecker;
import uk.org.ponder.rsf.view.ViewComponentProducer;
import uk.org.ponder.rsf.viewstate.SimpleViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParamsReporter;

import org.sakaiproject.lessonbuildertool.SimplePage;
import org.sakaiproject.lessonbuildertool.SimplePageItem;
import org.sakaiproject.lessonbuildertool.service.LessonEntity;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean.UrlItem;
import org.sakaiproject.lessonbuildertool.tool.view.GeneralViewParameters;
import org.sakaiproject.lessonbuildertool.model.SimplePageToolDao;
import org.sakaiproject.tool.cover.SessionManager;

@Slf4j
@Setter
public class ConversationsTopicPickerProducer implements ViewComponentProducer, NavigationCaseReporter, ViewParamsReporter {

  public static final String VIEW_ID = "ConversationsTopicPicker";

  private SimplePageBean simplePageBean;
  private SimplePageToolDao simplePageToolDao;
  public MessageLocator messageLocator;
  public LocaleGetter localeGetter;

  public String getViewID() {
    return VIEW_ID;
  }

  public void fillComponents(UIContainer tofill, ViewParameters viewparams, ComponentChecker checker) {

    if (((GeneralViewParameters) viewparams).getSendingPage() != -1) {
      // will fail if page not in this site
      // security then depends upon making sure that we only deal with this page
      try {
        simplePageBean.updatePageObject(((GeneralViewParameters) viewparams).getSendingPage());
      } catch (Exception e) {
        log.info("ConversationsTopicPicker permission exception: {}", e.toString());
        return;
      }
    }

    UIOutput.make(tofill, "html").decorate(new UIFreeAttributeDecorator("lang", localeGetter.get().getLanguage()))
        .decorate(new UIFreeAttributeDecorator("xml:lang", localeGetter.get().getLanguage()));

    Long itemId = ((GeneralViewParameters) viewparams).getItemId();

    simplePageBean.setItemId(itemId);

    if (simplePageBean.canEditPage()) {

      SimplePage page = simplePageBean.getCurrentPage();

      String currentItem = null; // default value, normally current
      // if itemid is null, we'll append to current page, so it's ok
      if (itemId != null && itemId != -1) {
        SimplePageItem i = simplePageToolDao.findItem(itemId);
        if (i == null) {
          return;
        }
        // trying to hack on item not on this page
        if (i.getPageId() != page.getPageId()) {
          return;
        }
        currentItem = i.getSakaiId();
      }

			UIForm form = UIForm.make(tofill, "conversations-topic-picker-form");
			Object sessionToken = SessionManager.getCurrentSession().getAttribute("sakai.csrf.token");
			if (sessionToken != null) {
        UIInput.make(form, "csrf", "simplePageBean.csrfToken", sessionToken.toString());
      }

      UIOutput.make(form, "conversations-topic-picker")
        .decorate(new UIFreeAttributeDecorator("site-id", simplePageBean.getCurrentSiteId()))
        .decorate(new UIFreeAttributeDecorator("endpoint", "/api/sites/" + page.getSiteId() + "/lessons/pages/" + page.getPageId() + "/embedded-items"));

      UIInput.make(form, "item-id", "#{simplePageBean.itemId}");
      UIInput.make(form, "add-before", "#{simplePageBean.addBefore}", ((GeneralViewParameters) viewparams).getAddBefore());

			UICommand.make(form, "submit", messageLocator.getMessage("simplepage.chooser.select"), "#{simplePageBean.addConversationsTopic}");
			UICommand.make(form, "cancel", messageLocator.getMessage("simplepage.cancel"), "#{simplePageBean.cancel}");
    }
  }

  public ViewParameters getViewParameters() {
    return new GeneralViewParameters();
  }

  public List reportNavigationCases() {
    List<NavigationCase> togo = new ArrayList<NavigationCase>();
    togo.add(new NavigationCase("success", new SimpleViewParameters(ShowPageProducer.VIEW_ID)));
    togo.add(new NavigationCase("failure", new SimpleViewParameters(ConversationsTopicPickerProducer.VIEW_ID)));
    togo.add(new NavigationCase("cancel", new SimpleViewParameters(ShowPageProducer.VIEW_ID)));
    return togo;
  }
}
