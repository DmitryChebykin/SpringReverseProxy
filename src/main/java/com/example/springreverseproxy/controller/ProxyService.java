package com.example.springreverseproxy.controller;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URISyntaxException;

public interface ProxyService {
    ResponseEntity<String> processProxyRequest(String body, HttpMethod method, HttpServletRequest request, HttpServletResponse response, String trace) throws URISyntaxException;
}

