package uk.org.llgc.annotation.store.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestWatcher;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import java.net.URISyntaxException;
import java.net.URI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.adapters.solr.SolrStore;
import uk.org.llgc.annotation.store.adapters.elastic.ElasticStore;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.encoders.Encoder;
import uk.org.llgc.annotation.store.data.users.User;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.query.* ;

import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.Properties;


public class TestUtils {
	protected static Logger _logger = LogManager.getLogger(TestUtils.class.getName());
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;
	@Rule
	public TemporaryFolder _testFolder = new TemporaryFolder();
	protected Properties _props = null;
    protected boolean _retainFailedData = false;

	public TestUtils() throws IOException {
		this(null);
	}

	public TestUtils(final Encoder pEncoder) throws IOException {
		_annotationUtils = new AnnotationUtils(new File(getClass().getResource("/contexts").getFile()), pEncoder);
        String tConfig = System.getenv("config") == null ? "test.properties" : System.getenv("config");
        _logger.debug("Reading in " + getClass().getResource("/contexts").getFile());
        _logger.debug("Calling load props");
        _logger.debug("Reading props " + getClass().getResource("/" + tConfig).getFile());
		_props = new Properties();
		_props.load(new FileInputStream(new File(getClass().getResource("/" + tConfig).getFile())));
        _retainFailedData = _props.getProperty("retain_failed_data") != null ? _props.getProperty("retain_failed_data").equals("true") : false;
	}

   public void setup() throws IOException {
		if (_props.getProperty("store").equals("jena")) {
            File tDataDir = null;
            if (_retainFailedData) {
                File tTmpDir = new File(System.getProperty("java.io.tmpdir"));
                tDataDir = new File(new File(tTmpDir, new java.text.SimpleDateFormat("dd-mm-yyyy_HH-mm-ss-SS").format(new java.util.Date())), "data");
            } else {
                tDataDir = new File(_testFolder.getRoot(), "data");
            }
			tDataDir.mkdirs();
			_props.put("data_dir",tDataDir.getPath());
		}

		StoreConfig tConfig = new StoreConfig(_props);
        tConfig.setAnnotationUtils(_annotationUtils);
		_store = StoreConfig.getConfig().getStore();
        _logger.debug("Store is " + _store);

		if (_props.getProperty("store").equals("elastic")) {
            ((ElasticStore)_store).setRefreshPolicy(org.elasticsearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE);
        }
	}

