/**
 * Copyright (c) 2003-2018 The Apereo Foundation
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
package org.sakaiproject.gradebookng.tool.panels;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.gradebookng.tool.component.GbAjaxLink;
import org.sakaiproject.gradebookng.tool.stats.CourseGradeStatistics;
import org.sakaiproject.grading.api.GradebookInformation;

/**
 * Renders the course grade graph in a modal window
 */
public class CourseGradeStatisticsPanel extends BasePanel {

	private static final long serialVersionUID = 1L;

	private final ModalWindow window;

	public CourseGradeStatisticsPanel(final String id, final ModalWindow window) {
		super(id);
		this.window = window;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onInitialize() {
		super.onInitialize();

		CourseGradeStatisticsPanel.this.window.setTitle(new ResourceModel("label.statistics.title.coursegrade"));

		final WebMarkupContainer chart = new WebMarkupContainer("chart");
		String siteId = getCurrentSiteId();
		String dataUrl = "/api/sites/" + siteId + "/grades/" + siteId + "/courseGrades";
		chart.add(AttributeModifier.append("data-url", dataUrl));
		add(chart);

		final CourseGradeStatistics stats = new CourseGradeStatistics("stats", getData());
		add(stats);

		add(new GbAjaxLink<Void>("done") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(final AjaxRequestTarget target) {
				CourseGradeStatisticsPanel.this.window.close(target);
			}
		});
	}

	/**
	 * Get the course grade data for the site and wrap it
	 *
	 * @return
	 */
	private IModel<Map<String, Object>> getData() {
		final Map<String, Object> data = new HashMap<>();
		final List<String> studentUuids = gradingService.getGradeableUsers(currentGradebookUid, currentSiteId, null);
		data.put("courseGradeMap", gradingService.getCourseGradeForStudents(currentGradebookUid, currentSiteId, studentUuids));

		final GradebookInformation info = this.businessService.getGradebookSettings(currentGradebookUid, currentSiteId);;
		data.put("gradingSchemaName", info.getGradeScale());
		data.put("bottomPercents", info.getSelectedGradingScaleBottomPercents());

		return Model.ofMap(data);
	}

}
