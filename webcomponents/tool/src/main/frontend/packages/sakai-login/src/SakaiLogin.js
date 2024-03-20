import { SakaiElement } from "@sakai-ui/sakai-element";
import { html, nothing } from "lit";
import { create, get, parseCreationOptionsFromJSON, parseRequestOptionsFromJSON } from "@github/webauthn-json/browser-ponyfill";

export class SakaiLogin extends SakaiElement {

  static properties = {
    _username: { state: true },
    _password: { state: true },
    _state: { state: true },
    _user: { state: true },
    _i18n: { state: true },
    _supportsPasskeys: { state: true },
  };

  constructor() {

    super();

    this._state = SakaiLogin.USER_PASS;

    this._supportsPasskeys = (typeof PublicKeyCredential !== "undefined");

    this._username = "";
    this._password = "";
  }

  _usernameChanged(e) { this._username = e.target.value; }

  _passwordChanged(e) { this._password = e.target.value; }

  async _createPasskey() {

    const initUrl = "/api/login/initiate-webauthn-registration";
    const request = fetch(initUrl);

    console.log(request);

    const json = await (await request).json();
    console.log(json);
    const credential = await create(parseCreationOptionsFromJSON(json));

    const registerUrl = "/api/login/complete-webauthn-registration";
    fetch(registerUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(credential),
    })
    .then(r => {

      if (r.ok) {
        this._dispatchLoginSuccessful();
      } else {
        console.warn("WEBAUTHN REGISTRATION FAILED");
      }
    });
  }

  _loginWithPasskey() {

    if (!this._online) return;

    const authnInitUrl = "/api/login/initiate-webauthn-login";
    fetch(authnInitUrl)
      .then(r => r.json())
      .then(data => {

        get(parseRequestOptionsFromJSON(data))
          .then(credential => {

            const authnUrl = "/api/login/complete-webauthn-login";
            fetch(authnUrl, {
              headers: { "Content-Type": "application/json" },
              method: "POST",
              body: JSON.stringify(credential),
            })
            .then(r => {

              if (r.ok) {
                return r.json();
              }

              console.warn("WEBAUTHN LOGIN FAILED");
            })
            .then(user => {

              this._user = user;
              this._dispatchLoginSuccessful();
            });
          })
          .catch(error => console.error(error));
      });
  }

  _login() {

    const url = `/api/login?username=${this._username}&password=${this._password}`;
    fetch(url)
    .then(r => {

      if (r.ok) {
        return r.json();
      }
      throw Error(`Error while logging in at ${url}`);
    })
    .then(user => {

      this._user = user;

      console.debug(`Logged in at ${url}. UserId: ${user.userId}`);
      user.password = this._password;

      if (this._supportsPasskeys) {
        this._state = SakaiLogin.REGISTER_PASSKEY;
      } else {
        this._dispatchLoginSuccessful();
      }
    })
    .catch(error => console.error(error));
  }

  _dispatchLoginSuccessful() {
    this.dispatchEvent(new CustomEvent("login-successful", { detail: { user: this._user } }));
  }

  render() {

    return html`
      ${this._state === SakaiLogin.USER_PASS ? html`
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
        ${this._supportsPasskeys ? html`
          <div class="text-center fs-4 my-3"><span>== OR ==</span></div>
          <div class="text-center mt-3">
            <button type="button" class="btn btn-secondary" @click=${this._loginWithPasskey}>Login with a Passkey</button>
          </div>
        ` : nothing}
      ` : nothing}

      ${this._state === SakaiLogin.REGISTER_PASSKEY && this._supportsPasskeys ? html`
        <h3 class="text-center mb-2">Login Successful</h3>
        <div class="mt-3">
          <div class="w-75 mx-auto">
            Now you've logged in using your username and password, you can create a passkey. Passkeys
            enable you to login just with your fingerprint, faceid or maybe your arseprint. If you want
            to create a passkey, click "Create a Passkey" below. If not, then hit "No, thanks".
          </div>
        </div>
        <div class="text-center mt-3">
          <button type="button" class="btn btn-primary" @click=${this._createPasskey}>Create a Passkey</button>
        </div>
        <div class="text-center mt-3">
          <button type="button" class="btn btn-link" @click=${this._dispatchLoginSuccessful}>No, thanks</button>
        </div>
      ` : nothing}
    `;
  }
}

SakaiLogin.USER_PASS = "USER_PASS";
SakaiLogin.REGISTER = "REGISTER_PASSKEY";
