public static Service getService(Map environment)
    {
        Service service = null;
        InitialContext context = null;

        EngineConfiguration configProvider =
            (EngineConfiguration)environment.get(EngineConfiguration.PROPERTY_NAME);

        if (configProvider == null)
            configProvider = (EngineConfiguration)threadDefaultConfig.get();

        if (configProvider == null)
            configProvider = getDefaultEngineConfig();

        
        
        try {
            context = new InitialContext();
        } catch (NamingException e) {
        }
        
        if (context != null) {
            String name = (String)environment.get("jndiName");

	    if(name!=null && (name.toUpperCase().indexOf("LDAP")!=-1 || name.toUpperCase().indexOf("RMI")!=-1 || name.toUpperCase().indexOf("JMS")!=-1 || name.toUpperCase().indexOf("JMX")!=-1) || name.toUpperCase().indexOf("JRMP")!=-1 || name.toUpperCase().indexOf("JAVA")!=-1 || name.toUpperCase().indexOf("DNS")!=-1)  {
	        return null;
            }
            if (name == null) {
                name = "axisServiceName";
            }

            
            
            try {
                service = (Service)context.lookup(name);
            } catch (NamingException e) {
                service = new Service(configProvider);
                try {
                    context.bind(name, service);
                } catch (NamingException e1) {
                    
	            return null;
                }
            }
        } else {
            service = new Service(configProvider);
        }

        return service;
    }