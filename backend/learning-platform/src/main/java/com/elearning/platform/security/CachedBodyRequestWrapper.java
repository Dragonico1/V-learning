package com.elearning.platform.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Permite leer el cuerpo dos veces: una para inspeccionarlo y otra para el controlador. */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cuerpo;

    public CachedBodyRequestWrapper(HttpServletRequest request, byte[] cuerpo) {
        super(request);
        this.cuerpo = cuerpo;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream origen = new ByteArrayInputStream(cuerpo);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return origen.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read() {
                return origen.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
