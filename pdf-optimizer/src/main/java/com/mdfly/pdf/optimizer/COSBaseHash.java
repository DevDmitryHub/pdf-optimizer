package com.mdfly.pdf.optimizer;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

/**
 * @author <a href="mailto:DevDmitryHub@users.noreply.github.com">DevDmitry</a>
 */
class COSBaseHash implements Comparable<COSBaseHash> {
    private final COSBase base;
    private final int hash;

    COSBaseHash(COSBase base) {
        this.base = base;
        this.hash = computeHash(base);
    }

    private Function<COSArray, Integer> computeArrayHash = array -> {
        int result = 1;
        for (COSBase item : array)
            result = result * 31 + (item == null ? 0 : item.hashCode());
        return result;
    };

    private Function<COSDictionary, Integer> computeDictHash = dict -> {
        int result = 3;
        for (Map.Entry<COSName, COSBase> entry : dict.entrySet())
            result += entry.hashCode();
        return result;
    };

    private int computeHash(COSBase base) {
        if (base instanceof COSArray) {
            return computeArrayHash.apply((COSArray) base);
        }
        if (base instanceof COSDictionary) {
            int result = computeDictHash.apply((COSDictionary) base);
            if (base instanceof COSStream) {
                try (InputStream data = ((COSStream) base).createRawInputStream()) {
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = data.read(buffer)) >= 0) {
                        md5.update(buffer, 0, bytesRead);
                    }
                    result = 31 * result + Arrays.hashCode(md5.digest());
                } catch (NoSuchAlgorithmException | IOException e) {
                    throw new RuntimeException("Couldn't calculate MD5 hash on COSStream", e);
                }
            }
            return result;
        }

        throw new IllegalArgumentException(String.format("Unknown complex COSBase type %s", base.getClass().getName()));
    }

    @Override
    public int compareTo(COSBaseHash o) {
        int result = Integer.compare(hash, o.hash);
        if (result == 0)
            result = Integer.compare(hashCode(), o.hashCode());
        return result;
    }

    int getHash() {
        return hash;
    }

    COSBase getObject() {
        return base;
    }
}
