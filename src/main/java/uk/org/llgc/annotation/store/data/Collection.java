package uk.org.llgc.annotation.store.data;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.IOException;

import java.net.URL;
import java.net.URISyntaxException;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.AnnotationUtils;
       
public class Collection implements Comparable {
	protected String _id = "";
	protected String _shortId = "";
	protected String _label = "";
    protected Map<String, Object> _json = null;
    protected User _user = null;
    protected List<Manifest> _manifests = new ArrayList<Manifest>();

    public Collection() {
        _json = new HashMap<String,Object>();
    }

	public Collection(final String pId, final User pUser, final String pLabel) {
        _json = new HashMap<String,Object>();
        this.setId(pId);
        this.setUser(pUser);
        this.setLabel(pLabel);
	}

    public Collection(final Map<String,Object> pJson) throws IOException {
        this.setJson(pJson);
    }

    public Map<String, Object> toJson() throws IOException {
        _json.remove("manifests");  // this will just return null if its not present
        _json.remove("members");
        if (!_manifests.isEmpty()) {
            List<Map<String,String>> tMembers = new ArrayList<Map<String, String>>();
            _json.put("members", tMembers);
            _json.put("manifests", tMembers);
            for (Manifest tManifest : _manifests) {
                Map<String,String> tManJson = new HashMap<String,String>();
                tManJson.put("@id", tManifest.getURI());
                tManJson.put("@type", "sc:Manifest");
                tManJson.put("label", tManifest.getLabel());

                tMembers.add(tManJson);
            }
        }
        _json.put("@id", this.getId());
        _json.put("@type", "sc:Collection");
        _json.put("dcterms:creator", _user.getId());
        _json.put("label", _label);
        _json.put("dc:identifier", this.getShortId());

        _json.put("@context", JsonUtils.fromString(new StringBuilder().append("[")
                        .append("{")
                            .append("\"dcterms\" : \"http://purl.org/dc/terms/\",")
                            .append("\"dcterms:creator\" : {")
                                .append("\"@type\" : \"@id\",")
                                .append("\"@id\" : \"dcterms:creator\"")
                            .append("}")
                        .append(" },")
                        .append("\"http://iiif.io/api/presentation/2/context.json\" ]").toString()));
        return _json;
    }

    public void setJson(final Map<String, Object> pJson) throws IOException {
        _json = pJson;
        this.setId((String)_json.get("@id"));
        this.setLabel((String)_json.get("label")); // will fail if there is a multilingual string
        this.setShortId((String)_json.get("dc:identifier")); 
        String tUserId = (String)_json.get("dcterms:creator");
        User tUser = new User();
        try {
            tUser.setId(tUserId);
        } catch (URISyntaxException tExcpt) {
            throw new IOException("Failed to add user because Id wasn't a URI" + tExcpt);
        }
        this.setUser(tUser); 

        _manifests = new ArrayList<Manifest>();
        if (_json.get("manifests") != null && _json.get("manifests") instanceof List) {
            for (Map<String, Object> tManifest: (List<Map<String, Object>>)_json.get("manifests")) {
                this.addManifest(tManifest);
            }
        }
        if (_json.get("members") != null && _json.get("members") instanceof List) {
            for (Map<String, Object> tManifest: (List<Map<String, Object>>)_json.get("members")) {
                if (tManifest.get("@type").equals("sc:Manifest")) {
                    this.addManifest(tManifest);
                }
            }
        }
    }

    protected void addManifest(final Map<String,Object> pManifestJson) throws IOException {
        try {
            Manifest tManifest = new Manifest(pManifestJson);
            if (!_manifests.contains(tManifest)) {
                _manifests.add(tManifest);
            }
        } catch (IOException tExcpt) {
            System.err.println("Failed to add Manifest due to " + tExcpt);
            System.err.println(JsonUtils.toPrettyString(pManifestJson));
        }
    }

    public String getType() {
        return "sc:Collection";
    }

    public Manifest getManifest(final String pId) {
        for (Manifest tManifest : _manifests) {
            if (tManifest.getURI().equals(pId)) {
                return tManifest;
            }
        }
        return null;
    }

    public boolean remove(final Manifest pManifest) {
        return _manifests.remove(pManifest);
    }

    public void add(final Manifest pManifest) {
        _manifests.add(pManifest);
    }

    public boolean contains(final Manifest pManifest) {
        return this.getManifest(pManifest.getURI()) != null; 
    }

    public String createDefaultId(final String pBaseURL) {
        StringBuffer tIdentifier = new StringBuffer(pBaseURL);
        if (!pBaseURL.endsWith("/")) {
            tIdentifier.append("/");
        }
        tIdentifier.append("collection/");
        tIdentifier.append(_user.getShortId());
        tIdentifier.append("/inbox.json");
        _id = tIdentifier.toString();
        return _id;
    }

    public String createId(final String pBaseURL) {
        _id = pBaseURL + _user.getShortId() + "/" + new Date().getTime() + ".json";
        return _id;
    }
	/**
	 * Get URI.
	 *
	 * @return URI as String.
	 */
	public String getId() {
	    return _id;
	}
	
	/**
	 * Set URI.
	 *
	 * @param URI the value to set.
	 */
	public void setId(final String pId) {
	     _id = pId;
	}
	
	/**
	 * Get shortId.
	 *
	 * @return shortId as String.
	 */
	public String getShortId() {
        if (_shortId == null || _shortId.isEmpty()) {
            try {
                _shortId = AnnotationUtils.getHash(_id, "md5");
            } catch (IOException tExcpt) {
                tExcpt.printStackTrace();
            }
        }
	    return _shortId;
	}
	
	/**
	 * Set shortId.
	 *
	 * @param shortId the value to set.
	 */
	public void setShortId(final String pShortId) {
	     _shortId = pShortId;
	}
	
	/**
	 * Get label.
	 *
	 * @return label as String.
	 */
	public String getLabel() {
	    return _label;
	}

    public User getUser() {
        return _user;
    }

    public void setUser(final User pUser) {
        _user = pUser;
    }
	
	/**
	 * Set label.
	 *
	 * @param label the value to set.
	 */
	public void setLabel(final String pLabel) {
	     _label = pLabel;
	}

    
    public List<Manifest> getManifests() {
        return _manifests;
    }

    public boolean equals(Object pOther) {
        if (pOther instanceof Collection) {
            return _id.equals(((Collection)pOther).getId());
        } else {
            return false;
        }
    }

    public int compareTo(final Object pOther) {
        Collection tOther = (Collection)pOther;
        if (_id.equals(tOther.getId())) {
            return 0; // objects are equal
        }
        if (_id.endsWith("inbox.json")) {
            return -1;
        }
        if (tOther.getId().endsWith("inbox.json")) {
            return 1;
        }
        long tTime1 = this.getTimestamp(_id);
        long tTime2 = this.getTimestamp(tOther.getId());

        return (int)(tTime1 - tTime2);
    }

    protected long getTimestamp(final String pID) {
        return Long.parseLong(pID.substring(pID.lastIndexOf("/")+ 1).split("\\.")[0]);
    }

    public String toString() {
        return "Id: " + _id + "\nShortId: " + _shortId + "\nLabel: " + _label + "\nManifests: " + _manifests.size();
    }
}
