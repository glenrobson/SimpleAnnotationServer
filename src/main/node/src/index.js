import Mirador from 'mirador/dist/es/src/index';
import annotationPlugins from 'mirador-annotations/es/';
import SimpleAnnotationServerV2Adapter from 'mirador-annotations/es/SimpleAnnotationServerV2Adapter';
import LocalStorageAdapter from 'mirador-annotations/es/LocalStorageAdapter';

// gmr
console.log('Dev change 6');
const params = new URL(document.location).searchParams;
const config = {
  id: 'mirador-viewer',
  annotation: {
    adapter: (canvasId) => new SimpleAnnotationServerV2Adapter(canvasId, 'http://localhost:8888/annotation'),
    // adapter: (canvasId) => new LocalStorageAdapter(`localStorage://?canvasId=${canvasId}`),
  },
  window: {
    defaultSideBarPanel: 'annotations',
    sideBarOpenByDefault: true,
  },
  windows: [
    {
      manifestId: params.get('iiif-content') || params.get('manifest') ,
    }
  ],
};

if (params.get('canvas')) {
    config.windows[0].canvasId = params.get('canvas');
}

// iiif-content -- manifest to show
// Canvas to show
// collection -- to load in the resources panel
// http://localhost:8080/?iiif-content=https://purl.stanford.edu/rd447dz7630/iiif/manifest&canvas=https://purl.stanford.edu/rd447dz7630/iiif/canvas/rd447dz7630_13&collection=
// http://localhost:8080/?collection=http://localhost:8888/collection/github_1969268/inbox.json&manifest=https://purl.stanford.edu/rd447dz7630/iiif/manifest&canvas=https://purl.stanford.edu/rd447dz7630/iiif/canvas/rd447dz7630_4

const plugins = [ ...annotationPlugins ]
//[...annotationPlugins];

let viewer = Mirador.viewer(config, plugins);

if (params.get('collection')) {
    fetch(params.get('collection'), {
        method: 'GET', // or 'PUT'
    })
    .then(response => response.json())
    .then(collection => {
        collection.members.forEach(function (manifest){
            let action = Mirador.actions.addResource(manifest['@id']);
            viewer.store.dispatch(action);
        })
    });
}

