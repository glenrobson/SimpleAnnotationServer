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
import uk.org.llgc.annotation.store.encoders.Encoder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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

	public Encoder getEncoder() throws ServletException {
		Encoder tEncoder = null;
		if (_props.get("encoder") != null) {
			try {
				Class tClass = Class.forName(_props.get("encoder"));
				tEncoder = (Encoder)tClass.newInstance();
				tEncoder.init(_props);
			} catch (ClassNotFoundException tExcpt) {
				throw new ServletException("The Encoder you specified in the configuration is either incorrect or dosn't exist in the classpath " + _props.get("encoder"));
			} catch (InstantiationException tExcpt) {
				throw new ServletException("The Encoder must have a default constructor " + _props.get("encoder"));
			} catch (IllegalAccessException tExcpt) {
				throw new ServletException("The default constructor must be public " + _props.get("encoder"));
			}
		}
		return tEncoder;
	}

	public Map<String,String> getProps() {
		return _props;
	}

	protected static StoreConfig _config = null;
	protected static void initConfig(final StoreConfig pConfig) {
		_config = pConfig;
	}

	public static StoreConfig getConfig() {
		return _config;
	}
}
