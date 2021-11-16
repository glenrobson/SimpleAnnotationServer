package uk.org.llgc.annotation.store.controllers;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.annotation.PostConstruct;

import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.PieChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.LegendPlacement;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.stats.TopLevel;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.AnnotationUtils;

@RequestScoped
@ManagedBean
public class StatsService {
    protected StoreAdapter _store = null;
	protected File _cacheDir = null;
    protected Map<String,Manifest> _manifests = new HashMap<String,Manifest>();
    protected Map<String,List<PageAnnoCount>> _manifestAnnoCount = new HashMap<String,List<PageAnnoCount>>();

    @PostConstruct
    public void init() {
        _store = StoreConfig.getConfig().getStore();
        _store.init(new AnnotationUtils());
    }

    public void init(final AnnotationUtils pUtils) {
        _store = StoreConfig.getConfig().getStore();
        _store.init(pUtils);
    }


    public Manifest getManifest(final String pId) throws IOException {
        if (_manifests.containsKey(pId)) {
            return _manifests.get(pId);
        } else {
            Manifest tManifest = _store.getManifest(pId);

            _manifests.put(pId, tManifest);
            return tManifest;
        }
    }

    public List<PageAnnoCount> getAnnoCountData(final Manifest pManifest, final User pUser) throws IOException {
        String tKey = "anno-count-" + pManifest.getShortId();
        if (pUser != null) {
            tKey += "-" + pUser.getShortId();
        }
        if (_manifestAnnoCount.containsKey(tKey)) {
            return _manifestAnnoCount.get(tKey);
        } else {
            List<PageAnnoCount> tPageCounts = _store.listAnnoPages(pManifest, pUser);
            _manifestAnnoCount.put(tKey, tPageCounts);
            return tPageCounts;
        }
    }

    public PieChartModel getPercentAnnotated(final String pId) throws IOException {
        Manifest tManifest = this.getManifest(pId);
        return getPercentAnnotated(tManifest);
    }

    public PieChartModel getPercentAnnotated(final Manifest pManifest) {
        UserService tUserService = new UserService();
        return this.getPercentAnnotated(pManifest, tUserService.getUser());
    }

    public PieChartModel getPercentAnnotated(final Manifest pManifest, final User pUser) {
        PieChartModel tModel = new PieChartModel();
        try {
            int tTranscribedTotal = this.getAnnoCountData(pManifest, pUser).size();
            int tCanvasTotal = pManifest.getCanvases().size();
            int tToDoTotal = tCanvasTotal - tTranscribedTotal;
            tModel.set("Canvases with annotations: " + (int)(((double)tTranscribedTotal / tCanvasTotal) * 100) + "%", tTranscribedTotal);
            tModel.set("Canvases without annotations: " + (int)(((double)tToDoTotal / tCanvasTotal) * 100) + "%", tToDoTotal);
     
            tModel.setTitle("Amount left to do");
            tModel.setLegendPosition("w");
            tModel.setShadow(true);
        } catch (IOException tExcpt) {
            tExcpt.printStackTrace();
        }
        return tModel;
    }

    public BarChartModel getManifestAnnoCount(final String pURI) throws IOException {
        Manifest tManifest = this.getManifest(pURI);
        return getManifestAnnoCount(tManifest);
    }

    public BarChartModel getManifestAnnoCount(final Manifest pManifest) {
        UserService tUserService = new UserService();
        return getManifestAnnoCount(pManifest, tUserService.getUser());
    }

    public BarChartModel getManifestAnnoCount(final Manifest pManifest, final User pUser) {
        BarChartModel model = new BarChartModel();
        try {

            // Get list of all annotations
            List<PageAnnoCount> tPageCounts = this.getAnnoCountData(pManifest, pUser);
            System.out.println(tPageCounts);
     
            ChartSeries annoCounts = new ChartSeries();
            annoCounts.setLabel("Number of annotations");
            int tMax = 0;
            for (PageAnnoCount tCount : tPageCounts) {
                String tKey = "";
                if (tCount.getCanvas().getLabel().replaceAll("'","").length() > 9) {
                    tKey = tCount.getCanvas().getLabel().replaceAll("'","").substring(0,10);
                } else {
                    tKey = tCount.getCanvas().getLabel().replaceAll("'","");
                }
                if (annoCounts.getData().containsKey(tKey)) {
                    int i = 1;
                    String tOriginalKey = tKey.replaceAll("-[0-9][0-9]*$","");
                    while (annoCounts.getData().containsKey(tKey)) {
                        tKey = tOriginalKey + "-" + i++;
                    }
                }
                annoCounts.set(tKey, tCount.getCount()); 
                if (tCount.getCount() > tMax) {
                    tMax = tCount.getCount();
                }
            }
            model.addSeries(annoCounts);
            model.setDataRenderMode("value");
            model.setDatatipEditor("tooltip");

            model.setTitle("Annotations per Canvas for " + pManifest.getLabel());
            model.setLegendPosition("n");
            model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
            model.setShowPointLabels(true);
            model.getAxes().put(AxisType.X, new CategoryAxis("Canvases"));
            Axis xAxis = model.getAxis(AxisType.X);
            xAxis.setTickAngle(-30);
            xAxis.setTickFormat("%.5s");
            xAxis.setTickFormat("shorternString");
            Axis yAxis = model.getAxis(AxisType.Y);
            yAxis.setLabel("Count");
            yAxis.setMin(0);
            yAxis.setTickFormat("%.0f");
            yAxis.setMax(tMax + 10);
        } catch (IOException tExcpt) {
            tExcpt.printStackTrace();
        }
        return model;
    }

    public TopLevel getTopLevelStats() {
        TopLevel tStats = new TopLevel();
        try {
            tStats.setTotalAnnotations(_store.getTotalAnnotations(null));
            tStats.setTotalManifests(_store.getTotalManifests(null));
            tStats.setTotalAnnoCanvases(_store.getTotalAnnoCanvases(null));
        } catch (IOException tExcpt) {
            tExcpt.printStackTrace();
        }

        return tStats;
    }

    public Map<String, Integer> getAuthMethodStats() {
        try {
            return _store.getTotalAuthMethods();
        } catch (IOException tExcpt) {
            tExcpt.printStackTrace();
        }
        return new HashMap<String,Integer>();
    }
}
