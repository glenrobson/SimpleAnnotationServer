
// From Getty code: http://www.getty.edu/art/collection/static/viewers/mirador/?manifest=https://data.getty.edu/museum/api/iiif/1895/manifest.json
function getURLParameter(param) {
    if(typeof(param) == "string" && param.length > 0) {
        if(typeof(window.location.search) == "string" && window.location.search.length > 0) {
            var _results = new RegExp(param + "=([^&]*)", "i").exec(window.location.search);
            if(typeof(_results) == "object" && _results !== null && typeof(_results.length) == "number" && _results.length > 0 && _results[1]) {
                if(typeof(_results[1]) == "string" && _results[1].length > 0) {
                    return unescape(_results[1]);
                }
            }
        }
    }
    return null;
}

