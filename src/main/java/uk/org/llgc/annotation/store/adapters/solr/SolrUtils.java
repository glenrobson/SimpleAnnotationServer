package uk.org.llgc.annotation.store.adapters.solr;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SolrUtils {
	protected static Logger _logger = LogManager.getLogger(SolrUtils.class.getName());
	protected SolrClient _solrClient = null;

    public SolrUtils(final SolrClient pClient) {
        _solrClient = pClient;
    }

	// Solr Helper methods
	public void addDoc(final SolrInputDocument pDoc, final boolean pCommit) throws IOException {
		try {
			UpdateResponse tResponse = _solrClient.add(pDoc);

			if (pCommit) {
				_solrClient.commit();
			}
		} catch (SolrServerException tExcpt) {
			tExcpt.printStackTrace();
			throw new IOException("Failed to remove annotations due to " + tExcpt);
		}
	}


	public void addSingle(final SolrInputDocument pDoc, final String pKey, final Object pObject) {
		if (pObject != null) {
			if (pObject instanceof String) {
				pDoc.addField(pKey, (String)pObject);
			} else {
				pDoc.addField(pKey, pObject);
			}
		}
	}

	public void addMultiple(final SolrInputDocument pDoc, final String pKey, final Object pObject) {
		if (pObject != null) {
			if (pObject instanceof List) {
				for (String tValue: (List<String>)pObject) {
					pDoc.addField(pKey, tValue);
				}
			} else {
				pDoc.addField(pKey, (String)pObject);
			}
		}
	}

	public SolrQuery getQuery() {
		SolrQuery tQuery = new SolrQuery();
		tQuery.setFields("id", "type", "motivation","body", "target","selector", "short_id", "within", "data", "canvas", "label");
		tQuery.setRows(1000);

		return tQuery;
	}

	public String escapeChars(final String pParam) {
		return pParam.replaceAll(":","\\\\:");
	}

	public boolean isType(final SolrDocument pResult, final String pType) {
		if (pResult.get("type") != null) {
			if (pResult.get("type") instanceof List) {
				List<String> tTypes = (List<String>)pResult.get("type");
				return tTypes.contains(pType);
			} else {
				return ((String)pResult.get("type")).equals(pType);
			}
		} else {
			return false;
		}
	}

	public void addSingleJson(final Map<String,Object> pParent, final String pKey, final Object pValue) {
		if (pValue != null) {
			pParent.put(pKey, (String)pValue);
		}
	}

	public void createItemListIfMultiple(final Map<String,Object> pParent, final String pKey, final Object pValue) {
		if (pValue != null) {
			if (pValue instanceof List) {
				List<String> tValues = (List<String>)pValue;
				if (tValues.size() == 1) {
					pParent.put(pKey, tValues.get(0));
				} else {
					this.createItemList(pParent, pKey, pValue);
				}
			} else {
				pParent.put(pKey, (String)pValue);
			}
		}
	}

	public void createItemList(final Map<String,Object> pParent, final String pKey, final Object pValue) {
		if (pValue != null) {
			List<String> tList = new ArrayList<String>();
			if (pValue instanceof List) {
				for (String tValueInst : (List<String>)pValue) {
					tList.add(tValueInst);
				}
			} else {
				tList.add((String)pValue);
			}
			pParent.put(pKey, tList);
		}
	}
}
