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
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;

public class ManifestStats extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(ManifestStats.class.getName()); 
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;
	protected File _cacheDir = null;

    public ManifestStats() {
    }

    public ManifestStats(final StoreAdapter pStore) {
        _store = pStore;
    }

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
		Manifest tManifest = null;
		final long tCacheTimeout = 86400000; // 24 hours
		if (tCache.exists() && tCache.length() != 0 && (tCache.lastModified() + tCacheTimeout) > new Date().getTime()) {
			tManifest = new Manifest((Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(tCache)), tShortId);
		} else {
			tManifest = new Manifest((Map<String,Object>)JsonUtils.fromInputStream(new URL(tManifestURI).openStream()), tShortId);
			JsonUtils.write(new FileWriter(tCache), tManifest);
		}	

        List<List> tAnnoPageData = this.getStatsForManifest(tManifest);

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
			tResult = tResult.replaceAll("##LABEL##", tManifest.getLabel());
			tResult = tResult.replaceAll("##ANNO_PAGE_WIDTH##", "" + (tAnnoPageData.size() * 2));
			tResult = tResult.replaceAll("##PAGE_ANNO_COUNT##", JsonUtils.toPrettyString(getTranscribedTotals(tAnnoPageData)));
			DecimalFormat tFormatter= new DecimalFormat("#,###");
			tResult = tResult.replaceAll("##total_anno##", "" + tFormatter.format(getTotalAnnotations(tAnnoPageData)));

			pRes.setContentType("text/html");
			pRes.getOutputStream().println(tResult);
		} finally {
			if (tReader != null) {
				tReader.close();
			}
		}

	}

    public List<List> getStatsForManifest(final Manifest pManifest) throws IOException {
        List<List> tAnnoPageData = new ArrayList<List>();
		List tTitle = new ArrayList();
		tTitle.add("Page");
		tTitle.add("Count");
		tAnnoPageData.add(tTitle);

        List<PageAnnoCount> tPageCounts = _store.listAnnoPages(pManifest);
        // turn list to map
        Map<String, Integer> tFacetMap = new HashMap<String, Integer>();
        for (PageAnnoCount tCount : tPageCounts) {
            tFacetMap.put(tCount.getPageId(), tCount.getCount());
        }

        Map<String, Object> tManifest = pManifest.getJson();
        List<Map<String,Object>> tSequence = (List<Map<String,Object>>)tManifest.get("sequences");
        
        for (Map<String,Object> tCanvas : (List<Map<String,Object>>)(tSequence).get(0).get("canvases")) {
            int tCount = 0;
            if (tFacetMap.get(tCanvas.get("@id")) != null) {
                tCount = tFacetMap.get(tCanvas.get("@id"));
            }
            List tRow = new ArrayList();
            tRow.add((String)tCanvas.get("label"));
            tRow.add(tCount);
            tAnnoPageData.add(tRow);
        }

        
        return tAnnoPageData;
    }

    public List<List> getTranscribedTotals(final List<List> pPages) {
        int tTranscribed = 0;
        int tTotal = 0;
        for (List row : pPages) {
            // Skip title row
            if (row.get(1) instanceof Integer) {
                if (((int)row.get(1)) != 0) {
                    tTranscribed++;
                }
                tTotal++;
            }
        }    

        // For pie chart data format:
        // label, count
        // transcribed, count
        // still to do, count
		List<List> tAnnoPageCountData = new ArrayList<List>();
		List tPageCountResult = new ArrayList();
		tPageCountResult = new ArrayList();
		tAnnoPageCountData.add(tPageCountResult);
		tPageCountResult.add("Canvas type");
		tPageCountResult.add("count");

		
		tPageCountResult = new ArrayList();
		tAnnoPageCountData.add(tPageCountResult);
		tPageCountResult.add("Transcribed");
		tPageCountResult.add(tTranscribed);

		tPageCountResult = new ArrayList();
		tAnnoPageCountData.add(tPageCountResult);
		tPageCountResult.add("Still to do");
		tPageCountResult.add(tTotal - tTranscribed);
        
        return tAnnoPageCountData;
    }     
        
    public int getTotalAnnotations(final List<List> pPages) {
        int tTotal = 0;
        for (List row : pPages) {
            // Skip title row
            if (row.get(1) instanceof Integer) {
                tTotal += (int)row.get(1);
            }    
        }
        return tTotal;
    }

}
