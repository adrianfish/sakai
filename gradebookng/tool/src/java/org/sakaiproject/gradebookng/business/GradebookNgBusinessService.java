/**
 * Copyright (c) 2003-2017 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.gradebookng.business;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Application;

import org.sakaiproject.assignment.api.AssignmentConstants;
import org.sakaiproject.assignment.api.AssignmentReferenceReckoner;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.model.AssignmentSubmission;
import org.sakaiproject.authz.api.GroupProvider;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityAdvisor.SecurityAdvice;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.Enrollment;
import org.sakaiproject.coursemanagement.api.EnrollmentSet;
import org.sakaiproject.coursemanagement.api.Membership;
import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.coursemanagement.api.exception.IdNotFoundException;
import org.sakaiproject.section.api.coursemanagement.CourseSection;
import org.sakaiproject.section.api.coursemanagement.EnrollmentRecord;
import org.sakaiproject.section.api.facade.Role;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.gradebookng.business.importExport.CommentValidator;
import org.sakaiproject.gradebookng.business.model.*;
import org.sakaiproject.gradebookng.business.util.EventHelper;
import org.sakaiproject.gradebookng.business.util.GbStopWatch;
import org.sakaiproject.grading.api.*;
import org.sakaiproject.grading.api.model.Gradebook;
import org.sakaiproject.grading.api.model.GradingEvent;
import org.sakaiproject.rubrics.api.RubricsConstants;
import org.sakaiproject.rubrics.api.RubricsService;
import org.sakaiproject.section.api.SectionManager;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tasks.api.Priorities;
import org.sakaiproject.tasks.api.Task;
import org.sakaiproject.tasks.api.TaskService;
import org.sakaiproject.time.api.UserTimeService;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.CandidateDetailProvider;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.api.FormattedText;
import org.sakaiproject.util.comparator.UserSortNameComparator;

/**
 * Business service for GradebookNG
 *
 * This is not designed to be consumed outside of the application or supplied entityproviders. Use at your own risk.
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */

// TODO add permission checks! Remove logic from entityprovider if there is a
// double up
// TODO some of these methods pass in empty lists and its confusing. If we
// aren't doing paging, remove this.

@Slf4j
public class GradebookNgBusinessService {

	@Setter
	private AssignmentService assignmentService;

	@Setter
	private SiteService siteService;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Setter @Getter
	private ServerConfigurationService serverConfigService;

	@Setter
	private ToolManager toolManager;

	@Setter
	private GradingService gradingService;

	@Setter
	private GradingPermissionService gradingPermissionService;

	@Setter
	private SectionManager sectionManager;

	@Setter
	private CourseManagementService courseManagementService;

	@Setter
	private GroupProvider groupProvider;

	@Setter
	private SecurityService securityService;

	@Setter
	private RubricsService rubricsService;
	
	@Setter
	private FormattedText formattedText;

	@Setter
	private FormatHelper formatHelper;

	@Setter
	private UserTimeService userTimeService;
	
	@Setter
	private TaskService taskService;
	
	public static final String ASSIGNMENT_ORDER_PROP = "gbng_assignment_order";
	public static final String ICON_SAKAI = "si si-";
	public static final String ALL = "all";

	private static final String SAK_PROP_ALLOW_STUDENTS_TO_COMPARE_GRADES = "gradebookng.allowStudentsToCompareGradesWithClassmates";
	private static final Boolean SAK_PROP_ALLOW_STUDENTS_TO_COMPARE_GRADES_DEFAULT = false;

	/**
	 * Get a list of all users in the given site, filtered by the given group, that can have grades
	 *
	 * @param siteId the id of the site to lookup
	 * @param groupFilter Group to filter on
	 *
	 * @return a list of users as uuids or null if none
	 */
	public List<String> getGradeableUsers(String gradebookUid, String siteId, String groupFilter) {
        return gradingService.getGradeableUsers(gradebookUid, siteId, groupFilter);
	}

	/**
	 * Given a list of uuids, get a list of Users
	 *
	 * @param userUuids list of user uuids
	 * @return a List of full User objects
	 */
	public List<User> getUsers(final List<String> userUuids) throws GbException {
		try {
			final List<User> users = this.userDirectoryService.getUsers(userUuids);
			users.sort(new UserSortNameComparator()); // TODO: remove this sort, it causes double sorting in various scenarios
			return users;
		} catch (final RuntimeException e) {
			// an LDAP exception can sometimes be thrown here, catch and rethrow
			throw new GbException("An error occurred getting the list of users.", e);
		}
	}

	/**
	* Create a map so that we can use the user's EID (from the imported file) to lookup their UUID (used to store the grade by the backend service).
	*
	* @return Map where the user's EID is the key and the {@link GbUser} object is the value
	*/
	public Map<String, GbUser> getUserEidMap(final List<GbUser> users) {
		final Map<String, GbUser> userEidMap = new HashMap<>();
		for (final GbUser user : users) {
			final String eid = user.getDisplayId();
			if (StringUtils.isNotBlank(eid)) {
				userEidMap.put(eid, user);
			}
		}

		return userEidMap;
	}

	/**
	 * Gets a List of GbUsers for the specified userUuids without any filtering.
	 * Appropriate only for back end business like grade exports, statistics, etc.
	 * @param userUuids
	 * @return
	 */
	public List<GbUser> getGbUsers(final String siteId, final List<String> userUuids)
	{
		final List<GbUser> gbUsers = new ArrayList<>(userUuids.size());
		final List<User> users = getUsers(userUuids);
		final Site site = getSite(siteId).orElse(null);

		Map<String, List<String>> userSections
			= (site != null) ? gradingService.getUserSections(site.getId()) : Collections.emptyMap();

		for (final User u : users) {
			gbUsers.add(new GbUser(u, gradingService.getStudentNumber(u, site))
							.setSections(userSections.getOrDefault(u.getId(), Collections.emptyList())));
		}

		return gbUsers;
	}

	/**
	 * Helper to get a reference to the gradebook for the specified site
	 *
	 * @param gradebookUid the gradebookUid
	 * @param siteId the siteId
	 * @return the gradebook for the site
	 */
	public Gradebook getGradebook(String gradebookUid, String siteId) {
	   return gradingService.getGradebook(gradebookUid, siteId);
	}

	/**
	 * Special operation to get a list of assignments in the gradebook that the specified student has access to. This taked into account
	 * externally defined assessments that may have grouping permissions applied.
	 *
	 * This should only be called if you are wanting to view the assignments that a student would see (ie if you ARE a student, or if you
	 * are an instructor using the student review mode)
	 *
	 * Define the sortedBy to return these assignments back in the desired order.
	 *
	 * @return a list of assignments or empty list if none/no gradebook
	 */
	public List<Assignment> getGradebookAssignmentsForStudent(final String gradebookUid, final String siteId, final String studentUuid, final SortType sortedBy) {

		final List<Assignment> assignments = getGradebookAssignments(gradebookUid, siteId, sortedBy);

		// NOTE: cannot do a role check here as it assumes the current user but this could have been called by an instructor (unless we add
		// a new method to handle this)
		// in any case the role check would just be a confirmation that the user passed in was a student.

		// for each assignment we need to check if it is grouped externally and if the user has access to the group
		final Iterator<Assignment> iter = assignments.iterator();
		while (iter.hasNext()) {
			final Assignment a = iter.next();
			if (a.getExternallyMaintained()) {
				if (this.gradingService.isExternalAssignmentGrouped(gradebookUid, a.getExternalId()) &&
					!this.gradingService.isExternalAssignmentVisible(gradebookUid, a.getExternalId(),
						studentUuid)) {
					iter.remove();
				}
			}
		}
		return assignments;
	}

	/**
	 * Get a list of assignments in the gradebook in the specified site that the current user is allowed to access, sorted by sort order
	 *
	 * @param gradebookUid
	 * @param siteId the siteId
	 * @param sortBy
	 * @return a list of assignments or empty list if none/no gradebook
	 */
	public List<Assignment> getGradebookAssignments(final String gradebookUid, final String siteId, final SortType sortBy) {

		final List<Assignment> assignments = new ArrayList<>();
		final Gradebook gradebook = getGradebook(gradebookUid, siteId);
		if (gradebook != null) {
			// applies permissions (both student and TA) and default sort is
			// SORT_BY_SORTING
			assignments.addAll(this.gradingService.getViewableAssignmentsForCurrentUser(gradebookUid, siteId, sortBy));
		}
        log.debug("Retrieved {} assignments", assignments.size());
		return assignments;
	}

	public List<Assignment> getGradebookAssignmentsForCategory(final String gradebookUid, final String siteId, final Long categoryId, final SortType sortBy) {
		final List<Assignment> returnList = new ArrayList<>();
		final List<Assignment> assignments = getGradebookAssignments(gradebookUid, siteId, sortBy);
		for (Assignment assignment : assignments) {
			if (Objects.equals(assignment.getCategoryId(), categoryId)) {
				returnList.add(assignment);
			}
		}
		return returnList;
	}

