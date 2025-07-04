/**********************************************************************************
 * $URL: $
 * $Id: $
 ***********************************************************************************
 *
 * This was was originally part of Simple Page Tool and was
 *
 * Copyright (c) 2007 Sakai Project/Sakai Foundation
 * Licensed under the Educational Community License version 1.0
 *
 * The author was Joshua Ryan josh@asu.edu
 *
 * However this version is primarily new code. The new code is
 *
 * Copyright (c) 2010 Rutgers, the State University of New Jersey
 *
 * Author: Eric Jeney, jeney@rutgers.edu
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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.UsageSession;
import org.sakaiproject.event.cover.UsageSessionService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.lessonbuildertool.ChecklistItemStatus;
import org.sakaiproject.lessonbuildertool.ChecklistItemStatusImpl;
import org.sakaiproject.lessonbuildertool.SimpleChecklistItem;
import org.sakaiproject.lessonbuildertool.SimplePage;
import org.sakaiproject.lessonbuildertool.SimplePageComment;
import org.sakaiproject.lessonbuildertool.SimplePageItem;
import org.sakaiproject.lessonbuildertool.SimplePageLogEntry;
import org.sakaiproject.lessonbuildertool.SimplePagePeerEvalResult;
import org.sakaiproject.lessonbuildertool.SimplePageQuestionAnswer;
import org.sakaiproject.lessonbuildertool.SimplePageQuestionResponse;
import org.sakaiproject.lessonbuildertool.SimplePageQuestionResponseTotals;
import org.sakaiproject.lessonbuildertool.SimpleStudentPage;
import org.sakaiproject.lessonbuildertool.model.SimplePageToolDao;
import org.sakaiproject.lessonbuildertool.service.BltiInterface;
import org.sakaiproject.lessonbuildertool.service.GradebookIfc;
import org.sakaiproject.lessonbuildertool.service.LessonBuilderAccessService;
import org.sakaiproject.lessonbuildertool.service.LessonEntity;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean.BltiTool;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean.GroupEntry;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean.Status;
import org.sakaiproject.lessonbuildertool.tool.evolvers.SakaiFCKTextEvolver;
import org.sakaiproject.lessonbuildertool.tool.view.CommentsGradingPaneViewParameters;
import org.sakaiproject.lessonbuildertool.tool.view.CommentsViewParameters;
import org.sakaiproject.lessonbuildertool.tool.view.ExportCCViewParameters;
import org.sakaiproject.lessonbuildertool.tool.view.FilePickerViewParameters;
import org.sakaiproject.lessonbuildertool.tool.view.GeneralViewParameters;
import org.sakaiproject.lessonbuildertool.tool.view.QuestionGradingPaneViewParameters;
import org.sakaiproject.lessonbuildertool.util.LessonConditionUtil;
import org.sakaiproject.lessonbuildertool.util.SimplePageItemUtilities;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.portal.util.CSSUtils;
import org.sakaiproject.portal.util.PortalUtils;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.time.api.UserTimeService;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.tool.api.ToolSession;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.api.FormattedText;
import org.sakaiproject.util.comparator.UserSortNameComparator;
import uk.org.ponder.localeutil.LocaleGetter;
import uk.org.ponder.messageutil.MessageLocator;
import uk.org.ponder.rsf.builtin.UVBProducer;
import uk.org.ponder.rsf.components.*;
import uk.org.ponder.rsf.components.decorators.UIDisabledDecorator;
import uk.org.ponder.rsf.components.decorators.UIFreeAttributeDecorator;
import uk.org.ponder.rsf.components.decorators.UIStyleDecorator;
import uk.org.ponder.rsf.components.decorators.UITooltipDecorator;
import uk.org.ponder.rsf.evolvers.FormatAwareDateInputEvolver;
import uk.org.ponder.rsf.evolvers.TextInputEvolver;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCase;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCaseReporter;
import uk.org.ponder.rsf.view.ComponentChecker;
import uk.org.ponder.rsf.view.DefaultView;
import uk.org.ponder.rsf.view.ViewComponentProducer;
import uk.org.ponder.rsf.viewstate.SimpleViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParamsReporter;

import javax.crypto.Cipher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.lessonbuildertool.service.AssignmentEntity;
import org.sakaiproject.site.api.Group;

/**
 * This produces the primary view of the page. It also handles the editing of
 * the properties of most of the items (through JQuery dialogs).
 * 
 * @author Eric Jeney <jeney@rutgers.edu>
 */
@Slf4j
public class ShowPageProducer implements ViewComponentProducer, DefaultView, NavigationCaseReporter, ViewParamsReporter {
	String reqStar = "<span class=\"reqStar\">*</span>";
	
