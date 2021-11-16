package uk.org.llgc.annotation.store.adapters.solr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.IIIFSearchResults;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.Collection;
import uk.org.llgc.annotation.store.data.AnnoListNav;
import uk.org.llgc.annotation.store.data.Body;
import uk.org.llgc.annotation.store.data.Target;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.users.LocalUser;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.adapters.AbstractStoreAdapter;

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
	public Annotation addAnnotationSafe(final Annotation pAnno) throws IOException {
		SolrInputDocument tDoc = new SolrInputDocument();
		// Only index what is neccesary for searching, everything else goes into data
		tDoc.addField("id", pAnno.getId());
		tDoc.addField("type", pAnno.getType());
		_utils.addMultiple(tDoc, "motivation", pAnno.getMotivations());
		tDoc.addField("created", pAnno.getCreated());
		tDoc.addField("modified", pAnno.getModified());
        if (pAnno.getCreator() != null) {
            tDoc.addField("creator", pAnno.getCreator().getId());
        }

        for (Body tBody : pAnno.getBodies()) {
            tDoc.addField("body", tBody.getIndexableContent());
        }

        for (Target tTarget : pAnno.getTargets()) {
            Canvas tCanvas = tTarget.getCanvas();
            tDoc.addField("target", tCanvas.getId());
            tDoc.addField("short_id", tCanvas.getShortId());
            if (tTarget.getManifest() != null) {
                tDoc.addField("within", tTarget.getManifest().getURI());
            }
        }
		String tJson = JsonUtils.toString(pAnno.toJson());
		tDoc.addField("data", Base64.getEncoder().encodeToString(tJson.getBytes("UTF-8")));

		_utils.addDoc(tDoc, true);

		return pAnno;
	}

	public AnnotationList getAllAnnotations() throws IOException {
		SolrQuery tQuery = _utils.getQuery();
		tQuery.set("q", "type:oa\\:Annotation");

        AnnotationList tAnnoList = new AnnotationList();
		try {
			QueryResponse tResponse = _solrClient.query(tQuery);
			long tResultNo = tResponse.getResults().getNumFound();
			int tPageSize = tResponse.getResults().size();
			int tStart = 0;
			do {
				tAnnoList.getAnnotations().addAll(this.buildAnnotationList(tResponse, false));

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
                            Annotation tAnnoJson =  this.buildAnnotation(tResult, false);

                            tAnnoJson.addWithin(pManifest, tCanvas);

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



	public Manifest getManifestForCanvas(final Canvas pCanvas) throws IOException {
		return _manifestStore.getManifestForCanvas(pCanvas.getId());
	}

	public List<Manifest> getManifests() throws IOException {
        return _manifestStore.getManifests();
    }

	public List<Manifest> getSkeletonManifests(final User pUser) throws IOException {
        return _manifestStore.getSkeletonManifests(pUser);
    }

	protected String indexManifestNoCheck(final String pShortId, final Manifest pManifest) throws IOException {
        pManifest.setShortId(pShortId);
        _manifestStore.indexManifestNoCheck(pManifest);
        this.linkupOrphanCanvas(pManifest);
        return pManifest.getShortId();
    }

	public String getManifestId(final String pShortId) throws IOException {
		return _manifestStore.getManifestId(pShortId);
	}

	public Manifest getManifest(final String pId) throws IOException {
		return _manifestStore.getManifest(pId);
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
                Annotation tAnnoJson =  this.buildAnnotation(tResult, false);
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

	public IIIFSearchResults search(final SearchQuery pQuery) throws IOException {
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
        if (pQuery.getUsers() != null && !pQuery.getUsers().isEmpty()) {
            StringBuffer tUserQuery = new StringBuffer();
            tUserQuery.append(" AND creator:");
            boolean tFoundAdmin = false;
			if (pQuery.getUsers().size() == 1) {
				tUserQuery.append("\"");
				tUserQuery.append(pQuery.getUsers().get(0).getId());
				tUserQuery.append("\"");
                tFoundAdmin = pQuery.getUsers().get(0).isAdmin();
			} else {
				tUserQuery.append("(");
				for (User tUser : pQuery.getUsers()) {
					tUserQuery.append(" ");
					tUserQuery.append(tUser.getId());
                    if (tUser.isAdmin()) {
                        tFoundAdmin = true;
                    }
				}
				tUserQuery.append(")");
			}
            if (!tFoundAdmin) {
                tSolrQuery.append(tUserQuery);
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

        IIIFSearchResults tAnnoList = null;
		try {
            tAnnoList = new IIIFSearchResults(pQuery.toURI());
			QueryResponse tResponse = _solrClient.query(tQuery);
			long tResultNo = tResponse.getResults().getNumFound();
			int tNumberOfPages = (int)(tResultNo / pQuery.getResultsPerPage());
			tAnnoList.getAnnotations().addAll(this.buildAnnotationList(tResponse, true));

            // Adding page count even if results are smaller than one page.
            AnnoListNav tWithin = new AnnoListNav();
            tAnnoList.setNav(tWithin);
            tWithin.setResults((int)tResultNo);
			if (tResultNo > pQuery.getResultsPerPage()) { // if paginating
				int tPageNo = pQuery.getPage();
                tAnnoList.setStartIndex(tPageNo);
				if (tNumberOfPages != pQuery.getPage()) { // not on last page
					int tPage = tPageNo + 1;
					pQuery.setPage(tPage);
					tAnnoList.setNext(pQuery.toURI().toString());
				}
				pQuery.setPage(0);
				tWithin.setFirst(pQuery.toURI().toString());
				pQuery.setPage(tNumberOfPages);
				tWithin.setLast(pQuery.toURI().toString());
			} else {
                tAnnoList.setStartIndex(0);
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

	public AnnotationList getAnnotationsFromPage(final User pUser, final Canvas pPage) throws IOException {
		SolrQuery tQuery = _utils.getQuery();
        String tUserQuery = "";
        if (!pUser.isAdmin()) {
            tUserQuery = " AND creator:\"" + pUser.getId() + "\"";
        }
		tQuery.set("q", "target:" + _utils.escapeChars(pPage.getId()) + tUserQuery);

        AnnotationList tAnnoList = new AnnotationList();
		try {
			QueryResponse tResponse = _solrClient.query(tQuery);
			long tResultNo = tResponse.getResults().getNumFound();
			int tPageSize = tResponse.getResults().size();
			int tStart = 0;
			do { // this gets all of the pages of results and creates one list of annotations which isn't going to scale
				// would need to fix this by implementing paging.
				tAnnoList.getAnnotations().addAll(this.buildAnnotationList(tResponse, false));

				tStart += tPageSize;
				tQuery.setStart(tStart);
				tResponse = _solrClient.query(tQuery);
			} while (tStart < tResultNo);
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run solr query due to " + tException.toString());
		}
        return tAnnoList;
	}

	public Annotation getAnnotation(final String pId) throws IOException {
		SolrQuery tQuery = _utils.getQuery();

		tQuery.set("q", "id:" + _utils.escapeChars(pId));

		try {
			QueryResponse tResponse  = _solrClient.query(tQuery);

			if (tResponse.getResults().size() == 1) {
				Annotation tAnnotation = this.buildAnnotation(tResponse.getResults().get(0), false);
				return tAnnotation;
			} else if (tResponse.getResults().size() == 0) {
				return null; // no annotation found with supplied id
			} else {
				throw new IOException("Found " + tResponse.getResults().size() + " annotations with ID " + pId);
			}
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run solr query due to " + tException.toString());
		}
	}

	public List<PageAnnoCount> listAnnoPages(final Manifest pManifest, final User pUser) throws IOException {
        String tUserQuery = "";
        if (pUser != null && !pUser.isAdmin()) {
            tUserQuery = " AND creator:\"" + pUser.getId() + "\"";
        }

        SolrQuery tQuery = new SolrQuery();
        tQuery.setRows(0);
        tQuery.setFacet(true);
        tQuery.addFacetField("target");
        tQuery.setFacetLimit(-1);
        tQuery.setFacetMinCount(1);
        tQuery.setFacetSort("index");
        tQuery.set("q", "type:\"oa:Annotation\" AND within:\"" + pManifest.getURI() + "\"" + tUserQuery);
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

	public Annotation buildAnnotation(final SolrDocument pDoc, final boolean pCollapseOn) throws IOException {
		Map<String,Object> tAnnotation = (Map<String,Object>)JsonUtils.fromString(new String(Base64.getDecoder().decode((String)pDoc.get("data"))));

        return new Annotation(tAnnotation);
	}

	public List<Annotation> buildAnnotationList(final QueryResponse pResponse, final boolean pCollapseOn) throws IOException {
		List<Annotation> tResults = new ArrayList<Annotation>();
		for (SolrDocument tResult : pResponse.getResults()) {
            Annotation tAnno = this.buildAnnotation(tResult, pCollapseOn);
			tResults.add(tAnno);
            if (pResponse.getHighlighting() != null && pResponse.getHighlighting().get(tAnno.getId()) != null) {
                // Add snippet as label to annotation
                if ( pResponse.getHighlighting().get(tAnno.getId()).get("text") != null) {
                    List<String> snippets = pResponse.getHighlighting().get(tAnno.getId()).get("text");

                    tAnno.setLabel(snippets.get(0).replaceAll("<[ /]*[a-zA-Z0-9 ]*[ /]*>", ""));
                } else {
                    tAnno.setLabel(((List<String>)tResult.get("body")).get(0).replaceAll("<[ /]*[a-zA-Z0-9 ]*[ /]*>", ""));
                }
            }
		}

		return tResults;
	}

    public List<User> getUsers() throws IOException {
        SolrQuery tQuery = new SolrQuery();
		tQuery.setFields("id", "type", "short_id", "name", "email", "password", "picture", "group", "authenticationMethod", "created", "modified");
		tQuery.setRows(1000);

		tQuery.set("q", "type:\"User\"");

        List<User> tUsers = new ArrayList<User>();

		try {
			QueryResponse tResponse  = _solrClient.query(tQuery);
            for (SolrDocument pDoc : tResponse.getResults()) {
                tUsers.add(json2user(pDoc));
            }
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run solr query due to " + tException.toString());
		}
        return tUsers;
    }

    public User getUser(final User pUser) throws IOException {
        User tUser = new User();

        SolrQuery tQuery = new SolrQuery();
		tQuery.setFields("id", "type", "short_id", "name", "email", "password", "picture", "group", "authenticationMethod", "created", "modified");
		tQuery.setRows(1000);

		tQuery.set("q", "type:\"User\" AND id:\"" + pUser.getId() + "\"");

		try {
			QueryResponse tResponse  = _solrClient.query(tQuery);

			if (tResponse.getResults().size() == 1) {
                tUser = json2user(tResponse.getResults().get(0));
			} else if (tResponse.getResults().size() == 0) {
				return null; // no annotation found with supplied id
			} else {
				throw new IOException("Found " + tResponse.getResults().size() + " Users with ID " + pUser.getShortId());
			}
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run solr query due to " + tException.toString());
		}
        tUser.setToken(pUser.getToken());
        return tUser;
    }

    public User saveUser(final User pUser) throws IOException {
        User tSavedUser = this.getUser(pUser);
        if (tSavedUser != null) {
            // this is an update
            pUser.setCreated(tSavedUser.getCreated());
            pUser.updateLastModified();
        }
		SolrInputDocument tDoc = new SolrInputDocument();
		tDoc.addField("id", pUser.getId());
		tDoc.addField("short_id", pUser.getShortId());
		tDoc.addField("name", pUser.getName());
		tDoc.addField("type", "User");
		tDoc.addField("email", pUser.getEmail());
		tDoc.addField("created", pUser.getCreated());
		tDoc.addField("modified", pUser.getLastModified());
        if (pUser instanceof LocalUser) {
            tDoc.addField("password", ((LocalUser)pUser).getPassword());
        }
        if (pUser.getPicture() != null && !pUser.getPicture().isEmpty()) {
            tDoc.addField("picture", pUser.getPicture());
        }
        if (pUser.isAdmin()) {
            tDoc.addField("group", "admin");
        }    
		tDoc.addField("authenticationMethod", pUser.getAuthenticationMethod());

		_utils.addDoc(tDoc, true);
        return pUser;
    }

    public User deleteUser(final User pUser) throws IOException {
        List<String> tOldIds = new ArrayList<String>();
		tOldIds.add(pUser.getId());
		try {
			_solrClient.deleteById(tOldIds);
			_solrClient.commit();
		} catch(SolrServerException tException) {
			tException.printStackTrace();
			throw new IOException("Failed to remove user due to " + tException);
		}
        return pUser;
    }

    protected User json2user(final SolrDocument pDoc) throws IOException {
        User tUser = new User();
        if (pDoc.get("authenticationMethod").equals(LocalUser.AUTH_METHOD)) {
            tUser = new LocalUser();
            ((LocalUser)tUser).setPassword((String)pDoc.get("password"),false);
        }

        try {
            tUser.setId((String)pDoc.get("id"));
        } catch (URISyntaxException tExcpt) {
            throw new IOException("Id is not a URI: " + pDoc.get("id") + "\"" + tExcpt);
        }
        tUser.setShortId((String)pDoc.get("short_id"));
        tUser.setName((String)pDoc.get("name"));
        tUser.setEmail(((List<String>)pDoc.get("email")).get(0));
        tUser.setCreated((Date)pDoc.get("created"));
        tUser.setLastModified((Date)pDoc.get("modified"));
        if (pDoc.get("picture") != null && !((List<String>)pDoc.get("picture")).isEmpty()) {
            tUser.setPicture(((List<String>)pDoc.get("picture")).get(0));
        }
        if (pDoc.get("group") != null) {
            for (String tGroup : (List<String>)pDoc.get("group")) {
                if (tGroup.equals("admin")) {
                    tUser.setAdmin(true);
                }
            }
        }
        tUser.setAuthenticationMethod((String)pDoc.get("authenticationMethod"));

        return tUser;
    }


    public Collection createCollection(final Collection pCollection) throws IOException {
        SolrInputDocument tDoc = new SolrInputDocument();
		tDoc.addField("id", pCollection.getId());
		tDoc.addField("short_id", pCollection.getShortId());
		tDoc.addField("label", pCollection.getLabel());
		tDoc.addField("type", "Collection");
		tDoc.addField("creator", pCollection.getUser().getId());

        for (Manifest tManifest: pCollection.getManifests()) { 
            tDoc.addField("members", tManifest.getURI());
            tDoc.addField("members", tManifest.getLabel());
        }

		_utils.addDoc(tDoc, true);
        return pCollection;
    }

    public List<Collection> getCollections(final User pUser) throws IOException {
        SolrQuery tQuery = new SolrQuery();
		tQuery.setFields("id", "type", "short_id", "label", "creator", "members");
		tQuery.setRows(1000);

		tQuery.set("q", "type:\"Collection\" AND creator:\"" + pUser.getId() + "\"");

        List<Collection> tCollections = new ArrayList<Collection>();
		try {
			QueryResponse tResponse  = _solrClient.query(tQuery);

            for (SolrDocument pDoc : tResponse.getResults()) {
                Collection tCollection = buildCollection(pDoc);
                tCollection.setUser(pUser);
                tCollections.add(tCollection);
            }
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run create collections query due to " + tException.toString());
		}

        return tCollections;
    }
    
    protected Collection buildCollection(final SolrDocument pCollectionData) throws IOException {
        Collection tCollection = new Collection();
        tCollection.setId((String)pCollectionData.get("id"));
        tCollection.setShortId((String)pCollectionData.get("short_id"));
		tCollection.setLabel(((List<String>)pCollectionData.get("label")).get(0));

        User tUser = new User();
        try {
            tUser.setId(((List<String>)pCollectionData.get("creator")).get(0));
        } catch (URISyntaxException tExcpt) {
            throw new IOException("Failed to create user because id wasn't a URI: " + pCollectionData.get("creator"));
        }
        tCollection.setUser(tUser);

        List<String> tManifestList = ((List<String>)pCollectionData.get("members"));
        if (tManifestList != null) {
            for (int i = 0; i < tManifestList.size(); i +=2) {
                Manifest tManifest = new Manifest();
                tManifest.setURI(tManifestList.get(i));
                tManifest.setLabel(tManifestList.get(i + 1));

                tCollection.add(tManifest);
            }
        }

        return tCollection;
    }

    public Collection getCollection(final String pId) throws IOException {
        SolrQuery tQuery = new SolrQuery();
		tQuery.setFields("id", "type", "short_id", "label", "creator", "members");
		tQuery.setRows(1000);

		tQuery.set("q", "type:\"Collection\" AND id:\"" + pId + "\"");

        try {
			QueryResponse tResponse  = _solrClient.query(tQuery);

			if (tResponse.getResults().size() == 1) {
                return buildCollection(tResponse.getResults().get(0));
            }
		} catch (SolrServerException tException) {
			throw new IOException("Failed to run create collections query due to " + tException.toString());
		}

        return null;
    }

    public void deleteCollection(final Collection pCollection) throws IOException {
        List<String> tOldIds = new ArrayList<String>();
		tOldIds.add(pCollection.getId());
		try {
			_solrClient.deleteById(tOldIds);
			_solrClient.commit();
		} catch(SolrServerException tException) {
			tException.printStackTrace();
			throw new IOException("Failed to remove collection due to " + tException);
		}
    }

    public int getTotalAnnotations(final User pUser) throws IOException {
        SolrQuery tQuery = _utils.getQuery();
        StringBuffer tQueryStr = new StringBuffer("type:oa\\:Annotation");

        if (pUser != null) {
            tQueryStr.append(" AND creator:");
            tQueryStr.append(pUser.getId());
        }
		tQuery.set("q", tQueryStr.toString());

        try {
            QueryResponse tResponse = _solrClient.query(tQuery);
            return Math.toIntExact(tResponse.getResults().getNumFound());
        } catch (SolrServerException tExcpt) {
            tExcpt.printStackTrace();
            throw new IOException(tExcpt.getMessage());
        }
    }

    public int getTotalManifests(final User pUser) throws IOException {
        SolrQuery tQuery = _utils.getQuery();
        if (pUser == null) {
            StringBuffer tQueryStr = new StringBuffer("");
            tQuery.set("q", "type:sc\\:Manifest");

            try {
                QueryResponse tResponse = _solrClient.query(tQuery);
                return Math.toIntExact(tResponse.getResults().getNumFound());
            } catch (SolrServerException tExcpt) {
                tExcpt.printStackTrace();
                throw new IOException(tExcpt.getMessage());
            }
        } else {
            List<Collection> tCollections = this.getCollections(pUser);
            List<String> tManifests = new ArrayList<String>();
            for (Collection tColl : tCollections) {
                for (Manifest tManifest : tColl.getManifests()) {
                    if (!tManifests.contains(tManifest)) {
                        tManifests.add(tManifest.getURI());
                    }
                }
            }
            return tManifests.size();
        }
    }

    public int getTotalAnnoCanvases(final User pUser) throws IOException {
        SolrQuery tQuery = _utils.getQuery();
        StringBuffer tQueryStr = new StringBuffer("type:oa\\:Annotation");

        if (pUser != null) {
            tQueryStr.append(" AND creator:");
            tQueryStr.append(pUser.getId());
        }
		tQuery.set("q", tQueryStr.toString());

        List<String> tCanvases = new ArrayList<String>();
        try {
            QueryResponse tResponse = _solrClient.query(tQuery);
            // Now collect unique canvas
            for (SolrDocument pDoc : tResponse.getResults()) {
                String tCanvas = (String)pDoc.get("target");
                if (!tCanvases.contains(tCanvas)) {
                    tCanvases.add(tCanvas);
                }
            }

        } catch (SolrServerException tExcpt) {
            tExcpt.printStackTrace();
            throw new IOException(tExcpt.getMessage());
        }

        return tCanvases.size();
    }
}
