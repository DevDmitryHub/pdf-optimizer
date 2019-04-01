package com.mdfly.pdf.optimizer.references;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSObject;

/**
 * @author <a href="mailto:DevDmitryHub@users.noreply.github.com">DevDmitry</a>
 */
public interface Reference {
    COSBase getFrom();
    COSBase getTo();
    void setTo(COSBase to);

}
