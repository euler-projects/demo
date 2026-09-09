/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eulerframework.uc.controller;

import org.eulerframework.security.web.endpoint.EulerSecurityEndpoints;
import org.eulerframework.security.web.endpoint.signup.EulerSecuritySignupEndpoint;
import org.eulerframework.security.web.endpoint.user.EulerSecurityUserEndpoint;
import org.eulerframework.web.core.base.controller.PageRender;
import org.eulerframework.web.core.base.controller.PageSupportWebController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public  class EntrancePageController extends PageSupportWebController implements EulerSecurityUserEndpoint, EulerSecuritySignupEndpoint {
    private String logoutProcessingUrl;
    private String loginSuccessRedirectParameter;

    protected EntrancePageController(PageRender pageRender) {
        super(pageRender);
    }

    /**
     * The shell renders this into a {@code <meta>} so the sign-out page can
     * post to the configured logout endpoint without hard-coding it.
     */
    @ModelAttribute("logoutProcessingUrl")
    public String getLogoutProcessingUrl() {
        return this.logoutProcessingUrl;
    }

    @Value("${" + EulerSecurityEndpoints.USER_LOGOUT_PROCESSING_URL_PROP_NAME + ":" + EulerSecurityEndpoints.USER_LOGOUT_PROCESSING_URL + "}")
    public void setLogoutProcessingUrl(String logoutProcessingUrl) {
        this.logoutProcessingUrl = logoutProcessingUrl;
    }

    @Override
    @GetMapping("${" + EulerSecurityEndpoints.SIGNUP_PAGE_PROP_NAME + ":" + EulerSecurityEndpoints.SIGNUP_PAGE + "}")
    public ModelAndView signupPage() {
        return this.display("entrance/index", false);
    }

    @Override
    public Object doSignup(String username, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    @GetMapping("${" + EulerSecurityEndpoints.USER_LOGIN_PAGE_PROP_NAME + ":" + EulerSecurityEndpoints.USER_LOGIN_PAGE + "}")
    public ModelAndView loginPage() {
        if (signedInAuthentication() != null) {
            // An authenticated session has nothing to collect on this page:
            // bounce straight to the pending target instead of rendering a
            // form the visitor no longer needs.
            return new ModelAndView("redirect:" + redirectTarget(this.getRequest().getParameter(this.loginSuccessRedirectParameter)));
        }
        return this.display("entrance/index", false);
    }

    /**
     * The signed-in user's name, or an empty string when anonymous. Always
     * rendered (empty rather than absent) so the SPA can tell a template-
     * rendered shell - where this controller already resolved the auth
     * state, so an empty marker authoritatively means anonymous - from a
     * statically deployed shell, which carries no marker at all and must
     * probe the session itself.
     */
    @ModelAttribute("currentUsername")
    public String getCurrentUsername() {
        Authentication authentication = signedInAuthentication();
        return authentication == null ? "" : authentication.getName();
    }

    /**
     * Reads the SecurityContext of the very request being served, so the
     * signed-in check costs no extra round trip and the login form never
     * flashes. The anonymous token counts as not signed in: on a permitAll
     * route it is present and "authenticated" for every anonymous visitor.
     */
    private static Authentication signedInAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication;
        }
        return null;
    }

    /**
     * Only relative paths are honoured: an absolute or protocol-relative
     * value would turn this bounce into an open redirect.
     */
    private static String redirectTarget(String redirectUrl) {
        if (redirectUrl != null && redirectUrl.startsWith("/") && !redirectUrl.startsWith("//")) {
            return redirectUrl;
        }
        return "/";
    }

    @Value("${" + EulerSecurityEndpoints.USER_LOGIN_SUCCESS_REDIRECT_PARAMETER_PROP_NAME + ":" + EulerSecurityEndpoints.USER_LOGIN_SUCCESS_REDIRECT_PARAMETER + "}")
    public void setLoginSuccessRedirectParameter(String loginSuccessRedirectParameter) {
        this.loginSuccessRedirectParameter = loginSuccessRedirectParameter;
    }

    @Override
    @GetMapping("${" + EulerSecurityEndpoints.USER_LOGOUT_PAGE_PROP_NAME + ":" + EulerSecurityEndpoints.USER_LOGOUT_PAGE + "}")
    public ModelAndView logoutPage() {
        return this.display("entrance/index", false);
    }
}
