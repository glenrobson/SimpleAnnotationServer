package uk.org.llgc.annotation.store.data;
 
import java.util.Map;
import java.util.HashMap;

public class AnnoListNav {
    protected int _results = -1;
    protected String _first = "";
    protected String _last = "";

    public AnnoListNav() {
    }
    
    /**
     * Get results.
     *
     * @return results as int.
     */
    public int getResults() {
        return _results;
    }
    
    /**
     * Set results.
     *
     * @param results the value to set.
     */
    public void setResults(final int pResults) {
         _results = pResults;
    }
    
    /**
     * Get first.
     *
     * @return first as String.
     */
    public String getFirst() {
        return _first;
    }
    
    /**
     * Set first.
     *
     * @param first the value to set.
     */
    public void setFirst(final String pFirst) {
         _first = pFirst;
    }
    
    /**
     * Get last.
     *
     * @return last as String.
     */
    public String getLast() {
        return _last;
    }
    
    /**
     * Set last.
     *
     * @param last the value to set.
     */
    public void setLast(final String pLast) {
         _last = pLast;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> tJson = new HashMap<String, Object>();
        tJson.put("@type", "sc:Layer");
        if (_results != -1) {
            tJson.put("total", _results);
        }
        if (_first != null && !_first.isEmpty()) {
            tJson.put("first", _first);
        }

        if (_last != null && !_last.isEmpty()) {
            tJson.put("last", _last);
        }
        return tJson;
    }
}
