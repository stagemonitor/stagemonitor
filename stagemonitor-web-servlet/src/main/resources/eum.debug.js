(function () {
  'use strict';

  var pageLoad = 'pl';

  // aliasing globals for improved minifications
  var win = window;
  var doc = win.document;
  var nav = navigator;
  var encodeURIComponent = win.encodeURIComponent;
  var XMLHttpRequest = win.XMLHttpRequest;
  var originalFetch = win.fetch;
  /**
   * Leverage's browser behavior to load image sources. Exposed via this module
   * to enable testing.
   */

  function executeImageRequest(url) {
    var image = new Image();
    image.src = url;
  }
  /**
   * Exposed via this module to enable testing.
   */

  function sendBeacon(url, data) {
    return nav.sendBeacon(url, data);
  }

  // protection against hasOwnProperty overrides.

  var globalHasOwnProperty = Object.prototype.hasOwnProperty;
  function hasOwnProperty(obj, key) {
    return globalHasOwnProperty.call(obj, key);
  }
  function now() {
    return new Date().getTime();
  }
  function noop() {} // We are trying to stay close to common tracing architectures and use
  // a hex encoded 64 bit random ID.

  var validIdCharacters = '0123456789abcdef'.split('');

  var generateUniqueIdImpl = function generateUniqueIdViaRandom() {
    var result = '';

    for (var i = 0; i < 16; i++) {
      result += validIdCharacters[Math.round(Math.random() * 15)];
    }

    return result;
  };

  if (win.crypto && win.crypto.getRandomValues && win.Uint32Array) {
    generateUniqueIdImpl = function generateUniqueIdViaCrypto() {
      var array = new win.Uint32Array(2);
      win.crypto.getRandomValues(array);
      return array[0].toString(16) + array[1].toString(16);
    };
  }

  var generateUniqueId = generateUniqueIdImpl;
  function addEventListener(target, eventType, callback) {
    if (target.addEventListener) {
      target.addEventListener(eventType, callback, false);
    } else if (target.attachEvent) {
      target.attachEvent('on' + eventType, callback);
    }
  }
  function matchesAny(regexp, s) {
    for (var i = 0, len = regexp.length; i < len; i++) {
      if (regexp[i].test(s)) {
        return true;
      }
    }

    return false;
  }

  var bus = {};
  function on(name, fn) {
    var listeners = bus[name] = bus[name] || [];
    listeners.push(fn);
  }
  function emit(name, value) {
    var listeners = bus[name];

    if (!listeners) {
      return;
    }

    for (var i = 0, length = listeners.length; i < length; i++) {
      listeners[i](value);
    }
  }

  var event = {
    name: 'e:onLoad',
    time: null,
    initialize: function () {
      if (document.readyState === 'complete') {
        return onReady();
      }

      addEventListener(win, 'load', function () {
        // we want to get timing data for loadEventEnd,
        // so asynchronously process this
        setTimeout(onReady, 0);
      });
    }
  };

  function onReady() {
    event.time = now();
    emit(event.name, event.time);
  }

  /* eslint-disable no-console */
  var log = createLogger('log');
  var info = createLogger('info');
  var warn = createLogger('warn');
  var error = createLogger('error');
  var debug = createLogger('debug');

  function createLogger(method) {
    if (typeof console === 'undefined' || typeof console.log !== 'function' || typeof console.log.apply !== 'function') {
      return noop;
    }

    if (console[method] && typeof console[method].apply === 'function') {
      return function () {
        console[method].apply(console, arguments);
      };
    }

    return function () {
      console.log.apply(console, arguments);
    };
  }

  var states = {};
  var currentStateName;
  function registerState(name, impl) {
    states[name] = impl;
  }
  function transitionTo(nextStateName) {
    {
      info('Transitioning from %s to %s', currentStateName || '<no state>', nextStateName);
    }

    currentStateName = nextStateName;
    states[nextStateName].onEnter();
  }
  function getActiveTraceId() {
    return states[currentStateName].getActiveTraceId();
  }
  function getActivePhase() {
    return states[currentStateName].getActivePhase();
  }
  function triggerManualPageLoad() {
    return states[currentStateName].triggerManualPageLoad();
  }

  var performance = win.performance || win.webkitPerformance || win.msPerformance || win.mozPerformance;
  var isTimingAvailable = performance && performance.timing;
  var isResourceTimingAvailable = performance && performance.getEntriesByType;

  var defaultVars = {
    nameOfLongGlobal: 'EumObject',
    pageLoadTraceId: generateUniqueId(),
    pageLoadBackendTraceId: null,
    serverTimingBackendTraceIdEntryName: 'intid',
    referenceTimestamp: now(),
    highResTimestampReference: performance && performance.now ? performance.now() : 0,
    initializerExecutionTimestamp: now(),
    reportingUrl: null,
    beaconBatchingTime: 500,
    apiKey: null,
    meta: {},
    ignoreUrls: [],
    ignorePings: true,
    ignoreErrorMessages: [],
    xhrTransmissionTimeout: 20000,
    whitelistedOrigins: [],
    manualPageLoadEvent: false,
    manualPageLoadTriggered: false,
    autoClearResourceTimings: true,
    page: undefined,
    wrapEventHandlers: false,
    wrappedEventHandlersOriginalFunctionStorageKey: '__weaselOriginalFunctions__',
    wrapTimers: false,
    secretPropertyKey: '__weaselSecretData__',
    userId: undefined,
    userName: undefined,
    userEmail: undefined,
    collectCspViolations: false,
    sampleRate: 1,
    maxLengthForImgRequest: 2000,
    disableResourceTimingTransmission: false
  };

  var state = {
    onEnter: function () {
      if (!defaultVars.manualPageLoadEvent || defaultVars.manualPageLoadTriggered) {
        on(event.name, onLoad);
        event.initialize();
      }
    },
    getActiveTraceId: function () {
      return defaultVars.pageLoadTraceId;
    },
    getActivePhase: function () {
      return pageLoad;
    },
    triggerManualPageLoad: onLoad
  };

  function onLoad() {
    transitionTo('pageLoaded');
  }

  var maximumNumberOfMetaDataFields = 50;
  var maximumLengthPerMetaDataField = 256;
  var languages = determineLanguages();
  function addCommonBeaconProperties(beacon) {
    beacon['k'] = defaultVars.apiKey;
    beacon['r'] = defaultVars.referenceTimestamp;
    beacon['p'] = defaultVars.page;
    beacon['pl'] = defaultVars.pageLoadTraceId;
    beacon['ui'] = defaultVars.userId;
    beacon['un'] = defaultVars.userName;
    beacon['ue'] = defaultVars.userEmail;
    beacon['ul'] = languages;
    beacon['ph'] = getActivePhase();
    beacon['ww'] = win.innerWidth;
    beacon['wh'] = win.innerHeight; // Google Closure compiler is not yet aware of these globals. Make sure it doesn't
    // mangle them.

    if (nav['connection'] && nav['connection']['effectiveType']) {
      beacon['ct'] = nav['connection']['effectiveType'];
    }

    if (doc.visibilityState) {
      beacon['h'] = doc.visibilityState === 'hidden' ? 1 : 0;
    }

    addMetaDataToBeacon(beacon, defaultVars.meta);
  }

  function determineLanguages() {
    if (nav.languages && nav.languages.length > 0) {
      return nav.languages.slice(0, 5).join(',');
    }

    if (typeof nav.userLanguage === 'string') {
      return [nav.userLanguage].join(',');
    }

    return undefined;
  }

  function addMetaDataToBeacon(beacon, meta) {
    var i = 0;

    for (var key in meta) {
      if (hasOwnProperty(meta, key)) {
        i++;

        if (i > maximumNumberOfMetaDataFields) {
          {
            warn('Maximum number of meta data fields exceeded. Not all meta data fields will be transmitted.');
          }

          return;
        }

        var serializedValue = null;

        if (typeof meta[key] === 'string') {
          serializedValue = meta[key];
        } else if (meta[key] === undefined) {
          serializedValue = 'undefined';
        } else if (meta[key] === null) {
          serializedValue = 'null';
        } else if (win.JSON) {
          try {
            serializedValue = win.JSON.stringify(meta[key]);
          } catch (e) {
            {
              warn('JSON serialization of meta data', key, meta[key], 'failed due to', e, '. This value will not be transmitted.');
            }

            continue;
          }
        } else {
          serializedValue = String(meta[key]);
        }

        beacon['m_' + key] = serializedValue.substring(0, maximumLengthPerMetaDataField);
      }
    }
  }

  var urlMaxLength = 255;
  var initiatorTypes = {
    'other': 0,
    'img': 1,
    // IMAGE element inside a SVG
    'image': 1,
    'link': 2,
    'script': 3,
    'css': 4,
    'xmlhttprequest': 5,
    'fetch': 5,
    'beacon': 5,
    'html': 6,
    'navigation': 6
  };
  var cachingTypes = {
    unknown: 0,
    cached: 1,
    validated: 2,
    fullLoad: 3
  };

  function serializeEntry(entry) {
    var result = [Math.round(entry['startTime'] - defaultVars.highResTimestampReference), Math.round(entry['duration']), initiatorTypes[entry['initiatorType']] || initiatorTypes['other']]; // When timing data is available, we can provide additional information about
    // caching and resource sizes.

    if (typeof entry['transferSize'] === 'number' && typeof entry['encodedBodySize'] === 'number' && // All this information may not be available due to the timing allow origin check.
    entry['encodedBodySize'] > 0) {
      if (entry['transferSize'] === 0) {
        result.push(cachingTypes.cached);
      } else if (entry['transferSize'] > 0 && (entry['encodedBodySize'] === 0 || entry['transferSize'] < entry['encodedBodySize'])) {
        result.push(cachingTypes.validated);
      } else {
        result.push(cachingTypes.fullLoad);
      }

      if (entry['encodedBodySize'] != null) {
        result.push(entry['encodedBodySize']);
      } else {
        result.push('');
      }

      if (entry['decodedBodySize'] != null) {
        result.push(entry['decodedBodySize']);
      } else {
        result.push('');
      }

      if (entry['transferSize'] != null) {
        result.push(entry['transferSize']);
      } else {
        result.push('');
      }
    } else {
      result.push('');
      result.push('');
      result.push('');
      result.push('');
    }

    if (entry['responseStart'] != null && // timing allow origin check may have failed
    entry['responseStart'] >= entry['fetchStart']) {
      result.push(calculateTiming(entry['redirectEnd'], entry['redirectStart']));
      result.push(calculateTiming(entry['domainLookupStart'], entry['fetchStart']));
      result.push(calculateTiming(entry['domainLookupEnd'], entry['domainLookupStart']));

      if (entry['secureConnectionStart'] != null && entry['secureConnectionStart'] > 0) {
        result.push(calculateTiming(entry['secureConnectionStart'], entry['connectStart']));
        result.push(calculateTiming(entry['connectEnd'], entry['secureConnectionStart']));
      } else {
        result.push(calculateTiming(entry['connectEnd'], entry['connectStart']));
        result.push('');
      }

      result.push(calculateTiming(entry['responseStart'], entry['requestStart']));
      result.push(calculateTiming(entry['responseEnd'], entry['responseStart']));
    }

    return result.join(',') // remove empty trailing timings
    .replace(/,+$/, '');
  }

  function calculateTiming(a, b) {
    if (a == null || b == null || // the values being equal indicates for example that a network connection didn't need
    // to be established. Do not report a timing of '0' as this will skew the statistics.
    a === b) {
      return '';
    }

    var diff = Math.round(a - b);

    if (diff < 0) {
      return '';
    }

    return diff;
  }

  var INTERNAL_END_MARKER = '<END>';
  function createTrie() {
    return new Trie();
  }

  function Trie() {
    this.root = {};
  }

  Trie.prototype.addItem = function addItem(key, value) {
    this.insertItem(this.root, key.split(''), 0, value);
    return this;
  };

  Trie.prototype.insertItem = function insertItem(node, keyCharacters, keyCharacterIndex, value) {
    var character = keyCharacters[keyCharacterIndex]; // Characters exhausted, add value to node

    if (character == null) {
      var values = node[INTERNAL_END_MARKER] = node[INTERNAL_END_MARKER] || [];
      values.push(value);
      return;
    }

    var nextNode = node[character] = node[character] || {};
    this.insertItem(nextNode, keyCharacters, keyCharacterIndex + 1, value);
  };

  Trie.prototype.toJs = function toJs(node) {
    node = node || this.root;
    var keys = getKeys(node);

    if (keys.length === 1 && keys[0] === INTERNAL_END_MARKER) {
      return node[INTERNAL_END_MARKER].slice();
    }

    var result = {};

    for (var i = 0, length = keys.length; i < length; i++) {
      var key = keys[i];
      var value = node[key];

      if (key === INTERNAL_END_MARKER) {
        result['$'] = value.slice();
        continue;
      }

      var combinedKeys = key;
      var child = node[key];
      var childKeys = getKeys(child);

      while (childKeys.length === 1 && childKeys[0] !== INTERNAL_END_MARKER) {
        combinedKeys += childKeys[0];
        child = child[childKeys[0]];
        childKeys = getKeys(child);
      }

      result[combinedKeys] = this.toJs(child);
    }

    return result;
  };

  function getKeys(obj) {
    var result = [];

    for (var key in obj) {
      if (hasOwnProperty(obj, key)) {
        result.push(key);
      }
    }

    return result;
  }

  // See https://www.w3.org/TR/hr-time/

  function addResourceTimings(beacon, minStartTime) {
    if (isResourceTimingAvailable && win.JSON) {
      var entries = getEntriesTransferFormat(performance.getEntriesByType('resource'), minStartTime);
      beacon['res'] = win.JSON.stringify(entries);

      if (defaultVars.autoClearResourceTimings && performance.clearResourceTimings) {
        {
          debug('Automatically clearing resource timing buffer.');
        }

        performance.clearResourceTimings();
      }
    } else {
      info('Resource timing not supported.');
    }
  }

  function getEntriesTransferFormat(performanceEntries, minStartTime) {
    var trie = createTrie();
    var lowerCaseReportingUrl = defaultVars.reportingUrl != null ? defaultVars.reportingUrl.toLowerCase() : null;

    for (var i = 0, len = performanceEntries.length; i < len; i++) {
      var entry = performanceEntries[i];

      if (minStartTime != null && entry['startTime'] - defaultVars.highResTimestampReference + defaultVars.referenceTimestamp < minStartTime) {
        continue;
      } else if (entry['duration'] < 0) {
        // Some old browsers do not properly implement resource timing. They report negative durations.
        // Ignore instead of reporting these, as the data isn't usable.
        continue;
      }

      var url = entry.name;
      var lowerCaseUrl = url.toLowerCase();
      var initiatorType = entry['initiatorType'];

      if (lowerCaseUrl === 'about:blank' || lowerCaseUrl.indexOf('javascript:') === 0 || // some iframe cases
      // Data transmission can be visible as a resource. Do not report it.
      lowerCaseReportingUrl != null && lowerCaseUrl.indexOf(lowerCaseReportingUrl) === 0) {
        continue;
      }

      if (url.length > urlMaxLength) {
        url = url.substring(0, urlMaxLength);
      } // We provide more detailed XHR insights via our XHR instrumentation.
      // The XHR instrumentation is available once the initialization was executed
      // (which is completely synchronous).


      if (initiatorType !== 'xmlhttprequest' || entry['startTime'] < defaultVars.highResTimestampReference) {
        trie.addItem(url, serializeEntry(entry));
      }
    }

    return trie.toJs();
  }

  // https://www.w3.org/TR/navigation-timing/

  function getPageLoadStartTimestamp() {
    if (!isTimingAvailable) {
      return defaultVars.initializerExecutionTimestamp;
    } // We don't use navigationStart since that includes unload times for the previous
    // page.


    var timing = performance.timing;
    return timing.fetchStart - (timing.redirectEnd - timing.redirectStart);
  }
  function addTimingToPageLoadBeacon(beacon) {
    if (!isTimingAvailable) {
      // This is our absolute fallback mode where we only have
      // approximations for speed information.
      beacon['ts'] = getPageLoadStartTimestamp() - defaultVars.referenceTimestamp;
      beacon['d'] = Number(event.time) - defaultVars.initializerExecutionTimestamp; // We add this as an extra property to the beacon so that
      // a backend can decide whether it should include timing
      // information in aggregated metrics. Since they are only
      // approximations, this is not always desirable.

      if (!isTimingAvailable) {
        beacon['tim'] = '0';
      }

      return;
    }

    var timing = performance.timing;
    var redirectTime = timing.redirectEnd - timing.redirectStart; // We don't use navigationStart since that includes unload times for the previous page.

    var start = getPageLoadStartTimestamp();
    beacon['ts'] = start - defaultVars.referenceTimestamp; // This can happen when the user aborts the page load. In this case, the load event
    // timing information is not available and will have the default value of "0".

    if (timing.loadEventStart > 0) {
      beacon['d'] = timing.loadEventStart - timing.fetchStart;
    } else {
      beacon['d'] = Number(event.time) - defaultVars.initializerExecutionTimestamp; // We have partial timing information, but since the load was aborted, we will
      // mark it as missing to indicate that the information should be ignored in
      // statistics.

      beacon['tim'] = '0';
    }

    beacon['t_unl'] = timing.unloadEventEnd - timing.unloadEventStart;
    beacon['t_red'] = redirectTime;
    beacon['t_apc'] = timing.domainLookupStart - timing.fetchStart;
    beacon['t_dns'] = timing.domainLookupEnd - timing.domainLookupStart;

    if (timing.secureConnectionStart != null && timing.secureConnectionStart > 0) {
      beacon['t_tcp'] = timing.secureConnectionStart - timing.connectStart;
      beacon['t_ssl'] = timing.connectEnd - timing.secureConnectionStart;
    } else {
      beacon['t_tcp'] = timing.connectEnd - timing.connectStart;
      beacon['t_ssl'] = 0;
    }

    beacon['t_req'] = timing.responseStart - timing.requestStart;
    beacon['t_rsp'] = timing.responseEnd - timing.responseStart;
    beacon['t_dom'] = timing.domContentLoadedEventStart - timing.domLoading;
    beacon['t_chi'] = timing.loadEventEnd - timing.domContentLoadedEventStart;
    beacon['t_bac'] = timing.responseStart - start;
    beacon['t_fro'] = timing.loadEventEnd - timing.responseStart;
    beacon['t_pro'] = timing.loadEventStart - timing.domLoading;
    beacon['t_loa'] = timing.loadEventEnd - timing.loadEventStart;
    addFirstPaintTimings(beacon, start);
  }

  function addFirstPaintTimings(beacon, start) {
    if (!isResourceTimingAvailable) {
      addFirstPaintFallbacks(beacon, start);
      return;
    }

    var paintTimings = performance.getEntriesByType('paint');
    var firstPaintFound = false;

    for (var i = 0; i < paintTimings.length; i++) {
      var paintTiming = paintTimings[i];

      switch (paintTiming.name) {
        case 'first-paint':
          beacon['t_fp'] = paintTiming.startTime | 0;
          firstPaintFound = true;
          break;

        case 'first-contentful-paint':
          beacon['t_fcp'] = paintTiming.startTime | 0;
          break;
      }
    }

    if (!firstPaintFound) {
      addFirstPaintFallbacks(beacon, start);
    }
  }

  function addFirstPaintFallbacks(beacon, start) {
    var firstPaint = null; // Chrome

    if (win.chrome && win.chrome.loadTimes) {
      // Convert to ms
      firstPaint = win.chrome.loadTimes().firstPaintTime * 1000;
    } // IE
    else if (typeof win.performance.timing.msFirstPaint === 'number') {
        firstPaint = win.performance.timing.msFirstPaint;
      } // standard
      else if (typeof win.performance.timing.firstPaint === 'number') {
          firstPaint = win.performance.timing.firstPaint;
        } // First paint may not be available -OR- the browser may have never
    // painted anything and thereby kept this value at 0.


    if (firstPaint != null && firstPaint !== 0) {
      beacon['t_fp'] = Math.round(firstPaint - start);
    }
  }

  /*
   * This file exists to resolve circular dependencies between
   * lib/transmission/index.js -> lib/transmission/batched.js -> lib/hooks/XMLHttpRequest.js -> lib/transmission/index.js
   */

  function disableMonitoringForXMLHttpRequest(xhr) {
    var state = xhr[defaultVars.secretPropertyKey] = xhr[defaultVars.secretPropertyKey] || {};
    state.ignored = true;
  }

  // very easy to parse in a streaming fashion on the server-side. This format is a basic
  // line-based encoding of key/value pairs. Each line contains a key/value pair.
  //
  // In contrast to form encoding, this encoding handles JSON much better.

  function encode(beacons) {
    var str = '';

    for (var i = 0; i < beacons.length; i++) {
      var beacon = beacons[i]; // Multiple beacons are separated by an empty line

      str += '\n';

      for (var key in beacon) {
        if (hasOwnProperty(beacon, key)) {
          var value = beacon[key];

          if (value != null) {
            str += '\n' + encodePart(key) + '\t' + encodePart(value);
          }
        }
      }
    }

    return str.substring(2);
  }

  function encodePart(part) {
    return String(part).replace(/\\/g, '\\\\').replace(/\n/g, '\\n').replace(/\t/g, '\\t');
  }

  var pendingBeacons = [];
  var pendingBeaconTransmittingTimeout;
  var isVisibilityApiSupported = typeof doc.visibilityState === 'string';
  var isSupported = !!XMLHttpRequest && isVisibilityApiSupported && isSendBeaconApiSupported();
  function isEnabled() {
    return isSupported && defaultVars.beaconBatchingTime > 0;
  } // We attempt batching of messages to be more efficient on the client, network and
  // server-side. While the connection is either a persistent HTTP 2 connection or
  // a HTTP 1.1 connection with keep-alive, there is still some overhead involved
  // in having many small messages.
  //
  // For this reason we attempt batching. When batching we must be careful to
  // force a transmission when the document is unloaded. To handle the variety
  // of unload cases, we are making use of the Page Visibility API. This abstracts
  // all unload cases.

  if (isSupported) {
    addEventListener(doc, 'visibilitychange', function () {
      if (isWindowHidden()) {
        transmit();
      }
    }); // According to the spec visibilitychange should be a replacement for
    // beforeunload, but the reality is different (as of 2019-04-17). Chrome will
    // close tabs without firing visibilitychange. beforeunload on the other hand
    // is fired.

    addEventListener(win, 'beforeunload', transmit);
  }

  function sendBeacon$1(beacon) {
    pendingBeacons.push(beacon); // We cannot guarantee that we will ever get time to transmit data in a batched
    // format when the window is hidden, as this might occur while the document is
    // being unloaded. Immediately force a transmission in these cases.

    if (!isWindowHidden() && defaultVars.beaconBatchingTime > 0) {
      if (pendingBeaconTransmittingTimeout == null) {
        pendingBeaconTransmittingTimeout = setTimeout(transmit, defaultVars.beaconBatchingTime);
      }
    } else {
      transmit();
    }
  }

  function transmit() {
    if (pendingBeaconTransmittingTimeout != null) {
      clearTimeout(pendingBeaconTransmittingTimeout);
      pendingBeaconTransmittingTimeout = null;
    }

    if (pendingBeacons.length === 0) {
      return;
    }

    var serializedBeacons = encode(pendingBeacons); // clear the array

    pendingBeacons.length = 0; // Empty beacons. Should never happen, but better be safe.

    if (serializedBeacons.length === 0) {
      return;
    } // This will transmit a text/plain;charset=UTF-8 content type. This may not be what we
    // want, but changing the content type via the Blob constructor currently
    // breaks for cross-origin requests.
    // https://bugs.chromium.org/p/chromium/issues/detail?id=490015


    var sendBeaconState = isSendBeaconApiSupported() && sendBeacon(String(defaultVars.reportingUrl), serializedBeacons); // There are limits to the amount of data transmittable via the sendBeacon API.
    // If it doesn't work via the sendBeacon, try it via plain old AJAX APIs
    // as a last resort.

    if (sendBeaconState === false) {
      var xhr = new XMLHttpRequest();
      disableMonitoringForXMLHttpRequest(xhr);
      xhr.open('POST', String(defaultVars.reportingUrl), true);
      xhr.setRequestHeader('Content-type', 'text/plain;charset=UTF-8'); // Ensure that browsers do not try to automatically parse the response.

      xhr.responseType = 'text';
      xhr.timeout = defaultVars.xhrTransmissionTimeout;
      xhr.send(serializedBeacons);
    }
  }

  function isWindowHidden() {
    return doc.visibilityState !== 'visible';
  }

  function isSendBeaconApiSupported() {
    return typeof nav.sendBeacon === 'function';
  }

  function createExcessiveUsageIdentifier(opts) {
    var maxCalls = opts.maxCalls || 512;
    var maxCallsPerTenSeconds = opts.maxCallsPerTenSeconds || 32;
    var totalCalls = 0;
    var totalCallsInLastTenSeconds = 0;
    setInterval(function () {
      totalCallsInLastTenSeconds = 0;
    }, 10000);
    return function isExcessiveUsage() {
      return ++totalCalls > maxCalls || ++totalCallsInLastTenSeconds > maxCallsPerTenSeconds;
    };
  }

  function sendBeacon$2(data) {
    var str = stringify(data);

    if (str.length === 0) {
      return;
    }

    if (XMLHttpRequest && str.length > defaultVars.maxLengthForImgRequest) {
      var xhr = new XMLHttpRequest();
      disableMonitoringForXMLHttpRequest(xhr);
      xhr.open('POST', String(defaultVars.reportingUrl), true);
      xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded;charset=UTF-8'); // Ensure that browsers do not try to automatically parse the response.

      xhr.responseType = 'text';
      xhr.timeout = defaultVars.xhrTransmissionTimeout;
      xhr.send(str);
    } else {
      // Older browsers do not support the XMLHttpRequest API. This sucks and may
      // result in a variety of issues, e.g. URL length restrictions. "Luckily", older
      // browsers also lack support for advanced features such as resource timing.
      // This should make this transmission via a GET request possible.
      executeImageRequest(String(defaultVars.reportingUrl) + '?' + str);
    }
  }

  function stringify(data) {
    var str = '';

    for (var key in data) {
      if (hasOwnProperty(data, key)) {
        var value = data[key];

        if (value != null) {
          str += '&' + encodeURIComponent(key) + '=' + encodeURIComponent(String(data[key]));
        }
      }
    }

    return str.substring(1);
  }

  var isExcessiveUsage = createExcessiveUsageIdentifier({
    maxCalls: 1024,
    maxCallsPerTenSeconds: 128
  });
  function sendBeacon$3(data) {
    {
      info('Transmitting beacon', data);
    }

    if (isExcessiveUsage()) {
      {
        info('Reached the maximum number of beacons to transmit.');
      }

      return;
    }

    try {
      if (isEnabled()) {
        sendBeacon$1(data);
      } else {
        sendBeacon$2(data);
      }
    } catch (e) {
      {
        error('Failed to transmit beacon', e);
      }
    }
  }

  var pageLoadBeaconTransmitted = false;
  var state$1 = {
    onEnter: function () {
      if (!pageLoadBeaconTransmitted) {
        pageLoadBeaconTransmitted = true;
        sendPageLoadBeacon();
      }
    },
    getActiveTraceId: function () {
      return null;
    },
    getActivePhase: function () {
      return undefined;
    },
    triggerManualPageLoad: function () {
      {
        warn('Page load triggered, but page is already considered as loaded. Did you mark it as loaded more than once?');
      }
    }
  };

  function sendPageLoadBeacon() {
    // $FlowFixMe: Find a way to define all properties beforehand so that flow doesn't complain about missing props.
    var beacon = {
      'ty': 'pl'
    };
    addCommonBeaconProperties(beacon);
    beacon['t'] = defaultVars.pageLoadTraceId;
    beacon['bt'] = defaultVars.pageLoadBackendTraceId;
    beacon['u'] = win.location.href;
    beacon['ph'] = pageLoad;
    addTimingToPageLoadBeacon(beacon);
    addResourceTimings(beacon);
    sendBeacon$3(beacon);
  }

  var dataUrlPrefix = 'data:';
  var ignorePingsRegex = /.*\/ping(\/?$|\?.*)/i;
  function isUrlIgnored(url) {
    if (!url) {
      return true;
    } // Force string conversion. During runtime we have seen that some URLs passed into this code path aren't actually
    // strings. Reason currently unknown.


    url = String(url);

    if (!url) {
      return true;
    } // We never want to track data URLs. Instead of matching these via regular expressions (which might be expensive),
    // we are explicitly doing a startsWith ignore case check
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URIs


    if (url.substring == null || url.substring(0, dataUrlPrefix.length).toLowerCase() === dataUrlPrefix) {
      return true;
    }

    if (defaultVars.ignorePings && ignorePingsRegex.test(url)) {
      return true;
    }

    return matchesAny(defaultVars.ignoreUrls, url);
  }
  function isErrorMessageIgnored(message) {
    return !message || matchesAny(defaultVars.ignoreErrorMessages, message);
  }

  var maxErrorsToReport = 100;
  var maxStackSize = 30;
  var reportedErrors = 0;
  var maxSeenErrorsTracked = 20;
  var numberOfDifferentErrorsSeen = 0;
  var seenErrors = {};
  var scheduledTransmissionTimeoutHandle; // We are wrapping global listeners. In these, we are catching and rethrowing errors.
  // In older browsers, rethrowing errors actually manipulates the error objects. As a
  // result, it is not possible to just mark an error as reported. The simplest way to
  // avoid double reporting is to temporarily disable the global onError handler…

  var ignoreNextOnError = false;
  function ignoreNextOnErrorEvent() {
    ignoreNextOnError = true;
  }
  function hookIntoGlobalErrorEvent() {
    var globalOnError = win.onerror;

    win.onerror = function (message, fileName, lineNumber, columnNumber, error) {
      if (ignoreNextOnError) {
        ignoreNextOnError = false;

        if (typeof globalOnError === 'function') {
          return globalOnError.apply(this, arguments);
        }

        return;
      }

      var stack = error && error.stack;

      if (!stack) {
        stack = 'at ' + fileName + ' ' + lineNumber;

        if (columnNumber != null) {
          stack += ':' + columnNumber;
        }
      }

      onUnhandledError(message, stack);

      if (typeof globalOnError === 'function') {
        return globalOnError.apply(this, arguments);
      }
    };
  }
  function reportError(error, opts) {
    if (!error) {
      return;
    }

    if (typeof error === 'string') {
      onUnhandledError(error, '', opts);
    } else {
      onUnhandledError(error['message'], error['stack'], opts);
    }
  }

  function onUnhandledError(message, stack, opts) {
    if (!message || reportedErrors > maxErrorsToReport) {
      return;
    }

    if (isErrorMessageIgnored(message)) {
      return;
    }

    if (numberOfDifferentErrorsSeen >= maxSeenErrorsTracked) {
      seenErrors = {};
      numberOfDifferentErrorsSeen = 0;
    }

    message = String(message).substring(0, 300);
    stack = shortenStackTrace(stack);
    var location = win.location.href;
    var parentId = getActiveTraceId();
    var key = message + stack + location + (parentId || '');
    var trackedError = seenErrors[key];

    if (trackedError) {
      trackedError.seenCount++;
    } else {
      var componentStack = undefined;

      if (opts && opts['componentStack']) {
        componentStack = String(opts['componentStack']).substring(0, 4096);
      }

      trackedError = seenErrors[key] = {
        message: message,
        stack: stack,
        componentStack: componentStack,
        location: location,
        parentId: parentId,
        seenCount: 1,
        transmittedCount: 0
      };
      numberOfDifferentErrorsSeen++;
    }

    scheduleTransmission();
  }

  function shortenStackTrace(stack) {
    return String(stack || '').split('\n').slice(0, maxStackSize).join('\n');
  }

  function scheduleTransmission() {
    if (scheduledTransmissionTimeoutHandle) {
      return;
    }

    scheduledTransmissionTimeoutHandle = setTimeout(send, 1000);
  }

  function send() {
    clearTimeout(scheduledTransmissionTimeoutHandle);
    scheduledTransmissionTimeoutHandle = null;

    for (var _key in seenErrors) {
      if (seenErrors.hasOwnProperty(_key)) {
        var seenError = seenErrors[_key];

        if (seenError.seenCount > seenError.transmittedCount) {
          sendBeaconForError(seenError);
          reportedErrors++;
        }
      }
    }

    seenErrors = {};
    numberOfDifferentErrorsSeen = 0;
  }

  function sendBeaconForError(error) {
    var spanId = generateUniqueId();
    var traceId = error.parentId || spanId; // $FlowFixMe

    var beacon = {
      'ty': 'err',
      's': spanId,
      't': traceId,
      'ts': now(),
      // error beacon specific data
      'l': error.location,
      'e': error.message,
      'st': error.stack,
      'cs': error.componentStack,
      'c': error.seenCount - error.transmittedCount
    };
    addCommonBeaconProperties(beacon);
    sendBeacon$3(beacon);
  }

  var messagePrefix = 'Unhandled promise rejection: ';
  var stackUnavailableMessage = '<unavailable because Promise wasn\'t rejected with an Error object>';
  function hookIntoGlobalUnhandledRejectionEvent() {
    if (typeof win.addEventListener === 'function') {
      win.addEventListener('unhandledrejection', onUnhandledRejection);
    }
  }
  function onUnhandledRejection(event) {
    if (event.reason == null) {
      reportError({
        message: messagePrefix + '<no reason defined>',
        stack: stackUnavailableMessage
      });
    } else if (typeof event.reason.message === 'string') {
      reportError({
        message: messagePrefix + event.reason.message,
        stack: typeof event.reason.stack === 'string' ? event.reason.stack : stackUnavailableMessage
      });
    } else if (typeof event.reason !== 'object') {
      reportError({
        message: messagePrefix + event.reason,
        stack: stackUnavailableMessage
      });
    }
  }

  function isWhitelistedOrigin(url) {
    return matchesAny(defaultVars.whitelistedOrigins, url);
  }

  var maximumHttpRequestUrlLength = 4096; // Asynchronously created a tag.

  var urlAnalysisElement = null;

  try {
    urlAnalysisElement = document.createElement('a');
  } catch (e) {
    {
      debug('Failed to create URL analysis element. Will not be able to normalize URLs.', e);
    }
  }

  function normalizeUrl(url) {
    if (urlAnalysisElement) {
      try {
        // "a"-elements normalize the URL when setting a relative URL or URLs
        // that are missing a scheme
        urlAnalysisElement.href = url;
        url = urlAnalysisElement.href;
      } catch (e) {
        {
          debug('Failed to normalize URL' + url);
        }
      }
    }

    if (url.length > maximumHttpRequestUrlLength) {
      url = url.substring(0, maximumHttpRequestUrlLength);
    }

    return url;
  }

  // document.createElement('a')

  var urlAnalysisElement$1 = null;
  var documentOriginAnalysisElement = null;

  try {
    urlAnalysisElement$1 = document.createElement('a');
    documentOriginAnalysisElement = document.createElement('a');
    documentOriginAnalysisElement.href = win.location.href;
  } catch (e) {
    {
      debug('Failed to create URL analysis elements. Will not be able to execute same-origin check, i.e. all same-origin checks will fail.', e);
    }
  }

  function isSameOrigin(url) {
    if (!urlAnalysisElement$1 || !documentOriginAnalysisElement) {
      return false;
    }

    try {
      urlAnalysisElement$1.href = url;
      return (// Most browsers support this fallback logic out of the box. Not so the Internet explorer.
        // To make it work in Internet explorer, we need to add the fallback manually.
        // IE 9 uses a colon as the protocol when no protocol is defined
        (urlAnalysisElement$1.protocol && urlAnalysisElement$1.protocol !== ':' ? urlAnalysisElement$1.protocol : documentOriginAnalysisElement.protocol) === documentOriginAnalysisElement.protocol && (urlAnalysisElement$1.hostname || documentOriginAnalysisElement.hostname) === documentOriginAnalysisElement.hostname && (urlAnalysisElement$1.port || documentOriginAnalysisElement.port) === documentOriginAnalysisElement.port
      );
    } catch (e) {
      return false;
    }
  }

  var isExcessiveUsage$1 = createExcessiveUsageIdentifier({
    maxCalls: 512,
    maxCallsPerTenSeconds: 32
  }); // In addition to the common HTTP status codes, a bunch of
  // additional outcomes are possible. Mainly errors, the following
  // status codes denote internal codes which are used for beacons
  // to describe the XHR result.

  var additionalStatuses = {
    // https://xhr.spec.whatwg.org/#the-timeout-attribute
    timeout: -100,
    // Used when the request is aborted:
    // https://xhr.spec.whatwg.org/#the-abort()-method
    abort: -101,
    // Errors may occur when opening an XHR object for a variety of
    // reasons.
    // https://xhr.spec.whatwg.org/#the-open()-method
    openError: -102,
    // Non-HTTP errors, e.g. failed to establish connection.
    // https://xhr.spec.whatwg.org/#events
    error: -103
  };
  var traceIdHeaderRegEx = /^X-INSTANA-T$/i;
  function instrumentXMLHttpRequest() {
    if (!XMLHttpRequest || !new XMLHttpRequest().addEventListener) {
      {
        info('Browser does not support the features required for XHR instrumentation.');
      }

      return;
    }

    var originalOpen = XMLHttpRequest.prototype.open;
    var originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;
    var originalSend = XMLHttpRequest.prototype.send;

    XMLHttpRequest.prototype.open = function open(method, url, async) {
      var xhr = this;

      if (isExcessiveUsage$1()) {
        {
          info('Reached the maximum number of XMLHttpRequests to monitor.');
        }

        return originalOpen.apply(xhr, arguments);
      }

      var state = xhr[defaultVars.secretPropertyKey] = xhr[defaultVars.secretPropertyKey] || {}; // probably ignored due to disableMonitoringForXMLHttpRequest calls

      if (state.ignored) {
        return originalOpen.apply(xhr, arguments);
      }

      state.ignored = isUrlIgnored(url);

      if (state.ignored) {
        {
          debug('Not generating XHR beacon because it should be ignored according to user configuration. URL: ' + url);
        }

        return originalOpen.apply(xhr, arguments);
      }

      state.spanAndTraceId = generateUniqueId();
      state.setBackendCorrelationHeaders = isSameOrigin(url) || isWhitelistedOrigin(url);
      state.sampled = Math.random() <= defaultVars.sampleRate ? 1 : 0; // $FlowFixMe: Some properties deliberately left our for js file size reasons.

      var beacon = {
        'ty': 'xhr',
        // general beacon data
        't': state.spanAndTraceId,
        's': state.spanAndTraceId,
        'ts': 0,
        'd': 0,
        // xhr beacon specific data
        // 's': '',
        'l': win.location.href,
        'm': method,
        'u': normalizeUrl(url),
        'a': async === undefined || async ? 1 : 0,
        'sp': state.sampled,
        'st': 0,
        'e': undefined,
        'bc': state.setBackendCorrelationHeaders ? 1 : 0
      };
      state.beacon = beacon;

      try {
        var result = originalOpen.apply(xhr, arguments);
        xhr.addEventListener('timeout', onTimeout);
        xhr.addEventListener('error', onError);
        xhr.addEventListener('abort', onAbort);
        xhr.addEventListener('readystatechange', onReadystatechange);
        return result;
      } catch (e) {
        beacon['ts'] = now() - defaultVars.referenceTimestamp;
        beacon['st'] = additionalStatuses.openError;
        beacon['e'] = e.message;
        addCommonBeaconProperties(beacon);
        sendBeacon$3(beacon);
        xhr[defaultVars.secretPropertyKey] = null;
        throw e;
      }

      function onFinish(status) {
        if (state.ignored) {
          return;
        }

        if (beacon['st'] !== 0) {
          // Multiple finish events. Should only happen when we setup the event handlers
          // in a wrong way or when the XHR object is reused. We don't support this use
          // case.
          return;
        }

        beacon['st'] = status; // When accessing object properties as object['property'] instead of
        // object.property flow does not know the type and assumes string.
        // Arithmetic operations like addition are only allowed on numbers. OTOH,
        // we can not safely use beacon.property as the compilation/minification
        // step will rename the properties which results in JSON payloads with
        // wrong property keys.
        // $FlowFixMe: see above

        beacon['d'] = Math.max(0, now() - (beacon['ts'] + defaultVars.referenceTimestamp));
        sendBeacon$3(beacon);
      }

      function onTimeout() {
        onFinish(additionalStatuses.timeout);
      }

      function onError(e) {
        if (state.ignored) {
          return;
        }

        var message = e && (e.error && e.error.message || e.message);

        if (typeof message === 'string') {
          beacon['e'] = message.substring(0, 300);
        }

        onFinish(additionalStatuses.error);
      }

      function onAbort() {
        onFinish(additionalStatuses.abort);
      }

      function onReadystatechange() {
        if (xhr.readyState === 4) {
          var status;

          try {
            status = xhr.status;
          } catch (e) {
            // IE 9 will throw errors when trying to access the status property
            // on aborted requests and timeouts. We can swallow the error
            // since we have separate event listeners for these types of
            // situations.
            onFinish(additionalStatuses.error);
            return;
          }

          if (status !== 0) {
            onFinish(status);
          }
        }
      }
    };

    XMLHttpRequest.prototype.setRequestHeader = function setRequestHeader(header) {
      var state = this[defaultVars.secretPropertyKey]; // If this request was initiated by a fetch polyfill, the Instana headers
      // will be set before xhr.send is called (by the fetch polyfill,
      // translating the headers from the request definition object into
      // XHR.setRequestHeader calls). We need to keep track of this so we can
      // set this XHR to ignored in xhr.send.

      if (state && traceIdHeaderRegEx.test(header)) {
        {
          debug('Not generating XHR beacon because correlation header is already set (possibly fetch polyfill applied).');
        }

        state.ignored = true;
      }

      return originalSetRequestHeader.apply(this, arguments);
    };

    XMLHttpRequest.prototype.send = function send() {
      var state = this[defaultVars.secretPropertyKey];

      if (!state || state.ignored) {
        return originalSend.apply(this, arguments);
      }

      if (state.setBackendCorrelationHeaders) {
        originalSetRequestHeader.call(this, 'X-INSTANA-T', state.spanAndTraceId);
        originalSetRequestHeader.call(this, 'X-INSTANA-S', state.spanAndTraceId);
        originalSetRequestHeader.call(this, 'X-INSTANA-L', state.sampled);
      }

      state.beacon['ts'] = now() - defaultVars.referenceTimestamp;
      addCommonBeaconProperties(state.beacon);
      return originalSend.apply(this, arguments);
    };
  }

  function getPageLoadBackendTraceId() {
    if (!isResourceTimingAvailable) {
      return null;
    }

    var entries = performance.getEntriesByType('navigation');

    for (var i = 0; i < entries.length; i++) {
      var entry = entries[i];

      if (entry['serverTiming'] != null) {
        for (var j = 0; j < entry['serverTiming'].length; j++) {
          var serverTiming = entry['serverTiming'][j];

          if (serverTiming['name'] === defaultVars.serverTimingBackendTraceIdEntryName) {
            {
              info('Found page load backend trace ID %s in Server-Timing header.', serverTiming['description']);
            }

            return serverTiming['description'];
          }
        }
      }
    }

    return null;
  }

  // https://github.com/facebook/flow/blob/master/lib/dom.js

  // Asynchronous function wrapping: The process of wrapping a listener which goes into one function, e.g.
  //
  //  - EventTarget#addEventListener
  //  - EventEmitter#on
  //
  // and is removed via another function, e.g.
  //
  //  - EventTarget#removeEventListener
  //  - EventEmitter#off
  //
  // What is complicated about this, is that these methods identify registered listeners by function reference.
  // When we wrap a function, we naturally change the reference. We must therefore keep track of which
  // original function belongs to what wrapped function.
  //
  // This file provides helpers that help in the typical cases. It is removed from all browser specific APIs
  // in order to allow simple unit test execution.
  //
  // Note that this file follows the behavior outlined in DOM specification. Among others, this means that it is not
  // possible to register the same listener twice.
  // http://dom.spec.whatwg.org
  function addWrappedFunction(storageTarget, wrappedFunction, valuesForEqualityCheck) {
    var storage = storageTarget[defaultVars.wrappedEventHandlersOriginalFunctionStorageKey] = storageTarget[defaultVars.wrappedEventHandlersOriginalFunctionStorageKey] || [];
    var index = findInStorage(storageTarget, valuesForEqualityCheck);

    if (index !== -1) {
      // already registered. Do not allow re-registration
      return storage[index].wrappedFunction;
    }

    storage.push({
      wrappedFunction: wrappedFunction,
      valuesForEqualityCheck: valuesForEqualityCheck
    });
    return wrappedFunction;
  }

  function findInStorage(storageTarget, valuesForEqualityCheck) {
    var storage = storageTarget[defaultVars.wrappedEventHandlersOriginalFunctionStorageKey];

    for (var i = 0; i < storage.length; i++) {
      var storageItem = storage[i];

      if (matchesEqualityCheck(storageItem.valuesForEqualityCheck, valuesForEqualityCheck)) {
        return i;
      }
    }

    return -1;
  }

  function popWrappedFunction(storageTarget, valuesForEqualityCheck, fallback) {
    var storage = storageTarget[defaultVars.wrappedEventHandlersOriginalFunctionStorageKey];

    if (storage == null) {
      return fallback;
    }

    var index = findInStorage(storageTarget, valuesForEqualityCheck);

    if (index === -1) {
      return fallback;
    }

    var storageItem = storage[index];
    storage.splice(index, 1);
    return storageItem.wrappedFunction;
  }

  function matchesEqualityCheck(valuesForEqualityCheckA, valuesForEqualityCheckB) {
    if (valuesForEqualityCheckA.length !== valuesForEqualityCheckB.length) {
      return false;
    }

    for (var i = 0; i < valuesForEqualityCheckA.length; i++) {
      if (valuesForEqualityCheckA[i] !== valuesForEqualityCheckB[i]) {
        return false;
      }
    }

    return true;
  }

  function addWrappedDomEventListener(storageTarget, wrappedFunction, eventName, eventListener, optionsOrCapture) {
    return addWrappedFunction(storageTarget, wrappedFunction, getDomEventListenerValuesForEqualityCheck(eventName, eventListener, optionsOrCapture));
  }

  function getDomEventListenerValuesForEqualityCheck(eventName, eventListener, optionsOrCapture) {
    return [eventName, eventListener, getDomEventListenerCaptureValue(optionsOrCapture)];
  }

  function getDomEventListenerCaptureValue(optionsOrCapture) {
    // > Let capture, passive, and once be the result of flattening more options.
    // https://dom.spec.whatwg.org/#dom-eventtarget-addeventlistener
    //
    // > To flatten more options, run these steps:
    // > 1. Let capture be the result of flattening options.
    // https://dom.spec.whatwg.org/#event-flatten-more
    //
    // > To flatten options, run these steps:
    // > 1. If options is a boolean, then return options.
    // > 2. Return options’s capture.
    // https://dom.spec.whatwg.org/#concept-flatten-options
    //
    // > dictionary EventListenerOptions {
    // >   boolean capture = false;
    // > };
    // https://dom.spec.whatwg.org/#dom-eventlisteneroptions-capture
    if (optionsOrCapture == null) {
      return false;
    } else if (typeof optionsOrCapture === 'object') {
      return Boolean(optionsOrCapture.capture);
    }

    return Boolean(optionsOrCapture);
  }
  function popWrappedDomEventListener(storageTarget, eventName, eventListener, optionsOrCapture, fallback) {
    return popWrappedFunction(storageTarget, getDomEventListenerValuesForEqualityCheck(eventName, eventListener, optionsOrCapture), fallback);
  }

  function wrapEventHandlers() {
    if (defaultVars.wrapEventHandlers) {
      wrapEventTarget(win.EventTarget);
    }
  }

  function wrapEventTarget(EventTarget) {
    if (!EventTarget || typeof EventTarget.prototype.addEventListener !== 'function' || typeof EventTarget.prototype.removeEventListener !== 'function') {
      return;
    }

    var originalAddEventListener = EventTarget.prototype.addEventListener;
    var originalRemoveEventListener = EventTarget.prototype.removeEventListener;

    EventTarget.prototype.addEventListener = function wrappedAddEventListener(eventName, fn, optionsOrCapture) {
      if (typeof fn !== 'function') {
        return originalAddEventListener.apply(this, arguments);
      } // non-deopt arguments copy


      var args = new Array(arguments.length);

      for (var i = 0; i < arguments.length; i++) {
        args[i] = arguments[i];
      }

      args[1] = function wrappedEventListener() {
        try {
          return fn.apply(this, arguments);
        } catch (e) {
          reportError(e);
          ignoreNextOnErrorEvent();
          throw e;
        }
      };

      args[1] = addWrappedDomEventListener(this, args[1], eventName, fn, optionsOrCapture);
      return originalAddEventListener.apply(this, args);
    };

    EventTarget.prototype.removeEventListener = function wrappedRemoveEventListener(eventName, fn, optionsOrCapture) {
      if (typeof fn !== 'function') {
        return originalRemoveEventListener.apply(this, arguments);
      } // non-deopt arguments copy


      var args = new Array(arguments.length);

      for (var i = 0; i < arguments.length; i++) {
        args[i] = arguments[i];
      }

      args[1] = popWrappedDomEventListener(this, eventName, fn, optionsOrCapture, fn);
      return originalRemoveEventListener.apply(this, args);
    };
  }

  var isExcessiveUsage$2 = createExcessiveUsageIdentifier({
    maxCalls: 64,
    maxCallsPerTenSeconds: 32
  });
  function observeCspViolations() {
    if (defaultVars.collectCspViolations) {
      addEventListener(doc, 'securitypolicyviolation', onViolation);
    }
  }

  function onViolation(e) {
    if (isExcessiveUsage$2()) {
      {
        info('Reached the maximum number of Content-Security Policy violations to monitor.');
      }

      return;
    } // $FlowFixMe: Some properties deliberately left our for js file size reasons.


    var beacon = {
      'ty': 'csp',
      'ts': now(),
      'l': win.location.href,
      // $FlowFixMe
      'bu': e['blockedURI'],
      // $FlowFixMe
      'ed': e['effectiveDirective'],
      // $FlowFixMe
      'vd': e['violatedDirective'],
      // $FlowFixMe
      'op': e['originalPolicy'],
      // $FlowFixMe
      'di': e['disposition'],
      // $FlowFixMe
      'st': e['statusCode'],
      // $FlowFixMe
      'sa': e['sample'],
      // $FlowFixMe
      'sf': e['sourceFile']
    }; // $FlowFixMe

    if (e['lineNumber']) {
      // $FlowFixMe
      beacon['ln'] = e['lineNumber'];
    } // $FlowFixMe


    if (e['columnNumber']) {
      // $FlowFixMe
      beacon['cn'] = e['columnNumber'];
    }

    addCommonBeaconProperties(beacon);
    sendBeacon$3(beacon);
  }

  var isExcessiveUsage$3 = createExcessiveUsageIdentifier({
    maxCalls: 512,
    maxCallsPerTenSeconds: 32
  });
  function instrumentFetch() {
    if (!win.fetch || !win.Request) {
      {
        info('Browser does not support the Fetch API.');
      }

      return;
    }

    win.fetch = function (input, init) {
      var request = new Request(input, init);

      if (isExcessiveUsage$3()) {
        {
          info('Reached the maximum number of fetch calls to monitor.');
        }

        return originalFetch(request);
      }

      var url = request.url;

      if (isUrlIgnored(url)) {
        {
          debug('Not generating XHR beacon for fetch call because it is to be ignored according to user configuration. URL: ' + url);
        }

        return originalFetch(request);
      } // $FlowFixMe: Some properties deliberately left our for js file size reasons.


      var beacon = {
        'ty': 'xhr',
        // 't': '',
        'ts': now() - defaultVars.referenceTimestamp,
        'd': 0,
        // xhr beacon specific data
        // 's': '',
        'l': win.location.href,
        'm': '',
        'u': '',
        'a': 1,
        'st': 0,
        'e': undefined
      };
      addCommonBeaconProperties(beacon);
      var spanAndTraceId = generateUniqueId();
      var setBackendCorrelationHeaders = isSameOrigin(url) || isWhitelistedOrigin(url);
      beacon['t'] = spanAndTraceId;
      beacon['s'] = spanAndTraceId;
      beacon['m'] = request.method;
      beacon['u'] = normalizeUrl(url);
      beacon['a'] = 1;
      beacon['bc'] = setBackendCorrelationHeaders ? 1 : 0;

      if (setBackendCorrelationHeaders) {
        request.headers.append('X-INSTANA-T', spanAndTraceId);
        request.headers.append('X-INSTANA-S', spanAndTraceId);
        request.headers.append('X-INSTANA-L', '1');
      }

      return originalFetch(request).then(function (response) {
        beacon['st'] = response.status; // When accessing object properties as object['property'] instead of
        // object.property flow does not know the type and assumes string.
        // Arithmetic operations like addition are only allowed on numbers. OTOH,
        // we can not safely use beacon.property as the compilation/minification
        // step will rename the properties which results in JSON payloads with
        // wrong property keys.
        // $FlowFixMe: see above

        beacon['d'] = now() - (beacon['ts'] + defaultVars.referenceTimestamp);
        sendBeacon$3(beacon);
        return response;
      }).catch(function (e) {
        // $FlowFixMe: see above
        beacon['d'] = now() - (beacon['ts'] + defaultVars.referenceTimestamp);
        beacon['e'] = e.message;
        beacon['st'] = -103;
        sendBeacon$3(beacon);
        throw e;
      });
    };
  }

  function reportCustomEvent(eventName, opts) {
    var traceId = getActiveTraceId();
    var spanId = generateUniqueId();

    if (!traceId) {
      traceId = spanId;
    } // $FlowFixMe: Some properties deliberately left our for js file size reasons.


    var beacon = {
      'ty': 'cus',
      's': spanId,
      't': traceId,
      'ts': now(),
      'n': eventName,
      'l': win.location.href
    };
    addCommonBeaconProperties(beacon);

    if (opts) {
      enrich(beacon, opts);
    }

    sendBeacon$3(beacon);
  }

  function enrich(beacon, opts) {
    if (opts['meta']) {
      addMetaDataToBeacon(beacon, opts['meta']);
    }

    if (typeof opts['duration'] === 'number') {
      beacon['d'] = opts['duration'];
    }

    if (typeof opts['backendTraceId'] === 'string') {
      beacon['bt'] = opts['backendTraceId'].substring(0, 64);
    }

    if (opts['error']) {
      beacon['e'] = String(opts['error']['message']).substring(0, 300);
      beacon['st'] = shortenStackTrace(opts['error']['stack']);
    }

    if (typeof opts['componentStack'] === 'string') {
      beacon['cs'] = opts['componentStack'].substring(0, 4096);
    }
  }

  var isExcessiveUsage$4 = createExcessiveUsageIdentifier({
    maxCalls: 512,
    maxCallsPerTenSeconds: 32
  });
  function setPage(page) {
    var pageChanged = defaultVars.page !== page;
    defaultVars.page = page;

    if (pageChanged) {
      if (isExcessiveUsage$4()) {
        {
          info('Reached the maximum number of page changes to monitor.');
        }
      } else {
        reportPageChange();
      }
    }
  }

  function reportPageChange() {
    // $FlowFixMe: Some properties deliberately left our for js file size reasons.
    var beacon = {
      'ty': 'pc',
      'ts': now(),
      'l': win.location.href
    };
    addCommonBeaconProperties(beacon);
    sendBeacon$3(beacon);
  }

  function processCommand(command) {
    switch (command[0]) {
      case 'apiKey':
        defaultVars.apiKey = command[1];
        break;

      case 'key':
        defaultVars.apiKey = command[1];
        break;

      case 'reportingUrl':
        defaultVars.reportingUrl = command[1];
        break;

      case 'meta':
        defaultVars.meta[command[1]] = command[2];
        break;

      case 'traceId':
        defaultVars.pageLoadBackendTraceId = command[1];
        break;

      case 'ignoreUrls':
        {
          validateRegExpArray('ignoreUrls', command[1]);
        }

        defaultVars.ignoreUrls = command[1];
        break;

      case 'ignoreErrorMessages':
        {
          validateRegExpArray('ignoreErrorMessages', command[1]);
        }

        defaultVars.ignoreErrorMessages = command[1];
        break;

      case 'whitelistedOrigins':
        {
          validateRegExpArray('whitelistedOrigins', command[1]);
        }

        defaultVars.whitelistedOrigins = command[1];
        break;

      case 'manualPageLoadEvent':
        defaultVars.manualPageLoadEvent = true;
        break;

      case 'triggerPageLoad':
        defaultVars.manualPageLoadTriggered = true;
        triggerManualPageLoad();
        break;

      case 'xhrTransmissionTimeout':
        defaultVars.xhrTransmissionTimeout = command[1];
        break;

      case 'autoClearResourceTimings':
        defaultVars.autoClearResourceTimings = command[1];
        break;

      case 'page':
        setPage(command[1]);
        break;

      case 'ignorePings':
        defaultVars.ignorePings = command[1];
        break;

      case 'reportError':
        reportError(command[1], command[2]);
        break;

      case 'wrapEventHandlers':
        defaultVars.wrapEventHandlers = command[1];
        break;

      case 'wrapTimers':
        defaultVars.wrapTimers = command[1];
        break;

      case 'sampleRate':
        defaultVars.sampleRate = command[1];
        break;

      case 'getPageLoadId':
        return defaultVars.pageLoadTraceId;

      case 'user':
        if (command[1]) {
          defaultVars.userId = String(command[1]).substring(0, 128);
        }

        if (command[2]) {
          defaultVars.userName = String(command[2]).substring(0, 128);
        }

        if (command[3]) {
          defaultVars.userEmail = String(command[3]).substring(0, 128);
        }

        break;

      case 'reportEvent':
        reportCustomEvent(command[1], command[2]);
        break;

      case 'beaconBatchingTime':
        defaultVars.beaconBatchingTime = command[1];
        break;

      case 'collectCspViolations':
        defaultVars.collectCspViolations = command[1];
        break;

      case 'maxLengthForImgRequest':
        defaultVars.maxLengthForImgRequest = command[1];
        break;

      case 'disableResourceTimingTransmission':
        defaultVars.disableResourceTimingTransmission = command[1];
        break;

      default:
        {
          warn('Unsupported command: ' + command[0]);
        }

        break;
    }
  }

  function validateRegExpArray(name, arr) {
    if (!(arr instanceof Array)) {
      return warn(name + ' is not an array. This will result in errors.');
    }

    for (var i = 0, len = arr.length; i < len; i++) {
      if (!(arr[i] instanceof RegExp)) {
        return warn(name + '[' + i + '] is not a RegExp. This will result in errors.');
      }
    }
  }

  function wrapTimers() {
    if (defaultVars.wrapTimers) {
      wrapTimer('setTimeout');
      wrapTimer('setInterval');
    }
  }

  function wrapTimer(name) {
    var original = win[name];

    if (typeof original !== 'function') {
      // cannot wrap because fn is not a function – should actually never happen
      return;
    }

    win[name] = function wrappedTimerSetter(fn) {
      // non-deopt arguments copy
      var args = new Array(arguments.length);

      for (var i = 0; i < arguments.length; i++) {
        args[i] = arguments[i];
      }

      args[0] = wrap(fn);
      return original.apply(this, args);
    };
  }

  function wrap(fn) {
    if (typeof fn !== 'function') {
      // cannot wrap because fn is not a function
      return fn;
    }

    return function wrappedTimerHandler() {
      try {
        return fn.apply(this, arguments);
      } catch (e) {
        reportError(e);
        ignoreNextOnErrorEvent();
        throw e;
      }
    };
  }

  var state$2 = {
    onEnter: function () {
      if (!fulfillsPrerequisites()) {
        warn('Browser does not have all the required features for web monitoring.');
      }

      var globalObjectName = win[defaultVars.nameOfLongGlobal];
      var globalObject = win[globalObjectName];

      if (!globalObject) {
        {
          warn('global ' + defaultVars.nameOfLongGlobal + ' not found. Did you use the initializer?');
        }

        return;
      }

      if (!globalObject.q) {
        {
          warn('Command queue not defined. Did you add the tracking script multiple times to your website?');
        }

        return;
      }

      if (typeof globalObject['l'] !== 'number') {
        {
          warn('Reference timestamp not set via EUM initializer. Was the initializer modified?');
        }

        return;
      }

      processCommands(globalObject.q); // prefer the backend trace ID which was explicitly set

      defaultVars.pageLoadBackendTraceId = defaultVars.pageLoadBackendTraceId || getPageLoadBackendTraceId();
      defaultVars.initializerExecutionTimestamp = globalObject['l'];
      addCommandAfterInitializationSupport();

      if (!defaultVars.reportingUrl) {
        {
          error('No reporting URL configured. Aborting EUM initialization.');
        }

        return;
      }

      instrumentXMLHttpRequest();
      instrumentFetch();
      hookIntoGlobalErrorEvent();
      wrapTimers();
      wrapEventHandlers();
      hookIntoGlobalUnhandledRejectionEvent();
      observeCspViolations();
      transitionTo('waitForPageLoad');
    },
    getActiveTraceId: function () {
      return defaultVars.pageLoadTraceId;
    },
    getActivePhase: function () {
      return pageLoad;
    },
    triggerManualPageLoad: function () {
      {
        warn('Triggering a page load while EUM is initializing is unsupported.');
      }
    }
  };

  function processCommands(commands) {
    for (var i = 0, len = commands.length; i < len; i++) {
      processCommand(commands[i]);
    }
  }

  function addCommandAfterInitializationSupport() {
    var globalObjectName = win[defaultVars.nameOfLongGlobal];

    win[globalObjectName] = function () {
      return processCommand(arguments);
    };
  }

  function fulfillsPrerequisites() {
    return win.XMLHttpRequest && win.JSON;
  }

  registerState('init', state$2);
  registerState('waitForPageLoad', state);
  registerState('pageLoaded', state$1);
  transitionTo('init');

}());
//# sourceMappingURL=eum.debug.js.map
