package uk.org.llgc.annotation.store;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.adapters.JenaStore;
import uk.org.llgc.annotation.store.adapters.SesameStore;

public class StoreConfig extends HttpServlet {
	protected Map<String,String> _props = null;
	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_props = new HashMap<String,String>();
		Enumeration<String> tParams = (Enumeration<String>)pConfig.getInitParameterNames();
		while(tParams.hasMoreElements()) {
			String tKey = tParams.nextElement();
			_props.put(tKey, pConfig.getInitParameter(tKey));
		}
		initConfig(this);
	}

	public StoreAdapter getStore() {
		StoreAdapter tAdapter = null;
		if (_props.get("store").equals("jena")) {
			tAdapter = new JenaStore(_props.get("data_dir"));
		}	
		if (_props.get("store").equals("sesame")) {
			tAdapter = new SesameStore(_props.get("repo_url"));
		}

		return tAdapter;
	}


	protected static StoreConfig _config = null;
	public static void initConfig(final StoreConfig pConfig) {
		_config = pConfig;
	}

	public static StoreConfig getConfig() {
		return _config;
	}
}
