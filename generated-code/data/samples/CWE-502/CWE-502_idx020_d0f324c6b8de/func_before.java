@SuppressWarnings("lgtm[java/dereferenced-value-may-be-null]")
    public Map<String, ArrayList<Object>> buildImageParameters(SQLInsertRecognizer recognizer) {
        List<String> duplicateKeyUpdateColumns = recognizer.getDuplicateKeyUpdate();
        if (CollectionUtils.isNotEmpty(duplicateKeyUpdateColumns)) {
            getTableMeta().getAllIndexes().forEach((k, v) -> {
                if ("PRIMARY".equalsIgnoreCase(k)) {
                    for (ColumnMeta m : v.getValues()) {
                        if (duplicateKeyUpdateColumns.contains(m.getColumnName())) {
                            throw new ShouldNeverHappenException("update pk value is not supported!");
                        }
                    }
                }
            });
        }
        Map<String, ArrayList<Object>> imageParameterMap = new LowerCaseLinkHashMap<>();
        Map<Integer, ArrayList<Object>> parameters = ((PreparedStatementProxy) statementProxy).getParameters();
        //  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        List<String> insertParamsList = recognizer.getInsertParamsValue();
        List<String> insertColumns = recognizer.getInsertColumns();
        int paramsindex = 1;
        for (String insertParams : insertParamsList) {
            String[] insertParamsArray = insertParams.split(",");
            for (int i = 0; i < insertColumns.size(); i++) {
                String m = insertColumns.get(i);
                String params = insertParamsArray[i];
                ArrayList<Object> imageListTemp = imageParameterMap.computeIfAbsent(m, k -> new ArrayList<>());
                if ("?".equals(params.trim())) {
                    ArrayList<Object> objects = parameters.get(paramsindex);
                    imageListTemp.addAll(objects);
                    paramsindex++;
                } else {
                    // params is character string constant
                    if ((params.trim().startsWith("'") && params.trim().endsWith("'")) || params.trim().startsWith("\"") && params.trim().endsWith("\"")) {
                        params = params.trim();
                        params = params.substring(1, params.length() - 1);
                    }
                    imageListTemp.add(params);
                }
                imageParameterMap.put(m, imageListTemp);
            }
        }
        return imageParameterMap;
    }