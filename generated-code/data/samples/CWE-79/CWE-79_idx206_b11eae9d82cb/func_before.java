@Override
    public Document clean(Reader originalHtmlContent, HTMLCleanerConfiguration configuration)
    {
        Document result;

        
        
        
        CleanerProperties cleanerProperties = getDefaultCleanerProperties(configuration);
        HtmlCleaner cleaner;
        if (isHTML5(configuration)) {
            
            cleaner = new HtmlCleaner(this.html5TagInfoProvider, cleanerProperties);
        }  else {
            cleaner = new HtmlCleaner(cleanerProperties);
        }

        TagNode cleanedNode;
        try {
            cleanedNode = cleaner.clean(originalHtmlContent);
        } catch (Exception e) {
            
            
            throw new RuntimeException("Unhandled error when cleaning HTML", e);
        }

        try {
            
            
            
            

            if (isHTML5(configuration)) {
                cleanedNode.setDocType(new DoctypeToken(HTMLConstants.TAG_HTML, null, null, null));
            } else {
                cleanedNode.setDocType(
                    new DoctypeToken(HTMLConstants.TAG_HTML, "PUBLIC", "-
                        "http:
            }
            result =
                new XWikiDOMSerializer(cleanerProperties).createDOM(getAvailableDocumentBuilder(), cleanedNode);
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException("Error while serializing TagNode into w3c dom.", ex);
        }

        
        for (HTMLFilter filter : configuration.getFilters()) {
            filter.filter(result, configuration.getParameters());
        }

        return result;
    }

    @Override
    public HTMLCleanerConfiguration getDefaultConfiguration()
    {
        HTMLCleanerConfiguration configuration = new DefaultHTMLCleanerConfiguration();
        configuration.setFilters(Arrays.asList(
            this.controlFilter,
            this.bodyFilter,
            this.listItemFilter,
            this.listFilter,
            this.fontFilter,
            this.attributeFilter,
            this.linkFilter));
        return configuration;
    }

    
    private CleanerProperties getDefaultCleanerProperties(HTMLCleanerConfiguration configuration)
    {
        CleanerProperties defaultProperties = new CleanerProperties();
        defaultProperties.setOmitUnknownTags(true);

        
        
        
        defaultProperties.setUseEmptyElementTags(false);

        
        defaultProperties.setUseCdataForScriptAndStyle(true);

        
        defaultProperties.setIgnoreQuestAndExclam(true);

        
        defaultProperties.setOmitCdataOutsideScriptAndStyle(true);

        
        String param = configuration.getParameters().get(HTMLCleanerConfiguration.NAMESPACES_AWARE);
        boolean namespacesAware = (param == null) || Boolean.parseBoolean(param);
        defaultProperties.setNamespacesAware(namespacesAware);

        
        defaultProperties.setCleanerTransformations(getDefaultCleanerTransformations(configuration));

        
        defaultProperties.setTranslateSpecialEntities(false);

        
        
        param = configuration.getParameters().get(HTMLCleanerConfiguration.USE_CHARACTER_REFERENCES);
        boolean useCharacterReferences = (param != null) && Boolean.parseBoolean(param);
        defaultProperties.setTransResCharsToNCR(useCharacterReferences);

        
        defaultProperties.setHtmlVersion(getHTMLVersion(configuration));

        
        
        
        defaultProperties.setTrimAttributeValues(false);

        
        defaultProperties.setRecognizeUnicodeChars(false);

        param = configuration.getParameters().get(HTMLCleanerConfiguration.TRANSLATE_SPECIAL_ENTITIES);
        boolean translateSpecialEntities = (param != null) && Boolean.parseBoolean(param);
        defaultProperties.setTranslateSpecialEntities(translateSpecialEntities);

        defaultProperties.setDeserializeEntities(true);

        return defaultProperties;
    }

    
    private TrimAttributeCleanerTransformations getDefaultCleanerTransformations(HTMLCleanerConfiguration configuration)
    {
        TrimAttributeCleanerTransformations defaultTransformations = new TrimAttributeCleanerTransformations();

        TagTransformation tt;

        
        if (!isHTML5(configuration)) {
            
            tt = new TagTransformation(HTMLConstants.TAG_B, HTMLConstants.TAG_STRONG, false);
            defaultTransformations.addTransformation(tt);

            tt = new TagTransformation(HTMLConstants.TAG_I, HTMLConstants.TAG_EM, false);
            defaultTransformations.addTransformation(tt);

            tt = new TagTransformation(HTMLConstants.TAG_U, HTMLConstants.TAG_INS, false);
            defaultTransformations.addTransformation(tt);

            tt = new TagTransformation(HTMLConstants.TAG_S, HTMLConstants.TAG_DEL, false);
            defaultTransformations.addTransformation(tt);
        }

        tt = new TagTransformation(HTMLConstants.TAG_STRIKE, HTMLConstants.TAG_DEL, false);
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.TAG_CENTER, HTMLConstants.TAG_P, false);
        tt.addAttributeTransformation(HTMLConstants.ATTRIBUTE_STYLE, "text-align:center");
        defaultTransformations.addTransformation(tt);

        if (isHTML5(configuration)) {
            
            defaultTransformations.addTransformation(new FontTagTransformation());

            
            tt = new TrimAttributeTagTransformation(HTMLConstants.TAG_TT, HTMLConstants.TAG_SPAN);
            tt.addAttributeTransformation(HTMLConstants.ATTRIBUTE_CLASS, "${class} monospace");
            defaultTransformations.addTransformation(tt);
        }

        String restricted = configuration.getParameters().get(HTMLCleanerConfiguration.RESTRICTED);
        if ("true".equalsIgnoreCase(restricted)) {

            tt = new TagTransformation(HTMLConstants.TAG_SCRIPT, HTMLConstants.TAG_PRE, false);
            defaultTransformations.addTransformation(tt);

            tt = new TagTransformation(HTMLConstants.TAG_STYLE, HTMLConstants.TAG_PRE, false);
            defaultTransformations.addTransformation(tt);
        }

        return defaultTransformations;
    }

    
    private boolean isHTML5(HTMLCleanerConfiguration configuration)
    {
        return getHTMLVersion(configuration) == 5;
    }

    
    private int getHTMLVersion(HTMLCleanerConfiguration configuration)
    {
        String param = configuration.getParameters().get(HTMLCleanerConfiguration.HTML_VERSION);
        int htmlVersion = 4;
        if ("5".equals(param)) {
            htmlVersion = 5;
        }
        return htmlVersion;
    }
}