package com.mdfly.pdf.optimizer;

import com.mdfly.pdf.optimizer.references.Reference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mdfly.pdf.optimizer.references.ReferenceUtils.resolveObject;


/**
 * @author <a href="mailto:DevDmitryHub@users.noreply.github.com">DevDmitry</a>
 */
class PdfObjectsMerger {
    private static final Log LOG = LogFactory.getLog(PdfObjectsMerger.class);

    /**
     * Identifies duplicate candidates by their hash value and then
     * forwards runs of objects with the same hash value to the method
     * {@link #mergeRun(Map, List)} for detailed comparison and actual
     * merging.
     */
    int mergeDuplicates(Map<COSBase, Collection<Reference>> complexObjects) throws IOException {
        List<COSBaseHash> hashes = complexObjects.keySet().stream()
            .map(COSBaseHash::new)
            .sorted()
            .collect(Collectors.toList());

        int removedDuplicates = 0;
        if (!hashes.isEmpty()) {
            int prevIndex = 0;
            int prevHash = hashes.get(prevIndex).getHash();
            for (int i = 0, s = hashes.size(); i < s; i++) {
                int hash = hashes.get(i).getHash();
                if (hash != prevHash) {
                    int count = i - prevIndex;
                    if (count != 1) {
                        LOG.info(String.format("Equal hash for %d elements.", count));
                        removedDuplicates += mergeRun(complexObjects, hashes.subList(prevIndex, i));
                    }
                    prevHash = hash;
                    prevIndex = i;
                }
            }
            int countTail = hashes.size() - prevIndex;
            if (countTail > 1) {
                LOG.info(String.format("Equal for %d elements.", countTail));
                removedDuplicates += mergeRun(complexObjects, hashes.subList(prevIndex, hashes.size()));
            }
        }
        return removedDuplicates;
    }

    private int mergeRun(Map<COSBase, Collection<Reference>> complexObjects, List<COSBaseHash> hashes) {
        int removedDuplicates = 0;
        List<List<COSBase>> duplicateSets = new ArrayList<>();
        hashes.forEach(baseHash -> {
            COSBase item = baseHash.getObject();
            for (List<COSBase> dupSet : duplicateSets) {
                if (equals(item, dupSet.get(0))) {
                    dupSet.add(item);
                    item = null;
                    break;
                }
            }
            if (item != null) {
                List<COSBase> dupSet = new ArrayList<>();
                dupSet.add(item);
                duplicateSets.add(dupSet);
            }
        });

        LOG.info(String.format("Identified %d set(s) of identical objects in run.", duplicateSets.size()));

        for (List<COSBase> dupSet : duplicateSets) {
            if (dupSet.size() > 1) {
                COSBase surviver = dupSet.remove(0);
                Collection<Reference> survRefs = complexObjects.get(surviver);
                for (COSBase duplicate : dupSet) {
                    Collection<Reference> refs = complexObjects.get(duplicate);
                    refs.forEach(ref -> {
                        ref.setTo(surviver);
                        survRefs.add(ref);
                    });
                    complexObjects.remove(duplicate);
                    duplicate.setDirect(false);

                    removedDuplicates++;
                }
                surviver.setDirect(false);
            }
        }
        return removedDuplicates;
    }

    private boolean equals(COSBase a, COSBase b) {
        if (a instanceof COSArray) {
            if (b instanceof COSArray) {
                COSArray aArray = (COSArray) a;
                COSArray bArray = (COSArray) b;
                if (aArray.size() == bArray.size()) {
                    for (int i = 0, s = aArray.size(); i < s; i++)
                        if (!resolveObject(aArray.get(i)).equals(resolveObject(bArray.get(i))))
                            return false;
                    return true;
                }
            }
        } else if (a instanceof COSDictionary) {
            if (b instanceof COSDictionary) {
                COSDictionary aDict = (COSDictionary) a;
                COSDictionary bDict = (COSDictionary) b;
                Set<COSName> keys = aDict.keySet();
                if (keys.equals(bDict.keySet())) {
                    for (COSName key : keys)
                        if (!resolveObject(aDict.getItem(key)).equals(bDict.getItem(key)))
                            return false;
                    return true;
                }
            }
        }
        return false;
    }

}
