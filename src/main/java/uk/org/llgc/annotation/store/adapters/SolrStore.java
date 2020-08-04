package uk.org.llgc.annotation.store.adapters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.jena.rdf.model.Model;

import java.net.URISyntaxException;

import com.github.jsonldjava.utils.JsonUtils;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.adapters.solr.SolrManifestStore;
import uk.org.llgc.annotation.store.adapters.solr.SolrUtils;

import java.text.ParseException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Base64;
import java.util.Date;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;

import com.github.jsonldjava.utils.JsonUtils;

public class SolrStore extends AbstractStoreAdapter implements StoreAdapter {
	protected static Logger _logger = LogManager.getLogger(SolrStore.class.getName());

	protected SolrClient _solrClient = null;
    protected SolrManifestStore _manifestStore = null;
    protected SolrUtils _utils = null;

	public SolrStore(final String pConnectionURL, final String pCollection) {
		if (pCollection == null || pCollection.trim().length() == 0) {
			_solrClient = (new  HttpSolrClient.Builder(pConnectionURL)).build();
		} else {
			//_solrClient = new CloudSolrClient.Builder().withZkHost(pConnectionURL).build();
            List<String> tHosts = new ArrayList<String>();
            if (pConnectionURL.contains(",")) {
                String[] tHostUrls = pConnectionURL.split(",");
                for (int i = 0;i < tHostUrls.length; i++) {
                    tHosts.add(tHostUrls[i]);
                }
            } else {
                tHosts.add(pConnectionURL);
            }
			_solrClient = new CloudSolrClient.Builder(tHosts).build();
			((CloudSolrClient)_solrClient).setDefaultCollection(pCollection);
		}
        _manifestStore = new SolrManifestStore(_solrClient);
        _utils = new SolrUtils(_solrClient);
	}

	public SolrClient getClient() {
		return _solrClient;
	}

// id, motivation, body, target, selector, within, data, short_id, label
	public Model addAnnotationSafe(final Map<String,Object> pJson) throws IOException {
		SolrInputDocument tDoc = new SolrInputDocument();

		// Only index what is neccesary for searching, everything else goes into data
		tDoc.addField("id", (String)pJson.get("@id"));
		_utils.addMultiple(tDoc, "type", pJson.get("@type"));
		_utils.addMultiple(tDoc, "motivation", pJson.get("motivation"));
		Date tCreated = null;
		try {
			if (pJson.get("created") != null) {
					tCreated = _dateFormatter.parse((String)pJson.get("created"));
			} else if (pJson.get("http://purl.org/dc/terms/created") != null) {
				tCreated = _dateFormatter.parse((String)pJson.get("http://purl.org/dc/terms/created"));
			}
		} catch (ParseException tException) {
			_logger.error("Failed to parse Created date");
		}
		_utils.addSingle(tDoc, "created", tCreated);
		Date tModified = null;
		try {
			if (pJson.get("modified") != null) {
				tModified = _dateFormatter.parse((String)pJson.get("modified"));
			} else if (pJson.get("http://purl.org/dc/terms/modified") != null) {
				tModified = _dateFormatter.parse((String)pJson.get("http://purl.org/dc/terms/modified"));
			}
		} catch (ParseException tException) {
			_logger.error("Failed to parse Modified date");
		}

		_utils.addSingle(tDoc, "modified", tModified);
		if (pJson.get("resource") != null) {
			if (pJson.get("resource") instanceof List) {
				List<Map<String,Object>> tMultpleBodies = (List<Map<String,Object>>)pJson.get("resource");
				for (Map<String,Object> tBody : tMultpleBodies) {
					_utils.addSingle(tDoc, "body", tBody.get("chars"));
				}
			} else {
				Map<String, Object> tResouce = (Map<String,Object>)pJson.get("resource");
				_utils.addSingle(tDoc, "body", tResouce.get("chars"));
			}
		}
		String tCanvasId = null;
		if (pJson.get("on") != null) {
			if (pJson.get("on") instanceof Map) {
				Map<String, Object> tOn = (Map<String,Object>)pJson.get("on");
				List<Map<String,Object>> tListOn = new ArrayList<Map<String,Object>>();
				tListOn.add(tOn);
				pJson.put("on",tListOn);
			}
			for (Map<String, Object> tOn : (List<Map<String,Object>>)pJson.get("on"))	{
				// for each linked canvas add link to canvas and manifest
				if (tOn.get("full") instanceof String) {
					_utils.addSingle(tDoc, "target", tOn.get("full"));
				} else {
					_logger.info("Probably have an invalid annotation ");
					_logger.info(JsonUtils.toPrettyString(pJson));
				}
				_utils.addSingle(tDoc, "target", tOn.get("source"));
                Canvas tCanvas = new Canvas((String)tDoc.get("target").getValue(), "");
                _utils.addMultiple(tDoc, "short_id", tCanvas.getShortId());
				if (tOn.get("selector") != null) { // index xywh in case in future you want to search within bounds
					Map<String,Object> tSelector = (Map<String, Object>)tOn.get("selector");
					if (tSelector.get("value") instanceof String) {
						_utils.addSingle(tDoc, "selector", tSelector.get("value"));
					} else {
						_logger.info("Probably have an invalid annotation ");
						_logger.info(JsonUtils.toPrettyString(pJson));
					}
				}
				// by the time it gets here the annotation should have a within if there is a manifest avilable.
				if (tOn.get("within") != null) {
					String tWithinURi = "";
					if (tOn.get("within") instanceof Map) {
						tWithinURi = (String)((Map<String,Object>)tOn.get("within")).get("@id");
					} else {
						tWithinURi = (String)tOn.get("within");
					}
					_utils.addSingle(tDoc, "within", tWithinURi);
				} else {
					_logger.debug("Missing canvas Id so couldn't find registered manifest");
				}
			}
		}
		String tJson = JsonUtils.toString(pJson);
		_utils.addSingle(tDoc, "data", Base64.getEncoder().encodeToString(tJson.getBytes("UTF-8")));
		//_utils.addSingle(tDoc, "data", DatatypeConverter.printBase64Binary(tJson.getBytes("UTF-8")));

		_utils.addDoc(tDoc, true);

		return super.convertAnnoToModel(pJson);
	}

