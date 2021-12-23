import { RubricsElement } from "./rubrics-element.js";
import { html } from "../assets/lit-element/lit-element.js";
import "./sakai-rubric-grading-comment.js";
import { SakaiRubricsLanguage, tr } from "./sakai-rubrics-language.js";
import { getUserId } from "../sakai-portal-utils.js";

export class SakaiRubricGrading extends RubricsElement {

  constructor() {

    super();

    this.rubric = { title: "" };
    this.criteria = [];
    this.totalPoints = 0;

    SakaiRubricsLanguage.loadTranslations().then(r => this.i18nLoaded = r);
  }

  static get properties() {

    return {
      token: String,
      toolId: { attribute: "tool-id", type: String },
      entityId: { attribute: "entity-id", type: String },
      evaluatedItemId: { attribute: "evaluated-item-id", type: String },
      evaluatedItemOwnerId: { attribute: "evaluated-item-owner-id", type: String },
      group: { type: Boolean},

      // Non attribute
      evaluation: { type: Object },
      totalPoints: Number,
      translatedTotalPoints: { type: Number },
      criteria: { type: Array },
      rubric: { type: Object }
    };
  }

  set token(newValue) {

    if (!newValue.startsWith("Bearer")) {
      this._token = `Bearer ${  newValue}`;
    } else {
      this._token = newValue;
    }

    this.getAssociation();
  }

  get token() {
    return this._token;
  }

  set entityId(value) {

    this._entityId = value;
    this.getAssociation();
  }

  get entityId() { return this._entityId; }

  set evaluatedItemId(value) {

    this._evaluatedItemId = value;
    this.getAssociation();
  }

  get evaluatedItemId() { return this._evaluatedItemId; }

  set evaluation(value) {

    this._evaluation = value;
    if (value.id && value.status !== "DRAFT") {
      // Store the evaluation in case the grading gets cancelled.
      sessionStorage.setItem(this.getStorageKey(), JSON.stringify(value));
    }
    this.requestUpdate();
  }

  get evaluation() { return this._evaluation; }

  set toolId(value) {

    this._toolId = value;
    this.getAssociation();
  }

  get toolId() { return this._toolId; }

  shouldUpdate() {
    return this.i18nLoaded;
  }

  getStorageKey() {
    return `rubric-evaluation-${this.evaluation.id}-${getUserId()}`;
  }

