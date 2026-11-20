private Response doSASLAuthentication(SMTPSession session, String initialResponseString) {
        return session.getConfiguration().saslConfiguration()
            .map(oidcSASLConfiguration -> hooks.stream()
                .flatMap(hook -> Optional.ofNullable(executeHook(session, hook,
                    hook2 -> hook2.doSasl(session, oidcSASLConfiguration, initialResponseString))).stream())
                .filter(response -> !SMTPRetCode.AUTH_FAILED.equals(response.getRetCode()))
                .findFirst()
                .orElseGet(() -> failSasl(oidcSASLConfiguration, session)))
            .orElse(doUnknownAuth(AUTH_TYPE_OAUTHBEARER));
    }