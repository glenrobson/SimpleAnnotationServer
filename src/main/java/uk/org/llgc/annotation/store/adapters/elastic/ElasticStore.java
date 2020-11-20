package uk.org.llgc.annotation.store.adapters.elastic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;
import java.net.URI;

import com.github.jsonldjava.utils.JsonUtils;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.IIIFSearchResults;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.Collection;
import uk.org.llgc.annotation.store.data.AnnoListNav;
import uk.org.llgc.annotation.store.data.Body;
import uk.org.llgc.annotation.store.data.Target;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.adapters.AbstractStoreAdapter;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentFactory;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;

import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import org.apache.http.HttpRequestInterceptor;

import java.text.ParseException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Base64;
import java.util.Date;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;

import org.apache.http.HttpHost;

import com.github.jsonldjava.utils.JsonUtils;

public class ElasticStore extends AbstractStoreAdapter implements StoreAdapter {
	protected static Logger _logger = LogManager.getLogger(ElasticStore.class.getName());

	protected RestHighLevelClient _client = null;
    protected String _index = "";
    protected RefreshPolicy _policy = RefreshPolicy.NONE;

    // http://host:port
	public ElasticStore(final String pConnectionURL) throws URISyntaxException, IOException {
        URI tConectionString = new URI(pConnectionURL);
        _client = buildClient(tConectionString);

        _index = tConectionString.getPath().replace("/","");
        if (_index == null || _index.trim().isEmpty()) {
            throw new IOException("No index specified in connection string " + pConnectionURL);
        }
        createIndex();
	}

    public void setRefreshPolicy(final RefreshPolicy pPolicy) {
        _policy = pPolicy;
    }
    public RefreshPolicy getRefreshPolicy() {
        return _policy;
    }

    // Used in unit test to manage test
    public static RestHighLevelClient buildClient(final URI pConnectionURL) {
        //final CredentialsProvider credentialsProvider =new BasicCredentialsProvider();credentialsProvider.setCredentials(AuthScope.ANY,new UsernamePasswordCredentials("username", "password"));
        if (System.getenv("AWS_REGION") != null && System.getenv("AWS_ACCESS_KEY_ID") != null && System.getenv("AWS_SECRET_ACCESS_KEY") != null) {
            String tServiceName = "es";
            AWSCredentialsProvider credentialsProvider = new EnvironmentVariableCredentialsProvider();
            AWS4Signer signer = new AWS4Signer();
            signer.setServiceName(tServiceName);
            signer.setRegionName(System.getenv("AWS_REGION")); // ENV
            HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(tServiceName, signer, credentialsProvider);
            return new RestHighLevelClient(RestClient.builder(new HttpHost(pConnectionURL.getHost(), pConnectionURL.getPort(), pConnectionURL.getScheme())).setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor)));
        } else {
            RestClientBuilder builder = RestClient.builder(new HttpHost(pConnectionURL.getHost(), pConnectionURL.getPort(), pConnectionURL.getScheme()));
         
            RestHighLevelClient tClient = new RestHighLevelClient(builder);

            return tClient ;
        }
    }

    protected void createIndex() throws IOException {
        if (!_client.indices().exists(new GetIndexRequest(_index), RequestOptions.DEFAULT)) {
            // Index doesn't exist so create it with mapping
            CreateIndexRequest tRequest = new CreateIndexRequest(_index);

            XContentBuilder tMapping = XContentFactory.jsonBuilder()
                .startObject()
                    .startObject("properties")
                        .startObject("id")
                            .field("type", "keyword")
                        .endObject()
                        .startObject("type")
                            .field("type", "keyword")
                        .endObject()
                        .startObject("creator")
                            .field("type", "keyword")
                        .endObject()
                        .startObject("created")
                            .field("type", "date")
                        .endObject()
                        .startObject("modified")
                            .field("type", "date")
                        .endObject()
                        .startObject("motivation")
                            .field("type", "keyword")
                        .endObject()
                        .startObject("body")
                            .field("type", "text")
                        .endObject()
                        .startObject("target")
                            .startObject("properties")
                                .startObject("id")
                                    .field("type", "keyword")
                                .endObject()
                                .startObject("type")
                                    .field("type", "keyword")
                                .endObject()
                                .startObject("short_id")
                                    .field("type", "keyword")
                                .endObject()
                                .startObject("within")
                                    .startObject("properties")
                                        .startObject("id")
                                            .field("type", "keyword")
                                        .endObject()
                                        .startObject("type")
                                            .field("type", "keyword")
                                        .endObject()
                                        .startObject("label")
                                            .field("type", "text")
                                        .endObject()
                                    .endObject()
                                .endObject()
                            .endObject()
                        .endObject()
                        // Manifest
                        .startObject("json")
                            .field("type", "object")
                            .field("enabled", "false")
                        .endObject()
                        .startObject("short_id")
                            .field("type", "keyword")
                        .endObject()
                        .startObject("label")
                            .field("type", "text")
                        .endObject()
                        .startObject("canvases")
                            .field("type", "object")
                            .startObject("properties")
                                .startObject("id")
                                    .field("type", "keyword")
                                .endObject()
                                .startObject("type")
                                    .field("type", "keyword")
                                .endObject()
                                .startObject("short_id")
                                    .field("type", "keyword")
                                .endObject()
                                .startObject("label")
                                    .field("type", "text")
                                .endObject()
                            .endObject()
                        .endObject()
                        // User
                        .startObject("name")
                            .field("type", "text")
                        .endObject()
                        .startObject("email")
                            .field("type", "text")
                        .endObject()
                        .startObject("picture")
                            .field("type", "keyword")
                        .endObject()
                        .startObject("group")
                            .field("type", "keyword")
                        .endObject()
                        .startObject("authenticationMethod")
                            .field("type", "keyword")
                        .endObject()
                        .startObject("members")
                            .field("type", "object")
                            .startObject("properties")
                                .startObject("id")
                                    .field("type", "keyword")
                                .endObject()
                                .startObject("type")
                                    .field("type", "keyword")
                                .endObject()
                                .startObject("label")
                                    .field("type", "text")
                                .endObject()
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
            tRequest.mapping(tMapping);

            _client.indices().create(tRequest, RequestOptions.DEFAULT);

        }
    }

