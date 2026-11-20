public final class ZipUtils {

    
    
    public static void unzipFilesToPath(String jarPath, String destinationDir) throws IOException {
        File file = new File(jarPath);
        try (JarFile jar = new JarFile(file)) {

            
            
            

            
            for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); ) {
                JarEntry entry = enums.nextElement();

                String fileName = destinationDir + File.separator + entry.getName();
                File f = new File(fileName);

                if (!f.getCanonicalPath().startsWith(destinationDir)) {
                    System.out.println("Zip Slip exploit detected. Skipping entry " + entry.getName());
                    continue;
                }

                File parent = f.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }

                if (!fileName.endsWith("/")) {
                    try (InputStream is = jar.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(f)) {
                        
                        while (is.available() > 0) {
                            fos.write(is.read());
                        }
                    }
                }
            }
        }
    }

    public static void zipFile(File inputFile, File outputZip) {
        byte[] buffer = new byte[1024];

        try (FileOutputStream fos = new FileOutputStream(outputZip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            ZipEntry ze = new ZipEntry(inputFile.getName());
            zos.putNextEntry(ze);
            try (FileInputStream in = new FileInputStream(inputFile)) {
                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void zipFolder(String srcFolder, String destZipFile, String ignore) throws Exception {
        try (FileOutputStream fileWriter = new FileOutputStream(destZipFile);
             ZipOutputStream zip = new ZipOutputStream(fileWriter)) {
            addFolderToZip("", srcFolder, zip, ignore);
            zip.flush();
        }
    }

    public static void zipFolderAPKTool(String srcFolder, String destZipFile) throws Exception {
        try (FileOutputStream fileWriter = new FileOutputStream(destZipFile);
             ZipOutputStream zip = new ZipOutputStream(fileWriter)) {
            addFolderToZipAPKTool("", srcFolder, zip);
            zip.flush();
        }
    }

    public static void addFileToZip(String path, String srcFile, ZipOutputStream zip, String ignore)
            throws Exception {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip, ignore);
        } else {
            byte[] buf = new byte[1024];
            int len;
            try (FileInputStream in = new FileInputStream(srcFile)) {
                ZipEntry entry;
                if (ignore == null)
                    entry = new ZipEntry(path + "/" + folder.getName());
                else
                    entry = new ZipEntry(path.replace(ignore, "BCV_Krakatau") + "/" + folder.getName());
                zip.putNextEntry(entry);
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            }
        }
    }

    public static void addFileToZipAPKTool(String path, String srcFile, ZipOutputStream zip) throws Exception {
        File folder = new File(srcFile);

        String check = path.toLowerCase();
        
        
        
        if (check.startsWith("decoded original") || check.startsWith("decoded smali") || check.startsWith("decoded "
                + "apktool.yml"))
            return;

        
        

        if (folder.isDirectory()) {
            addFolderToZipAPKTool(path, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            try (FileInputStream in = new FileInputStream(srcFile)) {
                ZipEntry entry;

                entry = new ZipEntry(path + "/" + folder.getName());
                zip.putNextEntry(entry);

                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            }
        }
    }

    public static void addFolderToZip(String path, String srcFolder, ZipOutputStream zip, String ignore)
            throws Exception {
        File folder = new File(srcFolder);

        for (String fileName : Objects.requireNonNull(folder.list())) {
            if (path.isEmpty()) {
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip, ignore);
            } else {
                addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip, ignore);
            }
        }
    }

    public static void addFolderToZipAPKTool(String path, String srcFolder, ZipOutputStream zip) throws Exception {
        File folder = new File(srcFolder);

        for (String fileName : Objects.requireNonNull(folder.list())) {
            if (path.isEmpty()) {
                addFileToZipAPKTool(folder.getName(), srcFolder + "/" + fileName, zip);
            } else {
                addFileToZipAPKTool(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
            }
        }
    }
}