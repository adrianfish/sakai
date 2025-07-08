import { html, nothing } from "lit";
import { SakaiShadowElement } from "@sakai-ui/sakai-element";

export class SakaiAccount extends SakaiShadowElement {

  static properties = {
    userId: { attribute: "user-id", type: String },

    _user: { state: true },
    _editingBasicInfo: { state: true },
    _editingPronunciationInfo: { state: true },
  };

  constructor() {

    super();

    this.loadTranslations("account");
  }

  connectedCallback() {

    super.connectedCallback();

    this._loadUser();
  }

  _loadUser() {

    const url = this.userId ? `/api/users/${this.userId}/profile` : "/api/users/me/profile";
    fetch(url)
    .then(r => {

      if (r.ok) {
        return r.json();
      }
      throw new Error(`Failed to get user from ${url}`);
    })
    .then(user => this._user = user);
  }

  _editBasicInfo() { this._editingBasicInfo = true; }

  _editPronunciationInfo() { this._editingPronunciationInfo = true; }

  _saveBasicInfo() {

    const formData = new FormData();
    formData.append("firstName", this.renderRoot.querySelector("#first-name-input").value);
    formData.append("lastName", this.renderRoot.querySelector("#last-name-input").value);
    formData.append("nickname", this.renderRoot.querySelector("#nickname-input").value);
    formData.append("pronouns", this.renderRoot.querySelector("#pronouns-input").value);

    const url = `/api/users/${this.userId}/profile`;
    fetch(url, {
      method: "PATCH",
      body: formData,
    })
    .then(r => {

      if (r.ok) {
        this._user.firstName = formData.get("firstName");
        this._user.lastName = formData.get("lastName");
        this._user.nickname = formData.get("nickname");
        this._user.pronouns = formData.get("pronouns");
        this._editingBasicInfo = false;
      } else {
        throw new Error(`Network error while patching to ${url}`);
      }
    });
  }

  shouldUpdate() {
    return this._i18n && this._user;
  }

  _renderBasicInfoBlock() {

    return html`
      <div>
        <div>${this._user.displayName}</div>
        <div class="mainSection">
          <div class="mainSectionHeading">${this._i18n.basic_info}</div>
          <button id="basic-info-edit-button"
              type="button"
              class="btn btn-secondary float-end"
              @click=${this._editBasicInfo}>
            ${this._i18n.edit}
          </button>
          <div class="mainSectionContent">
          ${this._user.firstName || this._user.lastName || this._user.nickname || this._user.pronouns ? html`
            <table class="profileContent">
              <tbody>
                ${this._user.firstName || this._editingBasicInfo ? html`
                <tr>
                  <td class="label">${this._i18n.first_name_label}</td>
                  <td class="content">
                    ${this._editingBasicInfo ? html`
                      <input id="first-name-input"
                          aria-label="${this._i18n.first_name_label}"
                          type="text"
                          class="formInputField"
                          .value="${this._user.firstName}">
                    ` : this._user.firstName}
                  </td>
                </tr>
                ` : nothing}
                ${this._user.lastName || this._editingBasicInfo ? html`
                <tr>
                  <td class="label">${this._i18n.last_name_label}</td>
                  <td class="content">
                    ${this._editingBasicInfo ? html`
                      <input id="last-name-input"
                          aria-label="${this._i18n.last_name_label}"
                          type="text"
                          class="formInputField"
                          .value="${this._user.lastName}">
                    ` : this._user.lastName}
                  </td>
                </tr>
                ` : nothing}
                ${this._user.nickname || this._editingBasicInfo ? html`
                <tr>
                  <td class="label">${this._i18n.nickname_label}</td>
                  <td class="content">
                    ${this._editingBasicInfo ? html`
                      <input id="nickname-input"
                          aria-label="${this._i18n.nickname_label}"
                          type="text"
                          class="formInputField"
                          .value="${this._user.nickname}">
                    ` : this._user.nickname}
                  </td>
                </tr>
                ` : nothing}
                ${this._user.pronouns || this._editingBasicInfo ? html`
                <tr>
                  <td class="label">${this._i18n.pronouns_label}</td>
                  <td class="content">
                    ${this._editingBasicInfo ? html`
                      <input id="pronouns-input"
                          aria-label="${this._i18n.pronouns_label}"
                          type="text"
                          class="formInputField"
                          .value="${this._user.pronouns}">
                    ` : this._user.pronouns}
                  </td>
                </tr>
                ` : nothing}
              </tbody>
            </table>
            ${this._editingBasicInfo ? html`
              <div>
                <input id="basic-info-save-button" type="button" value="Save" @click=${this._saveBasicInfo} />
                <input type="button" value="Cancel" @click=${this._resetBasicInfo} />
              </div>
            ` : nothing}
          ` : html`
              <div class="profile_instruction">${this._i18n.nothing_filled_out}</div>
          `}
          </div>
        </div>
      </div>
    `;
  }