	/**
	 * Get a list of categories in the gradebook in the specified site
	 *
	 * @param siteId the siteId
	 * @return a list of categories or empty if no gradebook
	 */
	public List<CategoryDefinition> getGradebookCategories(final String gradebookUid, final String siteId) {
		final Gradebook gradebook = getGradebook(gradebookUid, siteId);

		List<CategoryDefinition> rval = new ArrayList<>();

		if (gradebook == null) {
			return rval;
		}

		if (gradingService.categoriesAreEnabled(gradebookUid, siteId)) {
			rval = this.gradingService.getCategoryDefinitions(gradebookUid, siteId);
		}

		GbRole role;
		try {
			role = gradingService.getUserRole(siteId);
		} catch (GbAccessDeniedException e) {
			log.warn("GbAccessDeniedException trying to getGradebookCategories", e);
			return rval;
		}

		// filter for TAs
		if (role == GbRole.TA) {
			final User user = getCurrentUser();

			// build a list of categoryIds
			final List<Long> allCategoryIds = new ArrayList<>();
			for (final CategoryDefinition cd : rval) {
				allCategoryIds.add(cd.getId());
			}

			if (allCategoryIds.isEmpty()) {
				return Collections.emptyList();
			}

			// get a list of category ids the user can actually view
			List<Long> viewableCategoryIds = this.gradingPermissionService
					.getCategoriesForUser(gradebook.getId(), user.getId(), allCategoryIds);

			//FIXME: this is a hack to implement the old style realms checks. The above method only checks the gb_permission_t table and not realms
			//if categories is empty (no fine grain permissions enabled), Check permissions, if they are not empty then realms perms exist 
			//and they don't filter to category level so allow all.
			//This should still allow the gb_permission_t perms to override if the TA is restricted to certain categories
			if(viewableCategoryIds.isEmpty() && !gradingService.getPermissionsForUser(user.getId(), gradebookUid, siteId).isEmpty()){
				viewableCategoryIds = allCategoryIds;
			}

			// remove the ones that the user can't view
			final Iterator<CategoryDefinition> iter = rval.iterator();
			while (iter.hasNext()) {
				final CategoryDefinition categoryDefinition = iter.next();
				if (!viewableCategoryIds.contains(categoryDefinition.getId())) {
					iter.remove();
				}
			}

		}

		// Sort by categoryOrder
		Collections.sort(rval, CategoryDefinition.orderComparator);

		return rval;
	}

	public Optional<CategoryDefinition> getCategory(Long categoryId, String siteId) {
		return gradingService.getCategoryDefinition(categoryId, siteId);
	}

	public void updateCategory(CategoryDefinition category) {
		gradingService.updateCategory(category);
	}

	/**
	* Retrieve the categories visible to the given student.
	*
	* This should only be called if you are wanting to view the categories that a student would see (ie if you ARE a student, or if you
	* are an instructor using the student review mode)
	*
	* @param studentUuid
	* @return list of visible categories
	*/
	public List<CategoryDefinition> getGradebookCategoriesForStudent(String gradebookUid, String siteId, String studentUuid) {
		// find the categories that this student's visible assignments belong to
		List<Assignment> viewableAssignments = getGradebookAssignmentsForStudent(gradebookUid, siteId, studentUuid, SortType.SORT_BY_SORTING);
		final List<Long> catIds = new ArrayList<>();
		for (Assignment a : viewableAssignments) {
			Long catId = a.getCategoryId();
			if (catId != null && !catIds.contains(catId)) {
				catIds.add(a.getCategoryId());
			}
		}

		// get all the categories in the gradebook, use a security advisor in case the current user is the student
		SecurityAdvisor gbAdvisor = (String userId, String function, String reference)
						-> "gradebook.gradeAll".equals(function) ? SecurityAdvice.ALLOWED : SecurityAdvice.PASS;
		securityService.pushAdvisor(gbAdvisor);
		List<CategoryDefinition> catDefs = gradingService.getCategoryDefinitions(gradebookUid, siteId);
		securityService.popAdvisor(gbAdvisor);

		// filter out the categories that don't match the categories of the viewable assignments
		return catDefs.stream().filter(def -> catIds.contains(def.getId())).collect(Collectors.toList());

	}

	/**
	 * Get the course grade for a student. Safe to call when logged in as a student.
	 *
	 * @param studentUuid
	 * @return coursegrade. May have null fields if the coursegrade has not been released
	 */
	public CourseGradeTransferBean getCourseGrade(final String gradebookUid, final String siteId, final String studentUuid) {

		final CourseGradeTransferBean courseGrade = this.gradingService.getCourseGradeForStudent(gradebookUid, siteId, studentUuid);

		// handle the special case in the gradebook service where totalPointsPossible = -1
		if (courseGrade != null && (courseGrade.getTotalPointsPossible() == null || courseGrade.getTotalPointsPossible() == -1)) {
			courseGrade.setTotalPointsPossible(null);
			courseGrade.setPointsEarned(null);
		}

		return courseGrade;
	}

	/**
	 * Get the student's course grade's GradableObject ID.
	 * @return coursegrade's GradableObject ID.
	 */
	public Long getCourseGradeId(Long gradebookId){
		return this.gradingService.getCourseGradeId(gradebookId);
	}