	public Map<String, Object> getAllAnnotations() throws IOException {
		SolrQuery tQuery = _utils.getQuery();
		tQuery.set("q", "type:oa\\:Annotation");

		Map<String,Object> tAnnoList = new HashMap<String,Object>();
		tAnnoList.put("@context", "http://iiif.io/api/presentation/2/context.json");
		tAnnoList.put("@type", "sc:AnnotationList");

		List tResources = new ArrayList();
		tAnnoList.put("resources", tResources);
		try {
			QueryResponse tResponse = _solrClient.query(tQuery);
			long tResultNo = tResponse.getResults().getNumFound();
			int tPageSize = tResponse.getResults().size();
			int tStart = 0;
			do {
				tResources.addAll(this.buildAnnotationList(tResponse, false));

				tStart += tPageSize;
				tQuery.setStart(tStart);
				tResponse = _solrClient.query(tQuery);
			} while (tStart < tResultNo);
		} catch (SolrServerException tExcpt) {
			tExcpt.printStackTrace();
			throw new IOException("Failed to remove annotations due to " + tExcpt);
		}
		return tAnnoList;

	}

    public void linkupOrphanCanvas(final Manifest pManifest) throws IOException {
        try {
            for (Canvas tCanvas : pManifest.getCanvases()) {
                // Update any annotations that link to this new Manifest
                SolrQuery tQuery = _utils.getQuery();
                tQuery.set("q", "target:\"" + _utils.escapeChars(tCanvas.getId()) + "\" AND NOT within:\"" + _utils.escapeChars(pManifest.getURI()) + "\"");
                QueryResponse tResponse = _solrClient.query(tQuery);
                long tResultNo = tResponse.getResults().getNumFound();
                int tPageSize = tResponse.getResults().size();
                int tStart = 0;
                if (tPageSize == 0) {
                    _logger.debug("no annotations to update");
                } else {
                    _logger.debug("Found " + tResponse.getResults().size() + " annotations to update with within");
                    do {
                        for (SolrDocument tResult : tResponse.getResults()) {
                            Map<String,Object> tAnnoJson =  this.buildAnnotation(tResult, false);

                            super.addWithin(tAnnoJson, pManifest.getURI(), tCanvas.getId());

                            try {
                                super.updateAnnotation(tAnnoJson);
                            } catch (MalformedAnnotation tExcpt) {
                                throw new IOException("Failed to reload annotation after updating the within: " + tExcpt);
                            }
                        }

                        tStart += tPageSize;
                        tQuery.setStart(tStart);
                        tResponse = _solrClient.query(tQuery);
                    } while (tStart < tResultNo);
                }
            }
        } catch (SolrServerException tExcpt) {
            String tMessage = "Failed to update canvases with link to manifest due to " + tExcpt.toString();
            _logger.debug(tMessage);
            throw new IOException(tMessage);
        }
    }



