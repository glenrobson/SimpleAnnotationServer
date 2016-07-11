package uk.org.llgc.annotation.store.adapters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Syntax;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import info.aduna.iteration.Iterations;

import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.URI;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;


import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import com.github.jsonldjava.utils.JsonUtils;

import java.nio.charset.Charset;

public class SesameStore extends AbstractStoreAdapter implements StoreAdapter {
	protected static Logger _logger = LogManager.getLogger(SesameStore.class.getName()); 

	protected Repository _repo = null;

	public SesameStore(final String pRepositoryURL) {
		_repo = new HTTPRepository(pRepositoryURL);
		_logger.debug("Prefered RDF Format " + ((HTTPRepository)_repo).getPreferredRDFFormat());
	}

	public Model updateAnnotation(final Map<String,Object> pJson) throws IOException {
		// delete first as a update on an existing context retains the original data
		this.deleteAnnotation((String)pJson.get("@id"));
		return addAnnotationSafe(pJson);
	}

	public Model addAnnotationSafe(final Map<String,Object> pJson) throws IOException {
		Resource tContext = _repo.getValueFactory().createURI((String)pJson.get("@id"));
		pJson.put("@context","http://localhost:8080/bor/contexts/iiif-2.0.json"); // must have a remote context for a remote repo
		Map<String,Object> tOn = (Map<String,Object>)pJson.get("on");
		tOn.remove("scope");
		String tJson = JsonUtils.toString(pJson);
		_logger.debug("Seasme json:");
		_logger.debug(tJson);

		RepositoryConnection tConn = null;
		InputStream tInput = null;
		try {
			tConn = _repo.getConnection();
			tInput = new ByteArrayInputStream(tJson.getBytes(Charset.forName("UTF-8")));
			tConn.add(tInput, null, RDFFormat.JSONLD, tContext);
		} catch (RepositoryException tExcpt) {
			_logger.error("Problem connecting to Sesame " + tExcpt.getMessage());

			tExcpt.printStackTrace();
			throw new IOException("Problem connecting to Sesame " + tExcpt.getMessage());
		} catch (RDFParseException tExcpt) {
			_logger.error("Problem parsing Json " + tExcpt.getMessage());
			tExcpt.printStackTrace();
			throw new IOException("Problem connecting to Sesame " + tExcpt.getMessage());
		} finally {
			try {
				if (tConn != null) {
					tConn.close();
				}	
			} catch (RepositoryException tExcpt) {
				_logger.error("Problem closing the connecting to Sesame " + tExcpt.getMessage());
				tExcpt.printStackTrace();
				throw new IOException("Problem closing connecting to Sesame " + tExcpt.getMessage());
			} finally {
				tConn = null;
			}
			if (tInput != null) {
				tInput.close();
			}	
		}

		return this.getModelFromContext(tContext);
	}

	protected Model getNamedModel(final String pContext) throws IOException {
		return this.getModelFromContext(this.createURI(pContext));
	}

	protected Model getModelFromContext(final Resource pContext) throws IOException {
		RepositoryConnection tConn = null;
		ByteArrayOutputStream tByteOut = null;
		try {
			tConn = _repo.getConnection();
			RepositoryResult<Statement> tStatements = tConn.getStatements(null, null, null, false, pContext); //really want to add statments to jena model statement by statement
			org.openrdf.model.Model tAnno = Iterations.addAll(tStatements, new LinkedHashModel());
			if (tAnno.isEmpty()) {
				return null;
			} else {
				tByteOut = new ByteArrayOutputStream();
				Rio.write((Iterable<Statement>)tAnno, tByteOut, RDFFormat.N3);
			}
		} catch (RepositoryException tExcpt) {
			_logger.error("Problem connecting to Sesame " + tExcpt.getMessage());
			tExcpt.printStackTrace();
			throw new IOException("Problem connecting to Sesame " + tExcpt.getMessage());
		} catch (RDFHandlerException tExcpt) {
			_logger.error("Problem retrieving anno from Sesame " + tExcpt.getMessage());
			tExcpt.printStackTrace();
			throw new IOException("Problem retrieving anno from Sesame " + tExcpt.getMessage());
		} finally {
			try {
				if (tConn != null) {
					tConn.close();
				}	
			} catch (RepositoryException tExcpt) {
				_logger.error("Problem closing the connecting to Sesame " + tExcpt.getMessage());
				tExcpt.printStackTrace();
				throw new IOException("Problem closing connecting to Sesame " + tExcpt.getMessage());
			} finally {
				tConn = null;
			}
		}


		Model tJsonLDModel = ModelFactory.createDefaultModel();
		RDFDataMgr.read(tJsonLDModel, new ByteArrayInputStream(tByteOut.toByteArray()), Lang.N3);
		return tJsonLDModel;
	}

	public List<Model> addAnnotationList(final List<Map<String,Object>> pJson) throws IOException, IDConflictException {
		List<Model> tAnnos = new ArrayList<Model>();
		for (Map<String, Object> tJsonObj : pJson) {
			tAnnos.add(this.addAnnotation(tJsonObj));
		}
		return tAnnos;
	}

	public void deleteAnnotation(final String pAnnoId) throws IOException {
		Resource tContext = _repo.getValueFactory().createURI(pAnnoId);
		RepositoryConnection tConn = null;
		try {
			tConn = _repo.getConnection();
			tConn.clear(tContext);
		} catch (RepositoryException tExcpt) {
			_logger.error("Problem connecting to Sesame " + tExcpt.getMessage());
			tExcpt.printStackTrace();
			throw new IOException("Problem connecting to Sesame " + tExcpt.getMessage());
		} finally {
			try {
				if (tConn != null) {
					tConn.close();
				}	
			} catch (RepositoryException tExcpt) {
				_logger.error("Problem closing the connecting to Sesame " + tExcpt.getMessage());
				tExcpt.printStackTrace();
				throw new IOException("Problem closing connecting to Sesame " + tExcpt.getMessage());
			} finally {
				tConn = null;
			}
		}
	}


	protected QueryExecution getQueryExe(final String pQuery) {
		return new QueryEngineHTTP(((HTTPRepository)_repo).getRepositoryURL(),pQuery);
	}

	protected Resource createURI(final String pURI) {
		return  _repo.getValueFactory().createURI(pURI);
	}
}
