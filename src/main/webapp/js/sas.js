
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
function ajaxForm(config) {
    var original = setLoading(config.action.button, config.action.verb);
    var form=document.getElementById(config.form);
    $.ajax({
        url: form.action,
        data: $("form").serialize(),
        type: 'POST',
        success: function(data) {
            showMessage("info", data.message);
            clearLoading(config.action.button, original);
        },
        error: function(data) {
            if (data.status === 500) {
                showMessage("error", data.statusText);
            } else {
                showMessage("error", data.message);
            }
            clearLoading(config.action.button, original);
        }
    });
    return false;
}
function showMessage(messageType, message) {
    var messages = document.getElementById('messages');
    messages.textContent = message;
    if (messageType === "info") {
        messages.className = "alert alert-info";
    } else {
        messages.className = "alert alert-danger";
    }
    messages.style.display = 'block';
}

function setLoading(buttonName, verb) {
    var button = document.getElementById(buttonName);
    var orig = button.innerHTML;
    button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>' + verb + '...';
    button.disabled = true;
    return orig;
}

function clearLoading(buttonName, content) {
    var button = document.getElementById(buttonName);
    button.innerHTML = content;
    button.disabled = false;
}
