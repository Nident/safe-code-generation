public Cron parse(final String expression) {
        Preconditions.checkNotNull(expression, "Expression must not be null");
        final String replaced = expression.replaceAll("\\s+", " ").trim();
        if (StringUtils.isEmpty(replaced)) {
            throw new IllegalArgumentException("Empty expression!");
        }

        if(expression.contains("||")) {
            List<Cron> crons = Arrays.stream(expression.split("\\|\\|")).map(this::parse).collect(Collectors.toList());
            return new CompositeCron(crons);
        }
        if(expression.contains("|")){
            List<String> crons = new ArrayList<>();
            int cronscount = Arrays.stream(expression.split("\\s+")).mapToInt(s->s.split("\\|").length).max().orElse(0);
            for(int j=0; j<cronscount; j++){
                StringBuilder builder = new StringBuilder();
                for(String s : expression.split("\\s+")){
                    if(s.contains("|")){
                        builder.append(String.format("%s ", s.split("\\|")[j]));
                    }else{
                        builder.append(String.format("%s ", s));
                    }
                }
                crons.add(builder.toString().trim());
            }
            return new CompositeCron(crons.stream().map(this::parse).collect(Collectors.toList()));
        }else{
            final String[] expressionParts = replaced.toUpperCase().split(" ");
            final int expressionLength = expressionParts.length;
            String fieldWithTrailingCommas = Arrays.stream(expressionParts).filter(x -> x.endsWith(",")).findAny().orElse(null);
            if(fieldWithTrailingCommas!=null){
                throw new IllegalArgumentException(String.format("Invalid field value! Trailing commas not permitted! '%s'", fieldWithTrailingCommas));
            }
            final List<CronParserField> fields = expressions.get(expressionLength);
            if (fields == null) {
                throw new IllegalArgumentException(
                        String.format("Cron expression contains %s parts but we expect one of %s", expressionLength, expressions.keySet()));
            }
            try {

                final int size = expressionParts.length;
                final List<CronField> results = new ArrayList<>(size + 1);
                for (int j = 0; j < size; j++) {
                    results.add(fields.get(j).parse(expressionParts[j]));
                }
                return new SingleCron(cronDefinition, results).validate();
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("Failed to parse '%s'. %s", expression, e.getMessage()), e);
            }
        }
    }