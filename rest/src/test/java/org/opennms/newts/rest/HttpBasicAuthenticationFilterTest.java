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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.opennms.newts.rest.HttpBasicAuthenticationFilter.Credentials;

import com.google.common.base.Charsets;

public class HttpBasicAuthenticationFilterTest {

    private static final String m_user = "eevans";
    private static final String m_pass = "qwerty";

    private HttpServletRequest m_request;
    private HttpServletResponse m_response;
    private FilterChain m_chain;
    private NewtsConfig m_newtsConfig;
    private AuthenticationConfig m_authConfig;
    private HttpBasicAuthenticationFilter m_filter;

    @Before
    public void setUp() {
        m_request = mock(HttpServletRequest.class);
        m_response = mock(HttpServletResponse.class);
        m_chain = mock(FilterChain.class);
        m_newtsConfig = mock(NewtsConfig.class);
        m_authConfig = mock(AuthenticationConfig.class);
        m_filter = new HttpBasicAuthenticationFilter(m_newtsConfig);

        when(m_request.getHeader("Authorization")).thenReturn(basicAuthHeader(m_user, m_pass));
        when(m_request.getMethod()).thenReturn("GET");
        when(m_authConfig.isEnabled()).thenReturn(true);
        when(m_authConfig.getCredentials()).thenReturn(Collections.singletonMap(m_user, m_pass));
        when(m_newtsConfig.getAuthenticationConfig()).thenReturn(m_authConfig);
    }

    @Test
    public void testCorsPreflight() throws IOException, ServletException {
        // Authorization cannot succeed.
        when(m_request.getHeader("Authorization")).thenReturn(null);

        // Rejigger request instance for CORS preflight
        when(m_request.getMethod()).thenReturn("OPTIONS");
        when(m_request.getHeader("Origin")).thenReturn("http://host.example.com");
        when(m_request.getHeader("Access-Control-Request-Method")).thenReturn("POST");

        m_filter.doFilter(m_request, m_response, m_chain);
        verify(m_chain).doFilter(m_request, m_response);
    }
    
    @Test
    public void testFilterEnabled() throws IOException, ServletException {

        // Authentication is enabled, credentials are correct
        m_filter.doFilter(m_request, m_response, m_chain);

        verify(m_chain).doFilter(m_request, m_response);

        // Bad credentials; Authentication should fail
        when(m_request.getHeader("Authorization")).thenReturn(basicAuthHeader("peter", "sparkles"));

        m_filter.doFilter(m_request, m_response, m_chain);

        verify(m_response).sendError(eq(401), any(String.class));

    }

    @Test
    public void testFilterDisabled() throws IOException, ServletException {

        // Credentials are bad, but authentication is disabled.
        when(m_request.getHeader("Authorization")).thenReturn(basicAuthHeader("peter", "sparkles"));
        when(m_authConfig.isEnabled()).thenReturn(false);

        m_filter.doFilter(m_request, m_response, m_chain);

        verify(m_chain).doFilter(m_request, m_response);

    }

    @Test
    public void testCredentials() {
        String user = "eevans", pass = "qwerty";
        Credentials credentials = Credentials.fromHeader(basicAuthHeader(user, pass));

        assertThat(credentials.getUser(), is(user));
        assertThat(credentials.getPass(), is(pass));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testMalformedCredentialsHeader() {
        Credentials.fromHeader(String.format("Basically %s", base64Encode("eevans:qwerty")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMalformedCredentialsValue() {
        Credentials.fromHeader(String.format("Basic %s", base64Encode("eevans|qwerty")));
    }

    private String basicAuthHeader(String user, String pass) {
        return String.format("Basic %s", base64Encode(String.format("%s:%s", user, pass)));
    }

    private String base64Encode(String input) {
        return new String(Base64.encodeBase64(input.getBytes(Charsets.UTF_8)));
    }

}
