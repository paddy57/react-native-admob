package com.sbugert.rnadmob;

import android.content.Context;

import androidx.annotation.Nullable;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.google.android.gms.ads.formats.NativeAd.Image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.android.gms.ads.formats.NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT;


class ReactNativeView extends ReactViewGroup {

    String adUnitID;
    String adSize = "small";
    UnifiedNativeAdView nativeAdView;


    private TextView primaryView;
    private TextView secondaryView;
    private LinearLayout buttonLayout;

    private ImageView iconView;
    private MediaView mediaView;
    private LinearLayout callToActionParentView;
    private Button callToActionView;
    private LinearLayout background;

    public ReactNativeView(final Context context) {
        super(context);
    }

    private void createAdView() {
        final Context context = getContext();
        Log.d("NativeAD create", this.adUnitID);
        AdLoader.Builder builder = new AdLoader.Builder(context, this.adUnitID);
        Log.d("NativeAD builder", String.valueOf(builder));
        builder.forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
            @Override
            public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                // Show the ad.
                sendEvent(RNAdMobNativeViewManager.EVENT_AD_LOADED, null);
                Log.d("NativeAD adload", String.valueOf(unifiedNativeAd.getImages()));
                Log.d("NativeAD unifNeAd", String.valueOf(unifiedNativeAd));
                populateUnifiedNativeAdView(unifiedNativeAd, nativeAdView);
                if (nativeAdView == null) return; // ADD THIS LINE HERE
                //calculate ad size and layout
                nativeAdView.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
                nativeAdView.layout(0, 0, nativeAdView.getMeasuredWidth(), nativeAdView.getMeasuredHeight());
            }
        })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // Handle the failure by logging, altering the UI, and so on.
                        String errorMessage = "Unknown error";
                        Log.d("NativeAD ERROR", String.valueOf(errorCode));
                        Log.d("NativeAD ERROR2", "error");
                        switch (errorCode) {
                            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                                errorMessage = "Internal error, an invalid response was received from the ad server.";
                                break;
                            case AdRequest.ERROR_CODE_INVALID_REQUEST:
                                errorMessage = "Invalid ad request, possibly an incorrect ad unit ID was given.";
                                break;
                            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                                errorMessage = "The ad request was unsuccessful due to network connectivity.";
                                break;
                            case AdRequest.ERROR_CODE_NO_FILL:
                                errorMessage = "The ad request was successful, but no ad was returned due to lack of ad inventory.";
                                break;
                        }
                        WritableMap event = Arguments.createMap();
                        WritableMap error = Arguments.createMap();
                        Log.d("NativeAD ERROR", errorMessage);
                        error.putString("message", errorMessage);
                        event.putMap("error", error);
                        sendEvent(RNAdMobNativeViewManager.EVENT_AD_FAILED_TO_LOAD, event);
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .build())
                .build();


        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .setMediaAspectRatio(NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
                .build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
            }
        }).build();

        adLoader.loadAd(new PublisherAdRequest.Builder().build());
        // adLoader.loadAds(new AdRequest.Builder().build(), 5);
        LayoutInflater inflater = LayoutInflater.from(context);
        this.nativeAdView = (UnifiedNativeAdView) inflater.inflate(R.layout.medium_template, null);
        this.addView(nativeAdView);

    }

    private boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    private boolean adHasOnlyStore(UnifiedNativeAd nativeAd) {
        String store = nativeAd.getStore();
        String advertiser = nativeAd.getAdvertiser();
        return !isNullOrEmpty(store) && isNullOrEmpty(advertiser);
    }

    private boolean adHasOnlyAdvertiser(UnifiedNativeAd nativeAd) {
        String store = nativeAd.getStore();
        String advertiser = nativeAd.getAdvertiser();
        return !isNullOrEmpty(advertiser) && isNullOrEmpty(store);
    }

    private boolean adHasBothStoreAndAdvertiser(UnifiedNativeAd nativeAd) {
        String store = nativeAd.getStore();
        String advertiser = nativeAd.getAdvertiser();
        return (!isNullOrEmpty(advertiser)) && (!isNullOrEmpty(store));
    }

    /**
     * Populates a {@link UnifiedNativeAdView} object with data from a given
     * {@link UnifiedNativeAd}.
     *
     * @param nativeAd the object containing the ad's assets
     * @param adView   the view to be populated
     */
    private void populateUnifiedNativeAdView(UnifiedNativeAd nativeAd, UnifiedNativeAdView adView) {
        nativeAdView = (UnifiedNativeAdView) findViewById(R.id.native_ad_view);
        primaryView = (TextView) findViewById(R.id.primary);
        secondaryView = (TextView) findViewById(R.id.secondary);

        callToActionView = (Button) findViewById(R.id.cta);
        iconView = (ImageView) findViewById(R.id.icon);
        mediaView = (MediaView) findViewById(R.id.media_view);


        callToActionParentView = (LinearLayout) findViewById(R.id.cta_parent);
        buttonLayout = (LinearLayout) findViewById(R.id.buttonLayout);
        background = (LinearLayout) findViewById(R.id.background);

        //add this
        if (nativeAdView == null) {
            return;
        }

        String store = nativeAd.getStore();
        String advertiser = nativeAd.getAdvertiser();
        String headline = nativeAd.getHeadline();
        String body = nativeAd.getBody();
        String cta = nativeAd.getCallToAction();
        Double starRating = nativeAd.getStarRating();
        Image icon = nativeAd.getIcon();

//           Log.d("NativeAD responseAd store",store);
        //          Log.d("NativeAD responseAd advertiser",advertiser);
        Log.d("NativeAD responseAd headline", String.valueOf(nativeAd));
        Log.d("NativeAD responseAd headline", headline);
        Log.d("NativeAD responseAd body", body);
        Log.d("NativeAD responseAd cta", cta);
//                          Log.d("NativeAD responseAd store",store);
        Log.d("NativeAD responseAd starRating", String.valueOf(starRating));
        Log.d("NativeAD responseAd icon", String.valueOf(icon.getUri()));
        Log.d("NativeAD responseAd images", String.valueOf(nativeAd.getImages()));
        Log.d("NativeAD responseAd images", String.valueOf(nativeAd.getExtras()));

        String tertiaryText;
        //add this
        if (nativeAdView == null) {
            return;
        }

        nativeAdView.setCallToActionView(buttonLayout);
        nativeAdView.setHeadlineView(primaryView);
        nativeAdView.setMediaView(mediaView);

        mediaView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                if (child instanceof ImageView) {
                    ImageView imageView = (ImageView) child;


                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                    if (windowManager != null) {
                        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                    }

                    int imageHeight = imageView.getDrawable().getIntrinsicHeight();
                    int screenHeight = displayMetrics.heightPixels;
                    Log.d("DISPLAY1 screen", String.valueOf(screenHeight));
                    Log.d("DISPLAY1 image", String.valueOf(imageHeight));
                    if (imageHeight > (screenHeight / 2)) {
                        int newImageHeight = imageHeight - (screenHeight / 7);
                        imageView.getLayoutParams().height = newImageHeight;
                        Log.d("DISPLAY1 image change", String.valueOf(newImageHeight));

                    } else {
                        Log.d("DISPLAY1 no change", String.valueOf(imageHeight));
                        imageView.setAdjustViewBounds(true);
                    }

                }
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
            }
        });


        if (adHasOnlyAdvertiser(nativeAd)) {
            secondaryView.setLines(1);
        } else if (adHasBothStoreAndAdvertiser(nativeAd)) {
            secondaryView.setLines(1);
        } else {
            secondaryView.setLines(3);
        }

        //add this
        if (nativeAdView == null) {
            return;
        }

        primaryView.setText(headline);

        callToActionView.setText(cta);

        // Set the secondary view to be the star rating if available.
        // Otherwise fall back to the body text.
        //add this
        if (nativeAdView == null) {
            return;
        }

        if (starRating != null && starRating > 0) {
            secondaryView.setVisibility(GONE);
        } else {
            secondaryView.setText(body);
            secondaryView.setVisibility(VISIBLE);
            nativeAdView.setBodyView(secondaryView);
        }

        //add this
        if (nativeAdView == null) {
            return;
        }

        if (icon != null) {
            iconView.setVisibility(VISIBLE);
            iconView.setImageDrawable(icon.getDrawable());
        } else {
            iconView.setVisibility(GONE);
        }


        //add this
        if (nativeAdView == null) {
            return;
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd);

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        VideoController vc = nativeAd.getVideoController();
    }

    private void sendEvent(String name, @Nullable WritableMap event) {
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                name,
                event);
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(120 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public void loadNativeAd(String adUnitID) {
        this.createAdView();
    }

    public String getResponseFromAdmob() {
        Log.d("getResponseFromsServer", "triggred");
        String s = "custom methid";
        return "hello";
    }


    public void setAdUnitID(String adUnitID) {
        this.adUnitID = adUnitID;
    }

    public void setAdSize(String adSize) {
        this.adSize = adSize;
    }

    public void setTestDevices(String[] testDevices) {
        List<String> testDeviceIds = Arrays.asList(testDevices);
        RequestConfiguration configuration =
                new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
        MobileAds.setRequestConfiguration(configuration);
    }
}

