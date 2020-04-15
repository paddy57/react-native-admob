import { arrayOf, func, string, number } from 'prop-types';
import React, { Component } from 'react';
import {
  findNodeHandle,
  requireNativeComponent,
  UIManager,
  ViewPropTypes,
} from 'react-native';
import { createErrorFromErrorData } from './utils';

class RNAdMobNative extends Component {
  constructor() {
    super();
    this.handleAdFailedToLoad = this.handleAdFailedToLoad.bind(this);
    this.state = {
      style: {},
    };
  }

  componentDidMount() {
    const { adUnitID, navBarHeight } = this.props;
    console.debug("NativeAd dd",adUnitID )
    if (adUnitID) {
      return this.loadNativeAd(adUnitID , navBarHeight);
    }
    console.warn('Attempted to load native ad without ad unit id');
  }

  loadNativeAd(adUnitId, navBarHeight) {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this._nativeView),
      UIManager.getViewManagerConfig('RNGADNativeView').Commands.loadNativeAd,
      [adUnitId, navBarHeight]
    );
  }

  handleAdFailedToLoad(event) {
    if (this.props.onAdFailedToLoad) {
      this.props.onAdFailedToLoad(
        createErrorFromErrorData(event.nativeEvent.error)
      );
    }
  }

  setRef = (el) => (this._nativeView = el);

  render() {
    return (
      <RNGADNativeView
        {...this.props}
        style={[this.props.style, this.state.style]}
        onAdFailedToLoad={this.handleAdFailedToLoad}
        ref={this.setRef}
      />
    );
  }
}

RNAdMobNative.simulatorId = 'SIMULATOR';

RNAdMobNative.propTypes = {
  ...ViewPropTypes,

  /**
   * Google AdMob templates are being used to display native ads
   * (https://developers.google.com/admob/ios/native/templates)
   * Two sizes are available; medium and small
   *
   * small is default
   */
  adSize: string,
  navBarHeight: string, 

  /**
   * AdMob ad unit ID
   */
  adUnitID: string,

  /**
   * Array of test devices. Use AdMobBanner.simulatorId for the simulator
   */
  testDevices: arrayOf(string),

  /**
   * GADUnifiedNativeAdDelegate lifecycle methods
   * https://developers.google.com/ad-manager/mobile-ads-sdk/ios/api/reference/Protocols/GADUnifiedNativeAdDelegate
   */
  onAdOpened: func,
  onAdClosed: func,
  onAdLeftApplication: func,
  didRecordImpression: func,
  didRecordClick: func,

  /**
   * GADUnifiedNativeAdLoaderDelegate
   * https://developers.google.com/ad-manager/mobile-ads-sdk/ios/api/reference/Protocols/GADUnifiedNativeAdLoaderDelegate
   */
  onAdLoaded: func,
  onAdFailedToLoad: func,
};

const RNGADNativeView = requireNativeComponent('RNGADNativeView', RNAdMobNative);

export default RNAdMobNative;
