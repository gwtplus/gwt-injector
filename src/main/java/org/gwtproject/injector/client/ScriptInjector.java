/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gwtproject.injector.client;

import org.gwtproject.callback.shared.Callback;

import elemental2.dom.Document;
import elemental2.dom.DomGlobal;
import elemental2.dom.ErrorEvent;
import elemental2.dom.HTMLScriptElement;
import elemental2.dom.Window;
import jsinterop.annotations.JsProperty;
import jsinterop.base.Js;

/**
 * Dynamically create a script tag and attach it to the DOM.
 * 
 * Usage with script as local string:
 * <p>
 * 
 * <pre>
 *   String scriptBody = "var foo = ...";
 *   ScriptInjector.fromString(scriptBody).inject();
 * </pre>
 * <p>
 * Usage with script loaded as URL:
 * <p>
 * 
 * <pre>
 *   ScriptInjector.fromUrl("http://example.com/foo.js").setCallback(
 *     new Callback<Void, Exception>() {
 *        public void onFailure(Exception reason) {
 *          Window.alert("Script load failed.");
 *        }
 *        public void onSuccess(Void result) {
 *          Window.alert("Script load success.");
 *        }
 *     }).inject();
 * </pre>
 * <p>
 * NOTE: This class uses {@link Js#uncheckedCast(Object)} because
 * elemental2 types all assume &lt;global>/$wnd as a namespace and
 * this class uses both &lt;global> and &lt;window>
 */
public class ScriptInjector {
  
  @JsProperty(namespace = "<window>", name = "self")
  static native Window currentWindow();

  /**
   * Builder for directly injecting a script body into the DOM.
   */
  public static class FromString {
    private boolean removeTag = true;
    private final String scriptBody;
    private Window window;

    /**
     * @param scriptBody The script text to install into the document.
     */
    public FromString(String scriptBody) {
      this.scriptBody = scriptBody;
    }

    /**
     * Injects a script into the DOM. The JavaScript is evaluated and will be
     * available immediately when this call returns.
     * 
     * By default, the script is installed in the same window that the GWT code
     * is installed in.
     * 
     * @return the script element created for the injection. Note that it may be
     *         removed from the DOM.
     */
    public <T> T inject() {
      Window wnd = (window == null) ? currentWindow() : window;
      assert wnd != null;
      
      HasDocument hasDoc = Js.uncheckedCast(wnd);
      Document doc = hasDoc.document;
      assert doc != null;
      
      HTMLScriptElement scriptElement = Js.uncheckedCast(doc.createElement("script"));
      assert scriptElement != null;
      
      scriptElement.text = scriptBody;
      doc.head.appendChild(scriptElement);
      
      if (removeTag) {
        scriptElement.parentNode.removeChild(scriptElement);
      }
      
      return Js.uncheckedCast(scriptElement);
    }

    /**
     * @param removeTag If true, remove the tag immediately after injecting the
     *          source. This shrinks the DOM, possibly at the expense of
     *          readability if you are debugging javaScript.
     * 
     *          Default value is {@code true}.
     */
    public FromString setRemoveTag(boolean removeTag) {
      this.removeTag = removeTag;
      return this;
    }

    /**
     * <b>NOTE:</b> Previously this method accepted JavaScriptObject. The signature
     * changed to Object to remove dependency and keep it compatible with
     * existing code. Uses unchecked cast.
     * <p>
     * This call allows you to specify which DOM window object to install the
     * script tag in. To install into the Top level window call
     * <p>
     * <code>
     *   builder.setWindow(ScriptInjector.TOP_WINDOW);
     * </code>
     * 
     * @param window Specifies which window to install in.
     */
    public FromString setWindow(Object window) {
      this.window = (Window) window;
      return this;
    }
  }

  /**
   * Build an injection call for adding a script by URL.
   */
  public static class FromUrl {
    private Callback<Void, Exception> callback;
    private boolean removeTag = false;
    private final String scriptUrl;
    private Window window;

    private FromUrl(String scriptUrl) {
      this.scriptUrl = scriptUrl;
    }

    /**
     * Injects an external JavaScript reference into the document and optionally
     * calls a callback when it finishes loading.
     * 
     * @return the script element created for the injection.
     */
    public <T> T inject() {
      Window wnd = (window == null) ? currentWindow() : window;
      assert wnd != null;
      
      HasDocument hasDoc = Js.uncheckedCast(wnd);
      Document doc = hasDoc.document;
      assert doc != null;
      
      HTMLScriptElement scriptElement = Js.uncheckedCast(doc.createElement("script"));
      assert scriptElement != null;
      
      if (callback != null || removeTag) {
        attachListeners(scriptElement, callback, removeTag);
      }
      
      scriptElement.src = scriptUrl;
      doc.head.appendChild(scriptElement);
      
      return Js.uncheckedCast(scriptElement);
    }

