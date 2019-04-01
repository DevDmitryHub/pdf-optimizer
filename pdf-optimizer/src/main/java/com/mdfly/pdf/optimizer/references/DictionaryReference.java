package com.mdfly.pdf.optimizer.references;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;

/**
 * @author <a href="mailto:DevDmitryHub@users.noreply.github.com">DevDmitry</a>
 */
public class DictionaryReference implements Reference {
    private final COSDictionary from;
    private final COSName key;

    public DictionaryReference(COSDictionary from, COSName key) {
        this.from = from;
        this.key = key;
    }

    @Override
    public COSBase getFrom() {
        return from;
    }

    @Override
    public COSBase getTo() {
        return ReferenceUtils.resolveObject(from.getDictionaryObject(key));
    }

    @Override
    public void setTo(COSBase to) {
        from.setItem(key, to);
    }
}
