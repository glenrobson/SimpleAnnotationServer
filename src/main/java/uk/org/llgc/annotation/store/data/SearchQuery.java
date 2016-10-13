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

public class SearchQuery {
	protected String _query = "";
	protected List<String> _motivations = null;
	protected List<DateRange> _dates = null;
	protected List<String> _users = null;
	protected int _resultsPerPage = 1000;
	protected int _page = 0;
	protected String _scope = "";
	protected String _baseURI = "";

	public SearchQuery(final String pQuery) {
		this.setQuery(pQuery);
	}

	public SearchQuery(final URI pURI) throws ParseException {
		this.setBaseURI(pURI);

		List<NameValuePair> tParamsList = URLEncodedUtils.parse(pURI, "UTF-8");
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

	protected String convertListToString(final String pKey, final List pList) {
		if (pList != null && !pList.isEmpty()) {
			StringBuffer tBuff = new StringBuffer("&");
			tBuff.append(pKey);
			tBuff.append("=");
			for (Object tChild: pList) {
				tBuff.append(tChild);
				tBuff.append(" ");
			}
			return tBuff.toString().trim();
		} else {
			return "";
		}
	}

	public URI toURI() throws URISyntaxException {
		return new URI(_baseURI + "?" + this.toQueryString());
	}

	public String toQueryString() {
		StringBuffer tBuff = new StringBuffer("q=");
		tBuff.append(URLEncoder.encode(_query));

		if (_motivations != null) {
			tBuff.append("&");
			tBuff.append(URLEncoder.encode(this.convertListToString("motivation", _motivations)));
		}
		if (_dates != null) {
			tBuff.append("&");
			tBuff.append(this.convertListToString("date", _dates));
		}	
		if (_users != null) {
			tBuff.append("&");
			tBuff.append(URLEncoder.encode(this.convertListToString("user", _users)));
		}	
		if (_page != 0) {
			tBuff.append("&");
			tBuff.append("page=" + _page);
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

	public void setUsers(final String pUsers) {
		StringTokenizer tTokenizer = new StringTokenizer(pUsers);
		_users = new ArrayList<String>();
		while (tTokenizer.hasMoreTokens()) {
			_users.add(tTokenizer.nextToken());
		}
	}

	public List<String> getUsers() {
		return _users;
	}
}
