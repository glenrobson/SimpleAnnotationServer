package uk.org.llgc.annotation.store.contollers;

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
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.PageAnnoCount;
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


    protected Manifest getManifestFromId(final String pShortId) throws IOException {
        if (_manifests.containsKey(pShortId)) {
            return _manifests.get(pShortId);
        } else {
            Manifest tManifest = _store.getManifest(pShortId);

            _manifests.put(pShortId, tManifest);
            return tManifest;
        }

    }

    public List<PageAnnoCount> getManifestAnnoCount(final Manifest pManifest) throws IOException {
        if (_manifestAnnoCount.containsKey(pManifest.getShortId())) {
            return _manifestAnnoCount.get(pManifest.getShortId());
        } else {
            List<PageAnnoCount> tPageCounts = _store.listAnnoPages(pManifest);
            _manifestAnnoCount.put(pManifest.getShortId(), tPageCounts);
            return tPageCounts;
        }
    }

    public PieChartModel getPercentAnnotated(final String pShortId) {
        PieChartModel tModel = new PieChartModel();
        try {
            Manifest tManifest = this.getManifestFromId(pShortId);
     
            int tTranscribedTotal = this.getManifestAnnoCount(tManifest).size();
            int tCanvasTotal = tManifest.getCanvases().size();
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

    public BarChartModel getManifestAnnoCount(final String pShortId) {
        BarChartModel model = new BarChartModel();
        try {
            Manifest tManifest = this.getManifestFromId(pShortId);

            // Get list of all annotations
            List<PageAnnoCount> tPageCounts = this.getManifestAnnoCount(tManifest);
     
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

            model.setTitle("Annotations per Canvas for " + tManifest.getLabel());
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
}
