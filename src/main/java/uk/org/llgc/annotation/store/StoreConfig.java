package uk.org.llgc.annotation.store;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;

import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.query.Dataset;

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
			Dataset tDataset = TDBFactory.createDataset(_props.get("data_dir"));
			tAdapter = new StoreAdapter(tDataset);
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
