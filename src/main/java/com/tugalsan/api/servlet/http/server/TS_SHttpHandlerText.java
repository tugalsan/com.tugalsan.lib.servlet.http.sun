package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.*;
import com.tugalsan.api.callable.client.*;
import com.tugalsan.api.file.client.TGS_FileTypes;
import com.tugalsan.api.tuple.client.TGS_Tuple2;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import java.nio.charset.*;

public class TS_SHttpHandlerText extends TS_SHttpHandlerAbstract {

    private TS_SHttpHandlerText(String slash_path, TGS_CallableType1<TGS_Tuple2<TGS_FileTypes, String>, HttpExchange> handle) {
        super(slash_path, handle);
    }

    public static TS_SHttpHandlerText of(String slash_path, TGS_CallableType1<TGS_Tuple2<TGS_FileTypes, String>, HttpExchange> handle) {
        return new TS_SHttpHandlerText(slash_path, handle);
    }

    @Override
    public void handle(HttpExchange exchange) {
        /*
        Noted should be that the response.length() part in their example is bad, 
        it should have been response.getBytes().length. 
        Even then, the getBytes() method must explicitly 
        specify the charset which you then specify in the response header. 
        Alas, albeit misguiding to starters, it's after all just a basic kickoff example.
         */
        TGS_UnSafe.run(() -> {
            //GET PAYLOAD
            var payload = handle.call(exchange);
            {//SET HEADER
                var headers = exchange.getResponseHeaders();
                headers.add("Access-Control-Allow-Origin", "*");
                headers.set("Content-Type", payload.value0.content);
            }
            {//SEND DATA
                var data = payload.value1.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, data.length);
                try (var responseBody = exchange.getResponseBody()) {
                    responseBody.write(data);
                }
            }
        }, e -> e.printStackTrace());
    }

    //TODO BELOW
//    @SuppressWarnings("restriction")
//    public class HttpRequestHandler implements HttpHandler {
//
//        private static final String F_NAME = "fname";
//        private static final String L_NAME = "lname";
//
//        private static final int PARAM_NAME_IDX = 0;
//        private static final int PARAM_VALUE_IDX = 1;
//
//        private static final int HTTP_OK_STATUS = 200;
//
//        private static final String AND_DELIMITER = "&";
//        private static final String EQUAL_DELIMITER = "=";
//
//        public void handle(HttpExchange t) throws IOException {
//
//            //Create a response form the request query parameters
//            URI uri = t.getRequestURI();
//            String response = createResponseFromQueryParams(uri);
//            System.out.println("Response: " + response);
//            //Set the response header status and length
//            t.sendResponseHeaders(HTTP_OK_STATUS, response.getBytes().length);
//            //Write the response string
//            OutputStream os = t.getResponseBody();
//            os.write(response.getBytes());
//            os.close();
//        }
//
//        /**
//         * Creates the response from query params.
//         *
//         * @param uri the uri
//         * @return the string
//         */
//        private String createResponseFromQueryParams(URI uri) {
//
//            String fName = "";
//            String lName = "";
//            //Get the request query
//            String query = uri.getQuery();
//            if (query != null) {
//                System.out.println("Query: " + query);
//                String[] queryParams = query.split(AND_DELIMITER);
//                if (queryParams.length > 0) {
//                    for (String qParam : queryParams) {
//                        String[] param = qParam.split(EQUAL_DELIMITER);
//                        if (param.length > 0) {
//                            for (int i = 0; i < param.length; i++) {
//                                if (F_NAME.equalsIgnoreCase(param[PARAM_NAME_IDX])) {
//                                    fName = param[PARAM_VALUE_IDX];
//                                }
//                                if (L_NAME.equalsIgnoreCase(param[PARAM_NAME_IDX])) {
//                                    lName = param[PARAM_VALUE_IDX];
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            return "Hello, " + fName + " " + lName;
//        }
//    }
}
