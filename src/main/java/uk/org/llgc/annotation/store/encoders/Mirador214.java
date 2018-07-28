package uk.org.llgc.annotation.store.encoders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import java.io.StringReader;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;
import org.jdom2.JDOMException;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.Format;

import java.awt.Rectangle;

import com.github.jsonldjava.utils.JsonUtils;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdError;

import java.util.UUID;

public class Mirador214 implements Encoder {
	protected static Logger _logger = LogManager.getLogger(Mirador214.class.getName());

	protected Map<String,String> _props = null;
	protected Map<String,Object> _context = null;
	public Mirador214() {
	}

	public void init(final Map<String,String> pProps) {
		_props = pProps;
	}

	/**
	 * Ensure on is an array and add svg connector if required
	 */
	public void encode(final Map<String, Object> pModel) {
        this.ensureOnIsList(pModel);
        //this.ensureSvgChoice(pModel);
	}

	/**
	 * Decode RDF into RDFa/HTML for mirador
	 */
	public void decode(final Map<String, Object> pModel) {
        this.ensureOnIsList(pModel);
        //this.ensureSvgChoice(pModel);
	}

    protected void ensureOnIsList(final Map<String, Object> pJson) {
        if (pJson.get("on") instanceof Map) {
            Map<String,Object> tSelector = ((Map<String,Map<String,Object>>)pJson.get("on")).get("selector");
            if (tSelector.get("item") != null) {
                Map<String, Object> tOn = (Map<String,Object>)pJson.get("on");
                List<Map<String,Object>> tListOn = new ArrayList<Map<String,Object>>();
                tListOn.add(tOn);
                pJson.put("on",tListOn);
            }
        } // TODO handle string values
    }

    /**
     * Convert:
     * "selector": {
     *        "@type": "oa:FragmentSelector",
     *        "value": "xywh=275,637,681,290"
     *  }
     * to:
     * "selector": {
     *  "@type": "oa:Choice",
     *  "default": {
     *     "@type": "oa:FragmentSelector",
     *     "value": "xywh=1698,2663,1384,221"
     *  },
     *  "item": {
     *     "@type": "oa:SvgSelector",
     *     "value": "<svg xmlns='http://www.w3.org/2000/svg'><path xmlns=\"http://www.w3.org/2000/svg\" d=\"M1697.58302,2663.45269l692.03247,0l0,0l692.03247,0l0,110.49258l0,110.49258l-692.03247,0l-692.03247,0l0,-110.49258z\" data-paper-data=\"{&quot;defaultStrokeValue&quot;:1,&quot;editStrokeValue&quot;:5,&quot;currentStrokeValue&quot;:1,&quot;rotation&quot;:0,&quot;deleteIcon&quot;:null,&quot;rotationIcon&quot;:null,&quot;group&quot;:null,&quot;editable&quot;:true,&quot;annotation&quot;:null}\" id=\"rectangle_13145ec0-7fce-4cd1-9d37-60b112469f4e\" fill-opacity=\"0\" fill=\"#00bfff\" fill-rule=\"nonzero\" stroke=\"#00bfff\" stroke-width=\"5.8154\" stroke-linecap=\"butt\" stroke-linejoin=\"miter\" stroke-miterlimit=\"10\" stroke-dasharray=\"\" stroke-dashoffset=\"0\" font-family=\"none\" font-weight=\"none\" font-size=\"none\" text-anchor=\"none\" style=\"mix-blend-mode: normal\"/></svg>"
     *  }
     *},
     */
    protected void ensureSvgChoice(final Map<String, Object> pJson) {
        if (pJson.get("on") instanceof Map) {
            this.ensureOnIsList(pJson);
        }
        for (Map<String,Object> tTarget : (List<Map<String,Object>>)pJson.get("on")) {
            Map<String,Object> tSelector = (Map<String,Object>)tTarget.get("selector");
            if (!tSelector.get("@type").equals("oa:Choice")) {
                Map<String,String> tDefault = new HashMap<String,String>();
                tDefault.put("@type", (String)tSelector.get("@type"));
                tDefault.put("value", (String)tSelector.get("value"));

                Map<String,String> tSvg = new HashMap<String,String>();
                tSvg.put("@type", "oa:SvgSelector");
                tSvg.put("value", convertRectoSVG((String)tSelector.get("value")));

                tSelector = new HashMap<String,Object>();
                tSelector.put("@type", "oa:Choice");
                tSelector.put("default", tDefault);
                tSelector.put("item", tSvg);

                tTarget.put("selector", tSelector);
            }
        }
    }

    private int getInt(final String pInt) {
        return Integer.parseInt(pInt);
    }

    protected String convertRectoSVG(final String pDimensions) {
        // xywh=275,637,681,290
        String tNumString = pDimensions;
        if (pDimensions.contains("=")) {
            tNumString = pDimensions.split("=")[1];
        }
        String[] tValues = tNumString.split(",");
        Rectangle tRect = new Rectangle(getInt(tValues[0]), getInt(tValues[1]), getInt(tValues[2]), getInt(tValues[3]));

        Namespace SVG_NS = Namespace.getNamespace("http://www.w3.org/2000/svg");
        Document tSVGXml = new Document();
        Element tRoot = new Element("svg", SVG_NS);
        tSVGXml.setRootElement(tRoot);

        Element tPath = new Element("path", SVG_NS);
        tRoot.addContent(tPath);

        tPath.setAttribute("id","rectangle_" + UUID.randomUUID().toString());
        tPath.setAttribute("test","true");
        Format tFormat = Format.getCompactFormat();
        tFormat.setOmitDeclaration(true);
        tFormat.setOmitEncoding(true);
        XMLOutputter tOut = new XMLOutputter(tFormat);
        return tOut.outputString(tSVGXml);
    }
}
