// Copyright (C) 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.caja.service;

import junit.framework.AssertionFailedError;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.caja.reporting.MessageLevel;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class HtmlHandlerTest extends ServiceTestCase {
  public final void testHtml2Json() throws Exception {
    assertHtml2Json("*/*");
    assertHtml2Json("text/html");
    assertHtml2Json("text/html");
  }

  public final void testHtmlWithJsonpCallback() throws Exception {
    registerUri("http://foo/bar.html", "<p>some random text</p>", "text/html");

    {
      String s = (String) requestGet("?url=http://foo/bar.html"
          + "&input-mime-type=text/html"
          + "&alt=json-in-script"
          + "&callback=foo");
      assertCallbackInJsonp(s, "foo");
      assertSubstringsInJsonp(s, "html", "some random text");
    }

    try {
      assertCallbackInJsonp(
          (String) requestGet("?url=http://foo/bar.html"
              + "&input-mime-type=text/html"
              + "&alt=json-in-script"
              + "&callback=foo.bar"),
          "foo.bar");
      fail("Failed to reject non-identifier JSONP callback");
    } catch (AssertionFailedError e) {
      // Success
    }

    try {
      assertCallbackInJsonp(
          (String) requestGet("?url=http://foo/bar.html"
              + "&input-mime-type=text/html"
              + "&callback=foo.bar"),
          "foo.bar");
      fail("Added JSONP callback when not requested");
    } catch (AssertionFailedError e) {
      // Success
    }

    try {
      assertCallbackInJsonp(
          (String) requestGet("?url=http://foo/bar.html"
              + "&input-mime-type=text/html"
              + "&alt=json"
              + "&callback=foo.bar"),
          "foo.bar");
      fail("Added JSONP callback when not requested");
    } catch (AssertionFailedError e) {
      // Success
    }
  }

  public final void testSandboxedLink() throws Exception {
    registerUri(
        "http://foo/bar.css", "a { background-image: url(baz.png) }",
        "text/css");
    registerUri(
        "http://foo/index.html",
        "<link rel=stylesheet href=bar.css><a href=\"shizzle.html\">Clicky</a>",
        "text/html");
    String result = (String) requestGet(
        "?url=http://foo/index.html&input-mime-type=text/html"
        + "&output-mime-type=text/html&sext=true&idclass=foo___");
    JSONObject json = (JSONObject) json(result);
    assertContainsIgnoreSpace(
        (String) json.get("html"),
        " background-image: url('http://caja.appspot.com/cajole?url=http%3a%2f%2fcaja.appspot.com%2fcajole%3furl%3dhttp%253a%252f%252ffoo%252fbaz.png%26effect%3dSAME%5fDOCUMENT%26loader%3dSANDBOXED%26sext%3dtrue&effect=SAME%5fDOCUMENT&loader=SANDBOXED&sext=true')");
    assertContainsIgnoreSpace(
        (String) json.get("html"),
        "<a href=\"http://caja.appspot.com/cajole"
        + "?url=http%3a%2f%2ffoo%2fshizzle.html&amp;effect=NEW_DOCUMENT"
        + "&amp;loader=UNSANDBOXED&amp;sext=true\" target=\"_blank\">");
  }

  public final void testUnsandboxedLink() throws Exception {
    registerUri(
        "http://foo/bar.css", "a { background-image: url(baz.png) }",
        "text/css");
    registerUri(
        "http://foo/index.html",
        "<link rel=stylesheet href=bar.css><a href=\"shizzle.html\">Clicky</a>",
        "text/html");
    String result = (String) requestGet(
        "?url=http://foo/index.html&input-mime-type=text/html"
        + "&output-mime-type=text/html&sext=false&idclass=foo___");
    JSONObject json = (JSONObject) json(result);
    assertContainsIgnoreSpace(
        (String) json.get("html"),
        "background-image: url('http://foo/baz.png')");
    assertContainsIgnoreSpace(
        (String) json.get("html"),
        "<a href=\"http://foo/shizzle.html\" target=\"_blank\">Clicky</a>");
  }

  private void assertHtml2Json(String inputMimeType)
      throws Exception {
    registerUri(
        "http://foo/bar.html", "<p>hi</p><script>42;</script><p>bye</p>",
        "text/html");

    Object result = json((String) requestGet(
        "?url=http://foo/bar.html&input-mime-type=" + inputMimeType));
    assertNotNull(result);
    assertTrue(result instanceof JSONObject);
    JSONObject json = (JSONObject) result;

    // Check html generation is correct
    assertEquals("<p>hi<span id=\"id_1___\"></span></p><p>bye</p>",
        (String)json.get("html"));
    assertEquals("{"
        + "___.loadModule({"
        + "'instantiate':function(___,IMPORTS___){"
          + "return ___.prepareModule({"
              + "'instantiate':function(___,IMPORTS___){"
                + "var\nmoduleResult___,el___,emitter___;"
                +	"moduleResult___=___.NO_RESULT;"
                + "{"
                  +	"emitter___=IMPORTS___.htmlEmitter___;"
                  + "emitter___.discard(emitter___.attach('id_1___'))"
                + "}"
                + "return moduleResult___"
              + "},"
              + "'cajolerName':'com.google.caja',"
              + "'cajolerVersion':'testBuildVersion',"
              + "'cajoledDate':0})(IMPORTS___),"
              + "___.prepareModule({"
                + "'instantiate':function(___,IMPORTS___){var\n"
                  + "$v=___.readImport(IMPORTS___,'$v',{"
                      + "'getOuters':{'()':{}},"
                      + "'initOuter':{'()':{}},"
                      + "'ro':{'()':{}}"
                  + "});"
                  + "var\nmoduleResult___,$dis;"
                  + "moduleResult___=___.NO_RESULT;"
                  + "$dis=$v.getOuters();"
                  + "$v.initOuter('onerror');"
                  + "try{"
                    + "{moduleResult___=42}"
                  + "}catch(ex___){"
                    + "___.getNewModuleHandler().handleUncaughtException("
                        + "ex___,$v.ro('onerror'),'bar.html','1')"
                  + "}"
                  + "return moduleResult___"
                + "},"
                + "'cajolerName':'com.google.caja',"
                + "'cajolerVersion':'testBuildVersion',"
                + "'cajoledDate':0"
              + "})(IMPORTS___),___.prepareModule({"
                + "'instantiate':function(___,IMPORTS___){"
                  + "var\nmoduleResult___,el___,emitter___;"
                  + "moduleResult___=___.NO_RESULT;"
                  + "{"
                    + "emitter___=IMPORTS___.htmlEmitter___;"
                    + "el___=emitter___.finish();"
                    + "emitter___.signalLoaded()"
                  + "}"
                  + "return moduleResult___"
                + "},"
                + "'cajolerName':'com.google.caja',"
                + "'cajolerVersion':'testBuildVersion',"
                + "'cajoledDate':0"
              + "})(IMPORTS___)"
            + "},"
          + "'cajolerName':'com.google.caja',"
          + "'cajolerVersion':'testBuildVersion',"
          + "'cajoledDate':0"
          + "})"
        + "}",
      (String)json.get("js"));

    assertTrue(json.get("messages") instanceof JSONArray);
    JSONArray messages = (JSONArray)json.get("messages");
    assertMessagesLessSevereThan(messages, MessageLevel.ERROR);
  }

  public final void testErrors2Json() throws Exception {
    registerUri("http://foo/bar.html",
      "<script>with(foo){}</script>", "text/html");

    Object result = json((String) requestGet(
        "?url=http://foo/bar.html&input-mime-type=text/html"));
    assertTrue(result instanceof JSONObject);
    JSONObject json = (JSONObject) result;

    assertTrue(json.containsKey("messages"));
    JSONArray messages = (JSONArray)json.get("messages");
    boolean containsError = false;
    for (Object msg : messages) {
      JSONObject jsonMsg = (JSONObject) msg;
      containsError = containsError || jsonMsg.get("name").equals("ERROR");
    }
    assertTrue(containsError);
  }
}
