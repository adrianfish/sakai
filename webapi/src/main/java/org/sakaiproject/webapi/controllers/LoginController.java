/******************************************************************************
 * Copyright 2015 sakaiproject.org Licensed under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.sakaiproject.webapi.controllers;

import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.Authentication;
import org.sakaiproject.user.api.AuthenticationException;
import org.sakaiproject.user.api.Evidence;
import org.sakaiproject.util.IdPwEvidence;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.user.api.AuthenticationManager;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.webapi.beans.ClientData;
import org.sakaiproject.webapi.beans.PublicKeyCredential;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.attestation.statement.AttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.none.NoneAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.packed.PackedAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.u2f.FIDOU2FAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.trustworthiness.certpath.NullCertPathTrustworthinessVerifier;
import com.webauthn4j.verifier.attestation.trustworthiness.self.DefaultSelfAttestationTrustworthinessVerifier;
import com.webauthn4j.verifier.exception.VerificationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class LoginController extends AbstractSakaiApiController {

	@Autowired
    private PreferencesService preferencesService;

	@Autowired
	private ServerConfigurationService serverConfigurationService;

	@Autowired
	private SiteService siteService;

	@Autowired
	private SessionManager sessionManager;

	@Autowired
	private UsageSessionService usageSessionService;

	@Autowired
	private UserDirectoryService userDirectoryService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TimeService timeService;

    private WebAuthnManager webAuthnManager;

    @PostConstruct
    public void init() {

        List<AttestationStatementVerifier> attestationStatementVerifiers = List.of(new FIDOU2FAttestationStatementVerifier(), new NoneAttestationStatementVerifier(), new PackedAttestationStatementVerifier());

        webAuthnManager = new WebAuthnManager(attestationStatementVerifiers,
                                            new NullCertPathTrustworthinessVerifier(),
                                            new DefaultSelfAttestationTrustworthinessVerifier());
    }

	@GetMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> login(@RequestParam String username, @RequestParam String password, HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        Evidence e = new IdPwEvidence(username, password, request.getRemoteAddr());
        Authentication a = authenticationManager.authenticate(e);

        return loginAndEstablishSession(a.getUid(), request, response);
	}

	@GetMapping("/logout")
    public void logout() throws AuthenticationException {

        usageSessionService.logout();
    }

	@GetMapping(value = "/login/initiate-webauthn-registration", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> initWebAuthnRegistration(HttpServletRequest request) {

        // This is called when a user has already authenticated using the traditional user/password
        // method, using their Sakai credentials. So, we have their user id and we need to store
        // it so we can setup the Sakai session when they authenticate with webauthn in future.
        
        String userId = sessionManager.getCurrentSessionUserId();

        if (StringUtils.isBlank(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = null;

        try {
            user = userDirectoryService.getUser(userId);
        } catch (Exception e) {
            log.error("No user for id: {}", userId);
            return ResponseEntity.internalServerError().build();
        }

        int[] array = (new Random().ints(0, 100)).limit(32).toArray();
        ByteBuffer byteBuffer = ByteBuffer.allocate(array.length * Integer.BYTES);
        byteBuffer.asIntBuffer().put(array);

        String base64Challenge = Base64.getUrlEncoder().encodeToString(byteBuffer.array());

        Session session = sessionManager.getCurrentSession();
        session.setAttribute("challenge", base64Challenge);

        return ResponseEntity.ok(Map.<String, Object>of("publicKey", Map.<String, Object>of
            (
                "challenge", base64Challenge,
                "rp", Map.of("id", serverConfigurationService.getServerName(), "name", serverConfigurationService.getString("ui.service", "Sakai")),
                "user", Map.of("id", userId, "name", user.getEid(), "displayName", user.getDisplayName()),
                "pubKeyCredParams", List.of(Map.of("type", "public-key", "alg", "-7"), Map.of("type", "public-key", "alg", "-257")),
                "authenticatorSelection", Map.of("userVerification", "preferred", "authenticatorAttachment", "platform"),
                "extensions", Map.of("credProps", true)
            )));
    }

	@PostMapping(value = "/login/complete-webauthn-registration")
    public ResponseEntity completeWebAuthnRegistration(@RequestBody PublicKeyCredential credential) {

        if (StringUtils.isBlank(sessionManager.getCurrentSessionUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ClientData clientData = objectMapper.readValue(Base64.getDecoder().decode(credential.response.clientDataJSON), ClientData.class);

            byte[] attestationObject = Base64.getUrlDecoder().decode(credential.response.attestationObject.getBytes("UTF-8"));
            byte[] clientDataJSON = Base64.getUrlDecoder().decode(credential.response.clientDataJSON.getBytes("UTF-8"));

            RegistrationRequest rr = new RegistrationRequest(attestationObject, clientDataJSON, null, credential.response.transports);

            Session session = sessionManager.getCurrentSession();

            String registrationChallenge = (String) session.getAttribute("challenge");

            ServerProperty serverProperty = new ServerProperty(
                    new Origin(serverConfigurationService.getServerUrl()),
                    serverConfigurationService.getServerName(),
                    new DefaultChallenge(registrationChallenge));

            try {
                RegistrationData registrationData = webAuthnManager.verify(rr, new RegistrationParameters(serverProperty, null, true));

                CredentialRecord credentialRecord
                    = new CredentialRecordImpl(registrationData.getAttestationObject(),
                        registrationData.getCollectedClientData(),
                        registrationData.getClientExtensions(),
                        registrationData.getTransports());

                authenticationManager.registerWebAuthnCredential(credentialRecord);
            } catch (VerificationException ve) {
                log.warn("Failed to register webauthn device: {}", ve.toString());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            log.error("Failed to register webauthn device: {}", e.toString());
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }

	@GetMapping(value = "/login/initiate-webauthn-login", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> initiateWebAuthnLogin() {

        // This is now a clean sheet scenario. We may not have a user session at all, so we need a
        // way of associating a user's device with a public key.

        int[] array = (new Random().ints(0, 100)).limit(32).toArray();
        ByteBuffer byteBuffer = ByteBuffer.allocate(array.length * Integer.BYTES);
        byteBuffer.asIntBuffer().put(array);
        String base64Challenge = Base64.getUrlEncoder().encodeToString(byteBuffer.array());

        // Put the challenge in the session. We need it when login is completed and we don't know
        // which Sakai user is trying to login via webauthn, so we can't store it in the db.
        Session session = sessionManager.getCurrentSession();
        session.setAttribute("challenge", base64Challenge);

        return Map.of(
                    "publicKey", Map.of(
                        "challenge", base64Challenge,
                        "userVerification", "discouraged")
                );
    }

	@PostMapping(value = "/login/complete-webauthn-login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity webAuthnLogin(@RequestBody PublicKeyCredential cred, HttpServletRequest request, HttpServletResponse response) {

        Base64.Decoder decoder = Base64.getUrlDecoder();

        final byte[] credentialId;
        try {
            credentialId = decoder.decode(cred.id.getBytes("UTF-8"));
        } catch (Exception e) {
            log.warn("Failed to decode webauthn credential id {}: {}", cred.id, e.toString());
            return ResponseEntity.badRequest().build();
        }

        return authenticationManager.getWebAuthnCredential(credentialId).map(credentialMap -> {

            try {
                byte[] userHandle = cred.response.userHandle != null ? decoder.decode(cred.response.userHandle.getBytes("UTF-8")) : null;
                byte[] authenticatorData = decoder.decode(cred.response.authenticatorData.getBytes("UTF-8"));
                byte[] clientDataJSON = decoder.decode(cred.response.clientDataJSON.getBytes("UTF-8"));
                byte[] signature = decoder.decode(cred.response.signature.getBytes("UTF-8"));

                AuthenticationData authenticationData
                    = webAuthnManager.parse(new AuthenticationRequest(
                        credentialId,
                        userHandle,
                        authenticatorData,
                        clientDataJSON,
                        cred.clientExtensionJSON,
                        signature
                    ));

                Session session = sessionManager.getCurrentSession();
                String authenticationChallenge = ((String) session.getAttribute("challenge"));
                session.removeAttribute("challenge");

                ServerProperty serverProperty = new ServerProperty(
                        new Origin(serverConfigurationService.getServerUrl()),
                        serverConfigurationService.getServerName(),
                        new DefaultChallenge(authenticationChallenge.substring(0, authenticationChallenge.length() - 1)));

                CredentialRecord credentialRecord = (CredentialRecord) credentialMap.get("credentialRecord");

                webAuthnManager.verify(authenticationData, new AuthenticationParameters(
                        serverProperty,
                        credentialRecord,
                        null,
                        true,
                        false
                    ));

                return loginAndEstablishSession((String) credentialMap.get("userId"), request, response);
            } catch (Exception e) {
                log.warn("Failed to authenticate vi webauthn: {}", e.toString());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }).orElse(ResponseEntity.badRequest().build());
    }

    private ResponseEntity<Map<String, String>> loginAndEstablishSession(String userId, HttpServletRequest request, HttpServletResponse response) {

        String ipAddress = request.getRemoteAddr();

        Session s = sessionManager.startSession();

        if (s == null) {
            log.warn("/api/login failed to establish session for userid={} ip={}", userId, ipAddress);
            throw new RuntimeException("Unable to establish session");
        } else {
            // We do not care too much on the off-chance that this fails - folks simply won't show up in presense
            // and events won't be trackable back to people / IP Addresses - but if it fails - there is nothing
            // we can do anyway.
            
            sessionManager.setCurrentSession(s);

            User user;
            try {
                user = userDirectoryService.getUser(userId);
            } catch (UserNotDefinedException unde) {
                return ResponseEntity.badRequest().build();
            }

            usageSessionService.login(userId, user.getEid(), ipAddress, "/api/login", UsageSessionService.EVENT_LOGIN_WS);

            log.debug("/api/login userid={} ip={} session={}", userId, ipAddress, s.getId());

            String cookieName = "JSESSIONID";

            // retrieve the configured cookie name, if any
            if (System.getProperty(RequestFilter.SAKAI_COOKIE_PROP) != null) {
                cookieName = System.getProperty(RequestFilter.SAKAI_COOKIE_PROP);
            }

            // retrieve the configured cookie domain, if any

            // compute the session cookie suffix, based on this configured server id
            String suffix = System.getProperty(RequestFilter.SAKAI_SERVERID);
            if (StringUtils.isEmpty(suffix)) {
                suffix = "sakai";
            }

            Cookie c = new Cookie(cookieName, s.getId() + "." + suffix);
            c.setPath("/");
            c.setMaxAge(2630000);
            if (System.getProperty(RequestFilter.SAKAI_COOKIE_DOMAIN) != null) {
                c.setDomain(System.getProperty(RequestFilter.SAKAI_COOKIE_DOMAIN));
            }
            if (request.isSecure() == true) {
                c.setSecure(true);
            }

            if (response != null) {
                response.addCookie(c);
            }

            log.debug("/api/login userid={} ip={} session={}", userId, ipAddress, s.getId());

            String locale = preferencesService.getLocale(userId).toString();

            TimeZone userTz = timeService.getLocalTimeZone();
            return ResponseEntity.ok(Map.of("id", userId,
                                            "eid", user.getEid(),
                                            "displayName", user.getDisplayName(),
                                            "timezone", userTz.getID(),
                                            "locale", locale));
        }
    }
}
