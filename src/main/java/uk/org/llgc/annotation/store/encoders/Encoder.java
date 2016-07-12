package uk.org.llgc.annotation.store.encoders;

import java.util.Map;

public interface Encoder {
	public void init(final Map<String,String> pProps);
	/**
	 * Process the JSONLD and add any extra bits before storing in store
	 */
	public void encode(final Map<String, Object> pModel);
	/**
	 * Turn store model back into html for Mirador
	 */
	public void decode(final Map<String, Object> pModel);
}
