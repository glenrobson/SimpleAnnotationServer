package uk.org.llgc.annotation.store;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Properties;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.adapters.JenaStore;
import uk.org.llgc.annotation.store.adapters.SesameStore;
import uk.org.llgc.annotation.store.adapters.SolrStore;
import uk.org.llgc.annotation.store.encoders.Encoder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StoreConfig extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(StoreConfig.class.getName());
	protected Map<String,String> _props = null;

	public StoreConfig() {
		_props = null;
	}

	// Called from test scripts
	public StoreConfig(final Properties pProps) {
		this.overloadConfigFromEnviroment(pProps);
		initConfig(this);
	}

  // Called by servlet
	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		String tConfigFile = pConfig.getInitParameter("config_file");
        boolean isRelative = true;
        if (pConfig.getInitParameter("relative") != null || pConfig.getInitParameter("relative").trim().length() != 0) {
            isRelative = pConfig.getInitParameter("relative").equals("true");
        }
		Properties tProps = new Properties();
		try {
            InputStream tPropsStream = null;
            if (isRelative) {
                tPropsStream = this.getServletContext().getResourceAsStream("/WEB-INF/" + tConfigFile);
            } else {
                tPropsStream = new FileInputStream(new File(tConfigFile));
            }

            tProps.load(tPropsStream);
		} catch (IOException tExcpt) {
			tExcpt.printStackTrace();
			throw new ServletException("Failed to load config file due to: " + tExcpt.getMessage());
		}
		this.overloadConfigFromEnviroment(tProps);
		initConfig(this);
	}

	protected void overloadConfigFromEnviroment(final Properties pProps) {
		_props = new HashMap<String,String>();
		Map<String, String> env = System.getenv();
		for (String tKey : pProps.stringPropertyNames()) {
			if (env.get("SAS." + tKey) != null) {
				_logger.debug("Overloading " + tKey + " with value " + env.get("SAS." + tKey) + " from ENV");
				_props.put(tKey, env.get("SAS." + tKey));
			} else {
				_props.put(tKey, pProps.getProperty(tKey));
			}
		}
	}


	public String getBaseURI(final HttpServletRequest pReq) {
		if (_props.containsKey("baseURI")) {
			return _props.get("baseURI");// external hostname
		} else {
			int tBase = 0;
			String[] tURL = pReq.getRequestURL().toString().split("/");
			String tServletName = "";
			if (pReq.getServletPath().matches(".*/[a-zA-Z0-9.]*$")) {
				tServletName = pReq.getServletPath().replaceAll("/[a-zA-Z0-9.]*$","").replaceAll("/","");
			} else {
				tServletName = pReq.getServletPath().replaceAll("/","");
			}
			for (int i = tURL.length - 1; i >=0 ; i--) {
				if (tURL[i].equals(tServletName)) {
					tBase = i;
					break;
				}
			}
			StringBuffer tBaseURL = new StringBuffer();
			for (int i = 0; i < tBase; i++) {
				tBaseURL.append(tURL[i] + "/");
			}
			String tBaseURLStr = tBaseURL.toString();
			if (tBaseURLStr.endsWith("/")) {
				 tBaseURLStr = tBaseURLStr.replaceAll("/$","");
			}
			return tBaseURLStr;
		}
	}



	public StoreAdapter getStore() {

		StoreAdapter tAdapter = null;
		String tStore = _props.get("store");
		if (tStore.equals("jena")) {
			tAdapter = new JenaStore(_props.get("data_dir"));
		} else if (tStore.equals("sesame")) {
			tAdapter = new SesameStore(_props.get("repo_url"));
		} else if (tStore.equals("solr") || tStore.equals("solr-cloud")) {
			String tCollection = null;
			if (tStore.equals("solr-cloud")) {
					tCollection = _props.get("solr_collection");
					if (tCollection == null || tCollection.trim().length() == 0) {
						throw new IllegalArgumentException("If you are using solr-cloud you must specify the solr_collection field.");
					}
			}
			tAdapter = new SolrStore(_props.get("solr_connection"), tCollection);
		} else {
            _logger.error("Couldn't find a store for '" + tStore + "'.");
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
