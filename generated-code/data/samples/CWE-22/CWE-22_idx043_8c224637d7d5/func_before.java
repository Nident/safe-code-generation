 
public final class JUtil {

   static Class<?> rootClass = ResourceRoot.class;

   static String fileSep;
   static String rootPath;

   static String osarch;

   static {
      fileSep = "/"; 
      rootPath = "org" + fileSep + "psics";
   }


   private JUtil() {
	   
   }
   

   public static void setResourceRoot(Class<?> cls)
   {
	   rootClass=cls;
   }
   public static String getRelativeResource(Object obj, String path) throws ContentError {
      return getRelativeResource(obj.getClass(), path);
   }


   public static String getRelativeResource(String s) throws ContentError {
      return getRelativeResource(rootClass, s);
   }

   public static String getRelativeResource(Class<?> cls, String path) throws ContentError {
      String sret = null;

      try {
        if (path.contains("..")) {
            path = (new File(path)).getCanonicalPath();
        }
        InputStream fis = cls.getResourceAsStream(path);
        sret = readInputStream(fis);

      } catch (Exception ex) {
         throw new ContentError("ResourceAccess - can't get resource " + path + " relative to " + cls + ": " + ex);
      }
      return sret;
   }






   public static void copyResource(Object obj, String fnm, File fdest) throws IOException, ContentError {
	   String s = getRelativeResource(obj, fnm);
	   FileUtil.writeStringToFile(s, new File(fdest, fnm));
   }


   public static String getXMLResource(String path) throws ContentError {
      String sp = null;
      if (path.endsWith(".xml") || path.indexOf(".") < 0) {
         E.warning("getXMLReousrce should have a dot path, not " + path);
         sp = path;
      } else {
     

         sp = path.replaceAll("\\.", fileSep) + ".xml";
      }
       return getResource(sp);
   }


   public static String getFileResource(String path, String fnm) throws ContentError {
      String sp = path.replaceAll("\\.", fileSep) + fileSep + fnm;
      return getResource(sp);
   }


   private static String getResource(String pathin) throws ContentError {
	   String path = pathin;
      String sret = null;

      try {
         if (path.startsWith(rootPath)) {
            path = path.substring(rootPath.length()+1, path.length());

         
            InputStream fis = rootClass.getResourceAsStream(path);
            sret = readInputStream(fis);

         } else {
            E.warning("reading foreign resource from class path?");
            InputStream fis = ClassLoader.getSystemResourceAsStream(path);
            sret = readInputStream(fis);
         }

      } catch (Exception ex) {
        throw new ContentError("ResourceAccess - cant get " + path + " " + ex);
        
      }
      return sret;
   }



   private static String readInputStream(InputStream fis)
         throws NullPointerException, IOException {
      String sret = null;

      InputStreamReader insr = new InputStreamReader(fis);
      BufferedReader fr = new BufferedReader(insr);

      StringBuffer sb = new StringBuffer();
      while (fr.ready()) {
         sb.append(fr.readLine());
         sb.append("\n");
      }
      fr.close();
      sret = sb.toString();

      return sret;
   }



   public static void copyBinaryResource(String respathin, File dest) throws FileNotFoundException, IOException {
      String respath = respathin;
	   if (dest.exists()) {
      
         return;
      }

     
 
         if (respath.startsWith(rootPath)) {
            respath = respath.substring(rootPath.length()+1,respath.length());
         }
         extractRelativeResource(rootClass, respath, dest);
   }



   public static void extractRelativeResource(Class<?> c, String path, File dest) throws
        FileNotFoundException, IOException {
	   InputStream in = c.getResourceAsStream(path);
 	   
       OutputStream out = new FileOutputStream(dest);
       byte[] buf = new byte[1024];
       int len = in.read(buf);
       while (true) {
          if (len > 0) {
        	  out.write(buf, 0, len);
        	  len = in.read(buf);
          } else {
        	  break;
          }
       }
       in.close();
       out.close();
   }


 
 
  
   public static String shortClassName(Object ov) {
      String cnm = ov.getClass().getName();
      cnm = cnm.substring(cnm.lastIndexOf(".") + 1, cnm.length());
      return cnm;
   }

 



