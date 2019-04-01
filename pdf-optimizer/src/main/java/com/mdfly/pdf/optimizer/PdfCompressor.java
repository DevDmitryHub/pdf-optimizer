package com.mdfly.pdf.optimizer;

import java.io.IOException;

/**
 * @author <a href="mailto:DevDmitryHub@users.noreply.github.com">DevDmitry</a>
 */
public interface PdfCompressor {
    void compress(String srcFileName, String destFileName, int level) throws IOException;
    void bestLevelCompress(String srcFileName, String destFileName) throws IOException;
    void fullCompression(String srcFileName, String destFileName) throws IOException;
}
