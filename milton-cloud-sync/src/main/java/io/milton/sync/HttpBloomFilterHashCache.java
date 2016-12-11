package io.milton.sync;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;

import io.milton.httpclient.Host;
import io.milton.httpclient.HttpException;
import io.milton.sync.triplets.MemoryLocalTripletStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.hashsplit4j.api.HashCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author brad
 */
public class HttpBloomFilterHashCache implements HashCache {

    private static final Logger log = LoggerFactory.getLogger(HttpBloomFilterHashCache.class);

    private final Host host;
    private final Path basePath;
    private final String param;
    private final String paramVal;
    private BloomFilter<CharSequence> filter;

    public HttpBloomFilterHashCache(Host host, String basePath, String param, String paramVal) {
        this.host = host;
        this.basePath = Path.path(basePath);
        this.param = param;
        this.paramVal = paramVal;
        loadBloomFilter();
    }



    @Override
    public boolean hasHash(String hash) {
        boolean b = filter.mightContain(hash);
        if( !b ) {
            log.info("hasHash: " + b + " hash=" + hash + " in " + paramVal);
        }
        return b;
    }

    @Override
    public void setHash(String hash) {
        filter.put(hash);
    }

    private BloomFilter<CharSequence> loadBloomFilter() {
        try {
            Map<String,String> params = new HashMap();
            params.put(param, paramVal);
            byte[] arr = host.doGet(basePath, params);
            ByteArrayInputStream bin = new ByteArrayInputStream(arr);
            return loadBloomFilter(bin);
        } catch (IOException | NotFoundException | HttpException | NotAuthorizedException | BadRequestException | ConflictException iOException) {
            throw new RuntimeException("Could not load bloomfilter from " + basePath, iOException);
        }
    }

    private BloomFilter<CharSequence> loadBloomFilter(InputStream in) throws IOException {
        Funnel<CharSequence> funnel = Funnels.unencodedCharsFunnel();
        filter = BloomFilter.readFrom(in, funnel);
        return filter;
    }
}
