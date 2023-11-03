import { html, LitElement } from "./assets/lit-element/lit-element.js";

export class SakaiSorter extends LitElement {

  constructor() {

    super();
  }

  _getDragAfterElement(container, coord) {

    const notDraggedCards =
      [...container.querySelectorAll("[draggable='true']:not(.dragging)")];

    return notDraggedCards.reduce((closest, child) => {

      const box = child.getBoundingClientRect();
      const offset = coord - box.top - box.height / 2;
      if (offset < 0 && offset > closest.offset) {
        return { offset, element: child };
      } else {
        return closest;
      }
    }, { offset: Number.NEGATIVE_INFINITY }).element
  }

  firstUpdated() {

    const container = this.shadowRoot.querySelector("slot").assignedNodes().find(n => n.nodeType === Node.ELEMENT_NODE);

    container.addEventListener("dragover", e => {

      e.preventDefault();

      const afterElement = this._getDragAfterElement(container, e.clientY);
      if (afterElement == undefined) {
        container.appendChild(this.draggingElement) // add to the end
      } else {
        container.insertBefore(this.draggingElement, afterElement)
      }
    });

    container.querySelectorAll("*").forEach(sortable => {

      if (sortable.nodeType === Node.ELEMENT_NODE) {

        sortable.setAttribute("draggable", "true");

        const dragHandle = document.createElement("span");
        dragHandle.classList.add("bi", "bi-grid");
        sortable.insertBefore(dragHandle, sortable.firstChild)

        sortable.addEventListener("dragstart", e => {

          e.dataTransfer.setData('text/plain', 'handle');
          this.draggingElement = e.target;
          sortable.classList.add("dragging");
        });

        sortable.addEventListener("dragend", e => {
          e.target.classList.remove("dragging");
        });
      }
    });
  }

  render() {

    return html`
      <slot>
      </slot>
    `;
  }
}

if (!customElements.get("sakai-sorter")) {
  customElements.define("sakai-sorter", SakaiSorter);
}
