import { html, LitElement } from "./assets/lit-element/lit-element.js";

export class SakaiSorter extends LitElement {

  constructor() {

    super();
  }

  getDragAfterElement(container, y) {

    const notDraggedCards =
      [...container.querySelectorAll(":not(.dragging)")];

    return notDraggedCards.reduce((closest, child) => {

      const box = child.getBoundingClientRect();
      const offset = y - box.top - box.height / 2;
      if (offset < 0 && offset > closest.offset) {
        return { offset, element: child };
      } else {
        return closest;
      }
    }, { offset: Number.NEGATIVE_INFINITY }).element
  }

  updated() {

    const container = this.shadowRoot.querySelector("slot").assignedNodes().find(n => n.nodeType === Node.ELEMENT_NODE);

    container.addEventListener("dragover", e => {

      const draggedElement = document.querySelector(".dragging")

      const afterElement = this.getDragAfterElement(container, e.clientY);
      if (afterElement == undefined) {
        container.appendChild(draggedElement) // add to the end
      } else {
        container.insertBefore(draggedElement, afterElement)
      }
    });

    console.log(container);

    container.querySelectorAll("*").forEach(sortable => {

      if (sortable.nodeType === Node.ELEMENT_NODE) {
        sortable.addEventListener("dragstart", e => {

          this.draggingElement = e.target;
          sortable.classList.add("dragging");
        });

        sortable.addEventListener("dragend", e => {
          sortable.classList.remove("dragging");
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