	/**
	 * Save the grade and comment for a student's assignment and do concurrency checking
	 *
	 * @param assignmentId id of the gradebook assignment
	 * @param studentUuid uuid of the user
	 * @param oldGrade old grade, passed in for concurrency checking/ If null, concurrency checking is skipped.
	 * @param newGrade new grade for the assignment/user
	 * @param comment optional comment for the grade. Can be null.
	 *
	 * @return
	 *
	 * 		TODO make the concurrency check a boolean instead of the null oldGrade
	 */
	public GradeSaveResponse saveGrade(final String gradebookUid, final String siteId, final Long assignmentId, final String studentUuid, final String oldGrade,
			final String newGrade, final String comment) {

		final Gradebook gradebook = this.getGradebook(gradebookUid, siteId);
		if (gradebook == null) {
			return GradeSaveResponse.ERROR;
		}

		// if newGrade is null, no change
		if (newGrade == null) {
			return GradeSaveResponse.NO_CHANGE;
		}

		// get current grade
		final String storedGrade = this.gradingService.getAssignmentScoreString(gradebookUid, siteId, assignmentId,
				studentUuid);

		// get assignment config
		final Assignment assignment = this.getAssignment(gradebookUid, siteId, assignmentId);
		final Double maxPoints = assignment.getPoints();

		// check what grading mode we are in
		final Integer gradingType = gradebook.getGradeType();

		// if percentage entry type, reformat the grades, otherwise use points as is
		String newGradeAdjusted = newGrade;
		String oldGradeAdjusted = oldGrade;
		String storedGradeAdjusted = storedGrade;

		// Fix a problem when the grades comes from the old Gradebook API with locale separator, always compare the values using the same
		// separator
		if (StringUtils.isNotBlank(oldGradeAdjusted)) {
			oldGradeAdjusted = oldGradeAdjusted.replace(",".equals(formattedText.getDecimalSeparator()) ? "." : ",",
					",".equals(formattedText.getDecimalSeparator()) ? "," : ".");
		}
		if (StringUtils.isNotBlank(storedGradeAdjusted)) {
			storedGradeAdjusted = storedGradeAdjusted.replace(",".equals(formattedText.getDecimalSeparator()) ? "." : ",",
					",".equals(formattedText.getDecimalSeparator()) ? "," : ".");
		}

		if (Objects.equals(GradingConstants.GRADE_TYPE_PERCENTAGE, gradingType)) {
			// the passed in grades represents a percentage so the number needs to be adjusted back to points
			Double newGradePercentage = new Double("0.0");

			if(StringUtils.isNotBlank(newGrade)){
				newGradePercentage = formatHelper.validateDouble(newGrade);
			}

			final Double newGradePointsFromPercentage = (newGradePercentage / 100) * maxPoints;
			newGradeAdjusted = formatHelper.formatDoubleToDecimal(newGradePointsFromPercentage);

			// only convert if we had a previous value otherwise it will be out of sync
			if (StringUtils.isNotBlank(oldGradeAdjusted)) {
				// To check if our data is out of date, we first compare what we think
				// is the latest saved score against score stored in the database. As the score
				// is stored as points, we must convert this to a percentage. To be sure we're
				// comparing apples with apples, we first determine the number of decimal places
				// on the score, so the converted points-as-percentage is in the expected format.

				final Double oldGradePercentage = formatHelper.validateDouble(oldGradeAdjusted);
				final Double oldGradePointsFromPercentage = (oldGradePercentage / 100) * maxPoints;

				oldGradeAdjusted = formatHelper.formatDoubleToMatch(oldGradePointsFromPercentage, storedGradeAdjusted);

				oldGradeAdjusted = oldGradeAdjusted.replace(",".equals(formattedText.getDecimalSeparator()) ? "." : ",",
					",".equals(formattedText.getDecimalSeparator()) ? "," : ".");
			}

			// we dont need processing of the stored grade as the service does that when persisting.
		}

		// trim the .0 (and the ,0) from the grades if present. UI removes it so lets standardise
		// trim to null so we can better compare against no previous grade being recorded (as it will be null)
		// Note that we also trim newGrade so that don't add the grade if the new grade is blank and there was no grade previously
		storedGradeAdjusted = formatHelper.normalizeGrade(storedGradeAdjusted);
		oldGradeAdjusted = formatHelper.normalizeGrade(oldGradeAdjusted);
		newGradeAdjusted = formatHelper.normalizeGrade(newGradeAdjusted);

		log.debug("storedGradeAdjusted: {}", storedGradeAdjusted);
		log.debug("oldGradeAdjusted: {}", oldGradeAdjusted);
		log.debug("newGradeAdjusted: {}", newGradeAdjusted);

		// if comment longer than MAX_COMMENT_LENGTH chars, error.
		// SAK-33836 - MAX_COMMENT_LENGTH controlled by sakai.property 'gradebookng.maxCommentLength'; defaults to 20,000
		if (CommentValidator.isCommentInvalid(comment, serverConfigService)) {
			log.error("Comment too long. Maximum {} characters.", CommentValidator.getMaxCommentLength(serverConfigService));
			return GradeSaveResponse.ERROR;
		}

		// no change
		if (StringUtils.equals(storedGradeAdjusted, newGradeAdjusted)) {
			final Double storedGradePoints = formatHelper.validateDouble(storedGradeAdjusted);
			if (storedGradePoints != null && storedGradePoints.compareTo(maxPoints) > 0) {
				return GradeSaveResponse.OVER_LIMIT;
			} else {
				return GradeSaveResponse.NO_CHANGE;
			}
		}

		// concurrency check, if stored grade != old grade that was passed in,
		// someone else has edited.
		// if oldGrade == null, ignore concurrency check
		if (oldGrade != null) {
			try {
				NumberFormat format = NumberFormat.getNumberInstance();
				// SAK-42001 A stored value in database of 69.225 needs to match the 69.22 coming back from UI AJAX call
				final BigDecimal storedBig = storedGradeAdjusted == null ? BigDecimal.ZERO : new BigDecimal(format.parse(storedGradeAdjusted).doubleValue()).setScale(2, RoundingMode.HALF_DOWN);
				final BigDecimal oldBig = oldGradeAdjusted == null ? BigDecimal.ZERO : new BigDecimal(format.parse(oldGradeAdjusted).doubleValue()).setScale(2, RoundingMode.HALF_DOWN);
				if (storedBig.compareTo(oldBig) != 0) {
					log.warn("Rejected new grade because of concurrent edit: {} vs {}", storedBig, oldBig);
					return GradeSaveResponse.CONCURRENT_EDIT;
				}
			} catch (ParseException pe) {
				log.warn("Failed to parse adjusted grades in current locale");
			}
		}

		GradeSaveResponse rval = null;

		if (StringUtils.isNotBlank(newGradeAdjusted)) {
			final Double newGradePoints = formatHelper.validateDouble(newGradeAdjusted);

			// if over limit, still save but return the warning
			if (newGradePoints != null && newGradePoints.compareTo(maxPoints) > 0) {
				log.debug("over limit. Max: {}", maxPoints);
				rval = GradeSaveResponse.OVER_LIMIT;
			}
		}

		// save
		try {
			// note, you must pass in the comment or it will be nulled out by the GB service
			// also, must pass in the raw grade as the service does conversions between percentage etc
			this.gradingService.saveGradeAndCommentForStudent(gradebookUid, siteId, assignmentId, studentUuid,
					newGrade, comment);
			if (rval == null) {
				// if we don't have some other warning, it was all OK
				rval = GradeSaveResponse.OK;
			}
		} catch (InvalidGradeException | AssessmentNotFoundException e) {
			log.error("An error occurred saving the grade. {}: {}", e.getClass(), e.getMessage());
			rval = GradeSaveResponse.ERROR;
		}

		EventHelper.postUpdateGradeEvent(gradebook, assignmentId, studentUuid, newGrade, rval, getUserRoleOrNone(siteId));//mirar si se puede quitar esto y hacer una llamada solamente desde el service?

		return rval;
	}

	public GradeSaveResponse saveGradesAndCommentsForImport(final String gradebookUid, final String siteId, final Assignment assignment, final List<GradeDefinition> gradeDefList) {
		if (gradebookUid == null) {
			return GradeSaveResponse.ERROR;
		}

		try {
			gradingService.saveGradesAndComments(gradebookUid, siteId, assignment.getId(), gradeDefList);
			return GradeSaveResponse.OK;
		} catch (InvalidGradeException | AssessmentNotFoundException e) {
			log.error("An error occurred saving the grade. {}: {}", e.getClass(), e.getMessage());
			return GradeSaveResponse.ERROR;
		}
	}

	/**
	 *
	 * @param assignmentId
	 * @param studentUuid
	 * @param excuse
	 * @return
	 */
	public GradeSaveResponse saveExcuse(final String gradebookUid, final String siteId, final Long assignmentId, final String studentUuid, final boolean excuse){
		if (gradebookUid == null) {
			return GradeSaveResponse.ERROR;
		}

		// get current grade
		final String storedGrade = this.gradingService.getAssignmentScoreString(gradebookUid, siteId, assignmentId,
				studentUuid);

		// if percentage entry type, reformat the grade, otherwise use points as is
		String storedGradeAdjusted = storedGrade;
		final Integer gradingType = getGradebook(gradebookUid, siteId).getGradeType();
		if (Objects.equals(GradingConstants.GRADE_TYPE_PERCENTAGE, gradingType)) {
			// the stored grade represents points so the number needs to be adjusted back to percentage
			Double storedGradePoints = new Double("0.0");
			if (StringUtils.isNotBlank(storedGrade)) {
				storedGradePoints = formatHelper.validateDouble(storedGrade);
			}

			final Double maxPoints = this.getAssignment(gradebookUid, siteId, assignmentId).getPoints();
			final Double storedGradePointsFromPercentage = (storedGradePoints * 100) / maxPoints;
			storedGradeAdjusted = formatHelper.formatDoubleToDecimal(storedGradePointsFromPercentage);
		}
		// trim the .0 (and ,0) from the grades if present. UI removes it so lets standardise.
		storedGradeAdjusted = formatHelper.normalizeGrade(storedGradeAdjusted);

		log.debug("storedGradeAdjusted: {}", storedGradeAdjusted);

		GradeSaveResponse rval = null;

		// save
		try {
			this.gradingService.saveGradeAndExcuseForStudent(gradebookUid, siteId, assignmentId, studentUuid,
					storedGradeAdjusted, excuse);

			if (rval == null) {
				// if we don't have some other warning, it was all OK
				rval = GradeSaveResponse.OK;
			}
		} catch (InvalidGradeException | AssessmentNotFoundException e) {
			log.error("An error occurred saving the excuse. " + e.getClass() + ": " + e.getMessage());
			rval = GradeSaveResponse.ERROR;
		}
		return rval;
	}

	/**
	 * Build the matrix of assignments and grades for the Export process
	 *
	 * @param assignments list of assignments
	 * @param groupFilter
	 * @return
	 */
	public List<GbStudentGradeInfo> buildGradeMatrixForImportExport(final String gradebookUid, final String siteId, final List<Assignment> assignments, String groupFilter) throws GbException {
		// ------------- Initialization -------------
		final GbStopWatch stopwatch = new GbStopWatch();
		stopwatch.start();
		stopwatch.timeWithContext("buildGradeMatrixForImportExport", "buildGradeMatrix start", stopwatch.getTime());

		final Gradebook gradebook = this.getGradebook(gradebookUid, siteId);
		if (gradebook == null) {
			return Collections.EMPTY_LIST;
		}
		stopwatch.timeWithContext("buildGradeMatrixForImportExport", "getGradebook", stopwatch.getTime());

		// get current user
		final String currentUserUuid = getCurrentUser().getId();

		// get role for current user
		GbRole role;
		try {
			role = gradingService.getUserRole(siteId);
		} catch (final GbAccessDeniedException e) {
			throw new GbException("Error getting role for current user", e);
		}

		final GradebookUiSettings settings = new GradebookUiSettings();

		// ------------- Get Users -------------
		final List<String> studentUUIDs = gradingService.getGradeableUsers(gradebookUid, siteId, groupFilter);
		final List<GbUser> gbStudents = getGbUsers(siteId, studentUUIDs);
		stopwatch.timeWithContext("buildGradeMatrixForImportExport", "getGbUsersForUiSettings", stopwatch.getTime());

		// ------------- Course Grades -------------
		final Map<String, GbStudentGradeInfo> matrix = new LinkedHashMap<>();
		gradingService.putCourseGradesInMatrix(matrix, gbStudents, studentUUIDs, gradebook, siteId, role, isCourseGradeVisible(gradebookUid, siteId, currentUserUuid), settings);
		stopwatch.timeWithContext("buildGradeMatrixForImportExport", "putCourseGradesInMatrix", stopwatch.getTime());

		// ------------- Assignments -------------
		gradingService.putAssignmentsAndCategoryItemsInMatrix(matrix, gbStudents, studentUUIDs, assignments, gradebook, siteId, currentUserUuid, role, settings);
		stopwatch.timeWithContext("buildGradeMatrixForImportExport", "putAssignmentsAndCategoryItemsInMatrix", stopwatch.getTime());

		// ------------- Sorting -------------
		List<GbStudentGradeInfo> items = gradingService.sortGradeMatrix(gradebook.getUid(), siteId, matrix, settings);
		stopwatch.timeWithContext("buildGradeMatrixForImportExport", "sortGradeMatrix", stopwatch.getTime());

		return items;
	}

