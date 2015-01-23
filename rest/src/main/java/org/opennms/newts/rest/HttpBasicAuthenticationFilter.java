/*
 * Copyright 2015, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.rest;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

public class HttpBasicAuthenticationFilter implements Filter {

    private final static Logger LOG = LoggerFactory.getLogger(HttpBasicAuthenticationFilter.class);
    private final static String m_realm = "Newts";

    private final NewtsConfig m_config;

    public HttpBasicAuthenticationFilter(NewtsConfig config) {
        m_config = checkNotNull(config, "config argument");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.info("HTTP Basic Auth servlet filter initialized");
    }

    @Override
    public void destroy() {
        LOG.info("Shutting down HTTP Basic Auth servlet filter");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        LOG.trace("doFilter()");

        if (enabled()) {
            LOG.trace("Authentication is enabled");
            HttpServletRequest request = (HttpServletRequest)servletRequest;
            HttpServletResponse response = (HttpServletResponse)servletResponse;
            Optional<String> authHeader = getAuthorizationHeader(request);

            if (authHeader.isPresent()) {
                Credentials credentials = Credentials.fromHeader(authHeader.get());
                if (!isAuthorized(credentials)) {
                    LOG.trace("Credentials do NOT match; Authorizationi failed");
                    sendUnauthorized(response);
                    return;    // Stop processing filters on failed authentication
                }

                LOG.trace("User {} is authorized", credentials.getUser());
            }
            else {
                LOG.trace("Missing Authorization HTTP header; Authorization failed");
                sendUnauthorized(response);
                return;    // Stop processing filters on failed authentication
            }

        }
        else {
            LOG.trace("Authentication is NOT enabled (skipping...)");
        }

        chain.doFilter(servletRequest, servletResponse);

    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        sendUnauthorized(response, "Unauthorized");
    }

    private void sendUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + m_realm + "\"");
        response.sendError(401, msg);
    }

    private boolean isAuthorized(Credentials credentials) {
        Map<String, String> passwords = m_config.getAuthenticationConfig().getCredentials();
        String user = credentials.getUser(), pass = credentials.getPass();

        if (passwords.containsKey(user) && (passwords.get(user) != null)) {
            return passwords.get(user).equals(pass);
        }

        return false;
    }

    private boolean enabled() {
        return m_config.getAuthenticationConfig().isEnabled();
    }
    
    private static Optional<String> getAuthorizationHeader(HttpServletRequest request) {
        String v = trim(request.getHeader("Authorization"));
        return (v != null) ? Optional.of(v) : Optional.<String>absent();
    }

    /** Trim string if not null. */
    private static String trim(String s) {
        return (s != null) ? s.trim() : s;
    }

    static class Credentials {
        private static final Pattern s_headerPattern = Pattern.compile("Basic (?<token>.+)", Pattern.CASE_INSENSITIVE);
        private static final Pattern s_credsPattern  = Pattern.compile("(?<user>.+):(?<pass>.+)");

        private final String m_user;
        private final String m_pass;

        Credentials(String user, String pass) {
            m_user = checkNotNull(user, "user argument");
            m_pass = checkNotNull(pass, "pass argument");
        }

        String getUser() {
            return m_user;
        }

        String getPass() {
            return m_pass;
        }

        /** Creates a {@link Credentials} instance from an HTTP basic authentication header value. */
        static Credentials fromHeader(String headerValue) {
            String encoded, decoded;
            Matcher matcher;

            matcher = s_headerPattern.matcher(headerValue);

            if (matcher.matches()) {
                encoded = matcher.group("token");
            }
            else {
                throw new IllegalArgumentException("malformed credentials header");
            }

            try {
                decoded = new String(Base64.decodeBase64(encoded), "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                throw Throwables.propagate(e);
            }

            matcher = s_credsPattern.matcher(decoded);

            if (matcher.matches()) {
                return new Credentials(matcher.group("user"), matcher.group("pass"));
            }
            else {
                throw new IllegalArgumentException("malformed credentials header");
            }
        }

    }

}
