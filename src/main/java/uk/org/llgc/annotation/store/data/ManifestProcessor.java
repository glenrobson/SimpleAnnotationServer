package uk.org.llgc.annotation.store.data;

import java.util.Map;
import java.util.HashMap;

public class ManifestProcessor {
	public ManifestProcessor() {
	}

	public void process(final String pShortId, final String pBaseSearchURL, final Map<String, Object> pManifest) {
		/*
		 * "service": {
		 *		 "@context": "http://iiif.io/api/search/1/context.json",
		 *		 "@id": "http://example.org/services/identifier/search",
		 *		 "profile": "http://iiif.io/api/search/1/search"
		 *	  }
		 */
		Map<String, String> tService = new HashMap<String,String>();
		tService.put("@context","http://iiif.io/api/search/0/context.json");// "http://iiif.io/api/search/1/context.json");
		tService.put("@id", pBaseSearchURL + "/" + pShortId + "/search");
		tService.put("profile", "http://iiif.io/api/search/0/search"); //http://iiif.io/api/search/1/search");
		pManifest.put("service", tService);
	}
}
