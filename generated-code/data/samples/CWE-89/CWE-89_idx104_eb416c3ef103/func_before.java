public void setAuth(HashTree tree, MsAuthManager msAuthManager, HTTPSamplerProxy samplerProxy) {
        try {
            AuthManager authManager = new AuthManager();
            authManager.setEnabled(true);
            authManager.setName(StringUtils.isNotEmpty(this.getName()) ? this.getName() : "AuthManager");
            authManager.setProperty(TestElement.TEST_CLASS, AuthManager.class.getName());
            authManager.setProperty(TestElement.GUI_CLASS, SaveService.aliasToClass("AuthPanel"));
            Authorization auth = new Authorization();
            auth.setURL(samplerProxy.getUrl().toString());
            auth.setDomain(samplerProxy.getDomain());
            auth.setUser(msAuthManager.getUsername());
            auth.setPass(msAuthManager.getPassword());
            auth.setMechanism(AuthManager.Mechanism.DIGEST);
            authManager.addAuth(auth);
            tree.add(authManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }