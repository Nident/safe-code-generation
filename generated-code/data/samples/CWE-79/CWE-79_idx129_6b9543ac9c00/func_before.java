@GetMapping({"/questions/space/{space}", "/questions/space"})
	public String setSpace(@PathVariable(required = false) String space,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if ("all".equals(space) || utils.isAllSpaces(space)) {
			space = Post.ALL_MY_SPACES + ":" + utils.getLang(req).get("allspaces");
		} else {
			Sysprop spaceObj = pc.read(utils.getSpaceId(space));
			if (!StringUtils.isBlank(space) && spaceObj == null) {
				Profile authUser = utils.getAuthUser(req);
				if (authUser != null && utils.canAccessSpace(authUser, space)) {
					authUser.removeSpace(space);
					authUser.update();
				}
			}
			if (spaceObj != null) {
				space = spaceObj.getId().concat(Config.SEPARATOR).concat(spaceObj.getName());
			} else {
				space = Post.DEFAULT_SPACE;
			}
		}
		utils.storeSpaceIdInCookie(space, req, res);
		String backTo = req.getParameter("returnto");
		if (StringUtils.isBlank(backTo)) {
			return get(req.getParameter("sortby"), req, model);
		} else {
			return "redirect:" + (StringUtils.isBlank(backTo) ? QUESTIONSLINK : backTo);
		}
	}