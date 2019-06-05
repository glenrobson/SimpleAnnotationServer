package uk.org.llgc.annotation.store.test;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import uk.org.llgc.annotation.store.stats.ManifestStats;

import com.github.jsonldjava.utils.JsonUtils;

import java.util.Map;
import java.util.List;

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
        ManifestStats tStats = new ManifestStats(_store);
        List<List> tPageCounts = tStats.getStatsForManifest(tManifest);

        assertEquals("Expected size of 3, 1 title row and two pages.",tPageCounts.size(), 3);
        assertEquals("Expected 8 annotations for the first page.",tPageCounts.get(1).get(1), 8);

        // Now test how many images are left to do.
        List<List> tTotals = tStats.getTranscribedTotals(tPageCounts);
        assertEquals("Expected size of 3, 1 title row and two total rows.",tTotals.size(), 3);
        assertEquals("Expected done to be 1 page.",tTotals.get(1).get(1), 1);
        assertEquals("Expected todo to be 1 page.",tTotals.get(2).get(1), 1);

        int tTotalAnnos = tStats.getTotalAnnotations(tPageCounts);
        assertEquals("Expected 8 annos.",tTotalAnnos, 8);
    }
}
