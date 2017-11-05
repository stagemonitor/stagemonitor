(function () {
'use strict';

var win = window;
var encodeURIComponent = win.encodeURIComponent;
var OriginalXMLHttpRequest = win.XMLHttpRequest;

// aliasing the global function for improved minification and
// protection against hasOwnProperty overrides.
var globalHasOwnProperty = Object.prototype.hasOwnProperty;
function hasOwnProperty(obj, key) {
  return globalHasOwnProperty.call(obj, key);
}

function now() {
  return new Date().getTime();
}

function noop() {}

// We are trying to stay close to common tracing architectures and use
// a hex encoded 64 bit random ID.
var validIdCharacters = '0123456789abcdef'.split('');
var generateUniqueIdImpl$1 = function generateUniqueIdViaRandom() {
  var result = '';
  for (var i = 0; i < 16; i++) {
    result += validIdCharacters[Math.round(Math.random() * 15)];
  }
  return result;
};

if (win.crypto && win.crypto.getRandomValues && win.Uint32Array) {
  generateUniqueIdImpl$1 = function generateUniqueIdViaCrypto() {
    var array = new win.Uint32Array(2);
    win.crypto.getRandomValues(array);
    return array[0].toString(16) + array[1].toString(16);
  };
}

var generateUniqueId = generateUniqueIdImpl$1;

function addEventListener(target, eventType, callback) {
  if (target.addEventListener) {
    target.addEventListener(eventType, callback, false);
  } else if (target.attachEvent) {
    target.attachEvent('on' + eventType, callback);
  }
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


var info = createLogger('info');
var warn = createLogger('warn');
var error = createLogger('error');
var debug = createLogger('debug');

function createLogger(method) {
  if (typeof console === 'undefined') {
    return noop;
  } else if (!console[method]) {
    return function () {
      console.log.apply(console, arguments);
    };
  }

  return function () {
    console[method].apply(console, arguments);
  };
}

var states = {};
var currentStateName = void 0;

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

function triggerManualPageLoad() {
  return states[currentStateName].triggerManualPageLoad();
}

function startSpaPageTransition() {
  return states[currentStateName].startSpaPageTransition();
}

function endSpaPageTransition(opts) {
  return states[currentStateName].endSpaPageTransition(opts);
}

// a wrapper around win.performance for cross-browser support
var performance = win.performance || win.webkitPerformance || win.msPerformance || win.mozPerformance;

var isTimingAvailable = performance && performance.timing;
var isResourceTimingAvailable = performance && performance.getEntriesByType;

var defaultVars = {
  nameOfLongGlobal: 'EumObject',
  pageLoadTraceId: generateUniqueId(),
  pageLoadBackendTraceId: null,
  referenceTimestamp: now(),
  highResTimestampReference: performance && performance.now ? performance.now() : 0,
  initializerExecutionTimestamp: now(),
  reportingUrl: null,
  apiKey: null,
  meta: {},
  ignoreUrls: [],
  ignorePings: true,
  xhrTransmissionTimeout: 20000,
  manualPageLoadEvent: false,
  manualPageLoadTriggered: false,
  autoClearResourceTimings: true,
  page: undefined,
  sampleRate: 1
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


  triggerManualPageLoad: onLoad,

  startSpaPageTransition: function () {
    {
      warn('Cannot start an SPA page transition until the page is considered loaded.');
    }
  },


  /* eslint-disable no-unused-vars */
  endSpaPageTransition: function (opts) {
    /* eslint-enable no-unused-vars */
    {
      warn('No pending SPA page transition to end. Waiting for page load instead.');
    }
  }
};
function onLoad() {
  transitionTo('pageLoaded');
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
  var character = keyCharacters[keyCharacterIndex];
  // Characters exhausted, add value to node
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

// See https://w3c.github.io/resource-timing/
// See https://www.w3.org/TR/hr-time/

var urlMaxLength = 255;

var initiatorTypes = {
  'other': 0,
  'img': 1,
  'link': 2,
  'script': 3,
  'css': 4,
  'xmlhttprequest': 5,
  'html': 6,
  // IMAGE element inside a SVG
  'image': 7
};

var cachingTypes = {
  unknown: 0,
  cached: 1,
  validated: 2,
  fullLoad: 3
};

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

  for (var i = 0, len = performanceEntries.length; i < len; i++) {
    var entry = performanceEntries[i];
    if (minStartTime != null && entry['startTime'] - defaultVars.highResTimestampReference + defaultVars.referenceTimestamp < minStartTime) {
      continue;
    }

    var url = entry.name;
    var lowerCaseUrl = url.toLowerCase();
    if (lowerCaseUrl === 'about:blank' || lowerCaseUrl.indexOf('javascript:') === 0) {
      continue;
    }

    if (url.length > urlMaxLength) {
      url = url.substring(0, urlMaxLength);
    }

    // We provide more detailed XHR insights via our XHR instrumentation.
    // The XHR instrumentation is available once the initialization was executed
    // (which is completely synchronous).
    if (entry['initiatorType'] !== 'xmlhttprequest' || entry['startTime'] < defaultVars.highResTimestampReference) {
      trie.addItem(url, serializeEntry(entry));
    }
  }

  return trie.toJs();
}

function serializeEntry(entry) {
  var result = [Math.round(entry['startTime'] - defaultVars.highResTimestampReference), Math.round(entry['duration']), initiatorTypes[entry['initiatorType']] || initiatorTypes['other']];

  // When timing data is available, we can provide additional information about
  // caching and resource sizes.
  if (typeof entry['transferSize'] === 'number' && typeof entry['encodedBodySize'] === 'number' &&
  // All this information may not be available due to cross-origin.
  entry['transferSize'] > 0) {
    if (entry['transferSize'] === 0) {
      result.push(cachingTypes.cached);
    } else if (entry['transferSize'] < entry['encodedBodySize']) {
      result.push(cachingTypes.validated);
    } else {
      result.push(cachingTypes.fullLoad);
    }

    result.push(entry['encodedBodySize']);
  }

  return result.join(',');
}

function addMetaDataToBeacon(beacon) {
  for (var key in defaultVars.meta) {
    if (hasOwnProperty(defaultVars.meta, key)) {
      beacon['m_' + key] = defaultVars.meta[key];
    }
  }
}

var maxLengthForImgRequest = 2000;

function sendBeacon(data) {
  var str = stringify(data);
  if (str.length === 0) {
    return;
  }

  {
    info('Transmitting beacon', data);
  }

  if (OriginalXMLHttpRequest && str.length > maxLengthForImgRequest) {
    var xhr = new OriginalXMLHttpRequest();
    xhr.open('POST', String(defaultVars.reportingUrl), true);
    xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded;charset=UTF-8');
    xhr.timeout = defaultVars.xhrTransmissionTimeout;
    xhr.send(str);
  } else {
    // Older browsers do not support the XMLHttpRequest API. This sucks and may
    // result in a variety of issues, e.g. URL length restrictions. "Luckily", older
    // browsers also lack support for advanced features such as resource timing.
    // This should make this transmission via a GET request possible.
    var image = new Image();
    image.src = String(defaultVars.reportingUrl) + '?' + str;
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

// We are never recreating this beacon object since we are always reporting / rewriting all properties.
// $FlowFixMe: Find a way to define all properties beforehand so that flow doesn't complain about missing props.
var beacon = {};

var state$1 = {
  onEnter: function () {
    beacon['k'] = defaultVars.apiKey;
    beacon['r'] = defaultVars.referenceTimestamp;
    beacon['t'] = generateUniqueId();
    beacon['ts'] = now();

    beacon['ty'] = 'spa';
    beacon['pl'] = defaultVars.pageLoadTraceId;
  },
  getActiveTraceId: function () {
    // $FlowFixMe: Flow somehow considers this property as null|number|undefined.
    return beacon['t'];
  },
  triggerManualPageLoad: function () {
    {
      warn('Triggering a page load while SPA transitioning is unsupported.');
    }
  },
  startSpaPageTransition: function () {
    {
      warn('Triggering an SPA page transition while already transitioning is unsupported.');
    }

    // best effort here even though it is wrong usage
    transitionTo('spaTransition');
  },
  endSpaPageTransition: function (opts) {
    beacon['p'] = defaultVars.page;
    beacon['l'] = opts['url'];
    beacon['e'] = opts['explanation'];
    // $FlowFixMe: Flow somehow considers the ts property to be a string
    beacon['d'] = now() - beacon['ts'];

    switch (opts['status']) {
      case 'completed':
        beacon['s'] = 'c';
        break;
      case 'aborted':
        beacon['s'] = 'a';
        break;
      case 'error':
        beacon['s'] = 'e';
        break;
      default:
        {
          warn('Unsupported SPA transition status of type ' + opts['status']);
        }
        beacon['s'] = 'u';
        break;
    }

    addMetaDataToBeacon(beacon);
    // $FlowFixMe: Flow somehow considers the ts property to be a string
    addResourceTimings(beacon, beacon['ts'] - 1);
    sendBeacon(beacon);
    transitionTo('pageLoaded');
  }
};

// See spec:
// https://www.w3.org/TR/navigation-timing/

function getPageLoadStartTimestamp() {
  if (!isTimingAvailable) {
    return defaultVars.initializerExecutionTimestamp;
  }
  // We don't use navigationStart since that includes unload times for the previous
  // page.
  var timing = performance.timing;
  return timing.fetchStart - (timing.redirectEnd - timing.redirectStart);
}

function addTimingToPageLoadBeacon(beacon) {
  if (!isTimingAvailable) {
    // This is our absolute fallback mode where we only have
    // approximations for speed information.
    beacon['ts'] = getPageLoadStartTimestamp() - defaultVars.referenceTimestamp;
    beacon['d'] = Number(event.time) - defaultVars.initializerExecutionTimestamp;

    // We add this as an extra property to the beacon so that
    // a backend can decide whether it should include timing
    // information in aggregated metrics. Since they are only
    // approximations, this is not always desirable.
    if (!isTimingAvailable) {
      beacon['tim'] = '0';
    }

    return;
  }

  var timing = performance.timing;

  var redirectTime = timing.redirectEnd - timing.redirectStart;
  // We don't use navigationStart since that includes unload times for the previous page.
  var start = getPageLoadStartTimestamp();
  beacon['ts'] = start - defaultVars.referenceTimestamp;

  // This can happen when the user aborts the page load. In this case, the load event
  // timing information is not available and will have the default value of "0".
  if (timing.loadEventStart > 0) {
    beacon['d'] = timing.loadEventStart - timing.fetchStart;
  } else {
    beacon['d'] = Number(event.time) - defaultVars.initializerExecutionTimestamp;

    // We have partial timing information, but since the load was aborted, we will
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
  var firstPaint = null;

  // Chrome
  if (win.chrome && win.chrome.loadTimes) {
    // Convert to ms
    firstPaint = win.chrome.loadTimes().firstPaintTime * 1000;
  }
  // IE
  else if (typeof win.performance.timing.msFirstPaint === 'number') {
      firstPaint = win.performance.timing.msFirstPaint;
    }
    // standard
    else if (typeof win.performance.timing.firstPaint === 'number') {
        firstPaint = win.performance.timing.firstPaint;
      }

  // First paint may not be available -OR- the browser may have never
  // painted anything and thereby kept this value at 0.
  if (firstPaint != null && firstPaint !== 0) {
    beacon['t_fp'] = Math.round(firstPaint - start);
  }
}

var pageLoadBeaconTransmitted = false;

var state$2 = {
  onEnter: function () {
    if (!pageLoadBeaconTransmitted) {
      pageLoadBeaconTransmitted = true;
      sendPageLoadBeacon();
    }
  },
  getActiveTraceId: function () {
    return null;
  },
  triggerManualPageLoad: function () {
    {
      warn('Page load triggered, but page is already considered as loaded. Did you mark it as loaded more than once?');
    }
  },
  startSpaPageTransition: function () {
    transitionTo('spaTransition');
  },


  /* eslint-disable no-unused-vars */
  endSpaPageTransition: function (opts) {
    /* eslint-enable no-unused-vars */
    {
      warn('No pending SPA page transition to end.');
    }
  }
};
function sendPageLoadBeacon() {
  // $FlowFixMe: Find a way to define all properties beforehand so that flow doesn't complain about missing props.
  var beacon = {};
  beacon['ty'] = 'pl';
  beacon['r'] = defaultVars.referenceTimestamp;
  beacon['k'] = defaultVars.apiKey;
  beacon['t'] = defaultVars.pageLoadTraceId;
  beacon['p'] = defaultVars.page;
  beacon['bt'] = defaultVars.pageLoadBackendTraceId;
  beacon['u'] = win.location.href;

  addMetaDataToBeacon(beacon);
  addTimingToPageLoadBeacon(beacon);
  addResourceTimings(beacon);

  sendBeacon(beacon);
}

var ignorePingsRegex = /.*\/ping(\/?$|\?.*)/i;

function isUrlIgnored(url) {
  if (defaultVars.ignorePings && ignorePingsRegex.test(url)) {
    return true;
  }

  for (var i = 0, len = defaultVars.ignoreUrls.length; i < len; i++) {
    if (defaultVars.ignoreUrls[i].test(url)) {
      return true;
    }
  }

  return false;
}

// In addition to the common HTTP status codes, a bunch of
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

// Asynchronously created a tag.
// document.createElement('a')
var urlAnalysisElement = null;
try {
  urlAnalysisElement = document.createElement('a');
} catch (e) {
  {
    debug('Failed to create URL analysis element. Will not be able to determine same-origin / ', e);
  }
}

function instrumentXMLHttpRequest() {
  if (!OriginalXMLHttpRequest || !new OriginalXMLHttpRequest().addEventListener) {
    {
      info('Browser does not support the features required for XHR instrumentation.');
    }
    return;
  }

  function InstrumentedXMLHttpRequest() {
    var xhr = new OriginalXMLHttpRequest();

    var originalOpen = xhr.open;
    var originalSend = xhr.send;

    // $FlowFixMe: Some properties deliberately left our for js file size reasons.
    var beacon = {
      // general beacon data
      'r': defaultVars.referenceTimestamp,

      // $FlowFixMe: Some properties deliberately left our for js file size reasons.
      'k': defaultVars.apiKey,
      // 't': '',
      'ts': 0,
      'd': 0,

      // xhr beacon specific data
      'ty': 'xhr',
      // 's': '',
      'pl': defaultVars.pageLoadTraceId,
      'l': win.location.href,
      'm': '',
      'u': '',
      'a': 1,
      'st': 0,
      'e': undefined
    };

    addMetaDataToBeacon(beacon);

    // Whether or not we should ignore this beacon, e.g. because the URL is
    // ignored.
    var ignored = false;

    function onFinish(status) {
      if (ignored) return;

      if (beacon['st'] !== 0) {
        // Multiple finish events. Should only happen when we setup the event handlers
        // in a wrong way or when the XHR object is reused. We don't support this use
        // case.
        return;
      }

      beacon['st'] = status;
      // $FlowFixMe: Not sure why flow thinks that the ts value is a string :(
      beacon['d'] = now() - (beacon['ts'] + defaultVars.referenceTimestamp);
      sendBeacon(beacon);
    }

    xhr.addEventListener('timeout', function onTimeout() {
      if (ignored) return;

      onFinish(additionalStatuses.timeout);
    });

    xhr.addEventListener('error', function onError(e) {
      if (ignored) return;

      var message = e && (e.error && e.error.message || e.message);
      if (typeof message === 'string') {
        beacon['e'] = message.substring(0, 300);
      }
      onFinish(additionalStatuses.error);
    });

    xhr.addEventListener('abort', function onAbort() {
      if (ignored) return;

      onFinish(additionalStatuses.abort);
    });

    xhr.addEventListener('readystatechange', function onReadystatechange() {
      if (ignored) return;

      if (xhr.readyState === 4) {
        var status = void 0;

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
    });

    xhr.open = function open(method, url, async) {
      ignored = isUrlIgnored(url);
      if (ignored) {
        {
          debug('Not generating XHR beacon because it should be ignored according to user configuration. URL: ' + url);
        }
        return originalOpen.apply(xhr, arguments);
      }

      if (async === undefined) {
        async = true;
      }

      var traceId = getActiveTraceId();
      var spanId = generateUniqueId();
      if (!traceId) {
        traceId = spanId;
      }

      var sampled = Math.random() <= defaultVars.sampleRate ? 1 : 0;
      beacon['t'] = traceId;
      beacon['s'] = spanId;
      beacon['m'] = method;
      beacon['u'] = normalizeUrl(url);
      beacon['a'] = async ? 1 : 0;
      beacon['sp'] = sampled;

      try {
        var result = originalOpen.apply(xhr, arguments);

        if (isSameOrigin(url)) {
          xhr.setRequestHeader('X-INSTANA-T', traceId);
          xhr.setRequestHeader('X-INSTANA-S', spanId);
          xhr.setRequestHeader('X-INSTANA-L', sampled);
        }

        return result;
      } catch (e) {
        beacon['ts'] = now() - defaultVars.referenceTimestamp;
        beacon['st'] = additionalStatuses.openError;
        beacon['e'] = e.message;
        sendBeacon(beacon);
        throw e;
      }
    };

    xhr.send = function send() {
      if (ignored) {
        return originalSend.apply(xhr, arguments);
      }

      beacon['ts'] = now() - defaultVars.referenceTimestamp;
      beacon['p'] = defaultVars.page;
      return originalSend.apply(xhr, arguments);
    };

    return xhr;
  }

  InstrumentedXMLHttpRequest.prototype = OriginalXMLHttpRequest.prototype;
  InstrumentedXMLHttpRequest.DONE = OriginalXMLHttpRequest.DONE;
  InstrumentedXMLHttpRequest.HEADERS_RECEIVED = OriginalXMLHttpRequest.HEADERS_RECEIVED;
  InstrumentedXMLHttpRequest.LOADING = OriginalXMLHttpRequest.LOADING;
  InstrumentedXMLHttpRequest.OPENED = OriginalXMLHttpRequest.OPENED;
  InstrumentedXMLHttpRequest.UNSENT = OriginalXMLHttpRequest.UNSENT;
  win.XMLHttpRequest = InstrumentedXMLHttpRequest;
}

function isSameOrigin(url) {
  if (!urlAnalysisElement) {
    return false;
  }

  try {
    urlAnalysisElement.href = url;

    var loc = win.location;
    // Most browsers support this fallback logic out of the box. Not so the Internet explorer.
    // To make it work in Internet explorer, we need to add the fallback manually.
    // IE 9 uses a colon as the protocol when no protocol is defined
    return (urlAnalysisElement.protocol && urlAnalysisElement.protocol !== ':' ? urlAnalysisElement.protocol : loc.protocol) === loc.protocol && (urlAnalysisElement.hostname || loc.hostname) === loc.hostname && (urlAnalysisElement.port || loc.port) === loc.port;
  } catch (e) {
    return false;
  }
}

function normalizeUrl(url) {
  if (!urlAnalysisElement) {
    return url;
  }

  try {
    // "a"-elements normalize the URL when setting a relative URL or URLs
    // that are missing a scheme
    urlAnalysisElement.href = url;
    return urlAnalysisElement.href;
  } catch (e) {
    {
      debug('Failed to normalize URL' + url);
    }
    return url;
  }
}

var maxErrorsToReport = 100;
var maxStackSize = 30;

var reportedErrors = 0;
var erroneousPageViewReported = false;
var maxSeenErrorsTracked = 20;
var numberOfDifferentErrorsSeen = 0;
var seenErrors = {};

var scheduledTransmissionTimeoutHandle = void 0;

function hookIntoGlobalErrorEvent() {
  var globalOnError = win.onerror;
  win.onerror = function (message, fileName, lineNumber, columnNumber, error) {
    var stack = error && error.stack;
    if (!stack) {
      stack = 'at ' + fileName + ' ' + lineNumber + ':' + columnNumber;
    }
    onUnhandledError(message, stack);
    if (typeof globalOnError === 'function') {
      return globalOnError.apply(this, arguments);
    }
  };
}

function onUnhandledError(message, stack) {
  if (!erroneousPageViewReported) {
    erroneousPageViewReported = true;
    sendErroneousPageViewBeacon();
  }

  if (!message || reportedErrors > maxErrorsToReport) {
    return;
  }

  if (numberOfDifferentErrorsSeen >= maxSeenErrorsTracked) {
    seenErrors = {};
    numberOfDifferentErrorsSeen = 0;
  }

  message = String(message).substring(0, 300);
  stack = String(stack || '').split('\n').slice(0, maxStackSize).join('\n');
  var location = win.location.href;
  var parentId = getActiveTraceId();
  var key = message + stack + location + parentId;

  var trackedError = seenErrors[key];
  if (trackedError) {
    trackedError.seenCount++;
  } else {
    trackedError = seenErrors[key] = {
      message: message,
      stack: stack,
      location: location,
      parentId: parentId,
      seenCount: 1,
      transmittedCount: 0
    };
    numberOfDifferentErrorsSeen++;
  }

  scheduleTransmission();
}

function scheduleTransmission() {
  if (scheduledTransmissionTimeoutHandle) {
    return;
  }

  scheduledTransmissionTimeoutHandle = setTimeout(send$1, 1000);
}

function send$1() {
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
  var traceId = error.parentId || spanId;
  // $FlowFixMe
  var beacon = {
    // $FlowFixMe
    'k': defaultVars.apiKey,
    's': spanId,
    't': traceId,
    'ts': now(),
    'p': defaultVars.page,

    // xhr beacon specific data
    'ty': 'err',
    'pl': defaultVars.pageLoadTraceId,
    'l': error.location,
    'e': error.message,
    'st': error.stack,
    'c': error.seenCount - error.transmittedCount
  };
  addMetaDataToBeacon(beacon);

  sendBeacon(beacon);
}

function sendErroneousPageViewBeacon() {
  // $FlowFixMe
  var beacon = {
    // $FlowFixMe
    'k': defaultVars.apiKey,
    't': generateUniqueId(),
    'ts': getPageLoadStartTimestamp(),
    'p': defaultVars.page,

    // epv beacon specific data
    'ty': 'epv',
    'pl': defaultVars.pageLoadTraceId
  };
  addMetaDataToBeacon(beacon);
  sendBeacon(beacon);
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
        validateIgnoreUrls(command[1]);
      }
      defaultVars.ignoreUrls = command[1];
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
    case 'startSpaPageTransition':
      startSpaPageTransition();
      break;
    case 'endSpaPageTransition':
      endSpaPageTransition(command[1]);
      break;
    case 'autoClearResourceTimings':
      defaultVars.autoClearResourceTimings = command[1];
      break;
    case 'page':
      defaultVars.page = command[1];
      break;
    case 'ignorePings':
      defaultVars.ignorePings = command[1];
      break;
    case 'sampleRate':
      defaultVars.sampleRate = command[1];
      break;
    default:
      {
        warn('Unsupported command: ' + command[0]);
      }
      break;
  }
}

function validateIgnoreUrls(ignoreUrls) {
  if (!(ignoreUrls instanceof Array)) {
    return warn('ignoreUrls is not an array. This will result in errors.');
  }

  for (var i = 0, len = ignoreUrls.length; i < len; i++) {
    if (!(ignoreUrls[i] instanceof RegExp)) {
      return warn('ignoreUrls[' + i + '] is not a RegExp. This will result in errors.');
    }
  }
}

var state$3 = {
  onEnter: function () {
    if (!fulfillsPrerequisites()) {
      {
        return warn('Browser does not have all the required features for web EUM.');
      }
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

    processCommands(globalObject.q);

    defaultVars.initializerExecutionTimestamp = globalObject['l'];

    addCommandAfterInitializationSupport();

    if (defaultVars.reportingUrl) {
      instrumentXMLHttpRequest();
      hookIntoGlobalErrorEvent();
      transitionTo('waitForPageLoad');
    } else {
      error('No reporting URL configured. Aborting EUM initialization.');
    }
  },
  getActiveTraceId: function () {
    return null;
  },
  triggerManualPageLoad: function () {
    {
      warn('Triggering a page load while EUM is initializing is unsupported.');
    }
  },
  startSpaPageTransition: function () {
    {
      warn('Triggering an SPA page transition is unsupported while EUM is initializing.');
    }
  },


  /* eslint-disable no-unused-vars */
  endSpaPageTransition: function (opts) {
    /* eslint-enable no-unused-vars */
    {
      warn('SPA page transitions are unsupported while EUM is initializing.');
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
    processCommand(arguments);
  };
}

function fulfillsPrerequisites() {
  return win.XMLHttpRequest && win.JSON;
}

registerState('init', state$3);
registerState('waitForPageLoad', state);
registerState('pageLoaded', state$2);
registerState('spaTransition', state$1);

transitionTo('init');

}());
