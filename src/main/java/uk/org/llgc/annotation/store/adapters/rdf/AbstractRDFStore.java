package uk.org.llgc.annotation.store.adapters.rdf;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

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
import uk.org.llgc.annotation.store.data.Collection;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.IIIFSearchResults;
import uk.org.llgc.annotation.store.data.AnnoListNav;
import uk.org.llgc.annotation.store.data.users.User;
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
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.sparql.vocabulary.FOAF;
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

import java.net.URISyntaxException;

public abstract class AbstractRDFStore extends AbstractStoreAdapter {
	protected static Logger _logger = LogManager.getLogger(AbstractRDFStore.class.getName());
	protected AnnotationUtils _annoUtils = null;
    public AbstractRDFStore(final AnnotationUtils pUtils) {
        _annoUtils = pUtils;
    }

	public Annotation addAnnotationSafe(final Annotation pAnno) throws IOException {
        // As creator isn't in OA add something to the context to handle it correctly.
        Map<String, Object> tCreatorContext = new HashMap<String,Object>();
        Map<String, Object> tCreatorType = new HashMap<String,Object>();
        tCreatorType.put("@type", "@id");
        tCreatorContext.put("dcterms", "http://purl.org/dc/terms/");
        tCreatorContext.put("dcterms:creator", tCreatorType);

        Map<String, Object> tAnnoJson = pAnno.toJson();
        if (tAnnoJson.get("@context") instanceof List) {
            List tContext = (List)tAnnoJson.get("@context");
            tContext.add(tCreatorContext);
        } else {
            Object tOrigContext = tAnnoJson.get("@context");
            List tContext = new ArrayList();
            tContext.add(tCreatorContext);
            tContext.add(tOrigContext);

            tAnnoJson.put("@context", tContext);
        }

        Model tAnno = addAnnotationSafe(tAnnoJson);
        //RDFDataMgr.write(System.out, tAnno, Lang.TURTLE);
        Annotation tAfter = this.convertModel(tAnno);
        //System.out.println("Anno after read back " + JsonUtils.toPrettyString(tAfter.toJson()));
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
        if (tJson.get("http://purl.org/dc/terms/creator") != null) {
            tJson.put("dcterms:creator", tJson.get("http://purl.org/dc/terms/creator"));
            tJson.remove("http://purl.org/dc/terms/creator");
        }
        if (tJson.get("dcterms:creator") instanceof Map) {
            Map<String, Object> tCreator = (Map<String,Object>)tJson.get("dcterms:creator");
            tJson.put("dcterms:creator", tCreator.get("@id"));
        }
        
        return new Annotation(tJson);
    }

	protected abstract Model getNamedModel(final String pContext) throws IOException;
	protected abstract Model addAnnotationSafe(final Map<String,Object> pJson) throws IOException;