   public static void unpackJar(File fjar, File fout) throws IOException {
    
      JarFile jf = new JarFile(fjar);
      Enumeration<JarEntry> en = jf.entries();

      while (en.hasMoreElements()) {
         JarEntry je = en.nextElement();
         java.io.File f = new File(fout,  je.getName());
         if (je.isDirectory()) {
            f.mkdirs();
            continue;

         } else {
            

            if (f.getPath().indexOf("META-INF") >= 0) {
               
            } else {
            f.getParentFile().mkdirs();
            java.io.InputStream is = jf.getInputStream(je);
            java.io.FileOutputStream fos = new FileOutputStream(f);

            
            while (is.available() > 0) {
               fos.write(is.read());
            }
            fos.close();
            is.close();
         }
         }
      }

    

       
   }



   public static String absPath(Class<?> base, String pckgname) {
	   String rcnm = base.getName();
	    rcnm = rcnm.substring(0, rcnm.lastIndexOf("."));
	    String ppath = rcnm.replace(".", "/");
	    String path = ppath + "/" + pckgname;

	    if (path.endsWith("/.")) {
	    	path = path.substring(0, path.length()-2);
	    }

	    return path;
   }



   public static String[] getResourceList(Class<?> base, String pckgname, String ext) {

	    String path = absPath(base, pckgname);

	   	ArrayList<String> als = new ArrayList<String>();
		try {
			ClassLoader cld = Thread.currentThread().getContextClassLoader();
			URL resource = cld.getResource(path);
			File dir = new File(resource.getFile());

			if (dir.exists()) {
				
				for (String fnm : dir.list()) {
					if (ext == null || fnm.endsWith(ext)) {
						als.add(fnm);
					}
				}

			} else {
				
				JarURLConnection conn = (JarURLConnection) resource.openConnection();
				String starts = conn.getEntryName();
				JarFile jfile = conn.getJarFile();
				Enumeration<JarEntry> e = jfile.entries();
				while (e.hasMoreElements()) {
					ZipEntry entry = e.nextElement();
					String entryname = entry.getName();
					if (entryname.startsWith(starts)
							&& (entryname.lastIndexOf('/') <= starts.length())) {
						String rnm = entryname.substring(starts.length()+1, entryname.length());
						if (rnm.length() > 1 && (ext == null || rnm.endsWith(ext))) {
							als.add(rnm);
						}
					}

				}

			}

		} catch (Exception ex) {
			E.error("cant list resources? " + ex);
		}
		return als.toArray(new String[als.size()]);
	}



   	public static void unpackPackage(Class<?> base, String pkgname, File dir) throws FileNotFoundException, IOException {
   		 
   		String[] sa = getResourceList(base, pkgname, null);
   		for (String s : sa) {
   			E.info("resource to unpack " + s);
   		}

   		for (String s : sa) {
   			File fdest = new File(dir, s);
   			extractRelativeResource(base, pkgname + "/" + s, fdest);
   		}
   	 
   	}







public static void showThreads() {

	   
    ThreadGroup root = Thread.currentThread().getThreadGroup();
    while (root.getParent() != null) {
        root = root.getParent();
    }

    
    visit(root, 0);

}

    
    public static void visit(ThreadGroup group, int level) {
        
        int numThreads = group.activeCount();
        Thread[] threads = new Thread[numThreads*2];
        numThreads = group.enumerate(threads, false);

        
        for (int i=0; i<numThreads; i++) {
            
            Thread thread = threads[i];
            E.info("Thread: " + thread.isDaemon() + " " + thread);
        }

        
        int numGroups = group.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[numGroups*2];
        numGroups = group.enumerate(groups, false);

        
        for (int i=0; i<numGroups; i++) {
            visit(groups[i], level+1);
        }
    }


	public static String getOSArchitecture() throws ContentError {
		if (osarch == null) {
			String osn = System.getProperty("os.name").toLowerCase();

			if (osn.startsWith("linux")) {
				osarch = "linux";
			} else if (osn.startsWith("mac")) {
				osarch = "mac";
			} else if (osn.startsWith("windows")) {
				osarch = "windows";
			} else {
				throw new ContentError("unrecognized os " + osn);
			}
		}
		return osarch;
	}
}