	public List<String> getManifestForCanvas(final String pCanvasId) throws IOException {
		return _manifestStore.getManifestForCanvas(pCanvasId);
	}

	public List<Manifest> getManifests() throws IOException {
        return _manifestStore.getManifests();
    }

	public List<Manifest> getSkeletonManifests() throws IOException {
        return _manifestStore.getSkeletonManifests();
    }

	protected String indexManifestNoCheck(final String pShortId, Map<String,Object> pManifest) throws IOException {
        Manifest tManifest = new Manifest(pManifest, pShortId);
        _manifestStore.indexManifestNoCheck(tManifest);
        this.linkupOrphanCanvas(tManifest);
        return tManifest.getShortId();
    }

	public String getManifestId(final String pShortId) throws IOException {
		return _manifestStore.getManifestId(pShortId);
	}

	public Manifest getManifest(final String pShortId) throws IOException {
		return _manifestStore.getManifest(pShortId);
	}

    public Canvas resolveCanvas(final String pShortId) throws IOException {
        SolrQuery tQuery = _utils.getQuery();
		tQuery.set("q", "short_id:\"" + pShortId + "\"");
        try {
			QueryResponse tResponse = _solrClient.query(tQuery);
            if (tResponse.getResults().isEmpty()) {
                // Failed to find Canvas
                return null;
            }
            SolrDocument tResult = tResponse.getResults().get(0);
            String tLabel = "";
            if (tResult.get("label") != null) {
                tLabel = (String)tResult.get("label");
            }
            Canvas tCanvas = new Canvas((String)tResult.get("target"), tLabel);
            tCanvas.setShortId(pShortId);
            if (tResult.get("label") != null) {
                tCanvas.setLabel((String)tResult.get("label"));
            }
            return tCanvas;
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run solr query due to " + tException.toString());
		}
    }

    public void storeCanvas(final Canvas pCanvas) throws IOException {
        SolrQuery tQuery = _utils.getQuery();
		tQuery.set("q", "NOT short_id:* AND target:\"" + pCanvas.getId() + "\"");

        try {
			QueryResponse tResponse = _solrClient.query(tQuery);
            for (SolrDocument tResult : tResponse.getResults()) {
                Map<String,Object> tAnnoJson =  this.buildAnnotation(tResult, false);
                // This will add the canvas short_id to old annotations that don't have it
                super.updateAnnotation(tAnnoJson);
            }
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run solr query due to " + tException.toString());
        } catch (MalformedAnnotation tException) {
			throw new IOException("Failed to run solr query due to " + tException.toString());
		}
    }

	public void deleteAnnotation(final String pAnnoId) throws IOException {
		List<String> tOldIds = new ArrayList<String>();
		tOldIds.add(pAnnoId);
		try {
			_solrClient.deleteById(tOldIds);
			_solrClient.commit();
		} catch(SolrServerException tException) {
			tException.printStackTrace();
			throw new IOException("Failed to remove annotations due to " + tException);
		}
	}

