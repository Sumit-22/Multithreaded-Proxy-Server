package com.example.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class HttpResponseWriter {
    private static final DateTimeFormatter RFC_1123_DATE = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US);

    public static void write(OutputStream out, HttpResponse resp) throws IOException {
        byte[] body = resp.body() == null ? new byte[0] : resp.body();

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(resp.status()).append(' ').append(resp.reason()).append("\r\n");
        sb.append("Date: ").append(RFC_1123_DATE.format(ZonedDateTime.now())).append("\r\n");
        sb.append("Server: Java-MT-Server/1.0\r\n");
        sb.append("Connection: close\r\n");
        sb.append("Content-Length: ").append(body.length).append("\r\n");
        resp.headers().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\r\n"));
        sb.append("\r\n");

        out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
        if (body.length > 0) out.write(body);
        out.flush();
    }
}
