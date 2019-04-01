package com.mdfly.pdf.optimizer;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;

/**
 * @author <a href="mailto:DevDmitryHub@users.noreply.github.com">DevDmitry</a>
 */
public class ITextPdfCompressor implements PdfCompressor {
    private static final Logger logger = LoggerFactory.getLogger(ITextPdfCompressor.class);

    @Override
    public void compress(String srcFileName, String destFileName, int level) throws IOException {
        try {
            PdfStamper stamper = createStamper(srcFileName, destFileName);
            PdfReader pdfReader = stamper.getReader();
            int unusedObjects = pdfReader.removeUnusedObjects();
            logger.info("Removed {} unused objects.", unusedObjects);
            stamper.getWriter().setCompressionLevel(level);
            processPdfPages(pdfReader);
            stamper.close();
        } catch (DocumentException e) {
            throw new IOException("Couldn't process pdf document " + srcFileName, e);
        }
    }

    @Override
    public void bestLevelCompress(String srcFileName, String destFileName) throws IOException {
        compress(srcFileName, destFileName, Deflater.BEST_COMPRESSION);
    }

    @Override
    public void fullCompression(String srcFileName, String destFileName) throws IOException {
        try {
            PdfStamper stamper = createStamper(srcFileName, destFileName);
            PdfReader pdfReader = stamper.getReader();
            int unusedObjects = pdfReader.removeUnusedObjects();
            logger.info("Removed {} unused objects.", unusedObjects);
            stamper.getWriter().setFullCompression();
            processPdfPages(pdfReader);
            stamper.close();
        } catch (DocumentException e) {
            throw new IOException("Couldn't process pdf document " + srcFileName, e);
        }
    }

    private PdfStamper createStamper(String srcFileName, String destFileName)
        throws IOException, DocumentException {
        FileOutputStream pdfFos = new FileOutputStream(destFileName);
        PdfReader pdfReader = new PdfReader(srcFileName);
        return new PdfStamper(pdfReader, pdfFos, PdfWriter.VERSION_1_5);
    }

    private void processPdfPages(PdfReader pdfReader) throws IOException {
        int total = pdfReader.getNumberOfPages() + 1;
        for (int i = 0; i < total; i++) {
            pdfReader.setPageContent(i, pdfReader.getPageContent(i));
        }
    }
}
