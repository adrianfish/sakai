import { SakaiGradesChart } from "./SakaiGradesChart.js";

export class SakaiCourseGradesChart extends SakaiGradesChart {

  _getIndexAxis() {
    return "y";
  }

  _getTitle() {
    return this._i18n.course_grades_chart_title;
  }

  _getXLabel() {
    return this._i18n.course_grades_chart_x_label;
  }

  _getYLabel() {
    return this._i18n.course_grades_chart_y_label;
  }
}