	public List<GbGradeComparisonItem> buildMatrixForGradeComparison(String gradebookUid, String siteId, Assignment assignment, Integer gradingType, GradebookInformation settings){
		// Only return the list if the feature is activated
		boolean serverPropertyOn = serverConfigService.getConfig(
				SAK_PROP_ALLOW_STUDENTS_TO_COMPARE_GRADES,
				SAK_PROP_ALLOW_STUDENTS_TO_COMPARE_GRADES_DEFAULT
		);
		if (!serverPropertyOn) {
			return new ArrayList<>();
		}
		
		List<GbGradeComparisonItem> data;
		
		String userEid = getCurrentUser().getEid();
		
		boolean isComparingAndDisplayingFullName = settings
						.getComparingDisplayStudentNames() &&
				settings
						.getComparingDisplayStudentSurnames();

		boolean isComparingOrDisplayingFullName = settings
								.getComparingDisplayStudentNames() ||
						settings
								.getComparingDisplayStudentSurnames();

		// Add advisor to retrieve the grades as student
		SecurityAdvisor advisor = null;
		try {
			advisor = addSecurityAdvisor();
			data = gradingService.buildGradeMatrix(gradebookUid, siteId, Collections.singletonList(assignment), gradingService.getGradeableUsers(gradebookUid, siteId, null), null)
					.stream().map(GbGradeComparisonItem::new)
					.map(el -> {
						if(isComparingOrDisplayingFullName){
							String studentDisplayName = String.format(
								"%s%s%s",
								settings.getComparingDisplayStudentNames() ? el.getStudentFirstName() : "",
								isComparingAndDisplayingFullName ? " " : "",
								settings.getComparingDisplayStudentSurnames()? el.getStudentLastName() : ""
							);
							el.setStudentDisplayName(studentDisplayName);
						}
						el.setIsCurrentUser(userEid.equals(el.getEid()));
						
						el.setGrade(formatHelper.formatGrade(el.getGrade())
								+ (Objects.equals(GradingConstants.GRADE_TYPE_PERCENTAGE, gradingType) ? "%" : ""));
						return el;
					})
					.collect(Collectors.toList());
			
			if(settings.getComparingRandomizeDisplayedData()){
				Collections.shuffle(data);
			}
			return data;
		} finally {
			removeSecurityAdvisor(advisor);
		}
	}

	/**
	 * Builds up the matrix (a map<userUid, GbStudentGradeInfo>) for the specified students / assignments.a
	 * @param matrix output parameter; a map of studentUuids to GbStudentGradeInfo objects which will contain grade data for the specified assignments
	 * @param gbStudents list of GbUsers for whom to retrieve grading data
	 * @param studentUuids list of student UUIDs so we don't have to extract from GbUsers
	 * @param assignments the list of assignments for which to retrieve grading data. Computes category scores associated with these assignments as appropriate
	 * @param gradebook the gradebook containing the assignments, etc.
	 * @param siteId
	 * @param currentUserUuid
	 * @param role the current user's role
	 */
	public void putAssignmentsInMatrixForExport(Map<String, GbStudentGradeInfo> matrix, List<GbUser> gbStudents, List<String> studentUuids, List<Assignment> assignments,
													Gradebook gradebook, String siteId, String currentUserUuid, GbRole role) {
		// Collect list of studentUuids, and ensure the matrix is populated with GbStudentGradeInfo instances for each student
		gbStudents.stream().forEach(gbStudent -> {
			String userUuid = gbStudent.getUserUuid();
			GbStudentGradeInfo info = matrix.get(userUuid);
			if (info == null)
			{
				matrix.put(userUuid, new GbStudentGradeInfo(gbStudent));
			}
		});

		// iterate over assignments and get the grades for each
		// note, the returned list only includes entries where there is a grade
		// for the user
		// we also build the category lookup map here
		for (final Assignment assignment : assignments) {

			// get grades
			final List<GradeDefinition> defs = this.gradingService.getGradesForStudentsForItem(gradebook.getUid(), siteId, assignment.getId(), studentUuids);

			// iterate the definitions returned and update the record for each
			// student with the grades
			for (final GradeDefinition def : defs) {
				final GbStudentGradeInfo sg = matrix.get(def.getStudentUid());

				if (sg == null) {
					log.warn("No matrix entry seeded for: {}. This user may have been removed from the site", def.getStudentUid());
				} else {
					// this will overwrite the stub entry for the TA matrix if
					// need be
					sg.addGrade(assignment.getId(), new GbGradeInfo(def));
				}
			}
		}

		// for a TA, apply the permissions to each grade item to see if we can export it
		// the list of students, assignments and grades is already filtered to those that can be viewed
		// so we are only concerned with the gradeable permission
		if (role == GbRole.TA) {

			// get permissions
			final List<PermissionDefinition> permissions = gradingService.getPermissionsForUser(currentUserUuid, gradebook.getUid(), siteId);

			log.debug("All permissions: {}", permissions.size());

			// only need to process this if some are defined
			// again only concerned with grade permission, so parse the list to
			// remove those that aren't GRADE
			permissions.removeIf(permission -> !StringUtils.equalsIgnoreCase(GraderPermission.GRADE.toString(), permission.getFunctionName()));

			log.debug("Filtered permissions: {}", permissions.size());

			// if we still have permissions, they will be of type grade, so we
			// need to enrich the students grades
			if (!permissions.isEmpty()) {

				// first need a lookup map of assignment id to category, so we
				// can link up permissions by category
				final Map<Long, Long> assignmentCategoryMap = new HashMap<>();
				for (final Assignment assignment : assignments) {
					assignmentCategoryMap.put(assignment.getId(), assignment.getCategoryId());
				}

				// get the group membership for the students
				final Map<String, List<String>> groupMembershipsMap = gradingService.getGroupMemberships(gradebook.getUid(), siteId);

				// for every student
				for (final GbUser student : gbStudents) {
					log.debug("Processing student: {}", student.getDisplayId());

					final GbStudentGradeInfo sg = matrix.get(student.getUserUuid());

					// get their assignment/grade list
					final Map<Long, GbGradeInfo> gradeMap = sg.getGrades();

					// for every assignment that has a grade
					for (final Map.Entry<Long, GbGradeInfo> entry : gradeMap.entrySet()) {
						// categoryId
						final Long gradeCategoryId = assignmentCategoryMap.get(entry.getKey());

						log.debug("Grade: {}", entry.getValue());

						// iterate the permissions
						// if category, compare the category,
						// then check the group and find the user in the group
						// if all ok, mark it as GRADEABLE

						boolean gradeable = false;

						for (final PermissionDefinition permission : permissions) {
							// we know they are all GRADE so no need to check here

							boolean categoryOk = false;
							boolean groupOk = false;

							final Long permissionCategoryId = permission.getCategoryId();
							final String permissionGroupReference = permission.getGroupReference();

							log.debug("permissionCategoryId: {}", permissionCategoryId);
							log.debug("permissionGroupReference: {}", permissionGroupReference);

							// if permissions category is null (can grade all categories) or they match (can grade this category)
							if (permissionCategoryId == null || permissionCategoryId.equals(gradeCategoryId)) {
								categoryOk = true;
								log.debug("Category check passed");
							}

							// if group reference is null (can grade all groups) or group membership contains student (can grade this group)
							if (StringUtils.isBlank(permissionGroupReference)) {
								groupOk = true;
								log.debug("Group check passed #1");
							} else {
								final List<String> groupMembers = groupMembershipsMap.get(permissionGroupReference);
								log.debug("groupMembers: {}", groupMembers);

								if (groupMembers != null && groupMembers.contains(student.getUserUuid())) {
									groupOk = true;
									log.debug("Group check passed #2");
								}
							}

							if (categoryOk && groupOk) {
								gradeable = true;
								break;
							}
						}

						// set the gradeable flag on this grade instance
						final GbGradeInfo gradeInfo = entry.getValue();
						gradeInfo.setGradeable(gradeable);
					}
				}
			}
		}
	}

	/**
	 * Helper to get site. This will ONLY work in a portal site context, it will return empty otherwise (ie via an entityprovider).
	 *
	 * @return
	 */
	public Optional<Site> getSite(String siteId)
	{
		if (siteId != null)
		{
			try
			{
				return Optional.of(this.siteService.getSite(siteId));
			}
			catch (final IdUnusedException e)
			{
				// do nothing
			}
		}

		return Optional.empty();
	}

	/**
	 * Helper to get user
	 *
	 * @return
	 */
	public User getCurrentUser() {
		return this.userDirectoryService.getCurrentUser();
	}

