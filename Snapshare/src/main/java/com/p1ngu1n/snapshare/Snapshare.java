package com.p1ngu1n.snapshare;

/**
 Snapshare.java created on 6/26/13.

 Copyright (C) 2013 Sebastian Stammler <stammler@cantab.net>

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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.XModuleResources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.webkit.URLUtil;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.util.Matrix;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.newInstance;

public class Snapshare implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    /** Snapchat's version */
    private static int SNAPCHAT_VERSION;

    private static XModuleResources mResources;

    /** After calling initSnapPreviewFragment() below, we set the
     * initializedUri to the current media's Uri to prevent another call of onCreate() to initialize
     * the media again. E.g. onCreate() is called again if the phone is rotated. */
    private Uri initializedUri;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        mResources = XModuleResources.createInstance(startupParam.modulePath, null);
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Set isModuleEnabled to true for detection whether Snapshare is enabled
        if (lpparam.packageName.equals(Commons.PACKAGE_NAME)) {
            findAndHookMethod("com.p1ngu1n.snapshare.Utils", lpparam.classLoader, "isModuleEnabled", XC_MethodReplacement.returnConstant(true));
        }

        if (!lpparam.packageName.equals("com.snapchat.android"))
            return;

        XposedUtils.refreshPreferences();

        /** thanks to KeepChat for the following snippet: **/
        try {
            XposedUtils.log("----------------- SNAPSHARE HOOKED -----------------", false);
            Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
            Context context = (Context) callMethod(activityThread, "getSystemContext");

            PackageInfo piSnapChat = context.getPackageManager().getPackageInfo(lpparam.packageName, 0);
            XposedUtils.log("SnapChat Version: " + piSnapChat.versionName + " (" + piSnapChat.versionCode + ")", false);

            PackageInfo piSnapshare = context.getPackageManager().getPackageInfo(Commons.PACKAGE_NAME, 0);
            XposedUtils.log("Snapshare Version: " + piSnapshare.versionName + " (" + piSnapshare.versionCode + ")\n", false);

            SNAPCHAT_VERSION = Obfuscator.getVersion(piSnapChat.versionCode);
        }
        catch (Exception e) {
            XposedBridge.log("Snapshare: exception while trying to get version info. (" + e.getMessage() + ")");
            return;
        }

        final Class SnapCapturedEventClass = findClass("com.snapchat.android.util.eventbus.SnapCapturedEvent", lpparam.classLoader);
        final Media media = new Media(); // a place to store the media

        /**
         * Here the main work happens. We hook after the onCreate() call of the main Activity
         * to create a sensible media object.
         */
        findAndHookMethod("com.snapchat.android.LandingPageActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedUtils.refreshPreferences();
                XposedUtils.log("----------------- SNAPSHARE STARTED -----------------", false);
                final Activity activity = (Activity) param.thisObject;
                // Get intent, action and MIME type
                Intent intent = activity.getIntent();
                String type = intent.getType();
                String action = intent.getAction();
                XposedUtils.log("Intent type: " + type + ", intent action:" + action);

                // Check if this is a normal launch of Snapchat or actually called by Snapshare and if loaded from recents
                if (type != null && Intent.ACTION_SEND.equals(action) && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
                    Uri mediaUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    // Check for bogus call
                    if (mediaUri == null) {
                        return;
                    }
                    /* We check if the current media got already initialized and should exit instead
                     * of doing the media initialization again. This check is necessary
                     * because onCreate() is also called if the phone is just rotated. */
                    if (initializedUri == mediaUri) {
                        XposedUtils.log("Media already initialized, exit onCreate() hook");
                        return;
                    }

                    ContentResolver contentResolver = activity.getContentResolver();

                    if (type.startsWith("image/")) {
                        XposedUtils.log("Image URI: " + mediaUri.toString());
                        try {
                            /* TODO: use BitmapFactory with inSampleSize magic to avoid using too much memory,
                             * see http://developer.android.com/training/displaying-bitmaps/load-bitmap.html#load-bitmap */
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(contentResolver, mediaUri);
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            XposedUtils.log("Image shared, size: " + width + " x " + height + " (w x h)");

                            // Landscape images have to be rotated 90 degrees clockwise for Snapchat to be displayed correctly
                            if (width > height && Commons.ROTATION_MODE != Commons.ROTATION_NONE) {
                                XposedUtils.log("Landscape image detected, rotating image " + Commons.ROTATION_MODE + " degrees");
                                android.graphics.Matrix matrix = new android.graphics.Matrix();
                                matrix.setRotate(Commons.ROTATION_MODE);
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                                // resetting width and height
                                width = bitmap.getWidth();
                                height = bitmap.getHeight();
                            }

                            /**
                             * Scaling and cropping mayhem
                             *
                             * Snapchat will break if the image is too large and it will scale the image up if the
                             * Display rectangle (DisplayMetrics.widthPixels x DisplayMetrics.heightPixels rectangle)
                             * is larger than the image.
                             *
                             * So, we sample the image down such that the Display rectangle fits into it and touches one side.
                             * Then we crop the picture to that rectangle
                             */
                            DisplayMetrics dm = new DisplayMetrics();
                            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
                            int dWidth = dm.widthPixels;
                            int dHeight = dm.heightPixels;

                            XposedUtils.log("Display metrics: " + dWidth + " x " + dHeight + " (w x h)");
                            // DisplayMetrics' values depend on the phone's tilt, so we normalize them to Portrait mode
                            if (dWidth > dHeight) {
                                XposedUtils.log("Normalizing display metrics to Portrait mode.");
                                int temp = dWidth;
                                //noinspection SuspiciousNameCombination
                                dWidth = dHeight;
                                dHeight = temp;
                            }

                            if(Commons.ADJUST_METHOD == Commons.ADJUST_CROP) {
                                int imageToDisplayRatio = width * dHeight - height * dWidth;
                                if (imageToDisplayRatio > 0) {
                                    // i.e., width/height > dWidth/dHeight, so have to crop from left and right:
                                    int newWidth = (dWidth * height / dHeight);
                                    XposedUtils.log("New width after cropping left and right: " + newWidth);
                                    bitmap = Bitmap.createBitmap(bitmap, (width - newWidth) / 2, 0, newWidth, height);
                                } else if (imageToDisplayRatio < 0) {
                                    // i.e., width/height < dWidth/dHeight, so have to crop from top and bottom:
                                    int newHeight = (dHeight * width / dWidth);
                                    XposedUtils.log("New height after cropping top and bottom: " + newHeight);
                                    bitmap = Bitmap.createBitmap(bitmap, 0, (height - newHeight) / 2, width, newHeight);
                                }

                                /* If the image properly covers the Display rectangle, we mark it as a "large" image
                                 * and are going to scale it down. We make this distinction because we don't wanna
                                 * scale the image up if it is smaller than the Display rectangle. */
                                boolean largeImage = ((width > dWidth) & (height > dHeight));
                                XposedUtils.log(largeImage ? "Large image, scaling down" : "Small image");
                                if (largeImage) {
                                    bitmap = Bitmap.createScaledBitmap(bitmap, dWidth, dHeight, true);
                                }
                                // Scaling and cropping finished, ready to let Snapchat display our result
                            }
                            else {
                                // we are going to scale the image down and place a black background behind it
                                Bitmap background = Bitmap.createBitmap(dWidth, dHeight, Bitmap.Config.ARGB_8888);
                                background.eraseColor(Color.BLACK);
                                Canvas canvas = new Canvas(background);
                                android.graphics.Matrix transform = new android.graphics.Matrix();
                                float scale = dWidth / (float)width;
                                float xTrans = 0;
                                if(Commons.ADJUST_METHOD == Commons.ADJUST_NONE) {
                                    // Remove scaling and add some translation
                                    scale = 1;
                                    xTrans = dWidth/2 - width/2;
                                }
                                float yTrans = dHeight/2 - scale*height/2;

                                transform.preScale(scale, scale);
                                transform.postTranslate(xTrans, yTrans);
                                Paint paint = new Paint();
                                paint.setFilterBitmap(true);
                                canvas.drawBitmap(bitmap, transform, paint);
                                bitmap = background;
                            }

                            // Make Snapchat show the image
                            media.setContent(bitmap);
                        } catch (FileNotFoundException e) {
                            XposedUtils.log("File not found!\n" + e.getMessage());
                        } catch (IOException e) {
                            XposedUtils.log("IO Error!\n" + e.getMessage());
                        }
                    }
                    else if (type.startsWith("video/")) {
                        Uri videoUri;
                        // Snapchat expects the video URI to be in the file:// scheme, not content:// scheme
                        if (URLUtil.isFileUrl(mediaUri.toString())) {
                            videoUri = mediaUri;
                            XposedUtils.log("Already had File URI: " + mediaUri.toString());
                        } else { // No file URI, so we have to convert it
                            videoUri = Utils.convertContentToFileUri(contentResolver, mediaUri);
                            if (videoUri != null) {
                                XposedUtils.log("Converted content URI to file URI " + videoUri.toString());
                            } else {
                                XposedUtils.log("Couldn't resolve URI to file:// scheme: " + mediaUri.toString());
                                return;
                            }
                        }

                        File videoFile = new File(videoUri.getPath());
                        File tempFile = File.createTempFile("snapshare_video", null);

                        try {
                            if (Commons.ROTATION_MODE == Commons.ROTATION_NONE) {
                                XposedUtils.log("Rotation disabled, creating a temporary copy");
                                Utils.copyFile(videoFile, tempFile);
                            } else {
                                DataSource dataSource = new FileDataSourceImpl(videoFile);
                                IsoFile isoFile = new IsoFile(dataSource);

                                List<TrackBox> trackBoxes = isoFile.getMovieBox().getBoxes(TrackBox.class);
                                // Iterate through all tracks until the track is a video track (type equals 'vide')
                                for (TrackBox trackBox : trackBoxes) {
                                    if (trackBox.getMediaBox().getHandlerBox().getHandlerType().equals("vide")) {
                                        TrackHeaderBox trackHeaderBox = trackBox.getTrackHeaderBox();
                                        // Get the dimensions of the video
                                        double width = trackHeaderBox.getWidth();
                                        double height = trackHeaderBox.getHeight();
                                        DecimalFormat format = new DecimalFormat("#");
                                        XposedUtils.log("Video resolution: " + format.format(width) + " x " + format.format(height) + " (w x h)");

                                        // Determine the way to rotate
                                        Matrix matrix;
                                        if (width > height) {
                                            if (Commons.ROTATION_MODE == Commons.ROTATION_CW) {
                                                matrix = Matrix.ROTATE_90;
                                            } else {
                                                matrix = Matrix.ROTATE_270;
                                            }
                                        } else {
                                            matrix = Matrix.ROTATE_0;
                                        }

                                        if (matrix.equals(trackHeaderBox.getMatrix())) {
                                            XposedUtils.log("Keeping rotation at " + matrixToString(matrix) + ", just creating a copy");
                                            Utils.copyFile(videoFile, tempFile);
                                        } else {
                                            XposedUtils.log("Rotation changed from " + matrixToString(trackHeaderBox.getMatrix()) + " to " + matrixToString(matrix));
                                            // Set the rotation matrix
                                            trackHeaderBox.setMatrix(matrix);
                                            // Write the video to the temp file
                                            FileOutputStream fos = new FileOutputStream(tempFile);
                                            FileChannel fc = fos.getChannel();
                                            isoFile.writeContainer(fc);
                                            fc.close();
                                            fos.close();
                                        }
                                        break;
                                    }
                                }
                            }

                            videoUri = Uri.fromFile(tempFile);
                            XposedUtils.log("Temporary file path: " + videoUri);
                        } catch (Exception e) {
                            XposedUtils.log(e);
                            return;
                        }

                        long fileSize = tempFile.length();
                        // Get size of video and compare to the maximum size
                        if (Commons.CHECK_SIZE && fileSize > Commons.MAX_VIDEO_SIZE) {
                            String readableFileSize = Utils.formatBytes(fileSize);
                            String readableMaxSize = Utils.formatBytes(Commons.MAX_VIDEO_SIZE);
                            XposedUtils.log("Video too big (" + readableFileSize + ")");
                            // Inform the user with a dialog
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                            dialogBuilder.setTitle(mResources.getString(R.string.app_name));
                            dialogBuilder.setMessage(mResources.getString(R.string.size_error, readableFileSize, readableMaxSize));
                            dialogBuilder.setPositiveButton(mResources.getString(R.string.continue_anyway), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                            dialogBuilder.setNegativeButton(mResources.getString(R.string.go_back), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    activity.finish();
                                }
                            });
                            dialogBuilder.show();
                        }
                        media.setContent(videoUri);
                    }
                    /* Finally the image or video is marked as initialized to prevent reinitialisation of
                     * the SnapCapturedEvent in case of a screen rotation (because onCreate() is then called).
                     * This way, it is made sure that a shared image or media is only initialized and then
                     * showed in a SnapPreviewFragment once.
                     * Also, if Snapchat is used normally after being launched by Snapshare, a screen rotation
                     * while in the SnapPreviewFragment, would draw the shared image or video instead of showing
                     * what has just been recorded by the camera. */
                    initializedUri = mediaUri;
                }
                else {
                    XposedUtils.log("Regular call of Snapchat.");
                    initializedUri = null;
                }
            }

        });
        /** 
         * We needed to find a method that got called after the camera was ready. 
         * refreshFlashButton is the only method that falls under this category.
         * As a result, we need to be very careful to clean up after ourselves, to prevent
         * crashes and not being able to quit etc...
         * 
         * after the method is called, we call the eventbus to send a snapcapture event 
         * with our own media.
         */

        // new in 5.0.2: CameraFragment!
        String cameraFragment = (SNAPCHAT_VERSION < Obfuscator.FIVE_ZERO_TWO ? "CameraPreviewFragment" : "CameraFragment");
        findAndHookMethod("com.snapchat.android.camera." + cameraFragment, lpparam.classLoader, Obfuscator.CAMERA_LOAD.getValue(SNAPCHAT_VERSION), new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(initializedUri == null) return; // We don't have an image to send, so don't try to send one
                XposedUtils.log("Doing it's magic!");
                Object snapCaptureEvent;

                // new stuff for 4.1.10: Class called Snapbryo (gross)
                // this class now stores all the data for snaps. What's good for us is that we can continue using either a bitmap or a videouri in a method.
                // SnapCapturedEvent(Snapbryo(Builder(Media)))
                if(SNAPCHAT_VERSION >= Obfuscator.FOUR_ONE_TEN) {
                    Object builder = newInstance(findClass("com.snapchat.android.model.Snapbryo.Builder", lpparam.classLoader));
                    builder = callMethod(builder, Obfuscator.BUILDER_CONSTRUCTOR.getValue(SNAPCHAT_VERSION), media.getContent());
                    Object snapbryo = callMethod(builder, Obfuscator.CREATE_SNAPBRYO.getValue(SNAPCHAT_VERSION));
                    snapCaptureEvent = newInstance(SnapCapturedEventClass, snapbryo);
                }
                else {
                    snapCaptureEvent = newInstance(SnapCapturedEventClass, media.getContent());
                }

                Object busProvider = callStaticMethod(findClass("com.snapchat.android.util.eventbus.BusProvider", lpparam.classLoader), Obfuscator.GET_BUS.getValue(SNAPCHAT_VERSION));
                callMethod(busProvider, Obfuscator.BUS_POST.getValue(SNAPCHAT_VERSION), snapCaptureEvent);

                initializedUri = null; // clean up after ourselves. If we don't do this snapchat will crash.
            }
        });

        // Enable Snapchat's internal debugging class
        if (Commons.TIMBER) {
            XposedUtils.log("Timber enabled");
            Class<?> timberClass = findClass("com.snapchat.android.Timber", lpparam.classLoader);
            // Return true when checking whether it should debug
            findAndHookMethod(timberClass, "a", XC_MethodReplacement.returnConstant(true));
            findAndHookMethod(timberClass, "b", XC_MethodReplacement.returnConstant(true));
            findAndHookMethod(timberClass, "c", XC_MethodReplacement.returnConstant(true));
            // Usually returns the class name to use as the Log Tag, however we want a custom one
            findAndHookMethod(timberClass, "d", XC_MethodReplacement.returnConstant("SnapchatTimber"));
        }
    }

    private static String matrixToString(Matrix matrix) {
        if (matrix.equals(Matrix.ROTATE_0)) {
            return "0°";
        } else if (matrix.equals(Matrix.ROTATE_90)) {
            return "90°";
        } else if (matrix.equals(Matrix.ROTATE_180)) {
            return "180°";
        } else if (matrix.equals(Matrix.ROTATE_270)) {
            return "270°";
        } else {
            return "(unknown)°";
        }
    }

    /** {@code XposedHelpers.callMethod()} cannot call methods of the super class of an object, because it
     * uses {@code getDeclaredMethods()}. So we have to implement this little helper, which should work
     * similar to {@code }callMethod()}. Furthermore, the exceptions from getMethod() are passed on.
     * <p>
     * At the moment, only argument-free methods supported (only case needed here). After a discussion
     * with the Xposed author it looks as if the functionality to call super methods will be implemented
     * in {@code XposedHelpers.callMethod()} in a future release.
     *
     * @param obj Object whose method should be called
     * @param methodName String representing the name of the argument-free method to be called
     * @return The object that the method call returns
     * @see <a href="http://forum.xda-developers.com/showpost.php?p=42598280&postcount=1753">
     *     Discussion about calls to super methods in Xposed's XDA thread</a>
     */
    private Object callSuperMethod(Object obj, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return obj.getClass().getMethod(methodName).invoke(obj);
    }
}