  render() {

    return html`
      <div class="rubric-details grading">
        <h3>${this.rubric.title}</h3>
        ${this.evaluation && this.evaluation.status === "DRAFT" ? html`
          <div class="sak-banner-warn">
            ${tr('draft_evaluation', [tr(this.getToolDraftMessageKey())])}
          </div>
        ` : "" }
        <div class="criterion grading style-scope sakai-rubric-criteria-grading">
        ${this.criteria.map(c => html`
          <div id="criterion_row_${c.id}" class="criterion-row">
            <div class="criterion-detail" tabindex="0">
              <h4 class="criterion-title">${c.title}</h4>
              <p>${c.description}</p>
              ${this.rubric.weighted ?
                html`
                  <div class="criterion-weight">
                    <span>
                      <sr-lang key="weight">Weight</sr-lang>
                    </span>
                    <span>${c.weight.toLocaleString(this.locale)}</span>
                    <span>
                      <sr-lang key="percent_sign">%</sr-lang>
                    </span>
                  </div>`
                : ""
              }
            </div>
            <div class="criterion-ratings">
              <div class="cr-table">
                <div class="cr-table-row">
                ${c.ratings.map(r => html`
                  <div class="rating-item ${r.selected ? "selected" : ""}"
                        tabindex="0"
                        data-rating-id="${r.id}"
                        id="rating-item-${r.id}"
                        data-criterion-id="${c.id}"
                        @keypress=${this.toggleRating}
                        @click=${this.toggleRating}>
                    <h5 class="criterion-item-title">${r.title}</h5>
                    <p>${r.description}</p>
                    <span class="points" data-points="${r.points}">
                      ${this.rubric.weighted && r.points > 0 ?
                        html`
                          <b>
                            (${parseFloat((r.points * (c.weight / 100)).toFixed(2)).toLocaleString(this.locale)})
                          </b>`
                        : ""
                      }
                      ${r.points.toLocaleString(this.locale)}
                      <sr-lang key="points">Points</sr-lang>
                    </span>
                  </div>
                `)}
                </div>
              </div>
            </div>
            <div class="criterion-actions">
              <sakai-rubric-grading-comment id="comment-for-${c.id}" @comment-shown=${this.commentShown} @update-comment="${this.updateComment}" criterion="${JSON.stringify(c)}" evaluated-item-id="${this.evaluatedItemId}" entity-id="${this.entityId}"></sakai-rubric-grading-comment>
              <div class="rubric-grading-points-value">
                <strong id="points-display-${c.id}" class="points-display ${this.getOverriddenClass(c.pointoverride, c.selectedvalue)}">
                  ${c.selectedvalue.toLocaleString(this.locale)}
                </strong>
              </div>
              ${this.association.parameters.fineTunePoints ? html`
                  <input
                      title="${tr("point_override_details")}"
                      data-criterion-id="${c.id}"
                      name="rbcs-${this.evaluatedItemId}-${this.entityId}-criterion-override-${c.id}"
                      class="fine-tune-points form-control hide-input-arrows"
                      @input=${this.fineTuneRating}
                      .value="${c.pointoverride.toLocaleString(this.locale)}"
                  />
                ` : ""}
              <input aria-labelledby="${tr("points")}" type="hidden" id="rbcs-${this.evaluatedItemId}-${this.entityId}-criterion-${c.id}" name="rbcs-${this.evaluatedItemId}-${this.entityId}-criterion-${c.id}" .value="${c.selectedvalue}">
              <input type="hidden" name="rbcs-${this.evaluatedItemId}-${this.entityId}-criterionrating-${c.id}" .value="${c.selectedRatingId}">
            </div>
          </div>
        `)}
        </div>
        <div class="rubric-totals">
          <input type="hidden" aria-labelledby="${tr("total")}" id="rbcs-${this.evaluatedItemId}-${this.entityId}-totalpoints" name="rbcs-${this.evaluatedItemId}-${this.entityId}-totalpoints" .value="${this.totalPoints}">
          <div class="total-points">
            <sr-lang key="total">Total</sr-lang>: <strong id="sakai-rubrics-total-points">${this.totalPoints.toLocaleString(this.locale, {maximumFractionDigits: 2})}</strong>
          </div>
        </div>
      </div>
    `;
  }

  updateComment(e) {

    this.criteria.forEach(c => {

      if (c.id === e.detail.criterionId) {
        c.comments = e.detail.value;
      }
    });

    this._dispatchRatingChanged(this.criteria, 1);
  }

  calculateTotalPointsFromCriteria() {

    this.totalPoints = this.criteria.reduce((a, c) => {

      if (c.pointoverride) {
        return a + parseFloat(c.pointoverride);
      } else if (c.selectedvalue) {
        return a + parseFloat(c.selectedvalue);
      }
      return a;

    }, 0);
  }

  release() {
    this._dispatchRatingChanged(this.criteria, 2);
  }

  save() {
    this._dispatchRatingChanged(this.criteria, 1);
  }

  decorateCriteria() {

    this.evaluation.criterionOutcomes.forEach(ed => {

      this.criteria.forEach(c => {

        if (ed.criterionId === c.id) {

          c.selectedRatingId = ed.selectedRatingId;
          if (ed.pointsAdjusted) {
            c.pointoverride = ed.points;
            const ratingItem = c.ratings.filter(r => r.id == ed.selectedRatingId)[0];
            if (ratingItem) {
              c.selectedvalue = ratingItem.points;
              ratingItem.selected = true;
            }
          } else {
            const ratingItem = c.ratings.filter(r => r.id == ed.selectedRatingId)[0];
            if (ratingItem) {
              ratingItem.selected = true;
            }
            c.pointoverride = ed.points;
            c.selectedvalue = ed.points;
          }

          c.comments = ed.comments;
        }
      });
    });

    this.updateTotalPoints(false);
  }