	/**
	 * Determine if the current user is an admin user.
	 *
	 * @return true if the current user is admin, false otherwise.
	 */
	public boolean isSuperUser() {
		return this.securityService.isSuperUser();
	}

	/**
	 * Add a new assignment definition to the gradebook
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param assignment
	 * @return id of the newly created assignment or null if there were any errors
	 */
	public Long addAssignment(final String gradebookUid, final String siteId, final Assignment assignment) {

		final Gradebook gradebook = getGradebook(gradebookUid, siteId);

		if (gradebook != null) {
			final Long assignmentId = this.gradingService.addAssignment(gradebookUid, siteId, assignment);

			// Force the assignment to sit at the end of the list
			if (assignment.getSortOrder() == null) {
				final List<Assignment> allAssignments = this.gradingService.getAssignments(gradebookUid, siteId, SortType.SORT_BY_NONE);
				int nextSortOrder = allAssignments.size();
				for (final Assignment anotherAssignment : allAssignments) {
					if (anotherAssignment.getSortOrder() != null && anotherAssignment.getSortOrder() >= nextSortOrder) {
						nextSortOrder = anotherAssignment.getSortOrder() + 1;
					}
				}
				updateAssignmentOrder(gradebookUid, siteId, assignmentId, nextSortOrder);
			}

			// also update the categorized order
			updateAssignmentCategorizedOrder(gradebookUid, siteId, assignment.getCategoryId(), assignmentId,
					Integer.MAX_VALUE);

			EventHelper.postAddAssignmentEvent(gradebook, assignmentId, assignment, getUserRoleOrNone(siteId));
			
            if (assignment.getReleased()) {
                String reference =  GradingConstants.REFERENCE_ROOT + Entity.SEPARATOR + "a" + Entity.SEPARATOR + siteId + Entity.SEPARATOR + assignmentId;
                Task task = new Task();
                task.setSiteId(siteId);
                task.setReference(reference);
                task.setSystem(true);
                task.setDescription(assignment.getName());
                task.setDue((assignment.getDueDate() == null) ? null : assignment.getDueDate().toInstant());
                Set<String> users = new HashSet<>(gradingService.getGradeableUsers(gradebookUid, siteId, null));
                taskService.createTask(task, users, Priorities.HIGH);
            }
                        
			return assignmentId;

			// TODO wrap this so we can catch any runtime exceptions
		}
		return null;
	}

	/**
	 * Update the order of an assignment. If calling outside of GBNG, use this method as you can provide the site id.
	 *
	 * @param gradebookUid the gradebookUid
	 * @param siteId the siteId
	 * @param assignmentId the assignment we are reordering
	 * @param order the new order
	 */
	public void updateAssignmentOrder(final String gradebookUid, final String siteId, final long assignmentId, final int order) {
		this.gradingService.updateAssignmentOrder(gradebookUid, siteId, assignmentId, order);
	}

	/**
	 * Update the categorized order of an assignment.
	 *
	 * @param gradebookUid the gradebookUid
	 * @param siteId the site's id
	 * @param assignmentId the assignment we are reordering
	 * @param order the new order
	 * @throws IdUnusedException
	 * @throws PermissionException
	 */
	public void updateAssignmentCategorizedOrder(final String gradebookUid, final String siteId, final long assignmentId, final int order)
			throws IdUnusedException, PermissionException {

		final Gradebook gradebook = getGradebook(gradebookUid, siteId);

		if (gradebook == null) {
			log.error("Gradebook {} not found", gradebookUid);
			return;
		}

		final Assignment assignmentToMove = this.gradingService.getAssignment(gradebookUid, siteId, assignmentId);

		if (assignmentToMove == null) {
			// TODO Handle assignment not in gradebook
			log.error("GradebookAssignment {} not found in gradebook {}", assignmentId, gradebookUid);
			return;
		}

		updateAssignmentCategorizedOrder(gradebookUid, siteId, assignmentToMove.getCategoryId(), assignmentToMove.getId(),
				order);
	}

	/**
	 * Update the categorized order of an assignment via the gradebook service.
	 *
	 * @param gradebookId the gradebook's id
	 * @param siteId the site's id
	 * @param categoryId the id for the cataegory in which we are reordering
	 * @param assignmentId the assignment we are reordering
	 * @param order the new order
	 */
	private void updateAssignmentCategorizedOrder(final String gradebookId, final String siteId, final Long categoryId,
			final Long assignmentId, final int order) {
		this.gradingService.updateAssignmentCategorizedOrder(gradebookId, siteId, categoryId, assignmentId, order);
	}

	/**
	 * Get a list of edit events for this gradebook. Excludes any events for the current user
	 *
	 * @param gradebookUid the gradebook that we are interested in
	 * @param siteId the site id
	 * @param since the time to check for changes from
	 * @return
	 */
	public List<GbGradeCell> getEditingNotifications(final String gradebookUid, final String siteId, final Date since) {

		final User currentUser = getCurrentUser();

		final List<GbGradeCell> rval = new ArrayList<>();

		final List<Assignment> assignments = this.gradingService.getViewableAssignmentsForCurrentUser(gradebookUid, siteId,
				SortType.SORT_BY_SORTING);
        log.debug("Retrieved {} assignments", assignments.size());
		final List<Long> assignmentIds = assignments.stream().map(a -> a.getId()).collect(Collectors.toList());
		final List<GradingEvent> events = this.gradingService.getGradingEvents(assignmentIds, since);

		// keep a hash of all users so we don't have to hit the service each time
		final Map<String, GbUser> users = new HashMap<>();

		// filter out any events made by the current user
		for (final GradingEvent event : events) {
			if (!event.getGraderId().equals(currentUser.getId())) {
				// update cache (if required)
				users.putIfAbsent(event.getGraderId(), getUser(event.getGraderId()));

				// pull user from the cache
				final GbUser updatedBy = users.get(event.getGraderId());
				rval.add(
						new GbGradeCell(
								event.getStudentId(),
								event.getGradableObject().getId(),
								updatedBy.getDisplayName()));
			}
		}

		return rval;
	}
	/**
	 * Get an GradebookAssignment in the specified site given the assignment id
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param assignmentId
	 * @return
	 */
	public Assignment getAssignment(final String gradebookUid, final String siteId, final long assignmentId) {
		if (gradebookUid != null) {
			return this.gradingService.getAssignment(gradebookUid, siteId, assignmentId);
		}
		return null;
	}

	/**
	 * Get an GradebookAssignment in the specified site given the assignment name This should be avoided where possible but is required for the
	 * import process to allow modification of assignment point values
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param assignmentName
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public Assignment getAssignment(final String gradebookUid, final String siteId, final String assignmentName) {
		if (gradebookUid != null) {
			return this.gradingService.getAssignment(gradebookUid, siteId, assignmentName);
		}
		return null;
	}

	/**
	 * Get the sort order of an assignment. If the assignment has a sort order, use that. Otherwise we determine the order of the assignment
	 * in the list of assignments
	 *
	 * This means that we can always determine the most current sort order for an assignment, even if the list has never been sorted.
	 *
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param assignmentId
	 * @return sort order if set, or calculated, or -1 if cannot determine at all.
	 */
	public int getAssignmentSortOrder(final String gradebookUid, final String siteId, final long assignmentId) {

		if (gradebookUid != null) {
			final Assignment assignment = this.gradingService.getAssignment(gradebookUid, siteId, assignmentId);

			// if the assignment has a sort order, return that
			if (assignment.getSortOrder() != null) {
				return assignment.getSortOrder();
			}

			// otherwise we need to determine the assignment sort order within
			// the list of assignments
			final List<Assignment> assignments = this.getGradebookAssignments(gradebookUid, siteId, SortType.SORT_BY_SORTING);

			for (int i = 0; i < assignments.size(); i++) {
				final Assignment a = assignments.get(i);
				if (assignmentId == a.getId() && a.getSortOrder() != null) {
					return a.getSortOrder();
				}
			}
		}

		return -1;
	}

	/**
	 * Update the details of an assignment
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param assignment
	 * @return
	 */
	public void updateAssignment(final String gradebookUid, final String siteId, final Assignment assignment) {
		final Gradebook gradebook = getGradebook(gradebookUid, siteId);

		// need the original name as the service needs that as the key...
		final Assignment original = this.getAssignment(gradebookUid, siteId, assignment.getId());

		gradingService.updateAssignment(gradebook.getUid(), siteId, original.getId(), assignment);
		
		// Update task
		String reference =  GradingConstants.REFERENCE_ROOT + Entity.SEPARATOR + "a" + Entity.SEPARATOR + siteId + Entity.SEPARATOR + original.getId();
		Optional<Task> optTask = taskService.getTask(reference);
		if (optTask.isPresent()) {
			Task task = optTask.get();
			task.setDescription(assignment.getName());
			task.setDue((assignment.getDueDate() == null) ? null : assignment.getDueDate().toInstant());
			taskService.saveTask(task);
		} else if(assignment.getReleased()) {
			// Create the task
			Task task = new Task();
			task.setSiteId(siteId);
			task.setReference(reference);
			task.setSystem(true);
			task.setDescription(assignment.getName());
			task.setDue((assignment.getDueDate() == null) ? null : assignment.getDueDate().toInstant());
			Set<String> users = new HashSet<>(gradingService.getGradeableUsers(gradebookUid, siteId, null));
			taskService.createTask(task, users, Priorities.HIGH);
		}
        
		EventHelper.postUpdateAssignmentEvent(gradebook, assignment, getUserRoleOrNone(siteId));

		if (original.getCategoryId() != null && assignment.getCategoryId() != null
				&& original.getCategoryId().longValue() != assignment.getCategoryId().longValue()) {
			updateAssignmentCategorizedOrder(gradebook.getUid(), siteId, assignment.getCategoryId(), assignment.getId(),
					Integer.MAX_VALUE);
		}
	}