  _renderPronunciationInfoBlock() {

    return html`
      <div>
        <div class="mainSection">
          <div class="mainSectionHeading">${this._i18n.pronunciation_info}</div>
          <button id="pronunciation-info-edit-button"
              type="button"
              class="btn btn-secondary float-end"
              @click=${this._editPronunciationInfo}>
            ${this._i18n.edit}
          </button>
          <div class="mainSectionContent">
          ${this._user.phoneticName || this._user.nameRecordingUrl ? html`
            <table class="profileContent">
              <tbody>
                ${this._user.phoneticName || this._editingPronunciationInfo ? html`
                <tr>
                  <td class="label">${this._i18n.phonetic_name_label}</td>
                  <td class="content">
                  ${this._editingPronunciationInfo ? html`
                    <input type="text" id="phonetic-name-input"
                        aria-label="${this._i18n.phonetic_name_label}"
                        class="formInputField"
                        .value="${this._user.phoneticName}">
                  ` : this._user.phoneticName}
                  </td>
                </tr>
                ` : nothing}
                ${this._user.nameRecordingUrl || this._editingPronunciationInfo ? html`
                <tr>
                  <td class="label">${this._i18n.pronunciation_record_label}</td>
                  <td class="content">
                    <div id="audio-player" class="audioPlayer">
                      <audio src="${this._user.nameRecordingUrl}" controls controlsList='nodownload'></audio>
                    </div>
                  </td>
                </tr>
                ` : nothing}
              </tbody>
            </table>
          ` : html`
              <div class="profile_instruction">${this._i18n.nothing_filled_out}</div>
          `}
          </div>
        </div>
      </div>
    `;
  }

  _renderContactInfoBlock() {

    return html`
      <div>
        <div class="mainSection">
          <div class="mainSectionHeading">${this._i18n.contact_info}</div>
          <button id="contact-info-edit-button"
              type="button"
              class="btn btn-secondary float-end"
              @click=${this._editContactInfo}>
            ${this._i18n.edit}
          </button>
          <div class="mainSectionContent">
          ${this._user.email || this._user.mobile ? html`
            <table class="profileContent">
              <tbody>
                <tr>
                  <td class="label">${this._i18n.email_label}</td>
                  <td class="content">${this._user.email}</td>
                </tr>
                <tr>
                  <td class="label">${this._i18n.mobile_label}</td>
                  <td class="content">${this._user.mobile}</td>
                </tr>
              </tbody>
            </table>
          ` : html`
              <div class="profile_instruction">${this._i18n.nothing_filled_out}</div>
          `}
          </div>
        </div>
      </div>
    `;
  }

  _renderSocialInfoBlock() {

    return html`
      <div>
        <div class="mainSection">
          <div class="mainSectionHeading">${this._i18n.social_info}</div>
          <button id="social-info-edit-button"
              type="button"
              class="btn btn-secondary float-end"
              @click=${this._editSocialInfo}>
            ${this._i18n.edit}
          </button>
          <div class="mainSectionContent">
          ${this._user.facebook || this._user.linkedin || this._user.instagram ? html`
            <table class="profileContent">
              <tbody>
                <tr>
                  <td class="label">${this._i18n.facebook_label}</td>
                  <td class="content">${this._user.facebook}</td>
                </tr>
                <tr>
                  <td class="label">${this._i18n.linkedin_label}</td>
                  <td class="content">${this._user.linkedin}</td>
                </tr>
                <tr>
                  <td class="label">${this._i18n.instagram_label}</td>
                  <td class="content">${this._user.instagram}</td>
                </tr>
              </tbody>
            </table>
          ` : html`
              <div class="profile_instruction">${this._i18n.nothing_filled_out}</div>
          `}
          </div>
        </div>
      </div>
    `;
  }

  render() {

    return html`

      <h3>${this._i18n["usevie.revandmod"]}</h3>

      <div class="d-flex">
        <div>
          <div>
            <sakai-user-photo user-id="${this.userId}" classes="medium"></sakai-user-photo>
          </div>
          <button type="button"
              class="btn btn-link ms-auto me-auto mb-1 b edit-image-button"
              data-bs-toggle="modal"
              data-bs-target="#profile-image-upload">
            <i class="si si-edit"></i>
            <span>Change picture</span>
          </button>
        </div>
        ${this._renderBasicInfoBlock()}
      </div>
      ${this._renderPronunciationInfoBlock()}
      ${this._renderContactInfoBlock()}
      ${this._renderSocialInfoBlock()}
    `;
  }

  static styles = [
  ];
}

/*
<fieldset>
        <legend>${this._i18n["usevie.use"]}</legend>
        <table class="table table-striped table-hover table-bordered">
          <tbody>
            <tr>
                <td><label>${this._i18n["usecre.typ"]}</label></td>
                <td>${this._user.type}</td>
            </tr>
            ${this._canEdit ? html`
            <tr>
                <td><label>${this._i18n.disabled}</label></td>
                <td>${this._user.disabled ? this._i18n.false : this._i18n.true}</td>
            </tr>
            <tr>
                <td><label>${this._i18n["useedi.creby"]}</label></td>
                <td>${this._user.creatorDisplayName}</td>
            </tr>
            <tr>
                <td><label>${this._i18n["useedi.cre"]}</label></td>
                <td>${this._user.formattedCreatedDate}</td>
            </div>
            <tr>
                <td><label>${this._i18n["useedi.modby"]}</label></td>
                <td>${this._user.modifierDisplayName}</td>
            </tr>
            <tr>
                <td><label>${this._i18n["useedi.mod"]}</label></td>
                <td>${this._user.formattedModifiedDate}</td>
            </tr>
            ` : nothing}
          </tbody>
        </table>

        ${this._canEdit ? html`
        <div class="act">
          <input type="button" class="active" value="${this._i18n["usevie.mod2"]}" accesskey="s" @click=${this._modify} />
        </div>
        ` : nothing}
      </fieldset>
*/
