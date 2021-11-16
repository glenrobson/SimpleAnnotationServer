package uk.org.llgc.annotation.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.util.Map;
import java.util.List;
import java.util.Iterator;

import com.github.jsonldjava.utils.JsonUtils;

import org.apache.jena.rdf.model.Model;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.controllers.UserService;
import uk.org.llgc.annotation.store.encoders.Encoder;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;

public class Populate extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(Populate.class.getName());
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		Encoder tEncoder = StoreConfig.getConfig().getEncoder();
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")), tEncoder);
		_store = StoreConfig.getConfig().getStore();
		_store.init(_annotationUtils);
	}

	public void doPost(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		InputStream tAnnotationList = null;
		if (pReq.getParameter("uri") != null) {
			_logger.debug("Reading from " + pReq.getParameter("uri"));
			tAnnotationList = new URL(pReq.getParameter("uri")).openStream();

            List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(tAnnotationList, StoreConfig.getConfig().getBaseURI(pReq) + "/annotation"); //annotaiton list
            addAnnoList(tAnnotationListJSON, pReq, pRes);
		} else if (ServletFileUpload.isMultipartContent(pReq)){
            System.out.println("Found multi part content");
			tAnnotationList = pReq.getInputStream();
            // Create a factory for disk-based file items
            DiskFileItemFactory factory = new DiskFileItemFactory();

            // Configure a repository (to ensure a secure temp location is used)
            ServletContext servletContext = this.getServletConfig().getServletContext();
            File repository = (File)servletContext.getAttribute("javax.servlet.context.tempdir");
            factory.setRepository(repository);

            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);

            // Parse the request
            List<FileItem> items = null;
            try {
                items = upload.parseRequest(pReq);
            } catch (FileUploadException tExcpt) {
                tExcpt.printStackTrace();
                pRes.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                pRes.setContentType("text/plain");
                pRes.getOutputStream().println("Failed to load annotation list due to: " + tExcpt.toString());
                return;
            }
            System.out.println("Items: " + items);
            // Process the uploaded items
            Iterator<FileItem> iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = iter.next();
                System.out.println("Found " + item);
                if (!item.isFormField()) {
                    List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(item.getInputStream(), StoreConfig.getConfig().getBaseURI(pReq) + "/annotation"); //annotaiton list
                    addAnnoList(tAnnotationListJSON, pReq, pRes);
                }
            }
		}
    }

    protected void addAnnoList(final List<Map<String, Object>> pAnnotationListJSON, final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        _logger.debug("JSON in:");
		_logger.debug(JsonUtils.toPrettyString(pAnnotationListJSON));

		try {
            AnnotationList tList = new AnnotationList(pAnnotationListJSON);
            tList.setCreator(new UserService(pReq).getUser());
			_store.addAnnotationList(tList);

			pRes.setStatus(HttpServletResponse.SC_CREATED);
			pRes.setContentType("text/plain");
			pRes.getOutputStream().println("SUCCESS");
		} catch (IDConflictException tException) {
			tException.printStackTrace();
			pRes.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			pRes.setContentType("text/plain");
			pRes.getOutputStream().println("Failed to load annotation list as there was a conflict in ids " + tException.toString());
		} catch (MalformedAnnotation tExcpt) {
            tExcpt.printStackTrace();
			pRes.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			pRes.setContentType("text/plain");
			pRes.getOutputStream().println("Falied to load annotation as it was badly informed: " + tExcpt.toString());
        }

    }
}