	public AnnotationList getAnnotationsFromPage(final User pUser, final Canvas pPage) throws IOException {
        String tUserTest = "";
        if (!pUser.isAdmin()) {
            tUserTest = " ?annoId <http://purl.org/dc/terms/creator> <" + pUser.getId() + "> .";
        }
		String tQueryString = "select ?annoId ?graph where {"
										+ " GRAPH ?graph { ?on <http://www.w3.org/ns/oa#hasSource> <" + pPage.getId() + "> ."
                                        + tUserTest
										+ " ?annoId <http://www.w3.org/ns/oa#hasTarget> ?on } "
									+ "}";

		_logger.debug("Query " + tQueryString);
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

	public List<Manifest> getSkeletonManifests(final User pUser) throws IOException {
        String tUserTest = "";
        if (!pUser.isAdmin()) {
            tUserTest = " ?annoId <http://purl.org/dc/terms/creator> <" + pUser.getId() + "> . ";
        }

        String tQueryString = "select ?manifestId ?manifestLabel  where {" +
                                  "GRAPH ?graph { ?on <http://www.w3.org/ns/oa#hasSource> ?pageId ." +
                                  "  ?annoId <http://www.w3.org/ns/oa#hasTarget> ?target . " +
                                  tUserTest + 
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

	public Manifest getManifest(final String pId) throws IOException {
		Model tModel = this.getNamedModel(pId);
        if (tModel == null) {
            return null;
        } else {    
            Manifest tManifest = new RDFManifest(tModel);
            tManifest.setURI(pId);

            return tManifest;
        }
	}

	public IIIFSearchResults search(final SearchQuery pQuery) throws IOException {
        String tUserTest = "";
        if (pQuery.getUsers() != null) {
            User tUser = pQuery.getUsers().get(0);
            if (!tUser.isAdmin()) {
                tUserTest = " ?annoId <http://purl.org/dc/terms/creator> <" + tUser.getId() + "> .";
            }
        }    
		String tQueryString = "PREFIX oa: <http://www.w3.org/ns/oa#> "
									 + "PREFIX cnt: <http://www.w3.org/2011/content#> "
                                     + "PREFIX dcterms: <http://purl.org/dc/terms/> "
									 + "select ?anno ?content ?graph where { "
									 + "  GRAPH ?graph { ?anno oa:hasTarget ?target . "
									 + "  ?anno oa:hasBody ?body . "
                                     + tUserTest
                                     + "  ?target dcterms:isPartOf <" + pQuery.getScope() + "> ."
									 + "  ?body <" + Annotation.FULL_TEXT_PROPERTY + "> ?content ."
									 + "  FILTER regex(str(?content), \".*" + pQuery.getQuery() + ".*\")"
									 + "  }"
									 + "} ORDER BY ?anno";

		QueryExecution tExec = this.getQueryExe(tQueryString);


		this.begin(ReadWrite.READ);
		List<QuerySolution> tResults = ResultSetFormatter.toList(tExec.execSelect());
		this.end();
        IIIFSearchResults tAnnotationList = null;
		try {
            tAnnotationList = new IIIFSearchResults(pQuery.toURI());

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

    protected abstract void storeModel(final String pGraphName, final Model pModel) throws IOException;

    public void storeCanvas(final Canvas pCanvas) throws IOException {
		Model tModel = ModelFactory.createDefaultModel();
        Resource tCanvasURI = tModel.createResource(pCanvas.getId());
        tModel.add(tModel.createStatement(tCanvasURI, RDF.type, tModel.createResource("http://iiif.io/api/presentation/2#Canvas")));
        tModel.add(tModel.createStatement(tCanvasURI, DC.identifier, pCanvas.getShortId()));
        if (pCanvas.getLabel() != null && !pCanvas.getLabel().trim().isEmpty()) {
            tModel.add(tModel.createStatement(tCanvasURI, RDFS.label, pCanvas.getLabel()));
        }

        storeModel(pCanvas.getId(), tModel);
    }

    public User getUser(final User pUser) throws IOException {
        Model tUserModel = getNamedModel(pUser.getId());
        if (tUserModel == null) {
            return null;
        } else {
            this.begin(ReadWrite.READ);
            User tSavedUser = new User();
            tSavedUser.setToken(pUser.getToken());
            StmtIterator tStatements = tUserModel.listStatements();
            while (tStatements.hasNext()) {
                Statement tStatement = tStatements.nextStatement();
                //System.out.println("User statement " + tStatement.toString());
                if (tStatement.getPredicate().equals(RDF.type) && tStatement.getObject().equals(FOAF.Person)) {
                    try {
                        tSavedUser.setId(tStatement.getSubject().getURI());
                    } catch (URISyntaxException tExcpt) {
                        // This shouldn't happen in here
                        System.err.println("Failed to get user ID as a URI");
                    }
                }
                if (tStatement.getPredicate().equals(DC.identifier)) {
                    tSavedUser.setShortId(tStatement.getObject().toString());
                }
                if (tStatement.getPredicate().equals(FOAF.name)) {
                    tSavedUser.setName(tStatement.getObject().toString());
                }
                if (tStatement.getPredicate().equals(FOAF.mbox)) {
                    tSavedUser.setEmail(tStatement.getObject().toString());
                }
                if (tStatement.getPredicate().equals(FOAF.accountName)) {
                    tSavedUser.setAuthenticationMethod(tStatement.getObject().toString());
                }
                if (tStatement.getPredicate().equals(FOAF.img)) {
                    tSavedUser.setPicture(tStatement.getObject().toString());
                }
                if (tStatement.getPredicate().equals(FOAF.member) && tStatement.getSubject().getURI().equals("sas.permissions.admin")) {
                    tSavedUser.setAdmin(true);
                }
                if (tStatement.getPredicate().equals(DCTerms.created)) {
                    tSavedUser.setCreated(parseDate(tStatement.getObject().toString()));
                }
                if (tStatement.getPredicate().equals(DCTerms.modified)) {
                    tSavedUser.setLastModified(parseDate(tStatement.getObject().toString()));
                }
            }
            this.end();
            return tSavedUser;
        }
    }

    protected String formatDate(final Date pDate) {
        SimpleDateFormat tDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        return tDateFormatter.format(pDate);
    }

    protected Date parseDate(final String pDate) {
        try {
            SimpleDateFormat tDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            return tDateFormatter.parse(pDate);
        } catch (ParseException tExcpt) {
            tExcpt.printStackTrace();
            System.err.println("Failed to parse date " + pDate);
            return null;
        }
    }

    public User saveUser(final User pUser) throws IOException {
        Model tSavedUser = getNamedModel(pUser.getId());
        this.begin(ReadWrite.READ);
        if (tSavedUser != null) {
            // This is an update
            pUser.updateLastModified();
            /*System.out.println("**** Updateding created ******");
            RDFDataMgr.write(System.out, tSavedUser, Lang.TRIG) ;
            System.out.println("**** Saved user ******" + tSavedUser.listStatements().toList());*/
            Statement tDateStatement = tSavedUser.getProperty(tSavedUser.createResource(pUser.getId()), DCTerms.created);
            if (tDateStatement != null && tDateStatement.getObject() != null) {
                pUser.setCreated(parseDate(tDateStatement.getObject().toString()));
            }
            this.end();
            this.deleteAnnotation(pUser.getId());
            this.begin(ReadWrite.READ);
        }
        this.end();
        Model tModel = ModelFactory.createDefaultModel();
        Resource tPersonURI = tModel.createResource(pUser.getId());
        tModel.add(tModel.createStatement(tPersonURI, RDF.type, FOAF.Person));
        tModel.add(tModel.createStatement(tPersonURI, DC.identifier, pUser.getShortId()));
        tModel.add(tModel.createStatement(tPersonURI, FOAF.name, pUser.getName()));
        tModel.add(tModel.createStatement(tPersonURI, FOAF.mbox, pUser.getEmail()));
        tModel.add(tModel.createStatement(tPersonURI, DCTerms.created, formatDate(pUser.getCreated())));
        tModel.add(tModel.createStatement(tPersonURI, DCTerms.modified, formatDate(pUser.getLastModified())));
        if (pUser.getPicture() != null && !pUser.getPicture().isEmpty()) {
            tModel.add(tModel.createStatement(tPersonURI, FOAF.img, pUser.getPicture()));
        }

        Resource tAccount = tModel.createResource();
        tModel.add(tModel.createStatement(tPersonURI, FOAF.account, tAccount));
        tModel.add(tModel.createStatement(tAccount, RDF.type, FOAF.OnlineAccount));
        tModel.add(tModel.createStatement(tAccount, FOAF.accountName, pUser.getAuthenticationMethod()));

        if (pUser.isAdmin()) {
            Resource tAdminGroup = tModel.createResource("sas.permissions.admin");
            tModel.add(tModel.createStatement(tAdminGroup, FOAF.member, tPersonURI));
            tModel.add(tModel.createStatement(tAdminGroup, RDF.type, FOAF.Group));
        }

        this.storeModel(pUser.getId(), tModel);

        return pUser;
    }

    public Collection createCollection(final Collection pCollection) throws IOException {
        while (true) {
            if (this.getNamedModel(pCollection.getId()) != null){
                pCollection.setId(pCollection.getId() + "1");    
                System.out.println("Found model trying " + pCollection.getId());
            } else {
                break; // Id is unique
            }
        }
        Model tResult = addAnnotationSafe(pCollection.toJson());
        return new Collection(_annoUtils.frameCollection(tResult));
    }


    public List<Collection> getCollections(final User pUser) throws IOException {
         String tQueryString = "select ?collectionId where {" +
                                  "GRAPH ?collectionId { " +
                                  "  ?coll <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iiif.io/api/presentation/2#Collection> . " +
                                  "  ?coll <http://purl.org/dc/terms/creator> <" + pUser.getId() + "> " +
                                  "}" +
                                "}"; 
		QueryExecution tExec = this.getQueryExe(tQueryString);
        this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		this.end();
        List<Collection> tCollections = new ArrayList<Collection>();
		if (results != null) {
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution() ;
                Resource tCollectionResource = soln.getResource("collectionId");
                tCollections.add(this.getCollection(tCollectionResource.getURI()));
            }
		}
        return tCollections;
    }

    protected void walkList(final Model pModel, final Resource pKey, final List<String> pResults) {
        org.apache.jena.rdf.model.RDFNode tNode = null;
        StmtIterator tStatements = pModel.listStatements(pKey, null, tNode);
        while (tStatements.hasNext()) {
            Statement tStatement = tStatements.nextStatement();
            if (tStatement.getPredicate().equals(RDF.first)) {
                System.out.println("Adding " + tStatement.toString());
                if (!pResults.contains(tStatement.getObject().toString())) {
                    pResults.add(tStatement.getObject().toString());
                }
            }
            if (tStatement.getPredicate().equals(RDF.rest)) {
                System.out.println("Looping on " + tStatement.toString());
                walkList(pModel, tStatement.getObject().asResource(),pResults);
            }
        }
    }

    public Collection getCollection(final String pId) throws IOException {
        Model tCollModel = this.getNamedModel(pId);
        if (tCollModel != null) {
            this.begin(ReadWrite.READ);
            org.apache.jena.rdf.model.RDFNode tNode = null;
            StmtIterator tStatements = tCollModel.listStatements(null, tCollModel.createProperty("http://iiif.io/api/presentation/2#", "hasManifests"), tNode);
            List<Statement> tManifests = new ArrayList<Statement>();
            while (tStatements.hasNext()) {
                tManifests.add(tStatements.nextStatement());
            }
            this.end();
            Collection tCollection = null;
            if (tManifests.size() > 1) {
                System.out.println("***************************************************");
                System.out.println("***         somehow got multiple hasManifests   ***");
                System.out.println("***************************************************");
             // If we've reached here then somehow we have two sets of hasManifests which means the framing will break
                List<String> tManifestsIds = new ArrayList<String>();
                for (Statement tStatement : tManifests) { 
                    walkList(tCollModel, tStatement.getObject().asResource(), tManifestsIds);
                }    

                this.begin(ReadWrite.WRITE);
                tCollModel.removeAll(null,  tCollModel.createProperty("http://iiif.io/api/presentation/2#", "hasManifests"), tNode);
                tCollModel.removeAll(null,  tCollModel.createProperty("http://iiif.io/api/presentation/2#", "hasParts"), tNode);
                tCollModel.commit();    

                tCollection = new Collection(_annoUtils.frameCollection(tCollModel));
                for (String tURI : tManifestsIds) {
                    Manifest tManifest = new Manifest();
                    tManifest.setURI(tURI);

                    StmtIterator tStatementsIter = tCollModel.listStatements(tCollModel.createResource(tURI), RDFS.label, tNode);
                    if (tStatementsIter != null && tStatementsIter.hasNext()) {
                        tManifest.setLabel(tStatementsIter.next().getObject().toString());
                    }

                    tCollection.add(tManifest);
                }
                this.updateCollection(tCollection);
                return tCollection;
            } else {
                tCollection = new Collection(_annoUtils.frameCollection(tCollModel));
            }

            return tCollection;

        } else {
            return null;
        }
    }

    public void deleteCollection(final Collection pCollection) throws IOException {
        this.deleteAnnotation(pCollection.getId());
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
