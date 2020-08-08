package uk.org.llgc.annotation.store.adapters.rdf;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import java.io.IOException;

import com.github.jsonldjava.core.JsonLdError;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.AnnoListNav;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.data.rdf.RDFManifest;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.adapters.AbstractStoreAdapter;

import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import com.github.jsonldjava.utils.JsonUtils;

import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public abstract class AbstractRDFStore extends AbstractStoreAdapter {
	protected static Logger _logger = LogManager.getLogger(AbstractRDFStore.class.getName());
	protected AnnotationUtils _annoUtils = null;
    public AbstractRDFStore(final AnnotationUtils pUtils) {
        _annoUtils = pUtils;
    }

	public Annotation addAnnotationSafe(final Annotation pAnno) throws IOException {
        Model tAnno = addAnnotationSafe(pAnno.toJson());
        Annotation tAfter = this.convertModel(tAnno);
        return tAfter;
    }

	public Annotation getAnnotation(final String pId) throws IOException {
		Model tAnno = this.getNamedModel(pId);
        if (tAnno == null) {
            return null;
        }
        return this.convertModel(tAnno);
    }

    protected Annotation convertModel(final Model pModel) throws IOException {
        Map<String,Object> tJson = _annoUtils.frameAnnotation(pModel, false);
        return new Annotation(tJson);
    }

	protected abstract Model getNamedModel(final String pContext) throws IOException;
	protected abstract Model addAnnotationSafe(final Map<String,Object> pJson) throws IOException;

	public AnnotationList getAnnotationsFromPage(final Canvas pPage) throws IOException {
		String tQueryString = "select ?annoId ?graph where {"
										+ " GRAPH ?graph { ?on <http://www.w3.org/ns/oa#hasSource> <" + pPage.getId() + "> ."
										+ " ?annoId <http://www.w3.org/ns/oa#hasTarget> ?on } "
									+ "}";

	//	_logger.debug("Query " + tQueryString);
		QueryExecution tExec = this.getQueryExe(tQueryString);

		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		int i = 0;
		AnnotationList tAnnotations = new AnnotationList();
        List<String> tAnnoIds = new ArrayList<String>();
		while (results.hasNext()) {
			QuerySolution soln = results.nextSolution() ;
			Resource tAnnoId = soln.getResource("annoId") ; // Get a result variable - must be a resource
            tAnnoIds.add(tAnnoId.getURI());

		}
		this.end();
        for (String tAnnoId : tAnnoIds) {
			tAnnotations.add(this.getAnnotation(tAnnoId));
        }
		return tAnnotations;
	}

	public List<Manifest> getManifests() throws IOException {
		String tQueryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
										"select ?manifest ?label ?shortId where {"  +
										" GRAPH ?graph {?manifest <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iiif.io/api/presentation/2#Manifest> . " +
                                        "               ?manifest <http://www.w3.org/2000/01/rdf-schema#label> ?label ." +
                                        "               ?manifest <http://purl.org/dc/elements/1.1/identifier> ?shortId " +
                                        "}}"; // need to bring back short_id and label


		QueryExecution tExec = this.getQueryExe(tQueryString);

		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		int i = 0;
		List<Manifest> tManifests = new ArrayList<Manifest>();
		if (results != null) {
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
				Resource tManifestURI = soln.getResource("manifest") ; // Get a result variable - must be a resource

				_logger.debug("Found manifest " + tManifestURI.getURI());
                Manifest tManifest = new Manifest();
                tManifest.setURI(tManifestURI.getURI());
                tManifest.setLabel(soln.getLiteral("label").getString());
                tManifest.setShortId(soln.getLiteral("shortId").getString());
                // TODO add label and short id

				tManifests.add(tManifest);
			}
		} else {
			_logger.debug("no Manifests loaded");
		}
		this.end();

		return tManifests;
	}

	public List<Manifest> getSkeletonManifests() throws IOException {
        String tQueryString = "select ?manifestId ?manifestLabel  where {" +
                                  "GRAPH ?graph { ?on <http://www.w3.org/ns/oa#hasSource> ?pageId ." +
                                  "  ?annoId <http://www.w3.org/ns/oa#hasTarget> ?target . " +
                                  "  OPTIONAL { ?target <http://purl.org/dc/terms/isPartOf> ?manifestId } ." +
                                  "  OPTIONAL { ?manifestId <http://www.w3.org/2000/01/rdf-schema#label> ?manifestLabel }" +
                                  "}" +
                                  "OPTIONAL {GRAPH ?manifestId {" +
                                  "  ?manifestId <http://purl.org/dc/elements/1.1/identifier> ?shortId ." +
                                  "  }}" +
                                  " FILTER (!bound(?shortId)) " + 
                                "} group by ?manifestId ?manifestLabel"; 
		QueryExecution tExec = this.getQueryExe(tQueryString);
        this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		int i = 0;
		List<Manifest> tManifests = new ArrayList<Manifest>();
		if (results != null) {
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
				Resource tManifestURI = soln.getResource("manifestId") ; // Get a result variable - must be a resource

				_logger.debug("Found manifest " + tManifestURI.getURI());
                Manifest tManifest = new Manifest();
                tManifest.setURI(tManifestURI.getURI());
                if (soln.getLiteral("label") != null) {
                    tManifest.setLabel(soln.getLiteral("label").getString());
                }    

				tManifests.add(tManifest);
			}
		} else {
			_logger.debug("no Manifests loaded");
		}
		this.end();

		return tManifests;


    }

	public String getManifestId(final String pShortId) throws IOException {
		String tQueryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
								  + "select ?manifest where { "
								  + "GRAPH ?graph { ?manifest rdf:type <http://iiif.io/api/presentation/2#Manifest> . "
								  + "?manifest <http://purl.org/dc/elements/1.1/identifier> '" + pShortId + "' "
								  + "}}";

		QueryExecution tExec = this.getQueryExe(tQueryString);

		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		int i = 0;
		String tManifest = "";
		_logger.debug("Results " + results);
		if (results != null) {
			if (results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
				Resource tManifestURI = soln.getResource("manifest") ; // Get a result variable - must be a resource

				tManifest = tManifestURI.getURI();
			} else {
				this.end();
				_logger.debug("Manifest with short id " + pShortId + " not found");
				return null;
			}
		}
		this.end();

		return tManifest;

	}

	public Manifest getManifest(final String pShortId) throws IOException {
		String tManifestURI = this.getManifestId(pShortId);
		if (tManifestURI == null || tManifestURI.trim().length() == 0) {
			_logger.debug("Manifest URI not found for short id " + pShortId);
			return null;
		}
		Model tModel = this.getNamedModel(tManifestURI);
        Manifest tManifest = new RDFManifest(tModel);
        tManifest.setURI(tManifestURI);
        tManifest.setShortId(pShortId);

		return tManifest;
	}

	public AnnotationList search(final SearchQuery pQuery) throws IOException {
		String tQueryString = "PREFIX oa: <http://www.w3.org/ns/oa#> "
									 + "PREFIX cnt: <http://www.w3.org/2011/content#> "
                                     + "PREFIX dcterms: <http://purl.org/dc/terms/> "
									 + "select ?anno ?content ?graph where { "
									 + "  GRAPH ?graph { ?anno oa:hasTarget ?target . "
									 + "  ?anno oa:hasBody ?body . "
                                     + "  ?target dcterms:isPartOf <" + pQuery.getScope() + "> ."
									 + "  ?body <" + Annotation.FULL_TEXT_PROPERTY + "> ?content ."
									 + "  FILTER regex(str(?content), \".*" + pQuery.getQuery() + ".*\")"
									 + "  }"
									 + "} ORDER BY ?anno";

		QueryExecution tExec = this.getQueryExe(tQueryString);

		AnnotationList tAnnotationList = new AnnotationList();

		this.begin(ReadWrite.READ);
		List<QuerySolution> tResults = ResultSetFormatter.toList(tExec.execSelect());
		this.end();
		try {
			if (tResults != null) {
				int tStart = pQuery.getIndex();
				int tEnd = tStart + pQuery.getResultsPerPage();
				if (tEnd > tResults.size()) {
					tEnd = tResults.size();
				}
				int tResultNo = tResults.size();
                AnnoListNav tWithin = new AnnoListNav();
                tAnnotationList.setNav(tWithin);
                tWithin.setResults(tResults.size());
				if (tResultNo > pQuery.getResultsPerPage()) { // if paginating
					int tNumberOfPages = (int)(tResults.size() / pQuery.getResultsPerPage());
					int tPageNo = pQuery.getPage();
                    tAnnotationList.setStartIndex(tPageNo);
					if (tNumberOfPages != pQuery.getPage()) { // not on last page
						int tPage = tPageNo + 1;
						pQuery.setPage(tPage);
						tAnnotationList.setNext(pQuery.toURI().toString());
					}
					pQuery.setPage(0);
					tWithin.setFirst(pQuery.toURI().toString());
					pQuery.setPage(tNumberOfPages);
					tWithin.setLast(pQuery.toURI().toString());
				} else {
                    tAnnotationList.setStartIndex(0);
                }
				for (int i = tStart; i < tEnd; i++) {
					QuerySolution soln = tResults.get(i);
					Resource tAnnoURI = soln.getResource("anno") ; // Get a result variable - must be a resource

					Model tAnno = this.getNamedModel(tAnnoURI.getURI());

					Map<String,Object> tJsonAnno = _annoUtils.frameAnnotation(tAnno, true);
                    // If a single resource don't include as an array
                    if (tJsonAnno.get("resource") != null && tJsonAnno.get("resource") instanceof List && ((List)tJsonAnno.get("resource")).size() == 1) {
                        tJsonAnno.put("resource", ((List)tJsonAnno.get("resource")).get(0));
                    }

                    String tContent = "";
                    if (tJsonAnno.get("resource") instanceof List) {
                        // need to find which body matched the query
                        for (Map<String,Object> tbody : (List<Map<String,Object>>)tJsonAnno.get("resource")) {
                            if (((String)tbody.get("chars")).contains(pQuery.getQuery())) {
                                tContent = (String)tbody.get("chars");
                                break;
                            }
                        }
                    } else {
                        tContent = (String)((Map<String,Object>)tJsonAnno.get("resource")).get("chars");
                    }
                    // Create snipet
                    String tCharsString = tContent.replaceAll("<[ /]*[a-zA-Z0-9 ]*[ /]*>", "");
                    String[] tChars = tCharsString.split(" ");
                    String tSnippet = "";
                    if (tChars.length < 5) {
                        tSnippet = tCharsString;
                    } else {
                        int tFoundIndex = -1;
                        for (int j = 0; j < tChars.length; j++) {
                            if (tChars[j].contains(pQuery.getQuery())) {
                                tFoundIndex = j;
                                break;
                            }
                        }
                        if (tFoundIndex == -1) {
                            tSnippet = tCharsString; // failed to find string so use whole string
                        } else {
                            int start = tFoundIndex - 2;
                            if (start < 0) {
                                start = 0;
                            }
                            int end = tFoundIndex + 2;
                            if (end > tChars.length) {
                                end = tChars.length;
                            }
                            for (int j = start; j < end; j++) {
                                tSnippet += tChars[j] + " ";
                            }
                        }
                    }
                    tJsonAnno.put("label", tSnippet);

					tAnnotationList.add(new Annotation(tJsonAnno));
				}
			}
		} catch (URISyntaxException tException) {
			throw new IOException("Failed to work with base URI " + tException.toString());
		} catch (JsonLdError tException) {
			throw new IOException("Failed to generate annotation list due to " + tException.toString());
		}

		return tAnnotationList;
	}


	public AnnotationList getAllAnnotations() throws IOException {
		// get all annotations but filter our manifest annotations
		String tQueryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
									 "select ?anno where { " +
									 "GRAPH ?graph {?anno rdf:type <http://www.w3.org/ns/oa#Annotation> . " +
								    "FILTER NOT EXISTS {?canvas rdf:first ?anno} " +
							 	    "}}";
        _logger.debug("Running SPARQL: " + tQueryString);
		QueryExecution tExec = this.getQueryExe(tQueryString);

		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
        this.end();
		int i = 0;
		AnnotationList tAnnotationList = new AnnotationList();

		try {
			if (results != null) {
                _logger.debug("Searching for results");
				while (results.hasNext()) {
					QuerySolution soln = results.nextSolution() ;
					Resource tAnnoURI = soln.getResource("anno") ; // Get a result variable - must be a resource

					tAnnotationList.add(this.getAnnotation(tAnnoURI.getURI()));
				}
			}
		} catch (JsonLdError tException) {
			throw new IOException("Failed to generate annotation list due to " + tException.toString());
		}

		return tAnnotationList;
	}

    public Canvas resolveCanvas(final String pShortId) throws IOException {
        String tQueryString =   "select ?canvas ?label where {" +
										"   GRAPH ?canvas { " +
										"	 ?canvas <http://purl.org/dc/elements/1.1/identifier> \"" + pShortId + "\" ." +
										"	 ?canvas <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iiif.io/api/presentation/2#Canvas> ." +
										"	 OPTIONAL { ?canvas  <http://www.w3.org/2000/01/rdf-schema#label> ?label } " +
										"   } " +
										"}";
		QueryExecution tExec = this.getQueryExe(tQueryString);
		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		this.end();
		Canvas tCanvas = null;
		if (results != null && results.hasNext()) {
            QuerySolution soln = results.nextSolution() ;
            Resource tCanvasResource = soln.getResource("canvas");
            String tLabel = "";
            if (soln.getLiteral("label") != null) {
                tLabel = soln.getLiteral("label").getString();
            }
            tCanvas = new Canvas(tCanvasResource.getURI(), tLabel);
            tCanvas.setShortId(pShortId);
		}
        return tCanvas;
    }

    protected abstract void storeCanvas(final String pGraphName, final Model pModel) throws IOException;

    public void storeCanvas(final Canvas pCanvas) throws IOException {
		Model tModel = ModelFactory.createDefaultModel();
        Resource tCanvasURI = tModel.createResource(pCanvas.getId());
        tModel.add(tModel.createStatement(tCanvasURI, RDF.type, tModel.createResource("http://iiif.io/api/presentation/2#Canvas")));
        tModel.add(tModel.createStatement(tCanvasURI, DC.identifier, pCanvas.getShortId()));
        if (pCanvas.getLabel() != null && !pCanvas.getLabel().trim().isEmpty()) {
            tModel.add(tModel.createStatement(tCanvasURI, RDFS.label, pCanvas.getLabel()));
        }

        storeCanvas(pCanvas.getId(), tModel);
    }

	public List<PageAnnoCount> listAnnoPages() {
        String tQueryString = "select ?pageId ?manifestId ?manifestLabel ?shortId ?canvasLabel ?canvasShortId (count(?annoId) as ?count) where {" +
                                  "GRAPH ?graph { ?on <http://www.w3.org/ns/oa#hasSource> ?pageId ." +
                                  "  ?annoId <http://www.w3.org/ns/oa#hasTarget> ?target . " +
                                  "  OPTIONAL { ?target <http://purl.org/dc/terms/isPartOf> ?manifestId }" +
                                  "}" +
                                  "OPTIONAL {GRAPH ?manifestId {" +
                                  "  ?manifestId <http://www.w3.org/2000/01/rdf-schema#label> ?manifestLabel ." +
                                  "  ?manifestId <http://purl.org/dc/elements/1.1/identifier> ?shortId ." +
                                  "  ?pageId <http://www.w3.org/2000/01/rdf-schema#label> ?canvasLabel " +
                                  "  }}" +
                                  "OPTIONAL { GRAPH ?{ " +
                                  "	 ?canvas <http://purl.org/dc/elements/1.1/identifier> ?canvasShortId ." +
                                  "	 ?canvas <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iiif.io/api/presentation/2#Canvas> " +
                                  "  }}" +
                                "}group by ?pageId ?manifestId ?manifestLabel ?shortId ?canvasLabel ?canvasShortId order by ?pageId"; 
		QueryExecution tExec = this.getQueryExe(tQueryString);
        return listAnnoPagesQuery(tExec, null);
    }

    public List<PageAnnoCount> listAnnoPages(final Manifest pManifest) {
        String tQueryString = "select ?pageId ?canvasLabel ?canvasShortId (count(?annoId) as ?count) where {" +
                                  "GRAPH ?graph { ?on <http://www.w3.org/ns/oa#hasSource> ?pageId ." +
                                  "  ?annoId <http://www.w3.org/ns/oa#hasTarget> ?target . " +
                                  "  ?target <http://purl.org/dc/terms/isPartOf> <" + pManifest.getURI() + "> " +
                                  "}" +
                                  "OPTIONAL {GRAPH <" + pManifest.getURI() + "> {" +
                                  "  ?pageId <http://www.w3.org/2000/01/rdf-schema#label> ?canvasLabel" +
                                  "  }}" +
                                  "OPTIONAL { GRAPH ?pageId{ " +
                                  "	 ?canvas <http://purl.org/dc/elements/1.1/identifier> ?canvasShortId ." +
                                  "	 ?canvas <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iiif.io/api/presentation/2#Canvas> " +
                                  "  }}" +
                                "}group by ?pageId ?canvasLabel ?canvasShortId order by ?pageId";
        
		QueryExecution tExec = this.getQueryExe(tQueryString);
        return listAnnoPagesQuery(tExec, pManifest);
    }

	private List<PageAnnoCount> listAnnoPagesQuery(final QueryExecution pQuery, final Manifest pManifest) {
		this.begin(ReadWrite.READ);
		ResultSet results = pQuery.execSelect(); // Requires Java 1.7
		this.end();
		int i = 0;
		List<PageAnnoCount> tAnnotations = new ArrayList<PageAnnoCount>();
		if (results != null) {
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
				Resource tPageId = soln.getResource("pageId") ; // Get a result variable - must be a resource
				int tCount = soln.getLiteral("count").getInt();
				_logger.debug("Found " + tPageId + " count " + tCount);
                Manifest tManifest = pManifest;
                if (pManifest == null) {
                    // create Manifest from data
                    if (soln.getLiteral("shortId") != null) {
                        tManifest = new Manifest();
                        tManifest.setShortId(soln.getLiteral("shortId").getString());
                        tManifest.setURI(soln.getResource("manifestId").getURI());
                        tManifest.setLabel(soln.getLiteral("manifestLabel").getString());
                    } else if (soln.getResource("manifestId") != null) {
                        // Manifest not loaded but may have a link to the manifest URI
                        tManifest = new Manifest();
                        tManifest.setURI(soln.getResource("manifestId").getURI());
                    } else {
                        // No linked manifest from canvas
                        tManifest = null;
                    }
                }
                String tCanvasLabel = "";
                if (soln.getLiteral("canvasLabel") != null) {
                    tCanvasLabel = soln.getLiteral("canvasLabel").getString();
                }
                Canvas tCanvas = new Canvas(tPageId.getURI(), tCanvasLabel);
                if (soln.getLiteral("canvasShortId") != null) {
                    tCanvas.setShortId(soln.getLiteral("canvasShortId").getString());
                } else {
                    try {
                        storeCanvas(tCanvas);
                    } catch (IOException tExcpt) {
                        tExcpt.printStackTrace();
                    }
                }

				tAnnotations.add(new PageAnnoCount(tCanvas, tCount, tManifest));
			}
		}

		return tAnnotations;
	}

	protected QueryExecution getQueryExe(final String pQuery) {
		throw new UnsupportedOperationException("Either getQueryExe must be implemented in a subclass or you should overload listAnnoPages and getAnnotationsFromPage");
	}

	public Manifest getManifestForCanvas(final Canvas pCanvas) throws IOException {
		String tQueryString =   "PREFIX oa: <http://www.w3.org/ns/oa#> " +
										"PREFIX sc: <http://iiif.io/api/presentation/2#> " +
										"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
										"PREFIX dcterms: <http://purl.org/dc/terms/>" +
                                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
										"select ?manifest ?label where {" +
										"   GRAPH ?graph { " +
										"	 ?manifest sc:hasSequences ?seqence ." +
                                        "    optional { ?manifest <rdfs:label> ?label }." + 
										"	 ?seqence ?sequenceCount ?seqenceId ." +
										"	 ?seqenceId rdf:type sc:Sequence ." +
										"	 ?seqenceId sc:hasCanvases ?canvasList ." +
										"	 ?canvasList rdf:rest*/rdf:first <" + pCanvas.getId() + "> " +
										"   } " +
										"}";

		QueryExecution tExec = this.getQueryExe(tQueryString);
		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		this.end();
        Manifest tManifest = null;
		if (results != null && results.hasNext()) {
            QuerySolution soln = results.nextSolution() ;
            Resource tManifestURI = soln.getResource("manifest");

            Literal tLabel = soln.getLiteral("label");
            tManifest = new Manifest();
            tManifest.setURI(tManifestURI.getURI());
            if (tLabel != null) {
                tManifest.setLabel(tLabel.toString());
            } 
		}
        return tManifest;
    }

	protected abstract String indexManifestOnly(final String pShortId, Map<String, Object> pManifest) throws IOException;

	protected String indexManifestNoCheck(final String pShortId, Manifest pManifest) throws IOException {
        pManifest.setShortId(pShortId);
        pManifest.toJson().put(DC.identifier.getURI(), pShortId);
		String tShortId = this.indexManifestOnly(pShortId, pManifest.toJson());
		// Now update any annotations which don't contain a link to this manifest.
		String tQueryString =   "PREFIX oa: <http://www.w3.org/ns/oa#> " +
										"PREFIX sc: <http://iiif.io/api/presentation/2#> " +
										"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
										"PREFIX dcterms: <http://purl.org/dc/terms/>" +
										"select distinct ?graph ?canvas {" +
										"   GRAPH ?graph2 { " +
										"	 <" + pManifest.getURI() + "> sc:hasSequences ?seqence ." +
										"	 ?seqence ?sequenceCount ?seqenceId ." +
										"	 ?seqenceId rdf:type sc:Sequence ." +
										"	 ?seqenceId sc:hasCanvases ?canvasList ." +
										"	 ?canvasList rdf:rest*/rdf:first ?canvas " +
										"   } " +
										"	 GRAPH ?graph {" +
										"		?source oa:hasSource ?canvas ." +
										"		?anno oa:hasTarget ?source ." +
										"		  filter not exists {?source dcterms:isPartOf <" + pManifest.getURI() + "> }" +
										"  }" +
										"}";

		QueryExecution tExec = this.getQueryExe(tQueryString);
		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		this.end();
		if (results != null) {
            List<Map<String,String>> tUris = new ArrayList<Map<String, String>>();
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
                Map<String,String> tResult = new HashMap<String,String>();
                tResult.put("anno_id", soln.getResource("graph").toString());// Get a result variable - must be a resource
                tResult.put("canvas_id", soln.getResource("canvas").toString());
				tUris.add(tResult);
            }
            for (Map<String, String> tResult: tUris) {
                String tURI = tResult.get("anno_id");
                Canvas tCanvas = new Canvas(tResult.get("canvas_id"), "");
				Annotation tAnno = this.getAnnotation(tURI);
                // should add within without turning it back and forth into json

                if (tAnno != null) {
    				// add within
    				tAnno.addWithin(pManifest, tCanvas);

                    try {
        				super.updateAnnotation(tAnno);
                    } catch (MalformedAnnotation tExcpt) {
                        throw new IOException("Failed to reload annotation after updating the within: " + tExcpt);
                    }
                } else {
                    _logger.error("Failed to find annotation with id " + tURI);
                }
			}

		} else {
			// found no annotations that weren't linked to this manifest
		}

		return tShortId;
	}
}
