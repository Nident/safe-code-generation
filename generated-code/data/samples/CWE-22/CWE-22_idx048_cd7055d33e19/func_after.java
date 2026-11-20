public class WhiteLabelingServlet extends HttpServlet {
	protected Logger logger = LoggerFactory.getLogger(getClass());
	private static final long serialVersionUID = -4573847543526921509L;
	private Map<Pattern, String> wlMap;
	private String resDir;
	private String themesByDomain;
	private String defaultTheme = "default";
	private String applyCommonByDomain;
	private Pattern themePattern = Pattern.compile("^[a-zA-Z0-9]+$"); 

	@Override
	public void init(ServletConfig config) {
		WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
		themesByDomain = checkNotNull((String) ctxt.getBean("wlThemesByDomain"), "Missing wlThemesByDomain spring bean!");
		defaultTheme = checkNotNull((String) ctxt.getBean("wlDefaultTheme"), "Missing wlDefaultTheme spring bean!");
		applyCommonByDomain = (String) ctxt.getBean("wlApplyCommonByDomain");
		
		resDir = checkNotNull((String) ctxt.getBean("wlResDir"), "Missing wlResDir spring bean!");
		wlMap = new HashMap<Pattern, String>();
		String[] tds = themesByDomain.split("\\|");
		if (tds != null) {
			for (String td : tds) {
				String[] toks = td.split(":");
				if (toks.length != 2) {
					logger.error("Bad domain theme config! should be pattern:theme: " +td);
				}
				wlMap.put(Pattern.compile(toks[0]), toks[1]);
			}
		}
	}

	@Override
	protected void service(HttpServletRequest request, 
			HttpServletResponse response) throws IOException, ServletException {
		String c = (String) request.getParameter("c"); 
		String j = (String) request.getParameter("j"); 
		String a = (String) request.getParameter("a"); 
		String i = (String) request.getParameter("i"); 
		String h = (String) request.getParameter("h"); 
		String t = (String) request.getParameter("t"); 
		String s = request.getServerName();

		logger.info("ServerName={}", s);
		
		if (isNullOrEmpty(t)) {
			for (Pattern p : wlMap.keySet()) {
				Matcher matcher = p.matcher(s);
				if (matcher.find()) {
					t = wlMap.get(p);
					break;
				}
			}
		}
		if (t == null) {
			t = defaultTheme;
		}
		
		
		if (!themePattern.matcher(t).find()) {
			logger.warn("requested theme name is not valid! theme={}", t);
			return;
		}
			
		
		if (!new File(resDir, "theme/" +t).exists()) {
			logger.warn("requested theme does not exists! theme={}", t);
			return;
		}

		File comFile = null;
		File file = null;
		if (j != null) {
			t += "/j.js";
			comFile = new File(resDir, "theme/common/j.js");
			file = new File(resDir, "theme/" +t);
			response.setContentType("text/javascript");
		} else if (c != null) {
			t += "/c.css";
			comFile = new File(resDir, "theme/common/c.css");
			file = new File(resDir, "theme/" +t);
			response.setContentType("text/css");
		} else if (h != null) {
			t += "/" +extractFilename(h);
			file = new File(resDir, "theme/" +t);
			if (h.endsWith(".css")) {
				response.setContentType("text/css");
			} else if (h.endsWith(".js")) {
				response.setContentType("text/javascript");
			} else  if (h.endsWith(".html")) {
				response.setContentType("text/html");
			}
		} else if (a != null) {
			a = extractFilename(a);
			String[] toks = a.split("\\.");
			if (toks.length == 0) {
				return;
			}
			file = new File(resDir, "avatar/" +a);
			response.setContentType("image/" +toks[toks.length -1]);
		} else if (i != null) {
			i = extractFilename(i);
			String[] toks = i.split("\\.");
			if (toks.length == 0) {
				return;
			}
			t += "/" +i;
			file = new File(resDir, "theme/" +t);
			response.setContentType("image/" +toks[toks.length -1]);
		} else {
			
		}

		if (comFile != null && comFile.exists()) {
			boolean applyCommon = true; 
			if (!isNullOrEmpty(applyCommonByDomain)) {
				
				applyCommon = false;
				String[] hosts = applyCommonByDomain.split(",");
				for (String host : hosts) {
					for (Pattern p : wlMap.keySet()) {
						Matcher matcher = p.matcher(host);
						if (matcher.find()) {
							applyCommon = true;
							break;
						}
					}
				}
			}

			if (applyCommon) {
				if (comFile.exists()) {
					FileInputStream fis = new FileInputStream(comFile);
					IOUtils.copy(fis, response.getOutputStream());
					Closeables.close(fis, false);
				}
			}
		}

		if (file != null && file.exists()) {
			FileInputStream fis = new FileInputStream(file);
			IOUtils.copy(fis, response.getOutputStream());
			Closeables.close(fis, false);
		}
	}
	private String extractFilename(String name) {
		if (Strings.isNullOrEmpty(name)) {
			return null;
		}
		
		name = new File(name).getName();
		
		int pos = name.indexOf("?");
		if (pos > -1) {
			name = name.substring(0, pos);
		}
		return name;
	}
}