// id, motivation, body, target, selector, within, data, short_id, label
	public Annotation addAnnotationSafe(final Annotation pAnno) throws IOException {
        IndexRequest tIndex = new IndexRequest(_index);
        tIndex.id(pAnno.getId());

        tIndex.source(anno2json(pAnno));
        tIndex.setRefreshPolicy(_policy);
	
        _client.index(tIndex, RequestOptions.DEFAULT);

        for (Target tTarget : pAnno.getTargets()) {
            this.storeCanvas(tTarget.getCanvas());
        }
        return pAnno;
    }

    protected Map<String, Object> anno2json(final Annotation pAnno) {
        Map<String, Object> tJson = new HashMap<String,Object>();
		tJson.put("type", pAnno.getType());
		tJson.put("created", pAnno.getCreated());
		tJson.put("modified", pAnno.getModified());
		tJson.put("motivation", pAnno.getMotivations());
        if (pAnno.getCreator() != null && !pAnno.getCreator().isAdmin()) {
            tJson.put("creator", pAnno.getCreator().getId());
        }

        List<String> tBodies = new ArrayList<String>();
        for (Body tBody : pAnno.getBodies()) {
            tBodies.add(tBody.getIndexableContent());
        }
        tJson.put("body", tBodies);

        
        List<Map<String,Object>> tCanvases = new ArrayList<Map<String,Object>>();
        for (Target tTarget : pAnno.getTargets()) {
            Map<String, Object> tCanvasJson = new HashMap<String, Object>();
            Canvas tCanvas = tTarget.getCanvas();
            tCanvasJson.put("id", tCanvas.getId());
            tCanvasJson.put("type", "sc:Canvas");
            tCanvasJson.put("short_id", tCanvas.getShortId());
            if (tTarget.getManifest() != null) {
                Map<String, Object> tManifest = new HashMap<String, Object>();
                tManifest.put("id", tTarget.getManifest().getURI());
                tManifest.put("type", "sc:Manifest");
                if (tTarget.getManifest().getLabel() != null && !tTarget.getManifest().getLabel().isEmpty()) {
                    tManifest.put("label", tTarget.getManifest().getLabel());
                }

                tCanvasJson.put("within", tManifest);
            }
            tCanvases.add(tCanvasJson);
        }
        tJson.put("target", tCanvases);
		tJson.put("json", pAnno.toJson());

        return tJson;
    }

	public void deleteAnnotation(final String pAnnoId) throws IOException {
        DeleteRequest tDelete = new DeleteRequest(_index);
        tDelete.id(pAnnoId);

        tDelete.setRefreshPolicy(_policy);
        _client.delete(tDelete, RequestOptions.DEFAULT);
	}

	public AnnotationList getAnnotationsFromPage(final User pUser, final Canvas pPage) throws IOException {
        BoolQueryBuilder tBuilder = QueryBuilders.boolQuery();
        tBuilder.must(QueryBuilders.termQuery("target.id", pPage.getId()));

        if (!pUser.isAdmin()) {
            tBuilder.must(QueryBuilders.termQuery("creator", pUser.getId()));
        }

        AnnotationList tList = new AnnotationList();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(10000);
        searchSourceBuilder.query(tBuilder);
        SearchRequest searchRequest = new SearchRequest(_index);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = _client.search(searchRequest, RequestOptions.DEFAULT);
        for (SearchHit hit : searchResponse.getHits()) {
            tList.add(new Annotation((Map<String,Object>)hit.getSourceAsMap().get("json")));
        }
        return tList;
    }

	public Annotation getAnnotation(final String pId) throws IOException {
        GetRequest tRequest = new GetRequest(_index, pId);
        GetResponse tResponse = _client.get(tRequest, RequestOptions.DEFAULT);
        if (tResponse != null && tResponse.getSource() != null) {
            return new Annotation((Map<String,Object>)tResponse.getSourceAsMap().get("json"));
        } else {
            return null;
        }
	}


	public AnnotationList getAllAnnotations() throws IOException {
        AnnotationList tList = new AnnotationList();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("type", "oa:Annotation"));
        searchSourceBuilder.size(10000);
        SearchRequest searchRequest = new SearchRequest(_index);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = _client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            tList.add(new Annotation((Map<String,Object>)hit.getSourceAsMap().get("json")));
        }
        return tList;
	}

    public void linkupOrphanCanvas(final Manifest pManifest) throws IOException {
        for (Canvas tCanvas : pManifest.getCanvases()) {
            BoolQueryBuilder tBuilder = QueryBuilders.boolQuery();
            tBuilder.mustNot(QueryBuilders.existsQuery("target.within.id"));
            tBuilder.must(QueryBuilders.termQuery("target.id", tCanvas.getId()));

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(tBuilder);
            searchSourceBuilder.size(10000);
            SearchRequest searchRequest = new SearchRequest(_index);
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = _client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            boolean tMadeChange = false;
            for (SearchHit hit : searchHits) {
                Annotation tAnno = new Annotation((Map<String,Object>)hit.getSourceAsMap().get("json"));
                for (Target tTarget : tAnno.getTargets()) {
                    if (tTarget.getCanvas().getId().equals(tCanvas.getId())) {
                        tTarget.setManifest(pManifest);
                        tMadeChange = true;
                    }
                }
                if (tMadeChange) {
                    try {
                        this.updateAnnotation(tAnno);
                    } catch (MalformedAnnotation tExcpt) {
                        throw new IOException("Failed to reload annotation after updating the within: " + tExcpt);
                    }
                }
            }
        }
    }

	protected String indexManifestNoCheck(final String pShortId, final Manifest pManifest) throws IOException {
        pManifest.setShortId(pShortId);
        IndexRequest tIndex = new IndexRequest(_index);
        tIndex.id(pShortId);
        tIndex.source(manifest2Json(pManifest));
	
        tIndex.setRefreshPolicy(_policy);
        _client.index(tIndex, RequestOptions.DEFAULT);

        this.linkupOrphanCanvas(pManifest);
        for (Canvas tCanvas : pManifest.getCanvases()) {
            this.storeCanvas(tCanvas);
        }
        return pShortId;
    }

	public String getManifestId(final String pShortId) throws IOException {
        GetRequest tRequest = new GetRequest(_index, pShortId);
        GetResponse tResponse = _client.get(tRequest, RequestOptions.DEFAULT);

        if (tResponse != null && tResponse.isExists()) {
            Manifest tManifest = json2Manifest(tResponse.getSourceAsMap());
            return tManifest.getURI();
        } else {
            return null;
        }
	}

	public Manifest getManifest(final String pId) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("id", pId));
        searchSourceBuilder.size(1);
        SearchRequest searchRequest = new SearchRequest(_index);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = _client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        if (searchHits != null && searchHits.length > 0) {
            Manifest tManifest = null;
            for (SearchHit hit : searchHits) {
                tManifest = json2Manifest(hit.getSourceAsMap());
            }
            return tManifest;
        } else {
            return null;
        }
	}

	public Manifest getManifestForCanvas(final Canvas pCanvas) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("canvases.id", pCanvas.getId()));
        SearchRequest searchRequest = new SearchRequest(_index);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = _client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Manifest tManifest = null;
        for (SearchHit hit : searchHits) {
            tManifest = json2Manifest(hit.getSourceAsMap());
        }
	    return tManifest;
	}

	public List<Manifest> getManifests() throws IOException {
        List<Manifest> tManifests = new ArrayList<Manifest>();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(10000);
        searchSourceBuilder.query(QueryBuilders.termQuery("type", "sc:Manifest"));
        SearchRequest searchRequest = new SearchRequest(_index);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = _client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            tManifests.add(json2Manifest(hit.getSourceAsMap()));
        }
        return tManifests;

    }

    // TODO note this will return indexed manifests as well as non indexed..
	public List<Manifest> getSkeletonManifests(final User pUser) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.aggregation(AggregationBuilders.terms("manifests").field("target.within.id").size(10000));
        if (!pUser.isAdmin()) {
            searchSourceBuilder.query(QueryBuilders.termQuery("creator", pUser.getId()));
        }
        searchSourceBuilder.size(0);
        SearchRequest searchRequest = new SearchRequest(_index);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = _client.search(searchRequest, RequestOptions.DEFAULT);
        Terms tFacets = searchResponse.getAggregations().get("manifests");
        List<Manifest> tManifests = new ArrayList<Manifest>();
        for (Bucket tFacet : tFacets.getBuckets()) {
            Manifest tManifest = new Manifest();
            tManifest.setURI(tFacet.getKeyAsString());
            tManifests.add(tManifest);
        }

        return tManifests;
    }

    public void storeCanvas(final Canvas pCanvas) throws IOException {
        IndexRequest tIndex = new IndexRequest(_index);
        tIndex.id(pCanvas.getShortId());
        Map<String, Object> tJson = pCanvas.toJson();
        tJson.put("short_id", tJson.remove("http://purl.org/dc/terms/identifier"));
        tIndex.source(tJson);
	
        tIndex.setRefreshPolicy(_policy);
        _client.index(tIndex, RequestOptions.DEFAULT);
    }


    public Canvas resolveCanvas(final String pShortId) throws IOException {
        GetRequest tRequest = new GetRequest(_index, pShortId);
        GetResponse tResponse = _client.get(tRequest, RequestOptions.DEFAULT);

        if (tResponse != null && tResponse.isExists()) {
            Map<String, Object> tJson = tResponse.getSourceAsMap();
            tJson.put("http://purl.org/dc/terms/identifier", tJson.get("short_id"));
            return new Canvas(tJson);
        } else {
            return null;
        }
    }

    protected Map<String, Object> manifest2Json(final Manifest pManifest) {
        Map<String, Object> tJson = new HashMap<String,Object>();
        tJson.put("id", pManifest.getURI());
        tJson.put("type", pManifest.getType());
        tJson.put("label", pManifest.getLabel());
        tJson.put("short_id", pManifest.getShortId());

        List<Map<String,Object>> tCanvases = new ArrayList<Map<String, Object>>();
        for (Canvas tCanvas : pManifest.getCanvases()) {
            Map<String, Object> tCanvasJson = new HashMap<String, Object>();
            tCanvasJson.put("id", tCanvas.getId());
            tCanvasJson.put("type", "sc:Canvas");
            tCanvasJson.put("label", tCanvas.getLabel());
            tCanvases.add(tCanvasJson);
        }
        tJson.put("canvases", tCanvases);
        tJson.put("json", pManifest.toJson());
        return tJson;
    }

    protected Manifest json2Manifest(final Map<String, Object> pJson) throws IOException {
        return new Manifest((Map<String,Object>)pJson.get("json"), (String)pJson.get("short_id"));
    }

	public IIIFSearchResults search(final SearchQuery pQuery) throws IOException {
        BoolQueryBuilder tBuilder = QueryBuilders.boolQuery();
		if (pQuery.getMotivations() != null && !pQuery.getMotivations().isEmpty()) {
            tBuilder.must(QueryBuilders.termsQuery("motivation", pQuery.getMotivations()));
        }    
		if (pQuery.getUsers() != null && !pQuery.getUsers().isEmpty()) {
            List<String> tUserIds = new ArrayList<String>();
            boolean tFoundAdmin = false;
            for (User tUser : pQuery.getUsers()) {
                tUserIds.add(tUser.getId());
                if (tUser.isAdmin()) {
                    tFoundAdmin = true;
                }
            }
            if (!tFoundAdmin) {
                tBuilder.must(QueryBuilders.termsQuery("creator", tUserIds));
                // if we found an admin then they can access all results
            }
        }    
        tBuilder.must(QueryBuilders.termQuery("target.within.id", pQuery.getScope()));
        if (pQuery.getQuery() != null && !pQuery.getQuery().isEmpty()) {
            tBuilder.must(QueryBuilders.matchQuery("body", pQuery.getQuery()).operator(Operator.AND));
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.from(pQuery.getPage() * pQuery.getResultsPerPage());
        searchSourceBuilder.size(pQuery.getResultsPerPage());
        searchSourceBuilder.highlighter(new HighlightBuilder().field("body"));

        searchSourceBuilder.query(tBuilder);
        SearchRequest searchRequest = new SearchRequest(_index);
        searchRequest.source(searchSourceBuilder);
        SearchResponse tResponse = _client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = tResponse.getHits();
            
        try {
            IIIFSearchResults tAnnoList = new IIIFSearchResults();
            tAnnoList.setId(pQuery.toURI().toString());
            long tResultNo = hits.getTotalHits().value;
            int tNumberOfPages = (int)(tResultNo / pQuery.getResultsPerPage());

            for (SearchHit hit : hits) {
                Annotation tAnno = new Annotation((Map<String,Object>)hit.getSourceAsMap().get("json"));
                HighlightField tField = hit.getHighlightFields().get("body");
                if (tField != null) {
                    StringBuffer tLabel = new StringBuffer(); 
                    for (Text tText : tField.getFragments()) {
                        tLabel.append(tText.string() + " ");
                    }
                    tAnno.setLabel(tLabel.toString());
                } else {
                    StringBuffer tLabel = new StringBuffer(); 
                    for (Body tBody : tAnno.getBodies()) {
                        tLabel.append(tBody.getIndexableContent() + " ");
                    }
                    tAnno.setLabel(tLabel.toString());

                }
                tAnnoList.add(tAnno);
            }

            // Adding page count even if results are smaller than one page.
            AnnoListNav tWithin = new AnnoListNav();
            tAnnoList.setNav(tWithin);
            tWithin.setResults((int)tResultNo);
            if (tResultNo > pQuery.getResultsPerPage()) { // if paginating
                int tPageNo = pQuery.getPage();
                tAnnoList.setStartIndex(tPageNo);
                if (tNumberOfPages != pQuery.getPage()) { // not on last page
                    int tPage = tPageNo + 1;
                    pQuery.setPage(tPage);
                    tAnnoList.setNext(pQuery.toURI().toString());
                }
                pQuery.setPage(0);
                tWithin.setFirst(pQuery.toURI().toString());
                pQuery.setPage(tNumberOfPages);
                tWithin.setLast(pQuery.toURI().toString());
            } else {
                tAnnoList.setStartIndex(0);
            }
            return tAnnoList;
        } catch (URISyntaxException tExcpt) {
            throw new IOException("Unable to create manifest or query URI due to " + tExcpt);
        }
	}

	public List<PageAnnoCount> listAnnoPages(final Manifest pManifest) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("target.within.id", pManifest.getURI()));
        searchSourceBuilder.aggregation(AggregationBuilders.terms("pages").field("target.id").size(10000));
        searchSourceBuilder.size(0);
        SearchRequest searchRequest = new SearchRequest(_index);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = _client.search(searchRequest, RequestOptions.DEFAULT);
        Terms tFacets = searchResponse.getAggregations().get("pages");
        List<PageAnnoCount> tAnnoPageCount = new ArrayList<PageAnnoCount>();
        for (Bucket tFacet : tFacets.getBuckets()) {
            String tLabel = "";
            if (pManifest.getCanvas(tFacet.getKeyAsString()) != null) {
                tLabel = pManifest.getCanvas(tFacet.getKeyAsString()).getLabel();
            }
            Canvas tCanvas = new Canvas(tFacet.getKeyAsString(), tLabel);
            this.storeCanvas(tCanvas);
            tAnnoPageCount.add(new PageAnnoCount(tCanvas, (int)tFacet.getDocCount(), pManifest));
        }

        return tAnnoPageCount;
    }    

    public User getUser(final User pUser) throws IOException {
        GetRequest tRequest = new GetRequest(_index, pUser.getId());
        GetResponse tResponse = _client.get(tRequest, RequestOptions.DEFAULT);

        if (tResponse != null && tResponse.getSource() != null) {
            User tSavedUser = new User();
            tSavedUser.setToken(pUser.getToken());

            Map<String, Object> tJson = (Map<String,Object>)tResponse.getSourceAsMap();
            try {
                tSavedUser.setId((String)tJson.get("id"));
            } catch (URISyntaxException tExcpt) {
                throw new IOException("Unable to create user as ID was not a URI: " + tExcpt);
            }
            tSavedUser.setShortId((String)tJson.get("short_id"));
            tSavedUser.setName((String)tJson.get("name"));
            tSavedUser.setEmail((String)tJson.get("email"));
            if (tJson.get("created") != null) {
                tSavedUser.setCreated(super.parseDate((String)tJson.get("created")));
            }
            tSavedUser.setLastModified(super.parseDate((String)tJson.get("modified")));
            if (tJson.get("created") == null && tJson.get("modified") != null) {
                tSavedUser.setCreated(tSavedUser.getLastModified());
            }
            if (tJson.get("picture") != null) {
                tSavedUser.setPicture((String)tJson.get("picture"));
            }
            if (tJson.get("group") != null && tJson.get("group").toString().equals("admin")) {
                tSavedUser.setAdmin(true);
            }
            tSavedUser.setAuthenticationMethod((String)tJson.get("authenticationMethod"));
             
            return tSavedUser;         
        } else {
            return null;
        }
    }
    public User saveUser(final User pUser) throws IOException {
        User tSavedUser = getUser(pUser);
        if (tSavedUser != null) {
            // This is an update
            pUser.setCreated(tSavedUser.getCreated());
            pUser.updateLastModified();
        }
        IndexRequest tIndex = new IndexRequest(_index);
        tIndex.id(pUser.getId());
        Map<String, Object> tJson = this.user2json(pUser);
        tIndex.source(tJson);
	
        tIndex.setRefreshPolicy(_policy);
        _client.index(tIndex, RequestOptions.DEFAULT);

        return pUser;
    }

    protected Map<String, Object> user2json(final User pUser) {
        Map<String,Object> tJson = new HashMap<String,Object>();
        tJson.put("id", pUser.getId());
        tJson.put("type", "User");
        tJson.put("short_id", pUser.getShortId());
        tJson.put("name", pUser.getName());
        tJson.put("email", pUser.getEmail());
        // Elastic search could handle this but better to be explicit on the format
        tJson.put("created", super.formatDate(pUser.getCreated())); 
        tJson.put("modified", super.formatDate(pUser.getLastModified()));
        if (pUser.getPicture() != null && !pUser.getPicture().isEmpty()) {
            tJson.put("picture", pUser.getPicture());
        }
        if (pUser.isAdmin()) {
            tJson.put("group", "admin");
        }
        tJson.put("authenticationMethod", pUser.getAuthenticationMethod());

        return tJson;
    }

    protected Map<String, Object> object2json(final Collection pCollection) {
        Map<String,Object> tJson = new HashMap<String,Object>();
        tJson.put("id", pCollection.getId());
        tJson.put("type", "Collection");
        tJson.put("short_id", pCollection.getShortId());
        tJson.put("label", pCollection.getLabel());
        tJson.put("creator", pCollection.getUser().getId());

        List<Map<String,Object>> tMembers = new ArrayList<Map<String, Object>>();
        for (Manifest tManifest : pCollection.getManifests()) {
            Map<String, Object> tManifestJson = new HashMap<String, Object>();
            tManifestJson.put("id", tManifest.getURI());
            tManifestJson.put("type", "Manifest");
            tManifestJson.put("label", tManifest.getLabel());

            tMembers.add(tManifestJson);
        }
        tJson.put("members", tMembers);
        return tJson;
    }

    protected Collection collectionFromMap(final Map<String, Object> pJson) throws IOException {
        Collection tCollection = new Collection();

        tCollection.setId((String)pJson.get("id"));
        tCollection.setShortId((String)pJson.get("short_id"));
        tCollection.setLabel((String)pJson.get("label"));

        User tUser = new User();
        try {
            tUser.setId((String)pJson.get("creator"));
        } catch (URISyntaxException tExcpt) {
            throw new IOException("Unable to create user as the ID is not a valid URI: " + pJson.get("creator"));
        }
        tCollection.setUser(tUser);
        List<Map<String,Object>> tManifests = (List<Map<String,Object>>)pJson.get("members");
        for (Map<String,Object> tManifestJson : tManifests) {
            Manifest tManifest = new Manifest();
            tManifest.setURI((String)tManifestJson.get("id"));
            tManifest.setLabel((String)tManifestJson.get("label"));
            tCollection.add(tManifest);
        }

        return tCollection;
    }

    public Collection createCollection(final Collection pCollection) throws IOException {
        IndexRequest tIndex = new IndexRequest(_index);
        tIndex.id(pCollection.getId());
        Map<String, Object> tJson = this.object2json(pCollection);
        tIndex.source(tJson);
	
        tIndex.setRefreshPolicy(_policy);
        _client.index(tIndex, RequestOptions.DEFAULT);

        return pCollection;
    }

    public List<Collection> getCollections(final User pUser) throws IOException {
        BoolQueryBuilder tBuilder = QueryBuilders.boolQuery();
        tBuilder.must(QueryBuilders.termQuery("type", "Collection"));
        tBuilder.must(QueryBuilders.termQuery("creator", pUser.getId()));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(tBuilder);
        searchSourceBuilder.size(10000);

        List<Collection> tCollections = this.getCollections(searchSourceBuilder);
        if (tCollections == null) {
            tCollections = new ArrayList<Collection>();
        } else {
            // Make sure the fully populated user is added rather than just the ID
            for (Collection tCollection : tCollections) {
                System.out.println("Collection user was " + tCollection.getUser().getId() + " and will not be: " + pUser.getId() + " for collection " + tCollection.getId());
                tCollection.setUser(pUser);
            }
        }
        return tCollections;
    }
        
    public List<Collection> getCollections(final SearchSourceBuilder tQuery) throws IOException { 
        SearchRequest searchRequest = new SearchRequest(_index);
        searchRequest.source(tQuery);
        SearchResponse searchResponse = _client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        List<Collection> tCollections = new ArrayList<Collection>();
        if (searchHits.length == 0) {
            return null; // No user saved
        } else {
            for (int i = 0; i < searchHits.length; i++) {
                tCollections.add(this.collectionFromMap(searchHits[i].getSourceAsMap()));
            }
        }
             
        return tCollections;         
    }

    public Collection getCollection(final String pId) throws IOException {
        GetRequest tRequest = new GetRequest(_index, pId);
        GetResponse tResponse = _client.get(tRequest, RequestOptions.DEFAULT);

        if (tResponse != null && tResponse.getSource() != null && tResponse.getSourceAsMap() != null && tResponse.getSourceAsMap() != null) {
            return this.collectionFromMap((Map<String,Object>)tResponse.getSourceAsMap());
        } else {    
            return null;
        }
    }

    public void deleteCollection(final Collection pCollection) throws IOException {
        DeleteRequest tDelete = new DeleteRequest(_index);
        tDelete.id(pCollection.getId());

        tDelete.setRefreshPolicy(_policy);
        _client.delete(tDelete, RequestOptions.DEFAULT);
    }
}
