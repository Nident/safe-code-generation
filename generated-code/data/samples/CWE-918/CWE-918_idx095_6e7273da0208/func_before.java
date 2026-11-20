@PreAuthorize("hasPermission(#studyId, 'CancerStudyId', T(org.cbioportal.utils.security.AccessLevel).READ)")
    @RequestMapping(value = "/studies/{studyId}/tags", method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Get the tags of a study")
    public ResponseEntity<Object> getTags(
        @ApiParam(required = true, value = "Study ID e.g. acc_tcga")
        @PathVariable String studyId) throws JsonParseException, JsonMappingException,
        IOException {

        Map<String,Object> map = new HashMap<String,Object>();
        ObjectMapper mapper = new ObjectMapper();
        CancerStudyTags cancerStudyTags = studyService.getTags(studyId);
        if (cancerStudyTags != null) { //If tags is null an empty map is returned
            map = mapper.readValue(cancerStudyTags.getTags(), Map.class);
        }

        return new ResponseEntity<>(map, HttpStatus.OK);
    }