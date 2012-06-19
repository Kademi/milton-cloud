package io.milton.sync.triplets;

import io.milton.common.Path;
import io.milton.http.exceptions.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.milton.cloud.common.HashUtils;
import io.milton.cloud.common.Triplet;
import io.milton.httpclient.Host;

/**
 * Loads triplets from a remote server over HTTP
 *
 * @author brad
 */
public class HttpTripletStore implements TripletStore {
    private final Host host;
    private final Path rootPath;

    /**
     * 
     * @param httpClient
     * @param rootPath 
     */
    public HttpTripletStore(Host httpClient, Path rootPath) {
        this.host = httpClient;
        this.rootPath = rootPath;
    }



    @Override
    public List<Triplet> getTriplets(Path path) {
        Path p = rootPath.add(path);
        Map<String,String> params = new HashMap<>();
        params.put("type", "hashes");        
                        
        try {            
            byte[] arrRemoteTriplets = host.doGet(p, params);
            List<Triplet> triplets = HashUtils.parseTriplets(new ByteArrayInputStream(arrRemoteTriplets));
            return triplets;
        } catch (IOException ex) {
            throw new RuntimeException(p.toString(), ex);
        } catch (NotFoundException ex) {
            return null;
        } catch(Throwable e) {
            throw new RuntimeException(p.toString(), e);
        }
    }    
}
