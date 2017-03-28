Situm Android SDK Sample app
=======================

This is a sample Android application built with the Situm SDK. With the sample app you will be able 
to list all the buildings of your account and start the positioning for the selected building showing 
 the first floor image of the building and the locations received.

## Table of contents

[Introduction](#introduction)

[Step 1: Configure our SDK in your Android project](#configureproject)

[Step 2: Initialize the SDK](#init)

[Step 3: Set your credentials](#config)

[Step 4: Get buildings information](#communicationmanager)

[Step 5: Start the positioning](#positioning)

[More information](#moreinfo)

### Introduction

Situm SDK is a set of utilities that allow any developer to build location based apps using Situm's indoor positioning system. 
Among many other capabilities, apps developed with Situm SDK will be able to:

1. Obtain information related to buildings where Situm's positioning system is already configured: 
floor plans, points of interest, geotriggered events, etc.
2. Retrieve the location of the smartphone inside these buildings (position, orientation, and floor 
where the smartphone is).
3. Compute a route from a point A (e.g. where the smartphone is) to a point B (e.g. any point of 
interest within the building).
4. Trigger notifications when the user enters a certain area.

In this tutorial, we will guide you step by step to set up your first Android application using Situm SDK. 
Before starting to write code, we recommend you to set up an account in our Dashboard 
(https://dashboard.situm.es), retrieve your API KEY and configure your first building.

1. Go to the [sign in form](http://dashboard.situm.es/accounts/register) and enter your username 
and password to sign in.
2. Go to the [account section](https://dashboard.situm.es/accounts/profile) and on the bottom, click 
on "generate one" to generate your API KEY.
3. Go to the [buildings section](http://dashboard.situm.es/buildings) and create your first building.
4. Download [Situm Mapping Tool](https://play.google.com/store/apps/details?id=es.situm.maps) 
Android application. With this application you will be able to configure and test Situm's indoor 
positioning system in your buildings.

Perfect! Now you are ready to develop your first indoor positioning application.

### <a name="configureproject"></a> Step 1: Configure our SDK in your Android project

First of all, you must configure Situm SDK in your Android project. This has been already done for 
you in the sample application, but nonetheless we will walk you to the process.

* Add the next line to the project *build.gradle* in the *repositories* section of *allprojects* to
add our maven repository to the project:

```groovy
maven { url "https://repo.situm.es/artifactory/libs-release-local" }
```

* Then add the Situm SDK library dependency into the section *dependencies* of the app *build.gradle*.
It's important to add the `transitive = true` property to download the Situm SDK dependencies.

```groovy
    compile ('es.situm:situm-sdk:2.0.0@aar') {
        transitive = true
    }
```

### <a name="init"></a> Step 2: Initialize the SDK

You must initialize the sdk in the `onCreate()` method of your Application:

```java
SitumSdk.init(this);
```

### <a name="config"></a> Step 3: Set your credentials

There are two ways to set the credentials, in the `AndroidManifest.xml` file or programmatically.

##### Option 1: `AndroidManifest.xml` file

You can set the credentials (user and API key)in the `AndroidManifest.xml` file adding the next `meta-data` fields:

```xml
<meta-data
    android:name="es.situm.sdk.API_USER"
    android:value="API_USER_EMAIL" />
<meta-data
    android:name="es.situm.sdk.API_KEY"
    android:value="API_KEY" />
```

##### Option 2: programmatically

In the code, you can set the the user and API key with:

```java
SitumSdk.configuration().setApiKey("USER_EMAIL", "API_KEY");
```

or you can set the user and password with:

```java
SitumSdk.configuration().setUserPass("USER_EMAIL", "PASSWORD");
```

### <a name="communicationmanager"></a> Step 4: Get buildings information

Now that you have correctly configured your Android project, you can start writing your application's code. 
In this sample project, all this code has been included in the file [MainActivity.java](https://github.com/situmtech/situm-android-getting-started/blob/master/app/src/main/java/es/situm/gettingstarted/MainActivity.java)

First of all, you can get and instance of the `CommunicationManager` with `SitumSdk.communicationManager()`.
With this object you can fetch your building's data (list of buildings, floorplans, points of interest, etc.):

In the next example we fetch all the buildings associated with our user's account and print them to the Logcat:

```java
 SitumSdk.communicationManager().fetchBuildings(new Handler<Collection<Building>>() {
            @Override
            public void onSuccess(Collection<Building> buildings) {
                Log.d(TAG, "onSuccess: Your buildings: ");
                for (Building building : buildings) {
                    Log.i(TAG, "onSuccess: " + building.getIdentifier() + " - " + building.getName());
                }
            }

            @Override
            public void onFailure(Error error) {
                Log.e(TAG, "onFailure:" + error);
            }
        });
```

### <a name="positioning"></a>Step 5: Start the positioning

The last step is to initiate the indoor positioning on a certain building. 
This will allow the app to retrieve the location of the smartphone within this building.

To do this you need to create a location request indicating the building where you want to start
 the positioning:

```java
LocationRequest locationRequest = new LocationRequest.Builder()
        .buildingIdentifier(selectedBuilding.getIdentifier())
        .build();
```

Then, you need to implement the listener in where you will receive the location updates, status and errors.

```java
private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.i(TAG, "onLocationChanged() called with: location = [" + location + "]");
        }

        @Override
        public void onStatusChanged(@NonNull LocationStatus status) {
            Log.i(TAG, "onStatusChanged() called with: status = [" + status + "]");
        }

        @Override
        public void onError(@NonNull Error error) {
            Log.e(TAG, "onError() called with: error = [" + error + "]");
        }
    };

```

In `onLocationChanged(Location)` you will be updated with the locations. This `Location` object contains
the building identifier, level identifier, cartesian coordinates, geographic coordinates, orientation,
accuracy, among others.

In `onStatusChanged(int)` you will received the changes in the status of the system: starting, initializing,
user not in building, compass need to be calibrated, etc.

In `onError(Error)` you will received the errors produced, the positioning will stop. 

From API 23 you need to ask for the location permissions at runtime, if the location permission is not 
granted, and error with code `LocationErrorConstant.Code.MISSING_LOCATION_PERMISSION` will be received.
Also, the location must be enabled in order to scan Wifi and BLE, an error with code `LocationErrorConstant.Code.LOCATION_DISABLED`
will be received in this case. In the example you can see how to manage this errors.

Finally, you start the positioning with:

```java
SitumSdk.locationManager().requestLocationUpdates(locationRequest, locationListener);
```

## <a name="moreinfo"></a> More information

More info is available at https://des.situm.es/developers/pages/android/.
For any other question, contact us in https://situm.es/contact.
