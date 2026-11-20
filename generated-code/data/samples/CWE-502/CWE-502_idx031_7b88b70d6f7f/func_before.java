public class AgentRemoteInvokerServiceExporter extends HttpInvokerServiceExporter {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AgentRemoteInvokerServiceExporter.class);
    private static final Set<MethodSignature> KNOWN_METHODS_NEEDING_UUID_VALIDATION = Set.of(
            new MethodSignature("ping", AgentRuntimeInfo.class),
            new MethodSignature("getWork", AgentRuntimeInfo.class),
            new MethodSignature("reportCurrentStatus", AgentRuntimeInfo.class, JobIdentifier.class, JobState.class),
            new MethodSignature("reportCompleting", AgentRuntimeInfo.class, JobIdentifier.class, JobResult.class),
            new MethodSignature("reportCompleted", AgentRuntimeInfo.class, JobIdentifier.class, JobResult.class),
            new MethodSignature("isIgnored", AgentRuntimeInfo.class, JobIdentifier.class),
            new MethodSignature("getCookie", AgentRuntimeInfo.class)
    );

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            RemoteInvocation invocation = readRemoteInvocation(request);

            if (!authorized(request, response, invocation)) {
                return;
            }

            RemoteInvocationResult result = invokeAndCreateResult(invocation, getProxy());
            writeRemoteInvocationResult(request, response, result);
        } catch (ClassNotFoundException ex) {
            throw new NestedServletException("Class not found during deserialization", ex);
        }
    }

    
    private boolean authorized(HttpServletRequest request, HttpServletResponse response, RemoteInvocation invocation) throws IOException {
        final String uuid = request.getHeader("X-Agent-GUID"); 
        final MethodSignature current = new MethodSignature(invocation);

        LOG.debug(format("Checking authorization for agent [%s] on invocation: %s", uuid, invocation));

        if (KNOWN_METHODS_NEEDING_UUID_VALIDATION.contains(current)) {
            final String askingFor = AgentUUID.fromRuntimeInfo0(invocation.getArguments());

            if (!uuid.equals(askingFor)) {
                LOG.error(format("DENYING REQUEST: Agent [%s] is attempting a request on behalf of [%s]: %s", uuid, askingFor, invocation));
                reject(response, SC_FORBIDDEN, "Not allowing request on behalf of another agent");
                return false;
            }
        } else {
            LOG.error(format("DENYING REQUEST: Agent [%s] is requesting an unknown method invocation: %s", uuid, invocation));
            reject(response, SC_BAD_REQUEST, format("Unknown invocation: %s", invocation));
            return false;
        }

        LOG.debug(format("ALLOWING REQUEST: Agent [%s] is authorized to invoke: %s", uuid, invocation));
        return true;
    }

    
    private void reject(HttpServletResponse response, int statusCode, String msg) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("text/plain");
        final PrintWriter writer = response.getWriter();
        writer.println(msg);
        writer.flush();
        writer.close();
    }

    
    private static class AgentUUID {
        private static String fromRuntimeInfo0(Object[] args) {
            return ((AgentRuntimeInfo) args[0]).getIdentifier().getUuid();
        }
    }

    
    private static class MethodSignature {
        private final String name;
        private final Class<?>[] paramTypes;

        private MethodSignature(RemoteInvocation invocation) {
            this(invocation.getMethodName(), invocation.getParameterTypes());
        }

        private MethodSignature(String name, Class<?>... paramTypes) {
            this.name = name;
            this.paramTypes = paramTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodSignature that = (MethodSignature) o;
            return Objects.equals(name, that.name) && Arrays.equals(paramTypes, that.paramTypes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(name);
            result = 31 * result + Arrays.hashCode(paramTypes);
            return result;
        }
    }
}