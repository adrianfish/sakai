import {SakaiElement} from "/webcomponents/lib/sakai-element.js";
import {html} from "/webcomponents/assets/@polymer/lit-element/lit-element.js";
import {repeat} from "/webcomponents/assets/lit-html/directives/repeat.js";
import {unsafeHTML} from "/webcomponents/assets/lit-html/directives/unsafe-html.js";

class Poll extends SakaiElement {

  constructor() {

    super();

    this.poll;
    this.doneUrl;
    this.voteId;
  }

  static get properties() {

    return {
      pollId: {type: String},
      doneUrl: {type: String},
      voteId: {type: Number},
    };
  }

  attributeChangedCallback(name, oldValue, newValue) {

    if (name === "pollid") {
      this.loadPoll(newValue);
    }

    super.attributeChangedCallback(name, oldValue, newValue);
  }

  render() {

    if (this.voteId) {
      return html`
        <form action="${this.doneUrl}" method="GET">
          <div id="confirmation">
            <p><strong>${Poll.i18n["thanks_msg"]}</strong></p>
            <p><span>${Poll.i18n["thanks_ref"]}</span> <span>${this.voteId}</span></p>
            <p><input type="submit" class="active" value="${Poll.i18n["thanks_done"]}" /></p>
          </div>
        </form>
      `;
    } else {
      if (this.poll) {
        return html`
          <!--style>
            :host .option-text {
              display: inline-block;
            }
            :host .option-input {
              margin-right: 10px;
            }
          </style-->

          <div id="poll">
            <h3>${Poll.i18n["poll_vote_title"]}</h3>
            <!--div rsf:id="message-for:*" class="alertMessage">
              <span>Message for user here</span>
            </div-->
            <p><strong>${this.poll.text}</strong></p>
            <div>${unsafeHTML(this.poll.details)}</div>
            <table>
              ${repeat(this.poll.options, (o) => o.id, o => html`
              <tr>
                <td valign="middle"><input class="option-input" type="radio" name="polloption" id="${o.id}" value="${o.id}" /><label class="option-text" for="${o.id}">${unsafeHTML(o.text)}</label></td>
              </tr>
              `)}
            </table>
            <p class="act">
              <input type="button" value="${Poll.i18n["vote_vote"]}" class="active" @click=${this.submitVote} />
              <input type="button" value="${Poll.i18n["vote_cancel"]}" @click=${this.cancel} />
            </p>
          </div>
        `;
      } else {
        return html`Waiting for poll`;
      }
    }
  }

  loadPoll(pollId) {

    fetch(`/direct/polls/${pollId}/poll-view.json?includeOptions=true`)
      .then(res => res.json())
      .then(data => {
        this.poll = data;
        this.loadOrRefreshTranslations({loader: Poll.i18nLoader, bundle: Poll.i18nBundle, namespace: Poll.i18nNamespace})
          .then(res => {
            Poll.i18n = res;
            this.requestUpdate();
          });
      })
      .catch(error => console.log(`Failed to load poll with id ${pollId}`, error));
  }

  submitVote() {

    const pollOption = this.querySelector("input[name=\"polloption\"]:checked").value;

    fetch("/direct/polls/vote-create", {
      method: "POST",
      cache: "no-cache",
      credentials: "same-origin",
      body: new URLSearchParams(`pollId=${this.poll.pollId}&pollOption=${pollOption}`)
    })
      .then(res => res.text())
      .then(data => this.voteId = Number(data))
      .catch(error => console.log(`Failed to vote on poll ${this.poll.pollId}`, error));
  }

  cancel() {
    window.location = this.doneUrl;
  }
}

Poll.i18nLoader = "org.sakaiproject.poll.logic.PollListManager";
Poll.i18nBundle = "org.sakaiproject.poll.bundle.Messages";
Poll.i18nNamespace = "polls";

customElements.define("polls-poll", Poll);