	public Map<String, Object> search(final SearchQuery pQuery) throws IOException {
		StringBuffer tSolrQuery = new StringBuffer("text:\"");
		tSolrQuery.append(pQuery.getQuery());
		tSolrQuery.append("\"");
		if (pQuery.getMotivations() != null && !pQuery.getMotivations().isEmpty()) {
			tSolrQuery.append(" AND motivation:");
			if (pQuery.getMotivations().size() == 1) {
				tSolrQuery.append("\"");
				tSolrQuery.append(pQuery.getMotivations().get(0));
				tSolrQuery.append("\"");
			} else {
				tSolrQuery.append("(");
				for (String tMotivation : pQuery.getMotivations()) {
					tSolrQuery.append(" ");
					tSolrQuery.append(tMotivation);
				}
				tSolrQuery.append(")");
			}
		}

		tSolrQuery.append(" AND within:\"");
		tSolrQuery.append(pQuery.getScope());
		tSolrQuery.append("\"");

		SolrQuery tQuery = _utils.getQuery();
		tQuery.set("q", tSolrQuery.toString());
		tQuery.setStart(pQuery.getPage() * pQuery.getResultsPerPage());
		tQuery.setRows(pQuery.getResultsPerPage());
        tQuery.setHighlight(true);
        tQuery.addHighlightField("text");

		Map<String,Object> tAnnoList = new HashMap<String,Object>();
		tAnnoList.put("@type","sc:AnnotationList");
		List<Map<String, Object>> tAnnotations = new ArrayList<Map<String,Object>>();
		tAnnoList.put("resources", tAnnotations);

		try {
			tAnnoList.put("@id",pQuery.toURI().toString());
			QueryResponse tResponse = _solrClient.query(tQuery);
			long tResultNo = tResponse.getResults().getNumFound();
			int tNumberOfPages = (int)(tResultNo / pQuery.getResultsPerPage());
			tAnnotations.addAll(this.buildAnnotationList(tResponse, true));

            // Adding page count even if results are smaller than one page.
            Map<String,String> tWithin = new HashMap<String,String>();
            tAnnoList.put("within",tWithin);
            tWithin.put("@type","sc:Layer");
            tWithin.put("total","" + tResultNo);
			if (tResultNo > pQuery.getResultsPerPage()) { // if paginating
				int tPageNo = pQuery.getPage();
                tAnnoList.put("startIndex", tPageNo);
				if (tNumberOfPages != pQuery.getPage()) { // not on last page
					int tPage = tPageNo + 1;
					pQuery.setPage(tPage);
					tAnnoList.put("next",pQuery.toURI().toString());
				}
				pQuery.setPage(0);
				tWithin.put("first", pQuery.toURI().toString());
				pQuery.setPage(tNumberOfPages);
				tWithin.put("last", pQuery.toURI().toString());
			} else {
                tAnnoList.put("startIndex", 0);
            }

		} catch (SolrServerException tException) {
			tException.printStackTrace();
			throw new IOException("Failed to run solr query due to " + tException.toString());
		} catch (URISyntaxException tException) {
			tException.printStackTrace();
			throw new IOException("Failed to work with base URI " + tException.toString());
		}

		return tAnnoList;
	}

	public List<Model> getAnnotationsFromPage(final String pPageId) throws IOException {
		SolrQuery tQuery = _utils.getQuery();
		tQuery.set("q", "target:" + _utils.escapeChars(pPageId));

		try {
			QueryResponse tResponse = _solrClient.query(tQuery);
			long tResultNo = tResponse.getResults().getNumFound();
			int tPageSize = tResponse.getResults().size();
			int tStart = 0;
			List<Map<String,Object>> tAnnotations = new ArrayList<Map<String,Object>>();
			do { // this gets all of the pages of results and creates one list of annotations which isn't going to scale
				// would need to fix this by implementing paging.
				tAnnotations.addAll(this.buildAnnotationList(tResponse, false));

				tStart += tPageSize;
				tQuery.setStart(tStart);
				tResponse = _solrClient.query(tQuery);
			} while (tStart < tResultNo);
			List<Model> tAsModel = new ArrayList<Model>();
			for (Map<String, Object> tAnno : tAnnotations) {
				tAsModel.add(super.convertAnnoToModel(tAnno));
			}
			return tAsModel;
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run solr query due to " + tException.toString());
		}
	}

	protected Model getNamedModel(final String pContext) throws IOException {
		SolrQuery tQuery = _utils.getQuery();

		tQuery.set("q", "id:" + _utils.escapeChars(pContext));

		try {
			QueryResponse tResponse  = _solrClient.query(tQuery);

			if (tResponse.getResults().size() == 1) {
				Map<String, Object> tAnnotation = this.buildAnnotation(tResponse.getResults().get(0), false);
				return super.convertAnnoToModel(tAnnotation);
			} else if (tResponse.getResults().size() == 0) {
				return null; // no annotation found with supplied id
			} else {
				throw new IOException("Found " + tResponse.getResults().size() + " annotations with ID " + pContext);
			}
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run solr query due to " + tException.toString());
		}
	}

