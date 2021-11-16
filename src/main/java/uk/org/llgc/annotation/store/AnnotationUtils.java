package uk.org.llgc.annotation.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.github.jsonldjava.utils.JsonUtils;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdError;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.io.File;
import java.io.ByteArrayInputStream;

import java.security.MessageDigest;

import java.nio.charset.Charset;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import uk.org.llgc.annotation.store.encoders.Encoder;
import uk.org.llgc.annotation.store.encoders.Mirador214;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;

public class AnnotationUtils {
	protected static Logger _logger = LogManager.getLogger(AnnotationUtils.class.getName());

	protected File _contextDir = null;
	protected Encoder _encoder = null;

    // Called by servlet
    public AnnotationUtils() {
        try {
            _contextDir = StoreConfig.getConfig().getRealPath("/contexts"); // pContextDir;
            _encoder = StoreConfig.getConfig().getEncoder();
        } catch (javax.servlet.ServletException tExcpt) {
            //tExcpt.printStackTrace();
        }
    }

    // Called by tests
	public AnnotationUtils(final File pContextDir, final Encoder pEncoder) {
        _contextDir = pContextDir;
        _encoder = pEncoder;
    }

	/**
	 * Convert a IIIF annotation list into a list of annotations that have fragement
	 * identifiers
	 * @param InputStream the input stream to read to get the IIIF annotation list
	 */
	public List<Map<String,Object>> readAnnotationList(final InputStream pStream, final String pBaseURL) throws IOException {
		Object inputList = JsonUtils.fromInputStream(pStream);
        _logger.debug("Original untouched annotation list:");
        _logger.debug(JsonUtils.toPrettyString(inputList));
        List<Map<String,Object>> tAnnotations = null;
        if (inputList instanceof Map) {
            Map<String,Object> tAnnotationList = (Map<String,Object>)inputList;
            tAnnotations = (List<Map<String,Object>>)tAnnotationList.get("resources");
        } else if (inputList instanceof List) {
            tAnnotations = (List<Map<String,Object>>)inputList;
        } else {
            throw new IOException("Don't recognise annotation list " + inputList.getClass().getName());
        }


		//if (tAnnotationList.get("@id") == null) {
		//	_logger.debug(JsonUtils.toPrettyString(tAnnotationList));
		//	throw new IOException("Annotation list must have a @id at root");
		//}
		//String[] tListURI = ((String)tAnnotationList.get("@id")).split("/");
		//String tBucketId = tListURI[tListURI.length - 1].replaceAll(".json","");
		int tAnnoCount = 0;
		for (Map<String, Object> tAnno : tAnnotations) {
			if (tAnno.get("@id") == null) {
				tAnno.put("@id", this.generateAnnoId(pBaseURL, tAnnoCount++));
			}
			tAnno.put("@context", this.getContext()); // need to add context to each annotation fixes issue #18

			Map<String, Object> tResource = null;
			if (tAnno.get("resource") instanceof List) {
				tResource = (Map<String, Object>)((List)tAnno.get("resource")).get(0);
			} else {
				tResource = (Map<String, Object>)tAnno.get("resource");
			}
			// do I need to change the format to html?
            if (tResource.get("@type") != null && tResource.get("@type").equals("cnt:ContentAsText") || tResource.get("format") != null && tResource.get("format").equals("text/plain")) {
    			tResource.put("@type","dctypes:Text"); //requried for Mirador: js/src/annotations/osd-canvas-renderer.js:421:if (value["@type"] === "dctypes:Text") {
    			tResource.put("format","text/html");
                String tText = (String)tResource.get("chars");
    			if (!tText.trim().startsWith("<p>")) {
    				tResource.put("chars", "<p>" + tText + "</p>");
    			} else {
    				tResource.put("chars", tText);
    			}
            }
			// Not sure if this is strictly necessary:
			/*List<String> tMotivation = new ArrayList<String>();
			tMotivation.add("oa:commenting");
			tAnno.put("motivation", tMotivation); // replaces painting with commenting*/


			if (tAnno.get("on") instanceof String) {
				String[] tOnStr = ((String)tAnno.get("on")).split("#");

				Map<String,Object> tOnObj = new HashMap<String,Object>();
				tOnObj.put("@type", "oa:SpecificResource");
				tOnObj.put("full", tOnStr[0]);

				Map<String,Object> tSelector = new HashMap<String,Object>();
				tOnObj.put("selector", tSelector);
				tSelector.put("@type", "oa:FragmentSelector");
				tSelector.put("value", tOnStr[1]);

				tAnno.put("on", tOnObj);
			}

			if (_encoder != null) {
				_encoder.encode(tAnno);
			}
		}

        _logger.debug("Normalised annotation list:");
        _logger.debug(JsonUtils.toPrettyString(tAnnotations));
		return tAnnotations;
	}
    protected String getTarget(final Map<String, Object> pAnno) {
        if (pAnno.get("on") instanceof String) {
            String tTarget = (String)pAnno.get("on");
            if (tTarget.contains("#")) {
                return tTarget.substring(0,tTarget.indexOf("#"));
            } else {
                return tTarget;
            }
        } else {
            return (String)((Map<String,Object>)pAnno.get("on")).get("full");
        }
    }

