package uk.org.llgc.annotation.store.test;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.primefaces.model.chart.PieChartModel;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.contollers.StatsService;

import com.github.jsonldjava.utils.JsonUtils;

import java.util.Map;
import java.util.List;
import java.util.Set;

public class TestStats extends TestUtils {
	protected static Logger _logger = LogManager.getLogger(TestSearch.class.getName());

    public TestStats() throws IOException {
        super();
        _logger.debug("Called super()");
    }

    @Before
    public void setup() throws IOException {
        super.setup();
        _logger.debug("Called setup()");
    }

    @After
    public void tearDown() throws IOException {
        super.tearDown();
    }

    @Test
    public void listManifest() throws IOException, IDConflictException, MalformedAnnotation {
        List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/stats_AnnotationList.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

        // Upload Newspaper annotation list
        _store.addAnnotationList(tAnnotationListJSON);

        // Upload Manifest
		Map<String, Object> tManifest = (Map<String, Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/stats_manifest.json").getFile())); //annotaiton list
        String tShortId = _store.indexManifest(tManifest);

        List<Manifest> tManifests = _store.getManifests();
        assertEquals("Found " + tManifests.size() + " expected 1", 1, tManifests.size());

        assertEquals("Found different manifest that expected.", "https://damsssl.llgc.org.uk/iiif/2.0/1132230/manifest.json", tManifests.get(0).getURI());
    }

    @Test
    public void testStats() throws IOException, IDConflictException, MalformedAnnotation {
        List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/stats_AnnotationList.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

        // Upload Newspaper annotation list
        _store.addAnnotationList(tAnnotationListJSON);

        // Upload Manifest
		Map<String, Object> tManifestJson = (Map<String, Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/stats_manifest.json").getFile())); //annotaiton list
        String tShortId = _store.indexManifest(tManifestJson);
        Manifest tManifest = new Manifest(tManifestJson, tShortId);
        StatsService tStats = new StatsService();
        tStats.init();

        List<PageAnnoCount> tPageCounts = tStats.getManifestAnnoCount(tManifest);
        assertEquals("Expected size of 1.", 1, tPageCounts.size());
        assertEquals("Expected 8 annotations for the first page.", 8, tPageCounts.get(0).getCount());

        // Now test how many images are left to do.
        PieChartModel tModel = tStats.getPercentAnnotated(tManifest.getShortId());
        assertEquals("Expected a pie chart of size 2",tModel.getData().size(), 2);
        Set<String> tKeys = tModel.getData().keySet();
        int tDone = 0;
        int tToDo = 0;
        for (String tKey : tKeys) {
            if (tKey.contains("Canvases with annotations")) {
                tDone = tModel.getData().get(tKey).intValue();
            } else {
                tToDo = tModel.getData().get(tKey).intValue();
            }
        }
        
        assertEquals("Expected done to be 1 page.", tDone, 1);
        assertEquals("Expected to be done to be 1 page.", tToDo, 1);
        assertEquals("Expected to be done and done to add up to the number of canvases", tToDo + tDone, tManifest.getCanvases().size());

        PageAnnoCount tPage1 = tPageCounts.get(0);
        assertEquals("Mistmatch with canvas label ",  "[Aberystwyth] - page 1", tPage1.getLabel());
        assertEquals("Mistmatch between annotation count and expected manifest", "https://damsssl.llgc.org.uk/iiif/2.0/1132230/manifest.json", tPage1.getManifest().getURI());
        assertEquals("Mistmatch between expected Manifest label", "[Aberystwyth]", tPage1.getManifest().getLabel());
    }
}
