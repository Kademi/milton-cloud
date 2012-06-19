package io.milton.sync;

import io.milton.common.Path;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

/**
 *
 * @author brad
 */
public class HttpUtils {


    /**
     * Takes an unencoded local path (eg "/my docs") and turns it into a
     * percentage encoded path (eg "/my%20docs"), with the encoded rootPath
     * added to the front
     *
     * @param path
     * @return
     */
    public static String toHref(Path basePath, Path unencodedPath) {
        Path p = basePath;
        for (String name : unencodedPath.getParts()) {
            p = p.child(io.milton.common.Utils.percentEncode(name));
        }
        return p.toString();
    }

    public static int executeHttpWithStatus(org.apache.http.client.HttpClient client, HttpUriRequest m, OutputStream out) throws IOException {
        HttpResponse resp = client.execute(m);
        HttpEntity entity = resp.getEntity();
        if (entity != null) {
            InputStream in = null;
            try {
                in = entity.getContent();
                if (out != null) {
                    IOUtils.copy(in, out);
                }
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
        return resp.getStatusLine().getStatusCode();
    }
}
