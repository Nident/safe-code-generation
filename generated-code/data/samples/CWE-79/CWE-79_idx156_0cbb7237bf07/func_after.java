private static void addUsers(final Set<User> users, final TaskListener listener, @CheckForNull Run<?,?> run, final EnvVars env,
        final Set<InternetAddress> to, final Set<InternetAddress> cc, final Set<InternetAddress> bcc, final IDebug debug) {
        for (final User user : users) {
            if (EmailRecipientUtils.isExcludedRecipient(user, listener)) {
                debug.send("User %s is an excluded recipient.", user.getFullName());
            } else {
                final String userAddress = EmailRecipientUtils.getUserConfiguredEmail(user);
                if (userAddress != null) {
                    if (Jenkins.get().isUseSecurity()) {
                        try {
                            Authentication auth = user.impersonate();
                            if (run != null && !run.getACL().hasPermission(auth, Item.READ)) {
                                if (SEND_TO_USERS_WITHOUT_READ) {
                                    listener.getLogger().printf("Warning: user %s has no permission to view %s, but sending mail anyway%n", userAddress, run.getFullDisplayName());
                                } else {
                                    listener.getLogger().printf("Not sending mail to user %s with no permission to view %s", userAddress, run.getFullDisplayName());
                                    continue;
                                }
                            }
                        } catch (AuthenticationException x) {
                            
                            if (SEND_TO_UNKNOWN_USERS || ExtendedEmailPublisher.descriptor().isAllowUnregisteredEnabled() ) {
                                listener.getLogger().printf("Warning: %s is not a recognized user, but sending mail anyway%n", userAddress);
                            } else {
                                listener.getLogger().printf("Not sending mail to unregistered user %s because your SCM"
                                        + " claimed this was associated with a user ID ‘", userAddress);
                                try {
                                    listener.hyperlink('/' + user.getUrl(), user.getDisplayName());
                                } catch (IOException ignored) {
                                }
                                listener.getLogger().printf("' which your security realm does not recognize; you may need" +
                                        " changes in your SCM plugin%n");
                                continue;
                            }
                        }
                    }
                    debug.send("Adding %s with address %s", user.getFullName(), userAddress);
                    EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, userAddress, env, listener);
                } else {
                    listener.getLogger().println("Failed to send e-mail to "
                        + user.getFullName()
                        + " because no e-mail address is known, and no default e-mail domain is configured");
                }
            }
        }
    }