    /**
     * Specify a callback to be invoked when the script is loaded or loading
     * encounters an error.
     * <p>
     * <b>Warning:</b> This class <b>does not</b> control whether or not a URL
     * has already been injected into the document. The client of this class has
     * the responsibility of keeping score of the injected JavaScript files.
     * <p>
     * <b>Known bugs:</b>  This class uses the script tag's <code>onerror()
     * </code> callback to attempt to invoke onFailure() if the 
     * browser detects a load failure.  This is not reliable on all browsers 
     * (Doesn't work on IE or Safari 3 or less).
     * <p>
     * On Safari version 3 and prior, the onSuccess() callback may be invoked
     * even when the load of a page fails.  
     * <p>
     * To support failure notification on IE and older browsers, you should 
     * check some side effect of the script (such as a defined function)
     * to see if loading the script worked and include timeout logic.
     * <p>
     * {@link Callback#onFailure(Object)} may provide {@link CodeDownloadException}
     * 
     * @param callback callback that gets invoked asynchronously.
     */
    public FromUrl setCallback(Callback<Void, Exception> callback) {
      this.callback = callback;
      return this;
    }

    /**
     * @param removeTag If true, remove the tag after the script finishes
     *          loading. This shrinks the DOM, possibly at the expense of
     *          readability if you are debugging javaScript.
     *
     *          Default value is {@code false}, but this may change in a future
     *          release.
     */
    public FromUrl setRemoveTag(boolean removeTag) {
      this.removeTag = removeTag;
      return this;
    }

    /**
     * <b>NOTE:</b> Previously this method accepted JavaScriptObject. The signature
     * changed to Object to remove dependency and keep it compatible with
     * existing code. Uses unchecked cast.
     * <p>
     * This call allows you to specify which DOM window object to install the
     * script tag in. To install into the Top level window call
     * <p>
     * <code>
     *   builder.setWindow(ScriptInjector.TOP_WINDOW);
     * </code>
     * 
     * @param window Specifies which window to install in.
     */
    public FromUrl setWindow(Object window) {
      this.window = Js.uncheckedCast(window);
      return this;
    }
  }

  /**
   * Returns the top level window object. Use this to inject a script so that
   * global variable references are available under <code>$wnd</code> in JSNI
   * access.
   * <p>
   * Note that if your GWT app is loaded from a different domain than the top
   * window, you may not be able to add a script element to the top window.
   */
  public static final Window TOP_WINDOW = DomGlobal.window;

  /**
   * Build an injection call for directly setting the script text in the DOM.
   * 
   * @param scriptBody the script text to be injected and immediately executed.
   */
  public static FromString fromString(String scriptBody) {
    return new FromString(scriptBody);
  }

  /**
   * Build an injection call for adding a script by URL.
   * 
   * @param scriptUrl URL of the JavaScript to be injected.
   */
  public static FromUrl fromUrl(String scriptUrl) {
    return new FromUrl(scriptUrl);
  }

  /**
   * Attaches event handlers to a script DOM element that will run just once a
   * callback when it gets successfully loaded.
   * <p>
   * <b>IE Notes:</b> Internet Explorer calls {@code onreadystatechanged}
   * several times while varying the {@code readyState} property: in theory,
   * {@code "complete"} means the content is loaded, parsed and ready to be
   * used, but in practice, {@code "complete"} happens when the JS file was
   * already cached, and {@code "loaded"} happens when it was transferred over
   * the network. Other browsers just call the {@code onload} event handler. To
   * ensure the callback will be called at most once, we clear out both event
   * handlers when the callback runs for the first time. More info at the <a
   * href="http://www.phpied.com/javascript-include-ready-onload/">phpied.com
   * blog</a>.
   * <p>
   * In IE, do not trust the "order" of {@code readyState} values. For instance,
   * in IE 8 running in Vista, if the JS file is cached, only {@code "complete"}
   * will happen, but if the file has to be downloaded, {@code "loaded"} can
   * fire in parallel with {@code "loading"}.
   * 
   * 
   * @param scriptElement element to which the event handlers will be attached
   * @param callback callback that runs when the script is loaded and parsed.
   */
  private static void attachListeners(HTMLScriptElement scriptElement,
      Callback<Void, Exception> callback, boolean removeTag) {
    
    Runnable clearCallbacks = () -> {
      scriptElement.onload = null;
      scriptElement.onerror = null;
      
      if (removeTag) {
        scriptElement.parentNode.removeChild(scriptElement);
      }
    };
    
    scriptElement.onload = e -> {
      clearCallbacks.run();
      if (callback != null) {
        callback.onSuccess(null);
      }
      return false;
    };
    
    // or possibly more portable script_tag.addEventListener('error', function(){...}, true);
    scriptElement.onerror = e -> {
      clearCallbacks.run();
      if (callback != null) {
        ErrorEvent errorEvent = Js.uncheckedCast(e);
        CodeDownloadException reason = new CodeDownloadException(errorEvent.message);
        callback.onFailure(reason);
      }
      return false;
    };
  }
  
  // TODO: maybe handle onreadystatechange event if onload doesn't work on IE10+
  /*-{
    scriptElement.onreadystatechange = $entry(function() {
      if (/loaded|complete/.test(scriptElement.readyState)) {
        scriptElement.onload();
      }
    });
  }-*/

  /**
   * Utility class - do not instantiate.
   */
  private ScriptInjector() {
  }
}
