package org.example.fcgiServer;


import com.fastcgi.FCGIInterface;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class Main {
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String RESULT_JSON = """
            {
                "answer": %b,
                "executionTime": "%s",
                "serverTime": "%s"
            }
            """;
    private static final String HTTP_RESPONSE = """
        HTTP/1.1 200 OK
        Content-Type: application/json
        Content-Length: %d
        
        %s
        """;
    private static final String HTTP_ERROR = """
        HTTP/1.1 400 Bad Request
        Content-Type: application/json
        Content-Length: %d
        
        %s
        """;
    private static final String ERROR_JSON = """
        {
            "reason": "%s"
        }
        """;

    public static void main(String[] args) throws IOException {
        var fcgiInterface = new FCGIInterface();
        while (fcgiInterface.FCGIaccept() >= 0) {
            long startTime = System.nanoTime();
            try {
                var queryParams = System.getProperties().getProperty("QUERY_STRING");

                // Разбираем QUERY_STRING
                Map<String, String> params = new HashMap<>();
                if (queryParams != null && !queryParams.isEmpty()) {
                    String[] pairs = queryParams.split("&");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=");
                        if (keyValue.length == 2) {
                            params.put(keyValue[0], keyValue[1]);
                        }
                    }
                }

                double x = Double.parseDouble(params.getOrDefault("x", "Неизвестный"));
                double y = Double.parseDouble(params.getOrDefault("y", "Неизвестный"));
                double r = Double.parseDouble(params.getOrDefault("r", "Неизвестный"));

                boolean insideRectangle = isInsideRectangle(x,y,r);
                boolean insidePolygon = isInsidePolygon(x,y,r);
                boolean insidePath = isInsidePath(x,y,r);

                var json = String.format(RESULT_JSON, insideRectangle || insidePolygon || insidePath, System.nanoTime() - startTime, LocalDateTime.now().format(formatter));
                var responseBody = json.trim(); // Удаляем лишние пробелы
                var response = String.format(HTTP_RESPONSE, responseBody.getBytes(StandardCharsets.UTF_8).length, responseBody);
                try {
                    FCGIInterface.request.outStream.write(response.getBytes(StandardCharsets.UTF_8));
                    FCGIInterface.request.outStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (Exception ex) {
                var json = String.format(ERROR_JSON,ex.getMessage());
                var responseBody = json.trim(); // Удаляем лишние пробелы
                var response = String.format(HTTP_ERROR, responseBody.getBytes(StandardCharsets.UTF_8).length, responseBody);
                FCGIInterface.request.outStream.write(response.getBytes(StandardCharsets.UTF_8));
                FCGIInterface.request.outStream.flush();
            }
        }
    }

    private static boolean isInsideRectangle(double x, double y,double r) {
        return x <= 0 && y <= 0 && (x + y) >= -r;
    }

    private static boolean isInsidePolygon(double x, double y,double r){
        return x>=0 && x<=r && y<=0 && y>=(-r)/2;
    }

    private static boolean isInsidePath(double x, double y,double r) {
        double centerX = 0;
        double centerY = 0;
        double radius = r / 2;
        return centerY<=y && y<=radius && (-radius)<=x && x<=centerX && (x*x + y*y)<radius*radius;
    }
}