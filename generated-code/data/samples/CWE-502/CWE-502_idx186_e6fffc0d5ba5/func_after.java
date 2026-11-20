public static void shareText(@NonNull final Context context,
                                 @NonNull final String title,
                                 final String content,
                                 final String imagePreviewUrl) {
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        if (!TextUtils.isEmpty(title)) {
            shareIntent.putExtra(Intent.EXTRA_TITLE, title);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        }

        /* TODO: add the image of the content to Android share sheet with setClipData after
            generating a content URI of this image, then use ClipData.newUri(the content resolver,
            null, the content URI) and set the ClipData to the share intent with
            shareIntent.setClipData(generated ClipData).
        if (!imagePreviewUrl.isEmpty()) {
            //shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }*/

        openAppChooser(context, shareIntent, false);
    }