	/**
	 * Updates ungraded items in the given assignment for students within a particular group and with the given grade
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param assignmentId
	 * @param grade
	 * @param group
	 * @return
	 */
	public boolean updateUngradedItems(final String gradebookUid, final String siteId, final long assignmentId, final String grade, final String group) {
		final Gradebook gradebook = getGradebook(gradebookUid, siteId);
		final Assignment assignment = getAssignment(gradebookUid, siteId, assignmentId);

		// get students
		final List<String> studentUuids = gradingService.getGradeableUsers(gradebookUid, siteId, group);

		// get grades (only returns those where there is a grade, or comment; does not return those where there is no grade AND no comment)
		final List<GradeDefinition> defs = this.gradingService.getGradesForStudentsForItem(gradebook.getUid(), siteId, assignmentId, studentUuids);

		// Remove students who already have a grade
		studentUuids.removeIf(studentUUID -> defs.stream().anyMatch(def -> studentUUID.equals(def.getStudentUid()) && StringUtils.isNotBlank(def.getGrade())));
		defs.removeIf(def -> StringUtils.isNotBlank(def.getGrade()));

		// Create new GradeDefinition objects for those students who do not have one
		for (String studentUUID : studentUuids) {
			if (defs.stream().noneMatch(def -> studentUUID.equals(def.getStudentUid()))) {
				GradeDefinition def = new GradeDefinition();
				def.setStudentUid(studentUUID);
				def.setGradeEntryType(gradebook.getGradeType());
				def.setGradeReleased(gradebook.getAssignmentsDisplayed() && assignment.getReleased());
				defs.add(def);
			}
		}

		// Short circuit
		if (defs.isEmpty()) {
			log.debug("Setting default grade. No students are ungraded.");
		}

		// Apply the new grade to the GradeDefinitions to be updated
		for (GradeDefinition def : defs) {
			def.setGrade(grade);
			log.debug("Setting default grade. Values of assignmentId: {}, studentUuid: {}, grade: {}", assignmentId, def.getStudentUid(), grade);
		}

		// Batch update the GradeDefinitions, and post an event on completion
		try {
			gradingService.saveGradesAndComments(gradebook.getUid(), siteId, assignmentId, defs);
			EventHelper.postUpdateUngradedEvent(gradebook, assignmentId, String.valueOf(grade), getUserRoleOrNone(siteId));
			return true;
		} catch (final Exception e) {
			log.error("An error occurred updating the assignment", e);
		}

		return false;
	}

	/**
	 * Get the grade log for the given student and assignment
	 *
	 * @param studentUuid
	 * @param assignmentId
	 * @return
	 */
	public List<GbGradeLog> getGradeLog(final String studentUuid, final long assignmentId) {
		final List<GradingEvent> gradingEvents = this.gradingService.getGradingEvents(studentUuid, assignmentId);

		final List<GbGradeLog> rval = new ArrayList<>();
		for (final GradingEvent ge : gradingEvents) {
			rval.add(new GbGradeLog(ge));
		}

		return rval;
	}

	/**
	 * Get the user given a uuid
	 *
	 * @param userUuid
	 * @return GbUser or null if cannot be found
	 */
	public GbUser getUser(final String userUuid) {
		try {
			final User u = this.userDirectoryService.getUser(userUuid);
			return new GbUser(u);
		} catch (final UserNotDefinedException e) {
			return null;
		}
	}

	/**
	 * Get the comment for a given student assignment grade
	 *
	 * @param gradebookUid
	 * @param siteId site id
	 * @param assignmentId id of assignment
	 * @param studentUuid uuid of student
	 * @return the comment or null if none
	 */
	public String getAssignmentGradeComment(final String gradebookUid, final long assignmentId, final String studentUuid) {

		try {
			final CommentDefinition def = this.gradingService.getAssignmentScoreComment(gradebookUid,
					assignmentId, studentUuid);
			if (def != null) {
				return def.getCommentText();
			}
		} catch (AssessmentNotFoundException e) {
			log.error("An error occurred retrieving the comment. {}: {}", e.getClass(), e.getMessage());
		}
		return null;
	}

	public String getAssignmentExcuse(final String gradebookUid, final long assignmentId, final String studentUuid){

		try{
			final boolean excuse = this.gradingService.getIsAssignmentExcused(gradebookUid, assignmentId, studentUuid);
			if(excuse){
				return "1";
			}else{
				return "0";
			}
		} catch (AssessmentNotFoundException e) {
			log.error("An error occurred retrieving the excuse. " + e.getClass() + ": " + e.getMessage());
		}
		return null;
	}

	/**
	 * Update (or set) the comment for a student's assignment
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param assignmentId id of assignment
	 * @param studentUuid uuid of student
	 * @param comment the comment
	 * @return true/false
	 */
	public boolean updateAssignmentGradeComment(final String gradebookUid, final String siteId, final long assignmentId, final String studentUuid,
			final String comment) {

		final Gradebook gradebook = getGradebook(gradebookUid, siteId);

		try {
			// could do a check here to ensure we aren't overwriting someone
			// else's comment that has been updated in the interim...
			this.gradingService.setAssignmentScoreComment(gradebookUid, assignmentId, studentUuid, comment);

			EventHelper.postUpdateCommentEvent(gradebook, assignmentId, studentUuid, comment, getUserRoleOrNone(siteId));

			return true;
		} catch (AssessmentNotFoundException | IllegalArgumentException e) {
			log.error("An error occurred saving the comment. {}: {}", e.getClass(), e.getMessage());
		}

		return false;
	}

	/**
	 * Get the role of the current user in the given site
	 *
	 * @param siteId the siteId to check
	 * @return GbRole for the current user
	 * @throws GbAccessDeniedException if something goes wrong checking the site or user permissions
	 */
	public GbRole getUserRole(String siteId) throws GbAccessDeniedException {
		return gradingService.getUserRole(siteId);
	}

	/**
	 * Get the role of the current user in the given site or GbRole.NONE if the user does not have access
	 *
	 * @param siteId the siteId to check
	 * @return GbRole for the current user
	 */
	public GbRole getUserRoleOrNone(String siteId) {
		try {
			return gradingService.getUserRole(siteId);
		} catch (GbAccessDeniedException e) {
			return GbRole.NONE;
		}
	}

	/**
	 * Get a map of grades for the given student. Safe to call when logged in as a student.
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param studentUuid
	 * @return map of assignment to GbGradeInfo
	 */
	public Map<Long, GbGradeInfo> getGradesForStudent(final String gradebookUid, final String siteId, final String studentUuid) {

		final Gradebook gradebook = getGradebook(gradebookUid, siteId);

		// will apply permissions and only return those the student can view
		final List<Assignment> assignments = getGradebookAssignmentsForStudent(gradebookUid, siteId, studentUuid, SortType.SORT_BY_SORTING);

		final Map<Long, GbGradeInfo> rval = new LinkedHashMap<>();

		// if student, only proceed if grades are released for the site
		// if instructor or TA, skip this check
		// permission checks are still applied at the assignment level in the
		// GradingService
		GbRole role;
		try {
			role = gradingService.getUserRole(siteId);
		} catch (final GbAccessDeniedException e) {
			log.warn("GbAccessDeniedException trying to getGradesForStudent for student: {}", studentUuid, e);
			return rval;
		}

		if (role == GbRole.STUDENT) {
			final boolean released = gradebook.getAssignmentsDisplayed();
			if (!released) {
				log.debug("Grades not released for gradebook: {}, returning empty map", gradebookUid);
				return rval;
			}
		}

		// Extract assignment IDs for bulk fetch
		final List<Long> assignmentIds = assignments.stream()
				.map(Assignment::getId)
				.collect(Collectors.toList());

		if (assignmentIds.isEmpty()) {
			log.debug("No assignments found for student: {} in gradebook: {}", studentUuid, gradebookUid);
			return rval;
		}

		// Fetch all grades in one bulk operation
		final Map<Long, GradeDefinition> gradeDefinitions = getAllGradeDefinitionsWithCommentsForStudent(gradebookUid, siteId, studentUuid, assignmentIds);

		// Build the result map maintaining assignment order
		for (final Assignment assignment : assignments) {
			final Long assignmentId = assignment.getId();
			final GradeDefinition def = gradeDefinitions.get(assignmentId);
			
			// Create GbGradeInfo even if there's no grade definition (will be null grade)
			rval.put(assignmentId, new GbGradeInfo(def));
		}

		log.debug("Retrieved grades for {} assignments for student: {}", rval.size(), studentUuid);
		return rval;
	}

