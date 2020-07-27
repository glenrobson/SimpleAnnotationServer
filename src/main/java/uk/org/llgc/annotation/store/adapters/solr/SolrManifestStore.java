package uk.org.llgc.annotation.store.adapters.solr;

import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Canvas;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

import java.io.IOException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SolrManifestStore {
	protected static Logger _logger = LogManager.getLogger(SolrManifestStore.class.getName());

	protected SolrClient _solrClient = null;
    protected SolrUtils _utils = null;

    public SolrManifestStore(final SolrClient pClient) {
        _solrClient = pClient;
        _utils = new SolrUtils(_solrClient);
    }

    protected SolrQuery getManifestQuery() {
		SolrQuery tQuery = new SolrQuery();
		tQuery.setFields("id", "type", "short_id", "label", "canvas");
		tQuery.setRows(1000);

		return tQuery;
	}

	public Manifest getManifest(final String pShortId) throws IOException {
        SolrQuery tQuery = this.getManifestQuery();

		tQuery.set("q", "short_id:" + _utils.escapeChars(pShortId));

		try {
			QueryResponse tResponse  = _solrClient.query(tQuery);

			if (tResponse.getResults().size() == 1) {
                SolrDocument pDoc = tResponse.getResults().get(0);
                // Build helpers to convert from SOLR to annotation
                Manifest tManifest = new Manifest();

                tManifest.setURI((String)pDoc.get("id"));
                tManifest.setShortId((String)pDoc.get("short_id"));
                tManifest.setLabel(((List<String>)pDoc.get("label")).get(0));
                List<String> tCanvasList = ((List<String>)pDoc.get("canvas"));
                for (int i = 0; i < tCanvasList.size(); i +=2) {
                    tManifest.getCanvases().add(new Canvas(tCanvasList.get(i), tCanvasList.get(i + 1)));
                }

                return tManifest;
			} else if (tResponse.getResults().size() == 0) {
				return null; // no annotation found with supplied id
			} else {
				throw new IOException("Found " + tResponse.getResults().size() + " manifests with ID " + pShortId);
			}
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run solr query due to " + tException.toString());
		}
    }

	public void indexManifestNoCheck(final Manifest pManifest) throws IOException {
		SolrInputDocument tDoc = new SolrInputDocument();
		tDoc.addField("id", pManifest.getURI());
		tDoc.addField("short_id", pManifest.getShortId());
		tDoc.addField("label", pManifest.getLabel());
		tDoc.addField("type", pManifest.getType());

        for (Canvas tCanvas : pManifest.getCanvases()) { 
            tDoc.addField("canvas", tCanvas.getId());
            tDoc.addField("canvas", tCanvas.getLabel());
        }

		_utils.addDoc(tDoc, true);
    }
    
	public List<Manifest> getManifests() throws IOException {
        SolrQuery tQuery = this.getManifestQuery();
		tQuery.set("q", "type:sc\\:Manifest");

		List<Manifest> tManifests = new ArrayList<Manifest>();
		try {
			QueryResponse tResponse = _solrClient.query(tQuery);
			long tResultNo = tResponse.getResults().getNumFound();
			int tPageSize = tResponse.getResults().size();
			int tStart = 0;
			do {
				for (SolrDocument tResult : tResponse.getResults()) {
					Manifest tManifest = new Manifest();
					tManifest.setURI((String)tResult.get("id"));
					tManifest.setShortId((String)tResult.get("short_id"));
					tManifest.setLabel(((List<String>)tResult.get("label")).get(0));
					tManifests.add(tManifest);
				}

				tStart += tPageSize;
				tQuery.setStart(tStart);
				tResponse = _solrClient.query(tQuery);
			} while (tStart < tResultNo);
		} catch (SolrServerException tExcpt) {
			tExcpt.printStackTrace();
			throw new IOException("Failed to remove annotations due to " + tExcpt);
		}
		return tManifests;
    }

	public String getManifestId(final String pShortId) throws IOException {
        SolrQuery tQuery = _utils.getQuery();
		tQuery.set("q", "short_id:" + _utils.escapeChars(pShortId));

		try {
			QueryResponse tResponse  = _solrClient.query(tQuery);

			if (tResponse.getResults().size() == 1) {
				return (String)tResponse.getResults().get(0).get("id");
			} else if (tResponse.getResults().size() == 0) {
				return null; // no annotation found with supplied id
			} else {
				throw new IOException("Found " + tResponse.getResults().size() + " manifests with ID " + pShortId);
			}
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run solr query due to " + tException.toString());
		}
    }

	public List<String> getManifestForCanvas(final String pCanvasId) throws IOException {
        try {
			SolrQuery tQuery = _utils.getQuery();
			tQuery.set("q", "canvas:\"" + _utils.escapeChars(pCanvasId) + "\"");
			QueryResponse tResponse = _solrClient.query(tQuery);
			long tResultNo = tResponse.getResults().getNumFound();
			int tPageSize = tResponse.getResults().size();
			int tStart = 0;
			List<String> tWithin = new ArrayList<String>();
			do {
				for (SolrDocument tResult : tResponse.getResults()) {
					tWithin.add((String)tResult.get("id"));
				}

				tStart += tPageSize;
				tQuery.setStart(tStart);
				tResponse = _solrClient.query(tQuery);
			} while (tStart < tResultNo);
			if (tWithin.size() > 0) {
				return tWithin;
			} else {
				return null;
			}
		} catch (SolrServerException tException) {
			_logger.error("failed to find manifest for this canvas due to: " + tException.toString());
			throw new IOException("Failed to find manifest for canvasid " + pCanvasId + " due to " + tException.toString());
		}
    }
}
