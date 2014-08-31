package com.p1ngu1n.snapshare.Util;
/**
 Commons.java created on 8/28/14.

 Copyright (C) 2014 P1nGu1n

 This file is part of Snapshare.

 Snapshare is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Snapshare is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 a gazillion times. If not, see <http://www.gnu.org/licenses/>.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.DisplayMetrics;

public class ImageUtils {
    private int targetWidth;
    private int targetHeight;

    /**
     * Initializes the ImageUtils class, sets the device resolution in specific.
     * @param context
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public ImageUtils(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        targetWidth = dm.widthPixels;
        targetHeight = dm.heightPixels;

        XposedUtils.log("Display metrics: " + targetWidth + " x " + targetHeight + " (w x h)");
        // DisplayMetrics' values depend on the phone's tilt, so we normalize them to Portrait mode
        if (targetWidth > targetHeight) {
            XposedUtils.log("Normalizing display metrics to Portrait mode.");
            targetWidth = dm.heightPixels;
            targetHeight = dm.widthPixels;
        }
    }

    /**
     * Rotate a bitmap
     * @param bitmap The bitmap to be rotated
     * @param degrees The number of the degrees the bitmap should be rotated
     * @return
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees);
        return transformBitmap(bitmap, matrix);
    }

    /**
     * Transform a bitmap.
     * @param bitmap The bitmap to be transformed
     * @param matrix The matrix containing the transformation to be applied
     * @return
     */
    public static Bitmap transformBitmap(Bitmap bitmap, Matrix matrix) {
        try {
            Bitmap transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return transformed;
        } catch (OutOfMemoryError e) {
            XposedUtils.log(new Exception("OutOfMemoryError, original bitmap returned", e));
            return bitmap;
        }
    }

    /**
     * Crop the bitmap to the right aspect ratio using the device resolution.
     * @param bitmap The bitmap to be cropped
     * @return The cropped bitmap
     */
    public Bitmap adjustmentMethodCrop(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int imageToDisplayRatio = (width * targetHeight) - (height * targetWidth);
        if (imageToDisplayRatio > 0) {
            // i.e., width/height > targetWidth/targetHeight, so have to crop from left and right:
            int newWidth = (targetWidth * height / targetHeight);
            XposedUtils.log("New width after cropping left and right: " + newWidth);
            bitmap = Bitmap.createBitmap(bitmap, (width - newWidth) / 2, 0, newWidth, height);
        } else if (imageToDisplayRatio < 0) {
            // i.e., width/height < targetWidth/targetHeight, so have to crop from top and bottom:
            int newHeight = (targetHeight * width / targetWidth);
            XposedUtils.log("New height after cropping top and bottom: " + newHeight);
            bitmap = Bitmap.createBitmap(bitmap, 0, (height - newHeight) / 2, width, newHeight);
        }

        /* If the image properly covers the Display rectangle, we mark it as a "large" image
         * and are going to scale it down. We make this distinction because we don't wanna
         * scale the image up if it is smaller than the Display rectangle. */
        boolean largeImage = ((width > targetWidth) & (height > targetHeight));
        if (largeImage) {
            XposedUtils.log("Large image, scaling down");
            bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
        }
        return bitmap;
    }

    /**
     * Creates a bitmap with the resolution of the device and the given bitmap centered. The bitmap is not resized.
     * @param bitmap
     * @return
     */
    public Bitmap adjustmentMethodNone(Bitmap bitmap) {
        float scale = 1;
        float xTrans = (targetWidth / 2) - (bitmap.getWidth() / 2);
        float yTrans = (targetHeight / 2) - (bitmap.getHeight() / 2);
        return adjustmentMethodScale(bitmap, scale, xTrans, yTrans);
    }

    /**
     * Creates a bitmap with the resolution of the device and the given bitmap scaled. The bitmap is not cropped.
     * @param bitmap
     * @return
     */
    public Bitmap adjustmentMethodScale(Bitmap bitmap) {
        float scale = targetWidth / (float) bitmap.getWidth();
        float xTrans = 0;
        float yTrans = (targetHeight / 2) - ((scale * bitmap.getHeight()) / 2);
        return adjustmentMethodScale(bitmap, scale, xTrans, yTrans);
    }

    /**
     * Creates a bitmap with the resolution of the device and puts the given bitmap using the scale, x- and y-transitions.
     * @param bitmap
     * @param scale
     * @param xTrans The x-transition
     * @param yTrans The y-transition
     * @return
     */
    public Bitmap adjustmentMethodScale(Bitmap bitmap, float scale, float xTrans, float yTrans) {
        // we are going to scale the image down and place a black background behind it
        Bitmap background = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        background.eraseColor(Color.BLACK);
        Canvas canvas = new Canvas(background);

        Matrix transform = new Matrix();
        transform.preScale(scale, scale);
        transform.postTranslate(xTrans, yTrans);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, transform, paint);
        bitmap.recycle();
        return background;
    }
}
