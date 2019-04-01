package com.mdfly.pdf.optimizer.references;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSObject;

/**
 * @author <a href="mailto:dmitrii.merliakov@amdocs.com">Dmitrii Merliakov</a>
 */
public class ReferenceUtils {

    public static COSBase resolveObject(COSBase object) {
        while (object instanceof COSObject)
            object = ((COSObject)object).getObject();
        return object;
    }
}
