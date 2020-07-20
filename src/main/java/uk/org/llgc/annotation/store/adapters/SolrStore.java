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
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;

import java.text.ParseException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Base64;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;

import com.github.jsonldjava.utils.JsonUtils;

public class SolrStore extends AbstractStoreAdapter implements StoreAdapter {
	protected static Logger _logger = LogManager.getLogger(SolrStore.class.getName());

	protected SolrClient _solrClient = null;

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
	}

	public SolrClient getClient() {
		return _solrClient;
	}

// id, motivation, body, target, selector, within, data, short_id, label
	public Model addAnnotationSafe(final Map<String,Object> pJson) throws IOException {
		SolrInputDocument tDoc = new SolrInputDocument();

		// Only index what is neccesary for searching, everything else goes into data
		tDoc.addField("id", (String)pJson.get("@id"));
		this.addMultiple(tDoc, "type",pJson.get("@type"));
		this.addMultiple(tDoc, "motivation", pJson.get("motivation"));
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
		this.addSingle(tDoc, "created", tCreated);
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

		this.addSingle(tDoc, "modified", tModified);
		if (pJson.get("resource") != null) {
			if (pJson.get("resource") instanceof List) {
				List<Map<String,Object>> tMultpleBodies = (List<Map<String,Object>>)pJson.get("resource");
				for (Map<String,Object> tBody : tMultpleBodies) {
					this.addSingle(tDoc, "body", tBody.get("chars"));
				}
			} else {
				Map<String, Object> tResouce = (Map<String,Object>)pJson.get("resource");
				this.addSingle(tDoc, "body", tResouce.get("chars"));
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
					this.addSingle(tDoc, "target", tOn.get("full"));
				} else {
					_logger.info("Probably have an invalid annotation ");
					_logger.info(JsonUtils.toPrettyString(pJson));
				}
				this.addSingle(tDoc, "target", tOn.get("source"));
				if (tOn.get("selector") != null) { // index xywh in case in future you want to search within bounds
					Map<String,Object> tSelector = (Map<String, Object>)tOn.get("selector");
					if (tSelector.get("value") instanceof String) {
						this.addSingle(tDoc, "selector", tSelector.get("value"));
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
					this.addSingle(tDoc, "within", tWithinURi);
				} else {
					_logger.debug("Missing canvas Id so couldn't find registered manifest");
				}
			}
		}
		String tJson = JsonUtils.toString(pJson);
		//this.addSingle(tDoc, "data", Base64.getEncoder().encodeToString(tJson.getBytes("UTF-8")));
		this.addSingle(tDoc, "data", DatatypeConverter.printBase64Binary(tJson.getBytes("UTF-8")));

		this.addDoc(tDoc, true);

		return super.convertAnnoToModel(pJson);
	}


	public List<String> getManifestForCanvas(final String pCanvasId) throws IOException {
		try {
			SolrQuery tQuery = this.getQuery();
			tQuery.set("q", "canvas:\"" + this.escapeChars(pCanvasId) + "\"");
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



	public Map<String, Object> getAllAnnotations() throws IOException {
		SolrQuery tQuery = this.getQuery();
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


	protected String indexManifestNoCheck(final String pShortId, Map<String,Object> pManifest) throws IOException {
		String tManifestId = (String)pManifest.get("@id");
		SolrInputDocument tDoc = new SolrInputDocument();
		tDoc.addField("short_id", pShortId);
		tDoc.addField("id", tManifestId);
		this.addSingle(tDoc, "label", pManifest.get("label"));
		this.addMultiple(tDoc, "type", pManifest.get("@type"));
		List<String> tCanvases = new ArrayList<String>();
		for (Map<String,Object> tSequence : (List<Map<String,Object>>)pManifest.get("sequences")) {
			for (Map<String,Object> tCanvas : (List<Map<String,Object>>)tSequence.get("canvases")) {
				if (!tCanvases.contains((String)tCanvas.get("@id"))) {
					tCanvases.add((String)tCanvas.get("@id"));
					// Update any annotations that link to this new Manifest
					SolrQuery tQuery = this.getQuery();
					tQuery.set("q", "target:\"" + this.escapeChars((String)tCanvas.get("@id")) + "\" AND NOT within:\"" + this.escapeChars(tManifestId) + "\"");
					try {
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

                                    String tCanvasId = (String)tCanvas.get("@id");
                    				super.addWithin(tAnnoJson, tManifestId, tCanvasId);

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
					} catch (SolrServerException tExcpt) {
						String tMessage = "Failed to update canvases with link to manifest due to " + tExcpt.toString();
						_logger.debug(tMessage);
						throw new IOException(tMessage);
					}

				}
			}
		}
		this.addMultiple(tDoc, "canvas", tCanvases);

		this.addDoc(tDoc, true);

		// Now need to search for all canvases and add the within link
		return pShortId;
	}
	public String getManifestId(final String pShortId) throws IOException {
		SolrQuery tQuery = this.getQuery();
		tQuery.set("q", "short_id:" + this.escapeChars(pShortId));

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
	public Map<String,Object> getManifest(final String pShortId) throws IOException {
		SolrQuery tQuery = this.getQuery();

		tQuery.set("q", "short_id:" + this.escapeChars(pShortId));

		try {
			QueryResponse tResponse  = _solrClient.query(tQuery);

			if (tResponse.getResults().size() == 1) {
				Map<String, Object> tAnnotation = this.buildManifest(tResponse.getResults().get(0));
				return tAnnotation;
			} else if (tResponse.getResults().size() == 0) {
				return null; // no annotation found with supplied id
			} else {
				throw new IOException("Found " + tResponse.getResults().size() + " manifests with ID " + pShortId);
			}
		} catch (SolrServerException tException) {
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

		SolrQuery tQuery = this.getQuery();
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
		SolrQuery tQuery = this.getQuery();
		tQuery.set("q", "target:" + this.escapeChars(pPageId));

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
		SolrQuery tQuery = this.getQuery();

		tQuery.set("q", "id:" + this.escapeChars(pContext));

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
        tQuery.setFacetSort("index");
        tQuery.set("q", "type:oa\\:Annotation AND within:" + pManifest.getURI().replaceAll(":","\\\\:"));
        try {
            QueryResponse tResponse = _solrClient.query(tQuery);
            long tTotalAnnos = tResponse.getResults().getNumFound();
            FacetField tFacetCounts = tResponse.getFacetField("target");
            List<PageAnnoCount> tAnnoPageCount = new ArrayList<PageAnnoCount>();
            for (FacetField.Count tFacetValue : tFacetCounts.getValues()) {
                tAnnoPageCount.add(new PageAnnoCount(tFacetValue.getName(), (int)tFacetValue.getCount()));
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
                tAnnoPageCount.add(new PageAnnoCount(tFacetValue.getName(), (int)tFacetValue.getCount()));
            }
            return tAnnoPageCount;
        } catch (SolrServerException tExcept) {
            tExcept.printStackTrace();
            throw new IOException("Failed to run page count query due to " + tExcept.getMessage());
        }
	}

	// Solr Helper methods
	protected void addDoc(final SolrInputDocument pDoc, final boolean pCommit) throws IOException {
		try {
			UpdateResponse tResponse = _solrClient.add(pDoc);

			if (pCommit) {
				_solrClient.commit();
			}
		} catch (SolrServerException tExcpt) {
			tExcpt.printStackTrace();
			throw new IOException("Failed to remove annotations due to " + tExcpt);
		}
	}


	protected void addSingle(final SolrInputDocument pDoc, final String pKey, final Object pObject) {
		if (pObject != null) {
			if (pObject instanceof String) {
				pDoc.addField(pKey, (String)pObject);
			} else {
				pDoc.addField(pKey, pObject);
			}
		}
	}

	protected void addMultiple(final SolrInputDocument pDoc, final String pKey, final Object pObject) {
		if (pObject != null) {
			if (pObject instanceof List) {
				for (String tValue: (List<String>)pObject) {
					pDoc.addField(pKey, tValue);
				}
			} else {
				pDoc.addField(pKey, (String)pObject);
			}
		}
	}

	protected SolrQuery getManifestQuery() {
		SolrQuery tQuery = new SolrQuery();
		tQuery.setFields("id", "type", "short_id", "label", "canvas");
		tQuery.setRows(1000);

		return tQuery;
	}

	protected SolrQuery getQuery() {
		SolrQuery tQuery = new SolrQuery();
		tQuery.setFields("id", "type", "motivation","body", "target","selector", "short_id", "within", "data", "canvas");
		tQuery.setRows(1000);

		return tQuery;
	}

	protected String escapeChars(final String pParam) {
		return pParam.replaceAll(":","\\\\:");
	}

	protected boolean isType(final SolrDocument pResult, final String pType) {
		if (pResult.get("type") != null) {
			if (pResult.get("type") instanceof List) {
				List<String> tTypes = (List<String>)pResult.get("type");
				return tTypes.contains(pType);
			} else {
				return ((String)pResult.get("type")).equals(pType);
			}
		} else {
			return false;
		}
	}

	// Build helpers to convert from SOLR to annotation
	protected Map<String,Object> buildManifest(final SolrDocument pDoc) {
		Map<String,Object> tManifest = new HashMap<String,Object>();

		tManifest.put("@id", (String)pDoc.get("id"));
		tManifest.put("short_id", (String)pDoc.get("short_id"));
		tManifest.put("label", (String)pDoc.get("label"));
		this.createItemListIfMultiple(tManifest, "@type", pDoc.get("type"));
		List tSequence = new ArrayList();
		tManifest.put("sequences",tSequence);
		Map<String,Object> tFirstSequence = new HashMap<String,Object>();
		tSequence.add(tFirstSequence);
		this.createItemList(tFirstSequence, "canvases", pDoc.get("canvases"));

		return tManifest;
	}

	protected Map<String,Object> buildAnnotation(final SolrDocument pDoc, final boolean pCollapseOn) throws IOException {
		Map<String,Object> tAnnotation = (Map<String,Object>)JsonUtils.fromString(new String(DatatypeConverter.parseBase64Binary((String)pDoc.get("data"))));
		//Map<String,Object> tAnnotation = (Map<String,Object>)JsonUtils.fromString(new String(Base64.getDecoder().decode((String)pDoc.get("data"))));

		if (pCollapseOn) {
			_annoUtils.colapseFragement(tAnnotation);
			tAnnotation.put("@context", _annoUtils.getExternalContext());
		}
		return tAnnotation;
	}

	protected void addSingleJson(final Map<String,Object> pParent, final String pKey, final Object pValue) {
		if (pValue != null) {
			pParent.put(pKey, (String)pValue);
		}
	}

	protected void createItemListIfMultiple(final Map<String,Object> pParent, final String pKey, final Object pValue) {
		if (pValue != null) {
			if (pValue instanceof List) {
				List<String> tValues = (List<String>)pValue;
				if (tValues.size() == 1) {
					pParent.put(pKey, tValues.get(0));
				} else {
					this.createItemList(pParent, pKey, pValue);
				}
			} else {
				pParent.put(pKey, (String)pValue);
			}
		}
	}

	protected void createItemList(final Map<String,Object> pParent, final String pKey, final Object pValue) {
		if (pValue != null) {
			List<String> tList = new ArrayList<String>();
			if (pValue instanceof List) {
				for (String tValueInst : (List<String>)pValue) {
					tList.add(tValueInst);
				}
			} else {
				tList.add((String)pValue);
			}
			pParent.put(pKey, tList);
		}
	}

	protected List<Map<String,Object>> buildAnnotationList(final QueryResponse pResponse, final boolean pCollapseOn) throws IOException {
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
