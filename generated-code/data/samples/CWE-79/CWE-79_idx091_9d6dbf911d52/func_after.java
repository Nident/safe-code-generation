@Override
    public String execute( final Context context, final Map< String, String > params ) throws PluginException {
        final String formatString = params.get( "format" );

        try {
            final SimpleDateFormat fmt;
            if( formatString != null ) {
                fmt = new SimpleDateFormat( formatString );
            } else {
                fmt = Preferences.getDateFormat( context, TimeFormat.DATETIME );
            }

            final Date d = new Date();  // Now.

            return TextUtil.replaceEntities( fmt.format( d ) );

        } catch( final IllegalArgumentException e ) {
            final ResourceBundle rb = Preferences.getBundle( context, Plugin.CORE_PLUGINS_RESOURCEBUNDLE );
            throw new PluginException( rb.getString( "currenttimeplugin.badformat" ) + e.getMessage() );
        }
    }