  fineTuneRating(e) {

    const value = e.target.value;

    const parsed = value.replace(/,/g, ".");

    if (isNaN(parseFloat(parsed))) {
      return;
    }

    const criterion = this.criteria.find(c => c.id == e.target.dataset.criterionId);

    criterion.pointoverride = parsed;
    if (criterion.selectedvalue) {
      this.totalPoints = this.totalPoints - criterion.selectedvalue + criterion.pointoverride;
    } else {
      this.totalPoints = this.totalPoints + criterion.pointoverride;
    }

    this.dispatchEvent(new CustomEvent("rubric-ratings-changed", { bubbles: true, composed: true }));
    const detail = {
      evaluatedItemId: this.evaluatedItemId,
      entityId: this.entityId,
      criterionId: criterion.id,
      value: criterion.pointoverride,
    };
    this.dispatchEvent(new CustomEvent("rubric-rating-tuned", { detail, bubbles: true, composed: true }));

    this.updateTotalPoints();
    this._dispatchRatingChanged(this.criteria, 1);
  }

  _dispatchRatingChanged(criteria, status) {

    const crit = criteria.map(c => {

      return {
        criterionId: c.id,
        points: c.pointoverride ? parseFloat(c.pointoverride) : c.selectedvalue,
        comments: c.comments,
        pointsAdjusted: c.pointoverride !== c.selectedvalue,
        selectedRatingId: c.selectedRatingId
      };
    });

    const evaluation = {
      evaluatorId: getUserId(),
      evaluatedItemId: this.evaluatedItemId,
      evaluatedItemOwnerId: this.evaluatedItemOwnerId,
      evaluatedItemOwnerType: this.group ? "GROUP" : "USER",
      overallComment: "",
      criterionOutcomes: crit,
      toolItemRubricAssociation: this.association._links.self.href,
      status,
    };

    if (this.evaluation && this.evaluation.id) {
      evaluation.metadata = this.evaluation.metadata;
    }

    this.saveEvaluation(evaluation, status);
  }

  saveEvaluation(evaluation) {

    let url = "/rubrics-service/rest/evaluations";
    if (this.evaluation && this.evaluation.id) url += `/${this.evaluation.id}`;
    return fetch(url, {
      body: JSON.stringify(evaluation),
      credentials: "same-origin",
      headers: {
        "Authorization": this.token,
        "Accept": "application/json",
        "Content-Type": "application/json"
      },
      method: this.evaluation && this.evaluation.id ? "PATCH" : "POST"
    })
    .then(r => {

      if (r.ok) {
        return r.json();
      }
      throw new Error("Server error while saving rubric evaluation");
    })
    .then(data => this.evaluation = data)
    .catch(error => console.error(error));
  }

  deleteEvaluation() {

    if (!this?.evaluation?.id) return;

    const url = `/rubrics-service/rest/evaluations/${this.evaluation.id}`;
    fetch(url, {
      credentials: "same-origin",
      headers: { "Authorization": this.token, },
      method: "DELETE"
    })
    .then(r => {

      if (r.ok) {
        this.criteria.forEach(c => c.ratings.forEach(rat => rat.selected = false));
        this.evaluation = { criterionOutcomes: [] };
        this.requestUpdate();
      } else {
        throw new Error("Server error while deleting evaluation");
      }
    })
    .catch(error => console.error(error));
  }

  getOverriddenClass(ovrdvl, selected) {

    if (!this.association.parameters.fineTunePoints) {
      return '';
    }

    if ((ovrdvl || ovrdvl === 0) && parseFloat(ovrdvl) !== parseFloat(selected)) {
      return 'strike';
    }
    return '';

  }

  toggleRating(e) {

    e.stopPropagation();

    const criterionId = parseInt(e.currentTarget.dataset.criterionId);
    const ratingId = parseInt(e.currentTarget.dataset.ratingId);

    // Look up the criterion and rating objects
    const criterion = this.criteria.filter(c => c.id == criterionId)[0];
    const rating = criterion.ratings.filter(r => r.id === ratingId)[0];

    criterion.ratings.forEach(r => r.selected = false);

    if (rating.selected) {
      criterion.selectedvalue = 0.0;
      criterion.selectedRatingId = 0;
      criterion.pointoverride = 0.0;
      rating.selected = false;
    } else {
      const auxPoints = this.rubric.weighted ?
        (rating.points * (criterion.weight / 100)).toFixed(2) : rating.points;
      criterion.selectedvalue = auxPoints;
      criterion.selectedRatingId = rating.id;
      criterion.pointoverride = auxPoints;
      rating.selected = true;
    }

    // Whenever a rating is clicked, either to select or deselect, it cancels out any override so we
    // remove the strike out from the clicked points value
    this.querySelector(`#points-display-${criterionId}`).classList.remove("strike");

    this.dispatchEvent(new CustomEvent("rubric-ratings-changed", { bubbles: true, composed: true }));
    this.requestUpdate();
    this.updateTotalPoints();

    this._dispatchRatingChanged(this.criteria, 1);
  }

