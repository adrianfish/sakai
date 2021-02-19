import {SakaiElement} from "./sakai-element.js";
import {html} from "./assets/lit-element/lit-element.js";

class SakaiEditor extends SakaiElement {

  static get properties() {

    return {
      elementId: { attribute: "element-id", type: String },
      debug: { type: Boolean },
      content: String,
      active: { type: Boolean },
      delay: { type: Boolean },
      toolbar: String,
      setFocus: { attribute: "set-focus", type: Boolean },
    };
  }

  constructor() {

    super();

    if (this.debug) console.debug("Sakai Editor constructor");
    this.content = "";
    this.elementId = `editable_${Math.floor(Math.random() * 20) + 1}`;
  }

  getContent() {
    return this.editor.getData();
  }

  clear() {
    this.editor.setData("");
  }

  shouldUpdate() {
    return (this.content || this.elementId) && typeof CKEDITOR !== "undefined";
  }

  set active(value) {

    this._active = value;
    if (value) {
      this.attachEditor();
    } else {
      this.editor.destroy()
    }

  attachEditor() {

    if (CKEDITOR.instances[this.elementId]) {
      CKEDITOR.instances[this.elementId].destroy();
    }

    if (sakai?.editor?.launch) {
      this.editor = sakai.editor.launch(this.elementId, { autosave: { delay: 10000000, messageType: "no" } });
    } else {
      this.editor = CKEDITOR.replace(this.elementId, {toolbar: SakaiEditor.toolbars.get("basic")});
    }

    this.editor.on("change", (e) => {
      this.dispatchEvent(new CustomEvent("changed", { detail: { content: e.editor.getData() }, bubbles: true }));
    });

    if (this.setFocus) {
      this.editor.on("instanceReady", e => {
        e.editor.focus();
      });
    }
  }

  firstUpdated(changed) {
>>>>>>> b1af8df49e1 (SAK-45092 Create a new tool for handling conversations in Sakai)

    render() {
        console.debug("Sakai Editor render");

        return html`
            <div id="${this.editorId}">
                <h2>${this.text}</h2>
            </div>
        `;
    }

    firstUpdated(changedProperties) {
        console.debug("Sakai Editor firstUpdated");
        const element = this.querySelector(`#${this.editorId}`);

        if (this.mode === "inline") {
            CKEDITOR.InlineEditor.create(element)
                .then(editor => {
                    console.debug(editor);
                })
                .catch(error => {
                    console.error(error.stack);
                });
        } else if (this.mode === "balloon") {
            CKEDITOR.BalloonEditor.create(element)
                .then(editor => {
                    console.debug(editor);
                })
                .catch(error => {
                    console.error(error.stack);
                });
        } else {
            // classic editor is the default
            CKEDITOR.ClassicEditor.create(element)
                .then(editor => {
                    console.debug(editor);
                })
                .catch(error => {
                    console.error(error.stack);
                });
        }
    }
}

customElements.define("sakai-editor", SakaiEditor);
