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

    //console.log(container);

    this._sortableIds = [];

    [...container.children].filter(n => n.nodeType === Node.ELEMENT_NODE).forEach(sortable => {

      this._sortableIds.push(sortable.dataset.sortableId);

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

      //console.log(sortable);
    });

    //console.log(this._sortableIds);

    container.addEventListener("dragover", e => {

      e.preventDefault();
      e.dataTransfer.dropEffect = "move";

      const afterElement = this._getDragAfterElement(container, e.clientY);

      [...container.children].filter(n => n.nodeType === Node.ELEMENT_NODE).forEach(el => el.style.borderTop = "none");

      if (afterElement) {
        afterElement.style.borderTop = "2px solid black";
        afterElement.style.paddingTop = "2px";
        afterElement.style.marginTop = "2px";
      }
    });

    container.addEventListener("drop", e => {

      const afterElement = this._getDragAfterElement(container, e.clientY);

      [...container.children].filter(n => n.nodeType === Node.ELEMENT_NODE).forEach(el => el.style.borderTop = "none");

      //console.log(afterElement);

      const draggingIndex = this._sortableIds.findIndex(id => id === this.draggingElement.dataset.sortableId);
      this._sortableIds.splice(draggingIndex, 1);
      const afterIndex = afterElement ? this._sortableIds.findIndex(id => id === afterElement.dataset.sortableId) : this._sortableIds.length - 1;
      console.log(afterIndex);

      if (afterIndex === 0) {
        this._sortableIds.unshift(this.draggingElement.dataset.sortableId);
      } else if (afterIndex === this._sortableIds.length - 1) {
        this._sortableIds.push(this.draggingElement.dataset.sortableId);
      } else {
        this._sortableIds.splice(afterIndex, 0, this.draggingElement.dataset.sortableId);
        //this._sortableIds.splice(afterIndex - 1, 0, this.draggingElement.dataset.sortableId);
      }

      //console.log("SORTER");
      //console.log(this._sortableIds);

      this.dispatchEvent(new CustomEvent("reordered", { detail: this._sortableIds }));
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
