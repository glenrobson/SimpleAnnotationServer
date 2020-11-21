var userCollections = null;
var activeCollection = null;
function initCollections(active, callback=null) {
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
                li.dataset.id = id;
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
            $("#scroll").mCustomScrollbar('update');
            if (activeCollection == null) {
                activeCollection = data.collections[0];
            }
            updateCollectionView();
            if (callback != null) {
                callback();
            }
        },
        error: function(data) {
            console.log('Failed to load collection: ' + data);
        }
    });
}

function deleteCollection() {
    setLoading("confirmButton", "Deleting");

    console.log('Deleting: ' + activeCollection["@id"]);
    $.ajax({
        url: activeCollection["@id"],
        type: 'DELETE',
        success: function(data) {
            showMessage("confirmInfoMessage", "info", "Deleted Collection succesfully.");
            deleteId = activeCollection["@id"];
            activeCollection = null;
            var firstEntry = '';
            delete userCollections[deleteId];
            for (const [key, value] of Object.entries(userCollections)) {
                if (!firstEntry) {
                    firstEntry = key;
                }
                if (key.endsWith('inbox.json')) {
                    activeCollection = userCollections[key];
                    break;
                }
            }
            if (Object.keys(userCollections).length > 0) {
                if (activeCollection == null) {
                    activeCollection = userCollections[firstEntry];
                }
                var collectionsUL = document.getElementById('collections'); 
                var existingLis = collectionsUL.getElementsByTagName("li");
                var liToDelete = null;
                for (var i = 0; i < existingLis.length; i++) {
                    var li = existingLis[i];
                    if (li.className === "active") {
                        li.className = "";
                    }
                    if (li.dataset.id === deleteId) {
                        liToDelete = li;
                    }
                    if (activeCollection) {
                        if (li.dataset.id === activeCollection["@id"]) {
                            li.className = "active";
                        }
                    }
                }
                if (liToDelete) {
                    collectionsUL.removeChild(liToDelete);
                }
                updateCollectionView();
            } else {
                // All collections have been removed 
                initCollections('/inbox.json');
            }

            clearLoading("confirmButton", "Confirm");
            $('#confirm').modal('toggle');
        },
        error: function(data) {
            clearLoading("confirmButton", "Confirm");
            if (data.responseJSON) {
                if ("reason" in data.responseJSON) {
                    message = "Failed to delete collection due to: " + data.responseJSON.reason;
                } else if ("message" in data.responseJSON) {
                    message = data.responseJSON.message;
                } else {
                    message = "Failed to delete collection.";
                }
            } else if ("statusText" in data) {    
                message = "Failed to delete collection due to: " + data.statusText;
            } else {
                message = "Failed to delete collection.";
            }
            showMessage("confirmInfoMessage", "error", message);
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
    event.srcElement.parentNode.className = 'active';
    activeCollection =  userCollections[event.srcElement.href];
    updateCollectionView();
    return false;
}
function updateCollectionView() {
    var heading = document.getElementById("collectionName");
    heading.innerHTML = activeCollection.label;
    /*var openAll = document.getElementById("openAll");
    openAll.href = "view.xhtml?collection=" + activeCollection["@id"];
*/
    var deleteCollection = document.getElementById("deleteCollection");
    if (activeCollection["@id"].endsWith("inbox.json")) {
        deleteCollection.style.display = "none";
    } else {
        deleteCollection.style.display = "inline-block";
    }

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
    setLoading("movebutton", "Moving");
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
            showMessage("moveMessage", "info", "Succesfully moved manifest.");
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
            clearLoading("movebutton", "Move");
            hideMessage("moveMessage");
        },
        error: function(data) {
            clearLoading("movebutton", "Move");
            if (data.responseJSON) {
                if ("reason" in data.responseJSON) {
                    message = "Failed to move collection due to: " + data.responseJSON.reason;
                } else if ("message" in data.responseJSON) {
                    message = data.responseJSON.message;
                } else {
                    message = "Failed to move collection.";
                }
            } else if ("statusText" in data) {    
                message = "Failed to move collection due to: " + data.statusText;
            } else {
                message = "Failed to move collection.";
            }
            showMessage("moveMessage", "error", message);
        }
    });

}

