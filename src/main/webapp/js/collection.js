var userCollections = null;
var activeCollection = null;
function initCollections(active) {
    // Get collections list store it and generate menu
    var collectionsUL = document.getElementById('collections'); 

    $.ajax({
        url: 'collection/all.json',
        type: 'GET',
        success: function(data) {
            userCollections = {};
            collectionsUL.innerHTML = '';
            activeCollection = null;
            for (var index in data.collections) {
                var tCollection = data.collections[index];
                var id = tCollection["@id"];
                userCollections[id] = tCollection;
                var li = document.createElement("li");
                if (id.endsWith(active)) {
                    activeCollection = tCollection;
                    li.className = "active";
                }
                var anchor = document.createElement("a")
                anchor.innerHTML = tCollection.label;
                anchor.href= tCollection['@id'];
                anchor.onclick = showCollection;
                li.appendChild(anchor);
                collectionsUL.appendChild(li);
            }
            if (activeCollection == null) {
                activeCollection = data.collections[0];
            }
            updateCollectionView();
        },
        error: function(data) {
            console.log('Failed to load collection: ' + data);
        }
    });
}

function deleteCollection() {
    $.ajax({
        url: activeCollection["@id"],
        type: 'DELETE',
        success: function(data) {
            $('#confirm').modal('toggle');
            initCollections("inbox.json");
        },
        error: function(data) {
            console.log('Failed to delete collection: ' + data);
        }
    });
}

function addAction(parentEl, text, onClick) { 
    var ul = document.getElementById("actions");
    var li = document.createElement("li");
    var anchor = document.createElement("a")
    anchor.innerHTML = text;
    anchor.href= "#";
    anchor.onclick = onClick;
    li.appendChild(anchor);
    ul.appendChild(li);
}

function showCollection(event) {
    event.preventDefault();
    var collectionsUL = document.getElementById('collections'); 
    var items = collectionsUL.getElementsByTagName("li");
    for (var i = 0; i < items.length; ++i) {
        items[i].className = '';
    }
    event.srcElement.className = 'active';
    activeCollection =  userCollections[event.srcElement.href];
    updateCollectionView();
    return false;
}
function updateCollectionView() {
    var heading = document.getElementById("collectionName");
    heading.innerHTML = activeCollection.label;
    var openAll = document.getElementById("openAll");
    openAll.href = "view.xhtml?collection=" + activeCollection["@id"];

    var manifestsUl = document.getElementById("manifests");
    manifestsUl.innerHTML ='';
    if ('manifests' in activeCollection) { 
        for (var index in activeCollection.manifests) {
            var manifest = activeCollection.manifests[index];
            if ('@context' in manifest) {
                showManifestDiv(manifestsUl, manifest);
            } else {
                // Need to retrieve Manifest
                $.ajax({
                    url: manifest["@id"],
                    type: 'GET',
                    success: function(manifest) {
                        for (var index in activeCollection.manifests) {
                            if (activeCollection.manifests[index]["@id"] === manifest["@id"]) {
                                activeCollection.manifests[index] = manifest;
                            }
                        }
                        showManifestDiv(manifestsUl, manifest);
                    },
                    error: function(data) {
                        console.log('Failed to load manifest: ' + data);
                    }
                });
            }
                            
        }
    }
}

function moveManifest() {
    var select = document.getElementById("collectionSelect");
    var collectionId = select.options[select.selectedIndex].value;
    
    var manifestEl = document.getElementById("manifestURI");
    var manifestURL = manifestEl.value;

    $.ajax({
        url: '/collection/',
        data: {
            "from": activeCollection["@id"],
            "to": collectionId,
            "manifest": manifestURL
        },
        type: 'PUT',
        success: function(data) {
            for (var index in activeCollection.manifests) {
                var manifest = activeCollection.manifests[index];
                if (manifest["@id"] === manifestURL) {
                    if (!('manifests' in userCollections[collectionId])) {
                        userCollections[collectionId].manifests = [];
                    }
                    userCollections[collectionId].manifests.push(manifest);
                    activeCollection.manifests.splice(index,1);
                    break;
                }
            }
            updateCollectionView();
            $('#moveManifest').modal('toggle');
        },
        error: function(data) {
            console.log('Failed to load collection: ' + data);
        }
    });

}

function deleteManifest() {
    var manifestURL = document.getElementById("confirmId").value;
    var collection = activeCollection["@id"];
    
    $.ajax({
        url: '/collection/',
        data: {
            "from": collection,
            "manifest": manifestURL
        },
        type: 'PUT',
        success: function(data) {
            for (var index in activeCollection.manifests) {
                var manifest = activeCollection.manifests[index];
                if (manifest["@id"] === manifestURL) {
                    activeCollection.manifests.splice(index,1);
                    break;
                }
            }
            updateCollectionView();
            $('#confirm').modal('toggle');
        },
        error: function(data) {
            console.log('Failed to load collection: ' + data);
        }
    });

}


function showConfirm(event) {
    var header = document.getElementById("confirmTitle");
    var text = document.getElementById("confirmText");
    var id = document.getElementById("confirmId");
    var confirmButton = document.getElementById("confirmButton");

    mode = event.srcElement.dataset.mode;

    if (mode === 'delete_collection') {
        header.innerHTML = 'Delete collection';
        text.innerHTML = 'Are you sure you want to delete this collection?';
        id.value = activeCollection['@id'];

        confirmButton.onclick = deleteCollection;
    } else if (mode === 'remove_manifest') {
        header.innerHTML = 'Remove manifest';
        text.innerHTML = 'Are you sure you want to remove this manifest from this collection?';
        id.value = event.srcElement.dataset.manifest;

        confirmButton.onclick = deleteManifest;
    }

    $('#confirm').modal('toggle');
}

