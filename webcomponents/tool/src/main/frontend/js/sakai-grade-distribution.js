import { SakaiElement } from "./sakai-element.js";
import { html } from "./assets/lit-html/lit-html.js";
// eslint-disable-next-line no-unused-vars
import { BarController, BarElement, CategoryScale, Chart as Chart, LinearScale, Title } from "./assets/chart.js/dist/chart.js";

class SakaiGradeDistribution extends SakaiElement {

  constructor() {

    super();

    Chart.register(BarController, BarElement, CategoryScale, LinearScale, Title);

    this.selectedSites = [];
    this.selectedUsers = [];
    this.selectedTerms = [];
    this.selectedDepartments = [];
    this.selectedSubjects = [];
    this.loadTranslations("analytics").then(r => this.i18n = r);
  }

  static get properties() {

    return {
      departments: { type: Array },
      siteRef: { attribute: "site-ref", type: String },
      sites: { type: Array },
      users: { type: Array },
      terms: { type: Array },
      subjects: { type: Array },
      i18n: { attribute: false, type: Object },
      data: { attribute: false, type: Object },
      filtersActive: { attribute: false, type: Boolean },
    };
  }

  set siteRef(value) {

    this._siteRef = value;
    this.selectedSites.push(value);
  }

  get siteRef() { return this._siteRef; }

  shouldUpdate() {
    return this.i18n;
  }

  firstUpdated() {

    if (this.sites && this.sites.length > 0) {
      const el = this.querySelector("#anal-site-filter");
      new Choices(el, {
        allowHtml: true,
        placeholderValue: this.i18n.search_placeholder,
        choices: this.sites.map(s => ({ value: s.ref, label: s.title })),
      });
    }

    if (this.users && this.users.length > 0) {
      const el = this.querySelector("#anal-user-filter");
      new Choices(el, {
        allowHtml: true,
        placeholderValue: this.i18n.search_placeholder,
        choices: this.users.map(u => ({ value: u.id, label: u.displayName })),
      });
    }

    if (this.terms && this.terms.length > 0) {
      const el = this.querySelector("#anal-term-filter");
      new Choices(el, {
        allowHtml: true,
        placeholderValue: this.i18n.search_placeholder,
        choices: this.terms.map(t => ({ value: t, label: t })),
      });
    }
    if (this.departments && this.departments.length > 0) {
      const el = this.querySelector("#anal-department-filter");
      new Choices(el, {
        allowHtml: true,
        placeholderValue: this.i18n.search_placeholder,
        choices: this.departments.map(t => ({ value: t, label: t })),
      });
    }

    if (this.subjects && this.subjects.length > 0) {
      const el = this.querySelector("#anal-subject-filter");
      new Choices(el, {
        allowHtml: true,
        placeholderValue: this.i18n.search_placeholder,
        choices: this.subjects.map(t => ({ value: t, label: t })),
      });
    }

  }

  _addSite(e) {

    this.selectedSites.push(e.detail.value);
    this.filtersActive = true;
  }

  _removeSite(e) {

    const i = this.selectedSites.indexOf(e.detail.value);
    if (i > -1) this.selectedSites.splice(i, 1);
    this._setFiltersActive();
  }

  _addUser(e) {

    this.selectedUsers.push(e.detail.value);
    this.filtersActive = true;
  }

  _removeUser(e) {

    const i = this.selectedUsers.indexOf(e.detail.value);
    if (i > -1) this.selectedUsers.splice(i, 1);
    this._setFiltersActive();
  }

  _addTerm(e) {

    this.selectedTerms.push(e.detail.value);
    this.filtersActive = true;
  }

  _removeTerm(e) {

    const i = this.selectedTerms.indexOf(e.detail.value);
    if (i > -1) this.selectedTerms.splice(i, 1);
    this._setFiltersActive();
  }

  _addDepartment(e) {

    this.selectedDepartments.push(e.detail.value);
    this.filtersActive = true;
  }

  _removeDepartment(e) {

    const i = this.selectedDepartments.indexOf(e.detail.value);
    if (i > -1) this.selectedDepartments.splice(i, 1);
    this._setFiltersActive();
  }

  _addSubject(e) {

    this.selectedSubjects.push(e.detail.value);
    this.filtersActive = true;
  }

  _removeSubject(e) {

    const i = this.selectedSubjects.indexOf(e.detail.value);
    if (i > -1) this.selectedSubjects.splice(i, 1);
    this._setFiltersActive();
  }

  _setFiltersActive() {

    this.filtersActive = this.selectedUsers.length
      || this.selectedSites.length
      || this.selectedTerms.length
      || this.selectedDepartments.length
      || this.selectedSubjects.length;
  }