	public GradeDefinition getGradeForStudentForItem(String gradebookUid, String siteId, String studentId, Long assignmentId) {
		return this.gradingService.getGradeDefinitionForStudentForItem(gradebookUid, siteId, assignmentId, studentId);
	}

	/**
	 * Get the category score for the given student.
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param categoryId id of category
	 * @param studentUuid uuid of student
	 * @param isInstructor will calculate the category score with non-released items for instructors but not for students
	 * @return
	 */
	public Optional<CategoryScoreData> getCategoryScoreForStudent(final String gradebookUid, final String siteId, final Long categoryId, final String studentUuid, final boolean isInstructor) {

		final Gradebook gradebook = getGradebook(gradebookUid, siteId);

		final Optional<CategoryScoreData> result = gradingService.calculateCategoryScore(gradebook.getId(), studentUuid, categoryId, isInstructor, gradebook.getCategoryType(), null);
		log.debug("Category score for category: {}, student: {}:{}", categoryId, studentUuid, result.map(r -> r.score).orElse(null));

		return result;
	}

	/**
	 * Get all category scores for the given student in one efficient operation.
	 * This is much more efficient than calling getCategoryScoreForStudent repeatedly for each category.
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param studentUuid uuid of student
	 * @param isInstructor will calculate the category score with non-released items for instructors but not for students
	 * @return map of category ID to CategoryScoreData for all categories that have calculable scores
	 */
	public Map<Long, CategoryScoreData> getAllCategoryScoresForStudent(final String gradebookUid, final String siteId, final String studentUuid, final boolean isInstructor) {

		final Gradebook gradebook = getGradebook(gradebookUid, siteId);

		final Map<Long, CategoryScoreData> result = gradingService.calculateAllCategoryScores(gradebook.getId(), studentUuid, isInstructor, gradebook.getCategoryType());
		log.debug("Retrieved {} category scores for student: {}", result.size(), studentUuid);

		return result;
	}

	/**
	 * Get the settings for this gradebook. Safe to use from an entityprovider.
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @return
	 */
	public GradebookInformation getGradebookSettings(final String gradebookUid, final String siteId) {

		SecurityAdvisor advisor = null;
		try {
			advisor = addSecurityAdvisor();
			final GradebookInformation settings = this.gradingService.getGradebookInformation(gradebookUid, siteId);
			Collections.sort(settings.getCategories(), CategoryDefinition.orderComparator);
			return settings;
		} finally {
			removeSecurityAdvisor(advisor);
		}
	}

	/**
	 * Update the settings for this gradebook. Note that this CANNOT be called by a
	 * student.
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param settings GradebookInformation settings
	 */
	public void updateGradebookSettings(final String gradebookUid, final String siteId, final GradebookInformation settings) {

		final Gradebook gradebook = getGradebook(gradebookUid, siteId);

		this.gradingService.updateGradebookSettings(gradebookUid, siteId, settings);

		EventHelper.postUpdateSettingsEvent(gradebook);
	}

	/**
	 * Remove an assignment from its gradebook
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param assignmentId the id of the assignment to remove
	 */
	public void removeAssignment(final String gradebookUid, final String siteId, final Long assignmentId) {

		// Delete task
		String reference =  GradingConstants.REFERENCE_ROOT + Entity.SEPARATOR + "a" + Entity.SEPARATOR + siteId + Entity.SEPARATOR + assignmentId; 
		taskService.removeTaskByReference(reference);
		rubricsService.deleteRubricAssociationsByItemIdPrefix(assignmentId.toString(), RubricsConstants.RBCS_TOOL_GRADEBOOKNG);
		this.gradingService.removeAssignment(assignmentId);

		EventHelper.postDeleteAssignmentEvent(getGradebook(gradebookUid, siteId), assignmentId, getUserRoleOrNone(siteId));
	}

	/**
	 * Get a list of teaching assistants in the current site
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @return
	 */
	public List<GbUser> getTeachingAssistants(String gradebookUid, String siteId) {

		final List<GbUser> rval = new ArrayList<>();

		try {
			Set<String> userUuids = this.siteService.getSite(siteId).getUsersIsAllowed(GbRole.TA.getValue());
			if (!siteId.equals(gradebookUid)) {
				Group group = siteService.findGroup(gradebookUid);
				userUuids = group.getUsersIsAllowed(GbRole.TA.getValue());
			}
			for (final String userUuid : userUuids) {
				GbUser user = getUser(userUuid);
				if (user != null) {
					rval.add(getUser(userUuid));
				}
			}
		} catch (final IdUnusedException e) {
			log.warn("IdUnusedException trying to getTeachingAssistants", e);
		}

		return rval;
	}

	/**
	 * Update the permissions for the user. Note: These are currently only defined/used for a teaching assistant.
	 *
	 * @param gradebookUid
	 * @param userUuid
	 * @param permissions
	 */
	public void updatePermissionsForUser(final String gradebookUid, final String userUuid, final List<PermissionDefinition> permissions) {
		this.gradingPermissionService.updatePermissionsForUser(gradebookUid, userUuid, permissions);
	}

	/**
	 * Remove all permissions for the user. Note: These are currently only defined/used for users with the Teaching Assistant role.
	 *
	 * @param gradebookUid
	 * @param userUuid
	 */
	public void clearPermissionsForUser(final String gradebookUid, final String userUuid) {
		this.gradingPermissionService.clearPermissionsForUser(gradebookUid, userUuid);
	}

	/**
	 * Check if the course grade is visible to the user
	 *
	 * For TA's, the students are already filtered by permission so the TA won't see those they don't have access to anyway However if there
	 * are permissions and the course grade checkbox is NOT checked, then they explicitly do not have access to the course grade. So this
	 * method checks if the TA has any permissions assigned for the site, and if one of them is the course grade permission, then they have
	 * access.
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param userUuid user to check
	 * @return boolean
	 */
	public boolean isCourseGradeVisible(String gradebookUid, String siteId, String userUuid) {
        return gradingService.isCourseGradeVisible(gradebookUid, siteId, userUuid);
	}

	/**
	 * Are student numbers visible to the current user in the current site?
	 *
	 * @param siteId
	 * @return true if student numbers are visible
	 */
	public boolean isStudentNumberVisible(final String siteId)
	{
		if (getCandidateDetailProvider() == null) {
			return false;
		}

		final User user = getCurrentUser();
		final Optional<Site> site = getSite(siteId);
		return user != null && site.isPresent() && getCandidateDetailProvider().isInstitutionalNumericIdEnabled(site.get())
				&& this.gradingService.currentUserHasViewStudentNumbersPerm(siteId);
	}

	/**
	 * Are there any sections in the current site?
	 */
	public boolean isSectionsVisible(String siteId) {

		final Optional<Site> site = getSite(siteId);
		return site.isPresent() && !sectionManager.getSections(site.get().getId()).isEmpty();
	}

	/**
	 * Get the currently configured gradebook category type
	 *
	 * @return GradingCategoryType int value
	 */
	public Integer getGradebookCategoryType(String gradebookUid, String siteId) {
		final Gradebook gradebook = getGradebook(gradebookUid, siteId);
		return gradebook.getCategoryType();
	}

	/**
	 * Update the course grade (override) for this student
	 *
	 * @param gradebookUid
	 * @param siteId
	 * @param studentUuid uuid of the student
	 * @param grade the new grade
	 * @return
	 */
	public boolean updateCourseGrade(final String gradebookUid, final String siteId, final String studentUuid, final String grade, final String gradeScale) {

		final Gradebook gradebook = getGradebook(gradebookUid, siteId);

		try {
			gradingService.updateCourseGradeForStudent(gradebookUid, siteId, studentUuid, grade, gradeScale);
			EventHelper.postOverrideCourseGradeEvent(gradebook, studentUuid, grade, grade != null);
			return true;
		} catch (final Exception e) {
			log.error("An error occurred saving the course grade. {}: {}", e.getClass(), e.getMessage());
		}

		return false;
	}

	/**
	 * Get the user's preferred locale from the Sakai resource loader
	 *
	 * @return
	 */
	public Locale getUserPreferredLocale() {
		final ResourceLoader rl = new ResourceLoader();
		return rl.getLocale();
	}

	/**
	 * Helper to check if a user is roleswapped
	 *
	 * @return true if ja, false if nay.
	 */
	public boolean isUserRoleSwapped() {
		return securityService.isUserRoleSwapped();
	}

	/**
	 * Check if current user has "gradebook.editAssignments" permission 
	 *
	 * @return true if yes, false if no.
	 */
	public boolean isUserAbleToEditAssessments(String siteId) {
		return gradingService.currentUserHasEditPerm(siteId);
	}

	/**
	 * Returns true if the given grade is numeric and meets the gradebook requirements (10 digits/2 decimal places max)
	 * @param grade the grade to be validated, expected to be numeric
	 * @return true if the grade is numeric and meets the gradebook requirements
	 */
	public boolean isValidNumericGrade(String grade)
	{
		return gradingService.isValidNumericGrade(grade);
	}

