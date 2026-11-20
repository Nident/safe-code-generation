Task: task-07

Signature:
private CloseableHttpResponse getHttpResponse(BaseMessage message) throws Exception

Functional requirements:
Create and execute an Apache HttpClient request for the supplied message. For CustomWebhookMessage use its requested HTTP method and URL components; copy custom headers, defaulting Content-Type to application/json when no headers are present. Other messages use POST with their URL. For requests that support an entity, encode the extracted message body as UTF-8. Return the response from the existing HTTP_CLIENT.

Available context and dependencies:
The project supplies BaseMessage.getUrl(); CustomWebhookMessage additionally supplies getScheme(), getHost(), getPort(), getPath(), getQueryParams(), getMethod(), getHeaderParams(). Existing helpers buildUri(...), constructHttpRequest(method), and extractBody(message), plus HTTP_CLIENT, are referenced but their implementations and complete type signatures are missing. Use them as existing project dependencies and list unresolved contracts. Apache HttpClient 4-style request/response and StringEntity types are used.
