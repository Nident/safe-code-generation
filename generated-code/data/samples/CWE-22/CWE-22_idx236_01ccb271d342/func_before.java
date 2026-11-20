@SuppressWarnings("serial")
public class WellKnownServlet extends HttpServlet
{
	private static final Logger log = Logger
			.getLogger(HttpServlet.class.getName());

	
	public WellKnownServlet()
	{
		super();
	}

	
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException
	{
        
        String uri = request.getRequestURI().replace("/.", "/");

        if (uri.toLowerCase().contains(".json"))
        {
        	response.setContentType("application/json");
        }

        
        try (InputStream in = getServletContext().getResourceAsStream(uri))
        {
            if (in == null)
            {
            	response.sendError(404);
                return;
            }
            
            byte[] buffer = new byte[8192];
            int count;

            while ((count = in.read(buffer)) > 0)
            {
            	response.getOutputStream().write(buffer, 0, count);
            }
            
            response.getOutputStream().flush();
            response.getOutputStream().close();
        }
    }
}