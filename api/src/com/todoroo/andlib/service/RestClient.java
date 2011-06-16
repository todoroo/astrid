package com.todoroo.andlib.service;

import java.io.IOException;

import org.apache.http.HttpEntity;

/**
 * RestClient stub invokes the HTML requests as desired
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public interface RestClient {
    public String get(String url) throws IOException;
    public String post(String url, HttpEntity data) throws IOException;
}