function deleteManifest() {
    setLoading("confirmButton", "Deleting");
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
            showMessage("confirmInfoMessage", "info", "Removed manifest from collection succesfully.");
            for (var index in activeCollection.manifests) {
                var manifest = activeCollection.manifests[index];
                if (manifest["@id"] === manifestURL) {
                    activeCollection.manifests.splice(index,1);
                    break;
                }
            }
            updateCollectionView();
            $("#scroll").mCustomScrollbar('update');
            clearLoading("confirmButton", "Confirm");
            $('#confirm').modal('toggle');
        },
        error: function(data) {
            clearLoading("confirmButton", "Confirm");
            if (data.responseJSON) {
                if ("reason" in data.responseJSON) {
                    message = "Failed to delete collection due to: " + data.responseJSON.reason;
                } else if ("message" in data.responseJSON) {
                    message = data.responseJSON.message;
                } else {
                    message = "Failed to delete collection.";
                }
            } else if ("statusText" in data) {    
                message = "Failed to delete collection due to: " + data.statusText;
            } else {
                message = "Failed to delete collection.";
            }
            showMessage("confirmInfoMessage", "error", message);
        }
    });

}


function showConfirm(event) {
    hideMessage("confirmInfoMessage", "Confirm");
    var header = document.getElementById("confirmTitle");
    var text = document.getElementById("confirmText");
    var id = document.getElementById("confirmId");
    var confirmButton = document.getElementById("confirmButton");

    mode = event.srcElement.dataset.mode;
    if (!mode) {
        mode = event.srcElement.parentElement.dataset.mode;
    }

    if (mode === 'delete_collection') {
        header.innerHTML = 'Delete collection';
        text.innerHTML = 'Are you sure you want to delete this collection?';
        id.value = activeCollection['@id'];

        confirmButton.onclick = deleteCollection;
    } else if (mode === 'remove_manifest') {
        header.innerHTML = 'Remove manifest';
        text.innerHTML = 'Are you sure you want to remove this manifest from this collection?';
        id.value = event.srcElement.parentElement.dataset.manifest;

        confirmButton.onclick = deleteManifest;
    }

    $('#confirm').modal('toggle');
}

function showMoveManifest(event) {
    hideMessage("moveMessage");
    if (event.srcElement.dataset.manifest) {
        var manifestId = event.srcElement.dataset.manifest;
    } else {
        var manifestId = event.srcElement.parentElement.dataset.manifest;
    }
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
    li.className = "manifestSummary";

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
    img.src = thumbnail_img;

    openImg = document.createElement("a");
    openImg.href = "view.xhtml?collection=" + activeCollection["@id"] + "&manifest=" + manifest["@id"];
    openImg.className = "align-self-center";
    openImg.appendChild(img);


    mediaHeaderDiv= document.createElement("div");
    mediaHeaderDiv.className = "media-header-div";

    mediaBody = document.createElement("div");
    mediaBody.className = "media-body";

    remove = document.createElement("button");
    remove.type = "button";
    remove.className = "close";
    remove.innerHTML = "<i class='far fa-window-close'></i>";
    remove.setAttribute('aria-hidden', 'true');
    remove.dataset.mode = 'remove_manifest';
    remove.dataset.manifest = manifest["@id"];
    remove.onclick = showConfirm;
   // mediaBody.appendChild(remove);

    mediaHeader = document.createElement("h5");
    mediaHeader.className = "media-heading";
    mediaHeader.innerHTML = manifest.label;
    //mediaBody.appendChild(mediaHeader);

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
    open.className = "btn  btn-secondary mb-2";
    open.innerHTML = '<i class="far fa-edit"></i>';//"Open";
    open.title = "Open manifest for editing";
    actionsBar.appendChild(open);

    move = document.createElement("button");
    move.type = "button";
    move.className = "btn btn-secondary mb-2";
    move.innerHTML = '<i class="far fa-folder-open"></i>';
    move.title = "Move manifest to another collection.";
    move.onclick = showMoveManifest;
    move.dataset.manifest = manifest["@id"];
    actionsBar.appendChild(move);

    download = document.createElement("a");
    download.href = "manifest.xhtml?iiif-content=" + manifest["@id"];
    download.className = "btn btn-secondary mb-2";
    download.innerHTML = '<i class="fas fa-cloud-download-alt"></i>';
    download.title = "Export";
    actionsBar.appendChild(download);

    analytics = document.createElement("a");
    analytics.href = "stats/manifest.xhtml?iiif-content=" + manifest["@id"];
    analytics.className = "btn btn-secondary mb-2";
    analytics.innerHTML = '<i class="fas fa-chart-line"></i>';
    analytics.title = "Analytics";
    actionsBar.appendChild(analytics);

    mediaHeaderDiv.appendChild(remove)
    if ('logo' in manifest && '@id' in manifest.logo) {
        var logo = document.createElement("img");
        logo.className = "logo";
        logo.src= manifest.logo['@id'];
        actionsBar.appendChild(logo);
        mediaHeaderDiv.appendChild(logo)
    }

   // mediaBody.appendChild(remove);
    //mediaBody.appendChild(mediaHeader);

    cardDiv = document.createElement("div");
    cardDiv.className = "media";
    cardDiv.appendChild(openImg);
    cardDiv.appendChild(mediaBody);

    li.appendChild(mediaHeaderDiv);
    li.appendChild(mediaHeader)
    li.appendChild(cardDiv);
    ul.appendChild(li);
}

