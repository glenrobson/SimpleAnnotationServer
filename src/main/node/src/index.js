import Mirador from 'mirador/dist/es/src/index';
import annotationPlugins from 'mirador-annotations/es/';
import SimpleAnnotationServerV2Adapter from 'mirador-annotations/es/SimpleAnnotationServerV2Adapter';
import LocalStorageAdapter from 'mirador-annotations/es/LocalStorageAdapter';

const config = {
  id: 'mirador-viewer',
  annotation: {
   // adapter: (canvasId) => new SimpleAnnotationServerV2Adapter(canvasId, 'annotation'),
     adapter: (canvasId) => new LocalStorageAdapter(`localStorage://?canvasId=${canvasId}`),
  },
  window: {
    defaultSideBarPanel: 'annotations',
    sideBarOpenByDefault: true,
  },
  windows: [
    {
      manifestId: 'https://damsssl.llgc.org.uk/iiif/2.0/1132285/manifest.json',
    }
  ],
};

const plugins = [ ...annotationPlugins ]
//[...annotationPlugins];

Mirador.viewer(config, plugins);