    public static String getHash(String txt, String hashType) throws IOException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance(hashType);
            byte[] array = md.digest(txt.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < array.length; i++) {
                String hex = Integer.toHexString(0xFF & array[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("Failed to find algorithum " + hashType + " due to: " + e.toString());
        } catch (java.io.UnsupportedEncodingException tExcpt) {
            throw new IOException("Failed to convert string to md5 " + txt + " due to: " + tExcpt.toString());
        }
    }

	@SuppressWarnings("unchecked")
	public Map<String, Object> readAnnotaion(final InputStream pStream, final String pBaseURL) throws IOException {
		Object tAnnotation = JsonUtils.fromInputStream(pStream);
		Map<String, Object> tRoot = (Map<String,Object>)tAnnotation;

		if (tRoot.get("@id") == null) {
			String tID = this.generateAnnoId(pBaseURL);
			tRoot.put("@id", tID);
		}
		// Change context to local for quick processing
		tRoot.put("@context", this.getContext());

		if (_encoder != null) {
			_encoder.encode(tRoot);
		}
		return tRoot;
	}

	protected String getContext() {
        try {
    		return new File(_contextDir, "iiif-2.0.json").toURI().toURL().toString();
        } catch (IOException tExcpt) {
            _logger.error("Failed to find local context using external");
            return getExternalContext();

        }
	}

	public String getExternalContext() {
		return "http://iiif.io/api/presentation/2/context.json";
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> buildAnnotationListHead() {
		Map<String, Object> tRoot = (Map<String, Object>)new HashMap<String,Object>();
		tRoot.put("@context", getExternalContext());
		String tPageId = "tmp";
		tRoot.put("@id", "http://dams.llgc.org.uk/iiif/annotation/list/" + tPageId);// current URL TODO make better id and resolvable
		tRoot.put("@type", "sc:AnnotationList");
		List tResources = new ArrayList();
		tRoot.put("resources", tResources);

		return tRoot;
	}

	public Map<String,Object> createAnnotationList(final Model pAnnotation) throws IOException, MalformedAnnotation {
		List<Model> tTmpList = new ArrayList<Model>();
		tTmpList.add(pAnnotation);

		return createAnnotationList(tTmpList).get(0);
	}

	public List<Map<String,Object>> createAnnotationList(final List<Model> pAnnotations) throws IOException, MalformedAnnotation {
		final Object contextJson = JsonUtils.fromInputStream(new FileInputStream(new File(_contextDir, "annotation_frame.json")));
		((Map<String, Object>)contextJson).put("@context", this.getContext());

		final JsonLdOptions options = new JsonLdOptions();
		options.format = "application/jsonld";

		Map<String, Object> tRoot = this.buildAnnotationListHead();
		List<Map<String,Object>> tResources = (List<Map<String,Object>>)tRoot.get("resources");
		for (Model tAnnotation : pAnnotations) {
            Annotation tAnno = null;
			try {
				tAnno = new Annotation(this.frameAnnotation(tAnnotation, false));
				if (tAnno != null && tAnno.checkValid() != "") { // Only add if its a valid annotation
					tResources.add(tAnno.toJson());
				}
			} catch (JsonLdError tExcpt) {
				_logger.error("Failed to generate Model " + tAnno.getId() + "  due to " + tExcpt);
				tExcpt.printStackTrace();
			}
		}

		return tResources;//tRoot;
	}

	public Model convertAnnoToModel(final Map<String,Object> pJson) throws IOException {
		if (pJson.get("@context") == null) {
			pJson.put("@context", this.getContext());
		} else if (pJson.get("@context") instanceof String && ((String)pJson.get("@context")).trim().startsWith("file://")) {
			File tContext = new File(((String)pJson.get("@context")).trim().substring("file://".length()));
			if (!tContext.exists()) {
				pJson.put("@context", this.getContext());
			}
		}
		String tJson = JsonUtils.toString(pJson);

		Model tJsonLDModel = ModelFactory.createDefaultModel();
		RDFDataMgr.read(tJsonLDModel, new ByteArrayInputStream(tJson.getBytes(Charset.forName("UTF-8"))), Lang.JSONLD);

		return tJsonLDModel;
	}

	public Map<String,Object> frameAnnotation(final Model pAnno, final boolean pCollapse) throws JsonLdError, IOException  {
		final Map<String,Object> contextJson = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(new File(_contextDir, "annotation_frame.json")));
		Map<String,Object> tJsonLd = this.frame(pAnno, contextJson);
		if (pCollapse) {
			this.colapseFragement(tJsonLd);
		}
		Map<String, Object> tOn = null;
		if (tJsonLd.get("on") instanceof Map) {
			tOn = (Map<String, Object>)tJsonLd.get("on");
			if (tOn.get("selector") != null && ((Map<String,Object>)tOn.get("selector")).get("value") instanceof List || tOn.get("source") instanceof List) {
				_logger.error("Annotation is broken " + tJsonLd.get("@id"));
				return tJsonLd;
			}
            // Autmoatically fix mirador-2.1.4 annos as if its done in javascript then exports won't include fix so won't work in Mirador
            // Do it automatically for mirador2.1.4 annos rather than using an encoder as we only want to fix this kind
            // of annos rather than all of them.
            if (((Map<String,Object>)tJsonLd.get("on")).get("selector") != null && ((Map<String,Object>)tJsonLd.get("on")).get("selector") != null && ((Map<String,Object>)((Map<String,Object>)tJsonLd.get("on")).get("selector")).get("item") != null) {
                Encoder tEncoder = new Mirador214();
                tEncoder.encode(tJsonLd);
            }
		}
		// Check if this is a valid annotation
		// if it is valid it should have one source, one fragment selector
		if (_encoder != null) {
			_encoder.decode(tJsonLd);
		}
		return tJsonLd;
	}


	public Map<String,Object> frameManifest(final Model pManifest) throws JsonLdError, IOException  {
		final Map<String,Object> tContextJson = (Map<String,Object>)JsonUtils.fromInputStream(new URL("https://iiif.io/api/presentation/2/manifest_frame.json").openStream());
		return this.frame(pManifest, tContextJson);
	}

    public Map<String,Object> frameCollection(final Model pCollection) throws JsonLdError, IOException  {
		final Map<String,Object> tFrameJson = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(new File(_contextDir,"collection_frame.json")));
		Map<String,Object> tCollectionsJson = this.frame(pCollection, tFrameJson);
        tCollectionsJson.remove("sc:hasCollections");
        tCollectionsJson.remove("sc:hasManifests");
        tCollectionsJson.remove("sc:hasParts");
        if (tCollectionsJson.get("dcterms:creator") instanceof Map) {
            tCollectionsJson.put("dcterms:creator", ((Map<String,Object>)tCollectionsJson.get("dcterms:creator")).get("@id"));
        }
        return tCollectionsJson;
	}


