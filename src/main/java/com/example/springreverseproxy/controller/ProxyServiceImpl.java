package com.example.springreverseproxy.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Log4j2
public class ProxyServiceImpl implements ProxyService {
    @Override
    @Retryable(exclude = {HttpStatusCodeException.class}, include = Exception.class, backoff = @Backoff(delay = 5000, multiplier = 4.0), maxAttempts = 4)
    public ResponseEntity<String> processProxyRequest(String body, HttpMethod method, HttpServletRequest request, HttpServletResponse response, String traceId) throws URISyntaxException {
        ThreadContext.put("traceId", traceId);
        String requestUrl = request.getRequestURI().substring(6);

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();

        String domain = null;
        int port = -1;

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            if ("proxy-domain".equals(headerName)) {
                String[] strings = request.getHeader(headerName).split(":");
                domain = strings[0];
                port = Integer.parseInt(strings[1]);
            } else {
                headers.set(headerName, request.getHeader(headerName));
            }
        }

        headers.set("TRACE-PROXY", traceId);
        headers.remove(HttpHeaders.ACCEPT_ENCODING);

        //log if required in this line
        URI uri = new URI("https", null, domain, port, null, null, null);

        // replacing context path form urI to match actual gateway URI
        uri = UriComponentsBuilder.fromUri(uri)
                .path(requestUrl)
                .query(request.getQueryString())
                .build(true).toUri();

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);

        try {
            ResponseEntity<String> serverResponse = restTemplate.exchange(uri, method, httpEntity, String.class);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.put(HttpHeaders.CONTENT_TYPE, Objects.requireNonNull(serverResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE)));
            log.info(serverResponse);
            log.log(Level.valueOf("CUSTOM"), "Кастомный логгер");
            return serverResponse;
        } catch (HttpStatusCodeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(e.getRawStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString());
        }
    }

    @Recover
    public ResponseEntity<String> recoverFromRestClientErrors(Exception e, String body,
                                                              HttpMethod method, HttpServletRequest request, HttpServletResponse response, String traceId) {
        log.error("retry method for the following url " + request.getRequestURI() + " has failed" + e.getMessage());
        log.error(e.getStackTrace());
        throw new RuntimeException("There was an error trying to process you request. Please try again later");
    }
}


