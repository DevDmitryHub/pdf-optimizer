package com.mdfly.pdf.optimizer;


import com.mdfly.pdf.optimizer.references.ArrayReference;
import com.mdfly.pdf.optimizer.references.DictionaryReference;
import com.mdfly.pdf.optimizer.references.Reference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:DevDmitryHub@users.noreply.github.com">DevDmitry</a>
 */
public class PdfOptimizer {
    private static final Log LOG = LogFactory.getLog(PdfOptimizer.class);

    private PdfObjectsMerger merger;
    private PdfCompressor compressor;

    public PdfOptimizer() {
        this.merger = new PdfObjectsMerger();
        this.compressor = new ITextPdfCompressor();
    }

    /**
     * <p>
     * This method attempts to identify identical objects in a PDF
     * and remove all but a single instance of them. This is meant
     * to target in particular streams but other complex objects
     * (arrays and dictionaries) are also affected.
     * </p>
     * <p>
     * This method might be a bit over-eager in its removal. E.g.
     * in case of separate pages Adobe Reader expects different
     * objects for different pages. This method, though, might
     * collaps identically built pages to a single objects. Thus,
     * this method probably needs to be tamed a bit.
     * </p>
     */
    public void optimize(PDDocument pdDocument) throws IOException {
        Map<COSBase, Collection<Reference>> complexObjects = findComplexObjects(pdDocument);
        for (int pass = 1; ; pass++) {
            int merges = merger.mergeDuplicates(complexObjects);
            if (merges <= 0) {
                LOG.info(String.format("Pass %d - No merged objects", pass));
                break;
            }
            LOG.info(String.format("Pass %d - Merged objects: %d", pass, merges));
        }
    }

    public void optimize(Path sourcePdf, Path targetPdf) throws IOException {
        try (PDDocument document = PDDocument.load(sourcePdf.toFile())) {
            optimize(document);
            document.save(targetPdf.toFile());
        }
    }

    public void optimizeFonts(Path sourcePdf, Path targetPdf) throws IOException {
        try (PDDocument document = PDDocument.load(sourcePdf.toFile())) {
            optimizeFonts(document);
            document.save(targetPdf.toFile());
        }
    }

    public void optimizeFonts(PDDocument pdDocument) {
        Map<String, COSBase> fontFileCache = new HashMap<>();
        for (int pageNumber = 0; pageNumber < pdDocument.getNumberOfPages(); pageNumber++) {
            final PDPage page = pdDocument.getPage(pageNumber);
            COSDictionary pageDictionary = (COSDictionary) page.getResources().getCOSObject().getDictionaryObject(COSName.FONT);
            if (pageDictionary == null)
                continue;
            for (COSName currentFont : pageDictionary.keySet()) {
                COSDictionary fontDictionary = (COSDictionary) pageDictionary.getDictionaryObject(currentFont);
                for (COSName actualFont : fontDictionary.keySet()) {
                    COSBase actualFontDictionaryObject = fontDictionary.getDictionaryObject(actualFont);
                    if (actualFontDictionaryObject instanceof COSDictionary) {
                        COSDictionary fontFile = (COSDictionary) actualFontDictionaryObject;
                        if (fontFile.getItem(COSName.FONT_NAME) instanceof COSName) {
                            COSName fontName = (COSName) fontFile.getItem(COSName.FONT_NAME);
                            fontFileCache.computeIfAbsent(fontName.getName(), key -> fontFile.getItem(COSName.FONT_FILE2));
                            fontFile.setItem(COSName.FONT_FILE2, fontFileCache.get(fontName.getName()));
                        }
                    }
                }
            }
        }
    }

    private Map<COSBase, Collection<Reference>> findComplexObjects(PDDocument pdDocument) {
        COSDictionary catalogDictionary = pdDocument.getDocumentCatalog().getCOSObject();
        Map<COSBase, Collection<Reference>> result = new HashMap<>();
        result.put(catalogDictionary, new ArrayList<>());

        Set<COSBase> lastElements = Collections.singleton(catalogDictionary);
        Set<COSBase> currentElements = new HashSet<>();

        while (!lastElements.isEmpty()) {
            for (COSBase object : lastElements) {
                if (object instanceof COSArray) {
                    COSArray array = (COSArray) object;
                    for (int i = 0, s = array.size(); i < s; i++) {
                        addTargetRef(new ArrayReference(array, i), result, currentElements);
                    }
                } else if (object instanceof COSDictionary) {
                    COSDictionary dictionary = (COSDictionary) object;
                    for (COSName key : dictionary.keySet()) {
                        addTargetRef(new DictionaryReference(dictionary, key), result, currentElements);
                    }
                }
            }
            lastElements = currentElements;
            currentElements = new HashSet<>();
        }
        return result;
    }


    /**
     * Adds the given reference to its targets entry in
     * the mapping, also adding the target to the set if there was
     * no mapping before for the target (i.e. the target object has
     * not been analyzed yet and is newly found in this pass).
     */
    private void addTargetRef(Reference reference, Map<COSBase, Collection<Reference>> foundObjects, Set<COSBase> currentPass) {
        COSBase object = reference.getTo();
        if (object instanceof COSArray || object instanceof  COSDictionary) {
            Collection<Reference> foundRefs = foundObjects.get(object);
            if (foundRefs == null) {
                foundRefs = new ArrayList<>();
                foundObjects.put(object, foundRefs);
                currentPass.add(object);
            }
            foundRefs.add(reference);
        }
    }

    public PdfCompressor getCompressor() {
        return compressor;
    }

}
