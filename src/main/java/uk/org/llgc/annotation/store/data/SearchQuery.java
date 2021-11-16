package uk.org.llgc.annotation.store.data;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Map;

import java.text.ParseException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.NameValuePair;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import uk.org.llgc.annotation.store.data.users.User;

public class SearchQuery {
	protected String _query = "";
	protected List<String> _motivations = null;
	protected List<DateRange> _dates = null;
	protected List<User> _users = null;
	protected int _resultsPerPage = 1000;
	protected int _page = 0;
	protected String _scope = "";
	protected String _baseURI = "";

	public SearchQuery(final String pQuery) {
		this.setQuery(pQuery);
	}

	public SearchQuery(final URI pURI) throws ParseException, URISyntaxException {
		this.setBaseURI(pURI);

		List<NameValuePair> tParamsList = URLEncodedUtils.parse(pURI, Charset.forName("UTF-8"));
		Map<String,String> tParams = new HashMap<String,String>();
		for (NameValuePair tCurrent : tParamsList) {
			tParams.put(tCurrent.getName(), tCurrent.getValue());
		}
		if (tParams.get("q") != null) {
			this.setQuery(tParams.get("q"));
		} 
		if (tParams.get("motivation") != null) {
			this.setQuery(tParams.get("motivation"));
		} 
		if (tParams.get("date") != null) {
			this.setDateRanges(tParams.get("date"));
		} 
		if (tParams.get("user") != null) {
			this.setUsers(tParams.get("user"));
		} 
		if (tParams.get("page") != null) {
			this.setPage(Integer.parseInt(tParams.get("page")));
		} 
	}

	protected String convertListToString(final String pKey, final List pList) throws UnsupportedEncodingException {
		if (pList != null && !pList.isEmpty()) {
			StringBuffer tBuff = new StringBuffer();
			for (Object tChild: pList) {
                if (tChild instanceof User) {
                    tBuff.append(((User)tChild).getId());
                } else {
                    tBuff.append(tChild);
                }
				tBuff.append(" ");
			}
			return "&" + pKey + "=" + URLEncoder.encode(tBuff.toString().trim(), "UTF-8");
		} else {
			return "";
		}
	}

	public URI toURI() throws URISyntaxException {
		return new URI(_baseURI + "?" + this.toQueryString());
	}

	public String toQueryString() {
		StringBuffer tBuff = new StringBuffer("q=");
        try {
            tBuff.append(URLEncoder.encode(_query, "UTF-8"));
            if (_motivations != null) {
                tBuff.append(this.convertListToString("motivation", _motivations));
            }
            if (_dates != null) {
                tBuff.append(this.convertListToString("date", _dates));
            }	
            if (_users != null) {
                tBuff.append(this.convertListToString("user", _users));
            }	
            if (_page != 0) {
                tBuff.append("&page=");
                tBuff.append(_page);
            }	
        } catch (UnsupportedEncodingException tExcpt) {
            // shouldn't happen as UTF-8 should be supported.
            tExcpt.printStackTrace();
        }
		return tBuff.toString();
	}

	public void setBaseURI(final URI pURI) {
		StringBuffer tURI = new StringBuffer(pURI.getScheme());
		tURI.append("://");
		tURI.append(pURI.getAuthority());
		tURI.append(pURI.getPath());
		_baseURI = tURI.toString();
	}

	public void setBaseURI(final String pBaseURI) {
		_baseURI = pBaseURI;
	}

	public String getBaseURI() {
		return _baseURI;
	}

	public void setScope(final String pScope) {
		_scope = pScope;
	}

	public String getScope() {
		return _scope;
	}
		
	public void setQuery(final String pQuery) {
		_query = pQuery;
	}

	public String getQuery() {
		return _query;
	}

	public int getResultsPerPage() {
		return _resultsPerPage;
	}

	public void setResultsPerPage(final int pNumResults) {
		_resultsPerPage = pNumResults;
	}

	public void setPage(final int pPage) {
		_page = pPage;
	}

	public int getIndex() {
		return _page * _resultsPerPage;
	}

	public int getPage() {
		return _page;
	}

	// A space separated list of motivation terms. If multiple motivations are supplied, an annotation matches the search if any of the motivations are present.
	public void setMotivations(final String pMotivations) {
		StringTokenizer tTokenizer = new StringTokenizer(pMotivations);
		_motivations = new ArrayList<String>();
		while (tTokenizer.hasMoreTokens()) {
			_motivations.add(tTokenizer.nextToken());
		}
	}

	public List<String> getMotivations() {
		return _motivations;
	}

	// A space separated list of date ranges. An annotation matches if the date on which it was created falls within any of the supplied date ranges. The dates must be supplied in the ISO8601 format: YYYY-MM-DDThh:mm:ssZ/YYYY-MM-DDThh:mm:ssZ. The dates must be expressed in UTC and must be given in the Z based format.
	public void setDateRanges(final String pDates) throws ParseException {
		StringTokenizer tTokenizer = new StringTokenizer(pDates);
		_dates = new ArrayList<DateRange>();
		while (tTokenizer.hasMoreTokens()) {
			_dates.add(new DateRange(tTokenizer.nextToken()));
		}
	}

	public List<DateRange> getDateRanges() {
		return _dates;
	}

	public void setUsers(final String pUsers) throws URISyntaxException {
		StringTokenizer tTokenizer = new StringTokenizer(pUsers);
		_users = new ArrayList<User>();
		while (tTokenizer.hasMoreTokens()) {
            User tUser = new User();
            tUser.setId(tTokenizer.nextToken());
			_users.add(tUser);
		}
	}
    public void addUser(final User pUser) {
        if (_users == null) {
            _users = new ArrayList<User>();
        }
        boolean tFoundUser = false;
        for (User tSearchUser : _users) {
            if(tSearchUser.getId().equals(pUser.getId())) {
                tFoundUser = true;
                break;
            }
        }
        if (!tFoundUser) {
            _users.add(pUser);
        }
    }
        
	public List<User> getUsers() {
		return _users;
	}
}
