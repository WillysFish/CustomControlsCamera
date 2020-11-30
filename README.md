# CustomControlsCameraSample
<img src="https://github.com/WillysFish/CustomControlsCamera/blob/main/device-2020-11-10-185405.png" height="20%" width="20%" > <img src="https://github.com/WillysFish/CustomControlsCamera/blob/main/device-2020-11-10-185459.png" height="20%" width="20%" >

CustomControlsCamera is a camera preview view which customizable controls.  
And It also offers some functions to operation camera.  
They were implemented with CameraX API  


# Functions
- Rotate screen

- Capture

- Switch Lens  
  <img src="https://github.com/WillysFish/CustomControlsCamera/blob/main/device-2020-11-10-190210.gif" height="20%" width="20%" >

- Flashlight 
  - Capture with back flashlight  
    <img src="https://github.com/WillysFish/CustomControlsCamera/blob/main/device-2020-11-10-185911.gif" height="20%" width="20%" >

  - Capture with fake front flashlight  
    <img src="https://github.com/WillysFish/CustomControlsCamera/blob/main/device-2020-11-10-190653.gif" height="20%" width="20%" >


# Download
### Gradle:  
Add it in your root build.gradle at the end of repositories:
```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
Add the dependency
```gradle
dependencies {
   implementation 'com.github.WillysFish:CustomControlsCamera:1.0.0'
}
```
### Maven:
Add the JitPack repository to your build file
```xml
<repositories>
  <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
  </repository>
</repositories>
```
Add the dependency
```xml
<dependency>
  <groupId>com.github.WillysFish</groupId>
  <artifactId>CustomControlsCamera</artifactId>
  <version>1.1.0</version>
</dependency>
```
# How to use?  
- Give me permission of camera.
  ```xml
  <uses-feature android:name="android.hardware.camera" />
  <uses-permission android:name="android.permission.CAMERA" />
  ```
  And invoke Activity.requestPermissions()
  ```kotlin
    public final void requestPermissions(@NonNull String[] permissions, int requestCode) {}
  ```
&nbsp;
- Add CustomControlsCameraView in your layout  
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <studio.zewei.willy.customcontrolscameraview.CustomControlsCameraView
        android:id="@+id/cameraView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```  
&nbsp;
- Initial your controls layout and get it.
```kotlin
  // Init CameraView
  cameraView.initCameraWithOwner(this, R.layout.camera_control_layout) {
      
      // Wait for the views to be properly laid out
      val controlsView = cameraView.controlsView

      do something with controls...
  }
```
&nbsp;
- Finished! You can operate camera right now.
  - Rotate screen  
  ```kotlin
      fun setDisplayRotation(rotation: Int) {}
  ```
  - Capture  
  ```kotlin
      fun capture(photoFile: File, finishAction: (uri: Uri) -> Unit) {}
  ```
  - Switch Lens  
  ```kotlin
      fun switchLensFacing(specific: Int? = null) {}
  ```
  - Open Flashlight  
  ```kotlin
      cameraView.isNeedFlashlight = true
  ```

  

# License
```
Copyright 2020 zewei yan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
