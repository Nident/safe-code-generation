public EmbedFontInfo[] find(URI fontURI, InternalResourceResolver resourceResolver, FontCache fontCache) {
        URI embedUri = resourceResolver.resolveFromBase(fontURI);
        String embedStr = embedUri.toASCIIString();
        boolean useKerning = true;
        boolean useAdvanced = true;

        long fileLastModified = -1;
        if (fontCache != null) {
            fileLastModified = FontCache.getLastModified(fontURI);
            // firstly try and fetch it from cache before loading/parsing the font file
            if (fontCache.containsFont(embedStr)) {
                EmbedFontInfo[] fontInfos = fontCache.getFontInfos(embedStr, fileLastModified);
                if (fontInfos != null) {
                    return fontInfos;
                }
            // is this a previously failed parsed font?
            } else if (fontCache.isFailedFont(embedStr, fileLastModified)) {
                if (log.isDebugEnabled()) {
                    log.debug("Skipping font file that failed to load previously: " + embedUri);
                }
                return null;
            }
        }


        // try to determine triplet information from font file
        CustomFont customFont = null;
        if (fontURI.toASCIIString().toLowerCase().endsWith(".ttc")) {
            // Get a list of the TTC Font names
            List<String> ttcNames = null;
            InputStream in = null;
            try {
                in = resourceResolver.getResource(fontURI);
                TTFFile ttf = new TTFFile(false, false);
                FontFileReader reader = new FontFileReader(in);
                ttcNames = ttf.getTTCnames(reader);
                if (ttcNames == null) {
                    return null;
                }
            } catch (Exception e) {
                if (this.eventListener != null) {
                    this.eventListener.fontLoadingErrorAtAutoDetection(this,
                            fontURI.toASCIIString(), e);
                }
                return null;
            } finally {
                IOUtils.closeQuietly(in);
            }

            List<EmbedFontInfo> embedFontInfoList = new java.util.ArrayList<EmbedFontInfo>();

            // For each font name ...
            for (String fontName : ttcNames) {
                if (log.isDebugEnabled()) {
                    log.debug("Loading " + fontName);
                }
                try {
                    OFFontLoader ttfLoader = new OFFontLoader(fontURI, fontName, true,
                            EmbeddingMode.AUTO, EncodingMode.AUTO, useKerning, useAdvanced,
                            resourceResolver, false, false, true);
                    customFont = ttfLoader.getFont();
                    if (this.eventListener != null) {
                        customFont.setEventListener(this.eventListener);
                    }
                } catch (Exception e) {
                    if (fontCache != null) {
                        fontCache.registerFailedFont(embedUri.toASCIIString(), fileLastModified);
                    }
                    if (this.eventListener != null) {
                        this.eventListener.fontLoadingErrorAtAutoDetection(this,
                                embedUri.toASCIIString(), e);
                    }
                    continue;
                }
                EmbedFontInfo fi = getFontInfoFromCustomFont(fontURI, customFont, fontCache,
                        resourceResolver);
                if (fi != null) {
                    embedFontInfoList.add(fi);
                }
            }
            return embedFontInfoList.toArray(
                    new EmbedFontInfo[embedFontInfoList.size()]);
        } else {
            // The normal case
            try {
                FontUris fontUris = new FontUris(fontURI, null);
                customFont = FontLoader.loadFont(fontUris, null, true, EmbeddingMode.AUTO, EncodingMode.AUTO,
                        useKerning, useAdvanced, resourceResolver, false, false, true);
                if (this.eventListener != null) {
                    customFont.setEventListener(this.eventListener);
                }
            } catch (Exception e) {
                if (fontCache != null) {
                    fontCache.registerFailedFont(embedUri.toASCIIString(), fileLastModified);
                }
                if (this.eventListener != null) {
                    this.eventListener.fontLoadingErrorAtAutoDetection(this,
                            embedUri.toASCIIString(), e);
                }
                return null;
            }
            EmbedFontInfo fi = getFontInfoFromCustomFont(fontURI, customFont, fontCache, resourceResolver);
            if (fi != null) {
                return new EmbedFontInfo[] {fi};
            } else {
                return null;
            }
        }

    }