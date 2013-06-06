package io.milton.cloud.server.web.alt;

import io.milton.cloud.server.web.ContentDirectoryResource;
import io.milton.cloud.server.web.FileResource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.xhtmlrenderer.util.XRRuntimeException;
import org.xml.sax.SAXException;

public class PdfGenerator {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PdfGenerator.class);
    private static final long serialVersionUID = 1L;

    /**
     * Generate a PDF from multiple input web pages
     *
     * @param href
     * @param source
     * @param out
     * @throws NotAuthorizedException
     * @throws BadRequestException
     * @throws NotFoundException
     */
    public void convertHtmlToPdf(List<String> hrefs, OutputStream out) throws NotAuthorizedException, BadRequestException, NotFoundException {
        final MyTextRenderer renderer = new MyTextRenderer();
        boolean first = true;
        for (String href : hrefs) {
            try {
                renderer.setDocument(href);
                renderer.layout();
                if (first) {
                    first = false;
                    renderer.createPDF(out, false);
                } else {
                    renderer.writeNextDocument();
                }
            } catch (XRRuntimeException e) {
                Throwable e2 = e.getCause();
                if (e2 instanceof TransformerException) {
                    TransformerException te = (TransformerException) e2;
                    log.error(te.getMessageAndLocation());
                    SourceLocator loc = te.getLocator();
                    if (loc != null) {
                        log.error("Error at: " + loc.getLineNumber() + " - " + loc.getLineNumber() + " identifier: " + loc.getPublicId() + "/" + loc.getSystemId(), e2);
                    } else {
                        log.error("no locator");
                    }

                }
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        renderer.finishPDF();
        try {
            out.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    public FileResource convertHtmlToPdf(String href, GetableResource source, ContentDirectoryResource destDir, String destName) throws NotAuthorizedException, BadRequestException, NotFoundException {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        convertHtmlToPdf(href, source, outContent);
        FileResource fr = destDir.getOrCreateFile(destName);
        ByteArrayInputStream bin = new ByteArrayInputStream(outContent.toByteArray());
        fr.setContent(bin);
        return fr;
    }

    public void convertHtmlToPdf(String href, GetableResource source, OutputStream out) throws NotAuthorizedException, BadRequestException, NotFoundException {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        Map<String, String> params = new HashMap<>();
        try {
            source.sendContent(outContent, null, params, null);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        final InputStream in = new ByteArrayInputStream(outContent.toByteArray());
        final MyTextRenderer renderer = new MyTextRenderer();
        try {
            renderer.setDocument(in, href);
        } catch (SAXException e) {
            log.error("exception processing page: " + e.getClass(), e);
            throw new RuntimeException(e);
        } catch (XRRuntimeException e) {
            Throwable e2 = e.getCause();
            if (e2 instanceof TransformerException) {
                TransformerException te = (TransformerException) e2;
                log.error(te.getMessageAndLocation());
                SourceLocator loc = te.getLocator();
                if (loc != null) {
                    log.error("Error at: " + loc.getLineNumber() + " - " + loc.getLineNumber() + " identifier: " + loc.getPublicId() + "/" + loc.getSystemId(), e2);
                } else {
                    log.error("no locator");
                }

            }
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        renderer.layout();
        try {
            renderer.createPDF(out);
        } catch (Exception ex) {
            throw new RuntimeException("Exception processing: " + href, ex);
        }
        try {
            out.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }
//
//
//	public static void main(String[] args) {
//		if(args.length!=2)
//		{
//			System.err.println("Usage:Pdf2Image pdf imageFolder");
//			return;
//		}
//		File file = new File(args[0]);
//		RandomAccessFile raf;
//		try {
//			raf = new RandomAccessFile(file, "r");
//
//			FileChannel channel = raf.getChannel();
//			ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
//			PDFFile pdffile = new PDFFile(buf);
//			// draw the first page to an image
//			int num=pdffile.getNumPages();
//			for(int i=0;i<num;i++)
//			{
//				PDFPage page = pdffile.getPage(i);
//
//				//get the width and height for the doc at the default zoom
//				int width=(int)page.getBBox().getWidth();
//				int height=(int)page.getBBox().getHeight();
//
//				Rectangle rect = new Rectangle(0,0,width,height);
//				int rotation=page.getRotation();
//				Rectangle rect1=rect;
//				if(rotation==90 || rotation==270)
//					rect1=new Rectangle(0,0,rect.height,rect.width);
//
//				//generate the image
//				BufferedImage img = (BufferedImage)page.getImage(
//							rect.width, rect.height, //width & height
//							rect1, // clip rect
//							null, // null for the ImageObserver
//							true, // fill background with white
//							true  // block until drawing is done
//					);
//
//				ImageIO.write(img, "png", new File(args[1]+i+".png"));
//			}
//		}
//		catch (FileNotFoundException e1) {
//			System.err.println(e1.getLocalizedMessage());
//		} catch (IOException e) {
//			System.err.println(e.getLocalizedMessage());
//		}
//	}
}