public class RNAdMobNativeViewManager extends ViewGroupManager<ReactNativeView> {

    public static final String REACT_CLASS = "RNGADNativeView";

    //public static final String PROP_AD_SIZE = "adSize";
    public static final String PROP_AD_UNIT_ID = "adUnitID";
    public static final String PROP_AD_SIZE = "adSize";
    public static final String PROP_TEST_DEVICES = "testDevices";

    public static final String EVENT_SIZE_CHANGE = "onSizeChange";
    public static final String EVENT_AD_LOADED = "onAdLoaded";
    public static final String EVENT_AD_LOADING = "onAdLoading";
    public static final String EVENT_AD_FAILED_TO_LOAD = "onAdFailedToLoad";
    public static final String EVENT_AD_OPENED = "onAdOpened";
    public static final String EVENT_AD_CLOSED = "onAdClosed";
    public static final String EVENT_AD_LEFT_APPLICATION = "onAdLeftApplication";

    public static final int COMMAND_LOAD_NATIVE = 1;
    public static final int COMMAND_GET_RESPONSE = 2;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ReactNativeView createViewInstance(ThemedReactContext themedReactContext) {
        ReactNativeView adView = new ReactNativeView(themedReactContext);
        return adView;
    }

    @Override
    public void addView(ReactNativeView parent, View child, int index) {
        throw new RuntimeException("RNAdMobNativeView cannot have subviews");
    }