  _run() {

    const params = {
      siteRefs: this.selectedSites,
      userIds: this.selectedUsers,
      termIds: this.selectedTerms,
      departments: this.selectedDepartments,
      subjects: this.selectedSubjects,
    };

    const url = "/api/reports/grades/distribution";
    fetch(url, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(params),
    })
    .then(r => {

      if (r.ok) {
        return r.json();
      }

      throw new Error(`Network error while requesting report from ${url}`);
    })
    .then(data => {

      this.data = data;

      this.updateComplete.then(() => {

        if (this.chart) this.chart.destroy();

        const ctx = document.getElementById("grade-distribution-report-chart");

        this.chart = new Chart(ctx, {
          type: "bar",
          data: {
            labels: ["0-9%", "10-19%", "20-29%", "30-39%", "40-49%", "50-59%", "60-69%", "70-79%", "80-89%", "90-100%"],
            datasets: [
              {
                backgroundColor: getComputedStyle(document.documentElement).getPropertyValue("--sakai-grade-distribution-column-bg-color") || "orange",
                borderWidth: 1,
                borderRadius: 7,
                data: [this.data.dist[0], this.data.dist[1], this.data.dist[2], this.data.dist[3], this.data.dist[4], this.data.dist[5], this.data.dist[6], this.data.dist[7], this.data.dist[8], this.data.dist[9]],
              }
            ],
          },
          options: {
            responsive: true,
            plugins: {
              title: {
                display: true,
                text: this.i18n.grade_distribution_title,
                font: {
                  size: getComputedStyle(document.documentElement).getPropertyValue("--sakai-grade-distribution-title-size") || 18,
                  weight: "bold",
                  color: getComputedStyle(document.documentElement).getPropertyValue("--sakai-text-color-1") || "black",
                },
              },
            },
            scales: {
              x: {
                ticks: {
                  font: {
                    size: 14,
                    family: "Monospace",
                    weight: "bold",
                  },
                  color: getComputedStyle(document.documentElement).getPropertyValue("--sakai-text-color-1") || "black",
                },
                title: {
                  display: true,
                  text: this.i18n.percentage_scored,
                  font: {
                    size: 14,
                    weight: "bold",
                  },
                  color: getComputedStyle(document.documentElement).getPropertyValue("--sakai-text-color-1") || "black",
                }
              },
              y: {
                ticks: {
                  stepSize: 1,
                  font: {
                    family: "Monospace",
                    weight: "bold",
                  },
                  color: getComputedStyle(document.documentElement).getPropertyValue("--sakai-text-color-1") || "black",
                },
                title: {
                  display: true,
                  text: this.i18n.number_of_students,
                  font: {
                    size: 14,
                    weight: "bold",
                  },
                  color: getComputedStyle(document.documentElement).getPropertyValue("--sakai-text-color-1") || "black",
                }
              },
            },
          },
        });
      });
    });
  }

  render() {

    return html`

      <div id="anal-filters-button-wrapper">
        <div>
          <button id="anal-filters-button"
              class="btn btn-secondary"
              data-toggle="collapse"
              data-target="#anal-filter-block"
              aria-controls="anal-filter-block"
              aria-expanded="false">
            Show/Hide filters
          </button>
        </div>
        <div id="anal-active-filters-indicator" style="display: ${this.filtersActive ? "initial" : "none"}">${this.i18n.filters_active}</div>
      </div>

      <div id="anal-filter-block" class="collapse">

        ${this.sites ? html`
        <div class="anal-select-wrapper">
          <div><label for="anal-site-filter">${this.i18n.select_sites}</label></div>
          <div class="anal-select">
            <select id="anal-site-filter" @addItem=${this._addSite} @removeItem=${this._removeSite} multiple>
            </select>
          </div>
        </div>
        ` : ""}

        ${this.users ? html`
        <div class="anal-select-wrapper">
          <div><label for="anal-user-filter">${this.i18n.select_users}</label></div>
          <div class="anal-select">
            <select id="anal-user-filter" @addItem=${this._addUser} @removeItem=${this._removeUser} multiple>
            </select>
          </div>
        </div>
        ` : ""}

        ${this.terms ? html`
        <div class="anal-select-wrapper">
          <div><label for="anal-term-filter">${this.i18n.select_terms}</label></div>
          <div class="anal-select">
            <select id="anal-term-filter" @addItem=${this._addTerm} @removeItem=${this._removeTerm} multiple>
            </select>
          </div>
        </div>
        ` : ""}

        ${this.departments ? html`
        <div class="anal-select-wrapper">
          <div><label for="anal-department-filter">${this.i18n.select_departments}</label></div>
          <div class="anal-select">
            <select id="anal-department-filter" @addItem=${this._addDepartment} @removeItem=${this._removeDepartment} multiple>
            </select>
          </div>
        </div>
        ` : ""}

      ${this.subjects ? html`
        <div class="anal-select-wrapper">
          <div><label for="anal-subject-filter">${this.i18n.select_subjects}</label></div>
          <div class="anal-select">
            <select id="anal-subject-filter" @addItem=${this._addSubject} @removeItem=${this._removeSubject} multiple>
            </select>
          </div>
        </div>
        ` : ""}


      </div>

      <div>
        <button id="anal-run-button"
            class="btn btn-primary active"
            type="button"
            @click=${this._run}>
          ${this.i18n.run}
        </button>
      </div>

      ${this.data?.dist ? html`
      <div id="anal-canvas-wrapper">
        <canvas id="grade-distribution-report-chart">
        </canvas>

        <div id="anal-summary-grid">
          <div>Average (mean) grade</div><div>${this.data.mean}</div>
          <div>Median grade</div><div>${this.data.median}</div>
          <div>Standard deviation</div><div>${this.data.standardDeviation}</div>
          <div>Lowest grade</div><div>${this.data.lowest}</div>
          <div>Highest grade</div><div>${this.data.highest}</div>
          <div>Total graded</div><div>${this.data.lowest}</div>
        </div>
      </div>
      ` : ""}
    `;
  }
}

const tagName = "sakai-grade-distribution";
!customElements.get(tagName) && customElements.define(tagName, SakaiGradeDistribution);