	public String getAnnoId(final Model pModel) {
		Iterator<Resource> tSubjects = pModel.listSubjects();
		Resource tAnnoId = null;
		while (tSubjects.hasNext()) {
			Resource tSubject = tSubjects.next();
			if (tSubject.getURI() != null && tSubject.getURI().contains("http://")) {
				tAnnoId = tSubject;
				break;
			}
		}

		return tAnnoId.getURI();
	}

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, org.junit.runner.Description description) {
            if (_retainFailedData) {
                System.out.println("Test " + description + " failed. Jena data is aviliable in: " + _props.get("data_dir"));
            } else {
                this.deleteData();
            }
        }
        @Override
        protected void succeeded(org.junit.runner.Description description) {
            this.deleteData();
        }

        protected void deleteData() {
            String tStore = _props.getProperty("store");
    		if (tStore.equals("jena")) {
                // Only delete the data_dir if its been used. TestSetup only tests the enviroment variables
                if (_props.get("data_dir") != null) {
                    File tDataDir = new File((String)_props.get("data_dir")).getParentFile();
                    try {
            			delete(tDataDir);
                    } catch (IOException tExcpt) {
                        System.err.println("Failed to delete test dir " + tDataDir.getPath() + " due to " + tExcpt);
                    }
                }
            }
        }
    };

   public void tearDown() throws IOException {
		String tStore = _props.getProperty("store");
		if (tStore.equals("solr") || tStore.equals("solr-cloud")) {
			SolrClient _solrClient = ((SolrStore)_store).getClient();
			try {
				UpdateResponse tResponse = _solrClient.deleteByQuery("*:*");// deletes all documents
				_solrClient.commit();
			} catch(SolrServerException tException) {
				tException.printStackTrace();
				throw new IOException("Failed to remove annotations due to " + tException);
			}
		} else if (tStore.equals("elastic")) {
            try {
                URI tConectionString = new URI((String)_props.get("elastic_connection"));
                RestHighLevelClient tClient = ElasticStore.buildClient(tConectionString);
                String tIndex = tConectionString.getPath().replace("/","");
                
                tClient.indices().delete(new DeleteIndexRequest(tIndex), RequestOptions.DEFAULT);
            } catch (URISyntaxException tExcpt) {
				tExcpt.printStackTrace();
				throw new IOException("Failed to remove annotations due to " + tExcpt);
            }
		} else if (tStore.equals("sesame")) {
			try {
				HTTPRepository tRepo = new HTTPRepository(_props.getProperty("repo_url"));
				RepositoryConnection tConn = tRepo.getConnection();
				tConn.clear();
			} catch (RepositoryException tExcpt) {
				tExcpt.printStackTrace();
				throw new IOException(tExcpt.getMessage());
			}
		}
	}

	protected void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				this.delete(c);
			}
		}
		if (f.exists() && !f.delete()) {
			throw new IOException("Failed to delete file: " + f);
		}
	}

	protected void testAnnotation(final Model pModel, final String pValue, final String pTarget) {
		String tQuery = "PREFIX oa: <http://www.w3.org/ns/oa#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX cnt: <http://www.w3.org/2011/content#> select ?content ?uri ?fragement where { ?annoId oa:hasTarget ?target . ?target oa:hasSource ?uri . ?target oa:hasSelector ?fragmentCont . ?fragmentCont rdf:value ?fragement . ?annoId oa:hasBody ?body . ?body cnt:chars ?content }";
		this.queryAnnotation(pModel,tQuery, pValue, pTarget);
	}

	protected void testAnnotation(final Model pModel, final String pId, final String pValue, final String pTarget) {
		String tQuery = "PREFIX oa: <http://www.w3.org/ns/oa#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX cnt: <http://www.w3.org/2011/content#> select ?content ?uri ?fragement where { <$id> oa:hasTarget ?target . ?target oa:hasSource ?uri . ?target oa:hasSelector ?fragmentCont . ?fragmentCont rdf:value ?fragement . <$id> oa:hasBody ?body . ?body cnt:chars ?content }".replaceAll("\\$id",pId);
		this.queryAnnotation(pModel, tQuery, pValue, pTarget);
	}

	protected ResultSet queryAnnotation(final Model pModel, final String pQuery, final String pValue, final String pTarget) {
		Query query = QueryFactory.create(pQuery) ;
		ResultSetRewindable results = null;
		boolean tFound = false;
		try (QueryExecution qexec = QueryExecutionFactory.create(query,pModel)) {

			results = ResultSetFactory.copyResults(qexec.execSelect());
			for ( ; results.hasNext() ; ) {
				tFound = true;
				QuerySolution soln = results.nextSolution() ;
				assertEquals("Content doesn't match.", "<p>" + pValue + "</p>", soln.getLiteral("content").toString());

				String tURI = soln.getResource("uri").toString() + "#" + soln.getLiteral("fragement");
				assertEquals("Target doesn't match", pTarget, tURI);
			}
		}
		results.reset();
		assertTrue("Expected to find annotation but found no results.", tFound);
		return results;
	}

	protected Statement matchesValue(final Model pModel, final Resource pResource, final String pProp, final String pValue) {
		StmtIterator tResults = pModel.listStatements(pResource, pModel.createProperty(pProp), (Resource)null);
		assertTrue("Missing " + pProp + " for resource " + pResource.getURI(), tResults.hasNext());
		Statement tResult = tResults.nextStatement();
		assertEquals("Value mismatch", pValue, tResult.getString());

		return tResult;
	}

	protected Statement matchesValue(final Model pModel, final Resource pResource, final String pProp, final Resource pValue) {
		StmtIterator tResults = pModel.listStatements(pResource, pModel.createProperty(pProp), (Resource)null);
		assertTrue("Missing " + pProp + " for resource " + pResource.getURI(), tResults.hasNext());
		Statement tResult = tResults.nextStatement();
		assertEquals("Value mismatch", pValue.getURI(), tResult.getResource().getURI());

		return tResult;
	}

    protected User createAdminUser() {
        User tUser = new User();
        try {
            tUser.setId("http://example.com/admin");
        } catch (URISyntaxException tExcpt) {
        }
        tUser.setAdmin(true);

        return tUser;
    }
}
