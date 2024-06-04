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
package org.sakaiproject.assignment.tool;

import static org.sakaiproject.assignment.api.AssignmentConstants.*;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.assignment.api.AssignmentReferenceReckoner;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.model.Assignment;
import org.sakaiproject.assignment.api.model.AssignmentSubmission;
import org.sakaiproject.assignment.api.model.AssignmentSubmissionSubmitter;
import org.sakaiproject.cheftool.VelocityPortletPaneledAction;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.event.api.SessionState;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.grading.api.AssessmentNotFoundException;
import org.sakaiproject.grading.api.AssignmentHasIllegalPointsException;
import org.sakaiproject.grading.api.ConflictingAssignmentNameException;
import org.sakaiproject.grading.api.GradingService;
import org.sakaiproject.grading.api.InvalidGradeItemNameException;
import org.sakaiproject.lti.api.LTIService;
import org.sakaiproject.rubrics.api.RubricsConstants;
import org.sakaiproject.rubrics.api.RubricsService;
import org.sakaiproject.rubrics.api.model.ToolItemRubricAssociation;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.api.FormattedText;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class AssignmentToolUtils {

    private static FormattedText formattedText;

    static {
        formattedText = ComponentManager.get(FormattedText.class);
    }

    private AssignmentService assignmentService;
    private UserDirectoryService userDirectoryService;
    private GradingService gradingService;
    private RubricsService rubricsService;
    private TimeService timeService;
    private ToolManager toolManager;
    private LTIService ltiService;

    private static ResourceLoader rb = new ResourceLoader("assignment");

    /**
     * scale the point value by "factor" if there is a valid point grade
     */
    public String scalePointGrade(String point, int factor, List<String> alerts) {

        String decSeparator = formattedText.getDecimalSeparator();
        int dec = (int) Math.log10(factor);

        alerts.addAll(validPointGrade(point, factor));

        if (point != null && (point.length() >= 1)) {
            // when there is decimal points inside the grade, scale the number by "factor"
            // but only one decimal place is supported
            // for example, change 100.0 to 1000
            int index = point.indexOf(decSeparator);
            if (index != -1) {
                if (index == 0) {
                    int trailingData = point.substring(1).length();
                    // if the point is the first char, add a 0 for the integer part
                    point = "0".concat(point.substring(1));
                    // ensure that the point value has the correct # of decimals
                    // by padding with zeros
                    if (trailingData < dec) {
                        for (int i = trailingData; i < dec; i++) {
                            point = point + "0";
                        }
                    }
                } else if (index < point.length() - 1) {
                    // adjust the number of decimals, adding 0's to the end
                    int length = point.length() - index - 1;
                    for (int i = length; i < dec; i++) {
                        point = point + "0";
                    }

                    // use scale integer for gradePoint
                    point = point.substring(0, index) + point.substring(index + 1);
                } else {
                    // decimal point is the last char
                    point = point.substring(0, index);
                    for (int i = 0; i < dec; i++) {
                        point = point + "0";
                    }
                }
            } else {
                // if there is no decimal place, scale up the integer by "factor"
                for (int i = 0; i < dec; i++) {
                    point = point + "0";
                }
            }

            // filter out the "zero grade"
            if ("00".equals(point)) {
                point = "0";
            }
        }

        if (StringUtils.trimToNull(point) != null) {
            try {
                point = Integer.valueOf(point).toString();
            } catch (Exception e) {
                //log.warn(this + " scalePointGrade: cannot parse " + point + " into integer. " + e.getMessage());
            }
        }
        return point;

    } // scalePointGrade

    /**
     * Tests the format of the supplied grade and sets alert messages in the
     * state as required.
     */
    public List<String> validPointGrade(final String grade, int factor) {

        List<String> alerts = new ArrayList<>();

        if (grade != null && !"".equals(grade)) {
            if (grade.startsWith("-")) {
                // check for negative sign
                alerts.add(rb.getString("plesuse3"));
            } else {
                int dec = (int) Math.log10(factor);
                NumberFormat nbFormat = formattedText.getNumberFormat();
                String decSeparator = formattedText.getDecimalSeparator();

                // only the right decimal separator is allowed and no other grouping separator
                if ((",".equals(decSeparator) && grade.contains("."))
                        || (".".equals(decSeparator) && grade.contains(","))
                        || grade.contains(" ")) {
                    alerts.add(rb.getString("plesuse1"));
                    return alerts;
                }

                // parse grade from localized number format
                int index = grade.indexOf(decSeparator);
                if (index != -1) {
                    // when there is decimal points inside the grade, scale the number by "factor"
                    // but only one decimal place is supported
                    // for example, change 100.0 to 1000
                    if (!decSeparator.equals(grade)) {
                        if (grade.length() > index + dec + 1) {
                            // if there are more than "factor" decimal points
                            alerts.add(rb.getFormattedMessage("plesuse2", String.valueOf(dec)));
                        } else {
                            // decimal points is the only allowed character inside grade
                            // replace it with '1', and try to parse the new String into int
                            String zeros = "";
                            for (int i = 0; i < dec; i++) {
                                zeros = zeros.concat("0");
                            }
                            String gradeString = grade.endsWith(decSeparator) ? grade.substring(0, index).concat(zeros) :
                                    grade.substring(0, index).concat(grade.substring(index + 1));
                            try {
                                nbFormat.parse(gradeString);
                                try {
                                    Integer.parseInt(gradeString);
                                } catch (NumberFormatException e) {
                                    //log.warn(this + ":validPointGrade " + e.getMessage());
                                    alerts.addAll(alertInvalidPoint(gradeString, factor));
                                }
                            } catch (ParseException e) {
                                //log.warn(this + ":validPointGrade " + e.getMessage());
                                alerts.add(rb.getString("plesuse1"));
                            }
                        }
                    } else {
                        // grade is decSeparator
                        alerts.add(rb.getString("plesuse1"));
                    }
                } else {
                    // There is no decimal point; should be int number
                    String gradeString = grade;
                    for (int i = 0; i < dec; i++) {
                        gradeString = gradeString.concat("0");
                    }
                    try {
                        nbFormat.parse(gradeString);
                        try {
                            Integer.parseInt(gradeString);
                        } catch (NumberFormatException e) {
                            //log.warn(this + ":validPointGrade " + e.getMessage());
                            alerts.addAll(alertInvalidPoint(gradeString, factor));
                        }
                    } catch (ParseException e) {
                        //log.warn(this + ":validPointGrade " + e.getMessage());
                        alerts.add(rb.getString("plesuse1"));
                    }
                }
            }
        }

        return alerts;
    }

    public List<String> alertInvalidPoint(String grade, int factor) {

        List<String> alerts = new ArrayList<>();

        String decSeparator = formattedText.getDecimalSeparator();

        String VALID_CHARS_FOR_INT = "-01234567890";

        boolean invalid = false;
        // case 1: contains invalid char for int
        for (int i = 0; i < grade.length() && !invalid; i++) {
            char c = grade.charAt(i);
            if (VALID_CHARS_FOR_INT.indexOf(c) == -1) {
                invalid = true;
            }
        }
        if (invalid) {
            alerts.add(rb.getString("plesuse1"));
        } else {
            int dec = (int) Math.log10(factor);
            int maxInt = Integer.MAX_VALUE / factor;
            int maxDec = Integer.MAX_VALUE - maxInt * factor;
            // case 2: Due to our internal scaling, input String is larger than Integer.MAX_VALUE/10
            alerts.add(rb.getFormattedMessage("plesuse4", grade.substring(0, grade.length() - dec)
                    + decSeparator + grade.substring(grade.length() - dec), maxInt + decSeparator + maxDec));
        }

        return alerts;
    }

    /**
     * Common grading routine plus specific operation to differentiate cases when saving, releasing or returning grade.
     */
    public void gradeSubmission(AssignmentSubmission submission, String gradeOption, Map<String, Object> options, List<String> alerts) {

        if (submission == null) return;

        Assignment a = submission.getAssignment();
        String grade = (String) options.get(GRADE_SUBMISSION_GRADE);

        boolean gradeChanged = !Objects.equals(StringUtils.trimToNull(submission.getGrade()), StringUtils.trimToNull(grade));

        // the instructor feedback comment
        String submittedfeedbackComment = StringUtils.trimToNull((String) options.get(GRADE_SUBMISSION_FEEDBACK_COMMENT));
        submission.setFeedbackComment(submittedfeedbackComment);

        // the instructor inline feedback
        submission.setFeedbackText(StringUtils.trimToNull((String) options.get(GRADE_SUBMISSION_FEEDBACK_TEXT)));

        List<Reference> submittedfeedbackAttachments = (List<Reference>) options.get(GRADE_SUBMISSION_FEEDBACK_ATTACHMENT);
        if (submittedfeedbackAttachments != null) {
            // clear the old attachments first
            Set<String> feedbackAttachments = submission.getFeedbackAttachments();
            if (BooleanUtils.isFalse((Boolean) options.get(GRADE_SUBMISSION_DONT_CLEAR_CURRENT_ATTACHMENTS))) {
                feedbackAttachments.clear();
            }
            for (Reference attachment : submittedfeedbackAttachments) {
                feedbackAttachments.add(attachment.getReference());
            }
        }

        submission.setPrivateNotes(StringUtils.trimToNull((String) options.get(GRADE_SUBMISSION_PRIVATE_NOTES)));

        // determine if the submission is graded
        if (a.getTypeOfGrade().equals(Assignment.GradeType.UNGRADED_GRADE_TYPE)) {
            submission.setGrade(null);
            submission.setGraded(submittedfeedbackComment != null);
        } else {
            if (StringUtils.isNotBlank(grade)) {
                // if there is a grade then the submission is graded
                submission.setGraded(true);
                submission.setGrade(grade);
                if (gradeChanged) {
                    submission.setGradedBy(userDirectoryService.getCurrentUser() == null ? null : userDirectoryService.getCurrentUser().getId());
                }
            } else {
                // if no grade or feedback left then it is not graded
                submission.setGrade(null);
                submission.setGraded(false);
                if (gradeChanged) {
                    submission.setGradedBy(null);
                }
            }
        }

        if (a.getIsGroup()) {
            // group project only set a grade override for submitters
            for (AssignmentSubmissionSubmitter submitter : submission.getSubmitters()) {
                String submitterGradeOverride = StringUtils.trimToNull((String) options.get(GRADE_SUBMISSION_GRADE + "_" + submitter.getSubmitter()));
                if (!Objects.equals(submitterGradeOverride, submitter.getGrade())) {
                    submitter.setGrade(submitterGradeOverride);
                }
            }
        }

        if (SUBMISSION_OPTION_RELEASE.equals(gradeOption)) {
            submission.setGradeReleased(true);
            submission.setReturned(false);
            submission.setDateReturned(null);
        } else if (SUBMISSION_OPTION_RETURN.equals(gradeOption)) {
            submission.setGradeReleased(true);
            submission.setReturned(true);
            submission.setDateReturned(Instant.now());
        } else if (SUBMISSION_OPTION_RETRACT.equals(gradeOption)) {
            submission.setGradeReleased(false);
            submission.setReturned(false);
            submission.setDateReturned(null);
        }

        Map<String, String> properties = submission.getProperties();
        if (options.get(ALLOW_RESUBMIT_NUMBER) != null) {
            // get resubmit number
            properties.put(ALLOW_RESUBMIT_NUMBER, (String) options.get(ALLOW_RESUBMIT_NUMBER));

            if (options.get(ALLOW_RESUBMIT_CLOSE_YEAR) != null) {
                // get resubmit time
                Instant closeTime = getTimeFromOptions(options, ALLOW_RESUBMIT_CLOSE_MONTH, ALLOW_RESUBMIT_CLOSE_DAY, ALLOW_RESUBMIT_CLOSE_YEAR, ALLOW_RESUBMIT_CLOSE_HOUR, ALLOW_RESUBMIT_CLOSE_MIN);
                properties.put(ALLOW_RESUBMIT_CLOSETIME, String.valueOf(closeTime.toEpochMilli()));
            } else if (options.get(ALLOW_RESUBMIT_CLOSE_EPOCH_MILLIS) != null) {
                properties.put(ALLOW_RESUBMIT_CLOSETIME, (String) options.get(ALLOW_RESUBMIT_CLOSE_EPOCH_MILLIS));
            } else {
                properties.remove(ALLOW_RESUBMIT_CLOSETIME);
            }
        } else {
            // clean resubmission property
            properties.remove(ALLOW_RESUBMIT_CLOSETIME);
            properties.remove(ALLOW_RESUBMIT_NUMBER);
        }

        if (options.get(ALLOW_EXTENSION_CLOSETIME) != null){  //put State's info about extension into the Submission properties.
            Instant extensionDeadline = getTimeFromOptions(options, ALLOW_EXTENSION_CLOSE_MONTH, ALLOW_EXTENSION_CLOSE_DAY, ALLOW_EXTENSION_CLOSE_YEAR, ALLOW_EXTENSION_CLOSE_HOUR, ALLOW_EXTENSION_CLOSE_MIN);
            properties.put(ALLOW_EXTENSION_CLOSETIME, String.valueOf(extensionDeadline.toEpochMilli()));
        } else if (options.get(ALLOW_EXTENSION_CLOSE_EPOCH_MILLIS) != null) {
            properties.put(ALLOW_EXTENSION_CLOSETIME, (String) options.get(ALLOW_EXTENSION_CLOSE_EPOCH_MILLIS));
        } else { //if it's null, no need for it to be in Properties.
            properties.remove(ALLOW_EXTENSION_CLOSETIME);
        }

        // save a timestamp for this grading process
        properties.put(PROP_LAST_GRADED_DATE, DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(ZoneId.systemDefault()).format(Instant.now()));

        try {
            assignmentService.updateSubmission(submission);
        } catch (PermissionException e) {
            log.warn("Could not update submission: {}, {}", submission.getId(), e.getMessage());
            return;
        }

        // update grades in gradebook
        String gbItemId = a.getProperties().get(PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);

        if (gradeOption.equals("remove")) {
            alerts.addAll(removeGradeFromGradingItem(a, submission, Long.parseLong(gbItemId)));
        } else {
            alerts.addAll(updateGradeForGradingItem(a, submission, Long.parseLong(gbItemId)));
        }
    } // gradeSubmission

    /**
     * construct time object based on various state variables
     *
     * @param state
     * @param monthString
     * @param dayString
     * @param yearString
     * @param hourString
     * @param minString
     * @return
     */
    private Instant getTimeFromOptions(Map<String, Object> options, String monthString, String dayString, String yearString, String hourString, String minString) {

        if (options.get(monthString) != null ||
                options.get(dayString) != null ||
                options.get(yearString) != null ||
                options.get(hourString) != null ||
                options.get(minString) != null) {
            int month = (Integer) options.get(monthString);
            int day = (Integer) options.get(dayString);
            int year = (Integer) options.get(yearString);
            int hour = (Integer) options.get(hourString);
            int min = (Integer) options.get(minString);
            return LocalDateTime.of(year, month, day, hour, min, 0).atZone(timeService.getLocalTimeZone().toZoneId()).toInstant();
        } else {
            return null;
        }
    }

    /**
     * integration with gradebook
     *
     * @param state
     * @param assignment                   Assignment
     * @param gbItemId                     The gb item id for the associated GB assignment
     * @param assignmentOp                 "add" for adding the assignment; "update" for updating the assignment; "remove" for remove assignment
     * @param submissionRef                Any submission grade need to be updated? Do bulk update if null
     * @param submissionOp                 "update" for update submission;"remove" for remove submission
     * @param category                     the category id
     */
    List<String> integrateGradebook(Map<String, Object> options, Assignment assignment, Long gbItemId,
            String assignmentOp, String submissionRef, String submissionOp, long category) {

        Instant dueDate = assignment.getDueDate();
        int maxPoints = assignment.getMaxGradePoint();

        List<String> alerts = new ArrayList<>();

        String gradebookUid = assignment.getContext();

        if (!gradingService.currentUserHasGradingPerm(gradebookUid)) return alerts;

        String assignmentToolId = assignmentService.getToolId();
        String submissionId = AssignmentReferenceReckoner.reckoner().reference(submissionRef).reckon().getId();

        if (assignmentOp == null) return alerts;

        try {
            boolean isExternalAssignment = gradingService.isExternalAssignment(gbItemId);

            String newTitle = assignment.getTitle();
            // add an entry into Gradebook for newly created assignment or modified assignment, and there wasn't a correspond record in gradebook yet
            if (assignmentOp.equals(GRADEBOOK_INTEGRATION_ADD)) {
                if (!isExternalAssignment) {
                    String assignmentRef = AssignmentReferenceReckoner.reckoner().assignment(assignment).reckon().getReference();

                    org.sakaiproject.grading.api.Assignment existingExternalAssignment = null;
                    try {
                        existingExternalAssignment = gradingService.getExternalAssignment(gradebookUid, assignmentRef);
                        assignment.getProperties().put(PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT, existingExternalAssignment.getId().toString());
                        return alerts;
                    } catch (IllegalArgumentException iae) {
                    }

                    // add assignment into gradebook
                    try {
                        // add assignment to gradebook
                        Long newGbItemId = gradingService.addExternalAssessment(gradebookUid, assignmentRef, null, newTitle, maxPoints / (double) assignment.getScaleFactor(), Date.from(dueDate), assignmentToolId, null, false, category != -1 ? category : null, assignmentRef, (Boolean) options.get(AssignmentAction.DISPLAY_IN_GRADEBOOK));
                        assignment.getProperties().put(PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT, newGbItemId.toString());
                    } catch (AssignmentHasIllegalPointsException e) {
                        alerts.add(rb.getString("addtogradebook.illegalPoints"));
                        log.warn("integrateGradebook: {}", e.toString());
                    } catch (ConflictingAssignmentNameException e) {
                        // add alert prompting for change assignment title
                        alerts.add(rb.getFormattedMessage("addtogradebook.nonUniqueTitle", "\"" + newTitle + "\""));
                        log.warn("integrateGradebook: {}", e.toString());
                    } catch (InvalidGradeItemNameException e) {
                        // add alert prompting for invalid assignment title name
                        alerts.add(rb.getFormattedMessage("addtogradebook.titleInvalidCharacters", "\"" + newTitle + "\""));
                        log.warn("integrateGradebook: {}", e.toString());
                    } catch (Exception e) {
                        log.warn("integrateGradebook: {}", e.toString());
                    }
                } else {
                    try {
                        // update attributes if the GB assignment was created for the assignment
                        org.sakaiproject.grading.api.Assignment gbAssignment = gradingService.getAssignment(assignment.getContext(), gbItemId);
                        gbAssignment.setName(newTitle);
                        gbAssignment.setPoints(maxPoints / (double) assignment.getScaleFactor());
                        gbAssignment.setDueDate(Date.from(dueDate));
                        gbAssignment.setUngraded(false);
                        gbAssignment.setDisplayInGradebook((Boolean) options.get(AssignmentAction.DISPLAY_IN_GRADEBOOK));

                        gradingService.updateAssignment(gradebookUid, gbItemId, gbAssignment, true);
                    } catch (Exception e) {
                        alerts.add(rb.getFormattedMessage("cannotfin_assignment", gbItemId));
                        log.warn("{}: {}", rb.getFormattedMessage("cannotfin_assignment", gbItemId), e.toString());
                    }
                }
            } else if (assignmentOp.equals(GRADEBOOK_INTEGRATION_ASSOCIATE)) {
                System.out.println("HERE1");
                System.out.println(gbItemId);
                if (gbItemId != null) {
                    try {
                        org.sakaiproject.grading.api.Assignment gbAssignment = gradingService.getAssignment(assignment.getContext(), gbItemId);
                        gbAssignment.setId(gbItemId);
                        gbAssignment.setName(newTitle);
                        gbAssignment.setPoints(maxPoints / (double) assignment.getScaleFactor());
                        gbAssignment.setDueDate(Date.from(dueDate));
                        gbAssignment.setUngraded(false);
                        gbAssignment.setDisplayInGradebook((Boolean) options.get(AssignmentAction.DISPLAY_IN_GRADEBOOK));
                        gradingService.updateAssignment(gradebookUid, gbItemId, gbAssignment, true);
                    } catch (Exception e) {
                        alerts.add(rb.getFormattedMessage("cannotfin_assignment", gbItemId));
                        log.warn("{}: {}", rb.getFormattedMessage("cannotfin_assignment", gbItemId), e.toString());
                    }
                }
                assignment.getProperties().put(PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT, gbItemId.toString());
            }
        } catch (Exception e) {
            log.error("An exception occurred while integrating the grading item: {}", e.toString());
        }

        return alerts;
    } // integrateGradebook
    
    public List<String> updateGradesForGradingItem(Assignment assignment, Long gbItemId) {

        //Assignment scores map
        Map<String, String> sm = new HashMap<>();
        //Assignment comments map, though doesn't look like there's any way to update comments in bulk in the UI yet
        Map<String, String> cm = new HashMap<>();

        String gradebookUid = assignment.getContext();

        // bulk add all grades for assignment into gradebook
        for (AssignmentSubmission submission : assignmentService.getSubmissions(assignment)) {
            String gradeString = StringUtils.trimToNull(submission.getGrade());
            String commentString = formattedText.convertFormattedTextToPlaintext(submission.getFeedbackComment());

            String grade = gradeString != null ? displayGrade(gradeString, assignment.getScaleFactor()) : null;
            for (AssignmentSubmissionSubmitter submitter : submission.getSubmitters()) {
                String submitterId = submitter.getSubmitter();
                String submitterGrade = submitter.getGrade() != null ? displayGrade(submitter.getGrade(), assignment.getScaleFactor()) : null;
                String gradeStringToUse = (assignment.getIsGroup() && submitterGrade != null) ? submitterGrade : grade;
                sm.put(submitterId, gradeStringToUse);
                cm.put(submitterId, commentString);
            }
        }

        // need to update only when there is at least one submission
        if (!sm.isEmpty() && gbItemId != null) {
            // the associated assignment is internal one, update records one by one
            for (Map.Entry<String, String> entry : sm.entrySet()) {
                String submitterId = (String) entry.getKey();
                String grade = StringUtils.trimToNull(displayGrade((String) sm.get(submitterId), assignment.getScaleFactor()));
                if (grade != null && gradingService.isUserAbleToGradeItemForStudent(gradebookUid, gbItemId, submitterId)) {
                    gradingService.setAssignmentScoreString(gradebookUid, gbItemId, submitterId, grade, TOOL_ID);
                    String comment = StringUtils.isNotEmpty(cm.get(submitterId)) ? cm.get(submitterId) : "";
                    if (StringUtils.isNotBlank(comment)) {
                        gradingService.setAssignmentScoreComment(gradebookUid, gbItemId, submitterId, comment);
                    }
                }
            }
        }

        return Collections.<String>emptyList();
    }

    // only update one submission
    public List<String> updateGradeForGradingItem(Assignment assignment, AssignmentSubmission submission, Long gbItemId) {

        if (submission == null) {
            return Collections.<String>emptyList();
        }

        String gradebookUid = assignment.getContext();

        int factor = submission.getAssignment().getScaleFactor();
        Set<AssignmentSubmissionSubmitter> submitters = submission.getSubmitters();
        String gradeString = displayGrade(StringUtils.trimToNull(submission.getGrade()), factor);
        for (AssignmentSubmissionSubmitter submitter : submitters) {
            String gradeStringToUse = (assignment.getIsGroup() && submitter.getGrade() != null) ? displayGrade(StringUtils.trimToNull(submitter.getGrade()), factor) : gradeString;
            //Gradebook only supports plaintext strings
            String commentString = formattedText.convertFormattedTextToPlaintext(submission.getFeedbackComment());
            final String submitterId = submitter.getSubmitter();
            if (gradingService.isUserAbleToGradeItemForStudent(gradebookUid, gbItemId, submitterId)) {

                gradingService.setAssignmentScoreString(gradebookUid, gbItemId, submitterId,
                        gradeStringToUse != null  ? gradeStringToUse : "", TOOL_ID);
                gradingService.setAssignmentScoreComment(gradebookUid, gbItemId, submitterId,
                        commentString != null ? commentString : "");
            }
        }

        return Collections.<String>emptyList();
    }

    public List<String> removeGradesFromGradingItem(Assignment a, Long gbItemId) {

        String gradebookUid = a.getContext();

        assignmentService.getSubmissions(a).stream().filter(s -> StringUtils.isNotBlank(s.getGrade())).forEach(s -> {

            getSubmitters(s).map(ss -> ss.getId()).forEach(ssId -> {

                if (gradingService.isUserAbleToGradeItemForStudent(gradebookUid, gbItemId, ssId)) {
                    gradingService.setAssignmentScoreString(gradebookUid, gbItemId, ssId, "0", TOOL_ID);
                }
            });
        });

        return Collections.<String>emptyList();
    }

    public List<String> removeGradeFromGradingItem(Assignment a, AssignmentSubmission submission, Long gbItemId) {

        String gradebookUid = a.getContext();
        getSubmitters(submission).map(User::getId).forEach(submitterId -> {

            if (gradingService.isUserAbleToGradeItemForStudent(gradebookUid, gbItemId, submitterId)) {
                gradingService.setAssignmentScoreString(gradebookUid, gbItemId, submitterId, "0", TOOL_ID);
            }
        });
        return Collections.<String>emptyList();
    }

    /**
     * A utility class to find a gradebook column of a particular name
     */
    public org.sakaiproject.grading.api.Assignment findGradeBookColumn(String gradebookUid, String assignmentName) {
        try {
            return gradingService.getAssignmentByNameOrId(gradebookUid, assignmentName);
        } catch (AssessmentNotFoundException anfe) {
            return null;
        }
    }

    public Stream<User> getSubmitters(AssignmentSubmission aSubmission) {

		return assignmentService.getSubmissionSubmittersAsUsers(aSubmission).stream();
	}

    /**
     * Contains logic to consistently output a String based version of a grade
     * Interprets the grade using the scale for display
     *
     * This should probably be moved to a static utility class - ern
     *
     * @param grade
     * @param typeOfGrade
     * @param scaleFactor
     * @return
     */
    public String getGradeDisplay(String grade, Assignment.GradeType typeOfGrade, Integer scaleFactor) {
        String returnGrade = StringUtils.trimToEmpty(grade);
        if (scaleFactor == null) scaleFactor = assignmentService.getScaleFactor();

        switch (typeOfGrade) {
            case SCORE_GRADE_TYPE:
                if (!returnGrade.isEmpty() && !"0".equals(returnGrade)) {
                    int dec = new Double(Math.log10(scaleFactor)).intValue();
                    String decSeparator = formattedText.getDecimalSeparator();
                    String decimalGradePoint = returnGrade;
                    try {
                        Integer.parseInt(returnGrade);
                        // if point grade, display the grade with factor decimal place
                        if (returnGrade.length() > dec) {
                            decimalGradePoint = returnGrade.substring(0, returnGrade.length() - dec) + decSeparator + returnGrade.substring(returnGrade.length() - dec);
                        } else {
                            String newGrade = "0".concat(decSeparator);
                            for (int i = returnGrade.length(); i < dec; i++) {
                                newGrade = newGrade.concat("0");
                            }
                            decimalGradePoint = newGrade.concat(returnGrade);
                        }
                    } catch (NumberFormatException nfe1) {
                        log.debug("Could not parse grade [{}] as an Integer trying as a Float, {}", returnGrade, nfe1.getMessage());
                        try {
                            Float.parseFloat(returnGrade);
                            decimalGradePoint = returnGrade;
                        } catch (NumberFormatException nfe2) {
                            log.debug("Could not parse grade [{}] as a Float, {}", returnGrade, nfe2.getMessage());
                        }
                    }
                    // get localized number format
                    NumberFormat nbFormat = formattedText.getNumberFormat(dec, dec, false);
                    DecimalFormat dcformat = (DecimalFormat) nbFormat;
                    // show grade in localized number format
                    try {
                        Double dblGrade = dcformat.parse(decimalGradePoint).doubleValue();
                        decimalGradePoint = nbFormat.format(dblGrade);
                        returnGrade = decimalGradePoint;
                    } catch (Exception e) {
                        log.warn("Could not parse grade [{}], {}", returnGrade, e.getMessage());
                    }
                }
                break;
            case UNGRADED_GRADE_TYPE:
                if (returnGrade.equalsIgnoreCase("gen.nograd")) {
                    returnGrade = rb.getString("gen.nograd");
                }
                break;
            case PASS_FAIL_GRADE_TYPE:
                if (returnGrade.equalsIgnoreCase("Pass")) {
                    returnGrade = rb.getString("pass");
                } else if (returnGrade.equalsIgnoreCase("Fail")) {
                    returnGrade = rb.getString("fail");
                } else {
                    returnGrade = rb.getString("ungra");
                }
                break;
            case CHECK_GRADE_TYPE:
                if (returnGrade.equalsIgnoreCase("Checked")) {
                    returnGrade = rb.getString("gen.checked");
                } else {
                    returnGrade = rb.getString("ungra");
                }
                break;
            default:
                if (returnGrade.isEmpty()) {
                    returnGrade = rb.getString("ungra");
                }
        }
        return returnGrade;
    }

    public boolean isDraftSubmission(AssignmentSubmission s) {

        return (!s.getSubmitted()
            && ((s.getSubmittedText() != null && s.getSubmittedText().length() > 0)
            || (s.getAttachments() != null && s.getAttachments().size() > 0)));
    }

    private String displayGrade(String grade, Integer factor) {
        return assignmentService.getGradeDisplay(grade, Assignment.GradeType.SCORE_GRADE_TYPE, factor);
    }

    public void removeNonAssociatedExternalGradebookEntry(String context, Long gbItemId) {

        if (!gradingService.isExternalAssignment(gbItemId)) return;

        // iterate through all assignments currently in the site, see if any is associated with this GB entry
        if (assignmentService.getAssignmentsForContext(context).stream()
            .anyMatch(a -> Objects.equals(a.getProperties().get(PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT), gbItemId.toString()))) {
            // so if none of the assignment in this site is associated with the entry, remove the entry
            gradingService.removeAssignment(gbItemId, Boolean.FALSE);
        }
    }

    public void restoreGradingItem(Long gbItemId) {

        gradingService.restoreExternalAssignment(gbItemId);
    }

    public boolean hasRubricSelfReview(String assignmentId) {
        try {
            Optional<ToolItemRubricAssociation> rubricAssociation = rubricsService.getRubricAssociation(RubricsConstants.RBCS_TOOL_ASSIGNMENT_GRADES, assignmentId);
            if (rubricAssociation.isPresent()) {
                return Integer.valueOf(1).equals(rubricAssociation.get().getParameters().get(RubricsConstants.RBCS_STUDENT_SELF_REPORT));
            }
        } catch (Exception e) {
            log.warn("Error trying to retrieve rubrics association for assignment : {}", e.getMessage());
        }
        return false;
    }

    public int getRubricSelfReviewMode(String assignmentId) {
        try {
            Optional<ToolItemRubricAssociation> rubricAssociation = rubricsService.getRubricAssociation(RubricsConstants.RBCS_TOOL_ASSIGNMENT_GRADES, assignmentId);
            if (rubricAssociation.isPresent()) {
                return rubricAssociation.get().getParameters().get(RubricsConstants.RBCS_STUDENT_SELF_REPORT_MODE);
            }
        } catch (Exception e) {
            log.warn("Error trying to retrieve rubrics association for assignment : {}", e.getMessage());
        }
        return -1;
    }

    public boolean hasRubricHiddenToStudent(String assignmentId) {
        try {
            Optional<ToolItemRubricAssociation> rubricAssociation = rubricsService.getRubricAssociation(RubricsConstants.RBCS_TOOL_ASSIGNMENT_GRADES, assignmentId);
            if (rubricAssociation.isPresent()) {
                return Integer.valueOf(1).equals(rubricAssociation.get().getParameters().get("hideStudentPreview"));
            }
        } catch (Exception e) {
            log.warn("Error trying to retrieve rubrics association for assignment : {}", e.getMessage());
        }
        return false;
    }

    public String displayGrade(SessionState state, String grade, Integer factor) {

        String currentStateMessage = (String) state.getAttribute(VelocityPortletPaneledAction.STATE_MESSAGE);
        if (currentStateMessage == null || currentStateMessage.startsWith(rb.getString("pleasee6"))) {
            if (StringUtils.isNotBlank(grade)) {
                grade = assignmentService.getGradeDisplay(grade, Assignment.GradeType.SCORE_GRADE_TYPE, factor);
            } else {
                grade = "";
            }
        }
        return grade;
    }
}
