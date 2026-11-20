public class Utils {

  public static ClassLoader getClassLoader() {
    
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      
      cl = Utils.class.getClassLoader();
      if (cl == null) {
        
        cl = Object.class.getClassLoader();
      }
    }
    return cl;
  }

  private static final ZoneId ZONE_GMT = ZoneId.of("GMT");

  public static String formatRFC1123DateTime(final long time) {
    return DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(time).atZone(ZONE_GMT));
  }

  public static long parseRFC1123DateTime(final String header) {
    try {
      return header == null || header.isEmpty() ? -1 :
        LocalDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli();
    } catch (DateTimeParseException ex) {
      return -1;
    }
  }

  public static String pathOffset(String path, RoutingContext context) {
    final Route route = context.currentRoute();

    
    if (route == null) {
      return path;
    }

    if (!route.isExactPath()) {
      String rest = context.pathParam("*");
      if (rest != null) {
        
        if (rest.length() > 0) {
          
          rest = HttpUtils.removeDots(rest.replace('\\', '/'));
          if (rest.charAt(0) == '/') {
            return rest;
          } else {
            return "/" + rest;
          }
        } else {
          return "/";
        }
      }
    }
    int prefixLen = 0;
    String mountPoint = context.mountPoint();
    if (mountPoint != null) {
      prefixLen = mountPoint.length();
      
      if (mountPoint.charAt(mountPoint.length() - 1) == '/') {
        prefixLen--;
      }
    }
    
    if (!route.isRegexPath()) {
      String routePath = route.getPath();
      if (routePath != null) {
        prefixLen += routePath.length();
        
        if (routePath.charAt(routePath.length() - 1) == '/') {
          prefixLen--;
        }
      }
    }
    return prefixLen != 0 ? path.substring(prefixLen) : path;
  }

  public static long secondsFactor(long millis) {
    return millis - (millis % 1000);
  }

  public static boolean isJsonContentType(String contentType) {
    return contentType.contains("application/json") || contentType.contains("+json");
  }

  public static boolean isXMLContentType(String contentType) {
    return contentType.contains("application/xml") || contentType.contains("text/xml") || contentType.contains("+xml");
  }

  public static void addToMapIfAbsent(MultiMap map, CharSequence key, CharSequence value) {
    if (!map.contains(key)) {
      map.set(key, value);
    }
  }

  public static void appendToMapIfAbsent(MultiMap map, CharSequence key, CharSequence sep, CharSequence value) {
    if (!map.contains(key)) {
      map.set(key, value);
    } else {
      String existing = map.get(key);
      map.set(key, existing + sep + value);
    }
  }

  
  private static final Pattern CACHE_CONTROL_NO_CACHE_REGEXP = Pattern.compile("(?:^|,)\\s*?no-cache\\s*?(?:,|$)");

  public static boolean fresh(RoutingContext ctx) {
    return fresh(ctx, -1);
  }

  public static boolean fresh(RoutingContext ctx, long lastModified) {

    final HttpServerRequest req = ctx.request();
    final HttpServerResponse res = ctx.response();


    
    final String modifiedSince = req.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
    final String noneMatch = req.getHeader(HttpHeaders.IF_NONE_MATCH);

    
    if (modifiedSince == null && noneMatch == null) {
      return false;
    }

    
    
    
    final String cacheControl = req.getHeader(HttpHeaders.CACHE_CONTROL);
    if (cacheControl != null && CACHE_CONTROL_NO_CACHE_REGEXP.matcher(cacheControl).find()) {
      return false;
    }

    
    if (noneMatch != null && !"*".equals(noneMatch)) {
      final String etag = res.headers().get(HttpHeaders.ETAG);

      if (etag == null) {
        return false;
      }

      boolean etagStale = true;

      
      int end = 0;
      int start = 0;

      loop:
      for (int i = 0; i < noneMatch.length(); i++) {
        switch (noneMatch.charAt(i)) {
          case ' ':
            if (start == end) {
              start = end = i + 1;
            }
            break;
          case ',':
            String match = noneMatch.substring(start, end);
            if (match.equals(etag) || match.equals("W/" + etag) || ("W/" + match).equals(etag)) {
              etagStale = false;
              break loop;
            }
            start = end = i + 1;
            break;
          default:
            end = i + 1;
            break;
        }
      }

      if (etagStale) {
        
        String match = noneMatch.substring(start, end);
        if (!match.equals(etag) && !match.equals("W/" + etag) && !("W/" + match).equals(etag)) {
          return false;
        }
      }
    }

    
    if (modifiedSince != null) {
      if (lastModified == -1) {
        
        lastModified = parseRFC1123DateTime(res.headers().get(HttpHeaders.LAST_MODIFIED));
      }

      boolean modifiedStale = lastModified == -1 || !(lastModified <= parseRFC1123DateTime(modifiedSince));

      return !modifiedStale;
    }

    return true;
  }

  public static boolean canUpgradeToWebsocket(HttpServerRequest req) {
    
    
    
    final MultiMap headers = req.headers();
    if (headers.contains(HttpHeaders.CONNECTION)) {
      for (String connection : headers.getAll(HttpHeaders.CONNECTION)) {
        if (connection.toLowerCase().contains(HttpHeaders.UPGRADE)) {
          if (headers.contains(HttpHeaders.UPGRADE)) {
            for (String upgrade : headers.getAll(HttpHeaders.UPGRADE)) {
              if (upgrade.toLowerCase().contains(HttpHeaders.WEBSOCKET)) {
                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }
}