// Copyright (C) 2010 Google Inc.
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

package com.google.caja.plugin.stages;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.TranslatedCode;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.plugin.Job;
import com.google.caja.render.Concatenator;
import com.google.caja.reporting.RenderContext;

import java.net.URI;

import org.w3c.dom.Node;

import java.util.Collections;

/**
 * Converts HTML to a block of JavaScript.
 *
 * @author mikesamuel@gmail.com
 */
public final class HtmlToJsStage extends CompileHtmlStage {

  public HtmlToJsStage(CssSchema cssSchema, HtmlSchema htmlSchema) {
    super(cssSchema, htmlSchema);
  }

  @Override
  Job makeJobFromHtml(Node html, URI baseUri) {
    return hasContent(html)
        ? Job.jsJob(makeEmitStaticStmt(html), baseUri) : null;
  }

  private static Statement makeEmitStaticStmt(Node node) {
    return (Statement) QuasiBuilder.substV(
        "'use strict'; @stmt;",
        "stmt", new TranslatedCode(new Block(
            FilePosition.UNKNOWN,
            Collections.singletonList(new ExpressionStmt(
                (Expression) QuasiBuilder.substV(
                    "IMPORTS___./*@synthetic*/htmlEmitter___"
                        + "./*@synthetic*/emitStatic(@html)",
                    "html", renderDomAsJsStringLiteral(node)))))));
  }

  private static StringLiteral renderDomAsJsStringLiteral(Node node) {
    StringBuilder stringBuilder = new StringBuilder();
    TokenConsumer tc = new Concatenator(stringBuilder);
    Nodes.render(node, new RenderContext(tc));
    return StringLiteral.valueOf(Nodes.getFilePositionFor(node), stringBuilder);
  }

  private static boolean hasContent(Node n) {
    switch (n.getNodeType()) {
      case Node.DOCUMENT_FRAGMENT_NODE:
        for (Node c : Nodes.childrenOf(n)) {
          if (hasContent(c)) { return true; }
        }
        return false;
      case Node.COMMENT_NODE: return false;
      case Node.TEXT_NODE:
        return n.getNodeValue().length() != 0;
      default:
        return true;
    }
  }
}
