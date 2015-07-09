var http = (function() {
  "use strict";

  function doReq(method, path, headers, body, onSuccess, onFailure ) {
    var req = new XMLHttpRequest();
    req.open(method, path, true);

    for (var prop in headers) {
      req.setRequestHeader(prop, headers[prop]);
    }
 
    req.onload = function() {
     if (req.status >= 200 && req.status < 400) {
        onSuccess(req);
     } else {
        // request reached server, but we got an error
        onFailure(req.responseText);
      }
    };

    // request didn't reach server
    req.onerror = onFailure;

    if ( body !== "" ) {
      req.send(body);
    } else {
      req.send();
    }
  }

  // return exported functions
  return {
    get: function(path, headers, onSuccess, onFailure) { 
      doReq("GET", path, headers, "", onSuccess, onFailure);
    },
    post: function(path, headers, body, onSuccess, onFailure) {
      doReq("POST", path, headers, body, onSuccess, onFailure);
    },
    put: function(path, headers, body, onSuccess, onFailure) {
      doReq("PUT", path, headers, body, onSuccess, onFailure);
    },
    patch: function(path, headers, body, onSuccess, onFailure) {
      doReq("PATCH", path, headers, body, onSuccess, onFailure);
    },
    delete: function(path, headers, onSuccess, onFailure) {
      doReq("DELETE", path, headers, "", onSuccess, onFailure);
    }
  };
}());




var rdf = (function() {
  "use strict";

  function propsByClass( ontology, cls ) {
    return ontology["@graph"].filter(function(e) {
      return ( e["@type"] == "rdfs:Property" &&
        ( e["rdfs:domain"] === undefined ||
          e["rdfs:domain"]["@id"] === "rdfs:Class" || e["rdfs:domain"]["@id"] == "deichman:"+cls ) );
    });
  }

  function resolveURI( ontology, uri ) {
    var i = uri.indexOf(":");
    var prefix = uri.substr( 0, i );
    for ( var k in ontology["@context"] ) {
      if ( prefix === k ) {
        return ontology["@context"][k]+uri.substr(i+1);
      }
    }
  }

  function validateLiteral( value, range ) {
    switch (range) {
      case "http://www.w3.org/2001/XMLSchema#string":
        // a javscript string is always valid as xsd:string
        return true;
        break;
      case "http://www.w3.org/2001/XMLSchema#gYear":
        // According to its specification, a xsd:gYear allows time-zone information, but
        // we don't want that, and only accepts negative (BCE) or positive (CE) integers.
        // TODO shall we require 0-padding - i.e not allow "92" but require "0092"?
        return /^-?(\d){1,4}$/.test(value);
        break;
      case "http://www.w3.org/2001/XMLSchema#nonNegativeInteger":
        return /^\+?(\d)+$/.test(value);
        break;
      default:
        var err = "don't know how to validate literal of range: <" + range + ">";
        throw err;
    }
  }

  function createPatch( subject, predicate, el ) {
    var addPatch,
        delPatch;

    if ( el.current.value !== "") {
      addPatch = { op: "add", s: subject, p: predicate, o: { value: el.current.value } }
      if ( el.current.lang != "" ) {
        addPatch.o.lang = el.current.lang;
      }
    }

    if ( el.old.value !== "" ) {
    delPatch = { op: "del", s: subject, p: predicate, o: { value: el.old.value } }
      if ( el.old.lang != "" ) {
      delPatch.o.lang = el.old.lang;
      }
    }


    if (delPatch && addPatch) {
      return JSON.stringify([delPatch,addPatch]);
    } else if (delPatch) {
      return JSON.stringify(delPatch);
    } else {
      return JSON.stringify(addPatch);
    }
  }

  // exported functions
  return {
    propsByClass: propsByClass,
    createPatch: createPatch,
    resolveURI: resolveURI,
    validateLiteral: validateLiteral
  };
})();