    @Override
    @Nullable
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
        String[] events = {
                EVENT_SIZE_CHANGE,
                EVENT_AD_LOADED,
                EVENT_AD_LOADING,
                EVENT_AD_FAILED_TO_LOAD,
                EVENT_AD_OPENED,
                EVENT_AD_CLOSED,
                EVENT_AD_LEFT_APPLICATION
        };
        for (int i = 0; i < events.length; i++) {
            builder.put(events[i], MapBuilder.of("registrationName", events[i]));
        }
        return builder.build();
    }

    @ReactProp(name = PROP_AD_UNIT_ID)
    public void setPropAdUnitID(final ReactNativeView view, final String adUnitID) {
        view.setAdUnitID(adUnitID);
    }

    @ReactProp(name = PROP_AD_SIZE)
    public void setPropAdSize(final ReactNativeView view, final String adSize) {
        view.setAdSize(adSize);
    }

    @ReactProp(name = PROP_TEST_DEVICES)
    public void setPropTestDevices(final ReactNativeView view, final ReadableArray testDevices) {
        ReadableNativeArray nativeArray = (ReadableNativeArray) testDevices;
        ArrayList<Object> list = nativeArray.toArrayList();
        view.setTestDevices(list.toArray(new String[list.size()]));
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("loadNativeAd", COMMAND_LOAD_NATIVE);

    }

    @Override
    public void receiveCommand(ReactNativeView root, int commandId, @javax.annotation.Nullable ReadableArray args) {
        if (commandId == COMMAND_LOAD_NATIVE) {
            Log.d("getResponseFromsServer", " case triggred");
            root.loadNativeAd(args.getString(0));
        } else {
            Log.d("getResponseFromsServer", "case 2 triggred");
            root.getResponseFromAdmob();
        }

    }
}