	public Map<String,Object> frame(final Model pModel, final Map<String,Object> pFrame) throws JsonLdError, IOException {
		pFrame.put("@context", this.getContext());

		final JsonLdOptions tOptions = new JsonLdOptions();
		tOptions.format = "application/jsonld";
		tOptions.setPruneBlankNodeIdentifiers(true);

		StringWriter tStringOut = new StringWriter();
        if (pModel.supportsTransactions()) {
            pModel.begin();
        }
		RDFDataMgr.write(tStringOut, pModel, Lang.JSONLD);
        if (pModel.supportsTransactions()) {
            pModel.commit();
        }
		Map<String,Object> tFramed = (Map<String,Object>)JsonLdProcessor.frame(JsonUtils.fromString(tStringOut.toString()), pFrame,  tOptions);
		Map<String,Object> tJsonLd = (Map<String,Object>)((List)tFramed.get("@graph")).get(0);
		if (tJsonLd.get("@context") == null) {
			tJsonLd.put("@context", this.getExternalContext());
		}
		return tJsonLd;
	}

	// Need to move fragement into on
	public void colapseFragement(final Map<String,Object> pAnnotationJson) {
		if (pAnnotationJson.get("on") instanceof Map) {
            collapseFragmentOn(pAnnotationJson, (Map<String,Object>)pAnnotationJson.get("on"));
		} else if (pAnnotationJson.get("on") instanceof List) {
            for (Map<String,Object> tOn : (List<Map<String,Object>>)pAnnotationJson.get("on")) {
                collapseFragmentOn(pAnnotationJson, tOn);
            }
		}	// otherwise its already collapsed as its a string
	}
	public void collapseFragmentOn(final Map<String,Object> pAnnotationJson, final Map<String,Object> pOn) {
		if (pOn.get("selector") != null) {
			try {
                String tFragement = "";
                if (((Map)pOn.get("selector")).get("value") != null) {
    				tFragement = (String)((Map)pOn.get("selector")).get("value");
                } else {
    				tFragement = (String)((Map)((Map)pOn.get("selector")).get("default")).get("value");
                }
				String tTarget = (String)pOn.get("full");
				pAnnotationJson.put("on", tTarget + "#" + tFragement);
			} catch (ClassCastException tExcpt) {
				System.err.println("Failed to transform annotation");
				try {
					System.out.println(JsonUtils.toPrettyString(pAnnotationJson));
				} catch (IOException	tIOExcpt) {
					System.out.println("Failed to print failing annotation " + tIOExcpt);
				}
				throw tExcpt;
			}
		}
	}

	protected String generateAnnoId(final String pBaseURL) {
        StringBuffer tBuff = new StringBuffer(pBaseURL);
        if (!pBaseURL.endsWith("/")) {
            tBuff.append("/");
        }
        tBuff.append("anno/");
        tBuff.append("" + new java.util.Date().getTime());
		return tBuff.toString();
	}

    // To be used when potenially lots of annotations are added at once
    protected String generateAnnoId(final String pBaseURL, final int pOffest) {
        StringBuffer tBuff = new StringBuffer(this.generateAnnoId(pBaseURL));
        tBuff.append("/");
        tBuff.append("" + pOffest);
		return  tBuff.toString();
	}

}
