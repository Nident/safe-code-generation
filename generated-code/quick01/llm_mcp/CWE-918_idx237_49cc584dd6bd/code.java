import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

private CloseableHttpResponse getHttpResponse(BaseMessage message) throws Exception {
    HttpRequestBase request;
    if (message instanceof CustomWebhookMessage) {
        CustomWebhookMessage webhook = (CustomWebhookMessage) message;
        request = constructHttpRequest(webhook.getMethod());
        request.setURI(buildUri(webhook.getScheme(), webhook.getHost(), webhook.getPort(),
                webhook.getPath(), webhook.getQueryParams()));

        Map<String, String> headers = webhook.getHeaderParams();
        if (headers == null || headers.isEmpty()) {
            request.setHeader("Content-Type", "application/json");
        } else {
            headers.forEach(request::addHeader);
        }
    } else {
        request = new HttpPost(message.getUrl());
        request.setHeader("Content-Type", "application/json");
    }

    if (request instanceof HttpEntityEnclosingRequest) {
        HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
        entityRequest.setEntity(new StringEntity(extractBody(message), StandardCharsets.UTF_8));
    }

    return HTTP_CLIENT.execute(request);
}
