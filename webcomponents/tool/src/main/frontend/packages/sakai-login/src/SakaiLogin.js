import { SakaiElement } from "@sakai-ui/sakai-element";
import { html } from "lit";

export class SakaiLogin extends SakaiElement {

  static properties = {
    _username: { state: true },
    _password: { state: true },
    _i18n: { state: true },
  };

  constructor() {

    super();

    this._username = "";
    this._password = "";
  }

  _usernameChanged(e) { this._username = e.target.value; }

  _passwordChanged(e) { this._password = e.target.value; }

  _login() {

    const url = `/api/login?username=${this._username}&password=${this._password}`;
    fetch(url, { credentials: "include" })
    .then(r => {

      if (r.ok) {
        return r.json();
      }
      throw Error(`Error while logging in at ${url}`);
    })
    .then(user => {

      console.debug(`Logged in at ${url}. UserId: ${user.userId}`);
      this.dispatchEvent(new CustomEvent("login-successful", { detail: { user } }));
    })
    .catch(error => console.error(error));
  }

  /*
  shouldUpdate() {
    return this._i18n;
  }
  */

  render() {

    return html`
      <div class="m-4">
      <label for="username" class="form-label">Username</label>
      <input id="username" .value=${this._username} @change=${this._usernameChanged} type="text" class="form-control mb-2 " autocomplete="username">

      <label for="pw" class="form-label">Password</label>
      <div class="input-group mb-3 password-field">
        <input type="password" .value=${this._password} @change=${this._passwordChanged} class="form-control " autocomplete="current-password"></input>
        <input type="checkbox" class="btn-check" id="showPw" autocomplete="off">
        <label class="input-group-text" for="showPw">
          <i class="bi bi-eye-slash-fill" aria-hidden="true"></i>
          <span class="visually-hidden">Show Password</span>
        </label>
      </div>
      <div class="d-flex gap-2 mb-2">
        <button type="button" class="btn btn-primary btn-lg flex-grow-1" @click=${this._login}>Log in</button>
      </div>
      <div class="text-end px-3 py-2">
        <a href="http://localhost/portal/site/!gateway/page/!gateway-700" class="text-end right">Forgot your password?</a>
      </div>
      </div>
    `;
  }
}
