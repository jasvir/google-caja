// Copyright (C) 2011 Google Inc.
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

/**
 * @fileoverview This test attempts to run a simple piece of guest code. It
 * asserts that an error is returned from the request to cajole the guest code.
 * This test assumes it is run in an environment where "caja.js" and all its
 * client side dependencies are consistently of one version, but the cajoling
 * server is of a different version. It asserts that this situation will end in
 * failure, which is required to maintain invariants.
 *
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

// Alias the console to capture messages for future matching.

var consoleMessages = "";

// Callback for error checking

var checkErrorsInterval = undefined;

var testConsole = {
  log: function(msg) { consoleMessages += msg; }
};

// Register the test case.

// Flag indicating that the client-side subsystem loaded correctly (so we are
// sure that errors are due to the cajoling server, not something else)

var clientSideLoaded = false;

registerTest('testVersionSkew', function testVersionSkew() {
  caja.initialize({
    cajaServer: '/caja',
    console: testConsole
  });
  caja.load(undefined, undefined, function (frame) {
    clientSideLoaded = true;
    var extraImports = { x: 4, y: 3 };
    frame.code('es53-test-guest.js', 'text/javascript')
         .api(extraImports)
         .run(function(result) {
           // If we succeed in running, we fail the test!
           fail('testVersionSkew');
           clearInterval(checkErrorsInterval);
         });
  });
});

// Start the actual testing.

readyToTest();
jsunitRun();

// Check for error strings in the console and pass if the expected error
// is seen.

function checkErrors() {
  // TODO(ihab.awad): If we can pass the expected error message on the URL
  // to the test, we can look for custom errors for each individual case. We
  // would have to URL-encode/decode the expected message.
  if (clientSideLoaded && /Build version error/.test(consoleMessages)) {
    jsunitPass('testVersionSkew');
    clearInterval(checkErrorsInterval);
  }
}

checkErrorsInterval = setInterval(checkErrors, 125);