	public List<PageAnnoCount> listAnnoPages(final Manifest pManifest) throws IOException {
        SolrQuery tQuery = new SolrQuery();
        tQuery.setRows(0);
        tQuery.setFacet(true);
        tQuery.addFacetField("target");
        tQuery.setFacetLimit(-1);
        tQuery.setFacetMinCount(1);
        tQuery.setFacetSort("index");
        tQuery.set("q", "type:\"oa:Annotation\" AND within:\"" + pManifest.getURI() + "\"");
        try {
            QueryResponse tResponse = _solrClient.query(tQuery);
            long tTotalAnnos = tResponse.getResults().getNumFound();
            FacetField tFacetCounts = tResponse.getFacetField("target");
            List<PageAnnoCount> tAnnoPageCount = new ArrayList<PageAnnoCount>();
            for (FacetField.Count tFacetValue : tFacetCounts.getValues()) {
                String tLabel = "";
                if (pManifest.getCanvas(tFacetValue.getName()) != null) {
                    tLabel = pManifest.getCanvas(tFacetValue.getName()).getLabel();
                }
                Canvas tCanvas = new Canvas(tFacetValue.getName(), tLabel);
                this.storeCanvas(tCanvas);
                tAnnoPageCount.add(new PageAnnoCount(tCanvas, (int)tFacetValue.getCount(), pManifest)); 
            }
            return tAnnoPageCount;
        } catch (SolrServerException tExcept) {
            tExcept.printStackTrace();
            throw new IOException("Failed to run page count query due to " + tExcept.getMessage());
        }
    }    

	public List<PageAnnoCount> listAnnoPages() throws IOException {
        SolrQuery tQuery = new SolrQuery();
        tQuery.setRows(0);
        tQuery.setFacet(true);
        tQuery.addFacetField("target");
        tQuery.setFacetLimit(-1);
        tQuery.setFacetSort("index");
        tQuery.set("q", "type:oa\\:Annotation");
        try {
            QueryResponse tResponse = _solrClient.query(tQuery);
            long tTotalAnnos = tResponse.getResults().getNumFound();
            FacetField tFacetCounts = tResponse.getFacetField("target");
            List<PageAnnoCount> tAnnoPageCount = new ArrayList<PageAnnoCount>();
            for (FacetField.Count tFacetValue : tFacetCounts.getValues()) {
                Canvas tCanvas = new Canvas(tFacetValue.getName(), "");// TODO add manifest and canvas label
                tAnnoPageCount.add(new PageAnnoCount(tCanvas, (int)tFacetValue.getCount(), null)); 
            }
            return tAnnoPageCount;
        } catch (SolrServerException tExcept) {
            tExcept.printStackTrace();
            throw new IOException("Failed to run page count query due to " + tExcept.getMessage());
        }
	}

	public Map<String,Object> buildAnnotation(final SolrDocument pDoc, final boolean pCollapseOn) throws IOException {
		//Map<String,Object> tAnnotation = (Map<String,Object>)JsonUtils.fromString(new String(DatatypeConverter.parseBase64Binary((String)pDoc.get("data"))));
		Map<String,Object> tAnnotation = (Map<String,Object>)JsonUtils.fromString(new String(Base64.getDecoder().decode((String)pDoc.get("data"))));

		if (pCollapseOn) {
			_annoUtils.colapseFragement(tAnnotation);
			tAnnotation.put("@context", _annoUtils.getExternalContext());
		}
		return tAnnotation;
	}

	public List<Map<String,Object>> buildAnnotationList(final QueryResponse pResponse, final boolean pCollapseOn) throws IOException {
		List<Map<String,Object>> tResults = new ArrayList<Map<String,Object>>();
		for (SolrDocument tResult : pResponse.getResults()) {
            Map<String, Object> tAnno = this.buildAnnotation(tResult, pCollapseOn);
			tResults.add(tAnno);
            if (pResponse.getHighlighting() != null && pResponse.getHighlighting().get(tAnno.get("@id")) != null) {
                // Add snippet as label to annotation
                if ( pResponse.getHighlighting().get(tAnno.get("@id")).get("text") != null) {
                    List<String> snippets = pResponse.getHighlighting().get(tAnno.get("@id")).get("text");

                    tAnno.put("label", snippets.get(0).replaceAll("<[ /]*[a-zA-Z0-9 ]*[ /]*>", ""));
                } else {
                    tAnno.put("label", ((List<String>)tResult.get("body")).get(0).replaceAll("<[ /]*[a-zA-Z0-9 ]*[ /]*>", ""));
                }
            }
		}

		return tResults;
	}


}
