public static class HttpException extends RuntimeException {
    private final HttpResponse response;

    public HttpException(HttpResponse response) {
        this.response = response;
    }

    public HttpResponse response() {
        return response;
    }
}