  commentShown(e) {
    this.querySelectorAll(`sakai-rubric-grading-comment:not(#${e.target.id})`).forEach(c => c.hide());
  }

  updateTotalPoints(notify = true) {

    this.calculateTotalPointsFromCriteria();

    // Make sure total points is not negative
    if (parseFloat(this.totalPoints) < 0) this.totalPoints = 0;

    if (notify) {
      const detail = {
        evaluatedItemId: this.evaluatedItemId,
        entityId: this.entityId,
        value: this.totalPoints.toLocaleString(this.locale, { maximumFractionDigits: 2 }),
      };
      this.dispatchEvent(new CustomEvent('total-points-updated', { detail, bubbles: true, composed: true }));
    }
  }

  cancel() {

    if (this.evaluation.status !== "DRAFT") return;

    // Get the evaluation from session storage. This should be the last non draft evaluation that
    // the server originally sent before the user started setting ratings. Save it baack to the
    // server.
    const evaluation = JSON.parse(sessionStorage.getItem(this.getStorageKey()));
    if (evaluation) {
      sessionStorage.removeItem(this.getStorageKey());

      // Save cached evaluation and reset the criteria ready for rendering
      this.saveEvaluation(evaluation).then(() => {

        // Unset any ratings
        this.criteria.forEach(c => c.ratings.forEach(r => r.selected = false));
        // And set the original ones
        this.decorateCriteria();
      });
    } else {
      this.deleteEvaluation();
    }
  }

  getAssociation() {

    if (!this.toolId || !this.entityId || !this.token || !this.evaluatedItemId) {
      return;
    }

    $.ajax({
      url: `/rubrics-service/rest/rubric-associations/search/by-tool-and-assignment?toolId=${this.toolId}&itemId=${this.entityId}`,
      headers: { "authorization": this.token }
    }).done(data => {

      this.association = data._embedded['rubric-associations'][0];
      this.rubricId = data._embedded['rubric-associations'][0].rubricId;
      this.getRubric(this.rubricId);
    }).fail((jqXHR, textStatus, errorThrown) => {

      console.info(textStatus);
      console.error(errorThrown);
    });
  }

  getRubric(rubricId) {

    $.ajax({
      url: `/rubrics-service/rest/rubrics/${rubricId}?projection=inlineRubric`,
      headers: { "authorization": this.token }
    }).done(rubric => {

      $.ajax({
        url: `/rubrics-service/rest/evaluations/search/by-tool-and-assignment-and-submission?toolId=${this.toolId}&itemId=${this.entityId}&evaluatedItemId=${this.evaluatedItemId}`,
        headers: { "authorization": this.token }
      }).done(data => {

        this.evaluation = data._embedded.evaluations[0] || { criterionOutcomes: [] };

        this.rubric = rubric;

        this.criteria = this.rubric.criterions;
        this.criteria.forEach(c => {

          c.pointoverride = "";

          if (!c.selectedvalue) {
            c.selectedvalue = 0;
          }
          c.pointrange = this.getHighLow(c.ratings);
        });

        this.decorateCriteria();
      }).fail((jqXHR, textStatus, errorThrown) => {

        console.info(textStatus);
        console.error(errorThrown);
      });
    }).fail((jqXHR, textStatus, errorThrown) => {

      console.info(textStatus);
      console.error(errorThrown);
    });
  }

  getToolDraftMessageKey() {
    return `draft_evaluation_${this.toolId}`;
  }
}

const tagName = "sakai-rubric-grading";
!customElements.get(tagName) && customElements.define(tagName, SakaiRubricGrading);