function showMoveManifest(event) {
    var manifestId = event.srcElement.dataset.manifest;
    var manifestEl = document.getElementById("manifestURI");
    var manifestLabel = '';
    for (var index in activeCollection.manifests) {
        var manifest = activeCollection.manifests[index];
        if (manifest["@id"] === manifestId) {
            manifestLabel = manifest.label;
            manifestEl.value = manifestId;
            break;
        }
    }
    var tLabelEl = document.getElementById("manifestLabel");
    tLabelEl.innerHTML = manifestLabel;

    var collectionSelect = document.getElementById("collectionSelect");
    collectionSelect.innerHTML = '';
    for (var key in userCollections) { 
        var collection = userCollections[key];
        if (collection["@id"] !== activeCollection["@id"]) {
            var option = document.createElement("option");
            option.text = collection.label;
            option.value= collection["@id"];
            collectionSelect.add(option);
        }
    }

    $('#moveManifest').modal('toggle');
}

function showManifestDiv(ul, manifest) {
    var li = document.createElement("li");
    li.className = "media manifestSummary";
    var anchor = document.createElement("a");
    anchor.className = "pull-left";
    anchor.href= "manifest.xhtml?manifest=" + manifest["@id"];

    var thumbnail_img = "";
    if ('thumbnail' in manifest) {
        if (typeof manifest.thumbnail === 'string' || manifest.thumbnail instanceof String) {
            thumbnail_img = manifest.thumbnail;
        } else {
            thumbnail_img = manifest.thumbnail['@id'];
        }
    } else {
        // Get image service from first canvas
        var imageId = manifest.sequences[0].canvases[0].images[0].resource.service["@id"];
        thumbnail_img = imageId + '/full/,100/0/default.jpg';
    }
    
    var img = document.createElement("img");
    img.className = "align-self-center mr-3 media-img";
    img.src= thumbnail_img;

    mediaBody = document.createElement("div");
    mediaBody.className = "media-body";

    remove = document.createElement("button");
    remove.type = "button";
    remove.className = "close";
    remove.innerHTML = "x";
    remove.setAttribute('aria-hidden', 'true');
    remove.dataset.mode = 'remove_manifest';
    remove.dataset.manifest = manifest["@id"];
    remove.onclick = showConfirm;
    mediaBody.appendChild(remove);

    mediaHeader = document.createElement("h5");
    mediaHeader.className = "media-heading";
    mediaHeader.innerHTML = manifest.label;
    mediaBody.appendChild(mediaHeader);

    if ('description' in manifest) {
        mediaContent = document.createElement("p");
        mediaContent.className = "";
        mediaContent.innerHTML = manifest.description;
        mediaBody.appendChild(mediaContent);
    }

    if ('attribution' in manifest) {
        var attribution = manifest.attribution;
        if (typeof attribution === 'object' && attribution!== null) {
            attribution = manifest.attribution["@value"];
        }
        mediaContent = document.createElement("p");
        mediaContent.className = "";
        mediaContent.innerHTML = attribution;
        mediaBody.appendChild(mediaContent);
    }

    actionsBar = document.createElement("div");
    actionsBar.id = "actionBar";
    mediaBody.appendChild(actionsBar);

    open = document.createElement("a");
    open.href = "view.xhtml?collection=" + activeCollection["@id"] + "&manifest=" + manifest["@id"];
    open.className = "btn btn-primary mb-2";
    open.innerHTML = "Open";
    actionsBar.appendChild(open);

    move = document.createElement("button");
    move.type = "button";
    move.className = "btn btn-primary mb-2";
    move.innerHTML = "Move";
    move.onclick = showMoveManifest;
    move.dataset.manifest = manifest["@id"];
    actionsBar.appendChild(move);

    download = document.createElement("a");
    download.href = "manifest.xhtml?iiif-content=" + manifest["@id"];
    download.className = "btn btn-primary mb-2";
    download.innerHTML = "Export";
    actionsBar.appendChild(download);

    analytics = document.createElement("a");
    analytics.href = "stats/manifest.xhtml?iiif-content=" + manifest["@id"];
    analytics.className = "btn btn-primary mb-2";
    analytics.innerHTML = "Analytics";
    actionsBar.appendChild(analytics);

    if ('logo' in manifest && '@id' in manifest.logo) {
        var logo = document.createElement("img");
        logo.className = "logo";
        logo.src= manifest.logo['@id'];
        actionsBar.appendChild(logo);
    }

    li.appendChild(img);
    li.appendChild(mediaBody);
    ul.appendChild(li);
}

function createCollection() {
    $('#createCollection').modal('toggle');
    $.ajax({
        url: '/collection/',
        data: $("#collection_form").serialize(),
        type: 'POST',
        success: function(data) {
            initCollections(data['@id'])
        },
        error: function(data) {
            console.log('Failed to load collection: ' + data);
        }
    });

}

function addManifest() {
    var manifestUri = document.getElementById("manifest_uri").value;
    $.ajax({
        url: manifestUri,
        type: 'GET',
        success: function(manifest) {
            delete manifest.within;
            manifest.within = activeCollection["@id"];
            $.ajax({
                url: 'manifests',
                type: 'POST',
                data: JSON.stringify(manifest),
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                success: function(data) {
                    if ('manifests' in activeCollection) { 
                        activeCollection.manifests.push(manifest);
                    } else {
                        activeCollection.manifests = [manifest];
                    }
                    updateCollectionView();

                    $('#addManifest').modal('toggle');
                },
                error: function(data) {
                    console.log('Failed to add manifest: ' + data);
                }
            });

        },
        error: function(data) {
            console.log('Failed to delete collection: ' + data);
        }
    });
}
