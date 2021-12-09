import { html } from "../assets/lit-element/lit-element.js";
import { SakaiModal } from "../sakai-modal.js";
import "../datepicker/sakai-date-picker.js";
//import "../sakai-date-picker.js";
import "../sakai-icon.js";
import "../sakai-editor.js";

class SakaiTasksCreateTask extends SakaiModal {

  static get properties() {

    return {
      i18n: { attribute: false, type: Object },
      task: { type: Object },
      description: { type: String },
      error: { type: Boolean },
    };
  }

  constructor() {

    super();
    this.defaultTask = { taskId: "", description: "balls", priority: "3", notes: "", due: Date.now() };
    this.task = { ...this.defaultTask};
    this.loadTranslations("tasks").then(r => this.i18n = r);
  }

  title() {

    return html`
      ${this.task.taskId == "" ? this.i18n.create_new_task : this.i18n.edit_task}
    `;
  }

  save() {

    this.task.description = this.querySelector("#description").value;
    this.task.notes = this.getEditor().getData();

    fetch(`/api/tasks/${this.task.taskId}`, {
      credentials: "include",
      method: "PUT",
      cache: "no-cache",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(this.task),
    })
      .then(r => {

        if (r.ok) {
          this.error = false;
          return r.json();
        }
        this.error = true;
        throw new Error();

      })
      .then(savedTask => {

        this.task = savedTask;
        this.dispatchEvent(new CustomEvent("task-created", {detail: { task: this.task }, bubbles: true }));
        this.close();
      })
      .catch(() => {});
  }

  resetDate() {

    this.task.due = Date.now();
    const el = this.querySelector("#due");
    if (el) {
      el.epochMillis = this.task.due;
    }
  }

  set task(value) {

    const old = this._task;
    this._task = value;

    this.error = false;

    this.requestUpdate("task", old);
    this.updateComplete.then(() => {

      const datePicker = this.querySelector("#due");

      if (value.system) {
        datePicker.disable();
      } else {
        datePicker.enable();
        datePicker.epochMillis = value.due;
      }
      const descriptionEl = this.querySelector("#description");
      descriptionEl.disabled = value.system;
      descriptionEl.value = value.description;
      this.querySelector("#priority").value = value.priority;
      const editor = this.getEditor();
      if (editor) {
        editor.setData(value.notes);
        editor.isReadOnly = value.system;
      }
    });
  }

  get task() { return this._task; }

  shouldUpdate(changed) {
    return this.task && this.i18n && super.shouldUpdate(changed);
  }

  getEditorTag() {

    const el = this.querySelector("sakai-editor");
    return el;
  }

  getEditor() {
    return this.getEditorTag().editor;
  }

  reset() {
    this.task = { ...this.defaultTask};
  }

  complete(e) {

    this.task.complete = e.target.checked;

    if (e.target.checked) {
      this.task.softDeleted = false;
    }
  }

  resetEditor() {
    this.getEditor().setData(this.task.notes || "");
  }

  trigger() {

    return html`
      <div>
        <a
            href="javascript:;"
            title="${this.i18n.add_task}"
            data-toggle="modal" data-target="#${this.modalId}"
            aria-label="${this.i18n.add_task}">
          <sakai-icon type="add" size="small">
        </a>
      </div>`;
  }

  content() {

    return html`
    <div>
      <div class="sakai-modal-label">
        <label for="description">${this.i18n.description}</label>
      </div>
      <div class="input"><input type="text" id="description" size="50" maxlength="150" .value=${this.task.description}></div>
      <div id="due-and-priority-block">
        <div id="due-block">
          <div class="sakai-modal-label">
            <label for="due">${this.i18n.due}</label>
          </div>
          <div class="input">
            <sakai-date-picker1 id="due" @datetime-selected=${(e) => { this.task.due = e.detail.epochMillis; this.dueUpdated = true; }} epoch-millis=${this.task.due}></sakai-date-picker1>
          </div>
        </div>
        <div id="priority-block">
          <div class="sakai-modal-label">
            <label for="priority">${this.i18n.priority}</label>
          </div>
          <div class="input">
            <select id="priority" @change=${(e) => this.task.priority = e.target.value} .value=${this.task.priority}>
              <option value="5">${this.i18n.high}</option>
              <option value="4">${this.i18n.quite_high}</option>
              <option value="3">${this.i18n.medium}</option>
              <option value="2">${this.i18n.quite_low}</option>
              <option value="1">${this.i18n.low}</option>
            </select>
          </div>
        </div>
      </div>
      ${this.task.taskId != "" ? html`
        <div id="complete-block">
          <div>
            <label for="complete">${this.i18n.completed}</label>
            <input
              type="checkbox"
              id="complete"
              aria-label="${this.i18n.complete_tooltip}"
              title="${this.i18n.complete_tooltip}"
              @click=${this.complete}
              ?checked=${this.task.complete}>
          </div>
        </div>
      ` : ""}
      <div class="sakai-modal-label">
        <label for="text">${this.i18n.text}</label>
      </div>
      <div class="input">
        <sakai-editor element-id="task-text-editor" toolbar="basic"></sakai-editor>
      </div>
      ${this.error ? html`<div id="error">${this.i18n.save_failed}</div>` : ""}
      </div>
    `;
  }

  buttons() {

    return html`
      <sakai-button @click=${this.save} primary>${this.task.taskId == "" ? this.i18n.add : this.i18n.save}</sakai-button>
    `;
  }
}

const tagName = "sakai-tasks-create-task";
!customElements.get(tagName) && customElements.define(tagName, SakaiTasksCreateTask);
