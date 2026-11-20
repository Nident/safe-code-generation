private Map<String, byte[]> scanDirectory(File f, boolean recursive) throws IOException {
    Map<String, byte[]> res = new HashMap<>();
    for (File ff : f.listFiles()) {
      if (ff.isDirectory() && recursive) {
        res.putAll(scanDirectory(ff, true));
      } else if (!isIgnoreFile(ff)) {
        Manager.FhirFormat fmt = ResourceChecker.checkIsResource(getContext(), isDebug(), ff.getAbsolutePath());
        if (fmt != null) {
          res.put(Utilities.changeFileExt(ff.getName(), "." + fmt.getExtension()), TextFile.fileToBytes(ff.getAbsolutePath()));
        }
      }
    }
    return res;
  }