	/**
	 * Helper to determine the icon class to use depending on the assignment external source
	 *
	 * @param assignment
	 * @return
	 */
	public String getIconClass(final Assignment assignment) {
		final String externalAppName = assignment.getExternalAppName();
		String iconClass;
		switch (externalAppName) {
			case AssignmentConstants.TOOL_ID:
				iconClass = getAssignmentsIconClass();
				break;
			case "sakai.samigo":
				iconClass = getSamigoIconClass();
				break;
			case "sakai.lessonbuildertool":
				iconClass = getLessonBuilderIconClass();
				break;
			case "sakai.attendance":
				iconClass = getAttendanceIconClass();
				break;
			default:
				iconClass = getDefaultIconClass();
				break;
		}
		return iconClass;
	}

	/**
	 * Helper to determine the icon class for possible external app names
	 *
	 * @return
	 */
	public Map<String, String> getIconClassMap() {
		final Map<String, String> mapping = new HashMap<>();

		mapping.put(AssignmentConstants.TOOL_ID, getAssignmentsIconClass());
		mapping.put("sakai.samigo", getSamigoIconClass());
		mapping.put("sakai.lessonbuildertool", getLessonBuilderIconClass());
		mapping.put("sakai.attendance", getAttendanceIconClass());

		return mapping;
	}

	public String getDefaultIconClass() {
		return ICON_SAKAI + "default-tool bi bi-globe-americas";
	}

	private String getAssignmentsIconClass() {
		return ICON_SAKAI + "sakai-assignment-grades";
	}

	private String getSamigoIconClass() {
		return ICON_SAKAI + "sakai-samigo";
	}

	private String getLessonBuilderIconClass() {
		return ICON_SAKAI + "sakai-lessonbuildertool";
	}

	private String getAttendanceIconClass() {
		return ICON_SAKAI + "sakai-attendance";
	}

	/**
	 * Gets a list of assignment averages for a category.
	 * @param gradebookUid
	 * @param siteId
	 * @param category category
	 * @param group group of students - apparently never used so far
	 * @return allAssignmentGrades list of assignment averages for a specific group
	 */
	public List<Double> getCategoryAssignmentTotals(String gradebookUid, String siteId, CategoryDefinition category, String group){
		final List<Double> allAssignmentGrades = new ArrayList<>();
		final List<String> groupUsers = gradingService.getGradeableUsers(gradebookUid, siteId, group);
		final List<String> studentUUIDs = new ArrayList<>();
		studentUUIDs.addAll(groupUsers);
		final List<Assignment> assignments = category.getAssignmentList();
		final List<GbStudentGradeInfo> grades = gradingService.buildGradeMatrix(gradebookUid, siteId, assignments, studentUUIDs, null);
		for (final Assignment assignment : assignments) {
			if (assignment != null) {
				final List<Double> allGrades = new ArrayList<>();
				for (int j = 0; j < grades.size(); j++) {
					final GbStudentGradeInfo studentGradeInfo = grades.get(j);
					final Map<Long, GbGradeInfo> studentGrades = studentGradeInfo.getGrades();
					final GbGradeInfo grade = studentGrades.get(assignment.getId());
					if (grade != null && grade.getGrade() != null) {
						allGrades.add(Double.valueOf(grade.getGrade()));
					}
				}
				if (grades.size() > 0) {
					if (!assignment.getExtraCredit()) {
						if (allGrades.size() > 0) {
							allAssignmentGrades.add((calculateAverage(allGrades) / assignment.getPoints()) * 100);
						}
					}
				}
			}
		}
		return allAssignmentGrades;
	}

	/**
	 * Calculates the average grade for an assignment
	 * @param allGrades list of grades
	 * @return the average of the grades
	 */
	public double calculateAverage(final List<Double> allGrades) {
		return allGrades.stream().reduce(0D, (sub, el) -> sub + el.doubleValue()) / allGrades.size();
	}

	// Return a CandidateDetailProvider or null if it's not enabled
	private CandidateDetailProvider getCandidateDetailProvider() {
		return (CandidateDetailProvider)ComponentManager.get("org.sakaiproject.user.api.CandidateDetailProvider");
	}

	/**
	 * Add advisor as allowed.
	 *
	 * @return
	 */
	public SecurityAdvisor addSecurityAdvisor() {
		final SecurityAdvisor advisor = (final String userId, final String function, final String reference) -> SecurityAdvice.ALLOWED;
		this.securityService.pushAdvisor(advisor);
		return advisor;
	}

	/**
	 * Remove advisor
	 *
	 * @param advisor
	 */
	public void removeSecurityAdvisor(final SecurityAdvisor advisor) {
		this.securityService.popAdvisor(advisor);
	}

	public boolean getShowCalculatedGrade() {
		return  this.serverConfigService.getBoolean("gradebook.coursegrade.showCalculatedGrade", true) ;
	}

	/**
	 * Get the date and time formatted via the UserTimeService
	 * @param dateGraded
	 * @return
	 */
	public String formatDateTime(Date dateTime) {
		return userTimeService.dateTimeFormat(dateTime, getUserPreferredLocale(), DateFormat.SHORT);
	}


	/**
	 * Get the date formatted by the UserTimeService
	 * @param date
	 * @param ifNull string to return if date is null
	 * @return
	 */
	public String formatDate(Date date, final String ifNull) {
		if (date == null) {
			return ifNull;
		}

		return userTimeService.dateFormat(date, getUserPreferredLocale(), DateFormat.SHORT);
	}

	/**
	 * Get the tool title in the current user language
	 * @param externalAppId tool id
	 * @return a tool title in user's current language
	 */
	public String getExternalAppName(String externalAppId) {
		Tool externalTool = toolManager.getTool(externalAppId);
		return externalTool != null ? externalTool.getTitle() : externalAppId;
	}

	public String getExternalSubmissionId(String externalId, String userId) {

		String assignmentId = AssignmentReferenceReckoner.reckoner().reference(externalId).reckon().getId();

		try {
			AssignmentSubmission as = assignmentService.getSubmission(assignmentId, userId);

			if (as == null) {
				throw new IllegalArgumentException("No submission for external id " + externalId + " and user " + userId);
			}

			return as.getId();
		} catch (Exception e) {
			log.error("Exception while getting external submission: {}", e.toString());
			return "";
		}
	}

	/**
	 * Get all grade definitions for a student including comments in one bulk operation.
	 *
	 * @param gradebookUid the gradebook uid
	 * @param siteId the site id
	 * @param studentUuid the student's uuid
	 * @param assignmentIds list of assignment IDs to fetch grades for
	 * @return map of assignment ID to GradeDefinition (with comments if available)
	 */
	public Map<Long, GradeDefinition> getAllGradeDefinitionsWithCommentsForStudent(final String gradebookUid, final String siteId, 
			final String studentUuid, final List<Long> assignmentIds) {
		
		if (assignmentIds == null || assignmentIds.isEmpty()) {
			log.debug("No assignment IDs provided for bulk grade fetch with comments for student: {}", studentUuid);
			return new HashMap<>();
		}

		log.debug("Fetching {} grade definitions with comments in bulk for student: {}", assignmentIds.size(), studentUuid);
		
		final Map<Long, GradeDefinition> gradeMap = new HashMap<>();
		
		// Use the new bulk method for fetching grades with comments
		final Map<Long, List<GradeDefinition>> bulkGrades = this.gradingService.getGradesWithCommentsForStudentsForItems(
				gradebookUid, siteId, assignmentIds, Collections.singletonList(studentUuid));

		// Extract grades for the single student from the bulk result
		for (final Map.Entry<Long, List<GradeDefinition>> entry : bulkGrades.entrySet()) {
			final Long assignmentId = entry.getKey();
			final List<GradeDefinition> gradeDefinitions = entry.getValue();
			
			// Find the grade for our specific student
			for (final GradeDefinition def : gradeDefinitions) {
				if (def != null && studentUuid.equals(def.getStudentUid())) {
					gradeMap.put(assignmentId, def);
					break; // Only one grade per student per assignment
				}
			}
		}
		
		log.debug("Retrieved {} grade definitions with comments for student: {}", gradeMap.size(), studentUuid);
		return gradeMap;
	}

	public List<PermissionDefinition> getPermissionsForUser(String userId, String gradebookId, String siteId) {
		return gradingService.getPermissionsForUser(userId, gradebookId, siteId);
	}

	public Map<String, List<String>> getGroupMemberships(String gradebookId, String siteId) {
		return gradingService.getGroupMemberships(gradebookId, siteId);
	}

	/**
	 * Get a list of sections and groups in a site
	 *
	 * @param gradebookUid the gradebook to get sections/groups for
	 * @param siteId the site id to get sections/groups for
	 * @return a list of sections and groups in the site
	 */
	public List<GbGroup> getSiteSectionsAndGroups(String gradebookUid, String siteId) {
		return gradingService.getSiteSectionsAndGroups(gradebookUid, siteId);
	}
}
