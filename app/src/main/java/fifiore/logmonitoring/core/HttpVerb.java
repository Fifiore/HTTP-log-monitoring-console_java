package fifiore.logmonitoring.core;

class HttpVerb {

    enum Values {
        POST, GET, PUT, PATCH, DELETE, NONE;

        static Values fromText(String value) {
            if ("POST".equals(value)) {
                return POST;
            } else if ("GET".equals(value)) {
                return GET;
            } else if ("PUT".equals(value)) {
                return PUT;
            } else if ("PATCH".equals(value)) {
                return PATCH;
            } else if ("DELETE".equals(value)) {
                return DELETE;
            }
            return NONE;
        }
    }

    static Values fromRequest(String httpRequest) {
        return Values.fromText(extractOperation(httpRequest));
    }

    private static String extractOperation(String request) {
        String[] operation = request.split(" ");
        if (operation.length == 0) {
            return "";
        }
        return operation[0];
    }

    private HttpVerb() {}
}
