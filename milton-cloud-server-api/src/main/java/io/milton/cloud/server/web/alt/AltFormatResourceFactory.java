/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web.alt;

import io.milton.cloud.server.db.AltFormat;
import io.milton.cloud.server.web.FileResource;
import io.milton.cloud.server.web.HashResource;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.sync.GetResource;
import io.milton.common.FileUtils;
import io.milton.common.Path;
import io.milton.http.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;
import io.milton.vfs.db.utils.SessionManager;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;

import static io.milton.context.RequestContext._;

/**
 * Provides access to alternative formats for a given file.
 *
 * for example
 *
 * @author brad
 */
public class AltFormatResourceFactory implements ResourceFactory {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AltFormatResourceFactory.class);
    private final ResourceFactory wrapped;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private final AltFormatGenerator altFormatGenerator;

    public AltFormatResourceFactory(ResourceFactory wrapped, HashStore hashStore, BlobStore blobStore, AltFormatGenerator altFormatGenerator) {
        this.wrapped = wrapped;
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.altFormatGenerator = altFormatGenerator;
    }

    @Override
    public Resource getResource(String host, String sPath) throws NotAuthorizedException, BadRequestException {
        Path p = Path.path(sPath);
        if (p.getName().startsWith("alt-") && p.getName().endsWith(".m3u8") ) {
            Resource r = wrapped.getResource(host, p.getParent().toString());
            if (r instanceof HashResource) {
                HashResource hr = (HashResource) r;
                if( p.getName().equals("alt-hls.m3u8")) {
                    return new HlsPrimaryPlayListResource(AltFormatGenerator.HLS_FORMAT_SPEC, hr, sPath, hr, blobStore, hashStore, altFormatGenerator);
                } else {
                    return new HlsProgramPlayListResource(p.getName(), hr, blobStore, hashStore);
                }
            }       
            return null;
        } else if (p.getName().startsWith("alt-")) {
            Resource r = wrapped.getResource(host, p.getParent().toString());
            if (r instanceof HashResource) {
                HashResource hr = (HashResource) r;
                String sourceHash = hr.getHash();
                String formatName = p.getName().replace("alt-", "");
                AltFormat f = AltFormat.find(sourceHash, formatName, SessionManager.session());
                FormatSpec format = altFormatGenerator.findFormat(formatName);
                if (f != null) {
                    AltFormatResource alt = new AltFormatResource(hr, p.getName(), f, format, blobStore, hashStore, altFormatGenerator);
                    log.info("getResource: created resource for format: " + format + " - " + alt);
                    return alt;
                } else {
                    log.warn("getResource: pre-generated alt format not found: " + sourceHash + " - " + p.getName());
                    // if the format is valid then create a resource which will generate on demand                                        
                    if (format != null) {
                        AltFormatResource alt = new AltFormatResource(hr, p.getName(), format, blobStore, hashStore, altFormatGenerator);
                        log.info("getResource: created resource for format: " + format + " - " + alt);
                        return alt;
                    } else {
                        log.warn("getResource: unrecognised format: " + formatName);
                    }
                    return null;
                }
            } else {
                return null;
            }
        } else if (p.getName().endsWith(".ts")) {
            Resource r = wrapped.getResource(host, p.getParent().toString());
            if (r instanceof FileResource) {
                FileResource hr = (FileResource) r;
                String hash = FileUtils.stripExtension(p.getName());
                Fanout fanout = hashStore.getFileFanout(hash);
                if (fanout == null) {
                    log.warn("fanout not found");
                    return null;
                } else {
                    return new GetResource(p.getName(), fanout, hash, _(SpliffySecurityManager.class), blobStore, hashStore, hr.getParent());
                }
            }
            return null;
        } else {
            return wrapped.getResource(host, sPath);
        }
    }
}
