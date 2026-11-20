@Component
@Named(AuthenticationResourceReference.RESOURCE_TYPE_ID)
@Singleton
public class AuthenticationResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
{
    @Inject
    private XWikiContextInitializer xWikiContextInitializer;

    @Inject
    private Execution execution;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Collections.singletonList(AuthenticationResourceReference.TYPE);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        AuthenticationResourceReference authenticationResourceReference = (AuthenticationResourceReference) reference;

        switch (authenticationResourceReference.getAction()) {
            case RETRIEVE_USERNAME:
                this.handleAction("forgotusername", authenticationResourceReference.getWikiReference());
                break;

            case RESET_PASSWORD:
                this.handleAction("resetpassword", authenticationResourceReference.getWikiReference());
                break;

            default:
                
        }

        
        
        chain.handleNext(reference);
    }

    private void handleAction(String templateName, WikiReference wikiReference) throws ResourceReferenceHandlerException
    {
        ExecutionContext executionContext = this.execution.getContext();
        if (executionContext == null) {
            executionContext = new ExecutionContext();
        }
        WikiReference currentWiki = null;
        XWikiContext context = null;
        try {
            context = this.xWikiContextInitializer.initialize(executionContext);
            currentWiki = context.getWikiReference();
            context.setWikiReference(wikiReference);
            
            
            Utils.parseTemplate(templateName, true, context);
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException(
                String.format("Error while rendering template [%s]: [%s].",
                templateName, ExceptionUtils.getRootCauseMessage(e)), e);
        } finally {
            if (currentWiki != null) {
                context.setWikiReference(currentWiki);
            }
        }
    }
}