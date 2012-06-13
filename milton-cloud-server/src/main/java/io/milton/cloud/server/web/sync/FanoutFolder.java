package io.milton.cloud.server.web.sync;

import io.milton.resource.Resource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hashsplit4j.api.FanoutImpl;
import org.hashsplit4j.api.HashStore;
import io.milton.cloud.server.db.Organisation;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.resource.PutableResource;

/**
 *
 * @author brad
 */
public class FanoutFolder extends BaseResource implements PutableResource {

    private final HashStore hashStore;
    private final String name;

    public FanoutFolder(HashStore hashStore, String name, SpliffySecurityManager securityManager, Organisation org) {
        super(securityManager, org);
        this.hashStore = hashStore;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        long hash = Long.parseLong(newName);
        List<Long> list = new ArrayList<>();
        DataInputStream din = new DataInputStream(inputStream);
        long actualFanoutLength = din.readLong(); // first long is the content length of the chunks within the fanout. Ie the actual content length (not length of hashes!)
        try {
            while (true) {
                list.add(din.readLong());
            }
        } catch (EOFException e) {
            // cool
        }
        hashStore.setFanout(hash, list, actualFanoutLength);
        FanoutImpl fanoutImpl = new FanoutImpl(list, actualFanoutLength);
        return new FanoutResource(fanoutImpl, hash, securityManager, org);
    }

    @Override
    public Resource child(String string) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        return Collections.EMPTY_LIST;
    }
}
