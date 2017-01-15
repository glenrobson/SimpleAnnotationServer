package uk.org.llgc.annotation.store.stats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;

import com.github.jsonldjava.utils.JsonUtils;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.SolrServerException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import java.text.DecimalFormat;

import java.net.URL;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.adapters.SolrStore;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;

public class ManifestStats extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(ManifestStats.class.getName()); 
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;
	protected File _cacheDir = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")),StoreConfig.getConfig().getEncoder());
		_store = StoreConfig.getConfig().getStore();
		_store.init(_annotationUtils);
		_cacheDir = new File(pConfig.getServletContext().getRealPath("stats"));
		if (!_cacheDir.exists()) {
			_cacheDir.mkdirs();
		}
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		String[] tRequestURI = pReq.getRequestURI().split("/");
		String tShortId = tRequestURI[tRequestURI.length -1].replaceAll(".html","");
		_logger.debug("Short ID " + tShortId);
		String tManifestURI = _store.getManifestId(tShortId);

		File tCache = new File(_cacheDir,tShortId + ".json");
		System.out.println(tCache.getPath());
		Map<String, Object> tManifest = null;
		final long tCacheTimeout = 86400000; // 24 hours
		if (tCache.exists() && tCache.length() != 0 && (tCache.lastModified() + tCacheTimeout) > new Date().getTime()) {
			System.out.println("reading from cache");
			tManifest = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(tCache));
		} else {
			System.out.println("Getting remote manifest");
			tManifest = (Map<String,Object>)JsonUtils.fromInputStream(new URL(tManifestURI).openStream());
			JsonUtils.write(new FileWriter(tCache), tManifest);
		}	

		List<List> tAnnoPageData = new ArrayList<List>();
		List tTitle = new ArrayList();
		tTitle.add("Page");
		tTitle.add("Count");
		tAnnoPageData.add(tTitle);
		int tPageCount = 0;
		int tAnnoPageCount = 0;
		long tTotalAnnos= 0;
		try {
			SolrQuery tQuery = new SolrQuery();
			tQuery.setRows(0);
			tQuery.setFacet(true);
			tQuery.addFacetField("target");
			tQuery.setFacetLimit(-1);
			tQuery.setFacetMinCount(1);
			tQuery.setFacetSort("index");
			tQuery.set("q", "type:oa\\:Annotation AND within:" + tManifestURI.replaceAll(":","\\\\:"));

			QueryResponse tResponse  = ((SolrStore)_store).getClient().query(tQuery);
			tTotalAnnos = tResponse.getResults().getNumFound();
			FacetField tFacetCounts = tResponse.getFacetField("target");
			Map<String,Long> tFacetMap = new HashMap<String,Long>();
			for (FacetField.Count tFacetValue : tFacetCounts.getValues()) {
				tFacetMap.put(tFacetValue.getName(), tFacetValue.getCount());
			}
			tAnnoPageCount = tFacetCounts.getValues().size();

			List<Map<String,Object>> tSequence = (List<Map<String,Object>>)tManifest.get("sequences");
			
			for (Map<String,Object> tCanvas : (List<Map<String,Object>>)(tSequence).get(0).get("canvases")) {
				long tCount = 0;
				if (tFacetMap.get(tCanvas.get("@id")) != null) {
					tCount = tFacetMap.get(tCanvas.get("@id"));
				}
				List tRow = new ArrayList();
				tRow.add((String)tCanvas.get("label"));
				tRow.add(tCount);
				tAnnoPageData.add(tRow);
			}
			tPageCount = ((List<Map<String,Object>>)tSequence.get(0).get("canvases")).size();
		} catch (SolrServerException tException) {
			_logger.error("Failed to retrieve Manifest stats " + tException.toString());
			throw new IOException("Failed to retrieve Manifest stats due to " + tException.toString());
		}	

		List<List> tAnnoPageCountData = new ArrayList<List>();
		List tPageCountResult = new ArrayList();
		tPageCountResult = new ArrayList();
		tAnnoPageCountData.add(tPageCountResult);
		tPageCountResult.add("Canvas type");
		tPageCountResult.add("count");

		
		tPageCountResult = new ArrayList();
		tAnnoPageCountData.add(tPageCountResult);
		tPageCountResult.add("Transcribed");
		tPageCountResult.add(tAnnoPageCount);

		tPageCountResult = new ArrayList();
		tAnnoPageCountData.add(tPageCountResult);
		tPageCountResult.add("Still to do");
		tPageCountResult.add(tPageCount - tAnnoPageCount);

		File tTemplate = new File(new File(super.getServletContext().getRealPath("/templates")), "manifest.stats.template");
		BufferedReader tReader = null;
		try {
			tReader = new BufferedReader(new FileReader(tTemplate));
			String tLine = null;
			StringBuffer tHTML = new StringBuffer();
			while ((tLine = tReader.readLine()) != null ) {
				tHTML.append(tLine);
			}

			String tResult = tHTML.toString().replaceAll("##ANNO_PAGE_DATA##", JsonUtils.toPrettyString(tAnnoPageData));
			tResult = tResult.replaceAll("##LABEL##", (String)tManifest.get("label"));
			tResult = tResult.replaceAll("##ANNO_PAGE_WIDTH##", "" + (tPageCount * 2));
			tResult = tResult.replaceAll("##PAGE_ANNO_COUNT##", JsonUtils.toPrettyString(tAnnoPageCountData));
			DecimalFormat tFormatter= new DecimalFormat("#,###");
			tResult = tResult.replaceAll("##total_anno##", "" + tFormatter.format(tTotalAnnos));

			pRes.setContentType("text/html");
			pRes.getOutputStream().println(tResult);
		} finally {
			if (tReader != null) {
				tReader.close();
			}
		}

	}
}
