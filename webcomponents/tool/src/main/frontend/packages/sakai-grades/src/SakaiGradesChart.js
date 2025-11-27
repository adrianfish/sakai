import { html, nothing } from "lit";
import { SakaiShadowElement } from "@sakai-ui/sakai-element";
import Chart from "chart.js/auto";

export class SakaiGradesChart extends SakaiShadowElement {

  static properties = {
    dataUrl: { attribute: "data-url", type: String },

    _loading: { state: true },
  };

  constructor() {

    super();

    this._loading = true;

    this._i18nLoaded = this.loadTranslations("grades");
  }

  firstUpdated() {

    this._renderChart();
    this._loadData();
  }

  _getTitle() {
    return "Title";
  }

  _getXLabel() {
    return "X Label";
  }

  _getYLabel() {
    return "Y Label";
  }

  _getIndexAxis() {
    return "x";
  }

  setData(jsonData) {

    this._renderChartData(JSON.parse(jsonData));
  }

  _loadData() {

    if (!this.dataUrl) {
      console.warn("No data-url set on sakai-course-grades-chart");
      return;
    }

    fetch(this.dataUrl)
      .then(r => {
        if (r.ok) {
          return r.json();
        }

        throw new Error(`Network error while fetching chart data from ${this.dataUrl}`);
      })
      .then(data => this._i18nLoaded.then(() => this._renderChartData(data)));
  }

  _renderChart() {

    this._loading = false;

    const ctx = this.renderRoot.querySelector("#chart-block");

    this.chart = new Chart(ctx, {
      type: "bar",

      options: {
        indexAxis: this._getIndexAxis(),
        plugins: {
          title: {
            display: true,
            text: this._getTitle(),
            font: { size: 18, weight: 'bold' },
          },
          legend: { display: false },
        },
        scales: {
          x: {
            type: 'linear',
            display: true,
            title: {
              display: true,
              text: this._getXLabel(),
              font: { size: 14, family: 'Monospace', weight: 'bold' },
            },
            ticks: {
              font: { weight: 'bold' },
              callback: function (value) {
                // Display student values only as integers
                if (Math.floor(value) === value) {
                  return value;
                }
              },
            },
          },
          y: {
            display: true,
            title: {
              display: true,
              text: this._getYLabel(),
						  font: { size: 14, weight: 'bold' },
            },
            ticks: {
              font: { weight: 'bold', family: 'Monospace' },
              callback: function (value, index, ticks) {
                const label = this.getLabelForValue(value);
                return label + (label.length < 2 ? ' ' : '');
              }
            },
          },
        },
      },
    });
  }

  _renderChartData(chartData) {

    const data = Object.values(chartData.dataset);
    const labels = Object.keys(chartData.dataset);

    const backgroundColour = labels.map(l => '#15597e');

    //If this chart is being viewed by a student, display the bar that
    //includes their mark in a different colour
    if (chartData.studentGradeRange) {
      const index = labels.indexOf(studentGradeRange);
      index != -1 && (backgroundColour[index] = '#5bc0de');
    }

    this.chart.data = {
      labels: labels,
      datasets: [{
        data: data,
        backgroundColor: backgroundColour,
        borderWidth: 0
      }]
    };
    this.chart.update();

    this.requestUpdate();
  }

  shouldUpdate() {
    return this._i18n;
  }

  render() {

    return html`
      ${this._loading ? html`
        <div>${this._i18n.loading}</div>
      ` : nothing}
      <div>
      <canvas id="chart-block" width="400" height="400"></chart>
      </div>
    `;
  }
}
