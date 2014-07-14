Snapshare
=========
Introduction
------------

This programme lets you share any image via Snapchat, not only pictures you take with the camera from within Snapchat. When you are viewing an image and click on *share*, Snapshare will pop up as an option. It will load Snapchat and lets you even paint on the image as if you had just taken it with your camera from within Snapchat.

It uses the [Xposed framework](http://forum.xda-developers.com/showthread.php?t=1574401) to intercept launches of Snapchat from Snapshare and do it's magic.

Quick Installation
------------------
*Note: Obviously, you need __root access__ on your phone.*

1. Install [Xposed framework](http://forum.xda-developers.com/showthread.php?t=1574401)
2. Install Xposed Framework using Xposed Installer
2. Install Snapshare from the [Xposed Module Repository](http://repo.xposed.info/module/com.p1ngu1n.snapshare) in Xposed Installer
3. Activate Snapshare in the Xposed Installer app and soft reboot

Technical Details
-----------------
Snapshare introduces an Activity which presents itself to the Android system as a receiver of images.
When launched by a share action (`ACTION_SEND` as the Intent action), it launches the main launch
Activity of Snapchat, called `com.snapchat.android.LandingPageActivity` with the same intent. So
basically, it functions as a wrapper around Snapchat to be a receiver of images.

The next step uses the Xposed Framework to hook after the `onCreate()` method of the
`LandingPageActivity`. Now if the Intent is an `ACTION_SEND` Intent, Snapchat must have been
launched by Snapshare's image receiver Activity and the image URI is loaded into a Bitmap.

Before passing that Bitmap to Snapchat, the image is rotated if necessary, then cropped so that its
 aspect ratio is that of the display's viewing area and finally resized if it is larger that the
 viewing area.

Now an instance of `com.snapchat.android.util.eventbus.SnapCapturedEvent` is created with the just
 created Bitmap passed to the constructor. Finally, this instance is passed to the method
 `onSnapCaptured()` of the `LandingPageActivity`, which will load the `SnapPreviewFragment`
 displaying the image. Shazam!

License
-------
I like the GNU GPL, see the LICENSE file for further information.

*Copyright (C) 2013 Sebastian Stammler*