	private SimplePageBean simplePageBean;
	private SimplePageToolDao simplePageToolDao;
	@Setter private GradebookIfc gradebookIfc;
	@Setter private AuthzGroupService authzGroupService;
	@Setter private SecurityService securityService;
	@Setter ContentHostingService contentHostingService;
	private FormatAwareDateInputEvolver dateevolver;
	@Setter private UserTimeService userTimeService;
	@Setter private FormattedText formattedText;
	private HttpServletRequest httpServletRequest;
	private HttpServletResponse httpServletResponse;
	// have to do it here because we need it in urlCache. It has to happen before Spring initialization
	private static MemoryService memoryService = (MemoryService)ComponentManager.get(MemoryService.class);
	private ToolManager toolManager;
	public TextInputEvolver richTextEvolver;
	private static LessonBuilderAccessService lessonBuilderAccessService;
	DateFormat df = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, new ResourceLoader().getLocale());;
	
	private List<Long> printedSubpages;
	
	private Map<String,String> imageToMimeMap;
	public void setImageToMimeMap(Map<String,String> map) {
		this.imageToMimeMap = map;
	}
        public boolean useSakaiIcons = ServerConfigurationService.getBoolean("lessonbuilder.use-sakai-icons", false);
        public boolean allowSessionId = ServerConfigurationService.getBoolean("session.parameter.allow", false);
        public boolean allowCcExport = ServerConfigurationService.getBoolean("lessonbuilder.cc-export", true);
        public boolean allowDeleteOrphans = ServerConfigurationService.getBoolean("lessonbuilder.delete-orphans", false);

	public boolean isLessonPrintAllEnabled = ServerConfigurationService.getBoolean("lessonbuilder.printAll", false);

	// I don't much like the static, because it opens us to a possible race
	// condition, but I don't see much option
	// see the setter. It has to be static because it's used in makeLink, which
	// is static so it can be used
	// by ReorderProducer. I wonder if this whole producer could be made
	// application scope?
	private static LessonEntity forumEntity;
	private static LessonEntity quizEntity;
	private static LessonEntity assignmentEntity;
	private static LessonEntity bltiEntity;
	public MessageLocator messageLocator;
	private static LocaleGetter localegetter;
	public static final String VIEW_ID = "ShowPage";
	// mp4 means it plays with the flash player if HTML5 doesn't work.
	// flv is also played with the flash player, but it doesn't get a backup <OBJECT> inside the player
	// Strobe claims to handle MOV files as well, but I feel safer passing them to quicktime, though that requires Quicktime installation
        private static final String DEFAULT_MP4_TYPES = "video/mp4,video/m4v,audio/mpeg,audio/mp3,video/x-m4v";
        private static String[] mp4Types = null;
        private static final String DEFAULT_HTML5_TYPES = "video/mp4,video/m4v,video/webm,video/ogg,audio/mpeg,audio/ogg,audio/wav,audio/x-wav,audio/webm,audio/ogg,audio/mp4,audio/aac,audio/mp3,video/x-m4v";
	// jw can also handle audio: audio/mp4,audio/mpeg,audio/ogg
        private static String[] html5Types = null;
	private static final String DEFAULT_WIDTH = "640px";
	// almost ISO. Full ISO isn't available until Java 7. this uses -0400 where ISO uses -04:00
	SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	//institution's twitter widget id, should come from properties file
	public static final String TWITTER_WIDGET_ID = "lessonbuilder.twitter.widget.id";

	// WARNING: this must occur after memoryService, for obvious reasons. 
	// I'm doing it this way because it doesn't appear that Spring can do this kind of initialization
	// and it's better to let Java's initialization code handle synchronization than do it ourselves in
	// an init method
	private static Cache urlCache = memoryService.newCache("org.sakaiproject.lessonbuildertool.tool.producers.ShowPageProducer.url.cache");
    	public static int majorVersion = getMajorVersion();
        public static String fullVersion = getFullVersion();

	protected static final int DEFAULT_EXPIRATION = 10 * 60;

	public static int getMajorVersion() {

	    String sakaiVersion = ServerConfigurationService.getString("version.sakai", "12");

	    int major = 2;

		String majorString = "";

		// use - as separator to handle -SNAPSHOT, etc.
		String [] parts = sakaiVersion.split("[-.]");
		if (parts.length >= 1) {
		    majorString = parts[0];
		}

		try {
			major = Integer.parseInt(majorString);
		} catch (NumberFormatException nfe) {
			log.error(
				"Failed to parse Sakai version number. This may impact which versions of dependencies are loaded.");
		}

	    return major;
	}

	public static String getFullVersion() {

	    String sakaiVersion = ServerConfigurationService.getString("version.sakai", "12");

	    int i = sakaiVersion.indexOf("-"); // for -snapshot
	    if (i >= 0)
		sakaiVersion = sakaiVersion.substring(0, i);
	    
	    return sakaiVersion;
	}

	static final String ICONSTYLE = "\n.portletTitle .action .help img {\n        background: url({}/help.gif) center right no-repeat !important;\n}\n.portletTitle .action .help img:hover, .portletTitle .action .help img:focus {\n        background: url({}/help_h.gif) center right no-repeat\n}\n.portletTitle .title img {\n        background: url({}/reload.gif) center left no-repeat;\n}\n.portletTitle .title img:hover, .portletTitle .title img:focus {\n        background: url({}/reload_h.gif) center left no-repeat\n}\n";

	public String getViewID() {
		return VIEW_ID;
	}

	// this code is written to handle the fact the CSS uses NNNpx and old code
	// NNN. We need to be able to convert.
	// Length is intended to be a neutral representation. getOld returns without
	// px, getNew with px, and getOrig
	// the original version
	public class Length {
		String number;
		String unit;

		Length(String spec) {
			spec = spec.trim();
			int numlen;
			for (numlen = 0; numlen < spec.length(); numlen++) {
				if (!Character.isDigit(spec.charAt(numlen))) {
					break;
				}
			}
			number = spec.substring(0, numlen).trim();
			unit = spec.substring(numlen).trim().toLowerCase();
		}

		public String getOld() {
			return number + (unit.equals("px") ? "" : unit);
		}

		public String getNew() {
			return number + (unit.equals("") ? "px" : unit);
		}
	}

	// problem is it needs to work with a null argument
	public static String getOrig(Length l) {
		if (lengthOk(l))
			return l.number + l.unit;
		else
			return "";
	}

	// do we have a valid length?
	public static boolean lengthOk(Length l) {
		if (l == null || l.number == null || l.number.equals("")) {
			if (l != null && l.unit.equals("auto"))
				return true;
			return false;
		}
		return true;
	}

	public static boolean definiteLength(Length l) {
	    if (l == null || l.number == null)
		return false;
	    if (l.unit.equals("") || l.unit.equals("px"))
		return true;
	    return false;
	}

	// created style arguments. This was done at the time when i thought
	// the OBJECT tag actually paid attention to the CSS size. it doesn't.
	public String getStyle(Length w, Length h) {
	    String ret = null;
	    if (lengthOk(w))
		ret = "width:" + w.getNew();
	    if (lengthOk(h)) {
		if (ret != null)
		    ret = ret + ";";
		ret = ret + "height:" + h.getNew();
	    }
	    return ret;
	}

	// produce abbreviated versions of URLs, for use in constructing titles
	public String abbrevUrl(String url) {
		if (url.startsWith("/")) {
			int suffix = url.lastIndexOf("/");
			if (suffix > 0) {
				url = url.substring(suffix + 1);
			}
			if (url.startsWith("http:__")) {
				url = url.substring(7);
				suffix = url.indexOf("_");
				if (suffix > 0) {
					url = messageLocator.getMessage("simplepage.fromhost").replace("{}", url.substring(0, suffix));
				}
			} else if (url.startsWith("https:__")) {
				url = url.substring(8);
				suffix = url.indexOf("_");
				if (suffix > 0) {
					url = messageLocator.getMessage("simplepage.fromhost").replace("{}", url.substring(0, suffix));
				}
			}
		} else {
			// external, the hostname is probably best
			try {
				URL u = new URL(url);
				url = messageLocator.getMessage("simplepage.fromhost").replace("{}", u.getHost());
			} catch (Exception ignore) {
				log.error("exception in abbrevurl " + ignore);
			}
			;
		}

		return url;
	}

	public String myUrl() {
	    // previously we computed something, but this will give us the official one
	        return ServerConfigurationService.getServerUrl();
	}

	// NOTE:
	// pages should normally be called with 3 arguments:
	// sendingPageId - the page to show
	// itemId - the item used to choose the page, because pages can occur in
	// different places, and we need
	// to know the context in which this was called. Note that there's an item
	// even for top-level pages
	// path - push, next, or a number. The number is an index into the
	// breadcrumbs if someone clicks
	// on breadcrumbs. This item is used to maintain the path (the internal
	// form of the breadcrumbs)
	// missing is treated as next.
	// for startup, none of this will be known, so getCurrentPage will find the
	// top level page and item if
	// nothing is specified

	public void fillComponents(UIContainer tofill, ViewParameters viewParams, ComponentChecker checker) {
		GeneralViewParameters params = (GeneralViewParameters) viewParams;

                UIOutput.make(tofill, "html").decorate(new UIFreeAttributeDecorator("lang", localegetter.get().getLanguage()))
		    .decorate(new UIFreeAttributeDecorator("xml:lang", localegetter.get().getLanguage()));        

		UIOutput.make(tofill, "datepicker").decorate(new UIFreeAttributeDecorator("src", "/library/js/lang-datepicker/lang-datepicker.js" + PortalUtils.getCDNQuery()));

		UIOutput.make(tofill, "portletBody").decorate(new UIFreeAttributeDecorator("sakaimajor", Integer.toString(majorVersion)))
		    .decorate(new UIFreeAttributeDecorator("sakaiversion", fullVersion));

		boolean iframeJavascriptDone = false;
		
		// security model:
		// canEditPage and canReadPage are normal Sakai privileges. They apply

		// to all
		// pages in the site.
		// However when presented with a page, we need to make sure it's
		// actually in
		// this site, or users could get to pages in other sites. That's done
		// by updatePageObject. The model is that producers always work on the
		// current page, and updatePageObject makes sure that is in the current
		// site.
		// At that point we can safely use canEditPage.

		// somewhat misleading. sendingPage specifies the page we're supposed to
		// go to.  If path is "none", we don't want this page to be what we see
		// when we come back to the tool
		if (params.getSendingPage() != -1) {
			// will fail if page not in this site
			// security then depends upon making sure that we only deal with
			// this page
			try {
				simplePageBean.updatePageObject(params.getSendingPage(), !params.getPath().equals("none"));
			} catch (Exception e) {
				log.warn("ShowPage permission exception " + e);
				UIOutput.make(tofill, "error-div");
				UIOutput.make(tofill, "error", messageLocator.getMessage("simplepage.not_available"));
				return;
			}
		}
		
		boolean canEditPage = simplePageBean.canEditPage();
		boolean canReadPage = simplePageBean.canReadPage();
		boolean canSeeAll = simplePageBean.canSeeAll();  // always on if caneditpage
		
		boolean cameFromGradingPane = params.getPath().equals("none");

		TimeZone localtz = userTimeService.getLocalTimeZone();
		isoDateFormat.setTimeZone(localtz);

		if (!canReadPage) {
			// this code is intended for the situation where site permissions
			// haven't been set up.
			// So if the user can't read the page (which is pretty abnormal),
			// see if they have site.upd.
			// if so, give them some explanation and offer to call the
			// permissions helper
			String ref = "/site/" + simplePageBean.getCurrentSiteId();
			if (simplePageBean.canEditSite()) {
				SimplePage currentPage = simplePageBean.getCurrentPage();
				UIOutput.make(tofill, "needPermissions");

				GeneralViewParameters permParams = new GeneralViewParameters();
				permParams.setSendingPage(-1L);
				createStandardToolBarLink(PermissionsHelperProducer.VIEW_ID, tofill, "callpermissions", "simplepage.permissions", permParams, "simplepage.permissions.tooltip");

			}

			// in any case, tell them they can't read the page
			UIOutput.make(tofill, "error-div");
			UIOutput.make(tofill, "error", messageLocator.getMessage("simplepage.nopermissions"));
			return;
		}

		// Note: Legacy browser detection removed - modern browsers only
		// as far as I can tell, none of these supports fck or ck
		// we can make it configurable if necessary, or use WURFL
		// however this test is consistent with CKeditor's check.
		// that desireable, since if CKeditor is going to use a bare
		// text block, we want to handle it as noEditor
		//   Update, Apr 7, 2016: CKeditor now works except for very old
		// browser versions. from my reading of the code, it works except
		// for IE < 7, Firefox < 5, Safari < 5.1. Sakai itself isn't supported
		// for those versions, so I'm not going to bother to test.
		//String userAgent = httpServletRequest.getHeader("User-Agent");
		//if (userAgent == null)
		//    userAgent = "";
		//boolean noEditor = userAgent.toLowerCase().indexOf("mobile") >= 0;
		boolean noEditor = false;

		// set up locale
		Locale M_locale = null;
		String langLoc[] = localegetter.get().toString().split("_");
		if (langLoc.length >= 2) {
			if ("en".equals(langLoc[0]) && "ZA".equals(langLoc[1])) {
				M_locale = new Locale("en", "GB");
			} else {
				M_locale = new Locale(langLoc[0], langLoc[1]);
			}
		} else {
			M_locale = new Locale(langLoc[0]);
		}

		// clear session attribute if necessary, after calling Samigo
		String clearAttr = params.getClearAttr();
		if (StringUtils.isBlank(clearAttr)) {
			// TODO RSF is not populating viewParams correctly so we get it off the request
			clearAttr = httpServletRequest.getParameter("clearAttr");
		}

		if (StringUtils.isNotBlank(clearAttr)) {
			Session session = SessionManager.getCurrentSession();
			// don't let users clear random attributes
			if (clearAttr.startsWith("LESSONBUILDER_RETURNURL")) {
				session.setAttribute(clearAttr, null);
				params.setClearAttr(null);
			}
		}

		if (mp4Types == null) {
			String m4Types = ServerConfigurationService.getString("lessonbuilder.mp4.types", DEFAULT_MP4_TYPES);
			mp4Types = m4Types.split(",");
			for (int i = 0; i < mp4Types.length; i++) {
				mp4Types[i] = mp4Types[i].trim().toLowerCase();
			}
			Arrays.sort(mp4Types);
		}

		if (html5Types == null) {
			String jTypes = ServerConfigurationService.getString("lessonbuilder.html5.types", DEFAULT_HTML5_TYPES);
			html5Types = jTypes.split(",");
			for (int i = 0; i < html5Types.length; i++) {
				html5Types[i] = html5Types[i].trim().toLowerCase();
			}
			Arrays.sort(html5Types);
		}

		// remember that page tool was reset, so we need to give user the option
		// of going to the last page from the previous session
		SimplePageToolDao.PageData lastPage = simplePageBean.toolWasReset();

		// if this page was copied from another site we may have to update links
		// can only do the fixups if you can write. We could hack permissions, but
		// I assume a site owner will access the site first
		if (canEditPage)
		    simplePageBean.maybeUpdateLinks();

		// if starting the tool, sendingpage isn't set. the following call
		// will give us the top page.
		SimplePage currentPage = simplePageBean.getCurrentPage();
		
		// now we need to find our own item, for access checks, etc.
		SimplePageItem pageItem = null;
		if (currentPage != null) {
			pageItem = simplePageBean.getCurrentPageItem(params.getItemId());
		}
		// one more security check: make sure the item actually involves this
		// page.
		// otherwise someone could pass us an item from a different page in
		// another site
		// actually this normally happens if the page doesn't exist and we don't
		// have permission to create it
		if (currentPage == null || pageItem == null || 
		    (pageItem.getType() != SimplePageItem.STUDENT_CONTENT &&Long.valueOf(pageItem.getSakaiId()) != currentPage.getPageId())) {
			log.warn("ShowPage item not in page");
			UIOutput.make(tofill, "error-div");
			if (currentPage == null)
			    // most likely tool was created by site info but no page
			    // has created. It will created the first time an item is created,
			    // so from a user point of view it looks like no item has been added
			    UIOutput.make(tofill, "error", messageLocator.getMessage("simplepage.noitems_error_user"));
			else
			    UIOutput.make(tofill, "error", messageLocator.getMessage("simplepage.not_available"));
			return;
		}

		// the reason for a seaprate release date test is so we can show the date.
		// there are currently some issues. If the page is not released and the user doesn't have
		// access because of groups, this will show the not released data. That's misleading because
		// when the release date comes the user still won't be able to see it. Not sure if it's worth
		// creating a separate function that just checks the groups. It's easy to test hidden, so I do that. The idea is that
		// if it's both hidden and not released it makes sense to show hidden.

		// check two parts of isitemvisible where we want to give specific errors
		// potentially need time zone for setting release date
		if (!canSeeAll && currentPage.getReleaseDate() != null && currentPage.getReleaseDate().after(new Date()) && !currentPage.isHidden()) {
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, M_locale);
			TimeZone tz = userTimeService.getLocalTimeZone();
			df.setTimeZone(tz);
			String releaseDate = df.format(currentPage.getReleaseDate());
			String releaseMessage = messageLocator.getMessage("simplepage.not_yet_available_releasedate").replace("{}", releaseDate);

			UIOutput.make(tofill, "error-div");
			UIOutput.make(tofill, "error", releaseMessage);

			return;
		}
		
		// the only thing not already tested (or tested in release check below) in isItemVisible is groups. In theory
		// no one should have a URL to a page for which they aren't in the group,
		// so I'm not trying to give a better message than just hidden
		if (!canSeeAll && currentPage.isHidden() || !simplePageBean.isItemVisible(pageItem)) {
		    UIOutput.make(tofill, "error-div");
		    UIOutput.make(tofill, "error", messageLocator.getMessage("simplepage.not_available_hidden"));
		    return;
		}

		// I believe we've now checked all the args for permissions issues. All
		// other item and
		// page references are generated here based on the contents of the page
		// and items.

		// needed to process path arguments first, so refresh page goes the right page
		if (simplePageBean.getTopRefresh()) {
		    UIOutput.make(tofill, "refresh");
		    return;    // but there's no point doing anything more
		}

		// error from previous operation
		// consumes the message, so don't do it if refreshing
		List<String> errMessages = simplePageBean.errMessages();
		if (errMessages != null) {
		    UIOutput.make(tofill, "error-div");
		    for (String e: errMessages) {
			UIBranchContainer er = UIBranchContainer.make(tofill, "errors:");
			UIOutput.make(er, "error-message", e);
		    }
		}


		if (canEditPage) {
		    // special instructor-only javascript setup.
		    // but not if we're refreshing
			UIOutput.make(tofill, "instructoronly");
			// Chome and IE will abort a page if some on it was input from
			// a previous submit. I.e. if an HTML editor was used. In theory they
			// only do this if part of it is Javascript, but in practice they do
			// it for images as well. The protection isn't worthwhile, since it only
			// protects the first time. Since it will reesult in a garbled page, 
			// people will just refresh the page, and then they'll get the new
			// contents. The Chrome guys refuse to fix this so it just applies to Javascript
			httpServletResponse.setHeader("X-XSS-Protection", "0");
		}
		
		
		if (currentPage == null || pageItem == null) {
			UIOutput.make(tofill, "error-div");
			if (canEditPage) {
				UIOutput.make(tofill, "error", messageLocator.getMessage("simplepage.impossible1"));
			} else {
				UIOutput.make(tofill, "error", messageLocator.getMessage("simplepage.not_available"));
			}
			return;
		}
		
		// Set up customizable CSS
		ContentResource cssLink = simplePageBean.getCssForCurrentPage();
		if(cssLink != null) {
			UIOutput.make(tofill, "customCSS").decorate(new UIFreeAttributeDecorator("href", cssLink.getUrl()));
		}

		// offer to go to saved page if this is the start of a session, in case
		// user has logged off and logged on again.
		// need to offer to go to previous page? even if a new session, no need
		// if we're already on that page
		if (lastPage != null && lastPage.pageId != currentPage.getPageId()) {
			UIOutput.make(tofill, "refreshAlert");
			UIOutput.make(tofill, "refresh-message", messageLocator.getMessage("simplepage.last-visited"));
			// Should simply refresh
			GeneralViewParameters p = new GeneralViewParameters(VIEW_ID);
			p.setSendingPage(lastPage.pageId);
			p.setItemId(lastPage.itemId);
			// reset the path to the saved one
			p.setPath("log");
			
			String name = lastPage.name;
			
			// Titles are set oddly by Student Content Pages
			SimplePage lastPageObj = simplePageToolDao.getPage(lastPage.pageId);
			if(simplePageBean.isStudentPage(lastPageObj)) {
				name = lastPageObj.getTitle();
			}
			
			UIInternalLink.make(tofill, "refresh-link", name, p);
		}

		// path is the breadcrumbs. Push, pop or reset depending upon path=
		// programmer documentation.
		String title;
		String ownerName = null;
		if(pageItem.getType() != SimplePageItem.STUDENT_CONTENT) {
			title = pageItem.getName();
		}else {
			title = buildStudentPageTitle(pageItem, currentPage.getTitle(), currentPage.getGroup(), currentPage.getOwner(), simplePageBean.isPageOwner(currentPage), canEditPage);
		}
		
		String newPath = null;
		
		// If the path is "none", then we don't want to record this page as being viewed, or set a path
		if(!params.getPath().equals("none")) {
			newPath = simplePageBean.adjustPath(params.getPath(), currentPage.getPageId(), pageItem.getId(), title);
			simplePageBean.adjustBackPath(params.getBackPath(), currentPage.getPageId(), pageItem.getId(), pageItem.getName());
		}
		
		// put out link to index of pages
		GeneralViewParameters showAll = new GeneralViewParameters(PagePickerProducer.VIEW_ID);
		showAll.setSource("summary");
		UIInternalLink.make(tofill, "print-view", showAll)
		    .decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.print_view")));

		if (isLessonPrintAllEnabled) {
			UIOutput.make(tofill, "show-print-all");
		}

		UIInternalLink.make(tofill, "print-all", showAll)
 		    .decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.print_all")));
		UIInternalLink.make(tofill, "show-pages", showAll)
		    .decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.showallpages")));
		
		if (canEditPage) {
			// show tool bar, but not if coming from grading pane
			if(!cameFromGradingPane) {
				createToolBar(tofill, currentPage);
			}
			
			UIOutput.make(tofill, "title-descrip");
			String label = null;
			if (pageItem.getType() == SimplePageItem.STUDENT_CONTENT)
			    label = messageLocator.getMessage("simplepage.editTitle");
			else
			    label = messageLocator.getMessage("simplepage.title");
			String descrip = null;
			if (pageItem.getType() == SimplePageItem.STUDENT_CONTENT)
			    descrip = messageLocator.getMessage("simplepage.title-student-descrip");
			else if (pageItem.getPageId() == 0)
			    descrip = messageLocator.getMessage("simplepage.title-top-descrip");
			else
			    descrip = messageLocator.getMessage("simplepage.title-descrip");

			UIComponent edittitlelink = UIInternalLink.makeURL(tofill, "edit-title", "#");
			edittitlelink.decorate(new UIFreeAttributeDecorator("title", descrip));
			UIOutput.make(tofill, "edit-title-text", label);
			UIOutput.make(tofill, "title-descrip-text", descrip);

			if (pageItem.getPageId() == 0 && !simplePageBean.isStudentPage(currentPage)) {  // top level page
			    // need dropdown 
				UIOutput.make(tofill, "dropdown");
				UIOutput.make(tofill, "moreDiv");
				UIOutput.make(tofill, "new-page").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.new-page-tooltip")));
				createToolBarLink(PermissionsHelperProducer.VIEW_ID, tofill, "permissions", "simplepage.permissions", currentPage, "simplepage.permissions.tooltip");
				UIOutput.make(tofill, "import-cc").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.import_cc.tooltip")));
				UIOutput.make(tofill, "export-cc").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.export_cc.tooltip")));

				// Check to see if we have tools registered for external import
				List<Map<String, Object>> toolsImportItem = simplePageBean.getToolsImportItem();
				if ( toolsImportItem.size() > 0 ) {
					UIOutput.make(tofill, "show-lti-import");
					UIForm ltiImport =  UIForm.make(tofill, "lti-import-form");
					makeCsrf(ltiImport, "csrf1");
					GeneralViewParameters ltiParams = new GeneralViewParameters();
					ltiParams.setSendingPage(currentPage.getPageId());
					ltiParams.viewID = LtiImportItemProducer.VIEW_ID;
					UILink link = UIInternalLink.make(tofill, "lti-import-link", messageLocator.getMessage("simplepage.import_lti_button"), ltiParams);
					link.decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.importitem.tooltip")));
				}
			}
			
			// Checks to see that user can edit and that this is either a top level page,
			// or a top level student page (not a subpage to a student page)
			if(simplePageBean.getEditPrivs() == 0 && (pageItem.getPageId() == 0)) {
				UIOutput.make(tofill, "remove-li");
				UIOutput.make(tofill, "remove-page").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.remove-page-tooltip")));
				
				if (allowDeleteOrphans) {
				    UIOutput.make(tofill, "delete-orphan-li");
				    UIForm orphan =  UIForm.make(tofill, "delete-orphan-form");
				    makeCsrf(orphan, "csrf1");
				    UICommand.make(orphan, "delete-orphan", "#{simplePageBean.deleteOrphanPages}");
				    UIOutput.make(orphan, "delete-orphan-link").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.delete-orphan-pages-desc")));
				}

			} else if (simplePageBean.getEditPrivs() == 0 && simplePageBean.isStudentPage(currentPage)) {
			    // getEditPrivs < 2 if we want to let the student delete. Currently we don't. There can be comments
			    // from other students and the page can be shared
				SimpleStudentPage studentPage = simplePageToolDao.findStudentPage(currentPage.getTopParent());
				if (studentPage != null && studentPage.getPageId() == currentPage.getPageId()) {
					UIOutput.make(tofill, "remove-student");
					UIOutput.make(tofill, "remove-page-student").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.remove-student-page-explanation")));
				}
			}

			UIOutput.make(tofill, "dialogDiv");
			UIOutput.make(tofill, "siteid", simplePageBean.getCurrentSiteId());
			UIOutput.make(tofill, "locale", M_locale.toString());

		} else if (!canReadPage) {
			return;
		} else if (!canSeeAll) {
			// see if there are any unsatisfied prerequisites
		        // if this isn't a top level page, this will check that the page above is
		        // accessible. That matters because we check visible, available and release
		        // only for this page but not for the containing page
			List<String> needed = simplePageBean.pagesNeeded(pageItem);
			if (needed.size() > 0) {
				// yes. error and abort
				if (pageItem.getPageId() != 0) {
					// not top level. This should only happen from a "next"
					// link.
					// at any rate, the best approach is to send the user back
					// to the calling page
					List<SimplePageBean.PathEntry> path = simplePageBean.getHierarchy();
					SimplePageBean.PathEntry containingPage = null;
					if (path.size() > 1) {
						// page above this. this page is on the top
						containingPage = path.get(path.size() - 2);
					}

					if (containingPage != null) { // not a top level page, point
						// to containing page
						GeneralViewParameters view = new GeneralViewParameters(VIEW_ID);
						view.setSendingPage(containingPage.pageId);
						view.setItemId(containingPage.pageItemId);
						view.setPath(Integer.toString(path.size() - 2));
						UIInternalLink.make(tofill, "redirect-link", containingPage.title, view);
						UIOutput.make(tofill, "redirect");
					} else {
					    UIOutput.make(tofill, "error-div");
					    UIOutput.make(tofill, "error", messageLocator.getMessage("simplepage.not_available"));
					}

					return;
				}

				// top level page where prereqs not satisified. Output list of
				// pages he needs to do first
				UIOutput.make(tofill, "pagetitle", currentPage.getTitle());
				UIOutput.make(tofill, "error-div");
				UIOutput.make(tofill, "error", messageLocator.getMessage("simplepage.has_prerequistes"));
				UIBranchContainer errorList = UIBranchContainer.make(tofill, "error-list:");
				for (String errorItem : needed) {
					UIBranchContainer errorListItem = UIBranchContainer.make(errorList, "error-item:");
					UIOutput.make(errorListItem, "error-item-text", errorItem);
				}
				return;
			}
		}

		ToolSession toolSession = SessionManager.getCurrentToolSession();
		Placement placement = toolManager.getCurrentPlacement();
		String toolId = placement.getToolId();

		String skinName = null;
		String skinRepo = null;
		String iconBase = null;

		UIComponent titlediv = UIOutput.make(tofill, "titlediv");
		titlediv.decorate(new UIFreeAttributeDecorator("style", "display:none"));

		// note page accessed. the code checks to see whether all the required
		// items on it have been finished, and if so marks it complete, else just updates
		// access date save the path because if user goes to it later we want to restore the
		// breadcrumbs
		if(newPath != null) {
			if(pageItem.getType() != SimplePageItem.STUDENT_CONTENT) {
				simplePageBean.track(pageItem.getId(), newPath);
			}else {
				simplePageBean.track(pageItem.getId(), newPath, currentPage.getPageId());
			}
		}

		if(simplePageBean.isStudentPage(currentPage) && simplePageBean.getEditPrivs() == 0) {
			SimpleStudentPage student = simplePageToolDao.findStudentPageByPageId(currentPage.getPageId());
			
			// Make sure this is a top level student page
			if(student != null && pageItem.getGradebookId() != null) {
				if (simplePageBean.getEditPrivs() == 0 && !simplePageBean.getCurrentUserId().equals(currentPage.getOwner())) {
					UIOutput.make(tofill, "gradingSpan");
				}
				UIOutput.make(tofill, "commentsUUID", String.valueOf(student.getId()));
				UIOutput.make(tofill, "commentPoints", String.valueOf((student.getPoints() != null? student.getPoints() : "")));
				UIOutput pointsBox = UIOutput.make(tofill, "studentPointsBox");
				UIOutput.make(tofill, "topmaxpoints", String.valueOf((pageItem.getGradebookPoints() != null? pageItem.getGradebookPoints():"")));
				if (ownerName != null)
				    pointsBox.decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.grade-for-student").replace("{}",ownerName)));
			
				List<SimpleStudentPage> studentPages = simplePageToolDao.findStudentPages(student.getItemId());
				
				Collections.sort(studentPages, new Comparator<SimpleStudentPage>() {
					public int compare(SimpleStudentPage o1, SimpleStudentPage o2) {
						String title1 = o1.getTitle();
						if (title1 == null)
							title1 = "";
						String title2 = o2.getTitle();
						if (title2 == null)
							title2 = "";
						return title1.compareTo(title2);
				    }
				});
				
				for(int in = 0; in < studentPages.size(); in++) {
					if(studentPages.get(in).isDeleted()) {
						studentPages.remove(in);
					}
				}
				
				int i = -1;
			
				for(int in = 0; in < studentPages.size(); in++) {
					if(student.getId() == studentPages.get(in).getId()) {
						i = in;
						break;
					}
				}
			
				if(i > 0) {
					GeneralViewParameters eParams = new GeneralViewParameters(ShowPageProducer.VIEW_ID, studentPages.get(i-1).getPageId());
					eParams.setItemId(studentPages.get(i-1).getItemId());
					eParams.setPath("next");
				
					UIInternalLink.make(tofill, "gradingBack", eParams);
				}
			
				if(i < studentPages.size() - 1) {
					GeneralViewParameters eParams = new GeneralViewParameters(ShowPageProducer.VIEW_ID, studentPages.get(i+1).getPageId());
					eParams.setItemId(studentPages.get(i+1).getItemId());
					eParams.setPath("next");
				
					UIInternalLink.make(tofill, "gradingForward", eParams);
				}
			
				printGradingForm(tofill);
			}
		}

		// breadcrumbs
		if (pageItem.getPageId() != 0) {
			// Not top-level, so we have to show breadcrumbs

			List<SimplePageBean.PathEntry> breadcrumbs = simplePageBean.getHierarchy();

			int index = 0;
			if (breadcrumbs.size() > 1) {
				UIOutput.make(tofill, "crumbdiv");
				for (SimplePageBean.PathEntry e : breadcrumbs) {
					// don't show current page. We already have a title. This
					// was too much
					UIBranchContainer crumb = UIBranchContainer.make(tofill, "crumb:");
					GeneralViewParameters view = new GeneralViewParameters(VIEW_ID);
					view.setSendingPage(e.pageId);
					view.setItemId(e.pageItemId);
					view.setPath(Integer.toString(index));
					UIComponent link = null;
					if (index < breadcrumbs.size() - 1) {
						// Not the last item
						link = UIInternalLink.make(crumb, "crumb-link", e.title, view);
						UIOutput.make(crumb, "crumb-separator");
					} else {
						UIOutput.make(crumb, "crumb-follow", e.title).decorate(new UIStyleDecorator("bold"));
					}
					index++;
				}
			} else {
				UIOutput.make(tofill, "pagetitle", currentPage.getTitle());
			}
		} else {
			UIOutput.make(tofill, "pagetitle", currentPage.getTitle());
		}

		if (canEditPage){
			String ownerDisplayName = getUserDisplayName(currentPage.getOwner());
			if (StringUtils.isNotBlank(ownerDisplayName)) {
				UIOutput.make(tofill, "owner", " (" + ownerDisplayName + ")");
			}
		}

		// see if there's a next item in sequence.
		simplePageBean.addPrevLink(tofill, pageItem);
		simplePageBean.addNextLink(tofill, pageItem);

		long newItemId = -1L;
		String newItemStr = (String)toolSession.getAttribute("lessonbuilder.newitem");
		if (newItemStr != null) {
		    toolSession.removeAttribute("lessonbuilder.newitem");		    
		    try {
			newItemId = Long.parseLong(newItemStr);
		    } catch (Exception e) {}
		}

		List<SimplePageItem> itemList = null;
		
		// items to show
		if(httpServletRequest.getParameter("printall") != null && currentPage.getTopParent() != null) {
			itemList = simplePageBean.getItemsOnPage(currentPage.getTopParent());
		}
		else {
			itemList = simplePageBean.getItemsOnPage(currentPage.getPageId());
		}
		
		// Move all items with sequence <= 0 to the end of the list.
		// Count is necessary to guarantee we don't infinite loop over a
		// list that only has items with sequence <= 0.
		// Becauses sequence number is < 0, these start out at the beginning
		int count = 1;
		while(itemList.size() > count && itemList.get(0).getSequence() <= 0) {
			itemList.add(itemList.remove(0));
			count++;
		}

		// Make sure we only add the comments javascript file once,
		// even if there are multiple comments tools on the page.
		boolean addedCommentsScript = false;
		int commentsCount = 0;

		// Find the most recent comment on the page by current user
		long postedCommentId = -1;
		if (params.postedComment) {
			postedCommentId = findMostRecentComment();
		}

		// Show a link for downloading media when no plugin 
		// for media playback is available in the browser.
		boolean showDownloads = (simplePageBean.getCurrentSite().getProperties().getProperty("lessonbuilder-nodownloadlinks") == null);

		//
		//
		// MAIN list of items
		//
		// produce the main table

		// Is anything visible?
		// Note that we don't need to check whether any item is available, since the first visible
		// item is always available.
		boolean[] anyItemVisible = new boolean[1];
		anyItemVisible[0]=false;

		if (itemList.size() > 0) {
			UIBranchContainer container = UIBranchContainer.make(tofill, "itemContainer:");

			boolean showRefresh = false;
			boolean fisrt = false;
			int textboxcount = 1;

			int cols = 0;
			int colnum = 0;

			UIBranchContainer sectionWrapper = null;
			UIBranchContainer sectionContainer = null;
			UIBranchContainer columnContainer = null;
			UIBranchContainer tableContainer = null;

			boolean first = true;
					
			printedSubpages = new ArrayList<Long>();
			
			printSubpage(itemList, first, sectionWrapper, sectionContainer, columnContainer, tableContainer, 
					container, cols, colnum, canEditPage, currentPage, anyItemVisible, newItemId, showRefresh, canSeeAll, 
					M_locale, showDownloads, iframeJavascriptDone, tofill, placement, params, postedCommentId, 
					addedCommentsScript, cameFromGradingPane, pageItem, noEditor, commentsCount, textboxcount);

			// end of items. This is the end for normal users. Following is
			// special
			// checks and putting out the dialogs for the popups, for
			// instructors.

			boolean showBreak = false;

			// I believe refresh is now done automatically in all cases
			// if (showRefresh) {
			// UIOutput.make(tofill, "refreshAlert");
			//
			// // Should simply refresh
			// GeneralViewParameters p = new GeneralViewParameters(VIEW_ID);
			// p.setSendingPage(currentPage.getPageId());
			// UIInternalLink.make(tofill, "refreshLink", p);
			// showBreak = true;
			// }

			// stuff goes on the page in the order in the HTML file. So the fact
			// that it's here doesn't mean it shows
			// up at the end. This code produces errors and other odd stuff.

			if (canSeeAll) {
				// if the page is hidden, warn the faculty [students get stopped
				// at
				// the top]
				if (currentPage.isHidden()) {
					UIOutput.make(tofill, "hiddenAlert").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.pagehidden")));
					UIVerbatim.make(tofill, "hidden-text", messageLocator.getMessage("simplepage.pagehidden.text"));

					showBreak = true;
					// similarly warn them if it isn't released yet
				} else if (currentPage.getReleaseDate() != null && currentPage.getReleaseDate().after(new Date())) {
					DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, M_locale);
					TimeZone tz = userTimeService.getLocalTimeZone();
					df.setTimeZone(tz);
					String releaseDate = df.format(currentPage.getReleaseDate());
					UIOutput.make(tofill, "hiddenAlert").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.notreleased")));
					UIVerbatim.make(tofill, "hidden-text", messageLocator.getMessage("simplepage.notreleased.text").replace("{}", releaseDate));
					showBreak = true;
				}
			}

			if (showBreak) {
				UIOutput.make(tofill, "breakAfterWarnings");
			}
		}

		// more warnings: if no item on the page, give faculty instructions,
		// students an error
		if (!anyItemVisible[0]) {
			if (canEditPage) {
				String helpUrl = null;
				// order:
				// localized placedholder
				// localized general
				// default placeholder
				// we know the defaults exist because we include them, so
				// we never need to consider default general
				if (simplePageBean.isStudentPage(currentPage)) {
				    helpUrl = getLocalizedURL("student.html", true);
				}
				else {
				    helpUrl = getLocalizedURL("placeholder.html", false);
				    if (helpUrl == null)
					helpUrl = getLocalizedURL("general.html", false);
				    if (helpUrl == null)
					helpUrl = getLocalizedURL("placeholder.html", true);
				}

				UIOutput.make(tofill, "startupHelp")
				    .decorate(new UIFreeAttributeDecorator("src", helpUrl))
				    .decorate(new UIFreeAttributeDecorator("id", "iframe"))
					.decorate(new UIFreeAttributeDecorator("allow", String.join(";",
							Optional.ofNullable(ServerConfigurationService.getStrings("browser.feature.allow"))
									.orElseGet(() -> new String[]{}))));
				if (!iframeJavascriptDone) {
				    UIOutput.make(tofill, "iframeJavascript");
				    iframeJavascriptDone = true;
				}
			} else {
				UIOutput.make(tofill, "error-div");
				UIOutput.make(tofill, "error", messageLocator.getMessage("simplepage.noitems_error_user"));
			}
		}

		// now output the dialogs. but only for faculty (to avoid making the
		// file bigger)
		if (canEditPage) {
			createSubpageDialog(tofill, currentPage);
		}

		createDialogs(tofill, currentPage, pageItem, cssLink);

		// Add pageids to the page so the portal lessons subnav menu can update its state
		List<SimplePageBean.PathEntry> path = simplePageBean.getHierarchy();
		if (path.size() > 2) {
			SimplePageBean.PathEntry topLevelSubPage = path.get(1);
			UIOutput.make(tofill, "lessonsSubnavTopLevelPageId")
				.decorate(new UIFreeAttributeDecorator("value", String.valueOf(topLevelSubPage.pageId)));
		} else {
			UIOutput.make(tofill, "lessonsSubnavPageId")
				.decorate(new UIFreeAttributeDecorator("value", String.valueOf(simplePageBean.getCurrentPage().getPageId())));
		}
		UIOutput.make(tofill, "lessonsSubnavToolId")
			.decorate(new UIFreeAttributeDecorator("value", String.valueOf(placement.getId())));
		UIOutput.make(tofill, "lessonsSubnavItemId")
			.decorate(new UIFreeAttributeDecorator("value", String.valueOf(pageItem.getId())));

		String currentPageId = String.valueOf(simplePageBean.getCurrentPage().getPageId());
		UIOutput.make(tofill, "lessonsCurrentPageId")
		    .decorate(new UIFreeAttributeDecorator("value", currentPageId));
		UIOutput.make(tofill, "ckeditor-autosave-entity-id")
		    .decorate(new UIFreeAttributeDecorator("name", "ckeditor-autosave-entity-id"))
		    .decorate(new UIFreeAttributeDecorator("value", currentPageId));
	}

	public void printSubpage(List<SimplePageItem> itemList, boolean first, UIBranchContainer sectionWrapper, UIBranchContainer sectionContainer, UIBranchContainer columnContainer, UIBranchContainer tableContainer, 
			UIBranchContainer container, int cols, int colnum, boolean canEditPage, SimplePage currentPage, boolean[] anyItemVisible, long newItemId, boolean showRefresh, boolean canSeeAll, 
			Locale M_locale, boolean showDownloads, boolean iframeJavascriptDone, UIContainer tofill, Placement placement, GeneralViewParameters params, long postedCommentId, 
			boolean addedCommentsScript, boolean cameFromGradingPane, SimplePageItem pageItem, boolean noEditor, int commentsCount, int textboxcount) {
			
			boolean subPageTitleIncluded = false;
			boolean subPageTitleContinue = false;

			boolean includeTwitterLibrary = false;

			boolean forceButtonColor = false;
			String color = null;
			for (SimplePageItem i : itemList) {

				// break is not a normal item. handle it first
			        // this will work whether first item is break or not. Might be a section
			        // break or a normal item
				if (first || i.getType() == SimplePageItem.BREAK) {
				    boolean sectionbreak = false;
				    forceButtonColor = BooleanUtils.toBoolean(i.getAttribute("forceBtn"));
				    color = i.getAttribute("colcolor");
				    if (first || "section".equals(i.getFormat())) {
					sectionWrapper = UIBranchContainer.make(container, "sectionWrapper:");
					boolean collapsible = i.getAttribute("collapsible") != null && (!"0".equals(i.getAttribute("collapsible")));
					boolean defaultClosed = i.getAttribute("defaultClosed") != null && (!"0".equals(i.getAttribute("defaultClosed")));
					UIOutput sectionHeader = UIOutput.make(sectionWrapper, "sectionHeader");

					// only do this is there's an actual section break. Implicit ones don't have an item to hold the title
					String headerText = "";
					if ("section".equals(i.getFormat()) && i.getName() != null) {
					    headerText = i.getName();
					}
					UIOutput.make(sectionWrapper, "sectionHeaderText", headerText);
					UIOutput collapsedIcon = UIOutput.make(sectionWrapper, "sectionCollapsedIcon");
					sectionHeader.decorate(new UIStyleDecorator(headerText.equals("")? "skip" : ""));
					sectionContainer = UIBranchContainer.make(sectionWrapper, "section:");
						if(forceButtonColor){
							sectionContainer.decorate(new UIStyleDecorator("hasColor"));
						}
					boolean needIcon = false;
					if (collapsible) {
						sectionHeader.decorate(new UIStyleDecorator("collapsibleSectionHeader"));
						sectionHeader.decorate(new UIFreeAttributeDecorator("aria-controls", sectionContainer.getFullID()));
						sectionHeader.decorate(new UIFreeAttributeDecorator("aria-expanded", (defaultClosed?"false":"true")));
						sectionContainer.decorate(new UIStyleDecorator("collapsible"));
						if (defaultClosed ) {
							sectionHeader.decorate(new UIStyleDecorator("closedSectionHeader"));
							sectionContainer.decorate(new UIStyleDecorator("defaultClosed"));
							needIcon = true;
						} else {
							sectionHeader.decorate(new UIStyleDecorator("openSectionHeader"));
						}
					}
					if (!needIcon)
					    collapsedIcon.decorate(new UIFreeAttributeDecorator("style", "display:none"));

					sectionHeader.decorate(new UIStyleDecorator((color == null?"":"col"+color+"-header")));
					cols = colCount(itemList, i.getId());
					sectionbreak = true;
					colnum = 0;
				    } else if ("column".equals(i.getFormat()))
					colnum++;
				    String colForceBtnColor = i.getAttribute("forceBtn");
				    columnContainer = UIBranchContainer.make(sectionContainer, "column:");
				    if(StringUtils.isEmpty(colForceBtnColor) || StringUtils.equalsIgnoreCase(colForceBtnColor, "false")){
				    	columnContainer.decorate(new UIStyleDecorator("noColor"));
					}
				    tableContainer = UIBranchContainer.make(columnContainer, "itemTable:");
				    Integer width = new Integer(i.getAttribute("colwidth") == null ? "1" : i.getAttribute("colwidth"));
				    Integer split = new Integer(i.getAttribute("colsplit") == null ? "1" : i.getAttribute("colsplit"));
				    colnum += width; // number after this column

				    columnContainer.decorate(new UIStyleDecorator("cols" + cols + (width > 1?" double":"") + (split > 1?" split":"") + (color == null?"":" col"+color)));
				    UIOutput.make(columnContainer, "break-msg", messageLocator.getMessage(sectionbreak?"simplepage.break-here":"simplepage.break-column-here"));

				    if (canEditPage) {
				    UIComponent delIcon = UIOutput.make(columnContainer, "section-td");
				    if (first)
					delIcon.decorate(new UIFreeAttributeDecorator("style", "display:none"));

				    UIOutput.make(columnContainer, "section2");
				    UIOutput.make(columnContainer, "section3").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.columnopen")));
				    UIOutput.make(columnContainer, "addbottom");
				    UIOutput.make(columnContainer, "addbottom2").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.add-item-column")));
				    UIOutput mergeLink = UIOutput.make(columnContainer, "section-del-link");
				    mergeLink.decorate(new UIFreeAttributeDecorator("data-merge-id", String.valueOf(i.getId())));
				    mergeLink.decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.join-items")));
				    mergeLink.decorate(new UIFreeAttributeDecorator("aria-label", messageLocator.getMessage("simplepage.join-items")));
				    mergeLink.decorate(new UIStyleDecorator(sectionbreak?"section-merge-link":"column-merge-link"));
				    }

				    UIBranchContainer tableRow = UIBranchContainer.make(tableContainer, "item:");
				    tableRow.decorate(new UIFreeAttributeDecorator("class", "breakitem break" + i.getFormat()));
				    if (canEditPage) {
					// usual case is this is a break
					if (i.getType() == SimplePageItem.BREAK)
					    UIOutput.make(tableRow, "itemid", String.valueOf(i.getId()));
					else {
					    // page doesn't start with a break. have to use pageid
					    UIOutput.make(tableRow, "itemid", "p" + currentPage.getPageId());
					}
				    }

				    first = false;
				    if (i.getType() == SimplePageItem.BREAK)
				    continue;
				    // for first item, if wasn't break, process it
				}
				
				if (!simplePageBean.isItemVisible(i, currentPage)) {
					continue;
				}

				if(httpServletRequest.getParameter("printall") != null && i.getSakaiId() != null && !"".equals(i.getSakaiId()) && StringUtils.isNumeric(i.getSakaiId())
						&& !printedSubpages.contains(Long.valueOf(i.getSakaiId())))			
				{
					// is a subpage		
															
					printedSubpages.add(Long.valueOf(i.getSakaiId()));
					
					List<SimplePageItem> subitemList = (List<SimplePageItem>) simplePageBean.getItemsOnPage(Long.valueOf(i.getSakaiId()));
					printSubpage(subitemList, first, sectionWrapper, sectionContainer, columnContainer, tableContainer, 
							container, cols, colnum, canEditPage, currentPage, anyItemVisible, newItemId, showRefresh, canSeeAll, 
							M_locale, showDownloads, iframeJavascriptDone, tofill, placement, params, postedCommentId,
							addedCommentsScript, cameFromGradingPane, pageItem, noEditor, commentsCount, textboxcount);
					
					subPageTitleContinue = true;					
				}
				else
				{

				// listitem is mostly historical. it uses some shared HTML, but
				// if I were
				// doing it from scratch I wouldn't make this distinction. At
				// the moment it's
				// everything that isn't inline.

				boolean listItem = !(i.getType() == SimplePageItem.TEXT || i.getType() == SimplePageItem.MULTIMEDIA
						|| i.getType() == SimplePageItem.COMMENTS || i.getType() == SimplePageItem.STUDENT_CONTENT
						|| i.getType() == SimplePageItem.QUESTION || i.getType() == SimplePageItem.PEEREVAL || i.getType() == SimplePageItem.RESOURCE_FOLDER
					        || i.getType() == SimplePageItem.CHECKLIST || i.getType() == SimplePageItem.FORUM_SUMMARY
					        || i.getType() == SimplePageItem.BREAK || i.getType() == SimplePageItem.ANNOUNCEMENTS
					        || i.getType() == SimplePageItem.CALENDAR || i.getType() == SimplePageItem.TWITTER );
				// (i.getType() == SimplePageItem.PAGE &&
				// "button".equals(i.getFormat())))

				// break isn't a real item. probably don't want to count it
				if (i.getType() != SimplePageItem.BREAK)
				    anyItemVisible[0] = true;

				UIBranchContainer tableRow = UIBranchContainer.make(tableContainer, "item:");

				// set class name showing what the type is, so people can do funky CSS

				String itemClassName = "item ";

				switch (i.getType()) {
				case SimplePageItem.RESOURCE: itemClassName += "resourceType"; break;
				case SimplePageItem.PAGE: itemClassName += "pageType"; break;
				case SimplePageItem.ASSIGNMENT: itemClassName += "assignmentType"; break;
				case SimplePageItem.ASSESSMENT: itemClassName += "assessmentType"; break;
				case SimplePageItem.TEXT: itemClassName += "textType"; break;
				case SimplePageItem.URL: itemClassName += "urlType"; break;
				case SimplePageItem.MULTIMEDIA: itemClassName += "multimediaType"; break;
				case SimplePageItem.FORUM: itemClassName += "forumType"; break;
				case SimplePageItem.COMMENTS: itemClassName += "commentsType"; break;
				case SimplePageItem.STUDENT_CONTENT: itemClassName += "studentContentType"; break;
				case SimplePageItem.QUESTION: itemClassName += "question"; break;
				case SimplePageItem.BLTI: itemClassName += "bltiType"; break;
				case SimplePageItem.RESOURCE_FOLDER: itemClassName += "resourceFolderType"; break;
				case SimplePageItem.PEEREVAL: itemClassName += "peereval"; break;
				case SimplePageItem.TWITTER: itemClassName += "twitter"; break;
				case SimplePageItem.FORUM_SUMMARY: itemClassName += "forumSummary"; break;
				case SimplePageItem.ANNOUNCEMENTS: itemClassName += "announcementsType"; break;
				case SimplePageItem.CALENDAR: itemClassName += "calendar"; break;
				case SimplePageItem.CHECKLIST: itemClassName += "checklistType"; break;
				}

				Map<String,Object> ltiContent = null;
				Map<String,Object> ltiTool = null;

				String ltiToolNewPage = null;  // Not an LTI tool
				String ltiContentNewPage = null;

				// If we are an LTI tool...
				if ( i.getType() == SimplePageItem.BLTI && i.getSakaiId() != null ) {
					BltiInterface ltiItem = (bltiEntity == null ? null : (BltiInterface) bltiEntity.getEntity(i.getSakaiId()));
					ltiContent = ltiItem.getContent();
					ltiTool = ltiItem.getTool();
					if ( ltiContent != null ) {
						ltiContentNewPage = ltiContent.get("newpage") != null ? ltiContent.get("newpage").toString() : null;
					}
					if ( ltiTool != null ) {
						ltiToolNewPage = ltiTool.get("newpage") != null ? ltiTool.get("newpage").toString() : null;
					}
				}

				// The item's internal format value
				// "window" = popup, "page" = iframe on separate lessons page, "inline" = iframe on *this* page
				boolean isInline = (i.getType() == SimplePageItem.BLTI && "inline".equals(i.getFormat()));

				// toolNewPage "0" = never launch in popup, "1" = always launch in popup, "2" = delegate to content
				if ( "0".equals(ltiToolNewPage) ) {
					isInline = false;
					if ( "window".equals(i.getFormat()) ) i.setFormat("page");
				}
				if ( "1".equals(ltiToolNewPage) ) {
					isInline = false;
					if ( ! "window".equals(i.getFormat()) ) i.setFormat("window");
				}

				if ( "2".equals(ltiToolNewPage) && "1".equals(ltiContentNewPage) ) {
					i.setFormat("window");
					i.setSameWindow(false);
				}

				if (listItem && !isInline){
				    itemClassName = itemClassName + " listType";
				}
				if (canEditPage) {
				    itemClassName = itemClassName + "  canEdit";
				}

				if (i.getId() == newItemId)
				    itemClassName = itemClassName + " newItem";

				tableRow.decorate(new UIFreeAttributeDecorator("class", itemClassName));

				if (canEditPage)
				    UIOutput.make(tableRow, "itemid", String.valueOf(i.getId()));

				// you really need the HTML file open at the same time to make
				// sense of the following code
				if (listItem) { // Not an HTML Text, Element or Multimedia
					// Element

					if (canEditPage) {
						UIOutput.make(tableRow, "current-item-id2", String.valueOf(i.getId()));
					}

					// Put the LTI data into the markup
					if ( StringUtils.isNotEmpty(ltiToolNewPage) ) UIOutput.make(tableRow, "lti-tool-newpage2", ltiToolNewPage);
					if ( StringUtils.isNotEmpty(ltiContentNewPage) ) UIOutput.make(tableRow, "lti-content-newpage2", ltiContentNewPage);

					// users can declare a page item to be navigational. If so
					// we display
					// it to the left of the normal list items, and use a
					// button. This is
					// used for pages that are "next" pages, i.e. they replace
					// this page
					// rather than creating a new level in the breadcrumbs.
					// Since they can't
					// be required, they don't need the status image, which is
					// good because
					// they're displayed with colspan=2, so there's no space for
					// the image.

					boolean navButton = "button".equals(i.getFormat()) && !i.isRequired();
					boolean notDone = false;
					Status status = Status.NOT_REQUIRED;
					if (!navButton) {
						status = handleStatusIcon(tableRow, i);
						if (status == Status.REQUIRED) {
							notDone = true;
						}
					}

					UIOutput linktd = UIOutput.make(tableRow, "item-td");
					
					UIOutput contentCol = UIOutput.make(tableRow, "contentCol");
					// BLTI seems to require explicit specificaiton for column width. Otherwise
					// we get 300 px wide. Don't know why. Doesn't happen to other iframes
					if (isInline)
					    contentCol.decorate(new UIFreeAttributeDecorator("style", "width:100%"));

					UIBranchContainer linkdiv = null;
					if (!isInline) {
					    linkdiv = UIBranchContainer.make(tableRow, "link-div:");
					}
					if (!isInline && !navButton && !"button".equals(i.getFormat())) {
					    UIOutput itemicon = UIOutput.make(linkdiv,"item-icon");
					    switch (i.getType()) {
					    case SimplePageItem.FORUM:
						itemicon.decorate(new UIStyleDecorator("si si-sakai-forums"));
						break;
					    case SimplePageItem.ASSIGNMENT:
						itemicon.decorate(new UIStyleDecorator("si si-sakai-assignment-grades"));
						break;
					    case SimplePageItem.ASSESSMENT:
						itemicon.decorate(new UIStyleDecorator("si si-sakai-samigo"));
						break;
						case SimplePageItem.BLTI:
							String bltiIcon = "fa-globe";
							if (bltiEntity != null && ((BltiInterface)bltiEntity).servicePresent()) {
								LessonEntity lessonEntity = (bltiEntity == null ? null : bltiEntity.getEntity(i.getSakaiId()));
								String tmp = ((BltiInterface)lessonEntity).getIcon();
								bltiIcon = (tmp == null) ? bltiIcon : tmp;
							}
							itemicon.decorate(new UIStyleDecorator(bltiIcon));
							break;
					    case SimplePageItem.PAGE:
						itemicon.decorate(new UIStyleDecorator("fa-folder-open-o"));
						break;
					    case SimplePageItem.RESOURCE:
						String mimeType = simplePageBean.getContentType(i);

						String src = null;
						//if (!useSakaiIcons)
						    src = imageToMimeMap.get(mimeType);
						if (src == null) {
						    src = "fa-file-o";
						    //String image = ContentTypeImageService.getContentTypeImage(mimeType);
						    // if (image != null)
						    //	src = "/library/image/" + image;
						}
						
						if(src != null) {
						    itemicon.decorate(new UIStyleDecorator(src));
						}
						break;
					    }
					}

					UIOutput descriptiondiv = null;

					// refresh isn't actually used anymore. We've changed the
					// way things are
					// done so the user never has to request a refresh.
					//   FYI: this actually puts in an IFRAME for inline BLTI items
					showRefresh = !makeLink(tableRow, "link", i, canSeeAll, currentPage, notDone, status, forceButtonColor, color) || showRefresh;
					UILink.make(tableRow, "copylink", i.getName(), "http://lessonbuilder.sakaiproject.org/" + i.getId() + "/").
					    decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.copylink2").replace("{}", i.getName())));

					// dummy is used when an assignment, quiz, or forum item is
					// copied
					// from another site. The way the copy code works, our
					// import code
					// doesn't have access to the necessary info to use the item
					// from the
					// new site. So we add a dummy, which generates an
					// explanation that the
					// author is going to have to choose the item from the
					// current site
					if (i.getSakaiId().equals(SimplePageItem.DUMMY)) {
						String code = null;
						switch (i.getType()) {
						case SimplePageItem.ASSIGNMENT:
							code = "simplepage.copied.assignment";
							break;
						case SimplePageItem.ASSESSMENT:
							code = "simplepage.copied.assessment";
							break;
						case SimplePageItem.FORUM:
							code = "simplepage.copied.forum";
							break;
						}
						descriptiondiv = UIOutput.make(tableRow, "description", messageLocator.getMessage(code));
					} else {
						descriptiondiv = UIOutput.make(tableRow, "description", i.getDescription());
					}
					if (isInline)
					    descriptiondiv.decorate(new UIFreeAttributeDecorator("style", "margin-top: 4px"));

					if (!isInline) {
					    // nav button gets float left so any description goes to its
					    // right. Otherwise the
					    // description block will display underneath
					    if ("button".equals(i.getFormat())) {
						linkdiv.decorate(new UIFreeAttributeDecorator("style", "float:none"));
					    }
					    // for accessibility
					    if (navButton) {
						linkdiv.decorate(new UIFreeAttributeDecorator("role", "navigation"));
					    }
					}

					styleItem(tableRow, linktd, contentCol, i, "indentLevel", "custom-css-class");

					// note that a lot of the info here is used by the
					// javascript that prepares
					// the jQuery dialogs
					String itemGroupString = null;
					String editNote = null;
					boolean entityDeleted = false;
					boolean notPublished = false;
					if (canEditPage) {
						UIOutput.make(tableRow, "edit-td").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.generic").replace("{}", i.getName())));
						UILink.make(tableRow, "edit-link", (String)null, "").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.generic").replace("{}", i.getName())));

						// the following information is displayed using <INPUT
						// type=hidden ...
						// it contains information needed to populate the "edit"
						// popup dialog
						UIOutput.make(tableRow, "prerequisite-info", String.valueOf(i.isPrerequisite()));
						UIOutput.make(tableRow, "required-info", String.valueOf(i.isRequired()));

						if (i.getType() == SimplePageItem.ASSIGNMENT) {
							// the type indicates whether scoring is letter
							// grade, number, etc.
							// the javascript needs this to present the right
							// choices to the user
							// types 6 and 8 aren't legal scoring types, so they
							// are used as
							// markers for quiz or forum. I ran out of numbers
							// and started using
							// text for things that aren't scoring types. That's
							// better anyway
							int type = 4;
							LessonEntity assignment = null;
							if (!i.getSakaiId().equals(SimplePageItem.DUMMY)) {
								assignment = assignmentEntity.getEntity(i.getSakaiId(), simplePageBean);
								if (assignment != null) {
									type = assignment.getTypeOfGrade();
									String editUrl = assignment.editItemUrl(simplePageBean);
									if (editUrl != null) {
										UIOutput.make(tableRow, "edit-url", editUrl);
									}
									itemGroupString = simplePageBean.getItemGroupString(i, assignment, true);
									UIOutput.make(tableRow, "item-groups", itemGroupString);
									if (!assignment.objectExists())
									    entityDeleted = true;
									else if (assignment.notPublished())
									    notPublished = true;
								}
							}

							UIOutput.make(tableRow, "type", String.valueOf(type));
							String requirement = String.valueOf(i.getSubrequirement());
							if ((type == SimplePageItem.PAGE || type == SimplePageItem.ASSIGNMENT) && i.getSubrequirement()) {
								requirement = i.getRequirementText();
							}
							UIOutput.make(tableRow, "requirement-text", requirement);
						} else if (i.getType() == SimplePageItem.ASSESSMENT) {
							UIOutput.make(tableRow, "type", "6"); // Not used by
							// assignments,
							// so it is
							// safe to dedicate to assessments
							UIOutput.make(tableRow, "requirement-text", (i.getSubrequirement() ? i.getRequirementText() : "false"));
							LessonEntity quiz = quizEntity.getEntity(i.getSakaiId(),simplePageBean);
							if (quiz != null) {
								String editUrl = quiz.editItemUrl(simplePageBean);
								if (editUrl != null) {
									UIOutput.make(tableRow, "edit-url", editUrl);
								}
								editUrl = quiz.editItemSettingsUrl(simplePageBean);
								if (editUrl != null) {
									UIOutput.make(tableRow, "edit-settings-url", editUrl);
								}
								itemGroupString = simplePageBean.getItemGroupString(i, quiz, true);
								UIOutput.make(tableRow, "item-groups", itemGroupString);
								if (!quiz.objectExists())
								    entityDeleted = true;

							} else
							    notPublished = quizEntity.notPublished(i.getSakaiId());
						} else if (i.getType() == SimplePageItem.BLTI) {
						    UIOutput.make(tableRow, "type", "b");
						    LessonEntity blti = (bltiEntity == null ? null : bltiEntity.getEntity(i.getSakaiId()));
						    if (blti != null) {
							    String editUrl = blti.editItemUrl(simplePageBean);
							    editNote = blti.getEditNote();
							    if (editUrl != null) UIOutput.make(tableRow, "edit-url", editUrl);
							    UIOutput.make(tableRow, "item-format", i.getFormat());

								if (i.getHeight() != null) UIOutput.make(tableRow, "item-height", i.getHeight());
								itemGroupString = simplePageBean.getItemGroupString(i, null, true);
								UIOutput.make(tableRow, "item-groups", itemGroupString );
								if (!blti.objectExists())
									entityDeleted = true;
								else if (blti.notPublished())
									notPublished = true;
						    }
						} else if (i.getType() == SimplePageItem.FORUM) {
							UIOutput.make(tableRow, "extra-info");
							UIOutput.make(tableRow, "type", "8");
							LessonEntity forum = forumEntity.getEntity(i.getSakaiId());
							if (forum != null) {
								String editUrl = forum.editItemUrl(simplePageBean);
								if (editUrl != null) {
									UIOutput.make(tableRow, "edit-url", editUrl);
								}
								itemGroupString = simplePageBean.getItemGroupString(i, forum, true);
								UIOutput.make(tableRow, "item-groups", itemGroupString);
								if (!forum.objectExists())
								    entityDeleted = true;
								else if (forum.notPublished())
								    notPublished = true;

							}
						} else if (i.getType() == SimplePageItem.RESOURCE) {
							UIOutput.make(tableRow, "type", Integer.valueOf(i.getType()).toString());
						        try {
							    itemGroupString = simplePageBean.getItemGroupStringOrErr(i, null, true);
							} catch (IdUnusedException e) {
							    itemGroupString = "";
							    entityDeleted = true;
							}
							if (simplePageBean.getInherited())
							    UIOutput.make(tableRow, "item-groups", "--inherited--");
							else
							    UIOutput.make(tableRow, "item-groups", itemGroupString );
							UIOutput.make(tableRow, "item-samewindow", Boolean.toString(i.isSameWindow()));

							UIVerbatim.make(tableRow, "item-path", getItemPath(i));
						}

					} // end of canEditPage


					if (i.getType() == SimplePageItem.PAGE) {
						UIOutput.make(tableRow, "type", "page");
						UIOutput.make(tableRow, "page-next", Boolean.toString(i.getNextPage()));
						UIOutput.make(tableRow, "page-button", Boolean.toString("button".equals(i.getFormat())));
						SimplePage page = simplePageToolDao.getPage(Long.valueOf(i.getSakaiId()));
						UIOutput.make(tableRow, "page-hidden", Boolean.toString(page.isHidden()));
						itemGroupString = simplePageBean.getItemGroupString(i, null, true);
						UIOutput.make(tableRow, "item-groups", itemGroupString);
						SimplePage sPage = simplePageBean.getPage(Long.parseLong(i.getSakaiId()));
						if (sPage != null) {
							Date rDate = sPage.getReleaseDate();
							String rDateString = "";
							if (rDate != null) {
								rDateString = rDate.toString();
							}
							UIOutput.make(tableRow, "subpagereleasedate", rDateString);
						}
					}
					if (canSeeAll) {
						// haven't set up itemgroupstring yet
						if (!canEditPage) {
						    if (!i.getSakaiId().equals(SimplePageItem.DUMMY)) {
							LessonEntity lessonEntity = null;
							switch (i.getType()) {
							case SimplePageItem.ASSIGNMENT:
							    lessonEntity = assignmentEntity.getEntity(i.getSakaiId(), simplePageBean);
							    if (lessonEntity != null)
								itemGroupString = simplePageBean.getItemGroupString(i, lessonEntity, true);
							    if (!lessonEntity.objectExists())
								entityDeleted = true;
							    else if (lessonEntity.notPublished())
								notPublished = true;
							    break;
							case SimplePageItem.ASSESSMENT:
							    lessonEntity = quizEntity.getEntity(i.getSakaiId(),simplePageBean);
							    if (lessonEntity != null)
								itemGroupString = simplePageBean.getItemGroupString(i, lessonEntity, true);
							    else 
								notPublished = quizEntity.notPublished(i.getSakaiId());
							    if (!lessonEntity.objectExists())
								entityDeleted = true;
							    break;
							case SimplePageItem.FORUM:
							    lessonEntity = forumEntity.getEntity(i.getSakaiId());
							    if (lessonEntity != null)
								itemGroupString = simplePageBean.getItemGroupString(i, lessonEntity, true);
							    if (!lessonEntity.objectExists())
								entityDeleted = true;
							    else if (lessonEntity.notPublished())
								notPublished = true;
							    break;
							case SimplePageItem.BLTI:
							    if (bltiEntity != null) {
								    lessonEntity = bltiEntity.getEntity(i.getSakaiId());
							    }
							    if (lessonEntity != null)
								itemGroupString = simplePageBean.getItemGroupString(i, null, true);
							    if (!lessonEntity.objectExists())
								entityDeleted = true;
							    else if (lessonEntity.notPublished())
								notPublished = true;
							    break;
							case SimplePageItem.PAGE:
							    itemGroupString = simplePageBean.getItemGroupString(i, null, true);
							    break;
							case SimplePageItem.RESOURCE:
							    try {
								itemGroupString = simplePageBean.getItemGroupStringOrErr(i, null, true);
							    } catch (IdUnusedException e) {
								itemGroupString = "";
								entityDeleted = true;
							    }
							    break;
							}
						    }
						}

						String releaseString = simplePageBean.getReleaseString(i, M_locale);
						if (itemGroupString != null || releaseString != null || entityDeleted || notPublished) {
							if (itemGroupString != null)
							    itemGroupString = simplePageBean.getItemGroupTitles(itemGroupString, i);
							if (itemGroupString != null) {
							    itemGroupString = " [" + itemGroupString + "]";
							    if (releaseString != null)
								itemGroupString = " " + releaseString + itemGroupString;
							} else if (releaseString != null)
							    itemGroupString = " " + releaseString;
							if (notPublished) {
							    if (itemGroupString != null)
								itemGroupString = itemGroupString + " " + 
								    messageLocator.getMessage("simplepage.not-published");
							    else
								itemGroupString = messageLocator.getMessage("simplepage.not-published");
							}
							if ( StringUtils.isNotEmpty(editNote) ) {
							    if (StringUtils.isEmpty(itemGroupString) ) itemGroupString= "";
								itemGroupString = itemGroupString + " " + editNote;
							}
							if (entityDeleted) {
							    if (itemGroupString != null)
								itemGroupString = itemGroupString + " " + 
								    messageLocator.getMessage("simplepage.deleted-entity");
							    else
								itemGroupString = messageLocator.getMessage("simplepage.deleted-entity");
							}

							if (itemGroupString != null) {
								String cssClasses = "item-group-titles";
								if (i.getType() == SimplePageItem.PAGE) {
									SimplePage sPage = simplePageBean.getPage(Long.parseLong(i.getSakaiId()));
									if (sPage != null) {
										Date rDate = sPage.getReleaseDate();

										//hidden, deleted, not published, or release date is in future. Not considered released.
										if (sPage.isHidden() || entityDeleted || notPublished || (rDate != null && Instant.now().isBefore(rDate.toInstant()))) {
											cssClasses += " not-released";
										} //not hidden, deleted, is published, release date has passed. considered released
										else if(rDate != null && Instant.now().isAfter(rDate.toInstant())){
											cssClasses+= " released";
										} //not hidden, deleted, is published. No release date restriction. Considered released.
									}
								}
									UIOutput.make(tableRow, (isInline ? "item-group-titles-div" : "item-group-titles"), itemGroupString).decorate(new UIFreeAttributeDecorator("class", cssClasses));

							}
						}
					} // end of canSeeAll
					else {
						String releaseString = simplePageBean.getReleaseString(i, M_locale);
						if (itemGroupString != null || releaseString != null) {
							if (itemGroupString != null)
								itemGroupString = simplePageBean.getItemGroupTitles(itemGroupString, i);
							if (itemGroupString != null) {
								itemGroupString = " [" + itemGroupString + "]";
								if (releaseString != null)
									itemGroupString = " " + releaseString + itemGroupString;
							} else if (releaseString != null)
								itemGroupString = " " + releaseString;
						}
						if (itemGroupString != null) {
							String cssClasses = "item-group-titles";
							if (i.getType() == SimplePageItem.PAGE) {
								SimplePage sPage = simplePageBean.getPage(Long.parseLong(i.getSakaiId()));
								if (sPage != null) {
									Date rDate = sPage.getReleaseDate();

									//hidden, deleted, not published, or release date is in future. Not considered released.
									if (sPage.isHidden() || entityDeleted || notPublished || (rDate != null && Instant.now().isBefore(rDate.toInstant()))) {
										cssClasses += " not-released";
									} //not hidden, deleted, is published, release date has passed. considered released
									else if(rDate != null && Instant.now().isAfter(rDate.toInstant())){
										cssClasses+= " released";
									} //not hidden, deleted, is published. No release date restriction. Considered released.
								}
							}
							UIOutput.make(tableRow, (isInline ? "item-group-titles-div" : "item-group-titles"), itemGroupString).decorate(new UIFreeAttributeDecorator("class", cssClasses));

						}
					}
					// the following are for the inline item types. Multimedia
					// is the most complex because
					// it can be IMG, IFRAME, or OBJECT, and Youtube is treated
					// separately

				} else if (i.getType() == SimplePageItem.MULTIMEDIA) {
				    // This code should be read together with the code in SimplePageBean
				    // that sets up this data, method addMultimedia.  Most display is set
				    // up here, but note that show-page.js invokes the jquery oembed on all
				    // <A> items with class="oembed".

				    // historically this code was to display files ,and urls leading to things
				    // like MP4. as backup if we couldn't figure out what to do we'd put something
				    // in an iframe. The one exception is youtube, which we supposed explicitly.
				    //   However we now support several ways to embed content. We use the
				    // multimediaDisplayType code to indicate which. The codes are
				    // 	 1 -- embed code, 2 -- av type, 3 -- oembed, 4 -- iframe
				    // 2 is the original code: MP4, image, and as a special case youtube urls
				    // since we have old entries with no type code, and that behave the same as
				    // 2, we start by converting 2 to null.
				    //  then the logic is
				    //  if type == null & youtube, do youtube
				    //  if type == null & image, do iamge
				    //  if type == null & not HTML do MP4 or other player for file 
				    //  final fallthrough to handel the new types, with IFRAME if all else fails
				    // the old code creates ojbects in ContentHosting for both files and URLs.
				    // The new code saves the embed code or URL itself as an atteibute of the item
				    // If I were doing it again, I wouldn't create the ContebtHosting item
				    //   Note that IFRAME is only used for something where the far end claims the MIME
				    // type is HTML. For weird stuff like MS Word files I use the file display code, which
				    // will end up producing <OBJECT>.

					// the reason this code is complex is that we try to choose
					// the best
					// HTML for displaying the particular type of object. We've
					// added complexities
					// over time as we get more experience with different
					// object types and browsers.

				 	String itemGroupString = null;
					String itemGroupTitles = null;
					boolean entityDeleted = false;
					// new format explicit display indication
					String mmDisplayType = i.getAttribute("multimediaDisplayType");
					// 2 is the generic "use old display" so treat it as null
					if ("".equals(mmDisplayType) || "2".equals(mmDisplayType))
					    mmDisplayType = null;
					if (canSeeAll) {
					    try {
						itemGroupString = simplePageBean.getItemGroupStringOrErr(i, null, true);
					    } catch (IdUnusedException e) {
						itemGroupString = "";
						entityDeleted = true;
					    }
					    itemGroupTitles = simplePageBean.getItemGroupTitles(itemGroupString, i);
					    if (entityDeleted) {
						if (itemGroupTitles != null)
						    itemGroupTitles = itemGroupTitles + " " + messageLocator.getMessage("simplepage.deleted-entity");
						else
						    itemGroupTitles = messageLocator.getMessage("simplepage.deleted-entity");
					    }
					    if (itemGroupTitles != null) {
						itemGroupTitles = "[" + itemGroupTitles + "]";
					    }
					    UIOutput.make(tableRow, "item-groups", itemGroupString);
					} else if (entityDeleted)
					    continue;
					
					if (!"1".equals(mmDisplayType) && !"3".equals(mmDisplayType))
					    UIVerbatim.make(tableRow, "item-path", getItemPath(i));

					// the reason this code is complex is that we try to choose
					// the best
					// HTML for displaying the particular type of object. We've
					// added complexities
					// over time as we get more experience with different
					// object types and browsers.

					// String extension = Validator.getFileExtension(i.getSakaiId());

					// the extension is almost never used. Normally we have
					// the MIME type and use it. Extension is used only if
					// for some reason we don't have the MIME type
					UIComponent item;
					String youtubeKey;

					String widthSt = i.getWidth();
					Length width = null;

					if (mmDisplayType == null && simplePageBean.isImageType(i)) {
						// a wide default for images would produce completely wrong effect
					    	if (StringUtils.isNotBlank(widthSt))
						    width = new Length(widthSt);
					} else if (StringUtils.isBlank(widthSt)) {
						width = new Length(DEFAULT_WIDTH);
					} else {
						width = new Length(widthSt);
					}

					Length height = null;
					if (StringUtils.isNotBlank(i.getHeight())) {
						height = new Length(i.getHeight());
					}

					// Get the MIME type.
					String mimeType = simplePageBean.getContentType(i);

					// here goes. dispatch on the type and produce the right tag
					// type,
					// followed by the hidden INPUT tags with information for the
					// edit dialog
					if (mmDisplayType == null && simplePageBean.isImageType(i)) {

					    boolean available = simplePageBean.isItemAvailable(i);
					    if(canSeeAll || available) {
					    	    if (!available)
							UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.fake-item-unavailable"));
						    UIOutput.make(tableRow, "imageSpan");

						    if (itemGroupString != null) {
							    UIOutput.make(tableRow, "item-group-titles3", itemGroupTitles);
							    UIOutput.make(tableRow, "item-groups3", itemGroupString);
						    }

						    String imageName = i.getAlt();
						    if (imageName == null || imageName.equals("")) {
							    imageName = abbrevUrl(i.getURL());
						    }

						    item = UIOutput.make(tableRow, "image").decorate(new UIFreeAttributeDecorator("src", i.getItemURL(simplePageBean.getCurrentSiteId(),currentPage.getOwner()))).decorate(new UIFreeAttributeDecorator("alt", imageName));
						    if (lengthOk(width)) {
							    item.decorate(new UIFreeAttributeDecorator("width", width.getOld()));
						    }
						
						    if(lengthOk(height)) {
							    item.decorate(new UIFreeAttributeDecorator("height", height.getOld()));
						    }
					    } else {
					        UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.multimediaItemUnavailable"));
					    }

						// stuff for the jquery dialog
						if (canEditPage) {
							UIOutput.make(tableRow, "imageHeight", getOrig(height));
							UIOutput.make(tableRow, "imageWidth", widthSt);  // original value from database
							UIOutput.make(tableRow, "mimetype2", mimeType);
							UIOutput.make(tableRow, "current-item-id4", Long.toString(i.getId()));
							UIOutput.make(tableRow, "item-prereq3", String.valueOf(i.isPrerequisite()));
							UIVerbatim.make(tableRow, "item-path3", getItemPath(i));
							UIOutput.make(tableRow, "editimage-td").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.url").replace("{}", abbrevUrl(i.getURL()))));
							UILink.make(tableRow, "image-edit", (String)null, "").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.url").replace("{}", abbrevUrl(i.getURL()))));
						}
						
						UIOutput.make(tableRow, "description2", i.getDescription());

					} else if (mmDisplayType == null && (youtubeKey = simplePageBean.getYoutubeKey(i)) != null) {
						String youtubeUrl = SimplePageBean.getYoutubeUrlFromKey(youtubeKey);

						boolean available = simplePageBean.isItemAvailable(i);
						if(canSeeAll || simplePageBean.isItemAvailable(i)) {
						    if (!available)
							UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.fake-item-unavailable"));

						    UIOutput.make(tableRow, "youtubeSpan");

						    if (itemGroupString != null) {
							    UIOutput.make(tableRow, "item-group-titles4", itemGroupTitles);
							    UIOutput.make(tableRow, "item-groups4", itemGroupString);
						    }

						    // if width is blank or 100% scale the height

						    // <object style="height: 390px; width: 640px"><param
						    // name="movie"
						    // value="http://www.youtube.com/v/AKIC7OQqBrA?version=3"><param
						    // name="allowFullScreen" value="true"><param
						    // name="allowScriptAccess" value="always"><embed
						    // src="http://www.youtube.com/v/AKIC7OQqBrA?version=3"
						    // type="application/x-shockwave-flash"
						    // allowfullscreen="true" allowScriptAccess="always"
						    // width="640" height="390"></object>

						    item = UIOutput.make(tableRow, "youtubeIFrame")
									.decorate(new UIFreeAttributeDecorator("allow", String.join(";",
											Optional.ofNullable(ServerConfigurationService.getStrings("browser.feature.allow"))
													.orElseGet(() -> new String[]{}))));
						    // youtube seems ok with length and width
						    if(lengthOk(height)) {
							    item.decorate(new UIFreeAttributeDecorator("height", height.getOld()));
						    }
						    else if(!lengthOk(height) && lengthOk(width) && ("px".equals(width.unit) || "".equals(width.unit))) {
							    // Youtube seems to use aspect ratio of 16*9 from 2015 on
							    int youtubeDerivedHeight = (int) Math.ceil(new Double(width.getOld()) * 9 / 16);
							    item.decorate(new UIFreeAttributeDecorator("height", youtubeDerivedHeight + ""));
						    }
						
						    if(lengthOk(width)) {
							    item.decorate(new UIFreeAttributeDecorator("width", width.getOld()));
						    }
						
						    item.decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.youtube_player")));
						    item.decorate(new UIFreeAttributeDecorator("src", youtubeUrl));
						} else {
						    UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.multimediaItemUnavailable"));
						}

						if (canEditPage) {
							UIOutput.make(tableRow, "youtubeId", String.valueOf(i.getId()));
							UIOutput.make(tableRow, "currentYoutubeURL", youtubeUrl);
							UIOutput.make(tableRow, "currentYoutubeHeight", getOrig(height));
							UIOutput.make(tableRow, "currentYoutubeWidth", widthSt);
							UIOutput.make(tableRow, "current-item-id5", Long.toString(i.getId()));
							UIOutput.make(tableRow, "item-prereq4", String.valueOf(i.isPrerequisite()));
							UIVerbatim.make(tableRow, "item-path4", getItemPath(i));
							UIOutput.make(tableRow, "youtube-td").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.youtube")));
							UILink.make(tableRow, "youtube-edit", (String)null, "").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.youtube")));
						}

						UIOutput.make(tableRow, "description4", i.getDescription());
						
						// as of Oct 28, 2010, we store the mime type. mimeType
						// null is an old entry.
						// For that use the old approach of checking the
						// extension.
						// Otherwise we want to use iframes for HTML and OBJECT
						// for everything else
						// We need the iframes because IE up through 8 doesn't
						// reliably display
						// HTML with OBJECT. Experiments show that everything
						// else works with OBJECT
						// for most browsers. Unfortunately IE, even IE 9,
						// doesn't reliably call the
						// right player with OBJECT. EMBED works. But it's not
						// as nice because you can't
						// nest error recovery code. So we use OBJECT for
						// everything except IE, where we
						// use EMBED. OBJECT does work with Flash.
						// application/xhtml+xml is XHTML.

						// mimeType is 
					} else if (mmDisplayType == null && !simplePageBean.isHtmlType(i)) {

                        // except where explicit display is set,
			// this code is used for everything that isn't an image,
                        // Youtube, or HTML
			// This could be audio, video, flash, or something random like MS word.
                        // Random stuff will turn into an object.
                        // HTML is done with an IFRAME in the next "if" case
		        // The explicit display types are handled there as well

					    // in theory the things that fall through to iframe are
					    // html and random stuff without a defined mime type
					    // random stuff with mime type is displayed with object

                        String oMimeType = mimeType; // in case we change it for
                        // FLV or others

                        if (itemGroupString != null) {
                            UIOutput.make(tableRow, "item-group-titles5", itemGroupTitles);
                            UIOutput.make(tableRow, "item-groups5", itemGroupString);
                        }

			UIOutput.make(tableRow, "movieSpan");

			boolean available = simplePageBean.isItemAvailable(i);
                        if(canSeeAll || available) {
			    if (!available)
				UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.fake-item-unavailable"));

			    UIComponent item2;

                            String movieUrl = i.getItemURL(simplePageBean.getCurrentSiteId(),currentPage.getOwner());
                            // movieUrl = "https://heidelberg.rutgers.edu" + movieUrl;
                            // Safari doens't always pass cookies to plugins, so we have to pass the arg
                            // this requires session.parameter.allow=true in sakai.properties
                            // don't pass the arg unless that is set, since the whole point of defaulting
                            // off is to not expose the session id
                            String sessionParameter = getSessionParameter(movieUrl);
                            if (sessionParameter != null)
                                movieUrl = movieUrl + "?lb.session=" + sessionParameter;

			    UIComponent movieLink = UIOutput.make(tableRow, "movie-link-div");

                            //	if (allowSessionId)
                            //  movieUrl = movieUrl + "?sakai.session=" + SessionManager.getCurrentSession().getId();
                            // Determine the appropriate player based on content type
                            boolean isHtml5Compatible = Arrays.binarySearch(html5Types, mimeType) >= 0;
                            boolean isAudio = mimeType != null && mimeType.startsWith("audio/");
                            boolean isPDF = simplePageBean.isPDFType(i);
                            boolean isWavAudio = mimeType != null && (mimeType.equals("audio/wav") || mimeType.equals("audio/x-wav"));
                            
                            // Step 1: Create HTML5 player for compatible media types (modern browsers)
                            if (isHtml5Compatible) {
                                // Create the appropriate HTML5 element (audio or video)
                                UIComponent html5Player = UIOutput.make(tableRow, (isAudio ? "h5audio" : "h5video"));
                                UIComponent html5Source = UIOutput.make(tableRow, (isAudio ? "h5asource" : "h5source"));
                                
                                // Set up dimensions using CSS style (required for HTML5)
                                StringBuilder styleBuilder = new StringBuilder();
                                if (lengthOk(height)) {
                                    styleBuilder.append("height: ").append(height.getNew());
                                }
                                if (lengthOk(width)) {
                                    if (styleBuilder.length() > 0) {
                                        styleBuilder.append("; ");
                                    }
                                    styleBuilder.append("width: ").append(width.getNew());
                                }
                                
                                // Apply style if dimensions specified
                                if (styleBuilder.length() > 0) {
                                    html5Player.decorate(new UIFreeAttributeDecorator("style", styleBuilder.toString()));
                                }
                                
                                // Set the media source
                                html5Source.decorate(new UIFreeAttributeDecorator("src", movieUrl))
                                         .decorate(new UIFreeAttributeDecorator("type", mimeType));
                                
                                // Handle captions for video content
                                String caption = i.getAttribute("captionfile");
                                if (!isAudio && caption != null && caption.length() > 0) {
                                    movieLink.decorate(new UIStyleDecorator("has-caption allow-caption"));
                                    String captionUrl = "/access/lessonbuilder/item/" + i.getId() + caption;
                                    sessionParameter = getSessionParameter(captionUrl);
                                    if (sessionParameter != null) {
                                        captionUrl = captionUrl + "?lb.session=" + sessionParameter;
                                    }
                                    UIOutput.make(tableRow, "h5track")
                                        .decorate(new UIFreeAttributeDecorator("src", captionUrl));
                                } else if (!isAudio) {
                                    movieLink.decorate(new UIStyleDecorator("allow-caption"));
                                }
                            }

                            // Step 2: Create fallback players for browsers or content types not supporting HTML5
                            
                            // Handle PDF files with PDF.js viewer
                            if (isPDF) {
                                try {
                                    // URL encode for PDF.js viewer (needed for special characters)
                                    movieUrl = URLEncoder.encode(movieUrl, "UTF-8")
                                        .replaceAll("\\+", "%20")
                                        .replaceAll("\\%21", "!")
                                        .replaceAll("\\%27", "'")
                                        .replaceAll("\\%28", "(")
                                        .replaceAll("\\%29", ")")
                                        .replaceAll("\\%7E", "~");
                                } catch (Exception ex) {
                                    log.warn("Error encoding the PDF url, the PDF might not load in the UI. {}", ex.getMessage());
                                }
                                String pdfViewerUrl = String.format("/library/webjars/pdf-js/5.3.31/web/viewer.html?file=%s", movieUrl);
                                item2 = UIOutput.make(tableRow, "pdfEmbed")
                                        .decorate(new UIFreeAttributeDecorator("src", pdfViewerUrl))
                                        .decorate(new UIFreeAttributeDecorator("alt", messageLocator.getMessage("simplepage.mm_player").replace("{}", abbrevUrl(i.getURL()))));
                            } 
                            // Use embed tag for WAV files to prevent automatic download
                            else if (isWavAudio) {
                                item2 = UIOutput.make(tableRow, "movieEmbed")
                                        .decorate(new UIFreeAttributeDecorator("src", movieUrl))
                                        .decorate(new UIFreeAttributeDecorator("alt", messageLocator.getMessage("simplepage.mm_player").replace("{}", abbrevUrl(i.getURL()))));
                            } 
                            // Use object tag for all other content (PowerPoint, etc.)
                            else {
                                item2 = UIOutput.make(tableRow, "movieObject")
                                        .decorate(new UIFreeAttributeDecorator("data", movieUrl))
                                        .decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.mm_player").replace("{}", abbrevUrl(i.getURL()))));
                            }
                            if (mimeType != null) {
                                item2.decorate(new UIFreeAttributeDecorator("type", mimeType));
                            }
                            if (canEditPage) {
                                //item2.decorate(new UIFreeAttributeDecorator("style", "border: 1px solid black"));
                            }

                            // some object types seem to need a specification, so supply our default if necessary
                            if (lengthOk(height) && lengthOk(width)) {
                                item2.decorate(new UIFreeAttributeDecorator("height", height.getOld())).decorate(new UIFreeAttributeDecorator("width", width.getOld()));
                            } else if (definiteLength(width)) {
				// this is mostly because the default is 640 with no height specified
				// we've validated width, so no errors in conversion should occur
				Double h = new Double(width.getOld()) * 0.75;
                                if (oMimeType.startsWith("audio/"))
				    h = 100.0;
                                item2.decorate(new UIFreeAttributeDecorator("height", Double.toString(h))).decorate(new UIFreeAttributeDecorator("width", width.getOld()));
				// flag for javascript to adjust height
				if (!oMimeType.startsWith("audio/"))
				    item2.decorate(new UIFreeAttributeDecorator("defaultsize","true"));
			    } else {				
                                if (oMimeType.startsWith("audio/"))
                                item2.decorate(new UIFreeAttributeDecorator("height", "100")).decorate(new UIFreeAttributeDecorator("width", "400"));
                                else
                                item2.decorate(new UIFreeAttributeDecorator("height", "300")).decorate(new UIFreeAttributeDecorator("width", "400"));
                            }
                            if (!isWavAudio) { // Only add extra parameters for non-WAV files
                                UIOutput.make(tableRow, "movieURLInject").decorate(new UIFreeAttributeDecorator("value", movieUrl));
                                if (showDownloads) {
                                    UIOutput.make(tableRow, "noplugin-p", messageLocator.getMessage("simplepage.noplugin"));
                                    UIOutput.make(tableRow, "noplugin-br");
                                    UILink.make(tableRow, "noplugin", i.getName(), movieUrl);
                                } 
                            }

			    UIOutput.make(tableRow, "description3", i.getDescription());
                        } else {
			    UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.multimediaItemUnavailable"));
                        }

						if (canEditPage) {
							UIOutput.make(tableRow, "movieId", String.valueOf(i.getId()));
							UIOutput.make(tableRow, "movieHeight", getOrig(height));
							UIOutput.make(tableRow, "movieWidth", widthSt);
							UIOutput.make(tableRow, "mimetype5", oMimeType);
							UIOutput.make(tableRow, "prerequisite", (i.isPrerequisite()) ? "true" : "false");
							UIOutput.make(tableRow, "current-item-id6", Long.toString(i.getId()));
							UIVerbatim.make(tableRow, "item-path5", getItemPath(i));
							UIOutput.make(tableRow, "movie-td").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.url").replace("{}", abbrevUrl(i.getURL()))));
							UILink.make(tableRow, "edit-movie", (String)null, "").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.url").replace("{}", abbrevUrl(i.getURL()))));
						}
						
					} else {
					    // this is fallthrough for html or an explicit mm display type (i.e. embed code)
					    // odd types such as MS word will be handled by the AV code, and presented as <OBJECT>

					    boolean available = simplePageBean.isItemAvailable(i);
					    if(canSeeAll || available) {
						if (!available)
						    UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.fake-item-unavailable"));

						// definition of resizeiframe, at top of page
						if (!iframeJavascriptDone && getOrig(height).equals("auto")) {
							UIOutput.make(tofill, "iframeJavascript");
							iframeJavascriptDone = true;
						}

						UIOutput.make(tableRow, "iframeSpan");

						if (itemGroupString != null) {
							UIOutput.make(tableRow, "item-group-titles2", itemGroupTitles);
							UIOutput.make(tableRow, "item-groups2", itemGroupString);
						}
						String itemUrl = i.getItemURL(simplePageBean.getCurrentSiteId(),currentPage.getOwner());
						if ("1".equals(mmDisplayType)) {
						    // embed
						    item = UIVerbatim.make(tableRow, "mm-embed", i.getAttribute("multimediaEmbedCode"));
						    //String style = getStyle(width, height);
						    //if (style != null)
						    //item.decorate(new UIFreeAttributeDecorator("style", style));
						} else if ("3".equals(mmDisplayType)) {
						    item = UILink.make(tableRow, "mm-oembed", i.getAttribute("multimediaUrl"), i.getAttribute("multimediaUrl"));
						    if (lengthOk(width))
							item.decorate(new UIFreeAttributeDecorator("maxWidth", width.getOld()));
						    if (lengthOk(height))
							item.decorate(new UIFreeAttributeDecorator("maxHeight", height.getOld()));
						    // oembed
						} else  {
						    UIOutput.make(tableRow, "iframe-link-div");
						    UILink.make(tableRow, "iframe-link-link", messageLocator.getMessage("simplepage.open_new_window"), itemUrl);
						    item = UIOutput.make(tableRow, "iframe")
									.decorate(new UIFreeAttributeDecorator("src", itemUrl))
									.decorate(new UIFreeAttributeDecorator("allow", String.join(";",
											Optional.ofNullable(ServerConfigurationService.getStrings("browser.feature.allow"))
													.orElseGet(() -> new String[]{}))));
						    // if user specifies auto, use Javascript to resize the
						    // iframe when the
						    // content changes. This only works for URLs with the
						    // same origin, i.e.
						    // URLs in this sakai system
						    if (getOrig(height).equals("auto")) {
							item.decorate(new UIFreeAttributeDecorator("onload", "resizeiframe('" + item.getFullID() + "')"));
							if (lengthOk(width)) {
							    item.decorate(new UIFreeAttributeDecorator("width", width.getOld()));
							}
							item.decorate(new UIFreeAttributeDecorator("height", "300"));
						    } else {
							// we seem OK without a spec
							if (lengthOk(height) && lengthOk(width)) {
								item.decorate(new UIFreeAttributeDecorator("height", height.getOld())).decorate(new UIFreeAttributeDecorator("width", width.getOld()));
							}
						    }
						}
						item.decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.web_content").replace("{}", abbrevUrl(i.getURL()))));

						if (canEditPage) {
							UIOutput.make(tableRow, "iframeHeight", getOrig(height));
							UIOutput.make(tableRow, "iframeWidth", widthSt);
							UIOutput.make(tableRow, "mimetype3", mimeType);
							UIOutput.make(tableRow, "item-prereq2", String.valueOf(i.isPrerequisite()));
							UIOutput.make(tableRow, "embedtype", mmDisplayType);
							UIOutput.make(tableRow, "current-item-id3", Long.toString(i.getId()));
							UIVerbatim.make(tableRow, "item-path2", getItemPath(i));
							UIOutput.make(tableRow, "editmm-td").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.url").replace("{}", abbrevUrl(i.getURL()))));
							UILink.make(tableRow, "iframe-edit", (String)null, "").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.url").replace("{}", abbrevUrl(i.getURL()))));
						}
						
						UIOutput.make(tableRow, "description5", i.getDescription());
					    } else {
					        UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.multimediaItemUnavailable"));
					    }

					}

					// end of multimedia object

				} else if (i.getType() == SimplePageItem.COMMENTS) {
					// Load later using AJAX and CommentsProducer

					UIOutput.make(tableRow, "commentsSpan");

					boolean isAvailable = simplePageBean.isItemAvailable(i);
					// faculty missing preqs get warning but still see the comments
					if (!isAvailable && canSeeAll)
					    UIOutput.make(tableRow, "missing-prereqs", messageLocator.getMessage("simplepage.fake-missing-prereqs"));

					// students get warning and not the content
					if (!isAvailable && !canSeeAll) {
					    UIOutput.make(tableRow, "missing-prereqs", messageLocator.getMessage("simplepage.missing-prereqs"));
					}else {
						UIOutput.make(tableRow, "commentsDiv");
						UIOutput.make(tableRow, "placementId", placement.getId());

					        // note: the URL will be rewritten in comments.js to look like
					        //  /lessonbuilder-tool/faces/Comments...
						CommentsViewParameters eParams = new CommentsViewParameters(CommentsProducer.VIEW_ID);
						eParams.itemId = i.getId();
						eParams.placementId = placement.getId();
						if (params.postedComment) {
							eParams.postedComment = postedCommentId;
						}
						eParams.siteId = simplePageBean.getCurrentSiteId();
						eParams.pageId = currentPage.getPageId();
						
						if(params.author != null && !params.author.equals("")) {
							eParams.author = params.author;
							eParams.showAllComments = true;
						}

						UIInternalLink.make(tableRow, "commentsLink", eParams);

						if (!addedCommentsScript) {
							UIOutput.make(tofill, "comments-script");
							UIOutput.make(tofill, "fckScript");
							addedCommentsScript = true;
							UIOutput.make(tofill, "delete-comment-confirm-dialog");
						}
						
						// forced comments have to be edited on the main page
						if (canEditPage) {
							// Checks to make sure that the comments item isn't on a student page.
							// That it is graded.  And that we didn't just come from the grading pane.
							if(i.getPageId() > 0 && i.getGradebookId() != null && !cameFromGradingPane) {
								CommentsGradingPaneViewParameters gp = new CommentsGradingPaneViewParameters(CommentGradingPaneProducer.VIEW_ID);
								gp.placementId = toolManager.getCurrentPlacement().getId();
								gp.commentsItemId = i.getId();
								gp.pageId = currentPage.getPageId();
								gp.pageItemId = pageItem.getId();
								gp.siteId = simplePageBean.getCurrentSiteId();
								
								UIInternalLink.make(tableRow, "gradingPaneLink", gp)
								    .decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.show-grading-pane-comments")));
							}

							UIOutput.make(tableRow, "comments-td").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.comments")));
						
							if (i.getSequence() > 0) {
							    UILink.make(tableRow, "edit-comments", (String)null, "")
									.decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.comments")));

							    UIOutput.make(tableRow, "commentsId", String.valueOf(i.getId()));
							    UIOutput.make(tableRow, "commentsAnon", String.valueOf(i.isAnonymous()));
							    UIOutput.make(tableRow, "commentsitem-required", String.valueOf(i.isRequired()));
							    UIOutput.make(tableRow, "commentsitem-prerequisite", String.valueOf(i.isPrerequisite()));
							    UIOutput.make(tableRow, "commentsGrade", String.valueOf(i.getGradebookId() != null));
							    UIOutput.make(tableRow, "commentsMaxPoints", String.valueOf(i.getGradebookPoints()));
							
							    String itemGroupString = simplePageBean.getItemGroupString(i, null, true);
							    if (itemGroupString != null) {
							    	String itemGroupTitles = simplePageBean.getItemGroupTitles(itemGroupString, i);
							    	if (itemGroupTitles != null) {
							    		itemGroupTitles = "[" + itemGroupTitles + "]";
							    	}
							    	UIOutput.make(tableRow, "comments-groups", itemGroupString);
							    	UIOutput.make(tableRow, "item-group-titles6", itemGroupTitles);
							    }
							}
					    	
							// Allows AJAX posting of comment grades
					    	printGradingForm(tofill);
					    }

					    UIForm form = UIForm.make(tableRow, "comment-form");
					    makeCsrf(form, "csrf2");

					    UIInput.make(form, "comment-item-id", "#{simplePageBean.itemId}", String.valueOf(i.getId()));
					    UIInput.make(form, "comment-edit-id", "#{simplePageBean.editId}");

					    // usage * image is required and not done
					    if (i.isRequired() && !simplePageBean.isItemComplete(i))
						UIOutput.make(tableRow, "comment-required-image");

					    UIOutput.make(tableRow, "add-comment-link");
					    UIOutput.make(tableRow, "add-comment-text", messageLocator.getMessage("simplepage.add-comment"));
					    UIInput fckInput = UIInput.make(form, "comment-text-area-evolved:", "#{simplePageBean.formattedComment}");
					    fckInput.decorate(new UIFreeAttributeDecorator("height", "175"));
					    fckInput.decorate(new UIFreeAttributeDecorator("width", "800"));
					    fckInput.decorate(new UIStyleDecorator("evolved-box"));
					    fckInput.decorate(new UIFreeAttributeDecorator("aria-label", messageLocator.getMessage("simplepage.editor")));
					    fckInput.decorate(new UIFreeAttributeDecorator("role", "dialog"));

					    if (!noEditor) {
						fckInput.decorate(new UIStyleDecorator("using-editor"));  // javascript needs to know
						((SakaiFCKTextEvolver) richTextEvolver).evolveTextInput(fckInput, "" + commentsCount);
					    }
					    UICommand.make(form, "add-comment", "#{simplePageBean.addComment}");
					}
				
				}else if (i.getType() == SimplePageItem.PEEREVAL){
					
					String owner=currentPage.getOwner();
					String currentUser=UserDirectoryService.getCurrentUser().getId();
					Long pageId=currentPage.getPageId();
					
					boolean isOpen = false;
					boolean isPastDue = false;
					
					String peerEvalDateOpenStr = i.getAttribute("rubricOpenDate");
					String peerEvalDateDueStr  = i.getAttribute("rubricDueDate");
					boolean peerEvalAllowSelfGrade = Boolean.parseBoolean(i.getAttribute("rubricAllowSelfGrade"));
					boolean gradingSelf = owner.equals(currentUser) && peerEvalAllowSelfGrade;
				
					if (peerEvalDateOpenStr != null && peerEvalDateDueStr != null) {
						Date peerEvalNow = new Date();
						Date peerEvalOpen = new Date(Long.valueOf(peerEvalDateOpenStr));
						Date peerEvalDue = new Date(Long.valueOf(peerEvalDateDueStr));
						
						isOpen = peerEvalNow.after(peerEvalOpen);
						isPastDue = peerEvalNow.after(peerEvalDue);
					}
					
					if(isOpen){
						
					    // data needed to figure out what to show
					    // there are three cases:
					    // individual
					    // group where we evaluate the group
					    // group where we evaluate individuals
					    // for historical reasons when we evaluate the group the first person
					    // to create the group is shown as the owner.
					    
					        // construct row text -> row id
					        // old entries are by text, so need to be able to map them to id

					        Map<String, Long> catMap = new HashMap<String, Long>();

						List<Map> categories = (List<Map>) i.getJsonAttribute("rows");
						if (categories == null)   // not valid to do update on item without rubic
						    continue; 
						for (Map cat: categories) {
						    String rowText = String.valueOf(cat.get("rowText"));
						    String rowId = String.valueOf(cat.get("id"));
						    catMap.put(rowText, new Long(rowId));
						}

						List<String>groupMembers = simplePageBean.studentPageGroupMembers(i, null);

						boolean groupOwnedIndividual = (i.isGroupOwned() && "true".equals(i.getAttribute("group-eval-individual")));

						// if we should show form. 
						// individual owned
						// group owned and eval group
						// group owned and eval individual and we're in the group
						// i.e. not eval individual and we're outside group
						if(!(groupOwnedIndividual && !groupMembers.contains(currentUser))) {
						    UIOutput.make(tableRow, "peerReviewRubricStudent");
						    UIOutput.make(tableRow, "peer-eval-title-student", String.valueOf(i.getAttribute("rubricTitle")));
						    UIForm peerForm = UIForm.make(tableRow, "peer-review-form");
						    UIInput.make(peerForm, "peer-eval-itemid", "#{simplePageBean.itemId}", String.valueOf(i.getId()));
						    
						    // originally evalTargets was a list if ID's.
						    // but we need to sort by name, so unless we want to keep repeatedly
						    // going from ID to name, we need to use this:
						    class Target {
							String id;
							String name;
							String sort;
							Target(String i) {
							    name = i;
							    try {
								User u = UserDirectoryService.getUser(i);
								name = u.getDisplayName();
								sort = u.getSortName();
							    } catch (Exception ignore) {}
							    id = i;
							}
						    }

						    List<Target>evalTargets = new ArrayList<Target>();
						    if (groupOwnedIndividual) {
							String group = simplePageBean.getCurrentPage().getGroup();
							if (group != null)
							    group = "/site/" + simplePageBean.getCurrentSiteId() + "/group/" + group;
							try {
							    AuthzGroup g = authzGroupService.getAuthzGroup(group);
							    Set<Member> members = g.getMembers();
							    for (Member m: members) {
								evalTargets.add(new Target(m.getUserId()));
							    }
							} catch (Exception e) {
							    log.info("unable to get members of group " + group);
							}
							// no need to sort for other alternative, when there's only one
							Collections.sort(evalTargets, new Comparator<Target>() {
								public int compare(Target o1, Target o2) {
								    return o1.sort.compareTo(o2.sort);
								}
							    });
						    } else {
							Target target = new Target(owner);
							// individual handled above. So if group we're evaluating
							// the group. Use group name
							if (i.isGroupOwned()) {
							    String group = simplePageBean.getCurrentPage().getGroup();
							    target.name = simplePageBean.getCurrentSite().getGroup(group).getTitle();
							}
							evalTargets.add(target);
						    }

						    // for old format entries always need page owner or evaluee
						    // for new format when evaluating page need groupId
						    String groupId = null;
						    if (i.isGroupOwned() && !groupOwnedIndividual)
							groupId = simplePageBean.getCurrentPage().getGroup();

						    for (Target target: evalTargets) {
							UIContainer entry = UIBranchContainer.make(peerForm, "peer-eval-target:");
						    // for each target

							Map<Long, Map<Integer, Integer>> dataMap = new HashMap<Long, Map<Integer, Integer>>();
							// current data to show to target, all evaluations of target
							// But first see if we should show current data. Only show
							// user data evaluating him
							if ((i.isGroupOwned() && !groupOwnedIndividual && groupMembers.contains(currentUser)) ||
							    target.id.equals(currentUser)) {
							
							    List<SimplePagePeerEvalResult> evaluations = simplePageToolDao.findPeerEvalResultByOwner(pageId.longValue(), target.id, groupId);
							    
							    if(evaluations!=null) {
								for(SimplePagePeerEvalResult eval : evaluations) {
								    // for individual eval only show results for that one
									if (groupOwnedIndividual && !currentUser.equals(eval.getGradee()))
									    continue;
									Long rowId = eval.getRowId();
									if (rowId == 0L)
									    rowId = catMap.get(eval.getRowText());
									if (rowId == null)
									    continue; // don't recognize old-format entry

									Map<Integer, Integer> rowMap = dataMap.get(rowId);
									if (rowMap == null) {
									    rowMap = new HashMap<Integer, Integer>();
									    dataMap.put(rowId, rowMap);
									}
									Integer n = rowMap.get(eval.getColumnValue());
									if (n == null)
									    n = 1;
									else 
									    n++;
									rowMap.put(eval.getColumnValue(), n);
								}
							    }
							    
							}
							// end current data

							// now get current data to initiaize form. That's just
							// the submission by current user.
							List<SimplePagePeerEvalResult> evaluations = simplePageToolDao.findPeerEvalResult(pageId, currentUser, target.id, groupId);
							Map<Long,Integer> selectedCells = new HashMap<Long,Integer>();
							for (SimplePagePeerEvalResult result: evaluations) {
							    Long rowId = result.getRowId();
							    String rowText = result.getRowText();
							    if (rowId == 0L)
								rowId = catMap.get(rowText);
							    if (rowId == null)
								continue;
							    selectedCells.put(rowId, new Integer(result.getColumnValue()));
							}

							// for each student being evaluated
							UIOutput.make(entry, "peer-eval-target-name", target.name);
							UIOutput.make(entry, "peer-eval-target-id", target.id);

							// keep this is sync with canSubmit in SimplePageBean.savePeerEvalResult
							boolean canSubmit = (!i.isGroupOwned() && (!owner.equals(currentUser) || gradingSelf) ||
									     i.isGroupOwned() && !groupOwnedIndividual && (!groupMembers.contains(currentUser) || peerEvalAllowSelfGrade) ||
									     groupOwnedIndividual && groupMembers.contains(currentUser) && (peerEvalAllowSelfGrade || !target.id.equals(currentUser)));

							makePeerRubric(entry, i, makeStudentRubric, selectedCells, 
								       dataMap, canSubmit);

						    }

						    // can submit
						    // individual and (not that individual or gradingeself)
						    // group and (not in group or gradingself)
						    // group individual eval and in group
						    if(!i.isGroupOwned() && (!owner.equals(currentUser) || gradingSelf) ||
						       i.isGroupOwned() && !groupOwnedIndividual && (!groupMembers.contains(currentUser) || peerEvalAllowSelfGrade) ||
						       groupOwnedIndividual && groupMembers.contains(currentUser)) {

							// can actually submit

							if(isPastDue) {
							    UIOutput.make(tableRow, "peer-eval-grade-directions", messageLocator.getMessage("simplepage.peer-eval.past-due-date"));
							} else {
							    makeCsrf(peerForm, "csrf6");
							    UICommand.make(peerForm, "save-peereval-link",  messageLocator.getMessage("simplepage.submit"), "#{simplePageBean.savePeerEvalResult}");
							    UIOutput.make(peerForm, "save-peereval-text", messageLocator.getMessage("simplepage.save"));
							    UIOutput.make(peerForm, "cancel-peereval-button");
							    UIOutput.make(peerForm, "cancel-peereval-link");
							    UIOutput.make(peerForm, "cancel-peereval-text", messageLocator.getMessage("simplepage.cancel"));
							
							    UIOutput.make(tableRow, "peer-eval-grade-directions", messageLocator.getMessage("simplepage.peer-eval.click-on-cell"));
							} 

						    // in theory the only case where we show the form and can't grade
						    // is if it's for yourself.
						    } else {
							UIOutput.make(tableRow, "peer-eval-grade-directions", messageLocator.getMessage("simplepage.peer-eval.cant-eval-yourself"));
						    }
						//buttons
						UIOutput.make(tableRow, "add-peereval-link");
						UIOutput.make(tableRow, "add-peereval-text", messageLocator.getMessage("simplepage.view-peereval"));
						
						}
						if(canEditPage)
							UIOutput.make(tableRow, "peerReviewRubricStudent-edit");//lines up rubric with edit btn column for users with editing privs
					}
				}else if(i.getType() == SimplePageItem.STUDENT_CONTENT) {
					
					UIOutput.make(tableRow, "studentSpan");

					boolean isAvailable = simplePageBean.isItemAvailable(i);
					// faculty missing preqs get warning but still see the comments
					if (!isAvailable && canSeeAll)
					    UIOutput.make(tableRow, "student-missing-prereqs", messageLocator.getMessage("simplepage.student-fake-missing-prereqs"));
					if (!isAvailable && !canSeeAll)
					    UIOutput.make(tableRow, "student-missing-prereqs", messageLocator.getMessage("simplepage.student-missing-prereqs"));
					else {
						boolean isGrader = simplePageBean.getEditPrivs() == 0;

						UIOutput.make(tableRow, "studentDiv");
						
						HashMap<Long, SimplePageLogEntry> cache = simplePageBean.cacheStudentPageLogEntries(i.getId());
						List<SimpleStudentPage> studentPages = simplePageToolDao.findStudentPages(i.getId());

						// notSubmitted will be list of students or groups that didn't submit. Start with those who
						// should submit and remove as we see them
						Set<String> notSubmitted = new HashSet<String>();
						if (i.isGroupOwned()) {
						    notSubmitted = simplePageBean.getOwnerGroups(i);
						} else {
						    String siteRef = simplePageBean.getCurrentSite().getReference();
						    // only check students
						    List<User> studentUsers = securityService.unlockUsers("section.role.student", siteRef);
						    for (User u: studentUsers)
							notSubmitted.add(u.getId());
						}
					
						boolean hasOwnPage = false;
						String userId = UserDirectoryService.getCurrentUser().getId();
						
					    Collections.sort(studentPages, new Comparator<SimpleStudentPage>() {
						    public int compare(SimpleStudentPage o1, SimpleStudentPage o2) {
							String title1 = o1.getTitle();
							if (title1 == null)
							    title1 = "";
							String title2 = o2.getTitle();
							if (title2 == null)
							    title2 = "";
							return title1.compareTo(title2);
						    }
						});					    

					        UIOutput contentList = UIOutput.make(tableRow, "studentContentTable");
					        UIOutput contentTitle = UIOutput.make(tableRow, "studentContentTitle", messageLocator.getMessage("simplepage.student"));
						contentList.decorate(new UIFreeAttributeDecorator("aria-labelledby", contentTitle.getFullID()));
						boolean seeOnlyOwn = ("true".equals(i.getAttribute("see-only-own")));
						// Print each row in the table
						for(SimpleStudentPage page : studentPages) {
							if(page.isDeleted()) continue;

							// if seeOnlyOwn, skip other entries
							if (seeOnlyOwn && !canSeeAll) {
							    List<String>groupMembers = simplePageBean.studentPageGroupMembers(i, page.getGroup());
							    String currentUser = UserDirectoryService.getCurrentUser().getId();
							    if (!i.isGroupOwned() && !page.getOwner().equals(currentUser) ||
								i.isGroupOwned() && !groupMembers.contains(currentUser))
								continue;
							}

							// remove this from notSubmitted
							if (i.isGroupOwned()) {
							    String pageGroup = page.getGroup();
							    if (pageGroup != null)
								notSubmitted.remove(pageGroup);
							} else {
							    notSubmitted.remove(page.getOwner());
							}
							    
							SimplePageLogEntry entry = cache.get(page.getPageId());
							UIBranchContainer row = UIBranchContainer.make(tableRow, "studentRow:");
							
							// There's content they haven't seen
							if(entry == null || entry.getLastViewed().compareTo(page.getLastUpdated()) < 0) {
							    UIOutput.make(row, "newContentImg").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.new-student-content")));
							} else
							    UIOutput.make(row, "newContentImgT");
 
							// The comments tool exists, so we might have to show the icon
							if(i.getShowComments() != null && i.getShowComments()) {
 						
							    // New comments have been added since they last viewed the page
							    if(page.getLastCommentChange() != null && (entry == null || entry.getLastViewed().compareTo(page.getLastCommentChange()) < 0)) {
								UIOutput.make(row, "newCommentsImg").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.new-student-comments")));
							    } else
								UIOutput.make(row, "newCommentsImgT");							
							}
 
							// Never visited page
							if(entry == null) {
							    UIOutput.make(row, "newPageImg").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.new-student-content-page")));
							} else
							    UIOutput.make(row, "newPageImgT");

							GeneralViewParameters eParams = new GeneralViewParameters(ShowPageProducer.VIEW_ID, page.getPageId());
							eParams.setItemId(i.getId());
							eParams.setPath("push");

							String studentTitle = buildStudentPageTitle(i, page.getTitle(), page.getGroup(), page.getOwner(), simplePageBean.isPageOwner(page), canEditPage);

							UIInternalLink.make(row, "studentLink", studentTitle, eParams);
						
							if(simplePageBean.isPageOwner(page)) {
								hasOwnPage = true;
							}
							
							if(i.getGradebookId() != null && simplePageBean.getEditPrivs() == 0) {
								UIOutput.make(row, "studentGradingCell", String.valueOf((page.getPoints() != null? page.getPoints() : "")));
							}
						}
					
						// if grader, show people who didn't submit
						if (simplePageBean.getEditPrivs() == 0) {
						    if (notSubmitted.size() > 0) {
							UIBranchContainer row = UIBranchContainer.make(tableRow, "studentRow:");
							UIOutput.make(row, "missingStudentTitle", messageLocator.getMessage("simplepage.missing-students"));
						    }
						    List<String> missingUsers = new ArrayList<String>();
						    for(String owner: notSubmitted) {
							String sownerName;
							if (i.isGroupOwned()) {
							    try {
								sownerName = simplePageBean.getCurrentSite().getGroup(owner).getTitle();
							    } catch (Exception e) {
								// the only way I can make this happen is to add a group
								// to the item and then delete the group. If we can't find the
								// group, don't show the item.
								continue;
							    }
							} else {
							    try {
								sownerName = UserDirectoryService.getUser(owner).getDisplayName();
							    } catch (Exception e) {
								// can't find user, just show userid. Not very useful, but at least shows
								// what happened
								sownerName = owner;
							    }
							}
							missingUsers.add(sownerName);
						    }
						    Collections.sort(missingUsers);
						    for(String owner: missingUsers) {
							UIBranchContainer row = UIBranchContainer.make(tableRow, "studentRow:");
							UIOutput.make(row, "missingStudent", owner);
						    }
						    if (notSubmitted.size() > 0 && i.getGradebookId() != null) {
							UIBranchContainer row = UIBranchContainer.make(tableRow, "studentRow:");
							UIOutput zeroRow = UIOutput.make(row, "student-zero-div");
							UIForm zeroForm = UIForm.make(row, "student-zero-form");
							makeCsrf(zeroForm, "student-zero-csrf");
							UIInput.make(zeroForm, "student-zero-item", "#{simplePageBean.itemId}", String.valueOf(i.getId()));
							UICommand.make(zeroForm, "student-zero", messageLocator.getMessage("simplepage.zero-missing"), "#{simplePageBean.missingStudentSetZero}");
						    }
						}

						if(!hasOwnPage && simplePageBean.myStudentPageGroupsOk(i)) {
							UIBranchContainer row = UIBranchContainer.make(tableRow, "studentRow:");
							UIOutput.make(row, "linkRow");
							UIOutput.make(row, "linkCell");
							
							if (i.isRequired() && !simplePageBean.isItemComplete(i))
								UIOutput.make(row, "student-required-image");

							UIForm studentForm = UIForm.make(row, "add-content-form");
							makeCsrf(studentForm, "csrf27");
							UIInput.make(studentForm, "add-content-itemId", "#{simplePageBean.itemId}", "" + i.getId());
							UICommand.make(studentForm, "add-content", messageLocator.getMessage("simplepage.add-page"), "#{simplePageBean.createStudentPage}");;
						}
					
						String itemGroupString = null;
						// do before canEditAll because we need itemGroupString in it
						if (canSeeAll) {
						    itemGroupString = simplePageBean.getItemGroupString(i, null, true);
						    if (itemGroupString != null) {
							String itemGroupTitles = simplePageBean.getItemGroupTitles(itemGroupString, i);
							if (itemGroupTitles != null) {
							    itemGroupTitles = "[" + itemGroupTitles + "]";
							}
							UIOutput.make(tableRow, "item-group-titles7", itemGroupTitles);
						    }
						}

						if(canEditPage) {
							// Checks to make sure that the comments are graded and that we didn't
							// just come from a grading pane (would be confusing)
							if(i.getAltGradebook() != null && !cameFromGradingPane) {
								CommentsGradingPaneViewParameters gp = new CommentsGradingPaneViewParameters(CommentGradingPaneProducer.VIEW_ID);
								gp.placementId = toolManager.getCurrentPlacement().getId();
								gp.commentsItemId = i.getId();
								gp.pageId = currentPage.getPageId(); 
								gp.pageItemId = pageItem.getId();
								gp.studentContentItem = true;
							
								UIInternalLink.make(tableRow, "studentGradingPaneLink", gp)
								    .decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.show-grading-pane-content")));
							}
							
							UIOutput.make(tableRow, "student-td").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.student")));
							UILink.make(tableRow, "edit-student", (String)null, "")
									.decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.student")));
							
							UIOutput.make(tableRow, "studentId", String.valueOf(i.getId()));
							UIOutput.make(tableRow, "studentAnon", String.valueOf(i.isAnonymous()));
							UIOutput.make(tableRow, "studentComments", String.valueOf(i.getShowComments()));
							UIOutput.make(tableRow, "forcedAnon", String.valueOf(i.getForcedCommentsAnonymous()));
							UIOutput.make(tableRow, "studentGrade", String.valueOf(i.getGradebookId() != null));
							UIOutput.make(tableRow, "studentMaxPoints", String.valueOf(i.getGradebookPoints()));
							UIOutput.make(tableRow, "studentGradebookTitle", String.valueOf(i.getGradebookTitle()));
							UIOutput.make(tableRow, "studentGrade2", String.valueOf(i.getAltGradebook() != null));
							UIOutput.make(tableRow, "studentMaxPoints2", String.valueOf(i.getAltPoints()));
							UIOutput.make(tableRow, "studentitem-required", String.valueOf(i.isRequired()));
							UIOutput.make(tableRow, "studentitem-prerequisite", String.valueOf(i.isPrerequisite()));
							UIOutput.make(tableRow, "peer-eval", String.valueOf(i.getShowPeerEval()));
							makePeerRubric(tableRow,i, makeMaintainRubric, null, null, false);
							makeSamplePeerEval(tableRow);
							
							String peerEvalDate = i.getAttribute("rubricOpenDate");
							String peerDueDate = i.getAttribute("rubricDueDate");
							
							Calendar peerevalcal = Calendar.getInstance();
							
							if (peerEvalDate != null && peerDueDate != null) {
								DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, M_locale);
								//Open date from attribute string
								peerevalcal.setTimeInMillis(Long.valueOf(peerEvalDate));
								
								String dateStr = isoDateFormat.format(peerevalcal.getTime());
								
								UIOutput.make(tableRow, "peer-eval-open-date", dateStr);
								
								//Due date from attribute string
								peerevalcal.setTimeInMillis(Long.valueOf(peerDueDate));
								
								dateStr = isoDateFormat.format(peerevalcal.getTime());
								
								UIOutput.make(tableRow, "peer-eval-due-date", dateStr);
								UIOutput.make(tableRow, "peer-eval-allow-self", i.getAttribute("rubricAllowSelfGrade"));
								
							}else{
								//Default open and due date
								Date now = new Date();
								peerevalcal.setTime(now);

								//Default open date: now
								String dateStr = isoDateFormat.format(peerevalcal.getTime());
								
								UIOutput.make(tableRow, "peer-eval-open-date", dateStr);
								
								//Default due date: 7 days from now
								Date later = new Date(peerevalcal.getTimeInMillis() + 604800000);
								peerevalcal.setTime(later);
								
								dateStr = isoDateFormat.format(peerevalcal.getTime());
								
								//log.info("Setting date to " + dateStr + " and time to " + timeStr);
								
								UIOutput.make(tableRow, "peer-eval-due-date", dateStr);
								UIOutput.make(tableRow, "peer-eval-allow-self", i.getAttribute("rubricAllowSelfGrade"));
							}
							
							//Peer Eval Stats link
							GeneralViewParameters view = new GeneralViewParameters(PeerEvalStatsProducer.VIEW_ID);
							view.setSendingPage(currentPage.getPageId());
							view.setItemId(i.getId());
							if(i.getShowPeerEval()){
								UILink link = UIInternalLink.make(tableRow, "peer-eval-stats-link", view);
							}
							
							if (itemGroupString != null) {
								UIOutput.make(tableRow, "student-groups", itemGroupString);
							}
							UIOutput.make(tableRow, "student-owner-groups", simplePageBean.getItemOwnerGroupString(i));
							UIOutput.make(tableRow, "student-group-owned", (i.isGroupOwned()?"true":"false"));
							UIOutput.make(tableRow, "student-group-owned-eval-individual", (i.getAttribute("group-eval-individual")));
							UIOutput.make(tableRow, "student-group-owned-see-only-own", (i.getAttribute("see-only-own")));
						}
					}
				}else if(i.getType() == SimplePageItem.ANNOUNCEMENTS){
					UIOutput.make(tableRow, "announcementsSpan");
					String itemGroupString = null;
					String itemGroupTitles = null;
					if (canSeeAll) {
						itemGroupString = simplePageBean.getItemGroupString(i, null, true);
						if (itemGroupString != null)
							itemGroupTitles = simplePageBean.getItemGroupTitles(itemGroupString, i);
						if (itemGroupTitles != null) {
							itemGroupTitles = "[" + itemGroupTitles + "]";
						}
						if (canEditPage)
							UIOutput.make(tableRow, "item-groups", itemGroupString);
						if (itemGroupTitles != null)
							UIOutput.make(tableRow, "announcements-groups-titles", itemGroupTitles);
					}
					boolean available = simplePageBean.isItemAvailable(i);
					// note that there's currently no UI for setting availability check
					if(canSeeAll || available) {
						if (!available)
						    UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.fake-item-unavailable"));
						//create html for announcements widget
						String html = "<div align=\"left\" class=\"announcements-div\"></div>";
						UIVerbatim.make(tableRow, "content", html);
						UIOutput.make(tableRow, "announcements-id", String.valueOf(i.getId()));
						//setting announcements url to get all announcements for the site
						UIOutput.make(tableRow, "announcements-site-url", myUrl() + "/direct/announcement/site/" + simplePageBean.getCurrentSiteId());
						//setting this variable to redirect user to the particular announcement
						UIOutput.make(tableRow, "announcements-view-url", myUrl() + "/portal/directtool/" + simplePageBean.getCurrentTool(simplePageBean.ANNOUNCEMENTS_TOOL_ID) + "?itemReference=/announcement/msg/" + simplePageBean.getCurrentSiteId() + "/main/");
						//get numberOfAnnouncements for the widget
						String numberOfAnnouncements = i.getAttribute("numberOfAnnouncements") != null ? i.getAttribute("numberOfAnnouncements") : "";
						UIOutput.make(tableRow, "numberOfAnnouncements", numberOfAnnouncements);
					}else{
						UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.textItemUnavailable"));
					}
					if (canEditPage) {
						UIOutput.make(tableRow, "announcements-td");
						UILink.make(tableRow, "edit-announcements", (String) null, "");
					}
				} else if(i.getType() == SimplePageItem.FORUM_SUMMARY){
					UIOutput.make(tableRow, "forumSummarySpan");
					String itemGroupString = null;
					String itemGroupTitles = null;
					if (canSeeAll) {
						itemGroupString = simplePageBean.getItemGroupString(i, null, true);
						if (itemGroupString != null)
							itemGroupTitles = simplePageBean.getItemGroupTitles(itemGroupString, i);
						if (itemGroupTitles != null) {
							itemGroupTitles = "[" + itemGroupTitles + "]";
						}
						if (canEditPage)
							UIOutput.make(tableRow, "item-groups", itemGroupString);
						if (itemGroupTitles != null)
							UIOutput.make(tableRow, "forum-summary-groups-titles", itemGroupTitles);
					}
					boolean available = simplePageBean.isItemAvailable(i);
					// note that there's currently no UI for setting availability check
					if(canSeeAll || available) {
						if (!available)
						    UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.fake-item-unavailable"));
						//create html for forum-summary widget
						String html = "<div align=\"left\" class=\"forum-summary-div\"></div>";
						UIVerbatim.make(tableRow, "content", html);
						UIOutput.make(tableRow, "forum-summary-id", String.valueOf(i.getId()));
						//setting forums-messages url to get all recent messages for the site
						UIOutput.make(tableRow, "forum-summary-site-url", myUrl() + "/direct/forums/messages/" + simplePageBean.getCurrentSiteId());
						//setting the url such that request goes to ShowItemProducer to display forum conversations inside Lessons tool
						UIOutput.make(tableRow, "forum-summary-view-url", myUrl() + "/portal/site/" + simplePageBean.getCurrentSiteId() + "/tool/" + placement.getId()
								+"/" + ShowItemProducer.VIEW_ID + "?itemId=" +i.getId()+"&sendingPage="+currentPage.getPageId());
						//get numberOfConversations for the widget
						String numberOfConversations = i.getAttribute("numberOfConversations") != null ? i.getAttribute("numberOfConversations") : "" ;
						UIOutput.make(tableRow, "numberOfConversations", numberOfConversations);
					}else {
						UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.textItemUnavailable"));
					}
					if (canEditPage) {
						UIOutput.make(tableRow, "forum-summary-td");
						UILink.make(tableRow, "edit-forum-summary", (String) null, "");
					}
				}else if(i.getType() == SimplePageItem.RESOURCE_FOLDER){
					UIOutput.make(tableRow, "resourceFolderSpan");
					if (canSeeAll) {
						String itemGroupString = simplePageBean.getItemGroupString(i, null, true);
						String itemGroupTitles = simplePageBean.getItemGroupTitles(itemGroupString, i);
						if (itemGroupTitles != null) {
							itemGroupTitles = "[" + itemGroupTitles + "]";
						}

						UIOutput.make(tableRow, "resourceFolder-groups-titles", itemGroupTitles);
					}

					if(canSeeAll || simplePageBean.isItemAvailable(i)) {
						//get directory path from item's attribute
						String dataDirectory = i.getAttribute("dataDirectory") != null ? i.getAttribute("dataDirectory") : "";
						String collectionId = dataDirectory.replace("//", "/");
						String[] folderPath = collectionId.split("/");
						String folderName = folderPath[folderPath.length -1];
						try {
							// collection name should always be preferred
							ContentCollection collection = contentHostingService.getCollection(collectionId);
							folderName = collection.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME);
						} catch (PermissionException | IdUnusedException | TypeException e) {
							log.debug("Could not discern folder name from collection {}", collectionId, e);
						}
						if (StringUtils.isBlank(folderName)) {
							// if by chance it is still empty use the sites title
							folderName = simplePageBean.getCurrentSite().getTitle();
						}
						String html = "<p><b>" + folderName + "</b></p><div data-copyright=\"true\" class=\"no-highlight\" data-description=\"true\" data-directory='" +dataDirectory+ "' data-files=\"true\" data-folder-listing=\"true\"></div>";
						UIVerbatim.make(tableRow, "content", html);
					} else {
						UIComponent unavailableText = UIOutput.make(tableRow, "content", messageLocator.getMessage("simplepage.textItemUnavailable"));
						unavailableText.decorate(new UIFreeAttributeDecorator("class", "disabled-text-item"));
					}
					if (canEditPage) {
						UIOutput.make(tableRow, "resourceFolder-td");
						GeneralViewParameters eParams = new GeneralViewParameters();
						eParams.setSendingPage(currentPage.getPageId());
						eParams.setItemId(i.getId());
						eParams.viewID = FolderPickerProducer.VIEW_ID;
						UIInternalLink.make(tableRow, "edit-resourceFolder", (String)null, eParams);
					}
				}else if(i.getType() == SimplePageItem.CALENDAR){
					UIOutput.make(tableRow, "calendarSpan");
					String itemGroupString = null;
					String itemGroupTitles = null;
					if (canSeeAll) {
						itemGroupString = simplePageBean.getItemGroupString(i, null, true);
						if (itemGroupString != null)
							itemGroupTitles = simplePageBean.getItemGroupTitles(itemGroupString, i);
						if (itemGroupTitles != null) {
							itemGroupTitles = "[" + itemGroupTitles + "]";
						}
						if (canEditPage) {
							UIOutput.make(tableRow, "calendar-item-groups", itemGroupString);
							String name = i.getName()!= null ? i.getName() : "" ;
							UIOutput.make(tableRow, "calendar-name", name);
							String description = i.getDescription()!= null ? i.getDescription() : "" ;
							UIOutput.make(tableRow, "calendar-description", description);
							String indentLevel = i.getAttribute("indentLevel") != null ? i.getAttribute("indentLevel") : "" ;
							UIOutput.make(tableRow, "calendar-indentLevel", indentLevel);
							String customCssClass = i.getAttribute("customCssClass") != null ? i.getAttribute("customCssClass") : "" ;
							UIOutput.make(tableRow, "calendar-custom-css-class", customCssClass);
						}
						if (itemGroupTitles != null)
							UIOutput.make(tableRow, "calendar-groups-titles", itemGroupTitles);
					}
					if(canSeeAll || simplePageBean.isItemAvailable(i)) {
						//Create html for calendar widget
						String html = "<div class=\"calendar-div\"></div>";
						UIVerbatim.make(tableRow, "content", html);
						//set url to get events for the calendar
						UIOutput.make(tableRow, "site-events-url", myUrl() + "/direct/calendar/site/" + simplePageBean.getCurrentSiteId());
						//set calendar tool url for more info of calendar event
						UIOutput.make(tableRow, "event-tool-url", myUrl() + "/portal/directtool/" + simplePageBean.getCurrentTool(simplePageBean.CALENDAR_TOOL_ID) + "?eventReference=");
					}else {
						UIComponent unavailableText = UIOutput.make(tableRow, "content", messageLocator.getMessage("simplepage.textItemUnavailable"));
						unavailableText.decorate(new UIFreeAttributeDecorator("class", "disabled-text-item"));
					}
					if (canEditPage) {
						UIOutput.make(tableRow, "calendar-td");
						UIOutput.make(tableRow, "calendar-item-id", String.valueOf(i.getId()));
						UILink.make(tableRow, "edit-calendar", (String) null, "");
					}	
				}else if(i.getType() == SimplePageItem.QUESTION) {
				 	String itemGroupString = null;
					String itemGroupTitles = null;
					if (canSeeAll) {
					    itemGroupString = simplePageBean.getItemGroupString(i, null, true);
					    if (itemGroupString != null)
						itemGroupTitles = simplePageBean.getItemGroupTitles(itemGroupString, i);
					    if (itemGroupTitles != null) {
						itemGroupTitles = "[" + itemGroupTitles + "]";
					    }
					    if (canEditPage)
						UIOutput.make(tableRow, "item-groups", itemGroupString);
					    if (itemGroupTitles != null)
						UIOutput.make(tableRow, "questionitem-group-titles", itemGroupTitles);
					}
					SimplePageQuestionResponse response = simplePageToolDao.findQuestionResponse(i.getId(), simplePageBean.getCurrentUserId());
					
					UIOutput.make(tableRow, "questionSpan");

					boolean isAvailable = simplePageBean.isItemAvailable(i) || canSeeAll;
					
					UIOutput.make(tableRow, "questionDiv");
					
					UIVerbatim.make(tableRow, "questionText", i.getAttribute("questionText"));
					
					List<SimplePageQuestionAnswer> answers = new ArrayList<SimplePageQuestionAnswer>();
					if("multipleChoice".equals(i.getAttribute("questionType"))) {
						answers = simplePageToolDao.findAnswerChoices(i);
						UIOutput.make(tableRow, "multipleChoiceDiv");
						UIForm questionForm = UIForm.make(tableRow, "multipleChoiceForm");
						makeCsrf(questionForm, "csrf4");

						UIInput.make(questionForm, "multipleChoiceId", "#{simplePageBean.questionId}", String.valueOf(i.getId()));
						UIInput.make(questionForm, "raw-question-text", "#{simplePageBean.questionText}", i.getAttribute("questionText"));
						
						String[] options = new String[answers.size()];
						String initValue = null;
						for(int j = 0; j < answers.size(); j++) {
							options[j] = String.valueOf(answers.get(j).getId());
							if(response != null && answers.get(j).getId() == response.getMultipleChoiceId()) {
								initValue = String.valueOf(answers.get(j).getId());
							}
						}
						
						UISelect multipleChoiceSelect = UISelect.make(questionForm, "multipleChoiceSelect:", options, "#{simplePageBean.questionResponse}", initValue);

						// allow instructor to answer again, for testing
						if(!isAvailable || response != null) {
						    if (canSeeAll)
							fakeDisableLink(multipleChoiceSelect, messageLocator);
						    else
							multipleChoiceSelect.decorate(new UIDisabledDecorator());
						}
						 
						for(int j = 0; j < answers.size(); j++) {
							UIBranchContainer answerContainer = UIBranchContainer.make(questionForm, "multipleChoiceAnswer:", String.valueOf(j));
							UISelectChoice multipleChoiceInput = UISelectChoice.make(answerContainer, "multipleChoiceAnswerRadio", multipleChoiceSelect.getFullID(), j);
							multipleChoiceInput.decorate(new UIFreeAttributeDecorator("id", multipleChoiceInput.getFullID()));
							char answerOption = (char) (j + 65); // 65 Corresponds to A
							UIOutput.make(answerContainer, "multipleChoiceAnswerText", answerOption + " : " + answers.get(j).getText())
								.decorate(new UIFreeAttributeDecorator("for", multipleChoiceInput.getFullID()));
							
							if(!isAvailable || response != null) {
							    if (canSeeAll)
								fakeDisableLink(multipleChoiceInput, messageLocator);
							    else
								multipleChoiceInput.decorate(new UIDisabledDecorator());
							}
						}
						 
						UICommand answerButton = UICommand.make(questionForm, "answerMultipleChoice", messageLocator.getMessage("simplepage.answer_question"), "#{simplePageBean.answerMultipleChoiceQuestion}");
						if(!isAvailable || response != null) {
						    if (canSeeAll)
							fakeDisableLink(answerButton, messageLocator);
						    else
							answerButton.decorate(new UIDisabledDecorator());
						}
					}else if("shortanswer".equals(i.getAttribute("questionType"))) {
						UIOutput.make(tableRow, "shortanswerDiv");
						
						UIForm questionForm = UIForm.make(tableRow, "shortanswerForm");
						makeCsrf(questionForm, "csrf5");

						UIInput.make(questionForm, "shortanswerId", "#{simplePageBean.questionId}", String.valueOf(i.getId()));
						UIInput.make(questionForm, "raw-question-text", "#{simplePageBean.questionText}", i.getAttribute("questionText"));
						UIInput shortanswerInput = UIInput.make(questionForm, "shortanswerInput", "#{simplePageBean.questionResponse}");

						if(!isAvailable || response != null) {
							if (canSeeAll)
							    fakeDisableLink(shortanswerInput, messageLocator);
							else
							    shortanswerInput.decorate(new UIDisabledDecorator());
							if(response != null && response.getShortanswer() != null) {
								shortanswerInput.setValue(response.getShortanswer());
							}
						}
						
						UICommand answerButton = UICommand.make(questionForm, "answerShortanswer", messageLocator.getMessage("simplepage.answer_question"), "#{simplePageBean.answerShortanswerQuestion}");
						if(!isAvailable || response != null) {
						    if (canSeeAll)
							fakeDisableLink(answerButton, messageLocator);
						    else
							answerButton.decorate(new UIDisabledDecorator());
						}
					}
					
					Status questionStatus = getQuestionStatus(i, response);
					addStatusIcon(questionStatus, tableRow, "questionStatus");
					String statusNote = getStatusNote(questionStatus);
					if (statusNote != null) // accessibility version of icon
					    UIOutput.make(tableRow, "questionNote", statusNote);
					String statusText = null;
					if(questionStatus == Status.COMPLETED)
					    statusText = i.getAttribute("questionCorrectText");
					else if(questionStatus == Status.FAILED)
					    statusText = i.getAttribute("questionIncorrectText");
					if (statusText != null && !"".equals(statusText.trim()))
					    UIOutput.make(tableRow, "questionStatusText", statusText);
					
					// Output the poll data
					if("multipleChoice".equals(i.getAttribute("questionType")) &&
							(canEditPage || ("true".equals(i.getAttribute("questionShowPoll")) &&
									(questionStatus == Status.COMPLETED || questionStatus == Status.FAILED || questionStatus == Status.NEEDSGRADING)))) {
						UIOutput.make(tableRow, "showPollGraph", messageLocator.getMessage("simplepage.show-poll"));
						UIOutput questionGraph = UIOutput.make(tableRow, "questionPollGraph");
						questionGraph.decorate(new UIFreeAttributeDecorator("id", "poll" + i.getId()));
						
						List<SimplePageQuestionResponseTotals> totals = simplePageToolDao.findQRTotals(i.getId());
						HashMap<Long, Long> responseCounts = new HashMap<Long, Long>();
						// in theory we don't need the first loop, as there should be a total
						// entry for all possible answers. But in case things are out of sync ...
						for(SimplePageQuestionAnswer answer : answers)
						    responseCounts.put(answer.getId(), 0L);
						for(SimplePageQuestionResponseTotals total : totals)
						    responseCounts.put(total.getResponseId(), total.getCount());
						
						for(int j = 0; j < answers.size(); j++) {
							char letter = (char) ('A' + j); // Convert number to corresponding letter
							UIBranchContainer pollContainer = UIBranchContainer.make(tableRow, "questionPollData:", String.valueOf(j));
							UIOutput.make(pollContainer, "questionPollText", String.valueOf(letter));
							UIOutput.make(pollContainer, "questionPollLegend", letter + ":" + answers.get(j).getText());
							UIOutput.make(pollContainer, "questionPollNumber", String.valueOf(responseCounts.get(answers.get(j).getId())));
						}
					}
					
					
					if(canEditPage) {
						UIOutput.make(tableRow, "question-td").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.question")));
						
						// always show grading panel. Currently this is the only way to get feedback
						if( !cameFromGradingPane) {
							QuestionGradingPaneViewParameters gp = new QuestionGradingPaneViewParameters(QuestionGradingPaneProducer.VIEW_ID);
							gp.placementId = toolManager.getCurrentPlacement().getId();
							gp.questionItemId = i.getId();
							gp.pageId = currentPage.getPageId();
							gp.pageItemId = pageItem.getId();
						
							UIInternalLink.make(tableRow, "questionGradingPaneLink", gp)
							    .decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.show-grading-pane")));
						}
						
						UILink.make(tableRow, "edit-question", (String)null, "")
							.decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.question")));
						
						UIOutput.make(tableRow, "questionId", String.valueOf(i.getId()));
						boolean graded = "true".equals(i.getAttribute("questionGraded")) || i.getGradebookId() != null;
						UIOutput.make(tableRow, "questionGrade", String.valueOf(graded));
						UIOutput.make(tableRow, "questionMaxPoints", String.valueOf(i.getGradebookPoints()));
						UIOutput.make(tableRow, "questionGradebookTitle", String.valueOf(i.getGradebookTitle()));
						UIOutput.make(tableRow, "questionitem-required", String.valueOf(i.isRequired()));
						UIOutput.make(tableRow, "questionitem-prerequisite", String.valueOf(i.isPrerequisite()));
						UIOutput.make(tableRow, "questionitem-groups", itemGroupString);
						UIOutput.make(tableRow, "questionCorrectText", String.valueOf(i.getAttribute("questionCorrectText")));
						UIOutput.make(tableRow, "questionIncorrectText", String.valueOf(i.getAttribute("questionIncorrectText")));
						
						if("shortanswer".equals(i.getAttribute("questionType"))) {
							UIOutput.make(tableRow, "questionType", "shortanswer");
							UIOutput.make(tableRow, "questionAnswer", i.getAttribute("questionAnswer"));
						}else {
							UIOutput.make(tableRow, "questionType", "multipleChoice");
							
							for(int j = 0; j < answers.size(); j++) {
								UIBranchContainer answerContainer = UIBranchContainer.make(tableRow, "questionMultipleChoiceAnswer:", String.valueOf(j));
								UIOutput.make(answerContainer, "questionMultipleChoiceAnswerId", String.valueOf(answers.get(j).getId()));
								UIOutput.make(answerContainer, "questionMultipleChoiceAnswerText", answers.get(j).getText());
								UIOutput.make(answerContainer, "questionMultipleChoiceAnswerCorrect", String.valueOf(answers.get(j).isCorrect()));
								//SAK-46296
								UIInput.make(answerContainer, "raw-questionAnswer-text",  null, answers.get(j).getText());
							}
							
							UIOutput.make(tableRow, "questionShowPoll", String.valueOf(i.getAttribute("questionShowPoll")));
						}
					}
				} else if (i.getType() == SimplePageItem.CHECKLIST) {
					UIOutput checklistItemContainer = UIOutput.make(tableRow, "checklistItemContainer");

					UIOutput checklistDiv = UIOutput.make(tableRow, "checklistDiv");

					styleItem(tableRow, checklistItemContainer, checklistDiv, i, null, null);

					UIOutput checklistTitle = UIOutput.make(tableRow, "checklistTitle", i.getName());

					if(Boolean.valueOf(i.getAttribute(SimplePageItem.NAMEHIDDEN))) {
						if(canEditPage) {
							checklistTitle.setValue("( " + i.getName() + " )");
							checklistTitle.decorate(new UIStyleDecorator("hiddenTitle"));
						} else {
							checklistTitle.decorate(new UIStyleDecorator("noDisplay"));
						}
					}

					UIOutput.make(tableRow, "checklistDescription", i.getDescription());
					UIOutput.make(tableRow, "error-checklist-not-saved", messageLocator.getMessage("simplepage.checklist.error.not-saved"));

					List<SimpleChecklistItem> checklistItems = simplePageToolDao.findChecklistItems(i);

					UIOutput.make(tableRow, "checklistItemDiv");
					UIForm checklistForm = UIForm.make(tableRow, "checklistItemForm");

					UIInput.make(checklistForm, "checklistId", "#{simplePageBean.itemId}", String.valueOf(i.getId()));
					ArrayList<String> values = new ArrayList<String>();
					ArrayList<String> initValues = new ArrayList<String>();

					for (SimpleChecklistItem checklistItem : checklistItems) {
						values.add(String.valueOf(checklistItem.getId()));
						if(checklistItem.getLink() > 0L) {
							final SimplePageItem linkedItem = simplePageBean.findItem(checklistItem.getLink());
							if(linkedItem != null) {
								boolean available = simplePageBean.isItemAvailable(linkedItem);
								Status linkedItemStatus = Status.NOT_REQUIRED;
								if (available) {
									UIBranchContainer empty = UIBranchContainer.make(tableRow, "non-existent:");
									linkedItemStatus = handleStatusIcon(empty, linkedItem);
								}

								ChecklistItemStatus status = simplePageToolDao.findChecklistItemStatus(i.getId(), checklistItem.getId(), simplePageBean.getCurrentUserId());
								if (Status.COMPLETED.equals(linkedItemStatus)) {
									if (status != null) {
										if (!status.isDone()) {
											status.setDone(true);
											simplePageToolDao.saveChecklistItemStatus(status);
										}
									} else {
										status = new ChecklistItemStatusImpl(i.getId(), checklistItem.getId(), simplePageBean.getCurrentUserId());
										status.setDone(true);
										simplePageToolDao.saveChecklistItemStatus(status);
									}
								} else {
									if (status != null) {
										if (status.isDone()) {
											status.setDone(false);
											simplePageToolDao.saveChecklistItemStatus(status);
										}
									}
								}
							}
						}

						boolean isDone = simplePageToolDao.isChecklistItemChecked(i.getId(), checklistItem.getId(), simplePageBean.getCurrentUserId());

						if (isDone) {
							initValues.add(String.valueOf(checklistItem.getId()));
						} else {
							initValues.add("");
						}
					}

					UIOutput.make(checklistForm, "checklistItemsDiv");
					if(!checklistItems.isEmpty()) {
						UISelect select = UISelect.makeMultiple(checklistForm, "checklist-span", values.toArray(new String[1]), "#{simplePageBean.selectedChecklistItems}", initValues.toArray(new String[1]));
						int index = 0;
						for (SimpleChecklistItem checklistItem : checklistItems) {
							UIBranchContainer row = UIBranchContainer.make(checklistForm, "select-checklistitem-list:");
							UIComponent input = UISelectChoice.make(row, "select-checklistitem", select.getFullID(), index).decorate(new UIStyleDecorator("checklist-checkbox"));
							String checklistItemName = checklistItem.getName();
							if (checklistItem.getLink() > 0L) {
								//item with link
								row.decorate(new UIStyleDecorator("is-linked"));
								SimplePageItem linkedItem = simplePageBean.findItem(checklistItem.getLink());
								if(linkedItem != null) {
									String toolTipMessage = "simplepage.checklist.external.link.details.incomplete";
									if (simplePageBean.isItemComplete(linkedItem)) {
										toolTipMessage = "simplepage.checklist.external.link.details.complete";
									}
									String tooltipContent = messageLocator.getMessage(toolTipMessage).replace("{}", SimplePageItemUtilities.getDisplayName(linkedItem));

									if (!simplePageBean.isItemVisible(linkedItem)) {
										tooltipContent = messageLocator.getMessage("simplepage.checklist.external.link.details.notvisible");
										checklistItemName = messageLocator.getMessage("simplepage.checklist.external.link.hidden");
									}

									UIOutput.make(row, "select-checklistitem-linked-details", tooltipContent);
								}

								UIOutput.make(row, "select-checklistitem-name", checklistItemName);
								UIOutput.make(row, "linked-checklistitem-linked-icon");

							} else if (checklistItem.getLink() < -1L) {	// getLink will give out -2 for items that were once linked but broke during site duplication.
								//item with broken link
								row.decorate(new UIStyleDecorator("is-linked"));
								UIOutput.make(row, "select-checklistitem-name", checklistItemName);
								UIOutput.make(row, "linked-checklistitem-broken-link-icon");
								String toolTipContent = messageLocator.getMessage("simplepage.checklist.external.link.details.broken");
								UIOutput.make(row, "select-checklistitem-linked-details", toolTipContent);
							} else {
								//item without link
								UIOutput.make(row, "select-checklistitem-name", checklistItemName);
							}
							index++;
						}
					}

					if (canEditPage) {

						String itemGroupString = simplePageBean.getItemGroupString(i, null, true);
						String itemGroupTitles = simplePageBean.getItemGroupTitles(itemGroupString, i);
						if (itemGroupTitles != null) {
							itemGroupTitles = "[" + itemGroupTitles + "]";
						}

						UIOutput.make(tableRow, "item-groups-titles-checklist", itemGroupTitles);

						UIOutput.make(tableRow, "editchecklist-td").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-checklist").replace("{}", i.getName())));

						GeneralViewParameters eParams = new GeneralViewParameters();
						eParams.setSendingPage(currentPage.getPageId());
						eParams.setItemId(i.getId());
						eParams.viewID = ChecklistProducer.VIEW_ID;
						UIInternalLink.make(tableRow, "edit-checklist", (String)null, eParams).decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-checklist").replace("{}", i.getName())));

						GeneralViewParameters gvp = new GeneralViewParameters();
						gvp.setSendingPage(currentPage.getPageId());
						gvp.setItemId(i.getId());
						gvp.viewID = ChecklistProgressProducer.VIEW_ID;
						UIInternalLink.make(tableRow, "edit-checklistProgress", (String)null, gvp).decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.view-checklist").replace("{}", i.getName())));
					}

					makeSaveChecklistForm(tofill);
				}else if(i.getType() == SimplePageItem.TWITTER) {
					UIOutput.make(tableRow, "twitterSpan");
					String itemGroupString = null;
					String itemGroupTitles = null;
					if (canSeeAll) {
						itemGroupString = simplePageBean.getItemGroupString(i, null, true);
						if (itemGroupString != null)
							itemGroupTitles = simplePageBean.getItemGroupTitles(itemGroupString, i);
						if (itemGroupTitles != null) {
							itemGroupTitles = "[" + itemGroupTitles + "]";
						}
						if (canEditPage)
							UIOutput.make(tableRow, "item-groups", itemGroupString);
						if (itemGroupTitles != null)
							UIOutput.make(tableRow, "twitter-groups-titles", itemGroupTitles);
					}
					if(canSeeAll || simplePageBean.isItemAvailable(i)) {
						//Getting variables from attributes of this  twitter page item
						String tweetLimit = i.getAttribute("numberOfTweets") != null ? i.getAttribute("numberOfTweets") : "5";
						String height = i.getAttribute("height") != null ? i.getAttribute("height") : "" ;
						String username = i.getAttribute("username") != null ? i.getAttribute("username") : "";

						String href  = "https://twitter.com/" + StringUtils.trim(username);
						String divHeight = "height:" + height + "px;";
						//Note: widget id used is from uni's twitter account
						String html = "<div align=\"left\" style='"+divHeight+"' class=\"twitter-div\"><a class=\"twitter-timeline\" href= '" +href+ "' data-widget-id='" +ServerConfigurationService.getString(TWITTER_WIDGET_ID)+ "'  data-tweet-limit='" +tweetLimit +"' data-dnt=\"true\" data-screen-name='" +username+"'>Tweets by @'" +username+"'</a></div>";
						UIVerbatim.make(tableRow, "content", html);
						UIOutput.make(tableRow, "username", username);
						UIOutput.make(tableRow, "tweetLimit", tweetLimit);
						UIOutput.make(tableRow, "twitter-height", height);
						UIOutput.make(tableRow, "twitterId", String.valueOf(i.getId()));
						// Include Twitter javascript library
						includeTwitterLibrary = true;
					} else {
						UIComponent unavailableText = UIOutput.make(tableRow, "content", messageLocator.getMessage("simplepage.textItemUnavailable"));
						unavailableText.decorate(new UIFreeAttributeDecorator("class", "disabled-text-item"));
					}
					if (canEditPage) {
						UIOutput.make(tableRow, "twitter-td");
						UILink.make(tableRow, "edit-twitter", (String) null, "");
					}
				} else {
					// remaining type must be a block of HTML
					UIOutput.make(tableRow, "itemSpan");

					if (canSeeAll) {
					    String itemGroupString = simplePageBean.getItemGroupString(i, null, true);
					    String itemGroupTitles = simplePageBean.getItemGroupTitles(itemGroupString, i);
					    String subPagePath = null;
					    if(!subPageTitleIncluded && httpServletRequest.getParameter("printall") != null)
					    {
					    	subPagePath = simplePageBean.getSubPagePath(i, false);
					    	subPageTitleIncluded = true;					    	
					    }
					    if(subPageTitleContinue && httpServletRequest.getParameter("printall") != null)
					    {
					    	subPagePath = simplePageBean.getSubPagePath(i, true);
					    	subPageTitleContinue = false;					    	
					    }
					    if (itemGroupTitles != null && subPagePath != null) {
					    	itemGroupTitles = subPagePath +" - "+itemGroupTitles;
					    }
					    else if(subPagePath != null)
					    {
					    	itemGroupTitles = subPagePath;
					    }
					    
					    if (itemGroupTitles != null) {
					    	itemGroupTitles = "[" + itemGroupTitles + "]";
					    }
					    
					    UIOutput.make(tableRow, "item-groups-titles-text", itemGroupTitles);
					}

					boolean isAvailable = simplePageBean.isItemAvailable(i);
					if(canSeeAll || isAvailable) {
					    if (!isAvailable)
						UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.textItemUnavailable"));
					    String itemText;
					    if (ServerConfigurationService.getBoolean("lessonbuilder.personalize.text", false)) {
						itemText = personalizeText(i.getHtml());
					    } else {
						itemText = i.getHtml();
					    }
					    UIVerbatim.make(tableRow, "content", (itemText == null ? "" : itemText));
					} else {
					    UIOutput.make(tableRow, "notAvailableText", messageLocator.getMessage("simplepage.textItemUnavailable"));
					}

					// editing is done using a special producer that calls FCK.
					if (canEditPage) {
						GeneralViewParameters eParams = new GeneralViewParameters();
						eParams.setSendingPage(currentPage.getPageId());
						eParams.setItemId(i.getId());
						eParams.viewID = EditPageProducer.VIEW_ID;
						UIOutput.make(tableRow, "edittext-td").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.textbox").replace("{}", Integer.toString(textboxcount))));
						UIInternalLink.make(tableRow, "edit-link", (String)null, eParams).decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit-title.textbox").replace("{}", Integer.toString(textboxcount))));
						textboxcount++;
					}
				}
				} // else - is not a subpage

			}
			if (includeTwitterLibrary) {
				UIOutput.make(tofill, "twitter-library");
			}
	}
			
	
	public void makeCsrf(UIContainer tofill, String rsfid) {
	    Object sessionToken = SessionManager.getCurrentSession().getAttribute("sakai.csrf.token");
	    if (sessionToken != null)
		UIInput.make(tofill, rsfid, "simplePageBean.csrfToken", sessionToken.toString());
	}

	public void createDialogs(UIContainer tofill, SimplePage currentPage, SimplePageItem pageItem, ContentResource cssLink) {
		createEditItemDialog(tofill, currentPage, pageItem);
		createAddMultimediaDialog(tofill, currentPage);
		createEditMultimediaDialog(tofill, currentPage);
		createEditTitleDialog(tofill, currentPage, pageItem, cssLink);
		createNewPageDialog(tofill, currentPage, pageItem);
		createRemovePageDialog(tofill, currentPage, pageItem);
		createImportCcDialog(tofill);
		createExportCcDialog(tofill);
		createYoutubeDialog(tofill, currentPage);
		createMovieDialog(tofill, currentPage);
		createCommentsDialog(tofill);
		createStudentContentDialog(tofill, currentPage);
		createQuestionDialog(tofill, currentPage);
		createTwitterDialog(tofill, currentPage);
		createForumSummaryDialog(tofill, currentPage);
		createLayoutDialog(tofill, currentPage);
		createDeleteItemDialog(tofill, currentPage);
		createAnnouncementsDialog(tofill, currentPage);
		createColumnDialog(tofill, currentPage);
	}

    // get encrypted version of session id. This is our equivalent of session.id, except that we
    // encrypt it so it can't be used for anything else in case someone captures it.
    // we can't use time to avoid replay, as some time can go by between display of the page and
    // when the user clicks. The only thing this can be used for is reading multimedia files. I
    // think we're willing to risk that. I used to use session.id, but by default that's now off, 
    // and turning it on to use it here would expose us to more serious risks.  Cache the encryption.
    // we could include the whole URL in the encryption if it was worth the additional over head.
    // I think it's not.

    // url is /access/lessonbuilder/item/NNN/url. Because the server side
    // sees a reference starting with /item, we send that.
        public String getSessionParameter(String url) {
	    UsageSession session = UsageSessionService.getSession();
	    if (!url.startsWith("/access/lessonbuilder"))
		return null;
	    url = url.substring("/access/lessonbuilder".length());

	    try {
		Cipher sessionCipher = Cipher.getInstance("Blowfish");
		sessionCipher.init(Cipher.ENCRYPT_MODE, lessonBuilderAccessService.getSessionKey());
		String sessionParam = session.getId() + ":" + url;
		byte[] sessionBytes = sessionParam.getBytes("UTF8");
		sessionBytes = sessionCipher.doFinal(sessionBytes);
		sessionParam = DatatypeConverter.printHexBinary(sessionBytes);
		return sessionParam;
	    } catch (Exception e) {
		log.info("unable to generate encrypted session id " + e);
		return null;
	    }
	}

	public void setSimplePageBean(SimplePageBean simplePageBean) {
		this.simplePageBean = simplePageBean;
	}

	public void setHttpServletRequest(HttpServletRequest httpServletRequest) {
		this.httpServletRequest = httpServletRequest;
	}

	public void setHttpServletResponse(HttpServletResponse httpServletResponse) {
		this.httpServletResponse = httpServletResponse;
	}

	private boolean makeLink(UIContainer container, String ID, SimplePageItem i, boolean canEditPage, SimplePage currentPage, boolean notDone, Status status){
		return makeLink(container, ID, i, canEditPage, currentPage, notDone, status, false, null);
	}
	private boolean makeLink(UIContainer container, String ID, SimplePageItem i, boolean canEditPage, SimplePage currentPage, boolean notDone, Status status, boolean forceButtonColor, String color) {
		return makeLink(container, ID, i, simplePageBean, simplePageToolDao, messageLocator, canEditPage, currentPage, notDone, status, forceButtonColor, color);
	}

	protected boolean makeLink(UIContainer container, String ID, SimplePageItem i, SimplePageBean simplePageBean, SimplePageToolDao simplePageToolDao, MessageLocator messageLocator,
									  boolean canEditPage, SimplePage currentPage, boolean notDone, Status status) {
		return makeLink(container, ID, i, simplePageBean, simplePageToolDao, messageLocator, canEditPage, currentPage, notDone, status, false, null);
	}

	/**
	 * 
	 * @param container
	 * @param ID
	 * @param i
	 * @param simplePageBean
	 * @param simplePageToolDao
	 * @return Whether or not this item is available.
	 */
	protected boolean makeLink(UIContainer container, String ID, SimplePageItem i, SimplePageBean simplePageBean,
                               SimplePageToolDao simplePageToolDao, MessageLocator messageLocator, boolean canEditPage,
                               SimplePage currentPage, boolean notDone, Status status, boolean forceButtonColor, String color) {
		String URL = "";
		boolean available = simplePageBean.isItemAvailable(i);
		boolean usable = available || canEditPage;
		boolean fake = !usable;  // by default, fake output if not available
		String itemString = Long.toString(i.getId());

		if ((SimplePageItem.DUMMY).equals(i.getSakaiId())) {
		    fake = true; // dummy is fake, but still available
		} else if (i.getType() == SimplePageItem.RESOURCE || i.getType() == SimplePageItem.URL) {
			if (usable) {
				if (i.getType() == SimplePageItem.RESOURCE && i.isSameWindow()) {
					GeneralViewParameters params = new GeneralViewParameters(ShowItemProducer.VIEW_ID);
					params.setSendingPage(currentPage.getPageId());
					params.setItemId(i.getId());
					UILink link = UIInternalLink.make(container, "link", params);
					link.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));
					if (! available)
					    fakeDisableLink(link, messageLocator);
				} else {
				    // run this through /access/lessonbuilder so we can track it even if the user uses the context menu
				    // We could do this only for the notDone case, but I think it could cause trouble for power users
				    // if the url isn't always consistent.
				    if (i.getAttribute("multimediaUrl") != null) { // resource where we've stored the URL ourselves
					    URL = "/access/lessonbuilder/item/" + i.getId() + "/";
				    } else {
					    URL = i.getItemURL(simplePageBean.getCurrentSiteId(),currentPage.getOwner());
				    }
				    UILink link = UILink.make(container, ID, URL);
				    link.decorate(new UIFreeAttributeDecorator("target", "_blank"));
				    if (notDone)
					    link.decorate(new UIFreeAttributeDecorator("onclick",
										   "afterLink($(this)," + i.getId() + ") ; return true"));
				    if (! available)
					    fakeDisableLink(link, messageLocator);
				}
			}

		} else if (i.getType() == SimplePageItem.PAGE) {
			SimplePage p = simplePageToolDao.getPage(Long.valueOf(i.getSakaiId()));

			if(p != null) {
				GeneralViewParameters eParams = new GeneralViewParameters(ShowPageProducer.VIEW_ID, p.getPageId());
				eParams.setItemId(i.getId());
				// nextpage indicates whether it should be pushed onto breadcrumbs
				// or replace the top item
				if (i.getNextPage()) {
					eParams.setPath("next");
				} else {
					eParams.setPath("push");
				}
				boolean isbutton = false;
				// button says to display the link as a button. use navIntrTool,
				// which is standard
				// Sakai CSS that generates the type of button used in toolbars. We
				// have to override
				// with background:transparent or we get remnants of the gray
				if ("button".equals(i.getFormat())) {
					isbutton = true;
					UIOutput span = UIOutput.make(container, ID + "-button-span");
					ID = ID + "-button";
					isbutton = true;
				}
				
				UILink link;
				// if item shouldn't be visible, show fake link
				// if canEditPage, code already handles the situation
				if (!canEditPage && available && 
				    (p.isHidden() || p.getReleaseDate() != null && p.getReleaseDate().after(new Date()))) {
				    fake = true;
				}

				if (available && !fake) {
					link = UIInternalLink.make(container, ID, eParams);
					link.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));
					if (i.isPrerequisite()) {
						simplePageBean.checkItemPermissions(i, true);
					}
					if(!forceButtonColor){
						link.decorate(new UIStyleDecorator(i.getAttribute("btnColor")));
					}else{
						link.decorate(new UIStyleDecorator(color));
					}
					// at this point we know the page isn't available, i.e. user
					// hasn't
					// met all the prerequistes. Normally we give them a nonworking
					// grayed out link. But if they are the author, we want to
					// give them a real link. Otherwise if it's a subpage they have
					// no way to get to it (currently -- we'll fix that)
					// but we make it look like it's disabled so they can see what
					// students see
				} else if (canEditPage) {
				    // for author, need to be able to get to the subpage to edit it
				    // so put out a function button, but make it look disabled
					fake = false; // so we don't get an fake button as well
					link = UIInternalLink.make(container, ID, eParams);
					link.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));
					fakeDisableLink(link, messageLocator);
				}  // otherwise fake

				if (canEditPage || (p.getOwner()!=null && p.getOwner().equals(simplePageBean.getCurrentUserId()))) {
					String owner = getUserDisplayName(p.getOwner());
					if (StringUtils.isNotBlank(owner)) {
						UIOutput.make(container, "owner-text", "(" + messageLocator.getMessage("simplepage.owner.settings") + ": " + owner + ")");
					}
				}
			}else {
				log.warn("Lesson Builder Item #" + i.getId() + " does not have an associated page.");
				return false;
			}
		} else if (i.getType() == SimplePageItem.ASSIGNMENT) {
		    // assignments won't let us get the entity if we're not in the group, so set up permissions before other tests
			if (usable && i.isPrerequisite()) {
			    simplePageBean.checkItemPermissions(i, true);
			}
			LessonEntity lessonEntity = assignmentEntity.getEntity(i.getSakaiId(), simplePageBean);
			if (usable && lessonEntity != null && (canEditPage || !lessonEntity.notPublished())) {

				GeneralViewParameters params = new GeneralViewParameters(ShowItemProducer.VIEW_ID);
				params.setSendingPage(currentPage.getPageId());
				params.setItemId(i.getId());
				UILink link = UIInternalLink.make(container, "link", params);
				link.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));

				if (lessonEntity.showAdditionalLink()){
					UIOutput.make(container, "link-seperator", "");
					GeneralViewParameters reviewParams = (GeneralViewParameters) params.copy();
					reviewParams.setReviewAssessment(true);
					UIInternalLink.make(container, "link-additional", reviewParams)
						.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));
				}

				if (! available)
				    fakeDisableLink(link, messageLocator);
			} else {
				if (i.isPrerequisite()) {
					simplePageBean.checkItemPermissions(i, false);
				}
				fake = true; // need to set this in case it's available for missing entity
			}
		} else if (i.getType() == SimplePageItem.ASSESSMENT) {
		    // assignments won't let us get the entity if we're not in the group, so set up permissions before other tests
			if (usable && i.isPrerequisite()) {
			    simplePageBean.checkItemPermissions(i, true);
			}
			LessonEntity lessonEntity = quizEntity.getEntity(i.getSakaiId(),simplePageBean);
			if (usable && lessonEntity != null && (canEditPage || !quizEntity.notPublished(i.getSakaiId()))) {
				// we've hacked Samigo to look at a special lesson builder
				// session
				// attribute. otherwise at the end of the test, Samigo replaces
				// the
				// whole screen, exiting form our iframe. The other tools don't
				// do this.
				GeneralViewParameters view = new GeneralViewParameters(ShowItemProducer.VIEW_ID);
				view.setSendingPage(currentPage.getPageId());
				view.setClearAttr("LESSONBUILDER_RETURNURL_SAMIGO");
				view.setItemId(i.getId());
				UILink link = UIInternalLink.make(container, "link", view);
				link.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));

				if (lessonEntity.showAdditionalLink()) {
					UIOutput.make(container, "link-seperator", "");
					GeneralViewParameters reviewParams = (GeneralViewParameters) view.copy();
					reviewParams.setReviewAssessment(true);
					UIInternalLink.make(container, "link-additional", reviewParams)
						.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));
				}

				if (! available)
				    fakeDisableLink(link, messageLocator);
			} else {
				if (i.isPrerequisite()) {
					simplePageBean.checkItemPermissions(i, false);
				}
				fake = true; // need to set this in case it's available for missing entity
			}
		} else if (i.getType() == SimplePageItem.FORUM) {
		    // assignments won't let us get the entity if we're not in the group, so set up permissions before other tests
			if (usable && i.isPrerequisite()) {
			    simplePageBean.checkItemPermissions(i, true);
			}
			LessonEntity lessonEntity = forumEntity.getEntity(i.getSakaiId());
			if (usable && lessonEntity != null && (canEditPage || !lessonEntity.notPublished())) {
				if (i.isPrerequisite()) {
					simplePageBean.checkItemPermissions(i, true);
				}
				GeneralViewParameters view = new GeneralViewParameters(ShowItemProducer.VIEW_ID);
				view.setSendingPage(currentPage.getPageId());
				view.setItemId(i.getId());
				UILink link = UIInternalLink.make(container, "link", view);
				link.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));
				if (! available)
				    fakeDisableLink(link, messageLocator);
			} else {
				if (i.isPrerequisite()) {
					simplePageBean.checkItemPermissions(i, false);
				}
				fake = true; // need to set this in case it's available for missing entity
			}
		} else if (i.getType() == SimplePageItem.CHECKLIST) {
			if (usable) {
				if (i.isPrerequisite()) {
					simplePageBean.checkItemPermissions(i, true);
				}
				GeneralViewParameters view = new GeneralViewParameters(ChecklistProducer.VIEW_ID);
				view.setSendingPage(currentPage.getPageId());
				view.setItemId(i.getId());
				UILink link = UIInternalLink.make(container, "link", view);
				link.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));
				if (! available)
				    fakeDisableLink(link, messageLocator);
			} else {
				if (i.isPrerequisite()) {
					simplePageBean.checkItemPermissions(i, false);
				}
				fake = true; // need to set this in case it's available for missing entity
			}
		} else if (i.getType() == SimplePageItem.BLTI) {
		    LessonEntity lessonEntity = (bltiEntity == null ? null : bltiEntity.getEntity(i.getSakaiId()));
		    if ("inline".equals(i.getFormat())) {
                // no availability
                String height=null;
                if (i.getHeight() != null && !i.getHeight().equals(""))
                    height = i.getHeight().replace("px","");  // just in case

                UIComponent iframe = UIOutput.make(container, "blti-iframe")
                        .decorate(new UIFreeAttributeDecorator("allow", String.join(";",
                                Optional.ofNullable(ServerConfigurationService.getStrings("browser.feature.allow"))
                                        .orElseGet(() -> new String[]{}))));
                if (lessonEntity != null)
                    iframe.decorate(new UIFreeAttributeDecorator("src", lessonEntity.getUrl()));

                String h = "300";
                if (height != null && !height.trim().equals(""))
                    h = height;

                iframe.decorate(new UIFreeAttributeDecorator("height", h));
                iframe.decorate(new UIFreeAttributeDecorator("title", i.getName()));
                // normally we get the name from the link text, but there's no link text here
                UIOutput.make(container, "item-name", i.getName());
            } else if (!"window".equals(i.getFormat()) && (i.getFormat() != null)) {
                // this is the default if format isn't valid or is missing
                if (usable && lessonEntity != null) {
                    // I'm fairly sure checkitempermissions doesn't do anything useful for LTI,
                    // as it isn't group aware
                    if (i.isPrerequisite()) {
                        simplePageBean.checkItemPermissions(i, true);
                    }
                    GeneralViewParameters view = new GeneralViewParameters(ShowItemProducer.VIEW_ID);
                    view.setSendingPage(currentPage.getPageId());
                    view.setItemId(i.getId());
                    UILink link = UIInternalLink.make(container, "link", view);
                    link.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));
                    if (! available)
                        fakeDisableLink(link, messageLocator);
                } else {
                    if (i.isPrerequisite()) {
                        simplePageBean.checkItemPermissions(i, false);
                    }
                    fake = true; // need to set this in case it's available for missing entity
                }
            } else {
                if (usable && lessonEntity != null) {
					if (i.isPrerequisite()) {
					    simplePageBean.checkItemPermissions(i, true);
					}
					URL = lessonEntity.getUrl();
					// UIInternalLink link = LinkTrackerProducer.make(container, ID, i.getName(), URL, i.getId(), notDone);
					UILink link = UILink.make(container, ID, URL);
					link.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));
					link.decorate(new UIFreeAttributeDecorator("target", "_blank"));
					if (! available)
						fakeDisableLink(link, messageLocator);
					if (notDone)
						link.decorate(new UIFreeAttributeDecorator("onclick",
						 "setTimeout(function(){window.location.reload(true)},3000); return true"));

				} else {
					fake = true; // need to set this in case it's available for missing entity
				}
            }
		}

		String note = null;
		if (status == Status.COMPLETED) {
			note = messageLocator.getMessage("simplepage.status.completed");
		}
		if (status == Status.REQUIRED) {
			note = messageLocator.getMessage("simplepage.status.required");
		}

		if (fake) {
			ID = ID + "-fake";
			String linkText = i.getName();
			if (i.getType() == SimplePageItem.ASSIGNMENT) {
				linkText = getLinkText(linkText, i.getSakaiId());
			}
			UIOutput link = UIOutput.make(container, ID, linkText);
			link.decorate(new UIFreeAttributeDecorator("lessonbuilderitem", itemString));
			// fake and available occurs when prerequisites aren't the issue (it's avaiable)
			// so the item must be nonexistent or otherwise unavalable.
			if (available) {
				if (i.getType() == SimplePageItem.PAGE) {
					// set up locale
					Locale M_locale = null;
					String langLoc[] = localegetter.get().toString().split("_");
					if (langLoc.length >= 2) {
						if ("en".equals(langLoc[0]) && "ZA".equals(langLoc[1])) {
							M_locale = new Locale("en", "GB");
						} else {
							M_locale = new Locale(langLoc[0], langLoc[1]);
						}
					} else {
						M_locale = new Locale(langLoc[0]);
					}
					String releaseString = simplePageBean.getReleaseString(i, M_locale);
					link.decorate(new UITooltipDecorator(releaseString));
				} else {
					link.decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.not_usable")));
				}
			} else {
				link.decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.complete_required")));
			}
		} else {
			String linkText = i.getName();
			String linkAdditionalText = "";
			LessonEntity lessonEntity = null;
			switch (i.getType()) {
				case SimplePageItem.ASSIGNMENT:
					linkText = getLinkText(linkText, i.getSakaiId());
				case SimplePageItem.ASSESSMENT:
					lessonEntity = quizEntity.getEntity(i.getSakaiId(), simplePageBean);
					linkAdditionalText = messageLocator.getMessage("simplepage.assignment.review_submissions");
				default:
					UIOutput.make(container, ID + "-text", linkText)
						.decorate(new UIFreeAttributeDecorator("data-original-name", i.getName()));

					if (lessonEntity != null && lessonEntity.showAdditionalLink()) {
						UIOutput.make(container, ID + "-additional-text", linkAdditionalText)
							.decorate(new UIFreeAttributeDecorator("data-original-name", linkAdditionalText));
					}
					break;
			}

		}

		if (note != null) {
			UIOutput.make(container, ID + "-note", note + " ");
		}

		return available;
	}

	
	private String getLinkText(String linkText, String sakaiId ) {
		//Create a link with open and due dates for assignments links in Lessons
		SecurityAdvisor yesMan = (String arg0, String arg1, String agr2) -> SecurityAdvisor.SecurityAdvice.ALLOWED;
		securityService.pushAdvisor(yesMan);
		try {
			TimeZone tz = userTimeService.getLocalTimeZone();
			df.setTimeZone(tz);

			AssignmentEntity assignment = (AssignmentEntity) assignmentEntity.getEntity(sakaiId, simplePageBean);
			linkText += " " + messageLocator.getMessage("simplepage.assignment.open_close_date", 
					new Object[] {
							df.format(assignment.getOpenDate()),
							assignment.isHiddenDueDate() ? "-" : df.format(assignment.getDueDate())
			});
		} catch (Exception ex) {
			log.debug("getLinkText date exception", ex);
		}
		finally {
			securityService.popAdvisor(yesMan);
		}
		return linkText;
	}

	private static String getUserDisplayName(String owner) {
		if (owner == null) return StringUtils.EMPTY;

		try {
			User user = UserDirectoryService.getUser(owner);
			return String.format("%s (%s)", user.getSortName(), user.getEid());
		} catch (UserNotDefinedException e) {
            log.info("Owner #: {} does not have an associated user.", owner);
		}
		return StringUtils.EMPTY;
	}

	private static User getUser(String userId) {
		try {
			return UserDirectoryService.getUser(userId);
		} catch (UserNotDefinedException e) {
			log.error("User {} does not exist", userId);
		}
		return null;
	}

	//Get the twitter widget hashtag and other settings from the user.
	private void createTwitterDialog(UIContainer tofill, SimplePage currentPage) {
		UIOutput.make(tofill, "add-twitter-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.twitter")));
		UIForm form = UIForm.make(tofill, "add-twitter-form");
		makeCsrf(form, "csrf13");
		UIInput.make(form, "twitterEditId", "#{simplePageBean.itemId}");
		UIInput.make(form, "twitter-addBefore", "#{simplePageBean.addBefore}");
		UIInput.make(form, "twitter-username", "#{simplePageBean.twitterUsername}");
		UIOutput.make(form, "twitter-username-label", messageLocator.getMessage("simplepage.twitter-username"));
		UIInput.make(form, "widget-height", "#{simplePageBean.twitterWidgetHeight}");
		UIOutput.make(form, "height-label", messageLocator.getMessage("simplepage.twitter.height_label"));
		//Adding default values for tweet number dropdown
		String[] options = {"5","10","25","50","100","1000"};
		String[] labels = {"5","10","25","50","100","1000"};
		UIOutput.make(form, "numberDropdownLabel", messageLocator.getMessage("simplepage.number-dropdown-label"));
		UISelect.make(form, "numberDropdown", options, labels, "#{simplePageBean.twitterDropDown}","5");
		UICommand.make(form, "twitter-add-item", messageLocator.getMessage("simplepage.save_message"), "#{simplePageBean.addTwitterTimeline}");
		UICommand.make(form, "twitter-cancel", messageLocator.getMessage("simplepage.cancel"), null);
		UICommand.make(form, "delete-twitter-item", messageLocator.getMessage("simplepage.delete"), "#{simplePageBean.deleteItem}");
	}
	private static void disableLink(UIComponent link, MessageLocator messageLocator) {
		link.decorate(new UIFreeAttributeDecorator("onclick", "return false"));
		link.decorate(new UIDisabledDecorator());
		link.decorate(new UIStyleDecorator("disabled"));
		link.decorate(new UIFreeAttributeDecorator("style", "color:#999 !important"));
		link.decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.complete_required")));
	}

	// show is if it was disabled but don't actually
	private static void fakeDisableLink(UIComponent link, MessageLocator messageLocator) {
		link.decorate(new UIFreeAttributeDecorator("style", "color:#999 !important"));
		link.decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.complete_required")));
	}

	private void styleItem(UIContainer row, UIComponent container, UIComponent component, SimplePageItem i, String indent, String customClass) {
	    // Indent level - default to 0 if not previously set
	    String indentLevel = i.getAttribute(SimplePageItem.INDENT)==null?"0":i.getAttribute(SimplePageItem.INDENT);
	    // Indent number in em is 4 times the level of indent

	    if (!"0".equals(indentLevel))
		container.decorate(new UIFreeAttributeDecorator("x-indent", indentLevel));
	    if (indent != null)
		UIOutput.make(row, indent, indentLevel);

	    // Custom css class(es)
	    String customCssClass = i.getAttribute(SimplePageItem.CUSTOMCSSCLASS);
	    if (customCssClass != null && !customCssClass.equals("")) {
		component.decorate(new UIStyleDecorator(customCssClass));
	    }
	    if (customClass != null) {
		UIOutput.make(row, customClass, customCssClass);
	    }

	}

	public void setSimplePageToolDao(SimplePageToolDao s) {
		simplePageToolDao = s;
	}

	public void setDateEvolver(FormatAwareDateInputEvolver dateevolver) {
		this.dateevolver = dateevolver;
	}

	public void setLocaleGetter(LocaleGetter localegetter) {
		this.localegetter = localegetter;
	}

	public void setForumEntity(LessonEntity e) {
		// forumEntity is static, so it may already have been set
		// there is a possible race condition, but since the bean is
		// a singleton both people in the race will be trying to set
		// the same value. So it shouldn't matter
		if (forumEntity == null) {
			forumEntity = e;
		}
	}

	public void setQuizEntity(LessonEntity e) {
		// forumEntity is static, so it may already have been set
		// there is a possible race condition, but since the bean is
		// a singleton both people in the race will be trying to set
		// the same value. So it shouldn't matter
		if (quizEntity == null) {
			quizEntity = e;
		}
	}

	public void setAssignmentEntity(LessonEntity e) {
		// forumEntity is static, so it may already have been set
		// there is a possible race condition, but since the bean is
		// a singleton both people in the race will be trying to set
		// the same value. So it shouldn't matter
		if (assignmentEntity == null) {
			assignmentEntity = e;
		}
	}

	public void setBltiEntity(LessonEntity e) {
	    	if (bltiEntity == null)
			bltiEntity = e;
	}

	//Create a latest forum conversations dialog where user can enter other settings for the forum summary div
	private void createForumSummaryDialog(UIContainer tofill, SimplePage currentPage) {
		UIOutput.make(tofill, "add-forum-summary-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.forumSummaryLinkText")));
		UIForm form = UIForm.make(tofill, "add-forum-summary-form");
		makeCsrf(form, "csrf24");
		//check if site has forum tool added?if not then display info and return
		if (simplePageBean.getCurrentTool(simplePageBean.FORUMS_TOOL_ID) == null) {
			UIOutput.make(tofill, "forum-summary-error-div");
			UIOutput.make(tofill, "forum-summary-error-span", messageLocator.getMessage("simplepage.no_forum_tools"));
			UICommand.make(form, "forum-summary-cancel", messageLocator.getMessage("simplepage.cancel"), null);
			return;
		}
		UIInput.make(form, "forumSummaryEditId", "#{simplePageBean.itemId}");
		String[] options = {"5", "10", "15", "20", "30", "50"};
		String[] labels = {"5", "10", "15", "20", "30", "50"};
		UIOutput.make(form, "forumNumberDropdownLabel", messageLocator.getMessage("simplepage.forum-number-dropdown-label"));
		UISelect.make(form, "forumNumberDropdown", options, labels, "#{simplePageBean.forumSummaryDropDown}", "5");
		UICommand.make(form, "forum-summary-add-item", messageLocator.getMessage("simplepage.save_message"), "#{simplePageBean.addForumSummary}");
		UIInput.make(form, "forum-summary-add-before", "#{simplePageBean.addBefore}");
		UICommand.make(form, "forum-summary-cancel", messageLocator.getMessage("simplepage.cancel"), null);
		UICommand.make(form, "delete-forum-summary-item", messageLocator.getMessage("simplepage.delete"), "#{simplePageBean.deleteItem}");
	}
	public void setToolManager(ToolManager m) {
		toolManager = m;
	}

	public void setLessonBuilderAccessService (LessonBuilderAccessService a) {
	    if (lessonBuilderAccessService == null)
		lessonBuilderAccessService = a;
	}

	public ViewParameters getViewParameters() {
		return new GeneralViewParameters();
	}

	private void createToolBar(UIContainer tofill, SimplePage currentPage) {
		UIBranchContainer toolBar = UIBranchContainer.make(tofill, "tool-bar:");
		boolean studentPage = simplePageBean.isStudentPage(currentPage);

		// toolbar

		// dropdowns
		UIOutput.make(toolBar, "icondropc");
		UIOutput.make(toolBar, "icondrop");

		// right side
		createToolBarLink(ReorderProducer.VIEW_ID, toolBar, "reorder", "simplepage.reorder", currentPage, "simplepage.reorder-tooltip");
		createToolBarLink(SubpageBulkEditProducer.VIEW_ID, toolBar, "bulk-edit-pages", "simplepage.bulk-edit-pages", currentPage, "simplepage.bulk-edit-pages.tooltip");

		UIComponent layoutlink = UIInternalLink.makeURL(tofill, "layout-link", "#");
		layoutlink.decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.layout-descrip")));

		// add content menu
		createToolBarLink(EditPageProducer.VIEW_ID, tofill, "add-text1", null, currentPage, "simplepage.text.tooltip").setItemId(null);
		createFilePickerToolBarLink(ResourcePickerProducer.VIEW_ID, tofill, "add-resource1", null, false, false,  currentPage, "simplepage.resource.tooltip");
		createFilePickerToolBarLink(ResourcePickerProducer.VIEW_ID, tofill, "add-multimedia1", null, true, false, currentPage, "simplepage.multimedia.tooltip");
		UIInternalLink.makeURL(tofill, "subpage-link1", "#").
		    decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.subpage-descrip")));
		UIInternalLink.makeURL(tofill, "addcontent", "#").
		    decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.add-item-page")));

		createToolBarLink(EditPageProducer.VIEW_ID, tofill, "add-text", "simplepage.text", currentPage, "simplepage.text.tooltip").setItemId(null);
		createFilePickerToolBarLink(ResourcePickerProducer.VIEW_ID, tofill, "add-multimedia", "simplepage.multimedia", true, false, currentPage, "simplepage.multimedia.tooltip");
		createFilePickerToolBarLink(ResourcePickerProducer.VIEW_ID, tofill, "add-resource", "simplepage.resource", false, false,  currentPage, "simplepage.resource.tooltip");
		boolean showAddResourceFolderLink = ServerConfigurationService.getBoolean("lessonbuilder.show.resource.folder.link", true);
		if (showAddResourceFolderLink){
			createToolBarLink(FolderPickerProducer.VIEW_ID, tofill, "add-folder", "simplepage.folder", currentPage, "simplepage.addfolder.tooltip").setItemId(null);
		}
		UIComponent subpagelink = UIInternalLink.makeURL(tofill, "subpage-link", "#");
		subpagelink.decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.subpage-descrip")));
		createAddFromSubpageToolBarLink(PagePickerProducer.VIEW_ID, tofill, "add-from-subpage", "simplepage.reorder-addpage", currentPage, "simplepage.reorder-addpage");

		UIOutput.make(tofill, "add-break1");
		UIOutput.make(tofill, "add-break2");
		UIOutput.make(tofill, "add-break3");
		UIOutput.make(tofill, "add-break4");
		UIOutput.make(tofill, "add-break5");

		// content menu not on students
		if (!studentPage) {

		    // add website.
		    // Are we running a kernel with KNL-273?
			UIOutput.make(tofill, "addwebsite-li");
			createFilePickerToolBarLink(ResourcePickerProducer.VIEW_ID, tofill, "add-website", "simplepage.website", false, true, currentPage, "simplepage.website.tooltip");

		    //Adding 'Embed Announcements' component
		    UIOutput.make(tofill, "announcements-li");
		    UILink announcementsLink = UIInternalLink.makeURL(tofill, "announcements-link", "#");
		    announcementsLink.decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.announcements-descrip")));
		    UIOutput.make(tofill, "assignment-li");
		    createToolBarLink(AssignmentPickerProducer.VIEW_ID, tofill, "add-assignment", "simplepage.assignment-descrip", currentPage, "simplepage.assignment");

		    boolean showEmbedCalendarLink = ServerConfigurationService.getBoolean("lessonbuilder.show.calendar.link", true);
		    if (showEmbedCalendarLink){
			UIOutput.make(tofill, "calendar-li");
			UIOutput.make(tofill, "calendar-link").decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.calendar-descrip")));
			UIForm form = UIForm.make(tofill, "add-calendar-form");
			UIInput.make(form, "calendar-addBefore", "#{simplePageBean.addBefore}");
			makeCsrf(form, "csrf29");
			UICommand.make(form, "add-calendar", "#{simplePageBean.addCalendar}");
		    }		    
		    UIOutput.make(tofill, "quiz-li");
		    createToolBarLink(QuizPickerProducer.VIEW_ID, tofill, "add-quiz", "simplepage.quiz-descrip", currentPage, "simplepage.quiz");

		    //Adding 'Embed forum conversations' component
		    UIOutput.make(tofill, "forum-summary-li");
		    UILink forumSummaryLink = UIInternalLink.makeURL(tofill, "forum-summary-link", "#");
			forumSummaryLink.decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.forum-summary-descrip")));

		    UIOutput.make(tofill, "forum-li");
		    createToolBarLink(ForumPickerProducer.VIEW_ID, tofill, "add-forum", "simplepage.forum-descrip", currentPage, "simplepage.forum.tooltip");

		    UIOutput.make(tofill, "checklist-li");
		    createToolBarLink(ChecklistProducer.VIEW_ID, tofill, "add-checklist", "simplepage.checklist", currentPage, "simplepage.checklist");

		    UIOutput.make(tofill, "question-li");
		    UIComponent questionlink = UIInternalLink.makeURL(tofill, "question-link", "#");
		    questionlink.decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.question-descrip")));

		    UIOutput.make(tofill, "student-li");
		    UIOutput.make(tofill, "add-comments-link").	decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.comments.tooltip")));
		    UIForm form = UIForm.make(tofill, "add-comments-form");
		    UIInput.make(form, "comments-addBefore", "#{simplePageBean.addBefore}");
		    makeCsrf(form, "csrf25");
		    UICommand.make(form, "add-comments", "#{simplePageBean.addCommentsSection}");

		    UIOutput.make(tofill, "studentcontent-li");
		    UIOutput.make(tofill, "add-student-link").	decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.student-descrip")));
		    form = UIForm.make(tofill, "add-student-form");
		    UIInput.make(form, "add-student-addBefore", "#{simplePageBean.addBefore}");
		    makeCsrf(form, "csrf26");
		    UICommand.make(form, "add-student", "#{simplePageBean.addStudentContentSection}");

			boolean showEmbedTwitterLink = ServerConfigurationService.getBoolean("lessonbuilder.show.twitter.link", false);
			if (showEmbedTwitterLink){
				//Adding 'Embed twitter timeline' component
				UIOutput.make(tofill, "twitter-li");
				UILink twitterLink = UIInternalLink.makeURL(tofill, "add-twitter", "#");
				twitterLink.decorate(new UITooltipDecorator(messageLocator.getMessage("simplepage.twitter-descrip")));
			}

			// Add External Learning App
			UIOutput.make(tofill, "blti-app-li");
			createAppStoreToolBarLink(BltiPickerProducer.VIEW_ID, tofill, "add-blti-app", "simplepage.blti.app", currentPage, "simplepage.blti.app.tooltip");
			
		}
	}

	private GeneralViewParameters createToolBarLink(String viewID, UIContainer tofill, String ID, String message, SimplePage currentPage, String tooltip) {
		GeneralViewParameters params = new GeneralViewParameters();
		params.setSendingPage(currentPage.getPageId());
		createStandardToolBarLink(viewID, tofill, ID, message, params, tooltip);
		return params;
	}

	private GeneralViewParameters createAppStoreToolBarLink(String viewID, UIContainer tofill, String ID, String message, SimplePage currentPage, String tooltip) {
		GeneralViewParameters params = new GeneralViewParameters();
		params.setSendingPage(currentPage.getPageId());
		params.bltiAppStores = true;
		createStandardToolBarLink(viewID, tofill, ID, message, params, tooltip);
		return params;
	}


	private GeneralViewParameters createAddFromSubpageToolBarLink(String viewID, UIContainer tofill, String ID, String message, SimplePage currentPage, String tooltip) {
		GeneralViewParameters params = new GeneralViewParameters();
		params.setSendingPage(currentPage.getPageId());
		params.setReturnView("reorder"); // flag to pagepicker that it needs to come back
		params.setSource("anotherPage");	//flag that shows what link was clicked
		createStandardToolBarLink(viewID, tofill, ID, message, params, tooltip);
		return params;
	}

	private FilePickerViewParameters createFilePickerToolBarLink(String viewID, UIContainer tofill, String ID, String message, boolean resourceType, boolean website, SimplePage currentPage, String tooltip) {
		FilePickerViewParameters fileparams = new FilePickerViewParameters();
		fileparams.setSender(currentPage.getPageId());
		fileparams.setResourceType(resourceType);
		fileparams.setWebsite(website);
		createStandardToolBarLink(viewID, tofill, ID, message, fileparams, tooltip);
		return fileparams;
	}

	private void createStandardToolBarLink(String viewID, UIContainer tofill, String ID, String message, SimpleViewParameters params, String tooltip) {
		params.viewID = viewID;
		if (message != null)
		    message = messageLocator.getMessage(message);
		UILink link = UIInternalLink.make(tofill, ID, message , params);
		link.decorate(new UITooltipDecorator(messageLocator.getMessage(tooltip)));
	}

	public List reportNavigationCases() {
		List<NavigationCase> togo = new ArrayList<NavigationCase>();
		togo.add(new NavigationCase(null, new SimpleViewParameters(ShowPageProducer.VIEW_ID)));
		togo.add(new NavigationCase("success", new SimpleViewParameters(ShowPageProducer.VIEW_ID)));
		togo.add(new NavigationCase("cancel", new SimpleViewParameters(ShowPageProducer.VIEW_ID)));
		togo.add(new NavigationCase("failure", new SimpleViewParameters(ReloadPageProducer.VIEW_ID)));
		togo.add(new NavigationCase("reload", new SimpleViewParameters(ReloadPageProducer.VIEW_ID)));
		togo.add(new NavigationCase("removed", new SimpleViewParameters(RemovePageProducer.VIEW_ID)));
		
		return togo;
	}

	private void createSubpageDialog(UIContainer tofill, SimplePage currentPage) {
		UIOutput.make(tofill, "subpage-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.subpage")));
		UIForm form = UIForm.make(tofill, "subpage-form");
		makeCsrf(form, "csrf7");

		UIOutput.make(form, "subpage-label", messageLocator.getMessage("simplepage.pageTitle_label"));
		UIInput.make(form, "subpage-title", "#{simplePageBean.subpageTitle}");

		GeneralViewParameters view = new GeneralViewParameters(PagePickerProducer.VIEW_ID);
		view.setSendingPage(currentPage.getPageId());

		if(!simplePageBean.isStudentPage(currentPage)) {
			UIInternalLink.make(form, "subpage-choose", messageLocator.getMessage("simplepage.choose_existing_page"), view);
			UIOutput.make(form, "subpage-choose-button", messageLocator.getMessage("simplepage.page.chooser"));
		}
		
		UIBoundBoolean.make(form, "subpage-next", "#{simplePageBean.subpageNext}", false);
		UIBoundBoolean.make(form, "subpage-button", "#{simplePageBean.subpageButton}", false);

		UISelect buttonColors = UISelect.make(form, "subpage-btncolor", SimplePageBean.NewColors, simplePageBean.getNewColorLabelsI18n(), "#{simplePageBean.buttonColor}", SimplePageBean.NewColors[0]);

		UIInput.make(form, "subpage-add-before", "#{simplePageBean.addBefore}");
		UICommand.make(form, "create-subpage", messageLocator.getMessage("simplepage.create"), "#{simplePageBean.createSubpage}");
		UICommand.make(form, "cancel-subpage", messageLocator.getMessage("simplepage.cancel"), null);

	}

	private void createEditItemDialog(UIContainer tofill, SimplePage currentPage, SimplePageItem pageItem) {
		String currentToolTitle = simplePageBean.getPageTitle();
		String returnFromEditString = messageLocator.getMessage("simplepage.return_from_edit").replace("{}",currentToolTitle);  
  
		UIOutput.make(tofill, "edit-item-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edititem_header")));

		UIForm form = UIForm.make(tofill, "edit-form");
		makeCsrf(form, "csrf8");

		UIOutput.make(form, "name-label", messageLocator.getMessage("simplepage.name_label"));
		UIInput.make(form, "name", "#{simplePageBean.name}");

		UIOutput.make(form, "description-label", messageLocator.getMessage("simplepage.description_label"));
		UIInput.make(form, "description", "#{simplePageBean.description}");

		UIOutput changeDiv = UIOutput.make(form, "changeDiv");
		if(simplePageBean.isStudentPage(currentPage)) {
			changeDiv.decorate(new UIStyleDecorator("noDisplay"));
		}
		
		GeneralViewParameters params = new GeneralViewParameters();
		params.setSendingPage(currentPage.getPageId());
		params.viewID = AssignmentPickerProducer.VIEW_ID;
		UIInternalLink.make(form, "change-assignment", messageLocator.getMessage("simplepage.change_assignment"), params);

		params = new GeneralViewParameters();
		params.setSendingPage(currentPage.getPageId());
		params.viewID = QuizPickerProducer.VIEW_ID;
		UIInternalLink.make(form, "change-quiz", messageLocator.getMessage("simplepage.change_quiz"), params);

		params = new GeneralViewParameters();
		params.setSendingPage(currentPage.getPageId());
		params.viewID = ForumPickerProducer.VIEW_ID;
		UIInternalLink.make(form, "change-forum", messageLocator.getMessage("simplepage.change_forum"), params);

		params = new GeneralViewParameters();
		params.setSendingPage(currentPage.getPageId());
		params.viewID = BltiPickerProducer.VIEW_ID;
		UIInternalLink.make(form, "change-blti", messageLocator.getMessage("simplepage.change_blti"), params);

		FilePickerViewParameters fileparams = new FilePickerViewParameters();
		fileparams.setSender(currentPage.getPageId());
		fileparams.setResourceType(false);
		fileparams.setWebsite(false);
		fileparams.viewID = ResourcePickerProducer.VIEW_ID;
		UIInternalLink.make(form, "change-resource", messageLocator.getMessage("simplepage.change_resource"), fileparams);

		params = new GeneralViewParameters();
		params.setSendingPage(currentPage.getPageId());
		params.viewID = PagePickerProducer.VIEW_ID;
		UIInternalLink.make(form, "change-page", messageLocator.getMessage("simplepage.change_page"), params);

		params = new GeneralViewParameters();
		params.setSendingPage(currentPage.getPageId());
		params.setId(Long.toString(pageItem.getId()));
		params.setReturnView(VIEW_ID);
		params.setTitle(returnFromEditString);  
		params.setSource("EDIT");
		params.viewID = ShowItemProducer.VIEW_ID;
		UIInternalLink.make(form, "edit-item-object", params);
		UIOutput.make(form, "edit-item-text");

		params = new GeneralViewParameters();
		params.setSendingPage(currentPage.getPageId());
		params.setId(Long.toString(pageItem.getId()));
		params.setReturnView(VIEW_ID);
		params.setTitle(returnFromEditString);  
		params.setSource("SETTINGS");
		params.viewID = ShowItemProducer.VIEW_ID;
		UIInternalLink.make(form, "edit-item-settings", params);
		UIOutput.make(form, "edit-item-settings-text");

		UIBoundBoolean.make(form, "item-next", "#{simplePageBean.subpageNext}", false);
		UIBoundBoolean.make(form, "item-button", "#{simplePageBean.subpageButton}", false);

		UIInput.make(form, "item-id", "#{simplePageBean.itemId}");

		UIOutput permDiv = UIOutput.make(form, "permDiv");
		if(simplePageBean.isStudentPage(currentPage)) {
			permDiv.decorate(new UIStyleDecorator("noDisplay"));
		}
		
		UIBoundBoolean.make(form, "item-required2", "#{simplePageBean.subrequirement}", false);

		UIBoundBoolean.make(form, "item-required", "#{simplePageBean.required}", false);
		UIBoundBoolean.make(form, "item-prerequisites", "#{simplePageBean.prerequisite}", false);

		UIBoundBoolean.make(form, "item-newwindow", "#{simplePageBean.newWindow}", false);

		UISelect radios = UISelect.make(form, "format-select",
						new String[] {"window", "inline", "page"},
						"#{simplePageBean.format}", "");
		UISelectChoice.make(form, "format-window", radios.getFullID(), 0);
		UISelectChoice.make(form, "format-inline", radios.getFullID(), 1);
		UISelectChoice.make(form, "format-page", radios.getFullID(), 2);

		UIInput.make(form, "edit-height-value", "#{simplePageBean.height}");

		UISelect.make(form, "assignment-dropdown", SimplePageBean.GRADES, "#{simplePageBean.dropDown}", SimplePageBean.GRADES[0]);
		UIInput.make(form, "assignment-points", "#{simplePageBean.points}");

		LessonConditionUtil.makeConditionEditor(simplePageBean, form, "common-condition-editor");
		LessonConditionUtil.makeConditionPicker(simplePageBean, form, "common-condition-picker");

		UICommand.make(form, "edit-item", messageLocator.getMessage("simplepage.edit"), "#{simplePageBean.editItem}");

		String indentOptions[] = {"0","1","2","3","4","5","6","7","8"};
		UISelect.make(form, "indent-level", indentOptions, "#{simplePageBean.indentLevel}", indentOptions[0]);

		// If current user is an admin show the css class input box
		UIInput customCssClass = UIInput.make(form, "customCssClass", "#{simplePageBean.customCssClass}");
		UIOutput.make(form, "custom-css-label", messageLocator.getMessage("simplepage.custom.css.class"));

		UISelect buttonColors = UISelect.make(form, "btncolor", SimplePageBean.NewColors, simplePageBean.getNewColorLabelsI18n(), "#{simplePageBean.buttonColor}", SimplePageBean.NewColors[0]);

		UIBoundBoolean.make(form, "hide2", "#{simplePageBean.hidePage}", (currentPage.isHidden()));
		UIBoundBoolean.make(form, "page-releasedate2", "#{simplePageBean.hasReleaseDate}", Boolean.FALSE);

		String releaseDateString = "";
		try {
			releaseDateString = isoDateFormat.format(new Date());
		} catch (Exception e) {
			log.error(e + "bad format releasedate " + new Date());
		}

		UIOutput releaseForm2 = UIOutput.make(form, "releaseDate2:");
		UIOutput.make(form, "sbReleaseDate", releaseDateString);
		UIInput.make(form, "release_date2", "#{simplePageBean.releaseDate}" );

		// can't use site groups on user content, and don't want students to hack
		// on groups for site content
		if (!simplePageBean.isStudentPage(currentPage)) {
		    createGroupList(form, null, "", "#{simplePageBean.selectedGroups}");
		}
		UICommand.make(form, "edit-item-cancel", messageLocator.getMessage("simplepage.cancel"), null);
	}

	// for both add multimedia and add resource, as well as updating resources
	// in the edit dialogs
    public void createGroupList(UIContainer tofill, Collection<String> groupsSet, String prefix, String beanspec) {
		List<GroupEntry> groups = simplePageBean.getCurrentGroups();
		ArrayList<String> values = new ArrayList<String>();
		ArrayList<String> initValues = new ArrayList<String>();

		if (groups == null || groups.size() == 0)
			return;

		for (GroupEntry entry : groups) {
			values.add(entry.id);
			if (groupsSet != null && groupsSet.contains(entry.id)) {
				initValues.add(entry.id);
			}
		}
		if (groupsSet == null || groupsSet.size() == 0 || initValues.isEmpty()) {
			initValues.add("");
		}

		// this could happen if the only groups are Access groups
		if (values.size() == 0)
			return;

		UIOutput.make(tofill, prefix + "grouplist");
		UISelect select = UISelect.makeMultiple(tofill, prefix + "group-list-span", values.toArray(new String[1]), beanspec, initValues.toArray(new String[1]));

		int index = 0;
		for (GroupEntry entry : groups) {
			UIBranchContainer row = UIBranchContainer.make(tofill, prefix + "select-group-list:");
			UISelectChoice.make(row, prefix + "select-group", select.getFullID(), index);

			UIOutput.make(row, prefix + "select-group-text", entry.name);
			index++;
		}

	}
	//To display dialog to add Announcements widget in Lessons
	private void createAnnouncementsDialog(UIContainer tofill, SimplePage currentPage){
		UIOutput.make(tofill, "add-announcements-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.announcementsLinkText")));
		UIForm form = UIForm.make(tofill, "add-announcements-form");
		makeCsrf(form, "csrf23");
		//check if site has announcements tool added?if not then display info and return
		if(simplePageBean.getCurrentTool(simplePageBean.ANNOUNCEMENTS_TOOL_ID) == null){
			UIOutput.make(tofill, "announcements-error-div");
			UIOutput.make(tofill, "announcements-error-span", messageLocator.getMessage("simplepage.no_announcements_tool"));
			UICommand.make(form, "announcements-cancel", messageLocator.getMessage("simplepage.cancel"), null);
			UICommand.make(form, "delete-announcements-item", messageLocator.getMessage("simplepage.delete"), "#{simplePageBean.deleteItem}");
			return;
		}
		UIInput.make(form, "announcementsEditId", "#{simplePageBean.itemId}");
		String[] options = {"5","10","15","20","30","50"};
		String[] labels = {"5","10","15","20","30","50"};
		UIOutput.make(form, "announcementsNumberDropdownLabel", messageLocator.getMessage("simplepage.announcements-number-dropdown-label"));
		UISelect.make(form, "announcementsNumberDropdown", options, labels, "#{simplePageBean.announcementsDropdown}","5");
		UICommand.make(form, "announcements-add-item", messageLocator.getMessage("simplepage.save_message"), "#{simplePageBean.addAnnouncements}");
		UIInput.make(form, "announcements-add-before", "#{simplePageBean.addBefore}");
		UICommand.make(form, "announcements-cancel", messageLocator.getMessage("simplepage.cancel"), null);
		UICommand.make(form, "delete-announcements-item", messageLocator.getMessage("simplepage.delete"), "#{simplePageBean.deleteItem}");
	}
	// for both add multimedia and add resource, as well as updating resources
	// in the edit dialogs
	private void createAddMultimediaDialog(UIContainer tofill, SimplePage currentPage) {
		UIOutput.make(tofill, "add-multimedia-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.resource")));

		String max = ServerConfigurationService.getString("content.upload.max", "20");
		String uploadMax = ServerConfigurationService.getString("content.upload.ceiling", max);

		UIOutput.make(tofill, "mm-add-files-instructions", messageLocator.getMessage("simplepage.add_file_instructions", uploadMax));

		UILink.make(tofill, "mm-additional-instructions", messageLocator.getMessage("simplepage.additional-instructions-label"), 
			    getLocalizedURL( "multimedia.html", true));
		UILink.make(tofill, "mm-additional-website-instructions", messageLocator.getMessage("simplepage.additional-website-instructions-label"), 
			    getLocalizedURL( "website.html", true));

		
		
		UIOutput.make(tofill, "mm-max-file-upload-size", String.valueOf(uploadMax));
		UIForm form = UIForm.make(tofill, "add-multimedia-form");
		makeCsrf(form, "csrf9");

		UIInput.make(form, "mm-name", "#{simplePageBean.name}");
		UIInput.make(form, "mm-names", "#{simplePageBean.names}");
		UIOutput.make(form, "mm-file-label", messageLocator.getMessage("simplepage.upload_label"));

		UIOutput.make(form, "mm-url-label", messageLocator.getMessage("simplepage.addLink_label"));
		UIInput.make(form, "mm-url", "#{simplePageBean.mmUrl}");

		FilePickerViewParameters fileparams = new FilePickerViewParameters();
		fileparams.setSender(currentPage.getPageId());
		fileparams.setResourceType(true);
		fileparams.viewID = ResourcePickerProducer.VIEW_ID;
		
		UILink link = UIInternalLink.make(form, "mm-choose", messageLocator.getMessage("simplepage.choose_existing_or"), fileparams);

		if (!simplePageBean.isStudentPage(currentPage)) {
		    UIOutput.make(form, "mm-prerequisite-section");
		    UIBoundBoolean.make(form, "mm-prerequisite", "#{simplePageBean.prerequisite}", false);
		}
		UIBoundBoolean.make(form, "mm-file-replace", "#{simplePageBean.replacefile}", false);

		UICommand.make(form, "mm-add-item", messageLocator.getMessage("simplepage.save_message"), "#{simplePageBean.addMultimedia}");
		UIOutput.make(form, "mm-test-tryother").decorate(new UIFreeAttributeDecorator("value", messageLocator.getMessage("simplepage.mm-test-tryother")));
		UIOutput.make(form, "mm-test-start-over").decorate(new UIFreeAttributeDecorator("value", messageLocator.getMessage("simplepage.mm-test-start-over")));
		UIInput.make(form, "mm-item-id", "#{simplePageBean.itemId}");
		UIInput.make(form, "mm-add-before", "#{simplePageBean.addBefore}");
		UIInput.make(form, "mm-is-mm", "#{simplePageBean.isMultimedia}");
		UIInput.make(form, "mm-display-type", "#{simplePageBean.multimediaDisplayType}");
		UIInput.make(form, "mm-is-website", "#{simplePageBean.isWebsite}");
		UIInput.make(form, "mm-is-caption", "#{simplePageBean.isCaption}");
		UICommand.make(form, "mm-cancel", messageLocator.getMessage("simplepage.cancel"), null);
	}

	private void createImportCcDialog(UIContainer tofill) {
		UIOutput.make(tofill, "import-cc-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.import_cc")));

		UIForm form = UIForm.make(tofill, "import-cc-form");
		makeCsrf(form, "csrf11");

		UICommand.make(form, "import-cc-submit", messageLocator.getMessage("simplepage.import_message"), "#{simplePageBean.importCc}");
		UIBoundBoolean.make(form, "import-cc-archive", "#{simplePageBean.importArchive}", false);
		UICommand.make(form, "mm-cancel", messageLocator.getMessage("simplepage.cancel"), null);

		UIBoundBoolean.make(form, "import-toplevel", "#{simplePageBean.importtop}", false);


		class ToolData {
			String toolId;
			String toolName;
		}

		int numQuizEngines = 0;
		List<ToolData> quizEngines = new ArrayList<ToolData>();

		for (LessonEntity q = quizEntity; q != null; q = q.getNextEntity()) {
			String toolId = q.getToolId();
			String toolName = simplePageBean.getCurrentToolTitle(q.getToolId());
			// we only want the ones that are actually in our site
			if (toolName != null) {
				ToolData toolData = new ToolData();
				toolData.toolId = toolId;
				toolData.toolName = toolName;
				numQuizEngines++;
				quizEngines.add(toolData);
			}
		}

		if (numQuizEngines == 0) {
			UIVerbatim.make(form, "quizmsg", messageLocator.getMessage("simplepage.noquizengines"));
		} else if (numQuizEngines == 1) {
			UIInput.make(form, "quiztool", "#{simplePageBean.quiztool}", quizEntity.getToolId());
		} else { // put up message and then radio buttons for each possibility

			// need values array for RSF's select implementation. It sees radio
			// buttons as a kind of select
			ArrayList<String> values = new ArrayList<String>();
			for (ToolData toolData : quizEngines) {
				values.add(toolData.toolId);
			}

			// the message
			UIOutput.make(form, "quizmsg", messageLocator.getMessage("simplepage.choosequizengine"));
			// now the list of radio buttons
			UISelect quizselect = UISelect.make(form, "quiztools", values.toArray(new String[1]), "#{simplePageBean.quiztool}", null);
			int i = 0;
			for (ToolData toolData : quizEngines) {
				UIBranchContainer toolItem = UIBranchContainer.make(form, "quiztoolitem:", String.valueOf(i));
				UISelectChoice.make(toolItem, "quiztoolbox", quizselect.getFullID(), i);
				UIOutput.make(toolItem, "quiztoollabel", toolData.toolName);
				i++;
			}
		}

		int numTopicEngines = 0;
		List<ToolData> topicEngines = new ArrayList<ToolData>();

		for (LessonEntity q = forumEntity; q != null; q = q.getNextEntity()) {
			String toolId = q.getToolId();
			String toolName = simplePageBean.getCurrentToolTitle(q.getToolId());
			// we only want the ones that are actually in our site
			if (toolName != null) {
				ToolData toolData = new ToolData();
				toolData.toolId = toolId;
				toolData.toolName = toolName;
				numTopicEngines++;
				topicEngines.add(toolData);
			}
		}

		if (numTopicEngines == 0) {
			UIVerbatim.make(form, "topicmsg", messageLocator.getMessage("simplepage.notopicengines"));
		} else if (numTopicEngines == 1) {
			UIInput.make(form, "topictool", "#{simplePageBean.topictool}", forumEntity.getToolId());
		} else {
			ArrayList<String> values = new ArrayList<String>();
			for (ToolData toolData : topicEngines) {
				values.add(toolData.toolId);
			}

			UIOutput.make(form, "topicmsg", messageLocator.getMessage("simplepage.choosetopicengine"));
			UISelect topicselect = UISelect.make(form, "topictools", values.toArray(new String[1]), "#{simplePageBean.topictool}", null);
			int i = 0;
			for (ToolData toolData : topicEngines) {
				UIBranchContainer toolItem = UIBranchContainer.make(form, "topictoolitem:", String.valueOf(i));
				UISelectChoice.make(toolItem, "topictoolbox", topicselect.getFullID(), i);
				UIOutput.make(toolItem, "topictoollabel", toolData.toolName);
				i++;
			}
		}

		int numAssignEngines = 0;
		List<ToolData> assignEngines = new ArrayList<ToolData>();

		for (LessonEntity q = assignmentEntity; q != null; q = q.getNextEntity()) {
			String toolId = q.getToolId();
			String toolName = simplePageBean.getCurrentToolTitle(q.getToolId());
			// we only want the ones that are actually in our site
			if (toolName != null) {
				ToolData toolData = new ToolData();
				toolData.toolId = toolId;
				toolData.toolName = toolName;
				numAssignEngines++;
				assignEngines.add(toolData);
			}
		}

		if (numAssignEngines == 0) {
			UIVerbatim.make(form, "assignmsg", messageLocator.getMessage("simplepage.noassignengines"));
		} else if (numAssignEngines == 1) {
			UIInput.make(form, "assigntool", "#{simplePageBean.assigntool}", assignmentEntity.getToolId());
		} else {
			ArrayList<String> values = new ArrayList<String>();
			for (ToolData toolData : assignEngines) {
				values.add(toolData.toolId);
			}

			UIOutput.make(form, "assignmsg", messageLocator.getMessage("simplepage.chooseassignengine"));
			UISelect assignselect = UISelect.make(form, "assigntools", values.toArray(new String[1]), "#{simplePageBean.assigntool}", null);
			int i = 0;
			for (ToolData toolData : assignEngines) {
				UIBranchContainer toolItem = UIBranchContainer.make(form, "assigntoolitem:", String.valueOf(i));
				UISelectChoice.make(toolItem, "assigntoolbox", assignselect.getFullID(), i);
				UIOutput.make(toolItem, "assigntoollabel", toolData.toolName);
				i++;
			}
		}


	}

	private void createExportCcDialog(UIContainer tofill) {
		UIOutput.make(tofill, "export-cc-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.export-cc-title")));

		UIForm form = UIForm.make(tofill, "export-cc-form");
		UICommand.make(form, "export-cc-submit", messageLocator.getMessage("simplepage.exportcc-download"), "#{simplePageBean.importCc}");
		UICommand.make(form, "export-cc-cancel", messageLocator.getMessage("simplepage.cancel"), null);

		// the actual submission is with a GET. The submit button clicks this link.
		ExportCCViewParameters view = new ExportCCViewParameters("exportCc");
		view.setExportcc(true);
		view.setVersion("1.2");
		view.setBank("1");
		UIInternalLink.make(form, "export-cc-link", "export cc link", view);

	}

	private void createEditMultimediaDialog(UIContainer tofill, SimplePage currentPage) {
		UIOutput.make(tofill, "edit-multimedia-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.editMultimedia")));

		UIOutput.make(tofill, "instructions");

		UIForm form = UIForm.make(tofill, "edit-multimedia-form");
		makeCsrf(form, "csrf10");

		UIOutput.make(form, "height-label", messageLocator.getMessage("simplepage.height_label"));
		UIInput.make(form, "height", "#{simplePageBean.height}");

		UIOutput.make(form, "width-label", messageLocator.getMessage("simplepage.width_label"));
		UIInput.make(form, "width", "#{simplePageBean.width}");

		UIOutput.make(form, "description2-label", messageLocator.getMessage("simplepage.description_label"));
		UIInput.make(form, "description2", "#{simplePageBean.description}");

		if (!simplePageBean.isStudentPage(currentPage)) {
		    UIOutput.make(form, "multi-prerequisite-section");
		    UIBoundBoolean.make(form, "multi-prerequisite", "#{simplePageBean.prerequisite}",false);

			LessonConditionUtil.makeConditionPicker(simplePageBean, form, "multimedia-condition-picker");
		}

		FilePickerViewParameters fileparams = new FilePickerViewParameters();
		fileparams.setSender(currentPage.getPageId());
		fileparams.setResourceType(true);
		fileparams.viewID = ResourcePickerProducer.VIEW_ID;
		UIInternalLink.make(form, "change-resource-mm", messageLocator.getMessage("simplepage.change_resource"), fileparams);

		UIOutput.make(form, "alt-label", messageLocator.getMessage("simplepage.alt_label"));
		UIInput.make(form, "alt", "#{simplePageBean.alt}");

		UIInput.make(form, "mimetype", "#{simplePageBean.mimetype}");

		UICommand.make(form, "edit-multimedia-item", messageLocator.getMessage("simplepage.save_message"), "#{simplePageBean.editMultimedia}");

		UIInput.make(form, "multimedia-item-id", "#{simplePageBean.itemId}");

		UICommand.make(form, "delete-multimedia-item", messageLocator.getMessage("simplepage.delete"), "#{simplePageBean.deleteItem}");
		UICommand.make(form, "edit-multimedia-cancel", messageLocator.getMessage("simplepage.cancel"), null);
	}

	private void createYoutubeDialog(UIContainer tofill, SimplePage currentPage) {
		UIOutput.make(tofill, "youtube-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit_youtubelink")));

		UIForm form = UIForm.make(tofill, "youtube-form");
		makeCsrf(form, "csrf17");
		UIInput.make(form, "youtubeURL", "#{simplePageBean.youtubeURL}");
		UIInput.make(form, "youtubeEditId", "#{simplePageBean.youtubeId}");
		UIInput.make(form, "youtubeHeight", "#{simplePageBean.height}");
		UIInput.make(form, "youtubeWidth", "#{simplePageBean.width}");
		UIOutput.make(form, "description4-label", messageLocator.getMessage("simplepage.description_label"));
		UIInput.make(form, "description4", "#{simplePageBean.description}");
		UICommand.make(form, "delete-youtube-item", messageLocator.getMessage("simplepage.delete"), "#{simplePageBean.deleteYoutubeItem}");
		UICommand.make(form, "update-youtube", messageLocator.getMessage("simplepage.edit"), "#{simplePageBean.updateYoutube}");
		UICommand.make(form, "cancel-youtube", messageLocator.getMessage("simplepage.cancel"), null);
		UIBoundBoolean.make(form, "youtube-prerequisite", "#{simplePageBean.prerequisite}",false);
		
		if(!simplePageBean.isStudentPage(currentPage)) {
			UIOutput.make(form, "editgroups-youtube");
		}
	}

	private void createMovieDialog(UIContainer tofill, SimplePage currentPage) {
		UIOutput.make(tofill, "movie-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edititem_header")));

		UIForm form = UIForm.make(tofill, "movie-form");
		makeCsrf(form, "csrf18");

		UIInput.make(form, "movie-height", "#{simplePageBean.height}");
		UIInput.make(form, "movie-width", "#{simplePageBean.width}");
		UIInput.make(form, "movieEditId", "#{simplePageBean.itemId}");
		UIOutput.make(form, "description3-label", messageLocator.getMessage("simplepage.description_label"));
		UIInput.make(form, "description3", "#{simplePageBean.description}");
		UIInput.make(form, "mimetype4", "#{simplePageBean.mimetype}");

		FilePickerViewParameters fileparams = new FilePickerViewParameters();
		fileparams.setSender(currentPage.getPageId());
		fileparams.setResourceType(true);
		fileparams.viewID = ResourcePickerProducer.VIEW_ID;
		UIInternalLink.make(form, "change-resource-movie", messageLocator.getMessage("simplepage.change_resource"), fileparams);

		fileparams.setCaption(true);
		UIInternalLink.make(form, "change-caption-movie", messageLocator.getMessage("simplepage.change_caption"), fileparams);

		UIBoundBoolean.make(form, "movie-prerequisite", "#{simplePageBean.prerequisite}",false);

		UICommand.make(form, "delete-movie-item", messageLocator.getMessage("simplepage.delete"), "#{simplePageBean.deleteItem}");
		UICommand.make(form, "update-movie", messageLocator.getMessage("simplepage.edit"), "#{simplePageBean.updateMovie}");
		UICommand.make(form, "movie-cancel", messageLocator.getMessage("simplepage.cancel"), null);
	}

	private void createEditTitleDialog(UIContainer tofill, SimplePage page, SimplePageItem pageItem, ContentResource cssLink) {
		if (pageItem.getType() == SimplePageItem.STUDENT_CONTENT)
			UIOutput.make(tofill, "edit-title-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.editTitle")));
		else
			UIOutput.make(tofill, "edit-title-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.title")));

		UIForm form = UIForm.make(tofill, "title-form");
		makeCsrf(form, "csrf14");

		UIOutput.make(form, "pageTitleLabel", messageLocator.getMessage("simplepage.pageTitle_label"));

		// If this is a subpage we don't have to check tool configuration (only top level tool instance can be renamed via Site Info -> Tool Order)
		String effectivePageTitle = page.getTitle();
		if (page.getParent() == null) {
			final String placementId = toolManager.getCurrentPlacement() != null ? toolManager.getCurrentPlacement().getId() : null;
			final SitePage sitePage = simplePageBean.getCurrentSite() != null ? simplePageBean.getCurrentSite().getPage(page.getToolId()) : null;
			String externalPageTitle = null;
			if (sitePage != null && StringUtils.isNotBlank(placementId)) {
				 externalPageTitle = sitePage.getTools().stream()
						.filter(t -> t.getId().equals(placementId))
						.findFirst()
						.map(Placement::getTitle)
						.orElse("");
			}

			effectivePageTitle = StringUtils.defaultIfBlank(externalPageTitle, effectivePageTitle);
		}

		UIInput.make(form, "pageTitle", "#{simplePageBean.pageTitle}", effectivePageTitle);

		if (!simplePageBean.isStudentPage(page)) {
			UIOutput.make(tofill, "hideContainer");
			UIBoundBoolean.make(form, "hide", "#{simplePageBean.hidePage}", (page.isHidden()));

			Date releaseDate = page.getReleaseDate();

			UIBoundBoolean.make(form, "page-releasedate", "#{simplePageBean.hasReleaseDate}", (releaseDate != null));

			String releaseDateString = "";

			if (releaseDate != null) {
			try {
			    releaseDateString = isoDateFormat.format(releaseDate);
			} catch (Exception e) {
			    log.info(e + "bad format releasedate " + releaseDate);
			}
			}
			
			UIOutput releaseForm = UIOutput.make(form, "releaseDate:");
			UIOutput.make(form, "currentReleaseDate", releaseDateString);
			UIInput.make(form, "release_date_string", "#{simplePageBean.releaseDate}" );
			UIOutput.make(form, "release_date");

			if (pageItem.getPageId() == 0) {
			    UIOutput.make(form, "prereqContainer");
			    UIBoundBoolean.make(form, "page-required", "#{simplePageBean.required}", (pageItem.isRequired()));
			    UIBoundBoolean.make(form, "page-prerequisites", "#{simplePageBean.prerequisite}", (pageItem.isPrerequisite()));
			}
		}

		UIOutput gradeBook = UIOutput.make(form, "gradeBookDiv");
		if(simplePageBean.isStudentPage(page) || !simplePageBean.isGradebookExists()) {
			gradeBook.decorate(new UIStyleDecorator("noDisplay"));
		}
		
		UIOutput.make(form, "page-gradebook");
		Double points = page.getGradebookPoints();
		String pointString = "";
		if (points != null) {
			pointString = points.toString();
		}
		
		if(!simplePageBean.isStudentPage(page)) {
			UIOutput.make(form, "csssection");
			ArrayList<ContentResource> sheets = simplePageBean.getAvailableCss();
			String[] options = new String[sheets.size()+2];
			String[] labels = new String[sheets.size()+2];
			
			// Sets up the CSS arrays
			options[0] = null;
			labels[0] = messageLocator.getMessage("simplepage.default-css");
			options[1] = null;
			labels[1] = "----------";
			for(int i = 0; i < sheets.size(); i++) {
				if(sheets.get(i) != null) {
					options[i+2] = sheets.get(i).getId();
					labels[i+2] = sheets.get(i).getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME);
				}else {
					// We show just one un-named separator if there are only site css, or system css, but not both.
					// If we get here, it means we have both, so we name them.
					options[i+2] = null;
					labels[i+2] = "---" + messageLocator.getMessage("simplepage.system") + "---";
					labels[1] = "---" + messageLocator.getMessage("simplepage.site") + "---";
				}
			}
			
			// cssLink is set above to the actual CSS resource that's active for the page
			String currentCss = (cssLink == null ? null : cssLink.getId());

			UIOutput.make(form, "cssDropdownLabel", messageLocator.getMessage("simplepage.css-dropdown-label"));
			UISelect.make(form, "cssDropdown", options, labels, "#{simplePageBean.dropDown}", currentCss);
			
			UIOutput.make(form, "cssDefaultInstructions", messageLocator.getMessage("simplepage.css-default-instructions"));
			UIOutput.make(form, "cssUploadLabel", messageLocator.getMessage("simplepage.css-upload-label"));
			UIOutput.make(form, "cssUpload");
			boolean showSetOwner = ServerConfigurationService.getBoolean("lessonbuilder.show.set.owner", false);
			if (showSetOwner){
				//Set the changeOwner dropdown in the settings dialog
				UIOutput.make(form, "ownerDefaultInstructions", messageLocator.getMessage("simplepage.owner-default-instructions")
						.replace("{1}", messageLocator.getMessage("simplepage.permissions")).replace("{2}", messageLocator.getMessage("simplepage.more-tools")));
				UIOutput.make(form, "changeOwnerSection");
				List<String> roleOptions = new ArrayList<>();
				List<String> roleLabels = new ArrayList<>();
				List<String> possOwners = new LinkedList<>();
				boolean isOwned = page.isOwned();
				String owner = page.getOwner();
				Set<String> siteUsersCanUpdate = simplePageBean.getCurrentSite().getUsersIsAllowed(SimplePage.PERMISSION_LESSONBUILDER_UPDATE);

				// Sort the site member list before filling the "possOwners" list
				List<Member> siteMemberList = new ArrayList<Member>(simplePageBean.getCurrentSite().getMembers());
				Collections.sort(siteMemberList, new Comparator<Member>() {
					public int compare(Member lhs, Member rhs) {
						UserSortNameComparator userComparator = new UserSortNameComparator();
						return userComparator.compare(getUser(lhs.getUserId()), getUser(rhs.getUserId()));
					}
				});
				siteMemberList.forEach(member -> {
					String userId = member.getUserId();
					if (!siteUsersCanUpdate.contains(userId)) {
						possOwners.add(userId);
					}
				});

				if (isOwned){
					if (possOwners.contains(owner)){
						int i = possOwners.indexOf(owner);
						Collections.swap(possOwners, i, 0); // put owner top of list
					}
					else {
						roleOptions.add(owner);
						roleLabels.add(getUserDisplayName(owner));
					}
				}
				else {
					roleOptions.add(null);
					roleLabels.add(messageLocator.getMessage("simplepage.default-user"));
				}
				for(String user : possOwners){
					roleOptions.add(user);
					roleLabels.add(getUserDisplayName(user));
				}
				if (isOwned){
					roleOptions.add(null);
					roleLabels.add( messageLocator.getMessage("simplepage.default-user"));
				}
				UIOutput.make(form, "changeOwnerDropdownLabel", messageLocator.getMessage("simplepage.change-owner-dropdown-label"));
				UISelect.make(form, "changeOwnerDropdown", roleOptions.toArray(new String[roleOptions.size()]), roleLabels.toArray(new String[roleLabels.size()]), "#{simplePageBean.newOwner}", null);
			}
		}
		UIInput.make(form, "page-points", "#{simplePageBean.points}", pointString);

		UICommand.make(form, "create-title", messageLocator.getMessage("simplepage.save"), "#{simplePageBean.editTitle}");
		UICommand.make(form, "cancel-title", messageLocator.getMessage("simplepage.cancel"), "#{simplePageBean.cancel}");
	}

	private void createNewPageDialog(UIContainer tofill, SimplePage page, SimplePageItem pageItem) {
		UIOutput.make(tofill, "new-page-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.new-page")));

		UIForm form = UIForm.make(tofill, "new-page-form");
		makeCsrf(form, "csrf15");

		UIInput.make(form, "newPage", "#{simplePageBean.newPageTitle}");

		UIInput.make(form, "new-page-number", "#{simplePageBean.numberOfPages}");

		UIBoundBoolean.make(form, "new-page-copy", "#{simplePageBean.copyPage}", false);
		
		GeneralViewParameters view = new GeneralViewParameters(PagePickerProducer.VIEW_ID);
		view.setSendingPage(-1L);
		view.newTopLevel = true;
		UIInternalLink.make(tofill, "new-page-choose", messageLocator.getMessage("simplepage.lm_existing_page"), view);

		UICommand.make(form, "new-page-submit", messageLocator.getMessage("simplepage.save"), "#{simplePageBean.addPages}");
		UICommand.make(form, "new-page-cancel", messageLocator.getMessage("simplepage.cancel"), "#{simplePageBean.cancel}");
	}

	private void createRemovePageDialog(UIContainer tofill, SimplePage page, SimplePageItem pageItem) {
		UIOutput.make(tofill, "remove-page-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.remove-page")));
		UIOutput.make(tofill, "remove-page-explanation", 
			      (!simplePageBean.isStudentPage(page) ? messageLocator.getMessage("simplepage.remove-page-explanation") :
			       messageLocator.getMessage("simplepage.remove-student-page-explanation")));

		UIForm form = UIForm.make(tofill, "remove-page-form");
		makeCsrf(form, "csrf16");

		form.addParameter(new UIELBinding("#{simplePageBean.removeId}", page.getPageId()));
		
		//		if (page.getOwner() == null) {
		//		    // top level normal page. Use the remove page producer, which can handle removing tools out from under RSF
		//		    GeneralViewParameters params = new GeneralViewParameters(RemovePageProducer.VIEW_ID);
		//		    UIInternalLink.make(form, "remove-page-submit", "", params).decorate(new UIFreeAttributeDecorator("value", messageLocator.getMessage("simplepage.remove")));
		//		} else
		//		    // a student top level page. call remove page directly, as it will just return to show page
		//		    UICommand.make(form, "remove-page-submit", messageLocator.getMessage("simplepage.remove"), "#{simplePageBean.removePage}");

		
		UIComponent button = UICommand.make(form, "remove-page-submit", messageLocator.getMessage("simplepage.remove"), "#{simplePageBean.removePage}");
		if (!simplePageBean.isStudentPage(page)) // not student page
		    button.decorate(new UIFreeAttributeDecorator("onclick",
			         "window.location='/lessonbuilder-tool/removePage?site=" + simplePageBean.getCurrentSiteId() + 
				 "&page=" + page.getPageId() + "';return false;"));

		UICommand.make(form, "remove-page-cancel", messageLocator.getMessage("simplepage.cancel"), "#{simplePageBean.cancel}");
	}

	private void createCommentsDialog(UIContainer tofill) {
		UIOutput.make(tofill, "comments-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit_commentslink")));

		UIForm form = UIForm.make(tofill, "comments-form");
		makeCsrf(form, "csrf19");

		UIInput.make(form, "commentsEditId", "#{simplePageBean.itemId}");

		UIBoundBoolean.make(form, "comments-anonymous", "#{simplePageBean.anonymous}");

		UIOutput gradeBook = UIOutput.make(form, "gradeBookCommentsDiv");
		if(!simplePageBean.isGradebookExists()) {
			gradeBook.decorate(new UIStyleDecorator("noDisplay"));
		}
		UIBoundBoolean.make(form, "comments-graded", "#{simplePageBean.graded}");
		UIInput.make(form, "comments-max", "#{simplePageBean.maxPoints}");

		UIBoundBoolean.make(form, "comments-required", "#{simplePageBean.required}");
		UIBoundBoolean.make(form, "comments-prerequisite", "#{simplePageBean.prerequisite}");

		LessonConditionUtil.makeConditionPicker(simplePageBean, form, "comments-condition-picker");

		UICommand.make(form, "delete-comments-item", messageLocator.getMessage("simplepage.delete"), "#{simplePageBean.deleteItem}");
		UICommand.make(form, "update-comments", messageLocator.getMessage("simplepage.edit"), "#{simplePageBean.updateComments}");
		UICommand.make(form, "cancel-comments", messageLocator.getMessage("simplepage.cancel"), null);
	}
	
	private void createStudentContentDialog(UIContainer tofill, SimplePage currentPage) {
		UIOutput.make(tofill, "student-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit_studentlink")));

		UIForm form = UIForm.make(tofill, "student-form");
		makeCsrf(form, "csrf20");

		UIInput.make(form, "studentEditId", "#{simplePageBean.itemId}");

		UIBoundBoolean.make(form, "student-anonymous", "#{simplePageBean.anonymous}");
		UIBoundBoolean.make(form, "student-comments", "#{simplePageBean.comments}");
		UIBoundBoolean.make(form, "student-comments-anon", "#{simplePageBean.forcedAnon}");
		UIBoundBoolean.make(form, "student-required", "#{simplePageBean.required}");
		UIBoundBoolean.make(form, "student-prerequisite", "#{simplePageBean.prerequisite}");

		LessonConditionUtil.makeConditionPicker(simplePageBean, form, "student-condition-picker");
		
		UIOutput.make(form, "peer-evaluation-creation");
		
		UIBoundBoolean.make(form, "peer-eval-check", "#{simplePageBean.peerEval}");
		UIInput.make(form, "peer-eval-input-title", "#{simplePageBean.rubricTitle}");
		UIInput.make(form, "peer-eval-input-row", "#{simplePageBean.rubricRow}");

		UIOutput.make(form, "peer_eval_open_date_label", messageLocator.getMessage("simplepage.peer-eval.open_date"));
       
		UIOutput openDateField = UIOutput.make(form, "peer_eval_open_date:");
		UIInput.make(form, "open_date_string", "#{simplePageBean.peerEvalOpenDate}");
		UIOutput.make(form, "open_date_dummy");

		UIOutput.make(form, "peer_eval_due_date_label", messageLocator.getMessage("simplepage.peer-eval.due_date"));
       
		UIOutput dueDateField = UIOutput.make(form, "peer_eval_due_date:");
		UIInput.make(form, "due_date_string", "#{simplePageBean.peerEvalDueDate}");
		UIOutput.make(form, "due_date_dummy");
        
		UIBoundBoolean.make(form, "peer-eval-allow-selfgrade", "#{simplePageBean.peerEvalAllowSelfGrade}");

		UIInput.make(form, "gradebook-title", "#{simplePageBean.gradebookTitle}");

		UIBoundBoolean.make(form, "student-graded", "#{simplePageBean.graded}");
		UIInput.make(form, "student-max", "#{simplePageBean.maxPoints}");
		
		UIOutput gradeBook = UIOutput.make(form, "gradeBookStudentsDiv");
		UIOutput gradeBook2 = UIOutput.make(form, "gradeBookStudentCommentsDiv");
		if(!simplePageBean.isGradebookExists()) {
			gradeBook.decorate(new UIStyleDecorator("noDisplay"));
			gradeBook2.decorate(new UIStyleDecorator("noDisplay"));
		}
		UIBoundBoolean.make(form, "student-comments-graded", "#{simplePageBean.sGraded}");
		UIInput.make(form, "student-comments-max", "#{simplePageBean.sMaxPoints}");

		UIBoundBoolean.make(form, "student-group-owned", "#{simplePageBean.groupOwned}");
		createGroupList(form, null, "student-", "#{simplePageBean.studentSelectedGroups}");

		UIBoundBoolean.make(form, "student-group-owned-eval-individual", "#{simplePageBean.groupOwnedIndividual}");
		UIBoundBoolean.make(form, "student-group-owned-see-only-own", "#{simplePageBean.seeOnlyOwn}");

		UICommand.make(form, "delete-student-item", messageLocator.getMessage("simplepage.delete"), "#{simplePageBean.deleteItem}");
		UICommand.make(form, "update-student", messageLocator.getMessage("simplepage.edit"), "#{simplePageBean.updateStudent}");
		UICommand.make(form, "cancel-student", messageLocator.getMessage("simplepage.cancel"), null);

		// RU Rubrics
		UIOutput.make(tofill, "peer-eval-create-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.peer-eval-create-title")));
	}
	
	private void createQuestionDialog(UIContainer tofill, SimplePage currentPage) {
		UIOutput.make(tofill, "question-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.edit_questionlink")));
		
		UIForm form = UIForm.make(tofill, "question-form");
		makeCsrf(form, "csrf21");
		
		UISelect questionType = UISelect.make(form, "question-select", new String[] {"multipleChoice", "shortanswer"}, "#{simplePageBean.questionType}", "");
		UISelectChoice.make(form, "multipleChoiceSelect", questionType.getFullID(), 0);
		UISelectChoice.make(form, "shortanswerSelect", questionType.getFullID(), 1);

		UIOutput.make(form, "question-shortans-del").decorate(new UIFreeAttributeDecorator("alt", messageLocator.getMessage("simplepage.delete")));
		UIOutput.make(form, "question-mc-del").decorate(new UIFreeAttributeDecorator("alt", messageLocator.getMessage("simplepage.delete")));
		UIInput.make(form, "questionEditId", "#{simplePageBean.itemId}");
		
		UIBoundBoolean.make(form, "question-required", "#{simplePageBean.required}");
		UIBoundBoolean.make(form, "question-prerequisite", "#{simplePageBean.prerequisite}");

		UIInput questionInput = UIInput.make(form, "question-text-area-evolved:", "#{simplePageBean.questionText}");
		questionInput.decorate(new UIFreeAttributeDecorator("height", "175"));
		questionInput.decorate(new UIFreeAttributeDecorator("width", "800"));
		questionInput.decorate(new UIFreeAttributeDecorator("aria-label", messageLocator.getMessage("simplepage.editor")));
		questionInput.decorate(new UIStyleDecorator("using-editor"));  // javascript needs to know
		((SakaiFCKTextEvolver) richTextEvolver).evolveTextInput(questionInput, "1");

		UIInput.make(form, "question-answer-full-shortanswer", "#{simplePageBean.questionAnswer}");

		LessonConditionUtil.makeConditionPicker(simplePageBean, form, "question-condition-picker");

		UIOutput gradeBook = UIOutput.make(form, "gradeBookQuestionsDiv");
		if(!simplePageBean.isGradebookExists()) {
			gradeBook.decorate(new UIStyleDecorator("noDisplay"));
		}
		UIBoundBoolean.make(form, "question-graded", "#{simplePageBean.graded}");
		UIInput.make(form, "question-gradebook-title", "#{simplePageBean.gradebookTitle}");
		UIInput.make(form, "question-max", "#{simplePageBean.maxPoints}");

		UIInput.make(form, "multi_gradebook", String.valueOf(gradebookIfc.isGradebookGroupEnabled(simplePageBean.getCurrentSiteId())));

		UIInput.make(form, "question-multiplechoice-answer-complete", "#{simplePageBean.addAnswerData}");
		UIInput.make(form, "question-multiplechoice-answer-id", null);
		UIBoundBoolean.make(form, "question-multiplechoice-answer-correct");
		UIInput.make(form, "question-multiplechoice-answer", null);
		UIBoundBoolean.make(form, "question-show-poll", "#{simplePageBean.questionShowPoll}");
		
		UIInput.make(form, "question-correct-text", "#{simplePageBean.questionCorrectText}");
		UIInput.make(form, "question-incorrect-text", "#{simplePageBean.questionIncorrectText}");
		UIInput.make(form, "question-addBefore", "#{simplePageBean.addBefore}");
		
		UICommand.make(form, "delete-question-item", messageLocator.getMessage("simplepage.delete"), "#{simplePageBean.deleteItem}");
		UICommand.make(form, "update-question", messageLocator.getMessage("simplepage.save"), "#{simplePageBean.updateQuestion}");
		UICommand.make(form, "cancel-question", messageLocator.getMessage("simplepage.cancel"), null);
	}

	private void createLayoutDialog(UIContainer tofill, SimplePage currentPage) {
		UIOutput.make(tofill, "layout-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.add_layout")));

		UIForm form = UIForm.make(tofill, "layout-form");
		makeCsrf(form, "csrf28");

		UIInput input = UIInput.make(form, "layout-section-title", "#{simplePageBean.layoutSectionTitle}");
		input.decorate(new UIFreeAttributeDecorator("placeholder", messageLocator.getMessage("simplepage.layout.section.title")));

		UISelect colorSchemes = UISelect.make(form, "layout-color-scheme", SimplePageBean.NewColors, simplePageBean.getNewColorLabelsI18n(), "#{simplePageBean.layoutColorScheme}", SimplePageBean.NewColors[0]);

		UIBoundBoolean.make(form, "layout-section-collapsible", "#{simplePageBean.layoutSectionCollapsible}", false);
		UIBoundBoolean.make(form, "layout-section-start-collapsed", "#{simplePageBean.layoutSectionStartCollapsed}", false);
		UIBoundBoolean.make(form, "layout-section-show-borders", "#{simplePageBean.layoutSectionShowBorders}", true);
		UIBoundBoolean.make(form, "layout-section-force-button-color", "#{simplePageBean.forceButtonColor}", false);

		UISelect layouts = UISelect.make(form, "layout-select-layout",
				new String[] {"single-column", "two-equal", "left-double", "right-double", "three-equal"},
				"#{simplePageBean.layoutSelect}", "single-column");
		UISelectChoice.make(form, "layout-single", layouts.getFullID(), 0);
		UISelectChoice.make(form, "layout-two-equal", layouts.getFullID(), 1);
		UISelectChoice.make(form, "layout-left-double", layouts.getFullID(), 2);
		UISelectChoice.make(form, "layout-right-double", layouts.getFullID(), 3);
		UISelectChoice.make(form, "layout-three-equal", layouts.getFullID(), 4);

		UICommand.make(form, "layout-submit", messageLocator.getMessage("simplepage.add_layout"), "#{simplePageBean.addLayout}");
		UICommand.make(form, "layout-cancel", messageLocator.getMessage("simplepage.cancel"), "#{simplePageBean.cancel}");
		createPageLayoutTab(tofill);
	}

	private void createPageLayoutTab(UIContainer tofill){
		String pageLayoutLabels[] = {"", messageLocator.getMessage("simplepage.layout.page.menuSubpage"), messageLocator.getMessage("simplepage.layout.page.menuResources"), messageLocator.getMessage("simplepage.layout.page.menuTasks")};	//populate dropdown labels from properties
		UIForm form2 = UIForm.make(tofill, "page-form");
		makeCsrf(form2, "page-csrf28");
		String subpageCountValues[] = new String[20];
		for (int count=0; count<20; count++){	//make array of Strings for the number of subpages/tasks to create
			subpageCountValues[count] = String.valueOf(count+1);
		}
		UIOutput.make(form2,"page-preview-subpage-image").decorate(new UIFreeAttributeDecorator("src",ServerConfigurationService.getServerUrl() + "/library/image/lessons/preview-subpage-layout.png"));
		UIOutput.make(form2,"page-preview-subpage").decorate(new UIFreeAttributeDecorator("href",ServerConfigurationService.getServerUrl() + "/library/image/lessons/preview-subpage-layout.png")).decorate(new UIFreeAttributeDecorator("target","_blank"));
		UIOutput.make(form2,"page-preview-resource-image").decorate(new UIFreeAttributeDecorator("src",ServerConfigurationService.getServerUrl() + "/library/image/lessons/preview-resource-layout.png"));
		UIOutput.make(form2,"page-preview-resource").decorate(new UIFreeAttributeDecorator("href",ServerConfigurationService.getServerUrl() + "/library/image/lessons/preview-resource-layout.png")).decorate(new UIFreeAttributeDecorator("target","_blank"));
		UIOutput.make(form2,"page-preview-task-image").decorate(new UIFreeAttributeDecorator("src",ServerConfigurationService.getServerUrl() + "/library/image/lessons/preview-task-layout.png"));
		UIOutput.make(form2,"page-preview-task").decorate(new UIFreeAttributeDecorator("href",ServerConfigurationService.getServerUrl() + "/library/image/lessons/preview-task-layout.png")).decorate(new UIFreeAttributeDecorator("target","_blank"));
		UISelect.make(form2, "page-dropdown", SimplePageBean.pageLayoutValues, pageLayoutLabels, "#{simplePageBean.pageLayoutSelect}", SimplePageBean.pageLayoutValues[0]);
		UISelect.make(form2, "page-color-scheme", SimplePageBean.NewColors, simplePageBean.getNewColorLabelsI18n(), "#{simplePageBean.pageButtonColorScheme}", SimplePageBean.NewColors[0]);
		UISelect.make(form2, "page-color-scheme-3", SimplePageBean.NewColors, simplePageBean.getNewColorLabelsI18n(), "#{simplePageBean.pageColorScheme}", SimplePageBean.NewColors[0]);
		UIBoundBoolean.make(form2,"page-subpage-button","#{simplePageBean.pageSubpageButton}",true);
		UIInput.make(form2, "page-option-subpage-title", "#{simplePageBean.pageSubpageTitle}");
		UISelect.make(form2, "page-option-subpage-count", subpageCountValues,subpageCountValues, "#{simplePageBean.pageSubpageCount}", subpageCountValues[1]);
		UISelect.make(form2, "page-option-task-count", subpageCountValues, subpageCountValues, "#{simplePageBean.pageTaskCount}", subpageCountValues[1]);
		UIBoundBoolean.make(form2, "page-option-task-collapsible", "#{simplePageBean.pageTaskCollapsible}",false);
		UIBoundBoolean.make(form2, "page-option-task-closed", "#{simplePageBean.pageTaskClosed}",false);
		UICommand.make(form2, "page-submit", messageLocator.getMessage("simplepage.add_layout"), "#{simplePageBean.addPageLayout}");
		UICommand.make(form2, "page-cancel", messageLocator.getMessage("simplepage.cancel"), "#{simplePageBean.cancel}");
	}

	private void createDeleteItemDialog(UIContainer tofill, SimplePage currentPage) {
		UIForm form = UIForm.make(tofill, "delete-item-form");
		makeCsrf(form, "csrf22");
		UIInput.make(form, "delete-item-itemid", "#{simplePageBean.itemId}");
		UICommand.make(form, "delete-item-button", "#{simplePageBean.deleteItem}");
	}

	private void createColumnDialog(UIContainer tofill, SimplePage currentPage) {
		UIOutput.make(tofill, "column-dialog").decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.update.settings")));
		UIForm form = UIForm.make(tofill, "column-dialog-form");
		UICommand.make(form, "column-submit", messageLocator.getMessage("simplepage.save"), null);
		UICommand.make(form, "column-cancel", messageLocator.getMessage("simplepage.cancel"), null);
	}


	/*
	 * return true if the item is required and not completed, i.e. if we need to
	 * update the status after the user views the item
	 */
	private Status handleStatusIcon(UIContainer container, SimplePageItem i) {
		if (i.getType() != SimplePageItem.TEXT && i.getType() != SimplePageItem.MULTIMEDIA) {
			if (!i.isRequired()) {
				addStatusIcon(Status.NOT_REQUIRED, container, "status");
				return Status.NOT_REQUIRED;
			} else if (simplePageBean.isItemComplete(i)) {
				addStatusIcon(Status.COMPLETED, container, "status");
				return Status.COMPLETED;
			} else {
				addStatusIcon(Status.REQUIRED, container, "status");
				return Status.REQUIRED;
			}
		}
		return Status.NOT_REQUIRED;
	}
	
	/**
	 * Returns a Status object with the status of a user's response to a question.
	 * For showing status images next to the question.
	 */
	private Status getQuestionStatus(SimplePageItem question, SimplePageQuestionResponse response) {
		String questionType = question.getAttribute("questionType");
		boolean noSpecifiedAnswers = false;
		boolean manuallyGraded = false;

		if ("multipleChoice".equals(questionType) &&
		    !simplePageToolDao.hasCorrectAnswer(question))
		    noSpecifiedAnswers = true;
		else if ("shortanswer".equals(questionType) &&
			 "".equals(question.getAttribute("questionAnswer")))
		    noSpecifiedAnswers = true;

		if (noSpecifiedAnswers && "true".equals(question.getAttribute("questionGraded")))
		    manuallyGraded = true;

		if (noSpecifiedAnswers && !manuallyGraded) {
		    // poll. should we show completed if not required? Don't for
		    // other item types, but here there's no separate tool where you
		    // can look at the status. I'm currently showing completed, to
		    // be consistent with non-polls, where I always show a result
		    if (response != null) {
			    return response.isCorrect()?Status.COMPLETED:Status.FAILED;
		    }
		    if(question.isRequired())
			return Status.REQUIRED;
		    return Status.NOT_REQUIRED;
		}

		if (manuallyGraded && (response != null && !response.isOverridden())) {
			return Status.NEEDSGRADING;
		} else if (response != null && response.isCorrect()) {
			return Status.COMPLETED;
		} else if (response != null && !response.isCorrect()) {
			return Status.FAILED;			
		}else if(question.isRequired()) {
			return Status.REQUIRED;
		}else {
			return Status.NOT_REQUIRED;
		}
	}

	private String getStatusNote(Status status) {
		if (status == Status.COMPLETED)
			return messageLocator.getMessage("simplepage.status.completed");
		else if (status == Status.REQUIRED)
			return messageLocator.getMessage("simplepage.status.required");
		else if (status == Status.NEEDSGRADING)
			return messageLocator.getMessage("simplepage.status.needsgrading");
		else if (status == Status.FAILED)
			return messageLocator.getMessage("simplepage.status.failed");
		else
		return null;
	}

	private void addStatusIcon(Status status, UIContainer container, String iconId) {
		String iconClass = "fa fa-";
		String title;
		switch (status) {
			case COMPLETED:
				iconClass += "check";
				title = messageLocator.getMessage("simplepage.status.completed");
				break;
			case DISABLED:
				iconClass += "circle-o";
				title = messageLocator.getMessage("simplepage.status.disabled");
				break;
			case FAILED:
				iconClass += "times";
				title = messageLocator.getMessage("simplepage.status.failed");
				break;
			case REQUIRED:
				iconClass += "asterisk";
				title = messageLocator.getMessage("simplepage.status.required");
				break;
			case NEEDSGRADING:
				iconClass += "question";
				title = messageLocator.getMessage("simplepage.status.needsgrading");
				break;
			case NOT_REQUIRED:
				iconClass = "";
				title = "";
				break;
			default:
				iconClass = "";
				title = "";
				break;
		}
		UIOutput.make(container, "status-td");
		UIOutput.make(container, iconId).decorate(new UIStyleDecorator(iconClass)).decorate(new UIFreeAttributeDecorator("title", title));
	}

	private String getLocalizedURL(String fileName, boolean useDefault) {

		if (fileName == null || fileName.trim().length() == 0)
			return fileName;
		else {
			fileName = fileName.trim();
		}

		Locale locale = new ResourceLoader().getLocale();

		String helploc = ServerConfigurationService.getString("lessonbuilder.helpfolder", null);

		// we need to test the localized URL and return the initial one if it
		// doesn't exists
		// defaultPath will be the one to use if the localized one doesn't exist
		String defaultPath = null;
		// this is the part up to where we add the locale
		String prefix = null;
		// this is the part after the locale
		String suffix = null;
		// this is an additional prefix needed to make a full URL, for testing
		String testPrefix = null;

		int suffixIndex = fileName.lastIndexOf(".");
		if (suffixIndex >= 0) {
			prefix = fileName.substring(0, suffixIndex);
			suffix = fileName.substring(suffixIndex);
		} else {
			prefix = fileName;
			suffix = "";
		}

		// if user specified, we make up an absolute URL
		// otherwise use one relative to the servlet context
		if (helploc != null) {
			// user has specified a base URL. Will be absolute, but may not have
			// http and hostname
			defaultPath = helploc + fileName;
			prefix = helploc + prefix;
			if (helploc.startsWith("http:") || helploc.startsWith("https:")) {
				testPrefix = ""; // absolute, can test as is
			} else {
				testPrefix = myUrl(); // relative, need to make absolute
			}
		} else {
			// actual URL will be related to templates
			defaultPath = "/lessonbuilder-tool/templates/instructions/" + fileName;
			prefix = "/lessonbuilder-tool/templates/instructions/" + prefix;
			// but have to test relative to servlet base
			testPrefix = "";  // urlok will have to remove /lessonbuilder-tool
		}

		String[] localeDetails = locale.toString().split("_");
		int localeSize = localeDetails.length;

		String filePath = null;
		String localizedPath = null;

		if (localeSize > 2) {
			localizedPath = prefix + "_" + locale.toString() + suffix;
			filePath = testPrefix + localizedPath;
			if (UrlOk(filePath))
				return localizedPath;
		}

		if (localeSize > 1) {
			localizedPath = prefix + "_" + locale.getLanguage() + "_" + locale.getCountry() + suffix;
			filePath = testPrefix + localizedPath;
			if (UrlOk(filePath))
				return localizedPath;
		}

		if (localeSize > 0) {
			localizedPath = prefix + "_" + locale.getLanguage() + suffix;
			filePath = testPrefix + localizedPath;
			if (UrlOk(filePath))
				return localizedPath;
		}

		if (useDefault)
		    return defaultPath;

		// no localized version available
		return null;

	}

    // this can be either a fully specified URL starting with http: or https: 
    // or something relative to the servlet base, e.g. /lessonbuilder-tool/template/instructions/general.html
	private boolean UrlOk(String url) {
		String origurl = url;
		Boolean cached = (Boolean) urlCache.get(url);
		if (cached != null)
		    return (boolean) cached;

		if (url.startsWith("http:") || url.startsWith("https:")) {
		    // actual URL, check it out

		    try {
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod("HEAD");
			con.setConnectTimeout(30 * 1000);
			boolean ret = (con.getResponseCode() == HttpURLConnection.HTTP_OK);
			urlCache.put(origurl, (Boolean) ret);
			return ret;
		    } catch (java.net.SocketTimeoutException e) {
			log.error("Internationalization url lookup timed out for " + url + ": Please check lessonbuilder.helpfolder. It appears that the host specified is not responding.");
			urlCache.put(origurl, (Boolean) false);
			return false;
		    } catch (ProtocolException e) {
			urlCache.put(origurl, (Boolean) false);
			return false;
		    } catch (IOException e) {
			urlCache.put(origurl, (Boolean) false);
			return false;
		    }
		} else {
		    // remove the leading /lessonbuilder-tool, since getresource is
		    // relative to the top of the servlet
		    int i = url.indexOf("/", 1);
		    url = url.substring(i);
		    try {
			// inside the war file, check the file system. That avoid issues
			// with odd deployments behind load balancers, where the user's URL may not
			// work from one of the front ends
			if (httpServletRequest.getSession().getServletContext().getResource(url) == null) {
			    urlCache.put(origurl, (Boolean) false);
			    return false;
			} else {
			    urlCache.put(origurl, (Boolean) true);
			    return true;
			}
		    } catch (Exception e) {  // probably malfformed url
			log.error("Internationalization url lookup failed for " + url + ": " + e);
			urlCache.put(origurl, (Boolean) true);
			return true;
		    }

		}
	}

	private long findMostRecentComment() {
		List<SimplePageComment> comments = simplePageToolDao.findCommentsOnPageByAuthor(simplePageBean.getCurrentPage().getPageId(), UserDirectoryService.getCurrentUser().getId());

		Collections.sort(comments, new Comparator<SimplePageComment>() {
			public int compare(SimplePageComment c1, SimplePageComment c2) {
				return c1.getTimePosted().compareTo(c2.getTimePosted());
			}
		});

		if (comments.size() > 0)
			return comments.get(comments.size() - 1).getId();
		else
			return -1;
	}

	private boolean printedGradingForm = false;
	private void printGradingForm(UIContainer tofill) {
		// Ajax grading form so faculty can grade comments
		if(!printedGradingForm) {
			UIForm gradingForm = UIForm.make(tofill, "gradingForm");
			gradingForm.viewparams = new SimpleViewParameters(UVBProducer.VIEW_ID);
			UIInput idInput = UIInput.make(gradingForm, "gradingForm-id", "gradingBean.id");
			UIInput jsIdInput = UIInput.make(gradingForm, "gradingForm-jsId", "gradingBean.jsId");
			UIInput pointsInput = UIInput.make(gradingForm, "gradingForm-points", "gradingBean.points");
			UIInput typeInput = UIInput.make(gradingForm, "gradingForm-type", "gradingBean.type");
			Object sessionToken = SessionManager.getCurrentSession().getAttribute("sakai.csrf.token");
			UIInput csrfInput = UIInput.make(gradingForm, "csrf", "gradingBean.csrfToken", (sessionToken == null ? "" : sessionToken.toString()));
			UIInitBlock.make(tofill, "gradingForm-init", "initGradingForm", new Object[] {idInput, pointsInput, jsIdInput, typeInput, csrfInput, "gradingBean.results"});
			printedGradingForm = true;
		}
	}

	private boolean saveChecklistFormNeeded = false;
	private void makeSaveChecklistForm(UIContainer tofill) {
		// Ajax grading form so faculty can grade comments
		if(!saveChecklistFormNeeded) {
			UIForm saveChecklistForm = UIForm.make(tofill, "saveChecklistForm");
			saveChecklistForm.viewparams = new SimpleViewParameters(UVBProducer.VIEW_ID);
			UIInput checklistIdInput = UIInput.make(saveChecklistForm, "saveChecklistForm-checklistId", "checklistBean.checklistId");
			UIInput checklistItemIdInput = UIInput.make(saveChecklistForm, "saveChecklistForm-checklistItemIdInput", "checklistBean.checklistItemId");
			UIInput checklistItemDone = UIInput.make(saveChecklistForm, "saveChecklistForm-checklistItemDone", "checklistBean.checklistItemDone");
			Object sessionToken = SessionManager.getCurrentSession().getAttribute("sakai.csrf.token");
			String sessionTokenString = null;
			if (sessionToken != null)
				sessionTokenString = sessionToken.toString();
			UIInput checklistCsrfInput = UIInput.make(saveChecklistForm, "saveChecklistForm-csrf", "checklistBean.csrfToken", sessionTokenString);

			UIInitBlock.make(tofill, "saveChecklistForm-init", "checklistDisplay.initSaveChecklistForm", new Object[] {checklistIdInput, checklistItemIdInput, checklistItemDone, checklistCsrfInput, "checklistBean.results"});
			saveChecklistFormNeeded = true;
		}
	}
	
	private String getItemPath(SimplePageItem i)
	{

	    // users seem to want paths for the embedded items, so they can see what's going on
	        if (i.getType() == SimplePageItem.MULTIMEDIA) {
		    String mmDisplayType = i.getAttribute("multimediaDisplayType");
		    if ("".equals(mmDisplayType) || "2".equals(mmDisplayType))
			mmDisplayType = null;
		    if ("1".equals(mmDisplayType)) {
			// embed code
			return formattedText.escapeHtml(i.getAttribute("multimediaEmbedCode"),false);
		    } else if ("3".equals(mmDisplayType)) {
			// oembed
			return formattedText.escapeHtml(i.getAttribute("multimediaUrl"),false);
		    } else if ("4".equals(mmDisplayType)) {
			// iframe
			return formattedText.escapeHtml(i.getItemURL(simplePageBean.getCurrentSiteId(),simplePageBean.getCurrentPage().getOwner()),false);
		    }
		}		

		String itemPath = "";
		boolean isURL = false;
		String pathId = i.getType() == SimplePageItem.MULTIMEDIA ? "path-url":"path-url";
		String[] itemPathTokens = i.getSakaiId().split("/");
		for(int tokenIndex=3 ; tokenIndex < itemPathTokens.length ; tokenIndex++)
		{
			if(isURL)
			{
				itemPath+= "/<a target=\"_blank\" href=\"\" class=\"" + URLEncoder.encode(pathId) + "\">" + formattedText.escapeHtml(itemPathTokens[tokenIndex],false) + "</a>";
				isURL = false;
			}
			else
			    itemPath+="/" + formattedText.escapeHtml(itemPathTokens[tokenIndex],false);
			
			isURL = itemPathTokens[tokenIndex].equals("urls") ? true: false;
		}
		return itemPath;
	}
	
	//Output rubric data for a Student Content box. 
	private String[] makeStudentRubric  = {null, "peer-eval-row-student:", "peerReviewIdStudent", "peerReviewTextStudent", "peer-eval-row-data", "#{simplePageBean.rubricPeerGrade}"};
	private String[] makeMaintainRubric = {"peer-eval-title", 		 "peer-eval-row:", 		  "peerReviewId", 		 "peerReviewText", null, null};
	
	private void makePeerRubric(UIContainer parent, SimplePageItem i, String[] peerReviewRsfIds, Map<Long,Integer> selectedCells, Map<Long, Map<Integer, Integer>> dataMap, boolean allowSubmit)
	{
		//log.info("makePeerRubric(): i.getAttributesString() " + i.getAttributeString());
		//log.info("makePeerRubric(): i.getAttribute(\"rubricTitle\") " + i.getAttribute("rubricTitle"));
		//log.info("makePeerRubric(): i.getJsonAttribute(\"rows\") " + i.getJsonAttribute("rows"));
		
		if (peerReviewRsfIds[0] != null)
		    UIOutput.make(parent, peerReviewRsfIds[0], String.valueOf(i.getAttribute("rubricTitle")));
		
		class RubricRow implements Comparable{
			public Long id;
			public String text;
			public RubricRow(Long id, String text){ this.id=id; this.text=text;}
			public int compareTo(Object o){
				RubricRow r = (RubricRow)o;
				if(id==r.id)
					return 0;
				if(id>r.id)
					return 1;
				return -1;
			}
		}
		
		ArrayList<RubricRow> rows = new ArrayList<RubricRow>();
		List categories = (List) i.getJsonAttribute("rows");
		if(categories != null){
			for(Object o: categories){
				Map cat = (Map)o;
				Long rowId = Long.parseLong(String.valueOf(cat.get("id")));
				String rowText = String.valueOf(cat.get("rowText"));
				rows.add(new RubricRow(rowId, rowText));
			}
		}
		//else{log.info("This rubric has no rows.");}
		
		Collections.sort(rows);

		for(RubricRow row : rows){
			UIBranchContainer peerReviewRows = UIBranchContainer.make(parent, peerReviewRsfIds[1]);
			UIOutput.make(peerReviewRows, peerReviewRsfIds[2], String.valueOf(row.id));
			UIOutput.make(peerReviewRows, peerReviewRsfIds[3], row.text);
			if (allowSubmit && peerReviewRsfIds[4] != null)
			    UIInput.make(peerReviewRows, peerReviewRsfIds[4], peerReviewRsfIds[5]);
			if (selectedCells != null) {
			    for (int col = 4; col >= 0; col--) {
				String count = null;
				if (dataMap != null) {
				    Map<Integer, Integer>rowMap = dataMap.get(row.id);
				    if (rowMap != null && rowMap.get(col) != null)
					count = rowMap.get(col).toString();
				}

				UIComponent cell = UIOutput.make(peerReviewRows, "peer-eval-cell:", count);
				Integer selectedValue = selectedCells.get(row.id);

				if (selectedValue != null && selectedValue == col)
				    cell.decorate(new UIStyleDecorator("selectedPeerCell " + col));
				else
				    cell.decorate(new UIStyleDecorator("" + col));
			    }						  
			}			    
		}
	}
	
	private int colCount(List<SimplePageItem> items, long item) {
	    // if item = we're at beginning. start counting immediately
	    boolean found = (item == 0);
	    int cols = 1;
	    for (SimplePageItem i: items) {
		if (i.getId() == item) {
		    String width = i.getAttribute("colwidth");
		    if (width != null)
			cols += (new Integer(width)) - 1;
		    found = true;
		    continue;
		}
		if (found && i.getType() == SimplePageItem.BREAK) {
		    if ("column".equals(i.getFormat())) {
			cols++;
			String width = i.getAttribute("colwidth");
			if (width != null)
			    cols += (new Integer(width)) - 1;
		    } else // section break; in next section. we're done
			break;
		}
	    }
	    return cols;
	}

	private void makeSamplePeerEval(UIContainer parent)
	{
		UIOutput.make(parent, "peer-eval-sample-title", messageLocator.getMessage("simplepage.peer-eval.sample.title"));
		
		UIBranchContainer peerReviewRows = UIBranchContainer.make(parent, "peer-eval-sample-data:");
		UIOutput.make(peerReviewRows, "peer-eval-sample-id", "1");
		UIOutput.make(peerReviewRows, "peer-eval-sample-text", messageLocator.getMessage("simplepage.peer-eval.sample.1"));
		
		peerReviewRows = UIBranchContainer.make(parent, "peer-eval-sample-data:");
		UIOutput.make(peerReviewRows, "peer-eval-sample-id", "2");
		UIOutput.make(peerReviewRows, "peer-eval-sample-text", messageLocator.getMessage("simplepage.peer-eval.sample.2"));
		
		peerReviewRows = UIBranchContainer.make(parent, "peer-eval-sample-data:");
		UIOutput.make(peerReviewRows, "peer-eval-sample-id", "3");
		UIOutput.make(peerReviewRows, "peer-eval-sample-text", messageLocator.getMessage("simplepage.peer-eval.sample.3"));
		
		peerReviewRows = UIBranchContainer.make(parent, "peer-eval-sample-data:");
		UIOutput.make(peerReviewRows, "peer-eval-sample-id", "4");
		UIOutput.make(peerReviewRows, "peer-eval-sample-text", messageLocator.getMessage("simplepage.peer-eval.sample.4"));
	}

	private String personalizeText(String itemText) {
		if (StringUtils.isNotBlank(itemText)) {
			User currentUser = UserDirectoryService.getCurrentUser();
			itemText = StringUtils.replace(itemText, "{{firstname}}", currentUser.getFirstName() == null ? "" : currentUser.getFirstName());
			itemText = StringUtils.replace(itemText, "{{lastname}}", currentUser.getLastName() == null ? "" : currentUser.getLastName());
			itemText = StringUtils.replace(itemText, "{{fullname}}", currentUser.getDisplayName() == null ? "" : currentUser.getDisplayName());
		}
		return itemText;
	}

	public List<Long> getPrintedSubpages() {
		return printedSubpages;
	}

	public void setPrintedSubpages(List<Long> printedSubpages) {
		this.printedSubpages = printedSubpages;
	}

	private String buildStudentPageTitle(SimplePageItem item, String pageTitle, String groupId, String ownerId, boolean isOwner, boolean canEditPage) {
		String title = pageTitle;
		String ownerName = "";
		if (!item.isAnonymous() || canEditPage) {
			if (groupId != null) {
				Group g = simplePageBean.getCurrentSite().getGroup(groupId);
				ownerName = g != null ? g.getTitle() : messageLocator.getMessage("simplepage.student-group-deleted");
			} else {
				try {
					ownerName = UserDirectoryService.getUser(ownerId).getDisplayName();
				} catch (UserNotDefinedException e) {
					ownerName = messageLocator.getMessage("simplepage.student-user-deleted");
				}
			}
		} else if (isOwner) {
			ownerName = messageLocator.getMessage("simplepage.comment-you");
		}

		if (!ownerName.isEmpty() && !ownerName.equals(title)) {
			title += " (" + ownerName + ")";
		}

		return title;
	}
}
