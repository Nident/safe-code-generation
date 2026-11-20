@Component
@Named(SecureHTMLElementSanitizer.HINT)
@Singleton
@Unstable
public class SecureHTMLElementSanitizer implements HTMLElementSanitizer, Initializable
{
    
    public static final String HINT = "secure";

    static final Pattern IS_SCRIPT_OR_DATA = Pattern.compile("^(?:\\w+script|data):", Pattern.CASE_INSENSITIVE);

    static final Pattern ATTR_WHITESPACE =
        Pattern.compile("[\\u0000-\\u0020\\u00A0\\u1680\\u180E\\u2000-\\u2029\\u205F\\u3000]");

    static final Pattern DATA_ATTR = Pattern.compile("^data-[\\-\\w.\\u00B7-\\uFFFF]");

    static final Pattern ARIA_ATTR = Pattern.compile("^aria-[\\-\\w]+$");

    static final Pattern IS_ALLOWED_URI = Pattern.compile("^(?:(?:f|ht)tps?|mailto|tel|callto|cid|xmpp):",
        Pattern.CASE_INSENSITIVE);

    static final Pattern IS_NO_URI = Pattern.compile("^(?:[^a-z]|[a-z+.\\-]+(?:[^a-z+.\\-:]|$))",
        Pattern.CASE_INSENSITIVE);

    @Inject
    private HTMLElementSanitizerConfiguration htmlElementSanitizerConfiguration;

    @Inject
    private HTMLDefinitions htmlDefinitions;

    @Inject
    private SVGDefinitions svgDefinitions;

    @Inject
    private MathMLDefinitions mathMLDefinitions;

    
    private final Set<String> extraAllowedTags;

    
    private final Set<String> extraAllowedAttributes;

    
    private final Set<String> xmlAttributes;

    
    private final Set<String> dataUriTags;

    
    private final Set<String> uriSafeAttributes;

    private final Set<String> forbidTags;

    private final Set<String> forbidAttributes;

    private boolean allowUnknownProtocols;

    private Pattern allowedUriPattern;

    
    public SecureHTMLElementSanitizer()
    {
        this.dataUriTags = new HashSet<>(Arrays.asList("audio", "video", "img", "source", "image", "track"));

        this.uriSafeAttributes = new HashSet<>(
            Arrays.asList("alt", "class", "for", "id", "label", "name", "pattern", "placeholder", "role", "summary",
                "title", "value", "style", "xmlns"));

        this.xmlAttributes =
            new HashSet<>(Arrays.asList("xlink:href", "xml:id", "xlink:title", "xml:space", "xmlns:xlink"));

        this.extraAllowedTags = new HashSet<>();

        this.extraAllowedAttributes = new HashSet<>();

        this.forbidTags = new HashSet<>();

        this.forbidAttributes = new HashSet<>();

        this.allowedUriPattern = IS_ALLOWED_URI;
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.extraAllowedTags.addAll(this.htmlElementSanitizerConfiguration.getExtraAllowedTags());
        this.extraAllowedAttributes.addAll(this.htmlElementSanitizerConfiguration.getExtraAllowedAttributes());
        this.uriSafeAttributes.addAll(this.htmlElementSanitizerConfiguration.getExtraUriSafeAttributes());
        this.dataUriTags.addAll(this.htmlElementSanitizerConfiguration.getExtraDataUriTags());
        this.allowUnknownProtocols = this.htmlElementSanitizerConfiguration.isAllowUnknownProtocols();
        this.forbidTags.addAll(this.htmlElementSanitizerConfiguration.getForbidTags());
        this.forbidAttributes.addAll(this.htmlElementSanitizerConfiguration.getForbidAttributes());
        String configuredRegexp = this.htmlElementSanitizerConfiguration.getAllowedUriRegexp();
        if (StringUtils.isNotBlank(configuredRegexp)) {
            this.allowedUriPattern = Pattern.compile(configuredRegexp, Pattern.CASE_INSENSITIVE);
        }
    }

    @Override
    public boolean isElementAllowed(String elementName)
    {
        return !this.forbidTags.contains(elementName)
            && (this.extraAllowedTags.contains(elementName) || isElementSafe(elementName));
    }

    private boolean isElementSafe(String elementName)
    {
        return this.htmlDefinitions.isSafeTag(elementName) || this.svgDefinitions.isSafeTag(elementName)
            || this.mathMLDefinitions.isSafeTag(elementName);
    }

    @Override
    public boolean isAttributeAllowed(String elementName, String attributeName, String attributeValue)
    {
        boolean result = false;

        String lowerElement = elementName.toLowerCase();
        String lowerAttribute = attributeName.toLowerCase();

        if ((DATA_ATTR.matcher(lowerAttribute).find() || ARIA_ATTR.matcher(lowerAttribute).find())
            && !this.forbidAttributes.contains(lowerAttribute))
        {
            result = true;
        } else if (isAttributeAllowed(lowerAttribute) && !this.forbidAttributes.contains(lowerAttribute)) {
            result = isAllowedValue(lowerElement, lowerAttribute, attributeValue);
        }

        return result;
    }

    private boolean isAllowedValue(String lowercaseElementName, String lowercaseAttributeName, String attributeValue)
    {
        
        boolean result = StringUtils.isBlank(attributeValue);
        if (!result) {
            String valueNoWhitespace = ATTR_WHITESPACE.matcher(attributeValue).replaceAll("");
            result = this.uriSafeAttributes.contains(lowercaseAttributeName);
            result = result || IS_NO_URI.matcher(valueNoWhitespace).find();
            result = result || this.allowedUriPattern.matcher(valueNoWhitespace).find();
            result = result || isAllowedDataValue(lowercaseElementName, lowercaseAttributeName, attributeValue);
            result = result || (this.allowUnknownProtocols && !isScriptOrData(attributeValue));
        }
        return result;
    }

    private boolean isAttributeAllowed(String attributeName)
    {
        boolean result = this.extraAllowedAttributes.contains(attributeName);
        result = result || this.htmlDefinitions.isAllowedAttribute(attributeName);
        result = result || this.svgDefinitions.isAllowedAttribute(attributeName);
        result = result || this.mathMLDefinitions.isAllowedAttribute(attributeName);
        result = result || this.xmlAttributes.contains(attributeName);
        return result;
    }

    private boolean isScriptOrData(String attributeValue)
    {
        return IS_SCRIPT_OR_DATA.matcher(ATTR_WHITESPACE.matcher(attributeValue).replaceAll("")).find();
    }

    private boolean isAllowedDataValue(String elementName, String attributeName, String attributeValue)
    {
        boolean attributeAllowsData = "src".equals(attributeName) || "xlink:href".equals(attributeName)
            || "href".equals(attributeName);
        return attributeAllowsData && !"script".equals(elementName) && attributeValue.startsWith("data:")
            && this.dataUriTags.contains(elementName);
    }
}