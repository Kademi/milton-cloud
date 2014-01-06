package io.milton.sync.triplets;

import io.milton.common.Path;
import io.milton.http.exceptions.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.milton.httpclient.Host;
import org.hashsplit4j.triplets.HashCalc;
import org.hashsplit4j.triplets.ITriplet;

/**
 * Loads triplets from a remote server over HTTP. Can lookup triplets by path
 * or by parent directory hash
 *
 * @author brad
 */
public class HttpTripletStore implements ParentHashAwareTripletStore {
    private final Host host;
    private final Path rootPath;
    private final HashCalc hashCalc = HashCalc.getInstance();

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
    public List<ITriplet> getTriplets(Path path) {
        Path p = rootPath.add(path);
        p = p.child("_triplets");
        try {            
            byte[] arrRemoteTriplets = host.doGet(p);
            List<ITriplet> triplets = hashCalc.parseTriplets(new ByteArrayInputStream(arrRemoteTriplets));
            return triplets;
        } catch (IOException ex) {
            throw new RuntimeException(p.toString(), ex);
        } catch (NotFoundException ex) {
            return null;
        } catch(Throwable e) {
            throw new RuntimeException(p.toString(), e);
        }
    }

    @Override
    public List<ITriplet> getTriplets(String hash) {
        Path p = Path.root.child("_hashes").child("dirhashes").child(hash+"");
        Map<String,String> params = new HashMap<>();
                        
        try {            
            byte[] arrRemoteTriplets = host.doGet(p, params);
            List<ITriplet> triplets = hashCalc.parseTriplets(new ByteArrayInputStream(arrRemoteTriplets));
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
