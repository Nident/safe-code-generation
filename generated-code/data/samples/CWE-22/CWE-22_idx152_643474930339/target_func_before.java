public static void unzip(File input, File destinationDir) throws IOException {
		try (ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(new FileInputStream(input)))) {
			ZipEntry entry;
			while ((entry = zipInput.getNextEntry()) != null) {
				File dest = new File(destinationDir, entry.getName());
				if (entry.isDirectory()) {
					FileMisc.mkdirs(dest);
				} else {
					FileMisc.mkdirs(dest.getParentFile());
					try (OutputStream output = new BufferedOutputStream(new FileOutputStream(dest))) {
						copy(zipInput, output);
					}
				}
			}
		}
	}