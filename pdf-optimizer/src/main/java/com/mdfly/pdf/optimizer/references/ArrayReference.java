package com.mdfly.pdf.optimizer.references;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;

/**
 * @author <a href="mailto:DevDmitryHub@users.noreply.github.com">DevDmitry</a>
 */
public class ArrayReference implements Reference {
    private final COSArray from;
    private final int index;

    public ArrayReference(COSArray array, int index) {
        this.from = array;
        this.index = index;
    }

    @Override
    public COSBase getFrom() {
        return from;
    }

    @Override
    public COSBase getTo() {
        return ReferenceUtils.resolveObject(from.get(index));
    }

    @Override
    public void setTo(COSBase to) {
        from.set(index, to);
    }
}
