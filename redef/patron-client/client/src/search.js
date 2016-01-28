import axios from 'axios'
import Ractive from 'ractive'
Ractive.DEBUG = false;
import ractiveEventsKeys from 'ractive-events-keys'
Ractive.events = ractiveEventsKeys;
import url from 'url'

export default {
  init: function () {
    if (window && !window.Promise) {
      window.Promise = Ractive.Promise; // axios needs a Promise polyfill, so we use the one provided by ractive.
    }

    var config;
    return axios.get("/config").then(function (response) {
      config = response.data;
    }).then(function () {
      return axios.get("/search_template.html").then(function (response) {
        return response.data;
      });
    }).then(function (data) {
      var searchRactive = new Ractive({
        el: "#search-app",
        template: data,
        data: {
          hits: {
            total: 0,
            hits: []
          },
          workUrl: function (work) {
            return '/work/' + url.parse(work.uri).path.split('/').pop();
          },
          personUrl: function (person) {
            return '/person/' + url.parse(person.uri).path.split('/').pop();
          }
        }
      });

      searchRactive.on({
        search: function () {
          var term = searchRactive.get("search_term");
          axios.get("http://" + config.host + ":" + config.port + "/search/work/_search?q=" + term)
            .then(function (response) {
              searchRactive.set("currentSearchTerm", term);
              searchRactive.set("hits", response.data.hits);
            });
        }
      });
      return searchRactive;
    }).catch(function (error) {
      console.log(error);
    });
  }
}