function createCollection() {
    setLoading("createCollectionbutton", "Creating");
    $.ajax({
        url: '/collection/',
        data: $("#collection_form").serialize(),
        type: 'POST',
        success: function(data) {
            showMessage("createCollectionMessage", "info", "Created collection succesfully.");

            var collectionsUL = document.getElementById('collections'); 
            var existingLis = collectionsUL.getElementsByTagName("li");
            for (var i = 0; i < existingLis.length; i++) {
                var li = existingLis[i];
                if (li.className === "active") {
                    li.className = "";
                }
            }

            activeCollection = data;
            userCollections[data['@id']] = data
            var li = document.createElement("li");
            li.className = "active";
            li.dataset.id = data["@id"];
            var anchor = document.createElement("a")
            anchor.innerHTML = data.label;
            anchor.href= data['@id'];
            anchor.onclick = showCollection;
            li.appendChild(anchor);
            collectionsUL.appendChild(li);

            updateCollectionView();
            $("#scroll").mCustomScrollbar('update');
            clearLoading("createCollectionbutton", "Create");
            $('#createCollection').modal('toggle');
        },
        error: function(data) {
            clearLoading("createCollectionbutton", "Create");
            if (data.responseJSON) {
                if ("reason" in data.responseJSON) {
                    message = "Failed to add collection due to: " + data.responseJSON.reason;
                } else if ("message" in data.responseJSON) {
                    message = "Failed to add collection due to: " + data.responseJSON.message;
                } else {
                    message = "Failed to add collection.";
                }
            } else if ("statusText" in data) {    
                message = "Failed to add collection due to: " + data.statusText;
            } else {
                message = "Failed to add collection.";
            }
            showMessage("createCollectionMessage", "error", message);
            console.log('Failed to load collection: ' + data);
        }
    });
}

function addManifest() {
    setLoading("add_manifest", "Adding");
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
                    showMessage("addManifestMessages", "info", "Manifest added succesfully to collection.");
                    clearLoading("add_manifest", "Add");
                    if ('manifests' in activeCollection) { 
                        activeCollection.manifests.push(manifest);
                    } else {
                        activeCollection.manifests = [manifest];
                    }
                    updateCollectionView();

                    $('#addManifest').modal('toggle');
                    hideMessage("addManifestMessages");
                },
                error: function(data) {
                    clearLoading("add_manifest", "Add");
                    if (data.responseJSON) {
                        if ("reason" in data.responseJSON) {
                            message = "Failed to add manifest due to: " + data.responseJSON.reason;
                        } else if ("message" in data.responseJSON) {
                            message = "Failed to add manifest due to: " + data.responseJSON.message;
                        } else {
                            message = "Failed to add manifest. SAS only currently supports IIIF version 2 manifests.";
                        }
                    } else if ("statusText" in data) {
                            message = "Failed to add manifest due to: " + data.statusText;
                    } else {
                        message = "Failed to add manifest. SAS only currently supports IIIF version 2 manifests.";
                    }
                    showMessage("addManifestMessages", "error", message);
                    console.log('Failed to add manifest: ' + data);
                }
            });

        },
        error: function(data) {
            clearLoading("add_manifest", "Add");
            showMessage("addManifestMessages", "error", "Failed to retrieve Manifest. If it's accessible than it could be a CORS issue.");
            console.log('Failed to delete collection: ' + data);
        }
    });
}
