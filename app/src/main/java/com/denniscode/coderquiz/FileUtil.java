package com.denniscode.coderquiz;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtil {

    public static boolean hasImageFileName(String[] columns) {
        String lastColumn = columns[columns.length - 1].trim();
        return lastColumn.endsWith(".jpg") || lastColumn.endsWith(".png") || lastColumn.endsWith(".jpeg");
    }

    public static String getImageFileName(String[] columns) {
        return columns[columns.length - 1].trim();
    }

    public static boolean isTrueOrFalseQuestion(String[] columns) {
        for (String column : columns) {
            if (column != null) {
                String trimmedColumn = column.trim().toLowerCase();
                if (trimmedColumn.equals("true") || trimmedColumn.equals("false") ||
                        trimmedColumn.equals("yes") || trimmedColumn.equals("no")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean imageExistsInZip(ZipFile zipFile, String imagePath) {
        return zipFile.getEntry(imagePath) != null;
    }

    public static byte[] getImageBlob(ZipFile zipFile, Map<String, byte[]> imageCache, String imagePath) {
        if (imageCache.containsKey(imagePath)) {
            return imageCache.get(imagePath); // Use cached image
        }

        try {
            ZipEntry imageEntry = zipFile.getEntry(imagePath);
            if (imageEntry == null) {
                return null;
            }

            InputStream inputStream = zipFile.getInputStream(imageEntry);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }

            byte[] imageBlob = baos.toByteArray();
            imageCache.put(imagePath, imageBlob); // Cache the image
            return imageBlob;
        } catch (Exception e) {
            return null;
        }
    }


}
